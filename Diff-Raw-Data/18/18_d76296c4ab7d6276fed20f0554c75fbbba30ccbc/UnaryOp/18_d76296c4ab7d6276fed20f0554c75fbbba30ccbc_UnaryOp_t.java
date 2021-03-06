 package de.fuberlin.projecta.analysis.ast;
 
 import de.fuberlin.commons.lexer.TokenType;
 import de.fuberlin.projecta.analysis.SemanticException;
 
 public class UnaryOp extends Expression {
 	
 	TokenType op;
 	
 	public UnaryOp(TokenType op){
 		this.op = op;
 	}
 
 	public Expression getExpression() {
 		return (Expression)this.getChild(0);
 	}
 
 	public TokenType getOp() {
 		return this.op;
 	}
 
 	@Override
 	public void checkTypes() {
 		String type = getExpression().toTypeString();
 		switch (this.op) {
 		case OP_NOT:
 			if (!(type.equals(BasicType.TYPE_BOOL_STRING)))
 				throw new SemanticException("Invalid operand to NOT: " + type);
			break;
 		case OP_MINUS:
 			if (!type.equals(BasicType.TYPE_INT_STRING) || !type.equals(BasicType.TYPE_REAL_STRING))
 				throw new SemanticException("Invalid operand to MINUS: " + type);
			break;
		default:
			break;
 		}
 	}
 	
 	@Override
 	public String toTypeString(){
 		return getExpression().toTypeString();
 	}
 }
