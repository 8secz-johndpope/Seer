 /***** BEGIN LICENSE BLOCK *****
  * Version: CPL 1.0/GPL 2.0/LGPL 2.1
  *
  * The contents of this file are subject to the Common Public
  * License Version 1.0 (the "License"); you may not use this file
  * except in compliance with the License. You may obtain a copy of
  * the License at http://www.eclipse.org/legal/cpl-v10.html
  *
  * Software distributed under the License is distributed on an "AS
  * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
  * implied. See the License for the specific language governing
  * rights and limitations under the License.
  *
  * Copyright (C) 2006, 2007 Ola Bini <ola@ologix.com>
  * 
  * Alternatively, the contents of this file may be used under the terms of
  * either of the GNU General Public License Version 2 or later (the "GPL"),
  * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
  * in which case the provisions of the GPL or the LGPL are applicable instead
  * of those above. If you wish to allow use of your version of this file only
  * under the terms of either the GPL or the LGPL, and not to allow others to
  * use your version of this file under the terms of the CPL, indicate your
  * decision by deleting the provisions above and replace them with the notice
  * and other provisions required by the GPL or the LGPL. If you do not delete
  * the provisions above, a recipient may use your version of this file under
  * the terms of any one of the CPL, the GPL or the LGPL.
  ***** END LICENSE BLOCK *****/
 package org.jruby.ext.openssl;
 
 import java.lang.reflect.InvocationTargetException;
 import java.math.BigInteger;
 import java.text.DateFormat;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Calendar;
 import java.util.Date;
 import java.util.Enumeration;
 import java.util.HashMap;
 import java.util.IdentityHashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.io.IOException;
 
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import org.bouncycastle.asn1.ASN1Encodable;
 import org.bouncycastle.asn1.ASN1EncodableVector;
 import org.bouncycastle.asn1.ASN1InputStream;
 import org.bouncycastle.asn1.ASN1Sequence;
 import org.bouncycastle.asn1.DERBitString;
 import org.bouncycastle.asn1.DERBoolean;
 import org.bouncycastle.asn1.DEREncodableVector;
 import org.bouncycastle.asn1.DERInteger;
 import org.bouncycastle.asn1.DERNull;
 import org.bouncycastle.asn1.DERObjectIdentifier;
 import org.bouncycastle.asn1.DEROctetString;
 import org.bouncycastle.asn1.DERSequence;
 import org.bouncycastle.asn1.DERSet;
 import org.bouncycastle.asn1.DERString;
 import org.bouncycastle.asn1.DERTaggedObject;
 import org.bouncycastle.asn1.DERUTCTime;
 import org.bouncycastle.asn1.DERUTF8String;
 import org.jruby.Ruby;
 import org.jruby.RubyArray;
 import org.jruby.RubyBignum;
 import org.jruby.RubyClass;
 import org.jruby.RubyModule;
 import org.jruby.RubyNumeric;
 import org.jruby.RubyObject;
 import org.jruby.RubyString;
 import org.jruby.RubySymbol;
 import org.jruby.RubyTime;
 import org.jruby.anno.JRubyMethod;
 import org.jruby.exceptions.RaiseException;
 import org.jruby.runtime.Block;
 import org.jruby.runtime.ObjectAllocator;
 import org.jruby.runtime.ThreadContext;
 import org.jruby.runtime.builtin.IRubyObject;
 import org.jruby.util.ByteList;
 
 /**
  * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
  */
 public class ASN1 {
     private static Map<Ruby, Map<String, DERObjectIdentifier>> SYM_TO_OID = new IdentityHashMap<Ruby, Map<String, DERObjectIdentifier>>();
     private static Map<Ruby, Map<DERObjectIdentifier, String>> OID_TO_SYM = new IdentityHashMap<Ruby, Map<DERObjectIdentifier, String>>();
     private static Map<Ruby, Map<DERObjectIdentifier, Integer>> OID_TO_NID = new IdentityHashMap<Ruby, Map<DERObjectIdentifier, Integer>>();
     private static Map<Ruby, Map<Integer, DERObjectIdentifier>> NID_TO_OID = new IdentityHashMap<Ruby, Map<Integer, DERObjectIdentifier>>();
     private static Map<Ruby, Map<Integer, String>> NID_TO_SN = new IdentityHashMap<Ruby, Map<Integer, String>>();
     private static Map<Ruby, Map<Integer, String>> NID_TO_LN = new IdentityHashMap<Ruby, Map<Integer, String>>();
 
 
     static void addObject(Ruby runtime, int nid, String sn, String ln, String oid) {
         Map<String, DERObjectIdentifier> s2o = SYM_TO_OID.get(runtime);
         Map<DERObjectIdentifier, String> o2s = OID_TO_SYM.get(runtime);
         Map<DERObjectIdentifier, Integer> o2n = OID_TO_NID.get(runtime);
         Map<Integer, DERObjectIdentifier> n2o = NID_TO_OID.get(runtime);
         Map<Integer, String> n2s = NID_TO_SN.get(runtime);
         Map<Integer, String> n2l = NID_TO_LN.get(runtime);
         if(null != oid && (null != sn || null != ln)) {
             DERObjectIdentifier ident = new DERObjectIdentifier(oid);
             if(sn != null) {
                 s2o.put(sn.toLowerCase(),ident);
             }
             if(ln != null) {
                 s2o.put(ln.toLowerCase(),ident);
             }
             o2s.put(ident,sn == null ? ln : sn);
             o2n.put(ident,nid);
             n2o.put(nid,ident);
             n2s.put(nid,sn);
             n2l.put(nid,ln);
         }        
     }
 
     @SuppressWarnings("unchecked")
     private synchronized static void initMaps(Ruby runtime) {
         Map<String, DERObjectIdentifier> val = new HashMap<String, DERObjectIdentifier>(org.bouncycastle.asn1.x509.X509Name.DefaultLookUp);
         Map<DERObjectIdentifier, String> val2 = new HashMap<DERObjectIdentifier, String>(org.bouncycastle.asn1.x509.X509Name.DefaultSymbols);
         SYM_TO_OID.put(runtime,val);
         OID_TO_SYM.put(runtime,val2);
         OID_TO_NID.put(runtime,new HashMap<DERObjectIdentifier, Integer>());
         NID_TO_OID.put(runtime,new HashMap<Integer, DERObjectIdentifier>());
         NID_TO_SN.put(runtime,new HashMap<Integer, String>());
         NID_TO_LN.put(runtime,new HashMap<Integer, String>());
         OpenSSLImpl.defaultObjects(runtime);
     }
 
     synchronized static Integer obj2nid(Ruby runtime, String oid) {
         return obj2nid(runtime, new DERObjectIdentifier(oid));
     }
 
     synchronized static String ln2oid(Ruby runtime, String ln) {
         Map<String, DERObjectIdentifier> val = SYM_TO_OID.get(runtime);
         if(null == val) {
             initMaps(runtime);
             val = SYM_TO_OID.get(runtime);
         }
         return val.get(ln).getId();
     }
 
     synchronized static Integer obj2nid(Ruby runtime, DERObjectIdentifier oid) {
         Map<DERObjectIdentifier, Integer> o2n = OID_TO_NID.get(runtime);
         if(null == o2n) {
             initMaps(runtime);
             o2n = OID_TO_NID.get(runtime);
         }
         return o2n.get(oid);
     }
 
     synchronized static String o2a(Ruby runtime, DERObjectIdentifier obj) {
         Integer nid = obj2nid(runtime,obj);
         Map<Integer, String> n2l = NID_TO_LN.get(runtime);
         Map<Integer, String> n2s = NID_TO_SN.get(runtime);
         String one = n2l.get(nid);
         if(one == null) {
             one = n2s.get(nid);
         }
         return one;
     }
 
     synchronized static String nid2ln(Ruby runtime, int nid) {
         return nid2ln(runtime, new Integer(nid));
     }
 
     synchronized static String nid2ln(Ruby runtime, Integer nid) {
         Map<Integer, String> n2l = NID_TO_LN.get(runtime);
         if(null == n2l) {
             initMaps(runtime);
             n2l = NID_TO_LN.get(runtime);
         }
         return n2l.get(nid);
     }
     
     synchronized static Map<String, DERObjectIdentifier> getOIDLookup(Ruby runtime) {
         Map<String, DERObjectIdentifier> val = SYM_TO_OID.get(runtime);
         if(null == val) {
             initMaps(runtime);
             val = SYM_TO_OID.get(runtime);
         }
         return val;
     }
 
     synchronized static Map<DERObjectIdentifier, String> getSymLookup(Ruby runtime) {
         Map<DERObjectIdentifier, String> val = OID_TO_SYM.get(runtime);
         if(null == val) {
             initMaps(runtime);
             val = OID_TO_SYM.get(runtime);
         }
         return val;
     }
 
     private final static Object[][] ASN1_INFO = {
         {"EOC", null, null },
         {"BOOLEAN", org.bouncycastle.asn1.DERBoolean.class, "Boolean" },
         {"INTEGER", org.bouncycastle.asn1.DERInteger.class, "Integer" }, 
         {"BIT_STRING",  org.bouncycastle.asn1.DERBitString.class, "BitString" },
         {"OCTET_STRING",  org.bouncycastle.asn1.DEROctetString.class, "OctetString" },
         {"NULL",  org.bouncycastle.asn1.DERNull.class, "Null" },
         {"OBJECT",  org.bouncycastle.asn1.DERObjectIdentifier.class, "ObjectId" },
         {"OBJECT_DESCRIPTOR",  null, null },
         {"EXTERNAL",  null, null },
         {"REAL",  null, null },
         {"ENUMERATED",  org.bouncycastle.asn1.DEREnumerated.class, "Enumerated" },
         {"EMBEDDED_PDV",  null, null },
         {"UTF8STRING",  org.bouncycastle.asn1.DERUTF8String.class, "UTF8String" },
         {"RELATIVE_OID",  null, null },
         {"[UNIVERSAL 14]",  null, null },
         {"[UNIVERSAL 15]",  null, null },
         {"SEQUENCE",  org.bouncycastle.asn1.DERSequence.class, "Sequence" },
         {"SET",  org.bouncycastle.asn1.DERSet.class, "Set" },
         {"NUMERICSTRING",  org.bouncycastle.asn1.DERNumericString.class, "NumericString" },
         {"PRINTABLESTRING",  org.bouncycastle.asn1.DERPrintableString.class, "PrintableString" },
         {"T61STRING",  org.bouncycastle.asn1.DERT61String.class, "T61String" },
         {"VIDEOTEXSTRING", null, null },
         {"IA5STRING",  org.bouncycastle.asn1.DERIA5String.class, "IA5String" },
         {"UTCTIME",  org.bouncycastle.asn1.DERUTCTime.class, "UTCTime" },
         {"GENERALIZEDTIME",  org.bouncycastle.asn1.DERGeneralizedTime.class, "GeneralizedTime" },
         {"GRAPHICSTRING",  null, null },
         {"ISO64STRING",  null, null },
         {"GENERALSTRING",  org.bouncycastle.asn1.DERGeneralString.class, "GeneralString" },
         {"UNIVERSALSTRING",  org.bouncycastle.asn1.DERUniversalString.class, "UniversalString" },
         {"CHARACTER_STRING",  null, null },
         {"BMPSTRING", org.bouncycastle.asn1.DERBMPString.class, "BMPString" }};
 
     private final static Map<Class, Integer> CLASS_TO_ID = new HashMap<Class, Integer>();
     private final static Map<String, Integer> RUBYNAME_TO_ID = new HashMap<String, Integer>();
     
     static {
         for(int i=0;i<ASN1_INFO.length;i++) {
             if(ASN1_INFO[i][1] != null) {
                 CLASS_TO_ID.put((Class)ASN1_INFO[i][1],new Integer(i));
             }
             if(ASN1_INFO[i][2] != null) {
                 RUBYNAME_TO_ID.put((String)ASN1_INFO[i][2],new Integer(i));
             }
         }
     }
 
     public static int idForClass(Class type) {
         Integer v = null;
         while(type != Object.class && v == null) {
             v = CLASS_TO_ID.get(type);
             if(v == null) {
                 type = type.getSuperclass();
             }
         }
         return null == v ? -1 : v.intValue();
     }
 
     public static int idForRubyName(String name) {
         Integer v = RUBYNAME_TO_ID.get(name);
         return null == v ? -1 : v.intValue();
     }
 
     public static Class<? extends ASN1Encodable> classForId(int id) {
         @SuppressWarnings("unchecked")
         Class<? extends ASN1Encodable> result = (Class<? extends ASN1Encodable>)(ASN1_INFO[id][1]);
         return result;
     }
     
     public static void createASN1(Ruby runtime, RubyModule ossl) {
         RubyModule mASN1 = ossl.defineModuleUnder("ASN1");
         RubyClass openSSLError = ossl.getClass("OpenSSLError");
         mASN1.defineClassUnder("ASN1Error",openSSLError, openSSLError.getAllocator());
 
         mASN1.defineAnnotatedMethods(ASN1.class);
 
         List<IRubyObject> ary = new ArrayList<IRubyObject>();
         mASN1.setConstant("UNIVERSAL_TAG_NAME",runtime.newArray(ary));
         for(int i=0;i<ASN1_INFO.length;i++) {
             if(((String)(ASN1_INFO[i][0])).charAt(0) != '[') {
                 ary.add(runtime.newString(((String)(ASN1_INFO[i][0]))));
                 mASN1.setConstant(((String)(ASN1_INFO[i][0])),runtime.newFixnum(i));
             } else {
                 ary.add(runtime.getNil());
             }
         }
 
         RubyClass cASN1Data = mASN1.defineClassUnder("ASN1Data",runtime.getObject(), ASN1Data.ALLOCATOR);
         cASN1Data.attr_accessor(runtime.getCurrentContext(), new IRubyObject[]{runtime.newString("value"),runtime.newString("tag"),runtime.newString("tag_class")});
         cASN1Data.defineAnnotatedMethods(ASN1Data.class);
 
         RubyClass cASN1Primitive = mASN1.defineClassUnder("Primitive",cASN1Data, ASN1Primitive.ALLOCATOR);
         cASN1Primitive.attr_accessor(runtime.getCurrentContext(), new IRubyObject[]{runtime.newString("tagging")});
         cASN1Primitive.defineAnnotatedMethods(ASN1Primitive.class);
 
         RubyClass cASN1Constructive = mASN1.defineClassUnder("Constructive",cASN1Data,ASN1Constructive.ALLOCATOR);
         cASN1Constructive.includeModule(runtime.getModule("Enumerable"));
         cASN1Constructive.attr_accessor(runtime.getCurrentContext(), new IRubyObject[]{runtime.newString("tagging")});
         cASN1Constructive.defineAnnotatedMethods(ASN1Constructive.class);
 
         mASN1.defineClassUnder("Boolean",cASN1Primitive,cASN1Primitive.getAllocator());
         mASN1.defineClassUnder("Integer",cASN1Primitive,cASN1Primitive.getAllocator());
         mASN1.defineClassUnder("Enumerated",cASN1Primitive,cASN1Primitive.getAllocator());
 
         RubyClass cASN1BitString = mASN1.defineClassUnder("BitString",cASN1Primitive,cASN1Primitive.getAllocator());
 
         mASN1.defineClassUnder("OctetString",cASN1Primitive,cASN1Primitive.getAllocator());
         mASN1.defineClassUnder("UTF8String",cASN1Primitive,cASN1Primitive.getAllocator());
         mASN1.defineClassUnder("NumericString",cASN1Primitive,cASN1Primitive.getAllocator());
         mASN1.defineClassUnder("PrintableString",cASN1Primitive,cASN1Primitive.getAllocator());
         mASN1.defineClassUnder("T61String",cASN1Primitive,cASN1Primitive.getAllocator());
         mASN1.defineClassUnder("VideotexString",cASN1Primitive,cASN1Primitive.getAllocator());
         mASN1.defineClassUnder("IA5String",cASN1Primitive,cASN1Primitive.getAllocator());
         mASN1.defineClassUnder("GraphicString",cASN1Primitive,cASN1Primitive.getAllocator());
         mASN1.defineClassUnder("ISO64String",cASN1Primitive,cASN1Primitive.getAllocator());
         mASN1.defineClassUnder("GeneralString",cASN1Primitive,cASN1Primitive.getAllocator());
         mASN1.defineClassUnder("UniversalString",cASN1Primitive,cASN1Primitive.getAllocator());
         mASN1.defineClassUnder("BMPString",cASN1Primitive,cASN1Primitive.getAllocator());
         mASN1.defineClassUnder("Null",cASN1Primitive,cASN1Primitive.getAllocator());
 
         RubyClass cASN1ObjectId = mASN1.defineClassUnder("ObjectId",cASN1Primitive,cASN1Primitive.getAllocator());
         mASN1.defineClassUnder("UTCTime",cASN1Primitive,cASN1Primitive.getAllocator());
         mASN1.defineClassUnder("GeneralizedTime",cASN1Primitive,cASN1Primitive.getAllocator());
         mASN1.defineClassUnder("Sequence",cASN1Constructive,cASN1Constructive.getAllocator());
         mASN1.defineClassUnder("Set",cASN1Constructive,cASN1Constructive.getAllocator());
 
         cASN1ObjectId.defineAnnotatedMethods(ObjectId.class);
 
         cASN1BitString.attr_accessor(runtime.getCurrentContext(), new IRubyObject[]{runtime.newSymbol("unused_bits")});
     }
 
 
     private static String getShortNameFor(Ruby runtime, String nameOrOid) {
         DERObjectIdentifier oid = getObjectIdentifier(runtime,nameOrOid);
         Map em = getOIDLookup(runtime);
         String name = null;
         for(Iterator iter = em.keySet().iterator();iter.hasNext();) {
             Object key = iter.next();
             if(oid.equals(em.get(key))) {
                 if(name == null || ((String)key).length() < name.length()) {
                     name = (String)key;
                 }
             }
         }
         return name;
     }
 
     private static String getLongNameFor(Ruby runtime, String nameOrOid) {
         DERObjectIdentifier oid = getObjectIdentifier(runtime,nameOrOid);
         Map em = getOIDLookup(runtime);
         String name = null;
         for(Iterator iter = em.keySet().iterator();iter.hasNext();) {
             Object key = iter.next();
             if(oid.equals(em.get(key))) {
                 if(name == null || ((String)key).length() > name.length()) {
                     name = (String)key;
                 }
             }
         }
         return name;
     }
 
     private static DERObjectIdentifier getObjectIdentifier(Ruby runtime, String nameOrOid) {
         Object val1 = ASN1.getOIDLookup(runtime).get(nameOrOid.toLowerCase());
         if(null != val1) {
             return (DERObjectIdentifier)val1;
         }
         DERObjectIdentifier val2 = new DERObjectIdentifier(nameOrOid);
         return val2;
     }
     
     @JRubyMethod(name="Boolean", module=true, rest=true)
     public static IRubyObject fact_Boolean(IRubyObject recv, IRubyObject[] args) {
         return ((RubyModule)recv).getClass("Boolean").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
     }
 
     @JRubyMethod(name="Integer", module=true, rest=true)
     public static IRubyObject fact_Integer(IRubyObject recv, IRubyObject[] args) {
         return ((RubyModule)recv).getClass("Integer").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
     }
 
     @JRubyMethod(name="Enumerated", module=true, rest=true)
     public static IRubyObject fact_Enumerated(IRubyObject recv, IRubyObject[] args) {
         return ((RubyModule)recv).getClass("Enumerated").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
     }
 
     @JRubyMethod(name="BitString", module=true, rest=true)
     public static IRubyObject fact_BitString(IRubyObject recv, IRubyObject[] args) {
         return ((RubyModule)recv).getClass("BitString").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
     }
 
     @JRubyMethod(name="OctetString", module=true, rest=true)
     public static IRubyObject fact_OctetString(IRubyObject recv, IRubyObject[] args) {
         return ((RubyModule)recv).getClass("OctetString").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
     }
 
     @JRubyMethod(name="UTF8String", module=true, rest=true)
     public static IRubyObject fact_UTF8String(IRubyObject recv, IRubyObject[] args) {
         return ((RubyModule)recv).getClass("UTF8String").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
     }
 
     @JRubyMethod(name="NumericString", module=true, rest=true)
     public static IRubyObject fact_NumericString(IRubyObject recv, IRubyObject[] args) {
         return ((RubyModule)recv).getClass("NumericString").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
     }
 
     @JRubyMethod(name="PrintableString", module=true, rest=true)
     public static IRubyObject fact_PrintableString(IRubyObject recv, IRubyObject[] args) {
         return ((RubyModule)recv).getClass("PrintableString").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
     }
 
     @JRubyMethod(name="T61String", module=true, rest=true)
     public static IRubyObject fact_T61String(IRubyObject recv, IRubyObject[] args) {
         return ((RubyModule)recv).getClass("T61String").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
     }
 
     @JRubyMethod(name="VideotexString", module=true, rest=true)
     public static IRubyObject fact_VideotexString(IRubyObject recv, IRubyObject[] args) {
         return ((RubyModule)recv).getClass("VideotexString").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
     }
 
     @JRubyMethod(name="IA5String", module=true, rest=true)
     public static IRubyObject fact_IA5String(IRubyObject recv, IRubyObject[] args) {
         return ((RubyModule)recv).getClass("IA5String").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
     }
 
     @JRubyMethod(name="GraphicString", module=true, rest=true)
     public static IRubyObject fact_GraphicString(IRubyObject recv, IRubyObject[] args) {
         return ((RubyModule)recv).getClass("GraphicString").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
     }
 
     @JRubyMethod(name="ISO64String", module=true, rest=true)
     public static IRubyObject fact_ISO64String(IRubyObject recv, IRubyObject[] args) {
         return ((RubyModule)recv).getClass("ISO64String").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
     }
 
     @JRubyMethod(name="GeneralString", module=true, rest=true)
     public static IRubyObject fact_GeneralString(IRubyObject recv, IRubyObject[] args) {
         return ((RubyModule)recv).getClass("GeneralString").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
     }
 
     @JRubyMethod(name="UniversalString", module=true, rest=true)
     public static IRubyObject fact_UniversalString(IRubyObject recv, IRubyObject[] args) {
         return ((RubyModule)recv).getClass("UniversalString").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
     }
 
     @JRubyMethod(name="BMPString", module=true, rest=true)
     public static IRubyObject fact_BMPString(IRubyObject recv, IRubyObject[] args) {
         return ((RubyModule)recv).getClass("BMPString").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
     }
 
     @JRubyMethod(name="Nul", module=true, rest=true)
     public static IRubyObject fact_Null(IRubyObject recv, IRubyObject[] args) {
         return ((RubyModule)recv).getClass("Null").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
     }
 
     @JRubyMethod(name="ObjectId", module=true, rest=true)
     public static IRubyObject fact_ObjectId(IRubyObject recv, IRubyObject[] args) {
         return ((RubyModule)recv).getClass("ObjectId").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
     }
 
     @JRubyMethod(name="UTCTime", module=true, rest=true)
     public static IRubyObject fact_UTCTime(IRubyObject recv, IRubyObject[] args) {
         return ((RubyModule)recv).getClass("UTCTime").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
     }
 
     @JRubyMethod(name="GeneralizedTime", module=true, rest=true)
     public static IRubyObject fact_GeneralizedTime(IRubyObject recv, IRubyObject[] args) {
         return ((RubyModule)recv).getClass("GeneralizedTime").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
     }
 
     @JRubyMethod(name="Sequence", module=true, rest=true)
     public static IRubyObject fact_Sequence(IRubyObject recv, IRubyObject[] args) {
         return ((RubyModule)recv).getClass("Sequence").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
     }
 
     @JRubyMethod(name="Set", module=true, rest=true)
     public static IRubyObject fact_Set(IRubyObject recv, IRubyObject[] args) {
         return ((RubyModule)recv).getClass("Set").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
     }
 
     @JRubyMethod(meta=true, required=1)
     public static IRubyObject traverse(IRubyObject recv, IRubyObject a) {
         System.err.println("WARNING: unimplemented method called: traverse");
         return null;
     }
 
     public static class ObjectId {
         @JRubyMethod(meta=true, rest=true)
         public static IRubyObject register(IRubyObject recv, IRubyObject[] args) {
             DERObjectIdentifier deroi = new DERObjectIdentifier(args[0].toString());
             getOIDLookup(recv.getRuntime()).put(args[1].toString().toLowerCase(),deroi);
             getOIDLookup(recv.getRuntime()).put(args[2].toString().toLowerCase(),deroi);
             getSymLookup(recv.getRuntime()).put(deroi,args[1].toString());
             return recv.getRuntime().getTrue();
         }
 
         @JRubyMethod(name={"sn","short_name"})
         public static IRubyObject sn(IRubyObject self) {
             return self.getRuntime().newString(getShortNameFor(self.getRuntime(),self.callMethod(self.getRuntime().getCurrentContext(),"value").toString()));
         }
 
         @JRubyMethod(name={"ln","long_name"})
         public static IRubyObject ln(IRubyObject self) {
             return self.getRuntime().newString(getLongNameFor(self.getRuntime(),self.callMethod(self.getRuntime().getCurrentContext(),"value").toString()));
         }
 
         @JRubyMethod
         public static IRubyObject oid(IRubyObject self) {
             return self.getRuntime().newString(getObjectIdentifier(self.getRuntime(),self.callMethod(self.getRuntime().getCurrentContext(),"value").toString()).getId());
         }
     }
 
     private final static DateFormat dateF = new SimpleDateFormat("yyyyMMddHHmmssz");
     private static IRubyObject decodeObj(RubyModule asnM,Object v) throws IOException, java.text.ParseException {
         int ix = idForClass(v.getClass());
         String v_name = ix == -1 ? null : (String)(ASN1_INFO[ix][2]);
         ThreadContext tc = asnM.getRuntime().getCurrentContext();
         if(null != v_name) {
             RubyClass c = asnM.getClass(v_name);
             if(v instanceof DERBitString) {
                 String va = new String(ByteList.plain(((DERBitString)v).getBytes()));
                 IRubyObject bString = c.callMethod(tc,"new",asnM.getRuntime().newString(va));
                 bString.callMethod(tc,"unused_bits=",asnM.getRuntime().newFixnum(((DERBitString)v).getPadBits()));
                 return bString;
             } else if(v instanceof DERString) {
                 ByteList val; 
                 if (v instanceof DERUTF8String) {
                     val = new ByteList(((DERUTF8String) v).getString().getBytes("UTF-8"));
                 } else {
                     val = ByteList.create(((DERString)v).getString());
                 }
                 return c.callMethod(tc,"new",asnM.getRuntime().newString(val));
             } else if(v instanceof ASN1Sequence) {
                 List<IRubyObject> l = new ArrayList<IRubyObject>();
                 for(Enumeration enm = ((ASN1Sequence)v).getObjects(); enm.hasMoreElements(); ) {
                     l.add(decodeObj(asnM,enm.nextElement()));
                 }
                 return c.callMethod(tc,"new",asnM.getRuntime().newArray(l));
             } else if(v instanceof DERSet) {
                 List<IRubyObject> l = new ArrayList<IRubyObject>();
                 for(Enumeration enm = ((DERSet)v).getObjects(); enm.hasMoreElements(); ) {
                     l.add(decodeObj(asnM,enm.nextElement()));
                 }
                 return c.callMethod(tc,"new",asnM.getRuntime().newArray(l));
             } else if(v instanceof DERNull) {
                 return c.callMethod(tc,"new",asnM.getRuntime().getNil());
             } else if(v instanceof DERInteger) {
                 return c.callMethod(tc,"new",RubyNumeric.str2inum(asnM.getRuntime(),asnM.getRuntime().newString(((DERInteger)v).getValue().toString()),10));
             } else if(v instanceof DERUTCTime) {
                 Date d = dateF.parse(((DERUTCTime)v).getAdjustedTime());
                 Calendar cal = Calendar.getInstance();
                 cal.setTime(d);
                 IRubyObject[] argv = new IRubyObject[6];
                 argv[0] = asnM.getRuntime().newFixnum(cal.get(Calendar.YEAR));
                 argv[1] = asnM.getRuntime().newFixnum(cal.get(Calendar.MONTH)+1);
                 argv[2] = asnM.getRuntime().newFixnum(cal.get(Calendar.DAY_OF_MONTH));
                 argv[3] = asnM.getRuntime().newFixnum(cal.get(Calendar.HOUR_OF_DAY));
                 argv[4] = asnM.getRuntime().newFixnum(cal.get(Calendar.MINUTE));
                 argv[5] = asnM.getRuntime().newFixnum(cal.get(Calendar.SECOND));
                 return c.callMethod(tc,"new",asnM.getRuntime().getClass("Time").callMethod(tc,"local",argv));
             } else if(v instanceof DERObjectIdentifier) {
                 String av = ((DERObjectIdentifier)v).getId();
                 return c.callMethod(tc,"new",asnM.getRuntime().newString(av));
             } else if(v instanceof DEROctetString) {
                String va = new String(ByteList.plain(((DEROctetString)v).getOctets()));
                return c.callMethod(tc,"new",asnM.getRuntime().newString(va));
             } else if(v instanceof DERBoolean) {
                 return c.callMethod(tc,"new",((DERBoolean)v).isTrue() ? asnM.getRuntime().getTrue() : asnM.getRuntime().getFalse());
             } else {
                 System.out.println("Should handle: " + v.getClass().getName());
             }
         } else if(v instanceof DERTaggedObject) {
             RubyClass c = asnM.getClass("ASN1Data");
             IRubyObject val = decodeObj(asnM, ((DERTaggedObject)v).getObject());
             IRubyObject tag = asnM.getRuntime().newFixnum(((DERTaggedObject)v).getTagNo());
             IRubyObject tag_class = asnM.getRuntime().newSymbol("CONTEXT_SPECIFIC");
             return c.callMethod(tc,"new",new IRubyObject[]{asnM.getRuntime().newArray(val),tag,tag_class});
         }
 
         //        System.err.println("v: " + v + "[" + v.getClass().getName() + "]");
         return null;
     }
 
     @JRubyMethod(meta = true)
     public static IRubyObject decode(IRubyObject recv, IRubyObject obj) {
         try {
             IRubyObject obj2 = OpenSSLImpl.to_der_if_possible(obj);
             RubyModule asnM = (RubyModule)recv;
             ASN1InputStream asis = new ASN1InputStream(obj2.convertToString().getBytes());
             IRubyObject ret = decodeObj(asnM, asis.readObject());
             return ret;
         } catch(IOException e) {
             throw recv.getRuntime().newIOErrorFromException(e);
         } catch(Exception e) {
             throw recv.getRuntime().newArgumentError(e.getMessage());
         }
     }
 
     @JRubyMethod(meta=true, required=1)
     public static IRubyObject decode_all(IRubyObject recv, IRubyObject a) {
         System.err.println("WARNING: unimplemented method called: decode_all");
         return null;
     }
 
     public static class ASN1Data extends RubyObject {
         public static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
                 public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                     return new ASN1Data(runtime, klass);
                 }
             };
         public ASN1Data(Ruby runtime, RubyClass type) {
             super(runtime,type);
         }
 
         protected void asn1Error() {
             asn1Error(null);
         }
 
         protected void asn1Error(String msg) {
             throw new RaiseException(getRuntime(), (RubyClass)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("ASN1"))).getConstant("ASN1Error")), msg, true);
         }
 
         @JRubyMethod
         public IRubyObject initialize(IRubyObject value, IRubyObject tag, IRubyObject tag_class) {
             if(!(tag_class instanceof RubySymbol)) {
                 asn1Error("invalid tag class");
             }
             if(tag_class.toString().equals(":UNIVERSAL") && RubyNumeric.fix2int(tag) > 31) {
                 asn1Error("tag number for Universal too large");
             }
             ThreadContext tc = getRuntime().getCurrentContext();
             this.callMethod(tc,"tag=", tag);
             this.callMethod(tc,"value=", value);
             this.callMethod(tc,"tag_class=", tag_class);
 
             return this;
         }
 
         ASN1Encodable toASN1() {
             //            System.err.println(getMetaClass().getRealClass().getBaseName()+"#toASN1");
             ThreadContext tc = getRuntime().getCurrentContext();
             int tag = RubyNumeric.fix2int(callMethod(tc,"tag"));
             IRubyObject val = callMethod(tc,"value");
             if(val instanceof RubyArray) {
                 RubyArray arr = (RubyArray)callMethod(tc,"value");
                 if(arr.getList().size() > 1) {
                     ASN1EncodableVector vec = new ASN1EncodableVector();
                     for(Iterator iter = arr.getList().iterator();iter.hasNext();) {
                         vec.add(((ASN1Data)iter.next()).toASN1());
                     }
                     return new DERTaggedObject(tag, new DERSequence(vec));
                 } else {
                     return new DERTaggedObject(tag,((ASN1Data)(arr.getList().get(0))).toASN1());
                 }
             } else {
                 return new DERTaggedObject(tag, ((ASN1Data)val).toASN1());
             }
         }
 
         @JRubyMethod
         public IRubyObject to_der() {
             return getRuntime().newString(new String(ByteList.plain(toASN1().getDEREncoded())));
         }
 
         protected IRubyObject defaultTag() {
             int i = idForRubyName(getMetaClass().getRealClass().getBaseName());
             if(i != -1) {
                 return getRuntime().newFixnum(i);
             } else {
                 return getRuntime().getNil();
             }
         }
 
         protected void print() {
             print(0);
         }
 
         protected void printIndent(int indent) {
             for(int i=0;i<indent;i++) {
                 System.out.print(" ");
             }
         }
 
         protected void print(int indent) {
             printIndent(indent);
             System.out.println("ASN1Data: ");
             IRubyObject val = callMethod(getRuntime().getCurrentContext(),"value");
             if(val instanceof RubyArray) {
                 RubyArray arr = (RubyArray)val;
                 for(Iterator iter = arr.getList().iterator();iter.hasNext();) {
                     ((ASN1Data)iter.next()).print(indent+1);
                 }
             } else {
                 ((ASN1Data)val).print(indent+1);
             }
         }
     }
 
     public static class ASN1Primitive extends ASN1Data {
         public static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
                 public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                     return new ASN1Primitive(runtime, klass);
                 }
             };
         public ASN1Primitive(Ruby runtime, RubyClass type) {
             super(runtime,type);
         }
 
         public String toString() {
             return this.callMethod(getRuntime().getCurrentContext(),"value").toString();
         }
 
         @JRubyMethod
         public IRubyObject to_der() {
             return super.to_der();
         }
 
         @JRubyMethod(required=1, optional=4)
         public IRubyObject initialize(IRubyObject[] args) {
             IRubyObject value = args[0];
             IRubyObject tag = getRuntime().getNil();
             IRubyObject tagging = getRuntime().getNil();
             IRubyObject tag_class = getRuntime().getNil();
             if(args.length>1) {
                 tag = args[1];
                 if(args.length>2) {
                     tagging = args[2];
                     if(args.length>3) {
                         tag_class = args[3];
                     }
                 }
                 if(tag.isNil()) {
                     asn1Error("must specify tag number");
                 }
                 if(tagging.isNil()) {
                     tagging = getRuntime().newSymbol("EXPLICIT");
                 }
                 if(!(tagging instanceof RubySymbol)) {
                     asn1Error("invalid tag default");
                 }
                 if(tag_class.isNil()) {
                     tag_class = getRuntime().newSymbol("CONTEXT_SPECIFIC");
                 }
                 if(!(tag_class instanceof RubySymbol)) {
                     asn1Error("invalid tag class");
                 }
                 if(tagging.toString().equals(":IMPLICIT") && RubyNumeric.fix2int(tag) > 31) {
                     asn1Error("tag number for Universal too large");
                 }
             } else {
                 tag = defaultTag();
                 tagging = getRuntime().getNil();
                 tag_class = getRuntime().newSymbol("UNIVERSAL");
             }
             if("ObjectId".equals(getMetaClass().getRealClass().getBaseName())) {
                 String v = (String)(getSymLookup(getRuntime()).get(getObjectIdentifier(value.toString())));
                 if(v != null) {
                     value = getRuntime().newString(v);
                 }
             }
             ThreadContext tc = getRuntime().getCurrentContext();
             this.callMethod(tc,"tag=",tag);
             this.callMethod(tc,"value=",value);
             this.callMethod(tc,"tagging=",tagging);
             this.callMethod(tc,"tag_class=",tag_class);
 
             return this;
         }
 
         private DERObjectIdentifier getObjectIdentifier(String nameOrOid) {
             Object val1 = ASN1.getOIDLookup(getRuntime()).get(nameOrOid.toLowerCase());
             if(null != val1) {
                 return (DERObjectIdentifier)val1;
             }
             DERObjectIdentifier val2 = new DERObjectIdentifier(nameOrOid);
             return val2;
         }
 
         ASN1Encodable toASN1() {
             //            System.err.println(getMetaClass().getRealClass().getBaseName()+"#toASN1");
             int tag = idForRubyName(getMetaClass().getRealClass().getBaseName());
             @SuppressWarnings("unchecked") Class<? extends ASN1Encodable> imp = (Class<? extends ASN1Encodable>)ASN1_INFO[tag][1];
             IRubyObject val = callMethod(getRuntime().getCurrentContext(),"value");
             if(imp == DERObjectIdentifier.class) {
                 return getObjectIdentifier(val.toString());
             } else if(imp == DERNull.class) {
                 return new DERNull();
             } else if(imp == DERBoolean.class) {
                 return new DERBoolean(val.isTrue());
             } else if(imp == DERUTCTime.class) {
                 return new DERUTCTime(((RubyTime)val).getJavaDate());
             } else if(imp == DERInteger.class && val instanceof RubyBignum) {
                 return new DERInteger(((RubyBignum)val).getValue());
             } else if(imp == DERInteger.class) {
                 return new DERInteger(new BigInteger(val.toString()));
             } else if(imp == DEROctetString.class) {
                 return new DEROctetString(val.convertToString().getBytes());
             } else if(imp == DERBitString.class) {
                 byte[] bs = val.convertToString().getBytes();
                 int unused = 0;
                 for(int i = (bs.length-1); i>-1; i--) {
                     if(bs[i] == 0) {
                         unused += 8;
                     } else {
                         byte v2 = bs[i];
                         int x = 8;
                         while(v2 != 0) {
                             v2 <<= 1;
                             x--;
                         }
                         unused += x;
                         break;
                     }
                 }
                 return new DERBitString(bs,unused);
             } else if(val instanceof RubyString) {
                 try {
                     return imp.getConstructor(String.class).newInstance(val.toString());
                 } catch (Exception ex) {
                     throw RaiseException.createNativeRaiseException(getRuntime(), ex);
                 }
             }
             
             System.err.println("object with tag: " + tag + " and value: " + val + " and val.class: " + val.getClass().getName() + " and impl: " + imp.getName());
             System.err.println("WARNING: unimplemented method called: asn1data#toASN1");
             return null;
         }
 
         protected void print(int indent) {
             printIndent(indent);
             System.out.println(getMetaClass().getRealClass().getBaseName() + ": " + callMethod(getRuntime().getCurrentContext(),"value").callMethod(getRuntime().getCurrentContext(),"inspect").toString());
         }
     }
 
     public static class ASN1Constructive extends ASN1Data {
         public static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
                 public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                     return new ASN1Constructive(runtime, klass);
                 }
             };
         public ASN1Constructive(Ruby runtime, RubyClass type) {
             super(runtime,type);
         }
 
         @JRubyMethod
         public IRubyObject to_der() {
             return super.to_der();
         }
 
         @JRubyMethod(required=1, optional=3)
         public IRubyObject initialize(IRubyObject[] args) {
             IRubyObject value = args[0];
             IRubyObject tag = getRuntime().getNil();
             IRubyObject tagging = getRuntime().getNil();
             IRubyObject tag_class = getRuntime().getNil();
             if(args.length>1) {
                 tag = args[1];
                 if(args.length>2) {
                     tagging = args[2];
                     if(args.length>3) {
                         tag_class = args[3];
                     }
                 }
                 if(tag.isNil()) {
                     asn1Error("must specify tag number");
                 }
                 if(tagging.isNil()) {
                     tagging = getRuntime().newSymbol("EXPLICIT");
                 }
                 if(!(tagging instanceof RubySymbol)) {
                     asn1Error("invalid tag default");
                 }
                 if(tag_class.isNil()) {
                     tag_class = getRuntime().newSymbol("CONTEXT_SPECIFIC");
                 }
                 if(!(tag_class instanceof RubySymbol)) {
                     asn1Error("invalid tag class");
                 }
                 if(tagging.toString().equals(":IMPLICIT") && RubyNumeric.fix2int(tag) > 31) {
                     asn1Error("tag number for Universal too large");
                 }
             } else {
                 tag = defaultTag();
                 tagging = getRuntime().getNil();
                 tag_class = getRuntime().newSymbol("UNIVERSAL");
             }
             ThreadContext tc = getRuntime().getCurrentContext();
             this.callMethod(tc,"tag=",tag);
             this.callMethod(tc,"value=",value);
             this.callMethod(tc,"tagging=",tagging);
             this.callMethod(tc,"tag_class=",tag_class);
 
             return this;
         }
 
         ASN1Encodable toASN1() {
             //            System.err.println(getMetaClass().getRealClass().getBaseName()+"#toASN1");
             int id = idForRubyName(getMetaClass().getRealClass().getBaseName());
             if(id != -1) {
                 ASN1EncodableVector vec = new ASN1EncodableVector();
                 RubyArray arr = (RubyArray)callMethod(getRuntime().getCurrentContext(),"value");
                 for(Iterator iter = arr.getList().iterator();iter.hasNext();) {
                     IRubyObject v = (IRubyObject)iter.next();
                     if(v instanceof ASN1Data) {
                         vec.add(((ASN1Data)v).toASN1());
                     } else {
                         vec.add(((ASN1Data)ASN1.decode(getRuntime().getModule("OpenSSL").getConstant("ASN1"),OpenSSLImpl.to_der_if_possible(v))).toASN1());
                     }
                 }
                 try {
                     @SuppressWarnings("unchecked")
                     ASN1Encodable result = (ASN1Encodable)(((Class<? extends ASN1Encodable>)(ASN1_INFO[id][1])).getConstructor(new Class[]{DEREncodableVector.class}).newInstance(new Object[]{vec}));
                     return result;
                 } catch (Exception e) {
                     throw RaiseException.createNativeRaiseException(getRuntime(), e);
                 }
             }
             return null;
         }
 
         @JRubyMethod(frame=true)
         public IRubyObject each(Block block) {
             RubyArray arr = (RubyArray)callMethod(getRuntime().getCurrentContext(),"value");
             for(Iterator iter = arr.getList().iterator();iter.hasNext();) {
                 block.yield(getRuntime().getCurrentContext(),(IRubyObject)iter.next());
             }
             return getRuntime().getNil();
         }
 
         protected void print(int indent) {
             printIndent(indent);
             System.out.println(getMetaClass().getRealClass().getBaseName() + ": ");
             RubyArray arr = (RubyArray)callMethod(getRuntime().getCurrentContext(),"value");
             for(Iterator iter = arr.getList().iterator();iter.hasNext();) {
                 ((ASN1Data)iter.next()).print(indent+1);
             }
         }
     }
 }// ASN1
