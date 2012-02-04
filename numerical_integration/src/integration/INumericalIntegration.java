package integration;

import mathFunction.IInterval;
import mathFunction.IMathFunction;

/**
 * Interface for a numerical integration for one interval and mathematical
 * function.
 * 
 * @author Christof Pieloth
 * 
 * @param <T>
 */
public interface INumericalIntegration<T extends Number> {

	public void setFunction(IMathFunction<T> function);

	public T getIntegral(IInterval<T> interval, int resolution);

}
