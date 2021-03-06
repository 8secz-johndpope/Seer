 /* -*- tab-width: 4 -*-
  *
  * Electric(tm) VLSI Design System
  *
  * File: LibraryFiles.java
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
 package com.sun.electric.tool.io.input;
 
 import com.sun.electric.database.hierarchy.Library;
 import com.sun.electric.database.hierarchy.Cell;
 import com.sun.electric.database.hierarchy.View;
 import com.sun.electric.database.prototype.NodeProto;
 import com.sun.electric.database.text.Name;
 import com.sun.electric.database.text.TextUtils;
 import com.sun.electric.database.text.Version;
 import com.sun.electric.database.topology.NodeInst;
 import com.sun.electric.database.variable.FlagSet;
 import com.sun.electric.lib.LibFile;
 import com.sun.electric.technology.PrimitiveNode;
 import com.sun.electric.technology.Technology;
 import com.sun.electric.technology.technologies.MoCMOS;
 import com.sun.electric.technology.technologies.Schematics;
 import com.sun.electric.tool.user.dialogs.OpenFile;
 import com.sun.electric.tool.user.ErrorLogger;
 
 import java.io.InputStream;
 import java.io.File;
 import java.net.URL;
 import java.net.URLConnection;
 import java.util.List;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.HashMap;
 
 
 /**
  * This class reads Library files (ELIB or readable dump) format.
  */
 public class LibraryFiles extends Input
 {
 	// the cell information
 	/** The number of Cells in the file. */									protected int nodeProtoCount;
 	/** A list of cells being read. */										protected Cell [] nodeProtoList;
 	/** lambda value for each cell of the library */						protected double [] cellLambda;
 	/** total number of cells in all read libraries */						protected static int totalCells;
 	/** number of cells constructed so far. */								protected static int cellsConstructed;
 	/** a List of scaled Cells that got created */							protected List scaledCells;
 	/** a List of wrong-size Cells that got created */						protected List skewedCells;
 	/** The Electric version in the library file. */						protected int emajor, eminor, edetail;
 	/** the Electric version in the library file. */						protected Version version;
 	/** true if old MOSIS CMOS technologies appear in the library */		protected boolean convertMosisCmosTechnologies;
 
 	protected static class NodeInstList
 	{
 		protected NodeInst []  theNode;
 		protected NodeProto [] protoType;
 		protected Name []      name;
 		protected int []       lowX;
 		protected int []       highX;
 		protected int []       lowY;
 		protected int []       highY;
 		protected int []       anchorX;
 		protected int []       anchorY;
 		protected short []     rotation;
 		protected int []       transpose;
 		protected int []       userBits;
 	};
 
 	/** collection of libraries and their input objects. */					private static List libsBeingRead;
 	protected static final boolean VERBOSE = false;
 
 	LibraryFiles() {}
 
 	// *************************** THE CREATION INTERFACE ***************************
 
 	public static void initializeLibraryInput()
 	{
 		libsBeingRead = new ArrayList();
 	}
 
 	public boolean readInputLibrary()
 	{
 		// add this reader to the list
         assert(!libsBeingRead.contains(this));
         libsBeingRead.add(this);
 		//libsBeingRead.put(lib, this);
 		scaledCells = new ArrayList();
 		skewedCells = new ArrayList();
 
 		return readLib();
 	}
 
 	protected void scanNodesForRecursion(Cell cell, FlagSet markCellForNodes, NodeProto [] nil, int start, int end)
 	{
 		// scan the nodes in this cell and recurse
 		for(int j=start; j<end; j++)
 		{
 			NodeProto np = nil[j];
 			if (np instanceof PrimitiveNode) continue;
 			Cell otherCell = (Cell)np;
 			if (otherCell == null) continue;
 
 			// subcell: make sure that cell is setup
 			if (otherCell.isBit(markCellForNodes)) continue;
 
 			LibraryFiles reader = this;
 			if (otherCell.getLibrary() != cell.getLibrary())
 				reader = getReaderForLib(otherCell.getLibrary());
 
 			// subcell: make sure that cell is setup
 			if (reader != null)
 				reader.realizeCellsRecursively(otherCell, markCellForNodes, null, 0, 0);
 		}
 		cell.setBit(markCellForNodes);
 	}
 
 	/**
 	 * Method to read a Library.
 	 * This method is never called.
 	 * Instead, it is always overridden by the appropriate read subclass.
 	 * @return true on error.
 	 */
 	protected boolean readLib() { return true; }
 
 	/**
 	 * Method to find the View to use for an old View name.
 	 * @param viewName the old View name.
 	 * @return the View to use (null if not found).
 	 */
 	protected View findOldViewName(String viewName)
 	{
 		if (version.getMajor() < 8)
 		{
 			if (viewName.equals("compensated")) return View.LAYOUTCOMP;
 			if (viewName.equals("skeleton")) return View.LAYOUTSKEL;
 			if (viewName.equals("simulation-snapshot")) return View.DOCWAVE;
 			if (viewName.equals("netlist-netlisp-format")) return View.NETLISTNETLISP;
 			if (viewName.equals("netlist-rsim-format")) return View.NETLISTRSIM;
 			if (viewName.equals("netlist-silos-format")) return View.NETLISTSILOS;
 			if (viewName.equals("netlist-quisc-format")) return View.NETLISTQUISC;
 			if (viewName.equals("netlist-als-format")) return View.NETLISTALS;
 		}
 		return null;
 	}
 
 	protected Technology findTechnologyName(String name)
 	{
 		Technology tech = null;
 		if (convertMosisCmosTechnologies)
 		{
 			if (name.equals("mocmossub")) tech = MoCMOS.tech; else
 				if (name.equals("mocmos")) tech = Technology.findTechnology("mocmosold");
 		}
 		if (tech == null) tech = Technology.findTechnology(name);
 		if (tech == null && name.equals("logic"))
 			tech = Schematics.tech;
 		if (tech == null && (name.equals("epic8c") || name.equals("epic7c")))
 			tech = Technology.findTechnology("epic7s");
 		return tech;
 	}
 
 	/**
 	 * Method to read an external library file, given its name as stored on disk.
 	 * Attempts to find the file in many different ways, including asking the user.
 	 * @param theFileName the full path to the file, as written to disk.
 	 * @return a Library that was read. If library cannot be read or found, creates
      * a Library called DUMMYname, and returns that.
 	 */
 	protected Library readExternalLibraryFromFilename(String theFileName)
 	{
 		// get the path to the library file
 		File libFile = new File(theFileName);
 
 		// see if this library is already read in
 		String libFileName = libFile.getName();
 		String libFilePath = libFile.getParent();
 		
 		// special case if the library path came from a different computer system and still has separators
 		int backSlashPos = libFileName.lastIndexOf('\\');
 		int colonPos = libFileName.lastIndexOf(':');
 		int slashPos = libFileName.lastIndexOf('/');
 		int charPos = Math.max(backSlashPos, Math.max(colonPos, slashPos));
 		if (charPos >= 0)
 		{
 			libFileName = libFileName.substring(charPos+1);
 			libFilePath = "";
 		}
 		OpenFile.Type importType = OpenFile.Type.ELIB;
 		String libName = libFileName;
 		if (libName.endsWith(".elib"))
 		{
 			libName = libName.substring(0, libName.length()-5);
 		} else if (libName.endsWith(".txt"))
 		{
 			libName = libName.substring(0, libName.length()-4);
 			importType = OpenFile.Type.READABLEDUMP;
 		} else
 		{
 			// no recognizable extension, add one to the file name
 			libFileName += ".elib";
 		}
 
 		// first try the pure library name with no path information
 		Library elib = Library.findLibrary(libName);
 		if (elib == null)
 		{
 			// library does not exist: see if file is in the same directory as the main file
 			URL externalURL = TextUtils.makeURLToFile(mainLibDirectory + libFileName);
 			StringBuffer errmsg = new StringBuffer();
 			boolean exists = TextUtils.URLExists(externalURL, errmsg);
 			if (!exists)
 			{
 				// try secondary library file locations
 				for (Iterator libIt = LibDirs.getLibDirs(); libIt.hasNext(); )
 				{
 					externalURL = TextUtils.makeURLToFile((String)libIt.next() + File.separator + libFileName);
 					exists = TextUtils.URLExists(externalURL, errmsg);
 					if (exists) break;
 				}
 				if (!exists)
 				{
 					// try the exact path specified in the reference
 					externalURL = TextUtils.makeURLToFile(libFile.getPath());
 					exists = TextUtils.URLExists(externalURL, errmsg);
 					if (!exists)
 					{
 						// try the Electric library area
 						externalURL = LibFile.getLibFile(libFileName);
 						exists = TextUtils.URLExists(externalURL, errmsg);
 					}
 				}
 			}
 			if (!exists)
 			{
 				System.out.println("Error: cannot find referenced library " + libFile.getPath()+":");
 				System.out.print(errmsg.toString());
 				String pt = null;
 				while (true) {
 					// continue to ask the user where the library is until they hit "cancel"
 					String description = "Reference library '" + libFileName + "'";
 					pt = OpenFile.chooseInputFile(OpenFile.Type.ELIB, description);
 					if (pt == null) {
 						// user cancelled, break
 						break;
 					}
 					// see if user chose a file we can read
 					externalURL = TextUtils.makeURLToFile(pt);
 					if (externalURL != null) {
 						exists = TextUtils.URLExists(externalURL, null);
 						if (exists) {
 							// good pt, opened it, get out of here
 							break;
 						}
 					}
 				}
 			}
 
             // last option: let user pick library location
 			if (exists)
 			{
 				System.out.println("Reading referenced library " + externalURL.getFile());
 				elib = Library.newInstance(libName, externalURL);
 			}
 
             if (elib != null) {
                 // read the external library
                 String oldNote = progress.getNote();
                 if (progress != null)
                 {
                     progress.setProgress(0);
                     progress.setNote("Reading referenced library " + libName + "...");
                 }
 
                 elib = readALibrary(externalURL, elib, importType);
                 progress.setProgress((int)(byteCount * 100 / fileLength));
                 progress.setNote(oldNote);
             }
 
             if (elib == null) {
                 System.out.println("Error: cannot find referenced library " + libFile.getPath());
                 System.out.println("...Creating new "+libName+" Library instead");
                 elib = Library.newInstance(libName, null);
             }
 //			if (failed) elib->userbits |= UNWANTEDLIB; else
 //			{
 //				// queue this library for announcement through change control
 //				io_queuereadlibraryannouncement(elib);
 //			}
 		}
 		return elib;
 	}
 
 	public static void cleanupLibraryInput()
 	{
 		progress.setNote("Constructing cell contents...");
 		progress.setProgress(0);
 
 		// clear flag bits for scanning the library hierarchically
 		totalCells = 0;
 		FlagSet markCellForNodes = NodeProto.getFlagSet(1);
 		for(Iterator it = libsBeingRead.iterator(); it.hasNext(); )
 		{
 			LibraryFiles reader = (LibraryFiles)it.next();
 			totalCells += reader.nodeProtoCount;
 			for(int cellIndex=0; cellIndex<reader.nodeProtoCount; cellIndex++)
 			{
 				Cell cell = reader.nodeProtoList[cellIndex];
 				if (cell == null) continue;
 				if (cell.getLibrary() != reader.lib) continue;
 				reader.cellLambda[cellIndex] = reader.computeLambda(cell, cellIndex);
 				cell.setTempInt(cellIndex);
 				cell.clearBit(markCellForNodes);
 			}
 		}
 		cellsConstructed = 0;
 
 		// now recursively adjust lambda sizes
 		if (LibraryFiles.VERBOSE)
 			System.out.println("Preparing to compute scale factors");
 		for(int i=0; i<20; i++)
 		{
 			boolean unchanged = true;
 			for(Iterator it = libsBeingRead.iterator(); it.hasNext(); )
 			{
 				LibraryFiles reader = (LibraryFiles)it.next();
 				for(int cellIndex=0; cellIndex<reader.nodeProtoCount; cellIndex++)
 				{
 					Cell cell = reader.nodeProtoList[cellIndex];
 					if (cell == null) continue;
 					if (cell.getLibrary() != reader.lib) continue;
 					if (reader.spreadLambda(cell, cellIndex))
 					{
 						unchanged = false;
 					}
 				}
 			}
 			if (unchanged) break;
 		}
 		if (LibraryFiles.VERBOSE)
 			System.out.println("Finished computing scale factors");
 
 		// recursively create the cell contents
 		for(Iterator it = libsBeingRead.iterator(); it.hasNext(); )
 		{
 			LibraryFiles reader = (LibraryFiles)it.next();
 			for(int cellIndex=0; cellIndex<reader.nodeProtoCount; cellIndex++)
 			{
 				Cell cell = reader.nodeProtoList[cellIndex];
 				if (cell == null) continue;
 				if (cell.isBit(markCellForNodes)) continue;
 				reader.realizeCellsRecursively(cell, markCellForNodes, null, 0, 0);
 			}
 		}
 		markCellForNodes.freeFlagSet();
 
 		// tell which libraries had extra "scaled" cells added
 		boolean first = true;
 		for(Iterator it = libsBeingRead.iterator(); it.hasNext(); )
 		{
 			LibraryFiles reader = (LibraryFiles)it.next();
 			if (reader.scaledCells != null && reader.scaledCells.size() != 0)
 			{
 				if (first)
 				{
 					System.out.println("WARNING: to accommodate scaling inconsistencies, these cells were created:");
 					first = false;
 				}
 				StringBuffer sb = new StringBuffer();
 				sb.append("   Library " + reader.lib.getName() + ":");
 				for(Iterator sIt = reader.scaledCells.iterator(); sIt.hasNext(); )
 				{
 					Cell cell = (Cell)sIt.next();
 					sb.append(" " + cell.noLibDescribe());
 				}
 				System.out.println(sb.toString());
 			}
 			if (reader.skewedCells != null && reader.skewedCells.size() != 0)
 			{
 				if (first)
 				{
 					System.out.println("ERROR: because of library inconsistencies, these stretched cells were created:");
 					first = false;
 				}
 				StringBuffer sb = new StringBuffer();
 				sb.append("   Library " + reader.lib.getName() + ":");
 				for(Iterator sIt = reader.skewedCells.iterator(); sIt.hasNext(); )
 				{
 					Cell cell = (Cell)sIt.next();
 					sb.append(" " + cell.noLibDescribe());
 				}
 				System.out.println(sb.toString());
 			}
 		}
 
 		// adjust for old library conversion
 		convertOldLibraries();
         // clean up init (free LibraryFiles for garbage collection)
         libsBeingRead.clear();
 	}
 
 	private static void convertOldLibraries()
 	{
 		// see if the MOSIS CMOS technology now has old-style state information
 		MoCMOS.tech.convertOldState();
 	}
 
 	protected LibraryFiles getReaderForLib(Library lib) {
         for (Iterator it = libsBeingRead.iterator(); it.hasNext(); ) {
             LibraryFiles reader = (LibraryFiles)it.next();
             if (reader.lib == lib) return reader;
         }
         return null;
     }
 
 	// *************************** THE CELL CLEANUP INTERFACE ***************************
 
 	protected double computeLambda(Cell cell, int cellIndex) { return 1; }
 
 	protected boolean spreadLambda(Cell cell, int cellIndex) { return false; }
 
 	/**
 	 * Method to recursively create the contents of each cell in the library.
 	 */
 	protected void realizeCellsRecursively(Cell cell, FlagSet recursiveSetupFlag, String scaledCellName, double scaleX, double scaleY)
 	{
 	}
 
 	protected boolean readerHasExport(Cell c, String portName)
 	{
 		return false;
 	}
 
 }
