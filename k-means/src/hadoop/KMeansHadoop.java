package hadoop;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import lightLogger.Level;
import lightLogger.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FsUrlStreamHandlerFactory;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import stopwatch.StopWatch;
import utils.Points;
import clustering.ICPoint;
import clustering.IPoint;
import clustering.KMeans;

public class KMeansHadoop extends Configured implements Tool {

	public static final Level TIME_LEVEL = new Level(128, "TIME");

	public static final String PRE_MAPPHASE = "mapPhaseTime=";
	public static final String PRE_MAPMETHOD = "mapMethodTime=";
	public static final String PRE_REDUCEPHASE = "reducePhaseTime=";
	public static final String PRE_REDUCEMETHOD = "reduceMethodTime=";
	public static final String SUFFIX = StopWatch.SUFFIX;

	public static final int SUCCESS = 0;
	public static final int FAILURE = 1;

	public static final int INAME = 0;
	public static final int IINPUT = 1;
	public static final int IOUTPUT = 2;
	public static final int ICENTROIDS = 3;
	public static final int IITERATIONS = 4;
	public static final int IIMPL = 5;

	public static final String OCL = "ocl";
	public static final String CPU = "cpu";

	public static final String CHARSET = "UTF-8";

	public static String OUTPUT;

	public static void main(String[] args) throws Exception {
		GenericOptionsParser gop = new GenericOptionsParser(args);
		String[] rArgs = gop.getRemainingArgs();
		if (rArgs.length < 6) {
			System.out
					.println("Arguments: <Jobname> <Input> <Output> <Centroids> <Iterations> <ocl | cpu>");
			System.exit(FAILURE);
		}
		int res;
		OUTPUT = rArgs[IOUTPUT];
		final int ITERATIONS = Integer.parseInt(rArgs[IITERATIONS]);
		String centroids = rArgs[ICENTROIDS];
		int i = 0;

		// load HDFS handler, only once!
		FsUrlStreamHandlerFactory factory = new org.apache.hadoop.fs.FsUrlStreamHandlerFactory();
		java.net.URL.setURLStreamHandlerFactory(factory);

		generateInput(rArgs[IINPUT], rArgs[ICENTROIDS], gop.getConfiguration());

		StopWatch sw = new StopWatch("totalTime=", ";");
		sw.start();

		do {
			rArgs[IOUTPUT] = centroids + "-" + (i + 1);
			res = ToolRunner.run(gop.getConfiguration(), new KMeansHadoop(),
					rArgs);
			rArgs[ICENTROIDS] = centroids + "-" + (i + 1);
			i++;
		} while (i < ITERATIONS && res == SUCCESS);

		// collect clusters in a final map
		rArgs[IOUTPUT] = OUTPUT;
		res = ToolRunner.run(gop.getConfiguration(), new KMeansHadoop(), rArgs);

		sw.stop();
		Logger.log(TIME_LEVEL, KMeansHadoop.class, sw.getTimeString());

		System.exit(res);
	}

	private static void generateInput(String fPoints, String fCentroids,
			Configuration configuration) {
		KMeans kmeans = new KMeans();
		List<IPoint<Float>> centroids = kmeans.initialize(2, 10);
		Points pHelper = new Points(kmeans.getDim());
		List<ICPoint<Float>> points = pHelper.generate(kmeans.getK(), 1000, 1);

		FileSystem fs = null;
		FSDataOutputStream fos = null;
		try {
			fs = FileSystem.get(configuration);

			fos = fs.create(new Path(fPoints + "/points"));
			for (ICPoint<Float> p : points) {
				fos.write(PointOutputFormat.createString(p).getBytes(CHARSET));
				fos.write("\n".getBytes(CHARSET));
			}
			fos.close();

			fos = fs.create(new Path(fCentroids + "/centroids"));
			for (IPoint<Float> p : centroids) {
				fos.write(PointOutputFormat.createString(p).getBytes(CHARSET));
				fos.write("\n".getBytes(CHARSET));
			}
			fos.close();

			fs.close();
		} catch (IOException e) {
			Logger.logError(KMeansHadoop.class,
					"Could not generate input data.");
			e.printStackTrace();
		} finally {
			if (fs != null)
				try {
					fs.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (fos != null)
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

	}

	@Override
	public int run(String[] args) throws Exception {
		Job job = new Job(this.getConf());

		job.setJobName(args[INAME]);

		job.setJarByClass(KMeansHadoop.class);

		if (CPU.equals(args[IIMPL])) {
			job.setMapperClass(KMMapperReducer.KMapper.class);
			if (args[IOUTPUT] != OUTPUT)
				job.setReducerClass(KMMapperReducer.KReducer.class);
			else
				// Run a mapper only, to get the cluster result
				job.setNumReduceTasks(0);
		} else if (OCL.equals(args[IIMPL])) {
			job.setMapperClass(KMMapperReducerCL.KMMapper.class);
			if (args[IOUTPUT] != OUTPUT)
				job.setReducerClass(KMMapperReducerCL.KMReducer.class);
			else
				// Run a mapper only, to get the cluster result
				job.setNumReduceTasks(0);
		} else
			return FAILURE;

		job.setMapOutputKeyClass(PointWritable.class);
		job.setMapOutputValueClass(PointWritable.class);

		job.setOutputKeyClass(PointWritable.class);
		job.setOutputValueClass(PointWritable.class);

		job.setInputFormatClass(PointInputFormat.class);
		job.setOutputFormatClass(PointOutputFormat.class);

		PointInputFormat.setInputPaths(job, new Path(args[IINPUT]));
		PointOutputFormat.setOutputPath(job, new Path(args[IOUTPUT]));

		DistributedCache.addCacheFile(new URI(args[ICENTROIDS]),
				job.getConfiguration());

		int stat = job.waitForCompletion(true) ? SUCCESS : FAILURE;
		return stat;
	}

}
