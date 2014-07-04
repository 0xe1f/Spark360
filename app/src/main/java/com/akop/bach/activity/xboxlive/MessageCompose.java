/*
 * MessageCompose.java 
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

package com.akop.bach.activity.xboxlive;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.akop.bach.Account;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.parser.Parser;

public class MessageCompose
		extends RibbonedActivity
		implements OnClickListener, TextWatcher
{
	private Button mSendButton;
	private Button mDiscardButton;
	private Button mSelectButton;
	private TextView mMessageBody;
	private TextView mRecipientView;
	
	private ArrayList<String> mRecipientList;
	
	private class TaskHandler extends Handler
	{
		private static final int MSG_ALLOW_EDITS = 1;
		
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case MSG_ALLOW_EDITS:
				if (mSendButton != null)
					mSendButton.setEnabled(msg.arg1 != 0);
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
		
		public void allowEdits(boolean on)
		{
			sendMessage(Message.obtain(this, MSG_ALLOW_EDITS, on ? 1 : 0, 0));
		}
	}
	
	private TaskHandler mComposeHandler = new TaskHandler();
	
	private TaskListener mListener = new TaskListener()
	{
		@Override
		public void onAllTasksCompleted()
		{
			mHandler.showThrobber(false);
		}
		
		@Override
		public void onTaskSucceeded(Account account, Object requestParam, Object result)
		{
			mHandler.showToast(getString(R.string.message_sent));
		}
		
		@Override
		public void onTaskFailed(Account account, Exception e)
		{
			mComposeHandler.allowEdits(true); // Allow edits again
			mHandler.showToast(Parser.getErrorMessage(MessageCompose.this, e));
		}
		
		@Override
		public void onTaskStarted()
		{
			mHandler.showThrobber(true);
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.xbl_message_compose);
		
		mRecipientList = new ArrayList<String>();
		
		mSendButton = (Button)findViewById(R.id.message_compose_send);
		mDiscardButton = (Button)findViewById(R.id.message_compose_discard);
		mSelectButton = (Button)findViewById(R.id.message_compose_select);
		mMessageBody = (TextView)findViewById(R.id.message_compose_body);
		mRecipientView = (TextView)findViewById(R.id.message_compose_recipients);
		
		if (mSendButton != null)
			mSendButton.setOnClickListener(this);
		
		if (mDiscardButton != null)
			mDiscardButton.setOnClickListener(this);
		
		mSelectButton.setOnClickListener(this);
		mMessageBody.addTextChangedListener(this);
		mRecipientView.setOnClickListener(this);
		
		if (savedInstanceState != null)
			mRecipientList = savedInstanceState.getStringArrayList("recipients");
		else
		{
			String recipient = getIntent().getStringExtra("recipient");
			if (recipient != null)
				mRecipientList.add(recipient);
			
			String body = getIntent().getStringExtra("body");
			if (body != null)
				mMessageBody.setText(body);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		getMenuInflater().inflate(R.menu.xbl_message_compose, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
	    switch (item.getItemId()) 
	    {
	    case R.id.menu_send:
	    	sendMessage();
	    	return true;
	    }
	    return false;
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		outState.putStringArrayList("recipients", mRecipientList);
	}
	
	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.message_compose_send:
			sendMessage();
			break;
		case R.id.message_compose_select:
			MessageSelectRecipients.actionSelectFriends(this, mAccount, mRecipientList);
			break;
		case R.id.message_compose_discard:
			setResult(RESULT_CANCELED);
			finish();
			break;
		}
	}
	
	private void sendMessage()
	{
		String errorMessage = null;
		if (mRecipientList.size() < 1)
			errorMessage = getString(R.string.error_no_recipients);
		else if (TextUtils.isEmpty(mMessageBody.getText()))
			errorMessage = getString(R.string.error_no_message_body);
		
		if (errorMessage != null)
		{
			new AlertDialog.Builder(this)
					.setMessage(errorMessage)
					.setPositiveButton(R.string.close, null)
					.show();
			return;
		}
		
		String[] recipients = new String[mRecipientList.size()];
		
		mRecipientList.toArray(recipients);
		mHandler.showToast(getString(R.string.message_queued_for_send));
		
		TaskController.getInstance().sendMessage(mAccount, 
				recipients, mMessageBody.getText().toString(), 
				null, mListener);
		
		setResult(RESULT_OK);
		finish();
	}
	
	private void validate()
	{
		boolean valid = 
			mMessageBody.getText().toString().trim().length() > 0
				&& mRecipientList.size() > 0;
		
		if (mSendButton != null)
			mSendButton.setEnabled(valid);
	}
	
	private void refreshRecipients()
	{
		if (mRecipientList.size() < 1)
		{
			mRecipientView.setText(R.string.select_recipients);
		}
		else
		{
			mRecipientView.setText(getString(R.string.to_f, 
					TextUtils.join(", ", mRecipientList)));
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
        TaskController.getInstance().addListener(mListener);
		
		validate();
		refreshRecipients();
	}
	
	@Override
	public void afterTextChanged(Editable s)
	{
		validate();
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after)
	{
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		if (resultCode == RESULT_OK)
			mRecipientList = data.getStringArrayListExtra("selected");
	}
	
	public static void actionComposeMessage(Context context, 
			XboxLiveAccount account, String gamertag)
	{
		actionComposeMessage(context, account, gamertag, null);
	}
	
	public static void actionComposeMessage(Context context, 
			XboxLiveAccount account, String gamertag, String body)
	{
    	Intent intent = new Intent(context, MessageCompose.class);
    	intent.putExtra("account", account);
    	intent.putExtra("recipient", gamertag);
    	intent.putExtra("body", body);
    	
    	context.startActivity(intent);
	}
	
	@Override
	protected void updateRibbon()
	{
		if (mAccount != null)
		{
			updateRibbon(mAccount.getGamertag(),
					mAccount.getIconUrl(),
					getString(R.string.compose_message_u));
		}
	}
}
