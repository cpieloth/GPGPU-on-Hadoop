package utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import clustering.CPoint;
import clustering.ICPoint;
import clustering.IPoint;
import clustering.Point;

public class Points {

	private int dim;

	public Points(int dim) {
		this.dim = dim;
	}

	public List<ICPoint> generate(final int K, final int COUNT,
			final int MAGNITUDE) {
		Random r = new Random();
		ArrayList<IPoint> centroids = new ArrayList<IPoint>(K);
		Point c;
		for (int i = 0; i < K; i++) {
			c = new Point(this.dim);
			for (int d = 0; d < this.dim; d++)
				c.set(d, r.nextFloat() * MAGNITUDE);
			centroids.add(c);
		}

		int i = 0;
		ArrayList<ICPoint> points = new ArrayList<ICPoint>(COUNT);
		ICPoint p;
		float val;
		Random sign = new Random();
		while (i < COUNT * 0.6) {
			for (IPoint ref : centroids) {
				p = new CPoint(this.dim);
				for (int d = 0; d < this.dim; d++) {
					if (sign.nextBoolean())
						val = ref.get(d) + r.nextFloat() * MAGNITUDE / 10;
					else
						val = ref.get(d) - r.nextFloat() * MAGNITUDE / 10;
					if (val < 0)
						val = 0;
					if (val > MAGNITUDE)
						val = MAGNITUDE;
					p.set(d, val);
					p.setCentroid(ref);
				}
				points.add(p);
				i++;
			}
		}

		while (i < COUNT) {
			p = new CPoint(this.dim);
			for (int d = 0; d < this.dim; d++)
				p.set(d, r.nextFloat() * MAGNITUDE);
			points.add(p);
			i++;
		}

		return points;
	}

	public static void print(List<IPoint> points) {
		for (IPoint p : points)
			System.out.println(p.toString());
	}
	
	public List<IPoint> extractCentroids(List<ICPoint> points) {
		HashSet<IPoint> centroids = new HashSet<IPoint>();
		for(ICPoint p : points)
			centroids.add(p.getCentroid());
		centroids.remove(null);
		return new ArrayList<IPoint>(centroids);
	}

	public List<ICPoint> copyPoints(List<ICPoint> points) {
		List<ICPoint> newPoints = new ArrayList<ICPoint>(points.size());
		ICPoint newPoint;
		for(ICPoint p : points) {
			newPoint = new CPoint(this.dim);
			for(int d = 0; d < this.dim; d++)
				newPoint.set(d, p.get(d));
			newPoints.add(newPoint);
		}
		return newPoints;
	}

}
