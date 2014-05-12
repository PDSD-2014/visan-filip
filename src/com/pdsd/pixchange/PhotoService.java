package com.pdsd.pixchange;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;

public class PhotoService extends Service {
	private static final String TAG = "PhotoService";

	// locals
	private String ID;
	private PhotoObserver observer;
	private List<Photo> toShare;
	
	private class Photo {
		private File file;
		private String owner;

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
		List<Photo> photos;

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
			this.photos = photos;

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
			photos = null;
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);

			// get new photo
			Photo photo = getPhoto();
			if (photo != null) {
				// TODO: delayed
				// put new photo in photos list
				photos.add(photo);

				Log.d(TAG, "New photo " + photo.file.getName());
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
		// get the devices wifi MAC address to use as the identifier for this device
		WifiManager manager = (WifiManager)getSystemService(Context.WIFI_SERVICE);  
		ID = manager.getConnectionInfo().getMacAddress();
		
		// create a list to store which photos to share
		toShare = new ArrayList<Photo>();

		// register a ContentObserver that will trigger on a new image having
		// been taken with the device's camera
		observer = new PhotoObserver(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, toShare);

		// TODO: register a file observer to detect when a file is deleted
		// TODO: get photos via other devices

		Log.d(TAG, "ReceiverService started");
	}

	@Override
	public void onDestroy() {
		// unregister observer
		observer.unregister();

		// TODO: remove this
		Iterator<Photo> it = toShare.iterator();

		Log.d(TAG, "Photos list:");
		while (it.hasNext()) {
			Log.d(TAG, "\t" + it.next().file.getName());
		}

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