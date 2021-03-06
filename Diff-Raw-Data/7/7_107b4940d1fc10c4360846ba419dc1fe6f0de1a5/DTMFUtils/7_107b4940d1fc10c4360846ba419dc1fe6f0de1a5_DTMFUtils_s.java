 /*
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
 package org.jboss.mobicents.seam.util;
 
 import java.io.File;
 
 import javax.naming.InitialContext;
 import javax.naming.NamingException;
 import javax.servlet.sip.SipSession;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.jboss.mobicents.seam.actions.OrderManager;
 import org.jboss.mobicents.seam.listeners.DTMFListener;
 import org.jboss.mobicents.seam.listeners.MediaResourceListener;
 import org.mobicents.mscontrol.MsConnection;
 import org.mobicents.mscontrol.MsSignalGenerator;
 import org.mobicents.mscontrol.signal.Announcement;
 import org.mobicents.mscontrol.signal.Basic;
 
 /**
  * @author <A HREF="mailto:jean.deruelle@gmail.com">Jean Deruelle</A>
  *
  */
 public class DTMFUtils {
 	
 	private static Log logger = LogFactory.getLog(DTMFUtils.class);
 	
 	public static void adminApproval(SipSession session, String signal, String pathToAudioDirectory) {			
 		
 		if("1".equalsIgnoreCase(signal)) {
 			// Order Approved
 			logger.info("Order approved !");
 			String audioFile = pathToAudioDirectory + "OrderApproved.wav";					
 			
 			playFileInResponseToDTMFInfo(session, audioFile);
 //			try {
 //				InitialContext ctx = new InitialContext();
 //				OrderApproval orderApproval = (OrderApproval) ctx.lookup("shopping-demo/OrderApprovalAction/remote");
 //				orderApproval.fireOrderApprovedEvent();
 //			} catch (NamingException e) {
 //				logger.error("An exception occured while retrieving the EJB OrderApproval",e);
 //			}					
 		} else if("2".equalsIgnoreCase(signal)) {
 			// Order Rejected
 			logger.info("Order rejected !");
 			String audioFile = pathToAudioDirectory + "OrderCancelled.wav";					
 			
 			playFileInResponseToDTMFInfo(session, audioFile);
 //			try {
 //				InitialContext ctx = new InitialContext();
 //				OrderApproval orderApproval = (OrderApproval) ctx.lookup("shopping-demo/OrderApprovalAction/remote");
 //				orderApproval.fireOrderRejectedEvent();
 //			} catch (NamingException e) {
 //				logger.error("An exception occured while retrieving the EJB OrderApproval",e);
 //			}
 		}
 	}
 
 	public static void orderApproval(SipSession session, String signal, String pathToAudioDirectory) {
 		long orderId = (Long) session.getApplicationSession().getAttribute("orderId");
 		
 		if("1".equalsIgnoreCase(signal)) {
 			// Order Confirmed
 			logger.info("Order " + orderId + " confirmed !");
 			String audioFile = pathToAudioDirectory + "OrderApproved.wav";					
 			
 			playFileInResponseToDTMFInfo(session, audioFile);
 			try {
 				InitialContext ctx = new InitialContext();
 				OrderManager orderManager = (OrderManager) ctx.lookup("shopping-demo/OrderManagerBean/remote");
 				orderManager.confirmOrder(orderId);
 			} catch (NamingException e) {
 				logger.error("An exception occured while retrieving the EJB OrderManager",e);
 			}					
 		} else if("2".equalsIgnoreCase(signal)) {
 			// Order cancelled
 			logger.info("Order " + orderId + " cancelled !");
 			String audioFile = pathToAudioDirectory + "OrderCancelled.wav";					
 			
 			playFileInResponseToDTMFInfo(session, audioFile);
 			try {
 				InitialContext ctx = new InitialContext();
 				OrderManager orderManager = (OrderManager) ctx.lookup("shopping-demo/OrderManagerBean/remote");
 				orderManager.cancelOrder(orderId);
 			} catch (NamingException e) {
 				logger.error("An exception occured while retrieving the EJB OrderManager",e);
 			}
 		}
 	}
 
 	public static void updateDeliveryDate(SipSession session, String signal) {
		int cause = Integer.parseInt(signal);		
 
 		synchronized(session) {
 			String dateAndTime = (String) session.getAttribute("dateAndTime");
 			if(dateAndTime == null) {
 				dateAndTime = "";
 			}
 	
 			switch (cause) {
 			case Basic.CAUSE_DIGIT_0:
 				dateAndTime = dateAndTime + "0";
 				break;
 			case Basic.CAUSE_DIGIT_1:
 				dateAndTime = dateAndTime + "1";
 				break;
 			case Basic.CAUSE_DIGIT_2:
 				dateAndTime = dateAndTime + "2";
 				break;
 			case Basic.CAUSE_DIGIT_3:
 				dateAndTime = dateAndTime + "3";
 				break;
 			case Basic.CAUSE_DIGIT_4:
 				dateAndTime = dateAndTime + "4";
 				break;
 			case Basic.CAUSE_DIGIT_5:
 				dateAndTime = dateAndTime + "5";
 				break;
 			case Basic.CAUSE_DIGIT_6:
 				dateAndTime = dateAndTime + "6";
 				break;
 			case Basic.CAUSE_DIGIT_7:
 				dateAndTime = dateAndTime + "7";
 				break;
 			case Basic.CAUSE_DIGIT_8:
 				dateAndTime = dateAndTime + "8";
 				break;
 			case Basic.CAUSE_DIGIT_9:
 				dateAndTime = dateAndTime + "9";
 				break;
 			default:
 				break;
 			}
 	
 			// TODO: Add logic to check if date and time is valid. We assume that
 			// user is well educated and will always punch right date and time
 	
 			if (dateAndTime.length() == 10) {			
 				
 				char[] c = dateAndTime.toCharArray();
 	
 				StringBuffer stringBuffer = new StringBuffer();
 				stringBuffer.append("You have selected delivery date to be ");
 	
 				String date = "" + c[0] + c[1];
 				int iDate = (new Integer(date)).intValue();
 				stringBuffer.append(iDate);
 	
 				String month = "" + c[2] + c[3];
 				int iMonth = (new Integer(month)).intValue();
 	
 				String year = "" + c[4] + c[5];
 				int iYear = (new Integer(year)).intValue();
 	
 				String hour = "" + c[6] + c[7];
 				int iHour = (new Integer(hour)).intValue();
 	
 				String min = "" + c[8] + c[9];
 				int iMin = (new Integer(min)).intValue();
 	
 				switch (iMonth) {
 				case 1:
 					month = "January";
 					break;
 				case 2:
 					month = "February";
 					break;
 				case 3:
 					month = "March";
 					break;
 				case 4:
 					month = "April";
 					break;
 				case 5:
 					month = "May";
 					break;
 				case 6:
 					month = "June";
 					break;
 				case 7:
 					month = "July";
 					break;
 				case 8:
 					month = "August";
 					break;
 				case 9:
 					month = "September";
 					break;
 				case 10:
 					month = "October";
 					break;
 				case 11:
 					month = "November";
 					break;
 				case 12:
 					month = "December";
 					break;
 				default:
 					break;
 				}
 				stringBuffer.append(" of ");
 				stringBuffer.append(month);
 				stringBuffer.append(" ");
 				stringBuffer.append(2000 + iYear);
 				stringBuffer.append(" at ");
 				stringBuffer.append(iHour);
 				stringBuffer.append(" hour and ");
 				stringBuffer.append(iMin);
 				stringBuffer.append(" minute. Thank you. Bye.");
 	
 				java.sql.Timestamp timeStamp = new java.sql.Timestamp(
 						(iYear + 100), iMonth - 1, iDate, iHour, iMin, 0, 0);
 				
 				try {
 					InitialContext ctx = new InitialContext();
 					OrderManager orderManager = (OrderManager) ctx.lookup("shopping-demo/OrderManagerBean/remote");
 					orderManager.setDeliveryDate(session.getApplicationSession().getAttribute("orderId"), timeStamp);
 				} catch (NamingException e) {
 					logger.error("An exception occured while retrieving the EJB OrderManager",e);
 				}				
 				logger.info(stringBuffer.toString());
 				try {
 					TTSUtils.buildAudio(stringBuffer.toString(), "deliveryDate.wav");
 					MsConnection connection = (MsConnection) session.getApplicationSession().getAttribute("connection");
 					String endpoint = connection.getEndpoint();
 					MsSignalGenerator generator = connection.getSession().getProvider().getSignalGenerator(endpoint);
 					java.io.File speech = new File("deliveryDate.wav");
 					logger.info("Playing delivery date summary : " + "file://" + speech.getAbsolutePath());
 					MediaResourceListener mediaResourceListener = new MediaResourceListener(session, connection);
 					generator.addResourceListener(mediaResourceListener);
 					generator.apply(Announcement.PLAY, new String[]{"file://" + speech.getAbsolutePath()});
 					logger.info("delivery Date summary played. waiting for DTMF ");
 				} catch (Exception e) {
 					logger.error("An unexpected exception occured while generating the deliveryDate tts file");
 				}							
 			} else {
 				session.setAttribute("dateAndTime", dateAndTime);
 			}
 		}	
 	}
 	
 	/**
 	 * Make the media server play a file given in parameter
 	 * and add a listener so that when the media server is done playing the call is tear down 
 	 * @param session the sip session used to tear down the call
 	 * @param audioFile the file to play
 	 */
 	public static void playFileInResponseToDTMFInfo(SipSession session,
 			String audioFile) {
 		MsConnection connection = (MsConnection)session.getApplicationSession().getAttribute("connection");
 		String endpoint = connection.getEndpoint();
 		MsSignalGenerator generator = connection.getSession().getProvider().getSignalGenerator(endpoint);		
 		MediaResourceListener mediaResourceListener = new MediaResourceListener(session, connection);
 		generator.addResourceListener(mediaResourceListener);
 		generator.apply(Announcement.PLAY, new String[] { audioFile });
 		session.setAttribute("DTMFSession", DTMFListener.DTMF_SESSION_STOPPED);
 	}
 }
