 /*
  * 
  * 
  * Copyright (C) 2005 SIPfoundry Inc.
  * Licensed by SIPfoundry under the LGPL license.
  * 
  * Copyright (C) 2005 Pingtel Corp.
  * Licensed to SIPfoundry under a Contributor Agreement.
  * 
  * $
  */
 package org.sipfoundry.sipxconfig.site.admin;
 
 import org.apache.tapestry.event.PageBeginRenderListener;
 import org.apache.tapestry.event.PageEvent;
 import org.apache.tapestry.html.BasePage;
 import org.sipfoundry.sipxconfig.admin.parkorbit.ParkOrbitContext;
 import org.sipfoundry.sipxconfig.components.TapestryUtils;
 
 public abstract class MusicOnHold extends BasePage implements PageBeginRenderListener {
     public abstract ParkOrbitContext getParkOrbitContext();
 
     public abstract String getMusic();
 
     public abstract void setMusic(String music);
 
     public void pageBeginRender(PageEvent event_) {
         String music = getMusic();
         if (null == music) {
             music = getParkOrbitContext().getDefaultMusicOnHold();
             setMusic(music);
         }
     }
 
     public void saveValid() {
        if (!TapestryUtils.isValid(this)) {
            return;
         }
        ParkOrbitContext context = getParkOrbitContext();
        context.setDefaultMusicOnHold(getMusic());
        context.activateParkOrbits();
     }
 }
