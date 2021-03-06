 /**
  *  Copyright 2012 Sven Ewald
  *
  *  Licensed under the Apache License, Version 2.0 (the "License");
  *  you may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the License for the specific language governing permissions and
  *  limitations under the License.
  */
 package org.xmlbeam;
 
 import java.text.MessageFormat;
 
 import java.lang.annotation.Annotation;
 import java.lang.reflect.Field;
 import java.lang.reflect.InvocationHandler;
 import java.lang.reflect.Method;
 
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.regex.Pattern;
 
 import java.io.IOException;
 import java.io.Serializable;
 import java.io.StringWriter;
 
 import javax.xml.parsers.ParserConfigurationException;
 import javax.xml.transform.TransformerConfigurationException;
 import javax.xml.transform.TransformerException;
 import javax.xml.transform.dom.DOMSource;
 import javax.xml.transform.stream.StreamResult;
 import javax.xml.xpath.XPath;
 import javax.xml.xpath.XPathConstants;
 import javax.xml.xpath.XPathExpression;
 import javax.xml.xpath.XPathExpressionException;
 
import org.mockito.cglib.core.ReflectUtils;
 import org.w3c.dom.Attr;
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 import org.w3c.dom.Node;
 import org.w3c.dom.NodeList;
 import org.xml.sax.SAXException;
 import org.xmlbeam.XBProjector.InternalProjection;
 import org.xmlbeam.annotation.XBDelete;
 import org.xmlbeam.annotation.XBDocURL;
 import org.xmlbeam.annotation.XBRead;
 import org.xmlbeam.annotation.XBValue;
 import org.xmlbeam.annotation.XBWrite;
 import org.xmlbeam.dom.DOMAccess;
 import org.xmlbeam.types.TypeConverter;
 import org.xmlbeam.util.intern.DOMHelper;
 import org.xmlbeam.util.intern.ReflectionHelper;
 
 /**
  * @author <a href="https://github.com/SvenEwald">Sven Ewald</a>
  */
 @SuppressWarnings("serial")
 class ProjectionInvocationHandler implements InvocationHandler, Serializable {
     private static final Pattern LEGAL_XPATH_SELECTORS_FOR_SETTERS = Pattern.compile("^(/[a-zA-Z]+)*((/@[a-z:A-Z]+)|(/\\*))?$");
     private final Node node;
     private final Class<?> projectionInterface;
     private final XBProjector projector;
     private final Map<Class<?>, Object> defaultInvokers = new HashMap<Class<?>, Object>();
 
     ProjectionInvocationHandler(final XBProjector projector, final Node node, final Class<?> projectionInterface) {
         this.projector = projector;
         this.node = node;
         this.projectionInterface = projectionInterface;
         DOMAccess projectionInvoker = new DOMAccess() {
             @Override
             public Class<?> getProjectionInterface() {
                 return ProjectionInvocationHandler.this.projectionInterface;
             }
 
             @Override
             public Node getDOMNode() {
                 return ProjectionInvocationHandler.this.node;
             }
 
             @Override
             public Document getDOMOwnerDocument() {
               if (Node.DOCUMENT_NODE == ProjectionInvocationHandler.this.node.getNodeType()) {
                   return (Document) ProjectionInvocationHandler.this.node;
               }
               return ProjectionInvocationHandler.this.node.getOwnerDocument();
             }
         };
         Object objectInvoker = new Serializable() {
             @Override
             public boolean equals(Object o) {
                 if (!(o instanceof DOMAccess)) {
                     return false;
                 }
                 DOMAccess op = (DOMAccess) o;
                 if (!ProjectionInvocationHandler.this.projectionInterface.equals(op.getProjectionInterface())) {
                     return false;
                 }
                 // Unfortunatly Node.isEqualNode() is implementation specific and does
                 // not need to match our hashCode implementation. 
                 return DOMHelper.nodesAreEqual(node,op.getDOMNode());
             }
 
             @Override
             public int hashCode() {
                 return 31 * ProjectionInvocationHandler.this.projectionInterface.hashCode() + 27 * DOMHelper.nodeHashCode(node);
             }
 
             @Override
             public String toString() {
                 try {
                     StringWriter writer = new StringWriter();
                     projector.config().createTransformer().transform(new DOMSource(node), new StreamResult(writer));
                     String output = writer.getBuffer().toString();
                     return output;
                 } catch (TransformerConfigurationException e) {
                     throw new RuntimeException(e);
                 } catch (TransformerException e) {
                     throw new RuntimeException(e);
                 }
             }
         };
         defaultInvokers.put(DOMAccess.class, projectionInvoker);
         defaultInvokers.put(Object.class, objectInvoker);
     }
 
     /**
      * @param collection
      * @param parentElement
      */
     private void applyCollectionSetProjectionOnelement(Collection<?> collection, Element parentElement) {
         Set<String> elementNames = new HashSet<String>();
         for (Object o : collection) {
             if (!(o instanceof InternalProjection)) {
                 throw new IllegalArgumentException("Setter argument collection contains an object of type " + o.getClass().getName() + ". When setting a collection on a Projection, the collection must not contain other types than Projections.");
             }
             InternalProjection p = (InternalProjection) o;
             elementNames.add(p.getDOMNode().getNodeName());
         }
         for (String elementName : elementNames) {
             DOMHelper.removeAllChildrenByName(parentElement, elementName);
         }
         for (Object o : collection) {
             InternalProjection p = (InternalProjection) o;
             parentElement.appendChild(p.getDOMNode());
         }
     }
 
     private void applySingleSetProjectionOnElement(final InternalProjection projection, final Node element) {
         DOMHelper.removeAllChildrenByName(element, projection.getDOMNode().getNodeName());
         element.appendChild(projection.getDOMNode());
     }
 
     private List<?> evaluateAsList(final XPathExpression expression, final Node node, final Method method) throws XPathExpressionException {
         NodeList nodes = (NodeList) expression.evaluate(node, XPathConstants.NODESET);
         List<Object> linkedList = new LinkedList<Object>();
         Class<?> targetType = findTargetComponentType(method);
         TypeConverter typeConverter = projector.config().getTypeConverter();
         if (typeConverter.isConvertable(targetType)) {
             for (int i = 0; i < nodes.getLength(); ++i) {
                 linkedList.add(typeConverter.convertTo(targetType, nodes.item(i).getTextContent()));
             }
             return linkedList;
         }
         if (targetType.isInterface()) {
             for (int i = 0; i < nodes.getLength(); ++i) {
                 Node n = nodes.item(i).cloneNode(true);
                 InternalProjection subprojection = (InternalProjection) projector.projectDOMNode(n, targetType);
                 linkedList.add(subprojection);
             }
             return linkedList;
         }
         throw new IllegalArgumentException("Return type " + targetType + " is not valid for list or array component type returning from method " + method + " using the current type converter:" + projector.config().getTypeConverter()
                 + ". Please change the return type to a sub projection or add a conversion to the type converter.");
     }
 
     /**
      * @param method
      * @return index of fist parameter annotated with {@link XBValue} annotation.
      */
     private int findIndexOfValue(Method method) {
         int index = 0;
         for (Annotation[] annotations : method.getParameterAnnotations()) {
             for (Annotation a : annotations) {
                 if (XBValue.class.equals(a.annotationType())) {
                     return index;
                 }
             }
             ++index;
         }
         return 0; // If no attribute is annotated, the first one is taken.
     }
 
     private Class<?> findTargetComponentType(final Method method) {
         if (method.getReturnType().isArray()) {
             return method.getReturnType().getComponentType();
         }
         Class<?> targetType = method.getAnnotation(XBRead.class).targetComponentType();
         if (XBRead.class.equals(targetType)) {
             throw new IllegalArgumentException("When using List as return type for method " + method + ", please specify the list content type in the " + XBRead.class.getSimpleName() + " annotaion. I can not determine it from the method signature.");
         }
         return targetType;
     }
 
     private Node getNodeForMethod(final Method method, final Object[] args) throws SAXException, IOException, ParserConfigurationException {
         Node evaluationNode = node;
         if (method.getAnnotation(XBDocURL.class) != null) {
             String uri = method.getAnnotation(XBDocURL.class).value();
             Map<String, String> requestParams = projector.io().filterRequestParamsFromParams(uri, args);
             uri = MessageFormat.format(uri, args);   
             evaluationNode = DOMHelper.getDocumentFromURL(projector.config().createDocumentBuilder(), uri, requestParams, projectionInterface);
         }
         return evaluationNode;
     }
 
     /**
      * Determine a methods return value that does not depend on the methods execution. Possible
      * values are void or the proxy itself (would be "this").
      * 
      * @param method
      * @return
      */
     private Object getProxyReturnValueForMethod(final Object proxy, final Method method) {
         if (!ReflectionHelper.hasReturnType(method)) {
             return null;
         }
         if (method.getReturnType().equals(method.getDeclaringClass())) {
             return proxy;
         }
         throw new IllegalArgumentException("Method " + method + " has illegal return type \"" + method.getReturnType() + "\". I don't know what to return. I expected void or " + method.getDeclaringClass().getSimpleName());
     }
 
     private void injectMeAttribute(InternalProjection me, Object target) {
         Class<?> projectionInterface = me.getProjectionInterface();
         for (Field field : target.getClass().getDeclaredFields()) {
             if (!isValidMeField(field,projectionInterface)) {
                 continue;
             }
             if (!field.isAccessible()) {
                 field.setAccessible(true);
             }
             try {
                 field.set(target, me);
                 return;
             } catch (IllegalAccessException e) {
                 throw new RuntimeException(e);
             }
         }
         throw new IllegalArgumentException("Mixin "+target.getClass().getSimpleName()+" needs an attribute \"private "+projectionInterface.getSimpleName()+" me;\" to be able to access the projection.");
     }
     
     private boolean isValidMeField(Field field, Class<?> projInterface) {
         if (field==null) {
             return false;
         }
         if (!"me".equalsIgnoreCase(field.getName())) {
             return false;
         }
         if (DOMAccess.class.equals(field.getType())) {
             return true;
         }
         return field.getType().isAssignableFrom(projInterface);
     }
 
     @Override
     public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
         Class<?> methodsDeclaringInterface=ReflectionHelper.findDeclaringInterface(method,projectionInterface);
         Object customInvoker = projector.mixins().getProjectionMixin(projectionInterface, methodsDeclaringInterface);
         if (customInvoker != null) {
             injectMeAttribute((InternalProjection) proxy, customInvoker);
             return method.invoke(customInvoker, args);
         }
         Object defaultInvoker = defaultInvokers.get(methodsDeclaringInterface);
         if (defaultInvoker != null) {
             return method.invoke(defaultInvoker, args);
         }
 
         XBDelete delAnnotation = method.getAnnotation(XBDelete.class);
         if (delAnnotation != null) {
             return invokeDeleter(proxy, method, MessageFormat.format(delAnnotation.value(), args));
         }
 
         XBWrite writeAnnotation = method.getAnnotation(XBWrite.class);
         if (writeAnnotation != null) {
             return invokeSetter(proxy, method, MessageFormat.format(writeAnnotation.value(), args), args);
         }
 
         XBRead readAnnotation = method.getAnnotation(XBRead.class);
         if (readAnnotation != null) {
             return invokeGetter(proxy, method, MessageFormat.format(readAnnotation.value(), args), args);
         }
         throw new IllegalArgumentException("I don't known how to invoke method " + method + ". Did you forget to add a XB*-annotation or to register a mixin?");
     }
 
     /**
      * @param proxy
      * @param format
      */
     private Object invokeDeleter(Object proxy, Method method, String path) throws Throwable {
         final Document document = Node.DOCUMENT_NODE == node.getNodeType() ? ((Document) node) : node.getOwnerDocument();
         final XPath xPath = projector.config().createXPath(document);
         final XPathExpression expression = xPath.compile(path);
         NodeList nodes = (NodeList) expression.evaluate(node, XPathConstants.NODESET);
         for (int i = 0; i < nodes.getLength(); ++i) {
             if (Node.ATTRIBUTE_NODE==nodes.item(i).getNodeType()) {
                 Attr attr = (Attr) nodes.item(i);
                 attr.getOwnerElement().removeAttributeNode(attr);
                 continue;
             }                     
             Node parentNode = nodes.item(i).getParentNode();
             if (parentNode==null) {
                 continue;
             }
             parentNode.removeChild(nodes.item(i));
         }
         return getProxyReturnValueForMethod(proxy, method);
     }
 
     private Object invokeGetter(final Object proxy, final Method method, final String path, final Object[] args) throws Throwable {
         final Node node = getNodeForMethod(method, args);
         final Document document = Node.DOCUMENT_NODE == node.getNodeType() ? ((Document) node) : node.getOwnerDocument();
         final XPath xPath = projector.config().createXPath(document);
         final XPathExpression expression = xPath.compile(path);
         final Class<?> returnType = method.getReturnType();
         if (projector.config().getTypeConverter().isConvertable(returnType)) {
             String data = (String) expression.evaluate(node, XPathConstants.STRING);
             try {
                 return projector.config().getTypeConverter().convertTo(returnType, data);
             } catch (NumberFormatException e) {
                 throw new NumberFormatException(e.getMessage() + " XPath was:" + path);
             }
         }
         if (List.class.equals(returnType)) {
             return evaluateAsList(expression, node, method);
         }
         if (returnType.isArray()) {
             List<?> list = evaluateAsList(expression, node, method);            
             return list.toArray((Object[]) java.lang.reflect.Array.newInstance(returnType.getComponentType(), list.size()));
         }
         if (returnType.isInterface()) {
             Node newNode = (Node) expression.evaluate(node, XPathConstants.NODE);
             if (newNode==null) {
                 return null;
             }
             InternalProjection subprojection = (InternalProjection) projector.projectDOMNode(newNode, returnType);
             return subprojection;
         }
         throw new IllegalArgumentException("Return type " + returnType + " of method " + method + " is not supported. Please change to an projection interface, a List, an Array or one of current type converters types:" + projector.config().getTypeConverter());
     }
 
     private Object invokeSetter(final Object proxy, final Method method, final String path, final Object[] args) throws Throwable {
         if (!LEGAL_XPATH_SELECTORS_FOR_SETTERS.matcher(path).matches()) {
             throw new IllegalArgumentException("Method " + method + " was invoked as setter and did not have an XPATH expression with an absolute path to an element or attribute:\"" + path + "\"");
         }
         if (!ReflectionHelper.hasParameters(method)) {
             throw new IllegalArgumentException("Method " + method + " was invoked as setter but has no parameter. Please add a parameter so this method could actually change the DOM.");
         }
         if (method.getAnnotation(XBDocURL.class)!=null) {
             throw new IllegalArgumentException("Method " + method + " was invoked as setter but has a @"+XBDocURL.class.getSimpleName()+" annotation. Defining setters on external projections is not valid, because setters always change parts of documents.");
         }
         final String pathToElement = path.replaceAll("/@.*", "");
         final Node settingNode = getNodeForMethod(method, args);
         final Document document = Node.DOCUMENT_NODE == settingNode.getNodeType() ? ((Document) settingNode) : settingNode.getOwnerDocument();
         assert document != null;
         final Object valuetToSet = args[findIndexOfValue(method)];
         if ("/*".equals(pathToElement)) { // Setting a new root element.
             if ((valuetToSet != null) && (!(valuetToSet instanceof InternalProjection))) {
                 throw new IllegalArgumentException("Method " + method + " was invoked as setter changing the document root element. Expected value type was a projection but you provided a " + valuetToSet);
             }
             InternalProjection projection = (InternalProjection) valuetToSet;
             Element element = Node.DOCUMENT_NODE == projection.getDOMNode().getNodeType() ? ((Document) projection.getDOMNode()).getDocumentElement() : (Element) projection.getDOMNode();
             assert element != null;
             DOMHelper.setDocumentElement(document, element);
         } else {
             Element elementToChange = DOMHelper.ensureElementExists(document, pathToElement);
 
             if (path.contains("@")) {
                 String attributeName = path.replaceAll(".*@", "");
                 elementToChange.setAttribute(attributeName, valuetToSet.toString());
             } else {
                 if (valuetToSet instanceof InternalProjection) {
                     applySingleSetProjectionOnElement((InternalProjection) args[0], elementToChange);
                 }
                 if (valuetToSet instanceof Collection) {
                     applyCollectionSetProjectionOnelement((Collection<?>) valuetToSet, elementToChange);
                 } else {
                     elementToChange.setTextContent(valuetToSet.toString());
                 }
             }
         }
         return getProxyReturnValueForMethod(proxy, method);
     }
 }
