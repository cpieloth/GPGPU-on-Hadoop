package utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import lightLogger.Logger;
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
public class KMeansData {

	private static final Class<KMeansData> CLAZZ = KMeansData.class;

	public static final String SEPARATOR = "\t";

	public static final String CHARSET = "UTF-8";

	public enum Argument {
		OUTPUT("output", 0), SIZE("size", 1), DIM("dimension", 2), CLUSTER(
				"clusters", 3), /*FS(Argument.DFS + "|" + Argument.LFS, 4)*/;

		public final String name;
		public final int index;

		private Argument(String name, int index) {
			this.name = name;
			this.index = index;
		}

		// public static final String DFS = "dfs";
		// public static final String LFS = "lfs";
	}

	public static void main(String[] args) {
		if (args.length < /*5*/4) {
			StringBuilder sb = new StringBuilder();
			sb.append("Arguments:");
			for (Argument arg : Argument.values())
				sb.append(" <" + arg.name + ">");
			System.out.println(sb.toString());
			System.exit(1);
		}

		final String output = args[Argument.OUTPUT.index];
		final int size = Integer.parseInt(args[Argument.SIZE.index]);
		final int dim = Integer.parseInt(args[Argument.DIM.index]);
		final int k = Integer.parseInt(args[Argument.CLUSTER.index]);
		// final String fs = args[Argument.FS.index];

		Points pHelper = new Points(dim);
		List<ICPoint<Float>> points = pHelper.generate(k, size, 1);

		 write(points, output, null);
	}

	public static boolean write(List<ICPoint<Float>> points, String file) {
		return write(points, file, SEPARATOR);
	}

	public static boolean write(List<ICPoint<Float>> points, String file,
			final String separator) {
		boolean res = true;
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			write(fos, points, separator);
		} catch (IOException e) {
			Logger.logError(CLAZZ, "Could not write input data.");
			e.printStackTrace();
			res = false;
		} finally {
			if (fos != null)
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return res;
	}

	private static void write(OutputStream os, List<ICPoint<Float>> points,
			final String separator) throws UnsupportedEncodingException,
			IOException {
		for (ICPoint<Float> p : points) {
			if (p.getCentroid() != null && separator != null) {
				os.write(Points.createString(p.getCentroid()).getBytes(CHARSET));
				os.write(separator.getBytes(CHARSET));
			}
			os.write(Points.createString(p).getBytes(CHARSET));
			os.write("\n".getBytes(CHARSET));
		}
	}

	public static List<IPoint<Float>> readIPoints(File file) {
		return readIPoints(file, SEPARATOR);
	}

	public static List<IPoint<Float>> readIPoints(File file,
			final String separator) {
		List<IPoint<Float>> points = new LinkedList<IPoint<Float>>();
		Scanner sc = null;
		try {
			sc = new Scanner(file);

			String line;
			String[] splits;
			IPoint<Float> point;
			while (sc.hasNext()) {
				line = sc.nextLine();
				splits = line.split(separator);

				point = Points.createPoint(splits[0]);
				points.add(point);
			}
		} catch (Exception e) {
			Logger.logError(CLAZZ, "Could not open/read file.");
			e.printStackTrace();
		} finally {
			if (sc != null)
				sc.close();
		}
		return points;
	}

	public static List<ICPoint<Float>> readICPoints(File file) {
		return readICPoints(file, SEPARATOR);
	}

	public static List<ICPoint<Float>> readICPoints(File file,
			final String separator) {
		List<ICPoint<Float>> points = new LinkedList<ICPoint<Float>>();
		Scanner sc = null;
		try {
			sc = new Scanner(file);

			String line;
			String[] splits;
			IPoint<Float> centroid = null;
			ICPoint<Float> point = null;
			while (sc.hasNext()) {
				line = sc.nextLine();
				splits = line.split(separator);
				if (splits.length == 1) {
					point = new CPoint(Points.createPoint(splits[0]));
					centroid = new Point(point.getDim());
				} else if (splits.length == 2) {
					point = new CPoint(Points.createPoint(splits[1]));
					centroid = Points.createPoint(splits[0]);
				}

				point.setCentroid(centroid);
				points.add(point);
			}
		} catch (Exception e) {
			Logger.logError(CLAZZ, "Could not open/read file.");
			e.printStackTrace();
		} finally {
			if (sc != null)
				sc.close();
		}
		return points;
	}

}
