 package com.madgag.agit;
 
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.app.Dialog;
 import android.content.DialogInterface;
 import android.util.Log;
 import android.widget.EditText;
 import com.google.inject.Inject;
 import com.madgag.agit.blockingprompt.PromptHumper;
 import com.madgag.agit.blockingprompt.PromptUIProvider;
 import com.madgag.agit.blockingprompt.ResponseInterface;
 
 public class DialogPromptMonkey implements PromptUIProvider  {
 
 
     private final Activity activity;
     private final PromptHumper promptHumper;
 
     public static final int YES_NO_DIALOG=1,STRING_ENTRY_DIALOG=2;
     private final String TAG="DPM";
     private ResponseInterface responseInterface;
 
     @Inject
     public DialogPromptMonkey(Activity activity, PromptHumper promptHumper) {
         this.activity = activity;
         this.promptHumper = promptHumper;
     }
 
     protected Dialog onCreateDialog(int id) {
         AlertDialog.Builder builder = new AlertDialog.Builder(activity);
 		switch (id) {
 		case YES_NO_DIALOG:
 			builder.setMessage("...")
                 .setPositiveButton("Yes", sendDialogResponseOf(true))
                 .setNegativeButton("No", sendDialogResponseOf(false));
             break;
 		case STRING_ENTRY_DIALOG:
 			final EditText input = new EditText(activity);
 			builder.setView(input);
 			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
 				public void onClick(DialogInterface dialog, int which) {
                     responseInterface.setResponse(input.getText().toString());
 				}
 			});
             break;
 		}
         return builder.create();
 	}
 
 
     public void registerReceiverForServicePromptRequests() {
         Log.d(TAG, "Registering as prompt UI provider with "+promptHumper);
     	promptHumper.setActivityUIProvider(this);
 	}
 
 	public void unregisterRecieverForServicePromptRequests() {
 		promptHumper.clearActivityUIProvider(this);
 	}
 
     private DialogInterface.OnClickListener sendDialogResponseOf(final boolean bool) {
 		return new DialogInterface.OnClickListener() {
 			public void onClick(DialogInterface dialog, int which) {
                 responseInterface.setResponse(bool);
 			}
 		};
 	}
 
 	protected void onPrepareDialog(int id, Dialog dialog) {
         AlertDialog alertDialog = (AlertDialog) dialog;
 		switch (id) {
 		case YES_NO_DIALOG:
 			String msg = responseInterface.getOpPrompt().getOpNotification().getEventDetail();
			Log.d(TAG, "Going to yes/no " + msg);
 			alertDialog.setMessage(msg);
 		default:
 		}
 	}
 
 
 	void updateUIToReflectServicePromptRequests() {
 		if (responseInterface!=null && responseInterface.getOpPrompt()!=null) {
 			Class<?> requiredResponseType = responseInterface.getOpPrompt().getRequiredResponseType();
 			if (String.class.equals(requiredResponseType)) {
 				activity.showDialog(STRING_ENTRY_DIALOG);
 			} else if(Boolean.class.equals(requiredResponseType)) {
 				activity.showDialog(YES_NO_DIALOG);
 			} else {
 	//			hideAllPrompts();
 	//			view.requestFocus();
 			}
 		}
 	}
 
     public void acceptPrompt(ResponseInterface responseInterface) {
         this.responseInterface = responseInterface;
         updateUIToReflectServicePromptRequests();
     }
 
     public void clearPrompt() {
         // TODO clear any actual prompt that's going on...
         this.responseInterface = null;
     }
 }
