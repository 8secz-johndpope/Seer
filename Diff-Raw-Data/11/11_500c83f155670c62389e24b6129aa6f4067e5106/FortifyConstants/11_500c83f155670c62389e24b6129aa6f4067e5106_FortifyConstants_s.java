 /*
  * SonarQube Fortify Plugin
  * Copyright (C) 2012 SonarSource
  * dev@sonar.codehaus.org
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 3 of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
  */
 package org.sonar.plugins.fortify.base;
 
 public final class FortifyConstants {
   public static final String PROPERTY_URL = "sonar.fortify.sscUrl";
   public static final String PROPERTY_LOGIN = "sonar.fortify.sscLogin.secured";
   public static final String PROPERTY_PASSWORD = "sonar.fortify.sscPassword.secured";
   public static final String PROPERTY_ENABLE = "sonar.fortify.enable";
   public static final String PROPERTY_PROJECT_NAME = "sonar.fortify.projectName";
   public static final String PROPERTY_PROJECT_VERSION = "sonar.fortify.projectVersion";
 }
