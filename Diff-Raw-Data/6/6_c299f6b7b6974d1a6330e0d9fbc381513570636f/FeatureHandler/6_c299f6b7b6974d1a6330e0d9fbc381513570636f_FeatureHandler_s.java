 /*
  *                    BioJava development code
  *
  * This code may be freely distributed and modified under the
  * terms of the GNU Lesser General Public Licence.  This should
  * be distributed with the code.  If you do not have a copy,
  * see:
  *
  *      http://www.gnu.org/copyleft/lesser.html
  *
  * Copyright for this code is held jointly by the individual
  * authors.  These should be listed in @author doc comments.
  *
  * For more information on the BioJava project and its aims,
  * or to join the biojava-l mailing list, visit the home page
  * at:
  *
  *      http://www.biojava.org/
  *
  */
 
 package org.biojava.bio.program.xff;
 
 import java.util.*;
 
 import org.biojava.bio.*;
 import org.biojava.bio.seq.*;
 import org.biojava.bio.seq.io.*;
 import org.biojava.bio.symbol.*;
 
 import org.biojava.utils.*;
 import org.biojava.utils.stax.*;
 import org.xml.sax.*;
 
 /**
  * StAX handler for the basic <code>feature</code> type of XFF.
  * This class can also be subclassed to handle other XFF types.
  *
  * @author Thomas Down
  * @since 1.2
  */
 
 public class FeatureHandler extends StAXContentHandlerBase {
     public static final XFFPartHandlerFactory FEATURE_HANDLER_FACTORY = new XFFPartHandlerFactory() {
 	    public StAXContentHandler getPartHandler(XFFFeatureSetHandler xffenv) {
 		return new FeatureHandler(xffenv);
 	    }
 	} ;
 
     private XFFFeatureSetHandler xffenv;
     private Feature.Template template = null;
     private boolean startFired = false;
     private boolean endFired = false;
     private int level = 0;
 
     /**
      * Construct a new Feature handler, passing in an XFF-parsing environment.
      */
 
     public FeatureHandler(XFFFeatureSetHandler xffenv) {
 	this.xffenv = xffenv;
     }
 
     /**
      * Return the XFF processing environment passed in when this handler was
      * created.
      */
 
     public XFFFeatureSetHandler getXFFEnvironment() {
 	return xffenv;
     }
 
     /**
      * Get the template for the feature being constructed.  This should
      * usually not be overridden.  Delegates to <code>createFeatureTemplate</code>
      * for template construction.
      */
 
     protected Feature.Template getFeatureTemplate() {
 	if (template == null) {
 	    template = createFeatureTemplate();
 	}
 	return template;
     }
 
     /**
      * Create a new template of the appropriate type.  Override this method
      * if you wish to use a template type other than Feature.Template.
      */
 
     protected Feature.Template createFeatureTemplate() {
 	return new Feature.Template();
     }
 
     /**
      * Fire the startFeature event.
      */
 
     protected void fireStartFeature() 
         throws ParseException
     {
 	if (startFired) {
 	    throw new ParseException("startFeature event has already been fired for this feature");
 	}
 
	getXFFEnvironment().getFeatureListener().startFeature(getFeatureTemplate());
 	startFired = true;
     }
 
     /**
      * Fire the endFeature event.
      */
 
     protected void fireEndFeature() 
         throws ParseException
     {
 	if (!startFired) {
 	    throw new ParseException("startFeature has not yet been fired for this feature.");
 	}
 
 	if (endFired) {
 	    throw new ParseException("endFeature event has already been fired for this feature");
 	}
 
 	getXFFEnvironment().getFeatureListener().endFeature();
 	endFired = true;
     }
 
     /**
      * Set a property.
      */
 
     protected void setFeatureProperty(Object key, Object value) 
         throws ChangeVetoException, ParseException
     {
 	if (startFired) {
 	    getXFFEnvironment().getFeatureListener().addFeatureProperty(key, value);
 	} else {
 	    Feature.Template ft = getFeatureTemplate();
 	    if (ft.annotation == null) {
 		ft.annotation = new SimpleAnnotation();
 	    }
 	    ft.annotation.setProperty(key, value);
 	}
     }
     
     
     public void startElement(String nsURI,
 			     String localName,
 			     String qName,
 			     Attributes attrs,
 			     DelegationManager dm)
 	 throws SAXException
     {
 	level++;
 	if (level == 1) {
 	    // This was our initial startElement.
 	    String id = attrs.getValue("id");
 	    if (id != null) {
 		try {
 		    setFeatureProperty(XFFFeatureSetHandler.PROPERTY_XFF_ID, id);
 		} catch (Exception ex) {
 		    throw new SAXException("Couldn't set property", ex);
 		}
 	    }
 	} else {
 	    if (localName.equals("type")) {
 		dm.delegate(getTypeHandler());
 	    } else if (localName.equals("source")) {
 		dm.delegate(getSourceHandler());
 	    } else if (localName.equals("location")) {
 		dm.delegate(getLocationHandler());
 	    } else if (localName.equals("id")) {
 		dm.delegate(getOldIDHandler());
 	    } else if (localName.equals("details")) {
 		if (!startFired) {
 		    try {
 			fireStartFeature();
 		    } catch (ParseException ex) {
 			throw new SAXException(ex);
 		    }
 		}
 
 		dm.delegate(xffenv.getDetailsHandler());
 
 		// Need to handle details properly
 	    } else if (localName.equals("featureSet")) {
 		if (!startFired) {
 		    try {
 			fireStartFeature();
 		    } catch (ParseException ex) {
 			throw new SAXException(ex);
 		    }
 		}
 
 		dm.delegate(xffenv);
 	    } else {
 		// throw new SAXException("Couldn't recognize element " + localName + " in namespace " + nsURI); 
 	    }
 	}
     }
 
     public void endElement(String nsURI,
 			   String localName,
 			   String qName)
 	throws SAXException
     {
 	level--;
 
 	if (level == 0) {
 	    // Our tree is done.
 
 	    try {
 		if (!startFired) {
 		    fireStartFeature();
 		}
 		
 		if (!endFired) {
 		    fireEndFeature();
 		}
 	    } catch (ParseException ex) {
 		throw new SAXException(ex);
 	    }
 	}
     }
 
     protected StAXContentHandler getTypeHandler() {
 	return new StringElementHandlerBase() {
 		protected void setStringValue(String s) {
 		    getFeatureTemplate().type = s;
 		}
 	    } ;
     }
 
     protected StAXContentHandler getSourceHandler() {
 	return new StringElementHandlerBase() {
 		protected void setStringValue(String s) {
 		    getFeatureTemplate().source = s;
 		}
 	    } ;
     }
 
     protected StAXContentHandler getOldIDHandler() {
 	return new StringElementHandlerBase() {
 		protected void setStringValue(String s) 
 		    throws SAXException
 		{
 		    try {
 			setFeatureProperty(XFFFeatureSetHandler.PROPERTY_XFF_ID, s);
 		    } catch (Exception ex) {
 			throw new SAXException("Couldn't set property", ex);
 		    }
 		}
 	    } ;
     }
 
     protected StAXContentHandler getLocationHandler() {
 	return new LocationHandlerBase() {
 		protected void setLocationValue(Location l) {
 		    getFeatureTemplate().location = l;
 		}
 	    } ;
     }
 }
