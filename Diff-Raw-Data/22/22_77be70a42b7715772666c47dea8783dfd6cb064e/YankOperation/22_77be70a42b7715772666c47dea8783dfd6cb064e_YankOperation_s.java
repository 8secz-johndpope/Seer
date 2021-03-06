 package net.sourceforge.vrapper.vim.commands;
 
 import net.sourceforge.vrapper.utils.ContentType;
 import net.sourceforge.vrapper.utils.Position;
 import net.sourceforge.vrapper.utils.TextRange;
 import net.sourceforge.vrapper.utils.VimUtils;
 import net.sourceforge.vrapper.vim.EditorAdaptor;
 import net.sourceforge.vrapper.vim.modes.NormalMode;
 import net.sourceforge.vrapper.vim.register.RegisterContent;
 import net.sourceforge.vrapper.vim.register.StringRegisterContent;
 
 public class YankOperation extends SimpleTextOperation {
 
     public static final YankOperation INSTANCE = new YankOperation();
 
     private YankOperation() { /* NOP */ }
 
     @Override
     public void execute(EditorAdaptor editorAdaptor, TextRange region, ContentType contentType) {
         doIt(editorAdaptor, region, contentType);
     }
 
     public TextOperation repetition() {
         return null;
     }
 
     public static void doIt(EditorAdaptor editorAdaptor, TextRange range, ContentType contentType) {
         String text = editorAdaptor.getModelContent().getText(range.getLeftBound().getModelOffset(), range.getModelLength());
         //if we're expecting lines and this text doesn't end in a newline,
         //manually append a newline to the end
         //(this to handle yanking the last line of a file)
         if (contentType == ContentType.LINES && (text.length() == 0 || ! VimUtils.isNewLine(text.substring(text.length()-1)))) {
             text += editorAdaptor.getConfiguration().getNewLine();
         }
         
         RegisterContent content = new StringRegisterContent(contentType, text);
         editorAdaptor.getRegisterManager().getActiveRegister().setContent(content);
         if (contentType == ContentType.LINES && NormalMode.NAME.equals(editorAdaptor.getCurrentModeName())) {
             //if this is line-wise, move cursor to first line in selection but keep stickyColumn
         	//(keep stickyColumn for the 'yy' case)
             int lineNo = editorAdaptor.getModelContent().getLineInformationOfOffset(range.getLeftBound().getModelOffset()).getNumber();
             Position stickyPosition = editorAdaptor.getCursorService().stickyColumnAtModelLine(lineNo);
             editorAdaptor.getCursorService().setPosition(stickyPosition, true);
         }
         else {
         	Position cursor = editorAdaptor.getCursorService().getPosition();
         	Position newPos = range.getLeftBound();
         	//if cursor is at beginning of selection, leave it there
         	if(cursor.getModelOffset() != newPos.getModelOffset()) {
        		if(editorAdaptor.getCurrentModeName().contains("visual")) {
        			//bug in Eclipse Juno? setPosition(range.getLeftBound()) moves the cursor 
        			//one character before the actual selection (previous line ending when linewise)
        			newPos = editorAdaptor.getCursorService().newPositionForModelOffset(newPos.getModelOffset()+1);
        		}
         		//move cursor to beginning of selection
         		editorAdaptor.getCursorService().setPosition(newPos, true);
         	}
         }
     }
 
 }
