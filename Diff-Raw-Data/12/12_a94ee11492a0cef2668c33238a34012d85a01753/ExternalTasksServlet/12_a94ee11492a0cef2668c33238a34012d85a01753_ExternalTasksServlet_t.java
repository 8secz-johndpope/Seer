 package org.intalio.tempo.uiframework;
 
 import java.io.IOException;
 
 import javax.servlet.ServletException;
 import javax.servlet.ServletOutputStream;
 import javax.servlet.http.HttpServlet;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
 import org.intalio.tempo.security.Property;
 import org.intalio.tempo.security.authentication.AuthenticationConstants;
 import org.intalio.tempo.security.util.PropertyUtils;
 import org.intalio.tempo.security.ws.TokenClient;
 import org.intalio.tempo.uiframework.forms.FormManager;
 import org.intalio.tempo.uiframework.forms.FormManagerBroker;
 import org.intalio.tempo.web.ApplicationState;
 import org.intalio.tempo.web.User;
 import org.intalio.tempo.workflow.task.Task;
 import org.intalio.tempo.workflow.tms.ITaskManagementService;
 import org.intalio.tempo.workflow.tms.client.RemoteTMSFactory;
 
 public abstract class ExternalTasksServlet extends HttpServlet {
 
     public ExternalTasksServlet() {
         super();
     }
 
     @Override
     protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
         try {
            TokenClient tc = Configuration.getInstance().getTokenClient();
             String rtoken = request.getParameter("token");
             String pToken;
             String user;
             
             if(rtoken!=null) {
                 Property[] properties = tc.getTokenProperties(rtoken);
                 user = PropertyUtils.getProperty(properties, AuthenticationConstants.PROPERTY_USER).getValue().toString();
                 pToken = rtoken;
             } else {
                 ApplicationState state = ApplicationState.getCurrentInstance(request);
                 User currentUser = state.getCurrentUser();
                 pToken = currentUser.getToken();
                 user = currentUser.getName();
             }
 
             ITaskManagementService taskManager = getTMS(request, pToken);
             Task[] tasks = taskManager.getAvailableTasks("Task", "ORDER BY T._creationDate");
             FormManager fmanager = FormManagerBroker.getInstance().getFormManager();
             ServletOutputStream outputStream = response.getOutputStream();
             String filename = "tasks for "+user+getFileExt();
             response.setContentType(getFileMimeType());
             response.addHeader("Content-disposition", "attachment; filename=\""      + filename + "\"");
             
             generateFile(request, pToken, user, tasks, fmanager, outputStream);
         } catch (Exception e) {
             throw new ServletException(e);
         }
     }
 
     public abstract void generateFile(HttpServletRequest request, String pToken, String user, Task[] tasks, FormManager fmanager, ServletOutputStream outputStream) 
         throws Exception;
 
     public abstract String getFileExt();
     
     public abstract String getFileMimeType();
 
     protected ITaskManagementService getTMS(HttpServletRequest request, String participantToken) throws Exception {
         String endpoint = URIUtils.resolveURI(request, Configuration.getInstance().getServiceEndpoint());
         return new RemoteTMSFactory(endpoint, participantToken).getService();
     }
 
 }
