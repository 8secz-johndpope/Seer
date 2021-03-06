 /*
  * -----------------------------------------------------------------------\
  * PerfCake
  *  
  * Copyright (C) 2010 - 2013 the original author or authors.
  *  
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * 
  *      http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  * -----------------------------------------------------------------------/
  */
 package org.perfcake;
 
 import java.util.List;
 
 import org.apache.log4j.Logger;
 import org.perfcake.message.MessageTemplate;
 import org.perfcake.message.generator.AbstractMessageGenerator;
 import org.perfcake.message.sender.MessageSenderManager;
 import org.perfcake.reporting.ReportManager;
 import org.perfcake.validation.ValidatorManager;
 
 /**
  * 
  * Scenario encapsulates whole test execution, contains all information necessary to run the test.
  * 
  * @author Pavel Macík <pavel.macik@gmail.com>
  * @author Martin Večeřa <marvenec@gmail.com>
  * @author Jiří Sedláček <jiri@sedlackovi.cz>
  */
 public class Scenario {
 
    public static final Logger log = Logger.getLogger(Scenario.class);
    private AbstractMessageGenerator generator;
    private MessageSenderManager messageSenderManager;
    private ReportManager reportManager;
    private List<MessageTemplate> messageStore;
    private ValidatorManager validatorManager;
 
    public static final String VERSION = "0.3";
 
    /**
     * Initialize the scenario execution
     * 
     * @throws PerfCakeException
     */
    public void init() throws PerfCakeException {
       if (log.isTraceEnabled()) {
          log.trace("Scenario initialization...");
       }
 
       messageSenderManager.setReportManager(reportManager);
       generator.setReportManager(reportManager);
       generator.setValidatorManager(validatorManager);
 
       try {
          generator.init(messageSenderManager, messageStore);
       } catch (final Exception e) {
          throw new PerfCakeException("Cannot initialize message generator: ", e);
       }
    }
 
    /**
     * Execute the scenario. This mainly means to send the messages.
     * 
     * @throws PerfCakeException
     */
    public void run() throws PerfCakeException {
       if (log.isTraceEnabled()) {
          log.trace("Running scenario...");
       }
 
       if (validatorManager.isEnabled()){
          validatorManager.startValidation();
       }
 
       try {
          generator.generate();
       } catch (final Exception e) {
          throw new PerfCakeException("Error generating messages: ", e);
       }
    }
 
    /**
     * Finalize the scenario.
     * 
     * @throws PerfCakeException
     */
    public void close() throws PerfCakeException {
       if (generator != null) {
          generator.close();
       }
 
       try {
          validatorManager.waitForValidation();
       } catch (final InterruptedException ie) {
          throw new PerfCakeException("Could not finish messages response validation properly: ", ie);
       }
 
       if (log.isTraceEnabled()) {
          log.trace("Scenario finished successfully!");
       }
    }
 
    AbstractMessageGenerator getGenerator() {
       return generator;
    }
 
    void setGenerator(AbstractMessageGenerator generator) {
       this.generator = generator;
    }
 
    MessageSenderManager getMessageSenderManager() {
       return messageSenderManager;
    }
 
    void setMessageSenderManager(MessageSenderManager messageSenderManager) {
       this.messageSenderManager = messageSenderManager;
    }
 
    ReportManager getReportManager() {
       return reportManager;
    }
 
    void setReportManager(ReportManager reportManager) {
       this.reportManager = reportManager;
    }
 
    List<MessageTemplate> getMessageStore() {
       return messageStore;
    }
 
    void setMessageStore(List<MessageTemplate> messageStore) {
       this.messageStore = messageStore;
    }
 
    /**
     * Sets the value of validatorManager.
     * 
     * @param validatorManager
     *           The value of validatorManager to set.
     */
    void setValidatorManager(ValidatorManager validatorManager) {
      this.validatorManager = validatorManager;
    }
 
 }
