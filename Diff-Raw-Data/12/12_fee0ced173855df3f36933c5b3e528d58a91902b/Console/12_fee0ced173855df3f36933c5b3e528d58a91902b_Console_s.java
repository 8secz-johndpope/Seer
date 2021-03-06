 package com.tinkerpop.gremlin.console;
 
 import com.tinkerpop.gremlin.Gremlin;
 import jline.History;
 import org.codehaus.groovy.tools.shell.Groovysh;
 import org.codehaus.groovy.tools.shell.IO;
 import org.codehaus.groovy.tools.shell.InteractiveShellRunner;
 
 import java.io.File;
 
 /**
  * @author Marko A. Rodriguez (http://markorodriguez.com)
  */
 public class Console {
 
     private static final String HISTORY_FILE = ".gremlin_history";
 
    public static void main(final String[] args) throws Exception {
         IO io = new IO(System.in, System.out, System.err);
         io.out.println();
         io.out.println("         \\,,,/");
         io.out.println("         (o o)");
         io.out.println("-----oOOo-(_)-oOOo-----");
 
         final Groovysh groovy = new Groovysh();
         groovy.setResultHook(new NullResultHookClosure(groovy));
         groovy.execute("import com.tinkerpop.gremlin.*");
         groovy.execute("import com.tinkerpop.gremlin.Tokens.T");
         groovy.execute("import com.tinkerpop.blueprints.pgm.*");
         groovy.execute("import com.tinkerpop.blueprints.pgm.impls.tg.*");
         groovy.execute("import com.tinkerpop.blueprints.pgm.impls.neo4j.*");
         groovy.execute("import com.tinkerpop.blueprints.pgm.impls.orientdb.*");
         groovy.execute("import com.tinkerpop.blueprints.pgm.impls.rexster.*");
         groovy.execute("import com.tinkerpop.blueprints.pgm.impls.sail.*");
         groovy.execute("import com.tinkerpop.blueprints.pgm.impls.sail.impls.*");
         groovy.execute("import com.tinkerpop.blueprints.pgm.util.*");
         groovy.execute("import com.tinkerpop.blueprints.pgm.util.graphml.*");
         groovy.setResultHook(new ResultHookClosure(groovy, io));
         groovy.setHistory(new History());
 
         final InteractiveShellRunner runner = new InteractiveShellRunner(groovy, new PromptClosure(groovy));
         runner.setErrorHandler(new ErrorHookClosure(runner, io));
        runner.setHistory(new History(new File(HISTORY_FILE)));
        //runner.setHistoryFile(new File(HISTORY_FILE));
 
         new Gremlin();
         try {
             runner.run();
         } catch (Error e) {
             System.exit(0);
         }
     }
 }
