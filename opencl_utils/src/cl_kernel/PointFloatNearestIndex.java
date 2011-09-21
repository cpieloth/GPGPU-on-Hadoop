package cl_kernel;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import cl_util.CLInstance;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLQueue;

public class PointFloatNearestIndex extends AbstractKernel {

	private static final String KERNEL_NAME = "nearestIndex";
	private static final String KERNEL_PATH = "/cl_kernel/CLPointFloat.cl";

	private static final int SIZEOF_CL_INT = 4;
	
	private final CLContext CONTEXT;
	private final CLQueue QUEUE;
	private final int DIM;

	public PointFloatNearestIndex(CLInstance clInstance, int dim) {
		super(clInstance, KERNEL_NAME, KERNEL_PATH);
		this.defines.put("DEF_DIM", new Integer(dim));
		// this.defines.put("TYPE_T", "float");
		this.DIM = dim;
		CONTEXT = CL_INSTANCE.getContext();
		QUEUE = CL_INSTANCE.getQueue();
		createKernel();
	}

	public IntBuffer run(CLBuffer<Integer> resultBuffer,
			CLBuffer<Float> pointBuffer, int pointCount,
			CLBuffer<Float> centroidBuffer, int centroidCount) {
		kernel.setArg(0, resultBuffer);
		kernel.setArg(1, pointBuffer);
		kernel.setArg(2, pointCount);
		kernel.setArg(3, centroidBuffer);
		kernel.setArg(4, centroidCount);
		kernel.setArg(5, DIM);

		kernel.enqueueNDRange(QUEUE, new int[] { pointCount }, new CLEvent[0]);

		QUEUE.finish();

		IntBuffer res = ByteBuffer.allocateDirect(pointCount * SIZEOF_CL_INT)
				.order(CONTEXT.getByteOrder()).asIntBuffer();

		resultBuffer.read(QUEUE, 0, pointCount, res, true, new CLEvent[0]);

		QUEUE.finish();
		res.rewind();

		return res;
	}

	@Override
	public String getIdentifier() {
		return PointFloatNearestIndex.class.getName() + DIM;
	}
	
	public static String getIdentifier(int dim) {
		return PointFloatNearestIndex.class.getName();
	}

}
