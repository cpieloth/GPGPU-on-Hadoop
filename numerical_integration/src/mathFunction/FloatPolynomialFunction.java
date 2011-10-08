package mathFunction;


public class FloatPolynomialFunction  implements IMathFunction<Float> {

	private int degree;
	
	public FloatPolynomialFunction(int degree) {
		this.degree = degree;
	}
	
	public Float getIntegral(IInterval<Float> interv) {
		float end = interv.getEnd();
		float start = interv.getBegin();
		float result = 0;
		for(int i = 0; i <= this.degree; i++)
			result += ((float)i/(float)(i+1)) * Math.pow(end, i+1);
		
		for(int i = 0; i <= this.degree; i++)
			result -= ((float)i/(float)(i+1)) * Math.pow(start, i+1);
		return result;
	}
	
	@Override
	public Float getValue(Float x) {
		float result = 0;
		for(int i = 0; i <= this.degree; i++)
			result += i * Math.pow(x, i);
		return result;
	}

	@Override
	public String getOpenCLFunction() {
		StringBuilder sb = new StringBuilder();
		sb.append("float ");
		sb.append(FUNCTION_NAME);
		sb.append("(float x) {");
		
		sb.append("return");
		
		for(int i = 0; i <= this.degree; i++)
			sb.append(" " + i + " * pow(x, " + i + ") +");
		
		sb.deleteCharAt(sb.length()-1);		
		sb.append("; }");
		
		return sb.toString();
	}

}
