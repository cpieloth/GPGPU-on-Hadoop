package util;

import org.apache.hadoop.io.IntWritable;

public class Convert {

	public static final String toString(int[] a) {
		StringBuilder sb = new StringBuilder();
		for (int i : a) {
			sb.append(i + " ");
		}
		return sb.toString();
	}
	
	public static final String toString(IntWritable[] a) {
		StringBuilder sb = new StringBuilder();
		for (IntWritable i: a) {
			sb.append(i.toString() + " ");
		}
		return sb.toString();
	}

	public static int[] toInt(IntWritable[] a) {
		int[] tmp = new int[a.length];
		for (int i = 0; i < a.length; ++i)
			tmp[i] = a[i].get();
		return tmp;
	}

}
