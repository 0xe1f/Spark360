/*
 * BlogEntryViewFragment.java 
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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.akop.bach.R;
import com.akop.bach.fragment.GenericFragment;
import com.akop.bach.util.rss.RssItem;

public class BlogEntryViewFragment extends GenericFragment
{
	private RssItem mItem;
	
	public static BlogEntryViewFragment newInstance()
	{
		return newInstance(null);
	}
	
	public static BlogEntryViewFragment newInstance(RssItem item)
	{
		BlogEntryViewFragment f = new BlogEntryViewFragment();
		
		Bundle args = new Bundle();
		args.putParcelable("item", item);
		f.setArguments(args);
		
		return f;
	}
	
	@Override
	public void onCreate(Bundle state)
	{
		super.onCreate(state);
		
	    Bundle args = getArguments();
	    
		mItem = (RssItem)args.getParcelable("item");
		
		if (state != null)
		{
			if (state.containsKey("item"))
				mItem = (RssItem)state.getParcelable("item");
		}
		
		setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		if (container == null)
			return null;
		
		View layout = inflater.inflate(R.layout.psn_fragment_blog_entry_view,
				container, false);
		
		TextView title = (TextView)layout.findViewById(R.id.entry_title);
		if (title != null)
		{
			title.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (mItem != null && mItem.link != null)
					{
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse(mItem.link));
						
						startActivity(intent);
					}
				}
			});
		}
		
		return layout;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		if (mItem != null)
			outState.putParcelable("item", mItem);
	}
	
	public void resetTitle(RssItem item)
	{
		if (mItem == null || mItem.link != item.link)
		{
			mItem = item;
			
			synchronizeLocal();
			
	        if (android.os.Build.VERSION.SDK_INT >= 11)
	        	new HoneyCombHelper().invalidateMenu();
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
	    super.onCreateOptionsMenu(menu, inflater);
	    
	    inflater.inflate(R.menu.psn_blog_entry_view, menu);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu)
	{
	    super.onPrepareOptionsMenu(menu);
	    
	    menu.setGroupVisible(R.id.menu_group_selected, mItem != null);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
	    switch (item.getItemId()) 
	    {
	    case R.id.menu_open_link:
	    	if (mItem != null)
	    	{
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(mItem.link));
				
				startActivity(intent);
	    	}
	    	
	    	return true;
	    }
	    
	    return false;
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		synchronizeLocal();
	}
	
	private void synchronizeLocal()
	{
		View parent = getView();
		if (parent == null)
			return;
		
		TextView message = (TextView)parent.findViewById(R.id.unselected);
		
		if (mItem == null)
		{
			message.setVisibility(View.VISIBLE);
			parent.findViewById(R.id.selected).setVisibility(View.GONE);
		}
		else
		{
			message.setVisibility(View.GONE);
			parent.findViewById(R.id.selected).setVisibility(View.VISIBLE);
			
			TextView title = (TextView)parent.findViewById(R.id.entry_title);
			TextView posted = (TextView)parent.findViewById(R.id.entry_date);
			
			if (title != null)
				title.setText(mItem.title);
			if (posted != null)
				posted.setText(getString(R.string.blog_posted_f,
						DateFormat.getMediumDateFormat(getActivity()).format(mItem.date),
						DateFormat.getTimeFormat(getActivity()).format(mItem.date), mItem.author));
			
			WebView webView = (WebView)parent.findViewById(R.id.webview);
			webView.loadDataWithBaseURL(null, mItem.content, "text/html", "UTF-8", null);
		}
	}
}
