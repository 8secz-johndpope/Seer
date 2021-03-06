 package ceylon.language;
 
 import com.redhat.ceylon.compiler.java.metadata.Ceylon;
 import com.redhat.ceylon.compiler.java.metadata.Ignore;
 import com.redhat.ceylon.compiler.java.metadata.Method;
 import com.redhat.ceylon.compiler.java.metadata.Name;
 import com.redhat.ceylon.compiler.java.metadata.Sequenced;
 import com.redhat.ceylon.compiler.java.metadata.TypeInfo;
 import com.redhat.ceylon.compiler.java.metadata.TypeParameter;
 import com.redhat.ceylon.compiler.java.metadata.TypeParameters;
 
 @Ceylon(major = 1)
 @Method
 public final class first {
     
     private first() {
     }
     
    @TypeInfo("Element|ceylon.language.Nothing")
     @TypeParameters(@TypeParameter(value="Element"))
     public static <Element> Element first(@Name("elements") 
     @Sequenced @TypeInfo("ceylon.language.Iterable<Element>")
     final Iterable<? extends Element> elements) {
         java.lang.Object first = elements.getIterator().next();
         if (first instanceof Finished) {
             return null;
         }
         else {
             return (Element) first;
         }
     }
     @Ignore
     public static <Element> Element first() {
         return null;
     }
 }
