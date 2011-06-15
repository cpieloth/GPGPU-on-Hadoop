package vecAdd;

import java.io.IOException;
import java.util.Iterator;

import lightLogger.Logger;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Reducer;

import util.Convert;

public class ReduceCPU extends
		Reducer<IntWritable, IntArrayWritable, IntWritable, IntArrayWritable> {

	public static final boolean EXIT_FAILURE = false; // 1;
	public static final boolean EXIT_SUCCESS = true;// 0;

	private final Class<?> CLAZZ = ReduceCPU.class;

	private IntArrayWritable vector;

	@Override
	public void reduce(IntWritable key, Iterable<IntArrayWritable> values,
			Context context) throws IOException, InterruptedException {
		// TODO Input arrays are not in output order from Map
		Logger.logTrace(CLAZZ, "reduce called");
		Logger.logDebug(CLAZZ, "Hash: " + key.toString());

		Iterator<IntArrayWritable> it = values.iterator();
		int[] a, b, c;

		if (it.hasNext()) {
			a = it.next().toIntArray();
			Logger.logDebug(CLAZZ, "int[] a: " + Convert.toString(a));
		} else {
			Logger.logError(CLAZZ, "No vector A!");
			return;
		}
		if (it.hasNext()) {
			b = it.next().toIntArray();
			Logger.logDebug(CLAZZ, "int[] b: " + Convert.toString(b));
		} else {
			Logger.logError(CLAZZ, "No vector B!");
			return;
		}
		if (it.hasNext())
			Logger.logWarn(CLAZZ, "More vectors than A and B available!");

		c = new int[a.length];

		if (ReduceCPU.EXIT_FAILURE == this.addVec(c, a, b)) {
			Logger.logError(CLAZZ, "Vector.addVec");
			return;
		}
		Logger.logDebug(CLAZZ, "int[] c: " + Convert.toString(c));

		vector = new IntArrayWritable(c);
		context.write(key, vector);
		Logger.logDebug(CLAZZ, "vector: " + vector);
		Logger.logTrace(CLAZZ, "reduce end");
	}

	private boolean addVec(int[] c, int[] a, int[] b) {
		for (int i = 0; i < c.length; ++i)
			c[i] = a[i] + b[i];
		return ReduceCPU.EXIT_SUCCESS;
	}
}