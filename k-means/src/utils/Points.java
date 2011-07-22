package utils;

import hadoop.PointInputFormat;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import lightLogger.Logger;
import clustering.CPoint;
import clustering.ICPoint;
import clustering.IPoint;
import clustering.Point;

public class Points {
	
	private final static Class<Points> CLAZZ = Points.class;

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
		while (i < COUNT * 0.6) {
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

	public static List<IPoint<Float>> readIPoints(File file) {
		List<IPoint<Float>> points = new LinkedList<IPoint<Float>>();
		Scanner sc = null;
		try {
			sc = new Scanner(file);

			String line;
			String[] splits;
			IPoint<Float> point;
			while (sc.hasNext()) {
				line = sc.nextLine();
				splits = line.split("\t");
				
				point = PointInputFormat.createPointWritable(splits[0]);
				points.add(point);
			}
		} catch (Exception e) {
			Logger.logError(CLAZZ, "Could not open/read file.");
			e.printStackTrace();
		} finally {
			if(sc != null)
				sc.close();
		}
		return points;
	}

	public static List<ICPoint<Float>> readICPoints(File file, final String separator) {
		List<ICPoint<Float>> points = new LinkedList<ICPoint<Float>>();
		Scanner sc = null;
		try {
			sc = new Scanner(file);

			String line;
			String[] splits;
			IPoint<Float> centroid;
			ICPoint<Float> point;
			while (sc.hasNext()) {
				line = sc.nextLine();
				splits = line.split(separator);
				
				centroid = PointInputFormat.createPointWritable(splits[0]);
				point = new CPoint(PointInputFormat.createPointWritable(splits[1]));
				point.setCentroid(centroid);
				points.add(point);
			}
		} catch (Exception e) {
			Logger.logError(CLAZZ, "Could not open/read file.");
			e.printStackTrace();
		} finally {
			if(sc != null)
				sc.close();
		}
		return points;
	}

	public static void print(List<IPoint<Float>> points) {
		for (IPoint<Float> p : points)
			System.out.println(p.toString());
	}

	public List<IPoint<Float>> extractCentroids(List<ICPoint<Float>> points) {
		HashSet<IPoint<Float>> centroids = new HashSet<IPoint<Float>>();
		for (ICPoint<Float> p : points)
			centroids.add(p.getCentroid());
		centroids.remove(null);
		return new ArrayList<IPoint<Float>>(centroids);
	}

	public List<ICPoint<Float>> copyPoints(List<ICPoint<Float>> points) {
		List<ICPoint<Float>> newPoints = new ArrayList<ICPoint<Float>>(points.size());
		ICPoint<Float> newPoint;
		for (ICPoint<Float> p : points) {
			newPoint = new CPoint(this.dim);
			for (int d = 0; d < this.dim; d++)
				newPoint.set(d, p.get(d));
			newPoints.add(newPoint);
		}
		return newPoints;
	}

}
