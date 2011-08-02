float f(float x);

__kernel void integrationFloat(__global float* result, const float start,
		const float offset, const int n, __local float* localValues)
{
	const unsigned int LSIZE = get_local_size(0);
	const unsigned int LID = get_local_id(0);
	const unsigned int GID = get_global_id(0);

	const float h = offset / n;

	float div = 1;

	if (GID == 0 || GID == n)
		div = 2;

	if (GID <= n)
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
