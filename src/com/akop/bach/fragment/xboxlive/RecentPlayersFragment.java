/*
 * RecentPlayersFragment.java 
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

import java.io.IOException;
import java.lang.ref.SoftReference;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.akop.bach.Account;
import com.akop.bach.App;
import com.akop.bach.ImageCache;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.CustomTask;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.XboxLive.GamerProfileInfo;
import com.akop.bach.XboxLive.RecentPlayers;
import com.akop.bach.XboxLive.RecentPlayersCursor;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.activity.xboxlive.CompareGames;
import com.akop.bach.activity.xboxlive.MessageCompose;
import com.akop.bach.fragment.AlertDialogFragment;
import com.akop.bach.fragment.AlertDialogFragment.OnOkListener;
import com.akop.bach.fragment.GenericFragment;
import com.akop.bach.fragment.xboxlive.FriendsFragment.RequestInformation;
import com.akop.bach.parser.AuthenticationException;
import com.akop.bach.parser.Parser;
import com.akop.bach.parser.ParserException;
import com.akop.bach.parser.XboxLiveParser;

public class RecentPlayersFragment extends GenericFragment implements
		OnItemClickListener, OnOkListener
{
	public static interface OnPlayerSelectedListener
	{
		void onPlayerSelected(GamerProfileInfo info);
	}
	
	private final int DIALOG_CONFIRM_ADD = 1;
	
	private MyCursorAdapter mAdapter = null;
	private IconCursor2 mIconCursor = null;
	private RecentPlayers mPayload;
	private XboxLiveAccount mAccount = null;
	private long mTitleId = -1;
	private ListView mListView = null;
	private TextView mMessage = null;
	private View mProgress = null;
	
	private TaskListener mRequestListener = new TaskListener()
	{
		@Override
		public void onTaskFailed(Account account, Exception e)
		{
			mHandler.showToast(Parser.getErrorMessage(getActivity(), e));
		}
		
		@Override
		public void onTaskSucceeded(Account account, Object requestParam, Object result) 
		{
			// Update friends
			synchronizeWithServer();
			TaskController.getInstance().updateFriendList(mAccount, mListener);
			
			// Show toast
			if (requestParam instanceof RequestInformation)
			{
				RequestInformation ri = (RequestInformation)requestParam;
				mHandler.showToast(getActivity().getString(ri.resId, ri.gamertag));
			}
		}
	};
	
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
					if (App.LOGV)
						e.printStackTrace();
					
					if (getActivity() != null && e != null)
						mMessage.setText(XboxLiveParser.getErrorMessage(getActivity(), e));
					
					mListView.setEmptyView(mMessage);
					mProgress.setVisibility(View.GONE);
				}
			});
		}
		
		@Override
		public void onTaskSucceeded(Account account, Object requestParam, final Object result)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					if (result != null && result instanceof RecentPlayers)
					{
						mPayload = (RecentPlayers)result;
						
						initializeAdapter();
						synchronizeLocal();
					}
					
					mMessage.setText(getString(R.string.recent_players_empty));
					
					mListView.setEmptyView(mMessage);
					mProgress.setVisibility(View.GONE);
				}
			});
		}
	};
	
	private static class ViewHolder
	{
		public TextView gamertag;
		public TextView gamerscore;
		public ImageView avatarIcon;
		public ImageView titleIcon;
		public TextView status;
	}
	
	private class MyCursorAdapter extends CursorAdapter
	{
		public MyCursorAdapter(Context context, Cursor c)
		{
			super(context, c);
		}
		
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			LayoutInflater li = (LayoutInflater)context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			ViewHolder vh = new ViewHolder();
			View view = li.inflate(R.layout.xbl_friend_of_friend_list_item, 
					parent, false);
			view.setTag(vh);
			
	        vh.gamertag = (TextView)view.findViewById(R.id.friend_gamertag);
	        vh.gamerscore = (TextView)view.findViewById(R.id.friend_gp);
			vh.avatarIcon = (ImageView)view.findViewById(R.id.friend_avatar_icon);
			vh.status = (TextView)view.findViewById(R.id.friend_status);
			vh.titleIcon = (ImageView)view.findViewById(R.id.friend_title);
			
			return view;
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			ViewHolder vh = (ViewHolder)view.getTag();
			
			vh.gamertag.setText(cursor.getString(RecentPlayersCursor.COLUMN_GAMERTAG));
			vh.status.setText(cursor.getString(RecentPlayersCursor.COLUMN_ACTIVITY));
			vh.gamerscore.setText(getString(R.string.x_f, 
					cursor.getInt(RecentPlayersCursor.COLUMN_GAMERSCORE)));
			
			vh.titleIcon.setImageResource(R.drawable.xbox_game_default);
			vh.avatarIcon.setImageResource(R.drawable.avatar_default);
			
			String iconUrl = cursor.getString(RecentPlayersCursor.COLUMN_ICON_URL);
			SoftReference<Bitmap> icon = mIconCache.get(iconUrl);
			
			if (icon != null && icon.get() != null)
			{
				// Image is in the in-memory cache
				vh.avatarIcon.setImageBitmap(icon.get());
			}
			else
			{
				// Image has likely been garbage-collected
				// Load it into the cache again
				Bitmap bmp = ImageCache.getInstance().getCachedBitmap(iconUrl);
				if (bmp != null)
				{
					mIconCache.put(iconUrl, new SoftReference<Bitmap>(bmp));
					vh.avatarIcon.setImageBitmap(bmp);
				}
				else
				{
					// Image failed to load - just use placeholder
					vh.avatarIcon.setImageResource(R.drawable.avatar_default);
				}
			}
			
			iconUrl = cursor.getString(RecentPlayersCursor.COLUMN_TITLE_ICON_URL);
			icon = mIconCache.get(iconUrl);
			
			if (icon != null && icon.get() != null)
			{
				// Image is in the in-memory cache
				vh.titleIcon.setImageBitmap(icon.get());
			}
			else
			{
				// Image has likely been garbage-collected
				// Load it into the cache again
				Bitmap bmp = ImageCache.getInstance().getCachedBitmap(iconUrl);
				if (bmp != null)
				{
					mIconCache.put(iconUrl, new SoftReference<Bitmap>(bmp));
					vh.titleIcon.setImageBitmap(bmp);
				}
				else
				{
					// Image failed to load - just use placeholder
					vh.titleIcon.setImageResource(R.drawable.xbox_game_empty_boxart);
				}
			}
		}
	}
	
	public static RecentPlayersFragment newInstance(XboxLiveAccount account)
	{
		RecentPlayersFragment f = new RecentPlayersFragment();
		
		Bundle args = new Bundle();
		args.putParcelable("account", account);
		f.setArguments(args);
		
		return f;
	}
	
    @Override
	public void onCreate(Bundle state)
	{
	    super.onCreate(state);
	    
	    Bundle args = getArguments();
	    
	    mAccount = (XboxLiveAccount)args.getSerializable("account");
	    mIconCursor = null;
	    mPayload = null;
	    mAdapter = null;
		mTitleId = -1;
		
	    if (state != null)
	    {
	    	try
	    	{
				mTitleId = state.getLong("titleId", -1);
				mPayload = (RecentPlayers)state.getSerializable("payload");
				mIconCursor = (IconCursor2)state.getSerializable("icons");
	    	}
	    	catch(Exception e)
	    	{
	    		mTitleId = -1;
	    		mPayload = null;
	    		mIconCursor = null;
	    	}
		}
	    
	    setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		if (container == null)
			return null;
		
		View layout = inflater.inflate(R.layout.xbl_fragment_plain_list,
				container, false);
		
		mMessage = (TextView)layout.findViewById(R.id.message);
		mMessage.setText(R.string.recent_players_empty);
		
		mListView = (ListView)layout.findViewById(R.id.list);
		mListView.setOnItemClickListener(this);
		mListView.setEmptyView(mMessage);
		
		initializeAdapter();
		
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
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		outState.putLong("titleId", mTitleId);
		
		if (mPayload != null)
			outState.putSerializable("payload", mPayload);
		if (mIconCursor != null)
			outState.putSerializable("icons", mIconCursor);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
	    super.onCreateOptionsMenu(menu, inflater);
	    
    	inflater.inflate(R.menu.xbl_friends_of_friend_list, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
	    switch (item.getItemId()) 
	    {
	    case R.id.menu_refresh:
	    	synchronizeWithServer();
	    	return true;
	    }
	    return false;
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		
		TaskController.getInstance().removeListener(mListener);
		TaskController.getInstance().removeListener(mRequestListener);
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		TaskController.getInstance().addListener(mListener);
		TaskController.getInstance().addListener(mRequestListener);
		
		synchronizeLocal();
		
		if (mPayload == null)
			synchronizeWithServer();
	}
	
	@Override
	public void onImageReady(final long id, Object param, Bitmap bmp)
	{
		super.onImageReady(id, param, bmp);
		
		if (mAdapter != null)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					Uri uri = ContentUris.withAppendedId(RecentPlayersCursor.CONTENT_URI, id);
					getActivity().getContentResolver().notifyChange(uri, null);
				}
			});
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id)
	{
		mTitleId = id;
		mListView.setItemChecked(pos, true);
		
		if (getActivity() instanceof OnPlayerSelectedListener)
		{
			OnPlayerSelectedListener listener = (OnPlayerSelectedListener)getActivity();
			Cursor c = (Cursor)arg0.getItemAtPosition(pos);
			
			GamerProfileInfo info = new GamerProfileInfo();
			info.IsFriend = c.getInt(RecentPlayersCursor.COLUMN_IS_FRIEND) != 0;
			info.Gamertag = c.getString(RecentPlayersCursor.COLUMN_GAMERTAG);
			info.IconUrl = c.getString(RecentPlayersCursor.COLUMN_ICON_URL);
			info.CurrentActivity = c.getString(RecentPlayersCursor.COLUMN_ACTIVITY);
			info.Gamerscore = c.getInt(RecentPlayersCursor.COLUMN_GAMERSCORE);
			info.TitleIconUrl = c.getString(RecentPlayersCursor.COLUMN_TITLE_ICON_URL);
			info.TitleId = c.getString(RecentPlayersCursor.COLUMN_TITLE_ID);
			
			listener.onPlayerSelected(info);
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		
		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo)menuInfo;
		ViewHolder vh = (ViewHolder)acmi.targetView.getTag();
		
		menu.setHeaderTitle(vh.gamertag.getText());
		
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.xbl_friends_of_friend_list_context, menu);
		
		menu.setGroupVisible(R.id.menu_group_gold, mAccount.isGold());
		menu.setGroupVisible(R.id.menu_group_friend, false);
		menu.setGroupVisible(R.id.menu_group_nonfriend, true);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem menuItem)
	{
		AdapterView.AdapterContextMenuInfo info = 
			(AdapterView.AdapterContextMenuInfo)menuItem.getMenuInfo();
		
		Cursor c = (Cursor)mAdapter.getItem(info.position);
		if (c != null)
		{
			String gamertag = c.getString(RecentPlayersCursor.COLUMN_GAMERTAG);
			
			switch (menuItem.getItemId())
			{
			case R.id.menu_compare_games:
				CompareGames.actionShow(getActivity(), mAccount, gamertag);
				return true;
			case R.id.menu_compose:
				MessageCompose.actionComposeMessage(getActivity(), 
						mAccount, gamertag);
				return true;
			case R.id.menu_send_friend_request:
				AlertDialogFragment frag = AlertDialogFragment.newInstance(DIALOG_CONFIRM_ADD, 
						getString(R.string.are_you_sure),
						getString(R.string.send_friend_request_to_f, gamertag), 
						gamertag);
				
				frag.setOnOkListener(this);
				frag.show(getFragmentManager(), "dialog");
				
				return true;
			}
		}
		
		return super.onContextItemSelected(menuItem);
	}
	
	private void initializeAdapter()
	{
		synchronized (this)
		{
			if (mPayload != null)
			{
				Cursor payloadCursor = mPayload.Players;
				IconCursor2 ic = new IconCursor2();
				
				try
				{
					while (payloadCursor.moveToNext())
					{
						ic.newRow()
							.add(payloadCursor.getLong(0))
							.add(payloadCursor.getString(RecentPlayersCursor.COLUMN_ICON_URL))
							.add(payloadCursor.getString(RecentPlayersCursor.COLUMN_TITLE_ICON_URL));
					}
				}
				catch(Exception e)
				{
					if (App.LOGV)
						e.printStackTrace();
				}
				
				mIconCursor = ic;
				mAdapter = new MyCursorAdapter(getActivity(), payloadCursor);
				mListView.setAdapter(mAdapter);
			}
		}
	}
	
	private void synchronizeLocal()
	{
		// Load gamerpics
		
		syncIcons();
	}
	
	protected void synchronizeWithServer()
	{
		mListView.setEmptyView(mProgress);
		mMessage.setVisibility(View.GONE);
		
		TaskController.getInstance().runCustomTask(mAccount, new CustomTask<RecentPlayers>()
				{
					@Override
					public void runTask() throws AuthenticationException,
							IOException, ParserException
					{
						XboxLiveParser p = new XboxLiveParser(getActivity());
						
						try
						{
							RecentPlayers players = p.fetchRecentPlayers(mAccount);
							setResult(players);
						}
						finally
						{
							p.dispose();
						}
					}
				}, mListener);
	}
	
	@Override
    protected Cursor getIconCursor()
    {
		if (getActivity() == null)
			return null;
		
		return mIconCursor;
    }
	
	@Override
    public void okClicked(int code, long id, String param)
    {
		mHandler.showToast(getString(R.string.request_queued));
		TaskController.getInstance().addFriend(mAccount, param,
				new RequestInformation(R.string.added_friend_to_friend_list_f,
						param), mRequestListener);
    }
}
