 /*
  * Copyright (C) 2003-2008 eXo Platform SAS.
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Affero General Public License
  * as published by the Free Software Foundation; either version 3
  * of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, see<http://www.gnu.org/licenses/>.
  */
 package org.exoplatform.faq.webui;
 
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 
 import javax.mail.internet.AddressException;
 import javax.mail.internet.InternetAddress;
 
 import org.exoplatform.container.PortalContainer;
 import org.exoplatform.faq.service.FAQService;
 import org.exoplatform.faq.service.JcrInputProperty;
 import org.exoplatform.portal.webui.util.SessionProviderFactory;
 import org.exoplatform.portal.webui.util.Util;
 import org.exoplatform.services.jcr.ext.common.SessionProvider;
 import org.exoplatform.services.organization.Group;
 import org.exoplatform.services.organization.MembershipType;
 import org.exoplatform.services.organization.OrganizationService;
 import org.exoplatform.services.organization.User;
 import org.exoplatform.webui.form.UIFormDateTimeInput;
 import org.exoplatform.webui.form.UIFormInputBase;
 import org.exoplatform.webui.form.UIFormMultiValueInputSet;
 import org.exoplatform.webui.form.UIFormUploadInput;
 
 /**
  * Created by The eXo Platform SARL
  * Author : Truong Nguyen
  *					truong.nguyen@exoplatform.com
  * Apr 14, 2008, 2:56:30 PM
  */
 public class FAQUtils {
 	private static String AKONG = "@" ;
   public static String[] specialString = {"!", "#", "$", "%", "^", "&"
                                             , ":", ">", "<", "~", "`", "]", "'", "/"} ;
   
	static public FAQService getFAQService() throws Exception {
     return (FAQService)PortalContainer.getComponent(FAQService.class) ;
   }
 	
 	public static String filterString(String text, boolean isEmail) {
 
     for (String str : specialString) {
       text = text.replaceAll(str, "") ;
     }
     if (!isEmail) text = text.replaceAll(AKONG, "") ;
     int i = 0 ;
     while (i < text.length()) {
       if (text.charAt(i) == '?' || text.charAt(i) == '[' || text.charAt(i) == '(' || text.charAt(i) == '|'
         || text.charAt(i) == ')' || text.charAt(i) == '*' || text.charAt(i) == '\\' || text.charAt(i) == '+'
         || text.charAt(i) == '}' || text.charAt(i) == '{' || text.charAt(i) == '^' || text.charAt(i) == '$'
         || text.charAt(i) == '"'  ) {
         text = text.replace((text.charAt(i)) + "", "") ;
       } else {
         i ++ ;
       }
     }
     return text ;
   }
 	
 	public static User getUserByUserId(String userId) throws Exception {
   	OrganizationService organizationService = (OrganizationService) PortalContainer.getComponent(OrganizationService.class);
   	return organizationService.getUserHandler().findUserByName(userId) ;
   }
 	
 	public static Group getGroup(String group) throws Exception {
   	OrganizationService organizationService = (OrganizationService) PortalContainer.getComponent(OrganizationService.class);
   	return organizationService.getGroupHandler().findGroupById(group) ;
   }
 	
   public static MembershipType getMember(String member) throws Exception {
 		OrganizationService organizationService = (OrganizationService) PortalContainer.getComponent(OrganizationService.class);
 		return organizationService.getMembershipTypeHandler().findMembershipType(member) ;
 	}
 	
 	public static String[] splitForFAQ (String str) throws Exception {
 		if(str != null && str.length() > 0) {
 			if(str.contains(",")) return str.trim().split(",") ;
 			else return str.trim().split(";") ;
 		} else return new String[] {} ;
 	}
 	
 	 public static SessionProvider getSystemProvider() {
 	    return SessionProviderFactory.createSystemProvider();
 	  }
 	
   static public String getCurrentUser() throws Exception {
     return Util.getPortalRequestContext().getRemoteUser();
   }
   
   public static boolean isFieldEmpty(String s) {
     if (s == null || s.length() == 0) return true ;
     return false ;    
   }
   
   public static boolean isValidEmailAddresses(String addressList) throws Exception {
     if (isFieldEmpty(addressList))  return true ;
     boolean isInvalid = true ;
     try {
       InternetAddress[] iAdds = InternetAddress.parse(addressList, true);
       String emailRegex = "[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[_A-Za-z0-9-.]+\\.[A-Za-z]{2,5}" ;
       for (int i = 0 ; i < iAdds.length; i ++) {
         if(!iAdds[i].getAddress().toString().matches(emailRegex)) isInvalid = false;
       }
     } catch(AddressException e) {
       return false ;
     }
     return isInvalid ;
   }
   
   public static String[] getQuestionLanguages() {
     
     return null ;
   }
   
   public static Map prepareMap(List inputs, Map properties) throws Exception {
     Map<String, JcrInputProperty> rawinputs = new HashMap<String, JcrInputProperty>();
     HashMap<String, JcrInputProperty> hasMap = new HashMap<String, JcrInputProperty>() ;
     for (int i = 0; i < inputs.size(); i++) {
       JcrInputProperty property = null;
       if(inputs.get(i) instanceof UIFormMultiValueInputSet) {        
         String inputName = ((UIFormMultiValueInputSet)inputs.get(i)).getName() ;        
         if(!hasMap.containsKey(inputName)) {
           List<String> values = (List<String>) ((UIFormMultiValueInputSet)inputs.get(i)).getValue() ;
           property = (JcrInputProperty) properties.get(inputName);        
           if(property != null){          
             property.setValue(values.toArray(new String[values.size()])) ;
           }
         }
         hasMap.put(inputName, property) ;
       } else {
         UIFormInputBase input = (UIFormInputBase) inputs.get(i);
         property = (JcrInputProperty) properties.get(input.getName());
         if(property != null) {
           if (input instanceof UIFormUploadInput) {
             byte[] content = ((UIFormUploadInput) input).getUploadData() ; 
             property.setValue(content);
           } else if(input instanceof UIFormDateTimeInput) {
             property.setValue(((UIFormDateTimeInput)input).getCalendar()) ;
           } else {
             property.setValue(input.getValue()) ;
           }
         }
       }
     }
     Iterator iter = properties.values().iterator() ;
     JcrInputProperty property ;
     while (iter.hasNext()) {
       property = (JcrInputProperty) iter.next() ;
       rawinputs.put(property.getJcrPath(), property) ;
     }
     return rawinputs;
   }
   
 }
