 /**
  * Copyright 2005-2009 Noelios Technologies.
  * 
  * The contents of this file are subject to the terms of one of the following
  * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
  * "Licenses"). You can select the license that you prefer but you may not use
  * this file except in compliance with one of these Licenses.
  * 
  * You can obtain a copy of the LGPL 3.0 license at
  * http://www.opensource.org/licenses/lgpl-3.0.html
  * 
  * You can obtain a copy of the LGPL 2.1 license at
  * http://www.opensource.org/licenses/lgpl-2.1.php
  * 
  * You can obtain a copy of the CDDL 1.0 license at
  * http://www.opensource.org/licenses/cddl1.php
  * 
  * You can obtain a copy of the EPL 1.0 license at
  * http://www.opensource.org/licenses/eclipse-1.0.php
  * 
  * See the Licenses for the specific language governing permissions and
  * limitations under the Licenses.
  * 
  * Alternatively, you can obtain a royalty free commercial license with less
  * limitations, transferable or non-transferable, directly at
  * http://www.noelios.com/products/restlet-engine
  * 
  * Restlet is a registered trademark of Noelios Technologies.
  */
 
 package org.restlet.data;
 
 import java.io.IOException;
 import java.util.Map;
 import java.util.TreeMap;
 import java.util.logging.Level;
 
 import org.restlet.Context;
 import org.restlet.representation.Representation;
 import org.restlet.representation.StringRepresentation;
 
 /**
  * Generic message exchanged between client and server connectors.
  * 
  * @author Jerome Louvel
  */
 public abstract class Message {
     /** The modifiable attributes map. */
     private volatile Map<String, Object> attributes;
 
     /** The payload of the message. */
     private volatile Representation entity;
 
     // [ifdef gwt] member uncomment
     // /** The optional cached JSON representation. */
     // private org.restlet.representation.JsonRepresentation jsonRepresentation;
 
     // [ifdef gwt] member uncomment
     // /** The optional cached XML representation. */
     // private org.restlet.representation.XmlRepresentation xmlRepresentation;
 
     /** The optional cached Form. */
     private volatile Form entityForm;
 
     /** The optional cached text. */
     private volatile String entityText;
 
     /**
      * Constructor.
      */
     public Message() {
         this((Representation) null);
     }
 
     /**
      * Constructor.
      * 
      * @param entity
      *            The payload of the message.
      */
     public Message(Representation entity) {
         this.attributes = null;
         this.entity = entity;
         this.entityForm = null;
         this.entityText = null;
     }
 
     /**
      * Returns the modifiable map of attributes that can be used by developers
      * to save information relative to the message. Creates a new instance if no
      * one has been set. This is an easier alternative to the creation of a
      * wrapper instance around the whole message.<br>
      * <br>
      * 
      * In addition, this map is a shared space between the developer and the
      * connectors. In this case, it is used to exchange information that is not
      * uniform across all protocols and couldn't therefore be directly included
      * in the API. For this purpose, all attribute names starting with
      * "org.restlet" are reserved. Currently the following attributes are used:
      * <table>
      * <tr>
      * <th>Attribute name</th>
      * <th>Class name</th>
      * <th>Description</th>
      * </tr>
      * <tr>
      * <td>org.restlet.http.headers</td>
      * <td>org.restlet.data.Form</td>
      * <td>Server HTTP connectors must provide all request headers and client
      * HTTP connectors must provide all response headers, exactly as they were
      * received. In addition, developers can also use this attribute to specify
      * <b>non-standard</b> headers that should be added to the request or to the
      * response.</td>
      * </tr>
      * <tr>
      * <td>org.restlet.https.clientCertificates</td>
      * <td>List<java.security.cert.Certificate></td>
      * <td>For requests received via a secure connector, indicates the ordered
      * list of client certificates, if they are available and accessible.</td>
      * </tr>
      * </table>
      * <br>
      * Most of the standard HTTP headers are directly supported via the Restlet
      * API. Thus, adding such HTTP headers is forbidden because it could
      * conflict with the connector's internal behavior, limit portability or
      * prevent future optimizations. The other standard HTTP headers (that are
      * not supported) can be added as attributes via the
      * "org.restlet.http.headers" key.<br>
      * 
      * @return The modifiable attributes map.
      */
     public Map<String, Object> getAttributes() {
         if (this.attributes == null) {
             this.attributes = new TreeMap<String, Object>();
         }
 
         return this.attributes;
     }
 
     /**
      * Returns the entity representation.
      * 
      * @return The entity representation.
      */
     public Representation getEntity() {
         return this.entity;
     }
 
     /**
      * Returns the entity as a form.<br>
      * This method can be called several times and will always return the same
      * form instance. Note that if the entity is large this method can result in
      * important memory consumption.
      * 
      * @return The entity as a form.
      */
     public Form getEntityAsForm() {
         if (this.entityForm == null) {
             this.entityForm = new Form(getEntity());
         }
 
         return this.entityForm;
     }
 
     // [ifdef gwt] method uncomment
     // /**
     // * Returns the entity as a JSON representation.<br>
     // * This method can be called several times and will always return the same
     // * representation instance. Note that if the entity is large this method
     // * can result in important memory consumption.
     // *
     // * @return The entity as a JSON representation.
     // */
     // public org.restlet.representation.JsonRepresentation getEntityAsJson() {
     // if (this.jsonRepresentation == null) {
     // this.jsonRepresentation = (getEntity() == null) ? null
     // : new org.restlet.representation.JsonRepresentation(
     // getEntity());
     // }
     //
     // return this.jsonRepresentation;
     // }
 
     // [ifdef gwt] method uncomment
     // /**
     // * Returns the entity as a XML DOM representation.<br>
     // * This method can be called several times and will always return the same
     // * representation instance. Note that if the entity is large this method
     // * can result in important memory consumption.
     // *
     // * @return The entity as a XML DOM representation.
     // */
     // public org.restlet.representation.XmlRepresentation getEntityAsXml() {
     // if (this.xmlRepresentation == null) {
     // this.xmlRepresentation = (getEntity() == null) ? null
     // : new org.restlet.representation.XmlRepresentation(
     // getEntity());
     // }
     //
     // return this.xmlRepresentation;
     // }
 
     // [ifndef gwt] method
     /**
      * Returns the entity as text.<br>
      * This method can be called several times and will always return the same
      * text. Note that if the entity is large this method can result in
      * important memory consumption.
      * 
      * @return The entity as text.
      */
     public String getEntityAsText() {
         if (this.entityText == null) {
             try {
                 this.entityText = getEntity().getText();
             } catch (IOException e) {
                 Context.getCurrentLogger().log(Level.FINE,
                         "Unable to get the entity text.", e);
             }
         }
 
         return this.entityText;
     }
 
     /**
      * Indicates if the message was or will be exchanged confidentially, for
      * example via a SSL-secured connection.
      * 
      * @return True if the message is confidential.
      */
     public abstract boolean isConfidential();
 
     /**
      * Indicates if a content is available and can be sent. Several conditions
      * must be met: the content must exists and have some available data.
      * 
      * @return True if a content is available and can be sent.
      */
     public boolean isEntityAvailable() {
        return (getEntity() != null) && (getEntity().getSize() != 0)
                 && getEntity().isAvailable();
     }
 
     /**
      * Releases the message's entity. If the entity is transient and hasn't been
      * read yet, all the remaining content will be discarded, any open socket,
      * channel, file or similar source of content will be immediately closed.
      */
     public void release() {
         if (getEntity() != null) {
             getEntity().release();
         }
     }
 
     /**
      * Sets the modifiable map of attributes
      * 
      * @param attributes
      *            The modifiable map of attributes
      */
     public void setAttributes(Map<String, Object> attributes) {
         this.attributes = attributes;
     }
 
     /**
      * Sets the entity representation.
      * 
      * @param entity
      *            The entity representation.
      */
     public void setEntity(Representation entity) {
         this.entity = entity;
     }
 
     /**
      * Sets a textual entity.
      * 
      * @param value
      *            The represented string.
      * @param mediaType
      *            The representation's media type.
      */
     public void setEntity(String value, MediaType mediaType) {
         setEntity(new StringRepresentation(value, mediaType));
     }
 
 }
