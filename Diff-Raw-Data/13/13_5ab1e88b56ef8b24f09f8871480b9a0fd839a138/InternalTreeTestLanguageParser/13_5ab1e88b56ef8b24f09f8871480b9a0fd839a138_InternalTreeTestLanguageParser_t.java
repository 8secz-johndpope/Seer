 package org.eclipse.xtext.xtend.parser.antlr.internal; 
 
 import java.io.InputStream;
 import org.eclipse.xtext.*;
 import org.eclipse.xtext.parser.*;
 import org.eclipse.xtext.parser.impl.*;
 import org.eclipse.xtext.parsetree.*;
 import org.eclipse.emf.ecore.util.EcoreUtil;
 import org.eclipse.emf.ecore.EObject;
 import org.eclipse.xtext.parser.antlr.AbstractInternalAntlrParser;
 import org.eclipse.xtext.parser.antlr.XtextTokenStream;
 import org.eclipse.xtext.parser.antlr.XtextTokenStream.HiddenTokens;
 import org.eclipse.xtext.parser.antlr.AntlrDatatypeRuleToken;
 import org.eclipse.xtext.conversion.ValueConverterException;
 import org.eclipse.xtext.xtend.services.TreeTestLanguageGrammarAccess;
 
 
 
 import org.antlr.runtime.*;
 import java.util.Stack;
 import java.util.List;
 import java.util.ArrayList;
 
