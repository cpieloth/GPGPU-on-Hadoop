package clustering;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import lightLogger.Logger;
import utils.Points;
import utils.Visualize;
import cl_util.CLFloat;
import cl_util.CLInstance;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLException;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLMem;

/**
 * Untested sequential and undistributed K-Means implementation.
 * TODO use CLInstance, CLFLoat, CLPointFloat
 * 
 * @author christof
 * 
 * @param <T>
 * @param <K>
 */
public class KMeansCL extends KMeansCLBasic implements IKMeans {

	private static final Class<KMeansCL> CLAZZ = KMeansCL.class;

	private static final int CD_MAX_POINTS = 10240;

	private CLFloat clFloat;

	private int k, dim;

	@Override
	public List<IPoint> initialize(int dim, int k) {
		Logger.logTrace(CLAZZ, "initialize()");
		this.clFloat = new CLFloat(new CLInstance(CLInstance.TYPES.CL_GPU));

		this.dim = dim;
		this.k = k;

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

	public long copyTime = 0;

	public void computeDistances(List<ICPoint> points, List<IPoint> centroids) {
		Logger.logTrace(CLAZZ, "computeDistances(" + points.size() + ", "
				+ centroids.size() + ")");

		long start, end;

		final int BUFFER_ITEMS = points.size() < CD_MAX_POINTS ? points.size()
				: CD_MAX_POINTS;
		final int BUFFER_SIZE = BUFFER_ITEMS * this.dim;
		float[] buffer = new float[BUFFER_SIZE];
		int b = 0;

		try {
			// Prepare buffer
			CLBuffer<FloatBuffer> pBuffer = context.createFloatBuffer(
					CLMem.Usage.Input, BUFFER_SIZE);
			CLBuffer<FloatBuffer> cBuffer = context.createFloatBuffer(
					CLMem.Usage.Input, centroids.size() * this.dim);
			CLBuffer<FloatBuffer> rBuffer = context.createFloatBuffer(
					CLMem.Usage.InputOutput, BUFFER_SIZE);

			start = System.currentTimeMillis();
			for (IPoint c : centroids) {
				for (int d = 0; d < this.dim; d++)
					buffer[b * this.dim + d] = c.get(d);
				b++;

			}
			cBuffer.write(cmdQ, 0, this.dim * b, FloatBuffer.wrap(buffer),
					true, new CLEvent[0]);
			end = System.currentTimeMillis();
			copyTime += (end - start);
			// cmdQ.finish();

			CLKernel kernel = this.kernels.get(DISTANCES);

			ICPoint[] bufferPoints = new ICPoint[BUFFER_ITEMS];
			b = 0;

			Iterator<ICPoint> it = points.iterator();
			ICPoint p;
			IPoint centroid;

			start = System.currentTimeMillis();
			while (it.hasNext()) {
				p = it.next();

				if (b < BUFFER_ITEMS) {
					// Fill buffer
					bufferPoints[b] = p;
					for (int d = 0; d < this.dim; d++)
						buffer[b * this.dim + d] = p.get(d);
					b++;
				}
				if (b >= BUFFER_ITEMS || !it.hasNext()) {
					pBuffer.write(cmdQ, 0, this.dim * b,
							FloatBuffer.wrap(buffer), true, new CLEvent[0]);

					end = System.currentTimeMillis();
					copyTime += (end - start);
					cmdQ.finish();

					// compute distances
					kernel.setArg(0, rBuffer);
					kernel.setArg(1, pBuffer);
					kernel.setArg(2, b);
					kernel.setArg(3, cBuffer);
					kernel.setArg(4, centroids.size());
					kernel.setArg(5, this.dim);

					kernel.enqueueNDRange(cmdQ, new int[] { b }, new CLEvent[0]);
					cmdQ.finish();

					FloatBuffer res = ByteBuffer
							.allocateDirect(b * this.dim * SIZEOF_CL_FLOAT)
							.order(context.getByteOrder()).asFloatBuffer();

					rBuffer.read(cmdQ, 0, b * this.dim, res, true,
							new CLEvent[0]);

					// event.waitFor();
					cmdQ.finish();
					res.rewind();

					for (int i = 0; i < b; i++) {
						centroid = new Point(this.dim);
						for (int d = 0; d < this.dim; d++) {
							centroid.set(d, res.get());
						}
						bufferPoints[i].setCentroid(centroid);
					}

					b = 0;
					start = System.currentTimeMillis();
				}
			}
		} catch (CLException err) {
			Logger.logError(CLAZZ, "OpenCL error:\n" + err.getMessage() + "():"
					+ err.getCode());
			err.printStackTrace();
		} catch (Exception err) {
			Logger.logError(CLAZZ, "Error:\n" + err.getMessage() + "()");
			err.printStackTrace();
		}
	}

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

	public IPoint computeCentroid(List<ICPoint> points) {
		int size = points.size();
		// this.clFloat.resetBuffer(size);

		float sum;
		IPoint centroid = new Point(this.dim);

		for (int d = 0; d < this.dim; d++) {
			this.clFloat.resetResult();

			for (IPoint p : points)
				this.clFloat.add(p.get(d));

			sum = this.clFloat.getSum();
			centroid.set(d, sum / (float) size);
		}
		return centroid;
	}

	@Override
	public int getK() {
		return this.k;
	}

	public int getDim() {
		return this.dim;
	}

	@Override
	public void run(IKMeansBasic kBasic, List<ICPoint> points,
			List<IPoint> centroids, final int ITERATIONS) {
		Logger.logTrace(CLAZZ, "run() - ITERATIONS: " + ITERATIONS);

		// this.kBasic = kBasic;

		for (int i = 0; i < ITERATIONS; i++) {
			this.computeDistances(points, centroids);
			centroids = this.computeCentroids(points);
		}

		Logger.logTrace(CLAZZ, "run() finish");
	}

	@Override
	public void run(IKMeansBasic kBasic, List<ICPoint> points,
			List<IPoint> centroids) {
		Logger.logTrace(CLAZZ, "run()");

		// this.kBasic = kBasic;

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
				this.computeDistances(points, centroids);
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
		KMeansCL kmeans = new KMeansCL();
		if (!kmeans.initialize(KMeansCLBasic.TYPES.CL_GPU))
			return;
		List<IPoint> centroids = kmeans.initialize(2, 5);
		Points.print(centroids);
		List<ICPoint> points = new Points(kmeans.getDim()).generate(
				kmeans.getK(), 1000, 1);

		// View input
		new Visualize().drawCPoints(1, points);
		waitForView();

		long start = System.currentTimeMillis();
		kmeans.run(null, points, centroids);
		long end = System.currentTimeMillis();

		System.out.println("Time: " + (end - start));
		System.out.println("Copy time (without CLFLoat): " + kmeans.copyTime);

		// View clusters with centroid
		new Visualize().drawCPoints(1, points);
	}

	private static void waitForView() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
