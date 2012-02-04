package mathFunction;

/**
 * Represents a mathematical function.
 * 
 * @author Christof Pieloth
 * 
 * @param <T>
 */
public interface IMathFunction<T extends Number> {

	public static final String FUNCTION_NAME = "f";

	/**
	 * Calculates the function value at position x.
	 * 
	 * @param x
	 * @return
	 */
	public T getValue(T x);

	/**
	 * Returns the function as a OpenCL C method by using FUNCTION_NAME as
	 * method name.
	 * 
	 * @return
	 */
	public String getOpenCLFunction();
}
