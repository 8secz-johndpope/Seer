 package camml.core.newgui;
 
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.io.StringReader;
 import java.util.Random;
 
 import camml.core.library.WallaceRandom;
 import camml.core.models.ModelLearner;
 import camml.core.models.bNet.BNet;
 //import camml.core.models.bNet.BNetVisualiser;
 import camml.core.search.CaseInfo;
 import camml.core.search.MMLEC;
 import camml.core.search.MetropolisSearch;
 import camml.core.search.SearchPackage;
 import camml.plugin.friedman.FriedmanWrapper;
 import camml.plugin.netica.NeticaFn;
 import camml.plugin.rodoCamml.RodoCammlIO;
 import camml.plugin.tomCoster.ExpertElicitedTOMCoster;
 import camml.plugin.weka.Converter;
 import cdms.core.Type;
 import cdms.core.Value;
 
 /**Code for actually running a search. 
  * Designed to work closely with the cammLGUI class: i.e. a GUIModel object
  *  is passed to the cammlGUI constructor.
  * 
  * Much of this class amounts to a wrapper for existing CaMML methods, as well
  *  as checks on the parameters selected by the user via the GUI
  * 
  * @author Alex
  *
  */
 public class GUIModel implements GUIAvailableParameters {
 	protected MetropolisSearch metropolisSearch = null;
 	protected CaseInfo caseInfo = null;
 	
 	//Data:
 	Value.Vector data = null;
 	
 	//Search parameters:
 	protected ModelLearner MMLLearner = null;
 	protected double searchFactor = 1.0;
 	protected int maxSECs = 30;
 	protected double minTotalPosterior = 0.999;
 	
 	protected File selectedFile = null;			//Used mainly for path...
 	protected File lastExportedBNet = null;
 	
 	protected boolean fullResults = false;		//Should full results be generated after running search?
 	
 	protected Random r = RNGs[0];				//Random number generator. Defined in GUIAvailableParameters interface
 	protected boolean useSetSeed = RNGUseSetSeed[0];
 	protected boolean useSetSeed2 = RNGUseSetSeed2[0];
 	//Note: random seeds for WallaceRandom must be integers, seed for java.util.Random is long (hence integer OK)
 	protected int randomSeed = new Random().nextInt();	//Updated based on what user enters in GUI...
 	protected int randomSeed2 = new Random().nextInt();	//Wallace Random needs 2 parameters for seed...
 	
 	//Expert Priors
 	protected boolean useExpertPriors = false;
 	String expertPriorsString = null;			//String of the expert priors entered by the user
 	
 	//Full results:
 	protected Value.Vector searchResults = null;
 	
 
 	
 
 	/**Outputs the best scoring network from search to the specified location.
 	 * @param filepath Path (including filename) to export best scoring network to.
 	 */
 	public void outputNeticaBN( String filepath ) throws Exception {
 		if( metropolisSearch == null || !metropolisSearch.isFinished() )
 			throw new Exception("Cannot produce network if search has not been run.");
 
 		
 		BNet outputNet = metropolisSearch.getBNet();
 		
 		//Try exporting best TOM to Netica format:
 		Value.Vector params = null;
 		try{
 			params = metropolisSearch.getBestTOM().makeParameters( MMLLearner );
 		} catch( Exception e ){
 			System.out.println("Error: makeParameters for BestTOM failed!");
 		}
 		
 		
 		Value.Structured saveStruct = new Value.DefStructured( new Value[] {
                 new Value.Str(filepath), outputNet, params
             });
 		
 		try{
 			NeticaFn.SaveNet SaveNet = new NeticaFn.SaveNet();
 			SaveNet.apply(saveStruct);
 		} catch( Exception e ){
 			System.out.println("Error: Saving netica BN failed!");
 		}
 	}
 	
 	/**Export ONE network (Netica format) from the set of 'representative networks'
 	 *  in the full results to a specified location.
 	 * (i.e. after combining TOMs -> SECs -> MMLECs ) 
 	 * 
 	 * @param filepath Path and file name to save the specified network. 
 	 * @param index Index of network in full results array. NOTE: index appended to filename automatically.
 	 * @throws Exception
 	 */
 	public void outputFullResultsBN( String filepath, int index ) throws Exception {		
 		if( metropolisSearch == null || !metropolisSearch.isFinished() ) throw new Exception("Cannot produce network if search has not been run.");
 		if( searchResults == null ) throw new Exception("Search results: Not generated.");
 		if( index > searchResults.length()-1 || index < 0 ) throw new Exception("Invalid Index.");
 		
 		//Generate the file path:
 		String path;
 		if( filepath.endsWith(".dne") ){
 			path = filepath.substring(0, filepath.length()-4);
 		} else{
 			path = filepath;
 		}
 		
 		path = path + index;	//Concatenate number at end
 		path = path + ".dne";
 		
 		Value.Structured repNetwork = (Value.Structured)MMLEC.getRepresentative.apply( searchResults.elt(index) );
 		
 		//File path, Network, parameters
 		Value.Structured saveStruct = new Value.DefStructured( new Value[] {
                 new Value.Str(path), (BNet)repNetwork.cmpnt(0), (Value.Vector)repNetwork.cmpnt(1)
             });
 		
 		try{
 			NeticaFn.SaveNet SaveNet = new NeticaFn.SaveNet();
 			SaveNet.apply(saveStruct);
 		} catch( Exception e ){
 			System.out.println("Error: Saving netica BN failed!");
 		}
 		
 	}
 	
 	/**Exports ALL networks (Netica format) from the set of 'representative networks'
 	 *  in the full results to a specified location.
 	 *  NOTE: Number of network (sorted by posterior) appended to filename.
 	 * 
 	 * @param filepath Path and filename to export networks to.
 	 * @throws Exception
 	 */
 	public void outputFullResultsAllBNs( String filepath ) throws Exception{
 		if( metropolisSearch == null || !metropolisSearch.isFinished() ) throw new Exception("Cannot produce network if search has not been run.");
 		if( searchResults == null ) throw new Exception("Search results: Not generated.");
 		
 		for( int i = 0; i < searchResults.length(); i++ ){
 			outputFullResultsBN( filepath, i );
 		}
 	}
 	
 	
 	//Load data file:
 	public void loadDataFile( File f ) throws Exception
 	{
 		loadDataFile( f.getAbsolutePath() );
 	}
 	
 	/**Load data from a file. 
 	 * Currently supported formats: arff, cas and data 
 	 * @param path Location of data file (inc. file name)
 	 * @throws Exception If file not found, error loading file, etc
 	 */
 	public void loadDataFile( String path ) throws Exception
 	{
 		if( path.endsWith( "arff" ) ){
 			try{
 				//TODO: Currently set NOT to discretize continuous or replace missing values. Need to fix this.
 				data = Converter.load( path, false, false);
 				//System.out.println("File loaded OK");
 			} catch( FileNotFoundException e ){
 				data = null;
 				throw new Exception( "File not found." );
 			} catch( IOException e ){
 				data = null;
 				throw new Exception( "IO Exception" );
 			}
 			catch( Exception e ){
 				data = null;
 				throw new Exception( "Error loading file" );
 			}
 			return;
 		}
 		
 		if( path.endsWith( "cas" ) ){
 			try{
 				data = RodoCammlIO.load( path );
 			} catch( FileNotFoundException e ){
 				data = null;
 				throw new Exception( "File not found." );
 			} catch( IOException e ){
 				data = null;
 				throw new Exception( "IO Exception" );
 			}
 			catch( Exception e ){
 				data = null;
 				throw new Exception( "Error loading file" );
 			}
 			return;
 		}
 		
 		//TODO: Currently untested. FriedmanWrapper.loadData(...) seems to require a ".names" file in addition to the ".data" file???
 		if( path.endsWith( "data" ) ){
 			try{
 				data = FriedmanWrapper.loadData( path );
 			} catch( FileNotFoundException e ){
 				data = null;
 				throw new Exception( "File not found." );
 			} catch( IOException e ){
 				data = null;
 				throw new Exception( "IO Exception" );
 			}
 			catch( Exception e ){
 				data = null;
 				throw new Exception( "Error loading file" );
 			}
 			return;
 		}
 		
 		//If file extension not matched by now: Unknown format.
 		data = null;
 		throw new Exception("Unknown file format.");
 		
 	}
 	
 	//Returns true if not continuous; false otherwise
 	//TODO: Weka plugin does have the ability to deal with missing and continuous data: this needs to be implemented/tested!
 	public boolean checkDataNotContinuous(){
 		if( data == null ) return true;
 		
 		//First: determine number of variables:
 		//TODO: Surely there must be a better way to find number of variables?
 		int numVars = ((Type.Structured)((Type.Vector)data.t).elt).labels.length;
 		
 		for( int i = 0; i < numVars; i++ ){
 			if( data.cmpnt(i).elt(0).t instanceof cdms.core.Type.Continuous ){
 				return false;
 			}
 		}
 		
 		return true;
 	}
 	
 	/**Validates format of expert priors, entered as a string.
 	 * Note: Validity of a string of priors depends on data (for variable names etc)
 	 * @param ExpertPriors String encoding the expert priors
 	 * @return True if format acceptable, false if it has errors
 	 * @throws Exception If format invalid (exception text has reason + line number why)
 	 */
 	public boolean validateExpertPriors( String ExpertPriors ) throws Exception
 	{
 		StringReader rd = new StringReader( ExpertPriors );
 		try{
 			ExpertElicitedTOMCoster tempCoster = new ExpertElicitedTOMCoster( 0.5, rd, data );
 		} catch( RuntimeException e ){
 			//Constructor threw exception -> error in Expert Prior text...
 			throw new Exception( e );
 		}
 		return true;
 	}
 	
 	
 	public boolean dataValid(){
 		//TODO: More checks? i.e. only discrete or symbolic, not continuous...
 		return data != null;
 	}
 	
 	public boolean MMLLearnerValid(){
 		return MLLearner != null;
 	}
 	
 	/**Method to check if expert priors OK
 	 * @return True if expert priors OK, or expert priors are not used.
 	 */
 	public boolean expertPriorsValid(){
 		if( useExpertPriors ){
 			if( expertPriorsString == null ) return false;
 			try{
 				validateExpertPriors( expertPriorsString );
 			} catch( Exception e ){
 				return false;
 			}
 		}
 		
 		return true;
 	}
 	
 	public boolean searchFactorValid(){
 		return ( searchFactor >= 0.1 && searchFactor <= 20 );
 	}
 	
 	public boolean maxSECsValid(){
 		return ( maxSECs >= 3 );
 	}
 	
 	public boolean minTotalPosteriorValid(){
 		return ( minTotalPosterior >=0.3 && minTotalPosterior <= 1.0 );
 	}
 	
 	public boolean randomSeedValid(){
 		//TODO
 		return true;
 	}
 	
 	public boolean randomSeed2Valid(){
 		//TODO
 		return true;
 	}
 	
 	/**Method that actually runs the full Metropolis Search.
 	 * Does not check if data/parameters are valid: assumes these checks
 	 *  have already been conducted.
 	 * Once search complete: results can be obtained from the appropriate GUIModel object:
 	 *  - GUIModel.metropolisSearch
 	 *  - GUIModel.searchResults
 	 */
 	public void runSearch(){
 		//Create metropolis search object:
 		metropolisSearch = new MetropolisSearch( r, data, MLLearner, MMLLearner );
 		
 		//Change settings:
 		metropolisSearch.caseInfo.searchFactor = searchFactor;				//No separate value kept in MetropolisSearch
 		metropolisSearch.caseInfo.maxNumSECs = maxSECs;						//No separate value kept in MetropolisSearch
 		metropolisSearch.caseInfo.minTotalPosterior = minTotalPosterior;	//No separate value kept in MetropolisSearch
 		
 		//Reset search results in case already run before this session:
 		searchResults = null;
 		
 		//Display whether using set RNG seed or random, plus what the seed is:
 		System.out.print("RNG = " + r );
 		if( useSetSeed ){
 			if( r.getClass() == Random.class ){	//Java random
 				System.out.println("; Set seed = " + randomSeed );
 				r.setSeed(randomSeed);
 			} else if( r.getClass() == WallaceRandom.class ){
 				System.out.println("; Set seeds = " + randomSeed + " & " + randomSeed2 );
 				((WallaceRandom)r).setSeed( new int[]{ randomSeed, randomSeed2 } );
 			}
 		} else{
 			System.out.println("; Random seed");
 		}
 		
 		//Run the MetropolisSearch algorithm until finished:
 		int count = 0;
 		while( !metropolisSearch.isFinished() ){
 			metropolisSearch.doEpoch();
 			count++;
 		}
 		System.out.println("\n\nSearch finished... " + count + " loops");
 		
 		if( fullResults ){
 			System.out.println("\n\n===================================");
 			System.out.println("----- Generating full results -----");
 			generateFullResults();
 		}
 		
 	}
 	
 	/**Generate full results.
 	 * After calling this function, results can be obtained from: GUIModel.searchResults
 	 * @return True if results generated successfully, false otherwise (i.e. search not run)
 	 */
 	public boolean generateFullResults(){
 		if( metropolisSearch == null ) return false;
 		if( !metropolisSearch.isFinished() ) return false;
 		
 		searchResults = metropolisSearch.getResults();
 		return true;
 		
 	}
 	
 	/**Generates a netica BN for the best TOM found during search as a String
 	 * String intended to be used by BNetViewer class to display a network.
 	 * @return String representation of the single best TOM generated during search.
 	 * @throws Exception If search not run; if error during creation of parameters.
 	 */
 	public String generateBestNeticaNetworkString() throws Exception
 	{
 		if( !metropolisSearch.isFinished() ) throw new Exception("Cannot produce network if search has not been run.");
 		
 		BNet network = metropolisSearch.getBNet();
 		Value.Vector parameters;
 
 		try{
 			parameters = metropolisSearch.getBestTOM().makeParameters( metropolisSearch.getMMLModelLearner() );
 		} catch( Exception e ){
 			throw new Exception("Error: makeParameters for BestTOM failed!");
 		}
 		
 		String filename = selectedFile.getName();
 		if( filename == null || filename.equals("") ) filename = "Network";
 		
 		return network.export( filename, parameters, "netica");
 	}
 	
 	/**Return one of the representative networks (from full results) as a String
 	 * String intended to be used by BNetViewer class to display a network.
 	 * @param index Index of the representative network to be returned as a string.
 	 * @return String of network.
 	 * @throws Exception If search not run, full results not generated, or bad index passed.
 	 */
 	public String generateNetworkStringFullResults( int index ) throws Exception
 	{
 		if( metropolisSearch == null || !metropolisSearch.isFinished() ) throw new Exception("Cannot produce network if search has not been run.");
 		if( searchResults == null ) throw new Exception("Search results: Not generated.");
 		if( index > searchResults.length()-1 || index < 0 ) throw new Exception("Invalid Index.");
 		
 		//(Model,params)
 		Value.Structured repNetwork = (Value.Structured)MMLEC.getRepresentative.apply( searchResults.elt(index) );
 		
 		BNet network = (BNet)repNetwork.cmpnt(0);
 		
 		String filename = selectedFile.getName();
 		if( filename == null || filename.equals("") ) filename = "Network";
 		filename = filename + " - " + index;
 		
 		return network.export( filename, (Value.Vector)repNetwork.cmpnt(1), "netica");
 	}
 }
