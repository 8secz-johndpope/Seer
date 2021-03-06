 /*
  * Copyright (C) 2010 The Android Open Source Project
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.android.settings.wifi;
 
 import com.android.settings.R;
 
 import android.app.Activity;
 import android.content.Context;
 import android.content.Intent;
 import android.net.NetworkInfo.DetailedState;
 import android.net.wifi.WifiConfiguration;
 import android.net.wifi.WifiManager;
 import android.os.Bundle;
 import android.os.Handler;
 import android.preference.PreferenceCategory;
 import android.text.TextUtils;
 import android.util.Log;
 import android.view.ContextMenu;
 import android.view.ContextMenu.ContextMenuInfo;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.view.ViewGroup;
 import android.view.Window;
 import android.view.WindowManager;
 import android.view.inputmethod.InputMethodManager;
 import android.widget.Button;
 import android.widget.ProgressBar;
 import android.widget.TextView;
 
 import java.util.Collection;
 import java.util.EnumMap;
 
 /**
  * WifiSetings Activity specific for SetupWizard with X-Large screen size.
  */
 public class WifiSettingsForSetupWizardXL extends Activity implements OnClickListener {
     private static final String TAG = "SetupWizard";
     private static final boolean DEBUG = true;
 
     private static final EnumMap<DetailedState, DetailedState> stateMap =
             new EnumMap<DetailedState, DetailedState>(DetailedState.class);
 
     static {
         stateMap.put(DetailedState.IDLE, DetailedState.DISCONNECTED);
         stateMap.put(DetailedState.SCANNING, DetailedState.SCANNING);
         stateMap.put(DetailedState.CONNECTING, DetailedState.CONNECTING);
         stateMap.put(DetailedState.AUTHENTICATING, DetailedState.CONNECTING);
         stateMap.put(DetailedState.OBTAINING_IPADDR, DetailedState.CONNECTING);
         stateMap.put(DetailedState.CONNECTED, DetailedState.CONNECTED);
         stateMap.put(DetailedState.SUSPENDED, DetailedState.SUSPENDED);  // ?
         stateMap.put(DetailedState.DISCONNECTING, DetailedState.DISCONNECTED);
         stateMap.put(DetailedState.DISCONNECTED, DetailedState.DISCONNECTED);
         stateMap.put(DetailedState.FAILED, DetailedState.FAILED);
     }
 
     private WifiManager mWifiManager;
 
     /**
      * Used for resizing a padding above title. Hiden when software keyboard is shown.
      */
     private View mTopPadding;
 
     /**
      * Used for resizing a padding inside Config UI. Hiden when software keyboard is shown.
      */
     private View mWifiConfigPadding;
 
     private TextView mTitleView;
     /**
      * The name of a network currently connecting, or trying to connect.
      * This may be empty ("") at first, and updated when configuration is changed.
      */
     private CharSequence mNetworkName = "";
     private CharSequence mEditingTitle;
 
     private ProgressBar mProgressBar;
     private WifiSettings mWifiSettings;
 
     private Button mAddNetworkButton;
     private Button mRefreshButton;
     private Button mSkipOrNextButton;
     private Button mBackButton;
 
     private static int CONNECT_BUTTON_TAG_ADD_NETWORK = 1;
 
     private Button mConnectButton;
 
     private View mConnectingStatusLayout;
     private TextView mConnectingStatusView;
 
     // true when a user already pressed "Connect" button and waiting for connection.
     // Also true when the device is already connected to a wifi network on launch.
     private boolean mAfterConnectAction;
 
     private WifiConfigUiForSetupWizardXL mWifiConfig;
 
     private InputMethodManager mInputMethodManager;
 
     private final Handler mHandler = new Handler();
 
     private int mPreviousWpsFieldsVisibility = View.GONE;
     private int mPreviousSecurityFieldsVisibility = View.GONE;
     private int mPreviousTypeVisibility = View.GONE;
 
     private DetailedState mPreviousState = DetailedState.DISCONNECTED;
 
     private int mBackgroundId = R.drawable.setups_bg_default;
 
     // At first, we set "Skip" button disabled so that users won't press it soon after the screen
     // migration. The button is enabled after the wifi module returns some result
     // (a list of available network, etc.) One possible problem is that the notification from the
     // wifi module may be delayed and users may be stuck here, without any other way to exit this
     // screen.
     // To let users exit this Activity, we enable the button after waiting for a moment.
     private final int DELAYED_SKIP_ENABLE_TIME = 10000;  // Unit: millis
     private final Runnable mSkipButtonEnabler = new Runnable() {
         @Override
         public void run() {
             if (DEBUG) Log.d(TAG, "Delayed skip enabler starts running.");
             mSkipOrNextButton.setEnabled(true);
         }
     };
 
     @Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         requestWindowFeature(Window.FEATURE_NO_TITLE);
         setContentView(R.layout.wifi_settings_for_setup_wizard_xl);
 
         getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
 
         mWifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
         // There's no button here enabling wifi network, so we need to enable it without
         // users' request.
         mWifiManager.setWifiEnabled(true);
 
         mWifiSettings =
                 (WifiSettings)getFragmentManager().findFragmentById(R.id.wifi_setup_fragment);
         mInputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
         setup();
     }
 
     public void setup() {
         mTitleView = (TextView)findViewById(R.id.wifi_setup_title);
         mProgressBar = (ProgressBar)findViewById(R.id.scanning_progress_bar);
         mProgressBar.setMax(2);
 
         mProgressBar.setIndeterminate(true);
 
         mAddNetworkButton = (Button)findViewById(R.id.wifi_setup_add_network);
         mAddNetworkButton.setOnClickListener(this);
         mRefreshButton = (Button)findViewById(R.id.wifi_setup_refresh_list);
         mRefreshButton.setOnClickListener(this);
         mSkipOrNextButton = (Button)findViewById(R.id.wifi_setup_skip_or_next);
         mSkipOrNextButton.setOnClickListener(this);
         mConnectButton = (Button)findViewById(R.id.wifi_setup_connect);
         mConnectButton.setOnClickListener(this);
         mBackButton = (Button)findViewById(R.id.wifi_setup_cancel);
         mBackButton.setOnClickListener(this);
 
         mTopPadding = findViewById(R.id.top_padding);
         mWifiConfigPadding = findViewById(R.id.wifi_config_padding);
 
         mConnectingStatusLayout = findViewById(R.id.connecting_status_layout);
         mConnectingStatusView = (TextView) findViewById(R.id.connecting_status);
 
         // At first, Wifi module doesn't return SCANNING state (it's too early), so we manually
         // show it.
         showScanningStatus();
         mHandler.postDelayed(mSkipButtonEnabler, DELAYED_SKIP_ENABLE_TIME);
     }
 
     private void restoreFirstButtonVisibilityState() {
         showDefaultTitle();
         mAddNetworkButton.setVisibility(View.VISIBLE);
         mRefreshButton.setVisibility(View.VISIBLE);
         mSkipOrNextButton.setVisibility(View.VISIBLE);
         mConnectButton.setVisibility(View.GONE);
         mBackButton.setVisibility(View.GONE);
         setPaddingVisibility(View.VISIBLE, View.GONE);
     }
 
     @Override
     public void onClick(View view) {
         hideSoftwareKeyboard();
         if (view == mAddNetworkButton) {
             if (DEBUG) Log.d(TAG, "AddNetwork button pressed");
             onAddNetworkButtonPressed();
         } else if (view == mRefreshButton) {
             if (DEBUG) Log.d(TAG, "Refresh button pressed");
             refreshAccessPoints(true);
         } else if (view == mSkipOrNextButton) {
             if (DEBUG) Log.d(TAG, "Skip/Next button pressed");
             if (TextUtils.equals(getString(R.string.wifi_setup_skip), ((Button)view).getText())) {
                 // We don't want to let Wifi enabled when a user press skip without choosing
                 // any access point.
                 mWifiManager.setWifiEnabled(false);
             }
             setResult(Activity.RESULT_OK);
             finish();            
         } else if (view == mConnectButton) {
             if (DEBUG) Log.d(TAG, "Connect button pressed");
             onConnectButtonPressed();
         } else if (view == mBackButton) {
             if (DEBUG) Log.d(TAG, "Back button pressed");
             onBackButtonPressed();
         }
     }
 
     private void hideSoftwareKeyboard() {
         Log.i(TAG, "Hiding software keyboard.");
         final View focusedView = getCurrentFocus();
         if (focusedView != null) {
             mInputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
             focusedView.clearFocus();
         }
     }
 
     // Called from WifiSettings
     /* package */ void updateConnectionState(DetailedState originalState) {
         final DetailedState state = stateMap.get(originalState);
 
         if (originalState == DetailedState.FAILED) {
             // We clean up the current connectivity status and let users select another network
             // if they want.
             refreshAccessPoints(true);
         }
 
         switch (state) {
         case SCANNING: {
             // Let users know the device is working correctly though currently there's
             // no visible network on the list.
             if (mWifiSettings.getAccessPointsCount() == 0) {
                 mProgressBar.setIndeterminate(true);
             } else {
                 // Users already connected to a network, or see available networks.
                 mProgressBar.setIndeterminate(false);
             }
             break;
         }
         case CONNECTING: {
             showConnectingStatus();
             break;
         }
         case CONNECTED: {
             hideSoftwareKeyboard();
            setPaddingVisibility(View.VISIBLE);
 
             // If the device is already connected to a wifi without users' "Connect" request,
             // this can be false here. We want to treat it as "after connect action".
             mAfterConnectAction = true;
 
             trySetBackground(R.drawable.setups_bg_complete);
 
             mProgressBar.setIndeterminate(false);
             mProgressBar.setProgress(2);
 
             showConnectedTitle();
             mConnectingStatusView.setText(R.string.wifi_setup_description_connected);
             mConnectButton.setVisibility(View.GONE);
             mAddNetworkButton.setVisibility(View.GONE);
             mRefreshButton.setVisibility(View.GONE);
             mBackButton.setVisibility(View.VISIBLE);
             mBackButton.setText(R.string.wifi_setup_back);
             mSkipOrNextButton.setVisibility(View.VISIBLE);
             mSkipOrNextButton.setEnabled(true);
             mHandler.removeCallbacks(mSkipButtonEnabler);
             break;
         }
         default:  // DISCONNECTED, FAILED
             showDisconnectedStatus(Summary.get(this, state));
             break;
         }
         mPreviousState = state;
     }
 
     private void showDisconnectedStatus(String stateString) {
         mProgressBar.setIndeterminate(false);
         mProgressBar.setProgress(0);
 
         mAddNetworkButton.setEnabled(true);
         mRefreshButton.setEnabled(true);
     }
 
     private void showConnectingStatus() {
         mBackButton.setVisibility(View.VISIBLE);
         // We save this title and show it when authentication failed.
         mEditingTitle = mTitleView.getText();
         showConnectingTitle();
         mProgressBar.setIndeterminate(false);
         mProgressBar.setProgress(1);

        // We may enter "Connecting" status during editing password again (if the Wifi module
        // tries to (re)connect a network.)
        if (mAfterConnectAction) {
            setPaddingVisibility(View.VISIBLE);
        }
     }
 
     private void showDefaultTitle() {
         mTitleView.setText(getString(R.string.wifi_setup_title));
     }
 
     private void showAddNetworkTitle() {
         mNetworkName = "";
         mTitleView.setText(R.string.wifi_setup_title_add_network);
     }
 
     private void showEditingTitle() {
         if (TextUtils.isEmpty(mNetworkName) && mWifiConfig != null) {
             if (mWifiConfig.getController() != null &&
                 mWifiConfig.getController().getConfig() != null) {
                 mNetworkName = mWifiConfig.getController().getConfig().SSID;
             } else {
                 Log.w(TAG, "Unexpected null found (WifiController or WifiConfig is null). " +
                         "Ignore them.");
             }
         }
         mTitleView.setText(getString(R.string.wifi_setup_title_editing_network, mNetworkName));
     }
 
     private void showConnectingTitle() {
         if (TextUtils.isEmpty(mNetworkName) && mWifiConfig != null) {
             if (mWifiConfig.getController() != null &&
                     mWifiConfig.getController().getConfig() != null) {
                 mNetworkName = mWifiConfig.getController().getConfig().SSID;
             } else {
                 Log.w(TAG, "Unexpected null found (WifiController or WifiConfig is null). " +
                         "Ignore them.");
             }
         }
         mTitleView.setText(getString(R.string.wifi_setup_title_connecting_network, mNetworkName));
     }
 
     private void showConnectedTitle() {
         if (TextUtils.isEmpty(mNetworkName) && mWifiConfig != null) {
             if (mWifiConfig.getController() != null &&
                     mWifiConfig.getController().getConfig() != null) {
                 mNetworkName = mWifiConfig.getController().getConfig().SSID;
             } else {
                 Log.w(TAG, "Unexpected null found (WifiController or WifiConfig is null). " +
                         "Ignore them.");
             }
         }
         mTitleView.setText(getString(R.string.wifi_setup_title_connected_network, mNetworkName));
     }
 
     private void showScanningStatus() {
         mProgressBar.setIndeterminate(true);
         mAddNetworkButton.setEnabled(false);
         mRefreshButton.setEnabled(false);
     }
 
     private void onAddNetworkButtonPressed() {
         mWifiSettings.onAddNetworkPressed();
     }
 
     /**
      * Called when the screen enters wifi configuration UI. UI widget for configuring network
      * (a.k.a. ConfigPreference) should be taken care of by caller side.
      * This method should handle buttons' visibility/enabled.
      * @param selectedAccessPoint AccessPoint object being selected. null when a user pressed
      * "Add network" button, meaning there's no selected access point.
      */
     /* package */ void showConfigUi(AccessPoint selectedAccessPoint, boolean edit) {
         if (selectedAccessPoint != null &&
                 (selectedAccessPoint.security == AccessPoint.SECURITY_WEP ||
                         selectedAccessPoint.security == AccessPoint.SECURITY_PSK)) {
             // We forcibly set edit as true so that users can modify every field if they want,
             // while config UI doesn't allow them to edit some of them when edit is false
             // (e.g. password field is hiden when edit==false).
             edit = true;
         }
 
         trySetBackground(R.drawable.setups_bg_default);
 
         // We don't want to keep scanning Wi-Fi networks during users' configuring one network.
         mWifiSettings.pauseWifiScan();
 
         findViewById(R.id.wifi_setup).setVisibility(View.GONE);
         mConnectingStatusLayout.setVisibility(View.GONE);
         final ViewGroup parent = (ViewGroup)findViewById(R.id.wifi_config_ui);
         parent.setVisibility(View.VISIBLE);
         parent.removeAllViews();
         mWifiConfig = new WifiConfigUiForSetupWizardXL(this, parent, selectedAccessPoint, edit);
 
         // For safety, we forget the tag once. Tag will be updated in this method when needed.
         mConnectButton.setTag(null);
         if (selectedAccessPoint == null) {  // "Add network" flow
             showAddNetworkTitle();
             if (mWifiConfig != null) {
                 mWifiConfig.getView().findViewById(R.id.wifi_general_info).setVisibility(View.GONE);
             }
             mConnectButton.setVisibility(View.VISIBLE);
             mConnectButton.setTag(CONNECT_BUTTON_TAG_ADD_NETWORK);
 
             showEditingButtonState();
         } else if (selectedAccessPoint.security == AccessPoint.SECURITY_NONE) {
             mNetworkName = selectedAccessPoint.getTitle().toString();
 
             // onConnectButtonPressed() will change visibility status.
             mConnectButton.performClick();
         } else {
             mNetworkName = selectedAccessPoint.getTitle().toString();
             showEditingTitle();
             showEditingButtonState();
             if (selectedAccessPoint.security == AccessPoint.SECURITY_EAP) {
                 mConnectButton.setVisibility(View.GONE);
                 mBackButton.setText(R.string.wifi_setup_back);
             } else {
                 mConnectButton.setVisibility(View.VISIBLE);
 
                 // WifiConfigController shows Connect button as "Save" when edit==true and a user
                 // tried to connect the network.
                 // In SetupWizard, we just show the button as "Connect" instead.
                 mConnectButton.setText(R.string.wifi_connect);
                 mBackButton.setText(R.string.wifi_setup_cancel);
             }
         }
     }
 
     private void showEditingButtonState() {
         mSkipOrNextButton.setVisibility(View.GONE);
         mAddNetworkButton.setVisibility(View.GONE);
         mRefreshButton.setVisibility(View.GONE);
         mBackButton.setVisibility(View.VISIBLE);
     }
 
     // May be called when user press "connect" button in WifiDialog
     /* package */ void onConnectButtonPressed() {
         mAfterConnectAction = true;
 
         trySetBackground(R.drawable.setups_bg_wifi);
 
         mWifiSettings.submit(mWifiConfig.getController());
 
         // updateConnectionState() isn't called soon after the user's "connect" action,
         // and the user still sees "not connected" message for a while, which looks strange.
         // We instead manually show "connecting" message before the system gets actual
         // "connecting" message from Wi-Fi module.
         showConnectingStatus();
 
         // Might be better to delay showing this button.
         mBackButton.setVisibility(View.VISIBLE);
         mBackButton.setText(R.string.wifi_setup_back);
 
         // We need to restore visibility status when the device failed to connect the network.
         final View wpsFieldView = findViewById(R.id.wps_fields);
         if (wpsFieldView != null) {
             mPreviousWpsFieldsVisibility = wpsFieldView.getVisibility();
             wpsFieldView.setVisibility(View.GONE);
         }
         final View securityFieldsView = findViewById(R.id.security_fields);
         if (securityFieldsView != null) {
             mPreviousSecurityFieldsVisibility = securityFieldsView.getVisibility();
             securityFieldsView.setVisibility(View.GONE);
         }
         final View typeView = findViewById(R.id.type);
         if (typeView != null) {
             mPreviousTypeVisibility = typeView.getVisibility();
             typeView.setVisibility(View.GONE);
         }
 
         // TODO: investigate whether visibility handling above is needed. Now that we hide
         // them completely when connecting, so we may not need to do so, though we probably
         // need to show software keyboard conditionaly.
         final ViewGroup parent = (ViewGroup)findViewById(R.id.wifi_config_ui);
         parent.setVisibility(View.GONE);
         mConnectingStatusLayout.setVisibility(View.VISIBLE);
         mConnectingStatusView.setText(R.string.wifi_setup_description_connecting);
 
         mHandler.removeCallbacks(mSkipButtonEnabler);
         mSkipOrNextButton.setVisibility(View.VISIBLE);
         mSkipOrNextButton.setEnabled(false);
         mConnectButton.setVisibility(View.GONE);
         mAddNetworkButton.setVisibility(View.GONE);
         mRefreshButton.setVisibility(View.GONE);
     }
 
     private void onBackButtonPressed() {
         trySetBackground(R.drawable.setups_bg_default);
 
         if (mAfterConnectAction) {
             if (DEBUG) Log.d(TAG, "Back button pressed after connect action.");
             mAfterConnectAction = false;
 
             // When a user press "Back" button after pressing "Connect" button, we want to cancel
             // the "Connect" request and refresh the whole wifi status.
             restoreFirstButtonVisibilityState();
 
             mSkipOrNextButton.setEnabled(true);
             changeNextButtonState(false);  // Skip
 
             refreshAccessPoints(true);
         } else { // During user's Wifi configuration.
             mWifiSettings.resumeWifiScan();
 
             restoreFirstButtonVisibilityState();
 
             mAddNetworkButton.setEnabled(true);
             mRefreshButton.setEnabled(true);
             mSkipOrNextButton.setEnabled(true);
         }
 
         findViewById(R.id.wifi_setup).setVisibility(View.VISIBLE);
         mConnectingStatusLayout.setVisibility(View.GONE);
         final ViewGroup parent = (ViewGroup)findViewById(R.id.wifi_config_ui);
         parent.removeAllViews();
         parent.setVisibility(View.GONE);
         mWifiConfig = null;
     }
 
     /**
      * @param connected true when the device is connected to a specific network.
      */
     /* package */ void changeNextButtonState(boolean connected) {
         if (connected) {
             mSkipOrNextButton.setText(R.string.wifi_setup_next);
         } else {
             mSkipOrNextButton.setText(R.string.wifi_setup_skip);
         }
     }
 
     /**
      * Called when the list of AccessPoints are modified and this Activity needs to refresh
      * the list.
      */
     /* package */ void onAccessPointsUpdated(
             PreferenceCategory holder, Collection<AccessPoint> accessPoints) {
        // If we already show some of access points but the bar still shows "scanning" state, it
        // should be stopped.
        if (mProgressBar.isIndeterminate() && accessPoints.size() > 0) {
            mProgressBar.setIndeterminate(false);
            mAddNetworkButton.setEnabled(true);
            mRefreshButton.setEnabled(true);
        }

         for (AccessPoint accessPoint : accessPoints) {
             accessPoint.setLayoutResource(R.layout.custom_preference);
             holder.addPreference(accessPoint);
         }
     }
 
     private void refreshAccessPoints(boolean disconnectNetwork) {
         final Object tag = mConnectButton.getTag();
         if (tag != null && (tag instanceof Integer) &&
                 ((Integer)tag == CONNECT_BUTTON_TAG_ADD_NETWORK)) {
             // In "Add network" flow, we won't get DetaledState available for changing ProgressBar
             // state. Instead we manually show previous status here.
             showDisconnectedStatus(Summary.get(this, mPreviousState));
         } else {
             showScanningStatus();
         }
 
         if (disconnectNetwork) {
             mWifiManager.disconnect();
         }
 
         mWifiSettings.refreshAccessPoints();
     }
 
     /**
      * Called when {@link WifiSettings} received
      * {@link WifiManager#SUPPLICANT_STATE_CHANGED_ACTION}.
      */
     /* package */ void onSupplicantStateChanged(Intent intent) {
         final int errorCode = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
         if (errorCode == WifiManager.ERROR_AUTHENTICATING) {
             Log.i(TAG, "Received authentication error event.");
             onAuthenticationFailure();
         }
     }
 
     /**
      * Called once when Authentication failed.
      */
     private void onAuthenticationFailure() {
         mAfterConnectAction = false;
         mSkipOrNextButton.setVisibility(View.GONE);
         mConnectButton.setVisibility(View.VISIBLE);
         mConnectButton.setEnabled(true);
 
         trySetBackground(R.drawable.setups_bg_default);
 
         if (!TextUtils.isEmpty(mEditingTitle)) {
             mTitleView.setText(mEditingTitle);
         } else {
             Log.w(TAG, "Title during editing/adding a network was empty.");
             showEditingTitle();
         }
 
         final ViewGroup parent = (ViewGroup)findViewById(R.id.wifi_config_ui);
         parent.setVisibility(View.VISIBLE);
         mConnectingStatusLayout.setVisibility(View.GONE);
 
         // Restore View status which was tweaked on connection.
         final View wpsFieldView = findViewById(R.id.wps_fields);
         if (wpsFieldView != null) {
             wpsFieldView.setVisibility(mPreviousWpsFieldsVisibility);
         }
         final View securityFieldsView = findViewById(R.id.security_fields);
         if (securityFieldsView != null) {
             securityFieldsView.setVisibility(mPreviousSecurityFieldsVisibility);
             if (mPreviousSecurityFieldsVisibility == View.VISIBLE && mWifiConfig != null) {
                 final View passwordView = findViewById(R.id.password);
                 if (passwordView != null) {
                     if (passwordView.isFocused()) {
                         setPaddingVisibility(View.GONE);
                     }
                     mWifiConfig.requestFocusAndShowKeyboard(R.id.password);
                 }
             }
         }
         final View typeView = findViewById(R.id.type);
         if (typeView != null) {
             typeView.setVisibility(mPreviousTypeVisibility);
             if (mPreviousTypeVisibility == View.VISIBLE && mWifiConfig != null) {
                 final View ssidView = findViewById(R.id.ssid);
                 if (ssidView != null) {
                     if (ssidView.isFocused()) {
                         setPaddingVisibility(View.GONE);
                     }
                     mWifiConfig.requestFocusAndShowKeyboard(R.id.ssid);
                 }
             }
         }
     }
 
     public void setPaddingVisibility(int visibility) {
         setPaddingVisibility(visibility, visibility);
     }
 
     private void setPaddingVisibility(int topPaddingVisibility, int configVisibility) {
         mTopPadding.setVisibility(topPaddingVisibility);
         mWifiConfigPadding.setVisibility(configVisibility);
     }
 
     /**
      * Called when WifiManager is requested to save a network. This method sholud include
      * WifiManager#saveNetwork() call.
      *
      * Currently this method calls {@link WifiManager#connectNetwork(int)}.
      */
     /* package */ void onSaveNetwork(WifiConfiguration config) {
         mWifiManager.connectNetwork(config);
     }
 
     @Override
     public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
         super.onCreateContextMenu(menu, view, menuInfo);
     }
 
     /**
      * Replace the current background with a new background whose id is resId if needed.
      */
     private void trySetBackground(int resId) {
         if (mBackgroundId != resId) {
             getWindow().setBackgroundDrawable(getResources().getDrawable(resId));
             mBackgroundId = resId;
         }
     }
 }
