float f(float x);

__kernel void integrationFloat(__global float* result, const float start,
		const float offset, const int n)
{
	const unsigned int GID = get_global_id(0);
	const float h = offset / n;

	float div = 1;

	if(GID == 0 || GID == n)
		div = 2;

	result[GID] = h * (f(start + h * GID) / div);
}
