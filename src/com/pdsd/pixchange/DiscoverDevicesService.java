package com.pdsd.pixchange;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;

public class DiscoverDevicesService extends Service {

	protected UDPListener udpListener = null;
	protected static Activity parentActivity = null;
	protected DiscoverDevicesService context = this;
	protected boolean isRunning = false;
	
	@Override
	public void onCreate() {
		
		isRunning = true;
		
		udpListener = new BroadcastListener((WifiManager)this.getSystemService(Context.WIFI_SERVICE));
		udpListener.start();
		
		Button b = (Button)parentActivity.findViewById(R.id.broadcastButton);
		b.setOnClickListener(new ButtonListener());
	}
	
	public void startListeningBroadcast() {
		udpListener.start();
	}
	
	public void stopListeningBroadcast() {
		if (udpListener != null && udpListener.getSocket() != null)
			udpListener.closeSocket();
	}
	
	public void findDevices() {
		new UDPListener((WifiManager) parentActivity.getSystemService(Context.WIFI_SERVICE)).start();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
class ButtonListener implements View.OnClickListener {
		
		@Override
		public void onClick(View arg0) {
			if (arg0 instanceof Button) {
				if (((Button)arg0).getId() == R.id.broadcastButton) {
					stopListeningBroadcast();
					findDevices();
					
				}
			}
			
		}
	}

}
