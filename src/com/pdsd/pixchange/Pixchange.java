package com.pdsd.pixchange;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

/**
 * UI activity for Pixchange application
 * 
 * @author tudor
 * 
 */
public class Pixchange extends Activity {
	private static final String TAG = "Pixchange";

	// UI elements
	private Button shareButton;
	
	// TODO: remove this
	private Button DEBUG;

	// locals
	private Boolean running = false;

	/**
	 * Listener class for the start/stop share button
	 * 
	 * @author tudor
	 * 
	 */
	private class ShareButtonListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			if (!running) {
				ConnectivityManager conManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
				NetworkInfo netInfo = conManager
						.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

				// check if wifi exists
				if (wifiManager == null || netInfo == null) {
					Toast.makeText(v.getContext(),
							"Error detecting wifi on your device",
							Toast.LENGTH_SHORT);

					Log.e(TAG, "No wifi detected on device");
				} else {
					// check is wifi is enabled
					if (!wifiManager.isWifiEnabled()) {
						Toast.makeText(v.getContext(),
								"Wifi is currently disabled.",
								Toast.LENGTH_SHORT).show();
						Toast.makeText(
								v.getContext(),
								"Please turn on wifi and connect to a network in order start sharing.",
								Toast.LENGTH_LONG).show();

						Log.d(TAG,
								"Attempted to start share while wifi was off.");
					} else {
						// check if wifi is connected
						if (!netInfo.isConnected()) {
							Toast.makeText(
									v.getContext(),
									"Please connect to a network in order start sharing.",
									Toast.LENGTH_LONG).show();

							Log.d(TAG,
									"Attempted to start share while wifi was disconnected.");
						} else {
							// start receiver service
							startService(new Intent(v.getContext(),
									PhotoService.class));
							PhotoService.parentActivity = Pixchange.this;

							// TODO: start transmitter service

							running = true;
							shareButton.setText(R.string.stop_share_label);
						}
					}
				}
			} else {
				// stop receiver service
				stopService(new Intent(v.getContext(), PhotoService.class));

				// TODO: stop transmitter service

				running = false;
				shareButton.setText(R.string.start_share_label);
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
		running = isServiceRunning(PhotoService.class);

		// initialize UI
		shareButton = (Button) findViewById(R.id.share_button);
		shareButton.setOnClickListener(new ShareButtonListener());
		if (!running)
			shareButton.setText(R.string.start_share_label);
		else
			shareButton.setText(R.string.stop_share_label);
		
		// TODO: remove this
		DEBUG = (Button)findViewById(R.id.DEBUG);
		DEBUG.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivity(new Intent(v.getContext(), MainActivity.class));
			}
		});
	}
}