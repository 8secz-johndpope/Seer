 package org.eclipse.emf.codegen.ecore.templates.model;
 
 import java.util.*;
 import org.eclipse.emf.ecore.*;
 import org.eclipse.emf.ecore.util.EcoreUtil;
 import org.eclipse.emf.codegen.ecore.genmodel.*;
 import org.eclipse.emf.codegen.ecore.genmodel.impl.Literals;
 
 public class PackageClass
 {
   protected static String nl;
   public static synchronized PackageClass create(String lineSeparator)
   {
     nl = lineSeparator;
     PackageClass result = new PackageClass();
     nl = null;
     return result;
   }
 
   public final String NL = nl == null ? (System.getProperties().getProperty("line.separator")) : nl;
   protected final String TEXT_1 = "";
   protected final String TEXT_2 = "/**";
   protected final String TEXT_3 = NL + " * ";
   protected final String TEXT_4 = NL + " * <copyright>" + NL + " * </copyright>";
   protected final String TEXT_5 = NL + " *" + NL + " * ";
   protected final String TEXT_6 = "Id";
   protected final String TEXT_7 = NL + " */";
   protected final String TEXT_8 = NL + "package ";
   protected final String TEXT_9 = ";";
   protected final String TEXT_10 = NL + "package ";
   protected final String TEXT_11 = ";";
   protected final String TEXT_12 = NL;
   protected final String TEXT_13 = NL + NL + "/**" + NL + " * <!-- begin-user-doc -->" + NL + " * The <b>Package</b> for the model." + NL + " * It contains accessors for the meta objects to represent" + NL + " * <ul>" + NL + " *   <li>each class,</li>" + NL + " *   <li>each feature of each class,</li>" + NL + " *   <li>each enum,</li>" + NL + " *   <li>and each data type</li>" + NL + " * </ul>" + NL + " * <!-- end-user-doc -->";
   protected final String TEXT_14 = NL + " * <!-- begin-model-doc -->" + NL + " * ";
   protected final String TEXT_15 = NL + " * <!-- end-model-doc -->";
   protected final String TEXT_16 = NL + " * @see ";
   protected final String TEXT_17 = NL + " * @model ";
   protected final String TEXT_18 = NL + " *        ";
   protected final String TEXT_19 = NL + " * @model";
   protected final String TEXT_20 = NL + " * @generated" + NL + " */";
   protected final String TEXT_21 = NL + NL + "/**" + NL + " * <!-- begin-user-doc -->" + NL + " * An implementation of the model <b>Package</b>." + NL + " * <!-- end-user-doc -->" + NL + " * @generated" + NL + " */";
   protected final String TEXT_22 = NL + "public class ";
   protected final String TEXT_23 = " extends ";
   protected final String TEXT_24 = " implements ";
   protected final String TEXT_25 = NL + "public interface ";
   protected final String TEXT_26 = " extends ";
   protected final String TEXT_27 = NL + "{";
   protected final String TEXT_28 = NL + "\t/**" + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\t";
   protected final String TEXT_29 = " copyright = ";
   protected final String TEXT_30 = ";";
   protected final String TEXT_31 = NL;
   protected final String TEXT_32 = NL + "\t/**" + NL + "\t * The package name." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\t";
   protected final String TEXT_33 = " eNAME = \"";
   protected final String TEXT_34 = "\";";
   protected final String TEXT_35 = NL + NL + "\t/**" + NL + "\t * The package namespace URI." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\t";
   protected final String TEXT_36 = " eNS_URI = \"";
   protected final String TEXT_37 = "\";";
   protected final String TEXT_38 = NL + NL + "\t/**" + NL + "\t * The package namespace name." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\t";
   protected final String TEXT_39 = " eNS_PREFIX = \"";
   protected final String TEXT_40 = "\";";
   protected final String TEXT_41 = NL + NL + "\t/**" + NL + "\t * The package content type ID." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\t";
   protected final String TEXT_42 = " eCONTENT_TYPE = \"";
   protected final String TEXT_43 = "\";";
   protected final String TEXT_44 = NL + NL + "\t/**" + NL + "\t * The singleton instance of the package." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\t";
   protected final String TEXT_45 = " eINSTANCE = ";
   protected final String TEXT_46 = ".init();" + NL;
   protected final String TEXT_47 = NL + "\t/**";
   protected final String TEXT_48 = NL + "\t * The meta object id for the '{@link ";
   protected final String TEXT_49 = " <em>";
   protected final String TEXT_50 = "</em>}' class." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @see ";
   protected final String TEXT_51 = NL + "\t * The meta object id for the '{@link ";
   protected final String TEXT_52 = " <em>";
   protected final String TEXT_53 = "</em>}' class." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @see ";
   protected final String TEXT_54 = NL + "\t * The meta object id for the '{@link ";
   protected final String TEXT_55 = " <em>";
   protected final String TEXT_56 = "</em>}' enum." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @see ";
   protected final String TEXT_57 = NL + "\t * The meta object id for the '<em>";
   protected final String TEXT_58 = "</em>' data type." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->";
   protected final String TEXT_59 = NL + "\t * @see ";
   protected final String TEXT_60 = NL + "\t * @see ";
   protected final String TEXT_61 = "#get";
   protected final String TEXT_62 = "()" + NL + "\t * @generated" + NL + "\t */" + NL + "\t";
   protected final String TEXT_63 = "int ";
   protected final String TEXT_64 = " = ";
   protected final String TEXT_65 = ";" + NL;
   protected final String TEXT_66 = NL + "\t/**" + NL + "\t * The feature id for the '<em><b>";
   protected final String TEXT_67 = "</b></em>' ";
   protected final String TEXT_68 = "." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t * @ordered" + NL + "\t */" + NL + "\t";
   protected final String TEXT_69 = "int ";
   protected final String TEXT_70 = " = ";
   protected final String TEXT_71 = ";" + NL;
   protected final String TEXT_72 = NL + "\t/**" + NL + "\t * The number of structural features of the '<em>";
   protected final String TEXT_73 = "</em>' class." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t * @ordered" + NL + "\t */" + NL + "\t";
   protected final String TEXT_74 = "int ";
   protected final String TEXT_75 = " = ";
   protected final String TEXT_76 = ";" + NL;
   protected final String TEXT_77 = NL + "\t/**" + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\tprotected String packageFilename = \"";
   protected final String TEXT_78 = "\";";
   protected final String TEXT_79 = NL;
   protected final String TEXT_80 = NL + "\t/**" + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\tprivate ";
   protected final String TEXT_81 = " ";
   protected final String TEXT_82 = " = null;" + NL;
   protected final String TEXT_83 = NL + "\t/**" + NL + "\t * Creates an instance of the model <b>Package</b>, registered with" + NL + "\t * {@link org.eclipse.emf.ecore.EPackage.Registry EPackage.Registry} by the package" + NL + "\t * package URI value." + NL + "\t * <p>Note: the correct way to create the package is via the static" + NL + "\t * factory method {@link #init init()}, which also performs" + NL + "\t * initialization of the package, or returns the registered package," + NL + "\t * if one already exists." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @see org.eclipse.emf.ecore.EPackage.Registry" + NL + "\t * @see ";
   protected final String TEXT_84 = "#eNS_URI" + NL + "\t * @see #init()" + NL + "\t * @generated" + NL + "\t */" + NL + "\tprivate ";
   protected final String TEXT_85 = "()" + NL + "\t{" + NL + "\t\tsuper(eNS_URI, ";
   protected final String TEXT_86 = ");" + NL + "\t}" + NL + "" + NL + "\t/**" + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\tprivate static boolean isInited = false;" + NL + "" + NL + "\t/**" + NL + "\t * Creates, registers, and initializes the <b>Package</b> for this" + NL + "\t * model, and for any others upon which it depends.  Simple" + NL + "\t * dependencies are satisfied by calling this method on all" + NL + "\t * dependent packages before doing anything else.  This method drives" + NL + "\t * initialization for interdependent packages directly, in parallel" + NL + "\t * with this package, itself." + NL + "\t * <p>Of this package and its interdependencies, all packages which" + NL + "\t * have not yet been registered by their URI values are first created" + NL + "\t * and registered.  The packages are then initialized in two steps:" + NL + "\t * meta-model objects for all of the packages are created before any" + NL + "\t * are initialized, since one package's meta-model objects may refer to" + NL + "\t * those of another." + NL + "\t * <p>Invocation of this method will not affect any packages that have" + NL + "\t * already been initialized." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @see #eNS_URI";
   protected final String TEXT_87 = NL + "\t * @see #createPackageContents()" + NL + "\t * @see #initializePackageContents()";
   protected final String TEXT_88 = NL + "\t * @generated" + NL + "\t */" + NL + "\tpublic static ";
   protected final String TEXT_89 = " init()" + NL + "\t{" + NL + "\t\tif (isInited) return (";
   protected final String TEXT_90 = ")";
   protected final String TEXT_91 = ".Registry.INSTANCE.getEPackage(";
   protected final String TEXT_92 = ".eNS_URI);" + NL + "" + NL + "\t\t// Obtain or create and register package" + NL + "\t\t";
   protected final String TEXT_93 = " the";
   protected final String TEXT_94 = " = (";
   protected final String TEXT_95 = ")(";
   protected final String TEXT_96 = ".Registry.INSTANCE.getEPackage(eNS_URI) instanceof ";
   protected final String TEXT_97 = " ? ";
   protected final String TEXT_98 = ".Registry.INSTANCE.getEPackage(eNS_URI) : new ";
   protected final String TEXT_99 = "());" + NL + "" + NL + "\t\tisInited = true;" + NL;
   protected final String TEXT_100 = NL + "\t\t// Initialize simple dependencies";
   protected final String TEXT_101 = NL + "\t\t";
   protected final String TEXT_102 = ".eINSTANCE.eClass();";
   protected final String TEXT_103 = NL;
   protected final String TEXT_104 = NL + "\t\t// Obtain or create and register interdependencies";
   protected final String TEXT_105 = NL + "\t\t";
   protected final String TEXT_106 = " ";
   protected final String TEXT_107 = " = (";
   protected final String TEXT_108 = ")(";
   protected final String TEXT_109 = ".Registry.INSTANCE.getEPackage(";
   protected final String TEXT_110 = ".eNS_URI) instanceof ";
   protected final String TEXT_111 = " ? ";
   protected final String TEXT_112 = ".Registry.INSTANCE.getEPackage(";
   protected final String TEXT_113 = ".eNS_URI) : ";
   protected final String TEXT_114 = ".eINSTANCE);";
   protected final String TEXT_115 = NL;
   protected final String TEXT_116 = NL + "\t\t// Load packages";
   protected final String TEXT_117 = NL + "\t\tthe";
   protected final String TEXT_118 = ".loadPackage();";
   protected final String TEXT_119 = NL + "\t\t";
   protected final String TEXT_120 = ".loadPackage();";
   protected final String TEXT_121 = NL;
   protected final String TEXT_122 = NL + "\t\t// Create package meta-data objects";
   protected final String TEXT_123 = NL + "\t\tthe";
   protected final String TEXT_124 = ".createPackageContents();";
   protected final String TEXT_125 = NL + "\t\t";
   protected final String TEXT_126 = ".createPackageContents();";
   protected final String TEXT_127 = NL + NL + "\t\t// Initialize created meta-data";
   protected final String TEXT_128 = NL + "\t\tthe";
   protected final String TEXT_129 = ".initializePackageContents();";
   protected final String TEXT_130 = NL + "\t\t";
   protected final String TEXT_131 = ".initializePackageContents();";
   protected final String TEXT_132 = NL;
   protected final String TEXT_133 = NL + "\t\t// Fix loaded packages";
   protected final String TEXT_134 = NL + "\t\tthe";
   protected final String TEXT_135 = ".fixPackageContents();";
   protected final String TEXT_136 = NL + "\t\t";
   protected final String TEXT_137 = ".fixPackageContents();";
   protected final String TEXT_138 = NL;
   protected final String TEXT_139 = NL + "\t\t// Register package validator" + NL + "\t\t";
   protected final String TEXT_140 = ".Registry.INSTANCE.put" + NL + "\t\t\t(the";
   protected final String TEXT_141 = ", " + NL + "\t\t\t new ";
   protected final String TEXT_142 = ".Descriptor()" + NL + "\t\t\t {" + NL + "\t\t\t\t public ";
   protected final String TEXT_143 = " getEValidator()" + NL + "\t\t\t\t {" + NL + "\t\t\t\t\t return ";
   protected final String TEXT_144 = ".INSTANCE;" + NL + "\t\t\t\t }" + NL + "\t\t\t });" + NL;
   protected final String TEXT_145 = NL + "\t\t// Mark meta-data to indicate it can't be changed" + NL + "\t\tthe";
   protected final String TEXT_146 = ".freeze();" + NL;
   protected final String TEXT_147 = NL + "\t\treturn the";
   protected final String TEXT_148 = ";" + NL + "\t}" + NL;
   protected final String TEXT_149 = NL;
   protected final String TEXT_150 = NL + "\t/**";
   protected final String TEXT_151 = NL + "\t * Returns the meta object for class '{@link ";
   protected final String TEXT_152 = " <em>";
   protected final String TEXT_153 = "</em>}'." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @return the meta object for class '<em>";
   protected final String TEXT_154 = "</em>'." + NL + "\t * @see ";
   protected final String TEXT_155 = NL + "\t * @model ";
   protected final String TEXT_156 = NL + "\t *        ";
   protected final String TEXT_157 = NL + "\t * @model";
   protected final String TEXT_158 = NL + "\t * Returns the meta object for enum '{@link ";
   protected final String TEXT_159 = " <em>";
   protected final String TEXT_160 = "</em>}'." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @return the meta object for enum '<em>";
   protected final String TEXT_161 = "</em>'." + NL + "\t * @see ";
   protected final String TEXT_162 = NL + "\t * Returns the meta object for data type '<em>";
   protected final String TEXT_163 = "</em>'.";
   protected final String TEXT_164 = NL + "\t * Returns the meta object for data type '{@link ";
   protected final String TEXT_165 = " <em>";
   protected final String TEXT_166 = "</em>}'.";
   protected final String TEXT_167 = NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @return the meta object for data type '<em>";
   protected final String TEXT_168 = "</em>'.";
   protected final String TEXT_169 = NL + "\t * @see ";
   protected final String TEXT_170 = NL + "\t * @model ";
   protected final String TEXT_171 = NL + "\t *        ";
   protected final String TEXT_172 = NL + "\t * @model";
   protected final String TEXT_173 = NL + "\t * @generated" + NL + "\t */";
   protected final String TEXT_174 = NL + "\t/**" + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */";
   protected final String TEXT_175 = NL + "\tpublic ";
   protected final String TEXT_176 = " get";
   protected final String TEXT_177 = "()" + NL + "\t{";
   protected final String TEXT_178 = NL + "\t\tif (";
   protected final String TEXT_179 = " == null)" + NL + "\t\t{" + NL + "\t\t\t";
   protected final String TEXT_180 = " = (";
   protected final String TEXT_181 = ")";
   protected final String TEXT_182 = ".Registry.INSTANCE.getEPackage(";
   protected final String TEXT_183 = ".eNS_URI).getEClassifiers().get(";
   protected final String TEXT_184 = ");" + NL + "\t\t}";
   protected final String TEXT_185 = NL + "\t\treturn ";
   protected final String TEXT_186 = ";" + NL + "\t}" + NL;
   protected final String TEXT_187 = NL + "\t";
   protected final String TEXT_188 = " get";
   protected final String TEXT_189 = "();" + NL;
   protected final String TEXT_190 = NL + "\t/**" + NL + "\t * Returns the meta object for the ";
   protected final String TEXT_191 = " '{@link ";
   protected final String TEXT_192 = "#";
   protected final String TEXT_193 = " <em>";
   protected final String TEXT_194 = "</em>}'." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @return the meta object for the ";
   protected final String TEXT_195 = " '<em>";
   protected final String TEXT_196 = "</em>'." + NL + "\t * @see ";
   protected final String TEXT_197 = "#";
   protected final String TEXT_198 = "()";
   protected final String TEXT_199 = NL + "\t * @see #get";
   protected final String TEXT_200 = "()" + NL + "\t * @generated" + NL + "\t */";
   protected final String TEXT_201 = NL + "\t/**" + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */";
   protected final String TEXT_202 = NL + "\tpublic ";
   protected final String TEXT_203 = " get";
   protected final String TEXT_204 = "()" + NL + "\t{";
   protected final String TEXT_205 = NL + "\t\treturn (";
   protected final String TEXT_206 = ")";
   protected final String TEXT_207 = ".getEStructuralFeatures().get(";
   protected final String TEXT_208 = ");";
   protected final String TEXT_209 = NL + "        return (";
   protected final String TEXT_210 = ")get";
   protected final String TEXT_211 = "().getEStructuralFeatures().get(";
   protected final String TEXT_212 = ");";
   protected final String TEXT_213 = NL + "\t}";
   protected final String TEXT_214 = NL + "\t";
   protected final String TEXT_215 = " get";
   protected final String TEXT_216 = "();";
   protected final String TEXT_217 = NL;
   protected final String TEXT_218 = NL + "\t/**" + NL + "\t * Returns the factory that creates the instances of the model." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @return the factory that creates the instances of the model." + NL + "\t * @generated" + NL + "\t */";
   protected final String TEXT_219 = NL + "\t/**" + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */";
   protected final String TEXT_220 = NL + "\tpublic ";
   protected final String TEXT_221 = " get";
   protected final String TEXT_222 = "()" + NL + "\t{" + NL + "\t\treturn (";
   protected final String TEXT_223 = ")getEFactoryInstance();" + NL + "\t}";
   protected final String TEXT_224 = NL + "\t";
   protected final String TEXT_225 = " get";
   protected final String TEXT_226 = "();";
   protected final String TEXT_227 = NL;
   protected final String TEXT_228 = NL + "\t/**" + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\tprivate boolean isCreated = false;" + NL + "" + NL + "\t/**" + NL + "\t * Creates the meta-model objects for the package.  This method is" + NL + "\t * guarded to have no affect on any invocation but its first." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\tpublic void createPackageContents()" + NL + "\t{" + NL + "\t\tif (isCreated) return;" + NL + "\t\tisCreated = true;";
   protected final String TEXT_229 = NL + NL + "\t\t// Create classes and their features";
   protected final String TEXT_230 = NL + "\t\t";
   protected final String TEXT_231 = " = create";
   protected final String TEXT_232 = "(";
   protected final String TEXT_233 = ");";
   protected final String TEXT_234 = NL + "\t\tcreate";
   protected final String TEXT_235 = "(";
   protected final String TEXT_236 = ", ";
   protected final String TEXT_237 = ");";
   protected final String TEXT_238 = NL;
   protected final String TEXT_239 = NL + NL + "\t\t// Create enums";
   protected final String TEXT_240 = NL + "\t\t";
   protected final String TEXT_241 = " = createEEnum(";
   protected final String TEXT_242 = ");";
   protected final String TEXT_243 = NL + NL + "\t\t// Create data types";
   protected final String TEXT_244 = NL + "\t\t";
   protected final String TEXT_245 = " = createEDataType(";
   protected final String TEXT_246 = ");";
   protected final String TEXT_247 = NL + "\t}" + NL + "" + NL + "\t/**" + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\tprivate boolean isInitialized = false;" + NL;
   protected final String TEXT_248 = NL + "\t/**" + NL + "\t * Complete the initialization of the package and its meta-model.  This" + NL + "\t * method is guarded to have no affect on any invocation but its first." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\tpublic void initializePackageContents()" + NL + "\t{" + NL + "\t\tif (isInitialized) return;" + NL + "\t\tisInitialized = true;" + NL + "" + NL + "\t\t// Initialize package" + NL + "\t\tsetName(eNAME);" + NL + "\t\tsetNsPrefix(eNS_PREFIX);" + NL + "\t\tsetNsURI(eNS_URI);";
   protected final String TEXT_249 = NL + NL + "\t\t// Obtain other dependent packages";
   protected final String TEXT_250 = NL + "\t\t";
   protected final String TEXT_251 = " ";
   protected final String TEXT_252 = " = (";
   protected final String TEXT_253 = ")";
   protected final String TEXT_254 = ".Registry.INSTANCE.getEPackage(";
   protected final String TEXT_255 = ".eNS_URI);";
   protected final String TEXT_256 = NL + NL + "\t\t// Add subpackages";
   protected final String TEXT_257 = NL + "\t\tgetESubpackages().add(";
   protected final String TEXT_258 = ");";
   protected final String TEXT_259 = NL + NL + "\t\t// Create type parameters";
   protected final String TEXT_260 = NL + "\t\t";
   protected final String TEXT_261 = " ";
   protected final String TEXT_262 = "_";
   protected final String TEXT_263 = " = addETypeParameter(";
   protected final String TEXT_264 = ", \"";
   protected final String TEXT_265 = "\");";
   protected final String TEXT_266 = NL + "\t\taddETypeParameter(";
   protected final String TEXT_267 = ", \"";
   protected final String TEXT_268 = "\");";
   protected final String TEXT_269 = NL + NL + "\t\t// Set bounds for type parameters";
   protected final String TEXT_270 = NL + "\t\t";
   protected final String TEXT_271 = "g";
   protected final String TEXT_272 = " = createEGenericType(";
   protected final String TEXT_273 = ");";
   protected final String TEXT_274 = NL + "\t\tg";
   protected final String TEXT_275 = ".";
   protected final String TEXT_276 = "(g";
   protected final String TEXT_277 = ");";
   protected final String TEXT_278 = NL + "\t\t";
   protected final String TEXT_279 = "_";
   protected final String TEXT_280 = ".getEBounds().add(g1);";
   protected final String TEXT_281 = NL + NL + "\t\t// Add supertypes to classes";
   protected final String TEXT_282 = NL + "\t\t";
   protected final String TEXT_283 = ".getESuperTypes().add(";
   protected final String TEXT_284 = ".get";
   protected final String TEXT_285 = "());";
   protected final String TEXT_286 = NL + "\t\t";
   protected final String TEXT_287 = "g";
   protected final String TEXT_288 = " = createEGenericType(";
   protected final String TEXT_289 = ");";
   protected final String TEXT_290 = NL + "\t\tg";
   protected final String TEXT_291 = ".";
   protected final String TEXT_292 = "(g";
   protected final String TEXT_293 = ");";
   protected final String TEXT_294 = NL + "\t\t";
   protected final String TEXT_295 = ".getEGenericSuperTypes().add(g1);";
   protected final String TEXT_296 = NL + NL + "\t\t// Initialize classes and features; add operations and parameters";
   protected final String TEXT_297 = NL + "\t\tinitEClass(";
   protected final String TEXT_298 = ", ";
   protected final String TEXT_299 = "null";
   protected final String TEXT_300 = ".class";
   protected final String TEXT_301 = ", \"";
   protected final String TEXT_302 = "\", ";
   protected final String TEXT_303 = ", ";
   protected final String TEXT_304 = ", ";
   protected final String TEXT_305 = ", \"";
   protected final String TEXT_306 = "\"";
   protected final String TEXT_307 = ");";
   protected final String TEXT_308 = NL + "\t\t";
   protected final String TEXT_309 = "g";
   protected final String TEXT_310 = " = createEGenericType(";
   protected final String TEXT_311 = ");";
   protected final String TEXT_312 = NL + "\t\tg";
   protected final String TEXT_313 = ".";
   protected final String TEXT_314 = "(g";
   protected final String TEXT_315 = ");";
   protected final String TEXT_316 = NL + "\t\tinitEReference(get";
   protected final String TEXT_317 = "(), ";
   protected final String TEXT_318 = "g1";
   protected final String TEXT_319 = ".get";
   protected final String TEXT_320 = "()";
   protected final String TEXT_321 = ", ";
   protected final String TEXT_322 = ", \"";
   protected final String TEXT_323 = "\", ";
   protected final String TEXT_324 = ", ";
   protected final String TEXT_325 = ", ";
   protected final String TEXT_326 = ", ";
   protected final String TEXT_327 = ", ";
   protected final String TEXT_328 = ", ";
   protected final String TEXT_329 = ", ";
   protected final String TEXT_330 = ", ";
   protected final String TEXT_331 = ", ";
   protected final String TEXT_332 = ", ";
   protected final String TEXT_333 = ", ";
   protected final String TEXT_334 = ", ";
   protected final String TEXT_335 = ", ";
   protected final String TEXT_336 = ");";
   protected final String TEXT_337 = NL + "\t\tget";
   protected final String TEXT_338 = "().getEKeys().add(";
   protected final String TEXT_339 = ".get";
   protected final String TEXT_340 = "());";
   protected final String TEXT_341 = NL + "\t\tinitEAttribute(get";
   protected final String TEXT_342 = "(), ";
   protected final String TEXT_343 = "g1";
   protected final String TEXT_344 = ".get";
   protected final String TEXT_345 = "()";
   protected final String TEXT_346 = ", \"";
   protected final String TEXT_347 = "\", ";
   protected final String TEXT_348 = ", ";
   protected final String TEXT_349 = ", ";
   protected final String TEXT_350 = ", ";
   protected final String TEXT_351 = ", ";
   protected final String TEXT_352 = ", ";
   protected final String TEXT_353 = ", ";
   protected final String TEXT_354 = ", ";
   protected final String TEXT_355 = ", ";
   protected final String TEXT_356 = ", ";
   protected final String TEXT_357 = ", ";
   protected final String TEXT_358 = ", ";
   protected final String TEXT_359 = ");";
   protected final String TEXT_360 = NL;
   protected final String TEXT_361 = NL + "\t\t";
   protected final String TEXT_362 = "addEOperation(";
   protected final String TEXT_363 = ", ";
   protected final String TEXT_364 = "null";
   protected final String TEXT_365 = ".get";
   protected final String TEXT_366 = "()";
   protected final String TEXT_367 = ", \"";
   protected final String TEXT_368 = "\", ";
   protected final String TEXT_369 = ", ";
   protected final String TEXT_370 = ", ";
   protected final String TEXT_371 = ", ";
   protected final String TEXT_372 = ");";
   protected final String TEXT_373 = NL + "\t\t";
   protected final String TEXT_374 = "addEOperation(";
   protected final String TEXT_375 = ", ";
   protected final String TEXT_376 = ".get";
   protected final String TEXT_377 = "(), \"";
   protected final String TEXT_378 = "\", ";
   protected final String TEXT_379 = ", ";
   protected final String TEXT_380 = ", ";
   protected final String TEXT_381 = ", ";
   protected final String TEXT_382 = ");";
   protected final String TEXT_383 = NL + "\t\t";
   protected final String TEXT_384 = "addEOperation(";
   protected final String TEXT_385 = ", ";
   protected final String TEXT_386 = ".get";
   protected final String TEXT_387 = "(), \"";
   protected final String TEXT_388 = "\", ";
   protected final String TEXT_389 = ", ";
   protected final String TEXT_390 = ");";
   protected final String TEXT_391 = NL + "\t\t";
   protected final String TEXT_392 = "addEOperation(";
   protected final String TEXT_393 = ", null, \"";
   protected final String TEXT_394 = "\");";
   protected final String TEXT_395 = NL + "\t\t";
   protected final String TEXT_396 = "addETypeParameter(op, \"";
   protected final String TEXT_397 = "\");";
   protected final String TEXT_398 = NL + "\t\t";
   protected final String TEXT_399 = "g";
   protected final String TEXT_400 = " = createEGenericType(";
   protected final String TEXT_401 = ");";
   protected final String TEXT_402 = NL + "\t\tg";
   protected final String TEXT_403 = ".";
   protected final String TEXT_404 = "(g";
   protected final String TEXT_405 = ");";
   protected final String TEXT_406 = NL + "\t\tt";
   protected final String TEXT_407 = ".getEBounds().add(g1);";
   protected final String TEXT_408 = NL + "\t\t";
   protected final String TEXT_409 = "g";
   protected final String TEXT_410 = " = createEGenericType(";
   protected final String TEXT_411 = ");";
   protected final String TEXT_412 = NL + "\t\tg";
   protected final String TEXT_413 = ".";
   protected final String TEXT_414 = "(g";
   protected final String TEXT_415 = ");";
   protected final String TEXT_416 = NL + "\t\taddEParameter(op, ";
   protected final String TEXT_417 = "g1";
   protected final String TEXT_418 = ".get";
   protected final String TEXT_419 = "()";
   protected final String TEXT_420 = ", \"";
   protected final String TEXT_421 = "\", ";
   protected final String TEXT_422 = ", ";
   protected final String TEXT_423 = ", ";
   protected final String TEXT_424 = ", ";
   protected final String TEXT_425 = ");";
   protected final String TEXT_426 = NL + "\t\taddEParameter(op, ";
   protected final String TEXT_427 = "g1";
   protected final String TEXT_428 = ".get";
   protected final String TEXT_429 = "()";
   protected final String TEXT_430 = ", \"";
   protected final String TEXT_431 = "\", ";
   protected final String TEXT_432 = ", ";
   protected final String TEXT_433 = ", ";
   protected final String TEXT_434 = ", ";
   protected final String TEXT_435 = ");";
   protected final String TEXT_436 = NL + "\t\taddEParameter(op, ";
   protected final String TEXT_437 = "g1";
   protected final String TEXT_438 = ".get";
   protected final String TEXT_439 = "()";
   protected final String TEXT_440 = ", \"";
   protected final String TEXT_441 = "\", ";
   protected final String TEXT_442 = ", ";
   protected final String TEXT_443 = ");";
   protected final String TEXT_444 = NL + "\t\t";
   protected final String TEXT_445 = "g";
   protected final String TEXT_446 = " = createEGenericType(";
   protected final String TEXT_447 = ");";
   protected final String TEXT_448 = NL + "\t\tg";
   protected final String TEXT_449 = ".";
   protected final String TEXT_450 = "(g";
   protected final String TEXT_451 = ");";
   protected final String TEXT_452 = NL + "\t\taddEException(op, g";
   protected final String TEXT_453 = ");";
   protected final String TEXT_454 = NL + "\t\taddEException(op, ";
   protected final String TEXT_455 = ".get";
   protected final String TEXT_456 = "());";
   protected final String TEXT_457 = NL + "\t\t";
   protected final String TEXT_458 = "g";
   protected final String TEXT_459 = " = createEGenericType(";
   protected final String TEXT_460 = ");";
   protected final String TEXT_461 = NL + "\t\tg";
   protected final String TEXT_462 = ".";
   protected final String TEXT_463 = "(g";
   protected final String TEXT_464 = ");";
   protected final String TEXT_465 = NL + "\t\tinitEOperation(op, g1);";
   protected final String TEXT_466 = NL;
   protected final String TEXT_467 = NL + NL + "\t\t// Initialize enums and add enum literals";
   protected final String TEXT_468 = NL + "\t\tinitEEnum(";
   protected final String TEXT_469 = ", ";
   protected final String TEXT_470 = ".class, \"";
   protected final String TEXT_471 = "\");";
   protected final String TEXT_472 = NL + "\t\taddEEnumLiteral(";
   protected final String TEXT_473 = ", ";
   protected final String TEXT_474 = ".";
   protected final String TEXT_475 = ");";
   protected final String TEXT_476 = NL;
   protected final String TEXT_477 = NL + NL + "\t\t// Initialize data types";
   protected final String TEXT_478 = NL + "\t\tinitEDataType(";
   protected final String TEXT_479 = ", ";
   protected final String TEXT_480 = ".class, \"";
   protected final String TEXT_481 = "\", ";
   protected final String TEXT_482 = ", ";
   protected final String TEXT_483 = ", \"";
   protected final String TEXT_484 = "\"";
   protected final String TEXT_485 = ");";
   protected final String TEXT_486 = NL + NL + "\t\t// Create resource" + NL + "\t\tcreateResource(eNS_URI);";
   protected final String TEXT_487 = NL + NL + "\t\t// Create annotations";
   protected final String TEXT_488 = NL + "\t\t// ";
   protected final String TEXT_489 = NL + "\t\tcreate";
   protected final String TEXT_490 = "Annotations();";
   protected final String TEXT_491 = NL + "\t}" + NL;
   protected final String TEXT_492 = NL + "\t/**" + NL + "\t * Initializes the annotations for <b>";
   protected final String TEXT_493 = "</b>." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\tprotected void create";
   protected final String TEXT_494 = "Annotations()" + NL + "\t{" + NL + "\t\tString source = ";
   protected final String TEXT_495 = "null;";
   protected final String TEXT_496 = "\"";
   protected final String TEXT_497 = "\";";
   protected final String TEXT_498 = "\t";
   protected final String TEXT_499 = "\t" + NL + "\t\taddAnnotation" + NL + "\t\t  (";
   protected final String TEXT_500 = ", " + NL + "\t\t   source, " + NL + "\t\t   new String[] " + NL + "\t\t   {";
   protected final String TEXT_501 = NL + "\t\t\t ";
   protected final String TEXT_502 = ", ";
   protected final String TEXT_503 = NL + "\t\t   });";
   protected final String TEXT_504 = NL + "\t\taddAnnotation" + NL + "\t\t  (";
   protected final String TEXT_505 = ", " + NL + "\t\t   ";
   protected final String TEXT_506 = "," + NL + "\t\t   ";
   protected final String TEXT_507 = "null,";
   protected final String TEXT_508 = "\"";
   protected final String TEXT_509 = "\",";
   protected final String TEXT_510 = NL + "\t\t   new String[] " + NL + "\t\t   {";
   protected final String TEXT_511 = NL + "\t\t    ";
   protected final String TEXT_512 = ", ";
   protected final String TEXT_513 = NL + "\t\t   });";
   protected final String TEXT_514 = NL + "\t\taddAnnotation" + NL + "\t\t  (";
   protected final String TEXT_515 = ", " + NL + "\t\t   ";
   protected final String TEXT_516 = "," + NL + "\t\t   ";
   protected final String TEXT_517 = "null,";
   protected final String TEXT_518 = "\"";
   protected final String TEXT_519 = "\",";
   protected final String TEXT_520 = NL + "\t\t   new String[] " + NL + "\t\t   {";
   protected final String TEXT_521 = NL + "\t\t    ";
   protected final String TEXT_522 = ", ";
   protected final String TEXT_523 = NL + "\t\t   });";
   protected final String TEXT_524 = NL + "\t}" + NL;
   protected final String TEXT_525 = NL + "\t/**" + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\tprivate boolean isLoaded = false;" + NL + "" + NL + "\t/**" + NL + "\t * Laods the package and any sub-packages from their serialized form." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\tpublic void loadPackage()" + NL + "\t{" + NL + "\t\tif (isLoaded) return;" + NL + "\t\tisLoaded = true;" + NL + "" + NL + "\t\t";
   protected final String TEXT_526 = " url = getClass().getResource(packageFilename);" + NL + "\t\tif (url == null)" + NL + "\t\t{" + NL + "\t\t\tthrow new RuntimeException(\"Missing serialized package: \" + packageFilename);";
   protected final String TEXT_527 = NL + "\t\t}" + NL + "\t\t";
   protected final String TEXT_528 = " uri = ";
   protected final String TEXT_529 = ".createURI(url.toString());" + NL + "\t\t";
   protected final String TEXT_530 = " resource = new ";
   protected final String TEXT_531 = "().createResource(uri);" + NL + "\t\ttry" + NL + "\t\t{" + NL + "\t\t\tresource.load(null);" + NL + "\t\t}" + NL + "\t\tcatch (";
   protected final String TEXT_532 = " exception)" + NL + "\t\t{" + NL + "\t\t\tthrow new ";
   protected final String TEXT_533 = "(exception);" + NL + "\t\t}" + NL + "\t\tinitializeFromLoadedEPackage(this, (";
   protected final String TEXT_534 = ")resource.getContents().get(0));" + NL + "\t\tcreateResource(eNS_URI);" + NL + "\t}" + NL;
   protected final String TEXT_535 = NL + NL + "\t/**" + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\tprivate boolean isFixed = false;" + NL + "" + NL + "\t/**" + NL + "\t * Fixes up the loaded package, to make it appear as if it had been programmatically built." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\tpublic void fixPackageContents()" + NL + "\t{" + NL + "\t\tif (isFixed) return;" + NL + "\t\tisFixed = true;" + NL + "\t\tfixEClassifiers();" + NL + "\t}" + NL + "" + NL + "\t/**" + NL + "\t * Sets the instance class on the given classifier." + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */";
   protected final String TEXT_536 = NL + "\t@Override";
   protected final String TEXT_537 = NL + "\tprotected void fixInstanceClass(";
   protected final String TEXT_538 = " eClassifier)" + NL + "\t{" + NL + "\t\tif (eClassifier.getInstanceClassName() == null)" + NL + "\t\t{";
   protected final String TEXT_539 = NL + "\t\t\teClassifier.setInstanceClassName(\"";
   protected final String TEXT_540 = ".\" + eClassifier.getName());";
   protected final String TEXT_541 = NL + "\t\t\tsetGeneratedClassName(eClassifier);";
   protected final String TEXT_542 = NL + "\t\t\tswitch (eClassifier.getClassifierID())" + NL + "\t\t\t{";
   protected final String TEXT_543 = NL + "\t\t\t\tcase ";
   protected final String TEXT_544 = ":";
   protected final String TEXT_545 = NL + "\t\t\t\t{" + NL + "\t\t\t\t\tbreak;" + NL + "\t\t\t\t}" + NL + "\t\t\t\tdefault:" + NL + "\t\t\t\t{" + NL + "\t\t\t\t\teClassifier.setInstanceClassName(\"";
   protected final String TEXT_546 = ".\" + eClassifier.getName());";
   protected final String TEXT_547 = NL + "\t\t\t\t\tsetGeneratedClassName(eClassifier);" + NL + "\t\t\t\t\tbreak;" + NL + "\t\t\t\t}" + NL + "\t\t\t}";
   protected final String TEXT_548 = NL + "\t\t}" + NL + "\t}" + NL;
   protected final String TEXT_549 = NL + "\t/**" + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\tprotected ";
   protected final String TEXT_550 = " addEOperation(";
   protected final String TEXT_551 = " owner, ";
   protected final String TEXT_552 = " type, String name, int lowerBound, int upperBound, boolean isUnique, boolean isOrdered)" + NL + "\t{" + NL + "\t\t";
   protected final String TEXT_553 = " o = addEOperation(owner, type, name, lowerBound, upperBound);" + NL + "\t\to.setUnique(isUnique);" + NL + "\t\to.setOrdered(isOrdered);" + NL + "\t\treturn o;" + NL + "\t}" + NL + "\t";
   protected final String TEXT_554 = NL + "\t/**" + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\tprotected ";
   protected final String TEXT_555 = " addEParameter(";
   protected final String TEXT_556 = " owner, ";
   protected final String TEXT_557 = " type, String name, int lowerBound, int upperBound, boolean isUnique, boolean isOrdered)" + NL + "\t{" + NL + "\t\t";
   protected final String TEXT_558 = " p = ecoreFactory.createEParameter();" + NL + "\t\tp.setEType(type);" + NL + "\t\tp.setName(name);" + NL + "\t\tp.setLowerBound(lowerBound);" + NL + "\t\tp.setUpperBound(upperBound);" + NL + "\t\tp.setUnique(isUnique);" + NL + "\t\tp.setOrdered(isOrdered);" + NL + "\t\towner.getEParameters().add(p);" + NL + "\t\treturn p;" + NL + "\t}" + NL + "\t";
   protected final String TEXT_559 = NL + "\t/**" + NL + "\t * <!-- begin-user-doc -->" + NL + "\t * Defines literals for the meta objects that represent" + NL + "\t * <ul>" + NL + "\t *   <li>each class,</li>" + NL + "\t *   <li>each feature of each class,</li>" + NL + "\t *   <li>each enum,</li>" + NL + "\t *   <li>and each data type</li>" + NL + "\t * </ul>" + NL + "\t * <!-- end-user-doc -->" + NL + "\t * @generated" + NL + "\t */" + NL + "\t";
   protected final String TEXT_560 = "public ";
   protected final String TEXT_561 = "interface Literals" + NL + "\t{";
   protected final String TEXT_562 = NL + "\t\t/**";
   protected final String TEXT_563 = NL + "\t\t * The meta object literal for the '{@link ";
   protected final String TEXT_564 = " <em>";
   protected final String TEXT_565 = "</em>}' class." + NL + "\t\t * <!-- begin-user-doc -->" + NL + "\t\t * <!-- end-user-doc -->" + NL + "\t\t * @see ";
   protected final String TEXT_566 = NL + "\t\t * The meta object literal for the '{@link ";
   protected final String TEXT_567 = " <em>";
   protected final String TEXT_568 = "</em>}' class." + NL + "\t\t * <!-- begin-user-doc -->" + NL + "\t\t * <!-- end-user-doc -->" + NL + "\t\t * @see ";
   protected final String TEXT_569 = NL + "\t\t * The meta object literal for the '{@link ";
   protected final String TEXT_570 = " <em>";
   protected final String TEXT_571 = "</em>}' enum." + NL + "\t\t * <!-- begin-user-doc -->" + NL + "\t\t * <!-- end-user-doc -->" + NL + "\t\t * @see ";
   protected final String TEXT_572 = NL + "\t\t * The meta object literal for the '<em>";
   protected final String TEXT_573 = "</em>' data type." + NL + "\t\t * <!-- begin-user-doc -->" + NL + "\t\t * <!-- end-user-doc -->";
   protected final String TEXT_574 = NL + "\t\t * @see ";
   protected final String TEXT_575 = NL + "\t\t * @see ";
   protected final String TEXT_576 = "#get";
   protected final String TEXT_577 = "()" + NL + "\t\t * @generated" + NL + "\t\t */" + NL + "\t\t";
   protected final String TEXT_578 = " ";
   protected final String TEXT_579 = " = eINSTANCE.get";
   protected final String TEXT_580 = "();" + NL;
   protected final String TEXT_581 = NL + "\t\t/**" + NL + "\t\t * The meta object literal for the '<em><b>";
   protected final String TEXT_582 = "</b></em>' ";
   protected final String TEXT_583 = " feature." + NL + "\t\t * <!-- begin-user-doc -->" + NL + "\t\t * <!-- end-user-doc -->" + NL + "\t\t * @generated" + NL + "\t\t */" + NL + "\t\t";
   protected final String TEXT_584 = " ";
   protected final String TEXT_585 = " = eINSTANCE.get";
   protected final String TEXT_586 = "();" + NL;
   protected final String TEXT_587 = NL + "\t}" + NL;
   protected final String TEXT_588 = NL + "} //";
   protected final String TEXT_589 = NL;
 
   public String generate(Object argument)
   {
     final StringBuffer stringBuffer = new StringBuffer();
     
 /**
  * <copyright>
  *
  * Copyright (c) 2002-2006 IBM Corporation and others.
  * All rights reserved.   This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *   IBM - Initial API and implementation
  *
  * </copyright>
  */
 
     final GenPackage genPackage = (GenPackage)((Object[])argument)[0]; final GenModel genModel=genPackage.getGenModel();
     boolean isInterface = Boolean.TRUE.equals(((Object[])argument)[1]); boolean isImplementation = Boolean.TRUE.equals(((Object[])argument)[2]);
     String publicStaticFinalFlag = isImplementation ? "public static final " : "";
     boolean needsAddEOperation = false;
     boolean needsAddEParameter = false;
     stringBuffer.append(TEXT_1);
     stringBuffer.append(TEXT_2);
     {GenBase copyrightHolder = argument instanceof GenBase ? (GenBase)argument : argument instanceof Object[] && ((Object[])argument)[0] instanceof GenBase ? (GenBase)((Object[])argument)[0] : null;
     if (copyrightHolder != null && copyrightHolder.hasCopyright()) {
     stringBuffer.append(TEXT_3);
     stringBuffer.append(copyrightHolder.getCopyright(copyrightHolder.getGenModel().getIndentation(stringBuffer)));
     } else {
     stringBuffer.append(TEXT_4);
     }}
     stringBuffer.append(TEXT_5);
     stringBuffer.append("$");
     stringBuffer.append(TEXT_6);
     stringBuffer.append("$");
     stringBuffer.append(TEXT_7);
     if (isImplementation && !genModel.isSuppressInterfaces()) {
     stringBuffer.append(TEXT_8);
     stringBuffer.append(genPackage.getClassPackageName());
     stringBuffer.append(TEXT_9);
     } else {
     stringBuffer.append(TEXT_10);
     stringBuffer.append(genPackage.getReflectionPackageName());
     stringBuffer.append(TEXT_11);
     }
     stringBuffer.append(TEXT_12);
     genModel.markImportLocation(stringBuffer, genPackage);
     if (isImplementation) {
     genModel.addPseudoImport("org.eclipse.emf.ecore.EPackage.Registry");
     genModel.addPseudoImport("org.eclipse.emf.ecore.EPackage.Descriptor");
     if (genPackage.isLiteralsInterface()) {
     genModel.addPseudoImport(genPackage.getQualifiedPackageInterfaceName() + ".Literals");
     }
     for (GenClassifier genClassifier : genPackage.getOrderedGenClassifiers()) genModel.addPseudoImport(genPackage.getQualifiedPackageInterfaceName() + "." + genPackage.getClassifierID(genClassifier));
     }
     if (isInterface) {
     stringBuffer.append(TEXT_13);
     if (genPackage.hasDocumentation()) {
     stringBuffer.append(TEXT_14);
     stringBuffer.append(genPackage.getDocumentation(genModel.getIndentation(stringBuffer)));
     stringBuffer.append(TEXT_15);
     }
     stringBuffer.append(TEXT_16);
     stringBuffer.append(genPackage.getQualifiedFactoryInterfaceName());
     if (!genModel.isSuppressEMFModelTags()) { boolean first = true; for (StringTokenizer stringTokenizer = new StringTokenizer(genPackage.getModelInfo(), "\n\r"); stringTokenizer.hasMoreTokens(); ) { String modelInfo = stringTokenizer.nextToken(); if (first) { first = false;
     stringBuffer.append(TEXT_17);
     stringBuffer.append(modelInfo);
     } else {
     stringBuffer.append(TEXT_18);
     stringBuffer.append(modelInfo);
     }} if (first) {
     stringBuffer.append(TEXT_19);
     }}
     stringBuffer.append(TEXT_20);
     } else {
     stringBuffer.append(TEXT_21);
     }
     if (isImplementation) {
     stringBuffer.append(TEXT_22);
     stringBuffer.append(genPackage.getPackageClassName());
     stringBuffer.append(TEXT_23);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.impl.EPackageImpl"));
     if (!isInterface){
     stringBuffer.append(TEXT_24);
     stringBuffer.append(genPackage.getImportedPackageInterfaceName());
     }
     } else {
     stringBuffer.append(TEXT_25);
     stringBuffer.append(genPackage.getPackageInterfaceName());
     stringBuffer.append(TEXT_26);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.EPackage"));
     }
     stringBuffer.append(TEXT_27);
     if (genModel.hasCopyrightField()) {
     stringBuffer.append(TEXT_28);
     stringBuffer.append(publicStaticFinalFlag);
     stringBuffer.append(genModel.getImportedName("java.lang.String"));
     stringBuffer.append(TEXT_29);
     stringBuffer.append(genModel.getCopyrightFieldLiteral());
     stringBuffer.append(TEXT_30);
     stringBuffer.append(genModel.getNonNLS());
     stringBuffer.append(TEXT_31);
     }
     if (isInterface) {
     stringBuffer.append(TEXT_32);
     stringBuffer.append(publicStaticFinalFlag);
     stringBuffer.append(genModel.getImportedName("java.lang.String"));
     stringBuffer.append(TEXT_33);
     stringBuffer.append(genPackage.getPackageName());
     stringBuffer.append(TEXT_34);
     stringBuffer.append(genModel.getNonNLS());
     stringBuffer.append(TEXT_35);
     stringBuffer.append(publicStaticFinalFlag);
     stringBuffer.append(genModel.getImportedName("java.lang.String"));
     stringBuffer.append(TEXT_36);
     stringBuffer.append(genPackage.getNSURI());
     stringBuffer.append(TEXT_37);
     stringBuffer.append(genModel.getNonNLS());
     stringBuffer.append(TEXT_38);
     stringBuffer.append(publicStaticFinalFlag);
     stringBuffer.append(genModel.getImportedName("java.lang.String"));
     stringBuffer.append(TEXT_39);
     stringBuffer.append(genPackage.getNSName());
     stringBuffer.append(TEXT_40);
     stringBuffer.append(genModel.getNonNLS());
     if (genPackage.isContentType()) {
     stringBuffer.append(TEXT_41);
     stringBuffer.append(publicStaticFinalFlag);
     stringBuffer.append(genModel.getImportedName("java.lang.String"));
     stringBuffer.append(TEXT_42);
     stringBuffer.append(genPackage.getContentTypeIdentifier());
     stringBuffer.append(TEXT_43);
     stringBuffer.append(genModel.getNonNLS());
     }
     stringBuffer.append(TEXT_44);
     stringBuffer.append(publicStaticFinalFlag);
     stringBuffer.append(genPackage.getPackageInterfaceName());
     stringBuffer.append(TEXT_45);
     stringBuffer.append(genPackage.getQualifiedPackageClassName());
     stringBuffer.append(TEXT_46);
     for (GenClassifier genClassifier : genPackage.getOrderedGenClassifiers()) {
     stringBuffer.append(TEXT_47);
     if (genClassifier instanceof GenClass) { GenClass genClass = (GenClass)genClassifier;
     if (!genClass.isInterface()) {
     stringBuffer.append(TEXT_48);
     stringBuffer.append(genClass.getQualifiedClassName());
     stringBuffer.append(TEXT_49);
     stringBuffer.append(genClass.getFormattedName());
     stringBuffer.append(TEXT_50);
     stringBuffer.append(genClass.getQualifiedClassName());
     } else {
     stringBuffer.append(TEXT_51);
     stringBuffer.append(genClass.getQualifiedInterfaceName());
     stringBuffer.append(TEXT_52);
     stringBuffer.append(genClass.getFormattedName());
     stringBuffer.append(TEXT_53);
     stringBuffer.append(genClass.getQualifiedInterfaceName());
     }
     } else if (genClassifier instanceof GenEnum) { GenEnum genEnum = (GenEnum)genClassifier;
     stringBuffer.append(TEXT_54);
     stringBuffer.append(genEnum.getQualifiedName());
     stringBuffer.append(TEXT_55);
     stringBuffer.append(genEnum.getFormattedName());
     stringBuffer.append(TEXT_56);
     stringBuffer.append(genEnum.getQualifiedName());
     } else if (genClassifier instanceof GenDataType) { GenDataType genDataType = (GenDataType)genClassifier;
     stringBuffer.append(TEXT_57);
     stringBuffer.append(genDataType.getFormattedName());
     stringBuffer.append(TEXT_58);
     if (!genDataType.isPrimitiveType() && !genDataType.isArrayType()) {
     stringBuffer.append(TEXT_59);
     stringBuffer.append(genDataType.getRawInstanceClassName());
     }
     }
     stringBuffer.append(TEXT_60);
     stringBuffer.append(genPackage.getQualifiedPackageClassName());
     stringBuffer.append(TEXT_61);
     stringBuffer.append(genClassifier.getClassifierAccessorName());
     stringBuffer.append(TEXT_62);
     stringBuffer.append(publicStaticFinalFlag);
     stringBuffer.append(TEXT_63);
     stringBuffer.append(genPackage.getClassifierID(genClassifier));
     stringBuffer.append(TEXT_64);
     stringBuffer.append(genPackage.getClassifierValue(genClassifier));
     stringBuffer.append(TEXT_65);
     if (genClassifier instanceof GenClass) { GenClass genClass = (GenClass)genClassifier;
     for (GenFeature genFeature : genClass.getAllGenFeatures()) {
     stringBuffer.append(TEXT_66);
     stringBuffer.append(genFeature.getFormattedName());
     stringBuffer.append(TEXT_67);
     stringBuffer.append(genFeature.getFeatureKind());
     stringBuffer.append(TEXT_68);
     stringBuffer.append(publicStaticFinalFlag);
     stringBuffer.append(TEXT_69);
     stringBuffer.append(genClass.getFeatureID(genFeature));
     stringBuffer.append(TEXT_70);
     stringBuffer.append(genClass.getFeatureValue(genFeature));
     stringBuffer.append(TEXT_71);
     }
     stringBuffer.append(TEXT_72);
     stringBuffer.append(genClass.getFormattedName());
     stringBuffer.append(TEXT_73);
     stringBuffer.append(publicStaticFinalFlag);
     stringBuffer.append(TEXT_74);
     stringBuffer.append(genClass.getFeatureCountID());
     stringBuffer.append(TEXT_75);
     stringBuffer.append(genClass.getFeatureCountValue());
     stringBuffer.append(TEXT_76);
     }
     }
     }
     if (isImplementation) {
     if (genPackage.isLoadingInitialization()) {
     stringBuffer.append(TEXT_77);
     stringBuffer.append(genPackage.getSerializedPackageFilename());
     stringBuffer.append(TEXT_78);
     stringBuffer.append(genModel.getNonNLS());
     stringBuffer.append(TEXT_79);
     }
     for (GenClassifier genClassifier : genPackage.getGenClassifiers()) {
     stringBuffer.append(TEXT_80);
     stringBuffer.append(genClassifier.getImportedMetaType());
     stringBuffer.append(TEXT_81);
     stringBuffer.append(genClassifier.getClassifierInstanceName());
     stringBuffer.append(TEXT_82);
     }
     stringBuffer.append(TEXT_83);
     stringBuffer.append(genPackage.getQualifiedPackageInterfaceName());
     stringBuffer.append(TEXT_84);
     stringBuffer.append(genPackage.getPackageClassName());
     stringBuffer.append(TEXT_85);
     stringBuffer.append(genPackage.getQualifiedEFactoryInstanceAccessor());
     stringBuffer.append(TEXT_86);
     if (!genPackage.isLoadedInitialization()) {
     stringBuffer.append(TEXT_87);
     }
     stringBuffer.append(TEXT_88);
     stringBuffer.append(genPackage.getImportedPackageInterfaceName());
     stringBuffer.append(TEXT_89);
     stringBuffer.append(genPackage.getImportedPackageInterfaceName());
     stringBuffer.append(TEXT_90);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.EPackage"));
     stringBuffer.append(TEXT_91);
     stringBuffer.append(genPackage.getImportedPackageInterfaceName());
     stringBuffer.append(TEXT_92);
     stringBuffer.append(genPackage.getPackageClassName());
     stringBuffer.append(TEXT_93);
    stringBuffer.append(genPackage.getBasicPackageName());
     stringBuffer.append(TEXT_94);
     stringBuffer.append(genPackage.getPackageClassName());
     stringBuffer.append(TEXT_95);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.EPackage"));
     stringBuffer.append(TEXT_96);
     stringBuffer.append(genPackage.getPackageClassName());
     stringBuffer.append(TEXT_97);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.EPackage"));
     stringBuffer.append(TEXT_98);
     stringBuffer.append(genPackage.getPackageClassName());
     stringBuffer.append(TEXT_99);
     if (!genPackage.getPackageSimpleDependencies().isEmpty()) {
     stringBuffer.append(TEXT_100);
     for (GenPackage dep : genPackage.getPackageSimpleDependencies()) {
     stringBuffer.append(TEXT_101);
     stringBuffer.append(dep.getImportedPackageInterfaceName());
     stringBuffer.append(TEXT_102);
     }
     stringBuffer.append(TEXT_103);
     }
     if (!genPackage.getPackageInterDependencies().isEmpty()) {
     stringBuffer.append(TEXT_104);
     for (GenPackage interdep : genPackage.getPackageInterDependencies()) {
     stringBuffer.append(TEXT_105);
     stringBuffer.append(interdep.getImportedPackageClassName());
     stringBuffer.append(TEXT_106);
     stringBuffer.append(genPackage.getPackageInstanceVariable(interdep));
     stringBuffer.append(TEXT_107);
     stringBuffer.append(interdep.getImportedPackageClassName());
     stringBuffer.append(TEXT_108);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.EPackage"));
     stringBuffer.append(TEXT_109);
     stringBuffer.append(interdep.getImportedPackageInterfaceName());
     stringBuffer.append(TEXT_110);
     stringBuffer.append(interdep.getImportedPackageClassName());
     stringBuffer.append(TEXT_111);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.EPackage"));
     stringBuffer.append(TEXT_112);
     stringBuffer.append(interdep.getImportedPackageInterfaceName());
     stringBuffer.append(TEXT_113);
     stringBuffer.append(interdep.getImportedPackageInterfaceName());
     stringBuffer.append(TEXT_114);
     }
     stringBuffer.append(TEXT_115);
     }
     if (genPackage.isLoadedInitialization() || !genPackage.getPackageLoadInterDependencies().isEmpty()) {
     stringBuffer.append(TEXT_116);
     if (genPackage.isLoadingInitialization()) {
     stringBuffer.append(TEXT_117);
    stringBuffer.append(genPackage.getBasicPackageName());
     stringBuffer.append(TEXT_118);
     }
     for (GenPackage interdep : genPackage.getPackageLoadInterDependencies()) {
     if (interdep.isLoadingInitialization()) {
     stringBuffer.append(TEXT_119);
     stringBuffer.append(genPackage.getPackageInstanceVariable(interdep));
     stringBuffer.append(TEXT_120);
     }
     }
     stringBuffer.append(TEXT_121);
     }
     if (!genPackage.isLoadedInitialization() || !genPackage.getPackageBuildInterDependencies().isEmpty()) {
     stringBuffer.append(TEXT_122);
     if (!genPackage.isLoadedInitialization()) {
     stringBuffer.append(TEXT_123);
    stringBuffer.append(genPackage.getBasicPackageName());
     stringBuffer.append(TEXT_124);
     }
     for (GenPackage interdep : genPackage.getPackageBuildInterDependencies()) {
     stringBuffer.append(TEXT_125);
     stringBuffer.append(genPackage.getPackageInstanceVariable(interdep));
     stringBuffer.append(TEXT_126);
     }
     stringBuffer.append(TEXT_127);
     if (!genPackage.isLoadedInitialization()) {
     stringBuffer.append(TEXT_128);
    stringBuffer.append(genPackage.getBasicPackageName());
     stringBuffer.append(TEXT_129);
     }
     for (GenPackage interdep : genPackage.getPackageBuildInterDependencies()) {
     stringBuffer.append(TEXT_130);
     stringBuffer.append(genPackage.getPackageInstanceVariable(interdep));
     stringBuffer.append(TEXT_131);
     }
     stringBuffer.append(TEXT_132);
     }
     if (genPackage.isLoadedInitialization() || !genPackage.getPackageLoadInterDependencies().isEmpty()) {
     stringBuffer.append(TEXT_133);
     if (genPackage.isLoadedInitialization()) {
     stringBuffer.append(TEXT_134);
    stringBuffer.append(genPackage.getBasicPackageName());
     stringBuffer.append(TEXT_135);
     }
     for (GenPackage interdep : genPackage.getPackageLoadInterDependencies()) {
     stringBuffer.append(TEXT_136);
     stringBuffer.append(genPackage.getPackageInstanceVariable(interdep));
     stringBuffer.append(TEXT_137);
     }
     stringBuffer.append(TEXT_138);
     }
     if (genPackage.hasConstraints()) {
     stringBuffer.append(TEXT_139);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.EValidator"));
     stringBuffer.append(TEXT_140);
    stringBuffer.append(genPackage.getBasicPackageName());
     stringBuffer.append(TEXT_141);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.EValidator"));
     stringBuffer.append(TEXT_142);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.EValidator"));
     stringBuffer.append(TEXT_143);
     stringBuffer.append(genPackage.getImportedValidatorClassName());
     stringBuffer.append(TEXT_144);
     }
     if (!genPackage.isEcorePackage()) {
     stringBuffer.append(TEXT_145);
    stringBuffer.append(genPackage.getBasicPackageName());
     stringBuffer.append(TEXT_146);
     }
     stringBuffer.append(TEXT_147);
    stringBuffer.append(genPackage.getBasicPackageName());
     stringBuffer.append(TEXT_148);
     }
     if (isInterface) { // TODO REMOVE THIS BOGUS EMPTY LINE
     stringBuffer.append(TEXT_149);
     }
     for (GenClassifier genClassifier : genPackage.getGenClassifiers()) {
     if (isInterface) {
     stringBuffer.append(TEXT_150);
     if (genClassifier instanceof GenClass) { GenClass genClass = (GenClass)genClassifier;
     stringBuffer.append(TEXT_151);
     stringBuffer.append(genClass.getQualifiedInterfaceName());
     stringBuffer.append(TEXT_152);
     stringBuffer.append(genClass.getFormattedName());
     stringBuffer.append(TEXT_153);
     stringBuffer.append(genClass.getFormattedName());
     stringBuffer.append(TEXT_154);
     stringBuffer.append(genClass.getQualifiedInterfaceName());
     if (!genModel.isSuppressEMFModelTags() && (genClass.isExternalInterface() || genClass.isDynamic())) { boolean first = true; for (StringTokenizer stringTokenizer = new StringTokenizer(genClass.getModelInfo(), "\n\r"); stringTokenizer.hasMoreTokens(); ) { String modelInfo = stringTokenizer.nextToken(); if (first) { first = false;
     stringBuffer.append(TEXT_155);
     stringBuffer.append(modelInfo);
     } else {
     stringBuffer.append(TEXT_156);
     stringBuffer.append(modelInfo);
     }} if (first) {
     stringBuffer.append(TEXT_157);
     }}
     } else if (genClassifier instanceof GenEnum) { GenEnum genEnum = (GenEnum)genClassifier;
     stringBuffer.append(TEXT_158);
     stringBuffer.append(genEnum.getQualifiedName());
     stringBuffer.append(TEXT_159);
     stringBuffer.append(genEnum.getFormattedName());
     stringBuffer.append(TEXT_160);
     stringBuffer.append(genEnum.getFormattedName());
     stringBuffer.append(TEXT_161);
     stringBuffer.append(genEnum.getQualifiedName());
     } else if (genClassifier instanceof GenDataType) { GenDataType genDataType = (GenDataType)genClassifier;
     if (genDataType.isPrimitiveType() || genDataType.isArrayType()) {
     stringBuffer.append(TEXT_162);
     stringBuffer.append(genDataType.getFormattedName());
     stringBuffer.append(TEXT_163);
     } else {
     stringBuffer.append(TEXT_164);
     stringBuffer.append(genDataType.getRawInstanceClassName());
     stringBuffer.append(TEXT_165);
     stringBuffer.append(genDataType.getFormattedName());
     stringBuffer.append(TEXT_166);
     }
     stringBuffer.append(TEXT_167);
     stringBuffer.append(genDataType.getFormattedName());
     stringBuffer.append(TEXT_168);
     if (!genDataType.isPrimitiveType() && !genDataType.isArrayType()) {
     stringBuffer.append(TEXT_169);
     stringBuffer.append(genDataType.getRawInstanceClassName());
     }
     if (!genModel.isSuppressEMFModelTags()) {boolean first = true; for (StringTokenizer stringTokenizer = new StringTokenizer(genDataType.getModelInfo(), "\n\r"); stringTokenizer.hasMoreTokens(); ) { String modelInfo = stringTokenizer.nextToken(); if (first) { first = false;
     stringBuffer.append(TEXT_170);
     stringBuffer.append(modelInfo);
     } else {
     stringBuffer.append(TEXT_171);
     stringBuffer.append(modelInfo);
     }} if (first) {
     stringBuffer.append(TEXT_172);
     }}
     }
     stringBuffer.append(TEXT_173);
     } else {
     stringBuffer.append(TEXT_174);
     }
     if (isImplementation) {
     stringBuffer.append(TEXT_175);
     stringBuffer.append(genClassifier.getImportedMetaType());
     stringBuffer.append(TEXT_176);
     stringBuffer.append(genClassifier.getClassifierAccessorName());
     stringBuffer.append(TEXT_177);
     if (genPackage.isLoadedInitialization()) {
     stringBuffer.append(TEXT_178);
     stringBuffer.append(genClassifier.getClassifierInstanceName());
     stringBuffer.append(TEXT_179);
     stringBuffer.append(genClassifier.getClassifierInstanceName());
     stringBuffer.append(TEXT_180);
     stringBuffer.append(genClassifier.getImportedMetaType());
     stringBuffer.append(TEXT_181);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.EPackage"));
     stringBuffer.append(TEXT_182);
     stringBuffer.append(genPackage.getImportedPackageInterfaceName());
     stringBuffer.append(TEXT_183);
     stringBuffer.append(genPackage.getLocalClassifierIndex(genClassifier));
     stringBuffer.append(TEXT_184);
     }
     stringBuffer.append(TEXT_185);
     stringBuffer.append(genClassifier.getClassifierInstanceName());
     stringBuffer.append(TEXT_186);
     } else {
     stringBuffer.append(TEXT_187);
     stringBuffer.append(genClassifier.getImportedMetaType());
     stringBuffer.append(TEXT_188);
     stringBuffer.append(genClassifier.getClassifierAccessorName());
     stringBuffer.append(TEXT_189);
     }
     if (genClassifier instanceof GenClass) { GenClass genClass = (GenClass)genClassifier;
     for (GenFeature genFeature : genClass.getGenFeatures()) {
     if (isInterface) {
     stringBuffer.append(TEXT_190);
     stringBuffer.append(genFeature.getFeatureKind());
     stringBuffer.append(TEXT_191);
     stringBuffer.append(genClass.getQualifiedInterfaceName());
     if (!genClass.isMapEntry() && !genFeature.isSuppressedGetVisibility()) {
     stringBuffer.append(TEXT_192);
     stringBuffer.append(genFeature.getGetAccessor());
     }
     stringBuffer.append(TEXT_193);
     stringBuffer.append(genFeature.getFormattedName());
     stringBuffer.append(TEXT_194);
     stringBuffer.append(genFeature.getFeatureKind());
     stringBuffer.append(TEXT_195);
     stringBuffer.append(genFeature.getFormattedName());
     stringBuffer.append(TEXT_196);
     stringBuffer.append(genClass.getQualifiedInterfaceName());
     if (!genClass.isMapEntry() && !genFeature.isSuppressedGetVisibility()) {
     stringBuffer.append(TEXT_197);
     stringBuffer.append(genFeature.getGetAccessor());
     stringBuffer.append(TEXT_198);
     }
     stringBuffer.append(TEXT_199);
     stringBuffer.append(genClass.getClassifierAccessorName());
     stringBuffer.append(TEXT_200);
     } else {
     stringBuffer.append(TEXT_201);
     }
     if (isImplementation) {
     stringBuffer.append(TEXT_202);
     stringBuffer.append(genFeature.getImportedMetaType());
     stringBuffer.append(TEXT_203);
     stringBuffer.append(genFeature.getFeatureAccessorName());
     stringBuffer.append(TEXT_204);
     if (!genPackage.isLoadedInitialization()) {
     stringBuffer.append(TEXT_205);
     stringBuffer.append(genFeature.getImportedMetaType());
     stringBuffer.append(TEXT_206);
     stringBuffer.append(genClass.getClassifierInstanceName());
     stringBuffer.append(TEXT_207);
     stringBuffer.append(genClass.getLocalFeatureIndex(genFeature));
     stringBuffer.append(TEXT_208);
     } else {
     stringBuffer.append(TEXT_209);
     stringBuffer.append(genFeature.getImportedMetaType());
     stringBuffer.append(TEXT_210);
     stringBuffer.append(genClassifier.getClassifierAccessorName());
     stringBuffer.append(TEXT_211);
     stringBuffer.append(genClass.getLocalFeatureIndex(genFeature));
     stringBuffer.append(TEXT_212);
     }
     stringBuffer.append(TEXT_213);
     } else {
     stringBuffer.append(TEXT_214);
     stringBuffer.append(genFeature.getImportedMetaType());
     stringBuffer.append(TEXT_215);
     stringBuffer.append(genFeature.getFeatureAccessorName());
     stringBuffer.append(TEXT_216);
     }
     stringBuffer.append(TEXT_217);
     }
     }
     }
     if (isInterface) {
     stringBuffer.append(TEXT_218);
     } else {
     stringBuffer.append(TEXT_219);
     }
     if (isImplementation) {
     stringBuffer.append(TEXT_220);
     stringBuffer.append(genPackage.getImportedFactoryInterfaceName());
     stringBuffer.append(TEXT_221);
    stringBuffer.append(genPackage.getFactoryName());
     stringBuffer.append(TEXT_222);
     stringBuffer.append(genPackage.getImportedFactoryInterfaceName());
     stringBuffer.append(TEXT_223);
     } else {
     stringBuffer.append(TEXT_224);
     stringBuffer.append(genPackage.getFactoryInterfaceName());
     stringBuffer.append(TEXT_225);
    stringBuffer.append(genPackage.getFactoryName());
     stringBuffer.append(TEXT_226);
     }
     stringBuffer.append(TEXT_227);
     if (isImplementation) {
     if (!genPackage.isLoadedInitialization()) {
     stringBuffer.append(TEXT_228);
     if (!genPackage.getGenClasses().isEmpty()) {
     stringBuffer.append(TEXT_229);
     for (Iterator<GenClass> c=genPackage.getGenClasses().iterator(); c.hasNext();) { GenClass genClass = c.next();
     stringBuffer.append(TEXT_230);
     stringBuffer.append(genClass.getClassifierInstanceName());
     stringBuffer.append(TEXT_231);
     stringBuffer.append(genClass.getMetaType());
     stringBuffer.append(TEXT_232);
     stringBuffer.append(genClass.getClassifierID());
     stringBuffer.append(TEXT_233);
     for (GenFeature genFeature : genClass.getGenFeatures()) {
     stringBuffer.append(TEXT_234);
     stringBuffer.append(genFeature.getMetaType());
     stringBuffer.append(TEXT_235);
     stringBuffer.append(genClass.getClassifierInstanceName());
     stringBuffer.append(TEXT_236);
     stringBuffer.append(genClass.getFeatureID(genFeature));
     stringBuffer.append(TEXT_237);
     }
     if (c.hasNext()) {
     stringBuffer.append(TEXT_238);
     }
     }
     }
     if (!genPackage.getGenEnums().isEmpty()) {
     stringBuffer.append(TEXT_239);
     for (GenEnum genEnum : genPackage.getGenEnums()) {
     stringBuffer.append(TEXT_240);
     stringBuffer.append(genEnum.getClassifierInstanceName());
     stringBuffer.append(TEXT_241);
     stringBuffer.append(genEnum.getClassifierID());
     stringBuffer.append(TEXT_242);
     }
     }
     if (!genPackage.getGenDataTypes().isEmpty()) {
     stringBuffer.append(TEXT_243);
     for (GenDataType genDataType : genPackage.getGenDataTypes()) {
     stringBuffer.append(TEXT_244);
     stringBuffer.append(genDataType.getClassifierInstanceName());
     stringBuffer.append(TEXT_245);
     stringBuffer.append(genDataType.getClassifierID());
     stringBuffer.append(TEXT_246);
     }
     }
     stringBuffer.append(TEXT_247);
     
 ///////////////////////
 class Information
 {
   @SuppressWarnings("unused")
   EGenericType eGenericType;
   int depth;
   String type;
   String accessor;
 }
 
 class InformationIterator
 {
   Iterator<?> iterator;
   InformationIterator(EGenericType eGenericType)
   {
     iterator = EcoreUtil.getAllContents(Collections.singleton(eGenericType));
   }
 
   boolean hasNext()
   {
     return iterator.hasNext();
   }
 
   Information next()
   {
     Information information = new Information();
     EGenericType eGenericType = information.eGenericType = (EGenericType)iterator.next();
     for (EObject container = eGenericType.eContainer(); container instanceof EGenericType; container = container.eContainer())
     {
       ++information.depth;
     }
     if (eGenericType.getEClassifier() != null )
     {
       GenClassifier genClassifier = genModel.findGenClassifier(eGenericType.getEClassifier());
       information.type = genPackage.getPackageInstanceVariable(genClassifier.getGenPackage()) + ".get" + genClassifier.getClassifierAccessorName() + "()";
     }
     else if (eGenericType.getETypeParameter() != null)
     {
       ETypeParameter eTypeParameter = eGenericType.getETypeParameter();
       if (eTypeParameter.eContainer() instanceof EClass)
       {
         information.type = genModel.findGenClassifier((EClass)eTypeParameter.eContainer()).getClassifierInstanceName() + "_" + eGenericType.getETypeParameter().getName();
       }
       else
       {
         information.type = "t" + (((EOperation)eTypeParameter.eContainer()).getETypeParameters().indexOf(eTypeParameter) + 1);
       }
     }
     else
     {
       information.type ="";
     }
     if (information.depth > 0)
     {
       if (eGenericType.eContainmentFeature().isMany())
       {
         information.accessor = "getE" + eGenericType.eContainmentFeature().getName().substring(1) + "().add";
       }
       else
       {
         information.accessor = "setE" + eGenericType.eContainmentFeature().getName().substring(1);
       }
     }
     return information;
   }
 }
 ///////////////////////
 int maxGenericTypeAssignment = 0;
 
     stringBuffer.append(TEXT_248);
     if (!genPackage.getPackageInitializationDependencies().isEmpty()) {
     stringBuffer.append(TEXT_249);
     for (GenPackage dep : genPackage.getPackageInitializationDependencies()) {
     stringBuffer.append(TEXT_250);
     stringBuffer.append(dep.getImportedPackageInterfaceName());
     stringBuffer.append(TEXT_251);
     stringBuffer.append(genPackage.getPackageInstanceVariable(dep));
     stringBuffer.append(TEXT_252);
     stringBuffer.append(dep.getImportedPackageInterfaceName());
     stringBuffer.append(TEXT_253);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.EPackage"));
     stringBuffer.append(TEXT_254);
     stringBuffer.append(dep.getImportedPackageInterfaceName());
     stringBuffer.append(TEXT_255);
     }
     }
     if (!genPackage.getSubGenPackages().isEmpty()) {
     stringBuffer.append(TEXT_256);
     for (GenPackage sub : genPackage.getSubGenPackages()) {
     stringBuffer.append(TEXT_257);
     stringBuffer.append(genPackage.getPackageInstanceVariable(sub));
     stringBuffer.append(TEXT_258);
     }
     }
     if (!genPackage.getGenClasses().isEmpty()) { boolean firstOperationAssignment = true; int maxTypeParameterAssignment = 0;
     if (genModel.useGenerics()) {
     stringBuffer.append(TEXT_259);
     for (GenClassifier genClassifier : genPackage.getGenClassifiers()) {
     for (GenTypeParameter genTypeParameter : genClassifier.getGenTypeParameters()) {
     if (!genTypeParameter.getEcoreTypeParameter().getEBounds().isEmpty() || genTypeParameter.isUsed()) {
     stringBuffer.append(TEXT_260);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.ETypeParameter"));
     stringBuffer.append(TEXT_261);
     stringBuffer.append(genClassifier.getClassifierInstanceName());
     stringBuffer.append(TEXT_262);
     stringBuffer.append(genTypeParameter.getName());
     stringBuffer.append(TEXT_263);
     stringBuffer.append(genClassifier.getClassifierInstanceName());
     stringBuffer.append(TEXT_264);
     stringBuffer.append(genTypeParameter.getName());
     stringBuffer.append(TEXT_265);
     stringBuffer.append(genModel.getNonNLS());
     } else {
     stringBuffer.append(TEXT_266);
     stringBuffer.append(genClassifier.getClassifierInstanceName());
     stringBuffer.append(TEXT_267);
     stringBuffer.append(genTypeParameter.getName());
     stringBuffer.append(TEXT_268);
     stringBuffer.append(genModel.getNonNLS());
     }
     }
     }
     }
     if (genModel.useGenerics()) {
     stringBuffer.append(TEXT_269);
     for (GenClassifier genClassifier : genPackage.getGenClassifiers()) {
     for (GenTypeParameter genTypeParameter : genClassifier.getGenTypeParameters()) {
     for (EGenericType bound : genTypeParameter.getEcoreTypeParameter().getEBounds()) {
     for (InformationIterator i=new InformationIterator(bound); i.hasNext(); ) { Information info = i.next(); String prefix = ""; if (maxGenericTypeAssignment <= info.depth) { ++maxGenericTypeAssignment; prefix = genModel.getImportedName("org.eclipse.emf.ecore.EGenericType") + " "; }
     stringBuffer.append(TEXT_270);
     stringBuffer.append(prefix);
     stringBuffer.append(TEXT_271);
     stringBuffer.append(info.depth + 1);
     stringBuffer.append(TEXT_272);
     stringBuffer.append(info.type);
     stringBuffer.append(TEXT_273);
     if (info.depth > 0) {
     stringBuffer.append(TEXT_274);
     stringBuffer.append(info.depth);
     stringBuffer.append(TEXT_275);
     stringBuffer.append(info.accessor);
     stringBuffer.append(TEXT_276);
     stringBuffer.append(info.depth + 1);
     stringBuffer.append(TEXT_277);
     }
     }
     stringBuffer.append(TEXT_278);
     stringBuffer.append(genClassifier.getClassifierInstanceName());
     stringBuffer.append(TEXT_279);
     stringBuffer.append(genTypeParameter.getName());
     stringBuffer.append(TEXT_280);
     }
     }
     }
     }
     stringBuffer.append(TEXT_281);
     for (GenClass genClass : genPackage.getGenClasses()) {
     if (!genClass.hasGenericSuperTypes()) {
     for (GenClass baseGenClass : genClass.getBaseGenClasses()) {
     stringBuffer.append(TEXT_282);
     stringBuffer.append(genClass.getClassifierInstanceName());
     stringBuffer.append(TEXT_283);
     stringBuffer.append(genPackage.getPackageInstanceVariable(baseGenClass.getGenPackage()));
     stringBuffer.append(TEXT_284);
     stringBuffer.append(baseGenClass.getClassifierAccessorName());
     stringBuffer.append(TEXT_285);
     }
     } else {
     for (EGenericType superType : genClass.getEcoreClass().getEGenericSuperTypes()) {
     for (InformationIterator i=new InformationIterator(superType); i.hasNext(); ) { Information info = i.next(); String prefix = ""; if (maxGenericTypeAssignment <= info.depth) { ++maxGenericTypeAssignment; prefix = genModel.getImportedName("org.eclipse.emf.ecore.EGenericType") + " "; }
     stringBuffer.append(TEXT_286);
     stringBuffer.append(prefix);
     stringBuffer.append(TEXT_287);
     stringBuffer.append(info.depth + 1);
     stringBuffer.append(TEXT_288);
     stringBuffer.append(info.type);
     stringBuffer.append(TEXT_289);
     if (info.depth > 0) {
     stringBuffer.append(TEXT_290);
     stringBuffer.append(info.depth);
     stringBuffer.append(TEXT_291);
     stringBuffer.append(info.accessor);
     stringBuffer.append(TEXT_292);
     stringBuffer.append(info.depth + 1);
     stringBuffer.append(TEXT_293);
     }
     }
     stringBuffer.append(TEXT_294);
     stringBuffer.append(genClass.getClassifierInstanceName());
     stringBuffer.append(TEXT_295);
     }
     }
     }
     stringBuffer.append(TEXT_296);
     for (Iterator<GenClass> c=genPackage.getGenClasses().iterator(); c.hasNext();) { GenClass genClass = c.next(); boolean hasInstanceTypeName = genModel.useGenerics() && genClass.getEcoreClass().getInstanceTypeName() != null && genClass.getEcoreClass().getInstanceTypeName().contains("<");
     stringBuffer.append(TEXT_297);
     stringBuffer.append(genClass.getClassifierInstanceName());
     stringBuffer.append(TEXT_298);
     if (genClass.isDynamic()) {
     stringBuffer.append(TEXT_299);
     } else {
     stringBuffer.append(genClass.getRawImportedInterfaceName());
     stringBuffer.append(TEXT_300);
     }
     stringBuffer.append(TEXT_301);
     stringBuffer.append(genClass.getName());
     stringBuffer.append(TEXT_302);
     stringBuffer.append(genClass.getAbstractFlag());
     stringBuffer.append(TEXT_303);
     stringBuffer.append(genClass.getInterfaceFlag());
     stringBuffer.append(TEXT_304);
     stringBuffer.append(genClass.getGeneratedInstanceClassFlag());
     if (hasInstanceTypeName) {
     stringBuffer.append(TEXT_305);
     stringBuffer.append(genClass.getEcoreClass().getInstanceTypeName());
     stringBuffer.append(TEXT_306);
     }
     stringBuffer.append(TEXT_307);
     stringBuffer.append(genModel.getNonNLS());
     if (hasInstanceTypeName) {
     stringBuffer.append(genModel.getNonNLS(2));
     }
     for (GenFeature genFeature : genClass.getGenFeatures()) {
     if (genFeature.hasGenericType()) {
     for (InformationIterator i=new InformationIterator(genFeature.getEcoreFeature().getEGenericType()); i.hasNext(); ) { Information info = i.next(); String prefix = ""; if (maxGenericTypeAssignment <= info.depth) { ++maxGenericTypeAssignment; prefix = genModel.getImportedName("org.eclipse.emf.ecore.EGenericType") + " "; }
     stringBuffer.append(TEXT_308);
     stringBuffer.append(prefix);
     stringBuffer.append(TEXT_309);
     stringBuffer.append(info.depth + 1);
     stringBuffer.append(TEXT_310);
     stringBuffer.append(info.type);
     stringBuffer.append(TEXT_311);
     if (info.depth > 0) {
     stringBuffer.append(TEXT_312);
     stringBuffer.append(info.depth);
     stringBuffer.append(TEXT_313);
     stringBuffer.append(info.accessor);
     stringBuffer.append(TEXT_314);
     stringBuffer.append(info.depth + 1);
     stringBuffer.append(TEXT_315);
     }
     }
     }
     if (genFeature.isReferenceType()) { GenFeature reverseGenFeature = genFeature.getReverse();
     String reverse = reverseGenFeature == null ? "null" : genPackage.getPackageInstanceVariable(reverseGenFeature.getGenPackage()) + ".get" + reverseGenFeature.getFeatureAccessorName() + "()";
     stringBuffer.append(TEXT_316);
     stringBuffer.append(genFeature.getFeatureAccessorName());
     stringBuffer.append(TEXT_317);
     if (genFeature.hasGenericType()) {
     stringBuffer.append(TEXT_318);
     } else {
     stringBuffer.append(genPackage.getPackageInstanceVariable(genFeature.getTypeGenPackage()));
     stringBuffer.append(TEXT_319);
     stringBuffer.append(genFeature.getTypeClassifierAccessorName());
     stringBuffer.append(TEXT_320);
     }
     stringBuffer.append(TEXT_321);
     stringBuffer.append(reverse);
     stringBuffer.append(TEXT_322);
     stringBuffer.append(genFeature.getName());
     stringBuffer.append(TEXT_323);
     stringBuffer.append(genFeature.getDefaultValue());
     stringBuffer.append(TEXT_324);
     stringBuffer.append(genFeature.getLowerBound());
     stringBuffer.append(TEXT_325);
     stringBuffer.append(genFeature.getUpperBound());
     stringBuffer.append(TEXT_326);
     stringBuffer.append(genFeature.getContainerClass());
     stringBuffer.append(TEXT_327);
     stringBuffer.append(genFeature.getTransientFlag());
     stringBuffer.append(TEXT_328);
     stringBuffer.append(genFeature.getVolatileFlag());
     stringBuffer.append(TEXT_329);
     stringBuffer.append(genFeature.getChangeableFlag());
     stringBuffer.append(TEXT_330);
     stringBuffer.append(genFeature.getContainmentFlag());
     stringBuffer.append(TEXT_331);
     stringBuffer.append(genFeature.getResolveProxiesFlag());
     stringBuffer.append(TEXT_332);
     stringBuffer.append(genFeature.getUnsettableFlag());
     stringBuffer.append(TEXT_333);
     stringBuffer.append(genFeature.getUniqueFlag());
     stringBuffer.append(TEXT_334);
     stringBuffer.append(genFeature.getDerivedFlag());
     stringBuffer.append(TEXT_335);
     stringBuffer.append(genFeature.getOrderedFlag());
     stringBuffer.append(TEXT_336);
     stringBuffer.append(genModel.getNonNLS());
     stringBuffer.append(genModel.getNonNLS(genFeature.getDefaultValue(), 2));
     for (GenFeature keyFeature : genFeature.getKeys()) {
     stringBuffer.append(TEXT_337);
     stringBuffer.append(genFeature.getFeatureAccessorName());
     stringBuffer.append(TEXT_338);
     stringBuffer.append(genPackage.getPackageInstanceVariable(keyFeature.getGenPackage()));
     stringBuffer.append(TEXT_339);
     stringBuffer.append(keyFeature.getFeatureAccessorName());
     stringBuffer.append(TEXT_340);
     }
     } else {
     stringBuffer.append(TEXT_341);
     stringBuffer.append(genFeature.getFeatureAccessorName());
     stringBuffer.append(TEXT_342);
     if (genFeature.hasGenericType()) {
     stringBuffer.append(TEXT_343);
     } else {
     stringBuffer.append(genPackage.getPackageInstanceVariable(genFeature.getTypeGenPackage()));
     stringBuffer.append(TEXT_344);
     stringBuffer.append(genFeature.getTypeClassifierAccessorName());
     stringBuffer.append(TEXT_345);
     }
     stringBuffer.append(TEXT_346);
     stringBuffer.append(genFeature.getName());
     stringBuffer.append(TEXT_347);
     stringBuffer.append(genFeature.getDefaultValue());
     stringBuffer.append(TEXT_348);
     stringBuffer.append(genFeature.getLowerBound());
     stringBuffer.append(TEXT_349);
     stringBuffer.append(genFeature.getUpperBound());
     stringBuffer.append(TEXT_350);
     stringBuffer.append(genFeature.getContainerClass());
     stringBuffer.append(TEXT_351);
     stringBuffer.append(genFeature.getTransientFlag());
     stringBuffer.append(TEXT_352);
     stringBuffer.append(genFeature.getVolatileFlag());
     stringBuffer.append(TEXT_353);
     stringBuffer.append(genFeature.getChangeableFlag());
     stringBuffer.append(TEXT_354);
     stringBuffer.append(genFeature.getUnsettableFlag());
     stringBuffer.append(TEXT_355);
     stringBuffer.append(genFeature.getIDFlag());
     stringBuffer.append(TEXT_356);
     stringBuffer.append(genFeature.getUniqueFlag());
     stringBuffer.append(TEXT_357);
     stringBuffer.append(genFeature.getDerivedFlag());
     stringBuffer.append(TEXT_358);
     stringBuffer.append(genFeature.getOrderedFlag());
     stringBuffer.append(TEXT_359);
     stringBuffer.append(genModel.getNonNLS());
     stringBuffer.append(genModel.getNonNLS(genFeature.getDefaultValue(), 2));
     }
     }
     for (GenOperation genOperation : genClass.getGenOperations()) {String prefix = ""; if (genOperation.hasGenericType() || !genOperation.getGenParameters().isEmpty() || !genOperation.getGenExceptions().isEmpty() || !genOperation.getGenTypeParameters().isEmpty()) { if (firstOperationAssignment) { firstOperationAssignment = false; prefix = genModel.getImportedName("org.eclipse.emf.ecore.EOperation") + " op = "; } else { prefix = "op = "; }} 
     stringBuffer.append(TEXT_360);
     if (genModel.useGenerics()) {
     stringBuffer.append(TEXT_361);
     stringBuffer.append(prefix);
     stringBuffer.append(TEXT_362);
     stringBuffer.append(genClass.getClassifierInstanceName());
     stringBuffer.append(TEXT_363);
     if (genOperation.isVoid() || genOperation.hasGenericType()) {
     stringBuffer.append(TEXT_364);
     } else {
     stringBuffer.append(genPackage.getPackageInstanceVariable(genOperation.getTypeGenPackage()));
     stringBuffer.append(TEXT_365);
     stringBuffer.append(genOperation.getTypeClassifierAccessorName());
     stringBuffer.append(TEXT_366);
     }
     stringBuffer.append(TEXT_367);
     stringBuffer.append(genOperation.getName());
     stringBuffer.append(TEXT_368);
     stringBuffer.append(genOperation.getLowerBound());
     stringBuffer.append(TEXT_369);
     stringBuffer.append(genOperation.getUpperBound());
     stringBuffer.append(TEXT_370);
     stringBuffer.append(genOperation.getUniqueFlag());
     stringBuffer.append(TEXT_371);
     stringBuffer.append(genOperation.getOrderedFlag());
     stringBuffer.append(TEXT_372);
     stringBuffer.append(genModel.getNonNLS());
     } else if (!genOperation.isVoid()) {
     if (!genOperation.getEcoreOperation().isOrdered() || !genOperation.getEcoreOperation().isUnique()) { needsAddEOperation = true;
     stringBuffer.append(TEXT_373);
     stringBuffer.append(prefix);
     stringBuffer.append(TEXT_374);
     stringBuffer.append(genClass.getClassifierInstanceName());
     stringBuffer.append(TEXT_375);
     stringBuffer.append(genPackage.getPackageInstanceVariable(genOperation.getTypeGenPackage()));
     stringBuffer.append(TEXT_376);
     stringBuffer.append(genOperation.getTypeClassifierAccessorName());
     stringBuffer.append(TEXT_377);
     stringBuffer.append(genOperation.getName());
     stringBuffer.append(TEXT_378);
     stringBuffer.append(genOperation.getLowerBound());
     stringBuffer.append(TEXT_379);
     stringBuffer.append(genOperation.getUpperBound());
     stringBuffer.append(TEXT_380);
     stringBuffer.append(genOperation.getUniqueFlag());
     stringBuffer.append(TEXT_381);
     stringBuffer.append(genOperation.getOrderedFlag());
     stringBuffer.append(TEXT_382);
     stringBuffer.append(genModel.getNonNLS());
     } else {
     stringBuffer.append(TEXT_383);
     stringBuffer.append(prefix);
     stringBuffer.append(TEXT_384);
     stringBuffer.append(genClass.getClassifierInstanceName());
     stringBuffer.append(TEXT_385);
     stringBuffer.append(genPackage.getPackageInstanceVariable(genOperation.getTypeGenPackage()));
     stringBuffer.append(TEXT_386);
     stringBuffer.append(genOperation.getTypeClassifierAccessorName());
     stringBuffer.append(TEXT_387);
     stringBuffer.append(genOperation.getName());
     stringBuffer.append(TEXT_388);
     stringBuffer.append(genOperation.getLowerBound());
     stringBuffer.append(TEXT_389);
     stringBuffer.append(genOperation.getUpperBound());
     stringBuffer.append(TEXT_390);
     stringBuffer.append(genModel.getNonNLS());
     }
     } else {
     stringBuffer.append(TEXT_391);
     stringBuffer.append(prefix);
     stringBuffer.append(TEXT_392);
     stringBuffer.append(genClass.getClassifierInstanceName());
     stringBuffer.append(TEXT_393);
     stringBuffer.append(genOperation.getName());
     stringBuffer.append(TEXT_394);
     stringBuffer.append(genModel.getNonNLS());
     }
     if (genModel.useGenerics()) {
     for (ListIterator<GenTypeParameter> t=genOperation.getGenTypeParameters().listIterator(); t.hasNext(); ) { GenTypeParameter genTypeParameter = t.next(); String typeParameterVariable = ""; if (!genTypeParameter.getEcoreTypeParameter().getEBounds().isEmpty() || genTypeParameter.isUsed()) { if (maxTypeParameterAssignment <= t.previousIndex()) { ++maxTypeParameterAssignment; typeParameterVariable = genModel.getImportedName("org.eclipse.emf.ecore.ETypeParameter") + " t" + t.nextIndex() + " = "; } else { typeParameterVariable = "t" + t.nextIndex() + " = "; }} 
     stringBuffer.append(TEXT_395);
     stringBuffer.append(typeParameterVariable);
     stringBuffer.append(TEXT_396);
     stringBuffer.append(genTypeParameter.getName());
     stringBuffer.append(TEXT_397);
     stringBuffer.append(genModel.getNonNLS());
     for (EGenericType typeParameter : genTypeParameter.getEcoreTypeParameter().getEBounds()) {
     for (InformationIterator i=new InformationIterator(typeParameter); i.hasNext(); ) { Information info = i.next(); String typePrefix = ""; if (maxGenericTypeAssignment <= info.depth) { ++maxGenericTypeAssignment; typePrefix = genModel.getImportedName("org.eclipse.emf.ecore.EGenericType") + " "; }
     stringBuffer.append(TEXT_398);
     stringBuffer.append(typePrefix);
     stringBuffer.append(TEXT_399);
     stringBuffer.append(info.depth + 1);
     stringBuffer.append(TEXT_400);
     stringBuffer.append(info.type);
     stringBuffer.append(TEXT_401);
     if (info.depth > 0) {
     stringBuffer.append(TEXT_402);
     stringBuffer.append(info.depth);
     stringBuffer.append(TEXT_403);
     stringBuffer.append(info.accessor);
     stringBuffer.append(TEXT_404);
     stringBuffer.append(info.depth + 1);
     stringBuffer.append(TEXT_405);
     }
     }
     stringBuffer.append(TEXT_406);
     stringBuffer.append(t.nextIndex());
     stringBuffer.append(TEXT_407);
     }
     }
     }
     for (GenParameter genParameter : genOperation.getGenParameters()) {
     if (genParameter.hasGenericType()) {
     for (InformationIterator i=new InformationIterator(genParameter.getEcoreParameter().getEGenericType()); i.hasNext(); ) { Information info = i.next(); String typePrefix = ""; if (maxGenericTypeAssignment <= info.depth) { ++maxGenericTypeAssignment; typePrefix = genModel.getImportedName("org.eclipse.emf.ecore.EGenericType") + " "; }
     stringBuffer.append(TEXT_408);
     stringBuffer.append(typePrefix);
     stringBuffer.append(TEXT_409);
     stringBuffer.append(info.depth + 1);
     stringBuffer.append(TEXT_410);
     stringBuffer.append(info.type);
     stringBuffer.append(TEXT_411);
     if (info.depth > 0) {
     stringBuffer.append(TEXT_412);
     stringBuffer.append(info.depth);
     stringBuffer.append(TEXT_413);
     stringBuffer.append(info.accessor);
     stringBuffer.append(TEXT_414);
     stringBuffer.append(info.depth + 1);
     stringBuffer.append(TEXT_415);
     }
     }
     }
     if (genModel.useGenerics()) {
     stringBuffer.append(TEXT_416);
     if (genParameter.hasGenericType()){
     stringBuffer.append(TEXT_417);
     } else {
     stringBuffer.append(genPackage.getPackageInstanceVariable(genParameter.getTypeGenPackage()));
     stringBuffer.append(TEXT_418);
     stringBuffer.append(genParameter.getTypeClassifierAccessorName());
     stringBuffer.append(TEXT_419);
     }
     stringBuffer.append(TEXT_420);
     stringBuffer.append(genParameter.getName());
     stringBuffer.append(TEXT_421);
     stringBuffer.append(genParameter.getLowerBound());
     stringBuffer.append(TEXT_422);
     stringBuffer.append(genParameter.getUpperBound());
     stringBuffer.append(TEXT_423);
     stringBuffer.append(genParameter.getUniqueFlag());
     stringBuffer.append(TEXT_424);
     stringBuffer.append(genParameter.getOrderedFlag());
     stringBuffer.append(TEXT_425);
     stringBuffer.append(genModel.getNonNLS());
     } else if (!genParameter.getEcoreParameter().isOrdered() || !genParameter.getEcoreParameter().isUnique()) { needsAddEParameter = true;
     stringBuffer.append(TEXT_426);
     if (genParameter.hasGenericType()){
     stringBuffer.append(TEXT_427);
     } else {
     stringBuffer.append(genPackage.getPackageInstanceVariable(genParameter.getTypeGenPackage()));
     stringBuffer.append(TEXT_428);
     stringBuffer.append(genParameter.getTypeClassifierAccessorName());
     stringBuffer.append(TEXT_429);
     }
     stringBuffer.append(TEXT_430);
     stringBuffer.append(genParameter.getName());
     stringBuffer.append(TEXT_431);
     stringBuffer.append(genParameter.getLowerBound());
     stringBuffer.append(TEXT_432);
     stringBuffer.append(genParameter.getUpperBound());
     stringBuffer.append(TEXT_433);
     stringBuffer.append(genParameter.getUniqueFlag());
     stringBuffer.append(TEXT_434);
     stringBuffer.append(genParameter.getOrderedFlag());
     stringBuffer.append(TEXT_435);
     stringBuffer.append(genModel.getNonNLS());
     } else {
     stringBuffer.append(TEXT_436);
     if (genParameter.hasGenericType()){
     stringBuffer.append(TEXT_437);
     } else {
     stringBuffer.append(genPackage.getPackageInstanceVariable(genParameter.getTypeGenPackage()));
     stringBuffer.append(TEXT_438);
     stringBuffer.append(genParameter.getTypeClassifierAccessorName());
     stringBuffer.append(TEXT_439);
     }
     stringBuffer.append(TEXT_440);
     stringBuffer.append(genParameter.getName());
     stringBuffer.append(TEXT_441);
     stringBuffer.append(genParameter.getLowerBound());
     stringBuffer.append(TEXT_442);
     stringBuffer.append(genParameter.getUpperBound());
     stringBuffer.append(TEXT_443);
     stringBuffer.append(genModel.getNonNLS());
     }
     }
     if (genOperation.hasGenericExceptions()) {
     for (EGenericType genericExceptions : genOperation.getEcoreOperation().getEGenericExceptions()) {
     for (InformationIterator i=new InformationIterator(genericExceptions); i.hasNext(); ) { Information info = i.next(); String typePrefix = ""; if (maxGenericTypeAssignment <= info.depth) { ++maxGenericTypeAssignment; typePrefix = genModel.getImportedName("org.eclipse.emf.ecore.EGenericType") + " "; }
     stringBuffer.append(TEXT_444);
     stringBuffer.append(typePrefix);
     stringBuffer.append(TEXT_445);
     stringBuffer.append(info.depth + 1);
     stringBuffer.append(TEXT_446);
     stringBuffer.append(info.type);
     stringBuffer.append(TEXT_447);
     if (info.depth > 0) {
     stringBuffer.append(TEXT_448);
     stringBuffer.append(info.depth);
     stringBuffer.append(TEXT_449);
     stringBuffer.append(info.accessor);
     stringBuffer.append(TEXT_450);
     stringBuffer.append(info.depth + 1);
     stringBuffer.append(TEXT_451);
     }
     stringBuffer.append(TEXT_452);
     stringBuffer.append(info.depth + 1);
     stringBuffer.append(TEXT_453);
     }
     }
     } else {
     for (GenClassifier genException : genOperation.getGenExceptions()) {
     stringBuffer.append(TEXT_454);
     stringBuffer.append(genPackage.getPackageInstanceVariable(genException.getGenPackage()));
     stringBuffer.append(TEXT_455);
     stringBuffer.append(genException.getClassifierAccessorName());
     stringBuffer.append(TEXT_456);
     }
     }
     if (!genOperation.isVoid() && genOperation.hasGenericType()) {
     for (InformationIterator i=new InformationIterator(genOperation.getEcoreOperation().getEGenericType()); i.hasNext(); ) { Information info = i.next(); String typePrefix = ""; if (maxGenericTypeAssignment <= info.depth) { ++maxGenericTypeAssignment; typePrefix = genModel.getImportedName("org.eclipse.emf.ecore.EGenericType") + " "; }
     stringBuffer.append(TEXT_457);
     stringBuffer.append(typePrefix);
     stringBuffer.append(TEXT_458);
     stringBuffer.append(info.depth + 1);
     stringBuffer.append(TEXT_459);
     stringBuffer.append(info.type);
     stringBuffer.append(TEXT_460);
     if (info.depth > 0) {
     stringBuffer.append(TEXT_461);
     stringBuffer.append(info.depth);
     stringBuffer.append(TEXT_462);
     stringBuffer.append(info.accessor);
     stringBuffer.append(TEXT_463);
     stringBuffer.append(info.depth + 1);
     stringBuffer.append(TEXT_464);
     }
     }
     stringBuffer.append(TEXT_465);
     }
     }
     if (c.hasNext()) {
     stringBuffer.append(TEXT_466);
     }
     }
     }
     if (!genPackage.getGenEnums().isEmpty()) {
     stringBuffer.append(TEXT_467);
     for (Iterator<GenEnum> e=genPackage.getGenEnums().iterator(); e.hasNext();) { GenEnum genEnum = e.next();
     stringBuffer.append(TEXT_468);
     stringBuffer.append(genEnum.getClassifierInstanceName());
     stringBuffer.append(TEXT_469);
     stringBuffer.append(genEnum.getImportedName());
     stringBuffer.append(TEXT_470);
     stringBuffer.append(genEnum.getName());
     stringBuffer.append(TEXT_471);
     stringBuffer.append(genModel.getNonNLS());
     for (GenEnumLiteral genEnumLiteral : genEnum.getGenEnumLiterals()) {
     stringBuffer.append(TEXT_472);
     stringBuffer.append(genEnum.getClassifierInstanceName());
     stringBuffer.append(TEXT_473);
     stringBuffer.append(genEnum.getImportedName().equals(genEnum.getClassifierID()) ? genEnum.getQualifiedName() : genEnum.getImportedName());
     stringBuffer.append(TEXT_474);
     stringBuffer.append(genEnumLiteral.getEnumLiteralInstanceConstantName());
     stringBuffer.append(TEXT_475);
     }
     if (e.hasNext()) {
     stringBuffer.append(TEXT_476);
     }
     }
     }
     if (!genPackage.getGenDataTypes().isEmpty()) {
     stringBuffer.append(TEXT_477);
     for (GenDataType genDataType : genPackage.getGenDataTypes()) {boolean hasInstanceTypeName = genModel.useGenerics() && genDataType.getEcoreDataType().getInstanceTypeName() != null && genDataType.getEcoreDataType().getInstanceTypeName().contains("<");
     stringBuffer.append(TEXT_478);
     stringBuffer.append(genDataType.getClassifierInstanceName());
     stringBuffer.append(TEXT_479);
     stringBuffer.append(genDataType.getRawImportedInstanceClassName());
     stringBuffer.append(TEXT_480);
     stringBuffer.append(genDataType.getName());
     stringBuffer.append(TEXT_481);
     stringBuffer.append(genDataType.getSerializableFlag());
     stringBuffer.append(TEXT_482);
     stringBuffer.append(genDataType.getGeneratedInstanceClassFlag());
     if (hasInstanceTypeName) {
     stringBuffer.append(TEXT_483);
     stringBuffer.append(genDataType.getEcoreDataType().getInstanceTypeName());
     stringBuffer.append(TEXT_484);
     }
     stringBuffer.append(TEXT_485);
     stringBuffer.append(genModel.getNonNLS());
     if (hasInstanceTypeName) {
     stringBuffer.append(genModel.getNonNLS(2));
     }
     }
     }
     if (genPackage.getSuperGenPackage() == null) {
     stringBuffer.append(TEXT_486);
     }
     if (!genPackage.isEcorePackage() && !genPackage.getAnnotationSources().isEmpty()) {
     stringBuffer.append(TEXT_487);
     for (String annotationSource : genPackage.getAnnotationSources()) {
     stringBuffer.append(TEXT_488);
     stringBuffer.append(annotationSource);
     stringBuffer.append(TEXT_489);
     stringBuffer.append(genPackage.getAnnotationSourceIdentifier(annotationSource));
     stringBuffer.append(TEXT_490);
     }
     }
     stringBuffer.append(TEXT_491);
     for (String annotationSource : genPackage.getAnnotationSources()) {
     stringBuffer.append(TEXT_492);
     stringBuffer.append(annotationSource);
     stringBuffer.append(TEXT_493);
     stringBuffer.append(genPackage.getAnnotationSourceIdentifier(annotationSource));
     stringBuffer.append(TEXT_494);
     if (annotationSource == null) {
     stringBuffer.append(TEXT_495);
     } else {
     stringBuffer.append(TEXT_496);
     stringBuffer.append(annotationSource);
     stringBuffer.append(TEXT_497);
     stringBuffer.append(genModel.getNonNLS());
     }
     for (EAnnotation eAnnotation : genPackage.getAllAnnotations()) {
     stringBuffer.append(TEXT_498);
     if (annotationSource == null ? eAnnotation.getSource() == null : annotationSource.equals(eAnnotation.getSource())) {
     stringBuffer.append(TEXT_499);
     stringBuffer.append(genPackage.getAnnotatedModelElementAccessor(eAnnotation));
     stringBuffer.append(TEXT_500);
     for (Iterator<Map.Entry<String, String>> k = eAnnotation.getDetails().iterator(); k.hasNext();) { Map.Entry<String, String> detail = k.next(); String key = Literals.toStringLiteral(detail.getKey(), genModel); String value = Literals.toStringLiteral(detail.getValue(), genModel);
     stringBuffer.append(TEXT_501);
     stringBuffer.append(key);
     stringBuffer.append(TEXT_502);
     stringBuffer.append(value);
     stringBuffer.append(k.hasNext() ? "," : "");
     stringBuffer.append(genModel.getNonNLS(key + value));
     }
     stringBuffer.append(TEXT_503);
     }
     for (EAnnotation nestedEAnnotation : genPackage.getAllNestedAnnotations(eAnnotation)) {String nestedAnnotationSource = nestedEAnnotation.getSource(); int depth = 1; for (EObject eContainer = nestedEAnnotation.eContainer(); eContainer != eAnnotation; eContainer = eContainer.eContainer()) { ++depth; }
     stringBuffer.append(TEXT_504);
     stringBuffer.append(genPackage.getAnnotatedModelElementAccessor(eAnnotation));
     stringBuffer.append(TEXT_505);
     stringBuffer.append(depth);
     stringBuffer.append(TEXT_506);
     if (nestedAnnotationSource == null) {
     stringBuffer.append(TEXT_507);
     } else {
     stringBuffer.append(TEXT_508);
     stringBuffer.append(nestedAnnotationSource);
     stringBuffer.append(TEXT_509);
     stringBuffer.append(genModel.getNonNLS());
     }
     stringBuffer.append(TEXT_510);
     for (Iterator<Map.Entry<String, String>> l = nestedEAnnotation.getDetails().iterator(); l.hasNext();) { Map.Entry<String, String> detail = l.next(); String key = Literals.toStringLiteral(detail.getKey(), genModel); String value = Literals.toStringLiteral(detail.getValue(), genModel);
     stringBuffer.append(TEXT_511);
     stringBuffer.append(key);
     stringBuffer.append(TEXT_512);
     stringBuffer.append(value);
     stringBuffer.append(l.hasNext() ? "," : "");
     stringBuffer.append(genModel.getNonNLS(key + value));
     }
     stringBuffer.append(TEXT_513);
     }
     for (EAnnotation nestedEAnnotation : genPackage.getAllNestedAnnotations(eAnnotation)) {String nestedAnnotationSource = nestedEAnnotation.getSource(); int depth = 1; for (EObject eContainer = nestedEAnnotation.eContainer(); eContainer != eAnnotation; eContainer = eContainer.eContainer()) { ++depth; }
     stringBuffer.append(TEXT_514);
     stringBuffer.append(genPackage.getAnnotatedModelElementAccessor(eAnnotation));
     stringBuffer.append(TEXT_515);
     stringBuffer.append(depth);
     stringBuffer.append(TEXT_516);
     if (nestedAnnotationSource == null) {
     stringBuffer.append(TEXT_517);
     } else {
     stringBuffer.append(TEXT_518);
     stringBuffer.append(nestedAnnotationSource);
     stringBuffer.append(TEXT_519);
     stringBuffer.append(genModel.getNonNLS());
     }
     stringBuffer.append(TEXT_520);
     for (Iterator<Map.Entry<String, String>> l = nestedEAnnotation.getDetails().iterator(); l.hasNext();) { Map.Entry<String, String> detail = l.next(); String key = Literals.toStringLiteral(detail.getKey(), genModel); String value = Literals.toStringLiteral(detail.getValue(), genModel);
     stringBuffer.append(TEXT_521);
     stringBuffer.append(key);
     stringBuffer.append(TEXT_522);
     stringBuffer.append(value);
     stringBuffer.append(l.hasNext() ? "," : "");
     stringBuffer.append(genModel.getNonNLS(key + value));
     }
     stringBuffer.append(TEXT_523);
     }
     }
     stringBuffer.append(TEXT_524);
     }
     } else {
     if (genPackage.isLoadingInitialization()) {
     stringBuffer.append(TEXT_525);
     stringBuffer.append(genModel.getImportedName("java.net.URL"));
     stringBuffer.append(TEXT_526);
     stringBuffer.append(genModel.getNonNLS());
     stringBuffer.append(TEXT_527);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.common.util.URI"));
     stringBuffer.append(TEXT_528);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.common.util.URI"));
     stringBuffer.append(TEXT_529);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.resource.Resource"));
     stringBuffer.append(TEXT_530);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl"));
     stringBuffer.append(TEXT_531);
     stringBuffer.append(genModel.getImportedName("java.io.IOException"));
     stringBuffer.append(TEXT_532);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.common.util.WrappedException"));
     stringBuffer.append(TEXT_533);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.EPackage"));
     stringBuffer.append(TEXT_534);
     }
     stringBuffer.append(TEXT_535);
     if (genModel.useClassOverrideAnnotation()) {
     stringBuffer.append(TEXT_536);
     }
     stringBuffer.append(TEXT_537);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.EClassifier"));
     stringBuffer.append(TEXT_538);
     ArrayList<GenClass> dynamicGenClasses = new ArrayList<GenClass>(); for (GenClass genClass : genPackage.getGenClasses()) { if (genClass.isDynamic()) { dynamicGenClasses.add(genClass); } }
     if (dynamicGenClasses.isEmpty()) {
     stringBuffer.append(TEXT_539);
     stringBuffer.append(genPackage.getInterfacePackageName());
     stringBuffer.append(TEXT_540);
     stringBuffer.append(genModel.getNonNLS());
     stringBuffer.append(TEXT_541);
     } else {
     stringBuffer.append(TEXT_542);
     for (GenClass genClass : dynamicGenClasses) {
     if (genClass.isDynamic()) {
     stringBuffer.append(TEXT_543);
     stringBuffer.append(genPackage.getClassifierID(genClass));
     stringBuffer.append(TEXT_544);
     }
     }
     stringBuffer.append(TEXT_545);
     stringBuffer.append(genPackage.getInterfacePackageName());
     stringBuffer.append(TEXT_546);
     stringBuffer.append(genModel.getNonNLS());
     stringBuffer.append(TEXT_547);
     }
     stringBuffer.append(TEXT_548);
     }
     if (needsAddEOperation) {
     stringBuffer.append(TEXT_549);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.EOperation"));
     stringBuffer.append(TEXT_550);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.EClass"));
     stringBuffer.append(TEXT_551);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.EClassifier"));
     stringBuffer.append(TEXT_552);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.EOperation"));
     stringBuffer.append(TEXT_553);
     }
     if (needsAddEParameter) {
     stringBuffer.append(TEXT_554);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.EParameter"));
     stringBuffer.append(TEXT_555);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.EOperation"));
     stringBuffer.append(TEXT_556);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.EClassifier"));
     stringBuffer.append(TEXT_557);
     stringBuffer.append(genModel.getImportedName("org.eclipse.emf.ecore.EParameter"));
     stringBuffer.append(TEXT_558);
     }
     }
     if (isInterface && genPackage.isLiteralsInterface()) {
     stringBuffer.append(TEXT_559);
     if (isImplementation) {
     stringBuffer.append(TEXT_560);
     }
     stringBuffer.append(TEXT_561);
     for (GenClassifier genClassifier : genPackage.getGenClassifiers()) {
     stringBuffer.append(TEXT_562);
     if (genClassifier instanceof GenClass) { GenClass genClass = (GenClass)genClassifier;
     if (!genClass.isInterface()) {
     stringBuffer.append(TEXT_563);
     stringBuffer.append(genClass.getQualifiedClassName());
     stringBuffer.append(TEXT_564);
     stringBuffer.append(genClass.getFormattedName());
     stringBuffer.append(TEXT_565);
     stringBuffer.append(genClass.getQualifiedClassName());
     } else {
     stringBuffer.append(TEXT_566);
     stringBuffer.append(genClass.getQualifiedInterfaceName());
     stringBuffer.append(TEXT_567);
     stringBuffer.append(genClass.getFormattedName());
     stringBuffer.append(TEXT_568);
     stringBuffer.append(genClass.getQualifiedInterfaceName());
     }
     } else if (genClassifier instanceof GenEnum) { GenEnum genEnum = (GenEnum)genClassifier;
     stringBuffer.append(TEXT_569);
     stringBuffer.append(genEnum.getQualifiedName());
     stringBuffer.append(TEXT_570);
     stringBuffer.append(genEnum.getFormattedName());
     stringBuffer.append(TEXT_571);
     stringBuffer.append(genEnum.getQualifiedName());
     } else if (genClassifier instanceof GenDataType) { GenDataType genDataType = (GenDataType)genClassifier;
     stringBuffer.append(TEXT_572);
     stringBuffer.append(genDataType.getFormattedName());
     stringBuffer.append(TEXT_573);
     if (!genDataType.isPrimitiveType() && !genDataType.isArrayType()) {
     stringBuffer.append(TEXT_574);
     stringBuffer.append(genDataType.getRawInstanceClassName());
     }
     }
     stringBuffer.append(TEXT_575);
     stringBuffer.append(genPackage.getQualifiedPackageClassName());
     stringBuffer.append(TEXT_576);
     stringBuffer.append(genClassifier.getClassifierAccessorName());
     stringBuffer.append(TEXT_577);
     stringBuffer.append(publicStaticFinalFlag);
     stringBuffer.append(genClassifier.getImportedMetaType());
     stringBuffer.append(TEXT_578);
     stringBuffer.append(genPackage.getClassifierID(genClassifier));
     stringBuffer.append(TEXT_579);
     stringBuffer.append(genClassifier.getClassifierAccessorName());
     stringBuffer.append(TEXT_580);
     if (genClassifier instanceof GenClass) { GenClass genClass = (GenClass)genClassifier;
     for (GenFeature genFeature : genClass.getGenFeatures()) {
     stringBuffer.append(TEXT_581);
     stringBuffer.append(genFeature.getFormattedName());
     stringBuffer.append(TEXT_582);
     stringBuffer.append(genFeature.getFeatureKind());
     stringBuffer.append(TEXT_583);
     stringBuffer.append(publicStaticFinalFlag);
     stringBuffer.append(genFeature.getImportedMetaType());
     stringBuffer.append(TEXT_584);
     stringBuffer.append(genClass.getFeatureID(genFeature));
     stringBuffer.append(TEXT_585);
     stringBuffer.append(genFeature.getFeatureAccessorName());
     stringBuffer.append(TEXT_586);
     }
     }
     }
     stringBuffer.append(TEXT_587);
     }
     stringBuffer.append(TEXT_588);
     stringBuffer.append(isInterface ? genPackage.getPackageInterfaceName() : genPackage.getPackageClassName());
     genModel.emitSortedImports();
     stringBuffer.append(TEXT_589);
     return stringBuffer.toString();
   }
 }
