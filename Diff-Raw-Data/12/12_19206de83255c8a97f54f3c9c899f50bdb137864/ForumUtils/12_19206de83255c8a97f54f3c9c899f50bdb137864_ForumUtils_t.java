 /***************************************************************************
  * Copyright (C) 2003-2007 eXo Platform SAS.
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
  ***************************************************************************/
 /**
  * 
  */
 
 package org.exoplatform.forum;
 
 import java.text.Format;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Calendar;
 import java.util.Comparator;
 import java.util.Date;
 import java.util.GregorianCalendar;
 import java.util.List;
 import java.util.ResourceBundle;
 
 import javax.mail.internet.AddressException;
 import javax.mail.internet.InternetAddress;
 import javax.portlet.PortletPreferences;
 
 import org.apache.commons.lang.StringUtils;
import org.bouncycastle.jce.provider.JDKDSASigner.stdDSA;
 import org.exoplatform.forum.service.Post;
 import org.exoplatform.webui.application.WebuiRequestContext;
 import org.exoplatform.webui.application.portlet.PortletRequestContext;
 /**
  * Created by The eXo Platform SARL
  * Author : Vu Duy Tu
  *          tu.duy@exoplatform.com
  * Dec 21, 2007 5:35:54 PM 
  */
 
 public class ForumUtils {
 	public static final String FIELD_EXOFORUM_LABEL = "eXoForum".intern() ;
 	public static final String FIELD_SEARCHFORUM_LABEL = "SearchForum".intern() ;
 	public static String UPLOAD_FILE_SIZE = "uploadFileSizeLimitMB".intern() ;
 	
 	public static final String SEARCHFORM_ID = "SearchForm".intern() ;
 	public static final String GOPAGE_ID_T = "goPageTop".intern() ;
 	public static final String GOPAGE_ID_B = "goPageBottom".intern() ;
 	
 	public static final String CATEGORIES = "Categories".intern() ;
 	public static final String CATEGORY = "Category".intern() ;
 	public static final String FORUM = "Forum".intern() ;
 	public static final String THREAD = "Topic".intern() ;
 	public static final String TAG = "Tag".intern() ;
 	public static final String POST = "Post".intern() ;
 	public static final String POLL = "Poll".intern() ;
 
 	
 	public static final int MAXSIGNATURE = 300;
 	public static final int MAXTITLE = 100;
 	public static final long MAXMESSAGE = 10000;
 	
 	
 	@SuppressWarnings("deprecation")
 	public static String getFormatDate(String format, Date myDate) {
 		/*h,hh,H, m, mm, D, DD, DDD, DDDD, M, MM, MMM, MMMM, yy, yyyy
 		 * */
 		if(myDate == null) return "";
 		String strCase = "" ;
 		int day = myDate.getDay() ;
 		switch (day) {
 		case 0:
 			strCase = "Sunday" ;
 			break;
 		case 1:
 			strCase = "Monday" ;
 			break;
 		case 2:
 			strCase = "Tuesday" ;
 			break;
 		case 3:
 			strCase = "Wednesday" ;
 			break;
 		case 4:
 			strCase = "Thursday" ;
 			break;
 		case 5:
 			strCase = "Friday" ;
 			break;
 		case 6:
 			strCase = "Saturday" ;
 			break;
 		default:
 			break;
 		}
 		String form = "temp" + format ;
 		if(form.indexOf("DDDD") > 0) {
 			Format formatter = new SimpleDateFormat(form.substring(form.indexOf("DDDD") + 5));
 			return strCase + ", "	+ formatter.format(myDate).replaceAll(",", ", ");
 		} else if(form.indexOf("DDD") > 0) {
 			Format formatter = new SimpleDateFormat(form.substring(form.indexOf("DDD") + 4));
 			return strCase.replaceFirst("day", "") + ", " + formatter.format(myDate).replaceAll(",", ", ");
 		} else {
 			Format formatter = new SimpleDateFormat(format);
 			return formatter.format(myDate);
 		}
 	}
 	
 	public static Calendar getInstanceTempCalendar() { 
 		Calendar	calendar = GregorianCalendar.getInstance() ;
 		calendar.setLenient(false) ;
 		int gmtoffset = calendar.get(Calendar.DST_OFFSET) + calendar.get(Calendar.ZONE_OFFSET);
 		calendar.setTimeInMillis(System.currentTimeMillis() - gmtoffset) ; 
 		return	calendar;
 	}
 	
 	public static boolean isValidEmailAddresses(String addressList) throws Exception {
 		if (isEmpty(addressList))	return true ;
 		addressList = StringUtils.remove(addressList, " ");
 		addressList = StringUtils.replace(addressList, ";", ",");
 		try {
 			InternetAddress[] iAdds = InternetAddress.parse(addressList, true);
 			String emailRegex = "[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[_A-Za-z0-9-.]+\\.[A-Za-z]{2,5}" ;
 			for (int i = 0 ; i < iAdds.length; i ++) {
 				if(!iAdds[i].getAddress().toString().matches(emailRegex)) return false;
 			}
 		} catch(AddressException e) {
 			return false ;
 		}
 		return true ;
 	}
 	
 	public static String getSizeFile(double size) {
 		String unit = " Byte" ;
 		if(size >= 1000) {
 			size = size/1024 ;
 			unit = " Kb" ;
 		}
 		if(size >= 1000) {
 			size = size/1024 ;
 			unit = " Mb" ;
 		}
 		String str = String.valueOf(size) ;
 		int t = str.indexOf("."); 
 		if(t > 0) {
 			if(str.length() > (t+4))
 				str = str.substring(0,(t+4)) ;
 		}
 		return (str + unit);
 	}
 	
 	public static String getTimeZoneNumberInString(String string) {
 		if(!isEmpty(string)) {
 			StringBuffer stringBuffer = new StringBuffer();
 			for(int i = 0; i <	string.length(); ++i) {
 				char c = string.charAt(i) ; 
 				if(c == ')') break ;
 				if (Character.isDigit(c) || c == '-' || c == '+' || c == ':'){
 					if(c == ':') c = '.';
 					if(c == '3' && string.charAt(i-1) == ':') c = '5';
 					stringBuffer.append(c);
 				}
 			}
 			return stringBuffer.toString() ;
 		}
 		return null ;
 	}
 	
 	public static String[] getStarNumber(double voteRating) throws Exception {
 		int star = (int)voteRating ;
 		String[] className = new String[6] ;
 		float k = 0;
 		for (int i = 0; i < 5; i++) {
 			if(i < star) className[i] = "star" ;
 			else if(i == star) {
 				k = (float) (voteRating - i) ; 
 				if(k < 0.25) className[i] = "notStar" ;
 				if(k >= 0.25 && k < 0.75) className[i] = "halfStar" ;
 				if(k >= 0.75) className[i] = "star" ;
 			} else {
 				className[i] = "notStar" ;
 			}
 			className[5] = ("" + voteRating) ;
 			if(className[5].length() >= 3) className[5] = className[5].substring(0, 3) ;
 			if(k == 0) className[5] = "" + star ; 
 		}
 		return className ;
 	}
 	
 	public static String[] splitForForum (String str) throws Exception {
 		if(!isEmpty(str)) {
 			str = StringUtils.remove(str, " ");
 			if(str.contains(",")){ 
 				str.replaceAll(";", ",") ;
 				return str.trim().split(",") ;
 			} else { 
 				str.replaceAll(",", ";") ;
 				return str.trim().split(";") ;
 			}
 		} else return new String[] {} ;
 	}
 	
 	public static String unSplitForForum (String[] str) throws Exception {
 		StringBuilder rtn = new StringBuilder();
 		int t = str.length, i=0 ;
 		if(t > 0 && !str[0].equals(" ")) {
 			for (String temp : str) {
 				if(i == (t-1))rtn.append(temp) ;
 				else rtn.append(temp).append(",") ;
 				++i;
 			}
 		}
 		return rtn.toString() ;
 	}
 	
 	public static String removeSpaceInString(String str) throws Exception {
 		if(!isEmpty(str)) {
 			String strs[] = new String[]{";", ", ", " ,", ",,"};
 			for (int i = 0; i < strs.length; i++) {
 				while (str.indexOf(strs[i]) >= 0) {
 	        str = str.replaceAll(strs[i], ",");
         }
       }
 			if(str.lastIndexOf(",") == str.length() - 1) {
 				str = str.substring(0, str.length() - 1) ;
 			}
 			if(str.indexOf(",") == 0) {
 				str = str.substring(1, str.length()) ;
 			}
 			return str;
 		} else return "";
 	}
 	
 	public static String removeZeroFirstNumber(String str) {
 		if(!isEmpty(str)){
 			while(true) {
 				if(str.length() > 1) {
 					if(str.charAt(0)=='0'){
 						str = str.replaceFirst("0", "") ;
 					}	else break;
 				}	else break;
 			}
 		}
 		return str;
 	}
 	
 	public static String removeStringResemble(String s) throws Exception {
 		List<String> list = new ArrayList<String>();
 		if(!isEmpty(s)) {
 			String temp[] = splitForForum(s) ;
 			s = ""; int l = temp.length;
 			for (int i = 0; i < l; ++i) {
 				if(list.contains(temp[i]) || temp[i].trim().length() == 0) continue ;
 				list.add(temp[i]) ;
 				if(i == (l-1))s += temp[i];
 				else s += temp[i] + ",";
 			}
 			return s;
 		} else return "";
 	}
 	
 	public static boolean isEmpty(String str) {
 		if(str == null || str.trim().length() == 0) return true ;
 		else return false;
 	}
 
 	public static String[] addStringToString(String input, String output) throws Exception {
 		List<String> list = new ArrayList<String>();
 		if(!isEmpty(output)) {
 			if(!isEmpty(input)) {
 				if(input.lastIndexOf(",") != (input.length() - 1)) input = input + ",";
 				output = input + output ;
 				String temp[] = splitForForum(output) ;
 				for (String string : temp) {
 					if(list.contains(string) || string.length() == 0) continue ;
 					list.add(string) ;
 				}
 			}
 		}
 		if(list.size() == 0) list.add(" ");
 		return list.toArray(new String[] {}) ;
 	}
 	
 	public static boolean isStringInStrings(String []strings, String string) {
 		string = string.trim();
 		for (String string1 : strings) {
 			string1 = string1.trim();
 			if(string.equals(string1)) return true ;
 		}
 		return false;
 	}
 
 	public static boolean isStringInList(List<String>list, String string) {
 		if(list.contains(string)) return true;
 		return false;
 	}
 
 	public static String getSubString(String str, int max) {
 		if(!isEmpty(str)) {
 			int l = str.length() ;
 			if(l > max) {
 				str = str.substring(0, max) ;
 				int space = str.lastIndexOf(" ");
 				if(space > (max-6))
 					str = str.substring(0, space) + "...";
 				else str = str + "..." ;
 			}
 		}
 		return str ;
 	}
 	
 	public static String getLabel(String label, String key) {
 		if (isEmpty(key)) key = " ";
 		try {
 			return label.replaceFirst("<keyWord>", key) ;
     } catch (Exception e) {
     	String s = label.substring(0, label.indexOf("<keyWord>") - 1);
 	    return s + "'" + key + "'" + label.substring(label.indexOf("<keyWord>"));
     }
 	}
 	
 	public static String[] getColor() {
 		return new String[] {"blue", "DarkGoldenRod", "green", "yellow", "BlueViolet", "orange",
 				"darkBlue", "IndianRed","DarkCyan" ,"lawnGreen"} ; 
 	}
 	
 	public static String getDefaultMail(){
 		WebuiRequestContext context = WebuiRequestContext.getCurrentInstance() ;
 		ResourceBundle res = context.getApplicationResourceBundle() ;
 		return res.getString("UIForumAdministrationForm.label.notifyEmailContentDefault");
 	}
 	
 	public static boolean enableIPLogging(){
 		PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance() ;
 		PortletPreferences portletPref = pcontext.getRequest().getPreferences() ;
 		return Boolean.parseBoolean(portletPref.getValue("enableIPFiltering", ""));
 	}
 	
 	public static void savePortletPreference(String listCategoryId, String listForumId) throws Exception {
 		PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance() ;
 		PortletPreferences portletPref = pcontext.getRequest().getPreferences() ;
 		portletPref.setValue("invisibleCategories", listCategoryId);
 		portletPref.setValue("invisibleForums", listForumId);
 		portletPref.store();
   }
 	
 	public static SettingPortletPreference getPorletPreference() throws Exception {
 		SettingPortletPreference preference = new SettingPortletPreference();
 		PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance() ;
 		PortletPreferences portletPref = pcontext.getRequest().getPreferences() ;
 		preference.setShowForumActionBar(Boolean.parseBoolean(portletPref.getValue("showForumActionBar", "")));
 		preference.setForumNewPost(Integer.parseInt(portletPref.getValue("forumNewPost", "")));
 		preference.setUseAjax(Boolean.parseBoolean(portletPref.getValue("useAjax", "")));
 		preference.setEnableIPLogging(Boolean.parseBoolean(portletPref.getValue("enableIPLogging", "")));
 		preference.setEnableIPFiltering(Boolean.parseBoolean(portletPref.getValue("enableIPFiltering", "")));
 		preference.setInvisibleCategories(getListInValus(portletPref.getValue("invisibleCategories", ""))) ;
 		preference.setInvisibleForums((getListInValus(portletPref.getValue("invisibleForums", "")))) ;
 		// Show porlet
 		preference.setShowForumJump(Boolean.parseBoolean(portletPref.getValue("isShowForumJump", "")));
 		preference.setShowIconsLegend(Boolean.parseBoolean(portletPref.getValue("isShowIconsLegend", "")));
 		preference.setShowModerators(Boolean.parseBoolean(portletPref.getValue("isShowModerators", "")));
 		preference.setShowPoll(Boolean.parseBoolean(portletPref.getValue("isShowPoll", "")));
 		preference.setShowQuickReply(Boolean.parseBoolean(portletPref.getValue("isShowQuickReply", "")));
 		preference.setShowRules(Boolean.parseBoolean(portletPref.getValue("isShowRules", "")));
 		preference.setShowStatistics(Boolean.parseBoolean(portletPref.getValue("isShowStatistics", "")));
 	  return preference;
   }
 	
 	public static void savePortletPreference(SettingPortletPreference sPreference) throws Exception {
 		PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance() ;
 		PortletPreferences portletPref = pcontext.getRequest().getPreferences() ;
 		String listForumId = "", listCategoryId = "";
 		List<String>invisibleForums = sPreference.getInvisibleForums();
 		List<String>invisibleCategories = sPreference.getInvisibleCategories();
 		
 		if(!invisibleCategories.isEmpty()){
 			listForumId = invisibleForums.toString().replace('['+"", "").replace(']'+"", "").replaceAll(" ", "");
 			listCategoryId = invisibleCategories.toString().replace('['+"", "").replace(']'+"", "").replaceAll(" ", "");
 		}
 		
 		portletPref.setValue("isShowForumJump", sPreference.isShowForumJump()+ "");
 		portletPref.setValue("isShowIconsLegend",sPreference.isShowIconsLegend()+ "");
 		portletPref.setValue("isShowModerators", sPreference.isShowModerators() + "");
 		portletPref.setValue("isShowPoll", sPreference.isShowPoll() + "");
 		portletPref.setValue("isShowQuickReply", sPreference.isShowQuickReply() + "");
 		portletPref.setValue("isShowRules", sPreference.isShowRules() + "");
 		portletPref.setValue("isShowStatistics", sPreference.isShowStatistics() + "");
 		portletPref.setValue("invisibleCategories", listCategoryId);
 		portletPref.setValue("invisibleForums", listForumId);
 		portletPref.store();
   }
 
 	public static List<String> getListInValus(String value) throws Exception {
 		List<String>list = new ArrayList<String>();
 		if(!ForumUtils.isEmpty(value)) {
 			list.addAll(Arrays.asList(ForumUtils.addStringToString(value, value)));
 		}
 		return list;
 	}
 	
 	public static int getLimitUploadSize(){		
 		PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
 		PortletPreferences portletPref = pcontext.getRequest().getPreferences();
 		int limitMB ;
 		try {
 			limitMB = Integer.parseInt(portletPref.getValue(UPLOAD_FILE_SIZE, "").trim()) ;
 		}catch (Exception e) {
 			limitMB = -1 ;
 		}
 		return limitMB ;
 	}
 	
 	static public class DatetimeComparatorDESC implements Comparator<Object> {
     public int compare(Object o1, Object o2) throws ClassCastException {
     	Date date1 = ((Post) o1).getCreatedDate() ;
       Date date2  = ((Post) o2).getCreatedDate() ;
       return date1.compareTo(date2) ;
     }
   }
 	
 	static public String getActionViewInfoUser(String link, String conponentId, String actionRepl, String actionWith) {
 		if(isEmpty(link)) return "";
 		link = StringUtils.replace(link, conponentId, "UIForumPortlet");
 		link = StringUtils.replace(link, actionRepl, actionWith);
 		if(link.indexOf("javascript") < 0) {
 			link = "javascript:ajaxGet('" + link +"&ajaxRequest=true')";
 		}
 		return link;
 	}
	
	static public String getCalculateListEmail(String s) throws Exception{
		String []strs = splitForForum(s);
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < strs.length; i++) {
	    builder.append("<span title='").append(strs[i]).append("'>").append(getSubString(strs[i], 15)).append("</span>");
	    if(i > 0) builder.append(",<br/>");
    }
		return builder.toString();
	}
 }
