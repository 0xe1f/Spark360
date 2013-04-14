/*
 * SectionedAdapter.java
 * Copyright (C) 2010-2013 Akop Karapetyan
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

package com.akop.bach;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class SectionedAdapter
		extends BaseAdapter
{
	private static class HeaderContainer
	{
		private String mTemplate;
		private Adapter mAdapter;
		
		public HeaderContainer(String template, Adapter adapter)
		{
			mTemplate = template;
			mAdapter = adapter;
		}
		
		@Override
		public String toString()
		{
			if (mTemplate == null || mAdapter == null)
				return null;
			
			return String.format(mTemplate, mAdapter.getCount()); 
		}
	}
	
	private final List<TextView> mEmptyViews;
	private final Map<HeaderContainer, Adapter> mSections;
	private final ArrayAdapter<HeaderContainer> mHeaders;
	private final Context mContext;
	private final static int TYPE_SECTION_HEADER = 0;
	private final static int TYPE_SECTION_EMPTY = 1;
	
	public SectionedAdapter(Context context, int layout)
	{
		mContext = context;
		mEmptyViews = new ArrayList<TextView>();
		mSections = new LinkedHashMap<HeaderContainer, Adapter>();
		mHeaders = new ArrayAdapter<HeaderContainer>(context, layout);
	}
	
	public void addSection(String section, Adapter adapter)
	{
		TextView tv = new TextView(mContext);
		tv.setVisibility(View.GONE);
		tv.setHeight(0);
		
		HeaderContainer hc = new HeaderContainer(section, adapter);
		
		this.mEmptyViews.add(tv);
		this.mHeaders.add(hc);
		this.mSections.put(hc, adapter);
	}
	
	public Object getItem(int position)
	{
		for (Object section : this.mSections.keySet())
		{
			Adapter adapter = mSections.get(section);
			int size = adapter.getCount() + 1;
			
			// check if position inside this section
			if (position == 0)
				return section;
			if (position < size)
				return adapter.getItem(position - 1);
			
			// otherwise jump into next section
			position -= size;
		}
		
		return null;
	}
	
	public int getCount()
	{
		// total together all sections, plus one for each section header
		int total = 0;
		for (Adapter adapter : this.mSections.values())
			total += adapter.getCount() + 1;
		
		return total;
	}
	
	public int getViewTypeCount()
	{
		// assume that headers count as one, then total all sections
		int total = 2;
		for (Adapter adapter : this.mSections.values())
			total += adapter.getViewTypeCount();
		
		return total;
	}
	
	public int getItemViewType(int position)
	{
		int type = 2;
		for (Object section : this.mSections.keySet())
		{
			Adapter adapter = mSections.get(section);
			int size = adapter.getCount() + 1;
			
			// check if position inside this section
			if (position == 0)
			{
				if (size <= 1)
					return TYPE_SECTION_EMPTY;
				
				return TYPE_SECTION_HEADER;
			}
			
			if (position < size)
				return type + adapter.getItemViewType(position - 1);
			
			// otherwise jump into next section
			position -= size;
			type += adapter.getViewTypeCount();
		}
		
		return -1;
	}
	
	public boolean areAllItemsSelectable()
	{
		return false;
	}
	
	public boolean isEnabled(int position)
	{
		return (getItemViewType(position) != TYPE_SECTION_HEADER);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		int sectionnum = 0;
		for (Object section : this.mSections.keySet())
		{
			Adapter adapter = mSections.get(section);
			int size = adapter.getCount() + 1;
			
			// check if position inside this section
			if (position == 0)
			{
				if (size <= 1)
					return mEmptyViews.get(sectionnum);
				
				return mHeaders.getView(sectionnum, convertView, parent);
			}
			
			if (position < size)
				return adapter.getView(position - 1, convertView, parent);
			
			// otherwise jump into next section
			position -= size;
			sectionnum++;
		}
		
		return null;
	}
	
	@Override
	public long getItemId(int position)
	{
		return position;
	}
}
