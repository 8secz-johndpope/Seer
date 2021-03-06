 package com.redhat.victims.cli;
 
 /*
  * #%L
  * This file is part of victims-client.
  * %%
  * Copyright (C) 2013 The Victims Project
  * %%
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  * 
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  * 
  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  * #L%
  */
 import com.redhat.victims.cli.commands.Command;
 import com.redhat.victims.cli.commands.LastUpdateCommand;
 import com.redhat.victims.cli.commands.ConfigureCommand;
 import com.redhat.victims.cli.commands.ScanCommand;
 import com.redhat.victims.cli.commands.SynchronizeCommand;
 import com.redhat.victims.cli.commands.PomScannerCommand;
 import com.redhat.victims.cli.results.CommandResult;
 
 
 /**
  *
  * @author gm
  */
 public class Main {
 
     public static void main(String args[]) {
 
         Repl repl = new Repl();
         repl.register(new ConfigureCommand());
         repl.register(new LastUpdateCommand());
         repl.register(new SynchronizeCommand());
         repl.register(new ScanCommand());
         repl.register(new PomScannerCommand());
 
         if (args.length > 0) {
 
             StringBuilder sb = new StringBuilder();
             for (String arg : args) {
                 sb.append(arg);
                 sb.append(" ");
             }
             Command cmd = repl.eval(sb.toString());
             try {
                 if (cmd != null){
                     CommandResult res = cmd.call();
                     if (res != null) {
                         repl.print(res);
                     }
 
                     repl.shutdown(true);
                 }
 
             } catch (Exception e) {
                 System.out.println(e);
 
             }
 
         } else {
             // Don't launch repl by default (need to set -Dvictims.cli.repl)
            //String setting = System.getProperty(Repl.INTERACTIVE);
            //if (setting != null && setting.equals("true")) {
                 System.exit(repl.loop());
            
//            } else {
//                try {
//                    System.out.println(repl.eval("help").call().toString());
//                } catch (Exception e) {
//                    System.err.println("error: " + e.getMessage());
//                }
//            }
         }
 
     }
 
 }
