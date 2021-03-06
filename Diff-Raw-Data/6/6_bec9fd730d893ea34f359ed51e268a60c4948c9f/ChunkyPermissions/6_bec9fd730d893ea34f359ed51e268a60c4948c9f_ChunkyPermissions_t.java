 package com.dumptruckman.chunky.permission;
 
 import java.util.Arrays;
 import java.util.EnumSet;
 import java.util.HashMap;
 import java.util.Map;
 
 /**
  * @author dumptruckman, SwearWord
  */
 public class ChunkyPermissions {
 
     public enum Flags {
         BUILD('b'), DESTROY('d'), ITEMUSE('i'), SWITCH('s');
 
         private char rep;
 
         private static final Map<Character, Flags> lookup = new HashMap<Character, Flags>();
 
         static {
             for(Flags f : EnumSet.allOf(Flags.class))
                 lookup.put(f.getRep(), f);
         }
 
         Flags(char rep) {
             this.rep = rep;
         }
 
         private char getRep() {
             return rep;
         }
 
         public static Flags get(char c) {
             return lookup.get(c);
         }
     }
 
     protected EnumSet<Flags> flags;
 
     public ChunkyPermissions(Flags... flags) {
        EnumSet flagSet = EnumSet.noneOf(Flags.class);
        for (Flags flag : flags) {
            flagSet.add(flag);
        }
        this.flags = flagSet;
     }
 
     public boolean contains(Flags flag) {
         return flags.contains(flag);
     }
 
     public EnumSet<Flags> getFlags() {
         return flags;
     }
 
     public void setFlag(Flags flag, boolean status) {
         if (flags.contains(flag) && !status) {
             flags.remove(flag);
         } else if (!flags.contains(flag) && status) {
             flags.add(flag);
         }
     }
 }
