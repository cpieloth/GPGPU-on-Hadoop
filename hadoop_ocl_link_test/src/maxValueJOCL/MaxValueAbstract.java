package maxValueJOCL;

import static org.jocl.CL.CL_CONTEXT_DEVICES;
import static org.jocl.CL.CL_CONTEXT_PLATFORM;
import static org.jocl.CL.CL_QUEUE_PROFILING_ENABLE;
import static org.jocl.CL.clBuildProgram;
import static org.jocl.CL.clCreateCommandQueue;
import static org.jocl.CL.clCreateContextFromType;
import static org.jocl.CL.clCreateKernel;
import static org.jocl.CL.clCreateProgramWithSource;
import static org.jocl.CL.clGetContextInfo;
import static org.jocl.CL.clGetPlatformIDs;
import static org.jocl.CL.clReleaseCommandQueue;
import static org.jocl.CL.clReleaseContext;
import static org.jocl.CL.clReleaseEvent;
import static org.jocl.CL.clReleaseKernel;
import static org.jocl.CL.clReleaseProgram;

import java.io.File;
import java.util.Random;
import java.util.Scanner;

import lightLogger.Level;
import lightLogger.Logger;

import org.jocl.CL;
import org.jocl.CLException;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_event;
import org.jocl.cl_kernel;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;

public abstract class MaxValueAbstract {

	private static final Class<MaxValueAbstract> CLAZZ = MaxValueAbstract.class;

	/* Kommandozeilenargumente */
	public static final char CPU = 's';
	public static final char CLCPU = 'c';
	public static final char CLGPU = 'g';

	protected static final char INFO = 'i';
	protected static final char ERROR = 'e';
	protected static final char DEBUG = 'd';

	/* Globale Variablen */
	public int LEN = -1;
	public char TYPE;
	public static final int EXIT_FAILURE = 1;
	public static final int EXIT_SUCCESS = 0;
	public static final int MAX_FAILURE = Integer.MIN_VALUE;

	protected static final String KERNEL_PATH = "kernel.cl";
	protected static final int WG_FAC = 64;
	protected static final int SIZEOF_CL_INT = 4;

	protected cl_platform_id[] platforms;
	protected cl_device_id[] devices;
	protected cl_context context;
	protected cl_command_queue[] cmdQ;
	protected cl_program program;
	protected cl_kernel[] kernels;
	protected cl_event[] events;

	protected int numDevices;

	protected abstract int maxValue(long type, int[] values);

	public MaxValueAbstract(String args[]) {
		this.checkArguments(args);
	}

	public MaxValueAbstract() {
		this.LEN = 1;
		this.TYPE = CPU;
		Logger.logWarn(CLAZZ, "Constructor should only be used for testing");
	}

	public boolean initialize() {
		long type;

		switch (this.TYPE) {
		case CPU:
			type = -1;
			break;
		case CLCPU:
			type = CL.CL_DEVICE_TYPE_CPU;
			break;
		case CLGPU:
			type = CL.CL_DEVICE_TYPE_GPU;
			break;
		default:
			type = -1;
			break;
		}

		return this.initialize(type);
	}

