 /*******************************************************************************
  * Copyright (c) 2009 Red Hat, Inc.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Red Hat - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.linuxtools.callgraph.core;
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileReader;
 import java.io.IOException;
 import java.util.Calendar;
 import java.util.TimeZone;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import org.eclipse.core.runtime.IProgressMonitor;
 
 /**
  * Helper class parses the given string for recognizable error messages
  *
  */
 public class SystemTapErrorHandler {
 
     public static final String FILE_PROP = "errors.prop"; //$NON-NLS-1$
     public static final String FILE_ERROR_LOG = "Error.log"; //$NON-NLS-1$
     public static final int MAX_LOG_SIZE = 50000;
     private boolean errorRecognized;
     private StringBuilder errorMessage = new StringBuilder(""); //$NON-NLS-1$
     private StringBuilder logContents;
 
 
     public SystemTapErrorHandler() {
         errorRecognized = false;
        errorMessage.append(Messages
             .getString("SystemTapErrorHandler.ErrorMessage") + //$NON-NLS-1$
             Messages.getString("SystemTapErrorHandler.ErrorMessage1")); //$NON-NLS-1$
 
         logContents = new StringBuilder(); //$NON-NLS-1$
     }
 
     /**
      * Search given string for recognizable error messages. Can append the
      * contents of the string to the error log if writeToLog() or
      * finishHandling() are called.
      * @param doc
      */
     public void handle(IProgressMonitor m, String errors) {
         String[] errorsList = errors.split("\n"); //$NON-NLS-1$
 
         // READ FROM THE PROP FILE AND DETERMINE TYPE OF ERROR
         File file = new File(PluginConstants.getPluginLocation() + FILE_PROP);
         try {
             BufferedReader buff = new BufferedReader(new FileReader(file));
             String line;
             int index;
 
             for (String message : errorsList) {
                 buff = new BufferedReader(new FileReader(file));
                 while ((line = buff.readLine()) != null) {
                     if (m != null && m.isCanceled())
                         return;
                     index = line.indexOf('=');
                     Pattern pat = Pattern.compile(line.substring(0, index),Pattern.DOTALL);
                     Matcher matcher = pat.matcher(message);
 
                     if (matcher.matches()) {
                         if (!isErrorRecognized()) {
                        	//First error
                             errorMessage.append(Messages.getString("SystemTapErrorHandler.ErrorMessage2")); //$NON-NLS-1$
                             setErrorRecognized(true);
                         }
                         String errorFound = line.substring(index+1);
 
                         if (!errorMessage.toString().contains(errorFound)) {
                             errorMessage.append(errorFound+ PluginConstants.NEW_LINE);
                         }
                         break;
                     }
                 }
                 buff.close();
             }
 
             logContents.append(errors);
         } catch (FileNotFoundException e) {
             e.printStackTrace();
         } catch (IOException e) {
             e.printStackTrace();
         }
 
     }
    
    
     /**
      * Append to the log contents
      */
     public void appendToLog (String header){
         logContents.append(header);
     }
    
 
     /**
      * Handle the error.
      *
      * @param m
      * @param f Temporary error file
      * @throws IOException
      */
     public void handle(IProgressMonitor m, FileReader f) throws IOException {
         BufferedReader br = new BufferedReader(f);
 
         String line;
         StringBuilder builder = new StringBuilder();
         int counter = 0;
         while ((line = br.readLine()) != null) {
             counter++;
             builder.append(line);
             builder.append("\n"); //$NON-NLS-1$
             if (m != null && m.isCanceled())
                 return;
             if (counter == 300) {
                 handle(m, builder.toString());
                 builder = new StringBuilder();
                 counter = 0;
             }
         }
         handle(m, builder.toString());
     }
 
     /**
      * Run this method when there are no more error messages to handle. Creates
      * the error pop-up message and writes to log.Currently relaunch only works 
      * for the callgraph script.
      */
     public void finishHandling(IProgressMonitor m, String scriptPath) {
         if (!isErrorRecognized()) {
             errorMessage.append(Messages.getString("SystemTapErrorHandler.NoErrRecognized") + //$NON-NLS-1$
                     Messages.getString("SystemTapErrorHandler.NoErrRecognizedMsg")); //$NON-NLS-1$
         }
 
         writeToLog();
     }
     
    
     /**
      * Writes the contents of logContents to the error log, along with date and
      * time.
      */
     public void writeToLog() {
         File errorLog = new File(PluginConstants.getDefaultOutput() + "Error.log"); //$NON-NLS-1$
 
         try {
             // CREATE THE ERROR LOG IF IT DOES NOT EXIST
             // CLEAR THE ERROR LOG AFTER A FIXED SIZE(BYTES)
             if (!errorLog.exists() || errorLog.length() > MAX_LOG_SIZE) {
                 errorLog.delete();
                 errorLog.createNewFile();
             }
 
             Calendar cal = Calendar.getInstance(TimeZone.getDefault());
             int year = cal.get(Calendar.YEAR);
             int month = cal.get(Calendar.MONTH);
             int day = cal.get(Calendar.DAY_OF_MONTH);
             int hour = cal.get(Calendar.HOUR_OF_DAY);
             int minute = cal.get(Calendar.MINUTE);
             int second = cal.get(Calendar.SECOND);
 
             // APPEND THE ERROR TO THE LOG
             Helper.appendToFile(errorLog.getAbsolutePath(), Messages
                     .getString("SystemTapErrorHandler.ErrorLogDashes") //$NON-NLS-1$
                     + PluginConstants.NEW_LINE
                     + day
                     + "/" + month //$NON-NLS-1$
                     + "/" + year + " - " + hour + ":" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                     + minute
                     + ":" + second //$NON-NLS-1$
                     + PluginConstants.NEW_LINE
                     + logContents
                     + PluginConstants.NEW_LINE + PluginConstants.NEW_LINE);
         } catch (IOException e) {
             e.printStackTrace();
         }
 
         logContents = new StringBuilder(); //$NON-NLS-1$
     }
 
 
     /**
      * Convenience method for deleting and recreating log at default location
      */
     public static void deleteLog() {
         File log = new File(PluginConstants.getDefaultOutput() + FILE_ERROR_LOG); //$NON-NLS-1$
         deleteLog(log);
     }
    
     /**
      * Delete the log at File and replace it with a new (empty) file
      * @param log
      */
     public static void deleteLog(File log) {
         log.delete();
         try {
             log.createNewFile();
         } catch (IOException e) {
             e.printStackTrace();
         }
     }
    
    
     /**
      * Returns true if an error matches one of the regex's in error.prop
      *
      * @return
      */
     public boolean isErrorRecognized() {
         return errorRecognized;
     }
 
     /**
      * Convenience method to change the error recognition value.
      *
      * @param errorsRecognized
      */
     private void setErrorRecognized(boolean errorsRecognized) {
         errorRecognized = errorsRecognized;
     }
     
     
     public String getErrorMessage(){
     	return errorMessage.toString();
     }
     
     
     public String getLogContents(){
     	return logContents.toString();
     }
    
 }
