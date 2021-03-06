 package ed.doc;
 
 import java.io.*;
 import java.util.Calendar;
 
 import ed.js.*;
 import ed.js.func.*;
 import ed.js.engine.*;
 import ed.db.*;
 import static ed.js.JSInternalFunctions.*;
 import ed.js.engine.Scope;
 import ed.appserver.AppContext;
 import ed.io.SysExec;
 
 /** Documentation generator for JavaScript and Java
  * @expose
  */
 public class Generate {
 
     /** Takes objects from the db and makes them into HTML pages.  Uses a default output directory.
      * @param A jsoned obj from the db
      */
     public static void toHTML(String objStr) {
         toHTML(objStr, "../../www/html/doc/");
     }
 
     /** Takes objects from the db and makes them into HTML pages.
      * @param A jsoned obj from the db
      * @param The output dir
      */
     public static void toHTML(String objStr, String path) {
         try {
             Scope s = Scope.getThreadLocal();
             Object dbo = s.get("__instance__");
             if(! (dbo instanceof AppContext)) throw new RuntimeException("your appserver is having an identity crisis");
             String instanceName = ((AppContext)dbo).getName();
 
            File check = new File("../"+instanceName+"/"+path+"DOC_DIR");
            if(check.exists())
                removeOldDocs("../"+instanceName+"/"+path);

            System.out.println("instance: "+instanceName+" path: "+path);
            SysExec.Result r = SysExec.exec("java -jar jsrun.jar app/run.js -d=../../"+instanceName+"/"+path+" -t=templates/jsdoc2", null, new File("../core-modules/docgen/"), objStr);
            // System.out.println(r.getOut());

            check.createNewFile();
         }
         catch(Exception e) {
             e.printStackTrace();
         }
 
     }
 
 
     /** Takes source files/dirs, generates jsdoc from them, stores resulting js obj in the db
      * @param Path to the file or folder to be documented
      */
     public static void JSToDb(String path) throws IOException {
         File f = new File(path);
         if(!f.exists()) {
             System.out.println("File does not exist: "+path);
             return;
         }
         if(f.isDirectory()) {
             File farray[] = f.listFiles();
             for(int i=0; i<farray.length; i++) {
                 JSToDb(farray[i].getCanonicalPath());
             }
         }
         else {
             try {
                SysExec.Result r = SysExec.exec("java -jar jsrun.jar app/run.js -r -t=templates/json ../"+path, null, new File("../core-modules/docgen/"), "");
 
                 JSObjectBase ss = new JSObjectBase();
                 ss.set("symbolSet", r.getOut());
                 JSObjectBase obj = new JSObjectBase();
                 obj.set("ts", Calendar.getInstance().getTime().toString());
                 obj.set("_index", ss);
 
                 Scope s = Scope.getThreadLocal();
                 Object dbo = s.get("db");
                 if(! (dbo instanceof DBApiLayer)) throw new RuntimeException("your database is having an identity crisis");
 
                 DBApiLayer db = (DBApiLayer)dbo;
                 DBCollection collection = db.getCollection("doc");
                 collection.save(obj);
             }
             catch(Exception e) {
                 e.printStackTrace();
             }
         }
     }
 
     /** Generate a js obj from javadoc
      * @param Path to file or folder to be documented
      */
     public static void JavadocArgHelper(String arg) throws IOException {
         File f = new File(arg);
         if(!f.exists()) return;
         if(f.isDirectory()) {
             File farray[] = f.listFiles();
             for(int i=0; i<farray.length; i++) {
                 JavadocArgHelper(farray[i].getCanonicalPath());
             }
         }
         else {
             com.sun.tools.javadoc.Main.execute(new String[]{"-doclet", "JavadocToDB", "-docletpath", "./", arg } );
         }
     }

    public static void removeOldDocs(String path) throws IOException {
        File f = new File(path);
        if(!f.exists()) return;
        if(f.isDirectory()) {
            File farray[] = f.listFiles();
            for(int i=0; i<farray.length; i++) {
                removeOldDocs(farray[i].getCanonicalPath());
            }
        }
        else {
            f.delete();
        }
    }
 }
