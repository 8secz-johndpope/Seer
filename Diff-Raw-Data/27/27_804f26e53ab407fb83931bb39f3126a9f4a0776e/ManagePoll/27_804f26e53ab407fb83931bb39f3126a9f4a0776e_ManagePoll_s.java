 package controllers;
 import java.util.Calendar;
 import java.util.Date;
 import java.util.List;
 
 import play.Logger;
 import play.mvc.Controller;
 import play.mvc.Http.StatusCode;
 import play.mvc.results.RenderJson;
 import api.entities.ChoiceJSON;
 import api.entities.PollInstanceJSON;
 import api.entities.PollJSON;
 import api.entities.VoteJSON;
 import api.requests.CreatePollInstanceRequest;
 import api.requests.CreatePollRequest;
 import api.requests.ReadPollByTokenRequest;
 import api.requests.ReadPollInstanceRequest;
 import api.requests.ReadPollRequest;
 import api.requests.ReadUserDetailsRequest;
 import api.requests.UpdatePollRequest;
 import api.requests.VoteOnPollInstanceRequest;
 import api.responses.CreatePollInstanceResponse;
 import api.responses.CreatePollResponse;
 import api.responses.ReadPollInstanceResponse;
 import api.responses.ReadPollResponse;
 import api.responses.ReadPollByTokenResponse;
 import api.responses.ReadUserDetailsResponse;
 import api.responses.Response;
 import api.responses.UpdatePollResponse;
 import api.responses.VoteOnPollInstanceResponse;
 
 public class ManagePoll extends BaseController {
 	public static void index() {
     	
 		APIClient apiClient = new APIClient();
 		/** get all polls + instances for the current user */
     	try {
 			ReadUserDetailsResponse responseUser = (ReadUserDetailsResponse) apiClient.sendRequest(new ReadUserDetailsRequest(controllers.LoginUser.getCurrentUserId()));
 			renderArgs.put("pollsJson", responseUser.polls);
 			render();
     	} catch (Exception e) {
 			e.printStackTrace();
 		}
 	}
 
 	public static void update(String token, String reference, Boolean multipleAllowed, String question, List<ChoiceJSON> choices) {
 		validation.required(reference);
 		validation.required(multipleAllowed);
 		validation.required(question);
 		validation.required(choices);
 
 		if (!validation.hasErrors()) {
 			// TODO: error handling
 			}
 
 		PollJSON pollJson = new PollJSON();
 		pollJson.choices = choices;
 		pollJson.multipleAllowed = multipleAllowed;
 		pollJson.question = question;
 		pollJson.reference = reference;
 
 		try {
 			UpdatePollResponse response = (UpdatePollResponse) APIClient.send(new UpdatePollRequest(pollJson));
 		} catch (Exception e) {
 			// TODO: tell the user it failed
 		}
 	}
 
 
 
 	public static void activate(Date start, Date end) {
 		// TODO: this! (activate/instantiate pollinstance with start and end time
 		PollInstanceJSON pollInstance = new PollInstanceJSON();
 		pollInstance.end = end;
 		pollInstance.start = start;
 
 		validation.required(end);
 		validation.required(start);
 
 		if (!validation.hasErrors()) {
 			try {
 				CreatePollInstanceResponse response = (CreatePollInstanceResponse) APIClient.send(new CreatePollInstanceRequest(pollInstance));
 			} catch (Exception e) {
 				// TODO Exception handling
 			}
 			render();
 		}
 		else {
 			// TODO error handling
 		}
 	}
 
 	public static void clonepoll() {
 		// TODO: this! (clone existing poll)
 	}
 
	public static void statistics() {
 		try {
 //			// Load service data.
 //			Response res0 = APIClient.loadServiceData("data.yml");
 //			if(!StatusCode.success(res0.statusCode)) {
 //				throw new Exception("Couldn't load test data.");
 //			}
 //			boolean authenticated = APIClient.authenticateSimple("spam@creen.dk", "openarms");
 //			if(!authenticated) {
 //				throw new Exception("Could not authenticate");
 //			}
 //
 //			ReadPollByTokenResponse res1 = (ReadPollByTokenResponse)APIClient.send(new ReadPollByTokenRequest("123456"));
 //			if(!StatusCode.success(res1.statusCode)) {
 //				throw new Exception("Couldn't read poll with token 123456.");
 //			}
 //			
 //			PollInstanceJSON pi = new PollInstanceJSON();
 //			pi.poll_id = res1.poll.id;
 //			pi.start = Calendar.getInstance().getTime();
 //			Calendar endDateTimeCalendar = Calendar.getInstance();
 //			endDateTimeCalendar.set(Calendar.YEAR, 2020);
 //			pi.end = endDateTimeCalendar.getTime();
 //			CreatePollInstanceResponse res2 = (CreatePollInstanceResponse)APIClient.send(new CreatePollInstanceRequest(pi));
 //			if(!StatusCode.success(res2.statusCode)) {
 //				throw new Exception("Couldn't create poll instance with token time now.");
 //			}
 //
 //        	for(ChoiceJSON c: res1.poll.choices) {
 //        		int voteCount = (int)(Math.round(Math.random()*100.0));
 //		        for(int vIndex = 0; vIndex < voteCount; vIndex++) {
 //		        	Logger.debug("Creating a new vote (%d/%d) for choice %s on pollinstance %s", vIndex, voteCount, c.id, res2.pollinstance.id);
 //		        	VoteJSON v = new VoteJSON();
 //		        	v.choiceid = c.id;
 //		        	v.pollInstanceid = res2.pollinstance.id;
 //		        	VoteOnPollInstanceResponse res = (VoteOnPollInstanceResponse)APIClient.send(new VoteOnPollInstanceRequest(v));
 //		        	if(!StatusCode.success(res.statusCode)) {
 //						throw new Exception("Couldn't create vote.");
 //		        	}
 //		        }
 //        	}
			ReadPollInstanceResponse res = (ReadPollInstanceResponse) APIClient.send(new ReadPollInstanceRequest((long) 1));
 			PollInstanceJSON pollInstance = res.pollinstance;
 			render(pollInstance);
 		} catch(Exception e) {
 			e.printStackTrace();
 			// TODO: Use an error template.
 		}
 	}
 
 	public static void testStatistics() {
 		renderText("{\"pollID\":518196,\"questionID\":127,\"question\":\"Question?\",\"answers\":[\"Answer 1\",\"Answer 2\", \"Answer 3\"],\"votes\":[50, 10, 40]}");
 	}
}
