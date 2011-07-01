package maxValue;

import java.io.File;
import java.util.Random;
import java.util.Scanner;

import lightLogger.Level;
import lightLogger.Logger;

import com.nativelibs4java.opencl.CLDevice;

public abstract class MaxValueAbstract {
	
	private static final Class<MaxValueAbstract> CLAZZ = MaxValueAbstract.class;
	

	/* Kommandozeilenargumente */
	protected static final char CPU = 's';
	protected static final char CLCPU = 'c';
	protected static final char CLGPU = 'g';
	
	protected static final char INFO = 'i';
	protected static final char ERROR = 'e';
	protected static final char DEBUG = 'd';

	/* Globale Variablen */
	public int LEN = -1;
	public char TYPE;
	public static final int EXIT_FAILURE = 1;
	public static final int EXIT_SUCCESS = 0;
	public static final int MAX_FAILURE = Integer.MIN_VALUE;

	protected static final String KERNEL_PATH = "src/kernel.cl";
	protected static final int WG_FAC = 64;
	protected static final int SIZEOF_CL_INT = 4;
	
	public abstract int maxValue(CLDevice.Type type, int[] values);
	
	public MaxValueAbstract(String args[]) {
		this.checkArguments(args);
	}

	public MaxValueAbstract() {
		this.LEN = 1;
		this.TYPE = CPU;
		Logger.logWarn(CLAZZ, "Constructor should only be used for testing");
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
			return maxValue(CLDevice.Type.GPU, values);
		case CLCPU:
			return maxValue(CLDevice.Type.CPU, values);
		default:
			Logger.logError(CLAZZ, "Device type not supported!");
			return MAX_FAILURE;
		}
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
			System.out.println("Argumente: " + INFO + "|" + DEBUG + "|" + ERROR + " " + CLCPU + "|" + CLGPU + "|" + CPU
					+ " <Vektorgroesze>");
			return false;
		}

		switch(args[0].charAt(0))
		{
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
