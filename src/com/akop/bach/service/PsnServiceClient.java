/*
 * PsnServiceClient.java 
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.akop.bach.BasicAccount;
import com.akop.bach.App;
import com.akop.bach.PSN.Friends;
import com.akop.bach.PSN.NotifyStates;
import com.akop.bach.PsnAccount;
import com.akop.bach.R;
import com.akop.bach.activity.playstation.FriendList;
import com.akop.bach.parser.AuthenticationException;
import com.akop.bach.parser.ParserException;
import com.akop.bach.parser.PsnEuParser;
import com.akop.bach.parser.PsnParser;
import com.akop.bach.service.NotificationService.AccountSchedule;

public class PsnServiceClient extends ServiceClient
{
	private static class PsnStatus
	{
		public List<Long> friendsOnline;
		
		public PsnStatus()
		{
			this.friendsOnline = new ArrayList<Long>();
		}
	}
	
	private void notifyFriends(PsnAccount account, long[] friendsOnline,
			List<Long> lastFriendsOnline)
	{
		NotificationManager mgr = getNotificationManager();
		Context context = getContext();
		
		int notificationId = 0x8000000 | ((int)account.getId() & 0xffffff);
		
		if (App.getConfig().logToConsole())
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
			
			if (App.getConfig().logToConsole())
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
					tickerTitle = context.getString(R.string.friend_online);
					tickerText = context.getString(R.string.notify_friend_online_f,
							Friends.getOnlineId(context, friendsOnline[0]),
							account.getDescription()); 
				}
				else
				{
					tickerTitle = context.getString(R.string.friends_online);
					tickerText = context.getString(R.string.notify_friends_online_f,
							account.getScreenName(), friendsOnline.length,
							account.getDescription());
				}
				
				Notification notification = new Notification(R.drawable.psn_stat_notify_friend,
						tickerText, System.currentTimeMillis());
				notification.flags |= Notification.FLAG_AUTO_CANCEL;
				
				Intent intent = new Intent(context, FriendList.class);
				intent.putExtra("account", account);
				
		    	PendingIntent contentIntent = PendingIntent.getActivity(context, 
		    			0, intent, 0);
		    	
				notification.setLatestEventInfo(context, tickerTitle,
						tickerText, contentIntent);
		    	
				// New users online
				
		    	if (friendsOnline.length > 1)
		    		notification.number = friendsOnline.length;
		    	
		    	notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		    	notification.ledOnMS = DEFAULT_LIGHTS_ON_MS;
		    	notification.ledOffMS = DEFAULT_LIGHTS_OFF_MS;
		    	notification.ledARGB = DEFAULT_LIGHTS_COLOR;
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
	
	@Override
	public Object setupParameters(BasicAccount account) 
	{
		Context context = getContext();
		PsnAccount psnAccount = (PsnAccount)account;
		PsnStatus status = new PsnStatus();
		
		if (App.getConfig().logToConsole())
			App.logv("Creating a new account schedule for %s", 
					account.getScreenName());
		
		if (psnAccount.getFriendNotifications() != PsnAccount.FRIEND_NOTIFY_OFF)
		{
			// Load last state
			long[] lastNotified = NotifyStates.getFriendsLastNotified(context, 
					psnAccount);
			
			for (long id : lastNotified)
				status.friendsOnline.add(id);
		}
		
		return status;
	}

	@Override
	protected void synchronize(BasicAccount account)
			throws IOException, ParserException, AuthenticationException
	{
		Context context = getContext();
		PsnAccount psnAccount = (PsnAccount)account;
		PsnParser p = new PsnEuParser(context);
		
		try
		{
			p.fetchFriends(psnAccount);
		}
		finally
		{
			p.dispose();
		}
	}
	
	@Override
	protected void notify(BasicAccount account, AccountSchedule schedule) 
	{
		Context context = getContext();
		PsnAccount psnAccount = (PsnAccount)account;
		PsnStatus status = (PsnStatus)schedule.param;
		
		long[] newFriendsOnline = null;
		
		// Friends
		
		try
		{
			newFriendsOnline = Friends.getOnlineFriendIds(context, 
					psnAccount);
			
			if (psnAccount.getFriendNotifications() != PsnAccount.FRIEND_NOTIFY_OFF)
			{
				notifyFriends(psnAccount, newFriendsOnline, 
						status.friendsOnline);
				
				NotifyStates.setFriendsLastNotified(context, psnAccount, 
						newFriendsOnline);
			}
		}
		catch(Exception e)
		{
			if (App.getConfig().logToConsole())
			{
				App.logv("Suppressed exception");
				e.printStackTrace();
			}
		}
		
		status.friendsOnline.clear();
		if (newFriendsOnline != null)
			for (long o : newFriendsOnline)
				status.friendsOnline.add(o);
	}
	
	public static void clearFriendNotifications(Context context,
			PsnAccount account)
	{
		int notificationId = 0x8000000 | ((int)account.getId() & 0xffffff);
		NotificationManager mgr = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    	mgr.cancel(notificationId);
	}
}