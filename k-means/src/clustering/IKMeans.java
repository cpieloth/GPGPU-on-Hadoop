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
	 * Returns dimension of points.
	 * 
	 * @return
	 */
	public int getDim();

	/**
	 * Returns count of clusters.
	 * 
	 * @return
	 */
	public int getK();

	/**
	 * Creates random centroids and initialize required data.
	 * 
	 * @param dim
	 *            Dimension of point
	 * @param k
	 *            Number of centroids
	 * @return List of generated centroids.
	 */
	public List<IPoint> initialize(int dim, int k);

	/**
	 * Runs the K-Means implementation with the specified iterations.
	 * 
	 * @param points
	 *            Data input.
	 * @param centroids
	 *            Initial centroids.
	 * @param ITERATIONS
	 *            Number of iterations.
	 */
	public void run(List<ICPoint> points, List<IPoint> centroids,
			final int ITERATIONS);

	/**
	 * Runs the K-Means implementation with a break condition e.g. centroids are
	 * not changing anymore.
	 * 
	 * @param points
	 *            Data input.
	 * @param centroids
	 *            Initial centroids.
	 */
	public void run(List<ICPoint> points, List<IPoint> centroids);

	/**
	 * Assigns the nearest centroid for each point.
	 * 
	 * @param points
	 *            Points to assign centroids.
	 * @param centroids
	 *            Centroids to be assigned.
	 */
	public void assignCentroids(List<ICPoint> points, List<IPoint> centroids);

	/**
	 * Computes the new centroids of each cluster.
	 * 
	 * @param points
	 *            Points with assigned centroids.
	 * @return New centroids.
	 */
	public List<IPoint> computeCentroids(List<ICPoint> points);

}
