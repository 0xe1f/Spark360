/*
 * CompareGames.java 
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

import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.akop.bach.App;
import com.akop.bach.R;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.fragment.xboxlive.CompareAchievementsFragment;
import com.akop.bach.fragment.xboxlive.CompareGamesFragment;
import com.akop.bach.fragment.xboxlive.CompareGamesFragment.OnGameSelectedListener;

public class CompareGames extends RibbonedMultiPaneActivity implements
        OnGameSelectedListener
{
	private String mGamertag;
	
	@Override
	protected boolean initializeParameters()
	{
		if ((mGamertag = getIntent().getStringExtra("gamertag")) == null)
		{
			if (App.LOGV)
				App.logv("Missing gamertag; bailing");
			
			return false;
		}
		
	    return super.initializeParameters();
	}
	
	@Override
    protected Fragment instantiateDetailFragment()
    {
	    return CompareAchievementsFragment.newInstance(mAccount, mGamertag);
    }
	
	@Override
    protected Fragment instantiateTitleFragment()
    {
	    return CompareGamesFragment.newInstance(mAccount, mGamertag);
    }
	
	public static void actionShow(Context context, XboxLiveAccount account, String gamertag)
	{
		Intent intent = new Intent(context, CompareGames.class);
		intent.putExtra("account", account);
		intent.putExtra("gamertag", gamertag);
		context.startActivity(intent);
	}

	@Override
	protected String getSubtitle()
	{
		return getString(R.string.compare_games_vs_f, mGamertag);
	}

	@Override
    public void onGameSelected(String yourGamerpicUrl, HashMap<Integer, Object> gameInfo)
    {
		if (isDualPane())
		{
			CompareAchievementsFragment detailFragment = (CompareAchievementsFragment)mDetailFragment;
			detailFragment.resetTitle(yourGamerpicUrl, gameInfo);
		}
		else
		{
			CompareAchievements.actionShow(this,
					yourGamerpicUrl, gameInfo, mAccount, mGamertag);
		}
    }
}
