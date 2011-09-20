package performanceTests;

import java.nio.ByteBuffer;
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

public class readTest {

	private static final int FACTOR = 8;
	private static final int ROUNDS = 5;
	private static final int SIZEOF_CL_INT = 4;

	public static void main(String[] args) {
		CLPlatform[] platforms = JavaCL.listGPUPoweredPlatforms();
		CLDevice device = platforms[0].getBestDevice();
		CLContext context = JavaCL.createContext(null, device);
		CLQueue cmdQ = context
				.createDefaultQueue(new CLDevice.QueueProperties[0]);

		final int MAX_COUNT = (int) Math.min(65536, device.getGlobalMemSize()
				/ 8 / SIZEOF_CL_INT - 65536);
		System.out.println("Device: " + device.getName());
		System.out.println("MAX_COUNT: " + MAX_COUNT);

		System.out.println();

		Random ran = new Random(System.currentTimeMillis());
		int count;
		StopWatch sw = new StopWatch("time", "");
		double time;
		int[] buffer;
		CLBuffer<IntBuffer> clBuffer = context.createIntBuffer(
				Usage.InputOutput, 1);

		// many single read
		System.out.println("Many single reads");
		for (count = 128; count <= MAX_COUNT; count *= FACTOR) {
			sw.reset();
			System.out.println("\tIntValues: " + count);

			buffer = new int[count];
			for (int i = 0; i < buffer.length; i++)
				buffer[i] = ran.nextInt();
			clBuffer.release();
			clBuffer = context.createIntBuffer(Usage.InputOutput,
					IntBuffer.wrap(buffer), true);

			sw.start();
			for (int r = 0; r < ROUNDS; r++) {
				IntBuffer intBuffer = ByteBuffer
						.allocateDirect(1 * SIZEOF_CL_INT)
						.order(context.getByteOrder()).asIntBuffer();
				for (int i = 0; i < count; i++) {
					clBuffer.read(cmdQ, i, 1, intBuffer, true);
					intBuffer.rewind();
					buffer[i] = intBuffer.get();
				}
			}
			sw.stop();
			
			time = sw.getTime() / ROUNDS;
			System.out.println("\ttime: " + time);
			System.out.println("\ttime/value: " + time / count);
			System.out.println();
		}

		// one big read
		System.out.println("One big single read");
		for (count = 128; count <= MAX_COUNT; count *= FACTOR) {
			sw.reset();
			System.out.println("\tIntValues: " + count);
			buffer = new int[count];
			for (int i = 0; i < buffer.length; i++)
				buffer[i] = ran.nextInt();
			clBuffer.release();
			clBuffer = context.createIntBuffer(Usage.InputOutput,
					IntBuffer.wrap(buffer), true);

			sw.start();
			for (int r = 0; r < ROUNDS; r++) {
				IntBuffer intBuffer = ByteBuffer
						.allocateDirect(count * SIZEOF_CL_INT)
						.order(context.getByteOrder()).asIntBuffer();
				clBuffer.read(cmdQ, intBuffer, true);
				intBuffer.rewind();
				for (int i = 0; i < count; i++) {
					buffer[i] = intBuffer.get();
				}
			}
			sw.stop();
			
			time = sw.getTime() / ROUNDS;
			System.out.println("\ttime: " + time);
			System.out.println("\ttime/value: " + time / count);
			System.out.println();
		}

		// fix data, variable buffer size
		System.out.println("One single big read on different buffer size");
		int bufferSize;
		for (count = 128; count <= MAX_COUNT; count *= FACTOR) {
			sw.reset();
			bufferSize = count * 2;
			System.out.println("\tCLBuffer size: " + bufferSize);
			System.out.println("\tIntValues: " + count);
			buffer = new int[bufferSize];
			for (int i = 0; i < buffer.length; i++)
				buffer[i] = ran.nextInt();
			clBuffer.release();
			clBuffer = context.createIntBuffer(Usage.InputOutput,
					IntBuffer.wrap(buffer), true);
			
			sw.start();
			for (int r = 0; r < ROUNDS; r++) {
				IntBuffer intBuffer = ByteBuffer
						.allocateDirect(count * SIZEOF_CL_INT)
						.order(context.getByteOrder()).asIntBuffer();
				clBuffer.read(cmdQ, 0, count, intBuffer, true);
				intBuffer.rewind();
				for (int i = 0; i < count; i++) {
					buffer[i] = intBuffer.get();
				}
			}
			sw.stop();
			
			time = sw.getTime() / ROUNDS;
			System.out.println("\ttime: " + time);
			System.out.println("\ttime/value: " + time / count);
			System.out.println();
		}

		// creation time
		System.out.println("Buffer creation time");
		count = MAX_COUNT;
		for (count = 128; count <= MAX_COUNT; count *= FACTOR) {
			sw.reset();
			System.out.println("\tIntValues: " + count);
			
			sw.start();
			for (int r = 0; r < ROUNDS; r++) {
				clBuffer.release();
				clBuffer = context.createIntBuffer(Usage.InputOutput, count);
				
			}
			sw.stop();
			
			time = sw.getTime() / ROUNDS;
			System.out.println("\ttime: " + time);
			System.out.println("\ttime/value: " + time / count);
			System.out.println();
		}

	}
}
