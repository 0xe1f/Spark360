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

package com.akop.bach.fragment.xboxlive;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
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
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.CachePolicy;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.XboxLive;
import com.akop.bach.XboxLive.Beacons;
import com.akop.bach.XboxLive.Friends;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.activity.xboxlive.CompareGames;
import com.akop.bach.activity.xboxlive.FriendsOfFriendList;
import com.akop.bach.activity.xboxlive.MessageCompose;
import com.akop.bach.fragment.AlertDialogFragment;
import com.akop.bach.fragment.AlertDialogFragment.OnOkListener;
import com.akop.bach.fragment.GenericFragment;
import com.akop.bach.fragment.xboxlive.FriendsFragment.RequestInformation;
import com.akop.bach.parser.XboxLiveParser;

public class FriendProfileFragment extends GenericFragment implements OnOkListener
{
	private static final int DIALOG_CONFIRM_REMOVE = 1;
	private TaskListener mListener = new TaskListener();
	private static CachePolicy sCp = new CachePolicy(CachePolicy.SECONDS_IN_HOUR * 4);
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
	
	private XboxLiveAccount mAccount;
	private long mTitleId = -1;
	private String mGamertag;
	
	private class ViewHolder
	{
		TextView gamertag;
		TextView gamerScore;
		ImageView avatar;
		ImageView avatarBody;
		TextView status;
		TextView info;
		TextView name;
		TextView location;
		TextView bio;
		TextView motto;
		View rep;
		LinearLayout beaconRoot;
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
    
	public static FriendProfileFragment newInstance(XboxLiveAccount account)
	{
		return newInstance(account, -1);
	}
	
	public static FriendProfileFragment newInstance(XboxLiveAccount account,
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
		    
		    mAccount = (XboxLiveAccount)args.getSerializable("account");
		    mTitleId = args.getLong("titleId", -1);
		}
		
