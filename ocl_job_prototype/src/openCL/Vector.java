package openCL;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Scanner;

import lightLogger.Level;
import lightLogger.Logger;
import util.Convert;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLBuildException;
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

public class Vector {

	private static final Class<?> CLAZZ = Vector.class;

	/*** Globale Variablen ***/
	public static final boolean EXIT_FAILURE = false; // 1;
	public static final boolean EXIT_SUCCESS = true;// 0;

	// private static final Level TIME = new Level(32, "TIME");

	private static final String KERNEL_PATH = "kernel.cl";
	// private static final String KERNEL_SRC =
	// "__kernel void addVec(__global int* vecC, const __global int* vecA, const __global int* vecB, const unsigned int size) { unsigned int w = get_global_id(0); if(w >= size) return; vecC[w] = vecA[w] + vecB[w]; }";

	private static CLPlatform[] platforms;
	private static ArrayList<CLDevice> devices;
	private static CLContext context;
	private static CLProgram program;
	private static CLKernel kernel;
	private static CLQueue cmdQ;

	public synchronized static boolean addVec(CLDevice.Type clType, int[] vecC,
			int[] vecA, int[] vecB) {

		try {
			if ((Logger.getLogMask() & Level.DEFAULT.DEBUG.getLevel()
					.getValue()) == Level.DEFAULT.DEBUG.getLevel().getValue()) {
				Logger.logDebug(CLAZZ,
						"addVec() vecA: " + Convert.toString(vecA));
				Logger.logDebug(CLAZZ,
						"addVec() vecB: " + Convert.toString(vecB));
			}

			/*** Initialisiere OpenCL-Objekte ***/
			initCL(clType);

			/*** Ausgabe von Informationen ueber gewaehltes OpenCL-Device ***/
			Logger.logInfo(CLAZZ, "max compute units: "
					+ devices.get(0).getMaxComputeUnits());
			Logger.logInfo(CLAZZ, "max work group sizes: "
					+ devices.get(0).getMaxWorkGroupSize());
			Logger.logInfo(CLAZZ, "max global mem size (KB): "
					+ devices.get(0).getGlobalMemSize() / 1024);
			Logger.logInfo(CLAZZ, "max local mem size (KB): "
					+ devices.get(0).getLocalMemSize() / 1024);

			/*** Erstellen und Vorbereiten der Daten ***/
			IntBuffer tmpBuffer = ByteBuffer
					.allocateDirect(vecA.length * Integer.SIZE)
					.order(context.getByteOrder()).asIntBuffer();

			tmpBuffer.put(vecA);
			CLBuffer<IntBuffer> aBuffer = context.createBuffer(
					CLMem.Usage.Input, tmpBuffer, true);

			tmpBuffer.clear();
			tmpBuffer.put(vecB);
			CLBuffer<IntBuffer> bBuffer = context.createBuffer(
					CLMem.Usage.Input, tmpBuffer, true);

			CLBuffer<IntBuffer> cBuffer = context.createBuffer(
					CLMem.Usage.Output, vecC.length, IntBuffer.class);

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
			
			if ((Logger.getLogMask() & Level.DEFAULT.DEBUG.getLevel()
					.getValue()) == Level.DEFAULT.DEBUG.getLevel().getValue()) {
				Logger.logDebug(CLAZZ,
						"addVec() vecC: " + Convert.toString(vecC));
			}
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

	private static void initCL(CLDevice.Type clType) throws Exception {
		/*** Hole OpenCL-Plattformen z.B. AMD APP, NVIDIA CUDA ***/
		platforms = JavaCL.listPlatforms();

		/*** Hole OpenCL-Device des geforderten Typs z.B. GPU, CPU ***/
		EnumSet<CLDevice.Type> types = EnumSet.of(clType);
		devices = new ArrayList<CLDevice>();
		CLDevice[] devTmp;

		for (CLPlatform platform : platforms) {
			devTmp = platform.listDevices(types, true);
			devices.addAll(Arrays.asList(devTmp));
		}

		/*** Erstelle OpenCL-Context und CommandQueue ***/
		devTmp = new CLDevice[devices.size()];
		context = JavaCL.createContext(null, devices.toArray(devTmp));
		cmdQ = context.createDefaultQueue(QueueProperties.ProfilingEnable);

		/*** OpenCL-Quellcode einlesen ***/
		String src = readFile(KERNEL_PATH);
		// String src = KERNEL_SRC;

		/*** OpenCL-Programm aus Quellcode erstellen ***/
		program = context.createProgram(src);

		try {
			program.build();
		} catch (CLBuildException err) {
			Logger.logError(CLAZZ, "Build log for \"" + devices.get(0) + "\n"
					+ err.getMessage());
			throw err;
		}

		/*** OpenCL-Kernel laden ***/
		kernel = program.createKernel("addVec");
	}

	private static String readFile(String fName) {
		StringBuffer sb = new StringBuffer();
		try {
			// Scanner sc = new Scanner(new File(fName));
			Scanner sc = new Scanner(CLAZZ.getResourceAsStream(fName));
			while (sc.hasNext())
				sb.append(sc.nextLine());
			sc.close();

		} catch (Exception e) {
			Logger.logError(CLAZZ, "Could not read file: " + fName);
			e.printStackTrace();
		}
		return sb.toString();
	}

}
