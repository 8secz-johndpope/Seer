 package lua;
 import java.io.ByteArrayOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 
 import junit.framework.TestCase;
 import lua.addon.luacompat.LuaCompat;
 import lua.addon.luajava.LuaJava;
 import lua.debug.DebugStackState;
 import lua.io.Closure;
 import lua.io.LoadState;
 import lua.io.Proto;
 import lua.value.LValue;
 
 
 public class LuaJTest extends TestCase {
 
 	public void testTest1() throws IOException, InterruptedException {
 		runTest( "test1" );
 	}
 
 	public void testTest2() throws IOException, InterruptedException {
 		runTest( "test2" );
 	}
 
 	public void testTest3() throws IOException, InterruptedException {
 		runTest( "test3" );
 	}
 
 	public void testTest4() throws IOException, InterruptedException {
 		runTest( "test4" );
 	}
 
 	public void testTest5() throws IOException, InterruptedException {
 		runTest( "test5" );
 	}
 
 	public void testTest6() throws IOException, InterruptedException {
 		runTest( "test6" );
 	}
 
 	public void testTest7() throws IOException, InterruptedException {
 		runTest( "test7" );
 	}
 	
 	public void testAutoload() throws IOException, InterruptedException {
 		runTest( "autoload" );
 	}
 
 	public void testBoolean() throws IOException, InterruptedException {
 		runTest( "boolean" );
 	}
 
 	public void testCalls() throws IOException, InterruptedException {
 		runTest( "calls" );
 	}
 
 	public void testCoercions() throws IOException, InterruptedException {
 		runTest( "coercions" );
 	}
 	
 	public void testCompare() throws IOException, InterruptedException {
 		runTest( "compare" );
 	}
 
 	public void testMathLib() throws IOException, InterruptedException {
 		runTest( "mathlib" );
 	}
 
 	public void testMetatables() throws IOException, InterruptedException {
 		runTest( "metatables" );
 	}
 
 	public void testSelect() throws IOException, InterruptedException {
 		runTest( "select" );
 	}
 
 	public void testSetlist() throws IOException, InterruptedException {
 		runTest( "setlist" );
 	}
 	
 	public void testStrLib() throws IOException, InterruptedException {
 		runTest( "strlib" );
 	}
 	
 	public void testType() throws IOException, InterruptedException {
 		runTest( "type" );
 	}
 	
 	public void testUpvalues() throws IOException, InterruptedException {
 		runTest( "upvalues" );
 	}
 	
 	public void testUpvalues2() throws IOException, InterruptedException {
 		runTest( "upvalues2" );
 	}
 
 	private void runTest( String testName ) throws IOException, InterruptedException {
 		
 		// Reset the _G table just in case some test mucks with it
 		GlobalState.resetGlobals();
 		
 		// add LuaJava bindings
 		LuaJava.install();
 
 		// add LuaCompat bindings
 		LuaCompat.install();
 		
 		// new lua state 
 		StackState state = new DebugStackState();
 		
 		// load the file
 		Proto p = loadScriptResource( state, testName );
 		
 		// Replace System.out with a ByteArrayOutputStream
 		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
 		Builtin.redirectOutput( outputStream );
 		try {
 			// create closure and execute
 			Closure c = new Closure( state, p );
 			state.doCall(c, new LValue[0]);
 			
 			final String actualOutput = new String( outputStream.toByteArray() );
 			final String expectedOutput = getExpectedOutput( testName );
 			
 			assertEquals( expectedOutput, actualOutput );
 		} finally {
 			Builtin.restoreStandardOutput();
 			outputStream.close();
 		}
 	}
 	
 	private Proto loadScriptResource( StackState state, String name ) throws IOException {
 		InputStream script = getClass().getResourceAsStream( "/"+name+".luac" );
 		if ( script == null ) {
 			script = getClass().getResourceAsStream( "/"+name+".lua" );
 			if ( script == null ) {
 				fail( "Could not load script for test case: "+name );
 			}
 		}
 		
 		try {
 			return LoadState.undump(state, script, name);
 		} finally {
 			script.close();
 		}
 	}
 	
 	private String getExpectedOutput( final String testName ) throws IOException, InterruptedException {
 		String expectedOutputName = "/" + testName + "-expected.out";
 		InputStream is = getClass().getResourceAsStream( expectedOutputName );
 		if ( is != null ) {
 			try {
 				return readString( is );
 			} finally {
 				is.close();
 			}
 		} else {
 			InputStream script;
 //			script = getClass().getResourceAsStream( "/" + testName + ".luac" );
 //			if ( script == null ) {
 				script = getClass().getResourceAsStream( "/" + testName + ".lua" );
 				if ( script == null ) {
 					fail( "Could not find script for test case: "+testName );
 				}
 //			}
 			try {
 				return collectProcessOutput( new String[] { "lua", "-" }, script );
 			} finally {
 				script.close();
 			}
 		}
 	}
 	
 	private String collectProcessOutput( String[] cmd, final InputStream input ) throws IOException, InterruptedException {
 		Runtime r = Runtime.getRuntime();
 		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
 		final Process p = r.exec( cmd );
 		try {
 			// start a thread to write the given input to the subprocess.
 			Thread inputCopier = (new Thread() {
 				public void run() {
 					try {
 						OutputStream processStdIn = p.getOutputStream();
 						try {
 							copy( input, processStdIn );
 						} finally {
 							processStdIn.close();
 						}
 					} catch ( IOException e ) {
 						e.printStackTrace();
 					}
 				}
 			});
 			inputCopier.start();
 			
 			// start another thread to read output from the subprocess.
 			Thread outputCopier = (new Thread() {
 				public void run() {
 					try {
 						InputStream processStdOut = p.getInputStream();
 						try {
 							copy( processStdOut, baos );
 						} finally {
 							processStdOut.close();
 						}
 					} catch ( IOException ioe ) {
 						ioe.printStackTrace();
 					}
 				}
 			});
 			outputCopier.start();
 			
 			p.waitFor();
 			inputCopier.join();
 			outputCopier.join();
 			
 			return new String( baos.toByteArray() );
 			
 		} finally {
 			p.destroy();
 		}
 	}
 	
 	private String readString( InputStream is ) throws IOException {
 		ByteArrayOutputStream baos = new ByteArrayOutputStream();
 		copy( is, baos );
 		return new String( baos.toByteArray() );
 	}
 	
 	private void copy( InputStream is, OutputStream os ) throws IOException {
 		byte[] buf = new byte[ 1024 ];
 		int r;
 		while ( ( r = is.read( buf ) ) >= 0 ) {
 			os.write( buf, 0, r );
 		}
 	}
 
 }
