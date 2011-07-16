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
public interface IKMeansBasic {

	/**
	 * Returns the dimension. Each Point's dimension must be equal with this
	 * value.
	 * 
	 * @return
	 */
	public int getDim();
	
	/**
	 * Computes the distance between two n-dimensional points.
	 * @param p Point 1
	 * @param c Point 2
	 * @return Distance
	 */
	public float computeDistance(final IPoint p, final IPoint c);

	/**
	 * Computes the centroid of a cluster of points.
	 * @param points
	 * @return Centroid
	 */
	public IPoint computeCentroid(final List<ICPoint> points);
}
