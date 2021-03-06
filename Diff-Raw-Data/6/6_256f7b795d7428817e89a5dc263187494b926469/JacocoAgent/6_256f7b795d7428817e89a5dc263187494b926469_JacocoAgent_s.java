 /*******************************************************************************
  * Copyright (c) 2009, 2010 Mountainminds GmbH & Co. KG and Contributors
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *    Marc R. Hoffmann - initial API and implementation
  *    
  * $Id: $
  *******************************************************************************/
 package org.jacoco.agent.rt;
 
 import java.io.BufferedOutputStream;
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.OutputStream;
 import java.lang.instrument.Instrumentation;
 
 import org.jacoco.core.data.ExecutionDataWriter;
 import org.jacoco.core.runtime.AgentOptions;
 import org.jacoco.core.runtime.IRuntime;
 import org.jacoco.core.runtime.ModifiedSystemClassRuntime;
 
 /**
  * The agent which is referred as the <code>Premain-Class</code>.
  * 
  * @author Marc R. Hoffmann
  * @version $Revision: $
  */
 public class JacocoAgent {
 
 	private final AgentOptions options;
 
 	private IRuntime runtime;
 
 	/**
 	 * Creates a new agent with the given agent options.
 	 * 
 	 * @param options
 	 *            agent options
 	 */
 	public JacocoAgent(AgentOptions options) {
 		this.options = options;
 	}
 
 	/**
 	 * Creates a new agent with the given agent options string.
 	 * 
	 * @param agentArgs
 	 *            agent options as text string
 	 */
 	public JacocoAgent(String options) {
 		this(new AgentOptions(options));
 	}
 
 	/**
 	 * Initializes this agent.
 	 * 
 	 * @param inst
 	 *            instrumentation services
 	 * @throws Exception
 	 *             internal startup problem
 	 */
 	public void init(final Instrumentation inst) throws Exception {
 		runtime = createRuntime(inst);
 		runtime.startup();
 		inst.addTransformer(new CoverageTransformer(runtime, options));
 	}
 
 	/**
 	 * Creates the specific coverage runtime implementation.
 	 * 
 	 * @param inst
 	 *            instrumentation services
 	 * @return coverage runtime instance
 	 * @throws Exception
 	 *             creation problem
 	 */
 	protected IRuntime createRuntime(final Instrumentation inst)
 			throws Exception {
 		return ModifiedSystemClassRuntime.createFor(inst, "java/sql/Types");
 	}
 
 	/**
 	 * Shutdown the agent again.
 	 */
 	public void shutdown() {
 		writeExecutionData();
 	}
 
 	/**
 	 * Writes the collected execution data to the specified file.
	 * 
	 * @param runtime
	 *            runtime containing the execution data
 	 */
 	protected void writeExecutionData() {
 		try {
 			File execFile = new File(options.getDestfile()).getAbsoluteFile();
 			File folder = execFile.getParentFile();
 			if (folder != null) {
 				folder.mkdirs();
 			}
 			OutputStream output = new BufferedOutputStream(
 					new FileOutputStream(execFile, options.getAppend()));
 			ExecutionDataWriter writer = new ExecutionDataWriter(output);
 			writer.writeHeader();
 			runtime.collect(writer, false);
 			output.close();
 		} catch (IOException e) {
 			e.printStackTrace();
 		}
 	}
 
 	/**
 	 * This method is called by the JVM to initialize Java agents.
 	 * 
 	 * @param options
 	 *            agent options
 	 * @param inst
 	 *            intrumentation callback provided by the JVM
 	 */
 	public static void premain(final String options, final Instrumentation inst)
 			throws Exception {
 		final JacocoAgent agent = new JacocoAgent(options);
 		agent.init(inst);
 
 		Runtime.getRuntime().addShutdownHook(new Thread() {
 			@Override
 			public void run() {
 				agent.shutdown();
 			}
 		});
 	}
 
 }
