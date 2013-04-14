/*
 * GameCatalogDetails.java 
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

package com.akop.bach.activity.playstation;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.akop.bach.PSN.GameCatalogItem;
import com.akop.bach.PsnAccount;
import com.akop.bach.R;
import com.akop.bach.fragment.playstation.GameCatalogDetailsFragment;

public class GameCatalogDetails extends PsnSinglePane
{
	public static void actionShow(Context context, PsnAccount account,
			GameCatalogItem item)
	{
		Intent intent = new Intent(context, GameCatalogDetails.class);
		intent.putExtra("account", account);
		intent.putExtra("gameItem", item);
		
		context.startActivity(intent);
	}
	
	@Override
	protected String getSubtitle()
	{
		GameCatalogItem item = (GameCatalogItem)getIntent().getSerializableExtra("gameItem");
		
		return getString(R.string.game_details_f, item.Title);
	}
	
    @Override
    protected Fragment createFragment()
    {
		return GameCatalogDetailsFragment.newInstance(getAccount(),
				(GameCatalogItem)getIntent().getSerializableExtra("gameItem"));
    }
}
