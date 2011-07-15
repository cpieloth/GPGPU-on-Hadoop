package clustering;

/**
 * Represents a Point with a label.
 * 
 * @author christof
 * 
 * @param <T>
 * @param <K>
 */
public interface ICPoint extends IPoint {

	/**
	 * Returns the current centroid.
	 * 
	 * @return
	 */
	public IPoint getCentroid();

	/**
	 * Sets the centroid.
	 * 
	 * @param centroid
	 *            Centroid
	 */
	public void setCentroid(IPoint centroid);

}
