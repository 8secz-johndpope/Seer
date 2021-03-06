 /**********************************************************************
  * Copyright (c) 2004 IBM Corporation and others.
  * All rights reserved.   This program and the accompanying materials
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  * 
  * Contributors: 
  * IBM - Initial API and implementation
  **********************************************************************/
 package org.eclipse.cdt.internal.core.dom;
 
 import org.eclipse.cdt.core.CCorePlugin;
 import org.eclipse.cdt.core.dom.IASTServiceProvider;
 import org.eclipse.cdt.core.dom.ICodeReaderFactory;
 import org.eclipse.cdt.core.dom.IParserConfiguration;
 import org.eclipse.cdt.core.dom.ast.ASTCompletionNode;
 import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
 import org.eclipse.cdt.core.filetype.ICFileType;
 import org.eclipse.cdt.core.filetype.ICFileTypeConstants;
 import org.eclipse.cdt.core.parser.CodeReader;
 import org.eclipse.cdt.core.parser.IScanner;
 import org.eclipse.cdt.core.parser.IScannerInfo;
 import org.eclipse.cdt.core.parser.IScannerInfoProvider;
 import org.eclipse.cdt.core.parser.ParserFactory;
 import org.eclipse.cdt.core.parser.ParserLanguage;
 import org.eclipse.cdt.core.parser.ParserMode;
 import org.eclipse.cdt.core.parser.ParserUtil;
 import org.eclipse.cdt.core.parser.ScannerInfo;
 import org.eclipse.cdt.internal.core.dom.parser.ISourceCodeParser;
 import org.eclipse.cdt.internal.core.dom.parser.c.ANSICParserExtensionConfiguration;
 import org.eclipse.cdt.internal.core.dom.parser.c.GCCParserExtensionConfiguration;
 import org.eclipse.cdt.internal.core.dom.parser.c.GNUCSourceParser;
 import org.eclipse.cdt.internal.core.dom.parser.c.ICParserExtensionConfiguration;
 import org.eclipse.cdt.internal.core.dom.parser.cpp.ANSICPPParserExtensionConfiguration;
 import org.eclipse.cdt.internal.core.dom.parser.cpp.GNUCPPSourceParser;
 import org.eclipse.cdt.internal.core.dom.parser.cpp.GPPParserExtensionConfiguration;
 import org.eclipse.cdt.internal.core.dom.parser.cpp.ICPPParserExtensionConfiguration;
 import org.eclipse.cdt.internal.core.parser.scanner2.DOMScanner;
 import org.eclipse.cdt.internal.core.parser.scanner2.GCCScannerExtensionConfiguration;
 import org.eclipse.cdt.internal.core.parser.scanner2.GPPScannerExtensionConfiguration;
 import org.eclipse.cdt.internal.core.parser.scanner2.IScannerExtensionConfiguration;
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IResource;
 
 /**
  * @author jcamelon
  */
 public class InternalASTServiceProvider implements IASTServiceProvider {
 
     protected static final GCCScannerExtensionConfiguration C_GNU_SCANNER_EXTENSION = new GCCScannerExtensionConfiguration();
    protected static final GPPScannerExtensionConfiguration CPP_GNU_SCANNER_EXTENSION = new GPPScannerExtensionConfiguration();
    private static final String[] dialects = { "C99",  //$NON-NLS-1$
             "C++98",  //$NON-NLS-1$
             "GNUC",  //$NON-NLS-1$
             "GNUC++" };  //$NON-NLS-1$
 
 
     /* (non-Javadoc)
      * @see org.eclipse.cdt.core.dom.IASTServiceProvider#getName()
      */
     public String getName() {
         return "CDT AST Service"; //$NON-NLS-1$
     }
 
     /* (non-Javadoc)
      * @see org.eclipse.cdt.core.dom.IASTServiceProvider#getTranslationUnit()
      */
     public IASTTranslationUnit getTranslationUnit(IFile fileToParse) throws UnsupportedDialectException {
         return getTranslationUnit( fileToParse, SavedCodeReaderFactory.getInstance(), null );
     }
 
     /* (non-Javadoc)
      * @see org.eclipse.cdt.core.dom.IASTServiceProvider#getTranslationUnit(org.eclipse.cdt.core.dom.ICodeReaderFactory)
      */
     public IASTTranslationUnit getTranslationUnit(IFile fileToParse, ICodeReaderFactory fileCreator) throws UnsupportedDialectException {
         return getTranslationUnit( fileToParse, fileCreator, null );
     }
 
     /* (non-Javadoc)
      * @see org.eclipse.cdt.core.dom.IASTServiceProvider#getTranslationUnit(org.eclipse.cdt.core.dom.ICodeReaderFactory, org.eclipse.cdt.core.dom.IParserConfiguration)
      */
     public IASTTranslationUnit getTranslationUnit(
             IFile fileToParse, ICodeReaderFactory fileCreator, IParserConfiguration configuration) throws UnsupportedDialectException {
 		//Get the scanner info
 		IProject currentProject = fileToParse.getProject();
 		IScannerInfo scanInfo = null;
 		
 		if( configuration == null )
 		{
 			IScannerInfoProvider provider = CCorePlugin.getDefault().getScannerInfoProvider(currentProject);
 			if (provider != null){
 			  IScannerInfo buildScanInfo = provider.getScannerInformation(fileToParse);
 			  if (buildScanInfo != null){
 			      scanInfo = buildScanInfo;
 			  }
 			  else
 			      scanInfo = new ScannerInfo();
 			}
 		}
 		else
 		    scanInfo = configuration.getScannerInfo();
 
 		
 		CodeReader reader = fileCreator.createCodeReaderForTranslationUnit( fileToParse.getLocation().toOSString() );
 		IScanner scanner = null;
 		ISourceCodeParser parser = null;
 
 		if( configuration == null )
 		{
 		    ParserLanguage l = getLanguage(fileToParse);
 		    IScannerExtensionConfiguration scannerExtensionConfiguration = null;
 		    if( l == ParserLanguage.CPP )
 		       scannerExtensionConfiguration = CPP_GNU_SCANNER_EXTENSION;
 		    else
 		       scannerExtensionConfiguration = C_GNU_SCANNER_EXTENSION;
 		    
 		    scanner = new DOMScanner(reader, scanInfo, ParserMode.COMPLETE_PARSE,
 	                l, ParserFactory.createDefaultLogService(), scannerExtensionConfiguration, fileCreator );
 		    //assume GCC
 		    if( l == ParserLanguage.C )
 		        parser = new GNUCSourceParser( scanner, ParserMode.COMPLETE_PARSE, ParserUtil.getParserLogService(), new GCCParserExtensionConfiguration()  );
 		    else
 		        parser = new GNUCPPSourceParser( scanner, ParserMode.COMPLETE_PARSE, ParserUtil.getParserLogService(), new GPPParserExtensionConfiguration() );		    
 		}
 		else
 		{
 		    String dialect = configuration.getParserDialect();
 		    if( dialect.equals( dialects[0]) || dialect.equals( dialects[2]))	
 			    scanner = new DOMScanner(reader, scanInfo, ParserMode.COMPLETE_PARSE,
 		                ParserLanguage.C, 
 		                ParserUtil.getScannerLogService(), C_GNU_SCANNER_EXTENSION, fileCreator );
 		    else if( dialect.equals( dialects[1] ) || dialect.equals( dialects[3] ))
 			    scanner = new DOMScanner(reader, scanInfo, ParserMode.COMPLETE_PARSE,
 			            ParserLanguage.CPP, 
 		                ParserUtil.getScannerLogService(), CPP_GNU_SCANNER_EXTENSION, fileCreator );
 		    else
 		        throw new UnsupportedDialectException();
 		    
 		    if( dialect.equals( dialects[0]))
 		    {
 		        ICParserExtensionConfiguration config = new ANSICParserExtensionConfiguration();
 		        parser = new GNUCSourceParser( scanner, ParserMode.COMPLETE_PARSE, ParserUtil.getParserLogService(), config ); 
 		    }
 		    else if( dialect.equals( dialects[1] ))
 		    {
 		        ICPPParserExtensionConfiguration config = new ANSICPPParserExtensionConfiguration();
 		        parser = new GNUCPPSourceParser( scanner, ParserMode.COMPLETE_PARSE, ParserUtil.getParserLogService(), config );
 		    }
 		    else if( dialect.equals( dialects[2]))
 		    {
 		        ICParserExtensionConfiguration config = new GCCParserExtensionConfiguration();
 		        parser = new GNUCSourceParser( scanner, ParserMode.COMPLETE_PARSE, ParserUtil.getParserLogService(), config ); 		        
 		    }
 		    else if( dialect.equals( dialects[3]))
 		    {
 		        ICPPParserExtensionConfiguration config = new GPPParserExtensionConfiguration();
 		        parser = new GNUCPPSourceParser( scanner, ParserMode.COMPLETE_PARSE, ParserUtil.getParserLogService(), config );		        
 		    }
 		}
 		IASTTranslationUnit tu = parser.parse();
 		return tu;
     }
 
     /* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.dom.IASTServiceProvider#getCompletionNode(org.eclipse.core.resources.IFile, int, org.eclipse.cdt.core.dom.ICodeReaderFactory)
 	 */
 	public ASTCompletionNode getCompletionNode(IFile fileToParse, int offset,
 			ICodeReaderFactory fileCreator) throws UnsupportedDialectException {
 		// Get the scanner info
 		IProject currentProject = fileToParse.getProject();
 		IScannerInfo scanInfo = null;
 
 		IScannerInfoProvider provider = CCorePlugin.getDefault()
 				.getScannerInfoProvider(currentProject);
 		if (provider != null) {
 			IScannerInfo buildScanInfo = provider
 					.getScannerInformation(fileToParse);
 			if (buildScanInfo != null)
 				scanInfo = buildScanInfo;
 			else
 				scanInfo = new ScannerInfo();
 		}
 
 		CodeReader reader = fileCreator
 				.createCodeReaderForTranslationUnit(fileToParse.getLocation()
 						.toOSString());
 
 		ParserLanguage l = getLanguage(fileToParse);
 		IScannerExtensionConfiguration scannerExtensionConfiguration = null;
 		if (l == ParserLanguage.CPP)
 			scannerExtensionConfiguration = CPP_GNU_SCANNER_EXTENSION;
 		else
 			scannerExtensionConfiguration = C_GNU_SCANNER_EXTENSION;
 
 		IScanner scanner = new DOMScanner(reader, scanInfo, ParserMode.COMPLETION_PARSE,
 				l, ParserFactory.createDefaultLogService(),
 				scannerExtensionConfiguration, fileCreator);
 		scanner.setContentAssistMode(offset);
 		
 		// assume GCC
 		ISourceCodeParser parser = null;
 		if (l == ParserLanguage.C)
 			parser = new GNUCSourceParser(scanner, ParserMode.COMPLETION_PARSE,
 					ParserUtil.getParserLogService(),
 					new GCCParserExtensionConfiguration());
 		else
 			parser = new GNUCPPSourceParser(scanner, ParserMode.COMPLETION_PARSE,
 					ParserUtil.getParserLogService(),
 					new GPPParserExtensionConfiguration());
 		
 		// Run the parse and return the completion node
 		parser.parse();
 		return parser.getCompletionNode();
 	}
 	
     /*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.cdt.core.dom.IASTServiceProvider#getSupportedDialects()
 	 */
     public String[] getSupportedDialects() {
         return dialects;
     }
 
     private ParserLanguage getLanguage( IResource resource )
     {    
 		IProject project = resource.getProject();
 		ICFileType type = CCorePlugin.getDefault().getFileType(project, resource.getLocation().lastSegment());
        boolean isHeader= type.isHeader();
        if( isHeader ) 
            return ParserLanguage.CPP; // assumption
        String lid = type.getLanguage().getId();
        if( lid.equals(ICFileTypeConstants.LANG_CXX))
            return ParserLanguage.CPP;
        if( lid.equals( ICFileTypeConstants.LANG_C ) )
            return ParserLanguage.C;
		return ParserLanguage.CPP;
     }
 }
