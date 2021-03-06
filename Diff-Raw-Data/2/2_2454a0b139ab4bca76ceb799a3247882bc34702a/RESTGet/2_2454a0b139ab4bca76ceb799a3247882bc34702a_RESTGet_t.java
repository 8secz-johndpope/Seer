 package org.basex.api.rest;
 
 import static javax.servlet.http.HttpServletResponse.*;
 import static org.basex.api.rest.RESTText.*;
 
 import java.io.IOException;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Map.Entry;
 
 import org.basex.io.serial.SerializerProp;
 import org.basex.util.Token;
 import org.basex.util.TokenBuilder;
 
 /**
  * This class processes GET requests sent to the REST server.
  *
  * @author BaseX Team 2005-11, BSD License
  * @author Christian Gruen
  */
 final class RESTGet extends RESTCode {
   @Override
   void run(final RESTContext ctx) throws RESTException, IOException {
     final Map<String, String[]> vars = new HashMap<String, String[]>();
 
     // handle query parameters
     String operation = null;
     String input = null;
     byte[] item = null;
 
     final TokenBuilder ser = new TokenBuilder();
     final Map<?, ?> map = ctx.req.getParameterMap();
     final SerializerProp sp = new SerializerProp();
     for(final Entry<?, ?> s : map.entrySet()) {
       final String key = s.getKey().toString();
       final String[] vals = s.getValue() instanceof String[] ?
           (String[]) s.getValue() : new String[] { s.getValue().toString() };
       final String val = vals[0];
 
       if(key.equals(COMMAND) || key.equals(QUERY) || key.equals(RUN)) {
         if(operation != null || vals.length > 1)
           throw new RESTException(SC_BAD_REQUEST, ERR_ONLYONE);
         operation = key;
         input = val;
       } else if(key.startsWith("$")) {
         // external variables
         vars.put(key, new String[] { val });
       } else if(key.equals(WRAP)) {
         // wrapping flag
         wrap(val, ctx);
       } else if(key.equals(CONTEXT)) {
         // wrapping flag
         item = Token.token(val);
       } else {
         // serialization parameters
         if(sp.get(key) != null) {
           for(final String v : vals) ser.add(key).add('=').add(v).add(',');
         } else {
           throw new RESTException(SC_BAD_REQUEST, ERR_PARAM + key);
         }
       }
     }
     ctx.serialization = ser.toString();
 
     final RESTCode code;
     if(operation == null) {
       code = new RESTList();
     } else if(operation.equals(QUERY)) {
       code = new RESTQuery(input, vars, item);
     } else if(operation.equals(RUN)) {
      code = new RESTRun(input, vars, item);
     } else {
       code = new RESTCommand(input);
     }
     code.run(ctx);
   }
 }
