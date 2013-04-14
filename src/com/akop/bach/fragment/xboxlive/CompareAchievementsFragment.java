/*
 * CompareAchievementsFragment.java 
 * Copyright (C) 2010-2013 Akop Karapetyan
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
import java.text.DateFormat;
import java.util.HashMap;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
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
import com.akop.bach.ImageCache.OnImageReadyListener;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.XboxLive.ComparedAchievementCursor;
import com.akop.bach.XboxLive.ComparedAchievementInfo;
import com.akop.bach.XboxLive.ComparedGameCursor;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.activity.xboxlive.GameOverview;
import com.akop.bach.fragment.GenericFragment;
import com.akop.bach.parser.XboxLiveParser;

public class CompareAchievementsFragment extends GenericFragment implements
		OnItemClickListener
{
	private static class ViewHolder
	{
		public TextView title;
		public TextView description;
		public TextView score;
		public ImageView myGamerpic;
		public ImageView yourGamerpic;
		public TextView myAcquired;
		public TextView yourAcquired;
		public ImageView icon;
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
			View view = li.inflate(R.layout.xbl_compared_achievement_list_item, 
					parent, false);
			view.setTag(vh);
			
			vh.title = (TextView)view.findViewById(R.id.achievement_title);
			vh.description = (TextView)view.findViewById(R.id.achievement_description);
			vh.score = (TextView)view.findViewById(R.id.achievement_gp);
			vh.myGamerpic = (ImageView)view.findViewById(R.id.me_gamerpic);
			vh.yourGamerpic = (ImageView)view.findViewById(R.id.you_gamerpic);
			vh.myAcquired = (TextView)view.findViewById(R.id.achievement_my_acquired);
			vh.yourAcquired = (TextView)view.findViewById(R.id.achievement_your_acquired);
			vh.icon = (ImageView)view.findViewById(R.id.achievement_icon);
			
			return view;
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			ViewHolder vh = (ViewHolder)view.getTag();
			
			vh.title.setText(cursor.getString(ComparedAchievementCursor.COLUMN_TITLE));
			vh.description.setText(cursor.getString(ComparedAchievementCursor.COLUMN_DESCRIPTION));
			vh.score.setText(getString(R.string.x_f, 
					cursor.getInt(ComparedAchievementCursor.COLUMN_SCORE)));
			
			String myAcquired;
			if (cursor.getInt(ComparedAchievementCursor.COLUMN_MY_IS_LOCKED) != 0)
			{
				myAcquired = getString(R.string.locked);
			}
			else
			{
				long acquired = cursor.getLong(ComparedAchievementCursor.COLUMN_MY_ACQUIRED);
				if (acquired == 0)
					myAcquired = getString(R.string.acquired_offline);
				else
					myAcquired = DateFormat.getDateInstance().format(acquired);
			}
			
			String yourAcquired;
			if (cursor.getInt(ComparedAchievementCursor.COLUMN_YOUR_IS_LOCKED) != 0)
			{
				yourAcquired = getString(R.string.locked);
			}
			else
			{
				long acquired = cursor.getLong(ComparedAchievementCursor.COLUMN_YOUR_ACQUIRED);
				if (acquired == 0)
					yourAcquired = getString(R.string.acquired_offline);
				else
					yourAcquired = DateFormat.getDateInstance().format(acquired);
			}
			
			vh.myAcquired.setText(myAcquired);
			vh.yourAcquired.setText(yourAcquired);
			
			vh.myGamerpic.setImageBitmap(mMyGamerpic);
			vh.yourGamerpic.setImageBitmap(mYourGamerpic);
			
			String iconUrl = cursor.getString(ComparedAchievementCursor.COLUMN_ICON_URL);
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
					vh.icon.setImageResource(R.drawable.xbox_achieve_locked_default);
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
	
	private TaskListener mListener = new TaskListener("XboxCompareAchieves")
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
					{
						if (App.getConfig().logToConsole())
							e.printStackTrace();
						
						mMessage.setText(XboxLiveParser.getErrorMessage(getActivity(), e));
					}
					
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
					if (result != null && result instanceof ComparedAchievementInfo)
					{
						mPayload = (ComparedAchievementInfo)result;
						
						initializeAdapter();
						synchronizeLocal();
					}
					
					mMessage.setText(getString(R.string.game_has_no_achieves));
				}
			});
		}
	};
	
	private XboxLiveAccount mAccount;
	private String mGamertag;
	private String mTitleUid;
	private String mMyGamerpicUrl;
	private String mYourGamerpicUrl;
	private HashMap<Integer, Object> mGameInfo;
	
	private ListView mListView = null;
	private TextView mMessage = null;
	private View mProgress = null;
	
	private IconCursor mIconCursor = null;
	private ComparedAchievementInfo mPayload;
	private MyCursorAdapter mAdapter = null;
	private Handler mHandler = new Handler();
	private Bitmap mMyGamerpic;
	private Bitmap mYourGamerpic;
	
	public static CompareAchievementsFragment newInstance(XboxLiveAccount account, 
			String gamertag, HashMap<Integer, Object> gameInfo,
			String yourGamerpicUrl)
	{
		CompareAchievementsFragment f = new CompareAchievementsFragment();
		
		Bundle args = new Bundle();
		args.putParcelable("account", account);
		args.putString("gamertag", gamertag);
		args.putSerializable("gameInfo", gameInfo);
		args.putString("yourGamerpicUrl", yourGamerpicUrl);
		f.setArguments(args);
		
		return f;
	}
	
	public static CompareAchievementsFragment newInstance(XboxLiveAccount account, 
			String gamertag)
	{
		CompareAchievementsFragment f = new CompareAchievementsFragment();
		
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
	    
	    Bundle args = getArguments();
	    
	    mAccount = (XboxLiveAccount)args.getParcelable("account");
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
					mPayload = (ComparedAchievementInfo)state.getSerializable("payload");
				
				if (state.containsKey("icons"))
					mIconCursor = (IconCursor)state.getSerializable("icons");
				
				if (state.containsKey("yourGamerpicUrl"))
					mYourGamerpicUrl = state.getString("yourGamerpicUrl");
	    	}
	    	catch(Exception e)
	    	{
	    		if (App.getConfig().logToConsole())
	    			e.printStackTrace();
	    		
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
		
		View layout = inflater.inflate(R.layout.xbl_fragment_compare_achievement_list,
				container, false);
		
		mMessage = (TextView)layout.findViewById(R.id.message);
		mMessage.setText(R.string.select_game_to_compare_achieves);
		
		mListView = (ListView)layout.findViewById(R.id.list);
		mListView.setOnItemClickListener(this);
		mListView.setEmptyView(mMessage);
		
		initializeAdapter();
		
		//registerForContextMenu(mListView);
		
		mProgress = layout.findViewById(R.id.loading);
		mProgress.setVisibility(View.GONE);
		
		View gameDetails = layout.findViewById(R.id.game_details);
		if (gameDetails != null)
		{
			gameDetails.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (mGameInfo != null)
					{
						String gameUrl = (String)mGameInfo.get(ComparedGameCursor.COLUMN_GAME_URL);
						if (gameUrl != null)
							GameOverview.actionShow(getActivity(),
									mAccount, gameUrl);
					}
				}
			});
		}
		
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
    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)
    {
		Cursor c = (Cursor)arg0.getItemAtPosition(position);
		if (c != null)
		{
			String title = c.getString(ComparedAchievementCursor.COLUMN_TITLE);
			String description = c.getString(ComparedAchievementCursor.COLUMN_DESCRIPTION);
			
			showAchievementDetails(title, description);
		}
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
					Uri uri = ContentUris.withAppendedId(ComparedAchievementCursor.CONTENT_URI, id);
					getActivity().getContentResolver().notifyChange(uri, null);
				}
			});
		}
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
						ic.newRow()
							.add(payloadCursor.getLong(0))
							.add(payloadCursor.getString(ComparedAchievementCursor.COLUMN_ICON_URL));
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
			if ((mMyGamerpic = ic.getCachedBitmap(mMyGamerpicUrl)) == null)
				ic.requestImage(mMyGamerpicUrl, mGamerpicListener, 0, ICON_MINE);
		}
		
		if (mYourGamerpicUrl != null && mYourGamerpic == null)
		{
			if ((mYourGamerpic = ic.getCachedBitmap(mYourGamerpicUrl)) == null)
				ic.requestImage(mYourGamerpicUrl, mGamerpicListener, 0, ICON_YOURS);
		}
		
		// Game details
		
		if (mGameInfo != null)
		{
			View parent = getView();
			TextView tv;
			ImageView gpIcon;
			
			if ((tv = (TextView)parent.findViewById(R.id.game_title)) != null)
				tv.setText((String)mGameInfo.get(ComparedGameCursor.COLUMN_TITLE));
			
			if ((gpIcon = (ImageView)parent.findViewById(R.id.me_gamerpic)) != null)
				gpIcon.setImageBitmap(mMyGamerpic);
			
			if ((gpIcon = (ImageView)parent.findViewById(R.id.you_gamerpic)) != null)
				gpIcon.setImageBitmap(mYourGamerpic);
			
			if ((tv = (TextView)parent.findViewById(R.id.me_achievements_unlocked)) != null)
			{
				String text = getString(R.string.x_of_x_f, 
						mGameInfo.get(ComparedGameCursor.COLUMN_MY_ACH_UNLOCKED),
						mGameInfo.get(ComparedGameCursor.COLUMN_ACH_TOTAL));
				tv.setText(text);
			}
			
			if ((tv = (TextView)parent.findViewById(R.id.you_achievements_unlocked)) != null)
			{
				String text = getString(R.string.x_of_x_f, 
						mGameInfo.get(ComparedGameCursor.COLUMN_YOUR_ACH_UNLOCKED), 
						mGameInfo.get(ComparedGameCursor.COLUMN_ACH_TOTAL));
				tv.setText(text);
			}
			
			if ((tv = (TextView)parent.findViewById(R.id.me_gp_earned)) != null)
			{
				String text = getString(R.string.x_of_x_f, 
						mGameInfo.get(ComparedGameCursor.COLUMN_MY_GP_EARNED), 
						mGameInfo.get(ComparedGameCursor.COLUMN_GP_TOTAL));
				tv.setText(text);
			}
			
			if ((tv = (TextView)parent.findViewById(R.id.you_gp_earned)) != null)
			{
				String text = getString(R.string.x_of_x_f, 
						mGameInfo.get(ComparedGameCursor.COLUMN_YOUR_GP_EARNED), 
						mGameInfo.get(ComparedGameCursor.COLUMN_GP_TOTAL));
				tv.setText(text);
			}
			
			final ImageView iv;
			if ((iv = (ImageView)parent.findViewById(R.id.game_icon)) != null)
			{
				String iconUrl = (String)mGameInfo.get(ComparedGameCursor.COLUMN_BOXART_URL);
				Bitmap bmp;
				
				if ((bmp = ic.getCachedBitmap(iconUrl)) != null)
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
						}, 0, null, true, null);
					}
				}
			}
		}
		
		syncIcons();
	}
	
	private void synchronizeWithServer()
	{
		if (mGameInfo != null)
		{
		    if ((Integer)mGameInfo.get(ComparedGameCursor.COLUMN_ACH_TOTAL) <= 0)
		    {
			    mMessage.setText(R.string.game_has_no_achieves);
			    mListView.setEmptyView(mMessage);
			    mProgress.setVisibility(View.GONE);
			    
			    return;
		    }
		}
	    
		if (mTitleUid != null)
		{
			mListView.setEmptyView(mProgress);
			mMessage.setVisibility(View.GONE);
			
			TaskController.getInstance().compareAchievements(mAccount, mGamertag, 
				mTitleUid, mListener);
		}
	}
}
