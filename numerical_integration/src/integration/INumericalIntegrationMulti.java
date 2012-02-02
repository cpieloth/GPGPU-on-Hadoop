package integration;

import java.util.List;

import mathFunction.IInterval;

import cl_util.ICLBufferedOperation;

public interface INumericalIntegrationMulti<T extends Number> extends INumericalIntegration<T>, ICLBufferedOperation<IInterval<T>> {
	
	public List<T> getIntegrals(int resolution);	

}
