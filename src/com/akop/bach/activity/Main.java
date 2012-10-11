/*
 * Main.java 
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

package com.akop.bach.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;

import com.akop.bach.BasicAccount;
import com.akop.bach.App;
import com.akop.bach.PSN;
import com.akop.bach.XboxLive;

public class Main extends Activity
{
	private static final Uri[] URI_LIST = 
	{
		PSN.Profiles.CONTENT_URI,
		XboxLive.Profiles.CONTENT_URI,
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
		BasicAccount account = (BasicAccount)getIntent().getParcelableExtra("account");
		if (account != null)
		{
			if (App.getConfig().logToConsole())
				App.logv("Explicit account requested: " + account.getScreenName());
			
			// An account was explicitly requested
			account.open(Main.this);
		}
		else
		{
			start();
		}
		
    	finish();
	}
	
	public void start()
	{
		List<Uri> accounts = new ArrayList<Uri>();
		
		for (Uri uri: URI_LIST)
		{
			Cursor c = null;
			
			try
			{
				c = getContentResolver().query(uri, 
						new String[] { "AccountId" }, null, null, null);
			}
			catch(Exception e)
			{
			}
			
			if (c != null)
			{
				try
				{
					while (c.moveToNext())
					{
						Uri fullUri = ContentUris.withAppendedId(uri, c.getLong(0));
						accounts.add(fullUri);
					}
				}
				finally
				{
					c.close();
				}
			}
		}
		
		if (accounts.size() == 1)
		{
			Intent intent = new Intent(Intent.ACTION_VIEW, accounts.get(0));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    	startActivity(intent);
		}
		else
		{
			Accounts.actionShow(this);
		}
	}
}
