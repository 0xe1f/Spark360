/*
 * SentMessageViewFragment.java 
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

package com.akop.bach.fragment.xboxlive;

import java.text.DateFormat;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.XboxLive.Messages;
import com.akop.bach.XboxLive.SentMessages;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.activity.xboxlive.GamerProfile;
import com.akop.bach.activity.xboxlive.MessageCompose;
import com.akop.bach.fragment.AlertDialogFragment;
import com.akop.bach.fragment.AlertDialogFragment.OnOkListener;
import com.akop.bach.fragment.GenericFragment;
import com.akop.bach.parser.Parser;

public class SentMessageViewFragment extends GenericFragment implements
		OnClickListener, OnOkListener
{
	private class MyHandler extends Handler
	{
		public void showToast(final String message)
		{
			this.post(new Runnable()
			{
				@Override
				public void run()
				{
					Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
				}
			});
		}
	}
	
	class HoneyCombHelper
	{
		public void invalidateMenu()
		{
			getActivity().invalidateOptionsMenu();
		}
	}
	
	private final ContentObserver mObserver = new ContentObserver(new Handler())
	{
		@Override
		public void onChange(boolean selfUpdate)
		{
			super.onChange(selfUpdate);
			
			synchronizeLocal();
		}
    };
    
	private static final String[] PROJECTION = new String[]
  	{ 
  		SentMessages._ID, 
  		SentMessages.BODY, 
  		SentMessages.SENT, 
  		SentMessages.RECIPIENTS,
  	};
	
	private static final int DIALOG_CONFIRM = 2;
	
	private static final int COLUMN_BODY = 1;
	private static final int COLUMN_SENT = 2;
	private static final int COLUMN_SENDER = 3;
	
	private XboxLiveAccount mAccount;
	private String mRecipients;
	private long mTitleId = -1;
	private MyHandler mHandler = new MyHandler();
	private TaskListener mListener = new TaskListener();
	
	public static SentMessageViewFragment newInstance(XboxLiveAccount account)
	{
		return newInstance(account, -1);
	}
	
	public static SentMessageViewFragment newInstance(XboxLiveAccount account,
			long titleId)
	{
		SentMessageViewFragment f = new SentMessageViewFragment();
		
		Bundle args = new Bundle();
		args.putSerializable("account", account);
		args.putLong("titleId", titleId);
		f.setArguments(args);
		
		return f;
	}
	
	@Override
	public void onCreate(Bundle state)
	{
		super.onCreate(state);
		
		if (mAccount == null)
		{
		    Bundle args = getArguments();
		    
		    mAccount = (XboxLiveAccount)args.getSerializable("account");
		    mTitleId = args.getLong("titleId", -1);
		}
		
	    if (state != null)
			mTitleId = state.getLong("titleId");
	    
		setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		if (container == null)
			return null;
		
		View layout = inflater.inflate(R.layout.xbl_fragment_sent_message_view,
				container, false);
		
		return layout;
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		
		TaskController.get().removeListener(mListener);
		
		ContentResolver cr = getActivity().getContentResolver();
        cr.unregisterContentObserver(mObserver);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		
		TaskController.get().addListener(mListener);
		
		ContentResolver cr = getActivity().getContentResolver();
		cr.registerContentObserver(SentMessages.CONTENT_URI, true, mObserver);
		
		synchronizeLocal();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		outState.putLong("titleId", mTitleId);
	}
	
	@Override
	public void onImageReady(long id, Object param, Bitmap bmp)
	{
		super.onImageReady(id, param, bmp);
		
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				synchronizeLocal();
			}
		});
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
	    super.onCreateOptionsMenu(menu, inflater);
	    
    	inflater.inflate(R.menu.xbl_sent_message_view, menu);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu)
	{
	    super.onPrepareOptionsMenu(menu);
	    
	    menu.setGroupVisible(R.id.menu_group_selected, getView() != null && mTitleId >= 0);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
	    switch (item.getItemId()) 
	    {
	    case R.id.menu_delete:
	    	
	    	if (mTitleId >= 0)
	    	{
				AlertDialogFragment frag = AlertDialogFragment.newInstance(DIALOG_CONFIRM,
						getString(R.string.are_you_sure),
						getString(R.string.delete_message_q), mTitleId);
				
				frag.setOnOkListener(this);
				frag.show(getFragmentManager(), "dialog");
	    	}
	    	
	    	return true;
	    }
	    return false;
	}
	
	@Override
    public void onClick(View v)
    {
		MessageCompose.actionComposeMessage(getActivity(), mAccount, mRecipients);
    }
	
	@Override
    public void okClicked(int code, long id, String param)
    {
		Uri sentMessage = ContentUris.withAppendedId(SentMessages.CONTENT_URI, id);
		
		try
		{
			getActivity().getContentResolver().delete(sentMessage, null, null);
		}
		catch(Exception e)
		{
			mHandler.showToast(Parser.getErrorMessage(getActivity(), e));
		}
		
		synchronizeLocal();
    }
	
	public void resetTitle(long id)
	{
		mTitleId = id;
		
		synchronizeLocal();
	}
	
	private void synchronizeLocal()
	{
		boolean messageExists = false;
		
		if (mTitleId >= 0)
		{
			ContentResolver cr = getActivity().getContentResolver();
			Uri uri = ContentUris.withAppendedId(SentMessages.CONTENT_URI, mTitleId);
			
			Cursor cursor = cr.query(uri, new String[] { SentMessages._ID }, 
					Messages._ID + "=" + mTitleId, null, null);
			
			if (cursor != null)
			{
				try
				{
					if (cursor.moveToFirst())
						messageExists = true;
				}
				finally
				{
					cursor.close();
				}
			}
		}
		
		if (!messageExists)
			mTitleId = -1;
		
		refreshMessageContents();
	}
	
	private void refreshMessageContents()
	{
		View container = getView();
		if (container == null)
			return;

		if (mTitleId < 0)
		{
			container.findViewById(R.id.message).setVisibility(View.VISIBLE);
			container.findViewById(R.id.message_pane).setVisibility(View.GONE);
		}
		else
		{
			container.findViewById(R.id.message_pane).setVisibility(View.VISIBLE);
			container.findViewById(R.id.message).setVisibility(View.GONE);
			
			LinearLayout recipientList = (LinearLayout)container.findViewById(R.id.recipient_list);
			recipientList.removeAllViews();
			
			TextView tvSent = (TextView)container.findViewById(R.id.message_view_sent);
			TextView tvBody = (TextView)container.findViewById(R.id.message_view_body);
			
			LayoutInflater inflater = getActivity().getLayoutInflater();
			
			if (mTitleId >= 0)
			{
				ContentResolver cr = getActivity().getContentResolver();
				Cursor c = cr.query(SentMessages.CONTENT_URI, PROJECTION,
						SentMessages.ACCOUNT_ID + "=" + mAccount.getId() + " AND "
								+ SentMessages._ID + "=" + mTitleId, null, null);
				
				if (c != null)
				{
					try
					{
						if (c.moveToFirst())
						{
							mRecipients = c.getString(COLUMN_SENDER);
							
							String body = c.getString(COLUMN_BODY);
							
							if (body == null || body.length() < 1)
								body = getString(R.string.no_text_in_message);
							
							if (mRecipients != null)
							{
								String[] gamertags = mRecipients.split(",");
								for (final String gamertag : gamertags)
								{
									TextView item = (TextView)inflater.inflate(R.layout.xbl_recipient_item, null);
									
									item.setText(gamertag);
									if (mAccount.canSendMessages())
									{
										item.setOnClickListener(new View.OnClickListener()
										{
											@Override
											public void onClick(View v)
											{
												Context context = getActivity();
												
												if (mAccount.canSendMessages())
												{
													MessageCompose.actionComposeMessage(context, 
															mAccount, gamertag);
												}
												else
												{
													GamerProfile.actionShow(context,
															mAccount, gamertag);
												}
											}
										});
									}
									
									recipientList.addView(item);
								}
							}
							
							tvSent.setText(DateFormat.getDateInstance().format(c.getLong(COLUMN_SENT)));
							tvBody.setText(body);
						}
						else
						{
							mTitleId = -1;
						}
					}
					finally
					{
						c.close();
					}
				}
			}
			
			if (mTitleId < 0)
			{
				tvSent.setText("");
				tvBody.setText("");
			}
		}
		
        if (android.os.Build.VERSION.SDK_INT >= 11)
        	new HoneyCombHelper().invalidateMenu();
	}
}
