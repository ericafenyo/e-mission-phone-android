package edu.berkeley.eecs.emission.usercache;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Abstract superclass for the client side component of the user cache.
 */
public interface UserCache {

    class TimeQuery {
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public double getStartTs() {
            return startTs;
        }

        public void setStartTs(double startTs) {
            this.startTs = startTs;
        }

        public double getEndTs() {
            return endTs;
        }

        public void setEndTs(double endTs) {
            this.endTs = endTs;
        }

        String key;
        double startTs;
        double endTs;

        public TimeQuery(String key, double startTs, double endTs) {
            this.key = key;
            this.startTs = startTs;
            this.endTs = endTs;
        }

        public String toString() {
            return startTs + " < " + key + " < " + endTs;
        }
    }

    /**
      The value should be an object that is serializable using GSON.
      Most objects are, but it would be good to confirm, probably by
      adding a serialization/deserialization test to WrapperTest.
     */
    public abstract void putSensorData(int key, Object value);
    public abstract void putMessage(int key, Object value);
    public abstract void putReadWriteDocument(int key, Object value);

    /*
     Versions that save JSON objects directly. For use with the plugin interface.
     */
    public abstract void putSensorData(String key, JSONObject value);
    public abstract void putMessage(String key, JSONObject value);
    public abstract void putReadWriteDocument(String key, JSONObject value);


    // Versions that return an object retrieved via GSON. These are intended for use with native code.
    public abstract <T> T[] getMessagesForInterval(int key, TimeQuery tq, Class<T> classOfT);
    public abstract <T> T[] getSensorDataForInterval(int key, TimeQuery tq, Class<T> classOfT);

    public abstract <T> T[] getLastMessages(int key, int nEntries, Class<T> classOfT);
    public abstract <T> T[] getLastSensorData(int key, int nEntries, Class<T> classOfT);

    public abstract <T> T[] getFirstMessages(int key, int nEntries, Class<T> classOfT);
    public abstract <T> T[] getFirstSensorData(int key, int nEntries, Class<T> classOfT);

    // Versions that return a raw JSON object. These are intended for use with the plugin.
    // Note that for the plugin to use the prior interface, we would need to keep a mapping
    // of keys to wrapper objects, which is:
    // a) cumbersome and
    // b) not necessary when the objects are not consumed in native code
    // c) wasted cycles JSON -> GSON -> JSON
    public abstract JSONArray getMessagesForInterval(String key, TimeQuery tq,
                                                     boolean withMetadata) throws JSONException;
    public abstract JSONArray getSensorDataForInterval(String key, TimeQuery tq,
                                                       boolean withMetadata) throws JSONException;

    public abstract JSONArray getLastMessages(String key, int nEntries,
                                              boolean withMetadata) throws JSONException;
    public abstract JSONArray getLastSensorData(String key, int nEntries,
                                                boolean withMetadata) throws JSONException;

    public abstract JSONArray getFirstMessages(String key, int nEntries,
                                              boolean withMetadata) throws JSONException;
    public abstract JSONArray getFirstSensorData(String key, int nEntries,
                                                boolean withMetadata) throws JSONException;

    /*
     * Versions that retrieve temporary configurations that need to be shared between
     * javascript and native. Functions as standard k-v store, no wrapper classes,
     * so no object interface.
     */

    public abstract void putLocalStorage(String key, JSONObject value);
    public abstract JSONObject getLocalStorage(String key, boolean withMetadata) throws JSONException;
    public abstract void removeLocalStorage(String key);
        /**
         * Return the document that matches the specified key.
         * The class of T needs to be passed in, and an appropriate type will be reconstructed
         * and returned.
         */

    public abstract <T> T getDocument(int key, Class<T> classOfT);
    // Can be either JSONObject or JSONArray, and they don't have a common superclass other than
    // object
    public abstract Object getDocument(String key, boolean withMetadata) throws JSONException;

    /**
     * Delete documents that match the specified time query.
     * This allows us to support eventual consistency without locking.
     */
    public abstract void clearEntries(TimeQuery tq);
    public abstract void invalidateCache(TimeQuery tq);

    /*
     * Nuclear option to recover from bad state
     */
    public abstract void clear();
}
