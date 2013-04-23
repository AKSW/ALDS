package org.aksw.lds.service;

import org.aksw.lds.MainActivity;
import org.aksw.lds.server.JettyServer;
import org.aksw.lds.service.config.ConfigManager;

import android.R;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class BackgroundService extends Service {
	private static final String TAG = "BackgroundService";
	
	private JettyServer server;
	private ConfigManager configManager;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Service onStartCommand");
		android.os.Debug.waitForDebugger();
		
		Notification note = new Notification(0, null, System.currentTimeMillis());
	    note.flags |= Notification.FLAG_NO_CLEAR;
	    startForeground( 42, note );
 		
		configManager = new ConfigManager(this);
 		server = new JettyServer(configManager);
		
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		Log.i(TAG, "Service onBind");
		return null;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "Service onDestroy");
	}
}
