 /*******************************************************************************
  * Copyright (c) 2005, 2009 Andrea Bittau, University College London, and others
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Andrea Bittau - initial API and implementation from the PsychoPath XPath 2.0 
  *     Jesper Steen Moeller - bug 282096 - clean up string storage
  *******************************************************************************/
 
 package org.eclipse.wst.xml.xpath2.processor.internal.function;
 
 import org.eclipse.wst.xml.xpath2.processor.DynamicError;
 import org.eclipse.wst.xml.xpath2.processor.ResultSequence;
 import org.eclipse.wst.xml.xpath2.processor.ResultSequenceFactory;
 import org.eclipse.wst.xml.xpath2.processor.internal.*;
 import org.eclipse.wst.xml.xpath2.processor.internal.types.*;
 
 import java.util.*;
 import java.util.regex.*;
 
 /**
  * This function breaks the $input string into a sequence of strings, treating
  * any substring that matches $pattern as a separator. The separators themselves
  * are not returned.
  */
 public class FnTokenize extends AbstractRegExFunction {
 	private static Collection _expected_args = null;
 
 	/**
 	 * Constructor for FnTokenize.
 	 */
 	public FnTokenize() {
 		super(new QName("tokenize"), 2, 3);
 	}
 
 	/**
 	 * Evaluate arguments.
 	 * 
 	 * @param args
 	 *            argument expressions.
 	 * @throws DynamicError
 	 *             Dynamic error.
 	 * @return Result of evaluation.
 	 */
 	@Override
 	public ResultSequence evaluate(Collection args) throws DynamicError {
 		return tokenize(args);
 	}
 
 	/**
 	 * Tokenize operation.
 	 * 
 	 * @param args
 	 *            Result from the expressions evaluation.
 	 * @throws DynamicError
 	 *             Dynamic error.
 	 * @return Result of fn:tokenize operation.
 	 */
 	public static ResultSequence tokenize(Collection args) throws DynamicError {
 		Collection cargs = Function.convert_arguments(args, expected_args());
 
 		ResultSequence rs = ResultSequenceFactory.create_new();
 
 		// get args
 		Iterator argiter = cargs.iterator();
 		ResultSequence arg1 = (ResultSequence) argiter.next();
 		String str1 = "";
 		if (!arg1.empty()) {
 			str1 = ((XSString) arg1.first()).value();
 		}
 
 		ResultSequence arg2 = (ResultSequence) argiter.next();
 		String pattern = ((XSString) arg2.first()).value();
 		String flags = null;
 
 		if (argiter.hasNext()) {
 			ResultSequence flagRS = null;
 			flagRS = (ResultSequence) argiter.next();
 			flags = flagRS.first().string_value();
 			if (validflags.indexOf(flags) == -1 && flags.length() > 0 ) {
 				throw DynamicError.regex_flags_error(null);
 			}
 		}
 
 		try {
 			ArrayList<String> ret = tokenize(pattern, flags, str1);
 
 			for(String token : ret) {
 				rs.add(new XSString(token));
 			}
 		} catch (PatternSyntaxException err) {
 			throw DynamicError.regex_error(null);
 		}
 
 		return rs;
 	}
 	
 	private static ArrayList<String> tokenize(String pattern, String flags, String src) throws DynamicError {
 		Matcher matcher = regex(pattern, flags, src);
 		ArrayList<String> tokens = new ArrayList<String>();
 		int startpos = 0;
 		int endpos = src.length();
 		while (matcher.find()) {
 			String delim = matcher.group();
 			if (delim.length() == 0) {
 				throw DynamicError.regex_match_zero_length(null);
 			}
 			String token = src.substring(startpos, matcher.start());
 			startpos = matcher.end();
 			tokens.add(token);
 		}
		if (startpos > 0 && startpos < endpos) {
 			String token = src.substring(startpos, endpos);
 			tokens.add(token);
 		}
 		return tokens;
 	}
 
 	/**
 	 * Obtain a list of expected arguments.
 	 * 
 	 * @return Result of operation.
 	 */
 	public static Collection expected_args() {
 		if (_expected_args == null) {
 			_expected_args = new ArrayList();
 			SeqType arg = new SeqType(new XSString(), SeqType.OCC_QMARK);
 			_expected_args.add(arg);
 			_expected_args.add(new SeqType(new XSString(), SeqType.OCC_NONE));
 			_expected_args.add(new SeqType(new XSString(), SeqType.OCC_NONE));
 		}
 
 		return _expected_args;
 	}
 }
