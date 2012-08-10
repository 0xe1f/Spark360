/*
 * PsnAccount.java 
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

import java.io.IOException;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.akop.bach.activity.AuthenticatingAccountLogin;
import com.akop.bach.activity.playstation.AccountSettings;
import com.akop.bach.activity.playstation.AccountSummary;
import com.akop.bach.activity.playstation.GameList;
import com.akop.bach.parser.AuthenticationException;
import com.akop.bach.parser.ParserException;
import com.akop.bach.parser.PsnEuParser;
import com.akop.bach.parser.PsnParser;
import com.akop.bach.parser.PsnUsParser;
import com.akop.bach.service.PsnServiceClient;
import com.akop.bach.service.ServiceClient;
import com.akop.bach.util.rss.RssChannel;

public class PsnAccount
		extends AuthenticatingAccount
		implements SupportsGames, SupportsAchievements, SupportsFriends,
		SupportsCompareGames, SupportsCompareAchievements
{
	private static final long serialVersionUID = 4804600471625415833L;
	
	public static final String REGION_US = "us";
	public static final String REGION_EU = "eu";
	
	// These should match the values declared in 
	// @arrays/friend_notification_values
	public static final int FRIEND_NOTIFY_OFF = 0;
	public static final int FRIEND_NOTIFY_FAVORITES = 1;
	public static final int FRIEND_NOTIFY_ALL = 2;
	
	private boolean mDirtyOnlineId;
	private String mOnlineId;
	private boolean mDirtyRingtone;
	private String mRingtone;
	private boolean mDirtyVibrate;
	private boolean mVibrate;
	private boolean mDirtyFriendNotifications;
	private int mFriendNotifications;
	private boolean mDirtyLastSummarySync;
	private long mLastSummarySync;
	private boolean mDirtyLastGameSync;
	private long mLastGameSync;
	private boolean mDirtyLastFriendSync;
	private long mLastFriendSync;
	private boolean mDirtyPsnServer;
	private String mPsnServer;
	private boolean mDirtyLocale;
	private String mLocale;
	
	private int mConsole;
	private boolean mDirtyConsole;
	private int mSortOrder;
	private boolean mDirtySortFilter;
	private int mReleaseStatus;
	private boolean mDirtyReleaseStatus;
	
	public PsnAccount(Context context)
	{
		super(context);
		
		mOnlineId = null;
		mLastSummarySync = 0;
		mLastGameSync = 0;
		mLastFriendSync = 0;
		mRingtone = null;
		mVibrate = false;
		mPsnServer = REGION_EU;
		mLocale = REGION_US;
		mFriendNotifications = FRIEND_NOTIFY_OFF;
		
		mConsole = PSN.CATALOG_CONSOLE_PS3;
		mSortOrder = PSN.CATALOG_SORT_BY_RELEASE;
		mReleaseStatus = PSN.CATALOG_RELEASE_OUT_NOW;
	}
	
	public PsnAccount(Preferences preferences, String uuid)
	{
		super(preferences, uuid);
	}
	
	protected PsnParser createServerBasedParser(Context context)
	{
		return getPsnServer().equals(REGION_EU) ? createEuParser(context)
				: createUsParser(context);
	}
	
	public PsnParser createLocaleBasedParser(Context context)
	{
		return getLocale().equals(REGION_EU) ? createEuParser(context)
				: createUsParser(context);
	}
	
	public boolean canCompareUnknowns()
	{
		return !getPsnServer().equals(REGION_EU);
	}
	
	protected PsnParser createUsParser(Context context)
	{
		return new PsnUsParser(context);
	}
	
	protected PsnParser createEuParser(Context context)
	{
		return new PsnEuParser(context);
	}
	
	public long getLastSummaryUpdate()
	{
		return mLastSummarySync;
	}
	
	public void setOnlineId(String onlineId)
	{
		if (!TextUtils.equals(onlineId, mOnlineId))
		{
			mOnlineId = onlineId;
			mDirtyOnlineId = true;
		}
	}
	
	public int getCatalogConsole()
	{
		return mConsole;
	}
	
	public void setCatalogConsole(int value)
	{
		if (mConsole != value)
		{
			mConsole = value;
			mDirtyConsole = true;
		}
	}
	
	public int getCatalogSortOrder()
	{
		return mSortOrder;
	}
	
	public void setCatalogSortOrder(int value)
	{
		if (mSortOrder != value)
		{
			mSortOrder = value;
			mDirtySortFilter = true;
		}
	}
	
	public boolean supportsFilteringByReleaseDate()
	{
		return REGION_EU.equals(mLocale);
	}
	
	public int getCatalogReleaseStatus()
	{
		return mReleaseStatus;
	}
	
	public void setCatalogReleaseStatus(int value)
	{
		if (mReleaseStatus != value)
		{
			mReleaseStatus = value;
			mDirtyReleaseStatus = true;
		}
	}
	
	public void setLastSummaryUpdate(long ms)
	{
		if (ms != mLastSummarySync)
		{
			mLastSummarySync = ms;
			mDirtyLastSummarySync = true;
		}
	}
	
	public long getSummaryRefreshInterval()
	{
		return 1 * DateUtils.HOUR_IN_MILLIS;
	}
	
	@Override
	protected void onSave(Preferences p, SharedPreferences.Editor editor)
	{
		super.onSave(p, editor);
		
		if (mDirtyOnlineId)
			editor.putString(mUuid + ".onlineId", mOnlineId);
		if (mDirtyLastSummarySync)
			editor.putLong(mUuid + ".lastSummarySync", mLastSummarySync);
		if (mDirtyLastGameSync)
			editor.putLong(mUuid + ".lastGameSync", mLastGameSync);
		if (mDirtyLastFriendSync)
			editor.putLong(mUuid + ".lastFriendSync", mLastFriendSync);
		if (mDirtyRingtone)
			editor.putString(mUuid + ".ringtone", mRingtone);
		if (mDirtyVibrate)
			editor.putBoolean(mUuid + ".vibrate", mVibrate);
		if (mDirtyFriendNotifications)
			editor.putInt(mUuid + ".friendNotifs", mFriendNotifications);
		if (mDirtyPsnServer)
			editor.putString(mUuid + ".psnServer", mPsnServer);
		if (mDirtyLocale)
			editor.putString(mUuid + ".locale", mLocale);
		
		if (mDirtyConsole)
			editor.putInt(mUuid + ".catConsole", mConsole);
		if (mDirtySortFilter)
			editor.putInt(mUuid + ".catSort", mSortOrder);
		if (mDirtyReleaseStatus)
			editor.putInt(mUuid + ".catRelease", mReleaseStatus);
	}
	
	@Override
	protected void onLoad(Preferences preferences)
	{
		super.onLoad(preferences);
		
		mOnlineId = preferences.getString(mUuid + ".onlineId", null);
		mLastSummarySync = preferences.getLong(mUuid + ".lastSummarySync", 0);
		mLastGameSync = preferences.getLong(mUuid + ".lastGameSync", 0);
		mLastFriendSync = preferences.getLong(mUuid + ".lastFriendSync", 0);
		mRingtone = preferences.getString(mUuid + ".ringtone", null);
		mVibrate = preferences.getBoolean(mUuid + ".vibrate", false);
		mFriendNotifications = preferences.getInt(mUuid + ".friendNotifs", 
				FRIEND_NOTIFY_OFF);
		mPsnServer = preferences.getString(mUuid + ".psnServer", REGION_EU);
		mLocale = preferences.getString(mUuid + ".locale", mPsnServer); // same as server
		
		mConsole = preferences.getInt(mUuid + ".catConsole", PSN.CATALOG_CONSOLE_PS3);
		mSortOrder = preferences.getInt(mUuid + ".catSort", PSN.CATALOG_SORT_BY_RELEASE);
		mReleaseStatus = preferences.getInt(mUuid + ".catRelease", PSN.CATALOG_RELEASE_OUT_NOW);
	}
	
	@Override
	public String getDescription(Context context)
	{
		return context.getString(R.string.playstation_network);
	}
	
	@Override
	public String getScreenName()
	{
		return mOnlineId;
	}
	
	@Override
	public void open(Context context)
	{
		AccountSummary.actionShow(context, this);
	}
	
	@Override
	public void editLogin(Activity context)
	{
		AuthenticatingAccountLogin.actionEditLoginData(context, this);
	}
	
	@Override
	public ContentValues validate(Context context) throws AuthenticationException,
			IOException, ParserException
	{
		PsnParser p = createEuParser(context);
		
		try
		{
			return p.validateAccount(this);
		}
		finally
		{
			p.dispose();
		}
	}

	@Override
	public void cleanUp(Context context)
	{
		PsnParser p = createEuParser(context);
		
		try
		{
			p.deleteAccount(this);
		}
		finally
		{
			p.dispose();
		}
	}
	
	@Override
	public void updateProfile(Context context) throws AuthenticationException,
			IOException, ParserException
	{
		PsnParser p = createEuParser(context);
		
		try
		{
			p.fetchSummary(this);
		}
		finally
		{
			p.dispose();
		}
	}
	
	@Override
	public void edit(Context context)
	{
		AccountSettings.actionEditSettings(context, this);
	}
	
	@Override
	public void create(Context context, ContentValues cv)
	{
		PsnParser p = createEuParser(context);
		
		try
		{
			p.createAccount(this, cv);
		}
		finally
		{
			p.dispose();
		}
	}
	
	// ISupportsGames
	
	@Override
	public void updateGames(Context context)
			throws AuthenticationException, IOException, ParserException
	{
		PsnParser p = createServerBasedParser(context);
		
		try
		{
			p.fetchGames(this);
		}
		finally
		{
			p.dispose();
		}
	}
	
	@Override
	public long getLastGameUpdate()
	{
		return mLastGameSync;
	}
	
	@Override
	public void setLastGameUpdate(long ms)
	{
		if (ms != mLastGameSync)
		{
			mLastGameSync = ms;
			mDirtyLastGameSync = true;
		}
	}
	
	@Override
	public long getGameHistoryRefreshInterval()
	{
		return 1 * DateUtils.HOUR_IN_MILLIS;
	}
	
	@Override
	public void updateAchievements(Context context, Object gameId)
			throws AuthenticationException, IOException, ParserException
	{
		PsnParser p = createServerBasedParser(context);
		
		try
		{
			p.fetchTrophies(this, (Long)gameId);
		}
		finally
		{
			p.dispose();
		}
	}
	
	// ISupportsFriends
	
	@Override
	public void updateFriends(Context context)
			throws AuthenticationException, IOException, ParserException
	{
		PsnParser p = createEuParser(context);
		
		try
		{
			p.fetchFriends(this);
		}
		finally
		{
			p.dispose();
		}
	}
	
	@Override
	public void updateFriendProfile(Context context, Object friendId)
			throws AuthenticationException, IOException, ParserException
	{
		PsnParser p = createEuParser(context);
		
		try
		{
			p.fetchFriendSummary(this, (String)friendId);
		}
		finally
		{
			p.dispose();
		}
	}
	
	@Override
	public long getLastFriendUpdate()
	{
		return mLastFriendSync;
	}
	
	@Override
	public void setLastFriendUpdate(long ms)
	{
		if (ms != mLastFriendSync)
		{
			mLastFriendSync = ms;
			mDirtyLastFriendSync = true;
		}
	}
	
	@Override
	public long getFriendRefreshInterval()
	{
		return 5 * DateUtils.MINUTE_IN_MILLIS;
	}
	
	@Override
	public Object compareGames(Context context, Object userId)
			throws AuthenticationException, IOException, ParserException
	{
		PsnParser p = createServerBasedParser(context);
		
		try
		{
			return p.compareGames(this, (String)userId);
		}
		finally
		{
			p.dispose();
		}
	}
	
	@Override
	public Object compareAchievements(Context context, Object userId, Object gameId)
			throws AuthenticationException, IOException, ParserException
	{
		PsnParser p = createServerBasedParser(context);
		
		try
		{
			return p.compareTrophies(this, (String)userId, (String)gameId);
		}
		finally
		{
			p.dispose();
		}
	}
	
	public RssChannel getBlog(Context context)
			throws ParserException
	{
		PsnParser p = createLocaleBasedParser(context);
		
		try
		{
			return p.fetchBlog();
		}
		finally
		{
			p.dispose();
		}
	}
	
	@Override
	protected void onClearDirtyFlags()
	{
		super.onClearDirtyFlags();
		
		mDirtyOnlineId = false;
		mDirtyLastGameSync = false;
		mDirtyLastFriendSync = false;
		mDirtyLastSummarySync = false;
		mDirtyRingtone = false;
		mDirtyVibrate = false;
		mDirtyFriendNotifications = false;
		mDirtyPsnServer = false;
		mDirtyLocale = false;
		
		mDirtyConsole = false;
		mDirtyReleaseStatus = false;
		mDirtySortFilter = false;
	}
	
	private static Pattern AVATAR_USUAL = Pattern.compile("_s.png$");
	private static Pattern AVATAR_AKUMA = Pattern.compile("[^_]s.png$");
	
	public String getLargeAvatar(String avatarUrl)
	{
		if (avatarUrl == null || !avatarUrl.contains("/avatar_s/"))
			return avatarUrl;
		
		String largeAvatarUrl = avatarUrl.replace("/avatar_s/", "/avatar/");
		
		if (AVATAR_USUAL.matcher(avatarUrl).find())
			return largeAvatarUrl.replace("_s.png", ".png");
		else if (AVATAR_AKUMA.matcher(avatarUrl).find())
			return largeAvatarUrl.replace("s.png", "l.png");
		
		return avatarUrl;
	}
	
	public String getRingtone()
	{
		return mRingtone;
	}
	
	public void setRingtone(String ringtone)
	{
		if (!TextUtils.equals(ringtone, mRingtone))
		{
			mRingtone = ringtone;
			mDirtyRingtone = true;
		}
	}
	
	public String getPsnServer()
	{
		return mPsnServer;
	}
	
	public void setPsnServer(String region)
	{
		if (region != mPsnServer)
		{
			mPsnServer = region;
			mDirtyPsnServer = true;
		}
	}
	
	public String getLocale()
	{
		return mLocale;
	}
	
	public void setLocale(String region)
	{
		if (region != mLocale)
		{
			mLocale = region;
			mDirtyLocale = true;
		}
	}
	
	public Uri getRingtoneUri()
	{
		if (mRingtone == null)
			return null;
		
		return Uri.parse(mRingtone);
	}
	
	public boolean isVibrationEnabled()
	{
		return mVibrate;
	}
	
	public void setVibration(boolean vibrate)
	{
		if (vibrate != mVibrate)
		{
			mVibrate = vibrate;
			mDirtyVibrate = true;
		}
	}
	
	public int getFriendNotifications()
	{
		return mFriendNotifications;
	}
	
	public void setFriendNotifications(int friendNotifications)
	{
		if (friendNotifications != mFriendNotifications)
		{
			mFriendNotifications = friendNotifications;
			mDirtyFriendNotifications = true;
		}
	}
	
	@Override
	public void actionShowGames(Context context)
	{
		GameList.actionShow(context, this);
	}
	
	@Override
	public ServiceClient createServiceClient() 
	{
		return new PsnServiceClient();
	}
}
