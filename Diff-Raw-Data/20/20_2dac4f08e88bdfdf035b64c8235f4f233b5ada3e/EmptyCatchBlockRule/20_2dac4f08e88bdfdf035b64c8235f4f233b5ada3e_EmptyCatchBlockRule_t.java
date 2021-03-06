 /*
  * User: tom
  * Date: Jun 14, 2002
  * Time: 12:13:55 PM
  */
 package net.sourceforge.pmd.rules;
 
 import net.sourceforge.pmd.ast.ASTBlock;
 import net.sourceforge.pmd.ast.ASTTryStatement;
import net.sourceforge.pmd.ast.ASTBlockStatement;
import net.sourceforge.pmd.ast.SimpleNode;
 import net.sourceforge.pmd.*;
 
 public class EmptyCatchBlockRule extends AbstractRule implements Rule {
 
     public String getDescription() {return "Avoid empty catch blocks";}
 
    public Object visit(ASTTryStatement node, Object data){
       ASTBlock catchBlock = (ASTBlock)node.jjtGetChild(2);
       if (catchBlock.jjtGetNumChildren() == 0) {
            (((RuleContext)data).getReport()).addRuleViolation(new RuleViolation(this, node.getBeginLine()));
        }
         return super.visit(node, data);
     }
 }
