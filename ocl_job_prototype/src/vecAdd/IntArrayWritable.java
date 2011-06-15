package vecAdd;

import java.util.List;

import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Writable;

public class IntArrayWritable extends ArrayWritable {

	public IntArrayWritable(int[] a) {
		super(IntWritable.class);

		IntWritable[] tmp = new IntWritable[a.length];
		for (int i = 0; i < a.length; ++i)
			tmp[i] = new IntWritable(a[i]);

		this.set(tmp);
	}

	public IntArrayWritable(List<IntWritable> l) {
		super(IntWritable.class);

		IntWritable[] tmp = new IntWritable[l.size()];
		tmp = l.toArray(tmp);
		this.set(tmp);
	}

	public IntArrayWritable() {
		super(IntWritable.class);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Writable w : this.get()) {
			sb.append(w.toString() + " ");
		}
		return sb.toString();
	}

	public int[] toIntArray() {
		Writable[] w = this.get();
		int[] a = new int[w.length];
		for (int i = 0; i < a.length; ++i) {
			a[i] = Integer.parseInt(w[i].toString());
		}
		return a;
	}
}