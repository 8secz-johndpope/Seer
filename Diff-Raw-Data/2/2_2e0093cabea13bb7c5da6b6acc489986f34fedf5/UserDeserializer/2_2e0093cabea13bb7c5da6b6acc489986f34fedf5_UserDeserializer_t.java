 package org.osiam.resources.helper;
 
 import java.io.IOException;
 
 import org.osiam.resources.scim.Constants;
 import org.osiam.resources.scim.Extension;
 import org.osiam.resources.scim.User;
 
 import com.fasterxml.jackson.core.JsonLocation;
 import com.fasterxml.jackson.core.JsonParseException;
 import com.fasterxml.jackson.core.JsonParser;
 import com.fasterxml.jackson.core.Version;
 import com.fasterxml.jackson.databind.DeserializationContext;
 import com.fasterxml.jackson.databind.DeserializationFeature;
 import com.fasterxml.jackson.databind.JsonMappingException;
 import com.fasterxml.jackson.databind.JsonNode;
 import com.fasterxml.jackson.databind.ObjectMapper;
 import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
 import com.fasterxml.jackson.databind.module.SimpleModule;
 
 public class UserDeserializer extends StdDeserializer<User> {
 
 	private static final long serialVersionUID = 1L;
 
 	public UserDeserializer(Class<?> valueClass) {
         super(valueClass);
     }
 
     @Override
     public User deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
         JsonNode rootNode = jp.readValueAsTree();
         ObjectMapper mapper = new ObjectMapper();
         mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
 
         ExtensionDeserializer deserializer = new ExtensionDeserializer(Extension.class);
        SimpleModule testModule = new SimpleModule("ExtensionDeserializerModule", Version.unknownVersion())
                 .addDeserializer(Extension.class, deserializer);
         mapper.registerModule(testModule);
 
         User user = mapper.readValue(rootNode.toString(), User.class);
         if (user.getSchemas() == null) {
             throw new JsonMappingException("Required field Schema is missing");
         }
         if (user.getSchemas().size() == 1) {
             return user;
         }
 
         User.Builder builder = new User.Builder(user);
 
         for (String urn : user.getSchemas()) {
             if (urn.equals(Constants.CORE_SCHEMA)) {
                 continue;
             }
 
             JsonNode extensionNode = rootNode.get(urn);
             if (extensionNode == null) {
                 throw new JsonParseException("Registered extension not present.", JsonLocation.NA);
             }
 
             deserializer.setUrn(urn);
             Extension extension = mapper.readValue(extensionNode.toString(), Extension.class);
             builder.addExtension(urn, extension);
 
         }
         return builder.build();
     }
 
 }
