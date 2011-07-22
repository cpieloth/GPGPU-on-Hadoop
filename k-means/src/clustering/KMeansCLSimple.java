package clustering;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import lightLogger.Logger;
import utils.Points;
import utils.Visualize;
import cl_util.CLInstance;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLException;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLQueue;

/**
 * Untested sequential and undistributed K-Means implementation. OpenCL part is
 * unfinished. Estimation: too slow
 * 
 * @author christof
 */
public class KMeansCLSimple implements IKMeans<Float> {

	private static final Class<KMeansCLSimple> CLAZZ = KMeansCLSimple.class;

	private static final float MAX_DISTANCE = Float.MAX_VALUE;

	private static final String KERNEL_PREFIX = CLAZZ.getSimpleName();
	private static final String KERNEL_PATH = "/kernel/KMeansCLSimple.cl";
	private static final String SQUARE_SUM = "squareSum";
	private static final String SUM = "sum";
	private static final int SIZEOF_CL_FLOAT = 4;

	private CLInstance clInstance;

	private int k = 0, dim = 0;

	@Override
	public List<IPoint<Float>> initialize(int dim, int k) {
		Logger.logTrace(CLAZZ, "initialize()");
		this.dim = dim;
		this.k = k;

		this.clInstance = new CLInstance(CLInstance.TYPES.CL_GPU);

		List<IPoint<Float>> centroids = new ArrayList<IPoint<Float>>(this.k);
		Random r = new Random();
		Point c;
		for (int i = 0; i < this.k; i++) {
			c = new Point(this.dim);
			for (int d = 0; d < this.dim; d++)
				c.set(d, r.nextFloat());
			centroids.add(c);
		}
		return centroids;
	}

	@Override
	public void assignCentroids(List<ICPoint<Float>> points,
			List<IPoint<Float>> centroids) {
		Logger.logTrace(CLAZZ, "computeDistances(" + points.size() + ", "
				+ centroids.size() + ")");
		float prevDist, dist;
		IPoint<Float> centroid;

		for (ICPoint<Float> p : points) {
			prevDist = MAX_DISTANCE;
			centroid = null;

			for (IPoint<Float> c : centroids) {
				dist = this.computeDistance(p, c);
				if (dist < prevDist) {
					prevDist = dist;
					centroid = c;
				}
			}

			p.setCentroid(centroid);
		}
	}

	private float computeDistance(IPoint<Float> p, IPoint<Float> c) {
		try {
			CLKernel kernel = this.clInstance.getKernel(KERNEL_PREFIX,
					SQUARE_SUM);
			if (kernel == null)
				kernel = this.clInstance.loadKernel(KERNEL_PATH, SQUARE_SUM,
						KERNEL_PREFIX);

			CLContext context = this.clInstance.getContext();
			CLQueue cmdQ = this.clInstance.getQueue();

			float[] tmp;

			// Prepate Data
			tmp = new float[p.getDim()];
			for (int d = 0; d < p.getDim(); d++)
				tmp[d] = p.get(d);
			CLBuffer<FloatBuffer> pBuffer = context.createFloatBuffer(
					CLMem.Usage.Input, FloatBuffer.wrap(tmp), true);
			tmp = new float[c.getDim()];
			for (int d = 0; d < c.getDim(); d++)
				tmp[d] = c.get(d);
			CLBuffer<FloatBuffer> cBuffer = context.createFloatBuffer(
					CLMem.Usage.Input, FloatBuffer.wrap(tmp), true);
			CLBuffer<FloatBuffer> resBuffer = context.createFloatBuffer(
					CLMem.Usage.Output, 1);

			cmdQ.finish();

			int globalSize = p.getDims().length;
			int localSize = p.getDims().length;

			kernel.setArg(0, resBuffer);
			kernel.setArg(1, pBuffer);
			kernel.setArg(2, cBuffer);
			kernel.setLocalArg(3, localSize * SIZEOF_CL_FLOAT);

			// Run kernel
			// CLEvent event =
			kernel.enqueueNDRange(cmdQ, new int[] { globalSize },
					new int[] { localSize }, new CLEvent[0]);

			cmdQ.finish();

			// Get results - first value in array
			FloatBuffer res = ByteBuffer.allocateDirect(1 * SIZEOF_CL_FLOAT)
					.order(context.getByteOrder()).asFloatBuffer();
			resBuffer.read(cmdQ, 0, 1, res, true, new CLEvent[0]);
			res.rewind();
			return (float) Math.sqrt(res.get(0));
		} catch (CLException err) {
			Logger.logError(CLAZZ, "OpenCL error:\n" + err.getMessage() + "():"
					+ err.getCode());
			err.printStackTrace();
			return MAX_DISTANCE;
		} catch (Exception err) {
			Logger.logError(CLAZZ, "Error:\n" + err.getMessage() + "()");
			err.printStackTrace();
			return MAX_DISTANCE;
		}
	}

	@Override
	public List<IPoint<Float>> computeCentroids(List<ICPoint<Float>> points) {
		Logger.logTrace(CLAZZ, "computeCentroids(" + points.size() + ")");

		// Collect points per centroid
		HashMap<IPoint<Float>, List<ICPoint<Float>>> clusters = new HashMap<IPoint<Float>, List<ICPoint<Float>>>();
		for (ICPoint<Float> p : points) {
			if (!clusters.containsKey(p.getCentroid())) {
				clusters.put(p.getCentroid(), new LinkedList<ICPoint<Float>>());
			}
			clusters.get(p.getCentroid()).add(p);
		}

		// Compute new centroid
		IPoint<Float> newCentroid = null;
		List<IPoint<Float>> newCentroids = new ArrayList<IPoint<Float>>(this.k);
		for (IPoint<Float> centroid : clusters.keySet()) {
			newCentroid = this.computeCentroid(clusters.get(centroid));
			newCentroids.add(newCentroid);
		}

		return newCentroids;
	}

