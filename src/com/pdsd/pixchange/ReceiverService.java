package com.pdsd.pixchange;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ReceiverService extends Service {
	private static final String TAG = "PixchangeReceiverService";
	private Boolean run = true;
	
	@Override
	public void onCreate() {
		Log.d(TAG, "ReceiverService started");
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG, "ReceiverService stopped");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    return super.onStartCommand(intent,flags,startId);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}