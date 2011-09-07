package cl_util;

/**
 * Interface for a buffered OpenCL operation. The implementation contains a
 * fixed-sized buffer (mostly an array) and the associated OpenCL buffer. Due to
 * performance issues the buffer should only created once at initialization or
 * resetBuffer(). Each implementation must provide a getResult() method or
 * similar.
 * 
 * @author christof
 * 
 * @param <T>
 *            Data types to be stored in the buffer or to be computed.
 */
public interface ICLBufferedOperation<T> {

	/**
	 * Maximum buffer items. Should be hardware and data type dependent!
	 * 
	 * @return Maximum objects which can be stored in the buffer.
	 */
	public int getMaxBufferItems();

	/**
	 * Current maximum buffer items. E.g. set by resetBuffer(int bufferItems);
	 * 
	 * @return Maximum objects which can be stored in the buffer.
	 */
	public int getCurrentMaxBufferItems();

	/**
	 * Resets the counter and creates a new buffer. Buffer could be resized or
	 * fit to getMaxBufferItems().
	 * 
	 * @param bufferItems
	 *            Minimum buffer size.
	 */
	public int resetBuffer(int bufferItems);

	/**
	 * Resets the buffer to its maximum.
	 */
	public int resetBuffer();

	/**
	 * Appends a object to the buffer. If the buffer is full, a intermediate
	 * result should be computed.
	 * 
	 * @param v
	 */
	public void put(T v);

}
