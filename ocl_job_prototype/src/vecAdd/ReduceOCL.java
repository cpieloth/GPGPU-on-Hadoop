package vecAdd;

import java.io.IOException;
import java.util.Iterator;

import lightLogger.Logger;
import openCL.Vector;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Reducer;

import util.Convert;

import com.nativelibs4java.opencl.CLDevice;

public class ReduceOCL extends
		Reducer<IntWritable, IntArrayWritable, IntWritable, IntArrayWritable> {

	private final Class<?> CLAZZ = ReduceOCL.class;

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

		// do OpenCL
		if (Vector.EXIT_FAILURE == Vector.addVec(CLDevice.Type.CPU, c, a, b)) {
			Logger.logError(CLAZZ, "Vector.addVec");
			return;
		}
		Logger.logDebug(CLAZZ, "int[] c: " + Convert.toString(c));

		vector = new IntArrayWritable(c);
		context.write(key, vector);
		Logger.logDebug(CLAZZ, "vector: " + vector);
		Logger.logTrace(CLAZZ, "reduce end");
	}
}