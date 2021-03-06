 package org.rubypeople.rdt.internal.ui.text.ruby;
 
 import java.util.regex.Pattern;
 
 import org.eclipse.jface.preference.IPreferenceStore;
 import org.eclipse.jface.text.BadLocationException;
 import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
 import org.eclipse.jface.text.DocumentCommand;
 import org.eclipse.jface.text.IDocument;
 import org.eclipse.jface.text.IRegion;
 import org.eclipse.jface.text.ITypedRegion;
 import org.eclipse.jface.text.TextUtilities;
 import org.eclipse.jface.util.IPropertyChangeListener;
 import org.eclipse.jface.util.PropertyChangeEvent;
 import org.jruby.lexer.yacc.SyntaxException;
 import org.rubypeople.rdt.core.IRubyProject;
 import org.rubypeople.rdt.internal.core.parser.RubyParser;
 import org.rubypeople.rdt.internal.corext.util.CodeFormatterUtil;
 import org.rubypeople.rdt.internal.ui.RubyPlugin;
 import org.rubypeople.rdt.internal.ui.text.IRubyPartitions;
 import org.rubypeople.rdt.internal.ui.text.RubyHeuristicScanner;
 import org.rubypeople.rdt.internal.ui.text.RubyIndenter;
 import org.rubypeople.rdt.ui.PreferenceConstants;
 
 public class RubyAutoIndentStrategy extends DefaultIndentLineAutoEditStrategy implements IPropertyChangeListener {
 
 	/** Preference key for automatically 'end'ing statements */
 	private final static String END_STATEMENTS= PreferenceConstants.EDITOR_END_STATEMENTS;
 	
 	private final Pattern openBlockPattern = Pattern.compile(".*[\\S].*do[\\w|\\s]*");
 	
 	private static final String BLOCK_CLOSER = "end";
 	private String fPartitioning;
 	private final IRubyProject fProject;
 	private boolean endStatements;
 	private IPreferenceStore fPreferenceStore;
 
 	/**
 	 * Creates a new Ruby auto indent strategy for the given document partitioning.
 	 *
 	 * @param partitioning the document partitioning
 	 * @param project the project to get formatting preferences from, or null to use default preferences
 	 */
 	public RubyAutoIndentStrategy(String partitioning, IRubyProject project) {
 		fPartitioning= partitioning;
 		fProject= project;
 		fPreferenceStore = RubyPlugin.getDefault().getPreferenceStore();
 		endStatements= fPreferenceStore.getBoolean(END_STATEMENTS);
 		fPreferenceStore.addPropertyChangeListener(this);
  	}
 	
 	/*
 	 * @see org.eclipse.jface.text.IAutoIndentStrategy#customizeDocumentCommand(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.DocumentCommand)
 	 */
 	public void customizeDocumentCommand(IDocument d, DocumentCommand c) {
 		if (c.doit == false)
 			return;
 		if (c.length == 0 && c.text != null && isLineDelimiter(d, c.text))
 			smartIndentAfterNewLine(d, c);
 	}
 	
     private boolean isLineDelimiter(IDocument document, String text) {
 		String[] delimiters= document.getLegalLineDelimiters();
 		if (delimiters != null)
 			return TextUtilities.equals(delimiters, text) > -1;
 		return false;
 	}
     
 	private void smartIndentAfterNewLine(IDocument d, DocumentCommand c) {
 		RubyHeuristicScanner scanner= new RubyHeuristicScanner(d);
 		RubyIndenter indenter= new RubyIndenter(d, scanner, fProject);
 		StringBuffer indent= indenter.computeIndentation(c.offset);
 		if (indent == null)
 			indent= new StringBuffer(); 
 
 		int docLength= d.getLength();
 		if (c.offset == -1 || docLength == 0)
 			return;
 
 		try {
 			int p= (c.offset == docLength ? c.offset - 1 : c.offset);
 			int line= d.getLineOfOffset(p);
 
 			StringBuffer buf= new StringBuffer(c.text + indent);
 
 
 			IRegion reg= d.getLineInformation(line);
 			int lineEnd= reg.getOffset() + reg.getLength();
 
 			int contentStart= findEndOfWhiteSpace(d, c.offset, lineEnd);
 			c.length=  Math.max(contentStart - c.offset, 0);
 
 			int start= reg.getOffset();
 			ITypedRegion region= TextUtilities.getPartition(d, fPartitioning, start, true);
 //			if (IRubyPartitions.RUBY_DOC.equals(region.getType()))
 //				start= d.getLineInformationOfOffset(region.getOffset()).getOffset();

			// insert closing "end" on new line after an unclosed block
			if (closeBlock() && unclosedBlock(d, start, c.offset)) {
 				buf.append(CodeFormatterUtil.createIndentString(1, fProject));
 				c.caretOffset= c.offset + buf.length();
 				c.shiftsCaret= false;
				
 				// copy old content of line behind insertion point to new line
 				if (c.offset == 0) {
 					if (lineEnd - contentStart > 0) {
 						c.length=  lineEnd - c.offset;
 						buf.append(d.get(contentStart, lineEnd - contentStart).toCharArray());
 					}
 				}
 				
 				buf.append(TextUtilities.getDefaultLineDelimiter(d));
 				buf.append(indent);
 				buf.append(BLOCK_CLOSER);
 			}
 			c.text= buf.toString();
 
 		} catch (BadLocationException e) {
 			RubyPlugin.log(e);
 		}
 	}
 
 	private boolean unclosedBlock(IDocument d, int start, int offset) {
 		// FIXME wow is this ugly! There has to be an easier way to tell if there's an unclosed block besides parsing and catching a syntaxError!
 		try {
			String line = d.get(start, offset - start);
			line = line.trim();
			if (!line.startsWith("class ") && !line.startsWith("if ") && !line.startsWith("module ") && !line.startsWith("unless ")
					&& !line.startsWith("def ") && !line.equals("begin") && !openBlockPattern.matcher(line).matches()) {
 				return false;
 			}
 		} catch (BadLocationException e1) {
 			RubyPlugin.log(e1);
 		}
 		
 		RubyParser parser = new RubyParser();
 		try {
 			parser.parse(d.get());
 		} catch (SyntaxException e) {
 			String msg = e.getMessage();
 			if (msg.contains("expecting") && (msg.contains("kEND") || msg.contains("kTHEN")))
 				return true;
 			try {
 				StringBuffer buffer = new StringBuffer(d.get());
 				buffer.insert(offset, "\n" + BLOCK_CLOSER);
 				parser.parse(buffer.toString());
 			} catch (SyntaxException syntaxException) {
 				return false;
 			}
 			return true;
 		}
 		return false;
 	}
 
 	private boolean closeBlock() {
 		return endStatements;
 	}
 
 	public void propertyChange(PropertyChangeEvent event) {
 		String property = event.getProperty();
 		if (END_STATEMENTS.equals(property)) {
 			endStatements = fPreferenceStore.getBoolean(property);
 			return;
 		}         
 	}
 	
 }
