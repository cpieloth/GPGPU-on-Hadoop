package mathFunction;

/**
 * Float implementation of IInterval.
 * 
 * @author christof
 * 
 */

public class FloatInterval implements IIntervalNamed<String, Float> {

	private Float begin, end;
	private String identifier;

	public FloatInterval(Float begin, Float end, String identifier) {
		this.begin = begin;
		this.end = end;
		this.identifier = identifier;
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
	public String getIdentifier() {
		return this.identifier;
	}

	@Override
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.identifier);
		sb.append(IInterval.WHITESPACE);
		sb.append("[");
		sb.append(this.begin.toString());
		sb.append(IInterval.SEPARATOR);
		sb.append(this.end.toString());
		sb.append("]");
		return sb.toString();
	}

}
