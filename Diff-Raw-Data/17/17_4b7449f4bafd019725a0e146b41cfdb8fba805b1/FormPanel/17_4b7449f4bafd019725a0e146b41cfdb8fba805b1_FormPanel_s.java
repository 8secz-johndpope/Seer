 package org.uva.sea.ql.gui;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.List;
 import javax.swing.JFrame;
 import javax.swing.JPanel;
 import org.uva.sea.ql.core.dom.Identifier;
 import org.uva.sea.ql.core.dom.statements.Form;
 import org.uva.sea.ql.core.dom.visitors.ExpressionVisitorToSetTypeOfIdentifiers;
 import org.uva.sea.ql.core.dom.visitors.StatementVisitorForRendering;
 import org.uva.sea.ql.core.dom.visitors.StatementVisitorToCheckVariableDefinitions;
 import org.uva.sea.ql.core.dom.visitors.StatementVisitorToSetTypesOfIdentifiers;
 import org.uva.sea.ql.parsers.FormParser;
 import org.uva.sea.ql.parsers.exceptions.ParseException;
 import org.uva.sea.ql.parsers.exceptions.QLException;
 
 public class FormPanel extends JFrame {
 
 	private static final long serialVersionUID = 1L;
 
 	public FormPanel() {
 	}
 
 	public static void main(String[] args) throws IOException {
 		FormPanel rootFrame = new FormPanel();
 		try {
 			String filePath = "C:\\Tubis\\School\\Software Construction\\QLTest.txt";
 			FormParser parser = new FormParser();
 			Form rootNode = (Form)parser.parseFromFile(filePath);			
 			List<Identifier> identifiers = ValidateIdentifierDefinitions(rootNode);
 			FillTypesOfIdentifiers(rootNode, identifiers);
 			
 			JPanel rootPanel=new JPanel();
 			AddFormOnRootPanel(rootPanel,rootNode);
 			
 			rootFrame.add(rootPanel);
 			
 		} catch (ParseException parseException) {
 			return;
 		}
 	}
 
 	private static List<Identifier> ValidateIdentifierDefinitions(Form form){
 		StatementVisitorToCheckVariableDefinitions statementVisitor = new StatementVisitorToCheckVariableDefinitions();
 		form.accept(statementVisitor);		
 		
 		List<QLException> statementExceptions= new ArrayList<QLException>(statementVisitor.getExceptions());
 		printException(statementExceptions);
 		
 		return statementVisitor.getIdentifierList();
 	}	
 	
 	private static void printException(List<QLException> exceptions) {		
 		for(QLException exception: exceptions){
 			System.out.println(exception.ToString());
 		}		
 	}
 	
 	private static void FillTypesOfIdentifiers(Form form, List<Identifier> identifiers){
 		ExpressionVisitorToSetTypeOfIdentifiers expressionVisitor=new ExpressionVisitorToSetTypeOfIdentifiers(identifiers);
 		StatementVisitorToSetTypesOfIdentifiers statementVisitor = new StatementVisitorToSetTypesOfIdentifiers(expressionVisitor);
 		
 		form.accept(statementVisitor);
 	}
 
 	private static void AddFormOnRootPanel(JPanel rootPanel,Form rootNode){
 		StatementVisitorForRendering statementVisitor=new StatementVisitorForRendering(rootPanel);
 		
 		rootNode.accept(statementVisitor);		
 	}
 }
