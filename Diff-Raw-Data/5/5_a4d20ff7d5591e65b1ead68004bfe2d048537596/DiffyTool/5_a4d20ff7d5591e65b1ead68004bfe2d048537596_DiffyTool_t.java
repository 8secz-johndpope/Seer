 package com.bazaarvoice.jolt;
 
 import com.fasterxml.jackson.core.JsonParseException;
 import net.sourceforge.argparse4j.ArgumentParsers;
 import net.sourceforge.argparse4j.impl.Arguments;
 import net.sourceforge.argparse4j.inf.ArgumentParser;
 import net.sourceforge.argparse4j.inf.ArgumentParserException;
 import net.sourceforge.argparse4j.inf.Namespace;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.util.Map;
 
 public class DiffyTool {
 
     public static void main( String[] args ) {
         ArgumentParser parser = ArgumentParsers.newArgumentParser( "diffy" )
                 .description( "Jolt CLI Diffy Tool. This tool will ingest two JSON inputs (from files or standard input) and " +
                         "perform the Jolt Diffy operation to detect any differences. The program will return and exit code of " +
                         "0 if no differences are found or a 1 if a difference is found or an error is encountered." )
                 .defaultHelp( true );
 
         File nullFile = null;
         parser.addArgument( "filePath1" ).help( "File path to feed to Input #1 for the Diffy operation. " +
                 "This file should contain properly formatted JSON." )
                 .type( Arguments.fileType().verifyExists().verifyIsFile().verifyCanRead() );
         parser.addArgument( "filePath2" ).help( "File path to feed to Input #2 for the Diffy operation. " +
                 "This file should contain properly formatted JSON. " +
                 "This argument is mutually exclusive with -i; one or the other should be specified." )
                 .type( Arguments.fileType().verifyExists().verifyIsFile().verifyCanRead() )
                 .nargs( "?" ).setDefault( nullFile );   // these last two method calls make filePath2 optional
 
         parser.addArgument( "-s" ).help( "Diffy will suppress output and run silently." )
                 .action( Arguments.storeTrue() );
         parser.addArgument( "-a" ).help( "Diffy will not consider array order when detecting differences" )
                 .action( Arguments.storeTrue() );
         parser.addArgument( "-i" ).help( "Diffy will use standard in as input for Input #2 rather than the filePath2 argument. " +
                 "Standard in should contain properly formatted JSON." )
                 .action( Arguments.storeTrue() );
 
         Namespace ns = null;
         try {
             ns = parser.parseArgs( args );
         } catch ( ArgumentParserException e ) {
             parser.handleError( e );
             System.exit( 1 );
         }
 
         boolean suppressOutput = ns.getBoolean( "s" );
 
         Diffy diffy;
         if ( ns.getBoolean( "a" ) ) {
             diffy = new ArrayOrderObliviousDiffy();
         } else {
             diffy = new Diffy();
         }
 
         Map<String, Object> objectMap1 = createObjectMapFromFile( (File) ns.get( "filePath1" ), suppressOutput );
         Map<String, Object> objectMap2 = null;
 
         if ( ns.getBoolean( "i" ) ) {
             try {
                 objectMap2 = JsonUtils.jsonToMap( System.in );
             } catch ( IOException e ) {
                 if ( e instanceof JsonParseException ) {
                     printOutput( suppressOutput, "Standard input did not contain properly formatted JSON." );
                 } else {
                     printOutput( suppressOutput, "Failed to process standard input." );
                 }
                 System.exit( 1 );
             }
         } else {
             File file = (File) ns.get( "filePath2" );
             if ( file == null ) {
                 printOutput( suppressOutput, "Second file path is required if standard input (-i) is not utilized." );
                 System.exit( 1 );
             } else {
                 objectMap2 = createObjectMapFromFile( file, suppressOutput );
             }
         }
 
         Diffy.Result result = diffy.diff( objectMap1, objectMap2 );
 
         if ( result.isEmpty() ) {
             printOutput( suppressOutput, "Diffy found no differences" );
             System.exit( 0 );
         } else {
             try {
                printOutput( suppressOutput, "Differences found. Input #1 contained this:\n" +
                         JsonUtils.toPrettyJsonString( result.expected ) + "\n" +
                         "Input #2 contained this:\n" +
                         JsonUtils.toPrettyJsonString( result.actual ) );
             } catch ( IOException e ) {
                printOutput( suppressOutput, "Differences found, but diffy encountered an error while writing the result." );
             } finally {
                 System.exit( 1 );
             }
         }
     }
 
     /**
      * Uses the File to build a Map containing JSON data found in the file. This method will
      * exit with an error code of 1 if has any trouble opening the file or the file did not
      * contain properly formatted JSON (i.e. the JSON parser was unable to parse its contents)
      *
      * @return the Map containing the JSON data
      */
     private static Map<String, Object> createObjectMapFromFile( File file, boolean suppressOutput ) {
         Map<String, Object> objectMap = null;
         try {
             FileInputStream inputStream = new FileInputStream( file );
             objectMap = (Map<String, Object>) JsonUtils.jsonToObject( inputStream );
             inputStream.close();
             return objectMap;
         } catch ( IOException e ) {
             if ( e instanceof JsonParseException ) {
                 printOutput( suppressOutput, "File " + file.getAbsolutePath() + " did not contain properly formatted JSON." );
             } else {
                 printOutput( suppressOutput, "Failed to open file: " + file.getAbsolutePath() );
             }
             System.exit( 1 );
         }
         return objectMap;
     }
 
     /**
      * Prints the given string to standard out, or doesn't, based on the suppressOutput flag
      */
     private static void printOutput( boolean suppressOutput, String output ) {
         if ( !suppressOutput ) {
             System.out.println( output );
         }
     }
 }
