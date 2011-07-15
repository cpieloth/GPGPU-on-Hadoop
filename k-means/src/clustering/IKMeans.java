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
	 * Returns the dimension. Each Point's dimension must be equal with this value.
	 * @return
	 */
	public int getDim();
	
	/**
	 * Return count of clusters
	 * @return
	 */
	public int getK();

	/**
	 * Create random centroids and initialize required data.
	 * 
	 * @return List of generated centroids.
	 */
	public List<IPoint> initialize();

	/**
	 * Runs the K-Means implementation with the specified iterations.
	 * 
	 * @param kmeans
	 *            Implementation of required basic methods.
	 * @param points
	 *            Data input.
	 * @param iterations
	 *            Number of iterations.
	 */
	public void run(IKMeansBasic kmeans, List<ICPoint> points,
			final int iterations);

	/**
	 * Runs the K-Means implementation with a break condition e.g. centroids are
	 * not changing anymore.
	 * 
	 * @param kmeans
	 *            Implementation of required basic methods.
	 * @param points
	 *            Data input.
	 */
	public void run(IKMeansBasic kmeans, List<ICPoint> points);
	
	// Possible Mapper: public void computeDistances(List<ILabeledPoint<T,K>> points, List<IPoint<T>> centroids);
	
	// Possible Reducer: public List<IPoint<T>> computeCentroids(List<ILabeledPoint<T,K>> points);
	
}
