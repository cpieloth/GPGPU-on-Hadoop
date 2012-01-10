package integration;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import lightLogger.Logger;
import mathFunction.IInterval;
import mathFunction.IMathFunction;
import stopwatch.StopWatch;
import cl_kernel.TrapeziumIntegrationFloat;
import cl_kernel.TrapeziumIntegrationMultiFloat;
import cl_util.CLInstance;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLException;
import com.nativelibs4java.opencl.CLMem.Usage;

public class TrapeziumIntegrationMultiCL implements
		INumericalIntegrationMulti<Float> {

	private static final Class<TrapeziumIntegrationMultiCL> CLAZZ = TrapeziumIntegrationMultiCL.class;
	private static final int SIZEOF_CL_FLOAT = 4;

	private final int MAX_ITEM_SIZE;// = 8388608; // Fan-In Summation ->
									// MAX_ITEMS == 2**x
	private final CLInstance CL_INSTANCE;

	private IMathFunction<Float> function;

	private float[] begin;
	private float[] end;
	private int bufferCount;
	private int BUFFER_SIZE;
	private final int STANDARD_BUFFER_SIZE = 1024;

	public TrapeziumIntegrationMultiCL(CLInstance clInstance) {
		this.CL_INSTANCE = clInstance;
		// approximated MAX_ITEM_SIZE
		// FIXME calculation ...
		int mxItmSize = (int) ((clInstance.getMaxMemAllocSize()
				/ SIZEOF_CL_FLOAT / 8));
		mxItmSize = mxItmSize / 2 / 256; // arrays of begin, end and workgroup
											// results
		if ((mxItmSize & (mxItmSize - 1)) != 0)
			mxItmSize = (int) Math.pow(2,
					Math.floor(Math.log(mxItmSize) / Math.log(2)));
		MAX_ITEM_SIZE = mxItmSize;
		Logger.logDebug(CLAZZ, "MAX_ITEM_SIZE: " + MAX_ITEM_SIZE);

		reset();
	}

	@Override
	public void setFunction(IMathFunction<Float> function) {
		this.function = function;
	}

	@Override
	public Float getIntegral(IInterval<Float> interval, int resolution) {
		reset(1);
		put(interval);
		List<Float> integrals = getIntegrals(resolution);
		if (integrals.size() == 1) {
			return integrals.get(0);
		} else
			return Float.NaN;
	}

	public List<Float> getIntegrals(int resolution) {
		List<Float> integrals = new ArrayList<Float>(bufferCount);
		if(bufferCount < 1)
			return integrals;
		// get kernel and queue
		TrapeziumIntegrationMultiFloat kernel = (TrapeziumIntegrationMultiFloat) this.CL_INSTANCE
				.getKernel(TrapeziumIntegrationFloat.getIdentifier(function
						.getOpenCLFunction()));
		if (kernel == null) {
			kernel = new TrapeziumIntegrationMultiFloat(this.CL_INSTANCE,
					this.function.getOpenCLFunction());
		}
		try {
			int globalSize = this.getOptimalItemCount(resolution + 1);
			int localSize = this.CL_INSTANCE.calcWorkGroupSize(globalSize);
			int workGroups = (globalSize / localSize);
			Logger.logDebug(CLAZZ, "globalSize: " + globalSize);
			Logger.logDebug(CLAZZ, "localSize: " + localSize);
			Logger.logDebug(CLAZZ, "workGroups: " + workGroups);

			CLBuffer<Float> resultBuffer = this.CL_INSTANCE.getContext()
					.createFloatBuffer(Usage.Output, workGroups * bufferCount);

			CLBuffer<Float> beginBuffer = this.CL_INSTANCE.getContext()
					.createFloatBuffer(Usage.Input,
							FloatBuffer.wrap(begin, 0, bufferCount), true);
			CLBuffer<Float> endBuffer = this.CL_INSTANCE.getContext()
					.createFloatBuffer(Usage.Input,
							FloatBuffer.wrap(end, 0, bufferCount), true);

			StopWatch sw = new StopWatch("timeKernel=", ";");
			sw.start();
			FloatBuffer resBuffer = kernel.run(resultBuffer, beginBuffer,
					endBuffer, bufferCount, resolution, globalSize, localSize);
			sw.stop();
			Logger.logDebug(CLAZZ, sw.getTimeString());

			float result;
			for (int bc = 0; bc < bufferCount; bc++) {
				result = 0;
				for (int i = 0; i < workGroups; i++) {
					result += resBuffer.get();
				}
				integrals.add(new Float(result));
			}

			bufferCount = 0;
			return integrals;
		} catch (CLException err) {
			Logger.logError(CLAZZ, "OpenCL error:\n" + err.getMessage() + "():"
					+ err.getCode());
			err.printStackTrace();
		} catch (Exception err) {
			Logger.logError(CLAZZ, "Error:\n" + err.getMessage() + "()");
			err.printStackTrace();
		}
		return integrals;
	}

	private int getOptimalItemCount(int items) {
		if (items <= CLInstance.WAVE_SIZE)
			return CLInstance.WAVE_SIZE;
		else {
			int dual = CLInstance.WAVE_SIZE;
			while (dual < items)
				dual *= 2;
			return dual;
		}
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
	public int getItemCount() {
		return bufferCount;
	}

	@Override
	public int getCurrentMaxItemSize() {
		return BUFFER_SIZE;
	}

	@Override
	public int reset(int expectedItemSize) {
		Logger.logTrace(CLAZZ, "reset(" + expectedItemSize + ")");
		BUFFER_SIZE = expectedItemSize < MAX_ITEM_SIZE ? expectedItemSize
				: MAX_ITEM_SIZE;

		if (begin == null || BUFFER_SIZE > begin.length) {
			begin = new float[BUFFER_SIZE];
			end = new float[BUFFER_SIZE];
		}

		bufferCount = 0;
		return BUFFER_SIZE;
	}

	@Override
	public int reset() {
		return reset(STANDARD_BUFFER_SIZE);
	}

	@Override
	public boolean put(IInterval<Float> v) {
		if (bufferCount < BUFFER_SIZE) {
			begin[bufferCount] = v.getBegin();
			end[bufferCount] = v.getEnd();
			bufferCount++;
			return true;
		} else {
			return false;
		}
	}

}
