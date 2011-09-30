package hadoop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import integration.FloatInterval;
import integration.IInterval;

import org.apache.hadoop.io.WritableComparable;

/**
 * Decorator for Hadoop implementation of IInterval to use as key and value.
 * 
 * @author christof
 * 
 */
public class FloatIntervalWritable implements IInterval<Float>,
		WritableComparable<FloatIntervalWritable> {

	private IInterval<Float> intervall;

	public FloatIntervalWritable() {
		this(0, 0, 0);
	}

	public FloatIntervalWritable(float begin, float end, int resolution) {
		this.intervall = new FloatInterval(begin, end, resolution);
	}

	public FloatIntervalWritable(IInterval<Float> intervall) {
		this.intervall = intervall;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeFloat(this.getBegin());
		out.writeFloat(this.getEnd());
		out.writeInt(this.getResolution());
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		this.setBegin(in.readFloat());
		this.setEnd(in.readFloat());
		this.setResolution(in.readInt());
	}

	@Override
	public int compareTo(FloatIntervalWritable arg0) {
		float size = (this.getEnd() - this.getBegin()) * this.getResolution();
		float oSize = (arg0.getEnd() - arg0.getBegin()) * arg0.getResolution();

		size -= oSize;
		if (size > 0)
			return 1;
		else if (size < 0)
			return -1;
		else
			return 0;
	}

	@Override
	public Float getBegin() {
		return this.intervall.getBegin();
	}

	@Override
	public void setBegin(Float begin) {
		this.intervall.setBegin(begin);
	}

	@Override
	public Float getEnd() {
		return this.intervall.getEnd();
	}

	@Override
	public void setEnd(Float end) {
		this.intervall.setEnd(end);
	}

	@Override
	public int getResolution() {
		return this.intervall.getResolution();
	}

	@Override
	public void setResolution(int resolution) {
		this.intervall.setResolution(resolution);
	}

	@Override
	public String toString() {
		return this.intervall.toString();
	}

}
