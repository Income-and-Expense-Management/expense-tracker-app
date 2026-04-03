package com.ptithcm.quanlichitieu.data.remote;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Thread-safe Singleton managing the Volley RequestQueue.
 * Uses double-checked locking with volatile to ensure safe lazy initialization.
 *
 * Single Responsibility: Only manages the lifecycle of the RequestQueue.
 */
public class VolleySingleton {

    private static final String TAG = "VolleySingleton";
    private static volatile VolleySingleton instance;
    private final RequestQueue requestQueue;

    private VolleySingleton(Context context) {
        Log.d(TAG, "Creating new RequestQueue instance");
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public static VolleySingleton getInstance(Context context) {
        if (instance == null) {
            synchronized (VolleySingleton.class) {
                if (instance == null) {
                    Log.d(TAG, "Initializing VolleySingleton");
                    instance = new VolleySingleton(context);
                }
            }
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        return requestQueue;
    }

    public <T> void addToRequestQueue(Request<T> request) {
        Log.d(TAG, "Adding request to queue: " + request.getUrl());
        requestQueue.add(request);
    }
}
