/*
 * FriendsOfFriendList.java 
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

package com.akop.bach.activity.xboxlive;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.akop.bach.R;
import com.akop.bach.XboxLive.GamerProfileInfo;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.fragment.xboxlive.FriendsOfFriendFragment;
import com.akop.bach.fragment.xboxlive.FriendsOfFriendFragment.OnPlayerSelectedListener;
import com.akop.bach.fragment.xboxlive.PlayerProfileFragment;

public class FriendsOfFriendList extends XboxLiveMultiPane implements
        OnPlayerSelectedListener
{
	private String getGamertag()
	{
		return getIntent().getStringExtra("gamertag");
	}
	
	@Override
	protected Fragment instantiateDetailFragment()
	{
		return PlayerProfileFragment.newInstance(getAccount());
	}
	
	@Override
	protected Fragment instantiateTitleFragment()
	{
		return FriendsOfFriendFragment.newInstance(getAccount(), getGamertag());
	}
	
	@Override
	public void onPlayerSelected(GamerProfileInfo info)
	{
		if (isDualPane())
		{
			PlayerProfileFragment detailFragment = (PlayerProfileFragment)mDetailFragment;
			detailFragment.resetTitle(info);
		}
		else
		{
			PlayerProfile.actionShow(this, getAccount(), info);
		}
	}
	
	public static void actionShow(Context context, XboxLiveAccount account, String gamertag)
	{
		Intent intent = new Intent(context, FriendsOfFriendList.class);
		intent.putExtra("account", account);
		intent.putExtra("gamertag", gamertag);
		context.startActivity(intent);
	}
	
	@Override
	protected String getSubtitle()
	{
		return getString(R.string.x_friends_f, getGamertag());
	}
}
