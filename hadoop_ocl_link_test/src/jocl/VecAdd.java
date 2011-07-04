package jocl;

import static org.jocl.CL.CL_CONTEXT_DEVICES;
import static org.jocl.CL.CL_CONTEXT_PLATFORM;
import static org.jocl.CL.CL_DEVICE_TYPE_CPU;
import static org.jocl.CL.CL_DEVICE_TYPE_GPU;
import static org.jocl.CL.CL_MEM_COPY_HOST_PTR;
import static org.jocl.CL.CL_MEM_READ_ONLY;
import static org.jocl.CL.CL_MEM_WRITE_ONLY;
import static org.jocl.CL.CL_QUEUE_PROFILING_ENABLE;
import static org.jocl.CL.CL_TRUE;
import static org.jocl.CL.clBuildProgram;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clCreateCommandQueue;
import static org.jocl.CL.clCreateContextFromType;
import static org.jocl.CL.clCreateKernel;
import static org.jocl.CL.clCreateProgramWithSource;
import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clEnqueueReadBuffer;
import static org.jocl.CL.clGetContextInfo;
import static org.jocl.CL.clGetPlatformIDs;
import static org.jocl.CL.clReleaseCommandQueue;
import static org.jocl.CL.clReleaseContext;
import static org.jocl.CL.clReleaseEvent;
import static org.jocl.CL.clReleaseKernel;
import static org.jocl.CL.clReleaseMemObject;
import static org.jocl.CL.clReleaseProgram;
import static org.jocl.CL.clSetKernelArg;
import static org.jocl.CL.clWaitForEvents;

import java.io.File;
import java.util.Scanner;

import lightLogger.Logger;

import org.jocl.CL;
import org.jocl.CLException;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_event;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;

public class VecAdd {

	/*** Kommandozeilenargumente ***/
	private static final char CPU = 'c';
	private static final char GPU = 'g';

	/*** Globale Variablen ***/
	private static int LEN;
	private static char TYPE;
	private static final int EXIT_FAILURE = 1;
	private static final int EXIT_SUCCESS = 0;

	private static final String KERNEL_PATH = "kernel.cl";

