package gsod;

public class DataSet {

	public final static int YEAR_START = 15;
	public final static int YEAR_END = 18;

	public final static int TEMP_START = 25;
	public final static int TEMP_END = 30;

	public final static int MAX_START = 103;
	public final static int MAX_END = 108;

	public final static int MISSING = 9999;

	public static String getYear(String line) {
		return line.substring(YEAR_START, YEAR_END + 1);
	}

	public static int getTemp(String line) {
		return getInt(line, TEMP_START, TEMP_END);
	}

	public static int getMax(String line) {
		return getInt(line, MAX_START, MAX_END);
	}

	private static int getInt(String line, final int START, final int END) {
		// parseInt doesn't like leading plus signs
		if (line.charAt(87) == '+')
			return Integer.parseInt(line.substring(START + 1, END + 1));
		else
			return Integer.parseInt(line.substring(START, END + 1));
	}

}
