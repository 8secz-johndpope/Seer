 /*
  * Copyright (c) 2007-2009 Concurrent, Inc. All Rights Reserved.
  *
  * Project and contact information: http://www.cascading.org/
  *
  * This file is part of the Cascading project.
  *
  * Cascading is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * Cascading is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with Cascading.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package cascading.scheme;
 
 import java.util.Arrays;
 import java.util.regex.Pattern;
 
 import cascading.tap.TapException;
 import cascading.tuple.Fields;
 import cascading.tuple.Tuple;
 import cascading.tuple.Tuples;
 import cascading.util.Util;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 /**
  * Class TextDelimited is a sub-class of {@link TextLine}. It provides direct support for delimited text files, like
  * TAB (\t) or COMMA (,) delimited files. It also optionally allows for quoted values.
  * <p/>
  * By default this {@link Scheme} is both {@code strict} and {@code safe}.
  * <p/>
  * Strict meaning if a line of text does not parse into the expected number of fields, this class will throw a
  * {@link TapException}. If strict is {@code false}, then arbitrarily sized {@link Tuple} instances will be returned.
  * <p/>
  * Safe meaning if a field cannot be coerced into an expected type, a {@code null} will be used for the value.
  * If safe is {@code false}, a {@link TapException} will be thrown.
  * <p/>
  * Also by default, {@code quote} strings are not searched for to improve processing speed. If a file is
  * COMMA delimited but may have COMMA's in a value, the whole value should be surrounded by the quote string, typically
  * double quotes (").
  * <p/>
  * Note all empty fields in a line will be returned as {@code null} unless coerced into a new type.
  *
  * @see TextLine
  */
 public class TextDelimited extends TextLine
   {
   private static final Logger LOG = LoggerFactory.getLogger( TextDelimited.class );
 
   private static final String QUOTED_REGEX_FORMAT = "%2$s(?!(?:[^%1$s%2$s]|[^%1$s]%2$s[^%1$s])+%1$s)";
   private static final String CLEAN_REGEX_FORMAT = "^(?:%1$s)(.*)(?:%1$s)$";
   private static final String ESCAPE_REGEX_FORMAT = "(%1$s%1$s)";
 
   /** Field splitPattern */
   protected Pattern splitPattern;
   /** Field cleanPattern */
   protected Pattern cleanPattern;
   /** Field escapePattern */
   protected Pattern escapePattern;
   /** Field quote */
   private String quote;
   /** Field strict */
   private boolean strict = true;
   /** Field numValues */
   private int numValues;
   /** Field types */
   private Class[] types;
   /** Field safe */
   private boolean safe = true;
 
   /**
    * Constructor TextDelimited creates a new TextDelimited instance.
    *
    * @param fields    of type Fields
    * @param delimiter of type String
    */
   public TextDelimited( Fields fields, String delimiter )
     {
     this( fields, null, delimiter, null, null );
     }
 
   /**
    * Constructor TextDelimited creates a new TextDelimited instance.
    *
    * @param fields    of type Fields
    * @param delimiter of type String
    * @param types     of type Class[]
    */
   public TextDelimited( Fields fields, String delimiter, Class[] types )
     {
     this( fields, null, delimiter, null, types );
     }
 
   /**
    * Constructor TextDelimited creates a new TextDelimited instance.
    *
    * @param fields    of type Fields
    * @param delimiter of type String
    * @param quote     of type String
    * @param types     of type Class[]
    */
   public TextDelimited( Fields fields, String delimiter, String quote, Class[] types )
     {
     this( fields, null, delimiter, quote, types );
     }
 
   /**
    * Constructor TextDelimited creates a new TextDelimited instance.
    *
    * @param fields    of type Fields
    * @param delimiter of type String
    * @param quote     of type String
    * @param types     of type Class[]
    * @param safe      of type boolean
    */
   public TextDelimited( Fields fields, String delimiter, String quote, Class[] types, boolean safe )
     {
     this( fields, null, delimiter, quote, types, safe );
     }
 
   /**
    * Constructor TextDelimited creates a new TextDelimited instance.
    *
    * @param fields          of type Fields
    * @param sinkCompression of type Compress
    * @param delimiter       of type String
    */
   public TextDelimited( Fields fields, Compress sinkCompression, String delimiter )
     {
     this( fields, sinkCompression, delimiter, null, null );
     }
 
   /**
    * Constructor TextDelimited creates a new TextDelimited instance.
    *
    * @param fields          of type Fields
    * @param sinkCompression of type Compress
    * @param delimiter       of type String
    * @param types           of type Class[]
    */
   public TextDelimited( Fields fields, Compress sinkCompression, String delimiter, Class[] types )
     {
     this( fields, sinkCompression, delimiter, null, types );
     }
 
   /**
    * Constructor TextDelimited creates a new TextDelimited instance.
    *
    * @param fields          of type Fields
    * @param sinkCompression of type Compress
    * @param delimiter       of type String
    * @param types           of type Class[]
    * @param safe            of type boolean
    */
   public TextDelimited( Fields fields, Compress sinkCompression, String delimiter, Class[] types, boolean safe )
     {
     this( fields, sinkCompression, delimiter, null, types, safe );
     }
 
   /**
    * Constructor TextDelimited creates a new TextDelimited instance.
    *
    * @param fields    of type Fields
    * @param delimiter of type String
    * @param quote     of type String
    */
   public TextDelimited( Fields fields, String delimiter, String quote )
     {
     this( fields, null, delimiter, quote );
     }
 
   /**
    * Constructor TextDelimited creates a new TextDelimited instance.
    *
    * @param fields          of type Fields
    * @param sinkCompression of type Compress
    * @param delimiter       of type String
    * @param quote           of type String
    */
   public TextDelimited( Fields fields, Compress sinkCompression, String delimiter, String quote )
     {
     this( fields, sinkCompression, delimiter, true, quote, null, true );
     }
 
   /**
    * Constructor TextDelimited creates a new TextDelimited instance.
    *
    * @param fields          of type Fields
    * @param sinkCompression of type Compress
    * @param delimiter       of type String
    * @param quote           of type String
    * @param types           of type Class[]
    */
   public TextDelimited( Fields fields, Compress sinkCompression, String delimiter, String quote, Class[] types )
     {
     this( fields, sinkCompression, delimiter, true, quote, types, true );
     }
 
   /**
    * Constructor TextDelimited creates a new TextDelimited instance.
    *
    * @param fields          of type Fields
    * @param sinkCompression of type Compress
    * @param delimiter       of type String
    * @param quote           of type String
    * @param types           of type Class[]
    * @param safe            of type boolean
    */
   public TextDelimited( Fields fields, Compress sinkCompression, String delimiter, String quote, Class[] types, boolean safe )
     {
     this( fields, sinkCompression, delimiter, true, quote, types, safe );
     }
 
   /**
    * Constructor TextDelimited creates a new TextDelimited instance.
    *
    * @param fields          of type Fields
    * @param sinkCompression of type Compress
    * @param delimiter       of type String
    * @param strict          of type boolean
    * @param quote           of type String
    * @param types           of type Class[]
    * @param safe            of type boolean
    */
   public TextDelimited( Fields fields, Compress sinkCompression, String delimiter, boolean strict, String quote, Class[] types, boolean safe )
     {
     super( sinkCompression );
 
     setSinkFields( fields );
     setSourceFields( fields );
 
     this.strict = strict;
     this.safe = safe;
     this.numValues = fields.size();
 
     if( this.numValues == 0 )
       throw new IllegalArgumentException( "may not be zero declared fields, found: " + fields.printVerbose() );
 
     this.quote = quote;
 
     if( quote == null || quote.isEmpty() )
       splitPattern = Pattern.compile( delimiter );
     else
       splitPattern = Pattern.compile( String.format( QUOTED_REGEX_FORMAT, quote, delimiter ) );
 
     if( quote != null && !quote.isEmpty() )
       {
       cleanPattern = Pattern.compile( String.format( CLEAN_REGEX_FORMAT, quote ) );
       escapePattern = Pattern.compile( String.format( ESCAPE_REGEX_FORMAT, quote ) );
       }
 
     if( types != null && types.length == 0 )
       this.types = null;
 
     if( types != null )
       this.types = Arrays.copyOf( types, types.length );
 
     if( types != null && types.length != fields.size() )
       throw new IllegalArgumentException( "num of types must equal number of fields: " + fields.printVerbose() + ", found: " + types.length );
     }
 
   @Override
   public Tuple source( Object key, Object value )
     {
     Object[] split = splitPattern.split( value.toString(), numValues );
 
     if( strict && split.length != numValues )
       throw new TapException( "did not parse correct number of values from input data, expected: " + numValues + ", got: " + split.length + ":" + Util.join( ",", (String[]) split ) );
 
     if( cleanPattern != null )
       {
       for( int i = 0; i < split.length; i++ )
         {
         split[ i ] = cleanPattern.matcher( (String) split[ i ] ).replaceAll( "$1" );
         split[ i ] = escapePattern.matcher( (String) split[ i ] ).replaceAll( quote );
         }
       }
 
     for( int i = 0; i < split.length; i++ )
       {
       if( ( (String) split[ i ] ).isEmpty() )
         split[ i ] = null;
       }
 
     if( types != null ) // forced null in ctor
       {
      Object[] result = new Object[split.length];

       for( int i = 0; i < split.length; i++ )
         {
         try
           {
          result[ i ] = Tuples.coerce( split[ i ], types[ i ] );
           }
         catch( Exception exception )
           {
          String message = "field " + getSourceFields().get( i ) + " cannot be coerced from : " + result[ i ] + " to: " + types[ i ].getName();
 
          result[ i ] = null;
 
           LOG.warn( message, exception );
 
           if( !safe )
             throw new TapException( message, exception );
           }
         }

      split = result;
       }
 
     return new Tuple( split );
     }
   }
 
