/*
 * FriendSummary.java 
 * Copyright (C) 2010-2014 Akop Karapetyan
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

package com.akop.bach.activity.playstation;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.akop.bach.App;
import com.akop.bach.PSN;
import com.akop.bach.PSN.Friends;
import com.akop.bach.Preferences;
import com.akop.bach.PsnAccount;
import com.akop.bach.fragment.playstation.FriendProfileFragment;

public class FriendSummary extends PsnSinglePane
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		long titleId = getTitleId();
		
		if (titleId >= 0)
		{
			long accountId = Friends.getAccountId(this, titleId);
			Preferences prefs = Preferences.get(this);
			
			mAccount = (PsnAccount)prefs.getAccount(accountId);
		}
		
		super.onCreate(savedInstanceState);
	}
	
	private long getTitleId()
	{
		long titleId = -1;
		
		if (getIntent() != null && getIntent().getData() != null)
		{
			String seg = getIntent().getData().getLastPathSegment();
			if (seg != null)
			{
				titleId = Long.valueOf(seg);
				
				long accountId = Friends.getAccountId(this, titleId);
				Preferences prefs = Preferences.get(this);
				
				mAccount = (PsnAccount)prefs.getAccount(accountId);
			}
		}
		
        if (titleId < 0)
        	titleId = getIntent().getLongExtra("friendId", -1);
        
        return titleId;
	}
	
	public static void actionShow(Context context,
			PsnAccount account, long friendId)
	{
		Intent intent = new Intent(context, FriendSummary.class);
		intent.putExtra("account", account);
		intent.putExtra("friendId", friendId);
		context.startActivity(intent);
	}
	
	public static void actionShow(Context context, long friendId)
	{
		actionShow(context,
				(PsnAccount)Preferences.get(context).getAccount(
						Friends.getAccountId(context, friendId)), friendId);
	}
	
	@Override
    protected String getSubtitle()
    {
		String gamertag;
        if ((gamertag = PSN.Friends.getOnlineId(this, getTitleId())) == null)
        {
        	if (App.getConfig().logToConsole())
        		App.logv("Friend not found");
        }
        
		return gamertag;
    }
	
	@Override
    protected Fragment createFragment()
    {
	    return FriendProfileFragment.newInstance(getAccount(), getTitleId());
    }
}
