package hadoop;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import lightLogger.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FsUrlStreamHandlerFactory;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import utils.Points;
import cl_util.CLInstance;
import cl_util.CLPointFloat;
import cl_util.CLSummarizerFloat;
import cl_util.ICLSummarizer;
import clustering.CPoint;
import clustering.ICPoint;
import clustering.IPoint;
import clustering.KMeans;

public class KMeansHadoopCL extends Configured implements Tool {

	public static final int SUCCESS = 0;
	public static final int FAILURE = 1;

	public static final int INAME = 0;
	public static final int IINPUT = 1;
	public static final int IOUTPUT = 2;
	public static final int ICENTROIDS = 3;
	public static final int IITERATIONS = 4;

	public static final String CHARSET = "UTF-8";

	public static String OUTPUT;

	public static void main(String[] args) throws Exception {
		GenericOptionsParser gop = new GenericOptionsParser(args);
		String[] rArgs = gop.getRemainingArgs();
		if (rArgs.length < 5) {
			System.out
					.println("Arguments: <Jobname> <Input> <Output> <Centroids> <Iterations>");
			System.exit(FAILURE);
		}
		int res;
		OUTPUT = rArgs[IOUTPUT];
		final int ITERATIONS = Integer.parseInt(rArgs[IITERATIONS]);
		String centroids = rArgs[ICENTROIDS];
		int i = 0;

		// load HDFS handler, only once!
		FsUrlStreamHandlerFactory factory = new org.apache.hadoop.fs.FsUrlStreamHandlerFactory();
		java.net.URL.setURLStreamHandlerFactory(factory);

		generateInput(rArgs[IINPUT], rArgs[ICENTROIDS], gop.getConfiguration());

		long start = System.currentTimeMillis();
		do {
			rArgs[IOUTPUT] = centroids + "-" + (i + 1);
			res = ToolRunner.run(gop.getConfiguration(), new KMeansHadoopCL(),
					rArgs);
			rArgs[ICENTROIDS] = centroids + "-" + (i + 1);
			i++;
		} while (i < ITERATIONS && res == SUCCESS);

		// collect clusters in a final map
		rArgs[IOUTPUT] = OUTPUT;
		res = ToolRunner.run(gop.getConfiguration(), new KMeansHadoopCL(),
				rArgs);
		long end = System.currentTimeMillis();
		System.out.println("Time: " + (end - start));

		System.exit(res);
	}

	private static void generateInput(String fPoints, String fCentroids,
			Configuration configuration) {
		KMeans kmeans = new KMeans();
		List<IPoint<Float>> centroids = kmeans.initialize(2, 10);
		Points pHelper = new Points(kmeans.getDim());
		List<ICPoint<Float>> points = pHelper.generate(kmeans.getK(), 1000, 1);

		FileSystem fs = null;
		FSDataOutputStream fos = null;
		try {
			fs = FileSystem.get(configuration);

			fos = fs.create(new Path(fPoints + "/points"));
			for (ICPoint<Float> p : points) {
				fos.write(PointOutputFormat.createString(p).getBytes(CHARSET));
				fos.write("\n".getBytes(CHARSET));
			}
			fos.close();

			fos = fs.create(new Path(fCentroids + "/centroids"));
			for (IPoint<Float> p : centroids) {
				fos.write(PointOutputFormat.createString(p).getBytes(CHARSET));
				fos.write("\n".getBytes(CHARSET));
			}
			fos.close();

			fs.close();
		} catch (IOException e) {
			Logger.logError(KMeansHadoopCL.class,
					"Could not generate input data.");
			e.printStackTrace();
		} finally {
			if (fs != null)
				try {
					fs.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (fos != null)
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

	}

	@Override
	public int run(String[] args) throws Exception {
		Job job = new Job(this.getConf());

		job.setJobName(args[INAME]);

		job.setJarByClass(KMeansHadoopCL.class);

		job.setMapperClass(KMeansHadoopCL.KMapper.class);
		if (args[IOUTPUT] != OUTPUT)
			job.setReducerClass(KMeansHadoopCL.KReducer.class);
		else
			// Run a mapper only, to get the cluster result
			job.setNumReduceTasks(0);

		job.setMapOutputKeyClass(PointWritable.class);
		job.setMapOutputValueClass(PointWritable.class);

		job.setOutputKeyClass(PointWritable.class);
		job.setOutputValueClass(PointWritable.class);

		job.setInputFormatClass(PointInputFormat.class);
		job.setOutputFormatClass(PointOutputFormat.class);

		PointInputFormat.setInputPaths(job, new Path(args[IINPUT]));
		PointOutputFormat.setOutputPath(job, new Path(args[IOUTPUT]));

		DistributedCache.addCacheFile(new URI(args[ICENTROIDS]),
				job.getConfiguration());

		int stat = job.waitForCompletion(true) ? SUCCESS : FAILURE;
		return stat;
	}

	public static class KMapper extends
			Mapper<NullWritable, PointWritable, PointWritable, PointWritable> {

		private List<PointWritable> centroids;
		private CLPointFloat clPoint;

		@Override
		protected void setup(KMapper.Context context) {
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
						sc = new Scanner(fs.open(fst.getPath()));
						while (sc.hasNext())
							this.centroids.add(PointInputFormat
									.createPointWritable(sc.next()));
						sc.close();
					}
				}
			} catch (IOException e) {
				Logger.logError(KMapper.class,
						"Could not get local cache files");
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
			this.clPoint.prepareNearestPoints(tmpCentroids);
			this.clPoint.resetBuffer(1);
		}

		@Override
		protected void map(NullWritable key, PointWritable value,
				KMapper.Context context) throws IOException,
				InterruptedException {
			ICPoint<Float> cp = new CPoint(value);

			this.clPoint.put(cp);
			this.clPoint.setNearestPoints();

			PointWritable centroid = new PointWritable(cp.getCentroid());

			context.write(centroid, value);
		}

	}

	public static class KReducer extends
			Reducer<PointWritable, PointWritable, PointWritable, PointWritable> {

		private ICLSummarizer<Float>[] clFloat;
		private CLInstance clInstance;

		@Override
		protected void setup(KReducer.Context context) {
			this.clInstance = new CLInstance(CLInstance.TYPES.CL_GPU);
		}

		@Override
		protected void reduce(PointWritable key,
				Iterable<PointWritable> values, Context context)
				throws IOException, InterruptedException {
			int DIM = key.getDim();

			// Each instance of CLFloat is for one sum only.
			// Hadoop-Iterable can only be use once!
			this.clFloat = new CLSummarizerFloat[DIM];
			for (int d = 0; d < DIM; d++)
				this.clFloat[d] = new CLSummarizerFloat(this.clInstance);

			int count = 0;
			for (IPoint<Float> p : values) {
				for (int d = 0; d < DIM; d++)
					this.clFloat[d].put(p.get(d));
				count++;
			}

			PointWritable centroid = new PointWritable(DIM);
			for (int d = 0; d < DIM; d++)
				centroid.set(d, this.clFloat[d].getSum() / count);

			context.write(centroid, null);
		}

	}

}
