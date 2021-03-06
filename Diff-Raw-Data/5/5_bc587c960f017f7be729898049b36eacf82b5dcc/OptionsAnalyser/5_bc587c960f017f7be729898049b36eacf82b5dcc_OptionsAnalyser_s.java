 /*******************************************************************************
  * Copyright (c) 2006-2009 
  * Software Technology Group, Dresden University of Technology
  * 
  * This program is free software; you can redistribute it and/or modify it under
  * the terms of the GNU Lesser General Public License as published by the Free
  * Software Foundation; either version 2 of the License, or (at your option) any
  * later version. This program is distributed in the hope that it will be useful, but
  * WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  * 
  * See the GNU Lesser General Public License for more details. You should have
  * received a copy of the GNU Lesser General Public License along with this
  * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place,
  * Suite 330, Boston, MA  02111-1307 USA
  * 
  * Contributors:
  *   Software Technology Group - TU Dresden, Germany 
  *   - initial API and implementation
  ******************************************************************************/
 package org.emftext.sdk.syntax_analysis;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import org.emftext.runtime.resource.ITextResource;
 import org.emftext.sdk.AbstractPostProcessor;
 import org.emftext.sdk.codegen.OptionManager;
 import org.emftext.sdk.concretesyntax.ConcreteSyntax;
 import org.emftext.sdk.concretesyntax.Option;
 import org.emftext.sdk.concretesyntax.OptionTypes;
 
 /**
  * A post processor that checks whether the values for all code generation
  * options are valid.
  */
 public class OptionsAnalyser extends AbstractPostProcessor {
 
 	private final List<OptionTypes> BOOLEAN_OPTIONS;
 	private final List<OptionTypes> STRING_OPTIONS;
 	
 	public OptionsAnalyser() {
 		BOOLEAN_OPTIONS = new ArrayList<OptionTypes>();
 		BOOLEAN_OPTIONS.add(OptionTypes.AUTOFIX_SIMPLE_LEFTRECURSION);
 		BOOLEAN_OPTIONS.add(OptionTypes.FORCE_EOF);
 		BOOLEAN_OPTIONS.add(OptionTypes.GENERATE_TEST_ACTION);
 		BOOLEAN_OPTIONS.add(OptionTypes.GENERATE_CODE_FROM_GENERATOR_MODEL);
 		BOOLEAN_OPTIONS.add(OptionTypes.GENERATE_PRINTER_STUB_ONLY);
 		BOOLEAN_OPTIONS.add(OptionTypes.RELOAD_GENERATOR_MODEL);
 		BOOLEAN_OPTIONS.add(OptionTypes.OVERRIDE_PLUGIN_XML);
 		BOOLEAN_OPTIONS.add(OptionTypes.OVERRIDE_MANIFEST);
 		BOOLEAN_OPTIONS.add(OptionTypes.OVERRIDE_PARSER);
 		BOOLEAN_OPTIONS.add(OptionTypes.OVERRIDE_TOKEN_RESOLVERS);
 		BOOLEAN_OPTIONS.add(OptionTypes.OVERRIDE_REFERENCE_RESOLVERS);
 		BOOLEAN_OPTIONS.add(OptionTypes.OVERRIDE_REFERENCE_RESOLVER_SWITCH);
 		BOOLEAN_OPTIONS.add(OptionTypes.OVERRIDE_TOKEN_RESOLVER_FACTORY);
 		BOOLEAN_OPTIONS.add(OptionTypes.OVERRIDE_PRINTER);
 		BOOLEAN_OPTIONS.add(OptionTypes.OVERRIDE_PRINTER_BASE);
 		BOOLEAN_OPTIONS.add(OptionTypes.USE_PREDEFINED_TOKENS);
 		BOOLEAN_OPTIONS.add(OptionTypes.ANTLR_BACKTRACKING);
 		BOOLEAN_OPTIONS.add(OptionTypes.ANTLR_MEMOIZE);
 		BOOLEAN_OPTIONS.add(OptionTypes.OVERRIDE_DOT_CLASSPATH);
 		BOOLEAN_OPTIONS.add(OptionTypes.OVERRIDE_DOT_PROJECT);
 		BOOLEAN_OPTIONS.add(OptionTypes.OVERRIDE_TEXT_RESOURCE);
 		BOOLEAN_OPTIONS.add(OptionTypes.OVERRIDE_RESOURCE_FACTORY);
 		BOOLEAN_OPTIONS.add(OptionTypes.OVERRIDE_NEW_FILE_WIZARD);
 
 		STRING_OPTIONS = new ArrayList<OptionTypes>();
 		STRING_OPTIONS.add(OptionTypes.BASE_PACKAGE);
 		STRING_OPTIONS.add(OptionTypes.RESOURCE_PLUGIN_ID);
 		STRING_OPTIONS.add(OptionTypes.SOURCE_FOLDER);
 		STRING_OPTIONS.add(OptionTypes.PARSER_GENERATOR);
 	}
 
 	@Override
 	public void analyse(ITextResource resource, ConcreteSyntax syntax) {
 		List<Option> options = syntax.getOptions();
 		for (Option option : options) {
 			analyseOption(resource, option);
 		}
 	}
 
 	private void analyseOption(ITextResource resource, Option option) {
 		OptionTypes type = option.getType();
 		String value = option.getValue();
 		checkValue(resource, option, type, value);
 	}
 
 	private void checkValue(ITextResource resource, Option option, OptionTypes type, String value) {
 		if (BOOLEAN_OPTIONS.contains(type)) {
 			checkBooleanValue(resource, option, type, value);
 		} else if (STRING_OPTIONS.contains(type)) {
 			// string values are accepted as they are
 		} else if (type == OptionTypes.TOKENSPACE) {
 			checkTokenspaceValue(resource, option, value);
		} else if (type == OptionTypes.PARSER_GENERATOR) {
			checkParserGeneratorValue(resource, option, value);
 		} else if (type == OptionTypes.DEFAULT_TOKEN_NAME) {
 			checkDefaultTokenNameValue(resource, option, value);
 		} else {
 			addProblem(resource, ECsProblemType.UNKNOWN_OPTION, "Unknown option (" + type + ").", option);
 		}
 	}
 
 	private void checkParserGeneratorValue(ITextResource resource,
 			Option option, String value) {
 		if (!OptionManager.ANTLR.equals(value) && !OptionManager.SCALES.equals(value)) {
 			addProblem(resource, ECsProblemType.INVALID_PARSER_GENERATOR, "Invalid parser generator (Valid generators are: " + OptionManager.ANTLR + ", " + OptionManager.SCALES + ").", option);
 		}
 	}
 
 	private void checkDefaultTokenNameValue(ITextResource resource,
 			Option option, String value) {
 		if (value == null || value.length() < 2) {
 			addProblem(resource, ECsProblemType.INVALID_DEFAULT_TOKEN_NAME, "Please provide a String with at least two letters.", option);
 		}
 	}
 
 	private void checkTokenspaceValue(ITextResource resource, Option option,
 			String value) {
 		try {
 			int v = Integer.parseInt(value);
 			if (v < 0) {
 				addProblem(resource, ECsProblemType.OPTION_VALUE_MUST_BE_POSITIVE_INTEGER, "Only positive integers are allowed.", option);
 			}
 		} catch (NumberFormatException e) {
 			addProblem(resource, ECsProblemType.OPTION_VALUE_MUST_BE_INTEGER, "Only integers are allowed.", option);
 		}
 	}
 
 	private void checkBooleanValue(ITextResource resource, Option option,
 			OptionTypes type, String value) {
 		boolean isTrue = "true".equals(value);
 		boolean isFalse = "false".equals(value);
 		if (!isTrue && !isFalse) {
 			addProblem(resource, ECsProblemType.OPTION_VALUE_MUST_BE_BOOLEAN, "Only boolean values: 'true' or 'false' are supported.", option);
 		}
 	}
 }
