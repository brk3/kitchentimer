package com.leinardi.kitchentimer.ui;

import com.leinardi.kitchentimer.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;

public class InfoActivity extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.info);
		WebView web = (WebView) findViewById(R.id.wv_info);
		web.loadUrl("file:///android_asset/" + getString(R.string.info_filename));

		findViewById(R.id.btn_back).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();				
			}
		});
	}
}
