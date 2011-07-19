package cl_util;

import java.nio.FloatBuffer;
import java.util.List;

import lightLogger.Logger;
import clustering.ICPoint;
import clustering.IPoint;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLException;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLQueue;

public class CLPointFloat {

	private static final Class<CLPointFloat> CLAZZ = CLPointFloat.class;

	public static final int MAX_BUFFER_SIZE = 8192;
	private int BUFFER_SIZE;
	private int BUFFER_ITEMS;
	private static final int SIZEOF_CL_FLOAT = 4;

	private final int DIM;
	private CLInstance clInstance;

	private ICPoint[] itemBuffer;
	private float[] buffer;
	private int itemCount;
	private CLBuffer<FloatBuffer> resultBuffer;
	private CLBuffer<FloatBuffer> compareBuffer;
	private int COMPARE_BUFFER_SIZE;

	public CLPointFloat(CLInstance clInstance, int dim) {
		this.clInstance = clInstance;
		this.DIM = dim;

		BUFFER_ITEMS = MAX_BUFFER_SIZE / DIM;
		BUFFER_SIZE = BUFFER_ITEMS * DIM;

		this.resetBuffer();
	}

	public void resetBuffer(int bufferItems) {
		BUFFER_ITEMS = (bufferItems * DIM) > MAX_BUFFER_SIZE ? (MAX_BUFFER_SIZE / DIM)
				: bufferItems * DIM;
		BUFFER_SIZE = BUFFER_ITEMS * DIM;

		this.itemBuffer = new ICPoint[BUFFER_ITEMS];
		this.buffer = new float[BUFFER_SIZE];
		this.itemCount = 0;

		this.resultBuffer = this.clInstance.getContext().createFloatBuffer(
				CLMem.Usage.InputOutput, BUFFER_SIZE);
	}

	public void resetBuffer() {
		this.resetBuffer(BUFFER_ITEMS);
	}

	public void prepareNearestPoints(List<IPoint> centroids) {
		COMPARE_BUFFER_SIZE = centroids.size() * DIM;
		float[] centroidsBuffer = new float[COMPARE_BUFFER_SIZE];

		int i = 0;
		for (IPoint c : centroids) {
			for (int d = 0; d < DIM; d++)
				centroidsBuffer[i++] = c.get(d);
		}

		try {
			CLContext context = this.clInstance.getContext();
			CLQueue cmdQ = this.clInstance.getQueue();

			this.compareBuffer = context.createFloatBuffer(CLMem.Usage.Input,
					FloatBuffer.wrap(centroidsBuffer), true);

			cmdQ.finish();
		} catch (CLException err) {
			Logger.logError(CLAZZ, "OpenCL error:\n" + err.getMessage() + "():"
					+ err.getCode());
			err.printStackTrace();
		} catch (Exception err) {
			Logger.logError(CLAZZ, "Error:\n" + err.getMessage() + "()");
			err.printStackTrace();
		}
	}

	public void put(ICPoint p) {
		if (this.itemCount < BUFFER_ITEMS) {
			this.itemBuffer[this.itemCount++] = p;
		} else {
			doNearestPoints(this.itemCount);
			this.itemBuffer[this.itemCount++] = p;
		}
	}

	public void setNearestPoints() {
		// TODO
	}

	private void doNearestPoints(int size) {
		int bufferCount = 0;
		// copy points to buffer
		for (ICPoint p : this.itemBuffer) {
			for (int d = 0; d < DIM; d++)
				this.buffer[bufferCount++] = p.get(d);
		}

		try {
			CLContext context = this.clInstance.getContext();
			CLQueue cmdQ = this.clInstance.getQueue();

			// copy buffer to device
			this.resultBuffer.write(cmdQ, 0, bufferCount,
					FloatBuffer.wrap(this.buffer), true, new CLEvent[0]);

			int globalSize = bufferCount;
			// TODO
			// run kernel
			// load resultBuffer

			cmdQ.finish();
		} catch (CLException err) {
			Logger.logError(CLAZZ, "OpenCL error:\n" + err.getMessage() + "():"
					+ err.getCode());
			err.printStackTrace();
		} catch (Exception err) {
			Logger.logError(CLAZZ, "Error:\n" + err.getMessage() + "()");
			err.printStackTrace();
		}

		this.itemCount = 0;
	}

}
