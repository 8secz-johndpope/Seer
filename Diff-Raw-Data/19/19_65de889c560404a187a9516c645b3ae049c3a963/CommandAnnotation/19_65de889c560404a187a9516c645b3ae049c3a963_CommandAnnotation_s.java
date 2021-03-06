 package org.p000ison.dev.commandapi;
 
 /**
  *
  */
 public @interface CommandAnnotation {
 
     String name();
 
     String usage();
 
     String[] identifiers();
 
     String[] aliases() default {};
 
     String[] subCommands() default {};
 
     String[] permissions() default {};
 
     int minArguments();
 
     int maxArguments();
 
     String[] arguments() default {};
 
     boolean subCommand() default false;
 }
