/*
 * AuthenticatingAccountLogin.java
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
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.akop.bach.BasicAccount;
import com.akop.bach.App;
import com.akop.bach.AuthenticatingAccount;
import com.akop.bach.Preferences;
import com.akop.bach.R;

public class AuthenticatingAccountLogin
		extends Activity
		implements OnClickListener, TextWatcher
{
	private static final int DIALOG_ERROR = 1;
	
	private AlertDialog mAlert;
	private boolean mCreate;
	private AuthenticatingAccount mAccount;
	
	private EditText mEmailAddress;
	private EditText mPassword;
	private Button mOkButton;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(getLayout());
		
		if (Intent.ACTION_INSERT.equals(getIntent().getAction()))
		{
			String authority = getIntent().getData().getAuthority();
			
			mCreate = true;
			mAccount = (AuthenticatingAccount)App.createAccountFromAuthority(this, authority);
		}
		else
		{
			mCreate = false;
			mAccount = (AuthenticatingAccount)getIntent().getParcelableExtra("account");
		}
		
        if (mAccount == null)
        {
        	if (App.LOGV)
        		App.logv("AuthenticatingAccountLogin: Account is null");
        	
        	finish();
        	return;
        }
        
        TextView tv = (TextView)findViewById(R.id.account_setup_login_directions);
		tv.setText(getString(R.string.type_your_credentials_f, 
				mAccount.getDescription()));
		
		mEmailAddress = (EditText)findViewById(R.id.account_setup_login_email);
		mPassword = (EditText)findViewById(R.id.account_setup_login_password);
		
        mEmailAddress.setText(mAccount.getEmailAddress());
        mPassword.setText(mAccount.getPassword());
		
		mEmailAddress.addTextChangedListener(this);
		mPassword.addTextChangedListener(this);
		
		mOkButton = (Button)findViewById(R.id.account_setup_login_ok);
        mOkButton.setOnClickListener(this);
	}
	
	protected int getLayout()
	{
		return R.layout.authenticating_account_login;
	}
	
	protected void setupTest(AuthenticatingAccount account)
	{
		String emailAddress = mEmailAddress.getText().toString();
		String password = mPassword.getText().toString();
		
		account.setEmailAddress(emailAddress);
		account.setPassword(password);
	}
	
	private void onTestChanges()
	{
		String emailAddress = mEmailAddress.getText().toString();
		
		// Make sure the account is not a duplicate
		BasicAccount[] accounts = Preferences.get(this).getAccounts();
		for (BasicAccount account: accounts)
		{
			if (account instanceof AuthenticatingAccount)
			{
				AuthenticatingAccount authAccount = (AuthenticatingAccount)account;
				
				if (emailAddress.equalsIgnoreCase(authAccount.getEmailAddress())
						&& mAccount.getClass().equals(authAccount.getClass())
						&& !mAccount.getUuid().equals(authAccount.getUuid()))
				{
					showDialog(DIALOG_ERROR);
					mAlert.setMessage(getString(R.string.account_email_already_exists));
					return;
				}
			}
		}
		
		setupTest(mAccount);
		
		AccountSetupTest.actionTestSettings(this, mAccount, mCreate);
	}
	
	@Override
	protected Dialog onCreateDialog(int id)
	{
		Builder builder = new Builder(this);
		switch (id)
		{
		case DIALOG_ERROR:
			builder.setTitle(R.string.error)
				.setNeutralButton(android.R.string.ok, new Dialog.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which) {}
				})
				.setMessage("");
			
			return mAlert = builder.create();
		}
		
		return super.onCreateDialog(id);
	}
	
	public void onClick(View v)
	{
		if (v.getId() == R.id.account_setup_login_ok)
			onTestChanges();
	}
	
	protected boolean isValid()
	{
		return AuthenticatingAccount.isEmailAddressValid(mEmailAddress.getText().toString())
				&& AuthenticatingAccount.isPasswordNonEmpty(mPassword.getText().toString());
	}
	
	protected void resetValidation()
	{
		mOkButton.setEnabled(isValid());
	}
	
	public void afterTextChanged(Editable arg0)
	{
		resetValidation();
	}
	
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after)
	{
	}
	
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		resetValidation();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		if (resultCode == RESULT_OK)
		{
			setResult(RESULT_OK);
			
			if (mCreate)
				mAccount.edit(this);
			
			finish();
		}
	}
	
	public static void actionEditLoginData(Activity context,
			AuthenticatingAccount account)
	{
		Intent intent = new Intent(context, AuthenticatingAccountLogin.class);
		intent.putExtra("account", account);
		context.startActivityForResult(intent, 1);
	}
}