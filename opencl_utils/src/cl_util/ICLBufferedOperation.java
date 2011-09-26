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
	 * Returns internal buffer size. If buffer is full, data is copied to OCL
	 * device.
	 * 
	 * @return internal buffer size
	 */
	public int getBufferSize();

	/**
	 * Returns the number of items that are stored in the internal buffer.
	 * 
	 * @return current buffer counter.
	 */
	public int getBufferCount();

	/**
	 * Maximum items size which can be stored by the OCL device. Should be
	 * hardware and data type dependent!
	 * 
	 * @return Maximum objects which can be stored in the buffer.
	 */
	public int getMaxItemSize();

	/**
	 * Returns the number of items that are stored in the OCL memory.
	 * 
	 * @return current item count.
	 */
	public int getItemCount();

	/**
	 * Size of the current allocated OCL memory.
	 * 
	 * @return Size of the current allocated OCL memory.
	 */
	public int getCurrentMaxItemSize();

	/**
	 * Resets the counter and allocates new OCL memory. OCL memory could be
	 * resized or fit to getMaxItemsSize().
	 * 
	 * @param expectedItemSize
	 *            Minimum buffer size.
	 * @return actual allocated item size
	 */
	public int reset(int expectedItemSize);

	/**
	 * Same like reset(getMaxItemSize()).
	 * 
	 * @return actual allocated item size
	 */
	public int reset();

	/**
	 * Appends a object to the buffer. If the buffer is full, it should be copied to OCL memory or a intermediate
	 * result should be computed.
	 * 
	 * @param v
	 * 
	 * @return false, if no result could be computed or no data could be copied!
	 */
	public boolean put(T v);

}
