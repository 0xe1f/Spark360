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

package com.akop.bach.fragment.playstation;

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

import com.akop.bach.Account;
import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.CachePolicy;
import com.akop.bach.PSN;
import com.akop.bach.PSN.Friends;
import com.akop.bach.Preferences;
import com.akop.bach.PsnAccount;
import com.akop.bach.R;
import com.akop.bach.SectionedAdapter;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.activity.playstation.CompareGames;
import com.akop.bach.activity.playstation.FindGamer;
import com.akop.bach.activity.playstation.GamerProfile;
import com.akop.bach.fragment.GenericFragment;
import com.akop.bach.parser.Parser;
import com.akop.bach.service.PsnServiceClient;
import com.akop.bach.uiwidget.PsnFriendListItem;
import com.akop.bach.uiwidget.PsnFriendListItem.OnStarClickListener;

public class FriendsFragment extends GenericFragment implements
        OnItemClickListener, OnStarClickListener
{
	private ListView mListView = null;
	private TextView mMessage = null;
	private View mProgress = null;
	private long mTitleId = -1;
	private CachePolicy mCp = null;
	
	private SectionedAdapter mAdapter = null;
	private PsnAccount mAccount = null;
	
	private static final String[] PROJECTION = 
	{
		Friends._ID,
		Friends.ONLINE_ID,
		Friends.LEVEL,
		Friends.ONLINE_STATUS,
		Friends.TROPHIES_BRONZE,
		Friends.TROPHIES_SILVER,
		Friends.TROPHIES_GOLD,
		Friends.TROPHIES_PLATINUM,
		Friends.PLAYING,
		Friends.ICON_URL,
		Friends.IS_FAVORITE,
		Friends.MEMBER_TYPE,
	};
	
	private static final int COLUMN_ONLINE_ID = 1;
	private static final int COLUMN_LEVEL = 2;
	private static final int COLUMN_ONLINE_STATUS = 3;
	private static final int COLUMN_TROPHIES_BRONZE = 4;
	private static final int COLUMN_TROPHIES_SILVER = 5;
	private static final int COLUMN_TROPHIES_GOLD = 6;
	private static final int COLUMN_TROPHIES_PLATINUM = 7;
	private static final int COLUMN_CURRENT_ACTIVITY = 8;
	private static final int COLUMN_ICON_URL = 9;
	private static final int COLUMN_IS_FAVORITE = 10;
	private static final int COLUMN_MEMBER_TYPE = 11;
	
	private static class ViewHolder
	{
		public TextView onlineId;
		public TextView trophiesBronze;
		public TextView trophiesSilver;
		public TextView trophiesGold;
		public TextView trophiesPlat;
		public TextView trophiesAll;
		public TextView level;
		public TextView currentActivity;
		public ImageView avatar;
		public ImageView isFavorite;
		public ImageView isPlus;
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
	
	private TaskListener mListener = new TaskListener("PsnFriends")
	{
		@Override
		public void onTaskFailed(Account account, final Exception e)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					mMessage.setText(Parser.getErrorMessage(getActivity(), e));
					
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
					mMessage.setText(R.string.friends_none);
					
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
			
			PsnFriendListItem view = (PsnFriendListItem)li.inflate(R.layout.psn_friend_list_item, 
					parent, false);
			ViewHolder vh = new ViewHolder();
			
			view.setTag(vh);
			
			vh.onlineId = (TextView)view.findViewById(R.id.friend_online_id);
			vh.trophiesBronze = (TextView)view.findViewById(R.id.profile_trophies_bronze);
			vh.trophiesSilver = (TextView)view.findViewById(R.id.profile_trophies_silver);
			vh.trophiesGold = (TextView)view.findViewById(R.id.profile_trophies_gold);
			vh.trophiesPlat = (TextView)view.findViewById(R.id.profile_trophies_plat);
			vh.trophiesAll = (TextView)view.findViewById(R.id.profile_trophies_all);
			vh.level = (TextView)view.findViewById(R.id.profile_level);
			vh.currentActivity = (TextView)view.findViewById(R.id.friend_playing);
			vh.avatar = (ImageView)view.findViewById(R.id.friend_avatar_icon);
			vh.isFavorite = (ImageView)view.findViewById(R.id.friend_favorite);
			vh.isPlus = (ImageView)view.findViewById(R.id.friend_plus);
			
			return view;
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			ViewHolder vh = (ViewHolder)view.getTag();
			
			if (vh == null || !(view instanceof PsnFriendListItem))
				return;
			
			PsnFriendListItem friend = (PsnFriendListItem) view;
			
			friend.mFriendId = cursor.getLong(0);
			friend.mStatusCode = cursor.getInt(COLUMN_ONLINE_STATUS);
            friend.mIsFavorite = (cursor.getInt(COLUMN_IS_FAVORITE) != 0);
            friend.mOnlineId = cursor.getString(COLUMN_ONLINE_ID);
            friend.mClickListener = FriendsFragment.this;
			
            vh.onlineId.setText(friend.mOnlineId);
            
            String activity = "";
            if (!cursor.isNull(COLUMN_CURRENT_ACTIVITY))
            	activity = cursor.getString(COLUMN_CURRENT_ACTIVITY);
            
            vh.currentActivity.setText(activity);
            
            int bronze = cursor.getInt(COLUMN_TROPHIES_BRONZE);
            int silver = cursor.getInt(COLUMN_TROPHIES_SILVER);
            int gold = cursor.getInt(COLUMN_TROPHIES_GOLD);
            int plat = cursor.getInt(COLUMN_TROPHIES_PLATINUM);
            boolean isPlus = (cursor.getInt(COLUMN_MEMBER_TYPE) 
            		& PSN.MEMBER_TYPE_PLUS) != 0;
            
			vh.trophiesBronze.setText(String.valueOf(bronze));
			vh.trophiesSilver.setText(String.valueOf(silver));
			vh.trophiesGold.setText(String.valueOf(gold));
			vh.trophiesPlat.setText(String.valueOf(plat));
			vh.trophiesAll.setText(String.valueOf(bronze + silver + gold + plat));
			vh.level.setText(String.valueOf(cursor.getInt(COLUMN_LEVEL)));
			vh.isPlus.setVisibility(isPlus ? View.VISIBLE : View.GONE);
			
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
				Bitmap bmp = ImageCache.getInstance().getCachedBitmap(iconUrl, mCp);
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
			
			vh.isFavorite.setImageResource(friend.mIsFavorite 
					? R.drawable.favorite_on : R.drawable.favorite_off);
		}
	}
	
	public static FriendsFragment newInstance(PsnAccount account)
	{
		FriendsFragment f = new FriendsFragment();
		
		Bundle args = new Bundle();
		args.putParcelable("account", account);
		f.setArguments(args);
		
		return f;
	}
	
	@Override
	public void onCreate(Bundle state)
	{
	    super.onCreate(state);
	    
		mCp = new CachePolicy();
		mCp.resizeHeight = 96;
		
		if (mAccount == null)
		{
		    Bundle args = getArguments();
			ContentResolver cr = getActivity().getContentResolver();
		    
		    mAccount = (PsnAccount)args.getParcelable("account");
			mTitleId = getFirstTitleId(cr.query(Friends.CONTENT_URI,
					new String[] { Friends._ID, },
					Friends.ACCOUNT_ID + "=" + mAccount.getId(), 
					null, Friends.DEFAULT_SORT_ORDER));
		}
		
	    if (state != null && state.containsKey("account"))
	    {
			mAccount = (PsnAccount)state.getParcelable("account");
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
		
		View layout = inflater.inflate(R.layout.psn_fragment_friend_list,
				container, false);
		
		String query = Friends.ACCOUNT_ID + "=" + mAccount.getId();
		SectionedAdapter adapter = new SectionedAdapter(getActivity(),
				R.layout.psn_friend_list_header);
		
		adapter.addSection(getString(R.string.pending_count),
				new MyCursorAdapter(getActivity(), adapter, 
						getActivity().managedQuery(Friends.CONTENT_URI, PROJECTION, 
						query + " AND " + Friends.ONLINE_STATUS + " IN (" + 
						PSN.STATUS_PENDING + "," + 
						PSN.STATUS_PENDING_SENT + "," + 
						PSN.STATUS_PENDING_RCVD + ")",
						null, Friends.DEFAULT_SORT_ORDER)));
		
		adapter.addSection(getString(R.string.online_count),
				new MyCursorAdapter(getActivity(), adapter, 
						getActivity().managedQuery(Friends.CONTENT_URI, PROJECTION, 
						query + " AND " + Friends.ONLINE_STATUS + 
						" IN (" + 
						PSN.STATUS_ONLINE + "," +
						PSN.STATUS_AWAY + ")",
						null, Friends.DEFAULT_SORT_ORDER)));
		
		adapter.addSection(getString(R.string.offline_count),
				new MyCursorAdapter(getActivity(), adapter, 
						getActivity().managedQuery(Friends.CONTENT_URI, PROJECTION, 
						query + " AND " + Friends.ONLINE_STATUS + 
						" NOT IN (" + 
						PSN.STATUS_PENDING + "," + 
						PSN.STATUS_PENDING_SENT + "," + 
						PSN.STATUS_PENDING_RCVD + "," +
						PSN.STATUS_ONLINE + "," +
						PSN.STATUS_AWAY + ")",
						null, Friends.DEFAULT_SORT_ORDER)));
		
		mAdapter = adapter;//new MyCursorAdapter(getActivity(), null);
		
		mMessage = (TextView)layout.findViewById(R.id.message);
		mMessage.setText(R.string.friends_none);
		
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
			outState.putParcelable("account", mAccount);
			outState.putLong("currentId", mTitleId);
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id)
	{
		PsnFriendListItem item = (PsnFriendListItem)arg1;
		
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
	protected CachePolicy getCachePolicy()
	{
		return mCp;
	}
	
	@Override
    protected Cursor getIconCursor()
    {
		if (getActivity() == null)
			return null;
		
		ContentResolver cr = getActivity().getContentResolver();
		return cr.query(Friends.CONTENT_URI,
				new String[] { Friends._ID, Friends.ICON_URL },
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
					lastUpdateText = getString(R.string.list_not_yet_updated);
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
		
		ContentResolver cr = getActivity().getContentResolver();
        cr.unregisterContentObserver(mObserver);
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		TaskController.getInstance().addListener(mListener);
		
		updateLastUpdateTime();
		
		PsnServiceClient.clearFriendNotifications(getActivity(), mAccount);
		
		ContentResolver cr = getActivity().getContentResolver();
        cr.registerContentObserver(Friends.CONTENT_URI, true, mObserver);
        
		if (System.currentTimeMillis() - mAccount.getLastFriendUpdate() > mAccount.getFriendRefreshInterval())
			synchronizeWithServer();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
	    super.onCreateOptionsMenu(menu, inflater);
	    
    	inflater.inflate(R.menu.psn_friend_list, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
	    switch (item.getItemId()) 
	    {
		case R.id.menu_find_gamer:
			FindGamer.actionShow(getActivity(), this, 1);
			return true;
	    case R.id.menu_refresh:
			TaskController.getInstance().updateFriendList(mAccount, mListener);
	    	return true;
	    case R.id.menu_search_friends:
			return getActivity().onSearchRequested();
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
					data.getStringExtra("onlineId"));
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		
		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo)menuInfo;
		
		ViewHolder vh = (ViewHolder)acmi.targetView.getTag();
		menu.setHeaderTitle(vh.onlineId.getText());
		
		getActivity().getMenuInflater().inflate(R.menu.psn_friend_list_context, 
				menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem menuItem)
	{
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem
				.getMenuInfo();
		PsnFriendListItem friend = (PsnFriendListItem)info.targetView;
		
		switch (menuItem.getItemId())
		{
		case R.id.menu_compare_games:
			CompareGames.actionShow(getActivity(), mAccount, friend.mOnlineId);
			return true;
		}
		
		return super.onContextItemSelected(menuItem);
	}
}
