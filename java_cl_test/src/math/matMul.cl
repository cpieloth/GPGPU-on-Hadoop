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
