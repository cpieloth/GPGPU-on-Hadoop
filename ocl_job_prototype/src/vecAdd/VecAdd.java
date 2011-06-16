package vecAdd;

import java.util.StringTokenizer;

import lightLogger.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class VecAdd {

	private static final Class<VecAdd> CLAZZ = VecAdd.class;

	public static void main(String[] args) throws Exception {
		Logger.logTrace(CLAZZ, "main start");
		
		if(args.length < 3) {
			Logger.logError(CLAZZ, "Arguments: <JobName> <InputPaths> <OutputPath> <cpu | ocl>");
			return;
		}
			
		Configuration conf = new Configuration(true);
		conf.set("mapred.job.tracker", "local"); // use localjobrunner
		conf.setInt("mapred.reduce.tasks", 0); // no reduce phase is needed

		Job job = new Job(conf, args[0]);
		job.setJobName(args[0]);

		job.setJarByClass(CLAZZ);
		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(IntArrayWritable.class);
		
		if("ocl".equals(args[3])) {
			job.setMapperClass(MapOCL.class);
			Logger.logInfo(CLAZZ, "Using OpenCL");
		}
		else if("cpu".equals(args[3])) {
			job.setMapperClass(MapCPU.class);
			Logger.logInfo(CLAZZ, "Using CPU");
		}
		else {
			job.setMapperClass(MapCPU.class);
			Logger.logInfo(CLAZZ, "Using default (CPU)");
		}
		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);

		FileInputFormat.setInputPaths(job, new Path(args[1]));
		FileOutputFormat.setOutputPath(job, new Path(args[2]));

		int stat = job.waitForCompletion(true) ? 0 : 1;
		Logger.logTrace(CLAZZ, "main end");
		System.exit(stat);
	}
	
	public static void readVector(StringTokenizer tokenizer, int[] a, int[] b) {
		// Vector A
		String tmp;
		int i = 0;
		
		while (tokenizer.hasMoreTokens()) {
			tmp = tokenizer.nextToken();
			if (";".equals(tmp))
				break;
			else {
				a[i++] = Integer.parseInt(tmp);
			}
		}
		Logger.logInfo(CLAZZ, "Reading vector a finished");

		// Vector B
		i = 0;
		while (tokenizer.hasMoreTokens()) {
			tmp = tokenizer.nextToken();
			b[i++] = Integer.parseInt(tmp);
		}
		Logger.logInfo(CLAZZ, "Reading vector b finished");
	}

}