__kernel void addVec(__global int* vecC, const __global int* vecA, const __global int* vecB, const unsigned int size) {
	unsigned int w = get_global_id(0);
	if(w >= size)
		return;
	vecC[w] = vecA[w] + vecB[w];
}

__kernel void matMulSingle(__global int* matC, const unsigned int mC, const unsigned int nC,
		const __global int* matA, const unsigned int mA, const unsigned int nA,
		const __global int* matB, const unsigned int mB, const unsigned int nB) {
	unsigned int w = get_global_id(0);
	unsigned int c = w % nC;
	unsigned int r = (w - c) / nC;

	if(w >= mC * nC)
	  return;

	int tmp = 0;

	for (size_t j = 0; j < nA && j < mB; ++j)
    {
		tmp += matA[r * nA + j] *matB[j * nB + c];
	}

	matC[w] = tmp;
}

__kernel void maxInt(__global int* values, const int SIZE, const int CHK_SIZE)
{
	 __local int localTemp[64];
	for (uint y = get_group_id(0); y < SIZE; y += get_num_groups(0))
	{
		int temp = -273;
		for (uint x = get_local_id(0); x < CHK_SIZE; x += get_local_size(0))
			temp = max(temp, values[x]);

		localTemp[get_local_id(0)] = temp;

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

	barrier(CLK_GLOBAL_MEM_FENCE);

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

