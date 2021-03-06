 // PathVisio,
 // a tool for data visualization and analysis using Biological Pathways
 // Copyright 2006-2007 BiGCaT Bioinformatics
 //
 // Licensed under the Apache License, Version 2.0 (the "License"); 
 // you may not use this file except in compliance with the License. 
 // You may obtain a copy of the License at 
 // 
 // http://www.apache.org/licenses/LICENSE-2.0 
 //  
 // Unless required by applicable law or agreed to in writing, software 
 // distributed under the License is distributed on an "AS IS" BASIS, 
 // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 // See the License for the specific language governing permissions and 
 // limitations under the License.
 //
 package org.pathvisio.kegg;
 
 import java.awt.Color;
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileReader;
 import java.io.IOException;
 import java.rmi.RemoteException;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 
 import javax.xml.rpc.ServiceException;
 
 import keggapi.Definition;
 import keggapi.KEGGLocator;
 import keggapi.KEGGPortType;
 import keggapi.LinkDBRelation;
 
 import org.jdom.Document;
 import org.jdom.Element;
 import org.jdom.JDOMException;
 import org.jdom.input.SAXBuilder;
 import org.pathvisio.debug.Logger;
 import org.pathvisio.model.ConnectorType;
 import org.pathvisio.model.ConverterException;
 import org.pathvisio.model.DataNodeType;
 import org.pathvisio.model.DataSource;
 import org.pathvisio.model.GpmlFormat;
