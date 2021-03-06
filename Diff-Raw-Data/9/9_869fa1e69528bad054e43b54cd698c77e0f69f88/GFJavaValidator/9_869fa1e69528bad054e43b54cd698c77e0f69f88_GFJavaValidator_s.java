 package org.grammaticalframework.eclipse.validation;
 
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.LinkedHashSet;
 
 import org.eclipse.emf.common.util.URI;
 import org.eclipse.emf.ecore.EObject;
 import org.eclipse.xtext.naming.IQualifiedNameConverter;
 import org.eclipse.xtext.naming.QualifiedName;
 import org.eclipse.xtext.resource.IEObjectDescription;
 import org.eclipse.xtext.resource.IResourceDescription;
 import org.eclipse.xtext.resource.IResourceDescriptions;
 import org.eclipse.xtext.scoping.IScope;
 import org.eclipse.xtext.scoping.IScopeProvider;
 import org.eclipse.xtext.validation.Check;
 import org.grammaticalframework.eclipse.gF.*;
 import org.grammaticalframework.eclipse.scoping.GFGlobalScopeProvider;
 import org.grammaticalframework.eclipse.scoping.GFLibraryAgent;
 
 import com.google.inject.Inject;
  
 
 public class GFJavaValidator extends AbstractGFJavaValidator {
 	
 	@Inject
 	private IScopeProvider scopeProvider;
 	protected IScopeProvider getScopeProvider() {
 		return scopeProvider;
 	}
 
 	@Inject
 	private GFGlobalScopeProvider provider;
 	
 	@Inject
 	private IQualifiedNameConverter converter = new IQualifiedNameConverter.DefaultImpl();
 	protected IQualifiedNameConverter getConverter() {
 		return converter;
 	}
 
 	@Inject
 	private GFLibraryAgent libAgent;
 
 	// ==============================================
 
 	/**
 	 * It is a compiler error for a module's name not to match its filename
 	 * @param modtype
 	 */
 	@Check
 	public void checkModuleNameMatchesFileName(ModType modtype) {
 		String idealName = modtype.eResource().getURI().trimFileExtension().lastSegment();
 		if (!modtype.getName().getS().equals(idealName)  ) {
 			String msg = String.format("Module name \"%s\" differs from file name \"%s\"", modtype.getName().getS(), idealName);
 			error(msg, GFPackage.Literals.MOD_TYPE__NAME);
 		}
 	}
 
 	/**
 	 * Warn when referencing a module which does not exist.
 	 * @param modtype
 	 */
 	@Check
 	public void checkAbstractModuleExists(ModType modtype) {
 		// Concrete, Instance
 		if (modtype.getAbstractName().getS() != null) {
 			if (!libAgent.moduleExists(modtype.eResource(), modtype.getAbstractName().getS())) {
 				String msg = String.format("Module \"%s\" not found", modtype.getAbstractName().getS());
 				warning(msg, GFPackage.Literals.MOD_TYPE__ABSTRACT_NAME);
 			}
 		}
 	}
 	@Check
 	public void checkReferencedModuleExists(Open open) {
 		// Opens, Instantiations
 		if (!libAgent.moduleExists(open.eResource(), open.getName().getS())) {
 			String msg = String.format("Module \"%s\" not found", open.getName().getS());
 			warning(msg, GFPackage.Literals.OPEN__NAME);
 		}
 	}
 	@Check
 	public void checkReferencedModuleExists(Included inc) {
 		// Extends, Functor instantiation
 		if (!libAgent.moduleExists(inc.eResource(), inc.getName().getS())) {
 			String msg = String.format("Module \"%s\" not found", inc.getName().getS());
 			warning(msg, GFPackage.Literals.INCLUDED__NAME);
 		}
 	}
 
 	/**
 	 * Some special flag checks.
 	 * @param flagdef
 	 */
 	@Check
 	public void checkFlags(FlagDef flagdef) {
 		if (flagdef.getName().getS().equals("startcat")) {
 			String startCat = flagdef.getValue().getS();
 			IScope scope = getScopeProvider().getScope(flagdef, GFPackage.Literals.FLAG_DEF__NAME);
 			if (scope.getSingleElement( getConverter().toQualifiedName(startCat) ) == null) {
 				String msg = String.format("Start category \"%1$s\" not found", startCat);
 				warning(msg, GFPackage.Literals.FLAG_DEF__VALUE);
 			}
 		}
 	}
 
 	/**
 	 * It is a compiler error to have a category declaration in a concrete module, and so on.
 	 * @param topdef
 	 */
 	@Check
 	public void checkDefsAreInCorrectModuleTypes(TopDef topdef) {
 		// Ascend to module
 		EObject temp = topdef;
 		while (!(temp  instanceof ModDef) && temp.eContainer() != null) {
 			temp = temp.eContainer();
 		}
 		ModType modtype = ((ModDef)temp).getType();
 
 		// Shouldn't be in concrete/resource
 		if (topdef.isCat() && (modtype.isConcrete() || modtype.isResource())) {
 			String msg = String.format("Category declarations don't belong in a concrete module");
 			error(msg, GFPackage.Literals.TOP_DEF__CAT);
 		}
 		if (topdef.isFun() && (modtype.isConcrete() || modtype.isResource())) {
 			String msg = String.format("Function declarations don't belong in a concrete module");
 			error(msg, GFPackage.Literals.TOP_DEF__FUN);
 		}
 		if (topdef.isDef() && (modtype.isConcrete() || modtype.isResource())) {
 			String msg = String.format("Function definitions don't belong in a concrete module");
 			error(msg, GFPackage.Literals.TOP_DEF__DEF);
 		}
 		
 		// Shouldn't be in abstract
 		if (topdef.isParam() && modtype.isAbstract()) {
 			String msg = String.format("Parameter type definitions don't belong in an abstract module");
 			error(msg, GFPackage.Literals.TOP_DEF__LINCAT);
 		}
 		if (topdef.isLincat() && modtype.isAbstract()) {
 			String msg = String.format("Linearization type definitions don't belong in an abstract module");
 			error(msg, GFPackage.Literals.TOP_DEF__LINCAT);
 		}
		if (topdef.isLindef() && modtype.isAbstract()) {
			String msg = String.format("Linearization default definitions don't belong in an abstract module");
			error(msg, GFPackage.Literals.TOP_DEF__LINDEF);
		}
 		if (topdef.isLin() && modtype.isAbstract()) {
 			String msg = String.format("Linearization definitions don't belong in an abstract module");
 			error(msg, GFPackage.Literals.TOP_DEF__LIN);
 		}
 		if (topdef.isPrintname() && modtype.isAbstract()) {
 			String msg = String.format("Printname definitions don't belong in an abstract module");
 			error(msg, GFPackage.Literals.TOP_DEF__LIN);
 		}
 
 	}
 	
 	/**
 	 * Warn about lineariation rules not having any corresponding abstract declarations
 	 * @param name
 	 */
 	@Check
 	public void checkLinearisationsHaveAbstractEquivalents(Name name) {
 		if (name.eContainer().eContainer() instanceof TopDef) {
 			TopDef topDef = (TopDef) name.eContainer().eContainer();
 			IScope scope = getScopeProvider().getScope(name, GFPackage.Literals.NAME__NAME);
 			boolean found = (scope.getSingleElement(getConverter().toQualifiedName(name.getName().getS())) != null); 
 			if (topDef.isLincat() && !found) {
 				String msg = String.format("No declaration \"cat %1$s\" found for \"lincat %1$s\"",	name.getName().getS());
 				warning(msg, GFPackage.Literals.NAME__NAME);
 			}
 			else if (topDef.isLin() && !found) {
 				String msg = String.format("No declaration \"fun %1$s\" found for \"lin %1$s\"", name.getName().getS());
 				warning(msg, GFPackage.Literals.NAME__NAME);
 			}
 		}
 	}
 	
 	/**
 	 * Warn about lineariation rules not having any corresponding abstract declarations
 	 * @param name
 	 */
 	@Check
 	public void checkOperatorOverloadsNamesMatch(Name name) {
 		
 		if (name.eContainer() instanceof Def && name.eContainer().eContainer() instanceof Def) {
 			Def parent = (Def) name.eContainer().eContainer();
 			if (parent.isOverload()) {
 				// Convert to list of strings to be able to make comparison
 				ArrayList<String> parentNames = new ArrayList<String>();
 				for (Name n : parent.getName())
 					parentNames.add(n.getName().getS());
 				
 				if (!parentNames.contains(name.getName().getS())) {
 					StringBuilder parentNamesSB = new StringBuilder();
 					Iterator<Name> i = parent.getName().iterator();
 					while (i.hasNext()) {
 					    parentNamesSB.append(i.next().getName().getS());
 					    if (i.hasNext())
 					    	parentNamesSB.append(", ");
 					}
 					String msg = String.format("Oper name \"%1$s\" does not occur in parent overload name \"%2$s\"", name.getName().getS(), parentNamesSB.toString());
 					warning(msg, GFPackage.Literals.NAME__NAME);
 				}
 			}
 			
 		}
 		
 	}
 
 	/**
 	 * The lexer will treat ResEng.Gender as Ident.Label, rather than as single Ident. Thus cross-referencing is only
 	 * checked on the module name ResEng, but not on the member Gender.
 	 * This method exists to perform this exact checking as a post-process to the generated parser.
 	 * @param label
 	 */
 	@Check
 	public void checkQualifiedNames(Label label) {
 		
 		// Try get first bit of qualified name, i.e. "ResEng". Labels do no necessarily follow Idents, but ANY type of Exp6.
 		try {
 			Ident qualifier = ((Exp5)label.eContainer()).getV().getName();
 			QualifiedName fullyQualifiedName = getConverter().toQualifiedName(qualifier.getS() + "." + label.getName().getS());
 			
 			// See if the qualifier is a valid MODULE name
 			EObject temp = label;
 			while (!(temp  instanceof TopDef) && temp.eContainer() != null) {
 				temp = temp.eContainer();
 			}
 			TopDef topDef = (TopDef)temp;
 			IScope scope = getScopeProvider().getScope(topDef, GFPackage.Literals.TOP_DEF__DEFINITIONS);
 			if (scope.getSingleElement(qualifier) != null) {
 				
 				// We now we are dealing with a Qualified name, now see if the full thing is valid:
 				if (scope.getSingleElement(fullyQualifiedName) == null) {
 					String msg = String.format("No declaration \"%1$s\" found in module \"%2$s\"", label.getName().getS(), qualifier.getS());
 					error(msg, GFPackage.Literals.LABEL__NAME);
 				}
 			}
 			
 		} catch (Exception _) {
 			// just means the first part wasn't an Ident
 		}
 	}
 	
 	
 	/**
 	 * Warn when functor instantiations don't fully instantiate their functor
 	 * @param open
 	 */
 	@Check
 	public void checkFunctorInstantiations(ModBody modBody) {
 		if (modBody.isFunctorInstantiation()) {
 			// Get list of what the functor itself OPENs
 			Ident functorName = modBody.getFunctor().getName();
 			ArrayList<String> functorOpens = new ArrayList<String>();
 			URI uri = libAgent.getModuleURI(modBody.eResource(), functorName.getS() );
 			if (!libAgent.moduleExists(modBody.eResource(), functorName.getS())) {
 				// This should have already been checked
 //				String msg = String.format("Cannot find module \"%1$s\"", functorName.getS());
 //				error(msg, GFPackage.Literals.OPEN__NAME);
 				return;
 			}
 			final LinkedHashSet<URI> uriAsCollection = new LinkedHashSet<URI>(1);
 			uriAsCollection.add(uri);
 			IResourceDescriptions descriptions = provider.getResourceDescriptions(modBody.eResource(), uriAsCollection);
 			IResourceDescription desc = descriptions.getResourceDescription(uri);
 			// TODO Checking via regexp is very bad! But it works >:(
 			for (IEObjectDescription qn : desc.getExportedObjectsByType(GFPackage.Literals.IDENT)) {
 				if(qn.getEObjectURI().toString().matches("^.*?//@body/@opens.[0-9]+/@name$")) {
 					functorOpens.add(qn.getName().getLastSegment());
 				}
 			}
 			
 			ArrayList<String> thisOpens = new ArrayList<String>();
 			for (Open o : modBody.getInstantiations())
 				thisOpens.add(o.getAlias().getS());
 			
 			// Check that we are instantiating one of them
 			if (!thisOpens.containsAll(functorOpens)) {
 				StringBuilder msg = new StringBuilder();
 				msg.append( String.format("Instantiation of functor \"%1$s\" must instantiate: ", functorName.getS()) );
 				Iterator<String> i = functorOpens.iterator();
 				while (i.hasNext()) {
 					msg.append(i.next());
 					if (i.hasNext())
 						msg.append(", ");
 				}
 				error(msg.toString(), GFPackage.Literals.MOD_BODY__FUNCTOR_INSTANTIATION);
 			}
 		}
 	}
 
 }
