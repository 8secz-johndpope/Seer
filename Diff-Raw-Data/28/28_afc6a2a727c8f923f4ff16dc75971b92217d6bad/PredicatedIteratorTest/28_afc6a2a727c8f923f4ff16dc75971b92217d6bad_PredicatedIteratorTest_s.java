 package uk.co.jezuk.mango;
 
 import junit.framework.*;
 
 public class PredicatedIteratorTest  extends TestCase
 {
   java.util.List list;
 
   public PredicatedIteratorTest(String name) { super(name); }
   public static Test suite() { return new TestSuite(PredicatedIteratorTest.class); }
 
   protected void setUp()
   {
     list = new java.util.ArrayList();
     for(int i = 0; i < 10; ++i)
       list.add(new Integer(i));
   } // setUp
 
    class LessThanFive implements Predicate
     {
	public boolean test(Object obj)
	{
	    Integer i = (Integer)obj;
	    return i.asInt();
	} // test
    }

   public void test1()
   {
    assertEquals(Mango.count(new PredicatedIterator(list.iterator(), new LessThanFive())));
   } // test1
 } // 
