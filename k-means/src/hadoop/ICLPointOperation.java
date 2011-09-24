package hadoop;

import java.util.List;

import cl_util.ICLBufferedOperation;

public interface ICLPointOperation<T extends Number> extends ICLBufferedOperation<PointWritable> {
	public void setNearestPoints();

	public void prepareNearestPoints(List<PointWritable> centroids);
}
