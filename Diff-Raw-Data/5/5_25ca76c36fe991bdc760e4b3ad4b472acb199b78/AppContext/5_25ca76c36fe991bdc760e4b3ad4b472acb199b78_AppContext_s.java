 // AppContext.java
 
 /**
 *    Copyright (C) 2008 10gen Inc.
 *
 *    This program is free software: you can redistribute it and/or  modify
 *    it under the terms of the GNU Affero General Public License, version 3,
 *    as published by the Free Software Foundation.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.
 *
 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
 
 package ed.appserver;
 
 import java.io.*;
 import java.util.*;
 
 import ed.appserver.jxp.*;
 import ed.appserver.templates.djang10.*;
 import ed.db.*;
 import ed.log.*;
 import ed.js.*;
 import ed.js.engine.*;
 import ed.js.func.*;
 import ed.lang.*;
 import ed.net.httpserver.*;
 import ed.util.*;
 
 /**
  * This is the container for an instance of a site on a single server.
  * This can be access via __instance__
  *
  * @anonymous name : {local}, isField : {true}, desc : {Refers to the site being run.}, type: {library}
  * @anonymous name : {core}, isField : {true}, desc : {Refers to corejs.} example : {core.core.mail() calls corejs/core/mail.js}, type : {library}
  * @anonymous name : {external}  isField : {true}, desc : {Refers to the external libraries.}, type : {library}
  * @anonymous name : {db}, isField : {true}, desc : {Refers to the database.}, type : {database}
  * @anonymous name : {setDB} desc : {changes <tt>db</tt> to refer to a different database.} param : {type : (string) name : (dbname) desc : (name of the database to which to connect)}
  * @anonymous name : {SYSOUT} desc : {Prints a string.} param : {type : (string) name : (str) desc : (the string to print)}
  * @anonymous name : {log} desc : {Global logger.} param : {type : (string) name : (str) desc : (the string to log)}
  * @expose
  */
 public class AppContext {
 
     /** @unexpose */
     static final boolean DEBUG = AppServer.D;
     /** If these files exist in the directory or parent directories of a file being run, run these files first. Includes _init.js and /~~/core/init.js.  */
 
    static final String INIT_FILES[] = new String[]{ "/~~/core/init.js" , "PREFIX_init.js" };
 
     /** Initializes a new context for a given site directory.
      * @param f the file to run
      */
     public AppContext( File f ){
         this( f.toString() );
     }
 
     /** Initializes a new context for a given site's path.
      * @param root the path to the site from where ed is being run
      */
     public AppContext( String root ){
         this( root , guessNameAndEnv( root )[0] , guessNameAndEnv( root )[1] );
     }
 
     /** Initializes a new context.
      * @param root the path to the site
      * @param name the name of the site
      * @param environment the version of the site
      */
     public AppContext( String root , String name , String environment ){
         this( root , new File( root ) , name , environment );
     }
 
     /** Initializes a new context.
      * @param root the path to the site
      * @param rootFile the directory in which the site resides
      * @param name the name of the site
      * @param environment the version of the site
      */
     public AppContext( String root , File rootFile , String name , String environment ){
 	this( root , rootFile , name , environment , null );
     }
 
     private AppContext( String root , File rootFile , String name , String environment , AppContext nonAdminParent ){
         if ( root == null )
             throw new NullPointerException( "AppContext root can't be null" );
 
         if ( rootFile == null )
             throw new NullPointerException( "AppContext rootFile can't be null" );
 
         if ( name == null )
             name = guessNameAndEnv( root )[0];
 
         if ( name == null )
             throw new NullPointerException( "how could name be null" );
 
         _root = root;
         _rootFile = rootFile;
         _name = name;
         _environment = environment;
 	_nonAdminParent = nonAdminParent;
         _admin = _nonAdminParent != null;
         _codePrefix = _admin ? "/~~/modules/admin/" : "";
 
         _gitBranch = GitUtils.hasGit( _rootFile ) ? GitUtils.getBranchOrTagName( _rootFile ) : null;
 
         _isGrid = name.equals( "grid" );
 
         _scope = new Scope( "AppContext:" + root + ( _admin ? ":admin" : "" ) , _isGrid ? ed.cloud.Cloud.getInstance().getScope() : Scope.newGlobal() , null , Language.JS , _rootFile );
         _scope.setGlobal( true );
 
         _logger = Logger.getLogger( _name + ":" + _environment );
         _usage = new UsageTracker( _name );
 
         _baseScopeInit();
 
         _adminContext = _admin ? null : new AppContext( root , rootFile , name , environment , this );
 
         _logger.info( "Started Context.  root:" + _root + " environment:" + environment + " git branch: " + _gitBranch );
 
     }
 
     /**
      * Creates a copy of this context.
      * @return an identical context
      */
     AppContext newCopy(){
         return new AppContext( _root , _rootFile , _name , _environment , _nonAdminParent );
     }
 
     /**
      *  Initializes the base scope for the application
      */
     private void _baseScopeInit(){
         // --- libraries
         
         if ( _admin )
             _scope.put( "local" , new JSObjectBase() , true );
         else
             _setLocalObject( new JSFileLibrary( _rootFile , "local" , this ) );
 	
 	_loadConfig();
 
         _core = CoreJS.get().getLibrary( getCoreJSVersion() , this , null , true );
         _logger.info( "corejs : " + _core.getRoot() );
         _scope.put( "core" , _core , true );
 
         _external = Module.getModule( "external" ).getLibrary( null , this , null , true );
         _scope.put( "external" , _external , true );
 
         _scope.put( "__instance__" , this , true );
         _scope.lock( "__instance__" );
 
         // --- db
 
         if ( ! _isGrid ){
             _scope.put( "db" , DBProvider.get( this ) , true );
             _scope.put( "setDB" , new JSFunctionCalls1(){
 
                     public Object call( Scope s , Object name , Object extra[] ){
 			if ( name.equals( _lastSetTo ) )
 			    return true;
 
                         DBBase db = (DBBase)s.get( "db" );
                         if ( ! db.allowedToAccess( name.toString() ) )
                             throw new JSException( "you are not allowed to access db [" + name + "]" );
                         
                         s.put( "db" , DBProvider.get( AppContext.this , name.toString() ) , false );
 			_lastSetTo = name.toString();
                         
                         if ( _adminContext != null ){
                             // yes, i do want a new copy so Constructors don't get copied for both
                             _adminContext._scope.put( "db" , DBProvider.get( AppContext.this , name.toString() ) , false );
                         }
                         
                         return true;
                     }
 
 		    String _lastSetTo = null;
 
                 } , true );
         }
 
         // --- output
 
 	_scope.put( "SYSOUT" , new JSFunctionCalls1(){
 		public Object call( Scope s , Object str , Object foo[] ){
 		    System.out.println( AppContext.this._name + " \t " + str );
 		    return true;
 		}
 	    } , true );
         
         _scope.put( "log" , _logger , true );
 
         // --- random?
 
         _scope.put( "openFile" , new JSFunctionCalls1(){
 		public Object call( Scope s , Object name , Object extra[] ){
                     return new JSLocalFile( _rootFile , name.toString() );
                 }
             } , true );
 
         _scope.put( "globalHead" , _globalHead , true  );
 
         Map<String, JSFileLibrary> rootFileMap = new HashMap<String, JSFileLibrary>();
         for(String rootKey : new String[] {"local", "core", "external"}) {
             Object temp = _scope.get(rootKey);
             if(temp instanceof JSFileLibrary)
                 rootFileMap.put(rootKey, (JSFileLibrary)temp);
         }
         
         Djang10Source.install(_scope, rootFileMap, _logger);
         
 	_scope.lock( "user" ); // protection against global user object
 
     }
 
     private void _loadConfig(){
 	try {
 
 	    File f = null;
 	    if ( ! _admin )
 		f = getFileSafe( "_config.js" );
 	    else 
 		f = new File( Module.getModule( "core-modules/admin" ).getRootFile( getVersionForLibrary( "admin" ) ) , "_config.js" );
 	    
 	    _libraryLogger.info( "config file [" + f + "] exists:" + f.exists() );
 	    
 	    if ( f == null || ! f.exists() )
 		return;
 
 	    Convert c = new Convert( f );
 	    c.get().call( _scope );
         }
         catch ( Exception e ){
             throw new RuntimeException( "couldn't load config" , e );
         }
 
     }
 
     /**
      * Get the version of corejs to run for this AppContext.
      * @return the version of corejs as a string. null if should use default
      */
     public String getCoreJSVersion(){
         Object o = _scope.get( "corejsversion" );
         if ( o != null ){
             _logger.error( "you are using corejsversion which is deprecated.  please use version.corejs" );
             return JS.toString( o );
         }
 
         return getVersionForLibrary( "corejs" );
     }
 
     /**
      * Get the version of a library to run.
      * @param name the name of the library to look up
      * @return the version of the library to run as a string.  null if should use default
      */
     public String getVersionForLibrary( String name ){
         return getVersionForLibrary( _scope , name , this );
     }
 
     /**
      * @unexpose
      */
     public static String getVersionForLibrary( Scope s , String name ){
         AppRequest ar = AppRequest.getThreadLocal();
         return getVersionForLibrary( s , name , ar == null ? null : ar.getContext() );
     }
 
 
     /**
      * @unexpose
      */
     public static String getVersionForLibrary( Scope s , String name , AppContext ctxt ){
 	final String version = _getVersionForLibrary( s , name , ctxt );
 	_libraryLogger.info( ctxt + "\t" + name + "\t" + version );
 	return version;
     }
     
     private static String _getVersionForLibrary( Scope s , String name , AppContext ctxt ){
 	final JSObject o1 = ctxt == null ? null : (JSObject)(s.get( "version_" + ctxt.getEnvironmentName()));
         final JSObject o2 = (JSObject)s.get( "version" );
 
 	_libraryLogger.info( ctxt + "\t versionConfig:" + ( o1 != null ) + " config:" + ( o2 != null ) );
 
         String version = _getString( name , o1 , o2 );
         if ( version != null )
             return version;
         
         if ( ctxt == null || ctxt._nonAdminParent == null )
             return null;
         
         return ctxt._nonAdminParent.getVersionForLibrary( name );
     }
 
     private static String _getString( String name , JSObject ... places ){
         for ( JSObject o : places ){
             if ( o == null )
                 continue;
             Object temp = o.get( name );
             if ( temp == null )
                 continue;
             return temp.toString();
         }
        return null;
     }
 
     static String[] guessNameAndEnv( String root ){
 	root = ed.io.FileUtil.clean( root );
         root = root.replaceAll( "\\.+/" , "" );
         String pcs[] = root.split("/+");
         
         if ( pcs.length == 0 )
             throw new RuntimeException( "no root for : " + root );
 
         // handle anything with sites/foo
         for ( int i=0; i<pcs.length-1; i++ )
             if ( pcs[i].equals( "sites" ) ){
                 return new String[]{ pcs[i+1] , i+2 < pcs.length ? pcs[i+2] : null };
             }
         
         final int start = pcs.length-1;
         for ( int i=start; i>0; i-- ){
             String s = pcs[i];
 
             if ( i == start && 
                  ( s.equals("master" ) ||
                    s.equals("test") ||
                    s.equals("www") ||
                    s.equals("staging") ||
                    s.equals("dev" ) ) )
                 continue;
 
             return new String[]{ s , i + 1 < pcs.length ? pcs[i+1] : null };
         }
 
         return new String[]{ pcs[0] , pcs.length > 1 ? pcs[1] : null };
     }
 
     /**
      * Returns the name of the site being run.
      * @return the name of the site
      */
     public String getName(){
         return _name;
     }
 
     /** Get the database being used.
      * @return The database being used
      */
     public DBBase getDB(){
         return (DBBase)_scope.get( "db" );
     }
 
     /** Given the _id of a JSFile, return the file.
      * @param id _id of the file to find
      * @return The file, if found, otherwise null
      */
     JSFile getJSFile( String id ){
 
         if ( id == null )
             return null;
 
         DBCollection f = getDB().getCollection( "_files" );
         return (JSFile)(f.find( new ObjectId( id ) ));
     }
 
     /** Returns (and if necessary, reinitializes) the scope this context is using.
      * @return the scope
      */
     public Scope getScope(){
 	return _scope();
     }
 
     /** Returns a child scope for app requests.
      * @return a child scope
      */
     Scope scopeChild(){
         Scope s = _scope().child( "AppRequest" );
         s.setGlobal( true );
         return s;
     }
 
     void setTLPreferredScope( AppRequest req , Scope s ){
         _scope.setTLPreferred( s );
     }
 
     private synchronized Scope _scope(){
         
         if ( _inScopeInit )
             return _scope;
 
         if ( _getScopeTime() > _lastScopeInitTime )
             _scopeInited = false;
 
         if ( _scopeInited )
             return _scope;
 
         _scopeInited = true;
         _lastScopeInitTime = System.currentTimeMillis();
 
 
         _initScope();
 
         return _scope;
     }
 
     /**
      * @unexpose
      */
     public File getFileSafe( final String uri ){
         try {
             return getFile( uri );
         }
         catch ( FileNotFoundException fnf ){
             return null;
         }
     }
 
     /**
      * @unexpose
      */
     public File getFile( final String uri )
         throws FileNotFoundException {
         File f = _files.get( uri );
 
         if ( f != null )
             return f;
 
         if ( uri.startsWith( "/~~/" ) || uri.startsWith( "~~/" ) )
             f = _core.getFileFromPath( uri.substring( 3 ) );
 	else if ( uri.startsWith( "/admin/" ) )
 	    f = _core.getFileFromPath( "/modules" + uri );
         else if ( uri.startsWith( "/@@/" ) || uri.startsWith( "@@/" ) )
             f = _external.getFileFromPath( uri.substring( 3 ) );
         else if ( _localObject != null && uri.startsWith( "/modules/" ) )
             f = _localObject.getFileFromPath( uri );
         else
             f = new File( _rootFile , uri );
 
         if ( f == null )
             throw new FileNotFoundException( uri );
 
         _files.put( uri , f );
         return f;
     }
 
     /**
      * This causes the AppContext to be started over.
      * All context level variable will be lost.
      * If code is being managed, will cause it to check that its up to date.
      */
     public void reset(){
         _reset = true;
     }
 
     /**
      * Checks if this context has been reset.
      */
     public boolean isReset() {
         return _reset;
     }
 
     /** Returns the path to the directory the appserver is running. (For example, site/version.)
      * @return the path
      */
     public String getRoot(){
         return _root;
     }
 
     /**
      * Creates an new request for the app server from an HTTP request.
      * @param request HTTP request to create
      * @return the request
      */
     AppRequest createRequest( HttpRequest request ){
         return createRequest( request , request.getHost() , request.getURI() );
     }
 
     /**
      * Creates an new request for the app server from an HTTP request.
      * @param request HTTP request to create
      * @param uri the URI requested
      * @return the request
      */
     AppRequest createRequest( HttpRequest request , String host , String uri ){
         _numRequests++;
         
         if ( AppRequest.isAdmin( request ) )
             return new AppRequest( _adminContext , request , host , uri );
 
 
         return new AppRequest( this , request , host , uri );
     }
 
     /**
      *  Tries to find the given file, assuming that it's missing the ".jxp" extension
      *
      * @param f  File to check
      * @return same file if not found to be missing the .jxp, or a new File w/ the .jxp appended
      */
     File tryNoJXP( File f ){
         if ( f.exists() )
             return f;
 
         if ( f.getName().indexOf( "." ) >= 0 )
             return f;
 
         File temp = new File( f.toString() + ".jxp" );
         return temp.exists() ? temp : f;
     }
 
     File tryOtherExtensions( File f ){
         if ( f.exists() )
             return f;
         
         if ( f.getName().indexOf( "." ) >= 0 )
             return f;
 
         for ( int i=0; i<JSFileLibrary._srcExtensions.length; i++ ){
             File temp = new File( f.toString() + JSFileLibrary._srcExtensions[i] );
             if ( temp.exists() )
                 return temp;
         }
 
         return f;
     }
 
     /**
      *    Maps a servlet-like URI to a jxp file.
      *
      * @example   /wiki/geir  ->  maps to wiki.jxp if exists
      *
      * @param f File to check
      * @return new File with <root>.jxp if exists, orig file if not
      */
     File tryServlet( File f ){
         if ( f.exists() )
             return f;
 
         String uri = f.toString();
 
         if ( uri.startsWith( _rootFile.toString() ) )
             uri = uri.substring( _rootFile.toString().length() );
 
         if ( _core != null && uri.startsWith( _core._base.toString() ) )
             uri = "/~~" + uri.substring( _core._base.toString().length() );
 
         while ( uri.startsWith( "/" ) )
             uri = uri.substring( 1 );
 
         int start = 0;
         while ( true ){
 
             int idx = uri.indexOf( "/" , start );
             if ( idx < 0 )
                 break;
             String foo = uri.substring( 0 , idx );
 
             File temp = getFileSafe( foo + ".jxp" );
 
             if ( temp != null && temp.exists() )
                 f = temp;
 
             start = idx + 1;
         }
 
         return f;
     }
 
     /**
      *   Returns the index.jxp for the File argument if it's an existing directory,
      *   and the index.jxp file exists
      *
      * @param f  directory to check
      * @return new File for index.jxp in that directory, or same file object if not
      */
     File tryIndex( File f ){
 
         if ( ! ( f.isDirectory() && f.exists() ) )
             return f;
         
         for ( int i=0; i<JSFileLibrary._srcExtensions.length; i++ ){
             File temp = new File( f , "index" + JSFileLibrary._srcExtensions[i] );
             if ( temp.exists() )
                 return temp;
         }
 
         return f;
     }
 
 
     JxpSource getSource( File f )
         throws IOException {
 
         if ( DEBUG ) System.err.println( "getSource\n\t " + f );
 
         File temp = _findFile(f);
 
         if ( DEBUG ) System.err.println( "\t " + temp );
 
         if (!temp.exists())
             return null;
 
         /*
          *   if it's a directory (and we know we can't find the index file)
          *  TODO : at some point, do something where we return an index for the dir?
          */
         if ( temp.isDirectory() )
             return null;
 
         /*
          *   if we at init time, save it as an initializaiton file
          */
         loadedFile(temp);
 
 
         /*
          *   Ensure that this is w/in the right tree for the context
          */
         if ( _localObject != null && _localObject.isIn(temp) )
             return _localObject.getSource(temp);
 
         /*
          *  if not, is it core?
          */
         if ( _core.isIn(temp) )
             return _core.getSource(temp);
 
         throw new RuntimeException( "what?  can't find:" + f );
     }
 
     /**
      *  Finds the appropriate file for the given path.
      *
      *  We have a hierarchy of attempts as we try to find a file :
      *
      *  1) first, see if it exists as is, or if it's really a .jxp w/o the extension
      *  2) next, see if it can be deconstructed as a servlet such that /foo/bar maps to /foo.jxp
      *  3) See if we can find the index file for it if a directory
      */
     File _findFile(File f) {
 
         File temp;
 
         if ((temp = tryNoJXP(f)) != f) {
             return temp;
         }
 
         if ((temp = tryOtherExtensions(f)) != f) {
             return temp;
         }
         
         if ((temp = tryServlet(f)) != f) {
             return temp;
         }
 
         if ((temp = tryIndex(f)) != f) {
             return temp;
         }
 
 
         return f;
     }
 
     public void loadedFile( File f ){
         if ( _inScopeInit )
             _initFlies.add( f );
     }
 
     JxpServlet getServlet( File f )
         throws IOException {
         JxpSource source = getSource( f );
         if ( source == null )
             return null;
         return source.getServlet( this );
     }
 
     private void _initScope(){
         if ( _inScopeInit )
             return;
 
         final Scope saveTLPref = _scope.getTLPreferred();
         _scope.setTLPreferred( null );
 
         final Scope saveTL = Scope.getThreadLocal();
         _scope.makeThreadLocal();
 
         _inScopeInit = true;
 
         try {
             _runInitFiles( INIT_FILES );
             
 	    if ( _adminContext != null ){
 		_adminContext._scope.set( "siteScope" , _scope );
                 _adminContext._setLocalObject( _localObject );
             }
 	    
             _lastScopeInitTime = _getScopeTime();
         }
         catch ( RuntimeException re ){
             _scopeInited = false;
             throw re;
         }
         catch ( Exception e ){
             _scopeInited = false;
             throw new RuntimeException( e );
         }
         finally {
             _inScopeInit = false;
             _scope.setTLPreferred( saveTLPref );
 
             if ( saveTL != null )
                 saveTL.makeThreadLocal();
         }
 
     }
 
     private void _runInitFiles( String[] files )
         throws IOException {
 
         if ( files == null )
             return;
 
         for ( int i=0; i<files.length; i++ )
            _runInitFile( getFile( files[i].replaceAll( "PREFIX" , _codePrefix ) ) );
     }
 
     private void _runInitFile( File f )
         throws IOException {
         if ( f == null )
             return;
         
         if ( ! f.exists() )
             return;
         
         _initFlies.add( f );
         JxpSource s = getSource( f );
         JSFunction func = s.getFunction();
         func.call( _scope );
     }
 
     long _getScopeTime(){
         long last = 0;
         for ( File f : _initFlies )
             if ( f.exists() )
                 last = Math.max( last , f.lastModified() );
         return last;
     }
 
 
     /**
      * Convert this AppContext to a string by returning the name of
      * the directory it's running in.
      * @return the filename of its root directory
      */
     public String toString(){
         return _rootFile.toString();
     }
 
     public String debugInfo(){
 	return _rootFile + " admin:" + _admin;
     }
 
     public void fix( Throwable t ){
         StackTraceHolder.getInstance().fix( t );
     }
 
     /**
      * Get a "global" head array. This array contains HTML that will
      * be inserted into the head of every request served by this app
      * context. It's analagous to the <tt>head</tt> array, but
      * persistent.
      * @return a mutable array
      */
     public JSArray getGlobalHead(){
         return _globalHead;
     }
 
     /**
      * Gets the date of creation for this app context.
      * @return the creation date as a JS Date.
      */
     public JSDate getWhenCreated(){
         return _created;
     }
 
     /**
      * Gets the number of requests served by this app context.
      * @return the number of requests served
      */
     public int getNumRequests(){
         return _numRequests;
     }
 
     /**
      * Get the name of the git branch we think we're running.
      * @return the name of the git branch, as a string
      */
     public String getGitBranch(){
         return _gitBranch;
     }
 
     /**
      * Update the git branch that we're running and return it.
      * @return the name of the git branch, or null if there isn't any
      */
     public String getCurrentGitBranch(){
         if ( _gitBranch == null )
             return null;
 
         if ( _gitFile == null )
             _gitFile = new File( _rootFile , ".git/HEAD" );
 
         if ( ! _gitFile.exists() )
             throw new RuntimeException( "this should be impossible" );
 
         if ( _lastScopeInitTime < _gitFile.lastModified() )
             _gitBranch = GitUtils.getBranchOrTagName( _rootFile );
 
         return _gitBranch;
     }
 
     /**
      * Get the environment in which this site is running
      * @return the environment name as a string
      */
     public String getEnvironmentName(){
         return _environment;
     }
 
 
     /**
      * updates the context to the correct branch based on environment
      * and to the latest version of the code
      * if name or environemnt is missing, does nothing
      */
     public String updateCode(){
 
         if ( ! GitUtils.isSourceDirectory( _rootFile ) )
             throw new RuntimeException( _rootFile + " is not a git repo" );
 
         _logger.info( "going to update code" );
         GitUtils.fullUpdate( _rootFile );
 
         if ( _name == null || _environment == null )
             return getCurrentGitBranch();
 
         JSObject env = AppContextHolder.getEnvironmentFromCloud( _name , _environment );
         if ( env == null )
             return null;
 
 
         String branch = env.get( "branch" ).toString() ;
         _logger.info( "updating to [" + branch + "]"  );
         AppContextHolder._checkout( _rootFile , branch );
 
         return getCurrentGitBranch();
     }
 
     private void _setLocalObject( JSFileLibrary local ){
         _localObject = local;
         _scope.put( "local" , _localObject , true );
         _scope.put( "jxp" , _localObject , true );
 	_scope.warn( "jxp" );
     }
 
     public static AppContext findThreadLocal(){
         AppRequest req = AppRequest.getThreadLocal();
         if ( req != null )
             return req._context;
 
         Scope s = Scope.getThreadLocal();
         if ( s != null ){
             Object foo = s.get( "__instance__" );
             if ( foo instanceof AppContext )
                 return (AppContext)foo;
         }
 
         return null;
     }
 
     final String _name;
     final String _root;
     final File _rootFile;
 
     private String _gitBranch;
     final String _environment;
     final boolean _admin;
 
     final AppContext _adminContext;
     final String _codePrefix;
     
     final AppContext _nonAdminParent;
 
     private JSFileLibrary _localObject;
     private JSFileLibrary _core;
     private JSFileLibrary _external;
 
     final Logger _logger;
     final Scope _scope;
     final UsageTracker _usage;
 
     final JSArray _globalHead = new JSArray();
 
     private final Map<String,File> _files = Collections.synchronizedMap( new HashMap<String,File>() );
     private final Set<File> _initFlies = new HashSet<File>();
 
     boolean _scopeInited = false;
     boolean _inScopeInit = false;
     long _lastScopeInitTime = 0;
 
     final boolean _isGrid;
 
     boolean _reset = false;
     int _numRequests = 0;
     final JSDate _created = new JSDate();
 
 
     private File _gitFile = null;
     private long _lastGitCheckTime = 0;
 
     private static Logger _libraryLogger = Logger.getLogger( "library.load" );
 }
