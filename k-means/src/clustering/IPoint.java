package clustering;

/**
 * Represents a n-dimensional Point.
 * 
 * @author christof
 * 
 * @param <T>
 */
public interface IPoint {

	/**
	 * Sets the value for one dimension.
	 * 
	 * @param dim
	 *            Dimension
	 * @param val
	 *            Value
	 */
	public void set(int dim, float val);

	/**
	 * Gets the value of one dimension.
	 * 
	 * @param dim
	 *            Dimesion
	 * @return Value
	 */
	public float get(int dim);

	/**
	 * Returns all values of the dimensions.
	 * 
	 * @return
	 */
	public float[] getDims();

	/**
	 * Returns the dimension size.
	 * 
	 * @return
	 */
	public int getDim();

}
