/*
 * FriendSelector.java 
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

package com.akop.bach.activity;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.ListActivity;
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
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.akop.bach.App;
import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.CachePolicy;
import com.akop.bach.ImageCache.OnImageReadyListener;
import com.akop.bach.R;
import com.akop.bach.SupportsFriends;

public class FriendSelector
		extends ListActivity
		implements OnImageReadyListener, OnItemClickListener
{
	private SupportsFriends mAccount;
	
	private TextView mNoRecords;
	private HashMap<String, SoftReference<Bitmap>> mIconCache;
	
	private static final int COLUMN_ID = 0;
	private static final int COLUMN_GAMERTAG = 1;
	private static final int COLUMN_ICON_URL = 2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.friend_selector);
		
		if ((mAccount = (SupportsFriends)getIntent().getParcelableExtra("account")) == null)
		{
			finish();
			return;
		}
		
		mNoRecords = (TextView)findViewById(R.id.friends_none);
		mIconCache = new HashMap<String, SoftReference<Bitmap>>();
		
		FriendCursorAdapter adapter = new FriendCursorAdapter(this, 
				mAccount.createCursor(this));
		
		ListView lv = (ListView)findViewById(android.R.id.list);
		
		lv.setEmptyView(mNoRecords);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(this);
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
				Cursor cursor = mAccount.createCursor(FriendSelector.this);
				
				if (cursor != null)
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
									ic.requestImage(iconUrl, FriendSelector.this,
											cursor.getInt(COLUMN_ID), iconUrl, false, cp);
				    		}
						}
					}
					catch(Exception e)
					{
						if (App.getConfig().logToConsole())
							e.printStackTrace();
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
	public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id)
	{
		Intent intent = new Intent();
		intent.putExtra("friendId", id);
		
		setResult(RESULT_OK, intent);
		finish();
	}
	
	@Override
	public void onImageReady(long id, Object param, Bitmap bmp)
	{
		String iconUrl = (String)param;
		mIconCache.put(iconUrl, new SoftReference<Bitmap>(bmp));
		getContentResolver().notifyChange(mAccount.getFriendUri(id), null);
	}
	
	public class FriendCursorAdapter extends CursorAdapter
	{
		private class ViewHolder
		{
			public TextView screenName;
			public ImageView avatar;
		}
		
		public FriendCursorAdapter(Context context, Cursor c)
		{
			super(context, c);
		}
		
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			LayoutInflater li = (LayoutInflater)context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = li.inflate(R.layout.friend_selector_item, parent, false);
			
			ViewHolder vh = new ViewHolder();
			vh.screenName = (TextView)view.findViewById(R.id.friend_screen_name);
			vh.avatar = (ImageView)view.findViewById(R.id.friend_avatar);
			
			view.setTag(vh);
			
			return view;
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
            ViewHolder vh = (ViewHolder)view.getTag();
            
            vh.screenName.setText(cursor.getString(COLUMN_GAMERTAG));
            
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
	
	public static void actionSelectFriends(Activity activity, 
			SupportsFriends account, ArrayList<Long> selected,
			boolean allowMultiselect)
	{
		Intent intent = new Intent(activity, FriendSelector.class);
    	intent.putExtra("account", account);
    	intent.putExtra("multiselect", allowMultiselect);
    	
    	if (selected != null)
    		intent.putExtra("selected", selected);
    	
    	activity.startActivityForResult(intent, 1);
	}
	
	public static void actionSelectFriends(Activity activity, 
			SupportsFriends account)
	{
		actionSelectFriends(activity, account, null, false);
	}
}
