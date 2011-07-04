package hadoop;

import gsod.DataSet;

import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class MaxTemperatureMapper extends
		Mapper<LongWritable, Text, Text, IntWritable> {

	private String line;
	private String year;
	private int airTemperature;
	
	@Override
	public void map(LongWritable key, Text value,
			MaxTemperatureMapper.Context context) throws IOException,
			InterruptedException {
		line = value.toString();

		if (line.startsWith("STN---"))
			return;

		year = DataSet.getYear(line);
		airTemperature = DataSet.getMax(line);

		if (airTemperature != DataSet.MISSING) {
			context.write(new Text(year), new IntWritable(airTemperature));
		}
	}

}
