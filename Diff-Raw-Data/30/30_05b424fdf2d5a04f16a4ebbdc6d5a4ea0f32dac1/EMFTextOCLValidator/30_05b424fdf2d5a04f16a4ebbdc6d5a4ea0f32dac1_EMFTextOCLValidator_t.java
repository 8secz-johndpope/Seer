 package org.reuseware.emftextedit.runtime.resource;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IMarker;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.emf.common.notify.AdapterFactory;
 import org.eclipse.emf.common.util.EList;
 import org.eclipse.emf.common.util.TreeIterator;
 import org.eclipse.emf.ecore.EAnnotation;
 import org.eclipse.emf.ecore.EClass;
 import org.eclipse.emf.ecore.EObject;
 import org.eclipse.emf.ecore.EStructuralFeature;
 import org.eclipse.emf.ecore.resource.Resource;
 import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
 import org.eclipse.emf.edit.provider.IItemLabelProvider;
 import org.eclipse.emf.edit.provider.ReflectiveItemProviderAdapterFactory;
 import org.eclipse.emf.workspace.util.WorkspaceSynchronizer;
 import org.eclipse.ocl.ParserException;
 import org.eclipse.ocl.Query;
 import org.eclipse.ocl.ecore.OCL;
 import org.eclipse.ocl.ecore.OCLExpression;
 import org.eclipse.ocl.util.Tuple;
 
 public class EMFTextOCLValidator {
 
 	private static final AdapterFactory reflectiveAdapterFactory = new ReflectiveItemProviderAdapterFactory();
 
 	private static final AdapterFactory defaultAdapterFactory = new ComposedAdapterFactory(
 			ComposedAdapterFactory.Descriptor.Registry.INSTANCE);
 
 	private TextResource textResource;
 
 	public void analyse(EObject rootObject) {
 
 		Resource resource = rootObject.eResource();

		// Since the resource is loaded with ETE
		// it is necessarily a TextResource
 		if (resource instanceof TextResource) {
 			textResource = (TextResource) resource;
		} 
 		TreeIterator<EObject> allContents = resource.getAllContents();
 
 		while (allContents.hasNext()) {
 			evaluate((EObject) allContents.next());
 		}
 	}
 
 	private Collection<EClass> getAllMetaclasses(EObject object) {
 		Set<EClass> metaclasses = new HashSet<EClass>();
 		EClass metaclass = object.eClass();
 		metaclasses.add(metaclass);
 		EList<EClass> superTypes = metaclass.getESuperTypes();
 		metaclasses.addAll(superTypes);
 		for (EClass supertype : superTypes) {
 			metaclasses.addAll(collectSupertypes(supertype));
 		}
 		return metaclasses;
 	}
 
 	private Collection<EClass> collectSupertypes(EClass type) {
 		Set<EClass> metaclasses = new HashSet<EClass>();
 		EList<EClass> superTypes = type.getESuperTypes();
 		metaclasses.addAll(superTypes);
 		for (EClass supertype : superTypes) {
 			metaclasses.addAll(collectSupertypes(supertype));
 		}
 		return metaclasses;
 	}
 
 	private Object evaluateOCL(Object context, String oclQuery) throws ParserException {
 		Object eval = null;
 		OCL ocl = OCL
 				.newInstance(org.eclipse.ocl.ecore.EcoreEnvironmentFactory.INSTANCE);
 		OCL.Helper helper = ocl.createOCLHelper();
 		
 		helper.setInstanceContext(context);
 		OCLExpression exp = helper.createQuery(oclQuery);
 		Query<?, ?, ?> query = OCL.newInstance().createQuery(exp);
 
 		eval = query.evaluate(context);
 		
 		return eval;
 	}
 
 	private void evaluate(EObject targetObject) {
 		List<EAnnotation> annotations = new ArrayList<EAnnotation>();
 		Collection<EClass> metaclasses = getAllMetaclasses(targetObject);
 
 		for (EClass metaclass : metaclasses) {
 			annotations.addAll(metaclass.getEAnnotations());
 		}
 
 		for (EAnnotation annotation : annotations) {
 
 			if (annotation.getSource().equals("OCL")) {
 				for (String key : annotation.getDetails().keySet()) {
 					String value = annotation.getDetails().get(key);
 					Object result;
 					String errorMsg = "";
 					try {
 						result = evaluateOCL(targetObject, value);
 						
 						if ((result instanceof Boolean)
 								&& ((Boolean) result).booleanValue() == false) {
 							errorMsg = key.toString();
 						} else {
 							errorMsg = constructErrorMsg(result);
 							if (errorMsg.length() > 0) {
 								errorMsg = key + ": " + errorMsg;
 							}
 						}
 					} catch (ParserException e) {
 						errorMsg += "Parse Error for OCL Expression: " + value;
 					}
 					
 
 					if (errorMsg.length() > 0) {
 						addErrorMessage(targetObject, errorMsg);
 					}
 				}
 			} else if (annotation.getSource().equals("OCL_Derivation")) {
 				for (String key : annotation.getDetails().keySet()) {
 					String value = annotation.getDetails().get(key);
 					Object result;
 					String errorMsg = "";
 					try {
 						result = evaluateOCL(targetObject, value);
 						EStructuralFeature structuralFeature = targetObject
 								.eClass().getEStructuralFeature(key);
 						if (structuralFeature != null && result != null) {
 							targetObject.eSet(structuralFeature, result);
 						}
 					} catch (ParserException e) {
 						errorMsg += "Parse Error for OCL Expression: " + value;
 					}
 					if (errorMsg.length() > 0) {
 						addErrorMessage(targetObject, errorMsg);
 					}
 				}
 
 			}
 		}
 	}
 
 	
 	/**
 	 * @param targetObject
 	 * @param resourceFile
 	 * @param errorMesssage
 	 */
 	private void addErrorMessage(EObject targetObject, String errorMesssage) {
 		if (textResource != null) {
 			textResource.addError(errorMesssage, targetObject);
 		}

 	}
 	
 
 	private String constructErrorMsg(Object result) {
 		String error = "";
 		OCL ocl = OCL
 				.newInstance(org.eclipse.ocl.ecore.EcoreEnvironmentFactory.INSTANCE);
 		if (result instanceof Boolean && ((Boolean) result).booleanValue() == true) {
 			return "";
 		}
 		else if (ocl.isInvalid(result)) {
 			error += "Evaluation of OCL expression failed: OclInvalid";
 		} 
 		else if (result instanceof String) {
 			error = "'" + result + "'";
 		} 
 		else if (result instanceof Tuple) {
 			Object[] array = ((Tuple<?,?>) result).getTupleType().oclProperties().toArray(new Object[]{});
 			if (array.length > 0) {
 				error += constructErrorMsg(array[0]);
 			}
 			for (int i = 1; i < array.length; i++) {
 				error += ", " + constructErrorMsg(array[i]);
 			}
 		} 
 		else if (result instanceof Collection) { 
 			Object[] array = ((Collection<?>) result).toArray(new Object[]{});
 			if (array.length > 0) {
 				error += constructErrorMsg(array[0]);
 			}
 			for (int i = 1; i < array.length; i++) {
 				error += ", " + constructErrorMsg(array[i]);
 			}
 			
 		}
 		else if (result instanceof EObject) {
 			EObject eObject = (EObject) result;
 
 			IItemLabelProvider labeler = (IItemLabelProvider) defaultAdapterFactory
 					.adapt(eObject, IItemLabelProvider.class);
 
 			if (labeler == null) {
 				labeler = (IItemLabelProvider) reflectiveAdapterFactory.adapt(
 						eObject, IItemLabelProvider.class);
 			}
 
 			if (labeler != null) {
 				error += labeler.getText(result);
 			}
 		}
 
 		return error.trim();
 	}
 }
