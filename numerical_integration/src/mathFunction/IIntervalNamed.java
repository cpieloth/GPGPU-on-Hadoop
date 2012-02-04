package mathFunction;

/**
 * Named interval.
 * 
 * @author Christof Pieloth
 *
 * @param <K>
 * @param <T>
 */
public interface IIntervalNamed<K, T extends Number> extends IInterval<T> {

	/**
	 * Returns the resolution of the interval.
	 * 
	 * @return
	 */
	public K getIdentifier();

	/**
	 * Sets the identifier of the interval.
	 * 
	 * @param resolution
	 */
	public void setIdentifier(K identifier);
	
}
