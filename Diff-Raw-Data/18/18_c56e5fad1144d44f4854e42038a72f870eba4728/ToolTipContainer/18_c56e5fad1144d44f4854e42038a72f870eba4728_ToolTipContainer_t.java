 /* 
  * This file is part of the Echo Extras Project.
  * Copyright (C) 2005-2009 NextApp, Inc.
  *
  * Version: MPL 1.1/GPL 2.0/LGPL 2.1
  *
  * The contents of this file are subject to the Mozilla Public License Version
  * 1.1 (the "License"); you may not use this file except in compliance with
  * the License. You may obtain a copy of the License at
  * http://www.mozilla.org/MPL/
  *
  * Software distributed under the License is distributed on an "AS IS" basis,
  * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
  * for the specific language governing rights and limitations under the
  * License.
  *
  * Alternatively, the contents of this file may be used under the terms of
  * either the GNU General Public License Version 2 or later (the "GPL"), or
  * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
  * in which case the provisions of the GPL or the LGPL are applicable instead
  * of those above. If you wish to allow use of your version of this file only
  * under the terms of either the GPL or the LGPL, and not to allow others to
  * use your version of this file under the terms of the MPL, indicate your
  * decision by deleting the provisions above and replace them with the notice
  * and other provisions required by the GPL or the LGPL. If you do not delete
  * the provisions above, a recipient may use your version of this file under
  * the terms of any one of the MPL, the GPL or the LGPL.
  */
 
 package nextapp.echo.extras.app;
 
 import nextapp.echo.app.Component;
 
 /**
 * ToolTipContainer component: a container which may contain two children, the first of which is always displayed and the second of
 * which is displayed with the mouse is hovered over the first. May contain zero, one, or two components as children. Many not
 * contain <code>Pane</code> components.
  */
 public class ToolTipContainer extends Component {
     
     /**
      * Default constructor.
      */
     public ToolTipContainer() {
         super();
     }
     
     /**
      * @see nextapp.echo.app.Component#isValidChild(nextapp.echo.app.Component)
      */
     public boolean isValidChild(Component child) {
         return getComponentCount() < 2;
     }
 }
