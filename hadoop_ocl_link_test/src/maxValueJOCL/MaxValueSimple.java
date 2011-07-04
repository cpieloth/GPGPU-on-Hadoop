package maxValueJOCL;

import static org.jocl.CL.CL_MEM_COPY_HOST_PTR;
import static org.jocl.CL.CL_TRUE;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clEnqueueReadBuffer;
import static org.jocl.CL.clReleaseMemObject;
import static org.jocl.CL.clWaitForEvents;
import lightLogger.Logger;

import org.jocl.CL;
import org.jocl.CLException;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;

public class MaxValueSimple extends MaxValueAbstract {

	private static final Class<MaxValueSimple> CLAZZ = MaxValueSimple.class;

	public MaxValueSimple(String[] args) {
		super(args);
	}

	public MaxValueSimple() {
		super();
	}

	public static void main(String[] args) {
		MaxValueAbstract maxVal = new MaxValueSimple(args);
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
		try {
			// Prepate Data
			Pointer vPointer = Pointer.to(values);
			cl_mem vBuffer = clCreateBuffer(context, CL.CL_MEM_READ_WRITE
					| CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * values.length,
					vPointer, null);

			for (cl_command_queue cq : cmdQ)
				CL.clFinish(cq);

			long info[] = new long[numDevices];
			CL.clGetDeviceInfo(devices[0], CL.CL_DEVICE_MAX_WORK_GROUP_SIZE,
					Sizeof.cl_long * numDevices, Pointer.to(info), null);

			final long MAX_GROUP_SIZE = info[0];
			int size = values.length;
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

}
