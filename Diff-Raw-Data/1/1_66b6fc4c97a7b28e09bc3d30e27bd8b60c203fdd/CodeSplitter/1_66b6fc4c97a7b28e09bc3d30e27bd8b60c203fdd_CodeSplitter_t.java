 /*
  * Copyright 2008 Google Inc.
  * 
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
  * use this file except in compliance with the License. You may obtain a copy of
  * the License at
  * 
  * http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  */
 package com.google.gwt.dev.jjs.impl;
 
 import com.google.gwt.core.ext.TreeLogger;
 import com.google.gwt.dev.jjs.SourceInfo;
 import com.google.gwt.dev.jjs.ast.Context;
 import com.google.gwt.dev.jjs.ast.JClassLiteral;
 import com.google.gwt.dev.jjs.ast.JDeclaredType;
 import com.google.gwt.dev.jjs.ast.JExpression;
 import com.google.gwt.dev.jjs.ast.JField;
 import com.google.gwt.dev.jjs.ast.JMethod;
 import com.google.gwt.dev.jjs.ast.JNewArray;
 import com.google.gwt.dev.jjs.ast.JNode;
 import com.google.gwt.dev.jjs.ast.JPrimitiveType;
 import com.google.gwt.dev.jjs.ast.JProgram;
 import com.google.gwt.dev.jjs.ast.JReferenceType;
 import com.google.gwt.dev.jjs.ast.JStringLiteral;
 import com.google.gwt.dev.jjs.ast.JVisitor;
 import com.google.gwt.dev.jjs.impl.FragmentExtractor.CfaLivenessPredicate;
 import com.google.gwt.dev.jjs.impl.FragmentExtractor.LivenessPredicate;
 import com.google.gwt.dev.jjs.impl.FragmentExtractor.NothingAlivePredicate;
 import com.google.gwt.dev.jjs.impl.FragmentExtractor.StatementLogger;
 import com.google.gwt.dev.js.ast.JsBlock;
 import com.google.gwt.dev.js.ast.JsExprStmt;
 import com.google.gwt.dev.js.ast.JsExpression;
 import com.google.gwt.dev.js.ast.JsFunction;
 import com.google.gwt.dev.js.ast.JsProgram;
 import com.google.gwt.dev.js.ast.JsStatement;
 import com.google.gwt.dev.js.ast.JsVars;
 import com.google.gwt.dev.js.ast.JsVars.JsVar;
 import com.google.gwt.dev.util.PerfLogger;
 import com.google.gwt.dev.util.collect.HashMap;
 import com.google.gwt.dev.util.collect.HashSet;
 
 import java.util.ArrayList;
 import java.util.LinkedHashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Queue;
 import java.util.Set;
 import java.util.concurrent.ArrayBlockingQueue;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 /**
  * <p>
  * Divides the code in a {@link JsProgram} into multiple fragments. The initial
  * fragment is sufficient to run all of the program's functionality except for
  * anything called in a callback supplied to
  * {@link com.google.gwt.core.client.GWT#runAsync(com.google.gwt.core.client.RunAsyncCallback)
  * GWT.runAsync()}. The remaining code should be downloadable via
  * {@link com.google.gwt.core.client.impl.AsyncFragmentLoader#inject(int)}.
  * </p>
  * 
  * <p>
  * The precise way the program is fragmented is an implementation detail that is
  * subject to change. Whenever the fragment strategy changes,
  * <code>AsyncFragmentLoader</code> must be updated in tandem. That said, the
  * current fragmentation strategy is to create an initial fragment, a leftovers
  * fragment, and one fragment per split point. Additionally, the splitter
  * computes an initial load sequence. All runAsync calls in the initial load
  * sequence are reached before any call not in the sequence. Further, any call
  * in the sequence is reached before any call later in the sequence.
  * </p>
  * 
  * <p>
  * The fragment for a split point contains different things depending on whether
  * it is in the initial load sequence or not. If it's in the initial load
  * sequence, then the fragment includes the code newly live once that split
  * point is crossed, that wasn't already live for the set of split points
  * earlier in the sequence. For a split point not in the initial load sequence,
  * the fragment contains only code exclusive to that split point, that is, code
  * that cannot be reached except via that split point. All other code goes into
  * the leftovers fragment.
  * </p>
  */
 public class CodeSplitter {
   /**
    * A statement logger that immediately prints out everything live that it
    * sees.
    */
   private class EchoStatementLogger implements StatementLogger {
     public void logStatement(JsStatement stat, boolean isIncluded) {
       if (isIncluded) {
         if (stat instanceof JsExprStmt) {
           JsExpression expr = ((JsExprStmt) stat).getExpression();
           if (expr instanceof JsFunction) {
             JsFunction func = (JsFunction) expr;
             if (func.getName() != null) {
               JMethod method = map.nameToMethod(func.getName());
               if (method != null) {
                 System.out.println(fullNameString(method));
               }
             }
           }
         }
 
         if (stat instanceof JsVars) {
           JsVars vars = (JsVars) stat;
           for (JsVar var : vars) {
             JField field = map.nameToField(var.getName());
             if (field != null) {
               System.out.println(fullNameString(field));
             }
             String string = map.stringLiteralForName(var.getName());
             if (string != null) {
               System.out.println("STRING " + var.getName());
             }
           }
         }
       }
     }
   }
 
   /**
    * A map from program atoms to the split point, if any, that they are
    * exclusive to. Atoms not exclusive to any split point are either mapped to 0
    * or left out of the map entirely. Note that the map is incomplete; any entry
    * not included has not been proven to be exclusive. Also, note that the
    * initial load sequence is assumed to already be loaded.
    */
   private static class ExclusivityMap {
     public Map<JField, Integer> fields = new HashMap<JField, Integer>();
     public Map<JMethod, Integer> methods = new HashMap<JMethod, Integer>();
     public Map<String, Integer> strings = new HashMap<String, Integer>();
     public Map<JReferenceType, Integer> types = new HashMap<JReferenceType, Integer>();
   }
 
   /**
    * A liveness predicate that is based on an exclusivity map.
    */
   private static class ExclusivityMapLivenessPredicate implements
       LivenessPredicate {
     private final int fragment;
     private final ExclusivityMap fragmentMap;
 
     public ExclusivityMapLivenessPredicate(ExclusivityMap fragmentMap,
         int fragment) {
       this.fragmentMap = fragmentMap;
       this.fragment = fragment;
     }
 
     public boolean isLive(JField field) {
       return checkMap(fragmentMap.fields, field);
     }
 
     public boolean isLive(JMethod method) {
       return checkMap(fragmentMap.methods, method);
     }
 
     public boolean isLive(JReferenceType type) {
       return checkMap(fragmentMap.types, type);
     }
 
     public boolean isLive(String literal) {
       return checkMap(fragmentMap.strings, literal);
     }
 
     public boolean miscellaneousStatementsAreLive() {
       return true;
     }
 
     private <T> boolean checkMap(Map<T, Integer> map, T x) {
       Integer entryForX = map.get(x);
       if (entryForX == null) {
         // unrecognized items are always live
         return true;
       } else {
         return (fragment == entryForX) || (entryForX == 0);
       }
     }
   }
 
   private static final Pattern LOADER_CLASS_PATTERN = Pattern.compile(FragmentLoaderCreator.ASYNC_LOADER_PACKAGE
       + "." + FragmentLoaderCreator.ASYNC_LOADER_CLASS_PREFIX + "([0-9]+)");
 
   /**
    * A Java property that causes the fragment map to be logged.
    */
   private static String PROP_LOG_FRAGMENT_MAP = "gwt.jjs.logFragmentMap";
 
   /**
    * Compute the set of initially live code for this program. Such code must be
    * included in the initial download of the program.
    */
   public static ControlFlowAnalyzer computeInitiallyLive(JProgram jprogram) {
     ControlFlowAnalyzer cfa = new ControlFlowAnalyzer(jprogram);
     traverseEntry(jprogram, cfa, 0);
     return cfa;
   }
 
   public static void exec(TreeLogger logger, JProgram jprogram,
       JsProgram jsprogram, JavaToJavaScriptMap map,
       LinkedHashSet<Integer> initialLoadSequence) {
     if (jprogram.entryMethods.size() == 1) {
       // Don't do anything if there is no call to runAsync
       return;
     }
 
     new CodeSplitter(logger, jprogram, jsprogram, map, initialLoadSequence).execImpl();
   }
 
   public static int getExclusiveFragmentNumber(int splitPoint,
       int numSplitPoints) {
     return splitPoint;
   }
 
   public static int getLeftoversFragmentNumber(int numSplitPoints) {
     return numSplitPoints + 1;
   }
 
   /**
    * Infer the number of split points for a given number of code fragments.
    */
   public static int numSplitPointsForFragments(int codeFragments) {
     assert (codeFragments != 2);
 
     if (codeFragments == 1) {
       return 0;
     }
 
     return codeFragments - 2;
   }
 
   /**
    * Choose an initial load sequence of split points for the specified program.
    * Do so by identifying split points whose code always load first, before any
    * other split points. As a side effect, modifies
    * {@link com.google.gwt.core.client.impl.AsyncFragmentLoader#initialLoadSequence}
    * in the program being compiled.
    */
   public static LinkedHashSet<Integer> pickInitialLoadSequence(
       TreeLogger logger, JProgram program) {
     LinkedHashSet<Integer> initialLoadSequence = new LinkedHashSet<Integer>();
     int numSplitPoints = program.entryMethods.size() - 1;
 
     if (numSplitPoints != 0) {
       Map<Integer, JMethod> splitPointToMethod = findRunAsyncMethods(program);
       assert (splitPointToMethod.size() == numSplitPoints);
 
       ControlFlowAnalyzer cfa = computeInitiallyLive(program);
 
       while (true) {
         Set<Integer> nextSplitPoints = splitPointsReachable(cfa,
             splitPointToMethod, numSplitPoints);
         nextSplitPoints.removeAll(initialLoadSequence);
 
         if (nextSplitPoints.size() != 1) {
           break;
         }
 
         int nextSplitPoint = nextSplitPoints.iterator().next();
         initialLoadSequence.add(nextSplitPoint);
         CodeSplitter.traverseEntry(program, cfa, nextSplitPoint);
       }
 
       logInitialLoadSequence(logger, initialLoadSequence);
       installInitialLoadSequenceField(program, initialLoadSequence);
     }
 
     return initialLoadSequence;
   }
 
   /**
    * <p>
    * Computes the "maximum total script size" for one permutation. The total
    * script size for one sequence of split points reached is the sum of the
    * scripts that are downloaded for that sequence. The maximum total script
    * size is the maximum such size for all possible sequences of split points.
    * </p>
    * 
    * @param jsLengths The lengths of the fragments for the compilation of one
    *          permutation
    */
   public static int totalScriptSize(int[] jsLengths) {
     /*
      * The total script size is currently simple: it's the sum of all the
      * individual script files.
      */
 
     int maxTotalSize;
     int numSplitPoints = numSplitPointsForFragments(jsLengths.length);
     if (numSplitPoints == 0) {
       maxTotalSize = jsLengths[0];
     } else {
       // Add up the initial and exclusive fragments
       maxTotalSize = jsLengths[0];
       for (int sp = 1; sp <= numSplitPoints; sp++) {
         int excl = getExclusiveFragmentNumber(sp, numSplitPoints);
         maxTotalSize += jsLengths[excl];
       }
 
       // Add the leftovers
       maxTotalSize += jsLengths[getLeftoversFragmentNumber(numSplitPoints)];
     }
     return maxTotalSize;
   }
 
   private static Map<JField, JClassLiteral> buildFieldToClassLiteralMap(
       JProgram jprogram) {
     final Map<JField, JClassLiteral> map = new HashMap<JField, JClassLiteral>();
     class BuildFieldToLiteralVisitor extends JVisitor {
       @Override
       public void endVisit(JClassLiteral lit, Context ctx) {
         map.put(lit.getField(), lit);
       }
     }
     (new BuildFieldToLiteralVisitor()).accept(jprogram);
     return map;
   }
 
   /**
    * Maps each split point number to its corresponding generated
    * <code>runAsync</code> method. If that method has been discarded, then map
    * the split point number to <code>null</code>.
    */
   private static Map<Integer, JMethod> findRunAsyncMethods(JProgram program)
       throws NumberFormatException {
     Map<Integer, JMethod> splitPointToLoadMethod = new HashMap<Integer, JMethod>();
     // These methods aren't indexed, so scan the whole program
 
     for (JDeclaredType type : program.getDeclaredTypes()) {
       Matcher matcher = LOADER_CLASS_PATTERN.matcher(type.getName());
       if (matcher.matches()) {
         int sp = Integer.parseInt(matcher.group(1));
         JMethod loadMethod = null;
         for (JMethod meth : type.getMethods()) {
           if (meth.getName().equals(
               FragmentLoaderCreator.LOADER_METHOD_RUN_ASYNC)) {
             loadMethod = meth;
           }
         }
         splitPointToLoadMethod.put(sp, loadMethod);
       }
     }
     return splitPointToLoadMethod;
   }
 
   private static String fullNameString(JField field) {
     return field.getEnclosingType().getName() + "." + field.getName();
   }
 
   private static String fullNameString(JMethod method) {
     return method.getEnclosingType().getName() + "."
         + JProgram.getJsniSig(method);
   }
 
   private static <T> int getOrZero(Map<T, Integer> map, T key) {
     Integer value = map.get(key);
     return (value == null) ? 0 : value;
   }
 
   private static void installInitialLoadSequenceField(JProgram program,
       LinkedHashSet<Integer> initialLoadSequence) {
     JField initLoadSeqField = program.getIndexedField("AsyncFragmentLoader.initialLoadSequence");
     SourceInfo info = program.createSourceInfoSynthetic(ReplaceRunAsyncs.class,
         "array with initial load sequence");
     List<JExpression> intExprs = new ArrayList<JExpression>();
     for (int sp : initialLoadSequence) {
       intExprs.add(program.getLiteralInt(sp));
     }
     /*
      * Note: the following field is known to have a manually installed
      * initializer, of new int[0].
      */
     initLoadSeqField.getDeclarationStatement().initializer = JNewArray.createInitializers(
         program, info, program.getTypeArray(JPrimitiveType.INT, 1), intExprs);
   }
 
   private static void logInitialLoadSequence(TreeLogger logger,
       LinkedHashSet<Integer> initialLoadSequence) {
     StringBuffer message = new StringBuffer();
     message.append("Initial load sequence of split points: ");
     if (initialLoadSequence.isEmpty()) {
       message.append("(none)");
     } else {
       boolean first = true;
       for (int sp : initialLoadSequence) {
         if (first) {
           first = false;
         } else {
           message.append(", ");
         }
         message.append(sp);
       }
     }
 
     logger.log(TreeLogger.TRACE, message.toString());
   }
 
   /**
    * Find all split points reachable in the specified ControlFlowAnalyzer.
    * 
    * @param cfa the control-flow analyzer to search
    * @param splitPointToMethod a map from split points to methods, computed with
    *          {@link #findRunAsyncMethods(JProgram)}.
    * @param numSplitPoints the number of split points in the program
    */
   private static Set<Integer> splitPointsReachable(ControlFlowAnalyzer cfa,
       Map<Integer, JMethod> splitPointToMethod, int numSplitPoints) {
     Set<Integer> nextSplitPoints = new HashSet<Integer>();
 
     for (int sp = 1; sp <= numSplitPoints; sp++) {
       if (cfa.getLiveFieldsAndMethods().contains(splitPointToMethod.get(sp))) {
         nextSplitPoints.add(sp);
       }
     }
 
     return nextSplitPoints;
   }
 
   /**
    * Traverse all code in the program that is reachable via split point
    * <code>splitPoint</code>.
    */
   private static void traverseEntry(JProgram jprogram, ControlFlowAnalyzer cfa,
       int splitPoint) {
     for (JMethod entryMethod : jprogram.entryMethods.get(splitPoint)) {
       cfa.traverseFrom(entryMethod);
     }
     if (splitPoint == 0) {
       /*
        * Include class literal factories for simplicity. It is possible to move
        * them out, if they are only needed by one fragment, but they are tiny,
        * so it does not seem worth the complexity in the compiler.
        */
       cfa.traverseFromClassLiteralFactories();
     }
   }
 
   private static <T> Set<T> union(Set<? extends T> set1, Set<? extends T> set2) {
     Set<T> union = new HashSet<T>();
     union.addAll(set1);
     union.addAll(set2);
     return union;
   }
 
   private static <T> void updateMap(int entry, Map<T, Integer> map,
       Set<?> liveWithoutEntry, Iterable<T> all) {
     for (T each : all) {
       if (!liveWithoutEntry.contains(each)) {
         /*
          * Note that it is fine to overwrite a preexisting entry in the map. If
          * an atom is dead until split point i has been reached, and is also
          * dead until entry j has been reached, then it is dead until both have
          * been reached. Thus, it can be downloaded along with either i's or j's
          * code.
          */
         map.put(each, entry);
       }
     }
   }
 
   private final Map<JField, JClassLiteral> fieldToLiteralOfClass;
   private final FragmentExtractor fragmentExtractor;
   private final LinkedHashSet<Integer> initialLoadSequence;
 
   /**
    * Code that is initially live when the program first downloads.
    */
   private final ControlFlowAnalyzer initiallyLive;
   private JProgram jprogram;
   private JsProgram jsprogram;
 
   /**
    * Computed during {@link #execImpl()}, so that intermediate steps of it can
    * be used as they are created.
    */
   private ControlFlowAnalyzer liveAfterInitialSequence;
   private final TreeLogger logger;
   private final boolean logging;
   private JavaToJavaScriptMap map;
   private final Set<JMethod> methodsInJavaScript;
   private final int numEntries;
 
   private CodeSplitter(TreeLogger logger, JProgram jprogram,
       JsProgram jsprogram, JavaToJavaScriptMap map,
       LinkedHashSet<Integer> initialLoadSequence) {
     this.logger = logger.branch(TreeLogger.TRACE,
         "Splitting JavaScript for incremental download");
     this.jprogram = jprogram;
     this.jsprogram = jsprogram;
     this.map = map;
     this.initialLoadSequence = initialLoadSequence;
 
     numEntries = jprogram.entryMethods.size();
     logging = Boolean.getBoolean(PROP_LOG_FRAGMENT_MAP);
     fieldToLiteralOfClass = buildFieldToClassLiteralMap(jprogram);
     fragmentExtractor = new FragmentExtractor(jprogram, jsprogram, map);
 
     initiallyLive = computeInitiallyLive(jprogram);
 
     methodsInJavaScript = fragmentExtractor.findAllMethodsInJavaScript();
   }
 
   /**
    * Create a new fragment and add it to the table of fragments.
    * 
    * @param splitPoint The split point to associate this code with
    * @param alreadyLoaded The code that should be assumed to have already been
    *          loaded
    * @param liveNow The code that is assumed live once this fragment loads;
    *          anything in here but not in <code>alreadyLoaded</code> will be
    *          included in the created fragment
    * @param stmtsToAppend Additional statements to append to the end of the new
    *          fragment
    * @param fragmentStats The list of fragments to append to
    */
   private void addFragment(int splitPoint, LivenessPredicate alreadyLoaded,
       LivenessPredicate liveNow, List<JsStatement> stmtsToAppend,
       Map<Integer, List<JsStatement>> fragmentStats) {
     if (logging) {
       System.out.println();
       System.out.println("==== Fragment " + fragmentStats.size() + " ====");
       fragmentExtractor.setStatementLogger(new EchoStatementLogger());
     }
     List<JsStatement> stats = fragmentExtractor.extractStatements(liveNow,
         alreadyLoaded);
     stats.addAll(stmtsToAppend);
     fragmentStats.put(splitPoint, stats);
   }
 
   /**
    * For each split point other than those in the initial load sequence, compute
    * a CFA that traces every other split point. For those that are in the
    * initial load sequence, add a <code>null</code> to the list.
    */
   private List<ControlFlowAnalyzer> computeAllButOneCfas() {
     List<ControlFlowAnalyzer> allButOnes = new ArrayList<ControlFlowAnalyzer>(
         numEntries - 1);
 
     for (int entry = 1; entry < numEntries; entry++) {
       if (isInitial(entry)) {
         allButOnes.add(null);
         continue;
       }
       ControlFlowAnalyzer cfa = new ControlFlowAnalyzer(
           liveAfterInitialSequence);
       traverseAllButEntry(cfa, entry);
       // Traverse leftoversFragmentHasLoaded, because it should not
       // go into any of the exclusive fragments.
       cfa.traverseFromLeftoversFragmentHasLoaded();
       allButOnes.add(cfa);
     }
 
     return allButOnes;
   }
 
   /**
    * Compute a CFA that covers the entire live code of the program.
    */
   private ControlFlowAnalyzer computeCompleteCfa() {
     ControlFlowAnalyzer everything = new ControlFlowAnalyzer(jprogram);
     for (int entry = 0; entry < numEntries; entry++) {
       traverseEntry(everything, entry);
     }
     everything.traverseFromLeftoversFragmentHasLoaded();
     return everything;
   }
 
   /**
    * Map each program atom as exclusive to some split point, whenever possible.
    * Also fixes up load order problems that could result from splitting code
    * based on this assumption.
    */
   private ExclusivityMap determineExclusivity() {
     ExclusivityMap fragmentMap = new ExclusivityMap();
 
     mapExclusiveAtoms(fragmentMap);
     fixUpLoadOrderDependencies(fragmentMap);
 
     return fragmentMap;
   }
 
   private void execImpl() {
     PerfLogger.start("CodeSplitter");
     Map<Integer, List<JsStatement>> fragmentStats = new HashMap<Integer, List<JsStatement>>();
 
     {
       /*
        * Compute the base fragment. It includes everything that is live when the
        * program starts.
        */
       LivenessPredicate alreadyLoaded = new NothingAlivePredicate();
       LivenessPredicate liveNow = new CfaLivenessPredicate(initiallyLive);
       List<JsStatement> noStats = new ArrayList<JsStatement>();
       addFragment(0, alreadyLoaded, liveNow, noStats, fragmentStats);
     }
 
     /*
      * Compute the base fragments, for split points in the initial load
      * sequence.
      */
     liveAfterInitialSequence = new ControlFlowAnalyzer(initiallyLive);
     for (int sp : initialLoadSequence) {
       LivenessPredicate alreadyLoaded = new CfaLivenessPredicate(
           liveAfterInitialSequence);
 
       ControlFlowAnalyzer liveAfterSp = new ControlFlowAnalyzer(
           liveAfterInitialSequence);
       traverseEntry(liveAfterSp, sp);
       LivenessPredicate liveNow = new CfaLivenessPredicate(liveAfterSp);
 
       List<JsStatement> statsToAppend = fragmentExtractor.createCallsToEntryMethods(sp);
 
       addFragment(sp, alreadyLoaded, liveNow, statsToAppend, fragmentStats);
 
       liveAfterInitialSequence = liveAfterSp;
     }
 
     ExclusivityMap fragmentMap = determineExclusivity();
 
     /*
      * Compute the exclusively live fragments. Each includes everything
      * exclusively live after entry point i.
      */
     for (int i = 1; i < numEntries; i++) {
       if (isInitial(i)) {
         continue;
       }
       LivenessPredicate alreadyLoaded = new ExclusivityMapLivenessPredicate(
           fragmentMap, 0);
       LivenessPredicate liveNow = new ExclusivityMapLivenessPredicate(
           fragmentMap, i);
       List<JsStatement> statsToAppend = fragmentExtractor.createCallsToEntryMethods(i);
       addFragment(i, alreadyLoaded, liveNow, statsToAppend, fragmentStats);
     }
 
     /*
      * Compute the leftovers fragment.
      */
     {
       LivenessPredicate alreadyLoaded = new CfaLivenessPredicate(
           liveAfterInitialSequence);
       LivenessPredicate liveNow = new ExclusivityMapLivenessPredicate(
           fragmentMap, 0);
       List<JsStatement> statsToAppend = fragmentExtractor.createCallToLeftoversFragmentHasLoaded();
       addFragment(numEntries, alreadyLoaded, liveNow, statsToAppend,
           fragmentStats);
     }
 
     // now install the new statements in the program fragments
     jsprogram.setFragmentCount(fragmentStats.size());
     for (int i = 0; i < fragmentStats.size(); i++) {
       JsBlock fragBlock = jsprogram.getFragmentBlock(i);
       fragBlock.getStatements().clear();
       fragBlock.getStatements().addAll(fragmentStats.get(i));
     }
 
     PerfLogger.end();
   }
 
   /**
    * <p>
    * Patch up the fragment map to satisfy load-order dependencies, as described
    * in the comment of {@link LivenessPredicate}. Load-order dependencies can be
    * violated when an atom is mapped to 0 as a leftover, but it has some
    * load-order dependency on an atom that was put in an exclusive fragment.
    * </p>
    * 
    * <p>
    * In general, it might be possible to split things better by considering load
    * order dependencies when building the fragment map. However, fixing them
    * after the fact makes CodeSplitter simpler. In practice, for programs tried
    * so far, there are very few load order dependency fixups that actually
    * happen, so it seems better to keep the compiler simpler.
    * </p>
    */
   private void fixUpLoadOrderDependencies(ExclusivityMap fragmentMap) {
     fixUpLoadOrderDependenciesForMethods(fragmentMap);
     fixUpLoadOrderDependenciesForTypes(fragmentMap);
     fixUpLoadOrderDependenciesForClassLiterals(fragmentMap);
     fixUpLoadOrderDependenciesForFieldsInitializedToStrings(fragmentMap);
   }
 
   private void fixUpLoadOrderDependenciesForClassLiterals(
       ExclusivityMap fragmentMap) {
     int numClassLitStrings = 0;
     int numFixups = 0;
     for (JField field : fragmentMap.fields.keySet()) {
       JClassLiteral classLit = fieldToLiteralOfClass.get(field);
       if (classLit != null) {
         int classLitFrag = fragmentMap.fields.get(field);
         for (String string : stringsIn(field.getInitializer())) {
           numClassLitStrings++;
           int stringFrag = getOrZero(fragmentMap.strings, string);
           if (stringFrag != classLitFrag && stringFrag != 0) {
             numFixups++;
             fragmentMap.strings.put(string, 0);
           }
         }
       }
     }
     logger.log(TreeLogger.DEBUG, "Fixed up load-order dependencies by moving "
         + numFixups
         + " strings in class literal constructors to fragment 0, out of "
         + numClassLitStrings);
   }
 
   private void fixUpLoadOrderDependenciesForFieldsInitializedToStrings(
       ExclusivityMap fragmentMap) {
     int numFixups = 0;
     int numFieldStrings = 0;
 
     for (JField field : fragmentMap.fields.keySet()) {
       if (field.getInitializer() instanceof JStringLiteral) {
         numFieldStrings++;
 
         String string = ((JStringLiteral) field.getInitializer()).getValue();
         int fieldFrag = getOrZero(fragmentMap.fields, field);
         int stringFrag = getOrZero(fragmentMap.strings, string);
         if (fieldFrag != stringFrag && stringFrag != 0) {
           numFixups++;
           fragmentMap.strings.put(string, 0);
         }
       }
     }
 
     logger.log(TreeLogger.DEBUG, "Fixed up load-order dependencies by moving "
         + numFixups
         + " strings used to initialize fields to fragment 0, out of "
         + +numFieldStrings);
   }
 
   private void fixUpLoadOrderDependenciesForMethods(ExclusivityMap fragmentMap) {
     int numFixups = 0;
 
     for (JDeclaredType type : jprogram.getDeclaredTypes()) {
       int typeFrag = getOrZero(fragmentMap.types, type);
 
       if (typeFrag != 0) {
         /*
          * If the type is in an exclusive fragment, all its instance methods
          * must be in the same one.
          */
         for (JMethod method : type.getMethods()) {
           if (!method.isStatic() && methodsInJavaScript.contains(method)) {
             int methodFrag = getOrZero(fragmentMap.methods, method);
             if (methodFrag != typeFrag) {
               fragmentMap.types.put(type, 0);
               numFixups++;
               break;
             }
           }
         }
       }
     }
 
     logger.log(TreeLogger.DEBUG,
         "Fixed up load-order dependencies for instance methods by moving "
             + numFixups + " types to fragment 0, out of "
             + jprogram.getDeclaredTypes().size());
   }
 
   private void fixUpLoadOrderDependenciesForTypes(ExclusivityMap fragmentMap) {
     int numFixups = 0;
     Queue<JReferenceType> typesToCheck = new ArrayBlockingQueue<JReferenceType>(
         jprogram.getDeclaredTypes().size());
     typesToCheck.addAll(jprogram.getDeclaredTypes());
 
     while (!typesToCheck.isEmpty()) {
       JReferenceType type = typesToCheck.remove();
       if (type.getSuperClass() != null) {
         int typeFrag = getOrZero(fragmentMap.types, type);
         int supertypeFrag = getOrZero(fragmentMap.types, type.getSuperClass());
         if (typeFrag != supertypeFrag && supertypeFrag != 0) {
           numFixups++;
           fragmentMap.types.put(type.getSuperClass(), 0);
           typesToCheck.add(type.getSuperClass());
         }
       }
     }
 
     logger.log(TreeLogger.DEBUG,
         "Fixed up load-order dependencies on supertypes by moving " + numFixups
             + " types to fragment 0, out of "
             + jprogram.getDeclaredTypes().size());
   }
 
   private boolean isInitial(int entry) {
     return initialLoadSequence.contains(entry);
   }
 
   /**
    * Map atoms to exclusive fragments. Do this by trying to find code atoms that
    * are only needed by a single split point. Such code can be moved to the
    * exclusively live fragment associated with that split point.
    */
   private void mapExclusiveAtoms(ExclusivityMap fragmentMap) {
     List<ControlFlowAnalyzer> allButOnes = computeAllButOneCfas();
 
     ControlFlowAnalyzer everything = computeCompleteCfa();
 
     Set<JField> allFields = new HashSet<JField>();
     Set<JMethod> allMethods = new HashSet<JMethod>();
 
     for (JNode node : everything.getLiveFieldsAndMethods()) {
       if (node instanceof JField) {
         allFields.add((JField) node);
       }
       if (node instanceof JMethod) {
         allMethods.add((JMethod) node);
       }
     }
     allFields.addAll(everything.getFieldsWritten());
 
     for (int entry = 1; entry < numEntries; entry++) {
       if (isInitial(entry)) {
         continue;
       }
       ControlFlowAnalyzer allButOne = allButOnes.get(entry - 1);
       Set<JNode> allLiveNodes = union(allButOne.getLiveFieldsAndMethods(),
           allButOne.getFieldsWritten());
       updateMap(entry, fragmentMap.fields, allLiveNodes, allFields);
       updateMap(entry, fragmentMap.methods,
           allButOne.getLiveFieldsAndMethods(), allMethods);
       updateMap(entry, fragmentMap.strings, allButOne.getLiveStrings(),
           everything.getLiveStrings());
       updateMap(entry, fragmentMap.types, allButOne.getInstantiatedTypes(),
           everything.getInstantiatedTypes());
     }
   }
 
   /**
    * Traverse <code>exp</code> and find all string literals within it.
    */
   private Set<String> stringsIn(JExpression exp) {
     final Set<String> strings = new HashSet<String>();
     class StringFinder extends JVisitor {
       @Override
       public void endVisit(JStringLiteral stringLiteral, Context ctx) {
         strings.add(stringLiteral.getValue());
       }
     }
     (new StringFinder()).accept(exp);
     return strings;
   }
 
   /**
    * Traverse all code in the program except for that reachable only via
    * fragment <code>frag</code>. This does not call
    * {@link ControlFlowAnalyzer#finishTraversal()}.
    */
   private void traverseAllButEntry(ControlFlowAnalyzer cfa, int entry) {
     for (int otherEntry = 0; otherEntry < numEntries; otherEntry++) {
       if (otherEntry != entry) {
         traverseEntry(cfa, otherEntry);
       }
     }
   }
 
   private void traverseEntry(ControlFlowAnalyzer cfa, int splitPoint) {
     traverseEntry(jprogram, cfa, splitPoint);
   }
 }