	private static int addVec(long clType, int[] vecC, int[] vecA, int[] vecB) {
		CL.setExceptionsEnabled(true);
		try {
			/*** Hole OpenCL-Plattformen z.B. AMD APP, NVIDIA CUDA ***/
			cl_platform_id[] platforms = new cl_platform_id[3];
			clGetPlatformIDs(platforms.length, platforms, null);

			/*** Erstelle Context mit dem gewuenschten Type z.B. CPU, GPU ***/
			cl_context_properties contextProperties = new cl_context_properties();
			for (cl_platform_id pId : platforms) {
				if (pId != null)
					contextProperties.addProperty(CL_CONTEXT_PLATFORM, pId);
				else
					break;
			}

			cl_context context = clCreateContextFromType(contextProperties,
					clType, null, null, null);
			if (context == null)
				throw new Exception("Could not create context");

			/*** OpenCL-Quellcode einlesen ***/
			String src = readFile(KERNEL_PATH);

			/*** OpenCL-Programm aus Quellcode erstellen ***/
			cl_program program = clCreateProgramWithSource(context, 1,
					new String[] { src }, null, null);
			clBuildProgram(program, 0, null, null, null, null);

			/*** Erstelle CommandQueue und Kernel ***/
			long numBytes[] = new long[1];
			clGetContextInfo(context, CL_CONTEXT_DEVICES, 0, null, numBytes);
			int numDevices = (int) numBytes[0] / Sizeof.cl_device_id;
			cl_device_id devices[] = new cl_device_id[numDevices];
			clGetContextInfo(context, CL_CONTEXT_DEVICES, numBytes[0],
					Pointer.to(devices), null);

			cl_command_queue cmdQ[] = new cl_command_queue[numDevices];
			cl_kernel kernels[] = new cl_kernel[numDevices];

			long properties = 0;
			properties |= CL_QUEUE_PROFILING_ENABLE;

			for (int i = 0; i < numDevices; i++) {
				cmdQ[i] = clCreateCommandQueue(context, devices[i], properties,
						null);
				kernels[i] = clCreateKernel(program, "addVec", null);
			}

			/*** Erstellen und Vorbereiten der Daten ***/
			Pointer aPointer = Pointer.to(vecA);
			Pointer bPointer = Pointer.to(vecB);
			Pointer cPointer = Pointer.to(vecC);

			cl_mem aBuffer = clCreateBuffer(context, CL_MEM_READ_ONLY
					| CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * vecA.length,
					aPointer, null);
			cl_mem bBuffer = clCreateBuffer(context, CL_MEM_READ_ONLY
					| CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * vecB.length,
					bPointer, null);
			cl_mem cBuffer = clCreateBuffer(context, CL_MEM_WRITE_ONLY,
					Sizeof.cl_int * vecC.length, null, null);

			/*** Kernel-Argumente setzen ***/
			for (cl_kernel kernel : kernels) {
				clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(cBuffer));
				clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(aBuffer));
				clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(bBuffer));
				clSetKernelArg(kernel, 3, Sizeof.cl_int,
						Pointer.to(new int[] { vecC.length }));
			}

			/*** Kernel ausfuehren und auf Abarbeitung warten ***/
			cl_event events[] = new cl_event[numDevices];
			for (int i = 0; i < numDevices; i++) {
				events[i] = new cl_event();
				clEnqueueNDRangeKernel(cmdQ[i], kernels[i], 1, null,
						new long[] { vecC.length }, new long[] { 1 }, 0, null,
						events[i]);
			}
			clWaitForEvents(events.length, events);

			/*** Daten vom OpenCL-Device holen ***/
			// FIXME if more than 1 device, vecC will be overwritten
			for (cl_command_queue cq : cmdQ) {
				clEnqueueReadBuffer(cq, cBuffer, CL_TRUE, 0, Sizeof.cl_int
						* vecC.length, cPointer, 0, null, null);
			}

			/*** Daten freigeben ***/
			for (int i = 0; i < numDevices; i++) {
				clReleaseKernel(kernels[i]);
				clReleaseCommandQueue(cmdQ[i]);
				clReleaseEvent(events[i]);
			}
			clReleaseProgram(program);
			clReleaseContext(context);

			clReleaseMemObject(aBuffer);
			clReleaseMemObject(bBuffer);
			clReleaseMemObject(cBuffer);

		} catch (CLException err) {
			Logger.logError(VecAdd.class, "OpenCL error:\n" + err.getMessage()
					+ "():" + err.getCause());
			err.printStackTrace();
			return EXIT_FAILURE;
		} catch (Exception err) {
			Logger.logError(VecAdd.class, "Error:\n" + err.getMessage() + "()");
			err.printStackTrace();
			return EXIT_FAILURE;
		}

		return EXIT_SUCCESS;
	}

	public static void main(String[] args) {
		if (!checkArguments(args))
			System.exit(EXIT_FAILURE);

		int success = EXIT_FAILURE;

		Logger.logInfo(VecAdd.class, "Device type: " + TYPE);
		Logger.logInfo(VecAdd.class, "Vector size: " + LEN);

		/*** Erzeugen der Vektoren ***/
		int[] vecA = new int[LEN];
		int[] vecB = new int[LEN];
		int[] vecC = new int[LEN];

		fill(vecA);
		fill(vecB);

		/*** Implementierung waehlen ***/
		switch (TYPE) {
		case GPU:
			success = addVec(CL_DEVICE_TYPE_GPU, vecC, vecA, vecB);
			break;
		case CPU:
			success = addVec(CL_DEVICE_TYPE_CPU, vecC, vecA, vecB);
			break;
		default:
			Logger.logError(VecAdd.class, "Device type not supported!");
		}

		if (success == EXIT_SUCCESS) {
			if (LEN < 80) {
				StringBuilder sb = new StringBuilder();
				sb.append("\n<");
				for (int i = 0; i < vecC.length; ++i)
					sb.append(vecC[i] + ",");
				sb.setCharAt(sb.length() - 1, '>');
				Logger.logInfo(VecAdd.class, sb.toString());
			}
			System.exit(EXIT_SUCCESS);
		} else {
			Logger.logError(VecAdd.class, "Error, no result!");
			System.exit(EXIT_FAILURE);
		}
	}

	private static boolean checkArguments(String[] args) {
		if (args.length < 2) {
			System.out.println("Argumente: " + CPU + "|" + GPU
					+ " <Vektorgroesze>");
			return false;
		}

		TYPE = args[0].charAt(0);
		LEN = Integer.valueOf(args[1]).intValue();

		return true;
	}

	private static String readFile(String fName) {
		StringBuffer sb = new StringBuffer();
		try {
			Scanner sc = new Scanner(new File(fName));
			while (sc.hasNext())
				sb.append(sc.nextLine());

		} catch (Exception e) {
			Logger.logError(VecAdd.class, "Could not read file: " + fName);
			e.printStackTrace();
		}
		return sb.toString();
	}

	private static void fill(int[] vec) {
		for (int i = 0; i < vec.length; i++)
			vec[i] = i % 1000;
	}

}
