 package serializers.xml;
 
 import java.io.*;
 
 import javax.xml.namespace.NamespaceContext;
 import javax.xml.stream.XMLStreamException;
 import javax.xml.stream.XMLStreamReader;
 import javax.xml.stream.XMLStreamWriter;
 
 import com.siemens.ct.exi.EXIFactory;
 import com.siemens.ct.exi.api.stream.StAXDecoder;
 import com.siemens.ct.exi.api.stream.StAXEncoder;
 import com.siemens.ct.exi.helpers.DefaultEXIFactory;
 
 import serializers.JavaBuiltIn;
 import serializers.TestGroups;
 
 public class ExiExificient
 {
    public static void register(TestGroups groups, boolean woodstox, boolean aalto, boolean fastinfoset)
     {
         groups.media.add(JavaBuiltIn.mediaTransformer, new ExificientSerializer());
     }
 
     public static final class ExificientSerializer extends BaseStaxMediaSerializer
     {
         private final static EXIFactory _exiFactory = DefaultEXIFactory.newInstance();
         
         public String getName() { return "xml/exi-manual"; }
 
         protected XMLStreamReader createReader(InputStream in) throws XMLStreamException {
             try {
                 StAXDecoder dec = new StAXDecoder(_exiFactory);
                 dec.setInputStream(in);
                 return dec;
             } catch (Exception e) {
                 throw new XMLStreamException(e);
             }
         }
 
         protected XMLStreamWriter createWriter(OutputStream out) throws XMLStreamException {
             try {
                 StAXEncoder enc = new StAXEncoder(_exiFactory);
                 enc.setOutputStream(out);
                 // Hmmmmmh. And why does it not implement XMLStreamWriter?!
                 return new WriterWrapper(enc);
             } catch (Exception e) {
                 throw new XMLStreamException(e);
             }
         }
     }
 
     final static class WriterWrapper implements XMLStreamWriter
     {
         private final StAXEncoder _encoder;
         
         public WriterWrapper(StAXEncoder enc) { _encoder = enc; }
 
         @Override
         public void close() throws XMLStreamException { }
 
         @Override
         public void flush() throws XMLStreamException { }
 
         @Override
         public NamespaceContext getNamespaceContext() {
             return null;
         }
 
         @Override
         public String getPrefix(String arg0) throws XMLStreamException {
             return null;
         }
 
         @Override
         public Object getProperty(String arg0) throws IllegalArgumentException {
             return null;
         }
 
         @Override
         public void setDefaultNamespace(String arg0) throws XMLStreamException {
         }
 
         @Override
         public void setNamespaceContext(NamespaceContext arg0)
                 throws XMLStreamException
         {
         }
 
         @Override
         public void setPrefix(String arg0, String arg1) throws XMLStreamException { }
 
         @Override
         public void writeAttribute(String localName, String value) throws XMLStreamException {
            _encoder.writeAttribute(null, null, localName, value);
         }
 
         @Override
         public void writeAttribute(String nsURI, String localName, String value)
                 throws XMLStreamException {
            _encoder.writeAttribute(null, nsURI, localName, value);
         }
 
         @Override
         public void writeAttribute(String prefix, String nsURI, String localName, String value)
                         throws XMLStreamException {
             _encoder.writeAttribute(prefix, nsURI, localName, value);
         }
 
         @Override
         public void writeCData(String data) throws XMLStreamException {
             _encoder.writeCData(data);
         }
 
         @Override
         public void writeCharacters(String text) throws XMLStreamException {
             _encoder.writeCharacters(text);
         }
 
         @Override
         public void writeCharacters(char[] text, int start, int len)
                 throws XMLStreamException {
             _encoder.writeCharacters(text, start, len);
         }
 
         @Override
         public void writeComment(String text) throws XMLStreamException {
             _encoder.writeComment(text);
         }
 
         @Override
         public void writeDTD(String stuff) throws XMLStreamException {
             _encoder.writeDTD(stuff);
         }
 
         @Override
         public void writeDefaultNamespace(String arg0)
                 throws XMLStreamException {
             // NOP
         }
 
         @Override
         public void writeEmptyElement(String localName) throws XMLStreamException {
            _encoder.writeStartElement(null, null, localName);
             _encoder.writeEndElement();
         }
 
         @Override
         public void writeEmptyElement(String nsURI, String localName)
                 throws XMLStreamException {
            _encoder.writeStartElement(null, localName, nsURI);
             _encoder.writeEndElement();
         }
 
         @Override
         public void writeEmptyElement(String prefix, String localName, String nsURI)
                 throws XMLStreamException {
             _encoder.writeStartElement(prefix, localName, nsURI);
             _encoder.writeEndElement();
         }
 
         @Override
         public void writeEndDocument() throws XMLStreamException {
             _encoder.writeEndDocument();
         }
 
         @Override
         public void writeEndElement() throws XMLStreamException {
             _encoder.writeEndElement();
         }
 
         @Override
         public void writeEntityRef(String name) throws XMLStreamException {
             _encoder.writeEntityRef(name);
         }
 
         @Override
         public void writeNamespace(String arg0, String arg1)
                 throws XMLStreamException {
             // NOP
         }
 
         @Override
         public void writeProcessingInstruction(String target) throws XMLStreamException {
             _encoder.writeProcessingInstruction(target);
         }
 
         @Override
         public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
             _encoder.writeProcessingInstruction(target, data);
         }
 
         @Override
         public void writeStartDocument() throws XMLStreamException {
             _encoder.writeStartDocument();
         }
 
         @Override
         public void writeStartDocument(String arg0) throws XMLStreamException {
             _encoder.writeStartDocument();
         }
 
         @Override
         public void writeStartDocument(String arg0, String arg1) throws XMLStreamException {
             _encoder.writeStartDocument();
         }
 
         @Override
         public void writeStartElement(String localName) throws XMLStreamException {
            _encoder.writeStartElement(null, localName, null);
         }
 
         @Override
         public void writeStartElement(String nsURI, String localName)
                 throws XMLStreamException {
            _encoder.writeStartElement(null, localName, nsURI);
         }
 
         @Override
         public void writeStartElement(String prefix, String localName, String nsURI)
                 throws XMLStreamException
         {
             _encoder.writeStartElement(prefix, localName, nsURI);
         }
         
     }
 }
