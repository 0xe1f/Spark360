/*
 * CompareAchievements.java 
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
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.akop.bach.R;
import com.akop.bach.XboxLive.ComparedGameCursor;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.fragment.xboxlive.CompareAchievementsFragment;

public class CompareAchievements
		extends XboxLiveSinglePane
{
	private String mGamertag;
	private String mGameTitle;
	
	@SuppressWarnings("unchecked")
    @Override
	protected void onCreate(Bundle savedInstanceState)
	{
		HashMap<Integer, Object> gameInfo = (HashMap<Integer, Object>)getIntent().getSerializableExtra("gameInfo");
		
		mGamertag = getIntent().getStringExtra("gamertag");
		mGameTitle = (String)gameInfo.get(ComparedGameCursor.COLUMN_TITLE);
		
		super.onCreate(savedInstanceState);
	}
	
	public static void actionShow(Context context,
			String yourGamerpicUrl,
			HashMap<Integer, Object> gameInfo, XboxLiveAccount account, 
			String gamertag)
	{
		Intent intent = new Intent(context, CompareAchievements.class);
		intent.putExtra("account", account);
		intent.putExtra("gamertag", gamertag);
		intent.putExtra("gameInfo", gameInfo);
		intent.putExtra("yourGamerpicUrl", yourGamerpicUrl);
		
		context.startActivity(intent);
	}
	
	@Override
    protected String getSubtitle()
    {
		return getString(R.string.compare_x_achieves_vs_f, 
				mGamertag, mGameTitle);
    }
	
	@SuppressWarnings("unchecked")
	@Override
	protected Fragment createFragment() 
	{
		String yourGamerpicUrl = getIntent().getStringExtra("yourGamerpicUrl"); 
		HashMap<Integer, Object> gameInfo = (HashMap<Integer, Object>)getIntent().getSerializableExtra("gameInfo");
		
		return CompareAchievementsFragment.newInstance(getAccount(), 
				mGamertag, gameInfo, yourGamerpicUrl);
	}
}
