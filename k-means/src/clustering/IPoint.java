package clustering;

/**
 * Represents a n-dimensional Point.
 * 
 * @author Christof Pieloth
 * 
 * @param <T>
 */
public interface IPoint<T extends Number> {

	/**
	 * Sets the value for one dimension.
	 * 
	 * @param dim
	 *            Dimension
	 * @param val
	 *            Value
	 */
	public void set(int dim, T val);

	/**
	 * Gets the value of one dimension.
	 * 
	 * @param dim
	 *            Dimesion
	 * @return Value
	 */
	public T get(int dim);

	/**
	 * Returns all values of the dimensions.
	 * 
	 * @return
	 */
	public T[] getDims();

	/**
	 * Returns the dimension size.
	 * 
	 * @return
	 */
	public int getDim();

}
