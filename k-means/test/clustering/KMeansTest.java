package clustering;

import static org.junit.Assert.assertArrayEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class KMeansTest {

	private KMeans kmeans;
	private Point c;
	private CPoint p;

	@Test
	public void testComputeDistance() {
		kmeans = new KMeans(1, 2);
		p = new CPoint(kmeans.getDim());
		p.set(0, 0);
		p.set(1, 0);

		c = new CPoint(kmeans.getDim());
		c.set(0, 3);
		c.set(1, 3);

		double dist1 = kmeans.computeDistance(p, c);
		double dist1Exp = Math.sqrt(18);

		kmeans = new KMeans(1, 5);
		p = new CPoint(kmeans.getDim());
		for (int d = 0; d < kmeans.getDim(); d++)
			p.set(d, 0);

		c = new Point(kmeans.getDim());
		for (int d = 0; d < kmeans.getDim(); d++)
			c.set(d, d);

		double dist2 = kmeans.computeDistance(p, c);
		double dist2Exp = Math.sqrt(30);

		assertArrayEquals(new double[] { dist1Exp, dist2Exp }, new double[] {
				dist1, dist2 }, 0.0001);
	}

	@Test
	public void testComputeCentroid() {
		kmeans = new KMeans(1, 2);
		List<ICPoint> points = new LinkedList<ICPoint>();

		p = new CPoint(kmeans.getDim());
		p.set(0, 1);
		p.set(1, 1);
		points.add(p);

		p = new CPoint(kmeans.getDim());
		p.set(0, 1);
		p.set(1, 4);
		points.add(p);

		p = new CPoint(kmeans.getDim());
		p.set(0, 4);
		p.set(1, 1);
		points.add(p);

		IPoint c = kmeans.computeCentroid(points);
		IPoint cExp = new Point(kmeans.getDim());
		cExp.set(0, 2);
		cExp.set(1, 2);

		assertArrayEquals(cExp.getDims(), c.getDims(), 0.0001);

		kmeans = new KMeans(1, 5);
		points.clear();

		for (int i = 0; i < 3; i++) {
			p = new CPoint(kmeans.getDim());
			for (int d = 0; d < kmeans.getDim(); d++)
				p.set(d, d + i * kmeans.getDim());
			points.add(p);
		}

		c = kmeans.computeCentroid(points);
		cExp = new Point(kmeans.getDim());
		cExp.set(0, 15 / 3);
		cExp.set(1, 18 / 3);
		cExp.set(2, 21 / 3);
		cExp.set(3, 24 / 3);
		cExp.set(4, 27 / 3);

		assertArrayEquals(cExp.getDims(), c.getDims(), 0.0001);
	}

}
