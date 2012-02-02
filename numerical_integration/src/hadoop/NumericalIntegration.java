package hadoop;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lightLogger.Level;
import lightLogger.Logger;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import stopwatch.StopWatch;
import utils.MathFunctions;

public class NumericalIntegration extends Configured implements Tool {

	public static final Level TIME_LEVEL = new Level(128, "TIME");
	
	public static List<String> jobURLs = new LinkedList<String>();

	public enum Argument {
		JOBNAME("jobname", 0), INPUT("input", 1), OUTPUT("output", 2), FUNCTION(MathFunctions.getAvailableIdentifer('|'),
				3), EXPONENT("exponent", 4), RESOLUTION("resolution", 5), INTERVALS(
				"intervals", 6), TYPE(Argument.CPU + "|" + Argument.OCL, 7), ;

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

	private static final Class<?> CLAZZ = NumericalIntegration.class;

	public static void main(String[] args) throws Exception {
		GenericOptionsParser gop = new GenericOptionsParser(args);
		String[] rArgs = gop.getRemainingArgs();
		if (rArgs.length < 8) {
			StringBuilder sb = new StringBuilder();
			sb.append("Arguments:");
			for (Argument arg : Argument.values())
				sb.append(" <" + arg.name + ">");
			System.out.println(sb.toString());
			System.exit(FAILURE);
		}

		if (!Argument.CPU.equals(args[Argument.TYPE.index])
				&& !Argument.OCL.equals(args[Argument.TYPE.index])) {
			Logger.logError(CLAZZ, "Unknown type!");
			System.exit(FAILURE);
		}
		if (!MathFunctions.isFunctionAvailable(args[Argument.FUNCTION.index])) {
			Logger.logError(CLAZZ, "Unknown function!");
			System.exit(FAILURE);
		}
		
		StringBuilder argString = new StringBuilder();
		for (String arg : args) {
			argString.append(arg);
			argString.append(" ");
		}
		Logger.logInfo(CLAZZ, argString.toString());

		final int intervals = Integer.parseInt(rArgs[Argument.INTERVALS.index]);
		FloatIntervalInputFormat.LINES_PER_SPLIT = intervals;

		StopWatch sw = new StopWatch("totalTime=", ";");
		sw.start();

		int res = ToolRunner.run(gop.getConfiguration(),
				new NumericalIntegration(), rArgs);

		sw.stop();
		Logger.log(TIME_LEVEL, CLAZZ, sw.getTimeString());
		
		StringBuilder sb = new StringBuilder();
		sb.append("JobIDs: ");
		for (String url : jobURLs) {
			sb.append(getJobID(url));
			sb.append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		Logger.logInfo(CLAZZ, sb.toString());

		System.exit(res);
	}
	
	private static String getJobID(String url) {
		final Pattern p = Pattern.compile(".*jobid=job_(\\d+_\\d+)");
		Matcher m = p.matcher(url);
		if (m.matches() && m.groupCount() > 0)
			return m.group(1);
		else
			return null;
	}

	@Override
	public int run(String[] args) throws Exception {
		Job job = new Job(this.getConf());

		job.getConfiguration().setInt(Argument.RESOLUTION.name,
				Integer.parseInt(args[Argument.RESOLUTION.index]));
		job.getConfiguration().set(Argument.FUNCTION.name,
				args[Argument.FUNCTION.index]);
		job.getConfiguration().set(Argument.EXPONENT.name,
				args[Argument.EXPONENT.index]);

		job.setJobName(args[Argument.JOBNAME.index]);

		job.setJarByClass(NumericalIntegration.class);

		if (Argument.CPU.equals(args[Argument.TYPE.index])) {
			job.setMapperClass(NIMapperReducer.NIMapper.class);
			job.setReducerClass(NIMapperReducer.NIReducer.class);
		} else if (Argument.OCL.equals(args[Argument.TYPE.index])) {
			job.setMapperClass(NIMapperReducerCL.NIMapper.class);
			job.setReducerClass(NIMapperReducerCL.NIReducer.class);
		} else
			return FAILURE;

		job.setMapOutputKeyClass(NullWritable.class);
		job.setMapOutputValueClass(FloatWritable.class);

		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(FloatWritable.class);

		job.setInputFormatClass(FloatIntervalInputFormat.class);
		job.setOutputFormatClass(FloatIntervalOutputFormat.class);

		FloatIntervalInputFormat.setInputPaths(job, new Path(
				args[Argument.INPUT.index]));
		FloatIntervalOutputFormat.setOutputPath(job, new Path(
				args[Argument.OUTPUT.index]));

		int stat = job.waitForCompletion(true) ? SUCCESS : FAILURE;
		
		jobURLs.add(job.getTrackingURL());

		return stat;
	}

}
