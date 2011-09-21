package performanceTests;

import java.nio.IntBuffer;
import java.util.Random;

import stopwatch.StopWatch;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;

public class writeTest {

	private static final String TIME_PER_VALUE = "\ttime/value (ms): ";
	private static final int ROUNDS = 5;
	private static final int SIZEOF_CL_INT = 4;
	private static final int FACTOR = 8;

	public static void main(String[] args) {
		CLPlatform[] platforms = JavaCL.listGPUPoweredPlatforms();
		CLDevice device = platforms[0].getBestDevice();
		CLContext context = JavaCL.createContext(null, device);
		CLQueue cmdQ = context
				.createDefaultQueue(new CLDevice.QueueProperties[0]);

		final int MAX_COUNT = (int) Math.min(65536, device.getGlobalMemSize()
				/ 8 / SIZEOF_CL_INT - 65536);
		System.out.println("Device: " + device.getName());
		System.out.println("ROUNDS: " + ROUNDS);
		System.out.println("MAX_COUNT: " + MAX_COUNT);

		System.out.println();

		Random ran = new Random(System.currentTimeMillis());
		int count;
		StopWatch sw = new StopWatch("\ttime (ms): ", "");
		double time;
		int[] buffer;
		CLBuffer<Integer> clBuffer = context.createIntBuffer(Usage.InputOutput,
				1);

		// many single writes
		System.out.println("Many single writes");
		for (count = 128; count <= MAX_COUNT; count *= FACTOR) {
			sw.reset();
			System.out.println("\tIntValues: " + count);

			clBuffer.release();
			clBuffer = context.createIntBuffer(Usage.InputOutput, count);
			sw.start();
			for (int r = 0; r < ROUNDS; r++) {
				for (int i = 0; i < count; i++) {
					clBuffer.write(cmdQ, i, 1,
							IntBuffer.wrap(new int[] { ran.nextInt() }), true);
				}
			}
			sw.stop();

			time = sw.getTime() / ROUNDS;
			System.out.println(sw.prefix + time);
			System.out.println(TIME_PER_VALUE + time / count);
			System.out.println();
		}

		// one big write
		System.out
				.println("One big single write (including the time for filling the array)");
		count = MAX_COUNT;
		for (count = 128; count <= MAX_COUNT; count *= FACTOR) {
			sw.reset();
			System.out.println("\tIntValues: " + count);

			clBuffer.release();
			clBuffer = context.createIntBuffer(Usage.InputOutput, count);
			buffer = new int[count];
			sw.start();
			for (int r = 0; r < ROUNDS; r++) {
				for (int i = 0; i < count; i++)
					buffer[i] = ran.nextInt();
				clBuffer.write(cmdQ, IntBuffer.wrap(buffer), true);
			}
			sw.stop();

			time = sw.getTime() / ROUNDS;
			System.out.println(sw.prefix + time);
			System.out.println(TIME_PER_VALUE + time / count);
			System.out.println();
		}

		// fix data, variable buffer size
		System.out
				.println("One single big write on different buffer size (including the time for filling the array)");
		int bufferSize;
		for (count = 128; count <= MAX_COUNT; count *= FACTOR) {
			sw.reset();
			bufferSize = count * 2;
			System.out.println("\tCLBuffer size: " + bufferSize);
			System.out.println("\tIntValues: " + count);
			buffer = new int[bufferSize];

			clBuffer.release();
			clBuffer = context.createIntBuffer(Usage.InputOutput, bufferSize);
			for (int r = 0; r < ROUNDS; r++) {
				for (int i = 0; i < count; i++)
					buffer[i] = ran.nextInt();
				clBuffer.write(cmdQ, IntBuffer.wrap(buffer), true);
			}
			sw.stop();

			time = sw.getTime() / ROUNDS;
			System.out.println(sw.prefix + time);
			System.out.println(TIME_PER_VALUE + time / count);
			System.out.println();
		}
	}
}
