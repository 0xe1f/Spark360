/*
 * PsnAccountLogin.java 
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

package com.akop.bach.activity.playstation;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;

import com.akop.bach.AuthenticatingAccount;
import com.akop.bach.PsnAccount;
import com.akop.bach.R;
import com.akop.bach.activity.AuthenticatingAccountLogin;

public class PsnAccountLogin
		extends AuthenticatingAccountLogin implements OnItemSelectedListener
{
	private Spinner mLocaleSpinner;
	
	@Override
	protected int getLayout()
	{
		return R.layout.psn_account_login;
	}
	
	@Override
	protected boolean isValid()
	{
		if (!super.isValid())
			return false;
		
		if (mLocaleSpinner == null)
			return false;
		
		return true; //mLocaleSpinner.getSelectedItemPosition() > 0;
	}
	
	@Override
	protected void setupTest(AuthenticatingAccount account)
	{
		super.setupTest(account);
		
		int pos = mLocaleSpinner.getSelectedItemPosition();
		String[] localeValues = getResources().getStringArray(R.array.region_values);
		PsnAccount psnAccount = (PsnAccount)account;
		
		psnAccount.setLocale(localeValues[pos]);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		mLocaleSpinner = (Spinner)findViewById(R.id.account_locale);
		mLocaleSpinner.setOnItemSelectedListener(this);
	}
	
	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3)
	{
		resetValidation();
	}
	
	@Override
	public void onNothingSelected(AdapterView<?> arg0)
	{
		resetValidation();
	}
}