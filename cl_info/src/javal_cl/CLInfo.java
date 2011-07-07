package javal_cl;

import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLException;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.JavaCL;

public class CLInfo {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			/*** Hole OpenCL-Plattformen z.B. AMD APP, NVIDIA CUDA ***/
			CLPlatform[] platforms = JavaCL.listPlatforms();
			System.out.println("Platforms:");
			for(CLPlatform pf : platforms) {
				System.out.println("\tName: " + pf.getName());
				System.out.println("\tVendor: " + pf.getVendor());
				System.out.println("\tVersion: " + pf.getVersion());
				System.out.println();
				System.out.println("\tDevices:");
				for(CLDevice dev : pf.listAllDevices(false)) {
					System.out.println("\t\tName: " + dev.getName());
					System.out.println("\t\tDriver: " + dev.getDriverVersion());
					System.out.println("\t\tOpenCL Version: " + dev.getOpenCLVersion());
					System.out.println("\t\tType: " + dev.getType());
					System.out.println("\t\tCompute Units: " + dev.getMaxComputeUnits());
					System.out.println("\t\tGlobal Memory Size: " + dev.getGlobalMemSize());
					System.out.println("\t\tWork Group Size: " + dev.getMaxWorkGroupSize());
					System.out.println("\t\tWork Item Size[Dim-0]: " + dev.getMaxWorkItemSizes()[0]);
					System.out.println("\t\tExtensions: " + getString(dev.getExtensions()));
					System.out.println();
				}
			}
			
		} catch (CLException err) {
			System.out.println("OpenCL error:\n" + err.getMessage()
					+ "():" + err.getCode());
			err.printStackTrace();
		} catch (Exception err) {
			System.out.println("Error:\n" + err.getMessage() + "()");
			err.printStackTrace();
		}

	}
	
	private static String getString(String[] values) {
		StringBuilder sb = new StringBuilder();
		for(String s : values)
			sb.append(s + " ");
		return sb.toString();
	}

}
