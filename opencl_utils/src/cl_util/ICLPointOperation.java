package cl_util;

import java.util.List;

import clustering.ICPoint;
import clustering.IPoint;

public interface ICLPointOperation<T extends Number> extends ICLBufferedOperation<ICPoint<T>> {
	public void setNearestPoints();

	public void prepareNearestPoints(List<IPoint<T>> centroids);
}
