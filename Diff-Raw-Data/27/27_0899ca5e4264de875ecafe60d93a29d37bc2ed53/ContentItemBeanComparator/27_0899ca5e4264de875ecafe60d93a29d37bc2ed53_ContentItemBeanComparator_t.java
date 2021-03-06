 /*
 * $Id: ContentItemBeanComparator.java,v 1.2 2005/03/10 18:31:29 eiki Exp $
  * Created on 15.2.2005
  *
  * Copyright (C) 2005 Idega Software hf. All Rights Reserved.
  *
  * This software is the proprietary information of Idega hf.
  * Use is subject to license terms.
  */
 package com.idega.content.bean;
 
 import java.util.Comparator;
import java.util.Date;
 
 
 /**
  * 
 *  Last modified: $Date: 2005/03/10 18:31:29 $ by $Author: eiki $
  * 
  * @author <a href="mailto:gummi@idega.com">Gudmundur Agust Saemundsson</a>
 * @version $Revision: 1.2 $
  */
 public class ContentItemBeanComparator implements Comparator {
 
 	private boolean reverse = false;
 	
 	/**
 	 * 
 	 */
 	public ContentItemBeanComparator() {
 		super();
 	}
 	
 	public void setReverseOrder(boolean value){
 		reverse = value;
 	}
 
 	/* (non-Javadoc)
 	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
 	 */
 	public int compare(Object o1, Object o2) {
 		ContentItemBean item1 = (ContentItemBean)o1;
 		ContentItemBean item2 = (ContentItemBean)o2;
 		int returner = 0;
 		
		Date item1Date = item1.getCreationDate();
		Date item2Date = item2.getCreationDate();
 		
		if(item1Date==null && item2Date==null){
			returner = 0;
		}
		else if(item2Date==null){
			returner = 1;
		}
		else if(item1Date==null){
			returner = -1;
		}
		else{
			returner = item1Date.compareTo(item2Date);
		}
 		return returner*((reverse)?-1:1);
 	}
 }
