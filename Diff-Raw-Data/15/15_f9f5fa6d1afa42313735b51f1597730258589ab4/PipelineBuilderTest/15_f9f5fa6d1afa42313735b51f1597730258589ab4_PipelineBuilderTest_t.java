 package org.codehaus.plexus.configuration.xml.xstream.pipeline;
 
 import junit.framework.TestCase;
 import org.codehaus.plexus.configuration.PlexusConfiguration;
 import org.codehaus.plexus.configuration.xml.xstream.PlexusXStream;
 import org.codehaus.plexus.configuration.xml.xstream.PlexusTools;
 
 import java.io.StringReader;
 
 /**
  *
  *
  * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
  *
  * @version $Id$
  */
 public class PipelineBuilderTest
     extends TestCase
 {
     public void testObjectBuilderWithSpecifiedImplementation()
         throws Exception
     {
         String configuration =
             "<pipeline>" +
             "  <valves>" +
            "    <valve implementation='org.codehaus.plexus.configuration.xml.xstream.pipeline.FirstValve'>" +
             "      <name>MyLittlePony</name>" +
             "    </valve>" +
            "    <valve implementation='org.codehaus.plexus.configuration.xml.xstream.pipeline.SecondValve'>" +
             "      <name>MyBiggerPony</name>" +
             "    </valve>" +
             "  </valves>" +
             "</pipeline>";
 
         PlexusXStream builder = new PlexusXStream();
 
         PlexusConfiguration c = PlexusTools.buildConfiguration( new StringReader( configuration ) );
 
         Pipeline pipeline = (Pipeline) builder.build( c, Pipeline.class );
 
         Valve firstValve = pipeline.getValve( 0 );
 
         assertNotNull( firstValve );
 
         assertEquals( "first", firstValve.getId() );
 
         assertEquals( "MyLittlePony", firstValve.getName() );
 
         Valve secondValve = pipeline.getValve( 1 );
 
         assertNotNull( secondValve );
 
         assertEquals( "second", secondValve.getId() );
 
         assertEquals( "MyBiggerPony", secondValve.getName() );
     }
 }
