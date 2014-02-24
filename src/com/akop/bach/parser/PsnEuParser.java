/*
 * PsnEuParser.java 
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
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.params.HttpParams;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.akop.bach.App;
import com.akop.bach.BasicAccount;
import com.akop.bach.PSN;
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
import com.akop.bach.util.rss.RssChannel;
import com.akop.bach.util.rss.RssHandler;

public class PsnEuParser
		extends PsnParser
{
	protected static final String URL_BLOG = "http://feeds.feedburner.com/SCEEBlog?format=xml";
	
	private static final String LARGE_TROPHY_ICON_PREFIX = 
		"http://trophy01.np.community.playstation.net";
	private static final String LARGE_AVATAR_ICON_PREFIX = 
		"http://static-resource.np.community.playstation.net";
	
	private static final String URL_RETURN_LOGIN = 
		"https://secure.eu.playstation.com/sign-in/confirmation/";
	
	private static final String URL_PROFILE_SUMMARY =
		"https://secure.eu.playstation.com/psn/mypsn/";
	private static final String URL_GAMES = 
		"http://uk.playstation.com/psn/mypsn/ajax/trophies/?startIndex=%1$d&sortBy=recent";
	private static final String URL_TROPHIES_f = 
		"http://uk.playstation.com/psn/mypsn/trophies/detail/?title=%1$s";
	private static final String URL_FRIENDS =
		"http://uk.playstation.com/psn/mypsn/friends/";
		//"https://secure.eu.playstation.com/psn/mypsn/friends/";
	private static final String URL_FRIENDS_AJAX =
		"http://uk.playstation.com/ajax/mypsn/friend/presence/";
		//"https://secure.eu.playstation.com/ajax/mypsn/friend/presence/";
	private static final String URL_COMPARE_GAMES_f =
		"http://uk.playstation.com/psn/mypsn/ajax/trophies-compare/friend/add/?onlineid=%s&endIndex=999&sortBy=recent";
	private static final String URL_COMPARE_TROPHIES_f =
		"http://uk.playstation.com/psn/mypsn/trophies-compare/detail/?title=%1$s&friend=%2$s";
		//"https://secure.eu.playstation.com/psn/mypsn/trophies-compare/detail/?title=%1$s&friend=%2$s";
	private static final String URL_FRIEND_SUMMARY_f =
		"http://uk.playstation.com/psn/mypsn/trophies-compare/?friend=%1$s";
		//"https://secure.eu.playstation.com/psn/mypsn/trophies-compare/?friend=%1$s";
	
	private static final String URL_GAME_CATALOG =
		"http://uk.playstation.com/ajax/games-hub/";
	
	private static final Pattern PATTERN_SIGN_OUT_LINK = Pattern
			.compile("data-aa-evt-name=\"sign out\"");
	
	private static final Pattern PATTERN_ONLINE_ID = Pattern
			.compile("<div class=\"user-info\">\\s+<h2>\\s+(\\S+)\\s+</h2>\\s+<h3>\\s+<strong>");
	
	private static final Pattern PATTERN_INFOBAR_ITEM = Pattern
			.compile("<div class=\"wrap\">\\s+<h4>[^<]+</h4>\\s+<div class=\"[^\"]+\"(?: style=\"[^\"]+\")?>\\s*([^<\\s]+)\\s*</div>");
	
	private static final Pattern PATTERN_PROGRESS = Pattern
			.compile("<div class=\"level-percentage\">\\s*(\\d+)\\s*<span>");
	private static final Pattern PATTERN_AVATAR_URL = Pattern
			.compile("<img alt=\"[^\"]+\" class=\"avatar\" src=\"([^\"]*)\"",
					Pattern.DOTALL);
	
	private static final Pattern PATTERN_IS_PLUS = Pattern
	        .compile("<img\\s+alt=\"[^\"]+\"\\s+class=\"psp-logo\"");
	
	private static final Pattern PATTERN_GAMES = Pattern.compile(
			"<li class=\"tile\">(.*?)</div>\\s+</li>", Pattern.DOTALL);
	
	private static final Pattern PATTERN_GAME_UID = Pattern
			.compile("<a class=\"tile-link [^\"]+\" href=\"[^=]*=(\\d+)\">");
	private static final Pattern PATTERN_GAME_PROGRESS = Pattern
			.compile("<span>(\\d+)%</span>");
	private static final Pattern PATTERN_GAME_TROPHIES = Pattern
			.compile("<li class=\"([^\"]+)\">([^<]+)</li>");
	private static final Pattern PATTERN_GAME_TITLE = Pattern
			.compile("<h2 class=\"clearfix title\">\\s+(\\S[^\\n\\r]+)\\r?\\n\\s+</h2>");
	private static final Pattern PATTERN_GAME_ICON = Pattern
			.compile("<img src=\"([^\"]*)\"");
	private static final Pattern PATTERN_GAME_MORE = Pattern
			.compile("class=\"more-link\">");
	
	private static final Pattern PATTERN_TROPHIES = Pattern.compile(
			"<tr>\\s+<td class=\"trophy-unlocked-([^\"]+)\">(.*?)</tr>", Pattern.DOTALL);
	private static final Pattern PATTERN_TROPHY_TITLE = Pattern.compile(
			"<h1 class=\"trophy_name ([^\"]+)\">\\s+(\\S[^\\n\\r]+)\\r?\\n\\s+</h1>");
	private static final Pattern PATTERN_TROPHY_DESCRIPTION = Pattern.compile(
			"<h2>\\s+(\\S[^\\n\\r]+)\\r?\\n\\s+</h2>");
	private static final Pattern PATTERN_TROPHY_ICON = Pattern.compile(
			"<img src=\"([^\"]*)\"");
	
	private static final Pattern PATTERN_FRIENDS = Pattern.compile(
			"<psn_friend>(.*?)</psn_friend>", Pattern.DOTALL);
	private static final Pattern PATTERN_FRIEND_ONLINE_ID = Pattern
			.compile("<onlineid>([^<]*)</");
	private static final Pattern PATTERN_FRIEND_LEVEL = Pattern
			.compile("<level>(\\d+)</");
	private static final Pattern PATTERN_FRIEND_AVATAR = Pattern
			.compile("<current_avatar>([^<]+)</");
	private static final Pattern PATTERN_FRIEND_TROPHY = Pattern
			.compile("<(platinum|gold|silver|bronze)>(\\d+)</");
	private static final Pattern PATTERN_FRIEND_PLAYING = Pattern
			.compile("<current_game>([^<]*)</");
	private static final Pattern PATTERN_FRIEND_STATUS = Pattern
			.compile("<current_presence>([^<]*)</");
	private static final Pattern PATTERN_FRIEND_COMMENT = Pattern
			.compile("<comment>([^<]*)</");
	private static final Pattern PATTERN_FRIEND_IS_PLUS = Pattern
			.compile("<playstationplus>([^<]*)</");
	
	private static final Pattern PATTERN_FRIENDS_PENDING = Pattern.compile(
			"class=\"friendPresence-pending[^\"]*\"[^>]*>\\s*</td>\\s*<td[^>]*>\\s*(\\S+)\\s*<div",
			Pattern.DOTALL);
	
	private static final Pattern PATTERN_FRIEND_SUMMARY = Pattern
			.compile(" id=\"trophySummaryContainer\"[^>]*>(.*?)</div>\\s*</div>\\s*</div>\\s*</div>\\s*</div>");
	private static final Pattern PATTERN_FRIEND_SUMMARY_ONLINE_ID = Pattern
			.compile("<strong>([^<]*)</strong>");
	private static final Pattern PATTERN_FRIEND_SUMMARY_AVATAR = Pattern
			.compile("<img src=\"([^\"]*)\"");
	private static final Pattern PATTERN_FRIEND_SUMMARY_LEVEL = Pattern
			.compile("<p class=\"summary level\">(\\d+)</p>");
	private static final Pattern PATTERN_FRIEND_SUMMARY_PROGRESS = Pattern
			.compile("<p class=\"percentage\">(\\d+)%</p>");
	private static final Pattern PATTERN_FRIEND_SUMMARY_TROPHIES = Pattern
			.compile("<strong id=\"fl(Bronze|Silver|Gold|Platinum)Trophies\">(\\d+)\\s*</strong>");
	private static final Pattern PATTERN_FRIEND_SUMMARY_IS_PLUS = Pattern
		    .compile("<div class=\"avatarPlayStationPlus\">");
	
	private static final Pattern PATTERN_COMPARED_GAMES = Pattern.compile(
			"<tr id=\"([^\"]+)\" data-ajax-endcount=\"[^\"]*\">(.*?)</tr>", 
			Pattern.DOTALL);
	private static final Pattern PATTERN_COMPARE_GAME_TITLE = Pattern
			.compile("</span>\\s+(\\S[^\\n\\r]+)\\r?\\n\\s+</div>\\s+</a>");
	private static final Pattern PATTERN_COMPARE_GAME_ICON = Pattern
			.compile("<div class=\"img-bkg\" style=\"background-image:url\\('([^']*)'");
	private static final Pattern PATTERN_COMPARE_USERNAME = Pattern
			.compile("<h4 class=\"user-name\">([^<]*)<span>");
	private static final Pattern PATTERN_COMPARE_AVATAR_URL = Pattern
	        .compile("<img class=\"avatar-image\" src=\"([^\"]+)\"");
	private static final Pattern PATTERN_COMPARE_GAME_PLAYER = Pattern
	        .compile("<td>(.*?)</td>", Pattern.DOTALL);
	private static final Pattern PATTERN_COMPARE_GAME_TROPHIES = Pattern
	        .compile("<li>(\\d+)</li>");
	private static final Pattern PATTERN_COMPARE_GAME_PROGRESS = Pattern
	        .compile("<span>(\\d+)%</span>");
	
	private static final Pattern PATTERN_COMPARED_TROPHIES = Pattern.compile(
			"<div class=\"gameLevelListRow\">(.*?<div class=\"gameLevelListItemCompare\"[^>]*>(?:.*?)</div>\\s*<div class=\"gameLevelListItemCompare\"[^>]*>(?:.*?)</div>)\\s*</div>", 
			Pattern.DOTALL);
	private static final Pattern PATTERN_COMPARED_TROPHY_PERSON = Pattern.compile(
			"<div class=\"gameLevelListItemCompare\"[^>]*>(.*?)</div>", 
			Pattern.DOTALL);
	
	private static final Pattern PATTERN_PARSE_RELATIVE_DATE = Pattern.compile(
			"^(\\d+) (\\w+)");
	
	private static final Pattern PATTERN_GAME_CATALOG_ITEMS = Pattern.compile(
			"<game_result content_id=\"[^\"]*\">(.*?)</game_result>", Pattern.DOTALL);
	private static final Pattern PATTERN_GAME_CATALOG_STATS = Pattern.compile(
			"<game_results page_current=\"(\\d+)\" page_total=\"(\\d+)\">");
	
	private static final Pattern PATTERN_GAME_CATALOG_NODE = Pattern.compile(
			"<([a-z_]+)[^>]*>([^<]*)</[a-z_]+>");
	
	private static final Pattern PATTERN_GAME_CATALOG_DETAIL_DESC = Pattern.compile(
			"<div id=\"content\" class=\"\">(.*?)</div>\\s*</div>", 
			Pattern.DOTALL);
	private static final Pattern PATTERN_GAME_CATALOG_DETAIL_DESC_ALT = Pattern.compile(
			"<div class=\"gameInfo\">\\s*<div class=\"info\" style=\"[^\"]+\">(.*?)</div>\\s*</div>", 
			Pattern.DOTALL);
	
	public PsnEuParser(Context context)
	{
		super(context);
	}
	
	@Override
	protected boolean onAuthenticate(BasicAccount account) 
		throws IOException, ParserException
	{
		PsnAccount psnAccount = (PsnAccount)account;
		
		String password = psnAccount.getPassword();
		if (password == null)
			throw new ParserException(mContext.getString(R.string.decryption_error));
		
		HttpParams params = mHttpClient.getParams();
		
		// Prepare POSTDATA
		List<NameValuePair> inputs = new ArrayList<NameValuePair>(3);
		
		addValue(inputs, "j_username", psnAccount.getEmailAddress());
		addValue(inputs, "j_password", password);
		addValue(inputs, "returnURL", URL_RETURN_LOGIN);
		
		// Enable redirection (max 1)
		params.setParameter("http.protocol.max-redirects", 3);
		
		// 1. Post authentication data
		String page = getResponse(URL_LOGIN, inputs);
		
		// Disable redirection
		params.setParameter("http.protocol.max-redirects", 1);
		
		// Get redirection URL
		Matcher m = PATTERN_SIGN_OUT_LINK.matcher(page);
	    if (!m.find())
	    {
	    	if (App.getConfig().logToConsole())
	    		App.logv("onAuthEU: Redir URL not found");
	    	
	    	String outageMessage;
	    	if ((outageMessage = getOutageMessage(page)) != null)
	    		throw new ParserException(outageMessage);
	    	
	    	return false;
	    }
	    
		return true;
	}
	
	@Override
	protected String getSessionFile(BasicAccount account)
	{
		return account.getUuid() + ".eu.session";
	}
	
	@Override
	protected ContentValues parseSummaryData(PsnAccount account)
			throws IOException, ParserException
	{
		String page = getResponse(URL_PROFILE_SUMMARY);
		ContentValues cv = new ContentValues(10);
	    Matcher m;
	    
	    long started = System.currentTimeMillis();
	    
		if (!(m = PATTERN_ONLINE_ID.matcher(page)).find())
			throw new ParserException(mContext, R.string.error_online_id_not_detected);
		
		cv.put(Profiles.ONLINE_ID, htmlDecode(m.group(1)));
		
		int memberType = PSN.MEMBER_TYPE_FREE;
		if (PATTERN_IS_PLUS.matcher(page).find())
			memberType = PSN.MEMBER_TYPE_PLUS;
		
		int level = 0;
		int trophiesPlat = 0;
		int trophiesGold = 0;
		int trophiesSilver = 0;
		int trophiesBronze = 0;
		
		Matcher infoBarMatcher = PATTERN_INFOBAR_ITEM.matcher(page);
		for (int i = 0; infoBarMatcher.find(); i++)
		{
			int value = Integer.parseInt(infoBarMatcher.group(1));
			if (i == 0)
				level = value;
			else if (i == 2)
				trophiesBronze = value;
			else if (i == 3)
				trophiesSilver = value;
			else if (i == 4)
				trophiesGold = value;
			else if (i == 5)
				trophiesPlat = value;
		}
		
		int progress = 0;
		if ((m = PATTERN_PROGRESS.matcher(page)).find())
			progress = Integer.parseInt(m.group(1));
		
		cv.put(Profiles.LEVEL, level);
		cv.put(Profiles.MEMBER_TYPE, memberType);
		cv.put(Profiles.PROGRESS, progress);
		cv.put(Profiles.TROPHIES_PLATINUM, trophiesPlat);
		cv.put(Profiles.TROPHIES_GOLD, trophiesGold);
		cv.put(Profiles.TROPHIES_SILVER, trophiesSilver);
		cv.put(Profiles.TROPHIES_BRONZE, trophiesBronze);
		
		String iconUrl = null;
		if ((m = PATTERN_AVATAR_URL.matcher(page)).find())
			iconUrl = getLargeAvatarIcon(resolveImageUrl(URL_PROFILE_SUMMARY,
					m.group(1)));
		
		cv.put(Profiles.ICON_URL, iconUrl);
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("parseSummaryData processing", started);
		
		return cv;
	}
	
	private boolean parseGamePage(int startIndex, String page, List<ContentValues> cvList)
	{
		Matcher m;
		Matcher gameMatcher = PATTERN_GAMES.matcher(page);
		
		for (int i = 0; gameMatcher.find(); i++)
		{
			String gameContent = gameMatcher.group(1);
			
			String gameUid;
			int gameProgress;
			String gameTitle = null;
			String gameIcon = null;
			
			int bronze = 0;
			int silver = 0;
			int gold = 0;
			int platinum = 0;
			
			if (!(m = PATTERN_GAME_TROPHIES.matcher(gameContent)).find())
				continue;
			
			bronze = Integer.parseInt(m.group(2));
				
			if (!m.find())
				continue;
			
			silver = Integer.parseInt(m.group(2));
			
			if (!m.find())
				continue;
			
			gold = Integer.parseInt(m.group(2));
			
			if (!m.find())
				continue;
			
			platinum = Integer.parseInt(m.group(2));
			
	        m = PATTERN_GAME_UID.matcher(gameContent);
	        if (!m.find())
	        	continue;
	        
    		gameUid = m.group(1);
	        
	        m = PATTERN_GAME_PROGRESS.matcher(gameContent);
	        if (!m.find())
	        	continue;
	        
    		gameProgress = Integer.parseInt(m.group(1));
        	
	        m = PATTERN_GAME_TITLE.matcher(gameContent);
	        if (m.find())
	        	gameTitle = htmlDecode(m.group(1));
	        
	        m = PATTERN_GAME_ICON.matcher(gameContent);
	        if (m.find())
	        	gameIcon = getLargeTrophyIcon(resolveImageUrl(URL_GAMES, m.group(1)));
	        
			ContentValues cv = new ContentValues(15);
			
            cv.put(Games.TITLE, gameTitle);
            cv.put(Games.UID, gameUid);
            cv.put(Games.ICON_URL, gameIcon);
            cv.put(Games.PROGRESS, gameProgress);
            cv.put(Games.SORT_ORDER, i + startIndex);
			cv.put(Games.UNLOCKED_PLATINUM, platinum);
			cv.put(Games.UNLOCKED_GOLD, gold);
			cv.put(Games.UNLOCKED_SILVER, silver);
			cv.put(Games.UNLOCKED_BRONZE, bronze);
			App.logv(gameTitle);
			cvList.add(cv);
		}
		
        return PATTERN_GAME_MORE.matcher(page).find();
	}
	
	@SuppressLint("DefaultLocale")
	@Override
	protected void parseGames(PsnAccount account)
			throws ParserException, IOException
	{
		long started = System.currentTimeMillis();
		boolean keepGoing = true;
		List<ContentValues> cvList = new ArrayList<ContentValues>();
		
		for (int startIndex = 0; keepGoing; startIndex += 16)
		{
			String url = String.format(URL_GAMES, startIndex);
			String page = getResponse(url);
			
			keepGoing = parseGamePage(startIndex, page, cvList);
		}
		
		final long accountId = account.getId();
		ContentResolver cr = mContext.getContentResolver();
		String[] queryParams = new String[1];
		Cursor c;
		long updated = System.currentTimeMillis();
		List<ContentValues> newCvs = new ArrayList<ContentValues>(100);
		
		// Check to see if we already have a record of this game
		for (ContentValues cv: cvList)
		{
			queryParams[0] = cv.getAsString(Games.UID);
			c = cr.query(Games.CONTENT_URI, GAMES_PROJECTION, Games.ACCOUNT_ID
					+ "=" + accountId + " AND " + Games.UID + "=?",
					queryParams, null);
			
			try
			{
		        if (c == null || !c.moveToFirst()) // New game
		        {
	                cv.put(Games.ACCOUNT_ID, accountId);
					cv.put(Games.TROPHIES_DIRTY, 1);
					cv.put(Games.LAST_UPDATED, updated);
					
	                newCvs.add(cv);
		        }
		        else // Existing game
		        {
		        	boolean isDirty = false;
		        	long gameId = c.getLong(COLUMN_GAME_ID);
		        	
		        	if (c.getInt(COLUMN_GAME_PROGRESS) != cv.getAsInteger(Games.PROGRESS))
		        		isDirty = true;
		        	if (c.getInt(COLUMN_GAME_BRONZE) != cv.getAsInteger(Games.UNLOCKED_BRONZE))
		        		isDirty = true;
		        	if (c.getInt(COLUMN_GAME_SILVER) != cv.getAsInteger(Games.UNLOCKED_SILVER))
		        		isDirty = true;
		        	if (c.getInt(COLUMN_GAME_GOLD) != cv.getAsInteger(Games.UNLOCKED_GOLD))
		        		isDirty = true;
		        	if (c.getInt(COLUMN_GAME_PLATINUM) != cv.getAsInteger(Games.UNLOCKED_PLATINUM))
		        		isDirty = true;
		        	
		        	if (isDirty)
		        		cv.put(Games.TROPHIES_DIRTY, 1);
		        	
	        		cv.put(Games.LAST_UPDATED, updated);
	        		
					cr.update(Games.CONTENT_URI, cv, Games._ID + "=" + gameId, null);
		        }
			}
			finally
			{
	        	if (c != null)
	        		c.close();
			}
		}
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Game page processing", started);
		
		if (newCvs.size() > 0)
		{
			ContentValues[] cvs = new ContentValues[newCvs.size()];
			newCvs.toArray(cvs);
			
			cr.bulkInsert(Games.CONTENT_URI, cvs);
			
			if (App.getConfig().logToConsole())
				displayTimeTaken("Game page insertion", started);
		}
		
		account.refresh(Preferences.get(mContext));
		account.setLastGameUpdate(System.currentTimeMillis());
		account.save(Preferences.get(mContext));
		
		cr.notifyChange(Games.CONTENT_URI, null);
	}
	
	@SuppressLint("DefaultLocale")
	@Override
	protected void parseTrophies(PsnAccount account, long gameId)
			throws ParserException, IOException
	{
		// Find game record in local DB
		ContentResolver cr = mContext.getContentResolver(); 
		
		String gameUid = Games.getUid(mContext, gameId);
		
		String url = String.format(URL_TROPHIES_f, gameUid);
		String page = getResponse(url);
		
	    int index = 0;
	    long started = System.currentTimeMillis();
	    
	    Matcher trophies = PATTERN_TROPHIES.matcher(page);
	    Matcher m;
	    
		List<ContentValues> cvList = new ArrayList<ContentValues>(100);
		while (trophies.find())
		{
			boolean trophyUnlocked = "true".equalsIgnoreCase(trophies.group(1));
			String trophyContent = trophies.group(2);
			
			String iconUrl = null;
			if ((m = PATTERN_TROPHY_ICON.matcher(trophyContent)).find())
				iconUrl = getLargeTrophyIcon(resolveImageUrl(url, m.group(1)));
			
			String title = mContext.getString(R.string.secret_trophy);
			String description = mContext.getString(R.string.this_is_a_secret_trophy);
			
			if (!(m = PATTERN_TROPHY_TITLE.matcher(trophyContent)).find())
				continue;
			
			int type = PSN.TROPHY_SECRET;
			boolean isSecret = false;
			String trophyType = m.group(1).toUpperCase();
			if (trophyType.equals("BRONZE"))
				type = PSN.TROPHY_BRONZE;
			else if (trophyType.equals("SILVER"))
				type = PSN.TROPHY_SILVER;
			else if (trophyType.equals("GOLD"))
				type = PSN.TROPHY_GOLD;
			else if (trophyType.equals("PLATINUM"))
				type = PSN.TROPHY_PLATINUM;
			else if (trophyType.equals("UNKNOWN"))
				isSecret = true;
			
			if (!isSecret)
			{
				title = htmlDecode(m.group(2));
				if ((m = PATTERN_TROPHY_DESCRIPTION.matcher(trophyContent)).find())
					description = htmlDecode(m.group(1));
			}
			
			long earned = 0;
			String earnedText = null;
			if (trophyUnlocked)
			{
				// No more date info
				earned = System.currentTimeMillis();
				earnedText = mContext.getString(R.string.unlocked);
			}
			
			ContentValues cv = new ContentValues(20);
			
			cv.put(Trophies.TITLE, title);
			cv.put(Trophies.DESCRIPTION, description);
			cv.put(Trophies.SORT_ORDER, index);
			cv.put(Trophies.EARNED, earned);
			cv.put(Trophies.EARNED_TEXT, earnedText);
			cv.put(Trophies.ICON_URL, iconUrl);
			cv.put(Trophies.GAME_ID, gameId);
			cv.put(Trophies.IS_SECRET, isSecret ? 1 : 0);
			cv.put(Trophies.TYPE, type);
			
			cvList.add(cv);
			index++;
		}
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("New trophy parsing", started);
		
		ContentValues[] cva = new ContentValues[cvList.size()];
		cvList.toArray(cva);
		
		cr.delete(Trophies.CONTENT_URI, Trophies.GAME_ID + "=" + gameId, null);
		
		// Bulk-insert new trophies
		cr.bulkInsert(Trophies.CONTENT_URI, cva);
		cr.notifyChange(Trophies.CONTENT_URI, null);
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("New trophy processing", started);
		
		// Update the game to remove the 'dirty' attribute
		ContentValues cv = new ContentValues(10);
		cv.put(Games.TROPHIES_DIRTY, 0); 
	    cr.update(Games.CONTENT_URI, cv, Games._ID + "=" + gameId, null);
		
		cr.notifyChange(ContentUris.withAppendedId(Games.CONTENT_URI, gameId), 
				null);
		
		if (App.getConfig().logToConsole())
			displayTimeTaken("Updating Game", started);
	}
	
	@SuppressLint("DefaultLocale")
	@Override
	protected void parseFriends(PsnAccount account)
			throws ParserException, IOException
	{
		synchronized (PsnEuParser.class)
		{
			ContentResolver cr = mContext.getContentResolver();
			ContentValues cv;
			List<ContentValues> newCvs = new ArrayList<ContentValues>(100);
			final long accountId = account.getId();
			
			int rowsInserted = 0;
			int rowsUpdated = 0;
			int rowsDeleted = 0;
			
			long updated = System.currentTimeMillis();
		    long started = updated;
			
			// Handle pending requests
			String page = getResponse(URL_FRIENDS);
		    
		    Matcher m;
			Matcher friendMatcher = PATTERN_FRIENDS_PENDING.matcher(page);
			
			while (friendMatcher.find())
			{
				String onlineId = htmlDecode(friendMatcher.group(1));
				Cursor c = cr.query(Friends.CONTENT_URI, FRIEND_ID_PROJECTION, 
						Friends.ACCOUNT_ID + "=" + account.getId() + " AND " + 
						Friends.ONLINE_ID + "=?", 
						new String[] { onlineId }, null);
				
				long friendId = -1;
				
				try
				{
			        if (c != null && c.moveToFirst())
			        	friendId = c.getLong(0);
				}
				finally
				{
					if (c != null)
						c.close();
				}
				
				cv = new ContentValues(15);
				
				cv.put(Friends.DELETE_MARKER, updated);
	            cv.put(Friends.ONLINE_STATUS, PSN.STATUS_PENDING);
	            
				if (friendId < 0)
				{
					// New
		            cv.put(Friends.ONLINE_ID, onlineId);
		            cv.put(Friends.ACCOUNT_ID, accountId);
		            cv.put(Friends.PROGRESS, 0);
		            cv.putNull(Friends.ICON_URL);
		            cv.put(Friends.LEVEL, 0);
					cv.put(Friends.TROPHIES_PLATINUM, 0);
					cv.put(Friends.TROPHIES_GOLD, 0);
					cv.put(Friends.TROPHIES_SILVER, 0);
					cv.put(Friends.TROPHIES_BRONZE, 0);
		            cv.putNull(Friends.PLAYING);
					cv.put(Friends.LAST_UPDATED, 0);
					
		            newCvs.add(cv);
				}
				else
				{
					cr.update(ContentUris.withAppendedId(Friends.CONTENT_URI,
							friendId), cv, null, null);
					
					rowsUpdated++;
				}
			}
			
			// Handle rest of friends
			page = getResponse(URL_FRIENDS_AJAX);
			friendMatcher = PATTERN_FRIENDS.matcher(page);
			
			while (friendMatcher.find())
			{
				String friendData = friendMatcher.group(1);
				
				String onlineId;
				if (!(m = PATTERN_FRIEND_ONLINE_ID.matcher(friendData)).find())
					continue;
				
				onlineId = htmlDecode(m.group(1));
				
				int level = 0;
				if ((m = PATTERN_FRIEND_LEVEL.matcher(friendData)).find())
					level = Integer.parseInt(m.group(1));
				
				String iconUrl = null;
				if ((m = PATTERN_FRIEND_AVATAR.matcher(friendData)).find())
					iconUrl = getLargeAvatarIcon(resolveImageUrl(URL_FRIENDS_AJAX,
							m.group(1)));
				
				String comment = null;
				if ((m = PATTERN_FRIEND_COMMENT.matcher(friendData)).find())
				{
					comment = htmlDecode(m.group(1));
					if (comment != null && comment.equals("null"))
						comment = null;
				}
				
				int memberType = PSN.MEMBER_TYPE_FREE;
				if ((m = PATTERN_FRIEND_IS_PLUS.matcher(friendData)).find() 
						&& m.group(1).equalsIgnoreCase("true"))
				{
					memberType = PSN.MEMBER_TYPE_PLUS;
				}
				
				int bronze = 0;
				int silver = 0;
				int gold = 0;
				int platinum = 0;
				
				m = PATTERN_FRIEND_TROPHY.matcher(friendData);
				while (m.find())
				{
					String type = m.group(1).toLowerCase();
					if ("bronze".equals(type))
						bronze = Integer.parseInt(m.group(2));
					else if ("silver".equals(type))
						silver = Integer.parseInt(m.group(2));
					else if ("gold".equals(type))
						gold = Integer.parseInt(m.group(2));
					else if ("platinum".equals(type))
						platinum = Integer.parseInt(m.group(2));
				}
				
				boolean inGame = false;
				int status = PSN.STATUS_OTHER;
				if ((m = PATTERN_FRIEND_STATUS.matcher(friendData)).find())
				{
					String presence = m.group(1).toLowerCase();
					if (presence.equals("offline"))
						status = PSN.STATUS_OFFLINE;
					else if (presence.equals("online"))
						status = PSN.STATUS_ONLINE;
					else if (presence.equals("online-ingame"))
					{
						status = PSN.STATUS_ONLINE;
						inGame = true;
					}
					else if (presence.equals("online-away"))
						status = PSN.STATUS_AWAY;
					else if (presence.equals("online-ingame-away"))
					{
						status = PSN.STATUS_AWAY;
						inGame = true;
					}
					else if (presence.equals("pending"))
						status = PSN.STATUS_PENDING;
				}
				
				String playing = null;
				if ((m = PATTERN_FRIEND_PLAYING.matcher(friendData)).find())
				{
					String activity = htmlDecode(m.group(1)).trim();
					
					if (activity != null && activity.length() > 0)
					{
						if (inGame)
							playing = mContext.getString(R.string.playing_f, activity);
						else
							playing = activity;
					}
				}
				
				Cursor c = cr.query(Friends.CONTENT_URI, FRIEND_ID_PROJECTION, 
						Friends.ACCOUNT_ID + "=" + account.getId() + " AND " + 
						Friends.ONLINE_ID + "=?", 
						new String[] { onlineId }, null);
				
				long friendId = -1;
				
				try
				{
			        if (c != null && c.moveToFirst())
			        	friendId = c.getLong(0);
				}
				finally
				{
					if (c != null)
						c.close();
				}
				
				cv = new ContentValues(15);
				
	            cv.put(Friends.ICON_URL, iconUrl);
	            cv.put(Friends.LEVEL, level);
	            cv.put(Friends.MEMBER_TYPE, memberType);
	            cv.put(Friends.COMMENT, comment);
	            cv.put(Friends.LEVEL, level);
	            cv.put(Friends.ONLINE_STATUS, status);
				cv.put(Friends.TROPHIES_PLATINUM, platinum);
				cv.put(Friends.TROPHIES_GOLD, gold);
				cv.put(Friends.TROPHIES_SILVER, silver);
				cv.put(Friends.TROPHIES_BRONZE, bronze);
				cv.put(Friends.PLAYING, playing);
				cv.put(Friends.DELETE_MARKER, updated);
	            
				if (friendId < 0)
				{
					// New
		            cv.put(Friends.ONLINE_ID, onlineId);
		            cv.put(Friends.ACCOUNT_ID, accountId);
		            cv.put(Friends.PROGRESS, 0);
					cv.put(Friends.LAST_UPDATED, 0);
					
		            newCvs.add(cv);
				}
				else
				{
					cr.update(ContentUris.withAppendedId(Friends.CONTENT_URI,
							friendId), cv, null, null);
					
					rowsUpdated++;
				}
			}
			
			// Remove friends
			rowsDeleted = cr.delete(Friends.CONTENT_URI, 
					Friends.ACCOUNT_ID + "=" + accountId + " AND " +
					Friends.DELETE_MARKER + "!=" + updated, null);
			
			if (newCvs.size() > 0)
			{
				ContentValues[] cvs = new ContentValues[newCvs.size()];
				newCvs.toArray(cvs);
				
				rowsInserted = cr.bulkInsert(Friends.CONTENT_URI, cvs);
			}
			
			account.refresh(Preferences.get(mContext));
			account.setLastFriendUpdate(System.currentTimeMillis());
			account.save(Preferences.get(mContext));
			
			cr.notifyChange(Friends.CONTENT_URI, null);
			
			if (App.getConfig().logToConsole())
				started = displayTimeTaken("Friend page processing [I:" +
						rowsInserted + ";U:" + rowsUpdated + 
						";D:" + rowsDeleted + "]", started);
		}
	}
	
	@SuppressLint("DefaultLocale")
	@Override
	protected void parseFriendSummary(PsnAccount account, String friendOnlineId)
			throws ParserException, IOException
	{
		String url = String.format(URL_FRIEND_SUMMARY_f,
				URLEncoder.encode(friendOnlineId, "UTF-8"));
		String friendData = getResponse(url);
		
		ContentResolver cr = mContext.getContentResolver();
		Cursor c = cr.query(Friends.CONTENT_URI, FRIEND_ID_PROJECTION, 
				Friends.ACCOUNT_ID + "=" + account.getId() + " AND " + 
				Friends.ONLINE_ID + "=?", 
				new String[] { friendOnlineId }, null);
		
	    long updated = System.currentTimeMillis();
	    long started = updated;
		long friendId = -1;
		
		try
		{
	        if (c != null && c.moveToFirst())
	        	friendId = c.getLong(0);
		}
		finally
		{
			if (c != null)
				c.close();
		}
		
		Matcher m;
		Matcher friendMatcher = PATTERN_FRIEND_SUMMARY.matcher(friendData); 
		if (friendMatcher.find() && friendMatcher.find()) // skip the first
		{
			String friendCard = friendMatcher.group(1);
			
			String onlineId = null;
			if ((m = PATTERN_FRIEND_SUMMARY_ONLINE_ID.matcher(friendCard)).find())
				onlineId = htmlDecode(m.group(1));
			
			if (onlineId != null)
			{
				int progress = 0;
				if ((m = PATTERN_FRIEND_SUMMARY_PROGRESS.matcher(friendCard)).find())
					progress = Integer.parseInt(m.group(1));
				
				int level = 0;
				if ((m = PATTERN_FRIEND_SUMMARY_LEVEL.matcher(friendCard)).find())
					level = Integer.parseInt(m.group(1));
				
				String iconUrl = null;
				if ((m = PATTERN_FRIEND_SUMMARY_AVATAR.matcher(friendCard)).find())
					iconUrl = getLargeAvatarIcon(resolveImageUrl(url,
							m.group(1)));
				
				int memberType = PSN.MEMBER_TYPE_FREE;
				if ((m = PATTERN_FRIEND_SUMMARY_IS_PLUS.matcher(friendCard)).find())
					memberType = PSN.MEMBER_TYPE_PLUS;
				
				int bronze = 0;
				int silver = 0;
				int gold = 0;
				int platinum = 0;
				
				m = PATTERN_FRIEND_SUMMARY_TROPHIES.matcher(friendCard);
				while (m.find())
				{
					String type = m.group(1).toLowerCase();
					if ("bronze".equals(type))
						bronze = Integer.parseInt(m.group(2));
					else if ("silver".equals(type))
						silver = Integer.parseInt(m.group(2));
					else if ("gold".equals(type))
						gold = Integer.parseInt(m.group(2));
					else if ("platinum".equals(type))
						platinum = Integer.parseInt(m.group(2));
				}
				
				ContentValues cv = new ContentValues(15);
				
				cv.put(Friends.LAST_UPDATED, updated);
	            cv.put(Friends.ONLINE_ID, onlineId);
	            cv.put(Friends.ICON_URL, iconUrl);
	            cv.put(Friends.LEVEL, level);
	            cv.put(Friends.PROGRESS, progress);
				cv.put(Friends.TROPHIES_PLATINUM, platinum);
				cv.put(Friends.TROPHIES_GOLD, gold);
				cv.put(Friends.TROPHIES_SILVER, silver);
				cv.put(Friends.TROPHIES_BRONZE, bronze);
				cv.put(Friends.MEMBER_TYPE, memberType);
				
				if (friendId < 0)
				{
					// New
		            cv.put(Friends.ACCOUNT_ID, account.getId());
		            cv.put(Friends.ONLINE_STATUS, PSN.STATUS_OTHER);
					cv.put(Friends.PLAYING, (String)null);
					
					cr.insert(Friends.CONTENT_URI, cv);
				}
				else
				{
					// Existing
					cr.update(Friends.CONTENT_URI, 
							cv, Friends._ID + "=" + friendId, null);
				}
				
				cr.notifyChange(Friends.CONTENT_URI, null);
			}
		}
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("parseCompareGames/processing", started);
	}
	
	@Override
	protected GamerProfileInfo parseGamerProfile(PsnAccount account, String psnId)
			throws ParserException, IOException
	{
		String url = String.format(URL_FRIEND_SUMMARY_f,
				URLEncoder.encode(psnId, "UTF-8"));
		String friendData = getResponse(url);
		
	    long updated = System.currentTimeMillis();
	    long started = updated;
		
	    GamerProfileInfo gpi = new GamerProfileInfo();
	    
		Matcher m;
		Matcher friendMatcher = PATTERN_FRIEND_SUMMARY.matcher(friendData); 
		if (friendMatcher.find() && friendMatcher.find()) // skip the first
		{
			String friendCard = friendMatcher.group(1);
			
			String onlineId = null;
			if ((m = PATTERN_FRIEND_SUMMARY_ONLINE_ID.matcher(friendCard)).find())
				onlineId = htmlDecode(m.group(1));
			
			if (onlineId != null)
			{
				int progress = 0;
				if ((m = PATTERN_FRIEND_SUMMARY_PROGRESS.matcher(friendCard)).find())
					progress = Integer.parseInt(m.group(1));
				
				int level = 0;
				if ((m = PATTERN_FRIEND_SUMMARY_LEVEL.matcher(friendCard)).find())
					level = Integer.parseInt(m.group(1));
				
				String iconUrl = null;
				if ((m = PATTERN_FRIEND_SUMMARY_AVATAR.matcher(friendCard)).find())
					iconUrl = getLargeAvatarIcon(resolveImageUrl(url,
							m.group(1)));
				
				int bronze = 0;
				int silver = 0;
				int gold = 0;
				int platinum = 0;
				
				m = PATTERN_FRIEND_SUMMARY_TROPHIES.matcher(friendCard);
				while (m.find())
				{
					String type = m.group(1).toLowerCase();
					if ("bronze".equals(type))
						bronze = Integer.parseInt(m.group(2));
					else if ("silver".equals(type))
						silver = Integer.parseInt(m.group(2));
					else if ("gold".equals(type))
						gold = Integer.parseInt(m.group(2));
					else if ("platinum".equals(type))
						platinum = Integer.parseInt(m.group(2));
				}
				
				gpi.OnlineId = onlineId;
				gpi.AvatarUrl = iconUrl;
				gpi.Level = level;
				gpi.Progress = progress;
				gpi.PlatinumTrophies = platinum;
				gpi.GoldTrophies = gold;
				gpi.SilverTrophies = silver;
				gpi.BronzeTrophies = bronze;
				gpi.OnlineStatus = PSN.STATUS_OTHER;
				gpi.Playing = null;
			}
		}
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("parseGamerProfile/processing", started);
		
		return gpi;
	}
	
	@Override
	protected ComparedGameInfo parseCompareGames(PsnAccount account,
			String friendId) throws ParserException, IOException
	{
		// Request comparison
		String comparePage = getResponse(String.format(URL_COMPARE_GAMES_f, 
				URLEncoder.encode(friendId, "UTF-8")));
		
	    long started = System.currentTimeMillis();
	    
		ComparedGameInfo cgi = new ComparedGameInfo(mContext.getContentResolver());
	    cgi.myAvatarIconUrl = account.getIconUrl();
	    
		Matcher m;
		
		int friendIndex = 1;
		
		// Determine the column index of the friend
		m = PATTERN_COMPARE_USERNAME.matcher(comparePage);
		for (int i = 0; m.find(); i++)
		{
			String userName = m.group(1);
			if (userName.equalsIgnoreCase(friendId))
			{
				friendIndex = i;
				break;
			}
		}
		
		m = PATTERN_COMPARE_AVATAR_URL.matcher(comparePage);
		for (int i = 0; i < friendIndex; i++)
			m.find(); // Skip to needed column
		
		if (m.find())
			cgi.yourAvatarIconUrl = getLargeAvatarIcon(resolveImageUrl(URL_PROFILE_SUMMARY,
					m.group(1)));
		
		Matcher rowMatcher = PATTERN_COMPARED_GAMES.matcher(comparePage);
		while (rowMatcher.find())
		{
			String uid = rowMatcher.group(1);
			String gameContent = rowMatcher.group(2);
			
			String title = null;
			String iconUrl = null;
			
			int selfPlatinum = 0;
			int selfGold = 0;
			int selfSilver = 0;
			int selfBronze = 0;
			int selfProgress = 0;
			boolean selfPlayed = false;
			
			int oppPlatinum = 0;
			int oppGold = 0;
			int oppSilver = 0;
			int oppBronze = 0;
			int oppProgress = 0;
			boolean oppPlayed = false;
			
			if ((m = PATTERN_COMPARE_GAME_TITLE.matcher(gameContent)).find())
				title = htmlDecode(m.group(1));
			
			if ((m = PATTERN_COMPARE_GAME_ICON.matcher(gameContent)).find())
				iconUrl = getLargeTrophyIcon(resolveImageUrl(URL_COMPARE_GAMES_f, 
						m.group(1)));
			
			Matcher playerMatcher = PATTERN_COMPARE_GAME_PLAYER.matcher(gameContent);
			if (playerMatcher.find())
			{
				// Skip the first (game data)
				
				if (playerMatcher.find())
				{
					// Account owner
					String playerContent = playerMatcher.group(1);
					
					m = PATTERN_COMPARE_GAME_PROGRESS.matcher(playerContent);
					if (m.find())
					{
						selfPlayed = true;
						selfProgress = Integer.parseInt(m.group(1));
						
						m = PATTERN_COMPARE_GAME_TROPHIES.matcher(playerContent);
						if (m.find())
						{
							selfBronze = Integer.parseInt(m.group(1));
							if (m.find())
							{
								selfSilver = Integer.parseInt(m.group(1));
								if (m.find())
								{
									selfGold = Integer.parseInt(m.group(1));
									if (m.find())
									{
										selfPlatinum  = Integer.parseInt(m.group(1));
									}
								}
							}
						}
					}
				}
				
				boolean found = false;
				for (int i = 0; i < friendIndex; i++)
					found = playerMatcher.find(); // Skip to needed column
				
				if (found)
				{
					// Opponent
					String oppContent = playerMatcher.group(1);
					
					m = PATTERN_COMPARE_GAME_PROGRESS.matcher(oppContent);
					if (m.find())
					{
						oppPlayed = true;
						oppProgress = Integer.parseInt(m.group(1));
						
						m = PATTERN_COMPARE_GAME_TROPHIES.matcher(oppContent);
						if (m.find())
						{
							oppBronze = Integer.parseInt(m.group(1));
							if (m.find())
							{
								oppSilver = Integer.parseInt(m.group(1));
								if (m.find())
								{
									oppGold = Integer.parseInt(m.group(1));
									if (m.find())
									{
										oppPlatinum  = Integer.parseInt(m.group(1));
									}
								}
							}
						}
					}
				}
			}
			
			cgi.cursor.addItem(uid, title, iconUrl, 
					selfPlayed, selfPlatinum, selfGold, selfSilver, selfBronze, selfProgress, 
					oppPlayed, oppPlatinum, oppGold, oppSilver, oppBronze, oppProgress);
		}
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("parseCompareGames/processing", started);
		
		return cgi;
	}
	
	@Override
	protected ComparedTrophyInfo parseCompareTrophies(PsnAccount account,
			String friendId, String gameId)
			throws ParserException, IOException
	{
		String url = String.format(URL_COMPARE_TROPHIES_f, 
				URLEncoder.encode(gameId, "UTF-8"), 
				URLEncoder.encode(friendId, "UTF-8"));
		String page = getResponse(url);
		
		ComparedTrophyInfo cti = new ComparedTrophyInfo(mContext.getContentResolver());
		
	    long started = System.currentTimeMillis();
	    
		Matcher trophyMatcher = PATTERN_COMPARED_TROPHIES.matcher(page);
		while (trophyMatcher.find())
		{
			String trophyRow = trophyMatcher.group(1);
			
			Matcher m;
			
			int type = 0;
			if (false) // FIXME (m = PATTERN_TROPHY_TYPE.matcher(trophyRow)).find())
			{
				String trophyType = m.group(1).toUpperCase();
				if (trophyType.equals("BRONZE"))
					type = PSN.TROPHY_BRONZE;
				else if (trophyType.equals("SILVER"))
					type = PSN.TROPHY_SILVER;
				else if (trophyType.equals("GOLD"))
					type = PSN.TROPHY_GOLD;
				else if (trophyType.equals("PLATINUM"))
					type = PSN.TROPHY_PLATINUM;
				else if (trophyType.equals("HIDDEN"))
					type = PSN.TROPHY_SECRET;
			}
			
			String title = null;
			String description = null;
			boolean isSecret = (type == PSN.TROPHY_SECRET);
			
			if (!isSecret)
			{
				if ((m = PATTERN_TROPHY_TITLE.matcher(trophyRow)).find())
					title = htmlDecode(m.group(1).trim());
				
				if ((m = PATTERN_TROPHY_DESCRIPTION.matcher(trophyRow)).find())
					description = htmlDecode(m.group(1).trim());
			}
			else
			{
				title = mContext.getString(R.string.secret_trophy);
				description = mContext.getString(R.string.this_is_a_secret_trophy);
			}
			
			String iconUrl = null;
			if ((m = PATTERN_TROPHY_ICON.matcher(trophyRow)).find())
				iconUrl = getLargeTrophyIcon(resolveImageUrl(url, m.group(1)));
			
			boolean isLocked = true;
			String selfEarned = null;
			String oppEarned = null;
			
			m = PATTERN_COMPARED_TROPHY_PERSON.matcher(trophyRow);
			if (m.find())
			{
				// FIXME
//				Matcher m2;
//				m2 = PATTERN_TROPHY_ATTAINED.matcher(m.group(1)); 
//				if (m2.find())
//				{
//					selfEarned = truncateAttainmentDate(m2.group(1));
//					isLocked = false;
//				}
//				else
//					selfEarned = mContext.getString(R.string.trophy_locked);
//				
//				if (m.find())
//				{
//					m2 = PATTERN_TROPHY_ATTAINED.matcher(m.group(1)); 
//					if (m2.find())
//						oppEarned = truncateAttainmentDate(m2.group(1));
//					else
//						oppEarned = mContext.getString(R.string.trophy_locked);
//				}
			}
			
			cti.cursor.addItem(title, description, iconUrl, type, 
					isSecret, isLocked, selfEarned, oppEarned);
		}
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("parseCompareTrophies/processing", 
					started);
		
		return cti;
	}
	
	@Override
	protected GameCatalogList parseGameCatalog(int console, int page,
			int releaseStatus, int sortOrder) throws ParserException, IOException
	{
		String[] rating = null;
		String[] genre = null;
		String releaseSpec = null;
		String sortOrderSpec = null;
		String consoleSpec = null;
		
		if (console == PSN.CATALOG_CONSOLE_PSVITA)
			consoleSpec = "psvita";
		else if (console == PSN.CATALOG_CONSOLE_PSP)
			consoleSpec = "psp";
		else if (console == PSN.CATALOG_CONSOLE_PS2)
			consoleSpec = "ps2";
		else 
			consoleSpec = "ps3";
		
		if (sortOrder == PSN.CATALOG_SORT_BY_ALPHA)
			sortOrderSpec = "ALPHANUMERIC";
		else
			sortOrderSpec = "RELEASE_DATE";
		
		if (releaseStatus == PSN.CATALOG_RELEASE_OUT_NOW)
			releaseSpec = "out-now";
		else
			releaseSpec = "coming-soon";
		
		int pageSize = 12;
		
		List<NameValuePair> inputs = new ArrayList<NameValuePair>(3);
		
		addValue(inputs, "sort", sortOrderSpec); // ?
		addValue(inputs, "vertical", consoleSpec); // ?
		addValue(inputs, "gameFilter.tab", releaseSpec); 
		addValue(inputs, "gameFilter.ageRatingBandString", 
				(rating == null) ? "18" : rating);
		
		if (genre != null)
			addValue(inputs, "gameFilter.genreIdsString", genre);
		
		addValue(inputs, "page", page + "");
		
	    long started = System.currentTimeMillis();
	    
		String catalogPage = getResponse(URL_GAME_CATALOG, inputs, true);
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("parseGameCatalog/data fetch", started);
		
		int fetchedPage = page;
		int totalPages = 0;
		
		Matcher m;
		if ((m = PATTERN_GAME_CATALOG_STATS.matcher(catalogPage)).find())
		{
			fetchedPage = Integer.parseInt(m.group(1));
			totalPages = Integer.parseInt(m.group(2));
		}
		
		Matcher itemMatcher = PATTERN_GAME_CATALOG_ITEMS.matcher(catalogPage);
		
		GameCatalogList catalog = new GameCatalogList();
		
		catalog.PageNumber = fetchedPage;
		catalog.PageSize = pageSize;
		catalog.MorePages = totalPages > fetchedPage;
		
		if (App.getConfig().logToConsole())
			App.logv("fetched: " + fetchedPage + "; totalPages: "
					+ totalPages + "; more?: " + catalog.MorePages);
		
		URL baseUrl = new URL(URL_GAME_CATALOG);
		
		SimpleDateFormat mdyParser = new SimpleDateFormat("d MMM yyyy");
		Pattern mdyDetector = Pattern.compile("^\\d+ \\S+ \\d{4}$");
		Pattern myDetector = Pattern.compile("^\\S+ \\d{4}$");
		
		while (itemMatcher.find())
		{
			String content = itemMatcher.group(1);
			m = PATTERN_GAME_CATALOG_NODE.matcher(content);
			
			GameCatalogItem item = new GameCatalogItem();
			
			while (m.find())
			{
				String nodeName = m.group(1);
				
				if (nodeName != null)
				{
					if (nodeName.equals("title"))
					{
						if (item.Title == null) // LAME!
							item.Title = htmlDecode(m.group(2));
					}
					else if (nodeName.equals("release_date"))
					{
						item.ReleaseDate = htmlDecode(m.group(2));
						item.ReleaseDateTicks = 0;
						
						String parseDate = null;
						if (mdyDetector.matcher(item.ReleaseDate).find())
							parseDate = item.ReleaseDate;
						else if (myDetector.matcher(item.ReleaseDate).find())
							parseDate = "1 " + item.ReleaseDate;
						
						if (parseDate != null)
						{
							try
							{
								item.ReleaseDateTicks = mdyParser.parse(parseDate).getTime();
							}
							catch(Exception e)
							{
							}
						}
					}
					else if (nodeName.equals("genre"))
						item.Genre = htmlDecode(m.group(2));
					else if (nodeName.equals("detail_link"))
						item.DetailUrl = new URL(baseUrl, m.group(2)).toString();
					else if (nodeName.equals("thumbnail_image_url"))
						item.BoxartUrl = new URL(baseUrl, m.group(2)).toString();
				}
			}
			
			if (item.Title == null)
				continue;
			
			catalog.Items.add(item);
		}
		
		if (App.getConfig().logToConsole())
			displayTimeTaken("parseGameCatalog/parsing", started);
		
		return catalog;
	}
	
	protected GameCatalogItemDetails parseGameCatalogItemDetails(GameCatalogItem item)
			throws ParserException, IOException
	{
		if (item == null)
			return null;
		
		//boolean isAlt = false;
		GameCatalogItemDetails details = GameCatalogItemDetails.fromItem(item);
		
	    long started = System.currentTimeMillis();
	    
		String detailPage = getResponse(item.DetailUrl);
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("parseGameCatalogItemDetails/data fetch", started);
		
		Matcher m;
		if ((m = PATTERN_GAME_CATALOG_DETAIL_DESC.matcher(detailPage)).find())
		{
			details.Description = m.group(1);
		}
		else if ((m = PATTERN_GAME_CATALOG_DETAIL_DESC_ALT.matcher(detailPage)).find())
		{
			details.Description = m.group(1);
			//isAlt = true;
		}
		
		if (App.getConfig().logToConsole())
			displayTimeTaken("parseGameCatalogItemDetails/parsing", started);
		
		return details;
	}
	
	private String truncateAttainmentDate(String attained)
	{
		int pos = attained.indexOf(":");
		if (pos < 1)
			return attained;
		
		return attained.substring(pos + 1).trim();
	}
	
	private String getLargeTrophyIcon(String url)
	{
		if (url == null)
			return null;
		
		int pos = url.indexOf("/trophy/");
		if (pos < 1)
			return url;
		
		return LARGE_TROPHY_ICON_PREFIX + url.substring(pos).trim();
	}
	
	private String getLargeAvatarIcon(String url)
	{
		if (url == null)
			return null;
		
		int pos = url.indexOf("/avatar/");
		if (pos < 1)
			return url;
		
		return LARGE_AVATAR_ICON_PREFIX + url.substring(pos).trim();
	}
	
	private long parseRelativeDate(String relativeDate)
	{
		if (relativeDate == null)
			return 0;
		
		Matcher m = PATTERN_PARSE_RELATIVE_DATE.matcher(relativeDate);
		if (!m.find())
			return 0;
		
		int value = Integer.parseInt(m.group(1));
		String unit = m.group(2).toLowerCase();
		
		long unitInSeconds;
		
		if (unit.startsWith("second"))
			unitInSeconds = value;
		else if (unit.startsWith("minute"))
			unitInSeconds = value * 60;
		else if (unit.startsWith("hour"))
			unitInSeconds = value * 3600;
		else if (unit.startsWith("day"))
			unitInSeconds = value * 3600 * 24;
		else if (unit.startsWith("week"))
			unitInSeconds = value * 3600 * 24 * 7;
		else if (unit.startsWith("month"))
			unitInSeconds = (long)((float)value * 3600.0 * 24.0 * (365.25 / 12.0));
		else if (unit.startsWith("year"))
			unitInSeconds = (long)((float)value * 3600.0 * 24.0 * 365.25);
		else 
			return 0;
		
		return System.currentTimeMillis() - (unitInSeconds * 1000);
	}
	
	@Override
	public RssChannel fetchBlog() throws ParserException
	{
		try
		{
			return RssHandler.getFeed(URL_BLOG);
		}
		catch(Exception e)
		{
			throw new ParserException(mContext, R.string.error_loading_blog);
		}
	}
}
