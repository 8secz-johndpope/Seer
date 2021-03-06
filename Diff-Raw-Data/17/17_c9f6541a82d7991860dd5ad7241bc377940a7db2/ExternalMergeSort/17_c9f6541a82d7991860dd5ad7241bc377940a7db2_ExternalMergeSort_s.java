 package org.geirove.exmeso;
 
 import java.io.BufferedOutputStream;
 import java.io.Closeable;
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.Iterator;
 import java.util.List;
 
 import org.codehaus.jackson.JsonGenerator;
 import org.codehaus.jackson.JsonParseException;
 import org.codehaus.jackson.annotate.JsonProperty;
 import org.codehaus.jackson.map.JsonMappingException;
 import org.codehaus.jackson.map.ObjectMapper;
 
 public class ExternalMergeSort<T> {
 
     private final SortHandler<T> handler;
     private final File tmpdir;
     
     private int chunkSize = 1000;
     private int chunkBytes = 100000000; // 100 Mb
     
     public ExternalMergeSort(SortHandler<T> handler, File tmpdir) {
         this.handler = handler;
         this.tmpdir = tmpdir;
     }
 
     //    public static interface SortReader<T> extends Closeable {
     //        T read() throws IOException;
     //    }
 
     public static interface SortHandler<T> extends Closeable {
         void sortChunk(List<T> values);
         void writeChunk(List<T> values, OutputStream out) throws IOException;
         Chunk<T> readChunk(InputStream input, int chunkSizeHint, int chunkBytesHint);
     }
 
     public static class JacksonSort<T> implements SortHandler<T> {
 
         private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();
         
         private final ObjectMapper mapper;
         private final Comparator<T> comparator;
         private final Class<T> type;
 
         public JacksonSort(Comparator<T> comparator, Class<T> type) {
             this(DEFAULT_MAPPER, comparator, type);
         }
         
         public JacksonSort(ObjectMapper mapper, Comparator<T> comparator, Class<T> type) {
             this.mapper = mapper;
             this.comparator = comparator;
             this.type = type;
             this.mapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
         }
 
         @Override
         public void writeChunk(List<T> values, OutputStream out) throws IOException{
             for (T value : values) {
                 mapper.writeValue(out, value);
             }
         }
 
         @Override
         public void close() throws IOException {
         }
 
         @Override
         public void sortChunk(List<T> values) {
             Collections.sort(values, comparator);
         }
 
         @Override
         public Chunk<T> readChunk(InputStream input, int chunkSize, int chunkBytes) {
            Chunk<T> result = new Chunk<T>(chunkSize);
             try {
                 while (input.read() != -1) {
                     T value = mapper.readValue(input, type);
                     result.add(value);
                 }
             } catch (Exception e) {
                 throw new RuntimeException(e);
             }
             return result;
         }
 
     }
 
     private List<File> sortChunks(InputStream input) throws IOException {
         List<File> result = new ArrayList<File>();
         while (true) {
             Chunk<T> chunk = readChunk(input);
             if (chunk == null) {
                 break;
             }
             File chunkFile = writeChunk(chunk);
             result.add(chunkFile);
         }
         return result;
     }
     
     private List<File> sortChunks(Iterator<T> input) throws IOException {
         List<File> result = new ArrayList<File>();
         while (input.hasNext()) {
             List<T> chunk = readChunk(input);
             File chunkFile = writeChunk(chunk);
             result.add(chunkFile);
         }
         return result;
         
     }
 
     private File writeChunk(List<T> chunk) throws IOException {
         File chunkFile = File.createTempFile("exmeso-", "", tmpdir);
        OutputStream out = new BufferedOutputStream(new FileOutputStream(chunkFile));
         try {
             try {
                 sortAndWriteChunk(chunk, out);
             } finally {
                 handler.close();
             }
         } finally {
             out.close();
         }
         return chunkFile;
     }
 
     private void sortAndWriteChunk(List<T> values, OutputStream out) throws IOException {
         handler.sortChunk(values);
         handler.writeChunk(values, out);
     }
 
     public static class Chunk<T> extends ArrayList<T> {
         private boolean isLast;
         public Chunk() {
         }
         public Chunk(int chunkSize) {
             super(chunkSize);
         }
         public boolean isLast() {
             return isLast;
         }
         public void setLast(boolean isLast) {
             this.isLast = isLast;
         }
     }
     
     private Chunk<T> readChunk(Iterator<T> input) {
        Chunk<T> result = new Chunk<T>(250);
         int c = 0;
         while (input.hasNext()) {
             c++;
             result.add(input.next());
            if (c > 1000) {
                 return result;
             }
         }
         result.setLast(true);
         return result;
     }
 
     private Chunk<T> readChunk(InputStream input) {
         // read chunk; specify size in bytes or documents; configured in handler
         return handler.readChunk(input, chunkSize, chunkBytes);
     }
 
     public static class StringPojo {
         private String value;
         public StringPojo(@JsonProperty("value") String value) {
             this.value = value;
         }
         public String getValue() {
             return value;
         }
     }
 
     public static void main(String[] args) throws Exception {
         // receive iterator of objects and write to file(s) of approxiately specified size
 
         Comparator<StringPojo> comparator = new Comparator<StringPojo>() {
             @Override
             public int compare(StringPojo o1, StringPojo o2) {
                 return o1.getValue().compareTo(o2.getValue());
             }
         };
        JacksonSort<StringPojo> writer = new JacksonSort<StringPojo>(comparator);
         ExternalMergeSort<StringPojo> sort = new ExternalMergeSort<StringPojo>(writer, new File("/tmp"));
 
         // load chunk array from iterator
         List<StringPojo> chunk = Arrays.<StringPojo>asList(new StringPojo("B"), new StringPojo("C"), new StringPojo("A"));
         
         // sort chunk array of objects to file
         OutputStream out = new BufferedOutputStream(new FileOutputStream(new File("/tmp/sort.temp")));
         try {
             try {
                 sort.sortAndWriteChunk(chunk, out);
             } finally {
                 writer.close();
             }
         } finally {
             out.close();
         }
     }
 
 }
