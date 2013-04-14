/*
 * FriendList.java 
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
import android.content.res.Configuration;
import android.support.v4.app.Fragment;

import com.akop.bach.App;
import com.akop.bach.R;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.fragment.xboxlive.FriendProfileFragment;
import com.akop.bach.fragment.xboxlive.FriendsFragment;
import com.akop.bach.fragment.xboxlive.FriendsFragment.OnFriendSelectedListener;

public class FriendList extends XboxLiveMultiPane implements
		OnFriendSelectedListener 
{
	@Override
	protected boolean initializeParameters()
	{
		if (reoriented())
			return false;
		
		return super.initializeParameters();
	}
	
	@Override
    protected Fragment instantiateDetailFragment()
    {
	    return FriendProfileFragment.newInstance(getAccount());
    }
	
	@Override
    protected Fragment instantiateTitleFragment()
    {
	    return FriendsFragment.newInstance(getAccount());
    }
	
	@Override
    public void onFriendSelected(long id)
    {
		if (isDualPane())
		{
			FriendProfileFragment detailFragment = (FriendProfileFragment)mDetailFragment;
			detailFragment.resetTitle(id);
		}
		else
		{
			FriendSummary.actionShow(this, getAccount(), id);
		}
    }
	
	public static void actionShow(Context context, XboxLiveAccount account)
	{
    	Intent intent = new Intent(context, FriendList.class);
    	intent.putExtra("account", account);
    	context.startActivity(intent);
	}
	
	@Override
    protected String getSubtitle()
    {
	    return getString(R.string.friends_of_f, getAccount().getGamertag());
    }
	
	public boolean reoriented()
	{
		Configuration config = getResources().getConfiguration();
		int orientation = config.orientation;
		int coverflowMode = getAccount().getCoverflowMode();
		
		if (coverflowMode == XboxLiveAccount.COVERFLOW_IN_LANDSCAPE 
				&& orientation == Configuration.ORIENTATION_LANDSCAPE)
		{
			if (App.getConfig().logToConsole())
				App.logv("Reorienting ...");
			
			FriendCoverflow.actionShowFriends(this, getAccount());
			return true;
		}
		else if (coverflowMode == XboxLiveAccount.COVERFLOW_ALWAYS)
		{
			if (App.getConfig().logToConsole())
				App.logv("Reorienting ...");
			
			FriendCoverflow.actionShowFriends(this, getAccount());
			return true;
		}
		
		return false;
	}
}
