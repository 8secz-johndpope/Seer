 /**
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License.  You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package at.illecker.hadoop.rootbeer.examples.matrixmultiplication;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Random;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.FSDataInputStream;
 import org.apache.hadoop.fs.FSDataOutputStream;
 import org.apache.hadoop.fs.FileStatus;
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.io.IOUtils;
 import org.apache.hadoop.io.IntWritable;
 import org.apache.hadoop.mapred.FileOutputFormat;
 import org.apache.hadoop.mapred.JobConf;
 import org.apache.hadoop.mapred.MapReduceBase;
 import org.apache.hadoop.mapred.Mapper;
 import org.apache.hadoop.mapred.OutputCollector;
 import org.apache.hadoop.mapred.Reducer;
 import org.apache.hadoop.mapred.Reporter;
 import org.apache.hadoop.mapred.SequenceFileInputFormat;
 import org.apache.hadoop.mapred.SequenceFileOutputFormat;
 import org.apache.hadoop.mapred.join.CompositeInputFormat;
 import org.apache.hadoop.mapred.join.TupleWritable;
 import org.apache.hadoop.util.ToolRunner;
 import org.apache.mahout.common.AbstractJob;
 import org.apache.mahout.math.DenseVector;
 import org.apache.mahout.math.RandomAccessSparseVector;
 import org.apache.mahout.math.SequentialAccessSparseVector;
 import org.apache.mahout.math.Vector;
 import org.apache.mahout.math.VectorWritable;
 import org.apache.mahout.math.function.Functions;
 
 import edu.syr.pcpratts.rootbeer.runtime.Kernel;
 import edu.syr.pcpratts.rootbeer.runtime.Rootbeer;
 import edu.syr.pcpratts.rootbeer.runtime.StatsRow;
 import edu.syr.pcpratts.rootbeer.runtime.util.Stopwatch;
 
 /**
  * @author MatrixMultiplication based on Mahout https://github.com/apache/mahout
  *         /blob/trunk/core/src/main/java/org/apache
  *         /mahout/math/hadoop/MatrixMultiplicationJob.java
  * 
  */
 
 public class MatrixMultiplicationGpu extends AbstractJob {
 
 	private static final Log LOG = LogFactory
 			.getLog(MatrixMultiplicationGpu.class);
 
 	private static final String OUT_CARD = "output.vector.cardinality";
 	private static final String DEBUG = "matrixmultiplication.gpu.debug";
 
 	private static final Path OUTPUT_DIR = new Path(
 			"output/hadoop/rootbeer/examples/matrixmultiplication/GPU-"
 					+ System.currentTimeMillis());
 	private static final Path MATRIX_A_PATH = new Path(
 			"input/hadoop/rootbeer/examples/MatrixA.seq");
 	private static final Path MATRIX_B_PATH = new Path(
 			"input/hadoop/rootbeer/examples/MatrixB.seq");
 	private static final Path MATRIX_C_PATH = new Path(OUTPUT_DIR
 			+ "/MatrixC.seq");
 
 	public static Configuration createMatrixMultiplicationGpuConf(Path aPath,
 			Path bPath, Path outPath, int outCardinality) {
 
 		return createMatrixMultiplicationGpuConf(new Configuration(), aPath,
 				bPath, outPath, outCardinality);
 	}
 
 	public static Configuration createMatrixMultiplicationGpuConf(
 			Configuration initialConf, Path aPath, Path bPath, Path outPath,
 			int outCardinality) {
 
 		JobConf conf = new JobConf(initialConf, MatrixMultiplicationCpu.class);
 		conf.setJobName("MatrixMultiplicationGPU: " + aPath + " x " + bPath
 				+ " = " + outPath);
 
 		conf.setInt(OUT_CARD, outCardinality);
 
 		conf.setInputFormat(CompositeInputFormat.class);
 		conf.set("mapred.join.expr", CompositeInputFormat.compose("inner",
 				SequenceFileInputFormat.class, aPath, bPath));
 
 		conf.setOutputFormat(SequenceFileOutputFormat.class);
 		FileOutputFormat.setOutputPath(conf, outPath);
 
 		conf.setMapperClass(MatrixMultiplyGpuMapper.class);
 		conf.setCombinerClass(MatrixMultiplicationGpuReducer.class);
 		conf.setReducerClass(MatrixMultiplicationGpuReducer.class);
 
 		conf.setMapOutputKeyClass(IntWritable.class);
 		conf.setMapOutputValueClass(VectorWritable.class);
 
 		conf.setOutputKeyClass(IntWritable.class);
 		conf.setOutputValueClass(VectorWritable.class);
 
 		// Inscrease client heap size for GPU Rootbeer execution
 		conf.set("mapred.child.java.opts", "-Xmx4G");
 
 		return conf;
 	}
 
 	public static void main(String[] args) throws Exception {
 		ToolRunner.run(new MatrixMultiplicationGpu(), args);
 	}
 
 	@Override
 	public int run(String[] strings) throws Exception {
 		// addOption("numRowsA", "nra",
 		// "Number of rows of the first input matrix", true);
 		// addOption("numColsA", "nca",
 		// "Number of columns of the first input matrix", true);
 
 		// addOption("numRowsB", "nrb",
 		// "Number of rows of the second input matrix", true);
 		// addOption("numColsB", "ncb",
 		// "Number of columns of the second input matrix", true);
 
 		// addOption("inputPathA", "ia", "Path to the first input matrix",
 		// true);
 		// addOption("inputPathB", "ib", "Path to the second input matrix",
 		// true);
 
 		// addOption("outputPath", "op", "Path to the output matrix", false);
 
 		addOption("numRows", "nr", "Number of rows of matrix", true);
 		addOption("numCols", "nc", "Number of columns of matrix", true);
 		addOption("debug", "db", "Enable debugging (true|false)", false);
 
 		Map<String, List<String>> argMap = parseArguments(strings);
 		if (argMap == null) {
 			return -1;
 		}
 
 		int numRows = Integer.parseInt(getOption("numRows"));
 		int numCols = Integer.parseInt(getOption("numCols"));
 		boolean isDebugging = Boolean.parseBoolean(getOption("debug"));
 
 		LOG.info("numRows: " + numRows);
 		LOG.info("numCols: " + numCols);
 		LOG.info("isDebugging: " + isDebugging);
 		LOG.info("outputPath: " + OUTPUT_DIR);
 
 		Configuration conf = new Configuration(getConf());
 		conf.setBoolean(DEBUG, isDebugging);
 
 		// Create random DistributedRowMatrix
 		// use constant seeds to get reproducable results
 		DistributedRowMatrix.createRandomDistributedRowMatrix(conf, numRows,
 				numCols, new Random(42L), MATRIX_A_PATH);
 		DistributedRowMatrix.createRandomDistributedRowMatrix(conf, numRows,
 				numCols, new Random(1337L), MATRIX_B_PATH);
 
 		// Load DistributedRowMatrix a and b
 		DistributedRowMatrix a = new DistributedRowMatrix(MATRIX_A_PATH,
 				OUTPUT_DIR, numRows, numCols);
 		a.setConf(conf);
 
 		DistributedRowMatrix b = new DistributedRowMatrix(MATRIX_B_PATH,
 				OUTPUT_DIR, numRows, numCols);
 		b.setConf(conf);
 
 		// MatrixMultiply all within a new MapReduce job
 		long startTime = System.currentTimeMillis();
 		DistributedRowMatrix c = a.timesMapReduce(b, MATRIX_C_PATH, true);
 		System.out.println("MatrixMultiplicationGpu using Hadoop finished in "
 				+ (System.currentTimeMillis() - startTime) / 1000.0
 				+ " seconds");
 
 		if (isDebugging) {
 			System.out.println("Matrix A:");
 			a.printDistributedRowMatrix();
 			System.out.println("Matrix B:");
 			b.printDistributedRowMatrix();
 			System.out.println("Matrix C:");
 			c.printDistributedRowMatrix();
 
 			printOutput(conf);
 		}
 		return 0;
 	}
 
 	static void printOutput(Configuration conf) throws IOException {
 		FileSystem fs = OUTPUT_DIR.getFileSystem(conf);
 		FileStatus[] files = fs.listStatus(OUTPUT_DIR);
 		for (int i = 0; i < files.length; i++) {
 			if (files[i].getLen() > 0) {
 				System.out.println("File " + files[i].getPath());
 				if (files[i].getPath().getName().endsWith(".log")) {
 					FSDataInputStream in = fs.open(files[i].getPath());
 					IOUtils.copyBytes(in, System.out, conf, false);
 					in.close();
 				}
 			}
 		}
 		// fs.delete(FileOutputFormat.getOutputPath(job), true);
 	}
 
 	public static class MatrixMultiplyGpuMapper extends MapReduceBase implements
 			Mapper<IntWritable, TupleWritable, IntWritable, VectorWritable> {
 
 		private int outCardinality;
 
 		private boolean isDebuggingEnabled;
 		private FSDataOutputStream logMapper;
 
 		private List<Kernel> kernels = new ArrayList<Kernel>();
 
 		// New Hadoop API would provide a context object to write results
 		// Mapper.cleanup(Context)
 		// To submit data in the close method we need a
 		// reference to OutputCollector;
 		OutputCollector<IntWritable, VectorWritable> out;
 
 		@Override
 		public void configure(JobConf conf) {
 
 			outCardinality = conf.getInt(OUT_CARD, Integer.MAX_VALUE);
 			isDebuggingEnabled = conf.getBoolean(DEBUG, false);
 
 			// Init logging
 			if (isDebuggingEnabled) {
 				try {
 					FileSystem fs = FileSystem.get(conf);
 					logMapper = fs.create(new Path(FileOutputFormat
 							.getOutputPath(conf).getParent()
 							+ "/Mapper_"
 							+ conf.get("mapred.job.id") + ".log"));
 
					logMapper.writeChars("map,NumMapTasks="
 							+ conf.getNumMapTasks() + "\n");
 
 					logMapper.writeChars("map,configure,outCardinality="
 							+ outCardinality + "\n");
 				} catch (IOException e) {
 					e.printStackTrace();
 				}
 			}
 		}
 
 		@Override
 		public void map(IntWritable index, TupleWritable v,
 				OutputCollector<IntWritable, VectorWritable> out,
 				Reporter reporter) throws IOException {
 
 			// Set OutputCollector reference, for close method
 			this.out = out;
 
 			// Logging
 			if (isDebuggingEnabled) {
 				for (int i = 0; i < v.size(); i++) {
 					Vector vector = ((VectorWritable) v.get(i)).get();
 					logMapper.writeChars("map,input,key=" + index + ",value="
 							+ vector.toString() + "\n");
 				}
 			}
 
 			boolean firstIsOutFrag = ((VectorWritable) v.get(0)).get().size() == outCardinality;
 			if (isDebuggingEnabled)
 				logMapper.writeChars("map,firstIsOutFrag=" + firstIsOutFrag
 						+ "\n");
 
 			Vector outFrag = firstIsOutFrag ? ((VectorWritable) v.get(0)).get()
 					: ((VectorWritable) v.get(1)).get();
 
 			Vector multiplier = firstIsOutFrag ? ((VectorWritable) v.get(1))
 					.get() : ((VectorWritable) v.get(0)).get();
 
 			if (isDebuggingEnabled) {
 				logMapper.writeChars("map,outFrag=" + outFrag + "\n");
 				logMapper.writeChars("map,multiplier=" + multiplier + "\n");
 			}
 
 			// multiplier to double[]
 			double[] multiplierArray = new double[multiplier.size()];
 			int i = 0;
 			for (Vector.Element e : multiplier.all()) {
 				multiplierArray[i] = e.get();
 				i++;
 			}
 
 			// Add Scalar Multiplication (Vector x Element) Kernel
 			for (Vector.Element e : outFrag.nonZeroes()) {
 				kernels.add(new MatrixMultiplicationMapperKernel(e.index(),
 						multiplierArray, e.get()));
 			}
 
 			if (isDebuggingEnabled) {
 				logMapper.writeChars("map,GPUKernels=" + kernels.size() + "\n");
 				logMapper.flush();
 			}
 		}
 
 		@Override
 		public void close() throws IOException {
 
 			// After last input key/value run GPU kernels
 			Stopwatch watch = new Stopwatch();
 			watch.start();
 			Rootbeer rootbeer = new Rootbeer();
 			// rootbeer.setThreadConfig(m_blockSize, m_gridSize);
 			rootbeer.runAll(kernels);
 			watch.stop();
 
 			if (isDebuggingEnabled) {
 				logMapper.writeChars("map,close,KernelCount=" + kernels.size()
 						+ ",GPUTime=" + watch.elapsedTimeMillis() + "ms\n");
 				List<StatsRow> stats = rootbeer.getStats();
 				for (StatsRow row : stats) {
 					logMapper.writeChars("  StatsRow:\n");
 					logMapper.writeChars("    init time: " + row.getInitTime()
 							+ "\n");
 					logMapper.writeChars("    serial time: "
 							+ row.getSerializationTime() + "\n");
 					logMapper.writeChars("    exec time: "
 							+ row.getExecutionTime() + "\n");
 					logMapper.writeChars("    deserial time: "
 							+ row.getDeserializationTime() + "\n");
 					logMapper.writeChars("    num blocks: "
 							+ row.getNumBlocks() + "\n");
 					logMapper.writeChars("    num threads: "
 							+ row.getNumThreads() + "\n");
 				}
 				logMapper.flush();
 			}
 
 			// Submit result of GPU kernels
 			for (Kernel kernel : kernels) {
 				MatrixMultiplicationMapperKernel mapperKernel = (MatrixMultiplicationMapperKernel) kernel;
 				out.collect(
 						new IntWritable(mapperKernel.row),
 						new VectorWritable(new DenseVector(mapperKernel.result)));
 
 				if (isDebuggingEnabled) {
					logMapper
							.writeChars("map,collect,key=" + mapperKernel.row
									+ ",value="
									+ mapperKernel.result.toString() + "\n");
 				}
 			}
 
 		}
 	}
 
 	public static class MatrixMultiplicationGpuReducer extends MapReduceBase
 			implements
 			Reducer<IntWritable, VectorWritable, IntWritable, VectorWritable> {
 
 		@Override
 		public void reduce(IntWritable rowNum, Iterator<VectorWritable> it,
 				OutputCollector<IntWritable, VectorWritable> out,
 				Reporter reporter) throws IOException {
 
 			if (!it.hasNext()) {
 				return;
 			}
 
 			Vector accumulator = new RandomAccessSparseVector(it.next().get());
 			while (it.hasNext()) {
 				Vector row = it.next().get();
 				accumulator.assign(row, Functions.PLUS);
 			}
 
 			out.collect(rowNum, new VectorWritable(
 					new SequentialAccessSparseVector(accumulator)));
 		}
 	}
 }
