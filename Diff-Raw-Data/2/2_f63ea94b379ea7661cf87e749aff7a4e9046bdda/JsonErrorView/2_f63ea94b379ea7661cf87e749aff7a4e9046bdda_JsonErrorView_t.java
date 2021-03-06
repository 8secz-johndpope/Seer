 package org.mitre.openid.connect.view;
 
 import java.io.IOException;
 import java.io.Writer;
 import java.util.Map;
 
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.http.HttpStatus;
 import org.springframework.stereotype.Component;
 import org.springframework.validation.BeanPropertyBindingResult;
 import org.springframework.web.servlet.view.AbstractView;
 
 import com.google.gson.ExclusionStrategy;
 import com.google.gson.FieldAttributes;
 import com.google.gson.Gson;
 import com.google.gson.GsonBuilder;
 import com.google.gson.JsonObject;
 
 /**
  * @author aanganes, jricher
  *
  */
@Component("jsonErrorView")
 public class JsonErrorView extends AbstractView {
 
 	private static Logger logger = LoggerFactory.getLogger(JsonEntityView.class);
 
 	private Gson gson = new GsonBuilder()
 	    .setExclusionStrategies(new ExclusionStrategy() {
 	
 	        public boolean shouldSkipField(FieldAttributes f) {
 	
 	            return false;
 	        }
 	
 	        public boolean shouldSkipClass(Class<?> clazz) {
 	            // skip the JPA binding wrapper
 	            if (clazz.equals(BeanPropertyBindingResult.class)) {
 	                return true;
 	            }
 	            return false;
 	        }
 	
 	    })
 	    .serializeNulls()
 	    .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
 	    .create();
 
 	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) {
 
 		response.setContentType("application/json");
 
         
 		HttpStatus code = (HttpStatus) model.get("code");
 		if (code == null) {
 			code = HttpStatus.OK; // default to 200
 		}
 		
 		response.setStatus(code.value());
 		
 		try {
 			
 			Writer out = response.getWriter();
 			
 			String errorMessage = (String) model.get("errorMessage");
 			JsonObject obj = new JsonObject();
 			obj.addProperty("error_message", errorMessage);
 	        gson.toJson(obj, out);
 	        
 		} catch (IOException e) {
 			
 			//TODO: Error Handling
 			logger.error("IOException in JsonErrorView.java: ", e);
 			
 		}
     }
 
 }
