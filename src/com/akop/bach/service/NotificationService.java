/*
 * NotificationService.java
 * Copyright (C) 2010-2013 Akop Karapetyan
 *
 * This file is part of Spark 360, the online gaming service client.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 *
 */

package com.akop.bach.service;

import java.text.DateFormat;
import java.util.HashMap;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.format.DateUtils;

import com.akop.bach.App;
import com.akop.bach.BasicAccount;
import com.akop.bach.Preferences;

public class NotificationService extends Service
{
	private static final String ACTION_UPDATE = "com.akop.bach.intent.action.SERVICE_UPDATE";
	private static final String ACTION_RESCHEDULE = "com.akop.bach.intent.action.SERVICE_RESCHEDULE";
	private static final String ACTION_CANCEL = "com.akop.bach.intent.action.SERVICE_CANCEL";
	
	private static PowerManager.WakeLock mWl;
	
	protected static class AccountSchedule
	{
		private String description;
		public long accountId;
		public long lastSyncMs;
		public long nextSyncMs;
		public long syncFreqMs;
		public Object param;
		
		public AccountSchedule(BasicAccount account, Context context)
		{
			this.description = account.getScreenName() + 
					" (" + account.getDescription() + ")";
			this.accountId = account.getId();
			this.lastSyncMs = 0;
			this.nextSyncMs = 0;
			this.syncFreqMs = account.getSyncPeriod() * DateUtils.MINUTE_IN_MILLIS;
			this.param = null;
		}
		
		public void updateSyncTime(long time)
		{
			this.lastSyncMs = time;
			this.nextSyncMs = time + this.syncFreqMs;
		}
		
		@Override
		public String toString()
		{
			return String.format("%s: last @ %.02f; next @ %.02f; now %.02f",
					this.description, 
					this.lastSyncMs / 1000.0,
					this.nextSyncMs / 1000.0,
					SystemClock.elapsedRealtime() / 1000.0);
		}
	}
	
	protected static HashMap<Long, AccountSchedule> mSchedules = new HashMap<Long, AccountSchedule>();
	
	static
	{
		mWl = null;
	}
	
	protected static void acquireWakeLock(Context context)
	{
		if (mWl == null)
		{
			if (App.getConfig().logToConsole())
				App.logv(" ++++++++ ACQUIRE WAKE LOCK ++++++++ ");
			
			PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
			
			mWl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Service Wake Lock");
			mWl.acquire();
		}
	}
	
	protected static void releaseWakeLock(Context context)
	{
		if (mWl != null)
		{
			if (App.getConfig().logToConsole())
				App.logv(" -------- RELEASE WAKE LOCK -------- ");
			
			if (mWl.isHeld())
				mWl.release();
			
			mWl = null;
		}
	}
	
	public static void actionReschedule(Context context)
	{
		acquireWakeLock(context);
		
		try
		{
			Intent intent = new Intent();
			intent.setClass(context, NotificationService.class);
			intent.setAction(NotificationService.ACTION_RESCHEDULE);
			
			context.startService(intent);
		}
		catch(Exception e)
		{
			// If something fails, release the wake lock
			// If the service starts without an error, the wake 
			// lock will be released there
			releaseWakeLock(context);
		}
	}
	
	public static void actionUpdate(Context context, long checkId)
	{
		acquireWakeLock(context);
		
		try
		{
			Intent intent = new Intent();
			intent.setClass(context, NotificationService.class);
			intent.setAction(NotificationService.ACTION_UPDATE);
			intent.putExtra("checkId", checkId);
			
			context.startService(intent);
		}
		catch(Exception e)
		{
			// If something fails, release the wake lock
			// If the service starts without an error, the wake 
			// lock will be released there
			releaseWakeLock(context);
		}
	}
    
	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
	
