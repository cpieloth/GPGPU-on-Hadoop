package clustering;

public class Point<T> implements IPoint<T> {
	
	private Object[] values;
	
	public Point(int dim) {
		this.values = new Object[dim];
		
	}
	
	@Override
	public void set(int dim, T val) {
		this.values[dim] = val;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T get(int dim) {
		return (T) this.values[dim];
	}

	@Override
	public int getDim() {
		return this.values.length;
	}

}
