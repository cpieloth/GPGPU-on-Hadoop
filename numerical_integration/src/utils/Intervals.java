package utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lightLogger.Logger;
import mathFunction.FloatInterval;
import mathFunction.IInterval;
import mathFunction.IIntervalNamed;

/**
 * Helper class to generate intervals and transform data.
 * 
 * @author Christof Pieloth
 * 
 */
public class Intervals {

	/*
	 * private static final String patternFloat = "\\[(-*\\d+.\\d+)" +
	 * IInterval.SEPARATOR + "(-*\\d+.\\d+)\\]";
	 */
	private static final String patternFloat = "\\[(.+)" + IInterval.SEPARATOR
			+ "(.+)\\]";

	private static final Pattern patternNamed = Pattern.compile("(.*)"
			+ IInterval.WHITESPACE + patternFloat);
	private static final Pattern pattern = Pattern.compile(patternFloat);

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
			try {
				return new FloatInterval(Float.parseFloat(matcher.group(2)),
						Float.parseFloat(matcher.group(3)), matcher.group(1));
			} catch (NumberFormatException e) {
				Logger.logError(Intervals.class,
						"Could not create interval from line: " + line);
				return null;
			}
		} else {
			Logger.logError(Intervals.class,
					"Could not create interval from line: " + line);
			return null;
		}
	}

	public static IInterval<Float> createFloatInterval(String line) {
		Matcher matcher = pattern.matcher(line);
		if (matcher.matches()) {
			try {
				return new FloatInterval(Float.valueOf(matcher.group(1)),
						Float.valueOf(matcher.group(2)),
						IInterval.DEFAULT_IDENTIFIER);
			} catch (NumberFormatException e) {
				Logger.logError(Intervals.class,
						"Could not create interval from line: " + line);
				return null;
			}
		} else {
			return createFloatIntervalNamed(line);
		}
	}
}
