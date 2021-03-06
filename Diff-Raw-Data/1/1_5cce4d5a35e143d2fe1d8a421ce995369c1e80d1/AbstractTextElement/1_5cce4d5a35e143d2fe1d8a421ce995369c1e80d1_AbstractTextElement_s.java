 /* ***** BEGIN LICENSE BLOCK *****
  * Version: MPL 1.1
  * The contents of this file are subject to the Mozilla Public License Version
  * 1.1 (the "License"); you may not use this file except in compliance with
  * the License. You may obtain a copy of the License at
  * http://www.mozilla.org/MPL/
  *
  * Software distributed under the License is distributed on an "AS IS" basis,
  * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
  * for the specific language governing rights and limitations under the
  * License.
  *
  * The Original Code is Riot.
  *
  * The Initial Developer of the Original Code is
  * Neteye GmbH.
  * Portions created by the Initial Developer are Copyright (C) 2006
  * the Initial Developer. All Rights Reserved.
  *
  * Contributor(s):
  *   Felix Gnass [fgnass at neteye dot de]
  *
  * ***** END LICENSE BLOCK ***** */
 package org.riotfamily.forms.element;
 
 import java.beans.PropertyEditor;
 import java.io.PrintWriter;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 
 import org.riotfamily.common.markup.Html;
 import org.riotfamily.common.markup.TagWriter;
 import org.riotfamily.common.util.PasswordGenerator;
 import org.riotfamily.forms.AbstractEditorBase;
 import org.riotfamily.forms.AbstractElement;
 import org.riotfamily.forms.Editor;
 import org.riotfamily.forms.ErrorUtils;
 import org.riotfamily.forms.event.ChangeEvent;
 import org.riotfamily.forms.event.ChangeListener;
 import org.riotfamily.forms.event.JavaScriptEvent;
 import org.riotfamily.forms.event.JavaScriptEventAdapter;
 import org.riotfamily.forms.request.FormRequest;
 import org.springframework.util.Assert;
 import org.springframework.util.ObjectUtils;
 import org.springframework.util.StringUtils;
 
 
 /**
  * Abstract base class for elements that handle textual input from a single HTTP
  * parameter. Optionally a <code>PropertyEditor</code> can be set to convert
  * the text into an arbitrary object.
  *
  * @see org.riotfamily.forms.EditorBinder#bind(Editor, String)
  */
 public abstract class AbstractTextElement extends AbstractEditorBase
 		implements Editor, JavaScriptEventAdapter {
 
 	private String type = "text";
 
 	private Integer maxLength;
 
 	private boolean trim = true;
 
 	private boolean randomParamName;
 
 	private List listeners;
 
 	private String text;
 
 	private Object value;
 
 	private PropertyEditor propertyEditor;
 
 	private boolean validateOnChange = false;
 
 	public AbstractTextElement() {
 	}
 
 	public AbstractTextElement(String type) {
 		this.type = type;
 	}
 
 	public String getType() {
 		return type;
 	}
 
 	public void setType(String type) {
 		this.type = type;
 	}
 
 	/**
 	 * Overrides {@link AbstractElement#getStyleClass()} and returns the
 	 * element's type if no custom style has been set.
 	 *
 	 * @see #getType()
 	 */
 	public String getStyleClass() {
 		String styleClass = super.getStyleClass();
 		return styleClass != null ? styleClass : type;
 	}
 
 	public Integer getMaxLength() {
 		return this.maxLength;
 	}
 
 	/**
 	 * Sets the maximum string length.
 	 */
 	public void setMaxLength(Integer maxLength) {
 		this.maxLength = maxLength;
 	}
 
 	/**
 	 * Sets whether the user input should be trimmed.
 	 */
 	public void setTrim(boolean trim) {
 		this.trim = trim;
 	}
 
 	/**
 	 * Sets whether a random (nonce) word should be used as parameter name
 	 * to disable the browser's autocompletion feature.
 	 */
 	public void setRandomParamName(boolean randomParamName) {
 		//REVIST Consider using setAttribute("autocomlete", "off") instead
 		this.randomParamName = randomParamName;
 	}
 
 	/**
 	 * Sets whether the element should be validated as soon as a change event
 	 * is received.
 	 */
 	public void setValidateOnChange(boolean validateOnChange) {
 		this.validateOnChange = validateOnChange;
 	}
 
 	public final void setPropertyEditor(PropertyEditor propertyEditor) {
 		this.propertyEditor = propertyEditor;
 	}
 
 	protected final PropertyEditor getPropertyEditor() {
 		return propertyEditor;
 	}
 
 	protected void initPropertyEditor() {
 		if (propertyEditor == null) {
 			this.propertyEditor = getEditorBinding().getPropertyEditor();
 		}
 	}
 
 	protected void afterBindingSet() {
 		initPropertyEditor();
 	}
 
 	public final String getText() {
 		return text;
 	}
 
 	/**
 	 * Sets the element's text value. If {@link #setTrim(boolean)} is set to
 	 * <code>true</code>, leading and trailing whitespaces are stripped.
 	 */
 	public void setText(String text) {
 		if (trim && text != null) {
 			this.text = text.trim();
 		}
 		else {
 			this.text = text;
 		}
 	}
 
 	public final Object getValue() {
 		return value;
 	}
 
 	public void setValue(Object value) {
 		this.value = value;
 		setTextFromValue();
 	}
 
 	protected void setTextFromValue() {
 		if (value == null) {
 			text = null;
 		}
 		else if (value instanceof String) {
 			text = (String) value;
 		}
 		else {
 			if (propertyEditor == null) {
 				initPropertyEditor();
 				Assert.notNull(propertyEditor, "Can't handle value of type "
 						+ value.getClass().getName() + " - no PropertyEditor "
 						+ "present");
 			}
 			propertyEditor.setValue(value);
 			text = propertyEditor.getAsText();
 		}
 	}
 
 	public void processRequest(FormRequest request) {
 		if (isEnabled()) {
 			text = request.getParameter(getParamName());
 			validate();
 			if (!ErrorUtils.hasErrors(this)) {
 				setValueFromText();
 			}
 		}
 	}
 
 	public void handleJavaScriptEvent(JavaScriptEvent event) {
 		if (event.getType() == JavaScriptEvent.ON_CHANGE) {
 			text = event.getValue();
 			ErrorUtils.removeErrors(this);
 			validateSyntax();
 			if (!ErrorUtils.hasErrors(this)) {
 				setValueFromText();
 			}
 		}
 	}
 
 	protected void setValueFromText() {
 		Object oldValue = value;
 		if (!StringUtils.hasText(text)) {
 			value = null;
 		}
 		else {
 			if (propertyEditor != null) {
 				propertyEditor.setAsText(text);
 				value = propertyEditor.getValue();
 			}
 			else {
 				value = text;
 			}
 		}
 		if (!ObjectUtils.nullSafeEquals(value, oldValue)) {
 			fireChangeEvent(value, oldValue);
 		}
 	}
 
 	protected void validate() {
 		if (isRequired() && !StringUtils.hasLength(text)) {
 			ErrorUtils.reject(this, "required");
 		}
 		validateSyntax();
 	}
 	
 	protected void validateSyntax() {
 	}
 
 	protected String getDesiredParamName() {
 		if (randomParamName) {
 			return PasswordGenerator.getDefaultInstance().generate();
 		}
 		return super.getDesiredParamName();
 	}
 
 	public void renderInternal(PrintWriter writer) {
 		TagWriter input = new TagWriter(writer);
 		input.startEmpty(Html.INPUT)
 				.attribute(Html.INPUT_TYPE, getType())
 				.attribute(Html.COMMON_CLASS, getStyleClass())
 				.attribute(Html.COMMON_ID, getId())
 				.attribute(Html.INPUT_NAME, getParamName())
 				.attribute(Html.INPUT_VALUE, getText())
 				.attribute(Html.INPUT_DISABLED, !isEnabled());
 
 		if (getMaxLength() != null) {
 			input.attribute(Html.INPUT_MAX_LENGTH, getMaxLength().intValue());
 		}
 		input.end();
 	}
 
 	public void addChangeListener(ChangeListener listener) {
 		if (listeners == null) {
 			listeners = new ArrayList();
 		}
 		listeners.add(listener);
 	}
 
 	protected void fireChangeEvent(Object newValue, Object oldValue) {
 		if (listeners != null) {
 			ChangeEvent event = new ChangeEvent(this, newValue, oldValue);
 			Iterator it = listeners.iterator();
 			while (it.hasNext()) {
 				ChangeListener listener = (ChangeListener) it.next();
 				listener.valueChanged(event);
 			}
 		}
 	}
 
 	public int getEventTypes() {
 		if (validateOnChange || listeners != null) {
 			return JavaScriptEvent.ON_CHANGE;
 		}
 		return JavaScriptEvent.NONE;
 	}
 
 }
