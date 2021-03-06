 package uk.ac.ebi.fgpt.sampletab.sra;
 
 import java.io.File;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.Writer;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Set;
 import java.util.TreeSet;
 
 import org.dom4j.Document;
 import org.dom4j.DocumentException;
 import org.dom4j.Element;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
 import uk.ac.ebi.arrayexpress2.magetab.validator.Validator;
 import uk.ac.ebi.arrayexpress2.sampletab.datamodel.SampleData;
 import uk.ac.ebi.arrayexpress2.sampletab.datamodel.msi.Database;
 import uk.ac.ebi.arrayexpress2.sampletab.datamodel.msi.Organization;
 import uk.ac.ebi.arrayexpress2.sampletab.datamodel.msi.Publication;
 import uk.ac.ebi.arrayexpress2.sampletab.datamodel.msi.TermSource;
 import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.SampleNode;
 import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.CharacteristicAttribute;
 import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.CommentAttribute;
 import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.DatabaseAttribute;
 import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.OrganismAttribute;
 import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.UnitAttribute;
 import uk.ac.ebi.arrayexpress2.sampletab.renderer.SampleTabWriter;
 import uk.ac.ebi.arrayexpress2.sampletab.validator.SampleTabValidator;
 import uk.ac.ebi.fgpt.sampletab.Normalizer;
 import uk.ac.ebi.fgpt.sampletab.utils.ENAUtils;
 import uk.ac.ebi.fgpt.sampletab.utils.SampleTabUtils;
 import uk.ac.ebi.fgpt.sampletab.utils.XMLUtils;
 
 public class ENASRAXMLToSampleTab {
 
     private static TermSource ncbitaxonomy = new TermSource("NCBI Taxonomy", "http://www.ncbi.nlm.nih.gov/taxonomy/", null);
     
     // logging
     private Logger log = LoggerFactory.getLogger(getClass());
     
     //characteristics that we want to ignore
     private static Collection<String> characteristicsIgnore;
     static {
         characteristicsIgnore = new TreeSet<String>();
         characteristicsIgnore.add("ENA-SPOT-COUNT");
         characteristicsIgnore.add("ENA-BASE-COUNT");
         characteristicsIgnore.add("ENA-SUBMISSION-TOOL");
         characteristicsIgnore = Collections.unmodifiableCollection(characteristicsIgnore);
     }
     
     public ENASRAXMLToSampleTab() {
         
     }
     
     private boolean isStudy(File infile) {
         String filename = infile.getName();
         String accession = filename.substring(0, filename.length()-4);
         return isStudy(accession);
     }
     
     private boolean isStudy(String accession) {
         if (accession.matches("[SED]RP[0-9]+")){
             return true;
         //new project identifiers are going to replace study identifiers
         } else if (accession.matches("PRJ(EB|NA|DA)[0-9]+")){
             return true;
         } else {
             return false;
         }
     }
     
     private boolean isSubmission(File infile) {
         String filename = infile.getName();
         String accession = filename.substring(0, filename.length()-4);
         return isSubmission(accession);
     }
     
     private boolean isSubmission(String accession) {
         if (accession.matches("[SED]RA[0-9]+")){
             return true;
         } else {
             return false;
         }
     }
 
     public SampleData convert(File infile) throws ParseException, IOException, DocumentException {
         if (isStudy(infile)) {
             Document doc = XMLUtils.getDocument(infile);
             Element root = doc.getRootElement();
             Collection<String> sampleAccessions = ENAUtils.getSamplesForStudy(root);
             return convert(infile, sampleAccessions);
         } else if (isSubmission(infile)) {
             Document doc = XMLUtils.getDocument(infile);
             Element root = doc.getRootElement();
             Collection<String> sampleAccessions = ENAUtils.getSamplesForSubmission(root);
             return convert(infile, sampleAccessions);
         }
         throw new IOException("infile is neither a study nor a submission");
     }
 
     public SampleData convert(File infile, Collection<String> sampleAccessions) throws ParseException, IOException, DocumentException {
         
         if (!infile.exists()) {
             throw new IOException(infile + " does not exist");
         }
 
         Document doc = XMLUtils.getDocument(infile);
         Element root = doc.getRootElement();
         
         SampleData st = new SampleData();
         String filename = infile.getName();
         String accession = filename.substring(0, filename.length()-4);
         st.msi.submissionIdentifier = "GEN-"+accession;
         Element mainElement = null;
         Collection<Element> links = null;
         
         if (isStudy(infile)) {
             mainElement = XMLUtils.getChildByName(root, "STUDY");
             Element descriptor = XMLUtils.getChildByName(mainElement, "DESCRIPTOR");
             if (descriptor != null) {
                 // title
                 if (XMLUtils.getChildByName(descriptor, "STUDY_TITLE") != null ) {
                     st.msi.submissionTitle = XMLUtils.getChildByName(descriptor, "STUDY_TITLE").getTextTrim();
                 }
                 // organization
                 Element centerName = XMLUtils.getChildByName(descriptor, "CENTER_NAME");
                 Element centerTitle = XMLUtils.getChildByName(descriptor, "CENTER_TITLE");
                 Element centerProjectName = XMLUtils.getChildByName(descriptor, "CENTER_PROJECT_NAME");
                 if (centerName != null) {
                     st.msi.organizations.add(new Organization(centerName.getTextTrim(), null, null, null, "Submitter"));
                 } else if (centerTitle != null) {
                     st.msi.organizations.add(new Organization(centerTitle.getTextTrim(), null, null, null, "Submitter"));
                 } else if (mainElement.attributeValue("center_name") != null) {
                     st.msi.organizations.add(new Organization(mainElement.attributeValue("center_name"), null, null, null, "Submitter"));
                 } else if (centerProjectName != null) {
                     st.msi.organizations.add(new Organization(centerProjectName.getTextTrim(), null, null, null, "Submitter"));
                 } else {
                     throw new ParseException("Unable to find organization name.");
                 }
             }
             
             // description
             String description = null;
             if (descriptor != null) {
                 Element studyAbstract = XMLUtils.getChildByName(descriptor, "STUDY_ABSTRACT");
                 Element studyDescription = XMLUtils.getChildByName(descriptor, "STUDY_DESCRIPTION");
                 if (studyAbstract != null) {
                     description = studyAbstract.getTextTrim();
                 } else if (studyDescription != null) {
                     description = studyDescription.getTextTrim();
                 } else {
                     log.warn("no STUDY_ABSTRACT or STUDY_DESCRIPTION");
                 }
 
             }
             if (description != null) {
                 st.msi.submissionDescription = description;
             }
             //study links
             links = XMLUtils.getChildrenByName(XMLUtils.getChildByName(mainElement, "STUDY_LINKS"), "STUDY_LINK");
             
         } else if (isSubmission(infile)) {
             mainElement = XMLUtils.getChildByName(root, "SUBMISSION");
             //study links
             links = XMLUtils.getChildrenByName(XMLUtils.getChildByName(mainElement, "SUBMISSION_LINKS"), "SUBMISSION_LINK");
             //organization
             if (mainElement.attributeValue("center_name") != null) {
                 st.msi.organizations.add(new Organization(mainElement.attributeValue("center_name"), null, null, null, "Submitter"));
             }
         } else {
             throw new IOException("infile is neither a study nor a submission");
         }
 
         // pubmed link
         Set<Integer> pmids = new TreeSet<Integer>();
         for (Element link : links) {
             Element xrefLink = XMLUtils.getChildByName(link, "XREF_LINK");
             Element db = XMLUtils.getChildByName(xrefLink, "DB");
             Element id = XMLUtils.getChildByName(xrefLink, "ID");
             if (db != null && db.getTextTrim().equals("PUBMED")) {
                 try {
                     pmids.add(new Integer(id.getTextTrim()));
                 } catch (NumberFormatException e){
                     log.warn("Unable to parsed PubMedID "+id.getTextTrim());
                 }
             }
         }
         for (Integer pmid : pmids) {
             st.msi.publications.add(new Publication(""+pmid, null));
         }
 
         // database link
         st.msi.databases.add(new Database("ENA SRA", 
                 "http://www.ebi.ac.uk/ena/data/view/" + accession, accession));
 
         
         log.info("MSI section complete, starting SCD section.");
 
         // start on the samples        
         File indir = infile.getParentFile();
         for (String sampleAccession : sampleAccessions) {
             File sampleFile = new File(indir, sampleAccession + ".xml");
 
             Document sampledoc;
             try {
                 sampledoc = XMLUtils.getDocument(sampleFile);
             } catch (DocumentException e) {
                 // rethrow as a ParseException
                 throw new ParseException("Unable to parse XML document of sample " + sampleAccession);
             }
             Element sampleroot = sampledoc.getRootElement();
             Element sampleElement = XMLUtils.getChildByName(sampleroot, "SAMPLE");
             Element sampleName = XMLUtils.getChildByName(sampleElement, "SAMPLE_NAME");
             Element sampledescription = XMLUtils.getChildByName(sampleElement, "DESCRIPTION");
             Element synonym = XMLUtils.getChildByName(sampleElement, "TITLE");
             
             //sometimes the study is public but the samples are private
             //check for that and skip sample
             if (sampleElement == null){
                 continue;
             }
 
             // check that this actually is the sample we want
             if (sampleElement.attributeValue("accession") != null
                     && !sampleElement.attributeValue("accession").equals(sampleAccession)) {
                 throw new ParseException("Accession in XML content does not match filename");
             }
 
             log.info("Processing sample " + sampleAccession);
 
             // create the actual sample node
             SampleNode samplenode = new SampleNode();
             samplenode.setNodeName(sampleAccession);
             
             samplenode.addAttribute(new DatabaseAttribute("ENA SRA",
                     sampleAccession, 
                     "http://www.ebi.ac.uk/ena/data/view/" + sampleAccession));
 
             // process any synonyms that may exist
             if (sampleName != null) {
                 Element taxon = XMLUtils.getChildByName(sampleName, "TAXON_ID");
                 Element indivname = XMLUtils.getChildByName(sampleName, "INDIVIDUAL_NAME");
                 Element scientificname = XMLUtils.getChildByName(sampleName, "SCIENTIFIC_NAME");
                 Element commonname = XMLUtils.getChildByName(sampleName, "COMMON_NAME");
                 Element annonname = XMLUtils.getChildByName(sampleName, "ANONYMIZED_NAME");
                 
                 // insert all synonyms at position zero so they display next to name
                 if (indivname != null) {
                     CommentAttribute synonymattrib = new CommentAttribute("Synonym", indivname.getTextTrim());
                     samplenode.addAttribute(synonymattrib, 0);
                 }
                 if (annonname != null) {
                     CommentAttribute synonymattrib = new CommentAttribute("Synonym", annonname.getTextTrim());
                     samplenode.addAttribute(synonymattrib, 0);
                 }
                 if (synonym != null) {
                     CommentAttribute synonymattrib = new CommentAttribute("Synonym", synonym.getTextTrim());
                     samplenode.addAttribute(synonymattrib, 0);
                 }
                 
                 //maybe these should be database ids rather than synonyms?
                 Element identifiers = XMLUtils.getChildByName(sampleElement, "IDENTIFIERS");
                 
                 //colect various IDs together
                 Collection<Element> otherIDs = XMLUtils.getChildrenByName(identifiers, "EXTERNAL_ID");
                 otherIDs.addAll(XMLUtils.getChildrenByName(identifiers, "SUBMITTER_ID"));
                 otherIDs.addAll(XMLUtils.getChildrenByName(identifiers, "SECONDARY_ID"));
                 
                 for (Element id : otherIDs) {
 
                     if (samplenode.getSampleAccession() == null && 
                             id.getTextTrim().matches("SAM[END][A]?[0-9]+")) {
                         //this is a biosamples accession, and we dont have one yet, so use it
                         samplenode.setSampleAccession(id.getTextTrim());
                    } else if (samplenode.getSampleAccession().equals(id.getTextTrim())) {
                         //same as the existing sample accession
                         //do nothing
                     } else if (samplenode.getSampleAccession() != null && 
                             id.getTextTrim().matches("SAM[END][A]?[0-9]+")){
                         //this is a biosamples accession, but we already have one, report an error and store as synonym
                         log.error("Strange biosample identifiers in "+sampleAccession);
                         CommentAttribute synonymattrib = new CommentAttribute("Synonym", id.getTextTrim());
                         samplenode.addAttribute(synonymattrib, 0);                        
                     } else {
                         //store it as a synonym
                         CommentAttribute synonymattrib = new CommentAttribute("Synonym", id.getTextTrim());
                         samplenode.addAttribute(synonymattrib, 0);
                     }
                 }
                 
                 //process any alias present
                 String alias = sampleElement.attributeValue("alias");
                 if (alias != null) {
                     alias = alias.trim();
                     if (samplenode.getSampleAccession() == null && 
                             alias.matches("SAM[END][A]?[0-9]+")) {
                         //this is a biosamples accession, and we dont have one yet, so use it
                         samplenode.setSampleAccession(alias);
                     } else if (samplenode.getSampleAccession() != null && 
                             alias.matches("SAM[END][A]?[0-9]+")){
                         //this is a biosamples accession, but we already have one, report an error and store as synonym
                         log.error("Strange biosample identifiers in "+sampleAccession);
                         CommentAttribute synonymattrib = new CommentAttribute("Synonym", alias);
                         samplenode.addAttribute(synonymattrib, 0);                        
                     } else {
                         //store it as a synonym
                         CommentAttribute synonymattrib = new CommentAttribute("Synonym", alias);
                         samplenode.addAttribute(synonymattrib, 0);
                     }
                 }
                 
                 
 
                 // now process organism
                 if (taxon != null) {
                     Integer taxid = new Integer(taxon.getTextTrim());
                     // could get taxon name by lookup, but this will be done by correction later on.
                     String taxName = null;
                     if (scientificname != null ) {
                         taxName = scientificname.getTextTrim();
                     } else {
                         taxName = taxid.toString();
                     }
                     
                     OrganismAttribute organismAttribute = null;
                     if (taxName != null && taxid != null) {
                         organismAttribute = new OrganismAttribute(taxName, st.msi.getOrAddTermSource(ncbitaxonomy), taxid);
                     } else if (taxName != null) {
                         organismAttribute = new OrganismAttribute(taxName);
                     }
 
                     if (organismAttribute != null) {
                         samplenode.addAttribute(organismAttribute);
                     }
                 }
             }
 
             if (sampleElement.attributeValue("alias") != null) {
                 CommentAttribute synonymattrib = new CommentAttribute("Synonym", sampleElement.attributeValue("alias"));
                 samplenode.addAttribute(synonymattrib, 0);
             }
             
             if (sampledescription != null) {
                 String descriptionstring = sampledescription.getTextTrim();
                 samplenode.setSampleDescription(descriptionstring);
             }
 
             // finally, any other attributes ENA SRA provides
             Element sampleAttributes = XMLUtils.getChildByName(sampleElement, "SAMPLE_ATTRIBUTES");
             if (sampleAttributes != null) {
                 for (Element sampleAttribute : XMLUtils.getChildrenByName(sampleAttributes, "SAMPLE_ATTRIBUTE")) {
                     Element tag = XMLUtils.getChildByName(sampleAttribute, "TAG");
                     Element value = XMLUtils.getChildByName(sampleAttribute, "VALUE");
                     Element units = XMLUtils.getChildByName(sampleAttribute, "UNITS");
                     if (tag != null) {
                         
                         String tagtext = tag.getTextTrim();
                         
                         if (characteristicsIgnore.contains(tagtext)) {
                             //skip this characteristic
                             log.debug("Skipping characteristic attribute "+tagtext);
                             continue;
                         }
                         
                         String valuetext;
                         if (value == null) {
                             // some ENA SRA attributes are boolean
                             valuetext = tagtext;
                         } else {
                             valuetext = value.getTextTrim();
                         }
                         CharacteristicAttribute characteristicAttribute = new CharacteristicAttribute(tagtext,
                                 valuetext);
                         
                         if (units != null && units.getTextTrim().length() > 0) {
                             log.info("Added unit "+units.getTextTrim());
                             characteristicAttribute.unit = new UnitAttribute();
                             characteristicAttribute.unit.setAttributeValue(units.getTextTrim());
                         }
 
                         samplenode.addAttribute(characteristicAttribute);
                     }
                 }
             }
 
             st.scd.addNode(samplenode);
         }
         
         //if we couldn't make a title earlier, do so now
         if (st.msi.submissionTitle == null || st.msi.submissionTitle.length() == 0) {
             st.msi.submissionTitle = SampleTabUtils.generateSubmissionTitle(st);
         }
 
         log.info("Finished convert()");
         return st;
     }
 
     public void convert(File file, Writer writer) throws IOException, ParseException, DocumentException {
         log.debug("recieved xml, preparing to convert");
         SampleData st = convert(file);
 
         log.info("SampleTab converted, preparing to write");
 
         Validator<SampleData> validator = new SampleTabValidator();
         validator.validate(st);
 
         Normalizer norm = new Normalizer();
         norm.normalize(st);
         
         SampleTabWriter sampletabwriter = new SampleTabWriter(writer);
         sampletabwriter.write(st);
         log.info("SampleTab written");
         sampletabwriter.close();
 
     }
 
     public void convert(File studyFile, String outfilename) throws IOException, ParseException, DocumentException {
 
         convert(studyFile, new File(outfilename));
     }
 
     public void convert(File studyFile, File sampletabFile) throws IOException, ParseException, DocumentException {
 
         // create parent directories, if they dont exist
         sampletabFile = sampletabFile.getAbsoluteFile();
         if (sampletabFile.isDirectory()) {
             sampletabFile = new File(sampletabFile, "sampletab.pre.txt");
         }
         if (!sampletabFile.getParentFile().exists()) {
             sampletabFile.getParentFile().mkdirs();
         }
 
         convert(studyFile, new FileWriter(sampletabFile));
     }
 
     public void convert(String studyFilename, Writer writer) throws IOException, ParseException, DocumentException {
         convert(new File(studyFilename), writer);
     }
 
     public void convert(String studyFilename, File sampletabFile) throws IOException, ParseException, DocumentException {
         convert(studyFilename, new FileWriter(sampletabFile));
     }
 
     public void convert(String studyFilename, String sampletabFilename) throws IOException, ParseException, DocumentException {
         convert(studyFilename, new File(sampletabFilename));
     }
 
     public static void main(String[] args) {
         new ENASRAXMLToSampleTab().doMain(args);
     }
 
     public void doMain(String[] args) {
         if (args.length < 2) {
             System.out.println("Must provide an ENA SRA study filename and a SampleTab output filename.");
             return;
         }
         String enasrafilename = args[0];
         String sampleTabFilename = args[1];
 
         ENASRAXMLToSampleTab converter = new ENASRAXMLToSampleTab();
 
         try {
             converter.convert(enasrafilename, sampleTabFilename);
         } catch (ParseException e) {
             log.error("ParseException converting " + enasrafilename + " to " + sampleTabFilename, e);
             System.exit(2);
             return;
         } catch (IOException e) {
             log.error("IOException converting " + enasrafilename + " to " + sampleTabFilename, e);
             System.exit(3);
             return;
         } catch (Exception e) {
             log.error("Error converting " + enasrafilename + " to " + sampleTabFilename, e);
             System.exit(4);
             return;
         }
     }
 }
