package cl_util;

import static org.junit.Assert.assertArrayEquals;
import junit.framework.Assert;

import org.junit.Test;

public class CLSummarizerFloatTest {

	private static final float DELTA = 0f;

	@Test
	public void testGetSum() {
		CLInstance clInstance = new CLInstance();
		clInstance.initialize(CLInstance.TYPES.CL_GPU);

		CLSummarizerFloat clFloat = new CLSummarizerFloat(clInstance);
		assertArrayEquals(new float[] { 0 }, new float[] { clFloat.getSum() },
				0);

		int COUNT;
		float VALUE, sum;

		// full buffer size test
		COUNT = clFloat.getMaxItemSize();
		VALUE = 1;
		sum = COUNT * VALUE;
		for (int i = 0; i < COUNT; i++) {
			clFloat.put(VALUE);
		}
		assertArrayEquals(new float[] { sum },
				new float[] { clFloat.getSum() }, DELTA);

		clFloat.resetResult();

		// value test
		COUNT = clFloat.getMaxItemSize() & 1024;
		VALUE = (float) 1;
		sum = COUNT / 2 * VALUE;
		VALUE = (float) 2;
		sum += COUNT / 2 * VALUE;
		for (int i = 0; i < COUNT; i++) {
			if (i % 2 == 0)
				VALUE = (float) 1;
			else
				VALUE = (float) 2;
			clFloat.put(VALUE);
		}
		assertArrayEquals(new float[] { sum },
				new float[] { clFloat.getSum() }, DELTA);

		clFloat.resetResult();

		// buffer overflow test
		COUNT = 2 * clFloat.getMaxItemSize() + 13;
		VALUE = (float) 1;
		sum = COUNT * VALUE;
		for (int i = 0; i < COUNT; i++) {
			clFloat.put(VALUE);
		}
		assertArrayEquals(new float[] { sum },
				new float[] { clFloat.getSum() }, DELTA);

		// intermediate result test
		VALUE = (float) 1;
		clFloat.put(VALUE);
		sum += VALUE;
		assertArrayEquals(new float[] { sum },
				new float[] { clFloat.getSum() }, DELTA);

		// reset test
		clFloat.put((float) 3);
		clFloat.resetResult();

		// less buffer items test
		COUNT = clFloat.getMaxItemSize() - 13;
		VALUE = (float) 1;
		sum = COUNT * VALUE;
		for (int i = 0; i < COUNT; i++) {
			clFloat.put(VALUE);
		}
		assertArrayEquals(new float[] { sum },
				new float[] { clFloat.getSum() }, DELTA);

		clFloat.resetResult();

		assertArrayEquals(new float[] { 0 }, new float[] { clFloat.getSum() },
				DELTA);

		// buffer resize test
		COUNT = CLInstance.WAVE_SIZE;
		VALUE = (float) 1;
		sum = COUNT * VALUE;
		clFloat.reset(COUNT);
		for (int i = 0; i < COUNT; i++) {
			clFloat.put(VALUE);
		}

		assertArrayEquals(new float[] { sum },
				new float[] { clFloat.getSum() }, DELTA);

		clFloat.resetResult();

		COUNT = 2 * CLInstance.WAVE_SIZE + 5;
		VALUE = (float) 1;
		sum = COUNT * VALUE;
		clFloat.reset(COUNT);
		for (int i = 0; i < COUNT; i++) {
			clFloat.put(VALUE);
		}

		assertArrayEquals(new float[] { sum },
				new float[] { clFloat.getSum() }, DELTA);
	}

	@Test
	public void testReset() {
		CLInstance clInstance = new CLInstance();
		clInstance.initialize(CLInstance.TYPES.CL_GPU);

		ICLBufferedOperation<Float> clBufferedOp = new CLSummarizerFloat(
				clInstance);
		clBufferedOp.reset();
		assertArrayEquals(new int[] { clBufferedOp.getMaxItemSize() },
				new int[] { clBufferedOp.getCurrentMaxItemSize() });

		int items = 2 * clBufferedOp.getMaxItemSize();
		clBufferedOp.reset(items);
		assertArrayEquals(new int[] { clBufferedOp.getMaxItemSize() },
				new int[] { clBufferedOp.getCurrentMaxItemSize() });

		items = clBufferedOp.getMaxItemSize() / 2;
		clBufferedOp.reset(items);
		int currItems = clBufferedOp.getCurrentMaxItemSize();
		if (!(items <= currItems && currItems <= clBufferedOp
				.getMaxItemSize()))
			Assert.fail();

		items = 0;
		clBufferedOp.reset(items);
		currItems = clBufferedOp.getCurrentMaxItemSize();
		if (!(items < currItems && currItems <= clBufferedOp
				.getMaxItemSize()))
			Assert.fail();

		items = -1;
		clBufferedOp.reset(items);
		currItems = clBufferedOp.getCurrentMaxItemSize();
		if (!(items < currItems && currItems <= clBufferedOp
				.getMaxItemSize()))
			Assert.fail();
	}

}
