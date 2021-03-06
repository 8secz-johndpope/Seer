 /**
  * JBoss, Home of Professional Open Source
  * Copyright 2012, Red Hat, Inc. and individual contributors
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
 package org.richfaces.tests.page.fragments.impl.calendar.common;
 
 import org.jboss.arquillian.drone.api.annotation.Drone;
 import org.jboss.arquillian.graphene.Graphene;
 import org.jboss.arquillian.graphene.condition.element.WebElementConditionFactory;
 import org.jboss.arquillian.graphene.fragment.Root;
 import org.joda.time.DateTime;
 import org.joda.time.format.DateTimeFormat;
 import org.joda.time.format.DateTimeFormatter;
 import org.openqa.selenium.WebDriver;
 import org.openqa.selenium.WebElement;
 import org.openqa.selenium.support.FindBy;
 import org.openqa.selenium.support.ui.ExpectedCondition;
 import org.richfaces.tests.page.fragments.impl.calendar.common.editor.RichFacesCalendarEditor;
 import org.richfaces.tests.page.fragments.impl.calendar.common.editor.yearAndMonth.YearAndMonthEditor;
 
 /**
  * Component for header controls of calendar.
  * @author <a href="mailto:jstefek@redhat.com">Jiri Stefek</a>
  */
 public class RichFacesHeaderControls implements HeaderControls {
 
     @Root
     private WebElement root;
 
     @Drone
     protected WebDriver driver;
     //
     private RichFacesCalendarEditor calendarEditor;
     //
     @FindBy(css = "td.rf-cal-hdr-month > div.rf-cal-tl-btn")
     private WebElement yearAndMonthEditorOpenerElement;
     @FindBy(xpath = ".//td[1] /div")
     private WebElement previousYearElement;
     @FindBy(xpath = ".//td[2] /div")
     private WebElement previousMonthElement;
     @FindBy(xpath = ".//td[4] /div")
     private WebElement nextMonthElement;
     @FindBy(xpath = ".//td[5] /div")
     private WebElement nextYearElement;
     //
     private final DateTimeFormatter formatter = DateTimeFormat.forPattern("MMMM, yyyy");
 
     private void _openYearAndMonthEditor() {
        if (!isVisible() || new WebElementConditionFactory(yearAndMonthEditorOpenerElement).not().isVisible().apply(driver)) {
             throw new RuntimeException("Cannot open date editor. "
                    + "Ensure that calendar popup and header controls are displayed and some date is set.");
         }
         yearAndMonthEditorOpenerElement.click();
         Graphene.waitGui().until(calendarEditor.getDateEditor().isVisibleCondition());
     }
 
     @Override
     public WebElement getNextMonthElement() {
         return nextMonthElement;
     }
 
     @Override
     public WebElement getNextYearElement() {
         return nextYearElement;
     }
 
     @Override
     public WebElement getPreviousMonthElement() {
         return previousMonthElement;
     }
 
     @Override
     public WebElement getPreviousYearElement() {
         return previousYearElement;
     }
 
     @Override
     public DateTime getYearAndMonth() {
         return formatter.parseDateTime(yearAndMonthEditorOpenerElement.getText());
     }
 
     @Override
     public YearAndMonthEditor getYearAndMonthEditor() {
         return calendarEditor.getDateEditor();
     }
 
     @Override
     public WebElement getYearAndMonthEditorOpenerElement() {
         return yearAndMonthEditorOpenerElement;
     }
 
     @Override
     public ExpectedCondition<Boolean> isNotVisibleCondition() {
         return new WebElementConditionFactory(root).not().isVisible();
     }
 
     @Override
     public boolean isVisible() {
         return isVisibleCondition().apply(driver);
     }
 
     @Override
     public ExpectedCondition<Boolean> isVisibleCondition() {
         return new WebElementConditionFactory(root).isVisible();
     }
 
     @Override
     public void nextMonth() {
         if (!isVisible() || new WebElementConditionFactory(nextMonthElement).not().isVisible().apply(driver)) {
             throw new RuntimeException("Cannot interact with nextMonth button. "
                    + "Ensure that calendar popup and header controls are displayed.");
         }
         String before = yearAndMonthEditorOpenerElement.getText();
         nextMonthElement.click();
        Graphene.waitAjax().until(new WebElementConditionFactory(yearAndMonthEditorOpenerElement).not().textEquals(before));
     }
 
     @Override
     public void nextYear() {
         if (!isVisible() || new WebElementConditionFactory(nextYearElement).not().isVisible().apply(driver)) {
             throw new RuntimeException("Cannot interact with nextYear button. "
                    + "Ensure that calendar popup and header controls are displayed.");
         }
         String before = yearAndMonthEditorOpenerElement.getText();
         nextYearElement.click();
        Graphene.waitAjax().until(new WebElementConditionFactory(yearAndMonthEditorOpenerElement).not().textEquals(before));
     }
 
     @Override
     public YearAndMonthEditor openYearAndMonthEditor() {
         if (calendarEditor.getDateEditor().isVisible()) {
             return calendarEditor.getDateEditor();
         } else {
             _openYearAndMonthEditor();
             return calendarEditor.getDateEditor();
         }
     }
 
     @Override
     public void previousYear() {
         if (!isVisible() || new WebElementConditionFactory(previousYearElement).not().isVisible().apply(driver)) {
             throw new RuntimeException("Cannot interact with previousYear button. "
                    + "Ensure that calendar popup and header controls are displayed.");
         }
         String before = yearAndMonthEditorOpenerElement.getText();
         previousYearElement.click();
        Graphene.waitAjax().until(new WebElementConditionFactory(yearAndMonthEditorOpenerElement).not().textEquals(before));
     }
 
     @Override
     public void previousMonth() {
         if (!isVisible() || new WebElementConditionFactory(previousMonthElement).not().isVisible().apply(driver)) {
             throw new RuntimeException("Cannot interact with previousMonth button. "
                    + "Ensure that calendar popup and header controls are displayed.");
         }
         String before = yearAndMonthEditorOpenerElement.getText();
         previousMonthElement.click();
        Graphene.waitAjax().until(new WebElementConditionFactory(yearAndMonthEditorOpenerElement).not().textEquals(before));
     }
 
     public void setCalendarEditor(RichFacesCalendarEditor calendarEditor) {
         this.calendarEditor = calendarEditor;
     }
 }
