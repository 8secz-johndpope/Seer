 package org.wikimedia.diffdb;
 import org.junit.*;
 import java.io.*;
 import java.util.*;
 import org.apache.lucene.index.*;
 import org.apache.lucene.store.*;
 import org.apache.lucene.search.*;
 import org.apache.lucene.document.*;
 
 import org.apache.lucene.util.Version;
 
 import static org.junit.Assert.*;
 
 public class TestIndexer {
   private static abstract class MyHitCollector extends Collector {
     public boolean acceptsDocsOutOfOrder() { return true; }
     public void setNextReader(IndexReader reader, int docBase) {}
     public void setScorer(Scorer scorer) {}
     public abstract void collect(final int docid);
   }
 
   private static File newTempFile(String content) throws IOException {
     File file = File.createTempFile("indexer", ".txt");
     file.deleteOnExit();
     BufferedWriter writer = new BufferedWriter(new FileWriter(file));
     writer.write(content);
     writer.close();
     return file;
   }
 
   private static int findDocument(IndexSearcher searcher, Term queryterm) throws IOException {
     try {
       final int[] found = new int[]{-1};
       try {
         searcher.search(new TermQuery(queryterm), new MyHitCollector(){
             public void collect(final int doc) {
               found[0] = doc;
             }
           });
         return found[0];
       } catch (RuntimeException e) {
         return found[0];
       } finally {
         searcher.close();
       }
     } catch (FileNotFoundException e) {
       return -1;
     }
   }
 
 
   @Test public void smallDocuments() throws IOException, InterruptedException {
     Directory dir = new RAMDirectory();
     IndexWriter writer = new IndexWriter(dir,
                                          new IndexWriterConfig(Version.LUCENE_34,
                                                                new SimpleNGramAnalyzer(3)));
     Indexer indexer = new Indexer(writer, 1, 2, 100);
     indexer.indexDocuments(newTempFile("233192	10	0	'Accessiblecomputing'	980043141	u'*'	False	99	u'RoseParks'	0:1:u'This subject covers\\n\\n* AssistiveTechnology\\n\\n* AccessibleSoftware\\n\\n* AccessibleWeb\\n\\n* LegalIssuesInAccessibleComputing\\n\\n'\n" +
                                        "18201	12	0	'Anarchism'	1014649222	u'Automated conversion'	True	None	u'Conversion script'	9230:1:u'[[talk:Anarchism|'	9252:1:u']]'	9260:1:u'[[Anarchism'	9276:1:u'|/Todo]]'	9292:1:u'talk:'	9304:-1:u'/Talk'	9464:1:u'\\n'\n"));
     indexer.finish();
     assertEquals(2, writer.numDocs());
 
     IndexReader reader = IndexReader.open(dir);
     IndexSearcher searcher = new IndexSearcher(reader);
     assertEquals("Accessiblecomputing", reader.document(findDocument(searcher, new Term("rev_id", "233192"))).get("title"));
     assertEquals("0", reader.document(findDocument(searcher, new Term("rev_id", "18201"))).get("namespace"));
     assertEquals("0", reader.document(findDocument(searcher, new Term("rev_id", "18201"))).get("namespace"));
    assertEquals("Automated conversion", reader.document(findDocument(searcher, new Term("rev_id", "18201"))).get("comment"));
     //System.err.println(reader.document(findDocument(searcher, new Term("rev_id", "18201"))).getFields());
   }
 }
 
 /*
  * Local variables:
  * tab-width: 2
  * c-basic-offset: 2
  * indent-tabs-mode: nil
  * End:
  */
 
