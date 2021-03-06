 /**
  *
  */
 package de.uni_koblenz.jgralab.greql2.optimizer;
 
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.Queue;
 import java.util.Set;
 import java.util.logging.Logger;
 
 import de.uni_koblenz.jgralab.Edge;
 import de.uni_koblenz.jgralab.EdgeDirection;
 import de.uni_koblenz.jgralab.JGraLab;
 import de.uni_koblenz.jgralab.Vertex;
 import de.uni_koblenz.jgralab.greql2.evaluator.GreqlEvaluator;
 import de.uni_koblenz.jgralab.greql2.exception.OptimizerException;
 import de.uni_koblenz.jgralab.greql2.optimizer.condexp.Formula;
 import de.uni_koblenz.jgralab.greql2.schema.BoolLiteral;
 import de.uni_koblenz.jgralab.greql2.schema.FunctionApplication;
 import de.uni_koblenz.jgralab.greql2.schema.Greql2;
 import de.uni_koblenz.jgralab.greql2.schema.Greql2Expression;
 import de.uni_koblenz.jgralab.greql2.schema.Greql2Vertex;
 import de.uni_koblenz.jgralab.greql2.schema.IsConstraintOf;
 
 /**
  * TODO: (heimdall) Comment class!
  * 
  * @author ist@uni-koblenz.de
  * 
  */
 public class ConditionalExpressionOptimizer extends OptimizerBase {
 
 	private static Logger logger = JGraLab
 			.getLogger(ConditionalExpressionOptimizer.class.getPackage()
 					.getName());
 
 	private static class VertexEdgeClassTuple {
 		public VertexEdgeClassTuple(Greql2Vertex v, Class<? extends Edge> ec) {
 			this.v = v;
 			this.ec = ec;
 		}
 
 		Greql2Vertex v;
 		Class<? extends Edge> ec;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see
 	 * de.uni_koblenz.jgralab.greql2.optimizer.Optimizer#isEquivalent(de.uni_koblenz
 	 * .jgralab.greql2.optimizer.Optimizer)
 	 */
 	@Override
 	public boolean isEquivalent(Optimizer optimizer) {
 		if (optimizer instanceof ConditionalExpressionOptimizer) {
 			return true;
 		}
 		return false;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see
 	 * de.uni_koblenz.jgralab.greql2.optimizer.Optimizer#optimize(de.uni_koblenz
 	 * .jgralab.greql2.evaluator.GreqlEvaluator,
 	 * de.uni_koblenz.jgralab.greql2.schema.Greql2)
 	 */
 	@Override
 	public boolean optimize(GreqlEvaluator eval, Greql2 syntaxgraph)
 			throws OptimizerException {
 		boolean simplifiedOrOptimized = false;
 
 		FunctionApplication top = findAndOrNotFunApp(syntaxgraph
 				.getFirstGreql2Expression());
 		while (top != null) {
 			LinkedList<VertexEdgeClassTuple> relinkables = rememberConnections(top);
 			Formula formula = Formula.createFormulaFromExpression(top, eval);
			top.delete();
			top = null;
 			Formula optimizedFormula = formula.simplify().optimize();
 			if (!formula.equals(optimizedFormula)) {
 				simplifiedOrOptimized = true;
 				logger.fine(optimizerHeaderString()
 						+ "Transformed constraint\n    " + formula
 						+ "\nto\n    " + optimizedFormula + ".");
 				Greql2Vertex newTop = optimizedFormula.toExpression();
 				for (VertexEdgeClassTuple vect : relinkables) {
 					syntaxgraph.createEdge(vect.ec, newTop, vect.v);
 				}
 				top = findAndOrNotFunApp(syntaxgraph.getFirstGreql2Expression());
 			}
 		}
 
 		// delete "with true" constraints
 		Set<Vertex> verticesToDelete = new HashSet<Vertex>();
 		for (IsConstraintOf ico : syntaxgraph.getIsConstraintOfEdges()) {
 			Vertex alpha = ico.getAlpha();
 			if (alpha instanceof BoolLiteral) {
 				BoolLiteral bl = (BoolLiteral) alpha;
 				if (bl.is_boolValue()) {
 					verticesToDelete.add(bl);
 				}
 			}
 		}
 		for (Vertex bl : verticesToDelete) {
 			bl.delete();
 		}
 
 		recreateVertexEvaluators(eval);
 		OptimizerUtility.createMissingSourcePositions(syntaxgraph);
 
 		// Tg2Dot.printGraphAsDot(syntaxgraph, true, "/home/horn/ceo.dot");
 		// System.out.println("Afted CEO:");
 		// System.out.println(((SerializableGreql2) syntaxgraph).serialize());
 
 		return simplifiedOrOptimized;
 	}
 
 	@SuppressWarnings("unchecked")
 	private LinkedList<VertexEdgeClassTuple> rememberConnections(
 			FunctionApplication top) {
 		LinkedList<VertexEdgeClassTuple> list = new LinkedList<VertexEdgeClassTuple>();
 		assert top.isValid();
 		for (Edge e : top.incidences(EdgeDirection.OUT)) {
 			list.add(new VertexEdgeClassTuple((Greql2Vertex) e.getOmega(),
 					(Class<? extends Edge>) e.getM1Class()));
 		}
 		return list;
 	}
 
 	private FunctionApplication findAndOrNotFunApp(Greql2Expression g) {
 		Queue<Greql2Vertex> queue = new LinkedList<Greql2Vertex>();
 		queue.add(g);
 		while (!queue.isEmpty()) {
 			Greql2Vertex v = queue.poll();
 			if (v instanceof FunctionApplication) {
 				FunctionApplication f = (FunctionApplication) v;
 				if (OptimizerUtility.isAnd(f) || OptimizerUtility.isOr(f)
 						|| OptimizerUtility.isNot(f)) {
 					return f;
 				}
 			}
 			for (Edge e : v.incidences(EdgeDirection.IN)) {
 				queue.offer((Greql2Vertex) e.getAlpha());
 			}
 		}
 		return null;
 	}
 
 }