	private void reschedule()
	{
		long timeNow = SystemClock.elapsedRealtime();
		long nextCheckTime = Long.MAX_VALUE;
		AccountSchedule nextAccount = null;
		
		synchronized(mSchedules)
		{
			for (AccountSchedule schedule : mSchedules.values())
			{
				long nextSyncMs = schedule.nextSyncMs;
				
				if (schedule.lastSyncMs <= 0 || nextSyncMs < timeNow)
				{
					nextCheckTime = 0;// timeNow + 1000; //slight delay before the service kicks in
					nextAccount = schedule;
				}
				else if (nextSyncMs < nextCheckTime)
				{
					nextCheckTime = nextSyncMs;
					nextAccount = schedule;
				}
			}
		}
		
		if (nextAccount == null)
		{
			cancel();
			return;
		}
		
		if (App.getConfig().logToConsole())
			App.logv("SparkService/reschedule: next up is %s", nextAccount.toString());
		
		// Create an update alarm
		AlarmManager alarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		PendingIntent pi = createAlarmIntent(nextAccount.accountId);
		alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextCheckTime, pi);
	}
	
	private void cancel()
	{
		if (App.getConfig().logToConsole())
			App.logv("SparkService/cancel: canceling alarms");
		
		AlarmManager alarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		PendingIntent pi = createAlarmIntent(-1);
		alarmMgr.cancel(pi);
	}
	
	@Override
	public void onStart(Intent intent, int startId)
	{
		handleStart(intent, startId);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		handleStart(intent, startId);
		
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}
	
	protected PendingIntent createAlarmIntent(long checkId)
	{
		Intent intent = new Intent(this, UpdateAlarmReceiver.class);
		intent.putExtra("checkId", checkId);
		
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, 
				PendingIntent.FLAG_UPDATE_CURRENT);
		
		return pi;
	}
	
	private void handleStart(Intent intent, final int startId)
	{
		super.onStart(intent, startId);
		
		String action = null;
		if (intent != null)
			action = intent.getAction();
		
		if (action == null || ACTION_RESCHEDULE.equals(action))
		{
			if (intent == null)
			{
				if (App.getConfig().logToConsole())
					App.logv("SparkService/onStart: Intent is null, so rescheduling");
			}
			
			try
			{
				if (App.getConfig().logToConsole())
					App.logv("SparkService/onStart @ %s: reschedule",
							DateFormat.getTimeInstance().format(System.currentTimeMillis()));
				
				setupSchedules();
				
				reschedule();
				stopSelf(startId);
			}
			finally
			{
				releaseWakeLock(this);
			}
		}
		else if (ACTION_CANCEL.equals(action))
		{
			try
			{
				if (App.getConfig().logToConsole())
					App.logv("SparkService/onStart @ %s: cancel",
							DateFormat.getTimeInstance().format(System.currentTimeMillis()));
				
				cancel();
				stopSelf(startId);
			}
			finally
			{
				releaseWakeLock(this);
			}
		}
		else if (ACTION_UPDATE.equals(action))
		{
			try
			{
				final long accountId = intent.getLongExtra("checkId", -1);
				
				new Thread(new Runnable() 
				{
					@Override
					public void run()
					{
						try
						{
							if (App.getConfig().logToConsole())
								App.logv("SparkService/onStart @ %s: update",
										DateFormat.getTimeInstance().format(System.currentTimeMillis()));
							
							update(accountId);
							stopSelf(startId);
						}
						finally
						{
							releaseWakeLock(NotificationService.this);
						}
					}
				}).start();
			}
			catch(Exception e)
			{
				releaseWakeLock(NotificationService.this);
			}
		}
	}
	
	private void update(long accountId)
	{
		Preferences prefs = Preferences.get(this);
		BasicAccount account = prefs.getAccount(accountId);
		
		if (account == null)
		{
			App.logv("SparkService/update: error; account %d is NULL", accountId);
			return;
		}
		
		AccountSchedule schedule;
		ServiceClient client = account.createServiceClient();
		
		if (client != null && (schedule = mSchedules.get(account.getId())) != null)
		{
			client.update(account, schedule);
			
    		synchronized (mSchedules)
    		{
    			schedule.updateSyncTime(SystemClock.elapsedRealtime());
    		}
		}
		
		reschedule();
	}
	
	private void setupSchedules()
	{
		if (App.getConfig().logToConsole())
			App.logv("SparkService/setupSchedules");
		
		BasicAccount[] accounts = Preferences.get(this).getAccounts();
		
		synchronized(mSchedules)
		{
			mSchedules.clear();
			for (BasicAccount account : accounts)
			{
				if (account.isAutoSyncEnabled())
				{
					if (App.getConfig().logToConsole())
						App.logv("Creating a new account schedule for %s", 
								account.getScreenName());
					
					ServiceClient client = account.createServiceClient();
					if (client != null)
					{
						AccountSchedule schedule = new AccountSchedule(account, this);
						schedule.param = client.setupParameters(account);
						
						mSchedules.put(account.getId(), schedule);
					}
				}
			}
		}
	}
}
