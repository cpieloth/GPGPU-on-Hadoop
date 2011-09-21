package cl_kernel;

import static org.junit.Assert.fail;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import cl_util.CLInstance;
import cl_util.CLInstance.TYPES;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLMem.Usage;

public class PointFloatNearestIndexTest {

	private PointFloatNearestIndex kernel;
	private CLInstance clInstance;
	private final int DIM = 42, CENTROIDS = 33;

	@Before
	public void setUp() throws Exception {
		clInstance = new CLInstance(TYPES.CL_GPU);
		kernel = new PointFloatNearestIndex(clInstance, DIM);
	}

	@Test
	public void testRun() {
		final int count = (int) (clInstance.getMaxMemAllocSize() / 4 / DIM / 4);

		float[] points = generateRandomArray(count * DIM);
		float[] centroids = generateRandomArray(CENTROIDS * DIM);
		int[] result = new int[count];
		long timeCpu = System.currentTimeMillis();
		int[] resultExpected = getNearestIndex(points, count, centroids,
				CENTROIDS, DIM);
		timeCpu = System.currentTimeMillis() - timeCpu;

		long timeOcl = System.currentTimeMillis();
		CLBuffer<Integer> resultBuffer = clInstance.getContext().createIntBuffer(
				Usage.Output, count);
		CLBuffer<Float> pointBuffer = clInstance.getContext().createFloatBuffer(
				Usage.Input, FloatBuffer.wrap(points), true);
		CLBuffer<Float> centroidBuffer = clInstance.getContext().createFloatBuffer(
				Usage.Input, FloatBuffer.wrap(centroids), true);
		IntBuffer res = kernel.run(resultBuffer, pointBuffer, count,
				centroidBuffer, CENTROIDS);
		res.get(result);
		timeOcl = System.currentTimeMillis() - timeOcl;

		System.out.println("Time (CPU): " + timeCpu);
		System.out.println("Time (OCL): " + timeOcl);

		Assert.assertArrayEquals(resultExpected, result);
	}

	private int[] getNearestIndex(float[] points, final int pointCount,
			float[] centroids, final int centroidCount, final int dim) {
		int[] result = new int[pointCount];

		int iCentroid;
		float distance, prevDistance;

		for (int p = 0; p < points.length; p = p + dim) {
			iCentroid = -1;
			prevDistance = Float.MAX_VALUE;

			for (int c = 0; c < centroids.length; c = c + dim) {
				distance = 0f;
				for (int d = 0; d < DIM; d++) {
					distance += (float) Math.pow(centroids[c + d]
							- points[p + d], 2);
				}
				distance = (float) Math.sqrt(distance);

				if (distance < prevDistance) {
					prevDistance = distance;
					iCentroid = (int) (c / DIM);
				}
			}

			result[(int) (p / DIM)] = iCentroid;
		}

		return result;
	}

	private float[] generateRandomArray(int size) {
		float[] array = new float[size];
		Random r = new Random(System.currentTimeMillis());
		for (int i = 0; i < size; ++i)
			array[i] = r.nextFloat();
		return array;
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
