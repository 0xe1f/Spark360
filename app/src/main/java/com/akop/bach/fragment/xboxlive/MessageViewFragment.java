/*
 * MessageViewFragment.java 
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

package com.akop.bach.fragment.xboxlive;

import java.text.DateFormat;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.OnImageReadyListener;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.XboxLive.Friends;
import com.akop.bach.XboxLive.Messages;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.activity.xboxlive.FriendSummary;
import com.akop.bach.activity.xboxlive.GamerProfile;
import com.akop.bach.activity.xboxlive.MessageCompose;
import com.akop.bach.fragment.AlertDialogFragment;
import com.akop.bach.fragment.AlertDialogFragment.OnOkListener;
import com.akop.bach.fragment.GenericFragment;
import com.akop.bach.parser.XboxLiveParser;

public class MessageViewFragment extends GenericFragment implements
		OnClickListener, OnOkListener 
{
	private OnImageReadyListener mAvatarLoader = new OnImageReadyListener()
	{
		@Override
		public void onImageReady(long id, Object param, final Bitmap bmp)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					synchronizeLocal();
				}
			});
		}
	};
	
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
  		Messages._ID, 
  		Messages.BODY, 
  		Messages.SENT, 
  		Messages.SENDER,
  		Messages.IS_DIRTY, 
  		Messages.GAMERPIC,
  		Messages.UID 
  	};
	
	private static final int DIALOG_CONFIRM = 2;
	
	private static final int COLUMN_BODY = 1;
	private static final int COLUMN_SENT = 2;
	private static final int COLUMN_SENDER = 3;
	private static final int COLUMN_GAMERPIC = 5;
	
	private XboxLiveAccount mAccount;
	private String mSender;
	private String mBody;
	private long mTitleId = -1;
	private TaskListener mListener = new TaskListener();
	
	public static MessageViewFragment newInstance(XboxLiveAccount account)
	{
		return newInstance(account, -1);
	}
	
	public static MessageViewFragment newInstance(XboxLiveAccount account,
			long titleId)
	{
		MessageViewFragment f = new MessageViewFragment();
		
		Bundle args = new Bundle();
		args.putParcelable("account", account);
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
		    
		    mAccount = (XboxLiveAccount)args.getParcelable("account");
		    mTitleId = args.getLong("titleId", -1);
		}
		
	    if (state != null)
	    {
			mAccount = (XboxLiveAccount)state.getParcelable("account");
			mTitleId = state.getLong("titleId");
		}
	    
		setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		if (container == null)
			return null;
		
		View layout = inflater.inflate(R.layout.xbl_fragment_message_view,
				container, false);
		
		View gamertagRow = layout.findViewById(R.id.gamertag_row);
		gamertagRow.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				if (mSender != null)
				{
					if (Friends.isFriend(getActivity(), mAccount, mSender))
						FriendSummary.actionShow(getActivity(), mAccount, mSender);
					else
						GamerProfile.actionShow(getActivity(), mAccount, mSender);
				}
			}
		});
		
		return layout;
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		
		TaskController.getInstance().removeListener(mListener);
		
		ContentResolver cr = getActivity().getContentResolver();
        cr.unregisterContentObserver(mObserver);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		
		TaskController.getInstance().addListener(mListener);
		
		ContentResolver cr = getActivity().getContentResolver();
		cr.registerContentObserver(Messages.CONTENT_URI, true, mObserver);
		
		synchronizeLocal();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		outState.putParcelable("account", mAccount);
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
	    
    	inflater.inflate(R.menu.xbl_message_view, menu);
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
	    case R.id.menu_share:
	    	share();
	    	return true;
	    }
	    return false;
	}
	
	@Override
    public void onClick(View v)
    {
		MessageCompose.actionComposeMessage(getActivity(), mAccount, mSender);
    }
	
	@Override
    public void okClicked(int code, long id, String param)
    {
		mHandler.showToast(getString(R.string.message_queued_for_delete));
		
		TaskController.getInstance().deleteMessage(mAccount, 
				Messages.getUid(getActivity(), mTitleId),
				getString(R.string.message_deleted),
				mListener);
    }
	
	public void resetTitle(long id)
	{
		mTitleId = id;
		
		synchronizeLocal();
	}
	
	private void synchronizeWithServer()
	{
		if (mTitleId >= 0)
		{
			TaskController.getInstance().synchronizeMessage(mAccount, 
					Messages.getUid(getActivity(), mTitleId), null, mListener);
		}
	}
	
	private void synchronizeLocal()
	{
		boolean messageExists = false;
		boolean isDirty = false;
		
		if (mTitleId >= 0)
		{
			ContentResolver cr = getActivity().getContentResolver();
			Cursor cursor = cr.query(Messages.CONTENT_URI, 
					new String[] { Messages.IS_DIRTY }, 
					Messages._ID + "=" + mTitleId, 
					null, null);
			
			if (cursor != null)
			{
				try
				{
					if (cursor.moveToFirst())
					{
						isDirty = cursor.getInt(0) != 0;
						messageExists = true;
					}
				}
				finally
				{
					cursor.close();
				}
			}
		}
		
		if (!messageExists)
			mTitleId = -1;
		
		if (isDirty)
			synchronizeWithServer();
		
		refreshMessageContents();
	}
	
	private void share()
	{
		if (mSender != null & mBody != null)
		{
			Intent sendIntent = new Intent(Intent.ACTION_SEND);
			sendIntent.setType("text/plain");
			sendIntent.putExtra(Intent.EXTRA_TEXT,
					getString(R.string.xbl_message_share_text, mSender, mBody));
			
			try
			{
				startActivity(Intent.createChooser(sendIntent, null));
			}
			catch(Exception e)
			{
				new AlertDialog.Builder(getActivity())
						.setTitle(R.string.error)
						.setMessage(R.string.error_sharing)
						.setPositiveButton(R.string.close, null)
						.show();
			}
		}
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
			
			TextView tvSender = (TextView)container.findViewById(R.id.message_view_sender);
			TextView tvSent = (TextView)container.findViewById(R.id.message_view_sent);
			TextView tvBody = (TextView)container.findViewById(R.id.message_view_body);
			ImageView ivSenderIcon = (ImageView)container.findViewById(R.id.avatar_icon);
			View vReplySection = container.findViewById(R.id.button_section);
			
			Button replyButton = (Button)container.findViewById(R.id.message_view_reply);
			replyButton.setOnClickListener(this);
			
			if (mTitleId >= 0)
			{
				ContentResolver cr = getActivity().getContentResolver();
				Cursor c = cr.query(Messages.CONTENT_URI, PROJECTION,
						Messages.ACCOUNT_ID + "=" + mAccount.getId() + " AND "
								+ Messages._ID + "=" + mTitleId, null, null);
				
				if (c != null)
				{
					try
					{
						if (c.moveToFirst())
						{
							mSender = c.getString(COLUMN_SENDER);
							mBody = c.getString(COLUMN_BODY);
							
							String gamerpic = c.getString(COLUMN_GAMERPIC);
							String body = mBody; 
							
							if (body == null || body.length() < 1)
								body = getString(R.string.no_text_in_message);
							
							vReplySection.setVisibility(mAccount.isGold() ? View.VISIBLE : View.GONE);
							
							tvSender.setText(mSender);
							tvSent.setText(DateFormat.getDateInstance().format(c.getLong(COLUMN_SENT)));
							tvBody.setText(body);
							
							if (gamerpic != null)
							{
								Bitmap bmp = ImageCache.getInstance().getCachedOrRequest(XboxLiveParser.getGamerpicUrl(mSender), 
										mAvatarLoader, 0,
										null, sCp);
								
								if (bmp != null)
									ivSenderIcon.setImageBitmap(bmp);
							}
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
				tvSender.setText("");
				tvSent.setText("");
				tvBody.setText("");
				ivSenderIcon.setImageResource(R.drawable.avatar_default);
				vReplySection.setVisibility(View.GONE);
			}
		}
		
        if (android.os.Build.VERSION.SDK_INT >= 11)
        	new HoneyCombHelper().invalidateMenu();
	}
}
