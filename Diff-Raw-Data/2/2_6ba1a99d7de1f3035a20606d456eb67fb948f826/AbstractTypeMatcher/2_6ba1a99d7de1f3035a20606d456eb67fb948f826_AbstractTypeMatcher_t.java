 /*
  * HUMBOLDT: A Framework for Data Harmonisation and Service Integration.
  * EU Integrated Project #030962                 01.10.2006 - 30.09.2010
  * 
  * For more information on the project, please refer to the this web site:
  * http://www.esdi-humboldt.eu
  * 
  * LICENSE: For information on the license under which this program is 
  * available, please refer to http:/www.esdi-humboldt.eu/license.html#core
  * (c) the HUMBOLDT Consortium, 2007 to 2011.
  */
 
 package eu.esdihumboldt.hale.io.gml.writer.internal.geometry;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Queue;
 
 import org.opengis.feature.type.Name;
 
 import eu.esdihumboldt.hale.schemaprovider.model.AttributeDefinition;
 import eu.esdihumboldt.hale.schemaprovider.model.SchemaElement;
 import eu.esdihumboldt.hale.schemaprovider.model.TypeDefinition;
 
 /**
  * Abstract type matcher. Finds candidates matching a custom parameter.
  * @param <T> the match parameter type
  *
  * @author Simon Templer
  * @partner 01 / Fraunhofer Institute for Computer Graphics Research
  */
 public abstract class AbstractTypeMatcher<T> {
 	
 	/**
 	 * Path candidate
 	 */
 	private static class PathCandidate {
 
 		private final TypeDefinition type;
 		private final DefinitionPath path;
 		private final HashSet<TypeDefinition> checkedTypes;
 
 		/**
 		 * Constructor
 		 * 
 		 * @param type the associated type
 		 * @param path the definition path
 		 * @param checkedTypes the type definitions that have already been checked
 		 *   (to prevent cycles)
 		 */
 		public PathCandidate(TypeDefinition type,
 				DefinitionPath path, HashSet<TypeDefinition> checkedTypes) {
 			this.type = type;
 			this.path = path;
 			this.checkedTypes = checkedTypes;
 		}
 
 		/**
 		 * @return the attributeType
 		 */
 		public TypeDefinition getType() {
 			return type;
 		}
 
 		/**
 		 * @return the definitionPath
 		 */
 		public DefinitionPath getPath() {
 			return path;
 		}
 
 		/**
 		 * @return the handledTypes
 		 */
 		public HashSet<TypeDefinition> getCheckedTypes() {
 			return checkedTypes;
 		}
 
 	}
 	
 	/**
 	 * Find candidates for a possible path
 	 * 
 	 * @param elementType the start element type
 	 * @param elementName the start element name
 	 * @param matchParam the match parameter
 	 * 
 	 * @return the path candidates
 	 */
 	public List<DefinitionPath> findCandidates(TypeDefinition elementType, 
 			Name elementName, T matchParam) {
 		Queue<PathCandidate> candidates = new LinkedList<PathCandidate>();
 		PathCandidate base = new PathCandidate(elementType, 
 				new DefinitionPath(elementType, elementName),
 				new HashSet<TypeDefinition>());
 		candidates.add(base);
 		
 		while (!candidates.isEmpty()) {
 			PathCandidate candidate = candidates.poll();
 			TypeDefinition type = candidate.getType();
 			DefinitionPath basePath = candidate.getPath();
 			HashSet<TypeDefinition> checkedTypes = candidate.getCheckedTypes();
 			
 			if (checkedTypes.contains(type)) {
 				continue; // prevent cycles
 			}
 			else {
 				checkedTypes.add(type);
 			}
 			
 			// check if there is a direct match
 			DefinitionPath path = matchPath(type, matchParam, basePath);
 			if (path != null) {
 				return Collections.singletonList(path); // return instantly
 				//XXX currently always only one path is returned - this might change if we allow matchPath to yield multiple results
 			}
 			
 			if (!type.isAbstract()) { // only allow stepping down properties if the type is not abstract
 				// step down properties
 				Iterable<AttributeDefinition> properties = (basePath.isEmpty() || basePath.getLastElement().isProperty())?(type.getAttributes()):(type.getDeclaredAttributes());
 				for (AttributeDefinition att : properties) {
					if (att.isElement() && att.getAttributeType() != null) { // only descend into elements, only descend if there actually is a type definition available
 						candidates.add(new PathCandidate(att.getAttributeType(), 
 								new DefinitionPath(basePath).addProperty(att), 
 								new HashSet<TypeDefinition>(checkedTypes)));
 					}
 				}
 			}
 			
 			// step down sub-types
 //			for (TypeDefinition subtype : type.getSubTypes()) {
 //				candidates.add(new PathCandidate(subtype,
 //						new DefinitionPath(basePath).addSubType(subtype),
 //						new HashSet<TypeDefinition>(checkedTypes)));
 //			}
 			for (SchemaElement element : type.getSubstitutions(basePath.getLastName())) {
 				candidates.add(new PathCandidate(element.getType(),
 						new DefinitionPath(basePath).addSubstitution(element),
 						new HashSet<TypeDefinition>(checkedTypes)));
 			}
 		}
 		
 		return new ArrayList<DefinitionPath>();
 	}
 	
 	/**
 	 * Determines if a type definition is compatible with the match parameter
 	 *  
 	 * @param type the type definition
 	 * @param matchParam the match parameter
 	 * @param path the current definition path
 	 * 
 	 * @return the (eventually updated) definition path if a match is found,
 	 * otherwise <code>null</code>
 	 */
 	protected abstract DefinitionPath matchPath(TypeDefinition type, 
 			T matchParam, DefinitionPath path);
 
 }
