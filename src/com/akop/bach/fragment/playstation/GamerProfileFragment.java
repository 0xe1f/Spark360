/*
 * GamerProfileFragment.java 
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

import java.io.IOException;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.akop.bach.IAccount;
import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.CachePolicy;
import com.akop.bach.PSN.GamerProfileInfo;
import com.akop.bach.PsnAccount;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.CustomTask;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.activity.playstation.CompareGames;
import com.akop.bach.fragment.GenericFragment;
import com.akop.bach.parser.AuthenticationException;
import com.akop.bach.parser.Parser;
import com.akop.bach.parser.ParserException;
import com.akop.bach.parser.PsnParser;
import com.akop.bach.parser.PsnUsParser;

public class GamerProfileFragment extends GenericFragment
{
	private static CachePolicy sCp = new CachePolicy(CachePolicy.SECONDS_IN_HOUR * 4);
	
	private PsnAccount mAccount;
	private String mGamertag;
	private Handler mHandler = new Handler();
	private TextView mMessage;
	private GamerProfileInfo mGamerProfile;
	
	private TaskListener mListener = new TaskListener()
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
						mMessage.setText(Parser.getErrorMessage(getActivity(), e));
				}
			});
		}
		
		@Override
		public void onTaskSucceeded(IAccount account, Object requestParam, final Object result)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					if (result != null && result instanceof GamerProfileInfo)
					{
						mGamerProfile = (GamerProfileInfo)result;
						mMessage.setText(R.string.loading_profile);
						
						synchronizeLocal();
					}
				}
			});
		}
	};
	
	public static GamerProfileFragment newInstance(PsnAccount account)
	{
		return newInstance(account, null);
	}
	
	public static GamerProfileFragment newInstance(PsnAccount account,
			String gamertag)
	{
		GamerProfileFragment f = new GamerProfileFragment();
		
		Bundle args = new Bundle();
		args.putSerializable("account", account);
		args.putString("gamertag", gamertag);
		f.setArguments(args);
		
		return f;
	}
	
	@Override
	public void onCreate(Bundle state)
	{
		super.onCreate(state);
		
	    Bundle args = getArguments();
	    
	    mAccount = (PsnAccount)args.getSerializable("account");
	    mGamertag = args.getString("gamertag");
	    mGamerProfile = null;
	    
	    if (state != null)
	    {
			mGamertag = state.getString("gamertag");
			mGamerProfile = (GamerProfileInfo)state.getSerializable("gamerProfile");
	    }
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		outState.putString("gamertag", mGamertag);
		
		if (mGamerProfile != null)
			outState.putSerializable("gamerProfile", mGamerProfile);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		if (container == null)
			return null;
		
		View layout = inflater.inflate(R.layout.psn_fragment_friend_summary,
				container, false);
		
		mMessage = (TextView)layout.findViewById(R.id.unselected);
		mMessage.setText(R.string.loading_profile);
		
		layout.findViewById(R.id.refresh_profile).setOnClickListener(new View.OnClickListener() 
		{
			@Override
			public void onClick(View v)
			{
				synchronizeWithServer();
			}
		});
		
		View compareButton = layout.findViewById(R.id.compare_games);
		compareButton.setVisibility(mAccount.canCompareUnknowns()
				? View.VISIBLE : View.GONE);
		compareButton.setOnClickListener(new View.OnClickListener() 
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
		
		TaskController.get().removeListener(mListener);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		
		TaskController.get().addListener(mListener);
		
		synchronizeLocal();
	}
	
	public void resetTitle(String gamertag)
	{
		mGamertag = gamertag;
		
		synchronizeLocal();
	}
	
	private void synchronizeLocal()
	{
		if (mGamertag != null)
		{
	        if (mGamerProfile == null)
	        	synchronizeWithServer();
		}
		
		loadFriendDetails();
		
        if (android.os.Build.VERSION.SDK_INT >= 11)
        	new HoneyCombHelper().invalidateMenu();
	}
	
	private void synchronizeWithServer()
	{
		if (mGamertag == null)
			return;
		
		TaskController.get().runCustomTask(null, new CustomTask<GamerProfileInfo>()
				{
					@Override
					public void runTask() throws AuthenticationException,
							IOException, ParserException
					{
						PsnParser p = new PsnUsParser(getActivity());
						
						try
						{
							setResult(p.fetchGamerProfile(mAccount, 
									mGamertag));
						}
						finally
						{
							p.dispose();
						}
					}
				}, mListener);
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
		
		if (mGamerProfile == null)
		{
			container.findViewById(R.id.unselected).setVisibility(View.VISIBLE);
			container.findViewById(R.id.profile_contents).setVisibility(View.GONE);
		}
		else
		{
			container.findViewById(R.id.unselected).setVisibility(View.GONE);
			container.findViewById(R.id.profile_contents).setVisibility(View.VISIBLE);
			
			int platinum = mGamerProfile.PlatinumTrophies;
			int gold = mGamerProfile.GoldTrophies;
			int silver = mGamerProfile.SilverTrophies;
			int bronze = mGamerProfile.BronzeTrophies;
			
			String fullStatus = getString(R.string.status_unknown);
			
			TextView tv;
			tv = (TextView)container.findViewById(R.id.profile_gamertag);
			tv.setText(mGamerProfile.OnlineId);
			
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
			tv.setText(String.valueOf(mGamerProfile.Level));
			tv = (TextView)container.findViewById(R.id.progress_ind);
			tv.setText(String.valueOf(mGamerProfile.Progress));
			
			ProgressBar pb = (ProgressBar)container.findViewById(R.id.progress_bar);
			pb.setProgress(mGamerProfile.Progress);
			
			String imageUrl = mAccount.getLargeAvatar(mGamerProfile.AvatarUrl);
			ImageCache ic = ImageCache.get();
			
			Bitmap bmp;
			ImageView iv;
			
			iv = (ImageView)container.findViewById(R.id.profile_plus);
			iv.setVisibility(View.GONE);
			
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
}
