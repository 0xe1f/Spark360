/*
 * DeleteAccount.java
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
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

import com.akop.bach.Account;
import com.akop.bach.Preferences;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.service.NotificationService;

public class DeleteAccount extends Activity
{
	private static final int DIALOG_WARN_DELETE = 1;
	private Account mAccount;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		if (getIntent().getData() != null)
		{
			String uriPart = getIntent().getData().getLastPathSegment();
			if (uriPart != null)
			{
				long accountId = Long.valueOf(uriPart);
				mAccount = Preferences.get(this).getAccount(accountId);
			}
		}
		else
		{
			finish();
			return; // Invalid call
		}
		
		showDialog(DIALOG_WARN_DELETE);
	}
	
	private void delete()
	{
		Preferences.get(this).deleteAccount(this, mAccount); 
		
		NotificationService.actionReschedule(this);
		
		// Schedule the account data to be removed from system
		TaskController.get().deleteAccount(mAccount, null);
	}
	
	@Override
	protected Dialog onCreateDialog(int id)
	{
		Builder builder;
		
		switch (id)
		{
		case DIALOG_WARN_DELETE:
			builder = new Builder(this);
			builder.setTitle(R.string.are_you_sure)
				.setPositiveButton(android.R.string.yes, new OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						delete();
						finish();
					}
				})
				.setNegativeButton(android.R.string.no, new OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						finish();
					}
				})
				.setIcon(android.R.drawable.ic_menu_help)
				.setMessage(R.string.erase_account_data);
			
			return builder.create();
		}
		
		return null;
	}
}
