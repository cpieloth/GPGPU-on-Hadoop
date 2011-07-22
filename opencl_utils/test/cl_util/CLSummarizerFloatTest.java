package cl_util;

import static org.junit.Assert.assertArrayEquals;
import junit.framework.Assert;

import org.junit.Test;

public class CLSummarizerFloatTest {

	private static final float DELTA = 0;

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
		COUNT = clFloat.getMaxBufferItems();
		VALUE = (float) Math.PI;
		sum = COUNT * VALUE;
		for (int i = 0; i < COUNT; i++) {
			clFloat.put(VALUE);
		}
		assertArrayEquals(new float[] { sum },
				new float[] { clFloat.getSum() }, DELTA);
		
		clFloat.resetResult();
		
		// value test
		COUNT = clFloat.getMaxBufferItems() & 1024;
		VALUE = (float) Math.PI;
		sum = COUNT/2 * VALUE;
		VALUE = (float) Math.E;
		sum += COUNT/2 * VALUE;
		for (int i = 0; i < COUNT; i++) {
			if(i % 2 == 0)
				VALUE = (float) Math.PI;
			else
				VALUE = (float) Math.E;
			clFloat.put(VALUE);
		}
		assertArrayEquals(new float[] { sum },
				new float[] { clFloat.getSum() }, DELTA);
		
		clFloat.resetResult();

		// buffer overflow test
		COUNT = 2 * clFloat.getMaxBufferItems() + 13;
		VALUE = (float) Math.PI;
		sum = COUNT * VALUE;
		for (int i = 0; i < COUNT; i++) {
			clFloat.put(VALUE);
		}
		assertArrayEquals(new float[] { sum },
				new float[] { clFloat.getSum() }, DELTA);

		// intermediate result test
		clFloat.put(Float.valueOf(10));
		sum += 10;
		assertArrayEquals(new float[] { sum },
				new float[] { clFloat.getSum() }, DELTA);

		// reset test
		clFloat.put(Float.valueOf(3));
		clFloat.resetResult();

		// less buffer items test
		COUNT = clFloat.getMaxBufferItems() - 13;
		VALUE = (float) Math.E;
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
		VALUE = (float) Math.E;
		sum = COUNT * VALUE;
		clFloat.resetBuffer(COUNT);
		for (int i = 0; i < COUNT; i++) {
			clFloat.put(VALUE);
		}

		assertArrayEquals(new float[] { sum },
				new float[] { clFloat.getSum() }, DELTA);
		
		clFloat.resetResult();
		
		COUNT = 2 * CLInstance.WAVE_SIZE + 5;
		VALUE = (float) Math.E;
		sum = COUNT * VALUE;
		clFloat.resetBuffer(COUNT);
		for (int i = 0; i < COUNT; i++) {
			clFloat.put(VALUE);
		}

		assertArrayEquals(new float[] { sum },
				new float[] { clFloat.getSum() }, DELTA);
	}
	
	@Test
	public void testResetBuffer(){
		CLInstance clInstance = new CLInstance();
		clInstance.initialize(CLInstance.TYPES.CL_GPU);

		ICLBufferedOperation<Float> clBufferedOp = new CLSummarizerFloat(clInstance);
		clBufferedOp.resetBuffer();
		assertArrayEquals(new int[]{clBufferedOp.getMaxBufferItems()}, new int[]{clBufferedOp.getCurrentMaxBufferItems()});
		
		int items = 2 * clBufferedOp.getMaxBufferItems();
		clBufferedOp.resetBuffer(items);
		assertArrayEquals(new int[]{clBufferedOp.getMaxBufferItems()}, new int[]{clBufferedOp.getCurrentMaxBufferItems()});
		
		items = clBufferedOp.getMaxBufferItems() / 2;
		clBufferedOp.resetBuffer(items);
		int currItems = clBufferedOp.getCurrentMaxBufferItems();
		if(!(items <= currItems && currItems <= clBufferedOp.getMaxBufferItems()))
			Assert.fail();
		
		items = 0;
		clBufferedOp.resetBuffer(items);
		currItems = clBufferedOp.getCurrentMaxBufferItems();
		if(!(items < currItems && currItems <= clBufferedOp.getMaxBufferItems()))
			Assert.fail();
		
		items = -1;
		clBufferedOp.resetBuffer(items);
		currItems = clBufferedOp.getCurrentMaxBufferItems();
		if(!(items < currItems && currItems <= clBufferedOp.getMaxBufferItems()))
			Assert.fail();
	}

}
