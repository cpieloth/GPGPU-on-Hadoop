package utils;

import java.io.File;
import java.util.List;

import lightLogger.Level;
import lightLogger.Logger;
import stopwatch.StopWatch;
import clustering.ICPoint;
import clustering.IKMeans;
import clustering.IPoint;
import clustering.KMeans;
import clustering.KMeansCL;

public class KMeansStarter {

	public static final Level TIME_LEVEL = new Level(128, "TIME");

	private static final Class<?> CLAZZ = KMeansStarter.class;

	public enum Argument {
		INPUT("input", 0), CENTROIDS("centroids", 1), OUTPUT("output", 2), TYPE(
				Argument.CPU + "|" + Argument.OCL, 3);

		public final String name;
		public final int index;

		private Argument(String name, int index) {
			this.name = name;
			this.index = index;
		}

		public static final String CPU = "cpu";
		public static final String OCL = "ocl";

	}

	public static void main(String[] args) {
		if (args.length < 4) {
			StringBuilder sb = new StringBuilder();
			sb.append("Arguments:");
			for (Argument arg : Argument.values())
				sb.append(" <" + arg.name + ">");
			System.out.println(sb.toString());
			System.exit(1);
		}

		final String iFile = args[Argument.INPUT.index];
		final String cFile = args[Argument.CENTROIDS.index];
		final String oFile = args[Argument.OUTPUT.index];
		final String type = args[Argument.TYPE.index];

		Logger.logInfo(CLAZZ, "Read input file ...");
		List<ICPoint<Float>> points = KMeansData.readICPoints(new File(iFile));
		Logger.logInfo(CLAZZ, "Read center file ...");
		List<IPoint<Float>> centroids = KMeansData.readIPoints(new File(cFile));

		if (points.isEmpty() || centroids.isEmpty()) {
			Logger.logError(CLAZZ, "Empty points or centroids!");
			System.exit(1);
		}

		if (points.get(0).getDim() != centroids.get(0).getDim()) {
			Logger.logError(CLAZZ, "Different dimensions!");
			System.exit(1);
		}
		int dim = points.get(0).getDim();

		StopWatch sw = new StopWatch("time" + type + "=", ";");
		sw.start();

		IKMeans<Float> kmeans = null;
		if (Argument.CPU.equals(type))
			kmeans = new KMeans();
		else if (Argument.OCL.equals(type))
			kmeans = new KMeansCL();
		else {
			Logger.logError(CLAZZ, "Unknown type");
			System.exit(1);
		}
		kmeans.initialize(dim, centroids.size(), false);
		
		StopWatch swCompute = new StopWatch("timeCompute" + type + "=", ";");
		swCompute.start();
		
		kmeans.run(points, centroids);

		swCompute.stop();
		sw.stop();
		Logger.log(TIME_LEVEL, CLAZZ, swCompute.getTimeString());
		Logger.log(TIME_LEVEL, CLAZZ, sw.getTimeString());

		KMeansData.writeToLFS(points, oFile);
	}
}
