 package uk.ac.ebi.gxa.loader.utils;
 
 import org.mged.magetab.error.ErrorItem;
 import org.mged.magetab.error.ErrorItemFactory;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
 import uk.ac.ebi.arrayexpress2.magetab.datamodel.SDRF;
 import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AssayNode;
 import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode;
 import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
 import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SourceNode;
 import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.CharacteristicsAttribute;
 import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.FactorValueAttribute;
 import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
 import uk.ac.ebi.microarray.atlas.model.Assay;
 import uk.ac.ebi.microarray.atlas.model.Property;
 import uk.ac.ebi.microarray.atlas.model.Sample;
 
 import java.util.Collection;
 import java.util.HashSet;
 import java.util.Set;
 
 /**
  * A class filled with handy convenience methods for performing writing tasks common to lots of SDRF graph nodes.  This
  * class contains methods that hepl with writing {@link Property} objects out given some nodes in the SDRF graph.
  *
  * @author Tony Burdett
  * @date 28-Aug-2009
  */
 public class SDRFWritingUtils {
     private static Logger log = LoggerFactory.getLogger(SDRFWritingUtils.class);
 
     /**
      * Write out the properties associated with a {@link Sample} in the SDRF graph.  These properties are obtained by
      * looking at the "characteristic" column in the SDRF graph, extracting the type and linking this type (the
      * property) to the name of the {@link SourceNode} provided (the property value).
      *
      * @param investigation the investigation being loaded
      * @param sample        the sample you want to attach properties to
      * @param sourceNode    the sourceNode being read
      * @throws ObjectConversionException if there is a problem creating the property object
      */
     public static void writeSampleProperties(
             MAGETABInvestigation investigation,
             Sample sample,
             SourceNode sourceNode)
             throws ObjectConversionException {
         // fetch characteristics of this sourceNode
         for (CharacteristicsAttribute characteristicsAttribute : sourceNode.characteristics) {
             // create Property for this attribute
             if (characteristicsAttribute.type.contains("||") || characteristicsAttribute.getNodeName().contains("||")) {
                 // generate error item and throw exception
                 String message = "Characteristics and their values must NOT contain '||' - " +
                         "this is a special reserved character used as a delimiter in the database";
 
                 ErrorItem error = ErrorItemFactory.getErrorItemFactory(SDRFWritingUtils.class.getClassLoader())
                         .generateErrorItem(message, 999, SDRFWritingUtils.class);
 
                 throw new ObjectConversionException(error, true);
             }
 
             // does this sample already contain this property/property value pair?
             boolean existing = false;
             if (sample.getProperties() != null) {
                 for (Property sp : sample.getProperties()) {
                     if (sp.getName().equals(characteristicsAttribute.type)) {
                         if (sp.getValue().equals(characteristicsAttribute.getNodeName())) {
                             existing = true;
                             break;
                         }
                         else {
                             // generate error item and throw exception
                             String message = "Inconsistent characteristic values for sample " + sample.getAccession() +
                                     ": property " + sp.getName() + " has values " + sp.getValue() + " and " +
                                     characteristicsAttribute.getNodeName() + " in different rows. Second value (" +
                                     characteristicsAttribute + ") will be ignored";
 
                             ErrorItem error =
                                     ErrorItemFactory.getErrorItemFactory(SDRFWritingUtils.class.getClassLoader())
                                             .generateErrorItem(message, 40, SDRFWritingUtils.class);
 
                             throw new ObjectConversionException(error, false);
 
                         }
                     }
                 }
             }
 
             if (!existing) {
                 Property p = new Property();
                 p.setAccession(AtlasLoaderUtils.getNodeAccession(
                         investigation, characteristicsAttribute));
                 p.setName(characteristicsAttribute.type);
                 p.setValue(characteristicsAttribute.getNodeName());
                 p.setFactorValue(false);
 
                 sample.addProperty(p);
 
                 // todo - characteristics can have ontology entries, and units (which can also have ontology entries) - set these values
             }
         }
     }
 
     /**
      * Write out the properties associated with an {@link Assay} in the SDRF graph.  These properties are obtained by
      * looking at the "factorvalue" column in the SDRF graph, extracting the type and linking this type (the property)
      * to the name of the {@link uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AssayNode} provided (the property
      * value).
      *
      * @param investigation the investigation being loaded
      * @param assay         the assay you want to attach properties to
      * @param assayNode     the assayNode being read
      * @throws ObjectConversionException if there is a problem creating the property object
      */
     public static void writeAssayProperties(
             MAGETABInvestigation investigation,
             Assay assay,
             AssayNode assayNode)
             throws ObjectConversionException {
         // fetch factor values of this assayNode
         for (FactorValueAttribute factorValueAttribute : assayNode.factorValues) {
             // create Property for this attribute
             if (factorValueAttribute.type.contains("||") || factorValueAttribute.getNodeName().contains("||")) {
                 // generate error item and throw exception
                 String message = "Factors and their values must NOT contain '||' - " +
                         "this is a special reserved character used as a delimiter in the database";
 
                 ErrorItem error = ErrorItemFactory.getErrorItemFactory(SDRFWritingUtils.class.getClassLoader())
                         .generateErrorItem(message, 999, SDRFWritingUtils.class);
 
                 throw new ObjectConversionException(error, true);
             }
 
             // does this assay already contain this property/property value pair?
             boolean existing = false;
             if (assay.getProperties() != null) {
                 for (Property ap : assay.getProperties()) {
                     if (ap.getName().equals(factorValueAttribute.type)) {
                         if (ap.getValue().equals(factorValueAttribute.getNodeName())) {
                             existing = true;
                             break;
                         }
                         else {
                             // generate error item and throw exception
                             String message = "Inconsistent characteristic values for assay " + assay.getAccession() +
                                     ": property " + ap.getName() + " has values " + ap.getValue() + " and " +
                                     factorValueAttribute.getNodeName() + " in different rows. Second value (" +
                                     factorValueAttribute + ") will be ignored";
 
                             ErrorItem error =
                                     ErrorItemFactory.getErrorItemFactory(SDRFWritingUtils.class.getClassLoader())
                                             .generateErrorItem(message, 40, SDRFWritingUtils.class);
 
                             throw new ObjectConversionException(error, false);
 
                         }
                     }
                 }
             }
 
             if (!existing) {
                 Property p = new Property();
                 p.setAccession(AtlasLoaderUtils.getNodeAccession(
                         investigation, factorValueAttribute));
                 p.setName(factorValueAttribute.type);
                 p.setValue(factorValueAttribute.getNodeName());
                p.setFactorValue(true);
 
                 assay.addProperty(p);
 
                 // todo - factor values can have ontology entries, set these values
             }
         }
     }
 
     /**
      * Write out the properties associated with an {@link Assay} in the SDRF graph.  These properties are obtained by
      * looking at the "factorvalue" column in the SDRF graph, extracting the type and linking this type (the property)
      * to the name of the {@link uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode} provided (the
      * property value).
      *
      * @param investigation     the investigation being loaded
      * @param assay             the assay you want to attach properties to
      * @param hybridizationNode the hybridizationNode being read
      * @throws ObjectConversionException if there is a problem creating the property object
      */
     public static void writeHybridizationProperties(MAGETABInvestigation investigation,
                                                     Assay assay,
                                                     HybridizationNode hybridizationNode)
             throws ObjectConversionException {
         // fetch factor values of this assayNode
         for (FactorValueAttribute factorValueAttribute : hybridizationNode.factorValues) {
             // create Property for this attribute
             if (factorValueAttribute.type.contains("||") || factorValueAttribute.getNodeName().contains("||")) {
                 // generate error item and throw exception
                 String message = "Factors and their values must NOT contain '||' - " +
                         "this is a special reserved character used as a delimiter in the database";
 
                 ErrorItem error = ErrorItemFactory.getErrorItemFactory(SDRFWritingUtils.class.getClassLoader())
                         .generateErrorItem(message, 999, SDRFWritingUtils.class);
 
                 throw new ObjectConversionException(error, true);
             }
 
             // does this assay already contain this property/property value pair?
             boolean existing = false;
             if (assay.getProperties() != null) {
                 for (Property ap : assay.getProperties()) {
                     if (ap.getName().equals(factorValueAttribute.type)) {
                         if (ap.getValue().equals(factorValueAttribute.getNodeName())) {
                             existing = true;
                             break;
                         }
                         else {
                             // generate error item and throw exception
                             String message = "Inconsistent characteristic values for assay " + assay.getAccession() +
                                     ": property " + ap.getName() + " has values " + ap.getValue() + " and " +
                                     factorValueAttribute.getNodeName() + " in different rows. Second value (" +
                                     factorValueAttribute + ") will be ignored";
 
                             ErrorItem error =
                                     ErrorItemFactory.getErrorItemFactory(SDRFWritingUtils.class.getClassLoader())
                                             .generateErrorItem(message, 40, SDRFWritingUtils.class);
 
                             throw new ObjectConversionException(error, false);
 
                         }
                     }
                 }
             }
 
             if (!existing) {
                 Property p = new Property();
                 p.setAccession(AtlasLoaderUtils.getNodeAccession(
                         investigation, factorValueAttribute));
                 p.setName(factorValueAttribute.type);
                 p.setValue(factorValueAttribute.getNodeName());
                p.setFactorValue(true);
 
                 assay.addProperty(p);
 
                 // todo - factor values can have ontology entries, set these values
             }
         }
     }
 }
