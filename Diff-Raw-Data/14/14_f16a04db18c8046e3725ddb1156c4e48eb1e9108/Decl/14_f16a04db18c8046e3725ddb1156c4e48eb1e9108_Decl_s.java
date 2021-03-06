 package xtc.oop;
 
 import java.io.File;
 import java.io.IOException;
 import java.io.Reader;
 import java.io.FileReader;
 
 import java.util.*;
 
 import xtc.parser.ParseException;
 import xtc.parser.Result;
 
 import xtc.tree.GNode;
 import xtc.tree.Node;
 import xtc.tree.Visitor;
 import xtc.tree.Location;
 
 import xtc.tree.Printer;
 
 import xtc.lang.JavaFiveParser;
 
 //our imports
 import xtc.oop.helper.Bubble;
 import xtc.oop.helper.Mubble;
 import xtc.oop.helper.PNode;
 import java.util.regex.*;
 import java.io.FileWriter;
 import java.io.BufferedWriter;
 
 /** A Java file Scope analyzer
  * For each static scope, prints
  * 		Enter scope at <filename>:<line>:<column>
  * upon entering the scope.
  *
  * @author Calvin Hawkes
  * @version 1.0
  */
 
 public class Decl extends xtc.util.Tool
 {
 
 
     public static String findFile(String query) {
 
         String sep = System.getProperty("file.separator");
         String cp = System.getProperty("java.class.path");
 	//Hardcoded as the working directory, otherwise real classpath
         cp = ".";
 
         query = query.replace(".",sep).concat(".java");
 	//System.out.println("+++++"+query);
         return findFile(cp, query);
     }
 
     //can return File if necessary
     public static String findFile(String cp, String query) {
         String sep = System.getProperty("file.separator");
 	File f = new File(cp);
 	File [] files = f.listFiles();
 	for(int i = 0; i < files.length; i++) {
 	    //System.out.println(sep+(cp.equals(".") ? "\\\\" : "")+cp+sep);
 	    //////////////////////////////////////
 	    //Hardcoding that sep is / and cp is .
 	    //////////////////////////////////////
 	    //System.out.println(query);
 	    if(files[i].isDirectory()) {
 		String a = findFile(files[i].getAbsolutePath(), query);
 		if(!a.equals(""))
 		    return a;
 	    }
 	    else if(files[i].getAbsolutePath().replaceAll("/\\./",sep).endsWith(query))
 		return files[i].getAbsolutePath();
 	}
 	return "";
     }
 
 
 
     public Decl()
     {
         // Nothing to do.
     }
 
     public String getName()
     {
         return "Java to C++ translator";
     }
 
     public String getCopy()
     {
 
         return "Ninja assassins: dk, calvin, Andrew*2";
     }
 
     public void init()
     {
         super.init();
 
         /*
         runtime.
             bool("printClassH", "printClassH", false, "print the .h that is interpreted from given AST").
             bool("printClassCC", "printClassCC", false, "Print Java AST.");
         */
     }
 
     public Node parse(Reader in, File file) throws IOException, ParseException
     {
         JavaFiveParser parser = new JavaFiveParser(in, file.toString(), (int)file.length());
         Result result = parser.pCompilationUnit(0);
 
         return (Node)parser.value(result);
     }
 
     public void process(Node node)
     {
         //construct inheritance tree!
         new Visitor()
         {//{{{
 
             //assemble the forces
             ArrayList<String> dataFields = new ArrayList<String>();
             ArrayList<String> methods = new ArrayList<String>();
             ArrayList<String> constructors = new ArrayList<String>();
             ArrayList<String> children = new ArrayList<String>();
             String name;
             Bubble parent;
             String className = "";
             String tempString = "";
             String packageName = "";
             int counter = 0;
 
             public void visitFieldDeclaration(GNode n){
                 dataFields.add("");
                 visit(n);
                 //dataField.add("\n");
             }
 
             public void visitDimensions(GNode n) {
                 visit(n);
                 Node parent0 = (Node)n.getProperty("parent0");
                 Node parent1 = (Node)n.getProperty("parent1");
                 if ((parent1.getName().equals("MethodDeclaration")) &&
                         (parent0.getName().equals("Type")))
                 {
                     String dims = getDimensions(n);
                     methods.set(methods.size()-1,methods.get(methods.size()-1)+" "+dims);
                 }
             }
 
             public String getDimensions(GNode n)
             {
                 String toReturn = "";
                 //runtime.console().pln("PARENT NODE: " + ((Node)(n.getProperty("parent"))).getName()).flush();
                 //runtime.console().pln("NODE: " + n.getName()).flush();
                 for(Object o : n)
                 {
                     if(o != null)
                     {
                         //runtime.console().pln("CHILD: " + o.toString()).flush();
                         if(o instanceof String){
                             //System.out.println(o.toString());
                             toReturn +=  o.toString();
                         }
                         else
                             toReturn +=  getDimensions((GNode)o);
                     }
                 }
                 return toReturn;
             }
 
 
             public void visitModifiers(GNode n){
                 visit(n);
             }
 
             public void visitMethodDeclaration(GNode n){
                 methods.add("");
                 visit(n);
                 String name = n.getString(3);
                 if (name == "static")
                     name = name + " " + n.getString(4);
                 methods.set(methods.size()-1,methods.get(methods.size()-1)+" "+name);
             }
 
             public void visitVoidType(GNode n){
                 visit(n);
                 Node parent1 = (Node)n.getProperty("parent1");
                 Node parent2 = (Node)n.getProperty("parent2");
                 if ((parent1.getName().equals("MethodDeclaration")) &&
                         (parent2.getName().equals("ClassBody"))){
                     methods.set(methods.size()-1,methods.get(methods.size()-1)+" void");
                         }
             }
 
             public void visitModifier(GNode n){
                 visit(n);
                 Node parent1 = (Node)n.getProperty("parent1");
                 Node parent2 = (Node)n.getProperty("parent2");
                 if ((parent1.getName().equals("MethodDeclaration")) &&
                         (parent2.getName().equals("ClassBody"))){
                     String name = n.getString(0);
                     methods.set(methods.size()-1,methods.get(methods.size()-1)+" "+name);
 		}
 		else if((parent1.getName().equals("FieldDeclaration")) &&
 			(parent2.getName().equals("ClassBody"))) {
 		    dataFields.set(dataFields.size()-1,dataFields.get(dataFields.size()-1)+" "+n.getString(0));
 		}
             }
 
             public void visitDeclarators(GNode n) {
                 visit(n);
             }
 
             public void visitCompilationUnit(GNode n) {
 
                 visit(n);
                 //identify the Object bubble so we can link it
                 Bubble object = new Bubble(null, null);
                 for(Bubble b : bubbleList){
                     if(b.getName().equals("Object")){
                         object = b;
                     }
                 }
                 //link Object bubble to children and vice versa
                 for(Bubble b: bubbleList){
                     //System.out.println(b.getName());
                     if(!(b == object) && b.parentToString() == null){
                         b.setParent(object);
                         object.addChild(b.getName());
                     }
                 }
 
                 /*
                    runtime.console().pln("CLASS NAME:");
                    runtime.console().pln(className);
                    runtime.console().pln("DATA FIELDS:");
                    for(String a : dataFields){
                    runtime.console().pln(a);
                    }
                    runtime.console().pln("METHOD HEADERS:");
                    for(String a : methods){
                    runtime.console().pln(a);
                    }
                    runtime.console().p("\n").flush();
                    */
 
             }
 
 
 
 
             public void visitDeclarator(GNode n) {
                 visit(n);
                 Node parent1 = (Node)n.getProperty("parent1");
                 Node parent2 = (Node)n.getProperty("parent2");
                 if ((parent1.getName().equals("FieldDeclaration")) &&
                         (parent2.getName().equals("ClassBody"))){
                     String name = n.getString(0);
                     dataFields.set(dataFields.size()-1,dataFields.get(dataFields.size()-1)+" "+name);
                         }
 
             }
 
             public void visitIntegerLiteral(GNode n) {
                 visit(n);
             }
 
             public void visitClassBody(GNode n){
                 visit(n);
             }
 
             //ArrayList<String> methods = new ArrayList<String>();
             //ArrayList<String> dataFields = new ArrayList<String>();
             //String parent;
 
 
             public void visitClassDeclaration(GNode n){
                 bubbleList.add(new Bubble(n.getString(1), null));
                 visit(n);
                 //get parent
                 //if none: parent = object
                 className = n.getString(1);
                 String parentName = "";
                 //get inheritance
                 if (!n.hasProperty("parent_class")){
                     n.setProperty("parent_class", "Object");
                 }
                 parentName = (String)n.getProperty("parent_class");
 
                 Boolean parentFound = false;
                 Bubble parent = null;
                 for(Bubble b : bubbleList){
                     //if the bubble has already been added by a child
                     if(b.getName().equals(parentName)){
                         //want to set the child field of this bubble with my name
                         parent = b;
                         parentFound = true;
                         b.addChild(className);
                     }
                 }
 
                 if(!parentFound){
                     parent = new Bubble(parentName, className);
                     bubbleList.add(parent);
                 }
 
                 //if classname in bubbleList
                 //set the data fields
                 Boolean bubbleExists = false;
                 for(Bubble b : bubbleList){
                     if(b.getName().equals(className)) {
                         b.setMethods(methods.toArray(new String[methods.size()]));
                         b.setDataFields(dataFields.toArray(new String[dataFields.size()]));
                         b.setConstructors(constructors.toArray(new String[constructors.size()]));
                         b.setPackageName(packageName);
                         if(parent != null) //it won't ever be null, but just to make compiler happy :P
                             b.setParent(parent);
                         bubbleExists = true;
                     }
                 }
                 //else: make that node
                 if(!bubbleExists){
                     Bubble temp = new Bubble(className,
                             methods.toArray(new String[methods.size()]),
                             dataFields.toArray(new String[dataFields.size()]),
                             parent, null, packageName, constructors.toArray(new String[constructors.size()]));
                     bubbleList.add(temp);
                 }
             }
 
             public void visitExtension(GNode n){
                 visit(n);
             }
 
             public void visitFormalParameters(GNode n){
 
                 visit(n);
                 Node parent1 = (Node)n.getProperty("parent1");
                 Node parent2 = (Node)n.getProperty("parent2");
 
                 if ((parent1.getName().equals("MethodDeclaration")) &&
                         (parent2.getName().equals("ClassBody"))){
                     methods.set(methods.size()-1,methods.get(methods.size()-1)+"(");
                         }
 
                 if ((parent1.getName().equals("MethodDeclaration")) &&
                         (parent2.getName().equals("ClassBody"))){
                     methods.set(methods.size()-1,methods.get(methods.size()-1)+")");
                         }
             }
 
             public void visitFormalParameter(GNode n) {
                 visit(n);
                 Node parent1 = (Node)n.getProperty("parent1");
                 Node parent2 = (Node)n.getProperty("parent2");
                 if ((parent1.getName().equals("MethodDeclaration")) &&
                         (parent2.getName().equals("ClassBody"))){
                     String name = n.getString(3);
                     methods.set(methods.size()-1,methods.get(methods.size()-1)+" "+name);
                         }
                 else if ((parent1.getName().equals("ConstructorDeclaration")) &&
                         (parent2.getName().equals("ClassBody"))){
                     String name = n.getString(3);
                     constructors.set(constructors.size()-1,constructors.get(constructors.size()-1)+" "+name);
                         }
             }
 	    /*
 	     *
 	      WTF IS THIS SHIT WITH THE PARENT1 != GETPROPERTY("PARENT0")
 	      *
 	     */
             public void visitType(GNode n) {
                 visit(n);
                 Node parent0 = (Node)n.getProperty("parent0");
                 Node parent2 = (Node)n.getProperty("parent2");
                 Node parent3 = (Node)n.getProperty("parent3");
 
                 if (!(parent0.getName().equals("FieldDeclaration")) && (parent2.getName().equals("MethodDeclaration")) &&
                         (parent3.getName().equals("ClassBody"))){
 
                     String name = getStringDescendants(n);
                     methods.set(methods.size()-1,methods.get(methods.size()-1)+" "+name);
                 }
                 else if ((parent2.getName().equals("ConstructorDeclaration")) &&
                         (parent3.getName().equals("ClassBody"))){
                     String name = getStringDescendants(n);
                     constructors.set(constructors.size()-1,constructors.get(constructors.size()-1)+" "+name);
                 }
 
             }
 
 	    public void visitTypeInstantiation(GNode n) {
 		visit(n);
 		Node parent2 = (Node)n.getProperty("parent2");
 		Node parent3 = (Node)n.getProperty("parent3");
 		if(parent2.getName().equals("FieldDeclaration") && parent3.getName().equals("ClassBody")) {
 		    dataFields.set(dataFields.size()-1,dataFields.get(dataFields.size()-1)+" "+n.getString(0));
 		}
 	    }
 
             public void visitConstructorDeclaration(GNode n)
             {
                 constructors.add("");
                 visit(n);
                 String name = n.getString(2);
                 constructors.set(constructors.size()-1,constructors.get(constructors.size()-1)+" "+name);
             }
 
 
             public String getStringDescendants(GNode n)
             {
                 String toReturn = "";
                 //runtime.console().pln("PARENT NODE: " + ((Node)(n.getProperty("parent"))).getName()).flush();
                 //runtime.console().pln("NODE: " + n.getName()).flush();
                 for(Object o : n)
                 {
                     if(o != null)
                     {
                         //runtime.console().pln("CHILD: " + o.toString()).flush();
                         if(o instanceof String){
                             //System.out.println(o.toString());
                             toReturn +=  o.toString() + " ";
                         }
                         else
                             toReturn +=  getStringDescendants((GNode)o);
                     }
                 }
                 return toReturn;
             }
             public void visitQualifiedIdentifier(GNode n){
                 visit(n);
                 //for(String s : n.properties())
                 //    System.out.println(s);
                 Node parent0 = (Node)n.getProperty("parent0");
                 Node parent1 = (Node)n.getProperty("parent1");
                 Node parent2 = (Node)n.getProperty("parent2");
                 //System.out.println(parent1);
                 //System.out.println(parent2);
                 if ((parent1.getName().equals("FieldDeclaration")) &&
                         (parent2.getName().equals("ClassBody"))){
                     String name = n.getString(0);
                     dataFields.set(dataFields.size()-1,dataFields.get(dataFields.size()-1)+" "+name);
                         }
 
                 if ((parent1.getName().equals("MethodDeclaration")) &&
                         (parent2.getName().equals("ClassBody"))){
                     String name = n.getString(0);
                     methods.set(methods.size()-1,methods.get(methods.size()-1)+" "+name);
                         }
                 if ((parent1.getName().equals("Extension")) &&
                         (parent2.getName().equals("ClassDeclaration"))){
                     String name = n.getString(0);
                     parent2.setProperty("parent_class", name);
                         }
 
                 if (parent0.getName().equals("PackageDeclaration")){
                     //add all children to packageName
                     String name;
                     for(int i=0; i<n.size(); i++){
                         name = n.getString(i);
                         packageName += " " + name;
                     }
                 }
 
                 boolean inList = false;
                 for(Bubble b : bubbleList){
                     if(b.getName().equals(n.getString(n.size()-1))){
                         inList = true;
                     }
                     //System.out.println(b);
                 }
 
                 if(!inList && !n.getString(n.size()-1).equals("String")){
 
 		    String path = "";
 		    for(int i = 0; i < n.size(); i++) {
 			path+="."+n.getString(i);
 		    }
 
                     System.out.println("about to call findFile on: " + path.substring(1));
 
                     path = d.findFile(path);
 
 		    if(!path.equals("")){
                         System.out.println(path);
                         try{
                             d.process(path);
                         } catch (Exception e) {System.out.println(e);}
                     }
                 }
             }
 
             public void visitImportDeclaration(GNode n){
                 visit(n);
             }
 
             public void visitForStatement(GNode n)
             {
                 visit(n);
             }
 
             public void visitBasicForControl(GNode n)
             {
                 visit(n);
             }
 
             public void visitPrimitiveType(GNode n)
             {
                 visit(n);
                 Node parent1 = (Node)n.getProperty("parent1");
                 Node parent2 = (Node)n.getProperty("parent2");
 
                 if ((parent1.getName().equals("MethodDeclaration")) &&
                         (parent2.getName().equals("ClassBody")))
                 {
                     methods.set(methods.size()-1,methods.get(methods.size()-1)+" " + n.getString(0));
                 }
             }
 
 
             public void visitExpressionList(GNode n)
             {
                 visit(n);
             }
 
             public void visitRelationalExpression(GNode n)
             {
                 visit(n);
             }
 
             public void visit(Node n)
             {
 
                 int counter = 1;
                 if(n.hasProperty("parent0")) {
                     Node temp = (Node)n.getProperty("parent0");
 
                     while(temp != null) {
                         //System.out.println(temp);
                         //temp = (Node)temp.getProperty("parent0");
 
 
                         n.setProperty("parent"+(counter++), temp.getProperty("parent0"));
                         temp = (Node)temp.getProperty("parent0");
                         //if(n.getProperty("parent2") == null)
                         //System.out.println(temp);
                     }
                 }
                 //don't need this, but not deleting.
                 for (String s : n.properties()) {
                     //System.out.println(n.getProperty(s));
                 }
 
                 for (Object o : n){
                     if (o instanceof Node){
                         ((Node)o).setProperty("parent_name", n.getName() );
                         ((Node)o).setProperty("parent0", n );
                         dispatch((Node)o);
                     }
                 }
             }//}}}
         }.dispatch(node);
     }
 
     //recursive call to populate all vtables in bubbleList
     public static void populateVTables(Bubble root){//{{{
         boolean overwritten = false;
         for(Bubble b : bubbleList){
             if (b.getParent() == root){
                 //creating child's vTable
                 for(String s : root.getVtable()) //getting parent's vtable
                     b.add2Vtable(s);
 
                 int i = 0;
                 for(String s : b.getMethods()) //adding new methods to vtable
                 {
                     //if its a main method, don't add it to vtable
                     if((s.indexOf("public static String [  args") == -1) && (s.indexOf("main") == -1))
                     {
                        overwritten = b.add2Vtable(s);
                        if(overwritten)
                        {
                            s = s + "\t";
                            b.setMethodAtIndex(i, s);
 
                            overwritten = false;
                        }
                     }
 
                     i++;
                 }
                 //recursively setting child's vtables
                 populateVTables(b);
             }
         }
 
         //cleaning vtables
         int i = 0;
         for(String s : root.getVtable())
         {
           s = s.replace("(Object", "(" + root.getName());
           root.setVtableIndex(i, s);
           i++;
         }
 
     }//}}}
 
     public static void formatConstructors()//{{{
     {
         String tmp = "";
         String cls = ""; //class name of constructor
         String[] sploded;
         ArrayList<String> newCons = new ArrayList<String>();
         for(Bubble b : bubbleList)
         {
             if(b.getConstructors() != null)
             {
                 for(String s: b.getConstructors()) //for each constructor
                 {
                     sploded = s.split(" ");
                     cls = sploded[sploded.length-1];
                     tmp = tmp + sploded[sploded.length-1] + "(";
                     //Get parameters
                     for(String part : sploded)
                     {
                         part = part.replace(" ", ""); //takes away spaces
                         if (part == cls)
                             break;
                         if (part.length() > 0)
                         {
                             if (part.indexOf("[") != -1)
                                 tmp = tmp + part + "]";
                             else
                                 tmp = tmp + " " + part;
                         }
                     }
                     tmp = tmp + ");";
                     newCons.add(tmp);
                     cls = "";
                     tmp = "";
                 }
 
                 //setting new constructors
                 b.setConstructors(newCons.toArray(new String[newCons.size()]));
                 newCons.clear();
             }
 
         }
 
     }
 
     public static void markNewMethods()
     {
         int index;
         for (Bubble b : bubbleList)
         {
             Bubble parent = b.getParent();
             if (parent == null)
                 continue;
 
             for (index = parent.getVtable().size(); index < b.getVtable().size(); index ++)
                 b.getVtable().set(index, b.getVtable().get(index) + "\t");
 
         }
     }
 
     public static void start(Bubble object)
     {
         populateVTables(object);
         markNewMethods();
         formatConstructors();
     }
     /**
      * Run the thing with the specified command line arguments.
      *
      * @param args The command line arguments.
      */
     static Decl d;
     static ArrayList<Bubble> bubbleList = new ArrayList<Bubble>();
     static ArrayList<PNode> packageTree = new ArrayList<PNode>();
     public static ArrayList<Mubble> mubbleList = new ArrayList<Mubble>();
     public static void main(String[] args)
     {//{{{
         packageTree.add(new PNode("DefaultPackage", null));
         //pre-load Object Bubble
         Bubble object = new Bubble("Object", null);
         //Creating Object's Vtable
         object.add2Vtable("Class __isa;");
         object.add2Vtable("int32_t (*hashCode)(Object);");
         object.add2Vtable("bool (*equals)(Object, Object);");
         object.add2Vtable("Class (*getClass)(Object);");
         object.add2Vtable("String (*toString)(Object);");
         bubbleList.add(object);
 
 
         //pre-load String Bubble
         Bubble string = new Bubble("String", null);
         //Creating Object's Vtable
         string.add2Vtable("Class __isa;");
         string.add2Vtable("int32_t (*hashCode)(String);");
         string.add2Vtable("bool (*equals)(String, Object);");
         string.add2Vtable("Class (*getClass)(String);");
         string.add2Vtable("String (*toString)(String);");
         string.add2Vtable("int32_t (*length)(String);");
         string.add2Vtable("char (*charAt)(String, int_32_t);");
         bubbleList.add(string);
 
         d = new Decl();
         d.init();
         d.prepare();
         for(int i = 0; i< args.length; i++){
             try{
                 d.process(args[i]);
             } catch (Exception e) {System.out.println(e);}
         }
 
         start(object);
 
         /*
          * Attach structs to packageTree
          */
         String uniStruct = "//Forward Decls of All Structs in package \n";
         String typedefs = "\n";
         String methName = "";
         //ADDED --Forward Decls of stucts and vtables
 
         for(Bubble b: bubbleList){//{{{
             //System.out.println("--------------------" + b.getName() + "--------------------");
 
 
 
             /*
             System.out.println(b);
             */
             //keep track of where we are indent-wise
             int indent = 0;
 
             //ignore string and object, they are lame
             if(b.getName() != "String" && b.getName() != "Object"){
 
 		        String struct = "";
                 //print the .h SON
 
                 //find which package node to add this struct to
                 String packName = b.getPackageName();
                 PNode p = constructPackageTree(packName);
 
 
                 //assemble the struct as a large string
 //{{{
                 struct +=(indentLevel(indent) + "struct _" + b.getName() + " {"+ "\n");
                 indent++;
 
                 //print data fields (Assumes correct format for them)
                 struct +=("//Data fields"+ "\n");
                 struct +=(indentLevel(indent)+ "_" + b.getName() + "_VT* __vptr;"+ "\n");
                 String[] dataFields = b.getDataFields();
                 for(int i= 0; i< dataFields.length; i++){
                     struct +=(indentLevel(indent) + dataFields[i]+ "\n");
                 }
 
                 struct +="\n";
 
                 //print constructors (assumes correct format)
                 struct +=("//Constructors"+ "\n");
                 String[] constructors = b.getConstructors();
                 for(int i= 0; i< constructors.length; i++){
                     struct +=(indentLevel(indent) + "_" + constructors[i]+ "\n");
                 }
 
                 struct +="\n";
                 struct +=("//Forward declaration of methods"+ "\n");
                 //print forward declarations of methods
 
 
                 //want to count number of words after any final/public/etc
                 //if  even:
                 //  first word after public/static etc is return type
                 //  following pairs are parameters in (type, name) format
                 //  last word is method name
                 //if odd:
                 //  following pairs are (type, name)
                 //  last word is method name
                 //
                 //Want to go from:
                 //  public String int  indent indentLevel
                 //To:
                 //  static <return type> <method name> (<Class name>, <argument type>)
                 for( String s : b.getMethods()){
                     int count = 0;
                     //String[] splitsies =  s.split(" ");
                     //want to remove extra
                     String returnType = "void";
                     String methodName;//{{{
                     String className = b.getName();
 
 
                     int square = 0;//{{{
                     for (int i = 0; i < s.length(); i++) {
                         if (s.charAt(i) == '[') square++;
                     }
                     String[] temp2 = s.split(" ");
                     for (int j = 0; j < temp2.length; j++) {
                         if (temp2[j].length() != 0) count++;
                     }
 
                     //take out pub/priv/etc and group [ with its previous word
                     for(String g : temp2){//{{{
                         //System.out.println(g);
                         if (g.equals("public") ||
                                 g.equals("private") ||
                                 g.equals("protected") ||
                                 g.equals("static") ||
                                 g.equals("final")) {count--;}
                         //count - square = the number of words (minus [ which are extras )
                         else{
 
                         }
                     }//}}}
                     String[] realWords = new String[count-square];
                     int gi=0;
                     //System.out.println(s);
                     //System.out.println("size of realWords = " + realWords.length);
                     for(int i = 0; i < temp2.length ; i++){//{{{
                         String g = temp2[i];
                         if (!(g.equals("public") || g.equals("private") ||g.equals("protected") ||
                                     g.equals("static") ||g.equals("final") || g.equals(" ") || g.length() == 0)) {
                             //System.out.println("The part of temp2 is: " + g);
                             if(g.equals("[")){
                                 realWords[gi-1] += "[]";
                                 gi--;
                             }
                             else{
                                 realWords[gi] = g;
                             }
                             gi++;
                         }
                     }//}}}
                     //realwords now has return type if there is one,
                     //pairs of parameters, and method name//}}}
                     int realLen = realWords.length;
                     methodName = realWords[realLen -1];
 
                     //change types
                     //replace arrays
                     for(int i=0; i < realLen; i++){
                         String word = realWords[i];
                         if(word.equals("int"))
                             realWords[i] = "int32_t";
                         if(word.equals("boolean"))
                             realWords[i] = "bool";
                         if(word.charAt(word.length()-1)==']'){
                            realWords[i] = "__rt::Array<"+ word.substring(0, word.length() -3) +">*";
                         }
                     }
 
 
 
                     if(realLen % 2 == 0){
                         returnType = realWords[0];
                         //System.out.println("return type is:" + returnType);
                         for(int i = 1; i < realLen -1; i++){
                             if(i%2 == 1){
                                 className += ", " +realWords[i];
                             }
                         }
                     }
                     else{
                         for(int i = 0; i < realLen -1; i++){
                             if(i%2 == 0){
                                 className += ", " + realWords[i];
                             }
                         }
                     }//}}}
 
                     //everything should be in correct format by now;
                     struct += "static " + returnType + " " + methodName + " (" + className + ")\n";
                     //System.out.println("static " + returnType + " " + methodName + " (" + className + ");\n");
                 }
 
 
 
 
 
 
 
                 struct +="\n";
 
                 //extra shit
                 struct +=(indentLevel(indent) + "static Class __class();\n" );
                 struct +=(indentLevel(indent) + "static _" + b.getName() + "_VT __vtable;"+ "\n");
 
                 for(int i = indent; i>0; i--){
                     struct+=("}");
                     if(indent == 1)
                         struct+=(";");
                     struct +="\n";
                 }//}}}
                // System.out.println(struct);
                 //System.out.println("think my package node is: " + p.getName());
 
                 //Add struct to correct PNode
                 p.addStructChild(struct);
 
 		//////////////////////
 		//Making vtable struct
 		//////////////////////
 
 		indent = 0;
 		String fullName = b.getPackageName().trim().replaceAll("\\s",".")+(b.getPackageName().equals("") ? "" : ".")+b.getName();
 		//Vtable comment
 		struct = indentLevel(indent) + "// The vtable layout for "+fullName+".\n";
 		//First struct line
 		struct += indentLevel(indent) + "struct _"+b.getName()+"_VT {\n";
 		indent++;
 		//Add vtable method decls
 		for(Object m : b.getVtable().toArray()) {
 		    struct += indentLevel(indent)+((String)m)+"\n";
 		}
 		//Add the constructor decl and :
 		struct+= "\n"+indentLevel(indent)+"_"+b.getName()+"_VT()\n"+indentLevel(indent)+":";
 
 		for(Object m : b.getVtable().toArray()) {
 		    String mm = (String)m;
 
 		    //if it's in the right format
 		    if(mm.matches("\\s*\\w+\\s*\\(\\*\\w+\\)\\s*\\(.*\\)\\s*;\\s*")){
 			String methodName = "";
 			String retType = "";
 			String params = "";
 			//get method name
 			Matcher match_m = Pattern.compile("(?<=(\\(\\*))\\w+(?=(\\)))").matcher(mm);
 			match_m.find();
 			methodName = match_m.group(0);
 
 			//if it's inherited
 			if(mm.charAt(mm.length()-1)!='\t') {
 
 			    //get return type
 			    Matcher match_rt = Pattern.compile("\\w+(?=(\\s*\\(\\*\\w+\\)\\s*\\(.*\\)\\s*;))").matcher(mm);
 			    match_rt.find();
 			    retType = match_rt.group(0);
 
 			    //get params
 			    Matcher match_p = Pattern.compile("(?<=(\\())[^\\*].+(?=\\))").matcher(mm);
 			    //Matcher match_p = Pattern.compile("(?<=(\\s*\\w+\\s*\\(\\*\\w+\\)\\s*\\()).*(?=(\\)))").matcher(mm);
 			    match_p.find();
 
 			    params = match_p.group(0);
 
 			    //Add that shit to struct
 			    struct += indentLevel(indent)+"  "+methodName+"(("+retType+"(*)("+params+"))&_"+(b.getParent().getName().equals("Object") || b.getParent().getName().equals("String") ? "_" : "")+b.getParent().getName()+"::"+methodName+"),\n";
 			}
 			//inherited methods get parent after &
 			//if it's overwritten or new
 			else {
 			    //Add that shit to struct
 			    struct += indentLevel(indent)+"  "+methodName+"(&_"+b.getName()+"::"+methodName+"),\n";
 			}
 		    }
 		    //if it's just __isa
 		    else if(mm.contains("__isa")) {
 			struct+= " __isa(_"+b.getName()+"::__class()),\n";
 		    }
 		}
 		struct = struct.substring(0,struct.length()-2)+" {\n"+indentLevel(indent--)+"}\n"+indentLevel(indent)+"};";
 		p.addStructChild(struct);
 
 	    }
 
         }//}}}
 
         //assign Children to PNodes
         for(PNode p : packageTree){
             if(!p.getName().equals("DefaultPackage")) {
                 for(PNode r : packageTree){
                     if (r.getName().equals(p.getParent().getName())){
                         r.addPNodeChild(p);
                     }
                 }
             }
 
         }
 
         for(PNode p : packageTree)
         {
             uniStruct = "";
             typedefs = "";
             if(p.getStructChildren() == null)
                 continue;
             for(String c : p.getStructChildren())
             {
                 if(c == null)
                     continue;
                 else if (c.indexOf("struct") == -1)
                     continue;
 
                 String className = Mubble.getStringBetween(c, "struct ", " {");
                 uniStruct += "struct " + className + ";\n";
                 String bareClassName = className.replace("_", "");
                 bareClassName = bareClassName.replace("VT", "");
                 if(className.indexOf("VT") == -1)
                     typedefs += "typedef " + className + "* " + bareClassName + ";\n";
             }
             uniStruct += typedefs;
             p.addFirstStruct(uniStruct);
 
         }
         /*
         System.out.println("NOW PRINTING PNODE TREE");
         //Print out each PNode
         for(PNode p : packageTree){
             System.out.println("------------------"+ p.getName() + "----------------");
             System.out.println(p);
         }
         */
 
         /* print later
 	for (Bubble b : bubbleList)
 	    b.printToFile(1);
 
         */
 
         ////////////////////////////////////////////////////////////////////////////////////////
         ////////////////////////// Should be done with .h by here///////////////////////////////
         ////////////////////////////////////////////////////////////////////////////////////////
 
 
         //Write .h to file
         String hFile = "test.h";
         try{
         File out = new File(hFile);
         FileWriter hstream = new FileWriter(out);
         BufferedWriter hwrite = new BufferedWriter(hstream);
         String includes = "#pragma once\n";
         includes += "#include \"java_lang.h\"\n";
         includes += "#include <stdint.h>\n";
         includes += "\n\n"; //for good measure
         hwrite.write(includes);
 
         String forwardh ="";
         for(PNode p : packageTree){
             if(p.getName().equals("DefaultPackage")){
                 forwardh += p.getForwardDecl();
             }
         }
         /*
          *Iterate through packageTree: in order (dfs)
          */
         String doth = "";
         //find Default package
         for(PNode p : packageTree){
             if(p.getName().equals("DefaultPackage")){
                 doth += p.getOutput();
             }
         }
 
         hwrite.write(forwardh);
         hwrite.write(doth);
         hwrite.close();
         } catch (Exception e){System.out.println("Error writing: "+ e);}
 
 
         //Add all Mubbles to the list
         for(Bubble b: bubbleList){
             String[] methods = b.getMethods();
             if (methods != null)
             {
                 for(String entry : methods) {
                     mubbleList.add(new Mubble(b.getName(), entry, false));
                 }
 
             }
             if(b.getConstructors() != null){
                 for(String a : b.getConstructors()){
                     //System.out.println(a);
 
                     String correctHeader = "_"+ b.getName() + "::_" + b.getName() + "(";
                     String params = Mubble.getStringBetween(a, "(", ")").trim();
                     String[] paramSplit = params.split(" ");
                     String[] temp;
                     int emptyCount=0;
                     for(int i=0; i < paramSplit.length ; i++){
                         if(paramSplit[i].length() == 0) {
                             emptyCount++;
                         }
                     }
                     temp = new String[paramSplit.length-(emptyCount)];
                     int ti=0;
                     //System.out.println("temp length is: " +temp.length);
                     for(int i=0; i < paramSplit.length ; i++){
                         if(!(paramSplit[i].length() == 0)) {
                             temp[ti] = paramSplit[i];
                             ti++;
                         }
                     }
                     paramSplit = temp;
 
                     for(int i=0; i < paramSplit.length; i++){
                         if(paramSplit[i].length() != 0){
                             String word = paramSplit[i];
                             if(word.equals("int"))
                                 paramSplit[i] = "int32_t";
                             if(word.equals("boolean"))
                                 paramSplit[i] = "bool";
                             if(word.charAt(word.length()-1)==']'){
                                 paramSplit[i] = "__rt::Array<"+ word.substring(0, word.length() -2) +">*";
                             }
                         }
                     }
                     for(int i=0; i < paramSplit.length - 1; i+=2){
                         correctHeader += paramSplit[i] + " " + paramSplit[i+1]+ ", ";
                     }
 
                     correctHeader = correctHeader.substring(0, correctHeader.length()-2) + ")";
                     //System.out.println(correctHeader);
                     mubbleList.add(new Mubble(b.getName(),correctHeader, true ));
                 }
 
             }
 
         }
 
 //===============IMPL SHIT====================================//
         Impl Q = new Impl(bubbleList, packageTree, mubbleList);
         Q.init();
         Q.prepare();
         for(int i = 0; i< args.length; i++){
             try{
                 Q.process(args[i]);
             } catch (Exception e) {System.out.println(e);}
         }
 
 
         //Write .cc to file
         try{
         File out = new File("test.cc");
         FileWriter ccstream = new FileWriter(out);
         BufferedWriter ccwrite = new BufferedWriter(ccstream);
         String includes = "#pragma once\n";
         includes += "#include \"" + hFile + "\"\n";
         includes += "\n\n"; //for good measure
         ccwrite.write(includes);
         /*
          *Iterate through packageTree: in order (dfs)
          */
         String dotcc = "";
         //find Default package
         for(PNode p : packageTree){
             if(p.getName().equals("DefaultPackage")){
                 dotcc += p.getOutputCC();
             }
         }
 
         ccwrite.write(dotcc);
         ccwrite.close();
         } catch (Exception e){System.out.println("Error writing: "+ e);}
     }//}}}
 
 
 
     public static PNode constructPackageTree(String packageName){//{{{
 
         //making sure tree branch exists for this full package name
         // returns node at the leaf of the branch
         //  -create node
         //  -search for parent:
         //      if exists: assign parent
         //      else: create parent recursively thru this method
         //
         //  Note: this will not assign children. make a second pass for that
         //  TODO add "DefaultPackage" to the packagetree
 
 
         if(packageName.equals("")){
 
             for(PNode n : packageTree){
                 if(n.getName().equals("DefaultPackage")){
                     return n;
                 }
             }
         }
 
         for(PNode n : packageTree){
             if(n.getName().equals(packageName)){
                 return n;
             }
         }
 
 
         String[] packageNameSplit = packageName.split("\\s");
 
         //see if parent has been created already
         String parentName = "";
         for(int i=0; i<packageNameSplit.length -1; i ++)
             if(!(i == packageNameSplit.length -2))
                 parentName += packageNameSplit[i] + " ";
             else
                 parentName += packageNameSplit[i];
         //System.out.println("Parent Name is: "+ parentName);
 
         if(parentName == "")
             parentName = "DefaultPackage";
 
         boolean parentFound = false;
         PNode parent= new PNode("INVALID");
         for(PNode n : packageTree){
             if(n.getName().equals(parentName)){
                 parent = n;
                 parentFound = true;
             }
         }
         if(!parentFound){
             parent = constructPackageTree(parentName);
         }
 
         PNode toReturn = new PNode(packageName, parent);
 
         packageTree.add(toReturn);
 
         return toReturn;
     }//}}}
 
     public static String indentLevel(int indent){
         String toReturn = "";
         for( int i=0; i<indent; i++){
             toReturn += "  ";
         }
         return toReturn;
     }
 }
 
 class Impl extends xtc.util.Tool{
 
     public static ArrayList<Bubble> bubbleList;
     public static ArrayList<PNode> packageTree;
     public static ArrayList<Mubble> mubbleList;
 
     public Impl(ArrayList<Bubble> bubbleList, ArrayList<PNode> packageTree, ArrayList<Mubble> mubbleList)
     {
         this.bubbleList = bubbleList;
         this.packageTree = packageTree;
         this.mubbleList = mubbleList;
     }
 
     public void init(){
         super.init();
     }
 
 
     public String getName()
     {
         return "Java to C++ translator";
     }
 
     public String getCopy()
     {
         return "Ninja assassins: dk, calvin, Andrew*2";
     }
 
     public Node parse(Reader in, File file) throws IOException, ParseException
     {
         JavaFiveParser parser = new JavaFiveParser(in, file.toString(), (int)file.length());
         Result result = parser.pCompilationUnit(0);
 
         return (Node)parser.value(result);
     }
 
     public void process(Node node)
     {
         new Visitor()
         {
 
 
             public void visitFieldDeclaration(GNode n){
 		if (onMeth) {
 		   ;
 		}
                 visit(n);
 		if (onMeth) {
 		    //methodString += ";\n";
 		}
             }
 
             public void visitDimensions(GNode n) {
                 visit(n);
             }
 
             public void visitModifiers(GNode n){
                 visit(n);
             }
 
             String tempString = "";
             String tmpCode = "";
             boolean onMeth = false;
             Mubble curMub = null;
 	    String methodString = "";
             public void visitMethodDeclaration(GNode n)
             {
                 visit(n);
 
                 tmpCode = "";
 
                 Node parent0 = (Node)n.getProperty("parent0");
                 Node parent1 = (Node)parent0.getProperty("parent0");
 
                 //Parent 1 Should be class decl
                 String classname = parent1.getString(1);
                 String methodname = n.getString(3);
 
                 for(Mubble m : mubbleList){
                     if(m.getName().equals(classname) && m.getMethName().equals(methodname))
                         curMub = m;
                 }
 
  //==============Assigning Package to CurMub===================//
                 //Assuming curMub has code
                 for(Bubble b: bubbleList)
                 {
                     if(b.getName().equals(classname)) // b's package is curMub's package
                     {
                         if(b.getPackageName().equals(""))
                             curMub.setPackageName("DefaultPackage");
                         else
                             curMub.setPackageName(b.getPackageName());
                         break;
                     }
                 }
                 //Adding curMub to the right pNode
                 for(PNode p : packageTree)
                 {
                     //System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&7 ADDING MUBBLE");
                     //System.out.println("P name: " + p.getName());
                     //System.out.println("curMub: " + curMub.getPackageName());
                     if(p.getName().equals(curMub.getPackageName()))
                         p.addMubble(curMub);
                 }
 //==============================================================//
 
 		//System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
 		//System.out.println(methodString);
                 //onMeth = false;
 		//methodString = "";
             }
 
             public void visitModifier(GNode n){
                 visit(n);
 
             }
 
 	    public void visitCallExpression(GNode n) {
 		//visit(n);
 		if (onMeth) {
 		    String tmp = "";
 		    /*
 		    for (Object o : n) {
 			if (o instanceof String && !((String)o).equals("")) {
 			    methodString += (String)o + "(";
 			}
 		    }
 		    */
 		    dispatchBitch(n);
 		    dispatch(n.getNode(0));
 		    methodString += n.getString(2) + "(";
 		    dispatch(n.getNode(3));
 		}
 		else {
 		    visit(n);
 		}
 	    }
 
 	    public void visitEmptyStatement(GNode n) {
 		if (onMeth) {
 		    methodString += ";\n";
 		}
 		visit(n);
 	    }
 
 	    public void visitConditionalStatement(GNode n) {
 		if (onMeth) {
 		    dispatchBitch(n);
 		    Node parent = (Node)n.getProperty("parent0");
 		    if (parent.getName().equals("ConditionalStatement")) {
 			methodString += "\n}\nelse ";
 		    }
 		    methodString += "if (";
 		    dispatch(n.getNode(0));
 		    methodString += ") {\n";
 		    for (int i = 1; i < n.size()-1; i++) {
 			dispatch(n.getNode(i));
 		    }
 		    if (n.getNode(n.size()-1) != null) {
 			Node parent1 = (Node)parent.getProperty("parent0");
 			if (!n.getNode(n.size()-1).getName()
 			    .equals("ConditionalStatement")) {
 			    methodString += "\n}\nelse {\n";
 			}
 			/*
 			if (!parent.getName().equals("ConditionalStatement")) {
 			    methodString += "\n}\nelse {\n";
 			}
 			*/
 			dispatch(n.getNode(n.size()-1));
 		    }
 		    if (!parent.getName().equals("ConditionalStatement")) {
 			methodString += "\n}\n";
 		    }
 		    //methodString += "}\n";
 		}
 		else {
 		    visit(n);
 		}
 	    }
 
 	    public void visitConditionalExpression(GNode n) {
 		if (onMeth) {
 		    dispatchBitch(n);
 		    methodString += "(";
 		    dispatch(n.getNode(0));
 		    methodString += " ? ";
 		    dispatch(n.getNode(1));
 		    methodString += " : ";
 		    dispatch(n.getNode(2));
 		    methodString += ")";
 		}
 		else {
 		    visit(n);
 		}
 	    }
 
             public void visitDeclarators(GNode n) {
                 visit(n);
 
 		if (onMeth && !((Node)n.getProperty("parent0")).getName()
 		    .equals("BasicForControl")) {
 		    methodString += ";\n";
 		}
             }
 
 	    public void visitBooleanLiteral(GNode n) {
 		if (onMeth) {
 		    methodString += n.getString(0);
 		}
 		visit(n);
 	    }
 
             public void visitDeclarator(GNode n) {
 		if (onMeth) {
 		    methodString += " " + n.getString(0);
 		    Object third = n.get(2);
 		    if (third instanceof Node) {
 			methodString += " = ";
 		    }
 		}
                 visit(n);
 		if (onMeth) {
 		    //methodString += ";\n";
 		}
             }
 
 	    public void visitEqualityExpression(GNode n) {
 		if(onMeth) {
 		    dispatchBitch(n);
 		    dispatch(n.getNode(0));
 		    methodString += " " + n.getString(1) + " ";
 		    dispatch(n.getNode(2));
 		}
 		else {
 		    visit(n);
 		}
 	    }
 
 
             public void visitIntegerLiteral(GNode n) {
 		if (onMeth) {
 		    methodString += n.getString(0);
 		}
                 visit(n);
             }
 
             public void visitClassBody(GNode n){
                 visit(n);
             }
 
             public void visitClassDeclaration(GNode n){
                 visit(n);
             }
 
             public void visitFormalParameters(GNode n){
                 visit(n);
             }
 
             public void visitFormalParameter(GNode n) {
 
                 visit(n);
             }
 
             public void visitQualifiedIdentifier(GNode n){
                 visit(n);
             }
 
             public void visitImportDeclaration(GNode n){
                 visit(n);
             }
 
             public void visitForStatement(GNode n)
             {
 		if (onMeth) {
 		    methodString += "for(";
 		}
                 visit(n);
 		if (onMeth) {
 		    methodString += "}\n";
 		}
             }
 
 	    public void visitLogicalAndExpression(GNode n) {
 		if(onMeth) {
 		    dispatchBitch(n);
 		    methodString += "(";
 		    dispatch(n.getNode(0));
 		    methodString += ") && (";
 		    dispatch(n.getNode(1));
 		    methodString += ")";
 		}
 		else {
 		    visit(n);
 		}
 	    }
 
 	    public void visitExpression(GNode n) {
 		if (onMeth) {
 		    dispatchBitch(n);
 		    dispatch(n.getNode(0));
 		    methodString += " " + n.getString(1) + " ";
 		    dispatch(n.getNode(2));
 
 		} else {
 		    visit(n);
 		}
 	    }
 
 	    public void visitExpressionStatement(GNode n) {
 		visit(n);
 		if (onMeth) {
 		    methodString += ";\n";
 		}
 	    }
 
 	    public void visitLogicalOrExpression(GNode n) {
 		if(onMeth) {
 		    dispatchBitch(n);
 		    methodString += "(";
 		    dispatch(n.getNode(0));
 		    methodString += ") || (";
 		    dispatch(n.getNode(1));
 		    methodString += ")";
 		}
 		else {
 		    visit(n);
 		}
 	    }
 
             public void visitBasicForControl(GNode n)
             {
 		if(onMeth) {
 		    dispatchBitch(n);
 		    for(int i = 0; i < n.size(); i++) {
 			dispatch(n.getNode(i));
 			if( i >= 2 && i < n.size()-1) {
 			    methodString += "; ";
 			}
 		    }
 		    methodString += ") {\n";
 		}
 		else {
 		    visit(n);
 		}
             }
 
 	    public void visitBlock(GNode n) {
 		if(((Node)n.getProperty("parent0")).getName()
 		   .equals("MethodDeclaration")) {
 		    onMeth = true;
 
 		    visit(n);
 		    System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
 		    System.out.println(methodString);
 		    onMeth = false;
 		    methodString = "";
 		}
 		else {
 		    visit(n);
 		}
 	    }
 
 	    public void visitPostfixExpression(GNode n) {
 		if (onMeth) {
 		    visit(n);
 		    methodString += n.getString(1);
 		}
 		else {
 		    visit(n);
 		}
 	    }
 
 	    public void visitPrimaryIdentifier(GNode n) {
 		if (onMeth) {
 		    methodString += n.getString(0);
 		}
 		visit(n);
 	    }
 
             public void visitPrimitiveType(GNode n) {
 		if (onMeth) {
 		    methodString += n.getString(0);
 		}
                 visit(n);
             }
 
 	    public void visitStringLiteral(GNode n) {
 		if (onMeth) {
 		    methodString += n.getString(0);
 		}
 		visit(n);
 	    }
 
 	    public void visitSwitchStatement(GNode n) {
 		if(onMeth) {
 		    dispatchBitch(n);
 		    methodString += "switch(";
 		    dispatch(n.getNode(0));
 		    methodString += ") {\n";
 		    for(int i = 1; i < n.size(); i++) {
 			dispatch(n.getNode(i));
 		    }
 
 		}
 		else {
 
 		}
 	    }
 
 	    public void visitArguments(GNode n) {
 		if (onMeth) {
 		    visit(n);
 		    methodString += ")";
 		}
 		else {
 		    visit(n);
 		}
 	    }
 
 	    public void visitSelectionExpression(GNode n) {
 		if (onMeth) {
 		    visit(n);
 		    if (n.get(1) != null) {
 			methodString += "." + n.getString(1) + ".";
 		    }
 		}
 		else {
 		    visit(n);
 		}
 	    }
 
 	    public void visitReturnStatement(GNode n) {
 		if (onMeth) {
 		    methodString += "return ";
 		}
 		visit(n);
 		if (onMeth) {
 		    methodString += ";\n";
 		}
 	    }
 
 	    public void visitUnaryExpression(GNode n) {
 		if (onMeth) {
 		    methodString += n.getString(0);
 		    visit(n);
 		}
 		else {
 		    visit(n);
 		}
 	    }
 
 	    public void visitWhileStatement(GNode n) {
 		if (onMeth) {
 		    dispatchBitch(n);
 		    methodString += "while(";
 		    dispatch(n.getNode(0));
 		    methodString += ") {\n";
 		    for(int i = 1; i < n.size(); i++) {
 			dispatch(n.getNode(i));
 		    }
 		    methodString += "}\n";
 		}
 		else {
 		    visit(n);
 		}
 	    }
 
             public void visitType(GNode n)
             {
                 visit(n);
             }
 
             public void visitExpressionList(GNode n)
             {
                 visit(n);
             }
 
             public void visitRelationalExpression(GNode n)
             {
 		if (onMeth) {
 		    dispatchBitch(n);
 		    dispatch(n.getNode(0));
 		    methodString += " " + n.getString(1) + " ";
 		    dispatch(n.getNode(2));
 		    //methodString += ";";
 		}
 
                 //visit(n);
             }
 
 	    public void dispatchBitch(Node n) {
                 int counter = 1;
                 if(n.hasProperty("parent0")) {
                     Node temp = (Node)n.getProperty("parent0");
 
                     while(temp != null) {
                         //System.out.println(temp);
                         //temp = (Node)temp.getProperty("parent0");
 
                         n.setProperty("parent"+(counter++), temp.getProperty("parent0"));
                         temp = (Node)temp.getProperty("parent0");
                         //if(n.getProperty("parent2") == null)
                         //System.out.println(temp);
                     }
                 }
                 //don't need this, but not deleting.
                 for (String s : n.properties()) {
                     //System.out.println(n.getProperty(s));
                 }
 
                 for (Object o : n){
                     if (o instanceof Node){
                         ((Node)o).setProperty("parent_name", n.getName() );
                         ((Node)o).setProperty("parent0", n );
                         //dispatch((Node)o);
                     }
                 }
 
 	    }
 
             public void visit(Node n)
             {
 
                 int counter = 1;
                 if(n.hasProperty("parent0")) {
                     Node temp = (Node)n.getProperty("parent0");
 
                     while(temp != null) {
                         //System.out.println(temp);
                         //temp = (Node)temp.getProperty("parent0");
 
                         n.setProperty("parent"+(counter++), temp.getProperty("parent0"));
                         temp = (Node)temp.getProperty("parent0");
                         //if(n.getProperty("parent2") == null)
                         //System.out.println(temp);
                     }
                 }
                 //don't need this, but not deleting.
                 for (String s : n.properties()) {
                     //System.out.println(n.getProperty(s));
                 }
 
                 for (Object o : n){
                     if (o instanceof Node){
                         ((Node)o).setProperty("parent_name", n.getName() );
                         ((Node)o).setProperty("parent0", n );
                         dispatch((Node)o);
                     }
                 }
             }
         }.dispatch(node);
     }
 }
 
