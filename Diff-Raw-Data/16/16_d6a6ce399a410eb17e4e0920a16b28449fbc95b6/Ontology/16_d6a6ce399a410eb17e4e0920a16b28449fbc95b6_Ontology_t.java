 /**
  * Ontology.java
  * ----------------------------------------------------------------------------------
  * 
  * Copyright (C) 2008 www.integratedmodelling.org
  * Created: Mar 3, 2008
  *
  * ----------------------------------------------------------------------------------
  * This file is part of Thinklab.
  * 
  * Thinklab is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 3 of the License, or
  * (at your option) any later version.
  * 
  * Thinklab is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  * 
  * You should have received a copy of the GNU General Public License
  * along with the software; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
  * 
  * ----------------------------------------------------------------------------------
  * 
  * @copyright 2008 www.integratedmodelling.org
  * @author    Ioannis N. Athanasiadis (ioannis@athanasiadis.info)
  * @date      Mar 3, 2008
  * @license   http://www.gnu.org/licenses/gpl.txt GNU General Public License v3
  * @link      http://www.integratedmodelling.org
  **/
 package org.integratedmodelling.thinklab.owlapi;
 
 import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.net.MalformedURLException;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import org.apache.log4j.Logger;
 import org.integratedmodelling.thinklab.KnowledgeManager;
 import org.integratedmodelling.thinklab.SemanticType;
 import org.integratedmodelling.thinklab.exception.ThinklabAmbiguousResultException;
 import org.integratedmodelling.thinklab.exception.ThinklabDuplicateNameException;
 import org.integratedmodelling.thinklab.exception.ThinklabException;
 import org.integratedmodelling.thinklab.exception.ThinklabIOException;
 import org.integratedmodelling.thinklab.exception.ThinklabMalformedSemanticTypeException;
 import org.integratedmodelling.thinklab.exception.ThinklabNoKMException;
 import org.integratedmodelling.thinklab.exception.ThinklabResourceNotFoundException;
 import org.integratedmodelling.thinklab.exception.ThinklabRuntimeException;
 import org.integratedmodelling.thinklab.owlapi.ThinklabOWLManager;
 import org.integratedmodelling.thinklab.interfaces.IConcept;
 import org.integratedmodelling.thinklab.interfaces.IInstance;
 import org.integratedmodelling.thinklab.interfaces.IKnowledge;
 import org.integratedmodelling.thinklab.interfaces.IOntology;
 import org.integratedmodelling.thinklab.interfaces.IProperty;
 import org.integratedmodelling.thinklab.interfaces.IResource;
 import org.integratedmodelling.utils.MiscUtilities;
 import org.integratedmodelling.utils.NameGenerator;
 import org.integratedmodelling.utils.Pair;
 import org.integratedmodelling.utils.Polylist;
 import org.semanticweb.owl.io.OWLXMLOntologyFormat;
 import org.semanticweb.owl.model.AddAxiom;
 import org.semanticweb.owl.model.OWLAxiom;
 import org.semanticweb.owl.model.OWLClass;
 import org.semanticweb.owl.model.OWLDataProperty;
 import org.semanticweb.owl.model.OWLEntity;
 import org.semanticweb.owl.model.OWLIndividual;
 import org.semanticweb.owl.model.OWLObjectProperty;
 import org.semanticweb.owl.model.OWLOntology;
 import org.semanticweb.owl.model.OWLOntologyChangeException;
 import org.semanticweb.owl.model.OWLOntologyStorageException;
 import org.semanticweb.owl.model.UnknownOWLOntologyException;
 import org.semanticweb.owl.util.OWLEntityRemover;
 import org.semanticweb.owl.util.OWLOntologyMerger;
 
 
 
 /**
  * @author Ioannis N. Athanasiadis, Dalle Molle Institute for Artificial Intelligence, USI/SUPSI
  * @author Ferdinando Villa
  * 
  * @since 13 sth 2006
  */
 public class Ontology implements IOntology {
 	
 	private String conceptSpace; // i.e. owl or xml or fs
 	protected FileKnowledgeRepository kr;
 	
 	private Logger log = Logger.getLogger(Ontology.class);
 	private long lastModified;
 	private boolean acceptDuplicateIDs = false;
 	protected OWLOntology ont;
 	private Map<SemanticType,IConcept> concepts;
 	private Map<SemanticType,IProperty> properties;
 	private Map<SemanticType,IInstance> instances;
 	private String cs;
 
 	class ReferenceRecord {
 		public IInstance target;
 		public String reference;
 		public IProperty property;
 		
 		public ReferenceRecord(IInstance t, IProperty prop, String ref) {
 			target = t;
 			reference = ref;
 			property = prop;
 		}
 	}
 	private void resolveReferences (Collection<ReferenceRecord> refs) throws ThinklabException {
 
 		for (ReferenceRecord r : refs) {
 			
 			IInstance rr = this.getInstance(r.reference);
 			if (rr == null) {
 				throw new ThinklabResourceNotFoundException("internal error: can't resolve reference to object " + r.reference);
 			}
 			
 			r.target.addObjectRelationship(r.property, rr);
 		}
 	}
 	
 	public Ontology(OWLOntology onto, FileKnowledgeRepository kr){
 		
 		this.ont = onto;
 		this.kr = kr;
 		this.concepts = new HashMap<SemanticType, IConcept>();
 		this.properties = new HashMap<SemanticType, IProperty>();
 		this.instances = new HashMap<SemanticType, IInstance>();
 	}
 	
 	protected void initialize(String cs){
 		
 		this.cs = cs;
 		
 		Set<OWLClass> allCl  = ont.getReferencedClasses();
 		for(OWLClass cl:allCl){
			
			if (cl.getURI().toString().startsWith(ont.getURI().toString()))
				concepts.put(kr.registry.getSemanticType(cl), new Concept(cl));
 		}
 		Set<OWLDataProperty> allPr = ont.getReferencedDataProperties();
 		for(OWLDataProperty p:allPr){
			if (p.getURI().toString().startsWith(ont.getURI().toString()))
				properties.put(kr.registry.getSemanticType(p), new Property(p));
 		}
 		
 		Set<OWLObjectProperty> objPr = ont.getReferencedObjectProperties();
 		for(OWLObjectProperty p:objPr){
			if (p.getURI().toString().startsWith(ont.getURI().toString()))
				properties.put(kr.registry.getSemanticType(p), new Property(p));
 		}
 		
 		Set<OWLIndividual> allIn = ont.getReferencedIndividuals();
 		for(OWLIndividual i:allIn){
			if (i.getURI().toString().startsWith(ont.getURI().toString()))
				instances.put(kr.registry.getSemanticType(i), new Instance(i));
 		}
 	}
 
 	
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IOntology#getConceptSpace()
 	 */
 	public String getConceptSpace() {
 		return cs;
 	}
 	
 	
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IOntology#getConcept(java.lang.String)
 	 */
 	public IConcept getConcept(String ID) {	
 		return concepts.get(new SemanticType(cs,ID));
 	}
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IOntology#getConcepts()
 	 */
 	public Collection<IConcept> getConcepts() {
 		return concepts.values();
 	}
 	
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IOntology#getProperty(java.lang.String)
 	 */
 	public IProperty getProperty(String ID) {
 		return properties.get(new SemanticType(cs, ID));
 	}
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IOntology#getProperties()
 	 */
 	public Collection<IProperty> getProperties() {
 		return properties.values();
 	}
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IOntology#getInstance(java.lang.String)
 	 */
 	public IInstance getInstance(String ID) {
 		return instances.get(new SemanticType(cs, ID));
 	}
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IOntology#getInstances()
 	 */
 	public Collection<IInstance> getInstances() throws ThinklabException {
 		return instances.values();
 	}
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IOntology#allowDuplicateInstanceIDs()
 	 */
 	public void allowDuplicateInstanceIDs() {
 		acceptDuplicateIDs = true;
 	}
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IOntology#createEquivalence(org.integratedmodelling.thinklab.interfaces.IInstance, org.integratedmodelling.thinklab.interfaces.IInstance)
 	 */
 	public void createEquivalence(IInstance o1, IInstance o2) {
 
 		OWLIndividual ind1 = ((Instance)o1).entity.asOWLIndividual();
 		OWLIndividual ind2 = ((Instance)o2).entity.asOWLIndividual();
 		
 		Set<OWLIndividual> oi = new HashSet<OWLIndividual>();
 		
 		oi.add(ind1);
 		oi.add(ind2);
 		
 		OWLAxiom ax = FileKnowledgeRepository.df.getOWLSameIndividualsAxiom(oi);
 		AddAxiom add = new AddAxiom(ont,ax);
 		
 		try {
 			FileKnowledgeRepository.get().manager.applyChange(add);
 		} catch (OWLOntologyChangeException e) {
 			throw new ThinklabRuntimeException(e);
 		}
 	}
 
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IOntology#createInstance(java.lang.String, org.integratedmodelling.thinklab.interfaces.IConcept)
 	 */
 	public IInstance createInstance(String ID, IConcept c) throws ThinklabException {
 		
 		String label = null;
 		
 		if(c.isAbstract()) {
 			throw new ThinklabIOException(" cannot create an instance of abstract concept " + c);
 		}
 
 		if (ID == null) {
 			ID = getUniqueObjectName(getConceptSpace());
 		}
 		
 		if (getInstance(ID) != null) {
 			
 			if (!acceptDuplicateIDs) {
 				
 				throw new ThinklabDuplicateNameException("instance with name " + ID
 						+ " already exists in concept space " + conceptSpace);
 			}
 			
 			/* if we find a duplicated ID, we add progressive numbers to it, and
 			 * we set the desired instance as a label, which can later be overwritten
 			 * by any label the user has specified.
 			 */
 			int cnt = 1;
 			label = ID;
 			
 			do {
 				// FIXME this makes the string longer and longer
 				ID = ID + "_" + cnt++;
 			} while (getInstance(ID) != null);
 		}
 
 		OWLIndividual ind = 
 			FileKnowledgeRepository.df.getOWLIndividual(URI.create(ont.getURI() + "#" + ID));
 		OWLAxiom ax = 
 			FileKnowledgeRepository.df.getOWLClassAssertionAxiom(ind, ((Concept)c).entity.asOWLClass());
 		AddAxiom add = new AddAxiom(ont,ax);
 		
 		try {
 			FileKnowledgeRepository.get().manager.applyChange(add);
 		} catch (OWLOntologyChangeException e) {
 			throw new ThinklabRuntimeException(e);
 		}
 		
 		Instance inst = new Instance(ind);
 		instances.put(new SemanticType(cs, ID), inst);
 		return inst;
 	}
 
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IOntology#createInstance(org.integratedmodelling.thinklab.interfaces.IInstance)
 	 */
 	public IInstance createInstance(IInstance i) throws ThinklabException {
 		
 		IInstance inst = getInstance(i.getLocalName());
 		if (inst != null)
 			return inst;
 		return createInstance(i.toList(null));
 	}
 
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IOntology#createInstance(java.lang.String, org.integratedmodelling.utils.Polylist)
 	 */
 	public IInstance createInstance(String ID, Polylist list)
 			throws ThinklabException {
 		
 		ArrayList<ReferenceRecord> reftable = new ArrayList<ReferenceRecord>();	
 		IInstance ret  = createInstanceInternal(ID, list, reftable);
 		resolveReferences(reftable);
 
 		return ret;
 	}
 
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IOntology#createInstance(org.integratedmodelling.utils.Polylist)
 	 */
 	public IInstance createInstance(Polylist list) throws ThinklabException {
 		
 		ArrayList<ReferenceRecord> reftable = new ArrayList<ReferenceRecord>();	
 		IInstance ret  = createInstanceInternal(list, reftable);
 		resolveReferences(reftable);
 
 		return ret;
 
 	}
 	
 	IInstance createInstanceInternal(String ID, Polylist list,
 			Collection<ReferenceRecord> reftable) throws ThinklabException {
 
 		IInstance ret = null;
 
 		for (Object o : list.array()) {
 
 			if (ret == null) {
 
 				Pair<IConcept, String> cc = ThinklabOWLManager
 						.getConceptFromListObject(o);
 				ret = createInstance(ID, cc.getFirst());
 
 			} else if (o instanceof Polylist) {
 				ThinklabOWLManager.get().interpretPropertyList((Polylist) o,
 						this, ret, reftable);
 			}
 		}
 
 		return ret;
 	}
 
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IOntology#getLastModificationDate()
 	 */
 	public long getLastModificationDate() {
 		// TODO Auto-generated method stub
 		return 0;
 	}
 
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IOntology#getURI()
 	 */
 	public String getURI() {
 		return ont.getURI().toString();
 	}
 
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IOntology#getUniqueObjectName(java.lang.String)
 	 */
 	public String getUniqueObjectName(String prefix) {
 		/*
 		 * FIXME
 		 * not the most efficient way, but OWLAPI doesn't seem to have a catalog of names or 
 		 * a name generation function like protege.
 		 */
 		String ret = NameGenerator.newName(prefix);
 		while (containsName(ret)) {
 			ret = NameGenerator.newName(prefix);
 		}
 		return ret;
 	}
 
 
 	private boolean containsName(String ret) {
 
 		SemanticType st = new SemanticType(cs, ret);
 		return 
 			instances.get(st) != null ||
 			concepts.get(st) != null ||
 			properties.get(st) != null;
 	}
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IOntology#read(java.net.URL)
 	 */
 	public void read(URL url) {
 
 		// TODO
 		
 	}
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IOntology#removeInstance(java.lang.String)
 	 */
 	public void removeInstance(String id) throws ThinklabException {
 
         OWLEntityRemover remover = 
         	new OWLEntityRemover(FileKnowledgeRepository.get().manager, Collections.singleton(ont));
 
         Instance inst = (Instance) instances.get(new SemanticType(cs, id));
 
         if (inst != null) {
         	
         	inst.entity.accept(remover);
 
         	try {
         		FileKnowledgeRepository.get().manager.applyChanges(remover.getChanges());
         	} catch (OWLOntologyChangeException e) {
         		throw new ThinklabRuntimeException(e);
         	}
         }
 
 	}
 
 	public void write(URI physicalURI) throws ThinklabException{
 
 		try {
 			FileKnowledgeRepository.get().manager.
 				saveOntology(ont, new OWLXMLOntologyFormat(), physicalURI);
 		} catch (Exception e) {
 			throw new ThinklabIOException(e);
 		}
 	}
 
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IResource#addDescription(java.lang.String)
 	 */
 	public void addDescription(String desc) {
 		// TODO Auto-generated method stub
 	
 	}
 
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IResource#addDescription(java.lang.String, java.lang.String)
 	 */
 	public void addDescription(String desc, String language) {
 		// TODO Auto-generated method stub
 		
 	}
 
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IResource#addLabel(java.lang.String)
 	 */
 	public void addLabel(String desc) {
 		// TODO Auto-generated method stub
 		
 	}
 
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IResource#addLabel(java.lang.String, java.lang.String)
 	 */
 	public void addLabel(String desc, String language) {
 		// TODO Auto-generated method stub
 		
 	}
 
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IResource#equals(java.lang.String)
 	 */
 	public boolean equals(String s) {
 		// TODO Auto-generated method stub
 		return false;
 	}
 
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IResource#equals(org.integratedmodelling.thinklab.interfaces.IResource)
 	 */
 	public boolean equals(IResource r) {
 		// TODO Auto-generated method stub
 		return false;
 	}
 
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IResource#getDescription()
 	 */
 	public String getDescription() {
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IResource#getDescription(java.lang.String)
 	 */
 	public String getDescription(String languageCode) {
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IResource#getLabel()
 	 */
 	public String getLabel() {
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 
 	/* (non-Javadoc)
 	 * @see org.integratedmodelling.thinklab.interfaces.IResource#getLabel(java.lang.String)
 	 */
 	public String getLabel(String languageCode) {
 		// TODO Auto-generated method stub
 		return null;
 	}
 	
 	public String toString(){
 		return cs;
 	}
 	    
 	protected IKnowledge resolveURI(URI uri){
 		SemanticType st = Registry.get().getSemanticType(uri);
 		if (concepts.containsKey(st)) return concepts.get(st);
 		else if (properties.containsKey(st)) return properties.get(st);
 		else if(instances.containsKey(st)) return instances.get(st);
 		else return null;
 	}
 
 	public IInstance createInstanceInternal(Polylist list,
 			Collection<ReferenceRecord> reftable) throws ThinklabException {
 		
 		IInstance ret = null;
 		
 		for (Object o : list.array()) {
 			
 			/**
 			 * We may simply add an instance as the only list element, meaning we just want to 
 			 * use it without creating anything.
 			 */
 			if (o instanceof IInstance) {
 				return (IInstance)o;
 			}
 			
 			if (ret == null) {
 
 				Pair<IConcept, String> cc = ThinklabOWLManager.getConceptFromListObject(o);
 				ret = createInstance(
 						cc.getSecond() == null ? getUniqueObjectName(getConceptSpace()) : cc.getSecond(),  
 						cc.getFirst());		
 				
 			} else if (o instanceof Polylist) {
 				ThinklabOWLManager.get().interpretPropertyList((Polylist)o, this, ret, reftable);
 			}
 			
 		}
 		
 		return ret;
 	}
 
 }
