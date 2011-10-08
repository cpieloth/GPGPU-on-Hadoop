package hadoop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


import mathFunction.FloatInterval;
import mathFunction.IInterval;

import org.apache.hadoop.io.WritableComparable;

import utils.Intervals;

/**
 * Decorator for Hadoop implementation of IInterval to use as key and value.
 * 
 * @author christof
 * 
 */
public class FloatIntervalWritable implements IInterval<Float>,
		WritableComparable<FloatIntervalWritable> {

	private IInterval<Float> interval;

	public FloatIntervalWritable() {
		this(0, 0);
	}

	public FloatIntervalWritable(float begin, float end) {
		this.interval = new FloatInterval(begin, end, IInterval.DEFAULT_IDENTIFIER);
	}

	public FloatIntervalWritable(IInterval<Float> interval) {
		this.interval = interval;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeFloat(this.getBegin());
		out.writeFloat(this.getEnd());
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		this.setBegin(in.readFloat());
		this.setEnd(in.readFloat());
	}

	@Override
	public int compareTo(FloatIntervalWritable arg0) {
		float size = this.getEnd() - this.getBegin();
		float oSize = arg0.getEnd() - arg0.getBegin();

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
		return this.interval.getBegin();
	}

	@Override
	public void setBegin(Float begin) {
		this.interval.setBegin(begin);
	}

	@Override
	public Float getEnd() {
		return this.interval.getEnd();
	}

	@Override
	public void setEnd(Float end) {
		this.interval.setEnd(end);
	}

	@Override
	public String toString() {
		return Intervals.createString(this.interval);
	}

}
