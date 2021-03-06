 /*
  * Copyright (c) 2006-2015 DMDirc Developers
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in
  * all copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  */
 
 package com.dmdirc.addons.ui_swing.components;
 
 import java.util.concurrent.ExecutionException;
 
 import javax.swing.SwingWorker;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 /**
  * Logging swing worker.
  *
  * @param <T> Return type for the doneInBackground method
  * @param <V> Return type for the progress and publish
  */
 public abstract class LoggingSwingWorker<T, V> extends SwingWorker<T, V> {
 
     private static final Logger LOG = LoggerFactory.getLogger(LoggingSwingWorker.class);
 
     @Override
     protected void done() {
         if (isCancelled()) {
             return;
         }
         try {
             get();
         } catch (InterruptedException ex) {
             //Ignore
         } catch (ExecutionException ex) {
            LOG.warn(ex.getMessage(), ex);
         }
     }
 
 }
