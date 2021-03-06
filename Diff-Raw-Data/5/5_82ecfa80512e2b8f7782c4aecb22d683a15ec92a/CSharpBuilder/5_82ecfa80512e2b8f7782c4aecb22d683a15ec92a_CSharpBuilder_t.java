 /* Copyright (C) 2004 - 2008  db4objects Inc.  http://www.db4o.com
 
 This file is part of the sharpen open source java to c# translator.
 
 sharpen is free software; you can redistribute it and/or modify it under
 the terms of version 2 of the GNU General Public License as published
 by the Free Software Foundation and as clarified by db4objects' GPL 
 interpretation policy, available at
 http://www.db4o.com/about/company/legalpolicies/gplinterpretation/
 Alternatively you can write to db4objects, Inc., 1900 S Norfolk Street,
 Suite 350, San Mateo, CA 94403, USA.
 
 sharpen is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or
 FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 for more details.
 
 You should have received a copy of the GNU General Public License along
 with this program; if not, write to the Free Software Foundation, Inc.,
 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA. */
 
 package sharpen.core;
 
 import java.util.*;
 import java.util.regex.*;
 
 import sharpen.core.Configuration.MemberMapping;
 import sharpen.core.csharp.ast.*;
 import sharpen.core.framework.*;
 import sharpen.core.util.*;
 
 import org.eclipse.jdt.core.*;
 import org.eclipse.jdt.core.dom.*;
 
 public class CSharpBuilder extends ASTVisitor  {	
 	
 	private static final String JAVA_LANG_VOID_TYPE = "java.lang.Void.TYPE";
 	
 	private static final String JAVA_LANG_BOOLEAN_TYPE = "java.lang.Boolean.TYPE";
 	
 	private static final String JAVA_LANG_CHARACTER_TYPE = "java.lang.Character.TYPE";
 	
 	private static final String JAVA_LANG_INTEGER_TYPE = "java.lang.Integer.TYPE";
 	
 	private static final String JAVA_LANG_LONG_TYPE = "java.lang.Long.TYPE";
 	
 	private static final String JAVA_LANG_BYTE_TYPE = "java.lang.Byte.TYPE";
 	
 	private static final String JAVA_LANG_SHORT_TYPE = "java.lang.Short.TYPE";
 	
 	private static final String JAVA_LANG_FLOAT_TYPE = "java.lang.Float.TYPE";
 	
 	private static final String JAVA_LANG_DOUBLE_TYPE = "java.lang.Double.TYPE";
 	
 	private static final CSTypeReference OBJECT_TYPE_REFERENCE = new CSTypeReference("object");
 
 	private final CSCompilationUnit _compilationUnit;
 	
 	private final static List<String> _namespaces = new ArrayList<String>();
 
 	protected CSTypeDeclaration _currentType;
 	
 	protected List<String> _mappedMethodDeclarations = new ArrayList<String>();
 
 	private CSBlock _currentBlock;
 
 	private CSExpression _currentExpression;
 
 	protected CSMethodBase _currentMethod;
 
 	protected BodyDeclaration _currentBodyDeclaration;
 	
 	private static final Pattern SUMMARY_CLOSURE_PATTERN = Pattern.compile("\\.(\\s|$)");
 	
 	private static final Pattern HTML_ANCHOR_PATTERN = Pattern.compile("<([aA])\\s+.+>");
 
 	protected CompilationUnit _ast;
 
 	protected Configuration _configuration;
 
 	private ASTResolver _resolver;
 
 	private IVariableBinding _currentExceptionVariable;
 	
 	private final ContextVariable<Boolean> _ignoreExtends = new ContextVariable<Boolean>(Boolean.FALSE);
 	
 	protected NamingStrategy namingStrategy() {
 		return _configuration.getNamingStrategy();
 	}
 	
 	protected WarningHandler warningHandler() {
 		return _configuration.getWarningHandler();
 	}
 
 	public CSharpBuilder(Configuration configuration) {
 		_compilationUnit = new CSCompilationUnit();
 		_configuration = configuration;
 	}
 	
 	protected CSharpBuilder(CSharpBuilder other) {
 		_ast = other._ast;
 		_compilationUnit = other._compilationUnit;
 		_currentType = other._currentType;
 		_mappedMethodDeclarations = other._mappedMethodDeclarations;
 		_currentBlock = other._currentBlock;
 		_currentExpression = other._currentExpression;
 		_currentMethod = other._currentMethod;
 		_currentBodyDeclaration = other._currentBodyDeclaration;
 		_configuration = other._configuration;
 		_resolver = other._resolver;
 	}
 	
 	public void setSourceCompilationUnit(CompilationUnit ast) {
 		_ast = ast;
 	}
 	
 	public void run() {
 		if (null == warningHandler() || null == _ast) {
 			throw new IllegalStateException();
 		}
 		_ast.accept(this);
 		visit(_ast.getCommentList());
 	}
 	
 	@Override
 	public boolean visit(LineComment node) {
 		_compilationUnit.addComment(
 				new CSLineComment(
 						node.getStartPosition(),
 						getText(node.getStartPosition(), node.getLength())));
 		return false;
 	}
 
 	private String getText(int startPosition, int length) {
 		try {
 			return ((ICompilationUnit)_ast.getJavaElement()).getBuffer().getText(startPosition, length);
 		} catch (JavaModelException e) {
 			throw new RuntimeException(e);
 		}
 	}
 
 	public CSCompilationUnit compilationUnit() {
 		return _compilationUnit;
 	}
 	
 	public boolean visit(ImportDeclaration node) {
 		return false;
 	}
 	
 	public boolean visit(EnumDeclaration node) {
 		if (!isIgnored(node)) {
 			notImplemented(node);
 		}
 		return false;
 	}
 	
 	public boolean visit(LabeledStatement node) {
 		notImplemented(node);
 		return false;
 	}
 	
 	public boolean visit(SuperFieldAccess node) {
 		notImplemented(node);
 		return false;
 	}
 	
 	public boolean visit(MemberRef node) {
 		notImplemented(node);
 		return false;
 	}
 	
 	@Override
 	public boolean visit(WildcardType node) {
 		notImplemented(node);
 		return false;
 	}
 	
 	private void notImplemented(ASTNode node) {
 		throw new IllegalArgumentException(sourceInformation(node) + ": " + node.toString());
 	}
 	
 	public boolean visit(PackageDeclaration node) {
 		String namespace = node.getName().toString();
 		_compilationUnit.namespace(mappedNamespace(namespace));
 		return false;
 	}
 	
 	public boolean visit(AnonymousClassDeclaration node) {
 		CSAnonymousClassBuilder builder = mapAnonymousClass(node);
 		pushExpression(builder.createConstructorInvocation());
 		return false;
 	}
 
 	private CSAnonymousClassBuilder mapAnonymousClass(AnonymousClassDeclaration node) {
 		CSAnonymousClassBuilder builder = new CSAnonymousClassBuilder(this, node);
 		_currentType.addMember(builder.type());
 		return builder;
 	}
 	
 	public boolean visit(final TypeDeclaration node) {
 		if (processEnumType(node)) {
 			return false;
 		}
 		
 		if (processIgnoredType(node)) {
 			return false;
 		}
 		
 		registerMappedMethodNames(node);
 		
 		_ignoreExtends.using(ignoreExtends(node), new Runnable() {
 			public void run() {
 				
 				if (isNonStaticNestedType(node)) {
 					processNonStaticNestedTypeDeclaration(node);
 				} else {
 					processTypeDeclaration(node);
 				}
 				
 			}
 		});
 
 		return false;
 	}
 
 	private boolean processEnumType(TypeDeclaration node) {
 		if (!isEnum(node)) {
 			return false;
 		}
 		final CSEnum theEnum = new CSEnum(typeName(node));
 		mapVisibility(node, theEnum);
 		mapJavadoc(node, theEnum);
 		addType(theEnum);
 		
 		node.accept(new ASTVisitor() {
 			public boolean visit(VariableDeclarationFragment node) {
 				theEnum.addValue(identifier(node.getName()));
 				return false;
 			}
 			
 			@Override
 			public boolean visit(MethodDeclaration node) {
 				if (node.isConstructor() && isPrivate(node)) {
 					return false;
 				}
 				unsupportedConstruct(node, "Enum can contain only fields and a private constructor.");
 				return false;
 			}
 		});
 		return true;
 	} 
 
 	protected boolean isPrivate(MethodDeclaration node) {
 		return Modifier.isPrivate(node.getModifiers());
 	}
 
 	private boolean isEnum(TypeDeclaration node) {
 		return JavadocUtility.containsJavadoc(node, Annotations.SHARPEN_ENUM);
 	}
 
 	private boolean processIgnoredType(TypeDeclaration node) {
 		if (!isIgnored(node)) {
 			return false;
 		}
 		if (isMainType(node)) {
 			compilationUnit().ignore(true);
 		}
 		return true;
 	}
 
 	private void registerMappedMethodNames(TypeDeclaration node) {
 		_mappedMethodDeclarations.clear();
 		for (MethodDeclaration meth : node.getMethods()) {
 			if (isIgnored(meth)) continue;
 			_mappedMethodDeclarations.add(mappedMethodName(meth));
 		}
 	}
 
 	private void processNonStaticNestedTypeDeclaration(TypeDeclaration node) {
 		new NonStaticNestedClassBuilder(this, node);
 	}
 
 	protected CSTypeDeclaration processTypeDeclaration(TypeDeclaration node) {
 		CSTypeDeclaration type = mapTypeDeclaration(node);
 		
 		addType(type);
 		
 		mapSuperTypes(node, type);
 		
 		mapVisibility(node, type);
 		mapDocumentation(node, type);
 		processConversionJavadocTags(node, type);
 		mapMembers(node, type);
 		
 		autoImplementCloneable(node, type);
 		
 		return type;
 	}
 
 	private void autoImplementCloneable(TypeDeclaration node, CSTypeDeclaration type) {
 		
 		if (!implementsCloneable(type)) {
 			return;
 		}
 		
 		CSMethod clone = new CSMethod("System.ICloneable.Clone");
 		clone.returnType(OBJECT_TYPE_REFERENCE);
 		clone.body().addStatement(
 				new CSReturnStatement(
 					-1,
 					new CSMethodInvocationExpression(
 						new CSReferenceExpression("MemberwiseClone"))));
 		
 		type.addMember(clone);
 	}
 
 	private boolean implementsCloneable(CSTypeDeclaration node) {
 		for (CSTypeReferenceExpression typeRef : node.baseTypes()) {
 			if (typeRef.toString().equals("System.ICloneable")) {
 				return true;
 			}
 		}
 		return false;
 	}
 
 	private void mapSuperTypes(TypeDeclaration node, CSTypeDeclaration type) {
 		if (!_ignoreExtends.value()) {
 			mapSuperClass(node, type);
 		}
 		if (!ignoreImplements(node)) {
 			mapSuperInterfaces(node, type);
 		}
 	}
 
 	private boolean ignoreImplements(TypeDeclaration node) {
 		return JavadocUtility.containsJavadoc(node, Annotations.SHARPEN_IGNORE_IMPLEMENTS);
 	}
 
 	private boolean ignoreExtends(TypeDeclaration node) {
 		return JavadocUtility.containsJavadoc(node, Annotations.SHARPEN_IGNORE_EXTENDS);
 	}
 
 	private void processConversionJavadocTags(TypeDeclaration node, CSTypeDeclaration type) {
 		processPartialTagElement(node, type);
 		processExtendsTagElement(node, type);
 	}
 
 	private CSTypeDeclaration mapTypeDeclaration(TypeDeclaration node) {
 		CSTypeDeclaration type = typeDeclarationFor(node);
 		type.startPosition(node.getStartPosition());
 		type.sourceLength(node.getLength());
 		mapTypeParameters(node.typeParameters(), type);
 		return checkForMainType(node, type);
 	}
 
 	private void mapTypeParameters(final List typeParameters, CSTypeParameterProvider type) {
 		for (Object item : typeParameters) {
 			type.addTypeParameter(mapTypeParameter((TypeParameter)item));
 		}
 	}
 
 	private CSTypeParameter mapTypeParameter(TypeParameter item) {
 		return new CSTypeParameter(identifier(item.getName()));
 	}
 
 	private CSTypeDeclaration typeDeclarationFor(TypeDeclaration node) {
 		if (node.isInterface()) {
 			return new CSInterface(processInterfaceName(node));
 		}		
 		final String typeName = typeName(node);
 		if (isStruct(node)) {
 			return new CSStruct(typeName);
 		}
 		return new CSClass(typeName, mapClassModifier(node.getModifiers()));
 	}
 
 	private String typeName(TypeDeclaration node) {
 		final String renamed = renamedName(node);
 		if (renamed != null) return renamed;
 		return node.getName().toString();
 	}
 
 	private boolean isStruct(TypeDeclaration node) {
 		return JavadocUtility.containsJavadoc(node, Annotations.SHARPEN_STRUCT);
 	}
 
 	private CSTypeDeclaration checkForMainType(TypeDeclaration node, CSTypeDeclaration type) {
 		if (isMainType(node)) {
 			setCompilationUnitElementName(type.name());
 		}
 		return type;
 	}
 
 	private void setCompilationUnitElementName(String name) {
 		_compilationUnit.elementName(name + ".cs");
 	}
 
 	private String processInterfaceName(TypeDeclaration node) {
 		String name = node.getName().getFullyQualifiedName();
 		if (!_configuration.nativeInterfaces()) {
 			return name;
 		}
 		return interfaceName(name);
 	}
 
 	private boolean isMainType(TypeDeclaration node) {
 		return
 			node.isPackageMemberTypeDeclaration()
 			&& Modifier.isPublic(node.getModifiers());
 	}
 
 	private void mapSuperClass(TypeDeclaration node, CSTypeDeclaration type) {
 		if (null != node.getSuperclassType()) {
 			final ITypeBinding superClassBinding = node.getSuperclassType().resolveBinding();
 			if (null == superClassBinding) {
 				unresolvedTypeBinding(node.getSuperclassType());
 			} else {
 				type.addBaseType(mappedTypeReference(superClassBinding));
 			}
 		}
 	}
 
 	private void mapSuperInterfaces(TypeDeclaration node, CSTypeDeclaration type) {
 		final ITypeBinding serializable = resolveWellKnownType("java.io.Serializable");
 		for (Object itf : node.superInterfaceTypes()) {
 			Type iface = (Type) itf;
 			if (iface.resolveBinding() == serializable) {
 				continue;
 			}
 			type.addBaseType(mappedTypeReference(iface));
 		}
 		
 		if (!type.isInterface()
 			&& node.resolveBinding().isSubTypeCompatible(serializable)) {
 			type.addAttribute(new CSAttribute("System.Serializable"));
 		}
 	}
 	
 	private ITypeBinding resolveWellKnownType(String typeName) {
 		return _ast.getAST().resolveWellKnownType(typeName);
 	}
 
 	private void mapMembers(TypeDeclaration node, CSTypeDeclaration type) {
 		CSTypeDeclaration saved = _currentType;
 		_currentType = type;
 		visit(node.bodyDeclarations());
 		createInheritedAbstractMemberStubs(node);
 		_currentType = saved;
 	}
 
 	private void mapVisibility(BodyDeclaration node, CSMember member) {
 		member.visibility(mapVisibility(node));
 	}
 
 	private boolean isNonStaticNestedType(TypeDeclaration node) {
 		return isNonStaticNestedType(node.resolveBinding());
 	}
 
 	private boolean isNonStaticNestedType(ITypeBinding binding) {
 		if (binding.isInterface()) return false;
 		if (!binding.isNested()) return false;
 		return !Modifier.isStatic(binding.getModifiers());
 	}
 
 	private void addType(CSType type) {
 		if (null != _currentType) {
 			_currentType.addMember(type);
 		} else {
 			_compilationUnit.addType(type);
 		}
 	}
 
 	private void mapDocumentation(final BodyDeclaration bodyDecl, final CSMember member) {
 		if (processDocumentationOverlay(member)) {
 			return;
 		}
 		
 		mapJavadoc(bodyDecl, member);
 		mapDeclaredExceptions(bodyDecl, member);
 	}
 
 	private void mapDeclaredExceptions(BodyDeclaration bodyDecl, CSMember member) {
 		if (!(bodyDecl instanceof MethodDeclaration)) return;
 		
 		MethodDeclaration method = (MethodDeclaration) bodyDecl;
 		mapThrownExceptions(method.thrownExceptions(), member);
 	}
 
 	private void mapThrownExceptions(List thrownExceptions, CSMember member) {
 		for (Object exception : thrownExceptions) {
 			mapThrownException((Name)exception, member);
 		}
 	}
 
 	private void mapThrownException(Name exception, CSMember member) {
 		final String typeName = mappedTypeName(exception.resolveTypeBinding());
 		if (containsExceptionTagWithCRef(member, typeName)) return;
 		
 		member.addDoc(newTagWithCRef("exception", typeName));
 	}
 
 	private boolean containsExceptionTagWithCRef(CSMember member, String cref) {
 		for (CSDocNode node : member.docs()) {
 			if (!(node instanceof CSDocTagNode)) continue;
 			
 			if (cref.equals(((CSDocTagNode)node).getAttribute("cref"))) {
 				return true;
 			}
 		}
 		return false;
 	}
 
 	private void mapJavadoc(final BodyDeclaration bodyDecl,
 			final CSMember member) {
 		final Javadoc javadoc = bodyDecl.getJavadoc();
 		if (null == javadoc) {
 			return;
 		}
 		mapJavadocTags(javadoc, member);
 	}
 
 	private boolean processDocumentationOverlay(CSMember node) {
 		if (node instanceof CSTypeDeclaration) {
 			return processTypeDocumentationOverlay((CSTypeDeclaration) node);
 		}
 		return processMemberDocumentationOverlay((CSMember) node);
 	}
 
 	private boolean processMemberDocumentationOverlay(CSMember node) {
 		String overlay = documentationOverlay().forMember(currentTypeQName(), node.signature());
 		return processDocumentationOverlay(node, overlay);
 	}
 
 	private String currentTypeQName() {
 		return qualifiedName(_currentType);
 	}
 
 	private boolean processTypeDocumentationOverlay(CSTypeDeclaration node) {
 		String overlay = documentationOverlay().forType(qualifiedName(node));
 		return processDocumentationOverlay(node, overlay);
 	}
 
 	private boolean processDocumentationOverlay(CSMember node, String overlay) {
 		if (null == overlay) {
 			return false;
 		}
 		node.addDoc(new CSDocTextOverlay(overlay.trim()));
 		return true;
 	}
 
 	private DocumentationOverlay documentationOverlay() {
 		return _configuration.documentationOverlay();
 	}
 
 	private String qualifiedName(CSTypeDeclaration node) {
 		if (currentNamespace() == null) {
 			return node.name();
 		}
 		return currentNamespace() + "." + node.name();
 	}
 
 	private String currentNamespace() {
 		return _compilationUnit.namespace();
 	}
 
 	private void mapJavadocTags(final Javadoc javadoc, final CSMember member) {
 		for (Object tag : javadoc.tags()) {
 			try {
 				TagElement element = (TagElement)tag;
 				String tagName = element.getTagName();
 				if (null == tagName) {
 					mapJavadocSummary(member, element);
 				} else {
 					processTagElement(member, element);
 				}
 			} catch (Exception x) {
 				warning((ASTNode)tag, x.getMessage());
 				x.printStackTrace();
 			}
 		}
 	}
 
 	private void processTagElement(final CSMember member, TagElement element) {
 		if (processSemanticallySignificantTagElement(member, element)) {
 			return;
 		}
 		if (!isConversionTag(element.getTagName())) {
 			member.addDoc(mapTagElement(element));
 		}
 	}
 
 	private boolean processSemanticallySignificantTagElement(CSMember member,
 			TagElement element) {
 		if (element.getTagName().equals("@deprecated")) {
 			member.addAttribute(obsoleteAttributeFromDeprecatedTagElement(element));
 			return true;
 		}
 		return false;
 	}
 
 	private CSAttribute obsoleteAttributeFromDeprecatedTagElement(
 			TagElement element) {
 		
 		CSAttribute attribute = new CSAttribute(mappedTypeName("System.ObsoleteAttribute"));
 		if (element.fragments().isEmpty()) {
 			return attribute;
 		}
 		attribute.addArgument(
 				new CSStringLiteralExpression(
 						toLiteralStringForm(getWholeText(element))));
 		return attribute;
 	}
 
 	private String getWholeText(TagElement element) {
 		StringBuilder builder = new StringBuilder();
 		final List fragments = element.fragments();
 		for (Object fragment : fragments) {
 			if (fragment instanceof TextElement) {
 				TextElement textElement = (TextElement)fragment;
 				String text = textElement.getText();
 				builder.append(text);
 				builder.append(" ");
 			} else {
 				break;
 			}
 		}
 		return builder.toString().trim();
 	}
 
 	private String toLiteralStringForm(String s) {
 		// TODO: deal with escaping sequences here
 		return "@\"" + s.replace("\"", "\"\"") + "\"";
 	}
 
 	private boolean isConversionTag(String tagName) {
 		return tagName.startsWith("@sharpen.");
 	}
 
 	private void processPartialTagElement(TypeDeclaration node, CSTypeDeclaration member) {
 		TagElement element = JavadocUtility.getJavadocTag(node, Annotations.SHARPEN_PARTIAL);
 		if (null == element) return;
 		((CSTypeDeclaration)member).partial(true);
 	}
 	private void processExtendsTagElement(TypeDeclaration node, CSTypeDeclaration member) {
 		TagElement element = JavadocUtility.getJavadocTag(node, Annotations.SHARPEN_EXTENDS);
 		if (null == element) return;
 		
 		if (!(member instanceof CSInterface)) {
 			throw new IllegalArgumentException(Annotations.SHARPEN_EXTENDS + " is only implemented for interfaces");
 		}
 		
 		String baseType = getSingleTextFragment(element);
 		((CSInterface)member).addBaseType(new CSTypeReference(baseType));
 	}
 
 	private String getSingleTextFragment(TagElement element) {
 		List fragments = element.fragments();
 		if (fragments.size() != 1 || !isTextFragment(fragments, 0)) {
 			throw new IllegalArgumentException(sourceInformation(element) + ": expecting a single textual argument");
 		}
 		return textFragment(fragments, 0);
 	}
 
 	private boolean isTextFragment(List fragments, int index) {
 		return (fragments.get(index) instanceof TextElement);
 	}
 
 	public static String textFragment(List fragments, final int index) {
 		return JavadocUtility.textFragment(fragments, index);
 	}
 
 	private void mapJavadocSummary(final CSMember member, TagElement element) {
 		List<String> summary = getFirstSentence(element);
 		if (null != summary) {
 			CSDocTagNode summaryNode = new CSDocTagNode("summary");
 			for (String fragment : summary) {
 				summaryNode.addFragment(new CSDocTextNode(fragment));
 			}
 			member.addDoc(summaryNode);
 			member.addDoc(createTagNode("remarks", element));
 		} else {
 			member.addDoc(createTagNode("summary", element));
 		}
 	}
 
 	private List<String> getFirstSentence(TagElement element) {
 		List<String> fragments = new LinkedList<String>();
 		for (Object fragment : element.fragments()) {
 			if (fragment instanceof TextElement) {
 				TextElement textElement = (TextElement)fragment;
 				String text = textElement.getText();
 				int index = findSentenceClosure(text);
 				if (index > -1) {
 					fragments.add(text.substring(0, index+1));
 					return fragments;
 				} else {
 					fragments.add(text);
 				}
 			} else {
 				break;
 			}
 		}
 		return null;
 	}
 	
 	private int findSentenceClosure(String text) {
 		Matcher matcher = SUMMARY_CLOSURE_PATTERN.matcher(text);
 		return matcher.find() ? matcher.start() : -1;
 	}
 
 	private CSDocNode mapTagElement(TagElement element) {
 		String tagName = element.getTagName();
 		if (TagElement.TAG_PARAM.equals(tagName)) {
 			return mapTagParam(element); 
 		} else if (TagElement.TAG_RETURN.equals(tagName)) {
 			return createTagNode("returns", element);
 		} else if (TagElement.TAG_LINK.equals(tagName)) {
 			return mapTagLink(element);
 		} else if (TagElement.TAG_THROWS.equals(tagName)) {
 			return mapTagThrows(element);
 		} else if (TagElement.TAG_SEE.equals(tagName)) {
 			return mapTagWithCRef("seealso", element);
 		}
 		return createTagNode(tagName.substring(1), element);
 	}
 	
 	private CSDocNode mapTagThrows(TagElement element) {
 		return mapTagWithCRef("exception", element);
 	}
 	
 	private CSDocNode mapTagLink(TagElement element) {
 		return mapTagWithCRef("see", element);
 	}
 	
 	private CSDocNode mapTagWithCRef(String tagName, TagElement element) {
 		List fragments = element.fragments();
 		final ASTNode linkTarget = (ASTNode)fragments.get(0);
 		String cref = mapCRefTarget(linkTarget);
 		if (null == cref) {
 			warning(linkTarget, "Tag '" + element.getTagName() + "' demands a valid cref target.");
 			return createTagNode(tagName, element);
 		}
 		
 		CSDocTagNode node = newTagWithCRef(tagName, cref);
 		if (fragments.size() > 1) {
 			if (isLinkWithSimpleLabel(fragments, linkTarget)) {
 				node.addTextFragment(unqualifiedName(cref));
 			} else {
 				collectFragments(node, fragments, 1);
 			}
 		} else {
 			node.addTextFragment(cref);
 		}
 		return node;
 	}
 
 	private CSDocTagNode newTagWithCRef(String tagName, String cref) {
 		CSDocTagNode node = new CSDocTagNode(tagName);
 		node.addAttribute("cref", cref);
 		return node;
 	}
 
 	private boolean isLinkWithSimpleLabel(List fragments,
 			final ASTNode linkTarget) {
 		if (fragments.size() != 2) return false;
 		if (!isTextFragment(fragments, 1)) return false;
 		final String link = linkTarget.toString();
 		final String label = JavadocUtility.textFragment(fragments, 1);
 		return label.equals(link) || label.equals(unqualifiedName(link));
 	}
 
 	private String mapCRefTarget(final ASTNode crefTarget) {
 		switch (crefTarget.getNodeType()) {
 		case ASTNode.SIMPLE_NAME:
 		case ASTNode.QUALIFIED_NAME:
 			return mapCRefTarget(crefTarget, ((Name)crefTarget).resolveBinding());
 		case ASTNode.MEMBER_REF:
 			return mapCRefTarget(crefTarget, ((MemberRef)crefTarget).resolveBinding());
 		case ASTNode.METHOD_REF:
 			return mapCRefTarget(crefTarget, ((MethodRef)crefTarget).resolveBinding());
 		}
 		return null;
 	}
 
 	private String mapCRefTarget(ASTNode node, IBinding binding) {
 		if (null == binding) {	
 			warning(node, "Unresolved cref target");
 			return node.toString();
 		}
 		return mappedQualifiedName(binding);
 	}
 	
 	private CSDocNode mapTagParam(TagElement element) {
 		List fragments = element.fragments();
 		SimpleName name = (SimpleName) fragments.get(0);
 		if (null == name.resolveBinding()) {
 			warning(name, "Parameter '" + name + "' not found.");
 		}
 		CSDocTagNode param = new CSDocTagNode("param");
 		param.addAttribute("name", identifier(name));
 		collectFragments(param, fragments, 1);
 		return param;
 	}
 
 	private void collectFragments(CSDocTagNode node, List fragments, int index) {
 		for (int i=index; i<fragments.size(); ++i) {
 			node.addFragment(mapTagElementFragment((ASTNode)fragments.get(i)));
 		}
 	}
 
 	private CSDocNode mapTextElement(TextElement element) {
 		final String text = element.getText();
 		if (HTML_ANCHOR_PATTERN.matcher(text).find()) {
 			warning(element, "Caution: HTML anchors can result in broken links. Consider using @link instead.");
 		}
 		return new CSDocTextNode(text);
 	}
 
 	private CSDocNode createTagNode(String tagName, TagElement element) {
 		CSDocTagNode summary = new CSDocTagNode(tagName);
 		for (Object f : element.fragments()) {
 			summary.addFragment(mapTagElementFragment((ASTNode)f));
 		}
 		return summary;
 	}
 
 	private CSDocNode mapTagElementFragment(ASTNode node) {
 		switch (node.getNodeType()) {
 		case ASTNode.TAG_ELEMENT:
 			return mapTagElement((TagElement)node);
 		case ASTNode.TEXT_ELEMENT:
 			return mapTextElement((TextElement)node);
 		}
 		warning(node, "Documentation node not supported: " + node.getClass() + ": " + node);
 		return new CSDocTextNode(node.toString());
 	}
 
 	public boolean visit(FieldDeclaration node) {
 		
 		if (isIgnored(node)) {
 			return false;
 		}
 		
 		CSTypeReferenceExpression typeName = mappedTypeReference(node.getType());
 		CSVisibility visibility = mapVisibility(node);
 
 		for (Object item : node.fragments()) {
 			VariableDeclarationFragment fragment = (VariableDeclarationFragment) item;
 			CSField field = new CSField(fieldName(fragment), typeName, visibility, mapFieldInitializer(fragment));
 			if (isConstField(node, fragment)) {
 				field.addModifier(CSFieldModifier.Const);
 			} else {
 				processFieldModifiers(field, node.getModifiers());
 			}
 			mapDocumentation(node, field);
 			_currentType.addMember(field);
 		}
 
 		return false;
 	}
 
 	protected String fieldName(VariableDeclarationFragment fragment) {
 		return identifier(fragment.getName());
 	}
 
 	protected CSExpression mapFieldInitializer(VariableDeclarationFragment fragment) {
 		return mapExpression(fragment.getInitializer());
 	}
 	
     private boolean isConstField(FieldDeclaration node, VariableDeclarationFragment fragment) {
 		return Modifier.isFinal(node.getModifiers()) && node.getType().isPrimitiveType() && hasConstValue(fragment);
 	}
 
 	private boolean hasConstValue(VariableDeclarationFragment fragment) {
 		return null != fragment.resolveBinding().getConstantValue();
 	}
 
 	private void processFieldModifiers(CSField field, int modifiers) {
 		if (Modifier.isStatic(modifiers)) {
 			field.addModifier(CSFieldModifier.Static);
 		}
 		if (Modifier.isFinal(modifiers)) {
 			field.addModifier(CSFieldModifier.Readonly);
 		}
 		if (Modifier.isTransient(modifiers)) {
 			field.addAttribute(new CSAttribute(mappedTypeName("System.NonSerialized")));
 		}
 		if (Modifier.isVolatile(modifiers)) {
 			field.addModifier(CSFieldModifier.Volatile);
 		}
 		
 	}
 
 	private boolean isDestructor(MethodDeclaration node) {
 		return node.getName().toString().equals("finalize");
 	}
 	
 	public boolean visit(Initializer node) {
 		CSConstructor ctor = new CSConstructor(CSConstructorModifier.Static);
 		_currentType.addMember(ctor);
 		visitBodyDeclarationBlock(node, node.getBody(), ctor);
 		return false;
 	}
 
 	public boolean visit(MethodDeclaration node) {
 		if (isIgnored(node) || isRemoved(node)) {
 			return false;
 		}
 		
 		if (isEvent(node)) {
 			processEventDeclaration(node);
 			return false;
 		}
 		
 		if (isProperty(node)) {
 			processPropertyDeclaration(node);
 			return false;
 		}
 		
 		if (isIndexer(node)) {
 			processIndexerDeclaration(node);
 			return false;
 		}
 		
 		processMethodDeclaration(node);
 		
 		return false;
 	}
 	
 	private void processIndexerDeclaration(MethodDeclaration node) {
 		processPropertyDeclaration(node, CSProperty.INDEXER);
 	}
 
 	private boolean isIndexer(MethodDeclaration node) {
 		return isTaggedDeclaration(node, Annotations.SHARPEN_INDEXER);
 	}
 
 	private boolean isRemoved(MethodDeclaration node) {
 		return JavadocUtility.containsJavadoc(node, Annotations.SHARPEN_REMOVE)
 			|| isRemoved(node.resolveBinding());
 	}
 
 	private boolean isRemoved(final IMethodBinding binding) {
 		return _configuration.isRemoved(qualifiedName(binding));
 	}
 
 	private boolean isIgnored(BodyDeclaration node) {
 		return JavadocUtility.containsJavadoc(node, Annotations.SHARPEN_IGNORE);
 	}
 
 	public static boolean containsJavadoc(BodyDeclaration node, final String tag) {
 		return JavadocUtility.containsJavadoc(node, tag);
 	}
 
 	private void processPropertyDeclaration(MethodDeclaration node) {
 		processPropertyDeclaration(node, propertyName(node));
 	}
 
 	private void processPropertyDeclaration(MethodDeclaration node,
 			final String name) {
 		_currentType.addMember(mapPropertyDeclaration(node, name));
 	}
 
 	private CSProperty mapPropertyDeclaration(MethodDeclaration node,
 			final String propName) {
 		final boolean isGetter = isGetter(node);
 		final CSTypeReferenceExpression propertyType = isGetter
 			? mappedReturnType(node)
 			: mappedTypeReference(lastParameter(node).getType());
 			
 		final CSBlock block = mapBody(node);
 			
 		final CSProperty property = new CSProperty(propName, propertyType);		
 		if (isGetter) {
 			property.getter(block);
 		} else {
 			property.setter(block);
 		}
 		mapMetaMemberAttributes(node, property);
 		mapParameters(node, property);
 		return property;
 	}
 
 	private CSBlock mapBody(MethodDeclaration node) {
 		final CSBlock block = new CSBlock();
 		processBlock(node, node.getBody(), block);
 		return block;
 	}
 
 	private boolean isGetter(MethodDeclaration node) {
 		return !"void".equals(node.getReturnType2().toString());
 	}
 
 	private SingleVariableDeclaration lastParameter(MethodDeclaration node) {
 		return parameter(node, node.parameters().size()-1);
 	}
 	
 	private String propertyName(MethodDeclaration node) {
 		return methodName(node);
 	}
 
 	private boolean isProperty(MethodDeclaration node) {
 		return isTaggedDeclaration(node, Annotations.SHARPEN_PROPERTY);
 	}
 
 	private boolean isTaggedDeclaration(MethodDeclaration node, final String tag) {
 		if (null != JavadocUtility.getJavadocTag(node, tag)) return true;
 		
 		MethodDeclaration originalMethod = findOriginalMethodDeclaration(node);
 		if (null == originalMethod) return false;
 		
 		return null != JavadocUtility.getJavadocTag(originalMethod, tag);
 	}
 
 	private void processMethodDeclaration(MethodDeclaration node) {
 		if (isDestructor(node)) {
 			mapMethodParts(node, new CSDestructor());
 			return;
 		} 
 		
 		if (node.isConstructor()) {
 			mapMethodParts(node, new CSConstructor());
 			return;
 		}
 		
 		CSMethod method = new CSMethod(mappedMethodDeclarationName(node));
 		method.returnType(mappedReturnType(node));
 		method.modifier(mapMethodModifier(node));
 		mapTypeParameters(node.typeParameters(), method);
 		mapMethodParts(node, method);
 	}
 
 	private void mapMethodParts(MethodDeclaration node, CSMethodBase method) {
 		
 		_currentType.addMember(method);
 		
 		method.startPosition(node.getStartPosition());
 		method.isVarArgs(node.isVarargs());
 		mapVisibility(node, method);
 		mapParameters(node, method);
 		mapDocumentation(node, method);
 		visitBodyDeclarationBlock(node, node.getBody(), method);
 	}
 
 	private String mappedMethodDeclarationName(MethodDeclaration node) {
 		final String mappedName = mappedMethodName(node);
 		if (null == mappedName || 0 == mappedName.length()) {
 			return methodName(node.getName().toString());
 		}
 		return mappedName;
 	}
 
 	private void mapParameters(MethodDeclaration node, CSParameterized method) {
 		if (method instanceof CSMethod) {
 			mapMethodParameters(node, (CSMethod)method);
 			return;
 		}
 		for (Object p : node.parameters()) {
 			mapParameter((SingleVariableDeclaration) p, method);
 		}
 	}
 
 	private void mapParameter(SingleVariableDeclaration parameter, CSParameterized method) {
 		method.addParameter(createParameter(parameter));
 	}
 
 	private void mapMethodParameters(MethodDeclaration node, CSMethod method) {
 		for (Object o : node.parameters()) {
 			SingleVariableDeclaration p = (SingleVariableDeclaration)o;
 			ITypeBinding parameterType = p.getType().resolveBinding();
 			if (isGenericRuntimeParameterIdiom(node.resolveBinding(), parameterType)) {
 				
 				// System.Type <p.name> = typeof(<T>);
 				method.body().addStatement(
 					new CSDeclarationStatement(
 						p.getStartPosition(),
 						new CSVariableDeclaration(
 							identifier(p.getName()),
 							new CSTypeReference("System.Type"),
 							new CSTypeofExpression(genericRuntimeTypeIdiomType(parameterType)))));
 			
 			} else {
 			
 				mapParameter(p, method);
 			}
 		}
 	}
 
 	private CSTypeReferenceExpression genericRuntimeTypeIdiomType(
 			ITypeBinding parameterType) {
 		return mappedTypeReference(parameterType.getTypeArguments()[0]);
 	}
 
 	private boolean isGenericRuntimeParameterIdiom(IMethodBinding method,
 			ITypeBinding parameterType) {
 		return parameterType.isParameterizedType()
 			&& "java.lang.Class".equals(qualifiedName(parameterType))
 			&& parameterType.getTypeArguments()[0].getDeclaringMethod() == method;
 	}
 
 	private CSTypeReferenceExpression mappedReturnType(MethodDeclaration node) {
 		return mappedTypeReference(node.getReturnType2());
 	}
 
 	private void processEventDeclaration(MethodDeclaration node) {
 		CSTypeReference eventHandlerType = new CSTypeReference(getEventHandlerTypeName(node));
 		CSEvent event = createEventFromMethod(node, eventHandlerType);
 		mapMetaMemberAttributes(node, event);
 		if (_currentType.isInterface()) return;
 		
 		VariableDeclarationFragment field = getEventBackingField(node);
 		CSField backingField = (CSField)_currentType.getMember(field.getName().toString());
 		backingField.type(eventHandlerType);
 		
 		// clean field
 		backingField.initializer(null);
 		backingField.removeModifier(CSFieldModifier.Readonly);
 		
 		final CSBlock addBlock = createEventBlock(backingField, "System.Delegate.Combine");
 		String onAddMethod = getEventOnAddMethod(node);
 		if (onAddMethod != null) {
 			addBlock.addStatement(
 					new CSMethodInvocationExpression(
 							new CSReferenceExpression(onAddMethod)));
 		}
 		event.setAddBlock(addBlock);
 		event.setRemoveBlock(createEventBlock(backingField, "System.Delegate.Remove"));
 	}
 
 	private String getEventOnAddMethod(MethodDeclaration node) {
 		final TagElement onAddTag = JavadocUtility.getJavadocTag(node, Annotations.SHARPEN_EVENT_ON_ADD);
 		if (null == onAddTag) return null;
 		return methodName(getSingleTextFragment(onAddTag));
 	}
 
 	private String getEventHandlerTypeName(MethodDeclaration node) {
 		final String eventArgsType = getEventArgsType(node);
 		return buildEventHandlerTypeName(node, eventArgsType);
 	}
 
 	private void mapMetaMemberAttributes(MethodDeclaration node, CSMetaMember metaMember) {
 		mapVisibility(node, metaMember);
 		metaMember.modifier(mapMethodModifier(node));
 		mapDocumentation(node, metaMember);
 	}	
 	
 	private CSBlock createEventBlock(CSField backingField, String delegateMethod) {
 		CSBlock block = new CSBlock();
 		block.addStatement(
 				new CSInfixExpression(
 						"=",
 						new CSReferenceExpression(backingField.name()),
 						new CSCastExpression(
 								backingField.type(),
 								new CSMethodInvocationExpression(
 										new CSReferenceExpression(delegateMethod),
 										new CSReferenceExpression(backingField.name()),
 										new CSReferenceExpression("value")))));
 		return block;
 	}
 	
 	private static final class CheckVariableUseVisitor extends ASTVisitor {
 
 		private final IVariableBinding _var;
 		private boolean _used;
 
 		private CheckVariableUseVisitor(IVariableBinding var) {
 			this._var = var;
 		}
 
 		@Override
 		public boolean visit(SimpleName name) {
 			if (name.resolveBinding().equals(_var)) {
 				_used = true;
 			}
 				
 			return false;
 		}
 		
 		public boolean used() {
 			return _used;
 		}
 	}
 
 	private static final class FieldAccessFinder extends ASTVisitor {
 		public IBinding field;
 
 		@Override
 		public boolean visit(SimpleName node) {
 			field = node.resolveBinding();
 			return false;
 		}
 	}
 	
 	private VariableDeclarationFragment getEventBackingField(MethodDeclaration node) {
 		FieldAccessFinder finder = new FieldAccessFinder();
 		node.accept(finder);
 		return (VariableDeclarationFragment)findDeclaringNode(finder.field);
 	}
 
 	private CSEvent createEventFromMethod(MethodDeclaration node, CSTypeReference eventHandlerType) {
 		String eventName = methodName(node);
 		CSEvent event = new CSEvent(eventName, eventHandlerType);
 		_currentType.addMember(event);
 		createEventTypeIfNeeded(eventHandlerType);
 		return event;
 	}
 
 	private String methodName(MethodDeclaration node) {
 		return methodName(node.getName().toString());
 	}
 
 	private void createEventTypeIfNeeded(CSTypeReference eventHandlerType) {
 		// the delegate is only generated along with the interface
 		if (!_currentType.isInterface()) return;
 		
 		final String unqualifiedName = unqualifiedName(eventHandlerType.typeName());
 		for (CSType type : _compilationUnit.types()) {
 			if (type.name().equals(unqualifiedName)) return;
 		}
 		
 		CSDelegate delegate = new CSDelegate(unqualifiedName);
 		delegate.visibility(CSVisibility.Public);
 		delegate.addParameter("sender", OBJECT_TYPE_REFERENCE);
 		delegate.addParameter("args", new CSTypeReference(buildEventArgsTypeName(eventHandlerType)));
 		_compilationUnit.insertTypeBefore(delegate, _currentType);
 	}
 	
 	private String unqualifiedName(String typeName) {
 		int index = typeName.lastIndexOf('.');
 		if (index < 0) return typeName;
 		return typeName.substring(index + 1);
 	}
 
 	private String buildEventHandlerTypeName(ASTNode node, String typeName) {
 		if (!typeName.endsWith("EventArgs")) {
 			warning(node, Annotations.SHARPEN_EVENT + " type name must end with 'EventArgs'");
 			return typeName + "EventHandler";
 		}
 		
 		return buildEventTypeName(typeName, "EventArgs", "EventHandler");
 	}
 
 	private String buildEventArgsTypeName(CSTypeReference typeName) {
 		return buildEventTypeName(typeName.typeName(), "EventHandler", "EventArgs");
 	}
 	
 	private String buildEventTypeName(String typeName, String expected, String suffix) {
 		String prefix = typeName.substring(0, typeName.length() - expected.length());
 		return prefix + suffix;
 	}
 
 	private String getEventArgsType(MethodDeclaration node) {
 		TagElement tag = findEventTag(node);
 		if (null == tag) return null;
 		return mappedTypeName(getSingleTextFragment(tag));
 	}
 
 	private TagElement findEventTag(MethodDeclaration node) {
 		TagElement eventTag = getEventTag(node);
 		if (null != eventTag) return eventTag;
 		
 		MethodDeclaration originalMethod = findOriginalMethodDeclaration(node);
 		if (null == originalMethod) return null;
 
 		return getEventTag(originalMethod);
 	}
 
 	private MethodDeclaration findOriginalMethodDeclaration(MethodDeclaration node) {
 		return findOriginalMethodDeclaration(node.resolveBinding());
 	}
 	
 	private MethodDeclaration findOriginalMethodDeclaration(IMethodBinding binding) {
 		IMethodBinding definition = Bindings.findMethodDefininition(binding, _ast.getAST());
 		if (null == definition) return null;
 		return (MethodDeclaration)findDeclaringNode(definition);
 	}
 
 	private ASTNode findDeclaringNode(IBinding binding) {
 		final ASTNode declaringNode = _ast.findDeclaringNode(binding);
 		if (null != declaringNode) return declaringNode;
 		if (null == _resolver) return null;
 		return _resolver.findDeclaringNode(binding);
 	}
 
 	private TagElement getEventTag(MethodDeclaration node) {
 		return JavadocUtility.getJavadocTag(node, Annotations.SHARPEN_EVENT);
 	}
 
 	private void visitBodyDeclarationBlock(BodyDeclaration node, Block block, CSMethodBase method) {
 		CSMethodBase saved = _currentMethod;
 		_currentMethod = method;
 		
 		processDisableTags(node, method);
 		processBlock(node, block, method.body());
 		
 		_currentMethod = saved;
 	}
 
 	private void processDisableTags(BodyDeclaration node, CSMethodBase method) {
 		TagElement tag = JavadocUtility.getJavadocTag(node, Annotations.SHARPEN_IF);
 		if (null == tag) return;
 		
 		method.addEnclosingIfDef(getSingleTextFragment(tag));
 	}
 
 	private void processBlock(BodyDeclaration node, Block block, final CSBlock targetBlock) {
 		BodyDeclaration savedDeclaration = _currentBodyDeclaration;		
 		_currentBodyDeclaration = node;
 		
 		if (Modifier.isSynchronized(node.getModifiers())) {
 			CSLockStatement lock = new CSLockStatement(node.getStartPosition(), getLockTarget(node));
 			targetBlock.addStatement(lock);			
 			visitBlock(lock.body(), block);
 		} else {
 			visitBlock(targetBlock, block);
 		}
 		_currentBodyDeclaration = savedDeclaration;
 	}
 
 	private CSExpression getLockTarget(BodyDeclaration node) {
 		return Modifier.isStatic(node.getModifiers())
 			? new CSTypeofExpression(new CSTypeReference(_currentType.name()))
 			: new CSThisExpression();
 	}
 	
 	public boolean visit(ConstructorInvocation node) {
 		addChainedConstructorInvocation(new CSThisExpression(), node.arguments());
 		return false;
 	}
 
 	private void addChainedConstructorInvocation(CSExpression target, List arguments) {
 		CSConstructorInvocationExpression cie = new CSConstructorInvocationExpression(target);
 		mapArguments(cie, arguments);
 		((CSConstructor)_currentMethod).chainedConstructorInvocation(cie);
 	}
 	
 	public boolean visit(SuperConstructorInvocation node) {
 		if (null != node.getExpression()) {
 			notImplemented(node);
 		}
 		addChainedConstructorInvocation(new CSBaseExpression(), node.arguments());
 		return false;
 	}
 	
 	private <T extends ASTNode> void visitBlock(CSBlock block, T node) {
 		if (null == node) {
 			return;
 		}
 		
 		CSBlock saved = _currentBlock;
 		_currentBlock = block;		
 		node.accept(this);
 		_currentBlock = saved;
 	}
 	
 	public boolean visit(VariableDeclarationExpression node) {
 		pushExpression(new CSDeclarationExpression(createVariableDeclaration((VariableDeclarationFragment) node.fragments().get(0))));
 		return false;
 	}
 	
 	public boolean visit(VariableDeclarationStatement node) {
 		for (Object f : node.fragments()) {
 			VariableDeclarationFragment variable = (VariableDeclarationFragment)f;
 			addStatement(new CSDeclarationStatement(node.getStartPosition(), createVariableDeclaration(variable)));
 		}
 		return false;
 	}
 
 	private CSVariableDeclaration createVariableDeclaration(VariableDeclarationFragment variable) {
 		IVariableBinding binding = variable.resolveBinding();
 		return createVariableDeclaration(binding, mapExpression(variable.getInitializer()));
 	}
 
 	private CSVariableDeclaration createVariableDeclaration(IVariableBinding binding, CSExpression initializer) {
 		return new CSVariableDeclaration(identifier(binding.getName()), mappedTypeReference(binding.getType()), initializer);
 	}
 	
 	public boolean visit(ExpressionStatement node) {
 		if (isRemovedMethodInvocation(node.getExpression())) {
 			return false;
 		}
 		
 		addStatement(new CSExpressionStatement(node.getStartPosition(), mapExpression(node.getExpression())));
 		return false;
 	}
 	
 	private boolean isRemovedMethodInvocation(Expression expression) {
 		if (!(expression instanceof MethodInvocation)) {
 			return false;
 		}
 		
 		MethodInvocation invocation = (MethodInvocation)expression;
 		return isTaggedMethodInvocation(invocation, Annotations.SHARPEN_REMOVE)
 			|| isRemoved(invocation.resolveMethodBinding());
 		
 	}
 
 	public boolean visit(IfStatement node) {
 		Expression expression = node.getExpression();
 		
 		Object constValue = constValue(expression);
 		if (null != constValue) {
 			// dead branch elimination
 			if (isTrue(constValue)) {
 				node.getThenStatement().accept(this);
 			} else {
 				if (null != node.getElseStatement()) {
 					node.getElseStatement().accept(this);
 				}
 			}
 		} else {
 			CSIfStatement stmt = new CSIfStatement(node.getStartPosition(), mapExpression(expression));
 			visitBlock(stmt.trueBlock(), node.getThenStatement());
 			visitBlock(stmt.falseBlock(), node.getElseStatement());
 			addStatement(stmt);
 		}
 		return false;
 	}
 	
 	private boolean isTrue(Object constValue) {
 		return ((Boolean)constValue).booleanValue();
 	}
 
 	private Object constValue(Expression expression) {
 		switch (expression.getNodeType()) {
 		case ASTNode.PREFIX_EXPRESSION:
 			return constValue((PrefixExpression)expression);
 		case ASTNode.SIMPLE_NAME:
 		case ASTNode.QUALIFIED_NAME:
 			return constValue((Name)expression);
 		}
 		return null;
 	}
 	
 	public Object constValue(PrefixExpression expression) {
 		if (PrefixExpression.Operator.NOT == expression.getOperator()) {
 			Object value = constValue(expression.getOperand());
 			if (null != value) {
 				return isTrue(value) ? Boolean.FALSE : Boolean.TRUE; 
 			}
 		}
 		return null;
 	}
 	
 	public Object constValue(Name expression) {
 		IBinding binding = expression.resolveBinding();
 		if (IBinding.VARIABLE == binding.getKind()) {
 			return ((IVariableBinding)binding).getConstantValue();
 		}
 		return null;
 	}
 
 	public boolean visit(WhileStatement node) {
 		CSWhileStatement stmt = new CSWhileStatement(node.getStartPosition(), mapExpression(node.getExpression()));
 		visitBlock(stmt.body(), node.getBody());
 		addStatement(stmt);
 		return false;
 	}
 	
 	public boolean visit(DoStatement node) {
 		CSDoStatement stmt = new CSDoStatement(node.getStartPosition(), mapExpression(node.getExpression()));
 		visitBlock(stmt.body(), node.getBody());
 		addStatement(stmt);
 		return false;
 	}
 	
 	public boolean visit(TryStatement node) {
 		CSTryStatement stmt = new CSTryStatement(node.getStartPosition());
 		visitBlock(stmt.body(), node.getBody());
 		for (Object o : node.catchClauses()) {
 			CatchClause clause = (CatchClause)o;
 			if (!isIgnoredExceptionType(clause.getException().getType().resolveBinding())) {
 				stmt.addCatchClause(mapCatchClause(clause));
 			}
 		}
 		if (null != node.getFinally()) {
 			CSBlock finallyBlock = new CSBlock();
 			visitBlock(finallyBlock, node.getFinally());
 			stmt.finallyBlock(finallyBlock);
 		}
 		
 		if (null != stmt.finallyBlock()
 			|| !stmt.catchClauses().isEmpty()) {
 			
 			addStatement(stmt);
 		} else {
 
 			_currentBlock.addAll(stmt.body());
 		}
 		return false;
 	}
 	
 	private boolean isIgnoredExceptionType(ITypeBinding exceptionType) {
 		return qualifiedName(exceptionType).equals("java.lang.CloneNotSupportedException");
 	}
 
 	private CSCatchClause mapCatchClause(CatchClause node) {
 		IVariableBinding oldExceptionVariable = _currentExceptionVariable;
 		_currentExceptionVariable = node.getException().resolveBinding();
 		try {
 			CheckVariableUseVisitor check = new CheckVariableUseVisitor(_currentExceptionVariable);
 			node.getBody().accept(check);
 			
 			CSCatchClause clause;
 			if (isEmptyCatch(node, check)) {
 				clause = new CSCatchClause();
 			} else {
 				clause = new CSCatchClause(createVariableDeclaration(_currentExceptionVariable, null));
 			}
 			clause.anonymous(!check.used());
 			visitBlock(clause.body(), node.getBody());
 			return clause;
 		} finally {
 			_currentExceptionVariable = oldExceptionVariable;
 		}
 	}
 
 	private boolean isEmptyCatch(CatchClause clause, CheckVariableUseVisitor check) {
 		if (check.used()) return false;
 		return isThrowable(clause.getException().resolveBinding().getType());
 	}
 
 	private boolean isThrowable(ITypeBinding declaringClass) {
 		return "java.lang.Throwable".equals(qualifiedName(declaringClass));
 	}
 
 	public boolean visit(ThrowStatement node) {
 		addStatement(mapThrowStatement(node));
 		return false;
 	}
 
 	private CSThrowStatement mapThrowStatement(ThrowStatement node) {
 		Expression exception = node.getExpression();
 		if (isCurrentExceptionVariable(exception)) {
 			return new CSThrowStatement(node.getStartPosition(), null);
 		} 
 		return new CSThrowStatement(node.getStartPosition(), mapExpression(exception));
 	}
 	
 	private boolean isCurrentExceptionVariable(Expression exception) {
 		if (!(exception instanceof SimpleName)) {
 			return false;
 		}
 		return ((SimpleName)exception).resolveBinding() == _currentExceptionVariable;
 	}
 
 	public boolean visit(BreakStatement node) {
 		if (null != node.getLabel()) {
 			notImplemented(node.getLabel());
 		}
 		addStatement(new CSBreakStatement(node.getStartPosition()));
 		return false;
 	}
 	
 	public boolean visit(ContinueStatement node) {
 		if (null != node.getLabel()) {
 			notImplemented(node.getLabel());
 		}
 		addStatement(new CSContinueStatement(node.getStartPosition()));
 		return false;
 	}
 	
 	public boolean visit(SynchronizedStatement node) {
 		CSLockStatement stmt = new CSLockStatement(node.getStartPosition(), mapExpression(node.getExpression()));
 		visitBlock(stmt.body(), node.getBody());
 		addStatement(stmt);
 		return false;
 	}
 	
 	public boolean visit(ReturnStatement node) {
 		addStatement(new CSReturnStatement(node.getStartPosition(), mapExpression(node.getExpression())));
 		return false;
 	}
 	
 	public boolean visit(NumberLiteral node) {
 		
 		String token = node.getToken();
 		CSNumberLiteralExpression literal = new CSNumberLiteralExpression(token);
 		
 		if (token.startsWith("0x")) {
 			pushExpression(uncheckedCast("int", literal));
 		} else {
 			pushExpression(literal);
 		}
 		
 		return false;
 	}
 
 	private CSUncheckedExpression uncheckedCast(String type, CSExpression expression) {
 		return new CSUncheckedExpression(new CSCastExpression(new CSTypeReference(type), new CSParenthesizedExpression(expression)));
 	}
 	
 	public boolean visit(StringLiteral node) {
 		String value = node.getLiteralValue();
 		if (value != null && value.length() == 0) {
 			pushExpression(new CSReferenceExpression("string.Empty"));
 		} else {
 			pushExpression(new CSStringLiteralExpression(node.getEscapedValue()));
 		}
 		return false;
 	}
 	
 	public boolean visit(CharacterLiteral node) {
 		pushExpression(new CSCharLiteralExpression(node.getEscapedValue()));
 		return false;
 	}
 	
 	public boolean visit(NullLiteral node) {
 		pushExpression(new CSNullLiteralExpression());
 		return false;
 	}
 	
 	public boolean visit(BooleanLiteral node) {
 		pushExpression(new CSBoolLiteralExpression(node.booleanValue()));
 		return false;
 	}
 	
 	public boolean visit(ThisExpression node) {
 		pushExpression(new CSThisExpression());
 		return false;
 	}
 	
 	public boolean visit(ArrayAccess node) {
 		pushExpression(new CSIndexedExpression(mapExpression(node.getArray()), mapExpression(node.getIndex()))); 
 		return false;
 	}
 	
 	public boolean visit(ArrayCreation node) {			
 		if (node.dimensions().size() > 1) {
 			if (null != node.getInitializer()) {
 				notImplemented(node);
 			}
 			pushExpression(unfoldMultiArrayCreation(node));			
 		} else {
 			pushExpression(mapSingleArrayCreation(node));
 		}
 		return false;
 	}
 
 	/**
 	 * Unfolds java multi array creation shortcut "new String[2][3][2]" into explicitly
 	 * array creation "new string[][][] {
 	 * 	 					new string[][] { new string[2], new string[2], new string[2] },
 	 * 						new string[][] { new string[2], new string[2], new string[2] } }"
 	 */
 	private CSArrayCreationExpression unfoldMultiArrayCreation(ArrayCreation node) {
 		return unfoldMultiArray((ArrayType) node.getType().getComponentType(), node.dimensions(), 0);
 	}
 
 	private CSArrayCreationExpression unfoldMultiArray(ArrayType type, List dimensions, int dimensionIndex) {
 		final CSArrayCreationExpression expression = new CSArrayCreationExpression(mappedTypeReference(type));
 		expression.initializer(new CSArrayInitializerExpression());
 		int length = resolveIntValue(dimensions.get(dimensionIndex));
 		if (dimensionIndex < lastIndex(dimensions) - 1) {
 			for (int i=0; i<length; ++i) {
 				expression.initializer().addExpression(
 						unfoldMultiArray((ArrayType) type.getComponentType(), dimensions, dimensionIndex+1));
 			}
 		} else {
 			Expression innerLength = (Expression)dimensions.get(dimensionIndex+1);
 			CSTypeReferenceExpression innerType = mappedTypeReference(type.getComponentType());
 			for (int i=0; i<length; ++i) {
 				expression.initializer().addExpression(
 						new CSArrayCreationExpression(innerType, mapExpression(innerLength)));
 			}
 		}
 		return expression;
 	}
 
 	private int lastIndex(List dimensions) {
 		return dimensions.size()-1;
 	}
 
 	private int resolveIntValue(Object expression) {
 		return ((Number)((Expression)expression).resolveConstantExpressionValue()).intValue();
 	}
 
 	private CSArrayCreationExpression mapSingleArrayCreation(ArrayCreation node) {
 		CSArrayCreationExpression expression = new CSArrayCreationExpression(mappedTypeReference(componentType(node.getType())));
 		if (!node.dimensions().isEmpty()) {
 			expression.length(mapExpression((Expression) node.dimensions().get(0)));
 		}
 		expression.initializer(mapArrayInitializer(node));
 		return expression;
 	}
 
 	private CSArrayInitializerExpression mapArrayInitializer(ArrayCreation node) {
 		return (CSArrayInitializerExpression)mapExpression(node.getInitializer());
 	}
 
 	public boolean visit(ArrayInitializer node) {
 		if (isImplicitelyTypedArrayInitializer(node)) {
 			CSArrayCreationExpression ace = new CSArrayCreationExpression(mappedTypeReference(node.resolveTypeBinding().getComponentType()));
 			ace.initializer(mapArrayInitializer(node));
 			pushExpression(ace);
 			return false;
 		}
 		
 		pushExpression(mapArrayInitializer(node));
 		return false;
 	}
 	
 	private CSArrayInitializerExpression mapArrayInitializer(ArrayInitializer node) {
 		CSArrayInitializerExpression initializer = new CSArrayInitializerExpression();
 		for (Object e : node.expressions()) {
 			initializer.addExpression(mapExpression((Expression) e));
 		}
 		return initializer;
 	}
 
 	private boolean isImplicitelyTypedArrayInitializer(ArrayInitializer node) {
 		return !(node.getParent() instanceof ArrayCreation);
 	}
 
 	public ITypeBinding componentType(ArrayType type) {
 		return type.getComponentType().resolveBinding();
 	}
 	
 	@Override
 	public boolean visit(EnhancedForStatement node) {
 		CSForEachStatement stmt = new CSForEachStatement(node.getStartPosition(), mapExpression(node.getExpression()));
 		stmt.variable(createParameter(node.getParameter()));
 		visitBlock(stmt.body(), node.getBody());
 		addStatement(stmt);
 		return false;
 	}
 
 	public boolean visit(ForStatement node) {
 		CSForStatement stmt = new CSForStatement(node.getStartPosition(), mapExpression(node.getExpression()));
 		for (Object i : node.initializers()) {
 			stmt.addInitializer(mapExpression((Expression)i));
 		}
 		for (Object u : node.updaters()) {
 			stmt.addUpdater(mapExpression((Expression) u));
 		}
 		visitBlock(stmt.body(), node.getBody());
 		addStatement(stmt);
 		return false;
 	}
 	
 	public boolean visit(SwitchStatement node) {
 		CSBlock saved = _currentBlock;
 		
 		CSSwitchStatement stmt = new CSSwitchStatement(node.getStartPosition(), mapExpression(node.getExpression()));
 		addStatement(stmt);
 		
 		Iterator i = node.statements().iterator();
 		CSCaseClause defaultClause = null;
 		CSCaseClause current = null;
 		while (i.hasNext()) {
 			ASTNode element = (ASTNode) i.next();
 			if (ASTNode.SWITCH_CASE == element.getNodeType()) {
 				if (null == current) {
 					current = new CSCaseClause();
 					stmt.addCase(current);
 					_currentBlock = current.body();
 				}
 				SwitchCase sc = (SwitchCase)element;
 				if (sc.isDefault()) {
 					defaultClause = current;
 					current.isDefault(true);
 				} else {
 					current.addExpression(mapExpression(sc.getExpression()));
 				}
 			} else {
 				current = null;
 				element.accept(this);
 			}
 		}
 		
 		if (null != defaultClause) {
 			defaultClause.body().addStatement(new CSBreakStatement(Integer.MIN_VALUE));
 		}
 		
 		_currentBlock = saved;
 		return false;
 	}
 	
 	public boolean visit(CastExpression node) {
 		pushExpression(new CSCastExpression(mappedTypeReference(node.getType()), mapExpression(node.getExpression())));
 		return false;
 	}
 	
 	public boolean visit(PrefixExpression node) {
 		pushExpression(new CSPrefixExpression(node.getOperator().toString(), mapExpression(node.getOperand())));
 		return false;
 	}
 	
 	public boolean visit(PostfixExpression node) {
 		pushExpression(new CSPostfixExpression(node.getOperator().toString(), mapExpression(node.getOperand())));
 		return false;
 	}
 	
 	public boolean visit(InfixExpression node) {
 		
 		CSExpression left = mapExpression(node.getLeftOperand());
 		CSExpression right = mapExpression(node.getRightOperand());
 		if (node.getOperator() == InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED) {
 			String operator = ">>";
 			pushExpression(new CSInfixExpression("&", right, new CSNumberLiteralExpression("0x1f")));
 			pushExpression(new CSParenthesizedExpression(popExpression()));
 			pushExpression(new CSInfixExpression(operator, new CSParenthesizedExpression(left), popExpression()));
 		} else {
 			String operator = node.getOperator().toString();
 			pushExpression(new CSInfixExpression(operator, left, right));
 			pushExtendedOperands(operator, node);
 		}
 		
 		return false;
 	}
 
 	private void pushExtendedOperands(String operator, InfixExpression node) {
 		for (Object x : node.extendedOperands()) {
 			pushExpression(new CSInfixExpression(operator, popExpression(), mapExpression((Expression)x)));
 		}
 	}
 	
 	public boolean visit(ParenthesizedExpression node) {
 		pushExpression(new CSParenthesizedExpression(mapExpression(node.getExpression())));
 		return false;
 	}
 	
 	public boolean visit(ConditionalExpression node) {
 		pushExpression(new CSConditionalExpression(mapExpression(node.getExpression()), mapExpression(node.getThenExpression()), mapExpression(node.getElseExpression())));
 		return false;
 	}
 	
 	public boolean visit(InstanceofExpression node) {
 		pushExpression(new CSInfixExpression("is", mapExpression(node.getLeftOperand()), mappedTypeReference(node.getRightOperand().resolveBinding())));
 		return false;
 	}
 	
 	public boolean visit(Assignment node) {
 		Expression lhs = node.getLeftHandSide();
 		pushExpression(new CSInfixExpression(node.getOperator().toString(), mapExpression(lhs), mapExpression(lhs.resolveTypeBinding(), node.getRightHandSide())));
 		return false;
 	}
 
 	private CSExpression mapExpression(ITypeBinding expectedType,
 			Expression expression) {
 		return castIfNeeded(expectedType, expression.resolveTypeBinding(), mapExpression(expression));
 	}
 	
 	private CSExpression castIfNeeded(ITypeBinding expectedType, ITypeBinding actualType, CSExpression expression) {
 		ITypeBinding charType = resolveWellKnownType("char");
 		if (expectedType != charType) return expression;
 		if (actualType == expectedType) return expression;
 		return new CSCastExpression(mappedTypeReference(expectedType), expression);
 	}
 
 	public boolean visit(ClassInstanceCreation node) {
 		if (null != node.getAnonymousClassDeclaration()) {
 			node.getAnonymousClassDeclaration().accept(this);
 			return false;
 		}
 		
 		CSMethodInvocationExpression expression = mapConstructorInvocation(node);
 		if (null == expression) {
 			return false;
 		}
 		
 		if (isNonStaticNestedTypeCreation(node)) {
 			expression.addArgument(new CSThisExpression());
 		}
 		
 		mapArguments(expression, node.arguments());
 		pushExpression(expression);
 		return false;
 	}
 
 	private boolean isNonStaticNestedTypeCreation(ClassInstanceCreation node) {
 		return isNonStaticNestedType(node.resolveTypeBinding());
 	}
 
 	private CSMethodInvocationExpression mapConstructorInvocation(ClassInstanceCreation node) {
 		Configuration.MemberMapping mappedConstructor = methodMapping(node.resolveConstructorBinding());
 		if (null == mappedConstructor) {
 			return new CSConstructorInvocationExpression(mappedTypeReference(node.resolveTypeBinding()));
 		}
 		if (mappedConstructor.name.startsWith("System.Convert.To")) {
 			if (optimizeSystemConvert(mappedConstructor.name, node)) {
 				return null;
 			}
 		}
 		return new CSMethodInvocationExpression(new CSReferenceExpression(methodName(mappedConstructor.name)));
 	}
 	
 	private boolean optimizeSystemConvert(String mappedConstructor, ClassInstanceCreation node) {
 		String typeName = _configuration.getConvertRelatedWellKnownTypeName(mappedConstructor);
 		if (null != typeName) {
 			assert 1 == node.arguments().size();
 			Expression arg = (Expression)node.arguments().get(0);
 			if (arg.resolveTypeBinding() == resolveWellKnownType(typeName)) {
 				arg.accept(this);
 				return true;
 			}
 		}
 		return false;
 	}
 	
 	public boolean visit(TypeLiteral node) {
 		pushTypeOfExpression(mappedTypeReference(node.getType()));
 		return false;
 	}
 
 	private void pushTypeOfExpression(CSTypeReferenceExpression type) {
 		if (_configuration.nativeTypeSystem()) {
 			pushExpression(new CSTypeofExpression(type));
 		} else {
 			pushGetClassForTypeExpression(type);
 		}
 	}
 
 	private void pushGetClassForTypeExpression(final CSTypeReferenceExpression typeName) {
 		CSMethodInvocationExpression mie = new CSMethodInvocationExpression(
 				new CSReferenceExpression(
 						methodName(_configuration.getRuntimeTypeName() + ".getClassForType")));		
 		mie.addArgument(new CSTypeofExpression(typeName));
 		pushExpression(mie);
 	}
 	
 	public boolean visit(SuperMethodInvocation node) {
 		if (null != node.getQualifier()) {
 			notImplemented(node);
 		}
 		
 		IMethodBinding binding = originalMethodBinding(node.resolveMethodBinding());
 		Configuration.MemberMapping mapping = methodMapping(binding);
 		CSExpression target = new CSMemberReferenceExpression(
 									new CSBaseExpression(),
 									mappedMethodName(binding));
 		
 		if (mapping != null && mapping.kind != MemberKind.Method) {
 			pushExpression(target);
 			return false;
 		}
 		
 		CSMethodInvocationExpression mie = new CSMethodInvocationExpression(target);
 		mapArguments(mie, node.arguments());
 		pushExpression(mie);
 		return false;
 	}
 	
 	public boolean visit(MethodInvocation node) {
 		IMethodBinding binding = originalMethodBinding(node.resolveMethodBinding());
 		Configuration.MemberMapping mapping = methodMapping(binding);
 		
 		if (null == mapping && isIndexer(node)) {
 			mapping = new MemberMapping(null, MemberKind.Indexer);
 		}
 		
 		if (null == mapping) {
 			processUnmappedMethodInvocation(node);
 		} else {
 			processMappedMethodInvocation(node, binding, mapping);
 		}
 		return false;
 	}
 
 	private boolean isIndexer(MethodInvocation node) {
 		final MethodDeclaration declaration = findMethodDeclaration(node);
 		if (null == declaration) {
 			return false;
 		}
 		return isTaggedDeclaration(declaration, Annotations.SHARPEN_INDEXER);
 	}
 	
 	private IMethodBinding originalMethodBinding(IMethodBinding binding) {
 		IMethodBinding original = Bindings.findMethodDefininition(binding, _ast.getAST());
 		if (null != original) return original;
 		return binding;
 	}
 	
 	private void processUnmappedMethodInvocation(MethodInvocation node) {
 		
 		if (isMappedEventSubscription(node)) {
 			processMappedEventSubscription(node);
 			return;
 		}
 		
 		if (isEventSubscription(node)) {
 			processEventSubscription(node);
 			return;
 		}
 		
 		if (isRemovedMethodInvocation(node)) {
 			processRemovedInvocation(node);
 			return;
 		}
 		
 		String name = mappedMethodName(node.resolveMethodBinding(), null);
 		final CSExpression targetExpression = mapMethodTargetExpression(node);
 		CSExpression target = null == targetExpression
 			? new CSReferenceExpression(name)
 			: new CSMemberReferenceExpression(targetExpression, name);
 		CSMethodInvocationExpression mie = new CSMethodInvocationExpression(target);
 		mapMethodInvocationArguments(mie, node);
 		pushExpression(mie);
 	}
 
 	private void processMappedEventSubscription(MethodInvocation node) {
 		
 		final MethodInvocation event = (MethodInvocation) node.getExpression();
 		final String eventArgsType = _configuration.mappedEvent(qualifiedName(event));
 		final String eventHandlerType = buildEventHandlerTypeName(node, eventArgsType);
 		mapEventSubscription(node, eventArgsType, eventHandlerType);
 	}
 
 	private void processRemovedInvocation(MethodInvocation node) {
 		TagElement element = JavadocUtility.getJavadocTag(findMethodDeclaration(node), Annotations.SHARPEN_REMOVE);
 		
 		String exchangeValue = getSingleTextFragment(element);			
 		pushExpression(new CSReferenceExpression(exchangeValue));
 	}
 
 	private void mapMethodInvocationArguments(CSMethodInvocationExpression mie, MethodInvocation node) {
 		final List arguments = node.arguments();
 		final IMethodBinding actualMethod = node.resolveMethodBinding();
 		final ITypeBinding[] actualTypes = actualMethod.getParameterTypes();
 		final IMethodBinding originalMethod = actualMethod.getMethodDeclaration();
 		final ITypeBinding[] originalTypes = originalMethod.getParameterTypes();
		for (int i=0; i<arguments.size(); ++i) {
 			Expression arg = (Expression) arguments.get(i);
			if (i < originalTypes.length && isGenericRuntimeParameterIdiom(originalMethod, originalTypes[i])) {
 				mie.addTypeArgument(genericRuntimeTypeIdiomType(actualTypes[i]));
 			} else {
 				addArgument(mie, arg);
 			}
 		}
 	}
 
 	private void processEventSubscription(MethodInvocation node) {
 		
 		final MethodDeclaration addListener = findMethodDeclaration(node);
 		assertValidEventAddListener(node, addListener);
 		
 		final MethodInvocation eventInvocation = (MethodInvocation)node.getExpression();
 		
 		final MethodDeclaration eventDeclaration = findMethodDeclaration(eventInvocation);
 		mapEventSubscription(node, getEventArgsType(eventDeclaration), getEventHandlerTypeName(eventDeclaration));
 	}
 
 	private void mapEventSubscription(MethodInvocation node,
 			final String eventArgsType, final String eventHandlerType) {
 		final CSAnonymousClassBuilder listenerBuilder = mapAnonymousEventListener(node);
 		final CSMemberReferenceExpression handlerMethodRef = new CSMemberReferenceExpression(
 								listenerBuilder.createConstructorInvocation(),
 								eventListenerMethodName(listenerBuilder));
 		
 	
 		final CSReferenceExpression delegateType = new CSReferenceExpression(eventHandlerType);
 		
 		patchEventListener(listenerBuilder, eventArgsType);
 		
 		CSConstructorInvocationExpression delegateConstruction = new CSConstructorInvocationExpression(delegateType);
 		delegateConstruction.addArgument(handlerMethodRef);
 		
 		pushExpression(
 			new CSInfixExpression(
 				"+=",
 				mapMethodTargetExpression(node),
 				delegateConstruction));
 	}
 
 	private CSAnonymousClassBuilder mapAnonymousEventListener(MethodInvocation node) {
 		ClassInstanceCreation creation = (ClassInstanceCreation) node.arguments().get(0);
 		return mapAnonymousClass(creation.getAnonymousClassDeclaration());
 	}
 
 	private String eventListenerMethodName(final CSAnonymousClassBuilder listenerBuilder) {
 		return mappedMethodName(getFirstMethod(listenerBuilder.anonymousBaseType()));
 	}
 
 	private void patchEventListener(CSAnonymousClassBuilder listenerBuilder, String eventArgsType) {
 		final CSClass type = listenerBuilder.type();
 		type.clearBaseTypes();
 		
 		final CSMethod handlerMethod = (CSMethod)type.getMember(eventListenerMethodName(listenerBuilder));
 		handlerMethod.parameters().get(0).type(OBJECT_TYPE_REFERENCE);
 		handlerMethod.parameters().get(0).name("sender");
 		handlerMethod.parameters().get(1).type(new CSTypeReference(eventArgsType));
 		
 	}
 
 	private IMethodBinding getFirstMethod(ITypeBinding listenerType) {
 		return listenerType.getDeclaredMethods()[0];
 	}
 
 	private void assertValidEventAddListener(ASTNode source, MethodDeclaration addListener) {
 		if (isValidEventAddListener(addListener)) return;
 		
 		unsupportedConstruct(source, Annotations.SHARPEN_EVENT_ADD + " must take lone single method interface argument");
 	}
 
 	private boolean isValidEventAddListener(MethodDeclaration addListener) {
 		if (1 != addListener.parameters().size()) return false;
 		
 		final ITypeBinding type = getFirstParameterType(addListener);
 		if (!type.isInterface()) return false;
 		
 		return type.getDeclaredMethods().length == 1;
 	}
 
 	private ITypeBinding getFirstParameterType(MethodDeclaration addListener) {
 		return parameter(addListener, 0).getType().resolveBinding();
 	}
 
 	private SingleVariableDeclaration parameter(MethodDeclaration method,
 			final int index) {
 		return (SingleVariableDeclaration) method.parameters().get(index);
 	}
 
 	private boolean isEventSubscription(MethodInvocation node) {
 		return isTaggedMethodInvocation(node, Annotations.SHARPEN_EVENT_ADD);
 	}
 
 	private boolean isMappedEventSubscription(MethodInvocation node) {
 		return _configuration.isMappedEventAdd(qualifiedName(node));
 	}
 
 	private String qualifiedName(MethodInvocation node) {
 		return qualifiedName(node.resolveMethodBinding());
 	}
 
 	private boolean isTaggedMethodInvocation(MethodInvocation node,
 			final String tag) {
 		final MethodDeclaration method = findMethodDeclaration(node);
 		if (null == method) return false;
 		return null != JavadocUtility.getJavadocTag(method, tag);
 	}
 
 	private MethodDeclaration findMethodDeclaration(MethodInvocation node) {
 		return (MethodDeclaration)findDeclaringNode(node.resolveMethodBinding());
 	}
 
 	@SuppressWarnings("unchecked")
 	private void processMappedMethodInvocation(MethodInvocation node, IMethodBinding binding, Configuration.MemberMapping mapping) {
 		
 		if (mapping.kind == MemberKind.Indexer) {
 			processIndexerInvocation(node, binding, mapping);
 			return;
 		}
 		
 		String name = mappedMethodName(binding, mapping);
 		if (0 == name.length()) {
 			final Expression expression = node.getExpression();
 			final CSExpression target = expression != null
 				? mapExpression(expression)
 				: new CSThisExpression(); // see collections/EntrySet1
 			pushExpression(target);
 			return;
 		}
 		
 		boolean isMappingToStaticMethod = isMappingToStaticMember(name);
 		
 		List<Expression> arguments = node.arguments();
 		CSExpression expression = mapMethodTargetExpression(node);
 		CSExpression target = null;
 		
 		if (null == expression || isMappingToStaticMethod) {
 			target = new CSReferenceExpression(name);
 		} else {			
 			if (isStatic(binding) && arguments.size() > 0) {
 				// mapping static method to instance member
 				// typical example is String.valueOf(arg) => arg.ToString()
 				target = new CSMemberReferenceExpression(parensIfNeeded(mapExpression(arguments.get(0))), name);
 				arguments = arguments.subList(1, arguments.size());
 			} else {
 				target = new CSMemberReferenceExpression(expression, name);
 			}
 		}
 		
 		if (mapping.kind != MemberKind.Method) {
 			switch (arguments.size()) {
 			case 0:
 				pushExpression(target);
 				break;
 				
 			case 1:
 				pushExpression(
 					new CSInfixExpression(
 						"=",
 						target,
 						mapExpression(arguments.get(0))));
 				break;
 				
 			default:
 				unsupportedConstruct(node, "Method invocation with more than 1 argument mapped to property");
 				break;
 			}
 			return;
 		}
 		
 		CSMethodInvocationExpression mie = new CSMethodInvocationExpression(target);
 		if (isMappingToStaticMethod && isInstanceMethod(binding)) {
 			if (null == expression) {
 				mie.addArgument(new CSThisExpression());
 			} else {
 				mie.addArgument(expression);
 			}
 		}
 		mapArguments(mie, arguments);
 		pushExpression(mie);
 	}
 
 	private void processIndexerInvocation(MethodInvocation node, IMethodBinding binding, MemberMapping mapping) {
 		if (node.arguments().size() != 1) {
 			unsupportedConstruct(node, "indexer with any argument number but 1");
 		}
 		pushExpression(
 				new CSIndexedExpression(
 						mapIndexerTarget(node),
 						mapExpression((Expression) node.arguments().get(0))));
 	}
 
 	private CSExpression mapIndexerTarget(MethodInvocation node) {
 		if (node.getExpression() == null) {
 			return new CSThisExpression(); 
 		}
 		return mapMethodTargetExpression(node);
 	}
 
 	private CSExpression parensIfNeeded(CSExpression expression) {
 		if (expression instanceof CSInfixExpression
 			|| expression instanceof CSPrefixExpression
 			|| expression instanceof CSPostfixExpression) {
 				
 			return new CSParenthesizedExpression(expression);
 		}
 		return expression;
 	}
 
 	protected CSExpression mapMethodTargetExpression(MethodInvocation node) {
 		return mapExpression(node.getExpression());
 	}
 
 	private boolean isInstanceMethod(IMethodBinding binding) {
 		return !isStatic(binding);
 	}
 
 	private boolean isMappingToStaticMember(String name) {
 		return -1 != name.indexOf('.');
 	}
 
 	protected void mapArguments(CSMethodInvocationExpression mie, List arguments) {
 		for (Object arg : arguments) {
 			addArgument(mie, (Expression)arg);
 		}
 	}
 
 	private void addArgument(CSMethodInvocationExpression mie, Expression arg) {
 		mie.addArgument(mapExpression(arg));
 	}
 	
 	public boolean visit(FieldAccess node) {
 		String name = mappedFieldName(node);
 		if (null == node.getExpression()) {			
 			pushExpression(new CSReferenceExpression(name));			
 		} else {
 			pushExpression(new CSMemberReferenceExpression(mapExpression(node.getExpression()), name));
 		}
 		return false;
 	}
 
 	private boolean isBoolLiteral(String name) {
 		return name.equals("true") || name.equals("false");
 	}
 
 	private String mappedFieldName(FieldAccess node) {
 		String name = mappedFieldName(node.getName());
 		if (null != name) return name;
 		return identifier(node.getName());
 	}
 	
 	public boolean visit(SimpleName node) {
 		if (isTypeReference(node)) {
 			pushTypeReference(node.resolveTypeBinding());
 		} else {
 			pushExpression(new CSReferenceExpression(identifier(node)));
 		}
 		return false;
 	}
 	
 	private void addStatement(CSStatement statement) {
 		_currentBlock.addStatement(statement);
 	}
 
 	private void pushTypeReference(ITypeBinding typeBinding) {
 		pushExpression(createTypeReference(typeBinding));
 	}
 
 	protected CSReferenceExpression createTypeReference(ITypeBinding typeBinding) {
 		return new CSReferenceExpression(mappedTypeName(typeBinding));
 	}
 
 	private boolean isTypeReference(Name node) {
 		final IBinding binding = node.resolveBinding();
 		if (null == binding) {
 			unresolvedTypeBinding(node);
 			return false;
 		}
 		return IBinding.TYPE == binding.getKind();
 	}
 	
 	public boolean visit(QualifiedName node) {
 		if (isTypeReference(node)) {
 			pushTypeReference(node.resolveTypeBinding());
 		} else {
 			String primitiveTypeRef = checkForPrimitiveTypeReference(node);
 			if (primitiveTypeRef != null) {
 				pushTypeOfExpression(new CSTypeReference(primitiveTypeRef));
 			} else {
 				handleRegularQualifiedName(node);
 			}
 		}
 		return false;
 	}
 
 	private void handleRegularQualifiedName(QualifiedName node) {
 		String mapped = mappedFieldName(node);
 		if (null != mapped) {
 			if (isBoolLiteral(mapped)) {
 				pushExpression(new CSBoolLiteralExpression(Boolean.parseBoolean(mapped)));
 				return;
 			}
 			if (isMappingToStaticMember(mapped)) {
 				pushExpression(new CSReferenceExpression(mapped));
 			} else {
 				pushMemberReferenceExpression(node.getQualifier(), mapped);
 			}
 		} else {			
 			Name qualifier = node.getQualifier();
 			String name = identifier(node.getName().getIdentifier());
 			pushMemberReferenceExpression(qualifier, name);
 		}
 	}
 
 	private String checkForPrimitiveTypeReference(QualifiedName node) {
 		String name = qualifiedName(node);
 		if (name.equals(JAVA_LANG_VOID_TYPE)) return "void";
 		if (name.equals(JAVA_LANG_BOOLEAN_TYPE)) return "bool";
 		if (name.equals(JAVA_LANG_BYTE_TYPE)) return "byte";
 		if (name.equals(JAVA_LANG_CHARACTER_TYPE)) return "char";
 		if (name.equals(JAVA_LANG_SHORT_TYPE)) return "short";
 		if (name.equals(JAVA_LANG_INTEGER_TYPE)) return "int";
 		if (name.equals(JAVA_LANG_LONG_TYPE)) return "long";
 		if (name.equals(JAVA_LANG_FLOAT_TYPE)) return "float";
 		if (name.equals(JAVA_LANG_DOUBLE_TYPE)) return "double";
 		return null;
 	}
 	
 	private String qualifiedName(QualifiedName node) {
 		IVariableBinding binding = variableBinding(node);
 		if (binding == null) return node.toString();
 		return qualifiedName(binding);
 	}
 
 	private void pushMemberReferenceExpression(Name qualifier, String name) {
 		pushExpression(new CSMemberReferenceExpression(mapExpression(qualifier), name));
 	}
 	
 	private IVariableBinding variableBinding(Name node) {
 		if (node.resolveBinding() instanceof IVariableBinding) {
 			return (IVariableBinding)node.resolveBinding();
 		}
 		return null;
 	}
 	
 	private String mappedFieldName(Name node) {
 		IVariableBinding binding = variableBinding(node);
 		return null == binding ? null : mappedFieldName(binding);
 	}
 
 	private String mappedFieldName(IVariableBinding binding) {
 		if (!binding.isField()) return null;
 		Configuration.MemberMapping mapping = _configuration.mappedMember(qualifiedName(binding));
 		return mapping != null ? mapping.name : null;
 	}
 
 	protected CSExpression mapExpression(Expression expression) {
 		if (null == expression) return null;
 		
 		try {
 			expression.accept(this);
 			return popExpression();
 		} catch (Exception e) {
 			unsupportedConstruct(expression, e);
 			return null; // we'll never get here
 		}
 	}
 
 	private void unsupportedConstruct(ASTNode node, Exception cause) {
 		unsupportedConstruct(node, "failed to map: '" + node + "'", cause);
 	}
 	
 	private void unsupportedConstruct(ASTNode node, String message) {
 		unsupportedConstruct(node, message, null);
 	}
 
 	private void unsupportedConstruct(ASTNode node, final String message, Exception cause) {
 		throw new IllegalArgumentException(sourceInformation(node) + ": " + message, cause);
 	}
 	
 	protected void pushExpression(CSExpression expression) {
 		if (null != _currentExpression) {
 			throw new IllegalStateException();
 		}
 		_currentExpression = expression;
 	}
 
 	private CSExpression popExpression() {
 		if (null == _currentExpression ) {
 			throw new IllegalStateException();
 		}
 		CSExpression found = _currentExpression;
 		_currentExpression = null;
 		return found;
 	}
 
 	private CSVariableDeclaration createParameter(SingleVariableDeclaration declaration) {
 		return createVariableDeclaration(declaration.resolveBinding(), null);
 	}
 
 	protected void visit(List nodes) {
 		for (Object node : nodes) {
 			((ASTNode) node).accept(this);
 		}
 	}
 
 	private void createInheritedAbstractMemberStubs(TypeDeclaration node) {
 		if (node.isInterface()) return;
 		
 		ITypeBinding binding = node.resolveBinding();
 
 		Set<ITypeBinding> interfaces = new LinkedHashSet<ITypeBinding>();
 		collectInterfaces(interfaces, binding);
 		for (ITypeBinding baseType : interfaces) {
 			createInheritedAbstractMemberStubs(binding, baseType);
 		}
 	}
 
 	private void collectInterfaces(Set<ITypeBinding> interfaceList, ITypeBinding binding) {
 		ITypeBinding[] interfaces = binding.getInterfaces();
 		for (int i = 0; i < interfaces.length; ++i) {
 			ITypeBinding interfaceBinding = interfaces[i];
 			if (interfaceList.contains(interfaceBinding)) {
 				continue;
 			}
 			collectInterfaces(interfaceList, interfaceBinding);
 			interfaceList.add(interfaceBinding);
 		}
 	}
 
 	private void createInheritedAbstractMemberStubs(ITypeBinding type, ITypeBinding baseType) {
 		IMethodBinding[] methods = baseType.getDeclaredMethods();
 		for (int i = 0; i < methods.length; ++i) {
 			IMethodBinding method = methods[i];
 			if (!Modifier.isAbstract(method.getModifiers())) {
 				continue;
 			}
 			if (null != Bindings.findOverriddenMethodInTypeOrSuperclasses(type, method)) {
 				continue;
 			}
 			if (stubIsProperty(method)) {
 				_currentType.addMember(createAbstractPropertyStub(method));
 			} else {
 				_currentType.addMember(createAbstractMethodStub(method));
 			}
 		}
 	}
 
 	private boolean stubIsProperty(IMethodBinding method) {
 		MethodDeclaration dec = (MethodDeclaration)findDeclaringNode(method);
 		return dec != null && isProperty(dec);
 	}
 
 	private CSProperty createAbstractPropertyStub(IMethodBinding method) {
 		CSProperty stub = new CSProperty(mappedMethodName(method), mappedTypeReference(method.getReturnType()));
 		stub.modifier(CSMethodModifier.Abstract);
 		stub.visibility(mapVisibility(method.getModifiers()));
 		stub.getter(new CSBlock());
 		return stub;
 	}
 
 	private CSMethod createAbstractMethodStub(IMethodBinding method) {
 		CSMethod stub = new CSMethod(mappedMethodName(method));
 		stub.modifier(CSMethodModifier.Abstract);
 		stub.visibility(mapVisibility(method.getModifiers()));		
 		stub.returnType(mappedTypeReference(method.getReturnType()));
 		
 		ITypeBinding[] parameters = method.getParameterTypes();
 		for (int i = 0; i < parameters.length; ++i) {
 			stub.addParameter(
 				new CSVariableDeclaration("arg" + (i + 1), mappedTypeReference(parameters[i])));
 		}
 		
 		return stub;
 	}
 
 	CSMethodModifier mapMethodModifier(MethodDeclaration method) {
 		if (_currentType.isInterface()) {
 			return CSMethodModifier.Abstract;
 		}
 		int modifiers = method.getModifiers();
 		if (Modifier.isStatic(modifiers)) {
 			return CSMethodModifier.Static;
 		}
 		if (Modifier.isPrivate(modifiers)) {
 			return CSMethodModifier.None;
 		}
 		
 		boolean override = isOverride(method);
 		if (Modifier.isAbstract(modifiers)) {
 			return override
 				? CSMethodModifier.AbstractOverride
 				: CSMethodModifier.Abstract;
 		}		
 		boolean isFinal = Modifier.isFinal(modifiers);
 		if (override) {
 			return isFinal
 				? CSMethodModifier.Sealed
 				: CSMethodModifier.Override;
 		}
 		return isFinal || _currentType.isSealed()
 			? CSMethodModifier.None
 			: CSMethodModifier.Virtual;
 	}
 
 	private boolean isOverride(MethodDeclaration method) {
 		IMethodBinding methodBinding = method.resolveBinding();
 		ITypeBinding superclass = _ignoreExtends.value()
 			? resolveWellKnownType("java.lang.Object")
 			: methodBinding.getDeclaringClass().getSuperclass();
 		if (null == superclass) return false;
 		return null != Bindings.findOverriddenMethodInHierarchy(superclass, methodBinding);
 	}
 
 	CSClassModifier mapClassModifier(int modifiers) {
 		if (Modifier.isAbstract(modifiers)) {
 			return CSClassModifier.Abstract;
 		}
 		if (Modifier.isFinal(modifiers)) {
 			return CSClassModifier.Sealed;
 		}
 		return CSClassModifier.None;
 	}
 	
 	CSVisibility mapVisibility(BodyDeclaration node) {
 		if (null != JavadocUtility.getJavadocTag(node, Annotations.SHARPEN_INTERNAL)) {
 			return CSVisibility.Internal;
 		} else if (null != JavadocUtility.getJavadocTag(node, Annotations.SHARPEN_PRIVATE)) {
 			return CSVisibility.Private;
 		}
 		
 		return mapVisibility(node.getModifiers());
 	}
 
 	CSVisibility mapVisibility(int modifiers) {
 		if (Modifier.isPublic(modifiers)) {
 			return CSVisibility.Public;
 		}
 		if (Modifier.isProtected(modifiers)) {
 			return CSVisibility.Protected;
 		}
 		if (Modifier.isPrivate(modifiers)) {
 			return CSVisibility.Private;
 		}
 		return CSVisibility.Internal;
 	}
 	
 	protected CSTypeReferenceExpression mappedTypeReference(Type type) {
 		return mappedTypeReference(type.resolveBinding());
 	}
 	
 	protected CSTypeReferenceExpression mappedTypeReference(ITypeBinding type) {
 		if (type.isArray()) {
 			return mappedArrayTypeReference(type);
 		}
 		if (type.isWildcardType()) {
 			return mappedWildcardTypeReference(type);
 		}
 		final CSTypeReference typeRef = new CSTypeReference(mappedTypeName(type));
 		for (ITypeBinding arg : type.getTypeArguments()) {
 			typeRef.addTypeArgument(mappedTypeReference(arg));
 		}
 		return typeRef;
 	}
 
 	private CSTypeReferenceExpression mappedWildcardTypeReference(ITypeBinding type) {
 		final ITypeBinding bound = type.getBound();
 		return bound != null
 			? mappedTypeReference(bound)
 			: OBJECT_TYPE_REFERENCE;
 	}
 
 	private CSTypeReferenceExpression mappedArrayTypeReference(ITypeBinding type) {
 		return new CSArrayTypeReference(
 						mappedTypeReference(type.getElementType()),
 						type.getDimensions());
 		
 	}
 
 	protected String mappedTypeName(ITypeBinding type) {
 		if (type.isArray() || type.isWildcardType()) {
 			throw new IllegalArgumentException("type");
 		}
 		if (shouldPrefixInterface(type)) {
 			return registerMappedType(type, mappedInterfaceName(mappedTypeNameForKey(type)));
 		}
 		return registerMappedType(type, mappedTypeNameForKey(type));
 	}
 	
 	private String mappedInterfaceName(String name) {
 		int pos = name.lastIndexOf('.');
 		return name.substring(0, pos) + "." + interfaceName(name.substring(pos + 1));
 	}
 
 	private String mappedTypeNameForKey(ITypeBinding type) {
 		return mappedTypeName(typeMappingKey(type), qualifiedName(type));
 	}
 
 	private String typeMappingKey(ITypeBinding type) {
 		final ITypeBinding[] typeArguments = type.getTypeArguments();
 		if (typeArguments.length > 0) {
 			return qualifiedName(type) + "<" + repeat(',', typeArguments.length-1) + ">";
 		}
 		return qualifiedName(type);
 	}
 
 	private String repeat(char c, int count) {
 		StringBuilder builder = new StringBuilder(count);
 		for (int i=0; i<count; ++i) {
 			builder.append(c);
 		}
 		return builder.toString();
 	}
 
 	private String registerMappedType(ITypeBinding type, String fullName) {
 		if (!_configuration.organizeUsings()) return fullName;
 
 		int pos = fullName.lastIndexOf(".");
 		if (pos == -1) return fullName;
 
 		if(!hasMapping(type)){
 			pos = nameSpaceLength(type, fullName, pos);
 		}
 		
 		String namespace = fullName.substring(0, pos);
 		registerNamespace(namespace);
 		String name = fullName.substring(pos + 1);
 		
 		if (isConflicting(name)) return fullName;
 		
 		_compilationUnit.addUsing(new CSUsing(namespace));
 		return name;
 	}
 
 	private int nameSpaceLength(ITypeBinding type, String fullName, int pos) {
 		while (type.isNested()) {		
 			pos = fullName.lastIndexOf(".", pos -1);
 			type = type.getDeclaringClass();
 		}		
 		return pos;
 	}
 
 	private boolean isConflicting(String name) {
 		return _configuration.shouldFullyQualifyTypeName(name)
 			|| _namespaces.contains(name)
 			|| _mappedMethodDeclarations.contains(name);
 	}
 
 	private void registerNamespace(String namespace) {
 		if (_namespaces.contains(namespace)) return;
 		int pos = namespace.lastIndexOf(".");
 		if (pos == -1) {
 			_namespaces.add(namespace);
 			return;
 		}
 		
 		_namespaces.add(namespace.substring(pos + 1));
 		registerNamespace(namespace.substring(0, pos));
 	}
 
 	private String qualifiedName(ITypeBinding type) {
 		return Bindings.qualifiedName(type);
 	}
 
 	private boolean shouldPrefixInterface(ITypeBinding type) {
 		return _configuration.nativeInterfaces()
 			&& type.isInterface()
 			&& !hasMapping(type);
 	}
 
 	private boolean hasMapping(ITypeBinding type) {
 		return _configuration.typeHasMapping(typeMappingKey(type));
 	}
 
 	private String interfaceName(String name) {
 		return "I" + name;
 	}
 	
 	private String mappedTypeName(String typeName) {
 		return mappedTypeName(typeName, typeName);
 	}
 
 	private String mappedTypeName(String typeName, String defaultValue) {
 		return _configuration.mappedTypeName(typeName, defaultValue);
 	}
 	
 	private String renamedName(BodyDeclaration node) {
 		TagElement renameTag = JavadocUtility.getJavadocTag(node, Annotations.SHARPEN_RENAME);
 		if (null == renameTag) return null;
 		return getSingleTextFragment(renameTag);
 	}
 	
 	protected String mappedMethodName(MethodDeclaration node) {
 		String newName = renamedName(node);
 		if (null != newName) return newName;
 		
 		IMethodBinding original = originalMethodBinding(node.resolveBinding());
 		return mappedMethodName(original);
 	}
 
 	protected String mappedMethodName(IMethodBinding binding) {		
 		Configuration.MemberMapping mapping = methodMapping(binding);
 		return mappedMethodName(binding, mapping);
 	}
 
 	private String mappedMethodName(IMethodBinding binding, Configuration.MemberMapping mapping) {
 		if (isStaticVoidMain(binding)) return "Main";
 		String name = null != mapping && null != mapping.name ? mapping.name : binding.getName();
 		return methodName(name);
 	}
 
 	private Configuration.MemberMapping methodMapping(final IMethodBinding binding) {
 		IMethodBinding actual = originalMethodBinding(binding);
 		
 		MemberMapping mapping = _configuration.mappedMember(Bindings.qualifiedSignature(actual));
 		if (null != mapping) return mapping;
 		
 		mapping = _configuration.mappedMember(qualifiedName(actual));
 		if (null != mapping) return mapping;
 		
 		final MethodDeclaration declaring = (MethodDeclaration)findDeclaringNode(actual);
 		if (null == declaring) return null;
 		
 		if (!(isProperty(declaring) || isEvent(declaring))) return null;
 		
 		return new MemberMapping(propertyName(declaring), MemberKind.Property);
 	}
 
 	private String qualifiedName(IMethodBinding actual) {
 		return Bindings.qualifiedName(actual);
 	}
 
 	private boolean isEvent(MethodDeclaration declaring) {
 		MethodDeclaration original = findOriginalMethodDeclaration(declaring);
 		if (null != original) return containsEventTag(original);
 		return containsEventTag(declaring);
 	}
 
 	private boolean containsEventTag(MethodDeclaration original) {
 		return null != getEventTag(original);
 	}
 
 	private String methodName(String name) {
 		return namingStrategy().methodName(name);
 	}
 
 	protected String identifier(SimpleName name) {
 		return identifier(name.toString());
 	}
 
 	protected String identifier(String name) {
 		return namingStrategy().identifier(name);
 	}
 	
 	private String qualifiedName(IVariableBinding binding) {		
 		ITypeBinding declaringClass = binding.getDeclaringClass();
 		
 		if (null == declaringClass) {
 			return binding.getName();
 		}
 		return qualifiedName(declaringClass) + "." + binding.getName();
 	}
 	
 	private String mappedQualifiedName(IBinding binding) {
 		switch (binding.getKind()) {
 		case IBinding.METHOD:
 			return mappedQualifiedMethodName((IMethodBinding)binding);
 		case IBinding.TYPE:
 			return mappedTypeName((ITypeBinding)binding);
 		case IBinding.VARIABLE:
 			return mappedQualifiedFieldName((IVariableBinding)binding);
 		case IBinding.PACKAGE:
 			return mappedNamespace(((IPackageBinding)binding).getName());
 		}
 		throw new IllegalArgumentException("Binding type not supported: " + binding);
 	}
 
 	private String mappedNamespace(String namespace) {
 		return _configuration.mappedNamespace(namespace);
 	}
 	
 	private String mappedQualifiedFieldName(IVariableBinding binding) {
 		String name = mappedFieldName(binding);
 		if (null != name) return name;
 		return mappedTypeName(binding.getDeclaringClass()) + "." + identifier(binding.getName());
 	}
 
 	private String mappedQualifiedMethodName(IMethodBinding binding) {
 		String methodName = mappedMethodName(binding);
 		if (methodName.indexOf('.') > -1) {
 			return methodName;
 		}
 		return mappedTypeName(binding.getDeclaringClass()) + "." + methodName;
 	}
 
 	private boolean isStaticVoidMain(IMethodBinding binding) {
 		return isStatic(binding)
 				&& "main".equals(binding.getName());
 	}
 
 	private boolean isStatic(IMethodBinding binding) {
 		return Modifier.isStatic(binding.getModifiers());
 	}
 
 	private void unresolvedTypeBinding(ASTNode node) {
 		warning(node, "unresolved type binding for node: " + node);
 	}
 	
 	public boolean visit(CompilationUnit node) {
 		return true;
 	}
 
 	private void warning(ASTNode node, String message) {
 		warningHandler().warning(node, message);
 	}
 	
 	protected String sourceInformation(ASTNode node) {
 		return ASTUtility.sourceInformation(_ast, node);
 	}
 
 	@SuppressWarnings("deprecation")
 	protected int lineNumber(ASTNode node) {
 		return _ast.lineNumber(node.getStartPosition());
 	}
 
 	public void setASTResolver(ASTResolver resolver) {
 		_resolver = resolver;
 	}
 }
