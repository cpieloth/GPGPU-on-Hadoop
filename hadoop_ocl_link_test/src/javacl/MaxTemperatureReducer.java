package javacl;

import java.io.IOException;
import java.util.Iterator;

import lightLogger.Logger;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import com.nativelibs4java.opencl.CLDevice;

public class MaxTemperatureReducer extends
		Reducer<Text, IntWritable, Text, IntWritable> {

	private static final int MAX_VALUES = 65536; // max memory of opencl device,
											// MAX_VALUE % 64 = 0
	private int buffer[];
	
	private maxValueJavaCL.MaxValueAbstract maxVal;

	@Override
	protected void setup(MaxTemperatureReducer.Context context)
			throws IOException, InterruptedException {
		this.maxVal = new maxValueJavaCL.MaxValueSimple();
		this.maxVal.initialize(CLDevice.Type.GPU);
		
		this.buffer = new int[MAX_VALUES];
	}

	@Override
	public void reduce(Text key, Iterable<IntWritable> values, Context context)
			throws IOException, InterruptedException {
		buffer[0] = Integer.MIN_VALUE; // temporary max temp
		int i = 1;

		for (Iterator<IntWritable> it = values.iterator(); it.hasNext();) {
			// fill array to copy on opencl device, mind max memory of device!
			if (i < MAX_VALUES) {
				buffer[i++] = it.next().get();
			}
			// if max values or no more values to add, start opencl kernel
			if (i >= MAX_VALUES || !it.hasNext()) {
				buffer[0] = this.maxVal.maxValue(buffer);
				i = 1;
				Logger.logDebug(this.getClass(), "max: " + buffer[0] + " - key: " + key);
			}
		}

		context.write(key, new IntWritable(buffer[0]));
	}

}
