 /*-- $Id$ -- 
 
  ============================================================================
                    The Apache Software License, Version 1.1
  ============================================================================
  
     Copyright (C) 1999 The Apache Software Foundation. All rights reserved.
  
  Redistribution and use in source and binary forms, with or without modifica-
  tion, are permitted provided that the following conditions are met:
  
  1. Redistributions of  source code must  retain the above copyright  notice,
     this list of conditions and the following disclaimer.
  
  2. Redistributions in binary form must reproduce the above copyright notice,
     this list of conditions and the following disclaimer in the documentation
     and/or other materials provided with the distribution.
  
  3. The end-user documentation included with the redistribution, if any, must
     include  the following  acknowledgment:  "This product includes  software
     developed  by the  Apache Software Foundation  (http://www.apache.org/)."
     Alternately, this  acknowledgment may  appear in the software itself,  if
     and wherever such third-party acknowledgments normally appear.
  
  4. The names "Fop" and  "Apache Software Foundation"  must not be used to
     endorse  or promote  products derived  from this  software without  prior
     written permission. For written permission, please contact
     apache@apache.org.
  
  5. Products  derived from this software may not  be called "Apache", nor may
     "Apache" appear  in their name,  without prior written permission  of the
     Apache Software Foundation.
  
  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
  FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
  APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
  INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
  DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
  OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
  ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
  (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  
  This software  consists of voluntary contributions made  by many individuals
  on  behalf of the Apache Software  Foundation and was  originally created by
  James Tauber <jtauber@jtauber.com>. For more  information on the Apache 
  Software Foundation, please see <http://www.apache.org/>.
  
  */
 package org.apache.fop.fo.pagination;
 
 // FOP
 import org.apache.fop.fo.*;
 import org.apache.fop.fo.properties.*;
 import org.apache.fop.apps.FOPException;				   
 import org.apache.fop.layout.PageMaster;
 
 // Java
 import java.util.*;
 
 public class LayoutMasterSet extends FObj {
 
     public static class Maker extends FObj.Maker {
 	public FObj make(FObj parent, PropertyList propertyList)
 	    throws FOPException {
 	    return new LayoutMasterSet(parent,propertyList);
 	}
     }
 
     public static FObj.Maker maker() {
 	return new LayoutMasterSet.Maker();
     }
 
     private Hashtable simplePageMasters;
     private Hashtable pageSequenceMasters;
     private Hashtable allRegions;
     
     private Root root;
 	
     protected LayoutMasterSet(FObj parent, PropertyList propertyList)
 	throws FOPException {
 	super(parent, propertyList);
 	this.name = "fo:layout-master-set";
 
 	this.simplePageMasters = new Hashtable();
 	this.pageSequenceMasters = new Hashtable();
 	
 	if (parent.getName().equals("fo:root")) {
 	    this.root = (Root)parent;
 	    root.setLayoutMasterSet(this);
 	} else {
 	    throw
 		new FOPException("fo:layout-master-set must be child of fo:root, not "
 				 + parent.getName());
 	}
 	allRegions = new Hashtable();
 	
     }
 
     protected void addSimplePageMaster(SimplePageMaster simplePageMaster)
 		throws FOPException {
 	// check against duplication of master-name
 	if (existsName(simplePageMaster.getMasterName()))
		throw new FOPException( "'master-name' must be unique" +
 			"across page-masters and page-sequence-masters" );
 	this.simplePageMasters.put(simplePageMaster.getMasterName(), simplePageMaster);
     }
 
     protected SimplePageMaster getSimplePageMaster(String masterName) {
 	return (SimplePageMaster)this.simplePageMasters.get(masterName);
     }
 
     protected void addPageSequenceMaster(
 		String masterName, PageSequenceMaster pageSequenceMaster)
 		throws FOPException {
 	// check against duplication of master-name	
 	if (existsName(masterName))
		throw new FOPException( "'master-name' must be unique " +
 			"across page-masters and page-sequence-masters" );
 	this.pageSequenceMasters.put(masterName, pageSequenceMaster);
     }
 
     protected PageSequenceMaster getPageSequenceMaster(String masterName) {
 	return (PageSequenceMaster)this.pageSequenceMasters.get(masterName);
     }
 	
 	private boolean existsName( String masterName )
 	{
 		if (simplePageMasters.containsKey(masterName) ||
 			pageSequenceMasters.containsKey(masterName))
 			return true;
 		else
 			return false;
 	}
 
     protected void resetPageMasters()
     {
         for (Enumeration e = pageSequenceMasters.elements(); e.hasMoreElements(); )
 	    {
 		((PageSequenceMaster)e.nextElement()).reset();
 	    }
 	
     }
 
     protected void checkRegionNames() throws FOPException
     {
 	// Section 7.33.15 check to see that if a region-name is a
 	// duplicate, that it maps to the same region-class.
 	for (Enumeration spm = simplePageMasters.elements(); spm.hasMoreElements(); ) {
 	    SimplePageMaster simplePageMaster = (SimplePageMaster)spm.nextElement();
 	    Hashtable spmRegions = simplePageMaster.getRegions();
 	    for (Enumeration e = spmRegions.elements(); e.hasMoreElements(); ) {
 		Region region = (Region)e.nextElement();
 		if (allRegions.containsKey(region.getRegionName())) {
 		    String localClass = (String)allRegions.get(region.getRegionName());
 		    if (!localClass.equals(region.getRegionClass())) {
			throw new FOPException("Duplicate region-names must map "
					       + "to the same region-class");
 		    }
 		}
 		allRegions.put(region.getRegionName(),region.getRegionClass());
 	    }
 	}
     }
 	
     /**
      * Checks whether or not a region name exists in this master set
      * @returns true when the region name specified has a region in this LayoutMasterSet
      */
     protected boolean regionNameExists(String regionName)
     {
 	boolean result = false;
 	for (Enumeration e = simplePageMasters.elements(); e.hasMoreElements() ;) {
 	    result = ((SimplePageMaster)e.nextElement()).regionNameExists(regionName);
 	    if (result) {
 		return result;
 	    }
 	}
 	return result;
     }
 
 
 }
