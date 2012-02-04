package cl_kernel;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import lightLogger.Logger;
import cl_util.CLInstance;

import com.nativelibs4java.opencl.CLException;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLProgram;

/**
 * Implements common used methods of ICLKernel.
 * 
 * @author Christof Pieloth
 * 
 */
public abstract class AbstractKernel implements ICLKernel {

	private static final Class<?> CLAZZ = AbstractKernel.class;

	protected final String KERNEL_NAME, KERNEL_PATH;

	protected CLKernel kernel;
	protected final CLInstance CL_INSTANCE;

	protected List<String> buildOptions = new LinkedList<String>();
	protected List<String> includes = new LinkedList<String>();
	protected List<String> extendedSource = new LinkedList<String>();

	protected Map<String, Object> defines = new HashMap<String, Object>();

	public AbstractKernel(CLInstance clInstance, String kernelName,
			String kernelPath) {
		this.CL_INSTANCE = clInstance;
		this.KERNEL_NAME = kernelName;
		this.KERNEL_PATH = kernelPath;
	}

	@Override
	public String getKernelName() {
		return this.KERNEL_NAME;
	}

	@Override
	public String getKernelPath() {
		return this.KERNEL_PATH;
	}

	@Override
	public boolean createKernel() {
		if (kernel != null)
			return true;

		StringBuffer sb = new StringBuffer();
		try {
			Scanner sc = new Scanner(CLAZZ.getResourceAsStream(KERNEL_PATH));
			while (sc.hasNext())
				sb.append(sc.nextLine());
			sc.close();
		} catch (Exception e) {
			Logger.logError(CLAZZ, "Could not read file: " + KERNEL_PATH);
			e.printStackTrace();
			return false;
		}

		try {
			CLProgram program = CL_INSTANCE.getContext().createProgram(
					sb.toString());

			for (String bo : buildOptions)
				program.addBuildOption(bo);
			for (String i : includes)
				program.addInclude(i);
			for (String s : extendedSource)
				program.addSource(s);
			program.defineMacros(defines);

			try {
				program.build();
			} catch (Exception err) {
				Logger.logError(CLAZZ,
						"Build log for \""
								+ CL_INSTANCE.getContext().getDevices()[0]
								+ "\n" + err.getMessage());
				err.printStackTrace();
				return false;
			}

			kernel = program.createKernel(KERNEL_NAME);
			CL_INSTANCE.addKernel(this.getIdentifier(), this);
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
	public CLInstance getCLInstance() {
		return this.CL_INSTANCE;
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

	@Override
	public List<String> getExtendedSource() {
		return this.extendedSource;
	}

}
