/*
 * CreateAccountShortcut.java 
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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.akop.bach.BasicAccount;
import com.akop.bach.App;
import com.akop.bach.R;

public class CreateAccountShortcut
		extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		AccountSelector.actionSelectAccount(this);
	}
	
	private void createShortcut(BasicAccount account)
	{
		if (account == null)
		{
			if (App.LOGV)
				App.logv("Missing account");
			
			finish();
			return;
		}
		
		Intent shortcutIntent = new Intent(Intent.ACTION_VIEW,
				account.getProfileUri());
		shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP 
				| Intent.FLAG_ACTIVITY_NEW_TASK);
		
        // The result we are passing back from this activity
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, account.getScreenName());
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
				Intent.ShortcutIconResource.fromContext(this,
						R.drawable.shortcut_profile));
		
        setResult(RESULT_OK, intent);
 	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		if (resultCode == RESULT_OK)
			createShortcut((BasicAccount)data.getSerializableExtra("account"));
		
		finish();
	}
}
