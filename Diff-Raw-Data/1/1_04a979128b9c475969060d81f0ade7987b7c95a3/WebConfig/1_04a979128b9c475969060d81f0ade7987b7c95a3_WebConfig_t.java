 package au.com.pingmate;
 
 import org.springframework.context.annotation.Configuration;
 import org.springframework.web.servlet.config.annotation.*;
 
 @Configuration
 @EnableWebMvc
 public class WebConfig extends WebMvcConfigurerAdapter {
     @Override
     public void addResourceHandlers(ResourceHandlerRegistry registry) {
         registry.addResourceHandler("/**").addResourceLocations("classpath:/assets/");
     }
 
     @Override
     public void addViewControllers(ViewControllerRegistry registry) {
         registry.addViewController("/").setViewName("redirect:index.html");
     }
 }
