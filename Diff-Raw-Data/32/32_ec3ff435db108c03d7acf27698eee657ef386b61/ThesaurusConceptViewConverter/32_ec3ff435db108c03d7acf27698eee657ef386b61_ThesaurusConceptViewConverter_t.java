 /**
  * Copyright or © or Copr. Ministère Français chargé de la Culture
  * et de la Communication (2013)
  * <p/>
  * contact.gincoculture_at_gouv.fr
  * <p/>
  * This software is a computer program whose purpose is to provide a thesaurus
  * management solution.
  * <p/>
  * This software is governed by the CeCILL license under French law and
  * abiding by the rules of distribution of free software. You can use,
  * modify and/ or redistribute the software under the terms of the CeCILL
  * license as circulated by CEA, CNRS and INRIA at the following URL
  * "http://www.cecill.info".
  * <p/>
  * As a counterpart to the access to the source code and rights to copy,
  * modify and redistribute granted by the license, users are provided only
  * with a limited warranty and the software's author, the holder of the
  * economic rights, and the successive licensors have only limited liability.
  * <p/>
  * In this respect, the user's attention is drawn to the risks associated
  * with loading, using, modifying and/or developing or reproducing the
  * software by the user in light of its specific status of free software,
  * that may mean that it is complicated to manipulate, and that also
  * therefore means that it is reserved for developers and experienced
  * professionals having in-depth computer knowledge. Users are therefore
  * encouraged to load and test the software's suitability as regards their
  * requirements in conditions enabling the security of their systemsand/or
  * data to be ensured and, more generally, to use and operate it in the
  * same conditions as regards security.
  * <p/>
  * The fact that you are presently reading this means that you have had
  * knowledge of the CeCILL license and that you accept its terms.
  */
 package fr.mcc.ginco.extjs.view.utils;
 
 import fr.mcc.ginco.ark.IIDGeneratorService;
 import fr.mcc.ginco.beans.AssociativeRelationship;
 import fr.mcc.ginco.beans.Thesaurus;
 import fr.mcc.ginco.beans.ThesaurusConcept;
 import fr.mcc.ginco.beans.ThesaurusTerm;
 import fr.mcc.ginco.exceptions.BusinessException;
 import fr.mcc.ginco.extjs.view.pojo.ThesaurusConceptReducedView;
 import fr.mcc.ginco.extjs.view.pojo.ThesaurusConceptView;
 import fr.mcc.ginco.extjs.view.pojo.ThesaurusTermView;
 import fr.mcc.ginco.log.Log;
 import fr.mcc.ginco.services.IAssociativeRelationshipRoleService;
 import fr.mcc.ginco.services.IThesaurusConceptService;
 import fr.mcc.ginco.services.IThesaurusService;
 import fr.mcc.ginco.utils.DateUtil;
 import org.apache.commons.collections.ListUtils;
 import org.slf4j.Logger;
 import org.springframework.stereotype.Component;
 
 import javax.inject.Inject;
 import javax.inject.Named;
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 
 /**
  * Small class responsible for converting real {@link ThesaurusConcept} object
  * into its view {@link ThesaurusConceptReducedView}.
  */
 @Component("thesaurusConceptViewConverter")
 public class ThesaurusConceptViewConverter {
 	@Log
 	private Logger logger;
 
 	@Inject
 	@Named("thesaurusService")
 	private IThesaurusService thesaurusService;
 
 	@Inject
 	@Named("thesaurusConceptService")
 	private IThesaurusConceptService thesaurusConceptService;
 	
 	@Inject
 	@Named("associativeRelationshipRoleService")
 	private IAssociativeRelationshipRoleService associativeRelationshipRoleService;
 	
 	@Inject
 	@Named("generatorService")
 	private IIDGeneratorService generatorService;
 
 	public List<ThesaurusConceptReducedView> convert(
 			List<ThesaurusConcept> conceptList) throws BusinessException {
 
 		List<ThesaurusConceptReducedView> result = new ArrayList<ThesaurusConceptReducedView>();
 
 		for (ThesaurusConcept concept : conceptList) {
 			ThesaurusConceptReducedView view = new ThesaurusConceptReducedView();
 			view.setIdentifier(concept.getIdentifier());
 			view.setLabel(thesaurusConceptService.getConceptLabel(concept
 					.getIdentifier()));
 			result.add(view);
 		}
 
         return result;
     }
 
     public ThesaurusConceptReducedView convert(ThesaurusConcept concept) throws BusinessException {
         ThesaurusConceptReducedView view = new ThesaurusConceptReducedView();
         view.setIdentifier(concept.getIdentifier());
         view.setLabel(thesaurusConceptService.getConceptLabel(concept
                 .getIdentifier()));
         return view;
     }
 
 	public ThesaurusConceptView convert(ThesaurusConcept concept,
 			List<ThesaurusTerm> thesaurusTerms) {
 		ThesaurusConceptView view = new ThesaurusConceptView();
 		view.setIdentifier(concept.getIdentifier());
 		view.setCreated(DateUtil.toString(concept.getCreated()));
 		view.setModified(DateUtil.toString(concept.getModified()));
 		view.setTopconcept(concept.getTopConcept());
 		view.setThesaurusId(concept.getThesaurus().getIdentifier());
		
		//TODO : set status
		view.setStatus(concept.getStatus());
		
 		view.setParentConcepts(getIdsFromConceptList(concept.getParentConcepts()));
         view.setRootConcepts(getIdsFromConceptList(concept.getRootConcepts()));
         List<ThesaurusTermView> terms = new ArrayList<ThesaurusTermView>();
 		for (ThesaurusTerm thesaurusTerm : thesaurusTerms) {
 			terms.add(new ThesaurusTermView(thesaurusTerm));
 		}
 		view.setTerms(terms);
 		
 		List<String> associatedConcepts = new ArrayList<String>();
 		for (ThesaurusConcept conceptAssociated : thesaurusConceptService.getAssociatedConcepts(concept.getIdentifier())) {
 			associatedConcepts.add(conceptAssociated.getIdentifier());
 			logger.info("Found associated concept : " + conceptAssociated.getIdentifier());
 		}	
 		view.setAssociatedConcepts(associatedConcepts);
 		return view;
 	}
 
     private List<String> getIdsFromConceptList(Set<ThesaurusConcept> list) {
         List<String> result = new ArrayList<String>();
         for(ThesaurusConcept concept : list) {
             result.add(concept.getIdentifier());
         }
         return result;
     }
 
 	/**
 	 * @param source
 	 *            source to work with
 	 * @return ThesaurusConcept
 	 * @throws BusinessException
 	 *             This method extracts a ThesaurusConcept from a
 	 *             ThesaurusConceptView given in argument
 	 */
 	public ThesaurusConcept convert(ThesaurusConceptView source)
 			throws BusinessException {
 		ThesaurusConcept thesaurusConcept;
 
 		// Test if ThesaurusConcept already exists. If yes we get it, if no we
 		// create a new one
 		if ("".equals(source.getIdentifier())) {
 			thesaurusConcept = new ThesaurusConcept();
 			thesaurusConcept.setIdentifier(generatorService.generate());
 			thesaurusConcept.setCreated(DateUtil.nowDate());
 			logger.info("Creating a new concept");
 		} else {
 			thesaurusConcept = thesaurusConceptService
 					.getThesaurusConceptById(source.getIdentifier());
 			logger.info("Getting an existing concept");
 		}
 
 		if ("".equals(source.getThesaurusId())) {
 			throw new BusinessException(
 					"ThesaurusId is mandatory to save a concept",
 					"mandatory-thesaurus");
 		} else {
 			Thesaurus thesaurus = new Thesaurus();
 			thesaurus = thesaurusService.getThesaurusById(source
 					.getThesaurusId());
 			thesaurusConcept.setThesaurus(thesaurus);
 		}
 		thesaurusConcept.setModified(DateUtil.nowDate());
         thesaurusConcept.setTopConcept(source.getTopconcept());
 
         List<String> oldParentIds = getIdsFromConceptList(thesaurusConcept.getParentConcepts());
 
         List<String> addedParents = ListUtils.subtract(source.getParentConcepts(), oldParentIds);
         List<String> removedParents = ListUtils.subtract(oldParentIds, source.getParentConcepts());
 
         if(!addedParents.isEmpty() || !removedParents.isEmpty()) {
             Set<ThesaurusConcept> addedParentsSet =
                       thesaurusConceptService.getThesaurusConceptList(addedParents);
 
             if(!removedParents.isEmpty()) {
                 thesaurusConceptService.removeParents(thesaurusConcept, removedParents);
             }
 
             if(!addedParents.isEmpty()) {
                 thesaurusConcept.getParentConcepts().addAll(addedParentsSet);
             }
 
             thesaurusConcept.setRootConcepts(
                     new HashSet<ThesaurusConcept>(
                             thesaurusConceptService.getRootConcepts(thesaurusConcept)));
             
             //launching an Async method to calculate new root concept for this concept children
             thesaurusConceptService.calculateChildrenRoot(thesaurusConcept.getIdentifier());
 
         }
         List<String> associatedConceptsIds = source.getAssociatedConcepts();
         thesaurusConcept = manageAssociativeRelationship(thesaurusConcept,associatedConceptsIds);
 
         if(!thesaurusConcept.getParentConcepts().isEmpty()) {
             thesaurusConcept.setTopConcept(false);
         }
 
 		return thesaurusConcept;
 	}
 	
 	private ThesaurusConcept manageAssociativeRelationship(ThesaurusConcept concept, List<String> associatedConceptsIds) throws BusinessException{
 		Set<AssociativeRelationship> relations = new HashSet<AssociativeRelationship>();
 		
 		if (concept.getAssociativeRelationshipLeft() == null) {
 			concept.setAssociativeRelationshipLeft(new HashSet<AssociativeRelationship>());
 		}
 		concept.getAssociativeRelationshipLeft().clear();
 
 		if (concept.getAssociativeRelationshipRight() == null) {
 			concept.setAssociativeRelationshipRight(new HashSet<AssociativeRelationship>());
 		}
 		concept.getAssociativeRelationshipRight().clear();
 
 		for (String associatedConceptsId: associatedConceptsIds) {
 			logger.debug("Settings associated concept " + associatedConceptsId);
 			AssociativeRelationship relationship = new AssociativeRelationship();
 			AssociativeRelationship.Id relationshipId= new AssociativeRelationship.Id();
 			relationshipId.setConcept1(concept.getIdentifier());
 			relationshipId.setConcept2(associatedConceptsId);
 			relationship.setIdentifier(relationshipId);
 			relationship.setConceptLeft(concept);
 			relationship.setConceptRight(thesaurusConceptService.getThesaurusConceptById(associatedConceptsId));
 			relationship.setRelationshipRole(associativeRelationshipRoleService.getDefaultAssociativeRelationshipRoleRole());
 			relations.add(relationship);
 		}
 		concept.getAssociativeRelationshipLeft().addAll(relations);
 		
 		return concept;
 	}
 
 }
