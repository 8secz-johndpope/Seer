 // AppContext.java
 
 package ed.appserver;
 
 import java.io.*;
 import java.util.*;
 
 import ed.db.*;
 import ed.js.*;
 import ed.js.func.*;
 import ed.js.engine.*;
 import ed.net.httpserver.*;
 import ed.appserver.jxp.*;
 
 public class AppContext {
 
     static final boolean DEBUG = false;
     static final String INIT_FILES[] = new String[]{ "_init.js" , "/~~/core/init.js" };
 
     public AppContext( File f ){
         this( f.toString() );
     }
 
     public AppContext( String root ){
         this( root , guessName( root ) );
     }
 
     public AppContext( String root , String name ){
         _name = name;
         _root = root;
         _rootFile = new File( _root );
 
         _scope = new Scope( "AppContext:" + root , Scope.GLOBAL , null , _rootFile );
         _scope.setGlobal( true );
         
         _jxpObject = new JSFileLibrary( _rootFile , "jxp" , this );
         _scope.put( "jxp" , _jxpObject , true );
         
         _scope.put( "db" , DBProvider.get( _name ) , true );
 	_scope.put( "setDB" , new JSFunctionCalls1(){
 		public Object call( Scope s , Object name , Object extra[] ){
 		    System.out.println( "name:" + name + " _scope:" + _scope );
 		    s.put( "db" , DBProvider.get( name.toString() ) , false );
 		    return true;
 		}
 	    } , true );
         
         _scope.put( "openFile" , new JSFunctionCalls1(){
 		public Object call( Scope s , Object name , Object extra[] ){
                     return new JSLocalFile( _rootFile , name.toString() );
                 }
             } , true );
 
         _core = new JSFileLibrary( new File( "/data/corejs" ) ,  "core" , this );
         _scope.put( "core" , _core , true );
         
         _scope.put( "globalHead" , _globalHead , true  );
 
 	_scope.lock( "user" );
     }
 
     private static String guessName( String root ){
         String pcs[] = root.split("/");
 
         if ( pcs.length == 0 )
             throw new RuntimeException( "no root for : " + root );
         
         for ( int i=pcs.length-1; i>0; i-- ){
             String s = pcs[i];
 
             if ( s.equals("master" ) || 
                  s.equals("test") || 
                  s.equals("staging") || 
                  s.equals("dev" ) )
                 continue;
             
             return s;
         }
         
         return pcs[0];
     }
     
     public String getName(){
         return _name;
     }
 
     JSFile getJSFile( String id ){
 
         if ( id == null )
             return null;
         
         DBBase db = (DBBase)_scope.get( "db" );
         DBCollection f = db.getCollection( "_files" );
         return (JSFile)(f.find( new ObjectId( id ) ));
     }
 
     public Scope getScope(){
 	return _scope();
     }
 
     Scope scopeChild(){
         Scope s = _scope().child( "AppRequest" );
         s.setGlobal( true );
         return s;
     }
     
     private synchronized Scope _scope(){
         
         if ( _getScopeTime() > _lastScopeInitTime )
             _scopeInited = false;
 
         if ( _scopeInited )
             return _scope;
         
         _scopeInited = true;
         _lastScopeInitTime = System.currentTimeMillis();
         
         _initScope();
 
         return _scope;
     }
     
     public File getFile( final String uri ){
         File f = _files.get( uri );
         
         if ( f != null )
             return f;
         
        if ( uri.startsWith( "/~~/" ) || uri.startsWith( "~~/" ) )
             f = new File( _core._base , uri.substring( 3 ) );
 	else if ( uri.startsWith( "/%7E%7E/" ) )
 	    f = new File( _core._base , uri.substring( 7 ) );
        else if ( uri.startsWith( "/@@/" ) || uri.startsWith( "@@/" ) )
             f = new File( "/data/external/" , uri.substring( 3 ) );
         else if ( uri.startsWith( "/%40%40/" ) )
             f = new File( "/data/external/" , uri.substring( 7 ) );
         else
             f = new File( _rootFile , uri );
 
         _files.put( uri , f );
         return f;
     }
     
     public void resetScope(){
         _scopeInited = false;
         _scope.reset();
     }
     
     public String getRoot(){
         return _root;
     }
 
     public AppRequest createRequest( HttpRequest request ){
         return createRequest( request , request.getURI() );
     }
     
     public AppRequest createRequest( HttpRequest request , String uri ){
         return new AppRequest( this , request , uri );
     }
 
     File tryNoJXP( File f ){
         if ( f.exists() )
             return f;
 
         if ( f.getName().indexOf( "." ) >= 0 )
             return f;
         
         File temp = new File( f.toString() + ".jxp" );
         return temp.exists() ? temp : f;
     }
 
     File tryServlet( File f ){
         if ( f.exists() )
             return f;
         
         String uri = f.toString();
 	
 	if ( uri.startsWith( _rootFile.toString() ) )
 	    uri = uri.substring( _rootFile.toString().length() );
 	
 	if ( uri.startsWith( _core._base.toString() ) )
 	    uri = "/~~" + uri.substring( _core._base.toString().length() );
 
         
         while ( uri.startsWith( "/" ) )
             uri = uri.substring( 1 );
 
         int start = 0;
         while ( true ){
             
             int idx = uri.indexOf( "/" , start );
             if ( idx < 0 )
                 break; 
             String foo = uri.substring( 0 , idx );
 
             File temp = getFile( foo + ".jxp" );
 
             if ( temp.exists() )
                 f = temp;
             
             start = idx + 1;
         }
 
         return f;
     }
 
     File tryIndex( File f ){
 
         if ( ! ( f.isDirectory() && f.exists() ) )
             return f;
         
         File temp = new File( f , "index.jxp" );
         if ( temp.exists() )
             return temp;
         
         return f;
     }
     
     public JxpSource getSource( File f )
         throws IOException {
     
         if ( DEBUG ) System.err.println( "getSource\n\t " + f );
         f = tryNoJXP( f );
         f = tryServlet( f );
         f = tryIndex( f );
         if ( DEBUG ) System.err.println( "\t " + f );
         
         if ( ! f.exists() )
             return null;
 
         loadedFile( f );
 
 
         if ( _jxpObject.isIn( f ) )
             return _jxpObject.getSource( f );
         
         if ( _core.isIn( f ) )
             return _core.getSource( f );
         
         throw new RuntimeException( "what?  can't find:" + f );
     }
 
     public void loadedFile( File f ){
         if ( _inScopeInit )
             _initFlies.add( f );
     }
 
     public JxpServlet getServlet( File f )
         throws IOException {
         JxpSource source = getSource( f );
         if ( source == null )
             return null;
         return source.getServlet( this );
     }
 
     private void _initScope(){
         _inScopeInit = true;
         
         try {
             for ( int i=0; i<INIT_FILES.length; i++ ){
                 File f = getFile( INIT_FILES[i] );
                 if ( f.exists() ){
                     _initFlies.add( f );
                     JxpSource s = getSource( f );
                     JSFunction func = s.getFunction();
                     func.call( _scope );
                 }
             }
 
             _lastScopeInitTime = _getScopeTime();
         }
         catch ( RuntimeException re ){
             throw re;
         }
         catch ( Exception e ){
             throw new RuntimeException( e );
         }
         finally {
             _inScopeInit = false;
         }
         
     }
     
     long _getScopeTime(){
         long last = 0;
         for ( File f : _initFlies )
             if ( f.exists() )
                 last = Math.max( last , f.lastModified() );
         return last;
     }
     
 
     public String toString(){
         return _rootFile.toString();
     }
 
     public void fix( Throwable t ){
         _jxpObject.fix( t );
         _core.fix( t );
     }
 
     public JSArray getGlobalHead(){
         return _globalHead;
     }
 
     final String _name;
     final String _root;
     final File _rootFile;
 
     final JSFileLibrary _jxpObject;
     final JSFileLibrary _core;
 
     final Scope _scope;
     
     final JSArray _globalHead = new JSArray();
     
     private final Map<String,File> _files = new HashMap<String,File>();
     private final Set<File> _initFlies = new HashSet<File>();
 
     boolean _scopeInited = false;
     boolean _inScopeInit = false;
     long _lastScopeInitTime = 0;
 }
