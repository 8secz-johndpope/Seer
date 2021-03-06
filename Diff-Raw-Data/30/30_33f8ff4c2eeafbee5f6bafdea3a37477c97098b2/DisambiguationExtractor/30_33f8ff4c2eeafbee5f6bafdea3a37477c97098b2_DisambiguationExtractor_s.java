 /*
  * This file is part of CoAnSys project.
  * Copyright (c) 2012-2013 ICM-UW
  * 
  * CoAnSys is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
 
  * CoAnSys is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU Affero General Public License for more details.
  * 
  * You should have received a copy of the GNU Affero General Public License
  * along with CoAnSys. If not, see <http://www.gnu.org/licenses/>.
  */
 package pl.edu.icm.coansys.disambiguation.author.features.extractors.indicators;
 
 import pl.edu.icm.coansys.disambiguation.author.normalizers.PigNormalizer;
import pl.edu.icm.coansys.disambiguation.author.normalizers.ToEnglishLowerCase;
 import pl.edu.icm.coansys.disambiguation.author.normalizers.ToHashCode;
 
 public class DisambiguationExtractor {
 
 	private final PigNormalizer normalizers[];
 
 	public DisambiguationExtractor(){
 		normalizers = new PigNormalizer[] {
				new ToEnglishLowerCase(), 
 				new ToHashCode()
 			};
 	}
 	
 	public DisambiguationExtractor( PigNormalizer[] new_normalizers ) {
 		normalizers = new_normalizers.clone();
 	}
 
 	public Object normalizeExtracted( Object extracted ) {
 		Object tmp = extracted;
 		for ( PigNormalizer pn: normalizers ) {
 			tmp = pn.normalize( tmp );
 		}
 		return tmp;
 	}
 	
 	public static boolean isClassifCode(String str) {
 		return isMSc(str); 
 	}
 
 	public static boolean isMSc(String str) {
 		return str.toUpperCase().matches("[0-9][0-9][A-Z][0-9][0-9]");
 	}
 	
 	//to re-implement in each extractor
 	public String getId() {
 		return null;
 	}
 }
