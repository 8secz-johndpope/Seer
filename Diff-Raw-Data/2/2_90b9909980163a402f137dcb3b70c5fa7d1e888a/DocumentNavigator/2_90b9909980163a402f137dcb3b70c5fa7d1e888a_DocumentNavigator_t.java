 
 package org.jaxen.jdom;
 
 import org.jaxen.DefaultNavigator;
 import org.jaxen.FunctionCallException;
 
 import org.jaxen.util.SingleObjectIterator;
 
 import org.jdom.Document;
 import org.jdom.Element;
 import org.jdom.Comment;
 import org.jdom.Attribute;
 import org.jdom.Text;
 import org.jdom.CDATA;
 import org.jdom.ProcessingInstruction;
 import org.jdom.Namespace;
 import org.jdom.JDOMException;
 import org.jdom.input.SAXBuilder;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Iterator;
 
 public class DocumentNavigator extends DefaultNavigator
 {
     private static class Singleton
     {
         private static DocumentNavigator instance = new DocumentNavigator();
     }
 
     public static DocumentNavigator getInstance()
     {
         return Singleton.instance;
     }
 
     public boolean isElement(Object obj)
     {
         return obj instanceof Element;
     }
 
     public boolean isComment(Object obj)
     {
         return obj instanceof Comment;
     }
 
     public boolean isText(Object obj)
     {
         return ( obj instanceof String
                  ||
                  obj instanceof Text
                  ||
                  obj instanceof CDATA );
     }
 
     public boolean isAttribute(Object obj)
     {
         return obj instanceof Attribute;
     }
 
     public boolean isProcessingInstruction(Object obj)
     {
         return obj instanceof ProcessingInstruction;
     }
 
     public boolean isDocument(Object obj)
     {
         return obj instanceof Document;
     }
 
     public boolean isNamespace(Object obj)
     {
         return obj instanceof Namespace;
     }
 
     public String getElementName(Object obj)
     {
         Element elem = (Element) obj;
 
         return elem.getName();
     }
 
     public String getElementNamespaceUri(Object obj)
     {
         Element elem = (Element) obj;
         
         return elem.getNamespaceURI();
     }
 
     public String getAttributeName(Object obj)
     {
         Attribute attr = (Attribute) obj;
 
         return attr.getName();
     }
 
     public String getAttributeNamespaceUri(Object obj)
     {
         Attribute attr = (Attribute) obj;
 
         return attr.getNamespaceURI();
     }
 
     public Iterator getChildAxisIterator(Object contextNode)
     {
         if ( contextNode instanceof Element )
         {
             return ((Element)contextNode).getContent().iterator();
         }
         else if ( contextNode instanceof Document )
         {
             return ((Document)contextNode).getContent().iterator();
         }
 
         return null;
     }
 
     public Iterator getNamespaceAxisIterator(Object contextNode)
     {
         if ( ! ( contextNode instanceof Element ) )
         {
             return null;
         }
 
         Element elem = (Element) contextNode;
 
         List nsList = new ArrayList();
 
         Namespace ns = elem.getNamespace();
 
         if ( ns != Namespace.NO_NAMESPACE )
         {
             nsList.add( elem.getNamespace() );
         }
 
         nsList.addAll( elem.getAdditionalNamespaces() );
 
        nsList.add( Namespace.XML_NAMESPACE );

         return nsList.iterator();
     }
 
     public Iterator getParentAxisIterator(Object contextNode)
     {
         Object parent = null;
 
         if ( contextNode instanceof Document )
         {
             parent = contextNode;
         }
         else if ( contextNode instanceof Element )
         {
             parent = ((Element)contextNode).getParent();
 
             if ( parent == null )
             {
                 if ( ((Element)contextNode).isRootElement() )
                 {
                     parent = ((Element)contextNode).getDocument();
                 }
             }
         }
         else if ( contextNode instanceof Attribute )
         {
             parent = ((Attribute)contextNode).getParent();
         }
         else if ( contextNode instanceof ProcessingInstruction )
         {
             parent = ((ProcessingInstruction)contextNode).getParent();
         }
         else if ( contextNode instanceof Text )
         {
             parent = ((Text)contextNode).getParent();
         }
         else if ( contextNode instanceof Comment )
         {
             parent = ((Text)contextNode).getParent();
         }
         
         if ( parent != null )
         {
             return new SingleObjectIterator( parent );
         }
 
         return null;
     }
 
     public Iterator getAttributeAxisIterator(Object contextNode)
     {
         if ( ! ( contextNode instanceof Element ) )
         {
             return null;
         }
 
         Element elem = (Element) contextNode;
 
         return elem.getAttributes().iterator();
     }
 
     public Object getDocumentNode(Object contextNode)
     {
         if ( contextNode instanceof Document )
         {
             return contextNode;
         }
 
         Element elem = (Element) contextNode;
 
         return elem.getDocument();
     }
 
     public String getElementQName(Object obj)
     {
         Element elem = (Element) obj;
 
         String prefix = elem.getNamespacePrefix();
 
         if ( "".equals( prefix ) )
         {
             return elem.getName();
         }
 
         return prefix + ":" + elem.getName();
     }
 
     public String getAttributeQName(Object obj)
     {
         Attribute attr = (Attribute) obj;
 
         String prefix = attr.getNamespacePrefix();
 
         if ( "".equals( prefix ) )
         {
             return attr.getName();
         }
 
         return prefix + ":" + attr.getName();
     }
 
     public String getNamespaceStringValue(Object obj)
     {
         Namespace ns = (Namespace) obj;
 
         return ns.getURI();
     }
 
     public String getTextStringValue(Object obj)
     {
         if ( obj instanceof Text )
         {
             return ((Text)obj).getValue();
         }
 
         if ( obj instanceof CDATA )
         {
             return ((CDATA)obj).getText();
         }
 
         return "";
     }
 
     public String getAttributeStringValue(Object obj)
     {
         Attribute attr = (Attribute) obj;
 
         return attr.getValue();
     }
 
     public String getElementStringValue(Object obj)
     {
         Element elem = (Element) obj;
 
         StringBuffer buf = new StringBuffer();
 
         List     content     = elem.getContent();
         Iterator contentIter = content.iterator();
         Object   each        = null;
 
         while ( contentIter.hasNext() )
         {
             each = contentIter.next();
 
             if ( each instanceof String )
             {
                 buf.append( each );
             }
             else if ( each instanceof Text )
             {
                 buf.append( ((Text)each).getValue() );
             }
             else if ( each instanceof CDATA )
             {
                 buf.append( ((CDATA)each).getText() );
             }
             else if ( each instanceof Element )
             {
                 buf.append( getElementStringValue( each ) );
             }
         }
 
         return buf.toString();
     }
 
     public String getProcessingInstructionTarget(Object obj)
     {
         ProcessingInstruction pi = (ProcessingInstruction) obj;
 
         return pi.getTarget();
     }
 
     public String getProcessingInstructionData(Object obj)
     {
         ProcessingInstruction pi = (ProcessingInstruction) obj;
 
         return pi.getData();
     }
 
     public String getCommentStringValue(Object obj)
     {
         Comment cmt = (Comment) obj;
 
         return cmt.getText();
     }
 
     public String translateNamespacePrefixToUri(String prefix, Object context)
     {
         Element element = null;
         if ( context instanceof Element ) 
         {
             element = (Element) context;
         }
         else if ( context instanceof Attribute )
         {
             element = ((Attribute)context).getParent();
         }
         else if ( context instanceof Text )
         {
             element = ((Text)context).getParent();
         }
         else if ( context instanceof Comment )
         {
             element = ((Comment)context).getParent();
         }
         else if ( context instanceof ProcessingInstruction )
         {
             element = ((ProcessingInstruction)context).getParent();
         }
 
         if ( element != null )
         {
             Namespace namespace = element.getNamespace( prefix );
 
             if ( namespace != null ) 
             {
                 return namespace.getURI();
             }
         }
         return null;
     }
 
     public Object getDocument(String url) throws FunctionCallException
     {
         try
         {
             SAXBuilder builder = new SAXBuilder();
             
             return builder.build( url );
         }
         catch (JDOMException e)
         {
             throw new FunctionCallException( e.getMessage() );
         }
     }
 }
