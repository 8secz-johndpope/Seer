 /*******************************************************************************
  * Copyright (c) 2010 itemis AG (http://www.itemis.eu) and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *******************************************************************************/
 package de.itemis.xtext.antlr.splitting;
 
 import java.io.BufferedReader;
 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.PrintWriter;
 import java.io.StringReader;
 import java.io.StringWriter;
 import java.util.Collections;
 import java.util.Map;
 
 import org.eclipse.emf.common.util.URI;
 import org.eclipse.emf.ecore.resource.Resource;
 import org.eclipse.xtext.parsetree.reconstr.SerializerUtil;
 import org.eclipse.xtext.resource.IResourceFactory;
 import org.eclipse.xtext.resource.XtextResource;
 
 import com.google.inject.Injector;
import com.google.common.collect.Maps;
 
 import de.itemis.xtext.antlr.splitting.simpleExpressions.IfCondition;
 
 /**
  * @author Sebastian Zarnekow - Initial contribution and API
  */
 public class UnorderedGroupsSplitter {
 
 	private final static String INTERNAL_ENCODING = "UTF-8";
 	private final String content;
 	private ConditionSimplifier simplifier;
 
 	public UnorderedGroupsSplitter(String content) {
 		this.content = content;
 		this.simplifier = new ConditionSimplifier();
 	}
 
 	public String transform() {
 		Injector injector = new SimpleExpressionsStandaloneSetup().createInjectorAndDoEMFRegistration();
 		IResourceFactory resourceFactory = injector.getInstance(IResourceFactory.class);
 		return transformContent(resourceFactory); 
 	}
 
 	protected String transformContent(IResourceFactory resourceFactory) {
 		BufferedReader reader = new BufferedReader(new StringReader(content));
 		try {
 			try {
 				StringWriter writer = new StringWriter(content.length());
 				PrintWriter printer = new PrintWriter(writer);
 				String line = reader.readLine();
 				while(line != null) {
 					line = transfromLine(line, resourceFactory);
 					printer.println(line);
 					line = reader.readLine();
 				}
 				printer.close();
 				return writer.toString();
 			} finally {
 				reader.close();
 			}
 		} catch(IOException e) {
 			throw new RuntimeException(e);
 		}
 	}
 	
 	public String transfromLine(String line, IResourceFactory resourceFactory) throws IOException {
 		if (shouldSimplify(line)) {
 			int braceIdx = line.indexOf('{');
 			String lineAsInput = line.substring(0, braceIdx + 1);
 			Resource resource = getResource(lineAsInput, resourceFactory);
 			IfCondition condition = (IfCondition) resource.getContents().get(0);
 			simplifier.simplify(condition);
 			String fixedLine = saveResource(resource);
 			fixedLine = addPreAndPostfix(line, braceIdx, fixedLine);
 			return fixedLine;
 		} else {
 			return line;
 		}
 	}
 
 	protected String addPreAndPostfix(String line, int braceIdx, String fixedLine) {
 		int idx = line.indexOf("else if");
 		if (idx >= 0) {
 			fixedLine = line.substring(0, idx) + fixedLine;
 		} else {
 			idx = line.indexOf("if");
 			if (idx >= 0) {
 				fixedLine = line.substring(0, idx) + fixedLine;
 			}
 		}
 		if (braceIdx != line.length() - 1)
 			fixedLine = fixedLine + line.substring(braceIdx + 1);
 		return fixedLine;
 	}
 
 	protected String saveResource(Resource resource) throws IOException {
 		ByteArrayOutputStream out = new ByteArrayOutputStream(content.length());
 		try {
 			Map<String, Object> options = Maps.newHashMap();
 			options.put(XtextResource.OPTION_ENCODING, INTERNAL_ENCODING);
 			options.put(XtextResource.OPTION_SERIALIZATION_OPTIONS, SerializerUtil.NO_FORMAT);
 			resource.save(out, options);
 			String result = new String(out.toByteArray(), INTERNAL_ENCODING);
 			return result;
 		} finally {
 			out.close();
 		}
 	}
 
 	protected Resource getResource(String input, IResourceFactory resourceFactory) throws IOException {
 		InputStream inputStream = new ByteArrayInputStream(input.getBytes(INTERNAL_ENCODING));
 		try {
 			Resource resource = resourceFactory.createResource(URI.createURI("dummy.simpleexpressions"));
 			resource.load(inputStream, Collections.singletonMap(XtextResource.OPTION_ENCODING, INTERNAL_ENCODING));
 			if (!resource.getErrors().isEmpty()) {
 				throw new RuntimeException(input + " - " + resource.getErrors().toString());
 			}
 			inputStream.close();
 			return resource;
 		} finally {
 			inputStream.close();
 		}
 	}
 
 	public boolean shouldSimplify(String line) {
 		String trimmedLine = line.trim();
 		return (trimmedLine.startsWith("else if") || trimmedLine.startsWith("if")) && trimmedLine.contains("getUnorderedGroupHelper()");
 	}
 	
 }
