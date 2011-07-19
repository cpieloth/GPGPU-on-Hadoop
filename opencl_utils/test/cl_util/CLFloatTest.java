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
		
		// full buffer size test
		COUNT = CLFloat.MAX_BUFFER_ITEMS;
		VALUE = (float) Math.PI;
		sum = COUNT * VALUE;
		for (int i = 0; i < COUNT; i++) {
			clFloat.add(VALUE);
		}
		assertArrayEquals(new float[] { sum },
				new float[] { clFloat.getSum() }, DELTA);
		
		clFloat.resetResult();
		
		// value test
		COUNT = CLFloat.MAX_BUFFER_ITEMS & 1024;
		VALUE = (float) Math.PI;
		sum = COUNT/2 * VALUE;
		VALUE = (float) Math.E;
		sum += COUNT/2 * VALUE;
		for (int i = 0; i < COUNT; i++) {
			if(i % 2 == 0)
				VALUE = (float) Math.PI;
			else
				VALUE = (float) Math.E;
			clFloat.add(VALUE);
		}
		assertArrayEquals(new float[] { sum },
				new float[] { clFloat.getSum() }, DELTA);
		
		clFloat.resetResult();

		// buffer overflow test
		COUNT = 2 * CLFloat.MAX_BUFFER_ITEMS + 13;
		VALUE = (float) Math.PI;
		sum = COUNT * VALUE;
		for (int i = 0; i < COUNT; i++) {
			clFloat.add(VALUE);
		}
		assertArrayEquals(new float[] { sum },
				new float[] { clFloat.getSum() }, DELTA);

		// intermediate result test
		clFloat.add(10);
		sum += 10;
		assertArrayEquals(new float[] { sum },
				new float[] { clFloat.getSum() }, DELTA);

		// reset test
		clFloat.add(3);
		clFloat.resetResult();

		// less buffer items test
		COUNT = CLFloat.MAX_BUFFER_ITEMS - 13;
		VALUE = (float) Math.E;
		sum = COUNT * VALUE;
		for (int i = 0; i < COUNT; i++) {
			clFloat.add(VALUE);
		}
		assertArrayEquals(new float[] { sum },
				new float[] { clFloat.getSum() }, DELTA);

		clFloat.resetResult();

		assertArrayEquals(new float[] { 0 }, new float[] { clFloat.getSum() },
				DELTA);
		
		// buffer resize test
		COUNT = CLInstance.WAVE_SIZE;
		VALUE = (float) Math.E;
		sum = COUNT * VALUE;
		clFloat.resetBuffer(COUNT);
		for (int i = 0; i < COUNT; i++) {
			clFloat.add(VALUE);
		}

		assertArrayEquals(new float[] { sum },
				new float[] { clFloat.getSum() }, DELTA);
		
		clFloat.resetResult();
		
		COUNT = 2 * CLInstance.WAVE_SIZE + 5;
		VALUE = (float) Math.E;
		sum = COUNT * VALUE;
		clFloat.resetBuffer(COUNT);
		for (int i = 0; i < COUNT; i++) {
			clFloat.add(VALUE);
		}

		assertArrayEquals(new float[] { sum },
				new float[] { clFloat.getSum() }, DELTA);
	}

}
