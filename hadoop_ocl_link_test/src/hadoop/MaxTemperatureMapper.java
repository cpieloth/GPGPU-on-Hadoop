package hadoop;

import gsod.DataSet;

import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class MaxTemperatureMapper extends
		Mapper<LongWritable, Text, Text, IntWritable> {

	@Override
	public void map(LongWritable key, Text value,
			MaxTemperatureMapper.Context context) throws IOException,
			InterruptedException {
		String line = value.toString();
		String year = DataSet.getYear(line);
		int airTemperature = DataSet.getMax(line);

		// String quality = line.substring(92, 93);

		if (airTemperature != DataSet.MISSING /* && quality.matches("[01459]") */) {
			context.write(new Text(year), new IntWritable(airTemperature));
		}

	}

}
