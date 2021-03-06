 /*
  * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
  *
  * This library is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 2.1 of the License, or (at your option) any later version.
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this program; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
  */
 package org.spf4j.stackmonitor;
 
 import com.google.common.base.Charsets;
 import com.google.common.base.Function;
 import java.io.BufferedOutputStream;
 import java.io.BufferedWriter;
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.OutputStreamWriter;
 import java.io.Writer;
 import java.lang.management.ManagementFactory;
 import java.util.Arrays;
 import java.util.List;
 import java.util.Properties;
 import javax.annotation.Nullable;
 import javax.annotation.PreDestroy;
 import javax.annotation.concurrent.GuardedBy;
 import javax.annotation.concurrent.ThreadSafe;
 import javax.management.InstanceAlreadyExistsException;
 import javax.management.InstanceNotFoundException;
 import javax.management.MBeanRegistrationException;
 import javax.management.MalformedObjectNameException;
 import javax.management.NotCompliantMBeanException;
 import javax.management.ObjectName;
 import org.joda.time.format.DateTimeFormatter;
 import org.joda.time.format.ISODateTimeFormat;
 import org.spf4j.base.AbstractRunnable;
 import org.spf4j.stackmonitor.proto.Converter;
 
 /**
  * Utility that allow you to sample what the application is doing. It generates
  * a "Flame Graph" that allows you to quickly see you "heavy" operations.
  *
  * You can use JConsole to control the sampling while your application is
  * running.
  *
  * By using a sampling approach you can choose your overhead. (sampling takes
  * about 0.5 ms, so the default of 10Hz will give you 0.5% overhead)
  *
  * Collection is separated into CPU, WAIT and IO categories. I felt that most
  * important is to see what hogs your CPU because that is where, you can most
  * likely can do something about it.
  *
  * @author zoly
  */
 @ThreadSafe
 public final class Sampler implements SamplerMBean {
 
     private volatile boolean stopped;
     private volatile long sampleTimeMillis;
     private volatile long dumpTimeMillis;
     private volatile boolean isJmxRegistered;
     private final StackCollector stackCollector;
     private final ObjectName name;
     private volatile long lastDumpTime = System.currentTimeMillis();
     
     @GuardedBy("this")
     private Thread samplingThread;
     private final String filePrefix;
 
     public Sampler() {
         this(100, 3600000, new MxStackCollector());
     }
 
     public Sampler(final long sampleTimeMillis) {
         this(sampleTimeMillis, 3600000, new MxStackCollector());
     }
 
     public Sampler(final StackCollector collector) {
         this(100, 3600000, collector);
     }
     
     public Sampler(final long sampleTimeMillis, final long dumpTimeMillis, final StackCollector collector) {
         this(sampleTimeMillis, dumpTimeMillis, collector,
                 System.getProperty("perf.db.folder", System.getProperty("java.io.tmpdir")),
                 System.getProperty("perf.db.name", ManagementFactory.getRuntimeMXBean().getName()));
     }
 
     public Sampler(final long sampleTimeMillis, final long dumpTimeMillis, final StackCollector collector,
             final String dumpFolder, final String dumpFilePrefix) {
         stopped = true;
         this.sampleTimeMillis = sampleTimeMillis;
         this.dumpTimeMillis = dumpTimeMillis;
         this.stackCollector = collector;
         try {
             this.name = new ObjectName("SPF4J:name=StackSampler");
         } catch (MalformedObjectNameException ex) {
             throw new RuntimeException(ex);
         }
         this.isJmxRegistered = false;
         this.filePrefix = dumpFolder + File.separator + dumpFilePrefix;
     }
 
     public void registerJmx()
             throws MalformedObjectNameException, InstanceAlreadyExistsException,
             MBeanRegistrationException, NotCompliantMBeanException {
         ManagementFactory.getPlatformMBeanServer().registerMBean(this, name);
         isJmxRegistered = true;
     }
 
     @Override
     public synchronized void start() {
         if (stopped) {
             stopped = false;
             final long stMillis = sampleTimeMillis;
 
             final long dumpCount = dumpTimeMillis / stMillis;
 
             samplingThread = new Thread(new AbstractRunnable() {
                 
                 @SuppressWarnings("SleepWhileInLoop")
                 @Override
                 public void doRun() throws IOException, InterruptedException {
                     long dumpCounter = 0;
                     while (!stopped) {
                         stackCollector.sample();
                         Thread.sleep(stMillis);
                         dumpCounter++;
                         if (dumpCounter >= dumpCount) {
                             long timeSinceLastDump = System.currentTimeMillis() - lastDumpTime;
                             if (timeSinceLastDump >= dumpTimeMillis) {
                                 dumpCounter = 0;
                                 dumpToFile();
                             } else {
                                 dumpCounter -= (dumpTimeMillis - timeSinceLastDump) / stMillis;
                             }
                         }
                    }
                 }
             }, "Stack Sampling Thread");
             samplingThread.start();
         } else {
             throw new IllegalStateException("Sampling can only be started once");
         }
 
     }
     private static final DateTimeFormatter TS_FORMAT = ISODateTimeFormat.basicDateTimeNoMillis();
 
     public void dumpToFile() throws IOException {
         dumpToFile(null);
     }
     
     /**
      * Dumps the sampled stacks to file.
      * the collected samples are reset
      * @param id - id will be added to file name
      * @throws IOException
      */
     
     public synchronized void dumpToFile(@Nullable final String id) throws IOException {
         final BufferedOutputStream bos = new BufferedOutputStream(
                 new FileOutputStream(filePrefix + "_" + ((id == null) ? "" : id + "_")
                 + TS_FORMAT.print(lastDumpTime) + "_" + TS_FORMAT.print(System.currentTimeMillis()) + ".ssdump"));
         try {
             stackCollector.applyOnSamples(new Function<SampleNode, SampleNode>() {
                 @Override
                 public SampleNode apply(final SampleNode input) {
                     try {
                         Converter.fromSampleNodeToProto(input).writeTo(bos);
                     } catch (IOException ex) {
                         throw new RuntimeException(ex);
                     }
                     return null;
                 }
             });
 
         } finally {
             bos.close();
         }
         lastDumpTime = System.currentTimeMillis();
 
     }
 
     @Override
     public synchronized void generateHtmlMonitorReport(final String fileName, final int chartWidth, final int maxDepth)
             throws IOException {
         dumpToFile();
         final Writer writer
                 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), Charsets.UTF_8));
         try {
             writer.append("<html>");
 
             stackCollector.applyOnSamples(new Function<SampleNode, SampleNode>() {
                 @Override
                 public SampleNode apply(final SampleNode input) {
                     if (input != null) {
                         SampleNode finput = input;
                         try {
                             writer.append("<h1>Total stats</h1>");
                             StackVisualizer.generateHtmlTable(writer, Method.ROOT, finput, chartWidth, maxDepth);
                         } catch (IOException ex) {
                             throw new RuntimeException(ex);
                         }
                     }
                     return input;
                 }
             });
 
 
             stackCollector.applyOnSamples(new Function<SampleNode, SampleNode>() {
                 @Override
                 public SampleNode apply(final SampleNode input) {
                     if (input != null) {
                         SampleNode finput = input.filteredBy(WaitMethodClassifier.INSTANCE);
                         try {
                             writer.append("<h1>CPU stats</h1>");
                             StackVisualizer.generateHtmlTable(writer, Method.ROOT, finput, chartWidth, maxDepth);
                         } catch (IOException ex) {
                             throw new RuntimeException(ex);
                         }
                     }
                     return input;
                 }
             });
 
             writer.append("</html>");
 
         } finally {
             writer.close();
         }
 
     }
 
     @Override
     public synchronized void stop() throws InterruptedException {
        stopped = true;
        samplingThread.join();
     }
 
     @Override
     public long getSampleTimeMillis() {
         return sampleTimeMillis;
     }
 
     @Override
     public void setSampleTimeMillis(final long sampleTimeMillis) {
         this.sampleTimeMillis = sampleTimeMillis;
     }
 
     @Override
     public boolean isStopped() {
         return stopped;
     }
 
     @Override
     public List<String> generate(final Properties props) throws IOException {
         int width = Integer.parseInt(props.getProperty("width", "1200"));
         int maxDepth = Integer.parseInt(props.getProperty("maxDepth", "1200"));
         String fileName = File.createTempFile("stack", ".html").getAbsolutePath();
         generateHtmlMonitorReport(fileName, width, maxDepth);
         return Arrays.asList(fileName);
     }
 
     @Override
     public List<String> getParameters() {
         return Arrays.asList("width");
     }
 
     @Override
     public void clear() {
         stackCollector.clear();
     }
 
     public StackCollector getStackCollector() {
         return stackCollector;
     }
 
     @PreDestroy
     public void dispose() throws InterruptedException, InstanceNotFoundException {
         stop();
         try {
             if (isJmxRegistered) {
                 ManagementFactory.getPlatformMBeanServer().unregisterMBean(name);
             }
         } catch (InstanceNotFoundException ex) {
             throw new RuntimeException(ex);
         } catch (MBeanRegistrationException ex) {
             throw new RuntimeException(ex);
         }
     }
 
     @Override
     public void generateCpuSvg(final String fileName, final int chartWidth, final int maxDepth) throws IOException {
         final Writer writer
                 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), Charsets.UTF_8));
         try {
 
             stackCollector.applyOnSamples(new Function<SampleNode, SampleNode>() {
                 @Override
                 public SampleNode apply(final SampleNode input) {
                     if (input != null) {
                         SampleNode finput = input.filteredBy(WaitMethodClassifier.INSTANCE);
                         try {
                             StackVisualizer.generateSvg(writer, Method.ROOT, finput, 0, 0, chartWidth, maxDepth, "a");
                         } catch (IOException ex) {
                             throw new RuntimeException(ex);
                         }
                     }
                     return input;
                 }
             });
         } finally {
             writer.close();
         }
     }
 
     @Override
     public void generateTotalSvg(final String fileName, final int chartWidth, final int maxDepth) throws IOException {
         final Writer writer
                 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), Charsets.UTF_8));
         try {
 
             stackCollector.applyOnSamples(new Function<SampleNode, SampleNode>() {
                 @Override
                 public SampleNode apply(final SampleNode input) {
                     if (input != null) {
 
                         try {
                             StackVisualizer.generateSvg(writer, Method.ROOT, input, 0, 0, chartWidth, maxDepth, "b");
                         } catch (IOException ex) {
                             throw new RuntimeException(ex);
                         }
                     }
                     return input;
                 }
             });
         } finally {
             writer.close();
         }
     }
 
     @Override
     public void generateSvgHtmlMonitorReport(final String fileName, final int chartWidth, final int maxDepth)
             throws IOException {
         dumpToFile();
         final Writer writer
                 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), Charsets.UTF_8));
         try {
             writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                     + "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.0//EN\""
                     + " \"http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd\">\n");
             writer.append("<html>");
 
 
             stackCollector.applyOnSamples(new Function<SampleNode, SampleNode>() {
                 @Override
                 public SampleNode apply(final SampleNode input) {
                     if (input != null) {
 
                         try {
                             writer.append("<h1>Total stats</h1>");
                             StackVisualizer.generateSvg(writer, Method.ROOT, input, 0, 0, chartWidth, maxDepth, "a");
                         } catch (IOException ex) {
                             throw new RuntimeException(ex);
                         }
 
                     }
                     return input;
                 }
             });
 
 
             stackCollector.applyOnSamples(new Function<SampleNode, SampleNode>() {
                 @Override
                 public SampleNode apply(final SampleNode input) {
                     if (input != null) {
                         SampleNode finput = input.filteredBy(WaitMethodClassifier.INSTANCE);
                         if (finput != null) {
                             try {
                                 writer.append("<h1>CPU stats</h1>");
                                 StackVisualizer.generateSvg(writer, Method.ROOT,
                                         finput, 0, 0, chartWidth, maxDepth, "b");
                             } catch (IOException ex) {
                                 throw new RuntimeException(ex);
                             }
                         }
                     }
                     return input;
                 }
             });
 
 
             writer.append("</html>");
 
         } finally {
             writer.close();
         }
 
     }
 
     @Override
     public long getDumpTimeMillis() {
         return dumpTimeMillis;
     }
 
     @Override
     public void setDumpTimeMillis(final long dumpTimeMillis) {
         this.dumpTimeMillis = dumpTimeMillis;
     }
 }
