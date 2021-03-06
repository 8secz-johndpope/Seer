 package org.eclipse.xtext.xtend2.maven;
 
 import java.io.File;
 import java.io.IOException;
 
 import junit.framework.Assert;
 
 import org.apache.maven.it.VerificationException;
 import org.apache.maven.it.Verifier;
 import org.apache.maven.it.util.ResourceExtractor;
 import org.junit.Test;
 
 public class Xtend2BatchCompilerPluginIT {
 
 	@Test
 	public void simpleProject() throws Exception {
 		verifyErrorFreeLog("/it/simple");
 	}
 
 	@Test
 	public void pluginPrefix() throws Exception {
 		verifyErrorFreeLog("/it/simple", "xtend2:compile");
 	}
 
 	@Test
 	public void simpleProjectXtend2Tests() throws Exception {
 		verifyErrorFreeLog("/it/withtestsrc");
 	}
 
 	@Test
 	public void aggregation() throws Exception {
 		verifyErrorFreeLog("/it/aggregation");
 	}
 
 	@Test
	public void haltOnErrorsFromJdtCompiler() throws Exception {
 		Verifier verifier = newVerifier("/it/jdtcerrors");
		try {
			verifier.executeGoal("verify");
			Assert.fail("expected org.apache.maven.plugin.MojoExecutionException");
		} catch (Exception e) {
			verifier.verifyTextInLog("2: Couldn't resolve reference to JvmType 'JavaB'.");
			verifier.verifyTextInLog("BUILD FAILURE");
		}
 	}
 
 	@Test
 	public void haltOnXtendValidationErrors() throws Exception {
 		Verifier verifier = newVerifier("/it/xtenderrors");
 		try {
 			verifier.executeGoal("verify");
 			Assert.fail("expected org.apache.maven.plugin.MojoExecutionException");
 		} catch (Exception e) {
 			verifier.verifyTextInLog("3: Superclass must be a class");
 			verifier.verifyTextInLog("BUILD FAILURE");
 		}
 	}

	@Test
	public void continueOnXtendWarnings() throws Exception {
		Verifier verifier = newVerifier("/it/xtendwarnings");
		verifier.executeGoal("verify");
		verifier.verifyTextInLog("3: The import 'java.util.Collections' is never used.");
		verifier.verifyTextInLog("[INFO] BUILD SUCCESS");
	}
 
 	private void verifyErrorFreeLog(String pathToTestProject) throws IOException, VerificationException {
 		verifyErrorFreeLog(pathToTestProject, "verify");
 	}
 
 	private void verifyErrorFreeLog(String pathToTestProject, String goal) throws IOException, VerificationException {
 		Verifier verifier = newVerifier(pathToTestProject);
 		verifier.executeGoal(goal);
 		verifier.verifyErrorFreeLog();
 		verifier.setDebug(true);
 		verifier.resetStreams();
 	}
 
 	private Verifier newVerifier(String pathToTestProject) throws IOException, VerificationException {
 		File testDir = ResourceExtractor.simpleExtractResources(getClass(), pathToTestProject);
 		return new Verifier(testDir.getAbsolutePath());
 	}
 }
