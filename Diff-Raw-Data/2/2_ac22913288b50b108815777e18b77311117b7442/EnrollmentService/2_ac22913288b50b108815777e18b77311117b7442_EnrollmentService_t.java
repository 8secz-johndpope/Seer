 package org.motechproject.scheduletracking.api.service.impl;
 
 import org.joda.time.DateTime;
 import org.joda.time.Period;
 import org.motechproject.model.Time;
 import org.motechproject.scheduletracking.api.domain.*;
 import org.motechproject.scheduletracking.api.domain.exception.NoMoreMilestonesToFulfillException;
 import org.motechproject.scheduletracking.api.repository.AllEnrollments;
 import org.motechproject.scheduletracking.api.repository.AllTrackedSchedules;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.stereotype.Component;
 
 import static org.motechproject.scheduletracking.api.domain.EnrollmentStatus.COMPLETED;
 import static org.motechproject.scheduletracking.api.domain.EnrollmentStatus.UNENROLLED;
 import static org.motechproject.util.StringUtil.isNullOrEmpty;
 
 @Component
 public class EnrollmentService {
 
     private AllTrackedSchedules allTrackedSchedules;
     private AllEnrollments allEnrollments;
     private EnrollmentAlertService enrollmentAlertService;
     private EnrollmentDefaultmentService enrollmentDefaultmentService;
 
     @Autowired
     public EnrollmentService(AllTrackedSchedules allTrackedSchedules, AllEnrollments allEnrollments, EnrollmentAlertService enrollmentAlertService, EnrollmentDefaultmentService enrollmentDefaultmentService) {
         this.allTrackedSchedules = allTrackedSchedules;
         this.allEnrollments = allEnrollments;
         this.enrollmentAlertService = enrollmentAlertService;
         this.enrollmentDefaultmentService = enrollmentDefaultmentService;
     }
 
     public String enroll(String externalId, String scheduleName, String startingMilestoneName, DateTime referenceDateTime, DateTime enrollmentDateTime, Time preferredAlertTime) {
         Schedule schedule = allTrackedSchedules.getByName(scheduleName);
         EnrollmentStatus enrollmentStatus = EnrollmentStatus.ACTIVE;
        if (schedule.hasExpiredSince(referenceDateTime))
             enrollmentStatus = EnrollmentStatus.DEFAULTED;
 
         Enrollment enrollment = allEnrollments.addOrReplace(new Enrollment(externalId, scheduleName, startingMilestoneName, referenceDateTime, enrollmentDateTime, preferredAlertTime, enrollmentStatus));
         enrollmentAlertService.scheduleAlertsForCurrentMilestone(enrollment);
         enrollmentDefaultmentService.scheduleJobToCaptureDefaultment(enrollment);
 
         return enrollment.getId();
     }
 
     public void fulfillCurrentMilestone(Enrollment enrollment, DateTime fulfillmentDateTime) {
         Schedule schedule = allTrackedSchedules.getByName(enrollment.getScheduleName());
         if (isNullOrEmpty(enrollment.getCurrentMilestoneName()))
             throw new NoMoreMilestonesToFulfillException();
 
         unscheduleJobs(enrollment);
 
         enrollment.fulfillCurrentMilestone(fulfillmentDateTime);
         String nextMilestoneName = schedule.getNextMilestoneName(enrollment.getCurrentMilestoneName());
         enrollment.setCurrentMilestoneName(nextMilestoneName);
         if (nextMilestoneName == null)
             enrollment.setStatus(COMPLETED);
         else
             scheduleJobs(enrollment);
 
         allEnrollments.update(enrollment);
     }
 
     public void unenroll(Enrollment enrollment) {
         unscheduleJobs(enrollment);
         enrollment.setStatus(UNENROLLED);
         allEnrollments.update(enrollment);
     }
 
     // TODO: duplicated in alert and defaultment serviecs as well; tested here though
     public DateTime getCurrentMilestoneStartDate(Enrollment enrollment) {
         Schedule schedule = allTrackedSchedules.getByName(enrollment.getScheduleName());
         if (enrollment.getCurrentMilestoneName().equals(schedule.getFirstMilestone().getName()))
             return enrollment.getReferenceDateTime();
         return (enrollment.getFulfillments().isEmpty()) ? enrollment.getEnrollmentDateTime() : enrollment.getLastFulfilledDate();
     }
 
     public WindowName getCurrentWindowAsOf(Enrollment enrollment, DateTime asOf) {
         Schedule schedule = allTrackedSchedules.getByName(enrollment.getScheduleName());
         DateTime milestoneStart = getCurrentMilestoneStartDate(enrollment);
         Milestone milestone = schedule.getMilestone(enrollment.getCurrentMilestoneName());
         for (MilestoneWindow window : milestone.getMilestoneWindows()) {
             Period windowStart = milestone.getWindowStart(window.getName());
             Period windowEnd = milestone.getWindowEnd(window.getName());
             DateTime windowStartDateTime = milestoneStart.plus(windowStart);
             DateTime windowEndDateTime = milestoneStart.plus(windowEnd);
             if (inRange(asOf, windowStartDateTime, windowEndDateTime))
                 return window.getName();
         }
         return null;
     }
 
     public DateTime getStartOfWindowForCurrentMilestone(Enrollment enrollment, WindowName windowName) {
         Schedule schedule = allTrackedSchedules.getByName(enrollment.getScheduleName());
         DateTime currentMilestoneStartDate = getCurrentMilestoneStartDate(enrollment);
         Milestone currentMilestone = schedule.getMilestone(enrollment.getCurrentMilestoneName());
         return currentMilestoneStartDate.plus(currentMilestone.getWindowStart(windowName));
     }
 
     public DateTime getEndOfWindowForCurrentMilestone(Enrollment enrollment, WindowName windowName) {
         Schedule schedule = allTrackedSchedules.getByName(enrollment.getScheduleName());
         DateTime currentMilestoneStartDate = getCurrentMilestoneStartDate(enrollment);
         Milestone currentMilestone = schedule.getMilestone(enrollment.getCurrentMilestoneName());
         return currentMilestoneStartDate.plus(currentMilestone.getWindowEnd(windowName));
     }
 
     private boolean inRange(DateTime asOf, DateTime windowStartDateTime, DateTime windowEndDateTime) {
         return (asOf.equals(windowStartDateTime) || asOf.isAfter(windowStartDateTime)) && asOf.isBefore(windowEndDateTime);
     }
 
     private void scheduleJobs(Enrollment enrollment) {
         enrollmentAlertService.scheduleAlertsForCurrentMilestone(enrollment);
         enrollmentDefaultmentService.scheduleJobToCaptureDefaultment(enrollment);
     }
 
     private void unscheduleJobs(Enrollment enrollment) {
         enrollmentAlertService.unscheduleAllAlerts(enrollment);
         enrollmentDefaultmentService.unscheduleDefaultmentCaptureJob(enrollment);
     }
 }
