 /* This Source Code Form is subject to the terms of the Mozilla Public
  * License, v. 2.0. If a copy of the MPL was not distributed with this
  * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 
 package org.mozilla.gecko.fxa.activities;
 
 import java.io.IOException;
 
 import org.mozilla.gecko.R;
 import org.mozilla.gecko.background.common.log.Logger;
 import org.mozilla.gecko.background.fxa.FxAccountClient10.RequestDelegate;
 import org.mozilla.gecko.background.fxa.FxAccountClient20.LoginResponse;
 import org.mozilla.gecko.background.fxa.FxAccountClientException.FxAccountClientRemoteException;
 import org.mozilla.gecko.background.fxa.FxAccountUtils;
 import org.mozilla.gecko.fxa.FxAccountConstants;
 import org.mozilla.gecko.fxa.activities.FxAccountSetupTask.ProgressDisplay;
 import org.mozilla.gecko.fxa.authenticator.AndroidFxAccount;
 import org.mozilla.gecko.fxa.login.Engaged;
 import org.mozilla.gecko.fxa.login.State;
 import org.mozilla.gecko.sync.setup.Constants;
 
 import android.accounts.AccountManager;
 import android.content.Intent;
 import android.text.Editable;
 import android.text.InputType;
 import android.text.TextWatcher;
 import android.util.Patterns;
 import android.view.KeyEvent;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.view.View.OnFocusChangeListener;
 import android.widget.Button;
 import android.widget.EditText;
 import android.widget.ProgressBar;
 import android.widget.TextView;
 import android.widget.TextView.OnEditorActionListener;
 
 abstract public class FxAccountAbstractSetupActivity extends FxAccountAbstractActivity implements ProgressDisplay {
   public FxAccountAbstractSetupActivity() {
     super(CANNOT_RESUME_WHEN_ACCOUNTS_EXIST | CANNOT_RESUME_WHEN_LOCKED_OUT);
   }
 
   protected FxAccountAbstractSetupActivity(int resume) {
     super(resume);
   }
 
   private static final String LOG_TAG = FxAccountAbstractSetupActivity.class.getSimpleName();
 
   protected int minimumPasswordLength = 8;
 
   protected EditText emailEdit;
   protected EditText passwordEdit;
   protected Button showPasswordButton;
   protected TextView remoteErrorTextView;
   protected Button button;
   protected ProgressBar progressBar;
 
   protected void createShowPasswordButton() {
     showPasswordButton.setOnClickListener(new OnClickListener() {
       @Override
       public void onClick(View v) {
         boolean isShown = 0 == (passwordEdit.getInputType() & InputType.TYPE_TEXT_VARIATION_PASSWORD);
         // Changing input type loses position in edit text; let's try to maintain it.
         int start = passwordEdit.getSelectionStart();
         int stop = passwordEdit.getSelectionEnd();
         passwordEdit.setInputType(passwordEdit.getInputType() ^ InputType.TYPE_TEXT_VARIATION_PASSWORD);
         passwordEdit.setSelection(start, stop);
         if (isShown) {
           showPasswordButton.setText(R.string.fxaccount_password_show);
         } else {
           showPasswordButton.setText(R.string.fxaccount_password_hide);
         }
       }
     });
   }
 
   protected void hideRemoteError() {
     remoteErrorTextView.setVisibility(View.INVISIBLE);
   }
 
   protected void showRemoteError(Exception e, int defaultResourceId) {
     if (e instanceof IOException) {
       remoteErrorTextView.setText(R.string.fxaccount_remote_error_COULD_NOT_CONNECT);
     } else if (e instanceof FxAccountClientRemoteException) {
       remoteErrorTextView.setText(((FxAccountClientRemoteException) e).getErrorMessageStringResource());
     } else {
       remoteErrorTextView.setText(defaultResourceId);
     }
     Logger.warn(LOG_TAG, "Got exception; showing error message: " + remoteErrorTextView.getText().toString(), e);
     remoteErrorTextView.setVisibility(View.VISIBLE);
   }
 
   protected void addListeners() {
     TextChangedListener textChangedListener = new TextChangedListener();
     EditorActionListener editorActionListener = new EditorActionListener();
     FocusChangeListener focusChangeListener = new FocusChangeListener();
 
     emailEdit.addTextChangedListener(textChangedListener);
     emailEdit.setOnEditorActionListener(editorActionListener);
     emailEdit.setOnFocusChangeListener(focusChangeListener);
     passwordEdit.addTextChangedListener(textChangedListener);
     passwordEdit.setOnEditorActionListener(editorActionListener);
     passwordEdit.setOnFocusChangeListener(focusChangeListener);
   }
 
   protected class FocusChangeListener implements OnFocusChangeListener {
     @Override
     public void onFocusChange(View v, boolean hasFocus) {
       if (hasFocus) {
         return;
       }
       updateButtonState();
     }
   }
 
   protected class EditorActionListener implements OnEditorActionListener {
     @Override
     public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
       updateButtonState();
       return false;
     }
   }
 
   protected class TextChangedListener implements TextWatcher {
     @Override
     public void afterTextChanged(Editable s) {
       updateButtonState();
     }
 
     @Override
     public void beforeTextChanged(CharSequence s, int start, int count, int after) {
       // Do nothing.
     }
 
     @Override
     public void onTextChanged(CharSequence s, int start, int before, int count) {
       // Do nothing.
     }
   }
 
   protected boolean shouldButtonBeEnabled() {
     final String email = emailEdit.getText().toString();
     final String password = passwordEdit.getText().toString();
 
     boolean enabled =
         (email.length() > 0) &&
         Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
         (password.length() >= minimumPasswordLength);
     return enabled;
   }
 
   protected boolean updateButtonState() {
     boolean enabled = shouldButtonBeEnabled();
     if (!enabled) {
       // The user needs to do something before you can interact with the button;
       // presumably that interaction will fix whatever error is shown.
       hideRemoteError();
     }
     if (enabled != button.isEnabled()) {
       Logger.debug(LOG_TAG, (enabled ? "En" : "Dis") + "abling button.");
       button.setEnabled(enabled);
     }
     return enabled;
   }
 
   @Override
   public void showProgress() {
     progressBar.setVisibility(View.VISIBLE);
     button.setVisibility(View.INVISIBLE);
   }
 
   @Override
   public void dismissProgress() {
     progressBar.setVisibility(View.INVISIBLE);
     button.setVisibility(View.VISIBLE);
   }
 
   public Intent makeSuccessIntent(String email, LoginResponse result) {
     Intent successIntent;
     if (result.verified) {
       successIntent = new Intent(this, FxAccountVerifiedAccountActivity.class);
     } else {
       successIntent = new Intent(this, FxAccountConfirmAccountActivity.class);
       successIntent.putExtra("sessionToken", result.sessionToken);
     }
     successIntent.putExtra("email", email);
     // Per http://stackoverflow.com/a/8992365, this triggers a known bug with
     // the soft keyboard not being shown for the started activity. Why, Android, why?
     successIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
     return successIntent;
   }
 
   protected abstract class AddAccountDelegate implements RequestDelegate<LoginResponse> {
     public final String email;
     public final String password;
     public final String serverURI;
 
     public AddAccountDelegate(String email, String password, String serverURI) {
       this.email = email;
       this.password = password;
       this.serverURI = serverURI;
     }
 
     @Override
     public void handleSuccess(LoginResponse result) {
       Logger.info(LOG_TAG, "Got success response; adding Android account.");
 
       // We're on the UI thread, but it's okay to create the account here.
       AndroidFxAccount fxAccount;
       try {
         final String profile = Constants.DEFAULT_PROFILE;
         final String tokenServerURI = FxAccountConstants.DEFAULT_TOKEN_SERVER_URI;
         // TODO: This is wasteful.  We should be able to thread these through so they don't get recomputed.
         byte[] quickStretchedPW = FxAccountUtils.generateQuickStretchedPW(email.getBytes("UTF-8"), password.getBytes("UTF-8"));
         byte[] unwrapkB = FxAccountUtils.generateUnwrapBKey(quickStretchedPW);
         State state = new Engaged(email, result.uid, result.verified, unwrapkB, result.sessionToken, result.keyFetchToken);
        fxAccount = AndroidFxAccount.addAndroidAccount(getApplicationContext(), email, password,
             profile,
             serverURI,
             tokenServerURI,
             state);
         if (fxAccount == null) {
           throw new RuntimeException("Could not add Android account.");
         }
       } catch (Exception e) {
         handleError(e);
         return;
       }
 
       // For great debugging.
       if (FxAccountConstants.LOG_PERSONAL_INFORMATION) {
         fxAccount.dump();
       }
 
       // The GetStarted activity has called us and needs to return a result to the authenticator.
       final Intent intent = new Intent();
       intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, email);
       intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, FxAccountConstants.ACCOUNT_TYPE);
       // intent.putExtra(AccountManager.KEY_AUTHTOKEN, accountType);
       setResult(RESULT_OK, intent);
 
       // Show success activity depending on verification status.
       Intent successIntent = makeSuccessIntent(email, result);
       startActivity(successIntent);
       finish();
     }
   }
 }
