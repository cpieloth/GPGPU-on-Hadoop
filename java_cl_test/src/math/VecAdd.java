package math;

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

public class VecAdd {

	/*** Kommandozeilenargumente ***/
	private static final char CPU = 'c';
	private static final char GPU = 'g';

	/*** Globale Variablen ***/
	private static int LEN;
	private static char TYPE;
	private static final int EXIT_FAILURE = 1;
	private static final int EXIT_SUCCESS = 0;

	private static final String KERNEL_PATH = "kernel.cl";

	private static int addVec(CLDevice.Type clType, int[] vecC, int[] vecA,
			int[] vecB) {
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
				Logger.logError(VecAdd.class,
						"Build log for \"" + devices.get(0) + "\n" + err.getMessage());
				return EXIT_FAILURE;
			}

			/*** OpenCL-Kernel laden ***/
			CLKernel kernel = program.createKernel("addVec");

			/*** Erstellen und Vorbereiten der Daten ***/
			IntBuffer tmpBuffer = ByteBuffer
					.allocateDirect(vecA.length * Integer.SIZE)
					.order(context.getByteOrder()).asIntBuffer();

			tmpBuffer.put(vecA);
			CLBuffer<IntBuffer> aBuffer = context.createBuffer(
					CLMem.Usage.Input, tmpBuffer, true);

			tmpBuffer.clear();
			tmpBuffer.put(vecB);
			CLBuffer<Integer> bBuffer = context.createBuffer(
					CLMem.Usage.Input, tmpBuffer, true);

			CLBuffer<Integer> cBuffer = context.createIntBuffer(
					CLMem.Usage.Output, vecC.length);

			/*** Kernel-Argumente setzen ***/
			kernel.setArg(0, cBuffer);
			kernel.setArg(1, aBuffer);
			kernel.setArg(2, bBuffer);
			kernel.setArg(3, vecC.length);

			/*** Kernel ausfuehren und auf Abarbeitung warten ***/
			CLEvent event = kernel.enqueueNDRange(cmdQ,
					new int[] { vecC.length }, new CLEvent[0]);
			event.waitFor();
			cmdQ.finish();

			/*** Daten vom OpenCL-Device holen ***/
			cBuffer.read(cmdQ, tmpBuffer, true, new CLEvent[0]);
			tmpBuffer.clear();
			tmpBuffer.get(vecC);

			Logger.logDebug(VecAdd.class,
					"tmpBuffer.get(vecC.length-1) = " + tmpBuffer.get(vecC.length-1));
			Logger.logDebug(VecAdd.class, "vecC[vecC.length-1] = " + vecC[vecC.length-1]);
		} catch (CLException err) {
			Logger.logError(VecAdd.class, "OpenCL error:\n" + err.getMessage()
					+ "():" + err.getCode());
			err.printStackTrace();
			return EXIT_FAILURE;
		} catch (Exception err) {
			Logger.logError(VecAdd.class, "Error:\n" + err.getMessage() + "()");
			err.printStackTrace();
			return EXIT_FAILURE;
		}

		return EXIT_SUCCESS;
	}

	public static void main(String[] args) {
		if (!checkArguments(args))
			System.exit(EXIT_FAILURE);

		int success = EXIT_FAILURE;

		Logger.logInfo(VecAdd.class, "Device type: " + TYPE);
		Logger.logInfo(VecAdd.class, "Vector size: " + LEN);

		/*** Erzeugen der Vektoren ***/
		int[] vecA = new int[LEN];
		int[] vecB = new int[LEN];
		int[] vecC = new int[LEN];

		fill(vecA);
		fill(vecB);

		/*** Implementierung waehlen ***/
		switch (TYPE) {
		case GPU:
			success = addVec(CLDevice.Type.GPU, vecC, vecA, vecB);
			break;
		case CPU:
			success = addVec(CLDevice.Type.CPU, vecC, vecA, vecB);
			break;
		default:
			Logger.logError(VecAdd.class, "Device type not supported!");
		}

		if (success == EXIT_SUCCESS) {
			if (LEN < 80) {
				StringBuilder sb = new StringBuilder();
				sb.append("\n<");
				for (int i = 0; i < vecC.length; ++i)
					sb.append(vecC[i] + ",");
				sb.setCharAt(sb.length() - 1, '>');
				Logger.logInfo(VecAdd.class, sb.toString());
			}
			System.exit(EXIT_SUCCESS);
		} else {
			Logger.logError(VecAdd.class, "Error, no result!");
			System.exit(EXIT_FAILURE);
		}
	}

	private static boolean checkArguments(String[] args) {
		if (args.length < 2) {
			System.out.println("Argumente: " + CPU + "|" + GPU
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
			Logger.logError(VecAdd.class, "Could not read file: " + fName);
			e.printStackTrace();
		}
		return sb.toString();
	}

	private static void fill(int[] vec) {
		for (int i = 0; i < vec.length; i++)
			vec[i] = i % 1000;
	}

}
