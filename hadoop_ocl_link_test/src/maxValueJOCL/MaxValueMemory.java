package maxValueJOCL;

import static org.jocl.CL.CL_MEM_COPY_HOST_PTR;
import static org.jocl.CL.CL_TRUE;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clEnqueueReadBuffer;
import static org.jocl.CL.clReleaseMemObject;
import static org.jocl.CL.clWaitForEvents;

import java.util.Iterator;
import java.util.NoSuchElementException;

import lightLogger.Logger;

import org.jocl.CL;
import org.jocl.CLException;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;

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
		maxVal.initialize();

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
		
		maxVal.finalize();

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
	protected int maxValue(long type, int[] values) {
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

	private int maxIntOCL(long type, int[] values, int size) {
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
			Pointer vPointer = Pointer.to(values);
			cl_mem vBuffer = clCreateBuffer(context, CL.CL_MEM_READ_WRITE
					| CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * values.length,
					vPointer, null);

			for(cl_command_queue cq : cmdQ)
				CL.clFinish(cq);

			long info[] = new long[numDevices];
			CL.clGetDeviceInfo(devices[0], CL.CL_DEVICE_MAX_WORK_GROUP_SIZE, Sizeof.cl_long * numDevices, Pointer.to(info), null);

			final long MAX_GROUP_SIZE = info[0];
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

				for (cl_kernel kernel : kernels) {
					CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem,
							Pointer.to(vBuffer));
					CL.clSetKernelArg(kernel, 1, localSize * Sizeof.cl_int,
							null);
				}

				// Run kernel
				for (int i = 0; i < numDevices; i++) {
					clEnqueueNDRangeKernel(cmdQ[i], kernels[i], 1, null,
							new long[] { globalSize },
							new long[] { localSize }, 0, null, events[i]);
				}

				clWaitForEvents(events.length, events);

				for (cl_command_queue cq : cmdQ)
					CL.clFinish(cq);

				size = globalSize / localSize;
			} while (globalSize > localSize && localSize > 1);

			// Get results - first value in array
			// FIXME if more than 1 device, vBuffer will be overwritten
			for (cl_command_queue cq : cmdQ) {
				clEnqueueReadBuffer(cq, vBuffer, CL_TRUE, 0, Sizeof.cl_int * 1,
						vPointer, 0, null, null);
			}

			clReleaseMemObject(vBuffer);

			return values[0];
		} catch (CLException err) {
			Logger.logError(CLAZZ, "OpenCL error:\n" + err.getMessage() + "():"
					+ err.getCause());
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
