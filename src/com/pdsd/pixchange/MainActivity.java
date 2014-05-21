package com.pdsd.pixchange;

import com.pdsd.pixchange.DiscoverDevicesService.ButtonListener;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
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
			DiscoverDevicesService.parentActivity = MainActivity.this;
			Log.d("service", "Starting discovery service");
		}
		
		Button b = (Button)findViewById(R.id.broadcastReply);
		b.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Log.d("button", "button clicked!!");
				if (isServiceRunning(DiscoverDevicesService.class)) {
					stopService(new Intent(v.getContext(), DiscoverDevicesService.class));
					Log.d("Service", "Service stopped");
				}
			}
		});
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
		super.onDestroy();
		Log.d("State", "Application destroyed. Closing services");
		stopService(new Intent(this, DiscoverDevicesService.class));
	}
	
	
}
