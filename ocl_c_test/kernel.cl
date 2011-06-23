__kernel void
maxInt(__global int* values, __local int* localValues)
{
  const unsigned int LSIZE = get_local_size(0);
  const unsigned int LID = get_local_id(0);

  localValues[LID] = values[get_global_id(0)];

  barrier(CLK_LOCAL_MEM_FENCE);

  unsigned int stride = LSIZE;
  do
    {
      stride = convert_uint(ceil(convert_float(stride) / 2));

      if (LID < stride && (LID + stride) < LSIZE)
        localValues[LID] = max(localValues[LID], localValues[LID + stride]);

      barrier(CLK_LOCAL_MEM_FENCE);
    }
  while (stride > 1);

  if (get_local_id(0) == 0)
    values[get_group_id(0)] = localValues[0];

}
