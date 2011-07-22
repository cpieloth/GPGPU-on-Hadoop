package clustering;



public class Point implements IPoint<Float> {

	protected Float[] values;

	public Point(int dim) {
		this.values = new Float[dim];

	}

	@Override
	public void set(int dim, Float val) {
		this.values[dim] = val;
	}

	@Override
	public Float get(int dim) {
		return this.values[dim];
	}

	@Override
	public int getDim() {
		return this.values.length;
	}

	@Override
	public Float[] getDims() {
		return this.values;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (int d = 0; d < this.values.length; d++) {
			sb.append(this.values[d]);
			sb.append(";");
		}
		sb.setCharAt(sb.length() - 1, ')');
		return sb.toString();
	}

	
	@Override
	public int hashCode() {
		StringBuilder sb = new StringBuilder();
		for(int d = 0; d < this.getDim(); d++)
			sb.append(this.values[d]);
		return sb.toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!obj.getClass().equals(this.getClass())) {
			return false;
		} else {
			return this.hashCode() == obj.hashCode();
		}
	}

}
