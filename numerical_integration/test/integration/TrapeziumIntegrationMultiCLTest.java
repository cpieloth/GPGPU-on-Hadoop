package integration;

import java.util.ArrayList;
import java.util.List;

import lightLogger.Logger;
import mathFunction.FloatPolynomialFunction;
import mathFunction.IIntervalNamed;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import utils.NIData;
import cl_util.CLInstance;
import cl_util.CLInstance.TYPES;

public class TrapeziumIntegrationMultiCLTest {
	
	private static final Class<TrapeziumIntegrationMultiCLTest> CLAZZ = TrapeziumIntegrationMultiCLTest.class;

	private static final float DELTA = 0.001f;
	private static final int RESOLUTION = 1000;

	public TrapeziumIntegrationMultiCL integration;

	@Before
	public void setUp() throws Exception {
		integration = new TrapeziumIntegrationMultiCL(new CLInstance(
				TYPES.CL_GPU));
	}

	@Test
	public void testGetIntegral() {
		long tStart;
		int degree = 3;
		int intervalCount = 1000;
		List<IIntervalNamed<String, Float>> intervals = NIData
				.generateIntervals(-10f, 10f, intervalCount, "foo");
		List<Float> results = new ArrayList<Float>(intervalCount);
		List<Float> resultsExpected = new ArrayList<Float>(intervalCount);
		FloatPolynomialFunction func = new FloatPolynomialFunction(degree);
		integration.setFunction(func);

		// Computation on OpenCL device
		tStart = System.currentTimeMillis();
		for (IIntervalNamed<String, Float> interval : intervals) {
			if (!integration.put(interval)) {
				for (Float v : integration.getIntegrals(RESOLUTION))
					results.add(v);
				integration.put(interval);
			}
		}
		for (Float v : integration.getIntegrals(RESOLUTION))
			results.add(v);
		Logger.logInfo(CLAZZ, "Poly. (ms): " + (System.currentTimeMillis() - tStart));

		// Analytic solution
		for (IIntervalNamed<String, Float> interval : intervals)
			resultsExpected.add(func.getIntegral(interval));

		// Prepare to compare
		float[] res = new float[results.size()];
		float[] resEx = new float[resultsExpected.size()];
		for (int i = 0; i < res.length && i < results.size(); i++) {
			res[i] = results.get(i);
			resEx[i] = resultsExpected.get(i);
		}

		Assert.assertArrayEquals(resEx, res, DELTA);
	}
}
