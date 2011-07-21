package hadoop;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.ReflectionUtils;

import clustering.IPoint;

/**
 * Writes PointWritable in a human readable format to a file.
 * 
 * @author christof
 * 
 */
public class PointOutputFormat extends
		FileOutputFormat<PointWritable, PointWritable> {

	public static String createString(IPoint value) {
		StringBuilder sb = new StringBuilder();
		for (int d = 0; d < value.getDim(); d++)
			sb.append(value.get(d) + ";");
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	public RecordWriter<PointWritable, PointWritable> getRecordWriter(
			TaskAttemptContext job) throws IOException, InterruptedException {
		Configuration conf = job.getConfiguration();
	    boolean isCompressed = getCompressOutput(job);
	    String keyValueSeparator= conf.get("mapred.textoutputformat.separator",
	                                       "\t");
	    CompressionCodec codec = null;
	    String extension = "";
	    if (isCompressed) {
	      Class<? extends CompressionCodec> codecClass = 
	        getOutputCompressorClass(job, GzipCodec.class);
	      codec = (CompressionCodec) ReflectionUtils.newInstance(codecClass, conf);
	      extension = codec.getDefaultExtension();
	    }
	    Path file = getDefaultWorkFile(job, extension);
	    FileSystem fs = file.getFileSystem(conf);
	    if (!isCompressed) {
	      FSDataOutputStream fileOut = fs.create(file, false);
	      return new LineRecordWriter(fileOut, keyValueSeparator);
	    } else {
	      FSDataOutputStream fileOut = fs.create(file, false);
	      return new LineRecordWriter(new DataOutputStream
	                                        (codec.createOutputStream(fileOut)),
	                                        keyValueSeparator);
	    }
	}

	protected static class LineRecordWriter extends
			RecordWriter<PointWritable, PointWritable> {
		private static final String utf8 = "UTF-8";
		private static final byte[] newline;
		static {
			try {
				newline = "\n".getBytes(utf8);
			} catch (UnsupportedEncodingException uee) {
				throw new IllegalArgumentException("can't find " + utf8
						+ " encoding");
			}
		}

		protected DataOutputStream out;
		private final byte[] keyValueSeparator;

		public LineRecordWriter(DataOutputStream out, String keyValueSeparator) {
			this.out = out;
			try {
				this.keyValueSeparator = keyValueSeparator.getBytes(utf8);
			} catch (UnsupportedEncodingException uee) {
				throw new IllegalArgumentException("can't find " + utf8
						+ " encoding");
			}
		}

		public LineRecordWriter(DataOutputStream out) {
			this(out, "\t");
		}

		public synchronized void write(PointWritable key, PointWritable value)
				throws IOException {

			String s;
			if (key != null) {
				s = createString(key);
				out.write(s.getBytes(utf8));
			}
			if (key != null && value != null)
				out.write(keyValueSeparator);
			if (value != null) {
				s = createString(value);
				out.write(s.getBytes(utf8));
			}
			out.write(newline);
		}

		public synchronized void close(TaskAttemptContext context)
				throws IOException {
			out.close();
		}
	}
}
