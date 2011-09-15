package cl_kernel;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLQueue;

public class FloatGroupSum extends AbstractKernel {

	public static final String KERNEL_NAME = "floatGroupSum";
	private static final String KERNEL_PATH = "/cl_kernel/CLFloat.cl";

	private static final int SIZEOF_CL_FLOAT = 4;

	public FloatGroupSum(CLContext context) {
		super(context, KERNEL_NAME, KERNEL_PATH);
		// this.defines.put("TYPE_T", "float");
	}

	public FloatBuffer run(CLBuffer<Float> resultBuffer, int globalSize,
			int localSize, int firstItems) {
		if (context == null)
			return null;
		if (kernel == null)
			if (!createKernel())
				return null;

		kernel.setArg(0, resultBuffer);
		kernel.setLocalArg(1, localSize * SIZEOF_CL_FLOAT);

		// Run kernel
		CLQueue cmdQ = context.createDefaultQueue();
		kernel.enqueueNDRange(cmdQ, new int[] { globalSize },
				new int[] { localSize }, new CLEvent[0]);

		cmdQ.finish();

		if (firstItems > 0) {
			FloatBuffer resBuffer = ByteBuffer
					.allocateDirect(firstItems * SIZEOF_CL_FLOAT)
					.order(context.getByteOrder()).asFloatBuffer();
			resultBuffer.read(cmdQ, 0, firstItems, resBuffer, true, new CLEvent[0]);
			resBuffer.rewind();
			cmdQ.finish();
			cmdQ.release();
			return resBuffer;
		} else {
			cmdQ.release();
			return null;
		}
	}

}
