 /*
  * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
  *   http://www.griddynamics.com
  *
  *   This library is free software; you can redistribute it and/or modify it under the terms of
  *   the GNU Lesser General Public License as published by the Free Software Foundation; either
  *   version 2.1 of the License, or any later version.
  *
  *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  *   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  *   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  *   DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
  *   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
  *   DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
  *   SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
  *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
  *   OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
  *   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  *
  *   @Project:     Genesis
  *   @Description: E-mail notifications plugin
  */
 package com.griddynamics.genesis.notification.template;
 
 import org.junit.Before;
 import org.junit.Test;
 
 import java.io.File;
 import java.util.HashMap;
 import java.util.Map;
 
 import static org.junit.Assert.assertEquals;
 
 public class StringTemplateEngineTest {
 
   private TemplateEngine templateEngine;
 
   @Before
   public void setUp() throws Exception {
    templateEngine = new StringTemplateEngine("template");
   }
 
   @Test
   public void testRenderText() throws Exception {
     Map<String, String> params = new HashMap<String, String>();
    params.put("message1", "test");
    params.put("message2", "12345");
    params.put("message3", "67890");
     String result = templateEngine.renderText("email", params);
    assertEquals("test12345", result);
   }
 }
