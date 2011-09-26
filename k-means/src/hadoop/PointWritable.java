package hadoop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableUtils;

import clustering.ICPoint;
import clustering.IPoint;
import clustering.Point;

/**
 * Decorator for Hadoop implementation of ICPoint to use as key and value. NOTICE: Centroid is ignored by readFields() and write()!
 * 
 * @author christof
 * 
 */
public class PointWritable implements ICPoint<Float>, WritableComparable<PointWritable> {

	private IPoint<Float> point;
	// new
	private PointWritable centroid;
	
	public PointWritable() {
		this(0);
	}

	public PointWritable(int dim) {
		this.point = new Point(dim);
	}

	public PointWritable(IPoint<Float> point) {
		this.point = point;
	}

	@Override
	public void readFields(DataInput arg0) throws IOException {
		int dim = WritableUtils.readVInt(arg0);
		this.point = new Point(dim);
		for (int d = 0; d < dim; d++)
			this.point.set(d, arg0.readFloat());
	}

	@Override
	public void write(DataOutput arg0) throws IOException {
		WritableUtils.writeVInt(arg0, this.point.getDim());
		for (float v : this.point.getDims())
			arg0.writeFloat(v);
	}

	@Override
	public int compareTo(PointWritable arg0) {
		double dist = 0;
		for (int d = 0; d < this.getDim(); d++)
			dist += this.get(d) * this.get(d);
		dist = Math.sqrt(dist);

		double oDist = 0;
		for (int d = 0; d < arg0.getDim(); d++)
			oDist += arg0.get(d) * arg0.get(d);
		oDist = Math.sqrt(oDist);

		dist -= oDist;
		if (dist < 0)
			return -1;
		else if (dist > 0)
			return 1;
		else
			return 0;
	}

	@Override
	public void set(int dim, Float val) {
		this.point.set(dim, val);
	}

	@Override
	public Float get(int dim) {
		return this.point.get(dim);
	}

	@Override
	public Float[] getDims() {
		return this.point.getDims();
	}

	@Override
	public int getDim() {
		return this.point.getDim();
	}

	@Override
	public String toString() {
		return this.point.toString();
	}

	@Override
	public int hashCode() {
		return this.point.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!obj.getClass().equals(this.getClass())) {
			return false;
		} else
			return this.hashCode() == obj.hashCode();
	}

	// new
	@Override
	public IPoint<Float> getCentroid() {
		return this.centroid;
	}

	@Override
	public void setCentroid(IPoint<Float> centroid) {
		this.centroid = (PointWritable)centroid;
	}

}