	    if (state != null)
	    {
			mAccount = (XboxLiveAccount)state.getSerializable("account");
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
		
		View layout = inflater.inflate(R.layout.xbl_fragment_friend_summary,
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
		cr.registerContentObserver(Beacons.CONTENT_URI, true, mObserver);
		
		synchronizeLocal();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		outState.putParcelable("account", mAccount);
		outState.putLong("titleId", mTitleId);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
	    super.onCreateOptionsMenu(menu, inflater);
	    
    	inflater.inflate(R.menu.xbl_friend_summary, menu);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);
		
		if (mTitleId < 0)
		{
			menu.setGroupVisible(R.id.menu_group_selected, false);
			menu.setGroupVisible(R.id.menu_group_invite_rcvd, false);
			menu.setGroupVisible(R.id.menu_group_invite_sent, false);
			menu.setGroupVisible(R.id.menu_group_friend, false);
			menu.setGroupVisible(R.id.menu_group_gold, false);
		}
		else
		{
			menu.setGroupVisible(R.id.menu_group_selected, true);
			
			int statusCode = Friends.getStatusCode(getActivity(), mTitleId);
			
			if (statusCode == XboxLive.STATUS_INVITE_RCVD)
			{
				menu.setGroupVisible(R.id.menu_group_invite_rcvd, true);
				menu.setGroupVisible(R.id.menu_group_invite_sent, false);
				menu.setGroupVisible(R.id.menu_group_friend, false);
				menu.setGroupVisible(R.id.menu_group_gold, false);
			}
			else if (statusCode == XboxLive.STATUS_INVITE_SENT)
			{
				menu.setGroupVisible(R.id.menu_group_invite_rcvd, false);
				menu.setGroupVisible(R.id.menu_group_invite_sent, true);
				menu.setGroupVisible(R.id.menu_group_friend, false);
				menu.setGroupVisible(R.id.menu_group_gold, false);
			}
			else
			{
				menu.setGroupVisible(R.id.menu_group_invite_rcvd, false);
				menu.setGroupVisible(R.id.menu_group_invite_sent, false);
				menu.setGroupVisible(R.id.menu_group_friend, true);
				menu.setGroupVisible(R.id.menu_group_gold, mAccount.isGold());
			}
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		TaskController controller = TaskController.getInstance();
		
	    switch (item.getItemId()) 
	    {
	    /*
	    case R.id.menu_refresh:
	    	TaskController.get().updateFriendProfile(mAccount, 
	    			mGamertag, mListener);
	    	return true;
	    */
	    case R.id.menu_compose:
			MessageCompose.actionComposeMessage(getActivity(), mAccount, mGamertag);
	    	return true;
	    /*
	    case R.id.menu_block_friend:
			showDialog(DIALOG_CONFIRM_BLOCK);
			mAlert.setMessage(getString(R.string.block_communications_from_q_f, 
					mGamertag));
	    	return true;
	    */
	    case R.id.menu_remove_friend:
			AlertDialogFragment frag = AlertDialogFragment.newInstance(DIALOG_CONFIRM_REMOVE,
					getString(R.string.are_you_sure),
					getString(R.string.remove_from_friends_q_f, mGamertag), 
					mTitleId);
			frag.setOnOkListener(this);
			frag.show(getFragmentManager(), "dialog");
	    	return true;
	    case R.id.menu_view_friends:
			FriendsOfFriendList.actionShow(getActivity(), 
					mAccount, mGamertag);
	    	return true;
	    case R.id.menu_compare_games:
	    	CompareGames.actionShow(getActivity(), mAccount, mGamertag);
	    	return true;
		case R.id.menu_accept_friend:
			mHandler.showToast(getString(R.string.request_queued));
			controller.acceptFriendRequest(mAccount, mGamertag, 
					new RequestInformation(R.string.accepted_friend_request_from_f, 
							mGamertag),
					mListener);
			return true;
		case R.id.menu_reject_friend:
			mHandler.showToast(getString(R.string.request_queued));
			controller.rejectFriendRequest(mAccount, mGamertag, 
					new RequestInformation(R.string.declined_friend_request_from_f, 
							mGamertag),
					mListener);
			return true;
		case R.id.menu_cancel_friend:
			mHandler.showToast(getString(R.string.request_queued));
			controller.cancelFriendRequest(mAccount, mGamertag, 
					new RequestInformation(R.string.cancelled_friend_request_to_f, 
							mGamertag),
					mListener);
			return true;
	    }
	    return false;
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
					new String[] { Friends.GAMERTAG, Friends.LAST_UPDATED }, 
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
			
			ViewHolder holder = new ViewHolder();
			
			holder.gamertag = (TextView)container.findViewById(R.id.profile_gamertag);
			holder.gamerScore = (TextView)container.findViewById(R.id.profile_points);
			holder.avatar = (ImageView)container.findViewById(R.id.profile_avatar);
			holder.avatarBody = (ImageView)container.findViewById(R.id.profile_avatar_body);
			holder.status = (TextView)container.findViewById(R.id.profile_status);
			holder.info = (TextView)container.findViewById(R.id.profile_info);
			holder.name = (TextView)container.findViewById(R.id.profile_name);
			holder.location = (TextView)container.findViewById(R.id.profile_location);
			holder.bio = (TextView)container.findViewById(R.id.profile_bio);
			holder.motto = (TextView)container.findViewById(R.id.profile_motto);
			holder.rep = container.findViewById(R.id.profile_rep);
			holder.beaconRoot = (LinearLayout)container.findViewById(R.id.beacon_list);
			
			ContentResolver cr = getActivity().getContentResolver();
			Cursor c = cr.query(ContentUris.withAppendedId(Friends.CONTENT_URI, 
					mTitleId), 
					new String[] { 
						Friends.REP,
						Friends.GAMERTAG,
						Friends.GAMERSCORE,
						Friends.ICON_URL,
						Friends.NAME,
						Friends.LOCATION,
						Friends.BIO,
						Friends.STATUS,
						Friends.CURRENT_ACTIVITY,
						Friends.LAST_UPDATED,
						Friends.STATUS_CODE,
						Friends.MOTTO,
					},
					null, null, null);
			
			if (c != null)
			{
				try
				{
					if (c.moveToFirst())
					{
						//setTitle(c.getString(1));
						
						holder.gamertag.setText(c.getString(1));
						holder.gamerScore.setText(getString(R.string.x_f, c.getInt(2)));
						
						Bitmap bmp;
						ImageCache ic = ImageCache.getInstance();
						
						String gamerpicUrl = c.getString(3);
						if (gamerpicUrl != null)
						{
							if ((bmp = ic.getCachedBitmap(gamerpicUrl)) != null)
								holder.avatar.setImageBitmap(bmp);
							if (ic.isExpired(gamerpicUrl, sCp))
								ic.requestImage(gamerpicUrl, this, 0, null, sCp);
						}
						
						String avatarUrl = XboxLiveParser.getAvatarUrl(mGamertag);
						if (avatarUrl != null)
						{
							if ((bmp = ic.getCachedBitmap(avatarUrl)) != null)
								holder.avatarBody.setImageBitmap(bmp);
							if (ic.isExpired(avatarUrl, sCp))
								ic.requestImage(avatarUrl, this, 0, null, sCp);
						}
						
						holder.name.setText(c.getString(4));
						holder.location.setText(c.getString(5));
						holder.bio.setText(c.getString(6));
						holder.status.setText(c.getString(7));
						holder.status.setTag(c.getInt(10));
						holder.info.setText(c.getString(8));
						
						String motto = c.getString(11);
						
						holder.motto.setVisibility((motto == null || motto.length() < 1)
								? View.INVISIBLE : View.VISIBLE);
						holder.motto.setText(motto);
						
						int res;
						int rep = c.getInt(0);
						
						for (int starPos = 0, j = 0, k = 4; starPos < 5; starPos++, j += 4, k += 4)
						{
							if (rep < j) res = 0;
							else if (rep >= k) res = 4;
							else res = rep - j;
							
							ImageView starView = (ImageView)holder.rep.findViewById(starViews[starPos]);
							starView.setImageResource(starResources[res]);
						}
					}
				}
				finally
				{
					c.close();
				}
			}
			
			c = cr.query(Beacons.CONTENT_URI, 
					new String[] {
						Beacons._ID,
						Beacons.TITLE_NAME,
						Beacons.TITLE_BOXART,
						Beacons.TEXT,
					},
					Beacons.ACCOUNT_ID + "=" + mAccount.getId() + " AND " + 
					Beacons.FRIEND_ID + "=" + mTitleId, 
					null, null);
			
			LinearLayout root = holder.beaconRoot;
			root.removeAllViews();
			
			LayoutInflater inflater = getActivity().getLayoutInflater();
			
			if (c != null)
			{
				try
				{
					while (c.moveToNext())
					{
						final String title = c.getString(1);
						String boxartUrl = c.getString(2);
						String message = c.getString(3);
						
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
						
						root.addView(item);
						
						ImageView boxart = (ImageView)item.findViewById(R.id.title_boxart);
						Bitmap bmp = ImageCache.getInstance().getCachedBitmap(boxartUrl);
						
						if (bmp != null)
							boxart.setImageBitmap(bmp);
						else
							ImageCache.getInstance().requestImage(boxartUrl, this, c.getLong(0), boxartUrl);
						
						TextView titleName = (TextView)item.findViewById(R.id.title_name);
						titleName.setText(title);
						
						TextView beaconText = (TextView)item.findViewById(R.id.beacon_text);
						beaconText.setText(message);
					}
				}
				finally
				{
					c.close();
				}
			}
		}
	}
	
	@Override
    public void okClicked(int code, long id, String param)
    {
		if (code == DIALOG_CONFIRM_REMOVE)
		{
			mHandler.showToast(getString(R.string.request_queued));
			TaskController.getInstance()
					.removeFriend(mAccount, mGamertag,
							new RequestInformation(R.string.removed_friend_from_friend_list_f, mGamertag),
							mListener);
		}
    }
}
