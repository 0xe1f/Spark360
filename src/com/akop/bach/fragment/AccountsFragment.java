/*
 * AccountsFragment.java 
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

package com.akop.bach.fragment;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.akop.bach.App;
import com.akop.bach.ImageCache;
import com.akop.bach.PSN;
import com.akop.bach.R;
import com.akop.bach.SerializableMatrixCursor;
import com.akop.bach.XboxLive;

public class AccountsFragment extends GenericFragment implements
		OnItemClickListener
{
	public static interface OnAccountSelectedListener
	{
		void onNewAccount();
		void onAccountSelected(String uuid, Uri uri);
	}
	
	public static final class UniversalProfileCursor extends SerializableMatrixCursor
	{
		private static class AccountInfoComparator implements Comparator<AccountInfo>
		{
			@Override
			public int compare(AccountInfo object1, AccountInfo object2)
			{
				if (object1.uuid == null)
					return -1;
				else if (object2.uuid == null)
					return 1;
				return object1.screenName.toLowerCase().compareTo(
						object2.screenName.toLowerCase());
			}
		}
		
		private static class AccountInfo
		{
			public String screenName;
			public String description;
			public String iconUrl;
			public String uuid;
			public Uri uri;
			
			public AccountInfo(String screenName,
					String description,
					String iconUrl,
					String uuid,
					Uri uri)
			{
				this.screenName = screenName;
				this.description = description;
				this.iconUrl = iconUrl;
				this.uuid = uuid;
				this.uri = uri;
			}
		}
		
        private static final long serialVersionUID = -2972426843033896844L;
        
		public static final Uri CONTENT_URI = Uri.parse("content://com.akop.bach.profileprovider/uni_profile");
		
		public static final String _ID = BaseColumns._ID;
		public static final String UUID = "Uid"; 
		public static final String SCREEN_NAME = "ScreenName"; 
		public static final String DESCRIPTION = "Description"; 
		public static final String AVATAR_URL = "AvatarUrl";
		public static final String ACCOUNT_URI = "Uri";
		public static final String ICON_URL = "IconUrl";
		
		public static final int COLUMN_UUID = 1; 
		public static final int COLUMN_SCREEN_NAME = 2; 
		public static final int COLUMN_DESCRIPTION = 3; 
		public static final int COLUMN_AVATAR_URL = 4;
		public static final int COLUMN_URI = 5;
		public static final int COLUMN_ICON_URL = 6;
		
		private static final String[] KEYS = 
		{
			_ID,
			UUID,
			SCREEN_NAME,
			DESCRIPTION,
			AVATAR_URL,
			ACCOUNT_URI,
			ICON_URL,
		};
		
		public UniversalProfileCursor(Context context)
        {
			super(KEYS, 20);
			
			setNotificationUri(context.getContentResolver(), CONTENT_URI);
			
			for (AccountInfo info: listAccounts(context))
				addItem(info.uuid, info.screenName, info.description, 
						info.iconUrl, info.uri, info.iconUrl);
        }
		
		private AccountInfo[] listAccounts(Context context)
		{
			List<AccountInfo> accountList = new ArrayList<AccountInfo>();
			Cursor c = null;
			
			try
			{
				c = context.getContentResolver().query(PSN.Profiles.CONTENT_URI, 
						new String[] { 
							"Uuid",
							"AccountId",
							"OnlineId",
							"IconUrl",
						},
						null, null, null);
			}
			catch(Exception e)
			{
			}
			
			if (c != null)
			{
				try
				{
					while (c.moveToNext())
					{
						accountList.add(new AccountInfo(c.getString(2),
								context.getString(R.string.playstation_network),
								c.getString(3), 
								c.getString(0),
								ContentUris.withAppendedId(PSN.Profiles.CONTENT_URI, 
										c.getLong(1))));
					}
				}
				finally
				{
					c.close();
				}
			}
			
			c = null;
			
			try
			{
				c = context.getContentResolver().query(XboxLive.Profiles.CONTENT_URI, 
						new String[] { 
							"Uuid",
							"AccountId",
							"Gamertag",
							"IconUrl",
						},
						null, null, null);
			}
			catch(Exception e)
			{
			}
			
			if (c != null)
			{
				try
				{
					while (c.moveToNext())
					{
						accountList.add(new AccountInfo(c.getString(2),
								context.getString(R.string.xbox_live),
								c.getString(3), 
								c.getString(0),
								ContentUris.withAppendedId(XboxLive.Profiles.CONTENT_URI, 
										c.getLong(1))));
					}
				}
				finally
				{
					c.close();
				}
			}
			
			AccountInfo[] infos = new AccountInfo[accountList.size()];
			accountList.toArray(infos);
			Arrays.sort(infos, new AccountInfoComparator());
			
			return infos;
		}
		
		public void addItem(String uid, String screenName, 
				String description, String avatarUrl, Uri uri, String iconUrl)
		{
			this.newRow()
				.add(getCount())
				.add(uid)
				.add(screenName)
				.add(description)
				.add(avatarUrl)
				.add(uri.toString())
				.add(iconUrl);
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
    
	private class ViewHolder
	{
		TextView gamertag;
		TextView description;
		ImageView avatarIcon;
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
			View view = li.inflate(R.layout.account_list_item, parent, false);
			view.setTag(vh);
			
	        vh.gamertag = (TextView)view.findViewById(R.id.accounts_gamertag);
	        vh.description = (TextView)view.findViewById(R.id.accounts_description);
			vh.avatarIcon = (ImageView)view.findViewById(R.id.accounts_avatar_icon);
			
			return view;
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			ViewHolder vh = (ViewHolder)view.getTag();
			
			vh.gamertag.setText(cursor.getString(UniversalProfileCursor.COLUMN_SCREEN_NAME));
			vh.description.setText(cursor.getString(UniversalProfileCursor.COLUMN_DESCRIPTION));
			
			String iconUrl = cursor.getString(UniversalProfileCursor.COLUMN_AVATAR_URL);
			SoftReference<Bitmap> icon = mIconCache.get(iconUrl);
			
			if (icon != null && icon.get() != null)
			{
				// Image is in the in-memory cache
				vh.avatarIcon.setImageBitmap(icon.get());
			}
			else
			{
				// Image has likely been garbage-collected
				// Load it into the cache again
				Bitmap bmp = ImageCache.getInstance().getCachedBitmap(iconUrl);
				if (bmp != null)
				{
					mIconCache.put(iconUrl, new SoftReference<Bitmap>(bmp));
					vh.avatarIcon.setImageBitmap(bmp);
				}
				else
				{
					// Image failed to load - just use placeholder
					vh.avatarIcon.setImageResource(R.drawable.avatar_default);
				}
			}
		}
	}
	
	public static AccountsFragment newInstance()
	{
		AccountsFragment f = new AccountsFragment();
		
		Bundle args = new Bundle();
		//args.putSerializable("account", account);
		f.setArguments(args);
		
		return f;
	}
	
	private int mTitlePos = -1;
	private TextView mMessage = null;
	private ListView mListView = null;
	
	private MyCursorAdapter mAdapter = null;
	private Handler mHandler = new Handler();
	private UniversalProfileCursor mCursor = null;
	private IconCursor mIconCursor = null;
	
    @Override
	public void onCreate(Bundle state)
	{
	    super.onCreate(state);
	    
	    // Bundle args = getArguments();
	    
	    mTitlePos = -1;
	    mCursor = null;
	    mIconCursor = null;
	    mAdapter = null;
		
	    if (state != null)
	    {
	    	try
	    	{
				if (state.containsKey("cursor"))
					mCursor = (UniversalProfileCursor)state.getSerializable("cursor");
				
				if (state.containsKey("icons"))
					mIconCursor = (IconCursor)state.getSerializable("icons");
				
				mTitlePos = state.getInt("titleId");
	    	}
	    	catch(Exception e)
	    	{
	    		if (App.getConfig().logToConsole())
	    			e.printStackTrace();
	    		
	    		mCursor = null;
	    		mIconCursor = null;
	    		mTitlePos = -1;
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
		
		View layout = inflater.inflate(R.layout.xbl_fragment_account_list,
				container, false);
		
		mMessage = (TextView)layout.findViewById(R.id.message);
		mMessage.setVisibility(View.GONE);
		
		mListView = (ListView)layout.findViewById(R.id.list);
		mListView.setOnItemClickListener(this);
		mListView.setEmptyView(mMessage);
		
		View view = layout.findViewById(R.id.new_account);
		if (view != null)
		{
			view.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (getActivity() instanceof OnAccountSelectedListener)
					{
						((OnAccountSelectedListener)getActivity()).onNewAccount();
					}
				}
			});
		}
		
		initializeAdapter();
		
		registerForContextMenu(mListView);
		
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
		
		if (mCursor != null)
			outState.putSerializable("cursor", mCursor);
		
		if (mIconCursor != null)
			outState.putSerializable("icons", mIconCursor);
		
		outState.putInt("titleId", mTitlePos);
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		
		ContentResolver cr = getActivity().getContentResolver();
        cr.unregisterContentObserver(mObserver);
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		ContentResolver cr = getActivity().getContentResolver();
		
		cr.registerContentObserver(PSN.Profiles.CONTENT_URI, true, mObserver);
		cr.registerContentObserver(XboxLive.Profiles.CONTENT_URI, true, mObserver);
		
		synchronizeLocal();
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
					Uri uri = ContentUris.withAppendedId(UniversalProfileCursor.CONTENT_URI, id);
					getActivity().getContentResolver().notifyChange(uri, null);
				}
			});
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
    public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3)
    {
		Cursor c = (Cursor)arg0.getItemAtPosition(pos);
		mTitlePos = pos;
		
		if (c != null && getActivity() instanceof OnAccountSelectedListener)
		{
			OnAccountSelectedListener listener = (OnAccountSelectedListener)getActivity();
			listener.onAccountSelected(c.getString(UniversalProfileCursor.COLUMN_UUID),
					Uri.parse(c.getString(UniversalProfileCursor.COLUMN_URI)));
		}
    }
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		
		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo)menuInfo;
		ViewHolder vh = (ViewHolder)acmi.targetView.getTag();
		
		menu.setHeaderTitle(vh.gamertag.getText());
		
		getActivity().getMenuInflater().inflate(R.menu.accounts_context, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem menuItem)
	{
		AdapterView.AdapterContextMenuInfo info = 
			(AdapterView.AdapterContextMenuInfo)menuItem.getMenuInfo();
		
		Cursor c = (Cursor)mAdapter.getItem(info.position);
		Uri uri = Uri.parse(c.getString(UniversalProfileCursor.COLUMN_URI));
		
		if (menuItem.getItemId() == R.id.menu_delete_account)
		{
			deleteAccount(uri);
			return true;
		}
		else if (menuItem.getItemId() == R.id.menu_edit_account)
		{
			editAccount(uri);
			return true;
		}
		
		return super.onContextItemSelected(menuItem);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
	    super.onCreateOptionsMenu(menu, inflater);
	    
	    inflater.inflate(R.menu.accounts, menu);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu)
	{
	    super.onPrepareOptionsMenu(menu);
	    
		menu.setGroupVisible(R.id.menu_group_single_pane, !mDualPane);	
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
	    if (item.getItemId() == R.id.menu_new_account)
		{
	    	if (getActivity() instanceof OnAccountSelectedListener)
	    		((OnAccountSelectedListener)getActivity()).onNewAccount();
	    	
			return true;
		}
	    else if (item.getItemId() == R.id.menu_about)
		{
			((App)getActivity().getApplication()).showAboutDialog(getActivity());
			return true;
		}
	    /*
		else if (item.getItemId() == R.id.menu_refresh)
		{
			synchronizeLocal();
			return true;
		}
		*/
	    
	    return false;
	}
	
	private void initializeAdapter()
	{
		synchronized (this)
		{
			mCursor = new UniversalProfileCursor(getActivity());
			IconCursor ic = new IconCursor();
			int pos = mTitlePos;
			
			try
			{
				while (mCursor.moveToNext())
				{
					ic.newRow()
						.add(mCursor.getLong(0))
						.add(mCursor.getString(UniversalProfileCursor.COLUMN_AVATAR_URL));
				}
			}
			catch(Exception e)
			{
				if (App.getConfig().logToConsole())
					e.printStackTrace();
			}
			
			mIconCursor = ic;
			mAdapter = new MyCursorAdapter(getActivity(), mCursor);
			mListView.setAdapter(mAdapter);
			
			if (pos > -1)
				mListView.setItemChecked(pos, true);
		}
	}
	
	private void synchronizeLocal()
	{
		initializeAdapter();
		
		syncIcons();
	}
	
	private void editAccount(Uri uri)
	{
		try
		{
			Intent intent = new Intent(Intent.ACTION_EDIT, uri);
	    	startActivity(intent);
		}
		catch(ActivityNotFoundException ex)
		{
			AlertDialogFragment frag = AlertDialogFragment.newInstance(0,
					getString(R.string.error),
					getString(R.string.account_edit_error), 0);
			frag.show(getFragmentManager(), "dialog");
		}
	}
	
	private void deleteAccount(Uri uri)
	{
		try
		{
			Intent intent = new Intent(Intent.ACTION_DELETE, uri);
	    	startActivity(intent);
		}
		catch(ActivityNotFoundException ex)
		{
			AlertDialogFragment frag = AlertDialogFragment.newInstance(0,
					getString(R.string.error),
					getString(R.string.account_delete_error), 0);
			frag.show(getFragmentManager(), "dialog");
		}
	}
}
