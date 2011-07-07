#define TEST_ON TEST
#ifdef TEST_ON

#include <iostream>
#include <vector>

#define __CL_ENABLE_EXCEPTIONS
#include <CL/cl.hpp>

int main(int argc, char *argv[]) {
	try {
		std::vector<cl::Platform> platforms;
		cl::Platform::get(&platforms);

		std::cout << "Found " << platforms.size() << " OpenCL Platforms"
				<< std::endl << std::endl;

		for (std::vector<cl::Platform>::const_iterator pltfmIt =
				platforms.begin(); pltfmIt != platforms.end(); ++pltfmIt) {

			std::cout << "OpenCL platform: " << pltfmIt->getInfo<
					CL_PLATFORM_NAME> () << std::endl;

			std::cout << "OpenCL platform version: " << pltfmIt->getInfo<
								CL_PLATFORM_VERSION> () << std::endl;

			std::cout << std::endl;

			std::vector<cl::Device> devices;
			pltfmIt->getDevices(CL_DEVICE_TYPE_ALL, &devices);

			for (std::vector<cl::Device>::const_iterator devIt =
					devices.begin(); devIt != devices.end(); ++devIt) {

				std::cout << "  OpenCL device: " << devIt->getInfo<
						CL_DEVICE_NAME> () << std::endl;

				std::cout << "    type: ";

				switch (devIt->getInfo<CL_DEVICE_TYPE> ()) {
				case CL_DEVICE_TYPE_ACCELERATOR:
					std::cout << "ACCELERATOR" << std::endl;
					break;

				case CL_DEVICE_TYPE_CPU:
					std::cout << "CPU" << std::endl;
					break;

				case CL_DEVICE_TYPE_GPU:
					std::cout << "GPU" << std::endl;
					break;

				default:
					std::cout << "DEFAULT" << std::endl;
				}

				std::cout << "\tmax compute units: " << devIt->getInfo<
						CL_DEVICE_MAX_COMPUTE_UNITS> () << std::endl;

				cl_uint dims = devIt->getInfo<
						CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS> ();
				std::cout << "\tmax work item dims: " << dims << std::endl;

				std::cout << "\tmax work item sizes: " << std::endl;
				for (cl_uint i = 0; i < dims; ++i) {
					std::cout << "\t\tDIM-" << i + 1 << ": " << devIt->getInfo<
							CL_DEVICE_MAX_WORK_ITEM_SIZES> ()[i] << std::endl;
				}
				std::cout << std::endl;

				std::cout << "\tmax work group sizes: " << devIt->getInfo<
						CL_DEVICE_MAX_WORK_GROUP_SIZE> () << std::endl;

				std::cout << "\tmax global mem size (in mb): "
						<< devIt->getInfo<CL_DEVICE_GLOBAL_MEM_SIZE> ()
								/ 1048576 << std::endl;

				std::cout << "\tmax local mem size (in kb): "
						<< devIt->getInfo<CL_DEVICE_LOCAL_MEM_SIZE> () / 1024
						<< std::endl;

				std::cout << "\textensions: "
										<< devIt->getInfo<CL_DEVICE_EXTENSIONS>() << std::endl;

				std::cout << std::endl;
			}
		}
	} catch (cl::Error& err) {
		std::cerr << "OpenCL error: " << err.what() << "(" << err.err() << ")"
				<< std::endl;

		return EXIT_FAILURE;
	}

	return 0;
}

#endif /* TEST_ON */
