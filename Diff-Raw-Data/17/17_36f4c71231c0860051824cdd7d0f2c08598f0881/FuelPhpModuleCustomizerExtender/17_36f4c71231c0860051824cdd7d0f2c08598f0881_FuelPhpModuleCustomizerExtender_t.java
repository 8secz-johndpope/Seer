 package org.netbeans.modules.php.fuel;
 
 import java.util.EnumSet;
 import javax.swing.JComponent;
 import javax.swing.event.ChangeListener;
 import org.netbeans.modules.php.api.phpmodule.PhpModule;
 import org.netbeans.modules.php.api.phpmodule.PhpModule.Change;
 import org.netbeans.modules.php.api.phpmodule.PhpModuleProperties;
 import org.netbeans.modules.php.fuel.preferences.FuelPhpPreferences;
 import org.netbeans.modules.php.fuel.ui.FuelPhpCustomizerPanel;
 import org.netbeans.modules.php.project.PhpProject;
 import org.netbeans.modules.php.project.ProjectPropertiesSupport;
 import org.netbeans.modules.php.project.ui.customizer.PhpProjectProperties;
import org.netbeans.modules.php.project.ui.customizer.PhpProjectProperties.RunAsType;
 import org.netbeans.modules.php.project.ui.customizer.PhpProjectProperties.UploadFiles;
 import org.netbeans.modules.php.project.util.PhpProjectUtils;
 import org.netbeans.modules.php.spi.phpmodule.PhpModuleCustomizerExtender;
 import org.openide.util.HelpCtx;
 import org.openide.util.Lookup;
 import org.openide.util.NbBundle;
 
 /*
  * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  *
  * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
  *
  * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
  * Other names may be trademarks of their respective owners.
  *
  * The contents of this file are subject to the terms of either the GNU
  * General Public License Version 2 only ("GPL") or the Common
  * Development and Distribution License("CDDL") (collectively, the
  * "License"). You may not use this file except in compliance with the
  * License. You can obtain a copy of the License at
  * http://www.netbeans.org/cddl-gplv2.html
  * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
  * specific language governing permissions and limitations under the
  * License.  When distributing the software, include this License Header
  * Notice in each file and include the License file at
  * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
  * particular file as subject to the "Classpath" exception as provided
  * by Oracle in the GPL Version 2 section of the License file that
  * accompanied this code. If applicable, add the following below the
  * License Header, with the fields enclosed by brackets [] replaced by
  * your own identifying information:
  * "Portions Copyrighted [year] [name of copyright owner]"
  *
  * If you wish your version of this file to be governed by only the CDDL
  * or only the GPL Version 2, indicate your decision by adding
  * "[Contributor] elects to include this software in this distribution
  * under the [CDDL or GPL Version 2] license." If you do not indicate a
  * single choice of license, a recipient has the option to distribute
  * your version of this file under either the CDDL, the GPL Version 2 or
  * to extend the choice of license to its licensees as provided above.
  * However, if you add GPL Version 2 code and therefore, elected the GPL
  * Version 2 license, then the option applies only if the new code is
  * made subject to such option by the copyright holder.
  *
  * Contributor(s):
  *
  * Portions Copyrighted 2012 Sun Microsystems, Inc.
  */
 /**
  *
  * @author junichi11
  */
 public class FuelPhpModuleCustomizerExtender extends PhpModuleCustomizerExtender {
 
     private FuelPhpCustomizerPanel panel;
     private PhpModule phpModule;
     private String fuelName;
     private boolean useTestCaseMethod;
     private boolean ignoreMVCNode;
 
     public FuelPhpModuleCustomizerExtender(PhpModule phpModule) {
         this.phpModule = phpModule;
         fuelName = FuelPhpPreferences.getFuelName(phpModule);
         useTestCaseMethod = FuelPhpPreferences.useTestCaseMethod(phpModule);
         ignoreMVCNode = FuelPhpPreferences.ignoreMVCNode(phpModule);
     }
 
     @Override
     public String getDisplayName() {
         return NbBundle.getMessage(FuelPhpModuleCustomizerExtender.class, "LBL_FuelPHP");
     }
 
     @Override
     public void addChangeListener(ChangeListener listener) {
     }
 
     @Override
     public void removeChangeListener(ChangeListener listener) {
     }
 
     @Override
     public JComponent getComponent() {
         return getPanel();
     }
 
     @Override
     public HelpCtx getHelp() {
         return null;
     }
 
     @Override
     public boolean isValid() {
         if (isUplaodFilesOnSave() && ignoreMVCNode == true) {
             ignoreMVCNode = false;
             FuelPhpPreferences.setIgnoreMVCNode(phpModule, ignoreMVCNode);
         }
         return true;
     }
 
     @Override
     public String getErrorMessage() {
         return null;
     }
 
     @Override
     public EnumSet<Change> save(PhpModule phpModule) {
         EnumSet<Change> enumSet = null;
         boolean tempUseTestCaseMethod = panel.useTestCaseMethod();
         if (useTestCaseMethod != tempUseTestCaseMethod) {
             FuelPhpPreferences.setUseTestCaseMethod(phpModule, tempUseTestCaseMethod);
         }
 
         boolean tmpIgnoreMVCNode = panel.ignoreMVCNode();
         if (isUplaodFilesOnSave()) {
             tmpIgnoreMVCNode = false;
         }
         if (ignoreMVCNode != tmpIgnoreMVCNode) {
             FuelPhpPreferences.setIgnoreMVCNode(phpModule, tmpIgnoreMVCNode);
             enumSet = EnumSet.of(Change.IGNORED_FILES_CHANGE);
         }
         String newFuelName = panel.getFuelNameTextField().getText();
         if (!newFuelName.equals("") && !newFuelName.equals(fuelName)) {
             FuelPhpPreferences.setFuelName(phpModule, newFuelName);
             return EnumSet.of(Change.FRAMEWORK_CHANGE);
         }
 
         return enumSet;
     }
 
     private FuelPhpCustomizerPanel getPanel() {
         if (panel == null) {
             panel = new FuelPhpCustomizerPanel();
             panel.setFuelNameTextField(fuelName);
             panel.setUseTestCaseMethod(useTestCaseMethod);
             panel.setIgnoreMVCNode(ignoreMVCNode);
         }
         return panel;
     }
 
     private boolean isUplaodFilesOnSave() {
         PhpProject phpProject = PhpProjectUtils.getPhpProject(phpModule.getProjectDirectory());
        RunAsType runAs = ProjectPropertiesSupport.getRunAs(phpProject);
        if (runAs == RunAsType.REMOTE) {
            UploadFiles remoteUpload = ProjectPropertiesSupport.getRemoteUpload(phpProject);
            if (remoteUpload != null && remoteUpload == UploadFiles.ON_SAVE) {
                return true;
            }
         }
         return false;
     }
 }
