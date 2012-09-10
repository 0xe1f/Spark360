/*
 * PsBlog.java 
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

package com.akop.bach.activity.playstation;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.akop.bach.PsnAccount;
import com.akop.bach.R;
import com.akop.bach.fragment.playstation.BlogEntriesFragment;
import com.akop.bach.fragment.playstation.BlogEntriesFragment.OnBlogEntrySelectedListener;
import com.akop.bach.fragment.playstation.BlogEntryViewFragment;
import com.akop.bach.util.rss.RssItem;

public class PsBlog extends PsnMultiPane implements
		OnBlogEntrySelectedListener
{
	@Override
	protected Fragment instantiateDetailFragment()
	{
		return BlogEntryViewFragment.newInstance();
	}
	
	@Override
	protected Fragment instantiateTitleFragment()
	{
		return BlogEntriesFragment.newInstance(getAccount());
	}
	
	@Override
	public void onEntrySelected(RssItem item)
	{
		if (isDualPane())
		{
			BlogEntryViewFragment detailFragment = (BlogEntryViewFragment)mDetailFragment;
			detailFragment.resetTitle(item);
		}
		else
		{
			PsBlogEntry.actionShow(this, getAccount(), item);
		}
	}
	
	public static void actionShow(Context context, PsnAccount account)
	{
		Intent intent = new Intent(context, PsBlog.class);
		intent.putExtra("account", account);
		context.startActivity(intent);
	}
	
	@Override
	protected String getSubtitle()
	{
		return getString(R.string.playstation_blog);
	}
}
