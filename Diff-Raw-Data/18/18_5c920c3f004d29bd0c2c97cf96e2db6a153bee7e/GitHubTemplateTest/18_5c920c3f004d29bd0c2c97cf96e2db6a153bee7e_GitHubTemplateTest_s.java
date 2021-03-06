 /*
  * Copyright 2011 the original author or authors.
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
 package org.springframework.social.github;
 
 import static org.mockito.Mockito.*;
 
 import org.junit.Before;
 import org.junit.Test;
 import org.springframework.web.client.RestOperations;
 
 /**
  * @author Craig Walls
  */
 public class GitHubTemplateTest {
	private GitHubTemplate gowalla;
 	private RestOperations restOperations;
 
 	@Before
 	public void setup() {
		gowalla = new GitHubTemplate("ACCESS_TOKEN");
 		restOperations = mock(RestOperations.class);
		gowalla.restOperations = restOperations;
 	}
 
 	@Test
 	public void getProfileId() {
		// assertEquals("12345", gowalla.getProfileId());
 	}
 }
