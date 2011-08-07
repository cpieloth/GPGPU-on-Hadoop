package hadoop;

import java.net.URI;

import lightLogger.Level;
import lightLogger.Logger;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FsUrlStreamHandlerFactory;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import stopwatch.StopWatch;

public class KMeansHadoop extends Configured implements Tool {

	private static final Class<KMeansHadoop> CLAZZ = KMeansHadoop.class;

	public static final Level TIME_LEVEL = new Level(128, "TIME");

	public enum Argument {
		JOBNAME("jobname", 0), INPUT("input", 1), CENTROIDS("centroids", 2), OUTPUT(
				"output", 3), TYPE(Argument.CPU + "|" + Argument.OCL, 4), ITERATIONS(
				"iterations", 5);

		public final String name;
		public final int index;

		private Argument(String name, int index) {
			this.name = name;
			this.index = index;
		}

		public static final String CPU = "cpu";
		public static final String OCL = "ocl";

	}

	public enum Timer {
		MAPPHASE("mapPhaseTime=", StopWatch.SUFFIX), MAPMETHOD(
				"mapMethodTime=", StopWatch.SUFFIX), REDUCEPHASE(
				"reducePhaseTime=", StopWatch.SUFFIX), REDUCEMETHOD(
				"reduceMethodTime=", StopWatch.SUFFIX);

		public final String prefix;
		public final String suffix;

		Timer(String prefix, String suffix) {
			this.prefix = prefix;
			this.suffix = suffix;
		}
	}

	public static final int SUCCESS = 0;
	public static final int FAILURE = 1;

	public static final String CHARSET = "UTF-8";

	public static String OUTPUT;

	public static void main(String[] args) throws Exception {
		GenericOptionsParser gop = new GenericOptionsParser(args);
		String[] rArgs = gop.getRemainingArgs();
		if (rArgs.length < 6) {
			StringBuilder sb = new StringBuilder();
			sb.append("Arguments:");
			for (Argument arg : Argument.values())
				sb.append(" <" + arg.name + ">");
			System.out.println(sb.toString());
			System.exit(FAILURE);
		}

		int res;
		OUTPUT = rArgs[Argument.OUTPUT.index];
		final int iterations = Integer
				.parseInt(rArgs[Argument.ITERATIONS.index]);
		String centroids = rArgs[Argument.CENTROIDS.index];

		// load HDFS handler, only once!
		try {
			FsUrlStreamHandlerFactory factory = new org.apache.hadoop.fs.FsUrlStreamHandlerFactory();
			java.net.URL.setURLStreamHandlerFactory(factory);
		} catch (Exception e) {
			Logger.logWarn(
					CLAZZ,
					"Could not set org.apache.hadoop.fs.FsUrlStreamHandlerFactory. May be it has been set before.");
		}

		StopWatch sw = new StopWatch("totalTime=", ";");
		sw.start();

		int i = 0;
		do {
			rArgs[Argument.OUTPUT.index] = centroids + "-" + (i + 1);
			res = ToolRunner.run(gop.getConfiguration(), new KMeansHadoop(),
					rArgs);
			rArgs[Argument.CENTROIDS.index] = centroids + "-" + (i + 1);
			i++;
		} while (i < iterations && res == SUCCESS);

		if (res != SUCCESS) {
			Logger.logError(CLAZZ, "Error during job execution!");
			System.exit(FAILURE);
		}

		// collect clusters in a final map
		rArgs[Argument.OUTPUT.index] = OUTPUT;
		res = ToolRunner.run(gop.getConfiguration(), new KMeansHadoop(), rArgs);

		sw.stop();
		Logger.log(TIME_LEVEL, CLAZZ, sw.getTimeString());

		if (res != SUCCESS)
			Logger.logError(CLAZZ, "Error during job execution!");

		System.exit(res);
	}

	@Override
	public int run(String[] args) throws Exception {
		Job job = new Job(this.getConf());

		job.setJobName(args[Argument.JOBNAME.index]);

		job.setJarByClass(KMeansHadoop.class);

		if (Argument.CPU.equals(args[Argument.TYPE.index])) {
			job.setMapperClass(KMMapperReducer.KMMapper.class);
			if (args[Argument.OUTPUT.index] != OUTPUT)
				job.setReducerClass(KMMapperReducer.KMReducer.class);
			else
				// Run a mapper only, to get the cluster result
				job.setNumReduceTasks(0);
		} else if (Argument.OCL.equals(args[Argument.TYPE.index])) {
			job.setMapperClass(KMMapperReducerCL.KMMapper.class);
			if (args[Argument.OUTPUT.index] != OUTPUT)
				job.setReducerClass(KMMapperReducerCL.KMReducer.class);
			else
				// Run a mapper only, to get the cluster result
				job.setNumReduceTasks(0);
		} else {
			Logger.logError(CLAZZ, "Unknown type!");
			return FAILURE;
		}

		job.setMapOutputKeyClass(PointWritable.class);
		job.setMapOutputValueClass(PointWritable.class);

		job.setOutputKeyClass(PointWritable.class);
		job.setOutputValueClass(PointWritable.class);

		job.setInputFormatClass(PointInputFormat.class);
		job.setOutputFormatClass(PointOutputFormat.class);

		PointInputFormat.setInputPaths(job,
				new Path(args[Argument.INPUT.index]));
		PointOutputFormat.setOutputPath(job, new Path(
				args[Argument.OUTPUT.index]));

		DistributedCache.addCacheFile(new URI(args[Argument.CENTROIDS.index]),
				job.getConfiguration());

		int stat = job.waitForCompletion(true) ? SUCCESS : FAILURE;
		return stat;
	}

}
