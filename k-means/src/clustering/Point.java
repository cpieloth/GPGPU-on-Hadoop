package clustering;

public class Point implements IPoint {

	protected double[] values;

	public Point(int dim) {
		this.values = new double[dim];

	}

	@Override
	public void set(int dim, double val) {
		this.values[dim] = val;
	}

	@Override
	public double get(int dim) {
		return this.values[dim];
	}

	@Override
	public int getDim() {
		return this.values.length;
	}

	@Override
	public double[] getDims() {
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

}
