package utils;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;

import lightLogger.Logger;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLDevice.QueueProperties;
import com.nativelibs4java.opencl.CLDevice.Type;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLException;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLKernel.LocalSize;
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

	private final static int MAX_VALUES = 65536; // max memory of opencl device,
	// MAX_VALUE % 64 = 0

	private static final String KERNEL_PATH = "src/kernel.cl";
	private static final int WG_FAC = 64;

	private static int getMaxIntOCL(int[] values, int size) {
		System.out.println("getMaxIntOCL called. value.length=" + values.length
				+ " size=" + size);
		int EXIT_FAILURE = Integer.MIN_VALUE;
		final CLDevice.Type CL_TYPE = CLDevice.Type.GPU;

		// calculate optimal length
		int offset = size;
		if (size < values.length && size % WG_FAC != 0) {
			size = (int) Math.ceil((double) size / WG_FAC) * WG_FAC;
			size = size < values.length ? size : values.length;
			System.out.println("resized to optimal start size " + size);
		}
		// fill end of array with minimum
		for (int i = offset; i < size; i++)
			values[i] = Integer.MIN_VALUE;

		// FIXME wrong maxInt
		try {
			// Init OpenCL
			CLPlatform[] platforms = JavaCL.listPlatforms();

			EnumSet<CLDevice.Type> types = EnumSet.of(CL_TYPE);
			ArrayList<CLDevice> devices = new ArrayList<CLDevice>();
			CLDevice[] devTmp;

			for (CLPlatform platform : platforms) {
				devTmp = platform.listDevices(types, true);
				devices.addAll(Arrays.asList(devTmp));
			}

			devTmp = new CLDevice[devices.size()];
			CLContext context = JavaCL.createContext(null,
					devices.toArray(devTmp));
			CLQueue cmdQ = context
					.createDefaultQueue(QueueProperties.ProfilingEnable);

			String src = readFile(KERNEL_PATH);

			CLProgram program = context.createProgram(src);

			try {
				program.build();
			} catch (Exception err) {
				System.out.println("Build log for \"" + devices.get(0) + "\n"
						+ err.getMessage());
				return EXIT_FAILURE;
			}

			CLKernel kernel = program.createKernel("maxInt");

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
				System.out.println("GlobalSize: " + globalSize);
				System.out.println("LocalSize: " + localSize);
				if (localSize == 1) {
					globalSize = (int) (Math.ceil((double) size / WG_FAC) * WG_FAC);
					localSize = calcWorkGroupSize(globalSize, MAX_GROUP_SIZE);
					System.out.println("GlobalSize has been extended to "
							+ globalSize);
					System.out.println("LocalSize has been changed to "
							+ localSize);
				}

				kernel.setArg(0, vBuffer);
				kernel.setArg(1, new LocalSize(localSize));

				// Run kernel
				// CLEvent event =
				kernel.enqueueNDRange(cmdQ, new int[] { globalSize },
						new int[] { localSize }, new CLEvent[0]);

				cmdQ.finish();

				size = globalSize / localSize;
			} while (globalSize > localSize && localSize > 1);

			// Get results
			IntBuffer resBuffer = ByteBuffer
					.allocateDirect(values.length * Integer.SIZE)
					.order(context.getByteOrder()).asIntBuffer();
			vBuffer.read(cmdQ, resBuffer, true, new CLEvent[0]);
			resBuffer.rewind();
			resBuffer.get(values);

			return values[0];
		} catch (CLException err) {
			System.out.println("OpenCL error:\n" + err.getMessage() + "():"
					+ err.getCode());
			err.printStackTrace();
			return EXIT_FAILURE;
		} catch (Exception err) {
			System.out.println("Error:\n" + err.getMessage() + "()");
			err.printStackTrace();
			return EXIT_FAILURE;
		}
	}

	private static int calcWorkGroupSize(int globalSize,
			final long MAX_GROUP_SIZE) {
		int localSize = (int) MAX_GROUP_SIZE;
		if (globalSize < localSize)
			localSize = globalSize;
		else
			while (globalSize % localSize != 0)
				--localSize;
		return localSize;
	}

	private static String readFile(String fName) {
		StringBuffer sb = new StringBuffer();
		try {
			Scanner sc = new Scanner(new File(fName));
			while (sc.hasNext())
				sb.append(sc.nextLine());

		} catch (Exception e) {
			System.out.println("Could not read file: " + fName);
			e.printStackTrace();
		}
		return sb.toString();
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

		/*** Kontrollwerte ***/
		/*
		 * values[(size_t) (size / 3)] = 2323; values[(size_t) (size / 2)] =
		 * 4242; values[size - 1] = 7331;
		 */
		int max_pos = new Random().nextInt(LEN);
		// int max_pos = 1201;
		max_pos = LEN -1;
		values[max_pos] = 7331;
		
		System.out.println("max_pos: " + max_pos + " max_value: " + values[max_pos]);

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
			for (int i = 0; i < 5; i++)
				Logger.logInfo(CLAZZ, "Maximum: " + values[i]);
			System.exit(EXIT_SUCCESS);
		} else {
			Logger.logError(CLAZZ, "Error, no result!");
			System.exit(EXIT_FAILURE);
		}
	}

	private static int maxValue(Type gpu, int[] values) {
		int[] tmpValues = new int[MAX_VALUES];
		tmpValues[0] = Integer.MIN_VALUE; // temporary max temp
		int i = 1;

		// CHANGED
		Iterator<Integer> it = new IntArrayIterator(values);
		int tmp;
		while (it.hasNext()) {
			// fill array to copy on opencl device, mind max memory of device!
			if (i < MAX_VALUES) {
				tmp = it.next().intValue();
				tmpValues[i++] = tmp;
				if(tmp == 7331)
					System.out.println("___FOUND! >> " + tmp);
			}
			// if max values or no more values to add, start opencl kernel
			if (i >= MAX_VALUES || !it.hasNext()) {
				tmpValues[0] = getMaxIntOCL(tmpValues, i);
				i = 1;
				System.out.println("tmp_max: " + tmpValues[0]);
			}
		}
		values[0] = tmpValues[0];
		return EXIT_SUCCESS;
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

	private static void fill(int[] vec) {
		Random r = new Random();
		for (int i = 0; i < vec.length; i++)
			vec[i] = r.nextInt(1024);
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
