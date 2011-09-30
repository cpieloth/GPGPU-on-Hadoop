package utils;

import hadoop.FloatIntervalInputFormat;
import integration.FloatInterval;
import integration.IInterval;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lightLogger.Logger;

public class Intervals {

	private static final Pattern pattern = Pattern
			.compile("\\[(-*\\d+.\\d+),(-*\\d+.\\d+)\\]\\s+(\\d+)");

	public static String createString(IInterval<Float> value,
			final String whitespace) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(value.getBegin().toString());
		sb.append(",");
		sb.append(value.getEnd().toString());
		sb.append("]" + whitespace);
		sb.append(value.getResolution());
		return sb.toString();
	}

	public static IInterval<Float> createFloatInterval(String line) {
		Matcher matcher = pattern.matcher(line);
		if (matcher.matches()) {
			return new FloatInterval(Float.valueOf(matcher.group(1)),
					Float.valueOf(matcher.group(2)), Integer.valueOf(matcher
							.group(3)));
		} else {
			Logger.logError(FloatIntervalInputFormat.class,
					"Could not create interval from line: " + line);
			return null;
		}
	}
}
