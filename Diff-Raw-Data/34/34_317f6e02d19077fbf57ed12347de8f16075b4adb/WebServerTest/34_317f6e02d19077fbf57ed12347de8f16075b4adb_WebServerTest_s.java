 /**
  * Copyright (C) 2013 all@code-story.net
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *         http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License
  */
 package net.codestory.http;
 
 import static com.jayway.restassured.RestAssured.*;
 import static org.hamcrest.Matchers.*;
 
 import java.io.*;
 import java.nio.charset.*;
 
 import net.codestory.http.annotations.*;
 import net.codestory.http.misc.*;
 import net.codestory.http.templating.*;
 
 import org.junit.*;
 
 import com.jayway.restassured.specification.*;
 
 public class WebServerTest {
   @ClassRule
   public static WebServerRule server = new WebServerRule() {
     @Override
     protected boolean devMode() {
       return false;
     }
   };
 
   @Before
   public void resetWebServer() {
     server.reset();
   }
 
   @Test
   public void not_found() {
    expect().statusCode(404).when().get("/");
   }
 
   @Test
   public void content_types() {
     server.configure(routes -> {
       routes.get("/index", () -> "Hello");
       routes.get("/raw", () -> "RAW DATA".getBytes(StandardCharsets.UTF_8));
       routes.get("/json", () -> new Person("NAME", 42));
       routes.get("/text", () -> new Payload("text/plain", "TEXT"));
     });
 
     expect().body(equalTo("Hello")).contentType("text/html").when().get("/index");
     expect().body(equalTo("RAW DATA")).contentType("application/octet-stream").when().get("/raw");
     expect().body("name", equalTo("NAME")).body("age", equalTo(42)).contentType("application/json").when().get("/json");
     expect().body(equalTo("TEXT")).contentType("text/plain").when().get("/text");
   }
 
   @Test
   public void request_params() {
     server.configure(routes -> {
       routes.get("/hello/:name", (name) -> "Hello " + name);
       routes.get("/other/:name", (name) -> "Other " + name);
       routes.get("/say/:what/how/:loud", (what, loud) -> what + " " + loud);
       routes.get("/:one/:two/:three", (one, two, three) -> one + two + three);
     });
 
     expect().body(equalTo("Hello Dave")).when().get("/hello/Dave");
     expect().body(equalTo("Hello Bob")).when().get("/hello/Bob");
     expect().body(equalTo("Hello John Doe")).when().get("/hello/John Doe");
     expect().body(equalTo("Other Joe")).when().get("/other/Joe");
     expect().body(equalTo("HI LOUD")).when().get("/say/HI/how/LOUD");
   }
 
   @Test
   public void static_content_from_classpath() {
     server.configure(routes -> routes.staticDir("classpath:web"));
 
     expect().content(containsString("Hello From a File")).contentType("text/html").when().get("/index.html");
     expect().content(containsString("Hello From a File")).contentType("text/html").when().get("/");
     expect().content(containsString("TEST")).contentType("text/html").when().get("/test.html");
     expect().content(containsString("TEST")).contentType("text/html").when().get("/test");
     expect().content(containsString("console.log('Hello');")).contentType("application/javascript").when().get("/js/script.js");
     expect().content(containsString("console.log('Hello');")).contentType("application/javascript").when().get("/js/script.coffee");
     expect().content(containsString("* {}")).contentType("text/css").when().get("/assets/style.css");
     expect().content(containsString("body h1 {\n  color: red;\n}\n")).contentType("text/css").when().get("/assets/style.less");
     expect().statusCode(404).when().get("/../private.txt");
     expect().statusCode(404).when().get("/unknown");
   }
 
   @Test
   public void static_content_from_file() {
     String web = ClassLoader.getSystemResource("web").getFile();
 
     server.configure(routes -> routes.staticDir(web));
 
     expect().content(containsString("Hello From a File")).contentType("text/html").when().get("/index.html");
   }
 
   @Test
   public void annotated_resources() {
     server.configure(routes -> routes.add(new Object() {
       @Get("/hello")
       public String say_hello() {
         return "Hello World";
       }
 
       @Get("/bye/:whom")
       public String say_bye_to_(String whom) {
         return "Good Bye " + whom;
       }
 
       @Get("/add/:left/:right")
       public String say_bye_to_(int left, int right) {
         return Integer.toString(left + right);
       }
     }));
 
     expect().content(equalTo("Hello World")).when().get("/hello");
     expect().content(equalTo("Good Bye Bob")).when().get("/bye/Bob");
     expect().content(equalTo("42")).when().get("/add/22/20");
   }
 
   @Test
   public void annotated_resources_with_prefix() {
     server.configure(routes -> routes.add("/say", new Object() {
       @Get("/hello")
       public String say_hello() {
         return "Hello World";
       }
     }));
 
     expect().content(equalTo("Hello World")).when().get("/say/hello");
   }
 

   @Test
   public void ignore_query_params() {
     server.configure(routes -> routes.get("/index", () -> "Hello"));
 
     expect().content(equalTo("Hello")).when().get("/index?query=value");
   }
 
   @Test
   public void streams() {
     server.configure(routes -> routes.get("/", () -> new Payload("text/html", new ByteArrayInputStream("Hello".getBytes()))));
 
     expect().content(equalTo("Hello")).contentType("text/html").when().get("/");
   }
 
   @Test
   public void templates() {
     server.configure(routes -> {
       routes.staticDir("classpath:web");
       routes.get("/hello/:name", (name) -> new Template("classpath:web/1variable.txt").render("name", name));
     });
 
     expect().content(containsString("<div>_PREFIX_TEXT_SUFFIX_</div>")).when().get("/pageYaml");
     expect().content(equalTo("Hello Joe")).when().get("/hello/Joe");
   }
 
   @Test
   public void priority_to_route() {
     server.configure(routes -> {
       routes.staticDir("classpath:web");
          routes.get("/", () -> "PRIORITY");
     });
 
     expect().content(equalTo("PRIORITY")).when().get("/");
   }
 
   @Test
   public void redirect() {
     server.configure(routes -> {
       routes.get("/index", () -> Payload.seeOther("/login"));
       routes.get("/login", () -> "LOGIN");
     });
 
     expect().content(equalTo("LOGIN")).when().get("/index");
   }
 
   @Test
   public void filter() {
     server.configure(routes -> {
       routes.get("/", () -> "NOT FILTERED");
       routes.get("/other", () -> "OTHER");
       routes.filter((uri, exchange) -> {
         if ("/".equals(uri)) {
           exchange.sendResponseHeaders(200, 8);
           exchange.getResponseBody().write("FILTERED".getBytes());
           return true;
         }
         return false;
       });
     });
 
     expect().content(equalTo("FILTERED")).when().get("/");
     expect().content(equalTo("OTHER")).when().get("/other");
   }
 
   private ResponseSpecification expect() {
     return given().port(server.port()).expect();
   }
 
   static class Person {
     final String name;
     final int age;
 
     Person(String name, int age) {
       this.name = name;
       this.age = age;
     }
   }
 }
