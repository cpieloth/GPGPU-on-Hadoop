package javacl;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Scanner;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLDevice.QueueProperties;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLException;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLKernel.LocalSize;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;

public class MaxTemperatureReducer extends
		Reducer<Text, IntWritable, Text, IntWritable> {

	private final int MAX_VALUES = 65536; // max memory of opencl device,
											// MAX_VALUE % 64 = 0

	private static final String KERNEL_PATH = "src/kernel.cl";
	private static final int WG_FAC = 64;

	// TODO fehlberechnung auf gpu
	public void reduce(Text key, Iterable<IntWritable> values, Context context)
			throws IOException, InterruptedException {
		int[] tmpValues = new int[MAX_VALUES];
		tmpValues[0] = Integer.MIN_VALUE; // temporary max temp
		int i = 1;

		for (Iterator<IntWritable> it = values.iterator(); it.hasNext();) {
			// fill array to copy on opencl device, mind max memory of device!
			if (i < MAX_VALUES) {
				tmpValues[i++] = it.next().get();
			}
			// if max values or no more values to add, start opencl kernel
			if (i >= MAX_VALUES || !it.hasNext()) {
				tmpValues[0] = getMaxIntOCL(tmpValues, i);
				i = 1;
				System.out.println("max: " + tmpValues[0] + " - key: " + key);
			}
		}

		context.write(key, new IntWritable(tmpValues[0]));
	}

	private int getMaxIntOCL(int[] values, int size) {
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

	private int calcWorkGroupSize(int globalSize, final long MAX_GROUP_SIZE) {
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
}
