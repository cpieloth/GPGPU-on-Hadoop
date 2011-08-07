package hadoop;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
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
import clustering.IPoint;

public class KMMapperReducer {

	public static class KMMapper extends
			Mapper<NullWritable, PointWritable, PointWritable, PointWritable> {

		private static final Class<KMMapper> CLAZZ = KMMapper.class;

		private List<PointWritable> centroids;

		private StopWatch swPhase = new StopWatch(KMeansHadoop.PRE_MAPPHASE,
				KMeansHadoop.SUFFIX);
		private StopWatch swMethod = new StopWatch(KMeansHadoop.PRE_MAPMETHOD,
				KMeansHadoop.SUFFIX);

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
						Logger.logDebug(KMMapper.class,
								"centroids: " + fst.getPath());
						sc = new Scanner(fs.open(fst.getPath()));
						while (sc.hasNext())
							this.centroids.add(PointInputFormat
									.createPointWritable(sc.next()));
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
		}

		@Override
		protected void map(NullWritable key, PointWritable value,
				KMMapper.Context context) throws IOException,
				InterruptedException {
			swMethod.resume();

			PointWritable centroid = null;

			float prevDist = Float.MAX_VALUE, dist;

			for (PointWritable c : this.centroids) {
				dist = this.computeDistance(value, c);
				if (dist < prevDist) {
					prevDist = dist;
					centroid = c;
				}
			}

			context.write(centroid, value);

			swMethod.pause();
		}

		private float computeDistance(final IPoint<Float> p,
				final IPoint<Float> c) {
			float dist = 0;
			for (int d = 0; d < p.getDim(); d++)
				dist += Math.pow(c.get(d) - p.get(d), 2);
			return (float) Math.sqrt(dist);
		}

		@Override
		protected void cleanup(KMMapper.Context context) {
			swMethod.stop();
			Logger.log(KMeansHadoop.TIME_LEVEL, KMMapper.class,
					swMethod.getTimeString());

			swPhase.stop();
			Logger.log(KMeansHadoop.TIME_LEVEL, KMMapper.class,
					swPhase.getTimeString());
		}
	}

	public static class KMReducer extends
			Reducer<PointWritable, PointWritable, PointWritable, PointWritable> {

		private static final Class<KMReducer> CLAZZ = KMReducer.class;

		private StopWatch swPhase = new StopWatch(KMeansHadoop.PRE_REDUCEPHASE,
				KMeansHadoop.SUFFIX);
		private StopWatch swMethod = new StopWatch(
				KMeansHadoop.PRE_REDUCEMETHOD, KMeansHadoop.SUFFIX);

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
			Logger.log(KMeansHadoop.TIME_LEVEL, KMReducer.class,
					swMethod.getTimeString());

			swPhase.stop();
			Logger.log(KMeansHadoop.TIME_LEVEL, KMReducer.class,
					swPhase.getTimeString());
		}

	}

}
