 package us.codecraft.webmagic.model;
 
 import org.apache.commons.lang3.StringUtils;
 import us.codecraft.webmagic.Page;
 import us.codecraft.webmagic.model.annotation.*;
 import us.codecraft.webmagic.selector.*;
 import us.codecraft.webmagic.utils.ExtractorUtils;
 
 import java.lang.annotation.Annotation;
 import java.lang.reflect.Field;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.regex.Pattern;
 
 /**
  * The main internal logic of page model extractor.
  *
  * @author code4crafter@gmail.com <br>
  * @since 0.2.0
  */
 class PageModelExtractor {
 
     private List<Pattern> targetUrlPatterns = new ArrayList<Pattern>();
 
     private Selector targetUrlRegionSelector;
 
     private List<Pattern> helpUrlPatterns = new ArrayList<Pattern>();
 
     private Selector helpUrlRegionSelector;
 
     private Class clazz;
 
     private List<FieldExtractor> fieldExtractors;
 
     private Extractor objectExtractor;
 
     public static PageModelExtractor create(Class clazz) {
         PageModelExtractor pageModelExtractor = new PageModelExtractor();
         pageModelExtractor.init(clazz);
         return pageModelExtractor;
     }
 
     private void init(Class clazz) {
         this.clazz = clazz;
         initClassExtractors();
         fieldExtractors = new ArrayList<FieldExtractor>();
         for (Field field : clazz.getDeclaredFields()) {
             field.setAccessible(true);
             FieldExtractor fieldExtractor = getAnnotationExtractBy(clazz, field);
             FieldExtractor fieldExtractorTmp = getAnnotationExtractCombo(clazz, field);
             if (fieldExtractor != null && fieldExtractorTmp != null) {
                 throw new IllegalStateException("Only one of 'ExtractBy ComboExtract ExtractByUrl' can be added to a field!");
             } else if (fieldExtractor == null && fieldExtractorTmp != null) {
                 fieldExtractor = fieldExtractorTmp;
             }
             fieldExtractorTmp = getAnnotationExtractByUrl(clazz, field);
             if (fieldExtractor != null && fieldExtractorTmp != null) {
                 throw new IllegalStateException("Only one of 'ExtractBy ComboExtract ExtractByUrl' can be added to a field!");
             } else if (fieldExtractor == null && fieldExtractorTmp != null) {
                 fieldExtractor = fieldExtractorTmp;
             }
             if (fieldExtractor != null) {
                 if (!fieldExtractor.isMulti() && !String.class.isAssignableFrom(field.getType())) {
                     throw new IllegalStateException("Field " + field.getName() + " must be string");
                 } else if (fieldExtractor.isMulti() && !List.class.isAssignableFrom(field.getType())) {
                     throw new IllegalStateException("Field " + field.getName() + " must be list");
                 }
                 fieldExtractors.add(fieldExtractor);
             }
         }
     }
 
     private FieldExtractor getAnnotationExtractByUrl(Class clazz, Field field) {
         FieldExtractor fieldExtractor = null;
         ExtractByUrl extractByUrl = field.getAnnotation(ExtractByUrl.class);
         if (extractByUrl != null) {
             String regexPattern = extractByUrl.value();
             if (regexPattern.trim().equals("")) {
                 regexPattern = ".*";
             }
             fieldExtractor = new FieldExtractor(field, new RegexSelector(regexPattern), FieldExtractor.Source.Url, extractByUrl.notNull(), extractByUrl.multi());
             Method setterMethod = getSetterMethod(clazz, field);
             if (setterMethod != null) {
                 fieldExtractor.setSetterMethod(setterMethod);
             }
         }
         return fieldExtractor;
     }
 
     private FieldExtractor getAnnotationExtractCombo(Class clazz, Field field) {
         FieldExtractor fieldExtractor = null;
         ComboExtract comboExtract = field.getAnnotation(ComboExtract.class);
         if (comboExtract != null) {
             ExtractBy[] extractBies = comboExtract.value();
             Selector selector;
             switch (comboExtract.op()) {
                 case And:
                     selector = new AndSelector(ExtractorUtils.getSelectors(extractBies));
                     break;
                 case Or:
                     selector = new OrSelector(ExtractorUtils.getSelectors(extractBies));
                     break;
                 default:
                     selector = new AndSelector(ExtractorUtils.getSelectors(extractBies));
             }
             fieldExtractor = new FieldExtractor(field, selector, comboExtract.source() == ComboExtract.Source.RawHtml ? FieldExtractor.Source.RawHtml : FieldExtractor.Source.Html,
                     comboExtract.notNull(), comboExtract.multi());
             Method setterMethod = getSetterMethod(clazz, field);
             if (setterMethod != null) {
                 fieldExtractor.setSetterMethod(setterMethod);
             }
         }
         return fieldExtractor;
     }
 
     private FieldExtractor getAnnotationExtractBy(Class clazz, Field field) {
         FieldExtractor fieldExtractor = null;
         ExtractBy extractBy = field.getAnnotation(ExtractBy.class);
         if (extractBy != null) {
             Selector selector = ExtractorUtils.getSelector(extractBy);
             fieldExtractor = new FieldExtractor(field, selector, extractBy.source() == ExtractBy.Source.RawHtml ? FieldExtractor.Source.RawHtml : FieldExtractor.Source.Html,
                     extractBy.notNull(), extractBy.multi());
             Method setterMethod = getSetterMethod(clazz, field);
             if (setterMethod != null) {
                 fieldExtractor.setSetterMethod(setterMethod);
             }
         }
         return fieldExtractor;
     }
 
     public static Method getSetterMethod(Class clazz, Field field) {
         String name = "set" + StringUtils.capitalize(field.getName());
         try {
             Method declaredMethod = clazz.getDeclaredMethod(name, field.getType());
             declaredMethod.setAccessible(true);
             return declaredMethod;
         } catch (NoSuchMethodException e) {
             return null;
         }
     }
 
     private void initClassExtractors() {
         Annotation annotation = clazz.getAnnotation(TargetUrl.class);
         if (annotation == null) {
             targetUrlPatterns.add(Pattern.compile(".*"));
         } else {
             TargetUrl targetUrl = (TargetUrl) annotation;
             String[] value = targetUrl.value();
             for (String s : value) {
                 targetUrlPatterns.add(Pattern.compile("(" + s.replace(".", "\\.").replace("*", "[^\"'#]*") + ")"));
             }
             if (!targetUrl.sourceRegion().equals("")) {
                 targetUrlRegionSelector = new XpathSelector(targetUrl.sourceRegion());
             }
         }
         annotation = clazz.getAnnotation(HelpUrl.class);
         if (annotation != null) {
             HelpUrl helpUrl = (HelpUrl) annotation;
             String[] value = helpUrl.value();
             for (String s : value) {
                 helpUrlPatterns.add(Pattern.compile("(" + s.replace(".", "\\.").replace("*", "[^\"'#]*") + ")"));
             }
             if (!helpUrl.sourceRegion().equals("")) {
                 helpUrlRegionSelector = new XpathSelector(helpUrl.sourceRegion());
             }
         }
         annotation = clazz.getAnnotation(ExtractBy.class);
         if (annotation != null) {
             ExtractBy extractBy = (ExtractBy) annotation;
             objectExtractor = new Extractor(new XpathSelector(extractBy.value()), Extractor.Source.Html, extractBy.notNull(), extractBy.multi());
         }
     }
 
     public Object process(Page page) {
         boolean matched = false;
         for (Pattern targetPattern : targetUrlPatterns) {
             if (targetPattern.matcher(page.getUrl().toString()).matches()) {
                 matched = true;
             }
         }
         if (!matched) {
             return null;
         }
         if (objectExtractor == null) {
            return processSingle(page, null, true);
         } else {
             if (objectExtractor.multi) {
                 List<Object> os = new ArrayList<Object>();
                 List<String> list = objectExtractor.getSelector().selectList(page.getHtml().toString());
                 for (String s : list) {
                     Object o = processSingle(page, s, false);
                     if (o != null) {
                         os.add(o);
                     }
                 }
                 return os;
             } else {
                 String select = objectExtractor.getSelector().select(page.getHtml().toString());
                 Object o = processSingle(page, select, false);
                 return o;
             }
         }
     }
 
     private Object processSingle(Page page, String html, boolean isRaw) {
         Object o = null;
         try {
             o = clazz.newInstance();
             for (FieldExtractor fieldExtractor : fieldExtractors) {
                 if (fieldExtractor.isMulti()) {
                     List<String> value;
                     switch (fieldExtractor.getSource()) {
                         case RawHtml:
                             value = page.getHtml().selectDocumentForList(fieldExtractor.getSelector());
                             break;
                         case Html:
                             if (isRaw) {
                                 value = page.getHtml().selectDocumentForList(fieldExtractor.getSelector());
                             } else {
                                 value = fieldExtractor.getSelector().selectList(html);
                             }
                             break;
                         case Url:
                             value = fieldExtractor.getSelector().selectList(page.getUrl().toString());
                             break;
                         default:
                             value = fieldExtractor.getSelector().selectList(html);
                     }
                     if ((value == null || value.size() == 0) && fieldExtractor.isNotNull()) {
                         return null;
                     }
                     setField(o, fieldExtractor, value);
                 } else {
                     String value;
                     switch (fieldExtractor.getSource()) {
                         case RawHtml:
                             value = page.getHtml().selectDocument(fieldExtractor.getSelector());
                             break;
                         case Html:
                             if (isRaw) {
                                 value = page.getHtml().selectDocument(fieldExtractor.getSelector());
                             } else {
                                 value = fieldExtractor.getSelector().select(html);
                             }
                             break;
                         case Url:
                             value = fieldExtractor.getSelector().select(page.getUrl().toString());
                             break;
                         default:
                             value = fieldExtractor.getSelector().select(html);
                     }
                     if (value == null && fieldExtractor.isNotNull()) {
                         return null;
                     }
                     setField(o, fieldExtractor, value);
                 }
             }
             if (AfterExtractor.class.isAssignableFrom(clazz)) {
                 ((AfterExtractor) o).afterProcess(page);
             }
         } catch (InstantiationException e) {
             e.printStackTrace();
         } catch (IllegalAccessException e) {
             e.printStackTrace();
         } catch (InvocationTargetException e) {
             e.printStackTrace();
         }
         return o;
     }
 
     private void setField(Object o, FieldExtractor fieldExtractor, Object value) throws IllegalAccessException, InvocationTargetException {
         if (fieldExtractor.getSetterMethod() != null) {
             fieldExtractor.getSetterMethod().invoke(o, value);
         }
         fieldExtractor.getField().set(o, value);
     }
 
     Class getClazz() {
         return clazz;
     }
 
     List<Pattern> getTargetUrlPatterns() {
         return targetUrlPatterns;
     }
 
     List<Pattern> getHelpUrlPatterns() {
         return helpUrlPatterns;
     }
 
     Selector getTargetUrlRegionSelector() {
         return targetUrlRegionSelector;
     }
 
     Selector getHelpUrlRegionSelector() {
         return helpUrlRegionSelector;
     }
 }
