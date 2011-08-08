package hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lightLogger.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.util.LineReader;

/**
 * Same as NLineInputFormat with NullWritable as key and FloatIntervalWritable
 * as value.
 * 
 * @author christof
 * 
 */
public class FloatIntervalInputFormat extends
		FileInputFormat<NullWritable, FloatIntervalWritable> {

	private static final Pattern pattern = Pattern
			.compile("\\[(\\d+.\\d+),(\\d+.\\d+)\\]\\s+(\\d+)");

	/**
	 * Defines the lines per mapper/split.
	 */
	public static int LINES_PER_SPLIT = 4;

	/**
	 * Creates splits with LINES_PER_SPLIT lines (intervals). One small file
	 * shall be distributed to many mappers.
	 */
	@Override
	public List<InputSplit> getSplits(JobContext context) throws IOException {
		// TODO splits will be compute on nodes which hold the blocks?
		List<InputSplit> splits = new ArrayList<InputSplit>();

		for (FileStatus status : listStatus(context)) {
			Path path = status.getPath();
			if (status.isDir()) {
				throw new IOException("Not a file: " + path);
			}
			FileSystem fs = path.getFileSystem(context.getConfiguration());
			LineReader lr = null;
			try {
				lr = new LineReader(fs.open(path), context.getConfiguration());
				Text line = new Text();
				long numLines = 0;
				long begin = 0;
				long length = 0;
				int num = -1;
				while ((num = lr.readLine(line)) > 0) {
					numLines++;
					length += num;
					if (numLines == LINES_PER_SPLIT) {
						splits.add(new FileSplit(path, begin, length,
								new String[] {}));
						begin += length;
						length = 0;
						numLines = 0;
					}
				}
				if (numLines != 0) {
					splits.add(new FileSplit(path, begin, length,
							new String[] {}));
				}

			} finally {
				if (lr != null) {
					lr.close();
				}
			}
		}

		Logger.logDebug(FloatIntervalInputFormat.class, "Total # of splits: "
				+ splits.size());
		return splits;
	}

	@Override
	public RecordReader<NullWritable, FloatIntervalWritable> createRecordReader(
			InputSplit split, TaskAttemptContext context) throws IOException,
			InterruptedException {
		return new IntervalRecordReader();
	}

	public static FloatIntervalWritable createFloatIntervalWritable(String line) {
		Matcher matcher = pattern.matcher(line);
		FloatIntervalWritable interval = new FloatIntervalWritable();
		if (matcher.matches()) {
			interval.setBegin(Float.valueOf(matcher.group(1)));
			interval.setEnd(Float.valueOf(matcher.group(2)));
			interval.setResolution(Integer.valueOf(matcher.group(3)));
			return interval;

		} else {
			Logger.logError(FloatIntervalInputFormat.class,
					"Could not create interval from line: " + line);
			return interval;
		}
	}

	protected static class IntervalRecordReader extends
			RecordReader<NullWritable, FloatIntervalWritable> {

		private CompressionCodecFactory compressionCodecs = null;
		private long start;
		private long pos;
		private long end;
		private LineReader in;
		private int maxLineLength;
		private NullWritable key = NullWritable.get();
		private FloatIntervalWritable value = null;
		private Text line;

		@Override
		public void initialize(InputSplit genericSplit,
				TaskAttemptContext context) throws IOException,
				InterruptedException {
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

		@Override
		public boolean nextKeyValue() throws IOException, InterruptedException {
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
		public NullWritable getCurrentKey() throws IOException,
				InterruptedException {
			return key;
		}

		@Override
		public FloatIntervalWritable getCurrentValue() throws IOException,
				InterruptedException {
			this.value = FloatIntervalInputFormat
					.createFloatIntervalWritable(this.line.toString());
			return this.value;
		}

		@Override
		public float getProgress() throws IOException, InterruptedException {
			if (start == end) {
				return 0.0f;
			} else {
				return Math.min(1.0f, (pos - start) / (float) (end - start));
			}
		}

		@Override
		public void close() throws IOException {
			if (in != null) {
				in.close();
			}
		}

	}

}
