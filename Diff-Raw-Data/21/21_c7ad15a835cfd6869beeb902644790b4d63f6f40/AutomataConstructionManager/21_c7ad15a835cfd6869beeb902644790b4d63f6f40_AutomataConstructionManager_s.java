 package org.semanticweb.HermiT.structural;
 
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.Set;
 
 import org.semanticweb.HermiT.graph.Graph;
 import org.semanticweb.HermiT.structural.OWLAxioms.ComplexObjectPropertyInclusion;
 import org.semanticweb.owlapi.model.OWLObjectInverseOf;
 import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
 
 import rationals.Automaton;
 import rationals.NoSuchStateException;
 import rationals.State;
 import rationals.Transition;
 import rationals.transformations.Normalizer;
 import rationals.transformations.Reducer;
 
 public class AutomataConstructionManager {
 	
 	protected final Set<OWLObjectPropertyExpression> m_nonSimpleRoles;
 	
 	public AutomataConstructionManager(){
 		m_nonSimpleRoles = new HashSet<OWLObjectPropertyExpression>();
 	}
 
 	public Map<OWLObjectPropertyExpression,Automaton> createAutomata(Collection<OWLObjectPropertyExpression[]> simpleObjectPropertyInclusions,Collection<ComplexObjectPropertyInclusion> complexObjectPropertyInclusions) {
 		Map<OWLObjectPropertyExpression,Set<OWLObjectPropertyExpression>> equivalentPropertiesMap = findEquivalentRoles( simpleObjectPropertyInclusions );
 		Map<OWLObjectPropertyExpression,Set<OWLObjectPropertyExpression>> inverseRolesMap = buildInverseRolesMap( simpleObjectPropertyInclusions );
         Graph<OWLObjectPropertyExpression> propertyDependencyGraph = buildPropertyOrdering( simpleObjectPropertyInclusions, complexObjectPropertyInclusions, equivalentPropertiesMap );
         checkForRegularity( propertyDependencyGraph );
         
         Graph<OWLObjectPropertyExpression> complexRolesDependencyGraph = propertyDependencyGraph.clone();  
         Map<OWLObjectPropertyExpression,Automaton> individualAutomata = buildIndividualAutomata( complexRolesDependencyGraph, simpleObjectPropertyInclusions, complexObjectPropertyInclusions );
         Set<OWLObjectPropertyExpression> simpleRoles = findSimpleRoles( complexRolesDependencyGraph, individualAutomata );
         
         propertyDependencyGraph.removeElements( simpleRoles );
         complexRolesDependencyGraph.removeElements( simpleRoles );
 		m_nonSimpleRoles.addAll( complexRolesDependencyGraph.getElements() );
 
 		Map<OWLObjectPropertyExpression,Automaton> connectedAutomata = connectAllAutomata(propertyDependencyGraph,inverseRolesMap,individualAutomata);
 
 		return connectedAutomata;
 	}
 	private Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> buildInverseRolesMap(
 			Collection<OWLObjectPropertyExpression[]> simpleObjectPropertyInclusions) {
 		Map<OWLObjectPropertyExpression,Set<OWLObjectPropertyExpression>> inverseRolesMap = new HashMap<OWLObjectPropertyExpression,Set<OWLObjectPropertyExpression>>(); 
 		
 		for (OWLObjectPropertyExpression[] inclusion : simpleObjectPropertyInclusions)
 			if( inclusion[1] instanceof OWLObjectInverseOf ){
 				Set<OWLObjectPropertyExpression> inverseRoles = inverseRolesMap.get( inclusion[0] );
 				if( inverseRoles == null )
 					inverseRoles = new HashSet<OWLObjectPropertyExpression>();
 				inverseRoles.add( inclusion[1].getInverseProperty().getSimplified() );
 				inverseRolesMap.put(inclusion[0], inverseRoles);
 			}
 		return inverseRolesMap;
 	}
 
 	private Map<OWLObjectPropertyExpression,Set<OWLObjectPropertyExpression>> findEquivalentRoles(Collection<OWLObjectPropertyExpression[]> simpleObjectPropertyInclusions ){
 		Graph<OWLObjectPropertyExpression> propertyDependencyGraph = new Graph<OWLObjectPropertyExpression>();
 		Map<OWLObjectPropertyExpression,Set<OWLObjectPropertyExpression>> equivalentObjectPropertiesMapping = new HashMap<OWLObjectPropertyExpression,Set<OWLObjectPropertyExpression>>();
 	  	for (OWLObjectPropertyExpression[] inclusion : simpleObjectPropertyInclusions){
     		if( !inclusion[0].equals( inclusion[1] ) && !inclusion[0].equals( inclusion[1].getInverseProperty().getSimplified() )){
     			propertyDependencyGraph.addEdge( inclusion[0], inclusion[1] );
     		}
 	  	}
 		
 	  	propertyDependencyGraph.transitivelyClose();
 
 	  	for(OWLObjectPropertyExpression objExpr : propertyDependencyGraph.getElements()){
 	  		if( propertyDependencyGraph.getSuccessors( objExpr ).contains( objExpr ) || 
 	  			propertyDependencyGraph.getSuccessors( objExpr ).contains( objExpr.getInverseProperty().getSimplified() )){
 	  			Set<OWLObjectPropertyExpression> equivPropertiesSet = new HashSet<OWLObjectPropertyExpression>();
 	  			for( OWLObjectPropertyExpression succ : propertyDependencyGraph.getSuccessors( objExpr ) ){
 	  				if( !succ.equals( objExpr ) && (propertyDependencyGraph.getSuccessors( succ ).contains( objExpr ) || 
 	  												propertyDependencyGraph.getSuccessors( succ ).contains( objExpr.getInverseProperty().getSimplified() )) )
 	  					equivPropertiesSet.add( succ );
 	  			}
 	  			equivalentObjectPropertiesMapping.put( objExpr, equivPropertiesSet);
 	  		}
 	  	}
 		return equivalentObjectPropertiesMapping;
 	}
 	private Set<OWLObjectPropertyExpression> findSimpleRoles(Graph<OWLObjectPropertyExpression> complexRolesDependencyGraph,Map<OWLObjectPropertyExpression, Automaton> individualAutomata) {
 		Set<OWLObjectPropertyExpression> simpleRoles = new HashSet<OWLObjectPropertyExpression>();
 		
 		Graph<OWLObjectPropertyExpression> complexRolesDependencyGraphWithInverses = complexRolesDependencyGraph.clone(); 
 		
 		for( OWLObjectPropertyExpression complexOWLProp1 : complexRolesDependencyGraph.getElements() )
 			for( OWLObjectPropertyExpression complexOWLProp2 : complexRolesDependencyGraph.getSuccessors( complexOWLProp1 )) 
 				complexRolesDependencyGraphWithInverses.addEdge( complexOWLProp1.getInverseProperty().getSimplified(), complexOWLProp2.getInverseProperty().getSimplified() );
 		
 		Graph<OWLObjectPropertyExpression> invertedGraph = complexRolesDependencyGraphWithInverses.getInverse();
 		invertedGraph.transitivelyClose();
 
 		for(OWLObjectPropertyExpression owlProp : invertedGraph.getElements() ){
 			boolean someComplexSubRole = false;
 			for( OWLObjectPropertyExpression subDependingProperties : invertedGraph.getSuccessors( owlProp )){
 				if( individualAutomata.containsKey( subDependingProperties ) || 
 					individualAutomata.containsKey( subDependingProperties.getInverseProperty().getSimplified() ) ){
 					someComplexSubRole = true;
 					break;
 				}
 			}
 			if( !someComplexSubRole && !individualAutomata.containsKey( owlProp ) && !individualAutomata.containsKey( owlProp.getInverseProperty().getSimplified() ))
 				simpleRoles.add( owlProp );
 		}
 		return simpleRoles;
 	}
 	private Map<OWLObjectPropertyExpression, Automaton> connectAllAutomata(Graph<OWLObjectPropertyExpression> propertyDependencyGraph , Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> inverseRolesMap, Map<OWLObjectPropertyExpression, Automaton> individualAutomata) {
     	Graph<OWLObjectPropertyExpression> transClosedGraph = propertyDependencyGraph.clone();
     	transClosedGraph.transitivelyClose();
     	
     	Set<OWLObjectPropertyExpression> rolesToStartRecursion = new HashSet<OWLObjectPropertyExpression>();
     	for( OWLObjectPropertyExpression owlProp : transClosedGraph.getElements() )
     		if( transClosedGraph.getSuccessors( owlProp ).isEmpty() )
     			rolesToStartRecursion.add( owlProp );
 
     	Graph<OWLObjectPropertyExpression> inversePropertyDependencyGraph = propertyDependencyGraph.getInverse();
 
     	Map<OWLObjectPropertyExpression,Automaton> completeAutomata = new HashMap<OWLObjectPropertyExpression,Automaton>();
     	for( OWLObjectPropertyExpression superRole : rolesToStartRecursion )
     		buildCompleteAutomataForRoles( superRole, inverseRolesMap, individualAutomata, completeAutomata, inversePropertyDependencyGraph );
     	
     	for( OWLObjectPropertyExpression owlProp : individualAutomata.keySet() )
     		if( !completeAutomata.containsKey( owlProp ) ){
     			Automaton atomicAutomaton = individualAutomata.get( owlProp );
     			atomicAutomaton = minimizeAndNormalizeAutomaton( atomicAutomaton );
     			completeAutomata.put( owlProp, atomicAutomaton);
     		}
 
     	return completeAutomata;
 	}
 	private Automaton buildCompleteAutomataForRoles(OWLObjectPropertyExpression roleToBuildAutomaton, Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> inverseRolesMap, Map<OWLObjectPropertyExpression, Automaton> individualAutomata, Map<OWLObjectPropertyExpression, Automaton> completeAutomata, Graph<OWLObjectPropertyExpression> inversedPropertyDependencyGraph) {

        
 		if( completeAutomata.containsKey( roleToBuildAutomaton ) )
 			return completeAutomata.get( roleToBuildAutomaton );
 		else if( completeAutomata.containsKey( roleToBuildAutomaton.getInverseProperty().getSimplified() ) && 
 				 !individualAutomata.containsKey( roleToBuildAutomaton )){
 			Automaton mirroredCopy = getMirroredCopy( completeAutomata.get( roleToBuildAutomaton.getInverseProperty().getSimplified() ));
 			mirroredCopy = minimizeAndNormalizeAutomaton( mirroredCopy );
 			completeAutomata.put( roleToBuildAutomaton, mirroredCopy );
 			return mirroredCopy;
 		}
 			
 		if( inversedPropertyDependencyGraph.getSuccessors( roleToBuildAutomaton ).isEmpty() ){
 			Automaton autoForLeafProperty = individualAutomata.get( roleToBuildAutomaton );
 			if( autoForLeafProperty == null ){
 				Set<OWLObjectPropertyExpression> inverses = inverseRolesMap.get( roleToBuildAutomaton );
 				boolean noInverseRoleWithAutomaton = true;
 				for( OWLObjectPropertyExpression inverse : inverses )
 					if( individualAutomata.containsKey( inverse ) ){
 						autoForLeafProperty = getMirroredCopy( buildCompleteAutomataForRoles( inverse, inverseRolesMap, individualAutomata, completeAutomata, inversedPropertyDependencyGraph ) );
 						autoForLeafProperty = minimizeAndNormalizeAutomaton( autoForLeafProperty );
 						completeAutomata.put( roleToBuildAutomaton , autoForLeafProperty );
 						noInverseRoleWithAutomaton = false;
 						break;
 					}
 				if( noInverseRoleWithAutomaton ){
 					autoForLeafProperty = new Automaton();
 					State initial = autoForLeafProperty.addState(true , false );
 					State accepting = autoForLeafProperty.addState(false , true );
 					try {
 						autoForLeafProperty.addTransition( new Transition(initial, roleToBuildAutomaton, accepting ) );
 					} catch (NoSuchStateException e) {
 						throw new IllegalArgumentException( "Could not create automaton for role at the bottom of hierarchy (simple role)");
 					}
 					autoForLeafProperty = minimizeAndNormalizeAutomaton( autoForLeafProperty );
 					completeAutomata.put( roleToBuildAutomaton , autoForLeafProperty );
 	        		if( !individualAutomata.containsKey( roleToBuildAutomaton.getInverseProperty().getSimplified() ) ){
 	        			Automaton mirroredCopy = getMirroredCopy( autoForLeafProperty );
 	        			mirroredCopy = minimizeAndNormalizeAutomaton( mirroredCopy );
 	        			completeAutomata.put( roleToBuildAutomaton.getInverseProperty().getSimplified() , mirroredCopy );
 	        		}
 				}
 			}
 			else{
 				autoForLeafProperty = minimizeAndNormalizeAutomaton( autoForLeafProperty );
 				completeAutomata.put( roleToBuildAutomaton , autoForLeafProperty );
         		if( !individualAutomata.containsKey( roleToBuildAutomaton.getInverseProperty().getSimplified() ) ){
         			Automaton mirroredCopy = getMirroredCopy( autoForLeafProperty );
         			mirroredCopy = minimizeAndNormalizeAutomaton( mirroredCopy );
         			completeAutomata.put( roleToBuildAutomaton.getInverseProperty().getSimplified() , mirroredCopy );
         		}
 			}
 			return autoForLeafProperty;
 		}
 		else{
 			Automaton autoOfBiggerRole = individualAutomata.get( roleToBuildAutomaton );
 			if( autoOfBiggerRole == null ){
 				autoOfBiggerRole = new Automaton();
 				State initialState = autoOfBiggerRole.addState( true , false );
 				State finalState = autoOfBiggerRole.addState( false , true);
        			Transition trans = new Transition(initialState, roleToBuildAutomaton , finalState);
         		try {
         			autoOfBiggerRole.addTransition( trans );
         		} catch (NoSuchStateException e) {
 					throw new IllegalArgumentException("Could not create automaton");
 				}
         		for( OWLObjectPropertyExpression smallerRole : inversedPropertyDependencyGraph.getSuccessors( roleToBuildAutomaton ) ){
 	        		Automaton autoOfSmallerRole = buildCompleteAutomataForRoles( smallerRole, inverseRolesMap, individualAutomata, completeAutomata, inversedPropertyDependencyGraph );
 	        		connectAutomata( autoOfBiggerRole, autoOfSmallerRole, trans );
 		        }
         		autoOfBiggerRole = minimizeAndNormalizeAutomaton( autoOfBiggerRole );
         		completeAutomata.put( roleToBuildAutomaton , autoOfBiggerRole );
         		if( !individualAutomata.containsKey( roleToBuildAutomaton.getInverseProperty().getSimplified() ) ){
         			Automaton mirroredCopy = getMirroredCopy( autoOfBiggerRole );
         			mirroredCopy = minimizeAndNormalizeAutomaton( mirroredCopy );
         			completeAutomata.put( roleToBuildAutomaton.getInverseProperty().getSimplified() , mirroredCopy );
         		}
 			}
 			else{
 				Object[] transitionsIterator = autoOfBiggerRole.delta().toArray() ;
 	        	for( OWLObjectPropertyExpression smallerRole : inversedPropertyDependencyGraph.getSuccessors( roleToBuildAutomaton ) ){
 	        		boolean someInternalTransitionMatched = false;
 	        		for( int i=0 ; i<transitionsIterator.length ; i++ ) {
 	        			Transition trans = (Transition) transitionsIterator[i];
 	        			
 		        		if( trans.label() != null && trans.label().equals( smallerRole ) ){
 		        			Automaton autoOfSmallerRole = buildCompleteAutomataForRoles( smallerRole, inverseRolesMap, individualAutomata, completeAutomata, inversedPropertyDependencyGraph );
 		        			connectAutomata( autoOfBiggerRole, autoOfSmallerRole, trans );
 		        			someInternalTransitionMatched = true;
 		        		}
 	        		}
 		        	if( !someInternalTransitionMatched ){
 	        			Automaton autoOfSmallerRole = buildCompleteAutomataForRoles( smallerRole, inverseRolesMap, individualAutomata, completeAutomata, inversedPropertyDependencyGraph );
 	        			Transition initial2TerminalTransition = (Transition)autoOfBiggerRole.deltaFrom( (State)autoOfBiggerRole.initials().toArray()[0], (State)autoOfBiggerRole.terminals().toArray()[0]).toArray()[0];
 	        			connectAutomata( autoOfBiggerRole, autoOfSmallerRole, initial2TerminalTransition );
 		        	}
 	        	}
 			}
 			autoOfBiggerRole = minimizeAndNormalizeAutomaton( autoOfBiggerRole );
 	        completeAutomata.put( roleToBuildAutomaton , autoOfBiggerRole );
     		if( !individualAutomata.containsKey( roleToBuildAutomaton.getInverseProperty().getSimplified() ) ){
     			Automaton mirroredCopy = getMirroredCopy( autoOfBiggerRole );
     			mirroredCopy = minimizeAndNormalizeAutomaton( mirroredCopy );
     			completeAutomata.put( roleToBuildAutomaton.getInverseProperty().getSimplified() , mirroredCopy );
     		}
 	        return autoOfBiggerRole;
 		}
 	}
 	private Automaton minimizeAndNormalizeAutomaton(Automaton automaton) {
 		Reducer minimizerDeterminizer = new Reducer();
 		Normalizer normalizer = new Normalizer();
 		
 		automaton = minimizerDeterminizer.transform( automaton );
 		if( automaton.initials().size() != 1 )
 			automaton = normalizer.transform( automaton );
 		
 		return automaton;
 	}
 
 	private void connectAutomata(Automaton autoOfBiggerRole,Automaton autoOfSmallerRole, Transition tr) {
 		Map<State,State> stateMapper = getDisjointUnion( autoOfBiggerRole , autoOfSmallerRole);
 		try {
 			autoOfBiggerRole.addTransition( new Transition ( tr.start(), null, (State)stateMapper.get(autoOfSmallerRole.initials().toArray()[0])));
 			autoOfBiggerRole.addTransition( new Transition ( (State)stateMapper.get( (State)autoOfSmallerRole.terminals().toArray()[0] ), null, tr.end()));
 		} catch (NoSuchStateException e) {
 			throw new IllegalArgumentException( "Could not build the Complete Automata of non-Simple Roles" );
 		}
 	}
 
 	private Graph<OWLObjectPropertyExpression> buildPropertyOrdering(Collection<OWLObjectPropertyExpression[]> simpleObjectPropertyInclusions,Collection<ComplexObjectPropertyInclusion> complexObjectPropertyInclusions, Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> equivalentPropertiesMap){
     	Graph<OWLObjectPropertyExpression> propertyDependencyGraph = new Graph<OWLObjectPropertyExpression>();
 
     	for (OWLObjectPropertyExpression[] inclusion : simpleObjectPropertyInclusions){
     		if( !inclusion[0].equals( inclusion[1] ) && 
     			!inclusion[0].equals( inclusion[1].getInverseProperty().getSimplified() ) &&
     			(equivalentPropertiesMap.get( inclusion[0] ) == null ||
     			!equivalentPropertiesMap.get( inclusion[0] ).contains( inclusion[1] ) ) ){
     			propertyDependencyGraph.addEdge( inclusion[0], inclusion[1] );
     		}
     	}
     		
     	for (OWLAxioms.ComplexObjectPropertyInclusion inclusion : complexObjectPropertyInclusions){
     		OWLObjectPropertyExpression owlSuperProperty = inclusion.m_superObjectProperties;
     		OWLObjectPropertyExpression owlSubPropertyInChain = null;
     		OWLObjectPropertyExpression[] owlSubProperties = inclusion.m_subObjectProperties;
 
     		if( owlSubProperties.length != 2 && owlSuperProperty.equals( owlSubProperties[0] ) && owlSuperProperty.equals( owlSubProperties[owlSubProperties.length-1] ))
     			throw new IllegalArgumentException("The given role hierarchy is not regular.");
     		
     		for( int i=0 ; i<owlSubProperties.length ; i++ ){
     			owlSubPropertyInChain = owlSubProperties[i];
 
         		if( owlSubPropertyInChain.equals( owlSuperProperty ) && owlSubProperties.length != 2 && i>0 && i<owlSubProperties.length-1 )
         			throw new IllegalArgumentException("The given role hierarchy is not regular.");
         		else if( owlSubPropertyInChain.getInverseProperty().getSimplified().equals( owlSuperProperty ) )
         			throw new IllegalArgumentException("The given role hierarchy is not regular.");
         		else if ( !owlSubPropertyInChain.equals( owlSuperProperty ) )
         			propertyDependencyGraph.addEdge( owlSubPropertyInChain, owlSuperProperty );
     		}
     	}
      	return propertyDependencyGraph;
 	}
 	private void checkForRegularity(Graph<OWLObjectPropertyExpression> propertyDependencyGraph){
 	   	Graph<OWLObjectPropertyExpression> regularityCheckGraph = propertyDependencyGraph.clone();
     	regularityCheckGraph.transitivelyClose();
 
     	for( OWLObjectPropertyExpression prop : regularityCheckGraph.getElements() ){
     		Set<OWLObjectPropertyExpression> successors = regularityCheckGraph.getSuccessors( prop );
     		if( successors.contains( prop ) || successors.contains( prop.getInverseProperty().getSimplified() ) )
     			throw new IllegalArgumentException("The given role hierarchy is not regular.");
     	}
 	}
     private Map<OWLObjectPropertyExpression,Automaton> buildIndividualAutomata(Graph<OWLObjectPropertyExpression> complexRolesDependencyGraph, Collection<OWLObjectPropertyExpression[]> simpleObjectPropertyInclusions, Collection<ComplexObjectPropertyInclusion> complexObjectPropertyInclusions){
     	
     	Map<OWLObjectPropertyExpression,Automaton> automataMap = new HashMap<OWLObjectPropertyExpression,Automaton>();
     	Automaton auto = null;
     	State initialState = null;
     	State finalState = null;
 
      	for( OWLAxioms.ComplexObjectPropertyInclusion inclusion : complexObjectPropertyInclusions ){
     		OWLObjectPropertyExpression[] subObjectProperties = inclusion.m_subObjectProperties;
     		OWLObjectPropertyExpression superObjectProperty = inclusion.m_superObjectProperties;
     		if( !automataMap.containsKey( superObjectProperty )){
     			auto = new Automaton();
         		initialState = auto.addState( true , false );
         		finalState = auto.addState( false , true);
         		try {
 					auto.addTransition( new Transition(initialState, superObjectProperty , finalState) );
 				} catch (NoSuchStateException e) {
 					throw new IllegalArgumentException("Could not create automaton");
 				}
     		}
     		else{
     			auto = automataMap.get( superObjectProperty );
     			initialState = (State) auto.initials().toArray()[0];
     			finalState = (State) auto.terminals().toArray()[0];
     		}
     		//RR->R
     		if (subObjectProperties.length==2 && subObjectProperties[0].equals(superObjectProperty) && subObjectProperties[1].equals(superObjectProperty)) {
     			try {
					auto.addTransition( new Transition( finalState, null, initialState ) );
 				} catch (NoSuchStateException e) {
 					throw new IllegalArgumentException("Could not create automaton");
 				}
     		}
     		//R S2...Sn->R
     		else if( subObjectProperties[0].equals(superObjectProperty) ){
     			State fromState = finalState;
     			for( int i=1; i<subObjectProperties.length-1 ; i++ ){
     				try {
 						fromState = addNewTransition( auto, fromState, subObjectProperties[i] );
 					} catch (NoSuchStateException e) {
 						throw new IllegalArgumentException("Could not create automaton");
 					}
     			}
     			try {
 					auto.addTransition( new Transition( fromState, subObjectProperties[subObjectProperties.length-1], finalState) );
 				} catch (NoSuchStateException e) {
 					throw new IllegalArgumentException("Could not create automaton");
 				}
     		}
     		//S1...Sn-1 R->R
     		else if( subObjectProperties[subObjectProperties.length-1].equals(superObjectProperty) ){
     			State fromState = initialState;
     			for( int i=0; i<subObjectProperties.length-2 ; i++ ){
     				try {
 						fromState = addNewTransition( auto, fromState, subObjectProperties[i] );
 					} catch (NoSuchStateException e) {
 						throw new IllegalArgumentException("Could not create automaton");
 					}
     			}
     			try {
 					auto.addTransition( new Transition( fromState, subObjectProperties[subObjectProperties.length-2], initialState) );
 				} catch (NoSuchStateException e) {
 					throw new IllegalArgumentException("Could not create automaton");
 				}
     		}
     		//S1...Sn->R
     		else{
     			State fromState = initialState;
     			for( int i=0; i<subObjectProperties.length-1 ; i++ ){
     				try {
 						fromState = addNewTransition( auto, fromState, subObjectProperties[i] );
 					} catch (NoSuchStateException e) {
 						throw new IllegalArgumentException("Could not create automaton");
 					}
     			}
     			try {
 					auto.addTransition( new Transition( fromState, subObjectProperties[subObjectProperties.length-1], finalState) );
 				} catch (NoSuchStateException e) {
 					throw new IllegalArgumentException("Could not create automaton");
 				}
     		}
     		automataMap.put( superObjectProperty, auto );
 		}
    	for( OWLObjectPropertyExpression owlProp : automataMap.keySet() ){
    		for( OWLObjectPropertyExpression[] inclusion : simpleObjectPropertyInclusions ){
     			if( inclusion[0].equals( owlProp ) && inclusion[1].getInverseProperty().getSimplified().equals( owlProp ) ){
     				Automaton au = automataMap.get( owlProp );
     				buildAutomatonForSymmetricRole( au, getMirroredCopy( au ) );
     				automataMap.put( owlProp , au );
     			}
    		}
    	}
 
     	for(ComplexObjectPropertyInclusion inclusion : complexObjectPropertyInclusions){
     		OWLObjectPropertyExpression owlSuperProperty = inclusion.m_superObjectProperties;
     		OWLObjectPropertyExpression[] owlSubPropertyExpression = inclusion.m_subObjectProperties;
    		if( owlSubPropertyExpression.length==2 && owlSubPropertyExpression[0].equals(owlSuperProperty) && owlSubPropertyExpression[1].equals(owlSuperProperty)){
     			if( !complexRolesDependencyGraph.getElements().contains( owlSuperProperty ) ){
     				Automaton autoOfRole = automataMap.get( owlSuperProperty );
     				automataMap.put(owlSuperProperty, autoOfRole);
     				automataMap.put( owlSuperProperty.getInverseProperty().getSimplified(), getMirroredCopy( autoOfRole ) );
     			}
    		}
 		}
     	return automataMap;
     }
     private Map<State,State> getDisjointUnion( Automaton auto1, Automaton auto2 ){
     	Map<State,State> stateMapperUnionInverse = new HashMap<State,State>();
 		Object[] states = auto2.states().toArray();
 		for(int i=0 ; i<states.length ; i++ )
 			stateMapperUnionInverse.put((State) states[i] , auto1.addState(false , false));
 
 		Object[] transitions = auto2.delta().toArray();
 	    for(int i=0 ; i<transitions.length ; i++ ){
 	      Transition t = (Transition) transitions[i];
 	      try {
 	    	  auto1.addTransition(new Transition(	(State) stateMapperUnionInverse.get(t.start()) , 
 	    			  									t.label() , 
 	    			  								(State) stateMapperUnionInverse.get(t.end())));
 	      }catch(NoSuchStateException x){
 	    	  throw new IllegalArgumentException("Could not create disjoint union of automata");
 	      }
 	    }
 	    return stateMapperUnionInverse;
     }
     private void buildAutomatonForSymmetricRole(Automaton autoOfRole, Automaton autoOfInverseRole){
 
     	Map<State,State> stateMapperUnionInverse = getDisjointUnion(autoOfRole,autoOfInverseRole);
 		
 	    try {
 	    	autoOfRole.addTransition(
 					new Transition( (State)autoOfRole.initials().toArray()[0],
 									null, 
 									(State)stateMapperUnionInverse.get(autoOfInverseRole.terminals().toArray()[0])) );
 	    	autoOfRole.addTransition(
 					new Transition( (State)stateMapperUnionInverse.get(autoOfInverseRole.terminals().toArray()[0]),
 									null, 
 									(State)autoOfRole.initials().toArray()[0] ) );
 	    	autoOfRole.addTransition(
 					new Transition( (State)autoOfRole.terminals().toArray()[0],
 									null, 
 									(State)stateMapperUnionInverse.get(autoOfInverseRole.initials().toArray()[0])) );
 	    	autoOfRole.addTransition(
 					new Transition( (State)stateMapperUnionInverse.get(autoOfInverseRole.initials().toArray()[0]),
 									null, 
 									(State)autoOfRole.terminals().toArray()[0] ) );
 		} catch (NoSuchStateException e) {
 			throw new IllegalArgumentException("Could not create automaton for symmetric role");
 		}
     }
     private Automaton getMirroredCopy(Automaton auto){
     	Automaton mirroredCopy = new Automaton() ;
         Map<State,State> map = new HashMap<State,State>() ;
         Object[] objs = auto.states().toArray();
         for( int i =0; i<objs.length ; i++) {
           State e = (State) objs[i] ;
           map.put(e , mirroredCopy.addState(e.isTerminal() , e.isInitial())) ;
         }
         objs = auto.delta().toArray();
         for(int i=0 ; i<objs.length ; i++ ) {
           Transition t = (Transition) objs[i];
           try {
         	  if( t.label() instanceof OWLObjectPropertyExpression)
         		  mirroredCopy.addTransition(new Transition((State) map.get(t.end()),((OWLObjectPropertyExpression) t.label()).getInverseProperty().getSimplified() ,(State) map.get(t.start())));
         	  else
         		  mirroredCopy.addTransition(new Transition((State) map.get(t.end()) , t.label() ,(State) map.get(t.start())));
           } catch(NoSuchStateException x) {}
         }
         return mirroredCopy;
     }
 	private State addNewTransition(Automaton auto, State fromState,OWLObjectPropertyExpression objectPropertyExpression) throws NoSuchStateException {
 		OWLObjectPropertyExpression propertyOfChain = objectPropertyExpression;
 		State toState = auto.addState( false , false );
 		auto.addTransition( new Transition( fromState, propertyOfChain, toState) );
 		return toState;
 	}
 	public Set<OWLObjectPropertyExpression> getM_nonSimpleRoles() {
 		return m_nonSimpleRoles;
 	}
 }
