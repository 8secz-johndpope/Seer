 /*
  * Copyright 2012 SURFnet bv, The Netherlands
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package nl.surfnet.coin.selfservice.domain;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.SortedSet;
 import java.util.TreeSet;
 
 import javax.persistence.CascadeType;
 import javax.persistence.Column;
 import javax.persistence.Entity;
 import javax.persistence.FetchType;
 import javax.persistence.OneToMany;
 import javax.persistence.Transient;
 
 import nl.surfnet.coin.selfservice.domain.Field.Key;
 import nl.surfnet.coin.selfservice.domain.Field.Source;
 import nl.surfnet.coin.selfservice.domain.Provider.Language;
 import nl.surfnet.coin.shared.domain.DomainObject;
 
 import org.apache.commons.io.IOUtils;
 import org.apache.commons.lang.StringUtils;
 import org.apache.commons.lang.builder.ToStringBuilder;
 import org.apache.commons.lang.builder.ToStringStyle;
 import org.hibernate.annotations.Proxy;
 import org.hibernate.annotations.Sort;
 import org.hibernate.annotations.SortType;
 import org.springframework.core.io.ClassPathResource;
 import org.springframework.util.Assert;
 
 import static nl.surfnet.coin.selfservice.domain.Field.Key.APPSTORE_LOGO;
 import static nl.surfnet.coin.selfservice.domain.Field.Key.APP_URL;
 import static nl.surfnet.coin.selfservice.domain.Field.Key.DETAIL_LOGO;
 import static nl.surfnet.coin.selfservice.domain.Field.Key.ENDUSER_DESCRIPTION_EN;
 import static nl.surfnet.coin.selfservice.domain.Field.Key.ENDUSER_DESCRIPTION_NL;
 import static nl.surfnet.coin.selfservice.domain.Field.Key.EULA_URL;
 import static nl.surfnet.coin.selfservice.domain.Field.Key.INSTITUTION_DESCRIPTION_EN;
 import static nl.surfnet.coin.selfservice.domain.Field.Key.INSTITUTION_DESCRIPTION_NL;
 import static nl.surfnet.coin.selfservice.domain.Field.Key.SERVICE_DESCRIPTION_EN;
 import static nl.surfnet.coin.selfservice.domain.Field.Key.SERVICE_DESCRIPTION_NL;
 import static nl.surfnet.coin.selfservice.domain.Field.Key.SERVICE_URL;
 import static nl.surfnet.coin.selfservice.domain.Field.Key.SUPPORT_MAIL;
 import static nl.surfnet.coin.selfservice.domain.Field.Key.SUPPORT_URL;
 import static nl.surfnet.coin.selfservice.domain.Field.Key.TECHNICAL_SUPPORTMAIL;
 import static org.springframework.util.StringUtils.hasText;
 
 /**
  * CompoundServiceProvider.java
  * 
  */
 @SuppressWarnings("serial")
 @Entity
 @Proxy(lazy = false)
 public class CompoundServiceProvider extends DomainObject {
 
   @Transient
   private ServiceProvider serviceProvider;
 
   @Transient
   private License license;
 
   @Column
   private String serviceProviderEntityId;
 
   @Column
   private String lmngId;
   
   @Transient
   private AttributeScopeConstraints constraints;
 
   @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "compoundServiceProvider")
   @Sort(type = SortType.NATURAL)
   private SortedSet<FieldString> fields = new TreeSet<FieldString>();
 
   @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "compoundServiceProvider")
   @Sort(type = SortType.NATURAL)
   private SortedSet<FieldImage> fieldImages = new TreeSet<FieldImage>();
 
   @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "compoundServiceProvider")
   private Set<Screenshot> screenShotsImages = new HashSet<Screenshot>();
 
   public static CompoundServiceProvider builder(ServiceProvider serviceProvider, License license) {
     Assert.notNull(serviceProvider);  
     Assert.notNull(license);  
     
     byte[] appStoreLogoImageBytes = getImageBytesFromClasspath("300x300.png");
    byte[] detailLogoImageBytes = getImageBytesFromClasspath("450x300.png");
     byte[] screenshotImageBytes = getImageBytesFromClasspath("1024x768.png");
     String todo = null;
 
     CompoundServiceProvider provider = new CompoundServiceProvider();
     provider.setServiceProvider(serviceProvider);
     provider.setLicense(license);
 
     buildFieldImage(Key.APPSTORE_LOGO, null, null, appStoreLogoImageBytes, provider);
     buildFieldString(Key.APP_URL, null, serviceProvider.getHomeUrl(), todo, provider);
     buildFieldImage(Key.DETAIL_LOGO, license.getDetailLogo(), serviceProvider.getLogoUrl(), detailLogoImageBytes, provider);
     buildFieldString(Key.ENDUSER_DESCRIPTION_EN, null, serviceProvider.getDescription(Language.EN), todo, provider);
     buildFieldString(Key.ENDUSER_DESCRIPTION_NL, license.getEndUserDescriptionNl(), serviceProvider.getDescription(Language.NL), todo,
         provider);
     buildFieldString(Key.EULA_URL, null, serviceProvider.getEulaURL(), todo, provider);
     buildFieldString(Key.INSTITUTION_DESCRIPTION_EN, null, null, todo, provider);
     buildFieldString(Key.INSTITUTION_DESCRIPTION_NL, license.getInstitutionDescriptionNl(), null, todo, provider);
     buildFieldString(Key.SERVICE_DESCRIPTION_EN, null, serviceProvider.getName(Language.EN), todo, provider);
     buildFieldString(Key.SERVICE_DESCRIPTION_NL, license.getServiceDescriptionNl(), serviceProvider.getName(Language.NL), todo, provider);
     buildFieldString(Key.SERVICE_URL, null, serviceProvider.getUrl(), todo, provider);
     buildFieldString(Key.SUPPORT_MAIL, null, getMail(serviceProvider, ContactPersonType.help), todo, provider);
     buildFieldString(Key.SUPPORT_URL, null, serviceProvider.getUrl(), todo, provider);
     buildFieldString(Key.TECHNICAL_SUPPORTMAIL, null, getMail(serviceProvider, ContactPersonType.technical), todo, provider);
 
     provider.addScreenShot(new Screenshot(screenshotImageBytes));
 
     return provider;
   }
 
   public Set<FieldString> getFields() {
     return fields;
   }
 
   public void setFields(SortedSet<FieldString> fields) {
     this.fields = fields;
   }
 
   public Set<FieldImage> getFieldImages() {
     return fieldImages;
   }
 
   public void setFieldImages(SortedSet<FieldImage> fieldImages) {
     this.fieldImages = fieldImages;
   }
 
   public Set<Screenshot> getScreenShotsImages() {
     return screenShotsImages;
   }
 
   public void setScreenShotsImages(Set<Screenshot> screenshots) {
     this.screenShotsImages = screenshots;
   }
 
   public ServiceProvider getSp() {
     return serviceProvider;
   }
 
   public License getLs() {
     return license;
   }
 
   public String getServiceProviderEntityId() {
     return serviceProviderEntityId;
   }
 
   public String getLmngId() {
     return lmngId;
   }
 
   public String getServiceDescriptionNl() {
     return (String) getFieldValue(SERVICE_DESCRIPTION_NL);
   }
 
   public String getServiceDescriptionEn() {
     return (String) getFieldValue(SERVICE_DESCRIPTION_EN);
   }
 
   public String getInstitutionDescriptionNl() {
     return (String) getFieldValue(INSTITUTION_DESCRIPTION_NL);
   }
 
   public String getInstitutionDescriptionEn() {
     return (String) getFieldValue(INSTITUTION_DESCRIPTION_EN);
   }
 
   public String getEnduserDescriptionNl() {
     return (String) getFieldValue(ENDUSER_DESCRIPTION_NL);
   }
 
   public String getEnduserDescriptionEn() {
     return (String) getFieldValue(ENDUSER_DESCRIPTION_EN);
   }
 
   public String getAppStoreLogo() {
     return (String) getFieldValue(APPSTORE_LOGO);
   }
 
   public String getDetailLogo() {
     return (String) getFieldValue(DETAIL_LOGO);
   }
 
   public String getAppUrl() {
     return (String) getFieldValue(APP_URL);
   }
 
   public List<byte[]> getScreenshots() {
     List<byte[]> result = new ArrayList<byte[]>();
     for (Screenshot s : screenShotsImages) {
       result.add(s.getImage());
     }
     return result;
   }
 
   public String getServiceUrl() {
     return (String) getFieldValue(SERVICE_URL);
   }
 
   public String getSupportUrl() {
     return (String) getFieldValue(SUPPORT_URL);
   }
 
   public String getEulaUrl() {
     return (String) getFieldValue(EULA_URL);
   }
 
   public String getSupportMail() {
     return (String) getFieldValue(SUPPORT_MAIL);
   }
 
   public String getTechnicalSupportMail() {
     return (String) getFieldValue(TECHNICAL_SUPPORTMAIL);
   }
 
   public boolean addFieldString(FieldString f) {
     Assert.notNull(f);
     f.setCompoundServiceProvider(this);
     return this.fields.add(f);
   }
 
   public boolean addFieldImage(FieldImage f) {
     Assert.notNull(f);
     f.setCompoundServiceProvider(this);
     return this.fieldImages.add(f);
   }
 
   public boolean addScreenShot(Screenshot s) {
     Assert.notNull(s);
     s.setCompoundServiceProvider(this);
     return this.screenShotsImages.add(s);
   }
 
   public boolean removeScreenShot(Screenshot s) {
     Assert.notNull(s);
     s.setCompoundServiceProvider(null);
     return this.screenShotsImages.remove(s);
   }
 
   /*
    * Note that we could use reflection and create a very generic framework.
    * However this puts to many constraints on the naming convention of unrelated
    * Objects (e.g. Service Provider and License). Therefore we have chosen to
    * explicitly retrieve values.
    */
   private Object getFieldValue(Field.Key key) {
     if (this.constraints != null && !this.constraints.isAllowed(key)) {
       return null;
     }
     
     Assert.notNull(key);
     for (FieldString f : this.fields) {
       if (key.equals(f.getKey())) {
         switch (f.getSource()) {
         case LMNG:
           return getLmngProperty(key);
         case SURFCONEXT:
           return getSurfConextProperty(key);
         case DISTRIBUTIONCHANNEL:
           return getDistributionChannelProperty(f);
         default:
           throw new RuntimeException("Unknow Source ('" + f.getSource() + "')");
         }
       }
     }
     for (FieldImage f : this.fieldImages) {
       if (key.equals(f.getKey())) {
         switch (f.getSource()) {
         case LMNG:
           return getLmngProperty(key);
         case SURFCONEXT:
           return getSurfConextProperty(key);
         case DISTRIBUTIONCHANNEL:
           return getDistributionChannelProperty(f);
         default:
           throw new RuntimeException("Unknow Source ('" + f.getSource() + "')");
         }
       }
     }
     throw new RuntimeException("Unset key for ('" + this + "'");
   }
 
   private String getDistributionChannelProperty(Field field) {
     if (field instanceof FieldImage) {
       return ((FieldImage) field).getFileUrl();
     }
     if (field instanceof FieldString) {
       return ((FieldString) field).getValue();
     }
     throw new RuntimeException("Unknown Field class: " + field.getClass());
   }
 
   public Map<Key, String> getSurfConextFieldValues() {
     return getFieldValues(Source.SURFCONEXT);
   }
   public Map<Key, String> getLmngFieldValues() {
     return getFieldValues(Source.LMNG);
   }
   public Map<Key, String> getDistributionFieldValues() {
     return getFieldValues(Source.DISTRIBUTIONCHANNEL);
   }
   
   
   /**
    * Convenience method for JSP access
    * 
    * @return Map with all Keys currently supported by SURFconext
    */
   private Map<Key, String> getFieldValues(Source source) {
     Key[] values = Key.values();
     Map<Key, String> result = new HashMap<Field.Key, String>(); 
     if (source.equals(Source.DISTRIBUTIONCHANNEL)) {
       for (FieldString field : this.fields) {
         result.put(field.getKey(), field.getValue());
       }
       for (FieldImage fieldImage : this.fieldImages) {
         result.put(fieldImage.getKey(), fieldImage.getFileUrl());
       }
 
     } else {
       for (Key key : values) {
           try {
             switch (source) {
             case SURFCONEXT:
               result.put(key, (String)getSurfConextProperty(key));
               break;
             case LMNG:
               result.put(key, (String)getLmngProperty(key));
               break;
             case DISTRIBUTIONCHANNEL:
               //already covered
               break;
             }
           } catch (RuntimeException e) {
             //not a problem here
         }
       }
     }
     return result;
   }
 
   private Object getSurfConextProperty(Key key) {
     switch (key) {
     case ENDUSER_DESCRIPTION_NL:
       return this.serviceProvider.getDescription(Language.NL);
     case ENDUSER_DESCRIPTION_EN:
       return this.serviceProvider.getDescription(Language.EN);
     case SERVICE_DESCRIPTION_NL:
       return this.serviceProvider.getName(Language.NL);
     case SERVICE_DESCRIPTION_EN:
       return this.serviceProvider.getName(Language.EN);
     case DETAIL_LOGO:
       return this.serviceProvider.getLogoUrl();
     case APP_URL:
       return this.serviceProvider.getHomeUrl();
     case SERVICE_URL:
       return this.serviceProvider.getUrl();
     case SUPPORT_URL:
       return this.serviceProvider.getUrl();
     case SUPPORT_MAIL:
       ContactPerson helpCP = this.serviceProvider.getContactPerson(ContactPersonType.help);
       return helpCP != null ? helpCP.getEmailAddress() : null;
     case TECHNICAL_SUPPORTMAIL:
       ContactPerson cp = this.serviceProvider.getContactPerson(ContactPersonType.technical);
       return cp != null ? cp.getEmailAddress() : null;
     case EULA_URL:
       return this.serviceProvider.getEulaURL();
     default:
       throw new RuntimeException("SURFConext does not support property: " + key);
     }
   }
 
   private Object getLmngProperty(Key key) {
     switch (key) {
     case ENDUSER_DESCRIPTION_NL:
       return this.license.getEndUserDescriptionNl();
     case INSTITUTION_DESCRIPTION_NL:
       return this.license.getInstitutionDescriptionNl();
     case SERVICE_DESCRIPTION_NL:
       return this.license.getServiceDescriptionNl();
     case DETAIL_LOGO:
       return this.license.getDetailLogo();
     default:
       throw new RuntimeException("LMNG does not support property: " + key);
     }
   }
 
   @Override
   public String toString() {
     return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
       .append("id", getId())
       .append("serviceProvider", serviceProvider)
       .append("serviceProviderEntityId", serviceProviderEntityId)
       .append("lmngId", lmngId)
       .toString();
   }
 
   private static byte[] getImageBytesFromClasspath(String filename) {
     try {
       return IOUtils.toByteArray(new ClassPathResource(filename).getInputStream());
     } catch (IOException e) {
       throw new RuntimeException(e);
     }
   }
 
   public ServiceProvider getServiceProvider() {
     return serviceProvider;
   }
 
   public void setServiceProvider(ServiceProvider serviceProvider) {
     this.serviceProvider = serviceProvider;
     this.serviceProviderEntityId = serviceProvider.getId();
   }
 
   public License getLicense() {
     return license;
   }
 
   public void setLicense(License license) {
     this.license = license;
     this.lmngId = license.getLmngIdentifier();
   }
 
   public boolean isLicenseAvailable() {
     return getLicense() != null && StringUtils.isNotBlank(getLicense().getLmngIdentifier());
   }
   
   private void setServiceProviderEntityId(String serviceProviderEntityId) {
     this.serviceProviderEntityId = serviceProviderEntityId;
   }
 
   private void setLmngId(String lmngId) {
     this.lmngId = lmngId;
   }
 
   private static void buildFieldString(Key key, String lmng, String surfconext, String distributionChannel, CompoundServiceProvider provider) {
     FieldString fieldString;
     if (hasText(lmng)) {
       fieldString = new FieldString(Source.LMNG, key, null);
     } else if (hasText(surfconext)) {
       fieldString = new FieldString(Source.SURFCONEXT, key, null);
     } else {
       fieldString = new FieldString(Source.DISTRIBUTIONCHANNEL, key, distributionChannel);
     }
     provider.addFieldString(fieldString);
   }
 
   private static void buildFieldImage(Key key, String lmng, String surfconext, byte[] distributionChannel, CompoundServiceProvider provider) {
     FieldImage fieldImage;
     byte[] nullByte = null;
     if (hasText(lmng)) {
       fieldImage = new FieldImage(Source.LMNG, key, nullByte);
     } else if (hasText(surfconext)) {
       fieldImage = new FieldImage(Source.SURFCONEXT, key, nullByte);
     } else {
       fieldImage = new FieldImage(Source.DISTRIBUTIONCHANNEL, key, distributionChannel);
     }
     provider.addFieldImage(fieldImage);
   }
 
   private static String getMail(ServiceProvider serviceProvider, ContactPersonType type) {
     ContactPerson helpCP = serviceProvider.getContactPerson(type);
     return (helpCP == null ? null : helpCP.getEmailAddress());
   }
 
   public void setConstraints(AttributeScopeConstraints constraints) {
     this.constraints = constraints;
   }
 
 }
