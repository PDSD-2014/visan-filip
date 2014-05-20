package com.pdsd.pixchange;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	protected MainActivity context = this;
	protected Intent discoverDevicesService = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// start discovering devices service
		if (!isServiceRunning(DiscoverDevicesService.class)) {
			discoverDevicesService = new Intent(this, DiscoverDevicesService.class);
			startService(discoverDevicesService);
		}
		DiscoverDevicesService.parentActivity = MainActivity.this;
		
	}
	
	
	public void processMessage(IMessage message) {
		if (message.getMessageType() == IMessageTypes.BROADCAST_MESSAGE) {
			BroadcastMessage bm = (BroadcastMessage)message;
			TextView tv = (TextView)findViewById(R.id.broadcastReceived);
			Log.d("Message received on ", android.os.Build.MODEL + " " + bm.getInfo());
			tv.setText(bm.getInfo());
		}
		
	}
	
	private boolean isServiceRunning(Class<?> cls) {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
			if (cls.getName().equals(service.service.getClassName()))
				return true;

		return false;
	}
	
	@Override
	public void onDestroy() {
		Log.d("State", "Application destroyed. Closing services");
		stopService(new Intent(this, DiscoverDevicesService.class));
	}
	
	
}
