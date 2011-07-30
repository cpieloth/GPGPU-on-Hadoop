package integration;

/**
 * Data structure for an interval with a given resolution. Resolution is the
 * number of discrete values between begin and end.
 * 
 * @author christof
 * 
 * @param <T>
 *            Should be Double or Float
 */
public interface IInterval<T extends Number> {

	/**
	 * Returns the begin of the interval.
	 * 
	 * @return
	 */
	public T getBegin();

	/**
	 * Sets the begin of the interval.
	 * 
	 * @param begin
	 */
	public void setBegin(T begin);

	/**
	 * Returns the end of the interval.
	 * 
	 * @return
	 */
	public T getEnd();

	/**
	 * Sets the end of the interval.
	 * 
	 * @param end
	 */
	public void setEnd(T end);

	/**
	 * Returns the resolution of the interval.
	 * 
	 * @return
	 */
	public int getResolution();

	/**
	 * Sets the resolution of the interval.
	 * 
	 * @param resolution
	 */
	public void setResolution(int resolution);

}
