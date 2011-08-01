package integration;



public class TrapeziumIntegration implements INumeriacalIntegration<Float> {

	private IMathFunction<Float> function;
	
	@Override
	public void setFunction(IMathFunction<Float> function) {
		this.function = function;
	}
	
	@Override
	public Float getIntegral(IInterval<Float> interval) {
		float offset = interval.getEnd() - interval.getBegin();
		float h = offset / interval.getResolution();
		float result = 0;
		float start = interval.getBegin();
		int n = interval.getResolution();

		result += function.getValue(start) / 2;

		for (int i = 1; i < n; i++)
			result += function.getValue(start + h * i);

		result += function.getValue(start + n * h) / 2;
		
		return result * h;
	}

}
