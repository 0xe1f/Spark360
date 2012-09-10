/*
 * CompareTrophies.java 
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

import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.akop.bach.PSN.ComparedGameCursor;
import com.akop.bach.PsnAccount;
import com.akop.bach.R;
import com.akop.bach.fragment.playstation.CompareTrophiesFragment;

public class CompareTrophies extends PsnSinglePane
{
	public static void actionShow(Context context, String yourGamerpicUrl,
	        HashMap<Integer, Object> gameInfo, PsnAccount account,
	        String gamertag)
	{
		Intent intent = new Intent(context, CompareTrophies.class);
		intent.putExtra("account", account);
		intent.putExtra("gamertag", gamertag);
		intent.putExtra("gameInfo", gameInfo);
		intent.putExtra("yourGamerpicUrl", yourGamerpicUrl);

		context.startActivity(intent);
	}
	
	@SuppressWarnings("unchecked")
    @Override
	protected String getSubtitle()
	{
		HashMap<Integer, Object> gameInfo = (HashMap<Integer, Object>) getIntent()
		        .getSerializableExtra("gameInfo");
		
		String gamertag = getIntent().getStringExtra("gamertag");
		String gameTitle = (String) gameInfo.get(ComparedGameCursor.COLUMN_TITLE);
		
		return getString(R.string.comparing_x_trophies_with_y_f, gamertag,
				gameTitle);
	}
	
	@SuppressWarnings("unchecked")
    @Override
    protected Fragment createFragment()
    {
		String yourGamerpicUrl = getIntent().getStringExtra("yourGamerpicUrl");
		HashMap<Integer, Object> gameInfo = (HashMap<Integer, Object>) getIntent()
		        .getSerializableExtra("gameInfo");
		String gamertag = getIntent().getStringExtra("gamertag");
		
		return CompareTrophiesFragment.newInstance(getAccount(), gamertag,
		        gameInfo, yourGamerpicUrl);
    }
}
