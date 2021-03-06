 
 /*
  * generated by Xtext
  */
 package org.eclipse.xtext.xtend.ui;
 
 import org.eclipse.xtext.ui.DefaultUiModule;
 import org.eclipse.ui.plugin.AbstractUIPlugin;
 
 /**
  * Manual modifications go to {org.eclipse.xtext.xtend.ui.TreeTestLanguageUiModule}
  */
 public abstract class AbstractTreeTestLanguageUiModule extends DefaultUiModule {
 	
 	public AbstractTreeTestLanguageUiModule(AbstractUIPlugin plugin) {
 		super(plugin);
 	}
 	
 	
 	// contributed by de.itemis.xtext.antlr.XtextAntlrGeneratorFragment
 	public Class<? extends org.eclipse.jface.text.rules.ITokenScanner> bindITokenScanner() {
 		return org.eclipse.xtext.ui.editor.syntaxcoloring.antlr.AntlrTokenScanner.class;
 	}
 
 	// contributed by de.itemis.xtext.antlr.XtextAntlrGeneratorFragment
 	public Class<? extends org.eclipse.xtext.ui.editor.contentassist.IProposalConflictHelper> bindIProposalConflictHelper() {
 		return org.eclipse.xtext.ui.editor.contentassist.antlr.AntlrProposalConflictHelper.class;
 	}
 
 	// contributed by de.itemis.xtext.antlr.XtextAntlrGeneratorFragment
 	public Class<? extends org.eclipse.xtext.ui.editor.IDamagerRepairer> bindIDamagerRepairer() {
 		return org.eclipse.xtext.ui.editor.XtextDamagerRepairer.class;
 	}
 
 	// contributed by de.itemis.xtext.antlr.XtextAntlrGeneratorFragment
 	public void configureHighlightingLexer(com.google.inject.Binder binder) {
 		binder.bind(org.eclipse.xtext.parser.antlr.Lexer.class).annotatedWith(com.google.inject.name.Names.named(org.eclipse.xtext.ui.LexerUIBindings.HIGHLIGHTING)).to(org.eclipse.xtext.xtend.parser.antlr.internal.InternalTreeTestLanguageLexer.class);
 	}
 
 	// contributed by de.itemis.xtext.antlr.XtextAntlrGeneratorFragment
 	public void configureHighlightingTokenDefProvider(com.google.inject.Binder binder) {
 		binder.bind(org.eclipse.xtext.parser.antlr.ITokenDefProvider.class).annotatedWith(com.google.inject.name.Names.named(org.eclipse.xtext.ui.LexerUIBindings.HIGHLIGHTING)).to(org.eclipse.xtext.parser.antlr.AntlrTokenDefProvider.class);
 	}
 
 
 }
