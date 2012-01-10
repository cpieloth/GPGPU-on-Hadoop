package utils;

import integration.INumericalIntegrationMulti;
import integration.TrapeziumIntegration;
import integration.TrapeziumIntegrationMultiCL;

import java.io.File;
import java.util.List;

import lightLogger.Level;
import lightLogger.Logger;
import mathFunction.FloatInterval;
import mathFunction.FloatPolynomialFunction;
import mathFunction.FloatPowerFunction;
import mathFunction.FloatXSINXFunction;
import mathFunction.IInterval;
import mathFunction.IIntervalNamed;
import mathFunction.IMathFunction;
import stopwatch.StopWatch;
import cl_util.CLInstance;

public class NIStarter {

	public static final Level TIME_LEVEL = new Level(128, "TIME");

	private static final Class<NIStarter> CLAZZ = NIStarter.class;

	public enum Argument {
		INPUT("input", 0), FUNCTION(MathFunctions.getAvailableIdentifer('|'), 1), EXPONENT(
				"exponent", 2), RESOLUTION("resolution", 3), TYPE(Argument.CPU
				+ "|" + Argument.OCL, 4);

		public final String name;
		public final int index;

		private Argument(String name, int index) {
			this.name = name;
			this.index = index;
		}

		public static final String CPU = "cpu";
		public static final String OCL = "ocl";

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

		final String iFile = args[Argument.INPUT.index];
		final String type = args[Argument.TYPE.index];
		final int resolution = Integer
				.parseInt(args[Argument.RESOLUTION.index]);

		IMathFunction<Float> function = MathFunctions.getFunction(
				args[Argument.FUNCTION.index], args[Argument.EXPONENT.index]);
		if (function == null) {
			Logger.logError(CLAZZ, "Unknown function!");
			System.exit(1);
		}

		List<IIntervalNamed<String, Float>> intervals = NIData
				.readIIntervals(new File(iFile));

		if (intervals == null || intervals.isEmpty()) {
			Logger.logError(CLAZZ, "Empty intervals!");
			System.exit(1);
		}

		StopWatch sw = new StopWatch("time" + type + "=", ";");
		sw.start();

		INumericalIntegrationMulti<Float> integration = null;
		if (Argument.CPU.equals(type))
			integration = new TrapeziumIntegration();
		else if (Argument.OCL.equals(type))
			integration = new TrapeziumIntegrationMultiCL(new CLInstance(
					CLInstance.TYPES.CL_GPU));
		else {
			Logger.logError(CLAZZ, "Unknown type");
			System.exit(1);
		}

		integration.setFunction(function);

		StopWatch swCompute = new StopWatch("timeCompute" + type + "=", ";");
		swCompute.start();

		float integral = 0;
		for (IInterval<Float> interval : intervals) {
			integration.put(interval);
		}
		List<Float> is = integration.getIntegrals(resolution);
		for (Float f : is)
			integral += f;

		swCompute.stop();
		sw.stop();
		Logger.log(TIME_LEVEL, CLAZZ, swCompute.getTimeString());
		Logger.log(TIME_LEVEL, CLAZZ, sw.getTimeString());

		Logger.logInfo(CLAZZ, "Numerical result: " + integral);

		try {
			IInterval<Float> interval = new FloatInterval(intervals.get(0)
					.getBegin(), intervals.get(intervals.size() - 1).getEnd(),
					IInterval.DEFAULT_IDENTIFIER);
			float expected = Float.NaN;
			if (FloatPolynomialFunction.class.isInstance(function)) {
				expected = ((FloatPolynomialFunction) function)
						.getIntegral(interval);
				Logger.logInfo(CLAZZ, "Analytical result: " + expected);
			} else if (FloatPowerFunction.class.isInstance(function)) {
				expected = ((FloatPowerFunction) function)
						.getIntegral(interval);
				Logger.logInfo(CLAZZ, "Analytical result: " + expected);
			} else if (FloatXSINXFunction.class.isInstance(function)) {
				expected = ((FloatXSINXFunction) function)
						.getIntegral(interval);
				Logger.logInfo(CLAZZ, "Analytical result: " + expected);
			} else {
				Logger.logInfo(CLAZZ, "No analytical solution available!");
			}
			if (expected != Float.NaN) {
				Logger.logInfo(CLAZZ,
						"Abweichung: " + Math.abs(expected - integral));
			}
		} catch (Exception e) {
			Logger.logError(CLAZZ, "Could not compute analytical result!");
		}
	}

}
