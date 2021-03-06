 /*
  * Copyright: (c) 2004-2010 Mayo Foundation for Medical Education and 
  * Research (MFMER). All rights reserved. MAYO, MAYO CLINIC, and the
  * triple-shield Mayo logo are trademarks and service marks of MFMER.
  *
  * Except as contained in the copyright notice above, or as used to identify 
  * MFMER as the author of this software, the trade names, trademarks, service
  * marks, or product names of the copyright holder shall not be used in
  * advertising, promotion or otherwise in connection with this software without
  * prior written authorization of the copyright holder.
  * 
  * Licensed under the Eclipse Public License, Version 1.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at 
  * 
  * 		http://www.eclipse.org/legal/epl-v10.html
  * 
  */
 package org.LexGrid.LexBIG.Impl;
 
 import org.LexGrid.LexBIG.DataModel.Collections.LocalNameList;
 import org.LexGrid.LexBIG.DataModel.Core.CodingSchemeVersionOrTag;
 import org.LexGrid.LexBIG.Exceptions.LBException;
 import org.LexGrid.LexBIG.Exceptions.LBInvocationException;
 import org.LexGrid.LexBIG.Exceptions.LBParameterException;
 import org.LexGrid.LexBIG.Exceptions.LBResourceUnavailableException;
 import org.LexGrid.LexBIG.Impl.codednodeset.SingleLuceneIndexCodedNodeSet;
 import org.LexGrid.LexBIG.Impl.codednodeset.UnionSingleLuceneIndexCodedNodeSet;
 import org.LexGrid.LexBIG.LexBIGService.CodedNodeSet;
 import org.LexGrid.LexBIG.Utility.Constructors;
 import org.LexGrid.LexBIG.Utility.ServiceUtility;
 import org.LexGrid.codingSchemes.CodingScheme;
 import org.LexGrid.relations.Relations;
 import org.apache.commons.lang.BooleanUtils;
 import org.apache.commons.lang.StringUtils;
 import org.lexevs.dao.database.utility.DaoUtility;
 import org.lexevs.locator.LexEvsServiceLocator;
 import org.lexevs.registry.model.RegistryEntry;
 import org.lexevs.registry.service.Registry;
 
 /**
  * A factory for creating CodedNodeGraph objects.
  */
 public class CodedNodeSetFactory {
 
     private static String VERSION_17 = "1.7";
     private static String VERSION_18 = "1.8";
     private static String VERSION_20 = "2.0";
 
     /**
      * Gets the coded node graph.
      * 
      * @param codingScheme the coding scheme
      * @param versionOrTag the version or tag
      * @param relationContainerName the relation container name
      * 
      * @return the coded node graph
      * 
      * @throws LBParameterException the LB parameter exception
      */
     public CodedNodeSet getCodedNodeSet(String codingScheme, CodingSchemeVersionOrTag versionOrTag, Boolean activeOnly, LocalNameList types)
     throws LBInvocationException, LBParameterException, LBResourceUnavailableException, LBException {
         
         String version = ServiceUtility.getVersion(codingScheme, versionOrTag);
         String uri = LexEvsServiceLocator.getInstance().getSystemResourceService().getUriForUserCodingSchemeName(codingScheme, version);
 
         Registry registry = LexEvsServiceLocator.getInstance().getRegistry();
 
         RegistryEntry entry = 
             registry.getCodingSchemeEntry(Constructors.createAbsoluteCodingSchemeVersionReference(uri, version));
 
         if(entry.getDbSchemaVersion().equals(VERSION_18) || entry.getDbSchemaVersion().equals(VERSION_17)){
 
             return new CodedNodeSetImpl(uri, versionOrTag, activeOnly, types);
 
         }
 
         if(entry.getDbSchemaVersion().equals(VERSION_20)){
 
             if(entry.getSupplementsUri() != null && entry.getSupplementsVersion() != null) {
                 String parentUri = entry.getSupplementsUri();
                 String parentVersion = entry.getSupplementsVersion();
                 
                 SingleLuceneIndexCodedNodeSet supplement = new SingleLuceneIndexCodedNodeSet(
                         parentUri, 
                         Constructors.createCodingSchemeVersionOrTagFromVersion(parentVersion), 
                         activeOnly, 
                         types);
                 
                 SingleLuceneIndexCodedNodeSet parent = new SingleLuceneIndexCodedNodeSet(uri, versionOrTag, activeOnly, types);
                 
                 return new UnionSingleLuceneIndexCodedNodeSet(parent, supplement);
             } else {
                 CodedNodeSet codingSchemeToReturn = new SingleLuceneIndexCodedNodeSet(uri, versionOrTag, activeOnly, types);
                 
                 CodingScheme csToFind = LexBIGServiceImpl.defaultInstance().resolveCodingScheme(codingScheme, versionOrTag);
                 
                 for(Relations relation : csToFind.getRelations()) {
      
                     CodedNodeSet toNodeListSet = null;
                     
                     if(BooleanUtils.toBoolean(relation.getIsMapping())) {
                         
                         if(toNodeListSet == null) {
                             toNodeListSet = LexBIGServiceImpl.defaultInstance().getNodeGraph(
                                 uri, 
                                 versionOrTag,
                                 null).toNodeList(null, true, false, 1, -1);
                         }
 
                         String sourceCodingSchemeName = relation.getSourceCodingScheme();
                         String sourceCodingSchemeVersion = relation.getSourceCodingSchemeVersion();
                         String targetCodingSchemeName = relation.getTargetCodingScheme();
                         String targetCodingSchemeVersion = relation.getTargetCodingSchemeVersion();
 
                         if(! DaoUtility.containsNulls(
                                 sourceCodingSchemeName,
                                 targetCodingSchemeName)) {
 
                             CodedNodeSet unionedCodedNode = unionMappingCodingScheme(
                                     toNodeListSet,
                                     sourceCodingSchemeName,
                                     sourceCodingSchemeVersion,
                                     targetCodingSchemeName,
                                     targetCodingSchemeVersion);
 
                      
                                 codingSchemeToReturn = unionedCodedNode;
                         }
                     }
                 }   
 
                 return codingSchemeToReturn;
             }
         }
 
         throw new LBParameterException("Could not create a CodedNodeSet for CodingScheme: " + codingScheme);
     }
     
     private CodedNodeSet unionMappingCodingScheme(
             CodedNodeSet toNodeListSet, 
             String sourceCodingSchemeName, 
             String sourceCodingSchemeVersion,
             String targetCodingSchemeName, 
             String targetCodingSchemeVersion) {
         CodingSchemeVersionOrTag sourceCsvt = 
             StringUtils.isBlank(sourceCodingSchemeVersion) ? null : Constructors.createCodingSchemeVersionOrTagFromVersion(sourceCodingSchemeVersion);
 
         CodingSchemeVersionOrTag targetCsvt = 
             StringUtils.isBlank(targetCodingSchemeVersion) ? null : Constructors.createCodingSchemeVersionOrTagFromVersion(targetCodingSchemeVersion);
 
         CodedNodeSet cnsToReturn = null;
 
         try {
             CodedNodeSet sourceCodedNodeSet = LexBIGServiceImpl.defaultInstance().getNodeSet(
                     sourceCodingSchemeName, 
                     sourceCsvt,
                     null);
             
             cnsToReturn = sourceCodedNodeSet.intersect(toNodeListSet);
         } catch (LBException e) {
             //not loaded or unavailable
         }
         
         try {
             CodedNodeSet targetCodedNodeSet = LexBIGServiceImpl.defaultInstance().getNodeSet(
                     targetCodingSchemeName, 
                     targetCsvt,
                     null);
             
             if(cnsToReturn != null) {
                cnsToReturn = cnsToReturn.union(targetCodedNodeSet.intersect(toNodeListSet));
             } else {
                 cnsToReturn = targetCodedNodeSet;
             }
         } catch (LBException e) {
             //not loaded or unavailable
             if(cnsToReturn != null) {
                 try {
                     return cnsToReturn.union(toNodeListSet);
                 } catch (Exception innerEx) {
                     throw new RuntimeException(innerEx);
                 } 
             } else {
                 return toNodeListSet;
             }
         }
         
         return cnsToReturn; 
     }
 }
