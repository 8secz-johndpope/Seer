 /*
  ************************************************************************************
  * Copyright (C) 2001-2009 encuestame: system online surveys Copyright (C) 2009
  * encuestame Development Team.
  * Licensed under the Apache Software License version 2.0
  * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to  in writing,  software  distributed
  * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
  * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
  * specific language governing permissions and limitations under the License.
  ************************************************************************************
  */
 package org.encuestame.web.beans.admon;
 
 import java.io.Serializable;
 import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.StringTokenizer;
 
 import org.encuestame.core.exception.EnMeExpcetion;
 import org.encuestame.core.security.util.EmailUtils;
 import org.encuestame.utils.web.UnitPermission;
 import org.encuestame.utils.web.UnitUserBean;
 import org.encuestame.web.beans.MasterBean;
 import org.hibernate.HibernateException;
 import org.springframework.mail.MailSendException;
 
 /**
  * Security User Bean.
  * @author Picado, Juan juan@encuestame.org
  * @since 11/05/2009 13:52:28
  * @version $Id$
  */
 
 public class UserBean  extends MasterBean implements Serializable {
 
     private static final long serialVersionUID = -391208809931131195L;
     private UnitUserBean unitUserBean = new UnitUserBean();
     private UnitUserBean newUnitUserBean = new UnitUserBean();
     private String processedUserId;
     private Long selectedPermissionId;
     private String selectedAction;
     private String listUsers;
 
     /**
      * Create secondary user, is notificated is desactivated the password is returned and should be,
      * showed on screen.
      */
     public final void createUser(final String username) {
         try {
             log.info("username logged "+username);
             getSecurityService().createUser(getNewUnitUserBean(), username);
             addInfoMessage("User "+getNewUnitUserBean().getUsername()+" saved", "");
         } catch (EnMeExpcetion e) {
             addErrorMessage(e.getMessage(), e.getMessage());
         }
     }
 
     /**
      * Validate Email.
      */
     public final void validateEmail(){
         if(getSecurityService().searchUsersByEmail(getNewUnitUserBean().getEmail()).size() > 0){
             log.info("email valid");
         }
         else{
             log.info("email not valid");
         }
     }
 
     /**
      * Validate Username.
      */
     public final void validateUserName(){
        if(getSecurityService().searchUsersByEmail(getNewUnitUserBean().getUsername()).size() > 0){
             log.info("username valid");
         }
         else{
             log.info("username not valid");
         }
     }
 
     /**
      * Update secondary user.
      */
     public final void updateUser() {
         log.debug("update secondary user.");
         try {
             getServicemanager().getApplicationServices().getSecurityService().updateUser(
                     getUnitUserBean());
             addInfoMessage("User "+getNewUnitUserBean().getUsername()+" updated", "");
         } catch (Exception e) {
             addErrorMessage("error update user ", e.getMessage());
             log.error("error update user: " + e);
         }
     }
 
     /**
      * Invite user.
      */
     public final void inviteUser() {
         if (!getListUsers().isEmpty()) {
             final List<String> emails = new LinkedList<String>();
             String strDatos = getListUsers().trim();
             StringTokenizer tokens = new StringTokenizer(strDatos, ",");
             int i = 0;
             while (tokens.hasMoreTokens()) {
                 String str = tokens.nextToken();
                 emails.add(str.trim());
                 i++;
             }
             if (emails.size() > 0) {
                 Iterator<String> it = emails.iterator();
                 while (it.hasNext()) {
                     String email = (String) it.next();
                     if (EmailUtils.validateEmail(email)) {
                         try {
                             String code = getServicemanager()
                                     .getApplicationServices()
                                     .getSecurityService()
                                     .generateHashCodeInvitation();
                             getServicemanager()
                             .getApplicationServices()
                             .getSecurityService()
                                     .inviteUser(email, code);
                             addInfoMessage("Invitacion enviada para " + email
                                     + " Satisfactoriamente", "");
                         } catch (Exception e) {
                             addErrorMessage(
                                     "Lo siento,ocurrio un error al enviar este correo->"
                                             + email + " error->" + e, e
                                             .getMessage());
                         }
                     } else {
                         log.info("email invalido ->" + email);
                         addWarningMessage("invalid email: " + email, "");
                     }
                 }
                 setListUsers(null);
             } else {
                 addWarningMessage("sorry, no results", "");
             }
         } else {
             addWarningMessage("sorry, no results", "");
         }
     }
 
     /**
      * Search LDAP user.
      */
     public final void searchLDAPUser() {
         //TODO: need implement ldap search.
     }
 
     /**
      * Assing permissions to secondary user.
      */
     public final void assingPermissions() {
 
     }
 
     /**
      * Assing permission to user.
      * @param user  user
      * @param permission  permission
      * @throws EnMeExpcetion  if the default permission dont exist
      * @throws HibernateException error db
      */
     private void assingPermission(
             final UnitUserBean user,
             final UnitPermission permission)
             throws EnMeExpcetion{
         getServicemanager().getApplicationServices().getSecurityService().assignPermission(user,
                 permission);
 
     }
 
 
     /**
      * Delete user.
      * @param user {@link UnitUserBean}
      */
     private void deleteUser(final UnitUserBean user) {
         try {
             getServicemanager().getApplicationServices().getSecurityService().deleteUser(user);
             log.debug("user "+user.getUsername()+" deleted");
         } catch (Exception e) {
             log.error("Error on delete user. Trace:"+ e.getMessage());
             addErrorMessage("Error on delete user","");
         }
     }
 
     /**
      * Renew password.
      * @param user {@link UnitUserBean}
      */
     private void renewPassword(final UnitUserBean user) {
         try {
             getServicemanager().getApplicationServices().getSecurityService().renewPassword(user, user.getPassword());
         } catch (MailSendException e) {
             log.info("No recordo bien la contrase�a a->" + user.getUsername());
             addErrorMessage("No pudo recordar a ->" + user.getUsername()
                     + "por->" + e.getMessage(), "");
         } catch (Exception e) {
             addErrorMessage("Otro Error renewPassword a ->"
                     + user.getUsername() + "por->" + e.getMessage(), "");
         }
     }
 
     /**
      * Execute Actions for all user selected.
      */
     public final void initAction() {
         log.debug("action selected->" + getSelectedAction());
 
     }
 
     /**
      * @param unitUserBean {@link UnitUserBean}
      */
     public final void setUnitUserBean(final UnitUserBean unitUserBean) {
         log.info("setUnitUserBean->" + unitUserBean);
         this.unitUserBean = unitUserBean;
     }
 
     /**
      * @return id
      */
     public final String getProcessedUserId() {
         return processedUserId;
     }
 
     /**
      * @param processedUserId id.
      */
     public final void setProcessedUserId(final String processedUserId) {
         this.processedUserId = processedUserId;
     }
 
     /**
      * @return the selectedAction
      */
     public final String getSelectedAction() {
         return selectedAction;
     }
 
     /**
      * @param selectedAction
      *            the selectedAction to set
      */
     public final void setSelectedAction(final String selectedAction) {
         this.selectedAction = selectedAction;
     }
 
     /**
      * Getter.
      * @return {@link UnitUserBean}
      */
     public final UnitUserBean getUnitUserBean() {
         return unitUserBean;
     }
 
     /**
      * Load user selected in datatable
      */
     public final void loadSelectUser() {
         try {
             if (getProcessedUserId() != null) {
                 unitUserBean = null;
                 UnitUserBean unitUserBeanLocal = getServicemanager()
                         .getApplicationServices().getSecurityService().searchUserByUsername(
                                 getProcessedUserId());
                 setUnitUserBean(unitUserBeanLocal);
             } else {
                 addErrorMessage(
                         "Lo siento, no se pudo cargar la info del usuario", "");
             }
         } catch (Exception e) {
             addErrorMessage("Error Cargando Datos Usuario"
                     + getProcessedUserId(), "");
             log.error("Error Cargando Datos Usuario " + e.getMessage());
         }
     }
 
     /**
      * @return sda
      */
     public final UnitUserBean getNewUnitUserBean() {
         return newUnitUserBean;
     }
 
     /**
      * @param newUnitUserBean das
      */
     public final void setNewUnitUserBean(final UnitUserBean newUnitUserBean) {
         this.newUnitUserBean = newUnitUserBean;
     }
 
     /**
      * @return dsa
      */
     public final String getListUsers() {
         return listUsers;
     }
 
     /**
      * @param listUsers das
      */
     public final void setListUsers(final String listUsers) {
         this.listUsers = listUsers;
     }
 
     /**
      * @return the selectedPermissionId
      */
     public final Long getSelectedPermissionId() {
         return selectedPermissionId;
     }
 
     /**
      * @param selectedPermissionId
      *            the selectedPermissionId to set
      */
     public final void setSelectedPermissionId(final Long selectedPermissionId) {
         this.selectedPermissionId = selectedPermissionId;
     }
 }
