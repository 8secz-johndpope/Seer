 package hudson.scm;
 
 import hudson.model.AbstractBuild;
 import hudson.model.User;
 import hudson.scm.CVSChangeLogSet.CVSChangeLog;
 import hudson.util.IOException2;
 import org.apache.commons.digester.Digester;
 import org.xml.sax.SAXException;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Collection;
 import java.util.AbstractList;
 
 /**
  * {@link ChangeLogSet} for CVS.
  * @author Kohsuke Kawaguchi
  */
 public final class CVSChangeLogSet extends ChangeLogSet<CVSChangeLog> {
     private List<CVSChangeLog> logs;
 
     public CVSChangeLogSet(AbstractBuild<?,?> build, List<CVSChangeLog> logs) {
         super(build);
         this.logs = Collections.unmodifiableList(logs);
         for (CVSChangeLog log : logs)
             log.setParent(this);
     }
 
     /**
      * Returns the read-only list of changes.
      */
     public List<CVSChangeLog> getLogs() {
         return logs;
     }
 
     @Override
     public boolean isEmptySet() {
         return logs.isEmpty();
     }
 
 
     public Iterator<CVSChangeLog> iterator() {
         return logs.iterator();
     }
 
     public static CVSChangeLogSet parse( AbstractBuild build, java.io.File f ) throws IOException, SAXException {
         Digester digester = new Digester();
         ArrayList<CVSChangeLog> r = new ArrayList<CVSChangeLog>();
         digester.push(r);
 
         digester.addObjectCreate("*/entry",CVSChangeLog.class);
         digester.addBeanPropertySetter("*/entry/date");
         digester.addBeanPropertySetter("*/entry/time");
         digester.addBeanPropertySetter("*/entry/author","user");
         digester.addBeanPropertySetter("*/entry/msg");
         digester.addSetNext("*/entry","add");
 
         digester.addObjectCreate("*/entry/file",File.class);
         digester.addBeanPropertySetter("*/entry/file/name");
         digester.addBeanPropertySetter("*/entry/file/fullName");
         digester.addBeanPropertySetter("*/entry/file/revision");
         digester.addBeanPropertySetter("*/entry/file/prevrevision");
         digester.addCallMethod("*/entry/file/dead","setDead");
         digester.addSetNext("*/entry/file","addFile");
 
         try {
             digester.parse(f);
         } catch (IOException e) {
             throw new IOException2("Failed to parse "+f,e);
         } catch (SAXException e) {
             throw new IOException2("Failed to parse "+f,e);
         }
 
         // merge duplicate entries. Ant task somehow seems to report duplicate entries.
         for(int i=r.size()-1; i>=0; i--) {
             CVSChangeLog log = r.get(i);
             boolean merged = false;
             for(int j=0;j<i;j++) {
                 CVSChangeLog c = r.get(j);
                 if(c.canBeMergedWith(log)) {
                     c.merge(log);
                     merged = true;
                     break;
                 }
             }
             if(merged)
                 r.remove(log);
         }
 
         return new CVSChangeLogSet(build,r);
     }
 
     /**
      * In-memory representation of CVS Changelog.
      */
     public static class CVSChangeLog extends ChangeLogSet.Entry {
         private String date;
         private String time;
         private User author;
         private String msg;
         private final List<File> files = new ArrayList<File>();
 
         /**
          * Checks if two {@link CVSChangeLog} entries can be merged.
          * This is to work around the duplicate entry problems.
          */
         public boolean canBeMergedWith(CVSChangeLog that) {
             if(!this.date.equals(that.date))
                 return false;
             if(!this.time.equals(that.time))    // TODO: perhaps check this loosely?
                 return false;
             if(this.author==null || that.author==null || !this.author.equals(that.author))
                 return false;
             if(!this.msg.equals(that.msg))
                 return false;
             return true;
         }
 
         public void merge(CVSChangeLog that) {
             this.files.addAll(that.files);
         }
 
         public String getDate() {
             return date;
         }
 
         public void setDate(String date) {
             this.date = date;
         }
 
         public String getTime() {
             return time;
         }
 
         public void setTime(String time) {
             this.time = time;
         }
 
         public User getAuthor() {
             return author;
         }
 
         public Collection<String> getAffectedPaths() {
             return new AbstractList<String>() {
                 public String get(int index) {
                     return files.get(index).getName();
                 }
 
                 public int size() {
                     return files.size();
                 }
             };
         }
 
         public void setUser(String author) {
             this.author = User.get(author);
         }
 
         public String getUser() {// digester wants read/write property, even though it never reads. Duh.
             return author.getDisplayName();
         }
 
         public String getMsg() {
             return msg;
         }
 
         public void setMsg(String msg) {
             this.msg = msg;
         }
 
         public void addFile( File f ) {
             f.parent = this;
             files.add(f);
         }
 
         public List<File> getFiles() {
             return files;
         }
     }
 
     public static class File {
         private String name;
         private String fullName;
         private String revision;
         private String prevrevision;
         private boolean dead;
         private CVSChangeLog parent;
 
         /**
          * Gets the path name in the CVS repository, like
          * "foo/bar/zot.c"
          *
          * <p>
          * The path is relative to the workspace root.
          */
         public String getName() {
             return name;
         }
 
         /**
          * Gets the full path name in the CVS repository,
          * like "/module/foo/bar/zot.c"
          *
          * <p>
          * Unlike {@link #getName()}, this method returns
          * a full name from the root of the CVS repository.
          */
         public String getFullName() {
             if(fullName==null) {
                 // Hudson < 1.91 doesn't record full path name for CVS,
                 // so try to infer that from the current CVS setting.
                 // This is an approximation since the config could have changed
                 // since this build has done.
                 SCM scm = parent.getParent().build.getProject().getScm();
                 if(scm instanceof CVSSCM) {
                     CVSSCM cvsscm = (CVSSCM) scm;
                     if(cvsscm.isFlatten()) {
                         fullName = '/'+cvsscm.getAllModules()+'/'+name;
                     } else {
                         // multi-module set up.
                         fullName = '/'+name;
                     }
                 } else {
                     // no way to infer.
                     fullName = '/'+name;
                 }
             }
             return fullName;
         }
 
         public void setFullName(String fullName) {
             this.fullName = fullName;
         }
 
         /**
          * Gets just the last component of the path, like "zot.c"
          */
         public String getSimpleName() {
             int idx = name.lastIndexOf('/');
             if(idx>0)   return name.substring(idx+1);
             return name;
         }
 
         public void setName(String name) {
             this.name = name;
         }
 
         public String getRevision() {
             return revision;
         }
 
         public void setRevision(String revision) {
             this.revision = revision;
         }
 
         public String getPrevrevision() {
             return prevrevision;
         }
 
         public void setPrevrevision(String prevrevision) {
             this.prevrevision = prevrevision;
         }
 
         public boolean isDead() {
             return dead;
         }
 
         public void setDead() {
             this.dead = true;
         }
 
         public EditType getEditType() {
             // see issue #73. Can't do much better right now
             if(dead)
                 return EditType.DELETE;
             if(revision.equals("1.1"))
                 return EditType.ADD;
             return EditType.EDIT;
         }
 
         public CVSChangeLog getParent() {
             return parent;
         }
     }
 
     /**
      * Represents CVS revision number like "1.5.3.2". Immutable.
      */
     public static class Revision {
         public final int[] numbers;
 
         public Revision(int[] numbers) {
             this.numbers = numbers;
             assert numbers.length%2==0;
         }
 
         public Revision(String s) {
             String[] tokens = s.split("\\.");
             numbers = new int[tokens.length];
             for( int i=0; i<tokens.length; i++ )
                 numbers[i] = Integer.parseInt(tokens[i]);
             assert numbers.length%2==0;
         }
 
         /**
          * Returns a new {@link Revision} that represents the previous revision.
          *
          * For example, "1.5"->"1.4", "1.5.2.13"->"1.5.2.12", "1.5.2.1"->"1.5"
          *
          * @return
          *      null if there's no previous version, meaning this is "1.1"
          */
         public Revision getPrevious() {
             if(numbers[numbers.length-1]==1) {
                 // x.y.z.1 => x.y
                 int[] p = new int[numbers.length-2];
                 System.arraycopy(numbers,0,p,0,p.length);
                 if(p.length==0)     return null;
                 return new Revision(p);
             }
 
             int[] p = numbers.clone();
             p[p.length-1]--;
 
             return new Revision(p);
         }
 
         public String toString() {
             StringBuilder buf = new StringBuilder();
             for (int n : numbers) {
                 if(buf.length()>0)  buf.append('.');
                 buf.append(n);
             }
             return buf.toString();
         }
     }
 }
