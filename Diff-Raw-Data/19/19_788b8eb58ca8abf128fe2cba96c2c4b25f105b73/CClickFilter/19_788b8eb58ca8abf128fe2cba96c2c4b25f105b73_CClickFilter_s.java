 /*
   This file is part of JDasher.
 
   JDasher is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.
 
   JDasher is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
 
   You should have received a copy of the GNU General Public License
   along with JDasher; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 
   Copyright (C) 2006      Christopher Smowton <cs448@cam.ac.uk>
 
   JDasher is a port derived from the Dasher project; for information on
   the project see www.dasher.org.uk; for information on JDasher itself
   and related projects see www.smowton.net/chris
 
 */
 
 package dasher;
 
 /**
  * This is an InputFilter implementation which accepts mouse clicks
  * and causes Dasher to zoom to the location of successive clicks.
  * <p>
  * The filter does not pay any attention to the mouse position
  * except for when the user clicks the display, and does not
  * decorate the display in any way.
  * <p>
  * In order to zoom smoothly to a given location, it invokes
  * CDasherModel.ScheduleZoom, which interpolates a number of points
  * between the current crosshair location and the point clicked by
  * the user, and jumps to these points on each successive frame.
  * <p>
  * This filter registers itself with the name <i>Click Mode</i>.
  */
 
 public class CClickFilter extends CInputFilter {
 
 	private long minX;
 	/**
 	 * Sole constructor. Calls the CInputFilter constructor with a type of 7,
 	 * an ID of 1, and the name <i>Click Mode</i>.
 	 * 
 	 * @param EventHandler Event handler.
 	 * @param SettingsStore Settings repository.
 	 * @param Interface Interface with which the filter should be registered.
 	 */
 	public CClickFilter(CDasherInterfaceBase iface, CSettingsStore SettingsStore) {
 	  super(iface, SettingsStore, 7, "Click Mode");
 	  HandleEvent(new CParameterNotificationEvent(Elp_parameters.LP_MAX_ZOOM));
 	}
 
 	/**
	 * Timer simply calls the DasherModel's Tap_on_display method,
 	 * causing it to move forward a frame if there is currently
 	 * a zoom scheduled. In the event that no further destination
 	 * is scheduled (ie. we are stationary and the user has not
 	 * clicked a new destination), nothing is done.
 	 * 
 	 * @param Time Current system time as a Unix timestamp
 	 * @param pView View to be used for co-ordinate transforms
 	 * @param pInput current input device from which to obtain coordinates
 	 * @param pModel Model which will be instructed to advance a frame
 	 * @return True if the model has changed, false otherwise. 
 	 */
 	@Override public boolean Timer(long Time, CDasherView pView, CDasherInput pInput, CDasherModel m_DasherModel) {
 	  if (!m_DasherModel.nextScheduledStep(Time)) {
 		  //no steps scheduled. Reached a pause...
 		  assert (GetBoolParameter(Ebp_parameters.BP_DASHER_PAUSED));
 		  return false;
 	  }
 	  //ok - just executed a scheduled step
 	  return true;
 	}
 
 	/**
 	 * KeyDown is to be called by the Interface when the user
 	 * presses a key or clicks the mouse. ClickFilter responds to:
 	 * 
 	 * <b>Left mouse button</b>: Schedules a zoom to the clicked location.
 	 * 
 	 * @param iTime Current system time as a UNIX timestamp.
 	 * @param iId Key/button identifier.
 	 * @param Model DasherModel which should be zoomed in response to clicks.
 	 */
 	@Override public void KeyDown(long iTime, int iId, CDasherView pView, CDasherInput pInput, CDasherModel Model) {
 
 	  switch(iId) {
 	  case 100: // Mouse clicks
 	    pInput.GetDasherCoords(pView,inputCoords);
	    Model.ScheduleZoom(Math.max(minX, inputCoords[0]),inputCoords[1]);
 	    break;
 	  }
 	}
 	private final long[] inputCoords = new long[2];  
 
 	@Override public void HandleEvent(CEvent evt) {
 		if (evt instanceof CParameterNotificationEvent
 				&& ((CParameterNotificationEvent)evt).m_iParameter==Elp_parameters.LP_MAX_ZOOM)
 			minX = Math.max(2, GetLongParameter(Elp_parameters.LP_OX)/GetLongParameter(Elp_parameters.LP_MAX_ZOOM));
 	}
 }
