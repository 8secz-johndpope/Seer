 package firm;
 
 import com.sun.jna.Callback;
 import com.sun.jna.Library;
 import com.sun.jna.Native;
 import com.sun.jna.Platform;
 import com.sun.jna.Pointer;
 
 import firm.bindings.binding_firm_common;
 import firm.bindings.binding_irflag;
 import firm.bindings.binding_libc;
import firm.bindings.binding_typerep.ir_visibility;
 
 public final class Firm {
 
 	public interface binding_callback extends Library {
 
 		interface FirmCallback extends Callback {
 			void callback(String expr, String file, int line);
 		}
 
 		void firm_set_assert_callback(FirmCallback handler);
 	}
 
 	public static interface binding_compilerlib extends Library {
 		interface NameMangleCallback extends Callback {
 			Pointer callback(Pointer ident, Pointer type);
 		}
 		void set_compilerlib_name_mangle(NameMangleCallback handle);
 	}
 
 	public static int getMajorVersion() {
 		return binding_firm_common.ir_get_version_major();
 	}
 
 	public static int getMinorVersion() {
 		return binding_firm_common.ir_get_version_minor();
 	}
 
 	public static String getBuild() {
 		return binding_firm_common.ir_get_version_build();
 	}
 
 	public static String getRevision() {
 		return binding_firm_common.ir_get_version_revision();
 	}
 
 	private static binding_callback binding_cb = null;
 	private static binding_compilerlib binding_clib = null;
 
 	private static final binding_callback.FirmCallback handler = new binding_callback.FirmCallback() {
 		@Override
 		public void callback(String expr, String file, int line) {
 			// throw new FirmAssertionException(expr, file, line);
 			System.err.printf("Failed assert('%1s') in %2s:%3d\n", expr, file,
 					line);
 		}
 	};
 
 	private static final binding_compilerlib.NameMangleCallback addUnderscore = new binding_compilerlib.NameMangleCallback() {
 		@Override
 		public Pointer callback(Pointer ident, Pointer type) {
 			final Ident baseIdent = new Ident(ident);
 			final Ident mangledIdent = baseIdent.mangle("_", "");
 			return mangledIdent.ptr;
 		}
 	};
 
 	public static binding_libc.SigHandler sigHandler = new binding_libc.SigHandler() {
 		@Override
 		public void callback(int arg) {
 			throw new RuntimeException("Prog Aborted");
 		}
 	};
 
 	private static void setupUnderscorePrefixCompilerlibCallback() {
 		if (binding_clib == null) {
 			binding_clib = (binding_compilerlib) Native.loadLibrary("firm",
 					binding_compilerlib.class);
 		}
 		binding_clib.set_compilerlib_name_mangle(addUnderscore);
 	}
 
 	/**
 	 * Initializes the firm library. Must be called before using any operations
 	 * of the firm library (except querying the version numbers) Must not be
 	 * called more than once unless there was an finish() call.
 	 */
 	public static void init() {
 		/* hack to catch asserts... */
 		if (binding_cb == null) {
 			binding_cb = (binding_callback) Native.loadLibrary("firm",
 					binding_callback.class);
 		}
 
 		if (binding_cb != null) {
 			try {
 				binding_cb.firm_set_assert_callback(handler);
 				System.out
 						.println("Callback activated: firm_set_assert_callback found.");
 			} catch (UnsatisfiedLinkError e) {
 				/* not critical */
 			}
 		}
 
 		// catch abort signal
 		binding_libc.signal(/* SIGABRT */6, sigHandler);
 
 		binding_firm_common.ir_init();
 
 		/* disable automatic optimisations */
 		binding_irflag.set_optimize(0);
 
 		/* adapt backend to architecture */
 		if (Platform.isMac()) {
 			Backend.option("ia32-gasmode=macho");
 			Backend.option("ia32-stackalign=4");
 			Backend.option("pic");
 			setupUnderscorePrefixCompilerlibCallback();
 		} else if (Platform.isWindows()) {
 			Backend.option("ia32-gasmode=mingw");
 			setupUnderscorePrefixCompilerlibCallback();
 		} else {
 			Backend.option("ia32-gasmode=elf");
 		}
 	}
 
 	public static void enableOptimisations() {
 		binding_irflag.set_optimize(1);
 	}
 
 	/**
 	 * frees internal datastructures of firm. After this call no firm operaion
 	 * must be performed anymore.
 	 */
 	public static void finish() {
 		binding_firm_common.ir_finish();
 	}
 
 	private Firm() {
 	}
 }
