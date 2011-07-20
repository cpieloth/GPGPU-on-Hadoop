package cl_util;

import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

import utils.Points;
import clustering.ICPoint;
import clustering.IPoint;

public class CLPointFloatTest {
	
	private static final float DELTA = 0;

	@Test
	public void testSetNearestPoints() {
		int DIM, COUNT, K;
		Points pHelper;
		List<ICPoint> points;
		List<ICPoint> pointsExpected;
		List<IPoint> centroids;
		CLPointFloat clPoint;
		CLInstance clInstance = new CLInstance(CLInstance.TYPES.CL_GPU);
		
		DIM = 7;
		K = 5;
		COUNT = CLPointFloat.MAX_BUFFER_SIZE / DIM;
		clPoint = new CLPointFloat(clInstance, DIM);
		pHelper = new Points(DIM);
				
		points = pHelper.generate(K, COUNT, 1);
		centroids = pHelper.extractCentroids(points);
		pointsExpected = pHelper.copyPoints(points);
		
		clPoint.prepareNearestPoints(centroids);
		for(ICPoint p : points)
			clPoint.put(p);
		clPoint.setNearestPoints();
		
		this.computeNearestPoints(pointsExpected, centroids);
		
		this.checkCentroids(points, pointsExpected);
		
		// resetBuffer
		clPoint.resetBuffer(points.size());
		for(ICPoint p : points)
			clPoint.put(p);
		clPoint.setNearestPoints();
		
		this.computeNearestPoints(pointsExpected, centroids);
		
		this.checkCentroids(points, pointsExpected);
		
		// resetBuffer
		clPoint.resetBuffer(points.size()/2);
		for(ICPoint p : points)
			clPoint.put(p);
		clPoint.setNearestPoints();
		
		this.computeNearestPoints(pointsExpected, centroids);
		
		this.checkCentroids(points, pointsExpected);
	}
	
	public void checkCentroids(List<ICPoint> points, List<ICPoint> pointsExpected) {
		int size = points.size();
		if(size != pointsExpected.size())
			fail("Size of points are not equal.");

		for(int i = 0; i < size; i++)
			assertArrayEquals(points.get(i).getCentroid().getDims(), pointsExpected.get(i).getCentroid().getDims(), DELTA);
	}
	
	public void computeNearestPoints(List<ICPoint> points, List<IPoint> centroids) {
		double prevDist, dist;
		IPoint centroid;

		for (ICPoint p : points) {
			prevDist = Float.MAX_VALUE;
			centroid = null;

			for (IPoint c : centroids) {
				dist = 0;
				for (int d = 0; d < p.getDim(); d++)
					dist += Math.pow(c.get(d) - p.get(d), 2);
				if (dist < prevDist) {
					prevDist = dist;
					centroid = c;
				}
			}

			p.setCentroid(centroid);
		}
	}

}
