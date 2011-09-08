package clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import lightLogger.Logger;

/**
 * Untested sequential and undistributed K-Means implementation.
 * 
 * @author christof
 */
public class KMeans implements IKMeans<Float> {

	private static final Class<KMeans> CLAZZ = KMeans.class;

	private static final float MAX_DISTANCE = Float.MAX_VALUE;

	private int k = 0, dim = 0;

	@Override
	public List<IPoint<Float>> initialize(int dim, int k, boolean generate) {
		Logger.logTrace(CLAZZ, "initialize()");
		this.dim = dim;
		this.k = k;

		List<IPoint<Float>> centroids = null;
		if (generate) {
			centroids = new ArrayList<IPoint<Float>>(this.k);
			Random r = new Random();
			Point c;
			for (int i = 0; i < this.k; i++) {
				c = new Point(this.dim);
				for (int d = 0; d < this.dim; d++)
					c.set(d, r.nextFloat());
				centroids.add(c);
			}
		}
		return centroids;
	}

	@Override
	public void assignCentroids(List<ICPoint<Float>> points,
			List<IPoint<Float>> centroids) {
		Logger.logTrace(CLAZZ, "assignCentroids(" + points.size() + ", "
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

	private float computeDistance(final IPoint<Float> p, final IPoint<Float> c) {
		float dist = 0;
		for (int d = 0; d < p.getDim(); d++)
			dist += Math.pow(c.get(d) - p.get(d), 2);
		return (float) Math.sqrt(dist);
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

	private IPoint<Float> computeCentroid(final List<ICPoint<Float>> points) {
		Logger.logTrace(CLAZZ,
				"computeCentroid() - points.size(): " + points.size());
		Point c = new Point(this.dim);

		Iterator<ICPoint<Float>> it = points.iterator();
		IPoint<Float> p = it.next();

		for (int d = 0; d < this.dim; d++)
			c.set(d, p.get(d));

		while (it.hasNext()) {
			p = it.next();
			for (int d = 0; d < this.dim; d++)
				c.set(d, c.get(d) + p.get(d));
		}

		int n = points.size();
		for (int d = 0; d < this.dim; d++)
			c.set(d, c.get(d) / n);

		return c;
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

}
