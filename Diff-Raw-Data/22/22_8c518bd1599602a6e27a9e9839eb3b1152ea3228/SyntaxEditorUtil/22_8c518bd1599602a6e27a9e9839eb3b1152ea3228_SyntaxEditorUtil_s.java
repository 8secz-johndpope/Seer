 package com.eviware.soapui.support.xml;
 
 import java.awt.Color;
 
 import javax.swing.BorderFactory;
 import javax.swing.JPopupMenu;
 import javax.swing.JSeparator;
 import javax.swing.KeyStroke;
 
 import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
 import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
 import org.fife.ui.rtextarea.RTextScrollPane;
 
 import com.eviware.soapui.support.UISupport;
 import com.eviware.soapui.support.xml.actions.EnableLineNumbersAction;
 import com.eviware.soapui.support.xml.actions.FormatXmlAction;
 import com.eviware.soapui.support.xml.actions.GoToLineAction;
 import com.eviware.soapui.support.xml.actions.InsertBase64FileTextAreaAction;
 import com.eviware.soapui.support.xml.actions.LoadXmlTextAreaAction;
 import com.eviware.soapui.support.xml.actions.SaveXmlTextAreaAction;
 import com.eviware.soapui.ui.support.FindAndReplaceDialogView;
 
 public class SyntaxEditorUtil
 {
 	public static RSyntaxTextArea createDefaultXmlSyntaxTextArea()
 	{
 		return createDefaultSyntaxTextArea( SyntaxConstants.SYNTAX_STYLE_XML );
 	}
 
 	public static RSyntaxTextArea createDefaultJsonSyntaxTextArea()
 	{
 		return createDefaultSyntaxTextArea( SyntaxConstants.SYNTAX_STYLE_XML );
 	}
 
 	public static RSyntaxTextArea createDefaultJavaScriptSyntaxTextArea()
 	{
 		return createDefaultSyntaxTextArea( SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT );
 	}
 
 	public static RSyntaxTextArea createDefaultSQLSyntaxTextArea()
 	{
		return createDefaultSyntaxTextArea( SyntaxConstants.SYNTAX_STYLE_SQL );
 	}
 
	private static RSyntaxTextArea createDefaultSyntaxTextArea( String type )
 	{
		RSyntaxTextArea textArea = new RSyntaxTextArea();
		textArea.setSyntaxEditingStyle( type );
 		textArea.setFont( UISupport.getEditorFont() );
 		textArea.setCodeFoldingEnabled( true );
 		textArea.setAntiAliasingEnabled( true );
 		textArea.setCaretPosition( 0 );
 		textArea.setBorder( BorderFactory.createMatteBorder( 0, 2, 0, 0, Color.WHITE ) );
 		return textArea;
 	}
 
 	public static RSyntaxTextArea addDefaultActions( RSyntaxTextArea editor, RTextScrollPane scrollPane, boolean readOnly )
 	{
 		JPopupMenu popupMenu = editor.getPopupMenu();
 
 		SaveXmlTextAreaAction saveXmlTextAreaAction = new SaveXmlTextAreaAction( editor, "Save" );
 		EnableLineNumbersAction enableLineNumbersAction = new EnableLineNumbersAction( scrollPane, "Toggle Line Numbers" );
 		GoToLineAction goToLineAction = new GoToLineAction( editor, "Go To Line" );
 
 		int cnt = popupMenu.getComponentCount();
 		for( int i = cnt - 1; i >= 0; i-- )
 		{
 			if( popupMenu.getComponent( i ) instanceof JSeparator )
 			{
 				popupMenu.remove( popupMenu.getComponent( i ) );
 			}
 		}
 		FormatXmlAction formatXmlAction = null;
 		if( !readOnly )
 		{
 			formatXmlAction = new FormatXmlAction( editor );
 			FindAndReplaceDialogView findAndReplaceDialog = new FindAndReplaceDialogView( editor );
 			popupMenu.insert( formatXmlAction, 1 );
 			popupMenu.addSeparator();
 			popupMenu.add( findAndReplaceDialog );
 			editor.getInputMap().put( KeyStroke.getKeyStroke( "F3" ), findAndReplaceDialog );
 		}
 		popupMenu.addSeparator();
 		popupMenu.add( goToLineAction );
 		popupMenu.add( enableLineNumbersAction );
 		popupMenu.addSeparator();
 		popupMenu.add( saveXmlTextAreaAction );
 
 		LoadXmlTextAreaAction loadXmlTextAreaAction = null;
 		InsertBase64FileTextAreaAction insertBase64FileTextAreaAction = null;
 		if( !readOnly )
 		{
 			loadXmlTextAreaAction = new LoadXmlTextAreaAction( editor, "Load" );
 			insertBase64FileTextAreaAction = new InsertBase64FileTextAreaAction( editor, "Insert File as Base64" );
 			popupMenu.add( loadXmlTextAreaAction );
 			popupMenu.add( insertBase64FileTextAreaAction );
 		}
 
 		if( UISupport.isMac() )
 		{
 			editor.getInputMap().put( KeyStroke.getKeyStroke( "meta S" ), saveXmlTextAreaAction );
 			editor.getInputMap().put( KeyStroke.getKeyStroke( "control L" ), enableLineNumbersAction );
 			editor.getInputMap().put( KeyStroke.getKeyStroke( "control meta L" ), goToLineAction );
 			if( !readOnly )
 			{
 				editor.getInputMap().put( KeyStroke.getKeyStroke( "shift meta F" ), formatXmlAction );
 				editor.getInputMap().put( KeyStroke.getKeyStroke( "meta L" ), loadXmlTextAreaAction );
 			}
 		}
 		else
 		{
 			editor.getInputMap().put( KeyStroke.getKeyStroke( "ctrl S" ), saveXmlTextAreaAction );
 			editor.getInputMap().put( KeyStroke.getKeyStroke( "alt L" ), enableLineNumbersAction );
 			editor.getInputMap().put( KeyStroke.getKeyStroke( "control alt L" ), goToLineAction );
 			if( !readOnly )
 			{
 				editor.getInputMap().put( KeyStroke.getKeyStroke( "alt F" ), formatXmlAction );
 				editor.getInputMap().put( KeyStroke.getKeyStroke( "ctrl L" ), loadXmlTextAreaAction );
 			}
 		}
 		if( !readOnly )
 		{
 
 		}
 
 		return editor;
 	}
 }
