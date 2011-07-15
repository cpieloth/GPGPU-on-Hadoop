package clustering;

import java.util.List;

/**
 * Interface for a runnable K-Means implementation.
 * 
 * @author christof
 * 
 * @param <T>
 * @param <K>
 */
public interface IKMeans {

	/**
	 * Return count of clusters
	 * 
	 * @return
	 */
	public int getK();

	/**
	 * Create random centroids and initialize required data.
	 * @param dim Dimension of point
	 * @param k Number of centroids
	 * @return List of generated centroids.
	 */
	public List<IPoint> initialize(int dim, int k);

	/**
	 * Runs the K-Means implementation with the specified iterations.
	 * 
	 * @param kmeans
	 *            Implementation of required basic methods.
	 * @param points
	 *            Data input.
	 * @param centroids
	 *            Initial centroids.
	 * @param iterations
	 *            Number of iterations.
	 */
	public void run(IKMeansBasic kmeans, List<ICPoint> points,
			List<IPoint> centroids, final int iterations);

	/**
	 * Runs the K-Means implementation with a break condition e.g. centroids are
	 * not changing anymore.
	 * 
	 * @param kmeans
	 *            Implementation of required basic methods.
	 * @param points
	 *            Data input.
	 * @param centroids
	 *            Initial centroids.
	 */
	public void run(IKMeansBasic kmeans, List<ICPoint> points,
			List<IPoint> centroids);

	// Possible Mapper: public void computeDistances(List<ILabeledPoint<T,K>>
	// points, List<IPoint<T>> centroids);

	// Possible Reducer: public List<IPoint<T>>
	// computeCentroids(List<ILabeledPoint<T,K>> points);

}
