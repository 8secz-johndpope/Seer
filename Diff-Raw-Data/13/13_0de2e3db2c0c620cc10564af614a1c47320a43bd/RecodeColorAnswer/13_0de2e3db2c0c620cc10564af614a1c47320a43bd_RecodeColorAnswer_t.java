 package it.bisi.report.jasper.scriptlet;
 
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Map.Entry;
 
 import org.json.simple.JSONObject;
 import org.json.simple.JSONValue;
 
 import net.sf.jasperreports.engine.JRDefaultScriptlet;
 import net.sf.jasperreports.engine.JRScriptletException;
 
 public class RecodeColorAnswer extends JRDefaultScriptlet {
 
 	/**
 	 * Main purpose recode answer number (field) to 1,2,3 (negative,neutral, positive)
 	* for generating barchart and color info.
 	* if no valid recoded answer is given, valueRecoded is set to original answer
 	* TODO check if it is better to set valueRecoded to null
 	 * @param questionIdXform
 	 * @param answer
 	 * @return recodedAnswer
 	 * @throws JRScriptletException
 	 */
	public String recodeColorAnswer(String questionId, Double answer, String output) throws JRScriptletException
 	{
 		//get recode scheme
 		@SuppressWarnings("unchecked")
 		HashMap<String, Map<String, Map<String, Object>>> recodeScheme=(HashMap<String, Map<String, Map<String, Object>>>) this.getParameterValue("RECODE_COLOR_MAP");
 		//get recode scheme for this scale Type
 		//get json map: question id: type of question (questionId:number)
 		String questionInfoScale=(String) this.getParameterValue("SCALE_QUESTION_INFO");
 		JSONObject questionInfoScaleMap=(JSONObject) JSONValue.parse(questionInfoScale);
 		String scaleType=(String) questionInfoScaleMap.get(questionId);
		
 		Map<String, Map<String, Object>> scaleInfo=recodeScheme.get(scaleType);
 		
 		//iterate through scale type rows and find recoded Answer
 		//we test on >= and <= however the upper limit is excluded except for the last categorie, this is because if the value is te upper margin, it is also the lower margin of the next categorie
 		Double recodedAnswer = null;
 		String color=null;
 		for (Entry<String, Map<String, Object>> entry : scaleInfo.entrySet()) {
 			if (answer >= (Double) entry.getValue().get("lowest")  && answer <= (Double) entry.getValue().get("highest") ){
 				recodedAnswer= (Double) entry.getValue().get("targetValue");
 				color= (String) entry.getValue().get("color");
 				
 				
 			}
 		}
 		if (output.equals("color")) {
 			return color;
 		}else {
 			if (recodedAnswer==null){
 				recodedAnswer=answer;
 			}
 			//set recodedVariable
 			return recodedAnswer.toString();	
 		}
 	}
 }
