 package com.spotify.hdfs2cass;
 
 import org.apache.avro.mapred.AvroAsTextInputFormat;
 import org.apache.cassandra.hadoop.BulkOutputFormat;
 import org.apache.cassandra.hadoop.ColumnFamilyOutputFormat;
 import org.apache.cassandra.hadoop.ConfigHelper;
 import org.apache.cassandra.thrift.Column;
 import org.apache.cassandra.thrift.ColumnOrSuperColumn;
 import org.apache.cassandra.thrift.Mutation;
 import org.apache.cassandra.utils.ByteBufferUtil;
 import org.apache.commons.cli.*;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.conf.Configured;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.io.*;
 import org.apache.hadoop.mapred.*;
 import org.apache.hadoop.util.Tool;
 import org.apache.hadoop.util.ToolRunner;
 
 import java.io.IOException;
 import java.nio.ByteBuffer;
 import java.util.*;
 
 public class BulkLoader extends Configured implements Tool {
 
   //basic Map job to parse into a Text based reducable format.
   public static class MapToText extends MapReduceBase implements Mapper<Text, Text, Text, Text> {
     public void map(Text value, Text ignored, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
       // each value is a line of tab separated columns with values:
       // HdfsToCassandra 1 <rowkey> <colkey> <value>
       // HdfsToCassandra 2 <rowkey> <colkey> <ttl> <value>
       // HdfsToCassandra 3 <rowkey> <colkey> <timestamp> <ttl> <value>
       // if the timestamp isn't present, it is assumed to be Now
       // if the ttl isn't present, it is assumed to be none, ie keep forever
 
       String line = value.toString();
       int tab1, tab2;
       tab1 = 0;
       tab2 = line.indexOf('\t');
       String type = line.substring(tab1, tab2);
       if (!type.equals("HdfsToCassandra")) {
         System.err.println("Unknown type: " + type + ", in line: " + line);
         return;
       }
 
       tab1 = tab2 + 1;
       tab2 = line.indexOf('\t', tab1);
       int version = Integer.parseInt(line.substring(tab1, tab2));
 
       if (version < 1 || version > 3) {
         System.err.println("Unknown version: " + version + ", in line: " + line);
         return;
       }
 
       tab1 = tab2 + 1;
       tab2 = line.indexOf('\t', tab1);
       if (tab2 == -1) {
         System.err.println("Missing rowkey in line: " + line);
         return;
       }
       String rowkey = line.substring(tab1, tab2);
 
       tab1 = tab2 + 1;
       tab2 = line.indexOf('\t', tab1);
       if (tab2 == -1) {
         System.err.println("Missing colkey in line: " + line);
         return;
       }
       String colkey = line.substring(tab1, tab2);
 
       String timestamp;
       if (version > 2) {
         tab1 = tab2 + 1;
         tab2 = line.indexOf('\t', tab1);
         if (tab2 == -1) {
           System.err.println("Missing timestamp in line: " + line);
           return;
         }
         timestamp = line.substring(tab1, tab2);
       } else {
         timestamp = String.valueOf(System.currentTimeMillis() * 1000);
       }
 
       String ttl;
       if (version > 1) {
         tab1 = tab2 + 1;
         tab2 = line.indexOf('\t', tab1);
         if (tab2 == -1) {
           System.err.println("Missing ttl in line: " + line);
           return;
         }
         ttl = line.substring(tab1, tab2);
       } else {
         ttl = "0";
       }
 
       tab1 = tab2 + 1;
       String colvalue = line.substring(tab1);
 
       output.collect(new Text(rowkey), new Text(colkey + '\t' + timestamp + '\t' + ttl + '\t' + colvalue));
     }
   }
 
   //take values from MapToText and send to cassandra via the BulkOutputFormat which writes local sstables and streams them.
   public static class ReduceTextToCassandra extends MapReduceBase implements Reducer<Text, Text, ByteBuffer, List<Mutation>> {
     public void reduce(Text key, Iterator<Text> values, OutputCollector<ByteBuffer, List<Mutation>> output, Reporter reporter) throws IOException {
       String rowkey = key.toString();
       List<Mutation> list = new ArrayList<Mutation>();
       while (values.hasNext()) {
         //each value is a line of tab separated columns with meaning:
         //columnKey, timestamp, ttl, columnValue
         //rowkey isn't included since it's sent separately
         String line = values.next().toString();
         String[] cols = line.split("\t", 4);
 
         Column column = new Column();
         column.setName(ByteBufferUtil.bytes(cols[0]));
         column.setTimestamp(Long.parseLong(cols[1]));
         int ttl = Integer.parseInt(cols[2]);
         if (ttl != 0)
           column.setTtl(ttl);
         column.setValue(ByteBufferUtil.bytes(cols[3]));
 
         Mutation mutation = new Mutation();
         mutation.column_or_supercolumn = new ColumnOrSuperColumn();
         mutation.column_or_supercolumn.column = column;
         list.add(mutation);
       }
       if (!list.isEmpty()) {
         output.collect(ByteBufferUtil.bytes(rowkey), list);
       }
     }
   }
 
   public static void main(String[] args) throws Exception {
     int exitCode = ToolRunner.run(new BulkLoader(), args);
     System.exit(exitCode);
   }
 
   public int run(String[] args) throws Exception {
     CommandLine cmdLine = parseOptions(args);
 
     Path inputPath = new Path(cmdLine.getOptionValue('i'));
     String seedNodeHost = cmdLine.getOptionValue('h');
     String seedNodePort = cmdLine.getOptionValue('p', "9160");
     String keyspace = cmdLine.getOptionValue('k');
     String colfamily = cmdLine.getOptionValue('c');
     int mappers = Integer.parseInt(cmdLine.getOptionValue('m', "0"));
     int reducers = Integer.parseInt(cmdLine.getOptionValue('r', "0"));
 
     Configuration conf = new Configuration();
     ConfigHelper.setOutputColumnFamily(conf, keyspace, colfamily);
     ConfigHelper.setOutputInitialAddress(conf, seedNodeHost);
     ConfigHelper.setOutputRpcPort(conf, seedNodePort);
     ConfigHelper.setOutputPartitioner(conf, "org.apache.cassandra.dht.RandomPartitioner");
 
     if (cmdLine.hasOption('s')) {
       conf.set("mapreduce.output.bulkoutputformat.buffersize", cmdLine.getOptionValue('s', "32"));
     }
 
     if (cmdLine.hasOption('M')) {
       conf.set("mapreduce.output.bulkoutputformat.streamthrottlembits", cmdLine.getOptionValue('M'));
     }
 
     if (cmdLine.hasOption('C')) {
       ConfigHelper.setOutputCompressionClass(conf, cmdLine.getOptionValue('c'));
     }
 
     JobConf job = new JobConf(conf);
 
     if (mappers > 0)
       job.setNumMapTasks(mappers);
     if (reducers > 0)
       job.setNumReduceTasks(reducers);
 
     String jobName = "bulkloader-hdfs-to-cassandra";
     if (cmdLine.hasOption('n'))
       jobName += "-" + cmdLine.getOptionValue('n');
     job.setJobName(jobName);
     job.setJarByClass(BulkLoader.class);
 
     job.setInputFormat(AvroAsTextInputFormat.class);
 
     FileInputFormat.setInputPaths(job, inputPath);
 
     //map just outputs text, reduce sends to cassandra
     job.setMapperClass(MapToText.class);
     job.setMapOutputKeyClass(Text.class);
     job.setMapOutputValueClass(Text.class);
 
     job.setReducerClass(ReduceTextToCassandra.class);
     job.setOutputKeyClass(ByteBuffer.class);
     job.setOutputValueClass(List.class);
 
     if (cmdLine.hasOption('s'))
       job.setOutputFormat(BulkOutputFormat.class);
     else
       job.setOutputFormat(ColumnFamilyOutputFormat.class);
 
     JobClient.runJob(job);
     return 0;
   }
 
   private static CommandLine parseOptions(String[] args) throws ParseException {
     Options options = new Options();
     options.addOption("i", "input", true, "Input path");
     options.addOption("h", "host", true, "Cassandra Seed Node Host");
     options.addOption("p", "port", true, "Cassandra Seed Node Port [9160]");
     options.addOption("k", "keyspace", true, "Keyspace to write to");
     options.addOption("c", "columnfamily", true, "Column Family to write to");
     options.addOption("m", "mappers", true, "Number of Mappers");
     options.addOption("r", "reducers", true, "Number of Reducers");
    options.addOption("s", "sstablebuffersize", true, "Buffer size in MB before writing an sstable, otherwise, send directly");
     options.addOption("C", "compression", true, "Compression class to use, if writing sstable's");
     options.addOption("M", "throttle_mbits",true, "Throttling setting, if writing sstable's [0=UNLIMITED]");
     options.addOption("n", "jobname", true, "Name of this job [bulkloader-hdfs-to-cassandra]");
 //    options.addOption("l", "libjars",      true, "I don't know why ToolRunner propagates it"); //http://grokbase.com/t/hadoop/common-user/1181pxrd93/using-libjar-option
 
     CommandLineParser parser = new GnuParser();
     CommandLine cmdLine = null;
     try {
       cmdLine = parser.parse(options, args, false);
     } catch (MissingArgumentException e) {
     }
 
     boolean badOptions = false;
     if (cmdLine == null ||
             !cmdLine.hasOption('i') ||
             !cmdLine.hasOption('h') ||
             !cmdLine.hasOption('k') ||
             !cmdLine.hasOption('c')
             ) {
       badOptions = true;
     } else if (!cmdLine.hasOption('s') &&
             (cmdLine.hasOption('C') || cmdLine.hasOption('M'))) {
       badOptions = true;
     }
 
     if (badOptions) {
       printUsage(options);
       System.exit(1);
     }
 
     return cmdLine;
   }
 
   private static final String USAGE = "-i input/path -h cassandra-host.site.domain -k keyspace -c colfamily";
   private static final String HEADER = "HDFS to Cassandra Bulk Loader";
   private static final String FOOTER = "";
 
   private static void printUsage(Options options) {
     HelpFormatter helpFormatter = new HelpFormatter();
     helpFormatter.setWidth(80);
     helpFormatter.printHelp(USAGE, HEADER, options, FOOTER);
   }
 }
