package com.pdsd.pixchange;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

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
				ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo wifiInfo = manager
						.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

				// check if wifi is turned on
				if (!wifiInfo.isAvailable()) {
					Toast.makeText(v.getContext(),
							"Wifi is currently disabled.", Toast.LENGTH_SHORT)
							.show();
					Toast.makeText(
							v.getContext(),
							"Please turn it on and connect to a network in order start sharing.",
							Toast.LENGTH_LONG).show();

					Log.d(TAG, "Attempted to start share while wifi was off.");
				} else {
					// check if wifi is connected
					if (!wifiInfo.isConnected()) {
						Toast.makeText(
								v.getContext(),
								"Please connect to a network in order start sharing.",
								Toast.LENGTH_LONG).show();

						Log.d(TAG,
								"Attempted to start share while wifi was disconnected.");
					} else {
						// start receiver service
						startService(new Intent(v.getContext(),
								ReceiverService.class));

						running = true;
						shareButton.setText(R.string.stop_share_label);

						// TODO: start transmitter service
						// TODO: start timeout thread
					}
				}
			} else {
				// stop receiver service
				stopService(new Intent(v.getContext(), ReceiverService.class));

				running = false;
				shareButton.setText(R.string.start_share_label);

				// TODO: stop transmitter service
			}
		}
	}

	/**
	 * Check whether a specific service is running or not
	 * 
	 * @param cls
	 *            the service's class
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