 /*
  * Copyright 2009-2011 by The Regents of the University of California
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * you may obtain a copy of the License from
  * 
  *     http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package edu.uci.ics.asterix.api.http.servlet;
 
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.nio.ByteBuffer;
 
 import javax.servlet.ServletContext;
 import javax.servlet.http.HttpServlet;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
 import org.json.JSONArray;
 import org.json.JSONObject;
 
 import edu.uci.ics.asterix.result.ResultReader;
 import edu.uci.ics.asterix.result.ResultUtils;
import edu.uci.ics.asterix.runtime.formats.FormatUtils;
 import edu.uci.ics.hyracks.api.client.HyracksConnection;
 import edu.uci.ics.hyracks.api.client.IHyracksClientConnection;
 import edu.uci.ics.hyracks.api.dataset.ResultSetId;
 import edu.uci.ics.hyracks.api.job.JobId;
 
 public class QueryResultAPIServlet extends HttpServlet {
     private static final long serialVersionUID = 1L;
 
     private static final String HYRACKS_CONNECTION_ATTR = "edu.uci.ics.asterix.HYRACKS_CONNECTION";
 
     @Override
     public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
         String strHandle = request.getParameter("handle");
         PrintWriter out = response.getWriter();
         response.setContentType("text/html");
         ServletContext context = getServletContext();
         IHyracksClientConnection hcc;
         try {
             HyracksProperties hp = new HyracksProperties();
             String strIP = hp.getHyracksIPAddress();
             int port = hp.getHyracksPort();
 
             synchronized (context) {
                 hcc = (IHyracksClientConnection) context.getAttribute(HYRACKS_CONNECTION_ATTR);
                 if (hcc == null) {
                     hcc = new HyracksConnection(strIP, port);
                     context.setAttribute(HYRACKS_CONNECTION_ATTR, hcc);
                 }
             }
             JSONObject handleObj = new JSONObject(strHandle);
             JSONArray handle = handleObj.getJSONArray("handle");
             JobId jobId = new JobId(handle.getLong(0));
             ResultSetId rsId = new ResultSetId(handle.getLong(1));
             ByteBuffer buffer = ByteBuffer.allocate(ResultReader.FRAME_SIZE);
             /* TODO(madhusudancs): We need to find a way to JSON serialize default format obtained from
              * metadataProvider in the AQLTranslator and store it as part of the result handle.
              */
            ResultReader resultReader = new ResultReader(hcc, FormatUtils.getDefaultFormat());
             resultReader.open(jobId, rsId);
             buffer.clear();
             JSONObject jsonResponse = new JSONObject();
             JSONArray results = new JSONArray();
             while (resultReader.read(buffer) > 0) {
                 results.put(ResultUtils.getJSONFromBuffer(buffer, resultReader.getFrameTupleAccessor()));
             }
             jsonResponse.put("results", results);
             out.write(jsonResponse.toString());
 
         } catch (Exception e) {
             out.println(e.getMessage());
             e.printStackTrace(out);
         }
     }
 }
