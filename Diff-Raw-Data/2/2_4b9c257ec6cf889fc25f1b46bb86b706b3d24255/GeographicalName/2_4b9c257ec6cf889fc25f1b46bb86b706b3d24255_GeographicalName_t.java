 /*
  * HUMBOLDT: A Framework for Data Harmonisation and Service Integration.
  * EU Integrated Project #030962                  01.10.2006 - 30.09.2010
  * 
  * For more information on the project, please refer to the this web site:
  * http://www.esdi-humboldt.eu
  * 
  * LICENSE: For information on the license under which this program is 
  * available, please refer to http:/www.esdi-humboldt.eu/license.html#core
  * (c) the HUMBOLDT Consortium, 2007 to 2010.
  */
 
package eu.esdihumboldt.inspire.data;
 
 import java.util.ArrayList;
 
 
 /**
  * This is the INPSIRE GeographicalName object implementation
  * 
  * @author Ana Belen Anton & Jose Ignacio Gisbert
  * @partner 02 / ETRA Research and Development
  * @version $Id$ 
  */
 public class GeographicalName {
 
 	private ArrayList<SpellingOfName> spelling=new ArrayList<SpellingOfName>();
 	private String language=null;
 	private NativenessValue nativeness=null;
 	private NameStatusValue nameStatus=null;
 	private String sourceOfName=null;
 	private PronunciationOfName pronunciation=null;
 	private GrammaticalGenderValue grammaticalGender=null;
 	private GrammaticalNumberValue grammaticalNumber=null;
 	
 	public GeographicalName(){}
 	
 	public ArrayList<SpellingOfName> getSpellingList(){return spelling;}
 	public int getSpellingCount(){return spelling.size();}
 	public SpellingOfName getSpelling(int n){return spelling.get(n);}
 	public void setSpelling(SpellingOfName sp)
 	{
 		try {
 			spelling.clear();
 			spelling.add(0,(SpellingOfName)sp.clone());
 		} catch (CloneNotSupportedException e) {
 			e.printStackTrace();
 		}
 	}
 	public void addSpelling(SpellingOfName sp)
 	{
 		try {
 			spelling.add(0,(SpellingOfName)sp.clone());
 		} catch (CloneNotSupportedException e) {
 			e.printStackTrace();
 		}
 	}
 	
 	public String getLanguage(){return language;}
 	public void setLanguage(String lg){language=lg;}
 	
 	public NativenessValue getNativeness(){return nativeness;}
 	public void setNativeness(NativenessValue nt){nativeness=nt;}
 	
 	public NameStatusValue getNameStatus(){return nameStatus;}
 	public void setNameStatus(NameStatusValue nm){nameStatus=nm;}
 	
 	public String getSourceOfName(){return sourceOfName;}
 	public void setSourceOfName(String sn){sourceOfName=sn;}
 	
 	public PronunciationOfName getPronunciation(){return pronunciation;}
 	public void setPronunciation(PronunciationOfName pr)
 	{
 		try{
 			pronunciation=(PronunciationOfName)pr.clone();
 		}
 		catch(CloneNotSupportedException e)
 		{
 			e.printStackTrace();
 		}
 	}
 	
 	public GrammaticalGenderValue getGrammaticalGender(){return grammaticalGender;}
 	public void setGrammaticalGender(GrammaticalGenderValue gv){grammaticalGender=gv;}
 	
 	public GrammaticalNumberValue getGrammaticalNumber(){return grammaticalNumber;}
 	public void setGrammaticalNumber(GrammaticalNumberValue gn){grammaticalNumber=gn;}
 	
 	public boolean equals(GeographicalName target)
 	{
 		if (target==null)
 			return false;
 		
 		if (spelling.size()!=target.getSpellingCount())
 			return false;
 		else 
 		{
 			for (int j=0;j<spelling.size();j++)
 				if (spelling.get(j).equals(target.getSpelling(j))==false)
 					return false;
 		}
 		
 		if (pronunciation==null && target.getPronunciation()!=null)
 			return false;
 		else if (pronunciation.equals(target.getPronunciation())==false)
 			return false;
 		
 		if (language!=null && language.equals(target.getLanguage())==false)
 			return false;
 		else if (language==null && target.getLanguage()!=null)
 			return false;
 		
 		if (nativeness!=null && nativeness.equals(target.getNativeness())==false)
 			return false;
 		else if (nativeness==null && target.getNativeness()!=null)
 			return false;
 		
 		if (nameStatus!=null && nameStatus.equals(target.getNameStatus())==false)
 			return false;
 		else if (nameStatus==null && target.getNameStatus()!=null)
 			return false;
 		
 		if (sourceOfName!=null && sourceOfName.equals(target.getSourceOfName())==false)
 			return false;
 		else if (sourceOfName==null && target.getSourceOfName()!=null)
 			return false;
 		
 		if (grammaticalGender!=null && grammaticalGender.equals(target.getGrammaticalGender())==false)
 			return false;
 		else if (grammaticalGender==null && target.getGrammaticalGender()!=null)
 			return false;
 		
 		if (grammaticalNumber!=null && grammaticalNumber.equals(target.getGrammaticalNumber())==false)
 			return false;
 		else if (grammaticalNumber==null && target.getGrammaticalNumber()!=null)
 			return false;
 				
 		return true;
 	}
 	
 	public boolean equalsuniquevalues(GeographicalName target)
 	{
 		if (pronunciation==null && target.getPronunciation()!=null)
 			return false;
 		else if (pronunciation.equals(target.getPronunciation())==false)
 			return false;
 		
 		if (language!=null && language.equals(target.getLanguage())==false)
 			return false;
 		else if (language==null && target.getLanguage()!=null)
 			return false;
 		
 		if (nativeness!=null && nativeness.equals(target.getNativeness())==false)
 			return false;
 		else if (nativeness==null && target.getNativeness()!=null)
 			return false;
 		
 		if (nameStatus!=null && nameStatus.equals(target.getNameStatus())==false)
 			return false;
 		else if (nameStatus==null && target.getNameStatus()!=null)
 			return false;
 		
 		if (sourceOfName!=null && sourceOfName.equals(target.getSourceOfName())==false)
 			return false;
 		else if (sourceOfName==null && target.getSourceOfName()!=null)
 			return false;
 		
 		if (grammaticalGender!=null && grammaticalGender.equals(target.getGrammaticalGender())==false)
 			return false;
 		else if (grammaticalGender==null && target.getGrammaticalGender()!=null)
 			return false;
 		
 		if (grammaticalNumber!=null && grammaticalNumber.equals(target.getGrammaticalNumber())==false)
 			return false;
 		else if (grammaticalNumber==null && target.getGrammaticalNumber()!=null)
 			return false;
 		return true;
 	}
 }
