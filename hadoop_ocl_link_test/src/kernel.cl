/* avoiding wrong highlighting in IDEs, delete it before compiling */
#define __kernel
#define __global
#define __local

__kernel void addVec(__global int* vecC, const __global int* vecA, const __global int* vecB, const unsigned int size)
{
	unsigned int w = get_global_id(0);
	if(w >= size)
		return; 
	vecC[w] = vecA[w] + vecB[w];
}

#define MIN_TEMP -512

/* __local int* localTemp -> temp of each work group kernel.setArg(..., sizeof(cl_int) * WORK_GROUP_SIZE, NULL); */
__kernel void max(__global int* values, const int SIZE, const int CHK_SIZE, __local int* localTemp)
{
	// Each work-group computes multiple elements of W
	for (uint y = get_group_id(0); y < SIZE; y += get_num_groups(0))
	{
		int temp = MIN_TEMP;
		for (uint x = get_local_id(0); x < CHK_SIZE; x += get_local_size(0))
			temp = max(temp, values[x]);

		localTemp[get_local_id(0)] = temp;

		// work-items are gathering and calculating the dotProduct
		for (uint stride = get_local_size(0)/2; stride > 0; stride /= 2)
		{
			barrier(CLK_LOCAL_MEM_FENCE);

			if (get_local_id(0) < stride)
				localTemp[get_local_id(0)] = max(localTemp[get_local_id(0)], localTemp[get_local_id(0) + stride]);
		}

		if (get_local_id(0) == 0)
			values[get_group_id(0)] = localTemp[0];

		barrier(CLK_LOCAL_MEM_FENCE);
	}

	// TODO put in parallel reduction
	int maxTemp = values[0];
	if(get_global_id(0) == 0)
	{
		for(uint i = 1; i < get_num_groups(0); i++)
		{
			maxTemp = max(maxTemp, values[i]);
		}
		values[0] = maxTemp;
	}
}
