package math;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Scanner;

import lightLogger.Level;
import lightLogger.Logger;

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

public class MatMul {

	/*** Kommandozeilenargumente ***/
	private static final char SINGLE = 's';
	private static final char CPU = 'c';
	private static final char GPU = 'g';

	private static final char ALL = 'a';
	private static final char NORMAL = 'n';
	private static final char ERRORL = 'e';
	private static final char TIMEL = 't';

	/*** Globale Variablen ***/
	private static final int EXIT_FAILURE = 1;
	private static final int EXIT_SUCCESS = 0;
	private static int m, n;
	private static char type, log;
	private static final Level TIME = new Level(32, "TIME");

	private static final String KERNEL_PATH = "kernel.cl";

	private static CLPlatform[] platforms;
	private static ArrayList<CLDevice> devices;
	private static CLContext context;
	private static CLProgram program;
	private static CLKernel kernel;
	private static CLQueue cmdQ;

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

		/*** OpenCL-Programm aus Quellcode erstellen ***/
		program = context.createProgram(src);

		try {
			program.build();
		} catch (CLBuildException err) {
			Logger.logError(VecAdd.class, "Build log for \"" + devices.get(0)
					+ "\n" + err.getMessage());
			throw err;
		}

		/*** OpenCL-Kernel laden ***/
		kernel = program.createKernel("matMulSingle");
	}

	private static double matMulCL(CLDevice.Type clType, MatrixInt result,
			MatrixInt left, MatrixInt right) {
		Class<?> METHOD = MatMul.class;
		Timer timer = new Timer();

		try {

			/*** Initialisiere OpenCL-Objekte ***/
			initCL(clType);

			/*** Ausgabe von Informationen ueber gewaehltes OpenCL-Device ***/
			Logger.logInfo(METHOD, "max compute units: "
					+ devices.get(0).getMaxComputeUnits());
			Logger.logInfo(METHOD, "max work group sizes: "
					+ devices.get(0).getMaxWorkGroupSize());
			Logger.logInfo(METHOD, "max global mem size (KB): "
					+ devices.get(0).getGlobalMemSize() / 1024);
			Logger.logInfo(METHOD, "max local mem size (KB): "
					+ devices.get(0).getLocalMemSize() / 1024);

			/*** Erstellen und Vorbereiten der Daten ***/
			timer.start();

			long globalSize = result.rows * result.cols;
			long localSize = devices.get(0).getMaxWorkGroupSize();
			if (globalSize < localSize)
				localSize = globalSize;
			else
				while (globalSize % localSize != 0)
					--localSize;

			// Bei Laufzeitmessung auskommentieren
			// Logger::logDebug(METHOD, Logger::sStream << "localSize=" <<
			// localSize);
			// Logger::logDebug(METHOD, Logger::sStream << "globalSize=" <<
			// globalSize);

			IntBuffer tmpBuffer = ByteBuffer
					.allocateDirect(left.rows * left.cols * Integer.SIZE)
					.order(context.getByteOrder()).asIntBuffer();
			tmpBuffer.put(left.elements);
			CLBuffer<IntBuffer> leftBuffer = context.createBuffer(
					CLMem.Usage.Input, tmpBuffer, true);

			tmpBuffer.clear();
			tmpBuffer.put(right.elements);
			CLBuffer<Integer> rightBuffer = context.createIntBuffer(
					CLMem.Usage.Input, tmpBuffer, true);

			tmpBuffer.clear();
			CLBuffer<Integer> resultBuffer = context
					.createIntBuffer(CLMem.Usage.Output, result.elements.length);

			/*** Kernel-Argumente setzen ***/
			kernel.setArg(0, resultBuffer);
			kernel.setArg(1, result.rows);
			kernel.setArg(2, result.cols);
			kernel.setArg(3, leftBuffer);
			kernel.setArg(4, left.rows);
			kernel.setArg(5, left.cols);
			kernel.setArg(6, rightBuffer);
			kernel.setArg(7, right.rows);
			kernel.setArg(8, right.cols);

			/*** Kernel ausfuehren und auf Abarbeitung warten ***/
			// Anzahl der Work-Items = globalSize
			// Work-Items pro Work-Group = localSize
			CLEvent event = kernel.enqueueNDRange(cmdQ,
					new int[] { result.elements.length }, new CLEvent[0]);
			event.waitFor();
			cmdQ.finish();

			/*** Daten vom OpenCL-Device holen ***/
			resultBuffer.read(cmdQ, tmpBuffer, true, new CLEvent[0]);
			tmpBuffer.clear();
			tmpBuffer.get(result.elements);

			timer.stop();

			double runtimeKernel = 0;
			runtimeKernel += event.getProfilingCommandEnd();
			runtimeKernel -= event.getProfilingCommandStart();
			Logger.log(TIME, METHOD, "timeKernel=" + 1.0e-9 * runtimeKernel
					+ ";");
		} catch (CLException err) {
			Logger.logError(VecAdd.class, "OpenCL error:\n" + err.getMessage()
					+ "():" + err.getCode());
			err.printStackTrace();
		} catch (Exception err) {
			Logger.logError(VecAdd.class, "Error:\n" + err.getMessage() + "()");
			err.printStackTrace();
		}

		return timer.getTimeInSeconds();
	}

	public static void main(String[] args) {
		if (!checkArguments(args))
			System.exit(EXIT_FAILURE);

		Class<?> METHOD = MatMul.class;
		double runtime = 0;

		Logger.logInfo(METHOD, "type = " + type);
		Logger.logInfo(METHOD, "m = " + m);
		Logger.logInfo(METHOD, "n = " + n);
		Logger.logInfo(METHOD, "RAM (KB) > "
				+ ((2 * m * n + m * m) * Integer.SIZE) / 1024);

		/*** Erzeugen der Matrizen ***/
		MatrixInt left = new MatrixInt(m, n);
		MatrixInt right = new MatrixInt(n, m);
		MatrixInt result = new MatrixInt(left.rows, right.cols);

		if (!isMultipliable(result, left, right)) {
			Logger.logError(METHOD,
					"Multiplikation nicht moeglich (Zeilen-/ Spaltenanzahl)!");
			System.exit(EXIT_FAILURE);
		}

		if (left.elements == null || right.elements == null
				|| result.elements == null) {
			Logger.logError(METHOD, "left|right|result == NULL");
			System.exit(EXIT_FAILURE);
		}

		fillMatrix(left);
		fillMatrix(right);

		// ACHTUNG getMatrixString wuerde immer berechnet werden! (bad_alloc
		// moeglich)
		if (result.rows < 10 && result.cols < 10) {
			Logger.logDebug(METHOD, "Linke Matrix" + "\n" + left);
			Logger.logDebug(METHOD, "Rechte Matrix" + "\n" + right);
		}

		/*** Implementierung waehlen ***/
		switch (type) {
		case SINGLE:
			// runtime = matMul(result, left, right);
			Logger.logError(METHOD, "Not yet implemented!");
			break;
		case GPU:
			runtime = matMulCL(CLDevice.Type.GPU, result, left, right);
			break;
		case CPU:
			runtime = matMulCL(CLDevice.Type.CPU, result, left, right);
			break;
		default:
			Logger.logError(METHOD, "Falsches Argument \"" + type + "\"");
		}

		// ACHTUNG getMatrixString wuerde immer berechnet werden! (bad_alloc
		// moeglich)
		if (runtime > -1 && (result.rows < 10 && result.cols < 10))
			Logger.logDebug(METHOD, "Ergebnis\n" + result);
		Logger.log(TIME, METHOD, "time=" + runtime + ";");

		System.exit(EXIT_SUCCESS);
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

	@SuppressWarnings("serial")
	private static boolean checkArguments(String[] args) {
		if (args.length < 4) {
			System.out.println("Argumente: " + ALL + "|" + NORMAL + "|" + TIMEL
					+ "|" + ERRORL + " " + SINGLE + "|" + CPU + "|" + GPU
					+ " <m Zeilen> <n Spalten>");
			return false;
		}

		log = args[0].charAt(0);
		type = args[1].charAt(0);
		m = Integer.valueOf(args[2]);
		n = Integer.valueOf(args[3]);

		switch (log) {
		case ALL:
			Logger.setLogMask(Level.DEFAULT.ALL.getLevel().getValue());
			break;
		case NORMAL:
			Logger.setLogMask(7);
			break;
		case TIMEL:
			Logger.setLogMask(new HashSet<Level>() {
				{
					add(TIME);
				}
			});
			break;
		case ERRORL:
			Logger.setLogMask(Level.DEFAULT.ERROR.getLevel().getValue());
			break;
		default:
			Logger.setLogMask(7);
		}

		return true;
	}

	private static class MatrixInt {

		public int rows, cols;
		public int[] elements;

		public MatrixInt(int rows, int cols) {
			this.cols = cols;
			this.rows = rows;
			this.elements = new int[rows * cols];
		}

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			for (int r = 0; r < this.rows; ++r) {
				for (int c = 0; c < this.cols; ++c) {
					s.append(this.elements[r * this.cols + c] + " ");
				}
				s.append('\n');
			}
			return s.substring(0, s.length() - 1);
		}
	}

	private static boolean isMultipliable(MatrixInt result, MatrixInt left,
			MatrixInt right) {
		return (left.cols == right.rows);
	}

	private static void fillMatrix(MatrixInt mat) {
		for (int i = 0; i < mat.cols * mat.rows; ++i) {
			mat.elements[i] = (int) (Math.round(Math.random() * 2048) % 1024);
		}
	}
	
	private static class Timer {
		private boolean hasStarted;
		private boolean isStopped;
		
		private long startMs, endMs;
		
		public Timer() {
			this.reset();
		}
		
		public void reset() {
			this.hasStarted = false;
			this.isStopped = false;
		}
		
		public void start() {
			this.startMs = System.currentTimeMillis();
			this.hasStarted = true;
		}
		
		public void stop() {
			this.endMs = System.currentTimeMillis();
			this.isStopped = true;
		}
		
		public double getTimeInSeconds() {
			if (this.hasStarted && this.isStopped) {
				return ((double)(this.endMs - this.startMs))/1000;
			} else
				return -1;
		}
	}
}
