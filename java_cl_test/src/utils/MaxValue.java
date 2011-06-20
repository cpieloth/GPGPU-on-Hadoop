package utils;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Scanner;

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

// TODO fertig machen, kernel fehler
// TODO Im Moment wird das ganze Array vom OCL Device zurueckkopiert. Aendern zu nur einem Rueckgabewert.

public class MaxValue {

	private static final Class<MaxValue> CLAZZ = MaxValue.class;

	/*** Kommandozeilenargumente ***/
	private static final char CPU = 's';
	private static final char CLCPU = 'c';
	private static final char CLGPU = 'g';

	/*** Globale Variablen ***/
	private static int LEN;
	private static char TYPE;
	private static final int EXIT_FAILURE = 1;
	private static final int EXIT_SUCCESS = 0;

	private static final String KERNEL_PATH = "src/kernel.cl";
	private static final int WORK_GROUP_SIZE = 64;

	private static int maxValue(CLDevice.Type clType, int[] values) {
		try {
			/*** Hole OpenCL-Plattformen z.B. AMD APP, NVIDIA CUDA ***/
			CLPlatform[] platforms = JavaCL.listPlatforms();

			/*** Hole OpenCL-Device des geforderten Typs z.B. GPU, CPU ***/
			EnumSet<CLDevice.Type> types = EnumSet.of(clType);
			ArrayList<CLDevice> devices = new ArrayList<CLDevice>();
			CLDevice[] devTmp;

			for (CLPlatform platform : platforms) {
				devTmp = platform.listDevices(types, true);
				devices.addAll(Arrays.asList(devTmp));
			}

			/*** Erstelle OpenCL-Context und CommandQueue ***/
			devTmp = new CLDevice[devices.size()];
			CLContext context = JavaCL.createContext(null,
					devices.toArray(devTmp));
			CLQueue cmdQ = context
					.createDefaultQueue(QueueProperties.ProfilingEnable);

			/*** OpenCL-Quellcode einlesen ***/
			String src = readFile(KERNEL_PATH);

			/*** OpenCL-Programm aus Quellcode erstellen ***/
			CLProgram program = context.createProgram(src);

			try {
				program.build();
			} catch (Exception err) {
				Logger.logError(CLAZZ, "Build log for \"" + devices.get(0)
						+ "\n" + err.getMessage());
				return EXIT_FAILURE;
			}

			/*** OpenCL-Kernel laden ***/
			CLKernel kernel = program.createKernel("maxInt");

			/*** Erstellen und Vorbereiten der Daten ***/
			CLBuffer<IntBuffer> valBuffer = context.createBuffer(
					CLMem.Usage.InputOutput, IntBuffer.wrap(values), true);

			/*** Kernel-Argumente setzen ***/
			kernel.setArg(0, valBuffer);
			kernel.setArg(1, values.length);
			// TODO dyn. berechnen
			kernel.setArg(2, values.length / 64);
			// FIXME
			// kernel.setArg(3, WORK_GROUP_SIZE);

			/*** Kernel ausfuehren und auf Abarbeitung warten ***/
			CLEvent event = kernel.enqueueNDRange(cmdQ,
					new int[] { values.length }, new int[] { WORK_GROUP_SIZE }, new CLEvent[0]);
			event.waitFor();
			cmdQ.finish();

			/*** Daten vom OpenCL-Device holen ***/
			IntBuffer tmpBuffer = ByteBuffer
					.allocateDirect(values.length * Integer.SIZE)
					.order(context.getByteOrder()).asIntBuffer();
			valBuffer.read(cmdQ, tmpBuffer, true, new CLEvent[0]);
			tmpBuffer.clear();
			tmpBuffer.get(values);

		} catch (CLException err) {
			Logger.logError(CLAZZ, "OpenCL error:\n" + err.getMessage() + "():"
					+ err.getCode());
			err.printStackTrace();
			return EXIT_FAILURE;
		} catch (Exception err) {
			Logger.logError(CLAZZ, "Error:\n" + err.getMessage() + "()");
			err.printStackTrace();
			return EXIT_FAILURE;
		}

		return EXIT_SUCCESS;
	}

	public static void main(String[] args) {
		if (!checkArguments(args))
			System.exit(EXIT_FAILURE);

		int success = EXIT_FAILURE;

		Logger.logInfo(CLAZZ, "Device type: " + TYPE);
		Logger.logInfo(CLAZZ, "Vector size: " + LEN);

		/*** Erzeugen der Vektoren ***/
		int[] values = new int[LEN];

		fill(values);

		/*** Implementierung waehlen ***/
		switch (TYPE) {
		case CPU:
			success = maxValue(values);
			break;
		case CLGPU:
			success = maxValue(CLDevice.Type.GPU, values);
			break;
		case CLCPU:
			success = maxValue(CLDevice.Type.CPU, values);
			break;
		default:
			Logger.logError(CLAZZ, "Device type not supported!");
		}

		if (success == EXIT_SUCCESS) {
			for(int i = 0; i < 5; i++)
				Logger.logInfo(CLAZZ, "Maximum: " + values[i]);
			System.exit(EXIT_SUCCESS);
		} else {
			Logger.logError(CLAZZ, "Error, no result!");
			System.exit(EXIT_FAILURE);
		}
	}

	private static int maxValue(int[] values) {
		int max = Integer.MIN_VALUE;
		for (int v : values)
			max = Math.max(v, max);
		values[0] = max;
		return EXIT_SUCCESS;
	}

	private static boolean checkArguments(String[] args) {
		if (args.length < 2) {
			System.out.println("Argumente: " + CLCPU + "|" + CLGPU + "|" + CPU
					+ " <Vektorgroesze>");
			return false;
		}

		TYPE = args[0].charAt(0);
		LEN = Integer.valueOf(args[1]).intValue();

		return true;
	}

	private static String readFile(String fName) {
		StringBuffer sb = new StringBuffer();
		try {
			Scanner sc = new Scanner(new File(fName));
			while (sc.hasNext())
				sb.append(sc.nextLine());

		} catch (Exception e) {
			Logger.logError(CLAZZ, "Could not read file: " + fName);
			e.printStackTrace();
		}
		return sb.toString();
	}

	private static void fill(int[] vec) {
		for (int i = 0; i < vec.length; i++)
			vec[i] = i % 1000;
	}

}
