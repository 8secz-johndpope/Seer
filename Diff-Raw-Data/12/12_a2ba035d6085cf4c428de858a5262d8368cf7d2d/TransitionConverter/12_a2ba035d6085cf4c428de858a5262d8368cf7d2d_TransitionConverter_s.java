 /*******************************************************************************
  * Copyright (c) 2005, 2008 Christian Schwarz and Stein K. Skytteren
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *    Christian Schwarz and Stein K. Skytteren - initial API and implementation
  *******************************************************************************/
 package org.cubictest.exporters.selenium.runner.converters;
 
 import org.cubictest.common.utils.ErrorHandler;
 import org.cubictest.common.utils.Logger;
 import org.cubictest.export.converters.ITransitionConverter;
 import org.cubictest.export.exceptions.ExporterException;
 import org.cubictest.export.exceptions.UserInteractionException;
 import org.cubictest.exporters.selenium.runner.holders.CubicTestLocalRunner;
 import org.cubictest.exporters.selenium.runner.holders.SeleniumHolder;
 import org.cubictest.exporters.selenium.utils.SeleniumUtils;
 import org.cubictest.model.ActionType;
 import org.cubictest.model.IActionElement;
 import org.cubictest.model.PageElement;
 import org.cubictest.model.TestPartStatus;
 import org.cubictest.model.UserInteraction;
 import org.cubictest.model.UserInteractionsTransition;
 import org.cubictest.model.WebBrowser;
 import org.cubictest.model.context.Frame;
 import org.cubictest.model.formElement.Option;
 import org.cubictest.model.formElement.Select;
 
 /**
  * Class to convert transitions to selenium commands.
  * 
  * @author chr_schwarz
  */
 public class TransitionConverter implements ITransitionConverter<SeleniumHolder> {
 	
 	
 	/**
 	 * Converts a user interactions transition to a list of Selenium commands.
 	 * 
 	 * @param transition The transition to convert.
 	 */
 	public void handleUserInteractions(SeleniumHolder seleniumHolder, UserInteractionsTransition transition) {
 		
 		for (UserInteraction action : transition.getUserInteractions()) {
 			IActionElement actionElement = action.getElement();
 			
 			if (actionElement == null) {
 				Logger.warn("Action element was null. Skipping user interaction: " + action);
 				continue;
 			}
 			
 			handleUserInteraction(seleniumHolder, action);
 			
 			//increment the number of steps in test:
 			seleniumHolder.addResult(null, TestPartStatus.PASS);
 		}
 		
 		if (transition.hasCustomTimeout()) {
 			seleniumHolder.setNextPageElementTimeout(transition.getSecondsToWaitForResult());
 		}
 		else {
 			seleniumHolder.setNextPageElementTimeout(SeleniumUtils.getTimeout(seleniumHolder.getSettings()));;
 		}
 	}
 
 	/**
 	 * Converts a single user interaction to a Selenium command.
 	 * @return the Selenium command name invoked. 
 	 */
 	private String handleUserInteraction(SeleniumHolder seleniumHolder, UserInteraction userInteraction) {
 
 		IActionElement element = userInteraction.getElement();
 		ActionType actionType = userInteraction.getActionType();
 		boolean withinFrame = false;
 		if(element instanceof PageElement && seleniumHolder.isPageElementWithinFrame((PageElement)element)){
 			//check if parent frame was found:
 			if (TestPartStatus.FAIL == seleniumHolder.getParentFrame((PageElement)element).getStatus()) {
 				ErrorHandler.logAndShowErrorDialogAndThrow("Cannot interact with element " + element + ":\n" + 
 						"Parent frame " + seleniumHolder.getParentFrame((PageElement)element) + 
 						" not found.");
 			}
 			withinFrame = true;
 			getToRightFrame(seleniumHolder, seleniumHolder.getParentFrame((PageElement) element));
 		}
 		//Getting selenium commands, locators and values:
 		String commandName = SeleniumUtils.getCommandName(actionType);
 
 		String locator = null;
 		String inputValue = null;
 		
 		if (element instanceof Option) {
			Select select = ((Option) element).getParent();
			locator = "xpath=" + seleniumHolder.getFullContextWithAllElements(select);
 			inputValue = SeleniumUtils.getOptionLocator((Option) element);
 		}else {
 			//all other elements
 			if (element instanceof PageElement) {
 				locator = "xpath=" + seleniumHolder.getFullContextWithAllElements((PageElement) element);
 			}
 			else if(element instanceof WebBrowser){
 				locator = userInteraction.getValue();
 			}
 			else {
 				throw new ExporterException("Unsupported action element type");
 
 			}
 			inputValue = SeleniumUtils.getValue(userInteraction);
 		}
 		
 		try {
 			//invoke user interaction by reflection using command name from SeleniumUtil (legacy since Selenese exporter was written first):
 			
 			if (SeleniumUtils.hasSeleniumInputColumn(userInteraction)) {
 				//two parameters
 				seleniumHolder.getSelenium().execute(commandName, locator, inputValue);
 			}
 			else {
 				//one parameter only
 				seleniumHolder.getSelenium().execute(commandName, locator);
 			}
 			if(withinFrame && commandName.equals(SeleniumUtils.FIREEVENT))
 				upToParentFrame(seleniumHolder);
 			return commandName;
 		}
 		catch (Throwable e) {
 			String msg = "Error invoking user interaction: " + userInteraction.toString() + ".";
 			if (element instanceof PageElement) {
 				PageElement pe = (PageElement) element;
 				if (pe.getStatus().equals(TestPartStatus.FAIL)) {
 					msg += "\n\nPage element " + pe.toString() + " not found.";
 				}
 				seleniumHolder.addResultByIsNot(pe, TestPartStatus.EXCEPTION, pe.isNot());
 			}
 			Logger.error(msg, e);
 			throw new UserInteractionException(msg);
 		}
 	}
 
 	private void getToRightFrame(SeleniumHolder seleniumHolder, PageElement element) {
 		Frame parent = seleniumHolder.getParentFrame(element);
 		if(parent != null){
 			getToRightFrame(seleniumHolder, parent);
 		}
 		String locator = "xpath=" + seleniumHolder.getFullContextWithAllElements(element);
 		seleniumHolder.getSelenium().selectFrame(locator);
 	}
 
 	private void upToParentFrame(SeleniumHolder seleniumHolder) {
 		final CubicTestLocalRunner seleniumRunner = seleniumHolder.getSelenium();
 		seleniumRunner.selectFrame("relative=parent");
 	}
 }
