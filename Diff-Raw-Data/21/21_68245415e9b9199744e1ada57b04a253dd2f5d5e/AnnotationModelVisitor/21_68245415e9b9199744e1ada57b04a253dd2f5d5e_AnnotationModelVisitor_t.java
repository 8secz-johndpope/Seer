 package com.redhat.ceylon.compiler.java.codegen;
 
 import static com.redhat.ceylon.compiler.typechecker.analyzer.Util.isBooleanFalse;
 import static com.redhat.ceylon.compiler.typechecker.analyzer.Util.isBooleanTrue;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import com.redhat.ceylon.compiler.typechecker.model.Class;
 import com.redhat.ceylon.compiler.typechecker.model.Declaration;
 import com.redhat.ceylon.compiler.typechecker.model.Method;
 import com.redhat.ceylon.compiler.typechecker.model.Parameter;
 import com.redhat.ceylon.compiler.typechecker.model.ProducedType;
 import com.redhat.ceylon.compiler.typechecker.model.TypeDeclaration;
 import com.redhat.ceylon.compiler.typechecker.model.Unit;
 import com.redhat.ceylon.compiler.typechecker.model.Value;
 import com.redhat.ceylon.compiler.typechecker.tree.NaturalVisitor;
 import com.redhat.ceylon.compiler.typechecker.tree.Node;
 import com.redhat.ceylon.compiler.typechecker.tree.Tree;
 import com.redhat.ceylon.compiler.typechecker.tree.Tree.AnyMethod;
 import com.redhat.ceylon.compiler.typechecker.tree.Tree.Term;
 import com.redhat.ceylon.compiler.typechecker.tree.Visitor;
 
 /**
  * Visitor which inspects annotation constructors.
  * 
  * @author tom
  */
 public class AnnotationModelVisitor extends Visitor implements NaturalVisitor {
     private java.util.List<AnnotationFieldName> fieldNames = new ArrayList<AnnotationFieldName>();
     /** The annotation constructor we are currently visiting */
     private AnyMethod annotationConstructor;
     /** The instantiation in the body of the constructor, or in its default parameters */
     private AnnotationInvocation instantiation;
     private List<AnnotationInvocation> nestedInvocations;
     private boolean spread;
     private boolean checkingArguments;
     private boolean checkingDefaults;
     private AnnotationTerm term;
     private boolean checkingInvocationPrimary;
     private CollectionLiteralAnnotationTerm elements;
     
     public AnnotationModelVisitor() {
         
     }
     
     private void push(AnnotationFieldName parameter) {
         fieldNames.add(parameter);
     }
     
     private AnnotationFieldName pop() {
         return fieldNames.remove(fieldNames.size()-1);
     }
     
     public Parameter parameter() {
         return fieldNames.get(fieldNames.size()-1).getAnnotationField();
     }
     
     @Override
     public void handleException(Exception e, Node node) {
         if (e instanceof RuntimeException) {
             throw (RuntimeException)e;
         } else {
             throw new RuntimeException(e);
         }
     }
     
     public static boolean isAnnotationConstructor(AnyMethod def) {
         return isAnnotationConstructor(def.getDeclarationModel());
     }
     
     public static boolean isAnnotationConstructor(Declaration def) {
         return def.isToplevel()
                 && def instanceof Method
                 && def.isAnnotation();
     }
 
     public static boolean isAnnotationClass(Tree.ClassOrInterface def) {
         return isAnnotationClass(def.getDeclarationModel());
     }
 
     public static boolean isAnnotationClass(Declaration declarationModel) {
         return (declarationModel instanceof Class)
                 && declarationModel.isAnnotation();
     }
     
     @Override
     public void visit(Tree.MethodDefinition d) {
         if (isAnnotationConstructor(d)) {
             annotationConstructor = d;
             instantiation = new AnnotationInvocation();
             instantiation.setConstructorDeclaration(d.getDeclarationModel());
             d.getDeclarationModel().setAnnotationConstructor(instantiation);
         }
         super.visit(d);
         if (isAnnotationConstructor(d)) {
             instantiation = null;
             annotationConstructor = null;
         }
     }
     
     @Override
     public void visit(Tree.MethodDeclaration d) {
         if (isAnnotationConstructor(d)
                 && d.getSpecifierExpression() != null) {
             annotationConstructor = d;
             instantiation = new AnnotationInvocation();
             instantiation.setConstructorDeclaration(d.getDeclarationModel());
             d.getDeclarationModel().setAnnotationConstructor(instantiation);
         }
         super.visit(d);
         if (isAnnotationConstructor(d)
                 && d.getSpecifierExpression() != null) {
             instantiation = null;
             annotationConstructor = null;
         }
     }
     
     @Override
     public void visit(Tree.Statement d) {
         if (annotationConstructor != null) {
             if (!(annotationConstructor instanceof Tree.MethodDefinition 
                         && d instanceof Tree.Return)
                     && d != annotationConstructor) {
                 d.addError("Annotation constructors may only contain a return statement");
             }
         }
         super.visit(d);
     }
     
     @Override
     public void visit(Tree.AnnotationList al) {
         // Ignore statements in annotation lists
     }
     
     @Override
     public void visit(Tree.Parameter p) {
         
         if (annotationConstructor != null) {
             AnnotationConstructorParameter acp = new AnnotationConstructorParameter();
             acp.setParameter(p.getParameterModel());
             instantiation.getConstructorParameters().add(acp);
             push(acp);
             //super.visit(p);
             Tree.SpecifierOrInitializerExpression defaultArgument = Decl.getDefaultArgument(p);
             if (defaultArgument != null) {
                 defaultedParameter(defaultArgument);
             }
             pop();
             Tree.ValueParameterDeclaration vp;
             
         }
         // Ignore statements in parameters
     }
     
     
     public void defaultedParameter(Tree.SpecifierOrInitializerExpression d) {
         if (annotationConstructor != null) {
             AnnotationConstructorParameter annotationConstructorParameter = 
                     instantiation.getConstructorParameters().get(
                             instantiation.getConstructorParameters().size()-1);
             
             Declaration t = d.getUnit().getTrueValueDeclaration();
             Declaration f = d.getUnit().getFalseValueDeclaration();
             Term term = d.getExpression().getTerm();
             if (term instanceof Tree.InvocationExpression) {
                 Tree.Primary primary = ((Tree.InvocationExpression)term).getPrimary();
                 if (primary instanceof Tree.BaseMemberOrTypeExpression
                         && (isAnnotationConstructor( ((Tree.BaseMemberOrTypeExpression)primary).getDeclaration())
                           || isAnnotationClass( ((Tree.BaseMemberOrTypeExpression)primary).getDeclaration()))) {
                     final AnnotationInvocation prevInstantiation = this.instantiation;
                     this.instantiation = new AnnotationInvocation();
                     if (isAnnotationConstructor( ((Tree.BaseMemberOrTypeExpression)primary).getDeclaration())) {
                         Method constructor = (Method)((Tree.BaseMemberOrTypeExpression)primary).getDeclaration();
                         instantiation.setConstructorDeclaration(constructor);
                         instantiation.getConstructorParameters().addAll(((AnnotationInvocation)constructor.getAnnotationConstructor()).getConstructorParameters());
                     }
                     checkingDefaults = true;
                     
                     super.visit(d);
                     
                     annotationConstructorParameter.setDefaultArgument(this.term);
                     this.term = null;
                     checkingDefaults = false;
                     this.instantiation = prevInstantiation;
                 } else {
                     errorDefaultedParameter(d);
                 }
             } else if (term instanceof Tree.Literal
                     || (term instanceof Tree.BaseMemberExpression
                     && (((Tree.BaseMemberExpression)term).getDeclaration().equals(t)
                         || ((Tree.BaseMemberExpression)term).getDeclaration().equals(f)
                         || ((Tree.BaseMemberExpression)term).getDeclaration().isParameter()
                         || Decl.isAnonCaseOfEnumeratedType((Tree.BaseMemberExpression)term)))) {
                 checkingDefaults = true;
                 
                 super.visit(d);
                 
                 annotationConstructorParameter.setDefaultArgument(this.term);
                 this.term = null;
                 checkingDefaults = false;
             } else if (term instanceof Tree.Tuple
                     || term instanceof Tree.SequenceEnumeration) {
                 // TODO Tuples and SequenceEnumerations of the above cases should also be allowed
                 checkingDefaults = true;
                 
                 super.visit(d);
                 
                 annotationConstructorParameter.setDefaultArgument(this.term);
                 this.term = null;
                 checkingDefaults = false;
             } else {
                 errorDefaultedParameter(d);
             }
         }
     }
 
     private void errorDefaultedParameter(Node d) {
         d.addError("Only literals, true, false, and annotation class instantiations are permitted as annotation parameter defaults");
     }
     
     @Override
     public void visit(Tree.InvocationExpression invocation) {
         if (annotationConstructor != null) {
             
             final AnnotationInvocation prevInstantiation = this.instantiation;
             if (this.checkingArguments) {
                 this.instantiation = new AnnotationInvocation();
             }
             
             this.checkingInvocationPrimary = true;
             invocation.getPrimary().visit(this);
             this.checkingInvocationPrimary = false;
             
             if (invocation.getPositionalArgumentList() != null) {
                 invocation.getPositionalArgumentList().visit(this);
             }
             if (invocation.getNamedArgumentList() != null) {
                 invocation.getNamedArgumentList().visit(this);
             }
             InvocationAnnotationTerm invocationTerm = new InvocationAnnotationTerm();
             invocationTerm.setInstantiation(instantiation);
             if (this.checkingArguments) {
                 if (this.nestedInvocations == null) {
                     this.nestedInvocations = new ArrayList<AnnotationInvocation>();
                 }
                 this.nestedInvocations.add(this.instantiation);
                 this.instantiation = prevInstantiation;
             }
             this.term = invocationTerm;
         } else {
             super.visit(invocation);
         }
     }
     
     @Override
     public void visit(Tree.NamedArgumentList argumentList) {
         final boolean prevCheckingArguments = this.checkingArguments;
         this.checkingArguments = true;
         super.visit(argumentList);
         this.checkingArguments = prevCheckingArguments;
     }
     
     @Override
     public void visit(Tree.PositionalArgumentList argumentList) {
         final boolean prevCheckingArguments = this.checkingArguments;
         this.checkingArguments = true;
         super.visit(argumentList);
         this.checkingArguments = prevCheckingArguments;
     }
     
     public void visit(Tree.StringLiteral literal) {
         if (annotationConstructor != null) {
             if (checkingArguments || checkingDefaults){
                 LiteralAnnotationTerm argument = new StringLiteralAnnotationTerm(ExpressionTransformer.literalValue(literal));
                 argument.setTerm(literal);
                 appendLiteralArgument(literal, argument);
             }
         }
     }
     
     public void visit(Tree.CharLiteral literal) {
         if (annotationConstructor != null) {
             if (checkingArguments || checkingDefaults){
                 LiteralAnnotationTerm argument = new CharacterLiteralAnnotationTerm(ExpressionTransformer.literalValue(literal));
                 argument.setTerm(literal);
                 appendLiteralArgument(literal, argument);
             }
         }
     }
     
     public void visit(Tree.FloatLiteral literal) {
         if (annotationConstructor != null) {
             if (checkingArguments || checkingDefaults){
                 try {
                     LiteralAnnotationTerm argument = new FloatLiteralAnnotationTerm(ExpressionTransformer.literalValue(literal));
                     argument.setTerm(literal);
                     appendLiteralArgument(literal, argument);
                 } catch (ErroneousException e) {
                     // Ignore it: The ExpressionTransformer will produce an error later in codegen
                 }
             }
         }
     }
     
     public void visit(Tree.NaturalLiteral literal) {
         if (annotationConstructor != null) {
             if (checkingArguments || checkingDefaults){
                 try {
                    LiteralAnnotationTerm argument = new IntegerLiteralAnnotationTerm(ExpressionTransformer.literalValue(literal), parameter().getModel().getType());
                     argument.setTerm(literal);
                     appendLiteralArgument(literal, argument);
                 } catch (ErroneousException e) {
                     // Ignore it: The ExpressionTransformer will produce an error later in codegen
                 }
             }
         }
     }
     
     public void visit(Tree.NegativeOp op) {
         if (annotationConstructor != null) {
             if (checkingArguments || checkingDefaults){
                 try {
                     if (op.getTerm() instanceof Tree.NaturalLiteral) {
                        LiteralAnnotationTerm argument = new IntegerLiteralAnnotationTerm(ExpressionTransformer.literalValue(op), parameter().getModel().getType());
                         argument.setTerm(op);
                         appendLiteralArgument(op, argument);
                     } else if (op.getTerm() instanceof Tree.FloatLiteral) {
                         LiteralAnnotationTerm argument = new FloatLiteralAnnotationTerm(-ExpressionTransformer.literalValue((Tree.FloatLiteral)op.getTerm()));
                         argument.setTerm(op);
                         appendLiteralArgument(op, argument);
                     }
                 } catch (ErroneousException e) {
                     // Ignore it: The ExpressionTransformer will produce an error later in codegen
                 }
             }
         }
     }
     
     public void visit(Tree.MetaLiteral literal) {
         if (annotationConstructor != null) {
             if (checkingArguments || checkingDefaults){
                 LiteralAnnotationTerm argument = new DeclarationLiteralAnnotationTerm(ExpressionTransformer.getMetaLiteralDeclaration(literal));
                 argument.setTerm(literal);
                 appendLiteralArgument(literal, argument);
             }
         }
     }
     
     public void visit(Tree.Tuple literal) {
         if (annotationConstructor != null) {
             if (checkingArguments || checkingDefaults){
                 // Continue the visit to collect the elements
                 this.elements = new CollectionLiteralAnnotationTerm(null);
                 literal.visitChildren(this);
                 this.term = this.elements;
                 this.elements = null;
                 ((CollectionLiteralAnnotationTerm)this.term).setTerm(literal);
                 appendLiteralArgument(literal, (CollectionLiteralAnnotationTerm)term);
             }
         }
     }
     
     public void visit(Tree.SequenceEnumeration literal) {
         if (annotationConstructor != null) {
             if (checkingArguments || checkingDefaults){
                 // Continue the visit to collect the elements
                 Unit unit = literal.getUnit();
                 ProducedType iteratedType = unit.getIteratedType(literal.getTypeModel());
                 TypeDeclaration declaration = iteratedType.getDeclaration();
                 LiteralAnnotationTerm factory;
                 if (unit.getStringDeclaration().equals(declaration)) {
                     factory = new StringLiteralAnnotationTerm(null);
                 } else if (unit.getIntegerDeclaration().equals(declaration)) {
                    factory = new IntegerLiteralAnnotationTerm(0, null);
                 } else if (unit.getCharacterDeclaration().equals(declaration)) {
                     factory = new CharacterLiteralAnnotationTerm(0);
                 } else if (unit.getBooleanDeclaration().equals(declaration)) {
                     factory = new BooleanLiteralAnnotationTerm(false);
                 } else if (unit.getFloatDeclaration().equals(declaration)) {
                     factory = new FloatLiteralAnnotationTerm(0.0);
                 } else if (Decl.isEnumeratedTypeWithAnonCases(iteratedType)) {
                     factory = new ObjectLiteralAnnotationTerm(null);
                 } else {//if (iteratedType.isExactly(unit.getMetamodelDeclarationDeclaration().getType())) {
                     factory = new DeclarationLiteralAnnotationTerm(null);
                 } /*else {
                     throw new RuntimeException();
                 }*/
                 this.elements = new CollectionLiteralAnnotationTerm(factory);
                 literal.visitChildren(this);
                 this.term = this.elements;
                 this.elements = null;
                 ((CollectionLiteralAnnotationTerm)this.term).setTerm(literal);
                 appendLiteralArgument(literal, (CollectionLiteralAnnotationTerm)term);
             }
         }
     }
     
     @Override
     public void visit(Tree.Expression term) {
         if (annotationConstructor != null) {
             term.visitChildren(this);
         }
     }
     
     @Override
     public void visit(Tree.Term term) {
         if (annotationConstructor != null && !checkingDefaults) {
             term.addError("Unsupported term " + term.getClass().getSimpleName());
         }
     }
     
     @Override
     public void visit(Tree.BaseMemberExpression bme) {
         if (annotationConstructor != null) {
             Declaration declaration = bme.getDeclaration();
             if (checkingInvocationPrimary 
                     && isAnnotationConstructor(bme.getDeclaration())) {
                 Method ctor = (Method)bme.getDeclaration();
                 instantiation.setPrimary(ctor);
                 if (ctor.getAnnotationConstructor() != null) {
                     instantiation.getConstructorParameters().addAll(((AnnotationInvocation)ctor.getAnnotationConstructor()).getConstructorParameters());
                 }
             } else if (checkingArguments || checkingDefaults) {
                 if (declaration instanceof Value && ((Value)declaration).isParameter()) {
                     Value constructorParameter = (Value)declaration;
                     ParameterAnnotationTerm a = new ParameterAnnotationTerm();
                     a.setSpread(spread);
                     // XXX Is this right?
                     a.setSourceParameter(constructorParameter.getInitializerParameter());
                     this.term = a;
                 } else if (isBooleanTrue(declaration)) {
                     LiteralAnnotationTerm argument = new BooleanLiteralAnnotationTerm(true);
                     argument.setTerm(bme);
                     appendLiteralArgument(bme, argument);
                 } else if (isBooleanFalse(declaration)) {
                     LiteralAnnotationTerm argument = new BooleanLiteralAnnotationTerm(false);
                     argument.setTerm(bme);
                     appendLiteralArgument(bme, argument);
                 } else if (Decl.isAnonCaseOfEnumeratedType(bme)) {
                     LiteralAnnotationTerm argument = new ObjectLiteralAnnotationTerm(bme.getTypeModel());
                     argument.setTerm(bme);
                     appendLiteralArgument(bme, argument);
                 } else {
                     bme.addError("Unsupported base member expression in annotation constructor");
                 }
             } else {
                 bme.addError("Unsupported base member expression in annotation constructor");
             }
         }
     }
     
     /** 
      * Records <strong>either</strong> 
      * a literal argument to the annotation class instantiation:
      * <pre>
      *    ... => AnnotationClass("", 1, true, 1.0, 'x')
      * </pre>
      * <strong>Or</strong> a literal default argument in an annotation constructor:
      * <pre>
      *     AnnotationClass ctor(String s="", Integer i=1,
      *             Boolean b=true, Float f=1.0,
      *             Character c='x') => ...
      * </pre>
      * Literal is in the Javac sense.
      */
     private LiteralAnnotationTerm appendLiteralArgument(Node bme, LiteralAnnotationTerm argument) {
         if (spread) {
             bme.addError("Spread static arguments not supported");
         }
         if (this.elements != null) {
            this.elements.addElement(argument);
         } else {
             this.term = argument;
         }
         return argument;
     }
     
     @Override
     public void visit(Tree.BaseTypeExpression bte) {
         if (annotationConstructor != null) {
             if (isAnnotationClass(bte.getDeclaration())) {
                 instantiation.setPrimary((Class)bte.getDeclaration());
             } else {
                 bte.addError("Not an annotation class");
             }
         }
     }
     
     @Override
     public void visit(Tree.PositionalArgument argument) {
         if (annotationConstructor != null
                 && this.elements == null) {
             argument.addError("Unsupported positional argument");
         }
         super.visit(argument);
     }
     
     @Override
     public void visit(Tree.SpreadArgument argument) {
         if (annotationConstructor != null
                 && this.elements == null) {
             spread = true;
             argument.getExpression().visit(this);
             AnnotationArgument aa = new AnnotationArgument();
             aa.setParameter(argument.getParameter());
             aa.setTerm(this.term);
             instantiation.getAnnotationArguments().add(aa);
             this.term = null;
             spread = false;
         } else {
             super.visit(argument);
         }
     }
     
     @Override
     public void visit(Tree.ListedArgument argument) {
         if (annotationConstructor != null
                 && this.elements == null) {
             AnnotationArgument aa = new AnnotationArgument();
             aa.setParameter(argument.getParameter());
             push(aa);
             argument.getExpression().visit(this);
             aa.setTerm(this.term);
             instantiation.getAnnotationArguments().add(aa);
             this.term = null;
             pop();
         } else {
             super.visit(argument);
         }
     }
     
     @Override
     public void visit(Tree.NamedArgument argument) {
         if (annotationConstructor != null
                 && this.elements == null) {
             argument.addError("Unsupported named argument");
         }
         super.visit(argument);
     }
     
     @Override
     public void visit(Tree.SpecifiedArgument argument) {
         if (annotationConstructor != null
                 && this.elements == null) {
             AnnotationArgument aa = new AnnotationArgument();
             aa.setParameter(argument.getParameter());
             push(aa);
             argument.getSpecifierExpression().visit(this);
             
             aa.setTerm(this.term);
             instantiation.getAnnotationArguments().add(aa);
             this.term = null;
             pop();
         } else {
             super.visit(argument);
         }
     }   
 }
 
