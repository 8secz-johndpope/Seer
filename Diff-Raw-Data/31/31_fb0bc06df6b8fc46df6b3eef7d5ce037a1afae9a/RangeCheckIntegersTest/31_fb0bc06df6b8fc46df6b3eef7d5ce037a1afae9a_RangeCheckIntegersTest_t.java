 package org.drools.analytics;
 
 import java.util.Collection;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.Set;
 
 import org.drools.StatelessSession;
 import org.drools.StatelessSessionResult;
 import org.drools.analytics.dao.AnalyticsDataFactory;
 import org.drools.analytics.dao.AnalyticsResult;
 import org.drools.analytics.report.components.Gap;
 import org.drools.base.RuleNameMatchesAgendaFilter;
 
 /**
  * 
  * @author Toni Rikkola
  * 
  */
 public class RangeCheckIntegersTest extends TestBase {
 
 	public void testSmallerOrEqual() throws Exception {
 		StatelessSession session = getStatelessSession(this.getClass()
 				.getResourceAsStream("rangeChecks/Integers.drl"));
 
 		session
 				.setAgendaFilter(new RuleNameMatchesAgendaFilter(
 						"Range check for integers, if smaller than or equal is missing"));
 
		AnalyticsDataFactory.clearAnalyticsData();
 		Collection<? extends Object> testData = getTestData(this.getClass()
 				.getResourceAsStream("MissingRangesForInts.drl"));
 
 		AnalyticsDataFactory.clearAnalyticsResult();
 		AnalyticsResult result = AnalyticsDataFactory.getAnalyticsResult();
 		session.setGlobal("result", result);
 
 		StatelessSessionResult sessionResult = session
 				.executeWithResults(testData);
 
 		Iterator<Object> iter = sessionResult.iterateObjects();
 
 		Set<String> rulesThatHadErrors = new HashSet<String>();
 		while (iter.hasNext()) {
 			Object o = (Object) iter.next();
 			if (o instanceof Gap) {
 				rulesThatHadErrors.add(((Gap) o).getRuleName());
 			}
 			// System.out.println(o);
 		}
 
 		assertTrue(rulesThatHadErrors.remove("Integer gap rule 4a"));
 		assertTrue(rulesThatHadErrors.remove("Integer gap rule 5a"));
 
 		if (!rulesThatHadErrors.isEmpty()) {
 			for (String string : rulesThatHadErrors) {
 				fail("Rule " + string + " caused an error.");
 			}
 		}
 	}
 
 	public void testGreaterOrEqual() throws Exception {
 		StatelessSession session = getStatelessSession(this.getClass()
 				.getResourceAsStream("rangeChecks/Integers.drl"));
 
 		session
 				.setAgendaFilter(new RuleNameMatchesAgendaFilter(
 						"Range check for integers, if greater than or equal is missing"));
 
		AnalyticsDataFactory.clearAnalyticsData();
 		Collection<? extends Object> testData = getTestData(this.getClass()
 				.getResourceAsStream("MissingRangesForInts.drl"));
 
 		AnalyticsDataFactory.clearAnalyticsResult();
 		AnalyticsResult result = AnalyticsDataFactory.getAnalyticsResult();
 		session.setGlobal("result", result);
 
 		StatelessSessionResult sessionResult = session
 				.executeWithResults(testData);
 
 		Iterator<Object> iter = sessionResult.iterateObjects();
 
 		Set<String> rulesThatHadErrors = new HashSet<String>();
 		while (iter.hasNext()) {
 			Object o = (Object) iter.next();
 			if (o instanceof Gap) {
 				rulesThatHadErrors.add(((Gap) o).getRuleName());
 			}
 			// System.out.println(o);
 		}
 
 		assertTrue(rulesThatHadErrors.remove("Integer gap rule 4b"));
 		assertTrue(rulesThatHadErrors.remove("Integer gap rule 5b"));
 
 		if (!rulesThatHadErrors.isEmpty()) {
 			for (String string : rulesThatHadErrors) {
 				fail("Rule " + string + " caused an error.");
 			}
 		}
 	}
 
 	public void testEqualAndGreaterThan() throws Exception {
 		StatelessSession session = getStatelessSession(this.getClass()
 				.getResourceAsStream("rangeChecks/Integers.drl"));
 
 		session.setAgendaFilter(new RuleNameMatchesAgendaFilter(
 				"Range check for integers, equal and greater than"));
 
		AnalyticsDataFactory.clearAnalyticsData();
 		Collection<? extends Object> testData = getTestData(this.getClass()
 				.getResourceAsStream("MissingRangesForInts.drl"));
 
 		AnalyticsDataFactory.clearAnalyticsResult();
 		AnalyticsResult result = AnalyticsDataFactory.getAnalyticsResult();
 		session.setGlobal("result", result);
 
 		StatelessSessionResult sessionResult = session
 				.executeWithResults(testData);
 
 		Iterator<Object> iter = sessionResult.iterateObjects();
 
 		Set<String> rulesThatHadErrors = new HashSet<String>();
 		while (iter.hasNext()) {
 			Object o = (Object) iter.next();
 			if (o instanceof Gap) {
 				rulesThatHadErrors.add(((Gap) o).getRuleName());
 			}
 			// System.out.println(o);
 		}
 
 		assertTrue(rulesThatHadErrors.remove("Integer gap rule 1"));
 		assertTrue(rulesThatHadErrors.remove("Integer gap rule 7b"));
 		assertTrue(rulesThatHadErrors.remove("Integer gap rule 3"));
 
 		if (!rulesThatHadErrors.isEmpty()) {
 			for (String string : rulesThatHadErrors) {
 				fail("Rule " + string + " caused an error.");
 			}
 		}
 	}
 
 	public void testEqualAndSmallerThan() throws Exception {
 		StatelessSession session = getStatelessSession(this.getClass()
 				.getResourceAsStream("rangeChecks/Integers.drl"));
 
 		session.setAgendaFilter(new RuleNameMatchesAgendaFilter(
 				"Range check for integers, equal and smaller than"));
 
		AnalyticsDataFactory.clearAnalyticsData();
 		Collection<? extends Object> testData = getTestData(this.getClass()
 				.getResourceAsStream("MissingRangesForInts.drl"));
 
 		AnalyticsDataFactory.clearAnalyticsResult();
 		AnalyticsResult result = AnalyticsDataFactory.getAnalyticsResult();
 		session.setGlobal("result", result);
 
 		StatelessSessionResult sessionResult = session
 				.executeWithResults(testData);
 
 		Iterator<Object> iter = sessionResult.iterateObjects();
 
 		Set<String> rulesThatHadErrors = new HashSet<String>();
 		while (iter.hasNext()) {
 			Object o = (Object) iter.next();
 			if (o instanceof Gap) {
 				rulesThatHadErrors.add(((Gap) o).getRuleName());
 			}
 			// System.out.println(o);
 		}
 
 		assertTrue(rulesThatHadErrors.remove("Integer gap rule 1"));
 		assertTrue(rulesThatHadErrors.remove("Integer gap rule 6b"));
 		assertTrue(rulesThatHadErrors.remove("Integer gap rule 2"));
 
 		if (!rulesThatHadErrors.isEmpty()) {
 			for (String string : rulesThatHadErrors) {
 				fail("Rule " + string + " caused an error.");
 			}
 		}
 	}
 }
