 package org.vaadin.teemu.clara;
 
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Stack;
 
 import org.vaadin.teemu.clara.factory.ComponentManager;
 import org.xml.sax.Attributes;
 import org.xml.sax.SAXException;
 import org.xml.sax.helpers.DefaultHandler;
 
 import com.vaadin.ui.Component;
 import com.vaadin.ui.ComponentContainer;
 
 class LayoutInflaterContentHandler extends DefaultHandler {
 
     private Stack<Component> componentStack = new Stack<Component>();
     private ComponentContainer currentContainer;
     private Component currentComponent;
     private Component root;
     private String currentId;
     private final ComponentManager componentFactory;
     private final Map<String, Component> idMap = new HashMap<String, Component>();
 
     public LayoutInflaterContentHandler(ComponentManager componentFactory) {
         this.componentFactory = componentFactory;
     }
 
     public Component getRoot() {
         return root;
     }
 
     @Override
     public void startElement(String uri, String localName, String qName,
             Attributes attributes) throws SAXException {
         super.startElement(uri, localName, qName, attributes);
         currentComponent = null;
         if (uri.startsWith("urn:vaadin:")) {
             String packageName = uri.substring("urn:vaadin:".length());
             String className = localName;
 
             currentId = null;
             Map<String, String> attributeMap = getAttributeMap(attributes);
             currentComponent = componentFactory.createComponent(packageName,
                     className, attributeMap);
             if (currentId != null) {
                 idMap.put(currentId, currentComponent);
             }
             if (root == null) {
                 // This was the first Component created -> root.
                 root = currentComponent;
             }
             if (currentContainer != null) {
                 currentContainer.addComponent(currentComponent);
                 componentFactory.applyLayoutAttributes(currentContainer,
                         currentComponent, attributeMap);
             }
             if (currentComponent instanceof ComponentContainer) {
                 currentContainer = (ComponentContainer) currentComponent;
             }
             componentStack.push(currentComponent);
         }
     }
 
     private Map<String, String> getAttributeMap(Attributes attributes) {
         Map<String, String> attributeMap = new HashMap<String, String>(
                 attributes.getLength());
         for (int i = 0; i < attributes.getLength(); i++) {
             String value = attributes.getValue(i);
             String name = attributes.getLocalName(i);
             if (name.equals("id")) {
                 if (idMap.containsKey(value)) {
                     throw new LayoutInflaterException(String.format(
                             "Duplicate id: %s.", value));
                 } else {
                     currentId = value;
                 }
             } else {
                 attributeMap.put(name, value);
             }
         }
         return attributeMap;
     }
 
     @Override
     public void endElement(String uri, String localName, String qName)
             throws SAXException {
         super.endElement(uri, localName, qName);
         Component component = componentStack.pop();
         if (component instanceof ComponentContainer) {
             currentContainer = (ComponentContainer) component.getParent();
         }
     }
 
     public Map<String, Component> getIdMap() {
         return idMap;
     }
 
 }
