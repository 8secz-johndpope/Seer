 /*
  * Copyright (C) 2009 the original author(s).
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.sonatype.gshell.core.execute;
 
 import com.google.inject.Inject;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.slf4j.MDC;
 import org.sonatype.gshell.Shell;
 import org.sonatype.gshell.ShellHolder;
 import org.sonatype.gshell.Variables;
 import org.sonatype.gshell.util.cli.CommandLineProcessor;
 import org.sonatype.gshell.util.cli.OpaqueArguments;
 import org.sonatype.gshell.command.CommandAction;
 import org.sonatype.gshell.command.CommandContext;
 import org.sonatype.gshell.command.CommandDocumenter;
 import org.sonatype.gshell.command.IO;
 import org.sonatype.gshell.core.command.CommandHelpSupport;
 import org.sonatype.gshell.execute.CommandExecutor;
 import org.sonatype.gshell.execute.CommandLineParser;
 import org.sonatype.gshell.execute.CommandLineParser.CommandLine;
 import org.sonatype.gshell.util.i18n.PrefixingMessageSource;
 import org.sonatype.gshell.notification.ErrorNotification;
 import org.sonatype.gshell.notification.ResultNotification;
 import org.sonatype.gshell.util.pref.PreferenceProcessor;
 import org.sonatype.gshell.registry.CommandResolver;
 import org.sonatype.gshell.util.Arguments;
 import org.sonatype.gshell.util.Strings;
 import org.sonatype.iohijack.InputOutputHijacker;
 
 /**
  * The default {@link org.sonatype.gshell.execute.CommandExecutor} component.
  *
  * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
  * @since 2.0
  */
 public class CommandExecutorImpl
     implements CommandExecutor
 {
     private final Logger log = LoggerFactory.getLogger(getClass());
 
     private final CommandResolver resolver;
 
     private final CommandLineParser parser;
 
    private final CommandDocumenter documenter;
 
     @Inject
     public CommandExecutorImpl(final CommandResolver resolver, final CommandLineParser parser, final CommandDocumenter documenter) {
         assert resolver != null;
         this.resolver = resolver;
         assert parser != null;
         this.parser = parser;
         assert documenter != null;
        this.documenter = documenter;
     }
 
     public Object execute(final Shell shell, final String line) throws Exception {
         assert shell != null;
         assert line != null;
 
         if (line.trim().length() == 0) {
             log.trace("Ignoring empty line");
             return null;
         }
 
         final Shell lastShell = ShellHolder.set(shell);
 
         CommandLine cl = parser.parse(line);
 
         try {
             return cl.execute(shell, this);
         }
         catch (ErrorNotification n) {
             // Decode the error notification
             Throwable cause = n.getCause();
 
             if (cause instanceof Exception) {
                 throw (Exception) cause;
             }
             else if (cause instanceof Error) {
                 throw (Error) cause;
             }
             else {
                 throw n;
             }
         }
         finally {
             ShellHolder.set(lastShell);
         }
     }
 
     public Object execute(final Shell shell, final Object... args) throws Exception {
         assert shell != null;
         assert args != null;
 
         return execute(shell, String.valueOf(args[0]), Arguments.shift(args));
     }
 
     public Object execute(final Shell shell, final String name, final Object[] args) throws Exception {
         assert shell != null;
         assert name != null;
         assert args != null;
 
         log.debug("Executing ({}): [{}]", name, Strings.join(args, ", "));
 
         CommandAction command = resolver.resolveCommand(name);
 
         MDC.put(CommandAction.class.getName(), name);
 
         final Shell lastShell = ShellHolder.set(shell);
 
         final IO io = shell.getIo();
 
         // Hijack the system output streams
         if (!InputOutputHijacker.isInstalled()) {
             InputOutputHijacker.install(io.streams);
         }
         else {
             InputOutputHijacker.register(io.streams);
         }
 
         Object result = null;
         try {
             boolean execute = true;
 
             PreferenceProcessor pp = new PreferenceProcessor(command);
             pp.process();
 
             if (!(command instanceof OpaqueArguments)) {
                 CommandLineProcessor clp = new CommandLineProcessor(command);
                 clp.setMessages(new PrefixingMessageSource(command.getMessages(), CommandDocumenter.COMMAND_DOT));
                 CommandHelpSupport help = new CommandHelpSupport();
                 clp.addBean(help);
 
                 // Process the arguments
                 clp.process(Arguments.toStringArray(args));
 
                 // Render command-line usage
                 if (help.displayHelp) {
                     log.trace("Render command-line usage");
                    documenter.renderUsage(command, io);
                     result = CommandAction.Result.SUCCESS;
                     execute = false;
                 }
             }
 
             if (execute) {
                 try {
                     result = command.execute(new CommandContext()
                     {
                         public Shell getShell() {
                             return shell;
                         }
 
                         public Object[] getArguments() {
                             return args;
                         }
 
                         public IO getIo() {
                             return io;
                         }
 
                         public Variables getVariables() {
                             return shell.getVariables();
                         }
                     });
                 }
                 catch (ResultNotification n) {
                     result = n.getResult();
                 }
             }
         }
         finally {
             io.flush();
 
             InputOutputHijacker.deregister();
 
             ShellHolder.set(lastShell);
 
             MDC.remove(CommandAction.class.getName());
         }
 
         log.debug("Result: {}", result);
 
         return result;
     }
 }
