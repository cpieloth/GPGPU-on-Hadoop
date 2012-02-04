package cl_kernel;

import java.util.List;
import java.util.Map;

import cl_util.CLInstance;

import com.nativelibs4java.opencl.CLKernel;

/**
 * This interface is intended to simplify the work with OpenCL kernels. Every
 * "plain" OpenCL kernel (*.cl) should have one implementation of this
 * interface. An implementation ensures the correct compilation, work item/group
 * size and a safe kernel call. An implementation also needs a run method to
 * start the execution on the OpenCL device.
 * 
 * @author Christof Pieloth
 * 
 */
public interface ICLKernel {

	/**
	 * Generates an identifier for a specific kernel compilation. Kernels with
	 * same compile options (includes, defines, extended source) should have the
	 * same identifier.
	 * 
	 * @return
	 */
	public String getIdentifier();

	public String getKernelName();

	public String getKernelPath();

	public CLKernel getKernel();

	/**
	 * Compiles the underlying OpenCL kernel.
	 * 
	 * @return true, if successfully
	 */
	public boolean createKernel();

	public CLInstance getCLInstance();

	/**
	 * Returns required build options for compilation.
	 * 
	 * @return
	 */
	public List<String> getBuildOptions();

	/**
	 * Returns required defines for compilation.
	 * 
	 * @return
	 */
	public Map<String, Object> getDefines();

	/**
	 * Returns required includes for compilation.
	 * 
	 * @return
	 */
	public List<String> getIncludes();

	/**
	 * Returns additional source for compilation (dynamic code generation).
	 * 
	 * @return
	 */
	public List<String> getExtendedSource();

	/* public Buffer run(Object... args); */
}
