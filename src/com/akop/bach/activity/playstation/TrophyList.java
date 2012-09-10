/*
 * TrophyList.java 
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
import com.akop.bach.PSN.Games;
import com.akop.bach.PsnAccount;
import com.akop.bach.R;
import com.akop.bach.fragment.playstation.TrophiesFragment;

public class TrophyList extends PsnSinglePane
{
	private String mTitle = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		long titleId;
		if ((titleId = getIntent().getLongExtra("gameId", -1)) < 0)
		{
			if (App.LOGV)
				App.logv("TrophyList: no gameId");
			
			finish();
			return;
		}
		
		mTitle = Games.getTitle(this, titleId);
	}
	
	@Override
    protected Fragment createFragment()
    {
	    return TrophiesFragment.newInstance(getAccount(), 
	    		getIntent().getLongExtra("gameId", -1), true);
    }
	
	public static void actionShow(Context context, PsnAccount account, long gameId)
	{
    	Intent intent = new Intent(context, TrophyList.class);
    	intent.putExtra("account", account);
    	intent.putExtra("gameId", gameId);
    	context.startActivity(intent);
	}
	
	@Override
    protected String getSubtitle()
    {
		return getString(R.string.x_trophies_f, mTitle);
    }
}
