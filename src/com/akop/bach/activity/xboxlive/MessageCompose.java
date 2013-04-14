/*
 * MessageCompose.java 
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

package com.akop.bach.activity.xboxlive;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
	private class ViewHolder
	{
		Button sendButton;
		Button discardButton;
		Button selectButton;
		TextView messageBody;
		TextView recipients;
	}
	
	private ViewHolder mView;
	private ArrayList<String> mRecipients;
	
	private class TaskHandler extends Handler
	{
		private static final int MSG_ALLOW_EDITS = 1;
		
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case MSG_ALLOW_EDITS:
				mView.sendButton.setEnabled(msg.arg1 != 0);
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
		
		mRecipients = new ArrayList<String>();
		
		// Set up view holder
		mView = new ViewHolder();
		mView.sendButton = (Button)findViewById(R.id.message_compose_send);
		mView.discardButton = (Button)findViewById(R.id.message_compose_discard);
		mView.selectButton = (Button)findViewById(R.id.message_compose_select);
		mView.messageBody = (TextView)findViewById(R.id.message_compose_body);
		mView.recipients = (TextView)findViewById(R.id.message_compose_recipients);
		
		mView.sendButton.setOnClickListener(this);
		mView.discardButton.setOnClickListener(this);
		mView.selectButton.setOnClickListener(this);
		mView.messageBody.addTextChangedListener(this);
		mView.recipients.setOnClickListener(this);
		
		if (savedInstanceState != null)
			mRecipients = savedInstanceState.getStringArrayList("recipients");
		else
		{
			String recipient = getIntent().getStringExtra("recipient");
			if (recipient != null)
				mRecipients.add(recipient);
			
			String body = getIntent().getStringExtra("body");
			if (body != null)
				mView.messageBody.setText(body);
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		outState.putStringArrayList("recipients", mRecipients);
	}
	
	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.message_compose_send:
			{
				String[] recipients = new String[mRecipients.size()];
				
				mRecipients.toArray(recipients);
				mHandler.showToast(getString(R.string.message_queued_for_send));
				
				TaskController.getInstance().sendMessage(mAccount, 
						recipients, 
						mView.messageBody.getText().toString(), 
						null,
						mListener);
				
				setResult(RESULT_OK);
				finish();
			}
			break;
		case R.id.message_compose_select:
			MessageSelectRecipients.actionSelectFriends(this, mAccount, mRecipients);
			break;
		case R.id.message_compose_discard:
			setResult(RESULT_CANCELED);
			finish();
			break;
		}
	}

	private void validate()
	{
		boolean valid = 
			mView.messageBody.getText().toString().trim().length() > 0
				&& mRecipients.size() > 0;
		mView.sendButton.setEnabled(valid);
	}
	
	private void refreshRecipients()
	{
		if (mRecipients.size() < 1)
		{
			mView.recipients.setText(R.string.select_recipients);
		}
		else
		{
			mView.recipients.setText(getString(R.string.to_f, 
					TextUtils.join(", ", mRecipients)));
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
			mRecipients = data.getStringArrayListExtra("selected");
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
