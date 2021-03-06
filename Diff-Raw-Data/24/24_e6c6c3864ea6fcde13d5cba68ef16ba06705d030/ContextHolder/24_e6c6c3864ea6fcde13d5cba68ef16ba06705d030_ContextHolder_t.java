 /*
  * This software is licensed under the terms of the GNU GENERAL PUBLIC LICENSE
  * Version 2, which can be found at http://www.gnu.org/copyleft/gpl.html
 */
 package org.cubictest.exporters.selenium.utils;
 
import static org.cubictest.model.IdentifierType.LABEL;

 import java.util.Stack;
 
 import org.cubictest.export.IResultHolder;
 import org.cubictest.model.IdentifierType;
 import org.cubictest.model.context.AbstractContext;
 import org.cubictest.model.context.IContext;
 import org.cubictest.model.formElement.Select;
 
 /**
  * Holder for context info for runner and exporter.
  * 
  * @author Christian Schwarz
  */
 public class ContextHolder implements IResultHolder {
 
 	/** The XPath starting point (path) for lookup of elements. */
 	private Stack<String> context = new Stack<String>(); 
 
 	
 	public ContextHolder() {
 		//Setting default context to be anywhere in the document ("//" in XPath)
 		context.push("//");		
 	}
 	
 	
 	/**
 	 * Set new context, i.e. XPath starting point (path) for lookup of elements.
 	 * @param ctx
 	 */
 	public void pushContext(IContext ctx) {
 		if (ctx instanceof Select) {
 			Select select = (Select) ctx;
			if (select.getMainIdentifierType().equals(LABEL)) {
				context.push("select[@id=(//label[text()=\"" + select.getMainIdentifierValue() + "\"]/@for)]//");
			}
			else {
				String idType = SeleniumUtils.getIdType(select.getMainIdentifier());
				context.push("select[@" + idType + "=\"" + select.getMainIdentifierValue() + "\"]//");
			}
 		}
 		else if (ctx instanceof AbstractContext) {
 			AbstractContext abstractContext = (AbstractContext) ctx;
 			String idType = SeleniumUtils.getIdType(abstractContext.getMainIdentifier());
 			context.push("*[@" + idType + "=\"" + abstractContext.getMainIdentifierValue() + "\"]//");
 		}
 	}
 
 	
 	/**
 	 * Get the previous context, i.e. the previous XPath starting point (path) for lookup of elements.
 	 */
 	public void popContext() {
 		context.pop();
 	}
 	
 	
 	/**
 	 * Get the currenct active context, appending all known contexts into an XPath string 
 	 * that can be used for lookup of elements.
 	 * @return 
 	 */
 	public String getFullContext() {
 		StringBuffer buff = new StringBuffer();
 		
 		//Concatenate known contexts to a single XPath line:
 		//First in First Out: 
 		for (String contextPart : context) {
 			buff.append(contextPart);
 		}
 		
 		//return the full, concatenated context:
 		return buff.toString();
 	}
 
 
 	public String toResultString() {
 		return "";
 	}
 	
 }
