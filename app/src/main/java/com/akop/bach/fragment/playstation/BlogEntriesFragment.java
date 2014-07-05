/*
 * BlogEntriesFragment.java 
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

package com.akop.bach.fragment.playstation;

import java.lang.ref.SoftReference;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.akop.bach.Account;
import com.akop.bach.App;
import com.akop.bach.ImageCache;
import com.akop.bach.PsnAccount;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.CustomTask;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.fragment.GenericFragment;
import com.akop.bach.parser.Parser;
import com.akop.bach.util.rss.RssChannel;
import com.akop.bach.util.rss.RssItem;

public class BlogEntriesFragment extends GenericFragment implements
		OnItemClickListener
{
	public static interface OnBlogEntrySelectedListener
	{
		void onEntrySelected(String channelUrl, RssItem item);
	}
	
	private BaseAdapter mAdapter;
	private RssChannel mPayload = null;
	private IconCursor mIconCursor = null;
	private PsnAccount mAccount = null;
	private int mTitleIndex = -1;
	private ListView mListView = null;
	private TextView mMessage = null;
	private View mProgress = null;
	
	private TaskListener mListener = new TaskListener("PsnBlog")
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
						mMessage.setText(Parser.getErrorMessage(getActivity(), e));
					
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
					if (result != null && result instanceof RssChannel)
					{
						mPayload = (RssChannel)result;
						
						initializeAdapter();
						synchronizeLocal();
					}
					
					mMessage.setText(getString(R.string.blog_empty));
					
					mListView.setEmptyView(mMessage);
					mProgress.setVisibility(View.GONE);
				}
			});
		}
	};
	
	private class BlogItemAdapter extends ArrayAdapter<RssItem>
	{
		private java.text.DateFormat mMedDate = DateFormat.getMediumDateFormat(getActivity());
		private java.text.DateFormat mShortTime = DateFormat.getTimeFormat(getActivity());
		
		private class ViewHolder
		{
			TextView title;
			//TextView description;
			TextView posted;
			ImageView thumb;
		}
		
		public BlogItemAdapter(Context context, List<RssItem> entries)
		{
			super(context, R.layout.psn_blog_item, entries);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			LinearLayout row;
			ViewHolder vh;
			
			if (convertView == null)
			{
				row = (LinearLayout)getActivity().getLayoutInflater().inflate(R.layout.psn_blog_item, parent, false);
				
				vh = new ViewHolder();
				vh.title = (TextView) row.findViewById(R.id.blog_item_title);
				//vh.description = (TextView)row.findViewById(R.id.blog_item_content);
				vh.posted = (TextView) row.findViewById(R.id.blog_item_posted);
				vh.thumb = (ImageView) row.findViewById(R.id.blog_item_thumb);

				row.setTag(vh);
			}
			else
			{
				row = (LinearLayout) convertView;
				vh = (ViewHolder) row.getTag();
			}
			
			RssItem info = getItem(position);
			
			vh.title.setText(info.title);
			vh.posted.setText(getString(R.string.blog_posted_f,
					mMedDate.format(info.date), mShortTime.format(info.date),
			        info.author));
			//vh.description.setText(Html.fromHtml(info.description).toString());
			vh.title.setTag(info.link);
			
			String iconUrl = info.thumbUrl;
			SoftReference<Bitmap> icon = mIconCache.get(iconUrl);
			
			if (icon != null && icon.get() != null)
			{
				// Image is in the in-memory cache
				vh.thumb.setImageBitmap(icon.get());
			}
			else
			{
				// Image has likely been garbage-collected
				// Load it into the cache again
				Bitmap bmp = ImageCache.getInstance().getCachedBitmap(iconUrl);
				if (bmp != null)
					mIconCache.put(iconUrl, new SoftReference<Bitmap>(bmp));
				
				vh.thumb.setImageBitmap(bmp);
			}
			
			return row;
		}
	}
	
	public static BlogEntriesFragment newInstance(PsnAccount account)
	{
		BlogEntriesFragment f = new BlogEntriesFragment();
		
		Bundle args = new Bundle();
		args.putParcelable("account", account);
		f.setArguments(args);
		
		return f;
	}
	
    @Override
	public void onCreate(Bundle state)
	{
	    super.onCreate(state);
	    
	    Bundle args = getArguments();
	    
	    mAccount = (PsnAccount)args.getParcelable("account");
	    mIconCursor = null;
	    mAdapter = null;
		mPayload = null;
	    
	    if (state != null)
	    {
	    	try
	    	{
				mTitleIndex = state.getInt("titleIndex");
				if (state.containsKey("icons"))
					mIconCursor = (IconCursor)state.getSerializable("icons");
	    	    if (state.containsKey("feed"))
	    	    	mPayload = (RssChannel)state.getParcelable("feed");
	    	}
	    	catch(Exception e)
	    	{
	    		mTitleIndex = -1;
	    		mPayload = null;
	    		mIconCursor = null;
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
		
		View layout = inflater.inflate(R.layout.psn_fragment_plain_list,
				container, false);
		
		mMessage = (TextView)layout.findViewById(R.id.message);
		mMessage.setText(R.string.blog_empty);
		
		mListView = (ListView)layout.findViewById(R.id.list);
		mListView.setOnItemClickListener(this);
		mListView.setEmptyView(mMessage);
		
		registerForContextMenu(mListView);
		initializeAdapter();
		
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
		
		if (mAccount != null)
		{
			outState.putParcelable("account", mAccount);
			outState.putInt("titleIndex", mTitleIndex);
			
			if (mPayload != null)
				outState.putParcelable("feed", mPayload);
			if (mIconCursor != null)
				outState.putSerializable("icons", mIconCursor);
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		
		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo)menuInfo;
		
		TextView title = (TextView)acmi.targetView.findViewById(R.id.blog_item_title);
		if (title != null)
			menu.setHeaderTitle(title.getText());
		
		getActivity().getMenuInflater().inflate(R.menu.psn_blog_entry_list_context, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem menuItem)
	{
		AdapterView.AdapterContextMenuInfo info = 
			(AdapterView.AdapterContextMenuInfo)menuItem.getMenuInfo();
		
		RssItem item = (RssItem)mAdapter.getItem(info.position);
		if (item != null && item.link != null)
		{
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(item.link));
			
			startActivity(intent);
		}
		
		return super.onContextItemSelected(menuItem);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
	    super.onCreateOptionsMenu(menu, inflater);
	    
	    inflater.inflate(R.menu.psn_blog, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
	    switch (item.getItemId()) 
	    {
	    case R.id.menu_refresh:
	    	synchronizeWithServer();
	    	return true;
	    }
	    
	    return false;
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		
		TaskController.getInstance().removeListener(mListener);
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		TaskController.getInstance().addListener(mListener);
        
		synchronizeLocal();
		
		if (mPayload == null)
			synchronizeWithServer();
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
					mAdapter.notifyDataSetChanged();
				}
			});
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id)
	{
		if (getActivity() instanceof OnBlogEntrySelectedListener)
		{
			OnBlogEntrySelectedListener listener = (OnBlogEntrySelectedListener)getActivity();
			
			listener.onEntrySelected(mPayload.link, (RssItem)mAdapter.getItem(pos));
		}
	}
	
	private void initializeAdapter()
	{
		synchronized (this)
		{
			if (mPayload != null)
			{
				IconCursor ic = new IconCursor();
				
				try
				{
					for (RssItem item: mPayload.items)
					{
						if (item.thumbUrl != null)
						{
							ic.newRow()
								.add(ic.getCount())
								.add(item.thumbUrl);
						}
					}
				}
				catch(Exception e)
				{
					if (App.getConfig().logToConsole())
						e.printStackTrace();
				}
				
				mIconCursor = ic;
				mAdapter = new BlogItemAdapter(getActivity(), mPayload.items);
				mListView.setAdapter(mAdapter);
			}
		}
	}
	
	private void synchronizeLocal()
	{
		syncIcons();
	}
	
	private void synchronizeWithServer()
	{
		mListView.setEmptyView(mProgress);
		mMessage.setVisibility(View.GONE);
		
		TaskController.getInstance().runCustomTask(null, new CustomTask<RssChannel>()
				{
					@Override
					public void runTask() throws Exception
					{
						setResult(mAccount.getBlog(getActivity()));
					}
				}, mListener);
	}
	
	@Override
    protected Cursor getIconCursor()
    {
		if (getActivity() == null)
			return null;
		
		return mIconCursor;
    }
}
