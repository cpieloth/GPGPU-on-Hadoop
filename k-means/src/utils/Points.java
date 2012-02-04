package utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import clustering.CPoint;
import clustering.ICPoint;
import clustering.IPoint;
import clustering.Point;

/**
 * Helper class to generate points and transform data.
 * 
 * @author Christof Pieloth
 * 
 */
public class Points {

	private int dim;

	public Points(int dim) {
		this.dim = dim;
	}

	public List<ICPoint<Float>> generate(final int K, final int COUNT,
			final int MAGNITUDE) {
		Random r = new Random();
		ArrayList<IPoint<Float>> centroids = new ArrayList<IPoint<Float>>(K);
		Point c;
		for (int i = 0; i < K; i++) {
			c = new Point(this.dim);
			for (int d = 0; d < this.dim; d++)
				c.set(d, r.nextFloat() * MAGNITUDE);
			centroids.add(c);
		}

		int i = 0;
		ArrayList<ICPoint<Float>> points = new ArrayList<ICPoint<Float>>(COUNT);
		ICPoint<Float> p;
		float val;
		Random sign = new Random();
		while (i < COUNT * 0.6 && centroids.size() > 0) {
			for (IPoint<Float> ref : centroids) {
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

	public static void print(List<IPoint<Float>> points) {
		for (IPoint<Float> p : points)
			System.out.println(p.toString());
	}
	
	public static String createString(IPoint<Float> value) {
		StringBuilder sb = new StringBuilder();
		for (int d = 0; d < value.getDim(); d++)
			sb.append(value.get(d) + ";");
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
	
	public static IPoint<Float> createPoint(String line) {
		String[] splits = line.split(";");
		Point value = new Point(splits.length);
		for (int d = 0; d < splits.length; d++)
			value.set(d, Float.parseFloat(splits[d]));
		return value;
	}

	public List<IPoint<Float>> extractCentroids(List<ICPoint<Float>> points) {
		HashSet<IPoint<Float>> centroids = new HashSet<IPoint<Float>>();
		for (ICPoint<Float> p : points)
			centroids.add(p.getCentroid());
		centroids.remove(null);
		return new ArrayList<IPoint<Float>>(centroids);
	}

	public List<ICPoint<Float>> copyPoints(List<ICPoint<Float>> points) {
		List<ICPoint<Float>> newPoints = new ArrayList<ICPoint<Float>>(
				points.size());
		ICPoint<Float> newPoint;
		for (ICPoint<Float> p : points) {
			newPoint = new CPoint(this.dim);
			for (int d = 0; d < this.dim; d++)
				newPoint.set(d, p.get(d));
			newPoints.add(newPoint);
		}
		return newPoints;
	}
	
	public static IPoint<Float> copyPoint(IPoint<Float> point) {
		IPoint<Float> p = new Point(point.getDim());
		for(int i = 0; i < point.getDim(); ++i)
			p.set(i, point.get(i));
		return p;
	}

}
