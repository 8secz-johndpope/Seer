 /***
  * Copyright 2013 Teoti Graphix, LLC.
  * 
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * 
  * http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  * 
  * 
  * @author Michael Schmalle <mschmalle@teotigraphix.com>
  */
 
 package randori.compiler.internal.codegen.js;
 
 import java.io.FilterWriter;
 import java.util.List;
 
 import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.flex.compiler.definitions.IConstantDefinition;
 import org.apache.flex.compiler.definitions.IDefinition;
 import org.apache.flex.compiler.definitions.IFunctionDefinition;
 import org.apache.flex.compiler.definitions.IPackageDefinition;
 import org.apache.flex.compiler.internal.tree.as.FunctionNode;
 import org.apache.flex.compiler.internal.tree.as.FunctionObjectNode;
 import org.apache.flex.compiler.problems.ICompilerProblem;
 import org.apache.flex.compiler.tree.ASTNodeID;
 import org.apache.flex.compiler.tree.as.IBinaryOperatorNode;
 import org.apache.flex.compiler.tree.as.IClassNode;
 import org.apache.flex.compiler.tree.as.IContainerNode;
 import org.apache.flex.compiler.tree.as.IDefinitionNode;
 import org.apache.flex.compiler.tree.as.IDynamicAccessNode;
 import org.apache.flex.compiler.tree.as.IExpressionNode;
 import org.apache.flex.compiler.tree.as.IForLoopNode;
 import org.apache.flex.compiler.tree.as.IFunctionCallNode;
 import org.apache.flex.compiler.tree.as.IFunctionNode;
 import org.apache.flex.compiler.tree.as.IFunctionObjectNode;
 import org.apache.flex.compiler.tree.as.IIdentifierNode;
 import org.apache.flex.compiler.tree.as.ILanguageIdentifierNode;
 import org.apache.flex.compiler.tree.as.ILiteralNode;
 import org.apache.flex.compiler.tree.as.ILiteralNode.LiteralType;
 import org.apache.flex.compiler.tree.as.IMemberAccessExpressionNode;
 import org.apache.flex.compiler.tree.as.IPackageNode;
 import org.apache.flex.compiler.tree.as.IParameterNode;
 import org.apache.flex.compiler.tree.as.ITypeNode;
 import org.apache.flex.compiler.tree.as.IVariableNode;
 
 import randori.compiler.codegen.js.IRandoriEmitter;
 import randori.compiler.codegen.js.ISessionModel;
 import randori.compiler.internal.codegen.js.emitter.BinaryOperatorEmitter;
 import randori.compiler.internal.codegen.js.emitter.DynamicAccessEmitter;
 import randori.compiler.internal.codegen.js.emitter.FieldEmitter;
 import randori.compiler.internal.codegen.js.emitter.FooterEmitter;
 import randori.compiler.internal.codegen.js.emitter.FunctionCallEmitter;
 import randori.compiler.internal.codegen.js.emitter.HeaderEmitter;
 import randori.compiler.internal.codegen.js.emitter.IdentifierEmitter;
 import randori.compiler.internal.codegen.js.emitter.MemberAccessExpressionEmitter;
 import randori.compiler.internal.codegen.js.emitter.MethodEmitter;
 import randori.compiler.internal.utils.DefinitionUtils;
 import randori.compiler.internal.utils.MetaDataUtils;
 
 /**
  * The base ship...
  * 
  * @author Michael Schmalle
  */
 public class RandoriEmitter extends JSEmitter implements IRandoriEmitter
 {
     @Override
     public List<ICompilerProblem> getProblems()
     {
         return super.getProblems();
     }
 
     //--------------------------------------------------------------------------
     // Properties
     //--------------------------------------------------------------------------
 
     //----------------------------------
     // model
     //----------------------------------
 
     private final ISessionModel model;
 
     @Override
     public final ISessionModel getModel()
     {
         return model;
     }
 
     //--------------------------------------------------------------------------
     // Emitters
     //--------------------------------------------------------------------------
 
     private IdentifierEmitter identifier;
 
     private MemberAccessExpressionEmitter memberAccessExpression;
 
     private DynamicAccessEmitter dynamicAccessEmitter;
 
     private BinaryOperatorEmitter binaryOperator;
 
     private MethodEmitter method;
 
     private FieldEmitter field;
 
     private FunctionCallEmitter functionCall;
 
     private HeaderEmitter header;
 
     private FooterEmitter footer;
 
     //--------------------------------------------------------------------------
     // Constructor
     //--------------------------------------------------------------------------
 
     public RandoriEmitter(FilterWriter out)
     {
         super(out);
 
         model = new SessionModel();
 
         createEmitters();
     }
 
     //--------------------------------------------------------------------------
     // Protected :: Methods
     //--------------------------------------------------------------------------
 
     /**
      * Creates the sub emitters.
      */
     protected void createEmitters()
     {
         method = new MethodEmitter(this);
         field = new FieldEmitter(this);
 
         identifier = new IdentifierEmitter(this);
         memberAccessExpression = new MemberAccessExpressionEmitter(this);
         dynamicAccessEmitter = new DynamicAccessEmitter(this);
         binaryOperator = new BinaryOperatorEmitter(this);
         functionCall = new FunctionCallEmitter(this);
 
         header = new HeaderEmitter(this);
         footer = new FooterEmitter(this);
     }
 
     //--------------------------------------------------------------------------
     // Overridden Public :: Methods
     //--------------------------------------------------------------------------
 
     @Override
     public void emitPackageHeader(IPackageDefinition definition)
     {
         // TODO (mschmalle) emit package render comments
     }
 
     @Override
     public void emitPackageHeaderContents(IPackageDefinition definition)
     {
         IPackageNode node = definition.getNode();
         ITypeNode tnode = findTypeNode(node);
         if (!MetaDataUtils.isGlobal((IClassNode) tnode))
         {
             header.emit(definition);
         }
     }
 
     @Override
     public void emitPackageContents(IPackageDefinition definition)
     {
         IPackageNode node = definition.getNode();
         ITypeNode tnode = findTypeNode(node);
         if (tnode != null)
         {
             writeNewline();
             getWalker().walk(tnode); // IClassNode | IInterfaceNode
         }
     }
 
     @Override
     public void emitPackageFooter(IPackageDefinition definition)
     {
         IClassNode node = (IClassNode) findTypeNode(definition.getNode());
         if (node == null)
             return; // temp because of unit tests
 
         if (!MetaDataUtils.isGlobal(node))
         {
             footer.emit(node);
         }
     }
 
     @Override
     public void emitClass(IClassNode node)
     {
         // fields, methods
         final IDefinitionNode[] members = node.getAllMemberNodes();
         if (members.length > 0)
         {
             if (!MetaDataUtils.isGlobal(node))
             {
                 IFunctionDefinition constructor = node.getDefinition()
                         .getConstructor();
                 IFunctionNode cnode = (IFunctionNode) constructor.getNode();
                 if (cnode != null)
                 {
                     method.emit(cnode);
                 }
                 else
                 {
                     method.emitConstructor(constructor);
                 }
 
                 writeNewline(";");
             }
         }
 
         if (members.length > 0)
         {
             writeNewline();
 
             final int len = members.length;
             int i = 0;
             for (IDefinitionNode member : members)
             {
                 IDefinition definition = member.getDefinition();
 
                 if (member.getNodeID() == ASTNodeID.FunctionID)
                 {
                     if (((IFunctionDefinition) definition).isConstructor())
                         continue;
                 }
 
                 if (member.getNodeID() == ASTNodeID.VariableID)
                 {
                     model.addInjection(definition);
                     model.addViewInjection(definition);
 
                    if (definition.isStatic()
                            || definition instanceof IConstantDefinition)
                     {
                         getWalker().walk(member);
 
                         write(";");
                         writeNewline();
                         writeNewline();
                     }
                 }
                 else if (member.getNodeID() == ASTNodeID.FunctionID)
                 {
                     model.addInjection(definition);
 
                     getWalker().walk(member);
 
                     write(";");
                     writeNewline();
                     writeNewline();
                 }
                 else if (member.getNodeID() == ASTNodeID.GetterID
                         || member.getNodeID() == ASTNodeID.SetterID)
                 {
                     model.addInjection(definition);
                     model.addViewInjection(definition);
 
                     getWalker().walk(member);
 
                     write(";");
                     if (i < len - 1)
                     {
                         writeNewline();
                         writeNewline();
                     }
                 }
                 i++;
             }
         }
     }
 
     @Override
     public void emitField(IVariableNode node)
     {
         field.emit(node);
     }
 
     @Override
     public void emitFunctionBlockHeader(IFunctionNode node)
     {
         method.emitHeader(node);
     }
 
     @Override
     public void emitMethod(IFunctionNode node)
     {
         method.emit(node);
     }
 
     @Override
     public void emitFunctionObject(IFunctionObjectNode node)
     {
         FunctionObjectNode f = (FunctionObjectNode) node;
         FunctionNode fnode = f.getFunctionNode();
         //write(IRandoriEmitter.ANON_DELEGATE_NAME);
         //write("(");
         //write("this, ");
         write("function");
         emitParamters(fnode.getParameterNodes());
         emitType(fnode.getTypeNode());
         emitFunctionScope(fnode.getScopedNode());
         //write(")");
     }
 
     @Override
     public void emitFunctionCall(IFunctionCallNode node)
     {
         functionCall.emit(node);
     }
 
     @Override
     public void emitParameter(IParameterNode node)
     {
         getWalker().walk(node.getNameExpressionNode());
     }
 
     @Override
     protected void walkArguments(IExpressionNode[] nodes)
     {
     }
 
     @Override
     public void emitForEachLoop(IForLoopNode node)
     {
         IContainerNode conditionalNode = node.getConditionalsContainerNode();
         IContainerNode containerNode = (IContainerNode) node.getChild(1);
 
         writeNewline("var $1;");
         writeToken("for");
         write("(var $0");
         IBinaryOperatorNode bnode = (IBinaryOperatorNode) conditionalNode
                 .getChild(0);
         write(" in ($1 = ");
         getWalker().walk(bnode.getRightOperandNode());
         write("))");
 
         if (!isImplicit(containerNode))
             write(" ");
 
         final int len = node.getStatementContentsNode().getChildCount();
         if (len > 0)
             writeNewline("{", true);
         else
             writeNewline("{");
 
         for (int i = 0; i < len; i++)
         {
             if (i == 0)
             {
                 getWalker().walk(bnode.getLeftOperandNode());
                 writeNewline(" = $1[$0];");
             }
 
             getWalker().walk(node.getStatementContentsNode().getChild(i));
             if (i < len - 1)
                 writeNewline();
         }

         if (len > 0)
         {
             indentPop();
             writeNewline();
         }

         write("}");
     }
 
     @Override
     public void emitAsOperator(IBinaryOperatorNode node)
     {
         getWalker().walk(node.getLeftOperandNode());
     }
 
     @Override
     public void emitIsOperator(IBinaryOperatorNode node)
     {
         getWalker().walk(node.getLeftOperandNode());
         write(" instanceof ");
         getWalker().walk(node.getRightOperandNode());
     }
 
     @Override
     public void emitBinaryOperator(IBinaryOperatorNode node)
     {
         binaryOperator.emit(node);
     }
 
     @Override
     public void emitMemberAccessExpression(IMemberAccessExpressionNode node)
     {
         memberAccessExpression.emit(node);
     }
 
     @Override
     public void emitDynamicAccess(IDynamicAccessNode node)
     {
         dynamicAccessEmitter.emit(node);
     }
 
     @Override
     public void emitIdentifier(IIdentifierNode node)
     {
         identifier.emit(node);
     }
 
     @Override
     protected void emitType(IExpressionNode node)
     {
     }
 
     @Override
     public void emitLanguageIdentifier(ILanguageIdentifierNode node)
     {
         if (node.getKind() == ILanguageIdentifierNode.LanguageIdentifierKind.ANY_TYPE)
         {
         }
         else if (node.getKind() == ILanguageIdentifierNode.LanguageIdentifierKind.REST)
         {
         }
         else if (node.getKind() == ILanguageIdentifierNode.LanguageIdentifierKind.SUPER)
         {
             IIdentifierNode inode = (IIdentifierNode) node;
             if (inode.getParent() instanceof IMemberAccessExpressionNode)
             {
                 // emitFunctionCall() takes care of super.foo()
             }
             else
             {
                 IClassNode typeNode = (IClassNode) DefinitionUtils
                         .findTypeNode(inode.getParent());
                 String qualifiedName = DefinitionUtils
                         .toBaseClassQualifiedName(typeNode.getDefinition(),
                                 getWalker().getProject());
                 write(qualifiedName + ".call");
             }
         }
         else if (node.getKind() == ILanguageIdentifierNode.LanguageIdentifierKind.THIS)
         {
             IIdentifierNode inode = (IIdentifierNode) node;
             if (!(inode.getParent() instanceof IMemberAccessExpressionNode))
                 write("this");
         }
         else if (node.getKind() == ILanguageIdentifierNode.LanguageIdentifierKind.VOID)
         {
         }
     }
 
     @Override
     public void emitLiteral(ILiteralNode node)
     {
         String value = node.getValue(false);
         if (node.getLiteralType() == LiteralType.STRING)
         {
             value = "\"" + StringEscapeUtils.escapeEcmaScript(value) + "\"";
         }
         else if (node.getLiteralType() == LiteralType.REGEXP)
         {
             value = node.getValue(true);
         }
         write(value);
     }
 
     @Override
     public String toNodeString(IExpressionNode node)
     {
         return stringifyNode(node);
     }
 
     @Override
     public void emitParamters(IFunctionNode node)
     {
         emitParamters(node.getParameterNodes());
     }
 
     @Override
     public void emitMethodScope(IFunctionNode node)
     {
         emitMethodScope(node.getScopedNode());
     }
 }
