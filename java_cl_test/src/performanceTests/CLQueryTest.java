package performanceTests;

import java.util.Scanner;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;

public class CLQueryTest {

	private static final Class<CLQueryTest> CLAZZ = CLQueryTest.class;

	private static final int ROUNDS = 5;

	private static final String KERNEL_PATH = "/kernel.cl";
	private static final String KENREL_NAME = "maxInt";

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		long tStart, tEnd, time;

		CLPlatform[] platforms = null;
		CLDevice[] devices = null;
		CLContext context = null;
		CLQueue cmdQ = null;
		CLProgram program = null;
		String clSource = null;
		CLKernel kernel = null;

		/* TEST */
		System.out.println("OpenCL query time for platform:");
		time = 0;
		for (int r = 0; r < ROUNDS; r++) {
			tStart = System.currentTimeMillis();

			platforms = JavaCL.listGPUPoweredPlatforms();

			tEnd = System.currentTimeMillis();
			time += tEnd - tStart;
			for(CLPlatform p : platforms)
				p.release();
		}
		System.out.println("\ttime=" + (time / ROUNDS) + ";");
		System.out.println();

		/* TEST */
		System.out.println("OpenCL query time for devices:");
		platforms = JavaCL.listGPUPoweredPlatforms();
		time = 0;
		for (int r = 0; r < ROUNDS; r++) {
			tStart = System.currentTimeMillis();

			devices = platforms[0].listGPUDevices(true);

			tEnd = System.currentTimeMillis();
			time += tEnd - tStart;
			for(CLDevice d : devices)
				d.release();
		}
		System.out.println("\ttime=" + (time / ROUNDS) + ";");
		System.out.println();

		/* TEST */
		System.out.println("Context creation time:");
		devices = platforms[0].listGPUDevices(true);
		time = 0;
		for (int r = 0; r < ROUNDS; r++) {
			tStart = System.currentTimeMillis();

			context = JavaCL.createContext(null, devices);

			tEnd = System.currentTimeMillis();
			time += tEnd - tStart;
			context.release();
		}
		System.out.println("\ttime=" + (time / ROUNDS) + ";");
		System.out.println();

		/* TEST */
		System.out.println("CommandQueue creation time:");
		context = JavaCL.createContext(null, devices);
		time = 0;
		for (int r = 0; r < ROUNDS; r++) {
			tStart = System.currentTimeMillis();

			cmdQ = context.createDefaultQueue(new CLDevice.QueueProperties[0]);

			tEnd = System.currentTimeMillis();
			time += tEnd - tStart;
			cmdQ.release();
		}
		System.out.println("\ttime=" + (time / ROUNDS) + ";");
		System.out.println();

		/* TEST */
		System.out.println("Read file time:");
		StringBuffer sb;
		Scanner sc;
		time = 0;
		for (int r = 0; r < ROUNDS; r++) {
			tStart = System.currentTimeMillis();

			sb = new StringBuffer();
			try {
				sc = new Scanner(CLAZZ.getResourceAsStream(KERNEL_PATH));
				while (sc.hasNext())
					sb.append(sc.nextLine());
				sc.close();
				clSource = sb.toString();
			} catch (Exception e) {
				System.out.println("Could not read file: " + KERNEL_PATH);
				e.printStackTrace();
			}

			tEnd = System.currentTimeMillis();
			time += tEnd - tStart;
		}
		System.out.println("\ttime=" + (time / ROUNDS) + ";");
		System.out.println();

		/* TEST */
		System.out.println("Program creation time:");
		time = 0;
		for (int r = 0; r < ROUNDS; r++) {
			tStart = System.currentTimeMillis();

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

			tEnd = System.currentTimeMillis();
			time += tEnd - tStart;
		}
		System.out.println("\ttime=" + (time / ROUNDS) + ";");
		System.out.println();

		/* TEST */
		System.out.println("Kernel creation time:");
		time = 0;
		for (int r = 0; r < ROUNDS; r++) {
			tStart = System.currentTimeMillis();

			try {
				kernel = program.createKernel(KENREL_NAME);
			} catch (Exception err) {
				System.out.println("Error:\n" + err.getMessage() + "()");
				err.printStackTrace();
			}

			tEnd = System.currentTimeMillis();
			time += tEnd - tStart;
		}
		System.out.println("\ttime=" + (time / ROUNDS) + ";");
		System.out.println();
	}

}
