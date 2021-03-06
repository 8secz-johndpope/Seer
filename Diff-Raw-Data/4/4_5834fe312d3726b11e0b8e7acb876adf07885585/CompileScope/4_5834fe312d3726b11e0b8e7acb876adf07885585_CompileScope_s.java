 package php.runtime.env;
 
 import php.runtime.common.LangMode;
 import php.runtime.ext.CoreExtension;
 import php.runtime.ext.support.Extension;
 import php.runtime.ext.support.compile.CompileConstant;
 import php.runtime.ext.support.compile.CompileFunction;
 import php.runtime.lang.BaseException;
 import php.runtime.lang.Closure;
 import php.runtime.lang.StdClass;
 import php.runtime.lang.spl.*;
 import php.runtime.lang.spl.iterator.*;
 import php.runtime.loader.RuntimeClassLoader;
 import php.runtime.Memory;
 import php.runtime.reflection.*;
 import php.runtime.util.JVMStackTracer;
 
 import java.util.*;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.atomic.AtomicInteger;
 import java.util.concurrent.atomic.AtomicLong;
 
 public class CompileScope {
     protected RuntimeClassLoader classLoader;
     public final Set<String> superGlobals;
 
     protected final AtomicInteger moduleCount = new AtomicInteger(0);
     protected final AtomicLong classCount = new AtomicLong(0);
     protected final AtomicLong methodCount = new AtomicLong(0);
 
     public final ConcurrentHashMap<String, ModuleEntity> moduleMap;
     public final ConcurrentHashMap<String, ModuleEntity> moduleIndexMap;
 
     protected final Map<String, ClassEntity> classMap;
     protected final Map<String, FunctionEntity> functionMap;
     protected final Map<String, ConstantEntity> constantMap;
 
     protected Map<String, Extension> extensions;
 
     protected Map<String, CompileConstant> compileConstantMap;
     protected Map<String, CompileFunction> compileFunctionMap;
 
     public Map<String, Memory> configuration;
 
     // flags
     public boolean debugMode = false;
     public LangMode langMode = LangMode.JPHP;
 
     public final ClassEntity stdClassEntity;
     public final ClassEntity closureEntity;
     public final ClassEntity exceptionEntity;
 
     public CompileScope() {
         classLoader = new RuntimeClassLoader(Thread.currentThread().getContextClassLoader());
 
         moduleMap = new ConcurrentHashMap<String, ModuleEntity>();
         moduleIndexMap = new ConcurrentHashMap<String, ModuleEntity>();
 
         classMap = new HashMap<String, ClassEntity>();
         functionMap = new HashMap<String, FunctionEntity>();
         constantMap = new HashMap<String, ConstantEntity>();
 
         extensions = new LinkedHashMap<String, Extension>();
         compileConstantMap = new HashMap<String, CompileConstant>();
         compileFunctionMap = new HashMap<String, CompileFunction>();
 
         superGlobals = new HashSet<String>();
 
         superGlobals.add("GLOBALS");
         superGlobals.add("_ENV");
         superGlobals.add("_SERVER");
         superGlobals.add("_POST");
         superGlobals.add("_GET");
         superGlobals.add("_REQUEST");
         superGlobals.add("_FILES");
         superGlobals.add("_SESSION");
         superGlobals.add("_COOKIE");
 
         CoreExtension extension = new CoreExtension();
 
         registerClass(closureEntity = new ClassEntity(extension, this, Closure.class));
         registerClass(exceptionEntity = new ClassEntity(extension, this, BaseException.class));
         registerClass(new ClassEntity(extension, this, ErrorException.class));
         registerClass(stdClassEntity = new ClassEntity(extension, this, StdClass.class));
         registerClass(new ClassEntity(extension, this, ArrayAccess.class));
 
         // iterators
         registerClass(new ClassEntity(extension, this, Traversable.class));
         registerClass(new ClassEntity(extension, this, php.runtime.lang.spl.iterator.Iterator.class));
         registerClass(new ClassEntity(extension, this, IteratorAggregate.class));
 
         registerClass(new ClassEntity(extension, this, Serializable.class));
 
         registerExtension(extension);
     }
 
     public LangMode getLangMode() {
         return langMode;
     }
 
     public void setLangMode(LangMode langMode) {
         this.langMode = langMode;
     }
 
     public boolean isDebugMode() {
         return debugMode;
     }
 
     public void setDebugMode(boolean debugMode) {
         this.debugMode = debugMode;
     }
 
     public Map<String, ClassEntity> getClassMap() {
         return classMap;
     }
 
     public Map<String, FunctionEntity> getFunctionMap() {
         return functionMap;
     }
 
     public Map<String, ConstantEntity> getConstantMap() {
         return constantMap;
     }
 
     public int nextModuleIndex(){
         return moduleCount.incrementAndGet();
     }
 
     public long nextClassIndex(){
         return classCount.incrementAndGet();
     }
 
     public long nextMethodIndex(){
         return methodCount.incrementAndGet();
     }
 
     public void registerExtension(Extension extension){
         extension.onRegister(this);
         compileConstantMap.putAll(extension.getConstants());
         compileFunctionMap.putAll(extension.getFunctions());
 
         for(ClassEntity clazz : extension.getClasses().values()){
             if (!clazz.isInternal()){
                 registerClass(clazz);
                 clazz.setExtension(extension);
             }
         }
 
         for(CompileFunction function : extension.getFunctions().values()){
             functionMap.put(function.name.toLowerCase(), new CompileFunctionEntity(extension, function));
         }
 
         extensions.put(extension.getName().toLowerCase(), extension);
     }
 
     public Extension getExtension(String name){
         return extensions.get(name.toLowerCase());
     }
 
     public Set<String> getExtensions(){
         return extensions.keySet();
     }
 
     public void registerClass(ClassEntity clazz){
         classMap.put(clazz.getLowerName(), clazz);
     }
 
     public void addUserModule(ModuleEntity module){
         moduleMap.put(module.getName(), module);
         moduleIndexMap.put(module.getInternalName(), module);
     }
 
     public void registerFunction(FunctionEntity function){
         functionMap.put(function.getLowerName(), function);
     }
 
     public void registerConstant(ConstantEntity constant){
         constantMap.put(constant.getLowerName(), constant);
     }
 
     public ModuleEntity findUserModule(String name){
         return moduleMap.get(name);
     }
 
     public ClassEntity findUserClass(String name){
         return classMap.get(name.toLowerCase());
     }
 
     public FunctionEntity findUserFunction(String name){
         return functionMap.get(name.toLowerCase());
     }
 
     public ConstantEntity findUserConstant(String name){
         return constantMap.get(name.toLowerCase());
     }
 
     public Collection<ConstantEntity> getConstants(){
         return constantMap.values();
     }
 
     public CompileConstant findCompileConstant(String name){
         return compileConstantMap.get(name);
     }
 
     public CompileFunction findCompileFunction(String name){
         return compileFunctionMap.get(name.toLowerCase());
     }
 
     public ModuleEntity loadModule(String name){
         ModuleEntity entity = findUserModule(name);
         if (entity == null)
             return null;
 
         classLoader.loadModule(entity);
         return entity;
     }
 
     public void loadModule(ModuleEntity module){
         classLoader.loadModule(module);
     }
 
     public JVMStackTracer getStackTracer(StackTraceElement[] elements){
         return new JVMStackTracer(classLoader, elements);
     }
 
     public JVMStackTracer getStackTracer(Throwable throwable){
         return getStackTracer(throwable.getStackTrace());
     }
 
     public JVMStackTracer getStackTracer(){
         return getStackTracer(Thread.currentThread().getStackTrace());
     }
 }
