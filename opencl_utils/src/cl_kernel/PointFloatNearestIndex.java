package cl_kernel;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLQueue;

public class PointFloatNearestIndex extends AbstractKernel {

	public static final String KERNEL_NAME = "nearestIndex";
	private static final String KERNEL_PATH = "/cl_kernel/CLPointFloat.cl";

	private static final int SIZEOF_CL_INT = 4;
	
	private final int dim;

	public PointFloatNearestIndex(CLContext context, int dim) {
		super(context,KERNEL_NAME, KERNEL_PATH);
		this.defines.put("DEF_DIM", new Integer(dim));
		// this.defines.put("TYPE_T", "float");
		this.dim = dim;
	}

	public IntBuffer run(CLBuffer<Integer> resultBuffer,
			CLBuffer<Float> pointBuffer, int pointCount,
			CLBuffer<Float> centroidBuffer, int centroidCount) {
		if (context == null)
			return null;
		if (kernel == null)
			if (!createKernel())
				return null;

		kernel.setArg(0, resultBuffer);
		kernel.setArg(1, pointBuffer);
		kernel.setArg(2, pointCount);
		kernel.setArg(3, centroidBuffer);
		kernel.setArg(4, centroidCount);
		kernel.setArg(5, dim);

		CLQueue cmdQ = context.createDefaultQueue();

		kernel.enqueueNDRange(cmdQ, new int[] { pointCount }, new CLEvent[0]);

		cmdQ.finish();

		IntBuffer res = ByteBuffer.allocateDirect(pointCount * SIZEOF_CL_INT)
				.order(context.getByteOrder()).asIntBuffer();

		resultBuffer.read(cmdQ, 0, pointCount, res, true, new CLEvent[0]);

		cmdQ.finish();
		cmdQ.release();
		res.rewind();

		return res;
	}

}
