 /*
  * Copyright (C) 2012 Andreas Halle
  *
  * This file is part of pplex.
  *
  * pplex is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * pplex is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public license
  * along with pplex. If not, see <http://www.gnu.org/licenses/>.
  */
 package controller;
 
 import lightshell.Shell;
 import controller.shellcommands.Conditions;
 import controller.shellcommands.Exit;
 import controller.shellcommands.FormatCmd;
 import controller.shellcommands.Pivot;
 import controller.shellcommands.Read;
 import controller.shellcommands.Show;
 import controller.shellcommands.Warranty;
 
 class Main {
     public static void main(String[] args) {
         if (args.length > 0 && args[0].equals("-nogui")) {
             Shell shell = new Shell();
             shell.setWelcomeMsg(Data.FWELCOME);
            shell.setPrompt("pplex");
             shell.addCommand(new Conditions());
             shell.addCommand(new Exit());
             shell.addCommand(new FormatCmd());
             shell.addCommand(new Pivot());
             shell.addCommand(new Read());
             shell.addCommand(new Show());
             shell.addCommand(new Warranty());
             shell.run();
         } else {
             new GUI();
         }
     }
 }
