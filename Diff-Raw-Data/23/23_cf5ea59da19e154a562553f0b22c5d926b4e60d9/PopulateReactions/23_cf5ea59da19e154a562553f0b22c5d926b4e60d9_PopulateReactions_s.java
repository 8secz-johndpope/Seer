 ////////////////////////////////////////////////////////////////////////////////
 //
 //	RMG - Reaction Mechanism Generator
 //
 //	Copyright (c) 2002-2009 Prof. William H. Green (whgreen@mit.edu) and the
 //	RMG Team (rmg_dev@mit.edu)
 //
 //	Permission is hereby granted, free of charge, to any person obtaining a
 //	copy of this software and associated documentation files (the "Software"),
 //	to deal in the Software without restriction, including without limitation
 //	the rights to use, copy, modify, merge, publish, distribute, sublicense,
 //	and/or sell copies of the Software, and to permit persons to whom the
 //	Software is furnished to do so, subject to the following conditions:
 //
 //	The above copyright notice and this permission notice shall be included in
 //	all copies or substantial portions of the Software.
 //
 //	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 //	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 //	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 //	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 //	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 //	FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 //	DEALINGS IN THE SOFTWARE.
 //
 ////////////////////////////////////////////////////////////////////////////////
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileReader;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.LinkedHashSet;
 import java.util.StringTokenizer;
 
 import jing.chem.ChemGraph;
 import jing.chem.ForbiddenStructureException;
 import jing.chem.Species;
 import jing.chemParser.ChemParser;
 import jing.chemUtil.Graph;
 import jing.param.Global;
 import jing.param.Temperature;
 import jing.rxn.Kinetics;
 import jing.rxn.Reaction;
 import jing.rxn.TemplateReactionGenerator;
 import jing.rxnSys.ReactionModelGenerator;
 
 public class PopulateReactions {
 	/**
 	 * Generates a list of all possible reactions, and their modified Arrhenius
 	 * 	parameters, between all species supplied in the input file.
 	 * 
 	 * The input file's first line should specify the system temperature.  The
 	 * 	line should be formatted as follows:
 	 * 		Temperature: 500 (K)
 	 * The input (.txt) file should contain a list of species, with the first
 	 * 	line of each species being a user-defined name for the species and the 
 	 * 	remaining lines	containing the graph (in the form of an adjacency list).
 	 *  There is no limit on the number of ChemGraphs the user may supply.
 	 *  
 	 * The output of the function is two .txt files: PopRxnsOutput_rxns.txt and
 	 * 	PopRxnsOutput_spcs.txt.  The first contains the list of reactions (including
 	 * 	the modified Arrhenius parameters and RMG-generated comments) and the second
 	 * 	the list of species (including the ChemGraph) that are involved in the list
 	 * 	of reactions.
 	 * 
 	 *	UPDATE by MRH on 10/Jan/2010 - Fixed 2 bugs:
 	 *		(1) Module did not report the correct A (pre-exponential factor) for reverse
 	 *			reactions (e.g. H+CH4=CH3+H2)
 	 *		(2) Module reported the same reaction multiple times (no dupliate catch)
 	 */
 	public static void main(String[] args) {
 		initializeSystemProperties();
 		// Set Global.lowTemp and Global.highTemp
 		//	The values of the low/highTemp are not used in the function
 		//		(to the best of my knowledge).
 		//	They are necessary for the instances of additionalKinetics, 
 		//	e.g. H2C*-CH2-CH2-CH3 -> H3C-CH2-*CH-CH3
 		Global.lowTemperature = new Temperature(300,"K");
 		Global.highTemperature = new Temperature(1500,"K");
 
 		// Define variable 'speciesSet' to store the species contained in the input file
 		LinkedHashSet speciesSet = new LinkedHashSet();
 		// Define variable 'reactions' to store all possible rxns between the species in speciesSet
 		LinkedHashSet reactions = new LinkedHashSet();
 		
 		// Define two string variables 'listOfReactions' and 'listOfSpecies'
 		//	These strings will hold the list of rxns (including the structure,
 		//	modified Arrhenius parameters, and source/comments) and the list of
 		//	species (including the chemkin name and graph), respectively
 		String listOfReactions = "Arrhenius 'A' parameter has units of: mol,cm3,s\n" +
 			"Arrhenius 'n' parameter is unitless\n" +
 			"Arrhenius 'E' parameter has units of: kcal/mol\n\n";
 		String listOfSpecies = "";
 		
 		// Open and read the input file
 		try {
 			FileReader fr_input = new FileReader(args[0]);
 			BufferedReader br_input = new BufferedReader(fr_input);
 			// Read in the first line of the input file
 			//	This line should hold the temperature of the system, e.g.
 			//		Temperature: 500 (K)
 			String line = ChemParser.readMeaningfulLine(br_input);
 			Temperature systemTemp = null;
 			if (!line.startsWith("Temperature")) {
 				System.err.println("Error reading input file: Could not locate System Temperature.\n" +
 						"The first line of the input file should read: \"Temperature: Value (Units)\"");
 			} else {
 				StringTokenizer st = new StringTokenizer(line);
 				String dummyString = st.nextToken();	// This token should be "Temperature:"
 				systemTemp = new Temperature(Double.parseDouble(st.nextToken()),ChemParser.removeBrace(st.nextToken()));
 			}
 			Temperature systemTemp_K = new Temperature(systemTemp.getK(),"K");
 			
 			// Creating a new ReactionModelGenerator so I can set the variable temp4BestKinetics
 			ReactionModelGenerator rmg = new ReactionModelGenerator();
 			rmg.setTemp4BestKinetics(systemTemp_K);
 			TemplateReactionGenerator rtLibrary = new TemplateReactionGenerator();
 			
 			listOfReactions += "System Temperature: " + systemTemp_K.getK() + "K\n\n";
 			line = ChemParser.readMeaningfulLine(br_input);
 
             StringTokenizer st = new StringTokenizer(line);
           // The first line should start with "Solvation", otherwise do nothing and display a message to the user
           if (st.nextToken().startsWith("Solvation")) {
         	  line = st.nextToken().toLowerCase();
         	  // The options for the "Solvation" field are "on" or "off" (as of 18May2009), otherwise do nothing and display a message to the user
         	  // Note: I use "Species.useInChI" because the "Species.useSolvation" updates were not yet committed.
 
         	  if (line.equals("on")) {
         		  Species.useSolvation = true;
 //                  rmg.setUseDiffusion(true);
         		  listOfReactions += "Solution-phase chemistry!\n\n";
         	  } else if (line.equals("off")) {
         		  Species.useSolvation = false;
 //                  rmg.setUseDiffusion(false);
         		  listOfReactions += "Gas-phase chemistry.\n\n";
         	  } else {
 
         		  System.out.println("Error in reading thermo_input.txt file:\nThe field 'Solvation' has the options 'on' or 'off'.");
         		  return;
         	  }
           }
           
             line = ChemParser.readMeaningfulLine(br_input);
             // Read in all of the species in the input file
 			while (line != null) {
 				// The first line of a new species is the user-defined name
 				String speciesName = line;
 				// The remaining lines are the graph
 				Graph g = ChemParser.readChemGraph(br_input);
 				// Make the ChemGraph, assuming it does not contain a forbidden structure
 				ChemGraph cg = null;
 				try {
 					cg = ChemGraph.make(g);
 				} catch (ForbiddenStructureException e) {
 					System.out.println("Error in reading graph: Graph contains a forbidden structure.\n" + g.toString());
 					System.exit(0);
 				}
 				// Make the species
 				Species species = Species.make(speciesName,cg);
 				// Add the new species to the set of species
 				speciesSet.add(species);
 
 				// Read the next line of the input file
 				line = ChemParser.readMeaningfulLine(br_input);
 			}
 			
 			// React all species with each other
 			reactions = rtLibrary.react(speciesSet);
 			
 			// Some of the reactions may be dupliates of one another 
 			//	(e.g. H+CH4=CH3+H2 as a forward reaction and reverse reaction)
 			//	Create new LinkedHashSet which will store the non-duplicate rxns
 			LinkedHashSet nonDuplicateRxns = new LinkedHashSet();
 			int Counter = 0;
 			
 			Iterator iter_rxns = reactions.iterator();
         	while (iter_rxns.hasNext()){
         		++Counter;
         		Reaction r = (Reaction)iter_rxns.next();
         		
         		// The first reaction is not a duplicate of any previous reaction
         		if (Counter == 1) {
         			nonDuplicateRxns.add(r);
         			listOfReactions += writeOutputString(r,rtLibrary);
             		speciesSet.addAll(r.getProductList());
         		}
         		
         		// Check whether the current reaction (or its reverse) has the same structure
         		//	of any reactions already reported in the output
         		else {
         			Iterator iterOverNonDup = nonDuplicateRxns.iterator();
         			boolean dupRxn = false;
 	        		while (iterOverNonDup.hasNext()) {
 	        			Reaction temp_Reaction = (Reaction)iterOverNonDup.next();
	        			if (r.getStructure() == temp_Reaction.getStructure() || 
	        					r.getReverseReaction().getStructure() == temp_Reaction.getStructure()) {
 	        				dupRxn = true;
 	        				break;
	        			}	
 	        		}
 	        		if (!dupRxn) {
         				nonDuplicateRxns.add(r);
         				listOfReactions += writeOutputString(r,rtLibrary);
                 		speciesSet.addAll(r.getProductList());
 	        		}
         		}
 			}
         	
         	Iterator iter_species = speciesSet.iterator();
         	// Define dummy integer 'i' so our getChemGraph().toString()
         	//	call only returns the graph
         	int i = 0;
         	while (iter_species.hasNext()) {
         		Species species = (Species)iter_species.next();
         		listOfSpecies += species.getChemkinName() + "\n" +
         			species.getChemGraph().toString(i) + "\n";
         	}
         	
         	// Write the output files
         	try{
         		File rxns = new File("PopRxnsOutput_rxns.txt");
         		FileWriter fw_rxns = new FileWriter(rxns);
         		fw_rxns.write(listOfReactions);
         		fw_rxns.close();
         		File spcs = new File("PopRxnsOutput_spcs.txt");
         		FileWriter fw_spcs = new FileWriter(spcs);
         		fw_spcs.write(listOfSpecies);
         		fw_spcs.close();
         	}
         	catch (IOException e) {
             	System.out.println("Could not write PopRxnsOutput.txt files");
             	System.exit(0);
             }
         	// Display to the user that the program was successful and also
         	//	inform them where the results may be located
 			System.out.println("Reaction population complete. Results are stored" 
 					+ " in PopRxnsOutput_rxns.txt and PopRxnsOutput_spcs.txt");
 		} catch (FileNotFoundException e) {
 			System.err.println("File was not found!\n");
 		} catch(IOException e) {
 			System.err.println("Something wrong with ChemParser.readChemGraph");
 		}
 	}
 	
 	public static void initializeSystemProperties() {
 		File GATPFit = new File("GATPFit");
 		ChemParser.deleteDir(GATPFit);
 		GATPFit.mkdir();
 
 		String name= "RMG_database";
 		String workingDir = System.getenv("RMG");
 		System.setProperty("RMG.workingDirectory", workingDir);
 		System.setProperty("jing.chem.ChemGraph.forbiddenStructureFile",workingDir + "/databases/" + name + "/forbiddenStructure/forbiddenStructure.txt");
 		System.setProperty("jing.chem.ThermoGAGroupLibrary.pathName",	workingDir + "/databases/" + name + "/thermo");
 		System.setProperty("jing.rxn.ReactionTemplateLibrary.pathName",	workingDir + "/databases/" + name + "/kinetics");
 	};
 	
 	public static String updateListOfReactions(Kinetics rxn_k) {
 		String output = rxn_k.getAValue() + "\t" + rxn_k.getNValue()
 			   + "\t" + rxn_k.getEValue() + "\t" + rxn_k.getSource() 
 			   + "\t" + rxn_k.getComment() + "\n";
 		return output;
 	}
 	
 	public static String updateListOfReactions(Kinetics rxn_k, String reverseRxnName) {
 		String output = rxn_k.getAValue() + "\t" + rxn_k.getNValue()
 			   + "\t" + rxn_k.getEValue() + "\t" + reverseRxnName + ": "
 			   + rxn_k.getSource() + "\t" + rxn_k.getComment() + "\n";
 		return output;
 	}
 	
 	public static String writeOutputString(Reaction r, TemplateReactionGenerator rtLibrary) {
 		String listOfReactions = "";
 		if (r.hasAdditionalKinetics()) {
 			HashSet indiv_rxn = r.getAllKinetics();
 			for (Iterator iter = indiv_rxn.iterator(); iter.hasNext();) {
 				Kinetics k_rxn = (Kinetics)iter.next();
 				if (r.isForward())	listOfReactions += r.toString() + "\t" + updateListOfReactions(k_rxn) + "\tDUP\n";
 				//else if (r.isBackward()) listOfReactions += r.getReverseReaction().toString() + "\t" + updateListOfReactions(k_rxn) + "\tDUP\n";
 				else if (r.isBackward()) {
 					LinkedHashSet reverseReactions = new LinkedHashSet();
 					Iterator iter2 = r.getStructure().getProducts();
 					Species species1 = (Species)iter.next();
 					Species species2 = null;
 					while (iter2.hasNext()) 
 						species2 = (Species)iter2.next();
 					String rxnFamilyName = r.getReverseReaction().getReactionTemplate().getName();
 					reverseReactions = rtLibrary.reactSpecificFamily(species1, species2, rxnFamilyName);
 					for (Iterator iter3 = reverseReactions.iterator(); iter3.hasNext();) {
 						Reaction currentRxn = (Reaction)iter3.next();
 						if (currentRxn.getStructure() == r.getReverseReaction().getStructure())
 							listOfReactions += currentRxn.toString() + "\t" + updateListOfReactions(currentRxn.getKinetics()) + "\tDUP\n"; 
 					}
 					//listOfReactions += r.getReverseReaction().toString() + "\t" + updateListOfReactions(r.getReverseReaction().getKinetics());
 				}
 				else listOfReactions += r.toString() + "\t" + updateListOfReactions(k_rxn) + "\tDUP\n";
 			}
 		} else {
 			if (r.isForward()) listOfReactions += r.toString() + "\t" + updateListOfReactions(r.getKinetics());
 			//else if (r.isBackward()) listOfReactions += r.toString() + "\t" + updateListOfReactions(r.getFittedReverseKinetics(),r.getReactionTemplate().getName());
 			else if (r.isBackward()) {
 				LinkedHashSet reverseReactions = new LinkedHashSet();
 				Iterator iter = r.getStructure().getProducts();
 				Species species1 = (Species)iter.next();
 				Species species2 = null;
 				while (iter.hasNext()) 
 					species2 = (Species)iter.next();
 				String rxnFamilyName = r.getReverseReaction().getReactionTemplate().getName();
 				reverseReactions = rtLibrary.reactSpecificFamily(species1, species2, rxnFamilyName);
 				for (Iterator iter2 = reverseReactions.iterator(); iter2.hasNext();) {
 					Reaction currentRxn = (Reaction)iter2.next();
 					if (currentRxn.getStructure() == r.getReverseReaction().getStructure())
 						listOfReactions += currentRxn.toString() + "\t" + updateListOfReactions(currentRxn.getKinetics()); 
 				}
 				//listOfReactions += r.getReverseReaction().toString() + "\t" + updateListOfReactions(r.getReverseReaction().getKinetics());
 			}
 			else listOfReactions += r.toString() + "\t" + updateListOfReactions(r.getKinetics());
 		}
 		return listOfReactions;
 	}
 
 }
