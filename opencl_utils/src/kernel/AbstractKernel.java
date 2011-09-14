package kernel;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import lightLogger.Logger;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLException;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLProgram;

public abstract class AbstractKernel implements ICLKernel {

	private static final Class<?> CLAZZ = AbstractKernel.class;

	protected String kernelName, kernelPath;

	protected CLKernel kernel;
	protected CLContext context;

	protected List<String> buildOptions = new LinkedList<String>();
	protected List<String> includes = new LinkedList<String>();

	protected Map<String, Object> defines = new HashMap<String, Object>();

	public AbstractKernel(CLContext context, String kernelName, String kernelPath) {
		this.context = context;
		this.kernelName = kernelName;
		this.kernelPath = kernelPath;
	}

	@Override
	public String getKernelName() {
		return this.kernelName;
	}

	@Override
	public String getKernelPath() {
		return this.kernelPath;
	}

	@Override
	public boolean createKernel() {
		StringBuffer sb = new StringBuffer();
		try {
			Scanner sc = new Scanner(CLAZZ.getResourceAsStream(kernelPath));
			while (sc.hasNext())
				sb.append(sc.nextLine());
			sc.close();
		} catch (Exception e) {
			Logger.logError(CLAZZ, "Could not read file: " + kernelPath);
			e.printStackTrace();
			return false;
		}

		try {
			CLProgram program = context.createProgram(sb.toString());

			for (String bo : buildOptions)
				program.addBuildOption(bo);
			for (String i : includes)
				program.addInclude(i);
			program.defineMacros(defines);

			try {
				program.build();
			} catch (Exception err) {
				Logger.logError(CLAZZ,
						"Build log for \"" + context.getDevices()[0] + "\n"
								+ err.getMessage());
				err.printStackTrace();
				return false;
			}

			kernel = program.createKernel(kernelName);
			// this.kernels.put(prefix + kernelName, kernel);

			return true;
		} catch (CLException err) {
			Logger.logError(CLAZZ, "OpenCL error:\n" + err.getMessage() + "():"
					+ err.getCode());
			err.printStackTrace();
			return false;
		} catch (Exception err) {
			Logger.logError(CLAZZ, "Error:\n" + err.getMessage() + "()");
			err.printStackTrace();
			return false;
		}
	}

	@Override
	public CLKernel getKernel() {
		return this.kernel;
	}

	@Override
	public void setKernel(CLKernel kernel) {
		this.kernel = kernel;
	}

	@Override
	public CLContext getContext() {
		return this.context;
	}

	@Override
	public void setContext(CLContext context) {
		this.context = context;
	}

	@Override
	public List<String> getBuildOptions() {
		return this.buildOptions;
	}

	@Override
	public Map<String, Object> getDefines() {
		return this.defines;
	}

	@Override
	public List<String> getIncludes() {
		return this.includes;
	}

}
