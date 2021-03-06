 /*
  * HUMBOLDT: A Framework for Data Harmonisation and Service Integration.
  * EU Integrated Project #030962                 01.10.2006 - 30.09.2010
  * 
  * For more information on the project, please refer to the this web site:
  * http://www.esdi-humboldt.eu
  * 
  * LICENSE: For information on the license under which this program is 
  * available, please refer to http:/www.esdi-humboldt.eu/license.html#core
  * (c) the HUMBOLDT Consortium, 2007 to 2011.
  */
 
 package eu.esdihumboldt.hale.ui.views.data.internal.tree;
 
 import java.text.MessageFormat;
 
 import org.eclipse.jface.viewers.BaseLabelProvider;
 import org.eclipse.jface.viewers.CellLabelProvider;
 import org.eclipse.jface.viewers.StyledCellLabelProvider;
 import org.eclipse.jface.viewers.StyledString;
 import org.eclipse.jface.viewers.TreePath;
 import org.eclipse.jface.viewers.ViewerCell;
 
 import eu.esdihumboldt.hale.instance.model.Group;
 import eu.esdihumboldt.hale.instance.model.Instance;
 import eu.esdihumboldt.hale.schema.model.ChildDefinition;
 import eu.esdihumboldt.hale.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.schema.model.constraint.type.HasValueFlag;
 import eu.esdihumboldt.hale.ui.common.definition.DefinitionImages;
 
 /**
  * Label provider for instances in a tree based on a 
  * {@link TypeDefinitionContentProvider}
  * @author Simon Templer
  */
 public class DefinitionInstanceLabelProvider extends StyledCellLabelProvider {
 
 	private final Instance instance;
 	
 	private final DefinitionImages images = new DefinitionImages();
 	
 	/**
 	 * Create an instance label provider for tree based on a 
 	 * {@link TypeDefinition} 
 	 * @param instance the instance to use 
 	 */
 	public DefinitionInstanceLabelProvider(Instance instance) {
 		super();
 		
 		this.instance = instance;
 	}
 
 	/**
 	 * @see CellLabelProvider#update(ViewerCell)
 	 */
 	@Override
 	public void update(ViewerCell cell) {
 		TreePath treePath = cell.getViewerRow().getTreePath();
 		
 		// descend in instance
 		int otherValues = 0;
 		Object value = instance;
 		for (int i = 0; value != null && i < treePath.getSegmentCount(); i++) {
 			Object segment = treePath.getSegment(i);
 			if (segment instanceof ChildDefinition<?>) {
 				ChildDefinition<?> child = (ChildDefinition<?>) segment;
 				Object[] values = ((Group) value).getProperty(child.getName());
 				if (values != null && values.length > 0) {
 					value = values[0];
 					//FIXME what about the other values? XXX mark cell? XXX create button for cell to see all for this instance?
 					otherValues = values.length - 1;
 				}
 				else {
 					value = null;
 				}
 			}
 			else {
 				//TODO log message?
 				value = null;
 			}
 		}
 		
		boolean hasValue = false;
		if (value instanceof Instance) {
			hasValue = ((Instance) value).getValue() != null;
		}
		
 		StyledString styledString;
 		if (value == null) {
 			styledString = new StyledString("no value", StyledString.DECORATIONS_STYLER);
 		}
		else if (value instanceof Group && !hasValue) {
 			styledString = new StyledString("+", StyledString.QUALIFIER_STYLER);
 		}
 		else {
			if (value instanceof Instance) {
				value = ((Instance) value).getValue();
			}
 			//TODO some kind of conversion?
 			styledString = new StyledString(value.toString(), null);
 		}
 		
 		// mark cell if there are other values
 		if (otherValues > 0) {
 			String decoration = " " + MessageFormat.format("(1 of {0})", 
 					new Object[] { Integer.valueOf(otherValues + 1) });
 			styledString.append(decoration, StyledString.COUNTER_STYLER);
 		}
 		
 		cell.setText(styledString.toString());
 		cell.setStyleRanges(styledString.getStyleRanges());
 		
 		//XXX use definition images?
 //		Object lastSegment = treePath.getLastSegment();
 //		if (lastSegment instanceof Definition) {
 //			cell.setImage(images.getImage((Definition<?>) lastSegment));
 //		}
 		
 //		cell.setText(getText(element));
 //		Image image = getImage(element);
 //		cell.setImage(image);
 //		cell.setBackground(getBackground(element));
 //		cell.setForeground(getForeground(element));
 //		cell.setFont(getFont(element));
 		
 		super.update(cell);
 	}
 
 	/**
 	 * @see BaseLabelProvider#dispose()
 	 */
 	@Override
 	public void dispose() {
 		images.dispose();
 		
 		super.dispose();
 	}
 
 	//TODO override some of the tooltip methods?!
 	
 }
