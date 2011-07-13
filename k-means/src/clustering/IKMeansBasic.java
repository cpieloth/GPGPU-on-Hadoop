package clustering;

import java.util.List;

/**
 * Interface for atomic basic methods of K-Means.
 * 
 * @author christof
 * 
 * @param <T>
 * @param <K>
 */
public interface IKMeansBasic<T, K> {

	/**
	 * Computes the distance between two n-dimensional points.
	 * @param p Point 1
	 * @param c Point 2
	 * @return Distance
	 */
	public int computeDistances(IPoint<T> p, IPoint<T> c);

	/**
	 * Computes the centroid of a cluster of points.
	 * @param points
	 * @return Centroid
	 */
	public IPoint<T> computeCentroid(List<IPoint<T>> points);
}
