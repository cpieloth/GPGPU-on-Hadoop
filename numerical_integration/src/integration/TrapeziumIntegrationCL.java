package integration;

import java.nio.FloatBuffer;

import lightLogger.Logger;
import cl_kernel.TrapeziumIntegrationFloat;
import cl_util.CLInstance;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLException;
import com.nativelibs4java.opencl.CLMem.Usage;

public class TrapeziumIntegrationCL implements INumeriacalIntegration<Float> {

	private static final Class<TrapeziumIntegrationCL> CLAZZ = TrapeziumIntegrationCL.class;
	private static final int MAX_ITEMS = 8388608; // Fan-In Summation ->
													// MAX_ITEMS == 2**x

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
		CLContext context = this.clInstance.getContext();
		TrapeziumIntegrationFloat kernel = (TrapeziumIntegrationFloat) this.clInstance
				.getKernel("", TrapeziumIntegrationFloat.KERNEL_NAME);
		if (kernel == null) {
			kernel = new TrapeziumIntegrationFloat(context,
					this.function.getOpenCLFunction());
			this.clInstance.addKernel(function.toString(),
					TrapeziumIntegrationFloat.KERNEL_NAME, kernel);
		}

		try {
			int globalSize = this.getOptimalItemCount(n + 1);
			int localSize = this.clInstance.calcWorkGroupSize(globalSize);
			int workGroups = (globalSize / localSize);

			CLBuffer<Float> resultBuffer = this.clInstance.getContext()
					.createFloatBuffer(Usage.Output, workGroups);

			FloatBuffer resBuffer = kernel.run(resultBuffer, start, offset, n,
					globalSize, localSize);

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
			while (dual < items && dual < MAX_ITEMS)
				dual *= 2;
			return dual;
		}
	}

}
