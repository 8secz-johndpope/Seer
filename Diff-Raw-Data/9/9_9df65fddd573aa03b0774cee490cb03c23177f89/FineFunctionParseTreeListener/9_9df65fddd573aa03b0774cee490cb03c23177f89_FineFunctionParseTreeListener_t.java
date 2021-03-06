 package main.FineFunctionParser;
 
 import java.util.Iterator;
 import java.util.List;
 
 import org.antlr.v4.runtime.ParserRuleContext;
 
 import main.CommonParser;
 import main.codeitems.declarations.IdentifierDecl;
 import main.codeitems.declarations.builders.IdentifierDeclBuilder;
 import main.codeitems.functionContent.builders.FineFunctionContentBuilder;
 
 import antlr.FineFunctionGrammarParser;
 import antlr.FineFunctionGrammarBaseListener;
 import antlr.FineFunctionGrammarParser.Init_declarator_listContext;
 import antlr.FineFunctionGrammarParser.Type_nameContext;
 
 public class FineFunctionParseTreeListener extends FineFunctionGrammarBaseListener
 {
 	CommonParser p;
 	
 	FineFunctionParseTreeListener(CommonParser aP)
 	{
 		p = aP;
 	}
 	
 	@Override
 	public void enterStatements(FineFunctionGrammarParser.StatementsContext ctx)
 	{
 		FineFunctionContentBuilder builder = new FineFunctionContentBuilder();
 		builder.createNew(ctx);
 		p.itemStack.push(builder);
 	}
 	
 	@Override
 	public void exitStatements(FineFunctionGrammarParser.StatementsContext ctx)
 	{
 		FineFunctionContentBuilder builder = (FineFunctionContentBuilder) p.itemStack.peek();
 		builder.exitStatements(ctx);
 		// p.processor.processItem(builder.getItem(), p.itemStack);
 	}
 	
 	@Override public void enterStatement(FineFunctionGrammarParser.StatementContext ctx)
 	{
 		FineFunctionContentBuilder builder = (FineFunctionContentBuilder) p.itemStack.peek();
 		builder.enterStatement(ctx);
 	}
 	
 	@Override public void exitStatement(FineFunctionGrammarParser.StatementContext ctx)
 	{	
 		FineFunctionContentBuilder builder = (FineFunctionContentBuilder) p.itemStack.peek();
 		builder.exitStatement(ctx);
 	}
 	
 	@Override public void enterAssign_expr(FineFunctionGrammarParser.Assign_exprContext ctx)
 	{
 		FineFunctionContentBuilder builder = (FineFunctionContentBuilder) p.itemStack.peek();
 		builder.enterAssignment(ctx);
 	}
 	
 	@Override public void enterElse_statement(FineFunctionGrammarParser.Else_statementContext ctx)
 	{
 		FineFunctionContentBuilder builder = (FineFunctionContentBuilder) p.itemStack.peek();
 		builder.enterElse(ctx);
 	}
 	
 	@Override public void enterIf_statement(FineFunctionGrammarParser.If_statementContext ctx)
 	{
 		FineFunctionContentBuilder builder = (FineFunctionContentBuilder) p.itemStack.peek();
 		builder.enterIf(ctx);
 	}
 	
 	
 	@Override
 	public void enterBlock_starter(FineFunctionGrammarParser.Block_starterContext ctx)
 	{
 		FineFunctionContentBuilder builder = (FineFunctionContentBuilder) p.itemStack.peek();
 		builder.enterBlockStarter(ctx);
 	}
 	
 	@Override
 	public void enterOpening_curly(FineFunctionGrammarParser.Opening_curlyContext ctx)
 	{
 		FineFunctionContentBuilder builder = (FineFunctionContentBuilder) p.itemStack.peek();
 		builder.enterOpeningCurly(ctx);
 	}
 	
 	@Override
 	public void enterClosing_curly(FineFunctionGrammarParser.Closing_curlyContext ctx)
 	{
 		FineFunctionContentBuilder builder = (FineFunctionContentBuilder) p.itemStack.peek();
 		builder.enterClosingCurly(ctx);
 	}
 	
 	@Override
 	public void enterDeclByType(FineFunctionGrammarParser.DeclByTypeContext ctx)
 	{
 		FineFunctionContentBuilder builder = (FineFunctionContentBuilder) p.itemStack.peek();
 		builder.enterDeclByType();
 		
 		Init_declarator_listContext decl_list = ctx.init_declarator_list();
 		Type_nameContext typeName = ctx.type_name();
 		emitDeclarations(decl_list, typeName);
 	}
 	
 	private void emitDeclarations(ParserRuleContext decl_list,
 			  ParserRuleContext typeName)
 	{
 		IdentifierDeclBuilder builder = new IdentifierDeclBuilder();
 		List<IdentifierDecl> declarations = builder.getDeclarations(decl_list, typeName);
 
 		FineFunctionContentBuilder contentBuilder = (FineFunctionContentBuilder) p.itemStack.peek();
 		
 		Iterator<IdentifierDecl> it = declarations.iterator();
 		while(it.hasNext()){
 			IdentifierDecl decl = it.next();
 			contentBuilder.addLocalDecl(decl);
 		}		
 	}
 	
 	
 }
