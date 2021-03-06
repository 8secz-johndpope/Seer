 /*
  * PermissionsEx - Permissions plugin for Bukkit
  * Copyright (C) 2011 t3hk0d3 http://www.tehkode.ru
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License
  * as published by the Free Software Foundation; either version 2
  * of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
  */
 package ru.tehkode.permissions;
 
 import java.util.HashMap;
 import java.util.Map;
 import java.util.regex.Pattern;
 
 /**
  *
  * @author code
  */
 public abstract class PermissionEntity {
 
     protected PermissionManager manager;
     private String name;
     protected boolean virtual = true;
     protected String prefix = "";
     protected String suffix = "";
 
     public PermissionEntity(String name, PermissionManager manager) {
         this.manager = manager;
         this.name = name;
     }
 
     public String getName() {
         return this.name;
     }
 
     public boolean has(String permission, String world) {
         if (permission != null && permission.isEmpty()) { // empty permission for public access :)
             return true;
         }
 
         String expression = getMatchingExpression(permission, world);
         if (expression != null) {
             return this.explainExpression(expression);
         }
 
         return false;
     }
 
     protected String getMatchingExpression(String permission, String world) {
         for (String expression : this.getPermissions(world)) {
             if (isMatches(expression, permission, true)) {
                 return expression;
             }
         }
 
         return null;
     }
 
     public void setPermission(String permission, String value) {
         this.setOption(permission, value, "");
     }
 
     public void removePermission(String permission) {
         this.removePermission(permission, "");
     }
 
     public String getOptionValue(String permission, String world) {
         return this.getOption(permission, world, true);
     }
 
     public String getOptionValue(String permission) {
         return this.getOption(permission, "", true);
     }
 
     protected boolean explainExpression(String expression) {
         return !expression.substring(0, 1).equals("-"); // If expression have - (minus) before then that mean expression are negative
     }
 
     public void setPermissions(String[] permission) {
         this.setPermissions(permission, null);
     }
 
     public String getPrefix() {
         return this.prefix;
     }
 
     public String getSuffix() {
         return this.suffix;
     }
 
     public void setPrefix(String prefix) {
         this.prefix = prefix;
     }
 
     public void setSuffix(String postfix) {
         this.suffix = postfix;
     }
 
     @Override
     public String toString() {
         return this.getName();
     }
 
     public boolean isVirtual() {
         return this.virtual;
     }
 
     public abstract String[] getPermissions(String world);
 
     public abstract Map<String, String[]> getAllPermissions();
 
     public abstract Map<String, Map<String, String>> getAllOptions();
 
     public abstract Map<String, String> getOptions(String world);
 
     public abstract String getOption(String permission, String world, boolean inheritance);
 
     public void setOption(String permission, String value) {
         this.setOption(permission, value, null);
     }
 
     public abstract void setOption(String permission, String value, String world);
 
     public abstract void addPermission(String permission, String world);
 
     public abstract void removePermission(String permission, String world);
 
     public abstract void setPermissions(String[] permissions, String world);
 
     public abstract void save();
 
     public abstract void remove();
 
     @Override
     public boolean equals(Object obj) {
         if (obj == null) {
             return false;
         }
         if (getClass() != obj.getClass()) {
             return false;
         }
         final PermissionEntity other = (PermissionEntity) obj;
         if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
             return false;
         }
         return true;
     }
 
     @Override
     public int hashCode() {
         int hash = 7;
         hash = 89 * hash + (this.name != null ? this.name.hashCode() : 0);
         return hash;
     }
     /**
      * Pattern cache
      */
     protected static HashMap<String, Pattern> patternCache = new HashMap<String, Pattern>();
 
     public static boolean isMatches(String expression, String permission, boolean additionalChecks) {
         if (expression.startsWith("-")) {
             expression = expression.substring(1);
         }
 
         if (!patternCache.containsKey(expression)) {
             patternCache.put(expression, Pattern.compile(prepareRegexp(expression)));
         }
 
         if (patternCache.get(expression).matcher(permission).matches()) {
             return true;
         }
 
         if (additionalChecks && expression.endsWith(".*") && isMatches(expression.substring(0, expression.length() - 2), permission, false)) {
             return true;
         }
/*
         if (additionalChecks && !expression.endsWith(".*") && isMatches(expression + ".*", permission, false)) {
             return true;
         }
*/
         return false;
     }
 
     protected static String prepareRegexp(String expression) {
         return expression.replace(".", "\\.").replace("*", "(.*)");
     }
 }
