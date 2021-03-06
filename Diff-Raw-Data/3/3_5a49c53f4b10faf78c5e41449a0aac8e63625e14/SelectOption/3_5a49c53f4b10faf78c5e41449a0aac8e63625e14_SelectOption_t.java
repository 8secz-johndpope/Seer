 package com.idega.presentation.ui;
 
 import java.util.Map;
 
 import javax.faces.component.UIComponent;
 
 import com.idega.presentation.IWContext;
 
 /**
  * @author laddi
  */
 public class SelectOption extends InterfaceObject {
 	
 	private Class windowClass;
 	private Map parameterMap;
 	private String target;
 	private int fileID = -1;
 
 	public SelectOption() {
 		this("untitled");
 	}
 
 	public SelectOption(String value) {
 		this(value, value);
 	}
 	
 	public SelectOption(String name, int value) {
 		this(name, String.valueOf(value));	
 	}
 	
 	public SelectOption(String name, char value) {
 		this(name, String.valueOf(value));	
 	}
 
 	public SelectOption(String name, String value) {
 		super();
 		setName(name);
 		setValue(value);
 		setSelected(false);
 		setDisabled(false);
 	}
 
 	/**
 	 * Sets whether the <code>SelectOption</code> is selected or not.
 	 * @param selected	The status to set.
 	 */
 	public void setSelected(boolean selected) {
 		if (selected)
 			setMarkupAttribute("selected", "selected");
 		else
 			this.removeMarkupAttribute("selected");
 	}
 	
 	/**
 	 * Sets the label for the <code>SelectOption</code>.
 	 * @param label	The label to set.
 	 */
 	public void setLabel(String label) {
 		setMarkupAttribute("label", label);
 	}
 	
 	/**
 	 * Returns the selected status of the <code>SelectOption</code>.
 	 * @return boolean	True if <code>SelectOption</code> is selected, false otherwise.
 	 */
 	public boolean getSelected() {
 		if (isMarkupAttributeSet("selected"))
 			return true;
 		return false;	
 	}
 	
 	public void main(IWContext iwc) throws Exception {
 		if (windowClass != null) {
 			String URL = Window.getWindowURLWithParameters(windowClass, iwc, parameterMap);
 			String arguments = Window.getWindowArgumentCallingScript(windowClass);
 			setValue(URL + "$" + arguments + "$" + target);
 			
 			getParentSelect().addSelectScript(true);
 		}
 		if (fileID != -1) {
			String URL = getICFileSystem(iwc).getFileURI(fileID);
 			String arguments = Window.getWindowArgumentCallingScript(false, false, false, false, false, true, true, true, false, 640, 480, null, null);
 			setValue(URL + "$" + arguments + "$" + "_blank");
 		}
 	}
 	
 	protected GenericSelect getParentSelect() {
 		UIComponent parent = this.getParent();
 		if (parent != null && parent instanceof GenericSelect) {
 			return (GenericSelect) parent;
 		}
 		return null;
 	}
 
 	public void print(IWContext iwc) throws Exception {
 		if (getMarkupLanguage().equals("HTML")) {
 			print("<option " + getMarkupAttributesString() + " >");
 			print(getName());
 			println("</option>");
 		}
 		else if (getMarkupLanguage().equals("WML")) {
 			print("<option value=\"" + getValueAsString() + "\" >");
 			print(getName());
 			println("</option>");
 		}
 	}
 
 	public void setWindowToOpenOnSelect(Class windowClass, Map parameterMap) {
 		this.windowClass = windowClass;
 		this.parameterMap = parameterMap;
 		this.target = "undefined";
 	}
 
 	public void setWindowToOpenOnSelect(Class windowClass, Map parameterMap, String target) {
 		this.windowClass = windowClass;
 		this.parameterMap = parameterMap;
 		this.target = target;
 	}
 	
 	public void setFileToOpenOnSelect(int fileID) {
 		this.fileID = fileID;
 	}
 
 	/**
 	 * @see com.idega.presentation.ui.InterfaceObject#handleKeepStatus(IWContext)
 	 */
 	public void handleKeepStatus(IWContext iwc) {
 	}
 
 	/* (non-Javadoc)
 	 * @see com.idega.presentation.PresentationObject#isContainer()
 	 */
 	public boolean isContainer() {
 		return false;
 	}
 }
