/*
 * RssItem.java
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

package com.akop.bach.util.rss;

import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;

public class RssItem implements Parcelable
{
	public String title = "";
	public Date date;
	public String description = "";
	public String link;
	public String author = "";
	public String thumbUrl;
	public String content = "";
	
	public static final Parcelable.Creator<RssItem> CREATOR = new Parcelable.Creator<RssItem>() 
	{
		public RssItem createFromParcel(Parcel in) 
		{
			return new RssItem(in);
		}
		
		public RssItem[] newArray(int size) 
		{
			return new RssItem[size];
		}
	};
	
	public RssItem()
	{
		this.title = "";
		this.description = "";
		this.author = "";
		this.content = "";
	}
	
	private RssItem(Parcel in) 
	{
		this.title = in.readString();
		this.date = new Date(in.readLong());
		this.description = in.readString();
		this.link = in.readString();
		this.author = in.readString();
		this.thumbUrl = in.readString();
		this.content = in.readString();
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) 
	{
		dest.writeString(this.title);
		dest.writeLong(this.date.getTime());
		dest.writeString(this.description);
		dest.writeString(this.link);
		dest.writeString(this.author);
		dest.writeString(this.thumbUrl);
		dest.writeString(this.content);
	}
	
	@Override
	public int describeContents() 
	{
		return 0;
	}
}