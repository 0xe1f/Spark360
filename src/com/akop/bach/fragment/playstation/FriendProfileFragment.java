/*
 * FriendProfileFragment.java 
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

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.CachePolicy;
import com.akop.bach.PSN;
import com.akop.bach.PSN.Friends;
import com.akop.bach.PsnAccount;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.activity.playstation.CompareGames;
import com.akop.bach.fragment.GenericFragment;

public class FriendProfileFragment extends GenericFragment
{
	private TaskListener mListener = new TaskListener();
	private static CachePolicy sCp = new CachePolicy(CachePolicy.SECONDS_IN_HOUR * 4);
	
	private PsnAccount mAccount;
	private long mTitleId = -1;
	private String mGamertag;
	
	@TargetApi(11)
    class HoneyCombHelper
	{
		public void invalidateMenu()
		{
			getActivity().invalidateOptionsMenu();
		}
	}
	
	private final ContentObserver mObserver = new ContentObserver(new Handler())
	{
		@Override
		public void onChange(boolean selfUpdate)
		{
			super.onChange(selfUpdate);
			
			synchronizeLocal();
		}
    };
    
	public static FriendProfileFragment newInstance(PsnAccount account)
	{
		return newInstance(account, -1);
	}
	
	public static FriendProfileFragment newInstance(PsnAccount account,
			long titleId)
	{
		FriendProfileFragment f = new FriendProfileFragment();
		
		Bundle args = new Bundle();
		args.putParcelable("account", account);
		args.putLong("titleId", titleId);
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
		
		View layout = inflater.inflate(R.layout.psn_fragment_friend_summary,
				container, false);
		
		layout.findViewById(R.id.refresh_profile).setOnClickListener(new View.OnClickListener() 
		{
			@Override
			public void onClick(View v)
			{
		    	TaskController.getInstance().updateFriendProfile(mAccount, 
		    			mGamertag, mListener);
			}
		});
		
		layout.findViewById(R.id.compare_games).setOnClickListener(new View.OnClickListener() 
		{
			@Override
			public void onClick(View v)
			{
		    	CompareGames.actionShow(getActivity(), mAccount, mGamertag);
			}
		});
		
		return layout;
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
		cr.registerContentObserver(Friends.CONTENT_URI, true, mObserver);
		
		synchronizeLocal();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		outState.putParcelable("account", mAccount);
		outState.putLong("titleId", mTitleId);
	}
	
	public void resetTitle(long id)
	{
		mTitleId = id;
		
		synchronizeLocal();
	}
	
	private void synchronizeLocal()
	{
		if (mTitleId > 0)
		{
			boolean exists = false;
			boolean isDirty = false;
			
			ContentResolver cr = getActivity().getContentResolver();
			Cursor cursor = cr.query(Friends.CONTENT_URI, 
					new String[] { Friends.ONLINE_ID, Friends.LAST_UPDATED }, 
					Friends._ID + "=" + mTitleId, 
					null, null);
			
			if (cursor != null)
			{
				try
				{
					if (cursor.moveToFirst())
					{
						isDirty = System.currentTimeMillis() - cursor.getLong(1) 
							> mAccount.getSummaryRefreshInterval();
	        			
						exists = true;
						mGamertag = cursor.getString(0);
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
		
		loadFriendDetails();
		
        if (android.os.Build.VERSION.SDK_INT >= 11)
        	new HoneyCombHelper().invalidateMenu();
	}
	
	private void synchronizeWithServer()
	{
		TaskController.getInstance().updateFriendProfile(mAccount, 
				mGamertag, mListener);
	}
	
	@Override 
	public void onImageReady(long id, Object param, Bitmap bmp)
	{
		super.onImageReady(id, param, bmp);
		
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				loadFriendDetails();
			}
		});
	}
	
	private void loadFriendDetails()
	{
		View container = getView();
		if (container == null)
			return;
		
		if (mTitleId < 0)
		{
			container.findViewById(R.id.unselected).setVisibility(View.VISIBLE);
			container.findViewById(R.id.profile_contents).setVisibility(View.GONE);
		}
		else
		{
			container.findViewById(R.id.unselected).setVisibility(View.GONE);
			container.findViewById(R.id.profile_contents).setVisibility(View.VISIBLE);
			
			ContentResolver cr = getActivity().getContentResolver();
			Cursor c = cr.query(Friends.CONTENT_URI, 
					new String[] { 
						Friends.ONLINE_ID,
						Friends.LEVEL,
						Friends.PROGRESS,
						Friends.ICON_URL,
						Friends.TROPHIES_PLATINUM,
						Friends.TROPHIES_GOLD,
						Friends.TROPHIES_SILVER,
						Friends.TROPHIES_BRONZE,
						Friends.PLAYING,
						Friends.ONLINE_STATUS,
						Friends.MEMBER_TYPE,
					},
					Friends.ACCOUNT_ID + "=" + mAccount.getId() + 
					" AND " + Friends.ONLINE_ID + "=?", 
					new String[] { mGamertag }, 
					null);
			
			if (c != null)
			{
				try
				{
					if (c.moveToFirst())
					{
						int platinum = c.getInt(4);
						int gold = c.getInt(5);
						int silver = c.getInt(6);
						int bronze = c.getInt(7);
						int currentStatus = c.getInt(9);
						boolean isPlus = (c.getInt(10) == PSN.MEMBER_TYPE_PLUS);
						
						String currentActivity = null;
						String statusDescription = PSN.getOnlineStatusDescription(getActivity(), 
								currentStatus);
						
						currentActivity = c.getString(8);
						String fullStatus;
						
						if (currentActivity == null)
							fullStatus = statusDescription;
						else
							fullStatus = getString(R.string.status_description_f,
									statusDescription, currentActivity);
						
						TextView tv;
						tv = (TextView)container.findViewById(R.id.profile_gamertag);
						tv.setText(c.getString(0));
						tv = (TextView)container.findViewById(R.id.profile_trophies_plat);
						tv.setText(getString(R.string.x_platinum, platinum));
						tv = (TextView)container.findViewById(R.id.profile_trophies_gold);
						tv.setText(getString(R.string.x_gold, gold));
						tv = (TextView)container.findViewById(R.id.profile_trophies_silver);
						tv.setText(getString(R.string.x_silver, silver));
						tv = (TextView)container.findViewById(R.id.profile_trophies_bronze);
						tv.setText(getString(R.string.x_bronze, bronze));
						tv = (TextView)container.findViewById(R.id.profile_trophy_total);
						tv.setText(String.valueOf(bronze + silver + gold + platinum));
						tv = (TextView)container.findViewById(R.id.profile_playing);
						tv.setText(fullStatus);
						
						tv = (TextView)container.findViewById(R.id.profile_level);
						tv.setText(String.valueOf(c.getInt(1)));
						tv = (TextView)container.findViewById(R.id.progress_ind);
						tv.setText(String.valueOf(c.getInt(2)));
						
						ProgressBar pb = (ProgressBar)container.findViewById(R.id.progress_bar);
						pb.setProgress(c.getInt(2));
						
						String imageUrl = mAccount.getLargeAvatar(c.getString(3));
						ImageCache ic = ImageCache.getInstance();
						
						Bitmap bmp;
						ImageView iv;
						
						iv = (ImageView)container.findViewById(R.id.profile_plus);
						iv.setVisibility(isPlus ? View.VISIBLE : View.GONE);
						
						iv = (ImageView)container.findViewById(R.id.profile_avatar);
						
						if ((bmp = ic.getCachedBitmap(imageUrl, sCp)) != null)
						{
							iv.setImageBitmap(bmp);
						}
						else
						{
							iv.setImageResource(R.drawable.psn_avatar_large);
							ic.requestImage(imageUrl, this, 0, null, sCp);
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
}
