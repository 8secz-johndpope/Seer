 package it.unibz.krdb.obda.owlrefplatform.core.reformulation;
 
 import it.unibz.krdb.obda.model.Atom;
 import it.unibz.krdb.obda.model.CQIE;
 import it.unibz.krdb.obda.model.DatalogProgram;
 import it.unibz.krdb.obda.model.OBDADataFactory;
 import it.unibz.krdb.obda.model.Predicate;
 import it.unibz.krdb.obda.model.PredicateAtom;
 import it.unibz.krdb.obda.model.Query;
 import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
 import it.unibz.krdb.obda.owlrefplatform.core.basicoperations.AtomUnifier;
 import it.unibz.krdb.obda.owlrefplatform.core.basicoperations.CQCUtilities;
 import it.unibz.krdb.obda.owlrefplatform.core.basicoperations.PositiveInclusionApplicator;
 import it.unibz.krdb.obda.owlrefplatform.core.basicoperations.QueryAnonymizer;
 import it.unibz.krdb.obda.owlrefplatform.core.ontology.Assertion;
 import it.unibz.krdb.obda.owlrefplatform.core.ontology.ConceptDescription;
 import it.unibz.krdb.obda.owlrefplatform.core.ontology.PositiveInclusion;
 import it.unibz.krdb.obda.owlrefplatform.core.ontology.RoleDescription;
 import it.unibz.krdb.obda.owlrefplatform.core.ontology.imp.AtomicConceptDescriptionImpl;
 import it.unibz.krdb.obda.owlrefplatform.core.ontology.imp.AtomicRoleDescriptionImpl;
 import it.unibz.krdb.obda.owlrefplatform.core.ontology.imp.DLLiterConceptInclusionImpl;
 import it.unibz.krdb.obda.owlrefplatform.core.ontology.imp.DLLiterRoleInclusionImpl;
 import it.unibz.krdb.obda.owlrefplatform.core.ontology.imp.ExistentialConceptDescriptionImpl;
 import it.unibz.krdb.obda.utils.QueryUtils;
 
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 public class TreeRedReformulator implements QueryRewriter {
 
 	private QueryAnonymizer							anonymizer					= null;
 	private AtomUnifier								unifier						= null;
 	private PositiveInclusionApplicator				piApplicator				= null;
 	private Set<PositiveInclusion>					assertions					= null;
 
 	private List<Assertion>							originalassertions			= null;
 	/* Assertions indexed by left side predicate */
 	// private Map<Predicate, Set<PositiveInclusion>> leftAssertionIndex = null;
 
 	/* Assertions indexed by left side predicate */
 	private Map<Predicate, Set<PositiveInclusion>>	rightAssertionIndex			= null;
 
 	private Map<Predicate, Set<PositiveInclusion>>	rightNonExistentialIndex	= null;
 
 	private Map<Predicate, Set<PositiveInclusion>>	rightExistentialIndex		= null;
 
 	Logger											log							= LoggerFactory.getLogger(TreeRedReformulator.class);
 
 	OBDADataFactory									fac							= OBDADataFactoryImpl.getInstance();
 
 	SemanticQueryOptimizer							sqoOptimizer				= null;
 
 	public TreeRedReformulator(List<Assertion> assertions) {
 		this.originalassertions = assertions;
 		log.debug("Given assertions: {}", assertions);
 
 		/*
 		 * Our strategy requires that for every aciom R ISA S, we also have the
 		 * axioms \exists R ISA \exist S and \exists R- ISA \exists S- this
 		 * allows us to keep the cycles to a minimum
 		 */
 		originalassertions.addAll(computeExistentials());
 
 		/*
 		 * Our strategy requires saturation to minimize the number of cycles
 		 * that will be necessary to compute reformulation.
 		 */
 		saturateAssertions();
 
 		log.debug("Computed assertions: {}", this.assertions);
 
 		unifier = new AtomUnifier();
 		anonymizer = new QueryAnonymizer();

		// sqoOptimizer = new SemanticQueryOptimizer(fac, rightAssertionIndex);

 		piApplicator = new PositiveInclusionApplicator(sqoOptimizer);
 
 	}
 
 	/***
 	 * This method adds to the TBox a pair of axioms ER ISA ES and ER- ISA ES-
 	 * for each role inclusion R ISA S found in the ontology.
 	 * 
 	 * @return The set of extra existential assertions that need to be added to
 	 *         the ontology to account for the semantics of role inclusions
 	 *         w.r.t. their domains and ranges.
 	 */
 	private Set<Assertion> computeExistentials() {
 		HashSet<Assertion> newassertion = new HashSet<Assertion>(1000);
 		for (Assertion assertion : originalassertions) {
 			if (assertion instanceof DLLiterRoleInclusionImpl) {
 				DLLiterRoleInclusionImpl rinclusion = (DLLiterRoleInclusionImpl) assertion;
 				RoleDescription r1 = rinclusion.getIncluded();
 				RoleDescription r2 = rinclusion.getIncluding();
 
 				ExistentialConceptDescriptionImpl e11 = new ExistentialConceptDescriptionImpl(r1.getPredicate(), r1.isInverse());
 				;
 				ExistentialConceptDescriptionImpl e12 = new ExistentialConceptDescriptionImpl(r2.getPredicate(), r2.isInverse());
 				ExistentialConceptDescriptionImpl e21 = new ExistentialConceptDescriptionImpl(r1.getPredicate(), !r1.isInverse());
 				ExistentialConceptDescriptionImpl e22 = new ExistentialConceptDescriptionImpl(r2.getPredicate(), !r2.isInverse());
 
 				DLLiterConceptInclusionImpl inc1 = new DLLiterConceptInclusionImpl(e11, e12);
 				DLLiterConceptInclusionImpl inc2 = new DLLiterConceptInclusionImpl(e21, e22);
 				newassertion.add(inc1);
 				newassertion.add(inc2);
 			}
 		}
 		return newassertion;
 	}
 
 	public Query rewrite(Query input) throws Exception {
 
 		long starttime = System.currentTimeMillis();
 
 		if (!(input instanceof DatalogProgram)) {
 			throw new Exception("Rewriting exception: The input must be a DatalogProgram instance");
 		}
 
 		DatalogProgram prog = (DatalogProgram) input;
 
 		log.debug("Starting query rewrting. Received query: \n{}", prog);
 
 		if (!prog.isUCQ()) {
 			throw new Exception("Rewriting exception: The input is not a valid union of conjuctive queries");
 		}
 
 		/* Query preprocessing */
 		log.debug("Anonymizing the query");
 
 		DatalogProgram anonymizedProgram = anonymizer.anonymize(prog);
 
 		log.debug("Removing redundant atoms by query containment");
 		/* Simpliying the query by removing redundant atoms w.r.t. to CQC */
 		HashSet<CQIE> oldqueries = new HashSet<CQIE>(5000);
 		for (CQIE q : anonymizedProgram.getRules()) {
 			oldqueries.add(CQCUtilities.removeRundantAtoms(q));
 		}
 		oldqueries.addAll(anonymizedProgram.getRules());
 
 		HashSet<CQIE> result = new HashSet<CQIE>(5000);
 		result.addAll(oldqueries);
 
 		/*
 		 * Main loop of the rewriter
 		 */
 
 		log.debug("Starting maing rewriting loop");
 		boolean loop = true;
 		while (loop) {
 			loop = false;
 			HashSet<CQIE> newqueriesbyPI = new HashSet<CQIE>(1000);
 
 			/*
 			 * Handling simple inclusions, none involves existentials on the
 			 * right side of an inclusion
 			 */
 			for (CQIE oldquery : oldqueries) {
 
 				HashSet<PositiveInclusion> relevantInclusions = new HashSet<PositiveInclusion>(1000);
 				for (Atom atom : oldquery.getBody()) {
 					relevantInclusions.addAll(getRightNotExistential(((PredicateAtom) atom).getPredicate()));
 				}
 
 				for (CQIE newcq : piApplicator.apply(oldquery, relevantInclusions)) {
 					newqueriesbyPI.add(anonymizer.anonymize(newcq));
 				}
 
 			}
 
 			/*
 			 * Optimizing the set
 			 */
 			newqueriesbyPI = CQCUtilities.removeDuplicateAtoms(newqueriesbyPI);
 
 			/*
 			 * Handling existential inclusions, and unification We will collect
 			 * all predicates mantioned in the old queries, we will collect all
 			 * the existential inclusions relevant for those predicates and sort
 			 * them by inverse and not inverse. Last we apply these groups of
 			 * inclusions at the same time. All of them will share one single
 			 * unification atempt.
 			 */
 
 			HashSet<CQIE> newqueriesbyunificationandPI = new HashSet<CQIE>(1000);
 
 			// Collecting relevant predicates
 
 			HashSet<Predicate> predicates = new HashSet<Predicate>(1000);
 			for (CQIE oldquery : oldqueries) {
 				for (Atom atom : oldquery.getBody()) {
 					predicates.add(((PredicateAtom) atom).getPredicate());
 				}
 			}
 
 			for (CQIE oldquery : newqueriesbyPI) {
 				for (Atom atom : oldquery.getBody()) {
 					predicates.add(((PredicateAtom) atom).getPredicate());
 				}
 			}
 
 			// Collecting relevant inclusion
 			for (Predicate predicate : predicates) {
 				Set<PositiveInclusion> relevantInclusion = getRightExistential(predicate);
 				// sorting inverse and not inverse
 				Set<PositiveInclusion> relevantinverse = new HashSet<PositiveInclusion>(1000);
 				Set<PositiveInclusion> relevantnotinverse = new HashSet<PositiveInclusion>(1000);
 				for (PositiveInclusion inc : relevantInclusion) {
 					DLLiterConceptInclusionImpl samplepi = (DLLiterConceptInclusionImpl) inc;
 					ExistentialConceptDescriptionImpl ex = (ExistentialConceptDescriptionImpl) samplepi.getIncluding();
 					if (ex.isInverse())
 						relevantinverse.add(inc);
 					else
 						relevantnotinverse.add(inc);
 				}
 
 				/*
 				 * Collecting the relevant queries from the old set, and the
 				 * queries that have been just produced
 				 */
 				Set<CQIE> relevantQueries = new HashSet<CQIE>(1000);
 				for (CQIE query : oldqueries) {
 					if (containsPredicate(query, predicate))
 						relevantQueries.add(query);
 				}
 
 				for (CQIE query : newqueriesbyPI) {
 					if (containsPredicate(query, predicate))
 						relevantQueries.add(query);
 				}
 
 				/*
 				 * Applying the existential inclusions and unifications
 				 * (targeted) removing duplicate atoms before adding the queries
 				 * to the set.
 				 */
 
 				newqueriesbyunificationandPI.addAll(CQCUtilities.removeDuplicateAtoms(piApplicator.applyExistentialInclusions(
 						relevantQueries, relevantinverse, rightAssertionIndex)));
 				newqueriesbyunificationandPI.addAll(CQCUtilities.removeDuplicateAtoms(piApplicator.applyExistentialInclusions(
 						relevantQueries, relevantnotinverse, rightAssertionIndex)));
 			}
 
 			/*
 			 * Removing duplicated atoms in each of the queries to simplify
 			 */
 
 			newqueriesbyPI = CQCUtilities.removeDuplicateAtoms(newqueriesbyPI);
 			/* These queries are final, no need for new passes on these */
			if (sqoOptimizer != null) {
				result.addAll(sqoOptimizer.optimizeBySQO(newqueriesbyPI));
			} else {
				result.addAll(newqueriesbyPI);
			}
 
 			/*
 			 * Preparing the set of queries for the next iteration.
 			 */
 
 			newqueriesbyunificationandPI = CQCUtilities.removeDuplicateAtoms(newqueriesbyunificationandPI);
 			LinkedList<CQIE> newquerieslist = new LinkedList<CQIE>();
			if (sqoOptimizer != null) {
				newquerieslist.addAll(sqoOptimizer.optimizeBySQO(newqueriesbyunificationandPI));
			} else {
				newquerieslist.addAll(newqueriesbyunificationandPI);
			}
 
 			oldqueries = new HashSet<CQIE>(newquerieslist.size() * 2);
 			for (CQIE newquery : newquerieslist) {
 				if (result.add(newquery)) {
 					loop = true;
 					oldqueries.add(newquery);
 				}
 			}
 
 		}
 		log.debug("Main loop ended");
 		LinkedList<CQIE> resultlist = new LinkedList<CQIE>();
 		resultlist.addAll(result);
 
 		/* One last pass of the syntactic containment checker */
 
 		log.debug("Removing trivially contained queries");
 		CQCUtilities.removeContainedQueriesSyntacticSorter(resultlist, false);
 
 		// if (resultlist.size() < 300) {
 		log.debug("Removing CQC contained queries");
 		CQCUtilities.removeContainedQueriesSorted(resultlist, true);
 		// }
 
 		DatalogProgram resultprogram = fac.getDatalogProgram();
 		resultprogram.appendRule(resultlist);
 
 		long endtime = System.currentTimeMillis();
 
 		QueryUtils.copyQueryModifiers(input, resultprogram);
 
 		log.debug("Computed reformulation: \n{}", resultprogram);
 		log.debug("Final size of the reformulation: {}", resultlist.size());
 		double seconds = (endtime - starttime) / 1000;
 		log.info("Time elapsed for reformulation: {}s", seconds);
 
 		return resultprogram;
 	}
 
 	private boolean containsPredicate(CQIE q, Predicate predicate) {
 		for (Atom atom : q.getBody()) {
 			if (((PredicateAtom) atom).getPredicate().equals(predicate))
 				return true;
 		}
 		return q.getHead().getPredicate().equals(predicate);
 	}
 
 	/***
 	 * Saturates the set of assertions and creates the indexes for these based
 	 * on their predicates. It only takes into account positive inclusions. PIs
 	 * with qualififed existntital concepts are ignored.
 	 * 
 	 * To saturate, we do
 	 * 
 	 * For each pair of assertions C1 ISA C2, C2 ISA C3, we compute C1 ISA C3.
 	 */
 	private void saturateAssertions() {
 		assertions = new HashSet<PositiveInclusion>();
 		// leftAssertionIndex = new HashMap<Predicate,
 		// Set<PositiveInclusion>>();
 		rightAssertionIndex = new HashMap<Predicate, Set<PositiveInclusion>>();
 		rightNonExistentialIndex = new HashMap<Predicate, Set<PositiveInclusion>>();
 		rightExistentialIndex = new HashMap<Predicate, Set<PositiveInclusion>>();
 
 		/*
 		 * Loading the initial assertions, filtering postive inlusions and
 		 * indexing
 		 */
 		for (Assertion assertion : originalassertions) {
 			if (assertion instanceof PositiveInclusion) {
 				PositiveInclusion pi = (PositiveInclusion) assertion;
 				assertions.add(pi);
 				index(pi);
 			}
 		}
 
 		/* Saturating is-a hierachy loop */
 		boolean loop = true;
 		while (loop) {
 			loop = false;
 			HashSet<PositiveInclusion> newInclusions = new HashSet<PositiveInclusion>();
 			for (PositiveInclusion pi1 : assertions) {
 				for (PositiveInclusion pi2 : assertions) {
 					if ((pi1 instanceof DLLiterConceptInclusionImpl) && (pi2 instanceof DLLiterConceptInclusionImpl)) {
 						DLLiterConceptInclusionImpl ci1 = (DLLiterConceptInclusionImpl) pi1;
 						DLLiterConceptInclusionImpl ci2 = (DLLiterConceptInclusionImpl) pi2;
 						if (ci1.getIncluding().equals(ci2.getIncluded())) {
 							DLLiterConceptInclusionImpl newinclusion = new DLLiterConceptInclusionImpl(ci1.getIncluded(),
 									ci2.getIncluding());
 							newInclusions.add(newinclusion);
 						} else if (ci1.getIncluded().equals(ci2.getIncluding())) {
 							DLLiterConceptInclusionImpl newinclusion = new DLLiterConceptInclusionImpl(ci2.getIncluded(),
 									ci1.getIncluding());
 							newInclusions.add(newinclusion);
 						}
 					} else if ((pi1 instanceof DLLiterRoleInclusionImpl) && (pi2 instanceof DLLiterRoleInclusionImpl)) {
 						DLLiterRoleInclusionImpl ci1 = (DLLiterRoleInclusionImpl) pi1;
 						DLLiterRoleInclusionImpl ci2 = (DLLiterRoleInclusionImpl) pi2;
 						if (ci1.getIncluding().equals(ci2.getIncluded())) {
 							DLLiterRoleInclusionImpl newinclusion = new DLLiterRoleInclusionImpl(ci1.getIncluded(), ci2.getIncluding());
 							newInclusions.add(newinclusion);
 						} else if (ci1.getIncluded().equals(ci2.getIncluding())) {
 							DLLiterRoleInclusionImpl newinclusion = new DLLiterRoleInclusionImpl(ci2.getIncluded(), ci1.getIncluding());
 							newInclusions.add(newinclusion);
 						}
 					}
 				}
 			}
 
 			loop = loop || assertions.addAll(newInclusions);
 			if (loop)
 				indexAll(newInclusions);
 		}
 
 		// /* saturating A ISA ER (if A ISA ER and R ISA S -> A ISA ES) */
 		// /* This will be used for SQO and for optimizing the applicaiton of
 		// existential restrictions */
 		// HashSet<PositiveInclusion> newExistentials = new
 		// HashSet<PositiveInclusion>();
 		// for (PositiveInclusion pi: assertions) {
 		// if (!(pi instanceof DLLiterConceptInclusionImpl))
 		// continue;
 		// DLLiterConceptInclusionImpl ci = (DLLiterConceptInclusionImpl)pi;
 		// if (!(ci.getIncluding() instanceof
 		// ExistentialConceptDescriptionImpl)) {
 		// continue;
 		// }
 		// ExistentialConceptDescriptionImpl ex =
 		// (ExistentialConceptDescriptionImpl)ci.getIncluding();
 		//
 		//
 		//
 		// }
 
 	}
 
 	private void indexAll(Collection<PositiveInclusion> pis) {
 		for (PositiveInclusion pi : pis) {
 			index(pi);
 		}
 	}
 
 	private void index(PositiveInclusion pi) {
 		if (pi instanceof DLLiterConceptInclusionImpl) {
 			DLLiterConceptInclusionImpl cpi = (DLLiterConceptInclusionImpl) pi;
 			// ConceptDescription description1 = cpi.getIncluded();
 			ConceptDescription description2 = cpi.getIncluding();
 
 			// /* Processing left side */
 			// if (description1 instanceof AtomicConceptDescriptionImpl) {
 			// AtomicConceptDescriptionImpl acd = (AtomicConceptDescriptionImpl)
 			// description1;
 			// Set<PositiveInclusion> leftAssertion =
 			// getLeft(acd.getPredicate());
 			// leftAssertion.add(pi);
 			// } else if (description1 instanceof
 			// ExistentialConceptDescriptionImpl) {
 			// ExistentialConceptDescriptionImpl ecd =
 			// (ExistentialConceptDescriptionImpl) description1;
 			// Set<PositiveInclusion> leftAssertion =
 			// getLeft(ecd.getPredicate());
 			// leftAssertion.add(pi);
 			// }
 
 			/* Processing right side */
 			if (description2 instanceof AtomicConceptDescriptionImpl) {
 				AtomicConceptDescriptionImpl acd = (AtomicConceptDescriptionImpl) description2;
 				Set<PositiveInclusion> rightAssertion = getRight(acd.getPredicate());
 				rightAssertion.add(pi);
 
 				Set<PositiveInclusion> rightNonExistential = getRightNotExistential(acd.getPredicate());
 				rightNonExistential.add(pi);
 
 			} else if (description2 instanceof ExistentialConceptDescriptionImpl) {
 				ExistentialConceptDescriptionImpl ecd = (ExistentialConceptDescriptionImpl) description2;
 				Set<PositiveInclusion> rightAssertion = getRight(ecd.getPredicate());
 				rightAssertion.add(pi);
 
 				Set<PositiveInclusion> rightExistential = getRightExistential(ecd.getPredicate());
 				rightExistential.add(pi);
 			}
 
 		} else if (pi instanceof DLLiterRoleInclusionImpl) {
 			DLLiterRoleInclusionImpl cpi = (DLLiterRoleInclusionImpl) pi;
 
 			// RoleDescription description1 = cpi.getIncluded();
 			RoleDescription description2 = cpi.getIncluding();
 
 			// /* Processing left side */
 			// if (description1 instanceof AtomicRoleDescriptionImpl) {
 			// AtomicRoleDescriptionImpl acd = (AtomicRoleDescriptionImpl)
 			// description1;
 			// Set<PositiveInclusion> leftAssertion =
 			// getLeft(acd.getPredicate());
 			// leftAssertion.add(pi);
 			// }
 			/* Processing right side */
 			if (description2 instanceof AtomicRoleDescriptionImpl) {
 				AtomicRoleDescriptionImpl acd = (AtomicRoleDescriptionImpl) description2;
 				Set<PositiveInclusion> rightAssertion = getRight(acd.getPredicate());
 				rightAssertion.add(pi);
 
 				Set<PositiveInclusion> rightNonExistential = getRightNotExistential(acd.getPredicate());
 				rightNonExistential.add(pi);
 
 			}
 
 		}
 	}
 
 	// private Set<PositiveInclusion> getLeft(Predicate pred) {
 	// Set<PositiveInclusion> assertions = leftAssertionIndex.get(pred);
 	// if (assertions == null) {
 	// assertions = new HashSet<PositiveInclusion>();
 	// leftAssertionIndex.put(pred, assertions);
 	// }
 	// return assertions;
 	// }
 
 	private Set<PositiveInclusion> getRight(Predicate pred) {
 		Set<PositiveInclusion> assertions = rightAssertionIndex.get(pred);
 		if (assertions == null) {
 			assertions = new HashSet<PositiveInclusion>();
 			rightAssertionIndex.put(pred, assertions);
 		}
 		return assertions;
 	}
 
 	private Set<PositiveInclusion> getRightNotExistential(Predicate pred) {
 		Set<PositiveInclusion> assertions = rightNonExistentialIndex.get(pred);
 		if (assertions == null) {
 			assertions = new HashSet<PositiveInclusion>();
 			rightNonExistentialIndex.put(pred, assertions);
 		}
 		return assertions;
 	}
 
 	private Set<PositiveInclusion> getRightExistential(Predicate pred) {
 		Set<PositiveInclusion> assertions = rightExistentialIndex.get(pred);
 		if (assertions == null) {
 			assertions = new HashSet<PositiveInclusion>();
 			rightExistentialIndex.put(pred, assertions);
 		}
 		return assertions;
 	}
 
 	@Override
 	public void updateAssertions(List<Assertion> ass) {
 		this.originalassertions = ass;
 	}
 
 }
