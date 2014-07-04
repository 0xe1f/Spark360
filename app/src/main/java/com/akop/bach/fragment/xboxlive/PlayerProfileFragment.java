/*
 * PlayerProfileFragment.java 
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

package com.akop.bach.fragment.xboxlive;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.akop.bach.Account;
import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.CachePolicy;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.CustomTask;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.XboxLive.BeaconInfo;
import com.akop.bach.XboxLive.GamerProfileInfo;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.activity.xboxlive.CompareGames;
import com.akop.bach.activity.xboxlive.MessageCompose;
import com.akop.bach.fragment.AlertDialogFragment;
import com.akop.bach.fragment.AlertDialogFragment.OnOkListener;
import com.akop.bach.fragment.GenericFragment;
import com.akop.bach.fragment.xboxlive.FriendsFragment.RequestInformation;
import com.akop.bach.parser.AuthenticationException;
import com.akop.bach.parser.Parser;
import com.akop.bach.parser.ParserException;
import com.akop.bach.parser.XboxLiveParser;

import java.io.IOException;

public class PlayerProfileFragment extends GenericFragment implements
        OnOkListener
{
	private static final int DIALOG_CONFIRM_ADD = 1;
	private static final int DIALOG_CONFIRM_REMOVE = 2;
	
	private TaskListener mRequestListener = new TaskListener()
	{
		@Override
		public void onTaskFailed(Account account, Exception e)
		{
			mHandler.showToast(Parser.getErrorMessage(getActivity(), e));
		}
		
		@Override
		public void onTaskSucceeded(Account account, Object requestParam, Object result) 
		{
			// Update friends
			TaskController.getInstance().updateFriendList(mAccount, mListener);
			
			// Show toast
			if (requestParam instanceof RequestInformation)
			{
				RequestInformation ri = (RequestInformation)requestParam;
				mHandler.showToast(getActivity().getString(ri.resId, ri.gamertag));
			}
		}
	};
	
	private TaskListener mListener = new TaskListener()
	{
		@Override
		public void onTaskFailed(Account account, final Exception e)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					mHandler.showToast(XboxLiveParser.getErrorMessage(getActivity(), e));
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
					if (result != null && result instanceof GamerProfileInfo)
					{
						GamerProfileInfo info = (GamerProfileInfo)result;
						if (mGamertag == null || mGamertag.equalsIgnoreCase(info.Gamertag))
						{
							mPayload = info;
							synchronizeLocal();
						}
					}
				}
			});
		}
	};
	
	private static CachePolicy sCp = new CachePolicy(CachePolicy.SECONDS_IN_HOUR * 4);

	private XboxLiveAccount mAccount;
	private String mGamertag;
	private GamerProfileInfo mPayload;
	
	private class ViewHolder
	{
		TextView gamertag;
		TextView gamerScore;
		ImageView avatar;
		ImageView avatarBody;
		TextView info;
		LinearLayout beaconRoot;
	}
	
	public static PlayerProfileFragment newInstance(XboxLiveAccount account)
	{
		return newInstance(account, null);
	}
	
	public static PlayerProfileFragment newInstance(XboxLiveAccount account,
			GamerProfileInfo info)
	{
		PlayerProfileFragment f = new PlayerProfileFragment();
		
		Bundle args = new Bundle();
		args.putParcelable("account", account);
		args.putParcelable("info", info);
		f.setArguments(args);
		
		return f;
	}
	
	@Override
	public void onCreate(Bundle state)
	{
		super.onCreate(state);
		
	    Bundle args = getArguments();
	    
	    mAccount = (XboxLiveAccount)args.getParcelable("account");
	    mPayload = (GamerProfileInfo)args.getParcelable("info");
	    mGamertag = null;

	    if (mPayload != null)
	    {
	    	mGamertag = mPayload.Gamertag;
	    }
		
	    if (state != null)
	    {
    		mGamertag = state.getString("gamertag");
    		mPayload = (GamerProfileInfo)state.getParcelable("info");
	    }
	    
		setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		if (container == null)
			return null;
		
		View layout = inflater.inflate(R.layout.xbl_fragment_friend_summary,
				container, false);
		
		View composeButton = layout.findViewById(R.id.compose_message);
		composeButton.setVisibility(mAccount.canSendMessages() 
				? View.VISIBLE : View.INVISIBLE);
		composeButton.setOnClickListener(new View.OnClickListener() 
		{
			@Override
			public void onClick(View v)
			{
				MessageCompose.actionComposeMessage(getActivity(), mAccount, mGamertag);
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
		TaskController.getInstance().removeListener(mRequestListener);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		
		TaskController.getInstance().addListener(mListener);
		TaskController.getInstance().addListener(mRequestListener);
		
		synchronizeLocal();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		outState.putParcelable("info", mPayload);
		outState.putString("gamertag", mGamertag);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
	    super.onCreateOptionsMenu(menu, inflater);
	    
	    inflater.inflate(R.menu.xbl_gamer_profile, menu);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);
		
		menu.setGroupVisible(R.id.menu_group_selected, mGamertag != null);
		menu.setGroupVisible(R.id.menu_group_is_friend, 
				mGamertag != null && mPayload != null && mPayload.IsFriend);
		menu.setGroupVisible(R.id.menu_group_not_friend, 
				mGamertag != null && mPayload != null && !mPayload.IsFriend);
		menu.setGroupVisible(R.id.menu_group_gold, 
				mGamertag != null && mAccount.isGold());
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (mGamertag == null)
			return false;
		
	    switch (item.getItemId()) 
	    {
	    case R.id.menu_add_friend:
		    {
				AlertDialogFragment frag = AlertDialogFragment.newInstance(DIALOG_CONFIRM_ADD, 
						getString(R.string.are_you_sure),
						getString(R.string.send_friend_request_to_f, mGamertag), 
						mGamertag);
				
				frag.setOnOkListener(this);
				frag.show(getFragmentManager(), "dialog");
				
				return true;
		    }
	    case R.id.menu_remove_friend:
		    {
				AlertDialogFragment frag = AlertDialogFragment.newInstance(DIALOG_CONFIRM_REMOVE, 
						getString(R.string.are_you_sure),
						getString(R.string.remove_from_friends_q_f, mGamertag), 
						mGamertag);
				
				frag.setOnOkListener(this);
				frag.show(getFragmentManager(), "dialog");
				
				return true;
		    }
	    case R.id.menu_compose:
	    	
			MessageCompose.actionComposeMessage(getActivity(), mAccount, mGamertag);
	    	
	    	return true;
	    	
	    case R.id.menu_compare_games:
	    	
			CompareGames.actionShow(getActivity(), mAccount, mGamertag);
	    	
	    	return true;
	    }
	    
	    return false;
	}
	
	public void resetTitle(GamerProfileInfo info)
	{
		if (mGamertag != info.Gamertag)
		{
			mGamertag = info.Gamertag;
			mPayload = info;
		}
		
		synchronizeLocal();
	}
	
	private void synchronizeLocal()
	{
		View view = getView();
		if (view == null)
			return;
		
		if (mPayload == null)
		{
			view.findViewById(R.id.unselected).setVisibility(View.VISIBLE);
			view.findViewById(R.id.profile_contents).setVisibility(View.GONE);
		}
		else
		{
			view.findViewById(R.id.unselected).setVisibility(View.GONE);
			view.findViewById(R.id.profile_contents).setVisibility(View.VISIBLE);
			
			ViewHolder holder = new ViewHolder();
			
			holder.gamertag = (TextView)view.findViewById(R.id.profile_gamertag);
			holder.gamerScore = (TextView)view.findViewById(R.id.profile_points);
			holder.avatar = (ImageView)view.findViewById(R.id.profile_avatar);
			holder.avatarBody = (ImageView)view.findViewById(R.id.profile_avatar_body);
			holder.info = (TextView)view.findViewById(R.id.profile_info);
			holder.beaconRoot = (LinearLayout)view.findViewById(R.id.beacon_list);
			
			holder.gamertag.setText(mPayload.Gamertag);
			holder.gamerScore.setText(getString(R.string.x_f, mPayload.Gamerscore));
			
			Bitmap bmp;
			ImageCache ic = ImageCache.getInstance();
			
			String gamerpicUrl = mPayload.IconUrl;
			if (gamerpicUrl != null)
			{
				if ((bmp = ic.getCachedBitmap(gamerpicUrl)) != null)
					holder.avatar.setImageBitmap(bmp);
				if (ic.isExpired(gamerpicUrl, sCp))
					ic.requestImage(gamerpicUrl, this, 0, null, sCp);
			}
			
			String avatarUrl = XboxLiveParser.getAvatarUrl(mPayload.Gamertag);
			if (avatarUrl != null)
			{
				if ((bmp = ic.getCachedBitmap(avatarUrl)) != null)
					holder.avatarBody.setImageBitmap(bmp);
				if (ic.isExpired(avatarUrl, sCp))
					ic.requestImage(avatarUrl, this, 0, null, sCp);
			}
			
			holder.info.setText(mPayload.CurrentActivity);
			holder.beaconRoot.removeAllViews();
			
			LayoutInflater inflater = getActivity().getLayoutInflater();
			
			if (mPayload.Beacons != null)
			{
				for (BeaconInfo beacon: mPayload.Beacons)
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
							Context context = getActivity();
							
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
					
					holder.beaconRoot.addView(item);
					
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
		
        if (android.os.Build.VERSION.SDK_INT >= 11)
        	new HoneyCombHelper().invalidateMenu();
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
    public void okClicked(int code, long id, String param)
    {
		if (code == DIALOG_CONFIRM_ADD)
		{
			mHandler.showToast(getString(R.string.request_queued));
			TaskController.getInstance().addFriend(mAccount, param,
					new RequestInformation(R.string.added_friend_to_friend_list_f,
							param), mRequestListener);
		}
		else if (code == DIALOG_CONFIRM_REMOVE)
		{
			mHandler.showToast(getString(R.string.request_queued));
			TaskController.getInstance().removeFriend(mAccount, param,
					new RequestInformation(R.string.removed_friend_from_friend_list_f,
							param), mRequestListener);
		}
    }
}
