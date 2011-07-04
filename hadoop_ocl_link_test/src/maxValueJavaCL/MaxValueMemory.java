package maxValueJavaCL;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

import lightLogger.Logger;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLException;
import com.nativelibs4java.opencl.CLMem;

public class MaxValueMemory extends MaxValueAbstract {

	private static final Class<MaxValueMemory> CLAZZ = MaxValueMemory.class;

	// max memory of opencl device, MAX_VALUE % 64 = 0
	private final static int MAX_VALUES = 65536;

	public MaxValueMemory(String[] args) {
		super(args);
	}

	public MaxValueMemory() {
		super();
	}

	public static void main(String[] args) {
		MaxValueAbstract maxVal = new MaxValueMemory(args);

		if (maxVal.LEN < 1)
			System.exit(EXIT_FAILURE);

		Logger.logInfo(CLAZZ, "Device type: " + maxVal.TYPE);
		Logger.logInfo(CLAZZ, "Vector size: " + maxVal.LEN);

		/* Erzeugen der Vektoren */
		int[] values = maxVal.prepareData(maxVal.LEN);
		int max_pos = maxVal.setTestValues(values);

		Logger.logDebug(CLAZZ, "max_pos: " + max_pos + "; values[max_pos]: "
				+ values[max_pos]);

		/* Implementierung waehlen */
		int max = maxVal.maxValue(values);

		if (max > MAX_FAILURE) {
			for (int i = 0; i < 5; i++)
				Logger.logInfo(CLAZZ, "values[" + i + "]: " + values[i]);
			System.exit(EXIT_SUCCESS);
		} else {
			Logger.logError(CLAZZ, "Error, no result!");
			System.exit(EXIT_FAILURE);
		}
	}

	@Override
	protected int maxValue(CLDevice.Type type, int[] values) {
		int[] tmpValues = new int[MAX_VALUES];
		tmpValues[0] = MAX_FAILURE; // temporary max temp
		int i = 1;

		Iterator<Integer> it = new IntArrayIterator(values);
		while (it.hasNext()) {
			// fill array to copy on opencl device, mind max memory of device!
			if (i < MAX_VALUES) {
				tmpValues[i++] = it.next().intValue();
			}
			// if max values or no more values to add, start opencl kernel
			if (i >= MAX_VALUES || !it.hasNext()) {
				tmpValues[0] = maxIntOCL(type, tmpValues, i);
				i = 1;
			}
		}

		values[0] = tmpValues[0];
		return values[0];
	}

	private int maxIntOCL(CLDevice.Type type, int[] values, int size) {
		// calculate optimal length
		int offset = size;
		if (size < values.length && size % WG_FAC != 0) {
			size = (int) Math.ceil((double) size / WG_FAC) * WG_FAC;
			size = size < values.length ? size : values.length;
			Logger.logDebug(CLAZZ, "resized to optimal start size " + size);
		}
		// fill end of array with minimum
		for (int i = offset; i < size; i++)
			values[i] = MAX_FAILURE;

		try {
			// Prepate Data
			CLBuffer<IntBuffer> vBuffer = context.createBuffer(
					CLMem.Usage.InputOutput, IntBuffer.wrap(values, 0, size),
					true);

			cmdQ.finish();

			final long MAX_GROUP_SIZE = devices.get(0).getMaxWorkGroupSize();
			int globalSize;
			int localSize;

			do {
				globalSize = size;
				localSize = calcWorkGroupSize(globalSize, MAX_GROUP_SIZE);
				Logger.logDebug(CLAZZ, "GlobalSize: " + globalSize);
				Logger.logDebug(CLAZZ, "LocalSize: " + localSize);
				if (localSize == 1) {
					globalSize = (int) (Math.ceil((double) size / WG_FAC) * WG_FAC);
					localSize = calcWorkGroupSize(globalSize, MAX_GROUP_SIZE);
					Logger.logDebug(CLAZZ, "GlobalSize has been extended to "
							+ globalSize);
					Logger.logDebug(CLAZZ, "LocalSize has been changed to "
							+ localSize);
				}

				kernel.setArg(0, vBuffer);
				kernel.setLocalArg(1, localSize * SIZEOF_CL_INT);

				// Run kernel
				// CLEvent event =
				kernel.enqueueNDRange(cmdQ, new int[] { globalSize },
						new int[] { localSize }, new CLEvent[0]);

				cmdQ.finish();

				size = globalSize / localSize;
			} while (globalSize > localSize && localSize > 1);

			// Get results - first value in array
			IntBuffer resBuffer = ByteBuffer.allocateDirect(1 * SIZEOF_CL_INT)
					.order(context.getByteOrder()).asIntBuffer();
			vBuffer.read(cmdQ, 0, 1, resBuffer, true, new CLEvent[0]);
			resBuffer.rewind();
			values[0] = resBuffer.get(0);

			return values[0];
		} catch (CLException err) {
			Logger.logError(CLAZZ, "OpenCL error:\n" + err.getMessage() + "():"
					+ err.getCode());
			err.printStackTrace();
			return MAX_FAILURE;
		} catch (Exception err) {
			Logger.logError(CLAZZ, "Error:\n" + err.getMessage() + "()");
			err.printStackTrace();
			return MAX_FAILURE;
		}
	}

	private static class IntArrayIterator implements Iterator<Integer> {

		private int[] values;
		private int i = 0;

		public IntArrayIterator(int[] values) {
			this.values = values;
		}

		@Override
		public boolean hasNext() {
			return i < values.length;
		}

		@Override
		public Integer next() {
			if (this.hasNext()) {
				int tmp = values[i++];
				return new Integer(tmp);
			} else
				throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			// TODO Auto-generated method stub

		}

	}

}
