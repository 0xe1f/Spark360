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

package com.akop.bach.fragment.xboxlive;

import java.io.IOException;

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
import android.widget.TextView;
import android.widget.Toast;

import com.akop.bach.App;
import com.akop.bach.IAccount;
import com.akop.bach.ImageCache;
import com.akop.bach.Preferences;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.CustomTask;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.XboxLive.Friends;
import com.akop.bach.XboxLive.Messages;
import com.akop.bach.XboxLive.Profiles;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.activity.About;
import com.akop.bach.activity.Accounts;
import com.akop.bach.activity.xboxlive.AccountSettings;
import com.akop.bach.activity.xboxlive.FriendList;
import com.akop.bach.activity.xboxlive.GameList;
import com.akop.bach.activity.xboxlive.MessageList;
import com.akop.bach.activity.xboxlive.MsPointConverter;
import com.akop.bach.activity.xboxlive.ServerStatus;
import com.akop.bach.fragment.GenericFragment;
import com.akop.bach.fragment.xboxlive.EditProfileFragment.ProfileUpdater;
import com.akop.bach.parser.AuthenticationException;
import com.akop.bach.parser.ParserException;
import com.akop.bach.parser.XboxLiveParser;

public class AccountProfileFragment extends GenericFragment implements ProfileUpdater
{
	private static final int starViews[] = 
	{ 
		R.id.profile_rep_star0,
		R.id.profile_rep_star1,
		R.id.profile_rep_star2,
		R.id.profile_rep_star3,
		R.id.profile_rep_star4,
	};
	
	private static final int starResources[] = 
	{ 
		R.drawable.xbox_star_o0,
		R.drawable.xbox_star_o1,
		R.drawable.xbox_star_o2,
		R.drawable.xbox_star_o3,
		R.drawable.xbox_star_o4,
	};
	
