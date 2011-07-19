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
        localValues[LID] += localValues[LID + stride];

      barrier(CLK_LOCAL_MEM_FENCE);
    }
  while (stride > 1);

  if (LID == 0)
    values[get_group_id(0)] = localValues[0];

}
