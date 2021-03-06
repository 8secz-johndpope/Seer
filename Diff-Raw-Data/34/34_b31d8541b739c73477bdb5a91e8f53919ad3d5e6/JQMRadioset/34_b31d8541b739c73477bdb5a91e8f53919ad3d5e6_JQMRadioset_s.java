 package com.sksamuel.jqm4gwt.form.elements;
 
 import com.google.gwt.dom.client.Document;
 import com.google.gwt.dom.client.Element;
 import com.google.gwt.event.dom.client.*;
 import com.google.gwt.event.logical.shared.HasSelectionHandlers;
 import com.google.gwt.event.logical.shared.SelectionHandler;
 import com.google.gwt.event.logical.shared.ValueChangeHandler;
 import com.google.gwt.event.shared.HandlerRegistration;
 import com.google.gwt.uibinder.client.UiChild;
 import com.google.gwt.user.client.ui.*;
 import com.sksamuel.jqm4gwt.HasOrientation;
 import com.sksamuel.jqm4gwt.HasText;
 import com.sksamuel.jqm4gwt.JQMWidget;
 import com.sksamuel.jqm4gwt.form.JQMFieldset;
 import com.sksamuel.jqm4gwt.html.Legend;
 
 import java.util.ArrayList;
 import java.util.List;
 
 /**
  * @author Stephen K Samuel samspade79@gmail.com 24 May 2011 08:17:31
  * 
  *         A widget that is a collection of one or more radio buttons. All radio
  *         buttons must belong to a radioset. All radio buttons in a set are
  *         grouped and styled together.
  * 
  *       * <h3>Use in UiBinder Templates</h3>
  *
  * When working with JQMRadioset in
  * {@link com.google.gwt.uibinder.client.UiBinder UiBinder} templates, you
  * can add Radio buttons via child elements. For example:
  * <pre>
  * &lt;jqm:form.elements.JQMRadioset>
  *    &lt;jqm:radio id="radioId#1" text="radio #1"/>
  *    &lt;jqm:radio id="radioId#2" text="radio #2"/>
  * &lt;/jqm:form.elements.JQMRadioset>
  * </pre>
  *
  */
 public class JQMRadioset extends JQMWidget implements HasText<JQMRadioset>, HasSelectionHandlers<String>, HasOrientation<JQMRadioset>, HasValue<String>,
 		JQMFormWidget, HasClickHandlers {
 
 	/**
 	 * The panel that is used for the controlgroup container
 	 */
 	private JQMFieldset	fieldset;
 
 	private Legend		legend;
 
 	/**
 	 * The panel that acts as the fieldcontain container
 	 */
 	private final FlowPanel		flow;
 
 	/**
 	 * The input's that are used for the radio buttons
 	 */
 	private final List<TextBox>	radios	= new ArrayList();
 
 	/**
 	 * Creates a new {@link JQMRadioset} with no label
 	 * 
 	 */
 	public JQMRadioset() {
 		this(null);
 	}
 
 	/**
 	 * Creates a new {@link JQMRadioset} with the label text set to the given
 	 * value
 	 * 
 	 * @param text
 	 *              the text for the label
 	 */
 	public JQMRadioset(String text) {
 
 		flow = new FlowPanel();
 		flow.getElement().setId(Document.get().createUniqueId());
 		initWidget(flow);
 		setDataRole("fieldcontain");
 		setupFieldset(text);
 	}
 
 	private void setupFieldset(String labelText) {
 		if(fieldset != null) flow.remove(fieldset);
 		// the fieldset is the inner container and is contained inside the
 		// flow
 		fieldset = new JQMFieldset();
 		fieldset.getElement().setId(Document.get().createUniqueId());
 		flow.add(fieldset);
 
 		// the legend must be added to the fieldset
 		legend = new Legend();
 		legend.setText(labelText);
 		fieldset.add(legend);		
 	}
 	
 	BlurHandler blurHandler;
 	ArrayList<HandlerRegistration> blurHandlers = new ArrayList<HandlerRegistration>();
 
 	private void addRadiosBlurHandler(final BlurHandler handler)
 	{
 		for (TextBox radio : radios) {
 			blurHandlers.add(radio.addChangeHandler(new ChangeHandler() {
 
 				@Override
 				public void onChange(ChangeEvent event) {
 					handler.onBlur(null);
 				}
 			}));
 		}		
 	}
 	
 	private void clearBlurHandlers()
 	{
 		for(HandlerRegistration blurHandler : blurHandlers) blurHandler.removeHandler();
 		blurHandlers.clear();
 	}
 	
 	protected void onLoad()
 	{
 		if(blurHandler != null && blurHandlers.size() == 0) addRadiosBlurHandler(blurHandler);
 	}
 	
 	protected void onUnload()
 	{
 		clearBlurHandlers();
 	}	
 	
 	/**
 	 * no-op implementation required for {@link JQMFormWidget}
 	 */
 	@Override
 	public HandlerRegistration addBlurHandler(final BlurHandler handler) {
 		this.blurHandler = handler;
 		clearBlurHandlers();
 		addRadiosBlurHandler(handler);
 		return null;
 	}
 
 	@Override
 	public HandlerRegistration addClickHandler(ClickHandler handler) {
 		return addDomHandler(handler, ClickEvent.getType());
 	}
 
 	@Override
 	public Label addErrorLabel() {
 		return null;
 	}
 
 	/**
 	 * Adds a new radio button to this radioset using the given string for
 	 * both the value and the text. Returns a JQMRadio instance which can be
 	 * used to change the value and label of the radio button.
 	 * 
 	 * This method is the same as calling addRadio(String, String) with the
 	 * same string twice.
 	 * 
 	 * @param value
 	 *              the value to associate with this radio option. This will
 	 *              be the value returned by methods that query the selected
 	 *              value. The value will also be used for the label.
 	 * 
 	 * @return a JQMRadio instance to adjust the added radio button
 	 */
 	public JQMRadio addRadio(String value) {
 		return addRadio(value, value);
 	}
 
 	/**
 	 * Adds a new radio button to this radioset using the given value and
 	 * text. Returns a JQMRadio instance which can be used to change the value
 	 * and label of the radio button.
 	 * 
 	 * @param value
 	 *              the value to associate with this radio option. This will
 	 *              be the value returned by methods that query the selected
 	 *              value.
 	 * 
 	 * @param text
 	 *              the label to show for this radio option.
 	 * 
 	 * @return a JQMRadio instance to adjust the added radio button
 	 */
 	public JQMRadio addRadio(String value, String text) {
         JQMRadio radio = new JQMRadio(value, text);
         addRadio(radio);
 		return radio;
 	}
 
     /**
      * UiBinder call method to add a radio button
      * @param radio
      */
     @UiChild(tagname = "radio")
     public void addRadio(JQMRadio radio) {
         radio.setName(fieldset.getId());
         radios.add(radio.getInput());
         fieldset.add(radio.getInput());
         fieldset.add(radio.getLabel());
     }
 
 
     public void clear() {
 		radios.clear();
 		setupFieldset(getText());
 	}
 	
     @Override
     public void setTheme(String themeName) {
     	for(TextBox radio : radios) applyTheme(radio, themeName);
     }
 	
     @Override
     public JQMWidget withTheme(String themeName) {
         setTheme(themeName);
         return this;
     }
 
 	@Override
 	public HandlerRegistration addSelectionHandler(SelectionHandler<String> handler) {
 		return null;
 	}
 
 	@Override
 	public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
 		return null;
 	}
 
 	/**
 	 * Alias for getValue()
 	 */
 	public String getSelectedValue() {
 		return getValue();
 	}
 
 	/**
 	 * Returns the text used for the main label
 	 */
 	@Override
 	public String getText() {
 		return legend.getText();
 	}
 
 	/**
 	 * 
 	 * Note: this method will return null until after the jquery init phase
 	 * has completed. That means if you call getValue() on an element in the
 	 * intial construction of a page then it will return null.
 	 * 
 	 * @return the value of the currently selected radio button or null if no
 	 *         button is currently selected.
 	 * 
 	 * 
 	 */
 	@Override
 	public String getValue() {
		// we can't use the jquery methods because they fire after the GWT
		// event handlers. We also must look for the hover element first.
		for (int k = 0; k < fieldset.getWidgetCount(); k++) {
			Widget widget = fieldset.getWidget(k);
			Element element = widget.getElement();
			String classAttribute = element.getAttribute("class");
			if (classAttribute != null)
			{
				if (classAttribute.contains("ui-btn-hover")) {
					return getValueForId(element.getAttribute("for"));
				}
				else if (classAttribute.contains("ui-btn-active")) {
					return getValueForId(element.getAttribute("for"));
				}
				else if (classAttribute.contains("ui-radio-on")) {
					return getValueForId(element.getAttribute("for"));
				}
			}
		}
		
 		for (TextBox radio : radios) {
 			Element element = radio.getElement();
			if (element.getAttribute("checked") != null && element.getAttribute("checked").equals("checked")) {
				return getValueForId(element.getAttribute("value"));
 			}
 		}
 		return null;
 	}
 
 	/**
 	 * Returns the value of the radio option at the given index.
 	 * 
 	 * @return the value of the k'th radio option
 	 */
 	public String getValue(int k) {
 		return radios.get(k).getElement().getAttribute("value");
 	}
 
 	/**
 	 * Returns the value of the button that has the given id
 	 * 
 	 * @return the value of the button with the given id
 	 */
 	private String getValueForId(String id) {
 		for (int k = 0; k < fieldset.getWidgetCount(); k++) {
 			Widget widget = fieldset.getWidget(k);
 			if (id.equals(widget.getElement().getAttribute("id")))
 				return widget.getElement().getAttribute("value");
 		}
 		return null;
 	}
 
 	@Override
 	public boolean isHorizontal() {
 		return fieldset.isHorizontal();
 	}
 
 	@Override
 	public boolean isVertical() {
 		return fieldset.isVertical();
 	}
 
 	protected native void refresh() /*-{
 							$wnd.$("input[type='radio']").checkboxradio("refresh");
 							}-*/;
 
 	/**
 	 * Removes the given {@link JQMRadio} from this radioset
 	 * 
 	 * @param radio
 	 *              the radio to remove
 	 */
 	public void removeRadio(JQMRadio radio) {
 		removeRadio(radio);
 	}
 
 	/**
 	 * Removes the {@link JQMRadio} button that has the given value
 	 */
 	public void removeRadio(String value) {
 		// TODO traverse all elements removing anything that has a "for" for
 		// this id or actually has this id
 	}
 
 	@Override
 	public void setHorizontal() {
 		fieldset.withHorizontal();
 	}
 
     @Override
    	public JQMRadioset withHorizontal() {
         setHorizontal();
         return this;
    	}
 
 	/**
 	 * Sets the selected value. This is an alias for setValue(String)
 	 * 
 	 * @see #setValue(String)
 	 */
 	public void setSelectedValue(String value) {
 		setValue(value);
 	}
 
 	@Override
 	public void setText(String text) {
 		legend.setText(text);
 	}
 
 	@Override
 	public void setValue(String value) {
 		setValue(value, false);
 	}
 
 	/**
 	 * Sets the currently selected radio to the given value.
 	 * 
 	 * NOTE: Currently jquery mobile has a bug. Once the selected is changed
 	 * by a user clicking, it can no longer be changed programatically.
 	 */
 	@Override
 	public void setValue(String value, boolean fireEvents) {
 		for (TextBox radio : radios) {
 			Element e = radio.getElement();
 			if (value.equals(radio.getValue())) {
 				e.setAttribute("checked", "checked");
 			} else {
 				e.removeAttribute("checked");
 			}
 		}
 	}
 
 	@Override
 	public void setVertical() {
 		fieldset.withVertical();
 	}
 
     @Override
    	public JQMRadioset withVertical() {
         setVertical();
       	return this;
    	}
 
 	/**
 	 * Returns the number of radio options set on this radioset
 	 * 
 	 * @return the integer number of options
 	 */
 	public int size() {
 		return radios.size();
 	}
 
     @Override
     public JQMRadioset withText(String text) {
         setText(text);
         return this;
     }
}
