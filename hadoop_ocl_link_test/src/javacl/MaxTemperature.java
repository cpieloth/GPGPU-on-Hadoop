package javacl;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class MaxTemperature {

	private static final Class<MaxTemperature> CLAZZ = MaxTemperature.class;

	public static void main(String[] args) throws Exception {
		if (args.length < 3) {
			System.out
					.println("Arguments: <JobName> <InputPaths> <OutputPath>");
			return;
		}

		Configuration conf = new Configuration(true);
		conf.set("mapred.job.tracker", "local"); // use localjobrunner

		Job job = new Job(conf, args[0]);
		job.setJobName(args[0]);

		job.setJarByClass(CLAZZ);

		job.setMapperClass(hadoop.MaxTemperatureMapper.class);
		job.setReducerClass(MaxTemperatureReducer.class);

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);

		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);

		FileInputFormat.setInputPaths(job, new Path(args[1]));
		FileOutputFormat.setOutputPath(job, new Path(args[2] + "_" + args[0]));

		int stat = job.waitForCompletion(true) ? 0 : 1;
		System.exit(stat);
	}
}
