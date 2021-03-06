 /* JNativeHook: Global keyboard and mouse hooking for Java.
  * Copyright (C) 2006-2013 Alexander Barker.  All Rights Received.
  * http://code.google.com/p/jnativehook/
  *
  * JNativeHook is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * JNativeHook is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package org.jnativehook;
 
 // Imports
 import java.lang.reflect.Field;
 import javax.swing.event.EventListenerList;
 import org.jnativehook.keyboard.NativeKeyEvent;
 import org.jnativehook.keyboard.NativeKeyListener;
 import org.jnativehook.keyboard.listeners.NativeKeyListenerImpl;
 import org.jnativehook.mouse.NativeMouseEvent;
 import org.jnativehook.mouse.NativeMouseListener;
 import org.jnativehook.mouse.NativeMouseMotionListener;
 import org.jnativehook.mouse.NativeMouseWheelEvent;
 import org.jnativehook.mouse.NativeMouseWheelListener;
 import org.jnativehook.mouse.listeners.NativeMouseInputListenerImpl;
 import org.jnativehook.mouse.listeners.NativeMouseWheelListenerImpl;
 import org.junit.AfterClass;
 import org.junit.BeforeClass;
 import org.junit.Test;
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertNull;
 import static org.junit.Assert.assertNotNull;
 import static org.junit.Assert.assertTrue;
 import static org.junit.Assert.assertFalse;
 import static org.junit.Assert.fail;
 
 public class GlobalScreenTest {
 	@BeforeClass
 	public static void setUpClass() {
 		GlobalScreen.loadNativeLibrary();
 
 		// TODO review the generated test code and remove the default call to fail.
 		//fail("The test case is a prototype.");
 	}
 
 	@AfterClass
 	public static void tearDownClass() throws NativeHookException {
 		GlobalScreen.unloadNativeLibrary();
 	}
 
     @Test
     public void testProperties() {
 		System.out.println("properties");
 
 		assertNotNull("Auto Repeat Rate",
 				System.getProperty("jnativehook.autoRepeatRate"));
 
 		assertNotNull("Auto Repeat Delay",
 				System.getProperty("jnativehook.autoRepeatDelay"));
 
 		assertNotNull("Double Click Time",
 				System.getProperty("jnativehook.multiClickInterval"));
 
 		assertNotNull("Pointer Sensitivity",
 				System.getProperty("jnativehook.pointerSensitivity"));
 
 		assertNotNull("Pointer Acceleration Multiplier",
 				System.getProperty("jnativehook.pointerAccelerationMultiplier"));
 
 		assertNotNull("Pointer Acceleration Threshold",
 				System.getProperty("jnativehook.pointerAccelerationThreshold"));
     }
 
 	/**
 	 * Test of getInstance method, of class GlobalScreen.
 	 */
 	@Test
 	public void testGetInstance() {
 		System.out.println("getInstance");
 
 		assertNotNull("Checking getInstance() for null", GlobalScreen.getInstance());
 	}
 
 	/**
 	 * Test of addNativeKeyListener method, of class GlobalScreen.
 	 */
 	@Test
 	public void testAddNativeKeyListener() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
 		System.out.println("addNativeKeyListener");
 
 		NativeKeyListener listener = new NativeKeyListenerImpl();
 		GlobalScreen.getInstance().addNativeKeyListener(listener);
 
 		Field eventListeners = GlobalScreen.class.getDeclaredField("eventListeners");
 		eventListeners.setAccessible(true);
 		EventListenerList listeners = (EventListenerList) eventListeners.get(GlobalScreen.getInstance());
 
 		boolean found = false;
 		NativeKeyListener[] nativeKeyListeners = listeners.getListeners(NativeKeyListener.class);
 		for (int i = 0; i < nativeKeyListeners.length && !found; i++) {
 			if (nativeKeyListeners[i].equals(listener)) {
 				found = true;
 			}
 		}
 
 		if (!found) {
 			fail("Could not find the listener after it was added!");
 		}
 
 		GlobalScreen.getInstance().removeNativeKeyListener(listener);
 	}
 
 	/**
 	 * Test of removeNativeKeyListener method, of class GlobalScreen.
 	 */
 	@Test
 	public void testRemoveNativeKeyListener() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
 		System.out.println("removeNativeKeyListener");
 
 		NativeKeyListener listener = new NativeKeyListenerImpl();
 		GlobalScreen.getInstance().addNativeKeyListener(listener);
 		GlobalScreen.getInstance().removeNativeKeyListener(listener);
 
 		Field eventListeners = GlobalScreen.class.getDeclaredField("eventListeners");
 		eventListeners.setAccessible(true);
 		EventListenerList listeners = (EventListenerList) eventListeners.get(GlobalScreen.getInstance());
 
 		boolean found = false;
 		NativeKeyListener[] nativeKeyListeners = listeners.getListeners(NativeKeyListener.class);
 		for (int i = 0; i < nativeKeyListeners.length && !found; i++) {
 			if (nativeKeyListeners[i].equals(listener)) {
 				found = true;
 			}
 		}
 
 		if (found) {
 			fail("Found the listener after it was removed!");
 		}
 	}
 
 	/**
 	 * Test of addNativeMouseListener method, of class GlobalScreen.
 	 */
 	@Test
 	public void testAddNativeMouseListener() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
 		System.out.println("addNativeMouseListener");
 
 		NativeMouseListener listener = new NativeMouseInputListenerImpl();
 		GlobalScreen.getInstance().addNativeMouseListener(listener);
 
 		Field eventListeners = GlobalScreen.class.getDeclaredField("eventListeners");
 		eventListeners.setAccessible(true);
 		EventListenerList listeners = (EventListenerList) eventListeners.get(GlobalScreen.getInstance());
 
 		boolean found = false;
 		NativeMouseListener[] nativeKeyListeners = listeners.getListeners(NativeMouseListener.class);
 		for (int i = 0; i < nativeKeyListeners.length && !found; i++) {
 			if (nativeKeyListeners[i].equals(listener)) {
 				found = true;
 			}
 		}
 
 		if (!found) {
 			fail("Could not find the listener after it was added!");
 		}
 
 		GlobalScreen.getInstance().removeNativeMouseListener(listener);
 	}
 
 	/**
 	 * Test of removeNativeMouseListener method, of class GlobalScreen.
 	 */
 	@Test
 	public void testRemoveNativeMouseListener() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
 		System.out.println("removeNativeMouseListener");
 
 		NativeMouseListener listener = new NativeMouseInputListenerImpl();
 		GlobalScreen.getInstance().addNativeMouseListener(listener);
 		GlobalScreen.getInstance().removeNativeMouseListener(listener);
 
 		Field eventListeners = GlobalScreen.class.getDeclaredField("eventListeners");
 		eventListeners.setAccessible(true);
 		EventListenerList listeners = (EventListenerList) eventListeners.get(GlobalScreen.getInstance());
 
 		boolean found = false;
 		NativeMouseListener[] nativeKeyListeners = listeners.getListeners(NativeMouseListener.class);
 		for (int i = 0; i < nativeKeyListeners.length && !found; i++) {
 			if (nativeKeyListeners[i].equals(listener)) {
 				found = true;
 			}
 		}
 
 		if (found) {
 			fail("Found the listener after it was removed!");
 		}
 	}
 
 	/**
 	 * Test of addNativeMouseMotionListener method, of class GlobalScreen.
 	 */
 	@Test
 	public void testAddNativeMouseMotionListener() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
 		System.out.println("addNativeMouseMotionListener");
 
 		NativeMouseMotionListener listener = new NativeMouseInputListenerImpl();
 		GlobalScreen.getInstance().addNativeMouseMotionListener(listener);
 
 		Field eventListeners = GlobalScreen.class.getDeclaredField("eventListeners");
 		eventListeners.setAccessible(true);
 		EventListenerList listeners = (EventListenerList) eventListeners.get(GlobalScreen.getInstance());
 
 		boolean found = false;
 		NativeMouseMotionListener[] nativeKeyListeners = listeners.getListeners(NativeMouseMotionListener.class);
 		for (int i = 0; i < nativeKeyListeners.length && !found; i++) {
 			if (nativeKeyListeners[i].equals(listener)) {
 				found = true;
 			}
 		}
 
 		if (!found) {
 			fail("Could not find the listener after it was added!");
 		}
 
 		GlobalScreen.getInstance().removeNativeMouseMotionListener(listener);
 	}
 
 	/**
 	 * Test of removeNativeMouseMotionListener method, of class GlobalScreen.
 	 */
 	@Test
 	public void testRemoveNativeMouseMotionListener() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
 		System.out.println("removeNativeMouseMotionListener");
 
 		NativeMouseMotionListener listener = new NativeMouseInputListenerImpl();
 		GlobalScreen.getInstance().addNativeMouseMotionListener(listener);
 		GlobalScreen.getInstance().removeNativeMouseMotionListener(listener);
 
 		Field eventListeners = GlobalScreen.class.getDeclaredField("eventListeners");
 		eventListeners.setAccessible(true);
 		EventListenerList listeners = (EventListenerList) eventListeners.get(GlobalScreen.getInstance());
 
 		boolean found = false;
 		NativeMouseMotionListener[] nativeKeyListeners = listeners.getListeners(NativeMouseMotionListener.class);
 		for (int i = 0; i < nativeKeyListeners.length && !found; i++) {
 			if (nativeKeyListeners[i].equals(listener)) {
 				found = true;
 			}
 		}
 
 		if (found) {
 			fail("Found the listener after it was removed!");
 		}
 	}
 
 	/**
 	 * Test of addNativeMouseWheelListener method, of class GlobalScreen.
 	 */
 	@Test
 	public void testAddNativeMouseWheelListener() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
 		System.out.println("addNativeMouseWheelListener");
 
 
 		NativeMouseWheelListener listener = new NativeMouseWheelListenerImpl();
 		GlobalScreen.getInstance().addNativeMouseWheelListener(listener);
 
 		Field eventListeners = GlobalScreen.class.getDeclaredField("eventListeners");
 		eventListeners.setAccessible(true);
 		EventListenerList listeners = (EventListenerList) eventListeners.get(GlobalScreen.getInstance());
 
 		boolean found = false;
 		NativeMouseWheelListener[] nativeKeyListeners = listeners.getListeners(NativeMouseWheelListener.class);
 		for (int i = 0; i < nativeKeyListeners.length && !found; i++) {
 			if (nativeKeyListeners[i].equals(listener)) {
 				found = true;
 			}
 		}
 
 		if (!found) {
 			fail("Could not find the listener after it was added!");
 		}
 
 		GlobalScreen.getInstance().removeNativeMouseWheelListener(listener);
 	}
 
 	/**
 	 * Test of removeNativeMouseWheelListener method, of class GlobalScreen.
 	 */
 	@Test
 	public void testRemoveNativeMouseWheelListener() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
 		System.out.println("removeNativeMouseWheelListener");
 
 		NativeMouseWheelListener listener = new NativeMouseWheelListenerImpl();
 		GlobalScreen.getInstance().addNativeMouseWheelListener(listener);
 		GlobalScreen.getInstance().removeNativeMouseWheelListener(listener);
 
 		Field eventListeners = GlobalScreen.class.getDeclaredField("eventListeners");
 		eventListeners.setAccessible(true);
 		EventListenerList listeners = (EventListenerList) eventListeners.get(GlobalScreen.getInstance());
 
 		boolean found = false;
 		NativeMouseWheelListener[] nativeKeyListeners = listeners.getListeners(NativeMouseWheelListener.class);
 		for (int i = 0; i < nativeKeyListeners.length && !found; i++) {
 			if (nativeKeyListeners[i].equals(listener)) {
 				found = true;
 			}
 		}
 
 		if (found) {
 			fail("Found the listener after it was removed!");
 		}
 	}
 
 	/**
 	 * Test of registerNativeHook method, of class GlobalScreen.
 	 */
 	@Test
 	public void testRegisterNativeHook() throws NativeHookException {
 		System.out.println("registerNativeHook");
 
 		GlobalScreen.registerNativeHook();
 	}
 
 	/**
 	 * Test of unregisterNativeHook method, of class GlobalScreen.
 	 */
 	@Test
 	public void testUnregisterNativeHook() {
 		System.out.println("unregisterNativeHook");
 
 		GlobalScreen.unregisterNativeHook();
 	}
 
 	/**
 	 * Test of isNativeHookRegistered method, of class GlobalScreen.
 	 */
 	@Test
 	public void testIsNativeHookRegistered() throws NativeHookException {
 		System.out.println("isNativeHookRegistered");
 
 		GlobalScreen.registerNativeHook();
 		assertTrue(GlobalScreen.isNativeHookRegistered());
 
 		GlobalScreen.unregisterNativeHook();
 		assertFalse(GlobalScreen.isNativeHookRegistered());
 	}
	
 	/**
 	 * Test of dispatchEvent method, of class GlobalScreen.
 	 */
 	@Test
 	public void testPostNativeEvent() throws InterruptedException {
 		System.out.println("dispatchEvent");
 
 		// Setup and event listener.
 		NativeKeyListenerImpl keyListener = new NativeKeyListenerImpl();
 		GlobalScreen.getInstance().addNativeKeyListener(keyListener);
 
 		NativeMouseInputListenerImpl mouseListener = new NativeMouseInputListenerImpl();
 		GlobalScreen.getInstance().addNativeMouseListener(mouseListener);
 
 		NativeMouseWheelListenerImpl wheelListener = new NativeMouseWheelListenerImpl();
 		GlobalScreen.getInstance().addNativeMouseWheelListener(wheelListener);
 
 		// Make sure the dispatcher is running!
 		GlobalScreen.getInstance().startEventDispatcher();
 
 		// Dispatch a key event and check to see if it was sent.
 		NativeKeyEvent keyEvent = new NativeKeyEvent(
 				NativeKeyEvent.NATIVE_KEY_PRESSED,
 				System.currentTimeMillis(),
 				0x00,		// Modifiers
 				0x41,		// Raw Code
				NativeKeyEvent.VK_UNDEFINED,
 				NativeKeyEvent.CHAR_UNDEFINED,
				NativeKeyEvent.KEY_LOCATION_UNKNOWN);
 
 		synchronized (keyListener) {
 			GlobalScreen.postNativeEvent(keyEvent);
 			keyListener.wait(3000);
 			assertEquals(keyEvent, keyListener.getLastEvent());
 		}
 

 		// Dispatch a mouse event and check to see if it was sent.
 		NativeMouseEvent mouseEvent = new NativeMouseEvent(
 				NativeMouseEvent.NATIVE_MOUSE_CLICKED,
 				System.currentTimeMillis(),
 				0x00,	// Modifiers
 				50,		// X
 				75,		// Y
 				1,		// Click Count
 				NativeMouseEvent.BUTTON1);
 
 		synchronized (mouseListener) {
 			GlobalScreen.postNativeEvent(mouseEvent);
 			mouseListener.wait(3000);
 			assertEquals(mouseEvent, mouseListener.getLastEvent());
 		}
 
 		// Dispatch a mouse event and check to see if it was sent.
 		NativeMouseWheelEvent wheelEvent = new NativeMouseWheelEvent(
 				NativeMouseEvent.NATIVE_MOUSE_WHEEL,
 				System.currentTimeMillis(),
 				0x00,	// Modifiers
 				50,		// X
 				75,		// Y
 				1,		// Click Count
 				NativeMouseWheelEvent.WHEEL_UNIT_SCROLL,
 				3,		// Scroll Amount
 				-1);	// Wheel Rotation
 
 		synchronized (wheelListener) {
 			GlobalScreen.postNativeEvent(wheelEvent);
 			wheelListener.wait(3000);
 			assertEquals(wheelEvent, wheelListener.getLastEvent());
 		}

 		// Stop the event dispatcher.
 		GlobalScreen.getInstance().stopEventDispatcher();
 
 		// Remove all added listeners.
 		GlobalScreen.getInstance().removeNativeKeyListener(keyListener);
 		GlobalScreen.getInstance().removeNativeMouseListener(mouseListener);
 		GlobalScreen.getInstance().removeNativeMouseWheelListener(wheelListener);
 	}
	
 	/**
 	 * Test of dispatchEvent method, of class GlobalScreen.
 	 */
 	@Test
 	public void testDispatchEvent() throws InterruptedException {
 		System.out.println("dispatchEvent");
 
 		// Setup and event listener.
 		NativeKeyListenerImpl keyListener = new NativeKeyListenerImpl();
 		GlobalScreen.getInstance().addNativeKeyListener(keyListener);
 
 		NativeMouseInputListenerImpl mouseListener = new NativeMouseInputListenerImpl();
 		GlobalScreen.getInstance().addNativeMouseListener(mouseListener);
 
 		NativeMouseWheelListenerImpl wheelListener = new NativeMouseWheelListenerImpl();
 		GlobalScreen.getInstance().addNativeMouseWheelListener(wheelListener);
 
 		// Make sure the dispatcher is running!
 		GlobalScreen.getInstance().startEventDispatcher();
 
 		// Dispatch a key event and check to see if it was sent.
 		NativeKeyEvent keyEvent = new NativeKeyEvent(
 				NativeKeyEvent.NATIVE_KEY_PRESSED,
 				System.currentTimeMillis(),
 				0x00,		// Modifiers
 				0x41,		// Raw Code
				NativeKeyEvent.VK_UNDEFINED,
 				NativeKeyEvent.CHAR_UNDEFINED,
				NativeKeyEvent.KEY_LOCATION_UNKNOWN);
 
 		synchronized (keyListener) {
 			GlobalScreen.getInstance().dispatchEvent(keyEvent);
 			keyListener.wait(3000);
 			assertEquals(keyEvent, keyListener.getLastEvent());
 		}
 
 
 		// Dispatch a mouse event and check to see if it was sent.
 		NativeMouseEvent mouseEvent = new NativeMouseEvent(
 				NativeMouseEvent.NATIVE_MOUSE_CLICKED,
 				System.currentTimeMillis(),
 				0x00,	// Modifiers
 				50,		// X
 				75,		// Y
 				1,		// Click Count
 				NativeMouseEvent.BUTTON1);
 
 		synchronized (mouseListener) {
 			GlobalScreen.getInstance().dispatchEvent(mouseEvent);
 			mouseListener.wait(3000);
 			assertEquals(mouseEvent, mouseListener.getLastEvent());
 		}
 
 		// Dispatch a mouse event and check to see if it was sent.
 		NativeMouseWheelEvent wheelEvent = new NativeMouseWheelEvent(
 				NativeMouseEvent.NATIVE_MOUSE_WHEEL,
 				System.currentTimeMillis(),
 				0x00,	// Modifiers
 				50,		// X
 				75,		// Y
 				1,		// Click Count
 				NativeMouseWheelEvent.WHEEL_UNIT_SCROLL,
 				3,		// Scroll Amount
 				-1);	// Wheel Rotation
 
 		synchronized (wheelListener) {
 			GlobalScreen.getInstance().dispatchEvent(wheelEvent);
 			wheelListener.wait(3000);
 			assertEquals(wheelEvent, wheelListener.getLastEvent());
 		}
 
 		// Stop the event dispatcher.
 		GlobalScreen.getInstance().stopEventDispatcher();
 
 		// Remove all added listeners.
 		GlobalScreen.getInstance().removeNativeKeyListener(keyListener);
 		GlobalScreen.getInstance().removeNativeMouseListener(mouseListener);
 		GlobalScreen.getInstance().removeNativeMouseWheelListener(wheelListener);
 	}
 
 	/**
 	 * Test of processKeyEvent method, of class GlobalScreen.
 	 */
 	@Test
 	public void testProcessKeyEvent() {
 		System.out.println("processKeyEvent");
 
 		// Setup and event listener.
 		NativeKeyListenerImpl listener = new NativeKeyListenerImpl();
 		GlobalScreen.getInstance().addNativeKeyListener(listener);
 
 		// Dispatch a key event and check to see if it was sent.
 		NativeKeyEvent event = new NativeKeyEvent(
 				NativeKeyEvent.NATIVE_KEY_PRESSED,
 				System.currentTimeMillis(),
 				0x00,		// Modifiers
 				0x41,		// Raw Code
 				NativeKeyEvent.VK_UNDEFINED,
 				NativeKeyEvent.CHAR_UNDEFINED,
 				NativeKeyEvent.KEY_LOCATION_UNKNOWN);
 
 		GlobalScreen.getInstance().processKeyEvent(event);
 		assertEquals(event, listener.getLastEvent());
 
 		GlobalScreen.getInstance().removeNativeKeyListener(listener);
 	}
 
 	/**
 	 * Test of processMouseEvent method, of class GlobalScreen.
 	 */
 	@Test
 	public void testProcessMouseEvent() {
 		System.out.println("processMouseEvent");
 
 		// Setup and event listener.
 		NativeMouseInputListenerImpl listener = new NativeMouseInputListenerImpl();
 		GlobalScreen.getInstance().addNativeMouseListener(listener);
 
 		// Dispatch a mouse event and check to see if it was sent.
 		NativeMouseEvent event = new NativeMouseEvent(
 				NativeMouseEvent.NATIVE_MOUSE_CLICKED,
 				System.currentTimeMillis(),
 				0x00,	// Modifiers
 				50,		// X
 				75,		// Y
 				1,		// Click Count
 				NativeMouseEvent.BUTTON1);
 
 		GlobalScreen.getInstance().processMouseEvent(event);
 		assertEquals(event, listener.getLastEvent());
 
 		GlobalScreen.getInstance().removeNativeMouseListener(listener);
 	}
 
 	/**
 	 * Test of processMouseWheelEvent method, of class GlobalScreen.
 	 */
 	@Test
 	public void testProcessMouseWheelEvent() {
 		System.out.println("processMouseWheelEvent");
 
 		// Setup and event listener.
 		NativeMouseWheelListenerImpl listener = new NativeMouseWheelListenerImpl();
 		GlobalScreen.getInstance().addNativeMouseWheelListener(listener);
 
 		// Dispatch a mouse event and check to see if it was sent.
 		NativeMouseWheelEvent event = new NativeMouseWheelEvent(
 				NativeMouseEvent.NATIVE_MOUSE_WHEEL,
 				System.currentTimeMillis(),
 				0x00,	// Modifiers
 				50,		// X
 				75,		// Y
 				1,		// Click Count
 				NativeMouseWheelEvent.WHEEL_UNIT_SCROLL,
 				3,		// Scroll Amount
 				-1);	// Wheel Rotation
 
 		GlobalScreen.getInstance().processMouseWheelEvent(event);
 		assertEquals(event, listener.getLastEvent());
 
 		GlobalScreen.getInstance().removeNativeMouseWheelListener(listener);
 	}
 
 	/**
 	 * Test of startEventDispatcher method, of class GlobalScreen.
 	 */
 	@Test
 	public void testStartEventDispatcher() throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
 		System.out.println("startEventDispatcher");
 
 		GlobalScreen.getInstance().startEventDispatcher();
 
 		Field eventExecutor = GlobalScreen.class.getDeclaredField("eventExecutor");
 		eventExecutor.setAccessible(true);
 		assertNotNull(eventExecutor.get(GlobalScreen.getInstance()));
 	}
 
 	/**
 	 * Test of stopEventDispatcher method, of class GlobalScreen.
 	 */
 	@Test
 	public void testStopEventDispatcher() throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
 		System.out.println("stopEventDispatcher");
 
 		GlobalScreen.getInstance().stopEventDispatcher();
 
 		Field eventExecutor = GlobalScreen.class.getDeclaredField("eventExecutor");
 		eventExecutor.setAccessible(true);
 		assertNull(eventExecutor.get(GlobalScreen.getInstance()));
 	}
 }
