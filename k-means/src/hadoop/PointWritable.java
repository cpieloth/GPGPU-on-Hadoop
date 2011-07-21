package hadoop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableUtils;

import clustering.Point;

/**
 * Hadoop implementation of IPoint to use as key and value.
 * 
 * @author christof
 * 
 */
public class PointWritable extends Point implements
		WritableComparable<PointWritable> {

	public PointWritable() {
		super(0);
	}

	public PointWritable(int dim) {
		super(dim);
	}

	@Override
	public void readFields(DataInput arg0) throws IOException {
		int dim = WritableUtils.readVInt(arg0);
		this.values = new float[dim];
		for (int d = 0; d < dim; d++)
			this.values[d] = arg0.readFloat();
	}

	@Override
	public void write(DataOutput arg0) throws IOException {
		WritableUtils.writeVInt(arg0, this.values.length);
		for (float v : this.values)
			arg0.writeFloat(v);
	}

	@Override
	public int compareTo(PointWritable arg0) {
		double dist = 0;
		for (int d = 0; d < this.getDim(); d++)
			dist += this.values[d] * this.values[d];
		dist = Math.sqrt(dist);

		double oDist = 0;
		for (int d = 0; d < arg0.getDim(); d++)
			oDist += arg0.get(d) * arg0.get(d);
		oDist = Math.sqrt(oDist);

		dist -= oDist;
		if(dist < 0)
			return -1;
		else if(dist > 0)
			return 1;
		else
			return 0;
	}

}
