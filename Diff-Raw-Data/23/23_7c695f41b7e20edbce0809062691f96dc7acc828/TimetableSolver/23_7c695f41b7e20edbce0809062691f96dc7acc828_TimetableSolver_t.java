 /*
  * UniTime 3.0 (University Course Timetabling & Student Sectioning Application)
  * Copyright (C) 2007, UniTime.org, and individual contributors
  * as indicated by the @authors tag.
  * 
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  * 
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  * 
  * You should have received a copy of the GNU General Public License along
  * with this program; if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
 package org.unitime.timetable.solver;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.Serializable;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Date;
 import java.util.Enumeration;
 import java.util.HashSet;
 import java.util.Hashtable;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.Set;
 import java.util.StringTokenizer;
 import java.util.Vector;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.dom4j.Element;
 import org.hibernate.Session;
 import org.hibernate.Transaction;
 import org.unitime.timetable.model.Assignment;
 import org.unitime.timetable.model.Class_;
 import org.unitime.timetable.model.Department;
 import org.unitime.timetable.model.DepartmentalInstructor;
 import org.unitime.timetable.model.Location;
 import org.unitime.timetable.model.Solution;
 import org.unitime.timetable.model.SolverGroup;
 import org.unitime.timetable.model.TimePattern;
 import org.unitime.timetable.model.dao.DepartmentalInstructorDAO;
 import org.unitime.timetable.model.dao.LocationDAO;
 import org.unitime.timetable.model.dao.SolutionDAO;
 import org.unitime.timetable.model.dao.SolverGroupDAO;
 import org.unitime.timetable.model.dao.TimePatternDAO;
 import org.unitime.timetable.solver.interactive.ClassAssignmentDetails;
 import org.unitime.timetable.solver.interactive.Hint;
 import org.unitime.timetable.solver.interactive.Suggestion;
 import org.unitime.timetable.solver.interactive.Suggestions;
 import org.unitime.timetable.solver.interactive.SuggestionsModel;
 import org.unitime.timetable.solver.remote.BackupFileFilter;
 import org.unitime.timetable.solver.ui.AssignmentPreferenceInfo;
 import org.unitime.timetable.solver.ui.ConflictStatisticsInfo;
 import org.unitime.timetable.solver.ui.DeptBalancingReport;
 import org.unitime.timetable.solver.ui.DiscouragedInstructorBtbReport;
 import org.unitime.timetable.solver.ui.PerturbationReport;
 import org.unitime.timetable.solver.ui.PropertiesInfo;
 import org.unitime.timetable.solver.ui.RoomReport;
 import org.unitime.timetable.solver.ui.SameSubpartBalancingReport;
 import org.unitime.timetable.solver.ui.SolverUnassignedClassesModel;
 import org.unitime.timetable.solver.ui.StudentConflictsReport;
 import org.unitime.timetable.solver.ui.TimetableInfo;
 import org.unitime.timetable.solver.ui.TimetableInfoFileProxy;
 import org.unitime.timetable.solver.ui.TimetableInfoUtil;
 import org.unitime.timetable.solver.ui.ViolatedDistrPreferencesReport;
 import org.unitime.timetable.util.Constants;
 import org.unitime.timetable.webutil.timegrid.SolverGridModel;
 import org.unitime.timetable.webutil.timegrid.TimetableGridModel;
 
 import net.sf.cpsolver.coursett.TimetableXMLLoader;
 import net.sf.cpsolver.coursett.TimetableXMLSaver;
 import net.sf.cpsolver.coursett.constraint.ClassLimitConstraint;
 import net.sf.cpsolver.coursett.constraint.DepartmentSpreadConstraint;
 import net.sf.cpsolver.coursett.constraint.GroupConstraint;
 import net.sf.cpsolver.coursett.constraint.InstructorConstraint;
 import net.sf.cpsolver.coursett.constraint.RoomConstraint;
 import net.sf.cpsolver.coursett.constraint.SpreadConstraint;
 import net.sf.cpsolver.coursett.model.Lecture;
 import net.sf.cpsolver.coursett.model.Placement;
 import net.sf.cpsolver.coursett.model.RoomLocation;
 import net.sf.cpsolver.coursett.model.TimeLocation;
 import net.sf.cpsolver.coursett.model.TimetableModel;
 import net.sf.cpsolver.ifs.extension.ConflictStatistics;
 import net.sf.cpsolver.ifs.extension.Extension;
 import net.sf.cpsolver.ifs.model.Constraint;
 import net.sf.cpsolver.ifs.model.Value;
 import net.sf.cpsolver.ifs.solver.Solver;
 import net.sf.cpsolver.ifs.util.CSVFile;
 import net.sf.cpsolver.ifs.util.Callback;
 import net.sf.cpsolver.ifs.util.DataProperties;
 import net.sf.cpsolver.ifs.util.Progress;
 import net.sf.cpsolver.ifs.util.ProgressWriter;
 import net.sf.cpsolver.ifs.util.ToolBox;
 import net.sf.cpsolver.ifs.util.CSVFile.CSVField;
 
 /**
  * @author Tomas Muller
  */
 public abstract class TimetableSolver extends net.sf.cpsolver.coursett.TimetableSolver implements SolverProxy, TimetableInfoFileProxy {
 	private static Log sLog = LogFactory.getLog(TimetableSolver.class);
 	private boolean iWorking = false;
 	private Date iLoadedDate = null;
 	private int iDebugLevel = Progress.MSGLEVEL_INFO;
 	private Vector iAssignmentRecords = new Vector();
 	private Vector iBestAssignmentRecords = new Vector();
 	private ConflictStatisticsInfo iCbsInfo = null;
 	private CommitedClassAssignmentProxy iCommitedClassAssignmentProxy = null;
 	
 	private long iLastTimeStamp = System.currentTimeMillis();
 	private boolean iIsPassivated = false;
 	private Map iProgressBeforePassivation = null;
 	private PropertiesInfo iGlobalInfoBeforePassivation = null;
 	private Hashtable iCurrentSolutionInfoBeforePassivation = null;
 	private Hashtable iBestSolutionInfoBeforePassivation = null;
 	private File iPassivationFolder = null;
 	private String iPassivationPuid = null;
 	public static long sInactiveTimeToPassivate = 1800000;
 
 	public TimetableSolver(DataProperties properties) {
 		super(properties);
 		iCommitedClassAssignmentProxy = new CommitedClassAssignmentProxy();
 	}
 	
 	public Date getLoadedDate() {
 		if (iLoadedDate==null && !isPassivated()) {
 			Vector log = Progress.getInstance(currentSolution().getModel()).getLog();
 			if (log!=null && !log.isEmpty()) {
 				iLoadedDate = ((Progress.Message)log.firstElement()).getDate();
 			}
 		}
 		return iLoadedDate;
 	}
 	
 	public net.sf.cpsolver.ifs.solution.Solution currentSolution() {
 		activateIfNeeded();
 		return super.currentSolution();
 	}
 	
 	public void start() {
 		activateIfNeeded();
 		iCbsInfo = null;
 		super.start();
 	}
 	
 	public String getLog() {
 		return Progress.getInstance(currentSolution().getModel()).getHtmlLog(iDebugLevel, true);
 	}
 	public String getLog(int level, boolean includeDate) {
 		return Progress.getInstance(currentSolution().getModel()).getHtmlLog(level, includeDate);
 	}
 	public String getLog(int level, boolean includeDate, String fromStage) {
 		return Progress.getInstance(currentSolution().getModel()).getHtmlLog(level, includeDate, fromStage);
 	}
 	public String getNote() {
 		return getProperties().getProperty("General.Note");
 	}
 	public void setNote(String note) {
 		getProperties().setProperty("General.Note",note);
 	}
 	public void setDebugLevel(int level) { iDebugLevel = level; }
 	public int getDebugLevel() { return iDebugLevel; }
 	
 	public boolean isWorking() {
 		if (isRunning()) return true;
 		return iWorking;
 	}
 	
 	public void restoreBest() {
 		iAssignmentRecords = new Vector(iBestAssignmentRecords);
 		currentSolution().restoreBest();
 	}
 	
 	public void saveBest() {
 		iBestAssignmentRecords = new Vector(iAssignmentRecords);
 		currentSolution().saveBest();
 	}
 	
 	public Map getProgress() {
 		if (isPassivated()) return iProgressBeforePassivation;
     	try {
     		Hashtable ret = new Hashtable(); 
     		Progress p = Progress.getInstance(super.currentSolution().getModel());
     		ret.put("STATUS",p.getStatus());
     		ret.put("PHASE",p.getPhase());
     		ret.put("PROGRESS",new Long(p.getProgress()));
     		ret.put("MAX_PROGRESS",new Long(p.getProgressMax()));
     		ret.put("VERSION", Constants.VERSION+"."+Constants.BLD_NUMBER);
     		return ret;
     	} catch (Exception e) {
     		sLog.error(e.getMessage(),e);
     		return null;
     	}
     }
     
 	public void finalSectioning() {
 		(new FinalSectioning()).start();
 	}
     
     public class FinalSectioning extends Thread {
     	public void run() {
     		setName("FinalSectioning");
     		iWorking = true;
     		try {
     			((TimetableModel)currentSolution().getModel()).switchStudents();
    	    		//((TimetableModel)currentSolution().getModel()).checkStudentEnrollments(Progress.getInstance(currentSolution().getModel()));
     		} finally {
     			iWorking = false;
     			Progress.getInstance(currentSolution().getModel()).setStatus("Awaiting commands ...");
     		}
     		afterFinalSectioning();
     	}
     }
     
     public void dispose() {
     	disposeNoInherit();
     }
     
     private void disposeNoInherit() {
     	super.dispose();
    		iAssignmentRecords.clear(); iBestAssignmentRecords.clear(); iCbsInfo = null;
     	if (currentSolution()!=null && currentSolution().getModel()!=null)
     		Progress.removeInstance(currentSolution().getModel());
     	setInitalSolution((net.sf.cpsolver.ifs.solution.Solution)null);
     }
     
     protected void onFinish() {
 		super.onFinish();
 		try {
 			iWorking = true;
 			if (currentSolution().getBestInfo()!=null)
 				currentSolution().restoreBest();
 			if (getProperties().getPropertyBoolean("General.SwitchStudents",false)) {
 				((TimetableModel)currentSolution().getModel()).switchStudents();
 				currentSolution().saveBest();
 			}
 			if (currentSolution().getBestInfo()!=null && getProperties().getPropertyBoolean("General.Save",false)) {
 				TimetableDatabaseSaver saver = new TimetableDatabaseSaver(this);
 				synchronized (currentSolution()) {
 					saver.save();
 				}
 			}
 			int repeat = getProperties().getPropertyInt("Test.Repeat",0);
 			if (repeat>0) {
 				getProperties().setProperty("Test.Repeat", String.valueOf(repeat-1));
 				getProperties().remove("General.SolutionId");
 				load(getProperties());
 				return;
 			}
 			if (getProperties().getPropertyBoolean("General.Unload",false)) {
 				dispose();
 			} else {
 				Progress.getInstance(currentSolution().getModel()).setStatus("Awaiting commands ...");
 			}
 		} finally {
 			iWorking = false;
 		}
 	}
     
     protected void onStop() {
     	super.onStop();
 		if (currentSolution().getBestInfo()!=null)
 			currentSolution().restoreBest();
     }
     
     public void save(boolean createNewSolution, boolean commitSolution) {
 		iWorking = true;
 		getProperties().setProperty("General.CreateNewSolution",(createNewSolution?"true":"false"));
 		if (createNewSolution)
 			getProperties().remove("General.SolutionId");
 		getProperties().setProperty("General.CommitSolution",(commitSolution?"true":"false"));
 		TimetableDatabaseSaver saver = new TimetableDatabaseSaver(this);
 		saver.setCallback(getSavingDoneCallback());
 		Thread thread = new Thread(saver);
 		thread.setPriority(THREAD_PRIORITY);
 		thread.start();
     }
     
     public void load(DataProperties properties) {
     	iAssignmentRecords.clear(); iBestAssignmentRecords.clear(); iCbsInfo = null;
     	sLog.debug("History cleared");
     	setProperties(properties);
     	TimetableModel model = new TimetableModel(getProperties());
     	Progress.getInstance(model).addProgressListener(new ProgressWriter(System.out));
     	
 		iWorking = true;
 		setInitalSolution(model);
 		initSolver();
 		
 		TimetableDatabaseLoader loader = new TimetableDatabaseLoader(model);
 		loader.setCallback(getLoadingDoneCallback());
 		Thread thread = new Thread(loader);
 		thread.setPriority(THREAD_PRIORITY);
 		thread.start();
     }
     
     public void reload(DataProperties properties) {
     	if (currentSolution()==null || currentSolution().getModel()==null) {
     		load(properties);
     		return;
     	}
     	
     	Callback callBack = getReloadingDoneCallback();
     	setProperties(properties);
     	TimetableModel model = new TimetableModel(getProperties());
     	
 		iWorking = true;
     	Progress.changeInstance(currentSolution().getModel(),model);
 		setInitalSolution(model);
 		initSolver();
 		
 		TimetableDatabaseLoader loader = new TimetableDatabaseLoader(model);
 		loader.setCallback(callBack);
 		(new Thread(loader)).start();
     }
     
     public Callback getLoadingDoneCallback() {
     	return new LoadingDoneCallback();
     }
     
     public Callback getReloadingDoneCallback() {
     	return new ReloadingDoneCallback();
     }
 
     public Callback getSavingDoneCallback() {
     	return new SavingDoneCallback();
     }
     
     protected void afterSave() {
     }
     
     protected void afterLoad() {
     }
 
     protected void afterFinalSectioning() {
     }
 
     public class ReloadingDoneCallback implements Callback {
     	Hashtable iCurrentAssignmentTable = new Hashtable();
     	Hashtable iBestAssignmentTable = new Hashtable();
     	Hashtable iInitialAssignmentTable = new Hashtable();
     	String iSolutionId = null;
     	Progress iProgress = null;
     	public ReloadingDoneCallback() {
     		iSolutionId = getProperties().getProperty("General.SolutionId");
     		for (Enumeration e=currentSolution().getModel().variables().elements();e.hasMoreElements();) {
     			Lecture lecture = (Lecture)e.nextElement();
     			if (lecture.getAssignment()!=null)
     				iCurrentAssignmentTable.put(lecture.getClassId(),lecture.getAssignment());
     			if (lecture.getBestAssignment()!=null)
     				iBestAssignmentTable.put(lecture.getClassId(),lecture.getBestAssignment());
     			if (lecture.getInitialAssignment()!=null)
     				iInitialAssignmentTable.put(lecture.getClassId(),lecture.getInitialAssignment());
     		}
     	}
     	private Lecture getLecture(Long classId) {
     		for (Enumeration e=currentSolution().getModel().variables().elements();e.hasMoreElements();) {
     			Lecture l = (Lecture)e.nextElement();
     			if (l.getClassId().equals(classId)) 
     				return l;
     		}
     		return null;
     	}
     	private Placement getPlacement(Lecture lecture, Placement placement) {
     		TimeLocation time = null;
     		for (Enumeration f=lecture.timeLocations().elements();f.hasMoreElements();) {
     			TimeLocation t = (TimeLocation)f.nextElement();
     			if (placement.getTimeLocation().equals(t)) {
     				time = t; break;
     			}
     		}
     		if (time==null) {
     			iProgress.warn("WARNING: Time "+placement.getTimeLocation()+" is no longer valid for class "+lecture.getName());
     			return null;
     		}
     		Vector rooms = new Vector();
     		for (Enumeration f=lecture.roomLocations().elements();f.hasMoreElements();) {
     			RoomLocation r = (RoomLocation)f.nextElement();
     			if (placement.isMultiRoom() && placement.getRoomLocations().contains(r)) {
     				rooms.add(r);
     			}
     			if (!placement.isMultiRoom() && placement.getRoomLocation().equals(r)) {
     				rooms.add(r); break;
     			}
     		}
     		if (rooms.size()!=lecture.getNrRooms()) {
     			iProgress.warn("WARNING: Room(s) "+(placement.isMultiRoom()?placement.getRoomLocations().toString():placement.getRoomLocation().getName())+" are no longer valid for class "+lecture.getName());
     			return null;
     		}
     		return new Placement(lecture,time,rooms);
     	}
     	private void assign(Placement placement) {
             Hashtable conflictConstraints = currentSolution().getModel().conflictConstraints(placement);
             if (conflictConstraints.isEmpty()) {
             	placement.variable().assign(0,placement);
             } else {
                 iProgress.warn("Unable to assign "+placement.variable().getName()+" := "+placement.getName());
                 iProgress.warn("&nbsp;&nbsp;Reason:");
                 for (Enumeration ex=conflictConstraints.keys();ex.hasMoreElements();) {
                     Constraint c = (Constraint)ex.nextElement();
                     Collection vals = (Collection)conflictConstraints.get(c);
                     for (Iterator j=vals.iterator();j.hasNext();) {
                         Value v = (Value) j.next();
                         iProgress.warn("&nbsp;&nbsp;&nbsp;&nbsp;"+v.variable().getName()+" = "+v.getName());
                     }
                     iProgress.debug("&nbsp;&nbsp;&nbsp;&nbsp;in constraint "+c);
                 }
             }
     	}
     	private void unassignAll() {
     		for (Enumeration e=currentSolution().getModel().variables().elements();e.hasMoreElements();) {
     			Lecture lecture = (Lecture)e.nextElement();
     			if (lecture.getAssignment()!=null)
     				lecture.unassign(0);
     		}
     	}
     	public void execute() {
     		iProgress = Progress.getInstance(currentSolution().getModel());
     		
             if (!iBestAssignmentTable.isEmpty()) {
             	iProgress.setPhase("Creating best assignment ...",iBestAssignmentTable.size());
             	unassignAll();
             	for (Iterator i=iBestAssignmentTable.entrySet().iterator();i.hasNext();) {
             		Map.Entry entry = (Map.Entry)i.next();
             		iProgress.incProgress();
             		Lecture lecture = getLecture((Long)entry.getKey()); 
             		if (lecture==null) continue;
             		Placement placement = getPlacement(lecture,(Placement)entry.getValue());
             		if (placement!=null && placement.isValid()) assign(placement);
             	}
             	
             	currentSolution().saveBest();
             }
             if (!iInitialAssignmentTable.isEmpty()) {
             	iProgress.setPhase("Creating initial assignment ...",iInitialAssignmentTable.size());
             	for (Iterator i=iInitialAssignmentTable.entrySet().iterator();i.hasNext();) {
             		Map.Entry entry = (Map.Entry)i.next();
             		iProgress.incProgress();
             		Lecture lecture = getLecture((Long)entry.getKey()); 
             		if (lecture==null) continue;
             		Placement placement = getPlacement(lecture,(Placement)entry.getValue());
             		if (placement!=null && placement.isValid())  lecture.setInitialAssignment(placement);
             	}
             }
             if (!iCurrentAssignmentTable.isEmpty()) {
             	iProgress.setPhase("Creating current assignment ...",iCurrentAssignmentTable.size());
             	unassignAll();
             	for (Iterator i=iCurrentAssignmentTable.entrySet().iterator();i.hasNext();) {
             		Map.Entry entry = (Map.Entry)i.next();
             		iProgress.incProgress();
             		Lecture lecture = getLecture((Long)entry.getKey()); 
             		if (lecture==null) continue;
             		Placement placement = getPlacement(lecture,(Placement)entry.getValue());
             		if (placement!=null && placement.isValid()) assign(placement);
             	}
             }
             iCurrentAssignmentTable.clear();
             iBestAssignmentRecords.clear();
             iInitialAssignmentTable.clear();
             iProgress = null;
             
             if (iSolutionId!=null)
             	getProperties().setProperty("General.SolutionId",iSolutionId);
 
     		iLoadedDate = new Date();
     		iWorking = false;
     		afterLoad();
     		Progress.getInstance(currentSolution().getModel()).setStatus("Awaiting commands ...");
     	}
     }
     public class LoadingDoneCallback implements Callback {
     	public void execute() {
     		iLoadedDate = new Date();
     		iWorking = false;
     		afterLoad();
     		Progress.getInstance(currentSolution().getModel()).setStatus("Awaiting commands ...");
     		if (getProperties().getPropertyBoolean("General.StartSolver",false))
     			start();
     	}
     }
     public class SavingDoneCallback implements Callback {
     	public void execute() {
     		iWorking = false;
     		afterSave();
     		Progress.getInstance(currentSolution().getModel()).setStatus("Awaiting commands ...");
     	}
     }
 
     public PropertiesInfo getGlobalInfo() {
     	if (isPassivated()) return iGlobalInfoBeforePassivation;
     	Hashtable info = null;
     	synchronized (super.currentSolution()) {
     		info = super.currentSolution().getBestInfo();
 			if (info==null)
 				info = super.currentSolution().getInfo();
 		}
 		PropertiesInfo globalInfo = new PropertiesInfo(); 
 		for (Iterator i1=info.entrySet().iterator();i1.hasNext();) {
 			Map.Entry entry = (Map.Entry)i1.next();
 			String key = (String)entry.getKey();
 			String value = entry.getValue().toString();
 			globalInfo.setProperty(key, value);
 		}
 		return globalInfo;
     }
     
     public ConflictStatisticsInfo getCbsInfo() {
     	ConflictStatistics cbs = null;
     	for (Enumeration e=getExtensions().elements();e.hasMoreElements();) {
     		Extension ext = (Extension)e.nextElement();
     		if (ext instanceof ConflictStatistics) {
     			cbs = (ConflictStatistics)ext;
     			break;
     		}
     	}
     	if (cbs==null || cbs.getNoGoods().isEmpty()) {
     		if (iCbsInfo!=null) return iCbsInfo;
     		return null;
     	}
     	ConflictStatisticsInfo info = new ConflictStatisticsInfo();
     	synchronized (currentSolution()) {
     		info.load(cbs);
     	}
     	return info; 
     }
     
     public ConflictStatisticsInfo getCbsInfo(Long classId) {
     	ConflictStatistics cbs = null;
     	for (Enumeration e=getExtensions().elements();e.hasMoreElements();) {
     		Extension ext = (Extension)e.nextElement();
     		if (ext instanceof ConflictStatistics) {
     			cbs = (ConflictStatistics)ext;
     			break;
     		}
     	}
     	if (cbs==null || cbs.getNoGoods().isEmpty()) {
     		if (iCbsInfo!=null) return iCbsInfo;
     		return null;
     	}
     	ConflictStatisticsInfo info = new ConflictStatisticsInfo();
     	synchronized (currentSolution()) {
     		info.load(cbs, classId);
     	}
     	return info; 
     }    
     
     public SolverUnassignedClassesModel getUnassignedClassesModel() {
     	synchronized (currentSolution()) {
     		return new SolverUnassignedClassesModel(this);
     	}
     }
     
     private boolean match(String findString, String name) {
 		if (findString==null || findString.trim().length()==0) return true;
 		StringTokenizer stk = new StringTokenizer(findString.toUpperCase()," ,");
 		String n = name.toUpperCase();
 		while (stk.hasMoreTokens()) {
 			String token = stk.nextToken().trim();
 			if (token.length()==0) continue;
 			if (n.indexOf(token)<0) return false;
 		}
 		return true;
 	}    
     
     public Vector getTimetableGridTables(String findString, int resourceType, int startDay, int bgMode) {
     	Vector models = new Vector();
     	synchronized (currentSolution()) {
     		TimetableModel model = (TimetableModel)currentSolution().getModel();
     		if (resourceType==TimetableGridModel.sResourceTypeRoom) {
     			for (Enumeration e=model.getRoomConstraints().elements();e.hasMoreElements();) {
     				RoomConstraint rc = (RoomConstraint)e.nextElement();
     				if (!match(findString, rc.getName())) continue;
     				models.add(new SolverGridModel(this,rc,startDay,bgMode));
     			}
     		} else if (resourceType==TimetableGridModel.sResourceTypeInstructor) {
     			for (Enumeration e=model.getInstructorConstraints().elements();e.hasMoreElements();) {
     				InstructorConstraint ic = (InstructorConstraint)e.nextElement();
     				if (!match(findString, ic.getName())) continue;
     				models.add(new SolverGridModel(this,ic,startDay,bgMode));
     			}
     		} else if (resourceType==TimetableGridModel.sResourceTypeDepartment) {
     			for (Enumeration e=model.getDepartmentSpreadConstraints().elements();e.hasMoreElements();) {
     				DepartmentSpreadConstraint dc = (DepartmentSpreadConstraint)e.nextElement();
     				if (!match(findString, dc.getName())) continue;
     				models.add(new SolverGridModel(this,dc,startDay,bgMode));
     			}
     		}
     	}
 		return models;
     }
     
     public ClassAssignmentDetails getClassAssignmentDetails(Long classId, boolean includeConstraints) {
     	synchronized (currentSolution()) {
     		TimetableModel model = (TimetableModel)currentSolution().getModel();
     		for (Enumeration e=model.variables().elements();e.hasMoreElements();) {
     			Lecture lecture = (Lecture)e.nextElement();
     			if (lecture.getClassId().equals(classId))
     				return new ClassAssignmentDetails(this,lecture,includeConstraints);
     		}
    			return null;
     	}
     }
     
     public Suggestions getSuggestions(SuggestionsModel model) {
     	synchronized (currentSolution()) {
     		return new Suggestions(this,model);
     	}
     }
     
     public AssignmentPreferenceInfo getInfo(Hint hint) {
     	synchronized (currentSolution()) {
     		return hint.getInfo(this);
     	}
     }
     
     public String getNotValidReason(Hint hint) {
     	synchronized (currentSolution()) {
     		return hint.getNotValidReason(this);
     	}
     }
 
     public void assign(Collection hints) {
 		synchronized (currentSolution()) {
 			Hashtable initialAssignments = new Hashtable();
 			for (Enumeration e=currentSolution().getModel().assignedVariables().elements();e.hasMoreElements();) {
 				Lecture lec = (Lecture)e.nextElement();
 				initialAssignments.put(lec,lec.getAssignment());
 			}
 			AssignmentRecord record = new AssignmentRecord(this);
 			for (Iterator i=hints.iterator();i.hasNext();) {
 				Hint hint = (Hint)i.next();
 				Placement p = hint.getPlacement((TimetableModel)currentSolution().getModel());
 				if (p!=null) {
 					Placement ini = (Placement)initialAssignments.get(p.variable());
 					record.add(ini,p);
 					Progress.getInstance(currentSolution().getModel()).info(p.variable().getName()+": "+(ini==null?"not assigned":ini.getName())+" &rarr; "+p.getName());
                    if (ini!=null) p.variable().unassign(0);
 				}
 			}
            for (Iterator i=hints.iterator();i.hasNext();) {
                Hint hint = (Hint)i.next();
                Placement p = hint.getPlacement((TimetableModel)currentSolution().getModel());
                if (p!=null) p.variable().assign(0,p);
            }
 			for (Enumeration e=currentSolution().getModel().unassignedVariables().elements();e.hasMoreElements();) {
 				Lecture lec = (Lecture)e.nextElement();
 				Placement p = (Placement)initialAssignments.get(lec);
 				if (p!=null) { 
 					record.add(p,null);
 					Progress.getInstance(currentSolution().getModel()).info(p.variable().getName()+": "+p.getName()+" &rarr; not assigned");
 				}
 			}
 			record.done();
 			iAssignmentRecords.addElement(record);
 		}
     }
     
     public Hashtable conflictInfo(Collection hints) {
     	Hashtable conflictTable = new Hashtable();
 		synchronized (currentSolution()) {
 			HashSet done = new HashSet();
 			for (Iterator i=hints.iterator();i.hasNext();) {
 				Hint hint = (Hint)i.next();
 				Placement p = hint.getPlacement((TimetableModel)currentSolution().getModel());
 				if (p==null) continue;
 		        for (Enumeration c=p.variable().hardConstraints().elements(); c.hasMoreElements();) {
 		            Constraint constraint = (Constraint)c.nextElement();
 		            HashSet conflicts = new HashSet();
 		            constraint.computeConflicts(p, conflicts);
 		            if (conflicts!=null && !conflicts.isEmpty()) {
 		            	for (Iterator j=conflicts.iterator();j.hasNext();) {
 		            		Placement conflict = (Placement)j.next();
 		            		Hint confHint = new Hint(this, conflict);
 		            		if (done.contains(confHint)) continue;
 		            		if (!conflictTable.containsKey(confHint)) {
 		            			String name = constraint.getName();
 		        				if (constraint instanceof RoomConstraint) {
 		        					name = "Room "+constraint.getName();
 		        				} else if (constraint instanceof InstructorConstraint) {
 		        					name = "Instructor "+constraint.getName();
 		        				} else if (constraint instanceof GroupConstraint) {
 		        					name = "Distribution "+constraint.getName();
 		        				} else if (constraint instanceof DepartmentSpreadConstraint) {
 		        					name = "Balancing of department "+constraint.getName();
 		        				} else if (constraint instanceof SpreadConstraint) {
 		        					name = "Same subpart spread "+constraint.getName();
 		        				} else if (constraint instanceof ClassLimitConstraint) {
 		        					name = "Class limit "+constraint.getName();
 		        				}
 		        				conflictTable.put(confHint, name);
 		            		}
 		            	}
 		            }
 		        }
 		        done.add(hint);
 			}
 		}
 		return conflictTable;
     }
     
     public Hashtable currentSolutionInfo() {
     	if (isPassivated()) return iCurrentSolutionInfoBeforePassivation;
     	synchronized (super.currentSolution()) {
     		return super.currentSolution().getInfo();
     	}
     }
 
     public Hashtable bestSolutionInfo() {
     	if (isPassivated()) return iBestSolutionInfoBeforePassivation;
     	synchronized (super.currentSolution()) {
     		return super.currentSolution().getBestInfo();
     	}
     }
     
     public void setProperties(DataProperties properties) {
     	activateIfNeeded();
     	this.getProperties().putAll(properties);
     }
     
     public boolean backup(File folder, String puid) {
     	folder.mkdirs();
     	if (currentSolution()==null) return false;
     	synchronized (currentSolution()) {
     		TimetableXMLSaver saver = new TimetableXMLSaver(this);
     		File outXmlFile = new File(folder,puid+BackupFileFilter.sXmlExtension);
     		File outPropertiesFile = new File(folder,puid+BackupFileFilter.sPropertiesExtension);
     		try {
     			saver.save(outXmlFile);
     			FileOutputStream fos = null;
     			try {
     				fos = new FileOutputStream(outPropertiesFile);
     				getProperties().store(fos,"Backup file");
     				fos.flush(); fos.close(); fos=null;
     			} finally {
             		try {
             			if (fos!=null) fos.close();
             		} catch (IOException e) {}
     			}
     			return true;
     		} catch (Exception e) {
     			sLog.error(e.getMessage(),e);
     			if (outXmlFile.exists()) outXmlFile.delete();
     			if (outPropertiesFile.exists()) outPropertiesFile.delete();
     		}
     	}
 		return false;
     }
     
     public boolean restore(File folder, String puid) {
     	return restore(folder, puid, false);
     }
     
     public boolean restore(File folder, String puid, boolean removeFiles) {
    		iAssignmentRecords.clear(); iBestAssignmentRecords.clear(); iCbsInfo = null;
     	sLog.debug("restore(folder="+folder+",puid="+puid+")");
 		File inXmlFile = new File(folder,puid+BackupFileFilter.sXmlExtension);
 		File inPropertiesFile = new File(folder,puid+BackupFileFilter.sPropertiesExtension);
 		
 		TimetableModel model = null;
 		try {
 			if (isRunning()) stopSolver();
 			this.disposeNoInherit();
 			FileInputStream fis = null;
 			try {
 				fis = new FileInputStream(inPropertiesFile);
 				getProperties().load(fis);
 			} finally {
 				if (fis!=null) fis.close();
 			}
 			
 			model = new TimetableModel(getProperties());
 	    	Progress.getInstance(model).addProgressListener(new ProgressWriter(System.out));
 	    	
 	    	setInitalSolution(model);
 	    	initSolver();
 			
 			TimetableXMLLoader loader = new TimetableXMLLoader(model);
 			loader.setSolver(this);
 			loader.setInputFile(inXmlFile);
 			loader.load(currentSolution());
 			loader.setSolver(null);
 			
 			Progress.getInstance(model).setStatus("Awaiting commands ...");
 			
 			if (removeFiles) {
 				inXmlFile.delete();
 				inPropertiesFile.delete();
 			}
 			
 			return true;
 		} catch (Exception e) {
 			sLog.error(e.getMessage(),e);
 			if (model!=null) Progress.removeInstance(model);
 		}
 		
 		return false;
     }
     
     public Long[] getOwnerId() {
     	return getProperties().getPropertyLongArry("General.SolverGroupId", null);
     }
     
     private HashSet iDepartmentIds = null;
     public Set getDepartmentIds() {
     	if (iDepartmentIds!=null) return iDepartmentIds;
     	iDepartmentIds = new HashSet();
     	Long ownerId[] = getOwnerId();
     	for (int i=0;i<ownerId.length;i++) {
     		SolverGroup sg = (new SolverGroupDAO()).get(ownerId[i]);
     		for (Iterator j=sg.getDepartments().iterator();j.hasNext();) {
     			iDepartmentIds.add(((Department)j.next()).getUniqueId());
     		}
     	}
     	return iDepartmentIds;
     }
     
 	public Assignment getAssignment(Class_ clazz) throws Exception {
 		Department dept = clazz.getManagingDept();
 		if (dept!=null && getDepartmentIds().contains(dept.getUniqueId()))
 			return getAssignment(clazz.getUniqueId());
         return iCommitedClassAssignmentProxy.getAssignment(clazz);
     }
     
     public Assignment getAssignment(Long classId) {
     	synchronized (currentSolution()) {
     		Lecture lecture = null;
     		for (Enumeration e=currentSolution().getModel().variables().elements();e.hasMoreElements();) {
     			Lecture l = (Lecture)e.nextElement();
     			if (l.getClassId().equals(classId)) {
     				lecture = l; break;
     			}
     		}
     		if (lecture==null) return null;
     		Placement placement = (Placement)lecture.getAssignment();
     		if (placement==null) return null;
         	Assignment assignment = new Assignment();
         	assignment.setClassName(lecture.getName());
     		assignment.setDays(new Integer(placement.getTimeLocation().getDayCode()));
     		assignment.setStartSlot(new Integer(placement.getTimeLocation().getStartSlot()));
     		if (placement.getTimeLocation().getDatePatternId()!=null) {
     			assignment.setCachedDatePatternId(placement.getTimeLocation().getDatePatternId());
     		}
     		assignment.setSlotsPerMtg(placement.getTimeLocation().getLength());
     		assignment.setBreakTime(placement.getTimeLocation().getBreakTime());
     		HashSet rooms = new HashSet();
     		if (placement.isMultiRoom()) {
     			for (Enumeration e=placement.getRoomLocations().elements();e.hasMoreElements();) {
     				RoomLocation r = (RoomLocation)e.nextElement();
     				Location room = (new LocationDAO()).get(r.getId());
     				if (room!=null) rooms.add(room);
     			}
     		} else {
     			Location room = (new LocationDAO()).get(placement.getRoomLocation().getId());
     			if (room!=null) rooms.add(room);
     		}
     		assignment.setRooms(rooms);
     		TimePattern pattern = (new TimePatternDAO()).get(placement.getTimeLocation().getTimePatternId());
     		assignment.setTimePattern(pattern);
     		HashSet instructors = new HashSet();
     		for (Enumeration e=lecture.getInstructorConstraints().elements();e.hasMoreElements();) {
     			InstructorConstraint ic = (InstructorConstraint)e.nextElement();
     			DepartmentalInstructor instructor = null;
     			if (ic.getResourceId()!=null) {
 					instructor = (new DepartmentalInstructorDAO()).get(ic.getResourceId());
 				}
     			if (instructor!=null) instructors.add(instructor);
     			
     		}
     		assignment.setInstructors(instructors);
     		return assignment;
     	}
     }
 
     public AssignmentPreferenceInfo getAssignmentInfo(Class_ clazz) throws Exception {
 		Department dept = clazz.getManagingDept();
 		if (dept!=null && getDepartmentIds().contains(dept.getUniqueId()))
 				return getAssignmentInfo(clazz.getUniqueId());
 		return iCommitedClassAssignmentProxy.getAssignmentInfo(clazz);
     }
     
     public AssignmentPreferenceInfo getAssignmentInfo(Long classId) {
     	synchronized (currentSolution()) {
     		Lecture lecture = null;
     		for (Enumeration e=currentSolution().getModel().variables().elements();e.hasMoreElements();) {
     			Lecture l = (Lecture)e.nextElement();
     			if (l.getClassId().equals(classId)) {
     				lecture = l; break;
     			}
     		}
     		if (lecture==null) return null;
     		Placement placement = (Placement)lecture.getAssignment();
     		if (placement==null) return null;
     		return new AssignmentPreferenceInfo(this,placement);
     	}
     }
 
 	public Hashtable getAssignmentTable(Collection classesOrClassIds) throws Exception {
 		Hashtable assignments = new Hashtable();
 		for (Iterator i=classesOrClassIds.iterator();i.hasNext();) {
 			Object classOrClassId = i.next();
 			Assignment assignment = (classOrClassId instanceof Class_ ? getAssignment((Class_)classOrClassId) : getAssignment((Long)classOrClassId));
 			if (assignment!=null)
 				assignments.put(classOrClassId instanceof Class_ ? ((Class_)classOrClassId).getUniqueId() : (Long)classOrClassId, assignment);
 		}
 		return assignments;
 	}
     public Hashtable getAssignmentTable2(Collection classesOrClassIds) throws Exception {
         return getAssignmentTable(classesOrClassIds);
     }
 	
 	public Hashtable getAssignmentInfoTable(Collection classesOrClassIds) throws Exception {
 		Hashtable infos = new Hashtable();
 		for (Iterator i=classesOrClassIds.iterator();i.hasNext();) {
 			Object classOrClassId = i.next();
 			AssignmentPreferenceInfo info = (classOrClassId instanceof Class_ ? getAssignmentInfo((Class_)classOrClassId) : getAssignmentInfo((Long)classOrClassId));
 			if (info!=null)
 				infos.put(classOrClassId instanceof Class_ ? ((Class_)classOrClassId).getUniqueId() : (Long)classOrClassId, info);
 		}
 		return infos;
 	}
     public Hashtable getAssignmentInfoTable2(Collection classesOrClassIds) throws Exception {
         return getAssignmentInfoTable(classesOrClassIds);
     }
 
 	public void saveToFile(String name, TimetableInfo info) throws Exception {
 		TimetableInfoUtil.getInstance().saveToFile(name, info);
 	}
 	public TimetableInfo loadFromFile(String name) throws Exception {
 		return TimetableInfoUtil.getInstance().loadFromFile(name);
 	}
 	public void deleteFile(String name) throws Exception {
 		TimetableInfoUtil.getInstance().deleteFile(name);
 	}
 	
 	public Vector getAssignmentRecords() {
 		return iAssignmentRecords;
 	}
 	
 	public Vector getChangesToInitial() {
 		Vector ret = new Vector();
 		synchronized (currentSolution()) {
 			for (Enumeration e=currentSolution().getModel().variables().elements();e.hasMoreElements();) {
 				Lecture lecture = (Lecture)e.nextElement();
 				if (!ToolBox.equals(lecture.getInitialAssignment(),lecture.getAssignment())) {
 					RecordedAssignment a = new RecordedAssignment(this,(Placement)lecture.getInitialAssignment(),(Placement)lecture.getAssignment()); 
 					if (lecture.getInitialAssignment()!=null) {
 						a.getBefore().setDetails(new ClassAssignmentDetails(this,lecture,(Placement)lecture.getInitialAssignment(),false));
 					}
 					if (lecture.getAssignment()!=null) {
 						a.getAfter().setDetails(new ClassAssignmentDetails(this,lecture,false));
 					}
 					ret.addElement(a);
 				}
 			}
 		}
 		return ret;
 	}
 	
 	public Vector getAssignedClasses() {
 		Vector ret = new Vector();
 		synchronized (currentSolution()) {
 			for (Enumeration e=currentSolution().getModel().assignedVariables().elements();e.hasMoreElements();) {
 				Lecture lecture = (Lecture)e.nextElement();
 				ret.addElement(new ClassAssignmentDetails(this,lecture,false));
 			}
 		}
 		return ret;
 	}
 	
 	public Vector getChangesToBest() {
 		Vector ret = new Vector();
 		synchronized (currentSolution()) {
 			for (Enumeration e=currentSolution().getModel().variables().elements();e.hasMoreElements();) {
 				Lecture lecture = (Lecture)e.nextElement();
 				if (!ToolBox.equals(lecture.getBestAssignment(),lecture.getAssignment())) {
 					RecordedAssignment a = new RecordedAssignment(this,(Placement)lecture.getBestAssignment(),(Placement)lecture.getAssignment());
 					if (lecture.getBestAssignment()!=null) {
 						a.getBefore().setDetails(new ClassAssignmentDetails(this,lecture,(Placement)lecture.getBestAssignment(),false));
 					}
 					if (lecture.getAssignment()!=null) {
 						a.getAfter().setDetails(new ClassAssignmentDetails(this,lecture,false));
 					}
 					ret.addElement(a);
 				}
 			}
 		}
 		return ret;
 	}
 	
 	public Vector getChangesToSolution(Long solutionId) throws Exception {
 		return getChangesToSolution(solutionId, false);
 	}
 	
 	public Vector getChangesToSolution(Long solutionId, boolean closeSession) throws Exception {
 		synchronized (currentSolution()) {
 			Session hibSession = (new SolutionDAO()).getSession();
 			Transaction tx = null;
 			Vector ret = new Vector();
 			try {
 				tx = hibSession.beginTransaction();
 				Solution solution = (new SolutionDAO()).get(solutionId, hibSession);
 				if (solution==null) return null;
 				Long ownerId = solution.getOwner().getUniqueId();
 				Long[] ownerIds = getOwnerId();
 				boolean sameOwner = false;
 				for (int i=0;i<ownerIds.length;i++)
 					if (ownerId.equals(ownerIds[i])) {
 						sameOwner = true; break;
 					}
 				if (!sameOwner) return null;
 				HashSet ids = new HashSet();
 				for (Iterator i=solution.getAssignments().iterator();i.hasNext();) {
 					Assignment assignment = (Assignment)i.next();
 					Lecture lecture = null;
 					for (Enumeration e=currentSolution().getModel().variables().elements();e.hasMoreElements();) {
 						Lecture l = (Lecture)e.nextElement();
 						if (l.getClassId().equals(assignment.getClassId())) {
 							lecture = l; break;
 						}
 					}
 					ids.add(assignment.getClassId());
 					if (lecture==null || lecture.getAssignment()==null) {
 						RecordedAssignment a = new RecordedAssignment(this, assignment.getPlacement(), null);
 						a.getBefore().setDetails(new ClassAssignmentDetails(solution, assignment, false, hibSession, null));
 						ret.addElement(a);
 					} else {
 						Placement placement = (Placement)lecture.getAssignment();
 						if (placement.equals(assignment.getPlacement())) continue;
 						RecordedAssignment a = new RecordedAssignment(this, assignment.getPlacement(), placement);
 						a.getBefore().setDetails(new ClassAssignmentDetails(solution, assignment, false, hibSession, null));
 						a.getAfter().setDetails(new ClassAssignmentDetails(this,lecture,false));
 						ret.addElement(a);
 					}
 				}
 				for (Enumeration e=currentSolution().getModel().variables().elements();e.hasMoreElements();) {
 					Lecture lecture = (Lecture)e.nextElement();
 					if (ids.contains(lecture.getClassId()) || lecture.getAssignment()==null) continue;
 					if (!ownerId.equals(lecture.getSolverGroupId())) continue;
 					Placement placement = (Placement)lecture.getAssignment();
 					RecordedAssignment a = new RecordedAssignment(this, null, placement);
 					a.getAfter().setDetails(new ClassAssignmentDetails(this,lecture,false));
 					ret.addElement(a);
 				}			
 				if (tx!=null) tx.commit();
 			} catch (Exception e) {
 				if (tx!=null) tx.rollback();
 				throw e;
 			} finally {
 				//here we still need to close the session since it can be called by the remote solver as well
 				if (closeSession && hibSession!=null && hibSession.isOpen())
 					hibSession.close();
 			}
 			return ret;
 		}
 	}
 
 	public static class AssignmentRecord implements Serializable {
 		private static final long serialVersionUID = 1L;
 		private transient Solver iSolver; 
 		private Date iTimeStamp = new Date();
 		private Suggestion iBefore = null, iAfter = null;
 		private Vector iAssignments = new Vector(); 
 		
 		public AssignmentRecord() {}
 		public AssignmentRecord(Solver solver) {
 			iSolver = solver;
 			iBefore = new Suggestion(iSolver);
 		}
 		public void done() {
 			iAfter = new Suggestion(iSolver);
 		}
 		public void add(Placement before, Placement after) {
 			iAssignments.add(new RecordedAssignment(iSolver, before, after));
 		}
 		public Date getTimeStamp() {
 			return iTimeStamp;
 		}
 		public Suggestion getBefore() { return iBefore; }
 		public Suggestion getAfter() { return iAfter; }
 		public Vector getAssignments() { return iAssignments; }
 		public String toString() {
 			return "Record{TS="+iTimeStamp+", before="+iBefore+", after="+iAfter+", assignments="+iAssignments.size()+"}";
 		}
 		public void toXml(Element element) throws Exception {
 			if (iTimeStamp!=null)
 				element.addAttribute("timeStamp", String.valueOf(iTimeStamp.getTime()));
 			if (iBefore!=null)
 				iBefore.toXml(element.addElement("before"));
 			if (iAfter!=null)
 				iAfter.toXml(element.addElement("after"));
 			if (iAssignments!=null) {
 				for (Enumeration e=iAssignments.elements();e.hasMoreElements();) {
 					RecordedAssignment ra = (RecordedAssignment)e.nextElement();
 					ra.toXml(element.addElement("assignment"));
 				}
 			}
 		}
 		public static AssignmentRecord fromXml(Element element) throws Exception {
 			AssignmentRecord r = new AssignmentRecord();
 			if (element.attributeValue("timeStamp")!=null)
 				r.iTimeStamp = new Date(Long.parseLong(element.attributeValue("timeStamp")));
 			if (element.element("before")!=null)
 				r.iBefore = Suggestion.fromXml(element.element("before"));
 			if (element.element("after")!=null)
 				r.iAfter = Suggestion.fromXml(element.element("after"));
 			for (Iterator i=element.elementIterator("assignment");i.hasNext();) {
 				r.iAssignments.add(RecordedAssignment.fromXml((Element)i.next()));
 			}
 			return r;
 		}
 	}
 	
 	public static class RecordedAssignment implements Serializable {
 		private static final long serialVersionUID = 1L;
 		private Hint iBefore = null, iAfter = null;
 		public RecordedAssignment(Solver solver, Placement before, Placement after) {
 			if (before!=null)
 				iBefore = new Hint(solver,before,false);
 			if (after!=null)
 				iAfter = new Hint(solver,after,false);
 		}
 		public RecordedAssignment(Hint before, Hint after) {
 			iBefore = before; iAfter = after;
 		}
 		public Hint getBefore() { return iBefore; }
 		public Hint getAfter() { return iAfter; }
 		public void toXml(Element element) {
 			if (iBefore!=null)
 				iBefore.toXml(element.addElement("before"));
 			if (iAfter!=null)
 				iAfter.toXml(element.addElement("after"));
 		}
 		public static RecordedAssignment fromXml(Element element) {
 			Hint before = null, after = null;
 			if (element.element("before")!=null) {
 				before = Hint.fromXml(element.element("before"));
 			}
 			if (element.element("after")!=null) {
 				after = Hint.fromXml(element.element("after"));
 			}
 			return new RecordedAssignment(before, after);
 		}
 	}
 	
 	public RoomReport getRoomReport(int startDay, int endDay, int nrWeeks, Integer roomType) {
 		synchronized (currentSolution()) {
 			return new RoomReport((TimetableModel)currentSolution().getModel(), startDay, endDay, nrWeeks, roomType);
 		}
 	}
 	public DeptBalancingReport getDeptBalancingReport() {
 		synchronized (currentSolution()) {
 			return new DeptBalancingReport(this);
 		}
 	}
 	public ViolatedDistrPreferencesReport getViolatedDistrPreferencesReport() {
 		synchronized (currentSolution()) {
 			return new ViolatedDistrPreferencesReport(this);
 		}
 	}
 	public DiscouragedInstructorBtbReport getDiscouragedInstructorBtbReport() {
 		synchronized (currentSolution()) {
 			return new DiscouragedInstructorBtbReport(this);
 		}
 	}
 	public StudentConflictsReport getStudentConflictsReport() {
 		synchronized (currentSolution()) {
 			return new StudentConflictsReport(this);
 		}
 	}
 	public SameSubpartBalancingReport getSameSubpartBalancingReport() {
 		synchronized (currentSolution()) {
 			return new SameSubpartBalancingReport(this);
 		}
 	}
 	public PerturbationReport getPerturbationReport() {
 		synchronized (currentSolution()) {
 			return new PerturbationReport(this);
 		}
 	}
 	
 	public void load(Element element) {
 		try {
 			iAssignmentRecords.clear(); iBestAssignmentRecords.clear();
 			Element assignmentRecords = element.element("assignmentRecords");
 			if (assignmentRecords!=null) {
 				for (Iterator i=assignmentRecords.elementIterator("record");i.hasNext();) {
 					iAssignmentRecords.add(AssignmentRecord.fromXml((Element)i.next()));
 				}
 			}
 			Element bestAssignmentRecords = element.element("bestAssignmentRecords");
 			if (bestAssignmentRecords!=null) {
 				for (Iterator i=bestAssignmentRecords.elementIterator("record");i.hasNext();) {
 					iBestAssignmentRecords.add(AssignmentRecord.fromXml((Element)i.next()));
 				}
 			}
 			if (element.element("cbsInfo")!=null) {
 				iCbsInfo = new ConflictStatisticsInfo();
 				iCbsInfo.load(element.element("cbsInfo"));
 			}
 		} catch (Exception e) {
 			sLog.error("Unable to load solver-related data, reson:"+e.getMessage(),e);
 		}
 	}
 	
 	public void save(Element element) {
 		try {
 			if (!iAssignmentRecords.isEmpty()) {
 				Element assignmentRecords = element.addElement("assignmentRecords");
 				for (Enumeration e=iAssignmentRecords.elements();e.hasMoreElements();) {
 					AssignmentRecord r = (AssignmentRecord)e.nextElement();
 					r.toXml(assignmentRecords.addElement("record"));
 				}
 			}
 			if (!iBestAssignmentRecords.isEmpty()) {
 				Element bestAssignmentRecords = element.addElement("bestAssignmentRecords");
 				for (Enumeration e=iBestAssignmentRecords.elements();e.hasMoreElements();) {
 					AssignmentRecord r = (AssignmentRecord)e.nextElement();
 					r.toXml(bestAssignmentRecords.addElement("record"));
 				}
 			}
 			ConflictStatisticsInfo cbsInfo = getCbsInfo();
 			if (cbsInfo!=null)
 				cbsInfo.save(element.addElement("cbsInfo"));
 		} catch (Exception e) {
 			sLog.error("Unable to save solver-related data, reson:"+e.getMessage(),e);
 		}
 	}
 	
 	public CSVFile export() {
 		synchronized (currentSolution()) {
 			CSVFile file = new CSVFile();
 			file.setSeparator(",");
 			file.setQuotationMark("\"");
 			file.setHeader(new CSVField[] {
 					new CSVField("COURSE"),
 					new CSVField("ITYPE"),
 					new CSVField("SECTION"),
 					new CSVField("SUFFIX"),
 					new CSVField("DATE_PATTERN"),
 					new CSVField("DAY"),
 					new CSVField("START_TIME"),
 					new CSVField("END_TIME"),
 					new CSVField("ROOM"),
 					new CSVField("INSTRUCTOR")
 			});
 			
 			Vector lectures = new Vector(currentSolution().getModel().variables());
 			Collections.sort(lectures);
 			for (Iterator i=lectures.iterator();i.hasNext();) {
 				Lecture lecture = (Lecture)i.next();
 				Placement placement = (Placement)lecture.getAssignment();
 				String name = lecture.getName();
 				String itype = ""; String section = "";
 				if (name.indexOf(' ')>=0) {
 					section = name.substring(name.lastIndexOf(' ')+1);
 					name = name.substring(0, name.lastIndexOf(' ')).trim();
 				}
 				String suffix="";
 				while (section.charAt(section.length()-1)>='a' && section.charAt(section.length()-1)<='z') {
 					suffix = section.charAt(section.length()-1)+suffix;
 					section = section.substring(0,section.length()-1);
 				}
 				if (name.indexOf(' ')>=0) {
 					itype = name.substring(name.lastIndexOf(' ')+1);
 					name = name.substring(0, name.lastIndexOf(' ')).trim();
 				}
 				try {
 					Integer.parseInt(itype);
 					itype = name.substring(name.lastIndexOf(' ')+1)+" "+itype;
 					name = name.substring(0, name.lastIndexOf(' ')).trim();
 				} catch (NumberFormatException e) {}
 				file.addLine(new CSVField[] {
 						new CSVField(name),
 						new CSVField(itype),
 						new CSVField(section),
 						new CSVField(suffix),
 						new CSVField(placement==null?"":placement.getTimeLocation().getDatePatternName()),
 						new CSVField(placement==null?"":placement.getTimeLocation().getDayHeader()),
 						new CSVField(placement==null?"":placement.getTimeLocation().getStartTimeHeader()),
 						new CSVField(placement==null?"":placement.getTimeLocation().getEndTimeHeader()),
 						new CSVField(placement==null?"":placement.getRoomName(",")),
 						new CSVField(lecture.getInstructorName()==null?"":lecture.getInstructorName())
 				});
 			}
 			return file;
 		}
 	}
 	
 	public synchronized boolean isPassivated() {
 		return iIsPassivated;
 	}
 	
 	public synchronized long timeFromLastUsed() {
 		return System.currentTimeMillis()-iLastTimeStamp;
 	}
 	
 	public synchronized boolean activateIfNeeded() {
 		iLastTimeStamp = System.currentTimeMillis();
 		if (!isPassivated()) return false;
 		sLog.debug("<activate "+iPassivationPuid+">");
 
 		iIsPassivated = false;
 
 		System.gc();
 		sLog.debug(" -- memory usage before activation:"+org.unitime.commons.Debug.getMem());
 		restore(iPassivationFolder, iPassivationPuid, true);
         System.gc();
         sLog.debug(" -- memory usage after activation:"+org.unitime.commons.Debug.getMem());
 		
 		return true;
 	}
 	
 	public synchronized boolean passivate(File folder, String puid) {
 		if (isPassivated() || super.currentSolution()==null || super.currentSolution().getModel()==null) return false;
 		sLog.debug("<passivate "+puid+">");
         System.gc();
         sLog.debug(" -- memory usage before passivation:"+org.unitime.commons.Debug.getMem());
 		iProgressBeforePassivation = getProgress();
 		if (iProgressBeforePassivation!=null)
 			iProgressBeforePassivation.put("STATUS","Pasivated");
 		iGlobalInfoBeforePassivation = getGlobalInfo();
 		iCurrentSolutionInfoBeforePassivation = currentSolutionInfo();
 		iBestSolutionInfoBeforePassivation = bestSolutionInfo();
 		
 		iPassivationFolder = folder;
 		iPassivationPuid = puid;
 		backup(iPassivationFolder, iPassivationPuid);
 
    		disposeNoInherit();
         System.gc();
         sLog.debug(" -- memory usage after passivation:"+org.unitime.commons.Debug.getMem());
 		
 		iIsPassivated = true;
 		return true;
 	}
 
 	public synchronized boolean passivateIfNeeded(File folder, String puid) {
 		if (isPassivated() || timeFromLastUsed()<sInactiveTimeToPassivate || isWorking()) return false;
 		return passivate(folder, puid);
 	}
 	
 	public Date getLastUsed() {
 		return new Date(iLastTimeStamp);
 	}
     
     protected void onAssigned(double startTime) {
         // Check if the solution is the best ever found one
         if (iCurrentSolution.getModel().unassignedVariables().isEmpty() && getSolutionComparator().isBetterThanBestSolution(iCurrentSolution)) {
             fixCompleteSolution(startTime);
         } else {
             // If the solver is not able to improve solution in the last 5000 iterations, take the best one and try to fix it
             if (iCurrentSolution.getBestInfo()!=null && iCurrentSolution.getModel().getBestUnassignedVariables()>0 && iCurrentSolution.getIteration()==iCurrentSolution.getBestIteration()+5000) {
                 iCurrentSolution.restoreBest();
                 fixCompleteSolution(startTime);
             }
         }
     }
 }
