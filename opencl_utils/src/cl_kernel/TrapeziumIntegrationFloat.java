package cl_kernel;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLQueue;

public class TrapeziumIntegrationFloat extends AbstractKernel {

	public static final String KERNEL_NAME = "integrationFloat";
	private static final String KERNEL_PATH = "/cl_kernel/CLTrapeziumIntegrationFloat.cl";

	private static final int SIZEOF_CL_FLOAT = 4;

	public TrapeziumIntegrationFloat(CLContext context, String function) {
		super(context, KERNEL_NAME, KERNEL_PATH);
		this.extendedSource.add(function);
	}

	public FloatBuffer run(CLBuffer<Float> resultBuffer, float start,
			float offset, int n, int globalSize, int localSize) {
		if (context == null)
			return null;
		if (kernel == null)
			if (!createKernel())
				return null;

		kernel.setArg(0, resultBuffer);
		kernel.setArg(1, start);
		kernel.setArg(2, offset);
		kernel.setArg(3, n);
		kernel.setLocalArg(4, localSize * SIZEOF_CL_FLOAT);

		CLQueue cmdQ = context.createDefaultQueue();

		kernel.enqueueNDRange(cmdQ, new int[] { globalSize },
				new int[] { localSize }, new CLEvent[0]);

		cmdQ.finish();

		int localCount = (globalSize / localSize);
		FloatBuffer resBuffer = ByteBuffer
				.allocateDirect(localCount * SIZEOF_CL_FLOAT)
				.order(context.getByteOrder()).asFloatBuffer();
		resultBuffer.read(cmdQ, resBuffer, true, new CLEvent[0]);
		resBuffer.rewind();

		cmdQ.finish();
		cmdQ.release();

		return resBuffer;
	}

}
