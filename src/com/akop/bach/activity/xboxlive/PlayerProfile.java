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
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.akop.bach.App;
import com.akop.bach.R;
import com.akop.bach.XboxLive.GamerProfileInfo;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.fragment.xboxlive.PlayerProfileFragment;

public class PlayerProfile
		extends RibbonedSinglePaneActivity
{
	private String mGamertag = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		GamerProfileInfo info = (GamerProfileInfo)getIntent().getSerializableExtra("info");
		if (info == null)
		{
			if (App.LOGV)
				App.logv("GamerProfileInfo is null");
			
			finish();
			return;
		}
		
		mGamertag = info.Gamertag;
        
		FragmentManager fm = getSupportFragmentManager();
		Fragment titleFrag;
		
		FragmentTransaction ft = fm.beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        
		if ((titleFrag = (PlayerProfileFragment)fm.findFragmentByTag("details")) == null)
		{
			titleFrag = PlayerProfileFragment.newInstance(mAccount, info);
			ft.replace(R.id.fragment_titles, titleFrag, "details");
		}
		
		ft.commit();
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
}
