 
 /* Soot - a J*va Optimization Framework
  * Copyright (C) 2003 Ondrej Lhotak
  *
  * This library is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 2.1 of the License, or (at your option) any later version.
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this library; if not, write to the
  * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
  * Boston, MA 02111-1307, USA.
  */
 
 /* THIS FILE IS AUTO-GENERATED FROM soot_options.xml. DO NOT MODIFY. */
 
 package soot.options;
 import soot.*;
 import java.util.*;
 import soot.PackManager;
 
 /** Soot command-line options parser.
  * @author Ondrej Lhotak
  */
 
 public class Options extends OptionsBase {
     public Options(Singletons.Global g) { }
     public static Options v() { return G.v().Options(); }
 
 
     public static final int src_prec_c = 1;
     public static final int src_prec_class = 1;
     public static final int src_prec_J = 2;
     public static final int src_prec_jimple = 2;
     public static final int output_format_J = 1;
     public static final int output_format_jimple = 1;
     public static final int output_format_j = 2;
     public static final int output_format_jimp = 2;
     public static final int output_format_S = 3;
     public static final int output_format_shimple = 3;
     public static final int output_format_s = 4;
     public static final int output_format_shimp = 4;
     public static final int output_format_B = 5;
     public static final int output_format_baf = 5;
     public static final int output_format_b = 6;
     public static final int output_format_G = 7;
     public static final int output_format_grimple = 7;
     public static final int output_format_g = 8;
     public static final int output_format_grimp = 8;
     public static final int output_format_X = 9;
     public static final int output_format_xml = 9;
     public static final int output_format_n = 10;
     public static final int output_format_none = 10;
     public static final int output_format_jasmin = 11;
     public static final int output_format_c = 12;
     public static final int output_format_class = 12;
     public static final int output_format_d = 13;
     public static final int output_format_dava = 13;
 
     public boolean parse( String[] argv ) {
         for( int i = argv.length; i > 0; i-- ) {
             pushOptions( argv[i-1] );
         }
         while( hasMoreOptions() ) {
             String option = nextOption();
             if( option.charAt(0) != '-' ) {
                 classes.add( option );
                 continue;
             }
             while( option.charAt(0) == '-' ) {
                 option = option.substring(1);
             }
             if( false );
 
             else if( false 
             || option.equals( "h" )
             || option.equals( "help" )
             )
                 help = true;
   
             else if( false 
             || option.equals( "pl" )
             || option.equals( "phase-list" )
             )
                 phase_list = true;
   
             else if( false
             || option.equals( "ph" )
             || option.equals( "phase-help" )
             ) {
                 if( !hasMoreOptions() ) {
                     G.v().out.println( "No value given for option -"+option );
                     return false;
                 }
                 String value = nextOption();
     
                 if( phase_help == null )
                     phase_help = new LinkedList();
 
                 phase_help.add( value );
             }
   
             else if( false 
             || option.equals( "version" )
             )
                 version = true;
   
             else if( false 
             || option.equals( "v" )
             || option.equals( "verbose" )
             )
                 verbose = true;
   
             else if( false 
             || option.equals( "app" )
             )
                 app = true;
   
             else if( false 
             || option.equals( "w" )
             || option.equals( "whole-program" )
             )
                 whole_program = true;
   
             else if( false 
             || option.equals( "debug" )
             )
                 debug = true;
   
             else if( false
             || option.equals( "cp" )
             || option.equals( "soot-class-path" )
             || option.equals( "soot-classpath" )
             ) {
                 if( !hasMoreOptions() ) {
                     G.v().out.println( "No value given for option -"+option );
                     return false;
                 }
                 String value = nextOption();
     
                 if( soot_classpath.length() == 0 )
                     soot_classpath = value;
                 else {
                     G.v().out.println( "Duplicate values "+soot_classpath+" and "+value+" for option -"+option );
                     return false;
                 }
             }
   
             else if( false
             || option.equals( "src-prec" )
             ) {
                 if( !hasMoreOptions() ) {
                     G.v().out.println( "No value given for option -"+option );
                     return false;
                 }
                 String value = nextOption();
     
                 if( false );
     
                 else if( false
                 || value.equals( "c" )
                 || value.equals( "class" )
                 ) {
                     if( src_prec != 0
                     && src_prec != src_prec_class ) {
                         G.v().out.println( "Multiple values given for option "+option );
                         return false;
                     }
                     src_prec = src_prec_class;
                 }
     
                 else if( false
                 || value.equals( "J" )
                 || value.equals( "jimple" )
                 ) {
                     if( src_prec != 0
                     && src_prec != src_prec_jimple ) {
                         G.v().out.println( "Multiple values given for option "+option );
                         return false;
                     }
                     src_prec = src_prec_jimple;
                 }
     
                 else {
                     G.v().out.println( "Invalid value "+value+" given for option -"+option );
                     return false;
                 }
            }
   
             else if( false 
             || option.equals( "allow-phantom-refs" )
             )
                 allow_phantom_refs = true;
   
             else if( false
             || option.equals( "d" )
             || option.equals( "output-dir" )
             ) {
                 if( !hasMoreOptions() ) {
                     G.v().out.println( "No value given for option -"+option );
                     return false;
                 }
                 String value = nextOption();
     
                 if( output_dir.length() == 0 )
                     output_dir = value;
                 else {
                     G.v().out.println( "Duplicate values "+output_dir+" and "+value+" for option -"+option );
                     return false;
                 }
             }
   
             else if( false
             || option.equals( "f" )
             || option.equals( "output-format" )
             ) {
                 if( !hasMoreOptions() ) {
                     G.v().out.println( "No value given for option -"+option );
                     return false;
                 }
                 String value = nextOption();
     
                 if( false );
     
                 else if( false
                 || value.equals( "J" )
                 || value.equals( "jimple" )
                 ) {
                     if( output_format != 0
                     && output_format != output_format_jimple ) {
                         G.v().out.println( "Multiple values given for option "+option );
                         return false;
                     }
                     output_format = output_format_jimple;
                 }
     
                 else if( false
                 || value.equals( "j" )
                 || value.equals( "jimp" )
                 ) {
                     if( output_format != 0
                     && output_format != output_format_jimp ) {
                         G.v().out.println( "Multiple values given for option "+option );
                         return false;
                     }
                     output_format = output_format_jimp;
                 }
     
                 else if( false
                 || value.equals( "S" )
                 || value.equals( "shimple" )
                 ) {
                     if( output_format != 0
                     && output_format != output_format_shimple ) {
                         G.v().out.println( "Multiple values given for option "+option );
                         return false;
                     }
                     output_format = output_format_shimple;
                 }
     
                 else if( false
                 || value.equals( "s" )
                 || value.equals( "shimp" )
                 ) {
                     if( output_format != 0
                     && output_format != output_format_shimp ) {
                         G.v().out.println( "Multiple values given for option "+option );
                         return false;
                     }
                     output_format = output_format_shimp;
                 }
     
                 else if( false
                 || value.equals( "B" )
                 || value.equals( "baf" )
                 ) {
                     if( output_format != 0
                     && output_format != output_format_baf ) {
                         G.v().out.println( "Multiple values given for option "+option );
                         return false;
                     }
                     output_format = output_format_baf;
                 }
     
                 else if( false
                 || value.equals( "b" )
                 ) {
                     if( output_format != 0
                     && output_format != output_format_b ) {
                         G.v().out.println( "Multiple values given for option "+option );
                         return false;
                     }
                     output_format = output_format_b;
                 }
     
                 else if( false
                 || value.equals( "G" )
                 || value.equals( "grimple" )
                 ) {
                     if( output_format != 0
                     && output_format != output_format_grimple ) {
                         G.v().out.println( "Multiple values given for option "+option );
                         return false;
                     }
                     output_format = output_format_grimple;
                 }
     
                 else if( false
                 || value.equals( "g" )
                 || value.equals( "grimp" )
                 ) {
                     if( output_format != 0
                     && output_format != output_format_grimp ) {
                         G.v().out.println( "Multiple values given for option "+option );
                         return false;
                     }
                     output_format = output_format_grimp;
                 }
     
                 else if( false
                 || value.equals( "X" )
                 || value.equals( "xml" )
                 ) {
                     if( output_format != 0
                     && output_format != output_format_xml ) {
                         G.v().out.println( "Multiple values given for option "+option );
                         return false;
                     }
                     output_format = output_format_xml;
                 }
     
                 else if( false
                 || value.equals( "n" )
                 || value.equals( "none" )
                 ) {
                     if( output_format != 0
                     && output_format != output_format_none ) {
                         G.v().out.println( "Multiple values given for option "+option );
                         return false;
                     }
                     output_format = output_format_none;
                 }
     
                 else if( false
                 || value.equals( "jasmin" )
                 ) {
                     if( output_format != 0
                     && output_format != output_format_jasmin ) {
                         G.v().out.println( "Multiple values given for option "+option );
                         return false;
                     }
                     output_format = output_format_jasmin;
                 }
     
                 else if( false
                 || value.equals( "c" )
                 || value.equals( "class" )
                 ) {
                     if( output_format != 0
                     && output_format != output_format_class ) {
                         G.v().out.println( "Multiple values given for option "+option );
                         return false;
                     }
                     output_format = output_format_class;
                 }
     
                 else if( false
                 || value.equals( "d" )
                 || value.equals( "dava" )
                 ) {
                     if( output_format != 0
                     && output_format != output_format_dava ) {
                         G.v().out.println( "Multiple values given for option "+option );
                         return false;
                     }
                     output_format = output_format_dava;
                 }
     
                 else {
                     G.v().out.println( "Invalid value "+value+" given for option -"+option );
                     return false;
                 }
            }
   
             else if( false 
             || option.equals( "via-grimp" )
             )
                 via_grimp = true;
   
             else if( false 
             || option.equals( "xml-attributes" )
             )
                 xml_attributes = true;
   
             else if( false
             || option.equals( "p" )
             || option.equals( "phase-option" )
             ) {
                 if( !hasMoreOptions() ) {
                     G.v().out.println( "No phase name given for option -"+option );
                     return false;
                 }
                 String phaseName = nextOption();
                 if( !hasMoreOptions() ) {
                     G.v().out.println( "No phase option given for option -"+option+" "+phaseName );
                     return false;
                 }
                 String phaseOption = nextOption();
     
                if( !setPhaseOption( phaseName, phaseOption ) )
                    return false;
             }
   
             else if( false
             || option.equals( "O" )
             || option.equals( "optimize" )
             ) {
                 
                 pushOptions( "enabled:true" );
                 pushOptions( "sop" );
                 pushOptions( "-p" );
                 pushOptions( "enabled:true" );
                 pushOptions( "jop" );
                 pushOptions( "-p" );
                 pushOptions( "enabled:true" );
                 pushOptions( "gop" );
                 pushOptions( "-p" );
                 pushOptions( "enabled:true" );
                 pushOptions( "bop" );
                 pushOptions( "-p" );
                 pushOptions( "only-stack-locals:false" );
                 pushOptions( "gb.a2" );
                 pushOptions( "-p" );
                 pushOptions( "only-stack-locals:false" );
                 pushOptions( "gb.a1" );
                 pushOptions( "-p" );
             }
   
             else if( false
             || option.equals( "W" )
             || option.equals( "whole-optimize" )
             ) {
                 
                 pushOptions( "-w" );
                 pushOptions( "enabled:true" );
                 pushOptions( "wjop" );
                 pushOptions( "-p" );
             }
   
             else if( false 
             || option.equals( "via-shimple" )
             )
                 via_shimple = true;
   
             else if( false
             || option.equals( "process-path" )
             ) {
                 if( !hasMoreOptions() ) {
                     G.v().out.println( "No value given for option -"+option );
                     return false;
                 }
                 String value = nextOption();
     
                 if( process_path == null )
                     process_path = new LinkedList();
 
                 process_path.add( value );
             }
   
             else if( false
             || option.equals( "i" )
             || option.equals( "include" )
             ) {
                 if( !hasMoreOptions() ) {
                     G.v().out.println( "No value given for option -"+option );
                     return false;
                 }
                 String value = nextOption();
     
                 if( include == null )
                     include = new LinkedList();
 
                 include.add( value );
             }
   
             else if( false
             || option.equals( "x" )
             || option.equals( "exclude" )
             ) {
                 if( !hasMoreOptions() ) {
                     G.v().out.println( "No value given for option -"+option );
                     return false;
                 }
                 String value = nextOption();
     
                 if( exclude == null )
                     exclude = new LinkedList();
 
                 exclude.add( value );
             }
   
             else if( false
             || option.equals( "dynamic-classes" )
             ) {
                 if( !hasMoreOptions() ) {
                     G.v().out.println( "No value given for option -"+option );
                     return false;
                 }
                 String value = nextOption();
     
                 if( dynamic_classes == null )
                     dynamic_classes = new LinkedList();
 
                 dynamic_classes.add( value );
             }
   
             else if( false
             || option.equals( "dynamic-path" )
             ) {
                 if( !hasMoreOptions() ) {
                     G.v().out.println( "No value given for option -"+option );
                     return false;
                 }
                 String value = nextOption();
     
                 if( dynamic_path == null )
                     dynamic_path = new LinkedList();
 
                 dynamic_path.add( value );
             }
   
             else if( false
             || option.equals( "dynamic-package" )
             ) {
                 if( !hasMoreOptions() ) {
                     G.v().out.println( "No value given for option -"+option );
                     return false;
                 }
                 String value = nextOption();
     
                 if( dynamic_package == null )
                     dynamic_package = new LinkedList();
 
                 dynamic_package.add( value );
             }
   
             else if( false 
             || option.equals( "keep-line-number" )
             )
                 keep_line_number = true;
   
             else if( false 
             || option.equals( "keep-bytecode-offset" )
             || option.equals( "keep-offset" )
             )
                 keep_offset = true;
   
             else if( false
             || option.equals( "annot-nullpointer" )
             ) {
                 
                 pushOptions( "enabled:true" );
                 pushOptions( "tag.an" );
                 pushOptions( "-p" );
                 pushOptions( "enabled:true" );
                 pushOptions( "jap.npc" );
                 pushOptions( "-p" );
             }
   
             else if( false
             || option.equals( "annot-arraybounds" )
             ) {
                 
                 pushOptions( "enabled:true" );
                 pushOptions( "tag.an" );
                 pushOptions( "-p" );
                 pushOptions( "enabled:true" );
                 pushOptions( "jap.abc" );
                 pushOptions( "-p" );
                 pushOptions( "enabled:true" );
                 pushOptions( "wjap.ra" );
                 pushOptions( "-p" );
             }
   
             else if( false
             || option.equals( "annot-side-effect" )
             ) {
                 
                 pushOptions( "enabled:true" );
                 pushOptions( "tag.dep" );
                 pushOptions( "-p" );
                 pushOptions( "enabled:true" );
                 pushOptions( "jap.sea" );
                 pushOptions( "-p" );
                 pushOptions( "-w" );
             }
   
             else if( false
             || option.equals( "annot-fieldrw" )
             ) {
                 
                 pushOptions( "enabled:true" );
                 pushOptions( "tag.fieldrw" );
                 pushOptions( "-p" );
                 pushOptions( "enabled:true" );
                 pushOptions( "jap.fieldrw" );
                 pushOptions( "-p" );
                 pushOptions( "-w" );
             }
   
             else if( false 
             || option.equals( "time" )
             )
                 time = true;
   
             else if( false 
             || option.equals( "subtract-gc" )
             )
                 subtract_gc = true;
   
             else {
                 G.v().out.println( "Invalid option -"+option );
                 return false;
             }
         }
         return true;
     }
 
     public boolean help() { return help; }
     private boolean help = false;
     public void set_help( boolean setting ) { help = setting; }
   
     public boolean phase_list() { return phase_list; }
     private boolean phase_list = false;
     public void set_phase_list( boolean setting ) { phase_list = setting; }
   
     public List phase_help() { 
         if( phase_help == null )
             return java.util.Collections.EMPTY_LIST;
         else
             return phase_help;
     }
     public void set_phase_help( List setting ) { phase_help = setting; }
     private List phase_help = null;
     public boolean version() { return version; }
     private boolean version = false;
     public void set_version( boolean setting ) { version = setting; }
   
     public boolean verbose() { return verbose; }
     private boolean verbose = false;
     public void set_verbose( boolean setting ) { verbose = setting; }
   
     public boolean app() { return app; }
     private boolean app = false;
     public void set_app( boolean setting ) { app = setting; }
   
     public boolean whole_program() { return whole_program; }
     private boolean whole_program = false;
     public void set_whole_program( boolean setting ) { whole_program = setting; }
   
     public boolean debug() { return debug; }
     private boolean debug = false;
     public void set_debug( boolean setting ) { debug = setting; }
   
     public String soot_classpath() { return soot_classpath; }
     public void set_soot_classpath( String setting ) { soot_classpath = setting; }
     private String soot_classpath = "";
     public int src_prec() {
         if( src_prec == 0 ) return src_prec_class;
         return src_prec; 
     }
     public void set_src_prec( int setting ) { src_prec = setting; }
     private int src_prec = 0;
     public boolean allow_phantom_refs() { return allow_phantom_refs; }
     private boolean allow_phantom_refs = false;
     public void set_allow_phantom_refs( boolean setting ) { allow_phantom_refs = setting; }
   
     public String output_dir() { return output_dir; }
     public void set_output_dir( String setting ) { output_dir = setting; }
     private String output_dir = "";
     public int output_format() {
         if( output_format == 0 ) return output_format_class;
         return output_format; 
     }
     public void set_output_format( int setting ) { output_format = setting; }
     private int output_format = 0;
     public boolean via_grimp() { return via_grimp; }
     private boolean via_grimp = false;
     public void set_via_grimp( boolean setting ) { via_grimp = setting; }
   
     public boolean xml_attributes() { return xml_attributes; }
     private boolean xml_attributes = false;
     public void set_xml_attributes( boolean setting ) { xml_attributes = setting; }
   
     public boolean via_shimple() { return via_shimple; }
     private boolean via_shimple = false;
     public void set_via_shimple( boolean setting ) { via_shimple = setting; }
   
     public List process_path() { 
         if( process_path == null )
             return java.util.Collections.EMPTY_LIST;
         else
             return process_path;
     }
     public void set_process_path( List setting ) { process_path = setting; }
     private List process_path = null;
     public List include() { 
         if( include == null )
             return java.util.Collections.EMPTY_LIST;
         else
             return include;
     }
     public void set_include( List setting ) { include = setting; }
     private List include = null;
     public List exclude() { 
         if( exclude == null )
             return java.util.Collections.EMPTY_LIST;
         else
             return exclude;
     }
     public void set_exclude( List setting ) { exclude = setting; }
     private List exclude = null;
     public List dynamic_classes() { 
         if( dynamic_classes == null )
             return java.util.Collections.EMPTY_LIST;
         else
             return dynamic_classes;
     }
     public void set_dynamic_classes( List setting ) { dynamic_classes = setting; }
     private List dynamic_classes = null;
     public List dynamic_path() { 
         if( dynamic_path == null )
             return java.util.Collections.EMPTY_LIST;
         else
             return dynamic_path;
     }
     public void set_dynamic_path( List setting ) { dynamic_path = setting; }
     private List dynamic_path = null;
     public List dynamic_package() { 
         if( dynamic_package == null )
             return java.util.Collections.EMPTY_LIST;
         else
             return dynamic_package;
     }
     public void set_dynamic_package( List setting ) { dynamic_package = setting; }
     private List dynamic_package = null;
     public boolean keep_line_number() { return keep_line_number; }
     private boolean keep_line_number = false;
     public void set_keep_line_number( boolean setting ) { keep_line_number = setting; }
   
     public boolean keep_offset() { return keep_offset; }
     private boolean keep_offset = false;
     public void set_keep_offset( boolean setting ) { keep_offset = setting; }
   
     public boolean time() { return time; }
     private boolean time = false;
     public void set_time( boolean setting ) { time = setting; }
   
     public boolean subtract_gc() { return subtract_gc; }
     private boolean subtract_gc = false;
     public void set_subtract_gc( boolean setting ) { subtract_gc = setting; }
   
 
     public String getUsage() {
         return ""
 
 +"\nGeneral Options:\n"
       
 +padOpt(" -h -help", "Display Help and Exit" )
 +padOpt(" -pl -phase-list", "Print list of available phases" )
 +padOpt(" -ph ARG -phase-help ARG", "Print help for specified phase" )
 +padOpt(" -version", "Display Version Information and Exit" )
 +padOpt(" -v -verbose", "Verbose Mode" )
 +padOpt(" -app", "Run in Application Mode" )
 +padOpt(" -w -whole-program", "Run in Whole-program Mode" )
 +padOpt(" -debug", "Prints Various Soot Debugging Info" )
 +"\nInput Options:\n"
       
 +padOpt(" -cp ARG -soot-class-path ARG -soot-classpath ARG", "Uses given PATH as the classpath for finding classes for Soot processing." )
 +padOpt(" -src-prec ARG", "Sets Source Precedence for Soot" )
 +padVal(" c class", "" )
 +padVal(" J jimple", "" )
 +padOpt(" -allow-phantom-refs", "Allow unresolved classes: may cause errors" )
 +"\nOutput Options:\n"
       
 +padOpt(" -d ARG -output-dir ARG", "Store Produced Files in PATH" )
 +padOpt(" -f ARG -output-format ARG", "Sets Output Format for Soot" )
 +padVal(" J jimple", "" )
 +padVal(" j jimp", "" )
 +padVal(" S shimple", "" )
 +padVal(" s shimp", "" )
 +padVal(" B baf", "" )
 +padVal(" b", "" )
 +padVal(" G grimple", "" )
 +padVal(" g grimp", "" )
 +padVal(" X xml", "" )
 +padVal(" n none", "" )
 +padVal(" jasmin", "" )
 +padVal(" c class", "" )
 +padVal(" d dava", "" )
 +padOpt(" -via-grimp", "Convert Jimple to Bytecode via Grimp Instead of via Baf" )
 +padOpt(" -xml-attributes", "Save tags to XML attributes for Eclipse" )
 +"\nProcessing Options:\n"
       
 +padOpt(" -p PHASE-NAME PHASE-OPTIONS -phase-option PHASE-NAME PHASE-OPTIONS", "set run-time option KEY to VALUE for PHASE-NAME" )
 +padOpt(" -O -optimize", "perform scalar optimizations on the classfiles" )
 +padOpt(" -W -whole-optimize", "perform whole program optimizations on the classfiles" )
 +padOpt(" -via-shimple", "enables phases operating on Shimple SSA representation" )
 +"\nSingle File Mode Options:\n"
       
 +padOpt(" -process-path ARG", "Process all Classes on the PATH" )
 +"\nApplication Mode Options:\n"
       
 +padOpt(" -i ARG -include ARG", "Marks Classfiles in PACKAGE (e.g. java.util.) as Application Classes" )
 +padOpt(" -x ARG -exclude ARG", "Marks Classfiles in PACKAGE (e.g. java.) as Context Classes" )
 +padOpt(" -dynamic-classes ARG", "Marks CLASSES (separated by colons) as Potentially Dynamic Classes" )
 +padOpt(" -dynamic-path ARG", "Marks all Class Files in PATH as Potentially Dynamic Classes" )
 +padOpt(" -dynamic-package ARG", "Marks Class Files in PACKAGES (separated by commas) as Potentially Dynamic Classes" )
 +"\nInput Attribute Options:\n"
       
 +padOpt(" -keep-line-number", "Keep Line Number Tables" )
 +padOpt(" -keep-bytecode-offset -keep-offset", "Attach Bytecode Offset to Jimple Statement" )
 +"\nAnnotation Options:\n"
       
 +padOpt(" -annot-nullpointer", "Turn on the Annotation for Null Pointer" )
 +padOpt(" -annot-arraybounds", "Turn on the Annotation for Array Bounds Check" )
 +padOpt(" -annot-side-effect", "Turn on Side-effect Attributes" )
 +padOpt(" -annot-fieldrw", "Turn on Field Read/Write Attributes" )
 +"\nMiscellaneous Options:\n"
       
 +padOpt(" -time", "Print out Time Statistics about Tranformations" )
 +padOpt(" -subtract-gc", "Attempt to Subtract the gc from the Time Stats" );
     }
 
 
     public String getPhaseList() {
         return ""
     
         +padOpt("jb", "Create a JimpleBody for each method")
         +padVal("jb.ls", "Associates separate locals with each use-def web")
         +padVal("jb.a", "Removes some unnecessary copies")
         +padVal("jb.ule", "Removes unused locals")
         +padVal("jb.tr", "Assigns types to locals")
         +padVal("jb.ulp", "Minimizes number of locals")
         +padVal("jb.lns", "Gives names to locals")
         +padVal("jb.cp", "Removes unnecessary copies")
         +padVal("jb.dae", "")
         +padVal("jb.cp-ule", "Removes unused locals")
         +padVal("jb.lp", "Minimizes number of locals")
         +padVal("jb.ne", "")
         +padVal("jb.uce", "")
         +padOpt("cg", "Build a call graph")
         +padVal("cg.cha", "Build a call graph using Class Hierarchy Analysis")
         +padVal("cg.spark", "Spark points-to analysis framework")
         +padOpt("wjtp", "")
         +padOpt("wjop", "")
         +padVal("wjop.smb", "")
         +padVal("wjop.si", "")
         +padOpt("wjap", "")
         +padVal("wjap.ra", "")
         +padOpt("shimple", "General Shimple options.")
         +padOpt("stp", "")
         +padOpt("sop", "")
         +padVal("sop.cpf", "")
         +padOpt("jtp", "")
         +padOpt("jop", "")
         +padVal("jop.cse", "")
         +padVal("jop.bcm", "")
         +padVal("jop.lcm", "")
         +padVal("jop.cp", "Removes unnecessary copies")
         +padVal("jop.cpf", "")
         +padVal("jop.cbf", "")
         +padVal("jop.dae", "")
         +padVal("jop.uce1", "")
         +padVal("jop.ubf1", "")
         +padVal("jop.uce2", "")
         +padVal("jop.ubf2", "")
         +padVal("jop.ule", "Removes unused locals")
         +padOpt("jap", "")
         +padVal("jap.npc", "")
         +padVal("jap.abc", "")
         +padVal("jap.profiling", "")
         +padVal("jap.sea", "")
         +padVal("jap.fieldrw", "")
         +padVal("jap.cgtagger", "")
         +padOpt("gb", "")
         +padVal("gb.a1", "")
         +padVal("gb.cf", "")
         +padVal("gb.a2", "")
         +padVal("gb.ule", "Removes unused locals")
         +padOpt("gop", "")
         +padOpt("bb", "")
         +padVal("bb.lso", "")
         +padVal("bb.pho", "")
         +padVal("bb.ule", "Removes unused locals")
         +padVal("bb.lp", "Minimizes number of locals")
         +padOpt("bop", "")
         +padOpt("tag", "")
         +padVal("tag.ln", "")
         +padVal("tag.an", "")
         +padVal("tag.dep", "")
         +padVal("tag.fieldrw", "");
     }
 
     public String getPhaseHelp( String phaseName ) {
     
         if( phaseName.equals( "jb" ) )
             return "Phase "+phaseName+":\n"+
                 "\nJimple Body Creation creates a JimpleBody for each input \nmethod, using either coffi, to read .class files, or the jimple \nparser, to read .jimple files. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" )
                 +padOpt( "use-original-names", "" );
     
         if( phaseName.equals( "jb.ls" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Local Splitter identifies use-def webs for local variables \nand introduces new variables so that each disjoint web is \nassociated with a single local. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "jb.a" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Jimple Local Aggregator removes some unnecessary copies by \ncombining some local variables. Essentially, it finds \ndefinitions which have only a single use and, if it is safe to \ndo so, removes the original definition after replacing the use \nwith the definition's right-hand side. At this stage in \nJimpleBody construction, local aggregation serves largely to \nremove the copies to and from stack variables which simulate \nload and store instructions in the original bytecode."
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" )
                 +padOpt( "only-stack-locals (true)", "" );
     
         if( phaseName.equals( "jb.ule" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Unused Local Eliminator phase removes any unused locals \nfrom the method. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "jb.tr" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Type Assigner gives local variables types which will \naccommodate the values stored in the variable by the method. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "jb.ulp" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Unsplit-originals Local Packer executes only when the \n`use-original-names' option is chosen for the `jb' phase. The \nLocal Packer attempts to minimize the number of local variables \nrequired in a method by reusing the same variable for disjoint \ndef-use webs. Conceptually, it is the inverse of the Local \nSplitter. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" )
                 +padOpt( "unsplit-original-locals (true)", "" );
     
         if( phaseName.equals( "jb.lns" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Local Name Standardizer assigns generic names to local \nvariables. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" )
                 +padOpt( "only-stack-locals", "" );
     
         if( phaseName.equals( "jb.cp" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThis phase performs cascaded copy propagation. If the \npropagator encounters situations of the form: A: a = ...; \n... B: x = a; ... C: ... = ... x; where a and x are \neach defined only once (at A and B, respectively), then it can \npropagate immediately without checking between B and C for \nredefinitions of a. In this case the propagator is global. \nOtherwise, if a has multiple definitions then the propagator \nchecks for redefinitions and propagates copies only within \nextended basic blocks. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" )
                 +padOpt( "only-regular-locals", "" )
                 +padOpt( "only-stack-locals (true)", "" );
     
         if( phaseName.equals( "jb.dae" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Dead Assignment Eliminator eliminates assignment statements \nto locals whose values are not subsequently used, unless \nevaluating the right-hand side of the assignment may cause \nside-effects. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" )
                 +padOpt( "only-stack-locals (true)", "" );
     
         if( phaseName.equals( "jb.cp-ule" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThis phase removes any locals that are unused as a result of \ncopy propagation. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "jb.lp" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Local Packer attempts to minimize the number of local \nvariables required in a method by reusing the same variable for \ndisjoint def-use webs. Conceptually, it is the inverse of the \nLocal Splitter. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (false)", "" )
                 +padOpt( "unsplit-original-locals (false)", "" );
     
         if( phaseName.equals( "jb.ne" ) )
             return "Phase "+phaseName+":\n"+
                 "\nRemoves nop statements from the method. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "jb.uce" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Unreachable Code Eliminator removes unreachable code and \ntraps whose catch blocks are empty. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "cg" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe purpose of this pack is to compute a call graph. When this \npack finishes, a call graph is available in the Scene. The \ndifferent phases in this pack are different ways to construct \nthe call graph. Exactly one phase in this pack may be enabled; \nSoot will raise an error otherwise. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" )
                 +padOpt( "safe-forname (false)", "Handle Class.forName() calls conservatively" )
                 +padOpt( "verbose (false)", "Print warnings about where the call graph may be incomplete" );
     
         if( phaseName.equals( "cg.cha" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThis phase generates uses Class Hierarchy Analysis to generate a \ncall graph."
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" )
                 +padOpt( "verbose (false)", "Print statistics about the resulting call graph" );
     
         if( phaseName.equals( "cg.spark" ) )
             return "Phase "+phaseName+":\n"+
                 "\nSpark is a flexible points-to analysis framework. Aside from \nbuilding a call graph, it also generates information about the \ntargets of pointers. For details about Spark, please see Ondrej \nLhotak's M.Sc. thesis."
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (false)", "" )
                 +padOpt( "verbose (false)", "Print detailed information about the execution of Spark" )
                 +padOpt( "ignore-types (false)", "Make Spark completely ignore declared types of variables" )
                 +padOpt( "force-gc (false)", "Force garbage collection for measuring memory usage" )
                 +padOpt( "pre-jimplify (false)", "Jimplify all methods before starting Spark" )
                 +padOpt( "vta (false)", "Emulate Variable Type Analysis" )
                 +padOpt( "rta (false)", "Emulate Rapid Type Analysis" )
                 +padOpt( "field-based (false)", "Use a field-based rather than field-sensitive representation" )
                 +padOpt( "types-for-sites (false)", "Represent objects by their actual type rather than allocation site" )
                 +padOpt( "merge-stringbuffer (true)", "Represent all StringBuffers as one object" )
                 +padOpt( "simulate-natives (true)", "Simulate effects of native methods in standard class library" )
                 +padOpt( "simple-edges-bidirectional (false)", "Equality-based analysis between variable nodes" )
                 +padOpt( "on-fly-cg (true)", "Build call graph as receiver types become known" )
                 +padOpt( "parms-as-fields (false)", "Represent method parameters as fields of this" )
                 +padOpt( "returns-as-fields (false)", "Represent method return values as fields of this" )
                 +padOpt( "simplify-offline (false)", "Collapse single-entry subgraphs of the PAG" )
                 +padOpt( "simplify-sccs (false)", "Collapse strongly-connected components of the PAG" )
                 +padOpt( "ignore-types-for-sccs (false)", "Ignore declared types when determining node equivalence for SCCs" )
                 +padOpt( "propagator", "Select propagation algorithm" )
                 +padVal( "iter", "Simple iterative algorithm" )
                 
                 +padVal( "worklist (default)", "Fast, worklist-based algorithm" )
                 
                 +padVal( "cycle", "Unfinished on-the-fly cycle detection algorithm" )
                 
                 +padVal( "merge", "Unfinished field reference merging algorithms" )
                 
                 +padVal( "alias", "Alias-edge based algorithm" )
                 
                 +padVal( "none", "Disable propagation" )
                 
                 +padOpt( "set-impl", "Select points-to set implementation" )
                 +padVal( "hash", "Use Java HashSet" )
                 
                 +padVal( "bit", "Bit vector" )
                 
                 +padVal( "hybrid", "Hybrid representation using bit vector for large sets" )
                 
                 +padVal( "array", "Sorted array representation" )
                 
                 +padVal( "double (default)", "Double set representation for incremental propagation" )
                 
                 +padVal( "shared", "Shared bit-vector representation" )
                 
                 +padOpt( "double-set-old", "Select implementation of points-to set for old part of double set" )
                 +padVal( "hash", "Use Java HashSet" )
                 
                 +padVal( "bit", "Bit vector" )
                 
                 +padVal( "hybrid (default)", "Hybrid representation using bit vector for large sets" )
                 
                 +padVal( "array", "Sorted array representation" )
                 
                 +padVal( "shared", "Shared bit-vector representation" )
                 
                 +padOpt( "double-set-new", "Select implementation of points-to set for new part of double set" )
                 +padVal( "hash", "Use Java HashSet" )
                 
                 +padVal( "bit", "Bit vector" )
                 
                 +padVal( "hybrid (default)", "Hybrid representation using bit vector for large sets" )
                 
                 +padVal( "array", "Sorted array representation" )
                 
                 +padVal( "shared", "Shared bit-vector representation" )
                 
                 +padOpt( "dump-html (false)", "Dump pointer assignment graph to HTML for debugging" )
                 +padOpt( "dump-pag (false)", "Dump pointer assignment graph for other solvers" )
                 +padOpt( "dump-solution (false)", "Dump final solution for comparison with other solvers" )
                 +padOpt( "topo-sort (false)", "Sort variable nodes in dump" )
                 +padOpt( "dump-types (true)", "Include declared types in dump" )
                 +padOpt( "class-method-var (true)", "In dump, label variables by class and method" )
                 +padOpt( "dump-answer (false)", "Dump computed reaching types for comparison with other solvers" )
                 +padOpt( "add-tags (false)", "Output points-to results in tags for viewing with the Jimple" )
                 +padOpt( "set-mass (false)", "Calculate statistics about points-to set sizes" );
     
         if( phaseName.equals( "wjtp" ) )
             return "Phase "+phaseName+":\n"+
                 "\nSoot can do whole-program analyses. For the current version of \nSoot, this means that Jimple bodies are created for each method \nin the application, and analyses run on this set of Jimple \nbodies. The application consists of one class, specified on the \ncommand-line, plus all classes referenced (directly or \nindirectly) by it. It excludes classes in java.*, javax.*, and \nsun.*. This mode is triggered by the --app option. In \nwhole-program mode, Soot applies the contents of the \nWhole-Jimple Transformation Pack to each method under analysis. \nThis occurs after all Jimple bodies have been created. In an \nunmodified version of Soot, the only transformation in wjtp is \nthe Spark pointer analysis kit. Spark has many options, which \nare listed at spark.ps. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "wjop" ) )
             return "Phase "+phaseName+":\n"+
                 "\nTo run optimizing transformations on the whole program, use the \n-W command-line option. This tells Soot to apply Whole-Jimple \nOptimization Pack. The default behaviour of this Pack has \nstatic method binding disabled and static inlining enabled. To \nreverse this, give the options -p wjop.smb enabled:true -p \nwjop.si disabled. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (false)", "" );
     
         if( phaseName.equals( "wjop.smb" ) )
             return "Phase "+phaseName+":\n"+
                 "\nStatic method binding uses CHA or VTA to statically bind \nmonomorphic call sites. That is, smb takes the call graph \nreturned by CHA or VTA; if the analysis result shows that any \nvirtual invoke statement in the Jimple bodies actually only \ncalls one method, then a static copy of the method is made, and \nthe virtual invoke is changed to a static invoke. \n"
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (false)", "" )
                 +padOpt( "insert-null-checks (true)", "" )
                 +padOpt( "insert-redundant-casts (true)", "" )
                 +padOpt( "allowed-modifier-changes", "" )
                 +padVal( "unsafe (default)", "" )
                 
                 +padVal( "safe", "" )
                 
                 +padVal( "none", "" )
                 ;
     
         if( phaseName.equals( "wjop.si" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe StaticInliner takes an call graph returned by CHA or VTA \nand visits all call sites in the application in a bottom-up \nfashion, inlining invoke statements which is determined to be \nmonomorphic by analysis result. Note that the modifier \n``static'' is supposed to be compared to a \n(not-currently-implemented) profile-guided inliner. \n"
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" )
                 +padOpt( "insert-null-checks (true)", "" )
                 +padOpt( "insert-redundant-casts (true)", "" )
                 +padOpt( "allowed-modifier-changes", "" )
                 +padVal( "unsafe (default)", "" )
                 
                 +padVal( "safe", "" )
                 
                 +padVal( "none", "" )
                 
                 +padOpt( "expansion-factor (3)", "" )
                 +padOpt( "max-container-size (5000)", "" )
                 +padOpt( "max-inlinee-size (20)", "" );
     
         if( phaseName.equals( "wjap" ) )
             return "Phase "+phaseName+":\n"+
                 "\n"
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "wjap.ra" ) )
             return "Phase "+phaseName+":\n"+
                 "\n"
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (false)", " Find array variables always pointing to rectangular two-dimensional array objects. " );
     
         if( phaseName.equals( "shimple" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThese options affect the Shimple body behaviour. \nCurrently, the only available options affect Shimple during Phi \n	 node elimination."
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" )
                 +padOpt( "phi-elim-opt", "Options affecting Phi node             elimination." )
                 +padVal( "none", "If enabled, no optimizations are applied             before or after eliminating Phi nodes." )
                 
                 +padVal( "pre", "If enabled, some optimizations are applied               before Phi nodes are eliminated." )
                 
                 +padVal( "post (default)", "If enabled, some optimizations are applied after               Phi nodes are eliminated." )
                 
                 +padVal( "pre-and-post", "If enabled, some optimizations are applied             both before and after Phi nodes are eliminated." )
                 ;
     
         if( phaseName.equals( "stp" ) )
             return "Phase "+phaseName+":\n"+
                 "\n"
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "When enabled, Shimple-based transformations are applied." );
     
         if( phaseName.equals( "sop" ) )
             return "Phase "+phaseName+":\n"+
                 "\n"
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (false)", "When enabled, Shimple-based optimizations are           applied." );
     
         if( phaseName.equals( "sop.cpf" ) )
             return "Phase "+phaseName+":\n"+
                 "\n"
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "An example optimization written for 						Shimple." );
     
         if( phaseName.equals( "jtp" ) )
             return "Phase "+phaseName+":\n"+
                 "\n"
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "jop" ) )
             return "Phase "+phaseName+":\n"+
                 "\nWhen Soot is given the -O or -optimize command-line option, it \napplies the Jimple Optimization Pack to every JimpleBody in \napplication classes. This section lists the default \ntransformations in the Jimple Optimization Pack. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (false)", "" );
     
         if( phaseName.equals( "jop.cse" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Common Subexpression Eliminator runs an available \nexpressions analysis on the method body, then eliminates common \nsubexpressions. This implementation is especially slow, as it \nruns on individual statements rather than on basic blocks. A \nbetter implementation (which would find most common \nsubexpressions, but not all) would use basic blocks instead. \nThis implementation is also slow because the flow universe is \nexplicitly created; it need not be. A better implementation \nwould implicitly compute the kill sets at every node. Because \nof the current slowness, this transformation is not enabled by \ndefault. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (false)", "" )
                 +padOpt( "naive-side-effect", "Use a naive side effect analysis even if interprocedural information is available" );
     
         if( phaseName.equals( "jop.bcm" ) )
             return "Phase "+phaseName+":\n"+
                 "\nBusy Code Motion is a straightforward implementation of Partial \nRedundancy Elimination. This implementation is not very \naggressive. Lazy Code Motion is an improved version which \nshould be used instead of Busy Code Motion. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (false)", "" )
                 +padOpt( "naive-side-effect", "Use a naive side effect analysis even if interprocedural information is available" );
     
         if( phaseName.equals( "jop.lcm" ) )
             return "Phase "+phaseName+":\n"+
                 "\nLazy Code Motion is an enhanced version of Busy Code Motion, a \nPartial Redundancy Eliminator. Before doing Partial Redundancy \nElimination, this optimization performs loop inversion (turning \nwhile loops into do while loops inside an if statement). This \nallows the Partial Redundancy Eliminator to optimize loop \ninvariants of while loops. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (false)", "" )
                 +padOpt( "safety", "" )
                 +padVal( "safe (default)", "" )
                 
                 +padVal( "medium", "" )
                 
                 +padVal( "unsafe", "" )
                 
                 +padOpt( "unroll (true)", "" )
                 +padOpt( "naive-side-effect", "Use a naive side effect analysis even if interprocedural information is available" );
     
         if( phaseName.equals( "jop.cp" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThis phase performs cascaded copy propagation."
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" )
                 +padOpt( "only-regular-locals", "" )
                 +padOpt( "only-stack-locals", "" );
     
         if( phaseName.equals( "jop.cpf" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Constant Propagator and Folder evaluates any expressions \nconsisting entirely of compile-time constants, for example 2 * \n3, and replaces the expression with the constant result, in this \ncase 6. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "jop.cbf" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Conditional Branch Folder statically evaluates the \nconditional expression of Jimple IfStmts. If the condition is \nidentically `true' or `false', the Folder replaces the \nconditional branch statement with an unconditional `goto' \nstatement. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "jop.dae" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Dead Assignment Eliminator eliminates assignment statements \nto locals whose values are not subsequently used, unless \nevaluating the right-hand side of the assignment may cause \nside-effects. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" )
                 +padOpt( "only-stack-locals", "" );
     
         if( phaseName.equals( "jop.uce1" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Unreachable Code Eliminator removes unreachable code and \ntraps whose catch blocks are empty. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "jop.ubf1" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Unconditional Branch Folder removes unnecessary `goto' \nstatements from a JimpleBody. If a GotoStmt's target is the next \ninstruction, then it is removed. If a GotoStmt's target is \nanother GotoStmt, with target y, then the first statement's \ntarget is changed to y'. If some IfStmt's target is a GotoStmt, \nthen the IfStmt's target can be updated to the GotoStmt's \ntarget. (These situations can result from other optimizations, \nand folding branches may itself generate more unreachable code.)"
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "jop.uce2" ) )
             return "Phase "+phaseName+":\n"+
                 "\nAnother iteration of the Unreachable Code Eliminator. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "jop.ubf2" ) )
             return "Phase "+phaseName+":\n"+
                 "\nAnother iteration of the Unconditional Branch Folder. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "jop.ule" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Unused Local Eliminator phase removes any unused locals \nfrom the method. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "jap" ) )
             return "Phase "+phaseName+":\n"+
                 "\nSoot has a number of phase options to configure the annotation \nprocess. Array bounds check and null pointer check detection \nhave separate phases and phase options. \n"
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "jap.npc" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe null pointer check analysis has the phase name jtp.npc. It \nhas one phase option (aside from the default disabled option). \n"
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (false)", "" )
                 +padOpt( "only-array-ref", "" )
                 +padOpt( "profiling", "Insert profiling instructions counting the number of safe null pointer accesses." );
     
         if( phaseName.equals( "jap.abc" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe array bounds check analysis has the phase name jtp.abc. If \nwhole-program analysis is required, an extra phase wjap.ra for \nfinding rectangular arrays occurs. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (false)", "" )
                 +padOpt( "with-all", "" )
                 +padOpt( "with-fieldref", "" )
                 +padOpt( "with-arrayref", "" )
                 +padOpt( "with-cse", "" )
                 +padOpt( "with-classfield", "" )
                 +padOpt( "with-rectarray", "" )
                 +padOpt( "profiling", "Profile the results of array bounds check analysis." );
     
         if( phaseName.equals( "jap.profiling" ) )
             return "Phase "+phaseName+":\n"+
                 "\n"
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (false)", "" )
                 +padOpt( "notmainentry (false)", "" );
     
         if( phaseName.equals( "jap.sea" ) )
             return "Phase "+phaseName+":\n"+
                 "\nUses the active invoke graph to produce side-effect attributes \nas described in the Spark thesis, chapter 6. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (false)", "" )
                 +padOpt( "naive (false)", "" );
     
         if( phaseName.equals( "jap.fieldrw" ) )
             return "Phase "+phaseName+":\n"+
                 "\nUses the active invoke graph to produce tags indicating which \nfields may be read or written by each statement, including \ninvoke statements. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (false)", "" )
                 +padOpt( "threshold (100)", "" );
     
         if( phaseName.equals( "jap.cgtagger" ) )
             return "Phase "+phaseName+":\n"+
                 "\n"
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (false)", "" );
     
         if( phaseName.equals( "gb" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Grimp Body Creation phase creates a GrimpBody for each \nsource method. It is run only if the output format is grimp or \ngrimple, or if class files are being output and the via-grimp \noption has been specified. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "gb.a1" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Grimp Pre-folding Aggregator combines some local variables, \nfinding definitions with only a single use and removing the \ndefinition after replacing the use with the definition's \nright-hand side, if it is safe to do so. While the mechanism is \nthe same as that employed by the Jimple Local Aggregator, there \nis more scope for aggregation because of Grimp's more \ncomplicated expressions. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" )
                 +padOpt( "only-stack-locals (true)", "" );
     
         if( phaseName.equals( "gb.cf" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Grimp Constructor Folder combines new statements with the \nspecialinvoke statement that calls the new object's constructor. \nFor example, it turns r2 = new java.util.ArrayList; r2.init(); \ninto r2 = new java.util.ArrayList(); "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "gb.a2" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Grimp Post-folding Aggregator combines local variables \nafter constructors have been folded. Constructor folding \ntypically introduces new opportunities for aggregation, since \nwhen a sequence of instructions like r2 = new \njava.util.ArrayList; r2.init(); r3 = r2 is replaced by r2 = \nnew java.util.ArrayList(); r3 = r2 the invocation of init no \nlonger represents a potential side-effect separating the two \ndefinitions, so they can be combined into r3 = new \njava.util.ArrayList(); (assuming there are no subsequent uses \nof r2"
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" )
                 +padOpt( "only-stack-locals (true)", "" );
     
         if( phaseName.equals( "gb.ule" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThis phase removes any locals that are unused after constructor \nfolding and aggregation. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "gop" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Grimp Optimization pack performs optimizations on \nGrimpBodys (currently there are no optimizations performed \nspecifically on GrimpBodys, and the pack is empty). It is run \nonly if the output format is grimp or grimple, or if class files \nare being output and the via-grimp option has been specified. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (false)", "" );
     
         if( phaseName.equals( "bb" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Baf Body Creation phase creates a BafBody from each source \nmethod. It is run if the output format is baf or b, or if class \nfiles are being output and the via-grimp option has not been \nspecified. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "bb.lso" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Load Store Optimizer replaces some combinations of loads to \nand stores from local variables with stack instructions. A \nsimple example would be the replacement of store.r $r2; load.r \n$r2; with dup1.r in cases where the value of r2 is not used \nsubsequently. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" )
                 +padOpt( "debug", "" )
                 +padOpt( "inter", "" )
                 +padOpt( "sl (true)", "" )
                 +padOpt( "sl2", "" )
                 +padOpt( "sll (true)", "" )
                 +padOpt( "sll2", "" );
     
         if( phaseName.equals( "bb.pho" ) )
             return "Phase "+phaseName+":\n"+
                 "\nApplies peephole optimizations to the Baf intermediate \nrepresentation. Individual optimizations must be implemented by \nclasses implementing the Peephole interface. The Peephole \nOptimizer reads the names of the Peephole classes at runtime \nfrom the file peephole.dat and loads them dynamically. Then it \ncontinues to run apply the Peepholes repeatedly until none of \nthem are able to perform any further optimizations. Soot \nprovides only one Peephole, named ExamplePeephole, which is not \nenabled by the delivered peephole.dat file. ExamplePeephole \nremoves all checkcast instructions."
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "bb.ule" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThis phase removes any locals that are unused after load store \noptimization and peephole optimization. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "bb.lp" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Local Packer attempts to minimize the number of local \nvariables required in a method by reusing the same variable for \ndisjoint def-use webs. Conceptually, it is the inverse of the \nLocal Splitter. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" )
                 +padOpt( "unsplit-original-locals", "" );
     
         if( phaseName.equals( "bop" ) )
             return "Phase "+phaseName+":\n"+
                 "\nThe Baf Optimization pack performs optimizations on BafBodys \n(currently there are no optimizations performed specifically on \nBafBodys, and the pack is empty). It is run only if the output \nformat is baf or b, or if class files are being output and the \nvia-grimp option has not been specified. "
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (false)", "" );
     
         if( phaseName.equals( "tag" ) )
             return "Phase "+phaseName+":\n"+
                 "\n"
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (true)", "" );
     
         if( phaseName.equals( "tag.ln" ) )
             return "Phase "+phaseName+":\n"+
                 "\n"
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (false)", "" );
     
         if( phaseName.equals( "tag.an" ) )
             return "Phase "+phaseName+":\n"+
                 "\n"
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (false)", "" );
     
         if( phaseName.equals( "tag.dep" ) )
             return "Phase "+phaseName+":\n"+
                 "\n"
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (false)", "" );
     
         if( phaseName.equals( "tag.fieldrw" ) )
             return "Phase "+phaseName+":\n"+
                 "\nAggregates field read/write tags produced by the Field \nRead/Write Tagger, phase jap.fieldrw."
                 +"\n\nRecognized options (with default values):\n"
                 +padOpt( "enabled (false)", "" );
     
 
         return "Unrecognized phase: "+phaseName;
     }
   
     public static String getDeclaredOptionsForPhase( String phaseName ) {
     
         if( phaseName.equals( "jb" ) )
             return ""
                 +"enabled "
                 +"use-original-names ";
     
         if( phaseName.equals( "jb.ls" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "jb.a" ) )
             return ""
                 +"enabled "
                 +"only-stack-locals ";
     
         if( phaseName.equals( "jb.ule" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "jb.tr" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "jb.ulp" ) )
             return ""
                 +"enabled "
                 +"unsplit-original-locals ";
     
         if( phaseName.equals( "jb.lns" ) )
             return ""
                 +"enabled "
                 +"only-stack-locals ";
     
         if( phaseName.equals( "jb.cp" ) )
             return ""
                 +"enabled "
                 +"only-regular-locals "
                 +"only-stack-locals ";
     
         if( phaseName.equals( "jb.dae" ) )
             return ""
                 +"enabled "
                 +"only-stack-locals ";
     
         if( phaseName.equals( "jb.cp-ule" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "jb.lp" ) )
             return ""
                 +"enabled "
                 +"unsplit-original-locals ";
     
         if( phaseName.equals( "jb.ne" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "jb.uce" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "cg" ) )
             return ""
                 +"enabled "
                 +"safe-forname "
                 +"verbose ";
     
         if( phaseName.equals( "cg.cha" ) )
             return ""
                 +"enabled "
                 +"verbose ";
     
         if( phaseName.equals( "cg.spark" ) )
             return ""
                 +"enabled "
                 +"verbose "
                 +"ignore-types "
                 +"force-gc "
                 +"pre-jimplify "
                 +"vta "
                 +"rta "
                 +"field-based "
                 +"types-for-sites "
                 +"merge-stringbuffer "
                 +"simulate-natives "
                 +"simple-edges-bidirectional "
                 +"on-fly-cg "
                 +"parms-as-fields "
                 +"returns-as-fields "
                 +"simplify-offline "
                 +"simplify-sccs "
                 +"ignore-types-for-sccs "
                 +"propagator "
                 +"set-impl "
                 +"double-set-old "
                 +"double-set-new "
                 +"dump-html "
                 +"dump-pag "
                 +"dump-solution "
                 +"topo-sort "
                 +"dump-types "
                 +"class-method-var "
                 +"dump-answer "
                 +"add-tags "
                 +"set-mass ";
     
         if( phaseName.equals( "wjtp" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "wjop" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "wjop.smb" ) )
             return ""
                 +"enabled "
                 +"insert-null-checks "
                 +"insert-redundant-casts "
                 +"allowed-modifier-changes ";
     
         if( phaseName.equals( "wjop.si" ) )
             return ""
                 +"enabled "
                 +"insert-null-checks "
                 +"insert-redundant-casts "
                 +"allowed-modifier-changes "
                 +"expansion-factor "
                 +"max-container-size "
                 +"max-inlinee-size ";
     
         if( phaseName.equals( "wjap" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "wjap.ra" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "shimple" ) )
             return ""
                 +"enabled "
                 +"phi-elim-opt ";
     
         if( phaseName.equals( "stp" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "sop" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "sop.cpf" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "jtp" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "jop" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "jop.cse" ) )
             return ""
                 +"enabled "
                 +"naive-side-effect ";
     
         if( phaseName.equals( "jop.bcm" ) )
             return ""
                 +"enabled "
                 +"naive-side-effect ";
     
         if( phaseName.equals( "jop.lcm" ) )
             return ""
                 +"enabled "
                 +"safety "
                 +"unroll "
                 +"naive-side-effect ";
     
         if( phaseName.equals( "jop.cp" ) )
             return ""
                 +"enabled "
                 +"only-regular-locals "
                 +"only-stack-locals ";
     
         if( phaseName.equals( "jop.cpf" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "jop.cbf" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "jop.dae" ) )
             return ""
                 +"enabled "
                 +"only-stack-locals ";
     
         if( phaseName.equals( "jop.uce1" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "jop.ubf1" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "jop.uce2" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "jop.ubf2" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "jop.ule" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "jap" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "jap.npc" ) )
             return ""
                 +"enabled "
                 +"only-array-ref "
                 +"profiling ";
     
         if( phaseName.equals( "jap.abc" ) )
             return ""
                 +"enabled "
                 +"with-all "
                 +"with-fieldref "
                 +"with-arrayref "
                 +"with-cse "
                 +"with-classfield "
                 +"with-rectarray "
                 +"profiling ";
     
         if( phaseName.equals( "jap.profiling" ) )
             return ""
                 +"enabled "
                 +"notmainentry ";
     
         if( phaseName.equals( "jap.sea" ) )
             return ""
                 +"enabled "
                 +"naive ";
     
         if( phaseName.equals( "jap.fieldrw" ) )
             return ""
                 +"enabled "
                 +"threshold ";
     
         if( phaseName.equals( "jap.cgtagger" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "gb" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "gb.a1" ) )
             return ""
                 +"enabled "
                 +"only-stack-locals ";
     
         if( phaseName.equals( "gb.cf" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "gb.a2" ) )
             return ""
                 +"enabled "
                 +"only-stack-locals ";
     
         if( phaseName.equals( "gb.ule" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "gop" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "bb" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "bb.lso" ) )
             return ""
                 +"enabled "
                 +"debug "
                 +"inter "
                 +"sl "
                 +"sl2 "
                 +"sll "
                 +"sll2 ";
     
         if( phaseName.equals( "bb.pho" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "bb.ule" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "bb.lp" ) )
             return ""
                 +"enabled "
                 +"unsplit-original-locals ";
     
         if( phaseName.equals( "bop" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "tag" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "tag.ln" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "tag.an" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "tag.dep" ) )
             return ""
                 +"enabled ";
     
         if( phaseName.equals( "tag.fieldrw" ) )
             return ""
                 +"enabled ";
     
         // The default set of options is just enabled.
         return "enabled";
     }
 
     public static String getDefaultOptionsForPhase( String phaseName ) {
     
         if( phaseName.equals( "jb" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "jb.ls" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "jb.a" ) )
             return ""
               +"enabled:true "
               +"only-stack-locals:true ";
     
         if( phaseName.equals( "jb.ule" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "jb.tr" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "jb.ulp" ) )
             return ""
               +"enabled:true "
               +"unsplit-original-locals:true ";
     
         if( phaseName.equals( "jb.lns" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "jb.cp" ) )
             return ""
               +"enabled:true "
               +"only-stack-locals:true ";
     
         if( phaseName.equals( "jb.dae" ) )
             return ""
               +"enabled:true "
               +"only-stack-locals:true ";
     
         if( phaseName.equals( "jb.cp-ule" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "jb.lp" ) )
             return ""
               +"enabled:false "
               +"unsplit-original-locals:false ";
     
         if( phaseName.equals( "jb.ne" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "jb.uce" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "cg" ) )
             return ""
               +"enabled:true "
               +"safe-forname:false "
               +"verbose:false ";
     
         if( phaseName.equals( "cg.cha" ) )
             return ""
               +"enabled:true "
               +"verbose:false ";
     
         if( phaseName.equals( "cg.spark" ) )
             return ""
               +"enabled:false "
               +"verbose:false "
               +"ignore-types:false "
               +"force-gc:false "
               +"pre-jimplify:false "
               +"vta:false "
               +"rta:false "
               +"field-based:false "
               +"types-for-sites:false "
               +"merge-stringbuffer:true "
               +"simulate-natives:true "
               +"simple-edges-bidirectional:false "
               +"on-fly-cg:true "
               +"parms-as-fields:false "
               +"returns-as-fields:false "
               +"simplify-offline:false "
               +"simplify-sccs:false "
               +"ignore-types-for-sccs:false "
               +"propagator:worklist "
               +"set-impl:double "
               +"double-set-old:hybrid "
               +"double-set-new:hybrid "
               +"dump-html:false "
               +"dump-pag:false "
               +"dump-solution:false "
               +"topo-sort:false "
               +"dump-types:true "
               +"class-method-var:true "
               +"dump-answer:false "
               +"add-tags:false "
               +"set-mass:false ";
     
         if( phaseName.equals( "wjtp" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "wjop" ) )
             return ""
               +"enabled:false ";
     
         if( phaseName.equals( "wjop.smb" ) )
             return ""
               +"enabled:false "
               +"insert-null-checks:true "
               +"insert-redundant-casts:true "
               +"allowed-modifier-changes:unsafe ";
     
         if( phaseName.equals( "wjop.si" ) )
             return ""
               +"enabled:true "
               +"insert-null-checks:true "
               +"insert-redundant-casts:true "
               +"allowed-modifier-changes:unsafe "
               +"expansion-factor:3 "
               +"max-container-size:5000 "
               +"max-inlinee-size:20 ";
     
         if( phaseName.equals( "wjap" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "wjap.ra" ) )
             return ""
               +"enabled:false ";
     
         if( phaseName.equals( "shimple" ) )
             return ""
               +"enabled:true "
               +"phi-elim-opt:post ";
     
         if( phaseName.equals( "stp" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "sop" ) )
             return ""
               +"enabled:false ";
     
         if( phaseName.equals( "sop.cpf" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "jtp" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "jop" ) )
             return ""
               +"enabled:false ";
     
         if( phaseName.equals( "jop.cse" ) )
             return ""
               +"enabled:false ";
     
         if( phaseName.equals( "jop.bcm" ) )
             return ""
               +"enabled:false ";
     
         if( phaseName.equals( "jop.lcm" ) )
             return ""
               +"enabled:false "
               +"safety:safe "
               +"unroll:true ";
     
         if( phaseName.equals( "jop.cp" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "jop.cpf" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "jop.cbf" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "jop.dae" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "jop.uce1" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "jop.ubf1" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "jop.uce2" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "jop.ubf2" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "jop.ule" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "jap" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "jap.npc" ) )
             return ""
               +"enabled:false ";
     
         if( phaseName.equals( "jap.abc" ) )
             return ""
               +"enabled:false ";
     
         if( phaseName.equals( "jap.profiling" ) )
             return ""
               +"enabled:false "
               +"notmainentry:false ";
     
         if( phaseName.equals( "jap.sea" ) )
             return ""
               +"enabled:false "
               +"naive:false ";
     
         if( phaseName.equals( "jap.fieldrw" ) )
             return ""
               +"enabled:false "
               +"threshold:100 ";
     
         if( phaseName.equals( "jap.cgtagger" ) )
             return ""
               +"enabled:false ";
     
         if( phaseName.equals( "gb" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "gb.a1" ) )
             return ""
               +"enabled:true "
               +"only-stack-locals:true ";
     
         if( phaseName.equals( "gb.cf" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "gb.a2" ) )
             return ""
               +"enabled:true "
               +"only-stack-locals:true ";
     
         if( phaseName.equals( "gb.ule" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "gop" ) )
             return ""
               +"enabled:false ";
     
         if( phaseName.equals( "bb" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "bb.lso" ) )
             return ""
               +"enabled:true "
               +"sl:true "
               +"sll:true ";
     
         if( phaseName.equals( "bb.pho" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "bb.ule" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "bb.lp" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "bop" ) )
             return ""
               +"enabled:false ";
     
         if( phaseName.equals( "tag" ) )
             return ""
               +"enabled:true ";
     
         if( phaseName.equals( "tag.ln" ) )
             return ""
               +"enabled:false ";
     
         if( phaseName.equals( "tag.an" ) )
             return ""
               +"enabled:false ";
     
         if( phaseName.equals( "tag.dep" ) )
             return ""
               +"enabled:false ";
     
         if( phaseName.equals( "tag.fieldrw" ) )
             return ""
               +"enabled:false ";
     
         // The default default value is enabled.
         return "enabled";
     }
   
     public void warnForeignPhase( String phaseName ) {
     
         if( phaseName.equals( "jb" ) ) return;
         if( phaseName.equals( "jb.ls" ) ) return;
         if( phaseName.equals( "jb.a" ) ) return;
         if( phaseName.equals( "jb.ule" ) ) return;
         if( phaseName.equals( "jb.tr" ) ) return;
         if( phaseName.equals( "jb.ulp" ) ) return;
         if( phaseName.equals( "jb.lns" ) ) return;
         if( phaseName.equals( "jb.cp" ) ) return;
         if( phaseName.equals( "jb.dae" ) ) return;
         if( phaseName.equals( "jb.cp-ule" ) ) return;
         if( phaseName.equals( "jb.lp" ) ) return;
         if( phaseName.equals( "jb.ne" ) ) return;
         if( phaseName.equals( "jb.uce" ) ) return;
         if( phaseName.equals( "cg" ) ) return;
         if( phaseName.equals( "cg.cha" ) ) return;
         if( phaseName.equals( "cg.spark" ) ) return;
         if( phaseName.equals( "wjtp" ) ) return;
         if( phaseName.equals( "wjop" ) ) return;
         if( phaseName.equals( "wjop.smb" ) ) return;
         if( phaseName.equals( "wjop.si" ) ) return;
         if( phaseName.equals( "wjap" ) ) return;
         if( phaseName.equals( "wjap.ra" ) ) return;
         if( phaseName.equals( "shimple" ) ) return;
         if( phaseName.equals( "stp" ) ) return;
         if( phaseName.equals( "sop" ) ) return;
         if( phaseName.equals( "sop.cpf" ) ) return;
         if( phaseName.equals( "jtp" ) ) return;
         if( phaseName.equals( "jop" ) ) return;
         if( phaseName.equals( "jop.cse" ) ) return;
         if( phaseName.equals( "jop.bcm" ) ) return;
         if( phaseName.equals( "jop.lcm" ) ) return;
         if( phaseName.equals( "jop.cp" ) ) return;
         if( phaseName.equals( "jop.cpf" ) ) return;
         if( phaseName.equals( "jop.cbf" ) ) return;
         if( phaseName.equals( "jop.dae" ) ) return;
         if( phaseName.equals( "jop.uce1" ) ) return;
         if( phaseName.equals( "jop.ubf1" ) ) return;
         if( phaseName.equals( "jop.uce2" ) ) return;
         if( phaseName.equals( "jop.ubf2" ) ) return;
         if( phaseName.equals( "jop.ule" ) ) return;
         if( phaseName.equals( "jap" ) ) return;
         if( phaseName.equals( "jap.npc" ) ) return;
         if( phaseName.equals( "jap.abc" ) ) return;
         if( phaseName.equals( "jap.profiling" ) ) return;
         if( phaseName.equals( "jap.sea" ) ) return;
         if( phaseName.equals( "jap.fieldrw" ) ) return;
         if( phaseName.equals( "jap.cgtagger" ) ) return;
         if( phaseName.equals( "gb" ) ) return;
         if( phaseName.equals( "gb.a1" ) ) return;
         if( phaseName.equals( "gb.cf" ) ) return;
         if( phaseName.equals( "gb.a2" ) ) return;
         if( phaseName.equals( "gb.ule" ) ) return;
         if( phaseName.equals( "gop" ) ) return;
         if( phaseName.equals( "bb" ) ) return;
         if( phaseName.equals( "bb.lso" ) ) return;
         if( phaseName.equals( "bb.pho" ) ) return;
         if( phaseName.equals( "bb.ule" ) ) return;
         if( phaseName.equals( "bb.lp" ) ) return;
         if( phaseName.equals( "bop" ) ) return;
         if( phaseName.equals( "tag" ) ) return;
         if( phaseName.equals( "tag.ln" ) ) return;
         if( phaseName.equals( "tag.an" ) ) return;
         if( phaseName.equals( "tag.dep" ) ) return;
         if( phaseName.equals( "tag.fieldrw" ) ) return;
         G.v().out.println( "Warning: Phase "+phaseName+" is not a standard Soot phase listed in XML files." );
     }
 
     public void warnNonexistentPhase() {
     
         if( !PackManager.v().hasPhase( "jb" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jb" );
         if( !PackManager.v().hasPhase( "jb.ls" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jb.ls" );
         if( !PackManager.v().hasPhase( "jb.a" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jb.a" );
         if( !PackManager.v().hasPhase( "jb.ule" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jb.ule" );
         if( !PackManager.v().hasPhase( "jb.tr" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jb.tr" );
         if( !PackManager.v().hasPhase( "jb.ulp" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jb.ulp" );
         if( !PackManager.v().hasPhase( "jb.lns" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jb.lns" );
         if( !PackManager.v().hasPhase( "jb.cp" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jb.cp" );
         if( !PackManager.v().hasPhase( "jb.dae" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jb.dae" );
         if( !PackManager.v().hasPhase( "jb.cp-ule" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jb.cp-ule" );
         if( !PackManager.v().hasPhase( "jb.lp" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jb.lp" );
         if( !PackManager.v().hasPhase( "jb.ne" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jb.ne" );
         if( !PackManager.v().hasPhase( "jb.uce" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jb.uce" );
         if( !PackManager.v().hasPhase( "cg" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase cg" );
         if( !PackManager.v().hasPhase( "cg.cha" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase cg.cha" );
         if( !PackManager.v().hasPhase( "cg.spark" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase cg.spark" );
         if( !PackManager.v().hasPhase( "wjtp" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase wjtp" );
         if( !PackManager.v().hasPhase( "wjop" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase wjop" );
         if( !PackManager.v().hasPhase( "wjop.smb" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase wjop.smb" );
         if( !PackManager.v().hasPhase( "wjop.si" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase wjop.si" );
         if( !PackManager.v().hasPhase( "wjap" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase wjap" );
         if( !PackManager.v().hasPhase( "wjap.ra" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase wjap.ra" );
         if( !PackManager.v().hasPhase( "shimple" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase shimple" );
         if( !PackManager.v().hasPhase( "stp" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase stp" );
         if( !PackManager.v().hasPhase( "sop" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase sop" );
         if( !PackManager.v().hasPhase( "sop.cpf" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase sop.cpf" );
         if( !PackManager.v().hasPhase( "jtp" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jtp" );
         if( !PackManager.v().hasPhase( "jop" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jop" );
         if( !PackManager.v().hasPhase( "jop.cse" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jop.cse" );
         if( !PackManager.v().hasPhase( "jop.bcm" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jop.bcm" );
         if( !PackManager.v().hasPhase( "jop.lcm" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jop.lcm" );
         if( !PackManager.v().hasPhase( "jop.cp" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jop.cp" );
         if( !PackManager.v().hasPhase( "jop.cpf" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jop.cpf" );
         if( !PackManager.v().hasPhase( "jop.cbf" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jop.cbf" );
         if( !PackManager.v().hasPhase( "jop.dae" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jop.dae" );
         if( !PackManager.v().hasPhase( "jop.uce1" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jop.uce1" );
         if( !PackManager.v().hasPhase( "jop.ubf1" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jop.ubf1" );
         if( !PackManager.v().hasPhase( "jop.uce2" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jop.uce2" );
         if( !PackManager.v().hasPhase( "jop.ubf2" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jop.ubf2" );
         if( !PackManager.v().hasPhase( "jop.ule" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jop.ule" );
         if( !PackManager.v().hasPhase( "jap" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jap" );
         if( !PackManager.v().hasPhase( "jap.npc" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jap.npc" );
         if( !PackManager.v().hasPhase( "jap.abc" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jap.abc" );
         if( !PackManager.v().hasPhase( "jap.profiling" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jap.profiling" );
         if( !PackManager.v().hasPhase( "jap.sea" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jap.sea" );
         if( !PackManager.v().hasPhase( "jap.fieldrw" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jap.fieldrw" );
         if( !PackManager.v().hasPhase( "jap.cgtagger" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase jap.cgtagger" );
         if( !PackManager.v().hasPhase( "gb" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase gb" );
         if( !PackManager.v().hasPhase( "gb.a1" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase gb.a1" );
         if( !PackManager.v().hasPhase( "gb.cf" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase gb.cf" );
         if( !PackManager.v().hasPhase( "gb.a2" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase gb.a2" );
         if( !PackManager.v().hasPhase( "gb.ule" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase gb.ule" );
         if( !PackManager.v().hasPhase( "gop" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase gop" );
         if( !PackManager.v().hasPhase( "bb" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase bb" );
         if( !PackManager.v().hasPhase( "bb.lso" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase bb.lso" );
         if( !PackManager.v().hasPhase( "bb.pho" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase bb.pho" );
         if( !PackManager.v().hasPhase( "bb.ule" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase bb.ule" );
         if( !PackManager.v().hasPhase( "bb.lp" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase bb.lp" );
         if( !PackManager.v().hasPhase( "bop" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase bop" );
         if( !PackManager.v().hasPhase( "tag" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase tag" );
         if( !PackManager.v().hasPhase( "tag.ln" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase tag.ln" );
         if( !PackManager.v().hasPhase( "tag.an" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase tag.an" );
         if( !PackManager.v().hasPhase( "tag.dep" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase tag.dep" );
         if( !PackManager.v().hasPhase( "tag.fieldrw" ) )
             G.v().out.println( "Warning: Options exist for non-existent phase tag.fieldrw" );
     }
   
 }
