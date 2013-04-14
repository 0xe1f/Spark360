/*
 * MessagesFragment.java 
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

package com.akop.bach.fragment.xboxlive;

import java.lang.ref.SoftReference;
import java.text.DateFormat;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.akop.bach.Account;
import com.akop.bach.ImageCache;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.XboxLive;
import com.akop.bach.XboxLive.Messages;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.activity.xboxlive.GamerProfile;
import com.akop.bach.activity.xboxlive.MessageCompose;
import com.akop.bach.activity.xboxlive.SentMessageList;
import com.akop.bach.fragment.AlertDialogFragment;
import com.akop.bach.fragment.AlertDialogFragment.OnOkListener;
import com.akop.bach.fragment.GenericFragment;
import com.akop.bach.parser.XboxLiveParser;
import com.akop.bach.service.XboxLiveServiceClient;

public class MessagesFragment extends GenericFragment implements
		OnItemClickListener, OnOkListener
{
	public static interface OnMessageSelectedListener
	{
		void onMessageSelected(long id);
	}
	
	private static final int DIALOG_CONFIRM = 2;
	
	public static final String[] PROJ = new String[]
	{
		Messages._ID,
		Messages.BODY,
		Messages.TYPE,
		Messages.SENT,
		Messages.SENDER,
		Messages.IS_READ,
		Messages.GAMERPIC,
		Messages.UID,
		Messages.IS_DIRTY,
	};
	
	private static final int COLUMN_MESSAGE = 1;
	private static final int COLUMN_TYPE = 2;
	private static final int COLUMN_SENT = 3;
	private static final int COLUMN_SENDER = 4;
	private static final int COLUMN_IS_READ = 5;
	private static final int COLUMN_GAMERPIC = 6;
	private static final int COLUMN_IS_DIRTY = 8;
	
	private class ViewHolder
	{
		ImageView gamerpic;
		TextView summary;
		TextView sent;
		TextView sender;
		ImageView type;
	}
	
	private TaskListener mListener = new TaskListener()
	{
		@Override
		public void onTaskFailed(Account account, final Exception e)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					if (getActivity() != null && e != null)
						mMessage.setText(XboxLiveParser.getErrorMessage(getActivity(), e));
					
					mListView.setEmptyView(mMessage);
					mProgress.setVisibility(View.GONE);
				}
			});
		}
		
		@Override
		public void onTaskSucceeded(Account account, Object requestParam, Object result)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					mMessage.setText(R.string.message_list_is_empty);
					
					mListView.setEmptyView(mMessage);
					mProgress.setVisibility(View.GONE);
					
					syncIcons();
				}
			});
		}
	};
	
	private class MyCursorAdapter extends CursorAdapter
	{
		private DateFormat DATE_FORMAT = DateFormat.getDateInstance();
		
		public MyCursorAdapter(Context context, Cursor c)
		{
			super(context, c);
		}
		
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			LayoutInflater li = (LayoutInflater)context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			View row = li.inflate(R.layout.xbl_message_list_item, parent, false);
			ViewHolder vh = new ViewHolder();
			
			row.setTag(vh);
			
			vh.gamerpic = (ImageView)row.findViewById(R.id.message_gamerpic);
			vh.summary = (TextView)row.findViewById(R.id.message_summary);
			vh.sent = (TextView)row.findViewById(R.id.message_sent);
			vh.sender = (TextView)row.findViewById(R.id.message_sender);
			vh.type = (ImageView)row.findViewById(R.id.message_type);
			
			return row;
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			ViewHolder vh = (ViewHolder)view.getTag();
			
			boolean isDirty = (cursor.getInt(COLUMN_IS_DIRTY) != 0);
			boolean isUnread = (cursor.getInt(COLUMN_IS_READ) == 0);
			long sentTicks = cursor.getLong(COLUMN_SENT);
			String excerpt = cursor.getString(COLUMN_MESSAGE);
			
			if (isDirty && excerpt.length() >= 18)
				excerpt = getString(R.string.message_excerpt_f, excerpt);
			
			vh.summary.setText(excerpt);
			vh.summary.setTypeface(null, (!isUnread) ? Typeface.NORMAL : Typeface.BOLD);
			
			vh.sent.setText(DATE_FORMAT.format(sentTicks));
			vh.sender.setText(cursor.getString(COLUMN_SENDER));
			
			switch (cursor.getInt(COLUMN_TYPE))
			{
			case XboxLive.MESSAGE_VOICE:
				vh.type.setImageResource(R.drawable.xbox_voice_message_icon);
				vh.type.setVisibility(View.VISIBLE);
				break;
			default:
				vh.type.setVisibility(View.INVISIBLE);
				break;
			}
			
			String iconUrl = cursor.getString(COLUMN_GAMERPIC);
			SoftReference<Bitmap> icon = null;
			
			if (iconUrl != null)
				icon = mIconCache.get(iconUrl);
			
			if (icon != null && icon.get() != null)
			{
				// Image is in the in-memory cache
				vh.gamerpic.setImageBitmap(icon.get());
			}
			else
			{
				// Image has likely been garbage-collected
				// Load it into the cache again
				Bitmap bmp = ImageCache.getInstance().getCachedBitmap(iconUrl);
				if (bmp != null)
				{
					mIconCache.put(iconUrl, new SoftReference<Bitmap>(bmp));
					vh.gamerpic.setImageBitmap(bmp);
				}
				else
				{
					// Image failed to load - just use placeholder
					vh.gamerpic.setImageResource(R.drawable.avatar_default);
				}
			}
		}
	}
	
	private LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderCallbacks<Cursor>()
	{
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args)
		{
			return new CursorLoader(getActivity(), 
					Messages.CONTENT_URI,
					PROJ, 
					Messages.ACCOUNT_ID + "=" + mAccount.getId(), 
					null,
					Messages.DEFAULT_SORT_ORDER);
		}
		
		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data)
		{
			mAdapter.changeCursor(data);
			
			// NEWTODO
			// if (mListView.getCheckedItemCount() < 1 && mListView.getCount() > 0)
			//	mListView.setItemChecked(0, true);
			//
		}
		
		@Override
		public void onLoaderReset(Loader<Cursor> arg0)
		{
			mAdapter.changeCursor(null);
		}
	};
	
	private XboxLiveAccount mAccount = null;
	private long mTitleId = -1;
	private ListView mListView = null;
	private TextView mMessage = null;
	private View mProgress = null;
	private CursorAdapter mAdapter = null;
	
	public static MessagesFragment newInstance(XboxLiveAccount account)
	{
		MessagesFragment f = new MessagesFragment();
		
		Bundle args = new Bundle();
		args.putParcelable("account", account);
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
			ContentResolver cr = getActivity().getContentResolver();
		    
		    mAccount = (XboxLiveAccount)args.getParcelable("account");
			mTitleId = getFirstTitleId(cr.query(Messages.CONTENT_URI,
					new String[] { Messages._ID, },
					Messages.ACCOUNT_ID + "=" + mAccount.getId(), 
					null, Messages.DEFAULT_SORT_ORDER));
		}
		
	    if (state != null && state.containsKey("account"))
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
		
		View layout = inflater.inflate(R.layout.xbl_fragment_message_list,
				container, false);
		
		mAdapter = new MyCursorAdapter(getActivity(), null);
		
		View composeMessageButton = layout.findViewById(R.id.new_message);
		TextView composeMessageDesc = (TextView)layout.findViewById(R.id.compose_message_description);
		View composeMessageTitle = layout.findViewById(R.id.compose_message_title);
		
		if (composeMessageButton != null)
		{
			if (mAccount.canSendMessages())
			{
				composeMessageButton.setFocusable(true);
				composeMessageButton.setClickable(true);
				
				composeMessageTitle.setVisibility(View.VISIBLE);
				composeMessageDesc.setText(R.string.compose_new_message);
				
				composeMessageButton.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						MessageCompose.actionComposeMessage(getActivity(), mAccount, null);
					}
				});
			}
			else
			{
				composeMessageButton.setFocusable(false);
				composeMessageButton.setClickable(false);
				
				composeMessageTitle.setVisibility(View.GONE);
				composeMessageDesc.setText(R.string.compose_not_available);
			}
		}
		
		mMessage = (TextView)layout.findViewById(R.id.message);
		mMessage.setText(R.string.message_list_is_empty);
		
		mListView = (ListView)layout.findViewById(R.id.list);
		mListView.setOnItemClickListener(this);
		mListView.setAdapter(mAdapter);
		mListView.setEmptyView(mMessage);
		
		registerForContextMenu(mListView);
		
		mProgress = layout.findViewById(R.id.loading);
		
		return layout;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		
		if (mDualPane)
			mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		
		getLoaderManager().initLoader(0, null, mLoaderCallbacks);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		if (mAccount != null)
		{
			outState.putParcelable("account", mAccount);
			outState.putLong("currentId", mTitleId);
		}
	}
	
	@Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id)
    {
		mTitleId = id;
		mListView.setItemChecked(pos, true);
		
		if (getActivity() instanceof OnMessageSelectedListener)
		{
			OnMessageSelectedListener listener = (OnMessageSelectedListener)getActivity();
			listener.onMessageSelected(id);
		}
    }
	
	private void synchronizeWithServer()
	{
		mListView.setEmptyView(mProgress);
		mMessage.setVisibility(View.GONE);
		
		TaskController.getInstance().synchronizeMessages(mAccount, mListener);
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		
		TaskController.getInstance().removeListener(mListener);
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		TaskController.getInstance().addListener(mListener);
		
		XboxLiveServiceClient.clearMessageNotifications(getActivity(), mAccount);
		if (System.currentTimeMillis() - mAccount.getLastMessageUpdate() > mAccount.getMessageRefreshInterval())
			synchronizeWithServer();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
	    super.onCreateOptionsMenu(menu, inflater);
	    
    	inflater.inflate(R.menu.xbl_message_list, menu);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu)
	{
	    super.onPrepareOptionsMenu(menu);
		
	    menu.setGroupVisible(R.id.menu_group_gold, mAccount.isGold());
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
	    switch (item.getItemId()) 
	    {
	    case R.id.menu_compose:
			MessageCompose.actionComposeMessage(getActivity(), mAccount, null);
	    	return true;
	    case R.id.menu_sent_mail:
	    	SentMessageList.actionShow(getActivity(), mAccount);
	    	return true;
	    case R.id.menu_refresh:
	    	synchronizeWithServer();
	    	return true;
	    }
	    
	    return false;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		
		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo)menuInfo;
		ViewHolder vh = (ViewHolder)acmi.targetView.getTag();
		
		getActivity().getMenuInflater().inflate(R.menu.xbl_message_list_context, 
				menu);
		
		menu.setHeaderTitle(getString(R.string.message_from_f,
				vh.sender.getText()));
		
		menu.setGroupVisible(R.id.menu_group_gold_required, mAccount.isGold());
		menu.setGroupVisible(R.id.menu_group_unread, 
				Messages.isUnreadTextMessage(getActivity(), acmi.id));
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem menuItem)
	{
		AdapterView.AdapterContextMenuInfo info = 
			(AdapterView.AdapterContextMenuInfo)menuItem.getMenuInfo();
		
		if (info.targetView.getTag() instanceof ViewHolder)
		{
			ViewHolder vh = (ViewHolder)info.targetView.getTag();
			
			switch (menuItem.getItemId())
			{
			case R.id.menu_mark_as_read:
				TaskController.getInstance().synchronizeMessage(mAccount, 
						Messages.getUid(getActivity(), info.id), null, mListener);
				return true;
			case R.id.menu_reply:
				MessageCompose.actionComposeMessage(getActivity(), mAccount, 
						vh.sender.getText().toString());
				return true;
			case R.id.menu_view_profile:
				GamerProfile.actionShow(getActivity(), mAccount, 
						vh.sender.getText().toString());
				return true;
			case R.id.menu_delete:
				AlertDialogFragment frag = AlertDialogFragment.newInstance(DIALOG_CONFIRM,
						getString(R.string.are_you_sure),
						getString(R.string.delete_message_from_f, vh.sender.getText()), 
						Messages.getUid(getActivity(), info.id));
				frag.setOnOkListener(this);
				frag.show(getFragmentManager(), "dialog");
				
				return true;
			}
		}
		
		return super.onContextItemSelected(menuItem);
	}

	@Override
    public void okClicked(int code, long id, String param)
    {
		if (code == DIALOG_CONFIRM)
		{
			mHandler.showToast(getString(R.string.message_queued_for_delete));
			TaskController.getInstance().deleteMessage(mAccount,
					id, getString(R.string.message_deleted), mListener);
		}
    }
	
	@Override
	public void onImageReady(long id, Object param, Bitmap bmp)
	{
		super.onImageReady(id, param, bmp);
		
		if (getActivity() != null)
		{
			getActivity().getContentResolver().notifyChange(
					ContentUris.withAppendedId(Messages.CONTENT_URI, id), null);
		}
	}
	
	@Override
    protected Cursor getIconCursor()
    {
		if (getActivity() == null)
			return null;
		
		ContentResolver cr = getActivity().getContentResolver();
		return cr.query(Messages.CONTENT_URI,
				new String[] { Messages._ID, Messages.GAMERPIC },
				Messages.ACCOUNT_ID + "=" + mAccount.getId(), 
				null, Messages.DEFAULT_SORT_ORDER);
    }
}
