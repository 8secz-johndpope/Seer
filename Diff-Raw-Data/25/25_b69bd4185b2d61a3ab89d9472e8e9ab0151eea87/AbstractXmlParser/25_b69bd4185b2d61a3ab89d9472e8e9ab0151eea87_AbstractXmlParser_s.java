 package org.apache.maven.doxia.parser;
 
 /*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied.  See the License for the
  * specific language governing permissions and limitations
  * under the License.
  */
 
 import java.io.IOException;
 import java.io.Reader;
 import java.io.StringReader;
 
 import org.apache.maven.doxia.macro.MacroExecutionException;
 import org.apache.maven.doxia.markup.XmlMarkup;
 import org.apache.maven.doxia.sink.Sink;
 import org.codehaus.plexus.util.xml.pull.MXParser;
 import org.codehaus.plexus.util.xml.pull.XmlPullParser;
 import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
 
 /**
  * An abstract class that defines some convenience methods for <code>XML</code> parsers.
  *
  * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
  * @version $Id$
  * @since 1.0
  */
 public abstract class AbstractXmlParser
     extends AbstractParser
     implements XmlMarkup
 {
     /** {@inheritDoc} */
     public void parse( Reader source, Sink sink )
         throws ParseException
     {
         try
         {
             XmlPullParser parser = new MXParser();
 
             parser.setInput( source );
 
             parseXml( parser, sink );
         }
         catch ( XmlPullParserException ex )
         {
             throw new ParseException( "Error parsing the model: " + ex.getMessage(), ex );
         }
         catch ( MacroExecutionException ex )
         {
             throw new ParseException( "Macro execution failed: " + ex.getMessage(), ex );
         }
     }
 
     /**
      * Convenience method to parse an arbitrary string and emit any xml events into the given sink.
      *
      * @param string A string that provides the source input. The string has to be completely
      * enclosed inside one xml root element, otherwise a ParseException is thrown.
      * @param sink A sink that consumes the Doxia events.
      * @throws ParseException if the string does not represent a well-formed xml snippet.
      */
     public void parse( String string, Sink sink )
         throws ParseException
     {
         parse( new StringReader( string ), sink );
     }
 
     /** {@inheritDoc} */
     public final int getType()
     {
         return XML_TYPE;
     }
 
     /**
      * Parse the model from the XmlPullParser into the given sink.
      *
      * @param parser A parser.
      * @param sink the sink to receive the events.
      * @throws XmlPullParserException if there's a problem parsing the model
      * @throws MacroExecutionException if there's a problem executing a macro
      */
     private void parseXml( XmlPullParser parser, Sink sink )
         throws XmlPullParserException, MacroExecutionException
     {
         int eventType = parser.getEventType();
 
         while ( eventType != XmlPullParser.END_DOCUMENT )
         {
             if ( eventType == XmlPullParser.START_TAG )
             {
                 handleStartTag( parser, sink );
             }
             else if ( eventType == XmlPullParser.END_TAG )
             {
                 handleEndTag( parser, sink );
             }
             else if ( eventType == XmlPullParser.TEXT )
             {
                 handleText( parser, sink );
             }
             else if ( eventType == XmlPullParser.CDSECT )
             {
                 handleCdsect( parser, sink );
             }
             else if ( eventType == XmlPullParser.COMMENT )
             {
                 handleComment( parser, sink );
             }
             else if ( eventType == XmlPullParser.ENTITY_REF )
             {
                 handleEntity( parser, sink );
             }
 
             try
             {
                 eventType = parser.nextToken();
             }
             catch ( IOException io )
             {
                 throw new XmlPullParserException( "IOException: " + io.getMessage(), parser, io );
             }
         }
     }
 
     /**
      * Goes through the possible start tags.
      *
      * @param parser A parser.
      * @param sink the sink to receive the events.
      * @throws XmlPullParserException if there's a problem parsing the model
      * @throws MacroExecutionException if there's a problem executing a macro
      */
     protected abstract void handleStartTag( XmlPullParser parser, Sink sink )
         throws XmlPullParserException, MacroExecutionException;
 
     /**
      * Goes through the possible end tags.
      *
      * @param parser A parser.
      * @param sink the sink to receive the events.
      * @throws XmlPullParserException if there's a problem parsing the model
      * @throws MacroExecutionException if there's a problem executing a macro
      */
     protected abstract void handleEndTag( XmlPullParser parser, Sink sink )
         throws XmlPullParserException, MacroExecutionException;
 
     /**
      * Handles text events.
      *
      * @param parser A parser.
      * @param sink the sink to receive the events.
      * @throws XmlPullParserException if there's a problem parsing the model
      */
     protected abstract void handleText( XmlPullParser parser, Sink sink )
         throws XmlPullParserException;
 
     /**
      * Handles CDATA sections.
      *
      * @param parser A parser.
      * @param sink the sink to receive the events.
      * @throws XmlPullParserException if there's a problem parsing the model
      */
     protected abstract void handleCdsect( XmlPullParser parser, Sink sink )
         throws XmlPullParserException;
 
     /**
      * Handles comments.
      *
      * @param parser A parser.
      * @param sink the sink to receive the events.
      * @throws XmlPullParserException if there's a problem parsing the model
      */
     protected abstract void handleComment( XmlPullParser parser, Sink sink )
         throws XmlPullParserException;
 
     /**
      * Handles entities.
      *
      * @param parser A parser.
      * @param sink the sink to receive the events.
      * @throws XmlPullParserException if there's a problem parsing the model
      */
     protected abstract void handleEntity( XmlPullParser parser, Sink sink )
         throws XmlPullParserException;
 
 }
