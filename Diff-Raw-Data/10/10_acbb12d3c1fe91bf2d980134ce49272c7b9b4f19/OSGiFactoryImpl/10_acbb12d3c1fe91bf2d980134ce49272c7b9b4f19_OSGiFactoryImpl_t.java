 /*
  * 
  * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  * 
  * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.
  * 
  * The contents of this file are subject to the terms of either the GNU
  * General Public License Version 2 only ("GPL") or the Common Development
  * and Distribution License("CDDL") (collectively, the "License").  You
  * may not use this file except in compliance with the License. You can obtain
  * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
  * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
  * language governing permissions and limitations under the License.
  * 
  * When distributing the software, include this License Header Notice in each
  * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
  * Sun designates this particular file as subject to the "Classpath" exception
  * as provided by Sun in the GPL Version 2 section of the License file that
  * accompanied this code.  If applicable, add the following below the License
  * Header, with the fields enclosed by brackets [] replaced by your own
  * identifying information: "Portions Copyrighted [year]
  * [name of copyright owner]"
  * 
  * Contributor(s):
  * 
  * If you wish your version of this file to be governed by only the CDDL or
  * only the GPL Version 2, indicate your decision by adding "[Contributor]
  * elects to include this software in this distribution under the [CDDL or GPL
  * Version 2] license."  If you don't indicate a single choice of license, a
  * recipient has the option to distribute your version of this file under
  * either the CDDL, the GPL Version 2 or to extend the choice of license to
  * its licensees as provided above.  However, if you add GPL Version 2 code
  * and therefore, elected the GPL Version 2 license, then the option applies
  * only if the new code is made subject to such option by the copyright
  * holder.
  */
 
 
 package org.jvnet.hk2.osgiadapter;
 
 import static org.jvnet.hk2.osgiadapter.Logger.logger;
 import com.sun.enterprise.module.common_impl.AbstractFactory;
 import com.sun.enterprise.module.common_impl.ModuleId;
 import com.sun.enterprise.module.ModuleDefinition;
 import org.osgi.framework.BundleContext;
 
 import java.util.logging.Level;
 
 /**
  * @author Sanjeeb.Sahoo@Sun.COM
  */
 public class OSGiFactoryImpl extends AbstractFactory {
 
     private BundleContext ctx;
 
    public static synchronized void initialize(BundleContext ctx) {
         if (Instance != null) {
             // TODO : this is somehow invoked twice during gf startup, we need to investigate.
             logger.logp(Level.FINE, "OSGiFactoryImpl", "initialize",
                     "Singleton already initialized as {0}", getInstance());
         }
         Instance = new OSGiFactoryImpl(ctx);
     }
 
     private OSGiFactoryImpl(BundleContext ctx) {
         this.ctx = ctx;
     }
 
     public OSGiModulesRegistryImpl createModulesRegistry() {
         return new OSGiModulesRegistryImpl(ctx);
     }
 
     public ModuleId createModuleId(String name, String version)
     {
         return new OSGiModuleId(name, version);
     }
 
     public ModuleId createModuleId(ModuleDefinition md)
     {
         return new OSGiModuleId(md);
     }
 }
