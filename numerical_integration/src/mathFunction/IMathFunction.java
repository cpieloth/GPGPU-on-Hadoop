package mathFunction;

public interface IMathFunction<T extends Number> {
	
	public static final String FUNCTION_NAME = "f";

	public T getValue(T x);
	
	public String getOpenCLFunction();
}
