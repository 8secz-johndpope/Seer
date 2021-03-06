 /**
  * 
  */
 package de.uni_koblenz.jgralab.greql2.funlib;
 
 import java.util.ArrayList;
 
 import de.uni_koblenz.jgralab.AttributedElement;
 import de.uni_koblenz.jgralab.Graph;
 import de.uni_koblenz.jgralab.GraphElement;
 import de.uni_koblenz.jgralab.graphmarker.BooleanGraphMarker;
 import de.uni_koblenz.jgralab.greql2.exception.EvaluateException;
 import de.uni_koblenz.jgralab.greql2.exception.WrongFunctionParameterException;
 import de.uni_koblenz.jgralab.greql2.jvalue.JValue;
 import de.uni_koblenz.jgralab.greql2.jvalue.JValueRecord;
 import de.uni_koblenz.jgralab.greql2.jvalue.JValueTuple;
 import de.uni_koblenz.jgralab.greql2.jvalue.JValueType;
 import de.uni_koblenz.jgralab.schema.Attribute;
 import de.uni_koblenz.jgralab.schema.AttributedElementClass;
 
 /**
  * Describes the given {@link AttributedElement}, that is, it returns a triple.
  * The first tuple component is the qualified name of the elements
  * {@link AttributedElementClass}, the second is its ID (or a record of ID and
  * graph version, if the element is a graph). The third component is a record
  * holding all the elements attributes and their values.
  * 
  * When given no parameter, the current graph is described.
  * 
  * <dl>
  * <dt><b>GReQL-signature</b></dt>
  * <dd><code>TUPLE describe(ae:ATTRIBUTEDELEMENT)</code></dd>
  * <dd><code>TUPLE describe()</code></dd>
  * <dd>&nbsp;</dd>
  * </dl>
  * <dl>
  * <dt></dt>
  * <dd>
  * <dl>
  * <dt><b>Parameters:</b></dt>
  * <dd><code>ae</code> - the AttributedElement to describe</dd>
  * <dt><b>Returns:</b></dt>
  * <dd>a triple (AttrElemClassQName, ID, AttrRecord)</dd>
  * <dd><code>Null</code> if one of the parameters is <code>Null</code></dd>
  * </dl>
  * </dd>
  * </dl>
  * 
  * 
  * @author Tassilo Horn &lt;horn@uni-koblenz.de&gt;
  * 
  */
 public class Describe extends Greql2Function {
 	{
 		JValueType[][] x = { {}, { JValueType.ATTRIBUTEDELEMENT } };
 		signatures = x;
 
 		description = "Return a tuple (QName, ID, Attrs) describing the given attributed element.\n"
 				+ "If no element is given, describe the graph itself, producing: (QName, (ID, Version), Attrs).";
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see
 	 * de.uni_koblenz.jgralab.greql2.funlib.Greql2Function#evaluate(de.uni_koblenz
 	 * .jgralab.Graph, de.uni_koblenz.jgralab.graphmarker.BooleanGraphMarker,
 	 * de.uni_koblenz.jgralab.greql2.jvalue.JValue[])
 	 */
 	@Override
 	public JValue evaluate(Graph graph, BooleanGraphMarker subgraph,
 			JValue[] arguments) throws EvaluateException {
 		AttributedElement attrElem = null;
 		switch (checkArguments(arguments)) {
 		case 0:
 			attrElem = graph;
 			break;
 		case 1:
 			attrElem = arguments[0].toAttributedElement();
 			break;
 		default:
 			throw new WrongFunctionParameterException(this, arguments);
 		}
 
 		JValueTuple tuple = new JValueTuple();
 		tuple.add(new JValue(attrElem.getAttributedElementClass()
 				.getQualifiedName()));
 		if (attrElem instanceof Graph) {
 			Graph g = (Graph) attrElem;
 			JValueRecord idRecord = new JValueRecord();
 			idRecord.add("id", new JValue(g.getId()));
 			idRecord.add("version", new JValue(g.getGraphVersion()));
 			tuple.add(idRecord);
 		} else {
 			GraphElement ge = (GraphElement) attrElem;
 			tuple.add(new JValue(ge.getId()));
 		}
 		JValueRecord attrRecord = new JValueRecord();
 		for (Attribute attr : attrElem.getAttributedElementClass()
 				.getAttributeList()) {
 			try {
				attrRecord.add(attr.getName(), JValue.fromObject(attrElem
 						.getAttribute(attr.getName())));
 			} catch (NoSuchFieldException e) {
 				e.printStackTrace();
 				throw new EvaluateException("Couldn't get value of attribute '"
 						+ attr.getName() + "'.", e);
 			}
 		}
 		tuple.add(attrRecord);
 		return tuple;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see
 	 * de.uni_koblenz.jgralab.greql2.funlib.Greql2Function#getEstimatedCardinality
 	 * (int)
 	 */
 	@Override
 	public long getEstimatedCardinality(int inElements) {
 		return 1;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see
 	 * de.uni_koblenz.jgralab.greql2.funlib.Greql2Function#getEstimatedCosts
 	 * (java.util.ArrayList)
 	 */
 	@Override
 	public long getEstimatedCosts(ArrayList<Long> inElements) {
 		return 3;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see de.uni_koblenz.jgralab.greql2.funlib.Greql2Function#getSelectivity()
 	 */
 	@Override
 	public double getSelectivity() {
 		return 1;
 	}
 
 }
