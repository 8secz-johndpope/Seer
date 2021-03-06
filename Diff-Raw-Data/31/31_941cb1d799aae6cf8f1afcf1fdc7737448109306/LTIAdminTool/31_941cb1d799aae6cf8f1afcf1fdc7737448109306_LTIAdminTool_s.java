 /**********************************************************************************
  * $URL$
  * $Id$
  ***********************************************************************************
  *
  * Copyright (c) 2011 The Sakai Foundation
  *
  * Licensed under the Educational Community License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *       http://www.osedu.org/licenses/ECL-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  **********************************************************************************/
 
 package org.sakaiproject.blti.tool;
 
 import java.io.Writer;
 import java.io.PrintWriter;
 import java.io.StringWriter;
 
 import java.util.Properties;
 import java.util.List;
 import java.util.Map;
 import java.util.HashMap;
 import java.util.Arrays;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.apache.commons.lang.StringUtils;
 
 import org.sakaiproject.cheftool.Context;
 import org.sakaiproject.cheftool.JetspeedRunData;
 import org.sakaiproject.cheftool.RunData;
 import org.sakaiproject.cheftool.VelocityPortlet;
 import org.sakaiproject.cheftool.VelocityPortletPaneledAction;
 import org.sakaiproject.event.api.SessionState;
 import org.sakaiproject.util.ResourceLoader;
 import org.sakaiproject.tool.api.Placement;
 import org.sakaiproject.tool.api.ToolManager;
 import org.sakaiproject.component.cover.ComponentManager;
 // import org.sakaiproject.component.cover.ServerConfigurationService;
 
 // TODO: FIX THIS
 import org.sakaiproject.tool.cover.SessionManager;
 
 import org.sakaiproject.lti.api.LTIService;
 import org.sakaiproject.lti.impl.DBLTIService; // HACK
 
 import org.sakaiproject.util.foorm.SakaiFoorm;
 
 /**
  * <p>
  * LTIAdminTool is a Simple Velocity-based Tool
  * </p>
  */
 public class LTIAdminTool extends VelocityPortletPaneledAction
 {
 	private static Log M_log = LogFactory.getLog(LTIAdminTool.class);
 	
 	/** Resource bundle using current language locale */
 	protected static ResourceLoader rb = new ResourceLoader("ltitool");
 
         private boolean inHelper = false;
 	
 	private static String STATE_POST = "lti:state_post";
 	private static String STATE_SUCCESS = "lti:state_success";
 	private static String STATE_ID = "lti:state_id";
 	private static String STATE_TOOL_ID = "lti:state_tool_id";
 	private static String STATE_CONTENT_ID = "lti:state_content_id";
 
 	/** Service Implementations */
 	protected static ToolManager toolManager = null; 
 	protected static LTIService ltiService = null; 
 
 	protected static SakaiFoorm foorm = new SakaiFoorm();
 
 	/**
 	 * Pull in any necessary services using factory pattern
 	 */
 	protected void getServices()
 	{
 		if ( toolManager == null ) toolManager = (ToolManager) ComponentManager.get("org.sakaiproject.tool.api.ToolManager");
 
 		/* HACK to save many restarts during development */ 
 		if ( ltiService == null ) { 
 			ltiService = (LTIService) new DBLTIService(); 
 			((org.sakaiproject.lti.impl.DBLTIService) ltiService).setAutoDdl("true"); 
 			((org.sakaiproject.lti.impl.DBLTIService) ltiService).init(); 
 		} 
                 /* End of HACK */
 
                 if ( ltiService == null ) ltiService = (LTIService) ComponentManager.get("org.sakaiproject.lti.api.LTIService");
 	}
 
 	/**
 	 * Populate the state with configuration settings
 	 */
 	protected void initState(SessionState state, VelocityPortlet portlet, JetspeedRunData rundata)
 	{
 		super.initState(state, portlet, rundata);
 
 		getServices();
 
 		Placement placement = toolManager.getCurrentPlacement();
                 String toolReg = placement.getToolId();
                 inHelper = ! ( "sakai.basiclti.admin".equals(toolReg));
 	}
 	
 	/**
 	 * Setup the velocity context and choose the template for the response.
 	 */
 	public String buildErrorPanelContext(VelocityPortlet portlet, Context context, 
 		RunData rundata, SessionState state)
 	{
 		context.put("tlang", rb);
 		state.removeAttribute(STATE_ID);
 		state.removeAttribute(STATE_TOOL_ID);
 		state.removeAttribute(STATE_POST);
 		state.removeAttribute(STATE_SUCCESS);
 		return "lti_error";
 	}
 
 	public String buildMainPanelContext(VelocityPortlet portlet, Context context, 
 		RunData rundata, SessionState state)
 	{
 		context.put("tlang", rb);
 		if ( ! ltiService.isMaintain() ) {
 		        addAlert(state,rb.getString("error.maintain.edit"));
 		        return "lti_error";
 		}
 		context.put("messageSuccess",state.getAttribute(STATE_SUCCESS));
 		context.put("isAdmin",new Boolean(ltiService.isAdmin()) );
 		List<Map<String,Object>> tools = ltiService.getTools(null,null,0,0);
                 context.put("inHelper",new Boolean(inHelper));
 		context.put("tools", tools);
 		context.put("getContext",toolManager.getCurrentPlacement().getContext());
                 context.put("doEndHelper", BUTTON + "doEndHelper");
 		state.removeAttribute(STATE_POST);
 		state.removeAttribute(STATE_SUCCESS);
 		return "lti_main";
 	}
 
        
         public void doEndHelper(RunData data, Context context)
         {
                 // Request a shortcut transfer back to the tool we are helping
                 SessionManager.getCurrentToolSession().setAttribute(HELPER_LINK_MODE, HELPER_MODE_DONE);
         }
 	
         public String buildToolViewPanelContext(VelocityPortlet portlet, Context context, 
 			RunData data, SessionState state)
 	{
 		context.put("tlang", rb);
 		if ( ! ltiService.isMaintain() ) {
 		        addAlert(state,rb.getString("error.maintain.view"));
 		        return "lti_error";
 		}
                 context.put("messageSuccess",state.getAttribute(STATE_SUCCESS));
 		String [] mappingForm = ltiService.getToolModel();
 		String id = data.getParameters().getString("id");
 		if ( id == null ) {
 		        addAlert(state,rb.getString("error.id.not.found"));
 		        return "lti_main";
 		}
 		Long key = new Long(id);
 		Map<String,Object> tool = ltiService.getTool(key);
 		if (  tool == null ) return "lti_main";		
 		String formOutput = ltiService.formOutput(tool, mappingForm);
 		context.put("formOutput", formOutput);
 		state.removeAttribute(STATE_SUCCESS);
 		return "lti_tool_view";
 	}
 	
 	public String buildToolEditPanelContext(VelocityPortlet portlet, Context context, 
 			RunData data, SessionState state)
 	{
 		context.put("tlang", rb);
 		String stateId = (String) state.getAttribute(STATE_ID);
 		state.removeAttribute(STATE_ID);
 		if ( ! ltiService.isMaintain() ) {
 		        addAlert(state,rb.getString("error.maintain.edit"));
 		        return "lti_error";
 		}
                 context.put("doToolAction", BUTTON + "doToolPut");
                 context.put("messageSuccess",state.getAttribute(STATE_SUCCESS));
 		String [] mappingForm = ltiService.getToolModel();
 		String id = data.getParameters().getString("id");
 	        if ( id == null ) id = stateId;
 		if ( id == null ) {
 		        addAlert(state,rb.getString("error.id.not.found"));
 		        return "lti_main";
 		}		
 		Long key = new Long(id);
 		Map<String,Object> tool = ltiService.getTool(key);
 		if (  tool == null ) return "lti_main";		
 		String formInput = ltiService.formInput(tool, mappingForm);
 		context.put("formInput", formInput);
 		state.removeAttribute(STATE_SUCCESS);
 		return "lti_tool_insert";
 	}
 	
 	public String buildToolDeletePanelContext(VelocityPortlet portlet, Context context, 
 			RunData data, SessionState state)
 	{
 		context.put("tlang", rb);
 		if ( ! ltiService.isMaintain() ) {
 		        addAlert(state,rb.getString("error.maintain.delete"));
 		        return "lti_error";
 		}
                 context.put("doToolAction", BUTTON + "doToolDelete");
 		String [] mappingForm = foorm.filterForm(ltiService.getToolModel(), "^title:.*|^launch:.*|^id:.*", null);
 		String id = data.getParameters().getString("id");
 		if ( id == null ) {
 		        addAlert(state,rb.getString("error.id.not.found"));
 		        return "lti_main";
 		}	
 		Long key = new Long(id);
 		Map<String,Object> tool = ltiService.getTool(key);
 		if (  tool == null ) {
 		        addAlert(state,rb.getString("error.tool.not.found"));
 		        return "lti_main";
 		}
 		String formOutput = ltiService.formOutput(tool, mappingForm);
 		context.put("formOutput", formOutput);
 		context.put("tool",tool);
 		state.removeAttribute(STATE_SUCCESS);
 		return "lti_tool_delete";
 	}
 
 	public void doToolDelete(RunData data, Context context)
 	{
 
 		String peid = ((JetspeedRunData) data).getJs_peid();
 		SessionState state = ((JetspeedRunData) data).getPortletSessionState(peid);
 		
 		if ( ! ltiService.isMaintain() ) {
 		        addAlert(state,rb.getString("error.maintain.delete"));
                         switchPanel(state, "Error");
 		        return;
 		}
 		Properties reqProps = data.getParameters().getProperties();
 		String id = data.getParameters().getString("id");
 		Object retval = null;
                 if ( id == null ) {
                         addAlert(state,rb.getString("error.id.not.found"));
                         switchPanel(state, "Main");
                         return;
                 }
                 Long key = new Long(id);
 		if ( ltiService.deleteTool(key) )
 		{
 		        state.setAttribute(STATE_SUCCESS,rb.getString("success.deleted"));
 		        switchPanel(state, "Main");
                 } else {
                         addAlert(state,rb.getString("error.delete.fail"));
                         switchPanel(state, "Main");
                 }
 	}
 	
 	public String buildToolInsertPanelContext(VelocityPortlet portlet, Context context, 
 			RunData data, SessionState state)
 	{
 		context.put("tlang", rb);
 		if ( ! ltiService.isMaintain() ) {
 		        addAlert(state,rb.getString("error.edit.maintain"));
 		        return "lti_error";
 		}
                 context.put("doToolAction", BUTTON + "doToolPut");
                 context.put("messageSuccess",state.getAttribute(STATE_SUCCESS));
 		String [] mappingForm = ltiService.getToolModel();
 		Properties previousPost = (Properties) state.getAttribute(STATE_POST);
 		String formInput = ltiService.formInput(previousPost, mappingForm);
 		context.put("formInput",formInput);
 		state.removeAttribute(STATE_POST);
 		state.removeAttribute(STATE_SUCCESS);
 		return "lti_tool_insert";
 	}
 
         // Insert or edit
 	public void doToolPut(RunData data, Context context)
 	{
 
 		String peid = ((JetspeedRunData) data).getJs_peid();
 		SessionState state = ((JetspeedRunData) data).getPortletSessionState(peid);
 		
 		if ( ! ltiService.isMaintain() ) {
 		        addAlert(state,rb.getString("error.maintain.delete"));
                         switchPanel(state,"Error");
 		        return;
 		}
 		Properties reqProps = data.getParameters().getProperties();
 		String id = data.getParameters().getString("id");
 		Object retval = null;
 		String success = null;
 		if ( id == null ) 
 		{
 	                retval = ltiService.insertTool(reqProps);
 	                success = rb.getString("success.created");
 		} else {
 			Long key = new Long(id);
 		        retval = ltiService.updateTool(key, reqProps);
 		        success = rb.getString("success.updated");
                 }
                 
                 if ( retval instanceof String ) 
 		{
 	                state.setAttribute(STATE_POST,reqProps);
 			addAlert(state, (String) retval);
 			state.setAttribute(STATE_ID,id);
 			return;
 		}
 
 		state.setAttribute(STATE_SUCCESS,success);
 		switchPanel(state, "Main");
 	}
 
 	/**
 	 * Setup the velocity context and choose the template for options.
 	 */
 	public String buildMappingPanelContext(VelocityPortlet portlet, Context context, 
 			RunData data, SessionState state)
 	{
 		context.put("tlang", rb);
 		if ( ! ltiService.isAdmin() ) {
 		        addAlert(state,rb.getString("error.admin.view"));
 		        return "lti_error";
 		}
 		List<Map<String,Object>> mappings = ltiService.getMappings(null,null,0,100);
 		context.put("mappings", mappings);
 		context.put("messageSuccess",state.getAttribute(STATE_SUCCESS));
 		state.removeAttribute(STATE_SUCCESS);
 		return "lti_mapping";
 	}
 
 	public String buildMappingInsertPanelContext(VelocityPortlet portlet, Context context, 
 			RunData data, SessionState state)
 	{
 		context.put("tlang", rb);
 		if ( ! ltiService.isAdmin() ) {
 		        addAlert(state,rb.getString("error.admin.edit"));
 		        return "lti_error";
 		}
                 context.put("doMappingAction", BUTTON + "doMappingPut");
                 context.put("messageSuccess",state.getAttribute(STATE_SUCCESS));
 		state.removeAttribute(STATE_SUCCESS);
 		String [] mappingForm = ltiService.getMappingModel();
 		Properties previousPost = (Properties) state.getAttribute(STATE_POST);
 		String formInput = ltiService.formInput(previousPost, mappingForm);
 		context.put("formInput",formInput);
 		return "lti_mapping_insert";
 	}
 	
 	public String buildMappingEditPanelContext(VelocityPortlet portlet, Context context, 
 			RunData data, SessionState state)
 	{
 		String stateId = (String) state.getAttribute(STATE_ID);
 		state.removeAttribute(STATE_ID);
 		
 		context.put("tlang", rb);
 		if ( ! ltiService.isAdmin() ) {
 		        addAlert(state,rb.getString("error.admin.edit"));
 		        return "lti_error";
 		}
                 context.put("doMappingAction", BUTTON + "doMappingPut");
                 context.put("messageSuccess",state.getAttribute(STATE_SUCCESS));
 		String [] mappingForm = ltiService.getMappingModel();
 		String id = data.getParameters().getString("id");
 		if ( id == null ) id = stateId;
 		if ( id == null ) {
 		        addAlert(state,rb.getString("error.id.not.found"));
 		        return "lti_mapping";
 		}
 		Long key = new Long(id);
 		Map<String,Object> mapping = ltiService.getMapping(key);
 		if (  mapping == null ) return "lti_main";		
 		String formInput = ltiService.formInput(mapping, mappingForm);
 		context.put("formInput", formInput);
 		state.removeAttribute(STATE_SUCCESS);
 		return "lti_mapping_insert";
 	}
 	
 	public String buildMappingDeletePanelContext(VelocityPortlet portlet, Context context, 
 			RunData data, SessionState state)
 	{
 		context.put("tlang", rb);
 		if ( ! ltiService.isAdmin() ) {
 		        addAlert(state,rb.getString("error.admin.delete"));
 		        return "lti_error";
 		}
                 context.put("doToolAction", BUTTON + "doMappingDelete");
 		String [] mappingForm = ltiService.getMappingModel();
 		String id = data.getParameters().getString("id");
 		if ( id == null ) {
 		        addAlert(state,rb.getString("error.id.not.found"));
 		        return "lti_mapping";
 		}
 		Long key = new Long(id);
 
 		Map<String,Object> mapping = ltiService.getMapping(key);
 
 		if (  mapping == null ) {
 		        addAlert(state,rb.getString("error.mapping.not.found"));
 		        return "lti_mapping";
 		}
 		String formOutput = ltiService.formOutput(mapping, mappingForm);
 		context.put("formOutput", formOutput);
 		context.put("mapping",mapping);
 		state.removeAttribute(STATE_SUCCESS);
 		return "lti_mapping_delete";
 	}
 	
 	// Insert or edit
 	public void doMappingPut(RunData data, Context context)
 	{
 
 		String peid = ((JetspeedRunData) data).getJs_peid();
 		SessionState state = ((JetspeedRunData) data).getPortletSessionState(peid);
 		
 		if ( ! ltiService.isAdmin() ) {
 		        addAlert(state,rb.getString("error.admin.edit"));
 		        switchPanel(state,"Error");
 		        return;
 		}
 		Properties reqProps = data.getParameters().getProperties();
 		String id = data.getParameters().getString("id");
 		Object retval = null;
 		String success = null;
 		if ( id == null ) 
 		{
 	                retval = ltiService.insertMapping(reqProps);
 	                success = rb.getString("success.created");
 		} else {
 			Long key = new Long(id);
 			retval = ltiService.updateMapping(key, reqProps);
 			success = rb.getString("success.updated");
                 }
                 
                 if ( retval instanceof String ) 
 		{
 	                state.setAttribute(STATE_POST,reqProps);
 			addAlert(state, (String) retval);
 			state.setAttribute(STATE_ID,id);
 			return;
 		}
 
 		state.setAttribute(STATE_SUCCESS,success);
 		switchPanel(state, "Mapping");
 	}
 
 	public void doMappingDelete(RunData data, Context context)
 	{
 
 		String peid = ((JetspeedRunData) data).getJs_peid();
 		SessionState state = ((JetspeedRunData) data).getPortletSessionState(peid);
 		
 		if ( ! ltiService.isAdmin() ) {
 		        addAlert(state,rb.getString("error.admin.delete"));
                         switchPanel(state,"Error");
 		        return;
 		}
 		Properties reqProps = data.getParameters().getProperties();
 		String id = data.getParameters().getString("id");
 		Object retval = null;
                 if ( id == null ) {
                         addAlert(state,rb.getString("error.id.not.found"));
                         switchPanel(state, "Mapping");
                         return;
                 }
                 Long key = new Long(id);
 		if ( ltiService.deleteMapping(key) )
 		{
 		        state.setAttribute(STATE_SUCCESS,rb.getString("success.deleted"));
 		        switchPanel(state, "Mapping");
                 } else {
                         addAlert(state,rb.getString("error.delete.fail"));
                         switchPanel(state, "Mapping");
                 }
 	}
 	
 	/** Content related methods ------------------------------ */
 	
 	public String buildContentPanelContext(VelocityPortlet portlet, Context context, 
 			RunData data, SessionState state)
 	{
 		context.put("tlang", rb);
 		if ( ! ltiService.isMaintain() ) {
 		        addAlert(state,rb.getString("error.maintain.view"));
 		        return "lti_error";
 		}
 		List<Map<String,Object>> contents = ltiService.getContents(null,null,0,0);
 		context.put("contents", contents);
 		context.put("messageSuccess",state.getAttribute(STATE_SUCCESS));
 		context.put("isAdmin",new Boolean(ltiService.isAdmin()) );
 		state.removeAttribute(STATE_SUCCESS);
 		return "lti_content";
 	}
 	
 	
 	public String buildContentPutPanelContext(VelocityPortlet portlet, Context context, 
 			RunData data, SessionState state)
 	{
 	        context.put("tlang", rb);
 		String stateToolId = (String) state.getAttribute(STATE_TOOL_ID);
 		if ( ! ltiService.isMaintain() ) {
 		        addAlert(state,rb.getString("error.maintain.edit"));
 		        return "lti_error";
 		}
 		context.put("isAdmin",new Boolean(ltiService.isAdmin()) );
                 context.put("doAction", BUTTON + "doContentPut");
 		state.removeAttribute(STATE_SUCCESS);
 
 		List<Map<String,Object>> tools = ltiService.getTools(null,null,0,0);
 		context.put("tools", tools);
 
                Long key = null;
                 Object previousData = null;
 
 		String contentId = data.getParameters().getString("id");
 		if ( contentId == null ) contentId = (String) state.getAttribute(STATE_CONTENT_ID);
                if ( contentId == null ) {
		        String toolId = data.getParameters().getString("tool_id");
	                if ( toolId == null ) toolId = stateToolId;
		        if ( toolId == null ) {
                                return "lti_content_insert";
		        }
        		key = new Long(toolId);
         		previousData = (Properties) state.getAttribute(STATE_POST);
                 } else {
 		        Long contentKey = new Long(contentId);
                         Map<String,Object> content = ltiService.getContent(contentKey);
                         if ( content == null ) {
                                 addAlert(state, rb.getString("error.content.not.found"));
                                 state.removeAttribute(STATE_CONTENT_ID);
                                 return "lti_content";
                         }
                        key = new Long((Integer)content.get("tool_id"));
                         previousData = content;
                 }
 
                 // We will handle the tool_id field ourselves in the Velocity code
 		String [] contentForm = foorm.filterForm(null,ltiService.getContentModel(key), null, "^tool_id:.*");
                 if ( contentForm == null || key == null ) {
                         addAlert(state,rb.getString("error.tool.not.found"));
                         return "lti_error";
                 }
 	        String formInput = ltiService.formInput(previousData, contentForm);
 
 		context.put("formInput",formInput);
                 context.put("tool_id",key);
 		return "lti_content_insert";
 	}
 
 	// Insert or edit
 	public void doContentPut(RunData data, Context context)
 	{

 		String peid = ((JetspeedRunData) data).getJs_peid();
 		SessionState state = ((JetspeedRunData) data).getPortletSessionState(peid);
 		
 		if ( ! ltiService.isMaintain() ) {
 		        addAlert(state,rb.getString("error.maintain.edit"));
 		        switchPanel(state,"Error");
 		        return;
 		}
 		Properties reqProps = data.getParameters().getProperties();
 		String id = data.getParameters().getString("id");
 		String toolId = data.getParameters().getString("tool_id");
                 if ( toolId == null ) {
                         addAlert(state, rb.getString("error.id.not.found"));
                         return;
                 }
                 
 		Object retval = null;
 		String success = null;
 		if ( id == null ) 
 		{
 	                retval = ltiService.insertContent(reqProps);
 	                success = rb.getString("success.created");
 		} else {
 			Long key = new Long(id);
                         // TODO: Error check this
 			retval = ltiService.updateContent(key, reqProps);
                         success = rb.getString("success.updated");
                 }
                 
                 if ( retval instanceof String ) 
 		{
 	                state.setAttribute(STATE_POST,reqProps);
 			addAlert(state, (String) retval);
 			state.setAttribute(STATE_CONTENT_ID,id);
 			return;
 		}
 
 		state.setAttribute(STATE_SUCCESS,success);
 		switchPanel(state, "Content");
 	}
 
 	public String buildContentDeletePanelContext(VelocityPortlet portlet, Context context, 
 			RunData data, SessionState state)
 	{
 		context.put("tlang", rb);
 		if ( ! ltiService.isMaintain() ) {
 		        addAlert(state,rb.getString("error.maintain.delete"));
 		        return "lti_error";
 		}
                 context.put("doAction", BUTTON + "doContentDelete");
 		String id = data.getParameters().getString("id");
 		if ( id == null ) {
 		        addAlert(state,rb.getString("error.id.not.found"));
 		        return "lti_main";
 		}	
 		Long key = new Long(id);
 		Map<String,Object> content = ltiService.getContent(key);
 		if (  content == null ) {
 		        addAlert(state,rb.getString("error.content.not.found"));
 		        return "lti_main";
 		}
 		context.put("content",content);
 		state.removeAttribute(STATE_SUCCESS);
 		return "lti_content_delete";
 	}
 
         // Insert or edit
 	public void doContentDelete(RunData data, Context context)
 	{
 
 		String peid = ((JetspeedRunData) data).getJs_peid();
 		SessionState state = ((JetspeedRunData) data).getPortletSessionState(peid);
 		
 		if ( ! ltiService.isMaintain() ) {
 		        addAlert(state,rb.getString("error.maintain.delete"));
                         switchPanel(state, "Error");
 		        return;
 		}
 		Properties reqProps = data.getParameters().getProperties();
 		String id = data.getParameters().getString("id");
 		Object retval = null;
                 if ( id == null ) {
                         addAlert(state,rb.getString("error.id.not.found"));
                         switchPanel(state, "Content");
                         return;
                 }
                 Long key = new Long(id);
 		if ( ltiService.deleteContent(key) )
 		{
 		        state.setAttribute(STATE_SUCCESS,rb.getString("success.deleted"));
 		        switchPanel(state, "Content");
                 } else {
                         addAlert(state,rb.getString("error.delete.fail"));
                         switchPanel(state, "Content");
                 }
 	}
 
         public String buildTestPanelContext(VelocityPortlet portlet, Context context, 
 		RunData rundata, SessionState state)
 	{
 	    context.put("tlang", rb);
 	    if ( ! ltiService.isAdmin() ) {
 	        addAlert(state,rb.getString("error.admin.view"));
                 return "lti_error";
             }
             StringBuffer sb = new StringBuffer();
 
             try { 
                 // Get a list of tools
 		List<Map<String,Object>> tools = ltiService.getTools(null,null,0,0);
 		sb.append(""+tools.size()+" tools available\n");
 		if ( tools.size() > 2 ) {
 	                sb.append("Tools 1-2 (zero-based):\n");
 			tools = ltiService.getTools(null,null,1,2);
 		} else {
 	                sb.append("Tools 0-1 (first two):\n");
 			tools = ltiService.getTools(null,null,0,1);
 		}
 
                 for (Map<String, Object> tool : tools ) {
                         sb.append("  Tool\n");
                         for ( String key : tool.keySet() ) {
                                 sb.append("     ");
                                 sb.append(key);
                                 sb.append("=");
                                 Object obj = tool.get(key);
                                 if ( obj == null ) sb.append("null"); else sb.append(obj.toString());
                                 sb.append("\n");
                         }
                 }
 
                 if ( tools.size() < 1 ) {
                         sb.append("\n--No Tools Exist---\n");
 		        context.put("preOutput",sb.toString());
 		        return "lti_test";
                 }
 
                 // Lets grab the tool key...
                 Map<String,Object> tool = tools.get(0);
 
                 // We will assume this works
                 Long toolKey = foorm.getLongKey(tool.get("id"));
 
                 sb.append("Long Tool Key=");
                 sb.append(toolKey.toString());
                 sb.append("\n\n");
                 sb.append("Our Context=");
                 sb.append(toolManager.getCurrentPlacement().getContext());
                 sb.append("\n\n");
                 sb.append("Raw/underlying content model (String [])\n");
                 for(String field : ltiService.CONTENT_MODEL) {
                         sb.append("  ");
                         sb.append(field);
                         sb.append("\n");
                 }
                 sb.append("\n");
 
                 String [] contentModel = ltiService.getContentModel(toolKey);
                 sb.append("Properly filtered content model (String [])\n");
                 for(String field : contentModel) {
                         sb.append("  ");
                         sb.append(field);
                         sb.append("\n");
                 }
 
                 // Lets do this with properties (i.e. from a Request Object)
                 Properties props = new Properties ();
                 props.setProperty("SITE_ID",toolManager.getCurrentPlacement().getContext());
                 props.setProperty("tool_id",toolKey.toString());
                 props.setProperty("title", "A title");
                 props.setProperty("description", "A title");
                 props.setProperty("debug", "0"); 
                 
                 sb.append("\nConstructed content Properties for insert\n");
                 for ( Object okey : props.keySet() ) {
                         String key = (String) okey;
                         sb.append("     ");
                         sb.append(key);
                         sb.append("=");
                         String obj = props.getProperty(key);
                         if ( obj == null ) sb.append("null"); else sb.append(obj.toString());
                         sb.append("\n");
                 }
 
                 sb.append("Inserting from Properties...\n");
                 Object result = ltiService.insertContent(props);
                 Long contentKey = null;
                 if ( result instanceof String ) {
                         sb.append("Insert failed:");
                         sb.append((String) result);
                         sb.append("\n");
                         context.put("preOutput",sb.toString());
                         return "lti_test";
                 } else {
                         contentKey = (Long) result;
                         sb.append("Returned key=");
                         sb.append(contentKey.toString() );
                         sb.append("\n");
                 }
 
                 if ( contentKey < 0 ) {
                         sb.append("\nBad key returned (might be HSQL ickiness)\n");
                         context.put("preOutput",sb.toString());
                         return "lti_test";
                 }
 
                 sb.append("Retrieving content key=");
                 sb.append(contentKey.toString());
                 sb.append("\n");
 
                 Map<String,Object> contentMap = ltiService.getContent(contentKey);
                 if ( contentMap == null ) {
                         sb.append("getContent failed\n");
                         context.put("preOutput",sb.toString());
                         return "lti_test";
                 }
 
                 sb.append("\nRetrieved content\n");
                 for ( String key : contentMap.keySet() ) {
                         sb.append("  ");
                         sb.append(key);
                         sb.append("=");
                         Object obj = tool.get(key);
                         if ( obj == null ) sb.append("null"); else sb.append(obj.toString());
                         sb.append("\n");
                 }
 
                 // Lets update the content (as if we just got a form)
                 // We don't need all properties - just the ones we want to update
                 // tool_id is required
                 props = new Properties ();
                 props.setProperty("title", "A NEW AWESOME TITLE");
                 props.setProperty("tool_id",toolKey.toString());
 
                 sb.append("\nConstructed content Properties for update\n");
                 for ( Object okey : props.keySet() ) {
                         String key = (String) okey;
                         sb.append("     ");
                         sb.append(key);
                         sb.append("=");
                         String obj = props.getProperty(key);
                         if ( obj == null ) sb.append("null"); else sb.append(obj.toString());
                         sb.append("\n");
                 }
 
                 sb.append("Updating...");
                 // Object retval
                 result = ltiService.updateContent(contentKey, props);
                 if ( result instanceof String ) {
                         sb.append("Update failed:");
                         sb.append((String) result);
                         sb.append("\n");
                         context.put("preOutput",sb.toString());
                         return "lti_test";
                 } else {
                         sb.append("Update success\n");
                 }
 
                 sb.append("\nRe-retrieving content to verify update key=");
                 sb.append(contentKey.toString());
                 sb.append("\n");
 
                 // Map<String,Object>
                 contentMap = ltiService.getContent(contentKey);
                 if ( contentMap == null ) {
                         sb.append("getContent failed\n");
                         context.put("preOutput",sb.toString());
                         return "lti_test";
                 }
 
                 sb.append("\nRetrieved content to verify update success\n");
                 for ( String key : contentMap.keySet() ) {
                         sb.append("  ");
                         sb.append(key);
                         sb.append("=");
                         Object obj = contentMap.get(key);
                         if ( obj == null ) sb.append("null"); else sb.append(obj.toString());
                         sb.append("\n");
                 }
 
                 // Lets make an input form if you want this auto generated for free
                 sb.append("\nAn Input Form\n");
                 String formInp = ltiService.formInput(contentMap, contentModel);
                 sb.append(formInp.replace("><",">\n<").replace("<","&lt;").replace(">","&gt"));
 
                 sb.append("Lets make some mistakes\n");
 
                 // Properties 
                 props = new Properties ();
                 props.setProperty("SITE_ID",toolManager.getCurrentPlacement().getContext());
                 props.setProperty("tool_id","I should be an integer!");
                 props.setProperty("title", "A title");
                 
                 sb.append("\nConstructed broken content Properties for insert\n");
                 for ( Object okey : props.keySet() ) {
                         String key = (String) okey;
                         sb.append("     ");
                         sb.append(key);
                         sb.append("=");
                         String obj = props.getProperty(key);
                         if ( obj == null ) sb.append("null"); else sb.append(obj.toString());
                         sb.append("\n");
                 }
 
                 sb.append("Inserting from Properties...\n");
 
                 // OBJECT
                 result = ltiService.insertContent(props);
                 if ( result instanceof String ) {
                         sb.append("Insert correctly failed:");
                         sb.append((String) result);
                         sb.append("\n");
                 }
 
                 sb.append("\nLets forget a required parameter on an update...\n");
                 props = new Properties ();
                 props.setProperty("title", "YET ANOTHER AWESOME TITLE");
                 // FORGET ME: props.setProperty("tool_id",toolKey.toString());
 
                 sb.append("\nConstructed broken content Properties for update\n");
                 for ( Object okey : props.keySet() ) {
                         String key = (String) okey;
                         sb.append("     ");
                         sb.append(key);
                         sb.append("=");
                         String obj = props.getProperty(key);
                         if ( obj == null ) sb.append("null"); else sb.append(obj.toString());
                         sb.append("\n");
                 }
 
                 sb.append("\nUpdating......\n");
                 // Object retval
                 result = ltiService.updateContent(contentKey, props);
                 if ( result instanceof String ) {
                         sb.append("Update failed properly:");
                         sb.append((String) result);
                         sb.append("\n");
                 } else {
                         sb.append("Update success - not what we wanted to happen\n");
                 }
 
       // Lets delete the content to clean things up (if we got this far)
                 sb.append("\nALL DONE... CLEANUP TIME...\nDeleting key=");
                 sb.append(contentKey.toString());
                 sb.append("\n");
                 boolean retval = ltiService.deleteContent(contentKey);
                 sb.append("Return value from delete="+retval);
                 sb.append("\n");
             } catch (Exception e) {
                 sb.append(e.getMessage());
                 sb.append(getStackTrace(e));
             }
 
 		context.put("preOutput",sb.toString());
 		return "lti_test";
 	}
 
         public static String getStackTrace(Throwable throwable) {
                 Writer writer = new StringWriter();
                 PrintWriter printWriter = new PrintWriter(writer);
                 throwable.printStackTrace(printWriter);
                 return writer.toString();
          }
 
 }
