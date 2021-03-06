 /*******************************************************************************
  * Copyright (c) 2007 Red Hat, Inc.
  * Distributed under license by Red Hat, Inc. All rights reserved.
  * This program is made available under the terms of the
  * Eclipse Public License v1.0 which accompanies this distribution,
  * and is available at http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Red Hat, Inc. - initial API and implementation
  ******************************************************************************/  
 package org.jboss.tools.seam.ui.internal.project.facet;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import org.eclipse.jface.wizard.WizardPage;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.wst.common.frameworks.datamodel.DataModelEvent;
 import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
 import org.eclipse.wst.common.frameworks.datamodel.IDataModelListener;
 
 /**
  * Delegate class used during validation process in wizard dialog. It contains 
  * map from property name to IValidator instance. It is triggered by property 
  * change event from any registered property editor
  * 
  * @author eskimo
  *
  */
 public class DataModelValidatorDelegate implements IDataModelListener {
 	
 	/**
 	 * Target IDataModel instance
 	 */
 	protected IDataModel model = null;
 	
 	/**
 	 * WizardPage instance that should be validated
 	 */
 	protected WizardPage page = null;
 	
 	/**
 	 * Map from property name to IValidator instance
 	 */
 	protected Map<String,IValidator> mapPropToValidator = new HashMap<String,IValidator>();
 
 	private List<String> validationOrder= new ArrayList<String>();
 	
 	/**
 	 * 
 	 * @param model
 	 * @param page
 	 */
 	public DataModelValidatorDelegate(IDataModel model,WizardPage page) {
 		this.model = model;	
 		this.page = page;
 		model.addListener(this);
 	}
 	
 	/**
 	 * 
 	 * @return
 	 */
 	public Map<String, String> validate() {
 		Map<String, String> errors = new HashMap<String,String>();
 		
 		return errors;
 	}
 
 	/**
 	 * 
 	 */
 	public void propertyChanged(DataModelEvent event) {
 		validateUntillError();
 	}

 	/**
 	 * 
 	 */
 	public void validateUntillError() {
 		page.setErrorMessage(getFirstValidationError());
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				page.setPageComplete(page.getErrorMessage()==null);
			}
		});
 		if(page.getErrorMessage()==null) page.setMessage(null);
 	}

 	/**
 	 * 
 	 * @return
 	 */
 	public String getFirstValidationError() {
 		for (String validatorName : validationOrder) {
 			Map<String,String> errors = getValidator(validatorName).validate(
 					model.getProperty(validatorName),model);
 			String message = errors.get(validatorName);	
 			if(message!=null) {
 				return message;
 			}
 		}
 		return null;
 	}
 	
 	/**
 	 * 
 	 * @param name
 	 * @return
 	 */
 	public IValidator getValidator(String name) {
 		IValidator validator = mapPropToValidator.get(name);
 		return validator==null?ValidatorFactory.NO_ERRORS_VALIDATOR:validator;
 	}
 	
 	/**
 	 * 
 	 * @param name
 	 * @param validator
 	 */
 	public void addValidatorForProperty(String name, IValidator validator) {
 		mapPropToValidator.put(name, validator);
 		validationOrder.add(name);
 	}
 }
