package gsod;

public class DataSet {

	public final static int YEAR_START = 15 - 1;
	public final static int YEAR_END = 18 - 1;

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
		line = line.replace('*', ' ');
		return (int) (Double.parseDouble(line.substring(START, END + 1)) * 10);
	}

}
