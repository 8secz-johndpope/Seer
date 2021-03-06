 /*
  * Created on Apr 28, 2005
  *
  * This software is licensed under the terms of the GNU GENERAL PUBLIC LICENSE
  * Version 2, which can be found at http://www.gnu.org/copyleft/gpl.html
  * 
 */
 package org.cubictest.export.watir.delegates;
 
 import org.apache.commons.lang.StringUtils;
 import org.cubictest.common.converters.interfaces.IPageElementConverter;
 import org.cubictest.export.watir.TestStep;
 import org.cubictest.export.watir.interfaces.IStepList;
 import org.cubictest.export.watir.util.WatirUtils;
 import org.cubictest.model.FormElement;
 import org.cubictest.model.Link;
 import org.cubictest.model.PageElement;
 import org.cubictest.model.Text;
 import org.cubictest.model.Title;
 import org.cubictest.model.formElement.Button;
 import org.cubictest.model.formElement.Checkable;
 import org.cubictest.model.formElement.Option;
 import org.cubictest.model.formElement.Password;
 import org.cubictest.model.formElement.Select;
 import org.cubictest.model.formElement.TextArea;
 import org.cubictest.model.formElement.TextField;
 import static org.cubictest.model.IdentifierType.*;
 
 /**
  * This class is responsible for creating Watir steps for each element that can be checked for on a page.
  */
 public class PageElementConverter implements IPageElementConverter<IStepList> {	
 	
 	/**
 	 * Converts one element located on a page to a list of Watir Steps.
 	 * 
 	 * @param pe The Page element to convert.
 	 * @return An <code>java.util.List</code> with Watir Steps.
 	 */
 	public void handlePageElement(IStepList steps, PageElement pe) {
 		
 		if (pe instanceof Title) {
 			handleTitle(steps, (Title) pe);
 		}
 		else if (pe instanceof Link) {
 			handleLink(steps, (Link) pe);
 		}
 		else if (pe instanceof Text) {
 			handleText(steps, pe);
 		}
 		else if (pe instanceof FormElement) {
 			handleFormElement(steps, (FormElement) pe);
 		}
 	}
 	
 	
 	/**
 	 * Creates a test step for verifying that the specified title is present.
 	 * @param title The title to check for.
 	 * @return A test step
 	 */
 	private void handleTitle(IStepList steps, Title title) {
 		String titleText = StringUtils.replace(title.getText(),"\"", "\\\"");
 		String verify = "assert_equal(\"" + titleText + "\"," + steps.getPrefix() + ".title())";
 		
 		steps.add(new TestStep(verify).setDescription("Check title = '" + titleText + "'"));
 	}
 	
 
 	/**
 	 * Creates a Watir ITestElement testing for the presence of the given Text.
 	 * Supports contexts.
 	 * 
 	 * @param text The text to check for.
 	 * @return The Watir Step.
 	 */
 	private void handleText(IStepList steps, PageElement text) {
 		String txt = StringUtils.replace(text.getText(),"\"", "\\\"");
 		
 		StringBuffer str = new StringBuffer();
 		
 		append(str, "count = 0", 2);
 		
 		if (text.isNot()){
 			append(str, "i = 0", 2);
 			append(str, "while i != nil and count < 20 do", 2);
 			append(str, "begin", 3);
 			append(str, "i = " + steps.getPrefix() + ".text.index(\"" + text.getText() + "\")", 4);
 			append(str, "if(i != nil)", 4);
 			append(str, "raise \"string found exception\"", 5);
 			append(str, "end", 4);
 		} else {
 			append(str, "i = nil", 2);
 			append(str, "while i == nil and count < 20 do", 2);
 			append(str, "begin", 3);
 			append(str, "i = "+ steps.getPrefix() +".text.index(\"" + text.getText() + "\")", 4);
 			append(str, "if(i == nil)", 4);
 			append(str, "raise \"string NOT found exception\"", 5);
 			append(str, "end", 4);
 		}
 		append(str, "passedSteps += 1 ", 4);
 		append(str, "rescue => e", 3);
 		append(str, "count += 1", 4);
 		append(str, "sleep 0.1", 4);
 		append(str, "if ( count >= 20 ) then", 4);
 		append(str, "failedSteps += 1 ", 5);
 		String prefixEsc = StringUtils.replace(steps.getPrefix(),"\"", "\\\"");
 		String ctxMessage = "";
 		if (!prefixEsc.equalsIgnoreCase("ie")) {
 			ctxMessage = "(context: '" + prefixEsc + "')";
 		}
 		if (text.isNot()){
 			append(str, "puts \"Step failed: Check text NOT present: '"+ txt + "' " + ctxMessage + "\"", 5);
 		} else {
 			append(str, "puts \"Step failed: Check text present: '"+ txt + "' " + ctxMessage + "\"", 5);
 		}
 		append(str, "end", 4);
 		append(str, "end", 3);
 		append(str, "end", 2);
 		
 		TestStep step = new TestStep(str.toString());
 		step.setDecorated(true);
 
 		if (text.isNot())
 		{
 			step.setDescription("Check text NOT present: '" + txt + "', prefix " + steps.getPrefix());
 		}
 		else{
 			step.setDescription("Check text present: '" + txt + "', prefix: " + steps.getPrefix());
 		}
 		
 		steps.add(step);
 	}
 	
 	
 	/**
 	 * Creates Watir ITestElement checking for the presence of a link.
 	 * 
 	 * @param link The link to check for.
 	 * @return the Watir Step
 	 */
 	private void handleLink(IStepList steps, Link link) {
 		String idText = StringUtils.replace(link.getText(),"\"", "\\\"");
 		String idType = WatirUtils.getIdType(link);
 		
 		StringBuffer buff = new StringBuffer();
 		appendWaitStatement(buff, "link", idType, idText, steps.getPrefix());
 		append(buff, "assert_equal(\"" + link.getDescription() + "\", ie.link(" + idType + ", \"" + idText + "\").innerText())", 3);
 		TestStep step = new TestStep(buff.toString()).setDescription("Check link present with " + idType + " = '" + idText + "'");
 	
 		if (link.isNot())
 		{
			step = new TestStep("assert(!ie.link(" + idType + ", \"" + idText + "/\")).exists?)");
 			step.setDescription("Check link NOT present with " + idType + " = '" + idText + "'");
 		}
 		
 		steps.add(step);
 	}
 	
 
 	/**
 	 * Creates a test element verifying that the form element in the argument is present.
 	 */
 	private void handleFormElement(IStepList steps, FormElement fe) {
 		String elementType = WatirUtils.getElementType(fe);
 		String idType = WatirUtils.getIdType(fe);
 		String idText = fe.getText();
 		String value = "";
 
 		StringBuffer buff = new StringBuffer();
 
 		if (fe.getIdentifierType().equals(LABEL) && !(fe instanceof Button) && !(fe instanceof Option)) {
 			WatirUtils.appendGetLabelTargetId(buff, fe, fe.getDescription());
 			idText = "\" + labelTargetId + \"";
 			idType = ":id";
 		}
 		
 		if (fe instanceof TextField || fe instanceof Password || fe instanceof TextArea){
 			appendWaitStatement(buff, elementType, idType, idText, steps.getPrefix());
 			append(buff, "assert_equal(\"" + value + "\", " + steps.getPrefix() + "." + elementType + "(" + idType + ", \"" + idText + "\").getContents())", 3);
 		}
 		else if (fe instanceof Option) {
 			if (fe.getIdentifierType().equals(LABEL)) {
 				WatirUtils.appendCheckOptionPresent(buff, steps.getPrefix(), fe.getText());
 			}
 			else {
 				appendWaitStatement(buff, elementType, idType, idText, steps.getPrefix());
 				append(buff, steps.getPrefix() + "." + elementType + "(" + idType + ", \"" + idText + "\").to_s()", 3);
 			}
 		}
 		else if (fe instanceof Checkable){
 			value = ((Checkable)fe).isChecked() + "";
 			appendWaitStatement(buff, elementType, idType, idText, steps.getPrefix());
 			append(buff, "assert(" + steps.getPrefix() + "." + elementType + "(" + idType + ", \"" + idText + "\").checked? == " + value + ")", 3);
 		}
 		else if (fe instanceof Button) {
 			appendWaitStatement(buff, elementType, idType, idText, steps.getPrefix());
 			append(buff, steps.getPrefix() + "." + elementType + "(" + idType + ", \"" + idText + "\").to_s()", 3);
 		}
 		else if (fe instanceof Select){
 			appendWaitStatement(buff, elementType, idType, idText, steps.getPrefix());
 			append(buff, steps.getPrefix() + "." + elementType + "(" + idType + ", \"" + idText + "\").to_s()", 3);
 		}
 		
 		if (StringUtils.isNotBlank(buff.toString())) {
 			String desc = "Check " + fe.getType() + " present with " + idType + " = '" + idText + "'";
 			if (!(fe instanceof Option)) {
 				desc += " and value = '" + value + "'";
 			}
 			
 			TestStep step = new TestStep(buff.toString()).setDescription(desc);
 			if (fe instanceof Option) {
 				step.setDecorated(true); //do not wrap in retry logic
 			}
 		}
 }
 
 
 
 	
 	/**
 	 * @param element The element that are checked to be present. 
 	 * @param type The type of the value representing the element in Watir (name, value, etc)
 	 * @param value The value representing the element on the wep-page.
 	 * @return A Watir statement that will wait for the element, maximum 2 secounds. 
 	 */
 	private void appendWaitStatement(StringBuffer buff, String element, String type, String value, String prefix) {
 		
 		append(buff, "count = 0", 3);
 		append(buff, "while not " + prefix + "." + element + "(" + type + ", \"" + value + "\").exists? and count < 20 do", 3);
 		append(buff, "sleep 0.1", 4);
 		append(buff, "count += 1", 4);
 		append(buff, "end", 3);
 	}
 	
 	private void append(StringBuffer buff, String s, int indent) {
 		for (int i = 0; i < indent; i++) {
 			buff.append("\t");			
 		}
 		buff.append(s);
 		buff.append("\n");
 	}
 
 
 	
 }
