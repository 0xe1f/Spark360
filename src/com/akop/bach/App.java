/*
 * App.java
 * Copyright (C) 2010-2013 Akop Karapetyan
 *
 * This file is part of Caffeine, the online gaming service client support 
 * library. 
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

package com.akop.bach;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.akop.bach.activity.About;
import com.akop.bach.configurations.AppConfig;
import com.akop.bach.configurations.DevConfig;
import com.akop.bach.provider.PsnProvider;
import com.akop.bach.provider.XboxLiveProvider;
import com.akop.bach.service.NotificationService;

public class App extends Application
{
	private static final Class<? extends AppConfig> ConfigType =
			DevConfig.class;
	
	private static final String LOG_TAG = "bach";
	
	private static App sInstance = null;
	private AppConfig mConfig;
	
	public void showAboutDialog(Context context)
	{
		About.actionShowAbout(context);
	}
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		
		try 
		{
			mConfig = ConfigType.newInstance();
		}
		catch (Exception e) 
		{
			throw new RuntimeException("Configuration not valid");
		}
		
		sInstance = this;
		
		NotificationService.actionReschedule(this);
	}
	
	public static AppConfig getConfig()
	{
		return sInstance.mConfig;
	}
	
	public static App getInstance()
	{
		return sInstance;
	}
	
	public static void logv(String message)
	{
		if (getConfig().logToConsole())
			Log.v(LOG_TAG, message);
	}
	
	public static void logv(String format, Object... args)
	{
		if (getConfig().logToConsole())
			Log.v(LOG_TAG, String.format(format, args));
	}
	
	public static BasicAccount createAccountFromAuthority(Context context, String authority)
	{
		if (XboxLiveProvider.AUTHORITY.equals(authority))
			return new XboxLiveAccount(context);
		else if (PsnProvider.AUTHORITY.equals(authority))
			return new PsnAccount(context);
		else return null;
	}
}
