 package org.davinci.server;
 
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.net.MalformedURLException;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.net.URL;
 import java.net.URLConnection;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Vector;
 
 import org.apache.commons.io.filefilter.IOFileFilter;
 import org.apache.commons.io.filefilter.NameFileFilter;
 import org.apache.commons.io.filefilter.SuffixFileFilter;
 import org.davinci.ajaxLibrary.Library;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.Path;
 
 public class VLibraryResource implements IVResource {
     // abstracted file/resource class
     private URL            resourcePointer = null;
     private boolean        isWorkingCopy;
 
     private Library        library;
     private IVResource     parent;
     private String         name;
     private String bundleRoot;
 	private IVResource[] _parents;
 	private IVResource[] _files;
 
     public VLibraryResource(Library b, URL file, IVResource parent, String name, String bundleRoot) {
         this.resourcePointer = file;
 
         this.isWorkingCopy = false;
         this.library = b;
         this.parent = parent;
         this.name = name;
         this.bundleRoot = bundleRoot;
     }
 
     public boolean exists() {
         return true;
     }
 
     public String toString() {
         return this.getPath();
     }
 
     public URLConnection openConnection() throws MalformedURLException, IOException {
         return this.resourcePointer.openConnection();
     }
 
     public String getPath() {
     	if(this.parent==null)
     		return this.name;
     	
    	 return new Path(this.parent.getPath()).append(this.name).toString();
       /*        return this.parent.getPath() +  IVResource.SEPERATOR + this.name;
         if (name != null && name.length() > 0 && name.charAt(name.length() - 1) == '/') {
             name = name.substring(0, name.length() - 1);
         }
         if (name != null && name.length() > 0 && name.charAt(0) == '.') {
             name = name.substring(1);
         }
         if (name != null && name.length() > 0 && name.charAt(0) == '/') {
             name = name.substring(1);
         }
         */
         
     }
 
     public void createNewInstance() throws IOException {
         File f = new File(this.getPath());
         this.resourcePointer = f.toURL();
     }
 
     public boolean delete() {
         // TODO Auto-generated method stub
         return false;
     }
 
     public OutputStream getOutputStreem() throws FileNotFoundException {
         // TODO Auto-generated method stub
         return null;
 
     }
 
     public InputStream getInputStreem() throws IOException {
         // TODO Auto-generated method stub
         return this.resourcePointer.openStream();
     }
 
     public String getName() {
         return this.name;
         /*
          * String name = this.virtualPath; if(name.length() > 0 &&
          * name.charAt(name.length()-1) == '/') name =
          * name.substring(0,name.length()-1); return name;
          */
     }
 
     public URI getURI() throws URISyntaxException {
         // TODO Auto-generated method stub
         return this.resourcePointer.toURI();
     }
 
     public boolean isDirectory() {
         String path = this.resourcePointer.getPath();
         return path.endsWith("/");
     }
 
     public IVResource[] listFiles() {
     	if(this._files==null){
     		this._files = listFiles("*", false);
     	}
     	return this._files;
     	
     }
     
     public IVResource[] listFiles(String name, boolean recurse) {
     
 	    	IPath p1 = new Path(this.bundleRoot);
 	        String path = p1.append(name).toString();
 	        URL[] files = this.library.find(path, recurse);
 	        ArrayList found = new ArrayList();
 	        for (int i = 0; i < files.length; i++) {
 	            if (files[i] == null) {
 	                continue;
 	            }
 	            IPath myPath = new Path(this.resourcePointer.getPath());
 	            IPath itemPath = new Path(files[i].getPath());
 	            IPath newPath = itemPath.removeFirstSegments(myPath.matchingFirstSegments(itemPath));
 	            IVResource item = null;
 	            
 	            String[] pathSplit = newPath.segments();
 	            
 	            IVResource parent = this;
 	            IVResource lastParent = null;
 	            for(int k=0;k<pathSplit.length-1;k++){
 	            	lastParent = parent;
 	            	parent = this.get(pathSplit[k]);
 	            	if(parent==null)
 	            		parent = new VDirectory(lastParent, pathSplit[k]);
 	            }
 	            	
 	            String itemName = pathSplit[pathSplit.length-1];
 	            item = new VLibraryResource(this.library, files[i], parent, itemName, new Path(this.bundleRoot).append(itemName).toString());
 	            found.add(item);
 	
 	        }
     	
 	        return (IVResource[]) found.toArray(new IVResource[found.size()]);
     
     }
 
     public boolean mkdir() {
         // TODO Auto-generated method stub
         return false;
     }
 
     public boolean isFile() {
         return !isDirectory();
     }
 
     public IVResource[] find(String path) {
         // TODO Auto-generated method stub
         return new IVResource[] { get(path) };
     }
 
     public void flushWorkingCopy() {
         // TODO Auto-generated method stub
 
     }
 
     public boolean isDirty() {
         // TODO Auto-generated method stub
         return false;
     }
 
     public void removeWorkingCopy() {
         // TODO Auto-generated method stub
 
     }
 
     public IVResource getParent() {
         // TODO Auto-generated method stub
         return this.parent;
     }
 
     public IVResource[] getParents() {
     	
     	if(this._parents==null){
     	
 	        Vector parents = new Vector();
 	
 	        IVResource parent = this.getParent();
 	        while (parent != null) {
 	            parents.add(0, parent);
 	            parent = parent.getParent();
 	        }
 	        if (this.isDirectory()) {
 	            parents.add(this);
 	        }
 	       this._parents= (IVResource[]) parents.toArray(new IVResource[parents.size()]);
 	   	}
     	return this._parents;
     }
 
     public IVResource create(String path) {
         // TODO Auto-generated method stub
         return null;
     }
 
     public void add(IVResource v) {
         // TODO Auto-generated method stub
 
     }
 
    
     public IVResource get(String childName) {
         IPath p1 = new Path(this.bundleRoot);
         // p1 = p1.removeFirstSegments(new
         // Path(virtualRoot).matchingFirstSegments(p1));
         URL[] files = this.library.find(p1.append(childName).toString(), false);
         for (int i = 0; i < files.length; i++) {
             IPath myPath = new Path(this.resourcePointer.getPath());
             IPath itemPath = new Path(files[i].getPath());
             IPath newPath = itemPath.removeFirstSegments(myPath.matchingFirstSegments(itemPath));
             IVResource item = new VLibraryResource(this.library, files[i], this, newPath.removeTrailingSeparator().toString(), new Path(this.bundleRoot)
                     .append(newPath).toString());
 
             if (item != null) {
                 return item;
             }
         }
         return null;
     }
 
     public String getLibraryVersion() {
         return this.library.getVersion();
     }
 
     public String getLibraryId() {
         return this.library.getID();
     }
 
     public boolean committed() {
         return true;
     }
 
     public boolean readOnly() {
         // TODO Auto-generated method stub
         return true;
     }
 
     public IVResource[] findChildren(String childName) {
         IVResource[] children = this.listFiles(childName, true);
 
         return children;
         
         /*
         Path path = new Path(childName);
         IOFileFilter filter;
         if (path.segment(0).equals("*")) {
             filter = new NameFileFilter(path.lastSegment());
         } else {
             String lastSegment = path.lastSegment();
             if (lastSegment.startsWith("*")) {
                 filter = new SuffixFileFilter(lastSegment.substring(1));
             } else {
                 filter = null;
             }
         }
         Vector results = new Vector();
 
         for (int i = 0; i < children.length; i++) {
             IVResource r1 = children[i];
             File f1 = new File(r1.getName());
             if (filter.accept(f1)) {
                 results.add(r1);
             }
 
             if (r1.isDirectory()) {
                 IVResource[] more = r1.findChildren(childName);
                 results.addAll(Arrays.asList(more));
             }
 
         }
 
         return (IVResource[]) results.toArray(new IVResource[results.size()]);
 		*/
     }
 }
