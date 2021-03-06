 package org.wikimedia.diffdb;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.Reader;
 import java.text.SimpleDateFormat;
 import java.util.Date;
 import java.util.Set;
 import java.util.concurrent.BlockingQueue;
 
 import org.apache.commons.lang3.StringEscapeUtils;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.document.Field.Index;
 import org.apache.lucene.document.Field.Store;
 
 public class DiffDocumentProducer implements Runnable {
   private static class Prop {
     // Note that fields which are not stored are not available in documents
     // retrieved from the index
     private final String name;
     private final Store store;
     private final Index index;
     protected Prop() {
       this("", Field.Store.NO, Field.Index.NOT_ANALYZED);
     }
     public Prop(String name, Store store, Index index) {
       this.name = name;
       this.store = store;
       this.index = index;
     }
     public String name() { return this.name; }
     public Store store() { return this.store; }
     public Index index() { return this.index; }
   }
 
 	private static final Prop[] propTypes = new Prop[] {
 		new Prop("rev_id",    Field.Store.YES, Field.Index.NOT_ANALYZED),
 		new Prop("page_id",   Field.Store.YES, Field.Index.NOT_ANALYZED),
 		new Prop("namespace", Field.Store.YES, Field.Index.NOT_ANALYZED),
 		new Prop("title",     Field.Store.YES, Field.Index.ANALYZED),
 		new Prop("timestamp", Field.Store.YES, Field.Index.NOT_ANALYZED),
 		new Prop("comment",   Field.Store.YES, Field.Index.ANALYZED),
 		new Prop("minor",     Field.Store.YES, Field.Index.NOT_ANALYZED),
 		new Prop("user_id",   Field.Store.YES, Field.Index.NOT_ANALYZED),
 		new Prop("user_text", Field.Store.YES, Field.Index.ANALYZED),
 	};
 
 	public static enum Filter {
 		PASS_ALL {
 			@Override
 				public boolean pass(Document doc) {
 				return true;
 			}
 		}, PASS_TALK_NAMESPACE_ONLY {
 			@Override
 				public boolean pass(Document doc) {
 				return Integer.parseInt(doc.getField("namespace").stringValue()) % 2 == 1;
 			}
 		}, PASS_USER_TALK_NAMESPACE_ONLY {
 			@Override
 				public boolean pass(Document doc) {
 				return Integer.parseInt(doc.getField("namespace").stringValue()) == 3;
 			}
 		};
 		public abstract boolean pass(Document doc);
 	};
 
   private final BlockingQueue<Document> prodq;
   private final BlockingQueue<Document> poolq;
   private final BufferedReader reader;
	private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
 	private final Filter filter;
 
   public DiffDocumentProducer(Reader reader, BlockingQueue<Document> prodq, BlockingQueue<Document> poolq, Filter filter) {
     this.prodq = prodq;
     this.poolq = poolq;
     this.reader = new BufferedReader(reader);
 		this.filter = filter;
   }
   public DiffDocumentProducer(Reader reader, BlockingQueue<Document> prodq, BlockingQueue<Document> poolq) {
 		this(reader, prodq, poolq, Filter.PASS_ALL);
 	}
 
 	public int hashCode() {
 		return this.reader.hashCode();
 	}
 
 	public boolean equals(Object o) {
 		if ( o instanceof DiffDocumentProducer ) {
 			DiffDocumentProducer p = (DiffDocumentProducer)o;
 			return this.reader.equals(p.reader);
 		}
 		return false;
 	}
 
   public static  Document createEmptyDocument() {
     final Document doc = new Document();
     // Initialise document
     for (int i = 0; i < propTypes.length; ++i) {
       createField(doc, propTypes[i], "");
     }
     createField(doc, new Prop("added_size",   Field.Store.YES, Field.Index.NOT_ANALYZED), "");
     createField(doc, new Prop("removed_size", Field.Store.YES, Field.Index.NOT_ANALYZED), "");
     createField(doc, new Prop("added",        Field.Store.YES, Field.Index.ANALYZED), "");
     createField(doc, new Prop("removed",      Field.Store.YES, Field.Index.ANALYZED), "");
     createField(doc, new Prop("action",       Field.Store.YES, Field.Index.NOT_ANALYZED), "");
     return doc;
   }
 
 	private static void createField(Document doc, Prop prop, String value) {
 		doc.add(new Field(prop.name(), value, prop.store(), prop.index()));
 	}
 
   public void run() {
     String line;
     int linenumber = 1;
 		final StringBuffer abuff = new StringBuffer("");
 		final StringBuffer rbuff = new StringBuffer("");
     try {
       while ( (line = this.reader.readLine()) != null ) {
         Document doc = this.poolq.take();
         String[] props = line.split("\t");
         for (int i = 0; i < propTypes.length; ++i) {
           Prop proptype = propTypes[i];
           Field field = (Field) doc.getFieldable(proptype.name());
           field.setValue(parseString(props[i]));
         }
         // extract additions and removals and store them in the buffers
         abuff.delete(0, abuff.length());
         rbuff.delete(0, rbuff.length());
         for (int i = propTypes.length; i < props.length; ++i) {
           String[] p;
           try {
             p = parseDiff(props[i]);
           } catch (Exception e) {
             System.err.println(e + ": " + props[0] + ", " + props[i]);
             continue;
           }
           ("-1".equals(p[1]) ? rbuff: abuff).append(p[0] + p[1] +p[2] + "\t");
         }
         ((Field)doc.getFieldable("timestamp")).setValue(formatter.format(new Date(Long.parseLong(props[4])*1000L)));
         ((Field)doc.getFieldable("added")).setValue(abuff.toString());
         ((Field)doc.getFieldable("removed")).setValue(rbuff.toString());
         ((Field)doc.getFieldable("added_size")).setValue("" + abuff.length());
         ((Field)doc.getFieldable("removed_size")).setValue("" + rbuff.length());
 
 				String action = "none";
 				if ( abuff.length() > 0  &&  rbuff.length() > 0 ) {
 					action = "both";
 				} else if ( abuff.length() > 0 ) {
 					action = "addition";
 				} else {
 					action = "removal";
 				}
 					
         ((Field)doc.getFieldable("action")).setValue(action);
 
         if (props.length < propTypes.length) {
           System.err.println("line " + linenumber
                              + ": illegal line format");
         }
 
         ++linenumber;
 				if ( this.filter.pass(doc) ) {
 					this.prodq.put(doc);
 				} else {
 					this.poolq.put(doc);
 				}
       }
 		} catch ( InterruptedException e ) {
 			System.out.println(e);
 		} catch ( IOException e ) {
 			System.out.println(e);
 		} finally {
     }
   }
 
 	public static String parseString(String str) {
 		if ( str.charAt(0) == 'u' && str.charAt(1) == '\'' ) {
 			return str.substring(2, str.length() - 1);
 		} else if ( str.charAt(0) == '\'' ) {
 			return str.substring(1, str.length() - 1);
 		} else {
 			return str;
 		}
 	}
 
 	private static String[] parseDiff(String str) {
 		int i = str.indexOf(":");
 		int j = str.indexOf(":", i+1);
 		if ( j >= str.length() - 3 ) {
 			return new String[]{
 				str.substring(0,i),
 				str.substring(i+1,j),
 				""
 			};
 		} else {
 			return new String[]{
 				str.substring(0,i),
 				str.substring(i+1,j),
 				StringEscapeUtils.unescapeJava(parseString(str.substring(j+1)))
 			};
 		}
 	}
 
 }
 /*
  * Local variables:
  * tab-width: 2
  * c-basic-offset: 2
  * indent-tabs-mode: t
  * End:
  */
