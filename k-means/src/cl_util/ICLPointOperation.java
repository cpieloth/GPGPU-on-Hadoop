package cl_util;

import java.util.List;

import clustering.ICPoint;
import clustering.IPoint;

/**
 * Calculates the closest centroid for each point.
 * 
 * @author Christof Pieloth
 * 
 * @param <T>
 */
public interface ICLPointOperation<T extends Number> extends
		ICLBufferedOperation<ICPoint<T>> {

	/**
	 * Assigns the centroid to each point of the input list.
	 */
	public void setNearestPoints();

	/**
	 * Copies the centroids to OpenCL device.
	 * 
	 * @param centroids
	 */
	public void prepareNearestPoints(List<IPoint<T>> centroids);
}
