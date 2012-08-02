/*
 * XboxLiveServiceClient.java 
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.akop.bach.Account;
import com.akop.bach.App;
import com.akop.bach.R;
import com.akop.bach.XboxLive;
import com.akop.bach.XboxLive.Friends;
import com.akop.bach.XboxLive.Messages;
import com.akop.bach.XboxLive.NotifyStates;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.parser.AuthenticationException;
import com.akop.bach.parser.ParserException;
import com.akop.bach.parser.XboxLiveParser;
import com.akop.bach.service.NotificationService.AccountSchedule;

public class XboxLiveServiceClient extends ServiceClient
{
	private static final int MAX_BEACONS = 5;
	
	private static class XboxLiveStatus
	{
		public List<Long> friendsOnline;
		public List<Long> unreadMessages;
		public HashMap<String, long[]> beacons;
		
		public XboxLiveStatus()
		{
			this.friendsOnline = new ArrayList<Long>();
			this.unreadMessages = new ArrayList<Long>();
			this.beacons = new HashMap<String, long[]>();
		}
	}
	
	private boolean areArraysEquivalent(long[] arrayOne, long[] arrayTwo)
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
	
	private void notifyMessages(XboxLiveAccount account, long[] unreadMessages,
			List<Long> lastUnreadList)
	{
		NotificationManager mgr = getNotificationManager();
		Context context = getContext();
		
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
					tickerTitle = context.getString(R.string.new_message);
					tickerText = context.getString(R.string.notify_message_pending_f, 
							account.getScreenName(), 
							account.getSender(context, unreadMessages[0])); 
				}
				else
				{
					tickerTitle = context.getString(R.string.new_messages);
					tickerText = context.getString(R.string.notify_messages_pending_f,
							account.getScreenName(), unreadMessages.length,
							account.getDescription(context));
				}
				
				Notification notification = new Notification(account.getMessageNotificationIconResource(),
						tickerText, System.currentTimeMillis());
				
		    	PendingIntent contentIntent = PendingIntent.getActivity(context, 
		    			0, account.getMessageListIntent(context), 0);
		    	
		    	notification.flags |= Notification.FLAG_AUTO_CANCEL 
		    			| Notification.FLAG_SHOW_LIGHTS;
		    	
		    	if (unreadMessages.length > 1)
		    		notification.number = unreadMessages.length;
		    	
		    	notification.ledOnMS = DEFAULT_LIGHTS_ON_MS;
		    	notification.ledOffMS = DEFAULT_LIGHTS_OFF_MS;
		    	notification.ledARGB = DEFAULT_LIGHTS_COLOR;
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
	
	private void notifyFriends(XboxLiveAccount account, long[] friendsOnline,
			List<Long> lastFriendsOnline)
	{
		NotificationManager mgr = getNotificationManager();
		Context context = getContext();
		
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
					tickerTitle = context.getString(R.string.friend_online);
					tickerText = context.getString(R.string.notify_friend_online_f,
							account.getFriendScreenName(context, friendsOnline[0]),
							account.getDescription(context)); 
				}
				else
				{
					tickerTitle = context.getString(R.string.friends_online);
					tickerText = context.getString(R.string.notify_friends_online_f,
							account.getScreenName(), friendsOnline.length,
							account.getDescription(context));
				}
				
				Notification notification = new Notification(account.getFriendNotificationIconResource(),
						tickerText, System.currentTimeMillis());
				notification.flags |= Notification.FLAG_AUTO_CANCEL;
				
		    	PendingIntent contentIntent = PendingIntent.getActivity(context, 
		    			0, account.getFriendListIntent(context), 0);
		    	
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
	
	private void notifyBeacons(XboxLiveAccount account, 
			HashMap<String, long[]> matching,
			HashMap<String, long[]> lastMatching)
	{
		Context context = getContext();
		NotificationManager mgr = getNotificationManager();
		
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
						!areArraysEquivalent(matchingFriends, lastMatching.get(gameUid)))
				{
					if (matchingFriends.length > 0)
					{
						String title = XboxLive.Games.getTitle(context, 
								account, gameUid);
						String message = null;
						String ticker = null;
						
						int iconOverlayNumber = 0;
						Intent intent = null;
						
						if (matchingFriends.length > 1)
						{
							iconOverlayNumber = matchingFriends.length;
							intent = account.getFriendListIntent(context);
							ticker = context.getString(R.string.friends_playing_beaconed_game_title_f,
									matchingFriends.length, title);
							message = context.getString(R.string.friends_playing_beaconed_game_f,
									matchingFriends.length, 
									account.getDescription(context));
						}
						else
						{
							String friendScreenName = account.getFriendScreenName(context, 
									matchingFriends[0]);
							
							intent = account.getFriendIntent(context, friendScreenName);
							ticker = context.getString(R.string.friend_playing_beaconed_game_title_f,
									friendScreenName, title);
							message = context.getString(R.string.friend_playing_beaconed_game_f,
									friendScreenName, 
									account.getDescription(context));
						}
						
						NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
						builder.setContentIntent(PendingIntent.getActivity(context, 0, intent, 0))
						        .setSmallIcon(R.drawable.xbox_stat_notify_beacon)
						        .setContentTitle(title)
						        .setContentText(message)
						        .setTicker(ticker)
						        .setWhen(System.currentTimeMillis())
						        .setAutoCancel(true)
						        .setOnlyAlertOnce(true)
						        .setNumber(iconOverlayNumber)
						        .setLights(DEFAULT_LIGHTS_COLOR, 
						        		DEFAULT_LIGHTS_ON_MS,
						        		DEFAULT_LIGHTS_OFF_MS)
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
	
	@Override
	protected void synchronize(Account account)
			throws IOException, ParserException, AuthenticationException	
	{
		Context context = getContext();
		XboxLiveParser p = new XboxLiveParser(context);
		XboxLiveAccount xblAccount = (XboxLiveAccount)account;
		
		try
		{
			p.fetchMessages(xblAccount);
			p.fetchFriends(xblAccount);
		}
		finally
		{
			p.dispose();
		}
	}
	
	@Override
	protected void notify(Account account, AccountSchedule schedule) 
	{
		Context context = getContext();
		XboxLiveAccount xblAccount = (XboxLiveAccount)account;
		XboxLiveStatus status = (XboxLiveStatus)schedule.param;
		
		long[] newUnreadMessages = null;
		long[] newFriendsOnline = null;
		
		// Messages
		
		try
		{
			newUnreadMessages = Messages.getUnreadMessageIds(context, 
					xblAccount);
			
			if (xblAccount.isMessageNotificationEnabled())
			{
				notifyMessages(xblAccount, newUnreadMessages, 
						status.unreadMessages);
				
				NotifyStates.setMessagesLastNotified(context, 
						xblAccount, newUnreadMessages);
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
		
		// Friends
		
		try
		{
			newFriendsOnline = Friends.getOnlineFriendIds(context, 
					xblAccount);
			
			if (xblAccount.isFriendNotificationEnabled())
			{
				notifyFriends(xblAccount, newFriendsOnline, 
						status.friendsOnline);
				
				NotifyStates.setFriendsLastNotified(context, xblAccount, 
						newFriendsOnline);
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
		
		// Beacons
		
		HashMap<String, long[]> matching = null;
		
		try
		{
			matching = Friends.getOnlineFriendsMatchingMyBeacons(context, 
					xblAccount, xblAccount.getBeaconNotifications());
			
			if (xblAccount.isBeaconNotificationEnabled())
			{
				notifyBeacons(xblAccount, matching, status.beacons);
				
				NotifyStates.setBeaconsLastNotified(context, xblAccount, 
						matching);
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
		
		status.unreadMessages.clear();
		if (newUnreadMessages != null)
			for (long o : newUnreadMessages)
				status.unreadMessages.add(o);
		
		status.friendsOnline.clear();
		if (newFriendsOnline != null)
			for (long o : newFriendsOnline)
				status.friendsOnline.add(o);
		
		status.beacons.clear();
		if (matching != null)
			for (String gameUid: matching.keySet())
				status.beacons.put(gameUid, matching.get(gameUid));
	}
	
	@Override
	public Object setupParameters(Account account)
	{
		Context context = getContext();
		XboxLiveAccount xblAccount = (XboxLiveAccount)account;
		XboxLiveStatus status = new XboxLiveStatus();
		
		if (App.LOGV)
			App.logv("Creating a new account schedule for %s", 
					account.getScreenName());
		
		if (xblAccount.isMessageNotificationEnabled())
		{
			// Load last state
			long[] lastNotified = NotifyStates.getMessagesLastNotified(context, 
					xblAccount);
			
			for (long id : lastNotified)
				status.unreadMessages.add(id);
		}
		
		if (xblAccount.isFriendNotificationEnabled())
		{
			// Load last state
			long[] lastNotified = xblAccount.getFriendsLastNotified(context);
			for (long id : lastNotified)
				status.friendsOnline.add(id);
		}
		
		if (xblAccount.isBeaconNotificationEnabled())
		{
			HashMap<String, long[]> map = NotifyStates.getBeaconsLastNotified(context, 
					xblAccount);
			
			for (String gameId : map.keySet())
			{
				long[] friendIds = (long[])map.get(gameId);
				status.beacons.put(gameId, friendIds);
			}
		}
		
		return status;
	}
	
	public static void clearMessageNotifications(Context context,
			XboxLiveAccount account)
	{
		int notificationId = 0x1000000 | ((int)account.getId() & 0xffffff);
		NotificationManager mgr = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    	mgr.cancel(notificationId);
	}
	
	public static void clearFriendNotifications(Context context,
			XboxLiveAccount account)
	{
		int notificationId = 0x2000000 | ((int)account.getId() & 0xffffff);
		NotificationManager mgr = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    	mgr.cancel(notificationId);
	}
}