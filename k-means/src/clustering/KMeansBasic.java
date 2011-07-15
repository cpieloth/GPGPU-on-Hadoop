package clustering;

import java.util.Iterator;
import java.util.List;

import lightLogger.Logger;

public class KMeansBasic implements IKMeansBasic {
	
	private static final Class<KMeansBasic> CLAZZ = KMeansBasic.class;
	
	private int dim;
	
	public KMeansBasic(int dim) {
		this.dim = dim;
	}

	@Override
	public double computeDistance(final IPoint p, final IPoint c) {
		double dist = 0;
		for (int d = 0; d < p.getDim(); d++)
			dist += Math.pow(c.get(d) - p.get(d), 2);
		return Math.sqrt(dist);
	}

	@Override
	public IPoint computeCentroid(final List<ICPoint> points) {
		Logger.logTrace(CLAZZ,
				"computeCentroid() - points.size(): " + points.size());
		Point c = new Point(this.dim);

		Iterator<ICPoint> it = points.iterator();
		IPoint p = it.next();

		for (int d = 0; d < this.dim; d++)
			c.set(d, p.get(d));

		while (it.hasNext()) {
			p = it.next();
			for (int d = 0; d < this.dim; d++)
				c.set(d, c.get(d) + p.get(d));
		}

		int n = points.size();
		for (int d = 0; d < this.dim; d++)
			c.set(d, c.get(d) / n);

		return c;
	}
	
	@Override
	public int getDim() {
		return this.dim;
	}

}
