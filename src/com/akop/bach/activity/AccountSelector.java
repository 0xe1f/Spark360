/*
 * AccountSelector.java
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

package com.akop.bach.activity;

import java.util.Arrays;
import java.util.Comparator;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.akop.bach.BasicAccount;
import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.OnImageReadyListener;
import com.akop.bach.Preferences;
import com.akop.bach.R;

public class AccountSelector
		extends ListActivity
		implements OnItemClickListener, OnImageReadyListener
{
	private class TaskHandler extends Handler
	{
		private static final int MSG_NOTIFY_CHANGED = 1;
		
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case MSG_NOTIFY_CHANGED:
				((AccountInfoAdapter)getListAdapter()).notifyDataSetChanged();
				break;
			}
		}
		
		public void notifyDataSetChanged()
		{
			Message msg = Message.obtain(this, MSG_NOTIFY_CHANGED, 0, 0); 
			sendMessage(msg);
		}
	}
	
	private TaskHandler mHandler = new TaskHandler();
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.account_list);
		
		ListView lv = getListView();
		lv.setEmptyView(findViewById(R.id.accounts_none));
		lv.setOnItemClickListener(this);
	}
	
	private void refreshAccounts()
	{
		BasicAccount[] accounts = Preferences.get(this).getAccounts();
		AccountInfo[] infos = new AccountInfo[accounts.length];
		
		for (int i = 0; i < accounts.length; i++)
		{
			BasicAccount account = accounts[i];
			infos[i] = new AccountInfo(account.getScreenName(),
					account.getDescription(),
					account.getIconUrl(), 
					account.getUuid());
		}
		
		Arrays.sort(infos, new AccountInfoComparator());
		setListAdapter(new AccountInfoAdapter(this, infos));
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		
		ImageCache.getInstance().addListener(this);
		
		refreshAccounts();
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		
		ImageCache.getInstance().removeListener(this);
	}
	
	public static void actionSelectAccount(Activity activity, int requestCode)
	{
		Intent intent = new Intent(activity, AccountSelector.class);
		activity.startActivityForResult(intent, requestCode);
	}
	
	public static void actionSelectAccount(Activity activity)
	{
		actionSelectAccount(activity, 0);
	}
	
	private class AccountInfoComparator implements Comparator<AccountInfo>
	{
		@Override
		public int compare(AccountInfo object1, AccountInfo object2)
		{
			if (object1.uuid == null)
				return -1;
			else if (object2.uuid == null)
				return 1;
			return object1.screenName.toLowerCase().compareTo(
					object2.screenName.toLowerCase());
		}
	}
	
	private class AccountInfo
	{
		public String screenName;
		public String description;
		public String iconUrl;
		public Bitmap icon;
		public String uuid;
		
		public AccountInfo(String screenName,
				String description,
				String iconUrl,
				String uuid)
		{
			this.screenName = screenName;
			this.description = description;
			this.iconUrl = iconUrl;
			this.uuid = uuid;
			this.icon = (iconUrl == null) ? null : ImageCache.getInstance().getCachedBitmap(iconUrl);
		}
	}
	
	private class AccountInfoAdapter extends ArrayAdapter<AccountInfo>
	{
		private static final int ITEM_NEW = 0;
		private static final int ITEM_NORMAL = 1;
		
		private class ViewHolder
		{
			TextView gamertag;
			TextView description;
			ImageView avatarIcon;
		}
		
		public AccountInfoAdapter(Context context, AccountInfo[] accounts)
		{
			super(context, R.layout.account_list_item, accounts);
		}
		
		@Override
		public int getViewTypeCount()
		{
			return 2;
		}
		
		@Override
		public int getItemViewType(int position)
		{
			return ITEM_NORMAL;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View row;
			ViewHolder vh;
			
			if (convertView == null)
			{
				LayoutInflater inflater = getLayoutInflater();
				
				if (getItemViewType(position) == ITEM_NEW)
				{
					return inflater.inflate(R.layout.account_list_item_new, parent, false);
				}
				else
				{
					row = inflater.inflate(R.layout.account_list_item, parent, false);
			        vh = new ViewHolder();
			        vh.gamertag = (TextView)row.findViewById(R.id.accounts_gamertag);
			        vh.description = (TextView)row.findViewById(R.id.accounts_description);
					vh.avatarIcon = (ImageView)row.findViewById(R.id.accounts_avatar_icon);
					row.setTag(vh);
				}
			}
			else
			{
				if (getItemViewType(position) == ITEM_NEW)
					return convertView;
				
				row = convertView;
				vh = (ViewHolder)row.getTag();
			}
			
			AccountInfo info = getItem(position);
			
			vh.gamertag.setText(info.screenName);
			vh.description.setText(info.description);
			
			if (info.icon != null)
			{
				vh.avatarIcon.setImageBitmap(info.icon);
			}
			else
			{
				vh.avatarIcon.setImageResource(R.drawable.avatar_default);
				if (info.iconUrl != null)
					ImageCache.getInstance().requestImage(info.iconUrl,
							AccountSelector.this, 0, info);
			}
			
			return row;
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id)
	{
		AccountInfo accountInfo = (AccountInfo)getListAdapter().getItem(pos);
		Intent intent = new Intent();
		
		if (accountInfo.uuid == null)
		{
			intent.putExtra("newAccount", true);
		}
		else
		{
			BasicAccount account = Preferences.get(this).getAccount(accountInfo.uuid);
			
			intent.putExtra("account", account);
		}
		
		setResult(RESULT_OK, intent);
		finish();
	}
	
	@Override
	public void onImageReady(long id, Object param, Bitmap bmp)
	{
		AccountInfo info = (AccountInfo)param;
		info.icon = bmp;
		
		mHandler.notifyDataSetChanged();
	}
}
