package edu.berkeley.eecs.emission.tracker.sensors;

import android.content.Context;

import edu.berkeley.eecs.emission.R;
import edu.berkeley.eecs.emission.tracker.wrapper.Battery;
import edu.berkeley.eecs.emission.usercache.UserCacheFactory;

/**
 * Created by shankari on 7/8/15.
 */
public class BatteryPollSensor implements PollSensor {
    public void getAndSaveValue(Context ctxt) {
        Battery currBattery = BatteryUtils.getBatteryInfo(ctxt);
        UserCacheFactory.getUserCache(ctxt).putSensorData(R.string.key_usercache_battery, currBattery);
    }
}
