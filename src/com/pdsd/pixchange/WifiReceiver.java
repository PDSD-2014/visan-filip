package com.pdsd.pixchange;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.Log;

/**
 * Listens for wifi connect/disconnect events
 * 
 * @author tudor
 * 
 */
public class WifiReceiver extends BroadcastReceiver {
	private static final String TAG = "WifiReceiver";

	// locals
	private static boolean previouslyConnected = true; // the receiver won't be
														// started until there
														// is an active
														// connection
														// so this starts out as
														// true
	private static Context context;
	private static final long timeout = 5 * 60 * 1000;	// 5 minutes
	private static final Handler timeoutHandler = new Handler();
	private static final Runnable timeoutCallback = new Runnable() {

		@Override
		public void run() {
			Log.d(TAG, "Timeout reached!");

			// stop PhotoService
			context.stopService(new Intent(context, PhotoService.class));

			// TODO: stop OtherService
		}
	};

	@Override
	public void onReceive(Context context, Intent intent) {
		NetworkInfo netInfo = intent
				.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

		WifiReceiver.context = context;

		// we're only interested in wifi events
		if (netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
			if (netInfo.isConnected() && !previouslyConnected) {
				Log.d(TAG, "Wifi connected");
				previouslyConnected = true;

				// stop timeout
				stopTimeout();

				// TODO: start share round
			} else if (!netInfo.isConnected() && previouslyConnected) {
				Log.d(TAG, "Wifi disconnected");
				previouslyConnected = false;

				// start timeout
				startTimeout();
			}
		}
	}

	private void startTimeout() {
		timeoutHandler.postDelayed(timeoutCallback, timeout);
		Log.d(TAG, "Timeout started");
	}

	private void stopTimeout() {
		timeoutHandler.removeCallbacks(timeoutCallback);
		Log.d(TAG, "Timeout stopped");
	}
}