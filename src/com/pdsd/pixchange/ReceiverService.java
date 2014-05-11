package com.pdsd.pixchange;

import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

public class ReceiverService extends Service {
	private static final String TAG = "PixchangeReceiverService";

	// locals
	private PhotoObserver photoObserver = new PhotoObserver();
	private Boolean run = true;

	private class PhotoObserver extends ContentObserver {

		public PhotoObserver() {
			super(null);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			// Media media = readFromMediaStore(getApplicationContext(),
			// MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			Log.d(TAG, "New photo!");
		}
	}

	@Override
	public void onCreate() {
		this.getApplicationContext()
				.getContentResolver()
				.registerContentObserver(
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false,
						photoObserver);

		Log.d(TAG, "ReceiverService started");
	}

	@Override
	public void onDestroy() {
		this.getApplicationContext().getContentResolver()
				.unregisterContentObserver(photoObserver);

		Log.d(TAG, "ReceiverService stopped");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}