	private Handler mHandler = new Handler();
	private XboxLiveAccount mAccount = null;
	private TaskListener mListener = new TaskListener();
	private TaskListener mProfileUpdater = new TaskListener()
	{
		public void onTaskSucceeded(IAccount account, Object requestParam, Object result)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					Toast toast = Toast.makeText(getActivity(), 
							R.string.profile_updated, Toast.LENGTH_LONG);
					toast.show();
				}
			});
		}
	};
	
	private final ContentObserver mProfileObserver = new ContentObserver(new Handler())
	{
		@Override
		public void onChange(boolean selfUpdate)
		{
			super.onChange(selfUpdate);
			
			synchronizeLocal();
		}
    };
    
	private final ContentObserver mFriendObserver = new ContentObserver(new Handler())
	{
		@Override
		public void onChange(boolean selfUpdate)
		{
			super.onChange(selfUpdate);
			
			synchronizeLocal();
		}
    };
    
	private final ContentObserver mMessageObserver = new ContentObserver(new Handler())
	{
		@Override
		public void onChange(boolean selfUpdate)
		{
			super.onChange(selfUpdate);
			
			synchronizeLocal();
		}
    };
    
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
	
	public static AccountProfileFragment newInstance(XboxLiveAccount account)
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
		
		TaskController tc = TaskController.getInstance();
		long time = System.currentTimeMillis();
		
		if (force || time - mAccount.getLastSummaryUpdate() > mAccount.getSummaryRefreshInterval())
			tc.synchronizeSummary(mAccount, mListener);
		if (force || time - mAccount.getLastFriendUpdate() > mAccount.getFriendRefreshInterval())
			tc.updateFriendList(mAccount, mListener);
		if (force || time - mAccount.getLastMessageUpdate() > mAccount.getMessageRefreshInterval())
			tc.synchronizeMessages(mAccount, mListener);
	}
	
	public void resetTitle(XboxLiveAccount account)
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
			
			ImageCache ic = ImageCache.getInstance();
			String avatarUrl = XboxLiveParser.getAvatarUrl(mAccount.getGamertag());
			Bitmap bmp;
			
			if ((bmp = ic.getCachedBitmap(avatarUrl)) != null)
			{
				ImageView iv = (ImageView)layout.findViewById(R.id.profile_avatar_body);
				iv.setImageBitmap(bmp);
			}
			
			if (avatarUrl != null && ic.isExpired(avatarUrl, sCp))
				ic.requestImage(avatarUrl, this, 0, null, sCp);
			
			int rep = 0;
			int pointBal = 0;
			int gamerscore = 0;
			String tier = null;
			String name = null;
			String motto = null;
			String location = null;
			String bio = null;
			
			Cursor cursor = getActivity().getContentResolver().query(Profiles.CONTENT_URI,
					new String[] { 
						Profiles.REP, 
						Profiles.POINTS_BALANCE,
						Profiles.GAMERSCORE,
						Profiles.TIER,
						Profiles.NAME,
						Profiles.MOTTO,
						Profiles.LOCATION,
						Profiles.BIO,
					},
					Profiles.ACCOUNT_ID + "=" + mAccount.getId(), null,
					null);
			
			if (cursor != null)
			{
				try
				{
					if (cursor.moveToFirst())
					{
						rep = cursor.getInt(0);
						pointBal = cursor.getInt(1);
						gamerscore = cursor.getInt(2);
						tier = cursor.getString(3);
						name = cursor.getString(4);
						motto = cursor.getString(5);
						location = cursor.getString(6);
						bio = cursor.getString(7);
					}
				}
				catch(Exception ex)
				{
					// Do nothing
					if (App.LOGV)
						ex.printStackTrace();
				}
				finally
				{
					cursor.close();
				}
			}
			
			TextView tv = (TextView)layout.findViewById(R.id.profile_unread_messages);
			tv.setText(String.valueOf(Messages.getUnreadMessageCount(getActivity(), mAccount)));
			
			tv = (TextView)layout.findViewById(R.id.profile_friends_online);
			tv.setText(String.valueOf(Friends.getActiveFriendCount(getActivity(), mAccount)));
			
			tv = (TextView)layout.findViewById(R.id.profile_points);
			tv.setText(getString(R.string.x_f, gamerscore));
			
			tv = (TextView)layout.findViewById(R.id.profile_msp);
			tv.setText(getString(R.string.x_f, pointBal));
			
			tv = (TextView)layout.findViewById(R.id.profile_membership);
			tv.setText(tier);
			
			tv = (TextView)layout.findViewById(R.id.profile_name);
			tv.setText(name);
			
			tv = (TextView)layout.findViewById(R.id.profile_motto);
			tv.setText(motto);
			
			tv = (TextView)layout.findViewById(R.id.profile_location);
			tv.setText(location);
			
			tv = (TextView)layout.findViewById(R.id.profile_bio);
			tv.setText(bio);
			
			int res;
			for (int starPos = 0, j = 0, k = 4; starPos < 5; starPos++, j += 4, k += 4)
			{
				if (rep < j) res = 0;
				else if (rep >= k) res = 4;
				else res = rep - j;
				
				ImageView starView = (ImageView)layout.findViewById(starViews[starPos]);
				starView.setImageResource(starResources[res]);
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
				mAccount = (XboxLiveAccount)args.getSerializable("account");
			else
				mAccount = null;
		}
		
	    if (state != null && state.containsKey("account"))
			mAccount = (XboxLiveAccount)state.getSerializable("account");
	    
		setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		if (container == null)
			return null;
		
		View layout = inflater.inflate(R.layout.xbl_fragment_account_summary,
				container, false);
		
		View view = layout.findViewById(R.id.edit_profile);
		view.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v)
			{
				EditProfileFragment frag = EditProfileFragment.newInstance(mAccount);
				frag.setProfileUpdater(AccountProfileFragment.this);
				frag.show(getFragmentManager(), null);
			}
		});
		
		view = layout.findViewById(R.id.open_friends);
		view.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v)
			{
				FriendList.actionShow(getActivity(), mAccount);
			}
		});
		
		view = layout.findViewById(R.id.open_messages);
		view.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v)
			{
				MessageList.actionShow(getActivity(), mAccount);
			}
		});
		
		view = layout.findViewById(R.id.open_games);
		view.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v)
			{
				GameList.actionShow(getActivity(), mAccount);
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
		
		TaskController.getInstance().removeListener(mListener);
		TaskController.getInstance().removeListener(mProfileUpdater);
		
        ImageCache.getInstance().removeListener(this);
		
		ContentResolver cr = getActivity().getContentResolver();
		
        cr.unregisterContentObserver(mFriendObserver);
        cr.unregisterContentObserver(mMessageObserver);
        cr.unregisterContentObserver(mProfileObserver);
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		TaskController.getInstance().addListener(mListener);
		TaskController.getInstance().addListener(mProfileUpdater);
		
        ImageCache.getInstance().addListener(this);
		
		ContentResolver cr = getActivity().getContentResolver();
		
		cr.registerContentObserver(Profiles.CONTENT_URI, true, mProfileObserver);
        cr.registerContentObserver(Messages.CONTENT_URI, true, mFriendObserver);
        cr.registerContentObserver(Friends.CONTENT_URI, true, mMessageObserver);
        
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
	    
    	inflater.inflate(R.menu.xbl_account_summary, menu);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu)
	{
	    super.onPrepareOptionsMenu(menu);
	    
	    menu.setGroupVisible(R.id.menu_group_selected, mAccount != null);
	    menu.setGroupVisible(R.id.menu_group_selected2, mAccount != null);
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
	    case R.id.menu_tools_mspoint:
	    	MsPointConverter.actionShow(getActivity());
	    	return true;
	    case R.id.menu_server_status:
	    	ServerStatus.actionShow(getActivity());
	    	return true;
	    case R.id.menu_about:
	    	About.actionShowAbout(getActivity());
	    	return true;
	    }
	    
	    return false;
	}
	
	@Override
	public void updateProfile(XboxLiveAccount account, final String motto,
			final String name, final String location, final String bio)
	{
		TaskController.getInstance().runCustomTask(mAccount, new CustomTask<Void>()
				{
					@Override
					public void runTask() throws AuthenticationException,
							IOException, ParserException
					{
						XboxLiveParser p = new XboxLiveParser(getActivity());
						
						try
						{
							p.updateProfile(mAccount, motto, name, location, bio);
						}
						finally
						{
							p.dispose();
						}
					}
				}, mProfileUpdater);
	}
}
