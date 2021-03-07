package edu.berkeley.eecs.emission.tracker.location;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;

import androidx.annotation.Nullable;

import edu.berkeley.eecs.emission.MainActivity;

import java.util.Arrays;
import java.util.stream.Collectors;

import edu.berkeley.eecs.emission.tracker.verification.SensorControlBackgroundChecker;
import edu.berkeley.eecs.emission.tracker.ExplicitIntent;

import edu.berkeley.eecs.emission.unifiedlogger.Log;
import edu.berkeley.eecs.emission.unifiedlogger.NotificationHelper;

/**
 * Created by shankari on 1/30/18
 * as a hopefully short-to-medium term workaround for
 * https://github.com/e-mission/e-mission-data-collection/issues/164
 *
 * As background restrictions grow, this service is gaining in importance.
 * It is now created from user interaction components only:
 * - on consent
 * - on tracking on/off
 * - for the rest of the time, it receives messages from the FSM that it reflects in the notification message
 */

public class TripDiaryStateMachineForegroundService extends Service {
    private static String TAG = "TripDiaryStateMachineForegroundService";
    private static final int ONGOING_TRIP_ID = 6646464;
    private final IBinder mBinder = new LocalBinder();

    @Override
    public void onCreate() {
        Log.d(this, TAG, "onCreate called");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(this, TAG, "onStartCommand called with intent = "+intent+
          " flags = " + flags +  " and startId = " + startId);
        String message  = this.getString(R.string.notify_curr_state, TripDiaryStateMachineService.getState(this));
        if (intent == null) {
          SensorControlBackgroundChecker.checkLocationSettingsAndPermissions(this);
          message = this.getString(R.string.notify_curr_state, TripDiaryStateMachineService.getState(this));
        }
        handleStart(message, intent, flags, startId);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    /*
     * This is currently the only foreground service in the app and will remain running until
     * the user turns tracking off.
     */
    private void handleStart(String msg, Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(this, TAG, "onStartCommand called on oreo+, with msg "+ msg + " starting foreground service");
            // Go to the foreground with a dummy notification
            this.startForeground(ONGOING_TRIP_ID, getNotification(msg));
        } else {
            Log.d(this, TAG, "onStartCommand called on pre-oreo, ignoring");
        }
    }

    @Override
    public void onDestroy() {
        Log.d(this, TAG, "onDestroy called for foreground service");
        handleDestroy(this);
    }

    private void handleDestroy(Service srv) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(srv, TAG, "onDestroy called, removing notification");
            srv.stopForeground(true);
        } else {
            Log.d(srv, TAG, "onDestroy called on pre-oreo, ignoring");
        }
    }

  public class LocalBinder extends Binder {
    TripDiaryStateMachineForegroundService getService() {
      return TripDiaryStateMachineForegroundService.this;
    }
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    Log.d(this, TAG, "onBind called with intent "+intent);
    return mBinder;
  }

  public void setStateMessage(String message) {
    NotificationManager nMgr = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
    nMgr.notify(ONGOING_TRIP_ID, getNotification(message));
  }


  private Notification getNotification(String msg) {
      NotificationManager nMgr = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
      Notification.Builder builder = NotificationHelper.getNotificationBuilderForApp(this,
        nMgr, msg);
      builder.setOngoing(true);

      Intent activityIntent = new Intent(this, MainActivity.class);
      activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

      PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0,
        activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
      builder.setContentIntent(activityPendingIntent);
      return builder.build();
    }

    /*
     Returns the correct pending intent to be passed to invoke a background service
     when the app is in the background (which is almost 100% of the time in our case.
     We need to use getService for pre-Oreo and getForegroundService for O+.
     There are 4 usages of this pattern, so it is probably worth pulling out into a static function.
     When we ever move the minAPI up to 26, we can move this back inline.
     */

    public static PendingIntent getProperPendingIntent(Context ctxt, Intent innerIntent) {
        return PendingIntent.getService(ctxt, 0, innerIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void startProperly(Context ctxt) {
    Log.d(ctxt, TAG, "startProperly called with context = "+ctxt);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ctxt.startForegroundService(getForegroundServiceIntent(ctxt));
      } else {
        ctxt.startService(getForegroundServiceIntent(ctxt));
      }
    }

    private static Intent getForegroundServiceIntent(Context ctxt) {
      return new Intent(ctxt, TripDiaryStateMachineForegroundService.class);
    }

  public static void checkForegroundNotification(Context ctxt) {
    if(Build.VERSION.SDK_INT >Build.VERSION_CODES.O) {
      NotificationManager mgr = (NotificationManager) ctxt.getSystemService(Context.NOTIFICATION_SERVICE);
      StatusBarNotification[] activeNotifications = mgr.getActiveNotifications();
      Log.d(ctxt, TAG, "In checkForegroundNotification, found " + activeNotifications.length + " active notifications");
      for (StatusBarNotification notification : activeNotifications) {
        if (notification.getId() == TripDiaryStateMachineForegroundService.ONGOING_TRIP_ID) {
          Log.d(ctxt, TAG, "Found foreground notification with ID " + TripDiaryStateMachineForegroundService.ONGOING_TRIP_ID + " nothing to do");
          return;
        }
      }
      Log.d(ctxt, TAG, "Did not find foreground notification with ID " + TripDiaryStateMachineForegroundService.ONGOING_TRIP_ID + " in list " + Arrays.stream(activeNotifications).map(n -> n.getId()).collect(Collectors.toList()));
      NotificationHelper.createNotification(ctxt, ONGOING_TRIP_ID + 1, ctxt.getString(R.string.foreground_killed_email_log));
      TripDiaryStateMachineForegroundService.startProperly(ctxt);
      // It is not enough to move to the foreground; you also need to reinitialize tracking
      // https://github.com/e-mission/e-mission-docs/issues/580#issuecomment-700747931
      ctxt.sendBroadcast(new ExplicitIntent(ctxt, R.string.transition_initialize));
    } else {
      Log.d(ctxt, TAG, "Pre-Oreo, no foreground service, no need to check for notification");
    }
  }
}
