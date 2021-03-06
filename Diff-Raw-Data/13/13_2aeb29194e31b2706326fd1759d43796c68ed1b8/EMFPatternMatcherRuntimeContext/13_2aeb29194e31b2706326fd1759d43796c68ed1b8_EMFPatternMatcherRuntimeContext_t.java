 /*******************************************************************************
  * Copyright (c) 2004-2009 Gabor Bergmann and Daniel Varro
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *    Gabor Bergmann - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.viatra2.emf.incquery.runtime.internal;
 
 import java.util.ArrayList;
 import java.util.Collection;
 
 import org.eclipse.emf.common.notify.Notifier;
 import org.eclipse.emf.ecore.EAttribute;
 import org.eclipse.emf.ecore.EClass;
 import org.eclipse.emf.ecore.EDataType;
 import org.eclipse.emf.ecore.EObject;
 import org.eclipse.emf.ecore.EReference;
 import org.eclipse.emf.ecore.EStructuralFeature;
 import org.eclipse.emf.ecore.resource.Resource;
 import org.eclipse.emf.ecore.resource.ResourceSet;
 import org.eclipse.viatra2.emf.incquery.runtime.EMFPatternMatcherContext;
 import org.eclipse.viatra2.gtasm.patternmatcher.incremental.rete.boundary.IManipulationListener;
 import org.eclipse.viatra2.gtasm.patternmatcher.incremental.rete.boundary.IPredicateTraceListener;
 import org.eclipse.viatra2.gtasm.patternmatcher.incremental.rete.boundary.PredicateEvaluatorNode;
 import org.eclipse.viatra2.gtasm.patternmatcher.incremental.rete.matcher.IPatternMatcherRuntimeContext;
 import org.eclipse.viatra2.gtasm.patternmatcher.incremental.rete.matcher.ReteEngine;
 import org.eclipse.viatra2.gtasm.patternmatcher.incremental.rete.tuple.Tuple;
 
 
 
 /**
  * @author Bergmann Gábor
  *
  */
 public abstract class EMFPatternMatcherRuntimeContext<PatternDescription> 
 	extends EMFPatternMatcherContext<PatternDescription> 
 	implements IPatternMatcherRuntimeContext<PatternDescription>
 {
 	protected abstract EMFContainmentHierarchyTraversal newTraversal();
 	protected abstract Notifier getRoot();	
 	
 	/**
 	 * @param visitor
 	 */
 	protected void doVisit(CustomizedEMFVisitor visitor) {
 		if (traversalCoalescing) waitingVisitors.add(visitor);
 		else newTraversal().accept(visitor);
 	}
 	
 	static class CustomizedEMFVisitor extends EMFVisitor {
 		@Override
 		public final void visitInternalReference(EObject source, EReference feature, EObject target) {
 			if (target == null) return; // null-valued attributes / references are simply not stored
 			if (feature.getEOpposite() != null) {
 				if (feature.isContainment()) {
 					doVisitReference(target, feature.getEOpposite(), source);
 				} else if (feature.getEOpposite().isContainment()) {
 					return;
 				}
 			}
 			doVisitReference(source, feature, target);
 		}
 		@Override
 		public void visitExternalReference(EObject source, EReference feature, EObject target) {
 			if (target == null) return; // null-valued attributes / references are simply not stored
 			if (feature.getEOpposite() != null && feature.getEOpposite().isContainment()) return;
 			doVisitReference(source, feature, target);
 		}
 		void doVisitReference(EObject source, EReference feature, EObject target) {}
 	}
 	
 	public static class ForResourceSet<PatternDescription> extends EMFPatternMatcherRuntimeContext<PatternDescription> {
 		ResourceSet root;
 		public ForResourceSet(ResourceSet root) {
 			super();
 			this.root = root;
 		}
 		@Override
 		protected EMFContainmentHierarchyTraversal newTraversal() {
 			return new EMFContainmentHierarchyTraversal(root);
 		}
 		@Override
 		protected Notifier getRoot() {
 			return root;
 		}	
 	}
 	public static class ForResource<PatternDescription> extends EMFPatternMatcherRuntimeContext<PatternDescription> {
 		Resource root;
 		public ForResource(Resource root) {
 			super();
 			this.root = root;
 		}
 		@Override
 		protected EMFContainmentHierarchyTraversal newTraversal() {
 			return new EMFContainmentHierarchyTraversal(root);
 		}	
 		@Override
 		protected Notifier getRoot() {
 			return root;
 		}	
 	}
 	public static class ForEObject<PatternDescription> extends EMFPatternMatcherRuntimeContext<PatternDescription> {
 		EObject root;
 		public ForEObject(EObject root) {
 			super();
 			this.root = root;
 		}
 		@Override
 		protected EMFContainmentHierarchyTraversal newTraversal() {
 			return new EMFContainmentHierarchyTraversal(root);
 		}	
 		@Override
 		protected Notifier getRoot() {
 			return root;
 		}	
 	}
 	
 	protected Collection<EMFVisitor> waitingVisitors;
 	boolean traversalCoalescing;
 
 	/**
 	 * Notifier must be EObject, Resource or ResourceSet
 	 * @param notifier
 	 */
 	private EMFPatternMatcherRuntimeContext() {
 		this.waitingVisitors = new ArrayList<EMFVisitor>();
 		this.traversalCoalescing = false;
 	}
 	
 	@Override
 	public void startCoalescing() {
 		assert(!traversalCoalescing);
 		traversalCoalescing = true;
 	}
 	@Override
 	public void finishCoalescing() {
 		assert(traversalCoalescing);
 		traversalCoalescing = false;
 		if (! waitingVisitors.isEmpty()){
 			newTraversal().accept(new MultiplexerVisitor(waitingVisitors));
 			waitingVisitors.clear();
 		}
 	}
 
 	@Override
 	public void enumerateAllBinaryEdges(final ModelElementPairCrawler crawler) {
 		CustomizedEMFVisitor visitor = new CustomizedEMFVisitor() {
 			@Override
 			public void visitAttribute(EObject source, EAttribute feature, Object target) {
 				if (target != null) // Exclude NULL attribute values from RETE
 					crawler.crawl(source, target);
 				super.visitAttribute(source, feature, target);
 			}
 			@Override
 			public void doVisitReference(EObject source, EReference feature, EObject target) {
 				crawler.crawl(source, target);
 			}
 		};
 		doVisit(visitor);
 	}
 
 	@Override
 	public void enumerateAllGeneralizations(ModelElementPairCrawler crawler) {
 		throw new UnsupportedOperationException();
 	}
 
 	@Override
 	// Only direct instantiation of unaries is supported now
 	public void enumerateAllInstantiations(final ModelElementPairCrawler crawler) {
 		CustomizedEMFVisitor visitor = new CustomizedEMFVisitor() {
 			@Override
 			public void visitAttribute(EObject source, EAttribute feature, Object target) {
 				if (target != null) // Exclude NULL attribute values from RETE
 					crawler.crawl(feature.getEAttributeType(), target);
 			}
 			@Override
 			public void visitElement(EObject source) {
 				crawler.crawl(source.eClass(), source);
 			}
 		};
 		doVisit(visitor);
 	}
 
 	@Override
 	public void enumerateAllTernaryEdges(final ModelElementCrawler crawler) {
 		throw new UnsupportedOperationException();
 	}
 
 	@Override
 	public void enumerateAllUnaries(final ModelElementCrawler crawler) {
 		CustomizedEMFVisitor visitor = new CustomizedEMFVisitor() {
 			@Override
 			public void visitAttribute(EObject source, EAttribute feature, Object target) {
 				if (target != null) // Exclude NULL attribute values from RETE
 					crawler.crawl(target);
 				super.visitAttribute(source, feature, target);
 			}
 			@Override
 			public void visitElement(EObject source) {
 				crawler.crawl(source);
 				super.visitElement(source);
 			}
 		};
 		doVisit(visitor);
 	}
 
 	@Override
 	public void enumerateAllUnaryContainments(final ModelElementPairCrawler crawler) {
 		CustomizedEMFVisitor visitor = new CustomizedEMFVisitor() {
			// FIXME: containment no longer holds between EObject and its raw attribute values.
//			@Override
//			public void visitAttribute(EObject source, EAttribute feature, Object target) {
//				if (target != null) // Exclude NULL attribute values from RETE
//					crawler.crawl(source, target);
//				super.visitAttribute(source, feature, target);
//			}
 			@Override
 			public void doVisitReference(EObject source, EReference feature, EObject target) {
 				if (feature.isContainment()) crawler.crawl(source, target);
 			}
 		};
 		doVisit(visitor);
 	}
 
 	@Override
 	public void enumerateDirectBinaryEdgeInstances(Object typeObject, final ModelElementPairCrawler crawler) {
 		final EStructuralFeature structural = (EStructuralFeature) typeObject;
 		CustomizedEMFVisitor visitor = new CustomizedEMFVisitor() {
 			@Override
 			public void visitAttribute(EObject source, EAttribute feature, Object target) {
 				if (structural.equals(feature) && target != null) // NULL attribute values excluded from RETE
 					crawler.crawl(source, target);
 				super.visitAttribute(source, feature, target);
 			}
 			@Override
 			public void doVisitReference(EObject source, EReference feature, EObject target) {
 				if (structural.equals(feature)) crawler.crawl(source, target);
 			}
 		};
 		doVisit(visitor);
 	}
 	@Override
 	public void enumerateAllBinaryEdgeInstances(Object typeObject, final ModelElementPairCrawler crawler) {
 		enumerateDirectBinaryEdgeInstances(typeObject, crawler); // No edge subtyping
 	}
 
 	@Override
 	public void enumerateDirectTernaryEdgeInstances(Object typeObject, final ModelElementCrawler crawler) {
 		throw new UnsupportedOperationException();
 	}
 	@Override
 	public void enumerateAllTernaryEdgeInstances(Object typeObject, final ModelElementCrawler crawler) {
 		throw new UnsupportedOperationException();
 	}
 
 	@Override
 	public void enumerateDirectUnaryInstances(final Object typeObject, final ModelElementCrawler crawler) {
 		if (typeObject instanceof EClass) {
 			CustomizedEMFVisitor visitor = new CustomizedEMFVisitor() {
 				@Override
 				public void visitElement(EObject source) {			
 					if (source.eClass().equals(typeObject)) crawler.crawl(source);
 					super.visitElement(source);
 				}
 			};
 			doVisit(visitor);
 		} else if (typeObject instanceof EDataType) {
 			CustomizedEMFVisitor visitor = new CustomizedEMFVisitor() {
 				@Override
 				public void visitAttribute(EObject source, EAttribute feature, Object target) {
 					if (target != null && ((EDataType)typeObject).isInstance(target)) // Exclude NULL attribute values from RETE
 						crawler.crawl(target);
 					super.visitAttribute(source, feature, target);
 				}
 			};
 			doVisit(visitor);
 		} else throw new IllegalArgumentException("typeObject has invalid type " + typeObject.getClass().getName());
 	}
 	@Override
 	public void enumerateAllUnaryInstances(final Object typeObject, final ModelElementCrawler crawler) {
 		if (typeObject instanceof EClass) {
 			CustomizedEMFVisitor visitor = new CustomizedEMFVisitor() {
 				@Override
 				public void visitElement(EObject source) {			
 					if (((EClass)typeObject).isInstance(source)) crawler.crawl(source);
 					super.visitElement(source);
 				}
 			};
 			doVisit(visitor);
 		} else if (typeObject instanceof EDataType) {
 			CustomizedEMFVisitor visitor = new CustomizedEMFVisitor() {
 				@Override
 				public void visitAttribute(EObject source, EAttribute feature, Object target) {
 					if (target != null && ((EDataType)typeObject).isInstance(target)) // Exclude NULL attribute values from RETE
 						crawler.crawl(target);
 					super.visitAttribute(source, feature, target);
 				}
 			};
 			doVisit(visitor);
 		} else throw new IllegalArgumentException("typeObject has invalid type " + typeObject.getClass().getName());
 	}
 
 	@Override
 	public void modelReadLock() {
 		// TODO runnable? domain.runExclusive(read)
 		
 	}
 
 	@Override
 	public void modelReadUnLock() {
 		// TODO runnable? domain.runExclusive(read)
 			
 	}
 
 	//	@Override
 	//	public String retrieveUnaryTypeFQN(Object typeObject) {
 	//		return contextMapping.retrieveFQN((EClassifier)typeObject);
 	//	}
 	//
 	//	@Override
 	//	public String retrieveBinaryEdgeTypeFQN(Object typeObject) {
 	//		return contextMapping.retrieveFQN((EStructuralFeature)typeObject);
 	//	}
 	//
 	//	@Override
 	//	public String retrieveTernaryEdgeTypeFQN(Object typeObject) {
 	//		throw new UnsupportedOperationException();
 	//	}	
 		
 	
 	@Override
 	// TODO Transactional?
 	public IManipulationListener subscribePatternMatcherForUpdates(
 			ReteEngine<PatternDescription> engine) {
 		return new EMFContentTreeViralListener(engine, getRoot());
 	}
 
 	@Override
 	public Object ternaryEdgeSource(Object relation) {
 		throw new UnsupportedOperationException();
 	}
 
 	@Override
 	public Object ternaryEdgeTarget(Object relation) {
 		throw new UnsupportedOperationException();
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.viatra2.gtasm.patternmatcher.incremental.IPatternMatcherRuntimeContext#subscribePatternMatcherForTraceInfluences(org.eclipse.viatra2.gtasm.patternmatcher.incremental.ReteEngine)
 	 */
 	@Override
 	public IPredicateTraceListener subscribePatternMatcherForTraceInfluences(ReteEngine<PatternDescription> engine) {
 		// No ASMFunctions, use DUMMY
 		return new IPredicateTraceListener() {
 			@Override
 			public void registerSensitiveTrace(Tuple trace,
 					PredicateEvaluatorNode node) {
 			}
 			@Override
 			public void unregisterSensitiveTrace(Tuple trace,
 					PredicateEvaluatorNode node) {
 			}
 			@Override
 			public void disconnect() {
 			}
 		};
 	}
 	
 }
