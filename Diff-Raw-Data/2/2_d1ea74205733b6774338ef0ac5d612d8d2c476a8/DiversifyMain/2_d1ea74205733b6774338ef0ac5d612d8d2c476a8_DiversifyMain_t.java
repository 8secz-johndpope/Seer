 package fr.inria.diversify;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 
 import fr.inria.diversify.statistic.CrossValidation;
 import fr.inria.diversify.statistic.StatisticCodeFragment;
 import fr.inria.diversify.statistic.StatisticDiversification;
 import fr.inria.diversify.transformation.TransformationsWriter;
 import fr.inria.diversify.transformation.builder.AbstractBuilder;
 import fr.inria.diversify.transformation.builder.AntBuilder;
 import fr.inria.diversify.transformation.builder.MavenBuilder;
 import fr.inria.diversify.util.maven.MavenDependencyResolver;
 import javassist.ClassPool;
 import javassist.CtClass;
 import javassist.CtMethod;
 import javassist.NotFoundException;
 
 import org.json.JSONException;
 
 import spoon.compiler.SpoonCompiler;
 import spoon.processing.ProcessingManager;
 import spoon.reflect.Factory;
 import spoon.reflect.declaration.CtSimpleType;
 import spoon.support.DefaultCoreFactory;
 import spoon.support.QueueProcessingManager;
 import spoon.support.StandardEnvironment;
 import spoon.support.compiler.JDTCompiler;
 import fr.inria.diversify.codeFragmentProcessor.AbstractCodeFragmentProcessor;
 import fr.inria.diversify.coverage.CoverageReport;
 import fr.inria.diversify.coverage.ICoverageReport;
 import fr.inria.diversify.coverage.MultiCoverageReport;
 import fr.inria.diversify.coverage.NullCoverageReport;
 import fr.inria.diversify.diversification.AbstractDiversify;
 import fr.inria.diversify.diversification.Diversify;
 import fr.inria.diversify.diversification.Sosie;
 import fr.inria.diversify.statistic.Util;
 import fr.inria.diversify.transformation.Transformation;
 import fr.inria.diversify.transformation.TransformationParser;
 import fr.inria.diversify.transformation.query.ITransformationQuery;
 import fr.inria.diversify.transformation.query.ast.ASTTransformationQuery;
 import fr.inria.diversify.transformation.query.ast.ASTTransformationQueryFromList;
 import fr.inria.diversify.transformation.query.bytecode.ByteCodeTransformationQuery;
 import fr.inria.diversify.util.DiversifyProperties;
 import fr.inria.diversify.util.Log;
 
 /**
  * User: Simon
  * Date: 9/11/13
  * Time: 11:41 AM
  */
 public class DiversifyMain {
     private CodeFragmentList codeFragments;
 
     public DiversifyMain(String propertiesFile) throws Exception {
         new DiversifyProperties(propertiesFile);
 
         initLogLevel();
        if(DiversifyProperties.getProperty("builder").equals("maven")) {
             MavenDependencyResolver t = new MavenDependencyResolver();
             t.DependencyResolver(DiversifyProperties.getProperty("project") + "/pom.xml");
         }
         initSpoon();
         Log.info("number of statement: " + codeFragments.size());
 
         if (DiversifyProperties.getProperty("stat").equals("true")) {
             computeStatistic();
 
         } else {
             if (DiversifyProperties.getProperty("sosieOnMultiProject").equals("true")) {
 //            sosieOnMultiProject();
             }
             else
                 initAndRunBuilder();
         }
     }
 
     protected void initAndRunBuilder() throws Exception {
         AbstractDiversify abstractDiversify = initAbstractDiversify();
 
         ITransformationQuery query = initTransformationQuery();
         abstractDiversify.setTransformationQuery(query);
 
         //TODO refactor
         if (DiversifyProperties.getProperty("nbRun").equals("all")) {
             if(DiversifyProperties.getProperty("transformation.directory") != null) {
                 TransformationParser tf = new TransformationParser(codeFragments);
                 List<Transformation> transformations = tf.parseDir(DiversifyProperties.getProperty("transformation.directory"));
                 Set<Transformation> set = new HashSet<Transformation>(transformations);
                 Log.debug("apply {} transformation", set.size());
                 abstractDiversify.run(set);
             }
             Util util = new Util(codeFragments);
             if (DiversifyProperties.getProperty("transformation.type").equals("replace"))
                 abstractDiversify.run(util.getAllReplace());
             if (DiversifyProperties.getProperty("transformation.type").equals("add"))
                 abstractDiversify.run(util.getAllAdd());
             if (DiversifyProperties.getProperty("transformation.type").equals("delete"))
                 abstractDiversify.run(util.getAllDelete());
         } else if (DiversifyProperties.getProperty("transformation.type").equals("stupid")) {
             int n = Integer.parseInt(DiversifyProperties.getProperty("nbRun"));
             Util util = new Util(codeFragments);
             abstractDiversify.run(util.getStupidTransformation(n, (ASTTransformationQuery)query));
         }
         else
         {
             int n = Integer.parseInt(DiversifyProperties.getProperty("nbRun"));
             abstractDiversify.run(n);
         }
     }
 
     protected AbstractDiversify initAbstractDiversify() throws Exception {
         AbstractDiversify ad;
         String projet = DiversifyProperties.getProperty("project");
         String src = DiversifyProperties.getProperty("src");
         if(DiversifyProperties.getProperty("sosie").equals("true"))
             ad = new Sosie(projet,src);
         else
             ad = new Diversify(projet,src);
 
         String tmpDir = ad.init(projet, DiversifyProperties.getProperty("outputDir"));
         ad.setBuilder(initBuilder(tmpDir));
 
         return ad;
     }
 
     protected AbstractBuilder initBuilder(String directory) throws Exception {
         AbstractBuilder rb;
         String src = DiversifyProperties.getProperty("src");
         if(DiversifyProperties.getProperty("builder").equals("maven")) {
             rb = new MavenBuilder(directory, src);
             rb.setPhase(new String[]{"clean", "test"});
         }
         else {
             rb = new AntBuilder(directory);
             rb.setPhase(new String[]{"deepclean", "junit-all"});
         }
         int t = Integer.parseInt(DiversifyProperties.getProperty("timeOut"));
         if (t == -1)
             rb.initTimeOut();
         else
             rb.setTimeOut(t);
 
         String pomFile = DiversifyProperties.getProperty("newPomFile");
         if(!pomFile.equals(""))
             rb.initPom(pomFile);
 
         if (DiversifyProperties.getProperty("clojure").equals("true"))
             rb.setClojureTest(true);
         return rb;
     }
 
 
     protected ITransformationQuery initTransformationQuery() throws IOException, JSONException, ClassNotFoundException, NotFoundException {
         ICoverageReport rg = initCoverageReport();
 
         ITransformationQuery atq;
 
         String transformation = DiversifyProperties.getProperty("transformation.directory");
         if (transformation != null) {
             TransformationParser tf = new TransformationParser(codeFragments);
             List<Transformation> list = tf.parseDir(transformation);
             atq = new ASTTransformationQueryFromList(list, rg, codeFragments);
         } else {
             Class cl = Class.forName(DiversifyProperties.getProperty("CodeFragmentClass"));
             atq = new ASTTransformationQuery(rg, codeFragments,cl);
         }
 
         if(DiversifyProperties.getProperty("transformation.level").equals("bytecode"))
             atq = new ByteCodeTransformationQuery(allCtMethod(),rg);
 
         atq.setType(DiversifyProperties.getProperty("transformation.type"));
         int n = Integer.parseInt(DiversifyProperties.getProperty("transformation.size"));
         atq.setNbTransformation(n);
 
         return atq;
     }
 
     protected ICoverageReport initCoverageReport() throws IOException {
         ICoverageReport icr;
         String jacocoFile = DiversifyProperties.getProperty("jacoco");
         String classes = DiversifyProperties.getProperty("project") + "/" + DiversifyProperties.getProperty("classes");
 
         if (jacocoFile != null) {
             File file = new File(jacocoFile);
             if (file.isDirectory())
                 icr = new MultiCoverageReport(classes, file);
             else
                 icr = new CoverageReport(classes, file);
         } else
             icr = new NullCoverageReport();
 
         icr.create();
         return icr;
     }
 
     protected void initSpoon() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
         String srcDirectory = DiversifyProperties.getProperty("project") + "/" + DiversifyProperties.getProperty("src");
 
         StandardEnvironment env = new StandardEnvironment();
         int javaVersion = Integer.parseInt(DiversifyProperties.getProperty("javaVersion"));
         env.setComplianceLevel(javaVersion);
         env.setVerbose(true);
         env.setDebug(true);
 
         DefaultCoreFactory f = new DefaultCoreFactory();
         Factory factory = new Factory(f, env);
         SpoonCompiler c = new JDTCompiler(factory);
         Log.debug("sourceClasspath: {}",c.getSourceClasspath());
         for (String dir : srcDirectory.split(System.getProperty("path.separator")))
             try {
                 Log.debug("add {} to classpath",dir);
                 c.addInputSource(new File(dir));
             } catch (IOException e) {
                 Log.error("error in initSpoon", e);
             }
         try {
             c.build();
         } catch (Exception e) {
             e.printStackTrace();
         }
         ProcessingManager pm = new QueueProcessingManager(factory);
         Class classz = Class.forName(DiversifyProperties.getProperty("processor"));
         AbstractCodeFragmentProcessor processor =  (AbstractCodeFragmentProcessor)classz.newInstance();
         pm.addProcessor(processor);
         pm.process();
 
         codeFragments = processor.getCodeFragments();
     }
 
     protected void computeStatistic() throws IOException, JSONException, InterruptedException {
         String out = DiversifyProperties.getProperty("result");
         computeCodeFragmentStatistic(out);
 
         String transDir = DiversifyProperties.getProperty("transformation.directory");
         if (transDir != null) {
             computeDiversifyStat(transDir, out);
         }
         computeOtherStat();
     }
 
     protected void computeDiversifyStat(String transDir, String fileName) throws IOException, JSONException, InterruptedException {
         TransformationParser tf = new TransformationParser(codeFragments);
         List<Transformation> transformations = tf.parseDir(transDir);
         TransformationsWriter write = new TransformationsWriter(transformations, fileName);
         int i = 0;
         for (Transformation t : transformations)
             if(t.numberOfFailure() == -1)
                 i++;
 
         Log.debug("i: "+i);
 
         String name = write.writeAllTransformation(null);
         statForR(name);
         for(String type : getAllTransformationType(transformations)) {
             Log.debug("all transformation for: "+type);
             name = write.writeAllTransformation(type);
 //            statForR(name);
         }
 
         name = write.writeGoodTransformation(null);
 //        statForR(name);
         for(String type : getAllTransformationType(transformations)) {
             Log.debug("good transformation for: "+type);
             name = write.writeGoodTransformation(type);
 //            statForR(name);
         }
 
         StatisticDiversification sd = new StatisticDiversification(transformations, codeFragments);
         sd.writeStat(fileName);
     }
 
     protected Set<String> getAllTransformationType(List<Transformation> transformations) {
         Set<String> types = new HashSet<String>();
         for (Transformation t : transformations)
             types.add(t.getType());
         return types;
     }
 
     protected void statForR(String fileName) throws IOException, JSONException {
         TransformationParser tf = new TransformationParser(codeFragments);
         Log.debug("parse fileName: {}",fileName);
 
         List<Transformation> transformations = tf.parseFile(new File(fileName));
         if(transformations.isEmpty())
             return;
 
         Set<Transformation> set = new HashSet<Transformation>(tf.parseFile(new File(fileName)));
         Log.debug("number of transformation: {}",transformations.size());
         Log.debug("number of unique transformation: {}",set.size());
 
         StatisticDiversification sd = new StatisticDiversification(set, codeFragments);
         String name = fileName.split(".json")[0];
         sd.writeStat(name);
 
         CrossValidation cv = new CrossValidation(transformations,10);
         cv.write(name+"_crossValidation.csv");
     }
 
     protected void computeOtherStat() throws InterruptedException {
         Util stat = new Util(codeFragments);
 //        System.out.println("number of possible code fragment replace: " + stat.numberOfDiversification());
         try {
             System.out.println("number of not possible code fragment replace/add: " + stat.numberOfNotDiversification());
         } catch (InterruptedException e) {
             e.printStackTrace();
         }
         System.out.println("number of possible code fragment replace: "+ stat.getAllReplace().size());
         System.out.println("number of possible code fragment add: " + stat.getAllAdd().size());
         System.out.println("number of possible code fragment delete: " + stat.getAllDelete().size());
     }
 
     protected void computeCodeFragmentStatistic(String output) {
         StatisticCodeFragment stat = new StatisticCodeFragment(codeFragments);
         try {
             stat.writeStatistic(output);
         } catch (IOException e) {
             Log.error("computeCodeFragmentStatistic ", e);
         }
     }
     protected void initLogLevel() {
         int level = Integer.parseInt(DiversifyProperties.getProperty("logLevel"));
         Log.set(level);
     }
 
     private List<CtMethod> allCtMethod() throws NotFoundException {
         List<CtMethod> methods = new ArrayList<CtMethod>();
         ClassPool pool = ClassPool.getDefault();
         pool.insertClassPath(DiversifyProperties.getProperty("project") + "/" + DiversifyProperties.getProperty("classes"));
         for (CtSimpleType cl: codeFragments.getAllClasses()) {
             try {
                 CtClass cc = pool.get(cl.getQualifiedName());
                 for(CtMethod method : cc.getDeclaredMethods())
                     if(!method.isEmpty()) {
                         methods.add(method);
                     }
             }  catch (Exception e) {
                 Log.error("error in allCtMethod",e);
             }
 
         }
         return methods;
     }
 
     protected void sosieOnMultiProject() throws Exception {
 //        TestSosie d = new TestSosie(initTransformationQuery(), DiversifyProperties.getProperty("project"));
 //
 //        List<String> list = new ArrayList<String>();
 //        for (String mvn : DiversifyProperties.getProperty("mavenProjects").split(System.getProperty("path.separator")))
 //            list.add(mvn);
 //        d.setMavenProject(list);
 //
 //        initAndRunBuilder(d);
     }
 
 }
