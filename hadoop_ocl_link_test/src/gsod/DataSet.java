package gsod;

public class DataSet {

	public final static int YEAR_START = 15-1;
	public final static int YEAR_END = 18-1;

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
		return (int)(Double.parseDouble(line.substring(START, END + 1)) * 10);
	}

	public static void main(String[] args) {
		String line = "010010 99999  20081220    24.6 24    18.5 24   997.8 24   996.6 24    5.7  6   15.6 24   27.2  999.9    29.8*   20.7*  0.01G 999.9  001000";
		System.out.println(DataSet.getYear(line));
		System.out.println(DataSet.getTemp(line));
		System.out.println(DataSet.getMax(line));
	}

}
