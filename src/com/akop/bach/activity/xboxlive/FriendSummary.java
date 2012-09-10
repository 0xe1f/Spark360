/*
 * FriendSummary.java 
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

package com.akop.bach.activity.xboxlive;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.akop.bach.App;
import com.akop.bach.Preferences;
import com.akop.bach.R;
import com.akop.bach.SupportsFriends;
import com.akop.bach.XboxLive;
import com.akop.bach.XboxLive.Friends;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.fragment.xboxlive.FriendProfileFragment;

public class FriendSummary
		extends XboxLiveSinglePane
{
	private String mGamertag = null;
	private long mFriendId;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		if (getIntent() != null && getIntent().getData() != null)
		{
			String seg = getIntent().getData().getLastPathSegment();
			if (seg != null)
			{
				mFriendId = Long.valueOf(seg);
				
				long accountId = Friends.getAccountId(this, mFriendId);
				Preferences prefs = Preferences.get(this);
				
				mAccount = (XboxLiveAccount)prefs.getAccount(accountId);
			}
		}
		
		super.onCreate(savedInstanceState);
		
        if (mFriendId < 0)
        	mFriendId = getIntent().getLongExtra("friendId", -1);
        
		if (mFriendId < 0)
		{
        	if (App.LOGV)
        		App.logv("Friend not specified");
        	
			finish();
			return;
		}
		
        if ((mGamertag = XboxLive.Friends.getGamertag(this, mFriendId)) == null)
        {
        	if (App.LOGV)
        		App.logv("Friend not found");
        	
        	finish();
        	return;
        }
	}
	
	public static void actionShow(Context context, long friendId)
	{
		actionShow(context, (XboxLiveAccount)Preferences.get(context).getAccount(
						Friends.getAccountId(context, friendId)), friendId);
	}
	
	public static void actionShow(Context context, 
			SupportsFriends account, long friendId)
	{
		Intent intent = new Intent(context, FriendSummary.class);
		intent.putExtra("account", account);
		intent.putExtra("friendId", friendId);
		
		context.startActivity(intent);
	}
	
	public static void actionShow(Context context, 
			SupportsFriends account, String gamertag)
	{
		long friendId = Friends.getFriendId(context, account, gamertag);
		if (friendId > -1)
			actionShow(context, account, friendId);
	}
	
	@Override
    protected String getSubtitle()
    {
		return getString(R.string.friends_f, mGamertag);
    }
	
	@Override
	protected Fragment createFragment() 
	{
		return FriendProfileFragment.newInstance(getAccount(), mFriendId);
	}
}
