/*
 * SearchFriendsList.java 
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

import java.lang.ref.SoftReference;
import java.util.HashMap;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.akop.bach.App;
import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.CachePolicy;
import com.akop.bach.ImageCache.OnImageReadyListener;
import com.akop.bach.R;
import com.akop.bach.XboxLive.Friends;
import com.akop.bach.uiwidget.XboxLiveFriendListItem;

public class SearchFriendsList
		extends ListActivity
		implements OnImageReadyListener, OnItemClickListener
{
	private HashMap<String, SoftReference<Bitmap>> mIconCache;
	
	private TextView mNoResults;
	
	private String mQuery;
	
	private static final String[] PROJECTION = 
	{ 
		Friends._ID,
		Friends.GAMERTAG,
		Friends.GAMERSCORE,
		Friends.ICON_URL,
	};
	
	private static final int COLUMN_GAMERTAG = 1;
	private static final int COLUMN_GAMERSCORE = 2;
	private static final int COLUMN_ICON_URL = 3;
	
	private Cursor getManagedCursor()
	{
		return managedQuery(Friends.CONTENT_URI, PROJECTION, Friends.GAMERTAG
				+ " LIKE '%'||?||'%'", new String[] { mQuery },
				Friends.DEFAULT_SORT_ORDER);
	}
	
	private void search()
	{
		Cursor cursor = getManagedCursor();
		
		if (cursor == null || cursor.getCount() < 1)
		{
			mNoResults.setText(getString(R.string.no_results, mQuery));
			mNoResults.setVisibility(View.VISIBLE);
		}
		else
		{
			FriendCursorAdapter adapter = new FriendCursorAdapter(this,
					null, cursor);
			setListAdapter(adapter);
			mNoResults.setVisibility(View.GONE);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.xbl_search_friends_list);
		
		mNoResults = (TextView)findViewById(R.id.no_results);
		mIconCache = new HashMap<String, SoftReference<Bitmap>>();
		
	    Intent intent = getIntent();
	    
		if (Intent.ACTION_VIEW.equals(intent.getAction()))
		{
			if (intent.getData() != null)
			{
				FriendSummary.actionShow(this,
						Long.parseLong(intent.getData().getLastPathSegment()));
				finish();
			}
		}
		else if (Intent.ACTION_SEARCH.equals(intent.getAction()))
		{
			mQuery = intent.getStringExtra(SearchManager.QUERY);
			search();
		}
	    
		getListView().setOnItemClickListener(this);
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
				Cursor cursor = getManagedCursor();
				
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
								
								String iconUrl = (String)cursor.getString(COLUMN_ICON_URL);
					    		SoftReference<Bitmap> cachedIcon = mIconCache.get(iconUrl);
					    		
					    		// Is it in the in-memory cache?
					    		if (cachedIcon == null || cachedIcon.get() == null)
					    		{
									Bitmap icon = ic.getCachedBitmap(iconUrl);
									
									// It's not in the in-memory cache; is it
									// in the disk cache?
									if (icon == null)
										ic.requestImage(iconUrl, SearchFriendsList.this,
												cursor.getInt(0), iconUrl, false, cp);
					    		}
							}
						}
						catch(Exception e)
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
	
	@Override
	protected void onPause()
	{
		super.onPause();
		
		ImageCache.getInstance().removeListener(this);
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		
		ImageCache.getInstance().addListener(this);
		loadIconsInBackground();
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
	{
		if (!(arg1 instanceof XboxLiveFriendListItem))
			return;
		
		XboxLiveFriendListItem friend = (XboxLiveFriendListItem)arg1;
        FriendSummary.actionShow(this, friend.mFriendId);
	}
	
	@Override
	public void onImageReady(long id, Object param, Bitmap bmp)
	{
		String iconUrl = (String)param;
		mIconCache.put(iconUrl, new SoftReference<Bitmap>(bmp));
		getContentResolver().notifyChange(
				ContentUris.withAppendedId(Friends.CONTENT_URI, id), null);
	}
	
	public class FriendCursorAdapter extends CursorAdapter
	{
		private class ViewHolder
		{
			public TextView gamertag;
			public TextView gamerscore;
			public ImageView avatar;
		}
		
		public BaseAdapter mParent;
		
		public FriendCursorAdapter(Context context, Cursor c)
		{
			super(context, c);
		}
		
		public FriendCursorAdapter(Context context, BaseAdapter parent, Cursor c)
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
			XboxLiveFriendListItem view = (XboxLiveFriendListItem) li.inflate(
					R.layout.xbl_search_friends_list_item, parent, false);
			
			ViewHolder vh = new ViewHolder();
			vh.gamertag = (TextView)view.findViewById(R.id.friend_gamertag);
			vh.gamerscore = (TextView)view.findViewById(R.id.friend_gp);
			vh.avatar = (ImageView)view.findViewById(R.id.friend_avatar_icon);
			view.setTag(vh);
			
			return view;
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			if (!(view instanceof XboxLiveFriendListItem))
				return;
			
			XboxLiveFriendListItem friend = (XboxLiveFriendListItem)view;
            ViewHolder vh = (ViewHolder)view.getTag();
            
            friend.mFriendId = cursor.getLong(0);
            friend.mGamertag = cursor.getString(COLUMN_GAMERTAG);
            
            vh.gamertag.setText(cursor.getString(COLUMN_GAMERTAG));
			vh.gamerscore.setText(context.getString(R.string.x_f,
					cursor.getInt(COLUMN_GAMERSCORE)));
            
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
		}
	}
}
