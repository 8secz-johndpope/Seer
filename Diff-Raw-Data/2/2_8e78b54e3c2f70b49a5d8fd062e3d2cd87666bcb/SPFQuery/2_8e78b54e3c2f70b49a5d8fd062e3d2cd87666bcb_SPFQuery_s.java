 /***********************************************************************
  * Copyright (c) 2006 The Apache Software Foundation.                  *
  * All rights reserved.                                                *
  * ------------------------------------------------------------------- *
  * Licensed under the Apache License, Version 2.0 (the "License"); you *
  * may not use this file except in compliance with the License. You    *
  * may obtain a copy of the License at:                                *
  *                                                                     *
  *     http://www.apache.org/licenses/LICENSE-2.0                      *
  *                                                                     *
  * Unless required by applicable law or agreed to in writing, software *
  * distributed under the License is distributed on an "AS IS" BASIS,   *
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
  * implied.  See the License for the specific language governing       *
  * permissions and limitations under the License.                      *
  ***********************************************************************/
 
 package org.apache.james.jspf;
 
 import org.apache.log4j.ConsoleAppender;
 import org.apache.log4j.Level;
 import org.apache.log4j.Logger;
 import org.apache.log4j.SimpleLayout;
 
 /**
  * This class is used for commandline usage of JSPF
  * 
  * @author Norman Maurer <nm@byteaction.de>
  * @author Stefano Bagnara <apache@bago.org>
  */
 public class SPFQuery {
 
     private static Logger logger = Logger.getRootLogger();
 
     /**
      * @param args
      *            The commandline arguments to parse
      */
     public static void main(String[] args) {
 
         String ip = null;
         String sender = null;
         String helo = null;
 
         SimpleLayout layout = new SimpleLayout();
         ConsoleAppender consoleAppender = new ConsoleAppender(layout);
         logger.addAppender(consoleAppender);
 
         logger.setLevel(Level.ERROR);
 
         // Parse the command line arguments
         if (args.length < 3 || args.length > 4) {
             usage();
         } else {
             for (int i = 0; i < args.length; i++) {
                 String[] arguments = args[i].split("=");
 
                 if (arguments[0].equals("-ip")) {
                     ip = arguments[1];
                 } else if (arguments[0].equals("-sender")) {
                     sender = arguments[1];
                 } else if (arguments[0].equals("-helo")) {
                     helo = arguments[1];
                 } else if (arguments[0].equals("-debug")) {
                     logger.setLevel(Level.DEBUG);
                 } else if (arguments[0].equals("-verbose")) {
                     logger.setLevel(Level.TRACE);
                 } else {
                     usage();
                 }
 
             }
 
             // check if all needed values was set
             if (ip != null && sender != null && helo != null) {
                 SPF spf = new SPF();
                 SPFResult result = spf.checkSPF(ip, sender, helo);
                 System.out.println(result.getResult());
                 System.out.println(result.getHeader());
                 System.exit(0);
             } else {
                 usage();
             }
         }
     }
 
     /**
      * Print out the usage
      */
     private static void usage() {
         System.out
                .println("Usage: SPFQuery -ip=192.168.100.1 -sender=postmaster@foo.bar -helo=foo.bar [-debug] [-verbose]");
         System.exit(0);
     }
 
 }
