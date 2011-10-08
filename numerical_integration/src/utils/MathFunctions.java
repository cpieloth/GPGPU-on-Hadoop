package utils;

import mathFunction.FloatPiFunction;
import mathFunction.FloatPolynomialFunction;
import mathFunction.FloatPowerFunction;
import mathFunction.FloatXSINXFunction;
import mathFunction.IMathFunction;

public class MathFunctions {

	public enum MathFunction {
		PI("pi", FloatPiFunction.class), POLY("poly",
				FloatPolynomialFunction.class), POW("pow",
				FloatPolynomialFunction.class), XSINX("xsinx",
				FloatXSINXFunction.class);

		public final String identifier;
		public final Class<? extends IMathFunction<?>> clazz;

		private MathFunction(String identifier,
				Class<? extends IMathFunction<?>> clazz) {
			this.identifier = identifier;
			this.clazz = clazz;
		}
	}

	public static boolean isFunctionAvailable(String identifier) {
		for (MathFunction mf : MathFunction.values()) {
			if (mf.identifier.equals(identifier))
				return true;
		}
		return false;
	}

	public static IMathFunction<Float> getFunction(String identfier, String var) {
		if (MathFunction.PI.identifier.equals(identfier))
			return new FloatPiFunction();
		else if (MathFunction.POLY.identifier.equals(identfier))
			return new FloatPolynomialFunction(Integer.parseInt(var));
		else if (MathFunction.POW.identifier.equals(identfier))
			return new FloatPowerFunction(Float.parseFloat(var));
		else if (MathFunction.XSINX.identifier.equals(identfier))
			return new FloatXSINXFunction();
		else
			return null;
	}
	
	public static String getAvailableIdentifer(char separator) {
		StringBuilder sb = new StringBuilder();
		
		for (MathFunction mf : MathFunction.values()) {
			sb.append(mf.identifier);
			sb.append(separator);
		}
		sb.deleteCharAt(sb.length() - 1);
		
		return sb.toString();
	}
}
