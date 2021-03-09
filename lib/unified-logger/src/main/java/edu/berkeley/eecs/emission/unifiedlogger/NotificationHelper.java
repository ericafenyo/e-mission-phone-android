package edu.berkeley.eecs.emission.unifiedlogger;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;

import org.apache.cordova.CordovaActivity;

import edu.berkeley.eecs.emission.MainActivity;
import edu.berkeley.eecs.emission.R;

import java.util.List;


public class NotificationHelper {
	private static String TAG = "NotificationHelper";
	private static String DEFAULT_CHANNEL_ID = "emissionPluginChannel";
	private static String DEFAULT_CHANNEL_DESCRIPTION = "common channel used by all e-mission native plugins";
	public static final String DISPLAY_RESOLUTION_ACTION = "DISPLAY_RESOLUTION";
	public static final String RESOLUTION_PENDING_INTENT_KEY = "rpIntentKey";

	public static void createNotification(Context context, int id, String message) {
		NotificationManager nMgr =
				(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

		Notification.Builder builder = getNotificationBuilderForApp(context, nMgr, message);
		/*
		 * This is a bit of magic voodoo. The tutorial on launching the activity actually uses a stackbuilder
		 * to create a fake stack for the new activity. However, it looks like the stackbuilder
		 * is only available in more recent versions of the API. So I use the version for a special activity PendingIntent
		 * (since our app currently has only one activity) which resolves that issue.
		 * This also appears to work, at least in the emulator.
		 * 
		 * TODO: Decide what level API we want to support, and whether we want a more comprehensive activity.
		 */
		Intent activityIntent = new Intent(context, MainActivity.class);
		activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		PendingIntent activityPendingIntent = PendingIntent.getActivity(context, 0,
				activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(activityPendingIntent);		
		
		Log.d(context, TAG, "Generating notify with id " + id + " and message " + message);
		nMgr.notify(id, builder.build());
	}

	public static void createNotification(Context context, int id, String message, PendingIntent intent) {
		NotificationManager nMgr =
				(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

		Notification.Builder builder = getNotificationBuilderForApp(context, nMgr, message);
		builder.setContentIntent(intent);

		Log.d(context, TAG, "Generating notify with id " + id + ", message " + message
				+ " and pending intent " + intent);
		nMgr.notify(id, builder.build());
	}

		/*
	 * Used to show a resolution - e.g. to turn on location services
		 */
	public static void createResolveNotification(Context context, int id, String message, PendingIntent intent) {
		NotificationManager nMgr =
				(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

		Notification.Builder builder = getNotificationBuilderForApp(context, nMgr, message);

		Intent activityIntent = new Intent(context, MainActivity.class);
		activityIntent.setAction(DISPLAY_RESOLUTION_ACTION);
		activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		activityIntent.putExtra(NotificationHelper.RESOLUTION_PENDING_INTENT_KEY, intent);

		PendingIntent activityPendingIntent = PendingIntent.getActivity(context, 0,
				activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(activityPendingIntent);
		// builder.setAutoCancel(true);

		Log.d(context, TAG, "Generating notify with id " + id + ", message " + message
				+ " and pending intent " + intent);
		nMgr.notify(id, builder.build());
	}

	public static void cancelNotification(Context context, int id) {
		NotificationManager nMgr =
				(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

		Log.d(context, TAG, "Cancelling notify with id " + id);
		nMgr.cancel(id);
	}

	public static Notification.Builder getNotificationBuilderForApp(Context context,
																	NotificationManager nMgr,
																	String message) {
		Notification.Builder builder = null;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			createDefaultNotificationChannelIfNeeded(context);
			builder = new Notification.Builder(context, DEFAULT_CHANNEL_ID);
		} else {
			builder = new Notification.Builder(context);
		}
		Bitmap appIcon = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
		builder.setLargeIcon(appIcon);
		builder.setSmallIcon(R.drawable.ic_visibility_black);
		builder.setContentTitle(context.getString(R.string.app_name));
		builder.setContentText(message);

		return builder;
	}

	@TargetApi(26)
	private static void createDefaultNotificationChannelIfNeeded(final Context ctxt) {
		// only call on Android O and above
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			final NotificationManager notificationManager = (NotificationManager) ctxt.getSystemService(Context.NOTIFICATION_SERVICE);
			List<NotificationChannel> channels = notificationManager.getNotificationChannels();

			for (int i = 0; i < channels.size(); i++) {
				String id = channels.get(i).getId();
				Log.d(ctxt, TAG, "Checking channel with id = "+id);
				if (DEFAULT_CHANNEL_ID.equals(id)) {
					Log.d(ctxt, TAG, "Default channel found, returning");
					return;
				}
			}

			Log.d(ctxt, TAG, "Default channel not found, creating a new one");
			NotificationChannel dChannel = new NotificationChannel(DEFAULT_CHANNEL_ID,
					DEFAULT_CHANNEL_DESCRIPTION, NotificationManager.IMPORTANCE_LOW);
			dChannel.enableVibration(true);
			notificationManager.createNotificationChannel(dChannel);
		}
	}
}
