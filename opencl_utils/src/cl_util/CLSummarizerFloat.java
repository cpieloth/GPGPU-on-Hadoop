package cl_util;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

import lightLogger.Logger;
import cl_kernel.FloatGroupSum;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLException;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLQueue;

public class CLSummarizerFloat implements ICLSummarizer<Float> {

	private static final Class<CLSummarizerFloat> CLAZZ = CLSummarizerFloat.class;

	private final CLInstance CL_INSTANCE;

	private final int BUFFER_SIZE;
	private int bufferCount;
	private final float[] buffer;

	private final int MAX_ITEM_SIZE;
	private int itemCount;
	private int itemSize;

	private static final int SIZEOF_CL_FLOAT = 4;

	private CLBuffer<Float> resultBuffer;
	private float[] neutral;
	private float sum;

	private FloatGroupSum kernel;
	
	public CLSummarizerFloat(CLInstance clInstance) {
		this(clInstance, 65536);
	}

	public CLSummarizerFloat(CLInstance clInstance, int bufferSize) {
		BUFFER_SIZE = bufferSize;
		this.CL_INSTANCE = clInstance;
		int mxItmSize = (int) ((clInstance.getMaxMemAllocSize()
				/ SIZEOF_CL_FLOAT / 8));
		if ((mxItmSize & (mxItmSize - 1)) != 0)
			mxItmSize = (int) Math.pow(2,
					Math.floor(Math.log(mxItmSize) / Math.log(2)));
		MAX_ITEM_SIZE = mxItmSize;
		Logger.logDebug(CLAZZ, "MAX_ITEM_SIZE = " + MAX_ITEM_SIZE + "; "
				+ (MAX_ITEM_SIZE * SIZEOF_CL_FLOAT) / 1024 / 1024 + "MB");

		this.buffer = new float[BUFFER_SIZE];
		this.neutral = new float[BUFFER_SIZE];
		Arrays.fill(neutral, 0);
		
		this.resetResult();
		this.reset();
		
		kernel = (FloatGroupSum) CL_INSTANCE.getKernel(FloatGroupSum.class.getName());
		if(kernel == null)
			kernel = new FloatGroupSum(CL_INSTANCE);
	}

	@Override
	public int getBufferSize() {
		return BUFFER_SIZE;
	}

	@Override
	public int getBufferCount() {
		return bufferCount;
	}

	@Override
	public int getMaxItemSize() {
		return MAX_ITEM_SIZE;
	}

	@Override
	public int getCurrentMaxItemSize() {
		return itemSize;
	}

	@Override
	public int getItemCount() {
		return itemCount;
	}

	@Override
	public int reset(int expectedItemSize) {
		Logger.logTrace(CLAZZ, "reset(" + expectedItemSize + ")");
		boolean error;

		bufferCount = 0;
		itemCount = 0;
		
		do {
			error = false;
			try {
				itemSize = this.getOptimalItemCount(expectedItemSize);

				if (resultBuffer != null)
					resultBuffer.release();

				resultBuffer = CL_INSTANCE.getContext()
						.createFloatBuffer(CLMem.Usage.InputOutput, itemSize);
			} catch (CLException.InvalidBufferSize e) {
				Logger.logError(CLAZZ,
						"Could not create CLBuffer! Resize buffer item.");
				expectedItemSize /= 2;
				error = true;
			}
		} while (error);
		
		Logger.logDebug(CLAZZ, "reset() - itemSize = " + itemSize + " ~"
				+ (itemSize * SIZEOF_CL_FLOAT) / 1024 / 1024 + "MB");
		
		return itemSize;
	}

	@Override
	public int reset() {
		return this.reset(MAX_ITEM_SIZE);
	}

	private int getOptimalItemCount(int bufferItems) {
		if (bufferItems <= CLInstance.WAVE_SIZE)
			return CLInstance.WAVE_SIZE;
		else {
			int dual = CLInstance.WAVE_SIZE;
			while (dual < bufferItems && dual < MAX_ITEM_SIZE)
				dual *= 2;
			return dual;
		}
	}

	@Override
	public void resetResult() {
		this.sum = 0;
		this.bufferCount = 0;
		this.itemCount = 0;
	}

	@Override
	public void put(Float value) {
		// buffer ist noch nicht voll
		if (bufferCount < BUFFER_SIZE) {
			buffer[bufferCount++] = value;
		} else {
			writeBufferToOCL();
			put(value);
		}
	}

	private void writeBufferToOCL() {
		if (bufferCount == 0)
			return;

		// buffer fits into OCL memory
		if ((itemSize - itemCount) >= bufferCount) {
			resultBuffer.write(CL_INSTANCE.getQueue(), itemCount, bufferCount,
					FloatBuffer.wrap(buffer, 0, bufferCount), true);
			itemCount += bufferCount;
			bufferCount = 0;
		} else {
			doSum();
			writeBufferToOCL();
		}

	}

	private void doSum() {
		Logger.logTrace(CLAZZ, "doSum()");
		if (itemCount == 0)
			return;

		int globalSize, localSize, neutrals;

		// get kernel and queue
		CLQueue cmdQ = this.CL_INSTANCE.getQueue();

		try {
			// multiple rounds to sum each work group
			do {
				globalSize = this.getOptimalItemCount(itemCount);
				neutrals = globalSize - itemCount;
				while (neutrals > 0) {
					if (neutrals > neutral.length) {
						resultBuffer.write(cmdQ, itemCount, neutral.length,
								FloatBuffer.wrap(neutral), true);
						itemCount += neutral.length;
						neutrals -= neutral.length;
					} else {
						resultBuffer.write(cmdQ, itemCount, neutrals,
								FloatBuffer.wrap(neutral, 0, neutrals), true);
						itemCount += neutrals;
						break;
					}
				}
				localSize = this.CL_INSTANCE.calcWorkGroupSize(globalSize);

				kernel.run(resultBuffer, globalSize, localSize, 0);

				itemCount = globalSize / localSize;
			} while (globalSize > localSize && localSize > 1);

			cmdQ.finish();

			FloatBuffer resBuffer = ByteBuffer
					.allocateDirect(1 * SIZEOF_CL_FLOAT)
					.order(this.CL_INSTANCE.getContext().getByteOrder())
					.asFloatBuffer();
			resultBuffer.read(cmdQ, 0, 1, resBuffer, true, new CLEvent[0]);
			resBuffer.rewind();

			this.sum += resBuffer.get(0);
		} catch (CLException err) {
			Logger.logError(CLAZZ, "OpenCL error:\n" + err.getMessage() + "():"
					+ err.getCode());
			err.printStackTrace();
		} catch (Exception err) {
			Logger.logError(CLAZZ, "Error:\n" + err.getMessage() + "()");
			err.printStackTrace();
		}
		// all values in buffer are summarized and can be overwritten
		this.itemCount = 0;
	}

	public Float getSum() {
		writeBufferToOCL();
		this.doSum();
		return sum;
	}

}
