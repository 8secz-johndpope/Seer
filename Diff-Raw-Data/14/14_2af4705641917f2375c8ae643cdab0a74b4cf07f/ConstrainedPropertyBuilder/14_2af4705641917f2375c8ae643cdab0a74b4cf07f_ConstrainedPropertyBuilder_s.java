 /* Copyright 2004-2005 the original author or authors.
  * 
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * 
  *      http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */ 
 package org.codehaus.groovy.grails.validation;
 
 import java.beans.PropertyDescriptor;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Map;
 import org.springframework.beans.BeanWrapper;
 import org.springframework.beans.BeanWrapperImpl;
 import org.springframework.beans.InvalidPropertyException;
 
 import groovy.lang.MissingMethodException;
 import groovy.util.BuilderSupport;
 import grails.util.GrailsUtil;
 
 /**
  * Builder used as a delegate within the "constraints" closure of GrailsDomainClass instances 
  * 
  * @author Graeme Rocher
  * @since 10-Nov-2005
  */
 public class ConstrainedPropertyBuilder extends BuilderSupport {
 
 	private Object target;
 	private BeanWrapper bean;
 	private Map constrainedProperties = new HashMap();
 	private int order = 1;
 	
 	public ConstrainedPropertyBuilder(Object target) {
 		super();
 		this.target = target;
 		this.bean = new BeanWrapperImpl(target);
 	}
 
 
 
 	protected Object createNode(Object name, Map attributes) {
 		// we do this so that missing property exception is throw if it doesn't exist
 
         try {
 			String property = (String)name;
 			ConstrainedProperty cp;
 			if(constrainedProperties.containsKey(property)) {
 				cp = (ConstrainedProperty)constrainedProperties.get(property);				
 			}
 			else {
 				PropertyDescriptor pd = this.bean.getPropertyDescriptor(property);
 				cp = new ConstrainedProperty(this.target.getClass(), property, pd.getPropertyType());
 				cp.setOrder(order++);
 				constrainedProperties.put( property, cp );
 			}
 			for (Iterator i = attributes.keySet().iterator(); i.hasNext();) {
 				String constraintName = (String) i.next();
 				if(cp.supportsContraint(constraintName)) {
 					cp.applyConstraint(constraintName, attributes.get(constraintName));
 				} else {
                    GrailsUtil.warn( "Property [" + cp.getPropertyName() + "] of type [" + cp.getPropertyType().getName() + "] doesn't support constraint [" + constraintName + "]. This constraint will not be checked during validation." );
                 }
 			}				
 			return cp;
 		}
 		catch(InvalidPropertyException ipe) {
 			throw new MissingMethodException((String)name,target.getClass(),new Object[]{ attributes});
 		}		
 	}
 
 	protected Object createNode(Object name, Map attributes, Object value) {
 		throw new MissingMethodException((String)name,target.getClass(),new Object[]{ attributes,value});
 	}
 
 	protected void setParent(Object parent, Object child) {
 		// do nothing
 	}	
 	protected Object createNode(Object name) {
 		return createNode(name, Collections.EMPTY_MAP);
 	}
 	
 	protected Object createNode(Object name, Object value) {
 		return createNode(name,Collections.EMPTY_MAP,value);
 	}	
 	
 	public Map getConstrainedProperties() {
 		return this.constrainedProperties;
 	}
 
 }
