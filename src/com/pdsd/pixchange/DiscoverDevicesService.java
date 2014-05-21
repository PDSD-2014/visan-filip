package com.pdsd.pixchange;

import java.io.IOException;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class DiscoverDevicesService extends Service {

	protected static UDPListener udpListener = null;
	protected static TCPListener tcpListener = null;
	protected static Activity parentActivity = null;
	protected DiscoverDevicesService context = this;
	protected static boolean isRunning = false;
	
	@Override
	public void onCreate() {
		isRunning = true;
		
		udpListener = new BroadcastListener((WifiManager)this.getSystemService(Context.WIFI_SERVICE));
		udpListener.start();
		
		tcpListener = new TCPListener(parentActivity);
		tcpListener.start();
		
		Button b = (Button)parentActivity.findViewById(R.id.broadcastButton);
		b.setOnClickListener(new ButtonListener());
	}
	
	public void onDestroy() {
		Log.d("Socket", "Closing sockets");
		udpListener.closeSocket();
		for(int i = 0 ; i < tcpListener.getSockets().size() ; i++) {
			try {
				tcpListener.getSockets().get(i).close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		tcpListener.closeSocket();
	}
	
	public static void startListeningBroadcast() {
		udpListener.start();
	}
	
	public static void stopListeningBroadcast() {
		if (udpListener != null && udpListener.getSocket() != null)
			udpListener.closeSocket();
	}
	
	public static void findDevices() {
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
					new Thread()
					{
					    public void run() {
					    	try {
								udpListener.sendDiscoveryRequest(udpListener.getSocket());
							} catch (IOException e) {
								e.printStackTrace();
							}
					    }
					}.start();
				}
			}
			
		}
	}

}
