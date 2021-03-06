 /**********************************************************************
  **                                                                   **
  **               This code belongs to the KETTLE project.            **
  **                                                                   **
  ** Kettle, from version 2.2 on, is released into the public domain   **
  ** under the Lesser GNU Public License (LGPL).                       **
  **                                                                   **
  ** For more details, please read the document LICENSE.txt, included  **
  ** in this project                                                   **
  **                                                                   **
  ** http://www.kettle.be                                              **
  ** info@kettle.be                                                    **
  **                                                                   **
  **********************************************************************/
 
 package be.ibridge.kettle.trans;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Comparator;
 import java.util.Date;
 import java.util.Enumeration;
 import java.util.Hashtable;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.jface.dialogs.MessageDialog;
 import org.eclipse.jface.dialogs.MessageDialogWithToggle;
 import org.eclipse.swt.widgets.Shell;
 import org.w3c.dom.Document;
 import org.w3c.dom.Node;
 
 import be.ibridge.kettle.cluster.ClusterSchema;
 import be.ibridge.kettle.cluster.SlaveServer;
 import be.ibridge.kettle.core.CheckResult;
 import be.ibridge.kettle.core.Const;
 import be.ibridge.kettle.core.DBCache;
 import be.ibridge.kettle.core.KettleVariables;
 import be.ibridge.kettle.core.LogWriter;
 import be.ibridge.kettle.core.NotePadMeta;
 import be.ibridge.kettle.core.Point;
 import be.ibridge.kettle.core.Props;
 import be.ibridge.kettle.core.Rectangle;
 import be.ibridge.kettle.core.Result;
 import be.ibridge.kettle.core.Row;
 import be.ibridge.kettle.core.SQLStatement;
 import be.ibridge.kettle.core.SharedObjectInterface;
 import be.ibridge.kettle.core.SharedObjects;
 import be.ibridge.kettle.core.TransAction;
 import be.ibridge.kettle.core.XMLHandler;
 import be.ibridge.kettle.core.XMLInterface;
 import be.ibridge.kettle.core.database.Database;
 import be.ibridge.kettle.core.database.DatabaseMeta;
 import be.ibridge.kettle.core.exception.KettleDatabaseException;
 import be.ibridge.kettle.core.exception.KettleException;
 import be.ibridge.kettle.core.exception.KettleStepException;
 import be.ibridge.kettle.core.exception.KettleXMLException;
 import be.ibridge.kettle.core.reflection.StringSearchResult;
 import be.ibridge.kettle.core.reflection.StringSearcher;
 import be.ibridge.kettle.core.util.StringUtil;
 import be.ibridge.kettle.core.value.Value;
 import be.ibridge.kettle.partition.PartitionSchema;
 import be.ibridge.kettle.repository.Repository;
 import be.ibridge.kettle.repository.RepositoryDirectory;
 import be.ibridge.kettle.spoon.Spoon;
 import be.ibridge.kettle.trans.step.StepMeta;
 import be.ibridge.kettle.trans.step.StepMetaInterface;
 import be.ibridge.kettle.trans.step.StepPartitioningMeta;
 
 /**
  * This class defines a transformation and offers methods to save and load it from XML or a Kettle database repository.
  *
  * @since 20-jun-2003
  * @author Matt
  *
  */
 public class TransMeta implements XMLInterface, Comparator
 {
     public static final String XML_TAG = "transformation";
 
     private static LogWriter    log                = LogWriter.getInstance();
 
     private List                inputFiles;
 
     private ArrayList           databases;
 
     private ArrayList           steps;
 
     private ArrayList           hops;
 
     private ArrayList           notes;
 
     private ArrayList           dependencies;
     
     private ArrayList           slaveServers;
     
     private ArrayList           clusterSchemas;
 
     private RepositoryDirectory directory;
 
     private RepositoryDirectory directoryTree;
 
     private String              name;
 
     private String              filename;
 
     private StepMeta            readStep;
 
     private StepMeta            writeStep;
 
     private StepMeta            inputStep;
 
     private StepMeta            outputStep;
 
     private StepMeta            updateStep;
 
     private String              logTable;
 
     private DatabaseMeta        logConnection;
 
     private int                 sizeRowset;
 
     private DatabaseMeta        maxDateConnection;
 
     private String              maxDateTable;
 
     private String              maxDateField;
 
     private double              maxDateOffset;
 
     private double              maxDateDifference;
 
     private String              arguments[];
 
     private Hashtable           counters;
 
     private ArrayList           sourceRows;
 
     private boolean             changed, changed_steps, changed_databases, changed_hops, changed_notes;
 
     private ArrayList           undo;
 
     private int                 max_undo;
 
     private int                 undo_position;
 
     private DBCache             dbCache;
 
     private long                id;
 
     private boolean             useBatchId;
 
     private boolean             logfieldUsed;
 
     private String              createdUser, modifiedUser;
 
     private Value               createdDate, modifiedDate;
 
     private int                 sleepTimeEmpty;
 
     private int                 sleepTimeFull;
 
 	private Result              previousResult;
     private ArrayList           resultRows;
     private ArrayList           resultFiles;            
     
     
     private List                partitionSchemas;
 
     private boolean             usingUniqueConnections;
     
     private boolean             feedbackShown;
     private int                 feedbackSize;
     
     private boolean             usingThreadPriorityManagment;
     
     /** If this is null, we load from the default shared objects file : $KETTLE_HOME/.kettle/shared.xml */
     private String              sharedObjectsFile;
     
     // //////////////////////////////////////////////////////////////////////////
 
     public static final int     TYPE_UNDO_CHANGE   = 1;
 
     public static final int     TYPE_UNDO_NEW      = 2;
 
     public static final int     TYPE_UNDO_DELETE   = 3;
 
     public static final int     TYPE_UNDO_POSITION = 4;
 
     public static final String  desc_type_undo[]   = { "", Messages.getString("TransMeta.UndoTypeDesc.UndoChange"), Messages.getString("TransMeta.UndoTypeDesc.UndoNew"), Messages.getString("TransMeta.UndoTypeDesc.UndoDelete"), Messages.getString("TransMeta.UndoTypeDesc.UndoPosition") }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
 
     private static final String STRING_MODIFIED_DATE = "modified_date";
 
 
     /**
      * Builds a new empty transformation.
      */
     public TransMeta()
     {
         clear();
     }
 
     /**
      * Constructs a new transformation specifying the filename, name and arguments.
      *
      * @param filename The filename of the transformation
      * @param name The name of the transformation
      * @param arguments The arguments as Strings
      */
     public TransMeta(String filename, String name, String arguments[])
     {
         clear();
         this.filename = filename;
         this.name = name;
         this.arguments = arguments;
     }
 
     /**
      * Compares two transformation on name, filename
      */
     public int compare(Object o1, Object o2) 
     {
         TransMeta t1 = (TransMeta) o1;
         TransMeta t2 = (TransMeta) o2;
         
         if (Const.isEmpty(t1.getName()) && !Const.isEmpty(t2.getName())) return -1;
         if (!Const.isEmpty(t1.getName()) && Const.isEmpty(t2.getName())) return  1;
         if (Const.isEmpty(t1.getName()) && Const.isEmpty(t2.getName()))
         {
             if (Const.isEmpty(t1.getFilename()) && !Const.isEmpty(t2.getFilename())) return -1;
             if (Const.isEmpty(t1.getFilename()) && Const.isEmpty(t2.getFilename())) return  1;
             if (Const.isEmpty(t1.getFilename()) && Const.isEmpty(t2.getFilename()))
             {
                 return 0;
             }
             return t1.getFilename().compareTo(t2.getFilename());
         }
         return t1.getName().compareTo(t2.getName()); 
     } 
     
     public boolean equals(Object obj)
     {
         return compare(this, obj)==0;
     }
 
     /**
      * Get the database ID in the repository for this object.
      *
      * @return the database ID in the repository for this object.
      */
     public long getID()
     {
         return id;
     }
 
     /**
      * Set the database ID for this object in the repository.
      *
      * @param id the database ID for this object in the repository.
      */
     public void setID(long id)
     {
         this.id = id;
     }
 
     /**
      * Clears the transformation.
      */
     public void clear()
     {
         setID(-1L);
         databases = new ArrayList();
         steps = new ArrayList();
         hops = new ArrayList();
         notes = new ArrayList();
         dependencies = new ArrayList();
         partitionSchemas = new ArrayList();
         slaveServers = new ArrayList();
         clusterSchemas = new ArrayList();
         
         name = null;
         filename = null;
         readStep = null;
         writeStep = null;
         inputStep = null;
         outputStep = null;
         updateStep = null;
         logTable = null;
         logConnection = null;
 
         sizeRowset     = Const.ROWS_IN_ROWSET;
         sleepTimeEmpty = Const.SLEEP_EMPTY_NANOS;
         sleepTimeFull  = Const.SLEEP_FULL_NANOS;
 
         maxDateConnection = null;
         maxDateTable = null;
         maxDateField = null;
         maxDateOffset = 0.0;
 
         maxDateDifference = 0.0;
 
         undo = new ArrayList();
         max_undo = Const.MAX_UNDO;
         undo_position = -1;
 
         counters = new Hashtable();
         resultRows = null;
 
         clearUndo();
         clearChanged();
 
         useBatchId = true; // Make this one the default from now on...
         logfieldUsed = false; // Don't use the log-field by default...
 
         modifiedUser = "-"; //$NON-NLS-1$
         modifiedDate = new Value("modified_date", new Date()); //$NON-NLS-1$
 
         // LOAD THE DATABASE CACHE!
         dbCache = DBCache.getInstance();
 
         directoryTree = new RepositoryDirectory();
 
         // Default directory: root
         directory = directoryTree;
         
         resultRows = new ArrayList();
         resultFiles = new ArrayList();
         
         feedbackShown = true;
         feedbackSize = Const.ROWS_UPDATE;
         
         usingThreadPriorityManagment = true;
         
         
         // For testing purposes only, we add a single cluster schema.
         // T O D O: remove this test-code later on.
         /*
         ClusterSchema clusterSchema = new ClusterSchema();
         clusterSchema.setName("Local cluster");
         SlaveServer localSlave = new SlaveServer("127.0.0.1", "80", null, null, null, null, null);
         clusterSchema.getSlaveServers().add(localSlave);
         clusterSchemas.add(clusterSchema);
         */
 
     }
 
     public void clearUndo()
     {
         undo = new ArrayList();
         undo_position = -1;
     }
 
     /**
      * Get an ArrayList of defined DatabaseInfo objects.
      *
      * @return an ArrayList of defined DatabaseInfo objects.
      */
     public ArrayList getDatabases()
     {
         return databases;
     }
 
     /**
      * @param databases The databases to set.
      */
     public void setDatabases(ArrayList databases)
     {
         this.databases = databases;
     }
 
     /**
      * Add a database connection to the transformation.
      *
      * @param databaseMeta The database connection information.
      */
     public void addDatabase(DatabaseMeta databaseMeta)
     {
         databases.add(databaseMeta);
     }
     
     /**
      * Add a database connection to the transformation if that connection didn't exists yet.
      * Otherwise, replace the connection in the transformation
      *
      * @param databaseMeta The database connection information.
      */
     public void addOrReplaceDatabase(DatabaseMeta databaseMeta)
     {
         int index = databases.indexOf(databaseMeta);
         if (index<0)
         {
             databases.add(databaseMeta); 
         }
         else
         {
             DatabaseMeta previous = getDatabase(index);
             previous.replaceMeta(databaseMeta);
         }
         changed_databases = true;
     }
 
     /**
      * Add a new step to the transformation
      *
      * @param stepMeta The step to be added.
      */
     public void addStep(StepMeta stepMeta)
     {
         steps.add(stepMeta);
         changed_steps = true;
     }
     
     /**
      * Add a new step to the transformation if that step didn't exist yet.
      * Otherwise, replace the step.
      *
      * @param stepMeta The step to be added.
      */
     public void addOrReplaceStep(StepMeta stepMeta)
     {
         int index = steps.indexOf(stepMeta);
         if (index<0)
         {
             steps.add(stepMeta); 
         }
         else
         {
             StepMeta previous = getStep(index);
             previous.replaceMeta(stepMeta);
         }
         changed_steps = true;
     }
 
 
     /**
      * Add a new hop to the transformation.
      *
      * @param hi The hop to be added.
      */
     public void addTransHop(TransHopMeta hi)
     {
         hops.add(hi);
         changed_hops = true;
     }
 
     /**
      * Add a new note to the transformation.
      *
      * @param ni The note to be added.
      */
     public void addNote(NotePadMeta ni)
     {
         notes.add(ni);
         changed_notes = true;
     }
 
     /**
      * Add a new dependency to the transformation.
      *
      * @param td The transformation dependency to be added.
      */
     public void addDependency(TransDependency td)
     {
         dependencies.add(td);
     }
 
     /**
      * Add a database connection to the transformation on a certain location.
      *
      * @param p The location
      * @param ci The database connection information.
      */
     public void addDatabase(int p, DatabaseMeta ci)
     {
         databases.add(p, ci);
     }
 
     /**
      * Add a new step to the transformation
      *
      * @param p The location
      * @param stepMeta The step to be added.
      */
     public void addStep(int p, StepMeta stepMeta)
     {
         steps.add(p, stepMeta);
         changed_steps = true;
     }
 
     /**
      * Add a new hop to the transformation on a certain location.
      *
      * @param p the location
      * @param hi The hop to be added.
      */
     public void addTransHop(int p, TransHopMeta hi)
     {
         hops.add(p, hi);
         changed_hops = true;
     }
 
     /**
      * Add a new note to the transformation on a certain location.
      *
      * @param p The location
      * @param ni The note to be added.
      */
     public void addNote(int p, NotePadMeta ni)
     {
         notes.add(p, ni);
         changed_notes = true;
     }
 
     /**
      * Add a new dependency to the transformation on a certain location
      *
      * @param p The location.
      * @param td The transformation dependency to be added.
      */
     public void addDependency(int p, TransDependency td)
     {
         dependencies.add(p, td);
     }
 
     /**
      * Retrieves a database connection information a a certain location.
      *
      * @param i The database number.
      * @return The database connection information.
      */
     public DatabaseMeta getDatabase(int i)
     {
         return (DatabaseMeta) databases.get(i);
     }
 
     /**
      * Get an ArrayList of defined steps.
      *
      * @return an ArrayList of defined steps.
      */
     public ArrayList getSteps()
     {
         return steps;
     }
 
     /**
      * Retrieves a step on a certain location.
      *
      * @param i The location.
      * @return The step information.
      */
     public StepMeta getStep(int i)
     {
         return (StepMeta) steps.get(i);
     }
 
     /**
      * Retrieves a hop on a certain location.
      *
      * @param i The location.
      * @return The hop information.
      */
     public TransHopMeta getTransHop(int i)
     {
         return (TransHopMeta) hops.get(i);
     }
 
     /**
      * Retrieves notepad information on a certain location.
      *
      * @param i The location
      * @return The notepad information.
      */
     public NotePadMeta getNote(int i)
     {
         return (NotePadMeta) notes.get(i);
     }
 
     /**
      * Retrieves a dependency on a certain location.
      *
      * @param i The location.
      * @return The dependency.
      */
     public TransDependency getDependency(int i)
     {
         return (TransDependency) dependencies.get(i);
     }
 
     /**
      * Removes a database from the transformation on a certain location.
      *
      * @param i The location
      */
     public void removeDatabase(int i)
     {
         if (i < 0 || i >= databases.size()) return;
         databases.remove(i);
     }
 
     /**
      * Removes a step from the transformation on a certain location.
      *
      * @param i The location
      */
     public void removeStep(int i)
     {
         if (i < 0 || i >= steps.size()) return;
 
         steps.remove(i);
         changed_steps = true;
     }
 
     /**
      * Removes a hop from the transformation on a certain location.
      *
      * @param i The location
      */
     public void removeTransHop(int i)
     {
         if (i < 0 || i >= hops.size()) return;
 
         hops.remove(i);
         changed_hops = true;
     }
 
     /**
      * Removes a note from the transformation on a certain location.
      *
      * @param i The location
      */
     public void removeNote(int i)
     {
         if (i < 0 || i >= notes.size()) return;
         notes.remove(i);
         changed_notes = true;
     }
 
     /**
      * Removes a dependency from the transformation on a certain location.
      *
      * @param i The location
      */
     public void removeDependency(int i)
     {
         if (i < 0 || i >= dependencies.size()) return;
         dependencies.remove(i);
     }
 
     /**
      * Clears all the dependencies from the transformation.
      */
     public void removeAllDependencies()
     {
         dependencies.clear();
     }
 
     /**
      * Count the nr of databases in the transformation.
      *
      * @return The nr of databases
      */
     public int nrDatabases()
     {
         return databases.size();
     }
 
     /**
      * Count the nr of steps in the transformation.
      *
      * @return The nr of steps
      */
     public int nrSteps()
     {
         return steps.size();
     }
 
     /**
      * Count the nr of hops in the transformation.
      *
      * @return The nr of hops
      */
     public int nrTransHops()
     {
         return hops.size();
     }
 
     /**
      * Count the nr of notes in the transformation.
      *
      * @return The nr of notes
      */
     public int nrNotes()
     {
         return notes.size();
     }
 
     /**
      * Count the nr of dependencies in the transformation.
      *
      * @return The nr of dependencies
      */
     public int nrDependencies()
     {
         return dependencies.size();
     }
 
     /**
      * Changes the content of a step on a certain position
      *
      * @param i The position
      * @param stepMeta The Step
      */
     public void setStep(int i, StepMeta stepMeta)
     {
         steps.set(i, stepMeta);
     }
 
     /**
      * Changes the content of a hop on a certain position
      *
      * @param i The position
      * @param hi The hop
      */
     public void setTransHop(int i, TransHopMeta hi)
     {
         hops.set(i, hi);
     }
 
     /**
      * Counts the number of steps that are actually used in the transformation.
      *
      * @return the number of used steps.
      */
     public int nrUsedSteps()
     {
         int nr = 0;
         for (int i = 0; i < nrSteps(); i++)
         {
             StepMeta stepMeta = getStep(i);
             if (isStepUsedInTransHops(stepMeta)) nr++;
         }
         return nr;
     }
 
     /**
      * Gets a used step on a certain location
      *
      * @param lu The location
      * @return The used step.
      */
     public StepMeta getUsedStep(int lu)
     {
         int nr = 0;
         for (int i = 0; i < nrSteps(); i++)
         {
             StepMeta stepMeta = getStep(i);
             if (isStepUsedInTransHops(stepMeta))
             {
                 if (lu == nr) return stepMeta;
                 nr++;
             }
         }
         return null;
     }
 
     /**
      * Searches the list of databases for a database with a certain name
      *
      * @param name The name of the database connection
      * @return The database connection information or null if nothing was found.
      */
     public DatabaseMeta findDatabase(String name)
     {
         int i;
         for (i = 0; i < nrDatabases(); i++)
         {
             DatabaseMeta ci = getDatabase(i);
             if (ci.getName().equalsIgnoreCase(name)) { return ci; }
         }
         return null;
     }
 
     /**
      * Searches the list of steps for a step with a certain name
      *
      * @param name The name of the step to look for
      * @return The step information or null if no nothing was found.
      */
     public StepMeta findStep(String name)
     {
         return findStep(name, null);
     }
 
     /**
      * Searches the list of steps for a step with a certain name while excluding one step.
      *
      * @param name The name of the step to look for
      * @param exclude The step information to exclude.
      * @return The step information or null if nothing was found.
      */
     public StepMeta findStep(String name, StepMeta exclude)
     {
         if (name==null) return null;
 
         int excl = -1;
         if (exclude != null) excl = indexOfStep(exclude);
 
         for (int i = 0; i < nrSteps(); i++)
         {
             StepMeta stepMeta = getStep(i);
             if (i != excl && stepMeta.getName().equalsIgnoreCase(name)) { return stepMeta; }
         }
         return null;
     }
 
     /**
      * Searches the list of hops for a hop with a certain name
      *
      * @param name The name of the hop to look for
      * @return The hop information or null if nothing was found.
      */
     public TransHopMeta findTransHop(String name)
     {
         int i;
 
         for (i = 0; i < nrTransHops(); i++)
         {
             TransHopMeta hi = getTransHop(i);
             if (hi.toString().equalsIgnoreCase(name)) { return hi; }
         }
         return null;
     }
 
     /**
      * Search all hops for a hop where a certain step is at the start.
      *
      * @param fromstep The step at the start of the hop.
      * @return The hop or null if no hop was found.
      */
     public TransHopMeta findTransHopFrom(StepMeta fromstep)
     {
         int i;
         for (i = 0; i < nrTransHops(); i++)
         {
             TransHopMeta hi = getTransHop(i);
             if (hi.getFromStep() != null && hi.getFromStep().equals(fromstep)) // return the first
             { return hi; }
         }
         return null;
     }
 
     /**
      * Find a certain hop in the transformation..
      *
      * @param hi The hop information to look for.
      * @return The hop or null if no hop was found.
      */
     public TransHopMeta findTransHop(TransHopMeta hi)
     {
         return findTransHop(hi.getFromStep(), hi.getToStep());
     }
 
     /**
      * Search all hops for a hop where a certain step is at the start and another is at the end.
      *
      * @param from The step at the start of the hop.
      * @param to The step at the end of the hop.
      * @return The hop or null if no hop was found.
      */
     public TransHopMeta findTransHop(StepMeta from, StepMeta to)
     {
 
         int i;
         for (i = 0; i < nrTransHops(); i++)
         {
             TransHopMeta hi = getTransHop(i);
             if (hi.isEnabled())
             {
                 if (hi.getFromStep() != null && hi.getToStep() != null && hi.getFromStep().equals(from) && hi.getToStep().equals(to)) { return hi; }
             }
         }
         return null;
     }
 
     /**
      * Search all hops for a hop where a certain step is at the end.
      *
      * @param tostep The step at the end of the hop.
      * @return The hop or null if no hop was found.
      */
     public TransHopMeta findTransHopTo(StepMeta tostep)
     {
         int i;
         for (i = 0; i < nrTransHops(); i++)
         {
             TransHopMeta hi = getTransHop(i);
             if (hi.getToStep() != null && hi.getToStep().equals(tostep)) // Return the first!
             { return hi; }
         }
         return null;
     }
 
     /**
      * Determines whether or not a certain step is informative. This means that the previous step is sending information
      * to this step, but only informative. This means that this step is using the information to process the actual
      * stream of data. We use this in StreamLookup, TableInput and other types of steps.
      *
      * @param this_step The step that is receiving information.
      * @param prev_step The step that is sending information
      * @return true if prev_step if informative for this_step.
      */
     public boolean isStepInformative(StepMeta this_step, StepMeta prev_step)
     {
         String[] infoSteps = this_step.getStepMetaInterface().getInfoSteps();
         if (infoSteps == null) return false;
         for (int i = 0; i < infoSteps.length; i++)
         {
             if (prev_step.getName().equalsIgnoreCase(infoSteps[i])) return true;
         }
 
         return false;
     }
 
     /**
      * Counts the number of previous steps for a step name.
      *
      * @param stepname The name of the step to start from
      * @return The number of preceding steps.
      */
     public int findNrPrevSteps(String stepname)
     {
         return findNrPrevSteps(findStep(stepname), false);
     }
 
     /**
      * Counts the number of previous steps for a step name taking into account whether or not they are informational.
      *
      * @param stepname The name of the step to start from
      * @return The number of preceding steps.
      */
     public int findNrPrevSteps(String stepname, boolean info)
     {
         return findNrPrevSteps(findStep(stepname), info);
     }
 
     /**
      * Find the number of steps that precede the indicated step.
      *
      * @param stepMeta The source step
      *
      * @return The number of preceding steps found.
      */
     public int findNrPrevSteps(StepMeta stepMeta)
     {
         return findNrPrevSteps(stepMeta, false);
     }
 
     /**
      * Find the previous step on a certain location.
      *
      * @param stepname The source step name
      * @param nr the location
      *
      * @return The preceding step found.
      */
     public StepMeta findPrevStep(String stepname, int nr)
     {
         return findPrevStep(findStep(stepname), nr);
     }
 
     /**
      * Find the previous step on a certain location taking into account the steps being informational or not.
      *
      * @param stepname The name of the step
      * @param nr The location
      * @param info true if we only want the informational steps.
      * @return The step information
      */
     public StepMeta findPrevStep(String stepname, int nr, boolean info)
     {
         return findPrevStep(findStep(stepname), nr, info);
     }
 
     /**
      * Find the previous step on a certain location.
      *
      * @param stepMeta The source step information
      * @param nr the location
      *
      * @return The preceding step found.
      */
     public StepMeta findPrevStep(StepMeta stepMeta, int nr)
     {
         return findPrevStep(stepMeta, nr, false);
     }
 
     /**
      * Count the number of previous steps on a certain location taking into account the steps being informational or
      * not.
      *
      * @param stepMeta The name of the step
      * @param info true if we only want the informational steps.
      * @return The number of preceding steps
      */
     public int findNrPrevSteps(StepMeta stepMeta, boolean info)
     {
         int count = 0;
         int i;
 
         for (i = 0; i < nrTransHops(); i++) // Look at all the hops;
         {
             TransHopMeta hi = getTransHop(i);
             if (hi.getToStep() != null && hi.isEnabled() && hi.getToStep().equals(stepMeta))
             {
                 // Check if this previous step isn't informative (StreamValueLookup)
                 // We don't want fields from this stream to show up!
                 if (info || !isStepInformative(stepMeta, hi.getFromStep()))
                 {
                     count++;
                 }
             }
         }
         return count;
     }
 
     /**
      * Find the previous step on a certain location taking into account the steps being informational or not.
      *
      * @param stepMeta The step
      * @param nr The location
      * @param info true if we only want the informational steps.
      * @return The preceding step information
      */
     public StepMeta findPrevStep(StepMeta stepMeta, int nr, boolean info)
     {
         int count = 0;
         int i;
 
         for (i = 0; i < nrTransHops(); i++) // Look at all the hops;
         {
             TransHopMeta hi = getTransHop(i);
             if (hi.getToStep() != null && hi.isEnabled() && hi.getToStep().equals(stepMeta))
             {
                 if (info || !isStepInformative(stepMeta, hi.getFromStep()))
                 {
                     if (count == nr) { return hi.getFromStep(); }
                     count++;
                 }
             }
         }
         return null;
     }
 
     /**
      * Get the informational steps for a certain step. An informational step is a step that provides information for
      * lookups etc.
      *
      * @param stepMeta The name of the step
      * @return The informational steps found
      */
     public StepMeta[] getInfoStep(StepMeta stepMeta)
     {
         String[] infoStepName = stepMeta.getStepMetaInterface().getInfoSteps();
         if (infoStepName == null) return null;
 
         StepMeta[] infoStep = new StepMeta[infoStepName.length];
         for (int i = 0; i < infoStep.length; i++)
         {
             infoStep[i] = findStep(infoStepName[i]);
         }
 
         return infoStep;
     }
 
     /**
      * Find the the number of informational steps for a certains step.
      *
      * @param stepMeta The step
      * @return The number of informational steps found.
      */
     public int findNrInfoSteps(StepMeta stepMeta)
     {
         if (stepMeta == null) return 0;
 
         int count = 0;
 
         for (int i = 0; i < nrTransHops(); i++) // Look at all the hops;
         {
             TransHopMeta hi = getTransHop(i);
             if (hi == null || hi.getToStep() == null)
             {
                 log.logError(toString(), Messages.getString("TransMeta.Log.DestinationOfHopCannotBeNull")); //$NON-NLS-1$
             }
             if (hi != null && hi.getToStep() != null && hi.isEnabled() && hi.getToStep().equals(stepMeta))
             {
                 // Check if this previous step isn't informative (StreamValueLookup)
                 // We don't want fields from this stream to show up!
                 if (isStepInformative(stepMeta, hi.getFromStep()))
                 {
                     count++;
                 }
             }
         }
         return count;
     }
 
     /**
      * Find the informational fields coming from an informational step into the step specified.
      *
      * @param stepname The name of the step
      * @return A row containing fields with origin.
      */
     public Row getPrevInfoFields(String stepname) throws KettleStepException
     {
         return getPrevInfoFields(findStep(stepname));
     }
 
     /**
      * Find the informational fields coming from an informational step into the step specified.
      *
      * @param stepMeta The receiving step
      * @return A row containing fields with origin.
      */
     public Row getPrevInfoFields(StepMeta stepMeta) throws KettleStepException
     {
         Row row = new Row();
 
         for (int i = 0; i < nrTransHops(); i++) // Look at all the hops;
         {
             TransHopMeta hi = getTransHop(i);
             if (hi.isEnabled() && hi.getToStep().equals(stepMeta))
             {
                 if (isStepInformative(stepMeta, hi.getFromStep()))
                 {
                     getThisStepFields(stepMeta, row);
                     return row;
                 }
             }
         }
         return row;
     }
 
     /**
      * Find the number of succeeding steps for a certain originating step.
      *
      * @param stepMeta The originating step
      * @return The number of succeeding steps.
      */
     public int findNrNextSteps(StepMeta stepMeta)
     {
         int count = 0;
         int i;
         for (i = 0; i < nrTransHops(); i++) // Look at all the hops;
         {
             TransHopMeta hi = getTransHop(i);
             if (hi.isEnabled() && hi.getFromStep().equals(stepMeta)) count++;
         }
         return count;
     }
 
     /**
      * Find the succeeding step at a location for an originating step.
      *
      * @param stepMeta The originating step
      * @param nr The location
      * @return The step found.
      */
     public StepMeta findNextStep(StepMeta stepMeta, int nr)
     {
         int count = 0;
         int i;
 
         for (i = 0; i < nrTransHops(); i++) // Look at all the hops;
         {
             TransHopMeta hi = getTransHop(i);
             if (hi.isEnabled() && hi.getFromStep().equals(stepMeta))
             {
                 if (count == nr) { return hi.getToStep(); }
                 count++;
             }
         }
         return null;
     }
 
     /**
      * Retrieve an array of preceding steps for a certain destination step.
      *
      * @param stepMeta The destination step
      * @return An array containing the preceding steps.
      */
     public StepMeta[] getPrevSteps(StepMeta stepMeta)
     {
         int nr = findNrPrevSteps(stepMeta, true);
         StepMeta retval[] = new StepMeta[nr];
 
         for (int i = 0; i < nr; i++)
         {
             retval[i] = findPrevStep(stepMeta, i, true);
         }
         return retval;
     }
 
     /**
      * Retrieve an array of succeeding step names for a certain originating step name.
      *
      * @param stepname The originating step name
      * @return An array of succeeding step names
      */
     public String[] getPrevStepNames(String stepname)
     {
         return getPrevStepNames(findStep(stepname));
     }
 
     /**
      * Retrieve an array of preceding steps for a certain destination step.
      *
      * @param stepMeta The destination step
      * @return an array of preceding step names.
      */
     public String[] getPrevStepNames(StepMeta stepMeta)
     {
         StepMeta prevStepMetas[] = getPrevSteps(stepMeta);
         String retval[] = new String[prevStepMetas.length];
         for (int x = 0; x < prevStepMetas.length; x++)
             retval[x] = prevStepMetas[x].getName();
 
         return retval;
     }
 
     /**
      * Retrieve an array of succeeding steps for a certain originating step.
      *
      * @param stepMeta The originating step
      * @return an array of succeeding steps.
      */
     public StepMeta[] getNextSteps(StepMeta stepMeta)
     {
         int nr = findNrNextSteps(stepMeta);
         StepMeta retval[] = new StepMeta[nr];
 
         for (int i = 0; i < nr; i++)
         {
             retval[i] = findNextStep(stepMeta, i);
         }
         return retval;
     }
 
     /**
      * Retrieve an array of succeeding step names for a certain originating step.
      *
      * @param stepMeta The originating step
      * @return an array of succeeding step names.
      */
     public String[] getNextStepNames(StepMeta stepMeta)
     {
         StepMeta nextStepMeta[] = getNextSteps(stepMeta);
         String retval[] = new String[nextStepMeta.length];
         for (int x = 0; x < nextStepMeta.length; x++)
             retval[x] = nextStepMeta[x].getName();
 
         return retval;
     }
 
     /**
      * Find the step that is located on a certain point on the canvas, taking into account the icon size.
      *
      * @param x the x-coordinate of the point queried
      * @param y the y-coordinate of the point queried
      * @return The step information if a step is located at the point. Otherwise, if no step was found: null.
      */
     public StepMeta getStep(int x, int y, int iconsize)
     {
         int i, s;
         s = steps.size();
         for (i = s - 1; i >= 0; i--) // Back to front because drawing goes from start to end
         {
             StepMeta stepMeta = (StepMeta) steps.get(i);
             if (partOfTransHop(stepMeta) || stepMeta.isDrawn()) // Only consider steps from active or inactive hops!
             {
                 Point p = stepMeta.getLocation();
                 if (p != null)
                 {
                     if (x >= p.x && x <= p.x + iconsize && y >= p.y && y <= p.y + iconsize + 20) { return stepMeta; }
                 }
             }
         }
         return null;
     }
 
     /**
      * Find the note that is located on a certain point on the canvas.
      *
      * @param x the x-coordinate of the point queried
      * @param y the y-coordinate of the point queried
      * @return The note information if a note is located at the point. Otherwise, if nothing was found: null.
      */
     public NotePadMeta getNote(int x, int y)
     {
         int i, s;
         s = notes.size();
         for (i = s - 1; i >= 0; i--) // Back to front because drawing goes from start to end
         {
             NotePadMeta ni = (NotePadMeta) notes.get(i);
             Point loc = ni.getLocation();
             Point p = new Point(loc.x, loc.y);
             if (x >= p.x && x <= p.x + ni.width + 2 * Const.NOTE_MARGIN && y >= p.y && y <= p.y + ni.height + 2 * Const.NOTE_MARGIN) { return ni; }
         }
         return null;
     }
 
     /**
      * Determines whether or not a certain step is part of a hop.
      *
      * @param stepMeta The step queried
      * @return true if the step is part of a hop.
      */
     public boolean partOfTransHop(StepMeta stepMeta)
     {
         int i;
         for (i = 0; i < nrTransHops(); i++)
         {
             TransHopMeta hi = getTransHop(i);
             if (hi.getFromStep() == null || hi.getToStep() == null) return false;
             if (hi.getFromStep().equals(stepMeta) || hi.getToStep().equals(stepMeta)) return true;
         }
         return false;
     }
 
     /**
      * Returns the fields that are emitted by a certain step name
      *
      * @param stepname The stepname of the step to be queried.
      * @return A row containing the fields emitted.
      */
     public Row getStepFields(String stepname) throws KettleStepException
     {
         StepMeta stepMeta = findStep(stepname);
         if (stepMeta != null)
             return getStepFields(stepMeta);
         else
             return null;
     }
 
     /**
      * Returns the fields that are emitted by a certain step
      *
      * @param stepMeta The step to be queried.
      * @return A row containing the fields emitted.
      */
     public Row getStepFields(StepMeta stepMeta) throws KettleStepException
     {
         return getStepFields(stepMeta, null);
     }
 
     public Row getStepFields(StepMeta[] stepMeta) throws KettleStepException
     {
         Row fields = new Row();
 
         for (int i = 0; i < stepMeta.length; i++)
         {
             Row flds = getStepFields(stepMeta[i]);
             if (flds != null) fields.mergeRow(flds);
         }
         return fields;
     }
 
     /**
      * Returns the fields that are emitted by a certain step
      *
      * @param stepMeta The step to be queried.
      * @param monitor The progress monitor for progress dialog. (null if not used!)
      * @return A row containing the fields emitted.
      */
     public Row getStepFields(StepMeta stepMeta, IProgressMonitor monitor) throws KettleStepException
     {
         Row row = new Row();
 
         if (stepMeta == null) return row;
 
         log.logDebug(toString(), Messages.getString("TransMeta.Log.FromStepALookingAtPreviousStep", stepMeta.getName(), String.valueOf(findNrPrevSteps(stepMeta)) )); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         for (int i = 0; i < findNrPrevSteps(stepMeta); i++)
         {
             StepMeta prevStepMeta = findPrevStep(stepMeta, i);
 
             if (monitor != null)
             {
                 monitor.subTask(Messages.getString("TransMeta.Monitor.CheckingStepTask.Title", prevStepMeta.getName() )); //$NON-NLS-1$ //$NON-NLS-2$
             }
 
             Row add = getStepFields(prevStepMeta, monitor);
             if (add == null) add = new Row();
             log.logDebug(toString(), Messages.getString("TransMeta.Log.FoundFieldsToAdd") + add.toString()); //$NON-NLS-1$
             if (i == 0)
             {
                 row.addRow(add);
             }
             else
             {
                 // See if the add fields are not already in the row
                 for (int x = 0; x < add.size(); x++)
                 {
                     Value v = add.getValue(x);
                     Value s = row.searchValue(v.getName());
                     if (s == null)
                     {
                         row.addValue(v);
                     }
                 }
             }
         }
         // Finally, see if we need to add/modify/delete fields with this step "name"
         return getThisStepFields(stepMeta, row, monitor);
     }
 
     /**
      * Find the fields that are entering a step with a certain name.
      *
      * @param stepname The name of the step queried
      * @return A row containing the fields (w/ origin) entering the step
      */
     public Row getPrevStepFields(String stepname) throws KettleStepException
     {
         return getPrevStepFields(findStep(stepname));
     }
 
     /**
      * Find the fields that are entering a certain step.
      *
      * @param stepMeta The step queried
      * @return A row containing the fields (w/ origin) entering the step
      */
     public Row getPrevStepFields(StepMeta stepMeta) throws KettleStepException
     {
         return getPrevStepFields(stepMeta, null);
     }
 
     /**
      * Find the fields that are entering a certain step.
      *
      * @param stepMeta The step queried
      * @param monitor The progress monitor for progress dialog. (null if not used!)
      * @return A row containing the fields (w/ origin) entering the step
      */
     public Row getPrevStepFields(StepMeta stepMeta, IProgressMonitor monitor) throws KettleStepException
     {
         Row row = new Row();
 
         if (stepMeta == null) { return null; }
 
         log.logDebug(toString(), Messages.getString("TransMeta.Log.FromStepALookingAtPreviousStep", stepMeta.getName(), String.valueOf(findNrPrevSteps(stepMeta)) )); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         for (int i = 0; i < findNrPrevSteps(stepMeta); i++)
         {
             StepMeta prevStepMeta = findPrevStep(stepMeta, i);
 
             if (monitor != null)
             {
                 monitor.subTask(Messages.getString("TransMeta.Monitor.CheckingStepTask.Title", prevStepMeta.getName() )); //$NON-NLS-1$ //$NON-NLS-2$
             }
 
             Row add = getStepFields(prevStepMeta, monitor);
             log.logDebug(toString(), Messages.getString("TransMeta.Log.FoundFieldsToAdd2") + add.toString()); //$NON-NLS-1$
             if (i == 0) // we expect all input streams to be of the same layout!
             {
                 row.addRow(add); // recursive!
             }
             else
             {
                 // See if the add fields are not already in the row
                 for (int x = 0; x < add.size(); x++)
                 {
                     Value v = add.getValue(x);
                     Value s = row.searchValue(v.getName());
                     if (s == null)
                     {
                         row.addValue(v);
                     }
                 }
             }
         }
         return row;
     }
 
     /**
      * Return the fields that are emitted by a step with a certain name
      *
      * @param stepname The name of the step that's being queried.
      * @param row A row containing the input fields or an empty row if no input is required.
      * @return A Row containing the output fields.
      */
     public Row getThisStepFields(String stepname, Row row) throws KettleStepException
     {
         return getThisStepFields(findStep(stepname), row);
     }
 
     /**
      * Returns the fields that are emitted by a step
      *
      * @param stepMeta : The StepMeta object that's being queried
      * @param row : A row containing the input fields or an empty row if no input is required.
      *
      * @return A Row containing the output fields.
      */
     public Row getThisStepFields(StepMeta stepMeta, Row row) throws KettleStepException
     {
         return getThisStepFields(stepMeta, row, null);
     }
 
     /**
      * Returns the fields that are emitted by a step
      *
      * @param stepMeta : The StepMeta object that's being queried
      * @param row : A row containing the input fields or an empty row if no input is required.
      *
      * @return A Row containing the output fields.
      */
     public Row getThisStepFields(StepMeta stepMeta, Row row, IProgressMonitor monitor) throws KettleStepException
     {
         // Then this one.
         log.logDebug(toString(), Messages.getString("TransMeta.Log.GettingFieldsFromStep",stepMeta.getName(), stepMeta.getStepID())); //$NON-NLS-1$ //$NON-NLS-2$
         String name = stepMeta.getName();
 
         if (monitor != null)
         {
             monitor.subTask(Messages.getString("TransMeta.Monitor.GettingFieldsFromStepTask.Title", name )); //$NON-NLS-1$ //$NON-NLS-2$
         }
 
         StepMetaInterface stepint = stepMeta.getStepMetaInterface();
         Row inform = null;
         StepMeta[] lu = getInfoStep(stepMeta);
         if (lu != null)
         {
             inform = getStepFields(lu);
         }
         else
         {
             inform = stepint.getTableFields();
         }
 
         stepint.getFields(row, name, inform);
 
         return row;
     }
 
     /**
      * Determine if we should put a replace warning or not for the transformation in a certain repository.
      *
      * @param rep The repository.
      * @return True if we should show a replace warning, false if not.
      */
     public boolean showReplaceWarning(Repository rep)
     {
         if (getID() < 0)
         {
             try
             {
                 if (rep.getTransformationID(getName(), directory.getID()) > 0) return true;
             }
             catch (KettleDatabaseException dbe)
             {
                 log.logError(toString(), Messages.getString("TransMeta.Log.DatabaseError") + dbe.getMessage()); //$NON-NLS-1$
                 return true;
             }
         }
         return false;
     }
 
     /**
      * Saves the transformation to a repository.
      *
      * @param rep The repository.
      * @throws KettleException if an error occurrs.
      */
     public void saveRep(Repository rep) throws KettleException
     {
         saveRep(rep, null);
     }
 
     /**
      * Saves the transformation to a repository.
      *
      * @param rep The repository.
      * @throws KettleException if an error occurrs.
      */
     public void saveRep(Repository rep, IProgressMonitor monitor) throws KettleException
     {
         try
         {
         	if (monitor!=null) monitor.subTask(Messages.getString("TransMeta.Monitor.LockingRepository")); //$NON-NLS-1$
 
             rep.lockRepository(); // make sure we're they only one using the repository at the moment
 
             // Clear attribute id cache
             rep.clearNextIDCounters(); // force repository lookup.
 
             // Do we have a valid directory?
             if (directory.getID() < 0) { throw new KettleException(Messages.getString("TransMeta.Exception.PlsSelectAValidDirectoryBeforeSavingTheTransformation")); } //$NON-NLS-1$
 
             int nrWorks = 2 + nrDatabases() + nrNotes() + nrSteps() + nrTransHops();
             if (monitor != null) monitor.beginTask(Messages.getString("TransMeta.Monitor.SavingTransformationTask.Title") + getPathAndName(), nrWorks); //$NON-NLS-1$
             log.logDebug(toString(), Messages.getString("TransMeta.Log.SavingOfTransformationStarted")); //$NON-NLS-1$
 
             if (monitor!=null && monitor.isCanceled()) throw new KettleDatabaseException();
 
             // Before we start, make sure we have a valid transformation ID!
             // Two possibilities:
             // 1) We have a ID: keep it
             // 2) We don't have an ID: look it up.
             // If we find a transformation with the same name: ask!
             //
             if (monitor != null) monitor.subTask(Messages.getString("TransMeta.Monitor.HandlingOldVersionTransformationTask.Title")); //$NON-NLS-1$
             setID(rep.getTransformationID(getName(), directory.getID()));
 
             // If no valid id is available in the database, assign one...
             if (getID() <= 0)
             {
                 setID(rep.getNextTransformationID());
             }
             else
             {
                 // If we have a valid ID, we need to make sure everything is cleared out
                 // of the database for this id_transformation, before we put it back in...
                 if (monitor != null) monitor.subTask(Messages.getString("TransMeta.Monitor.DeletingOldVersionTransformationTask.Title")); //$NON-NLS-1$
                 log.logDebug(toString(), Messages.getString("TransMeta.Log.DeletingOldVersionTransformation")); //$NON-NLS-1$
                 rep.delAllFromTrans(getID());
                 log.logDebug(toString(), Messages.getString("TransMeta.Log.OldVersionOfTransformationRemoved")); //$NON-NLS-1$
             }
             if (monitor != null) monitor.worked(1);
 
             log.logDebug(toString(), Messages.getString("TransMeta.Log.SavingNotes")); //$NON-NLS-1$
             for (int i = 0; i < nrNotes(); i++)
             {
                 if (monitor!=null && monitor.isCanceled()) throw new KettleDatabaseException(Messages.getString("TransMeta.Log.UserCancelledTransSave"));
 
                 if (monitor != null) monitor.subTask(Messages.getString("TransMeta.Monitor.SavingNoteTask.Title") + (i + 1) + "/" + nrNotes()); //$NON-NLS-1$ //$NON-NLS-2$
                 NotePadMeta ni = getNote(i);
                 ni.saveRep(rep, getID());
                 if (ni.getID() > 0) rep.insertTransNote(getID(), ni.getID());
                 if (monitor != null) monitor.worked(1);
             }
 
             log.logDebug(toString(), Messages.getString("TransMeta.Log.SavingDatabaseConnections")); //$NON-NLS-1$
             for (int i = 0; i < nrDatabases(); i++)
             {
                 if (monitor!=null && monitor.isCanceled()) throw new KettleDatabaseException(Messages.getString("TransMeta.Log.UserCancelledTransSave"));
 
                 if (monitor != null) monitor.subTask(Messages.getString("TransMeta.Monitor.SavingDatabaseTask.Title") + (i + 1) + "/" + nrDatabases()); //$NON-NLS-1$ //$NON-NLS-2$
                 DatabaseMeta ci = getDatabase(i);
                 // ONLY save the database connection if it has changed and nothing was saved in the repository
                 if(ci.hasChanged() || ci.getID()<=0)
                 {
                     ci.saveRep(rep);
                 }
                 if (monitor != null) monitor.worked(1);
             }
 
             // Before saving the steps, make sure we have all the step-types.
             // It is possible that we received another step through a plugin.
             log.logDebug(toString(), Messages.getString("TransMeta.Log.CheckingStepTypes")); //$NON-NLS-1$
             rep.updateStepTypes();
 
             log.logDebug(toString(), Messages.getString("TransMeta.Log.SavingSteps")); //$NON-NLS-1$
             for (int i = 0; i < nrSteps(); i++)
             {
                 if (monitor!=null && monitor.isCanceled()) throw new KettleDatabaseException(Messages.getString("TransMeta.Log.UserCancelledTransSave"));
 
                 if (monitor != null) monitor.subTask(Messages.getString("TransMeta.Monitor.SavingStepTask.Title") + (i + 1) + "/" + nrSteps()); //$NON-NLS-1$ //$NON-NLS-2$
                 StepMeta stepMeta = getStep(i);
                 stepMeta.saveRep(rep, getID());
 
                 if (monitor != null) monitor.worked(1);
             }
             rep.closeStepAttributeInsertPreparedStatement();
 
             log.logDebug(toString(), Messages.getString("TransMeta.Log.SavingHops")); //$NON-NLS-1$
             for (int i = 0; i < nrTransHops(); i++)
             {
                 if (monitor!=null && monitor.isCanceled()) throw new KettleDatabaseException(Messages.getString("TransMeta.Log.UserCancelledTransSave"));
 
                 if (monitor != null) monitor.subTask(Messages.getString("TransMeta.Monitor.SavingHopTask.Title") + (i + 1) + "/" + nrTransHops()); //$NON-NLS-1$ //$NON-NLS-2$
                 TransHopMeta hi = getTransHop(i);
                 hi.saveRep(rep, getID());
                 if (monitor != null) monitor.worked(1);
             }
 
             if (monitor != null) monitor.subTask(Messages.getString("TransMeta.Monitor.FinishingTask.Title")); //$NON-NLS-1$
             log.logDebug(toString(), Messages.getString("TransMeta.Log.SavingTransformationInfo")); //$NON-NLS-1$
             
             rep.insertTransformation(this); // save the top level information for the transformation
             rep.closeTransAttributeInsertPreparedStatement();
 
             // Save the partition schemas
             for (int i=0;i<partitionSchemas.size();i++)
             {
                 PartitionSchema schema = (PartitionSchema) partitionSchemas.get(i);
                 schema.saveRep(rep, getID());
             }
 
             // Save the slaves
             for (int i=0;i<slaveServers.size();i++)
             {
                 SlaveServer slaveServer = (SlaveServer) slaveServers.get(i);
                 slaveServer.saveRep(rep, getID());
             }
             
             // Save the clustering schemas
             for (int i=0;i<clusterSchemas.size();i++)
             {
                 ClusterSchema schema = (ClusterSchema) clusterSchemas.get(i);
                 schema.saveRep(rep, getID());
             }
 
             
             log.logDebug(toString(), Messages.getString("TransMeta.Log.SavingDependencies")); //$NON-NLS-1$
             for (int i = 0; i < nrDependencies(); i++)
             {
                 if (monitor!=null && monitor.isCanceled()) throw new KettleDatabaseException(Messages.getString("TransMeta.Log.UserCancelledTransSave"));
 
                 TransDependency td = getDependency(i);
                 td.saveRep(rep, getID());
             }
 
             log.logDebug(toString(), Messages.getString("TransMeta.Log.SavingFinished")); //$NON-NLS-1$
 
         	if (monitor!=null) monitor.subTask(Messages.getString("TransMeta.Monitor.UnlockingRepository")); //$NON-NLS-1$
             rep.unlockRepository();
 
             // Perform a commit!
             rep.commit();
 
             clearChanged();
             if (monitor != null) monitor.worked(1);
             if (monitor != null) monitor.done();
         }
         catch (KettleDatabaseException dbe)
         {
             // Oops, rollback!
             rep.rollback();
 
             log.logError(toString(), Messages.getString("TransMeta.Log.ErrorSavingTransformationToRepository") + Const.CR + dbe.getMessage()); //$NON-NLS-1$
             throw new KettleException(Messages.getString("TransMeta.Log.ErrorSavingTransformationToRepository"), dbe); //$NON-NLS-1$
         }
         finally
         {
             // don't forget to unlock the repository.
             // Normally this is done by the commit / rollback statement, but hey there are some freaky database out
             // there...
             rep.unlockRepository();
         }
     }
 
     /**
      * Read the database connections in the repository and add them to this transformation if they are not yet present.
      *
      * @param rep The repository to load the database connections from.
      * @param overWriteShared if an object with the same name exists, overwrite
      */
     public void readDatabases(Repository rep, boolean overWriteShared) throws KettleException
     {
         try
         {
             long dbids[] = rep.getDatabaseIDs();
             for (int i = 0; i < dbids.length; i++)
             {
                 DatabaseMeta databaseMeta = new DatabaseMeta(rep, dbids[i]);
                 DatabaseMeta check = findDatabase(databaseMeta.getName()); // Check if there already is one in the transformation
                 if (check==null || overWriteShared) // We only add, never overwrite database connections. 
                 {
                     if (databaseMeta.getName() != null)
                     {
                         addOrReplaceDatabase(databaseMeta);
                         if (!overWriteShared) databaseMeta.setChanged(false);
                     }
                 }
             }
             changed_databases = false;
         }
         catch (KettleDatabaseException dbe)
         {
             throw new KettleException(Messages.getString("TransMeta.Log.UnableToReadDatabaseIDSFromRepository"), dbe); //$NON-NLS-1$
         }
         catch (KettleException ke)
         {
             throw new KettleException(Messages.getString("TransMeta.Log.UnableToReadDatabasesFromRepository"), ke); //$NON-NLS-1$
         }
     }
 
     /**
      * Read the database partitions in the repository and add them to this transformation if they are not yet present.
      * @param rep The repository to load from.
      * @param overWriteShared if an object with the same name exists, overwrite
      * @throws KettleException 
      */
     public void readPartitionSchemas(Repository rep, boolean overWriteShared) throws KettleException
     {
         try
         {
             long dbids[] = rep.getPartitionSchemaIDs();
             for (int i = 0; i < dbids.length; i++)
             {
                 PartitionSchema partitionSchema = new PartitionSchema(rep, dbids[i]);
                 PartitionSchema check = findPartitionSchema(partitionSchema.getName()); // Check if there already is one in the transformation
                 if (check==null || overWriteShared) 
                 {
                     if (!Const.isEmpty(partitionSchema.getName()))
                     {
                         addOrReplacePartitionSchema(partitionSchema);
                         if (!overWriteShared) partitionSchema.setChanged(false);
                     }
                 }
             }
         }
         catch (KettleException dbe)
         {
             throw new KettleException(Messages.getString("TransMeta.Log.UnableToReadPartitionSchemaFromRepository"), dbe); //$NON-NLS-1$
         }
     }
 
     /**
      * Read the slave servers in the repository and add them to this transformation if they are not yet present.
      * @param rep The repository to load from.
      * @param overWriteShared if an object with the same name exists, overwrite
      * @throws KettleException 
      */
     public void readSlaves(Repository rep, boolean overWriteShared) throws KettleException
     {
         try
         {
             long dbids[] = rep.getSlaveIDs();
             for (int i = 0; i < dbids.length; i++)
             {
                 SlaveServer slaveServer = new SlaveServer(rep, dbids[i]);
                 SlaveServer check = findSlaveServer(slaveServer.getName()); // Check if there already is one in the transformation
                 if (check==null || overWriteShared) 
                 {
                     if (!Const.isEmpty(slaveServer.getName()))
                     {
                         addOrReplaceSlaveServer(slaveServer);
                         if (!overWriteShared) slaveServer.setChanged(false);
                     }
                 }
             }
         }
         catch (KettleDatabaseException dbe)
         {
             throw new KettleException(Messages.getString("TransMeta.Log.UnableToReadSlaveServersFromRepository"), dbe); //$NON-NLS-1$
         }
     }
     
     /**
      * Read the clusters in the repository and add them to this transformation if they are not yet present.
      * @param rep The repository to load from.
      * @param overWriteShared if an object with the same name exists, overwrite
      * @throws KettleException 
      */
     public void readClusters(Repository rep, boolean overWriteShared) throws KettleException
     {
         try
         {
             long dbids[] = rep.getClusterIDs();
             for (int i = 0; i < dbids.length; i++)
             {
                 ClusterSchema cluster = new ClusterSchema(rep, dbids[i]);
                 ClusterSchema check = findClusterSchema(cluster.getName()); // Check if there already is one in the transformation
                 if (check==null || overWriteShared) 
                 {
                     if (!Const.isEmpty(cluster.getName()))
                     {
                         addOrReplaceClusterSchema(cluster);
                         if (!overWriteShared) cluster.setChanged(false);
                     }
                 }
             }
         }
         catch (KettleDatabaseException dbe)
         {
             throw new KettleException(Messages.getString("TransMeta.Log.UnableToReadClustersFromRepository"), dbe); //$NON-NLS-1$
         }
     }
         
     /**
      * Load the transformation name & other details from a repository.
      *
      * @param rep The repository to load the details from.
      */
     public void loadRepTrans(Repository rep) throws KettleException
     {
         try
         {
             Row r = rep.getTransformation(getID());
 
             if (r != null)
             {
                 name = r.searchValue("NAME").getString(); //$NON-NLS-1$
                 readStep = findStep(steps, r.getInteger("ID_STEP_READ", -1L)); //$NON-NLS-1$
                 writeStep = findStep(steps, r.getInteger("ID_STEP_WRITE", -1L)); //$NON-NLS-1$
                 inputStep = findStep(steps, r.getInteger("ID_STEP_INPUT", -1L)); //$NON-NLS-1$
                 outputStep = findStep(steps, r.getInteger("ID_STEP_OUTPUT", -1L)); //$NON-NLS-1$
                 updateStep = findStep(steps, r.getInteger("ID_STEP_UPDATE", -1L)); //$NON-NLS-1$
 
                 logConnection = Const.findDatabase(databases, r.getInteger("ID_DATABASE_LOG", -1L)); //$NON-NLS-1$
                 logTable = r.getString("TABLE_NAME_LOG", null); //$NON-NLS-1$
                 useBatchId = r.getBoolean("USE_BATCHID", false); //$NON-NLS-1$
                 logfieldUsed = r.getBoolean("USE_LOGFIELD", false); //$NON-NLS-1$
 
                 maxDateConnection = Const.findDatabase(databases, r.getInteger("ID_DATABASE_MAXDATE", -1L)); //$NON-NLS-1$
                 maxDateTable = r.getString("TABLE_NAME_MAXDATE", null); //$NON-NLS-1$
                 maxDateField = r.getString("FIELD_NAME_MAXDATE", null); //$NON-NLS-1$
                 maxDateOffset = r.getNumber("OFFSET_MAXDATE", 0.0); //$NON-NLS-1$
                 maxDateDifference = r.getNumber("DIFF_MAXDATE", 0.0); //$NON-NLS-1$
 
                 modifiedUser = r.getString("MODIFIED_USER", null); //$NON-NLS-1$
                 modifiedDate = r.searchValue("MODIFIED_DATE"); //$NON-NLS-1$
 
                 // Optional:
                 sizeRowset = Const.ROWS_IN_ROWSET;
                 Value val_size_rowset = r.searchValue("SIZE_ROWSET"); //$NON-NLS-1$
                 if (val_size_rowset != null && !val_size_rowset.isNull())
                 {
                     sizeRowset = (int) val_size_rowset.getInteger();
                 }
 
                 long id_directory = r.getInteger("ID_DIRECTORY", -1L); //$NON-NLS-1$
                 if (id_directory >= 0)
                 {
                     log.logDetailed(toString(), "ID_DIRECTORY=" + id_directory); //$NON-NLS-1$
                     // Set right directory...
                     directory = directoryTree.findDirectory(id_directory);
                 }
                 
                 usingUniqueConnections = rep.getTransAttributeBoolean(getID(), 0, "UNIQUE_CONNECTIONS");
                 feedbackShown = !"N".equalsIgnoreCase( rep.getTransAttributeString(getID(), 0, "FEEDBACK_SHOWN") );
                 feedbackSize = (int) rep.getTransAttributeInteger(getID(), 0, "FEEDBACK_SIZE");
                 usingThreadPriorityManagment = !"N".equalsIgnoreCase( rep.getTransAttributeString(getID(), 0, "USING_THREAD_PRIORITIES") );
             }
         }
         catch (KettleDatabaseException dbe)
         {
             throw new KettleException(Messages.getString("TransMeta.Exception.UnableToLoadTransformationInfoFromRepository"), dbe); //$NON-NLS-1$
         }
         finally
         {
             setInternalKettleVariables();
         }
     }
 
     /**
      * Read a transformation with a certain name from a repository
      *
      * @param rep The repository to read from.
      * @param transname The name of the transformation.
      * @param repdir the path to the repository directory
      */
     public TransMeta(Repository rep, String transname, RepositoryDirectory repdir) throws KettleException
     {
         this(rep, transname, repdir, null, true);
     }
 
     /**
      * Read a transformation with a certain name from a repository
      *
      * @param rep The repository to read from.
      * @param transname The name of the transformation.
      * @param repdir the path to the repository directory
      * @param setInternalVariables true if you want to set the internal variables based on this transformation information
      */
     public TransMeta(Repository rep, String transname, RepositoryDirectory repdir, boolean setInternalVariables) throws KettleException
     {
         this(rep, transname, repdir, null, setInternalVariables);
     }
 
     /**
      * Read a transformation with a certain name from a repository
      *
      * @param rep The repository to read from.
      * @param transname The name of the transformation.
      * @param repdir the path to the repository directory
      * @param monitor The progress monitor to display the progress of the file-open operation in a dialog
      */
     public TransMeta(Repository rep, String transname, RepositoryDirectory repdir, IProgressMonitor monitor) throws KettleException
     {
         this(rep, transname, repdir, monitor, true);
     }
 
     /**
      * Read a transformation with a certain name from a repository
      *
      * @param rep The repository to read from.
      * @param transname The name of the transformation.
      * @param repdir the path to the repository directory
      * @param monitor The progress monitor to display the progress of the file-open operation in a dialog
      * @param setInternalVariables true if you want to set the internal variables based on this transformation information
      */
     public TransMeta(Repository rep, String transname, RepositoryDirectory repdir, IProgressMonitor monitor, boolean setInternalVariables) throws KettleException
     {
         try
         {
             String pathAndName = repdir.isRoot() ? repdir + transname : repdir + RepositoryDirectory.DIRECTORY_SEPARATOR + transname;
 
             // Clear everything...
             clear();
             
             // Also read objects from the shared XML file
             readSharedObjects(rep);
             
             setName(transname);
             directory = repdir;
             directoryTree = directory.findRoot();
 
             // Get the transformation id
             log.logDetailed(toString(), Messages.getString("TransMeta.Log.LookingForTransformation", transname ,directory.getPath() )); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 
             if (monitor != null) monitor.subTask(Messages.getString("TransMeta.Monitor.ReadingTransformationInfoTask.Title")); //$NON-NLS-1$
             setID(rep.getTransformationID(transname, directory.getID()));
             if (monitor != null) monitor.worked(1);
 
             // If no valid id is available in the database, then give error...
             if (getID() > 0)
             {
                 long noteids[] = rep.getTransNoteIDs(getID());
                 long stepids[] = rep.getStepIDs(getID());
                 long hopids[] = rep.getTransHopIDs(getID());
 
                 int nrWork = 3 + noteids.length + stepids.length + hopids.length;
 
                 if (monitor != null) monitor.beginTask(Messages.getString("TransMeta.Monitor.LoadingTransformationTask.Title") + pathAndName, nrWork); //$NON-NLS-1$
 
                 log.logDetailed(toString(), Messages.getString("TransMeta.Log.LoadingTransformation", getName() )); //$NON-NLS-1$ //$NON-NLS-2$
 
                 // Load the common database connections
 
                 if (monitor != null) monitor.subTask(Messages.getString("TransMeta.Monitor.ReadingTheAvailableSharedObjectsTask.Title")); //$NON-NLS-1$
                 readSharedObjects(rep);
                 if (monitor != null) monitor.worked(1);
 
                 // Load the notes...
                 if (monitor != null) monitor.subTask(Messages.getString("TransMeta.Monitor.ReadingNoteTask.Title")); //$NON-NLS-1$
                 for (int i = 0; i < noteids.length; i++)
                 {
                     NotePadMeta ni = new NotePadMeta(log, rep, noteids[i]);
                     if (indexOfNote(ni) < 0) addNote(ni);
                     if (monitor != null) monitor.worked(1);
                 }
 
                 if (monitor != null) monitor.subTask(Messages.getString("TransMeta.Monitor.ReadingStepsTask.Title")); //$NON-NLS-1$
                 rep.fillStepAttributesBuffer(getID()); // read all the attributes on one go!
                 for (int i = 0; i < stepids.length; i++)
                 {
                     log.logDetailed(toString(), Messages.getString("TransMeta.Log.LoadingStepWithID") + stepids[i]); //$NON-NLS-1$
                     if (monitor != null) monitor.subTask(Messages.getString("TransMeta.Monitor.ReadingStepTask.Title") + (i + 1) + "/" + (stepids.length)); //$NON-NLS-1$ //$NON-NLS-2$
                     StepMeta stepMeta = new StepMeta(rep, stepids[i], databases, counters, partitionSchemas);
                     // In this case, we just add or replace the shared steps.
                     // The repository is considered "more central"
                     addOrReplaceStep(stepMeta);
                     
                     if (monitor != null) monitor.worked(1);
                 }
                 if (monitor != null) monitor.worked(1);
                 rep.setStepAttributesBuffer(null); // clear the buffer (should be empty anyway)
 
                 // Have all StreamValueLookups, etc. reference the correct source steps...
                 for (int i = 0; i < nrSteps(); i++)
                 {
                     StepMetaInterface sii = getStep(i).getStepMetaInterface();
                     sii.searchInfoAndTargetSteps(steps);
                 }
 
                 if (monitor != null) monitor.subTask(Messages.getString("TransMeta.Monitor.ReadingHopTask.Title")); //$NON-NLS-1$
                 for (int i = 0; i < hopids.length; i++)
                 {
                     TransHopMeta hi = new TransHopMeta(rep, hopids[i], steps);
                     addTransHop(hi);
                     if (monitor != null) monitor.worked(1);
                 }
                 
                 // Have all step partitioning meta-data reference the correct schemas that we just loaded
                 // 
                 for (int i = 0; i < nrSteps(); i++)
                 {
                     StepPartitioningMeta stepPartitioningMeta = getStep(i).getStepPartitioningMeta();
                     if (stepPartitioningMeta!=null)
                     {
                         stepPartitioningMeta.setPartitionSchemaAfterLoading(partitionSchemas);
                     }
                 }
                 
                 // Have all step clustering schema meta-data reference the correct cluster schemas that we just loaded
                 // 
                 for (int i = 0; i < nrSteps(); i++)
                 {
                     getStep(i).setClusterSchemaAfterLoading(clusterSchemas);
                 }
 
 
                 if (monitor != null) monitor.subTask(Messages.getString("TransMeta.Monitor.LoadingTransformationDetailsTask.Title")); //$NON-NLS-1$
                 loadRepTrans(rep);
                 if (monitor != null) monitor.worked(1);
                 
                 
 
                 // Have all partitioned step reference the correct partitioning schema
                 for (int i = 0; i < nrSteps(); i++)
                 {
                     getStep(i).getStepPartitioningMeta().setPartitionSchemaAfterLoading(partitionSchemas);
                 }
                 
                 if (monitor != null) monitor.subTask(Messages.getString("TransMeta.Monitor.ReadingTheDependenciesTask.Title")); //$NON-NLS-1$
                 long depids[] = rep.getTransDependencyIDs(getID());
                 for (int i = 0; i < depids.length; i++)
                 {
                     TransDependency td = new TransDependency(rep, depids[i], databases);
                     addDependency(td);
                 }
                 if (monitor != null) monitor.worked(1);
 
                 if (monitor != null) monitor.subTask(Messages.getString("TransMeta.Monitor.SortingStepsTask.Title")); //$NON-NLS-1$
                 sortSteps();
                 if (monitor != null) monitor.worked(1);
                 if (monitor != null) monitor.done();
             }
             else
             {
                 throw new KettleException(Messages.getString("TransMeta.Exception.TransformationDoesNotExist") + name); //$NON-NLS-1$
             }
 
             log.logDetailed(toString(), Messages.getString("TransMeta.Log.LoadedTransformation2", transname , String.valueOf(directory == null))); //$NON-NLS-1$ //$NON-NLS-2$
 
             log.logDetailed(toString(), Messages.getString("TransMeta.Log.LoadedTransformation", transname , directory.getPath() )); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 
         }
         catch (KettleDatabaseException e)
         {
             log.logError(toString(), Messages.getString("TransMeta.Log.DatabaseErrorOccuredReadingTransformation") + Const.CR + e); //$NON-NLS-1$
             throw new KettleException(Messages.getString("TransMeta.Exception.DatabaseErrorOccuredReadingTransformation"), e); //$NON-NLS-1$
         }
         catch (Exception e)
         {
             log.logError(toString(), Messages.getString("TransMeta.Log.DatabaseErrorOccuredReadingTransformation") + Const.CR + e); //$NON-NLS-1$
             throw new KettleException(Messages.getString("TransMeta.Exception.DatabaseErrorOccuredReadingTransformation2"), e); //$NON-NLS-1$
         }
         finally
         {
             if (setInternalVariables) setInternalKettleVariables();
         }
     }
 
     /**
      * Find the location of hop
      *
      * @param hi The hop queried
      * @return The location of the hop, -1 if nothing was found.
      */
     public int indexOfTransHop(TransHopMeta hi)
     {
         return hops.indexOf(hi);
     }
 
     /**
      * Find the location of step
      *
      * @param stepMeta The step queried
      * @return The location of the step, -1 if nothing was found.
      */
     public int indexOfStep(StepMeta stepMeta)
     {
         return steps.indexOf(stepMeta);
     }
 
     /**
      * Find the location of database
      *
      * @param ci The database queried
      * @return The location of the database, -1 if nothing was found.
      */
     public int indexOfDatabase(DatabaseMeta ci)
     {
         return databases.indexOf(ci);
     }
 
     /**
      * Find the location of a note
      *
      * @param ni The note queried
      * @return The location of the note, -1 if nothing was found.
      */
     public int indexOfNote(NotePadMeta ni)
     {
         return notes.indexOf(ni);
     }
 
     public String getXML()
     {
         Props props = null;
         if (Props.isInitialized()) props=Props.getInstance();
 
         StringBuffer retval = new StringBuffer();
 
         retval.append("<"+XML_TAG+">" + Const.CR); //$NON-NLS-1$
 
         retval.append("  <info>" + Const.CR); //$NON-NLS-1$
 
         retval.append("    " + XMLHandler.addTagValue("name", name)); //$NON-NLS-1$ //$NON-NLS-2$
         retval.append("    " + XMLHandler.addTagValue("directory", directory != null ? directory.getPath() : RepositoryDirectory.DIRECTORY_SEPARATOR)); //$NON-NLS-1$ //$NON-NLS-2$
         retval.append("    <log>" + Const.CR); //$NON-NLS-1$
         retval.append("      " + XMLHandler.addTagValue("read", readStep == null ? "" : readStep.getName())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         retval.append("      " + XMLHandler.addTagValue("write", writeStep == null ? "" : writeStep.getName())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         retval.append("      " + XMLHandler.addTagValue("input", inputStep == null ? "" : inputStep.getName())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         retval.append("      " + XMLHandler.addTagValue("output", outputStep == null ? "" : outputStep.getName())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         retval.append("      " + XMLHandler.addTagValue("update", updateStep == null ? "" : updateStep.getName())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         retval.append("      " + XMLHandler.addTagValue("connection", logConnection == null ? "" : logConnection.getName())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         retval.append("      " + XMLHandler.addTagValue("table", logTable)); //$NON-NLS-1$ //$NON-NLS-2$
         retval.append("      " + XMLHandler.addTagValue("use_batchid", useBatchId)); //$NON-NLS-1$ //$NON-NLS-2$
         retval.append("      " + XMLHandler.addTagValue("use_logfield", logfieldUsed)); //$NON-NLS-1$ //$NON-NLS-2$
         retval.append("      </log>" + Const.CR); //$NON-NLS-1$
         retval.append("    <maxdate>" + Const.CR); //$NON-NLS-1$
         retval.append("      " + XMLHandler.addTagValue("connection", maxDateConnection == null ? "" : maxDateConnection.getName())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         retval.append("      " + XMLHandler.addTagValue("table", maxDateTable)); //$NON-NLS-1$ //$NON-NLS-2$
         retval.append("      " + XMLHandler.addTagValue("field", maxDateField)); //$NON-NLS-1$ //$NON-NLS-2$
         retval.append("      " + XMLHandler.addTagValue("offset", maxDateOffset)); //$NON-NLS-1$ //$NON-NLS-2$
         retval.append("      " + XMLHandler.addTagValue("maxdiff", maxDateDifference)); //$NON-NLS-1$ //$NON-NLS-2$
         retval.append("      </maxdate>" + Const.CR); //$NON-NLS-1$
         
         retval.append("    " + XMLHandler.addTagValue("size_rowset", sizeRowset)); //$NON-NLS-1$ //$NON-NLS-2$
         
         retval.append("    " + XMLHandler.addTagValue("sleep_time_empty", sleepTimeEmpty)); //$NON-NLS-1$ //$NON-NLS-2$
         retval.append("    " + XMLHandler.addTagValue("sleep_time_full", sleepTimeFull)); //$NON-NLS-1$ //$NON-NLS-2$
         
         retval.append("    " + XMLHandler.addTagValue("unique_connections", usingUniqueConnections)); //$NON-NLS-1$ //$NON-NLS-2$
         
         retval.append("    " + XMLHandler.addTagValue("feedback_shown", feedbackShown)); //$NON-NLS-1$ //$NON-NLS-2$
         retval.append("    " + XMLHandler.addTagValue("feedback_size", feedbackSize)); //$NON-NLS-1$ //$NON-NLS-2$
         retval.append("    " + XMLHandler.addTagValue("using_thread_priorities", usingThreadPriorityManagment)); // $NON-NLS-1$
         
         retval.append("    <dependencies>" + Const.CR); //$NON-NLS-1$
         for (int i = 0; i < nrDependencies(); i++)
         {
             TransDependency td = getDependency(i);
             retval.append(td.getXML());
         }
         retval.append("      </dependencies>" + Const.CR); //$NON-NLS-1$
 
         // The database partitioning schemas...
         //
         retval.append("    <partitionschemas>" + Const.CR); //$NON-NLS-1$
         for (int i = 0; i < partitionSchemas.size(); i++)
         {
             PartitionSchema partitionSchema = (PartitionSchema) partitionSchemas.get(i);
             retval.append(partitionSchema.getXML());
         }
         retval.append("      </partitionschemas>" + Const.CR); //$NON-NLS-1$
 
         // The slave servers...
         //
         retval.append("    <slaveservers>" + Const.CR); //$NON-NLS-1$
         for (int i = 0; i < slaveServers.size(); i++)
         {
             SlaveServer slaveServer = (SlaveServer) slaveServers.get(i);
             retval.append("         ").append(slaveServer.getXML()).append(Const.CR);
         }
         retval.append("      </slaveservers>" + Const.CR); //$NON-NLS-1$
 
         // The cluster schemas...
         //
         retval.append("    <clusterschemas>" + Const.CR); //$NON-NLS-1$
         for (int i = 0; i < clusterSchemas.size(); i++)
         {
             ClusterSchema clusterSchema = (ClusterSchema) clusterSchemas.get(i);
             retval.append(clusterSchema.getXML());
         }
         retval.append("      </clusterschemas>" + Const.CR); //$NON-NLS-1$
 
         
         retval.append("  "+XMLHandler.addTagValue("modified_user", modifiedUser));
         retval.append("  "+XMLHandler.addTagValue("modified_date", modifiedDate!=null?modifiedDate.getString():""));
 
         retval.append("    </info>" + Const.CR); //$NON-NLS-1$
 
         retval.append("  <notepads>" + Const.CR); //$NON-NLS-1$
         if (notes != null) for (int i = 0; i < nrNotes(); i++)
         {
             NotePadMeta ni = getNote(i);
             retval.append(ni.getXML());
         }
         retval.append("    </notepads>" + Const.CR); //$NON-NLS-1$
 
         // The database connections...
         for (int i = 0; i < nrDatabases(); i++)
         {
             DatabaseMeta dbMeta = getDatabase(i);
             if (props!=null && props.areOnlyUsedConnectionsSavedToXML())
             {
                 if (isDatabaseConnectionUsed(dbMeta)) retval.append(dbMeta.getXML());
             }
             else
             {
                 retval.append(dbMeta.getXML());
             }
         }
 
         retval.append("  <order>" + Const.CR); //$NON-NLS-1$
         for (int i = 0; i < nrTransHops(); i++)
         {
             TransHopMeta transHopMeta = getTransHop(i);
             retval.append(transHopMeta.getXML());
         }
         retval.append("  </order>" + Const.CR + Const.CR); //$NON-NLS-1$
 
         for (int i = 0; i < nrSteps(); i++)
         {
             StepMeta stepMeta = getStep(i);
             retval.append(stepMeta.getXML());
         }
 
         retval.append("</"+XML_TAG+">" + Const.CR); //$NON-NLS-1$
 
         return retval.toString();
     }
 
     /**
      * Parse a file containing the XML that describes the transformation.
      * No default connections are loaded since no repository is available at this time.
      * Since the filename is set, internal variables are being set that relate to this.
      *
      * @param fname The filename
      */
     public TransMeta(String fname) throws KettleXMLException
     {
         this(fname, true);
     }
 
     /**
      * Parse a file containing the XML that describes the transformation.
      * No default connections are loaded since no repository is available at this time.
      *
      * @param fname The filename
      * @param setInternalVariables true if you want to set the internal variables based on this transformation information
      */
     public TransMeta(String fname, boolean setInternalVariables) throws KettleXMLException
     {
         this(fname, null, setInternalVariables);
     }
 
     /**
      * Parse a file containing the XML that describes the transformation.
      *
      * @param fname The filename
      * @param rep The repository to load the default set of connections from, null if no repository is avaailable
       */
     public TransMeta(String fname, Repository rep) throws KettleXMLException
     {
         this(fname, rep, true);
     }
 
     /**
      * Parse a file containing the XML that describes the transformation.
      *
      * @param fname The filename
      * @param rep The repository to load the default set of connections from, null if no repository is avaailable
      * @param setInternalVariables true if you want to set the internal variables based on this transformation information
       */
     public TransMeta(String fname, Repository rep, boolean setInternalVariables ) throws KettleXMLException
     {
         Document doc = XMLHandler.loadXMLFile(fname);
         if (doc != null)
         {
             // Clear the transformation
             clearUndo();
             clear();
 
             // Root node:
             Node transnode = XMLHandler.getSubNode(doc, "transformation"); //$NON-NLS-1$
 
             // Load from this node...
             loadXML(transnode, rep, setInternalVariables);
 
             setFilename(fname);
         }
         else
         {
             throw new KettleXMLException(Messages.getString("TransMeta.Exception.ErrorOpeningOrValidatingTheXMLFile", fname)); //$NON-NLS-1$
         }
     }
 
     /**
      * Load the transformation from an XML node
      *
      * @param transnode The XML node to parse
      * @throws KettleXMLException
      */
     public TransMeta(Node transnode) throws KettleXMLException
     {
         loadXML(transnode);
     }
 
     /**
      * Parse a file containing the XML that describes the transformation.
      * (no repository is available to load default list of database connections from)
      *
      * @param transnode The XML node to load from
      * @throws KettleXMLException
      */
     private void loadXML(Node transnode) throws KettleXMLException
     {
         loadXML(transnode, null, false);
     }
 
     
     /**
      * Parse a file containing the XML that describes the transformation.
      *
      * @param transnode The XML node to load from
      * @param rep The repository to load the default list of database connections from (null if no repository is available)
      * @param setInternalVariables true if you want to set the internal variables based on this transformation information
      * @throws KettleXMLException
      */
     private void loadXML(Node transnode, Repository rep, boolean setInternalVariables ) throws KettleXMLException
     {
         Props props = null;
         if (Props.isInitialized())
         {
             props=Props.getInstance();
         }
         
         try
         {
             // Clear the transformation
             clearUndo();
             clear();
             
             // Read all the database connections from the repository to make sure that we don't overwrite any there by loading from XML.
            if (rep!=null)
             {
                try
                {
                    readSharedObjects(rep);
                    clearChanged();
                }
                catch(KettleException e)
                {
                    throw new KettleXMLException(Messages.getString("TransMeta.Exception.UnableToReadSharedObjectsFromRepository.Message"), e);
                }
             }
 
             // Handle connections
             int n = XMLHandler.countNodes(transnode, "connection"); //$NON-NLS-1$
             log.logDebug(toString(), Messages.getString("TransMeta.Log.WeHaveConnections", String.valueOf(n) )); //$NON-NLS-1$ //$NON-NLS-2$
             for (int i = 0; i < n; i++)
             {
                 log.logDebug(toString(), Messages.getString("TransMeta.Log.LookingAtConnection") + i); //$NON-NLS-1$
                 Node nodecon = XMLHandler.getSubNodeByNr(transnode, "connection", i); //$NON-NLS-1$
 
                 DatabaseMeta dbcon = new DatabaseMeta(nodecon);
 
                 DatabaseMeta exist = findDatabase(dbcon.getName());
                 if (exist == null)
                 {
                     addDatabase(dbcon);
                 }
                 else
                 {
                     if (!exist.isShared()) // otherwise, we just keep the shared connection.
                     {
                         boolean askOverwrite = Props.isInitialized() ? props.askAboutReplacingDatabaseConnections() : false;
                         boolean overwrite = Props.isInitialized() ? props.replaceExistingDatabaseConnections() : true;
                         if (askOverwrite)
                         {
                             // That means that we have a Display variable set in Props...
                             if (props.getDisplay()!=null)
                             {
                                 Shell shell = props.getDisplay().getActiveShell();
                                 
                                 MessageDialogWithToggle md = new MessageDialogWithToggle(shell, 
                                     "Warning",  
                                     null,
                                     "Connection ["+dbcon.getName()+"] already exists, do you want to overwrite this database connection?",
                                     MessageDialog.WARNING,
                                     new String[] { "Yes", "No" },//"Yes", "No" 
                                     1,
                                     "Please, don't show this warning anymore.",
                                     !props.askAboutReplacingDatabaseConnections()
                                );
                                int idx = md.open();
                                props.setAskAboutReplacingDatabaseConnections(!md.getToggleState());
                                overwrite = ((idx&0xFF)==0); // Yes means: overwrite
                             }
                         }
     
                         if (overwrite)
                         {
                             int idx = indexOfDatabase(exist);
                             removeDatabase(idx);
                             addDatabase(idx, dbcon);
                         }
                     }
                 }
             }
 
             // Read the notes...
             Node notepadsnode = XMLHandler.getSubNode(transnode, "notepads"); //$NON-NLS-1$
             int nrnotes = XMLHandler.countNodes(notepadsnode, "notepad"); //$NON-NLS-1$
             for (int i = 0; i < nrnotes; i++)
             {
                 Node notepadnode = XMLHandler.getSubNodeByNr(notepadsnode, "notepad", i); //$NON-NLS-1$
                 NotePadMeta ni = new NotePadMeta(notepadnode);
                 notes.add(ni);
             }
 
             // Handle Steps
             int s = XMLHandler.countNodes(transnode, "step"); //$NON-NLS-1$
 
             log.logDebug(toString(), Messages.getString("TransMeta.Log.ReadingSteps") + s + " steps..."); //$NON-NLS-1$ //$NON-NLS-2$
             for (int i = 0; i < s; i++)
             {
                 Node stepnode = XMLHandler.getSubNodeByNr(transnode, "step", i); //$NON-NLS-1$
 
                 log.logDebug(toString(), Messages.getString("TransMeta.Log.LookingAtStep") + i); //$NON-NLS-1$
                 StepMeta stepMeta = new StepMeta(stepnode, databases, counters);
                 // Check if the step exists and if it's a shared step.
                 // If so, then we will keep the shared version, not this one.
                 // The stored XML is only for backup purposes.
                 StepMeta check = findStep(stepMeta.getName());
                 if (check!=null)
                 {
                     if (!check.isShared()) // Don't overwrite shared objects
                     {
                         addOrReplaceStep(stepMeta);
                     }
                     else
                     {
                         check.setDraw(stepMeta.isDrawn()); // Just keep the drawn flag and location
                         check.setLocation(stepMeta.getLocation());
                     }
                 }
                 else
                 {
                     addStep(stepMeta); // simply add it.
                 }
             }
 
             // Have all StreamValueLookups, etc. reference the correct source steps...
             for (int i = 0; i < nrSteps(); i++)
             {
                 StepMeta stepMeta = getStep(i);
                 StepMetaInterface sii = stepMeta.getStepMetaInterface();
                 if (sii != null) sii.searchInfoAndTargetSteps(steps);
             }
 
             // Handle Hops
             Node ordernode = XMLHandler.getSubNode(transnode, "order"); //$NON-NLS-1$
             n = XMLHandler.countNodes(ordernode, "hop"); //$NON-NLS-1$
 
             log.logDebug(toString(), Messages.getString("TransMeta.Log.WeHaveHops") + n + " hops..."); //$NON-NLS-1$ //$NON-NLS-2$
             for (int i = 0; i < n; i++)
             {
                 log.logDebug(toString(), Messages.getString("TransMeta.Log.LookingAtHop") + i); //$NON-NLS-1$
                 Node hopnode = XMLHandler.getSubNodeByNr(ordernode, "hop", i); //$NON-NLS-1$
 
                 TransHopMeta hopinf = new TransHopMeta(hopnode, steps);
                 addTransHop(hopinf);
             }
 
             //
             // get transformation info:
             //
             Node infonode = XMLHandler.getSubNode(transnode, "info"); //$NON-NLS-1$
 
             // Name
             name = XMLHandler.getTagValue(infonode, "name"); //$NON-NLS-1$
 
             /*
              * Directory String directoryPath = XMLHandler.getTagValue(infonode, "directory");
              */
 
             // Logging method...
             readStep = findStep(XMLHandler.getTagValue(infonode, "log", "read")); //$NON-NLS-1$ //$NON-NLS-2$
             writeStep = findStep(XMLHandler.getTagValue(infonode, "log", "write")); //$NON-NLS-1$ //$NON-NLS-2$
             inputStep = findStep(XMLHandler.getTagValue(infonode, "log", "input")); //$NON-NLS-1$ //$NON-NLS-2$
             outputStep = findStep(XMLHandler.getTagValue(infonode, "log", "output")); //$NON-NLS-1$ //$NON-NLS-2$
             updateStep = findStep(XMLHandler.getTagValue(infonode, "log", "update")); //$NON-NLS-1$ //$NON-NLS-2$
             String logcon = XMLHandler.getTagValue(infonode, "log", "connection"); //$NON-NLS-1$ //$NON-NLS-2$
             logConnection = findDatabase(logcon);
             logTable = XMLHandler.getTagValue(infonode, "log", "table"); //$NON-NLS-1$ //$NON-NLS-2$
             useBatchId = "Y".equalsIgnoreCase(XMLHandler.getTagValue(infonode, "log", "use_batchid")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
             logfieldUsed= "Y".equalsIgnoreCase(XMLHandler.getTagValue(infonode, "log", "USE_LOGFIELD")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 
             // Maxdate range options...
             String maxdatcon = XMLHandler.getTagValue(infonode, "maxdate", "connection"); //$NON-NLS-1$ //$NON-NLS-2$
             maxDateConnection = findDatabase(maxdatcon);
             maxDateTable = XMLHandler.getTagValue(infonode, "maxdate", "table"); //$NON-NLS-1$ //$NON-NLS-2$
             maxDateField = XMLHandler.getTagValue(infonode, "maxdate", "field"); //$NON-NLS-1$ //$NON-NLS-2$
             String offset = XMLHandler.getTagValue(infonode, "maxdate", "offset"); //$NON-NLS-1$ //$NON-NLS-2$
             maxDateOffset = Const.toDouble(offset, 0.0);
             String mdiff = XMLHandler.getTagValue(infonode, "maxdate", "maxdiff"); //$NON-NLS-1$ //$NON-NLS-2$
             maxDateDifference = Const.toDouble(mdiff, 0.0);
 
             // Check the dependencies as far as dates are concerned...
             // We calculate BEFORE we run the MAX of these dates
             // If the date is larger then enddate, startdate is set to MIN_DATE
             //
             Node depsNode = XMLHandler.getSubNode(infonode, "dependencies"); //$NON-NLS-1$
             int nrDeps = XMLHandler.countNodes(depsNode, "dependency"); //$NON-NLS-1$
 
             for (int i = 0; i < nrDeps; i++)
             {
                 Node depNode = XMLHandler.getSubNodeByNr(depsNode, "dependency", i); //$NON-NLS-1$
 
                 TransDependency transDependency = new TransDependency(depNode, databases);
                 if (transDependency.getDatabase() != null && transDependency.getFieldname() != null)
                 {
                     addDependency(transDependency);
                 }
             }
 
             // Read the partitioning schemas
             // 
             Node partSchemasNode = XMLHandler.getSubNode(infonode, "partitionschemas"); //$NON-NLS-1$
             int nrPartSchemas = XMLHandler.countNodes(partSchemasNode, PartitionSchema.XML_TAG); //$NON-NLS-1$
             for (int i = 0 ; i < nrPartSchemas ; i++)
             {
                 Node partSchemaNode = XMLHandler.getSubNodeByNr(partSchemasNode, PartitionSchema.XML_TAG, i);
                 PartitionSchema partitionSchema = new PartitionSchema(partSchemaNode);
                 
                 // Check if the step exists and if it's a shared step.
                 // If so, then we will keep the shared version, not this one.
                 // The stored XML is only for backup purposes.
                 PartitionSchema check = findPartitionSchema(partitionSchema.getName());
                 if (check!=null)
                 {
                     if (!check.isShared()) // we don't overwrite shared objects.
                     {
                         addOrReplacePartitionSchema(partitionSchema);
                     }
                 }
                 else
                 {
                     partitionSchemas.add(partitionSchema);
                 }
                 
             }
             
             // Have all step partitioning meta-data reference the correct schemas that we just loaded
             // 
             for (int i = 0; i < nrSteps(); i++)
             {
                 StepPartitioningMeta stepPartitioningMeta = getStep(i).getStepPartitioningMeta();
                 if (stepPartitioningMeta!=null)
                 {
                     stepPartitioningMeta.setPartitionSchemaAfterLoading(partitionSchemas);
                 }
             }
 
             // Read the slave servers...
             // 
             Node slaveServersNode = XMLHandler.getSubNode(infonode, "slaveservers"); //$NON-NLS-1$
             int nrSlaveServers = XMLHandler.countNodes(slaveServersNode, SlaveServer.XML_TAG); //$NON-NLS-1$
             for (int i = 0 ; i < nrSlaveServers ; i++)
             {
                 Node slaveServerNode = XMLHandler.getSubNodeByNr(slaveServersNode, SlaveServer.XML_TAG, i);
                 SlaveServer slaveServer = new SlaveServer(slaveServerNode);
                 
                 // Check if the object exists and if it's a shared object.
                 // If so, then we will keep the shared version, not this one.
                 // The stored XML is only for backup purposes.
                 SlaveServer check = findSlaveServer(slaveServer.getName());
                 if (check!=null)
                 {
                     if (!check.isShared()) // we don't overwrite shared objects.
                     {
                         addOrReplaceSlaveServer(slaveServer);
                     }
                 }
                 else
                 {
                     slaveServers.add(slaveServer);
                 }
             }
 
             // Read the cluster schemas
             // 
             Node clusterSchemasNode = XMLHandler.getSubNode(infonode, "clusterschemas"); //$NON-NLS-1$
             int nrClusterSchemas = XMLHandler.countNodes(clusterSchemasNode, ClusterSchema.XML_TAG); //$NON-NLS-1$
             for (int i = 0 ; i < nrClusterSchemas ; i++)
             {
                 Node clusterSchemaNode = XMLHandler.getSubNodeByNr(clusterSchemasNode, ClusterSchema.XML_TAG, i);
                 ClusterSchema clusterSchema = new ClusterSchema(clusterSchemaNode, slaveServers);
                 System.out.println("Loaded "+clusterSchema.getSlaveServers().size()+" servers in cluster "+clusterSchema.getName());
                 
                 // Check if the object exists and if it's a shared object.
                 // If so, then we will keep the shared version, not this one.
                 // The stored XML is only for backup purposes.
                 ClusterSchema check = findClusterSchema(clusterSchema.getName());
                 if (check!=null)
                 {
                     if (!check.isShared()) // we don't overwrite shared objects.
                     {
                         addOrReplaceClusterSchema(clusterSchema);
                     }
                 }
                 else
                 {
                     clusterSchemas.add(clusterSchema);
                 }
             }
             
             // Have all step clustering schema meta-data reference the correct cluster schemas that we just loaded
             // 
             for (int i = 0; i < nrSteps(); i++)
             {
                 getStep(i).setClusterSchemaAfterLoading(clusterSchemas);
             }
            
             String srowset = XMLHandler.getTagValue(infonode, "size_rowset"); //$NON-NLS-1$
             sizeRowset = Const.toInt(srowset, Const.ROWS_IN_ROWSET);
             sleepTimeEmpty = Const.toInt(XMLHandler.getTagValue(infonode, "sleep_time_empty"), Const.SLEEP_EMPTY_NANOS); //$NON-NLS-1$
             sleepTimeFull  = Const.toInt(XMLHandler.getTagValue(infonode, "sleep_time_full"), Const.SLEEP_FULL_NANOS); //$NON-NLS-1$
             usingUniqueConnections = "Y".equalsIgnoreCase( XMLHandler.getTagValue(infonode, "unique_connections") ); //$NON-NLS-1$
 
             feedbackShown = !"N".equalsIgnoreCase( XMLHandler.getTagValue(infonode, "feedback_shown") ); //$NON-NLS-1$
             feedbackSize = Const.toInt(XMLHandler.getTagValue(infonode, "feedback_size"), Const.ROWS_UPDATE); //$NON-NLS-1$
             usingThreadPriorityManagment = !"N".equalsIgnoreCase( XMLHandler.getTagValue(infonode, "using_thread_priorities") ); //$NON-NLS-1$ 
             
             // Changed user/date
             modifiedUser = XMLHandler.getTagValue(infonode, "modified_user");
             String modDate = XMLHandler.getTagValue(infonode, "modified_date");
             if (modDate!=null)
             {
                 modifiedDate = new Value(STRING_MODIFIED_DATE, modDate);
                 modifiedDate.setType(Value.VALUE_TYPE_DATE);
             }
             
             log.logDebug(toString(), Messages.getString("TransMeta.Log.NumberOfStepsReaded") + nrSteps()); //$NON-NLS-1$
             log.logDebug(toString(), Messages.getString("TransMeta.Log.NumberOfHopsReaded") + nrTransHops()); //$NON-NLS-1$
 
             sortSteps();
         }
         catch (KettleXMLException xe)
         {
             throw new KettleXMLException(Messages.getString("TransMeta.Exception.ErrorReadingTransformation"), xe); //$NON-NLS-1$
         }
         finally
         {
             if (setInternalVariables) setInternalKettleVariables();
         }
 
     }
 
     public void readSharedObjects(Repository rep) throws KettleException
     {
         // Extract the shared steps, connections, etc. using the SharedObjects class
         //
         String soFile = StringUtil.environmentSubstitute(sharedObjectsFile);
         SharedObjects sharedObjects = new SharedObjects(soFile, databases, counters, slaveServers); 
         Map objectsMap = sharedObjects.getObjectsMap();
         Collection objects = objectsMap.values();
         
         // First read the databases...
         // We read databases & slaves first because there might be dependencies that need to be resolved.
         //
         for (Iterator iter = objects.iterator(); iter.hasNext();)
         {
             Object object = iter.next();
             if (object instanceof DatabaseMeta)
             {
                 DatabaseMeta databaseMeta = (DatabaseMeta) object;
                 addOrReplaceDatabase(databaseMeta);
             }
         }
 
         // Then read the slave servers
         //
         for (Iterator iter = objects.iterator(); iter.hasNext();)
         {
             Object object = iter.next();
             if (object instanceof SlaveServer)
             {
                 SlaveServer slaveServer = (SlaveServer) object;
                 addOrReplaceSlaveServer(slaveServer);
                 System.out.println("Read slave server ["+slaveServer+"]");
             }
         }
 
         // Then the rest...
         ///
         for (Iterator iter = objects.iterator(); iter.hasNext();)
         {
             Object object = iter.next();
             if (object instanceof StepMeta)
             {
                 StepMeta stepMeta = (StepMeta) object;
                 addOrReplaceStep(stepMeta);
             }
             else if (object instanceof PartitionSchema)
             {
                 PartitionSchema partitionSchema = (PartitionSchema) object;
                 addOrReplacePartitionSchema(partitionSchema);
             }
             else if (object instanceof ClusterSchema)
             {
                 ClusterSchema clusterSchema = (ClusterSchema) object;
                 addOrReplaceClusterSchema(clusterSchema);
             }
         }
 
         if (rep!=null)
         {
             readDatabases(rep, true);
             readPartitionSchemas(rep, true);
             readSlaves(rep, true);
             readClusters(rep, true);
         }
     }
 
     /**
      * Gives you an ArrayList of all the steps that are at least used in one active hop. These steps will be used to
      * execute the transformation. The others will not be executed.
      *
      * @param all Set to true if you want to get ALL the steps from the transformation.
      * @return A ArrayList of steps
      */
     public ArrayList getTransHopSteps(boolean all)
     {
         ArrayList st = new ArrayList();
         int idx;
 
         for (int x = 0; x < nrTransHops(); x++)
         {
             TransHopMeta hi = getTransHop(x);
             if (hi.isEnabled() || all)
             {
                 idx = st.indexOf(hi.getFromStep()); // FROM
                 if (idx < 0) st.add(hi.getFromStep());
 
                 idx = st.indexOf(hi.getToStep()); // TO
                 if (idx < 0) st.add(hi.getToStep());
             }
         }
 
         // Also, add the steps that need to be painted, but are not part of a hop
         for (int x = 0; x < nrSteps(); x++)
         {
             StepMeta stepMeta = getStep(x);
             if (stepMeta.isDrawn() && !isStepUsedInTransHops(stepMeta))
             {
                 st.add(stepMeta);
             }
         }
 
         return st;
     }
 
     /**
      * Get the name of the transformation
      *
      * @return The name of the transformation
      */
     public String getName()
     {
         return name;
     }
 
     /**
      * Set the name of the transformation.
      *
      * @param n The new name of the transformation
      */
     public void setName(String n)
     {
         name = n;
         setInternalKettleVariables();
     }
 
     /**
      * Get the filename (if any) of the transformation
      *
      * @return The filename of the transformation.
      */
     public String getFilename()
     {
         return filename;
     }
 
     /**
      * Set the filename of the transformation
      *
      * @param fname The new filename of the transformation.
      */
     public void setFilename(String fname)
     {
         filename = fname;
         setInternalKettleVariables();
     }
 
     /**
      * Determines if a step has been used in a hop or not.
      *
      * @param stepMeta The step queried.
      * @return True if a step is used in a hop (active or not), false if this is not the case.
      */
     public boolean isStepUsedInTransHops(StepMeta stepMeta)
     {
         TransHopMeta fr = findTransHopFrom(stepMeta);
         TransHopMeta to = findTransHopTo(stepMeta);
         if (fr != null || to != null) return true;
         return false;
     }
 
     /**
      * Mark the transformation as being changed.
      *
      */
     public void setChanged()
     {
         setChanged(true);
     }
 
     /**
      * Sets the changed parameter of the transformation.
      *
      * @param ch True if you want to mark the transformation as changed, false if not.
      */
     public void setChanged(boolean ch)
     {
         changed = ch;
     }
 
     /**
      * Clears the different changed flags of the transformation.
      *
      */
     public void clearChanged()
     {
         changed = false;
         changed_steps = false;
         changed_databases = false;
         changed_hops = false;
         changed_notes = false;
 
         for (int i = 0; i < nrSteps(); i++)
         {
             getStep(i).setChanged(false);
         }
         for (int i = 0; i < nrDatabases(); i++)
         {
             getDatabase(i).setChanged(false);
         }
         for (int i = 0; i < nrTransHops(); i++)
         {
             getTransHop(i).setChanged(false);
         }
         for (int i = 0; i < nrNotes(); i++)
         {
             getNote(i).setChanged(false);
         }
         for (int i = 0; i < partitionSchemas.size(); i++)
         {
             ((PartitionSchema)partitionSchemas.get(i)).setChanged(false);
         }
         for (int i = 0; i < clusterSchemas.size(); i++)
         {
             ((ClusterSchema)clusterSchemas.get(i)).setChanged(false);
         }
     }
 
     /**
      * Checks whether or not the connections have changed.
      *
      * @return True if the connections have been changed.
      */
     public boolean haveConnectionsChanged()
     {
         if (changed_databases) return true;
 
         for (int i = 0; i < nrDatabases(); i++)
         {
             DatabaseMeta ci = getDatabase(i);
             if (ci.hasChanged()) return true;
         }
         return false;
     }
 
     /**
      * Checks whether or not the steps have changed.
      *
      * @return True if the connections have been changed.
      */
     public boolean haveStepsChanged()
     {
         if (changed_steps) return true;
 
         for (int i = 0; i < nrSteps(); i++)
         {
             StepMeta stepMeta = getStep(i);
             if (stepMeta.hasChanged()) return true;
         }
         return false;
     }
 
     /**
      * Checks whether or not any of the hops have been changed.
      *
      * @return True if a hop has been changed.
      */
     public boolean haveHopsChanged()
     {
         if (changed_hops) return true;
 
         for (int i = 0; i < nrTransHops(); i++)
         {
             TransHopMeta hi = getTransHop(i);
             if (hi.hasChanged()) return true;
         }
         return false;
     }
 
     /**
      * Checks whether or not any of the notes have been changed.
      *
      * @return True if the notes have been changed.
      */
     public boolean haveNotesChanged()
     {
         if (changed_notes) return true;
 
         for (int i = 0; i < nrNotes(); i++)
         {
             NotePadMeta ni = getNote(i);
             if (ni.hasChanged()) return true;
         }
 
         return false;
     }
     
     /**
      * Checks whether or not any of the database partitioning schemas have been changed.
      *
      * @return True if the partitioning schemas have been changed.
      */
     public boolean havePartitionSchemasChanged()
     {
         for (int i = 0; i < partitionSchemas.size(); i++)
         {
             PartitionSchema ps = (PartitionSchema) partitionSchemas.get(i);
             if (ps.hasChanged()) return true;
         }
 
         return false;
     }
     
     /**
      * Checks whether or not any of the clustering schemas have been changed.
      *
      * @return True if the clustering schemas have been changed.
      */
     public boolean haveClusterSchemasChanged()
     {
         for (int i = 0; i < clusterSchemas.size(); i++)
         {
             ClusterSchema cs = (ClusterSchema) clusterSchemas.get(i);
             if (cs.hasChanged()) return true;
         }
 
         return false;
     }
 
 
     /**
      * Checks whether or not the transformation has changed.
      *
      * @return True if the transformation has changed.
      */
     public boolean hasChanged()
     {
         if (changed) return true;
 
         if (haveConnectionsChanged()) return true;
         if (haveStepsChanged()) return true;
         if (haveHopsChanged()) return true;
         if (haveNotesChanged()) return true;
         if (havePartitionSchemasChanged()) return true;
         if (haveClusterSchemasChanged()) return true;
 
         return false;
     }
 
     /**
      * See if there are any loops in the transformation, starting at the indicated step. This works by looking at all
      * the previous steps. If you keep going backward and find the step, there is a loop. Both the informational and the
      * normal steps need to be checked for loops!
      *
      * @param stepMeta The step position to start looking
      *
      * @return True if a loop has been found, false if no loop is found.
      */
     public boolean hasLoop(StepMeta stepMeta)
     {
         return hasLoop(stepMeta, null, true) || hasLoop(stepMeta, null, false);
     }
 
     /**
      * See if there are any loops in the transformation, starting at the indicated step. This works by looking at all
      * the previous steps. If you keep going backward and find the orginal step again, there is a loop.
      *
      * @param stepMeta The step position to start looking
      * @param lookup The original step when wandering around the transformation.
      * @param info Check the informational steps or not.
      *
      * @return True if a loop has been found, false if no loop is found.
      */
     public boolean hasLoop(StepMeta stepMeta, StepMeta lookup, boolean info)
     {
         int nr = findNrPrevSteps(stepMeta, info);
         for (int i = 0; i < nr; i++)
         {
             StepMeta prevStepMeta = findPrevStep(stepMeta, i, info);
             if (prevStepMeta != null)
             {
                 if (prevStepMeta.equals(stepMeta)) return true;
                 if (prevStepMeta.equals(lookup)) return true;
                 if (hasLoop(prevStepMeta, lookup == null ? stepMeta : lookup, info)) return true;
             }
         }
         return false;
     }
 
     /**
      * Mark all steps in the transformation as selected.
      *
      */
     public void selectAll()
     {
         int i;
         for (i = 0; i < nrSteps(); i++)
         {
             StepMeta stepMeta = getStep(i);
             stepMeta.setSelected(true);
         }
         for (i = 0; i < nrNotes(); i++)
         {
             NotePadMeta ni = getNote(i);
             ni.setSelected(true);
         }
     }
 
     /**
      * Clear the selection of all steps.
      *
      */
     public void unselectAll()
     {
         int i;
         for (i = 0; i < nrSteps(); i++)
         {
             StepMeta stepMeta = getStep(i);
             stepMeta.setSelected(false);
         }
         for (i = 0; i < nrNotes(); i++)
         {
             NotePadMeta ni = getNote(i);
             ni.setSelected(false);
         }
     }
 
     /**
      * Count the number of selected steps in this transformation
      *
      * @return The number of selected steps.
      */
     public int nrSelectedSteps()
     {
         int i, count;
         count = 0;
         for (i = 0; i < nrSteps(); i++)
         {
             StepMeta stepMeta = getStep(i);
             if (stepMeta.isSelected() && stepMeta.isDrawn()) count++;
         }
         return count;
     }
 
     /**
      * Get the selected step at a certain location
      *
      * @param nr The location
      * @return The selected step
      */
     public StepMeta getSelectedStep(int nr)
     {
         int i, count;
         count = 0;
         for (i = 0; i < nrSteps(); i++)
         {
             StepMeta stepMeta = getStep(i);
             if (stepMeta.isSelected() && stepMeta.isDrawn())
             {
                 if (nr == count) return stepMeta;
                 count++;
             }
         }
         return null;
     }
 
     /**
      * Count the number of selected notes in this transformation
      *
      * @return The number of selected notes.
      */
     public int nrSelectedNotes()
     {
         int i, count;
         count = 0;
         for (i = 0; i < nrNotes(); i++)
         {
             NotePadMeta ni = getNote(i);
             if (ni.isSelected()) count++;
         }
         return count;
     }
 
     /**
      * Get the selected note at a certain index
      *
      * @param nr The index
      * @return The selected note
      */
     public NotePadMeta getSelectedNote(int nr)
     {
         int i, count;
         count = 0;
         for (i = 0; i < nrNotes(); i++)
         {
             NotePadMeta ni = getNote(i);
             if (ni.isSelected())
             {
                 if (nr == count) return ni;
                 count++;
             }
         }
         return null;
     }
 
     /**
      * Select all the steps in a certain (screen) rectangle
      *
      * @param rect The selection area as a rectangle
      */
     public void selectInRect(Rectangle rect)
     {
         for (int i = 0; i < nrSteps(); i++)
         {
             StepMeta stepMeta = getStep(i);
             Point a = stepMeta.getLocation();
             if (rect.contains(a)) stepMeta.setSelected(true);
         }
 
         for (int i = 0; i < nrNotes(); i++)
         {
             NotePadMeta ni = getNote(i);
             Point a = ni.getLocation();
             Point b = new Point(a.x + ni.width, a.y + ni.height);
             if (rect.contains(a) && rect.contains(b)) ni.setSelected(true);
         }
     }
 
     /**
      * Get an array of all the selected step and note locations
      *
      * @return The selected step and notes locations.
      */
     public Point[] getSelectedStepLocations()
     {
         ArrayList points = new ArrayList();
 
         for (int i = 0; i < nrSelectedSteps(); i++)
         {
             StepMeta stepMeta = getSelectedStep(i);
             Point p = stepMeta.getLocation();
             points.add(new Point(p.x, p.y)); // explicit copy of location
         }
 
         return (Point[]) points.toArray(new Point[points.size()]);
     }
 
     /**
      * Get an array of all the selected step and note locations
      *
      * @return The selected step and notes locations.
      */
     public Point[] getSelectedNoteLocations()
     {
         ArrayList points = new ArrayList();
 
         for (int i = 0; i < nrSelectedNotes(); i++)
         {
             NotePadMeta ni = getSelectedNote(i);
             Point p = ni.getLocation();
             points.add(new Point(p.x, p.y)); // explicit copy of location
         }
 
         return (Point[]) points.toArray(new Point[points.size()]);
     }
 
     /**
      * Get an array of all the selected steps
      *
      * @return An array of all the selected steps.
      */
     public StepMeta[] getSelectedSteps()
     {
         int sels = nrSelectedSteps();
         if (sels == 0) return null;
 
         StepMeta retval[] = new StepMeta[sels];
         for (int i = 0; i < sels; i++)
         {
             StepMeta stepMeta = getSelectedStep(i);
             retval[i] = stepMeta;
 
         }
         return retval;
     }
     
     /**
      * Get an array of all the selected steps
      *
      * @return A list containing all the selected & drawn steps.
      */
     public List getSelectedDrawnStepsList()
     {
         List list = new ArrayList();
         
         for (int i = 0; i < nrSteps(); i++)
         {
             StepMeta stepMeta = getStep(i);
             if (stepMeta.isDrawn() && stepMeta.isSelected()) list.add(stepMeta);
 
         }
         return list;
     }
 
     /**
      * Get an array of all the selected notes
      *
      * @return An array of all the selected notes.
      */
     public NotePadMeta[] getSelectedNotes()
     {
         int sels = nrSelectedNotes();
         if (sels == 0) return null;
 
         NotePadMeta retval[] = new NotePadMeta[sels];
         for (int i = 0; i < sels; i++)
         {
             NotePadMeta si = getSelectedNote(i);
             retval[i] = si;
 
         }
         return retval;
     }
 
     /**
      * Get an array of all the selected step names
      *
      * @return An array of all the selected step names.
      */
     public String[] getSelectedStepNames()
     {
         int sels = nrSelectedSteps();
         if (sels == 0) return null;
 
         String retval[] = new String[sels];
         for (int i = 0; i < sels; i++)
         {
             StepMeta stepMeta = getSelectedStep(i);
             retval[i] = stepMeta.getName();
         }
         return retval;
     }
 
     /**
      * Get an array of the locations of an array of steps
      *
      * @param steps An array of steps
      * @return an array of the locations of an array of steps
      */
     public int[] getStepIndexes(StepMeta steps[])
     {
         int retval[] = new int[steps.length];
 
         for (int i = 0; i < steps.length; i++)
         {
             retval[i] = indexOfStep(steps[i]);
         }
 
         return retval;
     }
 
     /**
      * Get an array of the locations of an array of notes
      *
      * @param notes An array of notes
      * @return an array of the locations of an array of notes
      */
     public int[] getNoteIndexes(NotePadMeta notes[])
     {
         int retval[] = new int[notes.length];
 
         for (int i = 0; i < notes.length; i++)
             retval[i] = indexOfNote(notes[i]);
 
         return retval;
     }
 
     /**
      * Get the maximum number of undo operations possible
      *
      * @return The maximum number of undo operations that are allowed.
      */
     public int getMaxUndo()
     {
         return max_undo;
     }
 
     /**
      * Sets the maximum number of undo operations that are allowed.
      *
      * @param mu The maximum number of undo operations that are allowed.
      */
     public void setMaxUndo(int mu)
     {
         max_undo = mu;
         while (undo.size() > mu && undo.size() > 0)
             undo.remove(0);
     }
 
     /**
      * Add an undo operation to the undo list
      *
      * @param from array of objects representing the old state
      * @param to array of objectes representing the new state
      * @param pos An array of object locations
      * @param prev An array of points representing the old positions
      * @param curr An array of points representing the new positions
      * @param type_of_change The type of change that's being done to the transformation.
      * @param nextAlso indicates that the next undo operation needs to follow this one.
      */
     public void addUndo(Object from[], Object to[], int pos[], Point prev[], Point curr[], int type_of_change, boolean nextAlso)
     {
         // First clean up after the current position.
         // Example: position at 3, size=5
         // 012345
         // ^
         // remove 34
         // Add 4
         // 01234
 
         while (undo.size() > undo_position + 1 && undo.size() > 0)
         {
             int last = undo.size() - 1;
             undo.remove(last);
         }
 
         TransAction ta = new TransAction();
         switch (type_of_change)
         {
         case TYPE_UNDO_CHANGE:
             ta.setChanged(from, to, pos);
             break;
         case TYPE_UNDO_DELETE:
             ta.setDelete(from, pos);
             break;
         case TYPE_UNDO_NEW:
             ta.setNew(from, pos);
             break;
         case TYPE_UNDO_POSITION:
             ta.setPosition(from, pos, prev, curr);
             break;
         }
         ta.setNextAlso(nextAlso);
         undo.add(ta);
         undo_position++;
 
         if (undo.size() > max_undo)
         {
             undo.remove(0);
             undo_position--;
         }
     }
 
     /**
      * Get the previous undo operation and change the undo pointer
      *
      * @return The undo transaction to be performed.
      */
     public TransAction previousUndo()
     {
         if (undo.size() == 0 || undo_position < 0) return null; // No undo left!
 
         TransAction retval = (TransAction) undo.get(undo_position);
 
         undo_position--;
 
         return retval;
     }
 
     /**
      * View current undo, don't change undo position
      *
      * @return The current undo transaction
      */
     public TransAction viewThisUndo()
     {
         if (undo.size() == 0 || undo_position < 0) return null; // No undo left!
 
         TransAction retval = (TransAction) undo.get(undo_position);
 
         return retval;
     }
 
     /**
      * View previous undo, don't change undo position
      *
      * @return The previous undo transaction
      */
     public TransAction viewPreviousUndo()
     {
         if (undo.size() == 0 || undo_position - 1 < 0) return null; // No undo left!
 
         TransAction retval = (TransAction) undo.get(undo_position - 1);
 
         return retval;
     }
 
     /**
      * Get the next undo transaction on the list. Change the undo pointer.
      *
      * @return The next undo transaction (for redo)
      */
     public TransAction nextUndo()
     {
         int size = undo.size();
         if (size == 0 || undo_position >= size - 1) return null; // no redo left...
 
         undo_position++;
 
         TransAction retval = (TransAction) undo.get(undo_position);
 
         return retval;
     }
 
     /**
      * Get the next undo transaction on the list.
      *
      * @return The next undo transaction (for redo)
      */
     public TransAction viewNextUndo()
     {
         int size = undo.size();
         if (size == 0 || undo_position >= size - 1) return null; // no redo left...
 
         TransAction retval = (TransAction) undo.get(undo_position + 1);
 
         return retval;
     }
 
     /**
      * Get the maximum size of the canvas by calculating the maximum location of a step
      *
      * @return Maximum coordinate of a step in the transformation + (100,100) for safety.
      */
     public Point getMaximum()
     {
         int maxx = 0, maxy = 0;
         for (int i = 0; i < nrSteps(); i++)
         {
             StepMeta stepMeta = getStep(i);
             Point loc = stepMeta.getLocation();
             if (loc.x > maxx) maxx = loc.x;
             if (loc.y > maxy) maxy = loc.y;
         }
         for (int i = 0; i < nrNotes(); i++)
         {
             NotePadMeta notePadMeta = getNote(i);
             Point loc = notePadMeta.getLocation();
             if (loc.x + notePadMeta.width > maxx) maxx = loc.x + notePadMeta.width;
             if (loc.y + notePadMeta.height > maxy) maxy = loc.y + notePadMeta.height;
         }
 
         return new Point(maxx + 100, maxy + 100);
     }
 
     /**
      * Get the names of all the steps.
      *
      * @return An array of step names.
      */
     public String[] getStepNames()
     {
         String retval[] = new String[nrSteps()];
 
         for (int i = 0; i < nrSteps(); i++)
             retval[i] = getStep(i).getName();
 
         return retval;
     }
 
     /**
      * Get all the steps in an array.
      *
      * @return An array of all the steps in the transformation.
      */
     public StepMeta[] getStepsArray()
     {
         StepMeta retval[] = new StepMeta[nrSteps()];
 
         for (int i = 0; i < nrSteps(); i++)
             retval[i] = getStep(i);
 
         return retval;
     }
 
     /**
      * Find a step with the ID in a given ArrayList of steps
      *
      * @param steps The ArrayList of steps
      * @param id The ID of the step
      * @return The step if it was found, null if nothing was found
      */
     public static final StepMeta findStep(ArrayList steps, long id)
     {
         if (steps == null) return null;
 
         for (int i = 0; i < steps.size(); i++)
         {
             StepMeta stepMeta = (StepMeta) steps.get(i);
             if (stepMeta.getID() == id) return stepMeta;
         }
         return null;
     }
 
     /**
      * Find a step with its name in a given ArrayList of steps
      *
      * @param steps The ArrayList of steps
      * @param stepname The name of the step
      * @return The step if it was found, null if nothing was found
      */
     public static final StepMeta findStep(ArrayList steps, String stepname)
     {
         if (steps == null) return null;
 
         for (int i = 0; i < steps.size(); i++)
         {
             StepMeta stepMeta = (StepMeta) steps.get(i);
             if (stepMeta.getName().equalsIgnoreCase(stepname)) return stepMeta;
         }
         return null;
     }
 
     /**
      * Look in the transformation and see if we can find a step in a previous location starting somewhere.
      *
      * @param startStep The starting step
      * @param stepToFind The step to look for backward in the transformation
      * @return true if we can find the step in an earlier location in the transformation.
      */
     public boolean findPrevious(StepMeta startStep, StepMeta stepToFind)
     {
         // Normal steps
         int nrPrevious = findNrPrevSteps(startStep, false);
         for (int i = 0; i < nrPrevious; i++)
         {
             StepMeta stepMeta = findPrevStep(startStep, i, false);
             if (stepMeta.equals(stepToFind)) return true;
 
             boolean found = findPrevious(stepMeta, stepToFind); // Look further back in the tree.
             if (found) return true;
         }
 
         // Info steps
         nrPrevious = findNrPrevSteps(startStep, true);
         for (int i = 0; i < nrPrevious; i++)
         {
             StepMeta stepMeta = findPrevStep(startStep, i, true);
             if (stepMeta.equals(stepToFind)) return true;
 
             boolean found = findPrevious(stepMeta, stepToFind); // Look further back in the tree.
             if (found) return true;
         }
 
         return false;
     }
 
     /**
      * Put the steps in alfabetical order.
      */
     public void sortSteps()
     {
         try
         {
             Const.quickSort(steps);
         }
         catch (Exception e)
         {
             System.out.println(Messages.getString("TransMeta.Exception.ErrorOfSortingSteps") + e); //$NON-NLS-1$
             e.printStackTrace();
         }
     }
 
     public void sortHops()
     {
         Const.quickSort(hops);
     }
 
     /**
      * Put the steps in a more natural order: from start to finish. For the moment, we ignore splits and joins. Splits
      * and joins can't be listed sequentially in any case!
      *
      */
     public void sortStepsNatural()
     {
         // Loop over the steps...
         for (int j = 0; j < nrSteps(); j++)
         {
             // Buble sort: we need to do this several times...
             for (int i = 0; i < nrSteps() - 1; i++)
             {
                 StepMeta one = getStep(i);
                 StepMeta two = getStep(i + 1);
 
                 if (!findPrevious(two, one))
                 {
                     setStep(i + 1, one);
                     setStep(i, two);
                 }
             }
         }
     }
 
     /**
      * Sort the hops in a natural way: from beginning to end
      */
     public void sortHopsNatural()
     {
         // Loop over the hops...
         for (int j = 0; j < nrTransHops(); j++)
         {
             // Buble sort: we need to do this several times...
             for (int i = 0; i < nrTransHops() - 1; i++)
             {
                 TransHopMeta one = getTransHop(i);
                 TransHopMeta two = getTransHop(i + 1);
 
                 StepMeta a = two.getFromStep();
                 StepMeta b = one.getToStep();
 
                 if (!findPrevious(a, b) && !a.equals(b))
                 {
                     setTransHop(i + 1, one);
                     setTransHop(i, two);
                 }
             }
         }
     }
 
     /**
      * This procedure determines the impact of the different steps in a transformation on databases, tables and field.
      *
      * @param impact An ArrayList of DatabaseImpact objects.
      *
      */
     public void analyseImpact(ArrayList impact, IProgressMonitor monitor) throws KettleStepException
     {
         if (monitor != null)
         {
             monitor.beginTask(Messages.getString("TransMeta.Monitor.DeterminingImpactTask.Title"), nrSteps()); //$NON-NLS-1$
         }
         boolean stop = false;
         for (int i = 0; i < nrSteps() && !stop; i++)
         {
             if (monitor != null) monitor.subTask(Messages.getString("TransMeta.Monitor.LookingAtStepTask.Title") + (i + 1) + "/" + nrSteps()); //$NON-NLS-1$ //$NON-NLS-2$
             StepMeta stepMeta = getStep(i);
 
             Row prev = getPrevStepFields(stepMeta);
             StepMetaInterface stepint = stepMeta.getStepMetaInterface();
             Row inform = null;
             StepMeta[] lu = getInfoStep(stepMeta);
             if (lu != null)
             {
                 inform = getStepFields(lu);
             }
             else
             {
                 inform = stepint.getTableFields();
             }
 
             stepint.analyseImpact(impact, this, stepMeta, prev, null, null, inform);
 
             if (monitor != null)
             {
                 monitor.worked(1);
                 stop = monitor.isCanceled();
             }
         }
 
         if (monitor != null) monitor.done();
     }
 
     /**
      * Proposes an alternative stepname when the original already exists...
      *
      * @param stepname The stepname to find an alternative for..
      * @return The alternative stepname.
      */
     public String getAlternativeStepname(String stepname)
     {
         String newname = stepname;
         StepMeta stepMeta = findStep(newname);
         int nr = 1;
         while (stepMeta != null)
         {
             nr++;
             newname = stepname + " " + nr; //$NON-NLS-1$
             stepMeta = findStep(newname);
         }
 
         return newname;
     }
 
     /**
      * Builds a list of all the SQL statements that this transformation needs in order to work properly.
      *
      * @return An ArrayList of SQLStatement objects.
      */
     public ArrayList getSQLStatements() throws KettleStepException
     {
         return getSQLStatements(null);
     }
 
     /**
      * Builds a list of all the SQL statements that this transformation needs in order to work properly.
      *
      * @return An ArrayList of SQLStatement objects.
      */
     public ArrayList getSQLStatements(IProgressMonitor monitor) throws KettleStepException
     {
         if (monitor != null) monitor.beginTask(Messages.getString("TransMeta.Monitor.GettingTheSQLForTransformationTask.Title"), nrSteps() + 1); //$NON-NLS-1$
         ArrayList stats = new ArrayList();
 
         for (int i = 0; i < nrSteps(); i++)
         {
             StepMeta stepMeta = getStep(i);
             if (monitor != null) monitor.subTask(Messages.getString("TransMeta.Monitor.GettingTheSQLForStepTask.Title",""+stepMeta )); //$NON-NLS-1$ //$NON-NLS-2$
             Row prev = getPrevStepFields(stepMeta);
             SQLStatement sql = stepMeta.getStepMetaInterface().getSQLStatements(this, stepMeta, prev);
             if (sql.getSQL() != null || sql.hasError())
             {
                 stats.add(sql);
             }
             if (monitor != null) monitor.worked(1);
         }
 
         // Also check the sql for the logtable...
         if (monitor != null) monitor.subTask(Messages.getString("TransMeta.Monitor.GettingTheSQLForTransformationTask.Title2")); //$NON-NLS-1$
         if (logConnection != null && logTable != null && logTable.length() > 0)
         {
             Database db = new Database(logConnection);
             try
             {
                 db.connect();
                 Row fields = Database.getTransLogrecordFields(useBatchId, logfieldUsed);
                 String sql = db.getDDL(logTable, fields);
                 if (sql != null && sql.length() > 0)
                 {
                     SQLStatement stat = new SQLStatement("<this transformation>", logConnection, sql); //$NON-NLS-1$
                     stats.add(stat);
                 }
             }
             catch (KettleDatabaseException dbe)
             {
                 SQLStatement stat = new SQLStatement("<this transformation>", logConnection, null); //$NON-NLS-1$
                 stat.setError(Messages.getString("TransMeta.SQLStatement.ErrorDesc.ErrorObtainingTransformationLogTableInfo") + dbe.getMessage()); //$NON-NLS-1$
                 stats.add(stat);
             }
             finally
             {
                 db.disconnect();
             }
         }
         if (monitor != null) monitor.worked(1);
         if (monitor != null) monitor.done();
 
         return stats;
     }
 
     /**
      * Get the SQL statements, needed to run this transformation, as one String.
      *
      * @return the SQL statements needed to run this transformation.
      */
     public String getSQLStatementsString() throws KettleStepException
     {
         String sql = ""; //$NON-NLS-1$
         ArrayList stats = getSQLStatements();
         for (int i = 0; i < stats.size(); i++)
         {
             SQLStatement stat = (SQLStatement) stats.get(i);
             if (!stat.hasError() && stat.hasSQL())
             {
                 sql += stat.getSQL();
             }
         }
 
         return sql;
     }
 
     /**
      * Checks all the steps and fills a List of (CheckResult) remarks.
      *
      * @param remarks The remarks list to add to.
      * @param only_selected Check only the selected steps.
      * @param monitor The progress monitor to use, null if not used
      */
     public void checkSteps(ArrayList remarks, boolean only_selected, IProgressMonitor monitor)
     {
         try
         {
             remarks.clear(); // Start with a clean slate...
 
             Hashtable values = new Hashtable();
             String stepnames[];
             StepMeta steps[];
             if (!only_selected || nrSelectedSteps() == 0)
             {
                 stepnames = getStepNames();
                 steps = getStepsArray();
             }
             else
             {
                 stepnames = getSelectedStepNames();
                 steps = getSelectedSteps();
             }
 
             boolean stop_checking = false;
 
             if (monitor != null) monitor.beginTask(Messages.getString("TransMeta.Monitor.VerifyingThisTransformationTask.Title"), steps.length + 2); //$NON-NLS-1$
 
             for (int i = 0; i < steps.length && !stop_checking; i++)
             {
                 if (monitor != null) monitor.subTask(Messages.getString("TransMeta.Monitor.VerifyingStepTask.Title",stepnames[i])); //$NON-NLS-1$ //$NON-NLS-2$
 
                 StepMeta stepMeta = steps[i];
 
                 int nrinfo = findNrInfoSteps(stepMeta);
                 StepMeta[] infostep = null;
                 if (nrinfo > 0)
                 {
                     infostep = getInfoStep(stepMeta);
                 }
 
                 Row info = null;
                 if (infostep != null)
                 {
                     try
                     {
                         info = getStepFields(infostep);
                     }
                     catch (KettleStepException kse)
                     {
                         info = null;
                         CheckResult cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("TransMeta.CheckResult.TypeResultError.ErrorOccurredGettingStepInfoFields.Description",""+stepMeta , Const.CR + kse.getMessage()), stepMeta); //$NON-NLS-1$
                         remarks.add(cr);
                     }
                 }
 
                 // The previous fields from non-informative steps:
                 Row prev = null;
                 try
                 {
                     prev = getPrevStepFields(stepMeta);
                 }
                 catch (KettleStepException kse)
                 {
                     CheckResult cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("TransMeta.CheckResult.TypeResultError.ErrorOccurredGettingInputFields.Description", ""+stepMeta
                             , Const.CR + kse.getMessage()), stepMeta); //$NON-NLS-1$
                     remarks.add(cr);
                     // This is a severe error: stop checking...
                     // Otherwise we wind up checking time & time again because nothing gets put in the database
                     // cache, the timeout of certain databases is very long... (Oracle)
                     stop_checking = true;
                 }
 
                 if (isStepUsedInTransHops(stepMeta))
                 {
                     // Get the input & output steps!
                     // Copy to arrays:
                     String input[] = getPrevStepNames(stepMeta);
                     String output[] = getPrevStepNames(stepMeta);
 
                     // Check step specific info...
                     stepMeta.check(remarks, prev, input, output, info);
 
                     // See if illegal characters etc. were used in field-names...
                     if (prev != null)
                     {
                         for (int x = 0; x < prev.size(); x++)
                         {
                             Value v = prev.getValue(x);
                             String name = v.getName();
                             if (name == null)
                                 values.put(v, Messages.getString("TransMeta.Value.CheckingFieldName.FieldNameIsEmpty.Description")); //$NON-NLS-1$
                             else
                                 if (name.indexOf(' ') >= 0)
                                     values.put(v, Messages.getString("TransMeta.Value.CheckingFieldName.FieldNameContainsSpaces.Description")); //$NON-NLS-1$
                                 else
                                 {
                                     char list[] = new char[] { '.', ',', '-', '/', '+', '*', '\'', '\t', '"', '|', '@', '(', ')', '{', '}', '!', '^' };
                                     for (int c = 0; c < list.length; c++)
                                     {
                                         if (name.indexOf(list[c]) >= 0)
                                             values.put(v, Messages.getString("TransMeta.Value.CheckingFieldName.FieldNameContainsUnfriendlyCodes.Description",String.valueOf(list[c]) )); //$NON-NLS-1$ //$NON-NLS-2$
                                     }
                                 }
                         }
 
                         // Check if 2 steps with the same name are entering the step...
                         if (prev.size() > 1)
                         {
                             String fieldNames[] = prev.getFieldNames();
                             String sortedNames[] = Const.sortStrings(fieldNames);
 
                             String prevName = sortedNames[0];
                             for (int x = 1; x < sortedNames.length; x++)
                             {
                                 // Checking for doubles
                                 if (prevName.equalsIgnoreCase(sortedNames[x]))
                                 {
                                     // Give a warning!!
                                     CheckResult cr = new CheckResult(CheckResult.TYPE_RESULT_WARNING,
                                             Messages.getString("TransMeta.CheckResult.TypeResultWarning.HaveTheSameNameField.Description", prevName ), stepMeta); //$NON-NLS-1$ //$NON-NLS-2$
                                     remarks.add(cr);
                                 }
                                 else
                                 {
                                     prevName = sortedNames[x];
                                 }
                             }
                         }
                     }
                     else
                     {
                         CheckResult cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("TransMeta.CheckResult.TypeResultError.CannotFindPreviousFields.Description") + stepMeta.getName(), //$NON-NLS-1$
                                 stepMeta);
                         remarks.add(cr);
                     }
                 }
                 else
                 {
                     CheckResult cr = new CheckResult(CheckResult.TYPE_RESULT_WARNING, Messages.getString("TransMeta.CheckResult.TypeResultWarning.StepIsNotUsed.Description"), stepMeta); //$NON-NLS-1$
                     remarks.add(cr);
                 }
 
                 if (monitor != null)
                 {
                     monitor.worked(1); // progress bar...
                     if (monitor.isCanceled()) stop_checking = true;
                 }
             }
 
             // Also, check the logging table of the transformation...
             if (monitor == null || !monitor.isCanceled())
             {
                 if (monitor != null) monitor.subTask(Messages.getString("TransMeta.Monitor.CheckingTheLoggingTableTask.Title")); //$NON-NLS-1$
                 if (getLogConnection() != null)
                 {
                     Database logdb = new Database(getLogConnection());
                     try
                     {
                         logdb.connect();
                         CheckResult cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("TransMeta.CheckResult.TypeResultOK.ConnectingWorks.Description"), //$NON-NLS-1$
                                 null);
                         remarks.add(cr);
 
                         if (getLogTable() != null)
                         {
                             if (logdb.checkTableExists(getLogTable()))
                             {
                                 cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("TransMeta.CheckResult.TypeResultOK.LoggingTableExists.Description", getLogTable() ), null); //$NON-NLS-1$ //$NON-NLS-2$
                                 remarks.add(cr);
 
                                 Row fields = Database.getTransLogrecordFields(isBatchIdUsed(), isLogfieldUsed());
                                 String sql = logdb.getDDL(getLogTable(), fields);
                                 if (sql == null || sql.length() == 0)
                                 {
                                     cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("TransMeta.CheckResult.TypeResultOK.CorrectLayout.Description"), null); //$NON-NLS-1$
                                     remarks.add(cr);
                                 }
                                 else
                                 {
                                     cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("TransMeta.CheckResult.TypeResultError.LoggingTableNeedsAdjustments.Description") + Const.CR + sql, //$NON-NLS-1$
                                             null);
                                     remarks.add(cr);
                                 }
 
                             }
                             else
                             {
                                 cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("TransMeta.CheckResult.TypeResultError.LoggingTableDoesNotExist.Description"), null); //$NON-NLS-1$
                                 remarks.add(cr);
                             }
                         }
                         else
                         {
                             cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("TransMeta.CheckResult.TypeResultError.LogTableNotSpecified.Description"), null); //$NON-NLS-1$
                             remarks.add(cr);
                         }
                     }
                     catch (KettleDatabaseException dbe)
                     {
 
                     }
                     finally
                     {
                         logdb.disconnect();
                     }
                 }
                 if (monitor != null) monitor.worked(1);
 
             }
 
             if (monitor != null) monitor.subTask(Messages.getString("TransMeta.Monitor.CheckingForDatabaseUnfriendlyCharactersInFieldNamesTask.Title")); //$NON-NLS-1$
             if (values.size() > 0)
             {
                 Enumeration keys = values.keys();
                 while (keys.hasMoreElements())
                 {
                     Value v = (Value) keys.nextElement();
                     String message = (String) values.get(v);
                     CheckResult cr = new CheckResult(CheckResult.TYPE_RESULT_WARNING, Messages.getString("TransMeta.CheckResult.TypeResultWarning.Description",v.getName() , message ,v.getOrigin() ), findStep(v.getOrigin())); //$NON-NLS-1$
                     remarks.add(cr);
                 }
             }
             else
             {
                 CheckResult cr = new CheckResult(CheckResult.TYPE_RESULT_OK,
                         Messages.getString("TransMeta.CheckResult.TypeResultOK.Description"), null); //$NON-NLS-1$
                 remarks.add(cr);
             }
             if (monitor != null) monitor.worked(1);
         }
         catch (Exception e)
         {
             e.printStackTrace();
             throw new RuntimeException(e);
         }
     }
 
     /**
      * @return Returns the resultRows.
      */
     public ArrayList getResultRows()
     {
         return resultRows;
     }
 
     /**
      * @param resultRows The resultRows to set.
      */
     public void setResultRows(ArrayList resultRows)
     {
         this.resultRows = resultRows;
     }
 
     /**
      * @return Returns the sourceRows.
      * @deprecated : use getPreviousResult().getRows()
      */
     public ArrayList getSourceRows()
     {
         return sourceRows;
     }
 
     /**
      * @param sourceRows The sourceRows to set.
      * @deprecated : use getPreviousResult().getRows().addAll(sourceRows)
      */
     public void setSourceRows(ArrayList sourceRows)
     {
         this.sourceRows = sourceRows;
     }
 
     /**
      * @return Returns the directory.
      */
     public RepositoryDirectory getDirectory()
     {
         return directory;
     }
 
     /**
      * @param directory The directory to set.
      */
     public void setDirectory(RepositoryDirectory directory)
     {
         this.directory = directory;
         setInternalKettleVariables();
     }
 
     /**
      * @return Returns the directoryTree.
      * @deprecated
      */
     public RepositoryDirectory getDirectoryTree()
     {
         return directoryTree;
     }
 
     /**
      * @param directoryTree The directoryTree to set.
      * @deprecated
      */
     public void setDirectoryTree(RepositoryDirectory directoryTree)
     {
         this.directoryTree = directoryTree;
     }
 
     /**
      * @return The directory path plus the name of the transformation
      */
     public String getPathAndName()
     {
         if (getDirectory().isRoot())
             return getDirectory().getPath() + getName();
         else
             return getDirectory().getPath() + RepositoryDirectory.DIRECTORY_SEPARATOR + getName();
     }
 
     /**
      * @return Returns the arguments.
      */
     public String[] getArguments()
     {
         return arguments;
     }
 
     /**
      * @param arguments The arguments to set.
      */
     public void setArguments(String[] arguments)
     {
         this.arguments = arguments;
     }
 
     /**
      * @return Returns the counters.
      */
     public Hashtable getCounters()
     {
         return counters;
     }
 
     /**
      * @param counters The counters to set.
      */
     public void setCounters(Hashtable counters)
     {
         this.counters = counters;
     }
 
     /**
      * @return Returns the dependencies.
      */
     public ArrayList getDependencies()
     {
         return dependencies;
     }
 
     /**
      * @param dependencies The dependencies to set.
      */
     public void setDependencies(ArrayList dependencies)
     {
         this.dependencies = dependencies;
     }
 
     /**
      * @return Returns the id.
      */
     public long getId()
     {
         return id;
     }
 
     /**
      * @param id The id to set.
      */
     public void setId(long id)
     {
         this.id = id;
     }
 
     /**
      * @return Returns the inputStep.
      */
     public StepMeta getInputStep()
     {
         return inputStep;
     }
 
     /**
      * @param inputStep The inputStep to set.
      */
     public void setInputStep(StepMeta inputStep)
     {
         this.inputStep = inputStep;
     }
 
     /**
      * @return Returns the logConnection.
      */
     public DatabaseMeta getLogConnection()
     {
         return logConnection;
     }
 
     /**
      * @param logConnection The logConnection to set.
      */
     public void setLogConnection(DatabaseMeta logConnection)
     {
         this.logConnection = logConnection;
     }
 
     /**
      * @return Returns the logTable.
      */
     public String getLogTable()
     {
         return logTable;
     }
 
     /**
      * @param logTable The logTable to set.
      */
     public void setLogTable(String logTable)
     {
         this.logTable = logTable;
     }
 
     /**
      * @return Returns the maxDateConnection.
      */
     public DatabaseMeta getMaxDateConnection()
     {
         return maxDateConnection;
     }
 
     /**
      * @param maxDateConnection The maxDateConnection to set.
      */
     public void setMaxDateConnection(DatabaseMeta maxDateConnection)
     {
         this.maxDateConnection = maxDateConnection;
     }
 
     /**
      * @return Returns the maxDateDifference.
      */
     public double getMaxDateDifference()
     {
         return maxDateDifference;
     }
 
     /**
      * @param maxDateDifference The maxDateDifference to set.
      */
     public void setMaxDateDifference(double maxDateDifference)
     {
         this.maxDateDifference = maxDateDifference;
     }
 
     /**
      * @return Returns the maxDateField.
      */
     public String getMaxDateField()
     {
         return maxDateField;
     }
 
     /**
      * @param maxDateField The maxDateField to set.
      */
     public void setMaxDateField(String maxDateField)
     {
         this.maxDateField = maxDateField;
     }
 
     /**
      * @return Returns the maxDateOffset.
      */
     public double getMaxDateOffset()
     {
         return maxDateOffset;
     }
 
     /**
      * @param maxDateOffset The maxDateOffset to set.
      */
     public void setMaxDateOffset(double maxDateOffset)
     {
         this.maxDateOffset = maxDateOffset;
     }
 
     /**
      * @return Returns the maxDateTable.
      */
     public String getMaxDateTable()
     {
         return maxDateTable;
     }
 
     /**
      * @param maxDateTable The maxDateTable to set.
      */
     public void setMaxDateTable(String maxDateTable)
     {
         this.maxDateTable = maxDateTable;
     }
 
     /**
      * @return Returns the outputStep.
      */
     public StepMeta getOutputStep()
     {
         return outputStep;
     }
 
     /**
      * @param outputStep The outputStep to set.
      */
     public void setOutputStep(StepMeta outputStep)
     {
         this.outputStep = outputStep;
     }
 
     /**
      * @return Returns the readStep.
      */
     public StepMeta getReadStep()
     {
         return readStep;
     }
 
     /**
      * @param readStep The readStep to set.
      */
     public void setReadStep(StepMeta readStep)
     {
         this.readStep = readStep;
     }
 
     /**
      * @return Returns the updateStep.
      */
     public StepMeta getUpdateStep()
     {
         return updateStep;
     }
 
     /**
      * @param updateStep The updateStep to set.
      */
     public void setUpdateStep(StepMeta updateStep)
     {
         this.updateStep = updateStep;
     }
 
     /**
      * @return Returns the writeStep.
      */
     public StepMeta getWriteStep()
     {
         return writeStep;
     }
 
     /**
      * @param writeStep The writeStep to set.
      */
     public void setWriteStep(StepMeta writeStep)
     {
         this.writeStep = writeStep;
     }
 
     /**
      * @return Returns the sizeRowset.
      */
     public int getSizeRowset()
     {
         return sizeRowset;
     }
 
     /**
      * @param sizeRowset The sizeRowset to set.
      */
     public void setSizeRowset(int sizeRowset)
     {
         this.sizeRowset = sizeRowset;
     }
 
     /**
      * @return Returns the dbCache.
      */
     public DBCache getDbCache()
     {
         return dbCache;
     }
 
     /**
      * @param dbCache The dbCache to set.
      */
     public void setDbCache(DBCache dbCache)
     {
         this.dbCache = dbCache;
     }
 
     /**
      * @return Returns the useBatchId.
      */
     public boolean isBatchIdUsed()
     {
         return useBatchId;
     }
 
     /**
      * @param useBatchId The useBatchId to set.
      */
     public void setBatchIdUsed(boolean useBatchId)
     {
         this.useBatchId = useBatchId;
     }
 
     /**
      * @return Returns the logfieldUsed.
      */
     public boolean isLogfieldUsed()
     {
         return logfieldUsed;
     }
 
     /**
      * @param logfieldUsed The logfieldUsed to set.
      */
     public void setLogfieldUsed(boolean logfieldUsed)
     {
         this.logfieldUsed = logfieldUsed;
     }
 
     /**
      * @return Returns the createdDate.
      */
     public Value getCreatedDate()
     {
         return createdDate;
     }
 
     /**
      * @param createdDate The createdDate to set.
      */
     public void setCreatedDate(Value createdDate)
     {
         this.createdDate = createdDate;
     }
 
     /**
      * @param createdUser The createdUser to set.
      */
     public void setCreatedUser(String createdUser)
     {
         this.createdUser = createdUser;
     }
 
     /**
      * @return Returns the createdUser.
      */
     public String getCreatedUser()
     {
         return createdUser;
     }
 
     /**
      * @param modifiedDate The modifiedDate to set.
      */
     public void setModifiedDate(Value modifiedDate)
     {
         this.modifiedDate = modifiedDate;
     }
 
     /**
      * @return Returns the modifiedDate.
      */
     public Value getModifiedDate()
     {
         return modifiedDate;
     }
 
     /**
      * @param modifiedUser The modifiedUser to set.
      */
     public void setModifiedUser(String modifiedUser)
     {
         this.modifiedUser = modifiedUser;
     }
 
     /**
      * @return Returns the modifiedUser.
      */
     public String getModifiedUser()
     {
         return modifiedUser;
     }
 
     /**
      * @return the textual representation of the transformation: it's name. If the name has not been set, the classname
      * is returned.
      */
     public String toString()
     {
         if (name != null) return name;
         if (filename != null) return filename;
         return Spoon.STRING_TRANS_NO_NAME;
     }
 
     /**
      * Cancel queries opened for checking & fieldprediction
      */
     public void cancelQueries() throws KettleDatabaseException
     {
         for (int i = 0; i < nrSteps(); i++)
         {
             getStep(i).getStepMetaInterface().cancelQueries();
         }
     }
 
     /**
      * Get the arguments used by this transformation.
      *
      * @param arguments
      * @return A row with the used arguments in it.
      */
     public Row getUsedArguments(String[] arguments)
     {
         Row args = new Row(); // Always at least return an empty row, not null!
         for (int i = 0; i < nrSteps(); i++)
         {
             StepMetaInterface smi = getStep(i).getStepMetaInterface();
             Row row = smi.getUsedArguments(); // Get the command line arguments that this step uses.
             if (row != null)
             {
                 for (int x = 0; x < row.size(); x++)
                 {
                     Value value = row.getValue(x);
                     String argname = value.getName();
                     if (args.searchValueIndex(argname) < 0) args.addValue(value);
                 }
             }
         }
 
         // OK, so perhaps, we can use the arguments from a previous execution?
         String[] saved = Props.isInitialized() ? Props.getInstance().getLastArguments() : null;
 
         // Set the default values on it...
         // Also change the name to "Argument 1" .. "Argument 10"
         for (int i = 0; i < args.size(); i++)
         {
             Value arg = args.getValue(i);
             int argNr = Const.toInt(arg.getName(), -1);
             if (arguments!=null && argNr >= 0 && argNr < arguments.length)
             {
                 arg.setValue(arguments[argNr]);
             }
             if (arg.isNull() || arg.getString() == null) // try the saved option...
             {
                 if (argNr >= 0 && argNr < saved.length && saved[argNr] != null)
                 {
                     arg.setValue(saved[argNr]);
                 }
             }
             arg.setName("Argument " + arg.getName()); //$NON-NLS-1$
         }
 
         return args;
     }
 
     public StepMeta getMappingInputStep()
     {
         for (int i = 0; i < nrSteps(); i++)
         {
             if (getStep(i).getStepID().equalsIgnoreCase("MappingInput")) { return getStep(i); } //$NON-NLS-1$
         }
         return null;
     }
 
     public StepMeta getMappingOutputStep()
     {
         for (int i = 0; i < nrSteps(); i++)
         {
             if (getStep(i).getStepID().equalsIgnoreCase("MappingOutput")) { return getStep(i); } //$NON-NLS-1$
         }
         return null;
     }
 
     /**
      * @return Sleep time waiting when buffer is empty, in nano-seconds
      */
     public int getSleepTimeEmpty()
     {
         return Const.SLEEP_EMPTY_NANOS;
     }
 
     /**
      * @return Sleep time waiting when buffer is full, in nano-seconds
      */
     public int getSleepTimeFull()
     {
         return Const.SLEEP_FULL_NANOS;
     }
 
     /**
      * @param sleepTimeEmpty The sleepTimeEmpty to set.
      */
     public void setSleepTimeEmpty(int sleepTimeEmpty)
     {
         this.sleepTimeEmpty = sleepTimeEmpty;
     }
 
     /**
      * @param sleepTimeFull The sleepTimeFull to set.
      */
     public void setSleepTimeFull(int sleepTimeFull)
     {
         this.sleepTimeFull = sleepTimeFull;
     }
 
     /**
      * This method asks all steps in the transformation whether or not the specified database connection is used.
      * The connection is used in the transformation if any of the steps uses it or if it is being used to log to.
      * @param databaseMeta The connection to check
      * @return true if the connection is used in this transformation.
      */
     public boolean isDatabaseConnectionUsed(DatabaseMeta databaseMeta)
     {
         for (int i=0;i<nrSteps();i++)
         {
             StepMeta stepMeta = getStep(i);
             DatabaseMeta dbs[] = stepMeta.getStepMetaInterface().getUsedDatabaseConnections();
             for (int d=0;d<dbs.length;d++)
             {
                 if (dbs[d].equals(databaseMeta)) return true;
             }
         }
 
         if (logConnection!=null && logConnection.equals(databaseMeta)) return true;
 
         return false;
     }
 
     public List getInputFiles() {
         return inputFiles;
     }
 
     public void setInputFiles(List inputFiles) {
         this.inputFiles = inputFiles;
     }
 
     /**
      * Get a list of all the strings used in this transformation.
      *
      * @return A list of StringSearchResult with strings used in the 
      */
     public List getStringList(boolean searchSteps, boolean searchDatabases, boolean searchNotes)
     {
         ArrayList stringList = new ArrayList();
 
         if (searchSteps)
         {
             // Loop over all steps in the transformation and see what the used vars are...
             for (int i=0;i<nrSteps();i++)
             {
                 StepMeta stepMeta = getStep(i);
                 stringList.add(new StringSearchResult(stepMeta.getName(), stepMeta, this, "Step name"));
                 if (stepMeta.getDescription()!=null) stringList.add(new StringSearchResult(stepMeta.getDescription(), stepMeta, this, "Step description"));
                 StepMetaInterface metaInterface = stepMeta.getStepMetaInterface();
                 StringSearcher.findMetaData(metaInterface, 1, stringList, stepMeta, this);
             }
         }
 
         // Loop over all steps in the transformation and see what the used vars are...
         if (searchDatabases)
         {
             for (int i=0;i<nrDatabases();i++)
             {
                 DatabaseMeta meta = getDatabase(i);
                 stringList.add(new StringSearchResult(meta.getName(), meta, this, "Database connection name"));
                 if (meta.getDatabaseName()!=null) stringList.add(new StringSearchResult(meta.getDatabaseName(), meta, this, "Database name"));
                 if (meta.getUsername()!=null) stringList.add(new StringSearchResult(meta.getUsername(), meta, this, "Database Username"));
                 if (meta.getDatabaseTypeDesc()!=null) stringList.add(new StringSearchResult(meta.getDatabaseTypeDesc(), meta, this, "Database type description"));
                 if (meta.getDatabasePortNumberString()!=null) stringList.add(new StringSearchResult(meta.getDatabasePortNumberString(), meta, this, "Database port"));
             }
         }
 
         // Loop over all steps in the transformation and see what the used vars are...
         if (searchNotes)
         {
             for (int i=0;i<nrNotes();i++)
             {
                 NotePadMeta meta = getNote(i);
                 if (meta.getNote()!=null) stringList.add(new StringSearchResult(meta.getNote(), meta, this, "Notepad text"));
             }
         }
 
         return stringList;
     }
 
     public List getUsedVariables()
     {
         // Get the list of Strings.
         List stringList = getStringList(true, true, false);
 
         List varList = new ArrayList();
 
         // Look around in the strings, see what we find...
         for (int i=0;i<stringList.size();i++)
         {
             StringSearchResult result = (StringSearchResult) stringList.get(i);
             StringUtil.getUsedVariables(result.getString(), varList, false);
         }
 
         return varList;
     }
 
 	/**
 	 * @return Returns the previousResult.
 	 */
 	public Result getPreviousResult()
 	{
 		return previousResult;
 	}
 
 	/**
 	 * @param previousResult The previousResult to set.
 	 */
 	public void setPreviousResult(Result previousResult)
 	{
 		this.previousResult = previousResult;
 	}
 
 	/**
 	 * @return Returns the resultFiles.
 	 */
 	public synchronized ArrayList getResultFiles()
 	{
 		return resultFiles;
 	}
 
 	/**
 	 * @param resultFiles The resultFiles to set.
 	 */
 	public void setResultFiles(ArrayList resultFiles)
 	{
 		this.resultFiles = resultFiles;
 	}
     
     /**
      * This method sets various internal kettle variables that can be used by the transformation.
      */
     public void setInternalKettleVariables()
     {
         KettleVariables variables = KettleVariables.getInstance();
         
         if (filename!=null) // we have a finename that's defined.
         {
             File file = new File(filename);
             try
             {
                 file = file.getCanonicalFile();
             }
             catch(IOException e)
             {
                 file = file.getAbsoluteFile();
             }
             
             // The directory of the transformation
             variables.setVariable(Const.INTERNAL_VARIABLE_TRANSFORMATION_FILENAME_DIRECTORY, file.getParent());
 
            // The filename of the transformation
             variables.setVariable(Const.INTERNAL_VARIABLE_TRANSFORMATION_FILENAME_NAME, file.getName());
         }
         else
         {
             variables.setVariable(Const.INTERNAL_VARIABLE_TRANSFORMATION_FILENAME_DIRECTORY, "");
             variables.setVariable(Const.INTERNAL_VARIABLE_TRANSFORMATION_FILENAME_NAME, "");
         }
         
         // The name of the transformation
         variables.setVariable(Const.INTERNAL_VARIABLE_TRANSFORMATION_NAME, Const.NVL(name, ""));
 
         // The name of the directory in the repository
         variables.setVariable(Const.INTERNAL_VARIABLE_TRANSFORMATION_REPOSITORY_DIRECTORY, directory!=null?directory.getPath():"");
     }
 
     /**
      * @return the partitionSchemas
      */
     public List getPartitionSchemas()
     {
         return partitionSchemas;
     }
 
     /**
      * @param partitionSchemas the partitionSchemas to set
      */
     public void setPartitionSchemas(List partitionSchemas)
     {
         this.partitionSchemas = partitionSchemas;
     }
 
     /**
      * Get the available partition schema names.
      * @return
      */
     public String[] getPartitionSchemasNames()
     {
         String names[] = new String[partitionSchemas.size()];
         for (int i=0;i<names.length;i++)
         {
             names[i] = ((PartitionSchema)partitionSchemas.get(i)).getName();
         }
         return names;
     }
 
     /**
      * @return the feedbackShown
      */
     public boolean isFeedbackShown()
     {
         return feedbackShown;
     }
 
     /**
      * @param feedbackShown the feedbackShown to set
      */
     public void setFeedbackShown(boolean feedbackShown)
     {
         this.feedbackShown = feedbackShown;
     }
 
     /**
      * @return the feedbackSize
      */
     public int getFeedbackSize()
     {
         return feedbackSize;
     }
 
     /**
      * @param feedbackSize the feedbackSize to set
      */
     public void setFeedbackSize(int feedbackSize)
     {
         this.feedbackSize = feedbackSize;
     }
 
     /**
      * @return the usingUniqueConnections
      */
     public boolean isUsingUniqueConnections()
     {
         return usingUniqueConnections;
     }
 
     /**
      * @param usingUniqueConnections the usingUniqueConnections to set
      */
     public void setUsingUniqueConnections(boolean usingUniqueConnections)
     {
         this.usingUniqueConnections = usingUniqueConnections;
     }
 
     public ArrayList getClusterSchemas()
     {
         return clusterSchemas;
     }
 
     public void setClusterSchemas(ArrayList clusterSchemas)
     {
         this.clusterSchemas = clusterSchemas;
     }
     
     /**
      * @return The slave server strings from this cluster schema
      */
     public String[] getClusterSchemaNames()
     {
         String[] names = new String[clusterSchemas.size()];
         for (int i=0;i<names.length;i++)
         {
             names[i] = ((ClusterSchema)clusterSchemas.get(i)).getName();
         }
         return names;
     }
 
     /**
      * Find a partition schema using its name.
      * @param name The name of the partition schema to look for.
      * @return the partition with the specified name of null if nothing was found 
      */
     public PartitionSchema findPartitionSchema(String name)
     {
         for (int i=0;i<partitionSchemas.size();i++)
         {
             PartitionSchema schema = (PartitionSchema)partitionSchemas.get(i);
             if (schema.getName().equalsIgnoreCase(name)) return schema;
         }
         return null;
     }
     
     /**
      * Find a clustering schema using its name
      * @param name The name of the clustering schema to look for.
      * @return the cluster schema with the specified name of null if nothing was found 
      */
     public ClusterSchema findClusterSchema(String name)
     {
         for (int i=0;i<clusterSchemas.size();i++)
         {
             ClusterSchema schema = (ClusterSchema)clusterSchemas.get(i);
             if (schema.getName().equalsIgnoreCase(name)) return schema;
         }
         return null;
     }
     
     /**
      * Add a new partition schema to the transformation if that didn't exist yet.
      * Otherwise, replace it.
      *
      * @param partitionSchema The partition schema to be added.
      */
     public void addOrReplacePartitionSchema(PartitionSchema partitionSchema)
     {
         int index = partitionSchemas.indexOf(partitionSchema);
         if (index<0)
         {
             partitionSchemas.add(partitionSchema);
         }
         else
         {
             PartitionSchema previous = (PartitionSchema) partitionSchemas.get(index);
             previous.replaceMeta(partitionSchema);
         }
         setChanged();
     }
     
     /**
      * Add a new slave server to the transformation if that didn't exist yet.
      * Otherwise, replace it.
      *
      * @param slaveServer The slave server to be added.
      */
     public void addOrReplaceSlaveServer(SlaveServer slaveServer)
     {
         int index = slaveServers.indexOf(slaveServer);
         if (index<0)
         {
             slaveServers.add(slaveServer); 
         }
         else
         {
             SlaveServer previous = (SlaveServer) slaveServers.get(index);
             previous.replaceMeta(slaveServer);
         }
         setChanged();
     }
     
     /**
      * Add a new cluster schema to the transformation if that didn't exist yet.
      * Otherwise, replace it.
      *
      * @param clusterSchema The cluster schema to be added.
      */
     public void addOrReplaceClusterSchema(ClusterSchema clusterSchema)
     {
         int index = clusterSchemas.indexOf(clusterSchema);
         if (index<0)
         {
             clusterSchemas.add(clusterSchema); 
         }
         else 
         {
             ClusterSchema previous = (ClusterSchema) clusterSchemas.get(index);
             previous.replaceMeta(clusterSchema);
         }
         setChanged();
     }
 
     public String getSharedObjectsFile()
     {
         return sharedObjectsFile;
     }
 
     public void setSharedObjectsFile(String sharedObjectsFile)
     {
         this.sharedObjectsFile = sharedObjectsFile;
     }
 
     public void saveSharedObjects() throws KettleException
     {
         try
         {
             // First load all the shared objects...
             String soFile = StringUtil.environmentSubstitute(sharedObjectsFile);
             SharedObjects sharedObjects = new SharedObjects(soFile, databases, counters, slaveServers);
             
             // Now overwrite the objects in there
             List shared = new ArrayList();
             shared.addAll(databases);
             shared.addAll(steps);
             shared.addAll(partitionSchemas);
             shared.addAll(slaveServers);
             shared.addAll(clusterSchemas);
             
             // The databases connections...
             for (int i=0;i<shared.size();i++)
             {
                 SharedObjectInterface sharedObject = (SharedObjectInterface) shared.get(i);
                 if (sharedObject.isShared()) 
                 {
                     sharedObjects.storeObject(sharedObject);
                 }
             }
             
             // Save the objects
             sharedObjects.saveToFile();
         }
         catch(IOException e)
         {
             
         }
     }
 
     /**
      * @return the usingThreadPriorityManagment
      */
     public boolean isUsingThreadPriorityManagment()
     {
         return usingThreadPriorityManagment;
     }
 
     /**
      * @param usingThreadPriorityManagment the usingThreadPriorityManagment to set
      */
     public void setUsingThreadPriorityManagment(boolean usingThreadPriorityManagment)
     {
         this.usingThreadPriorityManagment = usingThreadPriorityManagment;
     }
 
     public SlaveServer findSlaveServer(String serverString)
     {
         return SlaveServer.findSlaveServer(slaveServers, serverString);
     }
     
     public String[] getSlaveServerNames()
     {
         return SlaveServer.getSlaveServerNames(slaveServers);
     }
 
     /**
      * @return the slaveServers
      */
     public ArrayList getSlaveServers()
     {
         return slaveServers;
     }
 
     /**
      * @param slaveServers the slaveServers to set
      */
     public void setSlaveServers(ArrayList slaveServers)
     {
         this.slaveServers = slaveServers;
     }
 }
