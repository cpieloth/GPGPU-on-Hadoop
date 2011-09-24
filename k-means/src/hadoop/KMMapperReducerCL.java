package hadoop;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import lightLogger.Logger;

import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import stopwatch.StopWatch;
import utils.Points;
import cl_util.CLInstance;
import cl_util.CLPointFloat;
import clustering.CPoint;
import clustering.ICPoint;
import clustering.IPoint;

public class KMMapperReducerCL {

	public static class KMMapper extends
			Mapper<NullWritable, PointWritable, PointWritable, PointWritable> {

		private static final Class<KMMapper> CLAZZ = KMMapper.class;

		private List<PointWritable> centroids;
		private CLPointFloat clPoint;

		private StopWatch swPhase = new StopWatch(
				KMeansHadoop.Timer.MAPPHASE.prefix,
				KMeansHadoop.Timer.MAPPHASE.suffix);
		private StopWatch swMethod = new StopWatch(
				KMeansHadoop.Timer.MAPMETHOD.prefix,
				KMeansHadoop.Timer.MAPMETHOD.suffix);

		@Override
		protected void setup(KMMapper.Context context) {
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

			// TODO read max k from conf to use ArrayList
			this.centroids = new LinkedList<PointWritable>();

			Scanner sc = null;
			try {
				URI[] uris = DistributedCache.getCacheFiles(context
						.getConfiguration());
				FileSystem fs = FileSystem.get(context.getConfiguration());
				for (FileStatus fst : fs
						.listStatus(new Path(uris[0].toString()))) {
					if (!fst.isDir()) {
						Logger.logDebug(CLAZZ, "centroids: " + fst.getPath());
						sc = new Scanner(fs.open(fst.getPath()));
						while (sc.hasNext())
							this.centroids.add(new PointWritable(Points
									.createPoint(sc.next())));
						sc.close();
					}
				}
			} catch (IOException e) {
				Logger.logError(CLAZZ, "Could not get local cache files");
				e.printStackTrace();
			} finally {
				if (sc != null)
					sc.close();
				// don't close FileSystem!
			}

			this.clPoint = new CLPointFloat(new CLInstance(
					CLInstance.TYPES.CL_GPU), this.centroids.get(0).getDim());
			List<IPoint<Float>> tmpCentroids = new ArrayList<IPoint<Float>>(
					this.centroids.size());
			tmpCentroids.addAll(this.centroids);
			Logger.logDebug(CLAZZ, "Centroid size: " + tmpCentroids.size());
			this.clPoint.prepareNearestPoints(tmpCentroids);
			this.clPoint.reset(1);
		}

		@Override
		protected void map(NullWritable key, PointWritable value,
				KMMapper.Context context) throws IOException,
				InterruptedException {
			swMethod.resume();

			ICPoint<Float> cp = new CPoint(value);

			this.clPoint.put(cp);
			this.clPoint.setNearestPoints();

			PointWritable centroid = new PointWritable(cp.getCentroid());

			context.write(centroid, value);

			swMethod.pause();
		}

		@Override
		protected void cleanup(KMMapper.Context context) {
			swMethod.stop();
			Logger.log(KMeansHadoop.TIME_LEVEL, CLAZZ, swMethod.getTimeString());

			swPhase.stop();
			Logger.log(KMeansHadoop.TIME_LEVEL, CLAZZ, swPhase.getTimeString());
		}

	}

