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
	public void set(int dim, double val);

	/**
	 * Gets the value of one dimension.
	 * 
	 * @param dim
	 *            Dimesion
	 * @return Value
	 */
	public double get(int dim);

	/**
	 * Returns all values of the dimensions.
	 * 
	 * @return
	 */
	public double[] getDims();

	/**
	 * Returns the dimension size.
	 * 
	 * @return
	 */
	public int getDim();

}
