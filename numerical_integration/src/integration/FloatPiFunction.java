package integration;

public class FloatPiFunction implements IMathFunction<Float> {

	@Override
	public Float getValue(Float x) {
		return 4f / (1 + x * x);
	}

	@Override
	public String getOpenCLFunction() {
		return "float " + FUNCTION_NAME + "(float x) { return 4/(1+x*x); }";
	}

}
