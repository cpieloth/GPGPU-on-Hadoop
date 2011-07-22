package clustering;

/**
 * Represents a Point with a label.
 * 
 * @author christof
 * 
 * @param <T>
 * @param <K>
 */
public interface ICPoint<T extends Number> extends IPoint<T> {

	/**
	 * Returns the current centroid.
	 * 
	 * @return
	 */
	public IPoint<T> getCentroid();

	/**
	 * Sets the centroid.
	 * 
	 * @param centroid
	 *            Centroid
	 */
	public void setCentroid(IPoint<T> centroid);

}
