 import com.infradna.tool.bridge_method_injector.WithBridgeMethods;
 
 public class Foo {
     @WithBridgeMethods(Object.class)
     public String getMessage() {
        return "foo";
     }
 }
