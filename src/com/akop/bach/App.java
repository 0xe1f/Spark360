/*
 * App.java
 * Copyright (C) 2010-2012 Akop Karapetyan
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

import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.akop.bach.activity.About;
import com.akop.bach.provider.PsnProvider;
import com.akop.bach.provider.XboxLiveProvider;
import com.akop.bach.service.NotificationService;

@ReportsCrashes(
		formKey = "dHFlZVgtcEtRUC1FVnI0TXVpZUQxMnc6MQ",
		mode = ReportingInteractionMode.TOAST,
		resToastText = R.string.crash_reported,
		customReportContent = 
		{
				ReportField.REPORT_ID,
				ReportField.APP_VERSION_CODE, 
				ReportField.APP_VERSION_NAME, 
				ReportField.PACKAGE_NAME, 
				ReportField.PHONE_MODEL, 
				ReportField.BRAND, 
				ReportField.ANDROID_VERSION, 
				ReportField.BUILD, 
				ReportField.TOTAL_MEM_SIZE, 
				ReportField.AVAILABLE_MEM_SIZE, 
				ReportField.CUSTOM_DATA, 
				ReportField.STACK_TRACE, 
				ReportField.INITIAL_CONFIGURATION, 
				ReportField.CRASH_CONFIGURATION, 
				ReportField.DISPLAY, 
				ReportField.USER_COMMENT, 
				ReportField.USER_APP_START_DATE, 
				ReportField.USER_CRASH_DATE, 
				ReportField.DEVICE_FEATURES, 
				ReportField.SETTINGS_SYSTEM, 
		}
)
public class App extends Application
{
	public static final boolean LOGV = false;
	public static final boolean ENABLE_ACRA = false;
	
	private static final String LOG_TAG = "bach";
	
	private static App sInstance = null;
	
	public void showAboutDialog(Context context)
	{
		About.actionShowAbout(context);
	}
	
	@Override
	public void onCreate()
	{
        ACRA.init(this);
        
		super.onCreate();
		
		sInstance = this;
		
		ErrorReporter.getInstance().disable();
		
		NotificationService.actionReschedule(this);
	}
	
	public static App getInstance()
	{
		return sInstance;
	}
	
	public static void logv(String message)
	{
		Log.v(LOG_TAG, message);
	}
	
	public static void logv(String format, Object... args)
	{
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
