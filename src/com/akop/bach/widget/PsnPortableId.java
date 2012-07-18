/*
 * PsnPortableId.java 
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
import android.content.ContentResolver;
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
import com.akop.bach.PSN.Games;
import com.akop.bach.PSN.Profiles;
import com.akop.bach.Preferences;
import com.akop.bach.Preferences.WidgetInfo;
import com.akop.bach.PsnAccount;
import com.akop.bach.R;

public class PsnPortableId extends AppWidgetProvider
{
	private static class ProfileData
	{
		private static final String[] GAMES_PROJECTION = new String[] { 
			Games.ICON_URL,
		};
		private static final String[] GAMES_AGGR_PROJECTION = new String[] { 
			"SUM(" + Games.PROGRESS + ")",
		};
		
		public String onlineId;
		public String avatarUrl;
		public int trophiesBronze;
		public int trophiesSilver;
		public int trophiesGold;
		public int trophiesPlatinum;
		public int trophiesTotal;
		public int level;
		public int progress;
		public int gamesPlayed;
		public int trophiesUnlocked;
		
		public ProfileData(Context context)
		{
			this.onlineId = null;
			this.avatarUrl = null;
			this.trophiesBronze = 0;
			this.trophiesSilver = 0;
			this.trophiesGold = 0;
			this.trophiesPlatinum = 0;
			this.trophiesTotal = 0;
			this.level = 0;
			this.progress = 0;
			this.gamesPlayed = 0;
			this.trophiesUnlocked = 0;
		}
		
		public static ProfileData load(Context context, 
				PsnAccount account)
		{
			if (account == null)
				return null;
			
			ProfileData pd = new ProfileData(context);
			
			pd.onlineId = account.getScreenName();
			pd.avatarUrl = account.getIconUrl();
			
			ContentResolver cr = context.getContentResolver();
			Cursor cursor = cr.query(Profiles.CONTENT_URI, 
					new String[] { 
						Profiles.LEVEL,
						Profiles.PROGRESS,
						Profiles.TROPHIES_PLATINUM,
						Profiles.TROPHIES_GOLD,
						Profiles.TROPHIES_SILVER,
						Profiles.TROPHIES_BRONZE,
					},
					Profiles.ACCOUNT_ID + "=" + account.getId(), null, 
					null);
			
			if (cursor != null)
			{
				try
				{
					if (cursor.moveToFirst())
					{
						pd.level = cursor.getInt(0);
						pd.progress = cursor.getInt(1);
						pd.trophiesPlatinum = cursor.getInt(2);
						pd.trophiesGold = cursor.getInt(3);
						pd.trophiesSilver = cursor.getInt(4);
						pd.trophiesBronze = cursor.getInt(5);
						pd.trophiesTotal = pd.trophiesBronze 
							+ pd.trophiesSilver 
							+ pd.trophiesGold 
							+ pd.trophiesPlatinum;
					}
				}
				catch(Exception e)
				{
					if (App.LOGV)
						e.printStackTrace();
				}
				finally
				{
					cursor.close();
				}
			}
			
			// Load game data
			
			cursor = context.getContentResolver().query(Games.CONTENT_URI,
					GAMES_PROJECTION,
					Profiles.ACCOUNT_ID + "=" + account.getId(), null,
					Games.DEFAULT_SORT_ORDER);
			
			if (cursor != null)
			{
				try
				{
					pd.gamesPlayed = cursor.getCount();
				}
				catch(Exception e)
				{
					if (App.LOGV)
						e.printStackTrace();
				}
				finally
				{
					cursor.close();
				}
			}
			
			// Load aggregate game data
			
			cursor = context.getContentResolver().query(Games.CONTENT_URI,
					GAMES_AGGR_PROJECTION,
					Profiles.ACCOUNT_ID + "=" + account.getId(), null,
					Games.DEFAULT_SORT_ORDER);
			
			if (cursor != null)
			{
				try
				{
					if (cursor.moveToFirst() && pd.gamesPlayed > 0)
						pd.trophiesUnlocked = cursor.getInt(0) / pd.gamesPlayed;
				}
				catch(Exception e)
				{
					if (App.LOGV)
						e.printStackTrace();
				}
				finally
				{
					cursor.close();
				}
			}
			
			return pd;
		}
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
			ImageCache ic = ImageCache.get();
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
	
	@Override
	public void onDeleted(Context context, int[] appWidgetIds)
	{
		super.onDeleted(context, appWidgetIds);
		
		if (App.LOGV)
			App.logv("PsnPortableId::onDelete called");
		
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
			App.logv("PsnPortableId::onUpdate called");
		
		for (int appWidgetId : appWidgetIds)
			refreshWidget(appWidgetManager, context, appWidgetId, false);
	}
	
	public static void initialize(AppWidgetManager appWidgetManager,
			Context context, int appWidgetId)
	{
		try
		{
			// Refresh the account
			PsnAccount account = getAccount(context, appWidgetId);
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
	
	private static PsnAccount getAccount(Context context, int appWidgetId)
	{
		WidgetInfo info = Preferences.get(context).getWidget(appWidgetId);
		if (info == null)
			return null;
		
		return (PsnAccount)info.account;
	}
	
	private static void renderWidget(Context context, ProfileData pd, 
			RemoteViews views, int appWidgetId)
	{
		if (pd == null)
			return;
		
		// Online ID
		
		views.setTextViewText(R.id.widget_online_id, pd.onlineId);
		
		String iconUrl;
		Bitmap bmp;
		
		// Avatar
		
		if ((iconUrl = pd.avatarUrl) != null)
		{
			if ((bmp = ImageCache.get().getCachedBitmap(iconUrl)) != null)
			{
				// Cached; don't need to fetch
				
				views.setImageViewBitmap(R.id.widget_avatar, bmp);
			}
			else
			{
				// Not cached; get it
				
				TaskParam param = new TaskParam();
				
				param.imageUrl = iconUrl;
				param.resId = R.id.widget_avatar;
				param.views = views;
				
				new AsyncImageLoader().execute(param);
			}
		}
		
		views.setTextViewText(R.id.widget_level, 
				String.valueOf(pd.level));
		views.setTextViewText(R.id.widget_trophy_total, 
				String.valueOf(pd.trophiesTotal));
		views.setProgressBar(R.id.widget_progress_bar, 
				100, pd.progress, false);
		
		views.setTextViewText(R.id.widget_trophies_bronze, 
				context.getString(R.string.x_bronze, pd.trophiesBronze));
		views.setTextViewText(R.id.widget_trophies_silver, 
				context.getString(R.string.x_silver, pd.trophiesSilver));
		views.setTextViewText(R.id.widget_trophies_gold, 
				context.getString(R.string.x_gold, pd.trophiesGold));
		views.setTextViewText(R.id.widget_trophies_platinum, 
				context.getString(R.string.x_platinum, pd.trophiesPlatinum));
		
		views.setTextViewText(R.id.widget_games_played, 
				String.valueOf(pd.gamesPlayed));
		views.setTextViewText(R.id.widget_trophies_unlocked, 
				String.valueOf(pd.trophiesUnlocked) + "%");
	}
	
	private static void refreshWidget(final AppWidgetManager appWidgetManager,
			final Context context, final int appWidgetId, final boolean forceRefresh)
	{
		final PsnAccount account = getAccount(context, appWidgetId);
		final RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.psn_widget_portable_id);
		
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
					// Update account
					if (App.LOGV)
						App.logv("PsnPortableId[%s]: Updating account data...", 
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
						App.logv("PsnPortableId[%s]: Updating games list...", 
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
