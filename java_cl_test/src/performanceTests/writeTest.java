package performanceTests;

import java.nio.IntBuffer;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;

public class writeTest {

	private static final int ROUNDS = 5;

	public static void main(String[] args) {
		CLPlatform[] platforms = JavaCL.listGPUPoweredPlatforms();
		CLDevice device = platforms[0].getBestDevice();
		CLContext context = JavaCL.createContext(null, device);
		CLQueue cmdQ = context.createDefaultQueue(CLDevice.QueueProperties
				.values());

		final int MAX_COUNT = (int) (device.getGlobalMemSize() / 16);
		System.out.println("Device: " + device.getName());
		System.out.println("MAX_COUNT: " + MAX_COUNT);

		System.out.println();

		int count;
		long start, end, time;
		int[] buffer;
		CLBuffer<IntBuffer> clBuffer = context.createIntBuffer(
				Usage.InputOutput, 1);

		// many single writes
		System.out.println("Many single writes");
		for (int f = 1; f < 9; f++) {
			time = 0;
			count = 128 * f;
			System.out.println("\tIntValues: " + count);
			clBuffer.release();
			clBuffer = context.createIntBuffer(Usage.InputOutput, count);
			for (int r = 0; r < ROUNDS; r++) {
				start = System.currentTimeMillis();
				for (int i = 0; i < count; i++) {
					clBuffer.write(cmdQ, i, 1, IntBuffer.wrap(new int[] { i }),
							true);
					cmdQ.finish();
				}
				end = System.currentTimeMillis();
				time += (end - start);
			}
			System.out.println("\ttime=" + (time / ROUNDS) + ";");
			System.out.println();
		}

		// one big write
		System.out.println("One big single write");
		count = MAX_COUNT;
		for (int f = 1; f < 9; f++) {
			time = 0;
			count = count / f;
			clBuffer.release();
			clBuffer = context.createIntBuffer(Usage.InputOutput, count);
			System.out.println("\tIntValues: " + count);
			for (int r = 0; r < ROUNDS; r++) {
				start = System.currentTimeMillis();
				buffer = new int[count];
				for (int i = 0; i < count; i++)
					buffer[i] = i;
				clBuffer.write(cmdQ, IntBuffer.wrap(buffer), true);
				cmdQ.finish();
				end = System.currentTimeMillis();
				time += (end - start);
			}
			System.out.println("\ttime=" + (time / ROUNDS) + ";");
			System.out.println();
		}

		// fix data, variable buffer size
		System.out.println("One single big write on different buffer size");
		int bufferSize = MAX_COUNT / 4;
		for (int f = 1; f < 5; f++) {
			time = 0;
			count = bufferSize * f;
			System.out.println("\tCLBuffer size: " + count);
			System.out.println("\tIntValues: " + bufferSize);
			clBuffer.release();
			clBuffer = context.createIntBuffer(Usage.InputOutput, count);
			for (int r = 0; r < ROUNDS; r++) {
				start = System.currentTimeMillis();
				buffer = new int[bufferSize];
				for (int i = 0; i < bufferSize; i++)
					buffer[i] = i;
				clBuffer.write(cmdQ, IntBuffer.wrap(buffer), true);
				cmdQ.finish();
				end = System.currentTimeMillis();
				time += (end - start);
			}
			System.out.println("\ttime=" + (time / ROUNDS) + ";");
			System.out.println();
		}

		// creation time
		System.out.println("Buffer creation time");
		count = MAX_COUNT;
		for (int f = 1; f < 5; f++) {
			time = 0;
			count = count / f;
			for (int r = 0; r < ROUNDS; r++) {
				clBuffer.release();
				start = System.currentTimeMillis();
				clBuffer = context.createIntBuffer(Usage.InputOutput, count);
				end = System.currentTimeMillis();
				time += (end - start);
			}
			System.out.println("\tIntValues: " + count);
			System.out.println("\ttime=" + (time / ROUNDS) + ";");
			System.out.println();
		}

	}
}
