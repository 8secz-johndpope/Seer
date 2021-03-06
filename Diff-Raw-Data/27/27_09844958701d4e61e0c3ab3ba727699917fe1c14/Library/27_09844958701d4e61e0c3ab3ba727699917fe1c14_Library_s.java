 /* -*- tab-width: 4 -*-
  *
  * Electric(tm) VLSI Design System
  *
  * File: Library.java
  *
  * Copyright (c) 2003 Sun Microsystems and Static Free Software
  *
  * Electric(tm) is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * Electric(tm) is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with Electric(tm); see the file COPYING.  If not, write to
  * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  * Boston, Mass 02111-1307, USA.
  */
 package com.sun.electric.database.hierarchy;
 
 import com.sun.electric.database.change.Undo;
 import com.sun.electric.database.prototype.NodeProto;
 import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.Name;
 import com.sun.electric.database.text.Pref;
 import com.sun.electric.database.text.TextUtils;
 import com.sun.electric.database.text.Version;
 import com.sun.electric.database.topology.NodeInst;
 import com.sun.electric.database.variable.ElectricObject;
 import com.sun.electric.database.variable.Variable;
 import com.sun.electric.tool.user.ActivityLogger;
 import com.sun.electric.tool.user.ErrorLogger;
 import com.sun.electric.tool.user.ui.TopLevel;
 
 import java.net.URL;
 import java.util.ArrayList;
