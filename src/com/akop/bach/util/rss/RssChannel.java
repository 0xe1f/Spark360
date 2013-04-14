/*
 * RssChannel.java
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

package com.akop.bach.util.rss;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class RssChannel implements Parcelable
{
	public String title;
	public String description;
	public String link;
	public ArrayList<RssItem> items;
	
	public static final Parcelable.Creator<RssChannel> CREATOR = new Parcelable.Creator<RssChannel>() 
	{
		public RssChannel createFromParcel(Parcel in) 
		{
			return new RssChannel(in);
		}
		
		public RssChannel[] newArray(int size) 
		{
			return new RssChannel[size];
		}
	};
	
	public RssChannel()
	{
		this.items = new ArrayList<RssItem>();
	}
	
	private RssChannel(Parcel in) 
	{
		this.items = new ArrayList<RssItem>();
		
		this.title = in.readString();
		this.description = in.readString();
		this.link = in.readString();
		in.readTypedList(this.items, RssItem.CREATOR);
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) 
	{
		dest.writeString(this.title);
		dest.writeString(this.description);
		dest.writeString(this.link);
		dest.writeTypedList(this.items);
	}
	
	@Override
	public int describeContents() 
	{
		return 0;
	}
}