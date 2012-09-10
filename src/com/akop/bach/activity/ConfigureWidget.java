/*
 * ConfigureWidget.java
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import android.app.ListActivity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.akop.bach.BasicAccount;
import com.akop.bach.App;
import com.akop.bach.Preferences;
import com.akop.bach.Preferences.WidgetInfo;
import com.akop.bach.R;

public abstract class ConfigureWidget
		extends ListActivity
		implements OnItemClickListener
{
	protected int mAppWidgetId;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		mAppWidgetId = getIntent().getIntExtra(
				AppWidgetManager.EXTRA_APPWIDGET_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);
		
		ListView lv = getListView();
		lv.setOnItemClickListener(this);
		
		refreshAccounts();
	}
	
	protected abstract Class<?> getAccountClass();
	protected abstract Class<?> getWidgetClass();
	
	private void refreshAccounts()
	{
		BasicAccount[] accounts = Preferences.get(this).getAccounts();
		
		Class<?> accountClass = getAccountClass();
		List<AccountInfo> list = new ArrayList<AccountInfo>();
		
		for (BasicAccount account : accounts)
			if (accountClass.isInstance(account))
				list.add(new AccountInfo(account.getScreenName(),
						account.getUuid()));
		
		AccountInfo[] infos = new AccountInfo[list.size()];
		list.toArray(infos);
		Arrays.sort(infos, new AccountInfoComparator());
		
		ArrayAdapter<AccountInfo> adapter = new ArrayAdapter<AccountInfo>(this,
				android.R.layout.simple_list_item_1, infos);
		getListView().setAdapter(adapter);
	}
	
	private class AccountInfoComparator implements Comparator<AccountInfo>
	{
		@Override
		public int compare(AccountInfo object1, AccountInfo object2)
		{
			return object1.screenName.toLowerCase().compareTo(
					object2.screenName.toLowerCase());
		}
	}
	
	private class AccountInfo
	{
		public String screenName;
		public String uuid;
		
		public AccountInfo(String screenName, String uuid)
		{
			this.screenName = screenName;
			this.uuid = uuid;
		}
		
		@Override
		public String toString()
		{
			return screenName;
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id)
	{
		// Configure the widget
		AccountInfo selected = (AccountInfo)getListView().getItemAtPosition(pos);
		Preferences prefs = Preferences.get(this);
		BasicAccount account = prefs.getAccount(selected.uuid);
		Class<?> widgetClass = getWidgetClass();
		
		if (account != null)
		{
			WidgetInfo info = new WidgetInfo();
			
			info.account = account;
			info.widgetId = mAppWidgetId;
			info.componentName = new ComponentName(widgetClass.getPackage().getName(), 
					getWidgetClass().getName());
			
			prefs.addWidget(info);
			
			if (App.LOGV)
				App.logv("AppWidget %d configured with account %s", mAppWidgetId,
						selected.uuid);
			
			// Set up RemoteViews
			Class<?>[] methodParams = new Class[] { AppWidgetManager.class, 
					Context.class, int.class };
			
			try
			{
				Method initialize = widgetClass.getDeclaredMethod("initialize", 
						methodParams);
				initialize.invoke(null, AppWidgetManager.getInstance(this), 
						this, mAppWidgetId);
			}
			catch (Exception e)
			{
				if (App.LOGV) 
					e.printStackTrace();
				
				Toast.makeText(this, R.string.widget_could_not_be_added,
						Toast.LENGTH_SHORT).show();
				
				finish();
			}
			
			// Set up return intent
			Intent result = new Intent();
			result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
			setResult(RESULT_OK, result);
			finish();
		}
	}
}