import java.util.Collections;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.TreeMap;
 import java.util.TreeSet;
 import java.util.prefs.Preferences;
 
 import javax.swing.JOptionPane;
 
 /**
  * A Library represents a collection of Cells.
  * To find of a Library, you use Electric.getLibrary(String name).
  * Use Electric.newLibrary(String name) to create a new library, or
  * Electric.getCurrent() to get the current Library.
  * <p>
  * Once you have a Library, you can create a new Cell in it, find an existing
  * Cell, get an Enumeration of all Cells, or find the Cell that the user
  * is currently editing.
  */
 public class Library extends ElectricObject implements Comparable/*<Library>*/
 {
 	/** key of Variable holding font associations. */		public static final Variable.Key FONT_ASSOCIATIONS = ElectricObject.newKey("LIB_font_associations");
 
 	// ------------------------ private data ------------------------------
 
 	/** library has changed significantly */				private static final int LIBCHANGEDMAJOR =           01;
 //	/** set to see library in explorer */					private static final int OPENINEXPLORER =            02;
 	/** set if library came from disk */					private static final int READFROMDISK =              04;
 //	/** internal units in library (see INTERNALUNITS) */	private static final int LIBUNITS =                 070;
 //	/** right shift for LIBUNITS */							private static final int LIBUNITSSH =                 3;
 	/** library has changed insignificantly */				private static final int LIBCHANGEDMINOR =         0100;
 	/** library is "hidden" (clipboard library) */			private static final int HIDDENLIBRARY =           0200;
 //	/** library is unwanted (used during input) */			private static final int UNWANTEDLIB =             0400;
 
 	/** name of this library  */							private String libName;
 	/** file location of this library */					private URL libFile;
 	/** version of Electric which wrote the library. */		private Version version;
 	/** list of Cells in this library */					/*private*/ TreeMap/*<CellName,Cell>*/ cells = new TreeMap/*<CellName,Cell>*/();
 	/** Preference for cell currently being edited */		private Pref curCellPref;
 	/** flag bits */										private int userBits;
     /** list of referenced libs */                          private List/*<Library>*/ referencedLibs;
 	/** preferences for all libraries */					private static Preferences prefs = null;
 
 	/** static list of all libraries in Electric */			private static TreeMap/*<String,Library>*/ libraries = new TreeMap/*<String,Library>*/(TextUtils.STRING_NUMBER_ORDER);
 	/** static set of all linked libraries and cells */		static HashSet/*<Object>*/ databaseObjs = new HashSet/*<Object>*/();
 	/** the current library in Electric */					private static Library curLib = null;
 
 	// ----------------- private and protected methods --------------------
 
 	/**
 	 * The constructor is never called.  Use the factor method "newInstance" instead.
 	 */
 	private Library()
 	{
 		if (prefs == null) prefs = Preferences.userNodeForPackage(getClass());
 	}
 
 	/**
 	 * This method is a factory to create new libraries.
 	 * A Library has both a name and a file.
 	 * @param libName the name of the library (for example, "gates").
 	 * Library names must be unique, and they must not contain spaces.
 	 * @param libFile the URL to the disk file (for example "/home/strubin/gates.elib").
 	 * If the Library is being created, the libFile can be null.
 	 * If the Library file is given and it points to an existing file, then the I/O system
 	 * can be told to read that file and populate the Library.
 	 * @return the Library object.
 	 */
 	public static Library newInstance(String libName, URL libFile)
 	{
 		// make sure the name is legal
         if (libName == null || libName.equals("")) {
             System.out.println("Error: '"+libName+"' is not a valid name");
             return null;
         }
 		String legalName = libName.replace(' ', '-').replace(':', '-');
 		if (!legalName.equals(libName))
 			System.out.println("Warning: library '" + libName + "' renamed to '" + legalName + "'");
 		
 		// see if the library name already exists
 		Library existingLibrary = Library.findLibrary(legalName);
 		if (existingLibrary != null)
 		{
 			System.out.println("Error: library '" + legalName + "' already exists");
 			return existingLibrary;
 		}
 		
 		// create the library
 		Library lib = new Library();
 		lib.curCellPref = null;
 		lib.libName = legalName;
 		lib.libFile = libFile;
         lib.referencedLibs = new ArrayList/*<Library>*/();
 
 		// add the library to the global list
 		synchronized (libraries)
 		{
 			libraries.put(legalName, lib);
 			databaseObjs.add(lib);
 		}
 
         // always broadcast library changes
 //        Undo.setNextChangeQuiet(false);
         Undo.newObject(lib);
 		return lib;
 	}
 
 	/**
 	 * Method to delete this Library.
 	 * @param reason the reason for deleting this library (replacement or deletion).
 	 * @return true if the library was deleted.
 	 * Returns false on error.
 	 */
 	public boolean kill(String reason)
 	{
 		if (!isLinked())
 		{
 			System.out.println("Library already killed");
 			return false;
 		}
 		// cannot delete the current library
 		Library newCurLib = null;
 		if (curLib == this)
 		{
 			// find another library
 			/*for(Library lib: libraries)*/
 			for (Iterator it = libraries.values().iterator(); it.hasNext(); )
 			{
 				Library lib = (Library)it.next();
 				if (lib == curLib) continue;
 				if (lib.isHidden()) continue;
 				newCurLib = lib;
 				break;
 			}
 			if (newCurLib == null)
 			{
 				System.out.println("Cannot delete the last library");
 				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), "Cannot delete the last "+toString(),
 					"Close library", JOptionPane.INFORMATION_MESSAGE);
 				return false;
 			}
 		}
 
 		// make sure it is in the list of libraries
 		if (libraries.get(libName) != this)
 		{
 			System.out.println("Cannot delete library " + this);
 			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), "Cannot delete "+toString(),
 				"Close library", JOptionPane.ERROR_MESSAGE);
 			return false;
 		}
 
 		// make sure none of these cells are referenced by other libraries
 		boolean referenced = false;
 		/*for(Library lib: libraries)*/
 		for (Iterator it = libraries.values().iterator(); it.hasNext(); )
 		{
 			Library lib = (Library)it.next();
 			if (lib == this) continue;
 			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
 			{
 				Cell cell = (Cell)cIt.next();
 				for(Iterator nIt = cell.getNodes(); nIt.hasNext(); )
 				{
 					NodeInst ni = (NodeInst)nIt.next();
 					if (ni.getProto() instanceof Cell)
 					{
 						Cell subCell = (Cell)ni.getProto();
 						if (subCell.getLibrary() == this)
 						{
 							JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
								"Cannot " + reason + " library " + getName() + " because one of its cells (" +
								subCell.describe() + ") is being used (by " + cell.libDescribe() + ")",
 								"Close library", JOptionPane.ERROR_MESSAGE);
 							 referenced = true;
 							 break;
 						}
 					}
 				}
 				if (referenced) break;
 			}
 			if (referenced) break;
 		}
 		if (referenced) return false;
 
 		// remove all cells in the library
 		erase();
 
 		// remove it from the list of libraries
 		synchronized (libraries)
 		{
 			libraries.remove(libName);
 			databaseObjs.remove(this);
 		}
 
 		// set the new current library if appropriate
 		if (newCurLib != null) newCurLib.setCurrent();
 
 		// always broadcast library changes
 //		Undo.setNextChangeQuiet(false);
 		Undo.killObject(this);
 		return true;
 	}
 
 	/**
 	 * Method to remove all contents from this Library.
 	 */
 	public void erase()
 	{
 		// remove all cells in the library
         for (Iterator it = getCells(); it.hasNext(); ) {
             Cell c = (Cell)it.next();
             c.kill();
         }
 		cells.clear();
 	}
 
 	/**
 	 * Method to add a Cell to this Library.
 	 * @param c the Cell to add.
 	 */
 	void addCell(Cell c)
 	{
 		CellName cn = c.getCellName();
 		// sanity check: make sure Cell isn't already in the list
         synchronized (cells)
         {
             if (cells.containsKey(cn))
             {
                 System.out.println("Tried to re-add a cell to a library: " + c);
                 return;
             }
 			cells.put(cn, c);
 		}
 	}
 
 	/**
 	 * Method to remove a Cell from this Library.
 	 * @param c the Cell to remove.
 	 */
 	void removeCell(Cell c)
 	{
 		CellName cn = c.getCellName();
 		// sanity check: make sure Cell is in the list
         synchronized (cells)
         {
             if (cells.get(cn) != c)
             {
                 System.out.println("Tried to remove a non-existant Cell from a library: " + c);
                 return;
             }
 			cells.remove(cn);
 		}
 	}
 
     /**
      * Adds lib as a referenced library. This also checks the dependency
      * would create a circular dependency, in which case the reference is
      * not logged, and the method returns a LibraryDependency object.
      * If everything is ok, the method returns null.
      * @param lib the library to be added as a referenced lib
      * @return null if ok, a LibraryDependency object if this would create circular dependency
      */
     LibraryDependency addReferencedLib(Library lib) {
         synchronized(referencedLibs) {
             if (referencedLibs.contains(lib)) return null;          // already a referenced lib, is ok
         }
         // check recursively if there is a circular dependency
         List libDependencies = new ArrayList();
         if (lib.isReferencedLib(this, libDependencies)) {
             // there is a dependency
 
             // trace the dependency (this is an expensive operation)
             LibraryDependency d = new LibraryDependency();
             d.startLib = lib;
             d.finalRefLib = this;
             Library startLib = lib;
             for (Iterator itLib = libDependencies.iterator(); itLib.hasNext(); ) {
 
                 // startLib references refLib. Find out why
                 Library refLib = (Library)itLib.next();
                 boolean found = false;
                 // find a cell instance that creates dependency
                 for (Iterator itCell = startLib.getCells(); itCell.hasNext(); ) {
                     Cell c = (Cell)itCell.next();
                     for (Iterator it = c.getNodes(); it.hasNext(); ) {
                         NodeInst ni = (NodeInst)it.next();
                         NodeProto np = ni.getProto();
                         if (np instanceof Cell) {
                             Cell cc = (Cell)np;
                             if (cc.getLibrary() == refLib) {
                                 d.dependencies.add(c);
                                 d.dependencies.add(cc);
                                 found = true;
                                 break;
                             }
                         }
                     }
                     if (found) break;
                 }
                 if (!found) System.out.println("ERROR: Library.addReferencedLib dependency trace failed inexplicably");
                 startLib = refLib;
             }
 
             return d;
         }
 
         // would not create circular dependency, add and return
         synchronized(referencedLibs) {
             referencedLibs.add(lib);
         }
         return null;
     }
 
     /**
      * Try to remove lib as a referenced library. If it is no longer referenced,
      * it is removed as a reference library.
      * @param lib the reference library that may no longer be referenced
      */
     void removeReferencedLib(Library lib) {
         if (lib == this) return;            // we don't store references to self
         synchronized(referencedLibs) {
             assert(referencedLibs.contains(lib));
         }
         boolean refFound = false;
         for (Iterator itCell = getCells(); itCell.hasNext(); ) {
             Cell c = (Cell)itCell.next();
             for (Iterator it = c.getNodes(); it.hasNext(); ) {
                 NodeInst ni = (NodeInst)it.next();
                 NodeProto np = ni.getProto();
                 if (np instanceof Cell) {
                     Cell cc = (Cell)np;
                     if (cc.getLibrary() == lib) {
                         refFound = true;
                         break;
                     }
                 }
             }
             if (refFound) break;
         }
         if (!refFound) {
             // no instance that would create reference found, ok to remove reference
             synchronized(referencedLibs) {
                 referencedLibs.remove(lib);
             }
         }
     }
 
     /**
      * Returns true if this Library directly references the specified Library 'lib'.
      * It does not return true if the library is referenced through another library.
      * @param lib the possible referenced library
      * @return true if the library is directly referenced by this library, false otherwise
      */
     public boolean referencesLib(Library lib) {
         synchronized(referencedLibs) {
             if (referencedLibs.contains(lib))
                 return true;
         }
         return false;
     }
 
     /**
      * Checks to see if lib is referenced by this library, or by
      * any libraries this library references, recursively.
      * @param lib the lib to check if it is a referenced lib
      * @return true if it is through any number of references, false otherwise
      */
     private boolean isReferencedLib(Library lib, List libDepedencies) {
         List reflibsCopy = new ArrayList();
         synchronized(referencedLibs) {
             if (referencedLibs.contains(lib)) {
                 libDepedencies.add(lib);
                 return true;
             }
             reflibsCopy.addAll(referencedLibs);
         }
         for (Iterator it = reflibsCopy.iterator(); it.hasNext(); ) {
             Library reflib = (Library)it.next();
 
             // if reflib already in dependency list, ignore
             if (libDepedencies.contains(reflib)) continue;
 
             // check recursively
             libDepedencies.add(reflib);
             if (reflib.isReferencedLib(lib, libDepedencies)) return true;
             // remove reflib in accumulated list, try again
             libDepedencies.remove(reflib);
         }
         return false;
     }
 
     static class LibraryDependency {
         private List dependencies;
         private Library startLib;
         private Library finalRefLib;
 
         private LibraryDependency() {
             dependencies = new ArrayList();
         }
 
         public String toString() {
             StringBuffer buf = new StringBuffer();
             buf.append("Library " + startLib.getName() + " depends on Library " + finalRefLib.getName() + " through the following references:\n");
             for (Iterator it = dependencies.iterator(); it.hasNext(); ) {
                 Cell libCell = (Cell)it.next();
                 Cell instance = (Cell)it.next();
                 buf.append("   " + libCell.libDescribe() + " instantiates " + instance.libDescribe() + "\n");
             }
             return buf.toString();
         }
     }
 
 	// ----------------- public interface --------------------
 
     /**
      * Returns true if this Library is linked into database.
      * @return true if this Library is linked into database.
      */
 	public boolean isLinked()
 	{
 		return databaseObjs.contains(this);
 	}
 
 	/**
 	 * Method to check and repair data structure errors in this Library.
 	 */
 	public int checkAndRepair(boolean repair, ErrorLogger errorLogger)
 	{
 		int errorCount = 0;
 		for(Iterator it = getCells(); it.hasNext(); )
 		{
 			Cell cell = (Cell)it.next();
 			errorCount += cell.checkAndRepair(repair, errorLogger);
 		}
 		if (errorCount != 0 && repair)
 		{
 			setChangedMajor();
 			setChangedMinor();
 		}
 		return errorCount;
 	}
 
 	/**
 	 * Method to check invariants in this Library.
 	 * @exception AssertionError if invariants are not valid
 	 */
 	private void check()
 	{
 		assert databaseObjs.contains(this);
 		assert libName != null;
 		assert libName.length() > 0;
 		assert libName.indexOf(' ') == -1 && libName.indexOf(':') == -1 : libName;
 		HashSet cellGroups = new HashSet();
 		String protoName = null;
 		Cell.CellGroup cellGroup = null;
 		for(Iterator it = cells.entrySet().iterator(); it.hasNext(); )
 		{
 			Map.Entry e = (Map.Entry)it.next();
 			CellName cn = (CellName)e.getKey();
 			Cell cell = (Cell)e.getValue();
 			assert cell.getCellName().equals(cn);
 			assert cell.getLibrary() == this;
 			if (protoName == null || !cell.getName().equals(protoName))
 			{
 				protoName = cell.getName();
 				cellGroup = cell.getCellGroup();
 				assert cellGroup != null : cell;
 				cellGroups.add(cellGroup);
 			}
 			assert cell.getCellGroup() == cellGroup : cell;
 			cell.check();
 		}
 		for (Iterator it = cellGroups.iterator(); it.hasNext(); )
 		{
 			cellGroup = (Cell.CellGroup)it.next();
 			cellGroup.check();
 		}
 	}
 
 	private static boolean invariantsFailed = false;
 
 	/**
 	 * Method to check invariants in all Libraries.
 	 * @return true if invariants are valid
 	 */
 	public static boolean checkInvariants()
 	{
 		try
 		{
 			//long startTime = System.currentTimeMillis();
 			TreeSet libNames = new TreeSet(String.CASE_INSENSITIVE_ORDER);
 			for (Iterator it = libraries.entrySet().iterator(); it.hasNext(); )
 			{
 				Map.Entry e = (Map.Entry)it.next();
 				String libName = (String)e.getKey();
 				Library lib = (Library)e.getValue();
 				assert libName.equals(lib.libName) : libName + " " + lib;
 				assert !libNames.contains(libName) : "case insensitive " + libName;
 				libNames.add(libName);
 				lib.check();
 			}
 			for (Iterator it = databaseObjs.iterator(); it.hasNext(); )
 			{
 				Object o = it.next();
 				Library lib;
 				if (o instanceof Cell)
 				{
 					Cell cell = (Cell)o;
 					lib = cell.getLibrary();
 					assert lib.contains(cell);
 				} else
 				{
 					lib = (Library)o;
 				}
 				assert libraries.get(lib.libName) == lib;
 			}
 			//long endTime = System.currentTimeMillis();
 			//float finalTime = (endTime - startTime) / 1000F;
 			//System.out.println("**** Check Invariants took " + finalTime + " seconds");
 			return true;
 		} catch (Throwable e)
 		{
 			if (!invariantsFailed)
 			{
 				System.out.println("Exception checking database invariants");
 				e.printStackTrace();
 				ActivityLogger.logException(e);
 				invariantsFailed = true;
 			}
 		}
 		return false;
 	}
 
 
 	/**
 	 * Method to indicate that this Library has changed in a major way.
 	 * Major changes include creation, deletion, or modification of circuit elements.
 	 */
 	public void setChangedMajor() { userBits |= LIBCHANGEDMAJOR; }
 
 	/**
 	 * Method to indicate that this Library has not changed in a major way.
 	 * Major changes include creation, deletion, or modification of circuit elements.
 	 */
 	public void clearChangedMajor() { userBits &= ~LIBCHANGEDMAJOR; }
 
 	/**
 	 * Method to return true if this Library has changed in a major way.
 	 * Major changes include creation, deletion, or modification of circuit elements.
 	 * @return true if this Library has changed in a major way.
 	 */
 	public boolean isChangedMajor() { return (userBits & LIBCHANGEDMAJOR) != 0; }
 
 	/**
 	 * Method to indicate that this Library has changed in a minor way.
 	 * Minor changes include changes to text and other things that are not essential to the circuitry.
 	 */
 	public void setChangedMinor() { userBits |= LIBCHANGEDMINOR; }
 
 	/**
 	 * Method to indicate that this Library has not changed in a minor way.
 	 * Minor changes include changes to text and other things that are not essential to the circuitry.
 	 */
 	public void clearChangedMinor() { userBits &= ~LIBCHANGEDMINOR; }
 
 	/**
 	 * Method to return true if this Library has changed in a minor way.
 	 * Minor changes include changes to text and other things that are not essential to the circuitry.
 	 * @return true if this Library has changed in a minor way.
 	 */
 	public boolean isChangedMinor() { return (userBits & LIBCHANGEDMINOR) != 0; }
 
 	/**
 	 * Method to indicate that this Library came from disk.
 	 * Libraries that come from disk are saved without a file-selection dialog.
 	 */
 	public void setFromDisk() { userBits |= READFROMDISK; }
 
 	/**
 	 * Method to indicate that this Library did not come from disk.
 	 * Libraries that come from disk are saved without a file-selection dialog.
 	 */
 	public void clearFromDisk() { userBits &= ~READFROMDISK; }
 
 	/**
 	 * Method to return true if this Library came from disk.
 	 * Libraries that come from disk are saved without a file-selection dialog.
 	 * @return true if this Library came from disk.
 	 */
 	public boolean isFromDisk() { return (userBits & READFROMDISK) != 0; }
 
 	/**
 	 * Method to indicate that this Library is hidden.
 	 * Hidden libraries are not seen by the user.
 	 * For example, the "clipboard" library is hidden because it is only used
 	 * internally for copying and pasting circuitry.
 	 */
 	public void setHidden() { userBits |= HIDDENLIBRARY; }
 
 	/**
 	 * Method to indicate that this Library is not hidden.
 	 * Hidden libraries are not seen by the user.
 	 * For example, the "clipboard" library is hidden because it is only used
 	 * internally for copying and pasting circuitry.
 	 */
 	public void clearHidden() { userBits &= ~HIDDENLIBRARY; }
 
 	/**
 	 * Method to return true if this Library is hidden.
 	 * Hidden libraries are not seen by the user.
 	 * For example, the "clipboard" library is hidden because it is only used
 	 * internally for copying and pasting circuitry.
 	 * @return true if this Library is hidden.
 	 */
 	public boolean isHidden() { return (userBits & HIDDENLIBRARY) != 0; }
 
 	/**
 	 * Method to return the current Library.
 	 * @return the current Library.
 	 */
 	public static Library getCurrent() { return curLib; }
 	
 	/**
 	 * Method to make this the current Library.
 	 */
 	public void setCurrent() { curLib = this; }
 	
 	/**
 	 * Low-level method to get the user bits.
 	 * The "user bits" are a collection of flags that are more sensibly accessed
 	 * through special methods.
 	 * This general access to the bits is required because the ELIB
 	 * file format stores it as a full integer.
 	 * This should not normally be called by any other part of the system.
 	 * @return the "user bits".
 	 */
 	public int lowLevelGetUserBits() { return userBits; }
 	
 	/**
 	 * Low-level method to set the user bits.
 	 * The "user bits" are a collection of flags that are more sensibly accessed
 	 * through special methods.
 	 * This general access to the bits is required because the ELIB
 	 * file format stores it as a full integer.
 	 * This should not normally be called by any other part of the system.
 	 * @param userBits the new "user bits".
 	 */
 	public void lowLevelSetUserBits(int userBits) { this.userBits = userBits; }
 
 	/**
 	 * Get list of cells contained in other libraries
 	 * that refer to cells contained in this library
 	 * @param elib to search for
 	 * @return list of cells refering to elements in this library
 	 */
 	public static Set findReferenceInCell(Library elib)
 	{
 		TreeSet list = new TreeSet();
 
 		/*for (Library l: libraries)*/
 		for (Iterator it = libraries.values().iterator(); it.hasNext(); )
 		{
 			Library l = (Library)it.next();
 			// skip itself
 			if (l == elib) continue;
 
 			for (Iterator cIt = l.cells.values().iterator(); cIt.hasNext(); )
 			{
 				Cell cell = (Cell) cIt.next();
 				cell.findReferenceInCell(elib, list);
 			}
 		}
 		return list;
 	}
 	/**
 	 * Method to find a Library with the specified name.
 	 * @param libName the name of the Library.
 	 * Note that this is the Library name, and not the Library file.
 	 * @return the Library, or null if there is no known Library by that name.
 	 */
 	public static Library findLibrary(String libName)
 	{
 		if (libName == null) return null;
 		Library lib = (Library)libraries.get(libName);
 		if (lib != null) return lib;
 
 		/*for (Library l: libraries)*/
 		for (Iterator it = libraries.values().iterator(); it.hasNext(); )
 		{
 			Library l = (Library)it.next();
 			if (l.getName().equalsIgnoreCase(libName))
 				return l;
 		}
 		return null;
 	}
 
 	/**
 	 * Method to return an iterator over all libraries.
 	 * @return an iterator over all libraries.
 	 */
 	public static Iterator/*<Library>*/ getLibraries()
 	{
         synchronized(libraries) {
 			ArrayList/*<Library>*/ librariesCopy = new ArrayList/*<Library>*/(libraries.values());
 			return librariesCopy.iterator();
         }
 	}
 
 	/**
 	 * Method to return the number of libraries.
 	 * @return the number of libraries.
 	 */
 	public static int getNumLibraries()
 	{
 		return libraries.size();
 	}
 
 	/**
 	 * Method to return an iterator over all visible libraries.
 	 * @return an iterator over all visible libraries.
 	 */
 	public static List/*<Library>*/ getVisibleLibraries()
 	{
         synchronized(libraries) {
 			ArrayList/*<Library>*/ visibleLibraries = new ArrayList/*<Library>*/();
 			/*for (Library lib: libraries)*/
 			for (Iterator it = libraries.values().iterator(); it.hasNext(); )
 			{
 				Library lib = (Library)it.next();
 				if (!lib.isHidden()) visibleLibraries.add(lib);
 			}
 			return visibleLibraries;
         }
 	}
 
 	/**
 	 * Method to return the name of this Library.
 	 * @return the name of this Library.
 	 */
 	public String getName() { return libName; }
 
 	/**
 	 * Method to set the name of this Library.
 	 * @param libName the new name of this Library.
 	 * @return false if the library was renamed.
 	 * True on error.
 	 */
 	public boolean setName(String libName)
 	{
 		if (this.libName.equals(libName)) return true;
 
 		// make sure the name is legal
         if (libName == null || libName.equals("") || libName.indexOf(' ') >= 0 || libName.indexOf(':') >= 0) {
             System.out.println("Error: '"+libName+"' is not a valid name");
             return true;
         }
 		Library already = findLibrary(libName);
 		if (already != null)
 		{
 			System.out.println("Already a library called " + already.getName());
 			return true;
 		}
 
 		String oldName = this.libName;
 		lowLevelRename(libName);
 		Undo.renameObject(this, oldName);
 		return false;
 	}
 
 	/**
 	 * Method to rename this Library.
 	 * This method is for low-level use by the database, and should not be called elsewhere.
 	 * @param libName the new name of the Library.
 	 */
 	public void lowLevelRename(String libName)
 	{
 		libraries.remove(this.libName);
 		this.libName = libName;
 		libraries.put(libName, this);
 
 		String newLibFile = TextUtils.getFilePath(libFile) + libName;
 		String extension = TextUtils.getExtension(libFile);
 		if (extension.length() > 0) newLibFile += "." + extension;
 		this.libFile = TextUtils.makeURLToFile(newLibFile);
 	}
 
 	/**
 	 * Method to return the URL of this Library.
 	 * @return the URL of this Library.
 	 */
 	public URL getLibFile() { return libFile; }
 
 	/**
 	 * Method to set the URL of this Library.
 	 * @param libFile the new URL of this Library.
 	 */
 	public void setLibFile(URL libFile)
 	{
 		this.libFile = libFile;
 	}
 
     /**
      * Compares two <code>Library</code> objects.
      * @param   o   the Library to be compared.
      * @return	the result of comparison.
      */
     public int compareTo(Object o/*Library that*/)
 	{
 		Library that = (Library)o;
 		return TextUtils.nameSameNumeric(libName, that.libName);
     }
 
 	/**
 	 * Returns a printable version of this Library.
 	 * @return a printable version of this Library.
 	 */
 	public String toString()
 	{
		return "Library " + libName;
 	}
 
 	// ----------------- cells --------------------
 
 	private void getCurCellPref()
 	{
 		if (curCellPref == null)
 		{
 			curCellPref = Pref.makeStringPref("CurrentCellLibrary" + libName, prefs, "");
 		}
 	}
 
 	/**
 	 * Method to get the current Cell in this Library.
 	 * @return the current Cell in this Library.
 	 * Returns NULL if there is no current Cell.
 	 */
 	public Cell getCurCell()
 	{
 		getCurCellPref();
 		String cellName = curCellPref.getString();
 		if (cellName.length() == 0) return null;
 		Cell cell = this.findNodeProto(cellName);
 		if (cell == null) curCellPref.setString("");
 		return cell;
 	}
 
 	/**
 	 * Method to set the current Cell in this Library.
 	 * @param curCell the new current Cell in this Library.
 	 */
 	public void setCurCell(Cell curCell)
 	{
 		getCurCellPref();
 		String cellName = "";
 		if (curCell != null) cellName = curCell.noLibDescribe();
 		curCellPref.setString(cellName);
 	}
 
 	/**
 	 * Method to find the Cell with the given name in this Library.
 	 * @param name the name of the desired Cell.
 	 * @return the Cell with the given name in this Library.
 	 */
 	public Cell findNodeProto(String name)
 	{
 		if (name == null) return null;
 		CellName n = CellName.parseName(name);
 		if (n == null) return null;
 		Cell cell = (Cell)cells.get(n);
 		if (cell != null) return cell;
 
 		Cell onlyWithName = null;
 		for (Iterator it = cells.values().iterator(); it.hasNext();)
 		{
 			Cell c = (Cell) it.next();
 			if (!n.getName().equalsIgnoreCase(c.getName())) continue;
 			onlyWithName = c;
 			if (n.getView() != c.getView()) continue;
 			if (n.getVersion() > 0 && n.getVersion() != c.getVersion()) continue;
 			if (n.getVersion() == 0 && c.getNewestVersion() != c) continue;
 			return c;
 		}
 		if (n.getView() == View.UNKNOWN && onlyWithName != null) return onlyWithName;
 		return null;
 	}
 
 	/**
 	 * Returns true, if cell is contained in this Library.
 	 */
 	boolean contains(Cell cell) { return cells.get(cell.getCellName()) == cell; }
 
 	/**
 	 * Method to return an Iterator over all Cells in this Library.
 	 * @return an Iterator over all Cells in this Library.
 	 */
 	public Iterator getCells()
 	{
         synchronized(cells) {
 			ArrayList cellsCopy = new ArrayList(cells.values());
 			return cellsCopy.iterator();
         }
 	}
 
 	/**
 	 * Method to return an Iterator over all Cells in this Library after given CellName.
 	 * @param cn starting CellName
 	 * @return an Iterator over all Cells in this Library after given CellName.
 	 */
 	Iterator getCellsTail(CellName cn)
 	{
 		return cells.tailMap(cn).values().iterator();
 	}
 
 	/**
 	 * Returns verison of Electric which wrote this library.
 	 * Returns null for ReadableDumps, for new libraries and for dummy libraries.
 	 * @return version
 	 */
 	public Version getVersion()
 	{
 		return version;
 	}
 
 	/**
 	 * Method to set library version found in header.
 	 * @param version
 	 */
 	public void setVersion(Version version)
 	{
 		this.version = version;
 	}
 }