	// Old reducer with OCL
	// public static class KMReducer extends
	// Reducer<PointWritable, PointWritable, PointWritable, PointWritable> {
	//
	// private static final Class<KMReducer> CLAZZ = KMReducer.class;
	//
	// private StopWatch swPhase = new StopWatch(
	// KMeansHadoop.Timer.REDUCEPHASE.prefix,
	// KMeansHadoop.Timer.REDUCEPHASE.suffix);
	// private StopWatch swMethod = new StopWatch(
	// KMeansHadoop.Timer.REDUCEMETHOD.prefix,
	// KMeansHadoop.Timer.REDUCEMETHOD.suffix);
	//
	// private ICLSummarizer<Float>[] clFloat;
	// private CLInstance clInstance;
	//
	// @Override
	// protected void setup(KMReducer.Context context) {
	// swPhase.start();
	// swMethod.start();
	// swMethod.pause();
	//
	// Logger.logDebug(CLAZZ,
	// "TaskAttemptID: " + context.getTaskAttemptID());
	// try {
	// Logger.logDebug(CLAZZ, "Hostname: "
	// + InetAddress.getLocalHost().getHostName());
	// } catch (Exception e) {
	// Logger.logDebug(CLAZZ, "Hostname: unknown");
	// }
	//
	// this.clInstance = new CLInstance(CLInstance.TYPES.CL_GPU);
	// }
	//
	// @Override
	// protected void reduce(PointWritable key,
	// Iterable<PointWritable> values, Context context)
	// throws IOException, InterruptedException {
	// swMethod.resume();
	//
	// int DIM = key.getDim();
	//
	// // Each instance of CLFloat is for one sum only.
	// // Hadoop-Iterable can only be use once!
	// this.clFloat = new CLSummarizerFloat[DIM];
	// for (int d = 0; d < DIM; d++)
	// this.clFloat[d] = new CLSummarizerFloat(this.clInstance);
	//
	// int count = 0;
	// for (IPoint<Float> p : values) {
	// for (int d = 0; d < DIM; d++)
	// this.clFloat[d].put(p.get(d));
	// count++;
	// }
	//
	// PointWritable centroid = new PointWritable(DIM);
	// for (int d = 0; d < DIM; d++)
	// centroid.set(d, this.clFloat[d].getSum() / count);
	//
	// context.write(centroid, null);
	//
	// swMethod.pause();
	// }
	//
	// @Override
	// protected void cleanup(KMReducer.Context context) {
	// swMethod.stop();
	// Logger.log(KMeansHadoop.TIME_LEVEL, CLAZZ, swMethod.getTimeString());
	//
	// swPhase.stop();
	// Logger.log(KMeansHadoop.TIME_LEVEL, CLAZZ, swPhase.getTimeString());
	// }
	//
	// }

	public static class KMReducer extends
			Reducer<PointWritable, PointWritable, PointWritable, PointWritable> {

		private static final Class<KMReducer> CLAZZ = KMReducer.class;

		private StopWatch swPhase = new StopWatch(
				KMeansHadoop.Timer.REDUCEPHASE.prefix,
				KMeansHadoop.Timer.REDUCEPHASE.suffix);
		private StopWatch swMethod = new StopWatch(
				KMeansHadoop.Timer.REDUCEMETHOD.prefix,
				KMeansHadoop.Timer.REDUCEMETHOD.suffix);

		@Override
		protected void setup(KMReducer.Context context) {
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
		protected void reduce(PointWritable key,
				Iterable<PointWritable> values, KMReducer.Context context)
				throws IOException, InterruptedException {
			swMethod.resume();

			int DIM = key.getDim();

			float[] dimension = new float[DIM];
			for (int d = 0; d < DIM; d++)
				dimension[d] = 0;

			int count = 0;
			for (PointWritable point : values) {
				for (int d = 0; d < DIM; d++)
					dimension[d] += point.get(d);
				count++;
			}

			PointWritable centroid = new PointWritable(DIM);
			for (int d = 0; d < DIM; d++)
				centroid.set(d, dimension[d] / count);

			context.write(centroid, null);

			swMethod.pause();
		}

		@Override
		protected void cleanup(KMReducer.Context context) {
			swMethod.stop();
			Logger.log(KMeansHadoop.TIME_LEVEL, CLAZZ, swMethod.getTimeString());

			swPhase.stop();
			Logger.log(KMeansHadoop.TIME_LEVEL, CLAZZ, swPhase.getTimeString());
		}

	}

}
