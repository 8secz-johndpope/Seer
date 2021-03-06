 /*
  * DavMail POP/IMAP/SMTP/CalDav/LDAP Exchange Gateway
  * Copyright (C) 2010  Mickael Guessant
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License
  * as published by the Free Software Foundation; either version 2
  * of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
  */
 package davmail.exchange.dav;
 
 import org.apache.jackrabbit.webdav.DavConstants;
 import org.apache.jackrabbit.webdav.property.DavPropertyName;
 import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
 import org.apache.jackrabbit.webdav.xml.Namespace;
 
 import java.util.HashMap;
 import java.util.Map;
 
 /**
  * WebDav Field
  */
 public class Field {
 
     protected static final Map<DistinguishedPropertySetType, String> distinguishedPropertySetMap = new HashMap<DistinguishedPropertySetType, String>();
 
     static {
         distinguishedPropertySetMap.put(DistinguishedPropertySetType.Meeting, "6ed8da90-450b-101b-98da-00aa003f1305");
         distinguishedPropertySetMap.put(DistinguishedPropertySetType.Appointment, "00062002-0000-0000-c000-000000000046");
         distinguishedPropertySetMap.put(DistinguishedPropertySetType.Common, "00062008-0000-0000-c000-000000000046");
         distinguishedPropertySetMap.put(DistinguishedPropertySetType.PublicStrings, "00020329-0000-0000-c000-000000000046");
         distinguishedPropertySetMap.put(DistinguishedPropertySetType.Address, "00062004-0000-0000-c000-000000000046");
         distinguishedPropertySetMap.put(DistinguishedPropertySetType.InternetHeaders, "00020386-0000-0000-c000-000000000046");
         distinguishedPropertySetMap.put(DistinguishedPropertySetType.UnifiedMessaging, "4442858e-a9e3-4e80-b900-317a210cc15b");
         distinguishedPropertySetMap.put(DistinguishedPropertySetType.Task, "00062003-0000-0000-c000-000000000046");
     }
 
     protected static final Namespace DAV = Namespace.getNamespace("DAV:");
     protected static final Namespace URN_SCHEMAS_HTTPMAIL = Namespace.getNamespace("urn:schemas:httpmail:");
     protected static final Namespace URN_SCHEMAS_MAILHEADER = Namespace.getNamespace("urn:schemas:mailheader:");
 
     protected static final Namespace SCHEMAS_EXCHANGE = Namespace.getNamespace("http://schemas.microsoft.com/exchange/");
     protected static final Namespace SCHEMAS_MAPI_PROPTAG = Namespace.getNamespace("http://schemas.microsoft.com/mapi/proptag/");
     protected static final Namespace SCHEMAS_MAPI_ID = Namespace.getNamespace("http://schemas.microsoft.com/mapi/id/");
     protected static final Namespace SCHEMAS_MAPI_STRING = Namespace.getNamespace("http://schemas.microsoft.com/mapi/string/");
     protected static final Namespace SCHEMAS_REPL = Namespace.getNamespace("http://schemas.microsoft.com/repl/");
     protected static final Namespace URN_SCHEMAS_CONTACTS = Namespace.getNamespace("urn:schemas:contacts:");
     protected static final Namespace URN_SCHEMAS_CALENDAR = Namespace.getNamespace("urn:schemas:calendar:");
 
     protected static final Namespace SCHEMAS_MAPI_STRING_INTERNET_HEADERS =
             Namespace.getNamespace(SCHEMAS_MAPI_STRING.getURI() +
                     '{' + distinguishedPropertySetMap.get(DistinguishedPropertySetType.InternetHeaders) + "}/");
 
 
     /**
      * Property type list from EWS
      */
     protected static enum PropertyType {
         ApplicationTime, ApplicationTimeArray, Binary, BinaryArray, Boolean, CLSID, CLSIDArray, Currency, CurrencyArray,
         Double, DoubleArray, Error, Float, FloatArray, Integer, IntegerArray, Long, LongArray, Null, Object,
         ObjectArray, Short, ShortArray, SystemTime, SystemTimeArray, String, StringArray,
         Custom
     }
 
     protected static final Map<PropertyType, String> propertyTypeMap = new HashMap<PropertyType, String>();
 
     static {
         propertyTypeMap.put(PropertyType.Integer, "0003");
         propertyTypeMap.put(PropertyType.Boolean, "000b");
         propertyTypeMap.put(PropertyType.SystemTime, "0040");
         propertyTypeMap.put(PropertyType.String, "001f");
         propertyTypeMap.put(PropertyType.Binary, "0102");
         propertyTypeMap.put(PropertyType.Custom, "0030");
     }
 
     protected static enum DistinguishedPropertySetType {
         Meeting, Appointment, Common, PublicStrings, Address, InternetHeaders, CalendarAssistant, UnifiedMessaging, Task
     }
 
 
     protected static final Map<String, Field> fieldMap = new HashMap<String, Field>();
 
     static {
         // well known folders
         createField(URN_SCHEMAS_HTTPMAIL, "inbox");
         createField(URN_SCHEMAS_HTTPMAIL, "deleteditems");
         createField(URN_SCHEMAS_HTTPMAIL, "sentitems");
         createField(URN_SCHEMAS_HTTPMAIL, "sendmsg");
         createField(URN_SCHEMAS_HTTPMAIL, "drafts");
         createField(URN_SCHEMAS_HTTPMAIL, "calendar");
         createField(URN_SCHEMAS_HTTPMAIL, "contacts");
         createField(URN_SCHEMAS_HTTPMAIL, "outbox");
 
 
         // folder
         createField("folderclass", SCHEMAS_EXCHANGE, "outlookfolderclass");
         createField(DAV, "hassubs");
         createField(DAV, "nosubs");
         createField(URN_SCHEMAS_HTTPMAIL, "unreadcount");
         createField(SCHEMAS_REPL, "contenttag");
 
         // POP and IMAP message
         createField(DAV, "uid");
         createField("messageSize", 0x0e08, PropertyType.Integer);//PR_MESSAGE_SIZE
         createField("imapUid", 0x0e23, PropertyType.Integer);//PR_INTERNET_ARTICLE_NUMBER
         createField("junk", 0x1083, PropertyType.Integer);
         createField("flagStatus", 0x1090, PropertyType.Integer);//PR_FLAG_STATUS
         createField("messageFlags", 0x0e07, PropertyType.Integer);//PR_MESSAGE_FLAGS
         createField("lastVerbExecuted", 0x1081, PropertyType.Integer);//PR_LAST_VERB_EXECUTED
         createField("iconIndex", 0x1080, PropertyType.Integer);//PR_ICON_INDEX        
         createField(URN_SCHEMAS_HTTPMAIL, "read");
         //createField("read", 0x0e69, PropertyType.Boolean);//PR_READ
         createField("deleted", DistinguishedPropertySetType.Common, 0x8570, "deleted");
         createField("writedeleted", DistinguishedPropertySetType.Common, 0x8570, PropertyType.Custom);
         // http://schemas.microsoft.com/mapi/id/{00062008-0000-0000-C000-000000000046}/0x8506 private
         createField(URN_SCHEMAS_HTTPMAIL, "date");//PR_CLIENT_SUBMIT_TIME, 0x0039
         //createField("date", 0x0e06, PropertyType.SystemTime);//PR_MESSAGE_DELIVERY_TIME
         createField(URN_SCHEMAS_MAILHEADER, "bcc");//PS_INTERNET_HEADERS/bcc
         createField(URN_SCHEMAS_HTTPMAIL, "datereceived");//PR_MESSAGE_DELIVERY_TIME, 0x0E06
 
 
         // IMAP search
 
         createField(URN_SCHEMAS_HTTPMAIL, "subject"); // DistinguishedPropertySetType.InternetHeaders/Subject/String
         //createField("subject", 0x0037, PropertyType.String);//PR_SUBJECT
         createField("body", 0x1000, PropertyType.String);//PR_BODY
         createField(URN_SCHEMAS_HTTPMAIL, "from");
         //createField("from", DistinguishedPropertySetType.PublicStrings, 0x001f);//urn:schemas:httpmail:from
         createField(URN_SCHEMAS_MAILHEADER, "to"); // DistinguishedPropertySetType.InternetHeaders/To/String
         createField(URN_SCHEMAS_MAILHEADER, "cc"); // DistinguishedPropertySetType.InternetHeaders/To/String
 
         createField("lastmodified", 0x3008, PropertyType.SystemTime);//PR_LAST_MODIFICATION_TIME DAV:getlastmodified
 
         // failover search
         createField(DAV, "displayname");
 
         // items
         createField("etag", DAV, "getetag");
 
         // calendar
         createField(SCHEMAS_EXCHANGE, "permanenturl");
         createField(URN_SCHEMAS_CALENDAR, "instancetype"); // DistinguishedPropertySetType.PublicStrings/urn:schemas:calendar:instancetype/Integer
         createField(URN_SCHEMAS_CALENDAR, "dtstart"); // 0x10C3 SystemTime
         createField(SCHEMAS_EXCHANGE, "sensitivity"); // PR_SENSITIVITY 0x0036 Integer
         createField(URN_SCHEMAS_CALENDAR, "timezoneid"); // DistinguishedPropertySetType.PublicStrings/urn:schemas:calendar:timezoneid/Integer
         createField("processed", 0x65e8, PropertyType.Boolean);// PR_MESSAGE_PROCESSED
 
         createField(DAV, "contentclass");
         createField("internetContent", 0x6659, PropertyType.Binary);
 
         // contact
 
         createField(SCHEMAS_EXCHANGE, "outlookmessageclass");
         createField(URN_SCHEMAS_HTTPMAIL, "subject");
 
         createField(URN_SCHEMAS_CONTACTS, "middlename"); // PR_MIDDLE_NAME 0x3A44
         createField(URN_SCHEMAS_CONTACTS, "fileas"); // urn:schemas:contacts:fileas PS_PUBLIC_STRINGS
 
         createField("id", 0x0ff6, PropertyType.Binary); // PR_INSTANCE_KEY http://support.microsoft.com/kb/320749
 
         createField(URN_SCHEMAS_CONTACTS, "homepostaladdress"); // homeAddress DistinguishedPropertySetType.Address/0x0000801A/String
         createField(URN_SCHEMAS_CONTACTS, "otherpostaladdress"); // otherAddress DistinguishedPropertySetType.Address/0x0000801C/String
         createField(URN_SCHEMAS_CONTACTS, "mailingaddressid"); // postalAddressId DistinguishedPropertySetType.Address/0x00008022/String
         createField(URN_SCHEMAS_CONTACTS, "workaddress"); // workAddress DistinguishedPropertySetType.Address/0x0000801B/String
 
         createField(URN_SCHEMAS_CONTACTS, "alternaterecipient"); // alternaterecipient DistinguishedPropertySetType.PublicStrings/urn:schemas:contacts:alternaterecipient/String
 
         createField(SCHEMAS_EXCHANGE, "extensionattribute1"); // DistinguishedPropertySetType.Address/0x0000804F/String
         createField(SCHEMAS_EXCHANGE, "extensionattribute2"); // DistinguishedPropertySetType.Address/0x00008050/String
         createField(SCHEMAS_EXCHANGE, "extensionattribute3"); // DistinguishedPropertySetType.Address/0x00008051/String
         createField(SCHEMAS_EXCHANGE, "extensionattribute4"); // DistinguishedPropertySetType.Address/0x00008052/String
 
         createField(URN_SCHEMAS_CONTACTS, "bday"); // PR_BIRTHDAY 0x3A42 SystemTime
         createField(URN_SCHEMAS_CONTACTS, "businesshomepage"); // PR_BUSINESS_HOME_PAGE 0x3A51 String
         createField(URN_SCHEMAS_CONTACTS, "c"); // country DistinguishedPropertySetType.PublicStrings/urn:schemas:contacts:c/String
         createField(URN_SCHEMAS_CONTACTS, "cn"); // PR_DISPLAY_NAME 0x3001 String
         createField(URN_SCHEMAS_CONTACTS, "co"); // workAddressCountry DistinguishedPropertySetType.PublicStrings/0x00008049/String
         createField(URN_SCHEMAS_CONTACTS, "department"); // PR_DEPARTMENT_NAME 0x3A18 String
         createField(URN_SCHEMAS_CONTACTS, "email1"); // DistinguishedPropertySetType.PublicStrings/urn:schemas:contacts:email1/String
         createField(URN_SCHEMAS_CONTACTS, "email2"); // DistinguishedPropertySetType.PublicStrings/urn:schemas:contacts:email2/String
         createField(URN_SCHEMAS_CONTACTS, "email3"); // DistinguishedPropertySetType.PublicStrings/urn:schemas:contacts:email3/String
         createField(URN_SCHEMAS_CONTACTS, "facsimiletelephonenumber"); // PR_BUSINESS_FAX_NUMBER 0x3A24 String
         createField(URN_SCHEMAS_CONTACTS, "givenName"); // PR_GIVEN_NAME 0x3A06 String
         createField(URN_SCHEMAS_CONTACTS, "homeCity"); // PR_HOME_ADDRESS_CITY 0x3A59 String
         createField(URN_SCHEMAS_CONTACTS, "homeCountry"); // PR_HOME_ADDRESS_COUNTRY 0x3A5A String
         createField(URN_SCHEMAS_CONTACTS, "homePhone"); // PR_HOME_TELEPHONE_NUMBER 0x3A09 String
         createField(URN_SCHEMAS_CONTACTS, "homePostalCode"); // PR_HOME_ADDRESS_POSTAL_CODE 0x3A5B String
         createField(URN_SCHEMAS_CONTACTS, "homeState"); // PR_HOME_ADDRESS_STATE_OR_PROVINCE 0x3A5C String
         createField(URN_SCHEMAS_CONTACTS, "homeStreet"); // PR_HOME_ADDRESS_STREET 0x3A5D String
         createField(URN_SCHEMAS_CONTACTS, "l"); // workAddressCity DistinguishedPropertySetType.Address/0x00008046/String
         createField(URN_SCHEMAS_CONTACTS, "manager"); // PR_MANAGER_NAME 0x3A4E String
         createField(URN_SCHEMAS_CONTACTS, "mobile"); // PR_MOBILE_TELEPHONE_NUMBER 0x3A1C String
         createField(URN_SCHEMAS_CONTACTS, "namesuffix"); // PR_GENERATION 0x3A05 String
         createField(URN_SCHEMAS_CONTACTS, "nickname"); // PR_NICKNAME 0x3A4F String
         createField(URN_SCHEMAS_CONTACTS, "o"); // PR_OTHER_ADDRESS_CITY 0x3A5F String
         createField(URN_SCHEMAS_CONTACTS, "pager"); // PR_PAGER_TELEPHONE_NUMBER 0x3A21 String
         createField(URN_SCHEMAS_CONTACTS, "personaltitle"); // PR_DISPLAY_NAME_PREFIX 0x3A45 String
         createField(URN_SCHEMAS_CONTACTS, "postalcode"); // workAddressPostalCode DistinguishedPropertySetType.Address/0x00008048/String
         createField(URN_SCHEMAS_CONTACTS, "postofficebox"); // workAddressPostOfficeBox DistinguishedPropertySetType.Address/0x0000804A/String
         createField(URN_SCHEMAS_CONTACTS, "profession"); // PR_PROFESSION 0x3A46 String
         createField(URN_SCHEMAS_CONTACTS, "roomnumber"); // PR_OFFICE_LOCATION 0x3A19 String
         createField(URN_SCHEMAS_CONTACTS, "secretarycn"); // PR_ASSISTANT 0x3A30 String
         createField(URN_SCHEMAS_CONTACTS, "sn"); // PR_SURNAME 0x3A11 String
         createField(URN_SCHEMAS_CONTACTS, "spousecn"); // PR_SPOUSE_NAME 0x3A48 String
         createField(URN_SCHEMAS_CONTACTS, "st"); // workAddressState DistinguishedPropertySetType.Address/0x00008047/String
         createField(URN_SCHEMAS_CONTACTS, "street"); // workAddressStreet DistinguishedPropertySetType.Address/0x00008045/String
         createField(URN_SCHEMAS_CONTACTS, "telephoneNumber"); // PR_BUSINESS_TELEPHONE_NUMBER 0x3A08 String
         createField(URN_SCHEMAS_CONTACTS, "title"); // PR_TITLE 0x3A17 String
         createField(URN_SCHEMAS_HTTPMAIL, "textdescription"); // PR_BODY 0x1000 String
 
     }
 
     protected static void createField(String alias, int propertyTag, PropertyType propertyType) {
         String name = 'x' + Integer.toHexString(propertyTag) + propertyTypeMap.get(propertyType);
         Field field = new Field(alias, SCHEMAS_MAPI_PROPTAG, name);
         fieldMap.put(field.alias, field);
     }
 
     protected static void createField(String alias, DistinguishedPropertySetType propertySetType, int propertyTag, String responseAlias) {
         String name = '{' + distinguishedPropertySetMap.get(propertySetType) + "}/0x" + Integer.toHexString(propertyTag);
         Field field = new Field(alias, SCHEMAS_MAPI_ID, name, responseAlias);
         fieldMap.put(field.alias, field);
     }
 
     protected static void createField(String alias, DistinguishedPropertySetType propertySetType, int propertyTag, PropertyType propertyType) {
         String name = "_x" + propertyTypeMap.get(propertyType) + "_x" + Integer.toHexString(propertyTag);
 
         Field field = new Field(alias, Namespace.getNamespace(SCHEMAS_MAPI_ID.getURI() +
                 '{' + distinguishedPropertySetMap.get(propertySetType) + "}/"), name);
         fieldMap.put(field.alias, field);
     }
 
     protected static void createField(Namespace namespace, String name) {
         Field field = new Field(namespace, name);
         fieldMap.put(field.alias, field);
     }
 
     protected static void createField(String alias, Namespace namespace, String name) {
         Field field = new Field(alias, namespace, name);
         fieldMap.put(field.alias, field);
     }
 
     protected final DavPropertyName davPropertyName;
     protected final String alias;
     protected final String uri;
     protected final String requestPropertyString;
 
     public Field(Namespace namespace, String name) {
         this(name, namespace, name);
     }
 
     public Field(String alias, Namespace namespace, String name) {
         this(alias, namespace, name, null);
     }
 
     public Field(String alias, Namespace namespace, String name, String responseAlias) {
         davPropertyName = DavPropertyName.create(name, namespace);
         this.alias = alias;
         this.uri = namespace.getURI() + name;
         if (responseAlias == null) {
             this.requestPropertyString = '"' + uri + '"';
         } else {
             this.requestPropertyString = '"' + uri + "\" as " + responseAlias;
         }
     }
 
     public String getUri() {
         return uri;
     }
 
     public String getAlias() {
         return alias;
     }
 
     /**
      * Get Field by alias.
      *
      * @param alias field alias
      * @return field
      */
     public static Field get(String alias) {
         Field field = fieldMap.get(alias);
         if (field == null) {
             throw new IllegalArgumentException("Unknown field: " + alias);
         }
         return field;
     }
 
     /**
      * Get Mime header fieks.
      *
      * @param alias field alias
      * @return field
      */
     public static Field getHeader(String headerName) {
         return new Field(SCHEMAS_MAPI_STRING_INTERNET_HEADERS, headerName);
     }
 
     public static DavConstants createDavProperty(String alias, String value) {
         if (value == null) {
             // return DavPropertyName to remove property
             return Field.get(alias).davPropertyName;
         } else {
             return new DefaultDavProperty(Field.get(alias).davPropertyName, value);
         }
     }
 
 
     public static DavPropertyName getPropertyName(String alias) {
         return Field.get(alias).davPropertyName;
     }
 
     public static String getRequestPropertyString(String alias) {
         return Field.get(alias).requestPropertyString;
     }
 }
