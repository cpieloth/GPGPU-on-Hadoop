package performanceTests;

import java.util.Scanner;

import stopwatch.StopWatch;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;

public class CLQueryTest {

	private static final Class<CLQueryTest> CLAZZ = CLQueryTest.class;

	private static final int ROUNDS = 5;
	private static final int SIZEOF_CL_INT = 4;

	private static final String KERNEL_PATH = "/CLQueryTest.cl";
	private static final String KENREL_NAME = "maxInt";

	public static void main(String[] args) {
		double time;
		StopWatch sw = new StopWatch("\ttime (ms): ", "");

		CLPlatform[] platforms = null;
		CLDevice[] devices = null;
		CLContext context = null;
		CLQueue cmdQ = null;
		CLProgram program = null;
		String clSource = null;
		CLKernel kernel = null;

		/* TEST */
		System.out.println("OpenCL query time for platform");
		sw.reset();
		sw.start();
		for (int r = 0; r < ROUNDS; r++) {
			sw.resume();
			platforms = JavaCL.listGPUPoweredPlatforms();
			sw.pause();

			for (CLPlatform p : platforms)
				p.release();
		}
		sw.stop();

		time = sw.getTime() / ROUNDS;
		System.out.println(sw.prefix + time);
		System.out.println("\tPlatforms: " + platforms.length);
		System.out.println();

		/* TEST */
		System.out.println("OpenCL query time for devices");
		platforms = JavaCL.listGPUPoweredPlatforms();
		sw.reset();
		sw.start();
		for (int r = 0; r < ROUNDS; r++) {
			sw.resume();
			devices = platforms[0].listGPUDevices(true);
			sw.pause();

			for (CLDevice d : devices)
				d.release();
		}
		sw.stop();

		time = sw.getTime() / ROUNDS;
		System.out.println(sw.prefix + time);
		System.out.println("\tPlatform: " + platforms[0].getName());
		System.out.println("\tDevices: " + devices.length);
		System.out.println();

		/* TEST */
		System.out.println("Context creation time");
		devices = platforms[0].listGPUDevices(true);
		sw.reset();
		sw.start();
		for (int r = 0; r < ROUNDS; r++) {
			sw.resume();
			context = JavaCL.createContext(null, devices);
			sw.pause();

			context.release();
		}
		sw.stop();

		time = sw.getTime() / ROUNDS;
		System.out.println(sw.prefix + time);
		System.out.println("\tDevices: " + devices.length);
		System.out.println();

		/* TEST */
		System.out.println("CommandQueue creation time");
		context = JavaCL.createContext(null, devices);
		sw.reset();
		sw.start();
		for (int r = 0; r < ROUNDS; r++) {
			sw.resume();
			cmdQ = context.createDefaultQueue(new CLDevice.QueueProperties[0]);
			sw.pause();

			cmdQ.release();
		}
		sw.stop();

		time = sw.getTime() / ROUNDS;
		System.out.println(sw.prefix + time);
		System.out.println("\tDevices: " + devices.length);
		System.out.println();
		context.release();

		/* TEST */
		System.out.println("Read file time");
		StringBuffer sb;
		Scanner sc = null;
		sw.reset();
		sw.start();
		for (int r = 0; r < ROUNDS; r++) {
			sw.resume();
			sb = new StringBuffer();
			try {
				sc = new Scanner(CLAZZ.getResourceAsStream(KERNEL_PATH));
				while (sc.hasNext())
					sb.append(sc.nextLine());
				clSource = sb.toString();
			} catch (Exception e) {
				System.out.println("Could not read file: " + KERNEL_PATH);
				e.printStackTrace();
			} finally {
				if (sc != null)
					sc.close();
			}
		}
		sw.stop();

		time = sw.getTime() / ROUNDS;
		System.out.println(sw.prefix + time);
		System.out.println();

		/* TEST */
		System.out.println("Program creation time");
		context = JavaCL.createContext(null, devices);
		sw.reset();
		sw.start();
		for (int r = 0; r < ROUNDS; r++) {
			sw.resume();
			try {
				program = context.createProgram(clSource);
				try {
					program.build();
				} catch (Exception err) {
					System.out
							.println("Build log for \""
									+ context.getDevices()[0] + "\n"
									+ err.getMessage());
					err.printStackTrace();
				}
			} catch (Exception err) {
				System.out.println("Error:\n" + err.getMessage() + "()");
				err.printStackTrace();
			}
			sw.pause();

			if (program != null)
				program.release();
		}
		sw.stop();

		time = sw.getTime() / ROUNDS;
		System.out.println(sw.prefix + time);
		System.out.println();

		/* TEST */
		System.out.println("Kernel creation time");
		context = JavaCL.createContext(null, devices);
		try {
			program = context.createProgram(clSource);
			try {
				program.build();
			} catch (Exception err) {
				System.out.println("Build log for \"" + context.getDevices()[0]
						+ "\n" + err.getMessage());
				err.printStackTrace();
			}
		} catch (Exception err) {
			System.out.println("Error:\n" + err.getMessage() + "()");
			err.printStackTrace();
		}
		sw.start();
		for (int r = 0; r < ROUNDS; r++) {
			sw.resume();
			try {
				kernel = program.createKernel(KENREL_NAME);
			} catch (Exception err) {
				System.out.println("Error:\n" + err.getMessage() + "()");
				err.printStackTrace();
			}
			sw.pause();
			if (kernel != null)
				kernel.release();
		}
		sw.stop();

		time = sw.getTime() / ROUNDS;
		System.out.println(sw.prefix + time);
		System.out.println();

		// creation time
		final int MAX_COUNT = (int) Math.min(65536,
				devices[0].getGlobalMemSize() / 8 / SIZEOF_CL_INT - 65536);
		final int FACTOR = 8;
		System.out.println("Buffer creation time");
		int count = MAX_COUNT;
		CLBuffer<Integer> clBuffer = null;
		for (count = 128; count <= MAX_COUNT; count *= FACTOR) {
			sw.reset();
			System.out.println("\tIntValues: " + count);

			sw.start();
			for (int r = 0; r < ROUNDS; r++) {
				sw.resume();
				clBuffer = context.createIntBuffer(Usage.InputOutput, count);
				sw.pause();

				if (clBuffer != null)
					clBuffer.release();
			}
			sw.stop();

			time = sw.getTime() / ROUNDS;
			System.out.println(sw.prefix + time);
			System.out.println("\tDevice: " + devices[0].getName());
			System.out.println();
		}
	}

}
