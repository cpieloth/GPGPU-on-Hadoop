package utils;

import integration.FloatInterval;
import integration.FloatPolynomialFunction;
import integration.FloatPowerFunction;
import integration.FloatXSINXFunction;
import integration.IInterval;
import integration.IMathFunction;
import integration.INumeriacalIntegration;
import integration.TrapeziumIntegration;
import integration.TrapeziumIntegrationCL;

import java.io.File;
import java.util.List;

import lightLogger.Level;
import lightLogger.Logger;
import stopwatch.StopWatch;

public class NIStarter {

	public static final Level TIME_LEVEL = new Level(128, "TIME");

	private static final Class<NIStarter> CLAZZ = NIStarter.class;

	public enum Argument {
		INPUT("input", 0), FUNCTION(Argument.POLYNOM + "|" + Argument.POWER
				+ "|" + Argument.XSINX, 1), EXPONENT("exponent", 2), TYPE(
				Argument.CPU + "|" + Argument.OCL, 3);

		public final String name;
		public final int index;

		private Argument(String name, int index) {
			this.name = name;
			this.index = index;
		}

		public static final String CPU = "cpu";
		public static final String OCL = "ocl";
		public static final String POLYNOM = "poly";
		public static final String POWER = "pow";
		public static final String XSINX = "xsinx";

	}

	public static void main(String[] args) {
		if (args.length < 4) {
			StringBuilder sb = new StringBuilder();
			sb.append("Arguments:");
			for (Argument arg : Argument.values())
				sb.append(" <" + arg.name + ">");
			System.out.println(sb.toString());
			System.exit(1);
		}

		final String iFile = args[Argument.INPUT.index];
		final String type = args[Argument.TYPE.index];
		final String func = args[Argument.FUNCTION.index];
		final float exp = Float.parseFloat(args[Argument.EXPONENT.index]);

		IMathFunction<Float> function = null;
		if (Argument.POLYNOM.equals(func))
			function = new FloatPolynomialFunction((int) exp);
		else if (Argument.POWER.equals(func))
			function = new FloatPowerFunction(exp);
		else if (Argument.XSINX.equals(func))
			function = new FloatXSINXFunction();
		else {
			Logger.logError(CLAZZ, "Unknown function!");
			System.exit(1);
		}

		List<IInterval<Float>> intervals = NIData
				.readIIntervals(new File(iFile));

		if (intervals == null || intervals.isEmpty()) {
			Logger.logError(CLAZZ, "Empty intervals!");
			System.exit(1);
		}

		StopWatch sw = new StopWatch("time" + type + "=", ";");
		sw.start();

		INumeriacalIntegration<Float> integration = null;
		if (Argument.CPU.equals(type))
			integration = new TrapeziumIntegration();
		else if (Argument.OCL.equals(type))
			integration = new TrapeziumIntegrationCL();
		else {
			Logger.logError(CLAZZ, "Unknown type");
			System.exit(1);
		}

		integration.setFunction(function);

		StopWatch swCompute = new StopWatch("timeCompute" + type + "=", ";");
		swCompute.start();

		float integral = 0;
		for (IInterval<Float> interval : intervals) {
			integral += integration.getIntegral(interval);
		}

		swCompute.stop();
		sw.stop();
		Logger.log(TIME_LEVEL, CLAZZ, swCompute.getTimeString());
		Logger.log(TIME_LEVEL, CLAZZ, sw.getTimeString());

		Logger.logInfo(CLAZZ, "Numerical result: " + integral);

		try {
			IInterval<Float> interval = new FloatInterval(intervals.get(0)
					.getBegin(), intervals.get(intervals.size() - 1).getEnd(),
					0);
			if (FloatPolynomialFunction.class.isInstance(function)) {
				integral = ((FloatPolynomialFunction) function)
						.getIntegral(interval);
				Logger.logInfo(CLAZZ, "Analytical result: " + integral);
			} else if (FloatPowerFunction.class.isInstance(function)) {
				integral = ((FloatPowerFunction) function)
						.getIntegral(interval);
				Logger.logInfo(CLAZZ, "Analytical result: " + integral);
			} else if (FloatXSINXFunction.class.isInstance(function)) {
				integral = ((FloatXSINXFunction) function)
						.getIntegral(interval);
				Logger.logInfo(CLAZZ, "Analytical result: " + integral);
			} else {
				Logger.logInfo(CLAZZ, "No analytical solution available!");
			}
		} catch (Exception e) {
			Logger.logError(CLAZZ, "Could not compute analytical result!");
		}
	}

}
