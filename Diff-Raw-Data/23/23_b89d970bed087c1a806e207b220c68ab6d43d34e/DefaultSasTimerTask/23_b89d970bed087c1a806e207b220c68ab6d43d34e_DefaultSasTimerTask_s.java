 /*
  * JBoss, Home of Professional Open Source
  * Copyright 2008, Red Hat Middleware LLC, and individual contributors
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
  *
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
 package org.mobicents.servlet.sip.core.timers;
 
 import java.util.concurrent.ScheduledFuture;
 import java.util.concurrent.TimeUnit;
 
 import org.apache.log4j.Logger;
 import org.mobicents.servlet.sip.core.session.MobicentsSipApplicationSession;
 import org.mobicents.servlet.sip.core.session.SipApplicationSessionEventType;
 import org.mobicents.servlet.sip.startup.SipContext;
 
 /**
  * Timer task that will notify the listeners that the sip application session has expired 
  * It is an improved timer task that is delayed every time setLastAccessedTime is called on it.
  * It is delayed of lastAccessedTime + lifetime
  * 
  * @author <A HREF="mailto:jean.deruelle@gmail.com">Jean Deruelle</A>
  */
 public class DefaultSasTimerTask implements SipApplicationSessionTimerTask {
 	
 	private static final Logger logger = Logger.getLogger(DefaultSasTimerTask.class);
 	
 	private MobicentsSipApplicationSession sipApplicationSession;
 	
 	protected transient ScheduledFuture<MobicentsSipApplicationSession> expirationTimerFuture;
 	
 	public DefaultSasTimerTask(MobicentsSipApplicationSession mobicentsSipApplicationSession) {
 		this.sipApplicationSession = mobicentsSipApplicationSession;
 	}
 	
 	@SuppressWarnings("unchecked")
 	public void run() {	
 		if(logger.isDebugEnabled()) {
 			logger.debug("initial kick off of SipApplicationSessionTimerTask running for sip application session " + sipApplicationSession.getId());
 		}
 					
		long now = System.currentTimeMillis();
		if(sipApplicationSession.getExpirationTimeInternal() > now) {
 			// if the session has been accessed since we started it, put it to sleep
			long sleep =  getDelay();
 			if(logger.isDebugEnabled()) {
 				logger.debug("expirationTime is " + sipApplicationSession.getExpirationTimeInternal() + 
						", now is " + now + 
 						" sleeping for " + sleep / 1000L + " seconds");
 			}
 			final SipContext sipContext = sipApplicationSession.getSipContext();
 			final SipApplicationSessionTimerTask expirationTimerTask = sipContext.getSipApplicationSessionTimerService().createSipApplicationSessionTimerTask(sipApplicationSession);
 //			sipContext.getSipApplicationSessionTimerService().cancel(expirationTimerTask);
 			sipApplicationSession.setExpirationTimerTask(expirationTimerTask);					
 			expirationTimerFuture = (ScheduledFuture<MobicentsSipApplicationSession>) sipContext.getSipApplicationSessionTimerService().schedule(expirationTimerTask, sleep, TimeUnit.MILLISECONDS);
 		} else {
 			tryToExpire();
 		}
 	}
 
 	private void tryToExpire() {
 		final SipContext sipContext = getSipApplicationSession().getSipContext();
 		sipContext.enterSipApp(getSipApplicationSession(), null);
 		sipContext.enterSipAppHa(true);
 		try {
 			getSipApplicationSession().notifySipApplicationSessionListeners(SipApplicationSessionEventType.EXPIRATION);
 			//It is possible that the application grant an extension to the lifetime of the session, thus the sip application
 			//should not be treated as expired.
 			if(getDelay() <= 0) {
 				
 				getSipApplicationSession().setExpired(true);
 				if(getSipApplicationSession().isValidInternal()) {			
 					getSipApplicationSession().invalidate();				
 				}
 			}
 		} finally {							
 			sipContext.exitSipAppHa(null, null);
 			sipContext.exitSipApp(getSipApplicationSession(), null);
 			setSipApplicationSession(null);
 		}
 	}				
 	
 	public long getDelay() {
 		return sipApplicationSession.getExpirationTimeInternal() - System.currentTimeMillis();
 	}
 
 	/**
 	 * @param sipApplicationSession the sipApplicationSession to set
 	 */
 	public void setSipApplicationSession(MobicentsSipApplicationSession sipApplicationSession) {
 		this.sipApplicationSession = sipApplicationSession;
 	}
 
 	/**
 	 * @return the sipApplicationSession
 	 */
 	public MobicentsSipApplicationSession getSipApplicationSession() {
 		return sipApplicationSession;
 	}
 
 	public void setScheduledFuture(ScheduledFuture<MobicentsSipApplicationSession> schedule) {
 		expirationTimerFuture = schedule;
 	}						
 	
 	public ScheduledFuture<MobicentsSipApplicationSession> getScheduledFuture() {
 		return expirationTimerFuture;
 	}
 } 
