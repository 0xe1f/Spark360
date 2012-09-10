/*
 * CompareGamesFragment.java 
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

import java.lang.ref.SoftReference;
import java.util.HashMap;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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

import com.akop.bach.Account;
import com.akop.bach.App;
import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.OnImageReadyListener;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.XboxLive.ComparedGameCursor;
import com.akop.bach.XboxLive.ComparedGameInfo;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.activity.xboxlive.GameOverview;
import com.akop.bach.fragment.GenericFragment;
import com.akop.bach.parser.XboxLiveParser;

public class CompareGamesFragment extends GenericFragment implements
		OnItemClickListener
{
	public static interface OnGameSelectedListener
	{
		void onGameSelected(String yourGamerpicUrl, HashMap<Integer, Object> gameInfo);
	}
	
	private MyCursorAdapter mAdapter = null;
	private IconCursor mIconCursor = null;
	private ComparedGameInfo mPayload;
	private Handler mHandler = new Handler();
	private XboxLiveAccount mAccount = null;
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
	
	private TaskListener mListener = new TaskListener("XboxCompareGames")
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
						mMessage.setText(XboxLiveParser.getErrorMessage(getActivity(), e));
					
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
		public TextView myAchStats;
		public TextView myPointStats;
		public TextView yourAchStats;
		public TextView yourPointStats;
		public ImageView myGamerpic;
		public ImageView yourGamerpic;
		public ImageView boxart;
		public String gameUrl;
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
			View view = li.inflate(R.layout.xbl_compared_game_list_item, 
					parent, false);
			view.setTag(vh);
			
			vh.title = (TextView)view.findViewById(R.id.game_title);
			vh.myAchStats = (TextView)view.findViewById(R.id.me_achievements_unlocked);
			vh.yourAchStats = (TextView)view.findViewById(R.id.you_achievements_unlocked);
			vh.myPointStats = (TextView)view.findViewById(R.id.me_gp_earned);
			vh.yourPointStats = (TextView)view.findViewById(R.id.you_gp_earned);
			vh.myGamerpic = (ImageView)view.findViewById(R.id.me_gamerpic);
			vh.yourGamerpic = (ImageView)view.findViewById(R.id.you_gamerpic);
			vh.boxart = (ImageView)view.findViewById(R.id.game_icon);
			
			return view;
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			ViewHolder vh = (ViewHolder)view.getTag();
			
			vh.title.setText(cursor.getString(ComparedGameCursor.COLUMN_TITLE));
			vh.myAchStats.setText(getString(R.string.x_of_x_f, 
					cursor.getInt(ComparedGameCursor.COLUMN_MY_ACH_UNLOCKED), 
					cursor.getInt(ComparedGameCursor.COLUMN_ACH_TOTAL)));
			vh.yourAchStats.setText(getString(R.string.x_of_x_f, 
					cursor.getInt(ComparedGameCursor.COLUMN_YOUR_ACH_UNLOCKED), 
					cursor.getInt(ComparedGameCursor.COLUMN_ACH_TOTAL)));
			vh.myPointStats.setText(getString(R.string.x_of_x_f, 
					cursor.getInt(ComparedGameCursor.COLUMN_MY_GP_EARNED), 
					cursor.getInt(ComparedGameCursor.COLUMN_GP_TOTAL)));
			vh.yourPointStats.setText(getString(R.string.x_of_x_f, 
					cursor.getInt(ComparedGameCursor.COLUMN_YOUR_GP_EARNED), 
					cursor.getInt(ComparedGameCursor.COLUMN_GP_TOTAL)));
			vh.gameUrl = cursor.getString(ComparedGameCursor.COLUMN_GAME_URL);
			
			vh.myGamerpic.setImageBitmap(mMyGamerpic);
			vh.yourGamerpic.setImageBitmap(mYourGamerpic);
			
			String iconUrl = cursor.getString(ComparedGameCursor.COLUMN_BOXART_URL);
			SoftReference<Bitmap> icon = mIconCache.get(iconUrl);
			
			if (icon != null && icon.get() != null)
			{
				// Image is in the in-memory cache
				vh.boxart.setImageBitmap(icon.get());
			}
			else
			{
				// Image has likely been garbage-collected
				// Load it into the cache again
				Bitmap bmp = ImageCache.getInstance().getCachedBitmap(iconUrl);
				if (bmp != null)
				{
					mIconCache.put(iconUrl, new SoftReference<Bitmap>(bmp));
					vh.boxart.setImageBitmap(bmp);
				}
				else
				{
					// Image failed to load - just use placeholder
					vh.boxart.setImageResource(R.drawable.xbox_game_default);
				}
			}
		}
	}
	
	public static CompareGamesFragment newInstance(XboxLiveAccount account, String gamertag)
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
	    
	    Bundle args = getArguments();
	    
	    mAccount = (XboxLiveAccount)args.getSerializable("account");
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
		
		View layout = inflater.inflate(R.layout.xbl_fragment_plain_list,
				container, false);
		
		mMessage = (TextView)layout.findViewById(R.id.message);
		mMessage.setText(R.string.no_games_to_compare);
		
		mListView = (ListView)layout.findViewById(R.id.list);
		mListView.setOnItemClickListener(this);
		mListView.setEmptyView(mMessage);
		
		initializeAdapter();
		
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
				
				gameInfo.put(ComparedGameCursor.COLUMN_UID, c.getString(ComparedGameCursor.COLUMN_UID));
				gameInfo.put(ComparedGameCursor.COLUMN_TITLE, c.getString(ComparedGameCursor.COLUMN_TITLE));
				gameInfo.put(ComparedGameCursor.COLUMN_BOXART_URL, c.getString(ComparedGameCursor.COLUMN_BOXART_URL));
				gameInfo.put(ComparedGameCursor.COLUMN_GAME_URL, c.getString(ComparedGameCursor.COLUMN_GAME_URL));
				gameInfo.put(ComparedGameCursor.COLUMN_MY_ACH_UNLOCKED, c.getInt(ComparedGameCursor.COLUMN_MY_ACH_UNLOCKED));
				gameInfo.put(ComparedGameCursor.COLUMN_YOUR_ACH_UNLOCKED, c.getInt(ComparedGameCursor.COLUMN_YOUR_ACH_UNLOCKED));
				gameInfo.put(ComparedGameCursor.COLUMN_MY_GP_EARNED, c.getInt(ComparedGameCursor.COLUMN_MY_GP_EARNED));
				gameInfo.put(ComparedGameCursor.COLUMN_YOUR_GP_EARNED, c.getInt(ComparedGameCursor.COLUMN_YOUR_GP_EARNED));
				gameInfo.put(ComparedGameCursor.COLUMN_ACH_TOTAL, c.getInt(ComparedGameCursor.COLUMN_ACH_TOTAL));
				gameInfo.put(ComparedGameCursor.COLUMN_GP_TOTAL, c.getInt(ComparedGameCursor.COLUMN_GP_TOTAL));
				
				listener.onGameSelected(mPayload.yourAvatarIconUrl, gameInfo);
			}
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
		
		getActivity().getMenuInflater().inflate(R.menu.xbl_compare_game_list_context, 
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
			}
		}
		
		return super.onContextItemSelected(menuItem);
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
							.add(payloadCursor.getString(ComparedGameCursor.COLUMN_BOXART_URL));
					}
				}
				catch(Exception e)
				{
					if (App.LOGV)
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
				if ((mMyGamerpic = ic.getCachedBitmap(mPayload.myAvatarIconUrl)) == null)
					ic.requestImage(mPayload.myAvatarIconUrl, mGamerpicListener, 0, ICON_MINE);
			}
			
			if (mPayload.yourAvatarIconUrl != null && mYourGamerpic == null)
			{
				if ((mYourGamerpic = ic.getCachedBitmap(mPayload.yourAvatarIconUrl)) == null)
					ic.requestImage(mPayload.yourAvatarIconUrl, mGamerpicListener, 0, ICON_YOURS);
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
    protected Cursor getIconCursor()
    {
		if (getActivity() == null)
			return null;
		
		return mIconCursor;
    }
}