	public boolean initialize(long type) {
		if (type == -1) {
			this.TYPE = CPU;
			return true;
		}

		if (type == CL.CL_DEVICE_TYPE_CPU)
			this.TYPE = CLCPU;
		else if (type == CL.CL_DEVICE_TYPE_GPU)
			this.TYPE = CLGPU;
		else
			return false;

		CL.setExceptionsEnabled(true);
		try {
			// Init OpenCL
			platforms = new cl_platform_id[3];
			clGetPlatformIDs(platforms.length, platforms, null);

			cl_context_properties contextProperties = new cl_context_properties();
			for (cl_platform_id pId : platforms) {
				if (pId != null)
					contextProperties.addProperty(CL_CONTEXT_PLATFORM, pId);
				else
					break;
			}

			context = clCreateContextFromType(contextProperties, type, null,
					null, null);
			if (context == null)
				throw new Exception("Could not create context");

			String src = readFile(KERNEL_PATH);

			program = clCreateProgramWithSource(context, 1,
					new String[] { src }, null, null);
			clBuildProgram(program, 0, null, null, null, null);

			long numBytes[] = new long[1];
			clGetContextInfo(context, CL_CONTEXT_DEVICES, 0, null, numBytes);
			numDevices = (int) numBytes[0] / Sizeof.cl_device_id;
			devices = new cl_device_id[numDevices];
			clGetContextInfo(context, CL_CONTEXT_DEVICES, numBytes[0],
					Pointer.to(devices), null);

			cmdQ = new cl_command_queue[numDevices];
			kernels = new cl_kernel[numDevices];
			events = new cl_event[numDevices];

			long properties = 0;
			properties |= CL_QUEUE_PROFILING_ENABLE;

			for (int i = 0; i < numDevices; i++) {
				cmdQ[i] = clCreateCommandQueue(context, devices[i], properties,
						null);
				kernels[i] = clCreateKernel(program, "maxInt", null);
				events[i] = new cl_event();
			}

			return true;
		} catch (CLException err) {
			Logger.logError(CLAZZ, "OpenCL error:\n" + err.getMessage() + "():"
					+ err.getCause());
			err.printStackTrace();
			return false;
		} catch (Exception err) {
			Logger.logError(CLAZZ, "Error:\n" + err.getMessage() + "()");
			err.printStackTrace();
			return false;
		}
	}

	public int maxValue(int[] values) {
		switch (TYPE) {
		case CPU:
			int max = MAX_FAILURE;
			for (int v : values)
				max = Math.max(v, max);
			values[0] = max;
			return values[0];
		case CLGPU:
			return maxValue(CL.CL_DEVICE_TYPE_GPU, values);
		case CLCPU:
			return maxValue(CL.CL_DEVICE_TYPE_CPU, values);
		default:
			Logger.logError(CLAZZ, "Device type not supported!");
			return MAX_FAILURE;
		}
	}

	public void finalize() {
		if (TYPE == CPU)
			return;

		for (int i = 0; i < numDevices; i++) {
			clReleaseKernel(kernels[i]);
			clReleaseCommandQueue(cmdQ[i]);
			clReleaseEvent(events[i]);
		}
		clReleaseProgram(program);
		clReleaseContext(context);
	}

	protected static int calcWorkGroupSize(int globalSize,
			final long MAX_GROUP_SIZE) {
		int localSize = (int) MAX_GROUP_SIZE;
		if (globalSize < localSize)
			localSize = globalSize;
		else
			while (globalSize % localSize != 0)
				--localSize;
		return localSize;
	}

	protected static String readFile(String fName) {
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

	protected boolean checkArguments(String[] args) {
		if (args.length < 3) {
			System.out.println("Argumente: " + INFO + "|" + DEBUG + "|" + ERROR
					+ " " + CLCPU + "|" + CLGPU + "|" + CPU
					+ " <Vektorgroesze>");
			return false;
		}

		switch (args[0].charAt(0)) {
		case INFO:
			Logger.setLogMask(Level.DEFAULT.INFO.getLevel().getValue());
			break;
		case ERROR:
			Logger.setLogMask(Level.DEFAULT.ERROR.getLevel().getValue());
			break;
		case DEBUG:
			Logger.setLogMask(Level.DEFAULT.DEBUG.getLevel().getValue());
			break;
		default:
			Logger.setLogMask(Level.DEFAULT.ALL.getLevel().getValue());
			break;
		}
		TYPE = args[1].charAt(0);
		LEN = Integer.valueOf(args[2]).intValue();

		return true;
	}

	public int[] prepareData(int size) {
		// Erzeugen der Daten
		if (size % WG_FAC != 0) {
			size = (int) (Math.ceil((double) size / WG_FAC) * WG_FAC);
		}
		int[] values = new int[size];

		Random r = new Random();
		for (int i = 0; i < size; ++i) {
			values[i] = r.nextInt(size / 2);
		}

		return values;
	}

	public int setTestValues(int[] values) {
		int max_pos = new Random().nextInt(values.length);

		int max_val = 9;
		final int MAGNITUTE = (int) Math.ceil(Math.log10(values.length));
		for (int i = 0; i < MAGNITUTE; i++)
			max_val = max_val * 10 + 9;

		values[max_pos] = max_val;
		return max_pos;
	}

}
