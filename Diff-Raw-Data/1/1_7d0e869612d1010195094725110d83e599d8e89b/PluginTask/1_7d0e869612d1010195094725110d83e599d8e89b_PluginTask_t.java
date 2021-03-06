 package org.smoothbuild.task;
 
 import java.lang.reflect.InvocationTargetException;
 import java.util.Map;
 
 import org.smoothbuild.fs.base.exc.FileSystemException;
 import org.smoothbuild.function.base.Name;
 import org.smoothbuild.function.base.Signature;
 import org.smoothbuild.function.base.Type;
 import org.smoothbuild.function.plugin.PluginInvoker;
 import org.smoothbuild.plugin.Sandbox;
 import org.smoothbuild.task.err.FileSystemError;
 import org.smoothbuild.task.err.NullResultError;
 import org.smoothbuild.task.err.ReflexiveInternalError;
 import org.smoothbuild.task.err.UnexpectedError;
 
 import com.google.common.collect.ImmutableMap;
 import com.google.common.collect.ImmutableMap.Builder;
 
 public class PluginTask extends AbstractTask {
   private final Signature signature;
   private final PluginInvoker pluginInvoker;
 
   public PluginTask(Signature signature, PluginInvoker pluginInvoker, Map<String, Task> dependencies) {
     super(dependencies);
     this.signature = signature;
     this.pluginInvoker = pluginInvoker;
   }
 
   @Override
   public void calculateResult(Sandbox sandbox) {
     try {
       Object result = pluginInvoker.invoke(sandbox, calculateArguments(dependencies()));
       if (result == null && !isNullResultAllowed()) {
         sandbox.report(new NullResultError(functionName()));
       } else {
         setResult(result);
       }
     } catch (IllegalAccessException e) {
       sandbox.report(new ReflexiveInternalError(functionName(), e));
     } catch (InvocationTargetException e) {
       Throwable cause = e.getCause();
       if (cause instanceof FileSystemException) {
         sandbox.report(new FileSystemError(functionName(), cause));
       } else {
         sandbox.report(new UnexpectedError(functionName(), cause));
       }
     }
   }
 
   private Name functionName() {
     return signature.name();
   }
 
   private boolean isNullResultAllowed() {
     return signature.type() == Type.VOID;
   }
 
   private static ImmutableMap<String, Object> calculateArguments(
       ImmutableMap<String, Task> dependencies) {
     Builder<String, Object> builder = ImmutableMap.builder();
     for (Map.Entry<String, Task> entry : dependencies.entrySet()) {
       builder.put(entry.getKey(), entry.getValue().result());
     }
     return builder.build();
   }
 }
