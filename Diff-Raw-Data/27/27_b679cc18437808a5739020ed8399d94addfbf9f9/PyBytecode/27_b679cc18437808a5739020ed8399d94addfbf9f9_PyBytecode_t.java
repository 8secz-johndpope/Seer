 package org.python.core;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 
 public class PyBytecode extends PyBaseCode {
 
     // for debugging
     private int count = 0; // total number of opcodes run so far in this code obj
     private int maxCount = -1; // if -1, no cap on number of opcodes than can be run
     public static boolean defaultDebug = false;
     private static PyObject dis;
 
     private static synchronized PyObject get_dis() {
         if (dis == null) {
             dis = __builtin__.__import__("dis");
         }
         return dis;
     }
     private static PyObject opname;
 
     private static synchronized PyObject get_opname() {
         if (opname == null) {
             opname = get_dis().__getattr__("opname");
         }
         return opname;
     }
     private boolean debug;
 
     public static void _allDebug(boolean setting) {
         defaultDebug = setting;
     }
 
     public PyObject _debug(int maxCount) {
         debug = maxCount > 0;
         this.maxCount = maxCount;
         return Py.None;
     }
 
     // end debugging
     public final static int CO_MAXBLOCKS = 20; // same as in CPython
     public final char[] co_code; // widened to char to avoid signed byte issues
     public final PyObject[] co_consts;
     public final String[] co_names;
     public final int co_stacksize; // XXX - use to convert PyStack to use PyObject[] instead of ArrayList<PyObject>
     public final String co_lnotab; // ignored for now
     private final static int CALL_FLAG_VAR = 1;
     private final static int CALL_FLAG_KW = 2;
 
     // follows new.code's interface
     public PyBytecode(int argcount, int nlocals, int stacksize, int flags,
             String codestring, PyObject[] constants, String[] names, String varnames[],
             String filename, String name, int firstlineno, String lnotab) {
         this(argcount, nlocals, stacksize, flags, codestring,
                 constants, names, varnames, filename, name, firstlineno, lnotab,
                 null, null);
     }
 
     public PyBytecode(int argcount, int nlocals, int stacksize, int flags,
             String codestring, PyObject[] constants, String[] names, String varnames[],
             String filename, String name, int firstlineno, String lnotab,
             String[] cellvars, String[] freevars) {
 
         debug = defaultDebug;
 
         co_argcount = nargs = argcount;
         co_varnames = varnames;
         co_nlocals = nlocals; // maybe assert = varnames.length;
         co_filename = filename;
         co_firstlineno = firstlineno;
         co_cellvars = cellvars;
         co_freevars = freevars;
         co_name = name;
         varargs = (flags & CO_VARARGS) != 0;
         varkwargs = (flags & CO_VARKEYWORDS) != 0;
         co_flags |= flags;
 
         co_stacksize = stacksize;
         co_consts = constants;
         co_names = names;
         co_code = codestring.toCharArray();
         co_lnotab = lnotab; // ignore
     }
     private static final String[] __members__ = {
         "co_name", "co_argcount",
         "co_varnames", "co_filename", "co_firstlineno",
         "co_flags", "co_cellvars", "co_freevars", "co_nlocals",
         "co_code", "co_consts", "co_names", "co_lnotab", "co_stacksize"
     };
 
     public PyObject __dir__() {
         PyString members[] = new PyString[__members__.length];
         for (int i = 0; i < __members__.length; i++) {
             members[i] = new PyString(__members__[i]);
         }
         return new PyList(members);
     }
 
     private void throwReadonly(String name) {
         for (int i = 0; i < __members__.length; i++) {
             if (__members__[i] == name) {
                 throw Py.TypeError("readonly attribute");
             }
         }
         throw Py.AttributeError(name);
     }
 
     public void __setattr__(String name, PyObject value) {
         // no writable attributes
         throwReadonly(name);
     }
 
     public void __delattr__(String name) {
         throwReadonly(name);
     }
 
     private static PyTuple toPyStringTuple(String[] ar) {
         if (ar == null) {
             return Py.EmptyTuple;
         }
         int sz = ar.length;
         PyString[] pystr = new PyString[sz];
         for (int i = 0; i < sz; i++) {
             pystr[i] = new PyString(ar[i]);
         }
         return new PyTuple(pystr);
     }
 
     public PyObject __findattr_ex__(String name) {
         // have to craft co_varnames specially
         if (name == "co_varnames") {
             return toPyStringTuple(co_varnames);
         }
         if (name == "co_cellvars") {
             return toPyStringTuple(co_cellvars);
         }
         if (name == "co_freevars") {
             return toPyStringTuple(co_freevars);
         }
         if (name == "co_filename") {
             return new PyString(co_filename);
         }
         if (name == "co_name") {
             return new PyString(co_name);
         }
         return super.__findattr_ex__(name);
     }
 
     enum Why {
 
         NOT, /* No error */
         EXCEPTION, /* Exception occurred */
         RERAISE, /* Exception re-raised by 'finally' */
         RETURN, /* 'return' statement */
         BREAK, /* 'break' statement */
         CONTINUE, /* 'continue' statement */
         YIELD	/* 'yield' operator */
 
     };
 
     // to enable why's to be stored on a PyStack
     private class PyStackWhy extends PyObject {
 
         Why why;
 
         PyStackWhy(Why why) {
             this.why = why;
         }
 
         @Override
         public String toString() {
             return why.toString();
         }
     }
 
     private class PyStackException extends PyObject {
 
         PyException exception;
 
         PyStackException(PyException exception) {
             this.exception = exception;
         }
 
         @Override
         public String toString() {
             return exception.toString();
         }
     }
 
     private static String stringify_blocks(PyFrame f) {
         if (f.f_exits == null || f.f_blockstate[0] == 0) {
             return "[]";
         }
         StringBuilder buf = new StringBuilder("[");
         int len = f.f_blockstate[0];
         for (int i = 0; i < len; i++) {
             buf.append(f.f_exits[i].toString());
             if (i < len - 1) {
                 buf.append(", ");
             }
         }
         buf.append("]");
         return buf.toString();
     }
 
     private void print_debug(int count, int next_instr, int opcode, int oparg, PyStack stack, PyFrame f) {
         if (debug) {
             System.err.println(co_name + ":" +
                     count + "," + f.f_lasti + "> opcode: " +
                     get_opname().__getitem__(Py.newInteger(opcode)) +
                     (opcode >= Opcode.HAVE_ARGUMENT ? ", oparg: " + oparg : "") +
                     ", stack: " + stack.toString() +
                     ", blocks: " + stringify_blocks(f));
         }
     }
 
     private static PyTryBlock popBlock(PyFrame f) {
         return (PyTryBlock) (f.f_exits[--f.f_blockstate[0]]);
     }
 
     private static void pushBlock(PyFrame f, PyTryBlock block) {
         if (f.f_exits == null) { // allocate in the frame where they can fit! consider supporting directly in the frame
             f.f_exits = new PyObject[CO_MAXBLOCKS]; // f_blockstack in CPython - a simple ArrayList might be best
             f.f_blockstate = new int[]{0};        // f_iblock in CPython - f_blockstate is likely go away soon
         }
         f.f_exits[f.f_blockstate[0]++] = block;
     }
 
     private boolean blocksLeft(PyFrame f) {
         if (f.f_exits != null) {
             return f.f_blockstate[0] > 0;
         } else {
             return false;
         }
     }
 
     @Override
     protected PyObject interpret(PyFrame f) {
         final PyStack stack = new PyStack();
         int next_instr = -1;
         int opcode;	/* Current opcode */
         int oparg = 0; /* Current opcode argument, if any */
         Why why = Why.NOT;
         PyObject retval = null;
         ThreadState ts = Py.getThreadState(); // XXX - change interpret to pass through from PyFrame since our ts will never change
 
         // XXX - optimization opportunities
         // 1. consider detaching the setting/getting of frame fields to improve performance, instead do this
         // in a shadow version of the frame that we copy back to on entry/exit and downcalls
 
         if (debug) {
             System.err.println(co_name + ":" + f.f_lasti + "/" + co_code.length +
                     ", cells:" + Arrays.toString(co_cellvars) + ", free:" + Arrays.toString(co_freevars));
             int i = 0;
             for (String cellvar : co_cellvars) {
                 System.err.println(cellvar + " = " + f.f_env[i++]);
             }
             for (String freevar : co_freevars) {
                 System.err.println(freevar + " = " + f.f_env[i++]);
             }
             get_dis().invoke("disassemble", this);
 
         }
         if (f.f_lasti >= co_code.length) {
             throw Py.SystemError(""); // XXX - chose an appropriate error!!!
         }
 
         next_instr = f.f_lasti;
 
         // the restore stack aspects should occur ONLY after a yield
         boolean checkGeneratorInput = false;
         if (f.f_savedlocals != null) {
             for (int i = 0; i < f.f_savedlocals.length; i++) {
                 PyObject v = (PyObject) (f.f_savedlocals[i]);
                 stack.push(v);
             }
             checkGeneratorInput = true;
             f.f_savedlocals = null;
         }
 
         while (!debug || (maxCount == -1 || count < maxCount)) { // XXX - replace with while(true)
 
             try {
 
                 if (checkGeneratorInput) {
                     checkGeneratorInput = false;
                     Object generatorInput = f.getGeneratorInput();
                     if (generatorInput instanceof PyException) {
                         throw (PyException) generatorInput;
                     }
                     stack.push((PyObject) generatorInput);
                 }
 
                 opcode = co_code[next_instr];
                 if (opcode >= Opcode.HAVE_ARGUMENT) {
                     next_instr += 2;
                     oparg = (co_code[next_instr] << 8) + co_code[next_instr - 1];
                 }
                 print_debug(count, next_instr, opcode, oparg, stack, f);
 
                 count += 1;
                 next_instr += 1;
                 f.f_lasti = next_instr; // should have no worries about needing co_lnotab, just keep this current
 
                 switch (opcode) {
                     case Opcode.NOP:
                         break;
 
                     case Opcode.LOAD_FAST:
                         stack.push(f.getlocal(oparg));
                         break;
 
                     case Opcode.LOAD_CONST:
                         stack.push(co_consts[oparg]);
                         break;
 
                     case Opcode.STORE_FAST:
                         f.setlocal(oparg, stack.pop());
                         break;
 
                     case Opcode.POP_TOP:
                         stack.pop();
                         break;
 
                     case Opcode.ROT_TWO:
                         stack.rot2();
                         break;
 
                     case Opcode.ROT_THREE:
                         stack.rot3();
                         break;
 
                     case Opcode.ROT_FOUR:
                         stack.rot4();
                         break;
 
                     case Opcode.DUP_TOP:
                         stack.dup();
                         break;
 
                     case Opcode.DUP_TOPX: {
                         if (oparg == 2 || oparg == 3) {
                             stack.dup(oparg);
                         } else {
                             throw Py.RuntimeError("invalid argument to DUP_TOPX" +
                                     " (bytecode corruption?)");
                         }
                         break;
                     }
 
                     case Opcode.UNARY_POSITIVE:
                         stack.push(stack.pop().__pos__());
                         break;
 
                     case Opcode.UNARY_NEGATIVE:
                         stack.push(stack.pop().__neg__());
                         break;
 
                     case Opcode.UNARY_NOT:
                         stack.push(stack.pop().__not__());
                         break;
 
                     case Opcode.UNARY_CONVERT:
                         stack.push(stack.pop().__repr__());
                         break;
 
                     case Opcode.UNARY_INVERT:
                         stack.push(stack.pop().__invert__());
                         break;
 
                     case Opcode.BINARY_POWER: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._pow(b));
                         break;
                     }
 
                     case Opcode.BINARY_MULTIPLY: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._mul(b));
                         break;
                     }
 
                     case Opcode.BINARY_DIVIDE: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
 
                         if ((co_flags & CO_FUTUREDIVISION) == 0) {
                             stack.push(a._div(b));
                         } else {
                             stack.push(a._truediv(b));
                         }
                         break;
                     }
 
                     case Opcode.BINARY_TRUE_DIVIDE: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._truediv(b));
                         break;
                     }
 
                     case Opcode.BINARY_FLOOR_DIVIDE: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._floordiv(b));
                         break;
                     }
 
                     case Opcode.BINARY_MODULO: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._mod(b));
                         break;
                     }
 
                     case Opcode.BINARY_ADD: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._add(b));
                         break;
                     }
 
                     case Opcode.BINARY_SUBTRACT: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._sub(b));
                         break;
                     }
 
                     case Opcode.BINARY_SUBSCR: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a.__getitem__(b));
                         break;
                     }
 
                     case Opcode.BINARY_LSHIFT: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._lshift(b));
                         break;
                     }
 
                     case Opcode.BINARY_RSHIFT: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._rshift(b));
                         break;
                     }
 
                     case Opcode.BINARY_AND: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._and(b));
                         break;
                     }
 
 
                     case Opcode.BINARY_XOR: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._xor(b));
                         break;
                     }
 
                     case Opcode.BINARY_OR: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._or(b));
                         break;
                     }
 
                     case Opcode.LIST_APPEND: {
                         PyObject b = stack.pop();
                         PyList a = (PyList) (stack.pop());
                         a.append(b);
                         break;
                     }
 
                     case Opcode.INPLACE_POWER: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._ipow(b));
                         break;
                     }
 
                     case Opcode.INPLACE_MULTIPLY: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._imul(b));
                         break;
                     }
 
                     case Opcode.INPLACE_DIVIDE: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         if ((co_flags & CO_FUTUREDIVISION) == 0) {
                             stack.push(a._idiv(b));
                         } else {
                             stack.push(a._itruediv(b));
                         }
                         break;
                     }
 
                     case Opcode.INPLACE_TRUE_DIVIDE: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._itruediv(b));
                         break;
                     }
 
                     case Opcode.INPLACE_FLOOR_DIVIDE: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._ifloordiv(b));
                         break;
                     }
 
                     case Opcode.INPLACE_MODULO: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._imod(b));
                         break;
                     }
 
                     case Opcode.INPLACE_ADD: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._iadd(b));
                         break;
                     }
 
                     case Opcode.INPLACE_SUBTRACT: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._isub(b));
                         break;
                     }
 
                     case Opcode.INPLACE_LSHIFT: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._ilshift(b));
                         break;
                     }
 
                     case Opcode.INPLACE_RSHIFT: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._irshift(b));
                         break;
                     }
 
                     case Opcode.INPLACE_AND: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._iand(b));
                         break;
                     }
 
                     case Opcode.INPLACE_XOR: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._ixor(b));
                         break;
                     }
 
                     case Opcode.INPLACE_OR: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
                         stack.push(a._ior(b));
                         break;
                     }
 
                     case Opcode.SLICE + 0:
                     case Opcode.SLICE + 1:
                     case Opcode.SLICE + 2:
                     case Opcode.SLICE + 3: {
                         PyObject stop = (((opcode - Opcode.SLICE) & 2) != 0) ? stack.pop() : null;
                         PyObject start = (((opcode - Opcode.SLICE) & 1) != 0) ? stack.pop() : null;
                         PyObject obj = stack.pop();
                         stack.push(obj.__getslice__(start, stop));
                         break;
                     }
 
                     case Opcode.STORE_SLICE + 0:
                     case Opcode.STORE_SLICE + 1:
                     case Opcode.STORE_SLICE + 2:
                     case Opcode.STORE_SLICE + 3: {
                         PyObject stop = (((opcode - Opcode.STORE_SLICE) & 2) != 0) ? stack.pop() : null;
                         PyObject start = (((opcode - Opcode.STORE_SLICE) & 1) != 0) ? stack.pop() : null;
                         PyObject obj = stack.pop();
                         PyObject value = stack.pop();
                         obj.__setslice__(start, stop, value);
                         break;
                     }
 
                     case Opcode.DELETE_SLICE + 0:
                     case Opcode.DELETE_SLICE + 1:
                     case Opcode.DELETE_SLICE + 2:
                     case Opcode.DELETE_SLICE + 3: {
                         PyObject stop = (((opcode - Opcode.DELETE_SLICE) & 2) != 0) ? stack.pop() : null;
                         PyObject start = (((opcode - Opcode.DELETE_SLICE) & 1) != 0) ? stack.pop() : null;
                         PyObject obj = stack.pop();
                         obj.__delslice__(start, stop);
                         break;
                     }
 
                     case Opcode.STORE_SUBSCR: {
                         PyObject key = stack.pop();
                         PyObject obj = stack.pop();
                         PyObject value = stack.pop();
                         obj.__setitem__(key, value);
                         break;
                     }
 
                     case Opcode.DELETE_SUBSCR: {
                         PyObject key = stack.pop();
                         PyObject obj = stack.pop();
                         obj.__delitem__(key);
                         break;
                     }
 
                     case Opcode.PRINT_EXPR:
                         PySystemState.displayhook(stack.pop());
                         break;
 
                     case Opcode.PRINT_ITEM_TO:
                         Py.printComma(stack.pop(), stack.pop());
                         break;
 
                     case Opcode.PRINT_ITEM:
                         Py.printComma(stack.pop());
                         break;
 
                     case Opcode.PRINT_NEWLINE_TO:
                         Py.printlnv(stack.pop());
                         break;
 
                     case Opcode.PRINT_NEWLINE:
                         Py.println();
                         break;
 
                     case Opcode.RAISE_VARARGS:
 
                         switch (oparg) {
                             case 3: {
                                 PyTraceback tb = (PyTraceback) (stack.pop());
                                 PyObject value = stack.pop();
                                 PyObject type = stack.pop();
                                 throw PyException.doRaise(type, value, tb);
                             }
                             case 2: {
                                 PyObject value = stack.pop();
                                 PyObject type = stack.pop();
                                 throw PyException.doRaise(type, value, null);
                             }
                             case 1: {
                                 PyObject type = stack.pop();
                                 throw PyException.doRaise(type, null, null);
                             }
                             case 0:
                                 throw PyException.doRaise(null, null, null);
                             default:
                                 throw Py.SystemError("bad RAISE_VARARGS oparg");
                         }
 
                     case Opcode.LOAD_LOCALS:
                         stack.push(f.f_locals);
                         break;
 
                     case Opcode.RETURN_VALUE:
                         retval = stack.pop();
                         why = Why.RETURN;
                         break;
 
                     case Opcode.YIELD_VALUE:
                         retval = stack.pop();
                         why = Why.YIELD;
                         break;
 
                     case Opcode.EXEC_STMT: {
                         PyObject locals = stack.pop();
                         PyObject globals = stack.pop();
                         PyObject code = stack.pop();
                         Py.exec(code, globals, locals);
                         break;
                     }
 
                     case Opcode.POP_BLOCK: {
                         PyTryBlock b = popBlock(f);
                         while (stack.size() > b.b_level) {
                             stack.pop();
                         }
                         break;
                     }
 
                     case Opcode.END_FINALLY: {
                         PyObject v = stack.pop();
                         if (v instanceof PyStackWhy) {
                             why = ((PyStackWhy) v).why;
                             assert (why != Why.YIELD);
                             if (why == Why.RETURN || why == Why.CONTINUE) {
                                 retval = stack.pop();
                             }
                        } else if (v instanceof PyStackException) {
                             ts.exception = ((PyStackException) v).exception;
                             why = Why.RERAISE;

                        } else if (v instanceof PyString) {
                            if (debug) {
                                System.err.println("ts.exception=" + ts.exception + ", v=" + v);
//                            ts.exception = new PyException(v); // XXX - what do I do about string exceptions??!!!
                            }
                            why = Why.RERAISE;
                         } else if (v != Py.None) {
                             throw Py.SystemError("'finally' pops bad exception");
                         }
                         break;
                     }
 
                     case Opcode.BUILD_CLASS: {
                         PyObject methods = stack.pop();
                         PyObject bases[] = ((PySequenceList) (stack.pop())).getArray();
                         String name = stack.pop().toString();
                         stack.push(Py.makeClass(name, bases, methods));
                         break;
                     }
 
                     case Opcode.STORE_NAME:
                         f.setlocal(co_names[oparg], stack.pop());
                         break;
 
                     case Opcode.DELETE_NAME:
                         f.dellocal(co_names[oparg]);
                         break;
 
                     case Opcode.UNPACK_SEQUENCE: {
                         PyObject v = stack.pop();
                         int i = 0;
                         PyObject items[] = new PyObject[oparg];
                         for (PyObject item : v.asIterable()) {
                             items[i++] = item;
                             if (i > oparg) {
                                 break;
                             }
                         }
                         for (i = i - 1; i >= 0; i--) {
                             stack.push(items[i]);
                         }
                         break;
                     }
 
                     case Opcode.STORE_ATTR: {
                         PyObject obj = stack.pop();
                         PyObject v = stack.pop();
                         obj.__setattr__(co_names[oparg], v);
                         break;
                     }
 
                     case Opcode.DELETE_ATTR:
                         stack.pop().__delattr__(co_names[oparg]);
                         break;
 
                     case Opcode.STORE_GLOBAL:
                         f.setglobal(co_names[oparg], stack.pop());
                         break;
 
                     case Opcode.DELETE_GLOBAL:
                         f.delglobal(co_names[oparg]);
                         break;
 
                     case Opcode.LOAD_NAME:
                         stack.push(f.getname(co_names[oparg]));
                         break;
 
                     case Opcode.LOAD_GLOBAL:
                         stack.push(f.getglobal(co_names[oparg]));
                         break;
 
                     case Opcode.DELETE_FAST:
                         f.dellocal(oparg);
                         break;
 
                     case Opcode.LOAD_CLOSURE: {
                         PyCell cell = (PyCell) (f.getclosure(oparg));
                         if (cell.ob_ref == null) {
                             String name;
                             if (oparg >= co_cellvars.length) {
                                 name = co_freevars[oparg - co_cellvars.length];
                             } else {
                                 name = co_cellvars[oparg];
                             }
 //                            System.err.println("Loading closure: " + name);
                             // XXX - consider some efficient lookup mechanism, like a hash
                             if (f.f_fastlocals != null) {
                                 int i = 0;
                                 boolean matched = false;
                                 for (String match : co_varnames) {
                                     if (match.equals(name)) {
                                         matched = true;
                                         break;
                                     }
                                     i++;
                                 }
                                 if (matched) {
                                     cell.ob_ref = f.f_fastlocals[i];
                                 }
                             } else {
                                 cell.ob_ref = f.f_locals.__finditem__(name);
                             }
                         }
                         stack.push(cell);
                         break;
                     }
 
                     case Opcode.LOAD_DEREF: {
                         stack.push(f.getderef(oparg));
                         break;
                     }
 
                     case Opcode.STORE_DEREF:
                         f.setderef(oparg, stack.pop());
                         break;
 
                     case Opcode.BUILD_TUPLE:
                         stack.push(new PyTuple(stack.popN(oparg)));
                         break;
 
                     case Opcode.BUILD_LIST:
                         stack.push(new PyList(stack.popN(oparg)));
                         break;
 
                     case Opcode.BUILD_MAP:
                         stack.push(new PyDictionary());
                         break;
 
                     case Opcode.LOAD_ATTR:
                         stack.push(stack.pop().__getattr__(co_names[oparg]));
                         break;
 
                     case Opcode.COMPARE_OP: {
                         PyObject b = stack.pop();
                         PyObject a = stack.pop();
 
                         switch (oparg) {
 
                             case Opcode.PyCmp_LT:
                                 stack.push(a._lt(b));
                                 break;
                             case Opcode.PyCmp_LE:
                                 stack.push(a._le(b));
                                 break;
                             case Opcode.PyCmp_EQ:
                                 stack.push(a._eq(b));
                                 break;
                             case Opcode.PyCmp_NE:
                                 stack.push(a._ne(b));
                                 break;
                             case Opcode.PyCmp_GT:
                                 stack.push(a._gt(b));
                                 break;
                             case Opcode.PyCmp_GE:
                                 stack.push(a._ge(b));
                                 break;
                             case Opcode.PyCmp_IN:
                                 stack.push(a._in(b));
                                 break;
                             case Opcode.PyCmp_NOT_IN:
                                 stack.push(a._notin(b));
                                 break;
                             case Opcode.PyCmp_IS:
                                 stack.push(a._is(b));
                                 break;
                             case Opcode.PyCmp_IS_NOT:
                                 stack.push(a._isnot(b));
                                 break;
                             case Opcode.PyCmp_EXC_MATCH:
                                 if (a instanceof PyStackException) {
                                     PyException pye = ((PyStackException) a).exception;
                                     stack.push(Py.newBoolean(Py.matchException(pye, b)));
                                 } else {
                                     stack.push(Py.newBoolean(Py.matchException(new PyException(a), b)));
                                 }
                                 break;
 
                         }
                         break;
                     }
 
                     case Opcode.IMPORT_NAME: {
                         PyObject __import__ = f.f_builtins.__finditem__("__import__");
                         if (__import__ == null) {
                             throw Py.ImportError("__import__ not found");
                         }
                         PyString name = Py.newString(co_names[oparg]);
                         PyObject fromlist = stack.pop();
                         PyObject level = stack.pop();
 
                         if (level.asInt() != -1) {
                             stack.push(__import__.__call__(new PyObject[]{name, f.f_globals, f.f_locals, fromlist, level}));
                         } else {
                             stack.push(__import__.__call__(new PyObject[]{name, f.f_globals, f.f_locals, fromlist}));
                         }
                         break;
                     }
 
                     case Opcode.IMPORT_STAR: {
                         PyObject module = stack.pop();
                         imp.importAll(module, f);
                         break;
                     }
 
                     case Opcode.IMPORT_FROM:
                         String name = co_names[oparg];
                         try {
                             stack.push(stack.top().__getattr__(name));
 
                         } catch (PyException pye) {
                             if (Py.matchException(pye, Py.AttributeError)) {
                                 throw Py.ImportError(String.format("cannot import name %.230s", name));
                             } else {
                                 throw pye;
                             }
                         }
                         break;
 
                     case Opcode.JUMP_FORWARD:
                         next_instr += oparg;
                         break;
 
                     case Opcode.JUMP_IF_FALSE:
                         if (!stack.top().__nonzero__()) {
                             next_instr += oparg;
                         }
                         break;
 
                     case Opcode.JUMP_IF_TRUE:
                         if (stack.top().__nonzero__()) {
                             next_instr += oparg;
                         }
                         break;
 
                     case Opcode.JUMP_ABSOLUTE:
                         next_instr = oparg;
                         break;
 
                     case Opcode.GET_ITER: {
                         PyObject it = stack.top().__iter__();
                         if (it != null) {
                             stack.set_top(it);
                         }
                         break;
                     }
 
                     case Opcode.FOR_ITER: {
                         PyObject it = stack.pop();
                         try {
                             PyObject x = it.__iternext__();
                             if (x != null) {
                                 stack.push(it);
                                 stack.push(x);
                                 break;
                             }
                         } catch (PyException pye) {
                             if (!Py.matchException(pye, Py.StopIteration)) {
                                 throw pye;
                             }
                         }
                         next_instr += oparg;
                         break;
                     }
 
                     case Opcode.BREAK_LOOP:
                         why = Why.BREAK;
                         break;
 
                     case Opcode.CONTINUE_LOOP:
                         retval = Py.newInteger(oparg);
                         if (retval.__nonzero__()) {
                             why = Why.CONTINUE;
                         }
                         break;
 
                     case Opcode.SETUP_LOOP:
                     case Opcode.SETUP_EXCEPT:
                     case Opcode.SETUP_FINALLY:
                         pushBlock(f, new PyTryBlock(opcode, next_instr + oparg, stack.size()));
                         break;
 
                     case Opcode.WITH_CLEANUP: {
                         /* TOP is the context.__exit__ bound method.
                         Below that are 1-3 values indicating how/why
                         we entered the finally clause:
                         - SECOND = None
                         - (SECOND, THIRD) = (WHY_{RETURN,CONTINUE}), retval
                         - SECOND = WHY_*; no retval below it
                         - (SECOND, THIRD, FOURTH) = exc_info()
                         In the last case, we must call
                         TOP(SECOND, THIRD, FOURTH)
                         otherwise we must call
                         TOP(None, None, None)
 
                         In addition, if the stack represents an exception,
                          *and* the function call returns a 'true' value, we
                         "zap" this information, to prevent END_FINALLY from
                         re-raising the exception.  (But non-local gotos
                         should still be resumed.)
                          */
                         PyObject exit = stack.top();
                         PyObject u = stack.top(2);
                         PyObject v;
                         PyObject w;
                         if (u == Py.None || u instanceof PyStackWhy) {
                             u = v = w = Py.None;
                         } else {
                             v = stack.top(3);
                             w = stack.top(4);
                         }
                         PyObject x = exit.__call__(u, v, w);
                         if (u != Py.None && x.__nonzero__()) {
                             stack.popN(4);
                             stack.push(Py.None);
                         } else {
                             stack.pop();
                         }
                         break;
                     }
 
                     case Opcode.CALL_FUNCTION: {
                         int na = oparg & 0xff;
                         int nk = (oparg >> 8) & 0xff;
 
                         if (nk == 0) {
                             call_function(na, stack);
                         } else {
                             call_function(na, nk, stack);
                         }
                         break;
                     }
 
                     case Opcode.CALL_FUNCTION_VAR:
                     case Opcode.CALL_FUNCTION_KW:
                     case Opcode.CALL_FUNCTION_VAR_KW: {
                         int na = oparg & 0xff;
                         int nk = (oparg >> 8) & 0xff;
                         int flags = (opcode - Opcode.CALL_FUNCTION) & 3;
                         call_function(na, nk,
                                 (flags & CALL_FLAG_VAR) != 0,
                                 (flags & CALL_FLAG_KW) != 0,
                                 stack);
                         break;
                     }
 
                     case Opcode.MAKE_FUNCTION: {
                         PyCode code = (PyCode) stack.pop();
                         PyObject[] defaults = stack.popN(oparg);
                         PyFunction func = new PyFunction(f.f_globals, defaults, code);
                         stack.push(func);
                         break;
                     }
 
                     case Opcode.MAKE_CLOSURE: {
                         PyCode code = (PyCode) stack.pop();
                         PyObject[] closure_cells = ((PySequenceList) (stack.pop())).getArray();
                         PyObject[] defaults = stack.popN(oparg);
                         PyFunction func = new PyFunction(f.f_globals, defaults, code, closure_cells);
                         stack.push(func);
                         break;
                     }
 
                     case Opcode.BUILD_SLICE: {
                         PyObject step = oparg == 3 ? stack.pop() : null;
                         PyObject stop = stack.pop();
                         PyObject start = stack.pop();
                         stack.push(new PySlice(start, stop, step));
                         break;
                     }
 
                     case Opcode.EXTENDED_ARG:
                         opcode = co_code[next_instr++];
                         next_instr += 2;
                         oparg = oparg << 16 | ((co_code[next_instr] << 8) + co_code[next_instr - 1]);
                         break;
 
                     default:
                         Py.print(Py.getSystemState().stderr,
                                 Py.newString(
                                 String.format("XXX lineno: %d, opcode: %d\n",
                                 f.f_lasti, opcode)));
                         throw Py.SystemError("unknown opcode");
 
                 } // end switch
             } // end try
             catch (Throwable t) {
                 PyException pye = Py.JavaError(t);
                 why = Why.EXCEPTION;
                 ts.exception = pye;
                 if (debug) {
                     System.err.println("Caught exception:" + pye);
                 }
             }
 
             if (why == Why.YIELD) {
                 break;
             }
 
             // do some trace handling here, but for now just convert to EXCEPTION
             if (why == Why.RERAISE) {
                 why = Why.EXCEPTION;
             }
 
             while (why != Why.NOT && blocksLeft(f)) {
                 PyTryBlock b = popBlock(f);
                 if (debug) {
                     System.err.println("Processing block: " + b);
                 }
                 assert (why != Why.YIELD);
                 if (b.b_type == Opcode.SETUP_LOOP && why == Why.CONTINUE) {
                     pushBlock(f, b);
                     why = Why.NOT;
                     next_instr = retval.asInt();
                     break;
                 }
                 while (stack.size() > b.b_level) {
                     stack.pop();
                 }
                 if (b.b_type == Opcode.SETUP_LOOP && why == Why.BREAK) {
                     why = Why.NOT;
                     next_instr = b.b_handler;
                     break;
                 }
                 if (b.b_type == Opcode.SETUP_FINALLY || (b.b_type == Opcode.SETUP_EXCEPT && why == Why.EXCEPTION)) {
                     if (why == Why.EXCEPTION) {
                         PyException exc = ts.exception;
                         if (b.b_type == Opcode.SETUP_EXCEPT) {
                             exc.normalize();
                         }
                         stack.push(exc.traceback);
                         stack.push(exc.value);
                         stack.push(new PyStackException(exc)); // instead of stack.push(exc.type);, like CPython
                     } else {
                         if (why == Why.RETURN || why == Why.CONTINUE) {
                             stack.push(retval);
                         }
                        // FIGURE OUT THE FLOW HERE, in test_optparse2
                        stack.push(new PyStackWhy(why));
                     }
                     why = Why.NOT;
                     next_instr = b.b_handler;
                     break;
                 }
             } // unwindstack
 
             if (why != Why.NOT) {
                 break;
             }
 
         } // end-while of the instruction loop
 
         if (why != Why.YIELD) {
             while (stack.size() > 0) {
                 stack.pop();
             }
             if (why != Why.RETURN) {
                 retval = Py.None;
             }
         } else {
             // store the stack in the frame for reentry from the yield;
             f.f_savedlocals = stack.popN(stack.size());
         }
 
         f.f_lasti = next_instr; // need to update on function entry, etc
 
         if (debug) {
             System.err.println(count + "," + f.f_lasti + "> Returning from " + why + ": " + retval +
                     ", stack: " + stack.toString() +
                     ", blocks: " + stringify_blocks(f));
         }
 
         if (why == why.EXCEPTION) {
             throw ts.exception;
         }
 
         if ((co_flags & CO_GENERATOR) != 0 && why == Why.RETURN && retval == Py.None) {
             f.f_lasti = -1;
         }
 
         return retval;
     }
 
     private static void call_function(int na, PyStack stack) {
         switch (na) {
             case 0: {
                 PyObject callable = stack.pop();
                 stack.push(callable.__call__());
                 break;
             }
             case 1: {
                 PyObject arg = stack.pop();
                 PyObject callable = stack.pop();
                 stack.push(callable.__call__(arg));
                 break;
             }
             case 2: {
                 PyObject arg1 = stack.pop();
                 PyObject arg0 = stack.pop();
                 PyObject callable = stack.pop();
                 stack.push(callable.__call__(arg0, arg1));
                 break;
             }
             case 3: {
                 PyObject arg2 = stack.pop();
                 PyObject arg1 = stack.pop();
                 PyObject arg0 = stack.pop();
                 PyObject callable = stack.pop();
                 stack.push(callable.__call__(arg0, arg1, arg2));
                 break;
             }
             case 4: {
                 PyObject arg3 = stack.pop();
                 PyObject arg2 = stack.pop();
                 PyObject arg1 = stack.pop();
                 PyObject arg0 = stack.pop();
                 PyObject callable = stack.pop();
                 stack.push(callable.__call__(arg0, arg1, arg2, arg3));
                 break;
             }
             default: {
                 PyObject args[] = stack.popN(na);
                 PyObject callable = stack.pop();
                 stack.push(callable.__call__(args));
             }
         }
     }
 
     private static void call_function(int na, int nk, PyStack stack) {
         int n = na + nk * 2;
         PyObject params[] = stack.popN(n);
         PyObject callable = stack.pop();
 
         PyObject args[] = new PyObject[na + nk];
         String keywords[] = new String[nk];
         int i;
         for (i = 0; i < na; i++) {
             args[i] = params[i];
         }
         for (int j = 0; i < n; i += 2, j++) {
             keywords[j] = params[i].toString();
             args[na + j] = params[i + 1];
         }
         stack.push(callable.__call__(args, keywords));
     }
 
     private static void call_function(int na, int nk, boolean var, boolean kw, PyStack stack) {
         int n = na + nk * 2;
         PyObject kwargs = kw ? stack.pop() : null;
         PyObject starargs = var ? stack.pop() : null;
         PyObject params[] = stack.popN(n);
         PyObject callable = stack.pop();
 
         PyObject args[] = new PyObject[na + nk];
         String keywords[] = new String[nk];
         int i;
         for (i = 0; i < na; i++) {
             args[i] = params[i];
         }
         for (int j = 0; i < n; i += 2, j++) {
             keywords[j] = params[i].toString();
             args[na + j] = params[i + 1];
 
         }
         stack.push(callable._callextra(args, keywords, starargs, kwargs));
     }
 
     // XXX - perhaps add support for max stack size (presumably from co_stacksize)
     // and capacity hints
     private class PyStack {
 
         final List<PyObject> stack;
 
         PyStack() {
             stack = new ArrayList<PyObject>();
         }
 
         PyObject top() {
             return stack.get(stack.size() - 1);
         }
 
         PyObject top(int n) {
             return stack.get(stack.size() - n);
         }
 
         PyObject pop() {
             return stack.remove(stack.size() - 1);
         }
 
         void push(PyObject v) {
             stack.add(v);
         }
 
         void set_top(PyObject v) {
             stack.set(stack.size() - 1, v);
         }
 
         void dup() {
             stack.add(top());
         }
 
         void dup(int n) {
             int length = stack.size();
             for (int i = n; i > 0; i--) {
                 stack.add(stack.get(length - i));
             }
         }
 
         PyObject[] popN(int n) {
             int end = stack.size(); // exclusive
             PyObject ret[] = new PyObject[n];
             List<PyObject> lastN = stack.subList(end - n, end);
             lastN.toArray(ret);
             lastN.clear();
             return ret;
         }
 
         void rot2() {
             int length = stack.size();
             PyObject v = stack.get(length - 1);
             PyObject w = stack.get(length - 2);
             stack.set(length - 1, w);
             stack.set(length - 2, v);
         }
 
         void rot3() {
             int length = stack.size();
             PyObject v = stack.get(length - 1);
             PyObject w = stack.get(length - 2);
             PyObject x = stack.get(length - 3);
             stack.set(length - 1, w);
             stack.set(length - 2, x);
             stack.set(length - 3, v);
         }
 
         void rot4() {
             int length = stack.size();
             PyObject u = stack.get(length - 1);
             PyObject v = stack.get(length - 2);
             PyObject w = stack.get(length - 3);
             PyObject x = stack.get(length - 4);
             stack.set(length - 1, v);
             stack.set(length - 2, w);
             stack.set(length - 3, x);
             stack.set(length - 4, u);
         }
 
         int size() {
             return stack.size();
         }
 
         @Override
         public String toString() {
             return stack.toString();
         }
     }
 
     private class PyTryBlock extends PyObject { // purely to sit on top of the existing PyFrame in f_exits!!!
 
         int b_type;			/* what kind of block this is */
 
         int b_handler;		/* where to jump to find handler */
 
         int b_level;		/* value stack level to pop to */
 
 
         PyTryBlock(int type, int handler, int level) {
             b_type = type;
             b_handler = handler;
             b_level = level;
         }
 
         @Override
         public String toString() {
             return "<" + get_opname().__getitem__(Py.newInteger(b_type)) + "," +
                     b_handler + "," + b_level + ">";
         }
     }
 }
 
 
