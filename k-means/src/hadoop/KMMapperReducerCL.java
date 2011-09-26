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
import clustering.IPoint;

public class KMMapperReducerCL {

	public static class KMMapper extends
			Mapper<NullWritable, PointWritable, PointWritable, PointWritable> {

		private static final Class<KMMapper> CLAZZ = KMMapper.class;

		private CLPointFloat clPoint;
		private List<PointWritable> pointBuffer;
		private int MAX_ITEMS;

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

			List<PointWritable> centroids = new LinkedList<PointWritable>();
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
							centroids.add(new PointWritable(Points
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
					CLInstance.TYPES.CL_GPU), centroids.get(0).getDim());
			// copy to ArrayList due to constant time for get()
			this.clPoint.prepareNearestPoints(new ArrayList<IPoint<Float>>(
					centroids));
			Logger.logDebug(CLAZZ, "Centroid size: " + centroids.size());

			this.MAX_ITEMS = this.clPoint.getBufferSize() * 8;
			this.MAX_ITEMS = this.MAX_ITEMS < this.clPoint.getMaxItemSize() ? this.MAX_ITEMS
					: this.clPoint.getMaxItemSize();
			this.pointBuffer = new ArrayList<PointWritable>(this.MAX_ITEMS);
			Logger.logDebug(CLAZZ, "MAX_ITEMS: " + this.MAX_ITEMS);
		}

		@Override
		protected void map(NullWritable key, PointWritable value,
				KMMapper.Context context) throws IOException,
				InterruptedException {
			swMethod.resume();

			if (this.pointBuffer.size() < MAX_ITEMS) {
				this.pointBuffer.add(value);
				this.clPoint.put(value);
			} else {
				processBuffer(context);
				this.map(key, value, context);
			}

			swMethod.pause();
		}

		private void processBuffer(KMMapper.Context context)
				throws IOException, InterruptedException {
			this.clPoint.setNearestPoints();
			for (PointWritable cp : pointBuffer) {
				// Cast is safe, list contains PointWritable only. See setup()
				// and CLPointFloat.
				context.write((PointWritable) cp.getCentroid(), cp);
			}
			this.pointBuffer.clear();
			context.getCounter("OpenCL", "processBuffer() calls").increment(1);
		}

		@Override
		protected void cleanup(KMMapper.Context context) {
			try {
				processBuffer(context);
			} catch (Exception e) {
				Logger.logError(CLAZZ, "cleanup: " + e.getMessage());
			}

			swMethod.stop();
			Logger.log(KMeansHadoop.TIME_LEVEL, CLAZZ, swMethod.getTimeString());

			swPhase.stop();
			Logger.log(KMeansHadoop.TIME_LEVEL, CLAZZ, swPhase.getTimeString());
		}

	}

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
