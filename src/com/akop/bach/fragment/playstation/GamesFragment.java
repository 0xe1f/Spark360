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

package com.akop.bach.fragment.playstation;

import java.lang.ref.SoftReference;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.akop.bach.Account;
import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.CachePolicy;
import com.akop.bach.PSN.Games;
import com.akop.bach.PsnAccount;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.fragment.GenericFragment;
import com.akop.bach.parser.Parser;

public class GamesFragment extends GenericFragment implements
		OnItemClickListener
{
	public static interface OnGameSelectedListener
	{
		void onGameSelected(long id);
	}
	
	public static final String[] PROJ = new String[]
 	{ 
		Games._ID,
		Games.UID,
		Games.TITLE,
		Games.PROGRESS,
		Games.UNLOCKED_PLATINUM,
		Games.UNLOCKED_GOLD,
		Games.UNLOCKED_SILVER,
		Games.UNLOCKED_BRONZE,
		Games.ICON_URL,
 	};
 	
	public static final int COLUMN_TITLE = 2;
	public static final int COLUMN_PROGRESS = 3;
	public static final int COLUMN_UNLOCKED_PLATINUM = 4;
	public static final int COLUMN_UNLOCKED_GOLD = 5;
	public static final int COLUMN_UNLOCKED_SILVER = 6;
	public static final int COLUMN_UNLOCKED_BRONZE = 7;
	public static final int COLUMN_ICON_URL = 8;
	
	private class ViewHolder
	{
		public TextView title;
		public ImageView icon;
		public TextView trophiesPlat;
		public TextView trophiesGold;
		public TextView trophiesSilver;
		public TextView trophiesBronze;
		public TextView trophiesAll;
		public TextView progressValue;
		public ProgressBar progressBar;
	}
	
	private TaskListener mListener = new TaskListener("PsnGames")
	{
		@Override
		public void onTaskFailed(Account account, final Exception e)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					if (getActivity() != null && e != null)
						mMessage.setText(Parser.getErrorMessage(getActivity(), e));
					
					mListView.setEmptyView(mMessage);
					mProgress.setVisibility(View.GONE);
				}
			});
		}
		
		@Override
		public void onTaskSucceeded(Account account, Object requestParam, Object result)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					mMessage.setText(R.string.game_history_empty);
					
					mListView.setEmptyView(mMessage);
					mProgress.setVisibility(View.GONE);
					
					syncIcons();
				}
			});
		}
	};
	
	private class MyCursorAdapter extends CursorAdapter
	{
		public MyCursorAdapter(Context context, Cursor c)
		{
			super(context, c);
		}
		
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			LayoutInflater li = (LayoutInflater)context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			View view = li.inflate(R.layout.psn_game_list_item, parent, false);
			ViewHolder vh = new ViewHolder();
			
			view.setTag(vh);
			
			vh.icon = (ImageView)view.findViewById(R.id.game_icon);
			vh.title = (TextView)view.findViewById(R.id.game_title);
			
			vh.trophiesPlat = (TextView)view.findViewById(R.id.game_trophies_platinum);
			vh.trophiesGold = (TextView)view.findViewById(R.id.game_trophies_gold);
			vh.trophiesSilver = (TextView)view.findViewById(R.id.game_trophies_silver);
			vh.trophiesBronze = (TextView)view.findViewById(R.id.game_trophies_bronze);
			vh.trophiesAll = (TextView)view.findViewById(R.id.game_trophies_all);
			
			vh.progressValue = (TextView)view.findViewById(R.id.game_progress_ind);
			vh.progressBar = (ProgressBar)view.findViewById(R.id.game_progress_bar);
			
			return view;
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			ViewHolder vh = (ViewHolder)view.getTag();
			
			int platinum = cursor.getInt(COLUMN_UNLOCKED_PLATINUM);
			int gold = cursor.getInt(COLUMN_UNLOCKED_GOLD);
			int silver = cursor.getInt(COLUMN_UNLOCKED_SILVER);
			int bronze = cursor.getInt(COLUMN_UNLOCKED_BRONZE);
			int progress = cursor.getInt(COLUMN_PROGRESS);
			
			vh.title.setText(cursor.getString(COLUMN_TITLE));
			
			vh.trophiesPlat.setText(platinum + "");
			vh.trophiesGold.setText(gold + "");
			vh.trophiesSilver.setText(silver + "");
			vh.trophiesBronze.setText(bronze + "");
			vh.trophiesAll.setText((platinum + gold + silver + bronze) + "");
			
			vh.progressValue.setText(progress + "");
			vh.progressBar.setProgress(progress);
			
			String iconUrl = cursor.getString(COLUMN_ICON_URL);
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
				Bitmap bmp = ImageCache.getInstance().getCachedBitmap(iconUrl, mCp);
				if (bmp != null)
				{
					mIconCache.put(iconUrl, new SoftReference<Bitmap>(bmp));
					vh.icon.setImageBitmap(bmp);
				}
				else
				{
					// Image failed to load - just use placeholder
					vh.icon.setImageResource(R.drawable.psn_game_default);
				}
			}
		}
	}
	
	private LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderCallbacks<Cursor>()
	{
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args)
		{
			String criteria = Games.ACCOUNT_ID + "=" + mAccount.getId();
			/*
			if (!mAccount.isShowingApps())
				criteria += " AND " + Games.ACHIEVEMENTS_TOTAL + "> 0"; 
			*/
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
	
	private CachePolicy mCp = null;
	private CursorAdapter mAdapter = null;
	private ListView mListView = null;
	private TextView mMessage = null;
	private View mProgress = null;
	
	private PsnAccount mAccount = null;
	private long mTitleId = -1;
	
	public static GamesFragment newInstance(PsnAccount account)
	{
		GamesFragment f = new GamesFragment();
		
		Bundle args = new Bundle();
		args.putParcelable("account", account);
		f.setArguments(args);
		
		return f;
	}
	
	@Override
	public void onCreate(Bundle state)
	{
	    super.onCreate(state);
	    
		mCp = new CachePolicy();
		mCp.resizeHeight = 96;
		
		if (mAccount == null)
		{
		    Bundle args = getArguments();
			ContentResolver cr = getActivity().getContentResolver();
		    
		    mAccount = (PsnAccount)args.getSerializable("account");
			mTitleId = getFirstTitleId(cr.query(Games.CONTENT_URI,
					new String[] { Games._ID, },
					Games.ACCOUNT_ID + "=" + mAccount.getId(), 
					null, Games.DEFAULT_SORT_ORDER));
		}
		
	    if (state != null && state.containsKey("account"))
	    {
			mAccount = (PsnAccount)state.getSerializable("account");
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
		
		View layout = inflater.inflate(R.layout.psn_fragment_plain_list,
				container, false);
		
		mAdapter = new MyCursorAdapter(getActivity(), null);
		
		mMessage = (TextView)layout.findViewById(R.id.message);
		mMessage.setText(R.string.game_history_empty);
		
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
			outState.putParcelable("account", mAccount);
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
		
		getActivity().getMenuInflater().inflate(R.menu.psn_game_list_context, menu);
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
			case R.id.menu_google_trophies:
				Intent searchIntent = new Intent(Intent.ACTION_WEB_SEARCH);
				searchIntent.putExtra(SearchManager.QUERY, 
						getString(R.string.google_trophies_f, vh.title.getText()));
				
				startActivity(searchIntent);
				return true;
			}
		}
		
		return super.onContextItemSelected(menuItem);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
	    super.onCreateOptionsMenu(menu, inflater);
	    
	    inflater.inflate(R.menu.psn_game_list, menu);
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
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		TaskController.getInstance().addListener(mListener);
		
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
	protected CachePolicy getCachePolicy()
	{
		return mCp;
	}
	
	@Override
    protected Cursor getIconCursor()
    {
		if (getActivity() == null)
			return null;
		
		ContentResolver cr = getActivity().getContentResolver();
		return cr.query(Games.CONTENT_URI,
				new String[] { Games._ID, Games.ICON_URL },
				Games.ACCOUNT_ID + "=" + mAccount.getId(), 
				null, Games.DEFAULT_SORT_ORDER);
    }
}
