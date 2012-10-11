/*
 * AccountSetupTest.java
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

import java.io.IOException;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.akop.bach.Account;
import com.akop.bach.App;
import com.akop.bach.BasicAccount;
import com.akop.bach.Preferences;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.parser.AuthenticationException;
import com.akop.bach.parser.ParserException;

public class AccountSetupTest extends Activity implements OnClickListener
{
	private BasicAccount mAccount;
	private ContentValues mProfileData;
	private boolean mCreateAccount;
	
	private TextView mMessage;
	private Button mNextButton;
	private Button mBackButton;
	private ProgressBar mProgress;
	
	private class TaskHandler extends Handler
	{
		private static final int MSG_SHOW_ERROR = 1;
		private static final int MSG_COMPLETE = 2;
		
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case MSG_SHOW_ERROR:
				if (msg.obj == null)
					mMessage.setText(R.string.connection_error);
				else
					mMessage.setText(getString(R.string.connection_error_f, (String)msg.obj));
				break;
			case MSG_COMPLETE:
				mProgress.setIndeterminate(false);
				
				if (msg.arg1 != 0)
				{
					mNextButton.setEnabled(true);
					mProgress.setProgress(mProgress.getMax());
					mMessage.setText(R.string.connection_successful);
				}
				else
				{
					mProgress.setProgress(0);
				}
				
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
		
		public void complete(boolean success)
		{
			Message msg = Message.obtain(this, MSG_COMPLETE, success ? 1 : 0, 0); 
			sendMessage(msg);
		}
		
		public void showErrorDialog(String message)
		{
			Message msg = Message.obtain(this, MSG_SHOW_ERROR, 0, 0); 
			msg.obj = message;
			sendMessage(msg);
		}
	}
	
	private TaskHandler mHandler = new TaskHandler();
	
	private TaskListener mListener = new TaskListener("AccountSetupTest")
	{
		@Override
		public void onTaskFailed(Account account, Exception e)
		{
			String message = null;
			if (e instanceof IOException 
					|| e instanceof ParserException
					|| e instanceof AuthenticationException)
				message = e.getMessage();
			
			mHandler.showErrorDialog(message);
			mHandler.complete(false);
			
			if (App.getConfig().logToConsole())
				e.printStackTrace();
		}
		
		@Override
		public void onTaskSucceeded(Account account, Object requestParam, Object result)
		{
			mProfileData = (ContentValues)result;
			mHandler.complete(true);
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.account_test);
		
		mAccount = (BasicAccount)getIntent().getParcelableExtra("account");
		mCreateAccount = getIntent().getBooleanExtra("createAccount", false);
		
		mMessage = (TextView)findViewById(R.id.account_setup_test_message);
		mNextButton = (Button)findViewById(R.id.account_setup_test_next);
		mBackButton = (Button)findViewById(R.id.account_setup_test_cancel);
		mProgress = (ProgressBar)findViewById(R.id.account_setup_test_progress);
		
		mBackButton.setOnClickListener(this);
		mNextButton.setOnClickListener(this);
		
		if (savedInstanceState != null)
			mAccount = (BasicAccount)savedInstanceState.get("account");
		
		mProfileData = null;
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		outState.putParcelable("account", mAccount);
	}
	
	public static void actionTestSettings(Activity context, 
			BasicAccount account, 
			boolean createAccount)
	{
		Intent intent = new Intent(context, AccountSetupTest.class);
		intent.putExtra("account", account);
		intent.putExtra("createAccount", createAccount);
		context.startActivityForResult(intent, 1);
	}
	
	@Override
	public void onClick(View v)
	{
		if (v.getId() == R.id.account_setup_test_cancel)
		{
			setResult(RESULT_CANCELED);
			finish();
		}
		else if (v.getId() == R.id.account_setup_test_next)
		{
			if (mCreateAccount)
			{
				if (mProfileData == null)
					return;
				
				// CreateAccount will automatically save the account object
				mAccount.create(this, mProfileData);
				Toast.makeText(this, R.string.account_created, Toast.LENGTH_SHORT).show();
			}
			else
			{
				mAccount.save(Preferences.get(this));
				Toast.makeText(this, R.string.changes_saved, Toast.LENGTH_SHORT).show();
			}
			
			setResult(RESULT_OK);
			finish();
		}
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		
		TaskController.getInstance().removeListener(mListener);
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		
		TaskController controller = TaskController.getInstance();
        controller.addListener(mListener);
        
        if (mProfileData == null)
        	controller.validateAccount(mAccount, mListener);
	}
}
