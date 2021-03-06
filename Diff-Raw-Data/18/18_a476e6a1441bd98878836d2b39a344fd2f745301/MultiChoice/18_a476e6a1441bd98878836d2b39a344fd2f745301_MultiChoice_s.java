 package org.digitalcampus.mobile.quiz.model.questiontypes;
 
 import java.io.Serializable;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 
 import org.digitalcampus.mobile.quiz.model.QuizQuestion;
 import org.digitalcampus.mobile.quiz.model.Response;
 import org.json.JSONException;
 import org.json.JSONObject;
 
 public class MultiChoice implements Serializable, QuizQuestion {
 
 	/**
 	 * 
 	 */
 	private static final long serialVersionUID = -6605393327170759582L;
 	public static final String TAG = "MultiChoice";
 	private int id;
 	private String title;
 	private List<Response> responseOptions = new ArrayList<Response>();
 	private float userscore = 0;
 	private List<String> userResponses = new ArrayList<String>();
 	private HashMap<String,String> props = new HashMap<String,String>();
 	private String feedback = "";
 	private boolean feedbackDisplayed = false;
 	
 	@Override
 	public void addResponseOption(Response r){
 		responseOptions.add(r);
 	}
 	
 	@Override
 	public List<Response> getResponseOptions(){
 		return responseOptions;
 	}
 	
 	@Override
 	public void mark(){
 		
 		// loop through the responses
 		// find whichever are set as selected and add up the responses
 		float total = 0;
 		for (Response r : responseOptions){
 			Iterator<String> itr = this.userResponses.iterator();
 			while(itr.hasNext()) {
 				String a = itr.next(); 
 				if (r.getTitle().equals(a)){
 					total += r.getScore();
 					if(r.getProp("feedback") != null && !(r.getProp("feedback").equals(""))){
 						this.feedback = r.getProp("feedback");
 					}
 				}
 			}
 		}
 		if(this.getProp("maxscore") != null){
 			int maxscore = Integer.parseInt(this.getProp("maxscore"));
 			if (total > maxscore){
 				userscore = maxscore;
 			} else {
 				userscore = total;
 			}
 		}
 	}
 	
 	@Override
 	public int getID() {
 		return this.id;
 	}
 	
 	@Override
 	public void setID(int id) {
 		this.id = id;	
 	}
 	
 	@Override
 	public String getTitle() {
 		return this.title;
 	}
 	
 	@Override
 	public void setTitle(String title) {
 		this.title = title;	
 	}
 
 	@Override
 	public void setResponseOptions(List<Response> responses) {
 		this.responseOptions = responses;
 	}
 
 	@Override
 	public float getUserscore() {
 		return this.userscore;
 	}
 
 	/*public void setUserResponse(List<String> str) {
 		if (str != this.userResponses){
 			this.setFeedbackDisplayed(false);
 		}
 		this.userResponses = str;
 		
 	}*/
 
 	@Override
 	public List<String> getUserResponses() {
 		return this.userResponses;
 	}
 
 	@Override
 	public void setProps(HashMap<String,String> props) {
 		this.props = props;
 	}
 	
 	@Override
 	public String getProp(String key) {
 		return props.get(key);
 	}
 
 	@Override
 	public void setUserResponses(List<String> str) {
 		if (!str.equals(this.userResponses)){
 			this.setFeedbackDisplayed(false);
 		}
 		this.userResponses = str;
		
 	}
 	
 	@Override
 	public String getFeedback() {
 		// reset feedback back to nothing
 		this.feedback = "";
 		this.mark();
 		return this.feedback;
 	}
 	
 	@Override
 	public int getMaxScore() {
 		return Integer.parseInt(this.getProp("maxscore"));
 	}
 	
 	@Override
 	public JSONObject responsesToJSON() {
 		JSONObject jo = new JSONObject();
 		if(userResponses.size() == 0){
 			try {
 				jo.put("question_id", this.id);
 				jo.put("score",userscore);
 				jo.put("text", "");
 			} catch (JSONException e) {
 				e.printStackTrace();
 			}
 			return jo;
 		}
 		
 		for(String ur: userResponses ){
 			try {
 				jo.put("question_id", this.id);
 				jo.put("score",userscore);
 				jo.put("text", ur);
 			} catch (JSONException e) {
 				e.printStackTrace();
 			}
 		}
 		return jo;
 	}
 	
 	@Override
 	public boolean responseExpected() {
 		if (this.props.containsKey("required")){
 			return Boolean.parseBoolean(this.getProp("required"));
 		}
 		return true;
 	}
 	
 	@Override
 	public int getScoreAsPercent() {
 		int pc = Integer.valueOf((int) (100* this.getUserscore()))/this.getMaxScore();
 		return pc;
 	}
 	
 	@Override
 	public void setFeedbackDisplayed(boolean feedbackDisplayed) {
 		this.feedbackDisplayed = feedbackDisplayed;
 		
 	}
 
 	@Override
 	public boolean getFeedbackDisplayed() {
 		return feedbackDisplayed;
 	}
 
 }
 
