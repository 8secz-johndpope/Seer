// $ANTLR 3.2 Sep 23, 2009 12:02:23 src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g 2010-07-13 00:45:30
 
     package com.tinkerpop.gremlin.compiler;
 
     import java.util.ArrayList;
     
     import java.util.Map;
     import java.util.HashMap;
     import java.util.Iterator;
     
     import java.util.regex.Pattern;
     import java.util.regex.Matcher;
 
     import java.util.Collections;
     
     import com.tinkerpop.gremlin.Gremlin;
 
     import com.tinkerpop.gremlin.compiler.Tokens;
     import com.tinkerpop.gremlin.compiler.Atom;
     import com.tinkerpop.gremlin.compiler.VariableLibrary;
     import com.tinkerpop.gremlin.compiler.FunctionLibrary;
     import com.tinkerpop.gremlin.compiler.PathLibrary;
 
     // types
     import com.tinkerpop.gremlin.compiler.types.Range;
     import com.tinkerpop.gremlin.compiler.types.GPath;
 
     // operations
     import com.tinkerpop.gremlin.compiler.operations.Operation;
     import com.tinkerpop.gremlin.compiler.operations.UnaryOperation;
 
     import com.tinkerpop.gremlin.compiler.statements.*;
     import com.tinkerpop.gremlin.compiler.operations.math.*;
     import com.tinkerpop.gremlin.compiler.operations.logic.*;
     import com.tinkerpop.gremlin.compiler.operations.util.*;
 
     import com.tinkerpop.gremlin.compiler.functions.Function;
     import com.tinkerpop.gremlin.compiler.functions.NativeFunction;
 
     // blueprints
     import com.tinkerpop.blueprints.pgm.Vertex;
 
     // pipes
     import com.tinkerpop.pipes.Pipe;
     import com.tinkerpop.pipes.Pipeline;
 
     import com.tinkerpop.pipes.SingleIterator;
     import com.tinkerpop.pipes.MultiIterator;
     
     import com.tinkerpop.pipes.pgm.PropertyPipe;
     import com.tinkerpop.pipes.filter.FilterPipe;
     import com.tinkerpop.pipes.filter.FutureFilterPipe;
     
     import com.tinkerpop.gremlin.compiler.pipes.GremlinPipesHelper;
 
 
 import org.antlr.runtime.*;
 import org.antlr.runtime.tree.*;import java.util.Stack;
 import java.util.List;
 import java.util.ArrayList;
 
 
 public class GremlinEvaluator extends TreeParser {
     public static final String[] tokenNames = new String[] {
         "<invalid>", "<EOR>", "<DOWN>", "<UP>", "VAR", "ARG", "ARGS", "FUNC", "NS", "NAME", "FUNC_NAME", "PATH", "GPATH", "STEP", "TOKEN", "PREDICATE", "PREDICATES", "SELF", "HISTORY", "FUNC_CALL", "IF", "COND", "BLOCK", "FOREACH", "WHILE", "REPEAT", "INCLUDE", "INT", "LONG", "FLOAT", "DOUBLE", "STR", "ARR", "BOOL", "NULL", "RANGE", "PROPERTY_CALL", "VARIABLE_CALL", "COLLECTION_CALL", "COMMENT", "VARIABLE", "NEWLINE", "StringLiteral", "IDENTIFIER", "G_INT", "G_LONG", "G_FLOAT", "G_DOUBLE", "BOOLEAN", "PROPERTY", "DoubleStringCharacter", "SingleStringCharacter", "WS", "EscapeSequence", "CharacterEscapeSequence", "HexEscapeSequence", "UnicodeEscapeSequence", "SingleEscapeCharacter", "NonEscapeCharacter", "EscapeCharacter", "DecimalDigit", "HexDigit", "':='", "'/'", "'['", "']'", "'..'", "'and'", "'or'", "'include'", "'if'", "'end'", "'foreach'", "'in'", "'while'", "'repeat'", "'path'", "'func'", "'('", "')'", "','", "'='", "'!='", "'<'", "'<='", "'>'", "'>='", "'+'", "'-'", "'*'", "'div'", "':'", "NUMBER"
     };
     public static final int WHILE=24;
     public static final int DecimalDigit=60;
     public static final int EOF=-1;
     public static final int FUNC_CALL=19;
     public static final int TOKEN=14;
     public static final int SingleStringCharacter=51;
     public static final int HISTORY=18;
     public static final int T__91=91;
     public static final int NAME=9;
     public static final int T__90=90;
     public static final int ARG=5;
     public static final int PATH=11;
     public static final int G_INT=44;
     public static final int INCLUDE=26;
     public static final int SingleEscapeCharacter=57;
     public static final int ARGS=6;
     public static final int DOUBLE=30;
     public static final int VAR=4;
     public static final int GPATH=12;
     public static final int COMMENT=39;
     public static final int T__80=80;
     public static final int T__81=81;
     public static final int T__82=82;
     public static final int T__83=83;
     public static final int NS=8;
     public static final int NULL=34;
     public static final int NUMBER=92;
     public static final int BOOL=33;
     public static final int INT=27;
     public static final int DoubleStringCharacter=50;
     public static final int ARR=32;
     public static final int T__85=85;
     public static final int T__84=84;
     public static final int T__87=87;
     public static final int T__86=86;
     public static final int T__89=89;
     public static final int T__88=88;
     public static final int WS=52;
     public static final int T__71=71;
     public static final int PREDICATES=16;
     public static final int T__72=72;
     public static final int VARIABLE=40;
     public static final int T__70=70;
     public static final int G_DOUBLE=47;
     public static final int PROPERTY=49;
     public static final int FUNC=7;
     public static final int G_LONG=45;
     public static final int FOREACH=23;
     public static final int REPEAT=25;
     public static final int FUNC_NAME=10;
     public static final int CharacterEscapeSequence=54;
     public static final int T__76=76;
     public static final int T__75=75;
     public static final int T__74=74;
     public static final int T__73=73;
     public static final int EscapeSequence=53;
     public static final int T__79=79;
     public static final int T__78=78;
     public static final int T__77=77;
     public static final int T__68=68;
     public static final int T__69=69;
     public static final int T__66=66;
     public static final int T__67=67;
     public static final int T__64=64;
     public static final int T__65=65;
     public static final int T__62=62;
     public static final int T__63=63;
     public static final int HexEscapeSequence=55;
     public static final int STEP=13;
     public static final int FLOAT=29;
     public static final int HexDigit=61;
     public static final int PREDICATE=15;
     public static final int IF=20;
     public static final int STR=31;
     public static final int BOOLEAN=48;
     public static final int IDENTIFIER=43;
     public static final int EscapeCharacter=59;
     public static final int COLLECTION_CALL=38;
     public static final int G_FLOAT=46;
     public static final int PROPERTY_CALL=36;
     public static final int UnicodeEscapeSequence=56;
     public static final int RANGE=35;
     public static final int StringLiteral=42;
     public static final int NEWLINE=41;
     public static final int BLOCK=22;
     public static final int NonEscapeCharacter=58;
     public static final int COND=21;
     public static final int LONG=28;
     public static final int SELF=17;
     public static final int VARIABLE_CALL=37;
 
     // delegates
     // delegators
 
 
         public GremlinEvaluator(TreeNodeStream input) {
             this(input, new RecognizerSharedState());
         }
         public GremlinEvaluator(TreeNodeStream input, RecognizerSharedState state) {
             super(input, state);
              
         }
         
     protected TreeAdaptor adaptor = new CommonTreeAdaptor();
 
     public void setTreeAdaptor(TreeAdaptor adaptor) {
         this.adaptor = adaptor;
     }
     public TreeAdaptor getTreeAdaptor() {
         return adaptor;
     }
 
     public String[] getTokenNames() { return GremlinEvaluator.tokenNames; }
     public String getGrammarFileName() { return "src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g"; }
 
 
         // debug mode
         public  static boolean DEBUG = false;
         public  static boolean EMBEDDED = false;
 
         private boolean isGPath = false;
 
         private Pattern rangePattern = Pattern.compile("^(\\d+)..(\\d+)");
         
         private static VariableLibrary variables = new VariableLibrary();
         public  static FunctionLibrary functions = new FunctionLibrary();
         public  static PathLibrary     paths     = new PathLibrary();
 
         public static void declareVariable(String name, Atom value) {
             variables.declare(name, value);
         }
 
         public static Atom getVariable(String name) {
             return variables.get(name);
         }
 
         public static void freeVariable(String name) {
             variables.free(name);
         }
 
         public static VariableLibrary getVariableLibrary() {
             return variables;
         }
 
         public static void setVariableLibrary(VariableLibrary lib) {
             variables = lib;
         }
 
         private static void formProgramResult(List<Object> resultList, Operation currentOperation) {
             Atom result = currentOperation.compute();
 
             if (EMBEDDED) resultList.add(result.getValue());
 
             //System.out.println(result.getValue().getClass());
             
             if (!result.isNull() && DEBUG) {
                 if (result.isIterable()) {
                     for(Object o : (Iterable)result.getValue()) {
                         System.out.println(Tokens.RESULT_PROMPT + o);
                     }
                 } else if (result.isMap()) {
                     Map map = (Map) result.getValue();
 
                     for (Object key : map.keySet()) {
                         System.out.println(Tokens.RESULT_PROMPT + key + "=" + map.get(key));
                     }
                 } else if(result.getValue() instanceof Iterator) {
                     Iterator itty = (Iterator) result.getValue();
                     
                     while(itty.hasNext()) {
                         System.out.println(Tokens.RESULT_PROMPT + itty.next());
                     }
                 } else {
                     System.out.println(Tokens.RESULT_PROMPT + result);
                 }
             }
 
             if (!(currentOperation instanceof DeclareVariable)) {
                 declareVariable(Tokens.LAST_VARIABLE, result);
             }
             
         }
 
 
     public static class program_return extends TreeRuleReturnScope {
         public Iterable results;
         CommonTree tree;
         public Object getTree() { return tree; }
     };
 
     // $ANTLR start "program"
     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:132:1: program returns [Iterable results] : ( ( statement | col= collection | ^( VAR VARIABLE c= collection ) ) ( NEWLINE )* )+ ;
     public final GremlinEvaluator.program_return program() throws RecognitionException {
         GremlinEvaluator.program_return retval = new GremlinEvaluator.program_return();
         retval.start = input.LT(1);
 
         CommonTree root_0 = null;
 
         CommonTree _first_0 = null;
         CommonTree _last = null;
 
         CommonTree VAR2=null;
         CommonTree VARIABLE3=null;
         CommonTree NEWLINE4=null;
         GremlinEvaluator.collection_return col = null;
 
         GremlinEvaluator.collection_return c = null;
 
         GremlinEvaluator.statement_return statement1 = null;
 
 
         CommonTree VAR2_tree=null;
         CommonTree VARIABLE3_tree=null;
         CommonTree NEWLINE4_tree=null;
 
 
                 List<Object> resultList = new ArrayList<Object>();
             
         try {
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:136:5: ( ( ( statement | col= collection | ^( VAR VARIABLE c= collection ) ) ( NEWLINE )* )+ )
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:136:7: ( ( statement | col= collection | ^( VAR VARIABLE c= collection ) ) ( NEWLINE )* )+
             {
             root_0 = (CommonTree)adaptor.nil();
 
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:136:7: ( ( statement | col= collection | ^( VAR VARIABLE c= collection ) ) ( NEWLINE )* )+
             int cnt3=0;
             loop3:
             do {
                 int alt3=2;
                 int LA3_0 = input.LA(1);
 
                 if ( (LA3_0==VAR||LA3_0==FUNC||(LA3_0>=PATH && LA3_0<=GPATH)||(LA3_0>=FUNC_CALL && LA3_0<=IF)||(LA3_0>=FOREACH && LA3_0<=COLLECTION_CALL)||LA3_0==IDENTIFIER||(LA3_0>=67 && LA3_0<=68)||LA3_0==78||(LA3_0>=81 && LA3_0<=90)) ) {
                     alt3=1;
                 }
 
 
                 switch (alt3) {
             	case 1 :
             	    // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:136:8: ( statement | col= collection | ^( VAR VARIABLE c= collection ) ) ( NEWLINE )*
             	    {
             	    // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:136:8: ( statement | col= collection | ^( VAR VARIABLE c= collection ) )
             	    int alt1=3;
             	    switch ( input.LA(1) ) {
             	    case FUNC:
             	    case PATH:
             	    case GPATH:
             	    case FUNC_CALL:
             	    case IF:
             	    case FOREACH:
             	    case WHILE:
             	    case REPEAT:
             	    case INCLUDE:
             	    case INT:
             	    case LONG:
             	    case FLOAT:
             	    case DOUBLE:
             	    case STR:
             	    case ARR:
             	    case BOOL:
             	    case NULL:
             	    case RANGE:
             	    case PROPERTY_CALL:
             	    case VARIABLE_CALL:
             	    case IDENTIFIER:
             	    case 67:
             	    case 68:
             	    case 78:
             	    case 81:
             	    case 82:
             	    case 83:
             	    case 84:
             	    case 85:
             	    case 86:
             	    case 87:
             	    case 88:
             	    case 89:
             	    case 90:
             	        {
             	        alt1=1;
             	        }
             	        break;
             	    case VAR:
             	        {
             	        int LA1_2 = input.LA(2);
 
             	        if ( (LA1_2==DOWN) ) {
             	            int LA1_4 = input.LA(3);
 
             	            if ( (LA1_4==VARIABLE) ) {
             	                int LA1_5 = input.LA(4);
 
             	                if ( (LA1_5==VAR||LA1_5==FUNC||(LA1_5>=PATH && LA1_5<=GPATH)||(LA1_5>=FUNC_CALL && LA1_5<=IF)||(LA1_5>=FOREACH && LA1_5<=VARIABLE_CALL)||LA1_5==IDENTIFIER||(LA1_5>=67 && LA1_5<=68)||LA1_5==78||(LA1_5>=81 && LA1_5<=90)) ) {
             	                    alt1=1;
             	                }
             	                else if ( (LA1_5==COLLECTION_CALL) ) {
             	                    alt1=3;
             	                }
             	                else {
             	                    NoViableAltException nvae =
             	                        new NoViableAltException("", 1, 5, input);
 
             	                    throw nvae;
             	                }
             	            }
             	            else {
             	                NoViableAltException nvae =
             	                    new NoViableAltException("", 1, 4, input);
 
             	                throw nvae;
             	            }
             	        }
             	        else {
             	            NoViableAltException nvae =
             	                new NoViableAltException("", 1, 2, input);
 
             	            throw nvae;
             	        }
             	        }
             	        break;
             	    case COLLECTION_CALL:
             	        {
             	        alt1=2;
             	        }
             	        break;
             	    default:
             	        NoViableAltException nvae =
             	            new NoViableAltException("", 1, 0, input);
 
             	        throw nvae;
             	    }
 
             	    switch (alt1) {
             	        case 1 :
             	            // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:136:9: statement
             	            {
             	            _last = (CommonTree)input.LT(1);
             	            pushFollow(FOLLOW_statement_in_program60);
             	            statement1=statement();
 
             	            state._fsp--;
 
             	            adaptor.addChild(root_0, statement1.getTree());
 
             	                    formProgramResult(resultList, (statement1!=null?statement1.op:null));
             	                 
 
             	            }
             	            break;
             	        case 2 :
             	            // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:139:10: col= collection
             	            {
             	            _last = (CommonTree)input.LT(1);
             	            pushFollow(FOLLOW_collection_in_program73);
             	            col=collection();
 
             	            state._fsp--;
 
             	            adaptor.addChild(root_0, col.getTree());
 
             	                    formProgramResult(resultList, (col!=null?col.op:null));
             	                 
 
             	            }
             	            break;
             	        case 3 :
             	            // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:141:10: ^( VAR VARIABLE c= collection )
             	            {
             	            _last = (CommonTree)input.LT(1);
             	            {
             	            CommonTree _save_last_1 = _last;
             	            CommonTree _first_1 = null;
             	            CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             	            VAR2=(CommonTree)match(input,VAR,FOLLOW_VAR_in_program80); 
             	            VAR2_tree = (CommonTree)adaptor.dupNode(VAR2);
 
             	            root_1 = (CommonTree)adaptor.becomeRoot(VAR2_tree, root_1);
 
 
 
             	            match(input, Token.DOWN, null); 
             	            _last = (CommonTree)input.LT(1);
             	            VARIABLE3=(CommonTree)match(input,VARIABLE,FOLLOW_VARIABLE_in_program82); 
             	            VARIABLE3_tree = (CommonTree)adaptor.dupNode(VARIABLE3);
 
             	            adaptor.addChild(root_1, VARIABLE3_tree);
 
             	            _last = (CommonTree)input.LT(1);
             	            pushFollow(FOLLOW_collection_in_program86);
             	            c=collection();
 
             	            state._fsp--;
 
             	            adaptor.addChild(root_1, c.getTree());
 
             	            match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
             	            }
 
 
             	                    formProgramResult(resultList, new DeclareVariable((VARIABLE3!=null?VARIABLE3.getText():null), (c!=null?c.op:null))); 
             	                 
 
             	            }
             	            break;
 
             	    }
 
             	    // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:143:9: ( NEWLINE )*
             	    loop2:
             	    do {
             	        int alt2=2;
             	        int LA2_0 = input.LA(1);
 
             	        if ( (LA2_0==NEWLINE) ) {
             	            alt2=1;
             	        }
 
 
             	        switch (alt2) {
             	    	case 1 :
             	    	    // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:143:9: NEWLINE
             	    	    {
             	    	    _last = (CommonTree)input.LT(1);
             	    	    NEWLINE4=(CommonTree)match(input,NEWLINE,FOLLOW_NEWLINE_in_program92); 
             	    	    NEWLINE4_tree = (CommonTree)adaptor.dupNode(NEWLINE4);
 
             	    	    adaptor.addChild(root_0, NEWLINE4_tree);
 
 
             	    	    }
             	    	    break;
 
             	    	default :
             	    	    break loop2;
             	        }
             	    } while (true);
 
 
             	    }
             	    break;
 
             	default :
             	    if ( cnt3 >= 1 ) break loop3;
                         EarlyExitException eee =
                             new EarlyExitException(3, input);
                         throw eee;
                 }
                 cnt3++;
             } while (true);
 
 
                     retval.results = resultList;
                  
 
             }
 
             retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
 
         }
         catch (RecognitionException re) {
             reportError(re);
             recover(input,re);
         }
         finally {
         }
         return retval;
     }
     // $ANTLR end "program"
 
     public static class statement_return extends TreeRuleReturnScope {
         public Operation op;
         CommonTree tree;
         public Object getTree() { return tree; }
     };
 
     // $ANTLR start "statement"
     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:149:1: statement returns [Operation op] : ( if_statement | foreach_statement | while_statement | repeat_statement | path_definition_statement | function_definition_statement | include_statement | gpath_statement | ^( VAR VARIABLE s= statement ) | ^( 'and' a= statement b= statement ) | ^( 'or' a= statement b= statement ) | expression );
     public final GremlinEvaluator.statement_return statement() throws RecognitionException {
         GremlinEvaluator.statement_return retval = new GremlinEvaluator.statement_return();
         retval.start = input.LT(1);
 
         CommonTree root_0 = null;
 
         CommonTree _first_0 = null;
         CommonTree _last = null;
 
         CommonTree VAR13=null;
         CommonTree VARIABLE14=null;
         CommonTree string_literal15=null;
         CommonTree string_literal16=null;
         GremlinEvaluator.statement_return s = null;
 
         GremlinEvaluator.statement_return a = null;
 
         GremlinEvaluator.statement_return b = null;
 
         GremlinEvaluator.if_statement_return if_statement5 = null;
 
         GremlinEvaluator.foreach_statement_return foreach_statement6 = null;
 
         GremlinEvaluator.while_statement_return while_statement7 = null;
 
         GremlinEvaluator.repeat_statement_return repeat_statement8 = null;
 
         GremlinEvaluator.path_definition_statement_return path_definition_statement9 = null;
 
         GremlinEvaluator.function_definition_statement_return function_definition_statement10 = null;
 
         GremlinEvaluator.include_statement_return include_statement11 = null;
 
         GremlinEvaluator.gpath_statement_return gpath_statement12 = null;
 
         GremlinEvaluator.expression_return expression17 = null;
 
 
         CommonTree VAR13_tree=null;
         CommonTree VARIABLE14_tree=null;
         CommonTree string_literal15_tree=null;
         CommonTree string_literal16_tree=null;
 
         try {
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:150:2: ( if_statement | foreach_statement | while_statement | repeat_statement | path_definition_statement | function_definition_statement | include_statement | gpath_statement | ^( VAR VARIABLE s= statement ) | ^( 'and' a= statement b= statement ) | ^( 'or' a= statement b= statement ) | expression )
             int alt4=12;
             switch ( input.LA(1) ) {
             case IF:
                 {
                 alt4=1;
                 }
                 break;
             case FOREACH:
                 {
                 alt4=2;
                 }
                 break;
             case WHILE:
                 {
                 alt4=3;
                 }
                 break;
             case REPEAT:
                 {
                 alt4=4;
                 }
                 break;
             case PATH:
                 {
                 alt4=5;
                 }
                 break;
             case FUNC:
                 {
                 alt4=6;
                 }
                 break;
             case INCLUDE:
                 {
                 alt4=7;
                 }
                 break;
             case GPATH:
                 {
                 alt4=8;
                 }
                 break;
             case VAR:
                 {
                 alt4=9;
                 }
                 break;
             case 67:
                 {
                 alt4=10;
                 }
                 break;
             case 68:
                 {
                 alt4=11;
                 }
                 break;
             case FUNC_CALL:
             case INT:
             case LONG:
             case FLOAT:
             case DOUBLE:
             case STR:
             case ARR:
             case BOOL:
             case NULL:
             case RANGE:
             case PROPERTY_CALL:
             case VARIABLE_CALL:
             case IDENTIFIER:
             case 78:
             case 81:
             case 82:
             case 83:
             case 84:
             case 85:
             case 86:
             case 87:
             case 88:
             case 89:
             case 90:
                 {
                 alt4=12;
                 }
                 break;
             default:
                 NoViableAltException nvae =
                     new NoViableAltException("", 4, 0, input);
 
                 throw nvae;
             }
 
             switch (alt4) {
                 case 1 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:150:4: if_statement
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_if_statement_in_statement120);
                     if_statement5=if_statement();
 
                     state._fsp--;
 
                     adaptor.addChild(root_0, if_statement5.getTree());
                      retval.op = (if_statement5!=null?if_statement5.op:null); 
 
                     }
                     break;
                 case 2 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:151:4: foreach_statement
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_foreach_statement_in_statement150);
                     foreach_statement6=foreach_statement();
 
                     state._fsp--;
 
                     adaptor.addChild(root_0, foreach_statement6.getTree());
                      retval.op = (foreach_statement6!=null?foreach_statement6.op:null); 
 
                     }
                     break;
                 case 3 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:152:7: while_statement
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_while_statement_in_statement178);
                     while_statement7=while_statement();
 
                     state._fsp--;
 
                     adaptor.addChild(root_0, while_statement7.getTree());
                      retval.op = (while_statement7!=null?while_statement7.op:null); 
 
                     }
                     break;
                 case 4 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:153:4: repeat_statement
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_repeat_statement_in_statement205);
                     repeat_statement8=repeat_statement();
 
                     state._fsp--;
 
                     adaptor.addChild(root_0, repeat_statement8.getTree());
                      retval.op = (repeat_statement8!=null?repeat_statement8.op:null); 
 
                     }
                     break;
                 case 5 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:154:4: path_definition_statement
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_path_definition_statement_in_statement231);
                     path_definition_statement9=path_definition_statement();
 
                     state._fsp--;
 
                     adaptor.addChild(root_0, path_definition_statement9.getTree());
                      retval.op = (path_definition_statement9!=null?path_definition_statement9.op:null); 
 
                     }
                     break;
                 case 6 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:155:4: function_definition_statement
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_function_definition_statement_in_statement248);
                     function_definition_statement10=function_definition_statement();
 
                     state._fsp--;
 
                     adaptor.addChild(root_0, function_definition_statement10.getTree());
                      retval.op = (function_definition_statement10!=null?function_definition_statement10.op:null); 
 
                     }
                     break;
                 case 7 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:156:4: include_statement
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_include_statement_in_statement261);
                     include_statement11=include_statement();
 
                     state._fsp--;
 
                     adaptor.addChild(root_0, include_statement11.getTree());
                      retval.op = new UnaryOperation((include_statement11!=null?include_statement11.result:null)); 
 
                     }
                     break;
                 case 8 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:157:4: gpath_statement
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_gpath_statement_in_statement286);
                     gpath_statement12=gpath_statement();
 
                     state._fsp--;
 
                     adaptor.addChild(root_0, gpath_statement12.getTree());
                      retval.op = (gpath_statement12!=null?gpath_statement12.op:null); 
 
                     }
                     break;
                 case 9 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:158:4: ^( VAR VARIABLE s= statement )
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_1 = _last;
                     CommonTree _first_1 = null;
                     CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     VAR13=(CommonTree)match(input,VAR,FOLLOW_VAR_in_statement314); 
                     VAR13_tree = (CommonTree)adaptor.dupNode(VAR13);
 
                     root_1 = (CommonTree)adaptor.becomeRoot(VAR13_tree, root_1);
 
 
 
                     match(input, Token.DOWN, null); 
                     _last = (CommonTree)input.LT(1);
                     VARIABLE14=(CommonTree)match(input,VARIABLE,FOLLOW_VARIABLE_in_statement316); 
                     VARIABLE14_tree = (CommonTree)adaptor.dupNode(VARIABLE14);
 
                     adaptor.addChild(root_1, VARIABLE14_tree);
 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_statement_in_statement320);
                     s=statement();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, s.getTree());
 
                     match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
                     }
 
                      retval.op = new DeclareVariable((VARIABLE14!=null?VARIABLE14.getText():null), (s!=null?s.op:null)); 
 
                     }
                     break;
                 case 10 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:159:9: ^( 'and' a= statement b= statement )
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_1 = _last;
                     CommonTree _first_1 = null;
                     CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     string_literal15=(CommonTree)match(input,67,FOLLOW_67_in_statement342); 
                     string_literal15_tree = (CommonTree)adaptor.dupNode(string_literal15);
 
                     root_1 = (CommonTree)adaptor.becomeRoot(string_literal15_tree, root_1);
 
 
 
                     match(input, Token.DOWN, null); 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_statement_in_statement346);
                     a=statement();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, a.getTree());
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_statement_in_statement350);
                     b=statement();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, b.getTree());
 
                     match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
                     }
 
                      retval.op = new And((a!=null?a.op:null), (b!=null?b.op:null)); 
 
                     }
                     break;
                 case 11 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:160:9: ^( 'or' a= statement b= statement )
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_1 = _last;
                     CommonTree _first_1 = null;
                     CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     string_literal16=(CommonTree)match(input,68,FOLLOW_68_in_statement367); 
                     string_literal16_tree = (CommonTree)adaptor.dupNode(string_literal16);
 
                     root_1 = (CommonTree)adaptor.becomeRoot(string_literal16_tree, root_1);
 
 
 
                     match(input, Token.DOWN, null); 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_statement_in_statement372);
                     a=statement();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, a.getTree());
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_statement_in_statement376);
                     b=statement();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, b.getTree());
 
                     match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
                     }
 
                      retval.op = new Or((a!=null?a.op:null), (b!=null?b.op:null)); 
 
                     }
                     break;
                 case 12 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:161:9: expression
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_expression_in_statement392);
                     expression17=expression();
 
                     state._fsp--;
 
                     adaptor.addChild(root_0, expression17.getTree());
                      retval.op = (expression17!=null?expression17.expr:null); 
 
                     }
                     break;
 
             }
             retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
 
         }
         catch (RecognitionException re) {
             reportError(re);
             recover(input,re);
         }
         finally {
         }
         return retval;
     }
     // $ANTLR end "statement"
 
     public static class include_statement_return extends TreeRuleReturnScope {
         public Atom result;
         CommonTree tree;
         public Object getTree() { return tree; }
     };
 
     // $ANTLR start "include_statement"
     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:164:1: include_statement returns [Atom result] : ^( INCLUDE StringLiteral ) ;
     public final GremlinEvaluator.include_statement_return include_statement() throws RecognitionException {
         GremlinEvaluator.include_statement_return retval = new GremlinEvaluator.include_statement_return();
         retval.start = input.LT(1);
 
         CommonTree root_0 = null;
 
         CommonTree _first_0 = null;
         CommonTree _last = null;
 
         CommonTree INCLUDE18=null;
         CommonTree StringLiteral19=null;
 
         CommonTree INCLUDE18_tree=null;
         CommonTree StringLiteral19_tree=null;
 
         try {
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:165:2: ( ^( INCLUDE StringLiteral ) )
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:165:4: ^( INCLUDE StringLiteral )
             {
             root_0 = (CommonTree)adaptor.nil();
 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_1 = _last;
             CommonTree _first_1 = null;
             CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             INCLUDE18=(CommonTree)match(input,INCLUDE,FOLLOW_INCLUDE_in_include_statement435); 
             INCLUDE18_tree = (CommonTree)adaptor.dupNode(INCLUDE18);
 
             root_1 = (CommonTree)adaptor.becomeRoot(INCLUDE18_tree, root_1);
 
 
 
             match(input, Token.DOWN, null); 
             _last = (CommonTree)input.LT(1);
             StringLiteral19=(CommonTree)match(input,StringLiteral,FOLLOW_StringLiteral_in_include_statement437); 
             StringLiteral19_tree = (CommonTree)adaptor.dupNode(StringLiteral19);
 
             adaptor.addChild(root_1, StringLiteral19_tree);
 
 
             match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
             }
 
 
                         retval.result = new Atom(true);
 
                         String filename = (StringLiteral19!=null?StringLiteral19.getText():null);
                         try {
                             ANTLRFileStream file = new ANTLRFileStream(filename.substring(1, filename.length() - 1));
                             Gremlin.evaluate(file);
                         } catch(Exception e) {
                             retval.result = new Atom(false);
                         }
                     
 
             }
 
             retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
 
         }
         catch (RecognitionException re) {
             reportError(re);
             recover(input,re);
         }
         finally {
         }
         return retval;
     }
     // $ANTLR end "include_statement"
 
     public static class path_definition_statement_return extends TreeRuleReturnScope {
         public Operation op;
         CommonTree tree;
         public Object getTree() { return tree; }
     };
 
     // $ANTLR start "path_definition_statement"
     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:179:1: path_definition_statement returns [Operation op] : ^( PATH path_name= IDENTIFIER (gpath= gpath_statement | ^( PROPERTY_CALL pr= PROPERTY ) ) ) ;
     public final GremlinEvaluator.path_definition_statement_return path_definition_statement() throws RecognitionException {
         GremlinEvaluator.path_definition_statement_return retval = new GremlinEvaluator.path_definition_statement_return();
         retval.start = input.LT(1);
 
         CommonTree root_0 = null;
 
         CommonTree _first_0 = null;
         CommonTree _last = null;
 
         CommonTree path_name=null;
         CommonTree pr=null;
         CommonTree PATH20=null;
         CommonTree PROPERTY_CALL21=null;
         GremlinEvaluator.gpath_statement_return gpath = null;
 
 
         CommonTree path_name_tree=null;
         CommonTree pr_tree=null;
         CommonTree PATH20_tree=null;
         CommonTree PROPERTY_CALL21_tree=null;
 
 
                 List<Pipe> pipes = new ArrayList<Pipe>();
             
         try {
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:183:2: ( ^( PATH path_name= IDENTIFIER (gpath= gpath_statement | ^( PROPERTY_CALL pr= PROPERTY ) ) ) )
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:183:4: ^( PATH path_name= IDENTIFIER (gpath= gpath_statement | ^( PROPERTY_CALL pr= PROPERTY ) ) )
             {
             root_0 = (CommonTree)adaptor.nil();
 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_1 = _last;
             CommonTree _first_1 = null;
             CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             PATH20=(CommonTree)match(input,PATH,FOLLOW_PATH_in_path_definition_statement474); 
             PATH20_tree = (CommonTree)adaptor.dupNode(PATH20);
 
             root_1 = (CommonTree)adaptor.becomeRoot(PATH20_tree, root_1);
 
 
 
             match(input, Token.DOWN, null); 
             _last = (CommonTree)input.LT(1);
             path_name=(CommonTree)match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_path_definition_statement478); 
             path_name_tree = (CommonTree)adaptor.dupNode(path_name);
 
             adaptor.addChild(root_1, path_name_tree);
 
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:183:32: (gpath= gpath_statement | ^( PROPERTY_CALL pr= PROPERTY ) )
             int alt5=2;
             int LA5_0 = input.LA(1);
 
             if ( (LA5_0==GPATH) ) {
                 alt5=1;
             }
             else if ( (LA5_0==PROPERTY_CALL) ) {
                 alt5=2;
             }
             else {
                 NoViableAltException nvae =
                     new NoViableAltException("", 5, 0, input);
 
                 throw nvae;
             }
             switch (alt5) {
                 case 1 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:183:33: gpath= gpath_statement
                     {
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_gpath_statement_in_path_definition_statement483);
                     gpath=gpath_statement();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, gpath.getTree());
                      pipes.addAll(((GPathOperation)(gpath!=null?gpath.op:null)).getPipes()); 
 
                     }
                     break;
                 case 2 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:183:115: ^( PROPERTY_CALL pr= PROPERTY )
                     {
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_2 = _last;
                     CommonTree _first_2 = null;
                     CommonTree root_2 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     PROPERTY_CALL21=(CommonTree)match(input,PROPERTY_CALL,FOLLOW_PROPERTY_CALL_in_path_definition_statement490); 
                     PROPERTY_CALL21_tree = (CommonTree)adaptor.dupNode(PROPERTY_CALL21);
 
                     root_2 = (CommonTree)adaptor.becomeRoot(PROPERTY_CALL21_tree, root_2);
 
 
 
                     match(input, Token.DOWN, null); 
                     _last = (CommonTree)input.LT(1);
                     pr=(CommonTree)match(input,PROPERTY,FOLLOW_PROPERTY_in_path_definition_statement494); 
                     pr_tree = (CommonTree)adaptor.dupNode(pr);
 
                     adaptor.addChild(root_2, pr_tree);
 
 
                     match(input, Token.UP, null); adaptor.addChild(root_1, root_2);_last = _save_last_2;
                     }
 
                      pipes.add(new PropertyPipe((pr!=null?pr.getText():null).substring(1))); 
 
                     }
                     break;
 
             }
 
 
             match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
             }
 
 
                         paths.registerPath((path_name!=null?path_name.getText():null), pipes);
                         retval.op = new UnaryOperation(new Atom(null));
                     
 
             }
 
             retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
 
         }
         catch (RecognitionException re) {
             reportError(re);
             recover(input,re);
         }
         finally {
         }
         return retval;
     }
     // $ANTLR end "path_definition_statement"
 
     protected static class gpath_statement_scope {
         int pipeCount;
         Object startPoint;
         List<Pipe> pipeList;
     }
     protected Stack gpath_statement_stack = new Stack();
 
     public static class gpath_statement_return extends TreeRuleReturnScope {
         public Operation op;
         CommonTree tree;
         public Object getTree() { return tree; }
     };
 
     // $ANTLR start "gpath_statement"
     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:191:1: gpath_statement returns [Operation op] : ^( GPATH ( step )+ ) ;
     public final GremlinEvaluator.gpath_statement_return gpath_statement() throws RecognitionException {
         gpath_statement_stack.push(new gpath_statement_scope());
         GremlinEvaluator.gpath_statement_return retval = new GremlinEvaluator.gpath_statement_return();
         retval.start = input.LT(1);
 
         CommonTree root_0 = null;
 
         CommonTree _first_0 = null;
         CommonTree _last = null;
 
         CommonTree GPATH22=null;
         GremlinEvaluator.step_return step23 = null;
 
 
         CommonTree GPATH22_tree=null;
 
 
                 isGPath = true;
                 
                 ((gpath_statement_scope)gpath_statement_stack.peek()).pipeCount = 0;
                 ((gpath_statement_scope)gpath_statement_stack.peek()).startPoint = null;
                 ((gpath_statement_scope)gpath_statement_stack.peek()).pipeList = new ArrayList<Pipe>();
             
         try {
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:208:2: ( ^( GPATH ( step )+ ) )
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:208:4: ^( GPATH ( step )+ )
             {
             root_0 = (CommonTree)adaptor.nil();
 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_1 = _last;
             CommonTree _first_1 = null;
             CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             GPATH22=(CommonTree)match(input,GPATH,FOLLOW_GPATH_in_gpath_statement553); 
             GPATH22_tree = (CommonTree)adaptor.dupNode(GPATH22);
 
             root_1 = (CommonTree)adaptor.becomeRoot(GPATH22_tree, root_1);
 
 
 
             match(input, Token.DOWN, null); 
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:208:12: ( step )+
             int cnt6=0;
             loop6:
             do {
                 int alt6=2;
                 int LA6_0 = input.LA(1);
 
                 if ( (LA6_0==STEP) ) {
                     alt6=1;
                 }
 
 
                 switch (alt6) {
             	case 1 :
             	    // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:208:13: step
             	    {
             	    _last = (CommonTree)input.LT(1);
             	    pushFollow(FOLLOW_step_in_gpath_statement556);
             	    step23=step();
 
             	    state._fsp--;
 
             	    adaptor.addChild(root_1, step23.getTree());
 
             	    }
             	    break;
 
             	default :
             	    if ( cnt6 >= 1 ) break loop6;
                         EarlyExitException eee =
                             new EarlyExitException(6, input);
                         throw eee;
                 }
                 cnt6++;
             } while (true);
 
 
             match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
             }
 
 
                         if (((gpath_statement_scope)gpath_statement_stack.peek()).pipeList.size() > 0) {
                             retval.op = new GPathOperation(((gpath_statement_scope)gpath_statement_stack.peek()).pipeList, ((gpath_statement_scope)gpath_statement_stack.peek()).startPoint);
                         } else {
                             retval.op = new UnaryOperation(new Atom(null));
                         }
                     
 
             }
 
             retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
 
 
                     isGPath = false;
                 
         }
         catch (RecognitionException re) {
             reportError(re);
             recover(input,re);
         }
         finally {
             gpath_statement_stack.pop();
         }
         return retval;
     }
     // $ANTLR end "gpath_statement"
 
     public static class step_return extends TreeRuleReturnScope {
         CommonTree tree;
         public Object getTree() { return tree; }
     };
 
     // $ANTLR start "step"
     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:218:1: step : ^( STEP ^( TOKEN token ) ^( PREDICATES ( ^( PREDICATE statement ) )* ) ) ;
     public final GremlinEvaluator.step_return step() throws RecognitionException {
         GremlinEvaluator.step_return retval = new GremlinEvaluator.step_return();
         retval.start = input.LT(1);
 
         CommonTree root_0 = null;
 
         CommonTree _first_0 = null;
         CommonTree _last = null;
 
         CommonTree STEP24=null;
         CommonTree TOKEN25=null;
         CommonTree PREDICATES27=null;
         CommonTree PREDICATE28=null;
         GremlinEvaluator.token_return token26 = null;
 
         GremlinEvaluator.statement_return statement29 = null;
 
 
         CommonTree STEP24_tree=null;
         CommonTree TOKEN25_tree=null;
         CommonTree PREDICATES27_tree=null;
         CommonTree PREDICATE28_tree=null;
 
 
                 List<Operation> predicates = new ArrayList<Operation>();
             
         try {
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:222:5: ( ^( STEP ^( TOKEN token ) ^( PREDICATES ( ^( PREDICATE statement ) )* ) ) )
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:222:7: ^( STEP ^( TOKEN token ) ^( PREDICATES ( ^( PREDICATE statement ) )* ) )
             {
             root_0 = (CommonTree)adaptor.nil();
 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_1 = _last;
             CommonTree _first_1 = null;
             CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             STEP24=(CommonTree)match(input,STEP,FOLLOW_STEP_in_step594); 
             STEP24_tree = (CommonTree)adaptor.dupNode(STEP24);
 
             root_1 = (CommonTree)adaptor.becomeRoot(STEP24_tree, root_1);
 
 
 
             match(input, Token.DOWN, null); 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_2 = _last;
             CommonTree _first_2 = null;
             CommonTree root_2 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             TOKEN25=(CommonTree)match(input,TOKEN,FOLLOW_TOKEN_in_step597); 
             TOKEN25_tree = (CommonTree)adaptor.dupNode(TOKEN25);
 
             root_2 = (CommonTree)adaptor.becomeRoot(TOKEN25_tree, root_2);
 
 
 
             match(input, Token.DOWN, null); 
             _last = (CommonTree)input.LT(1);
             pushFollow(FOLLOW_token_in_step599);
             token26=token();
 
             state._fsp--;
 
             adaptor.addChild(root_2, token26.getTree());
 
             match(input, Token.UP, null); adaptor.addChild(root_1, root_2);_last = _save_last_2;
             }
 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_2 = _last;
             CommonTree _first_2 = null;
             CommonTree root_2 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             PREDICATES27=(CommonTree)match(input,PREDICATES,FOLLOW_PREDICATES_in_step603); 
             PREDICATES27_tree = (CommonTree)adaptor.dupNode(PREDICATES27);
 
             root_2 = (CommonTree)adaptor.becomeRoot(PREDICATES27_tree, root_2);
 
 
 
             if ( input.LA(1)==Token.DOWN ) {
                 match(input, Token.DOWN, null); 
                 // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:222:42: ( ^( PREDICATE statement ) )*
                 loop7:
                 do {
                     int alt7=2;
                     int LA7_0 = input.LA(1);
 
                     if ( (LA7_0==PREDICATE) ) {
                         alt7=1;
                     }
 
 
                     switch (alt7) {
                 	case 1 :
                 	    // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:222:44: ^( PREDICATE statement )
                 	    {
                 	    _last = (CommonTree)input.LT(1);
                 	    {
                 	    CommonTree _save_last_3 = _last;
                 	    CommonTree _first_3 = null;
                 	    CommonTree root_3 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                 	    PREDICATE28=(CommonTree)match(input,PREDICATE,FOLLOW_PREDICATE_in_step608); 
                 	    PREDICATE28_tree = (CommonTree)adaptor.dupNode(PREDICATE28);
 
                 	    root_3 = (CommonTree)adaptor.becomeRoot(PREDICATE28_tree, root_3);
 
 
 
                 	    match(input, Token.DOWN, null); 
                 	    _last = (CommonTree)input.LT(1);
                 	    pushFollow(FOLLOW_statement_in_step610);
                 	    statement29=statement();
 
                 	    state._fsp--;
 
                 	    adaptor.addChild(root_3, statement29.getTree());
                 	     predicates.add((statement29!=null?statement29.op:null)); 
 
                 	    match(input, Token.UP, null); adaptor.addChild(root_2, root_3);_last = _save_last_3;
                 	    }
 
 
                 	    }
                 	    break;
 
                 	default :
                 	    break loop7;
                     }
                 } while (true);
 
 
                 match(input, Token.UP, null); 
             }adaptor.addChild(root_1, root_2);_last = _save_last_2;
             }
 
 
             match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
             }
 
 
                         Atom tokenAtom = (token26!=null?token26.atom:null);
                         
                         if (tokenAtom != null) {
                             if (((gpath_statement_scope)gpath_statement_stack.peek()).pipeCount == 0) {
                                 if (tokenAtom.isIdentifier() && ((String)tokenAtom.getValue()).equals(".")) {
                                     ((gpath_statement_scope)gpath_statement_stack.peek()).startPoint = GremlinEvaluator.getVariable(Tokens.ROOT_VARIABLE).getValue();
                                 } else if (paths.isPath(tokenAtom.getValue().toString())) {
                                     ((gpath_statement_scope)gpath_statement_stack.peek()).pipeList.addAll(paths.getPath((String)tokenAtom.getValue()));
                                     ((gpath_statement_scope)gpath_statement_stack.peek()).startPoint = GremlinEvaluator.getVariable(Tokens.ROOT_VARIABLE).getValue();
                                 } else {
                                     ((gpath_statement_scope)gpath_statement_stack.peek()).startPoint = tokenAtom.getValue();
                                 }
 
                                 ((gpath_statement_scope)gpath_statement_stack.peek()).pipeList.addAll(GremlinPipesHelper.pipesForStep(predicates));
                             } else {
                                 ((gpath_statement_scope)gpath_statement_stack.peek()).pipeList.addAll(GremlinPipesHelper.pipesForStep((token26!=null?token26.atom:null), predicates));
                             }
                         }
                         
                         ((gpath_statement_scope)gpath_statement_stack.peek()).pipeCount++;
                     
 
             }
 
             retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
 
         }
         catch (RecognitionException re) {
             reportError(re);
             recover(input,re);
         }
         finally {
         }
         return retval;
     }
     // $ANTLR end "step"
 
     public static class token_return extends TreeRuleReturnScope {
         public Atom atom;
         CommonTree tree;
         public Object getTree() { return tree; }
     };
 
     // $ANTLR start "token"
     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:247:1: token returns [Atom atom] : ( expression | gpath_statement | collection | '..' );
     public final GremlinEvaluator.token_return token() throws RecognitionException {
         GremlinEvaluator.token_return retval = new GremlinEvaluator.token_return();
         retval.start = input.LT(1);
 
         CommonTree root_0 = null;
 
         CommonTree _first_0 = null;
         CommonTree _last = null;
 
         CommonTree string_literal33=null;
         GremlinEvaluator.expression_return expression30 = null;
 
         GremlinEvaluator.gpath_statement_return gpath_statement31 = null;
 
         GremlinEvaluator.collection_return collection32 = null;
 
 
         CommonTree string_literal33_tree=null;
 
         try {
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:248:5: ( expression | gpath_statement | collection | '..' )
             int alt8=4;
             switch ( input.LA(1) ) {
             case FUNC_CALL:
             case INT:
             case LONG:
             case FLOAT:
             case DOUBLE:
             case STR:
             case ARR:
             case BOOL:
             case NULL:
             case RANGE:
             case PROPERTY_CALL:
             case VARIABLE_CALL:
             case IDENTIFIER:
             case 78:
             case 81:
             case 82:
             case 83:
             case 84:
             case 85:
             case 86:
             case 87:
             case 88:
             case 89:
             case 90:
                 {
                 alt8=1;
                 }
                 break;
             case GPATH:
                 {
                 alt8=2;
                 }
                 break;
             case COLLECTION_CALL:
                 {
                 alt8=3;
                 }
                 break;
             case 66:
                 {
                 alt8=4;
                 }
                 break;
             default:
                 NoViableAltException nvae =
                     new NoViableAltException("", 8, 0, input);
 
                 throw nvae;
             }
 
             switch (alt8) {
                 case 1 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:248:8: expression
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_expression_in_token652);
                     expression30=expression();
 
                     state._fsp--;
 
                     adaptor.addChild(root_0, expression30.getTree());
                      retval.atom = (expression30!=null?expression30.expr:null).compute(); 
 
                     }
                     break;
                 case 2 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:249:9: gpath_statement
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_gpath_statement_in_token664);
                     gpath_statement31=gpath_statement();
 
                     state._fsp--;
 
                     adaptor.addChild(root_0, gpath_statement31.getTree());
                      retval.atom = (gpath_statement31!=null?gpath_statement31.op:null).compute(); 
 
                     }
                     break;
                 case 3 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:250:9: collection
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_collection_in_token676);
                     collection32=collection();
 
                     state._fsp--;
 
                     adaptor.addChild(root_0, collection32.getTree());
                      retval.atom = (collection32!=null?collection32.op:null).compute(); 
 
                     }
                     break;
                 case 4 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:251:9: '..'
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     string_literal33=(CommonTree)match(input,66,FOLLOW_66_in_token688); 
                     string_literal33_tree = (CommonTree)adaptor.dupNode(string_literal33);
 
                     adaptor.addChild(root_0, string_literal33_tree);
 
 
 
                                             List<Pipe> history = new ArrayList<Pipe>();
                                             List<Pipe> newPipes = new ArrayList<Pipe>();
                                             List<Pipe> pipes = ((gpath_statement_scope)gpath_statement_stack.peek()).pipeList;
 
                                             if ((pipes.size() == 1 && (pipes.get(0) instanceof FutureFilterPipe)) || pipes.size() == 0) {
                                                 ((gpath_statement_scope)gpath_statement_stack.peek()).pipeList = new ArrayList();
                                             } else {
                                                 int idx;
                                                 
                                                 for (idx = pipes.size() - 1; idx >= 0; idx--) {
                                                     Pipe currentPipe = pipes.get(idx);
                                                     history.add(currentPipe);
 
                                                     if (!(currentPipe instanceof FilterPipe)) break;
                                                 }
 
                                                 for (int i = 0; i < idx; i++) {
                                                     newPipes.add(pipes.get(i));
                                                 }
 
                                                 // don't like that - fix. PY
                                                 Collections.reverse(history);
                                                 newPipes.add(new FutureFilterPipe(new Pipeline(history)));
                                             
                                                 ((gpath_statement_scope)gpath_statement_stack.peek()).pipeList = newPipes;
                                             }
                                             
                                             retval.atom = null;
                                        
 
                     }
                     break;
 
             }
             retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
 
         }
         catch (RecognitionException re) {
             reportError(re);
             recover(input,re);
         }
         finally {
         }
         return retval;
     }
     // $ANTLR end "token"
 
     public static class if_statement_return extends TreeRuleReturnScope {
         public Operation op;
         CommonTree tree;
         public Object getTree() { return tree; }
     };
 
     // $ANTLR start "if_statement"
     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:285:1: if_statement returns [Operation op] : ^( IF ^( COND cond= statement ) block ) ;
     public final GremlinEvaluator.if_statement_return if_statement() throws RecognitionException {
         GremlinEvaluator.if_statement_return retval = new GremlinEvaluator.if_statement_return();
         retval.start = input.LT(1);
 
         CommonTree root_0 = null;
 
         CommonTree _first_0 = null;
         CommonTree _last = null;
 
         CommonTree IF34=null;
         CommonTree COND35=null;
         GremlinEvaluator.statement_return cond = null;
 
         GremlinEvaluator.block_return block36 = null;
 
 
         CommonTree IF34_tree=null;
         CommonTree COND35_tree=null;
 
         try {
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:286:2: ( ^( IF ^( COND cond= statement ) block ) )
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:286:4: ^( IF ^( COND cond= statement ) block )
             {
             root_0 = (CommonTree)adaptor.nil();
 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_1 = _last;
             CommonTree _first_1 = null;
             CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             IF34=(CommonTree)match(input,IF,FOLLOW_IF_in_if_statement713); 
             IF34_tree = (CommonTree)adaptor.dupNode(IF34);
 
             root_1 = (CommonTree)adaptor.becomeRoot(IF34_tree, root_1);
 
 
 
             match(input, Token.DOWN, null); 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_2 = _last;
             CommonTree _first_2 = null;
             CommonTree root_2 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             COND35=(CommonTree)match(input,COND,FOLLOW_COND_in_if_statement716); 
             COND35_tree = (CommonTree)adaptor.dupNode(COND35);
 
             root_2 = (CommonTree)adaptor.becomeRoot(COND35_tree, root_2);
 
 
 
             match(input, Token.DOWN, null); 
             _last = (CommonTree)input.LT(1);
             pushFollow(FOLLOW_statement_in_if_statement720);
             cond=statement();
 
             state._fsp--;
 
             adaptor.addChild(root_2, cond.getTree());
 
             match(input, Token.UP, null); adaptor.addChild(root_1, root_2);_last = _save_last_2;
             }
 
             _last = (CommonTree)input.LT(1);
             pushFollow(FOLLOW_block_in_if_statement723);
             block36=block();
 
             state._fsp--;
 
             adaptor.addChild(root_1, block36.getTree());
 
             match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
             }
 
 
                         retval.op = new If((cond!=null?cond.op:null), (block36!=null?block36.operations:null));
                     
 
             }
 
             retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
 
         }
         catch (RecognitionException re) {
             reportError(re);
             recover(input,re);
         }
         finally {
         }
         return retval;
     }
     // $ANTLR end "if_statement"
 
     public static class while_statement_return extends TreeRuleReturnScope {
         public Operation op;
         CommonTree tree;
         public Object getTree() { return tree; }
     };
 
     // $ANTLR start "while_statement"
     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:292:1: while_statement returns [Operation op] : ^( WHILE ^( COND cond= statement ) block ) ;
     public final GremlinEvaluator.while_statement_return while_statement() throws RecognitionException {
         GremlinEvaluator.while_statement_return retval = new GremlinEvaluator.while_statement_return();
         retval.start = input.LT(1);
 
         CommonTree root_0 = null;
 
         CommonTree _first_0 = null;
         CommonTree _last = null;
 
         CommonTree WHILE37=null;
         CommonTree COND38=null;
         GremlinEvaluator.statement_return cond = null;
 
         GremlinEvaluator.block_return block39 = null;
 
 
         CommonTree WHILE37_tree=null;
         CommonTree COND38_tree=null;
 
         try {
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:293:2: ( ^( WHILE ^( COND cond= statement ) block ) )
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:293:4: ^( WHILE ^( COND cond= statement ) block )
             {
             root_0 = (CommonTree)adaptor.nil();
 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_1 = _last;
             CommonTree _first_1 = null;
             CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             WHILE37=(CommonTree)match(input,WHILE,FOLLOW_WHILE_in_while_statement751); 
             WHILE37_tree = (CommonTree)adaptor.dupNode(WHILE37);
 
             root_1 = (CommonTree)adaptor.becomeRoot(WHILE37_tree, root_1);
 
 
 
             match(input, Token.DOWN, null); 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_2 = _last;
             CommonTree _first_2 = null;
             CommonTree root_2 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             COND38=(CommonTree)match(input,COND,FOLLOW_COND_in_while_statement754); 
             COND38_tree = (CommonTree)adaptor.dupNode(COND38);
 
             root_2 = (CommonTree)adaptor.becomeRoot(COND38_tree, root_2);
 
 
 
             match(input, Token.DOWN, null); 
             _last = (CommonTree)input.LT(1);
             pushFollow(FOLLOW_statement_in_while_statement758);
             cond=statement();
 
             state._fsp--;
 
             adaptor.addChild(root_2, cond.getTree());
 
             match(input, Token.UP, null); adaptor.addChild(root_1, root_2);_last = _save_last_2;
             }
 
             _last = (CommonTree)input.LT(1);
             pushFollow(FOLLOW_block_in_while_statement761);
             block39=block();
 
             state._fsp--;
 
             adaptor.addChild(root_1, block39.getTree());
 
             match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
             }
 
 
                         retval.op = new While((cond!=null?cond.op:null), (block39!=null?block39.operations:null));
                     
 
             }
 
             retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
 
         }
         catch (RecognitionException re) {
             reportError(re);
             recover(input,re);
         }
         finally {
         }
         return retval;
     }
     // $ANTLR end "while_statement"
 
     public static class foreach_statement_return extends TreeRuleReturnScope {
         public Operation op;
         CommonTree tree;
         public Object getTree() { return tree; }
     };
 
     // $ANTLR start "foreach_statement"
     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:299:1: foreach_statement returns [Operation op] : ^( FOREACH VARIABLE arr= statement block ) ;
     public final GremlinEvaluator.foreach_statement_return foreach_statement() throws RecognitionException {
         GremlinEvaluator.foreach_statement_return retval = new GremlinEvaluator.foreach_statement_return();
         retval.start = input.LT(1);
 
         CommonTree root_0 = null;
 
         CommonTree _first_0 = null;
         CommonTree _last = null;
 
         CommonTree FOREACH40=null;
         CommonTree VARIABLE41=null;
         GremlinEvaluator.statement_return arr = null;
 
         GremlinEvaluator.block_return block42 = null;
 
 
         CommonTree FOREACH40_tree=null;
         CommonTree VARIABLE41_tree=null;
 
         try {
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:300:2: ( ^( FOREACH VARIABLE arr= statement block ) )
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:300:4: ^( FOREACH VARIABLE arr= statement block )
             {
             root_0 = (CommonTree)adaptor.nil();
 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_1 = _last;
             CommonTree _first_1 = null;
             CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             FOREACH40=(CommonTree)match(input,FOREACH,FOLLOW_FOREACH_in_foreach_statement788); 
             FOREACH40_tree = (CommonTree)adaptor.dupNode(FOREACH40);
 
             root_1 = (CommonTree)adaptor.becomeRoot(FOREACH40_tree, root_1);
 
 
 
             match(input, Token.DOWN, null); 
             _last = (CommonTree)input.LT(1);
             VARIABLE41=(CommonTree)match(input,VARIABLE,FOLLOW_VARIABLE_in_foreach_statement790); 
             VARIABLE41_tree = (CommonTree)adaptor.dupNode(VARIABLE41);
 
             adaptor.addChild(root_1, VARIABLE41_tree);
 
             _last = (CommonTree)input.LT(1);
             pushFollow(FOLLOW_statement_in_foreach_statement794);
             arr=statement();
 
             state._fsp--;
 
             adaptor.addChild(root_1, arr.getTree());
             _last = (CommonTree)input.LT(1);
             pushFollow(FOLLOW_block_in_foreach_statement796);
             block42=block();
 
             state._fsp--;
 
             adaptor.addChild(root_1, block42.getTree());
 
             match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
             }
 
 
                         retval.op = new Foreach((VARIABLE41!=null?VARIABLE41.getText():null), (arr!=null?arr.op:null), (block42!=null?block42.operations:null));
                     
 
             }
 
             retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
 
         }
         catch (RecognitionException re) {
             reportError(re);
             recover(input,re);
         }
         finally {
         }
         return retval;
     }
     // $ANTLR end "foreach_statement"
 
     public static class repeat_statement_return extends TreeRuleReturnScope {
         public Operation op;
         CommonTree tree;
         public Object getTree() { return tree; }
     };
 
     // $ANTLR start "repeat_statement"
     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:306:1: repeat_statement returns [Operation op] : ^( REPEAT timer= statement block ) ;
     public final GremlinEvaluator.repeat_statement_return repeat_statement() throws RecognitionException {
         GremlinEvaluator.repeat_statement_return retval = new GremlinEvaluator.repeat_statement_return();
         retval.start = input.LT(1);
 
         CommonTree root_0 = null;
 
         CommonTree _first_0 = null;
         CommonTree _last = null;
 
         CommonTree REPEAT43=null;
         GremlinEvaluator.statement_return timer = null;
 
         GremlinEvaluator.block_return block44 = null;
 
 
         CommonTree REPEAT43_tree=null;
 
         try {
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:307:2: ( ^( REPEAT timer= statement block ) )
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:307:4: ^( REPEAT timer= statement block )
             {
             root_0 = (CommonTree)adaptor.nil();
 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_1 = _last;
             CommonTree _first_1 = null;
             CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             REPEAT43=(CommonTree)match(input,REPEAT,FOLLOW_REPEAT_in_repeat_statement824); 
             REPEAT43_tree = (CommonTree)adaptor.dupNode(REPEAT43);
 
             root_1 = (CommonTree)adaptor.becomeRoot(REPEAT43_tree, root_1);
 
 
 
             match(input, Token.DOWN, null); 
             _last = (CommonTree)input.LT(1);
             pushFollow(FOLLOW_statement_in_repeat_statement828);
             timer=statement();
 
             state._fsp--;
 
             adaptor.addChild(root_1, timer.getTree());
             _last = (CommonTree)input.LT(1);
             pushFollow(FOLLOW_block_in_repeat_statement830);
             block44=block();
 
             state._fsp--;
 
             adaptor.addChild(root_1, block44.getTree());
 
             match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
             }
 
 
                         retval.op = new Repeat((timer!=null?timer.op:null), (block44!=null?block44.operations:null));
                     
 
             }
 
             retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
 
         }
         catch (RecognitionException re) {
             reportError(re);
             recover(input,re);
         }
         finally {
         }
         return retval;
     }
     // $ANTLR end "repeat_statement"
 
     public static class block_return extends TreeRuleReturnScope {
         public List<Operation> operations;
         CommonTree tree;
         public Object getTree() { return tree; }
     };
 
     // $ANTLR start "block"
     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:313:1: block returns [List<Operation> operations] : ^( BLOCK ( ( statement | collection ) )+ ) ;
     public final GremlinEvaluator.block_return block() throws RecognitionException {
         GremlinEvaluator.block_return retval = new GremlinEvaluator.block_return();
         retval.start = input.LT(1);
 
         CommonTree root_0 = null;
 
         CommonTree _first_0 = null;
         CommonTree _last = null;
 
         CommonTree BLOCK45=null;
         GremlinEvaluator.statement_return statement46 = null;
 
         GremlinEvaluator.collection_return collection47 = null;
 
 
         CommonTree BLOCK45_tree=null;
 
 
                 List<Operation> operationList = new ArrayList<Operation>();
             
         try {
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:317:5: ( ^( BLOCK ( ( statement | collection ) )+ ) )
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:317:7: ^( BLOCK ( ( statement | collection ) )+ )
             {
             root_0 = (CommonTree)adaptor.nil();
 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_1 = _last;
             CommonTree _first_1 = null;
             CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             BLOCK45=(CommonTree)match(input,BLOCK,FOLLOW_BLOCK_in_block869); 
             BLOCK45_tree = (CommonTree)adaptor.dupNode(BLOCK45);
 
             root_1 = (CommonTree)adaptor.becomeRoot(BLOCK45_tree, root_1);
 
 
 
             match(input, Token.DOWN, null); 
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:317:15: ( ( statement | collection ) )+
             int cnt10=0;
             loop10:
             do {
                 int alt10=2;
                 int LA10_0 = input.LA(1);
 
                 if ( (LA10_0==VAR||LA10_0==FUNC||(LA10_0>=PATH && LA10_0<=GPATH)||(LA10_0>=FUNC_CALL && LA10_0<=IF)||(LA10_0>=FOREACH && LA10_0<=COLLECTION_CALL)||LA10_0==IDENTIFIER||(LA10_0>=67 && LA10_0<=68)||LA10_0==78||(LA10_0>=81 && LA10_0<=90)) ) {
                     alt10=1;
                 }
 
 
                 switch (alt10) {
             	case 1 :
             	    // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:317:17: ( statement | collection )
             	    {
             	    // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:317:17: ( statement | collection )
             	    int alt9=2;
             	    int LA9_0 = input.LA(1);
 
             	    if ( (LA9_0==VAR||LA9_0==FUNC||(LA9_0>=PATH && LA9_0<=GPATH)||(LA9_0>=FUNC_CALL && LA9_0<=IF)||(LA9_0>=FOREACH && LA9_0<=VARIABLE_CALL)||LA9_0==IDENTIFIER||(LA9_0>=67 && LA9_0<=68)||LA9_0==78||(LA9_0>=81 && LA9_0<=90)) ) {
             	        alt9=1;
             	    }
             	    else if ( (LA9_0==COLLECTION_CALL) ) {
             	        alt9=2;
             	    }
             	    else {
             	        NoViableAltException nvae =
             	            new NoViableAltException("", 9, 0, input);
 
             	        throw nvae;
             	    }
             	    switch (alt9) {
             	        case 1 :
             	            // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:317:18: statement
             	            {
             	            _last = (CommonTree)input.LT(1);
             	            pushFollow(FOLLOW_statement_in_block874);
             	            statement46=statement();
 
             	            state._fsp--;
 
             	            adaptor.addChild(root_1, statement46.getTree());
             	             operationList.add((statement46!=null?statement46.op:null)); 
 
             	            }
             	            break;
             	        case 2 :
             	            // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:317:68: collection
             	            {
             	            _last = (CommonTree)input.LT(1);
             	            pushFollow(FOLLOW_collection_in_block880);
             	            collection47=collection();
 
             	            state._fsp--;
 
             	            adaptor.addChild(root_1, collection47.getTree());
             	             operationList.add((collection47!=null?collection47.op:null)); 
 
             	            }
             	            break;
 
             	    }
 
 
             	    }
             	    break;
 
             	default :
             	    if ( cnt10 >= 1 ) break loop10;
                         EarlyExitException eee =
                             new EarlyExitException(10, input);
                         throw eee;
                 }
                 cnt10++;
             } while (true);
 
 
             match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
             }
 
              retval.operations = operationList; 
 
             }
 
             retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
 
         }
         catch (RecognitionException re) {
             reportError(re);
             recover(input,re);
         }
         finally {
         }
         return retval;
     }
     // $ANTLR end "block"
 
     public static class expression_return extends TreeRuleReturnScope {
         public Operation expr;
         CommonTree tree;
         public Object getTree() { return tree; }
     };
 
     // $ANTLR start "expression"
     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:320:1: expression returns [Operation expr] : ( ^( '=' a= operation b= operation ) | ^( '!=' a= operation b= operation ) | ^( '<' a= operation b= operation ) | ^( '>' a= operation b= operation ) | ^( '<=' a= operation b= operation ) | ^( '>=' a= operation b= operation ) | operation );
     public final GremlinEvaluator.expression_return expression() throws RecognitionException {
         GremlinEvaluator.expression_return retval = new GremlinEvaluator.expression_return();
         retval.start = input.LT(1);
 
         CommonTree root_0 = null;
 
         CommonTree _first_0 = null;
         CommonTree _last = null;
 
         CommonTree char_literal48=null;
         CommonTree string_literal49=null;
         CommonTree char_literal50=null;
         CommonTree char_literal51=null;
         CommonTree string_literal52=null;
         CommonTree string_literal53=null;
         GremlinEvaluator.operation_return a = null;
 
         GremlinEvaluator.operation_return b = null;
 
         GremlinEvaluator.operation_return operation54 = null;
 
 
         CommonTree char_literal48_tree=null;
         CommonTree string_literal49_tree=null;
         CommonTree char_literal50_tree=null;
         CommonTree char_literal51_tree=null;
         CommonTree string_literal52_tree=null;
         CommonTree string_literal53_tree=null;
 
         try {
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:321:5: ( ^( '=' a= operation b= operation ) | ^( '!=' a= operation b= operation ) | ^( '<' a= operation b= operation ) | ^( '>' a= operation b= operation ) | ^( '<=' a= operation b= operation ) | ^( '>=' a= operation b= operation ) | operation )
             int alt11=7;
             switch ( input.LA(1) ) {
             case 81:
                 {
                 alt11=1;
                 }
                 break;
             case 82:
                 {
                 alt11=2;
                 }
                 break;
             case 83:
                 {
                 alt11=3;
                 }
                 break;
             case 85:
                 {
                 alt11=4;
                 }
                 break;
             case 84:
                 {
                 alt11=5;
                 }
                 break;
             case 86:
                 {
                 alt11=6;
                 }
                 break;
             case FUNC_CALL:
             case INT:
             case LONG:
             case FLOAT:
             case DOUBLE:
             case STR:
             case ARR:
             case BOOL:
             case NULL:
             case RANGE:
             case PROPERTY_CALL:
             case VARIABLE_CALL:
             case IDENTIFIER:
             case 78:
             case 87:
             case 88:
             case 89:
             case 90:
                 {
                 alt11=7;
                 }
                 break;
             default:
                 NoViableAltException nvae =
                     new NoViableAltException("", 11, 0, input);
 
                 throw nvae;
             }
 
             switch (alt11) {
                 case 1 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:321:9: ^( '=' a= operation b= operation )
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_1 = _last;
                     CommonTree _first_1 = null;
                     CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     char_literal48=(CommonTree)match(input,81,FOLLOW_81_in_expression909); 
                     char_literal48_tree = (CommonTree)adaptor.dupNode(char_literal48);
 
                     root_1 = (CommonTree)adaptor.becomeRoot(char_literal48_tree, root_1);
 
 
 
                     match(input, Token.DOWN, null); 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_operation_in_expression914);
                     a=operation();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, a.getTree());
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_operation_in_expression918);
                     b=operation();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, b.getTree());
 
                     match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
                     }
 
                      retval.expr = new Equality((a!=null?a.op:null), (b!=null?b.op:null)); 
 
                     }
                     break;
                 case 2 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:322:9: ^( '!=' a= operation b= operation )
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_1 = _last;
                     CommonTree _first_1 = null;
                     CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     string_literal49=(CommonTree)match(input,82,FOLLOW_82_in_expression932); 
                     string_literal49_tree = (CommonTree)adaptor.dupNode(string_literal49);
 
                     root_1 = (CommonTree)adaptor.becomeRoot(string_literal49_tree, root_1);
 
 
 
                     match(input, Token.DOWN, null); 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_operation_in_expression936);
                     a=operation();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, a.getTree());
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_operation_in_expression940);
                     b=operation();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, b.getTree());
 
                     match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
                     }
 
                      retval.expr = new UnEquality((a!=null?a.op:null), (b!=null?b.op:null)); 
 
                     }
                     break;
                 case 3 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:323:9: ^( '<' a= operation b= operation )
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_1 = _last;
                     CommonTree _first_1 = null;
                     CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     char_literal50=(CommonTree)match(input,83,FOLLOW_83_in_expression954); 
                     char_literal50_tree = (CommonTree)adaptor.dupNode(char_literal50);
 
                     root_1 = (CommonTree)adaptor.becomeRoot(char_literal50_tree, root_1);
 
 
 
                     match(input, Token.DOWN, null); 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_operation_in_expression959);
                     a=operation();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, a.getTree());
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_operation_in_expression963);
                     b=operation();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, b.getTree());
 
                     match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
                     }
 
                      retval.expr = new LessThan((a!=null?a.op:null), (b!=null?b.op:null)); 
 
                     }
                     break;
                 case 4 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:324:9: ^( '>' a= operation b= operation )
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_1 = _last;
                     CommonTree _first_1 = null;
                     CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     char_literal51=(CommonTree)match(input,85,FOLLOW_85_in_expression977); 
                     char_literal51_tree = (CommonTree)adaptor.dupNode(char_literal51);
 
                     root_1 = (CommonTree)adaptor.becomeRoot(char_literal51_tree, root_1);
 
 
 
                     match(input, Token.DOWN, null); 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_operation_in_expression982);
                     a=operation();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, a.getTree());
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_operation_in_expression986);
                     b=operation();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, b.getTree());
 
                     match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
                     }
 
                      retval.expr = new GreaterThan((a!=null?a.op:null), (b!=null?b.op:null)); 
 
                     }
                     break;
                 case 5 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:325:9: ^( '<=' a= operation b= operation )
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_1 = _last;
                     CommonTree _first_1 = null;
                     CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     string_literal52=(CommonTree)match(input,84,FOLLOW_84_in_expression1000); 
                     string_literal52_tree = (CommonTree)adaptor.dupNode(string_literal52);
 
                     root_1 = (CommonTree)adaptor.becomeRoot(string_literal52_tree, root_1);
 
 
 
                     match(input, Token.DOWN, null); 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_operation_in_expression1004);
                     a=operation();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, a.getTree());
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_operation_in_expression1008);
                     b=operation();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, b.getTree());
 
                     match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
                     }
 
                      retval.expr = new LessThanOrEqual((a!=null?a.op:null), (b!=null?b.op:null)); 
 
                     }
                     break;
                 case 6 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:326:9: ^( '>=' a= operation b= operation )
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_1 = _last;
                     CommonTree _first_1 = null;
                     CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     string_literal53=(CommonTree)match(input,86,FOLLOW_86_in_expression1022); 
                     string_literal53_tree = (CommonTree)adaptor.dupNode(string_literal53);
 
                     root_1 = (CommonTree)adaptor.becomeRoot(string_literal53_tree, root_1);
 
 
 
                     match(input, Token.DOWN, null); 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_operation_in_expression1026);
                     a=operation();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, a.getTree());
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_operation_in_expression1030);
                     b=operation();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, b.getTree());
 
                     match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
                     }
 
                      retval.expr = new GreaterThanOrEqual((a!=null?a.op:null), (b!=null?b.op:null)); 
 
                     }
                     break;
                 case 7 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:327:9: operation
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_operation_in_expression1043);
                     operation54=operation();
 
                     state._fsp--;
 
                     adaptor.addChild(root_0, operation54.getTree());
                      retval.expr = (operation54!=null?operation54.op:null); 
 
                     }
                     break;
 
             }
             retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
 
         }
         catch (RecognitionException re) {
             reportError(re);
             recover(input,re);
         }
         finally {
         }
         return retval;
     }
     // $ANTLR end "expression"
 
     public static class operation_return extends TreeRuleReturnScope {
         public Operation op;
         CommonTree tree;
         public Object getTree() { return tree; }
     };
 
     // $ANTLR start "operation"
     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:330:1: operation returns [Operation op] : ( ^( '+' a= operation b= operation ) | ^( '-' a= operation b= operation ) | binary_operation );
     public final GremlinEvaluator.operation_return operation() throws RecognitionException {
         GremlinEvaluator.operation_return retval = new GremlinEvaluator.operation_return();
         retval.start = input.LT(1);
 
         CommonTree root_0 = null;
 
         CommonTree _first_0 = null;
         CommonTree _last = null;
 
         CommonTree char_literal55=null;
         CommonTree char_literal56=null;
         GremlinEvaluator.operation_return a = null;
 
         GremlinEvaluator.operation_return b = null;
 
         GremlinEvaluator.binary_operation_return binary_operation57 = null;
 
 
         CommonTree char_literal55_tree=null;
         CommonTree char_literal56_tree=null;
 
         try {
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:331:5: ( ^( '+' a= operation b= operation ) | ^( '-' a= operation b= operation ) | binary_operation )
             int alt12=3;
             switch ( input.LA(1) ) {
             case 87:
                 {
                 alt12=1;
                 }
                 break;
             case 88:
                 {
                 alt12=2;
                 }
                 break;
             case FUNC_CALL:
             case INT:
             case LONG:
             case FLOAT:
             case DOUBLE:
             case STR:
             case ARR:
             case BOOL:
             case NULL:
             case RANGE:
             case PROPERTY_CALL:
             case VARIABLE_CALL:
             case IDENTIFIER:
             case 78:
             case 89:
             case 90:
                 {
                 alt12=3;
                 }
                 break;
             default:
                 NoViableAltException nvae =
                     new NoViableAltException("", 12, 0, input);
 
                 throw nvae;
             }
 
             switch (alt12) {
                 case 1 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:331:9: ^( '+' a= operation b= operation )
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_1 = _last;
                     CommonTree _first_1 = null;
                     CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     char_literal55=(CommonTree)match(input,87,FOLLOW_87_in_operation1088); 
                     char_literal55_tree = (CommonTree)adaptor.dupNode(char_literal55);
 
                     root_1 = (CommonTree)adaptor.becomeRoot(char_literal55_tree, root_1);
 
 
 
                     match(input, Token.DOWN, null); 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_operation_in_operation1092);
                     a=operation();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, a.getTree());
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_operation_in_operation1096);
                     b=operation();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, b.getTree());
 
                     match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
                     }
 
                      retval.op = new Addition((a!=null?a.op:null), (b!=null?b.op:null)); 
 
                     }
                     break;
                 case 2 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:332:9: ^( '-' a= operation b= operation )
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_1 = _last;
                     CommonTree _first_1 = null;
                     CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     char_literal56=(CommonTree)match(input,88,FOLLOW_88_in_operation1110); 
                     char_literal56_tree = (CommonTree)adaptor.dupNode(char_literal56);
 
                     root_1 = (CommonTree)adaptor.becomeRoot(char_literal56_tree, root_1);
 
 
 
                     match(input, Token.DOWN, null); 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_operation_in_operation1114);
                     a=operation();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, a.getTree());
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_operation_in_operation1118);
                     b=operation();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, b.getTree());
 
                     match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
                     }
 
                      retval.op = new Subtraction((a!=null?a.op:null), (b!=null?b.op:null)); 
 
                     }
                     break;
                 case 3 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:333:9: binary_operation
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_binary_operation_in_operation1131);
                     binary_operation57=binary_operation();
 
                     state._fsp--;
 
                     adaptor.addChild(root_0, binary_operation57.getTree());
                      retval.op = (binary_operation57!=null?binary_operation57.operation:null); 
 
                     }
                     break;
 
             }
             retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
 
         }
         catch (RecognitionException re) {
             reportError(re);
             recover(input,re);
         }
         finally {
         }
         return retval;
     }
     // $ANTLR end "operation"
 
     public static class binary_operation_return extends TreeRuleReturnScope {
         public Operation operation;
         CommonTree tree;
         public Object getTree() { return tree; }
     };
 
     // $ANTLR start "binary_operation"
     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:336:1: binary_operation returns [Operation operation] : ( ^( '*' a= operation b= operation ) | ^( 'div' a= operation b= operation ) | atom );
     public final GremlinEvaluator.binary_operation_return binary_operation() throws RecognitionException {
         GremlinEvaluator.binary_operation_return retval = new GremlinEvaluator.binary_operation_return();
         retval.start = input.LT(1);
 
         CommonTree root_0 = null;
 
         CommonTree _first_0 = null;
         CommonTree _last = null;
 
         CommonTree char_literal58=null;
         CommonTree string_literal59=null;
         GremlinEvaluator.operation_return a = null;
 
         GremlinEvaluator.operation_return b = null;
 
         GremlinEvaluator.atom_return atom60 = null;
 
 
         CommonTree char_literal58_tree=null;
         CommonTree string_literal59_tree=null;
 
         try {
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:337:5: ( ^( '*' a= operation b= operation ) | ^( 'div' a= operation b= operation ) | atom )
             int alt13=3;
             switch ( input.LA(1) ) {
             case 89:
                 {
                 alt13=1;
                 }
                 break;
             case 90:
                 {
                 alt13=2;
                 }
                 break;
             case FUNC_CALL:
             case INT:
             case LONG:
             case FLOAT:
             case DOUBLE:
             case STR:
             case ARR:
             case BOOL:
             case NULL:
             case RANGE:
             case PROPERTY_CALL:
             case VARIABLE_CALL:
             case IDENTIFIER:
             case 78:
                 {
                 alt13=3;
                 }
                 break;
             default:
                 NoViableAltException nvae =
                     new NoViableAltException("", 13, 0, input);
 
                 throw nvae;
             }
 
             switch (alt13) {
                 case 1 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:337:9: ^( '*' a= operation b= operation )
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_1 = _last;
                     CommonTree _first_1 = null;
                     CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     char_literal58=(CommonTree)match(input,89,FOLLOW_89_in_binary_operation1168); 
                     char_literal58_tree = (CommonTree)adaptor.dupNode(char_literal58);
 
                     root_1 = (CommonTree)adaptor.becomeRoot(char_literal58_tree, root_1);
 
 
 
                     match(input, Token.DOWN, null); 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_operation_in_binary_operation1172);
                     a=operation();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, a.getTree());
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_operation_in_binary_operation1176);
                     b=operation();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, b.getTree());
 
                     match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
                     }
 
                      retval.operation = new Multiplication((a!=null?a.op:null), (b!=null?b.op:null)); 
 
                     }
                     break;
                 case 2 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:338:9: ^( 'div' a= operation b= operation )
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_1 = _last;
                     CommonTree _first_1 = null;
                     CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     string_literal59=(CommonTree)match(input,90,FOLLOW_90_in_binary_operation1195); 
                     string_literal59_tree = (CommonTree)adaptor.dupNode(string_literal59);
 
                     root_1 = (CommonTree)adaptor.becomeRoot(string_literal59_tree, root_1);
 
 
 
                     match(input, Token.DOWN, null); 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_operation_in_binary_operation1199);
                     a=operation();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, a.getTree());
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_operation_in_binary_operation1203);
                     b=operation();
 
                     state._fsp--;
 
                     adaptor.addChild(root_1, b.getTree());
 
                     match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
                     }
 
                      retval.operation = new Division((a!=null?a.op:null), (b!=null?b.op:null)); 
 
                     }
                     break;
                 case 3 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:339:9: atom
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_atom_in_binary_operation1219);
                     atom60=atom();
 
                     state._fsp--;
 
                     adaptor.addChild(root_0, atom60.getTree());
                      retval.operation = new UnaryOperation((atom60!=null?atom60.value:null)); 
 
                     }
                     break;
 
             }
             retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
 
         }
         catch (RecognitionException re) {
             reportError(re);
             recover(input,re);
         }
         finally {
         }
         return retval;
     }
     // $ANTLR end "binary_operation"
 
     public static class function_definition_statement_return extends TreeRuleReturnScope {
         public Operation op;
         CommonTree tree;
         public Object getTree() { return tree; }
     };
 
     // $ANTLR start "function_definition_statement"
     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:342:1: function_definition_statement returns [Operation op] : ^( FUNC ^( FUNC_NAME ^( NS ns= IDENTIFIER ) ^( NAME fn_name= IDENTIFIER ) ) ^( ARGS ( ^( ARG VARIABLE ) )* ) block ) ;
     public final GremlinEvaluator.function_definition_statement_return function_definition_statement() throws RecognitionException {
         GremlinEvaluator.function_definition_statement_return retval = new GremlinEvaluator.function_definition_statement_return();
         retval.start = input.LT(1);
 
         CommonTree root_0 = null;
 
         CommonTree _first_0 = null;
         CommonTree _last = null;
 
         CommonTree ns=null;
         CommonTree fn_name=null;
         CommonTree FUNC61=null;
         CommonTree FUNC_NAME62=null;
         CommonTree NS63=null;
         CommonTree NAME64=null;
         CommonTree ARGS65=null;
         CommonTree ARG66=null;
         CommonTree VARIABLE67=null;
         GremlinEvaluator.block_return block68 = null;
 
 
         CommonTree ns_tree=null;
         CommonTree fn_name_tree=null;
         CommonTree FUNC61_tree=null;
         CommonTree FUNC_NAME62_tree=null;
         CommonTree NS63_tree=null;
         CommonTree NAME64_tree=null;
         CommonTree ARGS65_tree=null;
         CommonTree ARG66_tree=null;
         CommonTree VARIABLE67_tree=null;
 
 
                 List<String> params = new ArrayList<String>();
             
         try {
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:346:2: ( ^( FUNC ^( FUNC_NAME ^( NS ns= IDENTIFIER ) ^( NAME fn_name= IDENTIFIER ) ) ^( ARGS ( ^( ARG VARIABLE ) )* ) block ) )
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:346:4: ^( FUNC ^( FUNC_NAME ^( NS ns= IDENTIFIER ) ^( NAME fn_name= IDENTIFIER ) ) ^( ARGS ( ^( ARG VARIABLE ) )* ) block )
             {
             root_0 = (CommonTree)adaptor.nil();
 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_1 = _last;
             CommonTree _first_1 = null;
             CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             FUNC61=(CommonTree)match(input,FUNC,FOLLOW_FUNC_in_function_definition_statement1277); 
             FUNC61_tree = (CommonTree)adaptor.dupNode(FUNC61);
 
             root_1 = (CommonTree)adaptor.becomeRoot(FUNC61_tree, root_1);
 
 
 
             match(input, Token.DOWN, null); 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_2 = _last;
             CommonTree _first_2 = null;
             CommonTree root_2 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             FUNC_NAME62=(CommonTree)match(input,FUNC_NAME,FOLLOW_FUNC_NAME_in_function_definition_statement1280); 
             FUNC_NAME62_tree = (CommonTree)adaptor.dupNode(FUNC_NAME62);
 
             root_2 = (CommonTree)adaptor.becomeRoot(FUNC_NAME62_tree, root_2);
 
 
 
             match(input, Token.DOWN, null); 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_3 = _last;
             CommonTree _first_3 = null;
             CommonTree root_3 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             NS63=(CommonTree)match(input,NS,FOLLOW_NS_in_function_definition_statement1283); 
             NS63_tree = (CommonTree)adaptor.dupNode(NS63);
 
             root_3 = (CommonTree)adaptor.becomeRoot(NS63_tree, root_3);
 
 
 
             match(input, Token.DOWN, null); 
             _last = (CommonTree)input.LT(1);
             ns=(CommonTree)match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_function_definition_statement1287); 
             ns_tree = (CommonTree)adaptor.dupNode(ns);
 
             adaptor.addChild(root_3, ns_tree);
 
 
             match(input, Token.UP, null); adaptor.addChild(root_2, root_3);_last = _save_last_3;
             }
 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_3 = _last;
             CommonTree _first_3 = null;
             CommonTree root_3 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             NAME64=(CommonTree)match(input,NAME,FOLLOW_NAME_in_function_definition_statement1291); 
             NAME64_tree = (CommonTree)adaptor.dupNode(NAME64);
 
             root_3 = (CommonTree)adaptor.becomeRoot(NAME64_tree, root_3);
 
 
 
             match(input, Token.DOWN, null); 
             _last = (CommonTree)input.LT(1);
             fn_name=(CommonTree)match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_function_definition_statement1295); 
             fn_name_tree = (CommonTree)adaptor.dupNode(fn_name);
 
             adaptor.addChild(root_3, fn_name_tree);
 
 
             match(input, Token.UP, null); adaptor.addChild(root_2, root_3);_last = _save_last_3;
             }
 
 
             match(input, Token.UP, null); adaptor.addChild(root_1, root_2);_last = _save_last_2;
             }
 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_2 = _last;
             CommonTree _first_2 = null;
             CommonTree root_2 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             ARGS65=(CommonTree)match(input,ARGS,FOLLOW_ARGS_in_function_definition_statement1300); 
             ARGS65_tree = (CommonTree)adaptor.dupNode(ARGS65);
 
             root_2 = (CommonTree)adaptor.becomeRoot(ARGS65_tree, root_2);
 
 
 
             if ( input.LA(1)==Token.DOWN ) {
                 match(input, Token.DOWN, null); 
                 // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:346:78: ( ^( ARG VARIABLE ) )*
                 loop14:
                 do {
                     int alt14=2;
                     int LA14_0 = input.LA(1);
 
                     if ( (LA14_0==ARG) ) {
                         alt14=1;
                     }
 
 
                     switch (alt14) {
                 	case 1 :
                 	    // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:346:80: ^( ARG VARIABLE )
                 	    {
                 	    _last = (CommonTree)input.LT(1);
                 	    {
                 	    CommonTree _save_last_3 = _last;
                 	    CommonTree _first_3 = null;
                 	    CommonTree root_3 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                 	    ARG66=(CommonTree)match(input,ARG,FOLLOW_ARG_in_function_definition_statement1305); 
                 	    ARG66_tree = (CommonTree)adaptor.dupNode(ARG66);
 
                 	    root_3 = (CommonTree)adaptor.becomeRoot(ARG66_tree, root_3);
 
 
 
                 	    match(input, Token.DOWN, null); 
                 	    _last = (CommonTree)input.LT(1);
                 	    VARIABLE67=(CommonTree)match(input,VARIABLE,FOLLOW_VARIABLE_in_function_definition_statement1307); 
                 	    VARIABLE67_tree = (CommonTree)adaptor.dupNode(VARIABLE67);
 
                 	    adaptor.addChild(root_3, VARIABLE67_tree);
 
                 	     params.add((VARIABLE67!=null?VARIABLE67.getText():null)); 
 
                 	    match(input, Token.UP, null); adaptor.addChild(root_2, root_3);_last = _save_last_3;
                 	    }
 
 
                 	    }
                 	    break;
 
                 	default :
                 	    break loop14;
                     }
                 } while (true);
 
 
                 match(input, Token.UP, null); 
             }adaptor.addChild(root_1, root_2);_last = _save_last_2;
             }
 
             _last = (CommonTree)input.LT(1);
             pushFollow(FOLLOW_block_in_function_definition_statement1316);
             block68=block();
 
             state._fsp--;
 
             adaptor.addChild(root_1, block68.getTree());
 
             match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
             }
 
 
                         NativeFunction fn = new NativeFunction((fn_name!=null?fn_name.getText():null), params, (block68!=null?block68.operations:null));
                         functions.registerFunction((ns!=null?ns.getText():null), fn);
 
                         retval.op = new UnaryOperation(new Atom(null));
                     
 
             }
 
             retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
 
         }
         catch (RecognitionException re) {
             reportError(re);
             recover(input,re);
         }
         finally {
         }
         return retval;
     }
     // $ANTLR end "function_definition_statement"
 
     public static class function_call_return extends TreeRuleReturnScope {
         public Atom value;
         CommonTree tree;
         public Object getTree() { return tree; }
     };
 
     // $ANTLR start "function_call"
     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:355:1: function_call returns [Atom value] : ^( FUNC_CALL ^( FUNC_NAME ^( NS ns= IDENTIFIER ) ^( NAME fn_name= IDENTIFIER ) ) ^( ARGS ( ^( ARG (st= statement | col= collection ) ) )* ) ) ;
     public final GremlinEvaluator.function_call_return function_call() throws RecognitionException {
         GremlinEvaluator.function_call_return retval = new GremlinEvaluator.function_call_return();
         retval.start = input.LT(1);
 
         CommonTree root_0 = null;
 
         CommonTree _first_0 = null;
         CommonTree _last = null;
 
         CommonTree ns=null;
         CommonTree fn_name=null;
         CommonTree FUNC_CALL69=null;
         CommonTree FUNC_NAME70=null;
         CommonTree NS71=null;
         CommonTree NAME72=null;
         CommonTree ARGS73=null;
         CommonTree ARG74=null;
         GremlinEvaluator.statement_return st = null;
 
         GremlinEvaluator.collection_return col = null;
 
 
         CommonTree ns_tree=null;
         CommonTree fn_name_tree=null;
         CommonTree FUNC_CALL69_tree=null;
         CommonTree FUNC_NAME70_tree=null;
         CommonTree NS71_tree=null;
         CommonTree NAME72_tree=null;
         CommonTree ARGS73_tree=null;
         CommonTree ARG74_tree=null;
 
 
                 List<Operation> params = new ArrayList<Operation>();
             
         try {
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:359:2: ( ^( FUNC_CALL ^( FUNC_NAME ^( NS ns= IDENTIFIER ) ^( NAME fn_name= IDENTIFIER ) ) ^( ARGS ( ^( ARG (st= statement | col= collection ) ) )* ) ) )
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:359:4: ^( FUNC_CALL ^( FUNC_NAME ^( NS ns= IDENTIFIER ) ^( NAME fn_name= IDENTIFIER ) ) ^( ARGS ( ^( ARG (st= statement | col= collection ) ) )* ) )
             {
             root_0 = (CommonTree)adaptor.nil();
 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_1 = _last;
             CommonTree _first_1 = null;
             CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             FUNC_CALL69=(CommonTree)match(input,FUNC_CALL,FOLLOW_FUNC_CALL_in_function_call1353); 
             FUNC_CALL69_tree = (CommonTree)adaptor.dupNode(FUNC_CALL69);
 
             root_1 = (CommonTree)adaptor.becomeRoot(FUNC_CALL69_tree, root_1);
 
 
 
             match(input, Token.DOWN, null); 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_2 = _last;
             CommonTree _first_2 = null;
             CommonTree root_2 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             FUNC_NAME70=(CommonTree)match(input,FUNC_NAME,FOLLOW_FUNC_NAME_in_function_call1356); 
             FUNC_NAME70_tree = (CommonTree)adaptor.dupNode(FUNC_NAME70);
 
             root_2 = (CommonTree)adaptor.becomeRoot(FUNC_NAME70_tree, root_2);
 
 
 
             match(input, Token.DOWN, null); 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_3 = _last;
             CommonTree _first_3 = null;
             CommonTree root_3 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             NS71=(CommonTree)match(input,NS,FOLLOW_NS_in_function_call1359); 
             NS71_tree = (CommonTree)adaptor.dupNode(NS71);
 
             root_3 = (CommonTree)adaptor.becomeRoot(NS71_tree, root_3);
 
 
 
             match(input, Token.DOWN, null); 
             _last = (CommonTree)input.LT(1);
             ns=(CommonTree)match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_function_call1363); 
             ns_tree = (CommonTree)adaptor.dupNode(ns);
 
             adaptor.addChild(root_3, ns_tree);
 
 
             match(input, Token.UP, null); adaptor.addChild(root_2, root_3);_last = _save_last_3;
             }
 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_3 = _last;
             CommonTree _first_3 = null;
             CommonTree root_3 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             NAME72=(CommonTree)match(input,NAME,FOLLOW_NAME_in_function_call1367); 
             NAME72_tree = (CommonTree)adaptor.dupNode(NAME72);
 
             root_3 = (CommonTree)adaptor.becomeRoot(NAME72_tree, root_3);
 
 
 
             match(input, Token.DOWN, null); 
             _last = (CommonTree)input.LT(1);
             fn_name=(CommonTree)match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_function_call1371); 
             fn_name_tree = (CommonTree)adaptor.dupNode(fn_name);
 
             adaptor.addChild(root_3, fn_name_tree);
 
 
             match(input, Token.UP, null); adaptor.addChild(root_2, root_3);_last = _save_last_3;
             }
 
 
             match(input, Token.UP, null); adaptor.addChild(root_1, root_2);_last = _save_last_2;
             }
 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_2 = _last;
             CommonTree _first_2 = null;
             CommonTree root_2 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             ARGS73=(CommonTree)match(input,ARGS,FOLLOW_ARGS_in_function_call1376); 
             ARGS73_tree = (CommonTree)adaptor.dupNode(ARGS73);
 
             root_2 = (CommonTree)adaptor.becomeRoot(ARGS73_tree, root_2);
 
 
 
             if ( input.LA(1)==Token.DOWN ) {
                 match(input, Token.DOWN, null); 
                 // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:359:83: ( ^( ARG (st= statement | col= collection ) ) )*
                 loop16:
                 do {
                     int alt16=2;
                     int LA16_0 = input.LA(1);
 
                     if ( (LA16_0==ARG) ) {
                         alt16=1;
                     }
 
 
                     switch (alt16) {
                 	case 1 :
                 	    // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:359:85: ^( ARG (st= statement | col= collection ) )
                 	    {
                 	    _last = (CommonTree)input.LT(1);
                 	    {
                 	    CommonTree _save_last_3 = _last;
                 	    CommonTree _first_3 = null;
                 	    CommonTree root_3 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                 	    ARG74=(CommonTree)match(input,ARG,FOLLOW_ARG_in_function_call1381); 
                 	    ARG74_tree = (CommonTree)adaptor.dupNode(ARG74);
 
                 	    root_3 = (CommonTree)adaptor.becomeRoot(ARG74_tree, root_3);
 
 
 
                 	    match(input, Token.DOWN, null); 
                 	    // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:359:91: (st= statement | col= collection )
                 	    int alt15=2;
                 	    int LA15_0 = input.LA(1);
 
                 	    if ( (LA15_0==VAR||LA15_0==FUNC||(LA15_0>=PATH && LA15_0<=GPATH)||(LA15_0>=FUNC_CALL && LA15_0<=IF)||(LA15_0>=FOREACH && LA15_0<=VARIABLE_CALL)||LA15_0==IDENTIFIER||(LA15_0>=67 && LA15_0<=68)||LA15_0==78||(LA15_0>=81 && LA15_0<=90)) ) {
                 	        alt15=1;
                 	    }
                 	    else if ( (LA15_0==COLLECTION_CALL) ) {
                 	        alt15=2;
                 	    }
                 	    else {
                 	        NoViableAltException nvae =
                 	            new NoViableAltException("", 15, 0, input);
 
                 	        throw nvae;
                 	    }
                 	    switch (alt15) {
                 	        case 1 :
                 	            // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:359:92: st= statement
                 	            {
                 	            _last = (CommonTree)input.LT(1);
                 	            pushFollow(FOLLOW_statement_in_function_call1386);
                 	            st=statement();
 
                 	            state._fsp--;
 
                 	            adaptor.addChild(root_3, st.getTree());
                 	             params.add((st!=null?st.op:null)); 
 
                 	            }
                 	            break;
                 	        case 2 :
                 	            // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:359:131: col= collection
                 	            {
                 	            _last = (CommonTree)input.LT(1);
                 	            pushFollow(FOLLOW_collection_in_function_call1394);
                 	            col=collection();
 
                 	            state._fsp--;
 
                 	            adaptor.addChild(root_3, col.getTree());
                 	             params.add((col!=null?col.op:null)); 
 
                 	            }
                 	            break;
 
                 	    }
 
 
                 	    match(input, Token.UP, null); adaptor.addChild(root_2, root_3);_last = _save_last_3;
                 	    }
 
 
                 	    }
                 	    break;
 
                 	default :
                 	    break loop16;
                     }
                 } while (true);
 
 
                 match(input, Token.UP, null); 
             }adaptor.addChild(root_1, root_2);_last = _save_last_2;
             }
 
 
             match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
             }
 
 
                         try {
                             retval.value = functions.runFunction((ns!=null?ns.getText():null), (fn_name!=null?fn_name.getText():null), params);
                         } catch(Exception e) {
                             System.err.println(e);
                         }
                     
 
             }
 
             retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
 
         }
         catch (RecognitionException re) {
             reportError(re);
             recover(input,re);
         }
         finally {
         }
         return retval;
     }
     // $ANTLR end "function_call"
 
     public static class collection_return extends TreeRuleReturnScope {
         public Operation op;
         CommonTree tree;
         public Object getTree() { return tree; }
     };
 
     // $ANTLR start "collection"
     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:369:1: collection returns [Operation op] : ^( COLLECTION_CALL ^( STEP ^( TOKEN token ) ^( PREDICATES ( ^( PREDICATE statement ) )+ ) ) ) ;
     public final GremlinEvaluator.collection_return collection() throws RecognitionException {
         GremlinEvaluator.collection_return retval = new GremlinEvaluator.collection_return();
         retval.start = input.LT(1);
 
         CommonTree root_0 = null;
 
         CommonTree _first_0 = null;
         CommonTree _last = null;
 
         CommonTree COLLECTION_CALL75=null;
         CommonTree STEP76=null;
         CommonTree TOKEN77=null;
         CommonTree PREDICATES79=null;
         CommonTree PREDICATE80=null;
         GremlinEvaluator.token_return token78 = null;
 
         GremlinEvaluator.statement_return statement81 = null;
 
 
         CommonTree COLLECTION_CALL75_tree=null;
         CommonTree STEP76_tree=null;
         CommonTree TOKEN77_tree=null;
         CommonTree PREDICATES79_tree=null;
         CommonTree PREDICATE80_tree=null;
 
 
                 Object startPoint = null;
                 List<Pipe> pipes = new ArrayList<Pipe>();
                 List<Operation> predicates = new ArrayList<Operation>();
             
         try {
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:375:5: ( ^( COLLECTION_CALL ^( STEP ^( TOKEN token ) ^( PREDICATES ( ^( PREDICATE statement ) )+ ) ) ) )
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:375:7: ^( COLLECTION_CALL ^( STEP ^( TOKEN token ) ^( PREDICATES ( ^( PREDICATE statement ) )+ ) ) )
             {
             root_0 = (CommonTree)adaptor.nil();
 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_1 = _last;
             CommonTree _first_1 = null;
             CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             COLLECTION_CALL75=(CommonTree)match(input,COLLECTION_CALL,FOLLOW_COLLECTION_CALL_in_collection1443); 
             COLLECTION_CALL75_tree = (CommonTree)adaptor.dupNode(COLLECTION_CALL75);
 
             root_1 = (CommonTree)adaptor.becomeRoot(COLLECTION_CALL75_tree, root_1);
 
 
 
             match(input, Token.DOWN, null); 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_2 = _last;
             CommonTree _first_2 = null;
             CommonTree root_2 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             STEP76=(CommonTree)match(input,STEP,FOLLOW_STEP_in_collection1446); 
             STEP76_tree = (CommonTree)adaptor.dupNode(STEP76);
 
             root_2 = (CommonTree)adaptor.becomeRoot(STEP76_tree, root_2);
 
 
 
             match(input, Token.DOWN, null); 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_3 = _last;
             CommonTree _first_3 = null;
             CommonTree root_3 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             TOKEN77=(CommonTree)match(input,TOKEN,FOLLOW_TOKEN_in_collection1449); 
             TOKEN77_tree = (CommonTree)adaptor.dupNode(TOKEN77);
 
             root_3 = (CommonTree)adaptor.becomeRoot(TOKEN77_tree, root_3);
 
 
 
             match(input, Token.DOWN, null); 
             _last = (CommonTree)input.LT(1);
             pushFollow(FOLLOW_token_in_collection1451);
             token78=token();
 
             state._fsp--;
 
             adaptor.addChild(root_3, token78.getTree());
 
             match(input, Token.UP, null); adaptor.addChild(root_2, root_3);_last = _save_last_3;
             }
 
             _last = (CommonTree)input.LT(1);
             {
             CommonTree _save_last_3 = _last;
             CommonTree _first_3 = null;
             CommonTree root_3 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             PREDICATES79=(CommonTree)match(input,PREDICATES,FOLLOW_PREDICATES_in_collection1455); 
             PREDICATES79_tree = (CommonTree)adaptor.dupNode(PREDICATES79);
 
             root_3 = (CommonTree)adaptor.becomeRoot(PREDICATES79_tree, root_3);
 
 
 
             match(input, Token.DOWN, null); 
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:375:60: ( ^( PREDICATE statement ) )+
             int cnt17=0;
             loop17:
             do {
                 int alt17=2;
                 int LA17_0 = input.LA(1);
 
                 if ( (LA17_0==PREDICATE) ) {
                     alt17=1;
                 }
 
 
                 switch (alt17) {
             	case 1 :
             	    // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:375:62: ^( PREDICATE statement )
             	    {
             	    _last = (CommonTree)input.LT(1);
             	    {
             	    CommonTree _save_last_4 = _last;
             	    CommonTree _first_4 = null;
             	    CommonTree root_4 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
             	    PREDICATE80=(CommonTree)match(input,PREDICATE,FOLLOW_PREDICATE_in_collection1460); 
             	    PREDICATE80_tree = (CommonTree)adaptor.dupNode(PREDICATE80);
 
             	    root_4 = (CommonTree)adaptor.becomeRoot(PREDICATE80_tree, root_4);
 
 
 
             	    match(input, Token.DOWN, null); 
             	    _last = (CommonTree)input.LT(1);
             	    pushFollow(FOLLOW_statement_in_collection1462);
             	    statement81=statement();
 
             	    state._fsp--;
 
             	    adaptor.addChild(root_4, statement81.getTree());
             	     predicates.add((statement81!=null?statement81.op:null)); 
 
             	    match(input, Token.UP, null); adaptor.addChild(root_3, root_4);_last = _save_last_4;
             	    }
 
 
             	    }
             	    break;
 
             	default :
             	    if ( cnt17 >= 1 ) break loop17;
                         EarlyExitException eee =
                             new EarlyExitException(17, input);
                         throw eee;
                 }
                 cnt17++;
             } while (true);
 
 
             match(input, Token.UP, null); adaptor.addChild(root_2, root_3);_last = _save_last_3;
             }
 
 
             match(input, Token.UP, null); adaptor.addChild(root_1, root_2);_last = _save_last_2;
             }
 
 
             match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
             }
 
 
                     Atom tokenAtom = (token78!=null?token78.atom:null);
 
                     if (tokenAtom.isIdentifier() && ((String)tokenAtom.getValue()).equals(".")) {
                         startPoint = GremlinEvaluator.getVariable(Tokens.ROOT_VARIABLE).getValue();
                     } else if (paths.isPath(tokenAtom.getValue().toString())) {
                         pipes.addAll(paths.getPath((String)tokenAtom.getValue()));
                         startPoint = GremlinEvaluator.getVariable(Tokens.ROOT_VARIABLE).getValue();
                     } else {
                         startPoint = tokenAtom.getValue();
                     }
 
                     pipes.addAll(GremlinPipesHelper.pipesForStep(predicates));
 
                     retval.op = new GPathOperation(pipes, startPoint);
                 
 
             }
 
             retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
 
         }
         catch (RecognitionException re) {
             reportError(re);
             recover(input,re);
         }
         finally {
         }
         return retval;
     }
     // $ANTLR end "collection"
 
     public static class atom_return extends TreeRuleReturnScope {
         public Atom value;
         CommonTree tree;
         public Object getTree() { return tree; }
     };
 
     // $ANTLR start "atom"
     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:395:1: atom returns [Atom value] : ( ^( INT G_INT ) | ^( LONG G_LONG ) | ^( FLOAT G_FLOAT ) | ^( DOUBLE G_DOUBLE ) | ^( RANGE min= G_INT max= G_INT ) | ^( STR StringLiteral ) | ^( BOOL b= BOOLEAN ) | NULL | ^( ARR ( NUMBER )+ ) | ^( VARIABLE_CALL VARIABLE ) | ^( PROPERTY_CALL PROPERTY ) | IDENTIFIER | function_call | '(' statement ')' );
     public final GremlinEvaluator.atom_return atom() throws RecognitionException {
         GremlinEvaluator.atom_return retval = new GremlinEvaluator.atom_return();
         retval.start = input.LT(1);
 
         CommonTree root_0 = null;
 
         CommonTree _first_0 = null;
         CommonTree _last = null;
 
         CommonTree min=null;
         CommonTree max=null;
         CommonTree b=null;
         CommonTree INT82=null;
         CommonTree G_INT83=null;
         CommonTree LONG84=null;
         CommonTree G_LONG85=null;
         CommonTree FLOAT86=null;
         CommonTree G_FLOAT87=null;
         CommonTree DOUBLE88=null;
         CommonTree G_DOUBLE89=null;
         CommonTree RANGE90=null;
         CommonTree STR91=null;
         CommonTree StringLiteral92=null;
         CommonTree BOOL93=null;
         CommonTree NULL94=null;
         CommonTree ARR95=null;
         CommonTree NUMBER96=null;
         CommonTree VARIABLE_CALL97=null;
         CommonTree VARIABLE98=null;
         CommonTree PROPERTY_CALL99=null;
         CommonTree PROPERTY100=null;
         CommonTree IDENTIFIER101=null;
         CommonTree char_literal103=null;
         CommonTree char_literal105=null;
         GremlinEvaluator.function_call_return function_call102 = null;
 
         GremlinEvaluator.statement_return statement104 = null;
 
 
         CommonTree min_tree=null;
         CommonTree max_tree=null;
         CommonTree b_tree=null;
         CommonTree INT82_tree=null;
         CommonTree G_INT83_tree=null;
         CommonTree LONG84_tree=null;
         CommonTree G_LONG85_tree=null;
         CommonTree FLOAT86_tree=null;
         CommonTree G_FLOAT87_tree=null;
         CommonTree DOUBLE88_tree=null;
         CommonTree G_DOUBLE89_tree=null;
         CommonTree RANGE90_tree=null;
         CommonTree STR91_tree=null;
         CommonTree StringLiteral92_tree=null;
         CommonTree BOOL93_tree=null;
         CommonTree NULL94_tree=null;
         CommonTree ARR95_tree=null;
         CommonTree NUMBER96_tree=null;
         CommonTree VARIABLE_CALL97_tree=null;
         CommonTree VARIABLE98_tree=null;
         CommonTree PROPERTY_CALL99_tree=null;
         CommonTree PROPERTY100_tree=null;
         CommonTree IDENTIFIER101_tree=null;
         CommonTree char_literal103_tree=null;
         CommonTree char_literal105_tree=null;
 
 
                 List<Double> array = new ArrayList<Double>();
             
         try {
             // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:399:2: ( ^( INT G_INT ) | ^( LONG G_LONG ) | ^( FLOAT G_FLOAT ) | ^( DOUBLE G_DOUBLE ) | ^( RANGE min= G_INT max= G_INT ) | ^( STR StringLiteral ) | ^( BOOL b= BOOLEAN ) | NULL | ^( ARR ( NUMBER )+ ) | ^( VARIABLE_CALL VARIABLE ) | ^( PROPERTY_CALL PROPERTY ) | IDENTIFIER | function_call | '(' statement ')' )
             int alt19=14;
             switch ( input.LA(1) ) {
             case INT:
                 {
                 alt19=1;
                 }
                 break;
             case LONG:
                 {
                 alt19=2;
                 }
                 break;
             case FLOAT:
                 {
                 alt19=3;
                 }
                 break;
             case DOUBLE:
                 {
                 alt19=4;
                 }
                 break;
             case RANGE:
                 {
                 alt19=5;
                 }
                 break;
             case STR:
                 {
                 alt19=6;
                 }
                 break;
             case BOOL:
                 {
                 alt19=7;
                 }
                 break;
             case NULL:
                 {
                 alt19=8;
                 }
                 break;
             case ARR:
                 {
                 alt19=9;
                 }
                 break;
             case VARIABLE_CALL:
                 {
                 alt19=10;
                 }
                 break;
             case PROPERTY_CALL:
                 {
                 alt19=11;
                 }
                 break;
             case IDENTIFIER:
                 {
                 alt19=12;
                 }
                 break;
             case FUNC_CALL:
                 {
                 alt19=13;
                 }
                 break;
             case 78:
                 {
                 alt19=14;
                 }
                 break;
             default:
                 NoViableAltException nvae =
                     new NoViableAltException("", 19, 0, input);
 
                 throw nvae;
             }
 
             switch (alt19) {
                 case 1 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:399:6: ^( INT G_INT )
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_1 = _last;
                     CommonTree _first_1 = null;
                     CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     INT82=(CommonTree)match(input,INT,FOLLOW_INT_in_atom1509); 
                     INT82_tree = (CommonTree)adaptor.dupNode(INT82);
 
                     root_1 = (CommonTree)adaptor.becomeRoot(INT82_tree, root_1);
 
 
 
                     match(input, Token.DOWN, null); 
                     _last = (CommonTree)input.LT(1);
                     G_INT83=(CommonTree)match(input,G_INT,FOLLOW_G_INT_in_atom1511); 
                     G_INT83_tree = (CommonTree)adaptor.dupNode(G_INT83);
 
                     adaptor.addChild(root_1, G_INT83_tree);
 
 
                     match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
                     }
 
                      retval.value = new Atom<Integer>(new Integer((G_INT83!=null?G_INT83.getText():null))); 
 
                     }
                     break;
                 case 2 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:400:6: ^( LONG G_LONG )
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_1 = _last;
                     CommonTree _first_1 = null;
                     CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     LONG84=(CommonTree)match(input,LONG,FOLLOW_LONG_in_atom1569); 
                     LONG84_tree = (CommonTree)adaptor.dupNode(LONG84);
 
                     root_1 = (CommonTree)adaptor.becomeRoot(LONG84_tree, root_1);
 
 
 
                     match(input, Token.DOWN, null); 
                     _last = (CommonTree)input.LT(1);
                     G_LONG85=(CommonTree)match(input,G_LONG,FOLLOW_G_LONG_in_atom1571); 
                     G_LONG85_tree = (CommonTree)adaptor.dupNode(G_LONG85);
 
                     adaptor.addChild(root_1, G_LONG85_tree);
 
 
                     match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
                     }
 
 
                     	                                                                    String longStr = (G_LONG85!=null?G_LONG85.getText():null);
                     	                                                                    retval.value = new Atom<Long>(new Long(longStr.substring(0, longStr.length() - 1)));
                     	                                                                
 
                     }
                     break;
                 case 3 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:404:6: ^( FLOAT G_FLOAT )
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_1 = _last;
                     CommonTree _first_1 = null;
                     CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     FLOAT86=(CommonTree)match(input,FLOAT,FOLLOW_FLOAT_in_atom1627); 
                     FLOAT86_tree = (CommonTree)adaptor.dupNode(FLOAT86);
 
                     root_1 = (CommonTree)adaptor.becomeRoot(FLOAT86_tree, root_1);
 
 
 
                     match(input, Token.DOWN, null); 
                     _last = (CommonTree)input.LT(1);
                     G_FLOAT87=(CommonTree)match(input,G_FLOAT,FOLLOW_G_FLOAT_in_atom1629); 
                     G_FLOAT87_tree = (CommonTree)adaptor.dupNode(G_FLOAT87);
 
                     adaptor.addChild(root_1, G_FLOAT87_tree);
 
 
                     match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
                     }
 
                      retval.value = new Atom<Float>(new Float((G_FLOAT87!=null?G_FLOAT87.getText():null))); 
 
                     }
                     break;
                 case 4 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:405:6: ^( DOUBLE G_DOUBLE )
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_1 = _last;
                     CommonTree _first_1 = null;
                     CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     DOUBLE88=(CommonTree)match(input,DOUBLE,FOLLOW_DOUBLE_in_atom1683); 
                     DOUBLE88_tree = (CommonTree)adaptor.dupNode(DOUBLE88);
 
                     root_1 = (CommonTree)adaptor.becomeRoot(DOUBLE88_tree, root_1);
 
 
 
                     match(input, Token.DOWN, null); 
                     _last = (CommonTree)input.LT(1);
                     G_DOUBLE89=(CommonTree)match(input,G_DOUBLE,FOLLOW_G_DOUBLE_in_atom1685); 
                     G_DOUBLE89_tree = (CommonTree)adaptor.dupNode(G_DOUBLE89);
 
                     adaptor.addChild(root_1, G_DOUBLE89_tree);
 
 
                     match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
                     }
 
 
                     	                                                                    String doubleStr = (G_DOUBLE89!=null?G_DOUBLE89.getText():null);
                     	                                                                    retval.value = new Atom<Double>(new Double(doubleStr.substring(0, doubleStr.length() - 1)));
                     	                                                                
 
                     }
                     break;
                 case 5 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:409:6: ^( RANGE min= G_INT max= G_INT )
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_1 = _last;
                     CommonTree _first_1 = null;
                     CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     RANGE90=(CommonTree)match(input,RANGE,FOLLOW_RANGE_in_atom1737); 
                     RANGE90_tree = (CommonTree)adaptor.dupNode(RANGE90);
 
                     root_1 = (CommonTree)adaptor.becomeRoot(RANGE90_tree, root_1);
 
 
 
                     match(input, Token.DOWN, null); 
                     _last = (CommonTree)input.LT(1);
                     min=(CommonTree)match(input,G_INT,FOLLOW_G_INT_in_atom1741); 
                     min_tree = (CommonTree)adaptor.dupNode(min);
 
                     adaptor.addChild(root_1, min_tree);
 
                     _last = (CommonTree)input.LT(1);
                     max=(CommonTree)match(input,G_INT,FOLLOW_G_INT_in_atom1745); 
                     max_tree = (CommonTree)adaptor.dupNode(max);
 
                     adaptor.addChild(root_1, max_tree);
 
 
                     match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
                     }
 
                      retval.value = new Atom(new Range((min!=null?min.getText():null), (max!=null?max.getText():null))); 
 
                     }
                     break;
                 case 6 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:410:4: ^( STR StringLiteral )
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_1 = _last;
                     CommonTree _first_1 = null;
                     CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     STR91=(CommonTree)match(input,STR,FOLLOW_STR_in_atom1785); 
                     STR91_tree = (CommonTree)adaptor.dupNode(STR91);
 
                     root_1 = (CommonTree)adaptor.becomeRoot(STR91_tree, root_1);
 
 
 
                     match(input, Token.DOWN, null); 
                     _last = (CommonTree)input.LT(1);
                     StringLiteral92=(CommonTree)match(input,StringLiteral,FOLLOW_StringLiteral_in_atom1787); 
                     StringLiteral92_tree = (CommonTree)adaptor.dupNode(StringLiteral92);
 
                     adaptor.addChild(root_1, StringLiteral92_tree);
 
 
                     match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
                     }
 
                      retval.value = new Atom((StringLiteral92!=null?StringLiteral92.getText():null)); 
 
                     }
                     break;
                 case 7 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:411:9: ^( BOOL b= BOOLEAN )
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_1 = _last;
                     CommonTree _first_1 = null;
                     CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     BOOL93=(CommonTree)match(input,BOOL,FOLLOW_BOOL_in_atom1840); 
                     BOOL93_tree = (CommonTree)adaptor.dupNode(BOOL93);
 
                     root_1 = (CommonTree)adaptor.becomeRoot(BOOL93_tree, root_1);
 
 
 
                     match(input, Token.DOWN, null); 
                     _last = (CommonTree)input.LT(1);
                     b=(CommonTree)match(input,BOOLEAN,FOLLOW_BOOLEAN_in_atom1844); 
                     b_tree = (CommonTree)adaptor.dupNode(b);
 
                     adaptor.addChild(root_1, b_tree);
 
 
                     match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
                     }
 
                      retval.value = new Atom(new Boolean((b!=null?b.getText():null))); 
 
                     }
                     break;
                 case 8 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:412:9: NULL
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     NULL94=(CommonTree)match(input,NULL,FOLLOW_NULL_in_atom1899); 
                     NULL94_tree = (CommonTree)adaptor.dupNode(NULL94);
 
                     adaptor.addChild(root_0, NULL94_tree);
 
                      retval.value = new Atom(null); 
 
                     }
                     break;
                 case 9 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:413:9: ^( ARR ( NUMBER )+ )
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_1 = _last;
                     CommonTree _first_1 = null;
                     CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     ARR95=(CommonTree)match(input,ARR,FOLLOW_ARR_in_atom1967); 
                     ARR95_tree = (CommonTree)adaptor.dupNode(ARR95);
 
                     root_1 = (CommonTree)adaptor.becomeRoot(ARR95_tree, root_1);
 
 
 
                     match(input, Token.DOWN, null); 
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:413:15: ( NUMBER )+
                     int cnt18=0;
                     loop18:
                     do {
                         int alt18=2;
                         int LA18_0 = input.LA(1);
 
                         if ( (LA18_0==NUMBER) ) {
                             alt18=1;
                         }
 
 
                         switch (alt18) {
                     	case 1 :
                     	    // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:413:16: NUMBER
                     	    {
                     	    _last = (CommonTree)input.LT(1);
                     	    NUMBER96=(CommonTree)match(input,NUMBER,FOLLOW_NUMBER_in_atom1970); 
                     	    NUMBER96_tree = (CommonTree)adaptor.dupNode(NUMBER96);
 
                     	    adaptor.addChild(root_1, NUMBER96_tree);
 
                     	     array.add(new Double((NUMBER96!=null?NUMBER96.getText():null))); 
 
                     	    }
                     	    break;
 
                     	default :
                     	    if ( cnt18 >= 1 ) break loop18;
                                 EarlyExitException eee =
                                     new EarlyExitException(18, input);
                                 throw eee;
                         }
                         cnt18++;
                     } while (true);
 
 
                     match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
                     }
 
                      retval.value = new Atom(array); 
 
                     }
                     break;
                 case 10 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:414:4: ^( VARIABLE_CALL VARIABLE )
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_1 = _last;
                     CommonTree _first_1 = null;
                     CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     VARIABLE_CALL97=(CommonTree)match(input,VARIABLE_CALL,FOLLOW_VARIABLE_CALL_in_atom1985); 
                     VARIABLE_CALL97_tree = (CommonTree)adaptor.dupNode(VARIABLE_CALL97);
 
                     root_1 = (CommonTree)adaptor.becomeRoot(VARIABLE_CALL97_tree, root_1);
 
 
 
                     match(input, Token.DOWN, null); 
                     _last = (CommonTree)input.LT(1);
                     VARIABLE98=(CommonTree)match(input,VARIABLE,FOLLOW_VARIABLE_in_atom1987); 
                     VARIABLE98_tree = (CommonTree)adaptor.dupNode(VARIABLE98);
 
                     adaptor.addChild(root_1, VARIABLE98_tree);
 
 
                     match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
                     }
 
                      
                                                                                           retval.value = getVariable((VARIABLE98!=null?VARIABLE98.getText():null)); 
                                                                                         
 
                     }
                     break;
                 case 11 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:417:4: ^( PROPERTY_CALL PROPERTY )
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     {
                     CommonTree _save_last_1 = _last;
                     CommonTree _first_1 = null;
                     CommonTree root_1 = (CommonTree)adaptor.nil();_last = (CommonTree)input.LT(1);
                     PROPERTY_CALL99=(CommonTree)match(input,PROPERTY_CALL,FOLLOW_PROPERTY_CALL_in_atom2030); 
                     PROPERTY_CALL99_tree = (CommonTree)adaptor.dupNode(PROPERTY_CALL99);
 
                     root_1 = (CommonTree)adaptor.becomeRoot(PROPERTY_CALL99_tree, root_1);
 
 
 
                     match(input, Token.DOWN, null); 
                     _last = (CommonTree)input.LT(1);
                     PROPERTY100=(CommonTree)match(input,PROPERTY,FOLLOW_PROPERTY_in_atom2032); 
                     PROPERTY100_tree = (CommonTree)adaptor.dupNode(PROPERTY100);
 
                     adaptor.addChild(root_1, PROPERTY100_tree);
 
 
                     match(input, Token.UP, null); adaptor.addChild(root_0, root_1);_last = _save_last_1;
                     }
 
                      
                                                                                             Atom propertyAtom = new Atom((PROPERTY100!=null?PROPERTY100.getText():null).substring(1));
                                                                                             propertyAtom.setProperty(true);
                                                                                             retval.value = propertyAtom;
                                                                                         
 
                     }
                     break;
                 case 12 :
                     // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:422:4: IDENTIFIER
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     IDENTIFIER101=(CommonTree)match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_atom2074); 
                     IDENTIFIER101_tree = (CommonTree)adaptor.dupNode(IDENTIFIER101);
 
                     adaptor.addChild(root_0, IDENTIFIER101_tree);
 
 
                     	                                                                    String idText = (IDENTIFIER101!=null?IDENTIFIER101.getText():null);
                                                                                             
                     	                                                                    if (idText.equals(".") && !isGPath) {
                    	                                                                        retval.value = getVariable(Tokens.ROOT_VARIABLE);
                     	                                                                    } else if (idText.matches("^[\\d]+..[\\d]+")) {
                                                                                                 Matcher range = rangePattern.matcher(idText);
                                                                                                 if (range.matches())
                                                                                                     retval.value = new Atom(new Range(range.group(1), range.group(2)));
                                                                                                 else
                                                                                                     retval.value = new Atom(null);
                     	                                                                    } else {
                                                                                                 Atom idAtom = new Atom((IDENTIFIER101!=null?IDENTIFIER101.getText():null));
                                                                                                 idAtom.setIdentifier(true);
                                                                                                 retval.value = idAtom;
                                                                                             }
                                                                                         
 
                     }
                     break;
                 case 13 :
                    // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:439:4: function_call
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_function_call_in_atom2130);
                     function_call102=function_call();
 
                     state._fsp--;
 
                     adaptor.addChild(root_0, function_call102.getTree());
                      retval.value = (function_call102!=null?function_call102.value:null); 
 
                     }
                     break;
                 case 14 :
                    // src/main/java/com/tinkerpop/gremlin/compiler/GremlinEvaluator.g:440:4: '(' statement ')'
                     {
                     root_0 = (CommonTree)adaptor.nil();
 
                     _last = (CommonTree)input.LT(1);
                     char_literal103=(CommonTree)match(input,78,FOLLOW_78_in_atom2183); 
                     _last = (CommonTree)input.LT(1);
                     pushFollow(FOLLOW_statement_in_atom2186);
                     statement104=statement();
 
                     state._fsp--;
 
                     adaptor.addChild(root_0, statement104.getTree());
                     _last = (CommonTree)input.LT(1);
                     char_literal105=(CommonTree)match(input,79,FOLLOW_79_in_atom2188); 
 
                     }
                     break;
 
             }
             retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
 
         }
         catch (RecognitionException re) {
             reportError(re);
             recover(input,re);
         }
         finally {
         }
         return retval;
     }
     // $ANTLR end "atom"
 
     // Delegated rules
 
 
  
 
     public static final BitSet FOLLOW_statement_in_program60 = new BitSet(new long[]{0x00000A7FFF981892L,0x0000000007FE4018L});
     public static final BitSet FOLLOW_collection_in_program73 = new BitSet(new long[]{0x00000A7FFF981892L,0x0000000007FE4018L});
     public static final BitSet FOLLOW_VAR_in_program80 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_VARIABLE_in_program82 = new BitSet(new long[]{0x0000004000000000L});
     public static final BitSet FOLLOW_collection_in_program86 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_NEWLINE_in_program92 = new BitSet(new long[]{0x00000A7FFF981892L,0x0000000007FE4018L});
     public static final BitSet FOLLOW_if_statement_in_statement120 = new BitSet(new long[]{0x0000000000000002L});
     public static final BitSet FOLLOW_foreach_statement_in_statement150 = new BitSet(new long[]{0x0000000000000002L});
     public static final BitSet FOLLOW_while_statement_in_statement178 = new BitSet(new long[]{0x0000000000000002L});
     public static final BitSet FOLLOW_repeat_statement_in_statement205 = new BitSet(new long[]{0x0000000000000002L});
     public static final BitSet FOLLOW_path_definition_statement_in_statement231 = new BitSet(new long[]{0x0000000000000002L});
     public static final BitSet FOLLOW_function_definition_statement_in_statement248 = new BitSet(new long[]{0x0000000000000002L});
     public static final BitSet FOLLOW_include_statement_in_statement261 = new BitSet(new long[]{0x0000000000000002L});
     public static final BitSet FOLLOW_gpath_statement_in_statement286 = new BitSet(new long[]{0x0000000000000002L});
     public static final BitSet FOLLOW_VAR_in_statement314 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_VARIABLE_in_statement316 = new BitSet(new long[]{0x0000083FFF981890L,0x0000000007FE4018L});
     public static final BitSet FOLLOW_statement_in_statement320 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_67_in_statement342 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_statement_in_statement346 = new BitSet(new long[]{0x0000083FFF981890L,0x0000000007FE4018L});
     public static final BitSet FOLLOW_statement_in_statement350 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_68_in_statement367 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_statement_in_statement372 = new BitSet(new long[]{0x0000083FFF981890L,0x0000000007FE4018L});
     public static final BitSet FOLLOW_statement_in_statement376 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_expression_in_statement392 = new BitSet(new long[]{0x0000000000000002L});
     public static final BitSet FOLLOW_INCLUDE_in_include_statement435 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_StringLiteral_in_include_statement437 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_PATH_in_path_definition_statement474 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_IDENTIFIER_in_path_definition_statement478 = new BitSet(new long[]{0x0000001000001000L});
     public static final BitSet FOLLOW_gpath_statement_in_path_definition_statement483 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_PROPERTY_CALL_in_path_definition_statement490 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_PROPERTY_in_path_definition_statement494 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_GPATH_in_gpath_statement553 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_step_in_gpath_statement556 = new BitSet(new long[]{0x0000000000002008L});
     public static final BitSet FOLLOW_STEP_in_step594 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_TOKEN_in_step597 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_token_in_step599 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_PREDICATES_in_step603 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_PREDICATE_in_step608 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_statement_in_step610 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_expression_in_token652 = new BitSet(new long[]{0x0000000000000002L});
     public static final BitSet FOLLOW_gpath_statement_in_token664 = new BitSet(new long[]{0x0000000000000002L});
     public static final BitSet FOLLOW_collection_in_token676 = new BitSet(new long[]{0x0000000000000002L});
     public static final BitSet FOLLOW_66_in_token688 = new BitSet(new long[]{0x0000000000000002L});
     public static final BitSet FOLLOW_IF_in_if_statement713 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_COND_in_if_statement716 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_statement_in_if_statement720 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_block_in_if_statement723 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_WHILE_in_while_statement751 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_COND_in_while_statement754 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_statement_in_while_statement758 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_block_in_while_statement761 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_FOREACH_in_foreach_statement788 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_VARIABLE_in_foreach_statement790 = new BitSet(new long[]{0x0000083FFF981890L,0x0000000007FE4018L});
     public static final BitSet FOLLOW_statement_in_foreach_statement794 = new BitSet(new long[]{0x0000000000400000L});
     public static final BitSet FOLLOW_block_in_foreach_statement796 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_REPEAT_in_repeat_statement824 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_statement_in_repeat_statement828 = new BitSet(new long[]{0x0000000000400000L});
     public static final BitSet FOLLOW_block_in_repeat_statement830 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_BLOCK_in_block869 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_statement_in_block874 = new BitSet(new long[]{0x0000087FFF981898L,0x0000000007FE4018L});
     public static final BitSet FOLLOW_collection_in_block880 = new BitSet(new long[]{0x0000087FFF981898L,0x0000000007FE4018L});
     public static final BitSet FOLLOW_81_in_expression909 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_operation_in_expression914 = new BitSet(new long[]{0x0000083FFF981890L,0x0000000007FE4018L});
     public static final BitSet FOLLOW_operation_in_expression918 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_82_in_expression932 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_operation_in_expression936 = new BitSet(new long[]{0x0000083FFF981890L,0x0000000007FE4018L});
     public static final BitSet FOLLOW_operation_in_expression940 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_83_in_expression954 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_operation_in_expression959 = new BitSet(new long[]{0x0000083FFF981890L,0x0000000007FE4018L});
     public static final BitSet FOLLOW_operation_in_expression963 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_85_in_expression977 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_operation_in_expression982 = new BitSet(new long[]{0x0000083FFF981890L,0x0000000007FE4018L});
     public static final BitSet FOLLOW_operation_in_expression986 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_84_in_expression1000 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_operation_in_expression1004 = new BitSet(new long[]{0x0000083FFF981890L,0x0000000007FE4018L});
     public static final BitSet FOLLOW_operation_in_expression1008 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_86_in_expression1022 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_operation_in_expression1026 = new BitSet(new long[]{0x0000083FFF981890L,0x0000000007FE4018L});
     public static final BitSet FOLLOW_operation_in_expression1030 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_operation_in_expression1043 = new BitSet(new long[]{0x0000000000000002L});
     public static final BitSet FOLLOW_87_in_operation1088 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_operation_in_operation1092 = new BitSet(new long[]{0x0000083FFF981890L,0x0000000007FE4018L});
     public static final BitSet FOLLOW_operation_in_operation1096 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_88_in_operation1110 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_operation_in_operation1114 = new BitSet(new long[]{0x0000083FFF981890L,0x0000000007FE4018L});
     public static final BitSet FOLLOW_operation_in_operation1118 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_binary_operation_in_operation1131 = new BitSet(new long[]{0x0000000000000002L});
     public static final BitSet FOLLOW_89_in_binary_operation1168 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_operation_in_binary_operation1172 = new BitSet(new long[]{0x0000083FFF981890L,0x0000000007FE4018L});
     public static final BitSet FOLLOW_operation_in_binary_operation1176 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_90_in_binary_operation1195 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_operation_in_binary_operation1199 = new BitSet(new long[]{0x0000083FFF981890L,0x0000000007FE4018L});
     public static final BitSet FOLLOW_operation_in_binary_operation1203 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_atom_in_binary_operation1219 = new BitSet(new long[]{0x0000000000000002L});
     public static final BitSet FOLLOW_FUNC_in_function_definition_statement1277 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_FUNC_NAME_in_function_definition_statement1280 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_NS_in_function_definition_statement1283 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_IDENTIFIER_in_function_definition_statement1287 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_NAME_in_function_definition_statement1291 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_IDENTIFIER_in_function_definition_statement1295 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_ARGS_in_function_definition_statement1300 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_ARG_in_function_definition_statement1305 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_VARIABLE_in_function_definition_statement1307 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_block_in_function_definition_statement1316 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_FUNC_CALL_in_function_call1353 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_FUNC_NAME_in_function_call1356 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_NS_in_function_call1359 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_IDENTIFIER_in_function_call1363 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_NAME_in_function_call1367 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_IDENTIFIER_in_function_call1371 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_ARGS_in_function_call1376 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_ARG_in_function_call1381 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_statement_in_function_call1386 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_collection_in_function_call1394 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_COLLECTION_CALL_in_collection1443 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_STEP_in_collection1446 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_TOKEN_in_collection1449 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_token_in_collection1451 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_PREDICATES_in_collection1455 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_PREDICATE_in_collection1460 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_statement_in_collection1462 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_INT_in_atom1509 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_G_INT_in_atom1511 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_LONG_in_atom1569 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_G_LONG_in_atom1571 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_FLOAT_in_atom1627 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_G_FLOAT_in_atom1629 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_DOUBLE_in_atom1683 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_G_DOUBLE_in_atom1685 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_RANGE_in_atom1737 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_G_INT_in_atom1741 = new BitSet(new long[]{0x0000100000000000L});
     public static final BitSet FOLLOW_G_INT_in_atom1745 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_STR_in_atom1785 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_StringLiteral_in_atom1787 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_BOOL_in_atom1840 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_BOOLEAN_in_atom1844 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_NULL_in_atom1899 = new BitSet(new long[]{0x0000000000000002L});
     public static final BitSet FOLLOW_ARR_in_atom1967 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_NUMBER_in_atom1970 = new BitSet(new long[]{0x0000000000000008L,0x0000000010000000L});
     public static final BitSet FOLLOW_VARIABLE_CALL_in_atom1985 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_VARIABLE_in_atom1987 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_PROPERTY_CALL_in_atom2030 = new BitSet(new long[]{0x0000000000000004L});
     public static final BitSet FOLLOW_PROPERTY_in_atom2032 = new BitSet(new long[]{0x0000000000000008L});
     public static final BitSet FOLLOW_IDENTIFIER_in_atom2074 = new BitSet(new long[]{0x0000000000000002L});
     public static final BitSet FOLLOW_function_call_in_atom2130 = new BitSet(new long[]{0x0000000000000002L});
     public static final BitSet FOLLOW_78_in_atom2183 = new BitSet(new long[]{0x0000083FFF981890L,0x0000000007FE4018L});
     public static final BitSet FOLLOW_statement_in_atom2186 = new BitSet(new long[]{0x0000000000000000L,0x0000000000008000L});
     public static final BitSet FOLLOW_79_in_atom2188 = new BitSet(new long[]{0x0000000000000002L});
 
 }
