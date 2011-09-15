package cl_util;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import lightLogger.Logger;
import cl_kernel.PointFloatNearestIndex;
import clustering.ICPoint;
import clustering.IPoint;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLException;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLQueue;

public class CLPointFloat implements ICLPointOperation<Float> {

	private static final Class<CLPointFloat> CLAZZ = CLPointFloat.class;

	private int MAX_BUFFER_SIZE;
	private int BUFFER_ITEMS;
	private static final int SIZEOF_CL_FLOAT = 4;

	private final int DIM;
	private final int ITEM_SIZE;
	private CLInstance clInstance;

	private ICPoint<Float>[] itemBuffer;
	private float[] buffer;
	private int itemCount;
	private int bufferCount;
	private CLBuffer<Integer> resultBuffer;
	private CLBuffer<Float> pointBuffer;
	private CLBuffer<Float> compareBuffer;
	private int COMPARE_ITEMS;
	private List<IPoint<Float>> centroids;

	public CLPointFloat(CLInstance clInstance, int dim) {
		this.clInstance = clInstance;
		this.DIM = dim;
		this.ITEM_SIZE = dim * SIZEOF_CL_FLOAT;

		MAX_BUFFER_SIZE = (int) (clInstance.getMaxMemAllocSize());
		BUFFER_ITEMS = getMaxBufferItems() / 16;

		this.resetBuffer(BUFFER_ITEMS);
	}

	@Override
	public int getMaxBufferItems() {
		return MAX_BUFFER_SIZE / ITEM_SIZE;
	}

	@Override
	public int getCurrentMaxBufferItems() {
		return BUFFER_ITEMS;
	}

	@SuppressWarnings("unchecked")
	@Override
	public int resetBuffer(int bufferItems) {
		Logger.logTrace(CLAZZ, "resetBuffer(" + bufferItems + ")");
		boolean error;
		do {
			error = false;

			try {
				bufferItems = bufferItems < 1 ? 1 : bufferItems;
				BUFFER_ITEMS = (bufferItems * ITEM_SIZE) > MAX_BUFFER_SIZE ? (MAX_BUFFER_SIZE / ITEM_SIZE)
						: bufferItems;

				if (this.itemBuffer == null
						|| BUFFER_ITEMS > this.itemBuffer.length)
					this.itemBuffer = new ICPoint[BUFFER_ITEMS];
				if (this.buffer == null
						|| (BUFFER_ITEMS * DIM) > this.buffer.length)
					this.buffer = new float[BUFFER_ITEMS * DIM];
				this.itemCount = 0;
				this.bufferCount = 0;
				this.pointBuffer = this.clInstance.getContext()
						.createFloatBuffer(CLMem.Usage.Input,
								BUFFER_ITEMS * DIM);
				this.resultBuffer = this.clInstance.getContext()
						.createIntBuffer(CLMem.Usage.Output, BUFFER_ITEMS);
			} catch (CLException.InvalidBufferSize e) {
				Logger.logError(CLAZZ,
						"Could not create CLBuffer! Resize buffer item.");
				MAX_BUFFER_SIZE = (BUFFER_ITEMS * ITEM_SIZE) / 2;
				bufferItems /= 2;
				error = true;
				this.buffer = null;
				this.itemBuffer = null;
			} catch (OutOfMemoryError e) {
				Logger.logError(CLAZZ,
						"Could not create float or point array! Resize.");
				MAX_BUFFER_SIZE = (BUFFER_ITEMS * ITEM_SIZE) / 2;
				bufferItems /= 2;
				error = true;
				this.buffer = null;
				this.itemBuffer = null;
			}
		} while (error);
		Logger.logDebug(CLAZZ, "resetBuffer() - BUFFER_ITEMS = " + BUFFER_ITEMS
				+ " ~" + (BUFFER_ITEMS * ITEM_SIZE) / 1024 / 1024 + "MB");
		return BUFFER_ITEMS;
	}

	@Override
	public int resetBuffer() {
		return this.resetBuffer(MAX_BUFFER_SIZE / ITEM_SIZE);
	}

	@Override
	public void prepareNearestPoints(List<IPoint<Float>> centroids) {
		// TODO if there are to many centroids, they have to be split like
		// points
		COMPARE_ITEMS = centroids.size();
		Logger.logTrace(CLAZZ, "prepareNearestPoints(" + COMPARE_ITEMS + ")");
		float[] centroidsBuffer = new float[COMPARE_ITEMS * DIM];
		this.centroids = centroids;

		int i = 0;
		for (IPoint<Float> c : centroids) {
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

	@Override
	public void put(ICPoint<Float> p) {
		if (this.itemCount < BUFFER_ITEMS) {
			this.itemBuffer[this.itemCount++] = p;

			for (int d = 0; d < DIM; d++)
				this.buffer[bufferCount++] = p.get(d);
		} else {
			doNearestPoints(this.itemCount);
			this.itemBuffer[this.itemCount++] = p;
			for (int d = 0; d < DIM; d++)
				this.buffer[bufferCount++] = p.get(d);
		}
	}

	@Override
	public void setNearestPoints() {
		if (0 < this.itemCount || this.itemCount == BUFFER_ITEMS)
			this.doNearestPoints(this.itemCount);
	}

	private void doNearestPoints(int size) {
		Logger.logTrace(CLAZZ, "doNearestPoints(" + size + ")");
		try {
			CLContext context = this.clInstance.getContext();
			CLQueue cmdQ = this.clInstance.getQueue();

			PointFloatNearestIndex kernel = (PointFloatNearestIndex) this.clInstance
					.getKernel(new Integer(DIM).toString(),
							PointFloatNearestIndex.KERNEL_NAME);
			if (kernel == null) {
				kernel = new PointFloatNearestIndex(context, DIM);
				this.clInstance.addKernel(new Integer(DIM).toString(),
						PointFloatNearestIndex.KERNEL_NAME, kernel);
			}

			// copy buffer to device
			this.pointBuffer.write(cmdQ, 0, bufferCount,
					FloatBuffer.wrap(this.buffer, 0, bufferCount), true,
					new CLEvent[0]);

			// run kernel
			IntBuffer res = kernel.run(resultBuffer, pointBuffer, size,
					compareBuffer, COMPARE_ITEMS);

			// compute result
			for (int i = 0; i < itemCount; i++) {
				itemBuffer[i].setCentroid(this.centroids.get(res.get()));
			}
		} catch (CLException err) {
			Logger.logError(CLAZZ, "OpenCL error:\n" + err.getMessage() + "():"
					+ err.getCode());
			err.printStackTrace();
		} catch (Exception err) {
			Logger.logError(CLAZZ, "Error:\n" + err.getMessage() + "()");
			err.printStackTrace();
		}

		this.itemCount = 0;
		this.bufferCount = 0;
	}

}
