 package org.sugarj;
 
 import static org.sugarj.common.ATermCommands.getApplicationSubterm;
 import static org.sugarj.common.ATermCommands.isApplication;
 
 import java.io.File;
 import java.io.IOException;
 import java.net.URL;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Set;
 
 import org.eclipse.core.runtime.FileLocator;
 import org.spoofax.interpreter.terms.IStrategoTerm;
 import org.strategoxt.HybridInterpreter;
 import org.strategoxt.stratego_gpp.parse_pptable_file_0_0;
 import org.sugarj.common.ATermCommands;
 import org.sugarj.common.CommandExecution;
 import org.sugarj.common.Environment;
 import org.sugarj.common.FileCommands;
 import org.sugarj.common.IErrorLogger;
 import org.sugarj.common.path.Path;
 import org.sugarj.common.path.RelativePath;
 import org.sugarj.common.path.RelativeSourceLocationPath;
 import org.sugarj.driver.sourcefilecontent.ISourceFileContent;
 import org.sugarj.driver.sourcefilecontent.SourceImport;
 import org.sugarj.haskell.HaskellSourceFileContent;
 
 /**
  * @author Sebastian Erdweg <seba at informatik uni-marburg de>
  */
 public class HaskellLib extends LanguageLib {
 
   private static final long serialVersionUID = -8916207157344649789L;
   
   private final String GHC_COMMAND = "ghc";
 
   private transient File libDir;
   
   private HaskellSourceFileContent sourceContent;
   private Path outFile;
   private Set<RelativePath> generatedModules = new HashSet<RelativePath>();
   
   private String relNamespaceName;
   private String moduleName;
 
   private IStrategoTerm ppTable;
 
   @Override
   public String getLanguageName() {
     return "Haskell";
   }
 
   @Override
   public List<File> getGrammars() {
     List<File> grammars = new LinkedList<File>(super.getGrammars());
     grammars.add(ensureFile("org/sugarj/languages/SugarHaskell.def"));
     grammars.add(ensureFile("org/sugarj/languages/Haskell.def"));
     return Collections.unmodifiableList(grammars);
   }
   
   @Override
   public File getInitGrammar() {
     return ensureFile("org/sugarj/haskell/initGrammar.sdf");
   }
 
   @Override
   public String getInitGrammarModule() {
     return "org/sugarj/haskell/initGrammar";
   }
 
   @Override
   public File getInitTrans() {
     return ensureFile("org/sugarj/haskell/initTrans.str");
   }
 
   @Override
   public String getInitTransModule() {
     return "org/sugarj/haskell/initTrans";
   }
 
   @Override
   public File getInitEditor() {
     return ensureFile("org/sugarj/haskell/initEditor.serv");
   }
 
   @Override
   public String getInitEditorModule() {
     return "org/sugarj/haskell/initEditor";
   }
 
   @Override
   public File getLibraryDirectory() {
     if (libDir == null) { // set up directories first
       String thisClassPath = "org/sugarj/HaskellLib.class";
       URL thisClassURL = HaskellLib.class.getClassLoader().getResource(thisClassPath);
       
       System.out.println(thisClassURL);
       
       if (thisClassURL.getProtocol().equals("bundleresource"))
         try {
           thisClassURL = FileLocator.resolve(thisClassURL);
         } catch (IOException e) {
           e.printStackTrace();
         }
       
       String classPath = thisClassURL.getPath();
       String binPath = classPath.substring(0, classPath.length() - thisClassPath.length());
       
       libDir = new File(binPath);
     }
     
     return libDir;
   }
 
   @Override
   public String getGeneratedFileExtension() {
     return ".o";
   }
 
   @Override
   public String getSugarFileExtension() {
     //TODO should be ".sugh"
     return ".sugj";
   }
 
   @Override
   public ISourceFileContent getSource() {
     return sourceContent;
   }
 
   @Override
   public Path getOutFile() {
     return outFile;
   }
 
   @Override
   public Set<RelativePath> getGeneratedFiles() {
     return generatedModules;
   }
   
   @Override
   public String getRelativeNamespace() {
     return relNamespaceName;
   }
 
   @Override
   public boolean isNamespaceDec(IStrategoTerm decl) {
     return isApplication(decl, "ModuleDec");
   }
 
   @Override
   public boolean isLanguageSpecificDec(IStrategoTerm decl) {
     return isApplication(decl, "HaskellBody");
   }
 
   @Override
   public boolean isSugarDec(IStrategoTerm decl) {
     return isApplication(decl, "SugarBody");
   }
 
   @Override
   public boolean isEditorServiceDec(IStrategoTerm decl) {
     return isApplication(decl, "EditorBody");   
   }
 
   @Override
   public boolean isImportDec(IStrategoTerm decl) {
     return isApplication(decl, "Import");   
   }
 
   @Override
   public boolean isPlainDec(IStrategoTerm decl) {
     return isApplication(decl, "PlainDec");   
   }
   
   @Override
   public LanguageLibFactory getFactoryForLanguage() {
     return HaskellLibFactory.getInstance();
   }
 
 
   
   /*
    * processing stuff follows here
    */
   
   @Override
   public void setupSourceFile(RelativePath sourceFile, Environment environment) {
     outFile = environment.createBinPath(FileCommands.dropExtension(sourceFile.getRelativePath()) + ".hs");
     sourceContent = new HaskellSourceFileContent();
   }
 
   @Override
   public void processNamespaceDec(IStrategoTerm toplevelDecl, Environment environment, HybridInterpreter interp, IErrorLogger errorLog, RelativeSourceLocationPath sourceFile, RelativeSourceLocationPath sourceFileFromResult) throws IOException {
     String qualifiedModuleName = prettyPrint(getApplicationSubterm(toplevelDecl, "ModuleDec", 0), interp);
     String qualifiedModulePath = qualifiedModuleName.replace('.', '/');
     String declaredModuleName = FileCommands.fileName(qualifiedModulePath);
     moduleName = FileCommands.dropExtension(FileCommands.fileName(sourceFile.getRelativePath()));
     String declaredRelNamespaceName = FileCommands.dropFilename(qualifiedModulePath);
     relNamespaceName = FileCommands.dropFilename(sourceFile.getRelativePath());
     
     RelativePath objectFile = environment.createBinPath(relNamespaceName + Environment.sep + moduleName + getGeneratedFileExtension());
     generatedModules.add(objectFile);
     
     sourceContent.setNamespaceDecl(prettyPrint(toplevelDecl, interp));
     
     if (!declaredRelNamespaceName.equals(relNamespaceName))
       setErrorMessage(toplevelDecl,
                       "The declared package '" + declaredRelNamespaceName + "'" +
                       " does not match the expected package '" + relNamespaceName + "'.", errorLog);
     
     if (!declaredModuleName.equals(moduleName))
       setErrorMessage(toplevelDecl,
                       "The declared package '" + declaredModuleName + "'" +
                       " does not match the expected package '" + moduleName + "'.", errorLog);
   }
 
   @Override
   public void processLanguageSpecific(IStrategoTerm toplevelDecl, Environment environment, HybridInterpreter interp) throws IOException {
     IStrategoTerm term = getApplicationSubterm(toplevelDecl, "HaskellBody", 0);
     String text = null;
     try {
       text = prettyPrint(term, interp);
     } catch (NullPointerException e) {
       ATermCommands.setErrorMessage(toplevelDecl, "pretty printing Haskell failed");
     }
     if (text != null)
       sourceContent.addBodyDecl(text);
   }
 
   @Override
   public String getImportedModulePath(IStrategoTerm toplevelDecl, HybridInterpreter interp) throws IOException {
     return prettyPrint(getApplicationSubterm(toplevelDecl, "Import", 2), interp).replace('.', '/');
   }
   
   @Override
   public void addImportModule(IStrategoTerm toplevelDecl, HybridInterpreter interp, boolean checked) throws IOException {
     SourceImport imp = new SourceImport(getImportedModulePath(toplevelDecl, interp), prettyPrint(toplevelDecl, interp));
     if (checked)
       sourceContent.addCheckedImport(imp);
     else
       sourceContent.addImport(imp);
   }
   
   @Override
   public String getSugarName(IStrategoTerm decl, HybridInterpreter interp) throws IOException {
     return moduleName;
   }
 
   @Override
   public int getSugarAccessibility(IStrategoTerm decl) {
     return PUBLIC_SUGAR;
   }
 
   @Override
   public IStrategoTerm getSugarBody(IStrategoTerm decl) {
     return getApplicationSubterm(decl, "SugarBody", 0);
   }
 
   @Override
   public String prettyPrint(IStrategoTerm term, HybridInterpreter interp) throws IOException {
     if (ppTable == null) {
       IStrategoTerm ppTableFile = ATermCommands.makeString(ensureFile("org/sugarj/languages/Haskell.pp").getAbsolutePath());
       ppTable = parse_pptable_file_0_0.instance.invoke(org.strategoxt.stratego_gpp.stratego_gpp.init(), ppTableFile);
     }
     
     return ATermCommands.prettyPrint(ppTable, term, interp);
   }
   
   @Override
  protected void compile(List<Path> outFiles, Path bin, List<Path> path, HybridInterpreter interp, boolean generateFiles) throws IOException {
     if (generateFiles) {
       List<String> cmds = new LinkedList<String>();
       cmds.add(GHC_COMMAND);
       for (Path outFile : outFiles)
         cmds.add(outFile.getAbsolutePath());
       
       CommandExecution.execute(cmds.toArray(new String[cmds.size()]));
     }
   }
 }
