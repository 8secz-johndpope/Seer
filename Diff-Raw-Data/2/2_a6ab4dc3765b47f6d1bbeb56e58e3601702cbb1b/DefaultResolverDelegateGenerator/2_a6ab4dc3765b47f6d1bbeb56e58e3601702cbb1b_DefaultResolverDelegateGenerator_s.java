 /*******************************************************************************
  * Copyright (c) 2006-2010 
  * Software Technology Group, Dresden University of Technology
  * 
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0 
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *   Software Technology Group - TU Dresden, Germany 
  *      - initial API and implementation
  ******************************************************************************/
 package org.emftext.sdk.codegen.resource.generators;
 
 import static org.emftext.sdk.codegen.resource.generators.IClassNameConstants.ADAPTER;
 import static org.emftext.sdk.codegen.resource.generators.IClassNameConstants.E_ATTRIBUTE;
 import static org.emftext.sdk.codegen.resource.generators.IClassNameConstants.E_CLASS;
 import static org.emftext.sdk.codegen.resource.generators.IClassNameConstants.E_LIST;
 import static org.emftext.sdk.codegen.resource.generators.IClassNameConstants.E_OBJECT;
 import static org.emftext.sdk.codegen.resource.generators.IClassNameConstants.E_OPERATION;
 import static org.emftext.sdk.codegen.resource.generators.IClassNameConstants.E_REFERENCE;
 import static org.emftext.sdk.codegen.resource.generators.IClassNameConstants.E_STRUCTURAL_FEATURE;
 import static org.emftext.sdk.codegen.resource.generators.IClassNameConstants.ILLEGAL_ARGUMENT_EXCEPTION;
 import static org.emftext.sdk.codegen.resource.generators.IClassNameConstants.INTERNAL_E_OBJECT;
 import static org.emftext.sdk.codegen.resource.generators.IClassNameConstants.ITERATOR;
 import static org.emftext.sdk.codegen.resource.generators.IClassNameConstants.LINKED_HASH_MAP;
 import static org.emftext.sdk.codegen.resource.generators.IClassNameConstants.LIST;
 import static org.emftext.sdk.codegen.resource.generators.IClassNameConstants.MAP;
 import static org.emftext.sdk.codegen.resource.generators.IClassNameConstants.NOTIFICATION;
 import static org.emftext.sdk.codegen.resource.generators.IClassNameConstants.NOTIFIER;
 import static org.emftext.sdk.codegen.resource.generators.IClassNameConstants.RESOURCE;
 import static org.emftext.sdk.codegen.resource.generators.IClassNameConstants.RESOURCE_SET;
 import static org.emftext.sdk.codegen.resource.generators.IClassNameConstants.RUNTIME_EXCEPTION;
 import static org.emftext.sdk.codegen.resource.generators.IClassNameConstants.URI;
 
 import org.emftext.sdk.codegen.composites.JavaComposite;
 import org.emftext.sdk.codegen.composites.StringComposite;
 import org.emftext.sdk.codegen.parameters.ArtifactParameter;
 import org.emftext.sdk.codegen.resource.GenerationContext;
 
 public class DefaultResolverDelegateGenerator extends JavaBaseGenerator<ArtifactParameter<GenerationContext>> {
 
 	@Override
 	public void generateJavaContents(JavaComposite sc) {
 		
 		sc.add("package " + getResourcePackageName() + ";");
         sc.addLineBreak();
         
 		sc.add("public class " + getResourceClassName() + "<ContainerType extends " + E_OBJECT + ", ReferenceType extends " + E_OBJECT + "> {");
 		sc.addLineBreak();
 		addInnerClassReferenceCache(sc);
 		addFields(sc);
 		addMethods(sc);
 		sc.add("}");
 	}
 
 	private void addInnerClassReferenceCache(StringComposite sc) {
 		sc.add("private static class ReferenceCache implements " + iReferenceCacheClassName + ", " + ADAPTER + " {");
 		sc.addLineBreak();
 		sc.add("private " + MAP + "<String, Object> cache = new " + LINKED_HASH_MAP + "<String, Object>();");
 		sc.add("private " + NOTIFIER + " target;");
 		sc.addLineBreak();
 		sc.add("public " + NOTIFIER + " getTarget() {");
 		sc.add("return target;");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add("public boolean isAdapterForType(Object arg0) {");
 		sc.add("return false;");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add("public void notifyChanged(" + NOTIFICATION + " arg0) {");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add("public void setTarget(" + NOTIFIER + " arg0) {");
 		sc.add("target = arg0;");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add("public Object get(String identifier) {");
 		sc.add("return cache.get(identifier);");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add("public void put(String identifier, Object newObject) {");
 		sc.add("cache.put(identifier, newObject);");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add("}");
 		sc.addLineBreak();
 	}
 
 	private void addMethods(JavaComposite sc) {
 		addResolveMethod(sc);
 		addCheckElementMethod(sc);
 		addCastMethod(sc);
 		addProduceDeResolveErrorMessage(sc);
 		addDeResolveMethod(sc);
 		addMatchesMethod1(sc);
 		addMatchesMethod2(sc);
 		addGetNameMethod(sc);
 		addHasCorrectTypeMethod(sc);
 		addLoadResourceMethod(sc);
 		addGetUriMethod(sc);
 		addGetCacheMethod(sc);
 	}
 
 	private void addGetCacheMethod(StringComposite sc) {
 		sc.add("protected " + iReferenceCacheClassName + " getCache(" + E_OBJECT + " object) {");
 		sc.add(E_OBJECT + " root = " + eObjectUtilClassName + ".findRootContainer(object);");
 		sc.add(LIST + "<" + ADAPTER + "> eAdapters = root.eAdapters();");
 		sc.add("for (" + ADAPTER + " adapter : eAdapters) {");
 		sc.add("if (adapter instanceof ReferenceCache) {");
 		sc.add("ReferenceCache cache = (ReferenceCache) adapter;");
 		sc.add("return cache;");
 		sc.add("}");
 		sc.add("}");
 		sc.add("ReferenceCache cache = new ReferenceCache();");
 		sc.add("root.eAdapters().add(cache);");
 		sc.add("return cache;");
 		sc.add("}");
 	}
 
 	private void addLoadResourceMethod(JavaComposite sc) {
 		sc.add("private " + E_OBJECT + " loadResource(" + RESOURCE_SET + " resourceSet, " + URI + " uri) {");
 		sc.add("try {");
 		sc.add(RESOURCE + " resource = resourceSet.getResource(uri, true);");
 		sc.add(E_LIST + "<" + E_OBJECT + "> contents = resource.getContents();");
 		sc.add("if (contents.size() > 0) {");
 		sc.add("return contents.get(0);");
 		sc.add("}");
 		sc.add("} catch (" + RUNTIME_EXCEPTION + " re) {");
 		sc.addComment("do nothing here. if no resource can be loaded the uriString is probably not a valid resource URI");
 		sc.add("}");
 		sc.add("return null;");
 		sc.add("}");
 		sc.addLineBreak();
 	}
 
 	private void addGetUriMethod(JavaComposite sc) {
 		sc.add("private " + URI + " getURI(String identifier, " + URI + " baseURI) {");
 		sc.add("if (identifier == null) {");
 		sc.add("return null;");
 		sc.add("}");
 		sc.add("try {");
 		sc.add(URI + " uri = " + URI + ".createURI(identifier);");
 		sc.add("if (uri.isRelative()) {");
 		sc.add("uri = uri.resolve(baseURI);");
 		sc.add("}");
 		sc.add("return uri;");
 		sc.add("} catch (" + ILLEGAL_ARGUMENT_EXCEPTION + " iae) {");
 		sc.addComment("the identifier string is not a valid URI");
 		sc.add("return null;");
 		sc.add("}");
 		sc.add("}");
 		sc.addLineBreak();
 	}
 
 	private void addGetNameMethod(JavaComposite sc) {
 		sc.add("private String getName(ReferenceType element) {");
 		sc.add(E_STRUCTURAL_FEATURE + " nameAttr = element.eClass().getEStructuralFeature(NAME_FEATURE);");
 		sc.add("if(element.eIsProxy()) {");
 		sc.add("String fragment = ((" + INTERNAL_E_OBJECT + ") element).eProxyURI().fragment();");
 		sc.add("if (fragment != null && fragment.startsWith(" + iContextDependentUriFragmentClassName + ".INTERNAL_URI_FRAGMENT_PREFIX)) {");
 		sc.add("fragment = fragment.substring(" + iContextDependentUriFragmentClassName + ".INTERNAL_URI_FRAGMENT_PREFIX.length());");
 		sc.add("fragment = fragment.substring(fragment.indexOf(\"_\") + 1);");
 		sc.add("}");
 		sc.add("return fragment;");
 		sc.add("}");
 		sc.add("else if (nameAttr instanceof " + E_ATTRIBUTE + ") {");
 		sc.add("return (String) element.eGet(nameAttr);");
 		sc.add("} else {");
 		sc.addComment("try any other string attribute found");
 		sc.add("for (" + E_ATTRIBUTE + " strAttribute : element.eClass().getEAllAttributes()) {");
 		sc.add("if (!strAttribute.isMany() &&");
 		sc.add("strAttribute.getEType().getInstanceClassName().equals(\"String\")) {");
 		sc.add("return (String) element.eGet(strAttribute);");
 		sc.add("}");
 		sc.add("}");
 		sc.add("for (" + E_OPERATION + " o : element.eClass().getEAllOperations()) {");
 		sc.add("if (o.getName().toLowerCase().endsWith(NAME_FEATURE) && o.getEParameters().size() == 0 ) {");
 		sc.add("String result = (String) " + eObjectUtilClassName + ".invokeOperation(element, o);");
 		sc.add("if (result != null) {");
 		sc.add("return result;");
 		sc.add("}");
 		sc.add("}");
 		sc.add("}");
 		sc.add("}");
 		sc.add("return null;");
 		sc.add("}");
 		sc.addLineBreak();
 	}
 
 	private void addMatchesMethod2(StringComposite sc) {
 		sc.add("private String matches(String identifier, Object attributeValue, boolean matchFuzzy) {");
 		sc.add("if (attributeValue != null && attributeValue instanceof String) {");
 		sc.add("String name = (String) attributeValue;");
 		sc.add("if (name.equals(identifier) || matchFuzzy) {");
 		sc.add("return name;");
 		sc.add("}");
 		sc.add("}");
 		sc.add("return null;");
 		sc.add("}");
 		sc.addLineBreak();
 	}
 
 	private void addMatchesMethod1(JavaComposite sc) {
 		sc.add("private String matches(" + E_OBJECT + " element, String identifier, boolean matchFuzzy) {");
 
 		sc.addComment("first check for attributes that have set the ID flag to true");
 		sc.add(LIST + "<" + E_STRUCTURAL_FEATURE + "> features = element.eClass().getEStructuralFeatures();");
 		sc.add("for (" + E_STRUCTURAL_FEATURE + " feature : features) {");
 		sc.add("if (feature instanceof " + E_ATTRIBUTE + ") {");
 		sc.add(E_ATTRIBUTE + " attribute = (" + E_ATTRIBUTE + ") feature;");
 		sc.add("if (attribute.isID()) {");
 		sc.add("Object attributeValue = element.eGet(attribute);");
 		sc.add("String match = matches(identifier, attributeValue, matchFuzzy);");
 		sc.add("if (match != null) {");
 		sc.add("return match;");
 		sc.add("}");
 		sc.add("}");
 		sc.add("}");
 		sc.add("}");
 		sc.addLineBreak();
 		
 		sc.addComment("then check for an attribute that is called 'name'");
 		sc.add(E_STRUCTURAL_FEATURE + " nameAttr = element.eClass().getEStructuralFeature(NAME_FEATURE);");
 		sc.add("if (nameAttr instanceof " + E_ATTRIBUTE + ") {");
 		sc.add("Object attributeValue = element.eGet(nameAttr);");
 		sc.add("return matches(identifier, attributeValue, matchFuzzy);");
 		sc.add("} else {");
 
 		sc.addComment("try any other string attribute found");
 		sc.add("for (" + E_ATTRIBUTE + " stringAttribute : element.eClass().getEAllAttributes()) {");
		sc.add("if (stringAttribute.getEType().getInstanceClassName().equals(\"String\")) {");
 		sc.add("Object attributeValue = element.eGet(stringAttribute);");
 		sc.add("String match = matches(identifier, attributeValue, matchFuzzy);");
 		sc.add("if (match != null) {");
 		sc.add("return match;");
 		sc.add("}");
 		sc.add("}");
 		sc.add("}");
 		sc.addLineBreak();
 		
 		sc.add("for (" + E_OPERATION + " o : element.eClass().getEAllOperations()) {");
 		sc.add("if (o.getName().toLowerCase().endsWith(NAME_FEATURE) && o.getEParameters().size() == 0 ) {");
 		sc.add("String result = (String) " + eObjectUtilClassName + ".invokeOperation(element, o);");
 		sc.add("String match = matches(identifier, result, matchFuzzy);");
 		sc.add("if (match != null) {");
 		sc.add("return match;");
 		sc.add("}");
 		sc.add("}");
 		sc.add("}");
 		sc.add("}");
 		sc.add("return null;");
 		sc.add("}");
 		sc.addLineBreak();
 	}
 
 	private void addDeResolveMethod(StringComposite sc) {
 		sc.add("protected String deResolve(ReferenceType element, ContainerType container, " + E_REFERENCE + " reference) {");
 		sc.add("return getName(element);");
 		sc.add("}");
 		sc.addLineBreak();
 	}
 
 	private void addProduceDeResolveErrorMessage(StringComposite sc) {
 		sc.add("protected String produceDeResolveErrorMessage(" + E_OBJECT + " refObject, " + E_OBJECT + " container, " + E_REFERENCE + " reference, " + iTextResourceClassName + " resource) {");
 		sc.add("String msg = getClass().getSimpleName() + \": \" + reference.getEType().getName() + \" \\\"\" + refObject.toString() + \"\\\" not de-resolveable\";");
 		sc.add("return msg;");
 		sc.add("}");
 		sc.addLineBreak();
 	}
 
 	private void addCastMethod(JavaComposite sc) {
 		sc.addJavadoc(
 			"This method encapsulates an unchecked cast from EObject to " +
 			"ReferenceType. We cannot do this cast strictly type safe, " +
 			"because type parameters are erased by compilation. Thus, an " +
 			"instanceof check cannot be performed at runtime."
 		);
 		sc.add("@SuppressWarnings(\"unchecked\")");
 		sc.addLineBreak();
 		sc.add("private ReferenceType cast(" + E_OBJECT + " element) {");
 		sc.add("return (ReferenceType) element;");
 		sc.add("}");
 		sc.addLineBreak();
 	}
 
 	private void addCheckElementMethod(JavaComposite sc) {
 		sc.add("private boolean checkElement(" + E_OBJECT + " element, " + E_CLASS + " type, String identifier, boolean resolveFuzzy, boolean checkStringWise, " + iReferenceResolveResultClassName + "<ReferenceType> result) {");
 		sc.add("if (element.eIsProxy()) {");
 		sc.add("return true;");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add("boolean hasCorrectType = hasCorrectType(element, type.getInstanceClass());");
 		sc.add("if (!hasCorrectType) {");
 		sc.add("return true;");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add("String match;");
 		sc.addComment("do not compare string-wise if identifier is a URI");
 		sc.add("if (checkStringWise) {");
 		sc.add("match = matches(element, identifier, resolveFuzzy);");
 		sc.add("} else {");
 		sc.add("match = identifier;");
 		sc.add("}");
 		sc.add("if (match == null) {");
 		sc.add("return true;");
 		sc.add("}");
 		sc.addComment(
 			"we can safely cast 'element' to 'ReferenceType' here, " +
 			"because we've checked the type of 'element' against " +
 			"the type of the reference. unfortunately the compiler " +
 			"does not know that this is sufficient, so we must call " +
 			"cast(), which is not type safe by itself."
 		);
 		sc.add("result.addMapping(match, cast(element));");
 		sc.add("if (!resolveFuzzy) {");
 		sc.add("return false;");
 		sc.add("}");
 		sc.add("return true;");
 		sc.add("}");
 		sc.addLineBreak();
 	}
 
 	private void addResolveMethod(JavaComposite sc) {
 		sc.addJavadoc(
 			"This standard implementation searches the tree for objects of the " +
 			"correct type with a name attribute matching the identifier."
 		);
 		sc.add("protected void resolve(String identifier, ContainerType container, " + E_REFERENCE + " reference, int position, boolean resolveFuzzy, " + iReferenceResolveResultClassName + "<ReferenceType> result) {");
 		sc.add("try {");
 		sc.add(E_CLASS + " type = reference.getEReferenceType();");
 		sc.add(E_OBJECT + " root = " + eObjectUtilClassName + ".findRootContainer(container);");
 		sc.addComment("first check whether the root element matches");
 		sc.add("boolean continueSearch = checkElement(root, type, identifier, resolveFuzzy, true, result);");
 		sc.add("if (!continueSearch) {");
 		sc.add("return;");
 		sc.add("}");
 		sc.addComment("then check the contents");
 		sc.add("for (" + ITERATOR + "<" + E_OBJECT + "> iterator = root.eAllContents(); iterator.hasNext(); ) {");
 		sc.add(E_OBJECT + " element = iterator.next();");
 		sc.add("continueSearch = checkElement(element, type, identifier, resolveFuzzy, true, result);");
 		sc.add("if (!continueSearch) {");
 		sc.add("return;");
 		sc.add("}");
 		sc.add("}");
 		sc.add(RESOURCE + " resource = container.eResource();");
 		sc.add("if (resource != null) {");
 		sc.add(URI + " uri = getURI(identifier, resource.getURI());");
 		sc.add("if (uri != null) {");
 		sc.add(E_OBJECT + " element = loadResource(container.eResource().getResourceSet(), uri);");
 		sc.add("if (element == null) {");
 		sc.add("return;");
 		sc.add("}");
 		sc.add("checkElement(element, type, identifier, resolveFuzzy, false, result);");
 		sc.add("}");
 		sc.add("}");
 		sc.add("} catch (" + RUNTIME_EXCEPTION + " rte) {");
 		sc.addComment("catch exception here to prevent EMF proxy resolution from swallowing it");
 		sc.add("rte.printStackTrace();");
 		sc.add("}");
 		sc.add("}");
 		sc.addLineBreak();
 	}
 
 	private void addHasCorrectTypeMethod(StringComposite sc) {
 		sc.add("private boolean hasCorrectType(org.eclipse.emf.ecore.EObject element, Class<?> expectedTypeClass) {");
 		sc.add("return expectedTypeClass.isInstance(element);");
 		sc.add("}");
 		sc.addLineBreak();
 	}
 	
 	private void addFields(StringComposite sc) {
 		sc.add("public final static String NAME_FEATURE = \"name\";");
 		sc.addLineBreak();
 	}
 
 	
 }
