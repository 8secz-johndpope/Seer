 /*
  * Copyright 2009-2010 WSO2, Inc. (http://wso2.com)
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.wso2.developerstudio.eclipse.esb.presentation.custom;
 
 
 import org.eclipse.emf.edit.provider.IItemPropertyDescriptor;
 import org.eclipse.emf.edit.provider.ItemPropertyDescriptor.PropertyValueWrapper;
 import org.eclipse.emf.edit.ui.provider.PropertyDescriptor;
 import org.eclipse.jface.viewers.CellEditor;
 import org.eclipse.swt.widgets.Composite;
 import org.wso2.developerstudio.eclipse.esb.NamespacedProperty;
 import org.wso2.developerstudio.eclipse.esb.mediators.HeaderMediator;
 import org.wso2.developerstudio.eclipse.esb.presentation.ui.NamespacedPropertyDecoratorHeaderMediator;
 
 /**
  * Custom {@link PropertyDescriptor} class.
  */
 public class CustomPropertyDescriptor extends PropertyDescriptor {
 	
 	/**
 	 * Creates a new {@link CustomPropertyDescriptor} instance.
 	 * 
 	 * @param object property container object.
 	 * @param itemPropertyDescriptor {@link IItemPropertyDescriptor} instance.
 	 */
 	public CustomPropertyDescriptor(Object object, IItemPropertyDescriptor itemPropertyDescriptor) {
 		super(object, itemPropertyDescriptor);
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	public CellEditor createPropertyEditor(Composite composite) {
 		CellEditor result = null;
 		
 //		if (object instanceof HeaderMediator) {
 			PropertyValueWrapper wrapper = (PropertyValueWrapper) itemPropertyDescriptor.getPropertyValue(object);
			if(wrapper!=null && wrapper.getEditableValue(object) instanceof NamespacedProperty){
 				//PropertyValueWrapper wrapper = (PropertyValueWrapper) itemPropertyDescriptor.getPropertyValue(object);
 				
 				NamespacedProperty namespacedProperty = (NamespacedProperty) wrapper.getEditableValue(object);
 				result = new NamespacedPropertyDecoratorHeaderMediator(
 						(CustomPropertyEditorFactory.createCustomPropertyEditor(
 								composite, object, itemPropertyDescriptor)),
 						composite, namespacedProperty, object,itemPropertyDescriptor);
 			} else {
 				result = CustomPropertyEditorFactory.createCustomPropertyEditor(
 						composite, object, itemPropertyDescriptor);
 			}
 			return (null == result) ? super.createPropertyEditor(composite)	: result;
 //			}
 //		else{
 //			
 //			 result = CustomPropertyEditorFactory.createCustomPropertyEditor(composite, object, itemPropertyDescriptor);		
 //			return (null == result) ? super.createPropertyEditor(composite) : result;	
 //		}
 			
 	}
 }
