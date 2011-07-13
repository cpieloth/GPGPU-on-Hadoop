package clustering;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Untested sequential and undistributed K-Means implementation.
 * @author christof
 *
 * @param <T>
 * @param <K>
 */
// TODO test
public class KMeans<T, K> implements IKMeans<T, K>, IKMeansBasic<T, K> {

	private static final int MAX_DISTANCE = Integer.MAX_VALUE;

	private int k;

	public KMeans(int k) {
		this.k = k;
	}

	@Override
	public List<IPoint<T>> initialize() {
		List<IPoint<T>> centroids = new ArrayList<IPoint<T>>(k);
		for (int i = 0; i < k; i++) {
			// TODO generate random centroids
		}
		return centroids;
	}

	public void computeDistances(List<ILPoint<T, K>> points,
			List<IPoint<T>> centroids) {
		int prevDist;
		int dist;
		K centroid;

		for (ILPoint<T, K> p : points) {
			prevDist = MAX_DISTANCE;
			centroid = null;

			for (IPoint<T> c : centroids) {
				dist = computeDistances(p, c);
				if (dist < prevDist) {
					prevDist = dist;
					centroid = (K) c;
				}
			}

			p.setLabel(centroid);
		}
	}

	@Override
	public int computeDistances(IPoint<T> p, IPoint<T> c) {
		int dist = -1;
		// TODO computeDistances
		return dist;
	}

	public List<IPoint<T>> computeCentroids(List<ILPoint<T, K>> values) {
		List<IPoint<T>> centroids = new ArrayList<IPoint<T>>(k);
		IPoint<T> centroid = null;
		// TODO optimize, store intermediate in class. (undistributed impl.)
		HashSet<K> labels = new HashSet<K>();
		for (ILPoint<T, K> p : values)
			labels.add(p.getLabel());

		List<IPoint<T>> points = new LinkedList<IPoint<T>>();
		for (K label : labels) {
			points.clear();
			for (ILPoint<T, K> p : values) {
				if (p.getLabel() == label)
					points.add(p);
			}
			centroid = computeCentroid(points);
			centroids.add(centroid);
		}
		return centroids;
	}

	@Override
	public IPoint<T> computeCentroid(List<IPoint<T>> points) {
		IPoint<T> c = null;
		// TODO computeCentroid
		return c;
	}

	@Override
	public void run(IKMeansBasic<T, K> kmeans, List<ILPoint<T, K>> points,
			int iterations) {
		List<IPoint<T>> centroids = this.initialize();

		for (int i = 0; i < iterations; i++) {
			this.computeDistances(points, centroids);
			centroids = this.computeCentroids(points);
		}
	}

	@Override
	public void run(IKMeansBasic<T, K> kmeans, List<ILPoint<T, K>> points) {
		// TODO run(IKMeans kmeans, List<Point> points)
		this.run(kmeans, points, 100);
	}

	public static void main(String[] args) {
		KMeans<Integer, Integer> kmeans = new KMeans<Integer, Integer>(3);
		// TODO generateInput();
		List<ILPoint<Integer, Integer>> points = new LinkedList<ILPoint<Integer, Integer>>();
		kmeans.run(null, points, 1000);
	}
}
