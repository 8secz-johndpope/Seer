 package edu.jhu.thrax.tools;
 
 import java.io.BufferedWriter;
 import java.io.FileOutputStream;
 import java.util.logging.Logger;
 
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.io.SequenceFile;
 
 import edu.jhu.jerboa.util.FileManager;
 import edu.jhu.thrax.hadoop.distributional.SignatureWritable;
 
 public class SequenceToSignatures {
 
   private static final Logger logger = Logger.getLogger(SequenceToSignatures.class.getName());
 
   public static void main(String[] args) throws Exception {
 
     boolean local = false;
     String input_file = null;
     int chunk_size = 500000;
     String output_prefix = null;
 
     for (int i = 0; i < args.length; i++) {
       if ("-i".equals(args[i]) && (i < args.length - 1)) {
         input_file = args[++i];
       } else if ("-o".equals(args[i]) && (i < args.length - 1)) {
         output_prefix = args[++i];
       } else if ("-c".equals(args[i]) && (i < args.length - 1)) {
         chunk_size = Integer.parseInt(args[++i]);
       } else if ("-l".equals(args[i])) {
         local = true;
       }
     }
     if (input_file == null) {
       logger.severe("No input file specified.");
       System.exit(0);
     }
     if (output_prefix == null) {
       logger.severe("No output prefix specified.");
       System.exit(0);
     }
    
    logger.info("Looking for " + input_file + " on " + (local ? "local filesystem" : "HDFS") +".");
    
     Configuration config = new Configuration();
    Path path = new Path(input_file);
    FileSystem file_system = (local ? FileSystem.getLocal(config) : FileSystem.get(config));
    SequenceFile.Reader reader = new SequenceFile.Reader(file_system, path, config);
     SignatureWritable signature = new SignatureWritable();
     
     int chunk_id = 0;
     int key_count = 0;
    
     FileOutputStream bytes_out = null;
     BufferedWriter strengths_writer = null;
     BufferedWriter keys_writer = null;
    
     while (reader.next(signature)) {
       if (key_count % chunk_size == 0) {
         if (key_count != 0) {
           keys_writer.close();
           bytes_out.close();
           strengths_writer.close();
         }
        String chunk_tag = String.format("-%05d", chunk_id); 
         bytes_out = new FileOutputStream(output_prefix + chunk_tag + ".bytes");
         strengths_writer = FileManager.getWriter(output_prefix + chunk_tag + ".strengths.gz");
         keys_writer = FileManager.getWriter(output_prefix + chunk_tag + ".keys.gz");
         chunk_id++;
       }
       keys_writer.write(signature.key.toString());
       keys_writer.newLine();
       bytes_out.write(signature.bytes.getBytes());
       strengths_writer.write("" + signature.strength.get());
       strengths_writer.newLine();
       key_count++;
     }
     reader.close();
     keys_writer.close();
     bytes_out.close();
     strengths_writer.close();
   }
 }
