package edu.berkeley.eecs.emission.unifiedlogger;

import android.content.Context;
import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Created by shankari on 1/19/15.
 * Drop-in replacement for android.Log which logs to a database that we can
 * then display on the phone and email if necessary.
 */

public class Log {
    private static DatabaseLogHandler logger;

    public static DatabaseLogHandler getLogger(Context ctxt) {
        if (ctxt == null) {
            return null;
        }
        if (logger == null) {
            System.out.println("logger == null, lazily creating new logger");
            logger = new DatabaseLogHandler(ctxt);
        }
        // System.out.println("Returning logger "+logger);
        return logger;
    }

    public static void clear(Context ctxt) {
        getLogger(ctxt).clear();
    }

    public static int getMaxIndex(Context ctxt) {
        return getLogger(ctxt).getMaxIndex();
    }

    public static JSONArray getMessagesFromIndex(Context ctxt, int startIndex, int count)
            throws JSONException {
        return getLogger(ctxt).getMessagesFromIndex(startIndex, count);
    }

    public static void truncateObsolete(Context ctxt) {
        getLogger(ctxt).truncateObsolete();
    }

    public static void log(Context ctxt, String level, String TAG, String message) {
        getLogger(ctxt).log(level,
                String.format("%s : %s", TAG, message));
    }

    public static void d(Context ctxt, String TAG, String message) {
        try {
            getLogger(ctxt).log("DEBUG",
                    String.format("%s : %s", TAG, message));
        } catch (Exception e) {
            e.printStackTrace();
        }
        android.util.Log.d(TAG, message);
    }

    public static void i(Context ctxt, String TAG, String message) {
        try {
            getLogger(ctxt).log("INFO",
                    String.format("%s : %s", TAG, message));
        } catch (Exception e) {
            e.printStackTrace();
        }
        android.util.Log.i(TAG, message);
    }

    public static void w(Context ctxt, String TAG, String message) {
        try {
            getLogger(ctxt).log("WARN",
                    String.format("%s : %s", TAG, message));
        } catch (Exception e) {
            e.printStackTrace();
        }
        android.util.Log.w(TAG, message);
    }

    public static void e(Context ctxt, String TAG, String message) {
        try {
            getLogger(ctxt).log("ERROR",
                    String.format("%s : %s", TAG, message));
        } catch (Exception e) {
            e.printStackTrace();
        }
        android.util.Log.e(TAG, message);
    }

    public static void exception(Context ctxt, String TAG, Exception exp) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exp.printStackTrace(pw);
            Log.e(ctxt, TAG, pw.toString());
            android.util.Log.e(TAG, pw.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
