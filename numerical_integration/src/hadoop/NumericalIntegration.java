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

public class NumericalIntegration extends Configured implements Tool {

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
	public static final int IIMPL = 3;

	public static final String OCL = "ocl";
	public static final String CPU = "cpu";

	public static void main(String[] args) throws Exception {
		GenericOptionsParser gop = new GenericOptionsParser(args);
		String[] rArgs = gop.getRemainingArgs();
		if (rArgs.length < 4) {
			System.out
					.println("Arguments: <Jobname> <Input> <Output> <ocl | cpu>");
			System.exit(FAILURE);
		}

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

		job.setJobName(args[INAME]);

		job.setJarByClass(NumericalIntegration.class);

		if (CPU.equals(args[IIMPL])) {
			job.setMapperClass(NIMapperReducer.NIMapper.class);
			job.setReducerClass(NIMapperReducer.NIReducer.class);
		} else if (OCL.equals(args[IIMPL])) {
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

		FloatIntervalInputFormat.setInputPaths(job, new Path(args[IINPUT]));
		FloatIntervalOutputFormat.setOutputPath(job, new Path(args[IOUTPUT]));

		int stat = job.waitForCompletion(true) ? SUCCESS : FAILURE;

		return stat;
	}

}
