/*
 * Accounts.java 
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

package com.akop.bach.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;

import com.akop.bach.Preferences;
import com.akop.bach.R;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.fragment.AccountsFragment;
import com.akop.bach.fragment.AccountsFragment.OnAccountSelectedListener;
import com.akop.bach.fragment.xboxlive.AccountProfileFragment;

public class Accounts extends RibbonedMultiPane implements
        OnAccountSelectedListener
{
	@Override
    protected Fragment instantiateDetailFragment()
    {
		return AccountProfileFragment.newInstance();
    }
	
	@Override
    protected Fragment instantiateTitleFragment()
    {
		return AccountsFragment.newInstance();
    }
	
	public static void actionShow(Context context)
	{
		Intent intent = new Intent(context, Accounts.class);
		context.startActivity(intent);
	}
	
	@Override
	protected String getBachTitle()
	{
		return getString(R.string.app_name);
	}
	
	@Override
	protected String getSubtitle()
	{
		return getString(R.string.select_account);
	}
	
	@Override
	protected boolean allowNullAccounts()
	{
	    return true;
	}
	
	@Override
    public void onAccountSelected(String uuid, Uri uri)
    {
	    openAccount(uuid, uri);
    }

	@Override
    public void onNewAccount()
    {
		NewAccount.actionShow(this);
    }
	
	private void openAccount(String uuid, Uri uri)
	{
		boolean openExternal = true;
		if (isDualPane())
		{
			AccountProfileFragment detailFragment = (AccountProfileFragment)mDetailFragment;
			XboxLiveAccount account = (XboxLiveAccount)Preferences.get(this).getAccount(uuid);
			
			if (account != null)
			{
				detailFragment.resetTitle(account);
				openExternal = false;
			}
			else
			{
				detailFragment.resetTitle(null);
			}
		}
		
		if (openExternal)
		{
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
	    	startActivity(intent);
		}
	}
	
	@Override
	protected int getActionBarLayout() 
	{
		return R.layout.xbl_actionbar_custom; // TODO: make it neutral
	}
	
	@Override
	protected int getLayout() 
	{
		return R.layout.xbl_multipane; // TODO: make it neutral
	}
}
