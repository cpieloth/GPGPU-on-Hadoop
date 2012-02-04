package integration;

import java.util.List;

import mathFunction.IInterval;

import cl_util.ICLBufferedOperation;

/**
 * Interface for a numerical integration for many intervals and one mathematical
 * function.
 * 
 * @author Christof Pieloth
 * 
 * @param <T>
 */

public interface INumericalIntegrationMulti<T extends Number> extends
		INumericalIntegration<T>, ICLBufferedOperation<IInterval<T>> {

	public List<T> getIntegrals(int resolution);

}
