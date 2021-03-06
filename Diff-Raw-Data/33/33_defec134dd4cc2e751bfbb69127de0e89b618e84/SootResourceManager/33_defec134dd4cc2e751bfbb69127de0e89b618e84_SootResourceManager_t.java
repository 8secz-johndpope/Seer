 /*
  * Created on 20-Mar-2003
  *
  * To change the template for this generated file go to
  * Window>Preferences>Java>Code Generation>Code and Comments
  */
 package ca.mcgill.sable.soot.attributes;
 
 import java.util.*;
 
 import org.eclipse.core.resources.*;
 import org.eclipse.core.runtime.CoreException;
 
 /**
  * @author jlhotak
  *
  * This library is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 2.1 of the License, or (at your option) any later version.
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this library; if not, write to the
  * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
  * Boston, MA 02111-1307, USA.
  */
 
 public class SootResourceManager implements IResourceChangeListener {
 
 	private static final String JAVA_FILE_EXT = "java";
 	private static final String JIMPLE_FILE_EXT = "jimple";
 	private static final int UPDATE_BIT = 0;
 	private static final int REMOVE_BIT = 1;
 	
 	private HashMap projects;
 	
 	public SootResourceManager() {
 		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
 	}
 	
 	public void initialize() {
 		setProjects(new HashMap());
 		IProject [] projs = ResourcesPlugin.getWorkspace().getRoot().getProjects();
 		for (int i = 0; i < projs.length; i++){
			System.out.println("adding to ResourceManger project: "+projs[i].getFullPath().toOSString());
 			getProjects().put(projs[i], getFilesForContainer(projs[i]));
 		}
 		
 	}
 	
 	public HashMap getFilesForContainer(IContainer parent) {
 		HashMap list = new HashMap();
 		try {
 			IResource [] members = parent.members();
 			for (int i = 0; i < members.length; i++){
 				
 				//System.out.println(members[i].getFullPath().toOSString()+" and ext: "+members[i].getFileExtension());
 				if (members[i] instanceof IFolder) {
 					
 					list.putAll(getFilesForContainer((IFolder)members[i]));
 				}
 				else if (members[i] instanceof IFile){
 					
 					IFile file = (IFile)members[i];
					if (file.getFileExtension() == null){
					}
					else if ((file.getFileExtension().equals(JAVA_FILE_EXT)) ||
 						(file.getFileExtension().equals(JIMPLE_FILE_EXT))){
 							//System.out.println("adding to Resource Manager file: "+file.getFullPath().toOSString());
 							list.put(file, new BitSet(2));
 						}
 				}
 			}
 		}
 		catch(CoreException e){
 			
 		}
 		return list;
 		
 	}
 	
 	public void updateSootRanFlag(){
 		//System.out.println("updating update bit");
 		if (getProjects() == null) return;
 		
 		Iterator it = getProjects().keySet().iterator();
 		while (it.hasNext()){
 			IProject proj = (IProject)it.next();
 			//System.out.println("updating update bit for proj: "+proj.getFullPath().toOSString());
 			HashMap files = (HashMap)getProjects().get(proj);
 			
 			if (files == null) break;
 			//System.out.println(files.keySet());
 			
 			Iterator fileIt = files.keySet().iterator();
 			
 			while (fileIt.hasNext()){
 				Object obj = fileIt.next();
 				//System.out.println(obj.getClass().toString());
 				IFile file = (IFile)obj;
 				//System.out.println("updating update bit for file: "+file.getFullPath().toOSString());
 				((BitSet)files.get(file)).set(UPDATE_BIT);
 			}
 		}
 	}
 	
 	public void updateFileChangedFlag(IFile file){
 		//System.out.println("updating remove bit");
 		if ((file.getFileExtension().equals(JAVA_FILE_EXT)) ||
 			(file.getFileExtension().equals(JIMPLE_FILE_EXT))){
 			HashMap files = (HashMap)getProjects().get(file.getProject());
 			if (files == null) return;
 			if (files.get(file) == null) return;
 			((BitSet)files.get(file)).set(REMOVE_BIT);
 			}
 
 	}
 	
 	public boolean isFileMarkersUpdate(IFile file){
 		//System.out.println("projects: "+getProjects());
 		HashMap files = (HashMap)getProjects().get(file.getProject());
 		//System.out.println(file.getFullPath().toOSString());
 		//System.out.println("files: "+files);
 		if (files == null) return false;
 		if (files.get(file) == null) return false;
 		return ((BitSet)files.get(file)).get(UPDATE_BIT);
 	}
 	
 	public void setToFalseUpdate(IFile file){
 		HashMap files = (HashMap)getProjects().get(file.getProject());
 		//System.out.println(file.getFullPath().toOSString());
 		//System.out.println("files: "+files);
 		if (files == null) return;
 		if (files.get(file) == null) return;
 		((BitSet)files.get(file)).clear(UPDATE_BIT);	
 	}
 	
 	public void setToFalseRemove(IFile file){
 		HashMap files = (HashMap)getProjects().get(file.getProject());
 		//System.out.println(file.getFullPath().toOSString());
 		//System.out.println("files: "+files);
 		if (files == null) return;
 		if (files.get(file) == null) return;
 		((BitSet)files.get(file)).clear(REMOVE_BIT);
 	}
 	
 	public boolean isFileMarkersRemove(IFile file){
 		//System.out.println("projects: "+getProjects());
 		HashMap files = (HashMap)getProjects().get(file.getProject());
 		//System.out.println(file.getFullPath().toOSString());
 		//System.out.println("files: "+files);
 		if (files == null) return false;
 		if (files.get(file) == null) return false;
 		return ((BitSet)files.get(file)).get(REMOVE_BIT);
 	}
 	
 	public void addToLists(IResource res){
 		if (res instanceof IProject){
 			getProjects().put((IProject)res, getFilesForContainer((IProject)res));
 		}
 		else if (res instanceof IFile){
 			IProject proj = res.getProject();
 			IFile file = (IFile)res;
 			if ((file.getFileExtension() == JAVA_FILE_EXT) ||
 			 	(file.getFileExtension() == JIMPLE_FILE_EXT)){
 						
 				((HashMap)getProjects().get(proj)).put(file, new BitSet(2));
 			 		System.out.println("added file: "+file.getFullPath().toOSString());
 			 	}
 		}
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
 	 */
 	public void resourceChanged(IResourceChangeEvent event) {
 		// TODO Auto-generated method stub
 		//System.out.println("event: "+event.getType());
 		switch(event.getType()){
 			/*case IResourceChangeEvent.POST_AUTO_BUILD:{
 				System.out.println("post auto build event");
 				break;
 			}*/
 			case IResourceChangeEvent.POST_CHANGE:{
 				//System.out.println("post change event");
 				try {
 					event.getDelta().accept(new SootDeltaVisitor());
 				}
 					catch (CoreException e){ 
 				}
 				break;
 			}
 		}
 
 	}
 	
 
 	/**
 	 * @return
 	 */
 	public HashMap getProjects() {
 		return projects;
 	}
 
 	/**
 	 * @param map
 	 */
 	public void setProjects(HashMap map) {
 		projects = map;
 	}
 
 }
