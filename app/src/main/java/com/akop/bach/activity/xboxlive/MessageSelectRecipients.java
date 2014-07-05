/*
 * MessageSelectRecipients.java 
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

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.OnImageReadyListener;
import com.akop.bach.R;
import com.akop.bach.SupportsFriends;
import com.akop.bach.XboxLive.Friends;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.parser.XboxLiveParser;

public class MessageSelectRecipients
		extends ListActivity
		implements OnClickListener, OnItemClickListener, OnImageReadyListener
{
	private XboxLiveAccount mAccount;
	private Button mSave;
	private Button mDiscard;
	private Button mToggle;
	
	private FriendItemAdapter mAdapter;
	private ArrayList<FriendItem> mRecipients;
	private HashMap<String, SoftReference<Bitmap>> mIconCache;
	private Handler mHandler = new Handler();
	
	private static class FriendItem implements Serializable
	{
		private static final long serialVersionUID = 4410148406290125944L;
		
		public String gamertag;
		public String iconUrl;
		public boolean isSelected;
	}
	
	private class FriendItemAdapter extends ArrayAdapter<FriendItem>
	{
		//private static final int ITEM_NEW = 0;
		private static final int ITEM_PLAIN = 1;
		
		private List<FriendItem> mItems;
		
		private class ViewHolder
		{
			TextView gamertag;
			ImageView gamerpic;
			ImageView isSelected;
		}
		
		public FriendItemAdapter(Context context, List<FriendItem> items)
		{
			super(context, 0);
			
			mItems = items;
		}
		
		@Override
		public int getViewTypeCount()
		{
			return 2;
		}
		
		@Override
		public long getItemId(int position)
		{
			return position;
		}
		
		@Override
		public int getCount()
		{
			return mItems.size();
		}
		
		@Override
		public FriendItem getItem(int position)
		{
			return mItems.get(position);
		}
		
		@Override
		public int getItemViewType(int position)
		{
			return ITEM_PLAIN;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View row;
			ViewHolder vh;
			
			if (convertView == null)
			{
				LayoutInflater inflater = getLayoutInflater();
				
				row = inflater.inflate(R.layout.xbl_selectable_friend_item, parent, false);
				
		        vh = new ViewHolder();
		        vh.gamertag = (TextView)row.findViewById(R.id.friend_gamertag);
				vh.gamerpic = (ImageView)row.findViewById(R.id.friend_gamerpic);
		        vh.isSelected = (ImageView)row.findViewById(R.id.is_selected);
		        
				row.setTag(vh);
			}
			else
			{
				row = convertView;
				vh = (ViewHolder)row.getTag();
			}
			
			FriendItem item = getItem(position);
			SoftReference<Bitmap> bmpRef = mIconCache.get(item.iconUrl);
			Bitmap bmp = null;
			
			if (bmpRef != null && (bmp = bmpRef.get()) != null)
			{
				vh.gamerpic.setImageBitmap(bmp);
			}
			else
			{
				// Image has likely been garbage-collected
				// Load it into the cache again
				
				bmp = ImageCache.getInstance().getCachedBitmap(item.iconUrl);
				if (bmp != null)
				{
					mIconCache.put(item.iconUrl, new SoftReference<Bitmap>(bmp));
					vh.gamerpic.setImageBitmap(bmp);
				}
				else
				{
					// Image failed to load - just use placeholder
					vh.gamerpic.setImageResource(R.drawable.avatar_default);
				}
			}
			
			vh.gamertag.setText(item.gamertag);
			vh.isSelected.setImageResource((item.isSelected)
					? R.drawable.checked_overlay_on
					: R.drawable.checked_overlay_off);
			
			return row;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.xbl_friend_selector);
		
        mAccount = (XboxLiveAccount)getIntent().getParcelableExtra("account");
        mSave = (Button)findViewById(R.id.dialog_save);
        mDiscard = (Button)findViewById(R.id.dialog_discard);
        mToggle = (Button)findViewById(R.id.dialog_toggle);
        
        mToggle.setVisibility(View.INVISIBLE);
        
		mSave.setOnClickListener(this);
		mDiscard.setOnClickListener(this);
		mToggle.setOnClickListener(this);
		
		mRecipients = new ArrayList<FriendItem>();
		mIconCache = new HashMap<String, SoftReference<Bitmap>>();
		mAdapter = new FriendItemAdapter(this, mRecipients);
		
		ListView lv = getListView();
		lv.setOnItemClickListener(this);
		lv.setAdapter(mAdapter);
		
		if (savedInstanceState != null
				&& savedInstanceState.containsKey("recipients"))
		{
			mRecipients.addAll((ArrayList<FriendItem>)savedInstanceState
					.getSerializable("recipients"));
		}
		else
		{
			ArrayList<String> selected = (ArrayList<String>)getIntent()
					.getSerializableExtra("selected");
			
			refreshRecipients(selected);
		}
		
		mAdapter.notifyDataSetChanged();
		
		mSave.setEnabled(mRecipients.size() > 0);
		mToggle.setEnabled(mRecipients.size() > 0);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		outState.putSerializable("recipients", mRecipients);
	}
	
	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.dialog_save:
			Intent intent = new Intent();
			intent.putExtra("selected", getSelection());
			setResult(RESULT_OK, intent);
			finish();
			break;
		case R.id.dialog_toggle:
			toggleAll();
			break;
		case R.id.dialog_discard:
			setResult(RESULT_CANCELED);
			finish();
			break;
		}
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
	public void onImageReady(long id, Object param, Bitmap bmp)
	{
		String iconUrl = (String)param;
		mIconCache.put(iconUrl, new SoftReference<Bitmap>(bmp));
		
		mHandler.post(new Runnable()
		{
			@Override
			public void run() { mAdapter.notifyDataSetChanged(); }
		});
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3)
	{
		FriendItem item = mRecipients.get(pos);
		item.isSelected = !item.isSelected;
		
		mAdapter.notifyDataSetChanged();
	}
	
	private void toggleAll()
	{
		if (mRecipients.size() < 1)
			return;
		
		boolean newState = !mRecipients.get(0).isSelected;
		for (FriendItem item : mRecipients)
			item.isSelected = newState;
		
		mAdapter.notifyDataSetChanged();
	}
	
	private ArrayList<String> getSelection()
	{
		ArrayList<String> selected = new ArrayList<String>();
		for (FriendItem item : mRecipients)
			if (item.isSelected)
				selected.add(item.gamertag);
		
		return selected;
	}
	
	private void loadIconsInBackground()
	{
		Thread t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				final ImageCache ic = ImageCache.getInstance();
				
				for (FriendItem item : mRecipients)
				{
					if (isFinishing())
						break;
					
					SoftReference<Bitmap> cachedIcon = mIconCache.get(item.iconUrl);
					
					// Is it in the in-memory cache?
					if (cachedIcon == null || cachedIcon.get() == null)
					{
						Bitmap icon = ic.getCachedBitmap(item.iconUrl);
						
						// It's not in the in-memory cache; is it
						// in the disk cache?
						
						if (icon == null)
						{
							ic.requestImage(item.iconUrl,
									MessageSelectRecipients.this, 0,
									item.iconUrl);
						}
					}
				}
			}
		});
		
		t.run();
	}
	
	private void refreshRecipients(ArrayList<String> suppliedRecipients)
	{
		List<String> recipientCopy = new ArrayList<String>(suppliedRecipients);
		Collections.sort(recipientCopy);
		
		mRecipients.clear();
		
		Cursor cursor = getContentResolver().query(Friends.CONTENT_URI, 
				new String[] 
				{
					Friends.GAMERTAG,
					Friends.ICON_URL,
				},
				Friends.ACCOUNT_ID + "=" + mAccount.getId(), null,
				Friends.GAMERTAG + " COLLATE NOCASE");
		
		if (cursor != null)
		{
			try
			{
				while (cursor.moveToNext())
				{
					FriendItem item = new FriendItem();
					
					item.gamertag = cursor.getString(0);
					item.iconUrl = cursor.getString(1);
					
					int index;
					if ((index = recipientCopy.indexOf(item.gamertag)) > -1)
					{
						item.isSelected = true;
						recipientCopy.remove(index);
					}
					
					mRecipients.add(item);
				}
			}
			finally
			{
				cursor.close();
			}
		}
		
		for (String r : recipientCopy)
		{
			if (r != null)
			{
				FriendItem item = new FriendItem();
				
				item.gamertag = r;
				item.iconUrl = XboxLiveParser.getGamerpicUrl(r);
				item.isSelected = true;
				
				mRecipients.add(0, item);
			}
		}
	}
	
	public static void actionSelectFriends(Activity activity, 
			SupportsFriends account,
			ArrayList<String> selected)
	{
		Intent intent = new Intent(activity, MessageSelectRecipients.class);
    	intent.putExtra("account", account);
    	intent.putExtra("selected", selected);
    	
    	activity.startActivityForResult(intent, 1);
	}
}