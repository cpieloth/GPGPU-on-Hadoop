package clustering;

public class CPoint extends Point implements ICPoint {

	private IPoint centroid = null;
	
	public CPoint(int dim) {
		super(dim);
	}

	@Override
	public IPoint getCentroid() {
		return this.centroid;
	}

	@Override
	public void setCentroid(IPoint label) {
		this.centroid = label;
	}

}
