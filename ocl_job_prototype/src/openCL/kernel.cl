__kernel void addVec(__global int* vecC, const __global int* vecA, const __global int* vecB, const unsigned int size) {
	unsigned int w = get_global_id(0);
	if(w >= size)
		return; 
	vecC[w] = vecA[w] + vecB[w];
}
