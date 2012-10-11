/*
 * XboxLive2x2Gamercard.java 
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

import java.util.ArrayList;
import java.util.List;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.widget.RemoteViews;

import com.akop.bach.App;
import com.akop.bach.ImageCache;
import com.akop.bach.Preferences;
import com.akop.bach.Preferences.WidgetInfo;
import com.akop.bach.R;
import com.akop.bach.XboxLive.Games;
import com.akop.bach.XboxLive.Profiles;
import com.akop.bach.XboxLiveAccount;

public class XboxLive2x2Gamercard extends AppWidgetProvider
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
	
	private static final int gameViews[] = { 
		R.id.profile_game0,
		R.id.profile_game1,
		R.id.profile_game2,
	};
	
	private static class ProfileData
	{
		private static final String[] PROFILE_PROJECTION = new String[] { 
			Profiles.GAMERSCORE,
			Profiles.REP,
			Profiles.ZONE,
		};
		private static final String[] GAMES_PROJECTION = new String[] { 
			Games.BOXART_URL,
		};
		
		public String gamertag;
		public String avatarUrl;
		public int gamerscore;
		public String[] gameIconUrls;
		private int rep;
		
		public ProfileData(Context context)
		{
			this.rep = 0;
			this.gamerscore = 0;
			this.gameIconUrls = null;
			this.gamertag = null;
			this.avatarUrl = null;
		}
		
		public static ProfileData load(Context context, 
				XboxLiveAccount account)
		{
			if (account == null)
				return null;
			
			ProfileData pd = new ProfileData(context);
			
			pd.gamertag = account.getGamertag();
			pd.avatarUrl = account.getIconUrl();
			
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
				if (App.getConfig().logToConsole())
					e.printStackTrace();
			}
			finally
			{
				if (cursor != null)
					cursor.close();
			}
			
			// Load game data
			
			String criteria = Games.ACCOUNT_ID + "=" + account.getId();
			if (!account.isShowingApps())
				criteria += " AND " + Games.ACHIEVEMENTS_TOTAL + "> 0"; 
			
			cursor = context.getContentResolver().query(Games.CONTENT_URI,
					GAMES_PROJECTION,
					criteria, null,
					Games.DEFAULT_SORT_ORDER);
			
			try
			{
				if (cursor != null)
				{
					int n = Math.min(cursor.getCount(), gameViews.length);
					
					pd.gameIconUrls = new String[n];
					
					for (int i = 0; i < n && cursor.moveToNext(); i++)
						pd.gameIconUrls[i] = cursor.getString(0);
				}
			}
			catch(Exception e)
			{
				if (App.getConfig().logToConsole())
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
		
		if (App.getConfig().logToConsole())
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
		
		if (App.getConfig().logToConsole())
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
			if (App.getConfig().logToConsole())
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
	
	private static class TaskParam
	{
		public RemoteViews views;
		public int resId;
		public String imageUrl;
		public Bitmap bmp;
	}
	
	private static class AsyncImageLoader extends AsyncTask<TaskParam, Void, List<TaskParam>>
	{
		@Override
        protected List<TaskParam> doInBackground(TaskParam... params)
        {
			ImageCache ic = ImageCache.getInstance();
			List<TaskParam> outParams = new ArrayList<TaskParam>();
			
			for (int i = 0; i < params.length; i++)
			{
				if (params[i] == null)
					continue;
				
				try
				{
					params[i].bmp = ic.getBitmap(params[i].imageUrl, false);
				}
				catch(Exception e)
				{
					params[i].bmp = null;
				}
				
				outParams.add(params[i]);
			}
			
			return outParams;
        }
		
		@Override
		protected void onPostExecute(List<TaskParam> result)
		{
			for (int i = 0; i < result.size(); i++)
			{
				TaskParam param = result.get(i);
				if (param.bmp != null)
					param.views.setImageViewBitmap(param.resId, param.bmp);
			}
		}
	};
	
	private static void renderWidget(Context context, ProfileData pd, 
			RemoteViews views, int appWidgetId)
	{
		if (pd == null)
			return;
		
		// Gamertag
		
		views.setTextViewText(R.id.profile_gamertag, pd.gamertag);
		
		String iconUrl;
		Bitmap bmp;
		
		// Avatar
		
		if ((iconUrl = pd.avatarUrl) != null)
		{
			if ((bmp = ImageCache.getInstance().getCachedBitmap(iconUrl)) != null)
			{
				// Cached; don't need to fetch
				
				views.setImageViewBitmap(R.id.profile_gamerpic, bmp);
			}
			else
			{
				// Not cached; get it
				
				TaskParam param = new TaskParam();
				
				param.imageUrl = iconUrl;
				param.resId = R.id.profile_gamerpic;
				param.views = views;
				
				new AsyncImageLoader().execute(param);
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
		
		// Update played game icons
		if (pd.gameIconUrls != null)
		{
			ArrayList<TaskParam> paramList = new ArrayList<TaskParam>();
			for (int i = 0, n = pd.gameIconUrls.length; i < n; i++)
			{
				if ((iconUrl = pd.gameIconUrls[i]) != null)
				{
					if ((bmp = ImageCache.getInstance().getCachedBitmap(iconUrl)) != null)
					{
						// Cached; don't need to fetch
						
						views.setImageViewBitmap(gameViews[i], bmp);
					}
					else
					{
						// Not cached; get it
						
						TaskParam param = new TaskParam();
						
						param.imageUrl = iconUrl;
						param.resId = gameViews[i];
						param.views = views;
						
						paramList.add(param);
					}
				}
			}
			
			if (paramList.size() > 0)
			{
				TaskParam[] params = new TaskParam[paramList.size()];
				paramList.toArray(params);
				
				new AsyncImageLoader().execute(params);
			}
		}
	}
	
	private static void refreshWidget(final AppWidgetManager appWidgetManager,
			final Context context, final int appWidgetId, final boolean forceRefresh)
	{
		final XboxLiveAccount account = getAccount(context, appWidgetId);
		final RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.xbl_2x2_gamercard_widget);
		
		if (account == null)
		{
			if (App.getConfig().logToConsole())
				App.logv("Widget %d is referencing an invalid account", appWidgetId);
			
			views.setTextViewText(R.id.profile_gamertag, 
					context.getString(R.string.account_removed));
			appWidgetManager.updateAppWidget(appWidgetId, views);
			
			return;
		}
		
		if (App.getConfig().logToConsole())
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
					if (App.getConfig().logToConsole())
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
						if (App.getConfig().logToConsole())
							e.printStackTrace();
					}
					
					// Update games list
					if (App.getConfig().logToConsole())
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
						if (App.getConfig().logToConsole())
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
