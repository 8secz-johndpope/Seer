 /*
  * This file is part of DrFTPD, Distributed FTP Daemon.
  *
  * DrFTPD is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * DrFTPD is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with DrFTPD; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  */
 package org.drftpd.mirroring;
 
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.Properties;
 import java.util.Set;
 
 import net.sf.drftpd.NoAvailableSlaveException;
 import net.sf.drftpd.mirroring.Job;
 import net.sf.drftpd.mirroring.JobManager;
 
 import org.apache.log4j.Logger;
 import org.drftpd.PropertyHelper;
 import org.drftpd.master.RemoteSlave;
 import org.drftpd.mirroring.archivetypes.IncompleteDirectoryException;
 import org.drftpd.mirroring.archivetypes.OfflineSlaveException;
 import org.drftpd.plugins.Archive;
 import org.drftpd.remotefile.LinkedRemoteFileInterface;
 import org.drftpd.sections.SectionInterface;
 
 
 /**
  * @author zubov
  * @version $Id$
  */
 public abstract class ArchiveType {
     private static final Logger logger = Logger.getLogger(ArchiveType.class);
     private long _archiveAfter;
     private LinkedRemoteFileInterface _directory;
     protected Archive _parent;
     protected SectionInterface _section;
     private Set<RemoteSlave> _slaveList;
 
     public ArchiveType(Archive archive, SectionInterface section, Properties p) {
         _parent = archive;
         _section = section;
         setProperties(p);
     }
 
     /**
      * Once the Jobs in the jobList have been sent, this method is called
      * This is where files are possibly deleted from slaves
      */
     public abstract void cleanup(ArrayList jobList);
 
     public abstract HashSet<RemoteSlave> findDestinationSlaves();
 
     public final LinkedRemoteFileInterface getDirectory() {
         return _directory;
     }
 
     /**
      * Returns the oldest LinkedRemoteFile(directory) that needs to be archived by this type's definition
      * If no such directory exists, it returns null
      */
     public final LinkedRemoteFileInterface getOldestNonArchivedDir() {
         ArrayList<LinkedRemoteFileInterface> oldDirs = new ArrayList<LinkedRemoteFileInterface>();
 
         for (Iterator iter = getSection().getFile().getFiles().iterator();
                 iter.hasNext();) {
             LinkedRemoteFileInterface lrf = (LinkedRemoteFileInterface) iter.next();
             if (!lrf.isDirectory()) {
             	continue;
             }
             try {
                 _parent.checkPathForArchiveStatus(lrf.getPath());
             } catch (DuplicateArchiveException e1) {
                 continue;
             }
 
             try {
                 if (!isArchivedDir(lrf)) {
                     if ((System.currentTimeMillis() - lrf.lastModified()) > getArchiveAfter()) {
                         oldDirs.add(lrf);
                     }
                 }
             } catch (IncompleteDirectoryException e) {
                 continue;
             } catch (OfflineSlaveException e) {
                 continue;
             }
         }
 
         LinkedRemoteFileInterface oldestDir = null;
 
         for (Iterator iter = oldDirs.iterator(); iter.hasNext();) {
             LinkedRemoteFileInterface temp = (LinkedRemoteFileInterface) iter.next();
 
             if (oldestDir == null) {
                 oldestDir = temp;
 
                 continue;
             }
 
             if (oldestDir.lastModified() > temp.lastModified()) {
                 oldestDir = temp;
             }
         }
 
         if (oldestDir != null) {
             logger.debug(getClass().toString() +
                 " - Returning the oldest directory " + oldestDir);
         } else {
             logger.debug(getClass().toString() +
                 " - Returning a null directory");
         }
 
         return oldestDir;
     }
 
     /**
      * if the directory is archived by this type's definition, this method returns true
      */
     protected abstract boolean isArchivedDir(LinkedRemoteFileInterface lrf)
         throws IncompleteDirectoryException, OfflineSlaveException;
 
     /**
      * Returns unmodifiable Set<RemoteSlave>.
      */
     public final Set<RemoteSlave> getRSlaves() {
         return _slaveList == null ? null : Collections.unmodifiableSet(_slaveList);
     }
 
     /**
      * Adds relevant Jobs to the JobManager and returns an ArrayList of those Job's
      */
     public ArrayList<Job> send() {
         ArrayList<Job> jobs = recursiveSend(getDirectory());
         JobManager jm = _parent.getGlobalContext().getJobManager();
         jm.addJobsToQueue(jobs);
         return jobs;
     }
 
     private final ArrayList<Job> recursiveSend(LinkedRemoteFileInterface lrf) {
         ArrayList<Job> jobQueue = new ArrayList<Job>();
 
         for (Iterator iter = lrf.getFiles().iterator(); iter.hasNext();) {
             LinkedRemoteFileInterface src = (LinkedRemoteFileInterface) iter.next();
 
             HashSet<RemoteSlave> destSlaves = new HashSet<RemoteSlave>(getRSlaves());
             if (src.isFile()) {
                 destSlaves.removeAll(src.getSlaves());
                 if(destSlaves.isEmpty()) continue;
 
                 Job job = new Job(src, destSlaves, 3, destSlaves.size());
                 logger.info("Adding " + job + " to the job queue");
                 //jm.addJobToQueue(job);
                 jobQueue.add(job);
             } else {
                 jobQueue.addAll(recursiveSend(src));
             }
         }
         return jobQueue;
     }
 
     protected static final boolean isArchivedToXSlaves(LinkedRemoteFileInterface lrf,
         int x) throws IncompleteDirectoryException, OfflineSlaveException {
         HashSet<RemoteSlave> slaveSet = null;
 
         if (lrf.getFiles().isEmpty()) {
             return true;
         }
 
         try {
             if (!lrf.lookupSFVFile().getStatus().isFinished()) {
                 logger.debug(lrf.getPath() + " is not complete");
                 throw new IncompleteDirectoryException(lrf.getPath() +
                     " is not complete");
             }
         } catch (FileNotFoundException e) {
         } catch (IOException e) {
         } catch (NoAvailableSlaveException e) {
         	throw new OfflineSlaveException("SFV is offline", e);
         }
 
         for (Iterator iter = lrf.getFiles().iterator(); iter.hasNext();) {
             LinkedRemoteFileInterface file = (LinkedRemoteFileInterface) iter.next();
 
             if (file.isDirectory()) {
                 if (!isArchivedToXSlaves(file, x)) {
                     return false;
                 }
             } else { // file.isFile()
             	if(!file.isAvailable()) throw new OfflineSlaveException(file.getPath()+" is offline");
                 Collection<RemoteSlave> availableSlaves = file.getSlaves();
 
                 if (slaveSet == null) {
                     slaveSet = new HashSet<RemoteSlave>(availableSlaves);
                 } else {
                     if (!(slaveSet.containsAll(availableSlaves) &&
                             availableSlaves.containsAll(slaveSet))) {
                         return false;
                     }
                 }
             }
         }
 
         if (slaveSet == null) { // no files found in directory
             return true;
         }
 
         for (Iterator iter = slaveSet.iterator(); iter.hasNext();) {
             RemoteSlave rslave = (RemoteSlave) iter.next();
 
             if (!rslave.isAvailable()) {
                 throw new OfflineSlaveException(rslave.getName() +
                     " is offline");
             }
         }
 
         return (slaveSet.size() == x);
     }
 
     public final boolean isBusy() {
         return (getDirectory() != null);
     }
 
     protected final long getArchiveAfter() {
         return _archiveAfter;
     }
 
     public final SectionInterface getSection() {
         return _section;
     }
 
     /**
      * Sets standard properties for this ArchiveType
      */
     private void setProperties(Properties properties) {
         try {
             _archiveAfter = 60000 * Long.parseLong(PropertyHelper.getProperty(
                         properties, getSection().getName() + ".archiveAfter"));
         } catch (NullPointerException e) {
             _archiveAfter = 0;
         }
     }
 
     public final void setDirectory(LinkedRemoteFileInterface lrf) {
         _directory = lrf;
     }
 
     public final void setRSlaves(Set<RemoteSlave> slaveList) {
         _slaveList = slaveList;
     }
 
     public final void waitForSendOfFiles(ArrayList jobQueue) {
         while (true) {
        	if (_directory.isDeleted()) {
        		// all files will be deleted too, no need to removejobs, JobManager will do that
        		return;
        	}
             for (Iterator iter = jobQueue.iterator(); iter.hasNext();) {
                 Job job = (Job) iter.next();
 
                 if (job.isDone()) {
                    logger.debug("Job " + job + " is done being sent");
                     iter.remove();
                 }
             }
 
             try {
                 Thread.sleep(10000);
             } catch (InterruptedException e) {
             }
 
             if (jobQueue.isEmpty()) {
                 break;
             }
         }
     }
 
     public abstract String toString();
 
     protected String outputSlaves(Collection slaveList) {
         String toReturn = new String();
 
         for (Iterator iter = slaveList.iterator(); iter.hasNext();) {
             RemoteSlave rslave = (RemoteSlave) iter.next();
             toReturn = toReturn + rslave.getName();
 
             if (iter.hasNext()) {
                 toReturn = toReturn + ",";
             } else {
                 return toReturn;
             }
         }
 
         return "Empty";
     }
 }
