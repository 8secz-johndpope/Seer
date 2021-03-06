 /**
  * 
  */
 package com.dianping.cat.message.consumer.transaction;
 
 import static junit.framework.Assert.assertEquals;
 
 import org.junit.After;
 import org.junit.AfterClass;
 import org.junit.Before;
 import org.junit.BeforeClass;
 import org.junit.Test;
 
 import com.dianping.cat.consumer.model.transaction.entity.TransactionName;
 import com.dianping.cat.consumer.model.transaction.entity.TransactionReport;
 import com.dianping.cat.consumer.model.transaction.entity.TransactionType;
 import com.dianping.cat.message.Message;
 import com.dianping.cat.message.internal.DefaultTransaction;
 import com.dianping.cat.message.spi.MessageTree;
 import com.dianping.cat.message.spi.internal.DefaultMessageTree;
 
 /**
  * @author sean.wang
  * @since Jan 5, 2012
  */
 public class TransactionReportMessageAnalyzerTest {
 
 	/**
 	 * @throws java.lang.Exception
 	 */
 	@BeforeClass
 	public static void setUpBeforeClass() throws Exception {
 	}
 
 	/**
 	 * @throws java.lang.Exception
 	 */
 	@AfterClass
 	public static void tearDownAfterClass() throws Exception {
 	}
 
 	/**
 	 * @throws java.lang.Exception
 	 */
 	@Before
 	public void setUp() throws Exception {
 	}
 
 	/**
 	 * @throws java.lang.Exception
 	 */
 	@After
 	public void tearDown() throws Exception {
 	}
 
 	/**
 	 * Test method for {@link com.dianping.cat.message.consumer.transaction.TransactionReportMessageAnalyzer#process(com.dianping.cat.message.spi.MessageTree)}.
 	 * 
 	 * @throws InterruptedException
 	 */
 	@Test
 	public void testCommonGenerate() throws InterruptedException {
 		TransactionReportMessageAnalyzer analyzer = new TransactionReportMessageAnalyzer();
 
 		for (int i = 1; i <= 1000; i++) {
 			MessageTree tree = new DefaultMessageTree();
 			tree.setMessageId("" + i);
 			tree.setDomain("group");
 			tree.setHostName("group001");
 			tree.setIpAddress("192.168.1.1");
 			DefaultTransaction t = new DefaultTransaction("A", "n1", null);
 
 			DefaultTransaction t2 = new DefaultTransaction("A-1", "n2", null);
 			if (i % 2 == 0) {
 				t2.setStatus("ERROR");
 			} else {
 				t2.setStatus(Message.SUCCESS);
 			}
 			t2.complete();
 			t2.setDuration(i);
 
 			t.addChild(t2);
 			if (i % 2 == 0) {
 				t.setStatus("ERROR");
 			} else {
 				t.setStatus(Message.SUCCESS);
 			}
 			t.complete();
 			t.setDuration(i * 2);
 
 			tree.setMessage(t);
 			analyzer.process(tree);
 		}
 
 		TransactionReport report = analyzer.generate();
 		TransactionType typeA = report.getTypes().get("A");
 		TransactionName n1 = typeA.getNames().get("n1");
		assertEquals(1000, n1.getTotalCount().intValue());
		assertEquals(500, n1.getFailCount().intValue());
		assertEquals(50.0, n1.getFailPercent().doubleValue());
		assertEquals(2.0, n1.getMin().doubleValue());
		assertEquals(2000.0, n1.getMax().doubleValue());
		assertEquals(1001.0, n1.getAvg().doubleValue());
		assertEquals(1001000.0, n1.getSum().doubleValue());
 		assertEquals("999", n1.getSampleSuccessMessageId());
 		assertEquals("1000", n1.getSampleFailMessageId());
 
 		TransactionType typeA1 = report.getTypes().get("A-1");
 		TransactionName n2 = typeA1.getNames().get("n2");
		assertEquals(1000, n2.getTotalCount().intValue());
		assertEquals(500, n2.getFailCount().intValue());
		assertEquals(50.0, n2.getFailPercent().doubleValue());
		assertEquals(1.0, n2.getMin().doubleValue());
		assertEquals(1000.0, n2.getMax().doubleValue());
		assertEquals(500.5, n2.getAvg().doubleValue());
		assertEquals(500500.0, n2.getSum().doubleValue());
 		assertEquals(null, n2.getSampleSuccessMessageId());
 		assertEquals(null, n2.getSampleFailMessageId());
 		System.out.println(report.toString());
 	}
 
 }
