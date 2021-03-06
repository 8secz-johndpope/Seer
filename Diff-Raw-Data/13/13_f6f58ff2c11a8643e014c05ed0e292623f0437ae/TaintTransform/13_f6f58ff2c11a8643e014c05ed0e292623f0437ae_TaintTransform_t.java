 package uk.ac.cam.db538.dexter.transform.taint;
 
 import java.io.IOException;
 import java.security.cert.Certificate;
 import java.security.cert.CertificateEncodingException;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Set;
 
 import lombok.val;
 
 import org.jf.dexlib.AnnotationVisibility;
 import org.jf.dexlib.Util.AccessFlags;
 
 import uk.ac.cam.db538.dexter.apk.Manifest;
 import uk.ac.cam.db538.dexter.apk.SignatureFile;
 import uk.ac.cam.db538.dexter.dex.Dex;
 import uk.ac.cam.db538.dexter.dex.DexAnnotation;
 import uk.ac.cam.db538.dexter.dex.DexClass;
 import uk.ac.cam.db538.dexter.dex.DexUtils;
 import uk.ac.cam.db538.dexter.dex.code.DexCode;
 import uk.ac.cam.db538.dexter.dex.code.DexCode.Parameter;
 import uk.ac.cam.db538.dexter.dex.code.InstructionList;
 import uk.ac.cam.db538.dexter.dex.code.elem.DexCodeElement;
 import uk.ac.cam.db538.dexter.dex.code.elem.DexLabel;
 import uk.ac.cam.db538.dexter.dex.code.elem.DexTryStart;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_ArrayGet;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_ArrayLength;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_ArrayPut;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_BinaryOp;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_BinaryOpLiteral;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_CheckCast;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_Compare;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_Const;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_ConstClass;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_ConstString;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_Convert;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_FillArrayData;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_FilledNewArray;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_Goto;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_IfTest;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_IfTestZero;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_InstanceGet;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_InstanceOf;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_InstancePut;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_Invoke;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_Monitor;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_Move;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_MoveException;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_MoveResult;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_NewArray;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_NewInstance;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_Return;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_ReturnVoid;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_StaticGet;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_StaticPut;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_Switch;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_Throw;
 import uk.ac.cam.db538.dexter.dex.code.insn.DexInstruction_UnaryOp;
 import uk.ac.cam.db538.dexter.dex.code.insn.Opcode_GetPut;
 import uk.ac.cam.db538.dexter.dex.code.macro.DexMacro;
 import uk.ac.cam.db538.dexter.dex.code.reg.DexRegister;
 import uk.ac.cam.db538.dexter.dex.code.reg.DexSingleAuxiliaryRegister;
 import uk.ac.cam.db538.dexter.dex.code.reg.DexSingleRegister;
 import uk.ac.cam.db538.dexter.dex.code.reg.DexTaintRegister;
 import uk.ac.cam.db538.dexter.dex.code.reg.RegisterType;
 import uk.ac.cam.db538.dexter.dex.field.DexInstanceField;
 import uk.ac.cam.db538.dexter.dex.field.DexStaticField;
 import uk.ac.cam.db538.dexter.dex.method.DexMethod;
 import uk.ac.cam.db538.dexter.dex.type.DexArrayType;
 import uk.ac.cam.db538.dexter.dex.type.DexClassType;
 import uk.ac.cam.db538.dexter.dex.type.DexFieldId;
 import uk.ac.cam.db538.dexter.dex.type.DexMethodId;
 import uk.ac.cam.db538.dexter.dex.type.DexPrimitiveType;
 import uk.ac.cam.db538.dexter.dex.type.DexPrototype;
 import uk.ac.cam.db538.dexter.dex.type.DexReferenceType;
 import uk.ac.cam.db538.dexter.dex.type.DexRegisterType;
 import uk.ac.cam.db538.dexter.dex.type.DexType;
 import uk.ac.cam.db538.dexter.dex.type.DexTypeCache;
 import uk.ac.cam.db538.dexter.hierarchy.BaseClassDefinition;
 import uk.ac.cam.db538.dexter.hierarchy.BaseClassDefinition.CallDestinationType;
 import uk.ac.cam.db538.dexter.hierarchy.ClassDefinition;
 import uk.ac.cam.db538.dexter.hierarchy.InstanceFieldDefinition;
 import uk.ac.cam.db538.dexter.hierarchy.InterfaceDefinition;
 import uk.ac.cam.db538.dexter.hierarchy.MethodDefinition;
 import uk.ac.cam.db538.dexter.hierarchy.RuntimeHierarchy;
 import uk.ac.cam.db538.dexter.hierarchy.RuntimeHierarchy.TypeClassification;
 import uk.ac.cam.db538.dexter.hierarchy.StaticFieldDefinition;
 import uk.ac.cam.db538.dexter.transform.FilledArray;
 import uk.ac.cam.db538.dexter.transform.InvokeClassifier;
 import uk.ac.cam.db538.dexter.transform.MethodCall;
 import uk.ac.cam.db538.dexter.transform.Transform;
 import uk.ac.cam.db538.dexter.transform.TryBlockSplitter;
 import uk.ac.cam.db538.dexter.transform.taint.sourcesink.LeakageAlert;
 import uk.ac.cam.db538.dexter.transform.taint.sourcesink.SourceSinkDefinition;
 import uk.ac.cam.db538.dexter.utils.Pair;
 import uk.ac.cam.db538.dexter.utils.Utils;
 import uk.ac.cam.db538.dexter.utils.Utils.NameAcceptor;
 
 import com.rx201.dx.translator.DexCodeAnalyzer;
 import com.rx201.dx.translator.RopType;
 import com.rx201.dx.translator.RopType.Category;
 import com.rx201.dx.translator.TypeSolver;
 
 public class TaintTransform extends Transform {
 
 	protected Dex dex;
     protected CodeGenerator codeGen;
     protected AuxiliaryDex dexAux;
     protected RuntimeHierarchy hierarchy;
     private DexTypeCache typeCache;
     private boolean hasSignatureFile;
 
     private Map<DexInstanceField, DexInstanceField> taintInstanceFields;
     private Map<StaticFieldDefinition, DexStaticField> taintStaticFields;
     
     /**
      * Perform necessary global transforms all together that are prerequisite of per class/method transform   
      */
     @Override
     public void prepare(Dex dex) {
         super.prepare(dex);
         
         this.dex = dex;
         dexAux = dex.getAuxiliaryDex();
         codeGen = new CodeGenerator(dexAux);
         hierarchy = dexAux.getHierarchy();
         typeCache = hierarchy.getTypeCache();
         hasSignatureFile = dex.getSignatureFile() != null;
 
         taintInstanceFields = new HashMap<DexInstanceField, DexInstanceField>();
         taintStaticFields = new HashMap<StaticFieldDefinition, DexStaticField>();
         
         createTaintFields();
         mergeAuxDex();
         doManifest();
         doSignatureFile();
     }
     
     private boolean isStaticTaintFieldsClass(DexClass clazz) {
     	return clazz.equals(this.dexAux.getType_StaticTaintFields());
     }
     
     @Override
 	public boolean handleLast(DexClass cls) {
 		return isStaticTaintFieldsClass(cls);
 	}
 
 	private DexCodeAnalyzer codeAnalysis;
     private Map<MethodCall, CallDestinationType> invokeClassification;
     private Set<DexCodeElement> noninstrumentableElements;
     private TemplateBuilder builder;
 
 	private void doManifest() {
 		Manifest manifest = dex.getManifest();
     	if (manifest == null)
     		return;
     	
     	String descAppClass = getAppClass(manifest);
     	
     	DexClass replacementAppClass = dexAux.getType_DexterApplication();
     	BaseClassDefinition defReplacementAppClass = replacementAppClass.getClassDef();
     	setAppClass(replacementAppClass, manifest);
 
     	if (descAppClass != null) {
     		DexClassType typeOriginalAppClass = DexClassType.parse(descAppClass, typeCache);
     		BaseClassDefinition defOriginalAppClass = hierarchy.getClassDefinition(typeOriginalAppClass);
     		BaseClassDefinition defAndroidAppClass = defReplacementAppClass.getSuperclass();
     		
     		assert defOriginalAppClass.isChildOf(defAndroidAppClass);
     		
     		/*
     		 * defOriginalAppClass could not be a DIRECT descendant of Application,
     		 * and so we might need to climb up the hierarchy tree
     		 */
     		while (!defOriginalAppClass.getSuperclass().equals(defAndroidAppClass))
     			defOriginalAppClass = defOriginalAppClass.getSuperclass();
     		
     		assert defOriginalAppClass.isInternal();
     		
     		// insert DexterApplication into the parent chain of the app class
     		defOriginalAppClass.setSuperclass(defReplacementAppClass);
     	}
 	}
     
 	public void doSignatureFile() {
     	SignatureFile signatureFile = dex.getSignatureFile();
     	if (signatureFile == null)
     		return;
     	
     	Certificate[] certs = signatureFile.getSignatures();
     	List<DexCodeElement> insns = new ArrayList<DexCodeElement>();
     	
     	// set package name
     	DexSingleRegister auxPackageName = codeGen.auxReg();
     	insns.add(codeGen.constant(auxPackageName, dex.getManifest().getPackageName()));
     	insns.add(codeGen.sput(auxPackageName, dexAux.getField_FakeSignature_PackageName().getFieldDef()));
 
     	// create signatures array
     	DexSingleRegister auxSignatureArray = codeGen.auxReg();
     	DexSingleRegister auxSignatureArrayLength = codeGen.auxReg();
     	insns.add(codeGen.constant(auxSignatureArrayLength, certs.length));
     	insns.add(codeGen.newSignatureArray(auxSignatureArray, auxSignatureArrayLength));
     	insns.add(codeGen.sput(auxSignatureArray, dexAux.getField_FakeSignature_Signatures().getFieldDef()));
     	
     	// set signatures
     	DexSingleRegister auxSignatureData = codeGen.auxReg();
     	DexSingleRegister auxSignatureLength = codeGen.auxReg();
     	DexSingleRegister auxSignatureObject = codeGen.auxReg();
     	DexSingleRegister auxSignatureIndex = codeGen.auxReg();
     	for (int i = 0; i < certs.length; ++i) {
     		byte[] certData;
 			try {
 				certData = certs[i].getEncoded();
 			} catch (CertificateEncodingException e) {
 				throw new RuntimeException("Problem reading a signature", e); 
 			}
 			
 			// convert data to a list
 			List<byte[]> convData = new ArrayList<byte[]>(certData.length);
 			for (byte b : certData)
 				convData.add(new byte[] { b });
     		
 			// create byte[] with data
     		insns.add(codeGen.constant(auxSignatureLength, convData.size()));
 	    	insns.add(new DexInstruction_NewArray(auxSignatureData, auxSignatureLength, DexArrayType.parse("[B", typeCache), hierarchy));
 	    	insns.add(new DexInstruction_FillArrayData(auxSignatureData, convData, hierarchy));
 	    	
 	    	// create Signature
 	    	insns.add(codeGen.newSignature(auxSignatureObject, auxSignatureData));
 	    	
 	    	// store it in the array
 	    	insns.add(codeGen.constant(auxSignatureIndex, i));
 	    	insns.add(codeGen.aput(auxSignatureObject, auxSignatureArray, auxSignatureIndex, Opcode_GetPut.Object));
     	}
     	
     	insns.add(codeGen.retrn());
     	
     	// replace the method
     	DexMethod oldMethod = dexAux.getMethod_FakeSignature_Clinit();
     	DexCode code = new DexCode(oldMethod.getMethodBody(), new InstructionList(insns));
     	DexMethod newMethod = new DexMethod(oldMethod, code);
     	dexAux.getType_FakeSignature().replaceMethod(oldMethod, newMethod);
     }
     
     private String getAppClass(Manifest manifest) {
    	String appClass = manifest.getApplicationClass();
    	if (appClass == null)
    		return null;
    	else
    		return DexType.jvm2dalvik(appClass);
     }
     
     private void setAppClass(DexClass clazz, Manifest manifest) {
 		manifest.setApplicationClass(clazz);    	
     }
     
 	@Override
     public DexCode doFirst(DexCode code, DexMethod method) {
         code = super.doFirst(code, method);
 
         codeGen.resetAsmIds(); // purely for esthetic reasons (each method will start with a0)
 
         codeAnalysis = new DexCodeAnalyzer(code);
         codeAnalysis.analyze();
         
         code = InvokeClassifier.collapseCalls(code);
         val classification = InvokeClassifier.classifyMethodCalls(code, codeAnalysis, codeGen);
         
         code = classification.getValA();
         invokeClassification = classification.getValB();
         noninstrumentableElements = classification.getValC();
         
         builder = new TemplateBuilder(code, codeAnalysis, codeGen);
 
         return code;
     }
 
     @Override
     public DexCodeElement doFirst(DexCodeElement element, DexCode code, DexMethod method) {
         element = super.doFirst(element, code, method);
 
         // code elements (markers etc.) should be left alone
         if (!(element instanceof DexInstruction) && !(element instanceof MethodCall) && !(element instanceof FilledArray))
             return element;
 
         // instructions added in preparation stage should be skipped over
         if (noninstrumentableElements.contains(element))
             return element;
 
         if (element instanceof DexInstruction_Const)
             return instrument_Const((DexInstruction_Const) element);
 
         if (element instanceof DexInstruction_ConstString)
             return instrument_ConstString((DexInstruction_ConstString) element);
 
         if (element instanceof DexInstruction_ConstClass)
             return instrument_ConstClass((DexInstruction_ConstClass) element);
 
         if (element instanceof MethodCall) {
             CallDestinationType type = invokeClassification.get(element);
             if (type == CallDestinationType.Internal)
                 return instrument_MethodCall_Internal((MethodCall) element, code, method);
             else if (type == CallDestinationType.External)
                 return instrument_MethodCall_External((MethodCall) element, code, method);
             else
                 throw new Error("Calls should never be classified as undecidable by this point");
         }
 
         if (element instanceof DexInstruction_Invoke ||
         		element instanceof DexInstruction_FilledNewArray ||
                 element instanceof DexInstruction_MoveResult)
             throw new Error("All method calls and filled-arrays should be collapsed at this point");
 
         if (element instanceof DexInstruction_Return)
             return instrument_Return((DexInstruction_Return) element);
 
         if (element instanceof DexInstruction_Move)
             return instrument_Move((DexInstruction_Move) element, code);
 
         if (element instanceof DexInstruction_BinaryOp)
             return instrument_BinaryOp((DexInstruction_BinaryOp) element);
 
         if (element instanceof DexInstruction_BinaryOpLiteral)
             return instrument_BinaryOpLiteral((DexInstruction_BinaryOpLiteral) element);
 
         if (element instanceof DexInstruction_Compare)
             return instrument_Compare((DexInstruction_Compare) element);
 
         if (element instanceof DexInstruction_Convert)
             return instrument_Convert((DexInstruction_Convert) element);
 
         if (element instanceof DexInstruction_UnaryOp)
             return instrument_UnaryOp((DexInstruction_UnaryOp) element);
 
         if (element instanceof DexInstruction_NewInstance)
             return instrument_NewInstance((DexInstruction_NewInstance) element);
 
         if (element instanceof DexInstruction_NewArray)
             return instrument_NewArray((DexInstruction_NewArray) element);
 
         if (element instanceof DexInstruction_CheckCast)
             return instrument_CheckCast((DexInstruction_CheckCast) element, code);
 
         if (element instanceof DexInstruction_InstanceOf)
             return instrument_InstanceOf((DexInstruction_InstanceOf) element);
 
         if (element instanceof DexInstruction_ArrayLength)
             return instrument_ArrayLength((DexInstruction_ArrayLength) element, code);
 
         if (element instanceof DexInstruction_ArrayPut)
             return instrument_ArrayPut((DexInstruction_ArrayPut) element, code);
 
         if (element instanceof DexInstruction_ArrayGet)
             return instrument_ArrayGet((DexInstruction_ArrayGet) element, code);
 
         if (element instanceof DexInstruction_InstancePut)
             return instrument_InstancePut((DexInstruction_InstancePut) element, code);
 
         if (element instanceof DexInstruction_InstanceGet)
             return instrument_InstanceGet((DexInstruction_InstanceGet) element, code);
 
         if (element instanceof DexInstruction_StaticPut)
             return instrument_StaticPut((DexInstruction_StaticPut) element);
 
         if (element instanceof DexInstruction_StaticGet)
             return instrument_StaticGet((DexInstruction_StaticGet) element);
 
         if (element instanceof DexInstruction_MoveException)
             return instrument_MoveException((DexInstruction_MoveException) element);
 
         if (element instanceof DexInstruction_FillArrayData)
             return instrument_FillArrayData((DexInstruction_FillArrayData) element, code);
 
         if (element instanceof FilledArray)
             return instrument_FilledArray((FilledArray) element, code);
 
         if (element instanceof DexInstruction_Monitor)
             return instrument_Monitor((DexInstruction_Monitor) element, code);
         
         if (element instanceof DexInstruction_Throw)
             return instrument_Throw((DexInstruction_Throw) element, code);
         
         // instructions that do not require instrumentation
         if (element instanceof DexInstruction_Goto ||
                 element instanceof DexInstruction_IfTest ||
                 element instanceof DexInstruction_IfTestZero ||
                 element instanceof DexInstruction_Switch ||
                 element instanceof DexInstruction_ReturnVoid)
             return element;
 
         throw new UnsupportedOperationException("Unhandled code element " + element.getClass().getSimpleName());
     }
     
     @Override
 	public DexCodeElement doLast(DexCodeElement element, DexCode code, DexMethod method) {
     	codeGen.reuseAuxRegs();
 		return super.doLast(element, code, method);
 	}
 
 	@Override
     public DexCode doLast(DexCode code, DexMethod method) {
 
 		code = insertInstanceFieldInit(code, method);
         code = TryBlockSplitter.checkAndFixTryBlocks(code);
         code = InvokeClassifier.expandCalls(code);
         code = insertTaintInit(code, method);
         
         invokeClassification = null;
         noninstrumentableElements = null;
         codeAnalysis = null;
         
         return super.doLast(code, method);
     }
 
     @Override
     public DexMethod doLast(DexMethod method) {
         method = super.doLast(method);
         if (method.getMethodBody() != null) {
             DexAnnotation anno = new DexAnnotation(dexAux.getAnno_InternalMethod().getType(), AnnotationVisibility.RUNTIME);
             method = new DexMethod(method, anno);
         }
         
         return method;
     }
 
     @Override
     public void doLast(DexClass clazz) {
         // implement the InternalDataStructure interface
         if (!clazz.getClassDef().isInterface()) {
             clazz.getClassDef().addImplementedInterface((InterfaceDefinition) dexAux.getType_InternalStructure().getClassDef());
             generateGetTaint(clazz);
             generateSetTaint(clazz);
         }
         
         // insert static taint field initialization into <clinit>
         createEmptyClinit(clazz);
         insertStaticFieldInit(clazz);
         
         super.doLast(clazz);
     }
 
     public LeakageAlert getLeakageAlert() {
     	return new LeakageAlert() {
 			@Override
 			public DexCodeElement generate(DexSingleRegister regTaint, String leakType, CodeGenerator codeGen) {
 				return codeGen.logLeakage(regTaint, leakType);
 			}
 		};
     }
     
     private void createEmptyClinit(DexClass clazz) {
     	if (getClinit(clazz) != null)
     		return;
 
     	// generate bytecode
     	
     	DexCode methodBody = new DexCode(
 			new InstructionList(codeGen.retrn()), 
 			null,
 			getClinitId().getPrototype().getReturnType(),
 			true,
 			hierarchy);
     	
     	// add to the hierarchy
     	
     	BaseClassDefinition classDef = clazz.getClassDef();
     	MethodDefinition methodDef = new MethodDefinition(
 			classDef,
 			getClinitId(),
 			DexUtils.assembleAccessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.CONSTRUCTOR));
     	classDef.addDeclaredMethod(methodDef);
 		
     	// add to the class
     	
     	clazz.addMethod(new DexMethod(
 			clazz,
 			methodDef,
 			methodBody));
     	
     	assert getClinit(clazz) != null;
     }
 
     private DexCode insertTaintInit(DexCode code, DexMethod method) {
         // If there are no parameters, no point in initializing them
         if (code.getParameters().isEmpty())
             return code;
 
         DexSingleRegister regInternalCallFlag = codeGen.auxReg();
         DexLabel labelExternal = codeGen.label();
         DexLabel labelEnd = codeGen.label();
 
         DexMacro init = new DexMacro(
         		codeGen.isInternalCall(regInternalCallFlag),
                 codeGen.ifZero(regInternalCallFlag, labelExternal),
                 // INTERNAL ORIGIN
                 codeGen.initTaints(code, true),
                 codeGen.jump(labelEnd),
                 labelExternal,
                 // EXTERNAL ORIGIN
                 codeGen.initTaints(code, false),
                 labelEnd);
 
         return new DexCode(code, new InstructionList(Utils.concat(init.getInstructions(), code.getInstructionList())));
     }
 
     private DexCodeElement instrument_Const(DexInstruction_Const insn) {
     	if (insn.getValue() == 0L) {
         	RopType type = codeAnalysis.reverseLookup(insn).getDefinedRegisterType(insn.getRegTo());
         	
         	if (type.category == Category.Unknown || type.category == Category.Conflicted)
         		throw new AssertionError("Cannot decide if zero value is NULL or not");
         	else if (type.category == Category.Null || type.category == Category.Reference) {
         		
         		DexReferenceType objType;
         		if (type.category == Category.Null)
         			objType = hierarchy.getRoot().getType(); // java.lang.Object
         		else
         			objType = type.type;
 
         		DexSingleRegister auxEmptyTaint = codeGen.auxReg();
         		DexSingleRegister regToRef = (DexSingleRegister) insn.getRegTo();
         		
         		return builder.create(
         				insn,
         				new DexMacro(
     						codeGen.setEmptyTaint(auxEmptyTaint),
     	        			codeGen.taintNull(regToRef, auxEmptyTaint, objType)));        						
         	}
     	}
     	
         return builder.create(
                    insn,
                    codeGen.setEmptyTaint(insn.getRegTo().getTaintRegister()));
     }
 
     private DexCodeElement instrument_ConstString(DexInstruction_ConstString insn) {
     	DexSingleRegister regEmptyTaint = codeGen.auxReg();
         return builder.create(
         		insn,
         		new DexMacro(
                    codeGen.setEmptyTaint(regEmptyTaint),
                    codeGen.taintCreate_External(insn.getRegTo(), regEmptyTaint)));
     }
 
     private DexCodeElement instrument_ConstClass(DexInstruction_ConstClass insn) {
     	DexSingleRegister regEmptyTaint = codeGen.auxReg();
         return builder.create(
         		insn,
         		new DexMacro(
                    codeGen.setEmptyTaint(regEmptyTaint),
                    codeGen.taintCreate_External(insn.getRegTo(), regEmptyTaint)));
     }
 
     private DexCodeElement instrument_MethodCall_Internal(MethodCall methodCall, DexCode code, DexMethod method) {
         DexInstruction_Invoke insnInvoke = methodCall.getInvoke();
         DexInstruction_MoveResult insnMoveResult = methodCall.getResult();
 
         DexPrototype prototype = insnInvoke.getMethodId().getPrototype();
         List<DexRegister> argRegisters = insnInvoke.getArgumentRegisters();
 
         boolean isStatic = insnInvoke.isStaticCall();
         
         // Need to store taints in the ThreadLocal ARGS array ?
 
         DexCodeElement macroSetParamTaints;
         if (!argRegisters.isEmpty())
             macroSetParamTaints = new DexMacro(
         		codeGen.setParamTaints(argRegisters, prototype, insnInvoke.getClassType(), isStatic),
         		codeGen.setInternalCallFlag());
         else
             macroSetParamTaints = codeGen.empty();
         
         DexCodeElement macroHandleResult;
 
         // Need to retrieve taint from the ThreadLocal RES field ?
 
         if (methodCall.movesResult()) {
         	
         	DexRegisterType resType = (DexRegisterType) prototype.getReturnType();
         	DexRegister regRes = insnMoveResult.getRegTo();
         	DexTaintRegister regResTaint = regRes.getTaintRegister();
         	
         	if (resType instanceof DexPrimitiveType)
         		macroHandleResult = codeGen.getResultPrimitiveTaint(regResTaint);
         	else if (resType instanceof DexReferenceType)
         		macroHandleResult = codeGen.getResultReferenceTaint(regResTaint, (DexReferenceType) codeGen.taintType(resType));
         	else
         		throw new Error();
         	
         	macroHandleResult = builder.nonthrowingTaintDefinition(
         			macroHandleResult, 
         			Arrays.asList(Pair.create(regRes, resType instanceof DexPrimitiveType)));
         } else
         	
             macroHandleResult = codeGen.empty();
         
         // generate instrumentation
         return new DexMacro(macroSetParamTaints, methodCall, macroHandleResult);
     }
     
     private DexCodeElement instrument_MethodCall_External(MethodCall methodCall, DexCode code, DexMethod method) {
         DexInstruction_Invoke insnInvoke = methodCall.getInvoke();
         DexInstruction_MoveResult insnMoveResult = methodCall.getResult();
 
         DexSingleAuxiliaryRegister regCombinedTaint = codeGen.auxReg();
         
         // Apply source/sink instrumentation
         
         SourceSinkDefinition sourceSinkDef = SourceSinkDefinition.findApplicableDefinition(methodCall, getLeakageAlert());
         DexCodeElement sourcesinkBefore, sourcesinkAfter, sourcesinkJustBefore, sourcesinkJustAfter;
         if (sourceSinkDef != null) {
         	System.out.println("Applying " + sourceSinkDef.getClass().getSimpleName() + " instrumentation");
         	
         	sourcesinkBefore = sourceSinkDef.insertBefore(codeGen);
         	sourcesinkAfter = sourceSinkDef.insertAfter(codeGen);
         	sourcesinkJustBefore = sourceSinkDef.insertJustBefore(regCombinedTaint, codeGen);
         	sourcesinkJustAfter = sourceSinkDef.insertJustAfter(regCombinedTaint, codeGen);
         } else {
         	sourcesinkBefore = codeGen.empty();
         	sourcesinkAfter = codeGen.empty();
         	sourcesinkJustBefore = codeGen.empty();
         	sourcesinkJustAfter = codeGen.empty();
         }
         
         DexCodeElement completeCall_BeforeResult;
         DexCodeElement completeCall_AfterResult;
         DexCodeElement completeCall_AfterException;
         
         if (isCallToSuperclassConstructor(insnInvoke, code, method)) {
 
             // Handle calls to external superclass constructor
         	
             assert(!methodCall.movesResult());
             DexSingleRegister regThis = (DexSingleRegister) insnInvoke.getArgumentRegisters().get(0);
             DexSingleRegister regThisTaint = regThis.getTaintRegister();
 
             completeCall_BeforeResult = new DexMacro(
             		   sourcesinkBefore,
                        codeGen.prepareExternalCall(regCombinedTaint, insnInvoke),
                        codeGen.taintSetPendingDefinition(regThisTaint, regCombinedTaint),
                        sourcesinkJustBefore);
             
             completeCall_AfterResult = new DexMacro(
             		
                        // At this point, the object reference is valid
                        // Need to generate new TaintInternal object with it
 
                        sourcesinkJustAfter,
                        codeGen.taintLookup_NoExtraTaint(null, regThis, TypeClassification.REF_INTERNAL),
                        sourcesinkAfter);
             
             completeCall_AfterException = builder.nonthrowingTaintDefinition(
             		   codeGen.taintErasePendingDefinition(),
             		   null);
 
         } else {
 
             // Standard external call
         	completeCall_BeforeResult = new DexMacro(
             		   sourcesinkBefore,
                        codeGen.prepareExternalCall(regCombinedTaint, insnInvoke),
                        sourcesinkJustBefore);
                        
             completeCall_AfterResult = new DexMacro(
                        sourcesinkJustAfter,
                        codeGen.finishExternalCall(regCombinedTaint, insnInvoke, insnMoveResult),
                        sourcesinkAfter);
 
             completeCall_AfterException = codeGen.empty();
         }
 
         // ensure non-throwing semantics of code after result
         if (completeCall_AfterResult.canThrow()) {
         	List<Pair<DexRegister, Boolean>> defRegs;
         	if (methodCall.movesResult())
         		defRegs = Arrays.asList(Pair.create(insnMoveResult.getRegTo(), insnMoveResult.getType() != RegisterType.REFERENCE));
         	else
         		defRegs = null;
         		
     		completeCall_AfterResult = builder.nonthrowingTaintDefinition(
         			completeCall_AfterResult, 
         			defRegs);
         }
         
         return new DexMacro(
     		methodCall.expand_ReplaceInternals(
 				new DexMacro(
 					completeCall_BeforeResult,
 					builder.taintException(
 			    		methodCall.expand_JustInternals(),
 			    		completeCall_AfterException,
 			    		regCombinedTaint))),
     		completeCall_AfterResult);
     }
 
     private DexCodeElement instrument_Return(DexInstruction_Return insn) {
     	DexTaintRegister regFromTaint = insn.getRegFrom().getTaintRegister();
     	
         DexTryStart tryBlock = codeGen.tryBlock(codeGen.catchAll());
     	DexCodeElement setResultTaint;
         if (insn.getOpcode() == RegisterType.REFERENCE)
             setResultTaint = codeGen.setResultReferenceTaint(regFromTaint);
         else
             setResultTaint = codeGen.setResultPrimitiveTaint(regFromTaint);
         
         return new DexMacro(
         	tryBlock,
         	setResultTaint,
         	tryBlock.getEndMarker(),
         	tryBlock.getCatchAllHandler(),
         	insn);
     }
     
     private DexCodeElement instrument_Move(DexInstruction_Move insn, DexCode code) {
         if (insn.getType() == RegisterType.REFERENCE)
     		return builder.create(
                        insn,
                        codeGen.move_tobj((DexSingleRegister) insn.getRegTo(), (DexSingleRegister) insn.getRegFrom()));
         	
         else
     		return builder.create(
                        insn,
                        codeGen.move_prim(insn.getRegTo().getTaintRegister(), insn.getRegFrom().getTaintRegister()));
     }
 
     private DexCodeElement instrument_BinaryOp(DexInstruction_BinaryOp insn) {
 		return builder.create(
                         insn,
 						codeGen.combineTaint(insn.getRegTo(), insn.getRegArgA(), insn.getRegArgB()));
     }
 
     private DexCodeElement instrument_BinaryOpLiteral(DexInstruction_BinaryOpLiteral insn) {
 		return builder.create(
                 insn,
 				codeGen.combineTaint(insn.getRegTo(), insn.getRegArgA()));
     }
 
     private DexCodeElement instrument_Compare(DexInstruction_Compare insn) {
 		return builder.create(
                 insn,
 				codeGen.combineTaint(insn.getRegTo(), insn.getRegSourceA(), insn.getRegSourceB()));
     }
 
     private DexCodeElement instrument_Convert(DexInstruction_Convert insn) {
 		return builder.create(
                    insn,
                    codeGen.combineTaint(insn.getRegTo(), insn.getRegFrom()));
     }
 
     private DexCodeElement instrument_UnaryOp(DexInstruction_UnaryOp insn) {
 		return builder.create(
                 insn,
                 codeGen.combineTaint(insn.getRegTo(), insn.getRegFrom()));
     }
 
     private DexCodeElement instrument_NewInstance(DexInstruction_NewInstance insn) {
     	if (insn.getTypeDef().isInternal())
     		return builder.create(
         		insn,
         		codeGen.taintCreate_Internal_Undefined(insn.getRegTo().getTaintRegister()));
     	else {
     		DexSingleRegister auxType = codeGen.auxReg();
     		return builder.create(
             		insn,
             		new DexMacro(
         				codeGen.constant(auxType, insn.getTypeDef()),
         				codeGen.taintCreate_External_Undefined(insn.getRegTo().getTaintRegister(), auxType)));
     	}
     }
 
     private DexCodeElement instrument_NewArray(DexInstruction_NewArray insn) {
         DexSingleRegister regTo = insn.getRegTo();
         DexSingleRegister regSize = insn.getRegSize();
 
         DexSingleRegister auxSize;
         DexSingleRegister auxSizeTaint = regSize.getTaintRegister();
 
         // We need to be careful if the instruction overwrites the size register
 
         if (regTo.equals(regSize))
             auxSize = codeGen.auxReg();
         else
             auxSize = regSize;
 
         if (insn.getValue().getElementType() instanceof DexPrimitiveType)
             return new DexMacro(
                        codeGen.move_prim(auxSize, regSize),
                        builder.create(
                     		   insn,
                     		   codeGen.taintCreate_ArrayPrimitive(regTo, auxSize, auxSizeTaint)));
         else
             return new DexMacro(
                        codeGen.move_prim(auxSize, regSize),
                        builder.create(
                     		   insn,
                     		   codeGen.taintCreate_ArrayReference(regTo, auxSize, auxSizeTaint)));
     }
 
     private DexCodeElement instrument_CheckCast(DexInstruction_CheckCast insn, DexCode code) {
     	DexSingleRegister regObject = insn.getRegObject();
     	DexSingleRegister regObjectTaint = regObject.getTaintRegister();
     	
         return builder.create(
         		   insn,
         		   codeGen.taintCast(regObject, regObjectTaint, insn.getValue()));
     }
 
     private DexCodeElement instrument_InstanceOf(DexInstruction_InstanceOf insn) {
         return builder.create(
         			insn,
                     codeGen.getTaint(insn.getRegTo().getTaintRegister(), insn.getRegObject()));
     }
 
     private DexCodeElement instrument_ArrayLength(DexInstruction_ArrayLength insn, DexCode code) {
     	return builder.create(
 				   insn,
                    codeGen.getTaint_Array_Length(insn.getRegTo().getTaintRegister(), insn.getRegArray().getTaintRegister()));
     }
 
     private DexCodeElement instrument_ArrayPut(DexInstruction_ArrayPut insn, DexCode code) {
     	/*
     	 * TODO: needs to include the taint of the index as well?
     	 */
     	
         DexTaintRegister regFromTaint = insn.getRegFrom().getTaintRegister();
         DexTaintRegister regArrayTaint = insn.getRegArray().getTaintRegister();
 
         if (isNull(insn, insn.getRegArray()))
         	return builder.create(insn, codeGen.empty()); // leave uninstrumented
         
         else {
 	        if (insn.getOpcode() == Opcode_GetPut.Object)
 	            return builder.create(
 	            		   insn,
 	                       codeGen.setTaint_ArrayReference(regFromTaint, regArrayTaint, insn.getRegIndex()));
 	        else
 	            return builder.create(
 	            		   insn,
 	                       codeGen.setTaint_ArrayPrimitive(regFromTaint, regArrayTaint, insn.getRegIndex()));
         }
     }
 
     private DexCodeElement instrument_ArrayGet(DexInstruction_ArrayGet insn, DexCode code) {
     	/*
     	 * TODO: needs to include the taint of the index as well?
     	 */
     	
         DexTaintRegister regToTaint = insn.getRegTo().getTaintRegister();
         DexTaintRegister regArrayTaint = insn.getRegArray().getTaintRegister();
 
         if (isNull(insn, insn.getRegArray()))
             return builder.create(insn, codeGen.setZero(regToTaint)); // leave uninstrumented (always throws)
         
         else {
         
 	        DexRegister regTo = insn.getRegTo();
 	        DexSingleRegister regIndex = insn.getRegIndex();
 	        DexSingleRegister regIndexBackup;
 	        if (regTo.equals(regIndex))
 	            regIndexBackup = codeGen.auxReg();
 	        else
 	            regIndexBackup = regIndex;
 	
 	        if (insn.getOpcode() == Opcode_GetPut.Object)
 	            return new DexMacro(
 	                       codeGen.move_prim(regIndexBackup, regIndex),
 	       	               builder.create(
 	       	            		   insn,
 	       	            		   new DexMacro(
 	       	            				   codeGen.getTaint_ArrayReference(regToTaint, regArrayTaint, regIndexBackup),
 	       	            				   codeGen.taintCast((DexSingleRegister) regTo, regToTaint, analysis_DefReg(insn, regTo)))));
 	                       
 	        else
 	            return new DexMacro(
 	                       codeGen.move_prim(regIndexBackup, regIndex),
 	       	               builder.create(
 	       	            		   insn,
 	       	            		   codeGen.getTaint_ArrayPrimitive(regToTaint, regArrayTaint, regIndexBackup)));
         }
     }
     
     private DexCodeElement instrument_FillArrayData(DexInstruction_FillArrayData insn, DexCode code) {
     	/*
     	 * Argument is an array of primitive type. Data are inserted into the 
     	 * array all at once, so if the array is too short, it will throw an
     	 * ArrayIndexOutOfBounds exception and nothing will be overwritten.
     	 */
     	
     	DexTypeCache cache = hierarchy.getTypeCache();
     	MethodDefinition hashcodeDef = hierarchy.getRoot().getMethod(
 			DexMethodId.parseMethodId(
 				"hashCode", 
 				DexPrototype.parse(cache.getCachedType_Integer(), null, cache),
 				cache));
     	
     	DexSingleRegister regEmptyTaint = codeGen.auxReg();
     	return builder.create(
  				  insn, 
 				  new DexMacro(
 							/*
 							 * This is a workaround for a bug in DX, which thinks 
 							 * that FillArrayData is a non-throwing instruction
 							 * and therefore removes the TRY block. By inserting
 							 * two meaningless method calls, it is forced to keep 
 							 * the TRY block there.
 							 */
 						  	codeGen.invoke(hashcodeDef, insn.getRegArray()),
 							insn,
 							codeGen.invoke(hashcodeDef, insn.getRegArray())),
 					new DexMacro(
 							codeGen.setEmptyTaint(regEmptyTaint),
 							codeGen.setTaint_ArrayPrimitive(regEmptyTaint, insn.getRegArray(), 0, insn.getElementData().size())));
     }
 
     /*
      * TODO: apply template
      */
     private DexCodeElement instrument_FilledArray(FilledArray insn, DexCode code) {
     	DexSingleRegister auxCombinedTaint = codeGen.auxReg();
     	DexSingleRegister auxLength = codeGen.auxReg();
     	DexSingleRegister auxLengthTaint = codeGen.auxReg();
     	DexSingleRegister regArray = (DexSingleRegister) insn.getResult().getRegTo();
     	
     	return new DexMacro(
     			codeGen.combineArgumentsTaint(auxCombinedTaint, insn.getFilledArray()),
     			insn,
     			codeGen.constant(auxLength, insn.getFilledArray().getArgumentRegisters().size()),
     			codeGen.setEmptyTaint(auxLengthTaint),
     			(insn.getFilledArray().getArrayType().getElementType() instanceof DexPrimitiveType) ?
     					codeGen.taintCreate_ArrayPrimitive(regArray, auxLength, auxLengthTaint) :
 						codeGen.taintCreate_ArrayReference(regArray, auxLength, auxLengthTaint),
     			codeGen.setTaint(auxCombinedTaint, regArray.getTaintRegister()));
     }
 
     private DexCodeElement instrument_InstancePut(DexInstruction_InstancePut insnIput, DexCode code) {
         InstanceFieldDefinition fieldDef = insnIput.getFieldDef();
         ClassDefinition classDef = (ClassDefinition) fieldDef.getParentClass();
 
         /*
          * The field definition points directly to the accessed field (looked up
          * during parsing). Therefore we can check whether the containing class is
          * internal/external.
          */
         
         DexRegister regFrom = insnIput.getRegFrom();
         DexTaintRegister regFromTaint = regFrom.getTaintRegister();
         DexSingleRegister regObject = insnIput.getRegObject();
 
         if (classDef.isInternal()) {
 
             DexClass parentClass = dex.getClass(classDef);
             DexInstanceField field = parentClass.getInstanceField(fieldDef);
             DexInstanceField taintField = getTaintField(field);
 
             return builder.create(
                        insnIput,
                        codeGen.iput(regFromTaint, regObject, taintField.getFieldDef()));
 
         } else {
 
             if (fieldDef.getFieldId().getType() instanceof DexPrimitiveType)
             	return builder.create(
                            insnIput,
                            codeGen.setTaintExternal(regFromTaint, regObject));
             else {
                 DexSingleAuxiliaryRegister regAux = codeGen.auxReg();
                 return builder.create(
                        insnIput,
                        new DexMacro(
                     		   codeGen.getTaint(regAux, (DexSingleRegister) regFrom),
                     		   codeGen.setTaintExternal(regAux, regObject)));
             }
         }
     }
 
     private DexCodeElement instrument_InstanceGet(DexInstruction_InstanceGet insnIget, DexCode code) {
         InstanceFieldDefinition fieldDef = insnIget.getFieldDef();
         ClassDefinition classDef = (ClassDefinition) fieldDef.getParentClass();
 
         DexRegister regTo = insnIget.getRegTo();
         DexTaintRegister regToTaint = regTo.getTaintRegister();
         DexSingleRegister regObject = insnIget.getRegObject();
         DexSingleRegister regObjectTaint = regObject.getTaintRegister();
 
         DexSingleRegister regObjectBackup;
         if (regTo.equals(regObject))
         	regObjectBackup = codeGen.auxReg();
         else
         	regObjectBackup = regObject;
         
         DexRegisterType resultType = insnIget.getFieldDef().getFieldId().getType();
         boolean isPrimitive = resultType instanceof DexPrimitiveType;
 
         DexCodeElement backup = codeGen.move_obj(regObjectBackup, regObject);
         DexCodeElement tainting;
         
         if (classDef.isInternal()) {
 
             DexClass parentClass = dex.getClass(classDef);
             DexInstanceField field = parentClass.getInstanceField(fieldDef);
             DexInstanceField taintField = getTaintField(field);
             
             tainting = new DexMacro(
             		   codeGen.iget(regToTaint, regObjectBackup, taintField.getFieldDef()),
             		   isPrimitive ? 
         				   codeGen.empty() :
     					   codeGen.taintCast((DexSingleRegister) regTo, regToTaint, analysis_DefReg(insnIget, regTo)));
 
         } else {
 
             if (isPrimitive)
             	tainting = codeGen.getTaintExternal(regToTaint, regObjectTaint);
 
             else {
                 DexSingleRegister auxToTaint = codeGen.auxReg();
                 tainting = new DexMacro(
                        codeGen.getTaintExternal(auxToTaint, regObjectTaint),
                        codeGen.taintLookup(regToTaint, (DexSingleRegister) regTo, auxToTaint, hierarchy.classifyType(resultType)),
                        codeGen.taintCast((DexSingleRegister) regTo, regToTaint, analysis_DefReg(insnIget, regTo)));
             }
         }
         
         return new DexMacro(backup, builder.create(insnIget, tainting));
     }
 
     private DexCodeElement instrument_StaticPut(DexInstruction_StaticPut insnSput) {
         /*
          * The getTaintField() method automatically creates a storage field.
          * If the parent class is internal, it creates it in the same class,
          * otherwise in a special auxiliary class.
          */
 
         DexStaticField taintField = getTaintField(insnSput.getFieldDef());
         DexTaintRegister regFromTaint = insnSput.getRegFrom().getTaintRegister();
 
         return builder.create(
                    insnSput,
                    codeGen.sput(regFromTaint, taintField.getFieldDef()));                   
     }
 
     private DexCodeElement instrument_StaticGet(DexInstruction_StaticGet insnSget) {
         DexStaticField taintField = getTaintField(insnSget.getFieldDef());
         DexRegister regTo = insnSget.getRegTo();
         DexTaintRegister regToTaint = regTo.getTaintRegister();
 
         DexRegisterType resultType = insnSget.getFieldDef().getFieldId().getType();
         boolean isPrimitive = (resultType instanceof DexPrimitiveType); 
 
         return builder.create(
                    insnSget,
                    new DexMacro(
                 		   codeGen.sget(regToTaint, taintField.getFieldDef()),
                 		   isPrimitive ?
                 				codeGen.empty() :
                 				codeGen.taintCast((DexSingleRegister) regTo, regToTaint, analysis_DefReg(insnSget, regTo))));
     }
 
     private DexCodeElement instrument_MoveException(DexInstruction_MoveException insn) {
         return builder.create(
                    insn,
                    codeGen.taintLookup_NoExtraTaint(insn.getRegTo().getTaintRegister(), insn.getRegTo(), hierarchy.classifyType(analysis_DefReg(insn, insn.getRegTo()))));
     }
 
     private DexCodeElement instrument_Monitor(DexInstruction_Monitor insn, DexCode code) {
     	return builder.create(insn, codeGen.empty());
     }
     
     private DexCodeElement instrument_Throw(DexInstruction_Throw insn, DexCode code) {
     	DexSingleRegister regException = insn.getRegFrom();
     	DexLabel lNull = codeGen.label();
     	return new DexMacro(
     			codeGen.ifZero(regException, lNull),
     			codeGen.thrw(regException), // duplicate the original
     			lNull,
     			builder.create(insn, codeGen.empty()),
     			// meaningless instruction (should be removed as dead code)
     			// only needed to pass CFG analysis
     			codeGen.jump(lNull));
     }
 
     private DexCode insertInstanceFieldInit(DexCode code, DexMethod method) {
     	if (!isConstructorWithSuperclassCall(code, method))
     		return code;
     	
     	DexClass clazz = method.getParentClass();
     	DexSingleRegister regThis = (DexSingleRegister) code.getParameters().get(0).getRegister();
     	
         DexSingleRegister regTaintObject = codeGen.auxReg();
         DexSingleRegister regEmptyTaint = codeGen.auxReg();
 
         List<DexCodeElement> insns = new ArrayList<DexCodeElement>();
 
         insns.add(codeGen.setEmptyTaint(regEmptyTaint));
         
         boolean nullTaintReady = false;
         
         for (DexInstanceField ifield : clazz.getInstanceFields()) {
             if (isTaintField(ifield))
                 continue;
 
             DexInstanceField tfield = getTaintField(ifield);
 
             TypeClassification ifield_type = hierarchy.classifyType(ifield.getFieldDef().getFieldId().getType());
             if (ifield_type == TypeClassification.PRIMITIVE)
             	insns.add(codeGen.iput(regEmptyTaint, regThis, tfield.getFieldDef()));
             else {
             	if (!nullTaintReady) {
             		/*
             		 * The type of NULL taint we create does not matter.
             		 * It will get converted after IGET anyway.
             		 */
             		insns.add(codeGen.taintCreate_External_Null(regTaintObject, regEmptyTaint));
             		nullTaintReady = true;
             	}
             	insns.add(codeGen.iput(regTaintObject, regThis, tfield.getFieldDef()));
             }
         }
 
         insns.addAll(code.getInstructionList());
         
         return new DexCode(code, new InstructionList(insns));
     }
     
     private void insertStaticFieldInit(DexClass clazz) {
         codeGen.resetAsmIds();
         
     	DexMethod clinitMethod = clazz.getMethod(getClinit(clazz));
     	
         DexSingleRegister regTaintObject = codeGen.auxReg();
         DexSingleRegister regEmptyTaint = codeGen.auxReg();
 
     	List<DexCodeElement> insns = new ArrayList<DexCodeElement>();
 
     	insns.add(codeGen.setEmptyTaint(regEmptyTaint));
         
         boolean nullTaintReady = false;
         
         for (DexStaticField tfield : clazz.getStaticFields()) {
             if (!isTaintField(tfield))
                 continue;
             
             StaticFieldDefinition sfield = null;
             for (Entry<StaticFieldDefinition, DexStaticField> entry : taintStaticFields.entrySet()) {
             	if (entry.getValue().equals(tfield)) {
         			sfield = entry.getKey();
         			break;
             	}
             }
 
             TypeClassification sfield_type = hierarchy.classifyType(sfield.getFieldId().getType());
             if (sfield_type == TypeClassification.PRIMITIVE)
             	insns.add(codeGen.sput(regEmptyTaint, tfield.getFieldDef()));
             else {
             	if (!nullTaintReady) {
             		/*
             		 * The type of NULL taint we create does not matter.
             		 * It will get converted after SGET anyway.
             		 */
             		insns.add(codeGen.taintCreate_External_Null(regTaintObject, regEmptyTaint));
             		nullTaintReady = true;
             	}
             	insns.add(codeGen.sput(regTaintObject, tfield.getFieldDef()));
             }
         }
 
     	insns.addAll(clinitMethod.getMethodBody().getInstructionList());
     	
     	DexCode newBody = new DexCode(clinitMethod.getMethodBody(), new InstructionList(insns));
     	DexMethod newMethod = new DexMethod(clinitMethod, newBody);
     	clazz.replaceMethod(clinitMethod, newMethod);
     }
 
     private void generateGetTaint(DexClass clazz) {
         DexTypeCache cache = hierarchy.getTypeCache();
         DexMethod implementationOf = dexAux.getMethod_InternalStructure_GetTaint();
 
         // generate bytecode
 
         DexSingleRegister regTotalTaint = codeGen.auxReg();
         DexSingleRegister regFieldTaint = codeGen.auxReg();
         DexSingleRegister regObject = codeGen.auxReg();
 
         List<DexCodeElement> insns = new ArrayList<DexCodeElement>();
 
         if (clazz.getClassDef().getSuperclass().isInternal())
             insns.add(codeGen.call_super_int(clazz, implementationOf, regTotalTaint, Arrays.asList(regObject)));
         else
             insns.add(codeGen.setEmptyTaint(regTotalTaint));
 
         for (DexInstanceField ifield : clazz.getInstanceFields()) {
             if (isTaintField(ifield))
                 continue;
 
             DexInstanceField tfield = getTaintField(ifield);
 
             insns.add(codeGen.iget(regFieldTaint, regObject, tfield.getFieldDef()));
 
             if (hierarchy.classifyType(ifield.getFieldDef().getFieldId().getType()) == TypeClassification.PRIMITIVE)
                 insns.add(codeGen.combineTaint(regTotalTaint, regTotalTaint, regFieldTaint));
             else {
                 DexLabel label = codeGen.label();
                 insns.add(codeGen.ifZero(regFieldTaint, label));
                 insns.add(codeGen.getTaint(regFieldTaint, regFieldTaint));
                 insns.add(codeGen.combineTaint(regTotalTaint, regTotalTaint, regFieldTaint));
                 insns.add(label);
             }
         }
 
         insns.add(codeGen.return_prim(regTotalTaint));
 
         InstructionList insnlist = new InstructionList(insns);
 
         // generate parameters
 
         Parameter paramThis = new Parameter(clazz.getClassDef().getType(), regObject);
         List<Parameter> params = Arrays.asList(paramThis);
 
         // generate DexCode
 
         DexCode methodBody = new DexCode(insnlist, params, cache.getCachedType_Integer(), false, hierarchy);
 
         // generate method and insert into the class
 
         implementMethod(clazz, implementationOf, methodBody);
     }
 
     private void generateSetTaint(DexClass clazz) {
         DexTypeCache cache = hierarchy.getTypeCache();
         DexMethod implementationOf = dexAux.getMethod_InternalStructure_SetTaint();
 
         // generate bytecode
 
         DexSingleRegister regFieldTaint = codeGen.auxReg();
         DexSingleRegister regAddedTaint = codeGen.auxReg();
         DexSingleRegister regObject = codeGen.auxReg();
 
         List<DexCodeElement> insns = new ArrayList<DexCodeElement>();
 
         if (clazz.getClassDef().getSuperclass().isInternal())
             insns.add(codeGen.call_super_int(clazz, implementationOf, null, Arrays.asList(regObject, regAddedTaint)));
 
         for (DexInstanceField ifield : clazz.getInstanceFields()) {
             if (isTaintField(ifield))
                 continue;
 
             DexInstanceField tfield = getTaintField(ifield);
 
             insns.add(codeGen.iget(regFieldTaint, regObject, tfield.getFieldDef()));
 
             if (hierarchy.classifyType(ifield.getFieldDef().getFieldId().getType()) == TypeClassification.PRIMITIVE) {
                 insns.add(codeGen.combineTaint(regFieldTaint, regFieldTaint, regAddedTaint));
                 insns.add(codeGen.iput(regFieldTaint, regObject, tfield.getFieldDef()));
             } else {
                 DexLabel label = codeGen.label();
                 insns.add(codeGen.ifZero(regFieldTaint, label));
                 insns.add(codeGen.setTaint(regAddedTaint, regFieldTaint));
                 insns.add(label);
             }
         }
 
         insns.add(codeGen.retrn());
 
         InstructionList insnlist = new InstructionList(insns);
 
         // generate parameters
 
         Parameter paramThis = new Parameter(clazz.getClassDef().getType(), regObject);
         Parameter paramAddedTaint = new Parameter(cache.getCachedType_Integer(), regAddedTaint);
         List<Parameter> params = Arrays.asList(paramThis, paramAddedTaint);
 
         // generate DexCode
 
         DexCode methodBody = new DexCode(insnlist, params, cache.getCachedType_Void(), false, hierarchy);
 
         // generate method and insert into the class
 
         implementMethod(clazz, implementationOf, methodBody);
     }
 
     private void implementMethod(DexClass clazz, DexMethod implementationOf, DexCode methodBody) {
         // generate method definition
 
         BaseClassDefinition classDef = clazz.getClassDef();
         DexMethodId methodId = implementationOf.getMethodDef().getMethodId();
         int accessFlags = DexUtils.assembleAccessFlags(AccessFlags.PUBLIC);
         MethodDefinition methodDef = new MethodDefinition(classDef, methodId, accessFlags);
         classDef.addDeclaredMethod(methodDef);
 
         // generate method
 
         DexMethod method = new DexMethod(clazz, methodDef, methodBody);
 
         // add it to the class
 
         clazz.replaceMethods(Utils.concat(clazz.getMethods(), method));
     }
 
     // UTILS
 
     private boolean isCallToSuperclassConstructor(DexInstruction_Invoke insnInvoke, DexCode code, DexMethod method) {
         return
         	method.getMethodDef().isConstructor() &&
         	!method.getMethodDef().isStatic() &&
             insnInvoke.getMethodId().isConstructor() &&
             insnInvoke.getClassType().equals(method.getParentClass().getClassDef().getSuperclass().getType()) &&
             isThisValue(insnInvoke, code);
     }
     
     private boolean isThisValue(DexInstruction_Invoke insnInvoke, DexCode code) {
     	return  !insnInvoke.getArgumentRegisters().isEmpty() &&
     			isThisValue(insnInvoke.getArgumentRegisters().get(0), insnInvoke, code);
     }
     
     private boolean isThisValue(DexRegister firstInsnParam, DexCodeElement refPoint, DexCode code) {
     	if (!(firstInsnParam instanceof DexSingleRegister))
     		return false;
     	
         // First check that the register is the same as this param of the method
     	assert !code.getParameters().isEmpty();
         DexRegister firstMethodParam = code.getParameters().get(0).getRegister();
 
         // Then check that they are unified, i.e. reg inherits the value
         TypeSolver solverStart = codeAnalysis.getStartOfMethod().getDefinedRegisterSolver(firstMethodParam);
         TypeSolver solverRefPoint = codeAnalysis.reverseLookup(refPoint).getUsedRegisterSolver(firstInsnParam);
 
         return solverStart.areEquivalent(solverRefPoint);
     }
 
     private boolean isTaintField(DexInstanceField field) {
         return taintInstanceFields.containsValue(field);
     }
 
     private boolean isTaintField(DexStaticField field) {
         return taintStaticFields.containsValue(field);
     }
 
     private DexInstanceField getTaintField(DexInstanceField field) {
 
         // Check if it has been already created
 
         DexInstanceField cachedTaintField = taintInstanceFields.get(field);
         if (cachedTaintField != null)
             return cachedTaintField;
 
         // It hasn't, so let's create a new one...
 
         final ClassDefinition classDef = (ClassDefinition) field.getParentClass().getClassDef();
 
         // Figure out a non-conflicting name for the new field
 
         // there is a test that tests this - need to change the names of methods if name generation changes!
         String newPrefix = "t_" + field.getFieldDef().getFieldId().getName();
         String newName = Utils.generateName(newPrefix, "", new NameAcceptor() {
             @Override
             public boolean accept(String name) {
                 return classDef.getInstanceField(name) == null;
             }
         });
 
         // Generate the new taint field
 
         DexFieldId fieldId = DexFieldId.parseFieldId(newName, codeGen.taintFieldType(field.getFieldDef().getFieldId().getType()), typeCache);
         int fieldAccessFlags = DexUtils.assembleAccessFlags(removeFinalFlag(field.getFieldDef().getAccessFlags()));
         InstanceFieldDefinition fieldDef = new InstanceFieldDefinition(classDef, fieldId, fieldAccessFlags);
         classDef.addDeclaredInstanceField(fieldDef);
 
         DexClass parentClass = field.getParentClass();
         DexInstanceField taintField = new DexInstanceField(parentClass, fieldDef);
         parentClass.replaceInstanceFields(Utils.concat(parentClass.getInstanceFields(), taintField));
 
         // Cache it
 
         taintInstanceFields.put(field, taintField);
 
         // Return
 
         return taintField;
     }
 
     private DexStaticField getTaintField(StaticFieldDefinition fieldDef) {
 
         // Check if it has been already created
 
         DexStaticField cachedTaintField = taintStaticFields.get(fieldDef);
         if (cachedTaintField != null)
             return cachedTaintField;
 
         // It hasn't, so let's create a new one...
 
         BaseClassDefinition classDef;
         DexClass parentClass;
 
         if (fieldDef.getParentClass().isInternal()) {
             classDef = fieldDef.getParentClass();
             parentClass = dex.getClass(classDef);
         } else {
             // field is external => cannot create extra field
             // in the same class
 
             parentClass = dexAux.getType_StaticTaintFields();
             classDef = parentClass.getClassDef();
         }
 
         // Figure out a non-conflicting name for the new field
 
         // there is a test that tests this - need to change the names of methods if name generation changes!
         String newPrefix = "t_" + fieldDef.getFieldId().getName();
         final BaseClassDefinition classDefFinal = classDef;
         String newName = Utils.generateName(newPrefix, "", new NameAcceptor() {
             @Override
             public boolean accept(String name) {
                 return classDefFinal.getStaticField(name) == null;
             }
         });
 
         // Generate the new taint field
 
         DexFieldId taintFieldId = DexFieldId.parseFieldId(newName, codeGen.taintFieldType(fieldDef.getFieldId().getType()), typeCache);
         int fieldAccessFlags = DexUtils.assembleAccessFlags(addPublicFlag(removeFinalFlag(fieldDef.getAccessFlags())));
         StaticFieldDefinition taintFieldDef = new StaticFieldDefinition(classDef, taintFieldId, fieldAccessFlags);
         classDef.addDeclaredStaticField(taintFieldDef);
 
         DexStaticField taintField = new DexStaticField(parentClass, taintFieldDef, null);
         parentClass.replaceStaticFields(Utils.concat(parentClass.getStaticFields(), taintField));
 
         // Cache it
 
         taintStaticFields.put(fieldDef, taintField);
 
         // Return
 
         return taintField;
     }
     
     private Collection<AccessFlags> removeFinalFlag(Collection<AccessFlags> flags) {
     	Set<AccessFlags> newFlags = new HashSet<AccessFlags>(flags);
     	flags.remove(AccessFlags.FINAL);
     	return newFlags;
     }
     
     private Collection<AccessFlags> addPublicFlag(Collection<AccessFlags> flags) {
     	Set<AccessFlags> newFlags = new HashSet<AccessFlags>(flags);
     	flags.add(AccessFlags.PUBLIC);
     	flags.remove(AccessFlags.PROTECTED);
     	flags.remove(AccessFlags.PRIVATE);
     	return newFlags;
     }
 
     private DexReferenceType analysis_DefReg(DexCodeElement insn, DexRegister reg) {
         RopType type = codeAnalysis.reverseLookup(insn).getDefinedRegisterSolver(reg).getType();
         if (type.category == RopType.Category.Reference)
         	return type.type;
         else
             throw new AssertionError("Cannot decide the type of register " + reg + " (" + type.category.name() + ") at " + insn);
     }
 
     private boolean isNull(DexInstruction insn, DexSingleRegister reg) {
         RopType type = codeAnalysis.reverseLookup(insn).getUsedRegisterSolver(reg).getType();
     	return type.category == Category.Null;
     }
 
     private DexMethodId getClinitId() {
     	DexTypeCache cache = hierarchy.getTypeCache();
 
     	return DexMethodId.parseMethodId(
 				"<clinit>",
 				DexPrototype.parse(cache.getCachedType_Void(), null, cache),
 				cache);    	
     }
     
     private MethodDefinition getClinit(DexClass clazz) {
         return clazz.getClassDef().getMethod(getClinitId());
     }
         
     private void createTaintFields() {
         for (DexClass clazz : dex.getClasses())  {
             for(DexInstanceField field : clazz.getInstanceFields())
                 getTaintField(field);
             for(DexStaticField field : clazz.getStaticFields())
                 getTaintField(field.getFieldDef());
         }
     }
     
     @Override
     public boolean exclude(DexClass clazz) {
         if (dexAux != null && (dexAux.getClasses().contains(clazz)))
             return true;
         
         return false;
     }
     
     private void mergeAuxDex() {
         // insert classes from dexAux to the resulting DEX
         dex.addClasses(dexAux.getClasses());
         
         // add static field initializer into StaticTaintFields class
         DexClass staticFieldsClass = dexAux.getType_StaticTaintFields();
         createEmptyClinit(staticFieldsClass);
         insertStaticFieldInit(staticFieldsClass);
     }
     
     private boolean isConstructorWithSuperclassCall(DexCode code, DexMethod method) {
     	if (!code.isConstructor())
     		return false;
     	
     	for (DexCodeElement elem : code.getInstructionList()) {
     		DexInstruction_Invoke invoke;
     		if (elem instanceof MethodCall)
     			invoke = ((MethodCall) elem).getInvoke();
     		else if (elem instanceof DexInstruction_Invoke)
     			invoke = (DexInstruction_Invoke) elem;
     		else
     			continue;
     		
     		if (isCallToSuperclassConstructor(invoke, code, method))
     			return true;
     	}
     	
     	return false;
     }
 
 	@Override
 	public void doClass(DexClass cls) {
 		if (isStaticTaintFieldsClass(cls)) {
 			createEmptyClinit(cls);
 			insertStaticFieldInit(cls);
 		} else
 			super.doClass(cls);
 	}
 }
