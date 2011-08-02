float f(float x);

__kernel void integrationFloat(__global float* result, const float start,
		const float offset, const int res, __local float* localValues)
{
	const unsigned int LSIZE = get_local_size(0);
	const unsigned int LID = get_local_id(0);
	const unsigned int GID = get_global_id(0);
	const unsigned int GSIZE = get_global_size(0);
	const unsigned int N = convert_uint(res);

	const float h = offset / N;

	localValues[LID] = 0;

	for (unsigned int i = GID; i <= N; i += GSIZE)
	{
		if (i == 0 || i == N)
			localValues[LID] += h * (f(start + h * i) / 2);
		else
			localValues[LID] += h * f(start + h * i);
	}

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
