 package d;
 
 import static d.Opcodes.*;
 
 import util.StringUtils;
 
 import common.*;
 import d.nat.*;
 import d.ty.ConcreteType;
 import d.ty.desc.TypeDesc;
 import d.ty.nf.NonFinalType;
 
 public class BytecodeMethodDef extends ConcreteMethodDef {
     public final String[] stringTable;
 
     private final int numLocals; // includes "this"
     private final int[] code;
 
     public BytecodeMethodDef(RawMethod desc,
             RawType[] typeDescTable,
             TypeDesc[] fullTypeDescTable,
             RawMethod[] methodDescTable,
             String[] stringTable,
             int numLocals, int[] bytecode) {
         super(desc, typeDescTable, fullTypeDescTable, methodDescTable);
 
         for (int i = 0; i < stringTable.length; ++i)
             stringTable[i] = stringTable[i].intern();
         this.stringTable = stringTable;
 
         this.numLocals = numLocals;
         this.code = bytecode;
     }
 
     @Override
     public void invoke(BaseObject[] stack, int bp, ConcreteType[] genericArgs) {
         int sp = bp + numLocals, ip = 0, op;
 
         try {
             for (;;) {
                 try {
                     op = code[ip++];
                 } catch (ArrayIndexOutOfBoundsException e) {
                     throw new NiftyException("Execution ran off the end of %s without returning.", desc);
                 }
 
                 switch (op) {
                     case POP:
                         --sp;
                         break;
 
                     case DUP: {
                         BaseObject object = stack[sp];
                         stack[++sp] = object;
                         break;
                     }
 
                     case CONST_INT:
                         stack[++sp] = new NativeInt(code[ip++]);
                         break;
 
                     case CONST_CHAR:
                         stack[++sp] = new NativeChar((char) code[ip++]);
                         break;
 
                     case CONST_TRUE:
                         stack[++sp] = NativeBool.TRUE;
                         break;
 
                     case CONST_FALSE:
                         stack[++sp] = NativeBool.FALSE;
                         break;
 
                     case CONST_STRING:
                        stack[++sp] = VMUtils.makeString(stringTable[code[ip++]]);
                         break;
 
                     case CREATE_SEQ: {
                         int typeTableIndex = code[ip++];
                         int len = code[ip++];
 
                         BaseObject me = desc.isStatic ? null : stack[bp + 1]; // "this"
                         assert fullTypeTable[typeTableIndex] != null;
                         ConcreteType elemType = fullTypeTable[typeTableIndex].toConcrete(me, genericArgs);
 
                         BaseObject[] contents = new BaseObject[len];
                         for (int i = 1; i <= len; ++i)
                             contents[len - i] = stack[sp--];
                         ConcreteType arrayType = new ConcreteType(
                                 NativeMutableArray.TYPE,
                                 new ConcreteType[] {elemType});
                         stack[++sp] = new NativeMutableArray(arrayType, contents);
                         break;
                     }
 
                     case GET_LOCAL: {
                         int localIndex = code[ip++];
                         stack[++sp] = stack[bp + localIndex + 1];
                         break;
                     }
 
                     case PUT_LOCAL: {
                         int localIndex = code[ip++];
                         stack[bp + localIndex + 1] = stack[sp--];
                         break;
                     }
 
                     case GET_STATIC_FIELD: {
                         int typeTableIndex = code[ip++];
                         int staticFieldIndex = code[ip++];
                         assert rawTypeTable.length > typeTableIndex :
                             String.format("Raw type table index %d is out of bounds; table size is %d.",
                                     typeTableIndex, rawTypeTable.length);
                         assert sp + 1 >= 0 : "Stack pointer is " + sp + ".";
                         stack[++sp] = rawTypeTable[typeTableIndex].staticFields[staticFieldIndex];
                         break;
                     }
 
                     case PUT_STATIC_FIELD: {
                         BaseObject newValue = stack[sp--];
                         int typeTableIndex = code[ip++];
                         int staticFieldIndex = code[ip++];
                         rawTypeTable[typeTableIndex].staticFields[staticFieldIndex] = newValue;
                         break;
                     }
 
                     case GET_INSTANCE_FIELD: {
                         int stringTableIndex = code[ip++];
                         String fieldName = stringTable[stringTableIndex];
                         assert fieldName.intern() == fieldName : "String in literal pool wasn't interned.";
 
                         BaseObject target = stack[sp--];
                         int fieldIndex = target.type.rawType.virtualFieldTable.get(fieldName);
                         stack[++sp] = target.fields[fieldIndex];
                         break;
                     }
 
                     case PUT_INSTANCE_FIELD: {
                         int stringTableIndex = code[ip++];
                         String fieldName = stringTable[stringTableIndex];
                         assert fieldName.intern() == fieldName : "String in literal pool wasn't interned.";
 
                         BaseObject target = stack[sp--];
                         BaseObject newValue = stack[sp--];
                         int fieldIndex = target.type.rawType.virtualFieldTable.get(fieldName);
                         target.fields[fieldIndex] = newValue;
                         break;
                     }
 
                     case INVOKE_STATIC: {
                         int methodTableIndex = code[ip++];
                         ConcreteMethodDef m = (ConcreteMethodDef) methodTable[methodTableIndex];
                         assert m != null : this.desc + " does not appear to have been linked.";
                         BaseObject a = desc.isStatic ? null : stack[bp + 1]; // current object
 
                         // Create array of generic arguments.
                         int numGenericArgs = m.desc.numGenericParams;
                         ConcreteType[] newGenericArgs;
                         if (numGenericArgs == 0)
                             // We don't want to allocate an empty array, that's wasteful.
                             newGenericArgs = null;
                         else {
                             newGenericArgs = new ConcreteType[numGenericArgs];
                             for (int j = 0; j < numGenericArgs; ++j)
                                 newGenericArgs[j] = fullTypeTable[code[ip++]].toConcrete(a, genericArgs);
                         }
 
                         // Invoke the method.
                         int numArgs = m.desc.paramTypes.length;
                         if (!m.desc.isStatic)
                             ++numArgs;
                         m.invoke(stack, sp - numArgs, newGenericArgs);
                         sp -= numArgs - 1;
                         break;
                     }
 
                     case INVOKE_VIRTUAL: {
                         int methodTableIndex = code[ip++]; // index into method table
                         MethodDef m = methodTable[methodTableIndex];
                         assert !m.desc.isStatic : "INVOKE_VIRTUAL was used with a static method.";
                         BaseObject a = desc.isStatic ? null : stack[bp + 1]; // current object
 
                         // Create array of generic arguments.
                         int numGenericArgs = m.desc.numGenericParams;
                         ConcreteType[] newGenericArgs;
                         if (numGenericArgs == 0)
                             // We don't want to allocate an empty array, that's wasteful.
                             newGenericArgs = null;
                         else {
                             newGenericArgs = new ConcreteType[numGenericArgs];
                             for (int j = 0; j < numGenericArgs; ++j)
                                 newGenericArgs[j] = fullTypeTable[code[ip++]].toConcrete(a, genericArgs);
                         }
 
                         int numArgs = m.desc.paramTypes.length;
                         a = stack[sp - numArgs]; // target
                         TypeDef targetOwner = a.type.rawType;
                         ConcreteMethodDef impl = targetOwner.virtualMethodTable.get(m);
                         assert impl != null : String.format(
                                 "No implementation for '%s' found in virtual method table of %s.",
                                 m.desc, targetOwner.desc);
                         impl.invoke(stack, sp - numArgs - 1, newGenericArgs);
                        sp -= numArgs;
                         break;
                     }
 
                     case NEW: {
                         int typeTableIndex = code[ip++];
                         BaseObject me = desc.isStatic ? null : stack[bp + 1]; // "this"
                         assert fullTypeTable[typeTableIndex] != null;
                         stack[++sp] = fullTypeTable[typeTableIndex].toConcrete(me, genericArgs).rawInstance();
                         break;
                     }
 
                     case JUMP: {
                         int offset = code[ip++];
                         ip += offset;
                         break;
                     }
 
                     case JUMP_COND: {
                         int offset = code[ip++];
                         if (((NativeBool) stack[sp--]).value)
                             ip += offset;
                         break;
                     }
 
                     case BOOL_NEG:
                         assert stack[sp] instanceof NativeBool : "Trying to negate " + stack[sp] + ".";
                         stack[sp] = new NativeBool(!((NativeBool) stack[sp]).value);
                         break;
 
                     case RETURN:
                         // TODO: this copy shouldn't be necessary if the INVOKE_s accounted for
                         // numLocals of the invoked method when reading the return value
                         stack[bp + 1] = stack[sp];
                         return;
 
                     default:
                         throw new NiftyException("bad opcode in %s: %d", desc, op);
                 }
             }
         } catch (ArrayIndexOutOfBoundsException e) {
             throw new NiftyException(e, "Possible stack overflow in method '%s'.", desc);
         } catch (Throwable e) {
             throw new NiftyException(e, "Error in method '%s'.", desc);
         }
     }
 
     @Override
     public String toString() {
         StringBuilder sb = new StringBuilder();
         sb.append(desc).append(" {\n");
 
         // Append raw type [desc] table.
         if (rawTypeDescTable == null) {
             sb.append("    Raw type table:\n");
             for (TypeDef typeDef : rawTypeTable)
                 sb.append(StringUtils.indent(typeDef.desc, 2)).append('\n');
         } else {
             sb.append("    Raw type desc table:\n");
             for (RawType typeDesc : rawTypeDescTable)
                 sb.append(StringUtils.indent(typeDesc, 2)).append('\n');
         }
 
         // Append full type [desc] table.
         if (fullTypeDescTable == null) {
             sb.append("    Full type table:\n");
             for (NonFinalType type : fullTypeTable)
                 sb.append(StringUtils.indent(type, 2)).append('\n');
         } else {
             sb.append("    Full type desc table:\n");
             for (TypeDesc typeDesc : fullTypeDescTable)
                 sb.append(StringUtils.indent(typeDesc, 2)).append('\n');
         }
 
         // Append method [desc] table.
         if (methodDescTable == null) {
             sb.append("    Method table:\n");
             for (MethodDef method : methodTable)
                 sb.append(StringUtils.indent(method.desc, 2)).append('\n');
         } else {
             sb.append("    Method desc table:\n");
             for (RawMethod methodDesc : methodDescTable)
                 sb.append(StringUtils.indent(methodDesc, 2)).append('\n');
         }
 
         // Append method body.
         String body;
         try {
             body = Opcodes.repr(code, rawTypeDescTable, fullTypeDescTable, methodDescTable, stringTable);
         } catch (RuntimeException e) {
             throw new NiftyException(e, "Problem representing code of method '%s'.", desc);
         }
         body = StringUtils.indent(body);
         if (!body.isEmpty())
             body = "\n" + body + "\n";
         sb.append(body);
 
         return sb.append('}').toString();
     }
 }
