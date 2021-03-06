 /*******************************************************************************
  * Copyright (c) 2004, 2010 Robert Munteanu and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Robert Munteanu - initial API and implementation
  *******************************************************************************/
 package com.itsolut.mantis.core;
 
 import org.eclipse.core.runtime.Assert;
 
 /**
  * @author Robert Munteanu
  * 
  */
 public class MantisRepositoryLocations {
 
     private static final String TICKET_ATTACHMENT_URL = "/file_download.php?type=bug&file_id=";
 
     private static final String URL_SHOW_BUG = "/view.php?id=";
 
     private final String baseRepositoryUrl;
 
     public static Integer extractTaskId(String rawUrl) {
 
         Assert.isNotNull(rawUrl);
 
        int index = rawUrl.lastIndexOf(URL_SHOW_BUG);
 
        return index == -1 ? null : Integer.parseInt(rawUrl.substring(index + URL_SHOW_BUG.length()));
     }
 
     public static MantisRepositoryLocations create(String rawUrl) {
 
         Assert.isNotNull(rawUrl);
 
         String repositoryUrl;
 
         if (rawUrl.endsWith(MantisAxis1SOAPClient.SOAP_API_LOCATION))
             repositoryUrl = rawUrl.substring(0, rawUrl.length() - MantisAxis1SOAPClient.SOAP_API_LOCATION.length() + 1);
         else if (rawUrl.indexOf(URL_SHOW_BUG) != -1)
             repositoryUrl = rawUrl.substring(0, rawUrl.indexOf(URL_SHOW_BUG) + 1);
         else
             repositoryUrl = rawUrl;
 
         return new MantisRepositoryLocations(repositoryUrl);
 
     }
 
     private MantisRepositoryLocations(String baseRepositoryUrl) {
 
         this.baseRepositoryUrl = baseRepositoryUrl;
     }
 
     public String getBaseRepositoryLocation() {
 
         return baseRepositoryUrl;
     }
 
     public String getSoapApiLocation() {
 
         return join(baseRepositoryUrl, MantisAxis1SOAPClient.SOAP_API_LOCATION);
     }
 
     private String join(String first, String second) {
 
         if (first.endsWith("/") && second.startsWith("/"))
             return first + second.substring(1);
 
         return first + second;
     }
 
     public String getTaskLocation(Integer taskId) {
 
         return join(baseRepositoryUrl, URL_SHOW_BUG + taskId);
     }
 
     public String getAttachmentDownloadLocation(Integer attachmentId) {
 
         return join(baseRepositoryUrl, TICKET_ATTACHMENT_URL + attachmentId);
     }
     
     public String getSignupLocation() {
         
         return join(baseRepositoryUrl, "/signup_page.php");
     }
     
     public String getAccountManagementLocation() {
         
         return join(baseRepositoryUrl, "/account_page.php");
     }
 }
