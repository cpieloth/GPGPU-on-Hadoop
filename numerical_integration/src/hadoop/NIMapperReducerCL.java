package hadoop;

import integration.IMathFunction;
import integration.INumeriacalIntegration;
import integration.TrapeziumIntegrationCL;

import java.io.IOException;
import java.net.InetAddress;

import lightLogger.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import stopwatch.StopWatch;
import utils.MathFunctions;

public class NIMapperReducerCL {

	public static class NIMapper
			extends
			Mapper<NullWritable, FloatIntervalWritable, NullWritable, FloatWritable> {

		private static final Class<NIMapper> CLAZZ = NIMapper.class;

		private int resolution;
		private INumeriacalIntegration<Float> integration;
		private IMathFunction<Float> function;

		private StopWatch swPhase = new StopWatch(
				NumericalIntegration.Timer.MAPPHASE.prefix,
				NumericalIntegration.Timer.MAPPHASE.suffix);
		private StopWatch swMethod = new StopWatch(
				NumericalIntegration.Timer.MAPMETHOD.prefix,
				NumericalIntegration.Timer.MAPMETHOD.suffix);

		@Override
		protected void setup(NIMapper.Context context) {
			swPhase.start();
			swMethod.start();
			swMethod.pause();

			Logger.logDebug(CLAZZ,
					"TaskAttemptID: " + context.getTaskAttemptID());
			try {
				Logger.logDebug(CLAZZ, "Hostname: "
						+ InetAddress.getLocalHost().getHostName());
			} catch (Exception e) {
				Logger.logDebug(CLAZZ, "Hostname: unknown");
			}

			integration = new TrapeziumIntegrationCL();
			Configuration conf = context.getConfiguration();
			function = MathFunctions.getFunction(conf.get(NumericalIntegration.Argument.FUNCTION.name), conf.get(NumericalIntegration.Argument.EXPONENT.name));
			Logger.logInfo(CLAZZ, "Function: " + conf.get(NumericalIntegration.Argument.FUNCTION.name));
			
			resolution = context.getConfiguration().getInt(NumericalIntegration.Argument.RESOLUTION.name, 0);
			Logger.logInfo(CLAZZ, "Resolution: " + resolution);
		}

		@Override
		protected void map(NullWritable key, FloatIntervalWritable value,
				NIMapper.Context context) throws IOException,
				InterruptedException {
			swMethod.resume();
			Logger.logDebug(CLAZZ, value.toString());

			integration.setFunction(function);
			Float result = integration.getIntegral(value, resolution);
			context.write(key, new FloatWritable(result));
			swMethod.pause();
		}

		@Override
		protected void cleanup(NIMapper.Context context) {
			swMethod.stop();
			Logger.log(NumericalIntegration.TIME_LEVEL, CLAZZ,
					swMethod.getTimeString());

			swPhase.stop();
			Logger.log(NumericalIntegration.TIME_LEVEL, CLAZZ,
					swPhase.getTimeString());
		}

	}

	public static class NIReducer extends
			Reducer<NullWritable, FloatWritable, NullWritable, FloatWritable> {

		private static final Class<NIReducer> CLAZZ = NIReducer.class;

		private StopWatch swPhase = new StopWatch(
				NumericalIntegration.Timer.REDUCEPHASE.prefix,
				NumericalIntegration.Timer.REDUCEPHASE.suffix);
		private StopWatch swMethod = new StopWatch(
				NumericalIntegration.Timer.REDUCEMETHOD.prefix,
				NumericalIntegration.Timer.REDUCEMETHOD.suffix);

		@Override
		protected void setup(NIReducer.Context context) {
			swPhase.start();
			swMethod.start();
			swMethod.pause();

			Logger.logDebug(CLAZZ,
					"TaskAttemptID: " + context.getTaskAttemptID());
			try {
				Logger.logDebug(CLAZZ, "Hostname: "
						+ InetAddress.getLocalHost().getHostName());
			} catch (Exception e) {
				Logger.logDebug(CLAZZ, "Hostname: unknown");
			}
		}

		@Override
		protected void reduce(NullWritable key, Iterable<FloatWritable> values,
				NIReducer.Context context) throws IOException,
				InterruptedException {
			swMethod.resume();

			float result = 0;
			for (FloatWritable value : values) {
				Logger.logDebug(CLAZZ, value.toString());
				result += value.get();
			}
			context.write(key, new FloatWritable(result));

			swMethod.pause();
		}

		@Override
		protected void cleanup(NIReducer.Context context) {
			swMethod.stop();
			Logger.log(NumericalIntegration.TIME_LEVEL, CLAZZ,
					swMethod.getTimeString());

			swPhase.stop();
			Logger.log(NumericalIntegration.TIME_LEVEL, CLAZZ,
					swPhase.getTimeString());
		}

	}
}
