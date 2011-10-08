package hadoop;

import lightLogger.Level;
import lightLogger.Logger;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import stopwatch.StopWatch;
import utils.MathFunctions;

public class NumericalIntegrationNamed extends Configured implements Tool {

	public static final Level TIME_LEVEL = new Level(128, "TIME");

	public enum Argument {
		JOBNAME("jobname", 0), INPUT("input", 1), OUTPUT("output", 2), FUNCTION(
				MathFunctions.getAvailableIdentifer('|'), 3), EXPONENT(
				"exponent", 4), RESOLUTION("resolution", 5), TYPE(Argument.CPU
				+ "|" + Argument.OCL, 6);

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

	public static void main(String[] args) throws Exception {
		GenericOptionsParser gop = new GenericOptionsParser(args);
		String[] rArgs = gop.getRemainingArgs();
		if (rArgs.length < 7) {
			StringBuilder sb = new StringBuilder();
			sb.append("Arguments:");
			for (Argument arg : Argument.values())
				sb.append(" <" + arg.name + ">");
			System.out.println(sb.toString());
			System.exit(FAILURE);
		}

		if (!Argument.CPU.equals(args[Argument.TYPE.index])
				&& !Argument.OCL.equals(args[Argument.TYPE.index])) {
			Logger.logError(NumericalIntegrationNamed.class, "Unknown type!");
			System.exit(FAILURE);
		}
		if (!MathFunctions.isFunctionAvailable(args[Argument.FUNCTION.index])) {
			Logger.logError(NumericalIntegrationNamed.class,
					"Unknown function!");
			System.exit(FAILURE);
		}

		// final int intervals =
		// Integer.parseInt(rArgs[Argument.INTERVALS.index]);
		// FloatIntervalInputFormat.LINES_PER_SPLIT = intervals;

		StopWatch sw = new StopWatch("totalTime=", ";");
		sw.start();

		int res = ToolRunner.run(gop.getConfiguration(),
				new NumericalIntegrationNamed(), rArgs);

		sw.stop();
		Logger.log(TIME_LEVEL, NumericalIntegrationNamed.class,
				sw.getTimeString());

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

		job.setJarByClass(NumericalIntegrationNamed.class);

		Logger.logDebug(getClass(), "NumReduceTask: " + job.getNumReduceTasks());

		if (Argument.CPU.equals(args[Argument.TYPE.index])) {
			job.setMapperClass(NIMapperReducerNamed.NIMapper.class);
			job.setReducerClass(NIMapperReducerNamed.NIReducer.class);
		} else if (Argument.OCL.equals(args[Argument.TYPE.index])) {
			job.setMapperClass(NIMapperReducerCLNamed.NIMapper.class);
			job.setReducerClass(NIMapperReducerCLNamed.NIReducer.class);
		} else
			return FAILURE;

		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(FloatIntervalWritable.class);

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(FloatWritable.class);

		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);

		FloatIntervalInputFormat.setInputPaths(job, new Path(
				args[Argument.INPUT.index]));
		FloatIntervalOutputFormat.setOutputPath(job, new Path(
				args[Argument.OUTPUT.index]));

		int stat = job.waitForCompletion(true) ? SUCCESS : FAILURE;

		return stat;
	}

}
