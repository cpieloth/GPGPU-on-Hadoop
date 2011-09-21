package cl_kernel;

import static org.junit.Assert.fail;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLMem.Usage;

import cl_kernel.FloatGroupSum;
import cl_util.CLInstance;
import cl_util.CLInstance.TYPES;

public class FloatGroupSumTest {

	private static final float DELTA = 0.001f;
	private FloatGroupSum kernel;
	private CLInstance clInstance;
	
	@Before
	public void setUp() throws Exception {
		clInstance = new CLInstance(TYPES.CL_GPU);
		kernel = new FloatGroupSum(clInstance);
	}

	@Test
	public void testRun() {
		int globalSize = (int) clInstance.getMaxMemAllocSize();
		globalSize = getOptimalItemCount(globalSize / 4, globalSize);
		System.out.println("globalSize: " + globalSize);
		
		float[] values = generateRandomArray(globalSize);
		int localSize = clInstance.calcWorkGroupSize(globalSize);
		int localCount = globalSize / localSize;
		
		float[] resultExpected = Arrays.copyOf(values, values.length);
		long timeCpu = System.currentTimeMillis();
		resultExpected = computeGroupSum(resultExpected, globalSize, localSize, localCount);
		timeCpu = System.currentTimeMillis() - timeCpu;
		
		long timeOcl = System.currentTimeMillis();
		CLBuffer<Float> resultBuffer = clInstance.getContext().createFloatBuffer(
				Usage.InputOutput, FloatBuffer.wrap(values), true);
		long timeOclCompute = System.currentTimeMillis();
		FloatBuffer res = kernel.run(resultBuffer, globalSize, localSize, localCount);
		timeOclCompute = System.currentTimeMillis() - timeOclCompute;
		float[] result = new float[localCount];
		res.get(result);
		timeOcl = System.currentTimeMillis() - timeOcl;
		
		System.out.println("localSize: " + localSize);
		System.out.println("localCount: " + localCount);
		System.out.println("Time (CPU): " + timeCpu);
		System.out.println("Time (OCL): " + timeOcl);
		System.out.println("Time (OCL, without transfers): " + timeOclCompute);
		
		Assert.assertArrayEquals(resultExpected, result, DELTA);
	}
	
	private float[] computeGroupSum(float[] values, int globalSize,
			int localSize, int localCount) {
		float sum;
		float[] results = new float[localCount];
		for(int groupID = 0; groupID < localCount; ++groupID) {
			sum = 0;
			for(int localId = 0; localId < localSize; ++localId) {
				sum += values[groupID*localSize + localId];
			}
			results[groupID] = sum;
		}
		return results;
	}

	private float[] generateRandomArray(int size) {
		float[] array = new float[size];
		Random r = new Random(System.currentTimeMillis());
		for (int i = 0; i < size; ++i)
			array[i] = r.nextFloat();
		return array;
	}
	
	private int getOptimalItemCount(final int size, final int max) {
		if (size <= CLInstance.WAVE_SIZE)
			return CLInstance.WAVE_SIZE;
		else {
			int dual = CLInstance.WAVE_SIZE;
			while (dual < size && dual < max)
				dual *= 2;
			return dual;
		}
	}

	@Test
	public void testGetKernelName() {
		if (kernel.getKernelName() == null || "".equals(kernel.getKernelName()))
			fail("Invalid kernel name.");
	}

	@Test
	public void testGetKernelPath() {
		if (kernel.getKernelPath() == null || "".equals(kernel.getKernelPath()))
			fail("Invalid kernel path.");
	}

	@Test
	public void testCreateKernel() {
		if (!kernel.createKernel())
			fail("Could not create kernel.");
	}

	@Test
	public void testGetKernel() {
		if (!kernel.createKernel())
			fail("Could not create kernel.");
		else if (kernel.getKernel() == null)
			fail("No kernel available!");
	}

}
