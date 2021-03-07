package edu.berkeley.eecs.emission.tracker;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationRequest;
import com.google.gson.Gson;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import edu.berkeley.eecs.emission.*;
import edu.berkeley.eecs.emission.tracker.location.TripDiaryStateMachineService;
import edu.berkeley.eecs.emission.tracker.location.TripDiaryStateMachineForegroundService;
import edu.berkeley.eecs.emission.tracker.location.TripDiaryStateMachineReceiver;
import edu.berkeley.eecs.emission.tracker.wrapper.ConsentConfig;
import edu.berkeley.eecs.emission.tracker.wrapper.LocationTrackingConfig;
import edu.berkeley.eecs.emission.tracker.wrapper.StatsEvent;
import edu.berkeley.eecs.emission.tracker.verification.SensorControlForegroundDelegate;
import edu.berkeley.eecs.emission.unifiedlogger.Log;
import edu.berkeley.eecs.emission.usercache.BuiltinUserCache;

public class DataCollectionPlugin extends CordovaPlugin {
    public static final String TAG = "DataCollectionPlugin";
    private SensorControlForegroundDelegate mControlDelegate = null;

    @Override
    public void pluginInitialize() {
        mControlDelegate = new SensorControlForegroundDelegate(this, cordova);
        final Activity myActivity = cordova.getActivity();
        BuiltinUserCache.getDatabase(myActivity).putMessage(R.string.key_usercache_client_nav_event,
                new StatsEvent(myActivity, R.string.app_launched));

        TripDiaryStateMachineReceiver.initOnUpgrade(myActivity);
    }

    @Override
    public boolean execute(String action, JSONArray data, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("launchInit")) {
            Log.d(cordova.getActivity(), TAG, "application launched, init is nop on android");
            callbackContext.success();
            return true;
        } else if (action.equals("markConsented")) {
            Log.d(cordova.getActivity(), TAG, "marking consent as done");
            Context ctxt = cordova.getActivity();
            JSONObject newConsent = data.getJSONObject(0);
            ConsentConfig cfg = new Gson().fromJson(newConsent.toString(), ConsentConfig.class);
            ConfigManager.setConsented(ctxt, cfg);
            TripDiaryStateMachineForegroundService.startProperly(cordova.getActivity().getApplication());
            // Now, really initialize the state machine
            // Note that we don't call initOnUpgrade so that we can handle the case where the
            // user deleted the consent and re-consented, but didn't upgrade the app
            mControlDelegate.checkAndPromptPermissions();
            // ctxt.sendBroadcast(new ExplicitIntent(ctxt, R.string.transition_initialize));
            // TripDiaryStateMachineReceiver.restartCollection(ctxt);
            callbackContext.success();
            return true;
        } else if (action.equals("storeBatteryLevel")) {
            Context ctxt = cordova.getActivity();
            TripDiaryStateMachineReceiver.saveBatteryAndSimulateUser(ctxt);
            callbackContext.success();
        } else if (action.equals("getConfig")) {
            Context ctxt = cordova.getActivity();
            LocationTrackingConfig cfg = ConfigManager.getConfig(ctxt);
            // Gson.toJson() represents a string and we are expecting an object in the interface
            callbackContext.success(new JSONObject(new Gson().toJson(cfg)));
            return true;
        } else if (action.equals("setConfig")) {
            Context ctxt = cordova.getActivity();
            JSONObject newConfig = data.getJSONObject(0);
            LocationTrackingConfig cfg = new Gson().fromJson(newConfig.toString(), LocationTrackingConfig.class);
            ConfigManager.updateConfig(ctxt, cfg);
            TripDiaryStateMachineReceiver.restartCollection(ctxt);
            callbackContext.success();
            return true;
        } else if (action.equals("getState")) {
            Context ctxt = cordova.getActivity();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);
            String state = prefs.getString(ctxt.getString(R.string.curr_state_key), ctxt.getString(R.string.state_start));
            callbackContext.success(state);
            return true;
        } else if (action.equals("forceTransition")) {
            // we want to run this in a background thread because it might sometimes wait to get
            // the current location
            final String generalTransition = data.getString(0);
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    Context ctxt = cordova.getActivity();
                    Map<String, String> transitionMap = getTransitionMap(ctxt);
                    if (transitionMap.containsKey(generalTransition)) {
                        String androidTransition = transitionMap.get(generalTransition);
                        ctxt.sendBroadcast(new ExplicitIntent(ctxt, androidTransition));
                        callbackContext.success(androidTransition);
                    } else {
                        callbackContext.error(generalTransition + " not supported, ignoring");
                    }
                }
            });
            return true;
        } else if (action.equals("handleSilentPush")) {
            throw new UnsupportedOperationException("silent push handling not supported for android");
        } else if (action.equals("getAccuracyOptions")) {
            JSONObject retVal = new JSONObject();
            retVal.put("PRIORITY_HIGH_ACCURACY", LocationRequest.PRIORITY_HIGH_ACCURACY);
            retVal.put("PRIORITY_BALANCED_POWER_ACCURACY", LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            retVal.put("PRIORITY_LOW_POWER", LocationRequest.PRIORITY_LOW_POWER);
            retVal.put("PRIORITY_NO_POWER", LocationRequest.PRIORITY_NO_POWER);
            callbackContext.success(retVal);
            return true;
        }
        return false;
    }

    private static Map<String, String> getTransitionMap(Context ctxt) {
        Map<String, String> retVal = new HashMap<String, String>();
        retVal.put("INITIALIZE", ctxt.getString(R.string.transition_initialize));
        retVal.put("EXITED_GEOFENCE", ctxt.getString(R.string.transition_exited_geofence));
        retVal.put("STOPPED_MOVING", ctxt.getString(R.string.transition_stopped_moving));
        retVal.put("STOP_TRACKING", ctxt.getString(R.string.transition_stop_tracking));
        retVal.put("START_TRACKING", ctxt.getString(R.string.transition_start_tracking));
        return retVal;
    }


    @Override
    public void onNewIntent(Intent intent) {
        Context mAct = cordova.getActivity();
        Log.d(mAct, TAG, "onNewIntent(" + intent.getAction() + ")");
        Log.d(mAct, TAG, "Found extras " + intent.getExtras());

        // this is will be NOP if we are not handling the right intent
        mControlDelegate.onNewIntent(intent);
        // This is where we can add other intent handlers
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException
    {
        // This will be a NOP if we are not requesting the right permission
        mControlDelegate.onRequestPermissionResult(requestCode, permissions, grantResults);
        // This is where we can add other permission callbacks
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(cordova.getActivity(), TAG, "received onActivityResult("+requestCode+","+
                resultCode+","+data.getDataString()+")");
        // This will be a NOP if we are not handling the correct activity intent
                mControlDelegate.onActivityResult(requestCode, resultCode, data);
        /*
         This is where we would handle other cases for activity results
        switch (requestCode) {
            default:
                Log.d(cordova.getActivity(), TAG, "Got unsupported request code "+requestCode+ " , ignoring...");
        }
         */
    }
}
