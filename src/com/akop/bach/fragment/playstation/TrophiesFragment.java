/*
 * TrophiesFragment.java 
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
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.akop.bach.App;
import com.akop.bach.IAccount;
import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.CachePolicy;
import com.akop.bach.ImageCache.OnImageReadyListener;
import com.akop.bach.PSN;
import com.akop.bach.PSN.Games;
import com.akop.bach.PSN.Trophies;
import com.akop.bach.PsnAccount;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.fragment.GenericFragment;
import com.akop.bach.parser.Parser;

public class TrophiesFragment extends GenericFragment implements
		OnItemClickListener
{
	private static final int[] TROPHY_RESOURCES = 
	{
		0,
		R.drawable.psn_trophy_bronze,
		R.drawable.psn_trophy_silver,
		R.drawable.psn_trophy_gold,
		R.drawable.psn_trophy_platinum, 
	};
	
	private static final String[] ACH_PROJ = new String[]
	{ 
		Trophies._ID,
		Trophies.TITLE,
		Trophies.DESCRIPTION,
		Trophies.EARNED,
		Trophies.EARNED_TEXT,
		Trophies.IS_SECRET,
		Trophies.TYPE,
		Trophies.ICON_URL,
	};
	
	private static final int COLUMN_TITLE = 1;
	private static final int COLUMN_DESCRIPTION = 2;
	private static final int COLUMN_EARNED = 3;
	private static final int COLUMN_EARNED_TEXT = 4;
	//private static final int COLUMN_IS_SECRET = 5;
	private static final int COLUMN_TYPE = 6;
	private static final int COLUMN_ICON_URL = 7;
	
	private class ViewHolder
	{
		public TextView title;
		public TextView description;
		public TextView earned;
		public ImageView typeIcon;
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
			if (mTitleId > 0 && Games.isDirty(getActivity(), mTitleId))
			{
				if (App.LOGV)
					App.logv("Game is Dirty - updating achieves");
				
				// It's dirty; refresh the achievements
				synchronizeWithServer();
			}
			
			synchronizeLocal();
		}
    };
    
	private TaskListener mListener = new TaskListener("PsnTrophies")
	{
		@Override
		public void onTaskFailed(IAccount account, final Exception e)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					mMessage.setText(Parser.getErrorMessage(getActivity(), e));
					
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
					mMessage.setText(R.string.trophies_none);
					
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
			LayoutInflater li = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			View view = li.inflate(R.layout.psn_trophy_list_item, parent, false);

			ViewHolder vh = new ViewHolder();
			view.setTag(vh);
			
			vh.title = (TextView)view.findViewById(R.id.trophy_title);
			vh.description = (TextView)view.findViewById(R.id.trophy_description);
			vh.earned = (TextView)view.findViewById(R.id.trophy_earned);
			vh.typeIcon = (ImageView)view.findViewById(R.id.trophy_type);
			vh.icon = (ImageView)view.findViewById(R.id.trophy_icon);
			
			return view;
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			ViewHolder vh = (ViewHolder) view.getTag();

			vh.title.setText(cursor.getString(COLUMN_TITLE));
			vh.description.setText(cursor.getString(COLUMN_DESCRIPTION));
			
			int type = cursor.getInt(COLUMN_TYPE);
			String earnedText = cursor.getString(COLUMN_EARNED_TEXT);
			long earned = cursor.getLong(COLUMN_EARNED);
			
			if (type == 0)
			{
				vh.typeIcon.setVisibility(View.GONE);
			}
			else
			{
				vh.typeIcon.setVisibility(View.VISIBLE);
				vh.typeIcon.setImageResource(TROPHY_RESOURCES[type]);
			}
			
			if (earned == 0 && earnedText == null)
			{
				vh.icon.setImageResource(R.drawable.psn_trophy_locked);
			}
			else
			{
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
						vh.icon.setImageResource(R.drawable.psn_trophy_default);
					}
				}
			}
			
			if (earnedText == null)
				earnedText = PSN.getTrophyUnlockString(getActivity(), earned);
			
			vh.earned.setText(earnedText);
		}
	}
	
	private LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderCallbacks<Cursor>()
	{
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args)
		{
			return new CursorLoader(getActivity(), Trophies.CONTENT_URI,
					ACH_PROJ, Trophies.GAME_ID + "=" + mTitleId, 
					null, Trophies.DEFAULT_SORT_ORDER);
		}
		
		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data)
		{
			if (mAdapter != null)
				mAdapter.changeCursor(data);
			
			if (data.getCount() < 1)
				mMessage.setText(R.string.trophies_none);
		}
		
		@Override
		public void onLoaderReset(Loader<Cursor> arg0)
		{
			if (mAdapter != null)
				mAdapter.changeCursor(null);
		}
	};
	
	private Handler mHandler = new Handler();
	
	private PsnAccount mAccount;
	private long mTitleId = -1;
	
	private CachePolicy mCp = null;
	private CursorAdapter mAdapter = null;
	private ListView mListView = null;
	private TextView mMessage = null;
	private View mProgress = null;
	private String mGameTitle = null;
	private boolean mShowGameTotals = false;
	
	public static TrophiesFragment newInstance(PsnAccount account,
			boolean showGameTotals)
	{
		return newInstance(account, -1, showGameTotals);
	}
	
	public static TrophiesFragment newInstance(PsnAccount account,
			long titleId, boolean showGameTotals)
	{
		TrophiesFragment f = new TrophiesFragment();
		
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
		
		mCp = new CachePolicy();
		mCp.resizeHeight = 96;
		
		if (mAccount == null)
		{
		    Bundle args = getArguments();
		    
		    mAccount = (PsnAccount)args.getSerializable("account");
		    mTitleId = args.getLong("titleId", -1);
		}
		
	    if (state != null)
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
		
		View layout = inflater.inflate(R.layout.psn_fragment_trophy_list,
				container, false);
		
		mShowGameTotals = false;
		View gameDetails = layout.findViewById(R.id.game_details);
		
		if (gameDetails != null)
		{
			mShowGameTotals = getArguments().getBoolean("showGameTotals");
			
			gameDetails.setVisibility(mShowGameTotals ? View.VISIBLE : View.GONE);
			/*
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
				GameListItem view = (GameListItem)gameDetails.findViewById(R.id.gameListItem);
				view.mClickListener = this;
			}
			*/
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
				R.menu.psn_trophy_list_context, menu);
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
			case R.id.menu_google_trophies:
				Intent searchIntent = new Intent(Intent.ACTION_WEB_SEARCH);
				searchIntent.putExtra(SearchManager.QUERY, 
						getString(R.string.google_trophy_f, mGameTitle,
								vh.title.getText()));
				
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
		
		TaskController.getInstance().removeListener(mListener);
		
		ContentResolver cr = getActivity().getContentResolver();
        cr.unregisterContentObserver(mObserver);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		
		TaskController.getInstance().addListener(mListener);
		
		ContentResolver cr = getActivity().getContentResolver();
		cr.registerContentObserver(Games.CONTENT_URI, true, mObserver);
		
		synchronizeLocal();
	}
	
	@Override
	public void onImageReady(long id, Object param, Bitmap bmp)
	{
		super.onImageReady(id, param, bmp);
		
		getActivity().getContentResolver().notifyChange(
				ContentUris.withAppendedId(Trophies.CONTENT_URI, id), null);
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position,
			long arg3)
	{
		ViewHolder vh = (ViewHolder)arg1.getTag();
		showTrophyDetails(vh.title.getText(), vh.description.getText());
	}
	
	@Override
    protected Cursor getIconCursor()
    {
		if (getActivity() == null)
			return null;
		
		ContentResolver cr = getActivity().getContentResolver();
		return cr.query(Trophies.CONTENT_URI, 
				new String[] { Trophies._ID, Trophies.ICON_URL },
				Trophies.GAME_ID + "=" + mTitleId + " AND (" + 
				Trophies.EARNED + " != 0 OR " + 
				Trophies.EARNED_TEXT + " IS NOT NULL)", 
				null, Trophies.DEFAULT_SORT_ORDER);
    }
	
	private void showTrophyDetails(CharSequence title, CharSequence description)
	{
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View layout = inflater.inflate(R.layout.psn_trophy_toast,
				(ViewGroup)getActivity().findViewById(R.id.toast_root));
		
		TextView text = (TextView)layout.findViewById(R.id.trophy_title);
		text.setText(title);
		text = (TextView)layout.findViewById(R.id.trophy_description);
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
					new String[] { Games.TITLE, Games.TROPHIES_DIRTY }, 
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
		
		TaskController.getInstance().synchronizeAchievements(mAccount, mTitleId,
				mListener);
	}
	
	public void resetTitle(long id)
	{
		mTitleId = id;
		
		synchronizeLocal();
		syncIcons();
	}
	
	@Override
	protected CachePolicy getCachePolicy()
	{
		return mCp;
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
					int platinum = c.getInt(GamesFragment.COLUMN_UNLOCKED_PLATINUM);
					int gold = c.getInt(GamesFragment.COLUMN_UNLOCKED_GOLD);
					int silver = c.getInt(GamesFragment.COLUMN_UNLOCKED_SILVER);
					int bronze = c.getInt(GamesFragment.COLUMN_UNLOCKED_BRONZE);
					int progress = c.getInt(GamesFragment.COLUMN_PROGRESS);
					
					TextView tv;
					ImageView iv;
					ProgressBar pb;
					
					if ((tv = (TextView)view.findViewById(R.id.game_title)) != null)
						tv.setText(c.getString(GamesFragment.COLUMN_TITLE));
					if ((tv = (TextView)view.findViewById(R.id.game_progress_ind)) != null)
						tv.setText(progress + "");
					if ((pb = (ProgressBar)view.findViewById(R.id.game_progress_bar)) != null)
						pb.setProgress(progress);
					
					if ((tv = (TextView)view.findViewById(R.id.game_trophies_platinum)) != null)
						tv.setText(platinum + "");
					if ((tv = (TextView)view.findViewById(R.id.game_trophies_gold)) != null)
						tv.setText(gold + "");
					if ((tv = (TextView)view.findViewById(R.id.game_trophies_silver)) != null)
						tv.setText(silver + "");
					if ((tv = (TextView)view.findViewById(R.id.game_trophies_bronze)) != null)
						tv.setText(bronze + "");
					if ((tv = (TextView)view.findViewById(R.id.game_trophies_all)) != null)
						tv.setText((platinum + gold + silver + bronze) + "");
					
					ImageCache ic = ImageCache.getInstance();
					String iconUrl = c.getString(GamesFragment.COLUMN_ICON_URL);
					Bitmap bmp;
					
					if ((bmp = ic.getCachedBitmap(iconUrl, mCp)) != null)
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
							}, 0, null, true, mCp);
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
}
