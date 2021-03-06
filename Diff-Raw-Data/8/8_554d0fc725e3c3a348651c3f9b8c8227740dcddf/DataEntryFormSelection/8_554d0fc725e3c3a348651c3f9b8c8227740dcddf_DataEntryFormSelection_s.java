 /*******************************************************************************
  * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
  * 
  * This program and the accompanying materials
  * are made available under the terms of the GNU Public License v3.0.
  * 
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  ******************************************************************************/
 package org.obiba.opal.sesame.report.impl;
 
 import java.util.Collections;
 import java.util.Set;
 
 import javax.xml.namespace.QName;
 
 import org.obiba.opal.elmo.owl.concepts.DataEntryFormClass;
 import org.obiba.opal.elmo.owl.concepts.DataItemClass;
 import org.obiba.opal.sesame.report.IDataItemSelection;
 import org.obiba.opal.sesame.report.ReportQueryBuilder;
 import org.openrdf.elmo.sesame.SesameManager;
 import org.openrdf.model.URI;
 
 /**
  * 
  */
 public class DataEntryFormSelection implements IDataItemSelection {
 
   private String name;
 
   private QName qname;
 
   public Set<DataItemClass> getSelection(SesameManager manager) {
     DataEntryFormClass def = manager.find(DataEntryFormClass.class, getQName());
     return Collections.unmodifiableSet(def.getDataVariables());
   }
 
   private QName getQName() {
     if(qname == null) {
       qname = QName.valueOf(name);
     }
     return qname;
   }
 
   public void contribute(ReportQueryBuilder builder, SesameManager manager) {
     URI defUri = manager.getConnection().getValueFactory().createURI(getQName().getNamespaceURI(), getQName().getLocalPart());
    builder.joinVariablePredicateValue("opal:withinDataEntryForm", defUri);
   }
 }
