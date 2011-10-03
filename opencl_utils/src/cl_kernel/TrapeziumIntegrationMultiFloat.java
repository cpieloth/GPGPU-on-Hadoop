package cl_kernel;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import cl_util.CLInstance;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLQueue;

public class TrapeziumIntegrationMultiFloat extends AbstractKernel {

	private static final String KERNEL_NAME = "integrationMultiFloat";
	private static final String KERNEL_PATH = "/cl_kernel/CLTrapeziumIntegrationFloat.cl";

	private static final int SIZEOF_CL_FLOAT = 4;
	
	private final CLQueue QUEUE;
	private final CLContext CONTEXT;
	
	private final int FUNCTION_HASH;

	public TrapeziumIntegrationMultiFloat(CLInstance clInstance, String function) {
		super(clInstance, KERNEL_NAME, KERNEL_PATH);
		this.extendedSource.add(function);
		QUEUE = clInstance.getQueue();
		CONTEXT = clInstance.getContext();
		createKernel();
		FUNCTION_HASH = function.hashCode();
		
	}

	public FloatBuffer run(CLBuffer<Float> resultBuffer, CLBuffer<Float> beginBuffer,
			CLBuffer<Float> endBuffer, int size, int n, int globalSize, int localSize) {
		kernel.setArg(0, resultBuffer);
		kernel.setArg(1, beginBuffer);
		kernel.setArg(2, endBuffer);
		kernel.setArg(3, size);
		kernel.setArg(4, n);
		kernel.setLocalArg(5, localSize * SIZEOF_CL_FLOAT);

		kernel.enqueueNDRange(QUEUE, new int[] { globalSize },
				new int[] { localSize }, new CLEvent[0]);

		QUEUE.finish();

		int localCount = (globalSize / localSize);
		FloatBuffer resBuffer = ByteBuffer
				.allocateDirect(localCount * SIZEOF_CL_FLOAT * size)
				.order(CONTEXT.getByteOrder()).asFloatBuffer();
		resultBuffer.read(QUEUE, resBuffer, true, new CLEvent[0]);
		resBuffer.rewind();

		QUEUE.finish();

		return resBuffer;
	}

	@Override
	public String getIdentifier() {
		return TrapeziumIntegrationMultiFloat.class.getName() + FUNCTION_HASH;
	}
	
	public static String getIdentifier(String function) {
		return TrapeziumIntegrationMultiFloat.class.getName() + function.hashCode();
	}

}
