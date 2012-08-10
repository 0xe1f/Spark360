/*
 * FriendsFragment.java 
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

import java.lang.ref.SoftReference;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
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
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.akop.bach.IAccount;
import com.akop.bach.ImageCache;
import com.akop.bach.Preferences;
import com.akop.bach.R;
import com.akop.bach.SectionedAdapter;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.XboxLive;
import com.akop.bach.XboxLive.Friends;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.activity.xboxlive.CompareGames;
import com.akop.bach.activity.xboxlive.FindGamer;
import com.akop.bach.activity.xboxlive.FriendsOfFriendList;
import com.akop.bach.activity.xboxlive.GamerProfile;
import com.akop.bach.activity.xboxlive.MessageCompose;
import com.akop.bach.activity.xboxlive.RecentPlayerList;
import com.akop.bach.fragment.AlertDialogFragment;
import com.akop.bach.fragment.AlertDialogFragment.OnOkListener;
import com.akop.bach.fragment.GenericFragment;
import com.akop.bach.parser.Parser;
import com.akop.bach.parser.XboxLiveParser;
import com.akop.bach.service.XboxLiveServiceClient;
import com.akop.bach.uiwidget.XboxLiveFriendListItem;
import com.akop.bach.uiwidget.XboxLiveFriendListItem.OnStarClickListener;

public class FriendsFragment extends GenericFragment implements
        OnItemClickListener, OnStarClickListener, OnOkListener
{
	private static int DIALOG_CONFIRM_REMOVE = 1;
	
	private MyHandler mHandler = new MyHandler();
	private ListView mListView = null;
	private TextView mMessage = null;
	private View mProgress = null;
	private long mTitleId = -1;
	private String mSelectedGamertag;
	
	private SectionedAdapter mAdapter = null;
	private XboxLiveAccount mAccount = null;
	
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
	
	private static final String[] PROJECTION = 
	{ 
		Friends._ID, 
		Friends.GAMERTAG,
		Friends.GAMERSCORE, 
		Friends.CURRENT_ACTIVITY, 
		Friends.STATUS,
		Friends.STATUS_CODE, 
		Friends.ICON_URL, 
		Friends.TITLE_URL,
		Friends.IS_FAVORITE, 
	};
	
	private static final int COLUMN_GAMERTAG = 1;
	private static final int COLUMN_POINTS = 2;
	private static final int COLUMN_ACTIVITY = 3;
	private static final int COLUMN_STATUS_CODE = 5;
	private static final int COLUMN_ICON_URL = 6;
	private static final int COLUMN_TITLE_ICON_URL = 7;
	private static final int COLUMN_IS_FAVORITE = 8;
	
	private static class ViewHolder
	{
		public TextView gamertag;
		public TextView currentActivity;
		public TextView gamerscore;
		public ImageView avatar;
		public ImageView isFavorite;
		public ImageView titleIcon;
	}
	
	private final ContentObserver mObserver = new ContentObserver(new Handler())
	{
		@Override
		public void onChange(boolean selfUpdate)
		{
			super.onChange(selfUpdate);
			
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					updateLastUpdateTime();
				}
			});
		}
    };
    
	public static class RequestInformation
	{
		public int resId;
		public String gamertag;

		public RequestInformation(int resId, String gamertag)
		{
			this.resId = resId;
			this.gamertag = gamertag;
		}
	}
	
	public static interface OnFriendSelectedListener
	{
		void onFriendSelected(long id);
	}
	
	private TaskListener mRequestListener = new TaskListener("XBoxCancelRequest")
	{
		@Override
		public void onTaskFailed(IAccount account, final Exception e)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					Toast toast = Toast.makeText(getActivity(), 
							Parser.getErrorMessage(getActivity(), e), Toast.LENGTH_LONG);
					
					toast.show();
				}
			});
		}
		
		@Override
		public void onTaskSucceeded(IAccount account, final Object requestParam, Object result)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					// Update friends
					TaskController.getInstance().updateFriendList(mAccount, mListener);
					
					// Show toast
					RequestInformation ri = (RequestInformation)requestParam;
					Toast toast = Toast.makeText(getActivity(), 
							getString(ri.resId, ri.gamertag), Toast.LENGTH_LONG);
					toast.show();
				}
			});
		}
	};
	
	private TaskListener mListener = new TaskListener("XBoxFriends")
	{
		@Override
		public void onTaskFailed(IAccount account, final Exception e)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					mMessage.setText(XboxLiveParser.getErrorMessage(getActivity(), e));
					
					mListView.setEmptyView(mMessage);
					mProgress.setVisibility(View.GONE);
				}
			});
		}
		
		@Override
		public void onTaskSucceeded(IAccount account, Object requestParam, Object result)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					mMessage.setText(R.string.no_friends);
					
					mListView.setEmptyView(mMessage);
					mProgress.setVisibility(View.GONE);
					
					syncIcons();
				}
			});
		}
	};
	
	private class MyCursorAdapter extends CursorAdapter
	{
		private BaseAdapter mParent;
		
		public MyCursorAdapter(Context context, Cursor c)
		{
			super(context, c);
		}
		
		public MyCursorAdapter(Context context, BaseAdapter parent, Cursor c)
		{
			super(context, c);
			
			this.mParent = parent;
		}
		
		@Override
		protected void onContentChanged()
		{
			super.onContentChanged();
			
			if (mParent != null)
				mParent.notifyDataSetChanged();
		}
		
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			LayoutInflater li = (LayoutInflater)context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			XboxLiveFriendListItem view = (XboxLiveFriendListItem)li.inflate(R.layout.xbl_friend_list_item, 
					parent, false);
			ViewHolder vh = new ViewHolder();
			
			view.setTag(vh);
			
			vh.gamertag = (TextView) view.findViewById(R.id.friend_gamertag);
			vh.currentActivity = (TextView) view
					.findViewById(R.id.friend_description);
			vh.gamerscore = (TextView) view.findViewById(R.id.friend_gp);
			vh.avatar = (ImageView) view.findViewById(R.id.friend_avatar_icon);
			vh.isFavorite = (ImageView) view.findViewById(R.id.friend_favorite);
			vh.titleIcon = (ImageView)view.findViewById(R.id.friend_title);
						
			return view;
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			ViewHolder vh = (ViewHolder)view.getTag();
			
			if (vh == null || !(view instanceof XboxLiveFriendListItem))
				return;
			
			XboxLiveFriendListItem friend = (XboxLiveFriendListItem) view;
			
			friend.mFriendId = cursor.getLong(0);
			friend.mStatusCode = cursor.getInt(COLUMN_STATUS_CODE);
			friend.mGamertag = cursor.getString(COLUMN_GAMERTAG);
			friend.mIsFavorite = (cursor.getInt(COLUMN_IS_FAVORITE) != 0);
			friend.mClickListener = FriendsFragment.this;
			
			vh.gamertag.setText(cursor.getString(COLUMN_GAMERTAG));
			vh.currentActivity.setText(cursor.getString(COLUMN_ACTIVITY));
			vh.gamerscore.setText(context.getString(R.string.x_f,
					cursor.getInt(COLUMN_POINTS)));
			
			String iconUrl = cursor.getString(COLUMN_ICON_URL);
			SoftReference<Bitmap> icon = mIconCache.get(iconUrl);
			
			if (icon != null && icon.get() != null)
			{
				// Image is in the in-memory cache
				vh.avatar.setImageBitmap(icon.get());
			}
			else
			{
				// Image has likely been garbage-collected
				// Load it into the cache again
				Bitmap bmp = ImageCache.getInstance().getCachedBitmap(iconUrl);
				if (bmp != null)
				{
					mIconCache.put(iconUrl, new SoftReference<Bitmap>(bmp));
					vh.avatar.setImageBitmap(bmp);
				}
				else
				{
					// Image failed to load - just use placeholder
					vh.avatar.setImageResource(R.drawable.avatar_default);
				}
			}
			
			iconUrl = cursor.getString(COLUMN_TITLE_ICON_URL);
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
			
			vh.isFavorite.setImageResource(friend.mIsFavorite 
					? R.drawable.favorite_on : R.drawable.favorite_off);
		}
	}
	
	public static FriendsFragment newInstance(XboxLiveAccount account)
	{
		FriendsFragment f = new FriendsFragment();
		
		Bundle args = new Bundle();
		args.putSerializable("account", account);
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
		    
		    mAccount = (XboxLiveAccount)args.getSerializable("account");
			mTitleId = getFirstTitleId(cr.query(Friends.CONTENT_URI,
					new String[] { Friends._ID, },
					Friends.ACCOUNT_ID + "=" + mAccount.getId(), 
					null, Friends.DEFAULT_SORT_ORDER));
		}
		
	    if (state != null && state.containsKey("account"))
	    {
			mAccount = (XboxLiveAccount)state.getSerializable("account");
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
		
		View layout = inflater.inflate(R.layout.xbl_fragment_friend_list,
				container, false);
		
		String query = Friends.ACCOUNT_ID + "=" + mAccount.getId();
		SectionedAdapter adapter = new SectionedAdapter(getActivity(),
				R.layout.xbl_friend_list_header);
		
		adapter.addSection(
				getString(R.string.pending_count),
				new MyCursorAdapter(getActivity(), adapter, getActivity().managedQuery(
						Friends.CONTENT_URI, PROJECTION, query + " AND ("
								+ Friends.STATUS_CODE + "="
								+ XboxLive.STATUS_INVITE_RCVD + " OR "
								+ Friends.STATUS_CODE + "="
								+ XboxLive.STATUS_INVITE_SENT + ")", null,
						Friends.DEFAULT_SORT_ORDER)));
		
		adapter.addSection(
				getString(R.string.online_count),
				new MyCursorAdapter(getActivity(), adapter, getActivity().managedQuery(
						Friends.CONTENT_URI, PROJECTION, query + " AND "
								+ Friends.STATUS_CODE + "="
								+ XboxLive.STATUS_ONLINE, null,
						Friends.DEFAULT_SORT_ORDER)));
		
		adapter.addSection(
				getString(R.string.offline_count),
				new MyCursorAdapter(getActivity(), adapter, getActivity().managedQuery(
						Friends.CONTENT_URI, PROJECTION, query + " AND "
								+ Friends.STATUS_CODE + "="
								+ XboxLive.STATUS_OFFLINE, null,
						Friends.DEFAULT_SORT_ORDER)));
		mAdapter = adapter;//new MyCursorAdapter(getActivity(), null);
		
		mMessage = (TextView)layout.findViewById(R.id.message);
		mMessage.setText(R.string.no_friends);
		
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
		
		//getLoaderManager().initLoader(0, null, mLoaderCallbacks);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		if (mAccount != null)
		{
			outState.putSerializable("account", mAccount);
			outState.putLong("currentId", mTitleId);
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id)
	{
		XboxLiveFriendListItem item = (XboxLiveFriendListItem)arg1;
		
		mTitleId = item.mFriendId;
		mListView.setItemChecked(pos, true);
		
		OnFriendSelectedListener listener = (OnFriendSelectedListener)getActivity();
		listener.onFriendSelected(mTitleId);
	}
	
	@Override
	public void onImageReady(long id, Object param, Bitmap bmp)
	{
		super.onImageReady(id, param, bmp);
		
		if (getActivity() != null)
		{
			getActivity().getContentResolver().notifyChange(
					ContentUris.withAppendedId(Friends.CONTENT_URI, id), null);
		}
	}
	
	@Override
    protected Cursor getIconCursor()
    {
		if (getActivity() == null)
			return null;
		
		ContentResolver cr = getActivity().getContentResolver();
		return cr.query(Friends.CONTENT_URI,
				new String[] { Friends._ID, Friends.ICON_URL, Friends.TITLE_URL },
				Friends.ACCOUNT_ID + "=" + mAccount.getId(), 
				null, Friends.DEFAULT_SORT_ORDER);
    }
	
	private void updateLastUpdateTime()
	{
		if (getView() != null)
		{
			TextView lastUpdated = (TextView)getView().findViewById(R.id.friends_last_updated);
			
			if (lastUpdated != null)
			{
				String lastUpdateText;
				mAccount.refresh(Preferences.get(getActivity()));
				
				if (mAccount.getLastFriendUpdate() < 1)
				{
					lastUpdateText = getString(R.string.not_yet_updated);
				}
				else
				{
					lastUpdateText = getString(R.string.last_updated_f,
							DateUtils.getRelativeDateTimeString(
									getActivity(),
									mAccount.getLastFriendUpdate(),
									DateUtils.MINUTE_IN_MILLIS,
									DateUtils.WEEK_IN_MILLIS, 0));
				}
				
				lastUpdated.setText(lastUpdateText);
			}
		}
	}
	
	private void synchronizeWithServer()
	{
		mListView.setEmptyView(mProgress);
		mMessage.setVisibility(View.GONE);
		
		TaskController.getInstance().updateFriendList(mAccount, mListener);
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		
		TaskController.getInstance().removeListener(mListener);
		TaskController.getInstance().removeListener(mRequestListener);
		
		ContentResolver cr = getActivity().getContentResolver();
        cr.unregisterContentObserver(mObserver);
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		TaskController.getInstance().addListener(mListener);
		TaskController.getInstance().addListener(mRequestListener);
		
		updateLastUpdateTime();
		
		XboxLiveServiceClient.clearFriendNotifications(getActivity(), mAccount);
		XboxLiveServiceClient.clearBeaconNotifications(getActivity(), mAccount);
		
		ContentResolver cr = getActivity().getContentResolver();
        cr.registerContentObserver(Friends.CONTENT_URI, true, mObserver);
        
		if (System.currentTimeMillis() - mAccount.getLastFriendUpdate() > mAccount.getFriendRefreshInterval())
			synchronizeWithServer();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
	    super.onCreateOptionsMenu(menu, inflater);
	    
    	inflater.inflate(R.menu.xbl_friend_list, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
	    switch (item.getItemId()) 
	    {
		case R.id.menu_search_friends:
			return getActivity().onSearchRequested();
		case R.id.menu_recent_players:
			RecentPlayerList.actionShow(getActivity(), mAccount);
			return true;
		case R.id.menu_find_gamer:
			FindGamer.actionShow(getActivity(), this, 1);
			return true;
		case R.id.menu_refresh:
			TaskController.getInstance().updateFriendList(mAccount, mListener);
			return true;
	    }
	    
	    return false;
	}
	
	@Override
    public void starClicked(long id, boolean isSet)
    {
		ContentValues cv = new ContentValues();
		cv.put(Friends.IS_FAVORITE, (isSet) ? 0 : 1);
		
		ContentResolver cr = getActivity().getContentResolver();
		Uri uri = ContentUris.withAppendedId(Friends.CONTENT_URI, id);
		
		cr.update(uri, cv, null, null);
		cr.notifyChange(uri, null);
    }
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		if (resultCode != Activity.RESULT_OK)
			return;
		
		if (requestCode == 1)
		{
			GamerProfile.actionShow(getActivity(), mAccount,
					data.getStringExtra("gamertag"));
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		
		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo)menuInfo;
		
		XboxLiveFriendListItem item = (XboxLiveFriendListItem)acmi.targetView;
		ViewHolder vh = (ViewHolder)acmi.targetView.getTag();
		menu.setHeaderTitle(vh.gamertag.getText());
		
		getActivity().getMenuInflater().inflate(R.menu.xbl_friend_list_context, 
				menu);
		
		int statusCode = Friends.getStatusCode(getActivity(), item.mFriendId);
		
		if (statusCode == XboxLive.STATUS_INVITE_RCVD)
		{
			menu.setGroupVisible(R.id.menu_group_invite_rcvd, true);
			menu.setGroupVisible(R.id.menu_group_invite_sent, false);
			menu.setGroupVisible(R.id.menu_group_friend, false);
			menu.setGroupVisible(R.id.menu_group_gold, false);
		}
		else if (statusCode == XboxLive.STATUS_INVITE_SENT)
		{
			menu.setGroupVisible(R.id.menu_group_invite_rcvd, false);
			menu.setGroupVisible(R.id.menu_group_invite_sent, true);
			menu.setGroupVisible(R.id.menu_group_friend, false);
			menu.setGroupVisible(R.id.menu_group_gold, false);
		}
		else
		{
			menu.setGroupVisible(R.id.menu_group_invite_rcvd, false);
			menu.setGroupVisible(R.id.menu_group_invite_sent, false);
			menu.setGroupVisible(R.id.menu_group_friend, true);
			menu.setGroupVisible(R.id.menu_group_gold, mAccount.isGold());
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem menuItem)
	{
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem
				.getMenuInfo();
		XboxLiveFriendListItem friend = (XboxLiveFriendListItem)info.targetView;
		
		mSelectedGamertag = friend.mGamertag;
		TaskController controller = TaskController.getInstance();
		
		switch (menuItem.getItemId())
		{
		case R.id.menu_view_friends:
			FriendsOfFriendList.actionShow(getActivity(), 
					mAccount, mSelectedGamertag);
			return true;
		case R.id.menu_compose:
			MessageCompose.actionComposeMessage(getActivity(), mAccount,
					mSelectedGamertag);
			return true;
		case R.id.menu_remove_friend:
			AlertDialogFragment frag = AlertDialogFragment.newInstance(DIALOG_CONFIRM_REMOVE, 
					getString(R.string.are_you_sure),
					getString(R.string.remove_from_friends_q_f, friend.mGamertag), 
					friend.mFriendId);
			frag.setOnOkListener(this);
			frag.show(getFragmentManager(), "dialog");
			return true;
		case R.id.menu_accept_friend:
			mHandler.showToast(getString(R.string.request_queued));
			controller.acceptFriendRequest(mAccount, mSelectedGamertag,
					new RequestInformation(
							R.string.accepted_friend_request_from_f,
							mSelectedGamertag), mRequestListener);
			return true;
		case R.id.menu_reject_friend:
			mHandler.showToast(getString(R.string.request_queued));
			controller.rejectFriendRequest(mAccount, mSelectedGamertag,
					new RequestInformation(
							R.string.declined_friend_request_from_f,
							mSelectedGamertag), mRequestListener);
			return true;
		case R.id.menu_cancel_friend:
			mHandler.showToast(getString(R.string.request_queued));
			controller.cancelFriendRequest(mAccount, mSelectedGamertag,
					new RequestInformation(
							R.string.cancelled_friend_request_to_f,
							mSelectedGamertag), mRequestListener);
			return true;
		case R.id.menu_compare_games:
			CompareGames.actionShow(getActivity(), mAccount, 
					mSelectedGamertag);
			return true;
		}
		
		return super.onContextItemSelected(menuItem);
	}

	@Override
    public void okClicked(int code, long id, String param)
    {
		if (code == DIALOG_CONFIRM_REMOVE)
		{
			mHandler.showToast(getString(R.string.request_queued));
			TaskController.getInstance()
					.removeFriend(mAccount, mSelectedGamertag,
							new RequestInformation(R.string.removed_friend_from_friend_list_f, mSelectedGamertag),
							mRequestListener);
		}
    }
}
