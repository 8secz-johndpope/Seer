 /*
  * Copyright 2011 cruxframework.org.
  * 
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
  * use this file except in compliance with the License. You may obtain a copy of
  * the License at
  * 
  * http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  */
 package org.cruxframework.crux.core.client.screen.views;
 
 import org.cruxframework.crux.core.client.collection.FastList;
 import org.cruxframework.crux.core.client.executor.BeginEndExecutor;
 import org.cruxframework.crux.core.client.screen.DeviceAdaptive.Device;
 import org.cruxframework.crux.core.client.screen.Screen;
 
 import com.google.gwt.core.client.JavaScriptObject;
 import com.google.gwt.dom.client.Element;
 import com.google.gwt.dom.client.IFrameElement;
 import com.google.gwt.event.logical.shared.CloseEvent;
 import com.google.gwt.event.logical.shared.CloseHandler;
 import com.google.gwt.event.logical.shared.ResizeEvent;
 import com.google.gwt.event.logical.shared.ResizeHandler;
 import com.google.gwt.event.logical.shared.ValueChangeEvent;
 import com.google.gwt.event.logical.shared.ValueChangeHandler;
 import com.google.gwt.event.shared.HandlerRegistration;
 import com.google.gwt.user.client.DOM;
 import com.google.gwt.user.client.History;
 import com.google.gwt.user.client.Window;
 import com.google.gwt.user.client.Window.ClosingEvent;
 import com.google.gwt.user.client.Window.ClosingHandler;
 import com.google.gwt.user.client.ui.RootPanel;
 
 /**
  * @author Thiago da Rosa de Bustamante
  *
  */
 public class ViewHandlers
 {
 	private static boolean initialized = false;
 	private static FastList<ViewContainer> boundContainers = null;
 	private static boolean hasWindowResizeHandler = false;
 	private static boolean hasOrientationChangeOrResizeHandler = false;
 	private static boolean hasWindowCloseHandler = false;
 	private static boolean hasWindowClosingHandler = false;
 	private static boolean hasHistoryHandler = false;
 	private static boolean historyFrameInitialized = false;
 	private static HandlerRegistration resizeHandler;
 	private static HandlerRegistration orientationChangeOrResizeHandler;
 	private static HandlerRegistration closeHandler;
 	private static HandlerRegistration closingHandler;
 	private static HandlerRegistration historyHandler;
 
 	protected static void initializeWindowContainers()
 	{
 		if (!initialized)
 		{
 			boundContainers = new FastList<ViewContainer>();
 			initialized = true;
 		}
 	}
 
 	/**
 	 * This method must be called when the container is attached to DOM
 	 */
 	protected static void bindToDOM(ViewContainer container)
 	{
 		boundContainers.add(container);
 		ensureViewContainerHandlers(container);
 	}
 	
 	/**
 	 * This method must be called when the container is detached from DOM
 	 */
 	protected static void unbindToDOM(ViewContainer container)
 	{
 		boundContainers.remove(boundContainers.indexOf(container));
 		ViewHandlers.removeViewContainerHandlers();
 	}
 	
 	/**
 	 * 
 	 * @param viewContainer
 	 */
 	protected static void ensureViewContainerHandlers(ViewContainer viewContainer)
     {
 	    ensureViewContainerResizeHandler(viewContainer);
 	    ensureViewContainerOrientationChangeOrResizeHandler(viewContainer);
 	    ensureViewContainerCloseHandler(viewContainer);
 	    ensureViewContainerClosingHandler(viewContainer);
 	    ensureViewContainerHistoryHandler(viewContainer);
     }
 
 	/**
 	 * 
 	 */
 	protected static void removeViewContainerHandlers()
     {
 	    removeViewContainerResizeHandler();
 	    removeViewContainerOrientationChangeOrResizeHandler();
 	    removeViewContainerCloseHandler();
 	    removeViewContainerClosingHandler();
 	    removeViewContainerHistoryHandler();
     }
 
 	/**
 	 * 
 	 * @param viewContainer
 	 */
 	protected static void ensureViewContainerResizeHandler(ViewContainer viewContainer)
     {
 	    if (!hasWindowResizeHandler && viewContainer.hasResizeHandlers())
 	    {
 	    	hasWindowResizeHandler = true;
 	    	resizeHandler = Window.addResizeHandler(new ResizeHandler()
 			{
 				@Override
 				public void onResize(ResizeEvent event)
 				{
 					for (int i=0; i< boundContainers.size(); i++)
 					{
 						boundContainers.get(i).notifyViewsAboutWindowResize(event);
 					}
 				}
 			});
 	    }
     }
 
 	/**
 	 * 
 	 * @param viewContainer
 	 */
 	protected static void ensureViewContainerHistoryHandler(ViewContainer viewContainer)
     {
 	    if (!hasHistoryHandler && viewContainer.hasHistoryHandlers())
 	    {
 	    	prepareHistoryFrame();
 	    	hasHistoryHandler = true;
 	    	historyHandler = History.addValueChangeHandler(new ValueChangeHandler<String>()
 			{
 				@Override
 				public void onValueChange(ValueChangeEvent<String> event)
 				{
 					for (int i=0; i< boundContainers.size(); i++)
 					{
 						boundContainers.get(i).notifyViewsAboutHistoryChange(event);
 					}
 				}
 			}); 
 	    }
     }
 
 	/**
 	 * 
 	 * @param viewContainer
 	 */
 	protected static void ensureViewContainerOrientationChangeOrResizeHandler(ViewContainer viewContainer)
     {
 	    if (!hasOrientationChangeOrResizeHandler && viewContainer.hasOrientationChangeOrResizeHandlers())
 	    {
 	    	hasOrientationChangeOrResizeHandler = true;
 	    	orientationChangeOrResizeHandler = addWindowOrientationChangeOrResizeHandler(new OrientationChangeOrResizeHandler()
 			{
 				@Override
 				public void onOrientationChangeOrResize()
 				{
 					for (int i=0; i< boundContainers.size(); i++)
 					{
 						boundContainers.get(i).notifyViewsAboutOrientationChangeOrResize();
 					}
 				}
 			});
 	    }
     }
 
 	/**
 	 * 
 	 * @param viewContainer
 	 */
 	protected static void ensureViewContainerCloseHandler(ViewContainer viewContainer)
     {
 	    if (!hasWindowCloseHandler && viewContainer.hasWindowCloseHandlers())
 	    {
 	    	hasWindowCloseHandler = true;
 	    	closeHandler = Window.addCloseHandler(new CloseHandler<Window>()
 			{
 				@Override
 				public void onClose(CloseEvent<Window> event)
 				{
 					for (int i=0; i< boundContainers.size(); i++)
 					{
 						boundContainers.get(i).notifyViewsAboutWindowClose(event);
 					}
 				}
 			});
 	    }
     }
 
 	/**
 	 * 
 	 * @param viewContainer
 	 */
 	protected static void ensureViewContainerClosingHandler(ViewContainer viewContainer)
     {
 	    if (!hasWindowClosingHandler && viewContainer.hasWindowClosingHandlers())
 	    {
 	    	hasWindowClosingHandler = true;
 	    	closingHandler = Window.addWindowClosingHandler(new ClosingHandler()
 			{
 				@Override
 				public void onWindowClosing(ClosingEvent event)
 				{
 					for (int i=0; i< boundContainers.size(); i++)
 					{
 						boundContainers.get(i).notifyViewsAboutWindowClosing(event);
 					}
 				}
 			});
 	    }
     }
 
 	/**
 	 * 
 	 */
 	private static void removeViewContainerOrientationChangeOrResizeHandler()
     {
 	    if (hasOrientationChangeOrResizeHandler)
 		{
 	    	boolean hasOrientationOrResizeHandlers = false;
 	    	for(int i=0; i< boundContainers.size(); i++)
 	    	{
 	    		if (boundContainers.get(i).hasOrientationChangeOrResizeHandlers())
 	    		{
 	    			hasOrientationOrResizeHandlers = true;
 	    			break;
 	    		}
 	    	}
 	    	if (!hasOrientationOrResizeHandlers)
 	    	{
 	    		if(orientationChangeOrResizeHandler != null)
 	    		{
 	    			orientationChangeOrResizeHandler.removeHandler();
 	    		}
 	    		orientationChangeOrResizeHandler = null;
 	    		hasOrientationChangeOrResizeHandler = false;
 	    	}
 		}
     }
 
 	/**
 	 * 
 	 */
 	private static void removeViewContainerResizeHandler()
     {
 	    if (hasWindowResizeHandler)
 		{
 	    	boolean hasResizeHandlers = false;
 	    	for(int i=0; i< boundContainers.size(); i++)
 	    	{
 	    		if (boundContainers.get(i).hasResizeHandlers())
 	    		{
 	    			hasResizeHandlers = true;
 	    			break;
 	    		}
 	    	}
 	    	if (!hasResizeHandlers)
 	    	{
 	    		if(resizeHandler != null)
 	    		{
 	    			resizeHandler.removeHandler();
 	    		}
 	    		resizeHandler = null;
 	    		hasWindowResizeHandler = false;
 	    	}
 		}
     }
 
 	/**
 	 * 
 	 */
 	private static void removeViewContainerHistoryHandler()
     {
 	    if (hasHistoryHandler)
 		{
 	    	boolean hasHistoryHandlers = false;
 	    	for(int i=0; i< boundContainers.size(); i++)
 	    	{
 	    		if (boundContainers.get(i).hasHistoryHandlers())
 	    		{
 	    			hasHistoryHandlers = true;
 	    			break;
 	    		}
 	    	}
 	    	if (!hasHistoryHandlers)
 	    	{
 	    		if(historyHandler != null)
 	    		{
 	    			historyHandler.removeHandler();
 	    		}
 	    		historyHandler = null;
 	    		hasHistoryHandler = false;
 	    	}
 		}
     }
 
 	/**
 	 * 
 	 */
 	private static void removeViewContainerCloseHandler()
     {
 	    if (hasWindowCloseHandler)
 		{
 	    	boolean hasCloseHandlers = false;
 	    	for(int i=0; i< boundContainers.size(); i++)
 	    	{
 	    		if (boundContainers.get(i).hasWindowCloseHandlers())
 	    		{
 	    			hasCloseHandlers = true;
 	    			break;
 	    		}
 	    	}
 	    	if (!hasCloseHandlers)
 	    	{
 	    		if(closeHandler != null)
 	    		{
 	    			closeHandler.removeHandler();
 	    		}
 	    		closeHandler = null;
 	    		hasWindowCloseHandler = false;
 	    	}
 		}
     }
 
 	/**
 	 * 
 	 */
 	private static void removeViewContainerClosingHandler()
     {
 	    if (hasWindowClosingHandler)
 		{
 	    	boolean hasClosingHandlers = false;
 	    	for(int i=0; i< boundContainers.size(); i++)
 	    	{
 	    		if (boundContainers.get(i).hasWindowClosingHandlers())
 	    		{
 	    			hasClosingHandlers = true;
 	    			break;
 	    		}
 	    	}
 	    	if (!hasClosingHandlers)
 	    	{
 	    		if(closingHandler != null)
 	    		{
 	    			closingHandler.removeHandler();
 	    		}
 	    		closingHandler = null;
 	    		hasWindowClosingHandler = false;
 	    	}
 		}
     }
 	
 	/**
 	 * @param handler
 	 * @return
 	 */
 	private static HandlerRegistration addWindowOrientationChangeOrResizeHandler(final OrientationChangeOrResizeHandler handler) 
 	{
 		final BeginEndExecutor executor = new BeginEndExecutor(100) 
 		{
 			private int clientHeight = Window.getClientHeight();
 			private int clientWidth = Window.getClientWidth();
 
 			@Override
 			protected void doEndAction() 
 			{
 				if (!Screen.getCurrentDevice().equals(Device.largeDisplayMouse))
 				{
 					int newClientHeight = Window.getClientHeight();
 					int newClientWidth = Window.getClientWidth();
 
 					if (this.clientHeight != newClientHeight || clientWidth != newClientWidth)
 					{
 						handler.onOrientationChangeOrResize();
 					}
 					clientHeight = newClientHeight;
 					clientWidth  = newClientWidth;
 				}
 				else
 				{
 					handler.onOrientationChangeOrResize();
 				}
 			}
 			
 			@Override
 			protected void doBeginAction() 
 			{
 				// nothing
 			}
 		};
 		
 		ResizeHandler resizeHandler = new ResizeHandler() 
 		{
 			public void onResize(ResizeEvent event) 
 			{
				if(Screen.isIos())
				{
					handler.onOrientationChangeOrResize();
				} else
				{
					executor.execute();					
				}
 			}
 		};
 		
 		final HandlerRegistration resizeHandlerRegistration = Window.addResizeHandler(resizeHandler);
 		final JavaScriptObject orientationHandler = attachOrientationChangeHandler(executor);
 		
 		return new HandlerRegistration() 
 		{
 			public void removeHandler() 
 			{
 				if(resizeHandlerRegistration != null)
 				{
 					resizeHandlerRegistration.removeHandler();
 				}
 				
 				if(orientationHandler != null)
 				{
 					removeOrientationChangeHandler(orientationHandler);
 				}
 			}
 		};
 	}
 	
 	/**
 	 * @param orientationHandler
 	 */
 	private static native void removeOrientationChangeHandler(JavaScriptObject orientationHandler) /*-{
 
 		var supportsOrientationChange = 'onorientationchange' in $wnd;
 		
 		if (supportsOrientationChange)
 		{
 			$wnd.removeEventListener("orientationchange", orientationHandler);
 		}
 		
 	}-*/;
 	
 	/**
 	 * @param executor
 	 * @return
 	 */
 	private static native JavaScriptObject attachOrientationChangeHandler(BeginEndExecutor executor)/*-{
 	
 		var supportsOrientationChange = 'onorientationchange' in $wnd;
 		
 		if (supportsOrientationChange)
 		{	
 			$wnd.previousOrientation = $wnd.orientation;
 			var checkOrientation = function()
 			{
 			    if($wnd.orientation !== $wnd.previousOrientation)
 			    {
 			        $wnd.previousOrientation = $wnd.orientation;
 		        	executor.@org.cruxframework.crux.core.client.executor.BeginEndExecutor::execute()();
 			    }
 			};
 		
 			$wnd.addEventListener("orientationchange", checkOrientation, false);
 			
 			return checkOrientation;
 		}
 		
 		return null;
 	}-*/;
 
 	
 	/**
 	 * 
 	 */
 	private static void prepareHistoryFrame() 
 	{
 		if (!historyFrameInitialized)
 		{
 			Element body = RootPanel.getBodyElement();
 			IFrameElement historyFrame = DOM.createIFrame().cast();
 			historyFrame.setSrc("javascript:''");
 			historyFrame.setId("__gwt_historyFrame");
 			historyFrame.getStyle().setProperty("position", "absolute");
 			historyFrame.getStyle().setProperty("width", "0");
 			historyFrame.getStyle().setProperty("height", "0");
 			historyFrame.getStyle().setProperty("border", "0");
 			body.appendChild(historyFrame);
 		    History.fireCurrentHistoryState();
 		    historyFrameInitialized = true;
 		}
 	}
 
 }
