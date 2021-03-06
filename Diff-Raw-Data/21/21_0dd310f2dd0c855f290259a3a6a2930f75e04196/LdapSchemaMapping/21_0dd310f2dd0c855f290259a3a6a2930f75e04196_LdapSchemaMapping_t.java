 /*
  * ====================
  * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  * 
  * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
  * 
  * The contents of this file are subject to the terms of the Common Development 
  * and Distribution License("CDDL") (the "License").  You may not use this file 
  * except in compliance with the License.
  * 
  * You can obtain a copy of the License at 
  * http://IdentityConnectors.dev.java.net/legal/license.txt
  * See the License for the specific language governing permissions and limitations 
  * under the License. 
  * 
  * When distributing the Covered Code, include this CDDL Header Notice in each file
  * and include the License file at identityconnectors/legal/license.txt.
  * If applicable, add the following below this CDDL Header, with the fields 
  * enclosed by brackets [] replaced by your own identifying information: 
  * "Portions Copyrighted [year] [name of copyright owner]"
  * ====================
  */
 package org.identityconnectors.ldap.schema;
 
 import static org.identityconnectors.ldap.LdapUtil.addBinaryOption;
 import static org.identityconnectors.ldap.LdapUtil.getStringAttrValue;
 import static org.identityconnectors.ldap.LdapUtil.hasBinaryOption;
 import static org.identityconnectors.ldap.LdapUtil.quietCreateLdapName;
 import static org.identityconnectors.ldap.LdapUtil.removeBinaryOption;
 
 import java.util.Collections;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import javax.naming.NamingEnumeration;
 import javax.naming.NamingException;
 import javax.naming.directory.BasicAttribute;
 import javax.naming.directory.BasicAttributes;
 import javax.naming.ldap.LdapContext;
 import javax.naming.ldap.LdapName;
 import javax.naming.ldap.Rdn;
 
 import org.identityconnectors.common.CollectionUtil;
 import org.identityconnectors.common.logging.Log;
 import org.identityconnectors.common.security.GuardedString;
 import org.identityconnectors.framework.common.exceptions.ConnectorException;
 import org.identityconnectors.framework.common.objects.Attribute;
 import org.identityconnectors.framework.common.objects.AttributeBuilder;
 import org.identityconnectors.framework.common.objects.AttributeInfo;
 import org.identityconnectors.framework.common.objects.AttributeUtil;
 import org.identityconnectors.framework.common.objects.Name;
 import org.identityconnectors.framework.common.objects.ObjectClass;
 import org.identityconnectors.framework.common.objects.ObjectClassInfo;
 import org.identityconnectors.framework.common.objects.Schema;
 import org.identityconnectors.framework.common.objects.Uid;
 import org.identityconnectors.ldap.AttributeMappingConfig;
 import org.identityconnectors.ldap.LdapConnection;
 import org.identityconnectors.ldap.LdapEntry;
 import org.identityconnectors.ldap.LdapPredefinedAttributes;
 import org.identityconnectors.ldap.ObjectClassMappingConfig;
 import org.identityconnectors.ldap.search.LdapSearches;
 
 /**
  * The authoritative description of the mapping between the LDAP schema
  * and the connector schema.
  *
  * @author Andrei Badea
  */
 public class LdapSchemaMapping {
 
     private static final Log log = Log.getLog(LdapSchemaMapping.class);
 
     // XXX
     // - which attrs returned by default? Currently only userApplications.
     // - return binary attrs by default too?
     // - type mapping.
     // - operations.
     // - groups.
 
     // XXX should the naming attribute be present in the schema (e.g. "cn" for account)?
 
     // XXX need a method like getAttributesToReturn(String[] wanted);
     // XXX need to check that (extended) naming attributes really exist.
 
     /**
      * The LDAP attributes with a byte array syntax.
      */
     static final Set<String> LDAP_BINARY_SYNTAX_ATTRS;
 
     /**
      * The LDAP attributes which require the binary option for transfer.
      */
     static final Set<String> LDAP_BINARY_OPTION_ATTRS;
 
     /**
      * The LDAP directory attributes to which are readable as framework attributes.
      */
     static final Set<String> LDAP_DIRECTORY_ATTRS;
 
     /**
      * The LDAP attribute to map to {@link Uid} by default.
      */
     static final String DEFAULT_LDAP_UID_ATTR = "entryUUID";
 
     /**
      * The LDAP attribute to map to {@link Name} by default.
      */
     static final String DEFAULT_LDAP_NAME_ATTR = "entryDN";
 
     static {
         // Cf. http://java.sun.com/products/jndi/tutorial/ldap/misc/attrs.html.
         LDAP_BINARY_SYNTAX_ATTRS = CollectionUtil.newCaseInsensitiveSet();
         LDAP_BINARY_SYNTAX_ATTRS.add("audio");
         LDAP_BINARY_SYNTAX_ATTRS.add("jpegPhoto");
         LDAP_BINARY_SYNTAX_ATTRS.add("photo");
         LDAP_BINARY_SYNTAX_ATTRS.add("personalSignature");
         LDAP_BINARY_SYNTAX_ATTRS.add("userPassword");
         LDAP_BINARY_SYNTAX_ATTRS.add("userCertificate");
         LDAP_BINARY_SYNTAX_ATTRS.add("caCertificate");
         LDAP_BINARY_SYNTAX_ATTRS.add("authorityRevocationList");
         LDAP_BINARY_SYNTAX_ATTRS.add("deltaRevocationList");
         LDAP_BINARY_SYNTAX_ATTRS.add("certificateRevocationList");
         LDAP_BINARY_SYNTAX_ATTRS.add("crossCertificatePair");
         LDAP_BINARY_SYNTAX_ATTRS.add("x500UniqueIdentifier");
         LDAP_BINARY_SYNTAX_ATTRS.add("supportedAlgorithms");
         // Java serialized objects.
         LDAP_BINARY_SYNTAX_ATTRS.add("javaSerializedData");
         // These seem to only be present in Active Directory.
         LDAP_BINARY_SYNTAX_ATTRS.add("thumbnailPhoto");
         LDAP_BINARY_SYNTAX_ATTRS.add("thumbnailLogo");
 
         // Cf. RFC 4522 and RFC 4523.
         LDAP_BINARY_OPTION_ATTRS = CollectionUtil.newCaseInsensitiveSet();
         LDAP_BINARY_OPTION_ATTRS.add("userCertificate");
         LDAP_BINARY_OPTION_ATTRS.add("caCertificate");
         LDAP_BINARY_OPTION_ATTRS.add("authorityRevocationList");
         LDAP_BINARY_OPTION_ATTRS.add("deltaRevocationList");
         LDAP_BINARY_OPTION_ATTRS.add("certificateRevocationList");
         LDAP_BINARY_OPTION_ATTRS.add("crossCertificatePair");
         LDAP_BINARY_OPTION_ATTRS.add("supportedAlgorithms");
 
         LDAP_DIRECTORY_ATTRS = CollectionUtil.newCaseInsensitiveSet();
         LDAP_DIRECTORY_ATTRS.add("createTimestamp");
         LDAP_DIRECTORY_ATTRS.add("modifyTimestamp");
         LDAP_DIRECTORY_ATTRS.add("creatorsName");
         LDAP_DIRECTORY_ATTRS.add("modifiersName");
         LDAP_DIRECTORY_ATTRS.add("entryDN");
     }
 
     private final LdapConnection conn;
 
     private Schema schema;
     private Map<String, Set<String>> ldapClass2TransSup;
 
     public LdapSchemaMapping(LdapConnection conn) {
         this.conn = conn;
     }
 
     private void init() {
         LdapSchemaBuilder bld = new LdapSchemaBuilder(conn);
         schema = bld.getSchema();
         ldapClass2TransSup = bld.getLdapClassTransitiveSuperiors();
     }
 
     public Schema schema() {
         if (schema == null) {
             init();
         }
         return schema;
     }
 
     private Map<String, Set<String>> getLdapClassTransitiveSuperiors() {
         if (ldapClass2TransSup == null) {
             init();
         }
         return ldapClass2TransSup;
     }
 
     /**
      * Returns the LDAP object class to which the given framework object
      * class is mapped.
      */
     public String getLdapClass(ObjectClass oclass) {
         ObjectClassMappingConfig oclassConfig = conn.getConfiguration().getObjectClassMappingConfigs().get(oclass);
         if (oclassConfig != null) {
             return oclassConfig.getLdapClass();
         }
         // XXX will need a check here if object class names are made special.
         return oclass.getObjectClassValue();
     }
 
     /**
      * Returns the LDAP object class to which the given framework object
      * class is mapped in a transitive manner, i.e., together with any superior
      * object classes, any superiors thereof, etc..
      */
     public Set<String> getLdapClassesTransitively(ObjectClass oclass) {
         Set<String> result = CollectionUtil.newCaseInsensitiveSet();
         String ldapClass = getLdapClass(oclass);
         result.add(ldapClass);
         result.addAll(getLdapClassTransitiveSuperiors().get(ldapClass));
         return Collections.unmodifiableSet(result);
     }
 
     public String getLdapAttribute(ObjectClass oclass, String attrName, boolean transfer) {
         String result = null;
        if (AttributeUtil.namesEqual(Uid.NAME, attrName)) {
             result = getLdapUidAttribute(oclass);
        } else if (AttributeUtil.namesEqual(Name.NAME, attrName)) {
             result = getLdapNameAttribute(oclass);
         } else {
             result = getLdapMappedAttribute(oclass, attrName);
         }
 
        if (result == null && !AttributeUtil.isSpecialName(attrName)) {
             result = attrName;
         }
 
         if (result != null && transfer && needsBinaryOption(result)) {
             result = addBinaryOption(result);
         }
 
         if (result == null) {
             log.warn("Attribute {0} of object class {1} is not mapped to an LDAP attribute",
                     attrName, oclass.getObjectClassValue());
         }
         return result;
     }
 
     /**
      * Returns the name of the LDAP attribute which corresponds to the given
      * attribute of the given object class, or null.
      */
     public String getLdapAttribute(ObjectClass oclass, Attribute attr) {
         return getLdapAttribute(oclass, attr.getName(), false);
     }
 
     /**
      * Returns the names of the LDAP attributes which correspond to the given
      * attribute names of the given object class. If {@code transfer} is {@code true},
      * the binary option will be added to the attributes which need it.
      */
     public Set<String> getLdapAttributes(ObjectClass oclass, Set<String> attrs, boolean transfer) {
         Set<String> result = CollectionUtil.newCaseInsensitiveSet();
         for (String attr : attrs) {
             String ldapAttr = getLdapAttribute(oclass, attr, transfer);
             if (ldapAttr != null) {
                 result.add(ldapAttr);
             }
         }
         return result;
     }
 
     /**
      * Returns the LDAP attribute which corresponds to {@link Uid}. Should
      * never return null.
      */
     public String getLdapUidAttribute(ObjectClass oclass) {
         String result = null;
         ObjectClassMappingConfig oclassConfig = conn.getConfiguration().getObjectClassMappingConfigs().get(oclass);
         if (oclassConfig != null) {
             result = oclassConfig.getUidAttribute();
         }
         if (result == null) {
             result = DEFAULT_LDAP_UID_ATTR;
         }
         return result;
     }
 
     /**
      * Returns the LDAP attribute which corresponds to {@link Name} for the
      * given object class. Might return {@code null} if, for example, the
      * object class was not configured explicitly in the configuration.
      */
     public String getLdapNameAttribute(ObjectClass oclass) {
         String result = null;
         ObjectClassMappingConfig oclassConfig = conn.getConfiguration().getObjectClassMappingConfigs().get(oclass);
         if (oclassConfig != null) {
             result = oclassConfig.getNameAttribute();
         }
         if (result == null) {
             result = DEFAULT_LDAP_NAME_ATTR;
         }
         return result;
     }
 
     public String getLdapMappedAttribute(ObjectClass oclass, String attrName) {
         String result = null;
         ObjectClassMappingConfig oclassConfig = conn.getConfiguration().getObjectClassMappingConfigs().get(oclass);
         if (oclassConfig != null) {
             AttributeMappingConfig attrConfig = oclassConfig.getAttributeMapping(attrName);
             if (attrConfig != null) {
                 result = attrConfig.getToAttribute();
             }
         }
         return result;
     }
 
     /**
      * Creates a {@link Uid} from the given attribute set.
      */
     public Uid createUid(ObjectClass oclass, LdapEntry entry) {
         String ldapUidAttr = getLdapUidAttribute(oclass);
         String value = getStringAttrValue(entry.getAttributes(), ldapUidAttr);
         if (value == null) {
             // We don't want to return a null Uid.
             throw new ConnectorException("No attribute named " + ldapUidAttr + " found in the search result");
         }
         return new Uid((String) value);
     }
 
     /**
      * Creates a {@link Name} for the given search result.
      */
     public Name createName(ObjectClass oclass, LdapEntry entry) {
         // First try the short way: assume the naming attribute is in the search result.
         String nameAttr = getLdapNameAttribute(oclass);
         if (nameAttr != null) {
             String name = getStringAttrValue(entry.getAttributes(), nameAttr);
             if (name != null) {
                 return new Name(name);
             }
         }
 
         // No naming attribute. Try to derive it from the DN.
         LdapName dn = entry.getDN();
         Rdn rdn = dn.getRdn(dn.size() - 1);
         if (rdn.size() != 1) {
             // XXX perhaps just use the first attribute?
             throw new ConnectorException("Unsupported number of naming attributes: " + rdn.size());
         }
         String name = (String) rdn.getValue();
         return new Name(name);
     }
 
     public Attribute createAttribute(ObjectClass oclass, String attrName, LdapEntry entry) {
         String ldapAttrNameForTransfer = getLdapAttribute(oclass, attrName, true);
         String ldapAttrName;
         if (hasBinaryOption(ldapAttrNameForTransfer)) {
             ldapAttrName = removeBinaryOption(ldapAttrNameForTransfer);
         } else {
             ldapAttrName = ldapAttrNameForTransfer;
         }
 
         javax.naming.directory.Attribute ldapAttr = entry.getAttributes().get(ldapAttrNameForTransfer);
         if (ldapAttr == null) {
             return null;
         }
 
         String mapDnToAttr = null;
         ObjectClassMappingConfig oclassConfig = conn.getConfiguration().getObjectClassMappingConfigs().get(oclass);
         if (oclassConfig != null) {
             AttributeMappingConfig attrConfig = oclassConfig.getDNMapping(ldapAttrName);
             if (attrConfig != null) {
                 mapDnToAttr = attrConfig.getToAttribute();
             }
         }
 
         AttributeBuilder builder = new AttributeBuilder();
         builder.setName(attrName);
         try {
             NamingEnumeration<?> valEnum = ldapAttr.getAll();
             while (valEnum.hasMore()) {
                 Object value = valEnum.next();
                 if (mapDnToAttr != null && value != null) {
                     LdapName entryDN = quietCreateLdapName(value.toString());
                     LdapEntry mapToEntry = LdapSearches.findEntry(conn, entryDN, mapDnToAttr);
                     if (mapToEntry != null) {
                         Object mappedValue = mapToEntry.getAttributes().get(mapDnToAttr).get();
                         if (mappedValue != null) {
                             // Only set value to the mapped value if one exists.
                             value = mappedValue;
                         }
                     }
                 }
                 builder.addValue(value);
             }
         } catch (NamingException e) {
             throw new ConnectorException(e);
         }
         return builder.build();
     }
     
     public LdapContext create(ObjectClass oclass, Name name, javax.naming.directory.Attributes initialAttrs) {
         String ldapNameAttr = getLdapNameAttribute(oclass);
         if (!"entryDN".equals(ldapNameAttr)) {
             // Not yet implemented.
             throw new UnsupportedOperationException("Name can only be mapped to entryDN");
         }
 
         LdapName entryName = quietCreateLdapName(name.getNameValue());
         Rdn rdn = entryName.getRdn(entryName.size() - 1);
         String containerDN = entryName.getPrefix(entryName.size() - 1).toString();
 
         BasicAttributes ldapAttrs = new BasicAttributes();
         NamingEnumeration<? extends javax.naming.directory.Attribute> initialAttrEnum = initialAttrs.getAll();
         while (initialAttrEnum.hasMoreElements()) {
             ldapAttrs.put(initialAttrEnum.nextElement());
         }
         BasicAttribute objectClass = new BasicAttribute("objectClass");
         for (String ldapClass : conn.getSchemaMapping().getLdapClassesTransitively(oclass)) {
             objectClass.add(ldapClass);
         }
         ldapAttrs.put(objectClass);
 
         // XXX this is a huge hack! Rewrite!
         javax.naming.directory.Attribute cnAttr = initialAttrs.get("cn");
         if (cnAttr == null) {
             cnAttr = new BasicAttribute("cn", rdn.getValue().toString());
             ldapAttrs.put(cnAttr);
         }
 
         log.ok("Creating LDAP subcontext {0} in {1} with attributes {2}", rdn, containerDN, ldapAttrs);
         try {
             LdapContext parentCtx = (LdapContext) conn.getInitialContext().lookup(containerDN);
             return (LdapContext) parentCtx.createSubcontext(rdn.toString(), ldapAttrs);
         } catch (NamingException e) {
             throw new ConnectorException(e);
         }
 
     }
 
     public javax.naming.directory.Attribute encodeAttribute(ObjectClass oclass, Attribute attr) {
         if (attr.is(LdapPredefinedAttributes.PASSWORD_NAME)) {
             throw new IllegalArgumentException("This method should not be used for password attributes");
         }
 
         String ldapAttrName = getLdapAttribute(oclass, attr.getName(), true);
         if (ldapAttrName == null) {
             return null;
         }
 
         final BasicAttribute ldapAttr = new BasicAttribute(ldapAttrName);
         List<Object> value = attr.getValue();
         if (value != null) {
             for (Object each : value) {
                 ldapAttr.add(each);
             }
         }
         return ldapAttr;
     }
 
     public GuardedPasswordAttribute encodePassword(ObjectClass oclass, Attribute attr) {
         if (!attr.is(LdapPredefinedAttributes.PASSWORD_NAME)) {
             throw new IllegalArgumentException("This method should be used for password attributes");
         }
         String ldapAttrName = getLdapAttribute(oclass, attr.getName(), true);
         if (ldapAttrName == null) {
             return null;
         }
 
         List<Object> value = attr.getValue();
         if (value != null) {
             for (Object each : value) {
                 GuardedString password = (GuardedString) each;
                 return GuardedPasswordAttribute.create(ldapAttrName, password);
             }
         }
         return GuardedPasswordAttribute.create(ldapAttrName);
     }
 
     public void rename(ObjectClass oclass, String entryDN, Name newName) {
         String ldapNameAttr = getLdapNameAttribute(oclass);
         if (!"entryDN".equals(ldapNameAttr)) {
             // Not yet implemented.
             throw new UnsupportedOperationException("Name can only be mapped to entryDN");
         }
 
         String newEntryDN = newName.getNameValue();
         try {
             conn.getInitialContext().rename(entryDN, newEntryDN);
         } catch (NamingException e) {
             throw new ConnectorException(e);
         }
     }
 
     public Set<String> getAttributesReturnedByDefault(ObjectClass oclass) {
         ObjectClassInfo oci = schema().findObjectClassInfo(oclass.getObjectClassValue());
         if (oci == null) {
             return CollectionUtil.newCaseInsensitiveSet();
         }
 
         Set<String> result = CollectionUtil.newCaseInsensitiveSet();
         for (AttributeInfo info : oci.getAttributeInfo()) {
             if (info.isReturnedByDefault()) {
                 result.add(info.getName());
             }
         }
         return result;
     }
 
     public void removeNonReadableAttributes(ObjectClass oclass, Set<String> attrNames) {
         ObjectClassInfo oci = schema().findObjectClassInfo(oclass.getObjectClassValue());
         if (oci == null) {
             return;
         }
 
         Set<String> attrs = CollectionUtil.newCaseInsensitiveSet();
         Set<String> readableAttrs = CollectionUtil.newCaseInsensitiveSet();
         for (AttributeInfo info : oci.getAttributeInfo()) {
             String attrName = info.getName();
             attrs.add(attrName);
             if (info.isReadable()) {
                 readableAttrs.add(attrName);
             }
         }
         for (Iterator<String> i = attrNames.iterator(); i.hasNext();) {
             String attrName = i.next();
             // Only remove the attribute if it is a known one. Otherwise
             // we could remove attributes that are readable, but not in the schema
             // (e.g., LDAP operational attributes).
             if (attrs.contains(attrName) && !readableAttrs.contains(attrName)) {
                 i.remove();
             }
         }
     }
 
     private static boolean needsBinaryOption(String ldapAttr) {
         return LDAP_BINARY_OPTION_ATTRS.contains(ldapAttr);
     }
 }