import org.pathvisio.model.GpmlFormatImpl1;
 import org.pathvisio.model.LineStyle;
 import org.pathvisio.model.LineType;
 import org.pathvisio.model.MLine;
 import org.pathvisio.model.ObjectType;
 import org.pathvisio.model.Organism;
 import org.pathvisio.model.OutlineType;
 import org.pathvisio.model.Pathway;
 import org.pathvisio.model.PathwayElement;
 import org.pathvisio.model.ConnectorShape.Segment;
 import org.pathvisio.model.ConnectorShape.WayPoint;
 import org.pathvisio.model.PathwayElement.MAnchor;
 import org.pathvisio.model.PathwayElement.MPoint;
 import org.pathvisio.view.LinAlg;
 import org.pathvisio.view.LinAlg.Point;
 import org.xml.sax.EntityResolver;
 import org.xml.sax.InputSource;
 
 public class KeggFormat {
 	boolean useWebservice = true;
 	
 	private static final String COMMENT_SOURCE = "KeggConverter";
 	private static KEGGLocator keggLocator = new KEGGLocator();
 	private static KEGGPortType keggPortType;
 
 	private static String ELM_ENTRY = "entry";
 	private static String ELM_RELATION = "relation";
 	private static String ELM_GRAPHICS = "graphics";
 	private static String ELM_REACTION = "reaction";
 
 	private static String TYPE_MAP = "map";
 	private static String TYPE_ENZYME = "enzyme";
 	private static String TYPE_COMPOUND = "compound";
 	private static String TYPE_ORTHOLOG = "ortholog";
 	private static String TYPE_GENE = "gene";
 
 	private HashMap<String, PathwayElement> id2element = 
 		new HashMap<String, PathwayElement>();
 	
 	private HashMap<String, List<PathwayElement>> reaction2element = 
 		new HashMap<String, List<PathwayElement>>();
 
 	private HashMap<String, PathwayElement> compound2element = 
 		new HashMap<String, PathwayElement>();
 
 	private Pathway pathway;
 	private Organism organism;
 
 	private KeggFormat(Pathway pathway) {
 		this.pathway = pathway;
 		this.organism = Organism.fromLatinName(pathway.getMappInfo().getOrganism());
 	}
 
 	private void doMapping(Document keggDoc, Pathway pathway) throws RemoteException, ConverterException {
 		Element rootelement = keggDoc.getRootElement();
 
 		PathwayElement mappInfo = pathway.getMappInfo();
 		String title = rootelement.getAttributeValue("title");
 		if(title != null) {
 			if(title.length() > 50) {
 				mappInfo.addComment(mappInfo.new Comment("Name truncated from: " + title, COMMENT_SOURCE));
 				title = title.substring(0, 50);
 			}
 			mappInfo.setMapInfoName(title);
 		}
 		mappInfo.setMapInfoDataSource("Kegg: " + rootelement.getAttributeValue("link"));
 
 		List<Element> keggElements = rootelement.getChildren();
 
 		int progress = 0;
 		for(Element keggElement : keggElements) {
 			String elementName = keggElement.getName();
 			String name = keggElement.getAttributeValue("name");
 			String type = keggElement.getAttributeValue("type");
 
 			Logger.log.trace(
 					"Processing element " + ++progress + " out of " + 
 					keggElements.size() + ": " + name + ", " + type);
 
 			if			(ELM_ENTRY.equals(elementName)) {
 				mapEntry(keggElement);
 			} else if 	(ELM_REACTION.equals(elementName)) {
 				mapReaction(keggElement);
 			} else if	(ELM_RELATION.equals(elementName)) {
 				mapRelation(keggElement);
 			}
 		}
 	}
 
 	private void mapRelation(Element relation) {
 		String type = relation.getAttributeValue("type");
 		if("ECrel".equals(type)) {
 			return; //ECrel is redundant with reaction
 		}
 			
 		PathwayElement e1 = id2element.get(relation.getAttributeValue("entry1"));
 		PathwayElement e2 = id2element.get(relation.getAttributeValue("entry2"));
 		if(e1 != null && e2 != null) {
 			PathwayElement line = createPathwayLine(e1, e2);
 			pathway.add(line);
 			line.addComment(line.new Comment(
 					type,
 					COMMENT_SOURCE
 			));
 			Element subtype = relation.getChild("subtype");
 			if(subtype != null) {
 				mapRelationType(subtype, line);
 			}
 		} else {
 			Logger.log.warn("Invalid relation, missing connecting element for: " + relation);
 		}
 	}
 
 	private void mapRelationType(Element subtype, PathwayElement line) {
 		String name = subtype.getAttributeValue("name");
 		String value = subtype.getAttributeValue("value");
 		
 		if			("--|".equals(value)) {
 			line.setEndLineType(LineType.TBAR);
 		} else if	("-->".equals(value)) {
 			line.setEndLineType(LineType.ARROW);
 			line.setLineStyle(LineStyle.DASHED);
 		} else if	("..>".equals(value)) {
 			line.setEndLineType(LineType.ARROW);
 			line.setLineStyle(LineStyle.DASHED);
 		} else if	("...".equals(value)) {
 			line.setEndLineType(LineType.LINE);
 			line.setLineStyle(LineStyle.DASHED);
 		} else if	("---".equals(value)) {
 			line.setEndLineType(LineType.LINE);
 		} else if	("-+-".equals(value)) {
 			line.setEndLineType(LineType.LINE);
 		} else if	("+p".equals(value)) {
 			line.setEndLineType(LineType.ARROW);
 		} else if	("-p".equals(value)) {
 			line.setEndLineType(LineType.ARROW);
 		} else if	("+g".equals(value)) {
 			line.setEndLineType(LineType.ARROW);
 		} else if	("+u".equals(value)) {
 			line.setEndLineType(LineType.ARROW);
 		} else if	("+m".equals(value)) {
 			line.setEndLineType(LineType.ARROW);
 		}
 		
 		line.addComment(line.new Comment(name, COMMENT_SOURCE));
 	}
 	
 	public static Pathway readFromKegg(File file, Organism species) throws ConverterException {
 		try {
 			connectToKegg();
 		} catch (ServiceException e) {
 			throw new ConverterException(e);
 		}
 
 		//Read the kgml file
 		SAXBuilder builder = new SAXBuilder(false);
 		
 		//Set custom entity resolver to use local DTD
 		//instead of fetching the DTD from the Kegg server
 		//every time
 		builder.setEntityResolver(new EntityResolver() {
 			public InputSource resolveEntity(String publicId, String systemId) {
 				//See if we can find the DTD entry
 				int slash = systemId.lastIndexOf('/');
 				if(slash > -1) {
 					String dtd = systemId.substring(slash + 1);
 					Logger.log.trace("Found DTD filename: " + dtd);
 					File dtdFile = new File(dtd);
 					if(dtdFile.exists()) {
 						try {
 							Logger.log.trace("Using local DTD: " + dtdFile);
 							return new InputSource(new FileReader(dtdFile));
 						} catch (FileNotFoundException e) {
 							Logger.log.trace("Couldn't read DTD file");
 						}
 					}
 				}
 				return null;
 			}
 		});
 		Document doc;
 		try {
 			doc = builder.build(file);
 		} catch (JDOMException e) {
 			throw new ConverterException(e);
 		} catch (IOException e) {
 			throw new ConverterException(e);
 		}
 
 		Pathway pathway = new Pathway();
 		pathway.getMappInfo().setOrganism(species.latinName());
 		KeggFormat keggFormat = new KeggFormat(pathway);
 
 		try {
 			keggFormat.doMapping(doc, pathway);
 		} catch (RemoteException e) {
 			throw new ConverterException(e);
 		}
 
 		return pathway;
 	}
 
 	private static void connectToKegg() throws ServiceException {
 		//Setup a connection to KEGG
 		if(keggPortType == null) {
 			keggPortType = keggLocator.getKEGGPort();
 		}
 	}
 
 	private void mapEntry(Element entry) throws RemoteException, ConverterException {
 		// Start converting elements
 		String type = entry.getAttributeValue("type");
 		Element graphics = entry.getChild(ELM_GRAPHICS);
 
 		if(graphics != null) {
 			//types: map, enzyme, compound
 			PathwayElement elm = null;
 			
 			Logger.log.trace("mapping " + type);
 			if			(TYPE_ENZYME.equals(type)) {
 				elm = mapToDataNode(entry);
 			} else if	(TYPE_ORTHOLOG.equals(type)) {
 				elm = mapToDataNode(entry);
 			} else if 	(TYPE_MAP.equals(type)) {
 				elm = mapMap(entry);
 			} else if	(TYPE_COMPOUND.equals(type)) {
 				elm = mapCompound(entry);
 			} else if   (TYPE_GENE.equals(type)) {
 				elm = mapToDataNode(entry);
 			}
 			if(elm != null) {
 				id2element.put(entry.getAttributeValue("id"), elm);
 			}
 		} else {
 			Logger.log.info("Skipped " + entry + ", no graphics element");
 		}
 	}
 
 	private PathwayElement mapToDataNode(Element entry) throws RemoteException, ConverterException {
 		String type = entry.getAttributeValue("type");
 		
 		Element graphics = entry.getChild(ELM_GRAPHICS);
 		String label = graphics.getAttributeValue("name");
 		
 		String name = entry.getAttributeValue("name");
 		String[] ids = name.split(" ");
 	
 		Set<PathwayElement> pwElms = new HashSet<PathwayElement>();
 	
 		int moveDown = 0;
 		for(int i = 0; i < ids.length; i++) {
 			String id = ids[i];
 			String[] genes = getGenes(id, organism, type);
 	
 			Set<PathwayElement> geneElms = createGeneProducts(entry, genes);
 	
 			for(PathwayElement dn : geneElms) {
 				moveDown += dn.getMHeight();
 			}
 	
 			if(geneElms.size() == 0) {
 				PathwayElement pwElm = createDataNode(
 						graphics,
 						DataNodeType.GENEPRODUCT,
 						graphics.getAttributeValue("name"),
 						label,
 						DataSource.getByFullName("Kegg " + type)
 				);
 				geneElms.add(pwElm);
 				mapDataNodeGraphics(pwElm, graphics);
 				pwElm.setMCenterY(pwElm.getMCenterY() + moveDown);
 				moveDown += pwElm.getMHeight();
 	
 				pathway.add(pwElm);
 				geneElms.add(pwElm);
 	
 				getReactionDataNodes(entry).add(pwElm);
 			}
 			pwElms.addAll(geneElms);
 		}
 	
 		PathwayElement[] elmArray = pwElms.toArray(new PathwayElement[pwElms.size()]);
 		if(pwElms.size() > 1) {
 			PathwayElement group = createGroup(name, elmArray);
 			stackElements(pwElms);
 			addToReactions(entry, group);
 			return group;
 		} else {
 			addToReactions(entry, elmArray[0]);
 			return elmArray[0];
 		}
 	}
 
 	private void addToReactions(Element entry, PathwayElement pathwayElement) {
 		List<PathwayElement> reactionElements = getReactionDataNodes(entry);
 		reactionElements.add(pathwayElement);
 	}
 
 	private PathwayElement createGroup(String name, PathwayElement[] elements) {
 		PathwayElement group = PathwayElement.createPathwayElement(ObjectType.GROUP);
 		group.setTextLabel(name);
 		pathway.add(group);
 		String id = pathway.getUniqueGroupId();
 		group.setGroupId(id);
 		for(PathwayElement pe : elements) {
 			pe.setGroupRef(id);
 		}
 		return group;
 	}
 
 	private List<PathwayElement> getReactionDataNodes(Element entry) {
 		List<PathwayElement> genes = new ArrayList <PathwayElement>(); 
 
 		String reactionString = entry.getAttributeValue("reaction");
 		if(reactionString != null) {
 			String[] reactions = reactionString.split(" ");
 
 			for(String reaction : reactions) {
 				genes = reaction2element.get(reaction);
 				if(genes == null) {
 					reaction2element.put(reaction, genes = new ArrayList<PathwayElement>());
 				}
 				break; //Take the first reaction only
 			}
 		}
 		return genes;
 	}
 
 	private PathwayElement createDataNode(Element graphics, DataNodeType type, String label, String id, DataSource source) {
 		PathwayElement dn = PathwayElement.createPathwayElement(ObjectType.DATANODE);
 		dn.setDataSource(source);
 		if(id != null && id.length() < 50) dn.setGeneID(id);
 		dn.setDataNodeType(type);
 		dn.setTextLabel(label);
 
 		mapDataNodeGraphics(dn, graphics);
 		
 		if(type == DataNodeType.METABOLITE) {
 			dn.setColor(Color.BLUE);
 		}
 		return dn;
 	}
 
 	private void mapDataNodeGraphics(PathwayElement pwElm, Element graphics) {
 		mapGraphics(pwElm, graphics);
 		//Use default datanode width
 		pwElm.setInitialSize();
 	}
 	
 	private void mapGraphics(PathwayElement pwElm, Element graphics) {
 		// Convert a hexadecimal color into an awt.Color object
 		// Remove the # before converting
 		String colorStringGPML = graphics.getAttributeValue("fgcolor");
 		Color colorGPML;
 		if (colorStringGPML != null) {
			colorGPML = GpmlFormatImpl1.gmmlString2Color(colorStringGPML.substring(1));
 		} else {
 			colorGPML = Color.BLACK;
 		}
 		pwElm.setColor(colorGPML);
 
 		// Set x, y, width, height
 		String s_cx = graphics.getAttributeValue("x");
 		String s_cy = graphics.getAttributeValue("y");
 		String s_w = graphics.getAttributeValue("width");
 		String s_h = graphics.getAttributeValue("height");
 
 		double height = Double.parseDouble(s_h);
 		double width = Double.parseDouble(s_w);
 		pwElm.setMWidth(coordinateToGpml(width));
 		pwElm.setMHeight(coordinateToGpml(height));
 
 		double centerY = Double.parseDouble(s_cy);
 		double centerX = Double.parseDouble(s_cx);
 
 		pwElm.setMCenterX(coordinateSpacing(coordinateToGpml(centerX)));
 		pwElm.setMCenterY(coordinateSpacing(coordinateToGpml(centerY)));
 	}
 
 	private double coordinateToGpml(double c) {
 		return c * GpmlFormat.pixel2model;
 	}
 
 	private double coordinateSpacing(double c) {
 		return c * 2.5; //Make pathway 2.5 times larger, for better spacing
 	}
 
 	private PathwayElement mapCompound(Element compound) throws RemoteException, ConverterException {
 		Element graphics = compound.getChild(ELM_GRAPHICS);
 
 		String name = compound.getAttributeValue("name");
 		String label = graphics.getAttributeValue("name");
 		if(useWebservice) { //fetch the real name from the webservice
 			label = getKeggSymbol(name);
 		}
 		
 		PathwayElement pwElm = createDataNode(
 				graphics,
 				DataNodeType.METABOLITE,
 				label,
 				name, //TODO: metabolite annotation
 				DataSource.KEGG_COMPOUND
 		);
 
 		pathway.add(pwElm);
 		compound2element.put(name, pwElm);
 		return pwElm;
 	}
 
 	private void stackElements(Collection<PathwayElement> pwElms) {
 		PathwayElement[] elements = pwElms.toArray(new PathwayElement[pwElms.size()]);
 		PathwayElement center = elements[0];
 		Logger.log.trace("Center of stack: " + center.getTextLabel());
 		double currAbove = center.getMTop();
 		double currBelow = center.getMTop() + center.getMHeight();
 		for(int i = 1; i < pwElms.size(); i++) {
 			PathwayElement e = elements[i];
 
 			if(i % 2 == 0) { //Place below
 				e.setMTop(currBelow);
 				currBelow += e.getMHeight();
 			} else { //Place above
 				currAbove -= e.getMHeight();
 				e.setMTop(currAbove);
 			}
 		}
 	}
 
 	private Set<PathwayElement> createGeneProducts(Element entry, String[] genes) throws RemoteException, ConverterException {
 		Set<PathwayElement> pwElms = new HashSet<PathwayElement>();
 		Element graphics = entry.getChild(ELM_GRAPHICS);
 
 		//Process as genes if we could annotate the entry to gene ids
 		if (genes.length > 0) {
 			for(int i = 0; i < genes.length; i++) {
 				String gene = genes[i];
 				String geneName = graphics.getAttributeValue("name");
 				if(useWebservice) { //fetch the real name from the webservice
 					geneName = getKeggSymbol(getKeggOrganism(organism) + ":" + gene);
 				}
 
 				//Create gpml element 
 				PathwayElement pwElm = createDataNode(
 						graphics,
 						DataNodeType.GENEPRODUCT,
 						geneName,
 						gene,
 						DataSource.ENTREZ_GENE
 				);
 
 				mapDataNodeGraphics(pwElm, graphics);
 
 				//Add comments regarding the source on KEGG
 				String e_id = entry.getAttributeValue("id");
 				String e_type = entry.getAttributeValue("type");
 				String e_name = entry.getAttributeValue("name");
 				pwElm.addComment(pwElm.new Comment(
 						"Original kegg element: " + e_type + ";" + e_id + ";" + e_name,
 						COMMENT_SOURCE
 				));
 				pathway.add(pwElm);
 				pwElms.add(pwElm);
 			}
 		}
 
 		return pwElms;
 	}
 
 	private PathwayElement mapMap(Element map) {
 		String label = map.getAttributeValue("name"); 
 		PathwayElement link = PathwayElement.createPathwayElement(ObjectType.LABEL);
 		link.setOutline(OutlineType.ROUNDED_RECTANGLE);
 		link.addComment(link.new Comment(map.getAttributeValue("link"), COMMENT_SOURCE));
 		Element graphics = map.getChild(ELM_GRAPHICS);
 		if(graphics != null) {
 			String glabel = graphics.getAttributeValue("name");
 			if(glabel == null) glabel = label;
 			else label = glabel;
 			if(label.startsWith("TITLE:")) {
 				return null; //This is the title of this map, skip it
 			}
 			mapGraphics(link, graphics);
 		}
 	
 		link.setTextLabel(label);
 		pathway.add(link);
 		return link;
 	}
 
 	private void mapReaction(Element reaction) {
 		String name = reaction.getAttributeValue("name");
 
 		Logger.log.trace("reaction " + name + " found");
 
 		// Create a list of elements in relations with reaction
 		List<Element> reactionElements = reaction.getChildren();
 		List<PathwayElement> dataNodes = reaction2element.get(name);
 
 		PathwayElement substrate = null;
 		PathwayElement product = null;
 
 		for(Element element : reactionElements) {
 			String compoundName = element.getAttributeValue("name");
 			if (element.getName().equals("substrate")){
 				substrate = compound2element.get(compoundName);
 			} else {
 				product = compound2element.get(compoundName);
 			}
 		}
 		//Add a reaction anchor + line to the datanodes
 		if(substrate != null && product != null){
 			MLine line = createPathwayLine(substrate, product);								
 			pathway.add(line);
 			if(dataNodes != null && dataNodes.size() > 0) {
 				PathwayElement m = dataNodes.get(0);
 
 				Point sp = findBorders(substrate, m)[0];
 				Point ep = findBorders(m, product)[1];
 				line.getMStart().setRelativePosition(sp.x, sp.y);
 				line.getMEnd().setRelativePosition(ep.x, ep.y);
 				
 				//Set the waypoints
 				line.setConnectorType(ConnectorType.ELBOW);
 				line.getConnectorShape().recalculateShape(line);
 				
 				WayPoint[] wps = line.getConnectorShape().getWayPoints();
 				Segment[] sgs = line.getConnectorShape().getSegments();
 
 				//Find the main axis of the substrate/product line
 				int maxis = 0;
 				double angle = LinAlg.angle(
 					new Point(substrate.getMStart().toPoint2D()),
 					new Point(product.getMStart().toPoint2D())
 				);
 				if(angle < -Math.PI/4 && angle > -3*Math.PI/4) {
 					maxis = 1;
 				}
 				if(angle > Math.PI/4 && angle < 3*Math.PI/4) {
 					maxis = 1;
 				}
 				
 				//Find the first segment that equals the main axis,
 				//its waypoint will be moved to the mediator
 				WayPoint wpm = null;
 				for(int i = 1; i < sgs.length - 1; i++) {
 					int axis = 0;
 					if(sgs[i].getMStart().getX() == sgs[i].getMEnd().getX()) {
 						axis = 1;
 					}
 					if(maxis == axis) {
 						wpm = wps[i - 1];
 						break;
 					}
 				}
 
 				List<MPoint> mpoints = new ArrayList<MPoint>();
 				mpoints.add(line.getMStart());
 				for(WayPoint wp : wps) {
 					if(wpm != null && wp == wpm) {
 						mpoints.add(line.new MPoint(
 								m.getMCenterX(),
 								m.getMTop() + m.getMHeight() + 5 * 15
 						));
 					} else {
 						mpoints.add(line.new MPoint(wp.getX(), wp.getY()));
 					}
 				}
 				mpoints.add(line.getMEnd());
 				line.setMPoints(mpoints);
 				
 				for(PathwayElement mediator : dataNodes) {
 					addReactionMediator(line, mediator);
 				}
 			}
 			
 		} else {
 			Logger.log.error("No DataNodes to connect to for reaction " + name + " in " + reaction.getName());
 		}
 	}
 
 	private void addReactionMediator(PathwayElement reactionLine, PathwayElement mediator) {
 		//calculate anchor position
 		Point lstart = new Point(reactionLine.getMStartX(), reactionLine.getMStartY());
 		Point lend = new Point(reactionLine.getMEndX(), reactionLine.getMEndY());
 		Point m = new Point(mediator.getMCenterX(), mediator.getMCenterY());
 		double position = LinAlg.toLineCoordinates(lstart, lend, m);
 
 		//create an anchor on the reaction line
 		MAnchor anchor = reactionLine.addMAnchor(position);
 		
 		//draw a line from the bottom of the datanodes to the anchor
 		PathwayElement aline = PathwayElement.createPathwayElement(ObjectType.LINE);
 		pathway.add(aline);
 		aline.setStartGraphRef(getGraphId(mediator));
 		anchor.setGeneratedGraphId();
 		aline.setEndGraphRef(anchor.getGraphId());
 		aline.getMStart().setRelativePosition(0, -1);
 		aline.getMEnd().setRelativePosition(0, 0);
 	}
 	
 	private MLine createPathwayLine(PathwayElement start, PathwayElement end)
 	{
 
 		// Create new pathway line
 		MLine line = (MLine)PathwayElement.createPathwayElement(ObjectType.LINE);
 
 		line.setColor(Color.BLACK);
 
 		String startId = getGraphId(start);
 		
 		line.setStartGraphRef(startId);
 		line.setEndLineType(LineType.ARROW);
 		
 		if(start.getMCenterX() == end.getMCenterX() || 
 				start.getMCenterY() == end.getMCenterY()) {
 			line.setConnectorType(ConnectorType.STRAIGHT);
 		} else {
 			line.setConnectorType(ConnectorType.ELBOW);
 		}
 
 		//TK: Quick hack, GraphId is not automatically generated,
 		//so set one explicitly...FIXME!
 		String endId = getGraphId(end);
 		line.setEndGraphRef(endId);
 
 		Point[] pts = findBorders(start, end);
 		line.getMStart().setRelativePosition(pts[0].x, pts[0].y);
 		line.getMEnd().setRelativePosition(pts[1].x, pts[1].y);
 		
 		return line;
 	}
 
 	private Point[] findBorders(PathwayElement start, PathwayElement end) {
 		Point psource = new Point(start.getMCenterX(), start.getMCenterY());
 		Point ptarget = new Point(end.getMCenterX(), end.getMCenterY());
 		
 		double angle = LinAlg.angle(ptarget.subtract(psource), new Point(1, 0));
 		double astart = angle;
 		double aend = angle;
 		if(angle < 0) 	aend += Math.PI;
 		else 			aend -= Math.PI;
 		if(angle == 0) {
 			if(psource.x > ptarget.x) {
 				aend += Math.PI;
 				astart += Math.PI;
 			}
 		}
 		Point pstart = findBorder(start, astart);
 		Point pend = findBorder(end, aend);
 		return new Point[] { pstart, pend };
 	}
 	
 	private String getGraphId(PathwayElement pwElm) {
 		//TK: Quick hack, GraphId is not automatically generated,
 		//so set one explicitly...FIXME!
 		String id = pwElm.getGraphId();
 		if(id == null) {
 			pwElm.setGraphId(id = pwElm.getParent().getUniqueGraphId());
 		}
 		return id;
 	}
 
 //	private int getDirection(Point2D start, Point2D end) {
 //		
 //	}
 //	
 //	static final int AXIS_X = 0;
 //	static final int AXIS_Y = 1;
 //	
 //	private int getAxis(Point2D start, Point2D end) {
 //		double angle = LinAlg.angle(start, end);
 //		if(angle <= Math.PI / 4) {
 //			
 //		}
 //		if(angle )
 //			LinAlg.direction(p1, p2)
 //	}
 	
 	/**
 	 * Find the border to connect to. Returns a point containing
 	 * the relative coordinates.
 	 */
 	private Point findBorder(PathwayElement pwElm, double angle) {
 		Point bp = new Point(-1, -1);
 
 		double diagAngle = Math.atan(pwElm.getMHeight() / pwElm.getMWidth());
 		double angleA = Math.abs(angle);
 		/*    da < |a| < da + pi/2
 		       \   /
 		        \ /
 |a| > da + pi/2	 \  |a| < da 
 		        / \
 		       /   \
 		         da < |a| < da + pi/2
 		 */
 
 		if(angleA >= diagAngle && angleA <= diagAngle + Math.PI/2) {
 			bp.x = 0; //center
 			if(angle < 0) {
 				bp.y += 2;
 			}
 		}
 		if(angleA < diagAngle || angleA > diagAngle + Math.PI/2) {
 			bp.y = 0;
 			if(angle < Math.PI / 2 && angle > -Math.PI / 2) {
 				bp.x += 2;
 			}
 		}
 
 		return bp;
 	}
 
 	private String[] getGenes(String keggId, Organism organism, String type) throws RemoteException, ConverterException {
 		if(useWebservice && !TYPE_GENE.equals(type)) {
 			if(TYPE_ORTHOLOG.equals(type)) {
 				return getGenesForKo(keggId, organism);
 			} else {
 				return getGenesForEc(keggId, organism);
 			}
 		} else {
 			//Assumes that if it's an annotated gene:
 			//a gene is of the form hsa:1234, where 1234 is the Entrez Gene id and hsa is the organism code
 			if(keggId.startsWith(getKeggOrganism(organism))) {
 				keggId = keggId.substring(4);
 				Logger.log.trace("Gene: " + keggId);
 			}
 			return new String[] { keggId };
 		}
 	}
 	
 	/**
 	 * Fetches the organism specific NCBI gene identifiers for the enzyme code
 	 * @throws ConverterException 
 	 * @throws RemoteException 
 	 */
 	private String[] getGenesForEc(String ec, Organism organism) throws RemoteException, ConverterException {
 		Set<String> genes = new HashSet<String>();
 
 		//Fetch the kegg gene IDs
 		String[] keggGenes = keggPortType.get_genes_by_enzyme(ec, getKeggOrganism(organism));
 		if(keggGenes != null) {
 			for(String kg : keggGenes) {
 				//KEGG code --> NCBI code
 				LinkDBRelation[] links = keggPortType.get_linkdb_by_entry(kg, "NCBI-GeneID", 1, 1000);
 				for(LinkDBRelation ldb : links) {
 					genes.add(ldb.getEntry_id2().substring(12));
 				}
 			}
 		}
 		return genes.toArray(new String[genes.size()]);  
 	}
 
 	private String[] getGenesForKo(String ko, Organism organism) throws RemoteException, ConverterException {
 		Set<String> genes = new HashSet<String>();
 
 		Definition[] keggGenes = keggPortType.get_genes_by_ko(ko, getKeggOrganism(organism));
 		if(keggGenes != null) {
 			for(Definition def : keggGenes) {
 				LinkDBRelation[] links = keggPortType.get_linkdb_by_entry(
 						def.getEntry_id(), "NCBI-GeneID", 1, 1000
 				);
 				for(LinkDBRelation ldb : links) {
 					genes.add(ldb.getEntry_id2().substring(12));
 				}
 			}
 		}
 
 		return genes.toArray(new String[genes.size()]);
 	}
 
 	private String getKeggSymbol(String geneId) throws RemoteException, ConverterException {
 		String result = keggPortType.btit(geneId);
 		String[] data = result.split(" ");
 		if(data.length > 1) {
 			result = data[1].substring(0, data[1].length()-1);
 		} else {
 			result = geneId;
 		}
 		return result;
 	}
 
 	private String getKeggOrganism(Organism organism) throws ConverterException {
 		switch(organism) {
 		case HomoSapiens:
 			return "hsa";
 		case RattusNorvegicus:
 			return "rno";
 		case MusMusculus:
 			return "mmu";
 		case SaccharomycesCerevisiae:
 			return "sce";
 		default:
 			throw new ConverterException("No KEGG code for organism " + organism);
 		}
 	}
 
 }
