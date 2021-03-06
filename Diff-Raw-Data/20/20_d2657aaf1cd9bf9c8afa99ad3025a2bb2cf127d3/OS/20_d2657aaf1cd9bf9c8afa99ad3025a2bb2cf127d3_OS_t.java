 /*
  * Copyright 1&1 Internet AG, http://www.1and1.org
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU Lesser General Public License as published by
  * the Free Software Foundation; either version 2 of the License,
  * or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  * See the GNU Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package net.sf.beezle.sushi.io;
 
 import net.sf.beezle.sushi.util.Separator;
 
 public enum OS {
     LINUX("Linux", "$", "", ':', "\n",
     		new String[] { "--format", "%a"},
             new String[] { "--format", "%u"},
             new String[] { "--format", "%g"}),
    MAC(new String[] { "Mac" /* official Apple Jdks */, "Darwin" /* OpenJdk 7 for Mac OS BSD Port */ }, "$", "", ':', "\n",
     		new String[] { "-f", "%Op"},
     		new String[] { "-f", "%u"},
     		new String[] { "-f", "%g"}),
     WINDOWS("Windows", "%", "%", ';', "\r\n",
     		new String[] { "/f", "%a" },
     		new String[] { "/f", "%u" },
     		new String[] { "/f", "%g" });
 
     private static OS detect() {
         String name;
 
         name = System.getProperty("os.name");
         for (OS os : values()) {
            for (String substring : os.substrings) {
                if (name.contains(substring)) {
                    return os;
                }
             }
         }
         throw new IllegalArgumentException("unknown os:" + name);
     }
 
     public static final OS CURRENT = detect();
 
    private final String[] substrings;
     private final String variablePrefix;
     private final String variableSuffix;
 
     public final Separator listSeparator;
 
     public final Separator lineSeparator;
 
     public final String[] mode;
     public final String[] uid;
     public final String[] gid;
 
     private OS(String substring, String variablePrefix, String variableSuffix,
             char listSeparatorChar, String lineSeparator, String[] mode, String[] uid, String[] gid) {
        this(new String[] { substring }, variablePrefix, variableSuffix, listSeparatorChar, lineSeparator,  mode, uid, gid);
    }

    private OS(String[] substrings, String variablePrefix, String variableSuffix,
            char listSeparatorChar, String lineSeparator, String[] mode, String[] uid, String[] gid) {
        this.substrings = substrings;
         this.variablePrefix = variablePrefix;
         this.variableSuffix = variableSuffix;
         this.listSeparator = Separator.on(listSeparatorChar);
         this.lineSeparator = Separator.on(lineSeparator);
         this.mode = mode;
         this.uid = uid;
         this.gid = gid;
     }
 
     public String lines(String ... lines) {
     	StringBuilder result;
 
     	result = new StringBuilder();
     	for (String line : lines) {
     		result.append(line);
     		result.append(lineSeparator.getSeparator());
     	}
     	return result.toString();
     }
 
     public String variable(String name) {
         return variablePrefix + name + variableSuffix;
     }
 }
