/*
 * AchievementsFragment.java 
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

package com.akop.bach.fragment.xboxlive;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.text.DateFormat;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.akop.bach.App;
import com.akop.bach.IAccount;
import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.OnImageReadyListener;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.CustomTask;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.XboxLive.Achievements;
import com.akop.bach.XboxLive.Games;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.activity.xboxlive.GameOverview;
import com.akop.bach.fragment.GenericFragment;
import com.akop.bach.fragment.xboxlive.BeaconTextPrompt.OnOkListener;
import com.akop.bach.parser.AuthenticationException;
import com.akop.bach.parser.Parser;
import com.akop.bach.parser.ParserException;
import com.akop.bach.parser.XboxLiveParser;
import com.akop.bach.uiwidget.XboxLiveGameListItem;
import com.akop.bach.uiwidget.XboxLiveGameListItem.OnBeaconClickListener;

public class AchievementsFragment extends GenericFragment implements
		OnItemClickListener, OnBeaconClickListener
{
	private static final String[] ACH_PROJ = new String[]
	{ 
		Achievements._ID, 
		Achievements.TITLE, 
		Achievements.DESCRIPTION,
		Achievements.POINTS, 
		Achievements.ACQUIRED, 
		Achievements.LOCKED,
		Achievements.ICON_URL 
	};
	
	private static final int ACH_TITLE = 1;
	private static final int ACH_DESCRIPTION = 2;
	private static final int ACH_POINTS = 3;
	private static final int ACH_ACQUIRED = 4;
	private static final int ACH_LOCKED = 5;
	private static final int ACH_ICON_URL = 6;
	
	private class ViewHolder
	{
		public TextView title;
		public TextView description;
		public TextView acquired;
		public TextView pointStats;
		public ImageView icon;
	}
	
	private final ContentObserver mObserver = new ContentObserver(new Handler())
	{
		@Override
		public void onChange(boolean selfUpdate)
		{
			super.onChange(selfUpdate);
			
			// A game has just been updated. Check to see if our particular
			// game is now 'dirty'
			if (mTitleId > 0 && Games.isDirty(getActivity(), 
					mAccount, mTitleId))
			{
				if (App.LOGV)
					App.logv("Game is Dirty - updating achieves");
				
				// It's dirty; refresh the achievements
				synchronizeWithServer();
			}
			
			synchronizeLocal();
		}
    };
    
	private TaskListener mBeaconListener = new TaskListener("GameBeacon")
	{
		@Override
		public void onTaskFailed(IAccount account, final Exception e)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					Toast toast = Toast.makeText(getActivity(), 
							Parser.getErrorMessage(getActivity(), e), 
							Toast.LENGTH_LONG);
					toast.show();
				}
			});
		}
	};
	
	private TaskListener mListener = new TaskListener("XBoxAchievements")
	{
		@Override
		public void onTaskFailed(IAccount account, final Exception e)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					mMessage.setText(XboxLiveParser.getErrorMessage(getActivity(), e));
					
					mListView.setEmptyView(mMessage);
					mProgress.setVisibility(View.GONE);
				}
			});
		}
		
		@Override
		public void onTaskSucceeded(IAccount account, Object requestParam,
				Object result)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					mMessage.setText(R.string.game_has_no_achieves);
					
					mListView.setEmptyView(mMessage);
					mProgress.setVisibility(View.GONE);
					
					syncIcons();
				}
			});
		}
	};
	
	private class MyCursorAdapter extends CursorAdapter
	{
		private DateFormat DATE_FORMAT = DateFormat.getDateInstance();

		public MyCursorAdapter(Context context, Cursor c)
		{
			super(context, c);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			LayoutInflater li = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			View view = li.inflate(R.layout.xbl_achievement_list_item, parent,
					false);

			ViewHolder vh = new ViewHolder();
			view.setTag(vh);

			vh.icon = (ImageView) view.findViewById(R.id.achievement_icon);
			vh.title = (TextView) view.findViewById(R.id.achievement_title);
			vh.description = (TextView) view
					.findViewById(R.id.achievement_description);
			vh.pointStats = (TextView) view.findViewById(R.id.achievement_gp);
			vh.acquired = (TextView) view
					.findViewById(R.id.achievement_acquired);

			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			ViewHolder vh = (ViewHolder) view.getTag();

			vh.title.setText(cursor.getString(ACH_TITLE));
			vh.description.setText(cursor.getString(ACH_DESCRIPTION));
			vh.pointStats.setText(context.getString(R.string.x_f, cursor
					.getInt(ACH_POINTS)));

			boolean locked = cursor.getInt(ACH_LOCKED) != 0;
			long acquired = cursor.getLong(ACH_ACQUIRED);
			String acqString = null;

			if (locked)
			{
				acqString = context.getString(R.string.locked);
			}
			else
			{
				acqString = (acquired == 0) ? context
						.getString(R.string.acquired_offline) : context
						.getString(R.string.acquired_f, DATE_FORMAT
								.format(acquired));
			}

			vh.acquired.setText(acqString);

			String iconUrl = cursor.getString(ACH_ICON_URL);
			SoftReference<Bitmap> icon = mIconCache.get(iconUrl);

			if (icon != null && icon.get() != null)
			{
				// Image is in the in-memory cache
				vh.icon.setImageBitmap(icon.get());
			}
			else
			{
				// Image has likely been garbage-collected
				// Load it into the cache again
				Bitmap bmp = ImageCache.get().getCachedBitmap(iconUrl);
				if (bmp != null)
				{
					mIconCache.put(iconUrl, new SoftReference<Bitmap>(bmp));
					vh.icon.setImageBitmap(bmp);
				}
				else
				{
					// Image failed to load - just use placeholder
					vh.icon.setImageResource(R.drawable.xbox_achieve_locked_default);
				}
			}
		}
	}
	
	private LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderCallbacks<Cursor>()
	{
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args)
		{
			return new CursorLoader(getActivity(), Achievements.CONTENT_URI,
					ACH_PROJ, Achievements.GAME_ID + "=" + mTitleId, 
					null, Achievements.DEFAULT_SORT_ORDER);
		}
		
		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data)
		{
			if (mAdapter != null)
				mAdapter.changeCursor(data);
			
			if (data.getCount() < 1)
				mMessage.setText(R.string.game_has_no_achieves);
		}
		
		@Override
		public void onLoaderReset(Loader<Cursor> arg0)
		{
			if (mAdapter != null)
				mAdapter.changeCursor(null);
		}
	};
	
	private Handler mHandler = new Handler();
	
	private XboxLiveAccount mAccount;
	private long mTitleId = -1;
	
	private CursorAdapter mAdapter = null;
	private ListView mListView = null;
	private TextView mMessage = null;
	private View mProgress = null;
	private String mGameTitle = null;
	private boolean mShowGameTotals = false;
	
	public static AchievementsFragment newInstance(XboxLiveAccount account,
			boolean showGameTotals)
	{
		return newInstance(account, -1, showGameTotals);
	}
	
	public static AchievementsFragment newInstance(XboxLiveAccount account,
			long titleId, boolean showGameTotals)
	{
		AchievementsFragment f = new AchievementsFragment();
		
		Bundle args = new Bundle();
		args.putSerializable("account", account);
		args.putLong("titleId", titleId);
		args.putBoolean("showGameTotals", showGameTotals);
		f.setArguments(args);
		
		return f;
	}
	
	@Override
	public void onCreate(Bundle state)
	{
		super.onCreate(state);
		
		if (mAccount == null)
		{
		    Bundle args = getArguments();
		    
		    mAccount = (XboxLiveAccount)args.getSerializable("account");
		    mTitleId = args.getLong("titleId", -1);
		}
		
	    if (state != null)
	    {
			mAccount = (XboxLiveAccount)state.getSerializable("account");
			mTitleId = state.getLong("titleId");
		}
	    
		setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		if (container == null)
			return null;
		
		View layout = inflater.inflate(R.layout.xbl_fragment_achievement_list,
				container, false);
		
		mShowGameTotals = false;
		View gameDetails = layout.findViewById(R.id.game_details);
		
		if (gameDetails != null)
		{
			mShowGameTotals = getArguments().getBoolean("showGameTotals");
			
			gameDetails.setVisibility(mShowGameTotals ? View.VISIBLE : View.GONE);
			gameDetails.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					String url = Games.getGameUrl(getActivity(), mTitleId);
					if (url != null)
						GameOverview.actionShow(getActivity(), mAccount, url);
				}
			});
			
			if (mShowGameTotals)
			{
				XboxLiveGameListItem view = (XboxLiveGameListItem)gameDetails.findViewById(R.id.gameListItem);
				view.mClickListener = this;
			}
		}
		
		mAdapter = new MyCursorAdapter(getActivity(), null);
		
		mMessage = (TextView) layout.findViewById(R.id.message);
		mMessage.setText(R.string.no_game_selected);
		
		mProgress = layout.findViewById(R.id.loading);
		mProgress.setVisibility(View.GONE);
		
		mListView = (ListView) layout.findViewById(R.id.list);
		mListView.setOnItemClickListener(this);
		mListView.setAdapter(mAdapter);
		mListView.setEmptyView(mMessage);
		
		registerForContextMenu(mListView);
		
		return layout;
	}
	
	@Override
	public void onActivityCreated(Bundle state)
	{
		super.onActivityCreated(state);
		
		getLoaderManager().initLoader(0, null, mLoaderCallbacks);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		outState.putSerializable("account", mAccount);
		outState.putLong("titleId", mTitleId);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		
		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) menuInfo;
		
		ViewHolder vh = (ViewHolder) acmi.targetView.getTag();
		menu.setHeaderTitle(vh.title.getText());
		
		getActivity().getMenuInflater().inflate(
				R.menu.xbl_achievement_list_context, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem menuItem)
	{
		AdapterView.AdapterContextMenuInfo info = 
			(AdapterView.AdapterContextMenuInfo)menuItem.getMenuInfo();
		
		if (mGameTitle != null && info.targetView.getTag() instanceof ViewHolder)
		{
			ViewHolder vh = (ViewHolder)info.targetView.getTag();
			
			switch (menuItem.getItemId())
			{
			case R.id.google_achievement:
				Intent searchIntent = new Intent(Intent.ACTION_WEB_SEARCH);
				searchIntent.putExtra(SearchManager.QUERY, getString(
						R.string.google_achievement_f, mGameTitle, vh.title
								.getText()));
				startActivity(searchIntent);
				return true;
			}
		}
		
		return super.onContextItemSelected(menuItem);
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		
		TaskController.get().removeListener(mListener);
		TaskController.get().removeListener(mBeaconListener);
		
		ContentResolver cr = getActivity().getContentResolver();
        cr.unregisterContentObserver(mObserver);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		
		TaskController.get().addListener(mListener);
		TaskController.get().addListener(mBeaconListener);
		
		ContentResolver cr = getActivity().getContentResolver();
		cr.registerContentObserver(Games.CONTENT_URI, true, mObserver);
		
		synchronizeLocal();
	}
	
	@Override
	public void onImageReady(long id, Object param, Bitmap bmp)
	{
		super.onImageReady(id, param, bmp);
		
		getActivity().getContentResolver().notifyChange(
				ContentUris.withAppendedId(Achievements.CONTENT_URI, id), null);
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position,
			long arg3)
	{
		Cursor c = (Cursor)arg0.getItemAtPosition(position);
		if (c != null)
		{
			String title = c.getString(ACH_TITLE);
			String description = c.getString(ACH_DESCRIPTION);
			
			showAchievementDetails(title, description);
		}
	}
	
	@Override
    protected Cursor getIconCursor()
    {
		if (getActivity() == null)
			return null;
		
		ContentResolver cr = getActivity().getContentResolver();
		return cr.query(Achievements.CONTENT_URI, 
				new String[] { Achievements._ID, Achievements.ICON_URL },
				Achievements.GAME_ID + "=" + mTitleId, 
				null, Achievements.DEFAULT_SORT_ORDER);
    }
	
	private void showAchievementDetails(CharSequence title, CharSequence description)
	{
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View layout = inflater.inflate(R.layout.xbl_achievement_toast,
				(ViewGroup)getActivity().findViewById(R.id.toast_root));
		
		TextView text = (TextView) layout.findViewById(R.id.achievement_title);
		text.setText(title);
		text = (TextView) layout.findViewById(R.id.achievement_description);
		text.setText(description);
		
		Toast toast = new Toast(getActivity());
		toast.setDuration(Toast.LENGTH_LONG);
		toast.setView(layout);
		
		toast.show();
	}
	
	private void synchronizeLocal()
	{
		getLoaderManager().restartLoader(0, null, mLoaderCallbacks);
		
		if (mTitleId >= 0)
		{
			boolean exists = false;
			boolean isDirty = false;
			
			ContentResolver cr = getActivity().getContentResolver();
			Cursor cursor = cr.query(Games.CONTENT_URI, 
					new String[] { Games.TITLE, Games.ACHIEVEMENTS_STATUS }, 
					Games._ID + "=" + mTitleId, 
					null, null);
			
			if (cursor != null)
			{
				try
				{
					if (cursor.moveToFirst())
					{
						isDirty = (cursor.getInt(1) != 0);
						exists = true;
						mGameTitle = cursor.getString(0);
					}
				}
				finally
				{
					cursor.close();
				}
			}
			
			if (isDirty)
				synchronizeWithServer();
			
			if (!exists)
				mTitleId = -1;
		}
		
		loadGameDetails();
	}
	
	private void synchronizeWithServer()
	{
		mListView.setEmptyView(mProgress);
		mMessage.setVisibility(View.GONE);
		
		TaskController.get().synchronizeAchievements(mAccount, mTitleId,
				mListener);
	}
	
	public void resetTitle(long id)
	{
		mTitleId = id;
		
		synchronizeLocal();
		syncIcons();
	}
	
	private void loadGameDetails()
	{
		View view = getView();
		if (view == null)
			return;
		
		if (mTitleId < 0)
		{
			view.findViewById(R.id.unselected).setVisibility(View.VISIBLE);
			view.findViewById(R.id.achievement_contents).setVisibility(View.GONE);
		}
		else
		{
			view.findViewById(R.id.unselected).setVisibility(View.GONE);
			view.findViewById(R.id.achievement_contents).setVisibility(View.VISIBLE);
		}
		
		if (!mShowGameTotals)
			return;
		
		Context context = getActivity();
		ContentResolver cr = context.getContentResolver();
		Cursor c = cr.query(Games.CONTENT_URI, GamesFragment.PROJ, 
				Games._ID + "=" + mTitleId, null, null);
		
		if (c != null)
		{
			try
			{
				if (c.moveToFirst())
				{
					TextView tv;
					ImageView iv;
					
					XboxLiveGameListItem gameListItem = (XboxLiveGameListItem)view.findViewById(R.id.gameListItem);
					gameListItem.mBeaconSet = c.getInt(GamesFragment.GAME_BEACON_SET) != 0;
					gameListItem.mItemId = mTitleId;
					
					if ((tv = (TextView)view.findViewById(R.id.game_title)) != null)
						tv.setText(c.getString(GamesFragment.GAME_TITLE));
					
					if ((tv = (TextView)view.findViewById(R.id.game_last_played)) != null)
						tv.setText(Games.getLastPlayedText(context, 
							c.getLong(GamesFragment.GAME_LAST_PLAYED)));
					
					if ((tv = (TextView)view.findViewById(R.id.game_achievements_unlocked)) != null)
						tv.setText(Games.getAchievementTotalText(context, 
							c.getInt(GamesFragment.GAME_ACH_EARNED),
							c.getInt(GamesFragment.GAME_ACH_TOTAL)));
					
					if ((tv = (TextView)view.findViewById(R.id.game_gp_earned)) != null)
						tv.setText(Games.getGamerscoreTotalText(context, 
							c.getInt(GamesFragment.GAME_GP_EARNED),
							c.getInt(GamesFragment.GAME_GP_TOTAL)));
					
					iv = (ImageView)view.findViewById(R.id.game_beacon);
					iv.setImageResource(c.getInt(GamesFragment.GAME_BEACON_SET) != 0
							? R.drawable.beacon_on : R.drawable.beacon_off);
					
					ImageCache ic = ImageCache.get();
					String iconUrl = c.getString(GamesFragment.GAME_ICON_URL);
					Bitmap bmp;
					
					if ((bmp = ic.getCachedBitmap(iconUrl)) != null)
					{
						iv = (ImageView)view.findViewById(R.id.game_icon);
						iv.setImageBitmap(bmp);
					}
					else
					{
						if (iconUrl != null)
						{
							ic.requestImage(iconUrl, new OnImageReadyListener()
							{
								@Override
								public void onImageReady(long id, Object param,
										Bitmap bmp)
								{
									mHandler.post(new Runnable()
									{
										@Override
										public void run()
										{
											loadGameDetails();											
										}
									});
								}
							}, 0, null, true, null);
						}
					}
				}
			}
			finally
			{
				c.close();
			}
		}
	}
	
	@Override
    public void beaconClicked(final long id, boolean isSet)
    {
		if (isSet)
		{
			// Beacon is currently set, so unset it
			
			TaskController.get().runCustomTask(null, new CustomTask<Void>()
					{
						@Override
						public void runTask() throws AuthenticationException,
								IOException, ParserException
						{
							XboxLiveParser p = new XboxLiveParser(getActivity());
							
							try
							{
								p.removeBeacon(mAccount, id);
							}
							finally
							{
								p.dispose();
							}
						}
					}, mBeaconListener);
		}
		else
		{
			// Beacon is unset - set it 
			
			if (Games.getSetBeaconCount(getActivity(), mAccount) >= XboxLiveParser.MAX_BEACONS)
			{
				Toast.makeText(getActivity(), 
						getString(R.string.too_many_beacons_f, XboxLiveParser.MAX_BEACONS), 
						Toast.LENGTH_LONG).show();
				
				return;
			}
			
			BeaconTextPrompt prompt = BeaconTextPrompt.newInstance();
			prompt.setOnOkListener(new OnOkListener()
			{
				@Override
				public void beaconTextEntered(final String message)
				{
					TaskController.get().runCustomTask(null, new CustomTask<Void>()
							{
								@Override
								public void runTask() throws AuthenticationException,
										IOException, ParserException
								{
									XboxLiveParser p = new XboxLiveParser(getActivity());
									
									try
									{
										p.setBeacon(mAccount, id, message);
									}
									finally
									{
										p.dispose();
									}
								}
							}, mBeaconListener);
				}
			});
			
			prompt.show(getFragmentManager(), "dialog");
		}
    }
}
