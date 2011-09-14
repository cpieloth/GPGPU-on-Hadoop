package clustering;

public class CPoint extends Point implements ICPoint<Float> {

	private IPoint<Float> centroid = null;

	public CPoint(IPoint<Float> point) {
		super(point.getDim());
		this.values = point.getDims();
	}

	public CPoint(int dim) {
		super(dim);
	}

	@Override
	public IPoint<Float> getCentroid() {
		return this.centroid;
	}

	@Override
	public void setCentroid(IPoint<Float> label) {
		this.centroid = label;
		this.hash = null;
	}

	@Override
	public int hashCode() {
		if (this.hash == null) {
			int hash = ~super.hashCode();
			if (this.centroid != null)
				hash |= this.centroid.hashCode();
			this.hash = Integer.valueOf(hash);
		}
		return this.hash;
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

}
