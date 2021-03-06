 /*
  * $Id: Editor.java 3508 2008-05-01 15:11:42Z uckelman $
  *
  * Copyright (c) 2008 by Joel Uckelman 
  *
  * This library is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Library General Public
  * License (LGPL) as published by the Free Software Foundation.
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Library General Public License for more details.
  *
  * You should have received a copy of the GNU Library General Public
  * License along with this library; if not, copies are available
  * at http://www.opensource.org.
  */
 
 package VASSAL.launch;
 
 import java.io.File;
 import java.io.Serializable;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 
 import gnu.getopt.Getopt;
 import gnu.getopt.LongOpt;
 
 import VASSAL.build.module.AbstractMetaData;
 import VASSAL.i18n.Resources;
 import VASSAL.Info;
 
 /**
  * Encapsulates and parses command-line arguments.
  * <code>args</code> and <code>LaunchRequest.parseArgs(args).toArgs()</code>
  * are equivalent (though perhaps not equal) argument lists.
  *
  * @author Joel Uckelman
  * @since 3.1.0
  */
 public class LaunchRequest implements Serializable {
   private static final long serialVersionUID = 1L;
 
   enum Mode {
     MANAGE   { public String toString() { return "manage"; } },
     LOAD     { public String toString() { return "load";   } },
     EDIT     { public String toString() { return "edit";   } },
     IMPORT   { public String toString() { return "import"; } },
     NEW      { public String toString() { return "new";    } },
     EDIT_EXT { public String toString() { return "edit-extension"; } },
     NEW_EXT  { public String toString() { return "new-extension";  } }
   }
 
   public Mode mode;
   public File module;
   public File game;
   public File extension;
 
   public boolean standalone;
 
   public boolean builtInModule;
   public List<String> autoext;
   public List<String> extract;
 
   public LaunchRequest() {
     this(null, null);
   }
 
   public LaunchRequest(Mode mode) {
     this(mode, null, null);
   }
 
   public LaunchRequest(Mode mode, File module) {
     this(mode, module, null);
   }
 
   public LaunchRequest(Mode mode, File module, File other) {
     this.mode = mode;
     this.module = module;
 
     if (mode == Mode.EDIT_EXT) extension = other;
     else game = other;
   }
 
   /**
    * Create an argument array equivalent to this <code>LaunchRequest</code>.
    *
    * @return an array which would be parsed to this <code>LaunchRequest</code>
    */
   public String[] toArgs() {
     final ArrayList<String> args = new ArrayList<String>();
 
     args.add("--" + mode.toString());
 
     if (standalone)    args.add("--standalone");
     if (builtInModule) args.add("--auto");
 
     if (autoext != null) {
       final StringBuilder sb = new StringBuilder("--auto-extensions=");
 
       final Iterator<String> i = autoext.iterator();
       sb.append(i.next());
       while (i.hasNext()) sb.append(',').append(i.next());
       args.add(sb.toString().replace(' ','_'));
     }
 
     if (extract != null) {
       for (String x : extract) {
         args.add("--extract=" + x);
       }
     }
 
     args.add("--");
 
     if (module != null) {
       args.add(module.getPath());
       if (game != null) {
         args.add(game.getPath());
       }
       else if (extension != null) {
         args.add(extension.getPath());
       }
     }
 
     return args.toArray(new String[args.size()]);
   }
 
   // FIXME: translate this somehow?
   private static final String help =
 "Usage:\n" +
 "  VASSAL -e [option]... module\n" +
 "  VASSAL -i [option]... module\n" +
 "  VASSAL -l [option]... module|save|log...\n" +
 "  VASSAL -n [option]...\n" +
 "  VASSAL -m\n" +
 "  VASSAL -h\n" +
 "  VASSAL --edit-extension [option]... module|extension...\n" +
 "  VASSAL --new-extension [option]...\n" +
 "\n" +
 "Options:\n" +
 "  -a, --auto          TODO\n" +
 "  -e, --edit          Edit a module\n" +
 "  -h, --help          Display this help and exit\n" +
 "  -i, --import        Import a non-VASSAL module\n" +
 "  -l, --load          Load a module and saved game or log\n" +
 "  -m, --manage        Use the module manager\n" +
 "  -n, --new           Create a new module\n" +
 "  -x, --extract       TODO\n" +
 "  --auto-extensions   TODO\n" +
 "  --edit-extension    Edit a module extension\n" +
 "  --new-extension     Create a new module extension\n" +
 //"  --standalone        run the Player or Editor alone, debugging use only\n" +
 "  --version           Display version information and exit\n" +
 "  --                  Terminate the list of options\n" +
 "\n" +
 "VASSAL defaults to '-m' if no options are given.\n" +
 "\n";
 
   /**
    * Parse an argument array to a <code>LaunchRequest</code>.
    * 
    * @param args an array of command-line arguments
    * @return a <code>LaunchRequest</code> equivalent to <code>args</code>
    */ 
   public static LaunchRequest parseArgs(String[] args) {
     final LaunchRequest lr = new LaunchRequest();
 
     final int AUTO_EXT = 2;
     final int EDIT_EXT = 3;
     final int NEW_EXT = 4;
     final int STANDALONE = 5;
     final int VERSION = 6;
 
     final LongOpt[] longOpts = new LongOpt[]{
       new LongOpt("auto",    LongOpt.NO_ARGUMENT,       null, 'a'),
       new LongOpt("edit",    LongOpt.NO_ARGUMENT,       null, 'e'),
       new LongOpt("extract", LongOpt.REQUIRED_ARGUMENT, null, 'x'), 
       new LongOpt("help",    LongOpt.NO_ARGUMENT,       null, 'h'),
       new LongOpt("import",  LongOpt.NO_ARGUMENT,       null, 'i'),
       new LongOpt("load",    LongOpt.NO_ARGUMENT,       null, 'l'),
       new LongOpt("manage",  LongOpt.NO_ARGUMENT,       null, 'm'),
       new LongOpt("new",     LongOpt.NO_ARGUMENT,       null, 'n'),
       new LongOpt("auto-extensions", LongOpt.REQUIRED_ARGUMENT, null, AUTO_EXT),
       new LongOpt("edit-extension", LongOpt.NO_ARGUMENT, null, EDIT_EXT),
       new LongOpt("new-extension", LongOpt.NO_ARGUMENT, null, NEW_EXT),
       new LongOpt("standalone", LongOpt.NO_ARGUMENT, null, STANDALONE),
       new LongOpt("version", LongOpt.NO_ARGUMENT, null, VERSION)
     };
 
     final Getopt g = new Getopt("VASSAL", args, ":aehilmnx:", longOpts);
     g.setOpterr(false);
 
     int c;
     String optarg;
     while ((c = g.getopt()) != -1) {
       switch (c) {
       case AUTO_EXT:
         if (lr.autoext == null) lr.autoext = new ArrayList<String>();
         for (String ext : g.getOptarg().split(",")) {
           lr.autoext.add(ext.replace("_"," "));
         }
         break;
       case EDIT_EXT:
         setMode(lr, Mode.EDIT_EXT);
         break;
       case NEW_EXT:
         setMode(lr, Mode.NEW_EXT);
         break;
       case STANDALONE:
         lr.standalone = true;
         break;
       case VERSION:
         System.err.println("VASSAL " + Info.getVersion());
         System.exit(0);
         break;
       case 'a':
         lr.builtInModule = true;
         break;
       case 'e':
         setMode(lr, Mode.EDIT);
         break;
       case 'h':
         System.err.print(help);
         System.exit(0);
         break;
       case 'i':
         setMode(lr, Mode.IMPORT);
         break;
       case 'l':
         setMode(lr, Mode.LOAD);
         break;
       case 'm':
         setMode(lr, Mode.MANAGE);
         break;
       case 'n':
         setMode(lr, Mode.NEW);
         break;
       case 'x':
         if (lr.extract == null) lr.extract = new ArrayList<String>();
         lr.extract.add(g.getOptarg());
         break;
       case ':':
         die("LaunchRequest.missing_argument", args[g.getOptind()-1]); 
         break;
       case '?':
         die("LaunchRequest.unrecognized_option", args[g.getOptind()-1]);
         break;
       default:
         // should never happen
         throw new IllegalStateException();  
       }
     }
 
     int i = g.getOptind();
 
     // load by default if a non-option argument is given; otherwise, manage
     if (lr.mode == null) {
       lr.mode = i < args.length ? Mode.LOAD : Mode.MANAGE;
     }
 
     // get the module and game, if specified
     switch (lr.mode) {
     case MANAGE:
       break;
     case LOAD:
       while (i < args.length) {
         final File file = new File(args[i++]);
         switch (AbstractMetaData.getFileType(file)) {
         case MODULE:
           if (lr.module != null)
             die("LaunchRequest.only_one", "module");
           lr.module = file;
           break;
         case EXTENSION:
           if (lr.extension != null) die("");
           lr.extension = file;
           break;
         case SAVE:
           if (lr.game != null)
             die("LaunchRequest.only_one", "saved game or log");
           lr.game = file;
           break;
         case UNKNOWN:
           die("LaunchRequest.unknown_file_type", file.toString());
           break;
         }
       }
 
       if (lr.module == null && lr.game == null) {
         die("LaunchRequest.missing_module");
       }
       break;
     case IMPORT:
    	lr.module = new File(args[i++]);
     	break;
     case EDIT:
     case NEW_EXT:
       if (i < args.length) {
         final File file = new File(args[i++]);
         switch (AbstractMetaData.getFileType(file)) {
         case MODULE:
           lr.module = file;
           break;
         case EXTENSION:
         case SAVE:
         case UNKNOWN:
           die("LaunchRequest.unknown_file_type", file.toString());
           break;
         }
       }
       else {
         die("LaunchRequest.missing_module");
       }
       break;
     case EDIT_EXT:
       while (i < args.length) {
         final File file = new File(args[i++]);
         switch (AbstractMetaData.getFileType(file)) {
         case MODULE:
           if (lr.module != null)
             die("LaunchRequest.only_one", "module");
           lr.module = file;
           break;
         case EXTENSION:
           if (lr.extension != null) die("");
           lr.extension = file;
           break;
         case SAVE:
         case UNKNOWN:
           die("LaunchRequest.unknown_file_type", file.toString());
           break;
         }
       }
 
       if (lr.module == null) {
         die("LaunchRequest.missing_module");
       }
       
       if (lr.extension == null) {
         die("LaunchRequest.missing_extension");
       }
       break;
     case NEW:
       break;
     }
 
     if (i < args.length) {
       die("LaunchRequest.excess_args", args[i]);
     }   
  
     // other consistency checks
     if (lr.builtInModule) {
       if (lr.mode != Mode.LOAD) {
         die("LaunchRequest.only_in_mode", "--auto", Mode.LOAD.toString());
       }      
 
       if (lr.module != null) {
         die("LaunchRequest.excess_args", args[i]);
       }
     }  
 
     if (lr.autoext != null) {
       if (lr.mode != Mode.LOAD) {
         die("LaunchRequest.only_in_mode",
             "--auto-extensions", Mode.LOAD.toString());
       }      
 
       if (lr.module != null) {
         die("LaunchRequest.excess_args", args[i]);
       }
     }
 
     if (lr.standalone && lr.mode == Mode.MANAGE) {
       die("LaunchRequest.not_in_mode", "--standalone", Mode.MANAGE.toString());
     } 
 
     return lr;
   }
 
   protected static void setMode(LaunchRequest lr, Mode mode) {
     if (lr.mode != null) die("LaunchRequest.only_one", "mode");
     lr.mode = mode; 
   }
 
   /** 
    * Print an error message and exit.
    *
    * @param key {@link Resources} key
    * @param vals {@link Resources} arguments
    */
   protected static void die(String key, String... vals) {
     System.err.println("VASSAL: " + Resources.getString(key, (Object[]) vals));
     System.exit(1);
   }
 }
