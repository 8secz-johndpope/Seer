 //!********************************************************************************
 //!
 //!    RMG: Reaction Mechanism Generator
 //!
 //!    Copyright: Jing Song, MIT, 2002, all rights reserved
 //!
 //!    Author's Contact: jingsong@mit.edu
 //!
 //!    Restrictions:
 //!    (1) RMG is only for non-commercial distribution; commercial usage
 //!        must require other written permission.
 //!    (2) Redistributions of RMG must retain the above copyright
 //!        notice, this list of conditions and the following disclaimer.
 //!    (3) The end-user documentation included with the redistribution,
 //!        if any, must include the following acknowledgment:
 //!        "This product includes software RMG developed by Jing Song, MIT."
 //!        Alternately, this acknowledgment may appear in the software itself,
 //!        if and wherever such third-party acknowledgments normally appear.
 //!
 //!    RMG IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED
 //!    WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 //!    OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 //!    DISCLAIMED.  IN NO EVENT SHALL JING SONG BE LIABLE FOR
 //!    ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 //!    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 //!    OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 //!    OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 //!    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 //!    (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 //!    THE USE OF RMG, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 //!
 //!******************************************************************************
 
 
 
 package jing.chem;
 
 
 import java.io.*;
 import java.util.*;
 
 import jing.mathTool.*;
 import jing.mathTool.Queue;
 import jing.chemUtil.*;
 import jing.chemParser.*;
 import jing.chemUtil.Node;
 import jing.param.Global;
 import jing.param.Temperature;
 
 //## package jing::chem
 
 //----------------------------------------------------------------------------
 // jing\chem\Species.java
 //----------------------------------------------------------------------------
 
 //## class Species
 public class Species {
 
 	protected boolean GATPFitExecuted = false;
     protected int ID;		//## attribute ID
 
     protected static int TOTAL_NUMBER = 0;		//## attribute TOTAL_NUMBER
     protected static boolean addID = true;
     protected ChemGraph chemGraph;		//## attribute chemGraph
 
     /**
     The initial value is the parameter for N2, now it is treated as a default value if no further detailed information provided.
     unit: cm-1
     */
     protected double deltaEDown = 461;		//## attribute deltaEDown
 
     protected String name = null;		//## attribute name
 
     protected LinkedHashSet resonanceIsomers = new LinkedHashSet();		//## attribute resonanceIsomers
 
     protected boolean therfitExecuted = false;		//## attribute therfitExecuted
 
     protected LennardJones LJ;
 	protected NASAThermoData nasaThermoData;
     protected ThreeFrequencyModel threeFrequencyModel;
 	//protected WilhoitThermoData wilhoitThermoData;
 
 	/**
 	 * The spectroscopic data for the species (vibrational frequencies,
 	 * rotational frequencies, symmetry number, and hindered frequency-barrier 
 	 * pairs. Will eventually replace ThreeFrequencyModel.
 	 */
 	protected SpectroscopicData spectroscopicData;
 	
     // Flag to tag certain species as library only... i.e. we won't try them against RMG templates.
     // They will only react as defined in the primary reaction library.   GJB
     protected boolean IsReactive = true; 
     
     protected HashSet paths;
     
     // Constructors
 
     //## operation Species()
     private  Species() {
         initRelations();
         //#[ operation Species()
         //#]
     }
     //## operation Species(String,ChemGraph)
     private  Species(int id, String p_name, ChemGraph p_chemGraph) {
         initRelations();
 		ID = id;
         //#[ operation Species(String,ChemGraph)
         name = p_name;
         chemGraph = p_chemGraph;
         generateResonanceIsomers();
         findStablestThermoData();
         calculateLJParameters();
         selectDeltaEDown();
 		generateNASAThermoData();
         //generateThreeFrequencyModel();
 		spectroscopicData = new SpectroscopicData();
         generateSpectroscopicData();
         //#]
     }
 
     //## operation addResonanceIsomer(ChemGraph)
     public boolean addResonanceIsomer(ChemGraph p_resonanceIsomer) {
         //#[ operation addResonanceIsomer(ChemGraph)
         if (resonanceIsomers == null) resonanceIsomers = new LinkedHashSet();
 
         p_resonanceIsomer.setSpecies(this);
         return resonanceIsomers.add(p_resonanceIsomer);
 
 
 
         //#]
     }
 
     //## operation calculateCp(Temperature)
     public double calculateCp(Temperature p_temperature) {
         //#[ operation calculateCp(Temperature)
         return getThermoData().calculateCp(p_temperature);
         //#]
     }
 
     //## operation calculateG(Temperature)
     public double calculateG(Temperature p_temperature) {
         //#[ operation calculateG(Temperature)
         //return getThermoData().calculateG(p_temperature);
 		return nasaThermoData.calculateFreeEnergy(p_temperature);
         //#]
     }
 
     //## operation calculateGLowerBound(Temperature)
     //svp
       public double calculateGLowerBound(Temperature p_temperature) {
         //#[ operation calculateGLowerBound(Temperature)
         return getThermoData().calculateGLowerBound(p_temperature);
         //#]
       }
 
     //## operation calculateGUpperBound(Temperature)
     //svp
       public double calculateGUpperBound(Temperature p_temperature) {
         //#[ operation calculateGUpperBound(Temperature)
         return getThermoData().calculateGUpperBound(p_temperature);
         //#]
       }
 
 
     //## operation calculateH(Temperature)
     public double calculateH(Temperature p_temperature) {
         //#[ operation calculateH(Temperature)
         //return getThermoData().calculateH(p_temperature);
         return nasaThermoData.calculateEnthalpy(p_temperature);
 		
 		//#]
     }
 
     //## operation calculateLJParameters()
     public void calculateLJParameters() {
         //#[ operation calculateLJParameters()
         int cNum = getChemGraph().getCarbonNumber();
 
         selectLJParametersForSpecialMolecule();
         if (cNum == 1) LJ = new LennardJones(3.758, 148.6);
         else if (cNum == 2) LJ = new LennardJones(4.443, 110.7);
         else if (cNum == 3) LJ = new LennardJones(5.118, 237.1);
         else if (cNum == 4) LJ = new LennardJones(4.687, 531.4);
         else if (cNum == 5) LJ = new LennardJones(5.784, 341.1);
         else LJ = new LennardJones(5.949, 399.3);
 
         return;
 
 
         //#]
     }
 
     //## operation calculateS(Temperature)
     public double calculateS(Temperature p_temperature) {
         //#[ operation calculateS(Temperature)
         //return getThermoData().calculateS(p_temperature);
         return nasaThermoData.calculateEntropy(p_temperature);
 		//#]
     }
 
     //## operation doDelocalization(ChemGraph,Stack)
     private ChemGraph doDelocalization(ChemGraph p_chemGraph, Stack p_path) {
         //#[ operation doDelocalization(ChemGraph,Stack)
         if (p_path.isEmpty() || p_path.size() != 3) throw new InvalidDelocalizationPathException();
 
         Graph graph = Graph.copy(p_chemGraph.getGraph());
 
         // n1-a1-n2-a2-n3.
         Node node1 = graph.getNodeAt((Integer)p_path.pop());
         Node node2 = graph.getNodeAt((Integer)p_path.pop());
         Node node3 = graph.getNodeAt((Integer)p_path.pop());
         Arc arc1 = graph.getArcBetween(node1,node2);
         Arc arc2 = graph.getArcBetween(node2,node3);
 
         // deal with node1
         Atom atom1 = (Atom)node1.getElement();
         Atom newAtom1 = (Atom)atom1.changeRadical(1,null);
         node1.setElement(newAtom1);
 
         // deal with arc1
         Bond bond1 = (Bond)arc1.getElement();
         Bond newBond1 = bond1.changeBond(-1);
         arc1.setElement(newBond1);
 
         // deal with node2, actually do nothing
 
         // deal with arc2
         Bond bond2 = (Bond)arc2.getElement();
         Bond newBond2 = bond2.changeBond(1);
         arc2.setElement(newBond2);
 
         // deal with node3
         Atom atom3 = (Atom)node3.getElement();
         Atom newAtom3 = (Atom)atom3.changeRadical(-1,null);
         node3.setElement(newAtom3);
 
         node1.updateFgElement();
         node1.updateFeElement();
         node2.updateFgElement();
         node2.updateFeElement();
         node3.updateFgElement();
         node3.updateFeElement();
 
         p_path = null;
 
         try {
         	ChemGraph newIsomer = ChemGraph.make(graph);
 
         	if (addResonanceIsomer(newIsomer)) return newIsomer;
         	else return null;
         }
         catch (ForbiddenStructureException e) {
         	return null;
         }
         //#]
     }
 
     //## operation findAllDelocalizationPaths(Node)
     private HashSet findAllDelocalizationPaths(Node p_radical) {
         //#[ operation findAllDelocalizationPaths(Node)
         HashSet allPaths = new HashSet();
         Atom atom = (Atom)p_radical.getElement();
         if (!atom.isRadical()) return allPaths;
 
         Iterator iter = p_radical.getNeighbor();
         while (iter.hasNext()) {
         	Arc arc1 = (Arc)iter.next();
         	Bond bond1 = (Bond)arc1.getElement();
         	if (bond1.isSingle() || bond1.isDouble()) {
         		Node node1 = arc1.getOtherNode(p_radical);
         		Iterator iter2 = node1.getNeighbor();
         		while (iter2.hasNext()) {
         			Arc arc2 = (Arc)iter2.next();
         			if (arc2 != arc1) {
         				Bond bond2 = (Bond)arc2.getElement();
         				if (bond2.isDouble() || bond2.isTriple()) {
         					Node node2 = arc2.getOtherNode(node1);
         					Stack path = new Stack();
         					path.push(p_radical.getID());
         					path.push(node1.getID());
         					path.push(node2.getID());
         					allPaths.add(path);
         				}
         			}
         		}
 
         	}
         }
 
         return allPaths;
         //#]
     }
 
     //## operation findStablestThermoData()
     public void findStablestThermoData() {
         //#[ operation findStablestThermoData()
         double H = chemGraph.getThermoData().getH298();
         ChemGraph stablest = chemGraph;
         if (resonanceIsomers != null) {
         	Iterator iter = resonanceIsomers.iterator();
         	while (iter.hasNext()) {
         		ChemGraph g = (ChemGraph)iter.next();
         		
         		double newH = g.getThermoData().getH298();
         		if (g.fromprimarythermolibrary){
         			H = newH;
         			stablest = g;
         			return;
         		}
         			
         		if (newH < H) {
         			H = newH;
         			stablest = g;
         		}
         	}
         }
 
         chemGraph = stablest;
         //#]
     }
 
 	public void generateNASAThermoData() {
         //nasaThermoData = Therfit.generateNASAThermoData(this);
         nasaThermoData = GATPFit.generateNASAThermoData(this);
 		GATPFitExecuted = (nasaThermoData != null);
     }
 
 
     /**
     Requires:
     Effects: generate all the possible resonance isomers for the primary chemGraph
     Modifies: this.resonanceIsomers
     */
     //## operation generateResonanceIsomers()
     public void generateResonanceIsomers() {
         //#[ operation generateResonanceIsomers()
         if (chemGraph == null) throw new NullPointerException();
 
         // check if the representation of chemGraph is correct
         if (!chemGraph.repOk()) {
         	resonanceIsomers = null;
         	throw new InvalidChemGraphException();
         }
 
         // generate RI for radical
         generateResonanceIsomersFromRadicalCenter();
 
         // generaate RI for O2, removed, don't allow .o-o.
         //generateResonanceIsomersForOxygen();
 
         if (resonanceIsomers.size() == 1) {
         	ChemGraph cg = (ChemGraph)(resonanceIsomers.iterator().next());
         	if (cg == chemGraph) resonanceIsomers.clear();
         	else addResonanceIsomer(chemGraph);
         }
         
         if (chemGraph.getRadicalNumber() >= 2){
         	//find if there are radicals next to each other and in that case 
         	//increase the bond order by 1
         	Iterator radicalnodeIter = chemGraph.getRadicalNode().iterator();
         	while (radicalnodeIter.hasNext()){
         		Node n1 = (Node)radicalnodeIter.next();
         		Iterator arcs = n1.getNeighbor();
         		while (arcs.hasNext()){
         			Arc arc1 = (Arc)arcs.next();
         			Bond bond1 = (Bond)arc1.getElement();
         			Node n2 = arc1.getOtherNode(n1);
         			Atom a2 = (Atom)n2.getElement();
         			if (a2.isRadical() && !bond1.isTriple()){
         			   Graph newG = Graph.copy(chemGraph.getGraph());
 
         			   Node newn1 = newG.getNodeAt(n1.getID());
         			   Atom newa1 = (Atom)newn1.getElement();
         			   newa1.changeRadical(-1, null);
         			   newn1.setElement(newa1);
 
         			   Node newn2 = newG.getNodeAt(n2.getID());
         			   Atom newa2 = (Atom)newn2.getElement();
         			   newa2.changeRadical(-1,null);
         			   newn2.setElement(newa2);
 
         			   Arc newarc1 = newG.getArcBetween(n2.getID(), n1.getID());
         			   Bond newb1 = (Bond)newarc1.getElement();
         			   newb1.changeBond(1);
         			   newarc1.setElement(newb1);
         			   ChemGraph newchemG;
         			   try {
 						  newchemG = ChemGraph.make(newG);
 						   addResonanceIsomer(newchemG);
 
 					   } catch (InvalidChemGraphException e) {
 						  // TODO Auto-generated catch block
 						  e.printStackTrace();
 					   } catch (ForbiddenStructureException e) {
 						  // TODO Auto-generated catch block
 						  e.printStackTrace();
 					   }
         			}
         			
         		}
         	}
         }
         /*Graph g = Graph.copy(chemGraph.getGraph());
         // generate node-electron stucture
         int nodeNumber = g.getNodeNumber();
         int [] electronOnNode = new int[nodeNumber+1];
 
         Iterator nodeIter = g.getNodeList();
         while (nodeIter.hasNext()) {
         	Node node = (Node)nodeIter.next();
         	Atom atom = (Atom)node.getElement();
         	if (atom.isRadical()) {
         		int ID = node.getID().intValue();
         		electronOnNode[ID] = atom.getRadicalNumber();
         		Iterator arcIter = node.getNeighbor();
         		arcLoop:while (arcIter.hasNext()) {
         			Arc arc = (Arc)arcIter.next();
         			Bond bond = (Bond)arc.getElement();
         			if (bond.isBenzene() || bond.isTriple()) {
         				electronOnNode[ID] = 1;
         				break arcLoop;
         			}
         			else if (bond.isDouble()) {
         				electronOnNode[node.getID().intValue()-1] += 1;
         			}
         		}
         	}
         }
         */
 
         // combine them accordingly
         //resonanceIsomer = combineElectronBetweenNodes(g);
 
         return;
 
 
 
 
         //#]
     }
 
     //## operation generateResonanceIsomersForOxygen()
     public void generateResonanceIsomersForOxygen() {
         //#[ operation generateResonanceIsomersForOxygen()
         // form a O2 graph
         Graph g1 = new Graph();
         Node n1 = g1.addNodeAt(1,Atom.make(ChemElement.make("O"), FreeElectron.make("0")));
         Node n2 = g1.addNodeAt(2,Atom.make(ChemElement.make("O"), FreeElectron.make("0")));
         g1.addArcBetween(n1,Bond.make("D"),n2);
 
         // form a .o-o. graph
         Graph g2 = new Graph();
         Node n3 = g2.addNodeAt(1,Atom.make(ChemElement.make("O"), FreeElectron.make("1")));
         Node n4 = g2.addNodeAt(2,Atom.make(ChemElement.make("O"), FreeElectron.make("1")));
         g2.addArcBetween(n3,Bond.make("S"),n4);
 
         try {
         	if (chemGraph.getGraph().isEquivalent(g1)) {
         		ChemGraph cg = ChemGraph.make(g2);
         		addResonanceIsomer(cg);
         		}
         	else if (chemGraph.getGraph().isEquivalent(g2)) {
         		ChemGraph cg = ChemGraph.make(g1,true);
         		addResonanceIsomer(cg);
         	}
         	return;
         }
         catch (ForbiddenStructureException e) {
         	return;
         }
 
 
 
         //#]
     }
 
     //## operation generateResonanceIsomersFromRadicalCenter()
     private void generateResonanceIsomersFromRadicalCenter() {
         //#[ operation generateResonanceIsomersFromRadicalCenter()
         // only radical is considered here
         if (chemGraph.getRadicalNumber() <= 0) return;
 
         addResonanceIsomer(chemGraph);
 
         Queue undoChemGraph = new Queue(4*chemGraph.getAtomNumber());
         undoChemGraph.enqueue(chemGraph);
 
         HashSet processedChemGraph = new HashSet();
         while (!undoChemGraph.isEmpty()) {
         	ChemGraph cg = (ChemGraph)undoChemGraph.dequeue();
         	HashSet radicalNode = cg.getRadicalNode();
         	Iterator radicalIter = radicalNode.iterator();
         	while (radicalIter.hasNext()) {
         		Node radical = (Node)radicalIter.next();
         		int radicalNumber = ((Atom)radical.getElement()).getRadicalNumber();
         		if (radicalNumber > 0) {
         			HashSet allPath = findAllDelocalizationPaths(radical);
         			Iterator pathIter = allPath.iterator();
         			while (pathIter.hasNext()) {
         				Stack path = (Stack)pathIter.next();
         				ChemGraph newCG = doDelocalization(cg, path);
         				if (newCG!=null && !processedChemGraph.contains(newCG)) {
         					undoChemGraph.enqueue(newCG);
         				}
         			}
         		}
         		
         	}
         	processedChemGraph.add(cg);
         }
         /*for (Iterator iter = getResonanceIsomers(); iter.hasNext(); ){
         	ChemGraph cg = (ChemGraph)iter.next();
         	makeSingletAndTriplet(cg);
         	
         }*/
         
         //#]
     }
 
     private void makeSingletAndTriplet(ChemGraph cg) {
     	HashSet radicalNode = cg.getRadicalNode();
     	Iterator radicalIter = radicalNode.iterator();
     	while (radicalIter.hasNext()) {
     		Node radical = (Node)radicalIter.next();
     		int radicalNumber = ((Atom)radical.getElement()).getRadicalNumber();
     		if (radicalNumber == 2 && radical.getFeElement().spin == null) {
     			// make singlet
     			Graph graph = Graph.copy(cg.getGraph());
     			int nodeID = radical.getID();
     			Node newNode = graph.getNodeAt(nodeID);
     			newNode.getFeElement().spin = "S";
     			
     			try {
     				ChemGraph newIsomer = ChemGraph.make(graph);
     				
     				addResonanceIsomer(newIsomer);
     			}
     			catch (ForbiddenStructureException e) {
     				
     			}
     			
 //    			 make triplet
     			Graph graph2 = Graph.copy(cg.getGraph());
     			
     			Node newNode2 = graph.getNodeAt(nodeID);
     			newNode.getFeElement().spin = "T";
     			try {
     				ChemGraph newIsomer = ChemGraph.make(graph2);
     				
     				addResonanceIsomer(newIsomer);
     			}
     			catch (ForbiddenStructureException e) {
     				
     			}
     		}
     		else if (radicalNumber == 2 && radical.getFeElement().spin.equals("T")) {
     			// make singlet
     			Graph graph = Graph.copy(cg.getGraph());
     			int nodeID = radical.getID();
     			Node newNode = graph.getNodeAt(nodeID);
     			newNode.getFeElement().spin = "S";
     			
     			try {
     				ChemGraph newIsomer = ChemGraph.make(graph);
     				
     				addResonanceIsomer(newIsomer);
     			}
     			catch (ForbiddenStructureException e) {
     				
     			}
     			
 //    			 make nonSinglet
     			Graph graph2 = Graph.copy(cg.getGraph());
     			
     			Node newNode2 = graph.getNodeAt(nodeID);
     			newNode.getFeElement().spin = null;
     			try {
     				ChemGraph newIsomer = ChemGraph.make(graph2);
     				
     				addResonanceIsomer(newIsomer);
     			}
     			catch (ForbiddenStructureException e) {
     				
     			}
     		}
     		else if (radicalNumber == 2 && radical.getFeElement().spin.equals("S")) {
     			// make triplet
     			Graph graph = Graph.copy(cg.getGraph());
     			int nodeID = radical.getID();
     			Node newNode = graph.getNodeAt(nodeID);
     			newNode.getFeElement().spin = "T";
     			try {
     				ChemGraph newIsomer = ChemGraph.make(graph);
     				
     				addResonanceIsomer(newIsomer);
     			}
     			catch (ForbiddenStructureException e) {
     				
     			}
     			
     			
 //   			 make nonSinglet
     			Graph graph2 = Graph.copy(cg.getGraph());
     			
     			Node newNode2 = graph.getNodeAt(nodeID);
     			newNode.getFeElement().spin = null;
     			try {
     				ChemGraph newIsomer = ChemGraph.make(graph2);
     				
     				addResonanceIsomer(newIsomer);
     			}
     			catch (ForbiddenStructureException e) {
     				
     			}
     		}
     		
     	}
 		
 	}
 	
 	public void generateSpectroscopicData() {
 		if (SpectroscopicData.useThreeFrequencyModel) {
 			generateThreeFrequencyModel();
 			spectroscopicData = null;
 		}
 		else {
 			spectroscopicData = FrequencyGroups.getINSTANCE().generateFreqData(this);
 			threeFrequencyModel = null;
 		}
 	}
 	
 	public void generateThreeFrequencyModel() {
         
 		// Do nothing if molecule is triatomic or smaller
 		if (isTriatomicOrSmaller()) 
 			return;
 
         // Create three frequency model for this species
         threeFrequencyModel = Therfit.generateThreeFrequencyModel(this);
 
 		therfitExecuted = (threeFrequencyModel != null);
     }
 
 	  public String getChemkinName() {
 	        //#[ operation getChemkinName()
 		  if (addID){
 			  String chemkinName = getName() + "(" + getID() + ")";
 		        if (chemkinName.length() > 16) chemkinName = "SPC(" + getID() + ")";
 		        return chemkinName;
 		  }
 		  else
 			  return getName();
 
 	        //#]
 	    }
 
     //## operation getInternalRotor()
     public int getInternalRotor() {
         //#[ operation getInternalRotor()
         return getChemGraph().getInternalRotor();
         //#]
     }
 
     //## operation getMolecularWeight()
     public double getMolecularWeight() {
         //#[ operation getMolecularWeight()
         return getChemGraph().getMolecularWeight();
         //#]
     }
 
 	   //## operation getNasaThermoData()
     public NASAThermoData getNasaThermoData() {
         //#[ operation getNasaThermoData()
         //if (nasaThermoData==null && !therfitExecuted) generateNASAThermoData();
 		if (nasaThermoData==null) generateNASAThermoData();
         return nasaThermoData;
         //#]
     }
 
     //## operation getResonanceIsomers()
     public Iterator getResonanceIsomers() {
         //#[ operation getResonanceIsomers()
         return resonanceIsomers.iterator();
         //#]
     }
 
 	public HashSet getResonanceIsomersHashSet(){
 		return resonanceIsomers;
 	}
 
     /**
     Requires:
     Effects: return the thermo data of the stablest resonance isomer.
     Modifies:
     */
     //## operation getThermoData()
     public ThermoData getThermoData() {
         //#[ operation getThermoData()
         return chemGraph.getThermoData();
         //#]
     }
 
     //## operation getThreeFrequencyMode()
     public ThreeFrequencyModel getThreeFrequencyMode() {
         //#[ operation getThreeFrequencyMode()
         if (threeFrequencyModel==null && !therfitExecuted) generateThreeFrequencyModel();
 
         return threeFrequencyModel;
         //#]
     }
 
     //## operation hasResonanceIsomers()
     public boolean hasResonanceIsomers() {
         //#[ operation hasResonanceIsomers()
         if (resonanceIsomers == null) return false;
         else return (resonanceIsomers.size() > 0);
         //#]
     }
 
     //## operation hasThreeFrequencyModel()
     public boolean hasThreeFrequencyModel() {
         //#[ operation hasThreeFrequencyModel()
         return (getThreeFrequencyModel() != null);
         //#]
     }
 	
 	public boolean hasSpectroscopicData() {
         return (spectroscopicData != null || threeFrequencyModel != null);
     }
 
     //## operation isRadical()
     public boolean isRadical() {
         //#[ operation isRadical()
         return chemGraph.isRadical();
         //#]
     }
 
     //## operation isTriatomicOrSmaller()
     public boolean isTriatomicOrSmaller() {
         //#[ operation isTriatomicOrSmaller()
         return (getChemGraph().getAtomNumber()<=3);
         //#]
     }
 
     //## operation make(String,ChemGraph)
     public static Species make(String p_name, ChemGraph p_chemGraph) {
         //#[ operation make(String,ChemGraph)
 		double pT = System.currentTimeMillis();
         SpeciesDictionary dictionary = SpeciesDictionary.getInstance();
         Species spe = (Species)(dictionary.getSpecies(p_chemGraph));
 		
         
         if (spe == null) {
         	
         	String name = p_name;
         	if (name == null || name.length()==0) {
         		name = p_chemGraph.getChemicalFormula();
         	}
 			int id= ++TOTAL_NUMBER;
 			
         	spe = new Species(id,name,p_chemGraph);
         	//spe.ID =
         	dictionary.putSpecies(spe, true);
         	
 
         }
         else {
 			if (spe.chemGraph.equals(p_chemGraph)){
 				//spe.chemGraph.graph = p_chemGraph.graph;
 				//p_chemGraph = spe.chemGraph;
 				
 				p_chemGraph.thermoData = spe.chemGraph.thermoData;
 				p_chemGraph.symmetryNumber = spe.chemGraph.symmetryNumber;
 				p_chemGraph.internalRotor = spe.chemGraph.internalRotor;
 			}
 			else if (spe.hasResonanceIsomers()){
 				Iterator cgIter = spe.getResonanceIsomers();
 				while(cgIter.hasNext()){
 					ChemGraph cg = (ChemGraph)cgIter.next();
 					if (cg.equals(p_chemGraph)){
 						p_chemGraph.thermoData = spe.chemGraph.thermoData;
 						p_chemGraph.symmetryNumber = spe.chemGraph.symmetryNumber;
 						p_chemGraph.internalRotor = spe.chemGraph.internalRotor;
 						break;
 					}
 				}
 			}
 			else {
 				System.out.println("Cannot make species which has a chemgraph: "+p_chemGraph.toString());
 				System.exit(0);
 			}
         }
 		p_chemGraph.setSpecies(spe);
 		Global.makeSpecies += (System.currentTimeMillis()-pT)/1000/60;
         return spe;
         //#]
     }
 
 //	## operation make(String,ChemGraph)
     /*public static Species make(String p_name, Graph p_graph) throws InvalidChemGraphException, ForbiddenStructureException {
         //#[ operation make(String,ChemGraph)
 		double pT = System.currentTimeMillis();
         SpeciesDictionary dictionary = SpeciesDictionary.getInstance();
         Species spe = dictionary.getSpeciesFromGraph(p_graph);
 		
         if (spe == null) {
         	ChemGraph cg = ChemGraph.make(p_graph);
 			spe = make(null, cg);
 			cg.setSpecies(spe);
 			
         }
         
         return spe;
         //#]
     }*/
 	
 	public static Species make(String p_name, ChemGraph p_chemGraph, int id) {
         //#[ operation make(String,ChemGraph)
         SpeciesDictionary dictionary = SpeciesDictionary.getInstance();
         Species spe = (Species)(dictionary.getSpecies(p_chemGraph));
 
         if (spe == null) {
         	String name = p_name;
         	if (name == null || name.length()==0) {
         		name = p_chemGraph.getChemicalFormula();
         	}
         	spe = new Species(id, name,p_chemGraph);
 			if (id > TOTAL_NUMBER) TOTAL_NUMBER=id;
         	dictionary.putSpecies(spe, false);
  
         }
         p_chemGraph.setSpecies(spe);
         return spe;
         //#]
     }
 
     //## operation repOk()
     public boolean repOk() {
         //#[ operation repOk()
         if (name == null || name.length() == 0) return false;
 
         if (ID < 0 || ID > TOTAL_NUMBER) return false;
 
         if (chemGraph != null && !chemGraph.repOk()) return false;
 
         Iterator iter = resonanceIsomers.iterator();
         while (iter.hasNext()) {
         	ChemGraph cg = (ChemGraph)iter.next();
         	if (cg == null || !cg.repOk()) return false;
         }
 
         return true;
         //#]
     }
 
     //## operation selectDeltaEDown()
     public void selectDeltaEDown() {
         //#[ operation selectDeltaEDown()
         String name = getName();
 
         if (name.equals("AR")) deltaEDown = 374.0;
         else if (name.equals("H2")) deltaEDown = 224.0;
         else if (name.equals("O2")) deltaEDown = 517.0;
         else if (name.equals("N2")) deltaEDown = 461.0;
         else if (name.equals("He")) deltaEDown = 291.0;
         else if (name.equals("CH4")) deltaEDown = 1285.0;
         else if (name.equals("HO2")) deltaEDown = 975.0;
         else if (name.equals("H2O2")) deltaEDown = 975.0;
         else if (name.equals("CO2")) deltaEDown = 417.0;
         else if (name.equals("CO")) deltaEDown = 283.0;
 
         return;
 
 
 
         //#]
     }
 
     //## operation selectLJParametersForSpecialMolecule()
     public void selectLJParametersForSpecialMolecule() {
         //#[ operation selectLJParametersForSpecialMolecule()
         String name = getName();
         if (name.equals("H2")) LJ = new LennardJones(2.8327, 59.7);
         else if (name.equals("O2")) LJ = new LennardJones(3.467, 106.7);
         else if (name.equals("H2O")) LJ = new LennardJones(2.641,809.1);
         else if (name.equals("H2O2")) LJ = new LennardJones(4.196,289.3);
         else if (name.equals("CO2")) LJ = new LennardJones(3.941,195.2);
         else if (name.equals("CO")) LJ = new LennardJones(3.690,91.7);
         return;
         //#]
     }
 	public String toChemkinString() {
 		//#[ operation toChemkinString()
 		return getChemkinName();
 		//#]
 	}
 
     //## operation toString()
     public String toString() {
         //#[ operation toString()
         String s = "Species " + String.valueOf(ID) + '\t';
         s = s + "Name: " + getName() + '\n';
         ChemGraph primary = getChemGraph();
         s = s + primary.toString();
 
         int index=0;
         for (Iterator iter = resonanceIsomers.iterator(); iter.hasNext(); ) {
         	index++;
         	ChemGraph isomer = (ChemGraph)iter.next();
         	if (isomer!=primary) {
         		s = s + '\n';
         		s = s + "isomer" + String.valueOf(index) + ":\n";
         		s = s + isomer.toString();
         	}
         }
         return s;
     }
 
 //		## operation toString()
 	    public String toString(int i) {
 	        //#[ operation toString()
 			String s ="";
 			ChemGraph primary = getChemGraph();
 	        s = s + primary.toString(i);
 
 	        int index=0;
 	        /*for (Iterator iter = resonanceIsomers.iterator(); iter.hasNext(); ) {
 	        	index++;
 	        	ChemGraph isomer = (ChemGraph)iter.next();
 	        	if (isomer!=primary) {
 	        		s = s + '\n';
 	        		s = s + "isomer" + String.valueOf(index) + ":\n";
 	        		s = s + isomer.toStringWithoutH(i);
 	        	}
 	        }*/
 	        return s;
     }
 
     //## operation toStringWithoutH()
     public String toStringWithoutH() {
         //#[ operation toStringWithoutH()
         String s = "Species " + String.valueOf(ID) + '\t';
         s = s + "Name: " + getName() + '\n';
         ChemGraph primary = getChemGraph();
         s = s + primary.toStringWithoutH();
 
         int index=0;
         for (Iterator iter = resonanceIsomers.iterator(); iter.hasNext(); ) {
         	index++;
         	ChemGraph isomer = (ChemGraph)iter.next();
         	if (isomer!=primary) {
         		s = s + '\n';
         		s = s + "isomer" + String.valueOf(index) + ":\n";
         		s = s + isomer.toStringWithoutH();
         	}
         }
         return s;
 
 
 
 
         //#]
     }
 
 	public String toStringWithoutH(int i) {
         //#[ operation toStringWithoutH()
         String s ="";
 		ChemGraph primary = getChemGraph();
         s = s + primary.toStringWithoutH(i);
 
         int index=0;
         /*for (Iterator iter = resonanceIsomers.iterator(); iter.hasNext(); ) {
         	index++;
         	ChemGraph isomer = (ChemGraph)iter.next();
         	if (isomer!=primary) {
         		s = s + '\n';
         		s = s + "isomer" + String.valueOf(index) + ":\n";
         		s = s + isomer.toStringWithoutH(i);
         	}
         }*/
         return s;
 
 
 
 
         //#]
     }
 
     public int getID() {
         return ID;
     }
 
     public static int getTOTAL_NUMBER() {
         return TOTAL_NUMBER;
     }
 
     public ChemGraph getChemGraph() {
         return chemGraph;
     }
 
     public void setChemGraph(ChemGraph p_chemGraph) {
         chemGraph = p_chemGraph;
     }
 
     public double getDeltaEDown() {
         return deltaEDown;
     }
 
     public String getName() {
         return name;
     }
 
     public void setName(String p_name) {
         name = p_name;
     }
 
     public boolean getTherfitExecuted() {
         return therfitExecuted;
     }
 
     public LennardJones getLJ() {
         return LJ;
     }
 
     public LennardJones newLJ() {
         LJ = new LennardJones();
         return LJ;
     }
 
     public void deleteLJ() {
         LJ=null;
     }
 
     public ThreeFrequencyModel getThreeFrequencyModel() {
         return threeFrequencyModel;
     }
 
     public SpectroscopicData getSpectroscopicData() {
         return spectroscopicData;
     }
 
 	protected void initRelations() {
         LJ = newLJ();
     }
 
     public boolean isReactive() {
     	return IsReactive;
     }	
     
     public void setReactivity(boolean reactive) {
     	IsReactive = reactive;
     }
     
 	public void addPdepPaths(HashSet pdepReactionSet) {
 		
 		if (paths == null)
 			paths = pdepReactionSet;
 		else
 			paths.addAll(pdepReactionSet);
 	}
 	
 	public HashSet getPdepPaths(){
 		return paths;
 	}
 	public static void setAddID(boolean p_addID){
 		addID = p_addID;
 	}
 }
 /*********************************************************************
 	File Path	: RMG\RMG\jing\chem\Species.java
 *********************************************************************/
 
