 package com.tinkerpop.gremlin;
 
 import com.tinkerpop.gremlin.compiler.GremlinEvaluator;
 import com.tinkerpop.gremlin.compiler.GremlinLexer;
 import com.tinkerpop.gremlin.compiler.GremlinParser;
 import com.tinkerpop.gremlin.compiler.context.GremlinScriptContext;
 import com.tinkerpop.gremlin.compiler.context.VariableLibrary;
 import org.antlr.runtime.ANTLRStringStream;
 import org.antlr.runtime.CharStream;
 import org.antlr.runtime.CommonTokenStream;
 import org.antlr.runtime.RecognitionException;
 import org.antlr.runtime.tree.CommonTree;
 import org.antlr.runtime.tree.CommonTreeNodeStream;
 
 import javax.script.AbstractScriptEngine;
 import javax.script.Bindings;
 import javax.script.ScriptContext;
 import javax.script.ScriptEngineFactory;
 import java.io.*;
 
 /**
  * @author Pavel A. Yaskevich
  */
 public class GremlinScriptEngine extends AbstractScriptEngine {
 
     public static final String GREMLIN_RC_FILE = ".gremlinrc";
 
     /**
      * This constructor used only by GremlinScriptEngineFactory
      * is you don't want to evaluate .gremlinrc file you can use it too
      */
     public GremlinScriptEngine() {
     }
 
     /**
      * This constructor used to initialize GremlinScriptEngine
      * and to evaluate .gremlinrc file on its' start up
      * @param context GremlinScriptContext
      */
     public GremlinScriptEngine(final GremlinScriptContext context) {
         try {
             this.eval(new FileReader(GREMLIN_RC_FILE), context);
         } catch (FileNotFoundException e) {
             // we do nothing if .gremlinrc is not found.
         } catch (IOException e) {
             throw new RuntimeException(e);
         }
     }
 
     public Bindings createBindings() {
         return new VariableLibrary();
     }
 
     public Object eval(final Reader reader, final ScriptContext context) {
 
         String line;
         String script = "";
         Iterable result = null;
         BufferedReader bReader = new BufferedReader(reader);
         
         try {
             // read whole script before evaluation
             while ((line = bReader.readLine()) != null) {
                 script += line + "\n";
             }
 
             // evaluate script
             result = this.evaluate(script, (GremlinScriptContext) context);
             
             // flushing output streams
             context.getWriter().flush();
         } catch (IOException e) {
             throw new RuntimeException(e.getMessage());
         } catch (RecognitionException e) {
             throw new RuntimeException(e.getMessage());
         }
         
         return result;
     }
 
     public Object eval(final String script, final ScriptContext context) {
         return this.eval(new StringReader(script), context);
     }
 
     public ScriptEngineFactory getFactory() {
         return new GremlinScriptEngineFactory();
     }
 
     private Iterable evaluate(final String code, final GremlinScriptContext context)
             throws RecognitionException
     {
         GremlinEvaluator.EMBEDDED = true;
 
         ANTLRStringStream input = new ANTLRStringStream(code + "\n");
         return evaluate(input, context);
     }
 
     private Iterable evaluate(final CharStream input, final GremlinScriptContext context)
             throws RecognitionException
     {
         final GremlinLexer lexer = new GremlinLexer(input);
         final CommonTokenStream tokens = new CommonTokenStream(lexer);
 
         final GremlinParser parser = new GremlinParser(tokens);
         final GremlinParser.program_return r = parser.program();
 
         final CommonTree t = (CommonTree) r.getTree();
 
         if (t.toStringTree().trim().isEmpty()) {
             return null;
         }
 
         final CommonTreeNodeStream nodes = new CommonTreeNodeStream(t);
         final GremlinEvaluator walker = new GremlinEvaluator(nodes, context);
 
         return walker.program().results;
     }
 }
