 /*******************************************************************************
  * Copyright (c) 2006-2010 
  * Software Technology Group, Dresden University of Technology
  * 
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0 
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *   Software Technology Group - TU Dresden, Germany 
  *      - initial API and implementation
  ******************************************************************************/
 package org.emftext.sdk.regex;
 
 import java.io.BufferedReader;
 import java.io.ByteArrayInputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.LinkedHashMap;
 import java.util.LinkedHashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import org.emftext.sdk.concretesyntax.TokenDefinition;
 import org.emftext.sdk.util.UnicodeConverter;
 
 import dk.brics.automaton.Automaton;
 import dk.brics.automaton.RegExp;
 
 public class TokenSorter {
 	private class ComparableTokenDefinition implements
 			Comparable<ComparableTokenDefinition> {
 		private Automaton automaton;
 		private TokenDefinition def;
 
 		public ComparableTokenDefinition(String regString, Automaton aut,
 				TokenDefinition definition) {
 			automaton = aut;
 			def = definition;
 		}
 
 		public Automaton getAutomaton() {
 			return automaton;
 		}
 
 		public TokenDefinition getDef() {
 			return def;
 		}
 
 		public int compareTo(ComparableTokenDefinition arg0) {
 			boolean firstComparison = isSubLanguage(automaton, arg0
 					.getAutomaton());
 			boolean secondComparison = isSubLanguage(arg0.getAutomaton(),
 					automaton);
 
 			// The first language is contained in the second.
 			if ((firstComparison == true) && (secondComparison == false))
 				return -1;
 
 			// The second language is contained in the first.
 			if ((firstComparison == false) && (secondComparison == true))
 				return 1;
 
 			// The languages are equal.
 			if ((firstComparison == true) && (secondComparison == true))
 				return 0;
 
 			// The languages can't be compared.
 			if ((firstComparison == false) && secondComparison == false)
 				return 0;
 			return 0;
 		}
 
 	}
 
 	private boolean doIntersect(Automaton firstLanguage,
 			Automaton secondLanguage) {
 		if (firstLanguage == null)
 			return true;
 		if (secondLanguage == null)
 			return true;
 
 		return (!firstLanguage.intersection(secondLanguage).isEmpty());
 	}
 
 	private boolean isSubLanguage(Automaton firstLanguage,
 			Automaton secondLanguage) {
 		Automaton complementSecond = secondLanguage.complement();
 
 		Automaton result = firstLanguage.intersection(complementSecond);
 
 		return result.isEmpty();
 
 		// return firstLanguage.subsetOf(secondLanguage);
 	}
 
 	public List<TokenDefinition> getNonReachables(List<TokenDefinition> ds)
 			throws SorterException {
 		List<TokenDefinition> nonReachables = new ArrayList<TokenDefinition>();
 		List<ComparableTokenDefinition> compareables = translateToComparables(ds);
		Automaton unionPreviosDefinitions = new Automaton();
		for (int i = 0; i < compareables.size() - 1; i++) {
			unionPreviosDefinitions = unionPreviosDefinitions
					.union(compareables.get(i).getAutomaton());
			ComparableTokenDefinition currentTokenDefinition = compareables.get(i+1);

			if (isSubLanguage(currentTokenDefinition.getAutomaton(),
					unionPreviosDefinitions)) {
				nonReachables.add(currentTokenDefinition.getDef());
 			}

 		}
 		return nonReachables;
 	}
 
 	/**
	 * Compares all given token definitions and determines conflicting
	 * definitions. For each conflicting definition the set of overlapping
	 * definitions is returned.
 	 * 
 	 * @param definitions
 	 * @return
 	 * @throws SorterException
 	 */
	public Map<TokenDefinition, Set<TokenDefinition>> getConflicting(
			List<TokenDefinition> definitions) throws SorterException {
 		Map<TokenDefinition, Set<TokenDefinition>> conflicting = new LinkedHashMap<TokenDefinition, Set<TokenDefinition>>();
 		List<ComparableTokenDefinition> compareables = translateToComparables(definitions);
 		for (int i = 0; i < compareables.size(); i++) {
 			ComparableTokenDefinition ci = compareables.get(i);
 			Set<TokenDefinition> previousDefinitions = new LinkedHashSet<TokenDefinition>();
 			for (int j = 0; j < i; j++) {
 				ComparableTokenDefinition cj = compareables.get(j);
 				if (doIntersect(ci.getAutomaton(), cj.getAutomaton())) {
 					previousDefinitions.add(cj.getDef());
 				}
 			}
 			if (!previousDefinitions.isEmpty()) {
 				conflicting.put(ci.getDef(), previousDefinitions);
 			}
 		}
 		return conflicting;
 	}
 
 	public List<TokenDefinition> sortTokens(List<TokenDefinition> toSort,
 			boolean ignoreUnreachables) throws SorterException {
 		List<ComparableTokenDefinition> compareables = translateToComparables(toSort);
 
 		Collections.sort(compareables);
 		// doSort(compareables);
 
 		List<TokenDefinition> resultList = new ArrayList<TokenDefinition>();
 		for (ComparableTokenDefinition directive : compareables) {
 			resultList.add(directive.getDef());
 		}
 		if (!ignoreUnreachables) {
 			List<TokenDefinition> conflicting = getNonReachables(resultList);
 			if (conflicting.size() > 0) {
 				throw new SorterException(
 						"Sorting Tokens failed. Grammar contains unreachable tokens",
 						conflicting);
 			}
 		}
 		return resultList;
 	}
 
 	private List<ComparableTokenDefinition> translateToComparables(
 			List<TokenDefinition> toSort) throws SorterException {
 		List<ComparableTokenDefinition> compareables = new ArrayList<ComparableTokenDefinition>();
 
 		for (TokenDefinition def : toSort) {
 			String original = def.getRegex();
 			compareables.add(createComparableTokenDirective(original, def));
 		}
 		return compareables;
 	}
 
 	private ComparableTokenDefinition createComparableTokenDirective(
 			String original, TokenDefinition def) throws SorterException {
 		String transformedRegExp = null;
 		try {
 			transformedRegExp = parseRegExp(original);
 			RegExp regExp = new RegExp(transformedRegExp);
 			Automaton automaton = regExp.toAutomaton();
 
 			return new ComparableTokenDefinition(transformedRegExp, automaton,
 					def);
 		} catch (Exception ex) {
 			ex.printStackTrace();
 			throw new SorterException(
 					"An error occurred while parsing a regular expression. The expression was: "
 							+ original);
 
 		}
 	}
 
 	/**
 	 * This method makes a transformation of the regular expression of the
 	 * EMFText to the format of the University of Aarhus automaton package. For
 	 * exmaple: the range operator in EMFText is '..' but in the automaton '-'.
 	 * 
 	 * @param exp
 	 *            regular expression to be transformed
 	 * @return the transformed regular expression
 	 * @throws Exception
 	 *             occurs if a parser error occurs
 	 */
 	private String parseRegExp(String exp) throws Exception {
		String regex = RegexpTranslationHelper
				.translateAntLRToAutomatonStyle(exp);
 		regex = convertUnicode(regex);
 
 		return regex;
 	}
 
 	private String convertUnicode(String regex) {
 		InputStream is = new ByteArrayInputStream(regex.getBytes());
 		UnicodeConverter uc = new UnicodeConverter(is);
 		BufferedReader reader = new BufferedReader(new InputStreamReader(uc));
 		StringBuilder sb = new StringBuilder();
 
 		String line = null;
 		try {
 			while ((line = reader.readLine()) != null) {
 				sb.append(line);
 			}
 		} catch (IOException e) {
 			e.printStackTrace();
 		} finally {
 			try {
 				is.close();
 			} catch (IOException e) {
 				e.printStackTrace();
 			}
 		}
 
 		regex = sb.toString();
 		return regex;
 	}
 
 	// private List<ComparableTokenDirective> doSort(
 	// List<ComparableTokenDirective> toSorted) {
 	// for (int i = 0; i < toSorted.size(); i++) {
 	// ComparableTokenDirective runHolder = toSorted.get(i);
 	//
 	// for (int j = i + 1; j < toSorted.size(); j++) {
 	// ComparableTokenDirective compareHolder = toSorted.get(j);
 	// int compare = runHolder.compareTo(compareHolder);
 	//
 	// if (compare > 0) {
 	// ComparableTokenDirective dummy = runHolder;
 	// toSorted.set(i, compareHolder);
 	// toSorted.set(j, dummy);
 	//
 	// runHolder = compareHolder;
 	// }
 	//
 	// }
 	// }
 	//
 	// return toSorted;
 	// }
 
 }
