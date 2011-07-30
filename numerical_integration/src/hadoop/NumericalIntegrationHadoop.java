package hadoop;

import java.io.IOException;

import lightLogger.Logger;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class NumericalIntegrationHadoop extends Configured implements Tool {

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
				new NumericalIntegrationHadoop(), rArgs);
		System.exit(res);
	}

	@Override
	public int run(String[] args) throws Exception {
		Job job = new Job(this.getConf());

		job.setJobName(args[INAME]);

		job.setJarByClass(NumericalIntegrationHadoop.class);

		job.setMapperClass(NumericalIntegrationHadoop.NIMapper.class);
		job.setReducerClass(NumericalIntegrationHadoop.NIReducer.class);

		job.setMapOutputKeyClass(NullWritable.class);
		job.setMapOutputValueClass(FloatIntervalWritable.class);

		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(FloatIntervalWritable.class);

		job.setInputFormatClass(FloatIntervalInputFormat.class);
		job.setOutputFormatClass(FloatIntervalOutputFormat.class);

		FloatIntervalInputFormat.setInputPaths(job, new Path(args[IINPUT]));
		FloatIntervalOutputFormat.setOutputPath(job, new Path(args[IOUTPUT]));

		int stat = job.waitForCompletion(true) ? SUCCESS : FAILURE;
		return stat;
	}

	public static class NIMapper
			extends
			Mapper<NullWritable, FloatIntervalWritable, NullWritable, FloatIntervalWritable> {

		@Override
		protected void map(NullWritable key, FloatIntervalWritable value,
				NIMapper.Context context) throws IOException,
				InterruptedException {
			Logger.logDebug(NIMapper.class, value.toString());
			context.write(key, value);
		}

	}

	public static class NIReducer
			extends
			Reducer<NullWritable, FloatIntervalWritable, NullWritable, FloatIntervalWritable> {

		@Override
		protected void reduce(NullWritable key,
				Iterable<FloatIntervalWritable> values,
				NIReducer.Context context) throws IOException,
				InterruptedException {
			for (FloatIntervalWritable value : values) {
				Logger.logDebug(NIMapper.class, value.toString());
				context.write(key, value);
			}
		}

	}
}
