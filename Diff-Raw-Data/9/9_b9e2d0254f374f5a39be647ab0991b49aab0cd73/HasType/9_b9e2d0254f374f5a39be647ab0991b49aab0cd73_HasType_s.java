 /*
  * JGraLab - The Java graph laboratory
  * (c) 2006-2009 Institute for Software Technology
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
 
 package de.uni_koblenz.jgralab.greql2.funlib;
 
 import java.util.ArrayList;
 
 import de.uni_koblenz.jgralab.AttributedElement;
 import de.uni_koblenz.jgralab.BooleanGraphMarker;
 import de.uni_koblenz.jgralab.Graph;
 import de.uni_koblenz.jgralab.greql2.exception.EvaluateException;
 import de.uni_koblenz.jgralab.greql2.exception.WrongFunctionParameterException;
 import de.uni_koblenz.jgralab.greql2.jvalue.JValue;
 import de.uni_koblenz.jgralab.greql2.jvalue.JValueType;
 import de.uni_koblenz.jgralab.greql2.jvalue.JValueTypeCollection;
 import de.uni_koblenz.jgralab.schema.AttributedElementClass;
 
 /**
  * Checks if the given edge or vertex has the given type. The type can be given
  * as AttributedElementClass or as String which holds the typename.
  *
  * <dl>
  * <dt><b>GReQL-signature</b></dt>
  * <dd><code>BOOLEAN hasType(ae:ATTRIBUTEDELEMENT, type:STRING)</code></dd>
  * <dd>
  * <code>BOOLEAN hasType(ae:ATTRIBUTEDELEMENT, aec:ATTRIBUTEDELEMENTCLASS)</code>
  * </dd>
  * <dd>&nbsp;</dd>
  * </dl>
  * <dl>
  * <dt></dt>
  * <dd>
  * <dl>
  * <dt><b>Parameters:</b></dt>
  * <dd><code>ae</code> - attributed element to check</dd>
  * <dd><code>type</code> - name of the type to check for</dd>
  * <dd><code>aec</code> - attributed element class which is the type to check
  * for</dd>
  * <dt><b>Returns:</b></dt>
  * <dd><code>true</code> if the given attributed element has the given type</dd>
  * <dd><code>Null</code> if one of the parameters is <code>Null</code></dd>
  * <dd><code>false</code> otherwise</dd>
  * </dl>
  * </dd>
  * </dl>
  *
  * @author ist@uni-koblenz.de
  *
  */
 
 public class HasType extends AbstractGreql2Function {
 	{
 		JValueType[][] x = {
 				{ JValueType.ATTRIBUTEDELEMENT, JValueType.STRING },
 				{ JValueType.ATTRIBUTEDELEMENT,
 						JValueType.ATTRIBUTEDELEMENTCLASS },
 				{ JValueType.ATTRIBUTEDELEMENT, JValueType.TYPECOLLECTION } };
 		signatures = x;
 	}
 
 	public JValue evaluate(Graph graph, BooleanGraphMarker subgraph,
 			JValue[] arguments) throws EvaluateException {
 		String typeName = null;
 		AttributedElementClass aeClass = null;
 		JValueTypeCollection typeCollection = null;
 		switch (checkArguments(arguments)) {
 		case 0:
 			typeName = arguments[1].toString();
 			break;
 		case 1:
 			aeClass = arguments[1].toAttributedElementClass();
 			break;
 		case 2:
 			typeCollection = arguments[1].toJValueTypeCollection();
 			break;
 		default:
 			throw new WrongFunctionParameterException(this, null, arguments);
 		}
 		AttributedElement elem = arguments[0].toAttributedElement();
 
 		if (typeCollection != null) {
 			return new JValue(typeCollection.acceptsType(elem
 					.getAttributedElementClass()), elem);
 		}
 
 		if (aeClass != null) {
			return new JValue(elem.getAttributedElementClass() == aeClass, elem);
 		}
 
 		AttributedElementClass type = elem.getSchema()
 				.getAttributedElementClass(typeName);
		return new JValue(type.getClass().isInstance(elem.getAttributedElementClass()), elem);

 	}
 
 	public long getEstimatedCosts(ArrayList<Long> inElements) {
 		return 2;
 	}
 
 	public double getSelectivity() {
 		return 0.1;
 	}
 
 	public long getEstimatedCardinality(int inElements) {
 		return 1;
 	}
 
 }
