package hadoop;

import integration.FloatPowerFunction;
import integration.IMathFunction;
import integration.INumeriacalIntegration;
import integration.TrapeziumIntegrationCL;

import java.io.IOException;

import lightLogger.Logger;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class NumericalIntegrationCL extends Configured implements Tool {

	public static final int SUCCESS = 0;
	public static final int FAILURE = 1;

	public static final int INAME = 0;
	public static final int IINPUT = 1;
	public static final int IOUTPUT = 2;

	public static void main(String[] args) throws Exception {
		GenericOptionsParser gop = new GenericOptionsParser(args);
		String[] rArgs = gop.getRemainingArgs();
		if (rArgs.length < 3) {
			System.out.println("Arguments: <Jobname> <Input> <Output>");
			System.exit(FAILURE);
		}

		int res = ToolRunner.run(gop.getConfiguration(),
				new NumericalIntegrationCL(), rArgs);
		
		System.exit(res);
	}

	@Override
	public int run(String[] args) throws Exception {
		Job job = new Job(this.getConf());

		job.setJobName(args[INAME]);

		job.setJarByClass(NumericalIntegrationCL.class);

		job.setMapperClass(NumericalIntegrationCL.NIMapper.class);
		job.setReducerClass(NumericalIntegrationCL.NIReducer.class);

		job.setMapOutputKeyClass(NullWritable.class);
		job.setMapOutputValueClass(FloatWritable.class);

		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(FloatWritable.class);

		job.setInputFormatClass(FloatIntervalInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);

		FloatIntervalInputFormat.setInputPaths(job, new Path(args[IINPUT]));
		FloatIntervalOutputFormat.setOutputPath(job, new Path(args[IOUTPUT]));

		long tStart = System.currentTimeMillis();
		int stat = job.waitForCompletion(true) ? SUCCESS : FAILURE;
		Logger.logInfo(NumericalIntegrationCL.class, "Time (ms): " + (System.currentTimeMillis() - tStart));
		
		return stat;
	}

	public static class NIMapper
			extends
			Mapper<NullWritable, FloatIntervalWritable, NullWritable, FloatWritable> {

		private static final INumeriacalIntegration<Float> integration = new TrapeziumIntegrationCL();
		private static final IMathFunction<Float> function = new FloatPowerFunction(3f);
		
		@Override
		protected void map(NullWritable key, FloatIntervalWritable value,
				NIMapper.Context context) throws IOException,
				InterruptedException {
			Logger.logDebug(NIMapper.class, value.toString());
			
			integration.setFunction(function);
			Float result = integration.getIntegral(value);
			context.write(key, new FloatWritable(result));
		}

	}

	public static class NIReducer
			extends
			Reducer<NullWritable, FloatWritable, NullWritable, FloatWritable> {

		@Override
		protected void reduce(NullWritable key,
				Iterable<FloatWritable> values,
				NIReducer.Context context) throws IOException,
				InterruptedException {
			float result = 0;
			for (FloatWritable value : values) {
				Logger.logDebug(NIMapper.class, value.toString());
				result += value.get();
			}
			context.write(key, new FloatWritable(result));
		}

	}
}
