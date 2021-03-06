 // Copyright (c) 2009 The Chromium Authors. All rights reserved.
 // Use of this source code is governed by a BSD-style license that can be
 // found in the LICENSE file.
 
 package org.chromium.sdk.internal;
 
 import static org.junit.Assert.assertNull;
 
 import java.io.IOException;
 import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
 
 import org.chromium.sdk.BrowserFactory;
 import org.chromium.sdk.BrowserTab;
 import org.chromium.sdk.DebugContext;
 import org.chromium.sdk.DebugEventListener;
 import org.chromium.sdk.UnsupportedVersionException;
 import org.chromium.sdk.DebugContext.ContinueCallback;
 import org.chromium.sdk.internal.transport.Connection;
 import org.junit.After;
 import org.junit.Before;
 
 /**
  * A base class for all tests that require an attachment to a browser tab.
  */
 public abstract class AbstractAttachedTest<T extends Connection>
     implements DebugEventListener {
 
   protected FixtureChromeStub messageResponder;
 
   protected BrowserImpl browser;
 
   protected BrowserTabImpl browserTab;
 
   protected DebugContextImpl suspendContext;
 
   protected Runnable suspendCallback;
 
   protected Runnable closedCallback;
 
   protected Runnable navigatedCallback;
 
   protected String newTabUrl;
 
   protected boolean isDisconnected = false;
 
   protected T connection;
 
   @Before
   public void setUpBefore() throws Exception {
     this.newTabUrl = "";
     this.messageResponder = new FixtureChromeStub();
     connection = createConnection();
     attachToBrowserTab();
   }
 
   @After
   public void tearDownAfter() {
     browser.disconnect();
   }
 
   protected void attachToBrowserTab() throws IOException, UnsupportedVersionException {
     browser = (BrowserImpl) ((BrowserFactoryImpl) BrowserFactory.getInstance()).create(connection);
     browser.connect();
     BrowserTab[] tabs = browser.getTabs();
     browserTab = (BrowserTabImpl) tabs[0];
     browserTab.attach(this);
   }
 
   protected abstract T createConnection();
 
   protected T getConnection() {
     return connection;
   }
 
   @After
   public void tearDown() {
     suspendContext = null;
   }
 
   public void closed() {
     this.newTabUrl = null;
     if (closedCallback != null) {
       closedCallback.run();
     }
   }
 
   public void disconnected() {
     this.isDisconnected = true;
   }
 
   public void navigated(String newUrl) {
     this.newTabUrl = newUrl;
     if (navigatedCallback != null) {
       navigatedCallback.run();
     }
   }
 
   public void resumed() {
     this.suspendContext = null;
   }
 
   public void suspended(DebugContext context) {
     this.suspendContext = (DebugContextImpl) context;
     if (suspendCallback != null) {
       suspendCallback.run();
     }
   }
 
   protected void waitForSuspend() throws InterruptedException {
     final CountDownLatch latch = new CountDownLatch(1);
     suspendCallback = new Runnable() {
       public void run() {
         latch.countDown();
       }
     };
     latch.await();
   }
 
   protected void resume() throws Exception {
     final CountDownLatch latch = new CountDownLatch(1);
     final String[] failure = new String[1];
     suspendContext.continueVm(null, 0, new ContinueCallback() {
       public void failure(String errorMessage) {
         failure[0] = errorMessage == null ? "" : errorMessage;
         latch.countDown();
       }
 
       public void success() {
         latch.countDown();
       }
     });
    latch.await(100, TimeUnit.MILLISECONDS);
     assertNull("Failure on continue: " + failure[0], failure[0]);
     assertNull(suspendContext);
   }
 
 }
