__kernel void squareSum(__global float* distance, const __global float* p, const __global float* c, __local float* sums) {
        const  unsigned int GID = get_global_id(0);
        const unsigned int LID = get_local_id(0);
        const unsigned int LSIZE = get_local_size(0);

        sums[LID] = c[LID] - p[LID];
        sums[LID] = pow(sums[LID], 2);

        barrier(CLK_LOCAL_MEM_FENCE);

          if (get_local_id(0) == 0) {
            for(size_t i = 1; i< LSIZE; i++)
                sums[0] += sums[i];
            distance[0] = sums[0];
          }
}

__kernel void sum(__global float* sum, const __global float* p, __local float* sums) {
        const  unsigned int GID = get_global_id(0);
        const unsigned int LID = get_local_id(0);
        const unsigned int LSIZE = get_local_size(0);

          if (GID == 0) {
              sum[0] = 0;
            for(size_t i = 0; i< LSIZE; i++)
              sum[0] += p[i];
          }
}

