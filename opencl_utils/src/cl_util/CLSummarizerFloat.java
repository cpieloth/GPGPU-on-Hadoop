package cl_util;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

import lightLogger.Logger;

import cl_kernel.FloatGroupSum;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLException;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLQueue;

public class CLSummarizerFloat implements ICLSummarizer<Float> {

	private static final Class<CLSummarizerFloat> CLAZZ = CLSummarizerFloat.class;

	private CLInstance clInstance;

	private int MAX_BUFFER_ITEMS;
	private int BUFFER_ITEMS;
	private static final int SIZEOF_CL_FLOAT = 4;

	private float[] buffer;
	private int count;
	private float sum;
	private CLBuffer<Float> resultBuffer;

	public CLSummarizerFloat(CLInstance clInstance) {
		this.clInstance = clInstance;
		MAX_BUFFER_ITEMS = (int) ((clInstance.getMaxMemAllocSize()
				/ SIZEOF_CL_FLOAT));
		if ((MAX_BUFFER_ITEMS & (MAX_BUFFER_ITEMS - 1)) != 0)
			MAX_BUFFER_ITEMS = (int) Math.pow(2,
					Math.floor(Math.log(MAX_BUFFER_ITEMS) / Math.log(2)));
		Logger.logDebug(CLAZZ, "MAX_BUFFER_ITEMS = " + MAX_BUFFER_ITEMS + "; "
				+ (MAX_BUFFER_ITEMS * SIZEOF_CL_FLOAT) / 1024 / 1024 + "MB");
		BUFFER_ITEMS = MAX_BUFFER_ITEMS / 16;
		this.resetResult();
		this.resetBuffer(BUFFER_ITEMS);
	}

	@Override
	public int getMaxBufferItems() {
		return MAX_BUFFER_ITEMS;
	}

	@Override
	public int getCurrentMaxBufferItems() {
		return BUFFER_ITEMS;
	}

	@Override
	public int resetBuffer(int bufferItems) {
		Logger.logTrace(CLAZZ, "resetBuffer(" + bufferItems + ")");
		boolean error;
		do {
			error = false;
			try {
				BUFFER_ITEMS = this.getOptimalItemCount(bufferItems);

				if (this.buffer == null || BUFFER_ITEMS > this.buffer.length)
					this.buffer = new float[BUFFER_ITEMS];
				this.count = 0;

				this.resultBuffer = this.clInstance.getContext()
						.createFloatBuffer(CLMem.Usage.InputOutput,
								BUFFER_ITEMS);

			} catch (CLException.InvalidBufferSize e) {
				Logger.logError(CLAZZ,
						"Could not create CLBuffer! Resize buffer item.");
				MAX_BUFFER_ITEMS = BUFFER_ITEMS / 2;
				bufferItems /= 2;
				error = true;
				this.buffer = null;
			} catch (OutOfMemoryError e) {
				Logger.logError(CLAZZ, "Could not create float array! Resize.");
				MAX_BUFFER_ITEMS = BUFFER_ITEMS / 2;
				bufferItems /= 2;
				error = true;
				this.buffer = null;
			}
		} while (error);
		Logger.logDebug(CLAZZ, "resetBuffer() - BUFFER_ITEMS = " + BUFFER_ITEMS
				+ " ~" + (BUFFER_ITEMS * SIZEOF_CL_FLOAT) / 1024 / 1024 + "MB");
		return BUFFER_ITEMS;
	}

	@Override
	public int resetBuffer() {
		return this.resetBuffer(MAX_BUFFER_ITEMS);
	}

	private int getOptimalItemCount(int bufferItems) {
		if (bufferItems <= CLInstance.WAVE_SIZE)
			return CLInstance.WAVE_SIZE;
		else {
			int dual = CLInstance.WAVE_SIZE;
			while (dual < bufferItems && dual < MAX_BUFFER_ITEMS)
				dual *= 2;
			return dual;
		}
	}

	@Override
	public void resetResult() {
		this.sum = 0;
		this.count = 0;
	}

	@Override
	public void put(Float value) {
		if (this.count < BUFFER_ITEMS) {
			this.buffer[count++] = value;
		} else {
			this.doSum(count);
			this.buffer[count++] = value;
		}
	}

	private void doSum(int size) {
		Logger.logTrace(CLAZZ, "doSum(" + size + ")");
		int globalSize, localSize;
		globalSize = this.getOptimalItemCount(size);
		// fill offset with 0
		Arrays.fill(this.buffer, size, globalSize, 0);

		size = globalSize;

		// get kernel and queue
		CLContext context = this.clInstance.getContext();
		CLQueue cmdQ = this.clInstance.getQueue();

		FloatGroupSum kernel = (FloatGroupSum) this.clInstance.getKernel("",
				FloatGroupSum.KERNEL_NAME);
		if (kernel == null) {
			kernel = new FloatGroupSum(context);
			this.clInstance.addKernel("", FloatGroupSum.KERNEL_NAME, kernel);
		}

		try {
			// write buffer on device
			this.resultBuffer.write(cmdQ, 0, BUFFER_ITEMS,
					FloatBuffer.wrap(this.buffer, 0, BUFFER_ITEMS), true,
					new CLEvent[0]);

			// multiple rounds to sum each work group
			do {
				globalSize = this.getOptimalItemCount(size);
				// fill offset with 0 and write on device
				if (size < globalSize) {
					for (int i = 0; i < globalSize - size; i++)
						buffer[i] = 0;
					resultBuffer.write(cmdQ, size, globalSize - size,
							FloatBuffer.wrap(this.buffer, 0, BUFFER_ITEMS),
							true, new CLEvent[0]);
				}
				localSize = this.clInstance.calcWorkGroupSize(globalSize);

				kernel.run(resultBuffer, globalSize, localSize, 0);

				size = globalSize / localSize;
			} while (globalSize > localSize && localSize > 1);

			cmdQ.finish();

			FloatBuffer resBuffer = ByteBuffer
					.allocateDirect(1 * SIZEOF_CL_FLOAT)
					.order(this.clInstance.getContext().getByteOrder())
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
		this.count = 0;
	}

	public Float getSum() {
		if (0 < this.count || this.count == BUFFER_ITEMS)
			this.doSum(this.count);
		return this.sum;
	}
}
