package com.pdsd.pixchange;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class UI extends Activity {
	private static final String TAG = "PixchangeUI";

	// UI elements
	private Button shareButton;

	// locals
	private Boolean running = false;

	private class ShareButtonListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			if (!running) {
				running = true;
				shareButton.setText(R.string.stop_share_label);

				// start receiver service
				startService(new Intent(v.getContext(), ReceiverService.class));

				// TODO: start transmitter service
				// TODO: start timeout thread
			} else {
				running = false;
				shareButton.setText(R.string.start_share_label);

				// stop receiver service
				stopService(new Intent(v.getContext(), ReceiverService.class));
				
				// TODO: start transmitter service
			}

		}
	}

	/**
	 * Check whether a specific service is running or not
	 * 
	 * @param cls the service's class
	 * @return true is running, false if not
	 */
	private boolean isServiceRunning(Class<?> cls) {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE))
			if (cls.getName().equals(service.service.getClassName()))
				return true;

		return false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ui);

		// check if ReceiverService is running
		running = isServiceRunning(ReceiverService.class);
		
		// initialize UI
		shareButton = (Button) findViewById(R.id.share_button);
		shareButton.setOnClickListener(new ShareButtonListener());
		if (!running)
			shareButton.setText(R.string.start_share_label);
		else
			shareButton.setText(R.string.stop_share_label);
	}
}