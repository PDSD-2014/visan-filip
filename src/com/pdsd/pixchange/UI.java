package com.pdsd.pixchange;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class UI extends Activity {
	private static final String TAG = "PixchangeUI";

	// UI elements
	private Button shareButton;

	// local variables
	private Boolean sharing = false;

	private class ShareButtonListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			if (!sharing) {
				sharing = true;
				shareButton.setText(R.string.stop_share_label);
				Log.d(TAG, "started sharing");

				// TODO: start sharing thread
				// TODO: start timeout thread
			} else {
				sharing = false;
				shareButton.setText(R.string.start_share_label);
				Log.d(TAG, "stopped sharing");

				// TODO: stop sharing thread
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ui);

		// get saved data
		if(savedInstanceState != null) {
			sharing = savedInstanceState.getBoolean("sharing");
		}
		
		// initialize share button
		shareButton = (Button) findViewById(R.id.share_button);
		shareButton.setOnClickListener(new ShareButtonListener());
		if (!sharing)
			shareButton.setText(R.string.start_share_label);
		else
			shareButton.setText(R.string.stop_share_label);

	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);

		// save current sharing state
		savedInstanceState.putBoolean("sharing", sharing);

		// TODO: save other useful stuff
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		// restore sharing state
		sharing = savedInstanceState.getBoolean("sharing");

		// TODO: restore other useful stuff
	}
}
