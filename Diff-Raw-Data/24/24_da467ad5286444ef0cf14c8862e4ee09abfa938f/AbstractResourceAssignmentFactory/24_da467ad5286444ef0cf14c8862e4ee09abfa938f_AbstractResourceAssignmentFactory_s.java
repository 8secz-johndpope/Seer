 /*
  * file:       AbstractResourceAssignmentFactory.java
  * author:     Jon Iles
  * copyright:  (c) Packwood Software 2010
  * date:       21/03/2010
  */
 
 /*
  * This library is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as published by the
  * Free Software Foundation; either version 2.1 of the License, or (at your
  * option) any later version.
  *
  * This library is distributed in the hope that it will be useful, but
  * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
  * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
  * License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this library; if not, write to the Free Software Foundation, Inc.,
  * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
  */
 
 package net.sf.mpxj.mpp;
 
 import java.util.Date;
 import java.util.List;
 import java.util.Set;
 
 import net.sf.mpxj.Duration;
 import net.sf.mpxj.ProjectCalendar;
 import net.sf.mpxj.ProjectFile;
 import net.sf.mpxj.Resource;
 import net.sf.mpxj.ResourceAssignment;
 import net.sf.mpxj.SplitTaskFactory;
 import net.sf.mpxj.Task;
 import net.sf.mpxj.TimeUnit;
 import net.sf.mpxj.TimephasedResourceAssignment;
 import net.sf.mpxj.TimephasedResourceAssignmentNormaliser;
 import net.sf.mpxj.WorkContour;
 import net.sf.mpxj.utility.NumberUtility;
 
 /**
  * Common implementation detail to extract resource assignment data from 
  * MPP9 and MPP12 files.
  */
 public abstract class AbstractResourceAssignmentFactory implements ResourceAssignmentFactory
 {
    /**
     * Retrieve the key for complete work data.
     * 
     * @return complete work key
     */
    protected abstract Integer getCompleteWorkKey();
 
    /**
     * Retrieve the key for planned work data.
     * 
     * @return planned work key
     */
    protected abstract Integer getPlannedWorkKey();
 
    /**
     * {@inheritDoc}
     */
    public void process(ProjectFile file, boolean useRawTimephasedData, VarMeta assnVarMeta, Var2Data assnVarData, FixedMeta assnFixedMeta, FixedData assnFixedData)
    {
       Set<Integer> set = assnVarMeta.getUniqueIdentifierSet();
       int count = assnFixedMeta.getItemCount();
       TimephasedResourceAssignmentFactory timephasedFactory = new TimephasedResourceAssignmentFactory();
       SplitTaskFactory splitFactory = new SplitTaskFactory();
       TimephasedResourceAssignmentNormaliser normaliser = new MPPTimephasedResourceAssignmentNormaliser();
 
       for (int loop = 0; loop < count; loop++)
       {
          byte[] meta = assnFixedMeta.getByteArrayValue(loop);
          if (meta[0] != 0)
          {
             continue;
          }
 
          int offset = MPPUtility.getInt(meta, 4);
          byte[] data = assnFixedData.getByteArrayValue(assnFixedData.getIndexFromOffset(offset));
          if (data == null)
          {
             continue;
          }
 
          int id = MPPUtility.getInt(data, 0);
          final Integer varDataId = Integer.valueOf(id);
          if (set.contains(varDataId) == false)
          {
             continue;
          }
 
          Integer taskID = Integer.valueOf(MPPUtility.getInt(data, 4));
          Task task = file.getTaskByUniqueID(taskID);
 
          if (task != null)
          {
             Integer resourceID = Integer.valueOf(MPPUtility.getInt(data, 8));
             Resource resource = file.getResourceByUniqueID(resourceID);
 
             ProjectCalendar calendar = null;
             if (resource != null)
             {
                calendar = resource.getResourceCalendar();
             }
 
             if (calendar == null || task.getIgnoreResourceCalendar())
             {
                calendar = task.getCalendar();
             }
 
             if (calendar == null)
             {
                calendar = file.getCalendar();
             }
 
             Date assignmentStart = MPPUtility.getTimestamp(data, 12);
             Date assignmentFinish = MPPUtility.getTimestamp(data, 16);
             double assignmentUnits = (MPPUtility.getDouble(data, 54)) / 100;
             byte[] completeWork = assnVarData.getByteArray(varDataId, getCompleteWorkKey());
             byte[] plannedWork = assnVarData.getByteArray(varDataId, getPlannedWorkKey());
             double remainingWork = (MPPUtility.getDouble(data, 86)) / 100;
             List<TimephasedResourceAssignment> timephasedComplete = timephasedFactory.getCompleteWork(calendar, assignmentStart, completeWork);
             List<TimephasedResourceAssignment> timephasedPlanned = timephasedFactory.getPlannedWork(calendar, assignmentStart, assignmentUnits, plannedWork, timephasedComplete);
             //System.out.println(timephasedComplete);
             //System.out.println(timephasedPlanned);
 
             if (task.getSplits() != null && task.getSplits().isEmpty())
             {
                splitFactory.processSplitData(task, timephasedComplete, timephasedPlanned);
             }
 
             if (resource != null)
             {
                ResourceAssignment assignment = task.addResourceAssignment(resource);
                assignment.setTimephasedNormaliser(normaliser);
 
                assignment.setActualCost(NumberUtility.getDouble(MPPUtility.getDouble(data, 110) / 100));
                assignment.setActualFinish(remainingWork == 0 ? assignmentFinish : null);
               assignment.setActualStart(completeWork == null ? null : assignmentStart);
                assignment.setActualWork(MPPUtility.getDuration((MPPUtility.getDouble(data, 70)) / 100, TimeUnit.HOURS));
                assignment.setCost(NumberUtility.getDouble(MPPUtility.getDouble(data, 102) / 100));
                assignment.setDelay(MPPUtility.getDuration(MPPUtility.getShort(data, 24), TimeUnit.HOURS));
                assignment.setFinish(assignmentFinish);
                assignment.setRemainingWork(MPPUtility.getDuration(remainingWork, TimeUnit.HOURS));
                assignment.setStart(assignmentStart);
                assignment.setUnits(Double.valueOf(assignmentUnits));
                assignment.setWork(MPPUtility.getDuration((MPPUtility.getDouble(data, 62)) / 100, TimeUnit.HOURS));
                assignment.setBaselineCost(NumberUtility.getDouble(MPPUtility.getDouble(data, 126) / 100));
                assignment.setBaselineFinish(MPPUtility.getTimestamp(data, 40));
                assignment.setBaselineStart(MPPUtility.getTimestamp(data, 36));
                assignment.setBaselineWork(Duration.getInstance(MPPUtility.getDouble(data, 94) / 60000, TimeUnit.HOURS));
 
                if (timephasedPlanned.isEmpty() && timephasedComplete.isEmpty())
                {
                   Duration workPerDay = TimephasedResourceAssignmentNormaliser.DEFAULT_NORMALIZER_WORK_PER_DAY;
                   int units = NumberUtility.getInt(assignment.getUnits());
                   if (units != 100)
                   {
                      workPerDay = Duration.getInstance((workPerDay.getDuration() * units) / 100.0, workPerDay.getUnits());
                   }
 
                   TimephasedResourceAssignment tra = new TimephasedResourceAssignment();
                   tra.setStart(assignmentStart);
                   tra.setWorkPerDay(workPerDay);
                   tra.setModified(false);
                   tra.setFinish(assignment.getFinish());
                   tra.setTotalWork(assignment.getWork().convertUnits(TimeUnit.MINUTES, file.getProjectHeader()));
                   timephasedPlanned.add(tra);
                }
 
                assignment.setTimephasedPlanned(timephasedPlanned, !useRawTimephasedData);
                assignment.setTimephasedComplete(timephasedComplete, !useRawTimephasedData);
 
                if (plannedWork != null)
                {
                   if (timephasedFactory.getWorkModified(timephasedPlanned))
                   {
                      assignment.setWorkContour(WorkContour.CONTOURED);
                   }
                   else
                   {
                      if (plannedWork.length >= 30)
                      {
                         assignment.setWorkContour(WorkContour.getInstance(MPPUtility.getShort(plannedWork, 28)));
                      }
                      else
                      {
                         assignment.setWorkContour(WorkContour.FLAT);
                      }
                   }
                   //System.out.println(assignment.getWorkContour());
                }
             }
          }
       }
    }
 }
