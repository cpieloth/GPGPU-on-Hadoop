float f(float x);

__kernel void integrationFloat(__global float* result, const float start,
		const float offset, const int n, __local float* localValues)
{
	const unsigned int LSIZE = get_local_size(0);
	const unsigned int LID = get_local_id(0);
	const unsigned int GID = get_global_id(0);
	const unsigned int N = convert_uint(n);

	const float h = offset / n;
	float div = 1;

	if (GID == 0 || GID == N)
		div = 2;

	if(GID <= N)
		localValues[LID] = h * (f(start + h * GID) / div);
	else
		localValues[LID] = 0;

	barrier(CLK_LOCAL_MEM_FENCE);

	unsigned int stride = LSIZE;
	do
	{
		stride = convert_uint(ceil(convert_float(stride) / 2));

		if (LID < stride && (LID + stride) < LSIZE)
			localValues[LID] += localValues[LID + stride];

		barrier(CLK_LOCAL_MEM_FENCE);
	} while (stride > 1);

	if (LID == 0)
		result[get_group_id(0)] = localValues[0];
}

__kernel void integrationMultiFloat(__global float* result, __global const float* begin, __global const float* end,
		const int size, const int n, __local float* localValues)
{
	const unsigned int LSIZE = get_local_size(0);
	const unsigned int LCOUNT = get_num_groups(0);
	const unsigned int LID = get_local_id(0);
	const unsigned int GID = get_global_id(0);
	const unsigned int N = convert_uint(n);

	float h;
	float offset;
	float div;
	unsigned int stride;

	for(int i = 0; i < size; i++)
	{
		offset = end[i] - begin[i];
		h = offset / n;
		div = 1;

		if (GID == 0 || GID == N)
			div = 2;

		if(GID <= N)
			localValues[LID] = h * (f(begin[i] + h * GID) / div);
		else
			localValues[LID] = 0;

		barrier(CLK_LOCAL_MEM_FENCE);

		stride = LSIZE;
		do
		{
			stride = convert_uint(ceil(convert_float(stride) / 2));

			if (LID < stride && (LID + stride) < LSIZE)
				localValues[LID] += localValues[LID + stride];

			barrier(CLK_LOCAL_MEM_FENCE);
		} while (stride > 1);

		if (LID == 0)
			result[get_group_id(0) + LCOUNT * i] = localValues[0];
		barrier(CLK_LOCAL_MEM_FENCE);
	}
}
