 package org.apache.flume.client.avro;
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileReader;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.net.InetSocketAddress;
 import java.nio.ByteBuffer;
 import java.util.HashMap;
 
 import org.apache.avro.ipc.NettyTransceiver;
 import org.apache.avro.ipc.Transceiver;
 import org.apache.avro.ipc.specific.SpecificRequestor;
 import org.apache.commons.cli.CommandLine;
 import org.apache.commons.cli.CommandLineParser;
 import org.apache.commons.cli.GnuParser;
 import org.apache.commons.cli.Options;
 import org.apache.commons.cli.ParseException;
 import org.apache.flume.source.avro.AvroFlumeEvent;
 import org.apache.flume.source.avro.AvroSourceProtocol;
 import org.apache.flume.source.avro.Status;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 public class AvroCLIClient {
 
   private static final Logger logger = LoggerFactory
       .getLogger(AvroCLIClient.class);
 
   private String hostname;
   private int port;
   private String fileName;
 
   private int sent;
 
   public static void main(String[] args) {
     AvroCLIClient client = new AvroCLIClient();
 
     try {
       client.parseCommandLine(args);
       client.run();
     } catch (ParseException e) {
       logger.error("Unable to parse command line options - {}", e.getMessage());
     } catch (IOException e) {
       logger.error("Unable to send data to Flume - {}", e.getMessage());
       logger.debug("Exception follows.", e);
     }
 
     logger.debug("Exiting");
   }
 
   private void parseCommandLine(String[] args) throws ParseException {
     Options options = new Options();
 
     options.addOption("p", "port", true, "port of the avro source")
         .addOption("H", "host", true, "hostname of the avro source")
         .addOption("F", "filename", true, "file to stream to avro source");
 
     CommandLineParser parser = new GnuParser();
     CommandLine commandLine = parser.parse(options, args);
 
     port = Integer.parseInt(commandLine.getOptionValue("port"));
     hostname = commandLine.getOptionValue("host");
     fileName = commandLine.getOptionValue("filename");
   }
 
   private void run() throws IOException {
 
     Transceiver transceiver = new NettyTransceiver(new InetSocketAddress(
         hostname, port));
     AvroSourceProtocol client = SpecificRequestor.getClient(
         AvroSourceProtocol.class, transceiver);
     BufferedReader reader = null;
 
     if (fileName != null) {
       reader = new BufferedReader(new FileReader(new File(fileName)));
     } else {
       reader = new BufferedReader(new InputStreamReader(System.in));
     }
 
     String line = null;
     long lastCheck = System.currentTimeMillis();
     long sentBytes = 0;
 
     while ((line = reader.readLine()) != null) {
       // logger.debug("read:{}", line);
 
       AvroFlumeEvent avroEvent = new AvroFlumeEvent();
 
       avroEvent.headers = new HashMap<CharSequence, CharSequence>();
       avroEvent.body = ByteBuffer.wrap(line.getBytes());
       sentBytes += avroEvent.body.capacity();
 
       Status status = client.append(avroEvent);
       sent++;
 
       long now = System.currentTimeMillis();
 
       if (now >= lastCheck + 5000) {
         logger.debug("Sent {} bytes, {} events", sentBytes, sent);
         lastCheck = now;
       }
 
       // logger.debug("Sent:{} Status:{}", ++sent, status);
     }
 
     logger.debug("Finished");
 
     reader.close();
     transceiver.close();
   }
 }
