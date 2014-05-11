package com.pdsd.pixchange;


import com.example.proiectpdsd.R;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	protected MainActivity context = this;
	protected BroadcastListener broadcastListener = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		broadcastListener = new BroadcastListener((WifiManager)this.getSystemService(Context.WIFI_SERVICE), this);
		broadcastListener.start();
		
		Button b = (Button)findViewById(R.id.broadcastButton);
		b.setOnClickListener(new ButtonListener());
		
		Button b1 = (Button)findViewById(R.id.broadcastReply);
		b1.setOnClickListener(new ButtonListener());
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public void onBackPressed() {
		try {
			stopListeningBroadcast();
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.onBackPressed();
	}
	
	public void processMessage(IMessage message) {
		if (message.getMessageType() == IMessageTypes.BROADCAST_MESSAGE) {
			BroadcastMessage bm = (BroadcastMessage)message;
			TextView tv = (TextView)findViewById(R.id.broadcastReceived);
			Log.d("Message received on ", android.os.Build.MODEL + " " + bm.getInfo());
			//daca las linia de mai jos, crapa socket-ul :-??
			//tv.setText(bm.getInfo());
		}
		
	}
	
	public void startListeningBroadcast() {
		broadcastListener.start();
	}
	
	public void stopListeningBroadcast() {
		if (broadcastListener != null && broadcastListener.getSocket() != null)
			broadcastListener.closeSocket();
	}
	
	class ButtonListener implements View.OnClickListener {
		
		@Override
		public void onClick(View arg0) {
			if (arg0 instanceof Button) {
				if (((Button)arg0).getId() == R.id.broadcastButton) {
					stopListeningBroadcast();
					new DiscoverDevices((WifiManager) context.getSystemService(Context.WIFI_SERVICE), context).start();
					//TextView tv = (TextView)findViewById(R.id.broadcastReceived);
				}
			}
			
		}
	}
	
}
