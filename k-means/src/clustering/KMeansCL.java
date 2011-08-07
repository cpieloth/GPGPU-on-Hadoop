package clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import lightLogger.Logger;
import cl_util.CLInstance;
import cl_util.CLPointFloat;
import cl_util.CLSummarizerFloat;
import cl_util.ICLPointOperation;
import cl_util.ICLSummarizer;

/**
 * Untested sequential and undistributed K-Means implementation. TODO use
 * CLInstance, CLFLoat, CLPointFloat
 * 
 * @author christof
 * 
 * @param <T>
 * @param <K>
 */
public class KMeansCL implements IKMeans<Float> {

	private static final Class<KMeansCL> CLAZZ = KMeansCL.class;

	private ICLSummarizer<Float> clFloat;
	private ICLPointOperation<Float> clPoint;

	private int k, dim;

	@Override
	public List<IPoint<Float>> initialize(int dim, int k, boolean generate) {
		Logger.logTrace(CLAZZ, "initialize()");

		this.dim = dim;
		this.k = k;

		CLInstance clInstance = new CLInstance(CLInstance.TYPES.CL_GPU);
		this.clFloat = new CLSummarizerFloat(clInstance);
		this.clPoint = new CLPointFloat(clInstance, dim);

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
		Logger.logTrace(CLAZZ, "computeDistances(" + points.size() + ", "
				+ centroids.size() + ")");

		this.clPoint.prepareNearestPoints(centroids);
		this.clPoint.resetBuffer(points.size());

		for (ICPoint<Float> p : points) {
			this.clPoint.put(p);
		}

		this.clPoint.setNearestPoints();
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
		int size = points.size();
		this.clFloat.resetBuffer(size);

		float sum;
		IPoint<Float> centroid = new Point(this.dim);

		for (int d = 0; d < this.dim; d++) {
			this.clFloat.resetResult();
			for (ICPoint<Float> p : points)
				this.clFloat.put(p.get(d));

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
