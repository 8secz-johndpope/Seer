 package org.jetbrains.emacs4ij.jelisp.subroutine;
 
import org.jetbrains.annotations.Nullable;
 import org.jetbrains.emacs4ij.jelisp.Environment;
 import org.jetbrains.emacs4ij.jelisp.GlobalEnvironment;
 import org.jetbrains.emacs4ij.jelisp.elisp.*;
 import org.jetbrains.emacs4ij.jelisp.exception.*;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.List;
 
 import static org.jetbrains.emacs4ij.jelisp.subroutine.BuiltinPredicates.subrp;
 
 /**
  * Created by IntelliJ IDEA.
  * User: Ekaterina.Polishchuk
  * Date: 7/13/11
  * Time: 3:49 PM
  * To change this template use File | Settings | File Templates.
  */
 public abstract class BuiltinsCore {
     private BuiltinsCore() {}
 
     public static void error (Environment environment, String message) {
         error(environment, message, null);
     }
 
     public static void error (Environment environment, String message, @Nullable LObject... args) {
         ArrayList<LObject> data = new ArrayList<>();
         data.add(new LispSymbol("error"));
         data.add(new LispString(message));
         if (args != null) {
             data.addAll(Arrays.asList(args));
         }
         LispList.list(data).evaluate(environment);
     }
 
 
     @Subroutine("set")
     public static LObject set (Environment environment, LispSymbol variable, LObject initValue) {
         LObject value = (initValue == null) ? LispSymbol.ourVoid : initValue;
         LispSymbol symbol = new LispSymbol(variable.getName(), value);
         environment.setVariable(symbol);
         return value;
     }
 
     public static boolean equals (LObject one, LObject two) {
         return one.equals(two);
     }
 
     @Subroutine("equal")
     public static LispObject equal (LObject one, LObject two) {
         return LispSymbol.bool(equals(one, two));
     }
 
     /* eq returns t if object1 and object2 are integers with the same value.
     Also, since symbol names are normally unique, if the arguments are symbols with the same name, they are eq.
     For other types (e.g., lists, vectors, strings), two arguments with the same contents or elements are not necessarily eq to each
     other: they are eq only if they are the same object, meaning that a change in the contents of one will be reflected by the
     same change in the contents of the other.
     * */
 
     public static boolean eqs (LObject one, LObject two) {
         if (one == two) return true;
         if (one.getClass() != two.getClass()) return false;
         if (one instanceof LispNumber) {
             return (((LispNumber) one).getData()  == ((LispNumber) two).getData());
         }
         if (one instanceof LispSymbol) {
             return ((LispSymbol) one).getName().equals(((LispSymbol) two).getName());
         }
         if ((one instanceof LispString) && (((LispString) one).getData().equals(""))) {
             return ((LispString) two).getData().equals("");
         }
         return false;
     }
 
     @Subroutine("eq")
     public static LispObject eq (LObject one, LObject two) {
         return LispSymbol.bool(eqs(one, two));
     }
 
     @Subroutine("null")
     public static LispObject lispNull (LObject lObject) {
         return LispSymbol.bool(lObject.equals(LispSymbol.ourNil));
     }
 
     @Subroutine("not")
     public static LispObject lispNot (LObject lObject) {
         return lispNull(lObject);
     }
 
     @Subroutine("call-interactively")
     public static LObject callInteractively (Environment environment, LispSymbol function, @Optional LObject recordFlag, LObject keys) {
         if (!BuiltinPredicates.commandp(environment, function, null).equals(LispSymbol.ourT))
             throw new WrongTypeArgumentException("commandp", function.getName());
         //read args
         //assign args
         //invoke function
         return LispSymbol.ourNil;
 
     }
 
     @Subroutine("funcall")
     public static LObject functionCall (Environment environment, LObject function, @Optional LObject... args) {
         environment.setArgumentsEvaluated(true);
         ArrayList<LObject> data = new ArrayList<LObject>();
         data.add(function);
         Collections.addAll(data, args);
         LispList funcall = LispList.list(data);
         return funcall.evaluate(environment);
     }
 
     @Subroutine("signal")
     public static LObject signal (LispSymbol errorSymbol, LispList data) {
         LObject errorMessage = errorSymbol.getProperty("error-message");
         String msg = '[' + errorSymbol.getName() + "] ";
         msg += (errorMessage instanceof LispString) ? ((LispString) errorMessage).getData() : "peculiar error";
         msg += ": " + data.toString();
 //        GlobalEnvironment.showErrorMessage(msg);
         //todo: this method returns for test only
         //  return new LispString(msg);
         System.out.println(msg);
         throw new LispException(msg);
     }
 
     private static void runFunction (Environment environment, LispSymbol function) {
         if (function.equals(LispSymbol.ourNil))
             return;
         if (!function.isFunction()) {
             throw new InvalidFunctionException(function.getName());
         }
         function.evaluateFunction(environment, new ArrayList<LObject>());
     }
 
     @Subroutine("run-hooks")
     public static LObject runHooks (Environment environment, @Optional LispSymbol... hooks) {
         if (hooks == null)
             return LispSymbol.ourNil;
         for (LispSymbol hook: hooks) {
             LispSymbol tHook = environment.find(hook.getName());
             if (tHook == null || tHook.equals(LispSymbol.ourNil))
                 continue;
             if (hook.getValue() instanceof LispSymbol) {
                 runFunction(environment, (LispSymbol) hook.getValue());
                 continue;
             }
             if (hook.getValue() instanceof LispList) {
                 for (LObject function: ((LispList) hook.getValue()).toLObjectList()) {
                     if (!(function instanceof LispSymbol))
                         throw new WrongTypeArgumentException("symbolp", function.toString());
 
                     LispSymbol tFunction = environment.find(((LispSymbol)function).getName());
                     runFunction(environment, tFunction);
                 }
                 continue;
             }
             throw new InvalidFunctionException(hook.getValue().toString());
         }
         return LispSymbol.ourNil;
     }
 
     @Subroutine("macroexpand")
     public static LObject macroExpand (Environment environment, LObject macroCall) {
         if (!(macroCall instanceof LispList))
             return macroCall;
         LispSymbol macro;
         try {
             macro = (LispSymbol) ((LispList) macroCall).car();
         } catch (ClassCastException e) {
             return macroCall;
         }
         LispSymbol trueMacro = environment.find(macro.getName());
         if (!trueMacro.isMacro())
             return macroCall;
 
         return trueMacro.macroExpand(environment, ((LispList) ((LispList) macroCall).cdr()).toLObjectList());
     }
 
     @Subroutine("fset")
     public static LObject functionSet (Environment environment, LispSymbol symbol, LObject function) {
         symbol.setFunction(function);
         environment.setVariable(symbol);
         return function;
     }
 
     /* private static LObject signalOrNot (LObject noError, String name, String data) {
         if (noError != null && !noError.equals(LispSymbol.ourNil))
             return LispSymbol.ourNil;
 
 
         LispSymbol errorSymbol = new LispSymbol(name);
         errorSymbol.setProperty("error-message", new LispString(name));
         return signal(errorSymbol, new LispList(new LispSymbol(data)));    
     }*/
 
     @Subroutine("indirect-function")
     public static LObject indirectFunction (LObject object, @Optional LObject noError) {
         if (!(object instanceof LispSymbol)) {
             return object;
         }
         LispSymbol symbol = (LispSymbol) object;
         ArrayList<String> examined = new ArrayList<String>();
         examined.add(symbol.getName());
 
         while (true) {
             if (!symbol.isFunction()) {
                 if (noError != null && !noError.equals(LispSymbol.ourNil))
                     return LispSymbol.ourNil;
                 throw new VoidFunctionException(((LispSymbol) object).getName());
                 //return signalOrNot(noError, "void-function", symbol.getName());
             }
             LObject f = symbol.getFunction();
             if (f instanceof LispSymbol) {
                 if (examined.contains(((LispSymbol) f).getName())) {
                     if (noError != null && !noError.equals(LispSymbol.ourNil))
                         return LispSymbol.ourNil;
                     throw new CyclicFunctionIndirectionException(symbol.getName());
 
                     //return signalOrNot(noError, "cyclic-function-indirection", symbol.getName());
                 }
                 symbol = (LispSymbol) f;
                 examined.add(symbol.getName());
                 continue;
             }
             return f;
         }
     }
 
     @Subroutine("subr-arity")
     public static LObject subrArity (LObject object) {
         if (subrp(object).equals(LispSymbol.ourNil))
             throw new WrongTypeArgumentException("subrp",
                     object instanceof LispSymbol ? ((LispSymbol) object).getName() : object.toString());
         Primitive subr = (Primitive)object;
         return LispList.cons(subr.getMinNumArgs(), subr.getMaxNumArgs());
     }
 
     @Subroutine("aref")
     public static LObject aRef (LObject array, LispInteger index) {
         try {
             if (array instanceof LispVector) {
                 return ((LispVector) array).get(index.getData());
             }
             if (array instanceof LispString) {
                 return new LispInteger(((LispString) array).getData().charAt(index.getData()));
             }
             //todo: char-table, bool-vector
             throw new WrongTypeArgumentException("arrayp", array.toString());
         } catch (IndexOutOfBoundsException e) {
             throw new ArgumentOutOfRange(array.toString(), index.toString());
         }
     }
 
     @Subroutine(value = "apply")
     public static LObject apply (Environment environment, LObject function, LObject... args) {
         if (!(function instanceof LispSymbol) || !((LispSymbol) function).isFunction()
                 || (!((LispSymbol) function).isCustom() && !((LispSymbol) function).isBuiltIn()))
             throw new InvalidFunctionException((function instanceof LispSymbol ?
                     ((LispSymbol) function).getName() : function.toString()));
 
         if (!(args[args.length-1] instanceof LispList) && args[args.length-1] != LispSymbol.ourNil)
             throw new WrongTypeArgumentException("listp", args[args.length-1].toString());
         ArrayList<LObject> list = new ArrayList<>();
         list.addAll(Arrays.asList(args).subList(0, args.length - 1));
 
         if (!args[args.length-1].equals(LispSymbol.ourNil)) {
             List<LObject> last = ((LispList)args[args.length-1]).toLObjectList();
             list.addAll(last);
         }
         environment.setArgumentsEvaluated(true);
         return ((LispSymbol) function).evaluateFunction(environment, list);
     }
 
     @Subroutine(value = "purecopy")
     public static LObject pureCopy (LObject object) {
         /*
         TODO: ?
          */
         return object;
     }
 
     @Subroutine(value = "eval")
     public static LObject evaluate (Environment environment, LObject object) {
         return object.evaluate(environment);
     }
 
     /* private static boolean checkFunction (Environment environment, LObject object) {
        CustomEnvironment inner = new CustomEnvironment(environment);
        inner.setArgumentsEvaluated(true);
        LispList list = LispList.list(new LispSymbol("functionp"), object);
        LObject result = list.evaluate(inner);
        return true;
    } */
 
     @Subroutine("defalias")
     public static LObject defineAlias (Environment environment, LispSymbol symbol, LObject functionDefinition, @Optional LObject docString) {
         LispSymbol real = GlobalEnvironment.INSTANCE.find(symbol.getName());
         if (real == null)
             real = new LispSymbol(symbol.getName());
         real.setFunction(functionDefinition);
         if (docString != null && !(docString instanceof LispNumber)) {
             real.setFunctionDocumentation(docString, environment);
         }
         GlobalEnvironment.INSTANCE.defineSymbol(real);
         return functionDefinition;
     }
 
     @Subroutine("provide")
     public static LispSymbol provide (LispSymbol feature, @Optional LObject subFeatures) {
         //todo: implement
         return feature;
     }
 
     @Subroutine("atom")
     public static LispSymbol atom (LObject object) {
         return LispSymbol.bool(!(object instanceof LispList));
     }
     
     @Subroutine("throw")
     public static void lispThrow (LObject tag, LObject value) {
         throw new LispThrow(tag, value);
     }
     
     @Subroutine("identity")
     public static LObject identity (LObject arg) {
         return arg;
     }
     
     @Subroutine("match-data")
     public static LObject matchData(@Optional LObject integers, LObject reuse, LObject reseat) {
         //todo :)
         return LispSymbol.ourNil;
     }
 }
