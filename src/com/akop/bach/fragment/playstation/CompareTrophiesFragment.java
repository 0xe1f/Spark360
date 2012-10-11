/*
 * CompareTrophiesFragment.java 
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
import java.util.HashMap;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.akop.bach.Account;
import com.akop.bach.App;
import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.CachePolicy;
import com.akop.bach.ImageCache.OnImageReadyListener;
import com.akop.bach.PSN.ComparedGameCursor;
import com.akop.bach.PSN.ComparedTrophyCursor;
import com.akop.bach.PSN.ComparedTrophyInfo;
import com.akop.bach.PsnAccount;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.fragment.GenericFragment;
import com.akop.bach.parser.Parser;

public class CompareTrophiesFragment extends GenericFragment implements
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
	
	private static class ViewHolder
	{
		public TextView title;
		public TextView description;
		public TextView selfEarned;
		public TextView oppEarned;
		public ImageView selfIcon;
		public ImageView oppIcon;
		public ImageView icon;
		public ImageView typeIcon;
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
			View view = li.inflate(R.layout.psn_compare_trophy_list_item, 
					parent, false);
			view.setTag(vh);
			
			vh.title = (TextView)view.findViewById(R.id.trophy_title);
			vh.description = (TextView)view.findViewById(R.id.trophy_description);
			
			vh.selfEarned = (TextView)view.findViewById(R.id.trophy_self_earned);
			vh.oppEarned = (TextView)view.findViewById(R.id.trophy_opp_earned);
			
			vh.selfIcon = (ImageView)view.findViewById(R.id.trophy_self_icon);
			vh.oppIcon = (ImageView)view.findViewById(R.id.trophy_opp_icon);
			
			vh.icon = (ImageView)view.findViewById(R.id.trophy_icon);
			vh.typeIcon = (ImageView)view.findViewById(R.id.trophy_type);
			
			return view;
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			ViewHolder vh = (ViewHolder)view.getTag();
			
			vh.title.setText(cursor.getString(ComparedTrophyCursor.COLUMN_TITLE));
			vh.description.setText(cursor.getString(ComparedTrophyCursor.COLUMN_DESCRIPTION));
			
			vh.selfEarned.setText(cursor.getString(ComparedTrophyCursor.COLUMN_SELF_EARNED));
			vh.oppEarned.setText(cursor.getString(ComparedTrophyCursor.COLUMN_OPP_EARNED));
			
			vh.selfIcon.setImageBitmap(mMyGamerpic);
			vh.oppIcon.setImageBitmap(mYourGamerpic);
			
			int type = cursor.getInt(ComparedTrophyCursor.COLUMN_TYPE);
			
			if (type == 0)
			{
				vh.typeIcon.setVisibility(View.GONE);
			}
			else
			{
				vh.typeIcon.setVisibility(View.VISIBLE);
				vh.typeIcon.setImageResource(TROPHY_RESOURCES[type]);
			}
			
			if (cursor.getInt(ComparedTrophyCursor.COLUMN_IS_LOCKED) != 0)
			{
				vh.icon.setImageResource(R.drawable.psn_trophy_locked);
			}
			else
			{
				String iconUrl = cursor.getString(ComparedTrophyCursor.COLUMN_ICON_URL);
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
		}
	}
	
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
	
	private TaskListener mListener = new TaskListener("PsnCompareTrophies")
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
					
					if (App.getConfig().logToConsole())
						e.printStackTrace();
					
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
					if (result != null && result instanceof ComparedTrophyInfo)
					{
						mPayload = (ComparedTrophyInfo)result;
						
						initializeAdapter();
						synchronizeLocal();
					}
					
					mListView.setEmptyView(mMessage);
					mMessage.setText(getString(R.string.trophies_none));
					mProgress.setVisibility(View.GONE);
				}
			});
		}
	};
	
	private PsnAccount mAccount;
	private String mGamertag;
	private String mTitleUid;
	private String mMyGamerpicUrl;
	private String mYourGamerpicUrl;
	private HashMap<Integer, Object> mGameInfo;
	
	private ListView mListView = null;
	private TextView mMessage = null;
	private View mProgress = null;
	
	private CachePolicy mCp = null;
	private IconCursor mIconCursor = null;
	private ComparedTrophyInfo mPayload;
	private MyCursorAdapter mAdapter = null;
	private Bitmap mMyGamerpic;
	private Bitmap mYourGamerpic;
	
	public static CompareTrophiesFragment newInstance(PsnAccount account, 
			String gamertag, HashMap<Integer, Object> gameInfo,
			String yourGamerpicUrl)
	{
		CompareTrophiesFragment f = new CompareTrophiesFragment();
		
		Bundle args = new Bundle();
		args.putParcelable("account", account);
		args.putString("gamertag", gamertag);
		args.putSerializable("gameInfo", gameInfo);
		args.putString("yourGamerpicUrl", yourGamerpicUrl);
		f.setArguments(args);
		
		return f;
	}
	
	public static CompareTrophiesFragment newInstance(PsnAccount account, 
			String gamertag)
	{
		CompareTrophiesFragment f = new CompareTrophiesFragment();
		
		Bundle args = new Bundle();
		args.putParcelable("account", account);
		args.putString("gamertag", gamertag);
		f.setArguments(args);
		
		return f;
	}
	
    @SuppressWarnings("unchecked")
    @Override
	public void onCreate(Bundle state)
	{
	    super.onCreate(state);
	    
		mCp = new CachePolicy();
		mCp.resizeHeight = 96;
		
	    Bundle args = getArguments();
	    
	    mAccount = (PsnAccount)args.getParcelable("account");
	    mGamertag = args.getString("gamertag");
		mMyGamerpicUrl = mAccount.getIconUrl();
		mYourGamerpicUrl = args.getString("yourGamerpicUrl");
		mGameInfo = (HashMap<Integer, Object>)args.getSerializable("gameInfo");
		
	    mIconCursor = null;
	    mPayload = null;
	    
	    mAdapter = null;
		mMyGamerpic = null;
		mYourGamerpic = null;
	    mTitleUid = null;
		
	    if (state != null)
	    {
	    	try
	    	{
				if (state.containsKey("payload"))
					mPayload = (ComparedTrophyInfo)state.getSerializable("payload");
				
				if (state.containsKey("icons"))
					mIconCursor = (IconCursor)state.getSerializable("icons");
				
				if (state.containsKey("yourGamerpicUrl"))
					mYourGamerpicUrl = state.getString("yourGamerpicUrl");
	    	}
	    	catch(Exception e)
	    	{
	    		mPayload = null;
	    		mIconCursor = null;
	    	}
		}
	    
	    if (mGameInfo != null)
	    	mTitleUid = (String)mGameInfo.get(ComparedGameCursor.COLUMN_UID);
	    
	    setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		if (container == null)
			return null;
		
		View layout = inflater.inflate(R.layout.psn_fragment_compare_trophy_list,
				container, false);
		
		mMessage = (TextView)layout.findViewById(R.id.message);
		mMessage.setText(R.string.select_game_to_compare_trophies);
		
		mListView = (ListView)layout.findViewById(R.id.list);
		mListView.setOnItemClickListener(this);
		mListView.setEmptyView(mMessage);
		
		initializeAdapter();
		
		mProgress = layout.findViewById(R.id.loading);
		mProgress.setVisibility(View.GONE);
		
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
		
		if (mPayload != null)
			outState.putSerializable("payload", mPayload);
		if (mIconCursor != null)
			outState.putSerializable("icons", mIconCursor);
		if (mYourGamerpicUrl != null)
			outState.putString("yourGamerpicUrl", mYourGamerpicUrl);
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
		
		if (mTitleUid != null && mPayload == null)
			synchronizeWithServer();
	}
	
	@Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
    {
		ViewHolder vh = (ViewHolder)arg1.getTag();
		showAchievementDetails(vh.title.getText(), vh.description.getText());
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
					Uri uri = ContentUris.withAppendedId(ComparedTrophyCursor.CONTENT_URI, id);
					getActivity().getContentResolver().notifyChange(uri, null);
				}
			});
		}
	}
	
	private void showAchievementDetails(CharSequence title, CharSequence description)
	{
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View layout = inflater.inflate(R.layout.psn_trophy_toast,
				(ViewGroup)getActivity().findViewById(R.id.toast_root));
		
		TextView text = (TextView) layout.findViewById(R.id.trophy_title);
		text.setText(title);
		text = (TextView) layout.findViewById(R.id.trophy_description);
		text.setText(description);
		
		Toast toast = new Toast(getActivity());
		toast.setDuration(Toast.LENGTH_LONG);
		toast.setView(layout);
		
		toast.show();
	}
	
	public void resetTitle(String yourGamerpicUrl, HashMap<Integer, Object> gameInfo)
	{
		if (gameInfo == null)
			return;
		
		String titleUid = (String)gameInfo.get(ComparedGameCursor.COLUMN_UID);
		if (titleUid != mTitleUid)
		{
			mYourGamerpicUrl = yourGamerpicUrl;
			mYourGamerpic = null;
			
			mGameInfo = gameInfo;
	    	mTitleUid = titleUid;
		    
		    mIconCursor = null;
		    mPayload = null;
		    mAdapter = null;
		    mListView.setAdapter(mAdapter);
			
	    	synchronizeWithServer();
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
						if (payloadCursor.getLong(ComparedTrophyCursor.COLUMN_IS_LOCKED) != 0)
							continue;
						
						ic.newRow()
							.add(payloadCursor.getLong(0))
							.add(payloadCursor.getString(ComparedTrophyCursor.COLUMN_ICON_URL));
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
		
		if (mMyGamerpicUrl != null && mMyGamerpic == null)
		{
			if ((mMyGamerpic = ic.getCachedBitmap(mMyGamerpicUrl, mCp)) == null)
				ic.requestImage(mMyGamerpicUrl, mGamerpicListener, 0, ICON_MINE, false, mCp);
		}
		
		if (mYourGamerpicUrl != null && mYourGamerpic == null)
		{
			if ((mYourGamerpic = ic.getCachedBitmap(mYourGamerpicUrl, mCp)) == null)
				ic.requestImage(mYourGamerpicUrl, mGamerpicListener, 0, ICON_YOURS, false, mCp);
		}
		
		// Game details
		
		if (mGameInfo != null)
		{
			View parent = getView();
			TextView tv;
			ImageView gpIcon;
			
			if ((tv = (TextView)parent.findViewById(R.id.game_title)) != null)
			{
				tv.setText((String)mGameInfo.get(ComparedGameCursor.COLUMN_TITLE));
				
				gpIcon = (ImageView)parent.findViewById(R.id.self_icon);
				gpIcon.setImageBitmap(mMyGamerpic);
				gpIcon = (ImageView)parent.findViewById(R.id.opp_icon);
				gpIcon.setImageBitmap(mYourGamerpic);
				
				tv = (TextView)parent.findViewById(R.id.self_trophies_platinum);
				tv.setText(String.valueOf(mGameInfo.get(ComparedGameCursor.COLUMN_SELF_PLATINUM)));
				tv = (TextView)parent.findViewById(R.id.self_trophies_gold);
				tv.setText(String.valueOf(mGameInfo.get(ComparedGameCursor.COLUMN_SELF_GOLD)));
				tv = (TextView)parent.findViewById(R.id.self_trophies_silver);
				tv.setText(String.valueOf(mGameInfo.get(ComparedGameCursor.COLUMN_SELF_SILVER)));
				tv = (TextView)parent.findViewById(R.id.self_trophies_bronze);
				tv.setText(String.valueOf(mGameInfo.get(ComparedGameCursor.COLUMN_SELF_BRONZE)));
				
				tv = (TextView)parent.findViewById(R.id.opp_trophies_platinum);
				tv.setText(String.valueOf(mGameInfo.get(ComparedGameCursor.COLUMN_OPP_PLATINUM)));
				tv = (TextView)parent.findViewById(R.id.opp_trophies_gold);
				tv.setText(String.valueOf(mGameInfo.get(ComparedGameCursor.COLUMN_OPP_GOLD)));
				tv = (TextView)parent.findViewById(R.id.opp_trophies_silver);
				tv.setText(String.valueOf(mGameInfo.get(ComparedGameCursor.COLUMN_OPP_SILVER)));
				tv = (TextView)parent.findViewById(R.id.opp_trophies_bronze);
				tv.setText(String.valueOf(mGameInfo.get(ComparedGameCursor.COLUMN_OPP_BRONZE)));
				
				if ((Integer)mGameInfo.get(ComparedGameCursor.COLUMN_SELF_PLAYED) != 0)
				{
					tv = (TextView)parent.findViewById(R.id.self_progress);
					tv.setText(getString(R.string.compare_game_progress_f,
							mGameInfo.get(ComparedGameCursor.COLUMN_SELF_PROGRESS)));
					
					parent.findViewById(R.id.self_section).setVisibility(View.VISIBLE);
					parent.findViewById(R.id.self_not_played).setVisibility(View.GONE);
				}
				else
				{
					parent.findViewById(R.id.self_section).setVisibility(View.GONE);
					parent.findViewById(R.id.self_not_played).setVisibility(View.VISIBLE);
				}
				
				if ((Integer)mGameInfo.get(ComparedGameCursor.COLUMN_OPP_PLAYED) != 0)
				{
					tv = (TextView)parent.findViewById(R.id.opp_progress);
					tv.setText(getString(R.string.compare_game_progress_f,
							mGameInfo.get(ComparedGameCursor.COLUMN_OPP_PROGRESS)));
					
					parent.findViewById(R.id.opp_section).setVisibility(View.VISIBLE);
					parent.findViewById(R.id.opp_not_played).setVisibility(View.GONE);
				}
				else
				{
					parent.findViewById(R.id.opp_section).setVisibility(View.GONE);
					parent.findViewById(R.id.opp_not_played).setVisibility(View.VISIBLE);
				}
				
				final ImageView iv;
				if ((iv = (ImageView)parent.findViewById(R.id.game_icon)) != null)
				{
					String iconUrl = (String)mGameInfo.get(ComparedGameCursor.COLUMN_ICON_URL);
					Bitmap bmp;
					
					if ((bmp = ic.getCachedBitmap(iconUrl, mCp)) != null)
					{
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
										final Bitmap bmp)
								{
									mHandler.post(new Runnable()
									{
										@Override
										public void run()
										{
											iv.setImageBitmap(bmp);
										}
									});
								}
							}, 0, null, true, mCp);
						}
					}
				}
			}
		}
		
		syncIcons();
	}
	
	private void synchronizeWithServer()
	{
		if (mTitleUid != null)
		{
			mListView.setEmptyView(mProgress);
			mMessage.setVisibility(View.GONE);
			
			TaskController.getInstance().compareAchievements(mAccount, mGamertag, 
				mTitleUid, mListener);
		}
	}
}