@SuppressWarnings("all")
 public class InternalTreeTestLanguageParser extends AbstractInternalAntlrParser {
     public static final String[] tokenNames = new String[] {
         "<invalid>", "<EOR>", "<DOWN>", "<UP>", "RULE_ID", "RULE_STRING", "RULE_INT", "RULE_ML_COMMENT", "RULE_SL_COMMENT", "RULE_WS", "RULE_ANY_OTHER", "'('", "')'", "'{'", "'};'"
     };
     public static final int RULE_ML_COMMENT=7;
     public static final int RULE_ID=4;
     public static final int RULE_WS=9;
     public static final int EOF=-1;
     public static final int RULE_INT=6;
     public static final int RULE_STRING=5;
     public static final int RULE_ANY_OTHER=10;
     public static final int RULE_SL_COMMENT=8;
 
         public InternalTreeTestLanguageParser(TokenStream input) {
             super(input);
         }
         
 
     public String[] getTokenNames() { return tokenNames; }
     public String getGrammarFileName() { return "../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g"; }
 
 
 
      	private TreeTestLanguageGrammarAccess grammarAccess;
      	
         public InternalTreeTestLanguageParser(TokenStream input, IAstFactory factory, TreeTestLanguageGrammarAccess grammarAccess) {
             this(input);
             this.factory = factory;
             registerRules(grammarAccess.getGrammar());
             this.grammarAccess = grammarAccess;
         }
         
         @Override
         protected InputStream getTokenFile() {
         	ClassLoader classLoader = getClass().getClassLoader();
         	return classLoader.getResourceAsStream("org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.tokens");
         }
         
         @Override
         protected String getFirstRuleName() {
         	return "Model";	
        	}
        	
        	@Override
        	protected TreeTestLanguageGrammarAccess getGrammarAccess() {
        		return grammarAccess;
        	}
 
 
 
     // $ANTLR start entryRuleModel
     // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:77:1: entryRuleModel returns [EObject current=null] : iv_ruleModel= ruleModel EOF ;
     public final EObject entryRuleModel() throws RecognitionException {
         EObject current = null;
 
         EObject iv_ruleModel = null;
 
 
         try {
             // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:78:2: (iv_ruleModel= ruleModel EOF )
             // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:79:2: iv_ruleModel= ruleModel EOF
             {
              currentNode = createCompositeNode(grammarAccess.getModelRule(), currentNode); 
             pushFollow(FollowSets000.FOLLOW_ruleModel_in_entryRuleModel75);
             iv_ruleModel=ruleModel();
             _fsp--;
 
              current =iv_ruleModel; 
             match(input,EOF,FollowSets000.FOLLOW_EOF_in_entryRuleModel85); 
 
             }
 
         }
          
             catch (RecognitionException re) { 
                 recover(input,re); 
                 appendSkippedTokens();
             } 
         finally {
         }
         return current;
     }
     // $ANTLR end entryRuleModel
 
 
     // $ANTLR start ruleModel
     // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:86:1: ruleModel returns [EObject current=null] : ( (lv_children_0_0= ruleNode ) )* ;
     public final EObject ruleModel() throws RecognitionException {
         EObject current = null;
 
         EObject lv_children_0_0 = null;
 
 
         EObject temp=null; setCurrentLookahead(); resetLookahead(); 
             
         try {
             // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:91:6: ( ( (lv_children_0_0= ruleNode ) )* )
             // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:92:1: ( (lv_children_0_0= ruleNode ) )*
             {
             // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:92:1: ( (lv_children_0_0= ruleNode ) )*
             loop1:
             do {
                 int alt1=2;
                 int LA1_0 = input.LA(1);
 
                 if ( (LA1_0==RULE_ID) ) {
                     alt1=1;
                 }
 
 
                 switch (alt1) {
             	case 1 :
             	    // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:93:1: (lv_children_0_0= ruleNode )
             	    {
             	    // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:93:1: (lv_children_0_0= ruleNode )
             	    // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:94:3: lv_children_0_0= ruleNode
             	    {
             	     
             	    	        currentNode=createCompositeNode(grammarAccess.getModelAccess().getChildrenNodeParserRuleCall_0(), currentNode); 
             	    	    
             	    pushFollow(FollowSets000.FOLLOW_ruleNode_in_ruleModel130);
             	    lv_children_0_0=ruleNode();
             	    _fsp--;
 
 
             	    	        if (current==null) {
             	    	            current = factory.create(grammarAccess.getModelRule().getType().getClassifier());
             	    	            associateNodeWithAstElement(currentNode.getParent(), current);
             	    	        }
             	    	        try {
             	    	       		add(
             	    	       			current, 
             	    	       			"children",
             	    	        		lv_children_0_0, 
             	    	        		"Node", 
             	    	        		currentNode);
             	    	        } catch (ValueConverterException vce) {
             	    				handleValueConverterException(vce);
             	    	        }
             	    	        currentNode = currentNode.getParent();
             	    	    
 
             	    }
 
 
             	    }
             	    break;
 
             	default :
             	    break loop1;
                 }
             } while (true);
 
 
             }
 
              resetLookahead(); 
                 	lastConsumedNode = currentNode;
                 
         }
          
             catch (RecognitionException re) { 
                 recover(input,re); 
                 appendSkippedTokens();
             } 
         finally {
         }
         return current;
     }
     // $ANTLR end ruleModel
 
 
     // $ANTLR start entryRuleNode
     // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:124:1: entryRuleNode returns [EObject current=null] : iv_ruleNode= ruleNode EOF ;
     public final EObject entryRuleNode() throws RecognitionException {
         EObject current = null;
 
         EObject iv_ruleNode = null;
 
 
         try {
             // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:125:2: (iv_ruleNode= ruleNode EOF )
             // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:126:2: iv_ruleNode= ruleNode EOF
             {
              currentNode = createCompositeNode(grammarAccess.getNodeRule(), currentNode); 
             pushFollow(FollowSets000.FOLLOW_ruleNode_in_entryRuleNode166);
             iv_ruleNode=ruleNode();
             _fsp--;
 
              current =iv_ruleNode; 
             match(input,EOF,FollowSets000.FOLLOW_EOF_in_entryRuleNode176); 
 
             }
 
         }
          
             catch (RecognitionException re) { 
                 recover(input,re); 
                 appendSkippedTokens();
             } 
         finally {
         }
         return current;
     }
     // $ANTLR end entryRuleNode
 
 
     // $ANTLR start ruleNode
     // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:133:1: ruleNode returns [EObject current=null] : ( ( (lv_name_0_0= RULE_ID ) ) '(' ( (lv_attrib_2_0= RULE_STRING ) ) ')' '{' ( (lv_children_5_0= ruleNode ) )* '};' ) ;
     public final EObject ruleNode() throws RecognitionException {
         EObject current = null;
 
         Token lv_name_0_0=null;
         Token lv_attrib_2_0=null;
         EObject lv_children_5_0 = null;
 
 
         EObject temp=null; setCurrentLookahead(); resetLookahead(); 
             
         try {
             // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:138:6: ( ( ( (lv_name_0_0= RULE_ID ) ) '(' ( (lv_attrib_2_0= RULE_STRING ) ) ')' '{' ( (lv_children_5_0= ruleNode ) )* '};' ) )
             // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:139:1: ( ( (lv_name_0_0= RULE_ID ) ) '(' ( (lv_attrib_2_0= RULE_STRING ) ) ')' '{' ( (lv_children_5_0= ruleNode ) )* '};' )
             {
             // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:139:1: ( ( (lv_name_0_0= RULE_ID ) ) '(' ( (lv_attrib_2_0= RULE_STRING ) ) ')' '{' ( (lv_children_5_0= ruleNode ) )* '};' )
             // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:139:2: ( (lv_name_0_0= RULE_ID ) ) '(' ( (lv_attrib_2_0= RULE_STRING ) ) ')' '{' ( (lv_children_5_0= ruleNode ) )* '};'
             {
             // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:139:2: ( (lv_name_0_0= RULE_ID ) )
             // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:140:1: (lv_name_0_0= RULE_ID )
             {
             // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:140:1: (lv_name_0_0= RULE_ID )
             // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:141:3: lv_name_0_0= RULE_ID
             {
             lv_name_0_0=(Token)input.LT(1);
             match(input,RULE_ID,FollowSets000.FOLLOW_RULE_ID_in_ruleNode218); 
 
             			createLeafNode(grammarAccess.getNodeAccess().getNameIDTerminalRuleCall_0_0(), "name"); 
             		
 
             	        if (current==null) {
             	            current = factory.create(grammarAccess.getNodeRule().getType().getClassifier());
             	            associateNodeWithAstElement(currentNode, current);
             	        }
             	        try {
             	       		set(
             	       			current, 
             	       			"name",
             	        		lv_name_0_0, 
             	        		"ID", 
             	        		lastConsumedNode);
             	        } catch (ValueConverterException vce) {
             				handleValueConverterException(vce);
             	        }
             	    
 
             }
 
 
             }
 
             match(input,11,FollowSets000.FOLLOW_11_in_ruleNode233); 
 
                     createLeafNode(grammarAccess.getNodeAccess().getLeftParenthesisKeyword_1(), null); 
                 
             // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:167:1: ( (lv_attrib_2_0= RULE_STRING ) )
             // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:168:1: (lv_attrib_2_0= RULE_STRING )
             {
             // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:168:1: (lv_attrib_2_0= RULE_STRING )
             // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:169:3: lv_attrib_2_0= RULE_STRING
             {
             lv_attrib_2_0=(Token)input.LT(1);
             match(input,RULE_STRING,FollowSets000.FOLLOW_RULE_STRING_in_ruleNode250); 
 
             			createLeafNode(grammarAccess.getNodeAccess().getAttribSTRINGTerminalRuleCall_2_0(), "attrib"); 
             		
 
             	        if (current==null) {
             	            current = factory.create(grammarAccess.getNodeRule().getType().getClassifier());
             	            associateNodeWithAstElement(currentNode, current);
             	        }
             	        try {
             	       		set(
             	       			current, 
             	       			"attrib",
             	        		lv_attrib_2_0, 
             	        		"STRING", 
             	        		lastConsumedNode);
             	        } catch (ValueConverterException vce) {
             				handleValueConverterException(vce);
             	        }
             	    
 
             }
 
 
             }
 
             match(input,12,FollowSets000.FOLLOW_12_in_ruleNode265); 
 
                     createLeafNode(grammarAccess.getNodeAccess().getRightParenthesisKeyword_3(), null); 
                 
             match(input,13,FollowSets000.FOLLOW_13_in_ruleNode275); 
 
                     createLeafNode(grammarAccess.getNodeAccess().getLeftCurlyBracketKeyword_4(), null); 
                 
             // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:199:1: ( (lv_children_5_0= ruleNode ) )*
             loop2:
             do {
                 int alt2=2;
                 int LA2_0 = input.LA(1);
 
                 if ( (LA2_0==RULE_ID) ) {
                     alt2=1;
                 }
 
 
                 switch (alt2) {
             	case 1 :
             	    // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:200:1: (lv_children_5_0= ruleNode )
             	    {
             	    // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:200:1: (lv_children_5_0= ruleNode )
             	    // ../org.eclipse.xtext.xtend.tests/src-gen/org/eclipse/xtext/xtend/parser/antlr/internal/InternalTreeTestLanguage.g:201:3: lv_children_5_0= ruleNode
             	    {
             	     
             	    	        currentNode=createCompositeNode(grammarAccess.getNodeAccess().getChildrenNodeParserRuleCall_5_0(), currentNode); 
             	    	    
             	    pushFollow(FollowSets000.FOLLOW_ruleNode_in_ruleNode296);
             	    lv_children_5_0=ruleNode();
             	    _fsp--;
 
 
             	    	        if (current==null) {
             	    	            current = factory.create(grammarAccess.getNodeRule().getType().getClassifier());
             	    	            associateNodeWithAstElement(currentNode.getParent(), current);
             	    	        }
             	    	        try {
             	    	       		add(
             	    	       			current, 
             	    	       			"children",
             	    	        		lv_children_5_0, 
             	    	        		"Node", 
             	    	        		currentNode);
             	    	        } catch (ValueConverterException vce) {
             	    				handleValueConverterException(vce);
             	    	        }
             	    	        currentNode = currentNode.getParent();
             	    	    
 
             	    }
 
 
             	    }
             	    break;
 
             	default :
             	    break loop2;
                 }
             } while (true);
 
             match(input,14,FollowSets000.FOLLOW_14_in_ruleNode307); 
 
                     createLeafNode(grammarAccess.getNodeAccess().getRightCurlyBracketSemicolonKeyword_6(), null); 
                 
 
             }
 
 
             }
 
              resetLookahead(); 
                 	lastConsumedNode = currentNode;
                 
         }
          
             catch (RecognitionException re) { 
                 recover(input,re); 
                 appendSkippedTokens();
             } 
         finally {
         }
         return current;
     }
     // $ANTLR end ruleNode
 
 
  
 
     
     private static class FollowSets000 {
         public static final BitSet FOLLOW_ruleModel_in_entryRuleModel75 = new BitSet(new long[]{0x0000000000000000L});
         public static final BitSet FOLLOW_EOF_in_entryRuleModel85 = new BitSet(new long[]{0x0000000000000002L});
         public static final BitSet FOLLOW_ruleNode_in_ruleModel130 = new BitSet(new long[]{0x0000000000000012L});
         public static final BitSet FOLLOW_ruleNode_in_entryRuleNode166 = new BitSet(new long[]{0x0000000000000000L});
         public static final BitSet FOLLOW_EOF_in_entryRuleNode176 = new BitSet(new long[]{0x0000000000000002L});
         public static final BitSet FOLLOW_RULE_ID_in_ruleNode218 = new BitSet(new long[]{0x0000000000000800L});
         public static final BitSet FOLLOW_11_in_ruleNode233 = new BitSet(new long[]{0x0000000000000020L});
         public static final BitSet FOLLOW_RULE_STRING_in_ruleNode250 = new BitSet(new long[]{0x0000000000001000L});
         public static final BitSet FOLLOW_12_in_ruleNode265 = new BitSet(new long[]{0x0000000000002000L});
         public static final BitSet FOLLOW_13_in_ruleNode275 = new BitSet(new long[]{0x0000000000004010L});
         public static final BitSet FOLLOW_ruleNode_in_ruleNode296 = new BitSet(new long[]{0x0000000000004010L});
         public static final BitSet FOLLOW_14_in_ruleNode307 = new BitSet(new long[]{0x0000000000000002L});
     }
 
 
 }
