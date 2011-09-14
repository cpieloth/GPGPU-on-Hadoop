__kernel void distFloat(__global int* result, const __global float* points,
		const int PSIZE, const __global float* centroids, const int CSIZE,
		const int DIM)
{
	const unsigned int GID = get_global_id(0);

	__global float* point = &points[GID*DIM];

	if (GID >= PSIZE)
		return;

	float prevDist = 3.4e38;
	float dist;

	int iCentroid = -1;
	__global float* centroid;

	for (int i = 0; i < CSIZE; i++)
	{
		centroid = &centroids[i * DIM];
		dist = 0;
		for (int d = 0; d < DIM; d++)
		{
			dist += pow(centroid[d] - point[d], 2);
		}
		dist = sqrt(dist);
		if (dist < prevDist)
		{
			prevDist = dist;
			iCentroid = i;
		}
	}

	result[GID] = iCentroid;
}

__kernel void distFloatOld(__global float* result,
		const __global float* points, const int PSIZE,
		const __global float* centroids, const int CSIZE, const int DIM)
{
	const unsigned int GID = get_global_id(0);
	__global float* point = &points[GID * DIM];

	if (GID >= PSIZE)
		return;

	float prevDist = 3.4e38;
	float dist;

	__global float* centroid;
	__global float* tmpCentroid;

	for (int i = 0; i < CSIZE; i++)
	{
		tmpCentroid = &centroids[i * DIM];
		dist = 0;
		for (int d = 0; d < DIM; d++)
		{
			dist += pow(tmpCentroid[d] - point[d], 2);
		}
		dist = sqrt(dist);
		if (dist < prevDist)
		{
			prevDist = dist;
			centroid = tmpCentroid;
		}
	}

	for (int d = 0; d < DIM; d++)
		result[GID * DIM + d] = centroid[d];
}
