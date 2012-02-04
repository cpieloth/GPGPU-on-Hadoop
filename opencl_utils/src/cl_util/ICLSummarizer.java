package cl_util;

/**
 * Interface for a buffered parallel summation of numeric types.
 * 
 * @author Christof Pieloth
 * 
 * @param <T>
 */
public interface ICLSummarizer<T extends Number> extends
		ICLBufferedOperation<T> {

	/**
	 * Resets the internal intermediate sum and counters.
	 */
	public void resetResult();

	/**
	 * Calculate the final sum.
	 * 
	 * @return Sum of all items which are put since the last resetResult().
	 */
	public T getSum();
}
