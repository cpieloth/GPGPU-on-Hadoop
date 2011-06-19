package math;

import com.amd.aparapi.Kernel;

public class VecAdd {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		final int LEN = 512;

		/*** Erzeugen der Vektoren ***/
		final int[] vecA = new int[LEN];
		final int[] vecB = new int[LEN];
		final int[] vecC = new int[LEN];

		fill(vecA);
		fill(vecB);

		Kernel kernel = new Kernel() {
			@Override
			public void run() {
				int gid = getGlobalId();
				vecC[gid] = vecA[gid] * vecB[gid];
			}
		};
		
		kernel.execute(LEN);
		
		System.out.println("Execution mode=" + kernel.getExecutionMode());

		for (int i = 0; i < LEN; i++) {
	         System.out.println(vecC[i] + ", ");
	      }

	      // Dispose Kernel resources.
	      kernel.dispose();
	}

	private static void fill(int[] vec) {
		for (int i = 0; i < vec.length; i++)
			vec[i] = i % 1000;
	}

}
