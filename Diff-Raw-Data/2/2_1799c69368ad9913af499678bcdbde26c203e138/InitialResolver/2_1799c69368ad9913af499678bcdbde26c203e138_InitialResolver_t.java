 package soot.javaToJimple;
 import soot.*;
 import java.util.*;
 
 public class InitialResolver {
 
     private polyglot.ast.Node astNode;  // source node
     private soot.SootClass sootClass;   // class being processed
     private ArrayList staticFieldInits; 
     private ArrayList fieldInits;
     private HashMap fieldMap;  // maps field instances to soot fields
     //private HashMap sourceToClassMap; // is used 
     private ArrayList initializerBlocks;
     private ArrayList staticInitializerBlocks;
 	private polyglot.frontend.Compiler compiler; 
     private polyglot.util.Position currentClassDeclPos;
     private BiMap anonClassMap;   // maps New to SootClass (name)
     private HashMap anonTypeMap;    //maps polyglot types to soot types
     private BiMap localClassMap;  // maps LocalClassDecl to SootClass (name)
     private HashMap localTypeMap;   // maps polyglot types to soot types
     private int privateAccessCounter = 0; // global for whole program because
                                           // the methods created are static 
     private HashMap privateAccessMap;
     private HashMap finalLocalInfo; // new or lcd mapped to list of final locals avail in current meth and the whether its static
     private HashMap newToOuterMap;    
   
     private HashMap sootNameToAST = null;
     private ArrayList hasOuterRefInInit; // list of sootclass types that need an outer class this param in for init
    
     private HashMap classToSourceMap;
     private HashMap specialAnonMap;
     
     /**
      * returns true if there is an AST avail for given soot class
      */
     public boolean hasASTForSootName(String name){
        if (sootNameToAST == null) return false;
        if (sootNameToAST.containsKey(name)) return true;
        return false;
     }
     
     /**
      * sets AST for given soot class if possible
      */
     public void setASTForSootName(String name){
         if (!hasASTForSootName(name)) {
             throw new RuntimeException("Can only set AST for name if it exists. You should probably not be calling this method unless you know what you're doing1!");
         }
         setAst((polyglot.ast.Node)sootNameToAST.get(name));
     }
    
     public InitialResolver(soot.Singletons.Global g){}
     public static InitialResolver v() {
         return soot.G.v().soot_javaToJimple_InitialResolver();
     }
     
 
     
     /**
      * Invokes polyglot and gets the AST for the source given in fullPath
      */
     public void formAst(String fullPath, List locations){
     
         JavaToJimple jtj = new JavaToJimple();
         polyglot.frontend.ExtensionInfo extInfo = jtj.initExtInfo(fullPath, locations);
         // only have one compiler - for memory issues
         if (compiler == null) {
 		    compiler = new polyglot.frontend.Compiler(extInfo);
         }
         // build ast
         astNode = jtj.compile(compiler, fullPath, extInfo);
     }
 
     /**
      * if you have a special AST set it here then call resolveFormJavaFile
      * on the soot class
      */ 
     public void setAst(polyglot.ast.Node ast) {
 	    astNode = ast;
     }
    
     /**
      *  adds source file tag to each sootclass
      */
     private void addSourceFileTag(soot.SootClass sc){
         if (sc.getTag("SourceFileTag") != null) return;
         String name = Util.getSourceFileOfClass(sc);
 
 
         if (classToSourceMap != null){
             if (classToSourceMap.containsKey(name)){
                 name = (String)classToSourceMap.get(name);
             }
         }
         //System.out.println("source file tag: "+name);
         // all classes may be in map 
         /*if (soot.SourceLocator.v().getSourceToClassMap() != null) {
             if (soot.SourceLocator.v().getSourceToClassMap().get(name) != null) {
                 name = (String)soot.SourceLocator.v().getSourceToClassMap().get(name);
             }
         }*/
 
         // add file extension
         //name += ".java";
         sc.addTag(new soot.tagkit.SourceFileTag(name));
     }
 
     
     /* get types and resolve them in the Scene
      * use a polyglot visitor to find all types in AST
      * for nested inner classes fix names (. -> $)
      * for local and anon find, invent names and resolve those
      */
     private void resolveTypes(){
         
         // get types and resolve them in the Scene
         TypeListBuilder typeListBuilder = new TypeListBuilder();
         
         astNode.visit(typeListBuilder);
         
 
         Iterator it = typeListBuilder.getList().iterator();
         while (it.hasNext()) {
             polyglot.types.Type type = (polyglot.types.Type)it.next();
             
             // ignore primitives
             if (type.isPrimitive()) continue;
               
             // ignore non class types
             if (!type.isClass()) continue;
                 
                
             polyglot.types.ClassType classType = (polyglot.types.ClassType)type;
             
             resolveClassType(classType);
             
         }
         
         
         // resolve Object, StringBuffer(used for string concat) and Throwable
         // (used for finally)
         SootResolver.v().assertResolvedClass("java.lang.Object");
         SootResolver.v().assertResolvedClass("java.lang.StringBuffer");
         SootResolver.v().assertResolvedClass("java.lang.Throwable");
         
     }
 
     /**
      * resolve class types - recursively resolving outer class if nec.
      */
     private void resolveClassType(polyglot.types.ClassType classType){
         
         soot.Type sootClassType;
             
 
         // fix class names of inner member classes
         if (classType.isNested()){
        
             resolveClassType(classType.outer());
         }
         
         if (classType.isLocal()) {
             resolveLocalClass(classType);
         }
                  
         if (classType.isAnonymous()) {
             resolveAnonClass(classType);
         }
 
         
         sootClassType = Util.getSootType(classType);
        
         if (classTypesFound == null){
             classTypesFound = new ArrayList();
         }
         classTypesFound.add(classType);
         // saves classnames mapped to AST 
         /*ClassDeclFinder finder = new ClassDeclFinder();
         finder.typeToFind(classType);
         astNode.visit(finder);
         if (finder.declFound() != null){
             addNameToAST(((soot.RefType)sootClassType).getClassName());
         }*/
         SootResolver.v().assertResolvedClassForType(sootClassType);
     }
 
     private ArrayList classTypesFound;
     private void makeASTMap(){
         ClassDeclFinder finder = new ClassDeclFinder();
         finder.typesToFind(classTypesFound);
         astNode.visit(finder);
         Iterator it = finder.declsFound().iterator();
         while (it.hasNext()){
             addNameToAST(Util.getSootType(((polyglot.ast.ClassDecl)it.next()).type()).toString());
         }
     }
     
     /**
      * add name to AST to map - used mostly for inner and non public
      * top-level classes
      */
     private void addNameToAST(String name){
         if (sootNameToAST == null){
             sootNameToAST = new HashMap();
         }   
         sootNameToAST.put(name, astNode);
     }
     
     // resolves all types and deals with .class literals and asserts
     public void resolveFromJavaFile(soot.SootClass sc) {
         sootClass = sc;
         //System.out.println("resolving sc: "+sc.getName()+" is interface: "+soot.Modifier.isInterface(sc.getModifiers()));
         // add sourcefile tag to Soot class
         //addSourceFileTag(sc);
         
         // get types and resolve them in the Scene
         resolveTypes();
 
         makeASTMap();
         // determine is ".class" literal is used
         /*ClassLiteralChecker classLitChecker = new ClassLiteralChecker();
         astNode.visit(classLitChecker);
         ArrayList classLitList = classLitChecker.getList();
         if (!classLitList.isEmpty()) {
             String methodName = "class$";
             soot.Type methodRetType = soot.RefType.v("java.lang.Class");
             ArrayList paramTypes = new ArrayList();
             paramTypes.add(soot.RefType.v("java.lang.String"));
             if (!sc.declaresMethod(methodName, paramTypes, methodRetType)){
                 soot.SootMethod sootMethod = new soot.SootMethod(methodName, paramTypes, methodRetType, soot.Modifier.STATIC);
                 ClassLiteralMethodSource mSrc = new ClassLiteralMethodSource();
                 sootMethod.setSource(mSrc);
                 sc.addMethod(sootMethod);
             }
         }
         Iterator classLitIt = classLitList.iterator();
         while (classLitIt.hasNext()) {
             polyglot.ast.ClassLit classLit = (polyglot.ast.ClassLit)classLitIt.next();
             // field
             String fieldName = "class$";
             String type = Util.getSootType(classLit.typeNode().type()).toString();
             type = soot.util.StringTools.replaceAll(type, ".", "$");
             fieldName = fieldName+type;
             soot.Type fieldType = soot.RefType.v("java.lang.Class");
             if (!sc.declaresField(fieldName, fieldType)){
                 soot.SootField sootField = new soot.SootField(fieldName, fieldType, soot.Modifier.STATIC);
                 sc.addField(sootField);
             }
 
         }*/
      
         // determine if assert is used
         /*AssertStmtChecker asc = new AssertStmtChecker();
         astNode.visit(asc);
 
         if (asc.isHasAssert()){
             handleAssert();
         }*/
         
         // create class to source map first 
         // create source file
         if (astNode instanceof polyglot.ast.SourceFile) {
             createClassToSourceMap((polyglot.ast.SourceFile)astNode);
             createSource((polyglot.ast.SourceFile)astNode);
         }
         
         addSourceFileTag(sc);
         
 
     }
 
     private void createClassToSourceMap(polyglot.ast.SourceFile src){
         //System.out.println("src source name: "+src.source().name());
         //System.out.println("src source path: "+src.source().path());
         //System.out.println("src package: "+src.package_());
        
        String srcName = src.source().path();
         String srcFileName = null;
         if (src.package_() != null){
             String slashedPkg = soot.util.StringTools.replaceAll(src.package_().package_().fullName(), ".", System.getProperty("file.separator"));
             //System.out.println("slashPkg: "+slashedPkg);
             srcFileName = srcName.substring(srcName.lastIndexOf(slashedPkg));
         }
         else {
             srcFileName = srcName.substring(srcName.lastIndexOf(System.getProperty("file.separator"))+1);
         }
 
         //System.out.println("srcFile name: "+srcFileName);
         //polyglot.ast.ClassDecl publicDecl = null;
         ArrayList list = new ArrayList();
         Iterator it = src.decls().iterator();
         while (it.hasNext()){
             polyglot.ast.ClassDecl nextDecl = (polyglot.ast.ClassDecl)it.next();
             /*if (nextDecl.flags().isPublic()){
                 publicDecl = nextDecl;
             }
             else {*/
             //    list.add(nextDecl);
             //System.out.println("adding to class to source map: "+nextDecl.type().toString()+" and "+srcFileName);
             addToClassToSourceMap(Util.getSootType(nextDecl.type()).toString(), srcFileName); 
             //}
         }
 
         /*Iterator it2 = list.iterator();
         while (it2.hasNext()){
             //addToClassToSourceMap(Util.getSootType(((polyglot.ast.ClassDecl)it2.next()).type()).toString(), Util.getSootType(publicDecl.type()).toString()); 
             addToClassToSourceMap(Util.getSootType(((polyglot.ast.ClassDecl)it2.next()).type()).toString(), srcFileName); 
         }*/
     }
 
 
     /**
      * Handling for assert stmts - extra fields and methods are needed
      * in the Jimple 
      */
     private void handleAssert(polyglot.ast.ClassBody cBody){
         AssertStmtChecker asc = new AssertStmtChecker();
         cBody.visit(asc);
         if (!asc.isHasAssert()) return;
         // two extra fields
         if (!sootClass.declaresField("$assertionsDisabled", soot.BooleanType.v())){
             sootClass.addField(new soot.SootField("$assertionsDisabled", soot.BooleanType.v(), soot.Modifier.STATIC | soot.Modifier.FINAL));
         }
         //System.out.println("adding class$ field to : "+sootClass+" from assert");
         if (!sootClass.declaresField("class$"+sootClass.getName(), soot.RefType.v("java.lang.Class"))){
             sootClass.addField(new soot.SootField("class$"+sootClass.getName(), soot.RefType.v("java.lang.Class"), soot.Modifier.STATIC));
         }
         // two extra methods
         String methodName = "class$";
         soot.Type methodRetType = soot.RefType.v("java.lang.Class");
         ArrayList paramTypes = new ArrayList();
         paramTypes.add(soot.RefType.v("java.lang.String"));
         if (!sootClass.declaresMethod(methodName, paramTypes, methodRetType)){
             soot.SootMethod sootMethod = new soot.SootMethod(methodName, paramTypes, methodRetType, soot.Modifier.STATIC);
             AssertClassMethodSource mSrc = new AssertClassMethodSource();
             sootMethod.setSource(mSrc);
             sootClass.addMethod(sootMethod);
         }
         methodName = "<clinit>";
         methodRetType = soot.VoidType.v();
         paramTypes = new ArrayList();
         if (!sootClass.declaresMethod(methodName, paramTypes, methodRetType)){
             soot.SootMethod sootMethod = new soot.SootMethod(methodName, paramTypes, methodRetType, soot.Modifier.STATIC);
             PolyglotMethodSource mSrc = new PolyglotMethodSource();
             mSrc.hasAssert(true);
             sootMethod.setSource(mSrc);
             sootClass.addMethod(sootMethod);
         }
         else {
             ((soot.javaToJimple.PolyglotMethodSource)sootClass.getMethod(methodName, paramTypes, methodRetType).getSource()).hasAssert(true);
         }
     }
 
     private int getNextAnonNum(){
         if (anonTypeMap == null) return 1;
         else return anonTypeMap.size()+1;
     }
     
     private void handleClassLiteral(polyglot.ast.ClassBody cBody){
        
         ClassLiteralChecker classLitChecker = new ClassLiteralChecker();
         cBody.visit(classLitChecker);
         ArrayList classLitList = classLitChecker.getList();
         //System.out.println("current class: "+sootClass);
         //System.out.println("class lit list: "+classLitList);
         String specialClassName = null;    
         if (!classLitList.isEmpty()) {
             String methodName = "class$";
             soot.Type methodRetType = soot.RefType.v("java.lang.Class");
             ArrayList paramTypes = new ArrayList();
             paramTypes.add(soot.RefType.v("java.lang.String"));
             soot.SootMethod sootMethod = new soot.SootMethod(methodName, paramTypes, methodRetType, soot.Modifier.STATIC);
             ClassLiteralMethodSource mSrc = new ClassLiteralMethodSource();
             sootMethod.setSource(mSrc);
             if (sootClass.isInterface()) {
                 // have to create a I$1 class
                 
                 specialClassName = sootClass.getName()+"$"+getNextAnonNum();    
                 addNameToAST(specialClassName);
                 soot.SootResolver.v().assertResolvedClass(specialClassName);    
                 // add meth to newly created class not this current one
                 soot.SootClass specialClass = soot.Scene.v().getSootClass(specialClassName);
                 if (specialAnonMap == null){
                     specialAnonMap = new HashMap();
                 }
                 specialAnonMap.put(sootClass, specialClass);
                 
                 if (!specialClass.declaresMethod(methodName, paramTypes, methodRetType)){
                     specialClass.addMethod(sootMethod);
                 }
             
             }
             else {
                 if (!sootClass.declaresMethod(methodName, paramTypes, methodRetType)){
                     sootClass.addMethod(sootMethod);
                 }
             }
         }
         Iterator classLitIt = classLitList.iterator();
         while (classLitIt.hasNext()) {
             polyglot.ast.ClassLit classLit = (polyglot.ast.ClassLit)classLitIt.next();
             //System.out.println("next class lit: "+classLit);
 
             // field
             String fieldName = "class$";
             String type = Util.getSootType(classLit.typeNode().type()).toString();
             type = soot.util.StringTools.replaceAll(type, ".", "$");
             fieldName = fieldName+type;
             soot.Type fieldType = soot.RefType.v("java.lang.Class");
             soot.SootField sootField = new soot.SootField(fieldName, fieldType, soot.Modifier.STATIC);
             if (sootClass.isInterface()){
                 soot.SootClass specialClass = soot.Scene.v().getSootClass(specialClassName);
                 //System.out.println("special class fields before: "+specialClass.getFields());
                 if (!specialClass.declaresField(fieldName, fieldType)){
                     specialClass.addField(sootField);
                 }
                 //System.out.println("special class fields after: "+specialClass.getFields());
             }
             else {
                 if (!sootClass.declaresField(fieldName, fieldType)){
                     sootClass.addField(sootField);
                 }
             }
 
         }
     
     }
   
     private void resolveAnonClass(polyglot.types.ClassType type){
         NewFinder finder = new NewFinder();
         finder.typeToFind(type);
         astNode.visit(finder);
         if (finder.newFound() == null){
             throw new RuntimeException("Couldn't find New corresponding to the anon class type: "+type);
         }
         // maybe this anon has already been resolved
         if (anonClassMap == null){
             anonClassMap = new BiMap();
         }
         if (anonTypeMap == null){
             anonTypeMap = new HashMap();
         }
         if (!anonClassMap.containsKey(finder.newFound())){
             int nextAvailNum = 1;
             polyglot.types.ClassType outerToMatch = type.outer();
             while (outerToMatch.isNested()){
                 outerToMatch = outerToMatch.outer();
             }
 
             if (!anonTypeMap.isEmpty()){
                 Iterator matchIt = anonTypeMap.keySet().iterator();
                 while (matchIt.hasNext()){
                     polyglot.types.ClassType pType = (polyglot.types.ClassType)((polyglot.util.IdentityKey)matchIt.next()).object();
                     polyglot.types.ClassType outerMatch = pType.outer();
                     while (outerMatch.isNested()){
                         outerMatch = outerMatch.outer();
                     }
                     if (outerMatch.equals(outerToMatch)){
                         int numFound = getAnonClassNum((String)anonTypeMap.get(new polyglot.util.IdentityKey(pType)));
                         if (numFound >= nextAvailNum){
                             nextAvailNum = numFound+1;
                         }
                     }
                 }
             }
             
             String realName = outerToMatch.fullName()+"$"+nextAvailNum;
             anonClassMap.put(finder.newFound(), realName);
             anonTypeMap.put(new polyglot.util.IdentityKey(type), realName);
             addNameToAST(realName);
             soot.SootResolver.v().assertResolvedClass(realName);
             
         }
     }
     
     private void resolveLocalClass(polyglot.types.ClassType type){
         LocalClassDeclFinder finder = new LocalClassDeclFinder();
         finder.typeToFind(type);
         astNode.visit(finder);
         if (finder.declFound() == null) {
             throw new RuntimeException("Couldn't find LocalClassDecl corresponding to the local class type: "+type);
         }
         // maybe this localdecl has already been resolved 
         if (localClassMap == null){
             localClassMap = new BiMap();
         }
         if (localTypeMap == null){
             localTypeMap = new HashMap();
         }
         
         if (!localClassMap.containsKey(finder.declFound())){
             int nextAvailNum = 1;
             polyglot.types.ClassType outerToMatch = type.outer();
             while (outerToMatch.isNested()){
                 outerToMatch = outerToMatch.outer();
             }
 
             if (!localTypeMap.isEmpty()){
                 Iterator matchIt = localTypeMap.keySet().iterator();
                 while (matchIt.hasNext()){
                     polyglot.types.ClassType pType = (polyglot.types.ClassType)((polyglot.util.IdentityKey)matchIt.next()).object();
                     polyglot.types.ClassType outerMatch = pType.outer();
                     while (outerMatch.isNested()){
                         outerMatch = outerMatch.outer();
                     }
                     if (outerMatch.equals(outerToMatch)){
                         int numFound = getLocalClassNum((String)localTypeMap.get(new polyglot.util.IdentityKey(pType)), finder.declFound().decl().name());
                         if (numFound >= nextAvailNum){
                             nextAvailNum = numFound+1;
                         }
                     }
                 }
             }
 
             String realName = outerToMatch.fullName()+"$"+nextAvailNum+finder.declFound().decl().name();
             localClassMap.put(finder.declFound(), realName);
             localTypeMap.put(new polyglot.util.IdentityKey(type), realName);
             addNameToAST(realName);
             soot.SootResolver.v().assertResolvedClass(realName);
         }
     }
 
     private static final int NO_MATCH = 0;
     
     private int getLocalClassNum(String realName, String simpleName){
         // a local inner class is named outer$NsimpleName where outer 
         // is the very outer most class
         //System.out.println("realName: "+realName);
         //System.out.println("simpleName: "+simpleName);
         int dIndex = realName.indexOf("$");
         int nIndex = realName.indexOf(simpleName, dIndex);
         //System.out.println("dIndex: "+dIndex+" nIndex: "+nIndex);
         if (nIndex == -1) return NO_MATCH;
         if (dIndex == -1) {
             throw new RuntimeException("Matching an incorrectly named local inner class: "+realName);
         }
         return (new Integer(realName.substring(dIndex+1, nIndex))).intValue();
     }
     
     private int getAnonClassNum(String realName){
         // a anon inner class is named outer$N where outer 
         // is the very outer most class
         //System.out.println("realName: "+realName);
         int dIndex = realName.indexOf("$");
         if (dIndex == -1) {
             throw new RuntimeException("Matching an incorrectly named anon inner class: "+realName);
         }
         return (new Integer(realName.substring(dIndex+1))).intValue();
     }
     
 
     /**
      * returns the name of the class without the package part
      */
     private String getSimpleClassName(){
         String name = sootClass.getName();
         if (sootClass.getPackageName() != null){
             name = name.substring(name.lastIndexOf(".")+1, name.length());
         }
         return name;
     }
     
     /**
      * Source Creation 
      */
     private void createSource(polyglot.ast.SourceFile source){
        
         String simpleName = sootClass.getName();
         
         Iterator declsIt = source.decls().iterator();
         boolean found = false;
 
         // first look in top-level decls
 		while (declsIt.hasNext()){
 			Object next = declsIt.next();
 			if (next instanceof polyglot.ast.ClassDecl) {
                 polyglot.types.ClassType nextType = ((polyglot.ast.ClassDecl)next).type();
                 if (Util.getSootType(nextType).equals(sootClass.getType())){
 				    createClassDecl((polyglot.ast.ClassDecl)next);
                     found = true;
                 }
                 /*else {
                     // if not already there put cdecl name in class to source file map
                     // its actually a map from class names to the corresponding source file
                     if (((polyglot.ast.ClassDecl)next).type().isTopLevel() && !((polyglot.ast.ClassDecl)next).flags().isPublic()){                    
                         if (sootClass.getName().indexOf("$") == -1){
                           
                             addToClassToSourceMap(((polyglot.ast.ClassDecl)next).type().fullName(), sootClass.getName());                
                         }
                     }
                 }*/
 		    }
 		}
 
         // if the class wasn't a top level then its nested, local or anon
         if (!found) {
             NestedClassListBuilder nestedClassBuilder = new NestedClassListBuilder();
             source.visit(nestedClassBuilder);
             
             Iterator nestedDeclsIt = nestedClassBuilder.getClassDeclsList().iterator();
             while (nestedDeclsIt.hasNext() && !found){
                 
                 polyglot.ast.ClassDecl nextDecl = (polyglot.ast.ClassDecl)nestedDeclsIt.next();
                 polyglot.types.ClassType type = (polyglot.types.ClassType)nextDecl.type();
                 if (type.isLocal() && !type.isAnonymous()) {
                    
                     if (localClassMap.containsVal(simpleName)){
                         createClassDecl(((polyglot.ast.LocalClassDecl)localClassMap.getKey(simpleName)).decl());
                         found = true;
                     }
                 }
                 else {
                
                     if (Util.getSootType(type).equals(sootClass.getType())){
                         createClassDecl(nextDecl);
                         found = true;
                     }
                 }
             }
 
             if (!found) {
                 // assume its anon class (only option left) 
                 //
                 if ((anonClassMap != null) && anonClassMap.containsVal(simpleName)){
                     
                     polyglot.ast.New aNew = (polyglot.ast.New)anonClassMap.getKey(simpleName);
                     createAnonClassDecl(aNew);
                     createClassBody(aNew.body());
                     handleFieldInits();
 
                 }                    
                 else {
                     // could be an anon class that was created out of thin air 
                     // for handling class lits in interfaces
                     sootClass.setSuperclass(soot.Scene.v().getSootClass("java.lang.Object"));
                 }
             }
         }
 
     }
 
     /**
      * ClassToSourceMap is for classes whos names don't match the source file
      * name - ex: multiple top level classes in a single file
      */
     private void addToClassToSourceMap(String className, String sourceName) {
             
         if (classToSourceMap == null){
             classToSourceMap = new HashMap();
         }
         classToSourceMap.put(className, sourceName);
         /*if (sourceToClassMap == null) {
             sourceToClassMap = new HashMap();
         }
             
         if (soot.SourceLocator.v().getSourceToClassMap() == null) {
             soot.SourceLocator.v().setSourceToClassMap(sourceToClassMap);
         }
             
         if (!soot.SourceLocator.v().getSourceToClassMap().containsKey(className)) {
             System.out.println("adding to source to class map class: "+className+" src: "+sourceName);
             soot.SourceLocator.v().addToSourceToClassMap(className, sourceName);
         }*/
     }
     
     /**
      * creates the Jimple for an anon class - in the AST there is no class 
      * decl for anon classes - the revelant fields and methods are 
      * created 
      */
     private void createAnonClassDecl(polyglot.ast.New aNew) {
        
         //System.out.println("creating anonn class decl: "+Util.getSootType(aNew.anonType()));
         soot.SootClass typeClass = ((soot.RefType)Util.getSootType(aNew.objectType().type())).getSootClass();
        
         // set superclass
         if (((polyglot.types.ClassType)aNew.objectType().type()).flags().isInterface()){
         //if (typeClass.isInterface()){
             sootClass.addInterface(typeClass);
             sootClass.setSuperclass(soot.Scene.v().getSootClass("java.lang.Object"));
         }
         else {
             sootClass.setSuperclass(typeClass);
         }
 
         // needs to be done for local also
         ArrayList params = new ArrayList();
             
         soot.SootMethod method;
         // if interface there are no extra params
         if (((polyglot.types.ClassType)aNew.objectType().type()).flags().isInterface()){
         //if (typeClass.isInterface()){
             method = new soot.SootMethod("<init>", params, soot.VoidType.v());
         }
         else {
             Iterator aIt = aNew.arguments().iterator();
             while (aIt.hasNext()){
                 polyglot.types.Type pType = ((polyglot.ast.Expr)aIt.next()).type();
                 params.add(Util.getSootType(pType));
             }
             method = new soot.SootMethod("<init>", params, soot.VoidType.v());
         }
         
         AnonClassInitMethodSource src = new AnonClassInitMethodSource();
         method.setSource(src);
         sootClass.addMethod(method);
    
         AnonLocalClassInfo info = (AnonLocalClassInfo)finalLocalInfo.get(new polyglot.util.IdentityKey(aNew.anonType()));
        
         //System.out.println("new : "+aNew);
         if (!info.inStaticMethod()){
             addOuterClassThisRefToInit(aNew.anonType().outer());
             addOuterClassThisRefField(aNew.anonType().outer());
             src.thisOuterType(Util.getSootType(aNew.anonType().outer()));
         }
         else if (aNew.qualifier() != null) {
             // add outer class ref
             addOuterClassThisRefToInit(aNew.qualifier().type());
             addOuterClassThisRefField(aNew.qualifier().type());
             src.thisOuterType(Util.getSootType(aNew.qualifier().type()));
             
         }
         
         src.inStaticMethod(info.inStaticMethod());
         //System.out.println("creating anon info: "+info);
         if (info != null){
             //System.out.println("want to add finals for : "+Util.getSootType(aNew.anonType()));
             src.setFinalsList(addFinalLocals(aNew.body(), info.finalLocals(), (polyglot.types.ClassType)aNew.anonType(), info));
         }
         src.outerClassType(Util.getSootType(aNew.anonType().outer()));
         if (((polyglot.types.ClassType)aNew.objectType().type()).isNested()){
             src.superOuterType(Util.getSootType(((polyglot.types.ClassType)aNew.objectType().type()).outer()));
             src.isSubType(Util.isSubType(aNew.anonType().outer(), ((polyglot.types.ClassType)aNew.objectType().type()).outer())); 
         }
     }
         
     private ArrayList addFinalLocals(polyglot.ast.ClassBody cBody, ArrayList finalLocals, polyglot.types.ClassType nodeKeyType, AnonLocalClassInfo info){
         ArrayList finalFields = new ArrayList();
         
         LocalUsesChecker luc = new LocalUsesChecker();
         cBody.visit(luc);
         Iterator localsNeededIt = luc.getLocals().iterator();
         //System.out.println("locals Needed: "+luc.getLocals());
         //System.out.println("locals avail: "+finalLocals);
         ArrayList localsUsed = new ArrayList();
         while (localsNeededIt.hasNext()){
             polyglot.types.LocalInstance li = (polyglot.types.LocalInstance)((polyglot.util.IdentityKey)localsNeededIt.next()).object();
             if (finalLocals.contains(new polyglot.util.IdentityKey(li))){
                 
                 // add as param for init
                 Iterator it = sootClass.getMethods().iterator();
                 while (it.hasNext()){
                     soot.SootMethod meth = (soot.SootMethod)it.next();
                     if (meth.getName().equals("<init>")){
                         meth.getParameterTypes().add(Util.getSootType(li.type()));
                     }
                 }
                 
                 // add field
                 soot.SootField sf = new soot.SootField("val$"+li.name(), Util.getSootType(li.type()), soot.Modifier.FINAL | soot.Modifier.PRIVATE);
                 sootClass.addField(sf);
                 finalFields.add(sf);
                 //System.out.println("added field: "+sf.getName()+" to class: "+sootClass.getName());
                 
                 localsUsed.add(new polyglot.util.IdentityKey(li));
             }
         }
     
         //System.out.println("locals Used: "+localsUsed);
         info.finalLocals(localsUsed);
         finalLocalInfo.put(new polyglot.util.IdentityKey(nodeKeyType), info);
         return finalFields;
     }
     
     /**
      * Class Declaration Creation
      */
     private void createClassDecl(polyglot.ast.ClassDecl cDecl){
        
         // modifiers
         polyglot.types.Flags flags = cDecl.flags();
         addModifiers(flags, cDecl);
 	    
         // super class
         if (cDecl.superClass() == null) {
 			soot.SootClass superClass = soot.Scene.v().getSootClass ("java.lang.Object"); 
 			sootClass.setSuperclass(superClass);
 		}
 		else {
 
             sootClass.setSuperclass(((soot.RefType)Util.getSootType(cDecl.superClass().type())).getSootClass());
 		
 		}
 
        
         // implements 
         Iterator interfacesIt = cDecl.interfaces().iterator();
         while (interfacesIt.hasNext()) {
             polyglot.ast.TypeNode next = (polyglot.ast.TypeNode)interfacesIt.next();
             //sootClass.addInterface(soot.Scene.v().getSootClass(next.toString()));
             sootClass.addInterface(((soot.RefType)Util.getSootType(next.type())).getSootClass());
         }
 	    
         currentClassDeclPos = cDecl.position();
 		createClassBody(cDecl.body());
 
         // handle initialization of fields 
         // static fields init in clinit
         // other fields init in init
         handleFieldInits();
         
         if ((staticFieldInits != null) || (staticInitializerBlocks != null)) {
             soot.SootMethod clinitMethod;
             if (!sootClass.declaresMethod("<clinit>", new ArrayList(), soot.VoidType.v())) {
                 clinitMethod = new soot.SootMethod("<clinit>", new ArrayList(), soot.VoidType.v(), soot.Modifier.STATIC, new ArrayList());
                 
                 sootClass.addMethod(clinitMethod);
                 clinitMethod.setSource(new soot.javaToJimple.PolyglotMethodSource());
             }
             else {
                 clinitMethod = sootClass.getMethod("<clinit>", new ArrayList(), soot.VoidType.v());
             
             }
             ((PolyglotMethodSource)clinitMethod.getSource()).setStaticFieldInits(staticFieldInits);
             ((PolyglotMethodSource)clinitMethod.getSource()).setStaticInitializerBlocks(staticInitializerBlocks);
 
             /*Iterator it = sootClass.getMethods().iterator();
             while (it.hasNext()){
                 ((PolyglotMethodSource)((soot.SootMethod)it.next()).getSource()).setStaticFieldInits(staticFieldInits);
             }*/
         }
 
        
         // add final locals to local inner classes inits
         if (cDecl.type().isLocal()) {
             //System.out.println("finalLocalInfo: "+finalLocalInfo);
             AnonLocalClassInfo info = (AnonLocalClassInfo)finalLocalInfo.get(new polyglot.util.IdentityKey(cDecl.type()));
                 ArrayList finalsList = addFinalLocals(cDecl.body(), info.finalLocals(), cDecl.type(), info); 
                 Iterator it = sootClass.getMethods().iterator();
                 while (it.hasNext()){
                     soot.SootMethod meth = (soot.SootMethod)it.next();
                     if (meth.getName().equals("<init>")){
                         ((PolyglotMethodSource)meth.getSource()).setFinalsList(finalsList);
                     }
                 }
             if (!info.inStaticMethod()){
                 polyglot.types.ClassType outerType = cDecl.type().outer();
                 addOuterClassThisRefToInit(outerType);
                 addOuterClassThisRefField(outerType);
             }
         }
         
         // add outer class ref to constructors of inner classes
         // and out class field ref (only for non-static inner classes
         else if (cDecl.type().isNested() && !cDecl.flags().isStatic()) {
             polyglot.types.ClassType outerType = cDecl.type().outer();
             addOuterClassThisRefToInit(outerType);
             addOuterClassThisRefField(outerType);
         }
         
         Util.addLineTag(sootClass, cDecl);
 	}
 
     private void handleFieldInits(){
         //System.out.println("field inits: "+fieldInits+" for class: "+sootClass);
         if ((fieldInits != null) || (initializerBlocks != null)) {
             Iterator methodsIt = sootClass.getMethods().iterator();
             while (methodsIt.hasNext()) {
                 soot.SootMethod next = (soot.SootMethod)methodsIt.next();
                 if (next.getName().equals("<init>")){
                
                     //if (next.getSource() instanceof soot.javaToJimple.PolyglotMethodSource){
                    //System.out.println("setting fieldInits: "+fieldInits+" for meth: "+next);
                         soot.javaToJimple.PolyglotMethodSource src = (soot.javaToJimple.PolyglotMethodSource)next.getSource();
                         src.setInitializerBlocks(initializerBlocks);
                         src.setFieldInits(fieldInits);
           
                     //}
                     /*else if (next.getSource() instanceof soot.javaToJimple.AnonClassInitMethodSource){
                         soot.javaToJimple.AnonClassInitMethodSource src = (soot.javaToJimple.AnonClassInitMethodSource)next.getSource();
                         src.initBlocks(initializerBlocks);
                         src.fieldInits(fieldInits);
                     }*/       
                 }
             }
         }
         
     }
     
     private void addOuterClassThisRefToInit(polyglot.types.Type outerType){
         soot.Type outerSootType = Util.getSootType(outerType);
         Iterator it = sootClass.getMethods().iterator();
         while (it.hasNext()){
             soot.SootMethod meth = (soot.SootMethod)it.next();
             if (meth.getName().equals("<init>")){
                 meth.getParameterTypes().add(0, outerSootType);
                 if (hasOuterRefInInit == null){
                     hasOuterRefInInit = new ArrayList();
                 }
                 //System.out.println("actually adding outer class this to init for: "+meth.getDeclaringClass().getType());
                 //System.out.println("methods: "+meth.getDeclaringClass().getMethods());
                 hasOuterRefInInit.add(meth.getDeclaringClass().getType());
             }
         }
     }
     
     private void addOuterClassThisRefField(polyglot.types.Type outerType){
         soot.Type outerSootType = Util.getSootType(outerType);
         soot.SootField field = new soot.SootField("this$0", outerSootType, soot.Modifier.PRIVATE | soot.Modifier.FINAL);
         sootClass.addField(field);
     }
     
    
     /**
      * adds modifiers
      */
 	private void addModifiers(polyglot.types.Flags flags, polyglot.ast.ClassDecl cDecl){
 		int modifiers = 0;
         if (cDecl.type().isNested()){
             if (flags.isPublic() || flags.isProtected() || flags.isPrivate()){
                 modifiers = soot.Modifier.PUBLIC;
             }
             if (flags.isInterface()){
                 modifiers = modifiers | soot.Modifier.INTERFACE;
             }
             // if inner classes are declared in an interface they need to be
             // given public access but I have no idea why
             if (cDecl.type().outer().flags().isInterface()){
                 modifiers = soot.Modifier.PUBLIC;
             }
         }
         else {
 		    modifiers = Util.getModifier(flags);
         }
 		sootClass.setModifiers(modifiers);
 	}
 	
     /**
      * Class Body Creation
      */
     private void createClassBody(polyglot.ast.ClassBody classBody){
         
         // reinit static lists
         staticFieldInits = null;
         fieldInits = null;
         initializerBlocks = null;
         staticInitializerBlocks = null;
       
         handleClassLiteral(classBody);
         handleAssert(classBody);
         
         // handle members
         Iterator it = classBody.members().iterator();
 		while (it.hasNext()){
 			Object next = it.next();
 			
 			if (next instanceof polyglot.ast.MethodDecl) {
 				createMethodDecl((polyglot.ast.MethodDecl)next);
 			}
 			else if (next instanceof polyglot.ast.FieldDecl) {
 				createFieldDecl((polyglot.ast.FieldDecl)next);
             }
 			else if (next instanceof polyglot.ast.ConstructorDecl){
                 createConstructorDecl((polyglot.ast.ConstructorDecl)next);
 			}
 			else if (next instanceof polyglot.ast.ClassDecl){
 			}
             else if (next instanceof polyglot.ast.Initializer) {
                 createInitializer((polyglot.ast.Initializer)next);
             }
             else {
                 throw new RuntimeException("Class Body Member not implemented");
 			}
         }
         handlePrivateAccessors(classBody);
     }
    
     /**
      * inner classes can access private fields and methods of the
      * outer class and special methods are created in order
      * to make this possible
      */
     private void handlePrivateAccessors(polyglot.ast.ClassBody cBody) {
         // determine and create acces methods for used private access
         // in inner classes
                
         ArrayList privateAccessList = new ArrayList();
         ArrayList uses = new ArrayList();
       
         // look through body for private field and procedure decls
         PrivateInstancesAvailable privateInsts = new PrivateInstancesAvailable();
         cBody.visit(privateInsts);
 
     
         // look through body again for all inner class bodies
         InnerClassBodies icb = new InnerClassBodies();
         cBody.visit(icb);
         
                
         // look through each body and see if they use a private instance
         Iterator cbIt = icb.getList().iterator();
         while (cbIt.hasNext()){
             polyglot.ast.ClassBody cb = (polyglot.ast.ClassBody)cbIt.next();
             PrivateAccessUses pau = new PrivateAccessUses();
             pau.avail(privateInsts.getList());
             cb.visit(pau);
 
             uses.addAll(pau.getList());
         }
            
         Iterator listIt = uses.iterator();
         while (listIt.hasNext()) {
             Object nextInst = ((polyglot.util.IdentityKey)listIt.next()).object();
             if (nextInst instanceof polyglot.types.FieldInstance) {
                 if (Util.getSootType(((polyglot.types.FieldInstance)nextInst).container()).equals(sootClass.getType())){
                     privateAccessList.add(nextInst);
                 }
             }
             if (nextInst instanceof polyglot.types.MethodInstance) {
                 if (Util.getSootType(((polyglot.types.MethodInstance)nextInst).container()).equals(sootClass.getType())){
                     privateAccessList.add(nextInst);
                 }
                       
             }
         }
 
         Iterator it = privateAccessList.iterator();
         while (it.hasNext()) {
             polyglot.types.MemberInstance inst = (polyglot.types.MemberInstance)it.next();
             String name = "access$"+privateAccessCounter+"00";
             
             ArrayList paramTypesList = new ArrayList();
             if (inst instanceof polyglot.types.MethodInstance) {
                 Iterator paramsIt = ((polyglot.types.MethodInstance)inst).formalTypes().iterator();
                 while (paramsIt.hasNext()) {
                     paramTypesList.add(Util.getSootType((polyglot.types.Type)paramsIt.next()));
                 }
             }
             if (!inst.flags().isStatic()) {
                 paramTypesList.add(sootClass.getType());
             }
             
             
             soot.Type returnType = null;
             if (inst instanceof polyglot.types.MethodInstance) {
                 returnType = Util.getSootType(((polyglot.types.MethodInstance)inst).returnType());    
             }
             else {
                 returnType = Util.getSootType(((polyglot.types.FieldInstance)inst).type());
             }
             
             soot.SootMethod accessMeth = new soot.SootMethod(name, paramTypesList, returnType, soot.Modifier.STATIC);
 
             if (inst instanceof polyglot.types.MethodInstance) {
                 PrivateMethodAccMethodSource pmams = new PrivateMethodAccMethodSource();
                 pmams.setMethodInst((polyglot.types.MethodInstance)inst);
                 /*ArrayList formalTypes = new ArrayList();
                 Iterator fIt = ((polyglot.types.MethodInstance)inst).formalTypes().iterator();
                 while (fIt.hasNext()){
                     formalTypes.add(Util.getSootType((polyglot.types.Type)fIt.next()));
                 }
                 pmams.formalTypes(formalTypes);
                 pmams.returnType(Util.getSootType(((polyglot.types.MethodInstance)inst).returnType()));
                 pmams.name(((polyglot.types.MethodInstance)inst).name());
                 pmams.flags(Util.getModifier(((polyglot.types.MethodInstance)inst).flags()));*/
                 accessMeth.setSource(pmams);
             }
             else {
                 PrivateFieldAccMethodSource pfams = new PrivateFieldAccMethodSource();
                 pfams.fieldName(((polyglot.types.FieldInstance)inst).name());
                 pfams.fieldType(Util.getSootType(((polyglot.types.FieldInstance)inst).type()));
                 pfams.classToInvoke(((soot.RefType)Util.getSootType(((polyglot.types.FieldInstance)inst).container())).getSootClass());
                 accessMeth.setSource(pfams);
             }
 
             sootClass.addMethod(accessMeth);
             if (privateAccessMap == null){
                 privateAccessMap = new HashMap();
             }
             privateAccessMap.put(inst, accessMeth);
             privateAccessCounter++;
         }
     }
 
 	/**
      * Procedure Declaration Helper Methods
      * creates procedure name
      */
     private String createName(polyglot.ast.ProcedureDecl procedure) {
         return procedure.name();
     }
 
     /**
      * creates soot params from polyglot formals
      */
     private ArrayList createParameters(polyglot.ast.ProcedureDecl procedure) {
 		ArrayList parameters = new ArrayList();
 		Iterator formalsIt = procedure.formals().iterator();
 		while (formalsIt.hasNext()){
 			polyglot.ast.Formal next = (polyglot.ast.Formal)formalsIt.next();
             parameters.add(Util.getSootType(next.type().type()));
 		}
         return parameters;
     }
     
     /**
      * creates soot exceptions from polyglot throws
      */
     private ArrayList createExceptions(polyglot.ast.ProcedureDecl procedure) {
 		ArrayList exceptions = new ArrayList();
 		Iterator throwsIt = procedure.throwTypes().iterator();
 		while (throwsIt.hasNext()){
             polyglot.types.Type throwType = ((polyglot.ast.TypeNode)throwsIt.next()).type();
             exceptions.add(((soot.RefType)Util.getSootType(throwType)).getSootClass());
 		}
         return exceptions; 
     }
     
     /**
      * looks after pos tags for methods and constructors
      */
     private void finishProcedure(polyglot.ast.ProcedureDecl procedure, soot.SootMethod sootMethod){
         
 		addProcedureToClass(sootMethod);
 	
         if (procedure.position() != null){
                 if (procedure.body() != null) {
                     if (procedure.body().position() != null) {
                         Util.addLnPosTags(sootMethod, procedure.position().line(), procedure.body().position().endLine(), procedure.position().column(), procedure.body().position().endColumn());
                     }
                 }
                 
         }
 
         //handle final local map for local and anon classes
         handleFinalLocals(procedure);
         /*MethodFinalsChecker mfc = new MethodFinalsChecker();
         procedure.visit(mfc);
         AnonLocalClassInfo alci = new AnonLocalClassInfo();
         if (mem instanceof polyglot.ast.ProcedureDecl){
             polyglot.ast.ProcedureDecl procedure = (polyglot.ast.ProcedureDecl)mem;
             alci.finalLocals(getFinalLocalsAvail(procedure));
             if (procedure.flags().isStatic()){
                 alci.inStaticMethod(true);
             }
         }
         //System.out.println("alci creation: "+mfc.finalLocals());
         //if (soot.Modifier.isStatic(sootMethod.getModifiers())){
         if (
         if (memsoot.Modifier.isStatic(sootMethod.getModifiers())){
             alci.inStaticMethod(true);
         }
         if (finalLocalInfo == null){
             finalLocalInfo = new HashMap();
         }
         Iterator it = mfc.inners().iterator();
         while (it.hasNext()){
             AnonLocalClassInfo info = new AnonLocalClassInfo();
             info.inStaticMethod(alci.inStaticMethod());
             info.finalLocals(alci.finalLocals());
             finalLocalInfo.put(it.next(), info);
         }*/
        
         //System.out.println("finalLocalInfo: "+finalLocalInfo);
 
         PolyglotMethodSource mSrc = new PolyglotMethodSource(procedure.body(), procedure.formals());
         mSrc.setPrivateAccessMap(privateAccessMap);
 
         
         sootMethod.setSource(mSrc);
         
 	}
 
     private void handleFinalLocals(polyglot.ast.ClassMember member){
         MethodFinalsChecker mfc = new MethodFinalsChecker();
         member.visit(mfc);
         AnonLocalClassInfo alci = new AnonLocalClassInfo();
         //System.out.println("member: "+member+" alci creation: "+mfc.finalLocals());
         if (member instanceof polyglot.ast.ProcedureDecl){
             polyglot.ast.ProcedureDecl procedure = (polyglot.ast.ProcedureDecl)member;
             //alci.finalLocals(getFinalLocalsAvail(procedure));
             // not sure if this will break deep nesting
             alci.finalLocals(mfc.finalLocals());
             //System.out.println("meth: "+procedure.name()+" staticness: "+procedure.flags().isStatic());
             if (procedure.flags().isStatic()){
                 alci.inStaticMethod(true);
                 //System.out.println("method is static");
             }
         }
         else if (member instanceof polyglot.ast.FieldDecl){
             alci.finalLocals(new ArrayList());
             //System.out.println("field: "+member);
             if (((polyglot.ast.FieldDecl)member).flags().isStatic()){
                 alci.inStaticMethod(true);
             }
         }
         else if (member instanceof polyglot.ast.Initializer){
             // for now don't make final locals avail in init blocks
             // need to test this
             //System.out.println("static: "+member);
             //alci.finalLocals(getFinalLocalsAvail(member));
             // 
             alci.finalLocals(mfc.finalLocals());
             //System.out.println("meth: "+procedure.name()+" staticness: "+procedure.flags().isStatic());
             if (((polyglot.ast.Initializer)member).flags().isStatic()){
                 alci.inStaticMethod(true);
             }
         }
         if (finalLocalInfo == null){
             finalLocalInfo = new HashMap();
         }
         Iterator it = mfc.inners().iterator();
         while (it.hasNext()){
             
             polyglot.types.ClassType cType = (polyglot.types.ClassType)((polyglot.util.IdentityKey)it.next()).object();
             //System.out.println("alci for type: "+Util.getSootType(cType));
             AnonLocalClassInfo info = new AnonLocalClassInfo();
             //System.out.println("adding alci.inStaticMethod(): "+alci.inStaticMethod());
             //System.out.println("anon type map: "+anonTypeMap);
             info.inStaticMethod(alci.inStaticMethod());
             info.finalLocals(alci.finalLocals());
             //System.out.println("info on add to map: "+info);
             finalLocalInfo.put(new polyglot.util.IdentityKey(cType), info);
         }
     }
 
     private ArrayList getFinalLocalsAvail(polyglot.ast.ClassMember member){
         ArrayList finalsAvail = new ArrayList();
         if (member instanceof polyglot.ast.ProcedureDecl){
             polyglot.ast.ProcedureDecl proc = (polyglot.ast.ProcedureDecl)member;
             if (proc.formals() != null){
                 Iterator formalsIt = proc.formals().iterator();
                 while (formalsIt.hasNext()){
                     polyglot.ast.Formal formal = (polyglot.ast.Formal)formalsIt.next();
                     if (formal.localInstance().flags().isFinal()){
                         finalsAvail.add(new polyglot.util.IdentityKey(formal.localInstance()));
                     }
                 }
                 
             }
         }
         polyglot.ast.Block fBody;
         if (member instanceof polyglot.ast.ProcedureDecl){
             polyglot.ast.ProcedureDecl proc = (polyglot.ast.ProcedureDecl)member;
             fBody = proc.body();
         }
         else if (member instanceof polyglot.ast.Initializer){
             polyglot.ast.Initializer initializer = (polyglot.ast.Initializer)member;
             fBody = initializer.body();
         
         }
         else {
             throw new RuntimeException("Only handle final locals for procedures and initializers!");
         }
         if ((fBody != null) && (fBody.statements() != null)){
             Iterator stmtsIt = fBody.statements().iterator();
             while (stmtsIt.hasNext()){
                 Object next = stmtsIt.next();
                 if (next instanceof polyglot.ast.LocalDecl){
                     polyglot.ast.LocalDecl decl = (polyglot.ast.LocalDecl)next;
                     if (decl.localInstance().flags().isFinal()){
                         finalsAvail.add(new polyglot.util.IdentityKey(decl.localInstance()));
                     }
                 }
             }
         }
         return finalsAvail;
     }
     
     private void addProcedureToClass(soot.SootMethod method) {
         sootClass.addMethod(method);
     }
     
 
     /**
      * Method Declaration Creation
      */
     private void createMethodDecl(polyglot.ast.MethodDecl method) {
 
         String name = createName(method);
             
         // parameters
         ArrayList parameters = createParameters(method);
                   
         // exceptions
         ArrayList exceptions = createExceptions(method);
     
 	    soot.SootMethod sootMethod = createSootMethod(name, method.flags(), method.returnType().type(), parameters, exceptions);
        
         finishProcedure(method, sootMethod);
     }
     
     
 	private soot.SootMethod createSootMethod(String name, polyglot.types.Flags flags , polyglot.types.Type returnType, ArrayList parameters, ArrayList exceptions){
         
 		int modifier = Util.getModifier(flags);
 		soot.Type sootReturnType = Util.getSootType(returnType);
 
 		soot.SootMethod method = new soot.SootMethod(name, parameters, sootReturnType, modifier, exceptions);
 		return method;
 	}
 	
     /**
      * Field Declaration Creation
      */
 	private void createFieldDecl(polyglot.ast.FieldDecl field){
 
         int modifiers = Util.getModifier(field.fieldInstance().flags());
         String name = field.fieldInstance().name();
         soot.Type sootType = Util.getSootType(field.fieldInstance().type());
         soot.SootField sootField = new soot.SootField(name, sootType, modifiers);
         //System.out.println("adding field: "+name+" to class: "+sootClass);
         sootClass.addField(sootField);
 
         if (fieldMap == null) {
             fieldMap = new HashMap();
         }
 
         fieldMap.put(field.fieldInstance(), sootField);
         
         if (field.fieldInstance().flags().isStatic()) {
             if (field.init() != null) {
                 if (staticFieldInits == null) {
                     staticFieldInits = new ArrayList();
                 }
                 staticFieldInits.add(field);
             }
         }
         else {
             if (field.init() != null) {
                 if (fieldInits == null) {
                     fieldInits = new ArrayList();
                 }
                 fieldInits.add(field);
             }
         }
 
         handleFinalLocals(field);
 
         Util.addLnPosTags(sootField, field.position());
 	}
 
     /**
      * Initializer Creation
      */
     private void createInitializer(polyglot.ast.Initializer initializer) {
         if (initializer.flags().isStatic()) {
             if (staticInitializerBlocks == null) {
                 staticInitializerBlocks = new ArrayList();
             }
             staticInitializerBlocks.add(initializer.body());
         }
         else {
             if (initializerBlocks == null) {
                 initializerBlocks = new ArrayList();
             }
             initializerBlocks.add(initializer.body());
         }
         handleFinalLocals(initializer);
     }
     
     /**
      * Constructor Declaration Creation
      */
     private void createConstructorDecl(polyglot.ast.ConstructorDecl constructor){
         String name = "<init>";
 
         ArrayList parameters = createParameters(constructor);
 
         ArrayList exceptions = createExceptions(constructor);
 
         soot.SootMethod sootMethod = createSootConstructor(name, constructor.flags(), parameters, exceptions);
 
         finishProcedure(constructor, sootMethod);
     }
 
     private soot.SootMethod createSootConstructor(String name, polyglot.types.Flags flags, ArrayList parameters, ArrayList exceptions) {
         
         int modifier = Util.getModifier(flags);
 
         soot.SootMethod method = new soot.SootMethod(name, parameters, soot.VoidType.v(), modifier);
 
         return method;
     }
     
     public BiMap getAnonClassMap(){
         return anonClassMap;
     }
 
     public BiMap getLocalClassMap(){
         return localClassMap;
     }
     
     public HashMap getAnonTypeMap(){
         return anonTypeMap;
     }
 
     public HashMap getLocalTypeMap(){
         return localTypeMap;
     }
   
     public HashMap finalLocalInfo(){
         return finalLocalInfo;
     }
 
     public int getNextPrivateAccessCounter(){
         int res = privateAccessCounter;
         privateAccessCounter++;
         return res;
     }
 
     public ArrayList getHasOuterRefInInit(){
         return hasOuterRefInInit;
     }
 
     public HashMap specialAnonMap(){
         return specialAnonMap;
     }
 }
