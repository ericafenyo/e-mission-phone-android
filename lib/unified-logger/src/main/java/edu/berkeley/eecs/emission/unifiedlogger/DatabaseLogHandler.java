package edu.berkeley.eecs.emission.unifiedlogger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * Created by shankari on 7/16/15.
 */
public class DatabaseLogHandler extends SQLiteOpenHelper {
    private String TABLE_LOG = "logTable";
    private String KEY_ID = "ID";
    private String KEY_TS = "ts";
    private String KEY_LEVEL = "level";
    private String KEY_MESSAGE = "message";
    private static final int DATABASE_VERSION = 1;

    private Context cachedContext;
    Formatter formatter;
    SQLiteDatabase writeDB;

    public DatabaseLogHandler(Context context) {
        super(context, "loggerDB", null, DATABASE_VERSION);
        cachedContext = context;
        writeDB = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String CREATE_LOG_TABLE = "CREATE TABLE " + TABLE_LOG +" (" +
                KEY_ID + " INTEGER PRIMARY KEY NOT NULL, "+ KEY_TS + " REAL, " +
                KEY_LEVEL + " TEXT, " + KEY_MESSAGE +" TEXT)";
        System.out.println("CREATE_LOG_TABLE = " + CREATE_LOG_TABLE);
        sqLiteDatabase.execSQL(CREATE_LOG_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_LOG);
        onCreate(sqLiteDatabase);
    }

    public void log(String level, String message) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_TS, currentTimeSecs());
        cv.put(KEY_LEVEL, level);
        cv.put(KEY_MESSAGE, message);
        writeDB.insert(TABLE_LOG, null, cv);
    }

    public void clear() {
        writeDB.delete(TABLE_LOG, null, null);
    }

    public void truncateObsolete() {
        // We somewhat arbitrarily decree that entries that are over a month old are obsolete
        // This is to avoid unbounded growth of the log table
        double monthAgoTs = currentTimeSecs() - 30 * 24 * 60 * 60; // 30 days * 24 hours * 60 minutes * 60 secs
        log("INFO", "truncating obsolete entries before "+monthAgoTs);
        writeDB.delete(TABLE_LOG, KEY_TS+" < "+monthAgoTs, null);
    }

    public int getMaxIndex() {
        String selectQuery = "SELECT MAX(ID) FROM "+TABLE_LOG;
        Cursor queryVal = writeDB.rawQuery(selectQuery, null);
        if (queryVal.moveToFirst()) {
            return queryVal.getInt(0);
        }
        return -1;
    }

    public JSONArray getMessagesFromIndex(int startIndex, int count) throws JSONException {
        JSONArray resultArr = new JSONArray();
        String selectQuery = "SELECT * FROM "+TABLE_LOG
                + " WHERE "+KEY_ID+" < "+startIndex
                + " ORDER BY "+KEY_ID+" DESC LIMIT "+count;
        Cursor queryVal = writeDB.rawQuery(selectQuery, null);
        int resultCount = queryVal.getCount();
        if (queryVal.moveToFirst()) {
            for (int i=0; i < resultCount; i++) {
                JSONObject currResult = new JSONObject();
                currResult.put(KEY_ID, queryVal.getInt(0));
                currResult.put(KEY_TS, queryVal.getDouble(1));
                currResult.put(KEY_LEVEL, queryVal.getString(2));
                currResult.put(KEY_MESSAGE, queryVal.getString(3));
                resultArr.put(currResult);
                queryVal.moveToNext();
            }
        }
        return resultArr;
    }

    private double currentTimeSecs() {
        return ((double)System.currentTimeMillis())/1000;
    }
}
