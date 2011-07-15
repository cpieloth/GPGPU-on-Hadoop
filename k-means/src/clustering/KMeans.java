package clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Level;

import lightLogger.Logger;
import utils.Points;
import utils.Visualize;

/**
 * Untested sequential and undistributed K-Means implementation.
 * 
 * @author christof
 * 
 * @param <T>
 * @param <K>
 */
public class KMeans implements IKMeans {

	private static final Class<KMeans> CLAZZ = KMeans.class;

	private static final int MAX_DISTANCE = Integer.MAX_VALUE;

	private int k, dim;
	
	private IKMeansBasic kBasic;

	@Override
	public List<IPoint> initialize(int dim, int k) {
		Logger.logTrace(CLAZZ, "initialize()");
		this.dim = dim;
		this.k = k;
		
		List<IPoint> centroids = new ArrayList<IPoint>(this.k);
		Random r = new Random();
		Point c;
		for (int i = 0; i < this.k; i++) {
			c = new Point(this.dim);
			for (int d = 0; d < this.dim; d++)
				c.set(d, r.nextDouble());
			centroids.add(c);
		}
		return centroids;
	}

	public void computeDistances(List<ICPoint> points, List<IPoint> centroids) {
		Logger.logTrace(CLAZZ, "computeDistances(" + points.size() + ", "
				+ centroids.size() + ")");
		double prevDist, dist;
		IPoint centroid;

		for (ICPoint p : points) {
			prevDist = MAX_DISTANCE;
			centroid = null;

			for (IPoint c : centroids) {
				dist = this.kBasic.computeDistance(p, c);
				if (dist < prevDist) {
					prevDist = dist;
					centroid = c;
				}
			}

			p.setCentroid(centroid);
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
			newCentroid = this.kBasic.computeCentroid(clusters.get(centroid));
			newCentroids.add(newCentroid);
		}

		return newCentroids;
	}

	

	@Override
	public int getK() {
		return this.k;
	}

	@Override
	public void run(IKMeansBasic kBasic, List<ICPoint> points,
			List<IPoint> centroids, final int ITERATIONS) {
		Logger.logTrace(CLAZZ, "run() - ITERATIONS: " + ITERATIONS);

		this.kBasic = kBasic;

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
		Visualize viz = new Visualize();

		this.kBasic = kBasic;

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

			viz.drawCPoints(1, points);
			waitForView();
			// finish, if every old cendroid has a new similar centroid
		} while (tmpCentroids.size() > 0);
		Logger.logTrace(CLAZZ, "run() finished after " + runs * ITERATIONS
				+ " iterations. Check break condition after every " + ITERATIONS + " iterations.");
	}

	public static void main(String[] args) {
		Logger.setLogMask(lightLogger.Level.DEFAULT.TRACE.getLevel().getValue());
		IKMeansBasic kBasic = new KMeansBasic(2);
//		KMeansBasicCL kBasic = new KMeansBasicCL(128);
//		kBasic.initialize(KMeansBasicCL.TYPES.CL_GPU);
		KMeans kmeans = new KMeans();
		List<IPoint> centroids = kmeans.initialize(kBasic.getDim(), 4);
		List<ICPoint> points = new Points(kBasic.getDim()).generate(kmeans.getK(),
				1000, 1);

		// View input
		new Visualize().drawCPoints(1, points);
		waitForView();

		long start = System.currentTimeMillis();
		kmeans.run(kBasic, points, centroids);
		long end = System.currentTimeMillis();
		
		System.out.println("Time: " + (end-start));

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
