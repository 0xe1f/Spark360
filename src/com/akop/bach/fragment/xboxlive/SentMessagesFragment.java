/*
 * SentMessagesFragment.java 
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
import android.database.Cursor;
import android.net.Uri;
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
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.akop.bach.R;
import com.akop.bach.XboxLive;
import com.akop.bach.XboxLive.SentMessages;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.activity.xboxlive.MessageCompose;
import com.akop.bach.fragment.AlertDialogFragment;
import com.akop.bach.fragment.AlertDialogFragment.OnOkListener;
import com.akop.bach.fragment.GenericFragment;
import com.akop.bach.parser.Parser;

public class SentMessagesFragment extends GenericFragment implements
		OnItemClickListener, OnOkListener
{
	public static interface OnMessageSelectedListener
	{
		void onMessageSelected(long id);
	}
	
	private static final int DIALOG_CONFIRM = 2;
	
	public static final String[] PROJ = new String[]
	{
		SentMessages._ID,
		SentMessages.PREVIEW,
		SentMessages.TYPE,
		SentMessages.SENT,
		SentMessages.RECIPIENTS,
	};
	
	private static final int COLUMN_PREVIEW = 1;
	private static final int COLUMN_TYPE = 2;
	private static final int COLUMN_SENT = 3;
	private static final int COLUMN_RECIPIENTS = 4;
	
	private class ViewHolder
	{
		TextView summary;
		TextView sent;
		TextView sender;
		ImageView type;
	}
	
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
			
			View row = li.inflate(R.layout.xbl_sent_message_list_item, parent, false);
			ViewHolder vh = new ViewHolder();
			
			row.setTag(vh);
			
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
			
			long sentTicks = cursor.getLong(COLUMN_SENT);
			String excerpt = cursor.getString(COLUMN_PREVIEW);
			
			String recipients = cursor.getString(COLUMN_RECIPIENTS);
			int index = recipients.indexOf(",");
			if (index >= 0)
				recipients = getString(R.string.sent_recipients_more_f, 
						recipients.substring(0, index));
			
			vh.summary.setText(excerpt);
			vh.sent.setText(DATE_FORMAT.format(sentTicks));
			vh.sender.setText(recipients);
			
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
		}
	}
	
	private LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderCallbacks<Cursor>()
	{
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args)
		{
			return new CursorLoader(getActivity(), 
					SentMessages.CONTENT_URI,
					PROJ, 
					SentMessages.ACCOUNT_ID + "=" + mAccount.getId(), 
					null,
					SentMessages.DEFAULT_SORT_ORDER);
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
	private CursorAdapter mAdapter = null;
	
	public static SentMessagesFragment newInstance(XboxLiveAccount account)
	{
		SentMessagesFragment f = new SentMessagesFragment();
		
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
			mTitleId = getFirstTitleId(cr.query(SentMessages.CONTENT_URI,
					new String[] { SentMessages._ID, },
					SentMessages.ACCOUNT_ID + "=" + mAccount.getId(), 
					null, SentMessages.DEFAULT_SORT_ORDER));
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
		
		View layout = inflater.inflate(R.layout.xbl_fragment_sent_message_list,
				container, false);
		
		mAdapter = new MyCursorAdapter(getActivity(), null);
		
		mMessage = (TextView)layout.findViewById(R.id.message);
		mMessage.setText(R.string.no_sent_messages);
		
		mListView = (ListView)layout.findViewById(R.id.list);
		mListView.setOnItemClickListener(this);
		mListView.setAdapter(mAdapter);
		mListView.setEmptyView(mMessage);
		
		registerForContextMenu(mListView);
		
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
	
	@Override
	public void onPause()
	{
		super.onPause();
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
	    super.onCreateOptionsMenu(menu, inflater);
	    
    	inflater.inflate(R.menu.xbl_sent_message_list, menu);
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
	    }
	    
	    return false;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		
		getActivity().getMenuInflater().inflate(R.menu.xbl_sent_message_list_context, 
				menu);
		
		menu.setHeaderTitle(getString(R.string.sent_message));
		menu.setGroupVisible(R.id.menu_group_gold_required, mAccount.isGold());
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem menuItem)
	{
		AdapterView.AdapterContextMenuInfo info = 
			(AdapterView.AdapterContextMenuInfo)menuItem.getMenuInfo();
		
		if (info.targetView.getTag() instanceof ViewHolder)
		{
			switch (menuItem.getItemId())
			{
			case R.id.menu_delete:
				AlertDialogFragment frag = AlertDialogFragment.newInstance(DIALOG_CONFIRM,
						getString(R.string.are_you_sure),
						getString(R.string.delete_message_q), 
						info.id);
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
			Uri sentMessage = ContentUris.withAppendedId(SentMessages.CONTENT_URI, id);
			
			try
			{
				getActivity().getContentResolver().delete(sentMessage, null, null);
			}
			catch(Exception e)
			{
				mHandler.showToast(Parser.getErrorMessage(getActivity(), e));
			}
		}
    }
}
