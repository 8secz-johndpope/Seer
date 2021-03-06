 /*
  * Created on Jan 3, 2004
  *
  * To change the template for this generated file go to
  * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
  */
 package org.epic.perleditor.editors.util;
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileReader;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.OutputStream;
 import java.io.OutputStreamWriter;
 import java.io.Reader;
 import java.io.Writer;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Hashtable;
 import java.util.List;
 import java.util.Map;
 import java.util.StringTokenizer;
 
 import org.eclipse.core.resources.IMarker;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.jface.text.Document;
 import org.eclipse.ui.IEditorDescriptor;
 import org.epic.perleditor.PerlEditorPlugin;
 import org.epic.perleditor.editors.AddEditorMarker;
 
 /**
  * @author luelljoc
  *
  * To change the template for this generated type comment go to
  * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
  */
 public class PerlValidator {
 
 	private static final String PERL_EDITOR_ID =
 		"org.epic.perleditor.editors.PerlEditor";
 	private static final String EMB_PERL_FILE_EXTENSION = "epl";
 
 	private static final String PERL_CMD_EXT = "-c";
 	private static final String PERL_ERROR_INDICATOR = " at - line ";
 	private static int maxErrorsShown = 10;
 	private static final String[] WARNING_STRINGS =
 		{ "possible", "Useless", "may", "better written as" };
 		
     private static final int BUF_SIZE = 1024;
 
 	public static boolean  validate(IResource resource) {
 		try {
 			//	Check if resource should be validated
 			IEditorDescriptor defaultEditorDescriptor =
 				PerlEditorPlugin
 					.getDefault()
 					.getWorkbench()
 					.getEditorRegistry()
 					.getDefaultEditor(resource.getFullPath().toString());
 					
 			if(defaultEditorDescriptor == null) {
 				return false;
 			}
 
 			if (!defaultEditorDescriptor.getId().equals(PERL_EDITOR_ID)
 			    ||  resource.getFileExtension().equals(EMB_PERL_FILE_EXTENSION)) {
 					return false;
 			}
    
            StringBuffer sourceCode = new StringBuffer();
 
 			//	Get the file content
 			char[] buf = new char[BUF_SIZE];
 			File inputFile = new File(resource.getLocation().makeAbsolute().toString());
 			BufferedReader in = new BufferedReader(new FileReader(inputFile));
 			
 			int read = 0;
 			while((read = in.read(buf)) > 0) {
 				sourceCode.append(buf, 0,  read);
 			}	
 			in.close();
 			
 			validate(resource, sourceCode.toString());
 			
 		} catch (Exception e) {
 			e.printStackTrace();
 			return false;
 		}
 		
 		return true;
 	}
 
 	public static void validate(IResource resource, String sourceCode) {
 		Process proc = null;
 		Map attributes = new HashMap(11);
 
 		try {
 			// Construct command line parameters
 			List cmdList =
				PerlExecutableUtilities.getPerlExecutableCommandLine(resource.getProject());
 			cmdList.add(PERL_CMD_EXT);
 
 			if (PerlEditorPlugin.getDefault().getWarningsPreference()) {
 				cmdList.add("-w");
 			}
 
 			if (PerlEditorPlugin.getDefault().getTaintPreference()) {
 				cmdList.add("-T");
 			}
 
 			String[] cmdParams =
 				(String[]) cmdList.toArray(new String[cmdList.size()]);
 
 			// Get working directory -- Fixes Bug: 736631
 			String workingDir =
 				resource.getLocation().makeAbsolute().removeLastSegments(1).toString();
 
 			/*
 			 * Due to Java Bug #4763384 sleep for a very small amount of time
 			 * immediately after starting the subprocess
 			*/
 			proc =
 				Runtime.getRuntime().exec(
 					cmdParams,
 					null,
 					new File(workingDir));
 			Thread.sleep(1);
 
 			proc.getInputStream().close();
 			InputStream in = proc.getErrorStream();
 			OutputStream out = proc.getOutputStream();
 			//TODO which charset?
 			Reader inr = new InputStreamReader(in);
 			Writer outw = new OutputStreamWriter(out);
 
 			StringReaderThread srt = new StringReaderThread();
 			srt.read(inr);
 
 			try {
 				outw.write(sourceCode);
 				outw.write(0x1a); //this should avoid problem with Win98
 				outw.flush();
 			} catch (IOException e) {
 				e.printStackTrace();
 			}
 			out.close();
 
 			String content = srt.getResult();
 			inr.close();
 			in.close();
 
 			//TODO check if content is empty (indicates error)
 
 			// DEBUG start
 			System.out.println("-----------------------------------------");
 			System.out.println("           OUTPUT");
 			System.out.println("-----------------------------------------");
 			System.out.println(content);
 			System.out.println("-----------------------------------------");
 			// DEBUG END
 
 			String line = null;
 			List lines = new ArrayList();
 			int index;
 
 			StringTokenizer st = new StringTokenizer(content, "\n");
 
 			int lineCount = 0;
 
 			while (st.hasMoreTokens()) {
 				line = st.nextToken();
 				if (line.indexOf("\r") != -1) {
 					line = line.substring(0, line.indexOf("\r"));
 				}
 
 				lines.add(line);
 				if (++lineCount >= maxErrorsShown) {
 					break;
 				}
 
 			}
 
 			//Delete markers
 			resource.deleteMarkers(IMarker.PROBLEM, true, 1);
 
 			// Hash for tracking line severity
 			Map lineHash = new Hashtable();
 
 			// Markers have to be added in reverse order
 			// Otherwise lower line number will appear at the end of the list
 			for (int i = lines.size() - 1; i >= 0; i--) {
 				line = (String) lines.get(i);
 
 				// Delete filename from error message
 				StringBuffer lineSb = new StringBuffer(line);
 				line = lineSb.toString();
 
 				if ((index = line.indexOf(PERL_ERROR_INDICATOR)) != -1) {
 
 					// truncatedLIne is the stripped error-line up to the next " " after the line number if present
 					// To avoid cluttering with other "." and "," which might occur in the error message
 					String truncatedLine = line;
 					if (truncatedLine
 						.indexOf(" ", index + PERL_ERROR_INDICATOR.length() + 1)
 						!= -1) {
 						truncatedLine =
 							truncatedLine.substring(
 								0,
 								truncatedLine.indexOf(
 									" ",
 									index + PERL_ERROR_INDICATOR.length() + 1));
 					}
 
 					int endIndex;
 					if ((endIndex = truncatedLine.indexOf(".", index)) == -1) {
 						endIndex = truncatedLine.indexOf(",", index);
 					}
 
 					if (endIndex == -1) {
 						continue;
 					}
 
 					String lineNr =
 						truncatedLine.substring(
 							index + PERL_ERROR_INDICATOR.length(),
 							endIndex);
 
 					// If there is an addition to the error message
 					if (i + 1 < lines.size()) {
 						if (((String) lines.get(i + 1)).startsWith(" ")) {
 							line += " " + (String) lines.get(i + 1);
 						}
 					}
 
 					// Check if it's a warning
 					boolean isWarning = false;
 
 					for (int x = 0; x < WARNING_STRINGS.length; x++) {
 						if (truncatedLine.indexOf(WARNING_STRINGS[x]) != -1) {
 							isWarning = true;
 						}
 					}
 
 					if (isWarning) {
 						attributes.put(
 							IMarker.SEVERITY,
 							new Integer(IMarker.SEVERITY_WARNING));
 					} else {
 						attributes.put(
 							IMarker.SEVERITY,
 							new Integer(IMarker.SEVERITY_ERROR));
 					}
 
 					attributes.put(IMarker.MESSAGE, line);
 
 					attributes.put(
 						IMarker.LINE_NUMBER,
 						new Integer(Integer.parseInt(lineNr)));
 
 					// Check if a marker with a higher severity already exists
 					boolean doUnderline;
 					Object obj =
 						lineHash.get(new Integer(Integer.parseInt(lineNr)));
 					if (obj == null) {
 						doUnderline = true;
 					} else if (
 						((Integer) obj).intValue() == IMarker.SEVERITY_ERROR) {
 						doUnderline = false;
 					} else {
 						doUnderline = true;
 					}
 
 					if (doUnderline) {
 						lineHash.put(
 							new Integer(Integer.parseInt(lineNr)),
 							isWarning
 								? new Integer(IMarker.SEVERITY_WARNING)
 								: new Integer(IMarker.SEVERITY_ERROR));
 						// Get start and end offset
 						int lineOffset = 0;
 						try {
 							Document document = new Document(sourceCode);
 							lineOffset =
 								document.getLineOffset(
 									Integer.parseInt(lineNr) - 1);
 						} catch (Exception ex) {
 							continue;
 						}
 
 						int endOfLine = sourceCode.indexOf("\n", lineOffset);
 						String markerLine;
 
 						if (endOfLine != -1) {
 							markerLine =
 								sourceCode.substring(lineOffset, endOfLine);
 						} else {
 							markerLine = sourceCode.substring(lineOffset);
 						}
 
 						char[] bytes = markerLine.toCharArray();
 
 						int start = 0;
 						while (start < bytes.length) {
 							if (bytes[start] != '\t' && bytes[start] != ' ') {
 								break;
 							}
 							start++;
 						}
 
 						start += lineOffset;
 
 						int end = start + markerLine.trim().length();
 
 						attributes.put(IMarker.CHAR_START, new Integer(start));
 						attributes.put(IMarker.CHAR_END, new Integer(end));
 					}
 					
 					// Add markers
 					AddEditorMarker ed = new AddEditorMarker();
 					ed.addMarker(resource, attributes, IMarker.PROBLEM);
 
 				}
 			}
 
 		} catch (Exception e) {
 			e.printStackTrace();
 		} finally {
 			try {
 			} catch (Exception ex) {
 				ex.printStackTrace();
 			}
 		}
 
 
 	}
 }
