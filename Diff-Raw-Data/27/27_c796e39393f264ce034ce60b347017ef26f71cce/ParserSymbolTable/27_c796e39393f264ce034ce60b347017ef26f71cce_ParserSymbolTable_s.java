 /**********************************************************************
  * Copyright (c) 2002,2003,2004 Rational Software Corporation and others. 
  * All rights reserved.   This program and the accompanying materials 
  * are made available under the terms of the Common Public License v0.5 
  * which accompanies this distribution, and is available at 
  * http://www.eclipse.org/legal/cpl-v05. html
  *
  * Contributors: 
  * Rational Software - Initial API and implementation
  *
 ***********************************************************************/
 
 
 package org.eclipse.cdt.internal.core.parser.pst;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.ListIterator;
 import java.util.Map;
 import java.util.Set;
 import java.util.SortedMap;
 
 import org.eclipse.cdt.core.parser.ParserLanguage;
 import org.eclipse.cdt.core.parser.ParserMode;
 import org.eclipse.cdt.core.parser.ast.ASTAccessVisibility;
 import org.eclipse.cdt.core.parser.ast.IASTMember;
 import org.eclipse.cdt.core.parser.ast.IASTNode;
 import org.eclipse.cdt.internal.core.parser.pst.IDerivableContainerSymbol.IParentSymbol;
 import org.eclipse.cdt.internal.core.parser.pst.TypeInfo.PtrOp;
 
 /**
  * @author aniefer
  */
 
 public class ParserSymbolTable {
 
 	public static final int    TYPE_LOOP_THRESHOLD = 50;
 	public static final String EMPTY_NAME = ""; //$NON-NLS-1$
 	public static final String THIS = "this";	//$NON-NLS-1$
 	
 	/**
 	 * Constructor for ParserSymbolTable.
 	 */
 	public ParserSymbolTable( ParserLanguage language, ParserMode mode ) {
 		super();
 		_compilationUnit = newContainerSymbol( EMPTY_NAME, TypeInfo.t_namespace );
 		_language = language;
 		_mode = mode;
 	}
 
 	public IContainerSymbol getCompilationUnit(){
 		return _compilationUnit;
 	}
 	
 	public IContainerSymbol newContainerSymbol( String name ){
 		if( name == null ) name = EMPTY_NAME;
 		return new ContainerSymbol( this, name );
 	}
 	public IContainerSymbol newContainerSymbol( String name, TypeInfo.eType type ){
 		if( name == null ) name = EMPTY_NAME;
 		return new ContainerSymbol( this, name, type );
 	}
 	
 	public ISymbol newSymbol( String name ){
 		if( name == null ) name = EMPTY_NAME;
 		return new BasicSymbol( this, name );
 	}
 	public ISymbol newSymbol( String name, TypeInfo.eType type ){
 		if( name == null ) name = EMPTY_NAME;
 		return new BasicSymbol( this, name, type );
 	}
 	
 	public IDerivableContainerSymbol newDerivableContainerSymbol( String name ){
 		if( name == null ) name = EMPTY_NAME;
 		return new DerivableContainerSymbol( this, name );
 	}
 	public IDerivableContainerSymbol newDerivableContainerSymbol( String name, TypeInfo.eType type ){
 		if( name == null ) name = EMPTY_NAME;
 		return new DerivableContainerSymbol( this, name, type );
 	}
 	public IParameterizedSymbol newParameterizedSymbol( String name ){
 		if( name == null ) name = EMPTY_NAME;
 		return new ParameterizedSymbol( this, name );
 	}
 	public IParameterizedSymbol newParameterizedSymbol( String name, TypeInfo.eType type ){
 		if( name == null ) name = EMPTY_NAME;
 		return new ParameterizedSymbol( this, name, type );
 	}
 	public ITemplateSymbol newTemplateSymbol( String name ){
 		if( name == null ) name = EMPTY_NAME;
 		return new TemplateSymbol( this, name );
 	}
 	
 	public ISpecializedSymbol newSpecializedSymbol( String name ){
 		if( name == null ) name = EMPTY_NAME;
 		return new SpecializedSymbol( this, name );
 	}
 	
 	public ITemplateFactory newTemplateFactory(){
 		return new TemplateFactory( this );
 	}
 	
 	/**
 	 * Lookup the name from LookupData starting in the inDeclaration
 	 * @param data
 	 * @param inDeclaration
 	 * @return Declaration
 	 * @throws ParserSymbolTableException
 	 */
 	static protected void lookup( LookupData data, IContainerSymbol inSymbol ) throws ParserSymbolTableException
 	{
 		//handle namespace aliases
 		if( inSymbol.isType( TypeInfo.t_namespace ) ){
 			ISymbol symbol = inSymbol.getTypeSymbol();
 			if( symbol != null && symbol.isType( TypeInfo.t_namespace ) ){
 				inSymbol = (IContainerSymbol) symbol;
 			}
 		}
 		
 		ArrayList transitives = null;	//list of transitive using directives
 		
 		//if this name define in this scope?
 		Map map = null;
 		if( !data.usingDirectivesOnly ){
 			map = lookupInContained( data, inSymbol );
 			if( data.foundItems == null || data.foundItems.isEmpty() ){
 				data.foundItems = map;
 			} else {
 				mergeResults( data, data.foundItems, map );
 			}	
 		}
 		
 		if( inSymbol.getSymbolTable().getLanguage() == ParserLanguage.CPP &&
 		    !data.ignoreUsingDirectives )
 		{
 			//check nominated namespaces
 			//the transitives list is populated in LookupInNominated, and then 
 			//processed in ProcessDirectives
 			
 			data.visited.clear(); //each namesapce is searched at most once, so keep track
 			
 			transitives = lookupInNominated( data, inSymbol, transitives );
 
 			//if we are doing a qualified lookup, only process using directives if
 			//we haven't found the name yet (and if we aren't ignoring them). 
 			if( !data.qualified || data.foundItems == null || data.foundItems.isEmpty() ){
 				processDirectives( inSymbol, data, transitives );
 				
 				if( inSymbol.hasUsingDirectives() ){
 					processDirectives( inSymbol, data, inSymbol.getUsingDirectives() );
 				}
 							
 				while( data.usingDirectives != null && data.usingDirectives.get( inSymbol ) != null ){
 					if( transitives != null )
 						transitives.clear();
 					
 					transitives = lookupInNominated( data, inSymbol, transitives );
 	
 					if( !data.qualified || data.foundItems == null ){
 						processDirectives( inSymbol, data, transitives );
 					}
 				}
 			}
 		}
 		
 		if( !data.isPrefixLookup() && ( ( data.foundItems != null && !data.foundItems.isEmpty()) || data.getStopAt() == inSymbol ) ){
 			return;
 		}
 			
 		if( !data.usingDirectivesOnly && inSymbol instanceof IDerivableContainerSymbol ){
 			//if we still havn't found it, check any parents we have
 			data.visited.clear();	//each virtual base class is searched at most once
 			map = lookupInParents( data, inSymbol );
 			
 			if( data.foundItems == null || data.foundItems.isEmpty() ){
 				data.foundItems = map;
 			} else {
 				mergeInheritedResults( data.foundItems, map );
 			}
 		}
 					
 		//if still not found, check our containing scope.			
 		if( ( data.foundItems == null || data.foundItems.isEmpty() || data.isPrefixLookup() )
 			&& inSymbol.getContainingSymbol() != null )
 		{ 
 			if( data.qualified ){
 				if( data.usingDirectives != null && !data.usingDirectives.isEmpty() ){
 					data.usingDirectivesOnly = true;
 					lookup( data, inSymbol.getContainingSymbol() );
 					
 				}
 			} else {
 				lookup( data, inSymbol.getContainingSymbol() );	
 			}
 			
 		}
 
 		return;
 	}
 	
 	/**
 	 * function LookupInNominated
 	 * @param data
 	 * @param transitiveDirectives
 	 * @return List
 	 * 
 	 * for qualified:
 	 *  3.4.3.2-2 "let S be the set of all declarations of m in X
 	 * and in the transitive closure of all namespaces nominated by using-
 	 * directives in X and its used namespaces, except that using-directives are
 	 * ignored in any namespace, including X, directly containing one or more
 	 * declarations of m."
 	 * 
 	 * for unqualified:
 	 * 7.3.4-2 The using-directive is transitive: if a scope contains a using
 	 * directive that nominates a second namespace that itself contains using-
 	 * directives, the effect is as if the using-directives from the second
 	 * namespace also appeared in the first.
 	 */
 	static private ArrayList lookupInNominated( LookupData data, IContainerSymbol symbol, ArrayList transitiveDirectives ) throws ParserSymbolTableException{
 		//if the data.usingDirectives is empty, there is nothing to do.
 		if( data.usingDirectives == null ){
 			return transitiveDirectives;
 		}
 			
 		//local variables
 		ArrayList  directives = null; //using directives association with declaration
 		IContainerSymbol temp = null;
 		
 		boolean foundSomething = false;
 		int size = 0;
 		
 		directives = (ArrayList) data.usingDirectives.remove( symbol );
 		
 		if( directives == null ){
 			return transitiveDirectives;
 		}
 		
 		size = directives.size();
 		for( int i = 0; i < size; i++ ){
 			temp = (IContainerSymbol) directives.get(i);
 
 			//namespaces are searched at most once
 			if( !data.visited.contains( temp ) ){
 				data.visited.add( temp );
 				
 				Map map = lookupInContained( data, temp );
 				foundSomething = ( map != null && !map.isEmpty() );
 				if( foundSomething ){
 					if( data.foundItems == null )
 						data.foundItems = map;
 					else
 						mergeResults( data, data.foundItems, map );
 				}
 				
 				//only consider the transitive using directives if we are an unqualified
 				//lookup, or we didn't find the name in decl
 				if( (!data.qualified || !foundSomething || data.isPrefixLookup() ) && temp.hasUsingDirectives() ){
 					//name wasn't found, add transitive using directives for later consideration
 					if( transitiveDirectives == null )
 						transitiveDirectives = new ArrayList(4);
 					transitiveDirectives.addAll( temp.getUsingDirectives() );
 				}
 			}
 		}
 		
 		return transitiveDirectives;
 	}
 	
 	/**
 	 * @param map
 	 * @param map2
 	 */
 	private static void mergeResults( LookupData data, Map resultMap, Map map ) throws ParserSymbolTableException {
 		if( resultMap == null || map == null || map.isEmpty() ){
 			return;
 		}
 		
 		Iterator keyIterator = map.keySet().iterator();
 		Object key = null;
 		while( keyIterator.hasNext() ){
 			key = keyIterator.next();
 			if( resultMap.containsKey( key ) ){
 				List list = new ArrayList();
 				Object obj = resultMap.get( key );
 
 				if ( obj instanceof List ) list.addAll( (List) obj  );
 				else  					   list.add( obj );
 				
 				obj = map.get( key );
 				
 				if( obj instanceof List ) list.addAll( (List) obj );
 				else 					  list.add( obj );
 				
 				resultMap.put( key, collectSymbol( data, list ) );
 			} else {
 				resultMap.put( key, map.get( key ) );
 			}
 		}
 	}
 
 	/**
 	 * function LookupInContained
 	 * @param data
 	 * @return List
 	 * 
 	 * Look for data.name in our collection _containedDeclarations
 	 */
 	protected static Map lookupInContained( LookupData data, IContainerSymbol lookIn ) throws ParserSymbolTableException{
 		Map found = null;
 		
 		Object obj = null;
 	
 		if( data.getAssociated() != null ){
 			//we are looking in lookIn, remove it from the associated scopes list
 			data.getAssociated().remove( lookIn );
 		}
 		
 		Map declarations = lookIn.getContainedSymbols();
 		
 		Iterator iterator = null;
 		if( data.isPrefixLookup() && declarations != Collections.EMPTY_MAP ){
 			if( declarations instanceof SortedMap ){
 				iterator = ((SortedMap)declarations).tailMap( data.name.toLowerCase() ).keySet().iterator();
 			} else {
 				throw new ParserSymbolTableError( ParserSymbolTableError.r_InternalError );
 			}
 		}
 		
 		String name = ( iterator != null && iterator.hasNext() ) ? (String) iterator.next() : data.name;
 		
 		while( name != null ) {
 			if( nameMatches( data, name ) ){
 				obj = ( !declarations.isEmpty() ) ? declarations.get( name ) : null;
 				if( obj != null ){
 					obj = collectSymbol( data, obj );
 					
 					if( obj != null ){
 						if( found == null )
 							found = new LinkedHashMap();
 						found.put( name, obj );
 					}
 				}
 			} else {
 				break;
 			}
 						
 			if( iterator != null && iterator.hasNext() ){
 				name = (String) iterator.next();
 			} else {
 				name = null;
 			}
 		} 
 		
 		if( found != null && !data.isPrefixLookup() ){
 			return found;
 		}
 		
 		if( lookIn instanceof IParameterizedSymbol ){
 			found = lookupInParameters(data, lookIn, found);
 		}
 		
 		if( lookIn.isTemplateMember() && data.templateMember == null ){
 			IContainerSymbol containing = lookIn.getContainingSymbol();
 			IContainerSymbol outer = (containing != null ) ? containing.getContainingSymbol() : null;
 		 	if( ( containing instanceof IDerivableContainerSymbol && outer instanceof ITemplateSymbol) ||
 		 		( lookIn instanceof IParameterizedSymbol && containing instanceof ITemplateSymbol ) ) 
 		 	{
 		 		data.templateMember = lookIn;
 		 	}
    		}
 		
 		return found;
 
 	}
 	/**
 	 * @param data
 	 * @param lookIn
 	 * @param found
 	 * @throws ParserSymbolTableException
 	 */
 	private static Map lookupInParameters(LookupData data, IContainerSymbol lookIn, Map found) throws ParserSymbolTableException {
 		Object obj;
 		Iterator iterator;
 		String name;
 		
 		if( lookIn instanceof ITemplateSymbol && !((ITemplateSymbol)lookIn).getDefinitionParameterMap().isEmpty() ){
 			ITemplateSymbol template = (ITemplateSymbol) lookIn;
 			if( data.templateMember != null && template.getDefinitionParameterMap().containsKey( data.templateMember ) ){
 				Map map = (Map) template.getDefinitionParameterMap().get( data.templateMember );
 				iterator = map.keySet().iterator();
 				while( iterator.hasNext() ){
 					ISymbol symbol = (ISymbol) iterator.next();
 					if( nameMatches( data, symbol.getName() ) ){
 						obj = collectSymbol( data, symbol );
 						if( obj != null ){
 							if( found == null )
 								found = new LinkedHashMap();
 							found.put( symbol.getName(), obj );
 						}
 					}
 				}
 				return found;
 			}
 			
 		}
 		Map parameters = ((IParameterizedSymbol)lookIn).getParameterMap();
 		if( parameters != Collections.EMPTY_MAP ){
 			iterator = null;
 			if( data.isPrefixLookup() ){
 				if( parameters instanceof SortedMap ){
 					iterator = ((SortedMap) parameters).tailMap( data.name.toLowerCase() ).keySet().iterator();
 				} else {
 					throw new ParserSymbolTableError( ParserSymbolTableError.r_InternalError );
 				}
 			}
 			
 			name = ( iterator != null && iterator.hasNext() ) ? (String) iterator.next() : data.name;
 			while( name != null ){
 				if( nameMatches( data, name ) ){
 					obj = parameters.get( name );
 					obj = collectSymbol( data, obj );
 					if( obj != null ){
 						if( found == null )
 							found = new LinkedHashMap();
 						found.put( name, obj );
 					}
 				} else {
 					break;
 				}
 				
 				if( iterator != null && iterator.hasNext() ){
 					name = (String) iterator.next();
 				} else {
 					name = null;
 				}
 			}
 		}
 		return found;
 	}
 
 	private static boolean nameMatches( LookupData data, String name ){
 		if( data.isPrefixLookup() ){
 			return name.regionMatches( true, 0, data.name, 0, data.name.length() );
 		} 
 		return name.equals( data.name );
 	}
 	private static boolean checkType( LookupData data, ISymbol symbol ) { //, TypeInfo.eType type, TypeInfo.eType upperType ){
 		if( data.getFilter() == null ){
 			return true;
 		}
 		
 		TypeInfo typeInfo = ParserSymbolTable.getFlatTypeInfo( symbol.getTypeInfo(), true );
 		boolean accept = data.getFilter().shouldAccept( symbol, typeInfo ) || data.getFilter().shouldAccept( symbol );
 		typeInfo.release();
 		
 		return accept;
 	}
 	
 	private static Object collectSymbol(LookupData data, Object object ) throws ParserSymbolTableException {
 		if( object == null ){
 			return null;
 		}
 		
 		ISymbol foundSymbol = null;
 		
 		List objList = ( object instanceof List ) ? (List)object : null;
 		int objListSize = ( objList != null ) ? objList.size() : 0;
 		ISymbol symbol = ( objList != null ) ? (ISymbol) objList.get( 0 ) : (ISymbol) object;
 	
 		Set functionSet = new HashSet();
 		Set templateFunctionSet = new HashSet();
 		
 		ISymbol obj	= null;
 		IContainerSymbol cls = null;
 		int idx = 1;
 		while( symbol != null ){
 			if( symbol instanceof ITemplateSymbol ){
 				ISymbol temp = ((ITemplateSymbol)symbol).getTemplatedSymbol();
 				symbol = ( temp != null ) ? temp : symbol;
 			}
 			
 			if( ( data.returnInvisibleSymbols || !symbol.getIsInvisible() ) && checkType( data, symbol ) ){
 				foundSymbol = symbol;
 				
 				if( foundSymbol.isType( TypeInfo.t_function ) ){
 					if( foundSymbol.isForwardDeclaration() && foundSymbol.getTypeSymbol() != null &&
 						foundSymbol.getTypeSymbol().getContainingSymbol() == foundSymbol.getContainingSymbol() )
 					{
 						foundSymbol = foundSymbol.getTypeSymbol();
 					}
 					if( foundSymbol.getContainingSymbol().isType( TypeInfo.t_template ) ){
 						templateFunctionSet.add( foundSymbol );
 					} else {
 						functionSet.add( foundSymbol );	
 					}
 					
 				} else {
 					//if this is a class-name, other stuff hides it
 					if( foundSymbol.isType( TypeInfo.t_class, TypeInfo.t_enumeration ) ){
 						if( cls == null ){
 							cls = (IContainerSymbol) foundSymbol;
 						} else {
 							if( cls.getTypeInfo().isForwardDeclaration() && cls.getTypeSymbol() == foundSymbol ){
 								//cls is a forward declaration of decl, we want decl.
 								cls = (IContainerSymbol) foundSymbol;
 							} else if( foundSymbol.getTypeInfo().isForwardDeclaration() && foundSymbol.getTypeSymbol() == cls ){
 								//decl is a forward declaration of cls, we already have what we want (cls)
 							} else {
 								if( data.isPrefixLookup() ){
 									data.addAmbiguity( foundSymbol.getName() );
 								} else {
 									throw new ParserSymbolTableException( ParserSymbolTableException.r_Ambiguous );
 								}
 							}
 						}
 					} else {
 						//an object, can only have one of these
 						if( obj == null ){
 							obj = foundSymbol;	
 						} else {
 							if( data.isPrefixLookup() ){
 								data.addAmbiguity( foundSymbol.getName() );
 							} else {
 								throw new ParserSymbolTableException( ParserSymbolTableException.r_Ambiguous );
 							} 
 						}
 					}
 				}
 			}
 			
 			if( objList != null && idx < objListSize ){
 				symbol = (ISymbol) objList.get( idx++ );
 			} else {
 				symbol = null;
 			}
 		}
 	
 		int numFunctions = functionSet.size();
 		int numTemplateFunctions = templateFunctionSet.size();
 		
 		boolean ambiguous = false;
 		
 		if( cls != null ){
 			//the class is only hidden by other stuff if they are from the same scope
 			if( obj != null && cls.getContainingSymbol() != obj.getContainingSymbol()){
 				ambiguous = true;	
 			}
 			
 			Iterator fnIter = null;
 			IParameterizedSymbol fn = null;
 			if( !templateFunctionSet.isEmpty() ){
 				fnIter = templateFunctionSet.iterator();
 			
 				for( int i = numTemplateFunctions; i > 0; i-- ){
 					fn = (IParameterizedSymbol) fnIter.next();
 					if( cls.getContainingSymbol()!= fn.getContainingSymbol()){
 						ambiguous = true;
 						break;
 					}
 				}
 			}
 			if( !functionSet.isEmpty() ){
 				fnIter = functionSet.iterator();
 				
 				for( int i = numFunctions; i > 0; i-- ){
 					fn = (IParameterizedSymbol) fnIter.next();
 					if( cls.getContainingSymbol()!= fn.getContainingSymbol()){
 						ambiguous = true;
 						break;
 					}
 				}
 			}
 		}
 		
 		if( numTemplateFunctions > 0 ){
 			if( data.getParameters() != null && ( !data.exactFunctionsOnly || data.getTemplateParameters() != null ) ){
 				List fns  = TemplateEngine.selectTemplateFunctions( templateFunctionSet, data.getParameters(), data.getTemplateParameters() );
 				if( fns != null )
 					functionSet.addAll( fns );
 				numFunctions = functionSet.size();
 			} else {
 				functionSet.addAll( templateFunctionSet );
 				numFunctions += numTemplateFunctions;
 			}
 		}
 		
 		if( obj != null && !ambiguous ){
 			if( numFunctions > 0 ){
 				ambiguous = true;
 			} else {
 				return obj;
 			}
 		} else if( numFunctions > 0 ) {
 			return new ArrayList( functionSet );
 		}
 		
 		if( ambiguous ){
 			if( data.isPrefixLookup() ){
 				data.addAmbiguity( foundSymbol.getName() );
 			} else {
 				throw new ParserSymbolTableException( ParserSymbolTableException.r_Ambiguous );
 			} 
 		} 
 		
 		return cls;
 	}
 	/**
 	 * 
 	 * @param data
 	 * @param lookIn
 	 * @return Declaration
 	 * @throws ParserSymbolTableException
 	 */
 	private static Map lookupInParents( LookupData data, ISymbol lookIn ) throws ParserSymbolTableException{
 		IDerivableContainerSymbol container = null;
 
 		if( lookIn instanceof IDerivableContainerSymbol ){
 			container = (IDerivableContainerSymbol) lookIn;
 		} else{
 			throw new ParserSymbolTableError( ParserSymbolTableError.r_InternalError );
 		}
 		
 		List scopes = container.getParents();
 
 		Map temp = null;
 		Map symbol = null;
 		Map inherited = null;
 		
 		IDerivableContainerSymbol.IParentSymbol wrapper = null;
 		
 		if( scopes == null )
 			return null;
 				
 		//use data to detect circular inheritance
 		if( data.inheritanceChain == null )
 			data.inheritanceChain = new HashSet();
 		
 		data.inheritanceChain.add( container );
 			
 		int size = scopes.size();
 		for( int i = 0; i < size; i++ )
 		{
 			wrapper = (IDerivableContainerSymbol.IParentSymbol) scopes.get(i);
 			ISymbol parent = wrapper.getParent();
 			if( parent == null )
 				continue;
 
 			if( !wrapper.isVirtual() || !data.visited.contains( parent ) ){
 				if( wrapper.isVirtual() ){
 					data.visited.add( parent );
 				}
 				
 				//if the inheritanceChain already contains the parent, then that 
 				//is circular inheritance
 				if( ! data.inheritanceChain.contains( parent ) ){
 					//is this name define in this scope?
 					if( parent instanceof IDeferredTemplateInstance ){
 						parent = ((IDeferredTemplateInstance)parent).getTemplate().getTemplatedSymbol();
 					} else if( parent instanceof ITemplateSymbol ){
 						parent = ((ITemplateSymbol)parent).getTemplatedSymbol();
 					}
 					if( parent instanceof IDerivableContainerSymbol ){
 						temp = lookupInContained( data, (IDerivableContainerSymbol) parent );
 					} else {
 						throw new ParserSymbolTableException( ParserSymbolTableException.r_BadTypeInfo );
 					}
 					
 					if( (temp == null || temp.isEmpty()) || data.isPrefixLookup() ){
 						inherited = lookupInParents( data, parent );
 						if( temp == null )
 							temp = inherited;
 						else
 							mergeInheritedResults( temp, inherited );
 					}
 				} else {
 					throw new ParserSymbolTableException( ParserSymbolTableException.r_CircularInheritance );
 				}
 			}	
 			
 			if( temp != null && !temp.isEmpty() ){
 				if( symbol == null || symbol.isEmpty() ){
 					symbol = temp;
 				} else if ( temp != null && !temp.isEmpty() ) {
 					Iterator iter = temp.keySet().iterator();
 					Object key = null;
 					while( iter.hasNext() ){
 						key = iter.next();
 						if( symbol.containsKey( key ) ){
 							Object obj = symbol.get( key );
 							List objList = ( obj instanceof List ) ? (List)obj : null;
 							int objListSize = ( objList != null ) ? objList.size() : 0, idx = 1;
 							ISymbol sym = (ISymbol) (( objList != null && objListSize > 0 ) ? objList.get(0) : obj);
 							while( sym != null ){
 								if( !checkAmbiguity( sym, temp.get( key ) ) ){
 									if( data.isPrefixLookup() ){
 										data.addAmbiguity( sym.getName() );
 									} else {
 										throw new ParserSymbolTableException( ParserSymbolTableException.r_Ambiguous );
 									} 								
 								}
 								
 								if( objList != null && idx < objListSize ){
 									sym = (ISymbol) objList.get( idx++ );
 								} else {
 									sym = null;
 								}
 							}
 						} else {
 							symbol.put( key, temp.get( key ) );
 						}
 					}
 				}
 			} else {
 				temp = null;	//reset temp for next iteration
 			}
 		}
 	
 		data.inheritanceChain.remove( container );
 
 		return symbol;	
 	}
 	
 	private static boolean checkAmbiguity( Object obj1, Object obj2 ){
 		//it is not ambiguous if they are the same thing and it is static or an enumerator
 		if( obj1 == obj2 ){
 			List objList = ( obj1 instanceof List ) ? (List) obj1 : null;
 			int objListSize = ( objList != null ) ? objList.size() : 0;
 			ISymbol symbol = ( objList != null ) ? (ISymbol) objList.get(0) : ( ISymbol )obj1;
 			int idx = 1;
 			while( symbol != null ) {
 				TypeInfo type = ((ISymbol)obj1).getTypeInfo();
 				if( !type.checkBit( TypeInfo.isStatic ) && !type.isType( TypeInfo.t_enumerator ) ){
 					return false;
 				}
 				
 				if( objList != null && idx < objListSize ){
 					symbol = (ISymbol) objList.get( idx++ );
 				} else {
 					symbol = null;
 				}
 			}
 			return true;
 		} 
 		return false;
 	}
 	
 	/**
 	 * Symbols in map are added to the resultMap if a symbol with that name does not already exist there
 	 * @param resultMap
 	 * @param map
 	 * @throws ParserSymbolTableException
 	 */
 	private static void mergeInheritedResults( Map resultMap, Map map ){
 		if( resultMap == null || map == null || map.isEmpty() ){
 			return;
 		}
 		
 		Iterator keyIterator = map.keySet().iterator();
 		Object key = null;
 		while( keyIterator.hasNext() ){
 			key = keyIterator.next();
 			if( !resultMap.containsKey( key ) ){
 				resultMap.put( key, map.get( key ) );
 			}
 		}
 	}
 	
 	/**
 	 * function isValidOverload
 	 * @param origDecl
 	 * @param newDecl
 	 * @return boolean
 	 * 
 	 * 3.3.7 "A class name or enumeration name can be hidden by the name of an
 	 * object, function or enumerator declared in the same scope"
 	 * 
 	 * 3.4-1 "Name lookup may associate more than one declaration with a name if
 	 * it finds the name to be a function name"
 	 */
 	protected static boolean isValidOverload( ISymbol origSymbol, ISymbol newSymbol ){
 		TypeInfo.eType origType = origSymbol.getType();
 		TypeInfo.eType newType  = newSymbol.getType();
 		
 		if( origType == TypeInfo.t_template ){
 			ITemplateSymbol template = (ITemplateSymbol) origSymbol;
 			origSymbol = template.getTemplatedSymbol();
 			if( origSymbol == null )
 				return true;
 			origType = origSymbol.getType();
 		}
 		
 		if( newType == TypeInfo.t_template ){
 			ITemplateSymbol template = (ITemplateSymbol) newSymbol;
 			newSymbol = template.getTemplatedSymbol();
 			if( newSymbol == null )
 				return true;
 			newType = newSymbol.getType();
 		}	
 		
 		//handle forward decls
 		if( origSymbol.getTypeInfo().isForwardDeclaration() ){
 			if( origSymbol.getTypeSymbol() == newSymbol )
 				return true;
 			
 			//friend class declarations
 			if( origSymbol.getIsInvisible() && origSymbol.isType( newSymbol.getType() ) ){
 				origSymbol.getTypeInfo().setTypeSymbol(  newSymbol );
 				return true;
 			}
 		}
 				
 		if( (origType.compareTo(TypeInfo.t_class) >= 0 && origType.compareTo(TypeInfo.t_enumeration) <= 0) && //class name or enumeration ...
 			( newType == TypeInfo.t_type || (newType.compareTo( TypeInfo.t_function ) >= 0 /*&& newType <= TypeInfo.typeMask*/) ) ){
 				
 			return true;
 		}
 		//if the origtype is not a class-name or enumeration name, then the only other
 		//allowable thing is if they are both functions.
 		if( origSymbol instanceof IParameterizedSymbol && newSymbol instanceof IParameterizedSymbol )
 			return isValidFunctionOverload( (IParameterizedSymbol) origSymbol, (IParameterizedSymbol) newSymbol );
 		return false;
 	}
 	
 	protected static boolean isValidOverload( List origList, ISymbol newSymbol ){
 		if( origList.size() == 1 ){
 			return isValidOverload( (ISymbol)origList.get(0), newSymbol );
 		} else if ( origList.size() > 1 ){
 			if( newSymbol.isType( TypeInfo.t_template ) ){
 				ITemplateSymbol template = (ITemplateSymbol) newSymbol;
 				newSymbol = (ISymbol) template.getContainedSymbols().get( template.getName() );	
 			}
 			
 			//the first thing can be a class-name or enumeration name, but the rest
 			//must be functions.  So make sure the newDecl is a function before even
 			//considering the list
 			if( newSymbol.getType() != TypeInfo.t_function && newSymbol.getType() != TypeInfo.t_constructor ){
 				return false;
 			}
 			
 			//Iterator iter = origList.iterator();
 			ISymbol symbol = (ISymbol) origList.get(0);
 			int numSymbols = origList.size();
 			if( symbol.isType( TypeInfo.t_template ) ){
 				IParameterizedSymbol template = (IParameterizedSymbol) symbol;
 				symbol = (ISymbol) template.getContainedSymbols().get( template.getName() );	
 			}
 			
 			boolean valid = isValidOverload( symbol, newSymbol );
 			int idx = 1;
 			while( valid && idx < numSymbols ){
 				symbol = (ISymbol) origList.get(idx++);
 				if( symbol.isType( TypeInfo.t_template ) ){
 					ITemplateSymbol template = (ITemplateSymbol) symbol;
 					symbol = template.getTemplatedSymbol();	
 				}
 				valid = ( symbol instanceof IParameterizedSymbol) && isValidFunctionOverload( (IParameterizedSymbol)symbol, (IParameterizedSymbol)newSymbol );
 			}
 			
 			return valid;
 		}
 		
 		//empty list, return true
 		return true;
 	}
 	
 	private static boolean isValidFunctionOverload( IParameterizedSymbol origSymbol, IParameterizedSymbol newSymbol ){
 		if( ( !origSymbol.isType( TypeInfo.t_function ) && !origSymbol.isType( TypeInfo.t_constructor ) ) || 
 			( ! newSymbol.isType( TypeInfo.t_function ) && ! newSymbol.isType( TypeInfo.t_constructor ) ) ){
 			return false;
 		}
 		
 		//handle forward decls
 		if( origSymbol.getTypeInfo().isForwardDeclaration() &&
 			origSymbol.getTypeSymbol() == newSymbol )
 		{
 			return true;
 		}
 		if( origSymbol.hasSameParameters( newSymbol ) ){
 			//functions with the same name and same parameter types cannot be overloaded if any of them
 			//is static
 			if( origSymbol.getTypeInfo().checkBit( TypeInfo.isStatic ) || newSymbol.getTypeInfo().checkBit( TypeInfo.isStatic ) ){
 				return false;
 			}
 			
 			//if none of them are static, then the function can be overloaded if they differ in the type
 			//of their implicit object parameter.
 			if( origSymbol.compareCVQualifiersTo( newSymbol ) != 0 ){
 				return true;
 			}
 			
 			return false;
 		}
 		
 		return true;
 	}
 	
 	/**
 	 * 
 	 * @param data
 	 * @return Declaration
 	 * @throws ParserSymbolTableException
 	 * 
 	 * Resolve the foundItems set down to one declaration and return that
 	 * declaration.  
 	 * If we successfully resolve, then the data.foundItems list will be
 	 * cleared.  If however, we were not able to completely resolve the set,
 	 * then the data.foundItems set will be left with those items that
 	 * survived the partial resolution and we will return null.  (currently,
 	 * this case applies to when we have overloaded functions and no parameter
 	 * information)
 	 * 
 	 * NOTE: data.parameters == null means there is no parameter information at
 	 * all, when looking for functions with no parameters, an empty list must be
 	 * provided in data.parameters.
 	 */
 	static protected ISymbol resolveAmbiguities( LookupData data ) throws ParserSymbolTableException{
 		ISymbol resolvedSymbol = null;
 		
 		if( data.foundItems == null || data.foundItems.isEmpty() || data.isPrefixLookup() ){
 			return null;
 		}
 		
 		Object object = data.foundItems.get( data.name );
 
 		ArrayList functionList = null;
 		
 		if( object instanceof List ){
 			//if we got this far with a list, they must all be functions
 			functionList = new ArrayList( ((List)object).size() );
 			functionList.addAll( (List) object );
 		} else {
 			ISymbol symbol = (ISymbol) object;
 			if( symbol.isType( TypeInfo.t_function ) ){
 				functionList = new ArrayList(1);
 				functionList.add( symbol );
 			} else {
 				if( symbol.isTemplateMember() && !symbol.isTemplateInstance() && 
 					!symbol.isType( TypeInfo.t_templateParameter ) && symbol.getContainingSymbol().isType( TypeInfo.t_template ))
 				{
 					resolvedSymbol = symbol.getContainingSymbol();
 				} else {
 					resolvedSymbol = symbol;
 				}
 			}
 		}
 		
 		if( resolvedSymbol == null ){
 			if( data.getParameters() == null ){
 				//we have no parameter information, if we only have one function, return
 				//that, otherwise we can't decide between them
 				if( functionList.size() == 1){
 					resolvedSymbol = (ISymbol) functionList.get(0);
 				} else {
 					throw new ParserSymbolTableException( ParserSymbolTableException.r_UnableToResolveFunction );
 				}
 			} else {
 				resolvedSymbol = resolveFunction( data, functionList );
 			}
 		}
 		return resolvedSymbol;
 	}
 
 	static protected IParameterizedSymbol resolveFunction( LookupData data, List functions ) throws ParserSymbolTableException{
 		if( functions == null ){
 			return null;
 		}
 		
 		//reduce our set of candidate functions to only those who have the right number of parameters
 		reduceToViable( data, functions );
 		
 		if( data.exactFunctionsOnly && data.getTemplateParameters() == null ){
 			if( functions.size() == 1 ){
 				return (IParameterizedSymbol) functions.get( 0 );
 			} else if( functions.size() == 0 ){
 				return null;
 			} else {
 				throw new ParserSymbolTableException( ParserSymbolTableException.r_Ambiguous );
 			}
 		}
 		
 		int numSourceParams = ( data.getParameters() == null ) ? 0 : data.getParameters().size();
 		int numFns = functions.size();
 		
 		if( numSourceParams == 0 ){
 			//no parameters
 			//if there is only 1 viable function, return it, if more than one, its ambiguous
 			if( numFns == 0 ){
 				return null;
 			} else if ( numFns == 1 ){
 				return (IParameterizedSymbol)functions.get(0);
 			} else if ( numFns == 2 ){
 				for (int i = 0; i < numFns; i++) {
 					IParameterizedSymbol fn = (IParameterizedSymbol) functions.get(i);
 					if( fn.getTypeInfo().isForwardDeclaration() && fn.getTypeSymbol() != null ){
 						if( functions.contains( fn.getTypeSymbol() ) ){
 							return (IParameterizedSymbol) fn.getTypeSymbol();
 						}
 					}
 				}
 			}
 			
 			if( data.getParameters() == null )
 				throw new ParserSymbolTableException( ParserSymbolTableException.r_Ambiguous );
 		}
 		
 		IParameterizedSymbol bestFn = null;		//the best function
 		IParameterizedSymbol currFn = null;		//the function currently under consideration
 		Cost [] bestFnCost = null;				//the cost of the best function
 		Cost [] currFnCost = null;				//the cost for the current function
 				
 		TypeInfo source = null;					//parameter we are called with
 		TypeInfo target = null;					//function's parameter
 		TypeInfo voidInfo = null;				//used to compare f() and f(void)
 		
 		int comparison;
 		Cost cost = null;						//the cost of converting source to target
 		Cost temp = null;						//the cost of using a user defined conversion to convert source to target
 				 
 		boolean hasWorse = false;				//currFn has a worse parameter fit than bestFn
 		boolean hasBetter = false;				//currFn has a better parameter fit than bestFn
 		boolean ambiguous = false;				//ambiguity, 2 functions are equally good
 		boolean currHasAmbiguousParam = false;	//currFn has an ambiguous parameter conversion (ok if not bestFn)
 		boolean bestHasAmbiguousParam = false;  //bestFn has an ambiguous parameter conversion (not ok, ambiguous)
 
 		List sourceParameters = null;			//the parameters the function is being called with
 		List targetParameters = null;			//the current function's parameters
 		
 		if( numSourceParams == 0 ){
 			//f() is the same as f( void )
 			sourceParameters = new ArrayList(1);
 			voidInfo = TypeInfoProvider.getTypeInfo();
 			voidInfo.setType( TypeInfo.t_void );
 			sourceParameters.add( voidInfo );
 			numSourceParams = 1;
 		} else {
 			sourceParameters = data.getParameters();
 		}
 		
 		try {
 			for( int fnIdx = 0; fnIdx < numFns; fnIdx++ ){
 				currFn = (IParameterizedSymbol) functions.get( fnIdx );
 				
 				if( bestFn != null ){
 					if( bestFn.isForwardDeclaration() && bestFn.getTypeSymbol() == currFn ){
 						bestFn = currFn;
 						continue;
 					} else if( currFn.isForwardDeclaration() && currFn.getTypeSymbol() == bestFn ){
 						continue;
 					}
 				}
 				
 				
 				if( currFn.getParameterList().isEmpty() && !currFn.hasVariableArgs() ){
 					//the only way we get here and have no parameters, is if we are looking
 					//for a function that takes void parameters ie f( void )
 					targetParameters = new ArrayList(1);
 					targetParameters.add( currFn.getSymbolTable().newSymbol( "", TypeInfo.t_void ) ); //$NON-NLS-1$
 				} else {
 					targetParameters = currFn.getParameterList();
 				}
 				
 				int numTargetParams = targetParameters.size();
 				if( currFnCost == null ){
 					currFnCost = new Cost [ numSourceParams ];	
 				}
 				
 				comparison = 0;
 				boolean varArgs = false;
 				
 				for( int j = 0; j < numSourceParams; j++ ){
 					source = (TypeInfo) sourceParameters.get(j);
 					
 					if( j < numTargetParams )
 						target = ((ISymbol)targetParameters.get(j)).getTypeInfo();
 					else 
 						varArgs = true;
 					
 					if( varArgs ){
 						cost = new Cost( source, null );
 						cost.rank = Cost.ELLIPSIS_CONVERSION;
 					} else if ( target.getHasDefault() && source.isType( TypeInfo.t_void ) && !source.hasPtrOperators() ){
 						//source is just void, ie no parameter, if target had a default, then use that
 						cost = new Cost( source, target );
 						cost.rank = Cost.IDENTITY_RANK;
 					} else if( source.equals( target ) ){
 						cost = new Cost( source, target );
 						cost.rank = Cost.IDENTITY_RANK;	//exact match, no cost
 					} else {
 						try{
 							cost = checkStandardConversionSequence( source, target );
 							
 							//12.3-4 At most one user-defined conversion is implicitly applied to
 							//a single value.  (also prevents infinite loop)				
 							if( cost.rank == Cost.NO_MATCH_RANK && !data.forUserDefinedConversion ){
 								temp = checkUserDefinedConversionSequence( source, target );
 								if( temp != null ){
 									cost.release();
 									cost = temp;
 								}
 							}
 						} catch( ParserSymbolTableException e ) {
 							if( cost != null ) { cost.release();  cost = null; }
 							if( temp != null ) { temp.release();  temp = null; }
 							throw e;
 						} catch( ParserSymbolTableError e ) {
 							if( cost != null ) { cost.release();  cost = null; }
 							if( temp != null ) { temp.release();  temp = null; }
 							throw e;
 						}
 					}
 					
 					currFnCost[ j ] = cost;
 				}
 				
 				
 				hasWorse = false;
 				hasBetter = false;
 				//In order for this function to be better than the previous best, it must
 				//have at least one parameter match that is better that the corresponding
 				//match for the other function, and none that are worse.
 				for( int j = 0; j < numSourceParams; j++ ){ 
 					if( currFnCost[ j ].rank < 0 ){
 						hasWorse = true;
 						hasBetter = false;
 						
 						if( data.isPrefixLookup() ){
 							//for prefix lookup, just remove from the function list those functions
 							//that don't fit the parameters
 							functions.remove( fnIdx-- );
 							numFns--;
 						}
 						break;
 					}
 					
 					//an ambiguity in the user defined conversion sequence is only a problem
 					//if this function turns out to be the best.
 					currHasAmbiguousParam = ( currFnCost[ j ].userDefined == 1 );
 					
 					if( bestFnCost != null ){
 						comparison = currFnCost[ j ].compare( bestFnCost[ j ] );
 						hasWorse |= ( comparison < 0 );
 						hasBetter |= ( comparison > 0 );
 					} else {
 						hasBetter = true;
 					}
 				}
 				
 				//during a prefix lookup, we don't need to rank the functions
 				if( data.isPrefixLookup() ){
 					releaseCosts( currFnCost );
 					continue;
 				}
 				
 				//If function has a parameter match that is better than the current best,
 				//and another that is worse (or everything was just as good, neither better nor worse).
 				//then this is an ambiguity (unless we find something better than both later)	
 				ambiguous |= ( hasWorse && hasBetter ) || ( !hasWorse && !hasBetter );
 				
 				if( !hasWorse ){
 					if( !hasBetter ){
 						//if they are both template functions, we can order them that way
 						boolean bestIsTemplate = bestFn.getContainingSymbol() instanceof ITemplateSymbol;
 						boolean currIsTemplate = currFn.getContainingSymbol() instanceof ITemplateSymbol; 
 						if( bestIsTemplate && currIsTemplate )
 						{
 							try{
 								ITemplateSymbol t1 = (ITemplateSymbol) bestFn.getInstantiatedSymbol().getContainingSymbol();
 								ITemplateSymbol t2 = (ITemplateSymbol) currFn.getInstantiatedSymbol().getContainingSymbol();
 								int order = TemplateEngine.orderTemplateFunctions( t1, t2 );
 								if ( order < 0 ){
 									hasBetter = true;	 				
 								} else if( order > 0 ){
 									ambiguous = false;
 								}
 							} catch( ParserSymbolTableException e ) {
 								if( currFnCost != null ) releaseCosts( currFnCost );
 								if( bestFnCost != null ) releaseCosts( bestFnCost );
 								throw e;
 							} catch( ParserSymbolTableError e ) {
 								if( currFnCost != null ) releaseCosts( currFnCost );
 								if( bestFnCost != null ) releaseCosts( bestFnCost );
 								throw e;
 							}
 						}
 						//we prefer normal functions over template functions, unless we specified template arguments
 						else if( bestIsTemplate && !currIsTemplate ){
 							if( data.getTemplateParameters() == null )
 								hasBetter = true;
 							else
 								ambiguous = false;
 						} else if( !bestIsTemplate && currIsTemplate ){
 							if( data.getTemplateParameters() == null )
 								ambiguous = false;
 							else
 								hasBetter = true;
 						} 
 					}
 					if( hasBetter ){
 						//the new best function.
 						ambiguous = false;
 						releaseCosts( bestFnCost );
 						bestFnCost = currFnCost;
 						bestHasAmbiguousParam = currHasAmbiguousParam;
 						currFnCost = null;
 						bestFn = currFn;
 					} else {
 						releaseCosts( currFnCost );
 					}
 				} else {
 					releaseCosts( currFnCost );
 				}
 			}
 		} finally {
 			if( currFnCost != null ){
 				releaseCosts( currFnCost );
 				currFnCost = null;
 			}
 			if( bestFnCost != null ){
 				releaseCosts( bestFnCost );
 				bestFnCost = null;
 			}
 			if( voidInfo != null )
 				voidInfo.release();
 		}
 		
 		if( ambiguous || bestHasAmbiguousParam ){
 			throw new ParserSymbolTableException( ParserSymbolTableException.r_Ambiguous );
 		}
 						
 		return bestFn;
 	}
 	static private void releaseCosts( Cost [] costs ){
 		if( costs != null ) {
 			for( int i = 0; i < costs.length; i++ ){
				costs[i].release();
 			}
 		}
 	}
 	
 	static private boolean functionHasParameters( IParameterizedSymbol function, List params ){
 		if( params == null ){
 			return function.getParameterList().isEmpty();
 		}
 		//create a new function that has params as its parameters, then use IParameterizedSymbol.hasSameParameters
 		IParameterizedSymbol tempFn = function.getSymbolTable().newParameterizedSymbol( EMPTY_NAME, TypeInfo.t_function );
 		
 		int size = params.size();
 		for( int i = 0; i < size; i++ ){
 			ISymbol param = function.getSymbolTable().newSymbol( EMPTY_NAME );
 			param.setTypeInfo( (TypeInfo) params.get(i) );
 			tempFn.addParameter( param );
 		}
 		
 		return function.hasSameParameters( tempFn );
 	}
 	
 	static private void reduceToViable( LookupData data, List functions ){
 		int numParameters = ( data.getParameters() == null ) ? 0 : data.getParameters().size();
 		int num;	
 			
 		if( data.isPrefixLookup() )
 		{
 			if( numParameters >= 1 )
 				numParameters++;
 		}
 		
 		//Trim the list down to the set of viable functions
 		IParameterizedSymbol function;
 		Object obj = null;
 		int size = functions.size();
 		for( int i = 0; i < size; i++ ){
 			obj = functions.get(i);
 			//sanity check
 			if( obj instanceof IParameterizedSymbol ){
 				function = (IParameterizedSymbol) obj;
 				if( !function.isType( TypeInfo.t_function) && !function.isType( TypeInfo.t_constructor ) ){
 					functions.remove( i-- );
 					size--;
 					continue;
 				}
 			} else {
 				functions.remove( i-- );
 				size--;
 				continue;
 			}
 			
 			num = ( function.getParameterList() == null ) ? 0 : function.getParameterList().size();
 		
 			//if there are m arguments in the list, all candidate functions having m parameters
 			//are viable	 
 			if( num == numParameters ){
 				if( data.exactFunctionsOnly && !functionHasParameters( function, data.getParameters() ) ){
 					functions.remove( i-- );
 					size--;
 				}
 				continue;
 			} 
 			//check for void
 			else if( numParameters == 0 && num == 1 ){
 				ISymbol param = (ISymbol)function.getParameterList().get(0);
 				if( param.isType( TypeInfo.t_void ) )
 					continue;
 			}
 			else if( numParameters == 1 && num == 0 ){
 				TypeInfo paramType = (TypeInfo) data.getParameters().get(0);
 				if( paramType.isType( TypeInfo.t_void ) )
 					continue;
 			}
 			
 			//A candidate function having fewer than m parameters is viable only if it has an 
 			//ellipsis in its parameter list.
 			if( num < numParameters ){
 				if( function.hasVariableArgs() ) {
 					continue;
 				} 
 				//not enough parameters, remove it
 				functions.remove( i-- );
 				size--;
 			} 
 			//a candidate function having more than m parameters is viable only if the (m+1)-st
 			//parameter has a default argument
 			else {
 				if( data.isPrefixLookup() ){
 					//during prefix lookup, having more parameters than what is provided is ok
 					continue;
 				}
 				List params = function.getParameterList();
 				TypeInfo param;
 				for( int j = num - 1; j > ( numParameters - num); j-- ){
 					param = ((ISymbol)params.get(j)).getTypeInfo();
 					if( !param.getHasDefault() ){
 						functions.remove( i-- );
 						size--;
 						break;
 					}
 				}
 			}
 		}
 	}
 	
 	/**
 	 * function ProcessDirectives
 	 * @param Declaration decl
 	 * @param LookupData  data
 	 * @param LinkedList  directives
 	 * 
 	 * Go through the directives and for each nominated namespace find the
 	 * closest enclosing declaration for that namespace and decl, then add the
 	 * nominated namespace to the lookup data for consideration when we reach
 	 * the enclosing declaration.
 	 */
 	static private void processDirectives( IContainerSymbol symbol, LookupData data, List directives ){
 		IContainerSymbol enclosing = null;
 		IContainerSymbol temp = null;
 		
 		if( directives == null )
 			return;
 		
 		int size = directives.size();
 		for( int i = 0; i < size; i++ ){
 			temp = ((IUsingDirectiveSymbol) directives.get(i)).getNamespace();
 		
 			//namespaces are searched at most once
 			if( !data.visited.contains( temp ) ){
 				enclosing = getClosestEnclosingDeclaration( symbol, temp );
 						
 				//the data.usingDirectives is a map from enclosing declaration to 
 				//a list of namespaces to consider when we reach that enclosing
 				//declaration
 				ArrayList list = (data.usingDirectives == null ) 
 								? null
 								: (ArrayList) data.usingDirectives.get( enclosing );
 				if ( list == null ){
 					list = new ArrayList(4);
 					list.add( temp );
 					if( data.usingDirectives == null ){
 						data.usingDirectives = new HashMap();
 					}
 					data.usingDirectives.put( enclosing, list );
 				} else {
 					list.add( temp );
 				}
 			}
 		}
 	}
 	
 	/**
 	 * function getClosestEnclosingDeclaration
 	 * @param decl1
 	 * @param decl2
 	 * @return Declaration
 	 * 
 	 * 7.3.4-1 "During unqualified lookup, the names appear as if they were
 	 * declared in the nearest enclosing namespace which contains both the
 	 * using-directive and the nominated namespace"
 	 * 
 	 * TBD: Consider rewriting this iteratively instead of recursively, for
 	 * performance
 	 */
 	static private IContainerSymbol getClosestEnclosingDeclaration( ISymbol symbol1, ISymbol symbol2 ){
 		if( symbol1 == symbol2 ){ 
 			return ( symbol1 instanceof IContainerSymbol ) ? (IContainerSymbol) symbol1 : symbol1.getContainingSymbol();
 		}
 				
 		if( symbol1.getDepth() == symbol2.getDepth() ){
 			return getClosestEnclosingDeclaration( symbol1.getContainingSymbol(), symbol2.getContainingSymbol() );
 		} else if( symbol1.getDepth() > symbol2.getDepth() ) {
 			return getClosestEnclosingDeclaration( symbol1.getContainingSymbol(), symbol2 );
 		} else {
 			return getClosestEnclosingDeclaration( symbol1, symbol2.getContainingSymbol() );
 		}
 	}
 	
 	/**
 	 * 
 	 * @param obj
 	 * @param base
 	 * @return int
 	 * figure out if base is a base class of obj, and return the "distance" to
 	 * the base class.
 	 * ie:
 	 *     A -> B -> C
 	 * the distance from A to B is 1 and from A to C is 2. This distance is used
 	 * to rank standard pointer conversions.
 	 * 
 	 * TBD: Consider rewriting iteratively for performance.
 	 */
 	static protected int hasBaseClass( ISymbol obj, ISymbol base ) throws ParserSymbolTableException {
 		return hasBaseClass( obj, base, false );
 	}
 	
 	static private int hasBaseClass( ISymbol obj, ISymbol base, boolean throwIfNotVisible ) throws ParserSymbolTableException{
 		if( obj == base ){
 			return 0;
 		}
 		IDerivableContainerSymbol symbol = null;
 
 		if( obj instanceof IDerivableContainerSymbol ){
 			symbol = (IDerivableContainerSymbol) obj;
 		} else {
 			return -1;
 		}
 		
 		if( symbol.hasParents() ){	
 			ISymbol temp = null;
 			IDerivableContainerSymbol parent = null;
 			IDerivableContainerSymbol.IParentSymbol wrapper;
 			
 			List parents = symbol.getParents();
 			int size = parents.size();
 			
 			for( int i = 0; i < size; i++ ){
 				wrapper = (IDerivableContainerSymbol.IParentSymbol) parents.get(i);	
 				temp = wrapper.getParent();
 				boolean isVisible = ( wrapper.getAccess() == ASTAccessVisibility.PUBLIC );
 				if ( temp instanceof IDerivableContainerSymbol ){
 					parent = (IDerivableContainerSymbol)temp;
 				} else {
 					continue; 
 				}
 				if( parent == base ){
 					if( throwIfNotVisible && !isVisible )
 						throw new ParserSymbolTableException( ParserSymbolTableException.r_BadVisibility );
 					return 1;
 				} 
 				int n = hasBaseClass( parent, base, throwIfNotVisible );
 				if( n > 0 )
 					return n + 1;
 			}
 		}
 		
 		return -1;
 	}
 
 	static protected void getAssociatedScopes( ISymbol symbol, HashSet associated ){
 		if( symbol == null ){
 			return;
 		}
 		//if T is a class type, its associated classes are the class itself,
 		//and its direct and indirect base classes. its associated Namespaces are the 
 		//namespaces in which its associated classes are defined	
 		//if( symbol.getType() == TypeInfo.t_class ){
 		if( symbol instanceof IDerivableContainerSymbol ){
 			associated.add( symbol );
 			associated.add( symbol.getContainingSymbol() );
 			getBaseClassesAndContainingNamespaces( (IDerivableContainerSymbol) symbol, associated );
 		} 
 		//if T is a union or enumeration type, its associated namespace is the namespace in 
 		//which it is defined. if it is a class member, its associated class is the member's
 		//class
 		else if( symbol.getType() == TypeInfo.t_union || symbol.getType() == TypeInfo.t_enumeration ){
 			associated.add( symbol.getContainingSymbol() );
 		}
 	}
 	
 	static private void getBaseClassesAndContainingNamespaces( IDerivableContainerSymbol obj, HashSet classes ){
 		if( obj.getParents() != null ){
 			if( classes == null ){
 				return;
 			}
 			
 			List parents = obj.getParents();
 			int size = parents.size();
 			IDerivableContainerSymbol.IParentSymbol wrapper;
 			ISymbol base;
 			
 			for( int i = 0; i < size; i++ ){
 				wrapper = (IDerivableContainerSymbol.IParentSymbol) parents.get(i);	
 				base = wrapper.getParent();
 				//TODO: what about IDeferredTemplateInstance parents?
 				if( base instanceof IDerivableContainerSymbol ){
 					classes.add( base );
 					if( base.getContainingSymbol().getType() == TypeInfo.t_namespace ){
 						classes.add( base.getContainingSymbol());
 					}
 					
 					getBaseClassesAndContainingNamespaces( (IDerivableContainerSymbol) base, classes );	
 				}
 				
 			}
 		}
 	}
 	
 	static protected boolean okToAddUsingDeclaration( ISymbol obj, IContainerSymbol context ){
 		boolean okToAdd = false;
 			
 		//7.3.3-5  A using-declaration shall not name a template-id
 		if( obj.isTemplateMember() && obj.getContainingSymbol().isType( TypeInfo.t_template ) ){
 			okToAdd = false;
 		}
 		//7.3.3-4
 		else if( context.isType( TypeInfo.t_class, TypeInfo.t_struct ) ){
 			IContainerSymbol container = obj.getContainingSymbol();
 			
 			try{
 				//a member of a base class
 				if( obj.getContainingSymbol().getType() == context.getType() ){
 					okToAdd = ( hasBaseClass( context, container ) > 0 );		
 				} 
 				else if ( obj.getContainingSymbol().getType() == TypeInfo.t_union ) {
 					// TODO : must be an _anonymous_ union
 					container = container.getContainingSymbol();
 					okToAdd = ( container instanceof IDerivableContainerSymbol ) 
 							  ? ( hasBaseClass( context, container ) > 0 )
 							  : false; 
 				}
 				//an enumerator for an enumeration
 				else if ( obj.getType() == TypeInfo.t_enumerator ){
 					container = container.getContainingSymbol();
 					okToAdd = ( container instanceof IDerivableContainerSymbol ) 
 							  ? ( hasBaseClass( context, container ) > 0 )
 							  : false; 
 				}
 			} catch ( ParserSymbolTableException e ) {
 				//not going to happen since we didn't ask for the visibility exception from hasBaseClass				
 			}
 		} else {
 			okToAdd = true;
 		}	
 		
 		return okToAdd;
 	}
 
 	static private Cost lvalue_to_rvalue( TypeInfo source, TypeInfo target ){
 
 		//lvalues will have type t_type
 		if( source.isType( TypeInfo.t_type ) ){
 			source = getFlatTypeInfo( source, false );
 		}
 		
 		if( target.isType( TypeInfo.t_type ) ){
 			ISymbol symbol = target.getTypeSymbol();
 			if( symbol != null && symbol.isForwardDeclaration() && symbol.getTypeSymbol() != null ){
 				target = new TypeInfo( target );
 				target.setType( TypeInfo.t_type );
 				target.setTypeSymbol( symbol.getTypeSymbol() );
 			}
 		}
 		
 		Cost cost = new Cost( source, target );
 		
 		//if either source or target is null here, then there was a problem 
 		//with the parameters and we can't match them.
 		if( cost.getSource() == null || cost.getTarget() == null ){
 			return cost;
 		}
 		
 		TypeInfo.PtrOp op = null;
 		
 		if( cost.getSource().hasPtrOperators() ){
 			List sourcePtrs = cost.getSource().getPtrOperators();
 			TypeInfo.PtrOp ptr = (TypeInfo.PtrOp)sourcePtrs.get( 0 );
 			if( ptr.getType() == TypeInfo.PtrOp.t_reference ){
 				sourcePtrs.remove( 0 );
 			}
 			int size = sourcePtrs.size();
 			for( int i = 0; i < size; i++ ){
 				op = (TypeInfo.PtrOp) sourcePtrs.get( 0 );
 				if( op.getType() == TypeInfo.PtrOp.t_array ){
 					op.setType( TypeInfo.PtrOp.t_pointer );		
 				}
 			}
 		}
 		
 		if( cost.getTarget().hasPtrOperators() ){
 			List targetPtrs = cost.getTarget().getPtrOperators();
 			//ListIterator iterator = targetPtrs.listIterator();
 			TypeInfo.PtrOp ptr = (TypeInfo.PtrOp)targetPtrs.get(0);
 
 			if( ptr.getType() == TypeInfo.PtrOp.t_reference ){
 				targetPtrs.remove(0);
 				cost.targetHadReference = true;
 			}
 			int size = targetPtrs.size();
 			for( int i = 0; i < size; i++ ){
 				op = (TypeInfo.PtrOp) targetPtrs.get(0);
 				if( op.getType() == TypeInfo.PtrOp.t_array ){
 					op.setType( TypeInfo.PtrOp.t_pointer );		
 				}
 			}
 		}
 		
 		return cost;
 	}
 	
 	/**
 	 * qualificationConversion
 	 * @param cost
 	 * 
 	 * see spec section 4.4 regarding qualification conversions
 	 */
 	static private void qualificationConversion( Cost cost ){
 		List sourcePtrs = cost.getSource().getPtrOperators();
 		List targetPtrs = cost.getTarget().getPtrOperators();
 		int size = sourcePtrs.size();
 		int size2 = targetPtrs.size();
 		
 		TypeInfo.PtrOp op1 = null, op2 = null;
 		boolean canConvert = true;
 
 		if( size != size2 ){
 			canConvert = false;
 		} else if( size > 0 ){
 			op1 = (TypeInfo.PtrOp) sourcePtrs.get(0);
 			op2 = (TypeInfo.PtrOp) targetPtrs.get(0);
 
 			boolean constInEveryCV2k = true;
 			
 			for( int j= 1; j < size; j++ ){
 				op1 = (TypeInfo.PtrOp) sourcePtrs.get(j);
 				op2 = (TypeInfo.PtrOp) targetPtrs.get(j);
 				
 				//pointer types must be similar
 				if( op1.getType() != op2.getType() ){
 					canConvert = false;
 					break;
 				}
 				//if const is in cv1,j then const is in cv2,j.  Similary for volatile
 				if( ( op1.isConst()    && !op2.isConst()    ) ||
 				    ( op1.isVolatile() && !op2.isVolatile() )  )
 				{
 					canConvert = false;
 					break;
 				}
 				
 				//if cv1,j and cv2,j are different then const is in every cv2,k for 0<k<j
 				if( ( op1.compareCVTo( op2 ) != 0 ) && !constInEveryCV2k ){
 					canConvert = false;
 					break; 
 				}
 				
 				constInEveryCV2k &= op2.isConst();
 			}
 		}
 		
 		if( ( cost.getSource().checkBit( TypeInfo.isConst ) && !cost.getTarget().checkBit( TypeInfo.isConst ) ) ||
 			( cost.getSource().checkBit( TypeInfo.isVolatile ) && !cost.getTarget().checkBit( TypeInfo.isVolatile ) ) )
 		{
 			canConvert = false;
 		}
 
 		if( canConvert == true ){
 			cost.qualification = 1;
 			cost.rank = Cost.LVALUE_OR_QUALIFICATION_RANK;
 		} else {
 			cost.qualification = 0;
 		}
 	}
 		
 	/**
 	 * 
 	 * @param source
 	 * @param target
 	 * @return int
 	 * 
 	 * 4.5-1 char, signed char, unsigned char, short int or unsigned short int
 	 * can be converted to int if int can represent all the values of the source
 	 * type, otherwise they can be converted to unsigned int.
 	 * 4.5-2 wchar_t or an enumeration can be converted to the first of the
 	 * following that can hold it: int, unsigned int, long unsigned long.
 	 * 4.5-4 bool can be promoted to int 
 	 * 4.6 float can be promoted to double
 	 */
 	static private void promotion( Cost cost ){
 		TypeInfo src = cost.getSource();
 		TypeInfo trg = cost.getTarget();
 		 
 		int mask = TypeInfo.isShort | TypeInfo.isLong | TypeInfo.isUnsigned | TypeInfo.isLongLong | TypeInfo.isSigned;
 		
 		if( (src.isType( TypeInfo.t__Bool, TypeInfo.t_float ) || src.isType( TypeInfo.t_enumeration )) &&
 			(trg.isType( TypeInfo.t_int ) || trg.isType( TypeInfo.t_double )) )
 		{
 			if( src.getType() == trg.getType() && (( src.getTypeInfo() & mask) == (trg.getTypeInfo() & mask)) ){
 				//same, no promotion needed
 				return;	
 			}
 			
 			if( src.isType( TypeInfo.t_float ) ){ 
 				cost.promotion = trg.isType( TypeInfo.t_double ) ? 1 : 0;
 			} else {
 				cost.promotion = ( trg.isType( TypeInfo.t_int ) && trg.canHold( src ) ) ? 1 : 0;
 			}
 			
 		} else {
 			cost.promotion = 0;
 		}
 		
 		cost.rank = (cost.promotion > 0 ) ? Cost.PROMOTION_RANK : Cost.NO_MATCH_RANK;
 	}
 	
 	/**
 	 * 
 	 * @param source
 	 * @param target
 	 * @return int
 	 * 
 	 */
 	static private void conversion( Cost cost ){
 		TypeInfo src = cost.getSource();
 		TypeInfo trg = cost.getTarget();
 		
 		int temp = -1;
 		
 		cost.conversion = 0;
 		cost.detail = 0;
 		
 		if( !src.hasSamePtrs( trg ) ){
 			return;
 		} 
 		if( src.hasPtrOperators() && src.getPtrOperators().size() == 1 ){
 			TypeInfo.PtrOp ptr = (TypeInfo.PtrOp)src.getPtrOperators().get(0);
 			ISymbol srcDecl = src.isType( TypeInfo.t_type ) ? src.getTypeSymbol() : null;
 			ISymbol trgDecl = trg.isType( TypeInfo.t_type ) ? trg.getTypeSymbol() : null;
 			if( ptr.getType() == TypeInfo.PtrOp.t_pointer ){
 				if( srcDecl == null || (trgDecl == null && !trg.isType( TypeInfo.t_void )) ){
 					return;	
 				}
 				
 				//4.10-2 an rvalue of type "pointer to cv T", where T is an object type can be
 				//converted to an rvalue of type "pointer to cv void"
 				if( trg.isType( TypeInfo.t_void ) ){
 					cost.rank = Cost.CONVERSION_RANK;
 					cost.conversion = 1;
 					cost.detail = 2;
 					return;	
 				}
 				
 				cost.detail = 1;
 				
 				//4.10-3 An rvalue of type "pointer to cv D", where D is a class type can be converted
 				// to an rvalue of type "pointer to cv B", where B is a base class of D.
 				if( (srcDecl instanceof IDerivableContainerSymbol) && trgDecl.isType( srcDecl.getType() ) ){
 					try {
 						temp = hasBaseClass( srcDecl, trgDecl );
 					} catch (ParserSymbolTableException e) {
 						//not going to happen since we didn't ask for the visibility exception
 					}
 					cost.rank = ( temp > -1 ) ? Cost.CONVERSION_RANK : Cost.NO_MATCH_RANK;
 					cost.conversion = ( temp > -1 ) ? temp : 0;
 					cost.detail = 1;
 					return;
 				}
 			} else if( ptr.getType() == TypeInfo.PtrOp.t_memberPointer ){
 				//4.11-2 An rvalue of type "pointer to member of B of type cv T", where B is a class type, 
 				//can be converted to an rvalue of type "pointer to member of D of type cv T" where D is a
 				//derived class of B
 				if( srcDecl == null || trgDecl == null ){
 					return;	
 				}
 
 				TypeInfo.PtrOp srcPtr =  trg.hasPtrOperators() ? (TypeInfo.PtrOp)trg.getPtrOperators().get(0) : null;
 				if( trgDecl.isType( srcDecl.getType() ) && srcPtr != null && srcPtr.getType() == TypeInfo.PtrOp.t_memberPointer ){
 					try {
 						temp = hasBaseClass( ptr.getMemberOf(), srcPtr.getMemberOf() );
 					} catch (ParserSymbolTableException e) {
 						//not going to happen since we didn't ask for the visibility exception
 					}
 					cost.rank = ( temp > -1 ) ? Cost.CONVERSION_RANK : Cost.NO_MATCH_RANK;
 					cost.detail = 1;
 					cost.conversion = ( temp > -1 ) ? temp : 0;
 					return; 
 				}
 			}
 		} else if( !src.hasPtrOperators() ) {
 			//4.7 An rvalue of an integer type can be converted to an rvalue of another integer type.  
 			//An rvalue of an enumeration type can be converted to an rvalue of an integer type.
 			if( src.isType( TypeInfo.t__Bool, TypeInfo.t_int ) ||
 				src.isType( TypeInfo.t_float, TypeInfo.t_double ) ||
 				src.isType( TypeInfo.t_enumeration ) )
 			{
 				if( trg.isType( TypeInfo.t__Bool, TypeInfo.t_int ) ||
 					trg.isType( TypeInfo.t_float, TypeInfo.t_double ) )
 				{
 					cost.rank = Cost.CONVERSION_RANK;
 					cost.conversion = 1;	
 				}
 			}
 		}
 	}
 	
 	static private void derivedToBaseConversion( Cost cost ) throws ParserSymbolTableException{
 		TypeInfo src = cost.getSource();
 		TypeInfo trg = cost.getTarget();
 		
 		ISymbol srcDecl = src.isType( TypeInfo.t_type ) ? src.getTypeSymbol() : null;
 		ISymbol trgDecl = trg.isType( TypeInfo.t_type ) ? trg.getTypeSymbol() : null;
 		
 		if( !src.hasSamePtrs( trg ) || srcDecl == null || trgDecl == null || !cost.targetHadReference ){
 			return;
 		}
 		
 		int temp = hasBaseClass( srcDecl, trgDecl, true );
 		
 		if( temp > -1 ){
 			cost.rank = Cost.DERIVED_TO_BASE_CONVERSION;
 			cost.conversion = temp;
 		}
 	}
 	
 	static protected Cost checkStandardConversionSequence( TypeInfo source, TypeInfo target ) throws ParserSymbolTableException{
 		Cost cost = lvalue_to_rvalue( source, target );
 		
 		if( cost.getSource() == null || cost.getTarget() == null ){
 			return cost;
 		}
 			
 		if( cost.getSource().equals( cost.getTarget() ) ){
 			cost.rank = Cost.IDENTITY_RANK;
 			return cost;
 		}
 	
 		qualificationConversion( cost );
 		
 		//if we can't convert the qualifications, then we can't do anything
 		if( cost.qualification == 0 ){
 			return cost;
 		}
 		
 		//was the qualification conversion enough?
 		if( cost.getSource().isType( TypeInfo.t_type ) && cost.getTarget().isType( TypeInfo.t_type ) ){
 			if( cost.getTarget().hasSamePtrs( cost.getSource() ) ){
 				ISymbol srcSymbol = cost.getSource().getTypeSymbol();
 				ISymbol trgSymbol = cost.getTarget().getTypeSymbol();
 				if( srcSymbol != null && trgSymbol != null ){
 					if( srcSymbol.equals( trgSymbol ) )
 					{
 						return cost;
 					}
 				}
 			}
 		} else if( cost.getSource().getType() == cost.getTarget().getType() && 
 				  (cost.getSource().getTypeInfo() & ~TypeInfo.isConst & ~TypeInfo.isVolatile) == (cost.getTarget().getTypeInfo() & ~TypeInfo.isConst & ~TypeInfo.isVolatile) )
 		{
 			return cost;
 		}
 		promotion( cost );
 		if( cost.promotion > 0 || cost.rank > -1 ){
 			return cost;
 		}
 		
 		conversion( cost );
 		
 		if( cost.rank > -1 )
 			return cost;
 			
 		derivedToBaseConversion( cost );
 		
 		return cost;	
 	}
 	
 	static private Cost checkUserDefinedConversionSequence( TypeInfo source, TypeInfo target ) throws ParserSymbolTableException {
 		Cost cost = null;
 		Cost constructorCost = null;
 		Cost conversionCost = null;
 
 		ISymbol targetDecl = null;
 		ISymbol sourceDecl = null;
 		IParameterizedSymbol constructor = null;
 		IParameterizedSymbol conversion = null;
 		
 		//constructors
 		if( target.getType() == TypeInfo.t_type ){
 			targetDecl = target.getTypeSymbol();
 			if( targetDecl == null ){
 				throw new ParserSymbolTableException( ParserSymbolTableException.r_BadTypeInfo );
 			}
 			if( targetDecl.isType( TypeInfo.t_class, TypeInfo.t_union ) ){
 				LookupData data = new LookupData( EMPTY_NAME){
 					public List getParameters() { return parameters; }
 					public TypeFilter getFilter() { return CONSTRUCTOR_FILTER; }
 					private List parameters = new ArrayList( 1 );
 				};
 				data.forUserDefinedConversion = true;
 				data.getParameters().add( source );
 				
 				if( targetDecl instanceof IDeferredTemplateInstance ){
 					targetDecl = ((IDeferredTemplateInstance)targetDecl).getTemplate().getTemplatedSymbol();
 				}
 				IDerivableContainerSymbol container = (IDerivableContainerSymbol) targetDecl;
 				
 				if( !container.getConstructors().isEmpty() ){
 					ArrayList constructors = new ArrayList( container.getConstructors() );
 					constructor = resolveFunction( data, constructors );
 				}
 				if( constructor != null && constructor.getTypeInfo().checkBit( TypeInfo.isExplicit ) ){
 					constructor = null;
 				}
 				
 			}
 		}
 		
 		//conversion operators
 		if( source.getType() == TypeInfo.t_type ){
 			source = getFlatTypeInfo( source, true );
 			sourceDecl = ( source != null ) ? source.getTypeSymbol() : null;
 			source.release();
 			
 			if( sourceDecl != null && (sourceDecl instanceof IContainerSymbol) ){
 				String name = target.toString();
 				
 				if( !name.equals(EMPTY_NAME) ){
 					LookupData data = new LookupData( "operator " + name ){ //$NON-NLS-1$
 						public List getParameters() { return Collections.EMPTY_LIST; }
 						public TypeFilter getFilter() { return FUNCTION_FILTER; }
 					};
 					data.forUserDefinedConversion = true;
 					data.foundItems = lookupInContained( data, (IContainerSymbol) sourceDecl );
 					conversion = (data.foundItems != null ) ? (IParameterizedSymbol)resolveAmbiguities( data ) : null;	
 				}
 			}
 		}
 		
 		try {
 			if( constructor != null ){
 				TypeInfo info = TypeInfoProvider.getTypeInfo();
 				info.setType( TypeInfo.t_type );
 				info.setTypeSymbol( constructor.getContainingSymbol() );
 				constructorCost = checkStandardConversionSequence( info, target );
 				info.release();
 			}
 			if( conversion != null ){
 				TypeInfo info = TypeInfoProvider.getTypeInfo();
 				info.setType( target.getType() );
 				info.setTypeSymbol( target.getTypeSymbol() );
 				conversionCost = checkStandardConversionSequence( info, target );
 				info.release();
 			}
 			
 			//if both are valid, then the conversion is ambiguous
 			if( constructorCost != null && constructorCost.rank != Cost.NO_MATCH_RANK && 
 				conversionCost != null && conversionCost.rank != Cost.NO_MATCH_RANK )
 			{
 				cost = constructorCost;
 				cost.userDefined = Cost.AMBIGUOUS_USERDEFINED_CONVERSION;	
 				cost.rank = Cost.USERDEFINED_CONVERSION_RANK;
 			} else {
 				if( constructorCost != null && constructorCost.rank != Cost.NO_MATCH_RANK ){
 					cost = constructorCost;
 					cost.userDefined = constructor.hashCode();
 					cost.rank = Cost.USERDEFINED_CONVERSION_RANK;
 				} else if( conversionCost != null && conversionCost.rank != Cost.NO_MATCH_RANK ){
 					cost = conversionCost;
 					cost.userDefined = conversion.hashCode();
 					cost.rank = Cost.USERDEFINED_CONVERSION_RANK;
 				} 			
 			}
 		} finally {
 			if( constructorCost != null && constructorCost != cost )
 				constructorCost.release();
 			if( conversionCost != null && conversionCost != cost )
 				conversionCost.release();			
 		}
 		return cost;
 	}
 
 	/**
 	 *	Determine the type of a conditional operator based on the second and third operands 
 	 * @param secondOp
 	 * @param thirdOp
 	 * @return
 	 * Spec 5.16
 	 * Determine if the second operand can be converted to match the third operand, and vice versa.
 	 * - If both can be converted, or one can be converted but the conversion is ambiguous, the program
 	 * is illformed  (throw ParserSymbolTableException)
 	 * - If neither can be converted, further checking must be done (return null)
 	 * - If exactly one conversion is possible, that conversion is applied ( return the other TypeInfo )
 	 */
 	static public TypeInfo getConditionalOperand( TypeInfo secondOp, TypeInfo thirdOp ) throws ParserSymbolTableException{
 		Cost thirdCost = null, secondCost = null;
 		TypeInfo temp = null;
 		
 		try{
 			//can secondOp convert to thirdOp ?
 			temp = getFlatTypeInfo( thirdOp, true );
 			secondCost = checkStandardConversionSequence( secondOp, temp );
 	
 			if( secondCost.rank == Cost.NO_MATCH_RANK ){
 				secondCost.release();
 				secondCost = checkUserDefinedConversionSequence( secondOp, temp );
 			}
 			temp.release();
 			temp = getFlatTypeInfo( secondOp, true );
 			
 			thirdCost = checkStandardConversionSequence( thirdOp, temp );
 			if( thirdCost.rank == Cost.NO_MATCH_RANK ){
 				thirdCost.release();
 				thirdCost = checkUserDefinedConversionSequence( thirdOp, temp );
 			}
 		} finally {
 			if( thirdCost != null ) thirdCost.release();
 			if( secondCost != null ) secondCost.release();
 			if( temp != null ) temp.release();
 		}
 		
 		boolean canConvertSecond = ( secondCost != null && secondCost.rank != Cost.NO_MATCH_RANK );
 		boolean canConvertThird  = ( thirdCost  != null && thirdCost.rank  != Cost.NO_MATCH_RANK );
 
 		if( !canConvertSecond && !canConvertThird ){
 			//neither can be converted
 			return null;
 		} else if ( canConvertSecond && canConvertThird ){
 			//both can be converted -> illformed
 			throw new ParserSymbolTableException( ParserSymbolTableException.r_Ambiguous );
 		} else {
 			if( canConvertSecond ){
 				if( secondCost.userDefined == Cost.AMBIGUOUS_USERDEFINED_CONVERSION ){
 					//conversion is ambiguous -> ill-formed
 					throw new ParserSymbolTableException( ParserSymbolTableException.r_Ambiguous );
 				} 
 				return thirdOp;
 			} 
 			if( thirdCost.userDefined == Cost.AMBIGUOUS_USERDEFINED_CONVERSION )
 				//conversion is ambiguous -> ill-formed
 				throw new ParserSymbolTableException( ParserSymbolTableException.r_Ambiguous );
 		 
 			return secondOp;
 		}
 	}
 	
 	/**
 	 * 
 	 * @param usePool TODO
 	 * @param decl
 	 * @return TypeInfo
 	 * The top level TypeInfo represents modifications to the object and the
 	 * remaining TypeInfo's represent the object.
 	 */
 	static protected TypeInfo getFlatTypeInfo( TypeInfo topInfo, boolean usePool ){
 		TypeInfo returnInfo = null;
 		TypeInfo info = null;
 		
 		if( topInfo.getType() == TypeInfo.t_type && topInfo.getTypeSymbol() != null ){
 			if( usePool ) returnInfo = TypeInfoProvider.getTypeInfo();
 			else          returnInfo = new TypeInfo();
 			
 			returnInfo.setTypeInfo( topInfo.getTypeInfo() );
 			ISymbol typeSymbol = topInfo.getTypeSymbol();
 			
 			info = typeSymbol.getTypeInfo();
 			int j = 0;
 			while( info.getTypeSymbol() != null && ( info.getType() == TypeInfo.t_type || info.isForwardDeclaration() ) ){
 				typeSymbol = info.getTypeSymbol();
 				
 				returnInfo.addPtrOperator( info.getPtrOperators() );	
 				returnInfo.setTypeInfo( ( returnInfo.getTypeInfo() | info.getTypeInfo() ) & ~TypeInfo.isTypedef & ~TypeInfo.isForward );
 				info = typeSymbol.getTypeInfo();
 				if( ++j > TYPE_LOOP_THRESHOLD ){
 					if( usePool )
 						returnInfo.release();
 					throw new ParserSymbolTableError();
 				}
 			}
 			
 			if( info.isType( TypeInfo.t_class, TypeInfo.t_enumeration ) || info.isType( TypeInfo.t_function ) ){
 				returnInfo.setType( TypeInfo.t_type );
 				returnInfo.setTypeSymbol( typeSymbol );
 			} else {
 				returnInfo.setTypeInfo( ( returnInfo.getTypeInfo() | info.getTypeInfo() ) & ~TypeInfo.isTypedef & ~TypeInfo.isForward );
 				returnInfo.setType( info.getType() );
 				returnInfo.setTypeSymbol( null );
 				returnInfo.addPtrOperator( info.getPtrOperators() );
 			}
 			if( returnInfo.isType( TypeInfo.t_templateParameter ) ){
 				returnInfo.setTypeSymbol( typeSymbol );
 			}
 			returnInfo.applyOperatorExpressions( topInfo.getOperatorExpressions() );
 			
 			if( topInfo.hasPtrOperators() ){
 				TypeInfo.PtrOp topPtr = (PtrOp) topInfo.getPtrOperators().get(0);
 				TypeInfo.PtrOp ptr = new PtrOp( topPtr.getType(), topPtr.isConst(), topPtr.isVolatile() );
 				returnInfo.addPtrOperator( ptr );
 			}
 		} else {
 			if( usePool ){
 				returnInfo = TypeInfoProvider.getTypeInfo();
 				returnInfo.copy( topInfo );
 			} else			
 				returnInfo = new TypeInfo( topInfo );
 		}
 		
 		return returnInfo;	
 	}
 
 	private IContainerSymbol _compilationUnit;
 	private ParserLanguage   _language;
 	private ParserMode		 _mode;
 	
 	public void setLanguage( ParserLanguage language ){
 		_language = language;
 	}
 	
 	public ParserLanguage getLanguage(){
 		return _language;
 	}
 	
 	public ParserMode getParserMode(){
 		return _mode;
 	}
 	
 //	protected void pushCommand( Command command ){
 //		undoList.addFirst( command );
 //	}
 	
 //	public Mark setMark(){
 //		Mark mark = new Mark();
 //		undoList.addFirst( mark );
 //		markSet.add( mark );
 //		return mark;
 //	}
 	
 //	public boolean rollBack( Mark toMark ){
 //		if( markSet.contains( toMark ) ){
 //			markSet.remove( toMark );
 //			Command command = ( Command )undoList.removeFirst();
 //			while( command != toMark ){
 //				command.undoIt();
 //				command = ( Command ) undoList.removeFirst();
 //			}
 //			
 //			return true;
 //		} 
 //		
 //		return false;
 //	}
 	
 //	public boolean commit( Mark toMark ){
 //		if( markSet.contains( toMark ) ){
 //			markSet.remove( toMark );
 //			Command command = ( Command )undoList.removeLast();
 //			while( command != toMark ){
 //				command = (Command) undoList.removeLast();
 //			}
 //			return true;
 //		}
 //		
 //		return false;
 //	}
 	
 //	static abstract protected class Command{
 //		abstract public void undoIt();
 //	}
 //	
 //	static public class Mark extends Command{
 //		public void undoIt(){ }
 //	}
 	
 
 	
 	static protected class LookupData
 	{
 		protected static final TypeFilter ANY_FILTER = new TypeFilter( TypeInfo.t_any );
 		protected static final TypeFilter CONSTRUCTOR_FILTER = new TypeFilter( TypeInfo.t_constructor );
 		protected static final TypeFilter FUNCTION_FILTER = new TypeFilter( TypeInfo.t_function );
 		
 		public String name;
 		public Map usingDirectives; 
 		public Set visited = new HashSet();	//used to ensure we don't visit things more than once
 		public HashSet inheritanceChain;	//used to detect circular inheritance
 		public ISymbol templateMember;  	//to assit with template member defs
 		
 		public boolean qualified = false;
 		public boolean ignoreUsingDirectives = false;
 		public boolean usingDirectivesOnly = false;
 		public boolean forUserDefinedConversion = false;
 		public boolean exactFunctionsOnly = false;
 		public boolean returnInvisibleSymbols = false;
 		
 		public Map foundItems = null;
 		
 		public LookupData( String n ){
 			name = n;
 		}
 
 		//the following function are optionally overloaded by anonymous classes deriving from 
 		//this LookupData
 		public boolean isPrefixLookup(){ return false;}       //prefix lookup
 		public Set getAmbiguities()    { return null; }       
 		public void addAmbiguity(String n ) {	}
 		public List getParameters()    { return null; }       //parameter info for resolving functions
 		public HashSet getAssociated() { return null; }       //associated namespaces for argument dependant lookup
 		public ISymbol getStopAt()     { return null; }       //stop looking along the stack once we hit this declaration
 		public List getTemplateParameters() { return null; }  //template parameters
 		public TypeFilter getFilter() { return ANY_FILTER; }
 	}
 
 	
 	static protected class Cost
 	{
 		
 		public Cost( TypeInfo s, TypeInfo t ){
 			source = TypeInfoProvider.getTypeInfo();
 			if( s != null )
 				source.copy( s );
 			
 			target = TypeInfoProvider.getTypeInfo();
 			if( t != null )
 				target.copy( t );
 		}
 		
 		private TypeInfo source;
 		private TypeInfo target;
 		
 		public boolean targetHadReference = false;
 		
 		public int lvalue;
 		public int promotion;
 		public int conversion;
 		public int qualification;
 		public int userDefined;
 		public int rank = -1;
 		public int detail;
 		
 		//Some constants to help clarify things
 		public static final int AMBIGUOUS_USERDEFINED_CONVERSION = 1;
 		
 		public static final int NO_MATCH_RANK = -1;
 		public static final int IDENTITY_RANK = 0;
 		public static final int LVALUE_OR_QUALIFICATION_RANK = 0;
 		public static final int PROMOTION_RANK = 1;
 		public static final int CONVERSION_RANK = 2;
 		public static final int DERIVED_TO_BASE_CONVERSION = 3;
 		public static final int USERDEFINED_CONVERSION_RANK = 4;
 		public static final int ELLIPSIS_CONVERSION = 5;
 
 		public void release(){
 			getSource().release();
 			getTarget().release();
 		}
 		
 		public int compare( Cost cost ){
 			int result = 0;
 			
 			if( rank != cost.rank ){
 				return cost.rank - rank;
 			}
 			
 			if( userDefined != 0 || cost.userDefined != 0 ){
 				if( userDefined == 0 || cost.userDefined == 0 ){
 					return cost.userDefined - userDefined;
 				} 
 				if( (userDefined == AMBIGUOUS_USERDEFINED_CONVERSION || cost.userDefined == AMBIGUOUS_USERDEFINED_CONVERSION) ||
 					(userDefined != cost.userDefined ) )
 						return 0;
 		 
 					// else they are the same constructor/conversion operator and are ranked
 					//on the standard conversion sequence
 		
 			}
 			
 			if( promotion > 0 || cost.promotion > 0 ){
 				result = cost.promotion - promotion;
 			}
 			if( conversion > 0 || cost.conversion > 0 ){
 				if( detail == cost.detail ){
 					result = cost.conversion - conversion;
 				} else {
 					result = cost.detail - detail;
 				}
 			}
 			
 			if( result == 0 ){
 				if( cost.qualification != qualification ){
 					return cost.qualification - qualification;
 				} else if( (cost.qualification == qualification) && qualification == 0 ){
 					return 0;
 				} else {
 					int size = cost.getTarget().hasPtrOperators() ? cost.getTarget().getPtrOperators().size() : 0;
 					int size2 = getTarget().hasPtrOperators() ? getTarget().getPtrOperators().size() : 0;
 					
 					ListIterator iter1 = cost.getTarget().getPtrOperators().listIterator( size );
 					ListIterator iter2 = getTarget().getPtrOperators().listIterator( size2 );
 					
 					TypeInfo.PtrOp op1 = null, op2 = null;
 					
 					int subOrSuper = 0;
 					for( int i = ( size < size2 ) ? size : size2; i > 0; i-- ){
 						op1 = (TypeInfo.PtrOp)iter1.previous();
 						op2 = (TypeInfo.PtrOp)iter2.previous();
 						
 						if( subOrSuper == 0)
 							subOrSuper = op1.compareCVTo( op2 );
 						else if( ( subOrSuper > 0 && ( op1.compareCVTo( op2 ) < 0 )) ||
 								 ( subOrSuper < 0 && ( op1.compareCVTo( op2 ) > 0 )) )
 						{
 							result = -1;
 							break;	
 						}
 					}
 					if( result == -1 ){
 						result = 0;
 					} else {
 						if( size == size2 ){
 							result = subOrSuper;
 						} else {
 							result = size - size2; 
 						}
 					}
 				}
 			}
 			 
 			return result;
 		}
 
 		/**
 		 * @return Returns the source.
 		 */
 		public TypeInfo getSource() {
 			return source;
 		}
 		
 		/**
 		 * @return Returns the target.
 		 */
 		public TypeInfo getTarget() {
 			return target;
 		}
 	}
 
 	/**
 	 * The visibility of the symbol is modified by the visibility of the base classes
 	 * @param symbol
 	 * @param qualifyingSymbol
 	 * @return
 	 */
 	public static ASTAccessVisibility getVisibility(ISymbol symbol, IContainerSymbol qualifyingSymbol){
 		
 		IContainerSymbol container = symbol.getContainingSymbol();
 		if( qualifyingSymbol == null || container.equals( qualifyingSymbol ) ){
 			ISymbolASTExtension extension = symbol.getASTExtension();
 			IASTNode node = extension != null ? extension.getPrimaryDeclaration() : null;
 			if( node != null && node instanceof IASTMember ){
 				return ((IASTMember)node).getVisiblity();
 			} 
 			throw new ParserSymbolTableError( ParserSymbolTableError.r_InternalError );
 		}
 		
 		if( ! (qualifyingSymbol instanceof IDerivableContainerSymbol) ){
 			return ASTAccessVisibility.PUBLIC;
 		}
 		
 		List parents = ((IDerivableContainerSymbol) qualifyingSymbol).getParents();
 		int numParents = parents.size();
 		IParentSymbol parent = null;
 		ASTAccessVisibility symbolAccess = null;
 		ASTAccessVisibility parentAccess = null;
 		
 		for( int i = 0; i < numParents; i++ ){
 			parent = (IParentSymbol) parents.get(i);
 			
 			if( container == parent.getParent() ){
 				parentAccess = parent.getAccess();
 				symbolAccess = ((IASTMember)symbol.getASTExtension().getPrimaryDeclaration()).getVisiblity();
 				
 				return ( parentAccess.isGreaterThan( symbolAccess ) )? parentAccess : symbolAccess;					
 			}
 		}
 		
 		//if static or an enumerator, the symbol could be visible through more than one path through the heirarchy,
 		//so we need to check all paths
 		boolean checkAllPaths = ( symbol.isType( TypeInfo.t_enumerator ) || symbol.getTypeInfo().checkBit( TypeInfo.isStatic ) );
 		ASTAccessVisibility resultingAccess = null;
 		for( int i = 0; i < numParents; i++ ){
 			parent = (IParentSymbol) parents.get(i);
 			parentAccess = parent.getAccess();
 			
 			ISymbol tmp = parent.getParent();
 			if( tmp instanceof IDeferredTemplateInstance )
 				tmp = ((IDeferredTemplateInstance)tmp).getTemplate().getTemplatedSymbol();
 			else if( tmp instanceof ITemplateSymbol ){
 				tmp = ((ITemplateSymbol)tmp).getTemplatedSymbol();
 			}
 			
 			if( !( tmp instanceof IContainerSymbol ) )
 				return null;
 				
 			symbolAccess = getVisibility( symbol, (IContainerSymbol) tmp );
 			
 			if( symbolAccess != null ){
 				symbolAccess = ( parentAccess.isGreaterThan( symbolAccess ) ) ? parentAccess : symbolAccess; 
 				if( checkAllPaths ){
 					if( resultingAccess != null )
 						resultingAccess = resultingAccess.isGreaterThan( symbolAccess ) ? symbolAccess : resultingAccess;
 					else
 						resultingAccess = symbolAccess;
 				} else {
 					return symbolAccess;
 				}
 			}
 		}
 		return resultingAccess;
 	}
 	
 	public static class TypeInfoProvider
 	{
 		private static final int POOL_SIZE = 16;
 		private static final TypeInfo [] pool;
 		private static final boolean [] free;
 		private static int firstFreeHint = 0;
 
 		static
 		{
 			pool = new TypeInfo[ POOL_SIZE ];
 			free = new boolean[POOL_SIZE];
 			for( int i = 0; i < POOL_SIZE; i++ )
 			{
 				pool[i] = new TypeInfo();
 				free[i] = true;
 			}
 		}	
 
 		public static synchronized TypeInfo getTypeInfo()
 		{
 			for( int i = firstFreeHint; i < POOL_SIZE; ++i )
 			{
 				if( free[i] )
 				{
 					free[i] = false;
 					firstFreeHint = i + 1;
 					return pool[i];
 				}
 			}
 			//if there is nothing free, just give them a new one
 			return new TypeInfo();
 		}
 		
 		public static synchronized void returnTypeInfo( TypeInfo t )
 		{
 			for( int i = 0; i < POOL_SIZE; i++ ){
 				if( pool[i] == t ){
 					t.clear();
 					free[i] = true;
 					if( i < firstFreeHint ){
 						firstFreeHint = i;
 					}
 					return;
 				}
 			}
 			//else it was one allocated outside the pool
 		}
 		
 		public static synchronized int numAllocated(){
			int num = 0;
			for( int i = 0; i < POOL_SIZE; i++ ){
				if( !free[i] )
					num++;
			}
			return num;
 		}
 	}
 }
