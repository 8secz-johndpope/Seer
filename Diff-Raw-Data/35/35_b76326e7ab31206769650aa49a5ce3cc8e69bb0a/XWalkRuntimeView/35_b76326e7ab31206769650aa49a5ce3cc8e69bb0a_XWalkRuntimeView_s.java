 // Copyright (c) 2013 Intel Corporation. All rights reserved.
 // Use of this source code is governed by a BSD-style license that can be
 // found in the LICENSE file.
 
 package org.xwalk.runtime;
 
 import android.content.Context;
 import android.content.Intent;
 import android.util.AttributeSet;
 import android.widget.FrameLayout;
 
 /**
  * This class is to provide public APIs which are called by web application
  * APKs. Since the runtime is shared as a library APK, web application APKs
  * have to call them via class loader but not direct API calling.
  *
  * A web application APK should create its Activity and set this view as
  * its content view.
  */
 // Implementation notes.
 // Please be careful to change any public APIs for the backward compatibility
 // is very important to us. Don't change any of them without permisson.
 public class XWalkRuntimeView extends FrameLayout {
     // The actual implementation to hide the internals to API users.
     private XWalkRuntimeViewProvider mProvider;
 
     /**
      * Contructs a XWalkRuntimeView with a Context object.
      *
      * @param context a Context used by runtime from web app APK.
      */
     public XWalkRuntimeView(Context context, AttributeSet attrs) {
         super(context, attrs);
         mProvider = new XWalkRuntimeViewProvider(context, this);
     }
 
     /**
      * Get the version information of current runtime library.
      *
      * @return the string containing the version information.
      */
     public static String getVersion() {
         // TODO(yongsheng): Implement the real version information.
         // May include our customized information by containing chromium
         // version info.
         return "0.1";
     }
 
     /**
      * Load a web application through the entry url. It may be
      * a file from assets or a url from network.
      *
      * @param url the url of loaded html resource.
      */
     public void loadAppFromUrl(String url) {
         mProvider.loadAppFromUrl(url);
     }
 
     /**
      * Load a web application through the url of the manifest file.
      * The manifest file typically is placed in android assets. Now it is
      * compliant to W3C SysApps spec.
      *
      * @param manifestUrl the url of the manifest file
      */
     public void loadAppFromManifest(String manifestUrl) {
         mProvider.loadAppFromManifest(manifestUrl);
     }
 
     /**
      * Tell runtime that the application is on creating. This can make runtime
      * be aware of application life cycle.
      */
     public void onCreate() {
         mProvider.onCreate();
     }
 
     /**
      * Tell runtime that the application is on resuming. This can make runtime
      * be aware of application life cycle.
      */
     public void onResume() {
         mProvider.onResume();
     }
 
     /**
      * Tell runtime that the application is on pausing. This can make runtime
      * be aware of application life cycle.
      */
     public void onPause() {
         mProvider.onPause();
     }
 
     /**
      * Tell runtime that the application is on destroying. This can make runtime
      * be aware of application life cycle.
      */
     public void onDestroy() {
         mProvider.onDestroy();
     }
 
     /**
      * Tell runtime that one activity exists so that it can know the result code
      * of the exit code.
      *
      * @param requestCode the request code to identify where the result is from
      * @param resultCode the result code of the activity
      * @param data the data to contain the result data
      */
     public void onActivityResult(int requestCode, int resultCode, Intent data) {
         mProvider.onActivityResult(requestCode, resultCode, data);
     }
 
     /**
      * Enable remote debugging for the loaded web application. The caller
      * can set the url of debugging url. Besides, the socket name for remote
      * debugging has to be unique so typically the string can be appended
      * with the package name of the application.
      *
      * @param frontEndUrl the url of debugging url. If it's empty, then a
      *                    default url will be used.
      * @param socketName the unique socket name for setting up socket for
     *                   remote debugging.
      */
    public void enableRemoteDebugging(String frontEndUrl, String socketName) {
         // TODO(yongsheng): Figure out which parameters are needed once we
         // have a conclusion.
        mProvider.enableRemoteDebugging(frontEndUrl,socketName);
     }
 
     /**
      * Disable remote debugging so runtime can close related stuff for
      * this feature.
      */
     public void disableRemoteDebugging() {
         mProvider.disableRemoteDebugging();
     }
 }
