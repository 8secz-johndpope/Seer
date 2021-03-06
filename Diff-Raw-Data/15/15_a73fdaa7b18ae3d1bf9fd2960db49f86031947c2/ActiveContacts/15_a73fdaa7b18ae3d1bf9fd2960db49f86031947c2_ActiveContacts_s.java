 /*
  * ActiveContacts.java
  *
  * Created on 20.01.2005, 21:20
  * Copyright (c) 2005-2008, Eugene Stahov (evgs), http://bombus-im.org
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License
  * as published by the Free Software Foundation; either version 2
  * of the License, or (at your option) any later version.
  *
  * You can also redistribute and/or modify this program under the
  * terms of the Psi License, specified in the accompanied COPYING
  * file, as published by the Psi Project; either dated January 1st,
  * 2005, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this library; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  *
  */
 package Client;
 
 import java.util.Enumeration;
 import locale.SR;
 import ui.MainBar;
 import ui.controls.form.DefForm;
 
 /**
  *
  * @author EvgS
  */
 public class ActiveContacts extends DefForm {
 
     /** Creates a new instance of ActiveContacts
      * @param current
      */
     public ActiveContacts(Contact current) {
         super(SR.MS_ACTIVE_CONTACTS, false);
 
         enableListWrapping(true);
         for (Enumeration r = sd.roster.hContacts.elements(); r.hasMoreElements();) {
             Contact c = (Contact) r.nextElement();
             if (c.active()) {
                 itemsList.addElement(c);
             }
         }        
 
     if (getItemCount() == 0) {
             return;
         }
 
         MainBar mb = new MainBar(2, String.valueOf(getItemCount()), " ", false);
         mb.addElement(SR.MS_ACTIVE_CONTACTS);
         setMainBarItem(mb);
         show();
         try {
             int focus = itemsList.indexOf(current);
             moveCursorTo(focus);
         } catch (Exception e) { }
     }
 
     public void cmdOk() {
         eventOk();
     }
 
     public void eventOk() {
         if (getItemCount() > 0) {
             Contact c = (Contact) getFocusedObject();
             new ContactMessageList(c);
         }
         //c.msgSuspended=null; // clear suspended message for selected contact
     }
 
     public void focusToNextUnreaded() {
         if (getItemCount() < 1) {
             return;
         }
 
         Contact c = (Contact) getFocusedObject();
         Enumeration i = itemsList.elements();
 
         int pass = 0; //
         while (pass < 2) {
             if (!i.hasMoreElements()) {
                 i = itemsList.elements();
             }
             Contact p = (Contact) i.nextElement();
             if (pass == 1) {
                 if (p.getNewMsgsCount() > 0) {
                     focusToContact(p);
                     setRotator();
                     break;
                 }
             }
             if (p == c) {
                 pass++; // полный круг пройден
             }
         }
     }
 
     private void focusToContact(final Contact c) {
         int index = itemsList.indexOf(c);
         if (index >= 0) {
             moveCursorTo(index);
         }
     }
 
    public void messageEditResume() {
         Contact c = (Contact) getFocusedObject();
         ui.VirtualList pview = new ContactMessageList(c);
         Roster.me = null;
         Roster.me = new MessageEdit(pview, c, c.msgSuspended);
         c.msgSuspended = null;
     }
 
    public void clearReadedInFocused() {
         Contact c = (Contact) getFocusedObject();
         c.purge();
         itemsList.removeElementAt(getCursor());
         getMainBarItem().setElementAt(String.valueOf(getItemCount()), 0);
     }
 
     public final void destroyView() {
         sd.roster.reEnumRoster();
         super.destroyView();
     }
 
     public void userKeyPressed(int key) {
         switch(key) {
             case 0:
                 focusToNextUnreaded();
         }
         super.userKeyPressed(key);
     }
 
     public String touchLeftCommand() {
         return SR.MS_SELECT;
     }
 }
