/*
 * RssHandler.java
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

import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.akop.bach.App;

public class RssHandler extends DefaultHandler
{
	private RssChannel mChannel;
	private RssItem mCurrent;
	
	private int mState;
	private static final int STATE_IN_TITLE = (1 << 12);
	private static final int STATE_IN_DESC = (1 << 13);
	private static final int STATE_IN_LINK = (1 << 14);
	
	private static final int STATE_IN_ITEM = (1 << 2);
	private static final int STATE_IN_ITEM_TITLE = (1 << 3);
	private static final int STATE_IN_ITEM_LINK = (1 << 4);
	//private static final int STATE_IN_ITEM_DESC = (1 << 5);
	private static final int STATE_IN_ITEM_DATE = (1 << 6);
	private static final int STATE_IN_ITEM_AUTHOR = (1 << 7);
	private static final int STATE_IN_ITEM_THUMB = (1 << 8);
	private static final int STATE_IN_ITEM_CONTENT = (1 << 9);
	
	private static HashMap<String, Integer> mStateMap;

	static
	{
		mStateMap = new HashMap<String, Integer>();
		
		mStateMap.put("title", new Integer(STATE_IN_ITEM_TITLE));
		mStateMap.put("link", new Integer(STATE_IN_ITEM_LINK));
		//mStateMap.put("description", new Integer(STATE_IN_ITEM_DESC));
		//mStateMap.put("content", new Integer(STATE_IN_ITEM_DESC));
		mStateMap.put("content:encoded", new Integer(STATE_IN_ITEM_CONTENT));
		
		mStateMap.put("dc:date", new Integer(STATE_IN_ITEM_DATE));
		mStateMap.put("updated", new Integer(STATE_IN_ITEM_DATE));
		mStateMap.put("pubDate", new Integer(STATE_IN_ITEM_DATE));
		
		mStateMap.put("dc:author", new Integer(STATE_IN_ITEM_AUTHOR));
		mStateMap.put("dc:creator", new Integer(STATE_IN_ITEM_AUTHOR));
		mStateMap.put("author", new Integer(STATE_IN_ITEM_AUTHOR));
		mStateMap.put("thumbnail_url", new Integer(STATE_IN_ITEM_THUMB));
	}
	
	private RssHandler(String url)
	{
		mState = 0;
		mChannel = new RssChannel();
		mChannel.link = url;
	}
	
	public static RssChannel getFeed(String rssUrl) throws Exception
	{
		RssHandler handler = new RssHandler(rssUrl);
		
		if (App.LOGV)
			App.logv("RssHandler: Fetching %s", rssUrl);
		
	    long started = System.currentTimeMillis();
	    
	    try
	    {
			URL url = new URL(rssUrl);
			URLConnection c = url.openConnection();
			
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			
			xr.setContentHandler(handler);
			xr.setErrorHandler(handler);
			xr.parse(new InputSource(c.getInputStream()));
	    }
	    finally
	    {
			if (App.LOGV)
			{
				App.logv("RssHandler: Completed (%.02f s)", 
						(System.currentTimeMillis() - started) / 1000.0);
			}
	    }
	    
		return handler.mChannel;
	}
	
	public void startElement(String uri, String name, String qName,
			Attributes attrs)
	{
		if (qName.equals("item"))
		{
			mState = STATE_IN_ITEM;
			
			mCurrent = new RssItem();
			mChannel.items.add(mCurrent);
			
			return;
		}
		
		if ((mState & STATE_IN_ITEM) == 0)
		{
			if (qName.equals("title"))
				mState = STATE_IN_TITLE;
			else if (qName.equals("link"))
				mState = STATE_IN_LINK;
			else if (qName.equals("description"))
				mState = STATE_IN_DESC;
		}
		else
		{
			if (mStateMap.containsKey(qName))
				mState = STATE_IN_ITEM | mStateMap.get(qName).intValue();
		}
	}
	
	public void endElement(String uri, String name, String qName)
	{
		if ((mState & STATE_IN_ITEM) != 0)
			mState = STATE_IN_ITEM;
		else
			mState = 0;
	}
	
	public void characters(char ch[], int start, int length)
	{
		switch (mState)
		{
		case STATE_IN_TITLE:
			mChannel.title = new String(ch, start, length);
			break;
		case STATE_IN_DESC:
			mChannel.description = new String(ch, start, length);
			break;
		case STATE_IN_LINK:
			mChannel.link = new String(ch, start, length);
			break;
		case STATE_IN_ITEM | STATE_IN_ITEM_TITLE:
			mCurrent.title = mCurrent.title.concat(new String(ch, start, length));
			break;
		/*
		case STATE_IN_ITEM | STATE_IN_ITEM_DESC:
			mCurrent.description = mCurrent.description.concat(new String(ch, start, length));
			break;
		*/
		case STATE_IN_ITEM | STATE_IN_ITEM_LINK:
			mCurrent.link = new String(ch, start, length);
			break;
		case STATE_IN_ITEM | STATE_IN_ITEM_DATE:
			mCurrent.date = DateUtils.parseDate(new String(ch, start, length));
			break;
		case STATE_IN_ITEM | STATE_IN_ITEM_AUTHOR:
			mCurrent.author = mCurrent.author.concat(new String(ch, start, length));
			break;
		case STATE_IN_ITEM | STATE_IN_ITEM_THUMB:
			mCurrent.thumbUrl = new String(ch, start, length);
			break;
		case STATE_IN_ITEM | STATE_IN_ITEM_CONTENT:
			mCurrent.content = mCurrent.content.concat(new String(ch, start, length));
			break;
		default:
		}
	}
	
	private static class DateUtils
	{
		private static final SimpleDateFormat[] dateFormats;
		
		private DateUtils()
		{
		}
		
		static
		{
			final String[] possibleDateFormats = {
			        "EEE, dd MMM yyyy HH:mm:ss z", // RFC_822
			        "EEE, dd MMM yyyy HH:mm zzzz",
			        "yyyy-MM-dd'T'HH:mm:ssZ",
			        "yyyy-MM-dd'T'HH:mm:ss.SSSzzzz", // Blogger Atom feed has
													 // millisecs also
			        "yyyy-MM-dd'T'HH:mm:sszzzz",
			        "yyyy-MM-dd'T'HH:mm:ss z",
			        "yyyy-MM-dd'T'HH:mm:ssz", // ISO_8601
			        "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HHmmss.SSSz",
			        "yyyy-MM-dd" };
			
			dateFormats = new SimpleDateFormat[possibleDateFormats.length];
			TimeZone gmtTZ = TimeZone.getTimeZone("GMT");
			
			for (int i = 0; i < possibleDateFormats.length; i++)
			{
				dateFormats[i] = new SimpleDateFormat(possibleDateFormats[i],
				        Locale.ENGLISH);
				dateFormats[i].setTimeZone(gmtTZ);
			}
		}
		
		public static Date parseDate(String strdate)
		{
			Date result = new Date();
			strdate = strdate.trim();
			
			if (strdate.length() > 10)
			{
				if ((strdate.substring(strdate.length() - 5).indexOf("+") == 0 || strdate
				        .substring(strdate.length() - 5).indexOf("-") == 0)
				        && strdate.substring(strdate.length() - 5).indexOf(":") == 2)
				{
					String sign = strdate.substring(strdate.length() - 5,
					        strdate.length() - 4);
					strdate = strdate.substring(0, strdate.length() - 5) + sign
					        + "0" + strdate.substring(strdate.length() - 4);
				}
				
				String dateEnd = strdate.substring(strdate.length() - 6);
				
				// try to deal with -05:00 or +02:00 at end of date
				// replace with -0500 or +0200
				if ((dateEnd.indexOf("-") == 0 || dateEnd.indexOf("+") == 0)
				        && dateEnd.indexOf(":") == 3)
				{
					if (!"GMT".equals(strdate.substring(strdate.length() - 9,
					        strdate.length() - 6)))
					{
						// continue treatment
						String oldDate = strdate;
						String newEnd = dateEnd.substring(0, 3)
						        + dateEnd.substring(4);
						strdate = oldDate.substring(0, oldDate.length() - 6)
						        + newEnd;
					}
				}
			}
			
			for (SimpleDateFormat fmt: dateFormats)
			{
				try
				{
					result = fmt.parse(strdate);
					break;
				}
				catch (ParseException eA)
				{
				}
			}
			
			return result;
		}
	}
}