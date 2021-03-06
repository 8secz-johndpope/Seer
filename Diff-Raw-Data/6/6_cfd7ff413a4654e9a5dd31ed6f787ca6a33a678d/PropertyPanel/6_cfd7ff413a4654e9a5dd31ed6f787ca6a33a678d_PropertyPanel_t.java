 // PathVisio,
 // a tool for data visualization and analysis using Biological Pathways
 // Copyright 2006-2007 BiGCaT Bioinformatics
 //
 // Licensed under the Apache License, Version 2.0 (the "License"); 
 // you may not use this file except in compliance with the License. 
 // You may obtain a copy of the License at 
 // 
 // http://www.apache.org/licenses/LICENSE-2.0 
 //  
 // Unless required by applicable law or agreed to in writing, software 
 // distributed under the License is distributed on an "AS IS" BASIS, 
 // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 // See the License for the specific language governing permissions and 
 // limitations under the License.
 //
 package org.pathvisio.gui.swt;
 
 import java.awt.Color;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Set;
 
 import org.eclipse.jface.viewers.ArrayContentProvider;
 import org.eclipse.jface.viewers.CellEditor;
 import org.eclipse.jface.viewers.ColorCellEditor;
 import org.eclipse.jface.viewers.ComboBoxCellEditor;
 import org.eclipse.jface.viewers.DialogCellEditor;
 import org.eclipse.jface.viewers.ICellModifier;
 import org.eclipse.jface.viewers.ILabelProviderListener;
 import org.eclipse.jface.viewers.IStructuredContentProvider;
 import org.eclipse.jface.viewers.ITableLabelProvider;
 import org.eclipse.jface.viewers.TableViewer;
 import org.eclipse.jface.viewers.TextCellEditor;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.graphics.Image;
 import org.eclipse.swt.graphics.RGB;
 import org.eclipse.swt.layout.FillLayout;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Control;
 import org.eclipse.swt.widgets.Table;
 import org.eclipse.swt.widgets.TableColumn;
 import org.eclipse.swt.widgets.TableItem;
 import org.pathvisio.ApplicationEvent;
 import org.pathvisio.Engine;
 import org.pathvisio.Engine.ApplicationEventListener;
 import org.pathvisio.gui.swt.dialogs.CommentsDialog;
 import org.pathvisio.model.DataNodeType;
 import org.pathvisio.model.DataSource;
 import org.pathvisio.model.LineStyle;
 import org.pathvisio.model.LineType;
 import org.pathvisio.model.ObjectType;
 import org.pathvisio.model.Organism;
 import org.pathvisio.model.OrientationType;
 import org.pathvisio.model.OutlineType;
 import org.pathvisio.model.PathwayElement;
 import org.pathvisio.model.PathwayEvent;
 import org.pathvisio.model.PathwayListener;
 import org.pathvisio.model.PropertyClass;
 import org.pathvisio.model.PropertyType;
 import org.pathvisio.model.ShapeType;
 import org.pathvisio.preferences.GlobalPreference;
 import org.pathvisio.util.swt.SwtUtils;
 import org.pathvisio.util.swt.TableColumnResizer;
 import org.pathvisio.view.Graphics;
 import org.pathvisio.view.VPathway;
 import org.pathvisio.view.SelectionBox.SelectionEvent;
 import org.pathvisio.view.SelectionBox.SelectionListener;
 
 /**
  * This class implements the sidepanel where you can edit graphical properties
  * of each object on the pathway.
  */
 public class PropertyPanel extends Composite implements PathwayListener, SelectionListener, ApplicationEventListener {
 	public TableViewer tableViewer;
 	CellEditor[] cellEditors = new CellEditor[2];
 	TextCellEditor textEditor;
 	ColorCellEditor colorEditor;
 	ComboBoxCellEditor comboBoxEditor;
 	//Disable suggest cell editors for now, can't get them to work
 	//on both linux and windows
 //	SuggestCellEditor identifierSuggestEditor;
 //	SuggestCellEditor symbolSuggestEditor;
 	DialogCellEditor commentsEditor;
 	
 	private List<PathwayElement> dataObjects;
 	
 	private List<PropertyType> attributes;
 	
 	final static int TYPES_DIFF = ObjectType.MIN_VALID -1;
 	final static Object VALUE_DIFF = new Different();
 	
 	private static class Different {
 		public boolean equals(Object obj) {
 			return obj instanceof Different;
 		}
 		public String toString() {
 			return "Different values";
 		}
 	}
 	
 	/**
 	 * Add a {@link PathwayElement} to the list of objects of which 
 	 * the properties are displayed
 	 * @param o
 	 */
 	public void addGmmlDataObject(PathwayElement o) {
 		if(!dataObjects.contains(o)) {
 			if(dataObjects.add(o)) {
 				o.addListener(this);
 				refresh();
 			}
 		}
 	}
 	
 	/**
 	 * Remove a {@link PathwayElement} from the list of objects of which 
 	 * the properties are displayed
 	 * @param o
 	 */
 	public void removeGmmlDataObject(PathwayElement o) {
 		if(dataObjects.remove(o)) {
 			o.removeListener(this);
 			refresh();
 		}
 	}
 	
 	/**
 	 * Clear the list of objects of which the properties are displayed
 	 */
 	private void clearGmmlDataObjects() {
 		for(PathwayElement o : dataObjects) o.removeListener(this);
 		dataObjects.clear();
 		refresh();
 	}
 	
 	/**
 	 * Refresh the table and attributes to display
 	 */
 	void refresh() {
 		setAttributes();
 		tableViewer.refresh();
 	}
 	
 	int getAggregateType() {
 		int type = TYPES_DIFF;
 		for(int i = 0; i < dataObjects.size(); i++) {
 			PathwayElement g = dataObjects.get(i);
 			
 			if(i != 0 && type != g.getObjectType()) return TYPES_DIFF;
 			
 			type = g.getObjectType();
 		}
 		return type;
 	}
 	
 	Object getAggregateValue(PropertyType key) {
 		Object value = VALUE_DIFF;
 		for(int i = 0; i < dataObjects.size(); i++) {
 			PathwayElement g = dataObjects.get(i);
 			Object o = g.getProperty(key);
 			if(i != 0 && (o == null || !o.equals(value))) return VALUE_DIFF;
 
 			value = o;
 		}
 		return value;
 	}
 		
 	/**
 	 * Sets the attributes for the selected objects
 	 * Only attributes that are present in all objects in the selection will be
 	 * added to the attributes list and shown in the property table
 	 */
 	public void setAttributes ()
 	{
 		HashMap<PropertyType, Integer> master = new HashMap<PropertyType, Integer>();
 		for (PathwayElement o : dataObjects)
 		{
 			// get attributes. Only get advanced attributes if the preferences say so.
 			for (PropertyType attr : o.getAttributes(
 					 GlobalPreference.getValueBoolean(GlobalPreference.SHOW_ADVANCED_ATTRIBUTES)))
 			{
 				if (master.containsKey(attr))
 				{
 					// increment
 					master.put(attr, master.get(attr) + 1);
 				}
 				else
 				{
 					// set to 1
 					master.put(attr, 1);
 				}
 			}
 		}
 		attributes.clear();
 		for (PropertyType attr : master.keySet())
 		{
 			if (master.get(attr) == dataObjects.size())
 			{
 				attributes.add(attr);
 			}
 		}
 		// sortAttributes();
 		Collections.sort (attributes);		
 	}
 	
 //	void sortAttributes() {
 //		Collections.sort(attributes, new Comparator() {
 //			public int compare(Object o1, Object o2) {
 //				return o1.ordinal() - o2.ordinal();
 //			}
 //		});
 //	}
 
 	final static String[] colNames = new String[] {"Property", "Value"};
 				
 	PropertyPanel(Composite parent, int style)
 	{
 		super(parent, style);
 		setLayout(new FillLayout());
 		Table t = new Table(this, style);
 		TableColumn tcName = new TableColumn(t, SWT.LEFT);
 		TableColumn tcValue = new TableColumn(t, SWT.LEFT);
 		tcName.setText(colNames[0]);
 		tcValue.setText(colNames[1]);
 		tcName.setWidth(80);
 		tcValue.setWidth(70);
 		tableViewer = new TableViewer(t);
 		tableViewer.getTable().setLinesVisible(true);
 		tableViewer.getTable().setHeaderVisible(true);
 		tableViewer.setContentProvider(tableContentProvider);
 		tableViewer.setLabelProvider(tableLabelProvider);
 		
 		cellEditors[1] = cellEditors[0] = textEditor = new TextCellEditor(tableViewer.getTable());
 		colorEditor = new ColorCellEditor(tableViewer.getTable());
 		comboBoxEditor = new ComboBoxCellEditor(tableViewer.getTable(), new String[] {""});
 //		identifierSuggestEditor = new GdbCellEditor(tableViewer.getTable(), GdbCellEditor.TYPE_IDENTIFIER);
 //		symbolSuggestEditor = new GdbCellEditor(tableViewer.getTable(), GdbCellEditor.TYPE_SYMBOL);
 		//Temporary table editor for comments, will be removed when right-click menu is implemented
 		commentsEditor = new DialogCellEditor(tableViewer.getTable()) {
 			protected Object openDialogBox(Control cellEditorWindow) {
 				if(dataObjects.size() == 1) {
 					PathwayElement e = dataObjects.get(0);
 					CommentsDialog d = new CommentsDialog(cellEditorWindow.getShell(), e);
 					d.open();
 					return e.getComments();
 				}
 				return getValue();
 			}
 		};
 		
 		tableViewer.setCellEditors(cellEditors);
 		tableViewer.setColumnProperties(colNames);
 		tableViewer.setCellModifier(cellModifier);
 		
 		t.addControlListener(new TableColumnResizer(t, t.getParent()));
 		
 		dataObjects = new ArrayList<PathwayElement>();
 		attributes = new ArrayList<PropertyType>();
 		tableViewer.setInput(attributes);
 		
 		Engine.getCurrent().addApplicationEventListener(this);
 		VPathway vp = Engine.getCurrent().getActiveVPathway();
 		if(vp != null) vp.addSelectionListener(this);
 	}
 	
 	/**
 	 * return the right cell editor for a certain object. Will return
 	 * one of existing editors. In the case of a list of possible values, 
 	 * a comboboxeditor will be set up with the proper values for
 	 * the drop down list.
 	 */
 	private final static String[] orientation_names = OrientationType.getNames();
 	private final static String[] linestyle_names = LineStyle.getNames();
 	private final static String[] boolean_names = {"false", "true"};
 	// shapetype is dynamic: can be changed with preferences
 	private String[] shape_names = null;
     // linetypes is dynamic: can be changed with preferences
     private String[] linetype_names = null; 
 	private final static String[] outlinetype_names = OutlineType.getTags(); 
 	private final static String[] genetype_names = DataNodeType.getNames();
 	// datasourcetypes is dynamic, can be changed by plugins
 	private static String[] datasource_names = null;
 	
 	private CellEditor getCellEditor(Object element)
 	{
 		PropertyType key = (PropertyType)element;
 		PropertyClass type = key.type();
 		switch(type)
 		{
 			case FONT:				
 			case STRING:
 			case DOUBLE:
 			case ANGLE:
 			case INTEGER: 	return textEditor;
 			case COLOR: 	return colorEditor;
 			case LINETYPE:
 				linetype_names = LineType.getNames();
 				comboBoxEditor.setItems(linetype_names);
 				return comboBoxEditor;
 			case OUTLINETYPE:
 				comboBoxEditor.setItems(outlinetype_names);
 				return comboBoxEditor;
 			case SHAPETYPE:
 				shape_names = ShapeType.getNames();
 				comboBoxEditor.setItems(shape_names);
 				return comboBoxEditor;
 			case DATASOURCE:
 				//refresh datasource info.
 				//get a fresh list, get the full names and sort alphabetically.
 			{
				List<String> fullNames = new ArrayList<String>();
				for (String s : DataSource.getFullNames())
				{
					if (s != null) { fullNames.add (s); }
				}
 				Collections.sort(fullNames);
 				datasource_names = fullNames.toArray (new String[0]);
 			}
 				comboBoxEditor.setItems(datasource_names);
 				return comboBoxEditor;
 			case ORIENTATION:
 				comboBoxEditor.setItems(orientation_names);
 				return comboBoxEditor;
 			case LINESTYLE:
 				comboBoxEditor.setItems(linestyle_names);
 				return comboBoxEditor;
 			case BOOLEAN:
 				comboBoxEditor.setItems(boolean_names);
 				return comboBoxEditor;
 			case ORGANISM:
 				comboBoxEditor.setItems(Organism.latinNamesArray());
 				return comboBoxEditor;
 			case GENETYPE:
 				comboBoxEditor.setItems(genetype_names);
 				return comboBoxEditor;
 			case DB_ID:
 				return textEditor;
 			case DB_SYMBOL:
 				return textEditor;
 			case COMMENTS:
 				return commentsEditor;
 		}
 		return textEditor;
 	}
 	
 	private ICellModifier cellModifier = new ICellModifier()
 	{
 		public boolean canModify(Object element, String property) {
 			if (!colNames[1].equals(property))
 			{
 				return false;
 			}
 			
 			cellEditors[1] = getCellEditor(element);
 			
 			VPathway vp = Engine.getCurrent().getActiveVPathway();
 			if(vp != null) {
 				return vp.isEditMode();
 			}
 			return false;
 		}
 
 		/**
 		 * Getvalue is the value that is passed to the Cell Editor when it is 
 		 * activated.
 		 * It should return an Integer object for ComboboxCellEditors.
 		 */
 		public Object getValue(Object element, String property) 
 		{
 			PropertyType key = (PropertyType)element;
 			Object value = getAggregateValue(key);
 			switch(key.type())
 			{
 				case ANGLE:
 					if(value instanceof Double)
 						value = Math.round((Double)(value) * 1800.0 / Math.PI) / 10.0;
 					break;
 				case DOUBLE:
 					if(value instanceof Double)
 						value = Math.round((Double)(value) * 100.0) / 100.0;
 					break;
 				case ORGANISM:
 					return Organism.latinNames().indexOf(value.toString());
 				case GENETYPE:
 					return Arrays.asList(genetype_names).indexOf(value.toString());
 				case DATASOURCE:
 					if (value == null)
 						return 0;
 					else
 						return Arrays.asList(datasource_names).indexOf(value.toString());				
 				// for all combobox types:
 				case BOOLEAN:
 					if(value instanceof Boolean)
 						return ((Boolean)value) ? 1 : 0;
 					else
 						return 0;
 				case SHAPETYPE:
 					if(value instanceof ShapeType)
 						return (((ShapeType)value).getOrdinal());
 					else
 						return 0;
 				case LINETYPE:
 					if(value instanceof LineType)
 						return (((LineType)value).getOrdinal());
 					else
 						return 0;
 			    case OUTLINETYPE:
 			    	if(value instanceof OutlineType)
 			    		return (((OutlineType)value).ordinal());
 					else
 						return 0;
 				case COLOR:
 					if(value instanceof Color)
 						value = SwtUtils.color2rgb((Color)value);
 					if(value instanceof RGB)
 						return (RGB)value;
 					else
 						return new RGB(0, 0, 0);//ColorEditor can't handle string
 					
 				case ORIENTATION:
 				case LINESTYLE:
 					if(value instanceof Integer)
 						return (Integer)value;
 					else
 						return 0;
 				case DB_ID:
 				case DB_SYMBOL:
 					if(value instanceof PropertyPanel.AutoFillData) 
 						return ((PropertyPanel.AutoFillData)value).getMainValue();
 					break;
 				case BIOPAXREF:
 				case COMMENTS:
 					return value;
 			}
 			//We can get here because:
 			// - the property type is a string
 			// - the property type is not recognised, safest is to return a string
 			// - the values were different, return the 'different values' string
 			return value == null ? "" : value.toString();
 		}
 		
 		public void modify(Object element, String property, Object value) {
 			PropertyType key = (PropertyType)((TableItem)element).getData();
 			
 			if(value == VALUE_DIFF || value == VALUE_DIFF.toString()) {
 				return;
 			}
 			/*
 			 * Here, we transform the output of the cell editor
 			 * to a value understood by PathwayElement.SetProperty().
 			 * 
 			 * The output of a comboboxCellEditor is Integer.
 			 * The output of a textCellEditor is String.
 			 * 
 			 * For linetype and shapetype we go from Integer to Integer. easy
 			 * For boolean, we go from Integer to Boolean
 			 * For Double / Integer, we go from String to Double
 			 * For Datasource, we go from Integer to String.
 			 */
 			switch(key.type())
 			{
 			case ANGLE: 	
 				try 
 				{ 
 					// convert degrees (property editor) to radians (model)
 					value = Double.parseDouble((String)value) * Math.PI / 180;					
 					break;
 				} 
 				catch(Exception e) 
 				{
 					// invalid input, ignore
 					return; 
 				}
 			case DOUBLE: 	
 				try 
 				{ 
 					value = Double.parseDouble((String)value); 
 					break; 
 				} 
 				catch(Exception e) 
 				{
 					// invalid input, ignore
 					return; 
 				}
 			case INTEGER: 	
 				try 
 				{ 
 					value = Integer.parseInt((String)value); 
 					break; 
 				}
 				catch(Exception e) 
 				{ 
 					// invalid input, ignore 
 					return; 
 				}
 			case DATASOURCE:
 				if((Integer)value == -1) return; //Nothing selected
 				value = datasource_names[(Integer)value];
 				break;
 			case BOOLEAN:
 				if ((Integer)value == 0)
 				{
 					value = new Boolean (false);
 				}
 				else
 				{
 					value = new Boolean (true);
 				}
 				break;
 			case ORGANISM:
 				if((Integer)value == -1) return; //Nothing selected
 				value = Organism.latinNames().get((Integer)value);
 				break;
 			case GENETYPE:
 				if((Integer)value == -1) return; //Nothing selected
 				value = genetype_names[(Integer)value];
 				break;
 			case COLOR:
 				value = SwtUtils.rgb2color((RGB)value);
 			case DB_SYMBOL:
 			case DB_ID:
 				if(value instanceof PropertyPanel.AutoFillData) {
 					PropertyPanel.AutoFillData adf = (PropertyPanel.AutoFillData)value;
 					for(PathwayElement o : dataObjects) {
 						if(o.getObjectType() == ObjectType.DATANODE) {
 							adf.fillData(o);
 						}
 					}
 					value = adf.getMainValue();
 				}
 				break;
 			}
 			Engine.getCurrent().getActiveVPathway().getUndoManager().newAction ("Change " + key + " property");
 			for(PathwayElement o : dataObjects) {
 				o.setProperty(key, value);
 			}
 			tableViewer.refresh();
 			Engine.getCurrent().getActiveVPathway().redrawDirtyRect();
 		}
 	};
 	
 	private IStructuredContentProvider tableContentProvider = new ArrayContentProvider();
 	
 	private ITableLabelProvider tableLabelProvider = new ITableLabelProvider() {
 		public Image getColumnImage(Object element, int columnIndex) {
 			return null;
 		}
 		public String getColumnText(Object element, int columnIndex) {
 			PropertyType key = (PropertyType)element;
 			switch(columnIndex) {
 				case 0:
 					return key.desc();					
 				case 1:
 					//TODO: prettier labels for different value types
 					if(attributes.contains(key))
 					{
 						Object value = getAggregateValue(key);
 						if (value == null)
 						{
 							return null;
 						}
 						else 
 						{
 							switch (key.type())
 							{
 								case ANGLE:
 								{
 									if (value instanceof Double)
 									{
 										Double x = Math.round((Double)(value) * 1800.0 / Math.PI) / 10.0;
 										return x.toString();
 									}
 									else
 										return value.toString();
 								}
 								case DOUBLE:								
 									if (value instanceof Double)
 									{
 										Double x = Math.round((Double)(value) * 10.0) / 10.0;
 										return x.toString();
 									}
 									else
 										return value.toString();
 										
 								case BOOLEAN:
 								{
 									if (value instanceof Boolean)
 									{
 										return (Boolean)(value) ? "true" : "false";
 									}
 									else
 										return value.toString();
 								}
 								case LINETYPE:
 								{
 									assert (linetype_names != null);
 									if (value instanceof Integer)
 										return linetype_names[(Integer)(value)];
 									else
 										return value.toString();
 								}
 								case OUTLINETYPE:
 								{
 									if (value instanceof Integer)
 										return outlinetype_names[(Integer)(value)];
 									else
 										return value.toString();
 								}
 								case LINESTYLE:
 								{
 									if (value instanceof Integer)
 										return linestyle_names[(Integer)(value)];
 									else
 										return value.toString();
 								}
 								case ORIENTATION:
 								{
 									if (value instanceof Integer)
 										return orientation_names[(Integer)(value)];
 									else
 										return value.toString();									
 								}
 								case SHAPETYPE:
 								{
 									assert (shape_names != null);
 									if (value instanceof Integer)
 										return shape_names[(Integer)(value)];
 									else
 										return value.toString();
 								}
 								case COLOR:
 									if(value instanceof Color) {
 										return SwtUtils.color2rgb((Color)value).toString();
 									}
 								default:
 									return value.toString();
 							}
 						}
 					}
 			}
 			return null;
 			}
 		
 		public void addListener(ILabelProviderListener listener) { }
 		public void dispose() {}
 		public boolean isLabelProperty(Object element, String property) {
 			return false;
 		}
 		public void removeListener(ILabelProviderListener listener) { }
 	};
 
 	public void gmmlObjectModified(PathwayEvent e) {
 		tableViewer.refresh();
 	}
 
 	//TODO: implement all attribute types as subclasses of MyType.
 //	class MyType {
 //		abstract String getColumnText(Object value);
 //		abstract Object adjustedValue(Object value);
 //		abstract CellEditor getCellEditor()
 //	}
 	
 	public void selectionEvent(SelectionEvent e) {
 		switch(e.type) {
 		case SelectionEvent.OBJECT_ADDED:
 			if(e.affectedObject instanceof Graphics)
 				addGmmlDataObject(((Graphics)e.affectedObject).getPathwayElement());
 			break;
 		case SelectionEvent.OBJECT_REMOVED:
 			if(e.affectedObject instanceof Graphics)
 				removeGmmlDataObject(((Graphics)e.affectedObject).getPathwayElement());
 			break;
 		case SelectionEvent.SELECTION_CLEARED:
 			 clearGmmlDataObjects();
 			break;
 		}
 		
 	}
 
 	static class AutoFillData {
 		PropertyType mProp;
 		Object mValue;
 		HashMap<PropertyType, String> values;
 		
 		private boolean doGuess = false;
 		
 		public AutoFillData(PropertyType mainProperty, String mainValue) {
 			values = new HashMap<PropertyType, String>();
 			mProp = mainProperty;
 			mValue = mainValue;
 			setProperty(mainProperty, mainValue);
 		}
 		
 		public void setProperty(PropertyType property, String value) {
 			values.put(property, value);
 		}
 		
 		public PropertyType getMainProperty() { return mProp; }
 		public Object getMainValue() { return mValue; }
 		
 		public String getProperty(PropertyType property) { return values.get(property); }
 		
 		public Set<PropertyType> getProperties() { return values.keySet(); }
 		
 		public void fillData(PathwayElement o) {
 			if(doGuess) guessData(o);
 			for(PropertyType p : getProperties()) {
 				Object vNew = getProperty(p);
 				o.setProperty(p, vNew);
 			}
 		}
 		
 		public void setDoGuessData(boolean doGuessData) {
 			doGuess = doGuessData;
 		}
 		
 		protected void guessData(PathwayElement o) {
 		}
 	}
 
 	public void applicationEvent(ApplicationEvent e) {
 		if(e.getType() == ApplicationEvent.VPATHWAY_CREATED) {
 			((VPathway)e.getSource()).addSelectionListener(this);
 		}
 	}
 }
 
