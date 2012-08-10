/*
 * XboxLiveAvatarGamercard.java 
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

package com.akop.bach.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import com.akop.bach.App;
import com.akop.bach.ImageCache;
import com.akop.bach.Preferences;
import com.akop.bach.Preferences.WidgetInfo;
import com.akop.bach.R;
import com.akop.bach.XboxLive.Profiles;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.parser.XboxLiveParser;

public class XboxLiveAvatarGamercard extends AppWidgetProvider
{
	private static final int starViews[] = { 
		R.id.profile_rep_star0,
		R.id.profile_rep_star1,
		R.id.profile_rep_star2,
		R.id.profile_rep_star3,
		R.id.profile_rep_star4,
	};
	
	private static final int starResources[] = { 
			R.drawable.xbox_ic_star0,
			R.drawable.xbox_ic_star1,
			R.drawable.xbox_ic_star2,
			R.drawable.xbox_ic_star3,
			R.drawable.xbox_ic_star4,
	};
	
	private static class ProfileData
	{
		private static final String[] PROFILE_PROJECTION = new String[] { 
			Profiles.GAMERSCORE,
			Profiles.REP,
			Profiles.ZONE,
		};
		
		public String gamertag;
		public String gamerpicUrl;
		public int gamerscore;
		private int rep;
		
		public ProfileData(Context context)
		{
			this.rep = 0;
			this.gamerscore = 0;
			this.gamertag = null;
			this.gamerpicUrl = null;
		}
		
		public static ProfileData load(Context context, 
				XboxLiveAccount account)
		{
			if (account == null)
				return null;
			
			ProfileData pd = new ProfileData(context);
			
			pd.gamertag = account.getGamertag();
			pd.gamerpicUrl = account.getIconUrl();
			
			Cursor cursor = context.getContentResolver().query(Profiles.CONTENT_URI,
					PROFILE_PROJECTION,
					Profiles.ACCOUNT_ID + "=" + account.getId(), null,
					null);
			
			// Load profile data
			
			try
			{
				if (cursor != null && cursor.moveToFirst())
				{
					pd.gamerscore = cursor.getInt(0);
					pd.rep = cursor.getInt(1);
				}
			}
			catch(Exception e)
			{
				if (App.LOGV)
					e.printStackTrace();
			}
			finally
			{
				if (cursor != null)
					cursor.close();
			}
			
			return pd;
		}
	}
	
	@Override
	public void onDeleted(Context context, int[] appWidgetIds)
	{
		super.onDeleted(context, appWidgetIds);
		
		if (App.LOGV)
			App.logv("XBoxLiveGamerCard::onDelete called");
		
		for (int appWidgetId : appWidgetIds)
		{
			// Remove widget from preferences
			Preferences.get(context).deleteWidget(appWidgetId);
		}
	}
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds)
	{
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		
		if (App.LOGV)
			App.logv("XBoxLiveGamerCard::onUpdate called");
		
		for (int appWidgetId : appWidgetIds)
			refreshWidget(appWidgetManager, context, appWidgetId, false);
	}
	
	public static void initialize(AppWidgetManager appWidgetManager,
			Context context, int appWidgetId)
	{
		try
		{
			// Refresh the account
			XboxLiveAccount account = getAccount(context, appWidgetId);
			if (account == null) // Not yet configured
				return;
			
			// Refresh the widget
			refreshWidget(appWidgetManager, context, appWidgetId, true);
		}
		catch(Exception e)
		{
			if (App.LOGV)
				e.printStackTrace();
		}
	}
	
	private static XboxLiveAccount getAccount(Context context, int appWidgetId)
	{
		WidgetInfo info = Preferences.get(context).getWidget(appWidgetId);
		if (info == null)
			return null;
		
		return (XboxLiveAccount)info.account;
	}
	
	private static void renderWidget(Context context, ProfileData pd, 
			RemoteViews views, int appWidgetId)
	{
		if (pd == null)
			return;
		
		ImageCache ic = ImageCache.getInstance();
		
		// Gamertag
		
		views.setTextViewText(R.id.profile_gamertag, pd.gamertag);
		
		String iconUrl;
		Bitmap bmp;
		
		// Gamerpic
		
		if ((iconUrl = pd.gamerpicUrl) != null)
		{
			try
			{
				if ((bmp = ic.getBitmap(iconUrl, false)) != null)
					views.setImageViewBitmap(R.id.profile_gamerpic, bmp);
			}
			catch(Exception e)
			{
				// Do nothing; ignore any errors
				if (App.LOGV)
					e.printStackTrace();
			}
		}
		
		// Avatar body
		
		if ((iconUrl = XboxLiveParser.getAvatarUrl(pd.gamertag)) != null)
		{
			try
			{
				if ((bmp = ic.getBitmap(iconUrl, false)) != null)
					views.setImageViewBitmap(R.id.profile_avatar, bmp);
			}
			catch(Exception e)
			{
				// Do nothing; ignore any errors
				if (App.LOGV)
					e.printStackTrace();
			}
		}
		
		views.setTextViewText(R.id.profile_gamerscore, 
				context.getString(R.string.x_f, pd.gamerscore));
		
		// Update rep
		int res;
		for (int starPos = 0, j = 0, k = 4; starPos < 5; starPos++, j += 4, k += 4)
		{
			if (pd.rep < j) res = 0;
			else if (pd.rep >= k) res = 4;
			else res = pd.rep - j;
			
			views.setImageViewResource(starViews[starPos], starResources[res]);
		}
	}
	
	private static void refreshWidget(final AppWidgetManager appWidgetManager,
			final Context context, final int appWidgetId, final boolean forceRefresh)
	{
		final XboxLiveAccount account = getAccount(context, appWidgetId);
		final RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.xbl_avatar_gamercard_widget);
		
		if (account == null)
		{
			if (App.LOGV)
				App.logv("Widget %d is referencing an invalid account", appWidgetId);
			
			views.setTextViewText(R.id.profile_gamertag, 
					context.getString(R.string.account_removed));
			appWidgetManager.updateAppWidget(appWidgetId, views);
			
			return;
		}
		
		if (App.LOGV)
			App.logv("Updating widget %d", appWidgetId);
		
		Uri uri = ContentUris.withAppendedId(Profiles.CONTENT_URI,
				account.getId());
		
        // Create an Intent to launch Account Summary
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        // Attach an on-click listener to the entire widget
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
				intent, 0);
		views.setOnClickPendingIntent(R.id.gamercard_widget_container,
				pendingIntent);
        
		// Render rest of widget
		renderWidget(context, ProfileData.load(context, account), 
				views, appWidgetId);
		
		appWidgetManager.updateAppWidget(appWidgetId, views);
		
		// Start an update task
		Thread t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				views.setViewVisibility(R.id.widget_loading, View.VISIBLE);
				appWidgetManager.updateAppWidget(appWidgetId, views);
				
				try
				{
					// Update account (for GS, rep)
					if (App.LOGV)
						App.logv("XboxLiveGamercard[%s]: Updating account data...", 
								account.getScreenName());
					
					long timeDiff;
					
					try
					{
						timeDiff = System.currentTimeMillis() - account.getLastSummaryUpdate();
						if (forceRefresh || timeDiff > account.getSummaryRefreshInterval())
							account.updateProfile(context);
					}
					catch(Exception e)
					{
						// Do nothing; ignore any errors
						if (App.LOGV)
							e.printStackTrace();
					}
					
					// Update games list
					if (App.LOGV)
						App.logv("XboxLiveGamercard[%s]: Updating games list...", 
								account.getScreenName());
					
					try
					{
						timeDiff = System.currentTimeMillis() - account.getLastGameUpdate();
						if (forceRefresh || timeDiff > account.getGameHistoryRefreshInterval())
							account.updateGames(context);
					}
					catch(Exception e)
					{
						// Do nothing; ignore any errors
						if (App.LOGV)
							e.printStackTrace();
					}
					
					// Update the widget
					renderWidget(context, ProfileData.load(context, account), 
							views, appWidgetId);
				}
				finally
				{
					views.setViewVisibility(R.id.widget_loading, View.GONE);
					appWidgetManager.updateAppWidget(appWidgetId, views);
				}
			}
		});
		
		t.start();
	}
}
