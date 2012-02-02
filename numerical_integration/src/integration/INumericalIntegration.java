package integration;

import mathFunction.IInterval;
import mathFunction.IMathFunction;

public interface INumericalIntegration<T extends Number> {
	
	public void setFunction(IMathFunction<T> function);
	
	public T getIntegral(IInterval<T> interval, int resolution);

}
