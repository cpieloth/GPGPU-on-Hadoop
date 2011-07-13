package clustering;

/**
 * Represents a Point with a label.
 * 
 * @author christof
 * 
 * @param <T>
 * @param <K>
 */
public interface ILPoint<T, K> extends IPoint<T> {

	/**
	 * Returns the label.
	 * 
	 * @return
	 */
	public K getLabel();

	/**
	 * Sets the label.
	 * 
	 * @param label
	 *            Label
	 */
	public void setLabel(K label);

}
