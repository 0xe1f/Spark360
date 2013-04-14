/*
 * BasicAccount.java 
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
import java.lang.reflect.Constructor;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Parcel;
import android.text.TextUtils;

import com.akop.bach.parser.AuthenticationException;
import com.akop.bach.parser.ParserException;
import com.akop.bach.service.ServiceClient;

public abstract class BasicAccount implements Account
{
	public static final int AUTOSYNC_DISABLED = 0;
	public static final int AUTOSYNC_DEFAULT_PERIOD = 15;
	
	// These don't change
	protected long mId;
	protected String mUuid;
	
	private boolean mDeleted = false;
	
	// These need to be tracked when they change
	private boolean mDirtyIconUrl;
	private String mIconUrl;
	private boolean mDirtySyncPeriodMin;
	private int mSyncPeriodMin;
	
	protected BasicAccount(Context context)
	{
		mId = Preferences.get(context).reserveAccountId();
		mUuid = UUID.randomUUID().toString();
		
		mSyncPeriodMin = AUTOSYNC_DISABLED;
		mIconUrl = null;
		
		onClearDirtyFlags();
	}
	
	protected BasicAccount(Preferences preferences, String uuid)
	{
		this.mUuid = uuid;
		
		refresh(preferences);
	} 
	
	public abstract boolean isValid();
	public abstract String getLogonId();
	public abstract String getScreenName();
	public abstract String getDescription();
	public abstract Uri getProfileUri();
	
	public abstract void editLogin(Activity context);
	public abstract void open(Context context);
	
	public abstract ContentValues validate(Context context)
			throws AuthenticationException, IOException, ParserException;
	public abstract void updateProfile(Context context)
			throws AuthenticationException, IOException, ParserException;
	public abstract void cleanUp(Context context);
	
	public boolean canOpenMessage(Context context, long messageUid)
	{
		return false;
	}
	
	private static Class<?> getClass(Preferences preferences, String uuid)
	{
		String className = preferences.getSharedPreferences().getString(uuid + ".class",
				null);
		
		try
		{
			return (className == null) ? null : Class.forName(className);
		}
		catch (ClassNotFoundException e)
		{
			if (App.getConfig().logToConsole())
				e.printStackTrace();
			
			return null;
		}
	}
	
	public static BasicAccount create(Preferences preferences, String uuid)
	{
		Class<?> accountClass = getClass(preferences, uuid);
		if (accountClass == null)
			return null;
		
		try
		{
			Class<?>[] pcClass = new Class[] { Preferences.class, String.class };
			Constructor<?> constructor = accountClass.getConstructor(pcClass);
			
			return (BasicAccount)constructor.newInstance(preferences, uuid);
		}
		catch (Exception e)
		{
			if (App.getConfig().logToConsole())
				e.printStackTrace();
			
			return null;
		}
	}
	
	public static boolean isMatch(Preferences preferences, String uuid, long id)
	{
		return preferences.getSharedPreferences().getLong(uuid + ".id", -1) == id;
	}
	
	public final void refresh(Preferences preferences)
	{
		// If account has been deleted, don't save
		if (mDeleted)
		{
			App.logv("Account/refresh: Account deleted; ignoring");
			return;
		}
		
		if ((mId = preferences.getSharedPreferences().getLong(mUuid + ".id", -1)) < 0)
			mDeleted = true;
		
		onClearDirtyFlags();
		onLoad(preferences);
	}
    
	public final long getId()
	{
		return mId;
	}
	
	public final String getUuid()
	{
		return mUuid;
	}
	
	public final String getIconUrl()
	{
		return mIconUrl;
	}
	
	public final int getSyncPeriod()
	{
		return mSyncPeriodMin;
	}
	
	public final void setIconUrl(String iconUrl)
	{
		if (mDirtyIconUrl = !TextUtils.equals(iconUrl, mIconUrl))
			mIconUrl = iconUrl;
	}
	
	public final void setSyncPeriod(int minutes)
	{
		if (mDirtySyncPeriodMin = (minutes != mSyncPeriodMin))
			mSyncPeriodMin = minutes;
	}
	
	public final boolean isAutoSyncEnabled()
	{
		return mSyncPeriodMin != AUTOSYNC_DISABLED;
	}
	
	public final void save(Preferences preferences)
	{
		// If account has been deleted, don't save
		if (mDeleted)
		{
			App.logv("Account/save: Account deleted; ignoring");
			return;
		}
		
		if (App.getConfig().logToConsole())
			App.logv("Account/save: Saving account %s", getScreenName());
		
		synchronized (BasicAccount.class)
		{
	        SharedPreferences.Editor editor = preferences.getSharedPreferences().edit();
	        
	        if (preferences.isNewAccount(this))
	        {
	        	preferences.addAccount(this);
	            editor.putLong(mUuid + ".id", mId);
	            editor.putString(mUuid + ".class", this.getClass().getCanonicalName());
	        }
	        
	        onSave(preferences, editor);
	        
	        editor.commit();
	        
	        onClearDirtyFlags();
		}
	}
	
	public final void delete(Context context)
	{
		synchronized(BasicAccount.class)
		{
	        // Delete preferences
	        SharedPreferences.Editor editor = 
	        	Preferences.get(context).getSharedPreferences().edit();
	        
	        mDeleted = true;
			Set<String> prefSet = Preferences.get(context).getSharedPreferences().getAll().keySet();
			
			for (String s : prefSet)
				if (s.startsWith(mUuid))
					editor.remove(s);
	        
	        editor.commit();
		}
	}
	
	@Override
	public final boolean equals(Object o)
	{
		if (o instanceof BasicAccount)
			return ((BasicAccount)o).mUuid.equals(mUuid);
		
		return super.equals(o);
	}
	
	public abstract void create(Context context, ContentValues cv);
	
	public abstract void edit(Context context);
	
	protected void onLoad(Preferences preferences)
	{
		mIconUrl = preferences.getString(mUuid + ".iconUrl", null);
		mSyncPeriodMin = preferences.getInt(mUuid + ".syncPeriod", 
				AUTOSYNC_DISABLED);
	}
	
	protected void onSave(Preferences p, SharedPreferences.Editor editor)
	{
        if (mDirtyIconUrl)
        	editor.putString(mUuid + ".iconUrl", mIconUrl);
        
        if (mDirtySyncPeriodMin)
        	editor.putInt(mUuid + ".syncPeriod", mSyncPeriodMin);
	}
	
	protected void onClearDirtyFlags()
	{
		mDirtyIconUrl = false;
		mDirtySyncPeriodMin = false;
	}
	
	public abstract ServiceClient createServiceClient();
	
	protected BasicAccount(Parcel in) 
	{
		mId = in.readLong();
		mUuid = in.readString();
		
		mDeleted = (in.readByte() != 0);
		
		mDirtyIconUrl = (in.readByte() != 0);
		mIconUrl = in.readString();
		
		mDirtySyncPeriodMin = (in.readByte() != 0);
		mSyncPeriodMin = in.readInt();
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) 
	{
		dest.writeLong(mId);
		dest.writeString(mUuid);
		
		dest.writeByte(mDeleted ? (byte)1 : 0);
		
		dest.writeByte(mDirtyIconUrl ? (byte)1 : 0);
		dest.writeString(mIconUrl);
		dest.writeByte(mDirtySyncPeriodMin ? (byte)1 : 0);
		dest.writeInt(mSyncPeriodMin);
	}
	
	@Override
	public int describeContents() 
	{
		return 0;
	}
}
