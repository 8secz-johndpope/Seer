 // Convert.java
 
 package ed.js.engine;
 
 import java.io.*;
 import java.util.*;
 
 import org.mozilla.javascript.*;
 
 import ed.js.*;
 import ed.io.*;
 import ed.util.*;
 
 public class Convert {
 
     static boolean D = Boolean.getBoolean( "DEBUG.JS" );
 
     public Convert( File sourceFile )
         throws IOException {
         this( sourceFile.toString() , StreamUtil.readFully( sourceFile , "UTF-8" ) );
     }
     
     public Convert( String name , String source )
         throws IOException {
         
         _name = name;
         _source = source;
         
         _className = _name.replaceAll(".*/(.*?)","").replaceAll( "[^\\w]+" , "_" );
         
         CompilerEnvirons ce = new CompilerEnvirons();
         
         Parser p = new Parser( ce , ce.getErrorReporter() );
         ScriptOrFnNode theNode = p.parse( _source , _name , 0 );
         _encodedSource = p.getEncodedSource();
         init( theNode );
     }
 
     private void init( ScriptOrFnNode sn ){
         
         if ( _it != null )
             throw new RuntimeException( "too late" );
         
         NodeTransformer nf = new NodeTransformer();
         nf.transform( sn );
         
         if ( D ){
             Debug.print( sn , 0 );
         }
 
         State state = new State();
 
         _setLineNumbers( sn , sn );
         _addFunctionNodes( sn , state );
 
         if ( D ) System.out.println( "***************" );
 
         Node n = sn.getFirstChild();
         
         while ( n != null ){
             if ( n.getType() != Token.FUNCTION ){
 
                 if ( n.getNext() == null && 
                      n.getType() == Token.EXPR_RESULT ){
                     _append( "return " , n );
                     _hasReturn = true;
                 }
                 
                 _add( n , sn , state );
                 
                 _append( "\n" , n );
             }
             n = n.getNext();
         }
         
         if ( ! _hasReturn ) {
             _append( "return null;" , sn );
         }
         else {
             int end = _mainJavaCode.length() - 1;
             boolean alreadyHaveOne = false;
             for ( ; end >= 0; end-- ){
                 char c = _mainJavaCode.charAt( end );
                 
                 if ( Character.isWhitespace( c ) )
                     continue;
 
                 if ( c == ';' ){
                     if ( ! alreadyHaveOne ){
                         alreadyHaveOne = true;
                         continue;
                     }
                     
                     _mainJavaCode.setLength( end + 1 );
                 }
 
                 break;
                     
             }
             
         }
     }
     
     private void _add( Node n , State s ){
         _add( n , null , s );
     }
     
     private void _add( Node n , ScriptOrFnNode sn , State state ){
         
         switch ( n.getType() ){
         
         case Token.TYPEOF:
             _append( "JS_typeof( " , n );
             _assertOne( n );
             _add( n.getFirstChild() , state );
             _append( " ) " , n );
             break;
 
         case Token.TYPEOFNAME:
             _append( "JS_typeof( " , n );
             if ( state.hasSymbol( n.getString() ) )
                 _append( n.getString() , n );
             else
                 _append( "scope.get( \"" + n.getString() + "\" )" , n );
             _append( " ) " , n );
             break;
     
         case Token.REGEXP:
             int myId = _regex.size();
             ScriptOrFnNode parent  = _nodeToSOR.get( n );
             int rId = n.getIntProp( Node.REGEXP_PROP , -1 );
             
             _regex.add( new Pair<String,String>( parent.getRegexpString( rId ) , parent.getRegexpFlags( rId ) ) );
             _append( " _regex[" + myId + "] " , n );
             break;
 
         case Token.ARRAYLIT:
             {
                _append( "( new JSArray( " , n );
                 Node c = n.getFirstChild();
                 while ( c != null ){
                     if ( c != n.getFirstChild() )
                         _append( " , " , n );
                     _add( c , state );
                     c = c.getNext();
                 }
                 _append( " ) ) " , n );
             }
             break;
 
         case Token.OBJECTLIT:
             {
                 _append( "JS_buildLiteralObject( new String[]{ " , n );
                 boolean first = true;
                 for ( Object id : (Object[])n.getProp( Node.OBJECT_IDS_PROP ) ){
                     if ( first )
                         first = false;
                     else 
                         _append( " , " , n );
                     _append( "\"" + id.toString() + "\"" , n );
                 }
                 _append( " } " , n );
                 
                 Node c = n.getFirstChild();
                 while ( c != null ){
                     _append( " , " , n );
                     _add( c , state  );
                     c = c.getNext();
                 }
                 _append( " ) " , n );
             }
             break;
 
         case Token.NEW:
             _append( "scope.clearThisNew( " , n );
             _addCall( n , state , true );
             _append( " ) " , n );
             break;
             
         case Token.THIS:
             _append( "passedIn.getThis()" , n );
             break;
 
         case Token.INC:
         case Token.DEC:
             _assertOne( n );
             
             Node tempChild = n.getFirstChild();
             if ( ( tempChild.getType() == Token.NAME || tempChild.getType() == Token.GETVAR ) &&
                  state.useLocalVariable( tempChild.getString() ) )
                 throw new RuntimeException( "can't increment local variables" );
 
             _append( "JS_inc( " , n );
             _createRef( n.getFirstChild() , state );
             _append( " , " , n );
             _append( String.valueOf( ( n.getIntProp( Node.INCRDECR_PROP , 0 ) & Node.POST_FLAG ) > 0 ) , n );
             _append( " , " , n );
             _append( String.valueOf( n.getType() == Token.INC ? 1 : -1 ) , n );
             _append( ")" , n );
             break;
             
         case Token.USE_STACK:
             _append( "__tempObject.get( " + state._tempOpNames.pop() + " ) "  , n );
             break;
 
         case Token.SETPROP_OP:
         case Token.SETELEM_OP:
             Node theOp = n.getFirstChild().getNext().getNext();
             if ( theOp.getType() == Token.ADD &&
                  ( theOp.getFirstChild().getType() == Token.USE_STACK || 
                    theOp.getFirstChild().getNext().getType() == Token.USE_STACK ) ){
                 _append( "\n" , n );
                 _append( "JS_setDefferedPlus( (JSObject) " , n );
                 _add( n.getFirstChild() , state );
                 _append( " , " , n );
                 _add( n.getFirstChild().getNext() , state );
                 _append( " , " , n );
                 _add( theOp.getFirstChild().getType() == Token.USE_STACK ? 
                       theOp.getFirstChild().getNext() : 
                       theOp.getFirstChild() ,
                       state );
                 _append( " \n ) \n" , n );
                 break;
             }
                    
             _append( "\n { \n" , n );
             
             _append( "JSObject __tempObject = (JSObject)" , n );
             _add( n.getFirstChild() , state );
             _append( ";\n" , n );
 
             
             String tempName = "__temp" + (int)(Math.random() * 10000);
             state._tempOpNames.push( tempName );
             
             _append( "Object " + tempName + " = " , n );
             _add( n.getFirstChild().getNext() , state );
             _append( ";\n" , n );
             
             _append( " __tempObject.set(" , n );
             _append( tempName , n );
             _append( " , " , n );
             _add( n.getFirstChild().getNext().getNext() , state );
             _append( " ); \n" , n );
 
             _append( " } \n" , n );
             
             break;
 
         case Token.SETPROP:
         case Token.SETELEM:
             _append( "((JSObject)" , n );
             _add( n.getFirstChild() , state );
             _append( ").set( " , n );
             _add( n.getFirstChild().getNext() , state );
             _append( " , " , n );
             _add( n.getFirstChild().getNext().getNext() , state );
             _append( " ); " , n );
             break;
 
         case Token.GETPROPNOWARN:
         case Token.GETPROP:
         case Token.GETELEM:
             _append( "((JSObject)" , n );
             _add( n.getFirstChild() , state );
             _append( ").get( " , n );
             _add( n.getFirstChild().getNext() , state );
             _append( " )" , n );
             break;
 
         case Token.EXPR_RESULT:
             _assertOne( n );
             _add( n.getFirstChild() , state );
             _append( ";\n" , n );
             break;
         case Token.CALL:
             _addCall( n , state );
             break;
 
         case Token.NUMBER:
             String temp = String.valueOf( n.getDouble() );
             if ( temp.endsWith( ".0" ) )
                 temp = "Integer.valueOf( " + temp.substring( 0 , temp.length() - 2 ) + " ) ";
             else
                 temp = "Double.valueOf( " + temp + " ) ";
             _append( temp , n );
             break;
         case Token.STRING:
             int stringId = _strings.size();
             _strings.add( n.getString() );
             _append( "_strings[" + stringId + "]" ,n );
             break;
         case Token.TRUE:
             _append( " true " , n );
             break;
         case Token.FALSE:
             _append( " false " , n );
             break;
         case Token.NULL:
             _append( " null " , n );
             break;
 
         case Token.VAR:
             _addVar( n , state );
             break;
         case Token.GETVAR:
             if ( state.useLocalVariable( n.getString() ) ){
                 _append( n.getString() , n );
                 break;
             }
         case Token.NAME:
             if ( state.useLocalVariable( n.getString() ) && state.hasSymbol( n.getString() ) )
                 _append( n.getString() , n );
             else
                 _append( "scope.get( \"" + n.getString() + "\" )" , n );
             break;
         case Token.SETVAR:
             final String foo = n.getFirstChild().getString();
             if ( state.useLocalVariable( foo ) ){
                 if ( ! state.hasSymbol( foo ) )
                     throw new RuntimeException( "something is wrong" );
                 _append( foo + " = " , n );
                 _add( n.getFirstChild().getNext() , state );
                 _append( "\n" , n );
             }
             else {
                 _setVar( foo , 
                          n.getFirstChild().getNext() ,
                          state , true );
             }
             break;
 
         case Token.SETNAME:
             _addSet( n , state );
             break;
         case Token.FUNCTION:
             _addFunction( n , state );
             break;
         case Token.BLOCK:
             _addBlock( n , state );
             break;
         case Token.EXPR_VOID:
             _assertOne( n );
             _add( n.getFirstChild() , state );
             _append( ";\n" , n );
             break;
         case Token.RETURN:
             _append( "return " , n );
             if ( n.getFirstChild() != null ){
                 _assertOne( n );
                 _add( n.getFirstChild() , state );
             }
             else {
                 _append( " null " , n );
             }
             _append( ";\n" , n );
             break;
 
         case Token.BITNOT:
             _assertOne( n );
             _append( "JS_bitnot( " , n );
             _add( n.getFirstChild() , state );
             _append( " ) " , n );
             break;
 
 
         case Token.HOOK:
             _append( " ( JS_evalToBool( " , n );
             _add( n.getFirstChild() , state );
             _append( " ) ? ( " , n );
             _add( n.getFirstChild().getNext() , state );
             _append( " ) : ( " , n );
             _add( n.getFirstChild().getNext().getNext() , state );
             _append( " ) ) " , n );
                      
             break;
             
         case Token.NE:
             _append( " ! " , n );
             
         case Token.MUL:
         case Token.DIV:
         case Token.ADD:
         case Token.SUB:
         case Token.EQ:
         case Token.SHEQ:
         case Token.GE:
         case Token.LE:
         case Token.LT:
         case Token.GT:
         case Token.BITOR:
         case Token.BITAND:
         case Token.BITXOR:
         case Token.URSH:
         case Token.RSH:
         case Token.LSH:
         case Token.MOD:
             _append( "JS_" , n );
             String fooooo = _2ThingThings.get( n.getType() );
             if ( fooooo == null )
                 throw new RuntimeException( "noting for : " + n );
             _append( fooooo , n );
             _append( "\n( " , n );
             _add( n.getFirstChild() , state );
             _append( " , " , n );
             _add( n.getFirstChild().getNext() , state );
             _append( " )\n " , n );
             break;
 
         case Token.IFNE:
             _addIFNE( n , state );
             break;
             
         case Token.LOOP:
             _addLoop( n , state );
             break;
 
         case Token.EMPTY:
             if ( n.getFirstChild() != null ){
                 Debug.printTree( n , 0 );
                 throw new RuntimeException( "not really empty" );
             }
             break;
 
         case Token.LABEL:
             _append( n.getString() + ":" , n );
             break;
         case Token.BREAK:
             _append( "break " + n.getString() + ";\n" , n );
             break;
         case Token.CONTINUE:
             _append( "continue " + n.getString() + ";\n" , n );
             break;
             
         case Token.WHILE:
             _append( "while( JS_evalToBool( " , n );
             _add( n.getFirstChild() , state );
             _append( " ) ) " , n );
             _add( n.getFirstChild().getNext() , state );
             break;
         case Token.FOR:
             _addFor( n , state );
             
             break;
             
         case Token.TARGET:
             break;
             
         case Token.NOT:
             _assertOne( n );
             _append( " ! JS_evalToBool( " , n );
             _add( n.getFirstChild() , state );
             _append( " ) " , n );
             break;
 
         case Token.AND:
             _append( " ( " , n );
             Node c = n.getFirstChild();
             while ( c != null ){
                 if ( c != n.getFirstChild() )
                     _append( " && " , n );
 
                 _append( " JS_evalToBool( " , n );
                 _add( c , state );
                 _append( " ) " , n );
                 c = c.getNext();
             }
             _append( " ) " , n );
             break;
             
         case Token.OR:
             Node cc = n.getFirstChild();
 
             if ( cc.getNext() == null )
                 throw new RuntimeException( "what?" );
             if ( cc.getNext().getNext() != null )
                 throw new RuntimeException( "what?" );
             
             _append( "( scope.orSave( " , n );
             _add( cc , state );
             _append( " ) ? scope.getOrSave() : ( " , n );
             _add( cc.getNext() , state );
             _append( " ) ) " , n );
 
             break;
             
         case Token.LOCAL_BLOCK:
             _assertOne( n );
             if ( n.getFirstChild().getType() != Token.TRY )
                 throw new RuntimeException("only know about LOCAL_BLOCK with try" );
             _addTry( n.getFirstChild() , state );
             break;
             
         case Token.THROW:
             _append( "throw new JSException( " , n );
             _add( n.getFirstChild() , state );
             _append( " ); " , n );
             break;
         case Token.INSTANCEOF:
             _append( "JS_instanceof( " , n );
             _add( n.getFirstChild() , state );
             _append( " , " , n );
             _add( n.getFirstChild().getNext() , state );
             _append( " ) " , n );
             if ( n.getFirstChild().getNext().getNext() != null )
                 throw new RuntimeException( "something is wrong" );
             break;
         default:
             Debug.printTree( n , 0 );
             throw new RuntimeException( "can't handle : " + n.getType() + ":" + Token.name( n.getType() ) + ":" + n.getClass().getName() + " line no : " + n.getLineno() );
         }
         
     }
     
     private void _createRef( Node n , State state ){
         
         if ( n.getType() == Token.NAME || n.getType() == Token.GETVAR ){
 
             if ( state.useLocalVariable( n.getString() ) )
                 throw new RuntimeException( "can't create a JSRef from a local variable : " + n.getString() );                
 
             _append( " new JSRef( scope , null , " , n );
             _append( "\"" + n.getString() + "\"" , n );
             _append( " ) " , n );
             return;
         }
         
         if ( n.getType() == Token.GETPROP || 
              n.getType() == Token.GETELEM ){
             _append( " new JSRef( scope , (JSObject)" , n );
             _add( n.getFirstChild() , state );
             _append( " , " , n );
             _add( n.getFirstChild().getNext() , state );
             _append( " ) " , n );
             return;
         }
         
         throw new RuntimeException( "can't handle" );
     }
 
     private void _addTry( Node n , State state ){
         _assertType( n , Token.TRY );
 
         Node mainBlock = n.getFirstChild();
         _assertType( mainBlock , Token.BLOCK );
 
         _append( "try { \n " , n );
         _add( mainBlock , state );
         _append( " \n } \n " , n );
         
         n = mainBlock.getNext();
 
         while ( n != null ){
 
             if ( n.getType() == Token.FINALLY ){
                 _assertType( n.getFirstChild() , Token.BLOCK );
                 _append( "finally { \n" , n );
                 _add( n.getFirstChild() , state );
                 _append( " \n } \n " , n );
                 n = n.getNext();
                 continue;
             }
             
             if ( n.getType() == Token.LOCAL_BLOCK &&
                  n.getFirstChild().getType() == Token.CATCH_SCOPE ){
                 
                 Node c = n.getFirstChild();
                 Node b = c.getNext();
                 
                 _assertType( b , Token.BLOCK );
                 _assertType( b.getFirstChild() , Token.ENTERWITH );
                 _assertType( b.getFirstChild().getNext() , Token.WITH );
                 
                 b = b.getFirstChild().getNext().getFirstChild();
                 _assertType( b , Token.BLOCK );
                                 
                 //Debug.printTree( b , 0 );
                 
                 String jsName = c.getFirstChild().getString();
                 String javaName = "javaEEE" + jsName;
                 _append( " catch ( Throwable " + javaName + " ){ \n " , c );
 
                 _append( " if ( " + javaName + " instanceof JSException ) " , c );
                 _append( " scope.put( \"" + jsName + "\" , ((JSException)" + javaName + ").getObject() , true ); " , c );
                 _append( " else \n " , c );
                 _append( " scope.put( \"" + jsName + "\" , " + javaName + " , true ); " , c );
                 
                 b = b.getFirstChild();
                 while ( b != null ){
                     if ( b.getType() == Token.LEAVEWITH )
                         break;
                     _add( b , state );
                     b = b.getNext();
                 }
                 _append( " } \n " , c );
 
                 n = n.getNext();
                 continue;
             }
             
             if ( n.getType() == Token.GOTO ||
                  n.getType() == Token.TARGET ||
                  n.getType() == Token.JSR ){
                 n = n.getNext();
                 continue;
             }
 
             throw new RuntimeException( "what : " + n.getType() );
         }
     }
     
     private void _addFor( Node n , State state ){
         _assertType( n , Token.FOR );
     
         final int numChildren = countChildren( n );
         if ( numChildren == 4 ){
             _append( "\n for ( " , n );
 
             if ( n.getFirstChild().getType() == Token.BLOCK )
                 _add( n.getFirstChild().getFirstChild() , state );
             else {
                 _add( n.getFirstChild() , state );
                 _append( " ; " , n );
             }
             
             _append( "  \n JS_evalToBool( " , n );
             _add( n.getFirstChild().getNext() , state );
             _append( " ) ; \n" , n );
             _add( n.getFirstChild().getNext().getNext() , state );
             _append( " )\n " , n );
             _add( n.getFirstChild().getNext().getNext().getNext() , state );
         }
         else if ( numChildren == 3 ){
             String name = n.getFirstChild().getString();
             String tempName = name + "TEMP";
 
             _append( "\n for ( String " , n );
             _append( tempName , n );
             _append( " : ((JSObject)" , n );
             _add( n.getFirstChild().getNext() , state );
             _append( " ).keySet() ){\n " , n );
 
             if ( state.useLocalVariable( name ) )
                 _append( name + " = new JSString( " + tempName + ") ; " , n );
             else
                 _append( "scope.put( \"" + name + "\" , new JSString( " + tempName + " ) , true );\n" , n );
             _add( n.getFirstChild().getNext().getNext() , state );
             _append( "\n}\n" , n );
         }
         else {
             throw new RuntimeException( "wtf?" );
         }
     }
 
     private void _addLoop( Node n , State state ){
         _assertType( n , Token.LOOP );
         
         final Node theLoop = n;
         n = n.getFirstChild();
         
         Node nodes[] = null;
         if ( ( nodes = _matches( n , _while1 ) ) != null ){
             Node main = nodes[1];
             Node predicate = nodes[5];
                 
             _append( "while ( JS_evalToBool( " , theLoop );
             _add( predicate.getFirstChild() , state );
             _append( " ) ) " , theLoop );
             _add( main , state );
         }
         else if ( ( nodes = _matches( n , _doWhile1 ) ) != null ){
             Node main = nodes[1];
             Node predicate = nodes[3];
             _assertType( predicate , Token.IFEQ );
             
             _append( "do  \n " , theLoop );
             _add( main , state );
             _append( " \n while ( JS_evalToBool( " , n );
             _add( predicate.getFirstChild() , state );
             _append( " ) );\n " , n );
         }
         else {
             throw new RuntimeException( "what?" );
         }
     }
     
     private void _addIFNE( Node n , State state ){
 
         _assertType( n , Token.IFNE );
 
         final Node.Jump theIf = (Node.Jump)n;
         _assertOne( n ); // this is the predicate
         Node ifBlock = n.getNext();
 
         _append( "if ( JS_evalToBool( " , n );
         _add( n.getFirstChild() , state );
         _append( " ) ){\n" , n );
         _add( ifBlock , state );
         _append( "}\n" , n );
         n = n.getNext().getNext();
         
         if ( n.getType() == Token.TARGET ){
             if ( n.getNext() != null )
                 throw new RuntimeException( "something is wrong" );
             return;
         }
         
         _assertType( n , Token.GOTO );
         _assertType( n.getNext() , Token.TARGET );
         if ( theIf.target.hashCode() != n.getNext().hashCode() )
             throw new RuntimeException( "hashes don't match" );
         
         n = n.getNext().getNext();
 
         _append( " else { " , n );
         _add( n , state );
         _append( " } \n" , n );
         
         _assertType( n.getNext() , Token.TARGET );
         if ( n.getNext().getNext() != null )
             throw new RuntimeException( "something is wrong" );
     }
     
     private void _addFunctionNodes( ScriptOrFnNode sn , State state ){
         for ( int i=0; i<sn.getFunctionCount(); i++ ){
             
             FunctionNode fn = sn.getFunctionNode( i );
             _setLineNumbers( fn , fn );
 
             String name = fn.getFunctionName();
             if ( name.length() == 0 )
                 name = "tempFunc_" + _id + "_" + i + "_" + _methodId++;
             
             state._functionIdToName.put( i , name );
             
             if ( D ){
                 System.out.println( "***************" );
                 System.out.println( i + " : " +  name );
             }
 
             _setVar( name , fn , state );
             _append( "; \n scope.getFunction( \"" + name + "\" ).setName( \"" + name + "\" );\n\n" , fn );
 
         }
     }
     
     private void _addFunction( Node n , State state ){
         if ( ! ( n instanceof FunctionNode ) ){
             _append( getFunc( n , state ) , n );
             return;
         }
 
         FunctionNode fn = (FunctionNode)n;
         _assertOne( n );
 
         state = state.child();
         state._hasLambdaExpressions = fn.getFunctionCount() > 0;
         
 
         boolean hasArguments = false;
         {
             List<Node> toSearch = new ArrayList<Node>();
             toSearch.add( n );
             while ( toSearch.size() > 0 ){
                 Node cur = toSearch.remove( toSearch.size() - 1 );
                 
                 if ( cur.getType() == Token.NAME ||
                      cur.getType() == Token.GETVAR )
                     if ( cur.getString().equals( "arguments" ) )
                         hasArguments = true;
                 
                 if ( cur.getType() == Token.INC )
                     if ( cur.getFirstChild().getType() == Token.GETVAR || 
                          cur.getFirstChild().getType() == Token.NAME )
                         state.addBadLocal( cur.getFirstChild().getString() );
                 
                 if ( cur.getNext() != null )
                     toSearch.add( cur.getNext() );
                 if ( cur.getFirstChild() != null )
                     toSearch.add( cur.getFirstChild() );
                 
             }
             
         }
         
         //state.debug();
 
         _append( "new JSFunctionCalls" + fn.getParamCount() + "( scope , null ){ \n" , n );
         
         String callLine = "public Object call( final Scope passedIn ";
         String varSetup = "";
         
         for ( int i=0; i<fn.getParamCount(); i++ ){
             final String foo = fn.getParamOrVarName( i );
             callLine += " , ";
             callLine += " Object " + foo;
 
             if ( ! state.useLocalVariable( foo ) ){
                 callLine += "INNNNN";
                 varSetup += " \nscope.put(\"" + foo + "\"," + foo + "INNNNN , true  );\n ";
                 if ( hasArguments )
                     varSetup += "arguments.add( " + foo + "INNNNN );\n";
             }
             else {
                 state.addSymbol( foo );
                 if ( hasArguments )
                     varSetup += "arguments.add( " + foo + " );\n";
             }
             
             callLine += " ";
         }
         callLine += " , Object extra[] ){\n" ;
 
         _append( callLine + " final Scope scope = new Scope( \"temp scope\" , _scope , passedIn ); " , n );
         if ( hasArguments ){
             _append( "JSArray arguments = new JSArray();\n" , n );
             _append( "scope.put( \"arguments\" , arguments , true );\n" , n );
         }
         _append(  varSetup , n );
         if ( hasArguments )
             _append( "if ( extra != null ) for ( Object TTTT : extra ) arguments.add( TTTT );\n" , n );
 
         for ( int i=fn.getParamCount(); i<fn.getParamAndVarCount(); i++ ){
             final String foo = fn.getParamOrVarName( i );
             if ( state.useLocalVariable( foo ) ){
                 state.addSymbol( foo );
                 _append( "Object " + foo + " = null;\n" , n );
             }
         }
 
         _addFunctionNodes( fn , state );
 
         _add( n.getFirstChild() , state );
         _append( "}\n" , n );
         
         int myStringId = _strings.size();
         _strings.add( getSource( fn ) );
         _append( "\t public String toString(){ return _strings[" + myStringId + "].toString(); }" , fn );
         
         _append( "}\n" , n );
 
     }
     
     private void _addBlock( Node n , State state ){
         _assertType( n , Token.BLOCK );
 
         if ( n.getFirstChild() == null )
             return;
 
         // this is weird.  look at bracing0.js
         
         boolean bogusBrace = true;
         Node c = n.getFirstChild();
         while ( c != null ){
             if ( c.getType() != Token.EXPR_VOID ){
                 bogusBrace = false;
                 break;
             }
             
             if ( c.getFirstChild().getNext() != null ){
                 bogusBrace = false;
                 break;
             }
 
             if ( c.getFirstChild().getType() != Token.SETVAR ){
                 bogusBrace = false;
                 break;
             }
 
             c = c.getNext();
         }
 
         bogusBrace = bogusBrace ||
             ( n.getFirstChild().getNext() == null &&
               n.getFirstChild().getType() == Token.EXPR_VOID &&
               n.getFirstChild().getFirstChild() == null );
         
         if ( bogusBrace ){
             c = n.getFirstChild();
             while ( c != null ){
                 _add( c  , state );
                 c = c.getNext();
             }
             return;
         }
         
         _append( "{" , n );
         Node child = n.getFirstChild();
         while ( child != null ){
             _add( child , state );
             
             if ( child.getType() == Token.IFNE )
                 break;
             
             child = child.getNext();
         }
         _append( "}" , n );
         
     }
 
     private void _addSet( Node n , State state ){
         _assertType( n , Token.SETNAME );
         Node name = n.getFirstChild();
         _setVar( name.getString() , name.getNext() , state );
     }
     
     private void _addVar( Node n , State state ){
         _assertType( n , Token.VAR );
         _assertOne( n );
         
         Node name = n.getFirstChild();
         _assertOne( name );
         _setVar( name.getString() , name.getFirstChild() , state );
     }
     
     private void _addCall( Node n , State state ){
         _addCall( n , state , false );
     }
 
     private void _addCall( Node n , State state , boolean isClass ){
         Node name = n.getFirstChild();
         
         boolean useThis = name.getType() == Token.GETPROP;
         
         if ( useThis )
             _append( "scope.clearThisNormal( " , n );
 
         String f = getFunc( name , state );
         _append( f + ".call( scope" + ( isClass ? ".newThis( " + f + " )" : "" ) + " " , n );
 
         Node param = name.getNext();
         while ( param != null ){
             _append( " , " , param );
             _add( param , state );
             param = param.getNext();
         }
 
         _append( " ) " , n );
         if ( useThis )
             _append( " ) " , n );
     }
 
     private void _setVar( String name , Node val , State state ){
         _setVar( name , val , state , false );
     }
     
     private void _setVar( String name , Node val , State state , boolean local ){
         if ( state.useLocalVariable( name ) && state.hasSymbol( name ) ){
             _append( name + " = " , val );
             _add( val , state );
             _append( ";\n" , val );
             return;
         }
         _append( "scope.put( \"" + name + "\" , " , val);
         _add( val , state );
         _append( " , " + local + "  ) " , val );
     }
     
     private int countChildren( Node n ){
         int num = 0;
         Node c = n.getFirstChild();
         while ( c != null ){
             num++;
             c = c.getNext();
         }
         return num;
     }
 
     public static void _assertOne( Node n ){
         if ( n.getFirstChild() == null ){
             Debug.printTree( n , 0 );
             throw new RuntimeException( "no child" );
         }
         if ( n.getFirstChild().getNext() != null ){
             Debug.printTree( n , 0 );
             throw new RuntimeException( "more than 1 child" );
         }
     }
 
     public static void _assertType( Node n , int type ){
         if ( type != n.getType() )
             throw new RuntimeException( "wrong type" );
     }
 
     private void _setLineNumbers( Node n , final ScriptOrFnNode sof ){
         final int line = n.getLineno();
 
         if ( line < 0 )
             throw new RuntimeException( "something is wrong" );
         
         List<Node> todo = new LinkedList<Node>();
         
         _nodeToSourceLine.put( n , line );
         _nodeToSOR.put( n , sof );
 
         if ( n.getFirstChild() != null )
             todo.add( n.getFirstChild() );
         if ( n.getNext() != null )
             todo.add( n.getNext() );
         
         while ( todo.size() > 0 ){
             n = todo.remove(0);
             if ( n.getLineno() > 0 ){
                 _setLineNumbers( n , n instanceof ScriptOrFnNode ? (ScriptOrFnNode)n : sof );
                 continue;
             }
 
             _nodeToSourceLine.put( n , line );
             _nodeToSOR.put( n , sof );            
 
             if ( n.getFirstChild() != null )
                 todo.add( n.getFirstChild() );
             if ( n.getNext() != null )
                 todo.add( n.getNext() );
         }
     }
 
     private void _append( String s , Node n ){
         _mainJavaCode.append( s );
         
         int numLines = 0;
         for ( int i=0; i<s.length(); i++ )
             if ( s.charAt( i ) == '\n' )
                 numLines++;
         
         final int start = _currentLineNumber;
         final int end = _currentLineNumber + numLines;
         
         for ( int i=start; i<end; i++ ){
             List<Node> l = _javaCodeToLines.get( i );
             if ( l == null ){
                 l = new ArrayList<Node>();
                 _javaCodeToLines.put( i , l );
             }
             l.add( n );
         }
 
         _currentLineNumber = end;
     }
 
     private String getFunc( Node n , State state ){
         return getFunc( n , state , null );
     }
     private String getFunc( Node n , State state , String asdad ){
         if ( n.getClass().getName().indexOf( "StringNode" ) < 0 ){
             if ( n.getType() == Token.GETPROP ){
                 _append( "scope.getFunctionAndSetThis( " , n );
                 _add( n.getFirstChild() , state );
                 _append( " , " , n );
                 _add( n.getFirstChild().getNext() , state );
                 _append( ".toString() ) " , n );
                 return "";
             }
             _append( "((JSFunction )" , n);
             _add( n , state );
             _append( ")" , n );
             return "";
         }
 
         String name = n.getString();
         if ( name == null || name.length() == 0 ){
             int id = n.getIntProp( Node.FUNCTION_PROP , -1 );
             if ( id == -1 )
                 throw new RuntimeException( "no name or id for this thing" );
             name = state._functionIdToName.get( id );
             if ( name == null || name.length() == 0 )
                 throw new RuntimeException( "no name for this id " );
         }
         
         if ( state.hasSymbol( name ) )
             return "(( JSFunction)" + name + ")";
         
         return "scope.getFunction( \"" + name + "\" )";
     }
 
     public String getClassName(){
         return _className;
     }
 
     String getClassString(){
         StringBuilder buf = new StringBuilder();
         
         buf.append( "package " + _package + ";\n" );
         
         buf.append( "import ed.js.*;\n" );
         buf.append( "import ed.js.func.*;\n" );
         buf.append( "import ed.js.engine.Scope;\n" );
         buf.append( "import ed.js.engine.JSCompiledScript;\n" );
 
         buf.append( "public class " ).append( _className ).append( " extends JSCompiledScript {\n" );
 
         buf.append( "\tpublic Object _call( Scope scope , Object extra[] ){\n" );
 
         buf.append( "\t\t final Scope passedIn = scope; \n" );
        buf.append( "\t\t scope = new Scope( \"temp scope\" , scope ); \n" );
 
         buf.append( "\t\t JSArray arguments = new JSArray(); scope.put( \"arguments\" , arguments , true );\n " );
         buf.append( "\t\t if ( extra != null ) for ( Object TTTT : extra ) arguments.add( TTTT );\n" );
 
         _preMainLines = StringUtil.count( buf.toString() , "\n" );
 
         buf.append( _mainJavaCode );
 
 
         buf.append( "\n\n\t}\n\n" );
         
         buf.append( "\n}\n\n" );
         return buf.toString();
     }
 
     public JSFunction get(){
         if ( _it != null )
             return _it;
         
         try {
             Class c = CompileUtil.compile( _package , getClassName() , getClassString() );
             JSCompiledScript it = (JSCompiledScript)c.newInstance();
             it._convert = this;
             
             it._regex = new JSRegex[ _regex.size() ];
             for ( int i=0; i<_regex.size(); i++ ){
                 Pair<String,String> p = _regex.get( i );
                 JSRegex foo = new JSRegex( p.first , p.second );
                 it._regex[i] = foo;
             }
             
             it._strings = new JSString[ _strings.size() ];
             for ( int i=0; i<_strings.size(); i++ )
                 it._strings[i] = new JSString( _strings.get( i ) );
 
             it.setName( _name );
             
             _it = it;
             return _it;
         }
         catch ( RuntimeException re ){
             re.printStackTrace();
             fixStack( re );
             throw re;
         }
         catch ( Exception e ){
             e.printStackTrace();
             fixStack( e );
             throw new RuntimeException( e );
         }
     }
 
     public void fixStack( Throwable e ){
         boolean removeThings = false;
         
         StackTraceElement stack[] = e.getStackTrace();
         
         boolean changed = false;
         for ( int i=0; i<stack.length; i++ ){
             
             StackTraceElement element = stack[i];
             if ( element == null )
                 continue;
 
             if ( element.toString().contains( ".call(JSFunctionCalls" ) || 
                  element.toString().contains( "ed.js.JSFunctionBase.call(" ) ||
                  element.toString().contains( "ed.js.engine.JSCompiledScript.call" ) ){
                 removeThings = true;
                 changed = true;
                 stack[i] = null;
                 continue;
             }
 
             final String file = getClassName() + ".java";
             
             String es = element.toString();
             
             if ( ! es.contains( file ) )
                 continue;
             
             int line = StringParseUtil.parseInt( es.substring( es.lastIndexOf( ":" ) + 1 ) , -1 ) - _preMainLines;
             List<Node> nodes = _javaCodeToLines.get( line );
             if ( nodes == null )
                 continue;
             
             // the +1 is for the way rhino stuff
             line = _nodeToSourceLine.get( nodes.get(0) ) + 1;
             
             ScriptOrFnNode sof = _nodeToSOR.get( nodes.get(0) );
             String method = "___";
             if ( sof instanceof FunctionNode )
                 method = ((FunctionNode)sof).getFunctionName();
             
             
             stack[i] = new StackTraceElement( _name , method , _name , line );
             changed = true;
         }
 
         if ( removeThings ){
             List<StackTraceElement> lst = new ArrayList<StackTraceElement>();
             for ( StackTraceElement s : stack ){
                 if ( s == null )
                     continue;
                 lst.add( s );
             }
             stack = new StackTraceElement[lst.size()];
             for ( int i=0; i<stack.length; i++ )
                 stack[i] = lst.get(i);
         }
             
         if ( changed )
             e.setStackTrace( stack );
     }
 
     String getSource( FunctionNode fn ){
         final int start = fn.getEncodedSourceStart();
         final int end = fn.getEncodedSourceEnd();
 
         final String encoded =  _encodedSource.substring( start , end );
         final String realSource = Decompiler.decompile( encoded , 0 , new UintMap() );
         
         return realSource;
     }
 
     public boolean hasReturn(){
         return _hasReturn;
     }
     
     //final File _file;
     final String _name;
     final String _source;
     final String _encodedSource;
     final String _className;
     final String _package = "ed.js.gen";
     final int _id = ID++;    
 
     // these 3 variables should only be use by _append
     private int _currentLineNumber = 0;    
     final Map<Integer,List<Node>> _javaCodeToLines = new TreeMap<Integer,List<Node>>();
     final Map<Node,Integer> _nodeToSourceLine = new HashMap<Node,Integer>();
     final Map<Node,ScriptOrFnNode> _nodeToSOR = new HashMap<Node,ScriptOrFnNode>();
     final List<Pair<String,String>> _regex = new ArrayList<Pair<String,String>>();
     final List<String> _strings = new ArrayList<String>();
     int _preMainLines = -1;
     private final StringBuilder _mainJavaCode = new StringBuilder();
 
     private boolean _hasReturn = false;
     private JSFunction _it;
     
     private int _methodId = 0;
     
     private static int ID = 1;
     
     private final static Map<Integer,String> _2ThingThings = new HashMap<Integer,String>();
     static {
         _2ThingThings.put( Token.ADD , "add" );
         _2ThingThings.put( Token.MUL , "mul" );
         _2ThingThings.put( Token.SUB , "sub" );
         _2ThingThings.put( Token.DIV , "div" );
         
         _2ThingThings.put( Token.SHEQ , "sheq" );
         _2ThingThings.put( Token.EQ , "eq" );
         _2ThingThings.put( Token.NE , "eq" );
         
         _2ThingThings.put( Token.GE , "ge" );
         _2ThingThings.put( Token.LE , "le" );
         _2ThingThings.put( Token.LT , "lt" );
         _2ThingThings.put( Token.GT , "gt" );
 
         _2ThingThings.put( Token.BITOR , "bitor" );
         _2ThingThings.put( Token.BITAND , "bitand" );
         _2ThingThings.put( Token.BITXOR , "bitxor" );
 
         _2ThingThings.put( Token.URSH , "ursh" );
         _2ThingThings.put( Token.RSH , "rsh" );
         _2ThingThings.put( Token.LSH , "lsh" );
         _2ThingThings.put( Token.MOD , "mod" );
 
     }
 
     private static final int _while1[] = new int[]{ Token.GOTO , Token.TARGET , 0 , 0 , Token.TARGET , Token.IFEQ , Token.TARGET };
     private static final int _doWhile1[] = new int[]{ Token.TARGET , 0 , Token.TARGET , Token.IFEQ , Token.TARGET };
 
     private static Node[] _matches( Node n , int types[] ){
         Node foo[] = new Node[types.length];
         
         for ( int i=0; i<types.length; i++ ){
             foo[i] = n;
             if ( types[i] > 0 && n.getType() != types[i] ) 
                 return null;
             n = n.getNext();
         }
 
         return n == null ? foo : null;
     }
 }
