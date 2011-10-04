package integration;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TrapeziumIntegration implements INumeriacalIntegrationMulti<Float> {

	private IMathFunction<Float> function;
	private final int MAX_ITEM_SIZE = 1024;
	private int bufferSize = MAX_ITEM_SIZE;
	private ArrayList<IInterval<Float>> intervals = new ArrayList<IInterval<Float>>(
			bufferSize);

	@Override
	public void setFunction(IMathFunction<Float> function) {
		this.function = function;
	}

	@Override
	public Float getIntegral(IInterval<Float> interval, int resolution) {
		float offset = interval.getEnd() - interval.getBegin();
		float h = offset / resolution;
		float result = 0;
		float start = interval.getBegin();
		int n = resolution;

		result += function.getValue(start) / 2;

		for (int i = 1; i < n; i++)
			result += function.getValue(start + h * i);

		result += function.getValue(start + n * h) / 2;

		return result * h;
	}

	@Override
	public int getBufferSize() {
		return this.bufferSize;
	}

	@Override
	public int getBufferCount() {
		return this.intervals.size();
	}

	@Override
	public int getMaxItemSize() {
		return MAX_ITEM_SIZE;
	}

	@Override
	public int getItemCount() {
		return this.intervals.size();
	}

	@Override
	public int getCurrentMaxItemSize() {
		return this.bufferSize;
	}

	@Override
	public int reset(int expectedItemSize) {
		bufferSize = expectedItemSize < MAX_ITEM_SIZE ? expectedItemSize
				: MAX_ITEM_SIZE;
		this.intervals.clear();
		this.intervals.ensureCapacity(bufferSize);
		return bufferSize;
	}

	@Override
	public int reset() {
		return reset(MAX_ITEM_SIZE);
	}

	@Override
	public boolean put(IInterval<Float> v) {
		if (this.intervals.size() < bufferSize) {
			this.intervals.add(v);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public List<Float> getIntegrals(int resolution) {
		List<Float> results = new LinkedList<Float>();
		for (IInterval<Float> interval : intervals)
			results.add(getIntegral(interval, resolution));
		return results;
	}

}
