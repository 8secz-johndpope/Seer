 package org.jenkinsci.plugins.scriptler.tokenmacro;
 
 import hudson.Extension;
 import hudson.model.TaskListener;
 import hudson.model.AbstractBuild;
 
 import java.io.IOException;
 
 import org.jenkinsci.plugins.scriptler.Messages;
 import org.jenkinsci.plugins.scriptler.config.Script;
 import org.jenkinsci.plugins.scriptler.util.GroovyScript;
 import org.jenkinsci.plugins.scriptler.util.ScriptHelper;
 import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
 import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
 
 /**
  * TokenMacro that allows the execution of a scriptler script an any arbitrary location supporting TokenMacros e.g. <code>${SCRIPTLER, scriptId="superscript.groovy"}</code>
  * 
  * @author Dominik Bartholdi (imod)
  * 
  */
 @Extension
 public class ScriptlerTokenMacro extends DataBoundTokenMacro {
 
     @Parameter
     public String scriptId;
 
     @Override
     public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName) throws MacroEvaluationException, IOException, InterruptedException {
 
         final Script script = ScriptHelper.getScript(scriptId, true);
        if (script.nonAdministerUsing) {
            listener.getLogger().println(Messages.tokenmacro_AdminScriptOnly(scriptId));
             throw new MacroEvaluationException(Messages.tokenmacro_ScriptDoesNotExist(scriptId));
         }
 
         Object output = context.getWorkspace().getChannel().call(new GroovyScript(script.script, null, true, listener));
 
         return output != null ? output.toString() : "";
     }
 
     @Override
     public boolean acceptsMacroName(String macroName) {
         return macroName.equals("SCRIPTLER");
     }
 
 }
