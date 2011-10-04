package utils;

import integration.FloatInterval;
import integration.IIntervalNamed;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import lightLogger.Logger;

public class NIData {

	private static final Class<NIData> CLAZZ = NIData.class;

	public static final String CHARSET = "UTF-8";

	public enum Argument {
		OUTPUT("output", 0), START("start", 1), END("end", 2), INTERVALS(
				"intervals", 3), IDENTIFIER("identifier", 4);

		public final String name;
		public final int index;

		private Argument(String name, int index) {
			this.name = name;
			this.index = index;
		}
	}

	public static void main(String[] args) {
		if (args.length < 5) {
			StringBuilder sb = new StringBuilder();
			sb.append("Arguments:");
			for (Argument arg : Argument.values())
				sb.append(" <" + arg.name + ">");
			System.out.println(sb.toString());
			System.exit(1);
		}

		final String output = args[Argument.OUTPUT.index];
		final float start = Float.parseFloat(args[Argument.START.index]);
		final float end = Float.parseFloat(args[Argument.END.index]);
		final int count = Integer.parseInt(args[Argument.INTERVALS.index]);
		final String identifier = args[Argument.IDENTIFIER.index];

		List<IIntervalNamed<String, Float>> intervals = generateIntervals(start, end, count,
				identifier);

		write(intervals, output);
	}

	public static List<IIntervalNamed<String, Float>> generateIntervals(float start,
			float end, int count, String identifier) {
		List<IIntervalNamed<String, Float>> intervals = new ArrayList<IIntervalNamed<String, Float>>(
				count);
		final float offset = (end - start) / count;
		IIntervalNamed<String, Float> interval = null;

		float tmpStart;
		for (int i = 0; i < count; i++) {
			tmpStart = start + i * offset;
			interval = new FloatInterval(tmpStart, tmpStart + offset,
					identifier);
			intervals.add(interval);
		}

		return intervals;
	}

	public static boolean write(List<IIntervalNamed<String, Float>> intervals, String file) {
		boolean res = true;
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			write(fos, intervals);
		} catch (IOException e) {
			Logger.logError(CLAZZ, "Could not write input data.");
			e.printStackTrace();
			res = false;
		} finally {
			if (fos != null)
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return res;
	}

	private static void write(OutputStream os,
			List<IIntervalNamed<String, Float>> intervals)
			throws UnsupportedEncodingException, IOException {
		for (IIntervalNamed<String, Float> i : intervals) {
			os.write(Intervals.createString(i)
					.getBytes(CHARSET));
			os.write("\n".getBytes(CHARSET));
		}
	}

	public static List<IIntervalNamed<String, Float>> readIIntervals(File file) {
		List<IIntervalNamed<String, Float>> intervals = new LinkedList<IIntervalNamed<String, Float>>();
		Scanner sc = null;
		try {
			sc = new Scanner(file);

			String line;
			IIntervalNamed<String, Float> interval;
			while (sc.hasNext()) {
				line = sc.nextLine();

				interval = Intervals.createFloatIntervalNamed(line);
				intervals.add(interval);
			}
		} catch (Exception e) {
			Logger.logError(CLAZZ, "Could not open/read file.");
			e.printStackTrace();
		} finally {
			if (sc != null)
				sc.close();
		}
		return intervals;
	}

}
