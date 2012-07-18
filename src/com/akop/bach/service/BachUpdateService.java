/*
 * BachUpdateService.java 
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

import java.util.Arrays;
import java.util.HashMap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.akop.bach.Account;
import com.akop.bach.App;
import com.akop.bach.R;
import com.akop.bach.SupportsNotifications;
import com.akop.bach.XboxLive;
import com.akop.bach.XboxLive.Friends;
import com.akop.bach.XboxLive.NotifyStates;
import com.akop.bach.XboxLiveAccount;

public class BachUpdateService extends UpdateService
{
	private static final int MAX_BEACONS = 5;
	
	private static final String BACH_ACTION_UPDATE = "com.akop.bach.intent.action.SERVICE_UPDATE";
	private static final String BACH_ACTION_RESCHEDULE = "com.akop.bach.intent.action.SERVICE_RESCHEDULE";
	private static final String BACH_ACTION_CANCEL = "com.akop.bach.intent.action.SERVICE_CANCEL";
	
	protected static final int LIGHTS_COLOR = 0xffff9d0c;
	
	protected static class BachAccountSchedule extends AccountSchedule
	{
		public HashMap<String, long[]> beacons;
		
		public BachAccountSchedule(Account account, Context context)
		{
			super(account, context);
			
			this.beacons = new HashMap<String, long[]>();
		}
		
		@Override
		public String toString() 
		{
			return super.toString();
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
			intent.setClass(context, BachUpdateService.class);
			intent.setAction(BACH_ACTION_RESCHEDULE);
			
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
			intent.setClass(context, BachUpdateService.class);
			intent.setAction(BACH_ACTION_UPDATE);
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
	protected PendingIntent createAlarmIntent(long checkId)
	{
		Intent intent = new Intent(this, BachUpdateAlarmReceiver.class);
		intent.putExtra("checkId", checkId);
		
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, 
				PendingIntent.FLAG_UPDATE_CURRENT);
		
		return pi;
	}
	
	private boolean areArraysEqual(long[] arrayOne, long[] arrayTwo)
	{
		if (arrayOne == null || arrayTwo == null)
			return arrayOne == arrayTwo;
		
		if (arrayOne.length != arrayTwo.length)
			return false;
		
		Arrays.sort(arrayTwo);
		for (int i = 0, n = arrayOne.length; i < n; i++)
			if (Arrays.binarySearch(arrayTwo, arrayOne[i]) < 0)
				return false;
		
		return true;
	}
	
	private void notifyBeacons(NotificationManager mgr,
			XboxLiveAccount account, HashMap<String, long[]> matching,
			HashMap<String, long[]> lastMatching)
	{
		if (App.LOGV)
		{
			if (lastMatching != null && lastMatching.size() > 0)
			{
				App.logv("Last matching:");
				
				for (String key : lastMatching.keySet())
				{
					String s = "* BEACONS " + key + ": ";
					for (long friendId : lastMatching.get(key))
						s += friendId + ",";
					
					App.logv(s);
				}
			}
			
			if (matching.size() > 0)
			{
				App.logv("Now matching:");
				
				for (String key : matching.keySet())
				{
					String s = "* BEACONS " + key + ": ";
					for (long friendId : matching.get(key))
						s += friendId + ",";
					
					App.logv(s);
				}
			}
		}
		
		int notificationId = 0x40000 | (((int)account.getId() & 0xfff) << 4);
		String[] gameUids = new String[matching.keySet().size()];
		
		matching.keySet().toArray(gameUids);
		
		for (int i = 0; i < MAX_BEACONS; i++)
		{
			if (i < gameUids.length)
			{
				String gameUid = gameUids[i];
				long[] matchingFriends = matching.get(gameUid);
				
				if (!lastMatching.containsKey(gameUid) || 
						!areArraysEqual(matchingFriends, lastMatching.get(gameUid)))
				{
					if (matchingFriends.length > 0)
					{
						String title = XboxLive.Games.getTitle(this, account, gameUid);
						String message = null;
						String ticker = null;
						
						int iconOverlayNumber = 0;
						Intent intent = null;
						
						if (matchingFriends.length > 1)
						{
							iconOverlayNumber = matchingFriends.length;
							intent = account.getFriendListIntent(this);
							ticker = getString(R.string.friends_playing_beaconed_game_title_f,
									matchingFriends.length, title);
							message = getString(R.string.friends_playing_beaconed_game_f,
									matchingFriends.length, account.getDescription(this));
						}
						else
						{
							String friendScreenName = account.getFriendScreenName(this, 
									matchingFriends[0]);
							
							intent = account.getFriendIntent(this, friendScreenName);
							ticker = getString(R.string.friend_playing_beaconed_game_title_f,
									friendScreenName, title);
							message = getString(R.string.friend_playing_beaconed_game_f,
									friendScreenName, account.getDescription(this));
						}
						
						NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
						builder.setContentIntent(PendingIntent.getActivity(this, 0, intent, 0))
						        .setSmallIcon(R.drawable.xbox_stat_notify_beacon)
						        .setContentTitle(title)
						        .setContentText(message)
						        .setTicker(ticker)
						        .setWhen(System.currentTimeMillis())
						        .setAutoCancel(true)
						        .setOnlyAlertOnce(true)
						        .setNumber(iconOverlayNumber)
						        .setLights(LIGHTS_COLOR, LIGHTS_ON_MS,
						                LIGHTS_OFF_MS)
						        .setSound(account.getRingtoneUri());
						
				    	if (account.isVibrationEnabled())
				    		builder.setDefaults(Notification.DEFAULT_VIBRATE);
				    	
				    	Notification notification = builder.getNotification();
				    	mgr.notify(notificationId, notification);
					}
					else
					{
						mgr.cancel(notificationId);
					}
				}
			}
			else
			{
				mgr.cancel(notificationId);
			}
			
			notificationId++;
		}
	}
	
	protected AccountSchedule createAccountSchedule(Account account)
	{
		return new BachAccountSchedule(account, this);
	}
	
	@Override
	protected String getRescheduleAction() 
	{
		return BACH_ACTION_RESCHEDULE;
	}
	
	@Override
	protected String getCancelAction() 
	{
		return BACH_ACTION_CANCEL;
	}
	
	@Override
	protected String getUpdateAction() 
	{
		return BACH_ACTION_UPDATE;
	}
	
	@Override
	protected void setupCustomSchedules(Context context, Account account, AccountSchedule schedule)
	{
		super.setupCustomSchedules(context, account, schedule);
		
		BachAccountSchedule bachSchedule = (BachAccountSchedule)schedule;
		XboxLiveAccount xboxLiveAccount = (XboxLiveAccount)account;
		
		bachSchedule.beacons.clear();
		
		if (xboxLiveAccount.isBeaconNotificationEnabled())
		{
			HashMap<String, long[]> map = XboxLive.NotifyStates.getBeaconsLastNotified(context, 
					xboxLiveAccount);
			
			for (String gameId : map.keySet())
			{
				long[] friendIds = (long[])map.get(gameId);
				bachSchedule.beacons.put(gameId, friendIds);
			}
		}
	}
	
	@Override
	protected Object performCustomNotifications(SupportsNotifications notif, 
			AccountSchedule schedule, NotificationManager mgr)
	{
		super.performCustomNotifications(notif, schedule, mgr);
		
		HashMap<String, long[]> matching = null;
		BachAccountSchedule bachSchedule = (BachAccountSchedule)schedule;  
		XboxLiveAccount xboxLiveAccount = (XboxLiveAccount)notif;
		
		if (xboxLiveAccount.isBeaconNotificationEnabled())
		{
			matching = Friends.getOnlineFriendsMatchingMyBeacons(this, 
					xboxLiveAccount, xboxLiveAccount.getBeaconNotifications());
			
			notifyBeacons(mgr, xboxLiveAccount, 
					matching, bachSchedule.beacons);
			
			NotifyStates.setBeaconsLastNotified(this, xboxLiveAccount, matching);
		}
		
		return matching;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void resetCustomSchedules(AccountSchedule schedule,
			Object customNotificationParameter) 
	{
		super.resetCustomSchedules(schedule, customNotificationParameter);
		
		BachAccountSchedule bachSchedule = (BachAccountSchedule)schedule;
        HashMap<String, long[]> matching = (HashMap<String, long[]>)customNotificationParameter; 
		
		bachSchedule.beacons.clear();
		if (matching != null)
			for (String gameUid: matching.keySet())
				bachSchedule.beacons.put(gameUid, matching.get(gameUid));
	}
	
	public static void clearBeaconNotifications(Context context,
			XboxLiveAccount account)
	{
		int notificationId = 0x40000 | (((int)account.getId() & 0xfff) << 4);
		
		NotificationManager mgr = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    	
		synchronized(mSchedules)
		{
			for (int i = 0; i < MAX_BEACONS; i++)
			{
		    	mgr.cancel(notificationId);
				notificationId++;
			}
			
			if (!mSchedules.containsKey(account.getId()))
				return; // Not set to autosync
			
			if (!account.isBeaconNotificationEnabled())
				return;
			
			BachAccountSchedule bachSchedule = (BachAccountSchedule)mSchedules.get(account.getId());
			
			bachSchedule.beacons.clear();
			
			HashMap<String, long[]> map = XboxLive.NotifyStates.getBeaconsLastNotified(context, 
					account);
			
			for (String gameId : map.keySet())
			{
				long[] friendIds = (long[])map.get(gameId);
				bachSchedule.beacons.put(gameId, friendIds);
			}
			
			if (App.LOGV)
			{
				if (map.size() > 0)
				{
					App.logv("Cleared beacon notifications for %s:", account.getScreenName());
					
					for (String key : map.keySet())
					{
						String s = "* BEACONS " + key + ": ";
						for (long friendId : map.get(key))
							s += friendId + ",";
						
						App.logv(s);
					}
				}
			}
		}
	}
	
}