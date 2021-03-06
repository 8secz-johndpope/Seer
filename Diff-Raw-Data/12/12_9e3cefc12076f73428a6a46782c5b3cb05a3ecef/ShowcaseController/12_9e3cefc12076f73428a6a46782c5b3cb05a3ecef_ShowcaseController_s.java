 /*
  * Copyright 2010 the original author or authors.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.springframework.social.samples.facebook;
 
 import javax.inject.Inject;
 
 import org.springframework.social.connect.providers.FacebookServiceProvider;
 import org.springframework.social.facebook.FacebookProfile;
 import org.springframework.stereotype.Controller;
 import org.springframework.ui.Model;
 import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.bind.annotation.RequestMethod;
 
 @Controller
 public class ShowcaseController {
 	private final FacebookServiceProvider facebookProvider;
 
 	@Inject
 	public ShowcaseController(FacebookServiceProvider FacebookServiceProvider) {
 		this.facebookProvider = FacebookServiceProvider;
 	}
 
 	@RequestMapping(value = "/", method = RequestMethod.GET)
 	public String home(Model model) {
 		if (facebookProvider.isConnected(1)) {
 			FacebookProfile userProfile = facebookProvider.getServiceOperations(1).getUserProfile();
 			model.addAttribute("fbUser", userProfile);
 			return "home";
 		}
 		return "redirect:/connect/facebook";
 	}
 }
