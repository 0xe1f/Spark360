/*
 * AuthenticatingAccount.java
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

import java.util.regex.Pattern;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.text.TextUtils;

public abstract class AuthenticatingAccount extends BasicAccount 
{
	private static final Pattern sEmailAddressValidator = Pattern.compile(
			"^[.\\w!#$%&'*+/=?`{|}~^-]+@[A-Z0-9-]+(?:\\.[A-Z0-9-]+)*$",
			Pattern.CASE_INSENSITIVE);
	
	private boolean mDirtyEmailAddress;
	private String mEmailAddress;
	private boolean mDirtyPassword;
	private String mPassword;
	
	protected AuthenticatingAccount(Context context)
	{
		super(context);
		
		mEmailAddress = null;
		mPassword = null;
	}
	
	public AuthenticatingAccount(Preferences preferences, String uuid)
	{
		super(preferences, uuid);
	} 
	
	public String getEmailAddress()
	{
		return mEmailAddress;
	}
	
	public String getPassword()
	{
		return mPassword;
	}
	
	public void setEmailAddress(String emailAddress)
	{
		if (!TextUtils.equals(emailAddress, mEmailAddress))
		{
			mEmailAddress = emailAddress;
			mDirtyEmailAddress = true; 
		}
	}
	
	public void setPassword(String password)
	{
		if (!TextUtils.equals(password, mPassword))
		{
			mPassword = password;
			mDirtyPassword = true; 
		}
	}
	
	public boolean isValid()
	{
		return getEmailAddress() != null && getPassword() != null;
	}
	
	public static boolean isEmailAddressValid(String emailAddress)
	{
		return sEmailAddressValidator.matcher(emailAddress).find();
	}
	
	public static boolean isPasswordNonEmpty(String password)
	{
		return password != null && password.length() > 0;
	}
	
	@Override
	protected void onSave(Preferences p, SharedPreferences.Editor editor)
	{
		super.onSave(p, editor);
		
		try
		{
			if (mDirtyEmailAddress)
			{
				if (App.LOGV)
					App.logv("AuthenticatingAccount/onSave: Email address dirty; saving");
				
				p.putEncrypted(editor, mUuid + ".emailAddress", mEmailAddress);
			}
			
			if (mDirtyPassword)
			{
				if (App.LOGV)
					App.logv("AuthenticatingAccount/onSave: Password dirty; saving");
				
				p.putEncrypted(editor, mUuid + ".password", mPassword);
			}
		}
		catch(EncryptionException e)
		{
			if (App.LOGV)
				e.printStackTrace();
		}
	}

	@Override
	protected void onLoad(Preferences preferences)
	{
		super.onLoad(preferences);
		
		String emailAddressKey = mUuid + ".emailAddress";
		String passwordKey = mUuid + ".password";
		
		try
		{
			mEmailAddress = preferences.getEncrypted(emailAddressKey);
			mPassword = preferences.getEncrypted(passwordKey);
		}
		catch(EncryptionException e)
		{
			if (App.LOGV)
				e.printStackTrace();
		}
		
		if (preferences.needsEncryptionRefresh(emailAddressKey))
			mDirtyEmailAddress = true;
		if (preferences.needsEncryptionRefresh(passwordKey))
			mDirtyPassword = true;
	}
	
	@Override
	protected void onClearDirtyFlags()
	{
		super.onClearDirtyFlags();
		
		mDirtyEmailAddress = false;
		mDirtyPassword = false;
	}
	
	@Override
	public String getLogonId()
	{
		return mEmailAddress;
	}
	
	protected AuthenticatingAccount(Parcel in) 
	{
		super(in);
		
		mDirtyEmailAddress = (in.readByte() != 0);
		mEmailAddress = in.readString();
		mDirtyPassword = (in.readByte() != 0);
		mPassword = in.readString();
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) 
	{
		super.writeToParcel(dest, flags);
		
		dest.writeByte(mDirtyEmailAddress ? (byte)1 : 0);
		dest.writeString(mEmailAddress);
		dest.writeByte(mDirtyPassword ? (byte)1 : 0);
		dest.writeString(mPassword);
	}
}