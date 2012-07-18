/*
 * AccountProfileFragment.java 
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.akop.bach.ImageCache;
import com.akop.bach.PSN;
import com.akop.bach.PSN.Friends;
import com.akop.bach.PSN.Profiles;
import com.akop.bach.Preferences;
import com.akop.bach.PsnAccount;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.activity.About;
import com.akop.bach.activity.Accounts;
import com.akop.bach.activity.playstation.AccountSettings;
import com.akop.bach.activity.playstation.FriendList;
import com.akop.bach.activity.playstation.GameCatalog;
import com.akop.bach.activity.playstation.GameList;
import com.akop.bach.activity.playstation.PsBlog;
import com.akop.bach.fragment.GenericFragment;

public class AccountProfileFragment extends GenericFragment
{
	private Handler mHandler = new Handler();
	private PsnAccount mAccount = null;
	private TaskListener mListener = new TaskListener();
	
	private final ContentObserver mObserver = new ContentObserver(new Handler())
	{
		@Override
		public void onChange(boolean selfUpdate)
		{
			super.onChange(selfUpdate);
			
			synchronizeLocal();
		}
    };
    
	@TargetApi(11)
    class HoneyCombHelper
	{
		public void invalidateMenu()
		{
			getActivity().invalidateOptionsMenu();
		}
	}
	
	public static AccountProfileFragment newInstance()
	{
		return newInstance(null);
	}
	
	public static AccountProfileFragment newInstance(PsnAccount account)
	{
		AccountProfileFragment f = new AccountProfileFragment();
		
		Bundle args = new Bundle();
		
		if (account != null)
			args.putSerializable("account", account);
		
		f.setArguments(args);
		
		return f;
	}
	
	private void synchronizeWithServer(boolean force)
	{
		if (mAccount == null)
			return; // Nothing to synch
		
		TaskController tc = TaskController.get();
		long time = System.currentTimeMillis();
		
		if (force || time - mAccount.getLastSummaryUpdate() > mAccount.getSummaryRefreshInterval())
			tc.synchronizeSummary(mAccount, mListener);
		if (force || time - mAccount.getLastFriendUpdate() > mAccount.getFriendRefreshInterval())
			tc.updateFriendList(mAccount, mListener);
	}
	
	public void resetTitle(PsnAccount account)
	{
		mAccount = account;
		
		synchronizeLocal();
		
		if (mAccount != null)
			synchronizeWithServer(false);
		
        if (android.os.Build.VERSION.SDK_INT >= 11)
        	new HoneyCombHelper().invalidateMenu();
	}
	
	private void synchronizeLocal()
	{
		View layout = getView(); 
		if (layout == null)
			return;
		
		if (mAccount != null)
		{
			layout.findViewById(R.id.profile).setVisibility(View.VISIBLE);
			layout.findViewById(R.id.message).setVisibility(View.GONE);
			
			ContentResolver cr = getActivity().getContentResolver();
			Cursor c = cr.query(Profiles.CONTENT_URI, 
					new String[] { 
						Profiles.ONLINE_ID,
						Profiles.LEVEL,
						Profiles.PROGRESS,
						Profiles.ICON_URL,
						Profiles.TROPHIES_PLATINUM,
						Profiles.TROPHIES_GOLD,
						Profiles.TROPHIES_SILVER,
						Profiles.TROPHIES_BRONZE,
						Profiles.MEMBER_TYPE,
					},
					Profiles.ACCOUNT_ID + "=" + mAccount.getId(), null, 
					null);
			
			try
			{
				if (c != null && c.moveToFirst())
				{
					int platinum = c.getInt(4);
					int gold = c.getInt(5);
					int silver = c.getInt(6);
					int bronze = c.getInt(7);
					boolean isPlus = c.getInt(8) == PSN.MEMBER_TYPE_PLUS;
					
					TextView tv;
					tv = (TextView)layout.findViewById(R.id.profile_trophies_plat);
					tv.setText(getString(R.string.x_platinum, platinum));
					tv = (TextView)layout.findViewById(R.id.profile_trophies_gold);
					tv.setText(getString(R.string.x_gold, gold));
					tv = (TextView)layout.findViewById(R.id.profile_trophies_silver);
					tv.setText(getString(R.string.x_silver, silver));
					tv = (TextView)layout.findViewById(R.id.profile_trophies_bronze);
					tv.setText(getString(R.string.x_bronze, bronze));
					tv = (TextView)layout.findViewById(R.id.profile_trophy_total);
					tv.setText(String.valueOf(bronze + silver + gold + platinum));
					
					tv = (TextView)layout.findViewById(R.id.profile_level);
					tv.setText(String.valueOf(c.getInt(1)));
					tv = (TextView)layout.findViewById(R.id.progress_ind);
					tv.setText(String.valueOf(c.getInt(2)));
					
					tv = (TextView)layout.findViewById(R.id.profile_friends_online);
					tv.setText(String.valueOf(Friends.getActiveFriendCount(getActivity(),
							mAccount)));
					
					ProgressBar pb = (ProgressBar)layout.findViewById(R.id.progress_bar);
					pb.setProgress(c.getInt(2));
					
					String imageUrl = mAccount.getLargeAvatar(c.getString(3));
					ImageCache ic = ImageCache.get();
					Bitmap bmp;
					ImageView iv;
					
					iv = (ImageView)layout.findViewById(R.id.profile_plus);
					iv.setVisibility(isPlus ? View.VISIBLE : View.GONE);
					
					iv = (ImageView)layout.findViewById(R.id.profile_avatar);
					if ((bmp = ic.getCachedBitmap(imageUrl)) != null)
					{
						iv.setImageBitmap(bmp);
					}
					else
					{
						iv.setImageResource(R.drawable.psn_avatar_large);
						ic.requestImage(imageUrl, this, 0, null);
					}
				}
			}
			finally
			{
				if (c != null)
					c.close();
			}
		}
		else
		{
			layout.findViewById(R.id.profile).setVisibility(View.GONE);
			layout.findViewById(R.id.message).setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	public void onCreate(Bundle state)
	{
		super.onCreate(state);
		
		if (mAccount == null)
		{
		    Bundle args = getArguments();
			
			if (args.containsKey("account"))
				mAccount = (PsnAccount)args.getSerializable("account");
			else
				mAccount = null;
		}
		
	    if (state != null && state.containsKey("account"))
			mAccount = (PsnAccount)state.getSerializable("account");
	    
		setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		if (container == null)
			return null;
		
		View layout = inflater.inflate(R.layout.psn_fragment_account_summary,
				container, false);
		
		View view = layout.findViewById(R.id.view_friends);
		view.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v)
			{
				FriendList.actionShow(getActivity(), mAccount);
			}
		});
		
		view = layout.findViewById(R.id.view_trophies);
		view.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v)
			{
				GameList.actionShow(getActivity(), mAccount);
			}
		});
		
		view = layout.findViewById(R.id.view_blog);
		view.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v)
			{
				PsBlog.actionShow(getActivity(), mAccount);
			}
		});
		
		view = layout.findViewById(R.id.view_catalog);
		view.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v)
			{
				GameCatalog.actionShow(getActivity(), mAccount);
			}
		});
		
		return layout;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		if (mAccount != null)
			outState.putSerializable("account", mAccount);
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		
		TaskController.get().removeListener(mListener);
        ImageCache.get().removeListener(this);
		
		ContentResolver cr = getActivity().getContentResolver();
        cr.unregisterContentObserver(mObserver);
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		TaskController.get().addListener(mListener);
        ImageCache.get().addListener(this);
		
		ContentResolver cr = getActivity().getContentResolver();
		
		cr.registerContentObserver(Profiles.CONTENT_URI, true, mObserver);
        cr.registerContentObserver(Friends.CONTENT_URI, true, mObserver);
        
		if (mAccount != null)
		{
			mAccount.refresh(Preferences.get(getActivity()));
			synchronizeWithServer(false);
		}
		
		synchronizeLocal();
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
			    synchronizeLocal();
			}
		});
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
	    super.onCreateOptionsMenu(menu, inflater);
	    
	    inflater.inflate(R.menu.psn_account_summary, menu);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu)
	{
	    super.onPrepareOptionsMenu(menu);
	    
	    menu.setGroupVisible(R.id.menu_group_selected, mAccount != null);
	    menu.setGroupVisible(R.id.menu_group_singlepane, !mDualPane);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
	    switch (item.getItemId()) 
	    {
	    case R.id.menu_refresh:
	    	if (mAccount != null)
	    		synchronizeWithServer(true);
	    	
	    	return true;
	    case R.id.menu_edit_account:
	    	if (mAccount != null)
	    		AccountSettings.actionEditSettings(getActivity(), mAccount);
	    	
	    	return true;
	    case R.id.menu_list_accounts:
	    	Accounts.actionShow(getActivity());
	    	getActivity().finish();
	    	return true;
	    case R.id.menu_about:
	    	About.actionShowAbout(getActivity());
	    	return true;
	    }
	    
	    return false;
	}
}
