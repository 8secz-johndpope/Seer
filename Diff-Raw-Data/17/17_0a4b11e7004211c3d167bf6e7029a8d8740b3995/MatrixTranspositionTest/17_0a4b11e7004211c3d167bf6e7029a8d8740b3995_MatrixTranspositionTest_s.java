 package de.tuberlin.dima.aim3.assignment3;
 
 import com.google.common.base.Preconditions;
 import com.google.common.collect.Maps;
 import com.google.common.io.Closeables;
 import de.tuberlin.dima.aim3.HadoopAndPactTestCase;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.io.IntWritable;
 import org.apache.hadoop.io.SequenceFile;
 import org.junit.Test;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.Map;
 
 import static org.junit.Assert.assertEquals;
 
 
 public class MatrixTranspositionTest extends HadoopAndPactTestCase {
 
   @Test
   public void transpose() throws Exception {
 
     File inputFile = getTestTempFile("lotr.txt");
     File outputDir = getTestTempDir("output");
     outputDir.delete();
 
     writeLines(inputFile,
         "0;One Ring to rule them all,",
         "1;One Ring to find them,",
         "2;One Ring to bring them all",
         "3;and in the darkness bind them");
 
     Configuration conf = new Configuration();
     MatrixTransposition transposition = new MatrixTransposition();
     transposition.setConf(conf);
 
    transposition.run(new String[] { "--input", inputFile.getAbsolutePath(), "--output", outputDir.getAbsolutePath() });
 
     Map<Integer, SparseVector> invertedIndex = readResult(new File(outputDir, "part-r-00000"), conf);
 
     SparseVector rowForRing = invertedIndex.get(Dictionary.indexOf("ring"));
     assertEquals(1, rowForRing.get(0), 0);
     assertEquals(1, rowForRing.get(1), 0);
     assertEquals(1, rowForRing.get(2), 0);
     assertEquals(0, rowForRing.get(3), 0);
 
     SparseVector rowForDarkness = invertedIndex.get(Dictionary.indexOf("darkness"));
     assertEquals(0, rowForRing.get(0), 0);
     assertEquals(0, rowForRing.get(1), 0);
     assertEquals(0, rowForRing.get(2), 0);
     assertEquals(1, rowForRing.get(3), 0);
   }
 
   protected Map<Integer, SparseVector> readResult(File outputFile, Configuration conf) throws IOException {
     Map<Integer, SparseVector> invertedIndex = Maps.newHashMap();
     SequenceFile.Reader reader = null;
     try {
       reader = new SequenceFile.Reader(FileSystem.get(conf), new Path(outputFile.getAbsolutePath()), conf);
 
       IntWritable row = new IntWritable();
       SparseVector vector = new SparseVector();
 
       while (reader.next(row, vector)) {
         invertedIndex.put(new Integer(row.get()), vector.clone());
       }
 
       boolean hasAtLeastOneRow = reader.next(row, vector);
       Preconditions.checkState(hasAtLeastOneRow, "result must have at least one value");
 
       return invertedIndex;
 
     } finally {
       Closeables.closeQuietly(reader);
     }
 
   }
 }
