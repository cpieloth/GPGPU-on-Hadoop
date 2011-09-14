package kernel;

import java.util.List;
import java.util.Map;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLKernel;

public interface ICLKernel {

	public String getKernelName();

	public String getKernelPath();

	public CLKernel getKernel();

	public void setKernel(CLKernel kernek);

	public boolean createKernel();

	public CLContext getContext();

	public void setContext(CLContext context);

	public List<String> getBuildOptions();

	public Map<String, Object> getDefines();

	public List<String> getIncludes();

	/* public Buffer run(Object... args ); */
}
