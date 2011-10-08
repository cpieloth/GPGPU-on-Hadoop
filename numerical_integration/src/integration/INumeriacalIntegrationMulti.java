package integration;

import java.util.List;

import mathFunction.IInterval;

import cl_util.ICLBufferedOperation;

public interface INumeriacalIntegrationMulti<T extends Number> extends INumeriacalIntegration<T>, ICLBufferedOperation<IInterval<T>> {
	
	public List<T> getIntegrals(int resolution);	

}
