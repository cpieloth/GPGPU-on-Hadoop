package performanceTests;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;

public class readTest {

	private static final int ROUNDS = 5;
	private static final int SIZEOF_CL_INT = 4;

	public static void main(String[] args) {
		CLPlatform[] platforms = JavaCL.listGPUPoweredPlatforms();
		CLDevice device = platforms[0].getBestDevice();
		CLContext context = JavaCL.createContext(null, device);
		CLQueue cmdQ = context.createDefaultQueue(new CLDevice.QueueProperties[0]);

		final int MAX_COUNT = (int) (device.getGlobalMemSize() / 16);
		System.out.println("Device: " + device.getName());
		System.out.println("MAX_COUNT: " + MAX_COUNT);

		System.out.println();

		int count;
		long start, end, time;
		int[] buffer;
		CLBuffer<IntBuffer> clBuffer = context.createIntBuffer(
				Usage.InputOutput, 1);

		// many single read
		System.out.println("Many single reads");
		for (int f = 1; f < 9; f++) {
			time = 0;
			count = 128 * f;
			System.out.println("\tIntValues: " + count);

			buffer = new int[count];
			for (int i = 0; i < buffer.length; i++)
				buffer[i] = i;
			clBuffer.release();
			clBuffer = context.createIntBuffer(Usage.InputOutput,
					IntBuffer.wrap(buffer), true);

			for (int r = 0; r < ROUNDS; r++) {
				start = System.currentTimeMillis();
				IntBuffer intBuffer = ByteBuffer
						.allocateDirect(1 * SIZEOF_CL_INT)
						.order(context.getByteOrder()).asIntBuffer();
				for (int i = 0; i < count; i++) {
					clBuffer.read(cmdQ, i, 1, intBuffer, true);
					intBuffer.rewind();
					buffer[i] = intBuffer.get();
				}
				end = System.currentTimeMillis();
				time += (end - start);
			}
			System.out.println("\ttime=" + (time / ROUNDS) + ";");
			System.out.println();
		}

		// one big read
		System.out.println("One big single read");
		count = MAX_COUNT;
		for (int f = 1; f < 9; f++) {
			time = 0;
			count = count / f;
			System.out.println("\tIntValues: " + count);
			buffer = new int[count];
			for (int i = 0; i < buffer.length; i++)
				buffer[i] = i;
			clBuffer.release();
			clBuffer = context.createIntBuffer(Usage.InputOutput,
					IntBuffer.wrap(buffer), true);

			for (int r = 0; r < ROUNDS; r++) {
				start = System.currentTimeMillis();
				IntBuffer intBuffer = ByteBuffer
						.allocateDirect(count * SIZEOF_CL_INT)
						.order(context.getByteOrder()).asIntBuffer();
				clBuffer.read(cmdQ, intBuffer, true);
				intBuffer.rewind();
				for (int i = 0; i < count; i++) {
					buffer[i] = intBuffer.get();
				}
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
			buffer = new int[count];
			for (int i = 0; i < buffer.length; i++)
				buffer[i] = i;
			clBuffer.release();
			clBuffer = context.createIntBuffer(Usage.InputOutput,
					IntBuffer.wrap(buffer), true);
			for (int r = 0; r < ROUNDS; r++) {
				start = System.currentTimeMillis();
				IntBuffer intBuffer = ByteBuffer
						.allocateDirect(bufferSize * SIZEOF_CL_INT)
						.order(context.getByteOrder()).asIntBuffer();
				clBuffer.read(cmdQ, 0, bufferSize, intBuffer, true);
				intBuffer.rewind();
				for (int i = 0; i < bufferSize; i++) {
					buffer[i] = intBuffer.get();
				}
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
