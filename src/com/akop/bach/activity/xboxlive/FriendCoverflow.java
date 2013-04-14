/*
 * FriendCoverflow.java 
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

import java.lang.ref.SoftReference;
import java.util.HashMap;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.akop.bach.Account;
import com.akop.bach.App;
import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.CachePolicy;
import com.akop.bach.ImageCache.OnImageReadyListener;
import com.akop.bach.Preferences;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.XboxLive.Friends;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.fragment.xboxlive.FriendsFragment.RequestInformation;
import com.akop.bach.parser.Parser;
import com.akop.bach.uiwidget.CoverFlow;

public class FriendCoverflow extends RibbonedActivity implements
		OnItemClickListener, OnImageReadyListener
{
	private HashMap<String, SoftReference<Bitmap>> mIconCache;
	
	private TextView mFriendsLastUpdated;
	
	private FriendCursorAdapter mAdapter = null;
	
	private static final int COLUMN_GAMERTAG = 1;
	private static final int COLUMN_GAMERSCORE = 2;
	private static final int COLUMN_ACTIVITY = 3;
	private static final int COLUMN_STATUS = 4;
	private static final int COLUMN_ICON_URL = 5;
	
	private static final String[] PROJECTION = 
	{ 
		Friends._ID, 
		Friends.GAMERTAG,
		Friends.GAMERSCORE, 
		Friends.CURRENT_ACTIVITY, 
		Friends.STATUS,
		Friends.ICON_URL, 
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		if (reoriented())
		{
			finish();
			return;
		}
		
		setContentView(R.layout.xbl_friend_coverflow);
		
		mIconCache = new HashMap<String, SoftReference<Bitmap>>();
		mFriendsLastUpdated = (TextView)findViewById(R.id.friends_last_updated);
		
		CoverFlow coverFlow = (CoverFlow)findViewById(R.id.coverflow);
		coverFlow.setSpacing(0);
		coverFlow.setMaxZoom(-240);
		coverFlow.setSelection(4, true);
		coverFlow.setAnimationDuration(1000);
		coverFlow.setOnItemClickListener(this);
		
		mAdapter = new FriendCursorAdapter(this, 
				managedQuery(Friends.CONTENT_URI, PROJECTION,
				Friends.ACCOUNT_ID + "=" + mAccount.getId(), null,
				Friends.DEFAULT_SORT_ORDER));
		
		mFriendHandler.updateSyncTime();
		
		coverFlow.setAdapter(mAdapter);
	}
	
	private void loadIconsInBackground()
	{
		final CachePolicy cp = new CachePolicy();
		final ImageCache ic = ImageCache.getInstance();
		
		Thread t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				Cursor cursor = getContentResolver().query(Friends.CONTENT_URI,
						new String[] { Friends._ID, Friends.ICON_URL },
						Friends.ACCOUNT_ID + "=" + mAccount.getId(), null,
						Friends.DEFAULT_SORT_ORDER);
				
				if (cursor != null)
				{
					try
					{
						try
						{
							while (cursor.moveToNext())
							{
								if (isFinishing())
									break;
								
								String iconUrl = (String) cursor.getString(1);
								SoftReference<Bitmap> cachedIcon = mIconCache
										.get(iconUrl);
								
								// Is it in the in-memory cache?
								if (cachedIcon == null
										|| cachedIcon.get() == null)
								{
									Bitmap icon = ic.getCachedBitmap(iconUrl);
									
									// It's not in the in-memory cache; is it
									// in the disk cache?
									if (icon == null)
										ic.requestImage(iconUrl,
												FriendCoverflow.this,
												cursor.getInt(0), iconUrl,
												false, cp);
								}
							}
						}
						catch (Exception e)
						{
							if (App.getConfig().logToConsole())
								e.printStackTrace();
						}
					}
					finally
					{
						cursor.close();
					}
				}
			}
		});
		
		t.start();
	}
	
	private class FriendHandler extends Handler
	{
		private static final int MSG_UPDATE_SYNC_TIME = 1;
		
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case MSG_UPDATE_SYNC_TIME:
			{
				String syncText;
				
				mAccount.refresh(Preferences.get(FriendCoverflow.this));
				if (mAccount.getLastFriendUpdate() < 1)
					syncText = getString(R.string.not_yet_updated);
				else
				{
					syncText = getString(R.string.last_updated_f,
							DateUtils.getRelativeDateTimeString(
									FriendCoverflow.this,
									mAccount.getLastFriendUpdate(),
									DateUtils.MINUTE_IN_MILLIS,
									DateUtils.WEEK_IN_MILLIS, 0));
				}
				
				mFriendsLastUpdated.setText(syncText);
			}
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
		
		public void updateSyncTime()
		{
			Message msg = Message.obtain(this, MSG_UPDATE_SYNC_TIME, 0, 0);
			sendMessage(msg);
		}
	};
	
	private FriendHandler mFriendHandler = new FriendHandler();
	
	private TaskListener mListener = new TaskListener("XBoxFriends")
	{
		@Override
		public void onAllTasksCompleted()
		{
			mHandler.showThrobber(false);
		}
		
		@Override
		public void onTaskFailed(Account account, Exception e)
		{
			mHandler.showError(e);
		}
		
		@Override
		public void onTaskSucceeded(Account account, Object requestParam,
				Object result)
		{
			mHandler.setLoadText(getString(R.string.no_friends));
			mFriendHandler.updateSyncTime();
		}
		
		@Override
		public void onTaskStarted()
		{
			mHandler.showThrobber(true);
			mHandler.setLoadText(getString(R.string.updating_friends));
		}
	};
	
	private TaskListener mRequestListener = new TaskListener(
		"XBoxCancelRequest")
	{
		@Override
		public void onAllTasksCompleted()
		{
			mHandler.showThrobber(false);
		}
		
		@Override
		public void onTaskFailed(Account account, Exception e)
		{
			mHandler.showToast(Parser.getErrorMessage(FriendCoverflow.this, e));
		}
		
		@Override
		public void onTaskSucceeded(Account account, Object requestParam,
				Object result)
		{
			// Update friends
			TaskController.getInstance().updateFriendList(mAccount, mListener);
			
			// Show toast
			RequestInformation ri = (RequestInformation)requestParam;
			mHandler.showToast(FriendCoverflow.this.getString(ri.resId,
			        ri.gamertag));
		}
		
		@Override
		public void onTaskStarted()
		{
			mHandler.showThrobber(true);
		}
	};
	
	private static class ViewHolder
	{
		TextView gamertag;
		ImageView gamerpic;
		TextView activity;
		TextView gamerscore;
		TextView status;
	}
	
	public class FriendCursorAdapter extends CursorAdapter
	{
		public FriendCursorAdapter(Context context, Cursor c)
		{
			super(context, c);
		}
		
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			LayoutInflater li = (LayoutInflater)context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			View view = li.inflate(R.layout.xbl_friend_cover, parent, false);
			ViewHolder vh = new ViewHolder();
			
			vh.gamertag = (TextView)view.findViewById(R.id.friend_gamertag);
			vh.gamerpic = (ImageView)view.findViewById(R.id.friend_gamerpic);
			vh.activity = (TextView)view.findViewById(R.id.friend_activity);
			vh.gamerscore = (TextView)view.findViewById(R.id.friend_gamerscore);
			vh.status = (TextView)view.findViewById(R.id.friend_status);
			
			view.setTag(vh);
			
			return view;
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			ViewHolder vh = (ViewHolder)view.getTag();
			
			vh.gamertag.setText(cursor.getString(COLUMN_GAMERTAG));
			vh.activity.setText(cursor.getString(COLUMN_ACTIVITY));
			vh.status.setText(cursor.getString(COLUMN_STATUS));
			vh.gamerscore.setText(context.getString(R.string.x_f, 
					cursor.getInt(COLUMN_GAMERSCORE)));
			
			String iconUrl = cursor.getString(COLUMN_ICON_URL);
			SoftReference<Bitmap> iconRef = mIconCache.get(iconUrl);
			Bitmap bmp = null;
			
			if (iconRef != null && iconRef.get() != null)
			{
				// Image is in the in-memory cache
				bmp = iconRef.get();
			}
			else
			{
				// Image has likely been garbage-collected
				// Load it into the cache again
				
				if ((bmp = ImageCache.getInstance().getCachedBitmap(iconUrl)) != null)
					mIconCache.put(iconUrl, new SoftReference<Bitmap>(bmp));
			}
			
			if (bmp != null)
				vh.gamerpic.setImageBitmap(bmp);
			else
				vh.gamerpic.setImageResource(R.drawable.avatar_default);
		}
	}
	
	public boolean reoriented()
	{
		Configuration config = getResources().getConfiguration();
		int orientation = config.orientation;
		int coverflowMode = mAccount.getCoverflowMode();
		
		if (coverflowMode == XboxLiveAccount.COVERFLOW_IN_LANDSCAPE 
				&& orientation == Configuration.ORIENTATION_PORTRAIT)
		{
			if (App.getConfig().logToConsole())
				App.logv("Reorienting ...");
			
			FriendList.actionShow(this, mAccount);
			return true;
		}
		else if (coverflowMode == XboxLiveAccount.COVERFLOW_OFF)
		{
			if (App.getConfig().logToConsole())
				App.logv("Reorienting ...");
			
			FriendList.actionShow(this, mAccount);
			return true;
		}
		
		return false;
	}
	
	public static void actionShowFriends(Context context,
			XboxLiveAccount account)
	{
		Intent intent = new Intent(context, FriendCoverflow.class);
		intent.putExtra("account", account);
		context.startActivity(intent);
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
	{
		FriendSummary.actionShow(this, mAccount, arg3);
	}

	@Override
	protected void updateRibbon()
	{
		if (mAccount != null)
			updateRibbon(mAccount.getGamertag(), mAccount.getIconUrl(),
					getString(R.string.my_friends));
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		
		ImageCache.getInstance().removeListener(this);
		
		TaskController.getInstance().removeListener(mListener);
		TaskController.getInstance().removeListener(mRequestListener);
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		
		ImageCache.getInstance().addListener(this);
		
		TaskController.getInstance().addListener(mListener);
		TaskController.getInstance().addListener(mRequestListener);
		
		loadIconsInBackground();
		
		if (System.currentTimeMillis() - mAccount.getLastFriendUpdate() > mAccount
				.getFriendRefreshInterval())
		{
			TaskController.getInstance().updateFriendList(mAccount, mListener);
		}
		
		if (mAdapter != null)
			mAdapter.notifyDataSetChanged();
		
		mFriendHandler.updateSyncTime();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.xbl_friend_coverflow, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.menu_search_friends:
			return onSearchRequested();
		case R.id.menu_recent_players:
			RecentPlayerList.actionShow(this, mAccount);
			return true;
		case R.id.menu_find_gamer:
			FindGamer.actionShow(this, R.id.menu_find_gamer);
			return true;
		case R.id.menu_refresh:
			TaskController.getInstance().updateFriendList(mAccount, mListener);
			return true;
		}
		return false;
	}
	
	@Override
	public void onImageReady(long id, Object param, Bitmap bmp)
	{
		String iconUrl = (String) param;
		mIconCache.put(iconUrl, new SoftReference<Bitmap>(bmp));
		getContentResolver().notifyChange(
				ContentUris.withAppendedId(Friends.CONTENT_URI, id), null);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode != RESULT_OK)
			return;

		if (requestCode == R.id.menu_find_gamer)
			GamerProfile.actionShow(this, mAccount,
					data.getStringExtra("gamertag"));
	}
}
