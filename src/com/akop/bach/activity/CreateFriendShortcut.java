/*
 * CreateFriendShortcut.java 
 * Copyright (C) 2010-2014 Akop Karapetyan
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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.akop.bach.App;
import com.akop.bach.R;
import com.akop.bach.SupportsFriends;

public class CreateFriendShortcut
		extends Activity
{
	private final static int SELECT_ACCOUNT = 0;
	private final static int SELECT_FRIEND = 1;
	
	private SupportsFriends mAccount;
	private long mFriendId;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		mAccount = null;
		mFriendId = -1;
		
		if (savedInstanceState != null)
		{
			if (savedInstanceState.containsKey("account"))
				mAccount = (SupportsFriends)savedInstanceState
						.getParcelable("account");
			
			if (savedInstanceState.containsKey("friendId"))
				mFriendId = savedInstanceState.getLong("friendId");
		}
		
		if (mAccount == null)
			AccountSelector.actionSelectAccount(this, SELECT_ACCOUNT);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		if (mAccount != null)
			outState.putParcelable("account", mAccount);
		if (mFriendId > -1)
			outState.putLong("friendId", mFriendId);
	}
	
	private void createShortcut()
	{
		if (mAccount == null || mFriendId < 0)
		{
			if (App.getConfig().logToConsole())
				App.logv("Missing account or friend ID");
			
			finish();
			return;
		}
		
		Intent shortcutIntent = new Intent(Intent.ACTION_VIEW,
				mAccount.getFriendUri(mFriendId));
		shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP 
				| Intent.FLAG_ACTIVITY_NEW_TASK);
		
        // The result we are passing back from this activity
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, 
				mAccount.getFriendScreenName(mFriendId));
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
				Intent.ShortcutIconResource.fromContext(this,
						R.drawable.shortcut_friend));
		
        setResult(RESULT_OK, intent);
 	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		if (resultCode == RESULT_OK)
		{
			if (requestCode == SELECT_ACCOUNT)
			{
				mAccount = (SupportsFriends)data.getParcelableExtra("account");
				FriendSelector.actionSelectFriends(this, mAccount);
			}
			else if (requestCode == SELECT_FRIEND)
			{
				mFriendId = data.getLongExtra("friendId", -1);
				createShortcut();
				
				finish();
			}
		}
		else
		{
			finish();
		}
	}
}
