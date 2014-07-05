/*
 * CompareGamesFragment.java 
 * Copyright (C) 2010-2014 Akop Karapetyan
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
import java.util.HashMap;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.akop.bach.Account;
import com.akop.bach.App;
import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.CachePolicy;
import com.akop.bach.ImageCache.OnImageReadyListener;
import com.akop.bach.PSN.ComparedGameCursor;
import com.akop.bach.PSN.ComparedGameInfo;
import com.akop.bach.PsnAccount;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.fragment.GenericFragment;
import com.akop.bach.parser.Parser;

public class CompareGamesFragment extends GenericFragment implements
		OnItemClickListener
{
	public static interface OnGameSelectedListener
	{
		void onGameSelected(String yourGamerpicUrl, HashMap<Integer, Object> gameInfo);
	}
	
	private CachePolicy mCp = null;
	private MyCursorAdapter mAdapter = null;
	private IconCursor mIconCursor = null;
	private ComparedGameInfo mPayload;
	private PsnAccount mAccount = null;
	private long mTitleId = -1;
	private String mGamertag;
	private ListView mListView = null;
	private TextView mMessage = null;
	private View mProgress = null;
	
	private Bitmap mMyGamerpic;
	private Bitmap mYourGamerpic;
	
	private static final int ICON_MINE  = 0;
	private static final int ICON_YOURS = 1;
	
	private OnImageReadyListener mGamerpicListener = new OnImageReadyListener()
	{
		@Override
        public void onImageReady(long id, Object param, Bitmap bmp)
        {
			if ((Integer)param == ICON_MINE)
				mMyGamerpic = bmp;
			else if ((Integer)param == ICON_YOURS)
				mYourGamerpic = bmp;
			
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					if (mAdapter != null)
						mAdapter.notifyDataSetChanged();
				}
			});
        }
	};
	
	private TaskListener mListener = new TaskListener("PsnCompareGames")
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
		public void onTaskSucceeded(Account account, Object requestParam, final Object result)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					if (result != null && result instanceof ComparedGameInfo)
					{
						mPayload = (ComparedGameInfo)result;
						
						initializeAdapter();
						synchronizeLocal();
					}
					
					mMessage.setText(getString(R.string.no_games_to_compare));
					
					mListView.setEmptyView(mMessage);
					mProgress.setVisibility(View.GONE);
				}
			});
		}
	};
	
	private static class ViewHolder
	{
		public TextView title;
		public ImageView gameIcon;
		
		public View selfSection;
		public TextView selfTrophiesPlat;
		public TextView selfTrophiesGold;
		public TextView selfTrophiesSilver;
		public TextView selfTrophiesBronze;
		public TextView selfTrophiesAll;
		public TextView selfProgress;
		public TextView selfNotPlayed;
		
		public View oppSection;
		public TextView oppTrophiesPlat;
		public TextView oppTrophiesGold;
		public TextView oppTrophiesSilver;
		public TextView oppTrophiesBronze;
		public TextView oppTrophiesAll;
		public TextView oppProgress;
		public TextView oppNotPlayed;
		
		public ImageView selfIcon;
		public ImageView oppIcon;
	}
	
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
			
			ViewHolder vh = new ViewHolder();
			View view = li.inflate(R.layout.psn_compare_game_list_item, 
					parent, false);
			view.setTag(vh);
			
			vh.title = (TextView)view.findViewById(R.id.game_title);
			vh.gameIcon = (ImageView)view.findViewById(R.id.game_icon);
			
			vh.selfSection = view.findViewById(R.id.self_section);
			vh.selfTrophiesPlat = (TextView)view.findViewById(R.id.self_trophies_platinum);
			vh.selfTrophiesGold = (TextView)view.findViewById(R.id.self_trophies_gold);
			vh.selfTrophiesSilver = (TextView)view.findViewById(R.id.self_trophies_silver);
			vh.selfTrophiesBronze = (TextView)view.findViewById(R.id.self_trophies_bronze);
			vh.selfTrophiesAll = (TextView)view.findViewById(R.id.self_trophies_all);
			vh.selfProgress = (TextView)view.findViewById(R.id.self_progress);
			vh.selfNotPlayed = (TextView)view.findViewById(R.id.self_not_played);
			
			vh.oppSection = view.findViewById(R.id.opp_section);
			vh.oppTrophiesPlat = (TextView)view.findViewById(R.id.opp_trophies_platinum);
			vh.oppTrophiesGold = (TextView)view.findViewById(R.id.opp_trophies_gold);
			vh.oppTrophiesSilver = (TextView)view.findViewById(R.id.opp_trophies_silver);
			vh.oppTrophiesBronze = (TextView)view.findViewById(R.id.opp_trophies_bronze);
			vh.oppTrophiesAll = (TextView)view.findViewById(R.id.opp_trophies_all);
			vh.oppProgress = (TextView)view.findViewById(R.id.opp_progress);
			vh.oppNotPlayed = (TextView)view.findViewById(R.id.opp_not_played);
			
			vh.selfIcon = (ImageView)view.findViewById(R.id.self_icon);
			vh.oppIcon = (ImageView)view.findViewById(R.id.opp_icon);
			
			return view;
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			ViewHolder vh = (ViewHolder)view.getTag();
			
			vh.title.setText(cursor.getString(ComparedGameCursor.COLUMN_TITLE));
			
			int selfPlatinum = cursor.getInt(ComparedGameCursor.COLUMN_SELF_PLATINUM);
			int selfGold = cursor.getInt(ComparedGameCursor.COLUMN_SELF_GOLD);
			int selfSilver = cursor.getInt(ComparedGameCursor.COLUMN_SELF_SILVER);
			int selfBronze = cursor.getInt(ComparedGameCursor.COLUMN_SELF_BRONZE);
			int selfAll = selfPlatinum + selfGold + selfSilver + selfBronze;
			
			int oppPlatinum = cursor.getInt(ComparedGameCursor.COLUMN_OPP_PLATINUM);
			int oppGold = cursor.getInt(ComparedGameCursor.COLUMN_OPP_GOLD);
			int oppSilver = cursor.getInt(ComparedGameCursor.COLUMN_OPP_SILVER);
			int oppBronze = cursor.getInt(ComparedGameCursor.COLUMN_OPP_BRONZE);
			int oppAll = oppPlatinum + oppGold + oppSilver + oppBronze;
			
			vh.selfTrophiesPlat.setText(String.valueOf(selfPlatinum));
			vh.selfTrophiesGold.setText(String.valueOf(selfGold));
			vh.selfTrophiesSilver.setText(String.valueOf(selfSilver));
			vh.selfTrophiesBronze.setText(String.valueOf(selfBronze));
			vh.selfTrophiesAll.setText(String.valueOf(selfAll));
			
			vh.oppTrophiesPlat.setText(String.valueOf(oppPlatinum));
			vh.oppTrophiesGold.setText(String.valueOf(oppGold));
			vh.oppTrophiesSilver.setText(String.valueOf(oppSilver));
			vh.oppTrophiesBronze.setText(String.valueOf(oppBronze));
			vh.oppTrophiesAll.setText(String.valueOf(oppAll));
			
			if (cursor.getInt(ComparedGameCursor.COLUMN_SELF_PLAYED) != 0)
			{
				vh.selfNotPlayed.setVisibility(View.GONE);
				vh.selfSection.setVisibility(View.VISIBLE);
				vh.selfProgress.setText(getString(R.string.compare_game_progress_f,
						cursor.getInt(ComparedGameCursor.COLUMN_SELF_PROGRESS)));
			}
			else
			{
				vh.selfSection.setVisibility(View.GONE);
				vh.selfNotPlayed.setVisibility(View.VISIBLE);
				//vh.selfProgress.setText(getString(R.string.no_progress));
			}
			
			if (cursor.getInt(ComparedGameCursor.COLUMN_OPP_PLAYED) != 0)
			{
				vh.oppNotPlayed.setVisibility(View.GONE);
				vh.oppSection.setVisibility(View.VISIBLE);
				vh.oppProgress.setText(getString(R.string.compare_game_progress_f,
						cursor.getInt(ComparedGameCursor.COLUMN_OPP_PROGRESS)));
			}
			else
			{
				vh.oppNotPlayed.setVisibility(View.VISIBLE);
				vh.oppSection.setVisibility(View.GONE);
				vh.oppProgress.setText(getString(R.string.no_progress));
			}
			
			vh.selfIcon.setImageBitmap(mMyGamerpic);
			vh.oppIcon.setImageBitmap(mYourGamerpic);
			
			String iconUrl = cursor.getString(ComparedGameCursor.COLUMN_ICON_URL);
			SoftReference<Bitmap> icon = mIconCache.get(iconUrl);
			
			if (icon != null && icon.get() != null)
			{
				// Image is in the in-memory cache
				vh.gameIcon.setImageBitmap(icon.get());
			}
			else
			{
				// Image has likely been garbage-collected
				// Load it into the cache again
				Bitmap bmp = ImageCache.getInstance().getCachedBitmap(iconUrl, mCp);
				if (bmp != null)
				{
					mIconCache.put(iconUrl, new SoftReference<Bitmap>(bmp));
					vh.gameIcon.setImageBitmap(bmp);
				}
				else
				{
					// Image failed to load - just use placeholder
					vh.gameIcon.setImageResource(R.drawable.psn_game_default);
				}
			}
		}
	}
	
	public static CompareGamesFragment newInstance(PsnAccount account, String gamertag)
	{
		CompareGamesFragment f = new CompareGamesFragment();
		
		Bundle args = new Bundle();
		args.putParcelable("account", account);
		args.putString("gamertag", gamertag);
		f.setArguments(args);
		
		return f;
	}
	
    @Override
	public void onCreate(Bundle state)
	{
	    super.onCreate(state);
	    
		mCp = new CachePolicy();
		mCp.resizeHeight = 96;
		
	    Bundle args = getArguments();
	    
	    mAccount = (PsnAccount)args.getParcelable("account");
	    mGamertag = args.getString("gamertag");
	    mIconCursor = null;
	    mPayload = null;
	    mAdapter = null;
		mTitleId = -1;
		
		mMyGamerpic = null;
		mYourGamerpic = null;
		
	    if (state != null)
	    {
	    	try
	    	{
				mTitleId = state.getLong("titleId");
				
				if (state.containsKey("payload"))
					mPayload = (ComparedGameInfo)state.getSerializable("payload");
				
				if (state.containsKey("icons"))
					mIconCursor = (IconCursor)state.getSerializable("icons");
	    	}
	    	catch(Exception e)
	    	{
	    		mTitleId = -1;
	    		mPayload = null;
	    		mIconCursor = null;
	    	}
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
		
		mMessage = (TextView)layout.findViewById(R.id.message);
		mMessage.setText(R.string.no_games_to_compare);
		
		mListView = (ListView)layout.findViewById(R.id.list);
		mListView.setOnItemClickListener(this);
		mListView.setEmptyView(mMessage);
		
		initializeAdapter();
		
		mProgress = layout.findViewById(R.id.loading);
		
		return layout;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		
		if (mDualPane)
			mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		if (mAccount != null)
		{
			outState.putParcelable("account", mAccount);
			outState.putString("gamertag", mGamertag);
			outState.putLong("currentId", mTitleId);
			
			if (mPayload != null)
				outState.putSerializable("payload", mPayload);
			if (mIconCursor != null)
				outState.putSerializable("icons", mIconCursor);
		}
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
	
	@Override
	public void onPause()
	{
		super.onPause();
		
		TaskController.getInstance().removeListener(mListener);
		ImageCache.getInstance().removeListener(mGamerpicListener);
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		TaskController.getInstance().addListener(mListener);
		ImageCache.getInstance().addListener(mGamerpicListener);
        
		synchronizeLocal();
		
		if (mPayload == null)
			synchronizeWithServer();
	}
	
	@Override
	public void onImageReady(final long id, Object param, Bitmap bmp)
	{
		super.onImageReady(id, param, bmp);
		
		if (mAdapter != null)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					Uri uri = ContentUris.withAppendedId(ComparedGameCursor.CONTENT_URI, id);
					getActivity().getContentResolver().notifyChange(uri, null);
				}
			});
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id)
	{
		mTitleId = id;
		mListView.setItemChecked(pos, true);
		
		if (getActivity() instanceof OnGameSelectedListener)
		{
			OnGameSelectedListener listener = (OnGameSelectedListener)getActivity();
			Cursor c = (Cursor)arg0.getItemAtPosition(pos);
			
			if (c != null)
			{
				HashMap<Integer, Object> gameInfo = new HashMap<Integer, Object>();
				
				gameInfo.put(ComparedGameCursor.COLUMN_UID, 
						c.getString(ComparedGameCursor.COLUMN_UID));
				gameInfo.put(ComparedGameCursor.COLUMN_TITLE, 
						c.getString(ComparedGameCursor.COLUMN_TITLE));
				gameInfo.put(ComparedGameCursor.COLUMN_ICON_URL, 
						c.getString(ComparedGameCursor.COLUMN_ICON_URL));
				
				gameInfo.put(ComparedGameCursor.COLUMN_SELF_PLAYED, 
						c.getInt(ComparedGameCursor.COLUMN_SELF_PLAYED));
				gameInfo.put(ComparedGameCursor.COLUMN_SELF_PLATINUM, 
						c.getInt(ComparedGameCursor.COLUMN_SELF_PLATINUM));
				gameInfo.put(ComparedGameCursor.COLUMN_SELF_GOLD, 
						c.getInt(ComparedGameCursor.COLUMN_SELF_GOLD));
				gameInfo.put(ComparedGameCursor.COLUMN_SELF_SILVER, 
						c.getInt(ComparedGameCursor.COLUMN_SELF_SILVER));
				gameInfo.put(ComparedGameCursor.COLUMN_SELF_BRONZE, 
						c.getInt(ComparedGameCursor.COLUMN_SELF_BRONZE));
				gameInfo.put(ComparedGameCursor.COLUMN_SELF_PROGRESS, 
						c.getInt(ComparedGameCursor.COLUMN_SELF_PROGRESS));
				
				gameInfo.put(ComparedGameCursor.COLUMN_OPP_PLAYED, 
						c.getInt(ComparedGameCursor.COLUMN_OPP_PLAYED));
				gameInfo.put(ComparedGameCursor.COLUMN_OPP_PLATINUM, 
						c.getInt(ComparedGameCursor.COLUMN_OPP_PLATINUM));
				gameInfo.put(ComparedGameCursor.COLUMN_OPP_GOLD, 
						c.getInt(ComparedGameCursor.COLUMN_OPP_GOLD));
				gameInfo.put(ComparedGameCursor.COLUMN_OPP_SILVER, 
						c.getInt(ComparedGameCursor.COLUMN_OPP_SILVER));
				gameInfo.put(ComparedGameCursor.COLUMN_OPP_BRONZE, 
						c.getInt(ComparedGameCursor.COLUMN_OPP_BRONZE));
				gameInfo.put(ComparedGameCursor.COLUMN_OPP_PROGRESS, 
						c.getInt(ComparedGameCursor.COLUMN_OPP_PROGRESS));
				
				listener.onGameSelected(mPayload.yourAvatarIconUrl, gameInfo);
			}
		}
	}
	
	private void initializeAdapter()
	{
		synchronized (this)
		{
			if (mPayload != null)
			{
				Cursor payloadCursor = mPayload.cursor;
				IconCursor ic = new IconCursor();
				
				try
				{
					while (payloadCursor.moveToNext())
					{
						ic.newRow()
							.add(payloadCursor.getLong(0))
							.add(payloadCursor.getString(ComparedGameCursor.COLUMN_ICON_URL));
					}
				}
				catch(Exception e)
				{
					if (App.getConfig().logToConsole())
						e.printStackTrace();
				}
				
				mIconCursor = ic;
				mAdapter = new MyCursorAdapter(getActivity(), payloadCursor);
				mListView.setAdapter(mAdapter);
			}
		}
	}
	
	private void synchronizeLocal()
	{
		// Load gamerpics
		
		ImageCache ic = ImageCache.getInstance();
		if (mPayload != null)
		{
			if (mPayload.myAvatarIconUrl != null && mMyGamerpic == null)
			{
				if ((mMyGamerpic = ic.getCachedBitmap(mPayload.myAvatarIconUrl, mCp)) == null)
					ic.requestImage(mPayload.myAvatarIconUrl, mGamerpicListener, 0, ICON_MINE, false, mCp);
			}
			
			if (mPayload.yourAvatarIconUrl != null && mYourGamerpic == null)
			{
				if ((mYourGamerpic = ic.getCachedBitmap(mPayload.yourAvatarIconUrl, mCp)) == null)
					ic.requestImage(mPayload.yourAvatarIconUrl, mGamerpicListener, 0, ICON_YOURS, false, mCp);
			}
		}
		
		syncIcons();
	}
	
	private void synchronizeWithServer()
	{
		mListView.setEmptyView(mProgress);
		mMessage.setVisibility(View.GONE);
		
		TaskController.getInstance().compareGames(mAccount, mGamertag, mListener);
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
		
		return mIconCursor;
    }
}
