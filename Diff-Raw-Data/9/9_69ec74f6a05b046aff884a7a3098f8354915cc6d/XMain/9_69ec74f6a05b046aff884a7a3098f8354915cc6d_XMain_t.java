 package biz.bokhorst.xprivacy;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.InputStreamReader;
 import java.util.List;
 
 import android.media.AudioFormat;
 import android.media.AudioRecord;
 import android.media.MediaRecorder;
 import android.media.MediaRecorder.AudioSource;
 import android.os.Bundle;
 import android.os.Environment;
 import android.provider.MediaStore;
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.content.Context;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.pm.PackageInfo;
 import android.view.LayoutInflater;
 import android.view.Menu;
 import android.view.MenuInflater;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.AdapterView;
 import android.widget.ArrayAdapter;
 import android.widget.ListView;
 import android.widget.TextView;
 import android.widget.Toast;
 
 public class XMain extends Activity {
 
 	private final static int cXposedMinVersion = 34;
 
 	@Override
 	protected void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		// Set layout
 		setContentView(R.layout.xmain);
 
 		// Show version
 		try {
 			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
 			TextView tvVersion = (TextView) findViewById(R.id.tvVersion);
 			tvVersion.setText(String.format(getString(R.string.app_version), pInfo.versionName, pInfo.versionCode));
 		} catch (Throwable ex) {
 			XUtil.bug(null, ex);
 		}
 
 		// Check Xposed version
 		int xVersion = XUtil.getXposedVersion();
 		if (xVersion < cXposedMinVersion) {
 			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
 			alertDialog.setTitle(getString(R.string.app_name));
 			alertDialog.setMessage(String.format(getString(R.string.app_notxposed), cXposedMinVersion));
 			alertDialog.setIcon(R.drawable.ic_launcher);
 			alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
 				@Override
 				public void onClick(DialogInterface dialog, int which) {
 				}
 			});
 			alertDialog.show();
 		}
 
 		// Show Xposed version
 		TextView tvXVersion = (TextView) findViewById(R.id.tvXVersion);
 		tvXVersion.setText(String.format(getString(R.string.app_xversion), xVersion));
 
 		// Fill restriction list view adapter
 		final List<String> listRestriction = XRestriction.getRestrictions();
 		final ListView lvRestriction = (ListView) findViewById(R.id.lvRestriction);
 		RestrictionAdapter restrictionAdapter = new RestrictionAdapter(getBaseContext(),
 				android.R.layout.simple_list_item_1, listRestriction);
 		lvRestriction.setAdapter(restrictionAdapter);
 
 		// Click: batch edit
 		lvRestriction.setOnItemClickListener(new AdapterView.OnItemClickListener() {
 			@Override
 			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
 
 				String restrictionName = listRestriction.get(position);
 				Intent intentBatch = new Intent(view.getContext(), XBatchEdit.class);
 				intentBatch.putExtra(XBatchEdit.cRestrictionName, restrictionName);
 				intentBatch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
 				startActivity(intentBatch);
 			}
 		});
 	}
 
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
 		MenuInflater inflater = getMenuInflater();
 		inflater.inflate(R.menu.xmain, menu);
 		return true;
 	}
 
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 		try {
 			Intent intent;
 			switch (item.getItemId()) {
 			case R.id.menu_capimage:
 				intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
 				startActivityForResult(intent, R.id.menu_capimage);
 				return true;
 			case R.id.menu_capvideo:
 				intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
 				startActivityForResult(intent, R.id.menu_capvideo);
 				return true;
 			case R.id.menu_recordvideo:
 				MediaRecorder vrecorder = new MediaRecorder();
				vrecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
 				vrecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
 				vrecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
 				vrecorder.setOutputFile(Environment.getExternalStorageDirectory().getPath() + "/XPrivacy.3gpp");
 				vrecorder.prepare();
 				vrecorder.start();
 				Thread.sleep(1000);
 				vrecorder.stop();
 				return true;
 			case R.id.menu_recordmic:
 				int bufferSize = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO,
 						AudioFormat.ENCODING_PCM_16BIT);
 				AudioRecord arecorder = new AudioRecord(AudioSource.MIC, 8000, AudioFormat.CHANNEL_IN_MONO,
 						AudioFormat.ENCODING_PCM_16BIT, bufferSize);
 				arecorder.startRecording();
 				Thread.sleep(1000);
 				arecorder.stop();
 				return true;
 			case R.id.menu_readsdcard:
 				File sdcard = Environment.getExternalStorageDirectory();
 				File folder = new File(sdcard.getAbsolutePath() + "/");
 				File file = new File(folder, "XPrivacy.test");
 				FileInputStream fis = new FileInputStream(file);
 				InputStreamReader isr = new InputStreamReader(fis);
 				isr.read();
 				isr.close();
 				fis.close();
 				return true;
 			default:
 				return super.onOptionsItemSelected(item);
 			}
 		} catch (Throwable ex) {
 			XUtil.bug(null, ex);
 			return true;
 		}
 	}
 
 	@Override
 	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
 		Toast toast = null;
 		if (requestCode == R.id.menu_capimage)
 			toast = Toast.makeText(this, "XPrivacy: image captured", Toast.LENGTH_LONG);
 		else if (requestCode == R.id.menu_capvideo)
 			toast = Toast.makeText(this, "XPrivacy: video captured", Toast.LENGTH_LONG);
 		if (toast != null)
 			toast.show();
 	}
 
 	private class RestrictionAdapter extends ArrayAdapter<String> {
 
 		public RestrictionAdapter(Context context, int resource, List<String> objects) {
 			super(context, resource, objects);
 		}
 
 		@Override
 		public View getView(int position, View convertView, ViewGroup parent) {
 			View row = convertView;
 			if (row == null) {
 				LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
 						Context.LAYOUT_INFLATER_SERVICE);
 				row = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
 			}
 			TextView tvRestriction = (TextView) row.findViewById(android.R.id.text1);
 
 			// Get entry
 			String restrictionName = getItem(position);
 
 			// Display localize name
 			tvRestriction.setText(XRestriction.getLocalizedName(row.getContext(), restrictionName));
 
 			return row;
 		}
 	}
 }
