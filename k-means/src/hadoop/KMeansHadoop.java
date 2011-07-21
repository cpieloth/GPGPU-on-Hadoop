package hadoop;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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
import clustering.ICPoint;
import clustering.IPoint;
import clustering.KMeans;

// TODO
public class KMeansHadoop extends Configured implements Tool {

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
		if(rArgs.length < 5) {
			System.out.println("Arguments: <Jobname> <Input> <Output> <Centroids> <Iterations>");
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

		do {
			rArgs[IOUTPUT] = centroids + "-" + (i+1);
			res = ToolRunner.run(gop.getConfiguration(), new KMeansHadoop(),
					rArgs);
			rArgs[ICENTROIDS] = centroids + "-" + (i+1);
			i++;
		} while (i < ITERATIONS && res == SUCCESS);

		// collect clusters in a final map
		rArgs[IOUTPUT] = OUTPUT;
		res = ToolRunner.run(gop.getConfiguration(), new KMeansHadoop(), rArgs);

		System.exit(res);
	}

	private static void generateInput(String fPoints, String fCentroids,
			Configuration configuration) {
		KMeans kmeans = new KMeans();
		List<IPoint> centroids = kmeans.initialize(2, 10);
		Points pHelper = new Points(kmeans.getDim());
		List<ICPoint> points = pHelper.generate(kmeans.getK(), 1000, 1);
		
		try {
			FileSystem fs = FileSystem.get(configuration);
			
			FSDataOutputStream fos = fs.create(new Path(fPoints + "/points"));
			for(ICPoint p : points) {
				fos.write(PointOutputFormat.createString(p).getBytes(CHARSET));
				fos.write("\n".getBytes(CHARSET));
			}
			fos.close();
			
			fos = fs.create(new Path(fCentroids + "/centroids"));
			for(IPoint p : centroids) {
				fos.write(PointOutputFormat.createString(p).getBytes(CHARSET));
				fos.write("\n".getBytes(CHARSET));
			}
			fos.close();
			
			fs.close();
		} catch (IOException e) {
			Logger.logError(KMeansHadoop.class, "Could not generate input data.");
			e.printStackTrace();
		}
		
	}

	@Override
	public int run(String[] args) throws Exception {
		Job job = new Job(this.getConf());
		
		job.setJobName(args[INAME]);
		
		job.setJarByClass(KMeansHadoop.class);
		
		job.setMapperClass(KMeansHadoop.KMapper.class);
		if(args[IOUTPUT] != OUTPUT)
			job.setReducerClass(KMeansHadoop.KReducer.class);
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

		@Override
		protected void setup(KMapper.Context context) {
			// TODO read max k from conf to use ArrayList
			this.centroids = new LinkedList<PointWritable>();
			try {
				InputStream is;
				Scanner sc;

				URI[] uris = DistributedCache.getCacheFiles(context
						.getConfiguration());
				FileSystem fs = FileSystem.get(context.getConfiguration());
				for (FileStatus fst : fs
						.listStatus(new Path(uris[0].toString()))) {
					if (!fst.isDir()) {
						Logger.logDebug(KMapper.class,
								"centroids: " + fst.getPath());
						is = fs.open(fst.getPath());
						sc = new Scanner(is);
						while (sc.hasNext())
							this.centroids.add(PointInputFormat
									.createPointWritable(sc.next()));
					}
				}
			} catch (IOException e) {
				Logger.logError(KMapper.class,
						"Could not get local cache files");
				e.printStackTrace();
			}
		}

		@Override
		public void map(NullWritable key, PointWritable value,
				KMapper.Context context) throws IOException,
				InterruptedException {

			PointWritable centroid = null;

			Logger.logDebug(KMapper.class, "map( " + key + " , " + value + " )");
			float prevDist = Float.MAX_VALUE, dist;

			for (PointWritable c : this.centroids) {
				dist = this.computeDistance(value, c);
				if (dist < prevDist) {
					prevDist = dist;
					centroid = c;
				}
			}

			context.write(centroid, value);
			Logger.logDebug(KMapper.class, "context.write( " + centroid + " , "
					+ value + " )");
		}

		private float computeDistance(final IPoint p, final IPoint c) {
			float dist = 0;
			for (int d = 0; d < p.getDim(); d++)
				dist += Math.pow(c.get(d) - p.get(d), 2);
			return (float) Math.sqrt(dist);
		}
	}

	public static class KReducer extends
			Reducer<PointWritable, PointWritable, PointWritable, PointWritable> {

		@Override
		public void reduce(PointWritable key, Iterable<PointWritable> values,
				Context context) throws IOException, InterruptedException {
			Logger.logDebug(KReducer.class, "reduce( " + key + " , ... )");
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
			Logger.logDebug(KReducer.class, "context.write( " + centroid
					+ " , " + null + " )");
		}

	}

}
