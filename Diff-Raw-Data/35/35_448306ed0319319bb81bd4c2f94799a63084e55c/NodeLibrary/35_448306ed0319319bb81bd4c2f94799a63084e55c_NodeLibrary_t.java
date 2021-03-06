 package nodebox.node;
 
 import com.google.common.base.Objects;
 import com.google.common.base.Splitter;
 import com.google.common.collect.ImmutableList;
 import com.google.common.collect.ImmutableMap;
 import nodebox.function.FunctionLibrary;
 import nodebox.function.FunctionRepository;
 import nodebox.graphics.Point;
 import nodebox.util.FileUtils;
 import nodebox.util.LoadException;
 
 import javax.xml.stream.XMLInputFactory;
 import javax.xml.stream.XMLStreamConstants;
 import javax.xml.stream.XMLStreamException;
 import javax.xml.stream.XMLStreamReader;
 import java.io.*;
 import java.util.*;
 
 import static com.google.common.base.Preconditions.*;
 
 public class NodeLibrary {
 
    public static final String CURRENT_FORMAT_VERSION = "15";
 
     public static final Splitter PORT_NAME_SPLITTER = Splitter.on(".");
 
     public static NodeLibrary create(String libraryName, Node root) {
         return create(libraryName, root, NodeRepository.of(), FunctionRepository.of(), UUID.randomUUID());
     }
 
     public static NodeLibrary create(String libraryName, Node root, FunctionRepository functionRepository) {
         return create(libraryName, root, NodeRepository.of(), functionRepository);
     }
 
     public static NodeLibrary create(String libraryName, Node root, NodeRepository nodeRepository, FunctionRepository functionRepository) {
         return create(libraryName, root, nodeRepository, functionRepository, UUID.randomUUID());
     }
 
     private static NodeLibrary create(String libraryName, Node root, NodeRepository nodeRepository, FunctionRepository functionRepository, UUID uuid) {
         return new NodeLibrary(libraryName, null, root, nodeRepository, functionRepository, ImmutableMap.<String, String>of(), uuid);
     }
 
     public static NodeLibrary load(String libraryName, String xml, NodeRepository nodeRepository) throws LoadException {
         checkNotNull(libraryName, "Library name cannot be null.");
         checkNotNull(xml, "XML string cannot be null.");
         try {
             return load(libraryName, null, new StringReader(xml), nodeRepository);
         } catch (XMLStreamException e) {
             throw new LoadException(null, "Could not read NDBX string", e);
         }
     }
 
     public static NodeLibrary load(String libraryName, String xml, File baseFile, NodeRepository nodeRepository) throws LoadException {
         checkNotNull(libraryName, "Library name cannot be null.");
         checkNotNull(xml, "XML string cannot be null.");
         try {
             return load(libraryName, baseFile, new StringReader(xml), nodeRepository);
         } catch (XMLStreamException e) {
             throw new LoadException(null, "Could not read NDBX string", e);
         }
     }
 
 
     public static NodeLibrary load(File f, NodeRepository nodeRepository) throws LoadException {
         checkNotNull(f, "File cannot be null.");
         String libraryName = FileUtils.stripExtension(f);
         try {
             return load(libraryName, f, new FileReader(f), nodeRepository);
         } catch (FileNotFoundException e) {
             throw new LoadException(f, "File not found.");
         } catch (XMLStreamException e) {
             throw new LoadException(f, "Could not read NDBX file", e);
         }
     }
 
     private final String name;
     private final File file;
     private final Node root;
     private final NodeRepository nodeRepository;
     private final FunctionRepository functionRepository;
     private final ImmutableMap<String, String> properties;
     private final UUID uuid;
 
     private NodeLibrary(String name, File file, Node root, NodeRepository nodeRepository, FunctionRepository functionRepository, Map<String, String> properties, UUID uuid) {
         checkNotNull(name, "Name cannot be null.");
         checkNotNull(root, "Root node cannot be null.");
         checkNotNull(functionRepository, "Function repository cannot be null.");
         this.name = name;
         this.root = root;
         this.nodeRepository = nodeRepository;
         this.functionRepository = functionRepository;
         this.file = file;
         this.properties = ImmutableMap.copyOf(properties);
         this.uuid = uuid;
     }
 
     public String getName() {
         return name;
     }
 
     public File getFile() {
         return file;
     }
 
     public UUID getUuid() {
         return uuid;
     }
 
     public Node getRoot() {
         return root;
     }
 
     public Node getNodeForPath(String path) {
         checkArgument(path.startsWith("/"), "Only absolute paths are supported.");
         if (path.length() == 1) return root;
 
         Node node = root;
         path = path.substring(1);
         for (String name : Splitter.on("/").split(path)) {
             node = node.getChild(name);
             if (node == null) return null;
         }
         return node;
     }
 
     public NodeRepository getNodeRepository() {
         return nodeRepository;
     }
 
     public FunctionRepository getFunctionRepository() {
         return functionRepository;
     }
 
 
     //// Properties ////
 
     public boolean hasProperty(String name) {
         return properties.containsKey(name);
     }
 
     public String getProperty(String name) {
         return properties.get(name);
     }
 
     public Set<String> getPropertyNames() {
         return properties.keySet();
     }
 
     public String getProperty(String name, String defaultValue) {
         if (hasProperty(name)) {
             return properties.get(name);
         } else {
             return defaultValue;
         }
     }
 
     public Map<String, String> getProperties() {
         return properties;
     }
 
     public boolean isValidPropertyName(String name) {
         checkNotNull(name);
         // no whitespace, only lowercase, numbers + period.
         return true;
     }
 
     public NodeLibrary withProperty(String name, String value) {
         ImmutableMap.Builder<String, String> b = ImmutableMap.builder();
         checkArgument(isValidPropertyName(name), "Property name '%s' is not valid.", name);
         b.putAll(properties);
         b.put(name, value);
         return withProperties(b.build());
     }
 
     public NodeLibrary withPropertyRemoved(String name) {
         if (!hasProperty(name)) return this;
         ImmutableMap.Builder<String, String> b = ImmutableMap.builder();
         for (Map.Entry<String, String> entry : this.properties.entrySet()) {
             if (!entry.getKey().equals(name)) {
                 b.put(entry);
             }
         }
         return withProperties(b.build());
     }
 
     public NodeLibrary withProperties(Map<String, String> properties) {
         return new NodeLibrary(this.name, this.file, this.root, this.nodeRepository, this.functionRepository, ImmutableMap.copyOf(properties), this.uuid);
 
     }
 
     //// Loading ////
 
     private static NodeLibrary load(String libraryName, File file, Reader r, NodeRepository nodeRepository) throws XMLStreamException {
         XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
         XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(r);
         NodeLibrary nodeLibrary = null;
         while (reader.hasNext()) {
             int eventType = reader.next();
             if (eventType == XMLStreamConstants.START_ELEMENT) {
                 String tagName = reader.getLocalName();
                 if (tagName.equals("ndbx")) {
                     String formatVersion = reader.getAttributeValue(null, "formatVersion");
                     if (formatVersion != null && !CURRENT_FORMAT_VERSION.equals(formatVersion)) {
                         throw new OutdatedLibraryException(file, "File uses version " + formatVersion + ", current version is " + CURRENT_FORMAT_VERSION + ".");
                     }
                     String uuidString = reader.getAttributeValue(null, "uuid");
                     UUID uuid = (uuidString == null) ? UUID.randomUUID() : UUID.fromString(uuidString);
                     nodeLibrary = parseNDBX(libraryName, file, reader, nodeRepository, uuid);
                 } else {
                     throw new XMLStreamException("Only tag ndbx allowed, not " + tagName, reader.getLocation());
                 }
             }
         }
         return nodeLibrary;
     }
 
     private static NodeLibrary parseNDBX(String libraryName, File file, XMLStreamReader reader, NodeRepository nodeRepository, UUID uuid) throws XMLStreamException {
         List<FunctionLibrary> functionLibraries = new LinkedList<FunctionLibrary>();
         Map<String, String> propertyMap = new HashMap<String, String>();
         Node rootNode = Node.ROOT;
 
         while (true) {
             int eventType = reader.next();
             if (eventType == XMLStreamConstants.START_ELEMENT) {
                 String tagName = reader.getLocalName();
                 if (tagName.equals("property")) {
                     parseProperty(reader, propertyMap);
                 } else if (tagName.equals("link")) {
                     FunctionLibrary functionLibrary = parseLink(file, reader);
                     functionLibraries.add(functionLibrary);
                 } else if (tagName.equals("node")) {
                     rootNode = parseNode(reader, rootNode, nodeRepository);
                 } else {
                     throw new XMLStreamException("Unknown tag " + tagName, reader.getLocation());
                 }
             } else if (eventType == XMLStreamConstants.END_ELEMENT) {
                 String tagName = reader.getLocalName();
                 if (tagName.equals("ndbx"))
                     break;
             }
         }
         FunctionLibrary[] fl = functionLibraries.toArray(new FunctionLibrary[functionLibraries.size()]);
         return new NodeLibrary(libraryName, file, rootNode, nodeRepository, FunctionRepository.of(fl), propertyMap, uuid);
     }
 
     private static FunctionLibrary parseLink(File file, XMLStreamReader reader) throws XMLStreamException {
         String linkRelation = reader.getAttributeValue(null, "rel");
         checkState(linkRelation.equals("functions"));
         String ref = reader.getAttributeValue(null, "href");
         // loading should happen lazily?
         return FunctionLibrary.load(file, ref);
     }
 
     /**
      * Parse the <property> tag and add the result to the propertyMap.
      */
     private static void parseProperty(XMLStreamReader reader, Map<String, String> propertyMap) throws XMLStreamException {
         String name = reader.getAttributeValue(null, "name");
         String value = reader.getAttributeValue(null, "value");
         if (name == null || value == null) return;
         propertyMap.put(name, value);
     }
 
     /**
      * Parse the <node> tag.
      *
      * @param reader         The XML stream.
      * @param parent         The parent node.
      * @param nodeRepository The node library dependencies.
      * @return The new node.
      * @throws XMLStreamException if a parse error occurs.
      */
     private static Node parseNode(XMLStreamReader reader, Node parent, NodeRepository nodeRepository) throws XMLStreamException {
         String prototypeId = reader.getAttributeValue(null, "prototype");
         String name = reader.getAttributeValue(null, "name");
         String category = reader.getAttributeValue(null, "category");
         String description = reader.getAttributeValue(null, "description");
         String image = reader.getAttributeValue(null, "image");
         String function = reader.getAttributeValue(null, "function");
         String outputType = reader.getAttributeValue(null, "outputType");
         String outputRange = reader.getAttributeValue(null, "outputRange");
         String position = reader.getAttributeValue(null, "position");
         String renderedChildName = reader.getAttributeValue(null, "renderedChild");
         String handle = reader.getAttributeValue(null, "handle");
         Node prototype = prototypeId == null ? Node.ROOT : lookupNode(prototypeId, parent, nodeRepository);
         if (prototype == null) {
             throw new XMLStreamException("Prototype " + prototypeId + " could not be found.", reader.getLocation());
         }
         Node node = prototype.extend();
 
         if (name != null)
             node = node.withName(name);
         if (category != null)
             node = node.withCategory(category);
         if (description != null)
             node = node.withDescription(description);
         if (image != null)
             node = node.withImage(image);
         if (function != null)
             node = node.withFunction(function);
         if (outputType != null)
             node = node.withOutputType(outputType);
         if (outputRange != null)
             node = node.withOutputRange(Port.Range.valueOf(outputRange.toUpperCase()));
         if (position != null)
             node = node.withPosition(Point.valueOf(position));
         if (handle != null)
             node = node.withHandle(handle);
 
         while (true) {
             int eventType = reader.next();
             if (eventType == XMLStreamConstants.START_ELEMENT) {
                 String tagName = reader.getLocalName();
                 if (tagName.equals("node")) {
                     node = node.withChildAdded(parseNode(reader, node, nodeRepository));
                 } else if (tagName.equals("port")) {
                     String portName = reader.getAttributeValue(null, "name");
                     // Remove the port if it is already on the prototype.
                     if (node.hasInput(portName)) {
                         node = node.withInputChanged(portName, parsePort(reader, node.getInput(portName)));
                     } else {
                         node = node.withInputAdded(parsePort(reader, null));
                     }
                 } else if (tagName.equals("conn")) {
                     node = node.withConnectionAdded(parseConnection(reader));
                 } else {
                     throw new XMLStreamException("Unknown tag " + tagName, reader.getLocation());
                 }
             } else if (eventType == XMLStreamConstants.END_ELEMENT) {
                 String tagName = reader.getLocalName();
                 if (tagName.equals("node"))
                     break;
             }
         }
 
         // This has to come at the end, since the child first needs to exist.
         if (renderedChildName != null)
             node = node.withRenderedChildName(renderedChildName);
 
         return node;
     }
 
     /**
      * Lookup the node in the node repository.
      * <p/>
      * If the node id consists of just a node name, without spaces, it is looked up in the parent node.
      *
      * @param nodeId         The node id.
      * @param parent         The parent node.
      * @param nodeRepository The node repository.
      * @return The existing node.
      */
     private static Node lookupNode(String nodeId, Node parent, NodeRepository nodeRepository) {
         if (nodeId.contains(".")) {
             return nodeRepository.getNode(nodeId);
         } else {
             return parent.getChild(nodeId);
         }
     }
 
     private static Port parsePort(XMLStreamReader reader, Port prototype) throws XMLStreamException {
         // Name and type are always required.
         String name = reader.getAttributeValue(null, "name");
         String type = reader.getAttributeValue(null, "type");
         String childReference = reader.getAttributeValue(null, "childReference");
         String widget = reader.getAttributeValue(null, "widget");
         String range = reader.getAttributeValue(null, "range");
         String value = reader.getAttributeValue(null, "value");
         String min = reader.getAttributeValue(null, "min");
         String max = reader.getAttributeValue(null, "max");
 
         Port port;
         if (prototype == null) {
             port = Port.portForType(name, type);
         } else {
             port = prototype;
         }
 
         // Widget, value, min, max are optional and could come from the prototype.
         if (childReference != null)
             port = port.withParsedAttribute(Port.Attribute.CHILD_REFERENCE, childReference);
         if (widget != null)
             port = port.withParsedAttribute(Port.Attribute.WIDGET, widget);
         if (range != null)
             port = port.withParsedAttribute(Port.Attribute.RANGE, range);
         if (value != null)
             port = port.withParsedAttribute(Port.Attribute.VALUE, value);
         if (min != null)
             port = port.withParsedAttribute(Port.Attribute.MINIMUM_VALUE, min);
         if (max != null)
             port = port.withParsedAttribute(Port.Attribute.MAXIMUM_VALUE, max);
 
         ImmutableList.Builder<MenuItem> b = ImmutableList.builder();
 
         while (true) {
             int eventType = reader.next();
             if (eventType == XMLStreamConstants.START_ELEMENT) {
                 String tagName = reader.getLocalName();
                 if (tagName.equals("menu")) {
                     b.add(parseMenuItem(reader));
                 } else {
                     throw new XMLStreamException("Unknown tag " + tagName, reader.getLocation());
                 }
             } else if (eventType == XMLStreamConstants.END_ELEMENT) {
                 String tagName = reader.getLocalName();
                 if (tagName.equals("port"))
                     break;
             }
         }
         ImmutableList<MenuItem> items = b.build();
         if (!items.isEmpty())
             port = port.withMenuItems(items);
         return port;
     }
 
     private static MenuItem parseMenuItem(XMLStreamReader reader) throws XMLStreamException {
         String key = reader.getAttributeValue(null, "key");
         String label = reader.getAttributeValue(null, "label");
         if (key == null)
             throw new XMLStreamException("Menu item key cannot be null.", reader.getLocation());
         return new MenuItem(key, label != null ? label : key);
     }
 
     private static Connection parseConnection(XMLStreamReader reader) throws XMLStreamException {
         String outputNode = reader.getAttributeValue(null, "output");
         String input = reader.getAttributeValue(null, "input");
         Iterator<String> inputIterator = PORT_NAME_SPLITTER.split(input).iterator();
         String inputNode = inputIterator.next();
         String inputPort = inputIterator.next();
         return new Connection(outputNode, inputNode, inputPort);
     }
 
     ///// Mutation methods ////
 
     public NodeLibrary withRoot(Node newRoot) {
         return new NodeLibrary(this.name, this.file, newRoot, this.nodeRepository, this.functionRepository, this.properties, this.uuid);
     }
 
     public NodeLibrary withFunctionRepository(FunctionRepository newRepository) {
         return new NodeLibrary(this.name, this.file, this.root, this.nodeRepository, newRepository, this.properties, this.uuid);
     }
 
     public NodeLibrary withFile(File newFile) {
         return new NodeLibrary(this.name, newFile, this.root, this.nodeRepository, this.functionRepository, this.properties, this.uuid);
     }
 
     //// Saving ////
 
     public String toXml() {
         return NDBXWriter.asString(this);
     }
 
     /**
      * Write the NodeLibrary to a file.
      *
      * @param file The file to save.
      * @throws java.io.IOException When file saving fails.
      */
     public void store(File file) throws IOException {
         NDBXWriter.write(this, file);
     }
 
     //// Object overrides ////
 
     @Override
     public int hashCode() {
         return Objects.hashCode(name, root, functionRepository);
     }
 
     @Override
     public boolean equals(Object o) {
         if (!(o instanceof NodeLibrary)) return false;
         final NodeLibrary other = (NodeLibrary) o;
         return Objects.equal(name, other.name)
                 && Objects.equal(root, other.root)
                 && Objects.equal(functionRepository, other.functionRepository);
     }
 
     @Override
     public String toString() {
         return String.format("<NodeLibrary %s>", name);
     }
 
 }
