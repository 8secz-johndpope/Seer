 /*
  * Copyright (C) 2010 Felix Bechstein
  * 
  * This file is part of ub0rlib.
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
 package de.ub0r.android.lib;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.security.KeyFactory;
 import java.security.PublicKey;
 import java.security.Signature;
 import java.security.spec.X509EncodedKeySpec;
 
 import org.apache.http.HttpResponse;
 import org.apache.http.client.ClientProtocolException;
 import org.apache.http.client.methods.HttpGet;
 import org.apache.http.impl.client.DefaultHttpClient;
 
 import android.app.Activity;
 import android.app.ProgressDialog;
 import android.content.Context;
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.net.Uri;
 import android.os.AsyncTask;
 import android.os.Bundle;
 import android.preference.PreferenceManager;
 import android.telephony.TelephonyManager;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.widget.CheckBox;
 import android.widget.EditText;
 import android.widget.Toast;
 
 /**
  * Display send IMEI hash, read signature..
  * 
  * @author flx
  */
 public class DonationHelper extends Activity implements OnClickListener {
 	/** Tag for output. */
 	private static final String TAG = "dh";
 
 	/** Preference's name: hide ads. */
 	static final String PREFS_HIDEADS = "hideads";
 
 	/** Standard buffer size. */
 	public static final int BUFSIZE = 512;
 
 	/** Preference: paypal id. */
 	private static final String PREFS_DONATEMAIL = "donate_mail";
 
 	/** URL for checking hash. */
 	private static final String URL = "http://nossl.ub0r.de/donation/";
 
 	/** Crypto algorithm for signing UID hashs. */
 	private static final String ALGO = "RSA";
 	/** Crypto hash algorithm for signing UID hashs. */
 	private static final String SIGALGO = "SHA1with" + ALGO;
 	/** My public key for verifying UID hashs. */
 	private static final String KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNAD"
 			+ "CBiQKBgQCgnfT4bRMLOv3rV8tpjcEqsNmC1OJaaEYRaTHOCC"
 			+ "F4sCIZ3pEfDcNmrZZQc9Y0im351ekKOzUzlLLoG09bsaOeMd"
 			+ "Y89+o2O0mW9NnBch3l8K/uJ3FRn+8Li75SqoTqFj3yCrd9IT"
 			+ "sOJC7PxcR5TvNpeXsogcyxxo3fMdJdjkafYwIDAQAB";
 
 	/** {@link EditText} for paypal id. */
 	private EditText etPaypalId;
 
 	/** Hashed IMEI. */
 	private static String imeiHash = null;
 
 	/**
 	 * Do all the IO.
 	 * 
 	 * @author flx
 	 */
 	private class InnerTask extends AsyncTask<Void, Void, Void> {
 		/** Mail address used. */
 		private String mail;
 		/** The progress dialog. */
 		private ProgressDialog dialog;
 		/** Did an error occurred? */
 		private boolean error = true;
 		/** Message to the user. */
 		private String msg = null;
 
 		/**
 		 * {@inheritDoc}
 		 */
 		@Override
 		protected void onPreExecute() {
 			this.mail = DonationHelper.this.etPaypalId.getText().toString();
 			this.dialog = ProgressDialog.show(DonationHelper.this, "",
 					DonationHelper.this.getString(R.string.load_hash_) + "...",
 					true, false);
 			final SharedPreferences p = PreferenceManager
 					.getDefaultSharedPreferences(DonationHelper.this);
 			p.edit().putString(PREFS_DONATEMAIL, this.mail).commit();
 			DonationHelper.this.findViewById(R.id.send).setEnabled(false);
 		}
 
 		/**
 		 * {@inheritDoc}
 		 */
 		@Override
 		protected void onPostExecute(final Void result) {
 			this.dialog.dismiss();
 			if (this.msg != null) {
 				Toast
 						.makeText(DonationHelper.this, this.msg,
 								Toast.LENGTH_LONG).show();
 			}
 			if (!this.error) {
 				DonationHelper.this.finish();
 			}
 			DonationHelper.this.findViewById(R.id.send).setEnabled(true);
 		}
 
 		/**
 		 * {@inheritDoc}
 		 */
 		@Override
 		protected Void doInBackground(final Void... params) {
 			final String url = URL + "?mail=" + Uri.encode(this.mail)
 					+ "&hash=" + getImeiHash(DonationHelper.this) + "&lang="
 					+ DonationHelper.this.getString(R.string.lang);
 			final HttpGet request = new HttpGet(url);
 			try {
 				Log.d(TAG, "url: " + url);
 				final HttpResponse response = new DefaultHttpClient()
 						.execute(request);
 				int resp = response.getStatusLine().getStatusCode();
 				if (resp != 200) {
 					this.msg = "Service is down. Retry later. Returncode: "
 							+ resp;
 					return null;
 				}
 				final BufferedReader bufferedReader = new BufferedReader(
 						new InputStreamReader(response.getEntity().getContent()),
 						BUFSIZE);
 				final String line = bufferedReader.readLine();
 				final boolean ret = checkSig(DonationHelper.this, line);
 				final SharedPreferences prefs = PreferenceManager
 						.getDefaultSharedPreferences(DonationHelper.this);
 				prefs.edit().putBoolean(PREFS_HIDEADS, ret).commit();
 
 				int text = R.string.sig_loaded;
 				if (!ret) {
 					text = R.string.sig_failed;
 				}
 				this.msg = DonationHelper.this.getString(text);
 				this.error = !ret;
 				if (this.error) {
 					this.msg += "\n" + line;
 				}
 			} catch (ClientProtocolException e) {
 				Log.e(TAG, "error loading sig", e);
 				this.msg = e.getMessage();
 			} catch (IOException e) {
 				Log.e(TAG, "error loading sig", e);
 				this.msg = e.getMessage();
 			}
 			return null;
 		}
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public final void onCreate(final Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		this.setContentView(R.layout.donation);
 
 		this.findViewById(R.id.donate_paypal).setOnClickListener(this);
 		this.findViewById(R.id.donate_market).setOnClickListener(this);
 		this.findViewById(R.id.send).setOnClickListener(this);
 		this.etPaypalId = (EditText) this.findViewById(R.id.paypalid);
 		this.etPaypalId.setText(PreferenceManager.getDefaultSharedPreferences(
 				this).getString(PREFS_DONATEMAIL, ""));
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	public final void onClick(final View v) {
 		switch (v.getId()) {
 		case R.id.donate_paypal:
 			try {
 				this.startActivity(new Intent(Intent.ACTION_VIEW, Uri
 						.parse(this.getString(R.string.donate_url))));
 			} catch (Exception e) {
 				Log.e(TAG, "error launching paypal", e);
 				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
 			}
 			return;
 		case R.id.donate_market:
 			try {
 				this
 						.startActivity(new Intent(
 								Intent.ACTION_VIEW,
 								Uri
 										.parse("market://search?q=pname:de.ub0r.android.donator")));
 			} catch (Exception e) {
 				Log.e(TAG, "error launching market", e);
 				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
 			}
 			return;
 		case R.id.send:
 			if (this.etPaypalId.getText().toString().length() == 0) {
				Toast.makeText(this, R.string.donator_id_, Toast.LENGTH_LONG)
 						.show();
 				return;
 			}
 			final CheckBox cb = (CheckBox) this.findViewById(R.id.accept);
 			if (!cb.isChecked()) {
 				Toast
 						.makeText(this, R.string.accept_missing,
 								Toast.LENGTH_LONG).show();
 				return;
 			}
 			new InnerTask().execute((Void[]) null);
 			return;
 		default:
 			return;
 		}
 	}
 
 	/**
 	 * Get MD5 hash of the IMEI (device id).
 	 * 
 	 * @param context
 	 *            {@link Context}
 	 * @return MD5 hash of IMEI
 	 */
 	public static String getImeiHash(final Context context) {
 		if (imeiHash == null) {
 			// get imei
 			TelephonyManager mTelephonyMgr = (TelephonyManager) context
 					.getSystemService(TELEPHONY_SERVICE);
 			final String did = mTelephonyMgr.getDeviceId();
 			if (did != null) {
 				imeiHash = Utils.md5(did);
 			}
 		}
 		return imeiHash;
 	}
 
 	/**
 	 * Check for signature updates.
 	 * 
 	 * @param context
 	 *            {@link Context}
 	 * @param s
 	 *            signature
 	 * @return true if ads should be hidden
 	 */
 	public static boolean checkSig(final Context context, final String s) {
 		Log.d(TAG, "checkSig(ctx, " + s + ")");
 		boolean ret = false;
 		try {
 			final byte[] publicKey = Base64Coder.decode(KEY);
 			final KeyFactory keyFactory = KeyFactory.getInstance(ALGO);
 			PublicKey pk = keyFactory.generatePublic(new X509EncodedKeySpec(
 					publicKey));
 			final String h = getImeiHash(context);
 			Log.d(TAG, "hash: " + h);
 			final String cs = s.replaceAll(" |\n|\t", "");
 			Log.d(TAG, "read sig: " + cs);
 			try {
 				byte[] signature = Base64Coder.decode(cs);
 				Signature sig = Signature.getInstance(SIGALGO);
 				sig.initVerify(pk);
 				sig.update(h.getBytes());
 				ret = sig.verify(signature);
 				Log.d(TAG, "ret: " + ret);
 			} catch (IllegalArgumentException e) {
 				Log.w(TAG, "error reading signature", e);
 			}
 		} catch (Exception e) {
 			Log.e(TAG, "error reading signatures", e);
 		}
 		return ret;
 	}
 
 	/**
 	 * Check if ads should be hidden.
 	 * 
 	 * @param context
 	 *            {@link Context}
 	 * @return true if ads should be hidden
 	 */
 	public static boolean hideAds(final Context context) {
 		final SharedPreferences p = PreferenceManager
 				.getDefaultSharedPreferences(context);
 		return p.getBoolean(PREFS_HIDEADS, false);
 	}
 }
