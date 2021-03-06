 package GeneticModels;
 
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 
 import org.jdom.DataConversionException;
 import org.jdom.Element;
 
 import VGL.GeneticModelAndCageSet;
 /**
  * Brian White Summer 2008
  * 
  * This program is free software; you can redistribute it and/or modify it under
  * the terms of the GNU General Public License as published by the Free Software
  * Foundation; either version 2 of the License, or (at your option) any later
  * version.
  * 
  * This program is distributed in the hope that it will be useful, but WITHOUT
  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
  * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
  * details.
  * 
  * You should have received a copy of the GNU General Public License along with
  * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
  * Place - Suite 330, Boston, MA 02111-1307, USA.
  * 
  * @author Brian White
 * @version 1.0 $Id: WorkFileProcessor.java,v 1.8 2008-10-25 14:54:43 brian Exp $
  */
 
 /**
  * class to read in work files and build a GeneticModel
  * and a set of populated cages
  * @author brian
  *
  */
 public class WorkFileProcessor {
 
 	private GeneticModel geneticModel;
 	private ArrayList<Cage> cages;
 
 	public WorkFileProcessor(List<Element> elements) {
 		Iterator<Element> it = elements.iterator();
 		while (it.hasNext()) {
 			Element current = it.next();
 			String name = current.getName();
 			try {
 				if (name.equals("GeneticModel")) geneticModel = processSavedModelInfo(current);
 				if (name.equals("Organisms")) cages = processSavedCages(current);
 			} catch (Exception e) {
 				e.printStackTrace();
 			}
 		}
 	}
 
 	public GeneticModel getGeneticModel() {
 		return geneticModel;
 	}
 
 	public ArrayList<Cage> getCages() {
 		return cages;
 	}
 
 	private GeneticModel processSavedModelInfo(Element e) throws Exception {
 
 		GeneticModel model = 
 			new GeneticModel(
					Boolean.parseBoolean(e.getAttributeValue("XX_XYSexDetermination")));
 
 		model.setMinOffspring(Integer.parseInt(e.getAttributeValue("MinOffspring")));
 		model.setMaxOffspring(Integer.parseInt(e.getAttributeValue("MaxOffspring")));
 
 		Iterator<Element> it = e.getChildren().iterator();
 		//get the tags inside the "Model" tag
 		if (e.getAttribute("BeginnerMode") != null) {
 			model.setBeginnerMode(e.getAttribute("BeginnerMode").getBooleanValue());
 		} 
 		int numberOfCharacters = e.getAttribute("NumberOfCharacters").getIntValue();
 
 		TraitFactory.getInstance().initializeTraitBank(2, numberOfCharacters, 6);
 
 		// now the rest
 		while(it.hasNext()) {
 			Element current = it.next();
 			String name = current.getName();
 			if (name.equals("CharacterOrderScrambler")) {
 				int[] scrambler = new int[numberOfCharacters];
 				Iterator<Element> scIt = current.getChildren().iterator();
 				int i = 0;
 				while (scIt.hasNext()) {
 					Element te = scIt.next();
 					scrambler[Integer.parseInt(te.getAttributeValue("Index"))] = 
 						Integer.parseInt(te.getValue());
 				}
 				model.setScrambledCharacterOrder(scrambler);
 
 			} else if (name.equals("ChromosomeModel")) {
 				boolean sexChromosome = 
 					Boolean.parseBoolean(
 							current.getAttributeValue("SexChromosome"));
 				processChromosomeModelInfo(model, sexChromosome, current);
 			}
 		}
 		return model;
 	}
 
 	private void processChromosomeModelInfo(
 			GeneticModel model, 
 			Boolean sexChromosome, 
 			Element e) throws Exception {
 
 		int chromoNumber = 1;
 		if (sexChromosome) chromoNumber = 0;
 
 		int numGeneModels = Integer.parseInt(e.getAttributeValue("NumGenes"));
 		// set up the geneModels first
 		GeneModel[] geneModels = new GeneModel[numGeneModels];
 		Iterator<Element> gmIt = e.getChildren().iterator();
 		int i = 0;
 		float rf = -1.0f;
 		while (gmIt.hasNext()) {
 			Element gmEl = gmIt.next();
 			rf = Float.parseFloat(gmEl.getAttributeValue("RfToPrevious"));
 			geneModels[i] = buildGeneModel(gmEl, chromoNumber, i);
 			i++;
 		}
 
 		if (sexChromosome) {
 			for (int j = 0; j < numGeneModels; j++) {
 				if (j == 0) {
 					model.addFirstSexLinkedGeneModel(geneModels[j]);
 				} else {
 					model.addNextSexLinkedGeneModel(rf, geneModels[j]);
 				}
 			}
 		} else {
 			for (int j = 0; j < numGeneModels; j++) {
 				if (j == 0) {
 					model.addFirstAutosomalGeneModel(geneModels[j]);
 				} else {
 					model.addNextAutosomalGeneModel(rf, geneModels[j]);
 				}
 			}			
 		}
 	}
 
 	private GeneModel buildGeneModel(Element e, int chromo, int gene) {
 		String type = e.getAttributeValue("Type");
 		List<Element> traitList = e.getChildren();
 		if (type.equals("TwoAlleleSimpleDominance")) {
 			return new TwoAlleleSimpleDominanceGeneModel(
 					traitList, chromo, gene);
 		} else if(type.equals("TwoAlleleIncompleteDominance")) {
 			return new TwoAlleleIncompleteDominanceGeneModel(
 					traitList, chromo, gene);
 		} else if(type.equals("ThreeAlleleHierarchicalDominance")) {
 			return new ThreeAlleleHierarchicalDominanceGeneModel(
 					traitList, chromo, gene);
 		} else if(type.equals("ThreeAlleleCircularDominance")) {
 			return new ThreeAlleleCircularDominanceGeneModel(
 					traitList, chromo, gene);			
 		} else if(type.equals("ThreeAlleleIncompleteDominance")) {
 			return new ThreeAlleleIncompleteDominanceGeneModel(
 					traitList, chromo, gene);						
 		} else {
 			return null;
 		}
 	}
 
 	private ArrayList<Cage> processSavedCages(Element e) {
 		ArrayList<Cage> cages = new ArrayList<Cage>();
 		Iterator<Element> cageIt = e.getChildren().iterator();
 		while (cageIt.hasNext()) {
 			Element cageE = cageIt.next();
 			if(cageE.getName().equals("Cage")) {
 				Cage c = buildCage(cageE);
 				cages.add(c);
 			}
 		}
 		return cages;
 	}
 
 	private Cage buildCage(Element e) {
 		int cageId = Integer.parseInt(e.getAttributeValue("Id"));
 		int numChildren = Integer.parseInt(e.getAttributeValue("NumChildren"));
 
 		Cage cage = new Cage(cageId);
 		Iterator<Element> contentsIt = e.getChildren().iterator();
 		while (contentsIt.hasNext()) {
 			Element item = contentsIt.next();
 			if (item.getName().equals("Parents")) {
 				Iterator<Element> parentIt = item.getChildren().iterator();
 				Element p1E = parentIt.next();
 				Organism p1 = 
 					OrganismFactory.buildOrganism(
 							p1E, 
 							Integer.parseInt(p1E.getAttributeValue("CageId")),
 							geneticModel);
 				Element p2E = parentIt.next();
 				Organism p2 = 
 					OrganismFactory.buildOrganism(
 							p2E,
 							Integer.parseInt(p2E.getAttributeValue("CageId")),
 							geneticModel);
 				cage.setParents(p1, p2);
 			} else if(item.getName().equals("Children")) {
 				Organism[] childrenInOrder = new Organism[numChildren];
 				Iterator<Element> childIt = item.getChildren().iterator();
 				while (childIt.hasNext()) {
 					Element childE = childIt.next();
 					if (childE.getName().equals("Organism")) {
 						int index = Integer.parseInt(childE.getAttributeValue("Id"));
 						childrenInOrder[index] = 
 							OrganismFactory.buildOrganism(
 									childE, cageId, geneticModel);
 					}
 				}
 				
 				for (int i = 0; i < numChildren; i++) {
 					cage.addSaved(childrenInOrder[i]);
 				}
 			}
 		}
 		return cage;
 	}
 
 }
