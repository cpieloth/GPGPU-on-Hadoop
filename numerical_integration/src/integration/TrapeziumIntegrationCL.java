package integration;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import lightLogger.Logger;
import cl_util.CLInstance;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLException;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLQueue;

public class TrapeziumIntegrationCL implements INumeriacalIntegration<Float> {

	private static final Class<TrapeziumIntegrationCL> CLAZZ = TrapeziumIntegrationCL.class;
	private static final int MAX_ITEMS = 8388608; // Fan-In Summation -> MAX_ITEMS == 2**x
	private static final int SIZEOF_CL_FLOAT = 4;

	private static final String PREFIX = CLAZZ.getSimpleName();
	private static final String KERNEL_NAME = "integrationFloat";
	private static final String KERNEL_PATH = "/kernel/TrapeziumIntegration.cl";

	private CLInstance clInstance;

	private IMathFunction<Float> function;

	public TrapeziumIntegrationCL() {
		this.clInstance = new CLInstance(CLInstance.TYPES.CL_GPU);
	}

	@Override
	public void setFunction(IMathFunction<Float> function) {
		this.function = function;
	}

	@Override
	public Float getIntegral(IInterval<Float> interval) {
		float offset = interval.getEnd() - interval.getBegin();
		float result = 0;
		float start = interval.getBegin();
		int n = interval.getResolution();

		// get kernel and queue
		CLQueue cmdQ = this.clInstance.getQueue();
		CLKernel kernel = this.clInstance.loadKernel(KERNEL_PATH, KERNEL_NAME,
				PREFIX, function.getOpenCLFunction());

		try {
			int globalSize = this.getOptimalItemCount(n+1);
			int localSize = this.clInstance.calcWorkGroupSize(globalSize);
			int workGroups = (globalSize/localSize);
			
			CLBuffer<FloatBuffer> resultBuffer = this.clInstance.getContext()
					.createFloatBuffer(Usage.Output, workGroups);

			kernel.setArg(0, resultBuffer);
			kernel.setArg(1, start);
			kernel.setArg(2, offset);
			kernel.setArg(3, n);
			kernel.setLocalArg(4, localSize * SIZEOF_CL_FLOAT);

			kernel.enqueueNDRange(cmdQ, new int[] { globalSize }, new int[] { localSize },
					new CLEvent[0]);

			cmdQ.finish();

			FloatBuffer resBuffer = ByteBuffer
					.allocateDirect(workGroups * SIZEOF_CL_FLOAT)
					.order(this.clInstance.getContext().getByteOrder())
					.asFloatBuffer();
			resultBuffer.read(cmdQ, resBuffer, true, new CLEvent[0]);
			resBuffer.rewind();

			for (int i = 0; i < workGroups; i++)
				result += resBuffer.get();

			return result;
		} catch (CLException err) {
			Logger.logError(CLAZZ, "OpenCL error:\n" + err.getMessage() + "():"
					+ err.getCode());
			err.printStackTrace();
		} catch (Exception err) {
			Logger.logError(CLAZZ, "Error:\n" + err.getMessage() + "()");
			err.printStackTrace();
		}
		return 0f;
	}
	
	private int getOptimalItemCount(int items) {
		if (items <= CLInstance.WAVE_SIZE)
			return CLInstance.WAVE_SIZE;
		else {
			int dual = CLInstance.WAVE_SIZE;
			while(dual < items && dual < MAX_ITEMS)
				dual *= 2;
			return dual;
		}
	}

}
