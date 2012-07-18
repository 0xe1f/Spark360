/*
 * UpdateService.java
 * Copyright (C) 2010-2012 Akop Karapetyan
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.format.DateUtils;

import com.akop.bach.Account;
import com.akop.bach.App;
import com.akop.bach.Preferences;
import com.akop.bach.R;
import com.akop.bach.SupportsFriendNotifications;
import com.akop.bach.SupportsMessageNotifications;
import com.akop.bach.SupportsNotifications;

public abstract class UpdateService extends Service
{
	private static final String ACTION_UPDATE = "com.akop.spark.intent.action.SPARK_SERVICE_UPDATE";
	private static final String ACTION_RESCHEDULE = "com.akop.spark.intent.action.SPARK_SERVICE_RESCHEDULE";
	private static final String ACTION_CANCEL = "com.akop.spark.intent.action.SPARK_SERVICE_CANCEL";
	
	protected static final int LIGHTS_ON_MS = 3000;
	protected static final int LIGHTS_OFF_MS = 10000;
	protected static final int LIGHTS_COLOR = 0xffffffff;
	
	private static PowerManager.WakeLock mWl;
	
	protected static class AccountSchedule
	{
		private String description;
		public long accountId;
		public long lastSyncMs;
		public long nextSyncMs;
		public long syncFreqMs;
		public List<Long> unreadMessages;
		public List<Long> friendsOnline;
		
		public AccountSchedule(Account account, Context context)
		{
			this.unreadMessages = new ArrayList<Long>();
			this.friendsOnline = new ArrayList<Long>();
			this.description = account.getScreenName() + 
					" (" + account.getDescription(context) + ")";
			this.accountId = account.getId();
			this.lastSyncMs = 0;
			this.nextSyncMs = 0;
			this.syncFreqMs = account.getSyncPeriod() * DateUtils.MINUTE_IN_MILLIS;
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
			if (App.LOGV)
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
			if (App.LOGV)
				App.logv(" -------- RELEASE WAKE LOCK -------- ");
			
			if (mWl.isHeld())
				mWl.release();
			
			mWl = null;
		}
	}
	
	public static void actionReschedule(Context context)
	{
		if (!App.ENABLE_SERVICES)
			return;
		
		acquireWakeLock(context);
		
		try
		{
			Intent intent = new Intent();
			intent.setClass(context, UpdateService.class);
			intent.setAction(UpdateService.ACTION_RESCHEDULE);
			
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
		if (!App.ENABLE_SERVICES)
			return;
		
		acquireWakeLock(context);
		
		try
		{
			Intent intent = new Intent();
			intent.setClass(context, UpdateService.class);
			intent.setAction(UpdateService.ACTION_UPDATE);
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
    
	protected String getUpdateAction()
	{
		return ACTION_UPDATE;
	}
	
	protected String getRescheduleAction()
	{
		return ACTION_RESCHEDULE;
	}
	
	protected String getCancelAction()
	{
		return ACTION_CANCEL;
	}
	
	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
	
	protected abstract PendingIntent createAlarmIntent(long checkId);
	
	private void notifyMessages(NotificationManager mgr,
			SupportsMessageNotifications account, long[] unreadMessages,
			List<Long> lastUnreadList)
	{
		int notificationId = 0x1000000 | ((int)account.getId() & 0xffffff);
		
		if (App.LOGV)
		{
			String s = "";
			for (Object unr : lastUnreadList)
				s += unr.toString() + ",";
			
			App.logv("Currently unread (%d): %s", lastUnreadList.size(), s);
			
			s = "";
			for (Object unr : unreadMessages)
				s += unr.toString() + ",";
			
			App.logv("New unread (%d): %s", unreadMessages.length, s);
		}
		
		if (unreadMessages.length > 0)
		{
			int unreadCount = 0;
			for (Object unread : unreadMessages)
			{
				if (!lastUnreadList.contains(unread))
					unreadCount++;
			}
			
			if (App.LOGV)
				App.logv("%d computed new", unreadCount);
			
			if (unreadCount > 0)
			{
				String tickerTitle;
				String tickerText;
				
				if (unreadMessages.length == 1)
				{
					tickerTitle = getString(R.string.new_message);
					tickerText = getString(R.string.notify_message_pending_f, 
							account.getScreenName(), 
							account.getSender(this, unreadMessages[0])); 
				}
				else
				{
					tickerTitle = getString(R.string.new_messages);
					tickerText = getString(R.string.notify_messages_pending_f,
							account.getScreenName(), unreadMessages.length,
							account.getDescription(this));
				}
				
				Notification notification = new Notification(account.getMessageNotificationIconResource(),
						tickerText, System.currentTimeMillis());
				Context context = getApplicationContext();
				
		    	PendingIntent contentIntent = PendingIntent.getActivity(UpdateService.this, 0, 
		    			account.getMessageListIntent(UpdateService.this), 0);
		    	
		    	notification.flags |= Notification.FLAG_AUTO_CANCEL 
		    			| Notification.FLAG_SHOW_LIGHTS;
		    	
		    	if (unreadMessages.length > 1)
		    		notification.number = unreadMessages.length;
		    	
		    	notification.ledOnMS = LIGHTS_ON_MS;
		    	notification.ledOffMS = LIGHTS_OFF_MS;
		    	notification.ledARGB = LIGHTS_COLOR;
		    	notification.sound = account.getRingtoneUri();
		    	
		    	if (account.isVibrationEnabled())
		    		notification.defaults |= Notification.DEFAULT_VIBRATE;
		    	
				notification.setLatestEventInfo(context, tickerTitle,
						tickerText, contentIntent);
		    	
		    	mgr.notify(notificationId, notification);
			}
		}
		else // No unread messages
		{
	    	mgr.cancel(notificationId);
		}
	}
	
	private void notifyFriends(NotificationManager mgr,
			SupportsFriendNotifications account, long[] friendsOnline,
			List<Long> lastFriendsOnline)
	{
		int notificationId = 0x2000000 | ((int)account.getId() & 0xffffff);
		
		if (App.LOGV)
		{
			String s = "";
			for (Object unr : lastFriendsOnline)
				s += unr.toString() + ",";
			
			App.logv("Currently online (%d): %s", lastFriendsOnline.size(), s);
			
			s = "";
			for (Object unr : friendsOnline)
				s += unr.toString() + ",";
			
			App.logv("New online (%d): %s", friendsOnline.length, s);
		}
		
		if (friendsOnline.length > 0)
		{
			int newOnlineCount = 0;
			for (Object online : friendsOnline)
				if (!lastFriendsOnline.contains(online))
					newOnlineCount++;
			
			if (App.LOGV)
				App.logv("%d computed new; %d online now; %d online before",
						newOnlineCount, friendsOnline.length,
						lastFriendsOnline.size());
			
			if (newOnlineCount > 0)
			{
				// Prepare notification objects
				String tickerTitle;
				String tickerText;
				
				if (friendsOnline.length == 1)
				{
					tickerTitle = getString(R.string.friend_online);
					tickerText = getString(R.string.notify_friend_online_f,
							account.getFriendScreenName(this, friendsOnline[0]),
							account.getDescription(this)); 
				}
				else
				{
					tickerTitle = getString(R.string.friends_online);
					tickerText = getString(R.string.notify_friends_online_f,
							account.getScreenName(), friendsOnline.length,
							account.getDescription(this));
				}
				
				Notification notification = new Notification(account.getFriendNotificationIconResource(),
						tickerText, System.currentTimeMillis());
				notification.flags |= Notification.FLAG_AUTO_CANCEL;
				
				Context context = getApplicationContext();
		    	PendingIntent contentIntent = PendingIntent.getActivity(UpdateService.this, 0, 
		    			account.getFriendListIntent(UpdateService.this), 0);
		    	
				notification.setLatestEventInfo(context, tickerTitle,
						tickerText, contentIntent);
		    	
				// New users online
				
		    	if (friendsOnline.length > 1)
		    		notification.number = friendsOnline.length;
		    	
		    	notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		    	notification.ledOnMS = LIGHTS_ON_MS;
		    	notification.ledOffMS = LIGHTS_OFF_MS;
		    	notification.ledARGB = LIGHTS_COLOR;
		    	notification.sound = account.getRingtoneUri();
		    	
		    	if (account.isVibrationEnabled())
		    		notification.defaults |= Notification.DEFAULT_VIBRATE;
				
		    	mgr.notify(notificationId, notification);
			}
		}
		else // No unread messages
		{
	    	mgr.cancel(notificationId);
		}
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
		
		if (App.LOGV)
			App.logv("SparkService/reschedule: next up is %s", nextAccount.toString());
		
		// Create an update alarm
		AlarmManager alarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		PendingIntent pi = createAlarmIntent(nextAccount.accountId);
		alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextCheckTime, pi);
	}
	
	private void cancel()
	{
		if (App.LOGV)
			App.logv("SparkService/cancel: canceling alarms");
		
		AlarmManager alarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		PendingIntent pi = createAlarmIntent(-1);
		alarmMgr.cancel(pi);
	}
	
	private void update(long accountId)
	{
		Preferences prefs = Preferences.get(this);
		Account account = prefs.getAccount(accountId);
		
		if (account == null)
		{
			App.logv("SparkService/update: error; account %d is NULL", accountId);
			return;
		}
		
    	if (account instanceof SupportsNotifications)
    	{
    		SupportsNotifications notif = (SupportsNotifications)account;
    		
    		if (App.LOGV)
    			App.logv("SparkService/update: %s (%s); now: %s",
    					notif.getScreenName(),
    					notif.getDescription(this),
    					DateFormat.getTimeInstance().format(System.currentTimeMillis()));
    		
    		AccountSchedule schedule = mSchedules.get(notif.getId());
    		long[] newUnreadMessages = null;
    		long[] newFriendsOnline = null;
    		
    		if (schedule != null)
    		{
    			NotificationManager mgr = 
    				(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
    			
    			try
    			{
    				notif.doBackgroundSynch(this);
    				
    				if (notif instanceof SupportsMessageNotifications)
    				{
						SupportsMessageNotifications smnAccount = 
							(SupportsMessageNotifications)notif;
						
						newUnreadMessages = smnAccount
								.getUnreadMessageIds(UpdateService.this);
	    				
						if (smnAccount.isMessageNotificationEnabled())
						{
							notifyMessages(mgr, smnAccount,
									newUnreadMessages, schedule.unreadMessages);
							
							smnAccount.setMessagesLastNotified(getApplicationContext(), 
									newUnreadMessages);
						}
    				}
    			}
    			catch(Exception e)
    			{
    				if (App.LOGV)
    				{
    					App.logv("Suppressed exception");
    					e.printStackTrace();
    				}
    			}
    			
    			try
    			{
    				if (notif instanceof SupportsFriendNotifications)
    				{
    					SupportsFriendNotifications sfnAccount = 
							(SupportsFriendNotifications)notif;
						
    					newFriendsOnline = sfnAccount.getOnlineFriendIds(UpdateService.this);
	    				
						if (sfnAccount.isFriendNotificationEnabled())
						{
							notifyFriends(mgr, sfnAccount,
									newFriendsOnline, schedule.friendsOnline);
							
							sfnAccount.setFriendsLastNotified(getApplicationContext(), 
									newFriendsOnline);
						}
    				}
    			}
    			catch(Exception e)
    			{
    				if (App.LOGV)
    				{
    					App.logv("Suppressed exception");
    					e.printStackTrace();
    				}
    			}
    			
    			Object customNotificationParameter = null;
    			
    			try
    			{
    				customNotificationParameter = performCustomNotifications(notif, schedule, mgr);
    			}
    			catch(Exception e)
    			{
    				if (App.LOGV)
    				{
    					App.logv("Suppressed exception");
    					e.printStackTrace();
    				}
    			}
    			
    			synchronized(mSchedules)
    			{
    				schedule.updateSyncTime(SystemClock.elapsedRealtime());
    				
					schedule.unreadMessages.clear();
    				if (newUnreadMessages != null)
    					for (long o : newUnreadMessages)
    						schedule.unreadMessages.add(o);
    				
					schedule.friendsOnline.clear();
    				if (newFriendsOnline != null)
    					for (long o : newFriendsOnline)
    						schedule.friendsOnline.add(o);
    				
    				resetCustomSchedules(schedule, customNotificationParameter);
    			}
    		}
    	}
    	
		reschedule();
	}
	
	private void setupSchedules()
	{
		if (App.LOGV)
			App.logv("SparkService/setupSchedules");
		
		Account[] accounts = Preferences.get(this).getAccounts();
		Context context = getApplicationContext();
		
		synchronized(mSchedules)
		{
			mSchedules.clear();
			for (Account account : accounts)
			{
				if (account.isAutoSyncEnabled())
				{
					if (App.LOGV)
						App.logv("Creating a new account schedule for %s", 
								account.getScreenName());
					
					AccountSchedule schedule = createAccountSchedule(account);
					if (account instanceof SupportsMessageNotifications)
					{
						SupportsMessageNotifications smnAccount = 
							(SupportsMessageNotifications)account;
						
						if (smnAccount.isMessageNotificationEnabled())
						{
							// Load last state
							long[] lastNotified = smnAccount.getMessagesLastNotified(context);
							for (long id : lastNotified)
								schedule.unreadMessages.add(id);
						}
					}
					
					if (account instanceof SupportsFriendNotifications)
					{
						SupportsFriendNotifications sfnAccount = 
							(SupportsFriendNotifications)account;
						
						if (sfnAccount.isFriendNotificationEnabled())
						{
							// Load last state
							long[] lastNotified = sfnAccount.getFriendsLastNotified(context);
							for (long id : lastNotified)
								schedule.friendsOnline.add(id);
						}
					}
					
					setupCustomSchedules(context, account, schedule);
					
					mSchedules.put(account.getId(), schedule);
				}
			}
		}
	}
	
	protected Object performCustomNotifications(SupportsNotifications notif, 
			AccountSchedule schedule, NotificationManager mgr)
	{
		// Default impl. does nothing
		
		return null;
	}
	
	protected void setupCustomSchedules(Context context, Account account, AccountSchedule schedule)
	{
		// Default impl. does nothing
	}
	
	protected void resetCustomSchedules(AccountSchedule schedule, Object customNotificationParameter)
	{
		// Default impl. does nothing
	}
	
	protected AccountSchedule createAccountSchedule(Account account)
	{
		return new AccountSchedule(account, this);
	}
	
	public static void clearMessageNotifications(Context context,
			SupportsMessageNotifications account)
	{
		int notificationId = 0x1000000 | ((int)account.getId() & 0xffffff);
		NotificationManager mgr = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    	mgr.cancel(notificationId);
    	
		synchronized(mSchedules)
		{
			if (!mSchedules.containsKey(account.getId()))
				return; // Not set to autosync
			
			if (!account.isMessageNotificationEnabled())
				return;
			
			AccountSchedule schedule = mSchedules.get(account.getId());
			long[] unreadMessages = account.getUnreadMessageIds(context);
			
			schedule.unreadMessages.clear();
			for (long o : unreadMessages)
				schedule.unreadMessages.add(o);
			
			if (App.LOGV)
			{
				String s = "";
				for (Object o : unreadMessages)
					s += o.toString() + ",";
				
				App.logv("Modifying schedule for %s; new list of unread messages: %s",
						account.getScreenName(), s);
			}
		}
	}
	
	public static void clearFriendNotifications(Context context,
			SupportsFriendNotifications account)
	{
		int notificationId = 0x2000000 | ((int)account.getId() & 0xffffff);
		NotificationManager mgr = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    	mgr.cancel(notificationId);
    	
		synchronized(mSchedules)
		{
			if (!mSchedules.containsKey(account.getId()))
				return; // Not set to autosync
			
			if (!account.isFriendNotificationEnabled())
				return;
			
			AccountSchedule schedule = mSchedules.get(account.getId());
			long[] friendsOnline = account.getOnlineFriendIds(context);
			
			schedule.friendsOnline.clear();
			for (long o : friendsOnline)
				schedule.friendsOnline.add(o);
			
			if (App.LOGV)
			{
				String s = "";
				for (Object o : friendsOnline)
					s += o.toString() + ",";
				
				App.logv("Modifying schedule for %s; new list of friends online: %s",
						account.getScreenName(), s);
			}
		}
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
	
	private void handleStart(Intent intent, final int startId)
	{
		super.onStart(intent, startId);
		
		String action = null;
		if (intent != null)
			action = intent.getAction();
		
		if (action == null || getRescheduleAction().equals(action))
		{
			if (intent == null)
			{
				if (App.LOGV)
					App.logv("SparkService/onStart: Intent is null, so rescheduling");
			}
			
			try
			{
				if (App.LOGV)
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
		else if (getCancelAction().equals(action))
		{
			try
			{
				if (App.LOGV)
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
		else if (getUpdateAction().equals(action))
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
							if (App.LOGV)
								App.logv("SparkService/onStart @ %s: update",
										DateFormat.getTimeInstance().format(System.currentTimeMillis()));
							
							update(accountId);
							stopSelf(startId);
						}
						finally
						{
							releaseWakeLock(UpdateService.this);
						}
					}
				}).start();
			}
			catch(Exception e)
			{
				releaseWakeLock(UpdateService.this);
			}
		}
	}
}
