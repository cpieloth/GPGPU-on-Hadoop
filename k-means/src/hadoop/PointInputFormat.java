package hadoop;

import java.io.IOException;

import lightLogger.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.util.LineReader;

/**
 * Reads and deserializes the output of PointOutputFormat.
 * 
 * @author christof
 * 
 */
public class PointInputFormat extends
		FileInputFormat<NullWritable, PointWritable> {

	@Override
	public RecordReader<NullWritable, PointWritable> createRecordReader(
			InputSplit arg0, TaskAttemptContext arg1) throws IOException,
			InterruptedException {
		return new PointRecordReader();
	}
	
	public static PointWritable createPointWritable(String line) {
		String[] splits = line.split(";");
		PointWritable value = new PointWritable(splits.length);
		for (int d = 0; d < splits.length; d++)
			value.set(d, Float.valueOf(splits[d]).floatValue());
		return value;
	}

	protected static class PointRecordReader extends
			RecordReader<NullWritable, PointWritable> {

		private CompressionCodecFactory compressionCodecs = null;
		private long start;
		private long pos;
		private long end;
		private LineReader in;
		private int maxLineLength;
		private NullWritable key = NullWritable.get();
		private PointWritable value = null;
		private Text line;

		public void initialize(InputSplit genericSplit,
				TaskAttemptContext context) throws IOException {
			FileSplit split = (FileSplit) genericSplit;
			Configuration job = context.getConfiguration();
			this.maxLineLength = job.getInt(
					"mapred.linerecordreader.maxlength", Integer.MAX_VALUE);
			start = split.getStart();
			end = start + split.getLength();
			final Path file = split.getPath();
			compressionCodecs = new CompressionCodecFactory(job);
			final CompressionCodec codec = compressionCodecs.getCodec(file);

			// open the file and seek to the start of the split
			FileSystem fs = file.getFileSystem(job);
			FSDataInputStream fileIn = fs.open(split.getPath());
			boolean skipFirstLine = false;
			if (codec != null) {
				in = new LineReader(codec.createInputStream(fileIn), job);
				end = Long.MAX_VALUE;
			} else {
				if (start != 0) {
					skipFirstLine = true;
					--start;
					fileIn.seek(start);
				}
				in = new LineReader(fileIn, job);
			}
			if (skipFirstLine) { // skip first line and re-establish "start".
				start += in.readLine(new Text(), 0,
						(int) Math.min((long) Integer.MAX_VALUE, end - start));
			}
			this.pos = start;
		}

		public boolean nextKeyValue() throws IOException {
			if (key == null) {
				key = NullWritable.get();
			}
			if (line == null) {
				line = new Text();
			}
			int newSize = 0;
			while (pos < end) {
				newSize = in.readLine(line, maxLineLength, Math.max(
						(int) Math.min(Integer.MAX_VALUE, end - pos),
						maxLineLength));
				if (newSize == 0) {
					break;
				}
				pos += newSize;
				if (newSize < maxLineLength) {
					break;
				}

				// line too long. try again
				Logger.logInfo(this.getClass(), "Skipped line of size "
						+ newSize + " at pos " + (pos - newSize));
			}
			if (newSize == 0) {
				key = null;
				value = null;
				return false;
			} else {
				return true;
			}
		}

		@Override
		public NullWritable getCurrentKey() {
			return key;
		}

		@Override
		public PointWritable getCurrentValue() {
			this.value = PointInputFormat.createPointWritable(this.line.toString());
			return this.value;
		}

		/**
		 * Get the progress within the split
		 */
		public float getProgress() {
			if (start == end) {
				return 0.0f;
			} else {
				return Math.min(1.0f, (pos - start) / (float) (end - start));
			}
		}

		public synchronized void close() throws IOException {
			if (in != null) {
				in.close();
			}
		}
	}

}