	private IPoint<Float> computeCentroid(List<ICPoint<Float>> points) {
		IPoint<Float> centroid = new Point(this.dim);
		try {
			CLKernel kernel = this.clInstance.getKernel(KERNEL_PREFIX, SUM);
			if (kernel == null)
				kernel = this.clInstance.loadKernel(KERNEL_PATH, SUM,
						KERNEL_PREFIX);

			CLContext context = this.clInstance.getContext();
			CLQueue cmdQ = this.clInstance.getQueue();
			float p1Dim[] = new float[points.size()];

			// Prepate Data
			CLBuffer<FloatBuffer> resBuffer = context.createFloatBuffer(
					CLMem.Usage.Output, 1);
			FloatBuffer res = ByteBuffer.allocateDirect(1 * SIZEOF_CL_FLOAT)
					.order(context.getByteOrder()).asFloatBuffer();

			CLBuffer<FloatBuffer> pBuffer;

			for (int d = 0; d < this.dim; d++) {
				for (int i = 0; i < points.size(); i++)
					p1Dim[i] = points.get(i).get(d);

				pBuffer = context.createFloatBuffer(CLMem.Usage.Input,
						FloatBuffer.wrap(p1Dim), true);

				cmdQ.finish();

				int globalSize = p1Dim.length;
				// int localSize = 1;

				kernel.setArg(0, resBuffer);
				kernel.setArg(1, pBuffer);
				// kernel.setLocalArg(2, localSize * SIZEOF_CL_FLOAT);

				// Run kernel
				kernel.enqueueNDRange(cmdQ, new int[] { globalSize },
						new CLEvent[0]);

				cmdQ.finish();

				// Get results - first value in array
				resBuffer.read(cmdQ, 0, 1, res, true, new CLEvent[0]);
				res.rewind();
				centroid.set(d, res.get(0) / points.size());
			}
			return centroid;
		} catch (CLException err) {
			Logger.logError(CLAZZ, "OpenCL error:\n" + err.getMessage() + "():"
					+ err.getCode());
			err.printStackTrace();
			return null;
		} catch (Exception err) {
			Logger.logError(CLAZZ, "Error:\n" + err.getMessage() + "()");
			err.printStackTrace();
			return null;
		}
	}

	@Override
	public int getDim() {
		return this.dim;
	}

	@Override
	public int getK() {
		return this.k;
	}

	@Override
	public void run(List<ICPoint<Float>> points, List<IPoint<Float>> centroids,
			final int ITERATIONS) {
		Logger.logTrace(CLAZZ, "run() - ITERATIONS: " + ITERATIONS);

		for (int i = 0; i < ITERATIONS; i++) {
			this.assignCentroids(points, centroids);
			centroids = this.computeCentroids(points);
		}

		Logger.logTrace(CLAZZ, "run() finish");
	}

	@Override
	public void run(List<ICPoint<Float>> points, List<IPoint<Float>> centroids) {
		Logger.logTrace(CLAZZ, "run()");

		final int ITERATIONS = 5;
		int runs = 0;

		List<IPoint<Float>> oldCentroids = new ArrayList<IPoint<Float>>(this.k);
		List<IPoint<Float>> tmpCentroids = new ArrayList<IPoint<Float>>(this.k);
		boolean similar;
		double diff;

		do {
			oldCentroids.clear();
			oldCentroids.addAll(centroids);

			for (int i = 0; i < ITERATIONS; i++) {
				this.assignCentroids(points, centroids);
				centroids = this.computeCentroids(points);
			}

			runs++;

			// Look for similar centroids to finish k-means
			tmpCentroids.clear();
			tmpCentroids.addAll(centroids);
			for (IPoint<Float> old : oldCentroids) {
				for (IPoint<Float> tmp : tmpCentroids) {
					similar = true;
					for (int d = 0; d < this.dim; d++) {
						diff = old.get(d) - tmp.get(d);
						if (Math.abs(diff) > 0.0000001) {
							similar = false;
							break;
						}
					}
					if (similar) {
						tmpCentroids.remove(tmp);
						break;
					}
				}
			}
			// finish, if every old cendroid has a new similar centroid
		} while (tmpCentroids.size() > 0);
		Logger.logTrace(CLAZZ, "run() finished after " + runs * ITERATIONS
				+ " iterations. Check break condition after every "
				+ ITERATIONS + " iterations.");
	}

	public static void main(String[] args) {
		KMeansCLSimple kmeans = new KMeansCLSimple();

		List<IPoint<Float>> centroids = kmeans.initialize(2, 5);
		List<ICPoint<Float>> points = new Points(kmeans.getDim()).generate(
				kmeans.getK(), 1000, 1);

		// View input
		new Visualize().drawCPoints(1, points);
		waitForView();

		long start = System.currentTimeMillis();
		kmeans.run(points, centroids);
		long end = System.currentTimeMillis();

		System.out.println("Time: " + (end - start));

		// View clusters with centroid
		new Visualize().drawCPoints(1, points);
	}

	private static void waitForView() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
