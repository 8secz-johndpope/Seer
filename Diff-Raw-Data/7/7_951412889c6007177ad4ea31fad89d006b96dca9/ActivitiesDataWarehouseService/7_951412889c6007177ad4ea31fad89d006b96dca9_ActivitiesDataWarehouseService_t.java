 /*
  * Copyright (c) 2010 - 2011 Open Source Strategies, Inc.
  *
  * Opentaps is free software: you can redistribute it and/or modify it
  * under the terms of the GNU Affero General Public License as published
  * by the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * Opentaps is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.
  *
  * You should have received a copy of the GNU Affero General Public License
  * along with Opentaps.  If not, see <http://www.gnu.org/licenses/>.
  */
 package com.opensourcestrategies.activities;
 
 import java.sql.Timestamp;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 
 import org.ofbiz.base.util.Debug;
 import org.ofbiz.entity.GenericEntityException;
 import org.ofbiz.entity.condition.EntityCondition;
 import org.ofbiz.entity.condition.EntityOperator;
 import org.opentaps.base.constants.RoleTypeConstants;
 import org.opentaps.base.constants.StatusItemConstants;
 import org.opentaps.base.constants.WorkEffortPurposeTypeConstants;
 import org.opentaps.base.entities.ActivityFact;
 import org.opentaps.base.entities.UserLogin;
 import org.opentaps.base.entities.WorkEffort;
 import org.opentaps.base.entities.WorkEffortPartyAssignment;
 import org.opentaps.base.services.ActivitiesTransformToActivityFactsService;
 import org.opentaps.common.reporting.etl.UtilEtl;
 import org.opentaps.domain.DomainService;
 import org.opentaps.domain.activities.Activity;
 import org.opentaps.domain.activities.ActivityRepositoryInterface;
 import org.opentaps.domain.party.Party;
 import org.opentaps.domain.party.PartyRepositoryInterface;
 import org.opentaps.foundation.entity.EntityNotFoundException;
 import org.opentaps.foundation.repository.RepositoryException;
 import org.opentaps.foundation.service.ServiceException;
 import org.ofbiz.base.util.UtilValidate;
 
 /**
  * Do data warehouse operations for activities.
  */
 public class ActivitiesDataWarehouseService extends DomainService {
 
     private static final String MODULE = ActivitiesDataWarehouseService.class.getName();
 
     private String workEffortId;
 
     /**
      * Sets the required input parameter for service {@link #transformToActivityFacts}.
      * @param workEffortId the ID of work effort
      */
     public void setWorkEffortId(String workEffortId) {
         this.workEffortId = workEffortId;
     }
 
     /**
      * Transformation wich transforms data from WorkEffort, WorkEffortPartyAssign entities to ActivityFact entities.
      *  It expands the WorkEffortPartyAssign to cover all target parties for all team members.
      *  The counts that is in ActivityFacts is based on the purpose of the WorkEffort.
      * @throws ServiceException if an error occurs
      */
     public void transformToActivityFacts() throws ServiceException {
         try {
 
             PartyRepositoryInterface repository = getDomainsDirectory().getPartyDomain().getPartyRepository();
 
             // Get WorkEffortPartyAssign and WorkEffort data by workEffortId.
 
             WorkEffort workEffort = repository.findOne(WorkEffort.class, repository.map(WorkEffort.Fields.workEffortId, workEffortId));
 
             // Pass only completed workEfforts to do the transformation.
             if (!Arrays.asList(StatusItemConstants.TaskStatus.TASK_COMPLETED, StatusItemConstants.EventStatus.EVENT_COMPLETED).contains(workEffort.getCurrentStatusId())) {
                 Debug.logInfo("WorkEffort [" + workEffort.getWorkEffortId() + "] is not completed, not generating an activity fact", MODULE);
                 return;
             }
 
             // Fill 2 lists according to assigment of work effort to team members (internal parties) and clients (external parties).
 
             List<WorkEffortPartyAssignment> internalPartyAssignments = new ArrayList<WorkEffortPartyAssignment>();
             List<WorkEffortPartyAssignment> externalPartyAssignments = new ArrayList<WorkEffortPartyAssignment>();
             List<WorkEffortPartyAssignment> assignments = repository.findList(WorkEffortPartyAssignment.class, repository.map(WorkEffortPartyAssignment.Fields.workEffortId, workEffortId));
 
             if (assignments.size() < 2) {
                 Debug.logInfo("WorkEffort [" + workEffort.getWorkEffortId() + "] has only " + assignments.size() + " parties assigned, not generating an activity fact", MODULE);
                 return;
             }
 
             for (WorkEffortPartyAssignment assignment : assignments) {
                 // Note: a party can be both internal and external
                 //   in case of multi-tenant setup there is a case
                 //   where A B X Y are involved in a WorkEffort; A and B being supposed to be
                 //   internal (as in two sales rep) but B would be considered external if
                 //   he is a contact somewhere else.
                 //   All parties could be both have the contact role and be an internal user.
                 boolean isInternal = false; // is the party a user of the system
                 boolean isExternal = false; // is the party a CRM party
 
                 Party assignedParty = repository.getPartyById(assignment.getPartyId());
 
                 // always consider the current user as internal
                 if (assignedParty.getPartyId().equals(getUser().getOfbizUserLogin().getString(UserLogin.Fields.partyId.name()))) {
                     isInternal = true;
                 } else {
                     // if the party as a userLogin it is internal
                     if (UtilValidate.isNotEmpty(repository.findList(UserLogin.class, repository.map(UserLogin.Fields.partyId, assignedParty.getPartyId())))) {
                         isInternal = true;
                     }
                 }
 
                 if (assignedParty.isAccount()) {
                     isExternal = true;
                 } else if (assignedParty.isContact()) {
                     isExternal = true;
                 } else if (assignedParty.isLead()) {
                     isExternal = true;
                 } else if (assignedParty.isPartner()) {
                     isExternal = true;
                 }
 
                 Debug.logInfo("External = " + isExternal + " / Internal = " + isInternal + " for WorkEffortPartyAssignment [" + assignment.getWorkEffortId() + "] with party [" + assignment.getPartyId() + "]", MODULE);
 
                 if (isExternal) {
                     externalPartyAssignments.add(assignment);
                 }
                 if (isInternal) {
                     internalPartyAssignments.add(assignment);
                 }
             }
 
             if (externalPartyAssignments.size() == 0 || internalPartyAssignments.size() == 0) {
                 Debug.logError("Missing internal or external assignments for WorkEffort [" + workEffort.getWorkEffortId() + "] (found: " + internalPartyAssignments.size() + " internal and " + externalPartyAssignments.size() + " external)", MODULE);
                 return;
             }
 
             // Get date dimension ID according to the work effort start date.
             Timestamp workEffortDate = workEffort.getActualCompletionDate();
             if (workEffortDate == null) {
                 workEffortDate = workEffort.getEstimatedCompletionDate();
             }
 
             Long dateDimId = UtilEtl.lookupDateDimensionForTimestamp(workEffortDate, repository.getInfrastructure().getDelegator());
 
             // Associate all team member with clients (add this association if it is not there in the place)
             // and increase count according to WorkEffort workEffortPurposeTypeId.
 
             for (WorkEffortPartyAssignment external : externalPartyAssignments) {
 
                 // Find out what type is external party: is it lead, is it account, ...
 
                 String targetPartyRoleTypeId = null;
                 Party assignedParty = repository.getPartyById(external.getPartyId());
                 if (assignedParty.isAccount()) {
                     targetPartyRoleTypeId = RoleTypeConstants.ACCOUNT;
                 } else if (assignedParty.isContact()) {
                     targetPartyRoleTypeId = RoleTypeConstants.CONTACT;
                 } else if (assignedParty.isLead()) {
                     targetPartyRoleTypeId = RoleTypeConstants.LEAD;
                 } else if (assignedParty.isPartner()) {
                     targetPartyRoleTypeId = RoleTypeConstants.PARTNER;
                 }
 
                 for (WorkEffortPartyAssignment internal : internalPartyAssignments) {
 
                     // skip if it is the same party as the external one
                     if (external.getPartyId().equals(internal.getPartyId())) {
                         continue;
                     }
 
                     // Try to find ActivityFact with such target party id and member party id and date dimension combination.
                     // If not such, then create it.
 
                     EntityCondition partiesCond = EntityCondition.makeCondition(EntityOperator.AND,
                         EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), external.getPartyId()),
                         EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), internal.getPartyId()),
                         EntityCondition.makeCondition(ActivityFact.Fields.targetPartyRoleTypeId.name(), targetPartyRoleTypeId),
                         EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyRoleTypeId.name(), internal.getRoleTypeId()),
                         EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), dateDimId));
                     List<ActivityFact> activityFacts = repository.findList(ActivityFact.class, partiesCond);
 
                     ActivityFact activityFact = null;
                     if (activityFacts.size() > 0) {
                         activityFact = activityFacts.get(0);
                     } else {
                         activityFact = new ActivityFact();
                         activityFact.setActivityFactId(repository.getNextSeqId(activityFact));
                         activityFact.setTargetPartyId(external.getPartyId());
                         activityFact.setTeamMemberPartyId(internal.getPartyId());
                         activityFact.setDateDimId(dateDimId);
                         activityFact.setEmailActivityCount(Long.valueOf(0));
                         activityFact.setPhoneCallActivityCount(Long.valueOf(0));
                         activityFact.setVisitActivityCount(Long.valueOf(0));
                         activityFact.setOtherActivityCount(Long.valueOf(0));
                         activityFact.setTargetPartyRoleTypeId(targetPartyRoleTypeId);
                         activityFact.setTeamMemberPartyRoleTypeId(internal.getRoleTypeId());
                     }
 
                     // Increase count according to WorkEffort workEffortPurposeTypeId.
 
                     String purpose = workEffort.getWorkEffortPurposeTypeId();
                     if (purpose == null) {
                         activityFact.setOtherActivityCount(activityFact.getOtherActivityCount() + 1);
                     } else if (purpose.compareTo(WorkEffortPurposeTypeConstants.WEPT_TASK_EMAIL) == 0) {
                         activityFact.setEmailActivityCount(activityFact.getEmailActivityCount() + 1);
                     } else if (purpose.compareTo(WorkEffortPurposeTypeConstants.WEPT_TASK_PHONE_CALL) == 0) {
                         activityFact.setPhoneCallActivityCount(activityFact.getPhoneCallActivityCount() + 1);
                     } else if (purpose.compareTo(WorkEffortPurposeTypeConstants.WEPT_MEETING) == 0) {
                         activityFact.setVisitActivityCount(activityFact.getVisitActivityCount() + 1);
                     } else {
                         activityFact.setOtherActivityCount(activityFact.getOtherActivityCount() + 1);
                     }
 
                     repository.createOrUpdate(activityFact);
 
                     Debug.logInfo("ActivityFact entity record [" + activityFact.getActivityFactId() + "] created/updated.", MODULE);
                 }
             }
 
         } catch (RepositoryException ex) {
             Debug.logError(ex, MODULE);
             throw new ServiceException(ex);
         }  catch (GenericEntityException ex) {
             Debug.logError(ex, MODULE);
             throw new ServiceException(ex);
         } catch (EntityNotFoundException ex) {
             Debug.logError(ex, MODULE);
             throw new ServiceException(ex);
         }
     }
 
     /**
      * Transform all Activities which are TASK_COMPLETED or EVENT_COMPLETED
      * into ActivityFact
      *
      * @throws ServiceException
      */
     public void transformAllActivities() throws ServiceException {
         ActivitiesTransformToActivityFactsService activitiesTransform = null;
 
         try {
             ActivityRepositoryInterface activityRepository = getDomainsDirectory().getActivitiesDomain().getActivityRepository();
            PartyRepositoryInterface patryRepository = getDomainsDirectory().getPartyDomain().getPartyRepository();
            
            // Remove all ActivityFacts
            List<ActivityFact> activityFacts = patryRepository.findAll(ActivityFact.class);
            patryRepository.remove(activityFacts);
            
             // Find all Activities which are TASK_COMPLETED or EVENT_COMPLETED
             List<Activity> activityList = activityRepository.getCompletedActivities();
 
             // Each found activity transform into ActivityFact entities.
             for(Activity activity : activityList) {
                 String workEffortId = activity.getWorkEffortId();
 
                 activitiesTransform = new ActivitiesTransformToActivityFactsService();
                 activitiesTransform.setInWorkEffortId(workEffortId);
                 activitiesTransform.setUser(getUser());
                 activitiesTransform.runSync(infrastructure);
             }
 
         } catch (RepositoryException e) {
             Debug.logError(e, MODULE);
             throw new ServiceException(e);
         }
     }
 }
 
