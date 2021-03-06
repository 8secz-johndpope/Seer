 /*
  * Copyright 2009 Distributed Systems Group, University of Konstanz
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  * $LastChangedRevision$
  * $LastChangedBy$
  * $LastChangedDate$
  *
  */
 package org.perfidix.socketadapter;
 
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Set;
 
 import org.perfidix.element.BenchmarkMethod;
import org.perfidix.exceptions.AbstractPerfidixMethodException;
 import org.perfidix.exceptions.SocketViewException;
 
 /**
  * This class creates the connection to the eclipse view via
  * {@link SocketViewStub}. It contains the methods which update the view to
  * inform about the bench process progress.
  * 
  * @author Lewandowski Lukas, University of Konstanz
  */
 public final class SocketViewProgressUpdater {
 
     private transient final SocketViewStub viewStub;
 
     /**
      * The constructor initializes the host and port for creation a client
      * socket.
      * 
      * @param host
      *            Host is the host name of the view provider.
      * @param port
      *            Port represent the port number of the eclipse view.
      * @throws SocketViewException
      */
     public SocketViewProgressUpdater(final String host, final int port)
             throws SocketViewException {
         String strubParam = host;
         if (host == null || host.equals("")) {
             strubParam = "localhost";
         }
         viewStub = new SocketViewStub(strubParam, port);
 
     }
 
     /**
      * This method initializes the values of the eclipse view and resets the
      * progress bar.
      * 
      * @param mapping
      *            a mapping with all methods to benchmark and the related runs
      * @throws SocketViewException
      */
     public void initProgressView(final Map<BenchmarkMethod, Integer> mapping)
             throws SocketViewException {
         if (mapping != null) {
             final Set<BenchmarkMethod> methodSet = mapping.keySet();
 
             final Map<String, Integer> finalMap =
                     new HashMap<String, Integer>();
             for (BenchmarkMethod benchmarkMethod : methodSet) {
 
                 finalMap.put(benchmarkMethod.getMethodWithClassName(), mapping
                         .get(benchmarkMethod));
             }
 
             viewStub.initTotalBenchProgress(finalMap);
         }
     }
 
     /**
      * This method notifies the eclipse view which element is currently benched.
      * 
      * @param name
      *            This param represents the java element which is currently
      *            benched and which is fully qualified.
      * @throws SocketViewException
      */
     public void updateCurrentElement(final String name)
             throws SocketViewException {
         if (name != null) {
             viewStub.updateCurrentRun(name);
         }
     }
 
     /**
      * This method informs the view that an error occurred while benching the
      * current element.
      * 
      * @param name
      *            Element represents the java element which has not been
      *            executed successfully.
      * @param exception
      *            The exception caused by the element.
      * @throws SocketViewException
      */
     public void updateErrorInElement(
             final String name, final Exception exception)
             throws SocketViewException {
        if (name != null && exception != null) {
            if (exception instanceof AbstractPerfidixMethodException) {
                AbstractPerfidixMethodException exc =
                        (AbstractPerfidixMethodException) exception;
                viewStub.updateError(name, exc
                        .getExec().getClass().getSimpleName());
            }
            if (exception instanceof SocketViewException) {
                SocketViewException viewException =
                        (SocketViewException) exception;
                viewStub.updateError(name, viewException
                        .getExc().getClass().getSimpleName());
            }
         }
     }
 
     /**
      * This method notifies the view that all benches have been executed and the
      * bench progress is finished.
      * 
      * @throws SocketViewException
      */
     public void finished() throws SocketViewException {
         viewStub.finishedBenchRuns();
     }
 
 }
