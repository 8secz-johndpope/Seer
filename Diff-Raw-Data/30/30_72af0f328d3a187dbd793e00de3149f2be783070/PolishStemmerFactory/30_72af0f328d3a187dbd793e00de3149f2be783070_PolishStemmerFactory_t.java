 
 /*
  * Carrot2 project.
  *
  * Copyright (C) 2002-2009, Dawid Weiss, Stanisław Osiński.
  * All rights reserved.
  *
  * Refer to the full license file "carrot2.LICENSE"
  * in the root folder of the repository checkout or at:
  * http://www.carrot2.org/carrot2.LICENSE
  */
 
 package org.carrot2.text.linguistic;
 
 import org.apache.log4j.Logger;
import java.util.List;

import morfologik.stemming.WordData;

 import org.carrot2.util.ReflectionUtils;
 
 
 /**
  * Factory of {@link IStemmer} implementations from the {@link LanguageCode#POLISH}
  * language. If <a href="http://morfologik.blogspot.com/">Morfologik-stemming</a> library
  * is available in classpath, a wrapper around this library is returned. Otherwise an
  * empty identity stemmer is returned.
  */
 final class PolishStemmerFactory
 {
     private final static Logger logger = Logger.getLogger(PolishStemmerFactory.class); 
 
     private final static IStemmer stemmer;
     static
     {
         stemmer = createStemmerInternal();
         if (stemmer instanceof IdentityStemmer)
         {
             logger.warn("Morfologik classes not available in classpath.");
         }
     }
 
     /**
      * An adapter converting Snowball programs into {@link IStemmer} interface.
      */
     private static class MorfologikStemmerAdapter implements IStemmer
     {
        private final morfologik.stemming.IStemmer stemmer;
 
         public MorfologikStemmerAdapter()
             throws Exception
         {
            final String stemmerClazzName = "morfologik.stemming.PolishStemmer";
 
            final Class<? extends morfologik.stemming.IStemmer> stemmerClazz = 
                 ReflectionUtils.classForName(stemmerClazzName)
                .asSubclass(morfologik.stemming.IStemmer.class);
 
             this.stemmer = stemmerClazz.newInstance();
         }
 
         public CharSequence stem(CharSequence word)
         {
            final List<WordData> stems = stemmer.lookup(word);
            if (stems == null || stems.size() == 0)
             {
                 return null;
             }
             else
             {
                return stems.get(0).getStem().toString();
             }
         }
     }
 
     /*
      * 
      */
     public static IStemmer createStemmer()
     {
         return stemmer;
     }
     
     /**
      * Attempts to instantiate <code>morfologik-stemming</code> (Lametyzator) stemmer.
      */
     private static IStemmer createStemmerInternal()
     {
         try
         {
             return new MorfologikStemmerAdapter();
         }
         catch (Throwable e)
         {
             return new IdentityStemmer();
         }
     }
 }
