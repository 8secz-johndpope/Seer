 /***************************************
  * Copyright (c) 2008
  * Bjoern Wagner
  *
  * Permission is hereby granted, free of charge, to any person obtaining a
  * copy of this software and associated documentation files (the "Software"),
  * to deal in the Software without restriction, including without limitation
  * the rights to use, copy, modify, merge, publish, distribute, sublicense,
  * and/or sell copies of the Software, and to permit persons to whom the
  * Software is furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in
  * all copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
  * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
  * DEALINGS IN THE SOFTWARE.
 ****************************************/
 
 package org.b3mn.poem.handler;
 
 import java.io.IOException;
 
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
 import org.b3mn.poem.Identity;
 import org.b3mn.poem.Representation;
 import org.b3mn.poem.util.AccessRight;
 import org.b3mn.poem.util.HandlerWithModelContext;
 import org.b3mn.poem.util.RestrictAccess;
 
 @HandlerWithModelContext(uri="/self", filterBrowser=true)
 public class ModelHandler extends  HandlerBase {
 
 	@Override
     public void doGet(HttpServletRequest request, HttpServletResponse response, Identity subject, Identity object) throws IOException {
 
 		Representation representation = object.read();
 		
 		response.setContentType("application/xhtml+xml");
 		response.getWriter().println(this.getOryxModel(representation.getTitle(), 
 				representation.getContent(), this.getLanguageCode(request), 
 				this.getCountryCode(request)));
 		response.setStatus(200);
 	}
 	
 	@Override
 	@RestrictAccess(AccessRight.WRITE)
     public void doPost(HttpServletRequest request, HttpServletResponse response, Identity subject, Identity object) throws IOException {
 		// TODO: add some error handling
		Representation.update(object.getId(), null, null, request.getParameter("data"), request.getParameter("svg"));
 		response.setStatus(200);
 	}
 
 	@Override
     public void doPut(HttpServletRequest request, HttpServletResponse response, Identity subject, Identity object) throws IOException {
 		response.setStatus(200);
 	}
 
 	@Override
 	@RestrictAccess(AccessRight.WRITE)
     public void doDelete(HttpServletRequest request, HttpServletResponse response, Identity subject, Identity object) throws IOException {
 		object.delete();
 		response.setStatus(200);
 	}
 
 }
