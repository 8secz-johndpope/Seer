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
 
 package eu.esdihumboldt.hale.io.xsd.reader.internal;
 
 import java.util.ArrayList;
 import java.util.Collection;
import java.util.HashSet;
 import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Queue;
import java.util.Set;
 
 import javax.xml.namespace.QName;
 
 import com.google.common.base.Preconditions;
 
 import eu.esdihumboldt.hale.io.xsd.constraint.XmlElements;
 import eu.esdihumboldt.hale.io.xsd.model.XmlElement;
 import eu.esdihumboldt.hale.schema.model.ChildDefinition;
 import eu.esdihumboldt.hale.schema.model.DefinitionGroup;
 import eu.esdihumboldt.hale.schema.model.PropertyDefinition;
 import eu.esdihumboldt.hale.schema.model.TypeDefinition;
 import eu.esdihumboldt.hale.schema.model.constraint.property.Cardinality;
 import eu.esdihumboldt.hale.schema.model.constraint.property.ChoiceFlag;
 import eu.esdihumboldt.hale.schema.model.constraint.type.AbstractFlag;
 import eu.esdihumboldt.hale.schema.model.impl.DefaultGroupPropertyDefinition;
 import eu.esdihumboldt.hale.schema.model.impl.DefaultPropertyDefinition;
 
 /**
  * Group property that resolves all possible substitutions for a property
  * and offers them as a choice.
  * The property must be set using {@link #setProperty(DefaultPropertyDefinition)}-
  * @author Simon Templer
  */
 public class SubstitutionGroupProperty extends LazyGroupPropertyDefinition {
 
 	private DefaultPropertyDefinition property;
 	
 	/**
 	 * The 
 	 * 
 	 * @param name the property name
 	 * @param parentGroup the parent group
 	 */
 	public SubstitutionGroupProperty(QName name, DefinitionGroup parentGroup) {
 		super(name, parentGroup, null, false);
 		
 		setConstraint(ChoiceFlag.ENABLED);
 	}
 
 	/**
 	 * Set the property represented by the group. The property must have been 
 	 * created with this group as parent and the {@link Cardinality} constraint
 	 * must have been already set.
 	 * @param property the property to set
 	 */
 	public void setProperty(DefaultPropertyDefinition property) {
 		Preconditions.checkArgument(property.getDeclaringGroup() == this);
 		
 		this.property = property;
 		
 		// apply cardinality to group
 		setConstraint(property.getConstraint(Cardinality.class));
 		// set cardinality to exactly one for the property
 		property.setConstraint(Cardinality.CC_EXACTLY_ONCE);
 	}
 
 	/**
 	 * @see DefaultGroupPropertyDefinition#addChild(ChildDefinition)
 	 */
 	@Override
 	public void addChild(ChildDefinition<?> child) {
 		// do nothing
 		// prevents a property being added manually
 	}
 
 	/**
 	 * @see LazyGroupPropertyDefinition#initChildren()
 	 */
 	@Override
 	protected void initChildren() {
 		if (property != null) {
 			TypeDefinition propertyType = property.getPropertyType();
 			
 			// add property and substitutions
 				
 			// collect substitution types and elements
 			List<XmlElement> substitutions = collectSubstitutions(property.getName(), propertyType);
 			
 			if (substitutions == null || substitutions.isEmpty()) {
 				// add property (XXX even if the property type is abstract)
 				super.addChild(property); // no redeclaration necessary as this is already the declaring group
 			}
 			else {
 				// add property if the type is not abstract
 				if (!propertyType.getConstraint(AbstractFlag.class).isEnabled()) {
 					super.addChild(property); // no redeclaration necessary as this is already the declaring group
 				}
 				
 				// add substitutions
 				for (XmlElement substitution : substitutions) {
 					PropertyDefinition p = new SubstitutionProperty(
 							substitution, property, this);
 					super.addChild(p); // must call super add
 				}
 			}
 		}
 		// else empty group
 	}
 
 	/**
 	 * Collect all sub-types from the given type that may substitute it on
 	 * condition of the given element name.
 	 * @param elementName the element name 
 	 * @param type the type to be substituted
 	 * @return the substitution types
 	 */
 	private List<XmlElement> collectSubstitutions(QName elementName, TypeDefinition type) {
		Set<QName> substitute = new HashSet<QName>();
		substitute.add(elementName);
 		Queue<TypeDefinition> subTypes = new LinkedList<TypeDefinition>();
 		subTypes.addAll(type.getSubTypes());
 		
 		List<XmlElement> result = new ArrayList<XmlElement>();
 		
 		while (!subTypes.isEmpty()) {
 			TypeDefinition subType = subTypes.poll();
 			
 			// check the declared elements for the substitution group
 			Collection<? extends XmlElement> elements = subType.getConstraint(XmlElements.class).getElements();
 			Iterator<? extends XmlElement> it = elements.iterator();
 			while (it.hasNext()) {
 				XmlElement element = it.next();
 				QName subGroup = element.getSubstitutionGroup();
				if (subGroup != null && substitute.contains(subGroup)) { // only if substitution group match
					// add element name also to the name that may be substituted
					substitute.add(element.getName());
					if (!element.getType().getConstraint(AbstractFlag.class).isEnabled()) { 
						// only add if type is not abstract
						result.add(element);
					}
 				}
 			}
 			
			//XXX what about using xsi:type?
			//XXX we could also add elements for other sub-types then, e.g. while also adding a specific constraint
			
 			// add the sub-type's sub-types
 			subTypes.addAll(subType.getSubTypes());
 		}
 		
 		return result;
 	}
 
 }
