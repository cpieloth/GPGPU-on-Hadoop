package cl_util;

public interface ICLSummarizer<T> extends ICLBufferedOperation<T>{

	public void resetResult();

	public T getSum();
}
