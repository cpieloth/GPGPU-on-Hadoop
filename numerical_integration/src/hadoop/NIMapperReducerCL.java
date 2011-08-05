package hadoop;

import integration.FloatPowerFunction;
import integration.IMathFunction;
import integration.INumeriacalIntegration;
import integration.TrapeziumIntegrationCL;

import java.io.IOException;

import lightLogger.Logger;

import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import stopwatch.StopWatch;

public class NIMapperReducerCL {

	public static class NIMapper
			extends
			Mapper<NullWritable, FloatIntervalWritable, NullWritable, FloatWritable> {

		private INumeriacalIntegration<Float> integration;
		private IMathFunction<Float> function;
		
		private StopWatch swPhase = new StopWatch(NumericalIntegration.PRE_MAPPHASE, NumericalIntegration.SUFFIX);
		private StopWatch swMethod = new StopWatch(NumericalIntegration.PRE_MAPMETHOD, NumericalIntegration.SUFFIX);
		
		@Override
		protected void setup(NIMapper.Context context) {
			swPhase.start();
			swMethod.start();
			swMethod.pause();
			
			integration = new TrapeziumIntegrationCL();
			function = new FloatPowerFunction(3f);
		}
		
		@Override
		protected void map(NullWritable key, FloatIntervalWritable value,
				NIMapper.Context context) throws IOException,
				InterruptedException {
			swMethod.resume();
			Logger.logDebug(NIMapper.class, value.toString());
			
			integration.setFunction(function);
			Float result = integration.getIntegral(value);
			context.write(key, new FloatWritable(result));
			swMethod.pause();
		}
		
		@Override
		protected void cleanup(NIMapper.Context context) {
			swMethod.stop();
			Logger.log(NumericalIntegration.TIME_LEVEL, NIMapper.class, swMethod.getTimeString());
			
			swPhase.stop();
			Logger.log(NumericalIntegration.TIME_LEVEL, NIMapper.class, swPhase.getTimeString());
		}

	}

	public static class NIReducer
			extends
			Reducer<NullWritable, FloatWritable, NullWritable, FloatWritable> {
		
		private StopWatch swPhase = new StopWatch(NumericalIntegration.PRE_REDUCEPHASE, NumericalIntegration.SUFFIX);
		private StopWatch swMethod = new StopWatch(NumericalIntegration.PRE_REDUCEMETHOD, NumericalIntegration.SUFFIX);
		
		@Override
		protected void setup(NIReducer.Context context) {
			swPhase.start();
			swMethod.start();
			swMethod.pause();
		}

		@Override
		protected void reduce(NullWritable key,
				Iterable<FloatWritable> values,
				NIReducer.Context context) throws IOException,
				InterruptedException {
			swMethod.resume();
			
			float result = 0;
			for (FloatWritable value : values) {
				Logger.logDebug(NIReducer.class, value.toString());
				result += value.get();
			}
			context.write(key, new FloatWritable(result));
			
			swMethod.pause();
		}
		
		@Override
		protected void cleanup(NIReducer.Context context) {
			swMethod.stop();
			Logger.log(NumericalIntegration.TIME_LEVEL, NIReducer.class, swMethod.getTimeString());
			
			swPhase.stop();
			Logger.log(NumericalIntegration.TIME_LEVEL, NIReducer.class, swPhase.getTimeString());
		}

	}
}
