package javacl;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class MaxTemperatureReducer extends
		Reducer<Text, IntWritable, Text, IntWritable> {

	private final int MAX_VALUES = 65535;	// max memory of opencl device
	
	public void reduce(Text key, Iterable<IntWritable> values, Context context)
			throws IOException, InterruptedException {
		int[] tmpValues = new int[MAX_VALUES];
		tmpValues[0] = Integer.MIN_VALUE;	// temporary max temp
		int i = 1;
		
		for(Iterator<IntWritable> it = values.iterator(); it.hasNext();) {
			// fill array to copy on opencl device, mind max memory of device!
			if(i < MAX_VALUES)
				tmpValues[i++] = it.next().get();
			// if max values or no more values to add, start opencl kernel
			if(i >= MAX_VALUES || !it.hasNext()) {
				tmpValues[0] = getMaxTempOCL(tmpValues, i);
				i = 1;
			}
		}

		context.write(key, new IntWritable(tmpValues[0]));
	}

	private int getMaxTempOCL(int[] tmpValues, int i) {
		// TODO Auto-generated method stub
		return 0;
	}
}
