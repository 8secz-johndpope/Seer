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
 package net.grinder;
 
 import static org.ngrinder.common.util.Preconditions.checkNotNull;
 
 import java.beans.PropertyChangeEvent;
 import java.beans.PropertyChangeListener;
 import java.io.BufferedWriter;
 import java.io.File;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.net.InetAddress;
 import java.net.UnknownHostException;
 import java.text.DecimalFormat;
 import java.text.NumberFormat;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import net.grinder.common.GrinderException;
 import net.grinder.common.GrinderProperties;
 import net.grinder.common.Test;
 import net.grinder.common.processidentity.AgentIdentity;
 import net.grinder.common.processidentity.WorkerProcessReport;
 import net.grinder.console.ConsoleFoundationEx;
 import net.grinder.console.common.Resources;
 import net.grinder.console.common.ResourcesImplementation;
 import net.grinder.console.communication.NGrinderConsoleCommunicationService;
 import net.grinder.console.communication.ProcessControl;
 import net.grinder.console.communication.ProcessControl.Listener;
 import net.grinder.console.communication.ProcessControl.ProcessReports;
 import net.grinder.console.communication.ProcessControlImplementation;
 import net.grinder.console.distribution.AgentCacheState;
 import net.grinder.console.distribution.FileDistribution;
 import net.grinder.console.distribution.FileDistributionHandler;
 import net.grinder.console.model.ConsoleProperties;
 import net.grinder.console.model.ModelTestIndex;
 import net.grinder.console.model.SampleListener;
 import net.grinder.console.model.SampleModel;
 import net.grinder.console.model.SampleModelImplementationEx;
 import net.grinder.console.model.SampleModelViews;
 import net.grinder.statistics.ExpressionView;
 import net.grinder.statistics.StatisticsSet;
 import net.grinder.util.AllocateLowestNumber;
 import net.grinder.util.ConsolePropertiesFactory;
 import net.grinder.util.Directory;
 import net.grinder.util.FileContents;
 import net.grinder.util.ListenerSupport;
 import net.grinder.util.ListenerSupport.Informer;
 import net.grinder.util.ReflectionUtil;
 import net.grinder.util.thread.Condition;
 
 import org.apache.commons.io.IOUtils;
 import org.apache.commons.lang.StringUtils;
 import org.apache.commons.lang.builder.ToStringBuilder;
 import org.ngrinder.common.exception.NGrinderRuntimeException;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 /**
  * Single console for multiple test.
  * 
  * @author JunHo Yoon
  */
 public class SingleConsole implements Listener, SampleListener {
 	private final ConsoleProperties consoleProperties;
 	private Thread thread;
 	private ConsoleFoundationEx consoleFoundation;
 	public static final Resources RESOURCE = new ResourcesImplementation("net.grinder.console.common.resources.Console");
 	public static final Logger LOGGER = LoggerFactory.getLogger(RESOURCE.getString("shortTitle"));
 
 	private Condition m_eventSyncCondition = new Condition();
 	private ProcessReports[] processReports;
 	private boolean cancel = false;
 
 	// for displaying tps graph in running page
 	private Double[] values = new Double[60];
 	private int cursor = 0;
 	private SampleModel sampleModel;
 	private SampleModelViews modelView;
 	private long startTime = 0;
 	private Date TPS_LESSTHAN_ZREO_TIME;
 	private final ListenerSupport<ConsoleShutdownListener> m_shutdownListeners = new ListenerSupport<ConsoleShutdownListener>();
 
 	private File reportPath;
 	private NumberFormat formatter = new DecimalFormat("###.###");
 	private Map<String, Object> statisticData;
 
 	/**
 	 * Constructor with console ip and port.
 	 * 
 	 * @param ip
 	 *            IP
 	 * @param port
 	 *            PORT
 	 */
 	public SingleConsole(String ip, int port) {
 		this(ip, port, ConsolePropertiesFactory.createEmptyConsoleProperties());
 	}
 
 	/**
 	 * Constructor with console port and properties.
 	 * 
 	 * @param port
 	 *            PORT
 	 * @param consoleProperties
 	 *            {@link ConsoleProperties} used.
 	 */
 	public SingleConsole(int port, ConsoleProperties consoleProperties) {
 		this("", port, consoleProperties);
 	}
 
 	/**
 	 * Constructor with IP, port, and properties.
 	 * 
 	 * @param ip
 	 *            IP
 	 * @param port
 	 *            PORT
 	 * @param consoleProperties
 	 *            {@link ConsoleProperties} used.
 	 */
 	public SingleConsole(String ip, int port, ConsoleProperties consoleProperties) {
 		this.consoleProperties = consoleProperties;
 
 		try {
 			this.getConsoleProperties().setConsoleHost(ip);
 			this.getConsoleProperties().setConsolePort(port);
 			this.consoleFoundation = new ConsoleFoundationEx(RESOURCE, LOGGER, consoleProperties, m_eventSyncCondition);
 			sampleModel = getConsoleComponent(SampleModelImplementationEx.class);
 			sampleModel.addTotalSampleListener(this);
 			modelView = getConsoleComponent(SampleModelViews.class);
 			getConsoleComponent(ProcessControl.class).addProcessStatusListener(this);
 
 		} catch (GrinderException e) {
 			throw new NGrinderRuntimeException("Exception occurs while creating SingleConsole", e);
 
 		}
 	}
 
 	/**
 	 * Simple constructor only setting port. It automatically binds all ip addresses.
 	 * 
 	 * @param port
 	 *            PORT number
 	 */
 	public SingleConsole(int port) {
 		this("", port);
 	}
 
 	/**
 	 * Return the assigned console port.
 	 * 
 	 * @return console port
 	 */
 	public int getConsolePort() {
 		return this.getConsoleProperties().getConsolePort();
 	}
 
 	/**
 	 * Return the assigned console host. If it's empty, it returns host IP
 	 * 
 	 * @return console host
 	 */
 	public String getConsoleHost() {
 		try {
 			return StringUtils.defaultIfBlank(this.getConsoleProperties().getConsoleHost(), InetAddress.getLocalHost()
 					.getHostAddress());
 		} catch (UnknownHostException e) {
 			return "";
 		}
 	}
 
 	/**
 	 * Start console and wait until it's ready to get agent message.
 	 */
 	public void start() {
 
 		synchronized (m_eventSyncCondition) {
 			thread = new Thread(new Runnable() {
 				public void run() {
 					consoleFoundation.run();
 				}
 			}, "SingleConsole on port " + getConsolePort());
 			thread.setDaemon(true);
 			thread.start();
 			// 10 second is too big?
 			m_eventSyncCondition.waitNoInterrruptException(10000);
 		}
 	}
 
 	/**
 	 * For test.
 	 */
 	public void startSync() {
 		consoleFoundation.run();
 	}
 
 	/**
 	 * Shutdown console and wait until underlying console logic is stop to run.
 	 */
 	public void shutdown() {
 		try {
 			synchronized (this) {
 				consoleFoundation.shutdown();
 				if (thread != null && !thread.isInterrupted()) {
 					thread.interrupt();
 					thread.join(1000);
 				}
 
 			}
 		} catch (Exception e) {
 			throw new NGrinderRuntimeException("Exception occurs while shutting down SingleConsole", e);
 		}
 	}
 
 	public int getAllAttachedAgentsCount() {
 		return ((ProcessControlImplementation) consoleFoundation.getComponent(ProcessControl.class))
 				.getNumberOfLiveAgents();
 	}
 
 	/**
 	 * Get all attached agent list on this console.
 	 * 
 	 * @return agent list
 	 */
 	public List<AgentIdentity> getAllAttachedAgents() {
 		final List<AgentIdentity> agentIdentities = new ArrayList<AgentIdentity>();
 		AllocateLowestNumber agentIdentity = (AllocateLowestNumber) checkNotNull(
 				ReflectionUtil.getFieldValue(
 						(ProcessControlImplementation) consoleFoundation.getComponent(ProcessControl.class),
 						"m_agentNumberMap"),
 				"m_agentNumberMap on ProcessControlImplemenation is not available in this grinder version");
 		agentIdentity.forEach(new AllocateLowestNumber.IteratorCallback() {
 			public void objectAndNumber(Object object, int number) {
 				agentIdentities.add((AgentIdentity) object);
 			}
 		});
 		return agentIdentities;
 	}
 
 	/**
 	 * Get the console Component.
 	 * 
 	 * @param <T>
 	 *            componentType component type
 	 * @param componentType
 	 *            component type
 	 * @return the consoleFoundation
 	 */
 	public <T> T getConsoleComponent(Class<T> componentType) {
 		return consoleFoundation.getComponent(componentType);
 	}
 
 	public ConsoleProperties getConsoleProperties() {
 		return consoleProperties;
 	}
 
 	public long startTest(GrinderProperties properties) {
 		properties.setInt(GrinderProperties.CONSOLE_PORT, getConsolePort());
 		getConsoleComponent(ProcessControl.class).startWorkerProcesses(properties);
 		this.startTime = System.currentTimeMillis();
 		return this.startTime;
 	}
 
 	public void setDistributionDirectory(File filePath) {
 		final ConsoleProperties properties = (ConsoleProperties) getConsoleComponent(ConsoleProperties.class);
 		Directory directory;
 		try {
 			directory = new Directory(filePath);
 			properties.setAndSaveDistributionDirectory(directory);
 		} catch (Exception e) {
 			LOGGER.error(e.getMessage(), e);
 			throw new NGrinderRuntimeException(e.getMessage(), e);
 		}
 	}
 
 	public void cancel() {
 		cancel = true;
 	}
 
 	private boolean shouldEnable(FileDistribution fileDistribution) {
 		return fileDistribution.getAgentCacheState().getOutOfDate();
 	}
 
 	/**
 	 * Distribute files on given filePath to attached agents.
 	 * 
 	 * @param filePath
 	 *            the distribution files
 	 */
 	public void distributeFiles(File filePath) {
 		setDistributionDirectory(filePath);
 		distributFiles();
 	}
 
 	/**
 	 * Distribute files on agents.
 	 */
 	public void distributFiles() {
 		final FileDistribution fileDistribution = (FileDistribution) getConsoleComponent(FileDistribution.class);
 		final AgentCacheState agentCacheState = fileDistribution.getAgentCacheState();
 		final Condition cacheStateCondition = new Condition();
 		agentCacheState.addListener(new PropertyChangeListener() {
 			public void propertyChange(PropertyChangeEvent ignored) {
 				synchronized (cacheStateCondition) {
 					cacheStateCondition.notifyAll();
 				}
 			}
 		});
 		final FileDistributionHandler distributionHandler = fileDistribution.getHandler();
 		// When cancel is called.. stop processing.
 		while (!cancel) {
 			try {
 				final FileDistributionHandler.Result result = distributionHandler.sendNextFile();
 				if (result == null) {
 					break;
 				}
 
 			} catch (FileContents.FileContentsException e) {
 				// FIXME : Forward this error to controller.!!
 				e.printStackTrace();
 			}
 			// The cache status is updated asynchronously by agent reports.
 			// If we have a listener, we wait for up to five seconds for all
 			// agents to indicate that they are up to date.
 			synchronized (cacheStateCondition) {
 				for (int i = 0; i < 5 && shouldEnable(fileDistribution); ++i) {
 					cacheStateCondition.waitNoInterrruptException(1000);
 				}
 			}
 		}
 	}
 
 	public void waitUntilAgentConnected(int size) {
 		int trial = 1;
 		while (trial++ < 10) {
 			// when agent finished one test, processReports will be updated as
 			// null
 			if (processReports == null || this.processReports.length != size) {
 				synchronized (m_eventSyncCondition) {
 					m_eventSyncCondition.waitNoInterrruptException(1000);
 				}
 			} else {
 				return;
 			}
 		}
 		throw new NGrinderRuntimeException("Connection is not completed. processReport is "
 				+ ToStringBuilder.reflectionToString(processReports));
 	}
 
 	/**
 	 * Check all test is finished. To be safe, this counts thread count and not finished workprocess. If one of them is
 	 * 0, It thinks test is finished.
 	 * 
 	 * @return true if finished
 	 */
 	public boolean isAllTestFinished() {
 		int workingThreadNum = 0;
 		int notFinishedWorkerCount = 0;
 		for (ProcessReports processReport : this.processReports) {
 			WorkerProcessReport[] reports = processReport.getWorkerProcessReports();
 			for (WorkerProcessReport report : reports) {
 				if (report.getState() != 3) {
 					notFinishedWorkerCount++;
 				}
 				workingThreadNum += report.getNumberOfRunningThreads();
 			}
 		}
 		return notFinishedWorkerCount == 0 || workingThreadNum == 0;
 	}
 
 	/**
 	 * Get Tps values,Its length is 60
 	 */
 	public void addTpsValue(Double newValue) {
 		values[cursor] = newValue;
 		if (++cursor >= values.length) {
 			cursor = 0;
 		}
 	}
 
 	public String getTpsValues() {
 		StringBuffer str = new StringBuffer("[");
 		for (int i = 0; i < values.length; i++) {
 			str.append(values[i]);
 
 			if (i != (values.length - 1)) {
 				str.append(",");
 			}
 		}
 		str.append("]");
 		return str.toString();
 	}
 
 	public long getStartTime() {
 		return startTime;
 	}
 
 	@Override
 	public void update(StatisticsSet intervalStatistics, StatisticsSet cumulativeStatistics) {
 
 		double tps = sampleModel.getTPSExpression().getDoubleValue(intervalStatistics);
 		// If the tps is low that it's can be the agents or scripts goes wrong.
 		if (tps < 0.001) {
 			if (TPS_LESSTHAN_ZREO_TIME == null) {
 				TPS_LESSTHAN_ZREO_TIME = new Date();
 			} else if (new Date().getTime() - TPS_LESSTHAN_ZREO_TIME.getTime() >= 60000) {
 				LOGGER.warn("Test has been forced stop because of tps is less than 0.001 and sustain more than one minitue.");
 				getListeners().apply(new Informer<ConsoleShutdownListener>() {
 					public void inform(ConsoleShutdownListener listener) {
 						listener.readyToStop();
 					}
 				});
 
 			}
 		} else {
 			TPS_LESSTHAN_ZREO_TIME = null;
 			//only if tps value is not too small ,It should be displayed 
 			addTpsValue(tps);
 		}
 
 		statisticData = this.getStatistics();
 		@SuppressWarnings("unchecked")
 		List<Map<String, Object>> lastSampleStatistics = (List<Map<String, Object>>) statisticData
 				.get("lastSampleStatistics");
 
 		if (lastSampleStatistics != null) {
 			double tpsSum = 0;
 			double errors = 0;
 			double meanTestTime = 0;
 
 			for (Map<String, Object> lastStatistic : lastSampleStatistics) {
 				tpsSum += (Double) lastStatistic.get("TPS");
 				errors += (Double) lastStatistic.get("Errors");
 				Double temp = (Double) lastStatistic.get("Mean_Test_Time_(ms)");
 
 				meanTestTime += temp != null ? temp : 0;
 			}
 
 			try {
 				writeReportData("tps_failed", errors);
 				writeReportData("tps_total", tpsSum);
 				writeReportData("response_time", meanTestTime);
 			} catch (IOException e) {
 				LOGGER.error("Write report data failed : ", e);
 			}
 		}
 
 	}
 
 	/**
 	 * To get statistics data when test is running
 	 */
 	private Map<String, Object> getStatistics() {
 
 		Map<String, Object> result = new HashMap<String, Object>();
 		List<Map<String, Object>> cumulativeStatistics = new ArrayList<Map<String, Object>>();
 		List<Map<String, Object>> lastSampleStatistics = new ArrayList<Map<String, Object>>();
 		ExpressionView[] views = modelView.getCumulativeStatisticsView().getExpressionViews();
 		ModelTestIndex modelIndex = ((SampleModelImplementationEx) sampleModel).getModelTestIndex();
 		if (modelIndex != null) {
 			for (int i = 0; i < modelIndex.getNumberOfTests(); i++) {
 				Map<String, Object> statistics = new HashMap<String, Object>();
 				Map<String, Object> lastStatistics = new HashMap<String, Object>();
 
 				Test test = modelIndex.getTest(i);
 				statistics.put("testNumber", test.getNumber());
 				statistics.put("testDescription", test.getDescription());
 				lastStatistics.put("testNumber", test.getNumber());
 				lastStatistics.put("testDescription", test.getDescription());
 
 				StatisticsSet set = modelIndex.getCumulativeStatistics(i);
 				StatisticsSet lastSet = modelIndex.getLastSampleStatistics(i);
 				for (ExpressionView expressionView : views) { // TODO :
 																// expressionView
 																// == null?
 					statistics.put(expressionView.getDisplayName().replaceAll("\\s+", "_"),
 							getRealDoubleValue(expressionView.getExpression().getDoubleValue(set)));
 					lastStatistics.put(expressionView.getDisplayName().replaceAll("\\s+", "_"),
 							getRealDoubleValue(expressionView.getExpression().getDoubleValue(lastSet)));
 				}
 
 				// Tests
 				Double tests = (Double) statistics.get("Tests");
 				Double errors = (Double) statistics.get("Errors");
 				statistics.put("TestsStr", formatter.format(tests));
 				statistics.put("ErrorsStr", formatter.format(errors));
 
 				Double lastTests = (Double) lastStatistics.get("Tests");
 				Double lastErrors = (Double) lastStatistics.get("Errors");
 				lastStatistics.put("TestsStr", formatter.format(lastTests));
 				lastStatistics.put("ErrorsStr", formatter.format(lastErrors));
 
 				cumulativeStatistics.add(statistics);
 				lastSampleStatistics.add(lastStatistics);
 			}
 		}
 
 		StatisticsSet totalSet = sampleModel.getTotalCumulativeStatistics();
 		Map<String, Object> totalStatistics = new HashMap<String, Object>();
 
 		for (ExpressionView expressionView : views) { // TODO : expressionView
 														// == null ?
 			totalStatistics.put(expressionView.getDisplayName().replaceAll("\\s+", "_"),
 					getRealDoubleValue(expressionView.getExpression().getDoubleValue(totalSet)));
 		}
 
 		Double tests = (Double) totalStatistics.get("Tests");
 		Double errors = (Double) totalStatistics.get("Errors");
 		totalStatistics.put("TestsStr", formatter.format(tests));
 		totalStatistics.put("ErrorsStr", formatter.format(errors));
 
 		result.put("totalStatistics", totalStatistics);
 		result.put("cumulativeStatistics", cumulativeStatistics);
 		result.put("lastSampleStatistics", lastSampleStatistics);
 
 		result.put("tpsChartData", this.getTpsValues());
 
 		ProcessControl processControl = getConsoleComponent(ProcessControl.class);
 		NGrinderConsoleCommunicationService.collectWorkerAndThreadInfo(processControl, result);
 		result.put("success", !this.isAllTestFinished());
 
 		return result;
 	}
 
 	private static Object getRealDoubleValue(Double doubleValue) {
 		if (doubleValue.isInfinite() || doubleValue.isNaN()) {
 			return null;
 		}
 		return doubleValue;
 	}
 
 	public ListenerSupport<ConsoleShutdownListener> getListeners() {
 		return this.m_shutdownListeners;
 	}
 
 	public void addListener(ConsoleShutdownListener listener) {
 		m_shutdownListeners.add(listener);
 	}
 
 	@Override
 	public void update(ProcessReports[] processReports) {
 		synchronized (m_eventSyncCondition) {
 			this.processReports = processReports;
 			m_eventSyncCondition.notifyAll();
 		}
 	}
 
 	public interface ConsoleShutdownListener {
 		void readyToStop();
 	}
 
 	public void writeReportData(String name, double value) throws IOException {
 		File filename = new File(this.reportPath, name + ".data");
 		FileWriter write = null;
 		BufferedWriter bw = null;
 		try {
 			write = new FileWriter(filename, true);
 			bw = new BufferedWriter(write);
 			bw.write(formatter.format(value));
 			bw.newLine();
 			bw.flush();
 		} catch (Exception e) {
 			LOGGER.error(e.getMessage(), e);
 			throw new NGrinderRuntimeException(e.getMessage(), e);
 		} finally {
 			IOUtils.closeQuietly(write);
 			IOUtils.closeQuietly(bw);
 		}
 	}
 
 	public File getReportPath() {
 		return reportPath;
 	}
 
 	public Map<String, Object> getStatictisData() {
 		return this.statisticData;
 	}
 
 	public void setReportPath(File reportDir) {
 		checkNotNull(reportDir, "report folder should not be empty!").mkdirs();
 		this.reportPath = reportDir;
 	}
 
 }
