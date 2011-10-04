package hadoop;

import lightLogger.Level;
import lightLogger.Logger;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import stopwatch.StopWatch;
import utils.MathFunctions;

public class NumericalIntegration extends Configured implements Tool {

	public static final Level TIME_LEVEL = new Level(128, "TIME");

	public enum Argument {
		JOBNAME("jobname", 0), INPUT("input", 1), OUTPUT("output", 2), FUNCTION(
				Argument.POLYNOM + "|" + Argument.POWER + "|" + Argument.XSINX,
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

		public static final String POLYNOM = "poly";
		public static final String POWER = "pow";
		public static final String XSINX = "xsinx";
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
			Logger.logError(NumericalIntegration.class, "Unknown type!");
			System.exit(FAILURE);
		}
		if (!MathFunctions.isFunctionAvailable(args[Argument.FUNCTION.index])) {
			Logger.logError(NumericalIntegration.class, "Unknown function!");
			System.exit(FAILURE);
		}

		final int intervals = Integer.parseInt(rArgs[Argument.INTERVALS.index]);
		FloatIntervalInputFormat.LINES_PER_SPLIT = intervals;

		StopWatch sw = new StopWatch("totalTime=", ";");
		sw.start();

		int res = ToolRunner.run(gop.getConfiguration(),
				new NumericalIntegration(), rArgs);

		sw.stop();
		Logger.log(TIME_LEVEL, NumericalIntegration.class, sw.getTimeString());

		System.exit(res);
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
		job.setOutputFormatClass(TextOutputFormat.class);

		FloatIntervalInputFormat.setInputPaths(job, new Path(
				args[Argument.INPUT.index]));
		FloatIntervalOutputFormat.setOutputPath(job, new Path(
				args[Argument.OUTPUT.index]));

		int stat = job.waitForCompletion(true) ? SUCCESS : FAILURE;

		return stat;
	}

}
