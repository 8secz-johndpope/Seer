 package org.basex.test.query;
 
 import static org.junit.Assert.*;
 import org.basex.core.BaseXException;
 import org.basex.core.Context;
 import org.basex.core.Prop;
 import org.basex.core.cmd.CreateDB;
 import org.basex.core.cmd.DropDB;
 import org.basex.core.cmd.Set;
 import org.basex.core.cmd.XQuery;
 import org.junit.AfterClass;
 import org.junit.Before;
 import org.junit.BeforeClass;
 import org.junit.Test;
 
 /**
  * This class tests namespaces.
  *
  * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
  * @author Christian Gruen
  */
 public class NamespaceTest {
   /** Database context. */
   private static Context context;
 
   /** Test documents. */
   private static String[][] docs = {
     { "d1", "<x/>" },
     { "d2", "<x xmlns='xx'/>" },
     { "d3", "<a:x xmlns:a='aa'><b:y xmlns:b='bb'/></a:x>" },
     { "d4", "<a:x xmlns:a='aa'><a:y xmlns:b='bb'/></a:x>" },
     { "d5", "<a:x xmlns:a='aa'/>" },
     { "d6", "<a:x xmlns='xx' xmlns:a='aa'><a:y xmlns:b='bb'/></a:x>" },
     { "d7", "<x xmlns='xx'><y/></x>" },
    { "d8", "<a><b xmlns='B'/><c/></a>" }
   };
 
 //  /** Test query. */
 //  @Test
 //  public final void simpleNsDuplicate() {
 //    query(
 //        "declare namespace a='aa'; insert node <a:y xmlns:a='aa'/>" +
 //        "into doc('d5')/a:x",
 //        "declare namespace a='aa';doc('d5')/a:x",
 //        "<a:x xlmns:a='aa'><a:y/></a:x>");
 //  }
 //
 //  /** Test query. */
 //  @Test
 //  public final void simpleNsDuplicate2() {
 //    query(
 //      "declare namespace a='aa'; insert node <a:y xmlns:a='aa'><a:b/></a:y> "+
 //      "into doc('d5')/a:x",
 //      "declare namespace a='aa';doc('d5')/a:x",
 //      "<a:x xlmns:a='aa'><a:y><a:b/></a:y></a:x>");
 //  }
 //
 //  /** Test query. */
 //  @Test
 //  public final void copy6() {
 //    query(
 //        "copy $c := <a:y xmlns:a='aa'><a:b/></a:y> modify () return $c",
 //        "<a:y xmlns:a='aa'><a:b/></a:y>");
 //  }
 
   /** Test query. */
   @Test
   public final void copy1() {
     query(
         "copy $c := <x:a xmlns:x='xx'><b/></x:a>/b modify () return $c",
         "<b xmlns:x='xx'/>");
   }
 
   /** Test query.
    * Detects corrupt namespace hierarchy.
    */
   @Test
   public final void copy2() {
     query(
         "declare namespace a='aa'; copy $c:=doc('d4') modify () return $c//a:y",
         "<a:y xmlns:b='bb' xmlns:a='aa'/>");
   }
 
   /** Test query.
    * Detects missing prefix declaration.
    */
   @Test
   public final void copy3() {
     query(
         "declare namespace a='aa'; copy $c:=doc('d4')//a:y " +
         "modify () return $c",
         "<a:y xmlns:b='bb' xmlns:a='aa'/>");
   }
 
   /** Test query.
    * Detects duplicate namespace declaration in MemData instance.
    */
   @Test
   public final void copy4() {
     query(
         "copy $c := <a xmlns='test'><b><c/></b><d/></a> " +
         "modify () return $c",
         "<a xmlns='test'><b><c/></b><d/></a>");
   }
 
   /** Test query.
    *  Detects bogus namespace after insert.
    */
   @Test
   public final void bogusDetector() {
     query(
         "insert node <a xmlns='test'><b><c/></b><d/></a> into doc('d1')/x",
         "declare namespace na = 'test';doc('d1')/x/na:a",
         "<a xmlns='test'><b><c/></b><d/></a>");
   }
 
   /** Test query.
    * Detects empty default namespace in serializer.
    */
   @Test
   public final void emptyDefaultNamespace() {
     query("<ns:x xmlns:ns='X'><y/></ns:x>",
         "<ns:x xmlns:ns='X'><y/></ns:x>");
   }
 
   /** Test query.
    * Detects duplicate default namespace in serializer.
    */
   @Test
   public final void duplicateDefaultNamespace() {
     query("<ns:x xmlns:ns='X'><y/></ns:x>",
         "<ns:x xmlns:ns='X'><y/></ns:x>");
   }
 
   /** Test query. */
   @Test
   public final void copy5() {
     query(
         "copy $c := <n><a:y xmlns:a='aa'/><a:y xmlns:a='aa'/></n> " +
         "modify () return $c",
     "<n><a:y xmlns:a='aa'/><a:y xmlns:a='aa'/></n>");
   }
 
   /** Test query. */
   @Test
   public final void insertD2intoD1() {
     query(
         "insert node doc('d2') into doc('d1')/x",
         "doc('d1')",
         "<x><x xmlns='xx'/></x>");
   }
 
   /** Test query. */
   @Test
   public final void insertD3intoD1() {
     query(
         "insert node doc('d3') into doc('d1')/x",
         "doc('d1')/x/*",
         "<a:x xmlns:a='aa'><b:y xmlns:b='bb'/></a:x>");
   }
 
   /** Test query. */
   @Test
   public final void insertD3intoD1b() {
     query(
         "insert node doc('d3') into doc('d1')/x",
         "doc('d1')/x/*/*",
         "<b:y xmlns:b='bb' xmlns:a='aa'/>");
   }
 
   /** Test query.
    * Detects missing prefix declaration.
    */
   @Test
   public final void insertD4intoD1() {
     query(
         "declare namespace a='aa'; insert node doc('d4')/a:x/a:y " +
         "into doc('d1')/x",
         "doc('d1')/x",
         "<x><a:y xmlns:a='aa' xmlns:b='bb'/></x>");
   }
 
   /** Test query.
    * Detects duplicate prefix declaration at pre=0 in MemData instance after
    * insert.
    * Though result correct, prefix
    * a is declared twice. -> Solution?
    */
   @Test
   public final void insertD4intoD5() {
     query(
         "declare namespace a='aa';insert node doc('d4')//a:y " +
         "into doc('d5')/a:x",
         "declare namespace a='aa';doc('d5')//a:y",
         "<a:y xmlns:b='bb' xmlns:a='aa'/>");
   }
 
   //  /** Test query.
 //   * Detects duplicate prefix declarations among the insertion nodes (MemData)
 //   * and the target node's data instance.
 //   */
 //  @Test
 //  public final void insertD4intoD3() {
 //    query(
 //        "declare namespace b='bb';insert node doc('d4') into doc('d3')//b:y",
 //        "declare namespace a='aa';doc('d3')/a:x",
 //        "<a:x xmlns:a='aa'><b:y xmlns:b='bb'><a:x><a:y/></a:x></b:y></a:x>");
 //  }
 
   /** Test query.
    * Detects duplicate namespace declarations in MemData instance.
    */
   @Test
   public final void insertD7intoD1() {
     query(
         "declare namespace x='xx';insert node doc('d7')/x:x into doc('d1')/x",
         "doc('d1')/x",
         "<x><x xmlns='xx'><y/></x></x>");
   }
 
 //  /** Test query.
 //   * Detects duplicate namespace declarations after insert.
 //   */
 //  @Test
 //  public final void insertD2intoD6() {
 //    query(
 //        "declare namespace ns='xx';declare namespace a='aa';" +
 //        "insert node doc('d2')/ns:x into doc('d6')/a:x",
 //        "declare namespace a='aa';doc('d6')/a:x",
 //        "<a:x xmlns='xx' xmlns:a='aa'><a:y xmlns:b='bb'/><x/></a:x>");
 //  }
 
   /** Test query.
    * Detects general problems with namespace references.
    */
   @Test
   public final void insertD6intoD4() {
     query(
         "declare namespace a='aa';insert node doc('d6') into doc('d4')/a:x",
         "declare namespace a='aa';doc('d4')/a:x/a:y",
         "<a:y xmlns:b='bb' xmlns:a='aa'/>");
   }
 
   /** Test query.
    * Detects wrong namespace references.
    */
   @Test
   public final void uriStack() {
     query(
         "doc('d8')",
         "<a><b xmlns='B'/><c/></a>");
   }
 
   /**
    * Creates the database context.
    * @throws BaseXException database exception
    */
   @BeforeClass
   public static void start() throws BaseXException {
     context = new Context();
     // turn off pretty printing
     new Set(Prop.SERIALIZER, "indent=no").execute(context);
   }
 
   /**
    * Creates all test databases.
    * @throws BaseXException database exception
    */
   @Before
   public void startTest() throws BaseXException {
     // create all test databases
     for(final String[] doc : docs) {
       new CreateDB(doc[0], doc[1]).execute(context);
     }
   }
   /**
    * Removes test databases and closes the database context.
    * @throws BaseXException database exception
    */
   @AfterClass
   public static void finish() throws BaseXException {
     // drop all test databases
     for(final String[] doc : docs) {
       new DropDB(doc[0]).execute(context);
     }
     context.close();
   }
 
   /**
    * Runs a query and matches the result against the expected output.
    * @param query query
    * @param expected expected output
    */
   private void query(final String query, final String expected) {
     query(null, query, expected);
   }
 
   /**
    * Runs an updating query and matches the result of the second query
    * against the expected output.
    * @param first first query
    * @param second second query
    * @param expected expected output
    */
   private void query(final String first, final String second,
       final String expected) {
 
     try {
       if(first != null) new XQuery(first).execute(context);
       final String result = new XQuery(second).execute(context);
 
       // quotes are replaced by apostrophes to simplify comparison
       final String res = result.replaceAll("\\\"", "'");
       final String exp = expected.replaceAll("\\\"", "'");
       if(!exp.equals(res)) fail("\n" + res + "\n" + exp + " expected");
     } catch(final BaseXException ex) {
       fail(ex.getMessage());
     }
   }
 }
