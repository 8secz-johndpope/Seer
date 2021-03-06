 /*
  * ====================================================================
  * Copyright (c) 2008 JavaGit Project.  All rights reserved.
  *
  * This software is licensed using the GNU LGPL v2.1 license.  A copy
  * of the license is included with the distribution of this source
  * code in the LICENSE.txt file.  The text of the license can also
  * be obtained at:
  *
  *   http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
  *
  * For more information on the JavaGit project, see:
  *
  *   http://www.javagit.com
  * ====================================================================
  */
 package edu.nyu.cs.javagit.api;
 
 import java.util.List;
 import java.util.ArrayList;
 import java.io.File;
 import java.io.IOException;
 
 /**
  * <code>GitDirectory</code> represents a directory object in a git working tree.
  */
 public class GitDirectory extends GitFileSystemObject {
   /**
    * The constructor.  Both arguments are required (i.e. cannot be null).
    * 
    * @param dir
    *          The underlying {@link java.io.File} object that we want to augment with git
    *          functionality.
    * @param workingTree
    *          The <code>WorkingTree</code> that this directory falls under.
    * 
    */
   protected GitDirectory(File dir, WorkingTree workingTree) {
     super(dir, workingTree);
   }
   
   @Override
   public boolean equals(Object obj) {
     if (!(obj instanceof GitDirectory)) {
       return false;
     }
 
     GitFileSystemObject gitObj = (GitFileSystemObject) obj;
     return super.equals(gitObj);
   }
 
   /**
    * Gets the children of this directory.
    * 
    * @return The children of this directory.
    */
   public List<GitFileSystemObject> getChildren() throws IOException {
     List<GitFileSystemObject> children = new ArrayList<GitFileSystemObject>();
 
     // get all of the file system objects currently located under this directory
     for (File memberFile : file.listFiles()) {
      // check if this file is hidden
      if (memberFile.isHidden()) {
         // ignore (could be .git directory)
         continue;
       }
 
       // now, just check for the type of the filesystem object
       if (memberFile.isDirectory()) {
         children.add(new GitDirectory(memberFile, workingTree));
       } else {
         children.add(new GitFile(memberFile, workingTree));
       }
     }
 
     return children;
   }
 }
