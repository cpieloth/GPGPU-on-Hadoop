package maxValue;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.nativelibs4java.opencl.CLDevice;

public class MaxValueSimpleTest {

	private final char SINGLE = 's';
	private final char CPU = 'c';
	private final char GPU = 'g';

	private int START = 64;
	private int END;
	private int STEP;

	private MaxValueAbstract maxVal;

	@Before
	public void initialize() {
		this.maxVal = new MaxValueSimple();
		this.maxVal.LEN = 3000000;
		this.END = this.maxVal.LEN;
		this.STEP = this.END / 32;
	}

	@Test
	public void testMaxValueOCLGPU() {
		boolean success = this.testMaxValueIntArry(GPU, START, END, STEP);
		assertTrue(success);
	}

	@Test
	public void testMaxValueOCLCPU() {
		boolean success = this.testMaxValueIntArry(CPU, START, END, STEP);
		assertTrue(success);
	}

	@Test
	public void testMaxValue() {
		boolean success = this.testMaxValueIntArry(SINGLE, START, END, STEP);
		assertTrue(success);
	}

	private boolean testMaxValueIntArry(final char TYPE, final int START,
			final int END, final int STEP) {
		boolean success = true;
		int[] values;

		/*** Testwerte setzen ***/
		int max_pos = -1;
		int max_val = -1;
		int max = MaxValueAbstract.MAX_FAILURE;

		for (int i = START; i < END; i += STEP) {
			values = this.maxVal.prepareData(i);
			max_pos = this.maxVal.setTestValues(values);
			max_val = values[max_pos];

			switch (TYPE) {
			case SINGLE:
				max = this.maxVal.maxValue(values);
				break;
			case GPU:
				max = this.maxVal.maxValue(CLDevice.Type.GPU, values);
				break;
			case CPU:
				max = this.maxVal.maxValue(CLDevice.Type.CPU, values);
				break;
			default:
				return false;
			}

			success = values[0] == max_val && max == max_val
					&& max > MaxValueAbstract.MAX_FAILURE;

			System.out.println("TEST: " + (success ? "PASSED" : "FAILED")
					+ "; max_pos: " + max_pos + "; max_val: " + max_val
					+ "; values[0]: " + values[0] + "; TYPE: " + TYPE);

			if (!success)
				break;
		}

		return success;
	}

}
