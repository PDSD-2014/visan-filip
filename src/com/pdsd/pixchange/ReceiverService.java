package com.pdsd.pixchange;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;

public class ReceiverService extends Service {
	private static final String TAG = "PixchangeReceiverService";

	// locals
	private PhotoObserver observer;
	List<File> toShare;
	private Boolean run = true;

	private class PhotoObserver extends ContentObserver {
		Uri observing;
		List<File> photos;

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
		public PhotoObserver(Uri observing, boolean notify, List<File> photos) {
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
			File photo = getPhoto();
			if (photo != null) {
				// put new photo in photos list
				photos.add(photo);

				Log.d(TAG, "New photo " + photo.getName());
			}
		}

		/**
		 * Get the latest photo from the URI you are watching
		 * 
		 * @return The last photo (in order of date added) from the URI
		 */
		private File getPhoto() {
			File photo = null;
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
				photo = new File(cursor.getString(0));

			cursor.close();
			return photo;
		}
	}

	@Override
	public void onCreate() {
		// create a list to store which photos to share
		toShare = new ArrayList<File>();

		// register a ContentObserver that will trigger on a new image having
		// been taken with the device's camera
		observer = new PhotoObserver(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, toShare);

		// TODO: get photos via other devices

		Log.d(TAG, "ReceiverService started");
	}

	@Override
	public void onDestroy() {
		// unregister observer
		observer.unregister();
		
		// TODO: remove this
		Iterator<File> it = toShare.iterator();
		
		Log.d(TAG, "Photos list:");
		while(it.hasNext()) {
			Log.d(TAG, "\t" + it.next().getName());
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