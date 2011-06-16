package vecAdd;

import java.io.IOException;
import java.util.StringTokenizer;

import lightLogger.Level;
import lightLogger.Logger;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class MapCPU extends
		Mapper<LongWritable, Text, IntWritable, IntArrayWritable> {

	private final Class<?> CLAZZ = MapCPU.class;

	public static final boolean EXIT_FAILURE = false; // 1;
	public static final boolean EXIT_SUCCESS = true;// 0;

	private IntArrayWritable result;
	private int[] a, b, c;
	private IntWritable hash;

	@Override
	public void map(LongWritable key, Text value, MapCPU.Context context)
			throws IOException, InterruptedException {
		Logger.logTrace(CLAZZ, "map called");

		String line = value.toString();
		hash = new IntWritable(line.hashCode());
		Logger.logDebug(CLAZZ, "hash: " + hash.toString());
		Logger.logDebug(CLAZZ, "key: " + key.toString());

		// Temp data
		StringTokenizer tokenizer = new StringTokenizer(line);
		int size = tokenizer.countTokens();
		size = (size - 1) / 2;

		a = new int[size];
		b = new int[size];
		c = new int[size];

		VecAdd.readVector(tokenizer, a, b);

		Logger.logInfo(CLAZZ, "Start calculating");
		if (MapCPU.EXIT_FAILURE == this.addVec(c, a, b)) {
			Logger.logError(CLAZZ, "Vector.addVec");
			return;
		}
		Logger.logInfo(CLAZZ, "Calculating finished");

		result = new IntArrayWritable(c);
		if ((Logger.getLogMask() & Level.DEFAULT.DEBUG.getLevel().getValue()) == Level.DEFAULT.DEBUG
				.getLevel().getValue())
			Logger.logDebug(CLAZZ, "vector c: " + result.toString());

		context.write(hash, result);
		Logger.logTrace(CLAZZ, "map end");
	}

	private boolean addVec(int[] c, int[] a, int[] b) {
		for (int i = 0; i < c.length; ++i)
			c[i] = a[i] + b[i];
		return MapCPU.EXIT_SUCCESS;
	}

}