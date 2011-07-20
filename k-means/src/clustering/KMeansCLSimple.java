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
public class KMeansCLSimple implements IKMeans {

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
	public List<IPoint> initialize(int dim, int k) {
		Logger.logTrace(CLAZZ, "initialize()");
		this.dim = dim;
		this.k = k;

		this.clInstance = new CLInstance(CLInstance.TYPES.CL_GPU);

		List<IPoint> centroids = new ArrayList<IPoint>(this.k);
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
	public void assignCentroids(List<ICPoint> points, List<IPoint> centroids) {
		Logger.logTrace(CLAZZ, "computeDistances(" + points.size() + ", "
				+ centroids.size() + ")");
		float prevDist, dist;
		IPoint centroid;

		for (ICPoint p : points) {
			prevDist = MAX_DISTANCE;
			centroid = null;

			for (IPoint c : centroids) {
				dist = this.computeDistance(p, c);
				if (dist < prevDist) {
					prevDist = dist;
					centroid = c;
				}
			}

			p.setCentroid(centroid);
		}
	}

	private float computeDistance(IPoint p, IPoint c) {
		try {
			CLKernel kernel = this.clInstance.getKernel(KERNEL_PREFIX,
					SQUARE_SUM);
			if (kernel == null)
				kernel = this.clInstance.loadKernel(KERNEL_PATH, SQUARE_SUM,
						KERNEL_PREFIX);

			CLContext context = this.clInstance.getContext();
			CLQueue cmdQ = this.clInstance.getQueue();

			// Prepate Data
			CLBuffer<FloatBuffer> pBuffer = context.createFloatBuffer(
					CLMem.Usage.Input, FloatBuffer.wrap(p.getDims()), true);
			CLBuffer<FloatBuffer> cBuffer = context.createFloatBuffer(
					CLMem.Usage.Input, FloatBuffer.wrap(c.getDims()), true);
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
	public List<IPoint> computeCentroids(List<ICPoint> points) {
		Logger.logTrace(CLAZZ, "computeCentroids(" + points.size() + ")");

		// Collect points per centroid
		HashMap<IPoint, List<ICPoint>> clusters = new HashMap<IPoint, List<ICPoint>>();
		for (ICPoint p : points) {
			if (!clusters.containsKey(p.getCentroid())) {
				clusters.put(p.getCentroid(), new LinkedList<ICPoint>());
			}
			clusters.get(p.getCentroid()).add(p);
		}

		// Compute new centroid
		IPoint newCentroid = null;
		List<IPoint> newCentroids = new ArrayList<IPoint>(this.k);
		for (IPoint centroid : clusters.keySet()) {
			newCentroid = this.computeCentroid(clusters.get(centroid));
			newCentroids.add(newCentroid);
		}

		return newCentroids;
	}

	private IPoint computeCentroid(List<ICPoint> points) {
		IPoint centroid = new Point(this.dim);
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
	public void run(List<ICPoint> points, List<IPoint> centroids,
			final int ITERATIONS) {
		Logger.logTrace(CLAZZ, "run() - ITERATIONS: " + ITERATIONS);

		for (int i = 0; i < ITERATIONS; i++) {
			this.assignCentroids(points, centroids);
			centroids = this.computeCentroids(points);
		}

		Logger.logTrace(CLAZZ, "run() finish");
	}

	@Override
	public void run(List<ICPoint> points, List<IPoint> centroids) {
		Logger.logTrace(CLAZZ, "run()");

		final int ITERATIONS = 5;
		int runs = 0;

		List<IPoint> oldCentroids = new ArrayList<IPoint>(this.k);
		List<IPoint> tmpCentroids = new ArrayList<IPoint>(this.k);
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
			for (IPoint old : oldCentroids) {
				for (IPoint tmp : tmpCentroids) {
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

		List<IPoint> centroids = kmeans.initialize(2, 5);
		List<ICPoint> points = new Points(kmeans.getDim()).generate(
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
