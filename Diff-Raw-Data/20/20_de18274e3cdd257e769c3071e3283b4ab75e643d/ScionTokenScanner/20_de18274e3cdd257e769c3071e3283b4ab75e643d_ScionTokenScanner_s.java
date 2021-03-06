 package net.sf.eclipsefp.haskell.ui.internal.editors.haskell.text;
 
 import java.io.File;
 import java.io.InputStream;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.ListIterator;
 import java.util.Map;
 import net.sf.eclipsefp.haskell.buildwrapper.BWFacade;
 import net.sf.eclipsefp.haskell.buildwrapper.BuildWrapperPlugin;
 import net.sf.eclipsefp.haskell.buildwrapper.types.Occurrence;
 import net.sf.eclipsefp.haskell.buildwrapper.types.TokenDef;
 import net.sf.eclipsefp.haskell.core.codeassist.IScionTokens;
 import net.sf.eclipsefp.haskell.core.util.ResourceUtil;
 import net.sf.eclipsefp.haskell.ui.HaskellUIPlugin;
 import net.sf.eclipsefp.haskell.ui.internal.preferences.editor.IEditorPreferenceNames;
 import net.sf.eclipsefp.haskell.ui.internal.preferences.editor.SyntaxPreviewer;
 import net.sf.eclipsefp.haskell.ui.internal.util.UITexts;
 import org.eclipse.core.resources.IFile;
 import org.eclipse.jface.dialogs.MessageDialog;
 import org.eclipse.jface.text.BadLocationException;
 import org.eclipse.jface.text.IDocument;
 import org.eclipse.jface.text.rules.IPartitionTokenScanner;
 import org.eclipse.jface.text.rules.IToken;
 import org.eclipse.jface.text.rules.Token;
 import org.eclipse.swt.widgets.Display;
 import org.json.JSONArray;
 
 /**
  * Uses Scion tokenTypesArbitrary function to get tokens for a haskell source
   *
   * @author JP Moresmau
  */
 public class ScionTokenScanner implements IPartitionTokenScanner, IEditorPreferenceNames {
   private final ScannerManager man;
   private final IFile file;
 
   private IDocument doc;
   private String contents;
 
   private TokenDef currentTokenDef;
   private List<TokenDef> lTokenDefs;
   private ListIterator<TokenDef> tokenDefs;
 
   private final Map<List<String>,List<TokenDef>> occurrences=new HashMap<List<String>,List<TokenDef>>();
   private final List<List<String>> tokenLocations=new ArrayList<List<String>>();
 
   private IToken currentToken;
   private int currentOffset;
   private int currentLength;
 
   private int offset;
   private int length;
 
   private File tgt;
 
   private boolean checkedTabs=false;
 
   private final Map<String,IToken> tokenByTypes;
 
   public ScionTokenScanner(final ScannerManager man,final IFile file){
     this.man=man;
     this.file=file;
     this.tokenByTypes = new HashMap<String, IToken>() {
       // Eclipse insists on a serial version identifier, not that this hash map will ever
       // get serialized...
       private static final long serialVersionUID = 3579246300065591883L;
       {
         put( IScionTokens.LITERAL_STRING, man.createToken( EDITOR_STRING_COLOR, EDITOR_STRING_BOLD ) );
         put( IScionTokens.LITERAL_CHAR, man.createToken( EDITOR_CHAR_COLOR, EDITOR_CHAR_BOLD ) );
         put( IScionTokens.DOCUMENTATION_ANNOTATION, man.createToken( EDITOR_COMMENT_COLOR,EDITOR_COMMENT_BOLD  ) );
         put( IScionTokens.LITERATE_COMMENT, man.createToken( EDITOR_LITERATE_COMMENT_COLOR, EDITOR_LITERATE_COMMENT_BOLD  ) );
         put( IScionTokens.KEYWORD, man.createToken( EDITOR_KEYWORD_COLOR, EDITOR_KEYWORD_BOLD   ) );
         put( IScionTokens.GHC_EXTENSION_KEYWORD, man.createToken( EDITOR_KEYWORD_COLOR, EDITOR_KEYWORD_BOLD   ) );
         put( IScionTokens.LITERAL_INTEGER, man.createToken( EDITOR_NUMBER_COLOR, EDITOR_NUMBER_BOLD   ) );
         put( IScionTokens.LITERAL_RATIONAL, man.createToken( EDITOR_NUMBER_COLOR, EDITOR_NUMBER_BOLD   ) );
         put( IScionTokens.LITERAL_WORD, man.createToken( EDITOR_NUMBER_COLOR, EDITOR_NUMBER_BOLD   ) );
         put( IScionTokens.LITERAL_FLOAT, man.createToken( EDITOR_NUMBER_COLOR, EDITOR_NUMBER_BOLD   ) );
         put( IScionTokens.IDENTIFIER_CONSTRUCTOR, man.createToken( EDITOR_CON_COLOR, EDITOR_CON_BOLD  ) );
         put( IScionTokens.IDENTIFIER_VARIABLE, man.createToken( EDITOR_VAR_COLOR, EDITOR_VAR_BOLD   ) );
         put( IScionTokens.SYMBOL_RESERVED, man.createToken( EDITOR_SYMBOL_COLOR, EDITOR_SYMBOL_BOLD   ) );
         put( IScionTokens.SYMBOL_SPECIAL, man.createToken( EDITOR_SYMBOL_COLOR, EDITOR_SYMBOL_BOLD   ) );
         put( IScionTokens.PREPROCESSOR_TEXT, man.createToken( EDITOR_CPP_COLOR, EDITOR_CPP_BOLD   ) );
         put( IScionTokens.TEMPLATE_HASKELL, man.createToken( EDITOR_TH_COLOR, EDITOR_TH_BOLD   ) );
       }
     };
   }
 
   public int getTokenLength() {
     if (currentTokenDef!=null){
       return currentLength;
 
     }
     return 0;
   }
 
   public int getTokenOffset() {
     if (currentTokenDef!=null){
       return currentOffset;
     }
     return 0;
   }
 
   public IToken nextToken() {
 
      do {
        if (tokenDefs!=null && tokenDefs.hasNext()){
          TokenDef nextTokenDef=tokenDefs.next();
          try {
            int nextOffset=nextTokenDef.getLocation().getStartOffset( doc );
            int nextEnd=nextTokenDef.getLocation().getEndOffset( doc );
            int end=Math.min( offset+length,nextEnd);
 
            //addTokenOccurence( nextOffset, nextEnd, nextTokenDef );
 
            IToken nextToken=getTokenFromTokenDef( nextTokenDef);
            if (currentToken!=null && currentToken.getData().equals( nextToken.getData() ) &&
                currentOffset+currentLength<nextOffset){
              nextOffset= currentOffset+currentLength;
            }
            int nextLength=end-nextOffset;
            currentLength=nextLength;
            currentOffset=nextOffset;
            currentTokenDef=nextTokenDef;
            currentToken=nextToken;
 
            if (currentOffset>offset+length)  {
              return Token.EOF;
            }
 
 
          } catch (BadLocationException ble){
            HaskellUIPlugin.log( ble );
          }
        } else {
          return Token.EOF;
        }
     } while(currentOffset<offset);
     return currentToken;
 
   }
 
   private void addTokenOccurence(final String s,final int offset,final int end,final TokenDef td){
     String name=td.getName();
     if (name.equals( IScionTokens.KEYWORD )
         || name.equals( IScionTokens.GHC_EXTENSION_KEYWORD )
         || name.equals( IScionTokens.IDENTIFIER_CONSTRUCTOR )
         || name.equals( IScionTokens.IDENTIFIER_VARIABLE )
         || name.equals( IScionTokens.SYMBOL_RESERVED )
         ){  //|| name.equals( IScionTokens.SYMBOL_SPECIAL )
 
       List<String> key=new LinkedList<String>();
       key.add(td.getName());
       key.add( s.substring(offset,end) );
 
       while (tokenLocations.size()<offset){
         tokenLocations.add( null );
       }
       while(tokenLocations.size()<end){
         tokenLocations.add(key);
       }
       List<TokenDef> l=occurrences.get( key );
       if (l==null){
         l=new LinkedList<TokenDef>();
         occurrences.put( key, l );
       }
       l.add(td);
 
     }
   }
 
   public List<Occurrence> getOccurrences(final int offset){
     LinkedList<Occurrence> ret=new LinkedList<Occurrence>();
     if (offset>0 && offset<tokenLocations.size()){
       List<String> key=tokenLocations.get( offset );
       if (key!=null){
         List<TokenDef> l=occurrences.get( key );
         if (l!=null){
 
           for (TokenDef td:l){
             ret.add(new Occurrence( td ));
           }
           return ret;
         }
       }
     }
     return ret;
   }
 
   public void setRange( final IDocument document, final int offset, final int length ) {
     currentTokenDef = null;
     // currentToken=null;
     tokenDefs = null;
 
     occurrences.clear();
     tokenLocations.clear();
 
     if( file != null ) {
       String newContents = document.get();
       if( !document.equals( doc ) || !newContents.equals( contents ) || lTokenDefs == null ) {
         doc = document;
         contents = newContents;
         if (!checkedTabs){
             checkedTabs=true;
             if (contents.contains( "\t" )){
 
             if (MessageDialog.openConfirm( Display.getCurrent().getActiveShell() , UITexts.error_tabs  , UITexts.error_tabs_message )){
               /*IDocumentProvider prov=new TextFileDocumentProvider();
               prov.connect(file);
               IDocument doc2=prov.getDocument( file );*/
               int tw=HaskellUIPlugin.getDefault().getPreferenceStore().getInt( EDITOR_TAB_WIDTH );
               StringBuilder sb=new StringBuilder();
               for (int a=0;a<tw;a++){
                 sb.append(" ");
               }
               contents=contents.replace( "\t",sb.toString() );
               // doc2.set(contents);
               document.set(contents);
               // prov.saveDocument( new NullProgressMonitor(), file, doc2, true );
             }
           }
         }
         BWFacade f=BuildWrapperPlugin.getFacade( file.getProject() );
         if (f==null){
           f=new BWFacade();
           f.setBwPath( BuildWrapperPlugin.getBwPath() );
           f.setProject( file.getProject() );
           f.setWorkingDir(new File(file.getProject().getLocation().toOSString()));
         }
 
         //long t0=System.currentTimeMillis();
         if (tgt==null){
           tgt=f.write( file, contents );
         } else {
           f.write( tgt, contents );
         }
         //long t01=System.currentTimeMillis();
         lTokenDefs = f.tokenTypes( file);
         //long t1=System.currentTimeMillis();
         //int l=ScionPlugin.getSharedScionInstance().tokenTypes( file, contents ).size();
         //long t2=System.currentTimeMillis();
         //HaskellUIPlugin.log( "bw:"+(t1-t0)+"ms ("+lTokenDefs.size()+",write: "+(t01-t0)+"ms ), scion:"+(t2-t1)+"ms ("+l+")", IStatus.INFO );
       }
     } else {
       try {
         InputStream stream = SyntaxPreviewer.class.getResourceAsStream( "preview.json" );
         // preview file
         JSONArray result = new JSONArray( ResourceUtil.readStream( stream ) );
         lTokenDefs = new ArrayList<TokenDef>( result.length() );
         for( int i = 0; i < result.length(); ++i ) {
           JSONArray arr = result.getJSONArray( i );
           lTokenDefs.add( new TokenDef( arr ) );
         }
       } catch( Exception ex ) {
         HaskellUIPlugin.log( "Could not read preview file.", ex ); //$NON-NLS-1$
       }
     }
     this.doc = document;
     if( lTokenDefs != null && lTokenDefs.size() > 0 ) {
       tokenDefs = lTokenDefs.listIterator();
     }
     String s=doc.get();
     for (TokenDef nextTokenDef:lTokenDefs){
       try {
         int nextOffset=nextTokenDef.getLocation().getStartOffset( doc );
         int nextEnd=nextTokenDef.getLocation().getEndOffset( doc );
         addTokenOccurence( s,nextOffset, nextEnd, nextTokenDef );
       } catch (BadLocationException ble){
         HaskellUIPlugin.log( ble );
       }
     }
     this.offset = offset;
     this.length = length;
   }
 
   private IToken getTokenFromTokenDef(final TokenDef td){
     IToken tok=tokenByTypes.get(td.getName());
     if (tok!=null){
       return tok;
     }
     return man.createToken( EDITOR_DEFAULT_COLOR, EDITOR_DEFAULT_BOLD );
   }
 
   public void setPartialRange( final IDocument document, final int offset, final int length,
       final String contentType, final int partitionOffset ) {
     setRange( document, offset, length );
   }
 
 }
