package integration;

import lightLogger.Logger;
import mathFunction.FloatInterval;
import mathFunction.FloatPiFunction;
import mathFunction.FloatPolynomialFunction;
import mathFunction.FloatPowerFunction;
import mathFunction.IInterval;

import org.junit.Assert;
import org.junit.Test;

public class TrapeziumIntegrationCLTest {
	
	private static final Class<TrapeziumIntegrationCLTest> CLAZZ = TrapeziumIntegrationCLTest.class;

	private static final float DELTA = 0.001f;
	private static final int RESOLUTION = 100000;
	
	@Test
	public void testGetIntegral() {
		long tStart;
		
		tStart = System.currentTimeMillis();
		INumeriacalIntegration<Float> integration = new TrapeziumIntegrationCL();
		Logger.logInfo(CLAZZ, "Constructor (ms): " + (System.currentTimeMillis() - tStart));
		IInterval<Float> interval;
		float result, resultExp;
		
		
		int degree = 7;
		integration.setFunction(new FloatPolynomialFunction(degree));
		interval = new FloatInterval(1f, 2f, IInterval.DEFAULT_IDENTIFIER);
		
		tStart = System.currentTimeMillis();
		result = integration.getIntegral(interval, RESOLUTION);
		Logger.logInfo(CLAZZ, "Poly. (ms): " + (System.currentTimeMillis() - tStart));
		resultExp = new FloatPolynomialFunction(degree).getIntegral(interval);

		Assert.assertArrayEquals(new float[] { resultExp },
				new float[] { result }, DELTA);
		
		
		float exp = 3;
		integration.setFunction(new FloatPowerFunction(exp));
		interval = new FloatInterval(1f, 6f, IInterval.DEFAULT_IDENTIFIER);
		tStart = System.currentTimeMillis();
		result = integration.getIntegral(interval, RESOLUTION);
		Logger.logInfo(CLAZZ, "Power (ms): " + (System.currentTimeMillis() - tStart));
		resultExp = new FloatPowerFunction(exp).getIntegral(interval);

		Assert.assertArrayEquals(new float[] { resultExp },
				new float[] { result }, DELTA);

		
		integration.setFunction(new FloatPiFunction());
		interval = new FloatInterval(0f, 1f, IInterval.DEFAULT_IDENTIFIER);
		tStart = System.currentTimeMillis();
		result = integration.getIntegral(interval, RESOLUTION);
		Logger.logInfo(CLAZZ, "Pi (ms): " + (System.currentTimeMillis() - tStart));
		resultExp = (float) Math.PI;

		Assert.assertArrayEquals(new float[] { resultExp },
				new float[] { result }, DELTA);
	}

}
