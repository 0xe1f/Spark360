/*
 * ScrollingActivity.java
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

package com.akop.bach.activity;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;

import com.akop.bach.App;
import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.CachePolicy;
import com.akop.bach.ImageCache.OnImageReadyListener;

public abstract class ScrollingActivity
		extends ListActivity
		implements ViewBinder, OnImageReadyListener
{
	protected static final int PRELOAD_LIST_SIZE = 13;
	
	protected class ListUpdater extends Handler
	{
		private static final int MSG_UPDATE = 1;
		private static final int MSG_NOTIFY_CHANGE = 2;
		private static final int MSG_REINITIALIZE = 3;
		private static final int MSG_SET_THROBBER = 4;
		
		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg)
		{
			switch(msg.what)
			{
			case MSG_UPDATE:
				synchronized (mAdapterList)
				{
					List<Map<String, Object>> list = (List<Map<String, Object>>)msg.obj;
					int start = msg.arg1, n = msg.arg2;
					
					if (start < 1)
						mAdapterList.clear();
					
					for (int i = start; i < n; i++)
					{
						Map<String, Object> item = list.get(i);
						mAdapterList.add(item);
					}
					
					mAdapter.notifyDataSetChanged();
				}
				break;
			case MSG_NOTIFY_CHANGE:
				mAdapter.notifyDataSetChanged();
				break;
			case MSG_REINITIALIZE:
				onDataUpdate();
				break;
			case MSG_SET_THROBBER:
				toggleProgressBar(msg.arg1 != 0);
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
		
		public void reinitialize()
		{
			this.reinitialize(null);
		}
		
		public void reinitialize(Object obj)
		{
			Message msg = Message.obtain(this, MSG_REINITIALIZE, obj); 
			sendMessage(msg);
		}
		
		public void notifyChange()
		{
			Message msg = Message.obtain(this, MSG_NOTIFY_CHANGE, 0, 0); 
			sendMessage(msg);
		}
		
		public void update(List<Map<String, Object>> list, int start, int end)
		{
			Message msg = Message.obtain(this, MSG_UPDATE, start, end); 
			msg.obj = list;
			sendMessage(msg);
		}
		
		public void showThrobber(boolean on)
		{
			Message msg = Message.obtain(this, MSG_SET_THROBBER, on ? 1 : 0, 0); 
			sendMessage(msg);
		}
	}
	
	protected abstract int getListItemLayoutResource();
	protected abstract String[] getAdapterKeys();
	protected abstract int[] getAdapterResourceIds();
	protected abstract String getIconUrlKey();
	
	protected boolean mListLoaded = false;
	
	protected BaseAdapter mAdapter;
	protected List<Map<String, Object>> mAdapterList;
	protected Map<String, SoftReference<Bitmap>> mIconCache;
	protected ListUpdater mUpdater = new ListUpdater();
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		initializeWindowFeatures();
		
		mListLoaded = false;
		mAdapterList = new ArrayList<Map<String, Object>>();
		mIconCache = new HashMap<String, SoftReference<Bitmap>>();
		
		mAdapter = new SimpleAdapter(this, mAdapterList,
				getListItemLayoutResource(), getAdapterKeys(),
				getAdapterResourceIds());
        ((SimpleAdapter)mAdapter).setViewBinder(this);
        
        /*
		mAdapter = new SectionedAdapter(this, R.layout.list_header);
		SimpleAdapter adapter = new SimpleAdapter(this, 
				mAdapterList, 
				getListItemLayoutResource(),  
				getAdapterKeys(),
				getAdapterResourceIds());
		adapter.setViewBinder(this);
		mAdapter.addSection("Security", adapter);
		*/
        
		setListAdapter(mAdapter);
		
		if (savedInstanceState != null && savedInstanceState.containsKey("__list"))
		{
			if (App.LOGV)
				App.logv("ScrollingActivity: Loading from state");
			
			// Load list from state
			List<Map<String, Object>> list = (List<Map<String, Object>>)savedInstanceState
					.getSerializable("__list");
			
			initializeList(list, 0, list.size());
		}
		else
		{
			if (App.LOGV)
				App.logv("ScrollingActivity: Initializing");
			
			rebind();
		}
	}
	
	protected void initializeWindowFeatures()
	{
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	}
	
	protected void toggleProgressBar(boolean visible)
	{
		setProgressBarIndeterminateVisibility(visible);
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		
		ImageCache ic = ImageCache.get();
        ic.removeListener(this);
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		
		ImageCache ic = ImageCache.get();
        ic.addListener(this);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		if (mListLoaded)
		{
			outState.putSerializable("__list",
					(ArrayList<Map<String, Object>>)mAdapterList);
			
			if (App.LOGV)
				App.logv("ScrollingActivity: Saving state");
		}
	}
	
	@Override
	public void onImageReady(long id, Object param, Bitmap bmp)
	{
		String iconUrl = (String)param;
		mIconCache.put(iconUrl, new SoftReference<Bitmap>(bmp));
		mUpdater.notifyChange();
	}
	
	protected void onDataUpdate()
	{
		rebind();
	}
	
	protected Cursor getListCursor()
	{
		return null;
	}
	
	protected Map<String, Object> getListItem(Cursor cursor)
	{
		return null;
	}
	
	protected void initializeList(List<Map<String, Object>> list, 
			int start, int end)
	{
		mUpdater.update(list, start, list.size());
		mListLoaded = true;
		
		refreshIcons(list);
	}
	
	protected void rebind()
	{
		mListLoaded = false;
		
		final Cursor cursor = getListCursor();
		final List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		
		if (cursor != null)
		{
			int i = 0;
			for (; i < PRELOAD_LIST_SIZE && cursor.moveToNext(); i++)
				list.add(getListItem(cursor));
			
			mUpdater.update(list, 0, list.size());
			final int start = i;
			
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						while (cursor.moveToNext())
							list.add(getListItem(cursor));
					}
					finally
					{
						cursor.close();
					}
					
					initializeList(list, start, list.size());
				}
			}).start();
		}
	}
	
	protected int getImageWidth()
	{
		return -1;
	}
	
	protected int getImageHeight()
	{
		return -1;
	}
	
	protected void refreshIcons(final List<Map<String, Object>> list)
	{
		// Convert to DIPs
		
		final int newImageWidth = (int)Math.ceil(getImageWidth()
				* getResources().getDisplayMetrics().density);
		final int newImageHeight = (int)Math.ceil(getImageHeight() 
				* getResources().getDisplayMetrics().density);
		final CachePolicy cp = new CachePolicy(newImageWidth, newImageHeight);
		
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				final ImageCache ic = ImageCache.get();
				final String iconUrlKey = getIconUrlKey(); 
				
				for (Map<String, Object> listItem : list)
				{
					String imageUrl = (String)listItem.get(iconUrlKey);
					Bitmap icon = ic.getCachedBitmap(imageUrl);
					
					if (icon != null)
						mIconCache.put(imageUrl, new SoftReference<Bitmap>(icon));
					else
						ic.requestImage(imageUrl, ScrollingActivity.this, 0,
								listItem.get(iconUrlKey), false, cp);
				}
				
				mUpdater.notifyChange();
			}
		}).start();
	}
	
	protected void setImage(ImageView imageView, 
			String imageUrl,
			int defaultImage)
	{
		if (imageUrl == null)
		{
			imageView.setImageResource(defaultImage);
			return;
		}
		
		SoftReference<Bitmap> icon = mIconCache.get(imageUrl);
		
		if (icon != null && icon.get() != null)
		{
			// Image is in the in-memory cache
			imageView.setImageBitmap(icon.get());
		}
		else
		{
			// Image has likely been garbage-collected
			// Load it into the cache again
			Bitmap bmp = ImageCache.get().getCachedBitmap(imageUrl);
			if (bmp != null)
			{
				mIconCache.put(imageUrl, new SoftReference<Bitmap>(bmp));
				imageView.setImageBitmap(bmp);
			}
			else
			{
				// Image failed to load - just use placeholder
				imageView.setImageResource(defaultImage);
			}
		}
	}
}
