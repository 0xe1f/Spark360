/*
 * XboxLiveAccount.java 
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

package com.akop.bach;

import java.io.IOException;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.akop.bach.XboxLive.Friends;
import com.akop.bach.XboxLive.Profiles;
import com.akop.bach.activity.AuthenticatingAccountLogin;
import com.akop.bach.activity.xboxlive.AccountSettings;
import com.akop.bach.activity.xboxlive.AccountSummary;
import com.akop.bach.activity.xboxlive.FriendList;
import com.akop.bach.activity.xboxlive.GameList;
import com.akop.bach.activity.xboxlive.MessageCompose;
import com.akop.bach.activity.xboxlive.MessageView;
import com.akop.bach.parser.AuthenticationException;
import com.akop.bach.parser.ParserException;
import com.akop.bach.parser.XboxLiveParser;
import com.akop.bach.service.ServiceClient;
import com.akop.bach.service.XboxLiveServiceClient;

public class XboxLiveAccount
		extends AuthenticatingAccount
		implements SupportsGames, SupportsAchievements,
		SupportsCompareGames, SupportsCompareAchievements,
		SupportsMessaging, SupportsFriendManagement
{
	// These should match the values declared in 
	// @arrays/friend_notification_values
	public static final int FRIEND_NOTIFY_OFF = 0;
	public static final int FRIEND_NOTIFY_FAVORITES = 1;
	public static final int FRIEND_NOTIFY_ALL = 2;
	
	public static final int COVERFLOW_OFF = 0;
	public static final int COVERFLOW_IN_LANDSCAPE = 1;
	public static final int COVERFLOW_ALWAYS = 2;
	
	private boolean mDirtyGamertag;
	private String mGamertag;
	private boolean mDirtyLastGameSync;
	private long mLastGameSync;
	private boolean mDirtyLastFriendSync;
	private long mLastFriendSync;
	private boolean mDirtyLastMessageSync;
	private long mLastMessageSync;
	private boolean mDirtyLastSummarySync;
	private long mLastSummarySync;
	private boolean mDirtyIsGold;
	private boolean mIsGold;
	private boolean mDirtyRingtone;
	private String mRingtone;
	private boolean mDirtyVibrate;
	private boolean mVibrate;
	private boolean mDirtyFriendNotifications;
	private int mFriendNotifications;
	private boolean mDirtyMessageNotifications;
	private boolean mMessageNotifications;
	private boolean mDirtyBeaconNotifications;
	private int mBeaconNotifications;
	private boolean mDirtyCoverflowMode;
	private int mCoverflowMode;
	private boolean mShowApps;
	private boolean mDirtyShowApps;
	
	public XboxLiveAccount(Context context)
	{
		super(context);
		
		mGamertag = null;
		mLastGameSync = 0;
		mLastFriendSync = 0;
		mLastMessageSync = 0;
		mLastSummarySync = 0;
		mIsGold = false;
		mRingtone = null;
		mVibrate = false;
		mFriendNotifications = FRIEND_NOTIFY_OFF;
		mBeaconNotifications = FRIEND_NOTIFY_OFF;
		mMessageNotifications = true;
		mCoverflowMode = COVERFLOW_IN_LANDSCAPE;
		mShowApps = true;
	}
	
	public XboxLiveAccount(Preferences preferences, String uuid)
	{
		super(preferences, uuid);
	} 
	
	public String getGamertag()
	{
		return mGamertag;
	}
	
	@Override
	public long getLastMessageUpdate()
	{
		return mLastMessageSync;
	}
	
	public long getLastSummaryUpdate()
	{
		return mLastSummarySync;
	}
	
	public boolean isGold()
	{
		return mIsGold;
	}
	
	public boolean isShowingApps()
	{
		return mShowApps;
	}
	
	public int getFriendNotifications()
	{
		return mFriendNotifications;
	}
	
	public int getBeaconNotifications()
	{
		return mBeaconNotifications;
	}
	
	public int getCoverflowMode()
	{
		return mCoverflowMode;
	}
	
	public boolean isMessageNotificationEnabled()
	{
		return mMessageNotifications;
	}
	
	public long getSummaryRefreshInterval()
	{
		return 1 * DateUtils.HOUR_IN_MILLIS;
	}
	
	public long getGameHistoryRefreshInterval()
	{
		return 1 * DateUtils.HOUR_IN_MILLIS;
	}
	
	@Override
	public long getMessageRefreshInterval()
	{
		return 5 * DateUtils.MINUTE_IN_MILLIS;
	}
	
	public String getRingtone()
	{
		return mRingtone;
	}
	
	public boolean isVibrationEnabled()
	{
		return mVibrate;
	}
	
	public Uri getRingtoneUri()
	{
		if (mRingtone == null)
			return null;
		
		return Uri.parse(mRingtone);
	}
	
	public void setGamertag(String gamertag)
	{
		if (!TextUtils.equals(gamertag, mGamertag))
		{
			mGamertag = gamertag;
			mDirtyGamertag = true;
		}
	}
	
	public void setGoldStatus(boolean value)
	{
		if (value != mIsGold)
		{
			mIsGold = value;
			mDirtyIsGold = true;
		}
	}
	
	public void setLastMessageUpdate(long ms)
	{
		if (ms != mLastMessageSync)
		{
			mLastMessageSync = ms;
			mDirtyLastMessageSync = true;
		}
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
	
	public void setLastSummaryUpdate(long ms)
	{
		if (ms != mLastSummarySync)
		{
			mLastSummarySync = ms;
			mDirtyLastSummarySync = true;
		}
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
	
	public void setRingtone(String ringtone)
	{
		if (!TextUtils.equals(ringtone, mRingtone))
		{
			mRingtone = ringtone;
			mDirtyRingtone = true;
		}
	}
	
	public void setShowApps(boolean showApps)
	{
		if (mShowApps != showApps)
		{
			mShowApps = showApps;
			mDirtyShowApps = true;
		}
	}
	
	public void setVibration(boolean vibrate)
	{
		if (vibrate != mVibrate)
		{
			mVibrate = vibrate;
			mDirtyVibrate = true;
		}
	}
	
	public void setFriendNotifications(int friendNotifications)
	{
		if (friendNotifications != mFriendNotifications)
		{
			mFriendNotifications = friendNotifications;
			mDirtyFriendNotifications = true;
		}
	}
	
	public void setBeaconNotifications(int beaconNotifications)
	{
		if (beaconNotifications != mBeaconNotifications)
		{
			mBeaconNotifications = beaconNotifications;
			mDirtyBeaconNotifications = true;
		}
	}
	
	public void setMessageNotifications(boolean messageNotifications)
	{
		if (messageNotifications != mMessageNotifications)
		{
			mMessageNotifications = messageNotifications;
			mDirtyMessageNotifications = true;
		}
	}
	
	public void setCoverflowMode(int mode)
	{
		if (mode != mCoverflowMode)
		{
			mCoverflowMode = mode;
			mDirtyCoverflowMode = true;
		}
	}
	
	@Override
	protected void onSave(Preferences p, SharedPreferences.Editor editor)
	{
		super.onSave(p, editor);
		
		if (mDirtyGamertag)
			editor.putString(mUuid + ".gamertag", mGamertag);
		if (mDirtyLastGameSync)
			editor.putLong(mUuid + ".lastGameSync", mLastGameSync);
		if (mDirtyLastFriendSync)
			editor.putLong(mUuid + ".lastFriendSync", mLastFriendSync);
		if (mDirtyLastMessageSync)
			editor.putLong(mUuid + ".lastMessageSync", mLastMessageSync);
		if (mDirtyLastSummarySync)
			editor.putLong(mUuid + ".lastSummarySync", mLastSummarySync);
		if (mDirtyIsGold)
			editor.putBoolean(mUuid + ".isGold", mIsGold);
		if (mDirtyRingtone)
			editor.putString(mUuid + ".ringtone", mRingtone);
		if (mDirtyVibrate)
			editor.putBoolean(mUuid + ".vibrate", mVibrate);
		if (mDirtyFriendNotifications)
			editor.putInt(mUuid + ".friendNotifs", mFriendNotifications);
		if (mDirtyBeaconNotifications)
			editor.putInt(mUuid + ".beaconNotifs", mBeaconNotifications);
		if (mDirtyMessageNotifications)
			editor.putBoolean(mUuid + ".messageNotifs", mMessageNotifications);
		if (mDirtyCoverflowMode)
			editor.putInt(mUuid + ".coverflow", mCoverflowMode);
		if (mDirtyShowApps)
			editor.putBoolean(mUuid + ".showApps", mShowApps);
	}

	@Override
	protected void onLoad(Preferences preferences)
	{
		super.onLoad(preferences);
		
		mGamertag = preferences.getString(mUuid + ".gamertag", null);
		mLastGameSync = preferences.getLong(mUuid + ".lastGameSync", 0);
		mLastFriendSync = preferences.getLong(mUuid + ".lastFriendSync", 0);
		mLastMessageSync = preferences.getLong(mUuid + ".lastMessageSync", 0);
		mLastSummarySync = preferences.getLong(mUuid + ".lastSummarySync", 0);
		mIsGold = preferences.getBoolean(mUuid + ".isGold", false);
		mRingtone = preferences.getString(mUuid + ".ringtone", null);
		mVibrate = preferences.getBoolean(mUuid + ".vibrate", false);
		mFriendNotifications = preferences.getInt(mUuid + ".friendNotifs", 
				FRIEND_NOTIFY_OFF);
		mBeaconNotifications = preferences.getInt(mUuid + ".beaconNotifs", 
				FRIEND_NOTIFY_OFF);
		mMessageNotifications = preferences.getBoolean(mUuid + ".messageNotifs", 
				true);
		mCoverflowMode = preferences.getInt(mUuid + ".coverflow", 
				COVERFLOW_IN_LANDSCAPE);
		mShowApps = preferences.getBoolean(mUuid + ".showApps", true);
	}
	
	@Override
	protected void onClearDirtyFlags()
	{
		super.onClearDirtyFlags();
		
		mDirtyGamertag = false;
		mDirtyLastGameSync = false;
		mDirtyLastFriendSync = false;
		mDirtyLastMessageSync = false;
		mDirtyLastSummarySync = false;
		mDirtyIsGold = false;
		mDirtyRingtone = false;
		mDirtyVibrate = false;
		mDirtyFriendNotifications = false;
		mDirtyBeaconNotifications = false;
		mDirtyMessageNotifications = false;
		mDirtyCoverflowMode = false;
		mDirtyShowApps = false;
	}
	
	@Override
	public String getDescription()
	{
		return App.getInstance().getString(R.string.xbox_live);
	}
	
	@Override
	public String getScreenName()
	{
		return mGamertag;
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
	public void actionComposeMessage(Context context, String to)
	{
		MessageCompose.actionComposeMessage(context, this, to);
	}
	
	@Override
	public void actionOpenMessage(Context context, long messageUid)
	{
		MessageView.actionShow(context, this, messageUid);
	}
	
	@Override
	public void actionShowGames(Context context)
	{
		GameList.actionShow(context, this);
	}
	
	public boolean canSendMessages()
	{
		return this.isGold();
	}
	
	@Override
	public boolean canOpenMessage(Context context, long messageUid)
	{
		Cursor c = context.getContentResolver().query(XboxLive.Messages.CONTENT_URI, 
				new String[] { XboxLive.Messages.TYPE },
				XboxLive.Messages._ID + "=" + messageUid, 
				null, null);
		
		if (c != null)
		{
			try
			{
				return (c.getInt(0) == XboxLive.MESSAGE_TEXT);
			}
			finally
			{
				c.close();
			}
		}
		
		return false;
	}
	
	// ISupportsMessaging
	
	@Override
	public void updateMessage(Context context, Object messageId)
			throws AuthenticationException, IOException, ParserException
	{
		XboxLiveParser p = new XboxLiveParser(context);
		
		try
		{
			p.fetchMessage(this, (Long)messageId);
		}
		finally
		{
			p.dispose();
		}
	}

	@Override
	public void updateMessages(Context context)
			throws AuthenticationException, IOException, ParserException
	{
		XboxLiveParser p = new XboxLiveParser(context);
		
		try
		{
			p.fetchMessages(this);
		}
		finally
		{
			p.dispose();
		}
	}

	@Override
	public void deleteMessage(Context context, Object messageId)
			throws AuthenticationException, IOException, ParserException
	{
		XboxLiveParser p = new XboxLiveParser(context);
		
		try
		{
			p.fetchDeleteMessage(this, (Long)messageId);
		}
		finally
		{
			p.dispose();
		}
	}

	@Override
	public void sendMessage(Context context, String[] recipients, String body)
			throws AuthenticationException, IOException, ParserException
	{
		XboxLiveParser p = new XboxLiveParser(context);
		
		try
		{
			p.fetchSendMessage(this, recipients, body);
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
		XboxLiveParser p = new XboxLiveParser(context);
		
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
		XboxLiveParser p = new XboxLiveParser(context);
		
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
	public long getFriendRefreshInterval()
	{
		return 5 * DateUtils.MINUTE_IN_MILLIS;
	}
	
	// ISupportsFriendManagement
	
	@Override
	public void addFriend(Context context, Object friendId)
			throws AuthenticationException, IOException, ParserException
	{
		XboxLiveParser p = new XboxLiveParser(context);
		
		try
		{
			p.addFriend(this, (String)friendId);
		}
		finally
		{
			p.dispose();
		}
	}
	
	@Override
	public void removeFriend(Context context, Object friendId)
			throws AuthenticationException, IOException, ParserException
	{
		XboxLiveParser p = new XboxLiveParser(context);
		
		try
		{
			p.removeFriend(this, (String)friendId);
		}
		finally
		{
			p.dispose();
		}
	}
	
	/*
	@Override
	public void blockFriend(Context context, Object friendId)
			throws AuthenticationException, IOException, ParserException
	{
		XboxLiveParser p = new XboxLiveParser(context);
		
		try
		{
			p.blockFriend(this, (String)friendId);
		}
		finally
		{
			p.dispose();
		}
	}
	*/
	
	public void blockMessage(Context context, long messageId)
			throws AuthenticationException, IOException, ParserException
	{
		XboxLiveParser p = new XboxLiveParser(context);
		
		try
		{
			p.fetchBlockMessage(this, messageId);
		}
		finally
		{
			p.dispose();
		}
	}
	
	@Override
	public void acceptFriendRequest(Context context, Object friendId)
			throws AuthenticationException, IOException, ParserException
	{
		XboxLiveParser p = new XboxLiveParser(context);
		
		try
		{
			p.acceptFriendRequest(this, (String)friendId);
		}
		finally
		{
			p.dispose();
		}
	}
	
	@Override
	public void cancelFriendRequest(Context context, Object friendId)
			throws AuthenticationException, IOException, ParserException
	{
		XboxLiveParser p = new XboxLiveParser(context);
		
		try
		{
			p.cancelFriendRequest(this, (String)friendId);
		}
		finally
		{
			p.dispose();
		}
	}

	@Override
	public void rejectFriendRequest(Context context, Object friendId)
			throws AuthenticationException, IOException, ParserException
	{
		XboxLiveParser p = new XboxLiveParser(context);
		
		try
		{
			p.rejectFriendRequest(this, (String)friendId);
		}
		finally
		{
			p.dispose();
		}
	}
	
	@Override
	public void updateGames(Context context)
			throws AuthenticationException, IOException, ParserException
	{
		XboxLiveParser p = new XboxLiveParser(context);
		
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
	public void updateAchievements(Context context, Object gameId)
			throws AuthenticationException, IOException, ParserException
	{
		XboxLiveParser p = new XboxLiveParser(context);
		
		try
		{
			p.fetchAchievements(this, (Long)gameId);
		}
		finally
		{
			p.dispose();
		}
	}

	@Override
	public ContentValues validate(Context context) 
		throws AuthenticationException, IOException, ParserException
	{
		XboxLiveParser p = new XboxLiveParser(context);
		
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
		XboxLiveParser p = new XboxLiveParser(context);
		
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
	public void updateProfile(Context context) 
		throws AuthenticationException, IOException, ParserException
	{
		XboxLiveParser p = new XboxLiveParser(context);
		
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
	public Object compareGames(Context context, Object userId)
		throws AuthenticationException, IOException, ParserException
	{
		XboxLiveParser p = new XboxLiveParser(context);
		
		try
		{
			return p.fetchCompareGames(this, (String)userId);
		}
		finally
		{
			p.dispose();
		}
	}

	@Override
	public Object compareAchievements(Context context, Object userId,
			Object gameId)
			throws AuthenticationException, IOException, ParserException
	{
		XboxLiveParser p = new XboxLiveParser(context);
		
		try
		{
			return p.fetchCompareAchievements(this, (String)userId, (String)gameId);
		}
		finally
		{
			p.dispose();
		}
	}
	
	@Override
	public void create(Context context, ContentValues cv)
	{
		XboxLiveParser p = new XboxLiveParser(context);
		
		try
		{
			p.createAccount(this, cv);
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
	
	public Intent getFriendIntent(Context context, String gamertag)
	{
		Intent intent;
		long friendId = XboxLive.Friends.getFriendId(context, this, gamertag);
		
		if (friendId < 0)
		{
			intent = new Intent(context, FriendList.class);
	    	intent.putExtra("account", this);
		}
		else
		{
			intent = new Intent(Intent.ACTION_VIEW, getFriendUri(friendId));
		}
		
		return intent;
	}
	
	public boolean isBeaconNotificationEnabled()
	{
		return getBeaconNotifications() != FRIEND_NOTIFY_OFF;
	}
	
	public boolean isMsnAccount()
	{
		return  getEmailAddress().endsWith("@msn.com");
	}
	
	@Override
	public ServiceClient createServiceClient() 
	{
		return new XboxLiveServiceClient();
	}
	
	@Override
	public Uri getProfileUri() 
	{
		return ContentUris.withAppendedId(Profiles.CONTENT_URI, getId());
	}
	
	@Override
	public Uri getFriendUri(long friendId) 
	{
		return ContentUris.withAppendedId(Friends.CONTENT_URI, friendId);
	}
	
	@Override
	public String getFriendScreenName(long friendId) 
	{
		return Friends.getGamertag(App.getInstance(), friendId);
	}
	
	@Override
	public Uri getFriendsUri()
	{
		return Friends.CONTENT_URI;
	}
	
	private static final String[] PROJECTION = 
	{ 
		Friends._ID,
		Friends.GAMERTAG,
		Friends.ICON_URL,
	};
	
	@Override
	public Cursor createCursor(Activity activity) 
	{
		return activity.managedQuery(getFriendsUri(), PROJECTION, 
				Friends.ACCOUNT_ID + "=" + getId(), null, null);
	}
	
	public static final Parcelable.Creator<XboxLiveAccount> CREATOR = new Parcelable.Creator<XboxLiveAccount>() 
	{
		public XboxLiveAccount createFromParcel(Parcel in) 
		{
			return new XboxLiveAccount(in);
		}
		
		public XboxLiveAccount[] newArray(int size) 
		{
			return new XboxLiveAccount[size];
		}
	};
	
	protected XboxLiveAccount(Parcel in) 
	{
		super(in);
		
		mDirtyGamertag = (in.readByte() != 0);
		mGamertag = in.readString();
		mDirtyLastGameSync = (in.readByte() != 0);
		mLastGameSync = in.readLong();
		mDirtyLastFriendSync = (in.readByte() != 0);
		mLastFriendSync = in.readLong();
		mDirtyLastMessageSync = (in.readByte() != 0);
		mLastMessageSync = in.readLong();
		mDirtyLastSummarySync = (in.readByte() != 0);
		mLastSummarySync = in.readLong();
		mDirtyIsGold = (in.readByte() != 0);
		mIsGold = (in.readByte() != 0);
		mDirtyRingtone = (in.readByte() != 0);
		mRingtone = in.readString();
		mDirtyVibrate = (in.readByte() != 0);
		mVibrate = (in.readByte() != 0);
		mDirtyFriendNotifications = (in.readByte() != 0);
		mFriendNotifications = in.readInt();
		mDirtyMessageNotifications = (in.readByte() != 0);
		mMessageNotifications = (in.readByte() != 0);
		mDirtyBeaconNotifications = (in.readByte() != 0);
		mBeaconNotifications = in.readInt();
		mDirtyCoverflowMode = (in.readByte() != 0);
		mCoverflowMode = in.readInt();
		mDirtyShowApps = (in.readByte() != 0);
		mShowApps = (in.readByte() != 0);
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) 
	{
		super.writeToParcel(dest, flags);
		
		dest.writeByte(mDirtyGamertag ? (byte)1 : 0);
		dest.writeString(mGamertag);
		dest.writeByte(mDirtyLastGameSync ? (byte)1 : 0);
		dest.writeLong(mLastGameSync);
		dest.writeByte(mDirtyLastFriendSync ? (byte)1 : 0);
		dest.writeLong(mLastFriendSync);
		dest.writeByte(mDirtyLastMessageSync ? (byte)1 : 0);
		dest.writeLong(mLastMessageSync);
		dest.writeByte(mDirtyLastSummarySync ? (byte)1 : 0);
		dest.writeLong(mLastSummarySync);
		dest.writeByte(mDirtyIsGold ? (byte)1 : 0);
		dest.writeByte(mIsGold ? (byte)1 : 0);
		dest.writeByte(mDirtyRingtone ? (byte)1 : 0);
		dest.writeString(mRingtone);
		dest.writeByte(mDirtyVibrate ? (byte)1 : 0);
		dest.writeByte(mVibrate ? (byte)1 : 0);
		dest.writeByte(mDirtyFriendNotifications ? (byte)1 : 0);
		dest.writeInt(mFriendNotifications);
		dest.writeByte(mDirtyMessageNotifications ? (byte)1 : 0);
		dest.writeByte(mMessageNotifications ? (byte)1 : 0);
		dest.writeByte(mDirtyBeaconNotifications ? (byte)1 : 0);
		dest.writeInt(mBeaconNotifications);
		dest.writeByte(mDirtyCoverflowMode ? (byte)1 : 0);
		dest.writeInt(mCoverflowMode);
		dest.writeByte(mDirtyShowApps ? (byte)1 : 0);
		dest.writeByte(mShowApps ? (byte)1 : 0);
	}
}
