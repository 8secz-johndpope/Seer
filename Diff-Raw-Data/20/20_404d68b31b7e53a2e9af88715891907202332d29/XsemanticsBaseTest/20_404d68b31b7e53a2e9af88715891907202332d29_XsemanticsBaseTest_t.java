 package it.xsemantics.dsl.tests;
 
 import com.google.inject.Inject;
 import it.xsemantics.dsl.XsemanticsInjectorProvider;
 import it.xsemantics.dsl.tests.input.FjTypeSystemFiles;
 import it.xsemantics.dsl.tests.input.XsemanticsTestFiles;
 import it.xsemantics.dsl.xsemantics.CheckRule;
 import it.xsemantics.dsl.xsemantics.EnvironmentAccess;
 import it.xsemantics.dsl.xsemantics.EnvironmentComposition;
 import it.xsemantics.dsl.xsemantics.EnvironmentMapping;
 import it.xsemantics.dsl.xsemantics.EnvironmentSpecification;
 import it.xsemantics.dsl.xsemantics.ErrorSpecification;
 import it.xsemantics.dsl.xsemantics.ExpressionInConclusion;
 import it.xsemantics.dsl.xsemantics.JudgmentDescription;
 import it.xsemantics.dsl.xsemantics.OrExpression;
 import it.xsemantics.dsl.xsemantics.Rule;
 import it.xsemantics.dsl.xsemantics.RuleConclusion;
 import it.xsemantics.dsl.xsemantics.RuleConclusionElement;
 import it.xsemantics.dsl.xsemantics.RuleInvocation;
 import it.xsemantics.dsl.xsemantics.RuleParameter;
 import it.xsemantics.dsl.xsemantics.RuleWithPremises;
 import it.xsemantics.dsl.xsemantics.XsemanticsSystem;
 import it.xsemantics.example.fj.fj.FjFactory;
 import java.util.List;
 import junit.framework.Assert;
 import org.eclipse.emf.common.util.EList;
 import org.eclipse.emf.ecore.EObject;
 import org.eclipse.xtext.EcoreUtil2;
 import org.eclipse.xtext.common.types.JvmFormalParameter;
 import org.eclipse.xtext.junit4.InjectWith;
 import org.eclipse.xtext.junit4.XtextRunner;
 import org.eclipse.xtext.junit4.util.ParseHelper;
 import org.eclipse.xtext.junit4.validation.ValidationTestHelper;
 import org.eclipse.xtext.xbase.XAbstractFeatureCall;
 import org.eclipse.xtext.xbase.XAssignment;
 import org.eclipse.xtext.xbase.XBlockExpression;
 import org.eclipse.xtext.xbase.XExpression;
 import org.eclipse.xtext.xbase.XForLoopExpression;
 import org.eclipse.xtext.xbase.XIfExpression;
 import org.eclipse.xtext.xbase.XVariableDeclaration;
 import org.eclipse.xtext.xbase.lib.Exceptions;
 import org.eclipse.xtext.xbase.lib.Functions.Function1;
 import org.eclipse.xtext.xbase.lib.IntegerExtensions;
 import org.eclipse.xtext.xbase.lib.IterableExtensions;
 import org.eclipse.xtext.xbase.lib.ObjectExtensions;
 import org.eclipse.xtext.xbase.lib.StringExtensions;
 import org.junit.BeforeClass;
 import org.junit.runner.RunWith;
 
 @SuppressWarnings("all")
 @InjectWith(XsemanticsInjectorProvider.class)
 @RunWith(XtextRunner.class)
 public class XsemanticsBaseTest {
   @Inject
   protected XsemanticsTestFiles testFiles;
   
   @Inject
   protected FjTypeSystemFiles fjTestFiles;
   
   @Inject
   protected ParseHelper<XsemanticsSystem> parser;
   
   @Inject
   private ValidationTestHelper _validationTestHelper;
   
   @BeforeClass
   public static void ensureFjIsLoaded() {
       FjFactory.eINSTANCE.createProgram();
       return;
   }
   
   public XsemanticsSystem parseAndAssertNoError(final CharSequence s) {
     try {
       XsemanticsSystem _xblockexpression = null;
       {
         XsemanticsSystem _parse = this.parser.parse(s);
         XsemanticsSystem ts = _parse;
         this._validationTestHelper.assertNoErrors(ts);
         _xblockexpression = (ts);
       }
       return _xblockexpression;
     } catch (Exception _e) {
       throw Exceptions.sneakyThrow(_e);
     }
   }
   
   public XsemanticsSystem parse(final CharSequence s) {
     try {
       XsemanticsSystem _parse = this.parser.parse(s);
       return _parse;
     } catch (Exception _e) {
       throw Exceptions.sneakyThrow(_e);
     }
   }
   
   public Rule getFirstRule(final CharSequence s) {
     Rule _rule = this.getRule(s, 0);
     return _rule;
   }
   
   public Rule getRule(final CharSequence s, final int index) {
     XsemanticsSystem _parseAndAssertNoError = this.parseAndAssertNoError(s);
     Rule _rule = this.getRule(_parseAndAssertNoError, index);
     return _rule;
   }
   
  public Rule getRuleWithoutValidation(final CharSequence s, final int index) {
    XsemanticsSystem _parse = this.parse(s);
    Rule _rule = this.getRule(_parse, index);
    return _rule;
  }
  
   public Rule getRule(final XsemanticsSystem ts, final int index) {
     Rule _xblockexpression = null;
     {
       EList<Rule> _rules = ts.getRules();
       final EList<Rule> rules = _rules;
       String _operator_plus = StringExtensions.operator_plus("no rule for index ", Integer.valueOf(index));
       String _operator_plus_1 = StringExtensions.operator_plus(_operator_plus, ", only ");
       int _size = rules.size();
       String _operator_plus_2 = StringExtensions.operator_plus(_operator_plus_1, Integer.valueOf(_size));
       int _size_1 = rules.size();
       boolean _operator_greaterThan = IntegerExtensions.operator_greaterThan(_size_1, index);
       Assert.assertTrue(_operator_plus_2, _operator_greaterThan);
       Rule _get = rules.get(index);
       _xblockexpression = (_get);
     }
     return _xblockexpression;
   }
   
   public CheckRule getFirstCheckRule(final CharSequence s) {
     CheckRule _checkRule = this.getCheckRule(s, 0);
     return _checkRule;
   }
   
   public CheckRule getCheckRule(final CharSequence s, final int index) {
     XsemanticsSystem _parseAndAssertNoError = this.parseAndAssertNoError(s);
     CheckRule _checkRule = this.getCheckRule(_parseAndAssertNoError, index);
     return _checkRule;
   }
   
   public CheckRule getCheckRule(final XsemanticsSystem ts, final int index) {
     CheckRule _xblockexpression = null;
     {
       EList<CheckRule> _checkrules = ts.getCheckrules();
       final EList<CheckRule> rules = _checkrules;
       String _operator_plus = StringExtensions.operator_plus("no rule for index ", Integer.valueOf(index));
       String _operator_plus_1 = StringExtensions.operator_plus(_operator_plus, ", only ");
       int _size = rules.size();
       String _operator_plus_2 = StringExtensions.operator_plus(_operator_plus_1, Integer.valueOf(_size));
       int _size_1 = rules.size();
       boolean _operator_greaterThan = IntegerExtensions.operator_greaterThan(_size_1, index);
       Assert.assertTrue(_operator_plus_2, _operator_greaterThan);
       CheckRule _get = rules.get(index);
       _xblockexpression = (_get);
     }
     return _xblockexpression;
   }
   
   public XVariableDeclaration firstVariableDeclaration(final CharSequence s) {
     XsemanticsSystem _parseAndAssertNoError = this.parseAndAssertNoError(s);
     List<XVariableDeclaration> _allContentsOfType = EcoreUtil2.<XVariableDeclaration>getAllContentsOfType(_parseAndAssertNoError, org.eclipse.xtext.xbase.XVariableDeclaration.class);
     XVariableDeclaration _get = _allContentsOfType.get(0);
     return _get;
   }
   
   public XAssignment firstAssignment(final CharSequence s) {
     XsemanticsSystem _parseAndAssertNoError = this.parseAndAssertNoError(s);
     List<XAssignment> _allContentsOfType = EcoreUtil2.<XAssignment>getAllContentsOfType(_parseAndAssertNoError, org.eclipse.xtext.xbase.XAssignment.class);
     XAssignment _get = _allContentsOfType.get(0);
     return _get;
   }
   
   public XIfExpression firstIf(final CharSequence s) {
     XsemanticsSystem _parseAndAssertNoError = this.parseAndAssertNoError(s);
     List<XIfExpression> _allContentsOfType = EcoreUtil2.<XIfExpression>getAllContentsOfType(_parseAndAssertNoError, org.eclipse.xtext.xbase.XIfExpression.class);
     XIfExpression _get = _allContentsOfType.get(0);
     return _get;
   }
   
   public XForLoopExpression firstFor(final CharSequence s) {
     XsemanticsSystem _parseAndAssertNoError = this.parseAndAssertNoError(s);
     List<XForLoopExpression> _allContentsOfType = EcoreUtil2.<XForLoopExpression>getAllContentsOfType(_parseAndAssertNoError, org.eclipse.xtext.xbase.XForLoopExpression.class);
     XForLoopExpression _get = _allContentsOfType.get(0);
     return _get;
   }
   
   public JudgmentDescription firstJudgmentDescription(final CharSequence s) {
     XsemanticsSystem _parseAndAssertNoError = this.parseAndAssertNoError(s);
     JudgmentDescription _firstJudgmentDescription = this.firstJudgmentDescription(_parseAndAssertNoError);
     return _firstJudgmentDescription;
   }
   
   public JudgmentDescription firstJudgmentDescription(final XsemanticsSystem ts) {
     List<JudgmentDescription> _allContentsOfType = EcoreUtil2.<JudgmentDescription>getAllContentsOfType(ts, it.xsemantics.dsl.xsemantics.JudgmentDescription.class);
     JudgmentDescription _get = _allContentsOfType.get(0);
     return _get;
   }
   
   public ErrorSpecification firstErrorSpecification(final EObject o) {
     List<ErrorSpecification> _allContentsOfType = EcoreUtil2.<ErrorSpecification>getAllContentsOfType(o, it.xsemantics.dsl.xsemantics.ErrorSpecification.class);
     ErrorSpecification _get = _allContentsOfType.get(0);
     return _get;
   }
   
   public RuleWithPremises getRuleWithPremises(final XsemanticsSystem ts, final int index) {
     Rule _rule = this.getRule(ts, index);
     RuleWithPremises _ruleWithPremises = this.getRuleWithPremises(_rule);
     return _ruleWithPremises;
   }
   
   public EList<XExpression> getRulePremises(final XsemanticsSystem ts, final int index) {
     RuleWithPremises _ruleWithPremises = this.getRuleWithPremises(ts, index);
     XExpression _premises = _ruleWithPremises.getPremises();
     EList<XExpression> _expressions = ((XBlockExpression) _premises).getExpressions();
     return _expressions;
   }
   
   public RuleWithPremises getRuleWithPremises(final Rule rule) {
     return ((RuleWithPremises) rule);
   }
   
   public EList<XExpression> getRulePremises(final Rule rule) {
     RuleWithPremises _ruleWithPremises = this.getRuleWithPremises(rule);
     XExpression _premises = _ruleWithPremises.getPremises();
     EList<XExpression> _expressions = ((XBlockExpression) _premises).getExpressions();
     return _expressions;
   }
   
   public XBlockExpression getRulePremisesAsBlock(final Rule rule) {
     RuleWithPremises _ruleWithPremises = this.getRuleWithPremises(rule);
     XExpression _premises = _ruleWithPremises.getPremises();
     return ((XBlockExpression) _premises);
   }
   
   public XBlockExpression getRulePremisesAsBlock(final CheckRule rule) {
     XExpression _premises = rule.getPremises();
     return ((XBlockExpression) _premises);
   }
   
   public RuleParameter ruleParameter(final RuleConclusionElement ruleConclusionElement) {
     return ((RuleParameter) ruleConclusionElement);
   }
   
   public RuleParameter ruleParameterByName(final Rule rule, final String name) {
     RuleConclusion _conclusion = rule.getConclusion();
     EList<RuleConclusionElement> _conclusionElements = _conclusion.getConclusionElements();
     List<RuleParameter> _typeSelect = EcoreUtil2.<RuleParameter>typeSelect(_conclusionElements, it.xsemantics.dsl.xsemantics.RuleParameter.class);
     final Function1<RuleParameter,Boolean> _function = new Function1<RuleParameter,Boolean>() {
         public Boolean apply(final RuleParameter it) {
           JvmFormalParameter _parameter = it.getParameter();
           String _name = _parameter.getName();
           boolean _operator_equals = ObjectExtensions.operator_equals(_name, name);
           return Boolean.valueOf(_operator_equals);
         }
       };
     RuleParameter _findFirst = IterableExtensions.<RuleParameter>findFirst(_typeSelect, _function);
     return _findFirst;
   }
   
   public ExpressionInConclusion ruleExpression(final RuleConclusionElement ruleConclusionElement) {
     return ((ExpressionInConclusion) ruleConclusionElement);
   }
   
   public void assertIsInstance(final Class superClass, final Object o) {
     Class<? extends Object> _class = o.getClass();
     String _name = _class.getName();
     String _operator_plus = StringExtensions.operator_plus(_name, " is not an instance of ");
     String _name_1 = superClass.getName();
     String _operator_plus_1 = StringExtensions.operator_plus(_operator_plus, _name_1);
     Class<? extends Object> _class_1 = o.getClass();
     boolean _isAssignableFrom = superClass.isAssignableFrom(_class_1);
     Assert.assertTrue(_operator_plus_1, _isAssignableFrom);
   }
   
   public void assertOrExpression(final XExpression exp, final int branches) {
     OrExpression _orExpression = this.getOrExpression(exp);
     EList<XExpression> _branches = _orExpression.getBranches();
     int _size = _branches.size();
     Assert.assertEquals(branches, _size);
   }
   
   public OrExpression getOrExpression(final XExpression exp) {
     return ((OrExpression) exp);
   }
   
   public RuleInvocation getRuleInvocationFromPremises(final Rule rule) {
     EList<XExpression> _rulePremises = this.getRulePremises(rule);
     XExpression _get = _rulePremises.get(0);
     return ((RuleInvocation) _get);
   }
   
   public EnvironmentComposition getEnvironmentComposition(final EnvironmentSpecification envSpec) {
     return ((EnvironmentComposition) envSpec);
   }
   
   public EnvironmentMapping getEnvironmentMapping(final EnvironmentSpecification envSpec) {
     return ((EnvironmentMapping) envSpec);
   }
   
   public EnvironmentSpecification getEnvironmentSpecificationOfRuleInvocation(final Rule rule) {
     RuleInvocation _ruleInvocationFromPremises = this.getRuleInvocationFromPremises(rule);
     EnvironmentSpecification _environment = _ruleInvocationFromPremises.getEnvironment();
     return _environment;
   }
   
   public XAbstractFeatureCall getXAbstractFeatureCall(final int index) {
     CharSequence _testRuleWithFeatureCalls = this.testFiles.testRuleWithFeatureCalls();
     XsemanticsSystem _parseAndAssertNoError = this.parseAndAssertNoError(_testRuleWithFeatureCalls);
     List<XAbstractFeatureCall> _xAbstractFeatureCalls = this.getXAbstractFeatureCalls(_parseAndAssertNoError);
     XAbstractFeatureCall _get = _xAbstractFeatureCalls.get(index);
     return _get;
   }
   
   public List<XAbstractFeatureCall> getXAbstractFeatureCalls(final XsemanticsSystem ts) {
     EList<Rule> _rules = ts.getRules();
     Rule _get = _rules.get(0);
     EList<XExpression> _rulePremises = this.getRulePremises(_get);
     List<XAbstractFeatureCall> _typeSelect = EcoreUtil2.<XAbstractFeatureCall>typeSelect(_rulePremises, org.eclipse.xtext.xbase.XAbstractFeatureCall.class);
     return _typeSelect;
   }
   
   public EnvironmentAccess getEnvironmentAccess(final XsemanticsSystem ts) {
     EList<Rule> _rules = ts.getRules();
     Rule _get = _rules.get(0);
     EList<XExpression> _rulePremises = this.getRulePremises(_get);
     List<EnvironmentAccess> _typeSelect = EcoreUtil2.<EnvironmentAccess>typeSelect(_rulePremises, it.xsemantics.dsl.xsemantics.EnvironmentAccess.class);
     EnvironmentAccess _get_1 = _typeSelect.get(0);
     return _get_1;
   }
   
   public void assertEqualsStrings(final Object o1, final Object o2) {
     String _operator_plus = StringExtensions.operator_plus("", o1);
     String _operator_plus_1 = StringExtensions.operator_plus("", o2);
     Assert.assertEquals(_operator_plus, _operator_plus_1);
   }
 }
