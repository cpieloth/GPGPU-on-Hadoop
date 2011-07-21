package clustering;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import lightLogger.Logger;
import utils.Points;
import utils.Visualize;
import cl_util.CLFloat;
import cl_util.CLInstance;
import cl_util.CLPointFloat;

/**
 * Untested sequential and undistributed K-Means implementation.
 * TODO use CLInstance, CLFLoat, CLPointFloat
 * 
 * @author christof
 * 
 * @param <T>
 * @param <K>
 */
public class KMeansCL implements IKMeans {

	private static final Class<KMeansCL> CLAZZ = KMeansCL.class;

	private CLFloat clFloat;
	private CLPointFloat clPoint;

	private int k, dim;

	@Override
	public List<IPoint> initialize(int dim, int k) {
		Logger.logTrace(CLAZZ, "initialize()");
		
		this.dim = dim;
		this.k = k;
		
		CLInstance clInstance = new CLInstance(CLInstance.TYPES.CL_GPU);
		this.clFloat = new CLFloat(clInstance);
		this.clPoint = new CLPointFloat(clInstance, dim);
		
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

		this.clPoint.prepareNearestPoints(centroids);
		this.clPoint.resetBuffer(points.size());
		
		for(ICPoint p : points) {
			this.clPoint.put(p);
		}
		
		this.clPoint.setNearestPoints();
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
		int size = points.size();
		this.clFloat.resetBuffer(size);

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

	@Override
	public int getDim() {
		return this.dim;
	}

	@Override
	public void run(List<ICPoint> points,
			List<IPoint> centroids, final int ITERATIONS) {
		Logger.logTrace(CLAZZ, "run() - ITERATIONS: " + ITERATIONS);

		for (int i = 0; i < ITERATIONS; i++) {
			this.assignCentroids(points, centroids);
			centroids = this.computeCentroids(points);
		}

		Logger.logTrace(CLAZZ, "run() finish");
	}

	@Override
	public void run(List<ICPoint> points,
			List<IPoint> centroids) {
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
		KMeansCL kmeans = new KMeansCL();
		
		List<IPoint> centroids = kmeans.initialize(2, 5);
		List<ICPoint> points; /* = new Points(kmeans.getDim()).generate(
				kmeans.getK(), 1000, 1);*/
		
		centroids = Points.readIPoints(new File("/home/christof/Documents/kmeans-data/centroids"));
		points = Points.readICPoints(new File("/home/christof/Documents/kmeans-data/part-m-00000"), "\t");

		// View centroids
		new Visualize().drawPoints(1, centroids);
		waitForView();
		
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
