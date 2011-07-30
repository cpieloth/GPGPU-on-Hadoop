package integration;

/**
 * Float implementation of IInterval.
 * 
 * @author christof
 * 
 */

public class FloatInterval implements IInterval<Float> {

	private Float begin, end;
	private int resolution;

	public FloatInterval(Float begin, Float end, int resolution) {
		this.begin = begin;
		this.end = end;
		this.resolution = resolution;
	}

	@Override
	public Float getBegin() {
		return this.begin;
	}

	@Override
	public void setBegin(Float begin) {
		this.begin = begin;
	}

	@Override
	public Float getEnd() {
		return this.end;
	}

	@Override
	public void setEnd(Float end) {
		this.end = end;
	}

	@Override
	public int getResolution() {
		return this.resolution;
	}

	@Override
	public void setResolution(int resolution) {
		this.resolution = resolution;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(this.begin.toString());
		sb.append(",");
		sb.append(this.end.toString());
		sb.append("] ");
		sb.append(this.resolution);
		return sb.toString();
	}

}
