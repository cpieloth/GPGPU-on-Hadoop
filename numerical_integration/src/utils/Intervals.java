package utils;

import integration.FloatInterval;
import integration.IInterval;
import integration.IIntervalNamed;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lightLogger.Logger;

public class Intervals {

	private static final Pattern patternNamed = Pattern.compile("(.*)"
			+ IInterval.WHITESPACE + "\\[(-*\\d+.\\d+)" + IInterval.SEPARATOR
			+ "(-*\\d+.\\d+)\\]");
	private static final Pattern pattern = Pattern.compile("\\[(-*\\d+.\\d+)"
			+ IInterval.SEPARATOR + "(-*\\d+.\\d+)\\]");

	public static String createString(IIntervalNamed<String, Float> value) {
		StringBuilder sb = new StringBuilder();
		sb.append(value.getIdentifier());
		sb.append(IInterval.WHITESPACE);
		sb.append("[");
		sb.append(value.getBegin().toString());
		sb.append(IInterval.SEPARATOR);
		sb.append(value.getEnd().toString());
		sb.append("]");
		return sb.toString();
	}

	public static String createString(IInterval<Float> value) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(value.getBegin().toString());
		sb.append(IInterval.SEPARATOR);
		sb.append(value.getEnd().toString());
		sb.append("]");
		return sb.toString();
	}

	public static IIntervalNamed<String, Float> createFloatIntervalNamed(
			String line) {
		Matcher matcher = patternNamed.matcher(line);
		if (matcher.matches()) {
			return new FloatInterval(Float.valueOf(matcher.group(2)),
					Float.valueOf(matcher.group(3)), matcher.group(1));
		} else {
			Logger.logError(Intervals.class,
					"Could not create interval from line: " + line);
			return null;
		}
	}

	public static IInterval<Float> createFloatInterval(String line) {
		Matcher matcher = pattern.matcher(line);
		if (matcher.matches()) {
			return new FloatInterval(Float.valueOf(matcher.group(1)),
					Float.valueOf(matcher.group(2)),
					IInterval.DEFAULT_IDENTIFIER);
		} else {
			matcher = patternNamed.matcher(line);
			if (matcher.matches()) {
				return new FloatInterval(Float.valueOf(matcher.group(2)),
						Float.valueOf(matcher.group(3)), matcher.group(1));
			} else {
				Logger.logError(Intervals.class,
						"Could not create interval from line: " + line);
				return null;
			}
		}
	}
}
