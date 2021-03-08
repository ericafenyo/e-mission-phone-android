package edu.berkeley.eecs.emission.usercache;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.google.gson.Gson;

import edu.berkeley.eecs.emission.R;

public class UserCachePlugin extends CordovaPlugin {

    protected void pluginInitialize() {
        // TODO: Figure out whether we still need this, given that we are using the standard usercache
        // interface anyway. But for now, we will retain it because otherwise, I am not sure how
        // best to deal with this.

        // Let's just access the usercache so that it is created
        UserCache currCache = UserCacheFactory.getUserCache(cordova.getActivity());
        System.out.println("During plugin initialize, created usercache" + currCache);
        // let's get a document - the table is created lazily during first use
        try {
            currCache.getDocument(R.string.key_usercache_transition, JSONObject.class);
        } catch (Exception e) {
            System.out.println("Expected error "+e+" while getting document since we are reading a dummy key");
        }
    }

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        Context ctxt = cordova.getActivity();
        if (action.equals("getDocument")) {
            final String documentKey = data.getString(0);
            final boolean withMetadata = data.getBoolean(1);
            Object retVal = UserCacheFactory.getUserCache(ctxt).getDocument(documentKey, withMetadata);
                try {
                callbackContext.success((JSONObject) retVal);
            } catch (ClassCastException e) {
                callbackContext.success((JSONArray) retVal);
            }
            return true;
        } else if (action.equals("getSensorDataForInterval")) {
            final String key = data.getString(0);
            final JSONObject tqJsonObject = data.getJSONObject(1);
            final boolean withMetadata = data.getBoolean(2);
            final UserCache.TimeQuery timeQuery = new Gson().fromJson(tqJsonObject.toString(), UserCache.TimeQuery.class);
            JSONArray result = UserCacheFactory.getUserCache(ctxt)
                    .getSensorDataForInterval(key, timeQuery, withMetadata);
            callbackContext.success(result);
            return true;
        } else if (action.equals("getMessagesForInterval")) {
            final String key = data.getString(0);
            final JSONObject tqJsonObject = data.getJSONObject(1);
            final boolean withMetadata = data.getBoolean(2);
            final UserCache.TimeQuery timeQuery = new Gson().fromJson(tqJsonObject.toString(),
                    UserCache.TimeQuery.class);
            JSONArray result = UserCacheFactory.getUserCache(ctxt)
                    .getMessagesForInterval(key, timeQuery, withMetadata);
            callbackContext.success(result);
            return true;
        } else if (action.equals("getLastMessages")) {
            final String key = data.getString(0);
            final int nEntries = data.getInt(1);
            final boolean withMetadata = data.getBoolean(2);
            JSONArray result = UserCacheFactory.getUserCache(ctxt)
                    .getLastMessages(key, nEntries, withMetadata);
            callbackContext.success(result);
            return true;
        } else if (action.equals("getLastSensorData")) {
            final String key = data.getString(0);
            final int nEntries = data.getInt(1);
            final boolean withMetadata = data.getBoolean(2);
            JSONArray result = UserCacheFactory.getUserCache(ctxt)
                    .getLastSensorData(key, nEntries, withMetadata);
            callbackContext.success(result);
            return true;
        } else if (action.equals("getFirstMessages")) {
            final String key = data.getString(0);
            final int nEntries = data.getInt(1);
            final boolean withMetadata = data.getBoolean(2);
            JSONArray result = UserCacheFactory.getUserCache(ctxt)
                    .getFirstMessages(key, nEntries, withMetadata);
            callbackContext.success(result);
            return true;
        } else if (action.equals("getFirstSensorData")) {
            final String key = data.getString(0);
            final int nEntries = data.getInt(1);
            final boolean withMetadata = data.getBoolean(2);
            JSONArray result = UserCacheFactory.getUserCache(ctxt)
                    .getFirstSensorData(key, nEntries, withMetadata);
            callbackContext.success(result);
            return true;
        } else if (action.equals("getLocalStorage")) {
            final String key = data.getString(0);
            final boolean withMetadata = data.getBoolean(1);
            JSONObject result = UserCacheFactory.getUserCache(ctxt)
                    .getLocalStorage(key, withMetadata);
            if (result == null) {
                // if we don't do this, we end up with passing in a null object
                // and we call toString() on it in the constructor of PluginResult
                // which crashes
                // there is a null check for string, so let's use that instead to workaround
                String temp = null;
                callbackContext.success(temp);
            } else {
            callbackContext.success(result);
            }
            return true;
        } else if (action.equals("putMessage")) {
            final String key = data.getString(0);
            final JSONObject msg = data.getJSONObject(1);
            UserCacheFactory.getUserCache(ctxt).putMessage(key, msg);
            callbackContext.success();
            return true;
        } else if (action.equals("putRWDocument")) {
            final String key = data.getString(0);
            final JSONObject msg = data.getJSONObject(1);
            UserCacheFactory.getUserCache(ctxt).putReadWriteDocument(key, msg);
            callbackContext.success();
            return true;
        } else if (action.equals("putSensorData")) {
            final String key = data.getString(0);
            final JSONObject msg = data.getJSONObject(1);
            UserCacheFactory.getUserCache(ctxt).putSensorData(key, msg);
            callbackContext.success();
            return true;
        } else if (action.equals("putLocalStorage")) {
            final String key = data.getString(0);
            final JSONObject msg = data.getJSONObject(1);
            UserCacheFactory.getUserCache(ctxt).putLocalStorage(key, msg);
            callbackContext.success();
            return true;
        } else if (action.equals("getLocalStorage")) {
            final String key = data.getString(0);
            final boolean withMetadata = data.getBoolean(1);
            JSONObject result = UserCacheFactory.getUserCache(ctxt)
                    .getLocalStorage(key, withMetadata);
            callbackContext.success(result);
            return true;
        } else if (action.equals("removeLocalStorage")) {
            final String key = data.getString(0);
            UserCacheFactory.getUserCache(ctxt)
                    .removeLocalStorage(key);
            callbackContext.success();
            return true;
        } else if (action.equals("clearEntries")) {
            final JSONObject tqJsonObject = data.getJSONObject(1);

            final UserCache.TimeQuery timeQuery = new Gson().fromJson(tqJsonObject.toString(),
                    UserCache.TimeQuery.class);
            UserCacheFactory.getUserCache(ctxt).clearEntries(timeQuery);
            callbackContext.success();
            return true;
        } else if (action.equals("invalidateCache")) {
            final JSONObject tqJsonObject = data.getJSONObject(0);

            final UserCache.TimeQuery timeQuery = new Gson().fromJson(tqJsonObject.toString(),
                    UserCache.TimeQuery.class);
            UserCacheFactory.getUserCache(ctxt).invalidateCache(timeQuery);
            callbackContext.success();
            return true;
        } else if (action.equals("clearAll")) {
            UserCacheFactory.getUserCache(ctxt).clear();
            callbackContext.success();
            return true;
        }
        return false;
    }
}

