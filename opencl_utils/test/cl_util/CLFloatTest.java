package cl_util;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class CLFloatTest {

	private static final float DELTA = 0;

	@Test
	public void testGetSum() {
		CLInstance clInstance = new CLInstance();
		clInstance.initialize(CLInstance.TYPES.CL_GPU);

		CLFloat clFloat = new CLFloat(clInstance);
		assertArrayEquals(new float[] { 0 }, new float[] { clFloat.getSum() },
				0);

		int COUNT;
		float VALUE, sum;

		COUNT = 2 * CLFloat.MAX_BUFFER_ITEMS + 13;
		VALUE = (float) Math.PI;
		sum = COUNT * VALUE;
		for (int i = 0; i < COUNT; i++) {
			clFloat.add(VALUE);
		}
		assertArrayEquals(new float[] { sum },
				new float[] { clFloat.getSum() }, DELTA);

		clFloat.add(10);
		sum += 10;
		assertArrayEquals(new float[] { sum },
				new float[] { clFloat.getSum() }, DELTA);

		clFloat.add(3);
		clFloat.reset();

		COUNT = CLFloat.MAX_BUFFER_ITEMS - 13;
		VALUE = (float) Math.E;
		sum = COUNT * VALUE;
		for (int i = 0; i < COUNT; i++) {
			clFloat.add(VALUE);
		}

		assertArrayEquals(new float[] { sum },
				new float[] { clFloat.getSum() }, 0);

		clFloat.reset();

		assertArrayEquals(new float[] { 0 }, new float[] { clFloat.getSum() },
				0);
	}

}
