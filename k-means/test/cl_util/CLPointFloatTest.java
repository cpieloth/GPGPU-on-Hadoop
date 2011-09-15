package cl_util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import utils.Points;
import clustering.ICPoint;
import clustering.IPoint;

public class CLPointFloatTest {
	
	// private static final float DELTA = 0.001f;

	@Test
	public void testSetNearestPoints() {
		int DIM, COUNT, K;
		Points pHelper;
		List<ICPoint<Float>> points;
		List<ICPoint<Float>> pointsExpected;
		List<IPoint<Float>> centroids;
		CLPointFloat clPoint;
		CLInstance clInstance = new CLInstance(CLInstance.TYPES.CL_GPU);
		
		DIM = 7;
		K = 5;
		clPoint = new CLPointFloat(clInstance, DIM);
		pHelper = new Points(DIM);
		COUNT = (int) (clPoint.getMaxBufferItems() / 4);
				
		points = pHelper.generate(K, COUNT, 1);
		centroids = pHelper.extractCentroids(points);
		pointsExpected = pHelper.copyPoints(points);
		
		clPoint.prepareNearestPoints(centroids);
		for(ICPoint<Float> p : points)
			clPoint.put(p);
		clPoint.setNearestPoints();
		
		this.computeNearestPoints(pointsExpected, centroids);
		
		this.checkCentroids(points, pointsExpected);
		
		// resetBuffer
		clPoint.resetBuffer(points.size());
		for(ICPoint<Float> p : points)
			clPoint.put(p);
		clPoint.setNearestPoints();
		
		this.computeNearestPoints(pointsExpected, centroids);
		
		this.checkCentroids(points, pointsExpected);
		
		// resetBuffer
		clPoint.resetBuffer(points.size()/2);
		for(ICPoint<Float> p : points)
			clPoint.put(p);
		clPoint.setNearestPoints();
		
		this.computeNearestPoints(pointsExpected, centroids);
		
		this.checkCentroids(points, pointsExpected);
	}
	
	private void checkCentroids(final List<ICPoint<Float>> points, final List<ICPoint<Float>> pointsExpected) {
		int size = points.size();
		if(size != pointsExpected.size())
			fail("Size of points are not equal.");

		for(int i = 0; i < size; i++)
			assertArrayEquals(points.get(i).getCentroid().getDims(), pointsExpected.get(i).getCentroid().getDims());
			// assertArrayEquals(points.get(i).getCentroid().getDims(), pointsExpected.get(i).getCentroid().getDims(), DELTA);
	}
	
	private void computeNearestPoints(final List<ICPoint<Float>> points, final List<IPoint<Float>> centroids) {
		double prevDist, dist;
		IPoint<Float> centroid;

		for (ICPoint<Float> p : points) {
			prevDist = Float.MAX_VALUE;
			centroid = null;

			for (IPoint<Float> c : centroids) {
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
	
	@Test
	public void testResetBuffer(){
		CLInstance clInstance = new CLInstance();
		clInstance.initialize(CLInstance.TYPES.CL_GPU);

		ICLBufferedOperation<ICPoint<Float>> clBufferedOp = new CLPointFloat(clInstance, 2);
		clBufferedOp.resetBuffer();
		assertArrayEquals(new int[]{clBufferedOp.getMaxBufferItems()}, new int[]{clBufferedOp.getCurrentMaxBufferItems()});
		
		int items = 2 * clBufferedOp.getMaxBufferItems();
		clBufferedOp.resetBuffer(items);
		assertArrayEquals(new int[]{clBufferedOp.getMaxBufferItems()}, new int[]{clBufferedOp.getCurrentMaxBufferItems()});
		
		items = clBufferedOp.getMaxBufferItems() / 2;
		clBufferedOp.resetBuffer(items);
		int currItems = clBufferedOp.getCurrentMaxBufferItems();
		if(!(items <= currItems && currItems <= clBufferedOp.getMaxBufferItems()))
			Assert.fail();
		
		items = 0;
		clBufferedOp.resetBuffer(items);
		currItems = clBufferedOp.getCurrentMaxBufferItems();
		if(!(items < currItems && currItems <= clBufferedOp.getMaxBufferItems()))
			Assert.fail();
	}

}
