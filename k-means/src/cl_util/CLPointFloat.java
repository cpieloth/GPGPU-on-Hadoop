package cl_util;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.List;

import lightLogger.Logger;
import cl_kernel.PointFloatNearestIndex;
import clustering.ICPoint;
import clustering.IPoint;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLException;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLQueue;

public class CLPointFloat implements ICLPointOperation<Float> {

	private static final Class<CLPointFloat> CLAZZ = CLPointFloat.class;

	private static final int SIZEOF_CL_FLOAT = 4;

	private final int DIM;
	private final int SIZEOF_POINT;
	private final CLInstance CL_INSTANCE;

	private final int BUFFER_SIZE;
	private final int BUFFER_ITEM_SIZE;
	private int bufferCount;
	private int bufferItemCount;
	private final float[] buffer;
	private List<ICPoint<Float>> points;

	private final int MAX_ITEM_SIZE;
	private int itemCount;
	private int itemSize;

	private CLBuffer<Integer> resultBuffer;
	private CLBuffer<Float> pointBuffer;
	private CLBuffer<Float> centroidBuffer;
	private int centroidItemSize;
	private List<IPoint<Float>> centroids;

	private PointFloatNearestIndex kernel;

	public CLPointFloat(CLInstance clInstance, int dim) {
		this(clInstance, dim, 65536);
	}

	public CLPointFloat(CLInstance clInstance, int dim, int bufferItems) {
		CL_INSTANCE = clInstance;
		DIM = dim;
		SIZEOF_POINT = dim * SIZEOF_CL_FLOAT;
		BUFFER_ITEM_SIZE = bufferItems;
		BUFFER_SIZE = bufferItems * DIM;
		MAX_ITEM_SIZE = (int) (clInstance.getMaxMemAllocSize() / 8 / SIZEOF_POINT);

		buffer = new float[BUFFER_SIZE];
		points = new LinkedList<ICPoint<Float>>();

		kernel = (PointFloatNearestIndex) this.CL_INSTANCE
				.getKernel(PointFloatNearestIndex.getIdentifier(dim));
		if (kernel == null) {
			kernel = new PointFloatNearestIndex(CL_INSTANCE, DIM);
		}

		this.reset();
	}

	@Override
	public int getBufferSize() {
		return BUFFER_ITEM_SIZE;
	}

	@Override
	public int getBufferCount() {
		return bufferItemCount;
	}

	@Override
	public int getMaxItemSize() {
		return MAX_ITEM_SIZE;
	}

	@Override
	public int getItemCount() {
		return itemCount;
	}

	@Override
	public int getCurrentMaxItemSize() {
		return itemSize;
	}

	@Override
	public int reset(int expectedItemSize) {
		Logger.logTrace(CLAZZ, "reset(" + expectedItemSize + ")");
		boolean error;

		bufferCount = 0;
		bufferItemCount = 0;
		itemCount = 0;

		do {
			error = false;
			try {
				itemSize = expectedItemSize < 1 ? 1 : expectedItemSize;
				itemSize = MAX_ITEM_SIZE < itemSize ? MAX_ITEM_SIZE : itemSize;

				if (resultBuffer != null)
					resultBuffer.release();
				resultBuffer = CL_INSTANCE.getContext().createIntBuffer(
						CLMem.Usage.Output, itemSize);

				if (pointBuffer != null)
					pointBuffer.release();
				pointBuffer = CL_INSTANCE.getContext().createFloatBuffer(
						CLMem.Usage.Input, itemSize * DIM);
				points.clear();
			} catch (CLException.InvalidBufferSize e) {
				Logger.logError(CLAZZ,
						"Could not create CLBuffer! Resize buffer item.");
				expectedItemSize /= 2;
				error = true;
			}
		} while (error);

		Logger.logDebug(CLAZZ, "reset() - itemSize = " + itemSize + " ~"
				+ (itemSize * SIZEOF_POINT) / 1024 / 1024 + "MB");

		return itemSize;
	}

	@Override
	public int reset() {
		return this.reset(MAX_ITEM_SIZE);
	}

	@Override
	public void prepareNearestPoints(List<IPoint<Float>> centroids) {
		// TODO if there are to many centroids, they have to be split like
		// points
		centroidItemSize = centroids.size();
		Logger.logTrace(CLAZZ, "prepareNearestPoints(" + centroidItemSize + ")");
		float[] centroidsBuffer = new float[centroidItemSize * DIM];
		this.centroids = centroids;

		int i = 0;
		for (IPoint<Float> c : centroids) {
			for (int d = 0; d < DIM; d++)
				centroidsBuffer[i++] = c.get(d);
		}

		try {
			CLContext context = CL_INSTANCE.getContext();
			CLQueue cmdQ = CL_INSTANCE.getQueue();

			if (centroidBuffer != null)
				centroidBuffer.release();
			centroidBuffer = context.createFloatBuffer(CLMem.Usage.Input,
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
		if (bufferItemCount < BUFFER_ITEM_SIZE) {
			points.add(p);

			for (int d = 0; d < DIM; d++)
				this.buffer[bufferCount++] = p.get(d);

			bufferItemCount++;
		} else {
			if (writeBufferToOCL())
				put(p);
			else {
				Logger.logError(CLAZZ, "Could not put point to list!");
				return;
			}
		}
	}

	private boolean writeBufferToOCL() {
		if (bufferItemCount == 0)
			return true;

		// buffer fits into OCL memory
		if ((itemSize - itemCount) >= bufferItemCount) {
			pointBuffer
					.write(CL_INSTANCE.getQueue(), itemCount * DIM,
							bufferCount,
							FloatBuffer.wrap(buffer, 0, bufferCount), true);
			itemCount += bufferItemCount;
			bufferCount = 0;
			bufferItemCount = 0;
			return true;
		} else {
			// TODO
			Logger.logError(CLAZZ, "Not enough memory on OCL device!");
			return false;
		}
	}

	@Override
	public void setNearestPoints() {
		writeBufferToOCL();
		doNearestPoints();
	}

	private void doNearestPoints() {
		Logger.logTrace(CLAZZ, "doNearestPoints()");
		if (itemCount == 0)
			return;

		try {
			// run kernel
			IntBuffer res = kernel.run(resultBuffer, pointBuffer, itemCount,
					centroidBuffer, centroidItemSize);

			// compute result
			for (ICPoint<Float> p : points) {
				p.setCentroid(this.centroids.get(res.get()));
			}
			points.clear();
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
