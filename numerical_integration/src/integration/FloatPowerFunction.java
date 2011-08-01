package integration;

public class FloatPowerFunction implements IMathFunction<Float> {

	private Float exponent;
	
	public FloatPowerFunction(Float exponent) {
		this.exponent = exponent;
	}
	
	public Float getIntegral(IInterval<Float> interv) {
		float end = interv.getEnd();
		float start = interv.getBegin();
		float result = 0;
		
		result += (1f/(this.exponent+1)) * Math.pow(end, this.exponent+1);
		result -= (1f/(this.exponent+1)) * Math.pow(start, this.exponent+1);
		
		return result;
	}
	
	@Override
	public Float getValue(Float x) {
		return (float) Math.pow(x, exponent);
	}

	@Override
	public String getOpenCLFunction() {
		return "float " + FUNCTION_NAME + "(float x) { return pow(x, " + exponent.floatValue() + "); }";
	}

}
