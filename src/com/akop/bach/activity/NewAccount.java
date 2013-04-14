/*
 * NewAccount.java
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.akop.bach.App;
import com.akop.bach.PSN;
import com.akop.bach.R;
import com.akop.bach.XboxLive;

public class NewAccount extends ListActivity implements OnItemClickListener
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.account_type_list);
		
		ListView lv = getListView();
		lv.setOnItemClickListener(this);
		
		refresh();
	}
	
	private void refresh()
	{
		List<TypeInfo> typeList = new ArrayList<TypeInfo>();
		
		Cursor c;
		Uri uri;
		TypeInfo info;
		String type;
		
		uri = XboxLive.Profiles.CONTENT_URI;
		c = getContentResolver().query(uri, null, null, null, null);
		type = getString(R.string.xbox_live);
		typeList.add(info = new TypeInfo(type));
		
		if (c != null)
		{
			c.close();
			
			info.isEnabled = true;
			info.intent = new Intent(Intent.ACTION_INSERT, uri);
			info.description = getString(R.string.create_new_account_f, type);
		}
		else
		{
			info.isEnabled = false;
			info.description = getString(R.string.xbox_live_missing);
			info.intent = new Intent(Intent.ACTION_VIEW, 
					Uri.parse("market://details?id=com.akop.bach"));
		}
		
		uri = PSN.Profiles.CONTENT_URI;
		c = getContentResolver().query(uri, null, null, null, null);
		type = getString(R.string.playstation_network);
		typeList.add(info = new TypeInfo(type));
		
		if (c != null)
		{
			c.close();
			
			info.isEnabled = true;
			info.intent = new Intent(Intent.ACTION_INSERT, uri);
			info.description = getString(R.string.create_new_account_f, type);
		}
		else
		{
			info.isEnabled = false;
			info.description = getString(R.string.psn_missing);
			info.intent = new Intent(Intent.ACTION_VIEW, 
					Uri.parse("market://details?id=com.akop.handel"));
		}
		
		TypeInfo[] infos = new TypeInfo[typeList.size()];
		typeList.toArray(infos);
		Arrays.sort(infos, new TypeInfoComparator());
		
		setListAdapter(new TypeInfoAdapter(this, infos));
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		
		refresh();
	}
	
	private class TypeInfoAdapter extends ArrayAdapter<TypeInfo>
	{
		private class ViewHolder
		{
			TextView title;
			TextView description;
		}
		
		public TypeInfoAdapter(Context context, TypeInfo[] accounts)
		{
			super(context, R.layout.account_type_list_item, accounts);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View row;
			ViewHolder vh;
			
			if (convertView == null)
			{
				LayoutInflater inflater = getLayoutInflater();
				
				row = inflater.inflate(R.layout.account_type_list_item, 
						parent, false);
				
		        vh = new ViewHolder();
		        vh.title = (TextView)row.findViewById(R.id.account_type);
		        vh.description = (TextView)row.findViewById(R.id.account_description);
				row.setTag(vh);
			}
			else
			{
				row = convertView;
				vh = (ViewHolder)row.getTag();
			}
			
			TypeInfo info = getItem(position);
			
			vh.title.setText(info.title);
			vh.description.setText(info.description);
			
			return row;
		}
	}
	
	private class TypeInfo
	{
		public String title;
		public String description;
		public Intent intent;
		public boolean isEnabled;
		
		public TypeInfo(String title)
		{
			this.isEnabled = false;
			this.title = title;
			this.intent = null;
			this.description = null;
		}
	}
	
	private class TypeInfoComparator implements Comparator<TypeInfo>
	{
		@Override
		public int compare(TypeInfo object1, TypeInfo object2)
		{
			if (!object1.isEnabled)
				return Integer.MAX_VALUE;
			if (!object2.isEnabled)
				return Integer.MIN_VALUE;
			
			return object1.title.compareToIgnoreCase(object2.title);
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id)
	{
		TypeInfo info = (TypeInfo)getListAdapter().getItem(pos);
		
		if (info.intent != null)
		{
			try
			{
				startActivity(info.intent);
		    	finish();
			}
			catch(ActivityNotFoundException ex)
			{
				if (App.getConfig().logToConsole())
					ex.printStackTrace();
				
				String text = (info.isEnabled)
					? getString(R.string.cannot_open_f)
					: getString(R.string.cannot_open_market);
				
				Toast.makeText(this, text, Toast.LENGTH_LONG).show();
			}
		}
	}
	
	public static void actionShow(Context context)
	{
		Intent intent = new Intent(context, NewAccount.class);
		context.startActivity(intent);
	}
}
