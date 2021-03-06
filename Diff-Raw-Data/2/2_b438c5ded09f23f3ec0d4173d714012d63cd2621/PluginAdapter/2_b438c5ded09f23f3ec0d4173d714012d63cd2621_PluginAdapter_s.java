 /*
 *    Copyright 2009 Vanessa Williams
  *
  *    Licensed under the Apache License, Version 2.0 (the "License");
  *    you may not use this file except in compliance with the License.
  *    You may obtain a copy of the License at
  *
  *        http://www.apache.org/licenses/LICENSE-2.0
  *
  *    Unless required by applicable law or agreed to in writing, software
  *    distributed under the License is distributed on an "AS IS" BASIS,
  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *    See the License for the specific language governing permissions and
  *    limitations under the License.
  */
 
 /*
  * PluginAdapter.java
  * Created on Mar 12, 2010
  */
 
 package savant.plugin;
 
 import savant.controller.*;
 
 public class PluginAdapter {
 
     public BookmarkController getBookmarkController(){
         return BookmarkController.getInstance();
     }
 
     public FrameController getFrameController() {
         return FrameController.getInstance();
     }
 
     public RangeController getRangeController() {
         return RangeController.getInstance();
     }
 
     public TrackController getTrackController() {
         return TrackController.getInstance();
     }
 
     public ViewTrackController getViewTrackController() {
         return ViewTrackController.getInstance();
     }
 
 
 }
