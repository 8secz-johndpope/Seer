 //$Id$
 /*
  * <p><b>License and Copyright: </b>The contents of this file is subject to the
  * same open source license as the Fedora Repository System at www.fedora-commons.org
  * Copyright &copy; 2006, 2007, 2008 by The Technical University of Denmark.
  * All rights reserved.</p>
  */
 package dk.defxws.fedoragsearch.server;
 
 import java.io.ByteArrayInputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 
 import javax.xml.transform.stream.StreamSource;
 
 import org.apache.log4j.Logger;
 import org.apache.lucene.demo.html.HTMLParser;
 import org.pdfbox.cos.COSDocument;
 import org.pdfbox.encryption.DocumentEncryption;
 import org.pdfbox.exceptions.CryptographyException;
 import org.pdfbox.exceptions.InvalidPasswordException;
 import org.pdfbox.pdfparser.PDFParser;
 import org.pdfbox.pdmodel.PDDocument;
 import org.pdfbox.util.PDFTextStripper;
 
 import dk.defxws.fedoragsearch.server.errors.GenericSearchException;
 
 /**
  * performs transformations from formatted documents to text
  * 
  * @author  gsp@dtv.dk
  * @version 
  */
 public class TransformerToText {
     
     private static final Logger logger =
         Logger.getLogger(TransformerToText.class);
     
     public static final String[] handledMimeTypes = {"text/plain", "text/xml",  "text/html", "application/pdf"};
     
     public TransformerToText() {
     }
     
     /**
      * 
      *
      * @throws TransformerConfigurationException, TransformerException.
      */
     public StringBuffer getText(byte[] doc, String mimetype) 
     throws GenericSearchException {
         if (mimetype.equals("text/plain")) {
             return getTextFromText(doc);
        } else if(mimetype.equals("text/xml") || mimetype.equals("application/xml")) {
             return getTextFromXML(doc);
         } else if(mimetype.equals("text/html")) {
             return getTextFromHTML(doc);
         } else if(mimetype.equals("application/pdf")) {
             return getTextFromPDF(doc);
         } else if(mimetype.equals("application/ps")) {
             return new StringBuffer();
         } else if(mimetype.equals("application/msword")) {
             return new StringBuffer();
         } else return new StringBuffer();
     }
     
     /**
      * 
      *
      * @throws GenericSearchException.
      */
     private StringBuffer getTextFromText(byte[] doc) 
     throws GenericSearchException {
         StringBuffer docText = new StringBuffer();
         InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(doc));
         try {
             int c = isr.read();
             while (c>-1) {
                 docText.append((char)c);
                 c=isr.read();
             }
         } catch (IOException e) {
             throw new GenericSearchException(e.toString());
         }
         return docText;
     }
 
 /**
  * 
  *
  * @throws GenericSearchException.
  */
 private StringBuffer getTextFromXML(byte[] doc) 
 throws GenericSearchException {
     InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(doc));
     StringBuffer docText = (new GTransformer()).transform(
     		"config/textFromXml", 
             new StreamSource(isr));
     docText.delete(0, docText.indexOf(">")+1);
     return docText;
 }
     
     /**
      * 
      *
      * @throws GenericSearchException.
      */
     private StringBuffer getTextFromHTML(byte[] doc) 
     throws GenericSearchException {
         StringBuffer docText = new StringBuffer();
         HTMLParser htmlParser = new HTMLParser(new ByteArrayInputStream(doc));
         try {
             InputStreamReader isr = (InputStreamReader) htmlParser.getReader();
             int c = isr.read();
             while (c>-1) {
                 docText.append((char)c);
                 c=isr.read();
             }
         } catch (IOException e) {
             throw new GenericSearchException(e.toString());
         }
         return docText;
     }
     
     /**
      * 
      *
      * @throws GenericSearchException.
      */
     private StringBuffer getTextFromPDF(byte[] doc) 
     throws GenericSearchException {
         StringBuffer docText = new StringBuffer();
         COSDocument cosDoc = null;
         PDDocument pdDoc = null;
         String password = "";
         try {
             cosDoc = parseDocument(new ByteArrayInputStream(doc));
         }
         catch (IOException e) {
             closeCOSDocument(cosDoc);
             throw new GenericSearchException(
                     "Cannot parse PDF document", e);
         }
         
         // decrypt the PDF document, if it is encrypted
         try {
             if (cosDoc.isEncrypted()) {
                 DocumentEncryption decryptor = new DocumentEncryption(cosDoc);
                 decryptor.decryptDocument(password);
             }
         }
         catch (CryptographyException e) {
             closeCOSDocument(cosDoc);
             throw new GenericSearchException(
                     "Cannot decrypt PDF document", e);
         }
         catch (InvalidPasswordException e) {
             closeCOSDocument(cosDoc);
             throw new GenericSearchException(
                     "Cannot decrypt PDF document", e);
         }
         catch (IOException e) {
             closeCOSDocument(cosDoc);
             throw new GenericSearchException(
                     "Cannot decrypt PDF document", e);
         }
         
         // extract PDF document's textual content
         try {
             PDFTextStripper stripper = new PDFTextStripper();
             pdDoc = new PDDocument(cosDoc);
             docText = new StringBuffer(stripper.getText(pdDoc));
         }
         catch (IOException e) {
             throw new GenericSearchException(
                     "Cannot parse PDF document", e);
         }
         finally {
             closeCOSDocument(cosDoc);
             closePDDocument(pdDoc);
         }
         return docText;
     }
     
     private static COSDocument parseDocument(InputStream is)
     throws IOException {
         PDFParser parser = new PDFParser(is);
         parser.parse();
         return parser.getDocument();
     }
     
     private void closeCOSDocument(COSDocument cosDoc) {
         if (cosDoc != null) {
             try {
                 cosDoc.close();
             }
             catch (IOException e) {
             }
         }
     }
     
     private void closePDDocument(PDDocument pdDoc) {
         if (pdDoc != null) {
             try {
             	pdDoc.close();
             }
             catch (IOException e) {
             }
         }
     }
 }
