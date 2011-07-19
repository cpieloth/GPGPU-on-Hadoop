__kernel void distFloat(__global float* result, const __global float* points, const int PSIZE, const __global float* centroids, const int CSIZE, const int DIM)
  {
    const unsigned int GID = get_global_id(0);
    __global float* point = &points[GID*DIM];

    if(GID >= PSIZE)
    return;

    float prevDist = 4242;
    float dist;

    __global float* centroid;
    __global float* tmpCentroid;

    for(int i = 0; i < CSIZE; i++)
      {
        tmpCentroid = &centroids[i*DIM];
        dist = 0;
        for(int d = 0; d < DIM; d++)
          {
            dist += pow(tmpCentroid[d] - point[d], 2);
          }
        dist = sqrt(dist);
        if(dist < prevDist)
          {
            prevDist = dist;
            centroid = tmpCentroid;
          }
      }

    for(int d = 0; d < DIM; d++)
    result[GID*DIM+d] = centroid[d];
  }

__kernel void sumFloat(__global float* values, __local float* localValues)
  {
    const unsigned int LSIZE = get_local_size(0);
    const unsigned int LID = get_local_id(0);
    const unsigned int GID = get_global_id(0);

    localValues[LID] = values[GID];

    barrier(CLK_LOCAL_MEM_FENCE);

    unsigned int stride = LSIZE;
    do
      {
        stride = convert_uint(ceil(convert_float(stride) / 2));

        if (LID < stride && (LID + stride) < LSIZE)
        localValues[LID] = localValues[LID] + localValues[LID + stride];

        barrier(CLK_LOCAL_MEM_FENCE);
      }
    while (stride > 1);

    if (get_local_id(0) == 0)
    values[get_group_id(0)] = localValues[0];

  }
