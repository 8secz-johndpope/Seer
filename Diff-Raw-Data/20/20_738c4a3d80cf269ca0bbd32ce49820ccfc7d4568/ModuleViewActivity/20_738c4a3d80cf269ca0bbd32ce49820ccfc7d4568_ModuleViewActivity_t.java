 package com.qubling.sidekick;
 
 import com.qubling.sidekick.metacpan.MetaCPANSearch;
 import com.qubling.sidekick.metacpan.result.Module;
 import com.qubling.sidekick.widget.ModuleHelper;
 
 import android.app.Activity;
 import android.content.Intent;
import android.net.Uri;
 import android.os.Bundle;
import android.text.TextUtils;
 import android.view.View;
 import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
 
 public class ModuleViewActivity extends Activity {
 	public static final String EXTRA_MODULE = "com.qubling.sidekick.intent.extra.MODULE";
 	
 	private Module module;
 
 	@Override
 	protected void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		
 		setContentView(R.layout.module_view);
 		
 		Intent intent = getIntent();
 		module = (Module) intent.getParcelableExtra(EXTRA_MODULE);
 		
 		View moduleHeader = findViewById(R.id.module_view_header);
 		ModuleHelper.updateItem(moduleHeader, module);
 		
 		setTitle(module.getModuleName());
 		
 		WebView podView = (WebView) findViewById(R.id.module_pod);
		
		podView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView podView, String url) {
				if (url.startsWith(MetaCPANSearch.METACPAN_API_URL)) {
					return false;
				}
				
				Toast.makeText(ModuleViewActivity.this, "Not Yet Implemented", Toast.LENGTH_SHORT).show();
				return true;
			}
		});
		
 		podView.loadUrl(MetaCPANSearch.METACPAN_API_URL + "/pod/" + module.getModuleName());
 	}
 }
