package cl_kernel;

import java.util.List;
import java.util.Map;

import cl_util.CLInstance;

import com.nativelibs4java.opencl.CLKernel;

public interface ICLKernel {
	
	public String getIdentifier();

	public String getKernelName();

	public String getKernelPath();

	public CLKernel getKernel();

	// public void setKernel(CLKernel kernek);

	public boolean createKernel();

	public CLInstance getCLInstance();

	// public void setContext(CLContext context);

	public List<String> getBuildOptions();

	public Map<String, Object> getDefines();

	public List<String> getIncludes();
	
	public List<String> getExtendedSource();

	/* public Buffer run(Object... args ); */
}
