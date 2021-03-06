 package com.redhat.qe.auto.bugzilla;
 
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import org.apache.xmlrpc.XmlRpcException;
 import org.testng.ISuite;
 import org.testng.ISuiteListener;
 import org.testng.ITestContext;
 import org.testng.ITestResult;
 import org.testng.SkipException;
 import org.testng.internal.IResultListener;
 
 import com.redhat.qe.auto.tcms.AbstractTestProcedureHandler;
 import com.redhat.qe.auto.testng.BzBugDependency;
 import com.redhat.qe.auto.testng.BzChecker;
 
 public class BugzillaTestNGListener implements IResultListener, ISuiteListener{
 
 	private static final String AUTO_VERIFIED = "AutoVerified";
 	private static final String BLOCKED_BY_BUG = "blockedByBug";
 	private static final String VERIFIES_BUG = "verifiesBug";
 	protected static Logger log = Logger.getLogger(BugzillaTestNGListener.class.getName());
 	protected static BzChecker bzChecker = null;
 	protected static Map<Object[], BzBugDependency> bzTests = new HashMap<Object[], BzBugDependency>();
 	protected static HashSet<String> blockingBugs = new HashSet<String>();
 	@Override
 	public void onConfigurationFailure(ITestResult arg0) {
 	}
 
 	@Override
 	public void onConfigurationSkip(ITestResult arg0) {
 	}
 
 	@Override
 	public void onConfigurationSuccess(ITestResult arg0) {
 	}
 
 	@Override
 	public void onFinish(ITestContext arg0) {
 		
 	}
 
 	@Override
 	public void onStart(ITestContext arg0) {
 	}
 
 	@Override
 	public void onTestFailedButWithinSuccessPercentage(ITestResult arg0) {
 	}
 
 	@Override
 	public void onTestFailure(ITestResult arg0) {
 	}
 
 	@Override
 	public void onTestSkipped(ITestResult arg0) {
 	}
 
	
	protected BzBugDependency getBzData(Object[] params){
		for (Object param: params){
			if (param instanceof BzBugDependency) return (BzBugDependency)param;
		}
		return null;
	}
 	@Override
 	public void onTestStart(ITestResult result) {
 		if (result.getStatus() == ITestResult.SKIP) return;  //don't do anything if the test is skipping already
 		
 		/*
 		 * if the test is in a group "blockedByBug-xxxxxx" and the bug is not in
 		 * ON_QA, VERIFIED, RELEASE_PENDING, POST, CLOSED then skip it 
 		 */
 		
 		bzChecker = BzChecker.getInstance();
 	
 		String[] groups = result.getMethod().getGroups();
 		
 		Pattern p = Pattern.compile("[" + VERIFIES_BUG + "|" + BLOCKED_BY_BUG +"]-(\\d+)");
 		for (String group: groups){
 			Matcher m = p.matcher(group);
 			if (m.find()){
 				String number = m.group(1);
 				lookupBugAndSkipIfOpen(number);
 			}			
 		}
 		//if nothing found, check the param list (if there is one) for certain types
 		Object[] params = result.getParameters();
		BzBugDependency bbb = getBzData(params);
		if (bbb != null){
 			String[] bugIds = bbb.getBugIds();
 			for (String bugId: bugIds) {
 				lookupBugAndSkipIfOpen(bugId);
 			}
 		}
 	}
 
 	protected void lookupBugAndSkipIfOpen(String bugId){
 		BzChecker.bzState state;
 		String summary;
 		boolean isBugOpen;
 		try {
 			state = bzChecker.getBugState(bugId);
 			summary = bzChecker.getBugField(bugId, "summary").toString();
 			isBugOpen = bzChecker.isBugOpen(bugId);
 		} catch(XmlRpcException xre) {
 			log.log(Level.WARNING, "Could not determine the state of Bugzilla bug "+bugId+". Assuming test needs to be run.", xre);
 			return;
 		}
 		
 		// throw a skip exception when the bug is open
 		if (isBugOpen) {
 			//add bug to list of blockers
 			blockingBugs.add(bugId);
 			throw new SkipException("This test is blocked by "+state.toString()+" Bugzilla bug '"+ summary +"'.  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");
 		}
 		else log.log(Level.INFO, "This test was previously blocked by "+state.toString()+" Bugzilla bug '"+ summary +"'.  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");
 
 	}
 
 	
 	@Override
 	public void onTestSuccess(ITestResult result) {
 		bzChecker = BzChecker.getInstance();
 		
 		//FIXME this method needs some work
 		
 		//if the test is in a group "verifiesBug-xxxxxx" and the bug is in ON_QA, close it
 		String[] groups = result.getMethod().getGroups();
 		Pattern p = Pattern.compile("[" + VERIFIES_BUG + "|" + BLOCKED_BY_BUG +"]-(\\d+)");
 		for (String group: groups){
 			Matcher m = p.matcher(group);
 			if (m.find()){
 				String number = m.group(1);
 				BzChecker.bzState state;
 				try {
 					state = bzChecker.getBugState(number);
 				}
 				catch(XmlRpcException xre) {
 					log.log(Level.WARNING, "Could not determine the state of bug " + number + ". It may need to be closed if is hasn't been already.", xre);
 					break;
 				}
 				if (group.startsWith(VERIFIES_BUG)) {
 					log.fine("This test verifies bugzilla bug #"+ number);
 					if (state.equals(BzChecker.bzState.ON_QA)){
 						//TODO need to call code here to actually close the bug (doesn't work yet)
 						log.warning("Need to verify bug " + number + "!");
 						/*
 						 * not ready to start auto-closing yet
 						 * bzChecker.setBugState(number, BzChecker.bzState.VERIFIED);
 						 * log.info("Verified bug " + number);
 						 **/
 						verifyComment(result, number);
 					}
 					else log.warning("Bug " + number + " has been verified, but it is in " + state + " state instead of ON_QA");
 				}
 				else { //blockedByBug
 					log.warning("Test is now unblocked by bug " + number + ".");
 				}
 			}
 		}
 		BzBugDependency blockedOrVerifiedBy = bzTests.get(result.getParameters());
 		if (blockedOrVerifiedBy != null){
 			log.warning("Bugs that were previously blocking this test: " + Arrays.deepToString(blockedOrVerifiedBy.getBugIds()) + ".");			
 			if (blockedOrVerifiedBy.getType().equals(BzBugDependency.Type.Verifies)){
 				verifyComment(result, blockedOrVerifiedBy.getBugIds());
 			}
 		}
 	}
 	
 	@Override
 	public void onFinish(ISuite arg0) {
 		//https://bugzilla.redhat.com/buglist.cgi?bug_id=580127,538160,546399,546397,562302,544353,535788,535806,535576,556928,535327,568917,569563
 		if (blockingBugs.size() > 0) {
 			Iterator<String> it = blockingBugs.iterator();
 			StringBuffer sb = new StringBuffer("https://bugzilla.redhat.com/buglist.cgi?bug_id=");
 			while (it.hasNext()){
 				sb.append(it.next());
 				if (it.hasNext()) sb.append(",");
 			}
 			log.log(Level.INFO, String.format("There were %d bugs blocking tests in this run: %s", blockingBugs.size(), sb.toString()));
 		}
 	}
 
 	@Override
 	public void onStart(ISuite arg0) {
 		// TODO Auto-generated method stub
 		
 	}
 
 	
 	protected void verifyComment(ITestResult result, String... bugNumbers){
 		for (String bugNumber: bugNumbers) {
 			//first check if this has been tested already.  if so, do nothing
 			try {
 				if (bzChecker.getBugField(bugNumber, "keywords").toString().indexOf(AUTO_VERIFIED) != -1 ) {
 					log.info("Bug " + bugNumber + " already has the AutoVerified keyword.");
 					return;
 				}
 			} catch(Exception e) {
 				log.log(Level.WARNING, "Could not determine if bug " + bugNumber + " has been marked AutoVerified yet.",e);
 			}
 			StringBuffer sb = new StringBuffer();
 			bzChecker.login(System.getProperty("bugzilla.login"), System.getProperty("bugzilla.password"));
 			sb.append("Verified by Automated Test " + result.getName() + " parameters: (" + Arrays.deepToString(result.getParameters()) + ")\n");
 			String log = AbstractTestProcedureHandler.getActiveLog();
 			if (log != null)
 			sb.append("Automation log:\n");
 			sb.append(AbstractTestProcedureHandler.getActiveLog());
 			bzChecker.addKeywords(bugNumber, AUTO_VERIFIED);
 			bzChecker.addComment(bugNumber, sb.toString());
 		}
 		
 		bzTests.remove(result.getParameters());
 	}
 	
 	public static void main (String... args) {
 		Pattern p = Pattern.compile("[verifiesBug|blockedByBug]-(\\d+)");
 		Matcher m = p.matcher("blockedByBug-12354542");
 		m.find();
 		System.out.println(m.group(1));
 	}
 
 }
