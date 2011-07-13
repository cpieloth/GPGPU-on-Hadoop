package clustering;

public class LPoint<T, K> extends Point<T> implements ILPoint<T, K> {

	private K label;
	
	public LPoint(int dim) {
		super(dim);
	}

	@Override
	public K getLabel() {
		return this.label;
	}

	@Override
	public void setLabel(K label) {
		this.label = label;
	}

}
