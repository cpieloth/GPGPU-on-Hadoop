package hadoop;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

// TODO
public class KMeansHadoop extends Configured implements Tool {

	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new Configuration(), new KMeansHadoop(), args);
		System.exit(res);
	}

	@Override
	public int run(String[] args) throws Exception {
		Job job = new Job(this.getConf());
		job.setJobName(args[0]);
		job.setJarByClass(KMeansHadoop.class);

		job.setMapperClass(KMeansHadoop.KMapper.class);
		job.setReducerClass(KMeansHadoop.KReducer.class);

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);

		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);

		FileInputFormat.setInputPaths(job, new Path(args[1]));
		FileOutputFormat.setOutputPath(job, new Path(args[2]));

		int stat = job.waitForCompletion(true) ? 0 : 1;
		return stat;
	}

	public static class KMapper extends
			Mapper<IntWritable, Text, Text, IntWritable> {

		@Override
		public void map(IntWritable key, Text value, KMapper.Context context)
				throws IOException, InterruptedException {

		}
	}

	public static class KReducer extends
			Reducer<Text, IntWritable, Text, IntWritable> {

		@Override
		public void reduce(Text key, Iterable<IntWritable> values,
				Context context) throws IOException, InterruptedException {
		}

	}

}
