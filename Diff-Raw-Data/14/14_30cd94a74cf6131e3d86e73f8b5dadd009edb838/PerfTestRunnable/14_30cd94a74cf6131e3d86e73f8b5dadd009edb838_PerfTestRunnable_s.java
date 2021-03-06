 /*
  * Copyright (C) 2012 - 2012 NHN Corporation
  * All rights reserved.
  *
  * This file is part of The nGrinder software distribution. Refer to
  * the file LICENSE which is part of The nGrinder distribution for
  * licensing details. The nGrinder distribution is available on the
  * Internet at http://nhnopensource.org/ngrinder
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
  * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
  * COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
  * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
  * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
  * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
  * OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 package org.ngrinder.perftest.service;
 
 import static org.ngrinder.model.Status.CANCELED;
 import static org.ngrinder.model.Status.DISTRIBUTE_FILES;
 import static org.ngrinder.model.Status.DISTRIBUTE_FILES_FINISHED;
 import static org.ngrinder.model.Status.START_AGENTS;
 import static org.ngrinder.model.Status.START_AGENTS_FINISHED;
 import static org.ngrinder.model.Status.START_CONSOLE;
 import static org.ngrinder.model.Status.START_TESTING;
 import static org.ngrinder.model.Status.TESTING;
 
 import java.util.Date;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 
 import net.grinder.SingleConsole;
 import net.grinder.SingleConsole.ConsoleShutdownListener;
 import net.grinder.StopReason;
 import net.grinder.common.GrinderProperties;
 import net.grinder.console.model.ConsoleProperties;
 
 import org.ngrinder.agent.model.AgentInfo;
 import org.ngrinder.chart.service.MonitorAgentService;
 import org.ngrinder.common.constant.NGrinderConstants;
import org.ngrinder.common.util.DateUtil;
 import org.ngrinder.extension.OnTestStartRunnable;
 import org.ngrinder.infra.config.Config;
 import org.ngrinder.infra.plugin.PluginManager;
 import org.ngrinder.model.PerfTest;
 import org.ngrinder.model.Status;
 import org.ngrinder.monitor.MonitorConstants;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.scheduling.annotation.Scheduled;
 import org.springframework.stereotype.Component;
 
 /**
  * {@link PerfTest} test running run scheduler.
  * 
  * This class is responsible to execute the performance test which is ready to execute. Mostly this
  * class is started from {@link #startTest()} method. This method is scheduled by Spring Task.
  * 
  * @author JunHo Yoon
  * @since 3.0
  */
 @Component
 public class PerfTestRunnable implements NGrinderConstants {
 
 	private static final Logger LOG = LoggerFactory.getLogger(PerfTestRunnable.class);
 
 	@Autowired
 	private PerfTestService perfTestService;
 
 	@Autowired
 	private ConsoleManager consoleManager;
 
 	@Autowired
 	private AgentManager agentManager;
 
 	@Autowired
 	private MonitorAgentService monitorDataService;
 
 	@Autowired
 	private PluginManager pluginManager;
 
 	@Autowired
 	private Config config;
 
 	/**
 	 * Scheduled method for test execution. This method dispatches the test candidates and run one
 	 * of them. This method is responsible until a test is executed.
 	 */
 	@Scheduled(fixedDelay = PERFTEST_RUN_FREQUENCY_MILLISECONDS)
 	public void startTest() {
 		// Block if the count of testing exceed the limit
 		if (!perfTestService.canExecuteTestMore()) {
 			// LOG MORE
 			List<PerfTest> currentlyRunningTests = perfTestService.getCurrentlyRunningTest();
 			LOG.debug("current running test is {}. so no tests start to run", currentlyRunningTests.size());
 			for (PerfTest perfTest : currentlyRunningTests) {
 				LOG.trace("- " + perfTest);
 			}
 			return;
 		}
 		// Find out next ready perftest
 		PerfTest runCandidate = perfTestService.getPerfTestCandiate();
 		if (runCandidate == null) {
 			return;
 		}
 
 		// schedule test
 		Date schedule = runCandidate.getScheduledTime();
		if (schedule != null && !DateUtil.compareDateEndWithMinute(schedule, new Date(System.currentTimeMillis()))) {
 			// this test project is reserved,but it isn't yet going to run test
 			// right now.
 			return;
 		}
 
 		// If agent is not enough...
 		int size = agentManager.getAllFreeApprovedAgents().size();
 		if (runCandidate.getAgentCount() > size) {
 			perfTestService.markProgress(runCandidate,
 							"The test is tried to execute but there is not enough free agents." +
 							"\n- Current free agent size : " 
 									+ size + "  / Requested : " + runCandidate.getAgentCount() + "\n");
 			return;
 		}
 
 		doTest(runCandidate);
 	}
 
 	/**
 	 * Run given test.
 	 * 
 	 * If fails, it marks STOP_ON_ERROR in the given {@link PerfTest} status
 	 * 
 	 * @param perfTest
 	 *            perftest instance;
 	 */
 	public void doTest(PerfTest perfTest) {
 		SingleConsole singleConsole = null;
 		try {
 			singleConsole = startConsole(perfTest);
 			prepareFiles(perfTest);
 			GrinderProperties grinderProperties = perfTestService.getGrinderProperties(perfTest);
 			startAgentsOn(perfTest, grinderProperties, singleConsole);
 			distributeFileOn(perfTest, grinderProperties, singleConsole);
 			singleConsole.setReportPath(perfTestService.getReportFileDirectory(perfTest.getId()));
 			runTestOn(perfTest, grinderProperties, singleConsole);
 		} catch (Exception e) {
 			// In case of error, mark the occurs error on perftest.
 			LOG.error("Error while excuting test", e);
 			perfTestService.markPerfTestError(perfTest, e.getMessage());
 		}
 	}
 
 	/**
 	 * Start console.
 	 * 
 	 * @param perfTest
 	 *            perftest
 	 * @return started console console
 	 */
 	SingleConsole startConsole(PerfTest perfTest) {
 		perfTestService.changePerfTestStatus(perfTest, START_CONSOLE, "Console is being prepared.");
 		// get available consoles.
 		ConsoleProperties consoleProperty = perfTestService.createConsoleProperties(perfTest);
 		SingleConsole singleConsole = consoleManager.getAvailableConsole(perfTest.getTestIdentifier(), consoleProperty);
 		singleConsole.start();
 		perfTestService.markPerfTestConsoleStart(perfTest, singleConsole.getConsolePort());
 		return singleConsole;
 	}
 
 	/**
 	 * Prepare files to be distributed.
 	 * 
 	 * @param perfTest
 	 *            perftest
 	 */
 	void prepareFiles(PerfTest perfTest) {
 		// Prepare the files before the grinder properties are configured
 		perfTestService.prepareDistribution(perfTest);
 	}
 
 	/**
 	 * Distribute files.
 	 * 
 	 * @param perfTest
 	 *            perftest
 	 * @param grinderProperties
 	 *            grinder properties
 	 * @param singleConsole
 	 *            console
 	 */
 	void distributeFileOn(PerfTest perfTest, GrinderProperties grinderProperties, SingleConsole singleConsole) {
 		// Distribute files
 		perfTestService.changePerfTestStatus(perfTest, DISTRIBUTE_FILES, "All necessary files are distributing.");
 		// the files have prepared before
 		singleConsole.distributeFiles(perfTestService.getPerfTestDirectory(perfTest));
 		perfTestService.changePerfTestStatus(perfTest, DISTRIBUTE_FILES_FINISHED,
 						"All necessary files are distributed.");
 	}
 
 	/**
 	 * Start agents.
 	 * 
 	 * @param perfTest
 	 *            perftest
 	 * @param grinderProperties
 	 *            grinder properties
 	 * @param singleConsole
 	 *            console
 	 */
 	void startAgentsOn(PerfTest perfTest, GrinderProperties grinderProperties, SingleConsole singleConsole) {
 		perfTestService.changePerfTestStatus(
 				perfTest, START_AGENTS, perfTest.getAgentCount() + " agents are starting.");
 		agentManager.runAgent(singleConsole, grinderProperties, perfTest.getAgentCount());
 		singleConsole.waitUntilAgentConnected(perfTest.getAgentCount());
 		perfTestService.changePerfTestStatus(perfTest, START_AGENTS_FINISHED, perfTest.getAgentCount()
 						+ " agents are started.");
 	}
 
 	/**
 	 * Run a test with given {@link GrinderProperties} and {@link SingleConsole} .
 	 * 
 	 * @param perfTest
 	 *            perftest
 	 * @param grinderProperties
 	 *            the grinder information
 	 * @param singleConsole
 	 *            console to be used.
 	 */
 	void runTestOn(final PerfTest perfTest, GrinderProperties grinderProperties, final SingleConsole singleConsole) {
 		// start target monitor
 
 		for (OnTestStartRunnable run : pluginManager.getEnabledModulesByClass(OnTestStartRunnable.class)) {
 			run.start(perfTest, perfTestService, config.getVesion());
 		}
 
 		Set<AgentInfo> agents = new HashSet<AgentInfo>();
 		List<String> targetIPList = perfTest.getTargetHostIP();
 		for (String targetIP : targetIPList) {
 			AgentInfo targetServer = new AgentInfo();
 			targetServer.setIp(targetIP);
 			targetServer.setPort(MonitorConstants.DEFAULT_MONITOR_PORT);
 			agents.add(targetServer);
 		}
 		// use perf test id as key for the set of target server.
 		monitorDataService.addMonitor("PerfTest-" + perfTest.getId(), agents);
 
 		// Run test
 		perfTestService.changePerfTestStatus(perfTest, START_TESTING, "Now the test is ready to start.");
 		// Add listener to detect abnormal condition and mark the perfTest
 		singleConsole.addListener(new ConsoleShutdownListener() {
 			@Override
 			public void readyToStop(StopReason stopReason) {
 				perfTestService.markAbromalTermination(perfTest, stopReason);
 				LOG.error("Abnormal test {} by {}", perfTest.getId(), stopReason.name());
 			}
 		});
 		singleConsole.startSampling(grinderProperties.getInt(GRINDER_PROP_IGNORE_SAMPLE_COUNT, 0));
 		long startTime = singleConsole.startTest(grinderProperties);
 		perfTestService.setRecodingStarting(perfTest, startTime);
 		perfTestService.changePerfTestStatus(perfTest, TESTING, "The test is started.");
 	}
 
 	/**
 	 * Run plugins' test finish methods.
 	 * 
 	 * @param perfTest
 	 *            PerfTest
 	 * @param reason
 	 *            the reason of test finish..
 	 * @see OnTestStartRunnable
 	 */
 	public void notifyFinsish(PerfTest perfTest, StopReason reason) {
 		for (OnTestStartRunnable run : pluginManager.getEnabledModulesByClass(OnTestStartRunnable.class)) {
 			run.finish(perfTest, reason.name(), perfTestService, config.getVesion());
 		}
 	}
 
 	/**
 	 * Scheduled method for test finish. There are three types of finish. <br/>
 	 * <ul>
 	 * <li>Abnormal test finish : when TPS is too low or too many errors occurs</li>
 	 * <li>User requested test finish : when user requested to finish test from the UI</li>
 	 * <li>Normal test finish : when test goes over the planned duration and run count.</li>
 	 * </ul>
 	 */
 	@Scheduled(fixedDelay = PERFTEST_RUN_FREQUENCY_MILLISECONDS)
 	public void finishTest() {
 		for (PerfTest each : perfTestService.getAbnoramlTestingPerfTest()) {
 			LOG.error("Terminate " + each.getId());
 			SingleConsole consoleUsingPort = consoleManager.getConsoleUsingPort(each.getPort());
 			doTerminate(each, consoleUsingPort);
 			notifyFinsish(each, StopReason.TOO_MANY_ERRORS);
 		}
 
 		for (PerfTest each : perfTestService.getStopRequestedPerfTest()) {
 			LOG.error("Stop " + each.getId());
 			SingleConsole consoleUsingPort = consoleManager.getConsoleUsingPort(each.getPort());
 			doStop(each, consoleUsingPort);
 			notifyFinsish(each, StopReason.STOP_BY_USER);
 		}
 
 		for (PerfTest each : perfTestService.getTestingPerfTest()) {
 			SingleConsole consoleUsingPort = consoleManager.getConsoleUsingPort(each.getPort());
 			if (isTestFinishCandidate(each, consoleUsingPort)) {
 				doFinish(each, consoleUsingPort);
 				notifyFinsish(each, StopReason.NORMAL);
 			}
 		}
 	}
 
 	/**
 	 * Check this test is ready to finish.
 	 * 
 	 * @param perfTest
 	 *            perf test
 	 * @param singleConsoleInUse
 	 *            singleConsole
 	 * @return true if it's a candiate.
 	 */
 	private boolean isTestFinishCandidate(PerfTest perfTest, SingleConsole singleConsoleInUse) {
 		// Give 5 seconds to be finished
 		if ("D".equals(perfTest.getThreshold())
 						&& singleConsoleInUse.getCurrentRunningTime() > (perfTest.getDuration() + 5000)) {
 			LOG.debug("Test {} is ready to Finish. Current : {}, Planned : {}",
 							new Object[] { perfTest.getTestIdentifier(), singleConsoleInUse.getCurrentRunningTime(),
 									perfTest.getDuration() });
 			return true;
 		} else if ("R".equals(perfTest.getThreshold())
 						&& singleConsoleInUse.getCurrentTestsCount() >= perfTest.getTotalRunCount()) {
 			LOG.debug("Test {} is ready to Finish. Current : {}, Planned : {}",
 							new Object[] { perfTest.getTestIdentifier(), singleConsoleInUse.getCurrentTestsCount(),
 									perfTest.getTotalRunCount() });
 			return true;
 		}
 		return false;
 	}
 
 	/**
 	 * Terminate test.
 	 * 
 	 * @param perfTest
 	 *            {@link PerfTest} to be finished
 	 * @param singleConsoleInUse
 	 *            {@link SingleConsole} which is being using for {@link PerfTest}
 	 */
 	public void doStop(PerfTest perfTest, SingleConsole singleConsoleInUse) {
 		perfTestService.markProgressAndStatusAndFinishTimeAndStatistics(perfTest, CANCELED, "Stop requested by user");
 		removeMonitorTargets(perfTest);
 		consoleManager.returnBackConsole(perfTest.getTestIdentifier(), singleConsoleInUse);
 	}
 
 	/**
 	 * Terminate test.
 	 * 
 	 * @param perfTest
 	 *            {@link PerfTest} to be finished
 	 * @param singleConsoleInUse
 	 *            {@link SingleConsole} which is being using for {@link PerfTest}
 	 */
 	public void doTerminate(PerfTest perfTest, SingleConsole singleConsoleInUse) {
 		perfTestService.markProgressAndStatusAndFinishTimeAndStatistics(perfTest, Status.STOP_ON_ERROR, "Stoped by error");
 		removeMonitorTargets(perfTest);
 		consoleManager.returnBackConsole(perfTest.getTestIdentifier(), singleConsoleInUse);
 	}
 
 	/**
 	 * Finish test.
 	 * 
 	 * @param perfTest
 	 *            {@link PerfTest} to be finished
 	 * @param singleConsoleInUse
 	 *            {@link SingleConsole} which is being using for {@link PerfTest}
 	 */
 	public void doFinish(PerfTest perfTest, SingleConsole singleConsoleInUse) {
 		// FIXME... it should found abnormal test status..
 		LOG.debug("PerfTest {} status - currentRunningTime {} ", perfTest.getId(),
 						singleConsoleInUse.getCurrentRunningTime());
 		// stop target host monitor
 		if (singleConsoleInUse.hasTooManyError()) {
 			perfTestService.markProgressAndStatusAndFinishTimeAndStatistics(perfTest, Status.STOP_ON_ERROR,
 							"The test is finished. but contains a lot of errors");
 		} else {
 			perfTestService.markProgressAndStatusAndFinishTimeAndStatistics(perfTest, Status.FINISHED,
 							"The test is finished successfully");
 		}
 		removeMonitorTargets(perfTest);
 		consoleManager.returnBackConsole(perfTest.getTestIdentifier(), singleConsoleInUse);
 	}
 	
 	private void removeMonitorTargets(PerfTest perfTest) {
 		List<String> targetIPList = perfTest.getTargetHostIP();
 		for (String targetIP : targetIPList) {
 			try {
 				monitorDataService.removeMonitorAgents(targetIP);
 			} catch(Exception e) {
 				LOG.error("Error while deleting monitor for {}", targetIP);
 			}
 		}
 	}
 }
