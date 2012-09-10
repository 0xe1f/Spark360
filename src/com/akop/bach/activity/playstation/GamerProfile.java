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

package com.akop.bach.activity.playstation;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.akop.bach.App;
import com.akop.bach.PsnAccount;
import com.akop.bach.R;
import com.akop.bach.fragment.playstation.GamerProfileFragment;

public class GamerProfile extends PsnSinglePane
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		if (!getIntent().hasExtra("gamertag"))
		{
			if (App.LOGV)
				App.logv("GamerProfile: no gameId");
			
			finish();
			return;
		}
	}
	
	@Override
    protected Fragment createFragment()
    {
	    return GamerProfileFragment.newInstance(getAccount(), 
	    		getIntent().getStringExtra("gamertag"));
    }
	
	public static void actionShow(Context context, PsnAccount account, String gamertag)
	{
    	Intent intent = new Intent(context, GamerProfile.class);
    	intent.putExtra("account", account);
    	intent.putExtra("gamertag", gamertag);
    	context.startActivity(intent);
	}
	
	@Override
    protected String getSubtitle()
    {
		return getString(R.string.psn_profile_f, 
				getIntent().getStringExtra("gamertag"));
    }
}
