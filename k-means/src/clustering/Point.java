package clustering;

public class Point implements IPoint {

	protected float[] values;

	public Point(int dim) {
		this.values = new float[dim];

	}

	@Override
	public void set(int dim, float val) {
		this.values[dim] = val;
	}

	@Override
	public float get(int dim) {
		return this.values[dim];
	}

	@Override
	public int getDim() {
		return this.values.length;
	}

	@Override
	public float[] getDims() {
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
			IPoint p = (IPoint) obj;
			if (p.getDim() != this.getDim())
				return false;
			for (int i = 0; i < this.getDim(); i++)
				if (this.get(i) != p.get(i))
					return false;
			return true;
		}
	}

}
