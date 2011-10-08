package mathFunction;


public class FloatXSINXFunction implements IMathFunction<Float> {

	public Float getIntegral(IInterval<Float> interv) {
		float x = interv.getEnd();
		double i = -x * Math.cos(x) + Math.sin(x);
		x = interv.getBegin();
		i -= -x * Math.cos(x) + Math.sin(x);
		return (float) i;
	}

	@Override
	public Float getValue(Float x) {
		return (float) (x * Math.sin(x));
	}

	@Override
	public String getOpenCLFunction() {
		// return "float " + FUNCTION_NAME + "(float x) { return pow(sin(x), " +
		// exponent.floatValue() + "f); }";
		return "float " + FUNCTION_NAME + "(float x) { return x * sin(x); }";
	}

}
