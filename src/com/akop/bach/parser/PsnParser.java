/*
 * PsnParser.java 
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

package com.akop.bach.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.akop.bach.BasicAccount;
import com.akop.bach.App;
import com.akop.bach.PSN.ComparedGameInfo;
import com.akop.bach.PSN.ComparedTrophyInfo;
import com.akop.bach.PSN.Friends;
import com.akop.bach.PSN.GameCatalogItem;
import com.akop.bach.PSN.GameCatalogItemDetails;
import com.akop.bach.PSN.GameCatalogList;
import com.akop.bach.PSN.GamerProfileInfo;
import com.akop.bach.PSN.Games;
import com.akop.bach.PSN.Profiles;
import com.akop.bach.PSN.Trophies;
import com.akop.bach.Preferences;
import com.akop.bach.PsnAccount;
import com.akop.bach.R;
import com.akop.bach.util.IgnorantHttpClient;
import com.akop.bach.util.rss.RssChannel;

public abstract class PsnParser
		extends Parser
{
	protected static final Pattern PATTERN_PSN_DOWN = Pattern.compile("id=\"siteMessageEN\".*?<h2>([^<]*)</h2>",
			Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
	protected static final Pattern PATTERN_TOS_UPDATED = Pattern.compile("href=\"/external/loadEula.action\">",
			Pattern.CASE_INSENSITIVE);
	
	protected static final String URL_LOGIN = "https://store.playstation.com/j_acegi_external_security_check?target=/external/login.action";
	
	protected static final int COLUMN_GAME_ID = 0;
	protected static final int COLUMN_GAME_PROGRESS = 1;
	protected static final int COLUMN_GAME_BRONZE = 2;
	protected static final int COLUMN_GAME_SILVER = 3;
	protected static final int COLUMN_GAME_GOLD = 4;
	protected static final int COLUMN_GAME_PLATINUM = 5;
	
	protected static final String[] GAMES_PROJECTION = new String[] { 
		Games._ID,
		Games.PROGRESS,
		Games.UNLOCKED_BRONZE,
		Games.UNLOCKED_SILVER,
		Games.UNLOCKED_GOLD,
		Games.UNLOCKED_PLATINUM,
	};
	
	protected static final String[] FRIEND_ID_PROJECTION = { Friends._ID };
	
	public PsnParser(Context context)
	{
		super(context);
		
		HttpParams params = mHttpClient.getParams();
		//params.setParameter("http.useragent", USER_AGENT);
		params.setParameter("http.protocol.max-redirects", 0);
	}
	
	@Override
	protected DefaultHttpClient createHttpClient(Context context)
	{
		return new IgnorantHttpClient();
	}
	
	@Override
	protected String preparseResponse(String response) 
		throws IOException
	{
		// Sony's stupid method of requesting re-authentication
		// This needs to be improved; too un-dependable
		
		if (response.startsWith("<script") && response.endsWith("</script>"))
			throw new ClientProtocolException();
		
		return super.preparseResponse(response);
	}
	
	protected String getOutageMessage(String response)
	{
		Matcher m;
		String message = null;
		
		if ((m = PATTERN_PSN_DOWN.matcher(response)).find())
			message = htmlDecode(m.group(1));
		
		if ((m = PATTERN_TOS_UPDATED.matcher(response)).find())
			message = mContext.getString(R.string.reaccept_tos);
		
		return message;
	}
	
	private void parseAccountSummary(PsnAccount account)
			throws ParserException, IOException
	{
		ContentValues cv = parseSummaryData(account);
		ContentResolver cr = mContext.getContentResolver();
		
		long accountId = account.getId();
		boolean newRecord = true;
		
	    long started = System.currentTimeMillis();
		Cursor c = cr.query(Profiles.CONTENT_URI, 
				new String[] { Profiles._ID },
				Profiles.ACCOUNT_ID + "=" + accountId, null, 
				null);
		
		if (c != null)
		{
			if (c.moveToFirst())
				newRecord = false;
			c.close();
		}
		
		if (newRecord)
		{
			cv.put(Profiles.ACCOUNT_ID, account.getId());
			cv.put(Profiles.UUID, account.getUuid());
			
			cr.insert(Profiles.CONTENT_URI, cv);
		}
		else
		{
			cr.update(Profiles.CONTENT_URI, cv, 
					Profiles.ACCOUNT_ID + "=" + accountId, null);
		}
		
		cr.notifyChange(Profiles.CONTENT_URI, null);
		
		if (App.getConfig().logToConsole())
			displayTimeTaken("Summary update", started);
		
		account.refresh(Preferences.get(mContext));
		account.setOnlineId(cv.getAsString(Profiles.ONLINE_ID));
		account.setIconUrl(cv.getAsString(Profiles.ICON_URL));
		account.setLastSummaryUpdate(System.currentTimeMillis());
		account.save(Preferences.get(mContext));
	}
	
	protected String getAvatarImage(String iconUrl)
	{
		int index = iconUrl.indexOf("=");
		if (index >= 0)
			return iconUrl.substring(index + 1);

		return iconUrl;
	}
	
	@Override
	protected abstract boolean onAuthenticate(BasicAccount account)
			throws IOException, ParserException;
	
	protected abstract ContentValues parseSummaryData(PsnAccount account)
			throws IOException, ParserException;
	
	protected abstract void parseGames(PsnAccount account)
			throws ParserException, IOException;
	
	protected abstract void parseTrophies(PsnAccount account, long gameId)
			throws ParserException, IOException;
	
	protected abstract void parseFriends(PsnAccount account) throws ParserException,
			IOException;
	
	protected abstract GamerProfileInfo parseGamerProfile(PsnAccount account,
			String onlineId) throws ParserException, IOException;
	
	protected abstract void parseFriendSummary(PsnAccount account,
			String friendOnlineId) throws ParserException, IOException;
	
	protected abstract ComparedGameInfo parseCompareGames(PsnAccount account,
			String friendId) throws ParserException, IOException;
	
	protected abstract ComparedTrophyInfo parseCompareTrophies(
			PsnAccount account, String friendId, String gameId)
			throws ParserException, IOException;
	
	public abstract RssChannel fetchBlog() throws ParserException;
	
	public void fetchSummary(PsnAccount account)
			throws AuthenticationException, IOException, ParserException
	{
		for (int i = 0; ; i++)
		{
			if (!authenticate(account, true))
				throw new AuthenticationException(mContext.getString(R.string.error_invalid_credentials_f,
						account.getEmailAddress()));
	        
	        try
	        {
	        	parseAccountSummary(account);
				saveSession(account);
	        }
	        catch(ClientProtocolException e)
	        {
	        	// We'll allow one ClientProtocolException, in case the 
	        	// session data is invalid and redirection has failed. 
	        	// If that happens, we re-authenticate
	        	if (i < 1)
	        	{
	                if (App.getConfig().logToConsole()) 
	                	App.logv("Re-authenticating");
	        		
	                deleteSession(account);
	        		continue;
	        	}
	        	
	        	throw e;
	        }
			break;
		}
	}
	
	public void fetchFriends(PsnAccount account)
			throws AuthenticationException, IOException, ParserException
	{
		for (int i = 0; ; i++)
		{
			if (!authenticate(account, true))
				throw new AuthenticationException(mContext.getString(R.string.error_invalid_credentials_f,
						account.getEmailAddress()));
		    
		    try
		    {
		    	parseFriends(account);
				saveSession(account);
		    }
		    catch(ClientProtocolException e)
		    {
		    	// We'll allow one ClientProtocolException, in case the 
		    	// session data is invalid and redirection has failed. 
		    	// If that happens, we re-authenticate
		    	if (i < 1)
		    	{
		            if (App.getConfig().logToConsole()) 
		            	App.logv("Re-authenticating");
		    		
		            deleteSession(account);
		    		continue;
		    	}
		    	
		    	throw e;
		    }
			break;
		}
	}
	
	public void fetchGames(PsnAccount account)
		throws AuthenticationException, IOException, ParserException
	{
		for (int i = 0; ; i++)
		{
			if (!authenticate(account, true))
				throw new AuthenticationException(mContext.getString(R.string.error_invalid_credentials_f,
						account.getEmailAddress()));
		    
		    try
		    {
		    	parseGames(account);
				saveSession(account);
		    }
		    catch(ClientProtocolException e)
		    {
		    	// We'll allow one ClientProtocolException, in case the 
		    	// session data is invalid and redirection has failed. 
		    	// If that happens, we re-authenticate
		    	if (i < 1)
		    	{
		            if (App.getConfig().logToConsole()) 
		            	App.logv("Re-authenticating");
		    		
		            deleteSession(account);
		    		continue;
		    	}
		    	
		    	throw e;
		    }
			break;
		}
	}
	
	public void fetchTrophies(PsnAccount account, long gameId)
		throws AuthenticationException, IOException, ParserException
	{
		for (int i = 0; ; i++)
		{
			if (!authenticate(account, true))
				throw new AuthenticationException(mContext.getString(R.string.error_invalid_credentials_f,
						account.getEmailAddress()));
		    
		    try
		    {
		    	parseTrophies(account, gameId);
				saveSession(account);
		    }
		    catch(ClientProtocolException e)
		    {
		    	// We'll allow one ClientProtocolException, in case the 
		    	// session data is invalid and redirection has failed. 
		    	// If that happens, we re-authenticate
		    	if (i < 1)
		    	{
		            if (App.getConfig().logToConsole()) 
		            	App.logv("Re-authenticating");
		    		
		            deleteSession(account);
		    		continue;
		    	}
		    	
		    	throw e;
		    }
			break;
		}
	}
	
	public void fetchFriendSummary(PsnAccount account, String friendId)
			throws AuthenticationException, IOException, ParserException
	{
		for (int i = 0; ; i++)
		{
			if (!authenticate(account, true))
				throw new AuthenticationException(mContext
						.getString(R.string.error_invalid_credentials_f, 
								account.getEmailAddress()));
	        
	        try
	        {
	        	parseFriendSummary(account, friendId);
				saveSession(account);
	        }
	        catch(ClientProtocolException e)
	        {
	        	// We'll allow one ClientProtocolException, in case the 
	        	// session data is invalid and redirection has failed. 
	        	// If that happens, we re-authenticate
	        	if (i < 1)
	        	{
	                if (App.getConfig().logToConsole()) 
	                	App.logv("Re-authenticating");
	        		
	                deleteSession(account);
	        		continue;
	        	}
	        	
	        	throw e;
	        }
			break;
		}
	}
	
	public GameCatalogList fetchGameCatalog(PsnAccount account, int console,
			int page, int releaseStatus, int sortOrder)
			throws AuthenticationException, IOException, ParserException
	{
		return parseGameCatalog(console, page, releaseStatus, sortOrder);
	}
	
	public GameCatalogItemDetails fetchGameCatalogItemDetails(
			PsnAccount account, GameCatalogItem item)
			throws AuthenticationException, IOException, ParserException
	{
		return parseGameCatalogItemDetails(item);
	}
	
	protected abstract GameCatalogList parseGameCatalog(int console,
			int page, int releaseStatus, int sortOrder)
			throws AuthenticationException, IOException, ParserException;
	
	protected abstract GameCatalogItemDetails parseGameCatalogItemDetails(
	        GameCatalogItem item) throws AuthenticationException, IOException,
	        ParserException;
	
	protected ArrayList<HashMap<String, String>> parsePairsInSimpleXml(String document, String nodeName)
	{
		Pattern outerNode = Pattern.compile("<" + nodeName + 
				"(?:\\s+[^>]*)?>(.*?)</" + nodeName + ">", Pattern.DOTALL);
		Pattern innerNode = Pattern.compile("<(\\w+)[^>]*>([^<]*)</\\1>");
		
		ArrayList<HashMap<String, String>> kvpList = new ArrayList<HashMap<String,String>>();
		
		Matcher nodeMatcher = outerNode.matcher(document);
		while (nodeMatcher.find())
		{
			String content = nodeMatcher.group(1);
			
			Matcher m = innerNode.matcher(content);
			HashMap<String, String> items = new HashMap<String, String>();
			
			while (m.find())
			{
				String innerNodeName = m.group(1);
				if (items.containsKey(innerNodeName))
					continue;
				
				items.put(innerNodeName, m.group(2));
			}
			
			if (items.size() > 0)
				kvpList.add(items);
		}
		
		return kvpList;
	}
	
	public GamerProfileInfo fetchGamerProfile(PsnAccount account, String psnId)
	        throws AuthenticationException, IOException, ParserException
	{
		for (int i = 0; ; i++)
		{
			if (!authenticate(account, true))
				throw new AuthenticationException(mContext
						.getString(R.string.error_invalid_credentials_f, 
								account.getEmailAddress()));
		    
		    try
		    {
		    	GamerProfileInfo gpi = parseGamerProfile(account, psnId);
				saveSession(account);
				
				return gpi;
		    }
		    catch(ClientProtocolException e)
		    {
		    	// We'll allow one ClientProtocolException, in case the 
		    	// session data is invalid and redirection has failed. 
		    	// If that happens, we re-authenticate
		    	if (i < 1)
		    	{
		            if (App.getConfig().logToConsole()) 
		            	App.logv("Re-authenticating");
		    		
		            deleteSession(account);
		    		continue;
		    	}
		    	
		    	throw e;
		    }
		}
	}
	
	public ComparedGameInfo compareGames(PsnAccount account, String friendId)
		throws AuthenticationException, IOException, ParserException
	{
		ComparedGameInfo cgi;
		
		for (int i = 0; ; i++)
		{
			if (!authenticate(account, true))
				throw new AuthenticationException(mContext.getString(R.string.error_invalid_credentials_f,
						account.getEmailAddress()));
		    
		    try
		    {
		    	cgi = parseCompareGames(account, friendId);
		    }
		    catch(ClientProtocolException e)
		    {
		    	// We'll allow one ClientProtocolException, in case the 
		    	// session data is invalid and redirection has failed. 
		    	// If that happens, we re-authenticate
		    	if (i < 1)
		    	{
		            if (App.getConfig().logToConsole()) 
		            	App.logv("Re-authenticating");
		    		
		            deleteSession(account);
		    		continue;
		    	}
		    	
		    	throw e;
		    }
			break;
		}
		
		return cgi;
	}
	
	public ComparedTrophyInfo compareTrophies(PsnAccount account,
			String friendId, String gameId) throws AuthenticationException,
			IOException, ParserException
	{
		ComparedTrophyInfo cti;
		
		for (int i = 0; ; i++)
		{
			if (!authenticate(account, true))
				throw new AuthenticationException(mContext.getString(R.string.error_invalid_credentials_f,
						account.getEmailAddress()));
		    
		    try
		    {
		    	cti = parseCompareTrophies(account, friendId, gameId);
		    }
		    catch(ClientProtocolException e)
		    {
		    	// We'll allow one ClientProtocolException, in case the 
		    	// session data is invalid and redirection has failed. 
		    	// If that happens, we re-authenticate
		    	if (i < 1)
		    	{
		            if (App.getConfig().logToConsole()) 
		            	App.logv("Re-authenticating");
		    		
		            deleteSession(account);
		    		continue;
		    	}
		    	
		    	throw e;
		    }
			break;
		}
		
		return cti;
	}
	
	@Override
	public ContentValues validateAccount(BasicAccount account)
			throws AuthenticationException, IOException, ParserException
	{
		if (!authenticate(account, false))
			throw new AuthenticationException(mContext.getString(
					R.string.error_invalid_credentials_f, account.getLogonId()));
		
		ContentValues cv = parseSummaryData((PsnAccount)account);
		cv.put(Profiles.ACCOUNT_ID, account.getId());
		cv.put(Profiles.UUID, account.getUuid());
		
		return cv;
	}
	
	@Override
	public void deleteAccount(BasicAccount account)
	{
		ContentResolver cr = mContext.getContentResolver();
		long accountId = account.getId();
		
        // Clear games & achievements
        StringBuffer buffer = new StringBuffer();
        Cursor c = cr.query(Games.CONTENT_URI, 
        		new String[] { Games._ID }, 
        		Games.ACCOUNT_ID + "=" + accountId, null, 
        		null);
        
        try
        {
	        if (c != null)
	        {
	        	while (c.moveToNext())
	        	{
	        		if (buffer.length() > 0)
	        			buffer.append(",");
	        		buffer.append(c.getLong(0));
	        	}
	        }
        }
        finally
        {
        	if (c != null)
        		c.close();
        }
        
    	// Clear trophies
    	cr.delete(Trophies.CONTENT_URI, 
    			Trophies.GAME_ID + " IN (" + buffer.toString() + ")", null);
    	
    	// Clear rest of data
    	cr.delete(Games.CONTENT_URI, Games.ACCOUNT_ID + "=" + accountId, null);
        cr.delete(Profiles.CONTENT_URI, Profiles.ACCOUNT_ID + "=" + accountId, null);
        cr.delete(Friends.CONTENT_URI, Friends.ACCOUNT_ID + "=" + accountId, null);
        
        // Send notifications
        cr.notifyChange(Profiles.CONTENT_URI, null);
        cr.notifyChange(Trophies.CONTENT_URI, null);
        cr.notifyChange(Games.CONTENT_URI, null);
        cr.notifyChange(Friends.CONTENT_URI, null);
        
        // Delete authenticated session
        deleteSession(account);
	}
	
	public void createAccount(BasicAccount account, ContentValues cv)
	{
		// Add profile to database
		ContentResolver cr = mContext.getContentResolver();
		cr.insert(Profiles.CONTENT_URI, cv);
		cr.notifyChange(Profiles.CONTENT_URI, null);
		
		PsnAccount psnAccount = (PsnAccount)account;
		
		// Save changes to preferences
		psnAccount.setOnlineId(cv.getAsString(Profiles.ONLINE_ID));
		psnAccount.setIconUrl(cv.getAsString(Profiles.ICON_URL));
		psnAccount.setLastSummaryUpdate(System.currentTimeMillis());
		
		account.save(Preferences.get(mContext));
	}
}
