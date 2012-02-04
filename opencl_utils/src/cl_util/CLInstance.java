package cl_util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;

import lightLogger.Logger;
import cl_kernel.ICLKernel;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLException;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;

/**
 * Data structure for caching frequently used OpenCL objects like kernels,
 * command queues or devices.
 * 
 * @author Christof Pieloth
 * 
 */
public class CLInstance {
	private static final Class<CLInstance> CLAZZ = CLInstance.class;

	private CLContext context;
	private CLQueue cmdQ;
	private final boolean isInitialized;

	public static final int WAVE_SIZE = 64;

	private HashMap<String, ICLKernel> kernels = new HashMap<String, ICLKernel>();

	public static enum TYPES {
		CL_CPU, CL_GPU
	};

	public CLInstance(TYPES type) {
		isInitialized = this.initialize(type);
	}

	/**
	 * Checks if all data and devices are created.
	 * 
	 * @return true of all data and devices are ready.
	 */
	public boolean isInitialized() {
		return isInitialized;
	}

	private boolean initialize(TYPES type) {
		if (type == TYPES.CL_CPU)
			return this.initialize(CLDevice.Type.CPU);
		else if (type == TYPES.CL_GPU)
			return this.initialize(CLDevice.Type.GPU);
		else
			return false;
	}

	private boolean initialize(CLDevice.Type type) {
		try {
			// Init OpenCL
			CLPlatform[] platforms = JavaCL.listPlatforms();
			EnumSet<CLDevice.Type> types = EnumSet.of(type);
			ArrayList<CLDevice> devices = new ArrayList<CLDevice>();

			CLDevice[] devTmp;
			for (CLPlatform platform : platforms) {
				devTmp = platform.listDevices(types, true);
				devices.addAll(Arrays.asList(devTmp));
			}

			devTmp = new CLDevice[1];
			devTmp[0] = devices.get(0);
			context = JavaCL.createContext(null, devTmp);
			cmdQ = context.createDefaultQueue();

			Logger.logInfo(CLAZZ, "Selected device: " + devTmp[0].getName());
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

	/**
	 * Computes the minimum alloc size of all used devices.
	 * 
	 * @return maximum alloc size which can be used.
	 */
	public long getMaxMemAllocSize() {
		long size = Long.MAX_VALUE;
		long tmp;
		for (CLDevice dev : this.context.getDevices()) {
			tmp = dev.getMaxMemAllocSize();
			if (tmp < size)
				size = tmp;
		}
		return size;
	}

	public CLContext getContext() {
		return this.context;
	}

	public CLQueue getQueue() {
		return this.cmdQ;
	}

	/**
	 * Returns a stored kernel with the given identifier.
	 * 
	 * @param identifier
	 * @return compiled kernel
	 */
	public ICLKernel getKernel(String identifier) {
		return this.kernels.get(identifier);
	}

	/**
	 * Stores a kernel with the given identifier. Kernel should be compiled.
	 * 
	 * @param identifier
	 * @param kernel
	 */
	public void addKernel(String identifier, ICLKernel kernel) {
		this.kernels.put(identifier, kernel);
	}

	/**
	 * Calculates the optimal work group size for the first device in context.
	 * 
	 * @param globalSize work items
	 * @return work group size
	 */
	public int calcWorkGroupSize(int globalSize) {
		final long MAX_GROUP_SIZE = this.context.getDevices()[0]
				.getMaxWorkGroupSize();

		int localSize = (int) MAX_GROUP_SIZE;
		if (globalSize < localSize)
			localSize = globalSize;
		else
			while (globalSize % localSize != 0)
				--localSize;
		return localSize;
	}

}
