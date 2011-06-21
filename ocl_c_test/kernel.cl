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

// unfinished
__kernel void maxInt(__global int* values, const unsigned int SIZE, const unsigned int CHK_SIZE,  __local int* localValues)
{
  int maxVal = -2147483648;
  //for (uint y = get_group_id(0); y < SIZE; y += get_num_groups(0))
  {
      localValues[get_local_id(0)] = values[get_local_id(0)];

      for (uint stride = ceil(get_local_size(0)/2); stride > 0; stride = ceil(stride/2))
      {
          barrier(CLK_LOCAL_MEM_FENCE);

          if (get_local_id(0) < stride && get_local_id(0)+stride < get_local_size(0))
            localValues[get_local_id(0)] = max(localValues[get_local_id(0)], localValues[get_local_id(0) + stride]);
      }

      if (get_local_id(0) == 0)
        values[get_group_id(0)] = localValues[0];

      barrier(CLK_LOCAL_MEM_FENCE);
  }


  barrier(CLK_GLOBAL_MEM_FENCE);

  if(get_global_id(0) == 0)
  {
          maxVal = values[0];
          for(uint i = 1; i < get_num_groups(0); i++)
          {
              maxVal = max(maxVal, values[i]);
          }
          values[0] = maxVal;
  }

}

