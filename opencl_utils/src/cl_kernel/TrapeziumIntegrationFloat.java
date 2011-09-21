package cl_kernel;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import cl_util.CLInstance;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLQueue;

public class TrapeziumIntegrationFloat extends AbstractKernel {

	private static final String KERNEL_NAME = "integrationFloat";
	private static final String KERNEL_PATH = "/cl_kernel/CLTrapeziumIntegrationFloat.cl";

	private static final int SIZEOF_CL_FLOAT = 4;
	
	private final CLQueue QUEUE;
	private final CLContext CONTEXT;
	
	private final int FUNCTION_HASH;

	public TrapeziumIntegrationFloat(CLInstance clInstance, String function) {
		super(clInstance, KERNEL_NAME, KERNEL_PATH);
		this.extendedSource.add(function);
		QUEUE = clInstance.getQueue();
		CONTEXT = clInstance.getContext();
		createKernel();
		FUNCTION_HASH = function.hashCode();
		
	}

	public FloatBuffer run(CLBuffer<Float> resultBuffer, float start,
			float offset, int n, int globalSize, int localSize) {
		kernel.setArg(0, resultBuffer);
		kernel.setArg(1, start);
		kernel.setArg(2, offset);
		kernel.setArg(3, n);
		kernel.setLocalArg(4, localSize * SIZEOF_CL_FLOAT);

		kernel.enqueueNDRange(QUEUE, new int[] { globalSize },
				new int[] { localSize }, new CLEvent[0]);

		QUEUE.finish();

		int localCount = (globalSize / localSize);
		FloatBuffer resBuffer = ByteBuffer
				.allocateDirect(localCount * SIZEOF_CL_FLOAT)
				.order(CONTEXT.getByteOrder()).asFloatBuffer();
		resultBuffer.read(QUEUE, resBuffer, true, new CLEvent[0]);
		resBuffer.rewind();

		QUEUE.finish();

		return resBuffer;
	}

	@Override
	public String getIdentifier() {
		return TrapeziumIntegrationFloat.class.getName() + FUNCTION_HASH;
	}
	
	public static String getIdentifier(String function) {
		return TrapeziumIntegrationFloat.class.getName() + function.hashCode();
	}

}
