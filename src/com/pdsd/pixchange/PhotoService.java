package com.pdsd.pixchange;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.widget.Button;

public class PhotoService extends Service {
	private static final String TAG = "PhotoService";

	// locals
	private String ID;
	private PhotoObserver observer;
	private WifiReceiver receiver;
	protected static Activity parentActivity;
	private List<Photo> toShare;
	protected static String storageFolder;
	
	private UDPListener udpListener = null;
	private TCPListener tcpListener = null;

	public class Photo {
		public File file;
		public String owner;

		public Photo(File file, String owner) {
			this.file = file;
			this.owner = owner;
		}

		@Override
		public boolean equals(Object o) {
			Photo p = (Photo) o;

			return ((file.equals(p.file)) && (owner == p.owner));
		}
	}

	private class PhotoObserver extends ContentObserver {
		Uri observing;
		//List<Photo> photos;

		private class PhotoQueuer implements Runnable {
			private Photo photo;

			private class PhotoQueuerHelper implements Runnable {
				Photo photo;

				public PhotoQueuerHelper(Photo photo) {
					this.photo = photo;
				}

				@Override
				public void run() {
					if(photo.file.exists()) {
						//photos.add(photo);
						//add photo to tcp server
						tcpListener.addPhoto(photo);
						//send udp broadcast
						new Thread() {
							public void run() {
								try {
									udpListener.sendDiscoveryRequest(udpListener.getSocket(), photo.file.getName());
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}.start();
						
						Log.d(TAG, "Photo " + photo.file.getName() + " queued");
					} else {
						Log.d(TAG, "Photo " + photo.file.getName() + " discarded");
					}
				}
			}

			public PhotoQueuer(Photo photo) {
				this.photo = photo;
			}

			@Override
			public void run() {
				new Handler().postDelayed(new PhotoQueuerHelper(photo), 5000);
			}
		}

		/**
		 * Create a new PhotoObserver object AND register it to the specified
		 * URI.
		 * 
		 * @param observing
		 *            The URI to observe.
		 * @param notify
		 *            If true changes to URIs beginning with observing will also
		 *            cause notifications to be sent. If false only changes to
		 *            the exact URI specified by observing will cause
		 *            notifications to be sent.
		 * @param photos
		 *            A list where to place new photos
		 */
		public PhotoObserver(Uri observing, boolean notify, List<Photo> photos) {
			super(null);

			this.observing = observing;
			//this.photos = photos;

			// register yourself as an observer to said URI
			getApplicationContext().getContentResolver()
					.registerContentObserver(observing, notify, this);
		}

		/**
		 * Unregisters the PhotoObserver from its URI
		 */
		public void unregister() {
			// unregister
			getApplicationContext().getContentResolver()
					.unregisterContentObserver(this);

			observing = null;
			//photos = null;
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);

			// get new photo
			Photo photo = getPhoto();
			if (photo != null) {
				Log.d(TAG, "New photo " + photo.file.getName());

				// put new photo in share list
				parentActivity.runOnUiThread(new PhotoQueuer(photo));
			}
		}

		/**
		 * Get the latest photo from the URI you are watching
		 * 
		 * @return The last photo (in order of date added) from the URI
		 */
		private Photo getPhoto() {
			Photo photo = null;
			String[] projection = { MediaColumns.DATA };
			Cursor cursor = getContentResolver().query(observing, projection,
					null, null, "date_added DESC");

			if (cursor == null) {
				Log.e(TAG, "Failed to query external content database");

				return null;
			} else if (cursor.getCount() == 0) {
				Log.d(TAG, "Query to external cotent database returned nothing");

				cursor.close();
				return null;
			}

			if (cursor.moveToNext())
				photo = new Photo(new File(cursor.getString(0)), ID);

			cursor.close();
			return photo;
		}
	}

	@Override
	public void onCreate() {
		// get the devices wifi MAC address to use as the identifier for this
		// device
		WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		ID = manager.getConnectionInfo().getMacAddress();

		// create a list to store which photos to share
		toShare = new ArrayList<Photo>();

		// register a BroadcastReceiver that will trigger when wifi
		// connects/disconnects
		receiver = new WifiReceiver();
		registerReceiver(receiver, new IntentFilter(
				"android.net.conn.CONNECTIVITY_CHANGE"));

		// register a ContentObserver that will trigger on a new image having
		// been taken with the device's camera
		observer = new PhotoObserver(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, toShare);

		//start udpListener for broadcasts
		udpListener = new BroadcastListener(this, (WifiManager)this.getSystemService(Context.WIFI_SERVICE));
		udpListener.start();
		
		//start tcpListener for incoming TCP connections
		tcpListener = new TCPListener(this, (WifiManager)this.getSystemService(Context.WIFI_SERVICE));
		tcpListener.start();
		
		// TODO: get photos via other devices

		Log.d(TAG, "ReceiverService started");
	}

	@Override
	public void onDestroy() {
		// unregister WifiReceiver
		Log.d("Service", "Service is being destroied");
		
		unregisterReceiver(receiver);

		// unregister PhotoObserver
		observer.unregister();

		// change shareButton text
		Button shareButton = (Button) parentActivity
				.findViewById(R.id.share_button);
		shareButton.setText(R.string.start_share_label);
		
		//close sockets
		udpListener.closeSocket();
		for(int i = 0 ; i < tcpListener.getSockets().size() ; i++) {
			try {
				tcpListener.getSockets().get(i).close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		tcpListener.closeSocket();
		
		
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
	
	public void setTCPListener(TCPListener tcpListener) {
		this.tcpListener = tcpListener;
	}
	
	public TCPListener getTCPListener() {
		return this.tcpListener;
	}
	
	public void setUDPListener(UDPListener udpListener) {
		this.udpListener = udpListener;
	}
	
	public UDPListener getUDPListener() {
		return this.udpListener;
	}
}