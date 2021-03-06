 /*
  * JGraLab - The Java graph laboratory
  * (c) 2006-2008 Institute for Software Technology
  *               University of Koblenz-Landau, Germany
  *
  *               ist@uni-koblenz.de
  *
  * Please report bugs to http://serres.uni-koblenz.de/bugzilla
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License
  * as published by the Free Software Foundation; either version 2
  * of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
  */
 
 package de.uni_koblenz.jgralab.schema.impl;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 
 import de.uni_koblenz.jgralab.schema.AggregationClass;
 import de.uni_koblenz.jgralab.schema.AttributedElementClass;
 import de.uni_koblenz.jgralab.schema.CompositionClass;
 import de.uni_koblenz.jgralab.schema.EdgeClass;
 import de.uni_koblenz.jgralab.schema.GraphClass;
 import de.uni_koblenz.jgralab.schema.GraphElementClass;
 import de.uni_koblenz.jgralab.schema.Package;
 import de.uni_koblenz.jgralab.schema.QualifiedName;
 import de.uni_koblenz.jgralab.schema.Schema;
 import de.uni_koblenz.jgralab.schema.VertexClass;
 import de.uni_koblenz.jgralab.schema.exception.DuplicateNamedElementException;
 import de.uni_koblenz.jgralab.schema.exception.InheritanceException;
 import de.uni_koblenz.jgralab.schema.exception.ReservedWordException;
 import de.uni_koblenz.jgralab.schema.exception.SchemaException;
 
 public class GraphClassImpl extends AttributedElementClassImpl implements
 		GraphClass {
 
 	private Schema schema;
 
 	private Map<QualifiedName, GraphElementClass> graphElementClasses;
 
 	private Map<QualifiedName, EdgeClass> edgeClasses;
 
 	private Map<QualifiedName, VertexClass> vertexClasses;
 
 	private Map<QualifiedName, AggregationClass> aggregationClasses;
 
 	private Map<QualifiedName, CompositionClass> compositionClasses;
 
 	/**
 	 * Creates the <b>sole</b> <code>GraphClass</code> in the
 	 * <code>Schema</code>, that holds all <code>GraphElementClasses</code>/
 	 * <code>EdgeClasses</code>/ <code>VertexClasses</code>/
 	 * <code>AggregationClasses</code>/ <code>CompositionClasses</code>.
 	 * <p>
 	 * <b>Caution:</b> The <code>GraphClass</code> should only be created by
 	 * using
 	 * {@link de.uni_koblenz.jgralab.schema.Schema#createGraphClass(QualifiedName id)
 	 * createGraphClass(Qualified id)} in <code>Schema</code>. Unfortunately,
 	 * due to restrictions in Java, the visibility of this constructor cannot be
 	 * changed without causing serious issues in the program.
 	 * </p>
 	 *
 	 * @param qn
 	 *            a unique name in the <code>Schema</code>
 	 * @param aSchema
 	 *            the <code>Schema</code> containing this
 	 *            <code>GraphClass</code>
 	 */
 	public GraphClassImpl(QualifiedName qn, Schema aSchema) {
 		super(qn);
 		schema = aSchema;
 		graphElementClasses = new HashMap<QualifiedName, GraphElementClass>();
 		edgeClasses = new HashMap<QualifiedName, EdgeClass>();
 		vertexClasses = new HashMap<QualifiedName, VertexClass>();
 		aggregationClasses = new HashMap<QualifiedName, AggregationClass>();
 		compositionClasses = new HashMap<QualifiedName, CompositionClass>();
 	}
 
 	@Override
 	public String getVariableName() {
 		return "gc_" + getQualifiedName().replace('.', '_');
 	}
 
 	@Override
 	public EdgeClass createEdgeClass(QualifiedName name, VertexClass from,
 			VertexClass to) {
 		return createEdgeClass(name, from, 0, Integer.MAX_VALUE, "", to, 0,
 				Integer.MAX_VALUE, "");
 	}
 
 	@Override
 	public EdgeClass createEdgeClass(QualifiedName name, VertexClass from,
 			String fromRoleName, VertexClass to, String toRoleName) {
 		return createEdgeClass(name, from, 0, Integer.MAX_VALUE, fromRoleName,
 				to, 0, Integer.MAX_VALUE, toRoleName);
 	}
 
 	public EdgeClass createEdgeClass(QualifiedName name, VertexClass from,
 			int fromMin, int fromMax, VertexClass to, int toMin, int toMax) {
 		return createEdgeClass(name, from, fromMin, fromMax, "", to, toMin,
 				toMax, "");
 	}
 
 	@Override
 	public EdgeClass createEdgeClass(QualifiedName qn, VertexClass from,
 			int fromMin, int fromMax, String fromRoleName, VertexClass to,
 			int toMin, int toMax, String toRoleName) {
 		if (!schema.isValidSchemaElementName(qn)) {
 			throw new ReservedWordException(qn.getQualifiedName(), "EdgeClass");
 		}
 		if (schema.knows(qn)) {
 			throw new DuplicateNamedElementException(
 					"there is already an element with the name " + qn
 							+ " in the schema " + schema.getQualifiedName());
 		}
 
 		EdgeClassImpl ec = new EdgeClassImpl(qn, this, from, fromMin, fromMax,
 				fromRoleName, to, toMin, toMax, toRoleName);
 		if (!qn.getQualifiedName().equals("Edge")) {
 			EdgeClass s = schema.getDefaultEdgeClass();
 			ec.addSuperClass(s);
 		}
 		from.addEdgeClass(ec);
 		to.addEdgeClass(ec);
 
 		graphElementClasses.put(qn, ec);
 		edgeClasses.put(qn, ec);
 		Package pkg = schema.createPackageWithParents(qn.getPackageName());
 		ec.setPackage(pkg);
 		pkg.addEdgeClass(ec);
 		schema.addToKnownElements(qn.getUniqueName(), ec);
 		return ec;
 	}
 
 	@Override
 	public AggregationClass createAggregationClass(QualifiedName name,
 			VertexClass from, boolean aggregateFrom, VertexClass to) {
 		return createAggregationClass(name, from, 0, Integer.MAX_VALUE, "",
 				aggregateFrom, to, 0, Integer.MAX_VALUE, "");
 	}
 
 	@Override
 	public AggregationClass createAggregationClass(QualifiedName name,
 			VertexClass from, String fromRoleName, boolean aggregateFrom,
 			VertexClass to, String toRoleName) {
 		return createAggregationClass(name, from, 0, Integer.MAX_VALUE, "",
 				aggregateFrom, to, 0, Integer.MAX_VALUE, "");
 	}
 
 	@Override
 	public AggregationClass createAggregationClass(QualifiedName name,
 			VertexClass from, int fromMin, int fromMax, boolean aggregateFrom,
 			VertexClass to, int toMin, int toMax) {
 		return createAggregationClass(name, from, fromMin, fromMax, "",
 				aggregateFrom, to, toMin, toMax, "");
 	}
 
 	@Override
 	public AggregationClass createAggregationClass(QualifiedName qn,
 			VertexClass from, int fromMin, int fromMax, String fromRoleName,
 			boolean aggregateFrom, VertexClass to, int toMin, int toMax,
 			String toRoleName) {
 		if (!schema.isValidSchemaElementName(qn)) {
 			throw new ReservedWordException(qn.getQualifiedName(),
 					"AggregationClass");
 		}
 		if (schema.knows(qn)) {
 			throw new DuplicateNamedElementException(
 					"there is already an element with the name " + qn
 							+ " in the schema " + schema.getQualifiedName());
 		}
 		AggregationClassImpl ac = new AggregationClassImpl(qn, this, from,
 				fromMin, fromMax, fromRoleName, aggregateFrom, to, toMin,
 				toMax, toRoleName);
 		if (!qn.getQualifiedName().equals("Aggregation")) {
 			ac.addSuperClass(schema.getDefaultAggregationClass());
 		} else {
 			ac.addSuperClass(schema.getDefaultEdgeClass());
 		}
 		from.addEdgeClass(ac);
 		to.addEdgeClass(ac);
 		graphElementClasses.put(qn, ac);
 		aggregationClasses.put(qn, ac);
 		Package pkg = schema.createPackageWithParents(qn.getPackageName());
 		ac.setPackage(pkg);
 		pkg.addEdgeClass(ac);
 		schema.addToKnownElements(qn.getUniqueName(), ac);
 		return ac;
 	}
 
 	@Override
 	public CompositionClass createCompositionClass(QualifiedName name,
 			VertexClass from, boolean compositeFrom, VertexClass to) {
		return createCompositionClass(name, from, 0, 1, "",
				compositeFrom, to, 0, 1, "");
 	}
 
 	@Override
 	public CompositionClass createCompositionClass(QualifiedName name,
 			VertexClass from, String fromRoleName, boolean compositeFrom,
 			VertexClass to, String toRoleName) {
		return createCompositionClass(name, from, 0, 1,
				fromRoleName, compositeFrom, to, 0, 1,
 				toRoleName);
 	}
 
 	@Override
 	public CompositionClass createCompositionClass(QualifiedName name,
 			VertexClass from, int fromMin, int fromMax, boolean compositeFrom,
 			VertexClass to, int toMin, int toMax) {
 		return createCompositionClass(name, from, fromMin, fromMax, "",
 				compositeFrom, to, toMin, toMax, "");
 	}
 
 	@Override
 	public CompositionClass createCompositionClass(QualifiedName qn,
 			VertexClass from, int fromMin, int fromMax, String fromRoleName,
 			boolean compositeFrom, VertexClass to, int toMin, int toMax,
 			String toRoleName) {
 		if (!schema.isValidSchemaElementName(qn)) {
 			throw new ReservedWordException(qn.getQualifiedName(),
 					"CompositionClass");
 		}
 		if (schema.knows(qn)) {
 			throw new DuplicateNamedElementException(
 					"there is already an element with the name " + qn
 							+ " in the schema " + schema.getQualifiedName());
 		}
 
 		if (!qn.getQualifiedName().equals("Composition")) {
 			if (compositeFrom && fromMax > 1) {
 				throw new SchemaException("Couldn't create CompositionClass "
 						+ qn
 						+ ", because its multiplicity on composite side is ("
 						+ fromMin + ", " + fromMax
 						+ ").  Only (0, 1) and (1,1) are allowed.");
 			}
 			if (!compositeFrom && toMax > 1) {
 				throw new SchemaException("Couldn't create CompositionClass "
 						+ qn
 						+ ", because its multiplicity on composite side is ("
 						+ toMin + ", " + toMax
 						+ ").  Only (0, 1) and (1,1) are allowed.");
 			}
 		}
 
 		CompositionClassImpl cc = new CompositionClassImpl(qn, this, from,
 				fromMin, fromMax, fromRoleName, compositeFrom, to, toMin,
 				toMax, toRoleName);
 		if (!qn.getQualifiedName().equals("Composition")) {
 			cc.addSuperClass(schema.getDefaultCompositionClass());
 		} else {
 			cc.addSuperClass(schema.getDefaultAggregationClass());
 		}
 		from.addEdgeClass(cc);
 		to.addEdgeClass(cc);
 		graphElementClasses.put(qn, cc);
 		compositionClasses.put(qn, cc);
 		Package pkg = schema.createPackageWithParents(qn.getPackageName());
 		cc.setPackage(pkg);
 		pkg.addEdgeClass(cc);
 		schema.addToKnownElements(qn.getUniqueName(), cc);
 		return cc;
 	}
 
 	@Override
 	public VertexClass createVertexClass(QualifiedName qn) {
 		if (!schema.isValidSchemaElementName(qn)) {
 			throw new ReservedWordException(qn.getQualifiedName(),
 					"VertexClass");
 		}
 		if (schema.knows(qn)) {
 			throw new DuplicateNamedElementException(
 					"there is already an element with the name " + qn
 							+ " in the schema " + schema.getQualifiedName());
 		}
 
 		VertexClassImpl vc = new VertexClassImpl(qn, this);
 		vc.addSuperClass(schema.getDefaultVertexClass());
 		graphElementClasses.put(qn, vc);
 		vertexClasses.put(qn, vc);
 		schema.addToKnownElements(qn.getUniqueName(), vc);
 		Package pkg = schema.createPackageWithParents(qn.getPackageName());
 		vc.setPackage(pkg);
 		pkg.addVertexClass(vc);
 		return vc;
 	}
 
 	@Override
 	public void addSuperClass(GraphClass superClass) {
 		// only the internal abstract base class "Graph" can be a superclass
 		if (!superClass.getQualifiedName().equals("Graph")) {
 			throw new InheritanceException(
 					"GraphClass can not be generealized.");
 		}
 		super.addSuperClass(superClass);
 	}
 
 	@Override
 	public void addSubClass(GraphClass subClass) {
 		throw new InheritanceException("GraphClass can not be generealized.");
 	}
 
 	@Override
 	public boolean knowsOwn(GraphElementClass aGraphElementClass) {
 		return (graphElementClasses.containsValue(aGraphElementClass));
 	}
 
 	@Override
 	public boolean knowsOwn(QualifiedName aGraphElementClass) {
 		return (graphElementClasses.containsKey(aGraphElementClass));
 	}
 
 	@Override
 	public boolean knows(GraphElementClass aGraphElementClass) {
 		if (graphElementClasses.containsKey(aGraphElementClass)) {
 			return true;
 		}
 		for (AttributedElementClass superClass : directSuperClasses) {
 			if (((GraphClass) superClass).knows(aGraphElementClass)) {
 				return true;
 			}
 		}
 		return false;
 	}
 
 	@Override
 	public boolean knows(QualifiedName aGraphElementClass) {
 		if (graphElementClasses.containsKey(aGraphElementClass)) {
 			return true;
 		}
 		for (AttributedElementClass superClass : directSuperClasses) {
 			if (((GraphClass) superClass).knows(aGraphElementClass)) {
 				return true;
 			}
 		}
 		return false;
 	}
 
 	@Override
 	public GraphElementClass getGraphElementClass(QualifiedName qn) {
 		if (graphElementClasses.containsKey(qn)) {
 			return graphElementClasses.get(qn);
 		}
 		for (AttributedElementClass superClass : directSuperClasses) {
 			if (((GraphClass) superClass).knows(qn)) {
 				return ((GraphClass) superClass).getGraphElementClass(qn);
 			}
 		}
 		return null;
 	}
 
 	@Override
 	public String toString() {
 		String output = "GraphClassImpl '" + getQualifiedName() + "'";
 		if (isAbstract()) {
 			output += " (abstract)";
 		}
 		output += ": \n";
 
 		output += "subClasses of '" + getQualifiedName() + "': ";
 		Iterator<AttributedElementClass> it = getAllSubClasses().iterator();
 		while (it.hasNext()) {
 			output += "'" + ((GraphClassImpl) it.next()).getQualifiedName()
 					+ "' ";
 		}
 
 		output += "\nsuperClasses of '" + getQualifiedName() + "': ";
 		Iterator<AttributedElementClass> it2 = getAllSuperClasses().iterator();
 		while (it2.hasNext()) {
 			output += "'" + ((GraphClassImpl) it2.next()).getQualifiedName()
 					+ "' ";
 		}
 		output += attributesToString();
 
 		output += "\n\nGraphElementClasses of '" + getQualifiedName()
 				+ "':\n\n";
 		Iterator<GraphElementClass> it3 = graphElementClasses.values()
 				.iterator();
 		while (it3.hasNext()) {
 			output += it3.next().toString() + "\n";
 		}
 		return output;
 	}
 
 	@Override
 	public List<GraphElementClass> getOwnGraphElementClasses() {
 		return new ArrayList<GraphElementClass>(graphElementClasses.values());
 	}
 
 	@Override
 	public List<GraphElementClass> getGraphElementClasses() {
 		List<GraphElementClass> allClasses = new ArrayList<GraphElementClass>();
 
 		for (AttributedElementClass superGraphClass : getAllSuperClasses()) {
 			allClasses.addAll(((GraphClass) superGraphClass)
 					.getOwnGraphElementClasses());
 		}
 
 		allClasses.addAll(graphElementClasses.values());
 
 		return allClasses;
 	}
 
 	@Override
 	public List<EdgeClass> getOwnEdgeClasses() {
 		List<EdgeClass> list = new ArrayList<EdgeClass>(edgeClasses.values());
 		for (EdgeClass ac : getOwnAggregationClasses()) {
 			list.add(ac);
 		}
 		return list;
 	}
 
 	@Override
 	public List<EdgeClass> getEdgeClasses() {
 		List<EdgeClass> allClasses = new ArrayList<EdgeClass>();
 
 		for (AttributedElementClass superGraphClass : getAllSuperClasses()) {
 			allClasses.addAll(((GraphClass) superGraphClass)
 					.getOwnEdgeClasses());
 		}
 
 		allClasses.addAll(getOwnEdgeClasses());
 
 		return allClasses;
 	}
 
 	@Override
 	public List<CompositionClass> getOwnCompositionClasses() {
 		return new ArrayList<CompositionClass>(compositionClasses.values());
 	}
 
 	@Override
 	public List<CompositionClass> getCompositionClasses() {
 		List<CompositionClass> allClasses = new ArrayList<CompositionClass>();
 
 		for (AttributedElementClass superGraphClass : getAllSuperClasses()) {
 			allClasses.addAll(((GraphClass) superGraphClass)
 					.getOwnCompositionClasses());
 		}
 
 		allClasses.addAll(getOwnCompositionClasses());
 
 		return allClasses;
 	}
 
 	@Override
 	public List<AggregationClass> getOwnAggregationClasses() {
 		List<AggregationClass> list = new ArrayList<AggregationClass>(
 				aggregationClasses.values());
 		for (AggregationClass cc : getOwnCompositionClasses()) {
 			list.add(cc);
 		}
 		return list;
 	}
 
 	@Override
 	public List<AggregationClass> getAggregationClasses() {
 		List<AggregationClass> allClasses = new ArrayList<AggregationClass>();
 
 		for (AttributedElementClass superGraphClass : getAllSuperClasses()) {
 			allClasses.addAll(((GraphClass) superGraphClass)
 					.getOwnAggregationClasses());
 		}
 
 		allClasses.addAll(getOwnAggregationClasses());
 
 		return allClasses;
 	}
 
 	@Override
 	public List<VertexClass> getOwnVertexClasses() {
 		return new ArrayList<VertexClass>(vertexClasses.values());
 	}
 
 	@Override
 	public List<VertexClass> getVertexClasses() {
 		List<VertexClass> allClasses = new ArrayList<VertexClass>();
 
 		for (AttributedElementClass superGraphClass : getAllSuperClasses()) {
 			allClasses.addAll(((GraphClass) superGraphClass)
 					.getOwnVertexClasses());
 		}
 
 		allClasses.addAll(vertexClasses.values());
 
 		return allClasses;
 	}
 
 	@Override
 	public Schema getSchema() {
 		return schema;
 	}
 
 	@Override
 	public int getOwnEdgeClassCount() {
 		return edgeClasses.size();
 	}
 
 	@Override
 	public int getOwnVertexClassCount() {
 		return vertexClasses.size();
 	}
 
 	@Override
 	public VertexClass getVertexClass(QualifiedName name) {
 		VertexClass vc = vertexClasses.get(name);
 		if (vc != null) {
 			return vc;
 		}
 		for (AttributedElementClass superclass : directSuperClasses) {
 			vc = ((GraphClass) superclass).getVertexClass(name);
 			if (vc != null) {
 				return vc;
 			}
 		}
 		return null;
 	}
 
 	@Override
 	public EdgeClass getEdgeClass(QualifiedName name) {
 		EdgeClass ec = edgeClasses.get(name);
 		if (ec != null) {
 			return ec;
 		}
 		ec = aggregationClasses.get(name);
 		if (ec != null) {
 			return ec;
 		}
 		ec = compositionClasses.get(name);
 		if (ec != null) {
 			return ec;
 		}
 		for (AttributedElementClass superclass : directSuperClasses) {
 			ec = ((GraphClass) superclass).getEdgeClass(name);
 			if (ec != null) {
 				return ec;
 			}
 		}
 		return null;
 	}
 
 	@Override
 	public CompositionClass getCompositionClass(QualifiedName name) {
 		CompositionClass cc = compositionClasses.get(name);
 		if (cc != null) {
 			return cc;
 		}
 		for (AttributedElementClass superclass : directSuperClasses) {
 			cc = ((GraphClass) superclass).getCompositionClass(name);
 			if (cc != null) {
 				return cc;
 			}
 		}
 		return null;
 	}
 
 	@Override
 	public AggregationClass getAggregationClass(QualifiedName name) {
 		AggregationClass ac = aggregationClasses.get(name);
 		if (ac != null) {
 			return ac;
 		}
 		ac = compositionClasses.get(name);
 		if (ac != null) {
 			return ac;
 		}
 		for (AttributedElementClass superclass : directSuperClasses) {
 			ac = ((GraphClass) superclass).getAggregationClass(name);
 			if (ac != null) {
 				return ac;
 			}
 		}
 		return null;
 	}
 
 }
