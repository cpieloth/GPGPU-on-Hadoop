package cl_kernel;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import cl_util.CLInstance;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLQueue;

/**
 * Parallel summation of float values. The result contains the sum of each work
 * group and not the sum of all values!
 * 
 * @author Christof Pieloth
 * 
 */
public class FloatGroupSum extends AbstractKernel {

	private static final String KERNEL_NAME = "floatGroupSum";
	private static final String KERNEL_PATH = "/cl_kernel/CLFloat.cl";

	private static final int SIZEOF_CL_FLOAT = 4;

	private final CLContext CONTEXT;
	private final CLQueue QUEUE;

	public FloatGroupSum(CLInstance clInstance) {
		super(clInstance, KERNEL_NAME, KERNEL_PATH);
		createKernel();
		CONTEXT = CL_INSTANCE.getContext();
		QUEUE = CL_INSTANCE.getQueue();
	}

	/**
	 * Calculates the sum of each work group. To calculate the total sum, all
	 * values ​​of the result buffer must be added.
	 * 
	 * @param resultBuffer
	 *            buffer to store the values
	 * @param globalSize
	 *            count for float values
	 * @param localSize
	 *            work group size
	 * @param firstItems
	 *            value count which should be read for return buffer
	 * @return FloatBuffer with the 0 to firstItems values of the resultBuffer
	 */
	public FloatBuffer run(CLBuffer<Float> resultBuffer, int globalSize,
			int localSize, int firstItems) {
		kernel.setArg(0, resultBuffer);
		kernel.setLocalArg(1, localSize * SIZEOF_CL_FLOAT);

		// Run kernel
		kernel.enqueueNDRange(QUEUE, new int[] { globalSize },
				new int[] { localSize }, new CLEvent[0]);

		QUEUE.finish();

		if (firstItems > 0) {
			FloatBuffer resBuffer = ByteBuffer
					.allocateDirect(firstItems * SIZEOF_CL_FLOAT)
					.order(CONTEXT.getByteOrder()).asFloatBuffer();
			resultBuffer.read(QUEUE, 0, firstItems, resBuffer, true,
					new CLEvent[0]);
			QUEUE.finish();
			resBuffer.rewind();
			return resBuffer;
		} else {
			return null;
		}
	}

	@Override
	public String getIdentifier() {
		return FloatGroupSum.class.getName();
	}

}
