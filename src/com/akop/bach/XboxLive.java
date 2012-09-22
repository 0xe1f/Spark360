/*
 * XboxLive.java 
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

package com.akop.bach;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.text.format.DateUtils;

import com.akop.bach.parser.Parser;
import com.akop.bach.provider.XboxLiveProvider;

public class XboxLive
{
	public static final int STATUS_INVITE_SENT = 1;
	public static final int STATUS_INVITE_RCVD = 2;
	public static final int STATUS_NOT_YET = 3;
	public static final int STATUS_ONLINE  = 4;
	public static final int STATUS_AWAY    = 5;
	public static final int STATUS_OFFLINE = 6;
	public static final int STATUS_OTHER   = 99;
	
	public static final int MESSAGE_OTHER = 0;
	public static final int MESSAGE_TEXT =  1;
	public static final int MESSAGE_VOICE = 2;
	
	public static final int LIVE_STATUS_OK = 1;
	public static final int LIVE_STATUS_ERROR = 2;
	
	public static final class Profiles implements BaseColumns
	{
		private Profiles() {}
		
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ XboxLiveProvider.AUTHORITY + "/profiles");
		
		public static final String CONTENT_TYPE = 
			"vnd.android.cursor.dir/vnd.akop.spark.xbl-profile";
		public static final String CONTENT_ITEM_TYPE = 
			"vnd.android.cursor.item/vnd.akop.spark.xbl-profile";
		
		public static final String ACCOUNT_ID = "AccountId";
		public static final String UUID = "Uuid";
		public static final String GAMERTAG = "Gamertag";
		public static final String ICON_URL = "IconUrl";
		public static final String REP = "Rep";
		public static final String ZONE = "Zone";
		public static final String GAMERSCORE = "Gamerscore";
		public static final String IS_GOLD = "IsGold";
		public static final String TIER = "Tier";
		public static final String POINTS_BALANCE = "PointsBalance";
		public static final String UNREAD_MESSAGES = "UnreadMessages";
		public static final String UNREAD_NOTIFICATIONS = "UnreadNotifications";
		public static final String MOTTO = "Motto";
		public static final String NAME = "Name";
		public static final String BIO = "Bio";
		public static final String LOCATION = "Location";
		
		public static final String DEFAULT_SORT_ORDER = GAMERTAG + " ASC";
		
		public static int getGamerscore(Context context, XboxLiveAccount account)
		{
			Cursor cursor = context.getContentResolver().query(
					ContentUris.withAppendedId(CONTENT_URI, account.getId()),
					new String[] { GAMERSCORE }, null, null, null);
			
			try
			{
				if (cursor != null && cursor.moveToFirst())
					return cursor.getInt(0);
			}
			catch(Exception ex)
			{
				// Do nothing
				if (App.LOGV)
					ex.printStackTrace();
			}
			finally
			{
				if (cursor != null)
					cursor.close();
			}
			
			return 0;
		}
		
		public static int getRep(Context context, XboxLiveAccount account)
		{
			Cursor cursor = context.getContentResolver().query(
					ContentUris.withAppendedId(CONTENT_URI, account.getId()),
					new String[] { REP }, null, null, null);
			
			try
			{
				if (cursor != null && cursor.moveToFirst())
					return cursor.getInt(0);
			}
			catch(Exception ex)
			{
				// Do nothing
				if (App.LOGV)
					ex.printStackTrace();
			}
			finally
			{
				if (cursor != null)
					cursor.close();
			}
			
			return 0;
		}
	}
	
	public static final class Friends implements BaseColumns
	{
		private Friends() {}

		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ XboxLiveProvider.AUTHORITY + "/friends");
		
		public static final String CONTENT_TYPE = 
			"vnd.android.cursor.dir/vnd.akop.spark.xbl-friend";
		public static final String CONTENT_ITEM_TYPE = 
			"vnd.android.cursor.item/vnd.akop.spark.xbl-friend";
		
		public static final String ACCOUNT_ID = "AccountId";
		public static final String GAMERTAG = "Gamertag";
		public static final String ICON_URL = "IconUrl";
		public static final String IS_FAVORITE = "IsFavorite";
		public static final String GAMERSCORE = "Gamerscore";
		public static final String STATUS = "StatusDescription";
		public static final String STATUS_CODE = "StatusCode";
		public static final String NAME = "Name";
		public static final String LOCATION = "Location";
		public static final String BIO = "Bio";
		public static final String REP = "Rep";
		public static final String MOTTO = "Motto";
		public static final String CURRENT_ACTIVITY = "CurrentActivity";
		public static final String TITLE_ID = "TitleId";
		public static final String TITLE_NAME = "TitleName";
		public static final String TITLE_URL = "TitleUrl";
		public static final String DELETE_MARKER = "DeleteMarker";
		public static final String LAST_UPDATED = "LastUpdated";
		
		public static final String DEFAULT_SORT_ORDER = 
			STATUS_CODE + " ASC," + GAMERTAG + " COLLATE NOCASE ASC";
		
		public static String getGamertag(Context context, long friendId)
		{
			Cursor cursor = context.getContentResolver().query(
					ContentUris.withAppendedId(CONTENT_URI, friendId),
					new String[] { GAMERTAG }, null, null, null);
			
			try
			{
				if (cursor != null && cursor.moveToFirst())
					return cursor.getString(0);
			}
			catch(Exception ex)
			{
				// Do nothing
				if (App.LOGV)
					ex.printStackTrace();
			}
			finally
			{
				if (cursor != null)
					cursor.close();
			}
			
			return null;
		}
		
		public static long getFriendId(Context context,
				SupportsFriends account, String gamertag)
		{
			Cursor cursor = context.getContentResolver().query(CONTENT_URI,
					new String[] { _ID },
					ACCOUNT_ID + "=" + account.getId() + " AND " + GAMERTAG
							+ " LIKE ?", new String[] { gamertag }, null);
			
			try
			{
				if (cursor != null && cursor.moveToFirst())
					return cursor.getLong(0);
			}
			catch(Exception ex)
			{
				// Do nothing
				if (App.LOGV)
					ex.printStackTrace();
			}
			finally
			{
				if (cursor != null)
					cursor.close();
			}
			
			return -1;
		}
		
		public static boolean isFriend(Context context,
				SupportsFriends account, String gamertag)
		{
			Cursor cursor = context.getContentResolver().query(CONTENT_URI,
					new String[] { _ID },
					ACCOUNT_ID + "=" + account.getId() + " AND " + GAMERTAG
							+ " LIKE ?", new String[] { gamertag }, null);
			
			if (cursor != null)
			{
				try
				{
					return cursor.moveToFirst();
				}
				catch(Exception ex)
				{
					// Do nothing
					if (App.LOGV)
						ex.printStackTrace();
				}
				finally
				{
					cursor.close();
				}
			}
			
			return false;
		}
		
		public static long getLastUpdated(Context context, long friendId)
		{
			Cursor cursor = context.getContentResolver().query(
					ContentUris.withAppendedId(CONTENT_URI, friendId),
					new String[] { LAST_UPDATED }, null, null, null);
			
			try
			{
				if (cursor != null && cursor.moveToFirst())
					return cursor.getLong(0);
			}
			catch(Exception ex)
			{
				// Do nothing
				if (App.LOGV)
					ex.printStackTrace();
			}
			finally
			{
				if (cursor != null)
					cursor.close();
			}
			
			return 0;
		}
		
		public static int getStatusCode(Context context, long friendId)
		{
			Cursor cursor = context.getContentResolver().query(
					ContentUris.withAppendedId(CONTENT_URI, friendId),
					new String[] { STATUS_CODE }, null, null, null);
			
			if (cursor != null)
			{
				try
				{
					if (cursor.moveToFirst())
						return cursor.getInt(0);
				}
				catch(Exception ex)
				{
					// Do nothing
					if (App.LOGV)
						ex.printStackTrace();
				}
				finally
				{
					cursor.close();
				}
			}
			
			return STATUS_OTHER;
		}
		
		public static int getActiveFriendCount(Context context, XboxLiveAccount account)
		{
			Cursor cursor = context.getContentResolver().query(
					CONTENT_URI,
					new String[] { _ID },
					ACCOUNT_ID + "=" + account.getId() + " AND (" + STATUS_CODE
							+ "=" + STATUS_ONLINE + " OR " + STATUS_CODE + "="
							+ "=" + STATUS_AWAY + ")", null, null);
			
			try
			{
				if (cursor != null)
					return cursor.getCount();
			}
			catch(Exception e)
			{
				// Suppress any errors
				if (App.LOGV)
					e.printStackTrace();
			}
			finally
			{
				if (cursor != null)
					cursor.close();
			}
			
			return 0;
		}
		
		public static int getFriendCount(Context context, XboxLiveAccount account)
		{
			Cursor cursor = context.getContentResolver().query(
					CONTENT_URI,
					new String[] { _ID },
					ACCOUNT_ID + "=" + account.getId(), 
					null, null);
			
			try
			{
				if (cursor != null)
					return cursor.getCount();
			}
			catch(Exception e)
			{
				// Suppress any errors
				if (App.LOGV)
					e.printStackTrace();
			}
			finally
			{
				if (cursor != null)
					cursor.close();
			}
			
			return 0;
		}
		
		public static long getAccountId(Context context, long friendId)
		{
			Cursor cursor = context.getContentResolver().query(
					CONTENT_URI,
					new String[] { ACCOUNT_ID },
					_ID + "=" + friendId, 
					null, null);
			
			try
			{
				if (cursor != null && cursor.moveToFirst())
					return cursor.getLong(0);
			}
			catch(Exception e)
			{
				// Suppress any errors
				if (App.LOGV)
					e.printStackTrace();
			}
			finally
			{
				if (cursor != null)
					cursor.close();
			}
			
			return -1;
		}
		
		public static long[] getOnlineFriendIds(Context context, 
				XboxLiveAccount account)
		{
			List<Long> onlineFriends = new ArrayList<Long>();
			
			if (account != null)
			{
				int notifMode = account.getFriendNotifications();
				if (notifMode != XboxLiveAccount.FRIEND_NOTIFY_OFF)
				{
					String selection = "";
					if (notifMode == XboxLiveAccount.FRIEND_NOTIFY_FAVORITES)
						selection += " AND " + IS_FAVORITE + "!=0";
					
					Cursor cursor = context.getContentResolver().query(CONTENT_URI,
							new String[] { _ID },
							ACCOUNT_ID + "=" + account.getId() 
							+ " AND (" + STATUS_CODE + "=" + STATUS_ONLINE
							+ " OR " + STATUS_CODE + "=" + STATUS_AWAY + ")"
							+ selection, 
							null, null);
					
					if (cursor != null)
					{
						try
						{
							while (cursor.moveToNext())
								onlineFriends.add(cursor.getLong(0));
						}
						finally
						{
							cursor.close();
						}
					}
				}
			}
			
			long[] asArray = new long[onlineFriends.size()];
			for (int i = 0, n = onlineFriends.size(); i < n; i++)
				asArray[i] = onlineFriends.get(i);
			
			return asArray;
		}
		
		public static long[] getFriendsCurrentlyPlaying(Context context, 
				XboxLiveAccount account, int notifMode, String gameUid)
		{
			List<Long> onlineFriends = new ArrayList<Long>();
			
			if (account != null)
			{
				String selection = "";
				if (notifMode == XboxLiveAccount.FRIEND_NOTIFY_FAVORITES)
					selection += " AND " + IS_FAVORITE + "!=0";
				
				Cursor cursor = context.getContentResolver().query(CONTENT_URI,
						new String[] { _ID },
						ACCOUNT_ID + "=" + account.getId()
						+ " AND " + TITLE_ID + "=?"
						+ " AND (" + STATUS_CODE + "=" + STATUS_ONLINE
						+ " OR " + STATUS_CODE + "=" + STATUS_AWAY + ")"
						+ selection, 
						new String[] { gameUid }, null);
				
				if (cursor != null)
				{
					try
					{
						while (cursor.moveToNext())
							onlineFriends.add(cursor.getLong(0));
					}
					finally
					{
						cursor.close();
					}
				}
			}
			
			long[] asArray = new long[onlineFriends.size()];
			for (int i = 0, n = onlineFriends.size(); i < n; i++)
				asArray[i] = onlineFriends.get(i);
			
			return asArray;
		}
		
		public static String getStatusDescription(Context context, int statusCode)
		{
			switch (statusCode)
			{
			case STATUS_INVITE_RCVD:
			case STATUS_INVITE_SENT:
				return context.getString(R.string.pending);
			case STATUS_OFFLINE:
				return context.getString(R.string.offline);
			case STATUS_ONLINE:
				return context.getString(R.string.online);
			case STATUS_AWAY:
				return context.getString(R.string.away);
			case STATUS_NOT_YET:
				return context.getString(R.string.not_yet_friends);
			case STATUS_OTHER:
			default:
				return context.getString(R.string.other);
			}
		}
		
		public static HashMap<String, long[]> getOnlineFriendsMatchingMyBeacons(Context context, 
				XboxLiveAccount account, int friendNotifyMode)
		{
			List<String> gameUids = new ArrayList<String>();
			
			// Get a list of all beaconed game UID's
			
			Cursor cursor = context.getContentResolver().query(Games.CONTENT_URI,
					new String[] { Games.UID },
					Games.ACCOUNT_ID + "=" + account.getId() + " AND " + 
					Games.BEACON_SET + "!=0", 
					null, null);
			
			if (cursor != null)
			{
				try
				{
					while (cursor.moveToNext())
						gameUids.add(cursor.getString(0));
				}
				finally
				{
					cursor.close();
				}
			}
			
			// Get a list of friends who are currently playing the game
			
			HashMap<String, long[]> beacons = new HashMap<String, long[]>();
			
			for (String gameUid : gameUids)
			{
				String favFilter = "";
				if (friendNotifyMode == XboxLiveAccount.FRIEND_NOTIFY_FAVORITES)
					favFilter = " AND " + IS_FAVORITE + "!=0";
				
				cursor = context.getContentResolver().query(CONTENT_URI,
						new String[] { _ID },
						ACCOUNT_ID + "=" + account.getId() +  
						" AND (" + STATUS_CODE + "=" + STATUS_ONLINE +
						" OR " + STATUS_CODE + "=" + STATUS_AWAY + ") AND " +
						TITLE_ID + "=?" + favFilter, 
						new String[] { gameUid }, null);
				
				if (cursor != null)
				{
					try
					{
						int n = cursor.getCount();
						
						if (n > 0)
						{
							int i = 0;
							long[] matchingFriends = new long[n];
							
							while (cursor.moveToNext())
								matchingFriends[i++] = cursor.getLong(0);
							
							beacons.put(gameUid, matchingFriends);
						}
					}
					finally
					{
						cursor.close();
					}
				}
			}
			
			return beacons;
		}
	}
	
	public static final class Games implements BaseColumns
	{
		private Games() {}
		
		private static final String[] GAME_AGGREGATE_STATS = new String[] { 
			"SUM(" + Games.POINTS_ACQUIRED + ")",
			"SUM(" + Games.POINTS_TOTAL + ")",
			"SUM(" + Games.ACHIEVEMENTS_UNLOCKED + ")",
			"SUM(" + Games.ACHIEVEMENTS_TOTAL + ")",
		};
		
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ XboxLiveProvider.AUTHORITY + "/games");
		
		public static final String CONTENT_TYPE = 
			"vnd.android.cursor.dir/vnd.akop.spark.xbl-game";
		public static final String CONTENT_ITEM_TYPE = 
			"vnd.android.cursor.item/vnd.akop.spark.xbl-game";
		
		public static final String ACCOUNT_ID = "AccountId";
		public static final String TITLE = "Title";
		public static final String UID = "Uid";
		public static final String BOXART_URL = "BoxartUrl";
		public static final String LAST_PLAYED = "LastPlayed";
		public static final String POINTS_ACQUIRED = "PointsAcquired";
		public static final String POINTS_TOTAL = "PointsTotal";
		public static final String ACHIEVEMENTS_UNLOCKED = "AchievementsUnlocked";
		public static final String ACHIEVEMENTS_TOTAL = "AchievementsTotal";
		public static final String ACHIEVEMENTS_STATUS = "AchievementsStatus";
		public static final String BEACON_SET = "BeaconSet";
		public static final String BEACON_TEXT = "BeaconText";
		public static final String LAST_UPDATED = "LastUpdated";
		public static final String GAME_URL = "GameUrl";
		public static final String INDEX = "ListIndex";
		
		public static final String DEFAULT_SORT_ORDER = 
			INDEX + " ASC, " + LAST_PLAYED + " DESC" ;
		
		public static String getUid(Context context, long gameId)
		{
			Cursor cursor = context.getContentResolver().query(
					ContentUris.withAppendedId(CONTENT_URI, gameId),
					new String[] { UID }, null, null, null);
			
			try
			{
				if (cursor != null && cursor.moveToFirst())
					return cursor.getString(0);
			}
			finally
			{
				if (cursor != null)
					cursor.close();
			}
			
			return null;
		}
		
		public static String getGameUrl(Context context, long gameId)
		{
			Cursor cursor = context.getContentResolver().query(
					ContentUris.withAppendedId(CONTENT_URI, gameId),
					new String[] { GAME_URL }, null, null, null);
			
			if (cursor != null)
			{
				try
				{
					if (cursor.moveToFirst())
						return cursor.getString(0);
				}
				finally
				{
					cursor.close();
				}
			}
			
			return null;
		}
		
		public static int getSetBeaconCount(Context context, XboxLiveAccount account)
		{
			Cursor cursor = context.getContentResolver().query(CONTENT_URI,
					new String[] { UID }, 
					ACCOUNT_ID + "=" + account.getId() + 
					" AND " + BEACON_SET + "!=" + 0,
					null, null);
			
			if (cursor != null)
			{
				try
				{
					return cursor.getCount();
				}
				finally
				{
					cursor.close();
				}
			}
			
			return 0;
		}
		
		public static String[] getGamesWithBeacons(Context context, XboxLiveAccount account)
		{
			Cursor cursor = context.getContentResolver().query(CONTENT_URI,
					new String[] { UID }, 
					ACCOUNT_ID + "=" + account.getId() + 
					" AND " + BEACON_SET + "!=" + 0,
					null, null);
			
			ArrayList<String> games = new ArrayList<String>();
			if (cursor != null)
			{
				try
				{
					while (cursor.moveToNext())
						games.add(cursor.getString(0));
				}
				finally
				{
					cursor.close();
				}
			}
			
			return games.toArray(null);
		}
		
		public static Long getId(Context context, XboxLiveAccount account, String titleId)
		{
			Cursor cursor = context.getContentResolver().query(CONTENT_URI,
					new String[] { _ID }, 
					ACCOUNT_ID + "=" + account.getId() + " AND " + UID + "=" + titleId, 
					null, null);
			
			if (cursor != null)
			{
				try
				{
					if (cursor.moveToFirst())
						return cursor.getLong(0);
				}
				finally
				{
					cursor.close();
				}
			}
			
			return null;
		}
		
		public static String getLastPlayedTitle(Context context, XboxLiveAccount account)
		{
			Cursor cursor = context.getContentResolver().query(
					CONTENT_URI,
					new String[] { TITLE }, 
					ACCOUNT_ID + "=" + account.getId(), 
					null,
					LAST_PLAYED + " DESC");
			
			try
			{
				if (cursor != null && cursor.moveToFirst())
					return cursor.getString(0);
				
				return null;
			}
			finally
			{
				if (cursor != null)
					cursor.close();
			}
		}
		
		public static int getGameCount(Context context, 
				XboxLiveAccount account)
		{
			Cursor cursor = context.getContentResolver().query(
					CONTENT_URI,
					new String[] { "COUNT(*)" }, 
					ACCOUNT_ID + "=" + account.getId(), 
					null, null);
			
			try
			{
				if (cursor != null && cursor.moveToFirst())
					return cursor.getInt(0);
			}
			catch(Exception e)
			{
				// Suppress any errors
				if (App.LOGV)
					e.printStackTrace();
			}
			finally
			{
				if (cursor != null)
					cursor.close();
			}
			
			return 0;
		}
		
		public static String getTitle(Context context, long gameId)
		{
			Cursor cursor = context.getContentResolver().query(
					CONTENT_URI,
					new String[] { TITLE }, 
					_ID + "=" + gameId, null, null);
			
			if (cursor != null)
			{
				try
				{
					if (cursor.moveToFirst())
						return cursor.getString(0);
				}
				catch(Exception e)
				{
					// Suppress any errors
					if (App.LOGV)
						e.printStackTrace();
				}
				finally
				{
					cursor.close();
				}
			}
			
			return null;
		}
		
		public static String getTitle(Context context, XboxLiveAccount account, String gameUid)
		{
			Cursor cursor = context.getContentResolver().query(
					CONTENT_URI,
					new String[] { TITLE }, 
					ACCOUNT_ID + "=" + account.getId() +
					" AND " + UID + "=?", new String[] { gameUid }, null);
			
			if (cursor != null)
			{
				try
				{
					if (cursor.moveToFirst())
						return cursor.getString(0);
				}
				catch(Exception e)
				{
					// Suppress any errors
					if (App.LOGV)
						e.printStackTrace();
				}
				finally
				{
					cursor.close();
				}
			}
			
			return null;
		}
		
		public static int getAchievementCompletionPercentage(Context context, 
				XboxLiveAccount account)
		{
			Cursor cursor = context.getContentResolver().query(
					CONTENT_URI,
					GAME_AGGREGATE_STATS, 
					ACCOUNT_ID + "=" + account.getId(), 
					null, null);
			
			int achPercent = 0;
			
			try
			{
				if (cursor != null && cursor.moveToFirst())
				{
					int achUnlocked = cursor.getInt(2);
					int achTotal = cursor.getInt(3);
					
					if (achTotal != 0)
						achPercent = (int)((float)achUnlocked / (float)achTotal * 100.0);
				}
			}
			catch(Exception e)
			{
				// Suppress any errors
				if (App.LOGV)
					e.printStackTrace();
			}
			finally
			{
				if (cursor != null)
					cursor.close();
			}
			
			return achPercent;
		}
		
		public static int getAchievementCount(Context context, 
				XboxLiveAccount account, long gameId)
		{
			Cursor cursor = context.getContentResolver().query(
					CONTENT_URI,
					new String[] { ACHIEVEMENTS_UNLOCKED, ACHIEVEMENTS_TOTAL }, 
					ACCOUNT_ID + "=" + account.getId() 
					+ " AND " + _ID + "=" + gameId, 
					null, null);
			
			try
			{
				if (cursor != null && cursor.moveToFirst())
					return cursor.getInt(1);
			}
			catch(Exception e)
			{
				// Suppress any errors
				if (App.LOGV)
					e.printStackTrace();
			}
			finally
			{
				if (cursor != null)
					cursor.close();
			}
			
			return 0;
		}
		
		public static int getAchievementCompletionPercentage(Context context, 
				XboxLiveAccount account, long gameId)
		{
			Cursor cursor = context.getContentResolver().query(
					CONTENT_URI,
					new String[] { ACHIEVEMENTS_UNLOCKED, ACHIEVEMENTS_TOTAL }, 
					ACCOUNT_ID + "=" + account.getId() 
					+ " AND " + _ID + "=" + gameId, 
					null, null);
			
			try
			{
				if (cursor != null && cursor.moveToFirst())
				{
					int total = cursor.getInt(1);
					if (total > 0)
						return (int)(((float)cursor.getInt(0) / (float)total) * 100.0f);
				}
			}
			catch(Exception e)
			{
				// Suppress any errors
				if (App.LOGV)
					e.printStackTrace();
			}
			finally
			{
				if (cursor != null)
					cursor.close();
			}
			
			return 0;
		}
		
		public static String getLastPlayedText(Context context, long lastPlayed)
		{
			return context.getString(R.string.last_played_f,
					DateFormat.getDateInstance().format(lastPlayed));
		}
		
		public static String getAchievementTotalText(Context context, 
				int achUnlocked, int achTotal)
		{
			if (achTotal <= 0)
			{
				// No achievements
				return context.getString(R.string.no_achievements);
			}
			else
			{
				return context.getString(R.string.achieves_x_of_x_f,
						achUnlocked, achTotal);
			}
		}
		
		public static String getGamerscoreTotalText(Context context,
				int gsAcquired, int gsTotal)
		{
			return context.getString(R.string.x_of_x_f,
					gsAcquired, gsTotal);
		}
		
		public static boolean isDirty(Context context, 
				XboxLiveAccount account, long gameId)
		{
			ContentResolver cr = context.getContentResolver();
			Cursor cursor = cr.query(Games.CONTENT_URI,
					new String[] { Games.ACHIEVEMENTS_STATUS },
					Games._ID + "=" + gameId, 
					null, null);
			
			if (cursor != null)
			{
				try
				{
					if (cursor.moveToFirst())
						return (cursor.getInt(0) != 0);
				}
				catch(Exception e)
				{
					// Suppress any errors
					if (App.LOGV)
						e.printStackTrace();
				}
				finally
				{
					cursor.close();
				}
			}
			
			return false;
		}
	}
	
	public static final class Achievements implements BaseColumns
	{
		private Achievements() {}
		
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ XboxLiveProvider.AUTHORITY + "/achievements");
		
		public static final String CONTENT_TYPE = 
			"vnd.android.cursor.dir/vnd.akop.spark.xbl-achievement";
		public static final String CONTENT_ITEM_TYPE = 
			"vnd.android.cursor.item/vnd.akop.spark.xbl-achievement";
		
		public static final String GAME_ID = "GameId";
		public static final String TITLE = "Title";
		public static final String DESCRIPTION = "Description";
		public static final String ICON_URL = "IconUrl";
		public static final String POINTS = "Points";
		public static final String ACQUIRED = "Acquired";
		public static final String LOCKED = "Locked";
		public static final String INDEX = "ListIndex";
		
		public static final String DEFAULT_SORT_ORDER = INDEX + " ASC";
	}
	
	public static final class Messages implements BaseColumns
	{
		private Messages() {}
		
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ XboxLiveProvider.AUTHORITY + "/messages");
		
		public static final String CONTENT_TYPE = 
			"vnd.android.cursor.dir/vnd.akop.spark.xbl-message";
		public static final String CONTENT_ITEM_TYPE = 
			"vnd.android.cursor.item/vnd.akop.spark.xbl-message";
		
		public static final String ACCOUNT_ID = "AccountId";
		public static final String UID = "Uid";
		public static final String SENDER = "Sender";
		public static final String GAMERPIC = "Gamerpic";
		public static final String TYPE = "MessageType";
		public static final String BODY = "Body";
		public static final String SENT = "Sent";
		public static final String IS_DIRTY = "IsDirty";
		public static final String IS_READ = "IsRead";
		public static final String DELETE_MARKER = "DeleteMarker";
		
		public static final String DEFAULT_SORT_ORDER = SENT + " DESC, UID DESC";
		
		public static long getUid(Context context, long messageId)
		{
			Cursor cursor = context.getContentResolver().query(
					ContentUris.withAppendedId(CONTENT_URI, messageId),
					new String[] { UID }, null, null, null);
			
			try
			{
				if (cursor != null && cursor.moveToFirst())
					return cursor.getLong(0);
			}
			finally
			{
				if (cursor != null)
					cursor.close();
			}
			
			return -1;
		}
		
		public static String getSender(Context context, long messageId)
		{
			Cursor cursor = context.getContentResolver().query(
					ContentUris.withAppendedId(CONTENT_URI, messageId),
					new String[] { SENDER }, null, null, null);
			
			if (cursor != null)
			{
				try
				{
					if (cursor.moveToFirst())
						return cursor.getString(0);
				}
				finally
				{
					cursor.close();
				}
			}
			
			return null;
		}
		
		public static long[] getUnreadMessageIds(Context context, XboxLiveAccount account)
		{
			List<Long> unreadMessages = new ArrayList<Long>();
			
			if (account != null)
			{
				Cursor cursor = context.getContentResolver().query(CONTENT_URI,
						new String[] { _ID },
						ACCOUNT_ID + "=" + account.getId() + " AND " + IS_READ + "= 0", 
						null, null);
				
				try
				{
					if (cursor != null)
						while (cursor.moveToNext())
							unreadMessages.add(cursor.getLong(0));
				}
				finally
				{
					if (cursor != null)
						cursor.close();
				}
			}
			
			long[] asArray = new long[unreadMessages.size()];
			for (int i = 0, n = unreadMessages.size(); i < n; i++)
				asArray[i] = unreadMessages.get(i);
				
			return asArray;
		}
		
		public static int getUnreadMessageCount(Context context, XboxLiveAccount account)
		{
			Cursor cursor = context.getContentResolver().query(CONTENT_URI,
					new String[] { _ID },
					ACCOUNT_ID + "=" + account.getId() + " AND " + IS_READ + "= 0", 
					null, null);
			
			try
			{
				if (cursor != null)
					return cursor.getCount();
			}
			finally
			{
				if (cursor != null)
					cursor.close();
			}
			
			return 0;
		}
		
		public static boolean isUnreadTextMessage(Context context, long messageId)
		{
			Cursor cursor = context.getContentResolver().query(
					ContentUris.withAppendedId(CONTENT_URI, messageId),
					new String[] { IS_READ, TYPE }, null, null, null);
			
			if (cursor != null)
			{
				try
				{
					if (cursor.moveToFirst())
						return (cursor.getInt(0) == 0)
						        && (cursor.getInt(1) == XboxLive.MESSAGE_TEXT);
				}
				finally
				{
					cursor.close();
				}
			}
			
			return false;
		}
	}
	
	public static final class Beacons implements BaseColumns
	{
		private Beacons() {}
		
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ XboxLiveProvider.AUTHORITY + "/beacons");
		
		public static final String CONTENT_TYPE = 
			"vnd.android.cursor.dir/vnd.akop.spark.xbl-beacon";
		public static final String CONTENT_ITEM_TYPE = 
			"vnd.android.cursor.item/vnd.akop.spark.xbl-beacon";
		
		public static final String ACCOUNT_ID = "AccountId";
		public static final String FRIEND_ID = "FriendId";
		public static final String TITLE_ID = "TitleId";
		public static final String TITLE_BOXART = "TitleBoxart";
		public static final String TITLE_NAME = "TitleName";
		public static final String TEXT = "Text";
		
		public static final String DEFAULT_SORT_ORDER = TITLE_NAME + " DESC";
	}
	
	public static final class Events implements BaseColumns
	{
		private Events() {}
		
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ XboxLiveProvider.AUTHORITY + "/events");
		
		public static final String CONTENT_TYPE = 
			"vnd.android.cursor.dir/vnd.akop.spark.xbl-event";
		public static final String CONTENT_ITEM_TYPE = 
			"vnd.android.cursor.item/vnd.akop.spark.xbl-event";
		
		public static final String ACCOUNT_ID = "AccountId";
		public static final String TITLE = "Title";
		public static final String DESCRIPTION = "Description";
		public static final String DATE = "Date";
		public static final String FLAGS = "Flags";
		
		public static final String DEFAULT_SORT_ORDER = DATE + " DESC";
	}
	
	public static final class NotifyStates implements BaseColumns
	{
		public static final int TYPE_MESSAGES = 1;
		public static final int TYPE_FRIENDS = 2;
		public static final int TYPE_BEACONS = 3;
		
		private static final long NOTIFY_FRESH_MILLIS = 30 * DateUtils.MINUTE_IN_MILLIS;
		
		private NotifyStates() {}
		
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ XboxLiveProvider.AUTHORITY + "/notify_states");
		
		public static final String CONTENT_TYPE = 
			"vnd.android.cursor.dir/vnd.akop.spark.notify-states";
		public static final String CONTENT_ITEM_TYPE = 
			"vnd.android.cursor.item/vnd.akop.spark.notify-states";
		
		public static final String ACCOUNT_ID = "AccountId";
		public static final String TYPE = "Type";
		public static final String DATA = "Data";
		public static final String LAST_UPDATED = "LastUpdated";
		
		public static final String DEFAULT_SORT_ORDER = TYPE + " DESC";
		
		private static long[] stringToLongArray(String data)
		{
			String[] items = data.split(",");
			long[] longArray = new long[items.length];
			
			for (int i = 0; i < items.length; i++)
			{
				try
				{
					longArray[i] = Long.parseLong(items[i]);
				}
				catch(Exception e)
				{
					longArray[i] = -1;
					continue;
				}
			}
			
			return longArray;
		}
		
		public static void setFriendsLastNotified(Context context, 
				XboxLiveAccount account, long[] lastNotified)
		{
			int type = TYPE_FRIENDS;
			
			ContentResolver cr = context.getContentResolver();
			cr.delete(CONTENT_URI, 
					ACCOUNT_ID + "=" + account.getId() + " AND " + 
					TYPE + "=" + type, 
					null);
			
			if (lastNotified != null && lastNotified.length > 0)
			{
				String data = Parser.joinString(lastNotified, ",");
				
				ContentValues cv = new ContentValues(10);
				cv.put(ACCOUNT_ID, account.getId());
				cv.put(TYPE, type);
				cv.put(DATA, data);
				cv.put(LAST_UPDATED, System.currentTimeMillis());
				
				cr.insert(CONTENT_URI, cv);
			}
		}
		
		public static void setMessagesLastNotified(Context context, 
				XboxLiveAccount account, long[] lastNotified)
		{
			int type = TYPE_MESSAGES;
			
			ContentResolver cr = context.getContentResolver();
			cr.delete(CONTENT_URI, 
					ACCOUNT_ID + "=" + account.getId() + " AND " + 
					TYPE + "=" + type, 
					null);
			
			if (lastNotified != null && lastNotified.length > 0)
			{
				String data = Parser.joinString(lastNotified, ",");
				
				ContentValues cv = new ContentValues(10);
				cv.put(ACCOUNT_ID, account.getId());
				cv.put(TYPE, type);
				cv.put(DATA, data);
				cv.put(LAST_UPDATED, System.currentTimeMillis());
				
				cr.insert(CONTENT_URI, cv);
			}
		}
		
		public static void setBeaconsLastNotified(Context context, 
				XboxLiveAccount account, HashMap<String, long[]> beacons)
		{
			int type = TYPE_BEACONS;
			
			ContentResolver cr = context.getContentResolver();
			cr.delete(CONTENT_URI, 
					ACCOUNT_ID + "=" + account.getId() + " AND " + 
					TYPE + "=" + type, 
					null);
			
			List<Object> groups = new ArrayList<Object>();
			for (String gameId : beacons.keySet())
			{
				long[] friendIds = (long[])beacons.get(gameId);
				if (friendIds != null && friendIds.length > 0)
					groups.add(gameId + "=" + Parser.joinString(friendIds, ","));
			}
			
			if (groups.size() > 0)
			{
				String data = Parser.joinString(groups, "|");
				
				ContentValues cv = new ContentValues(10);
				cv.put(ACCOUNT_ID, account.getId());
				cv.put(TYPE, type);
				cv.put(DATA, data);
				cv.put(LAST_UPDATED, System.currentTimeMillis());
				
				cr.insert(CONTENT_URI, cv);
			}
		}
		
		public static long[] getFriendsLastNotified(Context context, XboxLiveAccount account)
		{
			long[] lastNotified = null;
			
			Cursor cursor = context.getContentResolver().query(CONTENT_URI,
					new String[] { DATA, LAST_UPDATED },
					ACCOUNT_ID + "=" + account.getId() + " AND " + 
					TYPE + "=" + TYPE_FRIENDS, 
					null, null);
			
			if (cursor != null)
			{
				try
				{
					if (cursor.moveToNext())
					{
						if (System.currentTimeMillis() - cursor.getLong(1) < NOTIFY_FRESH_MILLIS)
							lastNotified = stringToLongArray(cursor.getString(0));
					}
				}
				finally
				{
					cursor.close();
				}
			}
			
			if (lastNotified == null)
				lastNotified = new long[0];
			
			return lastNotified;
		}
		
		public static long[] getMessagesLastNotified(Context context, XboxLiveAccount account)
		{
			long[] lastNotified = null;
			
			Cursor cursor = context.getContentResolver().query(CONTENT_URI,
					new String[] { DATA, LAST_UPDATED },
					ACCOUNT_ID + "=" + account.getId() + " AND " + 
					TYPE + "=" + TYPE_MESSAGES, 
					null, null);
			
			if (cursor != null)
			{
				try
				{
					if (cursor.moveToNext())
					{
						if (System.currentTimeMillis() - cursor.getLong(1) < NOTIFY_FRESH_MILLIS)
							lastNotified = stringToLongArray(cursor.getString(0));
					}
				}
				finally
				{
					cursor.close();
				}
			}
			
			if (lastNotified == null)
				lastNotified = new long[0];
			
			return lastNotified;
		}
		
		public static HashMap<String, long[]> getBeaconsLastNotified(Context context, 
				XboxLiveAccount account)
		{
			HashMap<String, long[]> beacons = new HashMap<String, long[]>();
			
			Cursor cursor = context.getContentResolver().query(CONTENT_URI,
					new String[] { DATA, LAST_UPDATED },
					ACCOUNT_ID + "=" + account.getId() + " AND " + 
					TYPE + "=" + TYPE_BEACONS, 
					null, null);
			
			// Each row is of format 
			// gameId=friendId,friendId,..|gameId=friendId,friendId,..|...
			
			if (cursor != null)
			{
				try
				{
					if (cursor.moveToNext())
					{
						if (System.currentTimeMillis() - cursor.getLong(1) < NOTIFY_FRESH_MILLIS)
						{
							String data = cursor.getString(0);
							String[] groups = data.split("\\|");
							
							for (String group : groups)
							{
								String[] pairs = group.split("=", 2);
								
								if (pairs.length > 1 && !beacons.containsKey(pairs[0]))
									beacons.put(pairs[0], stringToLongArray(pairs[1]));
							}
						}
					}
				}
				finally
				{
					cursor.close();
				}
			}
			
			return beacons;
		}
	}
	
	public static final class SentMessages implements BaseColumns
	{
		private SentMessages() {}
		
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ XboxLiveProvider.AUTHORITY + "/sent_messages");
		
		public static final String CONTENT_TYPE = 
			"vnd.android.cursor.dir/vnd.akop.spark.sent-message";
		public static final String CONTENT_ITEM_TYPE = 
			"vnd.android.cursor.item/vnd.akop.spark.sent-message";
		
		public static final String ACCOUNT_ID = "AccountId";
		public static final String RECIPIENTS = "Recipients";
		public static final String TYPE = "MessageType";
		public static final String PREVIEW = "Preview";
		public static final String BODY = "Body";
		public static final String SENT = "Sent";
		
		public static final String DEFAULT_SORT_ORDER = SENT + " DESC";
	}
	
	public static class RecentPlayersCursor extends SerializableMatrixCursor
	{
        private static final long serialVersionUID = 9064454867264128593L;
        
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ XboxLiveProvider.AUTHORITY + "/recent_players");
		
		public static final String _ID = BaseColumns._ID;
		public static final String GAMERTAG = "Gamertag"; 
		public static final String ACTIVITY = "Activity";
		public static final String ICON_URL = "IconUrl"; 
		public static final String TITLE_ICON_URL = "TitleIconUrl"; 
		public static final String TITLE_ID = "TitleId";
		public static final String GAMERSCORE = "GamerScore";
		public static final String IS_FRIEND = "IsFriend";
		
		public static final int COLUMN_GAMERTAG = 1; 
		public static final int COLUMN_ACTIVITY = 2; 
		public static final int COLUMN_ICON_URL = 3; 
		public static final int COLUMN_TITLE_ICON_URL = 4;
		public static final int COLUMN_TITLE_ID = 5;
		public static final int COLUMN_GAMERSCORE = 6;
		public static final int COLUMN_IS_FRIEND = 7;
		
		protected static final String[] KEYS = 
		{
			_ID,
			GAMERTAG,
			ACTIVITY,
			ICON_URL,
			TITLE_ICON_URL,
			TITLE_ID,
			GAMERSCORE,
			IS_FRIEND,
		};
		
		public RecentPlayersCursor(ContentResolver cr)
        {
			this(KEYS, 200);
			
			setNotificationUri(cr, CONTENT_URI);
        }
		
		protected RecentPlayersCursor(String[] columnNames,
		        int initialCapacity)
        {
			super(KEYS, 200);
        }
		
		public void addItem(String gamertag, String activity, 
				String iconUrl, String titleIconUrl, String titleId,
				int gamerscore, boolean isFriend)
		{
			addItem(getCount(), gamertag, activity, iconUrl,
					titleIconUrl, titleId, gamerscore, isFriend);
		}
		
		public void addItem(long id, String gamertag, String activity, 
				String iconUrl, String titleIconUrl, String titleId,
				int gamerscore, boolean isFriend)
		{
			this.newRow()
				.add(id)
				.add(gamertag)
				.add(activity)
				.add(iconUrl)
				.add(titleIconUrl)
				.add(titleId)
				.add(gamerscore)
				.add(isFriend ? 1 : 0);
		}
	}
	
	public static class FriendsOfFriendCursor extends RecentPlayersCursor
	{
        private static final long serialVersionUID = -2363313876556258929L;
        
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ XboxLiveProvider.AUTHORITY + "/friends_of_friend");
		
		public FriendsOfFriendCursor(ContentResolver cr)
        {
			super(KEYS, 200);
			
			setNotificationUri(cr, CONTENT_URI);
        }
	}
	
	public static final class ComparedGameCursor extends SerializableMatrixCursor
	{
        private static final long serialVersionUID = 4841347936245189704L;
        
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ XboxLiveProvider.AUTHORITY + "/compared_games");
		
		public static final String _ID = BaseColumns._ID;
		public static final String UID = "Uid"; 
		public static final String TITLE = "GameTitle"; 
		public static final String MY_ACH_UNLOCKED = "MyAchUnlocked"; 
		public static final String YOUR_ACH_UNLOCKED = "YourAchUnlocked";
		public static final String ACH_TOTAL = "AchTotal";
		public static final String MY_GP_EARNED = "MyGpEarned";
		public static final String YOUR_GP_EARNED = "YourGpEarned";
		public static final String GP_TOTAL = "GpTotal";
		public static final String BOXART_URL = "GameIconUrl";
		public static final String GAME_URL = "GameUrl";
		
		public static final int COLUMN_UID = 1; 
		public static final int COLUMN_TITLE = 2; 
		public static final int COLUMN_MY_ACH_UNLOCKED = 3; 
		public static final int COLUMN_YOUR_ACH_UNLOCKED = 4;
		public static final int COLUMN_ACH_TOTAL = 5;
		public static final int COLUMN_MY_GP_EARNED = 6;
		public static final int COLUMN_YOUR_GP_EARNED = 7;
		public static final int COLUMN_GP_TOTAL = 8;
		public static final int COLUMN_BOXART_URL = 9;
		public static final int COLUMN_GAME_URL = 10;
		
		private static final String[] KEYS = 
		{
			_ID,
			UID,
			TITLE,
			MY_ACH_UNLOCKED,
			YOUR_ACH_UNLOCKED,
			ACH_TOTAL,
			MY_GP_EARNED,
			YOUR_GP_EARNED,
			GP_TOTAL,
			BOXART_URL,
			GAME_URL,
		};
		
		public ComparedGameCursor(ContentResolver cr)
        {
			super(KEYS, 200);
			
			setNotificationUri(cr, CONTENT_URI);
        }
		
		public void addItem(String title, String uid, 
				int myAchieves, int yourAchieves, int achieveTotal,
				int myGamerscore, int yourGamerscore, int gamerscoreTotal,
				String boxartUrl, String gamerUrl)
		{
			this.newRow()
				.add(getCount())
				.add(uid)
				.add(title)
				.add(myAchieves)
				.add(yourAchieves)
				.add(achieveTotal)
				.add(myGamerscore)
				.add(yourGamerscore)
				.add(gamerscoreTotal)
				.add(boxartUrl)
				.add(gamerUrl);
		}
	}
	
	public static final class ComparedGameInfo implements Serializable
	{
		private static final long serialVersionUID = -7864419735761064375L;
		
		public int myGamesPlayed;
		public int myGamerscore;
		public String myAvatarIconUrl;
		public int yourGamesPlayed;
		public int yourGamerscore;
		public String yourAvatarIconUrl;
		public String overviewUrl;
		public ComparedGameCursor cursor;
		
		public ComparedGameInfo(ContentResolver cr)
		{
			cursor = new ComparedGameCursor(cr);
			myGamerscore = myGamesPlayed = 0;
			yourGamerscore = yourGamesPlayed = 0;
		}
	}
	
	public static final class ComparedAchievementCursor extends SerializableMatrixCursor
	{
        private static final long serialVersionUID = -6952024669915353534L;
        
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ XboxLiveProvider.AUTHORITY + "/compared_achievements");
		
		public static final String _ID = BaseColumns._ID;
		public static final String UID = "Uid";
		public static final String TITLE = "Title";
		public static final String DESCRIPTION = "Description";
		public static final String SCORE = "Score";
		public static final String MY_ACQUIRED = "MyAcquired";
		public static final String MY_IS_LOCKED = "MyIsLocked";
		public static final String YOUR_ACQUIRED = "YourAcquired";
		public static final String YOUR_IS_LOCKED = "YourIsLocked";
		public static final String ICON_URL = "IconUrl";
		
		public static final int COLUMN_UID = 1; 
		public static final int COLUMN_TITLE = 2;
		public static final int COLUMN_DESCRIPTION = 3;
		public static final int COLUMN_SCORE = 4;
		public static final int COLUMN_MY_ACQUIRED = 5;
		public static final int COLUMN_MY_IS_LOCKED = 6;
		public static final int COLUMN_YOUR_ACQUIRED = 7;
		public static final int COLUMN_YOUR_IS_LOCKED = 8;
		public static final int COLUMN_ICON_URL = 9;
		
		private static final String[] KEYS = 
		{
			_ID,
			UID,
			TITLE,
			DESCRIPTION,
			SCORE,
			MY_ACQUIRED,
			MY_IS_LOCKED,
			YOUR_ACQUIRED,
			YOUR_IS_LOCKED,
			ICON_URL,
		};
		
		public ComparedAchievementCursor(ContentResolver cr)
        {
			super(KEYS, 200);
			
			setNotificationUri(cr, CONTENT_URI);
        }
		
		public void addItem(String uid, String title,
				String description, int score,
				long myAcquired, int myIsLocked,
				long yourAcquired, int yourIsLocked,
				String iconUrl)
		{
			this.newRow()
				.add(getCount())
				.add(uid)
				.add(title)
				.add(description)
				.add(score)
				.add(myAcquired)
				.add(myIsLocked)
				.add(yourAcquired)
				.add(yourIsLocked)
				.add(iconUrl);
		}
	}
	
	public static final class ComparedAchievementInfo implements Serializable
	{
		private static final long serialVersionUID = 8053526431540231540L;
		
		public ComparedAchievementCursor cursor;
		public int myGamerscore;
		public int yourGamerscore;
		public int totalGamerscore;
		public String myAvatarIconUrl;
		public String yourAvatarIconUrl;
		
		public ComparedAchievementInfo(ContentResolver cr)
		{
			cursor = new ComparedAchievementCursor(cr);
			myGamerscore = yourGamerscore = 0;
			totalGamerscore = 0;
		}
	}
	
	public static final class RecentPlayers implements Serializable
	{
        private static final long serialVersionUID = -6056933243040134741L;
        
		public RecentPlayersCursor Players;
		
		public RecentPlayers(ContentResolver cr)
		{
			Players = new RecentPlayersCursor(cr);
		}
	}
	
	public static final class FriendsOfFriend implements Serializable
	{
        private static final long serialVersionUID = -6056933243040134741L;
        
		public FriendsOfFriendCursor SharedFriends;
		public FriendsOfFriendCursor NotYetFriends;
		
		public FriendsOfFriend(ContentResolver cr)
		{
			SharedFriends = new FriendsOfFriendCursor(cr);
			NotYetFriends = new FriendsOfFriendCursor(cr);
		}
	}
	
	public static final class LiveStatusInfo implements Parcelable
	{
		public static class Category implements Parcelable
		{
			public String name;
			public int status;
			public String statusText;
			
			public Category()
			{
			}
			
			public static final Parcelable.Creator<Category> CREATOR = new Parcelable.Creator<Category>() 
			{
				public Category createFromParcel(Parcel in) 
				{
					return new Category(in);
				}
				
				public Category[] newArray(int size) 
				{
					return new Category[size];
				}
			};
			
			private Category(Parcel in) 
			{
				this.name = in.readString();
				this.status = in.readInt();
				this.statusText = in.readString();
			}
			
			@Override
			public void writeToParcel(Parcel dest, int flags) 
			{
				dest.writeString(this.name);
				dest.writeInt(this.status);
				dest.writeString(this.statusText);
			}
			
			@Override
			public int describeContents() 
			{
				return 0;
			}
		}
		
		public List<Category> categories;
		
		public LiveStatusInfo()
		{
			this.categories = new ArrayList<Category>();
		}
		
		public static final Parcelable.Creator<LiveStatusInfo> CREATOR = new Parcelable.Creator<LiveStatusInfo>() 
		{
			public LiveStatusInfo createFromParcel(Parcel in) 
			{
				return new LiveStatusInfo(in);
			}
			
			public LiveStatusInfo[] newArray(int size) 
			{
				return new LiveStatusInfo[size];
			}
		};
		
		private LiveStatusInfo(Parcel in) 
		{
			this.categories = new ArrayList<Category>();
			
			in.readTypedList(this.categories, Category.CREATOR);
		}
		
		public void addCategory(String name, int status, String statusText)
		{
			Category cat = new Category();
			
			cat.name = name;
			cat.status = status;
			cat.statusText = statusText;
			
			categories.add(cat);
		}
		
		@Override
		public void writeToParcel(Parcel dest, int flags) 
		{
			dest.writeTypedList(this.categories);
		}
		
		@Override
		public int describeContents() 
		{
			return 0;
		}
	}
	
	public static final class BeaconInfo implements Parcelable
	{
		public String TitleName;
		public String TitleBoxArtUrl;
		public String Text;
		
		public BeaconInfo(ContentValues cv)
		{
			this.TitleName = cv.getAsString(Beacons.TITLE_NAME);
			this.TitleBoxArtUrl = cv.getAsString(Beacons.TITLE_BOXART);
			this.Text = cv.getAsString(Beacons.TEXT);
		}
		
		public static final Parcelable.Creator<BeaconInfo> CREATOR = new Parcelable.Creator<BeaconInfo>() 
		{
			public BeaconInfo createFromParcel(Parcel in) 
			{
				return new BeaconInfo(in);
			}
			
			public BeaconInfo[] newArray(int size) 
			{
				return new BeaconInfo[size];
			}
		};
		
		private BeaconInfo(Parcel in) 
		{
			this.TitleName = in.readString();
			this.TitleBoxArtUrl = in.readString();
			this.Text = in.readString();
		}
		
		@Override
		public void writeToParcel(Parcel dest, int flags) 
		{
			dest.writeString(this.TitleName);
			dest.writeString(this.TitleBoxArtUrl);
			dest.writeString(this.Text);
		}
		
		@Override
		public int describeContents() 
		{
			return 0;
		}
	}
	
	public static final class GamerProfileInfo implements Parcelable
	{
		public long AccountId;
		public String Gamertag;
		public String IconUrl;
		public int Gamerscore;
		public String CurrentActivity;
		public String TitleIconUrl;
		public String TitleId;
		public String Name;
		public String Location;
		public String Bio;
		public String Motto;
		public int Rep;
		public boolean IsFriend;
		
		public BeaconInfo[] Beacons;
		
		public GamerProfileInfo()
		{
			this.IsFriend = false;
			this.AccountId = -1;
			this.Gamertag = null;
			this.IconUrl = null;
			this.Gamerscore = 0;
			this.CurrentActivity = null;
			this.Name = null;
			this.Location = null;
			this.Bio = null;
			this.Motto = null;
			this.Rep = 0;
			this.Beacons = null;
		}
		
		public static final Parcelable.Creator<GamerProfileInfo> CREATOR = new Parcelable.Creator<GamerProfileInfo>() 
		{
			public GamerProfileInfo createFromParcel(Parcel in) 
			{
				return new GamerProfileInfo(in);
			}
			
			public GamerProfileInfo[] newArray(int size) 
			{
				return new GamerProfileInfo[size];
			}
		};
		
		private GamerProfileInfo(Parcel in) 
		{
			this.AccountId = in.readLong();
			this.Rep = in.readInt();
			this.IsFriend = in.readByte() != 0;
			this.Gamerscore = in.readInt();
			
			this.Gamertag = in.readString();
			this.IconUrl = in.readString();
			this.CurrentActivity = in.readString();
			this.TitleIconUrl = in.readString();
			this.TitleId = in.readString();
			this.Name = in.readString();
			this.Location = in.readString();
			this.Bio = in.readString();
			this.Motto = in.readString();
			
			this.Beacons = in.createTypedArray(BeaconInfo.CREATOR);
		}
		
		@Override
		public void writeToParcel(Parcel dest, int flags) 
		{
			dest.writeLong(this.AccountId);
			dest.writeInt(this.Rep);
			dest.writeByte(this.IsFriend ? (byte)1 : 0);
			dest.writeInt(this.Gamerscore);
			
			dest.writeString(this.Gamertag);
			dest.writeString(this.IconUrl);
			dest.writeString(this.CurrentActivity);
			dest.writeString(this.TitleIconUrl);
			dest.writeString(this.TitleId);
			dest.writeString(this.Name);
			dest.writeString(this.Location);
			dest.writeString(this.Bio);
			dest.writeString(this.Motto);
			
			dest.writeTypedArray(this.Beacons, 0);
		}
		
		@Override
		public int describeContents() 
		{
			return 0;
		}
	}
	
	public static final class GameOverviewInfo implements Parcelable
	{
		public String BannerUrl;
		public String Title;
		public String Description;
		public String ManualUrl;
		public String EsrbRatingDescription;
		public String EsrbRatingIconUrl;
		public int MyRating;
		public int AverageRating;
		public ArrayList<String> Screenshots;
		
		public GameOverviewInfo()
		{
			this.Screenshots = new ArrayList<String>();
		}
		
		public static final Parcelable.Creator<GameOverviewInfo> CREATOR = new Parcelable.Creator<GameOverviewInfo>() 
		{
			public GameOverviewInfo createFromParcel(Parcel in) 
			{
				return new GameOverviewInfo(in);
			}
			
			public GameOverviewInfo[] newArray(int size) 
			{
				return new GameOverviewInfo[size];
			}
		};
		
		private GameOverviewInfo(Parcel in) 
		{
			this.BannerUrl = in.readString();
			this.Title = in.readString();
			this.Description = in.readString();
			this.ManualUrl = in.readString();
			this.EsrbRatingDescription = in.readString();
			this.EsrbRatingIconUrl = in.readString();
			this.MyRating = in.readInt();
			this.AverageRating = in.readInt();
			this.Screenshots = in.createStringArrayList();
		}
		
		@Override
		public void writeToParcel(Parcel dest, int flags) 
		{
			dest.writeString(this.BannerUrl);
			dest.writeString(this.Title);
			dest.writeString(this.Description);
			dest.writeString(this.ManualUrl);
			dest.writeString(this.EsrbRatingDescription);
			dest.writeString(this.EsrbRatingIconUrl);
			dest.writeInt(this.MyRating);
			dest.writeInt(this.AverageRating);
			dest.writeStringList(this.Screenshots);
		}
		
		@Override
		public int describeContents() 
		{
			return 0;
		}
	}
}
