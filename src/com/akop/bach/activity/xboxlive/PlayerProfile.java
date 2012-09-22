/*
 * PlayerProfile.java 
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
import com.akop.bach.XboxLive.GamerProfileInfo;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.fragment.xboxlive.PlayerProfileFragment;

public class PlayerProfile
		extends XboxLiveSinglePane
{
	private String mGamertag = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		GamerProfileInfo info = (GamerProfileInfo)getIntent().getParcelableExtra("info");
		if (info == null)
		{
			if (App.LOGV)
				App.logv("GamerProfileInfo is null");
			
			finish();
			return;
		}
		
		mGamertag = info.Gamertag;
	}
	
	public static void actionShow(Context context, 
			XboxLiveAccount account, GamerProfileInfo info)
	{
		Intent intent = new Intent(context, PlayerProfile.class);
		intent.putExtra("account", account);
		intent.putExtra("info", info);
		
		context.startActivity(intent);
	}
	
	@Override
    protected String getSubtitle()
    {
		return mGamertag;
    }
	
	@Override
	protected Fragment createFragment() 
	{
		return PlayerProfileFragment.newInstance(getAccount(), 
				(GamerProfileInfo)getIntent().getParcelableExtra("info"));
	}
}
