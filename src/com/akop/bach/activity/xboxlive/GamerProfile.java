/*
 * GamerProfile.java 
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

package com.akop.bach.activity.xboxlive;

import java.io.IOException;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.akop.bach.IAccount;
import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.CachePolicy;
import com.akop.bach.ImageCache.OnImageReadyListener;
import com.akop.bach.Preferences;
import com.akop.bach.R;
import com.akop.bach.SupportsFriends;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.CustomTask;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.XboxLive.BeaconInfo;
import com.akop.bach.XboxLive.Friends;
import com.akop.bach.XboxLive.GamerProfileInfo;
import com.akop.bach.parser.AuthenticationException;
import com.akop.bach.parser.ParserException;
import com.akop.bach.parser.XboxLiveParser;

public class GamerProfile
		extends RibbonedActivity
		implements OnImageReadyListener
{
	private static final int starViews[] = { 
		R.id.profile_rep_star0,
		R.id.profile_rep_star1,
		R.id.profile_rep_star2,
		R.id.profile_rep_star3,
		R.id.profile_rep_star4,
	};
	
	private static final int starResources[] = { 
		R.drawable.xbox_star_o0,
		R.drawable.xbox_star_o1,
		R.drawable.xbox_star_o2,
		R.drawable.xbox_star_o3,
		R.drawable.xbox_star_o4,
	};
	
	private static CachePolicy sCp = 
		new CachePolicy(CachePolicy.SECONDS_IN_HOUR * 4);
	
	private class ViewHolder
	{
		TextView gamertag;
		TextView gamerScore;
		ImageView avatar;
		ImageView avatarBody;
		TextView currentActivity;
		TextView name;
		TextView location;
		TextView bio;
		TextView motto;
	}
	
	private static final int DIALOG_CONFIRM_ADD = 1;
	
	private String mGamertag;
	private ViewHolder mView;
	private GamerProfileInfo mInfo;
	
	private TaskListener mListener = new TaskListener()
	{
		@Override
		public void onAllTasksCompleted()
		{
			mHandler.showThrobber(false);
		}
		
		@Override
		public void onTaskFailed(IAccount account, Exception e)
		{
			mHandler.showError(e);
		}
		
		@Override
		public void onTaskStarted()
		{
			mHandler.showThrobber(true);
		}
		
		@Override
		public void onTaskSucceeded(IAccount account, Object requestParam, Object result) 
		{
			if (result instanceof GamerProfileInfo)
			{
				mInfo = (GamerProfileInfo)result;
				mGamertag = mInfo.Gamertag;
				
				mHandler.refresh();
			}
		}
	};
    
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.xbl_friend_summary);
		
        if ((mGamertag = getIntent().getStringExtra("gamertag")) == null)
        {
        	finish();
        	return;
        }
        
        mGamertag = mGamertag.trim();
        if (mGamertag.equalsIgnoreCase(mAccount.getGamertag()))
        {
        	AccountSummary.actionShow(this, mAccount);
        	finish();
        }
        
        long friendId = Friends.getFriendId(this, mAccount, mGamertag); 
        if (friendId >= 0)
        {
        	// Already a friend; open friend summary
        	FriendSummary.actionShow(this, friendId);
        	finish();
        }
        
        mInfo = null;
		if (savedInstanceState != null && savedInstanceState.containsKey("info"))
        	mInfo = (GamerProfileInfo)savedInstanceState.getSerializable("info");
		
		setTitle(mGamertag);
		
		mView = new ViewHolder();
		mView.gamertag = (TextView)findViewById(R.id.profile_gamertag);
		mView.gamerScore = (TextView)findViewById(R.id.profile_points);
		mView.avatar = (ImageView)findViewById(R.id.profile_avatar);
		mView.avatarBody = (ImageView)findViewById(R.id.profile_avatar_body);
		mView.currentActivity = (TextView)findViewById(R.id.profile_info);
		mView.name = (TextView)findViewById(R.id.profile_name);
		mView.location = (TextView)findViewById(R.id.profile_location);
		mView.bio = (TextView)findViewById(R.id.profile_bio);
		mView.motto = (TextView)findViewById(R.id.profile_motto);
		
		findViewById(R.id.compose_message).setVisibility((mAccount.isGold())
				? View.VISIBLE : View.GONE);
		
		findViewById(R.id.compose_message).setOnClickListener(new View.OnClickListener() 
		{
			@Override
			public void onClick(View v)
			{
				MessageCompose.actionComposeMessage(GamerProfile.this, mAccount, mGamertag);
			}
		});
		
		findViewById(R.id.compare_games).setOnClickListener(new View.OnClickListener() 
		{
			@Override
			public void onClick(View v)
			{
		    	CompareGames.actionShow(GamerProfile.this, mAccount, mGamertag);
			}
		});
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		if (mInfo != null)
			outState.putSerializable("info", mInfo);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		getMenuInflater().inflate(R.menu.xbl_gamer_profile, menu);
	    return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		menu.setGroupVisible(R.id.menu_group_gold, mAccount.isGold());
		
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
	    switch (item.getItemId()) 
	    {
	    case R.id.menu_refresh:
	    	refreshProfile();
	    	return true;
	    case R.id.menu_add_friend:
			showDialog(DIALOG_CONFIRM_ADD);
			return true;
	    case R.id.menu_compose:
	    	if (mInfo != null)
				MessageCompose.actionComposeMessage(this, mAccount,
						mInfo.Gamertag);
	    	
	    	return true;
	    case R.id.menu_compare_games:
	    	if (mInfo != null)
				CompareGames.actionShow(this, mAccount, mInfo.Gamertag);
	    	
	    	return true;
	    }
	    return false;
	}
	
	@Override
	protected void onErrorDialogOk()
	{
		super.onErrorDialogOk();
		
		finish();
	}
	
	@Override
	protected Dialog onCreateDialog(int id)
	{
		Builder builder = new Builder(this);
		switch (id)
		{
		case DIALOG_CONFIRM_ADD:
			builder.setTitle(R.string.are_you_sure)
				.setPositiveButton(android.R.string.yes, new OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						if (mInfo != null)
						{
							TaskController.getInstance().addFriend(mAccount, 
									mInfo.Gamertag, null, mListener);
						
							mHandler.showToast(R.string.request_queued);
							finish();
						}
					}
				})
				.setNegativeButton(android.R.string.no, new OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
					}
				})
				.setIcon(android.R.drawable.ic_menu_help)
				.setMessage(getString(R.string.send_friend_request_to_f, 
						(mInfo != null) ? mInfo.Gamertag : mGamertag));
			
			return builder.create();
		}
		
		return super.onCreateDialog(id);
	}
	
	@Override
	protected void updateRibbon()
	{
		if (mAccount != null)
		{
			updateRibbon(mAccount.getGamertag(), mAccount.getIconUrl(),
					getString(R.string.gamer_f,
							(mInfo != null) ? mInfo.Gamertag : mGamertag));
		}
	}
	
	@Override
	protected void onRefresh()
	{
		if (mInfo != null)
		{
			setRibbonTitles(mAccount.getScreenName(),
					getString(R.string.gamer_f, mInfo.Gamertag));
			
			mView.gamertag.setText(mInfo.Gamertag);
			mView.gamerScore.setText(getString(R.string.x_f, mInfo.Gamerscore));
			
			Bitmap bmp;
			ImageCache ic = ImageCache.getInstance();
			
			String gamerpicUrl = mInfo.IconUrl;
			if (gamerpicUrl != null)
			{
				if ((bmp = ic.getCachedBitmap(gamerpicUrl)) != null)
					mView.avatar.setImageBitmap(bmp);
				if (ic.isExpired(gamerpicUrl, sCp))
					ic.requestImage(gamerpicUrl, this, 0, null, sCp);
			}
			
			String avatarUrl = XboxLiveParser.getAvatarUrl(mInfo.Gamertag);
			if (avatarUrl != null)
			{
				if ((bmp = ic.getCachedBitmap(avatarUrl)) != null)
					mView.avatarBody.setImageBitmap(bmp);
				if (ic.isExpired(avatarUrl, sCp))
					ic.requestImage(avatarUrl, this, 0, null, sCp);
			}
			
			mView.name.setText(mInfo.Name);
			mView.location.setText(mInfo.Location);
			mView.bio.setText(mInfo.Bio);
			mView.currentActivity.setText(mInfo.CurrentActivity);
			
			String motto = mInfo.Motto;
			
			mView.motto.setVisibility((motto == null || motto.length() < 1)
					? View.INVISIBLE : View.VISIBLE);
			mView.motto.setText(motto);
			
			int res;
			int rep = mInfo.Rep;
			
			for (int starPos = 0, j = 0, k = 4; starPos < 5; starPos++, j += 4, k += 4)
			{
				if (rep < j) res = 0;
				else if (rep >= k) res = 4;
				else res = rep - j;
				
				ImageView starView = (ImageView)findViewById(starViews[starPos]);
				starView.setImageResource(starResources[res]);
			}
			
			LinearLayout root = (LinearLayout)findViewById(R.id.beacon_list);
			root.removeAllViews();
			
			LayoutInflater inflater = getLayoutInflater();
			
			if (mInfo.Beacons != null)
			{
				for (BeaconInfo beacon: mInfo.Beacons)
				{
					final String title = beacon.TitleName;
					String boxartUrl = beacon.TitleBoxArtUrl;
					String message = beacon.Text;
					
					View item = inflater.inflate(R.layout.xbl_beacon_item, null);
					item.setOnClickListener(new View.OnClickListener()
					{
						@Override
						public void onClick(View v)
						{
							Context context = GamerProfile.this;
							
							if (mAccount.canSendMessages())
							{
								MessageCompose.actionComposeMessage(context, 
										mAccount, mGamertag, 
										getString(R.string.lets_play_f, title));
							}
							else
							{
								CompareGames.actionShow(context,
										mAccount, mGamertag);
							}
						}
					});
					
					root.addView(item);
					
					ImageView boxart = (ImageView)item.findViewById(R.id.title_boxart);
					bmp = ImageCache.getInstance().getCachedBitmap(boxartUrl);
					
					if (bmp != null)
						boxart.setImageBitmap(bmp);
					else
						ImageCache.getInstance().requestImage(boxartUrl, this, 0, boxartUrl);
					
					TextView titleName = (TextView)item.findViewById(R.id.title_name);
					titleName.setText(title);
					
					TextView beaconText = (TextView)item.findViewById(R.id.beacon_text);
					beaconText.setText(message);
				}
			}
		}
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		
        TaskController.getInstance().removeListener(mListener);
        ImageCache.getInstance().removeListener(this);
	}
	
	private void refreshProfile()
	{
		TaskController.getInstance().runCustomTask(null, new CustomTask<GamerProfileInfo>()
				{
					@Override
					public void runTask() throws AuthenticationException,
							IOException, ParserException
					{
						XboxLiveParser p = new XboxLiveParser(GamerProfile.this);
						
						try
						{
							setResult(p.fetchGamerProfile(mAccount, mGamertag));
						}
						finally
						{
							p.dispose();
						}
					}
				}, mListener);
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		
		TaskController.getInstance().addListener(mListener);
        ImageCache.getInstance().addListener(this);
        
        mHandler.showThrobber(TaskController.getInstance().isBusy());
        mAccount.refresh(Preferences.get(this));
        
        if (mInfo == null)
        	refreshProfile();
        
        onRefresh();
	}
	
	public static void actionShow(Context context,
			SupportsFriends account, String gamertag)
	{
		Intent intent = new Intent(context, GamerProfile.class);
		intent.putExtra("account", account);
		intent.putExtra("gamertag", gamertag);
		
		context.startActivity(intent);
	}
	
	@Override
	public void onImageReady(long id, Object param, Bitmap bmp)
	{
		mHandler.refresh();
	}
}
