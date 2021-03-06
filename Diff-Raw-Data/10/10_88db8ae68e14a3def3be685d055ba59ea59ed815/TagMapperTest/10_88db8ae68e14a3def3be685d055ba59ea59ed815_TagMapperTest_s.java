 /*******************************************************************************
  * Copyright (c) 2012 György Orosz, Attila Novák.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the GNU Lesser Public License v3
  * which accompanies this distribution, and is available at
  * http://www.gnu.org/licenses/
  * 
  * This file is part of PurePos.
  * 
  * PurePos is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Lesser Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  * 
  * PurePos is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Lesser Public License for more details.
  * 
  * Contributors:
  *     György Orosz - initial API and implementation
  ******************************************************************************/
 package hu.ppke.itk.nlpg.purepos.model.internal;
 
 import java.util.Arrays;
 import java.util.List;
 
 import junit.framework.Assert;
 
 import org.junit.Test;
 
 public class TagMapperTest {
 
 	@Test
 	public void test() {
 		Vocabulary<String, Integer> vocabulary = new IntVocabulary<String>();
 		Integer fn = vocabulary.addElement("[FN][NOM]");
 		Integer mn = vocabulary.addElement("[MN][NOM]");
 		Integer fnlat = vocabulary.addElement("[FN|lat][NOM]");
 		Integer mnlat = vocabulary.addElement("[MN|lat][NOM]");
 		Integer ige = vocabulary.addElement("[IGE][Me3]");
 		TagMapper mapper = new TagMapper(vocabulary);
 		Assert.assertEquals(fn, mapper.map(fnlat));
		Assert.assertEquals(fn, fn);
		Assert.assertEquals(ige, ige);
		Assert.assertEquals(fn, fn);
		Assert.assertEquals(mn, mn);
 		List<Integer> from = Arrays.asList(fn, mnlat, fnlat);
 		List<Integer> to = Arrays.asList(fn, fn, fn);
 		Assert.assertEquals(to, mapper.map(from));
 	}
 }
