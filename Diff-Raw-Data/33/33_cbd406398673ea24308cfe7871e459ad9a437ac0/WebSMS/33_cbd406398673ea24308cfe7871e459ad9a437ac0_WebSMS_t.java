 /*
  * Copyright (C) 2010-2011 Felix Bechstein, Lado Kumsiashvili
  * 
  * This file is part of WebSMS.
  * 
  * This program is free software; you can redistribute it and/or modify it under
  * the terms of the GNU General Public License as published by the Free Software
  * Foundation; either version 3 of the License, or (at your option) any later
  * version.
  * 
  * This program is distributed in the hope that it will be useful, but WITHOUT
  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
  * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
  * details.
  * 
  * You should have received a copy of the GNU General Public License along with
  * this program; If not, see <http://www.gnu.org/licenses/>.
  */
 package de.ub0r.android.websms;
 
 import java.io.BufferedInputStream;
 import java.io.BufferedOutputStream;
 import java.io.BufferedReader;
 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.ObjectInputStream;
 import java.io.ObjectOutputStream;
 import java.util.ArrayList;
 import java.util.Calendar;
 import java.util.HashSet;
 import java.util.List;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.app.DatePickerDialog;
 import android.app.Dialog;
 import android.app.DatePickerDialog.OnDateSetListener;
 import android.app.TimePickerDialog.OnTimeSetListener;
 import android.content.BroadcastReceiver;
 import android.content.Context;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.content.SharedPreferences.Editor;
 import android.content.pm.ResolveInfo;
 import android.database.Cursor;
 import android.database.SQLException;
 import android.net.Uri;
 import android.os.Bundle;
 import android.preference.PreferenceManager;
 import android.text.ClipboardManager;
 import android.text.Editable;
 import android.text.TextUtils;
 import android.text.TextWatcher;
 import android.text.format.DateFormat;
 import android.view.Menu;
 import android.view.MenuInflater;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.ViewGroup;
 import android.view.View.OnClickListener;
 import android.view.View.OnLongClickListener;
 import android.widget.AdapterView;
 import android.widget.BaseAdapter;
 import android.widget.Button;
 import android.widget.DatePicker;
 import android.widget.EditText;
 import android.widget.GridView;
 import android.widget.ImageView;
 import android.widget.MultiAutoCompleteTextView;
 import android.widget.TextView;
 import android.widget.TimePicker;
 import android.widget.Toast;
 import android.widget.ToggleButton;
 import android.widget.AdapterView.OnItemClickListener;
 import de.ub0r.android.lib.Base64Coder;
 import de.ub0r.android.lib.Changelog;
 import de.ub0r.android.lib.DonationHelper;
 import de.ub0r.android.lib.Market;
 import de.ub0r.android.lib.apis.ContactsWrapper;
 import de.ub0r.android.lib.apis.TelephonyWrapper;
 import de.ub0r.android.websms.connector.common.Connector;
 import de.ub0r.android.websms.connector.common.ConnectorCommand;
 import de.ub0r.android.websms.connector.common.ConnectorSpec;
 import de.ub0r.android.websms.connector.common.Log;
 import de.ub0r.android.websms.connector.common.Utils;
 import de.ub0r.android.websms.connector.common.ConnectorSpec.SubConnectorSpec;
 
 /**
  * Main Activity.
  * 
  * @author flx
  */
 public class WebSMS extends Activity implements OnClickListener,
 		OnDateSetListener, OnTimeSetListener, OnLongClickListener {
 	/** Tag for output. */
 	public static final String TAG = "main";
 
 	/** Threshold for ad requests filled by the active connector. */
 	private static final double AD_THRESHOLD_CONNECTOR = 0.5;
 
 	/** Current unitid. */
 	private String unitId = null;
 
 	/** Ad's unit id. */
 	private static final String AD_UNITID = "a14c74c342a3f76";
 
 	/** Ad's keywords. */
 	public static final HashSet<String> AD_KEYWORDS = new HashSet<String>();
 	static {
 		AD_KEYWORDS.add("android");
 		AD_KEYWORDS.add("mobile");
 		AD_KEYWORDS.add("handy");
 		AD_KEYWORDS.add("cellphone");
 		AD_KEYWORDS.add("google");
 		AD_KEYWORDS.add("htc");
 		AD_KEYWORDS.add("samsung");
 		AD_KEYWORDS.add("motorola");
 		AD_KEYWORDS.add("market");
 		AD_KEYWORDS.add("app");
 		AD_KEYWORDS.add("message");
 		AD_KEYWORDS.add("txt");
 		AD_KEYWORDS.add("sms");
 		AD_KEYWORDS.add("mms");
 		AD_KEYWORDS.add("game");
 		AD_KEYWORDS.add("websms");
 		AD_KEYWORDS.add("amazon");
 	}
 
 	/** {@link TelephonyWrapper}. */
 	private static final TelephonyWrapper TWRAPPER = TelephonyWrapper
 			.getInstance();
 
 	/** Static reference to running Activity. */
 	private static WebSMS me;
 	/** Preference's name: user's phone number. */
 	static final String PREFS_SENDER = "sender";
 	/** Preference's name: default prefix. */
 	static final String PREFS_DEFPREFIX = "defprefix";
 	/** Preference's name: update balance on start. */
 	private static final String PREFS_AUTOUPDATE = "autoupdate";
 	/** Preference's name: exit after sending. */
 	private static final String PREFS_AUTOEXIT = "autoexit";
 	/** Preference's name: show mobile numbers only. */
 	private static final String PREFS_MOBILES_ONLY = "mobiles_only";
 	/** Preference's name: enable autosend. */
 	private static final String PREFS_AUTOSEND = "enable_autosend";
 	/** Preference's name: use current connector for autosend. */
 	private static final String PREFS_USE_CURRENT_CON = "use_current_connector";
 	/** Preference's name: vibrate on sending. */
 	static final String PREFS_SEND_VIBRATE = "send_vibrate";
 	/** Preference's name: vibrate on failed sending. */
 	static final String PREFS_FAIL_VIBRATE = "fail_vibrate";
 	/** Preference's name: sound on failed sending. */
 	static final String PREFS_FAIL_SOUND = "fail_sound";
 	/** Preference's name: alternative layout. */
 	private static final String PREFS_ALTERNATIVE_LAYOUT = "alternative_layout";
 	/** Preferemce's name: enable change connector button. */
 	private static final String PREFS_HIDE_CHANGE_CONNECTOR_BUTTON = // .
 	"hide_change_connector_button";
 	/** Preferemce's name: hide select recipients button. */
 	private static final String PREFS_HIDE_SELECT_RECIPIENTS_BUTTON = // .
 	"hide_select_recipients_button";
 	/** Preferemce's name: hide clear recipients button. */
 	private static final String PREFS_HIDE_CLEAR_RECIPIENTS_BUTTON = // .
 	"hide_clear_recipients_button";
 	/** Preference's name: hide send menu item. */
 	private static final String PREFS_HIDE_SEND_IN_MENU = "hide_send_in_menu";
 	/** Preference's name: hide emoticons button. */
 	private static final String PREFS_HIDE_EMO_BUTTON = "hide_emo_button";
 	/** Preference's name: hide send button. */
 	private static final String PREFS_HIDE_SEND_BUTTON = "hide_send_button";
 	/** Preference's name: hide cancel button. */
 	private static final String PREFS_HIDE_CANCEL_BUTTON = "hide_cancel_button";
 	/** Preference's name: hide extras button. */
 	private static final String PREFS_HIDE_EXTRAS_BUTTON = "hide_extras_button";
 	/** Preference's name: hide update text. */
 	private static final String PREFS_HIDE_UPDATE = "hide_update";
 	/** Preference's name: hide bg connector. */
 	private static final String PREFS_HIDE_BG_CONNECTOR = "hide_bg_connector";
 	/** Prefernece's name: hide paste button. */
 	private static final String PREFS_HIDE_PASTE = "hide_paste";
 	/** Preference's name: show title bar. */
 	public static final String PREFS_SHOWTITLEBAR = "show_titlebar";
 	/** Cache {@link ConnectorSpec}s. */
 	private static final String PREFS_CONNECTORS = "connectors";
 	/** Preference's name: try to send invalid characters. */
 	private static final String PREFS_TRY_SEND_INVALID = "try_send_invalid";
 	/** Preference's name: drop sent messages. */
 	static final String PREFS_DROP_SENT = "drop_sent";
 	/** Preference's name: backup of last sms. */
 	private static final String PREFS_BACKUPLASTTEXT = "backup_last_sms";
 
 	/** Preference's name: default recipient. */
 	private static final String PREFS_DEFAULT_RECIPIENT = "default_recipient";
 	/** Preference's name: signature. */
 	private static final String PREFS_SIGNATURE = "signature";
 
 	/** Preference's name: to. */
 	private static final String PREFS_TO = "to";
 	/** Preference's name: text. */
 	private static final String PREFS_TEXT = "text";
 	/** Preference's name: selected {@link ConnectorSpec} ID. */
 	static final String PREFS_CONNECTOR_ID = "connector_id";
 	/** Preference's name: selected {@link SubConnectorSpec} ID. */
 	static final String PREFS_SUBCONNECTOR_ID = "subconnector_id";
 
 	/** Preference's name: standard connector. */
 	static final String PREFS_STANDARD_CONNECTOR = "std_connector";
 	/** Preference's name: standard sub connector. */
 	static final String PREFS_STANDARD_SUBCONNECTOR = "std_subconnector";
 
 	/** Sleep before autoexit. */
 	private static final int SLEEP_BEFORE_EXIT = 75;
 
 	/** Buffersize for saving and loading Connectors. */
 	private static final int BUFSIZE = 4096;
 
 	/** Minimum length for showing sms length. */
 	private static final int TEXT_LABLE_MIN_LEN = 20;
 
 	/** Preferences: hide ads. */
 	private static boolean prefsNoAds = false;
 	/** Preferences: selected {@link ConnectorSpec}. */
 	private static ConnectorSpec prefsConnectorSpec = null;
 	/** Preferences: selected {@link SubConnectorSpec}. */
 	private static SubConnectorSpec prefsSubConnectorSpec = null;
 	/** Save prefsConnectorSpec.getPackage() here. */
 	private static String prefsConnectorID = null;
 
 	/** List of available {@link ConnectorSpec}s. */
 	private static final ArrayList<ConnectorSpec> CONNECTORS = // .
 	new ArrayList<ConnectorSpec>();
 
 	/** true if preferences got opened. */
 	static boolean doPreferences = false;
 
 	/** Menu item: restore. */
 	private static final int ITEM_RESTORE = 1;
 
 	/** Dialog: custom sender. */
 	private static final int DIALOG_CUSTOMSENDER = 3;
 	/** Dialog: send later: date. */
 	private static final int DIALOG_SENDLATER_DATE = 4;
 	/** Dialog: send later: time. */
 	private static final int DIALOG_SENDLATER_TIME = 5;
 	/** Dialog: emo. */
 	private static final int DIALOG_EMO = 6;
 
 	/** {@link Activity} result request. */
 	private static final int ARESULT_PICK_PHONE = 1;
 
 	/** Size of the emoticons png. */
 	private static final int EMOTICONS_SIZE = 50;
 	/** Padding for the emoticons png. */
 	private static final int EMOTICONS_PADDING = 5;
 
 	/** Intent's extra for error messages. */
 	static final String EXTRA_ERRORMESSAGE = // .
 	"de.ub0r.android.intent.extra.ERRORMESSAGE";
 	/** Intent's extra for sending message automatically. */
 	private static final String EXTRA_AUTOSEND = "AUTOSEND";
 
 	/** Persistent Message store. */
 	private static String lastMsg = null;
 	/** Persistent Recipient store. */
 	private static String lastTo = null;
 	/** Backup for params: custom sender. */
 	private static String lastCustomSender = null;
 	/** Backup for params: send later. */
 	private static long lastSendLater = -1;
 
 	/** {@link MultiAutoCompleteTextView} holding recipients. */
 	private MultiAutoCompleteTextView etTo;
 	/** {@link EditText} holding text. */
 	private EditText etText;
 	/** {@link TextView} holding balances. */
 	private TextView tvBalances;
 	/** {@link TextView} for pasting text. */
 	private TextView tvPaste;
 
 	/** {@link View} holding extras. */
 	private View vExtras;
 	/** {@link View} holding custom sender. */
 	private View vCustomSender;
 	/** {@link View} holding flashsms. */
 	private View vFlashSMS;
 	/** {@link View} holding send later. */
 	private View vSendLater;
 
 	/** {@link ClipboardManager}. */
 	private ClipboardManager cbmgr;
 
 	/** Text's label. */
 	private TextView etTextLabel;
 
 	/** Show extras. */
 	private boolean showExtras = false;
 
 	/** TextWatcher en-/disable send/cancal buttons. */
 	private TextWatcher twButtons = new TextWatcher() {
 		/**
 		 * {@inheritDoc}
 		 */
 		public void afterTextChanged(final Editable s) {
 			final boolean b0 = prefsConnectorSpec != null
 					&& prefsConnectorSpec
 							.hasStatus(ConnectorSpec.STATUS_ENABLED);
 			final boolean b1 = WebSMS.this.etTo.getText().length() > 0;
 			final boolean b2 = WebSMS.this.etText.getText().length() > 0;
 			WebSMS.this.findViewById(R.id.send_).setEnabled(b0 && b1 && b2);
 			WebSMS.this.findViewById(R.id.cancel).setEnabled(b1 || b2);
 			WebSMS.this.findViewById(R.id.clear).setEnabled(b1);
 		}
 
 		/** Needed dummy. */
 		public void beforeTextChanged(final CharSequence s, final int start,
 				final int count, final int after) {
 		}
 
 		/** Needed dummy. */
 		public void onTextChanged(final CharSequence s, final int start,
 				final int before, final int count) {
 		}
 	};
 
 	/** TextWatcher updating char count on writing. */
 	private TextWatcher twCount = new TextWatcher() {
 		/**
 		 * {@inheritDoc}
 		 */
 		public void afterTextChanged(final Editable s) {
 			int len = s.length();
 			if (len == 0) {
 				WebSMS.this.etTextLabel.setVisibility(View.GONE);
 				if (WebSMS.this.cbmgr.hasText()
 						&& !PreferenceManager.getDefaultSharedPreferences(
 								WebSMS.this)
 								.getBoolean(PREFS_HIDE_PASTE, false)) {
 					WebSMS.this.tvPaste.setVisibility(View.VISIBLE);
 				} else {
 					WebSMS.this.tvPaste.setVisibility(View.GONE);
 				}
 			} else {
 				final String sig = PreferenceManager
 						.getDefaultSharedPreferences(WebSMS.this).getString(
 								PREFS_SIGNATURE, "");
 				len += sig.length();
 				WebSMS.this.tvPaste.setVisibility(View.GONE);
 				if (len > TEXT_LABLE_MIN_LEN) {
 					int[] l = TWRAPPER.calculateLength(s.toString() + sig,
 							false);
 					WebSMS.this.etTextLabel.setText(l[0] + "/" + l[2]);
 					WebSMS.this.etTextLabel.setVisibility(View.VISIBLE);
 				} else {
 					WebSMS.this.etTextLabel.setVisibility(View.GONE);
 				}
 			}
 		}
 
 		/** Needed dummy. */
 		public void beforeTextChanged(final CharSequence s, final int start,
 				final int count, final int after) {
 		}
 
 		/** Needed dummy. */
 		public void onTextChanged(final CharSequence s, final int start,
 				final int before, final int count) {
 		}
 	};
 
 	/** Show extra button. */
 	private static boolean bShowExtras = true;
 
 	/**
 	 * Parse data pushed by {@link Intent}.
 	 * 
 	 * @param intent
 	 *            {@link Intent}
 	 */
 	private void parseIntent(final Intent intent) {
 		final String action = intent.getAction();
 		Log.d(TAG, "launched with action: " + action);
 		if (action == null) {
 			return;
 		}
 		final Uri uri = intent.getData();
 		Log.i(TAG, "launched with uri: " + uri);
 		if (uri != null && uri.toString().length() > 0) {
 			// launched by clicking a sms: link, target number is in URI.
 			final String scheme = uri.getScheme();
 			if (scheme != null) {
 				if (scheme.equals("sms") || scheme.equals("smsto")) {
 					final String s = uri.getSchemeSpecificPart();
 					this.parseSchemeSpecificPart(s);
 					this.displayAds();
 				} else if (scheme.equals("content")) {
 					this.parseThreadId(uri.getLastPathSegment());
 				}
 			}
 		}
 		// check for extras
 		String s = intent.getStringExtra("address");
 		if (!TextUtils.isEmpty(s)) {
 			lastTo = s;
 		}
 		s = intent.getStringExtra(Intent.EXTRA_TEXT);
 		if (s == null) {
 			s = intent.getStringExtra("sms_body");
 		}
 		if (s == null) {
 			final Uri stream = (Uri) intent
 					.getParcelableExtra(Intent.EXTRA_STREAM);
 			if (stream != null) {
 				try {
 					InputStream is = this.getContentResolver().openInputStream(
 							stream);
 					final BufferedReader r = new BufferedReader(
 							new InputStreamReader(is));
 					StringBuffer sb = new StringBuffer();
 					String line;
 					while ((line = r.readLine()) != null) {
 						sb.append(line + "\n");
 					}
 					s = sb.toString().trim();
 				} catch (IOException e) {
 					Log.e(TAG, "IO ERROR", e);
 				}
 
 			}
 		}
 		if (s != null) {
 			((EditText) this.findViewById(R.id.text)).setText(s);
 			lastMsg = s;
 		}
 		s = intent.getStringExtra(EXTRA_ERRORMESSAGE);
 		if (s != null) {
 			Toast.makeText(this, s, Toast.LENGTH_LONG).show();
 		}
 		final SharedPreferences p = PreferenceManager
 				.getDefaultSharedPreferences(this);
 		if (p.getBoolean(PREFS_AUTOSEND, true)) {
 			s = intent.getStringExtra(WebSMS.EXTRA_AUTOSEND);
 			if (s != null && lastMsg != null && lastMsg.length() > 0
 					&& lastTo != null && lastTo.length() > 0) {
 				// all data is here
 				if (p.getBoolean(PREFS_USE_CURRENT_CON, true)) {
 					// push it to current active connector
 					final String subc = WebSMS.getSelectedSubConnectorID();
 					if (prefsConnectorSpec != null && subc != null) {
 						if (this.send(prefsConnectorSpec, subc)
 								&& !this.isFinishing()) {
 							this.finish();
 						}
 					}
 				} else {
 					// show connector chooser
 					final AlertDialog.Builder b = new AlertDialog.Builder(this);
 					b.setTitle(R.string.change_connector_);
 					final String[] items = this.getConnectorMenuItems();
 					b.setItems(items, new DialogInterface.OnClickListener() {
 						@Override
 						public void onClick(final DialogInterface dialog,
 								final int which) {
 							final String sel = items[which];
 							// save old selected connector
 							final ConnectorSpec pr0 = prefsConnectorSpec;
 							final SubConnectorSpec pr1 = prefsSubConnectorSpec;
 							// switch to selected
 							WebSMS.this.saveSelectedConnector(sel);
 							// send message
 							final String subc = WebSMS
 									.getSelectedSubConnectorID();
 							boolean sent = false;
 							if (prefsConnectorSpec != null && subc != null) {
 								sent = WebSMS.this.send(prefsConnectorSpec,
 										subc);
 							}
 							// restore old connector
 							WebSMS.this.saveSelectedConnector(pr0, pr1);
 							// quit
 							if (sent && !WebSMS.this.isFinishing()) {
 								WebSMS.this.finish();
 							}
 						}
 					});
 					b.setNegativeButton(android.R.string.cancel, null);
 					b.show();
 				}
 			}
 		}
 	}
 
 	/**
 	 * parseSchemeSpecificPart from {@link Uri} and initialize WebSMS
 	 * properties.
 	 * 
 	 * @param part
 	 *            scheme specific part
 	 */
 	private void parseSchemeSpecificPart(final String part) {
 		String s = part;
 		if (s == null) {
 			return;
 		}
 		s = s.trim();
 		if (s.endsWith(",")) {
 			s = s.substring(0, s.length() - 1).trim();
 		}
 		if (s.indexOf('<') < 0) {
 			// try to fetch recipient's name from phone book
 			String n = ContactsWrapper.getInstance().getNameForNumber(
 					this.getContentResolver(), s);
 			if (n != null) {
 				s = n + " <" + s + ">, ";
 			}
 		}
 		((EditText) this.findViewById(R.id.to)).setText(s);
 		lastTo = s;
 	}
 
 	/**
 	 * Load data from Conversation.
 	 * 
 	 * @param threadId
 	 *            ThreadId
 	 */
 	private void parseThreadId(final String threadId) {
 		Log.d(TAG, "thradId: " + threadId);
 		final Uri uri = Uri
 				.parse("content://mms-sms/conversations/" + threadId);
 		final String[] proj = new String[] { "thread_id", "address" };
 		Cursor cursor = null;
 		try {
 			try {
 				cursor = this.getContentResolver().query(uri, proj, null, null,
 						null);
 			} catch (SQLException e) {
 				Log.e(TAG, null, e);
 				proj[0] = "_id";
 				proj[1] = "recipient_address";
 				cursor = this.getContentResolver().query(uri, proj, null, null,
 						null);
 			}
 			if (cursor != null && cursor.moveToFirst()) {
 				String a = null;
 				do {
 					a = cursor.getString(1);
 				} while (a == null && cursor.moveToNext());
 				Log.d(TAG, "found address: " + a);
 				this.parseSchemeSpecificPart(a);
 			}
 		} catch (IllegalStateException e) {
 			Log.e(TAG, "error parsing ThreadId: " + threadId, e);
 		}
 		if (cursor != null && !cursor.isClosed()) {
 			cursor.close();
 		}
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@SuppressWarnings("unchecked")
 	@Override
 	public final void onCreate(final Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		this.setTheme(Preferences.getTheme(this));
 		// Restore preferences
 
 		de.ub0r.android.lib.Utils.setLocale(this);
 
 		this.cbmgr = (ClipboardManager) this
 				.getSystemService(CLIPBOARD_SERVICE);
 
 		// save ref to me.
 		me = this;
 		final SharedPreferences p = PreferenceManager
 				.getDefaultSharedPreferences(this);
 		// inflate XML
 		if (p.getBoolean(PREFS_ALTERNATIVE_LAYOUT, false)) {
 			this.setContentView(R.layout.main_alternative);
 		} else {
 			this.setContentView(R.layout.main);
 		}
 
 		this.etTo = (MultiAutoCompleteTextView) this.findViewById(R.id.to);
 		this.etText = (EditText) this.findViewById(R.id.text);
 		this.etTextLabel = (TextView) this.findViewById(R.id.text_);
 		this.tvBalances = (TextView) this.findViewById(R.id.freecount);
 		this.tvPaste = (TextView) this.findViewById(R.id.text_paste);
 
 		this.vExtras = this.findViewById(R.id.extras);
 		this.vCustomSender = this.findViewById(R.id.custom_sender);
 		this.vFlashSMS = this.findViewById(R.id.flashsms);
 		this.vSendLater = this.findViewById(R.id.send_later);
 
 		if (Changelog.isNewVersion(this)) {
 			SharedPreferences.Editor editor = p.edit();
 			editor.remove(PREFS_CONNECTORS); // remove cache
 			editor.commit();
 		}
 		Changelog.showChangelog(this);
 
 		Object o = this.getPackageManager().getLaunchIntentForPackage(
 				"de.ub0r.android.smsdroid");
 		if (o == null) {
 			final Intent intent = Market.getInstallAppIntent(this,
 					"de.ub0r.android.smsdroid", Market.ALT_SMSDROID);
 			Changelog.showNotes(this, "get SMSdroid", null, intent);
 		} else {
 			Changelog.showNotes(this, null, null, null);
 		}
 		o = null;
 
 		// get cached Connectors
 		String s = p.getString(PREFS_CONNECTORS, "");
 		if (s.length() == 0) {
 			this.updateConnectors();
 		} else if (CONNECTORS.size() == 0) {
 			// skip static remaining connectors
 			try {
 				ArrayList<ConnectorSpec> cache;
 				cache = (ArrayList<ConnectorSpec>) (new ObjectInputStream(
 						new BufferedInputStream(new ByteArrayInputStream(
 								Base64Coder.decode(s)), BUFSIZE))).readObject();
 				CONNECTORS.addAll(cache);
 				if (p.getBoolean(PREFS_AUTOUPDATE, true)) {
 					final String defPrefix = p
 							.getString(PREFS_DEFPREFIX, "+49");
 					final String defSender = p.getString(PREFS_SENDER, "");
 					for (ConnectorSpec c : CONNECTORS) {
 						runCommand(me, c, ConnectorCommand.update(defPrefix,
 								defSender));
 					}
 				}
 			} catch (Exception e) {
 				Log.d(TAG, "error loading connectors", e);
 			}
 		}
 		s = null;
 		Log.d(TAG, "loaded connectors: " + CONNECTORS.size());
 
 		this.reloadPrefs();
 
 		lastTo = p.getString(PREFS_TO, "");
 		lastMsg = p.getString(PREFS_TEXT, "");
 
 		// register Listener
 		this.findViewById(R.id.send_).setOnClickListener(this);
 		this.findViewById(R.id.cancel).setOnClickListener(this);
 		this.findViewById(R.id.change_connector).setOnClickListener(this);
 		this.vExtras.setOnClickListener(this);
 		this.vCustomSender.setOnClickListener(this);
 		this.vSendLater.setOnClickListener(this);
 		this.findViewById(R.id.select).setOnClickListener(this);
 		View v = this.findViewById(R.id.clear);
 		v.setOnClickListener(this);
 		v.setOnLongClickListener(this);
 		this.findViewById(R.id.emo).setOnClickListener(this);
 		this.findViewById(R.id.titlebar).setOnClickListener(this);
 		this.tvBalances.setOnClickListener(this);
 		this.tvPaste.setOnClickListener(this);
 		this.etText.addTextChangedListener(this.twCount);
 		this.etText.addTextChangedListener(this.twButtons);
 		this.etTo.addTextChangedListener(this.twButtons);
 		this.etTo.setAdapter(new MobilePhoneAdapter(this));
 		this.etTo.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
 		this.etTo.requestFocus();
 
 		this.parseIntent(this.getIntent());
 
 		// check default prefix
 		if (!p.getString(PREFS_DEFPREFIX, "").startsWith("+")) {
 			WebSMS.this.log(R.string.log_wrong_defprefix);
 		}
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	protected final void onActivityResult(final int requestCode,
 			final int resultCode, final Intent data) {
 		if (requestCode == ARESULT_PICK_PHONE) {
 			if (resultCode == RESULT_OK) {
 				final Uri u = data.getData();
 				if (u == null) {
 					return;
 				}
 				try {
 					final String phone = ContactsWrapper.getInstance()
 							.getNameAndNumber(this.getContentResolver(), u)
 							+ ", ";
 					String t = this.etTo.getText().toString().trim();
 					if (t.length() == 0) {
 						if (TextUtils.isEmpty(lastTo)) {
 							lastTo = PreferenceManager
 									.getDefaultSharedPreferences(this)
 									.getString(PREFS_TO, "");
 						}
 						t = lastTo.trim();
 					}
 					if (t.length() == 0) {
 						t = phone;
 					} else if (t.endsWith(",")) {
 						t += " " + phone;
 					} else {
 						t += ", " + phone;
 					}
 					lastTo = t;
 					this.etTo.setText(t);
 				} catch (IllegalStateException e) {
 					Log.e(TAG, "failed resolving name and number", e);
 				}
 			}
 		}
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	protected final void onNewIntent(final Intent intent) {
 		super.onNewIntent(intent);
 		this.parseIntent(intent);
 	}
 
 	/**
 	 * Update {@link ConnectorSpec}s.
 	 */
 	private void updateConnectors() {
 		// query for connectors
 		final Intent i = new Intent(Connector.ACTION_CONNECTOR_UPDATE);
 		Log.d(TAG, "send broadcast: " + i.getAction());
 		this.sendBroadcast(i);
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	protected final void onResume() {
 		super.onResume();
 		// set accounts' balance to gui
 		this.updateBalance();
 
 		// if coming from prefs..
 		if (doPreferences) {
 			this.reloadPrefs();
 			this.updateConnectors();
 			doPreferences = false;
 			final SharedPreferences p = PreferenceManager
 					.getDefaultSharedPreferences(this);
 			final String defPrefix = p.getString(PREFS_DEFPREFIX, "+49");
 			final String defSender = p.getString(PREFS_SENDER, "");
 			final ConnectorSpec[] css = getConnectors(
 					ConnectorSpec.CAPABILITIES_BOOTSTRAP, // .
 					(short) (ConnectorSpec.STATUS_ENABLED | // .
 					ConnectorSpec.STATUS_READY));
 			for (ConnectorSpec cs : css) {
 				runCommand(this, cs, ConnectorCommand.bootstrap(defPrefix,
 						defSender));
 			}
 		} else {
 			// check is count of connectors changed
 			final List<ResolveInfo> ri = this.getPackageManager()
 					.queryBroadcastReceivers(
 							new Intent(Connector.ACTION_CONNECTOR_UPDATE), 0);
 			final int s1 = ri.size();
 			final int s2 = CONNECTORS.size();
 			if (s1 != s2) {
 				Log.d(TAG, "clear connector cache (" + s1 + "/" + s2 + ")");
 				CONNECTORS.clear();
 				this.updateConnectors();
 			}
 		}
 
 		this.setButtons();
 
 		if (lastTo == null || lastTo.length() == 0) {
 			final SharedPreferences p = PreferenceManager
 					.getDefaultSharedPreferences(this);
 			lastTo = p.getString(PREFS_DEFAULT_RECIPIENT, null);
 		}
 
 		// reload text/recipient from local store
 		if (TextUtils.isEmpty(this.etText.getText())) {
 			if (lastMsg != null) {
 				this.etText.setText(lastMsg);
 			} else {
 				this.etText.setText("");
 			}
 		}
 		if (TextUtils.isEmpty(this.etTo.getText())) {
 			if (lastTo != null) {
 				this.etTo.setText(lastTo);
 			} else {
 				this.etTo.setText("");
 			}
 		}
 
 		if (lastTo != null && lastTo.length() > 0) {
 			this.etText.requestFocus();
 			this.etText.setSelection(this.etText.getText().length());
 		} else {
 			this.etTo.requestFocus();
 		}
 	}
 
 	/**
 	 * Update balance.
 	 */
 	private void updateBalance() {
 		final StringBuilder buf = new StringBuilder();
 		final ConnectorSpec[] css = getConnectors(
 				ConnectorSpec.CAPABILITIES_UPDATE, // .
 				ConnectorSpec.STATUS_ENABLED);
 		String singleb = null;
 		for (ConnectorSpec cs : css) {
 			final String b = cs.getBalance();
 			if (b == null || b.length() == 0) {
 				continue;
 			}
 			if (buf.length() > 0) {
 				buf.append(", ");
 				singleb = null;
 			} else {
 				singleb = b;
 			}
 			buf.append(cs.getName());
 			buf.append(": ");
 			buf.append(b);
 		}
 		if (singleb != null) {
 			buf.replace(0, buf.length(), singleb);
 		}
 
 		buf.insert(0, this.getString(R.string.free_) + " ");
 		if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
 				PREFS_HIDE_UPDATE, false)) {
 			buf.append(" ");
 			buf.append(this.getString(R.string.click_for_update));
 		}
 		this.tvBalances.setText(buf.toString());
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	protected final void onPause() {
 		super.onPause();
 		// store input data to persitent stores
 		lastMsg = this.etText.getText().toString();
 		lastTo = this.etTo.getText().toString();
 
 		// store input data to preferences
 		final Editor editor = PreferenceManager.getDefaultSharedPreferences(
 				this).edit();
 		// common
 		editor.putString(PREFS_TO, lastTo);
 		editor.putString(PREFS_TEXT, lastMsg);
 		// commit changes
 		editor.commit();
 
 		this.savePreferences();
 	}
 
 	@Override
 	protected final void onDestroy() {
 		super.onDestroy();
 		final Editor editor = PreferenceManager.getDefaultSharedPreferences(
 				this).edit();
 		try {
 			final ByteArrayOutputStream out = new ByteArrayOutputStream();
 			ObjectOutputStream objOut = new ObjectOutputStream(
 					new BufferedOutputStream(out, BUFSIZE));
 			objOut.writeObject(CONNECTORS);
 			objOut.close();
 			final String s = String.valueOf(Base64Coder.encode(out
 					.toByteArray()));
 			Log.d(TAG, s);
 			editor.putString(PREFS_CONNECTORS, s);
 		} catch (Exception e) {
 			editor.remove(PREFS_CONNECTORS);
 			Log.e(TAG, "IO", e);
 		}
 		editor.commit();
 	}
 
 	/**
 	 * Read static variables holding preferences.
 	 */
 	private void reloadPrefs() {
 		Log.d(TAG, "reloadPrefs()");
		int ts = Preferences.getTextsize(this);
		if (ts != 0) {
			this.etTo.setTextSize(ts);
			this.etText.setTextSize(ts);
		}
 		final SharedPreferences p = PreferenceManager
 				.getDefaultSharedPreferences(this);
 		if (!p.getBoolean(PREFS_SHOWTITLEBAR, true)) {
 			this.findViewById(R.id.title).setVisibility(View.GONE);
 		} else {
 			this.findViewById(R.id.title).setVisibility(View.VISIBLE);
 		}
 		final boolean bShowChangeConnector = !p.getBoolean(
 				PREFS_HIDE_CHANGE_CONNECTOR_BUTTON, false);
 		final boolean bShowEmoticons = !p.getBoolean(PREFS_HIDE_EMO_BUTTON,
 				false);
 		final boolean bShowSend = !p.getBoolean(PREFS_HIDE_SEND_BUTTON, false);
 		final boolean bShowCancel = !p.getBoolean(PREFS_HIDE_CANCEL_BUTTON,
 				false);
 		bShowExtras = !p.getBoolean(PREFS_HIDE_EXTRAS_BUTTON, false);
 		final boolean bShowClearRecipients = !p.getBoolean(
 				PREFS_HIDE_CLEAR_RECIPIENTS_BUTTON, false);
 		final boolean bShowSelectRecipients = !p.getBoolean(
 				PREFS_HIDE_SELECT_RECIPIENTS_BUTTON, false);
 		View v = this.findViewById(R.id.select);
 		if (bShowSelectRecipients) {
 			v.setVisibility(View.VISIBLE);
 		} else {
 			v.setVisibility(View.GONE);
 		}
 		v = this.findViewById(R.id.clear);
 		if (bShowClearRecipients) {
 			v.setVisibility(View.VISIBLE);
 		} else {
 			v.setVisibility(View.GONE);
 		}
 		v = this.findViewById(R.id.emo);
 		if (bShowEmoticons) {
 			v.setVisibility(View.VISIBLE);
 		} else {
 			v.setVisibility(View.GONE);
 		}
 
 		if (bShowSend || bShowChangeConnector || bShowCancel || bShowExtras) {
 			v = this.findViewById(R.id.send_);
 			if (bShowSend) {
 				v.setVisibility(View.VISIBLE);
 			} else {
 				v.setVisibility(View.GONE);
 			}
 			v = this.findViewById(R.id.change_connector);
 			if (bShowChangeConnector) {
 				v.setVisibility(View.VISIBLE);
 			} else {
 				v.setVisibility(View.GONE);
 			}
 			v = this.findViewById(R.id.cancel);
 			if (bShowCancel) {
 				v.setVisibility(View.VISIBLE);
 			} else {
 				v.setVisibility(View.GONE);
 			}
 			if (!bShowExtras) {
 				this.vExtras.setVisibility(View.GONE);
 			}
 			this.findViewById(R.id.buttonbar).setVisibility(View.VISIBLE);
 		} else {
 			this.findViewById(R.id.buttonbar).setVisibility(View.GONE);
 			this.findViewById(R.id.send_).setVisibility(View.GONE);
 			this.findViewById(R.id.change_connector).setVisibility(View.GONE);
 			this.findViewById(R.id.cancel).setVisibility(View.GONE);
 			this.vExtras.setVisibility(View.GONE);
 		}
 		v = this.findViewById(R.id.text_connector);
 		if (p.getBoolean(PREFS_HIDE_BG_CONNECTOR, false)) {
 			v.setVisibility(View.INVISIBLE);
 		} else {
 			v.setVisibility(View.VISIBLE);
 		}
 
 		String s = p.getString(PREFS_STANDARD_CONNECTOR, "");
 		if (!TextUtils.isEmpty(s)) {
 			p.edit().putString(PREFS_CONNECTOR_ID, s).commit();
 		}
 		prefsConnectorID = p.getString(PREFS_CONNECTOR_ID, "");
 		prefsConnectorSpec = getConnectorByID(prefsConnectorID);
 		if (prefsConnectorSpec != null
 				&& prefsConnectorSpec.hasStatus(ConnectorSpec.STATUS_ENABLED)) {
 			prefsSubConnectorSpec = null;
 			s = p.getString(PREFS_STANDARD_SUBCONNECTOR, "");
 			if (!TextUtils.isEmpty(s)) {
 				p.edit().putString(PREFS_SUBCONNECTOR_ID, s).commit();
 			}
 			prefsSubConnectorSpec = prefsConnectorSpec.getSubConnector(p
 					.getString(PREFS_SUBCONNECTOR_ID, ""));
 			if (prefsSubConnectorSpec == null) {
 				prefsSubConnectorSpec = prefsConnectorSpec.// .
 						getSubConnectors()[0];
 			}
 		} else {
 			ConnectorSpec[] connectors = getConnectors(
 					ConnectorSpec.CAPABILITIES_SEND,
 					ConnectorSpec.STATUS_ENABLED);
 			if (connectors.length == 1) {
 				prefsConnectorSpec = connectors[0];
 				prefsSubConnectorSpec = prefsConnectorSpec // .
 						.getSubConnectors()[0];
 				Toast.makeText(
 						this,
 						this.getString(R.string.connectors_switch) + " "
 								+ prefsConnectorSpec.getName(),
 						Toast.LENGTH_LONG).show();
 			} else {
 				prefsConnectorSpec = null;
 				prefsSubConnectorSpec = null;
 			}
 		}
 
 		MobilePhoneAdapter.setMoileNubersObly(p.getBoolean(PREFS_MOBILES_ONLY,
 				false));
 
 		prefsNoAds = DonationHelper.hideAds(this);
 		this.displayAds();
 		this.setButtons();
 	}
 
 	/**
 	 * Show/hide, enable/disable send buttons.
 	 */
 	private void setButtons() {
 		if (prefsConnectorSpec != null && prefsSubConnectorSpec != null
 				&& prefsConnectorSpec.hasStatus(ConnectorSpec.STATUS_ENABLED)) {
 			final boolean sFlashsms = prefsSubConnectorSpec
 					.hasFeatures(SubConnectorSpec.FEATURE_FLASHSMS);
 			final boolean sCustomsender = prefsSubConnectorSpec
 					.hasFeatures(SubConnectorSpec.FEATURE_CUSTOMSENDER);
 			final boolean sSendLater = prefsSubConnectorSpec
 					.hasFeatures(SubConnectorSpec.FEATURE_SENDLATER);
 			if (bShowExtras && (sFlashsms || sCustomsender || sSendLater)) {
 				this.vExtras.setVisibility(View.VISIBLE);
 				this.findViewById(R.id.buttonbar).setVisibility(View.VISIBLE);
 			} else {
 				this.vExtras.setVisibility(View.GONE);
 			}
 			if (this.showExtras && sFlashsms) {
 				this.vFlashSMS.setVisibility(View.VISIBLE);
 			} else {
 				this.vFlashSMS.setVisibility(View.GONE);
 			}
 			if (this.showExtras && sCustomsender) {
 				this.vCustomSender.setVisibility(View.VISIBLE);
 			} else {
 				this.vCustomSender.setVisibility(View.GONE);
 			}
 			if (this.showExtras && sSendLater) {
 				this.vSendLater.setVisibility(View.VISIBLE);
 			} else {
 				this.vSendLater.setVisibility(View.GONE);
 			}
 
 			String t = this.getString(R.string.app_name) + ": "
 					+ prefsConnectorSpec.getName();
 			if (prefsConnectorSpec.getSubConnectorCount() > 1) {
 				t += " - " + prefsSubConnectorSpec.getName();
 			}
 			((TextView) this.findViewById(R.id.title)).setText(t);
 			String s = prefsConnectorSpec.getName();
 			if (lastSendLater > 0L) {
 				Calendar cal = Calendar.getInstance();
 				cal.setTimeInMillis(lastSendLater);
 				s += "\n@"
 						+ DateFormat.getDateFormat(this).format(cal.getTime());
 				s += " " + DateFormat.getTimeFormat(this).format(cal.getTime());
 			}
 			Log.d(TAG, "set backgroundtext: " + s);
 			((TextView) this.findViewById(R.id.text_connector)).setText(s);
 			((Button) this.findViewById(R.id.send_)).setEnabled(this.etTo
 					.getText().length() > 0
 					&& this.etText.getText().length() > 0);
 		} else {
 			((TextView) this.findViewById(R.id.title))
 					.setText(R.string.app_name);
 			((TextView) this.findViewById(R.id.text_connector)).setText("");
 			((Button) this.findViewById(R.id.send_)).setEnabled(false);
 			if (getConnectors(0, 0).length != 0) {
 				Toast.makeText(this, R.string.log_noselectedconnector,
 						Toast.LENGTH_SHORT).show();
 			}
 		}
 	}
 
 	/**
 	 * Resets persistent store.
 	 * 
 	 * @param backupText
 	 *            backup text to {@link SharedPreferences}
 	 */
 	private void reset(final boolean backupText) {
 		lastMsg = this.etText.getText().toString();
 
 		// save user preferences
 		final SharedPreferences.Editor editor = PreferenceManager
 				.getDefaultSharedPreferences(this).edit();
 		if (backupText) {
 			if (!TextUtils.isEmpty(lastMsg)) {
 				editor.putString(PREFS_BACKUPLASTTEXT, lastMsg);
 			}
 		} else {
 			editor.remove(PREFS_BACKUPLASTTEXT);
 		}
 		editor.putString(PREFS_TO, "");
 		editor.putString(PREFS_TEXT, "");
 		// commit changes
 		editor.commit();
 
 		this.etText.setText("");
 		this.etTo.setText("");
 		lastMsg = null;
 		lastTo = null;
 		lastCustomSender = null;
 		lastSendLater = -1;
 	}
 
 	/** Save prefs. */
 	final void savePreferences() {
 		if (prefsConnectorSpec != null) {
 			PreferenceManager.getDefaultSharedPreferences(this).edit()
 					.putString(PREFS_CONNECTOR_ID,
 							prefsConnectorSpec.getPackage()).commit();
 		}
 	}
 
 	/**
 	 * Run Connector.doUpdate().
 	 */
 	private void updateFreecount() {
 		final SharedPreferences p = PreferenceManager
 				.getDefaultSharedPreferences(this);
 		final String defPrefix = p.getString(PREFS_DEFPREFIX, "+49");
 		final String defSender = p.getString(PREFS_SENDER, "");
 		final ConnectorSpec[] css = getConnectors(
 				ConnectorSpec.CAPABILITIES_UPDATE, // .
 				(short) (ConnectorSpec.STATUS_ENABLED | // .
 				ConnectorSpec.STATUS_READY));
 		for (ConnectorSpec cs : css) {
 			if (cs.isRunning()) {
 				// skip running connectors
 				Log.d(TAG, "skip running connector: " + cs.getName());
 				continue;
 			}
 			runCommand(this, cs, ConnectorCommand.update(defPrefix, defSender));
 		}
 	}
 
 	/**
 	 * Send a command as broadcast.
 	 * 
 	 * @param context
 	 *            WebSMS required for performance issues
 	 * @param connector
 	 *            {@link ConnectorSpec}
 	 * @param command
 	 *            {@link ConnectorCommand}
 	 */
 	static final void runCommand(final WebSMS context,
 			final ConnectorSpec connector, final ConnectorCommand command) {
 		connector.setErrorMessage((String) null);
 		final Intent intent = command.setToIntent(null);
 		short t = command.getType();
 		boolean sendOrdered = false;
 		switch (t) {
 		case ConnectorCommand.TYPE_BOOTSTRAP:
 			sendOrdered = true;
 			intent.setAction(connector.getPackage()
 					+ Connector.ACTION_RUN_BOOTSTRAP);
 			connector.addStatus(ConnectorSpec.STATUS_BOOTSTRAPPING);
 			break;
 		case ConnectorCommand.TYPE_SEND:
 			sendOrdered = true;
 			intent.setAction(connector.getPackage() // .
 					+ Connector.ACTION_RUN_SEND);
 			connector.setToIntent(intent);
 			connector.addStatus(ConnectorSpec.STATUS_SENDING);
 			WebSMSReceiver.saveMessage(connector, me, command,
 					WebSMSReceiver.MESSAGE_TYPE_DRAFT);
 			break;
 		case ConnectorCommand.TYPE_UPDATE:
 			intent.setAction(connector.getPackage()
 					+ Connector.ACTION_RUN_UPDATE);
 			connector.addStatus(ConnectorSpec.STATUS_UPDATING);
 			break;
 		default:
 			break;
 		}
 		if (me != null && (t == ConnectorCommand.TYPE_BOOTSTRAP || // .
 				t == ConnectorCommand.TYPE_UPDATE)) {
 			me.findViewById(R.id.progress).setVisibility(View.VISIBLE);
 		}
 		Log.d(TAG, "send broadcast: " + intent.getAction());
 		if (sendOrdered) {
 			context.sendOrderedBroadcast(intent, null, new BroadcastReceiver() {
 				@Override
 				public void onReceive(final Context context, // .
 						final Intent intent) {
 					if (this.getResultCode() != Activity.RESULT_OK) {
 						ConnectorCommand command = new ConnectorCommand(intent);
 						ConnectorSpec specs = new ConnectorSpec(intent);
 						specs.setErrorMessage(// TODO: localize
 								"Connector did not react on message");
 						WebSMSReceiver.handleSendCommand(specs, context,
 								intent, command);
 					}
 				}
 			}, null, Activity.RESULT_CANCELED, null, null);
 		} else {
 			context.sendBroadcast(intent);
 		}
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	public final void onClick(final View v) {
 		CharSequence s;
 		switch (v.getId()) {
 		case R.id.titlebar:
 		case R.id.freecount:
 			this.updateFreecount();
 			return;
 		case R.id.send_:
 			this.send(prefsConnectorSpec, WebSMS.getSelectedSubConnectorID());
 			this.reloadPrefs();
 			return;
 		case R.id.cancel:
 			this.reset(true);
 			this.reloadPrefs();
 			return;
 		case R.id.select:
 			this.startActivityForResult(ContactsWrapper.getInstance()
 					.getPickPhoneIntent(), ARESULT_PICK_PHONE);
 			return;
 		case R.id.clear:
 			s = this.etTo.getText();
 			final String ss = s.toString();
 			int i = ss.lastIndexOf(",");
 			if (ss.substring(i + 1).trim().length() <= 0) {
 				i = ss.substring(0, i).lastIndexOf(",");
 			}
 
 			if (i <= 0) {
 				lastTo = null;
 				this.etTo.setText("");
 			} else {
 				lastTo = ss.substring(0, i) + ", ";
 				this.etTo.setText(lastTo);
 				this.etTo.setSelection(lastTo.length());
 			}
 			return;
 		case R.id.change_connector:
 			this.changeConnectorMenu();
 			return;
 		case R.id.extras:
 			this.showExtras = !this.showExtras;
 			this.setButtons();
 			return;
 		case R.id.custom_sender:
 			final ToggleButton cs = (ToggleButton) this.vCustomSender;
 			if (cs.isChecked()) {
 				this.showDialog(DIALOG_CUSTOMSENDER);
 			} else {
 				lastCustomSender = null;
 			}
 			return;
 		case R.id.send_later:
 			this.setButtons();
 			final ToggleButton sl = (ToggleButton) this.vSendLater;
 			if (sl.isChecked()) {
 				this.showDialog(DIALOG_SENDLATER_DATE);
 			} else {
 				lastSendLater = -1;
 			}
 			return;
 		case R.id.emo:
 			this.showDialog(DIALOG_EMO);
 			return;
 		case R.id.text_paste:
 			s = this.cbmgr.getText();
 			this.etText.setText(s);
 			this.etText.setSelection(s.length());
 			lastMsg = s.toString();
 			return;
 		default:
 			return;
 		}
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public final boolean onLongClick(final View v) {
 		switch (v.getId()) {
 		case R.id.clear:
 			lastTo = null;
 			this.etTo.setText("");
 			return true;
 		default:
 			return false;
 		}
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public final boolean onCreateOptionsMenu(final Menu menu) {
 		MenuInflater inflater = this.getMenuInflater();
 		inflater.inflate(R.menu.menu, menu);
 		if (prefsNoAds) {
 			menu.removeItem(R.id.item_donate);
 		}
 		final SharedPreferences p = PreferenceManager
 				.getDefaultSharedPreferences(this);
 		final boolean bShowSendButton = !p.getBoolean(PREFS_HIDE_SEND_IN_MENU,
 				false);
 		if (!bShowSendButton) {
 			menu.removeItem(R.id.item_send);
 		}
 		return true;
 	}
 
 	/**
 	 * Save selected connector.
 	 * 
 	 * @param cs
 	 *            {@link ConnectorSpec}
 	 * @param scs
 	 *            {@link SubConnectorSpec}
 	 */
 	private void saveSelectedConnector(final ConnectorSpec cs,
 			final SubConnectorSpec scs) {
 		prefsConnectorSpec = cs;
 		prefsSubConnectorSpec = scs;
 		this.setButtons();
 		if (cs == null || scs == null) {
 			return;
 		}
 		// save user preferences
 		final Editor e = PreferenceManager.getDefaultSharedPreferences(
 				WebSMS.this).edit();
 		e.putString(PREFS_CONNECTOR_ID, prefsConnectorSpec.getPackage());
 		e.putString(PREFS_SUBCONNECTOR_ID, prefsSubConnectorSpec.getID());
 		e.commit();
 	}
 
 	/**
 	 * Save selected connector.
 	 * 
 	 * @param name
 	 *            name of the item
 	 */
 	private void saveSelectedConnector(final String name) {
 		final SubConnectorSpec[] ret = ConnectorSpec
 				.getSubConnectorReturnArray();
 		this.saveSelectedConnector(getConnectorByName(name, ret), ret[0]);
 	}
 
 	/**
 	 * Get all enabled {@link ConnectorSpec}s as name.
 	 * 
 	 * @return array of {@link Connector} names.
 	 */
 	private String[] getConnectorMenuItems() {
 		final ConnectorSpec[] css = getConnectors(
 				ConnectorSpec.CAPABILITIES_SEND, ConnectorSpec.STATUS_ENABLED);
 		final ArrayList<String> items = new ArrayList<String>(css.length * 2);
 		SubConnectorSpec[] scs;
 		String n;
 		for (ConnectorSpec cs : css) {
 			scs = cs.getSubConnectors();
 			if (scs.length <= 1) {
 				items.add(cs.getName());
 			} else {
 				n = cs.getName() + " - ";
 				for (SubConnectorSpec sc : scs) {
 					items.add(n + sc.getName());
 				}
 			}
 		}
 		return items.toArray(new String[items.size()]);
 	}
 
 	/**
 	 * Display "change connector" menu.
 	 */
 	private void changeConnectorMenu() {
 		AlertDialog.Builder builder = new AlertDialog.Builder(this);
 		builder.setIcon(android.R.drawable.ic_menu_share);
 		builder.setTitle(R.string.change_connector_);
 		final String[] items = this.getConnectorMenuItems();
 		final int l = items.length;
 
 		if (l == 0) {
 			Toast.makeText(this, R.string.log_noreadyconnector,
 					Toast.LENGTH_LONG).show();
 		} else if (l == 1) {
 			this.saveSelectedConnector(items[0]);
 		} else if (l == 2) {
 			// Find actual connector, pick the other one from css
 			final SubConnectorSpec[] ret = ConnectorSpec
 					.getSubConnectorReturnArray();
 			final ConnectorSpec cs = getConnectorByName(items[0], ret);
 			final SubConnectorSpec subcs = ret[0];
 			String name;
 			if (prefsConnectorSpec == null || prefsSubConnectorSpec == null
 					|| cs == null || subcs == null) {
 				name = items[0];
 			} else if (cs.equals(prefsConnectorSpec)
 					&& subcs.getID().equals(prefsSubConnectorSpec.getID())) {
 				name = items[1];
 			} else {
 				name = items[0];
 			}
 			this.saveSelectedConnector(name);
 			Toast.makeText(this,
 					this.getString(R.string.connectors_switch) + " " + name,
 					Toast.LENGTH_SHORT).show();
 		} else {
 			builder.setItems(items, new DialogInterface.OnClickListener() {
 				public void onClick(final DialogInterface d, // .
 						final int item) {
 					WebSMS.this.saveSelectedConnector(items[item]);
 				}
 			});
 			builder.create().show();
 			return;
 		}
 	}
 
 	/**
 	 * Save some characters by stripping blanks.
 	 */
 	private void saveChars() {
 		String s = this.etText.getText().toString().trim();
 		if (s.length() == 0 || s.indexOf(" ") < 0) {
 			return;
 		}
 		StringBuilder buf = new StringBuilder();
 		final String[] ss = s.split(" ");
 		s = null;
 		for (String ts : ss) {
 			final int l = ts.length();
 			if (l == 0) {
 				continue;
 			}
 			buf.append(Character.toUpperCase(ts.charAt(0)));
 			if (l == 1) {
 				continue;
 			}
 			buf.append(ts.substring(1));
 		}
 		this.etText.setText(buf.toString());
 	}
 
 	/**
 	 *{@inheritDoc}
 	 */
 	@Override
 	public final boolean onPrepareOptionsMenu(final Menu menu) {
 		final boolean showRestore = !TextUtils.isEmpty(PreferenceManager
 				.getDefaultSharedPreferences(this).getString(
 						PREFS_BACKUPLASTTEXT, null));
 		menu.removeItem(ITEM_RESTORE);
 		if (showRestore) {
 			menu.add(0, ITEM_RESTORE, Menu.NONE, R.string.restore_);
 			menu.findItem(ITEM_RESTORE).setIcon(
 					android.R.drawable.ic_menu_revert);
 		}
 		return true;
 	}
 
 	/**
 	 *{@inheritDoc}
 	 */
 	@Override
 	public final boolean onOptionsItemSelected(final MenuItem item) {
 		Log.d(TAG, "onOptionsItemSelected(" + item.getItemId() + ")");
 		switch (item.getItemId()) {
 		case R.id.item_send:
 			// send by menu item
 			this.send(prefsConnectorSpec, WebSMS.getSelectedSubConnectorID());
 			return true;
 		case R.id.item_draft:
 			this.saveDraft();
 			return true;
 		case R.id.item_savechars:
 			this.saveChars();
 			return true;
 		case R.id.item_settings: // start settings activity
 			this.startActivity(new Intent(this, Preferences.class));
 			return true;
 		case R.id.item_donate:
 			this.startActivity(new Intent(this, DonationHelper.class));
 			return true;
 		case R.id.item_connector:
 			this.changeConnectorMenu();
 			return true;
 		case ITEM_RESTORE:
 			final SharedPreferences p = PreferenceManager
 					.getDefaultSharedPreferences(this);
 			final String s = p.getString(PREFS_BACKUPLASTTEXT, null);
 			if (!TextUtils.isEmpty(s)) {
 				this.etText.setText(s);
 			}
 			p.edit().remove(PREFS_BACKUPLASTTEXT).commit();
 			return true;
 		default:
 			return false;
 		}
 	}
 
 	/**
 	 * Create a Emoticons {@link Dialog}.
 	 * 
 	 * @return Emoticons {@link Dialog}
 	 */
 	private Dialog createEmoticonsDialog() {
 		final Dialog d = new Dialog(this);
 		d.setTitle(R.string.emo_);
 		d.setContentView(R.layout.emo);
 		d.setCancelable(true);
 		final String[] emoticons = this.getResources().getStringArray(
 				R.array.emoticons);
 		final GridView gridview = (GridView) d.findViewById(R.id.gridview);
 		gridview.setAdapter(new BaseAdapter() {
 			// references to our images
 			// keep order and count synced with string-array!
 			private Integer[] mThumbIds = { R.drawable.emo_im_angel,
 					R.drawable.emo_im_cool, R.drawable.emo_im_crying,
 					R.drawable.emo_im_foot_in_mouth, R.drawable.emo_im_happy,
 					R.drawable.emo_im_kissing, R.drawable.emo_im_laughing,
 					R.drawable.emo_im_lips_are_sealed,
 					R.drawable.emo_im_money_mouth, R.drawable.emo_im_sad,
 					R.drawable.emo_im_surprised,
 					R.drawable.emo_im_tongue_sticking_out,
 					R.drawable.emo_im_undecided, R.drawable.emo_im_winking,
 					R.drawable.emo_im_wtf, R.drawable.emo_im_yelling };
 
 			@Override
 			public long getItemId(final int position) {
 				return 0;
 			}
 
 			@Override
 			public Object getItem(final int position) {
 				return null;
 			}
 
 			@Override
 			public int getCount() {
 				return this.mThumbIds.length;
 			}
 
 			@Override
 			public View getView(final int position, final View convertView,
 					final ViewGroup parent) {
 				ImageView imageView;
 				if (convertView == null) { // if it's not recycled,
 					// initialize some attributes
 					imageView = new ImageView(WebSMS.this);
 					imageView.setLayoutParams(new GridView.LayoutParams(
 							EMOTICONS_SIZE, EMOTICONS_SIZE));
 					imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
 					imageView.setPadding(EMOTICONS_PADDING, EMOTICONS_PADDING,
 							EMOTICONS_PADDING, EMOTICONS_PADDING);
 				} else {
 					imageView = (ImageView) convertView;
 				}
 
 				imageView.setImageResource(this.mThumbIds[position]);
 				return imageView;
 			}
 		});
 		gridview.setOnItemClickListener(new OnItemClickListener() {
 
 			@Override
 			public void onItemClick(final AdapterView<?> adapter, final View v,
 					final int id, final long arg3) {
 				EditText et = WebSMS.this.etText;
 				final String e = emoticons[id];
 				int i = et.getSelectionStart();
 				int j = et.getSelectionEnd();
 				if (i > j) {
 					int x = i;
 					i = j;
 					j = x;
 				}
 				String t = et.getText().toString();
 				StringBuilder buf = new StringBuilder();
 				buf.append(t.substring(0, i));
 				buf.append(e);
 				buf.append(t.substring(j));
 				et.setText(buf.toString());
 				et.setSelection(i + e.length());
 				d.dismiss();
 				et.requestFocus();
 			}
 		});
 		return d;
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	protected final Dialog onCreateDialog(final int id) {
 		AlertDialog.Builder builder;
 		switch (id) {
 		case DIALOG_CUSTOMSENDER:
 			builder = new AlertDialog.Builder(this);
 			builder.setTitle(R.string.custom_sender);
 			builder.setCancelable(true);
 			final EditText et = new EditText(this);
 			builder.setView(et);
 			builder.setPositiveButton(android.R.string.ok,
 					new DialogInterface.OnClickListener() {
 						public void onClick(final DialogInterface dialog,
 								final int id) {
 							WebSMS.lastCustomSender = et.getText().toString();
 						}
 					});
 			builder.setNegativeButton(android.R.string.cancel, null);
 			return builder.create();
 		case DIALOG_SENDLATER_DATE:
 			Calendar c = Calendar.getInstance();
 			return new DatePickerDialog(this, this, c.get(Calendar.YEAR), c
 					.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
 		case DIALOG_SENDLATER_TIME:
 			c = Calendar.getInstance();
 			return new MyTimePickerDialog(this, this, c
 					.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true);
 		case DIALOG_EMO:
 			return this.createEmoticonsDialog();
 		default:
 			return null;
 		}
 	}
 
 	/**
 	 * Log text.
 	 * 
 	 * @param text
 	 *            text as resID
 	 */
 	public final void log(final int text) {
 		this.log(this.getString(text));
 	}
 
 	/**
 	 * Log text.
 	 * 
 	 * @param text
 	 *            text
 	 */
 	public final void log(final String text) {
 		try {
 			Toast.makeText(this.getApplicationContext(), text,
 					Toast.LENGTH_LONG).show();
 		} catch (RuntimeException e) {
 			Log.e(TAG, null, e);
 		}
 	}
 
 	/**
 	 * Show AdView on top or on bottom.
 	 */
 	private void displayAds() {
 		if (prefsNoAds) {
 			// do not display any ads for donators
 			return;
 		}
 
 		if (!prefsNoAds) {
 			if (this.unitId == null) {
 				this.unitId = AD_UNITID;
 				if (Math.random() > AD_THRESHOLD_CONNECTOR) {
 					// half of the requests are filled by the active connector
 					if (prefsConnectorSpec != null) {
 						final String s = prefsConnectorSpec.getAdUnitId();
 						Log.d(TAG, "load connectors ads: " + s);
 						if (s != null) {
 							this.unitId = s;
 						}
 					}
 				}
 			}
 			Ads.loadAd(this, R.id.ad, this.unitId, AD_KEYWORDS);
 		}
 	}
 
 	/**
 	 * Safe draft.
 	 */
 	private void saveDraft() {
 		// fetch text/recipient
 		final String to = this.etTo.getText().toString();
 		final String text = this.etText.getText().toString();
 		if (to.length() == 0 || text.length() == 0) {
 			return;
 		}
 
 		this.displayAds();
 
 		final String[] tos = Utils.parseRecipients(to);
 		final ConnectorCommand command = ConnectorCommand.send(null, null,
 				null, tos, text, false);
 		WebSMSReceiver.saveMessage(null, this, command,
 				WebSMSReceiver.MESSAGE_TYPE_DRAFT);
 		this.reset(false);
 	}
 
 	/**
 	 * Send text.
 	 * 
 	 * @param connector
 	 *            which connector should be used.
 	 * @param subconnector
 	 *            selected {@link SubConnectorSpec} ID
 	 * @return true if message was sent
 	 */
 	private boolean send(final ConnectorSpec connector, // .
 			final String subconnector) {
 		if (connector == null || TextUtils.isEmpty(subconnector)) {
 			Log.e(TAG, "connector: " + connector);
 			Log.e(TAG, "subconnector: " + subconnector);
 			Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show();
 			return false;
 		}
 		// fetch text/recipient
 		final String to = this.etTo.getText().toString();
 		String text = this.etText.getText().toString();
 		if (to.length() == 0 || text.length() == 0) {
 			Log.e(TAG, "to: " + to);
 			Log.e(TAG, "text: " + text);
 			Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show();
 			return false;
 		}
 		text = text.trim();
 		this.etText.setText(text);
 		final SharedPreferences p = PreferenceManager
 				.getDefaultSharedPreferences(this);
 		final String signature = p.getString(PREFS_SIGNATURE, null);
 		if (signature != null && signature.length() > 0
 				&& !text.endsWith(signature)) {
 			text = text + signature;
 			this.etText.setText(text);
 		}
 
 		if (!p.getBoolean(PREFS_TRY_SEND_INVALID, false)
 				&& connector.hasCapabilities(// .
 						ConnectorSpec.CAPABILITIES_CHARACTER_CHECK)) {
 			final String valid = connector.getValidCharacters();
 			if (valid == null) {
 				Log.i(TAG, "valid: " + valid);
 				Toast.makeText(this, R.string.log_error_char_nonvalid,
 						Toast.LENGTH_LONG).show();
 				return false;
 			}
 			final Pattern checkPattern = Pattern.compile("[^"
 					+ Pattern.quote(valid) + "]+");
 			Log.d(TAG, "pattern: " + checkPattern.pattern());
 			final Matcher m = checkPattern.matcher(text);
 			if (m.find()) {
 				final String illigal = m.group();
 				Log.i(TAG, "invalid character: " + illigal);
 				Toast.makeText(
 						this,
 						this.getString(R.string.log_error_char_notsendable)
 								+ ": " + illigal, Toast.LENGTH_LONG).show();
 				return false;
 			}
 		}
 
 		this.displayAds();
 
 		ToggleButton v = (ToggleButton) this.findViewById(R.id.flashsms);
 		final boolean flashSMS = (v.getVisibility() == View.VISIBLE)
 				&& v.isEnabled() && v.isChecked();
 		final String defPrefix = p.getString(PREFS_DEFPREFIX, "+49");
 		final String defSender = p.getString(PREFS_SENDER, "");
 
 		final String[] tos = Utils.parseRecipients(to);
 		final ConnectorCommand command = ConnectorCommand.send(subconnector,
 				defPrefix, defSender, tos, text, flashSMS);
 		command.setCustomSender(lastCustomSender);
 		command.setSendLater(lastSendLater);
 
 		boolean sent = false;
 		try {
 			if (connector.getSubConnector(subconnector).hasFeatures(
 					SubConnectorSpec.FEATURE_MULTIRECIPIENTS)
 					|| tos.length == 1) {
 				Log.d(TAG, "text: " + text);
 				Log.d(TAG, "to: ", tos);
 				runCommand(this, connector, command);
 			} else {
 				ConnectorCommand cc;
 				for (String t : tos) {
 					if (t.trim().length() < 1) {
 						continue;
 					}
 					cc = (ConnectorCommand) command.clone();
 					cc.setRecipients(t);
 					Log.d(TAG, "text: " + text);
 					Log.d(TAG, "to: ", tos);
 					runCommand(this, connector, cc);
 				}
 			}
 			sent = true;
 		} catch (Exception e) {
 			Log.e(TAG, "error running command", e);
 			Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show();
 		}
 		if (sent) {
 			this.reset(false);
 			if (p.getBoolean(PREFS_AUTOEXIT, false)) {
 				try {
 					Thread.sleep(SLEEP_BEFORE_EXIT);
 				} catch (InterruptedException e) {
 					Log.e(TAG, null, e);
 				}
 				this.finish();
 			}
 			return true;
 		}
 		return false;
 	}
 
 	/**
 	 * @return ID of selected {@link SubConnectorSpec}
 	 */
 	private static String getSelectedSubConnectorID() {
 		if (prefsSubConnectorSpec == null) {
 			return null;
 		}
 		return prefsSubConnectorSpec.getID();
 	}
 
 	/**
 	 * A Date was set.
 	 * 
 	 * @param view
 	 *            DatePicker View
 	 * @param year
 	 *            year set
 	 * @param monthOfYear
 	 *            month set
 	 * @param dayOfMonth
 	 *            day set
 	 */
 	public final void onDateSet(final DatePicker view, final int year,
 			final int monthOfYear, final int dayOfMonth) {
 		final Calendar c = Calendar.getInstance();
 		if (lastSendLater > 0) {
 			c.setTimeInMillis(lastSendLater);
 		}
 		c.set(Calendar.YEAR, year);
 		c.set(Calendar.MONTH, monthOfYear);
 		c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
 		lastSendLater = c.getTimeInMillis();
 
 		MyTimePickerDialog.setOnlyQuaters(prefsSubConnectorSpec
 				.hasFeatures(SubConnectorSpec.FEATURE_SENDLATER_QUARTERS));
 		this.showDialog(DIALOG_SENDLATER_TIME);
 		this.setButtons();
 	}
 
 	/**
 	 * A Time was set.
 	 * 
 	 * @param view
 	 *            TimePicker View
 	 * @param hour
 	 *            hour set
 	 * @param minutes
 	 *            minutes set
 	 */
 	public final void onTimeSet(final TimePicker view, final int hour,
 			final int minutes) {
 		if (prefsSubConnectorSpec
 				.hasFeatures(SubConnectorSpec.FEATURE_SENDLATER_QUARTERS)
 				&& minutes % 15 != 0) {
 			Toast.makeText(this, R.string.error_sendlater_quater,
 					Toast.LENGTH_LONG).show();
 			return;
 		}
 
 		final Calendar c = Calendar.getInstance();
 		if (lastSendLater > 0) {
 			c.setTimeInMillis(lastSendLater);
 		}
 		c.set(Calendar.HOUR_OF_DAY, hour);
 		c.set(Calendar.MINUTE, minutes);
 		lastSendLater = c.getTimeInMillis();
 		this.setButtons();
 	}
 
 	/**
 	 * Add or update a {@link ConnectorSpec}.
 	 * 
 	 * @param connector
 	 *            connector
 	 */
 	static final void addConnector(final ConnectorSpec connector) {
 		synchronized (CONNECTORS) {
 			if (connector == null || connector.getPackage() == null
 					|| connector.getName() == null) {
 				return;
 			}
 			ConnectorSpec c = getConnectorByID(connector.getPackage());
 			if (c != null) {
 				c.setErrorMessage((String) null); // fix sticky error status
 				c.update(connector);
 				final String em = c.getErrorMessage();
 				if (em != null && me != null) {
 					Toast.makeText(me, em, Toast.LENGTH_LONG).show();
 				}
 			} else {
 				final String name = connector.getName();
 				if (connector.getSubConnectorCount() == 0 || name == null
 						|| connector.getPackage() == null) {
 					Log.w(TAG, "skipped adding defect connector: " + name);
 					return;
 				}
 				Log.d(TAG, "add connector with id: " + connector.getPackage());
 				Log.d(TAG, "add connector with name: " + name);
 				boolean added = false;
 				final int l = CONNECTORS.size();
 				ConnectorSpec cs;
 				try {
 					for (int i = 0; i < l; i++) {
 						cs = CONNECTORS.get(i);
 						if (name.compareToIgnoreCase(cs.getName()) < 0) {
 							CONNECTORS.add(i, connector);
 							added = true;
 							break;
 						}
 					}
 				} catch (NullPointerException e) {
 					Log.e(TAG, "error while sorting", e);
 				}
 				if (!added) {
 					CONNECTORS.add(connector);
 				}
 				c = connector;
 				if (me != null) {
 					final SharedPreferences p = PreferenceManager
 							.getDefaultSharedPreferences(me);
 
 					// update connectors balance if needed
 					if (c.getBalance() == null && c.isReady() && !c.isRunning()
 							&& c.hasCapabilities(// .
 									ConnectorSpec.CAPABILITIES_UPDATE)
 							&& p.getBoolean(PREFS_AUTOUPDATE, true)) {
 						final String defPrefix = p.getString(PREFS_DEFPREFIX,
 								"+49");
 						final String defSender = p.getString(PREFS_SENDER, "");
 						runCommand(me, c, ConnectorCommand.update(defPrefix,
 								defSender));
 					}
 				}
 			}
 			if (me != null) {
 				final SharedPreferences p = PreferenceManager
 						.getDefaultSharedPreferences(me);
 
 				if (prefsConnectorSpec == null
 						&& prefsConnectorID.equals(connector.getPackage())) {
 					prefsConnectorSpec = connector;
 
 					prefsSubConnectorSpec = connector.getSubConnector(p
 							.getString(PREFS_SUBCONNECTOR_ID, ""));
 					me.setButtons();
 				}
 
 				final String b = c.getBalance();
 				final String ob = c.getOldBalance();
 				if (b != null && (ob == null || !b.equals(ob))) {
 					me.updateBalance();
 				}
 
 				boolean runningConnectors = getConnectors(
 						ConnectorSpec.CAPABILITIES_UPDATE,
 						ConnectorSpec.STATUS_ENABLED
 								| ConnectorSpec.STATUS_UPDATING).length != 0;
 				if (!runningConnectors) {
 					runningConnectors = getConnectors(
 							ConnectorSpec.CAPABILITIES_BOOTSTRAP,
 							ConnectorSpec.STATUS_ENABLED
 									| ConnectorSpec.STATUS_BOOTSTRAPPING).// .
 					length != 0;
 				}
 				if (runningConnectors) {
 					me.findViewById(R.id.progress).setVisibility(View.VISIBLE);
 				} else {
 					me.findViewById(R.id.progress).setVisibility(View.GONE);
 				}
 				if (prefsConnectorSpec != null && // .
 						prefsConnectorSpec.equals(c)) {
 					me.setButtons();
 				}
 			}
 		}
 	}
 
 	/**
 	 * Get {@link ConnectorSpec} by ID.
 	 * 
 	 * @param id
 	 *            ID
 	 * @return {@link ConnectorSpec}
 	 */
 	private static ConnectorSpec getConnectorByID(final String id) {
 		synchronized (CONNECTORS) {
 			if (id == null) {
 				return null;
 			}
 			final int l = CONNECTORS.size();
 			ConnectorSpec c;
 			for (int i = 0; i < l; i++) {
 				c = CONNECTORS.get(i);
 				if (id.equals(c.getPackage())) {
 					return c;
 				}
 			}
 		}
 		return null;
 	}
 
 	/**
 	 * Get {@link ConnectorSpec} by name.
 	 * 
 	 * @param name
 	 *            name
 	 * @param returnSelectedSubConnector
 	 *            if not null, array[0] will be set to selected
 	 *            {@link SubConnectorSpec}
 	 * @return {@link ConnectorSpec}
 	 */
 	private static ConnectorSpec getConnectorByName(final String name,
 			final SubConnectorSpec[] returnSelectedSubConnector) {
 		synchronized (CONNECTORS) {
 			if (name == null) {
 				return null;
 			}
 			final int l = CONNECTORS.size();
 			ConnectorSpec c;
 			String n;
 			SubConnectorSpec[] scs;
 			for (int i = 0; i < l; i++) {
 				c = CONNECTORS.get(i);
 				n = c.getName();
 				if (name.startsWith(n)) {
 					if (name.length() == n.length()) {
 						if (returnSelectedSubConnector != null) {
 							returnSelectedSubConnector[0] = c
 									.getSubConnectors()[0];
 						}
 						return c;
 					} else if (returnSelectedSubConnector != null) {
 						scs = c.getSubConnectors();
 						if (scs == null || scs.length == 0) {
 							continue;
 						}
 						for (SubConnectorSpec sc : scs) {
 							if (name.endsWith(sc.getName())) {
 								returnSelectedSubConnector[0] = sc;
 								return c;
 							}
 						}
 					}
 				}
 			}
 		}
 		return null;
 	}
 
 	/**
 	 * Get {@link ConnectorSpec}s by capabilities and/or status.
 	 * 
 	 * @param capabilities
 	 *            capabilities needed
 	 * @param status
 	 *            status required {@link SubConnectorSpec}
 	 * @return {@link ConnectorSpec}s
 	 */
 	static final ConnectorSpec[] getConnectors(final int capabilities,
 			final int status) {
 		synchronized (CONNECTORS) {
 			final ArrayList<ConnectorSpec> ret = new ArrayList<ConnectorSpec>(
 					CONNECTORS.size());
 			final int l = CONNECTORS.size();
 			ConnectorSpec c;
 			for (int i = 0; i < l; i++) {
 				c = CONNECTORS.get(i);
 				if (c.hasCapabilities((short) capabilities)
 						&& c.hasStatus((short) status)) {
 					ret.add(c);
 				}
 			}
 			return ret.toArray(new ConnectorSpec[0]);
 		}
 	}
 }
