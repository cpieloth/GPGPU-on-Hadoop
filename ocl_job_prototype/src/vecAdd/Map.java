package vecAdd;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import lightLogger.Logger;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class Map extends
		Mapper<LongWritable, Text, IntWritable, IntArrayWritable> {

	private final Class<?> CLAZZ = Map.class;

	private IntArrayWritable vector;
	private IntWritable hash;

	@Override
	public void map(LongWritable key, Text value, Map.Context context)
			throws IOException, InterruptedException {
		Logger.logTrace(CLAZZ, "map called");

		String line = value.toString();
		hash = new IntWritable(line.hashCode());
		Logger.logDebug(CLAZZ, "hash: " + hash.toString());
		Logger.logDebug(CLAZZ, "key: " + key.toString());

		StringTokenizer tokenizer = new StringTokenizer(line);

		// Temp data
		List<IntWritable> list = new LinkedList<IntWritable>();
		String tmp;

		// Vector A
		while (tokenizer.hasMoreTokens()) {
			tmp = tokenizer.nextToken();
			if (";".equals(tmp))
				break;
			else {
				list.add(new IntWritable(Integer.parseInt(tmp)));
			}
		}
		
		vector = new IntArrayWritable(list);
		context.write(hash, vector);
		Logger.logDebug(CLAZZ, "IntArrayWritable vec a: " + vector);

		// Clear
		list.clear();

		// Vector B
		while (tokenizer.hasMoreTokens()) {
			tmp = tokenizer.nextToken();
			list.add(new IntWritable(Integer.parseInt(tmp)));
		}

		vector = new IntArrayWritable(list);
		context.write(hash, vector);
		Logger.logDebug(CLAZZ, "IntArrayWritable vec b: "
				+ vector);

		Logger.logTrace(CLAZZ, "map end");
	}
}