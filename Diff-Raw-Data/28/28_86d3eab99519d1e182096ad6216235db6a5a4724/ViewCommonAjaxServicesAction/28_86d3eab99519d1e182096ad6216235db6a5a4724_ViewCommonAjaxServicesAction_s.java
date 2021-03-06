 /* ===============================================================================
  *
  * Part of the InfoGlue Content Management Platform (www.infoglue.org)
  *
  * ===============================================================================
  *
  *  Copyright (C)
  * 
  * This program is free software; you can redistribute it and/or modify it under
  * the terms of the GNU General Public License version 2, as published by the
  * Free Software Foundation. See the file LICENSE.html for more information.
  * 
  * This program is distributed in the hope that it will be useful, but WITHOUT
  * ANY WARRANTY, including the implied warranty of MERCHANTABILITY or FITNESS
  * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
  * 
  * You should have received a copy of the GNU General Public License along with
  * this program; if not, write to the Free Software Foundation, Inc. / 59 Temple
  * Place, Suite 330 / Boston, MA 02111-1307 / USA.
  *
  * ===============================================================================
  */
 
 package org.infoglue.cms.applications.common.actions;
 
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 
 import org.apache.log4j.Logger;
 import org.infoglue.cms.applications.databeans.ReferenceBean;
 import org.infoglue.cms.applications.databeans.ReferenceVersionBean;
 import org.infoglue.cms.controllers.kernel.impl.simple.ContentCategoryController;
 import org.infoglue.cms.controllers.kernel.impl.simple.ContentController;
 import org.infoglue.cms.controllers.kernel.impl.simple.DigitalAssetController;
 import org.infoglue.cms.controllers.kernel.impl.simple.RegistryController;
 import org.infoglue.cms.controllers.kernel.impl.simple.RepositoryController;
 import org.infoglue.cms.controllers.kernel.impl.simple.SiteNodeController;
 import org.infoglue.cms.entities.content.ContentVO;
 import org.infoglue.cms.entities.content.ContentVersionVO;
 import org.infoglue.cms.entities.kernel.BaseEntityVO;
 
 /**
  * This class implements the action class for the framed page in the content tool.
  * 
  * @author Mattias Bogeblad  
  */
 
 public class ViewCommonAjaxServicesAction extends InfoGlueAbstractAction
 {
     private final static Logger logger = Logger.getLogger(ViewCommonAjaxServicesAction.class.getName());
 
 	private static final long serialVersionUID = 1L;
 
 	private List repositories = null;
 	
 	public String doRepositories() throws Exception
     {
 		this.repositories = RepositoryController.getController().getAuthorizedRepositoryVOList(getInfoGluePrincipal(), false);
         
 		return "successRepositories";
     }
 
 	public String doSiteNodeIdPath() throws Exception
     {
 		String siteNodeIdPath = SiteNodeController.getController().getSiteNodeIdPath(new Integer(getRequest().getParameter("siteNodeId")));
 		this.getResponse().setContentType("text/plain");
 		this.getResponse().getWriter().print("" + siteNodeIdPath);
 		
 		return NONE;
     }
 
 	public String doSiteNodePath() throws Exception
     {
 		String siteNodePath = SiteNodeController.getController().getSiteNodePath(new Integer(getRequest().getParameter("siteNodeId")), false, true);
 		this.getResponse().setContentType("text/plain");
 		this.getResponse().getWriter().print("" + siteNodePath);
 		
 		return NONE;
     }
 
 	public String doContentIdPath() throws Exception
     {
 		String contentIdPath = ContentController.getContentController().getContentIdPath(new Integer(getRequest().getParameter("contentId")));
 		this.getResponse().setContentType("text/plain");
 		this.getResponse().getWriter().print("" + contentIdPath);
 		
 		return NONE;
     }
 
 	public String doContentPath() throws Exception
     {
 		String contentPath = ContentController.getContentController().getContentPath(new Integer(getRequest().getParameter("contentId")), false, true);
 		this.getResponse().setContentType("text/plain");
 		this.getResponse().getWriter().print("" + contentPath);
 		
 		return NONE;
     }
 
 	public String doRepositoryName() throws Exception
     {
		String repositoryName = null;
 		Integer repositoryId = null;
 		if(getRequest().getParameter("repositoryId") != null && getRequest().getParameter("repositoryId").equals(""))
 			repositoryId = new Integer(getRequest().getParameter("repositoryId"));
 		if(repositoryId == null)
 		{
 			String toolName = getRequest().getParameter("toolName");
 			if(toolName.equals("ContentTool"))
 			{
 				repositoryId = getContentRepositoryId();
 			}
 			else if(toolName.equals("StructureTool"))
 			{
 				repositoryId = getStructureRepositoryId();
 			}
 		}

		repositoryName = RepositoryController.getController().getRepositoryVOWithId(new Integer(repositoryId)).getName();
 		
 		this.getResponse().setContentType("text/plain");
 		this.getResponse().getWriter().print("" + repositoryName);
 		
 		return NONE;
     }
 
 	public String doReferenceCount() throws Exception
     {
 		List<Integer> uniqueList = new ArrayList<Integer>();
 		List<ReferenceBean> refList = RegistryController.getController().getReferencingObjectsForContent(new Integer(getRequest().getParameter("contentId")), 100, true, true);
 		for(ReferenceBean bean : refList)
 		{
 			if(bean.getReferencingCompletingObject() instanceof ContentVO)
 			{
 				for(ReferenceVersionBean versionBean : bean.getVersions())
 				{
 					uniqueList.add(((BaseEntityVO)versionBean.getReferencingObject()).getId());
 				}
 			}
 			else
 			{
 				uniqueList.add(((BaseEntityVO)bean.getReferencingCompletingObject()).getId());
 			}
 		}
 		this.getResponse().setContentType("text/plain");
 		this.getResponse().getWriter().print("" + uniqueList.size());
 		
 		return NONE;
     }
 
 	public String doAssetCount() throws Exception
     {
 		List digitalAssets = DigitalAssetController.getDigitalAssetVOList(new Integer(getRequest().getParameter("contentVersionId")));
 
 		this.getResponse().setContentType("text/plain");
 		this.getResponse().getWriter().print("" + digitalAssets.size());
 		
 		return NONE;
     }
 
 	public String doCategoryCount() throws Exception
     {
 		List categories = ContentCategoryController.getController().findByContentVersion(new Integer(getRequest().getParameter("contentVersionId")));
 		
 		this.getResponse().setContentType("text/plain");
 		this.getResponse().getWriter().print("" + categories.size());
 		
 		return NONE;
     }
 
 	public String doExecute() throws Exception
     {
 		
         return "success";
     }
 
 	
 	public List getRepositories()
 	{
 		return repositories;
 	}
 
 }
