/*
 * PSN.java 
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.format.DateFormat;

import com.akop.bach.provider.PsnProvider;

public class PSN
{
	public static final int CATALOG_SORT_BY_RELEASE = 0;
	public static final int CATALOG_SORT_BY_ALPHA = 1;
	
	public static final int CATALOG_RELEASE_OUT_NOW = 0;
	public static final int CATALOG_RELEASE_COMING_SOON = 1;
	
	public static final int CATALOG_CONSOLE_PS3 = 0;
	public static final int CATALOG_CONSOLE_PSVITA = 1;
	public static final int CATALOG_CONSOLE_PS2 = 2;
	public static final int CATALOG_CONSOLE_PSP = 3;
	
	public static final int MEMBER_TYPE_FREE = 0;
	public static final int MEMBER_TYPE_PLUS = 1;
	
	public static final int TROPHY_SECRET = 0;
	public static final int TROPHY_BRONZE = 1;
	public static final int TROPHY_SILVER = 2;
	public static final int TROPHY_GOLD = 3;
	public static final int TROPHY_PLATINUM = 4;
	
	public static final int STATUS_OTHER = 0;
	public static final int STATUS_PENDING_SENT = 1;
	public static final int STATUS_PENDING_RCVD = 2;
	public static final int STATUS_PENDING = 3;
	public static final int STATUS_ONLINE = 4;
	public static final int STATUS_AWAY = 5;
	public static final int STATUS_OFFLINE = 6;
	
	private static final int STATUS_DESC_RESID[] = {
		R.string.status_unknown,
		R.string.request_sent,
		R.string.request_rcvd,
		R.string.request_pending,
		R.string.online,
		R.string.away,
		R.string.offline,
	};
	
	public static final class Profiles implements BaseColumns
	{
		private Profiles() {}
		
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ PsnProvider.AUTHORITY + "/profiles");
		
		public static final String CONTENT_TYPE = 
			"vnd.android.cursor.dir/vnd.akop.bach.psn-profile";
		public static final String CONTENT_ITEM_TYPE = 
			"vnd.android.cursor.item/vnd.akop.bach.psn-profile";
		
		public static final String ACCOUNT_ID = "AccountId";
		public static final String UUID = "Uuid";
		public static final String ONLINE_ID = "OnlineId";
		public static final String ICON_URL = "IconUrl";
		public static final String LEVEL = "Level";
		public static final String PROGRESS = "Progress";
		public static final String MEMBER_TYPE = "MemberType";
		
		public static final String TROPHIES_PLATINUM = "PlatinumTrophies";
		public static final String TROPHIES_GOLD = "GoldTrophies";
		public static final String TROPHIES_SILVER = "SilverTrophies";
		public static final String TROPHIES_BRONZE = "BronzeTrophies";
		
		public static final String DEFAULT_SORT_ORDER = ONLINE_ID + " ASC";
	}
	
	public static final class Games implements BaseColumns
	{
		private Games() {}
		
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ PsnProvider.AUTHORITY + "/games");
		
		public static final String CONTENT_TYPE = 
			"vnd.android.cursor.dir/vnd.akop.bach.psn-game";
		public static final String CONTENT_ITEM_TYPE = 
			"vnd.android.cursor.item/vnd.akop.bach.psn-game";
		
		public static final String ACCOUNT_ID = "AccountId";
		public static final String TITLE = "Title";
		public static final String UID = "Uid";
		public static final String ICON_URL = "IconUrl";
		public static final String SORT_ORDER = "SortOrder";
		public static final String PROGRESS = "Progress";
		public static final String UNLOCKED_PLATINUM = "UnlockedPlatinum";
		public static final String UNLOCKED_GOLD = "UnlockedGold";
		public static final String UNLOCKED_SILVER = "UnlockedSilver";
		public static final String UNLOCKED_BRONZE = "UnlockedBronze";
		public static final String TROPHIES_DIRTY = "TrophiesDirty";
		public static final String LAST_UPDATED = "LastUpdated";
		
		public static final String DEFAULT_SORT_ORDER = SORT_ORDER + " ASC";
		
		public static String getUid(Context context, long gameId)
		{
			Cursor cursor = context.getContentResolver().query(
					ContentUris.withAppendedId(CONTENT_URI, gameId),
					new String[] { UID }, null, null, null);
			
			try
			{
				if (cursor != null && cursor.moveToFirst())
					return cursor.getString(0);
				
				throw new IllegalArgumentException(context
						.getString(R.string.game_not_found));
			}
			finally
			{
				if (cursor != null)
					cursor.close();
			}
		}
		
		public static String getTitle(Context context, long gameId)
		{
			Cursor cursor = context.getContentResolver().query(
					ContentUris.withAppendedId(CONTENT_URI, gameId),
					new String[] { TITLE }, null, null, null);
			
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
		
		public static boolean isDirty(Context context, long gameId)
		{
			Cursor cursor = context.getContentResolver().query(
					ContentUris.withAppendedId(CONTENT_URI, gameId),
					new String[] { TROPHIES_DIRTY }, null, null, null);
			
			if (cursor != null)
			{
				try
				{
					if (cursor.moveToFirst())
						return cursor.getInt(0) != 0;
				}
				finally
				{
					cursor.close();
				}
			}
			
			return false;
		}
	}
	
	public static final class Trophies implements BaseColumns
	{
		private Trophies() {}
		
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ PsnProvider.AUTHORITY + "/trophies");
		
		public static final String CONTENT_TYPE = 
			"vnd.android.cursor.dir/vnd.akop.bach.psn-trophies";
		public static final String CONTENT_ITEM_TYPE = 
			"vnd.android.cursor.item/vnd.akop.bach.psn-trophies";
		
		public static final String GAME_ID = "GameId";
		public static final String TITLE = "Title";
		public static final String DESCRIPTION = "Description";
		public static final String ICON_URL = "IconUrl";
		public static final String EARNED = "Earned";
		public static final String EARNED_TEXT = "EarnedText";
		public static final String TYPE = "Type";
		public static final String IS_SECRET = "IsSecret";
		public static final String SORT_ORDER = "SortOrder";
		
		public static final String DEFAULT_SORT_ORDER = EARNED + " DESC, "
				+ SORT_ORDER + " ASC";
	}
	
	public static final class Friends implements BaseColumns
	{
		private Friends() {}
		
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ PsnProvider.AUTHORITY + "/friends");
		
		public static final String CONTENT_TYPE = 
			"vnd.android.cursor.dir/vnd.akop.bach.psn-friend";
		public static final String CONTENT_ITEM_TYPE = 
			"vnd.android.cursor.item/vnd.akop.bach.psn-friend";
		
		public static final String ACCOUNT_ID = "AccountId";
		public static final String ONLINE_ID = "OnlineId";
		public static final String ICON_URL = "IconUrl";
		public static final String LEVEL = "Level";
		public static final String PROGRESS = "Progress";
		public static final String ONLINE_STATUS = "Status";
		public static final String PLAYING = "Playing";
		public static final String IS_FAVORITE = "IsFavorite";
		public static final String MEMBER_TYPE = "MemberType";
		public static final String COMMENT = "Comment";
		public static final String DELETE_MARKER = "DeleteMarker";
		public static final String LAST_UPDATED = "LastUpdated";
		
		public static final String TROPHIES_PLATINUM = "PlatinumTrophies";
		public static final String TROPHIES_GOLD = "GoldTrophies";
		public static final String TROPHIES_SILVER = "SilverTrophies";
		public static final String TROPHIES_BRONZE = "BronzeTrophies";
		
		public static final String DEFAULT_SORT_ORDER = 
			ONLINE_STATUS + " ASC," + ONLINE_ID + " COLLATE NOCASE ASC";
		
		public static int getActiveFriendCount(Context context, PsnAccount account)
		{
			Cursor cursor = context.getContentResolver().query(
					CONTENT_URI,
					new String[] { _ID },
					ACCOUNT_ID + "=" + account.getId() + " AND (" + ONLINE_STATUS
							+ "=" + STATUS_ONLINE + " OR " + ONLINE_STATUS + "="
							+ "=" + STATUS_AWAY + ")", null, null);
			
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
		
		public static String getOnlineId(Context context, long friendId)
		{
			Cursor cursor = context.getContentResolver().query(
					ContentUris.withAppendedId(CONTENT_URI, friendId),
					new String[] { ONLINE_ID }, null, null, null);
			
			if (cursor != null)
			{
				try
				{
					if (cursor.moveToFirst())
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
					cursor.close();
				}
			}
			
			return null;
		}
		
		public static long[] getOnlineFriendIds(Context context, 
				PsnAccount account)
		{
			List<Long> onlineFriends = new ArrayList<Long>();
			
			if (account != null)
			{
				int notifMode = account.getFriendNotifications();
				if (notifMode != PsnAccount.FRIEND_NOTIFY_OFF)
				{
					String selection = "";
					if (notifMode == PsnAccount.FRIEND_NOTIFY_FAVORITES)
						selection += " AND " + IS_FAVORITE + "!=0";
					
					Cursor cursor = context.getContentResolver().query(CONTENT_URI,
							new String[] { _ID },
							ACCOUNT_ID + "=" + account.getId() 
							+ " AND (" + ONLINE_STATUS + "=" + STATUS_ONLINE
							+ " OR " + ONLINE_STATUS + "=" + STATUS_AWAY + ")"
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
			
			long[] array = new long[onlineFriends.size()];
			for (int i = 0, n = onlineFriends.size(); i < n; i++)
				array[i] = onlineFriends.get(i);
			
			return array;
		}
		
		public static long getAccountId(Context context, long friendId)
		{
			Cursor cursor = context.getContentResolver().query(CONTENT_URI,
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
	}
	
	public static final class GameInfo implements Serializable
	{
		private static final long serialVersionUID = -6632154234709471532L;
		
		public static final String UNLOCKED_ALL = "UnlockedAll";
		
		public static Map<String, Object> create(Context context,
				long id,
				String uid,
				String title,
				int unlockedPlatinum, 
				int unlockedGold, 
				int unlockedSilver, 
				int unlockedBronze, 
				int progress,
				String iconUrl)
		{
			Map<String, Object> info = new HashMap<String, Object>();
			
			info.put(Games._ID, id);
			info.put(Games.UID, uid);
			info.put(Games.TITLE, title);
			info.put(Games.UNLOCKED_BRONZE, unlockedBronze + "");
			info.put(Games.UNLOCKED_SILVER, unlockedSilver + "");
			info.put(Games.UNLOCKED_GOLD, unlockedGold + "");
			info.put(Games.UNLOCKED_PLATINUM, unlockedPlatinum + "");
			info.put(UNLOCKED_ALL, unlockedPlatinum + unlockedGold + 
					unlockedSilver + unlockedBronze + "");
			info.put(Games.PROGRESS, progress);
			info.put(Games.ICON_URL, iconUrl);
			
			return info;
		}
	}
	
	public static final class TrophyInfo implements Serializable
	{
		private static final long serialVersionUID = 1730091357173766880L;
		
		public static Map<String, Object> create(Context context,
				String title,
				String description,
				long earned, 
				String earnedText, 
				int type, 
				boolean isSecret,
				String iconUrl)
		{
			Map<String, Object> info = new HashMap<String, Object>();
			
			info.put(Trophies.EARNED, (earnedText != null) 
					? earnedText 
					: getTrophyUnlockString(context, earned));
			info.put(Trophies.TITLE, title);
			info.put(Trophies.DESCRIPTION, description);
			info.put(Trophies.TYPE, type);
			info.put(Trophies.IS_SECRET, isSecret);
			info.put(Trophies.ICON_URL, iconUrl);
			
			return info;
		}
	}
	
	public static final class FriendInfo implements Serializable
	{
		private static final long serialVersionUID = 8782373061525911967L;
		
		public static final String TROPHIES_TOTAL = "TotalTrophies";
		public static final String LEVEL_PROGRESS = "LevelAndProgress";
		
		public static Map<String, Object> create(Context context,
				long id,
				String onlineId,
				int level,
				int progress, 
				int status, 
				int trophiesBronze,
				int trophiesSilver,
				int trophiesGold,
				int trophiesPlatinum,
				String playing,
				String iconUrl)
		{
			Map<String, Object> info = new HashMap<String, Object>();
			
			info.put(Friends._ID, id);
			info.put(Friends.ONLINE_ID, onlineId);
			info.put(FriendInfo.LEVEL_PROGRESS, level + "");
			info.put(Friends.ONLINE_STATUS, status);
			info.put(Friends.TROPHIES_BRONZE, trophiesBronze + "");
			info.put(Friends.TROPHIES_SILVER, trophiesSilver + "");
			info.put(Friends.TROPHIES_GOLD, trophiesGold + "");
			info.put(Friends.TROPHIES_PLATINUM, trophiesPlatinum + "");
			info.put(Friends.ICON_URL, iconUrl);
			info.put(TROPHIES_TOTAL, (trophiesPlatinum + 
					trophiesGold + trophiesSilver + trophiesBronze) + "");
			
			if (playing != null && playing.trim().length() > 0)
			{
				info.put(Friends.PLAYING, playing);
			}
			else
			{
				if (status == STATUS_OFFLINE)
					info.put(Friends.PLAYING, 
							context.getString(R.string.currently_offline));
				else
					info.put(Friends.PLAYING, 
							context.getString(R.string.current_activity_unknown));
			}
			
			return info;
		}
	}
	
	public static final class GameCatalogItemDetails implements Serializable
	{
        private static final long serialVersionUID = -1375286831255305287L;
        
		public String Title = null;
		public String Description = null;
		public String BoxartUrl = null;
		
		public String ReleaseDate = null;
		public long ReleaseDateTicks = 0;
		public String Platform = null;
		public String Genre = null;
		public String Rating = null;
		public String Players = null;
		public String OnlinePlayers = null;
		public String Publisher = null;
		
		public String[] ScreenshotsThumb = null;
		public String[] ScreenshotsLarge = null;
		
		public static GameCatalogItemDetails fromItem(GameCatalogItem item)
		{
			GameCatalogItemDetails details = new GameCatalogItemDetails();
			
			details.Title = item.Title;
			details.BoxartUrl = item.BoxartUrl;
			details.ReleaseDate = item.ReleaseDate;
			details.ReleaseDateTicks = item.ReleaseDateTicks;
			details.Platform = item.Platform;
			details.Genre = item.Genre;
			details.Players = item.Players;
			details.OnlinePlayers = item.OnlinePlayers;
			details.Publisher = item.Publisher;
			
			return details;
		}
	}
	
	public static final class GameCatalogList
	{
		public List<GameCatalogItem> Items = new ArrayList<GameCatalogItem>();
		public int PageNumber = 0;
		public int PageSize = 0;
		public boolean MorePages = false;
	}
	
	public static final class GameCatalogItem implements Serializable
	{
		private static final long serialVersionUID = -2084280535411809578L;
		
		public String Title = null;
		public String DetailUrl = null;
		public String BoxartUrl = null;
		
		public long ReleaseDateTicks = 0;
		public String ReleaseDate = null;
		public String Platform = null;
		public String Genre = null;
		public String Players = null;
		public String OnlinePlayers = null;
		public String Publisher = null;
	}
	
	public static final class ComparedGameCursor extends
	        SerializableMatrixCursor
	{
		private static final long serialVersionUID = -2907587481795955092L;
		
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ PsnProvider.AUTHORITY + "/compared_games");
		
		public static final String _ID = BaseColumns._ID;
		public static final String UID = "Uid";
		public static final String TITLE = "GameTitle"; 
		public static final String ICON_URL = "GameIconUrl";
		
		public static final String SELF_PLAYED = "SelfPlayed"; 
		public static final String SELF_PLATINUM = "SelfPlatinum";
		public static final String SELF_GOLD = "SelfGold";
		public static final String SELF_SILVER = "SelfSilver";
		public static final String SELF_BRONZE = "SelfBronze";
		public static final String SELF_PROGRESS = "SelfProgress";
		
		public static final String OPP_PLAYED = "OppPlayed"; 
		public static final String OPP_PLATINUM = "OppPlatinum";
		public static final String OPP_GOLD = "OppGold";
		public static final String OPP_SILVER = "OppSilver";
		public static final String OPP_BRONZE = "OppBronze";
		public static final String OPP_PROGRESS = "OppProgress";
		
		public static final int COLUMN_UID = 1; 
		public static final int COLUMN_TITLE = 2; 
		public static final int COLUMN_ICON_URL = 3;
		
		public static final int COLUMN_SELF_PLAYED = 4; 
		public static final int COLUMN_SELF_PLATINUM = 5;
		public static final int COLUMN_SELF_GOLD = 6;
		public static final int COLUMN_SELF_SILVER = 7;
		public static final int COLUMN_SELF_BRONZE = 8;
		public static final int COLUMN_SELF_PROGRESS = 9;
		
		public static final int COLUMN_OPP_PLAYED = 10; 
		public static final int COLUMN_OPP_PLATINUM = 11;
		public static final int COLUMN_OPP_GOLD = 12;
		public static final int COLUMN_OPP_SILVER = 13;
		public static final int COLUMN_OPP_BRONZE = 14;
		public static final int COLUMN_OPP_PROGRESS = 15;
		
		private static final String[] KEYS = 
		{
			_ID,
			UID,
			TITLE,
			ICON_URL,
			
			SELF_PLAYED,
			SELF_PLATINUM,
			SELF_GOLD,
			SELF_SILVER,
			SELF_BRONZE,
			SELF_PROGRESS,
			
			OPP_PLAYED,
			OPP_PLATINUM,
			OPP_GOLD,
			OPP_SILVER,
			OPP_BRONZE,
			OPP_PROGRESS,
		};
		
		public ComparedGameCursor(ContentResolver cr)
        {
			super(KEYS, 200);
			
			setNotificationUri(cr, CONTENT_URI);
        }
		
		public void addItem(String uid, String title, String iconUrl, 
				boolean selfPlayed, int selfPlatinum, int selfGold,
				int selfSilver, int selfBronze, int selfProgress,
				boolean oppPlayed, int oppPlatinum, int oppGold,
				int oppSilver, int oppBronze, int oppProgress)
		{
			this.newRow()
				.add(getCount())
				.add(uid)
				.add(title)
				.add(iconUrl)
				.add(selfPlayed ? 1 : 0)
				.add(selfPlatinum)
				.add(selfGold)
				.add(selfSilver)
				.add(selfBronze)
				.add(selfProgress)
				.add(oppPlayed ? 1 : 0)
				.add(oppPlatinum)
				.add(oppGold)
				.add(oppSilver)
				.add(oppBronze)
				.add(oppProgress);
		}
	}
	
	public static final class ComparedGameInfo implements Serializable
	{
		private static final long serialVersionUID = 8207146826841457602L;
		
		public String myAvatarIconUrl;
		public String yourAvatarIconUrl;
		public ComparedGameCursor cursor;
		
		public ComparedGameInfo(ContentResolver cr)
		{
			myAvatarIconUrl = null;
			yourAvatarIconUrl = null;
			cursor = new ComparedGameCursor(cr);
		}
	}
	
	public static final class ComparedTrophyCursor extends
			SerializableMatrixCursor
	{
		private static final long serialVersionUID = 4789169124631087663L;
		
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ PsnProvider.AUTHORITY + "/compared_trophies");
		
		public static final String _ID = BaseColumns._ID;
		public static final String TITLE = "Title";
		public static final String DESCRIPTION = "Description";
		public static final String ICON_URL = "IconUrl";
		public static final String TYPE = "Type";
		public static final String IS_SECRET = "IsSecret";
		public static final String IS_LOCKED = "IsLocked";
		public static final String SELF_EARNED = "SelfEarned";
		public static final String OPP_EARNED = "OppEarned";
		
		public static final int COLUMN_TITLE = 1;
		public static final int COLUMN_DESCRIPTION = 2;
		public static final int COLUMN_ICON_URL = 3;

		public static final int COLUMN_TYPE = 4;
		public static final int COLUMN_IS_SECRET = 5;
		public static final int COLUMN_IS_LOCKED = 6;
		
		public static final int COLUMN_SELF_EARNED = 7;
		public static final int COLUMN_OPP_EARNED = 8;
		
		private static final String[] KEYS =
		{ 
			_ID, 
			TITLE,
			DESCRIPTION,
			ICON_URL,
			TYPE,
			IS_SECRET,
			IS_LOCKED,
			SELF_EARNED,
			OPP_EARNED,
		};
		
		public ComparedTrophyCursor(ContentResolver cr)
		{
			super(KEYS, 200);
			
			setNotificationUri(cr, CONTENT_URI);
		}
		
		public void addItem(String title, String description, String iconUrl,
				int type, boolean isSecret, boolean isLocked, 
				String selfEarned, String oppEarned)
		{
			this.newRow()
				.add(getCount())
				.add(title)
				.add(description)
				.add(iconUrl)
				.add(type)
				.add(isSecret ? 1 : 0)
				.add(isLocked ? 1 : 0)
				.add(selfEarned)
				.add(oppEarned);
		}
	}
	
	public static final class ComparedTrophyInfo implements Serializable
	{
		private static final long serialVersionUID = 5525628855466576498L;
		
		public ComparedTrophyCursor cursor;
		
		public ComparedTrophyInfo(ContentResolver cr)
		{
			cursor = new ComparedTrophyCursor(cr);
		}
	}
	
	public static final class GamerProfileInfo implements Serializable
	{
		private static final long serialVersionUID = -8955829115533298714L;
		
		public String OnlineId;
		public String AvatarUrl;
		public int Level;
		public int Progress;
		public int OnlineStatus;
		public int PlatinumTrophies;
		public int GoldTrophies;
		public int SilverTrophies;
		public int BronzeTrophies;
		public String Playing;
	}
	
	public static String getOnlineStatusDescription(Context context, int onlineStatus)
	{
		if (onlineStatus < 0 || onlineStatus >= STATUS_DESC_RESID.length)
			return null;
		
		return context.getResources().getString(STATUS_DESC_RESID[onlineStatus]);
	}
	
	public static String getTrophyUnlockString(Context context, long timestamp)
	{
		if (timestamp <= 0)
			return context.getString(R.string.trophy_locked);
		else
		{
			return context.getString(R.string.trophies_earned_format,
					DateFormat.getMediumDateFormat(context).format(timestamp),
					DateFormat.getTimeFormat(context).format(timestamp));
		}
	}
	
	public static String getShortTrophyUnlockString(Context context, long timestamp)
	{
		if (timestamp <= 0)
			return context.getString(R.string.trophy_locked);
		else
		{
			return context.getString(R.string.trophies_earned_format,
					DateFormat.getDateFormat(context).format(timestamp), "");
					//,DateFormat.getTimeFormat(context).format(timestamp));
		}
	}
}
