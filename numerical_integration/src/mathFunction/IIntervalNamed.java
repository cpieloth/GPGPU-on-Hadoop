package mathFunction;

public interface IIntervalNamed<K, T extends Number> extends IInterval<T> {

	/**
	 * Returns the resolution of the interval.
	 * 
	 * @return
	 */
	public K getIdentifier();

	/**
	 * Sets the resolution of the interval.
	 * 
	 * @param resolution
	 */
	public void setIdentifier(K identifier);
	
}
