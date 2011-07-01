package maxValue;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;

import lightLogger.Logger;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLDevice.QueueProperties;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLException;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;

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
	public int maxValue(CLDevice.Type type, int[] values) {
		CLPlatform[] platforms;
		ArrayList<CLDevice> devices;
		CLContext context;
		CLQueue cmdQ;
		CLProgram program;
		CLKernel kernel;

		try {
			// Init OpenCL
			platforms = JavaCL.listPlatforms();

			EnumSet<CLDevice.Type> types = EnumSet.of(type);
			devices = new ArrayList<CLDevice>();
			CLDevice[] devTmp;

			for (CLPlatform platform : platforms) {
				devTmp = platform.listDevices(types, true);
				devices.addAll(Arrays.asList(devTmp));
			}

			devTmp = new CLDevice[1];
			devTmp[0] = devices.get(0);
			context = JavaCL.createContext(null, devTmp);
			cmdQ = context.createDefaultQueue(QueueProperties.ProfilingEnable);

			String src = readFile(KERNEL_PATH);

			program = context.createProgram(src);

			try {
				program.build();
			} catch (Exception err) {
				Logger.logError(CLAZZ, "Build log for \"" + devices.get(0)
						+ "\n" + err.getMessage());
				return EXIT_FAILURE;
			}

			kernel = program.createKernel("maxInt");

			// Prepate Data
			CLBuffer<IntBuffer> vBuffer = context.createBuffer(
					CLMem.Usage.InputOutput,
					IntBuffer.wrap(values, 0, values.length), true);

			cmdQ.finish();

			final long MAX_GROUP_SIZE = devices.get(0).getMaxWorkGroupSize();
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

}
