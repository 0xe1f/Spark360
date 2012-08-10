/*
 * GamesFragment.java 
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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.akop.bach.IAccount;
import com.akop.bach.ImageCache;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.CustomTask;
import com.akop.bach.TaskController.TaskListener;
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

public class GamesFragment extends GenericFragment implements
		OnItemClickListener, OnBeaconClickListener
{
	public static interface OnGameSelectedListener
	{
		void onGameSelected(long id);
	}
	
	public static final String[] PROJ = new String[]
 	{ 
 		Games._ID, 
 		Games.TITLE, 
 		Games.LAST_PLAYED, 
 		Games.ACHIEVEMENTS_UNLOCKED,
 		Games.ACHIEVEMENTS_TOTAL, 
 		Games.POINTS_ACQUIRED,
 		Games.POINTS_TOTAL, 
 		Games.BOXART_URL, 
 		Games.GAME_URL, 
 		Games.BEACON_SET,
 		Games.BEACON_TEXT,
 	};
 	
	public static final int GAME_TITLE = 1;
	public static final int GAME_LAST_PLAYED = 2;
	public static final int GAME_ACH_EARNED = 3;
	public static final int GAME_ACH_TOTAL = 4;
	public static final int GAME_GP_EARNED = 5;
	public static final int GAME_GP_TOTAL = 6;
	public static final int GAME_ICON_URL = 7;
	public static final int GAME_URL = 8;
	public static final int GAME_BEACON_SET = 9;
	public static final int GAME_BEACON_TEXT = 10;
	
	private class ViewHolder
	{
		public TextView title;
		public TextView lastPlayed;
		public TextView achStats;
		public TextView pointStats;
		public ImageView icon;
		public String gameUrl;
		public ImageView beacon;
	}
	
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
					if (getActivity() != null && e != null)
					{
						Toast toast = Toast.makeText(getActivity(), 
								Parser.getErrorMessage(getActivity(), e), 
								Toast.LENGTH_LONG);
						toast.show();
					}
				}
			});
		}
	};
	
	private TaskListener mListener = new TaskListener("XBoxGames")
	{
		@Override
		public void onTaskFailed(IAccount account, final Exception e)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					if (getActivity() != null && e != null)
						mMessage.setText(XboxLiveParser.getErrorMessage(getActivity(), e));
					
					mListView.setEmptyView(mMessage);
					mProgress.setVisibility(View.GONE);
				}
			});
		}
		
		@Override
		public void onTaskSucceeded(IAccount account, Object requestParam, Object result)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					mMessage.setText(R.string.no_games_played);
					
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
			LayoutInflater li = (LayoutInflater)context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			XboxLiveGameListItem view = (XboxLiveGameListItem)li.inflate(R.layout.xbl_game_list_item, parent, false);
			ViewHolder vh = new ViewHolder();
			
			view.setTag(vh);
			view.mClickListener = GamesFragment.this;
			
			vh.icon = (ImageView)view.findViewById(R.id.game_icon);
			vh.title = (TextView)view.findViewById(R.id.game_title);
			vh.lastPlayed = (TextView)view.findViewById(R.id.game_last_played);
			vh.achStats = (TextView)view.findViewById(R.id.game_achievements_unlocked);
			vh.pointStats = (TextView)view.findViewById(R.id.game_gp_earned);
			vh.beacon = (ImageView)view.findViewById(R.id.game_beacon);
			
			return view;
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			XboxLiveGameListItem gameListItem = (XboxLiveGameListItem)view;
			gameListItem.mItemId = cursor.getLong(0);
			
			ViewHolder vh = (ViewHolder)view.getTag();
			
			vh.title.setText(cursor.getString(GAME_TITLE));
			vh.lastPlayed.setText(context.getString(R.string.last_played_f,
					DATE_FORMAT.format(cursor.getLong(GAME_LAST_PLAYED))));
			vh.pointStats.setText(context.getString(R.string.x_of_x_f,
					cursor.getInt(GAME_GP_EARNED), cursor
							.getInt(GAME_GP_TOTAL)));
			vh.gameUrl = cursor.getString(GAME_URL);
			
			gameListItem.mBeaconSet = (cursor.getInt(GAME_BEACON_SET) != 0);
			int achTotal = cursor.getInt(GAME_ACH_TOTAL);
			
			vh.achStats.setText((achTotal <= 0) 
					? context.getString(R.string.no_achievements) 
							: context.getString(R.string.achieves_x_of_x_f, 
									cursor.getInt(GAME_ACH_EARNED), 
									achTotal));
			
			String iconUrl = cursor.getString(GAME_ICON_URL);
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
				Bitmap bmp = ImageCache.getInstance().getCachedBitmap(iconUrl);
				if (bmp != null)
				{
					mIconCache.put(iconUrl, new SoftReference<Bitmap>(bmp));
					vh.icon.setImageBitmap(bmp);
				}
				else
				{
					// Image failed to load - just use placeholder
					vh.icon.setImageResource(R.drawable.xbox_game_default);
				}
			}
			
			vh.beacon.setImageResource(gameListItem.mBeaconSet
					? R.drawable.beacon_on : R.drawable.beacon_off);
		}
	}
	
	private LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderCallbacks<Cursor>()
	{
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args)
		{
			String criteria = Games.ACCOUNT_ID + "=" + mAccount.getId();
			if (!mAccount.isShowingApps())
				criteria += " AND " + Games.ACHIEVEMENTS_TOTAL + "> 0"; 
			
			return new CursorLoader(getActivity(), 
					Games.CONTENT_URI,
					PROJ, 
					criteria, 
					null,
					Games.DEFAULT_SORT_ORDER);
		}
		
		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data)
		{
			mAdapter.changeCursor(data);
			
			/*NEWTODO
			if (mListView.getCheckedItemCount() < 1 && mListView.getCount() > 0)
				mListView.setItemChecked(0, true);
			*/
		}
		
		@Override
		public void onLoaderReset(Loader<Cursor> arg0)
		{
			mAdapter.changeCursor(null);
		}
	};
	
	private Handler mHandler = new Handler();
	private CursorAdapter mAdapter = null;
	private ListView mListView = null;
	private TextView mMessage = null;
	private View mProgress = null;
	
	private XboxLiveAccount mAccount = null;
	private long mTitleId = -1;
	
	public static GamesFragment newInstance(XboxLiveAccount account)
	{
		GamesFragment f = new GamesFragment();
		
		Bundle args = new Bundle();
		args.putSerializable("account", account);
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
			ContentResolver cr = getActivity().getContentResolver();
		    
		    mAccount = (XboxLiveAccount)args.getSerializable("account");
			mTitleId = getFirstTitleId(cr.query(Games.CONTENT_URI,
					new String[] { Games._ID, },
					Games.ACCOUNT_ID + "=" + mAccount.getId(), 
					null, Games.DEFAULT_SORT_ORDER));
		}
		
	    if (state != null && state.containsKey("account"))
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
		
		View layout = inflater.inflate(R.layout.xbl_fragment_plain_list,
				container, false);
		
		mAdapter = new MyCursorAdapter(getActivity(), null);
		
		mMessage = (TextView)layout.findViewById(R.id.message);
		mMessage.setText(R.string.no_games_played);
		
		mListView = (ListView)layout.findViewById(R.id.list);
		mListView.setOnItemClickListener(this);
		mListView.setAdapter(mAdapter);
		mListView.setEmptyView(mMessage);
		
		registerForContextMenu(mListView);
		
		mProgress = layout.findViewById(R.id.loading);
		
		return layout;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		
		if (mDualPane)
			mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		
		getLoaderManager().initLoader(0, null, mLoaderCallbacks);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		if (mAccount != null)
		{
			outState.putSerializable("account", mAccount);
			outState.putLong("currentId", mTitleId);
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		
		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo)menuInfo;
		
		ViewHolder vh = (ViewHolder)acmi.targetView.getTag();
		menu.setHeaderTitle(vh.title.getText());
		
		getActivity().getMenuInflater().inflate(R.menu.xbl_game_list_context, 
				menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem menuItem)
	{
		AdapterView.AdapterContextMenuInfo info = 
			(AdapterView.AdapterContextMenuInfo)menuItem.getMenuInfo();
		
		if (info.targetView.getTag() instanceof ViewHolder)
		{
			ViewHolder vh = (ViewHolder)info.targetView.getTag();
			
			switch (menuItem.getItemId())
			{
			case R.id.menu_game_overview:
				
				GameOverview.actionShow(getActivity(), mAccount, vh.gameUrl);
				return true;
				
			case R.id.google_achievements:
				
				Intent searchIntent = new Intent(Intent.ACTION_WEB_SEARCH);
				searchIntent.putExtra(SearchManager.QUERY, 
						getString(R.string.google_achievements_f, 
								vh.title.getText()));
				startActivity(searchIntent);
				
				return true;
				
			case R.id.menu_visit_webpage:
				
				Intent wwwIntent = new Intent(Intent.ACTION_VIEW, 
						Uri.parse(vh.gameUrl));
				startActivity(wwwIntent);
				
				return true;
			}
		}
		
		return super.onContextItemSelected(menuItem);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
	    super.onCreateOptionsMenu(menu, inflater);
	    
    	inflater.inflate(R.menu.xbl_game_list, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
	    switch (item.getItemId()) 
	    {
	    case R.id.menu_refresh:
	    	synchronizeWithServer();
	    	return true;
	    }
	    
	    return false;
	}
	
	private void synchronizeWithServer()
	{
		mListView.setEmptyView(mProgress);
		mMessage.setVisibility(View.GONE);
		
		TaskController.getInstance().synchronizeGames(mAccount, mListener);
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		
		TaskController.getInstance().removeListener(mListener);
		TaskController.getInstance().removeListener(mBeaconListener);
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		TaskController.getInstance().addListener(mListener);
		TaskController.getInstance().addListener(mBeaconListener);
		
		if (System.currentTimeMillis() - mAccount.getLastGameUpdate() > mAccount.getGameHistoryRefreshInterval())
			synchronizeWithServer();
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id)
	{
		mTitleId = id;
		mListView.setItemChecked(pos, true);
		
		OnGameSelectedListener listener = (OnGameSelectedListener)getActivity();
		listener.onGameSelected(id);
	}
	
	@Override
	public void onImageReady(long id, Object param, Bitmap bmp)
	{
		super.onImageReady(id, param, bmp);
		
		if (getActivity() != null)
		{
			getActivity().getContentResolver().notifyChange(
					ContentUris.withAppendedId(Games.CONTENT_URI, id), null);
		}
	}
	
	@Override
    protected Cursor getIconCursor()
    {
		if (getActivity() == null)
			return null;
		
		ContentResolver cr = getActivity().getContentResolver();
		return cr.query(Games.CONTENT_URI,
				new String[] { Games._ID, Games.BOXART_URL },
				Games.ACCOUNT_ID + "=" + mAccount.getId(), 
				null, Games.DEFAULT_SORT_ORDER);
    }
	
	@Override
	public void beaconClicked(final long id, boolean isSet)
	{
		if (isSet)
		{
			// Beacon is currently set, so unset it
			
			TaskController.getInstance().runCustomTask(null, new CustomTask<Void>()
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
					TaskController.getInstance().runCustomTask(null, new CustomTask<Void>()
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
