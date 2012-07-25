/*
 * GameCatalogFragment.java 
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
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.akop.bach.App;
import com.akop.bach.IAccount;
import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.CachePolicy;
import com.akop.bach.PSN;
import com.akop.bach.PSN.GameCatalogItem;
import com.akop.bach.PSN.GameCatalogList;
import com.akop.bach.Preferences;
import com.akop.bach.PsnAccount;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.CustomTask;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.fragment.GenericFragment;
import com.akop.bach.fragment.playstation.GameCatalogFilterFragment.OnOkListener;
import com.akop.bach.parser.AuthenticationException;
import com.akop.bach.parser.Parser;
import com.akop.bach.parser.ParserException;
import com.akop.bach.parser.PsnParser;

public class GameCatalogFragment extends GenericFragment implements
		OnItemClickListener, OnOkListener
{
	public static interface OnItemSelectedListener
	{
		void onItemSelected(GameCatalogItem item);
	}
	
	private CachePolicy mCp = null;
	private MyAdapter mAdapter = null;
	private Handler mHandler = new Handler();
	private PsnAccount mAccount = null;
	private int mItemPos = -1;
	private ListView mListView = null;
	private TextView mMessage = null;
	private View mProgress = null;
	private ArrayList<GameCatalogItem> mItems = null;
	
	private TextView mCatalogFilter = null;
	private TextView mCatalogSort = null;
	
	private int mLastFetchedPage;
	private int mLastRequestedPage;
	private boolean mLoadMore;
	
	private int mConsole = PSN.CATALOG_CONSOLE_PS3;
	private int mSortOrder = PSN.CATALOG_SORT_BY_RELEASE;
	private int mReleaseStatus = PSN.CATALOG_RELEASE_COMING_SOON;
	
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
					
					mLoadMore = false;
					mListView.setEmptyView(mMessage);
					mProgress.setVisibility(View.GONE);
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
					if (result != null && result instanceof GameCatalogList)
					{
						GameCatalogList list = (GameCatalogList)result;
						
						appendToAdapter(list);
						synchronizeLocal();
					}
					
					mMessage.setText(getString(R.string.no_games_in_catalog));
					
					mListView.setEmptyView(mMessage);
					mProgress.setVisibility(View.GONE);
				}
			});
		}
	};
	
	private static class ViewHolder
	{
		public TextView title;
		public TextView overview;
		public TextView releaseDate;
		public TextView genre;
		public ImageView icon;
		public TextView publisher;
	}
	
	private class MyAdapter extends BaseAdapter
	{
		private static final int ITEM_NORMAL = 0;
		private static final int ITEM_PLACEHOLDER = 1;
		
		private List<GameCatalogItem> mItems;
		
		public MyAdapter(List<GameCatalogItem> items)
		{
			mItems = items;
		}
		
		@Override
		public int getCount()
		{
			return mItems.size();
		}
		
		@Override
		public Object getItem(int position)
		{
			return mItems.get(position);
		}
		
		@Override
		public long getItemId(int position)
		{
			return position;
		}
		
		@Override
		public int getViewTypeCount()
		{
			return 2;
		}
		
		@Override
		public boolean isEnabled(int position)
		{
			return (getItemViewType(position) != ITEM_PLACEHOLDER);
		}
		
		@Override
		public int getItemViewType(int position)
		{
			int lastItemPos = mItems.size() - 1;
			if (position == lastItemPos && mItems.get(lastItemPos) == null)
				return ITEM_PLACEHOLDER;
			
			return ITEM_NORMAL;
		}
		
		private void renderItem(ViewHolder vh, GameCatalogItem item)
		{
			vh.title.setText(item.Title);
			vh.genre.setText(item.Genre);
			vh.publisher.setText(item.Publisher);
			
			String releaseDate = item.ReleaseDate;
			/*
			if (item.ReleaseDateTicks != 0)
				releaseDate = DateFormat.getMediumDateFormat(getActivity())
				        .format(item.ReleaseDateTicks);
			*/
			vh.releaseDate.setVisibility(releaseDate != null ? View.VISIBLE : View.INVISIBLE);
			vh.releaseDate.setText(getString(R.string.release_date_f, releaseDate));
			vh.overview.setVisibility(item.Overview != null ? View.VISIBLE : View.INVISIBLE);
			vh.overview.setText(item.Overview);
			
			String iconUrl = item.BoxartUrl;
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
				Bitmap bmp = ImageCache.get().getCachedBitmap(iconUrl, mCp);
				if (bmp != null)
					mIconCache.put(iconUrl, new SoftReference<Bitmap>(bmp));
				
				vh.icon.setImageBitmap(bmp);
			}
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View row;
			ViewHolder vh;
			
			if (convertView == null)
			{
				LayoutInflater inflater = getLayoutInflater(null);
				
				if (getItemViewType(position) == ITEM_PLACEHOLDER)
				{
					if (mLastFetchedPage <= mLastRequestedPage)
						loadMore();
					
					return inflater.inflate(R.layout.psn_game_catalog_placeholder_item,
							parent, false);
				}
				else
				{
					row = inflater.inflate(R.layout.psn_game_catalog_item, parent, false);
					vh = new ViewHolder();
					
					vh.title = (TextView)row.findViewById(R.id.cat_item_title);
					vh.overview = (TextView)row.findViewById(R.id.cat_item_overview);
					vh.icon = (ImageView)row.findViewById(R.id.cat_item_boxart);
					vh.genre = (TextView)row.findViewById(R.id.cat_item_genre);
					vh.releaseDate = (TextView)row.findViewById(R.id.cat_item_relDate);
					vh.publisher = (TextView)row.findViewById(R.id.cat_item_publisher);
					
					row.setTag(vh);
				}
			}
			else
			{
				if (getItemViewType(position) == ITEM_PLACEHOLDER)
				{
					if (mLastFetchedPage <= mLastRequestedPage)
						loadMore();
					
					return convertView;
				}
				
				row = convertView;
				vh = (ViewHolder)row.getTag();
			}
			
			if (vh != null)
				renderItem(vh, (GameCatalogItem)getItem(position));
			
			return row;
		}
	}
	
	public static GameCatalogFragment newInstance(PsnAccount account)
	{
		GameCatalogFragment f = new GameCatalogFragment();
		
		Bundle args = new Bundle();
		args.putSerializable("account", account);
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
	    
	    mAccount = (PsnAccount)args.getSerializable("account");
	    mItemPos = -1;
		
		mLastRequestedPage = 0;
		mLastFetchedPage = 0;
		mLoadMore = true;
		
		mConsole = mAccount.getCatalogConsole();
		mSortOrder = mAccount.getCatalogSortOrder();
		mReleaseStatus = mAccount.getCatalogReleaseStatus();
		
		mItems = null;
		
	    if (state != null)
	    {
    		mSortOrder = state.getInt("sortOrder", mSortOrder);
    		mReleaseStatus = state.getInt("releaseStatus", mReleaseStatus);
			mConsole = state.getInt("console", mConsole);
    		
	    	try
	    	{
	    		mItemPos = state.getInt("itemPos", -1);
	    		mLastRequestedPage = state.getInt("reqedPage", 0);
	    		mLastFetchedPage = state.getInt("fetchPage", 0);
	    		mLoadMore = state.getBoolean("loadMore", true);
				mItems = (ArrayList<GameCatalogItem>)state.getSerializable("items");
	    	}
	    	catch(Exception e)
	    	{
	    		if (App.LOGV)
	    			e.printStackTrace();
	    		
	    		mItems = null;
	    		mItemPos = -1;
	    	}
		}
	    
	    if (mItems == null)
	    	mItems = new ArrayList<GameCatalogItem>();
	    
	    mAdapter = new MyAdapter(mItems);
	    
	    setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		if (container == null)
			return null;
		
		View layout = inflater.inflate(R.layout.psn_fragment_catalog_list,
				container, false);
		
		mMessage = (TextView)layout.findViewById(R.id.message);
		mMessage.setText(R.string.no_games_in_catalog);
		
		mListView = (ListView)layout.findViewById(R.id.list);
		mListView.setOnItemClickListener(this);
		mListView.setEmptyView(mMessage);
	    mListView.setAdapter(mAdapter);
		registerForContextMenu(mListView);
	    
	    mCatalogFilter = (TextView)layout.findViewById(R.id.catalog_filter);
	    mCatalogSort = (TextView)layout.findViewById(R.id.catalog_sort);
	    
	    View filterButton = layout.findViewById(R.id.filter_button);
	    if (filterButton != null)
	    {
		    filterButton.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					showFilterSelector();
				}
			});
	    }
	    
		mProgress = layout.findViewById(R.id.loading);
		
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
		
		outState.putInt("sortOrder", mSortOrder);
		outState.putInt("releaseStatus", mReleaseStatus);
		outState.putInt("console", mConsole);
		
		outState.putInt("itemPos", mItemPos);
		outState.putInt("reqedPage", mLastRequestedPage);
		outState.putInt("fetchPage", mLastFetchedPage);
		outState.putBoolean("loadMore", mLoadMore);
		outState.putSerializable("items", mItems);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
	    super.onCreateOptionsMenu(menu, inflater);
	    
    	inflater.inflate(R.menu.psn_game_catalog_list, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
	    switch (item.getItemId()) 
	    {
	    case R.id.menu_refresh:
	    	synchronizeWithServer();
	    	return true;
	    case R.id.menu_catalog_filter:
	    	showFilterSelector();
	    	return true;
	    }
	    
	    return false;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		
		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo)menuInfo;
		GameCatalogItem item = (GameCatalogItem)mAdapter.getItem(acmi.position);
		
		menu.setHeaderTitle(item.Title);
		
		getActivity().getMenuInflater().inflate(R.menu.psn_game_catalog_list_context, 
				menu);
		
	    menu.setGroupVisible(R.id.menu_group_remindable,
	    		GameCatalogDetailsFragment.isRemindable(item));
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem menuItem)
	{
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem
				.getMenuInfo();
		GameCatalogItem item = (GameCatalogItem)mAdapter.getItem(info.position);
		
		switch (menuItem.getItemId())
		{
	    case R.id.menu_catalog_reminder:
	    	GameCatalogDetailsFragment.addReminder(getActivity(), item);
	    	return true;
	    case R.id.menu_catalog_website:
	    	GameCatalogDetailsFragment.openSite(getActivity(), item);
	    	return true;
		}
		
		return super.onContextItemSelected(menuItem);
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		
		TaskController.get().removeListener(mListener);
		ImageCache.get().removeListener(this);
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		TaskController.get().addListener(mListener);
		ImageCache.get().addListener(this);
        
		synchronizeLocal();
		
		if (mItems.size() < 1)
			synchronizeWithServer();
		else if (mLastRequestedPage > mLastFetchedPage)
		{
			mLastRequestedPage--; // try again
			loadMore();
		}
	}
	
	@Override
	public void onImageReady(final long id, Object param, Bitmap bmp)
	{
		super.onImageReady(id, param, bmp);
		
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mAdapter.notifyDataSetChanged();
			}
		});
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id)
	{
		mItemPos = pos;
		mListView.setItemChecked(pos, true);
		
		GameCatalogItem item = (GameCatalogItem)mAdapter.getItem(pos);
		notifyItemSelected(item);
	}
	
	private void notifyItemSelected(GameCatalogItem item)
	{
		if (getActivity() instanceof OnItemSelectedListener)
		{
			OnItemSelectedListener listener = (OnItemSelectedListener)getActivity();
			listener.onItemSelected(item);
		}
	}
	
	private void showFilterSelector()
	{
		GameCatalogFilterFragment frag = GameCatalogFilterFragment.newInstance(mAccount);
		frag.setOnOkListener(this);
    	frag.show(getFragmentManager(), null);
	}
	
	private void appendToAdapter(GameCatalogList list)
	{
		synchronized (this)
		{
			if (mItems.size() > 0)
				mItems.remove(mItems.size() - 1);
			
			for (GameCatalogItem item : list.Items)
			{
				mItems.add(item);
				
				try
				{
					String iconUrl = item.BoxartUrl;
					SoftReference<Bitmap> cachedIcon = mIconCache.get(iconUrl);
					
					if (cachedIcon == null || cachedIcon.get() == null)
					{
						if (!ImageCache.get().isCached(item.BoxartUrl, mCp))
							ImageCache.get().requestImage(iconUrl, this, 
									0, iconUrl, false, mCp);
					}
				}
				catch(Exception e)
				{
					if (App.LOGV)
						e.printStackTrace();
				}
			}
			
			mLoadMore = list.MorePages;
			//if (list.Items.size() < list.PageSize)
			//	mLoadMore = false;
			
			if (mLoadMore)
				mItems.add(null); // Placeholder
			
			mAdapter.notifyDataSetChanged();
			
			mLastFetchedPage = list.PageNumber;
		}
		
		syncIcons();
	}
	
	private void synchronizeLocal()
	{
		if (mCatalogFilter != null)
		{
			ArrayList<Object> filters = new ArrayList<Object>();
			
			if (mConsole == PSN.CATALOG_CONSOLE_PS3)
				filters.add(getString(R.string.console_ps3));
			else if (mConsole == PSN.CATALOG_CONSOLE_PS2)
				filters.add(getString(R.string.console_ps2));
			else if (mConsole == PSN.CATALOG_CONSOLE_PSP)
				filters.add(getString(R.string.console_psp));
			else if (mConsole == PSN.CATALOG_CONSOLE_PSVITA)
				filters.add(getString(R.string.console_psvita));
			else
				filters.add(getString(R.string.console_unknown));
			
			if (mAccount.supportsFilteringByReleaseDate())
			{
				if (mReleaseStatus == PSN.CATALOG_RELEASE_OUT_NOW)
					filters.add(getString(R.string.catalog_filtered_by_rs_out_now));
				else if (mReleaseStatus == PSN.CATALOG_RELEASE_COMING_SOON)
					filters.add(getString(R.string.catalog_filtered_by_rs_soon));
			}
			
			mCatalogFilter.setText(getString(R.string.catalog_filter_f, 
					Parser.joinString(filters, ", ")));
		}
		
		if (mCatalogSort != null)
		{
			String sortOrder = "";
			if (mSortOrder == PSN.CATALOG_SORT_BY_ALPHA)
				sortOrder = getString(R.string.catalog_sorted_by_title);
			else if (mSortOrder == PSN.CATALOG_SORT_BY_RELEASE)
				sortOrder = getString(R.string.catalog_sorted_by_reldate);
			
			mCatalogSort.setText(sortOrder);
		}
	}
	
	private void loadMore()
	{
		if (!mLoadMore)
			return;
		
		if (App.LOGV)
			App.logv("synchronizeWithServer: [R: " + mLastRequestedPage 
					+ "; F: " + mLastFetchedPage + "]");
		
		if (mLastRequestedPage == mLastFetchedPage + 1)// && TaskController.get().isBusy())
			return;
		
		mListView.setEmptyView(mProgress);
		mMessage.setVisibility(View.GONE);
		
		mLastRequestedPage = mLastFetchedPage + 1;
		TaskController.get().runCustomTask(mAccount, new CustomTask<GameCatalogList>()
				{
					@Override
					public void runTask() throws AuthenticationException,
							IOException, ParserException
					{
						PsnParser p = mAccount.createLocaleBasedParser(getActivity());
						
						try
						{
							GameCatalogList list = p.fetchGameCatalog(mAccount, 
									mConsole, mLastRequestedPage,
									mReleaseStatus, mSortOrder);
							setResult(list);
						}
						finally
						{
							p.dispose();
						}
					}
				}, mListener);
	}
	
	private void synchronizeWithServer()
	{
		mLastRequestedPage = 0;
		mLastFetchedPage = 0;
		mLoadMore = true;
		
		mItems.clear();
		mAdapter.notifyDataSetChanged();
		
		notifyItemSelected(null);
		loadMore();
	}

	@Override
    public void okClicked(int console, int releaseStatusFilter, int sortFilter)
    {
		mConsole = console;
		mReleaseStatus = releaseStatusFilter;
		mSortOrder = sortFilter;
		mItemPos = -1;
		mListView.setItemChecked(-1, true);
		
		mAccount.refresh(Preferences.get(getActivity()));
		
		synchronizeLocal();
		synchronizeWithServer();
    }
}
