 /*******************************************************************************
  * Copyright (c) 2010 Christopher Haines, Dale Scheppler, Nicholas Skaggs, Stephen V. Williams. All rights reserved.
  * This program and the accompanying materials are made available under the terms of the new BSD license which
  * accompanies this distribution, and is available at http://www.opensource.org/licenses/bsd-license.html Contributors:
  * Christopher Haines, Dale Scheppler, Nicholas Skaggs, Stephen V. Williams - initial API and implementation
  ******************************************************************************/
 package org.vivoweb.test.harvester.translate;
 
 import java.io.BufferedWriter;
 import java.io.File;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.Writer;
 import junit.framework.TestCase;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.vivoweb.harvester.translate.XSLTranslator;
 import org.vivoweb.harvester.util.InitLog;
 import org.vivoweb.harvester.util.repo.RecordHandler;
 
 /**
  * @author Stephen Williams (swilliams@ctrip.ufl.edu)
  * @author Christopher Haines (hainesc@ctrip.ufl.edu)
  */
 public class TranslateTest extends TestCase {
 	/**
 	 * SLF4J Logger
 	 */
 	private static Logger log = LoggerFactory.getLogger(TranslateTest.class);
 	/** */
 	private static final String inputData = "<?xml version=\"1.0\"?>\n<!DOCTYPE PubmedArticleSet PUBLIC \"-//NLM//DTD PubMedArticle, 1st January 2010//EN\" \"http://www.ncbi.nlm.nih.gov/corehtml/query/DTD/pubmed_100101.dtd\">\n<PubmedArticleSet>\n<PubmedArticle><MedlineCitation Owner=\"NLM\" Status=\"MEDLINE\"><PMID>19885433</PMID><DateCreated><Year>2009</Year><Month>11</Month><Day>03</Day></DateCreated><DateCompleted><Year>2010</Year><Month>02</Month><Day>02</Day></DateCompleted><Article PubModel=\"Print\"><Journal><ISSN IssnType=\"Print\">0882-2786</ISSN><JournalIssue CitedMedium=\"Print\"><Volume>24 Suppl</Volume><PubDate><Year>2009</Year></PubDate></JournalIssue><Title>The International journal of oral &amp; maxillofacial implants</Title><ISOAbbreviation>Int J Oral Maxillofac Implants</ISOAbbreviation></Journal><ArticleTitle>Local risk factors for implant therapy.</ArticleTitle><Pagination><MedlinePgn>28-38</MedlinePgn></Pagination><Abstract><AbstractText>PURPOSE: The aim of this review was to determine the effect of several potential local risk factors on implant survival and success (primary outcomes) as well as on mucosal recession, bleeding on probing, and proximal marginal bone loss (secondary outcomes). MATERIALS AND METHODS: A comprehensive review of the literature was conducted. The selection of publications reporting on human clinical studies was based on predetermined inclusion criteria and was agreed upon by three reviewers. After title and abstract screening of 2,681 publications obtained from the search, 19 articles were deemed to be relevant to the topic and the search criteria. RESULTS: Limited data show that when an implant is placed within 3 mm of the neighboring tooth, proximal bone is at risk. The data regarding the placement of implants into infected sites are still insufficient, but studies have shown that this may be possible. Soft tissue thickness has not been shown to be a risk factor in implant survival. There is also no evidence to support a relationship between the width of keratinized tissue and implant survival. No studies were found that directly related bone density to implant survival. Implant stability was also difficult to examine due to the lack of validated stability measures. DISCUSSION AND CONCLUSION: One critical factor that faced the group during the review of the literature and interpretation of the data was the multifactorial nature of implant therapy. This makes isolation of specific risk factors difficult. Conclusions are limited by the current lack of quality clinical trials in this area.</AbstractText></Abstract><Affiliation>Department of Oral and Maxillofacial Surgery, University of Florida College of Dentistry, 1600 SW Archer Road, Room D7-6, Gainesville, FL 32610, USA. WMartin102@Dental1.ufl.edu</Affiliation><AuthorList CompleteYN=\"Y\"><Author ValidYN=\"Y\"><LastName>Martin</LastName><ForeName>William Joe</ForeName><Initials>WJ</Initials></Author><Author ValidYN=\"Y\"><LastName>Lewis</LastName><ForeName>Emma</ForeName><Initials>E</Initials></Author><Author ValidYN=\"Y\"><LastName>Nicol</LastName><ForeName>Ailsa</ForeName><Initials>A</Initials></Author></AuthorList><Language>eng</Language><PublicationTypeList><PublicationType>Journal Article</PublicationType><PublicationType>Review</PublicationType></PublicationTypeList></Article><MedlineJournalInfo><Country>United States</Country><MedlineTA>Int J Oral Maxillofac Implants</MedlineTA><NlmUniqueID>8611905</NlmUniqueID><ISSNLinking>0882-2786</ISSNLinking></MedlineJournalInfo><ChemicalList><Chemical><RegistryNumber>0</RegistryNumber><NameOfSubstance>Dental Implants</NameOfSubstance></Chemical></ChemicalList><CitationSubset>D</CitationSubset><CitationSubset>IM</CitationSubset><MeshHeadingList><MeshHeading><DescriptorName MajorTopicYN=\"N\">Bone Density</DescriptorName></MeshHeading><MeshHeading><DescriptorName MajorTopicYN=\"N\">Dental Arch</DescriptorName><QualifierName MajorTopicYN=\"N\">anatomy &amp; histology</QualifierName></MeshHeading><MeshHeading><DescriptorName MajorTopicYN=\"N\">Dental Implantation, Endosseous</DescriptorName><QualifierName MajorTopicYN=\"Y\">contraindications</QualifierName></MeshHeading><MeshHeading><DescriptorName MajorTopicYN=\"N\">Dental Implants</DescriptorName><QualifierName MajorTopicYN=\"Y\">contraindications</QualifierName></MeshHeading><MeshHeading><DescriptorName MajorTopicYN=\"N\">Dental Restoration Failure</DescriptorName></MeshHeading><MeshHeading><DescriptorName MajorTopicYN=\"N\">Gingiva</DescriptorName><QualifierName MajorTopicYN=\"N\">anatomy &amp; histology</QualifierName></MeshHeading><MeshHeading><DescriptorName MajorTopicYN=\"N\">Humans</DescriptorName></MeshHeading><MeshHeading><DescriptorName MajorTopicYN=\"N\">Infection</DescriptorName></MeshHeading><MeshHeading><DescriptorName MajorTopicYN=\"N\">Periodontitis</DescriptorName></MeshHeading><MeshHeading><DescriptorName MajorTopicYN=\"N\">Risk Factors</DescriptorName></MeshHeading></MeshHeadingList><NumberOfReferences>72</NumberOfReferences></MedlineCitation><PubmedData><History><PubMedPubDate PubStatus=\"entrez\"><Year>2009</Year><Month>11</Month><Day>4</Day><Hour>6</Hour><Minute>0</Minute></PubMedPubDate><PubMedPubDate PubStatus=\"pubmed\"><Year>2009</Year><Month>12</Month><Day>4</Day><Hour>6</Hour><Minute>0</Minute></PubMedPubDate><PubMedPubDate PubStatus=\"medline\"><Year>2010</Year><Month>2</Month><Day>3</Day><Hour>6</Hour><Minute>0</Minute></PubMedPubDate></History><PublicationStatus>ppublish</PublicationStatus><ArticleIdList><ArticleId IdType=\"doi\">10.3949/ccjm.77.s3.07</ArticleId><ArticleId IdType=\"pubmed\">19885433</ArticleId></ArticleIdList></PubmedData></PubmedArticle>\n</PubmedArticleSet>";
 	/** */
 	//private static final String outputData = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<rdf:RDF xmlns:bibo=\"http://purl.org/ontology/bibo/\"\n         xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n         xmlns:owlPlus=\"http://www.w3.org/2006/12/owl2-xml#\"\n         xmlns:xs=\"http://www.w3.org/2001/XMLSchema#\"\n         xmlns:skos=\"http://www.w3.org/2008/05/skos#\"\n         xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n         xmlns:vocab=\"http://purl.org/vocab/vann/\"\n         xmlns:swvocab=\"http://www.w3.org/2003/06/sw-vocab-status/ns#\"\n         xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n         xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n         xmlns:core=\"http://vivoweb.org/ontology/core#\"\n         xmlns:vitro=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#\"\n         xmlns:foaf=\"http://xmlns.com/foaf/0.1/\"\n         xmlns:score=\"http://vivoweb.org/ontology/score#\">\n   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433\">\n      <rdf:type rdf:resource=\"http://purl.org/ontology/bibo/AcademicArticle\"/>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <bibo:pmid>19885433</bibo:pmid>\n      <rdfs:label>Local risk factors for implant therapy.</rdfs:label>\n      <core:Title>Local risk factors for implant therapy.</core:Title>\n      <score:Affiliation>Department of Oral and Maxillofacial Surgery, University of Florida College of Dentistry, 1600 SW Archer Road, Room D7-6, Gainesville, FL 32610, USA. WMartin102@Dental1.ufl.edu</score:Affiliation>\n      <bibo:volume>24 Suppl</bibo:volume>\n      <bibo:number/>\n      <bibo:doi>10.3949/ccjm.77.s3.07</bibo:doi>\n      <score:workEmail>WMartin102@Dental1.ufl.edu</score:workEmail>\n      <core:informationResourceInAuthorship rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship1\"/>\n      <core:informationResourceInAuthorship rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship2\"/>\n      <core:informationResourceInAuthorship rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship3\"/>\n      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh1\"/>\n      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh2\"/>\n      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh3\"/>\n      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh4\"/>\n      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh5\"/>\n      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh6\"/>\n      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh7\"/>\n      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh8\"/>\n      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh9\"/>\n      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh10\"/>\n      <core:hasPublicationVenue rdf:resource=\"http://vivoweb.org/pubMed/journal/j0882-2786\"/>\n      <score:hasCreateDate rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/dateCreated\"/>\n      <score:hasCompleteDate rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/dateCompleted\"/>\n   </rdf:Description>\n   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship1\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#Authorship\"/>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#DependentResource\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#DependentResource\"/>\n      <core:linkedAuthor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/author1\"/>\n      <core:linkedInformationResource rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Authorship for Martin, William</rdfs:label>\n      <core:authorRank rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">1</core:authorRank>\n   </rdf:Description>\n   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship2\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#Authorship\"/>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#DependentResource\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#DependentResource\"/>\n      <core:linkedAuthor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/author2\"/>\n      <core:linkedInformationResource rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Authorship for Lewis, Emma</rdfs:label>\n      <core:authorRank rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">2</core:authorRank>\n   </rdf:Description>\n   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship3\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#Authorship\"/>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#DependentResource\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#DependentResource\"/>\n      <core:linkedAuthor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/author3\"/>\n      <core:linkedInformationResource rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Authorship for Nicol, Ailsa</rdfs:label>\n      <core:authorRank rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">3</core:authorRank>\n   </rdf:Description>\n   <rdf:Description rdf:nodeID=\"pmid19885433mesh1\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Bone Density</rdfs:label>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <score:Descriptor>Bone Density</score:Descriptor>\n      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n      <score:Qualifier/>\n      <score:QualifierIsMajorTerm/>\n   </rdf:Description>\n   <rdf:Description rdf:nodeID=\"pmid19885433mesh2\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Dental Arch</rdfs:label>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <score:Descriptor>Dental Arch</score:Descriptor>\n      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n      <score:Qualifier>anatomy &amp; histology</score:Qualifier>\n      <score:QualifierIsMajorTerm>N</score:QualifierIsMajorTerm>\n   </rdf:Description>\n   <rdf:Description rdf:nodeID=\"pmid19885433mesh3\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Dental Implantation, Endosseous</rdfs:label>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <score:Descriptor>Dental Implantation, Endosseous</score:Descriptor>\n      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n      <score:Qualifier>contraindications</score:Qualifier>\n      <score:QualifierIsMajorTerm>Y</score:QualifierIsMajorTerm>\n   </rdf:Description>\n   <rdf:Description rdf:nodeID=\"pmid19885433mesh4\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Dental Implants</rdfs:label>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <score:Descriptor>Dental Implants</score:Descriptor>\n      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n      <score:Qualifier>contraindications</score:Qualifier>\n      <score:QualifierIsMajorTerm>Y</score:QualifierIsMajorTerm>\n   </rdf:Description>\n   <rdf:Description rdf:nodeID=\"pmid19885433mesh5\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Dental Restoration Failure</rdfs:label>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <score:Descriptor>Dental Restoration Failure</score:Descriptor>\n      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n      <score:Qualifier/>\n      <score:QualifierIsMajorTerm/>\n   </rdf:Description>\n   <rdf:Description rdf:nodeID=\"pmid19885433mesh6\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Gingiva</rdfs:label>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <score:Descriptor>Gingiva</score:Descriptor>\n      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n      <score:Qualifier>anatomy &amp; histology</score:Qualifier>\n      <score:QualifierIsMajorTerm>N</score:QualifierIsMajorTerm>\n   </rdf:Description>\n   <rdf:Description rdf:nodeID=\"pmid19885433mesh7\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Humans</rdfs:label>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <score:Descriptor>Humans</score:Descriptor>\n      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n      <score:Qualifier/>\n      <score:QualifierIsMajorTerm/>\n   </rdf:Description>\n   <rdf:Description rdf:nodeID=\"pmid19885433mesh8\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Infection</rdfs:label>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <score:Descriptor>Infection</score:Descriptor>\n      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n      <score:Qualifier/>\n      <score:QualifierIsMajorTerm/>\n   </rdf:Description>\n   <rdf:Description rdf:nodeID=\"pmid19885433mesh9\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Periodontitis</rdfs:label>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <score:Descriptor>Periodontitis</score:Descriptor>\n      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n      <score:Qualifier/>\n      <score:QualifierIsMajorTerm/>\n   </rdf:Description>\n   <rdf:Description rdf:nodeID=\"pmid19885433mesh10\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Risk Factors</rdfs:label>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <score:Descriptor>Risk Factors</score:Descriptor>\n      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n      <score:Qualifier/>\n      <score:QualifierIsMajorTerm/>\n   </rdf:Description>\n   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/journal/j0882-2786\">\n      <rdf:type rdf:resource=\"http://purl.org/ontology/bibo/Journal\"/>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <core:Title>The International journal of oral &amp; maxillofacial implants</core:Title>\n      <rdfs:label>The International journal of oral &amp; maxillofacial implants</rdfs:label>\n      <bibo:ISSN>0882-2786</bibo:ISSN>\n      <core:publicationVenueFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n   </rdf:Description>\n   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/dateCreated\">\n      <core:Year>\"2009\"</core:Year>\n      <core:Month>\"11\"</core:Month>\n      <core:Day>\"03\"</core:Day>\n   </rdf:Description>\n   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/dateCompleted\">\n      <core:Year>2010</core:Year>\n      <core:Month>02</core:Month>\n      <core:Day>02</core:Day>\n   </rdf:Description>\n</rdf:RDF>";
 	@SuppressWarnings("unused")
 	private static final String outputData = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
 		 + "<rdf:RDF xmlns:bibo=\"http://purl.org/ontology/bibo/\"\n"
 		 + "         xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n"
 		 + "         xmlns:owlPlus=\"http://www.w3.org/2006/12/owl2-xml#\"\n"
 		 + "         xmlns:xs=\"http://www.w3.org/2001/XMLSchema#\"\n"
 		 + "         xmlns:skos=\"http://www.w3.org/2008/05/skos#\"\n"
 		 + "         xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n"
 		 + "         xmlns:vocab=\"http://purl.org/vocab/vann/\"\n"
 		 + "         xmlns:swvocab=\"http://www.w3.org/2003/06/sw-vocab-status/ns#\"\n"
 		 + "         xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
 		 + "         xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
 		 + "         xmlns:core=\"http://vivoweb.org/ontology/core#\"\n"
 		 + "         xmlns:vitro=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#\"\n"
 		 + "         xmlns:foaf=\"http://xmlns.com/foaf/0.1/\"\n"
 		 + "         xmlns:score=\"http://vivoweb.org/ontology/score#\">\n"
 		 + "   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433\">\n"
 		 + "      <rdf:type rdf:resource=\"http://purl.org/ontology/bibo/AcademicArticle\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
 		 + "      <bibo:pmid>19885433</bibo:pmid>\n"
 		 + "      <rdfs:label>Local risk factors for implant therapy.</rdfs:label>\n"
 		 + "      <core:Title>Local risk factors for implant therapy.</core:Title>\n"
 		 + "      <score:Affiliation>Department of Oral and Maxillofacial Surgery, University of Florida College of Dentistry, 1600 SW Archer Road, Room D7-6, Gainesville, FL 32610, USA. WMartin102@Dental1.ufl.edu</score:Affiliation>\n"
 		 + "      <bibo:volume>24 Suppl</bibo:volume>\n"
 		 + "      <bibo:number/>\n"
 		 + "      <bibo:doi>10.3949/ccjm.77.s3.07</bibo:doi>\n"
 		 + "      <score:workEmail>WMartin102@Dental1.ufl.edu</score:workEmail>\n"
 		 + "      <core:informationResourceInAuthorship rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship1\"/>\n"
 		 + "      <core:informationResourceInAuthorship rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship2\"/>\n"
 		 + "      <core:informationResourceInAuthorship rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship3\"/>\n"
 		 + "      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh1\"/>\n"
 		 + "      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh2\"/>\n"
 		 + "      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh3\"/>\n"
 		 + "      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh4\"/>\n"
 		 + "      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh5\"/>\n"
 		 + "      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh6\"/>\n"
 		 + "      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh7\"/>\n"
 		 + "      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh8\"/>\n"
 		 + "      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh9\"/>\n"
 		 + "      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh10\"/>\n"
 		 + "      <core:hasPublicationVenue rdf:resource=\"http://vivoweb.org/pubMed/journal/j0882-2786\"/>\n"
 		 + "      <score:hasCreateDate rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/dateCreated\"/>\n"
 		 + "      <score:hasCompleteDate rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/dateCompleted\"/>\n"
 		 + "   </rdf:Description>\n"
 		 + "   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship1\">\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#Authorship\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#DependentResource\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#DependentResource\"/>\n"
 		 + "      <core:linkedAuthor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/author1\"/>\n"
 		 + "      <core:linkedInformationResource rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "      <rdfs:label>Authorship for Martin, William</rdfs:label>\n"
 		 + "      <core:authorRank rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">1</core:authorRank>\n"
 		 + "   </rdf:Description>\n"
 		 + "   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/author1\">\n"
 		 + "      <rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Person\"/>\n"
 		 + "      <rdfs:label>Martin, William</rdfs:label>\n"
 		 + "      <foaf:lastName>Martin</foaf:lastName>\n"
 		 + "      <score:foreName>William</score:foreName>\n"
 		 + "      <score:initials>W</score:initials>\n"
 		 + "      <score:suffix/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/harvester/excludeEntity\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
 		 + "      <core:authorInAuthorship rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship1\"/>\n"
 		 + "   </rdf:Description>\n"
 		 + "   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship2\">\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#Authorship\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#DependentResource\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#DependentResource\"/>\n"
 		 + "      <core:linkedAuthor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/author2\"/>\n"
 		 + "      <core:linkedInformationResource rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "      <rdfs:label>Authorship for Lewis, Emma</rdfs:label>\n"
 		 + "      <core:authorRank rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">2</core:authorRank>\n"
 		 + "   </rdf:Description>\n"
 		 + "   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/author2\">\n"
 		 + "      <rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Person\"/>\n"
 		 + "      <rdfs:label>Lewis, Emma</rdfs:label>\n"
 		 + "      <foaf:lastName>Lewis</foaf:lastName>\n"
 		 + "      <score:foreName>Emma</score:foreName>\n"
 		 + "      <score:initials>E</score:initials>\n"
 		 + "      <score:suffix/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/harvester/excludeEntity\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
 		 + "      <core:authorInAuthorship rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship2\"/>\n"
 		 + "   </rdf:Description>\n"
 		 + "   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship3\">\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#Authorship\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#DependentResource\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#DependentResource\"/>\n"
 		 + "      <core:linkedAuthor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/author3\"/>\n"
 		 + "      <core:linkedInformationResource rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "      <rdfs:label>Authorship for Nicol, Ailsa</rdfs:label>\n"
 		 + "      <core:authorRank rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">3</core:authorRank>\n"
 		 + "   </rdf:Description>\n"
 		 + "   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/author3\">\n"
 		 + "      <rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Person\"/>\n"
 		 + "      <rdfs:label>Nicol, Ailsa</rdfs:label>\n"
 		 + "      <foaf:lastName>Nicol</foaf:lastName>\n"
 		 + "      <score:foreName>Ailsa</score:foreName>\n"
 		 + "      <score:initials>A</score:initials>\n"
 		 + "      <score:suffix/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/harvester/excludeEntity\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
 		 + "      <core:authorInAuthorship rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship3\"/>\n"
 		 + "   </rdf:Description>\n"
 		 + "   <rdf:Description rdf:nodeID=\"pmid19885433mesh1\">\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n"
 		 + "      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "      <rdfs:label>Bone Density</rdfs:label>\n"
 		 + "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
 		 + "      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "      <score:Descriptor>Bone Density</score:Descriptor>\n"
 		 + "      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n"
 		 + "      <score:Qualifier/>\n"
 		 + "      <score:QualifierIsMajorTerm/>\n"
 		 + "   </rdf:Description>\n"
 		 + "   <rdf:Description rdf:nodeID=\"pmid19885433mesh2\">\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n"
 		 + "      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "      <rdfs:label>Dental Arch</rdfs:label>\n"
 		 + "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
 		 + "      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "      <score:Descriptor>Dental Arch</score:Descriptor>\n"
 		 + "      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n"
 		 + "      <score:Qualifier>anatomy &amp; histology</score:Qualifier>\n"
 		 + "      <score:QualifierIsMajorTerm>N</score:QualifierIsMajorTerm>\n"
 		 + "   </rdf:Description>\n"
 		 + "   <rdf:Description rdf:nodeID=\"pmid19885433mesh3\">\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n"
 		 + "      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "      <rdfs:label>Dental Implantation, Endosseous</rdfs:label>\n"
 		 + "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
 		 + "      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "      <score:Descriptor>Dental Implantation, Endosseous</score:Descriptor>\n"
 		 + "      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n"
 		 + "      <score:Qualifier>contraindications</score:Qualifier>\n"
 		 + "      <score:QualifierIsMajorTerm>Y</score:QualifierIsMajorTerm>\n"
 		 + "   </rdf:Description>\n"
 		 + "   <rdf:Description rdf:nodeID=\"pmid19885433mesh4\">\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n"
 		 + "      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "      <rdfs:label>Dental Implants</rdfs:label>\n"
 		 + "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
 		 + "      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "      <score:Descriptor>Dental Implants</score:Descriptor>\n"
 		 + "      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n"
 		 + "      <score:Qualifier>contraindications</score:Qualifier>\n"
 		 + "      <score:QualifierIsMajorTerm>Y</score:QualifierIsMajorTerm>\n"
 		 + "   </rdf:Description>\n"
 		 + "   <rdf:Description rdf:nodeID=\"pmid19885433mesh5\">\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n"
 		 + "      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "      <rdfs:label>Dental Restoration Failure</rdfs:label>\n"
 		 + "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
 		 + "      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "      <score:Descriptor>Dental Restoration Failure</score:Descriptor>\n"
 		 + "      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n"
 		 + "      <score:Qualifier/>\n"
 		 + "      <score:QualifierIsMajorTerm/>\n"
 		 + "   </rdf:Description>\n"
 		 + "   <rdf:Description rdf:nodeID=\"pmid19885433mesh6\">\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n"
 		 + "      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "      <rdfs:label>Gingiva</rdfs:label>\n"
 		 + "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
 		 + "      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "      <score:Descriptor>Gingiva</score:Descriptor>\n"
 		 + "      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n"
 		 + "      <score:Qualifier>anatomy &amp; histology</score:Qualifier>\n"
 		 + "      <score:QualifierIsMajorTerm>N</score:QualifierIsMajorTerm>\n"
 		 + "   </rdf:Description>\n"
 		 + "   <rdf:Description rdf:nodeID=\"pmid19885433mesh7\">\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n"
 		 + "      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "      <rdfs:label>Humans</rdfs:label>\n"
 		 + "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
 		 + "      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "      <score:Descriptor>Humans</score:Descriptor>\n"
 		 + "      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n"
 		 + "      <score:Qualifier/>\n"
 		 + "      <score:QualifierIsMajorTerm/>\n"
 		 + "   </rdf:Description>\n"
 		 + "   <rdf:Description rdf:nodeID=\"pmid19885433mesh8\">\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n"
 		 + "      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "      <rdfs:label>Infection</rdfs:label>\n"
 		 + "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
 		 + "      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "      <score:Descriptor>Infection</score:Descriptor>\n"
 		 + "      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n"
 		 + "      <score:Qualifier/>\n"
 		 + "      <score:QualifierIsMajorTerm/>\n"
 		 + "   </rdf:Description>\n"
 		 + "   <rdf:Description rdf:nodeID=\"pmid19885433mesh9\">\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n"
 		 + "      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "      <rdfs:label>Periodontitis</rdfs:label>\n"
 		 + "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
 		 + "      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "      <score:Descriptor>Periodontitis</score:Descriptor>\n"
 		 + "      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n"
 		 + "      <score:Qualifier/>\n"
 		 + "      <score:QualifierIsMajorTerm/>\n"
 		 + "   </rdf:Description>\n"
 		 + "   <rdf:Description rdf:nodeID=\"pmid19885433mesh10\">\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n"
 		 + "      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "      <rdfs:label>Risk Factors</rdfs:label>\n"
 		 + "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
 		 + "      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "      <score:Descriptor>Risk Factors</score:Descriptor>\n"
 		 + "      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n"
 		 + "      <score:Qualifier/>\n"
 		 + "      <score:QualifierIsMajorTerm/>\n"
 		 + "   </rdf:Description>\n"
 		 + "   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/journal/j0882-2786\">\n"
 		 + "      <rdf:type rdf:resource=\"http://purl.org/ontology/bibo/Journal\"/>\n"
 		 + "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
 		 + "      <core:Title>The International journal of oral &amp; maxillofacial implants</core:Title>\n"
 		 + "      <rdfs:label>The International journal of oral &amp; maxillofacial implants</rdfs:label>\n"
 		 + "      <bibo:ISSN>0882-2786</bibo:ISSN>\n"
 		 + "      <core:publicationVenueFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
 		 + "   </rdf:Description>\n"
 		 + "   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/dateCreated\">\n"
 		 + "      <core:Year>\"2009\"</core:Year>\n"
 		 + "      <core:Month>\"11\"</core:Month>\n"
 		 + "      <core:Day>\"03\"</core:Day>\n"
 		 + "   </rdf:Description>\n"
 		 + "   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/dateCompleted\">\n"
 		 + "      <core:Year>2010</core:Year>\n"
 		 + "      <core:Month>02</core:Month>\n"
 		 + "      <core:Day>02</core:Day>\n"
 		 + "   </rdf:Description>\n"
 		 + "</rdf:RDF>";
    
    /** */
	private static final String mapFilePath = "config/datamaps/pubmed-to-vivo.xsl";
 	/** */
 	private File inFile;
 	/** */
 	private File outFile;
 	/** */
 	private RecordHandler inRH;
 	/** */
 	private RecordHandler outRH;
 	
 	@Override
 	protected void setUp() throws Exception {
 		InitLog.initLogger(TranslateTest.class);
 		// create config file for input record handler
 		this.inFile = File.createTempFile("inputRHConfig", "xml");
 		Writer output = new BufferedWriter(new FileWriter(this.inFile));
 		output.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<RecordHandler type=\"org.vivoweb.harvester.util.repo.TextFileRecordHandler\">\n  <Param name=\"fileDir\">tmp://TranslateTestXML</Param>\n</RecordHandler>");
 		output.close();
 		// get the input record handler
 		this.inRH = RecordHandler.parseConfig(this.inFile.getAbsolutePath());
 		
 		// create config file for output record handler
 		this.outFile = File.createTempFile("outRHConfig", "xml");
 		output = new BufferedWriter(new FileWriter(this.outFile));
 		output.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<RecordHandler type=\"org.vivoweb.harvester.util.repo.TextFileRecordHandler\">\n  <Param name=\"fileDir\">tmp://TranslateTestRDF</Param>\n</RecordHandler>");
 		output.close();
 		// get the output record handler
 		this.outRH = RecordHandler.parseConfig(this.outFile.getAbsolutePath());
 	}
 	
 	/**
 	 * Test method for {@link org.vivoweb.harvester.translate.XSLTranslator#main(java.lang.String[]) main(String... args)}.
 	 * @throws IOException error
 	 */
 	public void testPubMedXSLT() throws IOException {
 		log.info("BEGIN testPubMedXSLT");
 		// add a record to the record handler
 		this.inRH.addRecord("1", inputData, this.getClass());
 		
 		// call the xlsTranslate
 		new XSLTranslator(new String[]{"-x", mapFilePath, "-i", this.inFile.getAbsolutePath(), "-o", this.outFile.getAbsolutePath()}).execute();
 		
 		// verify that output record handler has some records
 		assertTrue(this.outRH.iterator().hasNext());
 		
 		// compare the record data with expected output
 		log.debug("Translation Output:\n"+this.outRH.getRecordData("1"));
 		
 //			assertEquals(outputData, this.outRH.getRecordData("1"));
 		log.info("END testPubMedXSLT");
 	}
 	
 	@Override
 	public void tearDown() {
 		this.inFile = null;
 		this.outFile = null;
 		try {
 			this.inRH.close();
 		} catch(IOException e) {
 			log.debug(e.getMessage(), e);
 		}
 		this.inRH = null;
 		try {
 			this.outRH.close();
 		} catch(IOException e) {
 			log.debug(e.getMessage(), e);
 		}
 		this.outRH = null;
 	}
 }
