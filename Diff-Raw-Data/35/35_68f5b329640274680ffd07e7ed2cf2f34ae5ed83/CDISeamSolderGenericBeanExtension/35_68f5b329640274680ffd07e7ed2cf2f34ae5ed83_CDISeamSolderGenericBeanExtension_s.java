 /******************************************************************************* 
  * Copyright (c) 2011 Red Hat, Inc. 
  * Distributed under license by Red Hat, Inc. All rights reserved. 
  * This program is made available under the terms of the 
  * Eclipse Public License v1.0 which accompanies this distribution, 
  * and is available at http://www.eclipse.org/legal/epl-v10.html 
  * 
  * Contributors: 
  * Red Hat, Inc. - initial API and implementation 
  ******************************************************************************/
 package org.jboss.tools.cdi.seam.solder.core.generic;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.jdt.core.IPackageFragmentRoot;
 import org.eclipse.jdt.core.IType;
 import org.eclipse.jdt.core.JavaModelException;
 import org.jboss.tools.cdi.core.CDIConstants;
 import org.jboss.tools.cdi.core.CDICoreNature;
 import org.jboss.tools.cdi.core.IProducer;
 import org.jboss.tools.cdi.core.IQualifierDeclaration;
 import org.jboss.tools.cdi.core.IRootDefinitionContext;
 import org.jboss.tools.cdi.core.extension.ICDIExtension;
 import org.jboss.tools.cdi.core.extension.IDefinitionContextExtension;
 import org.jboss.tools.cdi.core.extension.feature.IBuildParticipantFeature;
 import org.jboss.tools.cdi.core.extension.feature.IProcessAnnotatedTypeFeature;
 import org.jboss.tools.cdi.core.extension.feature.IValidatorFeature;
 import org.jboss.tools.cdi.internal.core.impl.AnnotationDeclaration;
 import org.jboss.tools.cdi.internal.core.impl.CDIProject;
 import org.jboss.tools.cdi.internal.core.impl.ClassBean;
 import org.jboss.tools.cdi.internal.core.impl.definition.AbstractMemberDefinition;
 import org.jboss.tools.cdi.internal.core.impl.definition.AnnotationDefinition;
 import org.jboss.tools.cdi.internal.core.impl.definition.FieldDefinition;
 import org.jboss.tools.cdi.internal.core.impl.definition.MethodDefinition;
 import org.jboss.tools.cdi.internal.core.impl.definition.ParameterDefinition;
 import org.jboss.tools.cdi.internal.core.impl.definition.TypeDefinition;
 import org.jboss.tools.cdi.internal.core.scanner.FileSet;
 import org.jboss.tools.cdi.internal.core.validation.CDICoreValidator;
 import org.jboss.tools.cdi.seam.solder.core.CDISeamSolderCorePlugin;
 import org.jboss.tools.cdi.seam.solder.core.CDISeamSolderPreferences;
 import org.jboss.tools.cdi.seam.solder.core.Version;
 import org.jboss.tools.common.java.IAnnotationDeclaration;
 import org.jboss.tools.common.java.IJavaAnnotation;
 import org.jboss.tools.common.java.ParametedType;
 import org.jboss.tools.common.java.impl.AnnotationLiteral;
 import org.jboss.tools.common.model.XModelObject;
 import org.jboss.tools.common.preferences.SeverityPreferences;
 
 /**
  * 
  * @author Viacheslav Kabanovich
  *
  */
 public class CDISeamSolderGenericBeanExtension implements ICDIExtension, IBuildParticipantFeature, IProcessAnnotatedTypeFeature, IValidatorFeature, CDIConstants {
 	GenericBeanDefinitionContext context = new GenericBeanDefinitionContext(getVersion());
 
 	public CDISeamSolderGenericBeanExtension() {
 	}
 
 	protected Version getVersion() {
 		return Version.instance;
 	}
 
 	public IDefinitionContextExtension getContext() {
 		return context;
 	}
 
 	public void beginVisiting() {
 	}
 
 	public void visitJar(IPath path, IPackageFragmentRoot root,	XModelObject beansXML) {
 	}
 
 	public void visit(IFile file, IPath src, IPath webinf) {
 	}
 
 	public void buildDefinitions() {
 	}
 
 	public void buildDefinitions(FileSet fileSet) {
 	}
 
 	public void buildBeans(CDIProject target) {
 		CDIProject p = target;
 
 		for (GenericConfiguration c: context.getGenericConfigurations().values()) {
 			//Create fake bean for injection of generic type annotation.
 			AnnotationDefinition genericTypeDef = c.getGenericTypeDefinition();
 			if(genericTypeDef != null) {
 				TypeDefinition fakeGenericType = new TypeDefinition();
 				fakeGenericType.setType(genericTypeDef.getType(), context.getRootContext(), 0);
 				fakeGenericType.setBeanConstructor(true);
 				ClassBean b = new ClassBean();
 				b.setDefinition(fakeGenericType);
 				b.setParent(p);
 				p.addBean(b);				
 			}
 
 			Map<AbstractMemberDefinition, List<IQualifierDeclaration>> ms = c.getGenericConfigurationPoints();
 
 			Set<TypeDefinition> ts = c.getGenericBeans();
 			for (AbstractMemberDefinition gp: ms.keySet()) {
 				//check veto
 				if(gp.getTypeDefinition().isVetoed()) {
 					continue;
 				}
 				List<IQualifierDeclaration> list = ms.get(gp);
 				for (TypeDefinition t: ts) {
 					TypeDefinition ti = new TypeDefinition(); //TODO copy, do not create new.
 					ti.setType(t.getType(), context.getRootContext(), 0);
 					List<MethodDefinition> ps = ti.getMethods();
 					for (MethodDefinition m: ps) {
 						if(m.isAnnotationPresent(PRODUCES_ANNOTATION_TYPE_NAME)
 								|| m.isAnnotationPresent(getVersion().getUnwrapsAnnotationTypeName())) {
 							for (IAnnotationDeclaration d: list) {
 								m.addAnnotation(((AnnotationDeclaration)d).getDeclaration(), context.getRootContext());
 							}
 						}
 					}
 					List<FieldDefinition> fs = ti.getFields();
 					for (FieldDefinition f: fs) {
 						if(f.isAnnotationPresent(PRODUCES_ANNOTATION_TYPE_NAME)) {
 							for (IAnnotationDeclaration d: list) {
 								f.addAnnotation(((AnnotationDeclaration)d).getDeclaration(), context.getRootContext());
 							}
 						}
 					}
 					replaceGenericInjections(ti, list);
 
 					GenericClassBean cb = new GenericClassBean(getVersion());
 					cb.setGenericProducerBeanDefinition(gp);
 					cb.setParent(p);
 					cb.setDefinition(ti);
 					
 					p.addBean(cb);
					Set<IProducer> producers = cb.getProducers();
					for (IProducer producer: producers) {
 						p.addBean(producer);
 					}
 				}
 			}
 		}
 	}
 
 	private void replaceGenericInjections(TypeDefinition ti, List<IQualifierDeclaration> list) {
 		List<FieldDefinition> fs = ti.getFields();
 		for (FieldDefinition f: fs) {
 			if(f.isAnnotationPresent(INJECT_ANNOTATION_TYPE_NAME) && f.isAnnotationPresent(getVersion().getGenericQualifierAnnotationTypeName())) {
 				for (IAnnotationDeclaration d: list) {
 					f.addAnnotation(((AnnotationDeclaration)d).getDeclaration(), context.getRootContext());
 				}
 				AnnotationDeclaration gd = f.getAnnotation(getVersion().getGenericQualifierAnnotationTypeName());
 				f.removeAnnotation(gd);
 				IJavaAnnotation ja = createInjectGenericAnnotation(gd, context.getRootContext().getProject());
 				if(ja != null) {
 					f.addAnnotation(ja, context.getRootContext());
 				}
 			}
 		}
 		
 		List<MethodDefinition> ms = ti.getMethods();
 		for (MethodDefinition m: ms) {
 			boolean isObserver = m.isObserver();
 			if(m.isAnnotationPresent(INJECT_ANNOTATION_TYPE_NAME) || isObserver) {
 				boolean isMethodGeneric = m.isAnnotationPresent(getVersion().getGenericQualifierAnnotationTypeName());
 				List<ParameterDefinition> ps = m.getParameters();
 				for (ParameterDefinition p: ps) {
 					if(isMethodGeneric || p.isAnnotationPresent(getVersion().getGenericQualifierAnnotationTypeName())
 						|| (isObserver && p.isAnnotationPresent(OBSERVERS_ANNOTATION_TYPE_NAME))) {
 						for (IAnnotationDeclaration d: list) {
 							p.addAnnotation(((AnnotationDeclaration)d).getDeclaration(), context.getRootContext());
 						}
 						AnnotationDeclaration gd = p.getAnnotation(getVersion().getGenericQualifierAnnotationTypeName());
 						if(gd != null) {
 							p.removeAnnotation(gd);
 							IJavaAnnotation ja = createInjectGenericAnnotation(gd, context.getRootContext().getProject());
 							if(ja != null) {
 								p.addAnnotation(ja, context.getRootContext());
 							}
 						}
 					}
 				}
 			}
 		}
 	}
 
 	private IJavaAnnotation createInjectGenericAnnotation(AnnotationDeclaration genericAnnotation, CDICoreNature project) {
 		IType type =  project.getType(getVersion().getInjectGenericAnnotationTypeName());
 		return (type != null) ? new AnnotationLiteral(genericAnnotation.getResource(), 
 				genericAnnotation.getStartPosition(), genericAnnotation.getLength(), null, 0, type)
 			: null;
 	}
 
 	@Override
 	public void processAnnotatedType(TypeDefinition typeDefinition, IRootDefinitionContext context) {
 		if(typeDefinition.isAnnotationPresent(getVersion().getVetoAnnotationTypeName())) {
 			//ignore
 		} else if(typeDefinition.isAnnotationPresent(getVersion().getGenericConfigurationAnnotationTypeName())) {
 			typeDefinition.veto();
 			IAnnotationDeclaration d = typeDefinition.getAnnotation(getVersion().getGenericConfigurationAnnotationTypeName());
 			Object o = d.getMemberValue(null);
 			if(o != null) {
 				String s = o.toString();
 				if(s.length() > 0) {
 					try {
 						ParametedType p = context.getProject().getTypeFactory().getParametedType(typeDefinition.getType(), "Q" + s + ";");
 						if(p != null && p.getType() != null) {
 							GenericConfiguration c = ((GenericBeanDefinitionContext)this.context.getWorkingCopy()).getGenericConfiguration(p.getType().getFullyQualifiedName());
 							c.getGenericBeans().add(typeDefinition);
 							addToDependencies(c, typeDefinition, context);
 						}
 					} catch (JavaModelException e) {
 						CDISeamSolderCorePlugin.getDefault().logError(e);
 					}
 				}				
 			}
 		} else {
 			addGenericProducerBean(typeDefinition, context);
 			for (MethodDefinition m: typeDefinition.getMethods()) {
 				if(m.isAnnotationPresent(PRODUCES_ANNOTATION_TYPE_NAME)) {
 					addGenericProducerBean(m, context);
 				}
 			}
 			for (FieldDefinition f: typeDefinition.getFields()) {
 				if(f.isAnnotationPresent(PRODUCES_ANNOTATION_TYPE_NAME)) {
 					addGenericProducerBean(f, context);
 				}
 			}
 		}
 	}
 
 	private void addGenericProducerBean(AbstractMemberDefinition def, IRootDefinitionContext context) {
 		IAnnotationDeclaration d = findAnnotationAnnotatedWithGenericType(def);
 		if(d != null) {
 			addGenericProducerBean(def, d.getTypeName(), context);
 		}
 	}
 
 	private void addGenericProducerBean(AbstractMemberDefinition def, String genericType, IRootDefinitionContext context) {
 		GenericConfiguration c = ((GenericBeanDefinitionContext)this.context.getWorkingCopy()).getGenericConfiguration(genericType);
 
 		List<IQualifierDeclaration> list = new ArrayList<IQualifierDeclaration>();
 		List<IAnnotationDeclaration> ds = def.getAnnotations();
 		for (IAnnotationDeclaration d: ds) {
 			if(d instanceof IQualifierDeclaration) {
 				list.add((IQualifierDeclaration)d);
 			}
 		}
 		c.getGenericConfigurationPoints().put(def, list);
 		addToDependencies(c, def, context);
 	}
 
 	private void addToDependencies(GenericConfiguration c, AbstractMemberDefinition def, IRootDefinitionContext context) {
 		IResource r = def.getResource();
 		if(r != null && r.exists() && !c.getInvolvedTypes().contains(r.getFullPath())) {
 			IPath newPath = r.getFullPath();
 			Set<IPath> ps = c.getInvolvedTypes();
 			for (IPath p: ps) {
 				context.addDependency(p, newPath);
 				context.addDependency(newPath, p);
 			}
 			ps.add(newPath);				
 		}
 	}
 
 	private IAnnotationDeclaration findAnnotationAnnotatedWithGenericType(AbstractMemberDefinition m) {
 		List<IAnnotationDeclaration> ds = m.getAnnotations();
 		for (IAnnotationDeclaration d: ds) {
 			if(d.getTypeName() != null) {
 				AnnotationDefinition a = context.getRootContext().getAnnotation(d.getTypeName());
 				if(a != null && a.isAnnotationPresent(getVersion().getGenericTypeAnnotationTypeName())) {
 					return d;
 				}
 			}
 		}
 		return null;
 	}
 
 	public void validateResource(IFile file, CDICoreValidator validator) {
 		new GenericBeanValidator(getVersion()).validateResource(file, validator, context.getRootContext().getProject(), context);
 	}
 
 	public SeverityPreferences getSeverityPreferences() {
 		return CDISeamSolderPreferences.getInstance();
 	}
 	
 }
