 /*
 * $Id: XmlHandler.java,v 1.20 2005/05/27 20:26:28 andrew_avis Exp $
  * Created on Oct 14, 2004
  * 
  * This class is the "content handler" for xml input.
  * Each start and end of an element, document, content, etc. is
  * captured by the event handlers, and we use them to "build" a
  * recipe.
  */
 package strangebrew;
 
 import org.xml.sax.Attributes;
 import org.xml.sax.Locator;
 import org.xml.sax.SAXException;
 import org.xml.sax.SAXParseException;
 import org.xml.sax.helpers.DefaultHandler;
 
 /**
  * @author aavis
  *
  * This class handles XML import.
  * Currently it handles StrangeBrew 1.8 and QBrew formats.
  * TODO: handle misc ingredients
  * TODO: handle mash info
  */
 public class XmlHandler extends DefaultHandler{
 	private Recipe r = null;
 	private Fermentable m = null; 
 	private Hop h = null;
 	private Misc misc = null;
 	private Attributes currentAttributes = null;
 
 	private String currentList = null; //current List name
 	private String currentElement = null; // current element name
 	private String importType = null; // the type of recipe we're importing
	private String descrBuf = ""; // buffer to hold long descriptions
 	
 	// mash step stuff:
 	private String type;
 	private String method = "infusion";
 	private double startTemp;
 	private double endTemp;
 	private int minutes;
 	private int rampMin;
 
 	public Recipe getRecipe() {return r;}
 	
 	//===========================================================
 	// SAX DocumentHandler methods
 	//===========================================================
 
 	public void setDocumentLocator(Locator l) {
 		// Save this to resolve relative URIs or to give diagnostics.
 			System.out.print("LOCATOR");
 			System.out.print("\n SYS ID: " + l.getSystemId());
 			System.out.flush();
 
 	}
 
 	// we don't do anything with the start of a document
 	public void startDocument() throws SAXException {
 	}
 
 	// this is debug stuff, delete later
 	public void endDocument() throws SAXException {
 		// emit(r.toXML());
 		// r.testRecipe();
 		nl();
 		System.out.flush();
 	}
 
 	/**
 	 * This method is called every time we encounter a new element
 	 * To handle other xml import types, we check for 
 	 * a "signpost" element that indicates the file type, then
 	 * call out to the appropriate element handler for that file type.
 	 * 
 	 */
 	public void startElement(String namespaceURI, String lName, // local unit
 			String qName, // qualified unit
 			Attributes attrs) throws SAXException {
 		String eName = lName; // element unit
 
 		if ("".equals(eName))
 			eName = qName; // namespaceAware = false
 		
 		currentElement = eName;
 		currentAttributes = attrs;
 
 		if (eName.equalsIgnoreCase("STRANGEBREWRECIPE")) {
 			importType = "STRANGEBREW";
 			r = new Recipe();
 			emit("StrangeBrew recipe detected.");
 		} else if (eName.equalsIgnoreCase("RECIPE")) {
 			if (attrs != null) {
 				for (int i = 0; i < attrs.getLength(); i++) {
 					String s = attrs.getLocalName(i); // Attr name
 					if ("".equalsIgnoreCase(s))
 						s = attrs.getQName(i);
 					if (s.equalsIgnoreCase("generator")&& 
 							"qbrew".equalsIgnoreCase(attrs.getValue(i))){
 						importType = "QBREW";
 						r = new Recipe();
 						emit("QBrew recipe detected.");
 					}						
 				}
 			}
 
 		} else if (importType == "STRANGEBREW") {
 			// call SB start element
 			sbStartElement(eName);
 		} else if (importType == "QBREW") {
 			// call SB start element
 			qbStartElement(eName);
 		}
 
 	}
 	
 	void qbStartElement(String eName){
 		if (eName.equalsIgnoreCase("HOPS")) {
 			currentList = "HOPS";
 		}
 		else if (eName.equalsIgnoreCase("GRAINS")) {
 			currentList = "GRAINS";
 		}
 		else if (eName.equalsIgnoreCase("HOP")) {
 			// new hop
 			h = new Hop();
 			for (int i = 0; i < currentAttributes.getLength(); i++) {
 				String str = currentAttributes.getLocalName(i); // Attr name
 				if ("".equalsIgnoreCase(str))
 					str = currentAttributes.getQName(i);
 				if (str.equalsIgnoreCase("quantity"))
 					h.setAmountAndUnits(currentAttributes.getValue(i));
 				else if (str.equalsIgnoreCase("time"))
 					h.setMinutes(Integer.parseInt(currentAttributes.getValue(i)));
 				else if (str.equalsIgnoreCase("alpha"))
 					h.setAlpha(Double.parseDouble(currentAttributes.getValue(i)));
 			}
 		}
 		else if (eName.equalsIgnoreCase("GRAIN")) {
 			// new malt
 			m = new Fermentable();
 			for (int i = 0; i < currentAttributes.getLength(); i++) {
 				String str = currentAttributes.getLocalName(i); // Attr name
 				if ("".equalsIgnoreCase(str))
 					str = currentAttributes.getQName(i);
 				if (str.equalsIgnoreCase("quantity"))
 					m.setAmountAndUnits(currentAttributes.getValue(i));
 				else if (str.equalsIgnoreCase("color"))
 					m.setLov(Double.parseDouble(currentAttributes.getValue(i)));
 				else if (str.equalsIgnoreCase("extract"))
 					m.setPppg(Double.parseDouble(currentAttributes.getValue(i)));						
 			}
 		}
 		// we have to do this here, because <batch> is an empty element
 		else if (eName.equalsIgnoreCase("batch")){
 			for (int i = 0; i < currentAttributes.getLength(); i++) {
 				String str = currentAttributes.getLocalName(i); // Attr name
 				if ("".equalsIgnoreCase(str))
 					str = currentAttributes.getQName(i);
 				if (str.equalsIgnoreCase("quantity"))
 					r.setAmountAndUnits(currentAttributes.getValue(i));
 			}
 		}
 		// TODO: handle new misc ingredient
 	}
 	
 	/**
 	 * Start of an element handler when we know this is a StrangeBrew recipe
 	 * 
 	 * @param eName
 	 */
 	void sbStartElement(String eName) {
 		if (eName.equalsIgnoreCase("DETAILS")) {
 			currentList = "DETAILS";
 		} else if (eName.equalsIgnoreCase("FERMENTABLES")) {
 			currentList = "FERMENTABLES";
 		} else if (eName.equalsIgnoreCase("HOPS")) {
 			currentList = "HOPS";
 		} else if (eName.equalsIgnoreCase("MASH") && currentList.equals("")) {
 			currentList = "MASH";
 		} else if (eName.equalsIgnoreCase("MISC")) {
 			currentList = "MISC";
 		} else if (eName.equalsIgnoreCase("ITEM")) { // this is an item in a
 			// list
 			if (currentList.equals("FERMENTABLES")) {
 				m = new Fermentable();
 			} else if (currentList.equals("HOPS")) {
 				h = new Hop();
 			} else if (currentList.equals("MISC")) {
 				misc = new Misc();
 			}
 
 
 		} 
 	}
 
 	/**
 	 * At the end of each element, we should check if we're looking at a list.
 	 * If we are, set the list to null. This way, we can tell if we're looking
 	 * at an element that has (stupidly) the same unit as a list... eg <MASH>is
 	 * in the recipe (indicating whether it's mashed or not), and the unit of
 	 * the mash list!
 	 */
 	public void endElement(String namespaceURI, String sName, // simple name
 			String qName // qualified name
 	) throws SAXException {
 		if (importType == "STRANGEBREW") {
 			if (qName.equalsIgnoreCase("ITEM")
 					&& currentList.equalsIgnoreCase("FERMENTABLES")) {
 				r.addMalt(m);
 				m = null;
 			} else if (qName.equalsIgnoreCase("ITEM")
 					&& currentList.equalsIgnoreCase("HOPS")) {
				h.setDescription(descrBuf);
				descrBuf = "";
 				r.addHop(h);
 				h = null;
 			} else if (qName.equalsIgnoreCase("ITEM")
 					&& currentList.equalsIgnoreCase("MISC")) {
 				r.addMisc(misc);
 				misc = null;
 			
 			} else if (qName.equalsIgnoreCase("ITEM")
 						&& currentList.equalsIgnoreCase("MASH")) {
 					r.mash.addStep(type, startTemp, endTemp, "F", method, minutes, rampMin);
 									
 				
 			} else if (qName.equalsIgnoreCase("FERMENTABLS")
 					|| qName.equalsIgnoreCase("HOPS")
 					|| qName.equalsIgnoreCase("DETAILS")
 					|| qName.equalsIgnoreCase("MISC")) {
 				currentList = "";
 			}
 		}
 		else if (importType == "QBREW"){
 			if (qName.equalsIgnoreCase("GRAIN")){
 				r.addMalt(m);
 				m = null;
 			} 
 			else if (qName.equalsIgnoreCase("HOP")){
 				r.addHop(h);
 				h = null;
 			} 
 		}
 	}
 
 	/**
 	 * This is the "meat" of the handler.  We figure out where in the
 	 * document we are, based on the current list, and then start puting
 	 * the data into the recipe fields.
 	 */
 	public void characters(char buf[], int offset, int len) throws SAXException {
 		String s = new String(buf, offset, len);
 
 		if (!s.trim().equals("") && importType == "STRANGEBREW") {
 			sbCharacters(s.trim());	
 		} 
 		else if (!s.trim().equals("") && importType == "QBREW") {
 				qbCharacters(s.trim());	
 			} 
 	}
 	
 	void qbCharacters(String s){
 		if (currentElement.equalsIgnoreCase("GRAIN")){
 			m.setName(s);
 			
 		}
 		else if (currentElement.equalsIgnoreCase("HOP")){
 			h.setName(s);
 
 		}
 		else if (currentElement.equalsIgnoreCase("miscingredient")){
 			r.setYeastName(s);
 			// TODO: there is more data in the yeast record, and there may be other misc ingredients
 			// figure out how to get the yeast, and parse the other data.
 		}
 		else if (currentElement.equalsIgnoreCase("title")){
 			r.setName(s);
 		}
 		else if (currentElement.equalsIgnoreCase("brewer")){
 			r.setBrewer(s);
 		}
 		else if (currentElement.equalsIgnoreCase("style")){
 			r.setStyle(s);
 		}		
 	}	
 	
 	void sbCharacters(String s){
 		if (currentList.equals("FERMENTABLES")) {
 			if (currentElement.equalsIgnoreCase("MALT")) {
 				m.setName(s);
 			} else if (currentElement.equalsIgnoreCase("AMOUNT")) {
 				m.setAmount(Double.parseDouble(s));
 			} else if (currentElement.equalsIgnoreCase("POINTS")) {
 				m.setPppg(Double.parseDouble(s));
 			} else if (currentElement.equalsIgnoreCase("COSTLB")) {
 				m.setCost( s );
 			} else if (currentElement.equalsIgnoreCase("UNITS")) {
 				m.setUnits(s);
 			} else if (currentElement.equalsIgnoreCase("LOV")) {
 				m.setLov(Double.parseDouble(s));
 			} else if (currentElement.equalsIgnoreCase("DescrLookup")) {
 				m.setDescription(s);
 			}
 		}
 		else if (currentList.equalsIgnoreCase("HOPS")) {
 			if (currentElement.equalsIgnoreCase("HOP")) {
 				h.setName(s);
 			} else if (currentElement.equalsIgnoreCase("AMOUNT")) {
 				h.setAmount(Double.parseDouble(s));
 			} else if (currentElement.equalsIgnoreCase("ALPHA")) {
 				h.setAlpha(Double.parseDouble(s));
 			} else if (currentElement.equalsIgnoreCase("UNITS")) {
 				h.setUnits(s);
 			} else if (currentElement.equalsIgnoreCase("FORM")) {
 				h.setType(s);
 			} else if (currentElement.equalsIgnoreCase("COSTOZ")) {
 				h.setCost( s );
 			} else if (currentElement.equalsIgnoreCase("ADD")) {
 				h.setAdd(s);
 			} else if (currentElement.equalsIgnoreCase("DescrLookup")) {
				descrBuf = descrBuf + s;
 			} else if (currentElement.equalsIgnoreCase("TIME")) {
 				h.setMinutes(Integer.parseInt(s));
 			}
 		}
 		
 		else if (currentList.equalsIgnoreCase("MISC")) {
 			if (currentElement.equalsIgnoreCase("NAME")) {
 				misc.setName(s);
 			} else if (currentElement.equalsIgnoreCase("AMOUNT")) {
 				misc.setAmount(Double.parseDouble(s));
 			} else if (currentElement.equalsIgnoreCase("UNITS")) {
 				misc.setUnits(s);
 			} else if (currentElement.equalsIgnoreCase("COMMENTS")) {
 				misc.setComments(s);
 			} else if (currentElement.equalsIgnoreCase("COST_PER_U")) {
 				// misc.setCost( Double.parseDouble(s) );
 			} else if (currentElement.equalsIgnoreCase("ADD")) {
 				h.setAdd(s);
 			} else if (currentElement.equalsIgnoreCase("DescrLookup")) {
 				misc.setDescription(s);
 			} else if (currentElement.equalsIgnoreCase("TIME")) {
 				misc.setTime(Integer.parseInt(s));
 			} else if (currentElement.equalsIgnoreCase("STAGE")) {
 				misc.setStage(s);
 			}
 		}
 		else if (currentList.equalsIgnoreCase("MASH")) {
 			if (currentElement.equalsIgnoreCase("TYPE")) {
 				type = s;
 			} else if (currentElement.equalsIgnoreCase("TEMP")) {
 				startTemp = Double.parseDouble(s);
 			} else if (currentElement.equalsIgnoreCase("METHOD")) {
 				method = s;
 			} else if (currentElement.equalsIgnoreCase("MIN")) {
 				minutes = Integer.parseInt(s);
 			} else if (currentElement.equalsIgnoreCase("END_TEMP")) {
 				endTemp = Double.parseDouble(s);
 			} else if (currentElement.equalsIgnoreCase("RAMP_MIN")) {
 				rampMin = Integer.parseInt(s);
 			}
 		}
 
 		else if (currentList.equalsIgnoreCase("DETAILS")) {
 			if (currentElement.equalsIgnoreCase("NAME")) {
 				r.setName(s);
 			} else if (currentElement.equalsIgnoreCase("EFFICIENCY")) {
 				r.setEfficiency(Double.parseDouble(s));
 			} else if (currentElement.equalsIgnoreCase("ATTENUATION")) {
 				r.setAttenuation(Double.parseDouble(s));
 			} else if (currentElement.equalsIgnoreCase("PRESIZE")) {
 				r.setPreBoil(Double.parseDouble(s));
 			} else if (currentElement.equalsIgnoreCase("SIZE")) {
 				r.setPostBoil(Double.parseDouble(s));
 			} else if (currentElement.equalsIgnoreCase("SIZE_UNITS")) {
 				r.setPreBoilVolUnits(s);
 				r.setPostBoilVolUnits(s);
 			} else if (currentElement.equalsIgnoreCase("STYLE")) {
 				r.setStyle(s);
 			} else if (currentElement.equalsIgnoreCase("BOIL_TIME")) {
 				r.setBoilMinutes(Integer.parseInt(s));
 			} else if (currentElement.equalsIgnoreCase("HOPS_UNITS")) {
 				r.setHopsUnits(s);
 			} else if (currentElement.equalsIgnoreCase("MALT_UNITS")) {
 				r.setMaltUnits(s);
 			} else if (currentElement.equalsIgnoreCase("MASH_RATIO")) {
 				r.setMashRatio(Double.parseDouble(s));
 			} else if (currentElement.equalsIgnoreCase("MASH_RATIO_U")) {
 				r.setMashRatioU(s);
 			} else if (currentElement.equalsIgnoreCase("BREWER")) {
 				r.setBrewer(s);
 			} else if (currentElement.equalsIgnoreCase("MASH")) {
 				r.setMashed(Boolean.valueOf(s).booleanValue());
 			} else if (currentElement.equalsIgnoreCase("YEAST")) {
 				r.setYeastName(s);
 			}
 
 		}
 		else
 			s = "";
 	}
 
 	public void ignorableWhitespace(char buf[], int offset, int len)
 			throws SAXException {
 		nl();
 		emit("IGNORABLE");
 	}
 
 	public void processingInstruction(String target, String data)
 			throws SAXException {
 		nl();
 		emit("PROCESS: ");
 		emit("<?" + target + " " + data + "?>");
 	}
 
 	//===========================================================
 	// SAX ErrorHandler methods
 	//===========================================================
 
 	// treat validation errors as fatal
 	public void error(SAXParseException e) throws SAXParseException {
 		throw e;
 	}
 
 	// dump warnings too
 	public void warning(SAXParseException err) throws SAXParseException {
 		System.out.println("** Warning" + ", line " + err.getLineNumber()
 				+ ", uri " + err.getSystemId());
 		System.out.println("   " + err.getMessage());
 	}
 
 	//===========================================================
 	// Utility Methods ...
 	//===========================================================
 
 	// Wrap I/O exceptions in SAX exceptions, to
 	// suit handler signature requirements
 	private void emit(String s) throws SAXException {
 			System.out.print(s);
 			System.out.flush();
 	}
 
 	// Start a new line
 	// and indent the next line appropriately
 	private void nl() throws SAXException {
 		String indentString = "    "; // Amount to indent
 		int indentLevel = 0;
 		String lineEnd = System.getProperty("line.separator");
 		System.out.print(lineEnd);
 		for (int i = 0; i < indentLevel; i++)
 			System.out.print(indentString);
 
 	}
 
 }
