/*
 * PsnUsParser.java 
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

package com.akop.bach.parser;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.akop.bach.BasicAccount;
import com.akop.bach.App;
import com.akop.bach.PSN;
import com.akop.bach.PSN.ComparedGameCursor;
import com.akop.bach.PSN.ComparedGameInfo;
import com.akop.bach.PSN.ComparedTrophyCursor;
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
import com.akop.bach.util.rss.RssHandler;

public class PsnUsParser
		extends PsnParser
{
	protected static final String URL_BLOG = "http://feeds.feedburner.com/PSBlog?format=xml";	
	
	private static final DateFormat TROPHY_DATE_FORMAT = 
		new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy");
	private static final DateFormat COMPARE_TROPHY_DATE_FORMAT = 
		new SimpleDateFormat("MMM d, yyyy hh:mm:ss a zzz");
	
	protected static final String URL_LOGIN = "https://store.playstation.com/j_acegi_external_security_check?target=/external/login.action";
	private static final String URL_RETURN_LOGIN = "http://us.playstation.com/uwps/PSNTicketRetrievalGenericServlet";
	
	private static final String URL_GAMES = "http://us.playstation.com/playstation/psn/profile/%1$s/get_ordered_trophies_data";
	private static final String URL_TROPHIES = "http://us.playstation.com/playstation/psn/profile/%1$s/get_ordered_title_details_data";
	private static final String URL_COMPARE_TROPHIES = "http://us.playstation.com/playstation/psn/profile/trophies/%1$s/compare/detail?ids=%2$s&ids=%3$s";
	private static final String URL_COMPARE_TROPHIES_DETAIL = "http://us.playstation.com/playstation/psn/profile/get_user_trophies_with_profile?title=%1$s&target=%2$s";
	
	private static final String URL_PROFILE_SUMMARY = "http://us.playstation.com/playstation/psn/profile/trophies?id=%1.16f";
	private static final String URL_FRIENDS = "http://us.playstation.com/playstation/psn/profile/friends?id=%1.16f";
	private static final String URL_GAMER_DETAIL = "http://us.playstation.com/playstation/psn/profiles/%1$s";
	
	private static final String URL_COMPARE_TROPHIES_REFERER = "http://us.playstation.com/playstation/psn/profile/compare?ids=%1$s";
	
	private static final String URL_GAME_CATALOG = "http://us.playstation.com/ps-products/BrowseGames";
	
	private static final Pattern PATTERN_URL = Pattern
			.compile("(http:[^'\"]+)");
	private static final Pattern PATTERN_LOGIN_REDIR_URL = Pattern
			.compile("parent\\.location\\s*=\\s*'(http:[^']+)'");
	
	private static final Pattern PATTERN_ONLINE_ID = Pattern.compile(
			"<div id=\"id-handle\">\\s*(\\S+)\\s*</div>");
	private static final Pattern PATTERN_LEVEL = Pattern
			.compile("<div id=\"leveltext\">\\s*(\\d+)\\s*</div>");
	private static final Pattern PATTERN_PROGRESS = Pattern
			.compile("<div class=\"progresstext\">\\s*(\\d+)%\\s*</div>");
	private static final Pattern PATTERN_AVATAR_CONTAINER = Pattern
			.compile("<div id=\"id-avatar\">\\s(.*?)\\s*</div>");
	
	private static final Pattern PATTERN_GAMES = Pattern.compile(
			"<div class=\"slot\"[^>]*>(.*?)</div>\\s*</div>\\s*</div>\\s*</div>", 
			Pattern.DOTALL);
	private static final Pattern PATTERN_GAME_TITLE = Pattern.compile(
			"<span class=\"gameTitleSortField\">([^<]*)</span>");
	private static final Pattern PATTERN_GAME_ICON = Pattern.compile(
			"src=\"([^\"]*)\"");
	private static final Pattern PATTERN_GAME_PROGRESS = Pattern.compile(
			"<span class=\"gameProgressSortField\">\\s*(\\d+)\\s*</span>");
	private static final Pattern PATTERN_GAME_TROPHIES = Pattern.compile(
			"<div class=\"trophycontent\">\\s*(\\d+)\\s*</div>");
	private static final Pattern PATTERN_GAME_UID = Pattern.compile(
			"<a href=\"(/(?:[^/]+/)+([^\"]+))\">");
	
	private static final Pattern PATTERN_TROPHIES = Pattern.compile(
			"<div class=\"slot\\s*([^\"]*)\"[^>]*>(.*?)</div>\\s*</div>\\s*</div>\\s*</div>", 
			Pattern.DOTALL);
	private static final Pattern PATTERN_TROPHY_TITLE = Pattern.compile(
			"<span class=\"trophyTitleSortField\">([^<]*)</span>");
	private static final Pattern PATTERN_TROPHY_DESCRIPTION = Pattern.compile(
			"<span class=\"subtext\">([^<]*)</span>");
	private static final Pattern PATTERN_TROPHY_ICON = Pattern.compile(
			"src=\"([^\"]*)\"");
	private static final Pattern PATTERN_TROPHY_EARNED = Pattern.compile(
			"class=\"dateEarnedSortField\"[^>]*>([^<]*)</span>");
	private static final Pattern PATTERN_TROPHY_TYPE = Pattern.compile(
			"class=\"trophyTypeSortField\"[^>]*>\\s*([^<\\s]*)\\s*</span>");
	
	private static final Pattern PATTERN_FRIENDS = Pattern.compile(
			"name=\"ids\" value=\"([^\"]+)\"");
	
	private static final Pattern PATTERN_TROPHY_COUNT = Pattern.compile(
			"<div class=\"text ([^\"]*)\">(\\d+)[^<]*</div>");
	private static final Pattern PATTERN_URL_TROPHIES = Pattern.compile(
			"/TrophyDetailImage\\?([^\"]*)\"");
	
	private static final Pattern PATTERN_COMPARE_TROPHIES = Pattern.compile(
			"<div id=\"T_([^\"]+)\" class=\"slot\\s*([^\"]*)\"[^>]*>(.*?)</div>\\s*</div>\\s*</div>\\s*</div>", 
			Pattern.DOTALL);
	private static final Pattern PATTERN_COMPARE_TROPHIES_TITLE = Pattern.compile(
			"<span class=\"gameTitleSortField\">([^<]*)</span>");
	private static final Pattern PATTERN_COMPARE_TROPHIES_TITLE_ID = Pattern.compile(
			"/get_user_trophies_with_profile\\?title=([^&]+)&");
	
	private static final Pattern PATTERN_GAME_CATALOG_ITEMS = Pattern.compile(
			"<div class=\"bgame_list clearfix\">(.*?)</table>\\s*</div>",
			Pattern.DOTALL | Pattern.MULTILINE);
	
	private static final Pattern PATTERN_GAME_CATALOG_ESSENTIALS = Pattern.compile(
			"<div class=\"imagebox\">\\s*<a class=\"first\" href=" +
			"\"[^\"]*\"><img src=\"([^\"]*)\" alt=\"[^\"]*\" title=" +
			"\"[^\"]*\".*?<h6>\\s*<a href=\"([^\"]*)\" onclick=\"[^\"]*\">" +
			"([^<]*)</a>\\s*</h6>\\s*<p>(.*?)</p>", 
			Pattern.DOTALL);
	private static final Pattern PATTERN_GAME_CATALOG_STATS = Pattern.compile(
			"<p>([^:]*):\\s*([^<]*)</p>");
	
	private static final Pattern PATTERN_GAME_CATALOG_DETAILS = Pattern.compile(
			"<meta name=\"([^\"]+)\" content=\"([^\"]+)\"\\s*/>");
	
	private static final Pattern PATTERN_GAME_CATALOG_GAME_XML = Pattern
	        .compile("swf\"></a>\\s*<a href=\"([^\"]+\\.xml)\"></a>");
	
	private static final Pattern PATTERN_GAME_CATALOG_PSV_THUMBS = Pattern
			.compile("<img onclick=\"screenshotview\\(this\\)\" .*? src=\"([^\"]+)\"");
	private static final Pattern PATTERN_GAME_CATALOG_PSV_LARGE = Pattern
			.compile("id=\"ssimagelarge[^\"]+\"><img .*? src=\"([^\"]+)\"");
	
	private static final String SCREENSHOT_BASE_URL = 
			"http://webassets.scea.com/pscomauth/groups/public/documents/webasset/";
	
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
	
	public PsnUsParser(Context context)
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
	protected boolean onAuthenticate(BasicAccount account) throws IOException,
			ParserException
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
		params.setParameter("http.protocol.max-redirects", 2);
		
		CookieStore store = mHttpClient.getCookieStore();
		BasicClientCookie cookie = new BasicClientCookie(
				"APPLICATION_SITE_URL", "http://us.playstation.com/");
		cookie.setDomain(".playstation.com");
		cookie.setPath("/");
		store.addCookie(cookie);
		
		// Post authentication data
		String page = getResponse(URL_LOGIN, inputs);
		
		// Disable redirection
		params.setParameter("http.protocol.max-redirects", 1);
		
		// Get redirection URL
		Matcher m = PATTERN_LOGIN_REDIR_URL.matcher(page);
		
	    if (!m.find())
	    {
	    	if (App.LOGV)
	    		App.logv("onAuthUS: Redir URL not found");
	    	
	    	String outageMessage;
	    	if ((outageMessage = getOutageMessage(page)) != null)
	    		throw new ParserException(outageMessage);
	    	
	    	return false;
	    }
	    
		// 2. Post to redirection URL
	    
		try
		{
			getResponse(m.group(1));
		}
		catch(ClientProtocolException e)
		{
			// Ignore redirection error
		}
		
		return true;
	}
	
	@Override
	protected String getSessionFile(BasicAccount account)
	{
		return account.getUuid() + ".us.session";
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
	
	protected ContentValues parseSummaryData(PsnAccount account)
			throws IOException, ParserException
	{
	    long started = System.currentTimeMillis();
	    String url = String.format(URL_PROFILE_SUMMARY, Math.random());
	    
		HttpUriRequest request = new HttpGet(url);
		request.addHeader("Referer", "http://us.playstation.com/mytrophies/index.htm");
		request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		
		String page = getResponse(request);
		if (App.LOGV)
			started = displayTimeTaken("parseSummaryData page fetch", started);
		
		ContentValues cv = new ContentValues(10);
	    Matcher m;
	    
		if (!(m = PATTERN_ONLINE_ID.matcher(page)).find())
			throw new ParserException(mContext, R.string.error_online_id_not_detected);
		
		cv.put(Profiles.ONLINE_ID, m.group(1));
		
		int level = 0;
		if ((m = PATTERN_LEVEL.matcher(page)).find())
			level = Integer.parseInt(m.group(1));
		
		int progress = 0;
		if ((m = PATTERN_PROGRESS.matcher(page)).find())
			progress = Integer.parseInt(m.group(1));
		
		int trophiesPlat = 0;
		int trophiesGold = 0;
		int trophiesSilver = 0;
		int trophiesBronze = 0;
		
		if ((m = PATTERN_URL_TROPHIES.matcher(page)).find())
		{
			String queryString = m.group(1);
			String[] pairs = queryString.split("&");
			
			for (String pair : pairs)
			{
				String[] param = pair.split("=");
				if (param.length > 1)
				{
					if (param[0].equalsIgnoreCase("bronze"))
						trophiesBronze = Integer.parseInt(param[1]);
					else if (param[0].equalsIgnoreCase("silver"))
						trophiesSilver = Integer.parseInt(param[1]);
					else if (param[0].equalsIgnoreCase("gold"))
						trophiesGold = Integer.parseInt(param[1]);
					else if (param[0].equalsIgnoreCase("platinum"))
						trophiesPlat = Integer.parseInt(param[1]);
				}
			}
		}
		
		cv.put(Profiles.LEVEL, level);
		cv.put(Profiles.PROGRESS, progress);
		cv.put(Profiles.TROPHIES_PLATINUM, trophiesPlat);
		cv.put(Profiles.TROPHIES_GOLD, trophiesGold);
		cv.put(Profiles.TROPHIES_SILVER, trophiesSilver);
		cv.put(Profiles.TROPHIES_BRONZE, trophiesBronze);
		
		String iconUrl = null;
		if ((m = PATTERN_AVATAR_CONTAINER.matcher(page)).find())
		{
			if ((m = PATTERN_URL.matcher(m.group(1))).find())
				iconUrl = m.group(1);
		}
		
		cv.put(Profiles.ICON_URL, iconUrl);
		
		if (App.LOGV)
			started = displayTimeTaken("parseSummaryData processing", started);
		
		return cv;
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
		
		if (App.LOGV)
			displayTimeTaken("Summary update", started);
		
		account.refresh(Preferences.get(mContext));
		account.setOnlineId(cv.getAsString(Profiles.ONLINE_ID));
		account.setIconUrl(cv.getAsString(Profiles.ICON_URL));
		account.setLastSummaryUpdate(System.currentTimeMillis());
		account.save(Preferences.get(mContext));
	}
	
	protected void parseGames(PsnAccount account)
		throws ParserException, IOException
	{
		String page = getResponse(String.format(URL_GAMES, 
				URLEncoder.encode(account.getScreenName(), "UTF-8")), true);
		
		ContentResolver cr = mContext.getContentResolver();
		boolean changed = false;
		long updated = System.currentTimeMillis();
		Cursor c;
		String title;
		String iconUrl;
		String uid;
		int progress;
		int bronze;
		int silver;
		int gold;
		int platinum;
		String[] queryParams = new String[1];
		final long accountId = account.getId();
		ContentValues cv;
		List<ContentValues> newCvs = new ArrayList<ContentValues>(100);
		
        long started = System.currentTimeMillis();
        Matcher m;
		Matcher gameMatcher = PATTERN_GAMES.matcher(page);
		
		for (int rowNo = 1; gameMatcher.find(); rowNo++)
		{
			String group = gameMatcher.group(1);
			
			if (!(m = PATTERN_GAME_UID.matcher(group)).find())
				continue;
			
			uid = m.group(2);
			
			progress = 0;
			if ((m = PATTERN_GAME_PROGRESS.matcher(group)).find())
				progress = Integer.parseInt(m.group(1));
			
			bronze = silver = gold = platinum = 0;
			if ((m = PATTERN_GAME_TROPHIES.matcher(group)).find())
			{
				bronze = Integer.parseInt(m.group(1));
				
				if (m.find())
				{
					silver = Integer.parseInt(m.group(1));
					
					if (m.find())
					{
						gold = Integer.parseInt(m.group(1));
						
						if (m.find())
						{
							platinum = Integer.parseInt(m.group(1));
						}
					}
				}
			}
			
			// Check to see if we already have a record of this game
			queryParams[0] = uid;
			c = cr.query(Games.CONTENT_URI, GAMES_PROJECTION, Games.ACCOUNT_ID
					+ "=" + accountId + " AND " + Games.UID + "=?",
					queryParams, null);
			
        	changed = true;
        	
			try
			{
		        if (c == null || !c.moveToFirst()) // New game
		        {
					title = "";
					if ((m = PATTERN_GAME_TITLE.matcher(group)).find())
						title = htmlDecode(m.group(1));
					
					iconUrl = null;
					if ((m = PATTERN_GAME_ICON.matcher(group)).find())
						iconUrl = getAvatarImage(m.group(1));

					cv = new ContentValues(15);
					
	                cv.put(Games.ACCOUNT_ID, accountId);
	                cv.put(Games.TITLE, title);
	                cv.put(Games.UID, uid);
	                cv.put(Games.ICON_URL, iconUrl);
	                cv.put(Games.PROGRESS, progress);
	                cv.put(Games.SORT_ORDER, rowNo);
	    			cv.put(Games.UNLOCKED_PLATINUM, platinum);
	    			cv.put(Games.UNLOCKED_GOLD, gold);
	    			cv.put(Games.UNLOCKED_SILVER, silver);
	    			cv.put(Games.UNLOCKED_BRONZE, bronze);
					cv.put(Games.TROPHIES_DIRTY, 1);
					cv.put(Games.LAST_UPDATED, updated);
					
	                newCvs.add(cv);
		        }
		        else // Existing game
		        {
		        	boolean isDirty = false;
		        	long gameId = c.getLong(COLUMN_GAME_ID);
		        	
		        	cv = new ContentValues(15);
		        	
		        	if (c.getInt(COLUMN_GAME_PROGRESS) != progress)
		        	{
		        		isDirty = true;
		        		cv.put(Games.PROGRESS, progress);
		        	}
		        	if (c.getInt(COLUMN_GAME_BRONZE) != bronze)
		        	{
		        		isDirty = true;
		        		cv.put(Games.UNLOCKED_BRONZE, bronze);
		        	}
		        	if (c.getInt(COLUMN_GAME_SILVER) != silver)
		        	{
		        		isDirty = true;
		        		cv.put(Games.UNLOCKED_SILVER, silver);
		        	}
		        	if (c.getInt(COLUMN_GAME_GOLD) != gold)
		        	{
		        		isDirty = true;
		        		cv.put(Games.UNLOCKED_GOLD, gold);
		        	}
		        	if (c.getInt(COLUMN_GAME_PLATINUM) != platinum)
		        	{
		        		isDirty = true;
		        		cv.put(Games.UNLOCKED_PLATINUM, platinum);
		        	}
		        	
		        	if (isDirty)
		        		cv.put(Games.TROPHIES_DIRTY, 1);
		        	
	        		cv.put(Games.SORT_ORDER, rowNo);
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
		
		if (App.LOGV)
			started = displayTimeTaken("Game page processing", started);
		
		if (newCvs.size() > 0)
		{
			changed = true;
			
			ContentValues[] cvs = new ContentValues[newCvs.size()];
			newCvs.toArray(cvs);
			
			cr.bulkInsert(Games.CONTENT_URI, cvs);
			
			if (App.LOGV)
				displayTimeTaken("Game page insertion", started);
		}
		
		account.refresh(Preferences.get(mContext));
		account.setLastGameUpdate(System.currentTimeMillis());
		account.save(Preferences.get(mContext));
		
		if (changed)
			cr.notifyChange(Games.CONTENT_URI, null);
	}
	
	protected void parseTrophies(PsnAccount account, long gameId) 
		throws ParserException, IOException
	{
		// Find game record in local DB
		ContentResolver cr = mContext.getContentResolver(); 
		
		String gameUid = Games.getUid(mContext, gameId);
		List<NameValuePair> inputs = new ArrayList<NameValuePair>();
		inputs.add(new BasicNameValuePair("sortBy", "id_asc"));
		inputs.add(new BasicNameValuePair("titleId", gameUid));
		
		String page = getResponse(String.format(URL_TROPHIES, 
				URLEncoder.encode(account.getScreenName(), "UTF-8")), inputs, true);
		
	    int index = 0;
	    long started = System.currentTimeMillis();
	    
	    Matcher achievements = PATTERN_TROPHIES.matcher(page);
	    Matcher m;
	    
		List<ContentValues> cvList = new ArrayList<ContentValues>(100);
		while (achievements.find())
		{
			String trophyRow = achievements.group(2);
			
			String title = mContext.getString(R.string.secret_trophy);
			if ((m = PATTERN_TROPHY_TITLE.matcher(trophyRow)).find())
				title = htmlDecode(m.group(1));
			
			String description = mContext.getString(R.string.this_is_a_secret_trophy);
			if ((m = PATTERN_TROPHY_DESCRIPTION.matcher(trophyRow)).find())
				description = htmlDecode(m.group(1));
			
			String iconUrl = null;
			if ((m = PATTERN_TROPHY_ICON.matcher(trophyRow)).find())
				iconUrl = getAvatarImage(m.group(1));

			long earned = 0;
			if ((m = PATTERN_TROPHY_EARNED.matcher(trophyRow)).find())
			{
				try
				{
					earned = TROPHY_DATE_FORMAT.parse(m.group(1)).getTime();
				}
				catch (ParseException e)
				{
					if (App.LOGV)
						e.printStackTrace();
				}
			}
			
			boolean isSecret = false;
			int type = 0;
			
			if ((m = PATTERN_TROPHY_TYPE.matcher(trophyRow)).find())
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
					isSecret = true;
			}
			
			ContentValues cv = new ContentValues(20);
			
			cv.put(Trophies.TITLE, title);
			cv.put(Trophies.DESCRIPTION, description);
			cv.put(Trophies.SORT_ORDER, index);
			cv.put(Trophies.EARNED, earned);
			cv.put(Trophies.ICON_URL, iconUrl);
			cv.put(Trophies.GAME_ID, gameId);
			cv.put(Trophies.IS_SECRET, isSecret ? 1 : 0);
			cv.put(Trophies.TYPE, type);
			
			cvList.add(cv);
			index++;
		}
		
		if (App.LOGV)
			started = displayTimeTaken("New trophy parsing", started);
		
		ContentValues[] cva = new ContentValues[cvList.size()];
		cvList.toArray(cva);
		
		cr.delete(Trophies.CONTENT_URI, Trophies.GAME_ID + "=" + gameId, null);
		
		// Bulk-insert new trophies
		cr.bulkInsert(Trophies.CONTENT_URI, cva);
		cr.notifyChange(Trophies.CONTENT_URI, null);
		
		if (App.LOGV)
			started = displayTimeTaken("New trophy processing", started);
		
		// Update the game to remove the 'dirty' attribute
		ContentValues cv = new ContentValues(10);
		cv.put(Games.TROPHIES_DIRTY, 0); 
	    cr.update(Games.CONTENT_URI, cv, Games._ID + "=" + gameId, null);
		
		cr.notifyChange(ContentUris.withAppendedId(Games.CONTENT_URI, gameId), 
				null);
	    
		if (App.LOGV)
			displayTimeTaken("Updating Game", started);
	}

	protected String getAvatarImage(String iconUrl)
	{
		int index = iconUrl.indexOf("=");
		if (index >= 0)
			return iconUrl.substring(index + 1);

		return iconUrl;
	}
	
	protected void parseFriends(PsnAccount account) throws ParserException,
			IOException
	{
		synchronized (PsnUsParser.class)
		{
			String url = String.format(URL_FRIENDS, Math.random());
			HttpUriRequest request = new HttpGet(url);
			
			request.addHeader("Referer", "http://us.playstation.com/myfriends/");
			request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			
			String page = getResponse(request);
			
			ContentResolver cr = mContext.getContentResolver();
			final long accountId = account.getId();
			ContentValues cv;
			List<ContentValues> newCvs = new ArrayList<ContentValues>(100);
			
			int rowsInserted = 0;
			@SuppressWarnings("unused")
			int rowsUpdated = 0;
			int rowsDeleted = 0;
			
		    long updated = System.currentTimeMillis();
		    long started = updated;
			Matcher gameMatcher = PATTERN_FRIENDS.matcher(page);
			
			while (gameMatcher.find())
			{
				String friendGt = htmlDecode(gameMatcher.group(1));
				GamerProfileInfo gpi;
				
				try
				{
					gpi = parseGamerProfile(account, friendGt);
				}
				catch (IOException e)
				{
					if (App.LOGV)
						App.logv("Friend " + friendGt + " threw an IOException");
					
					// Update the DeleteMarker, so that the GT is not removed
					
					cv = new ContentValues(15);
					cv.put(Friends.DELETE_MARKER, updated);
					
					cr.update(Friends.CONTENT_URI, cv, Friends.ACCOUNT_ID + "="
							+ account.getId() + " AND " + Friends.ONLINE_ID
							+ "=?", new String[] { friendGt });
					
					continue;
				}
				catch(Exception e)
				{
					// The rest of the exceptions assume problems with Friend
					// and potentially remove him/her
					
					if (App.LOGV)
					{
						App.logv("Friend " + friendGt + " threw an Exception");
						e.printStackTrace();
					}
					
					continue;
				}
				
				Cursor c = cr.query(Friends.CONTENT_URI, FRIEND_ID_PROJECTION, 
						Friends.ACCOUNT_ID + "=" + account.getId() + " AND " + 
						Friends.ONLINE_ID + "=?", 
						new String[] { friendGt }, null);
				
				long friendId = -1;
				
				if (c != null)
				{
					try
					{
				        if (c.moveToFirst())
				        	friendId = c.getLong(0);
					}
					finally
					{
						c.close();
					}
				}
				
				cv = new ContentValues(15);
				
	            cv.put(Friends.ONLINE_ID, gpi.OnlineId);
	            cv.put(Friends.ICON_URL, gpi.AvatarUrl);
	            cv.put(Friends.LEVEL, gpi.Level);
	            cv.put(Friends.PROGRESS, gpi.Progress);
	            cv.put(Friends.ONLINE_STATUS, gpi.OnlineStatus);
				cv.put(Friends.TROPHIES_PLATINUM, gpi.PlatinumTrophies);
				cv.put(Friends.TROPHIES_GOLD, gpi.GoldTrophies);
				cv.put(Friends.TROPHIES_SILVER, gpi.SilverTrophies);
				cv.put(Friends.TROPHIES_BRONZE, gpi.BronzeTrophies);
				cv.put(Friends.PLAYING, gpi.Playing);
				cv.put(Friends.DELETE_MARKER, updated);
				cv.put(Friends.LAST_UPDATED, updated);
	            
				if (friendId < 0)
				{
					// New
		            cv.put(Friends.ACCOUNT_ID, accountId);
					
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
			
			if (App.LOGV)
				started = displayTimeTaken("Friend page processing [I:" +
						rowsInserted + ";U:" + rowsUpdated + 
						";D:" + rowsDeleted + "]", started);
		}
	}
	
	protected GamerProfileInfo parseGamerProfile(PsnAccount account,
			String onlineId) throws ParserException, IOException
	{
		onlineId = onlineId.trim();
		String url = String.format(URL_GAMER_DETAIL, 
				URLEncoder.encode(onlineId), 
				Math.random());
		String page = getResponse(url, true);
		
		Matcher m;
		if (!(m = PATTERN_ONLINE_ID.matcher(page)).find())
			throw new ParserException(mContext,
					R.string.psn_profile_not_found_f, onlineId);
		
		GamerProfileInfo gpi = new GamerProfileInfo();
		gpi.OnlineId = htmlDecode(m.group(1).trim());
		
		if ((m = PATTERN_PROGRESS.matcher(page)).find())
			gpi.Progress = Integer.parseInt(m.group(1));
		
		if ((m = PATTERN_LEVEL.matcher(page)).find())
			gpi.Level = Integer.parseInt(m.group(1));
		
		if ((m = PATTERN_AVATAR_CONTAINER.matcher(page)).find())
		{
			if ((m = PATTERN_URL.matcher(m.group(1))).find())
				gpi.AvatarUrl = m.group(1);
		}
		
		m = PATTERN_TROPHY_COUNT.matcher(page);
		while (m.find())
		{
			String type = m.group(1);
			
			if (type.equalsIgnoreCase("bronze"))
				gpi.BronzeTrophies = Integer.parseInt(m.group(2));
			else if (type.equalsIgnoreCase("silver"))
				gpi.SilverTrophies = Integer.parseInt(m.group(2));
			else if (type.equalsIgnoreCase("gold"))
				gpi.GoldTrophies = Integer.parseInt(m.group(2));
			else if (type.equalsIgnoreCase("platinum"))
				gpi.PlatinumTrophies = Integer.parseInt(m.group(2));
		}
		
		gpi.OnlineStatus = PSN.STATUS_OTHER;
		gpi.Playing = null;
		
		return gpi;
	}
	
	protected void parseFriendSummary(PsnAccount account, String friendOnlineId)
			throws ParserException, IOException
	{
	    long updated = System.currentTimeMillis();
	    long started = updated;
	    
		GamerProfileInfo gpi = parseGamerProfile(account, friendOnlineId);
		
		ContentResolver cr = mContext.getContentResolver();
		Cursor c = cr.query(Friends.CONTENT_URI, FRIEND_ID_PROJECTION, 
				Friends.ACCOUNT_ID + "=" + account.getId() + " AND " + 
				Friends.ONLINE_ID + "=?", 
				new String[] { friendOnlineId }, null);
		
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
		
		ContentValues cv = new ContentValues(15);
		
        cv.put(Friends.ONLINE_ID, gpi.OnlineId);
        cv.put(Friends.ICON_URL, gpi.AvatarUrl);
        cv.put(Friends.LEVEL, gpi.Level);
        cv.put(Friends.PROGRESS, gpi.Progress);
        cv.put(Friends.ONLINE_STATUS, gpi.OnlineStatus);
		cv.put(Friends.TROPHIES_PLATINUM, gpi.PlatinumTrophies);
		cv.put(Friends.TROPHIES_GOLD, gpi.GoldTrophies);
		cv.put(Friends.TROPHIES_SILVER, gpi.SilverTrophies);
		cv.put(Friends.TROPHIES_BRONZE, gpi.BronzeTrophies);
		cv.put(Friends.PLAYING, gpi.Playing);
		cv.put(Friends.LAST_UPDATED, updated);
        
		if (friendId < 0)
		{
			// New
            cv.put(Friends.ACCOUNT_ID, account.getId());
            cr.insert(Friends.CONTENT_URI, cv);
		}
		else
		{
			cr.update(ContentUris.withAppendedId(Friends.CONTENT_URI,
					friendId), cv, null, null);
		}
		
		if (App.LOGV)
			started = displayTimeTaken("Friend page processing", started);
		
		cr.notifyChange(ContentUris.withAppendedId(Friends.CONTENT_URI,
				friendId), null);
	}
	
	protected ComparedGameInfo parseCompareGames(PsnAccount account,
			String friendId)
			throws ParserException, IOException
	{
		String profileUrl = String.format(URL_GAMER_DETAIL, 
				URLEncoder.encode(friendId), Math.random());
		String profilePage = getResponse(profileUrl, true);
		
		Matcher m;
		String avatarUrl = null;
		
		if ((m = PATTERN_AVATAR_CONTAINER.matcher(profilePage)).find())
		{
			if ((m = PATTERN_URL.matcher(m.group(1))).find())
				avatarUrl = m.group(1);
		}
		
		String selfPage = getResponse(String.format(URL_GAMES, 
				URLEncoder.encode(account.getScreenName(), "UTF-8")), true);
		String oppPage = getResponse(String.format(URL_GAMES, 
				URLEncoder.encode(friendId, "UTF-8")), true);
		
		String uid;
		int bronze;
		int silver;
		int gold;
		int platinum;
		int progress;
		
		Map<Integer, Object> game;
		ArrayList<Map<Integer, Object>> games = new ArrayList<Map<Integer,Object>>();
		Map<String, Map<Integer, Object>> gameMap = 
			new HashMap<String, Map<Integer, Object>>();
		
	    long started = System.currentTimeMillis();
		
		// Own games
		
		Matcher gameMatcher = PATTERN_GAMES.matcher(selfPage);
		for (; gameMatcher.find(); )
		{
			String group = gameMatcher.group(1);
			
			if (!(m = PATTERN_GAME_UID.matcher(group)).find())
				continue;
			
			uid = m.group(2);
			
			//if (!gamePtr.containsKey(uid))
			//{
			game = new HashMap<Integer, Object>();
			game.put(ComparedGameCursor.COLUMN_UID, uid);
			
			if ((m = PATTERN_GAME_TITLE.matcher(group)).find())
				game.put(ComparedGameCursor.COLUMN_TITLE, htmlDecode(m.group(1)));
			else
				game.put(ComparedGameCursor.COLUMN_TITLE, null);
			
			if ((m = PATTERN_GAME_ICON.matcher(group)).find())
				game.put(ComparedGameCursor.COLUMN_ICON_URL, getAvatarImage(m.group(1)));
			else
				game.put(ComparedGameCursor.COLUMN_ICON_URL, null);
			
			games.add(game);
			gameMap.put(uid, game);
			//}
			//else
			//{
			//	game = gamePtr.get(uid);
			//}
			
			progress = bronze = silver = gold = platinum = 0;
			if ((m = PATTERN_GAME_PROGRESS.matcher(group)).find())
				progress = Integer.parseInt(m.group(1));
			
			if ((m = PATTERN_GAME_TROPHIES.matcher(group)).find())
			{
				bronze = Integer.parseInt(m.group(1));
				
				if (m.find())
				{
					silver = Integer.parseInt(m.group(1));
					
					if (m.find())
					{
						gold = Integer.parseInt(m.group(1));
						
						if (m.find())
							platinum = Integer.parseInt(m.group(1));
					}
				}
			}
			
			game.put(ComparedGameCursor.COLUMN_SELF_PLAYED, true);
			game.put(ComparedGameCursor.COLUMN_SELF_PROGRESS, progress);
			
			game.put(ComparedGameCursor.COLUMN_SELF_BRONZE, bronze);
			game.put(ComparedGameCursor.COLUMN_SELF_SILVER, silver);
			game.put(ComparedGameCursor.COLUMN_SELF_GOLD, gold);
			game.put(ComparedGameCursor.COLUMN_SELF_PLATINUM, platinum);
			
			// Defaults for opponent
			
			game.put(ComparedGameCursor.COLUMN_OPP_PLAYED, false);
			game.put(ComparedGameCursor.COLUMN_OPP_PROGRESS, 0);
			
			game.put(ComparedGameCursor.COLUMN_OPP_BRONZE, 0);
			game.put(ComparedGameCursor.COLUMN_OPP_SILVER, 0);
			game.put(ComparedGameCursor.COLUMN_OPP_GOLD, 0);
			game.put(ComparedGameCursor.COLUMN_OPP_PLATINUM, 0);
		}
		
		if (App.LOGV)
			started = displayTimeTaken("parseCompareGames/Own page processing", started);
		
		// Opponent's games
		
		gameMatcher = PATTERN_GAMES.matcher(oppPage);
		for (; gameMatcher.find(); )
		{
			String group = gameMatcher.group(1);
			
			if (!(m = PATTERN_GAME_UID.matcher(group)).find())
				continue;
			
			uid = m.group(2);
			
			if (!gameMap.containsKey(uid))
			{
				game = new HashMap<Integer, Object>();
				game.put(ComparedGameCursor.COLUMN_UID, uid);
				
				if ((m = PATTERN_GAME_TITLE.matcher(group)).find())
					game.put(ComparedGameCursor.COLUMN_TITLE, htmlDecode(m.group(1)));
				else
					game.put(ComparedGameCursor.COLUMN_TITLE, null);
				
				if ((m = PATTERN_GAME_ICON.matcher(group)).find())
					game.put(ComparedGameCursor.COLUMN_ICON_URL,
							getAvatarImage(m.group(1)));
				else
					game.put(ComparedGameCursor.COLUMN_ICON_URL, null);
				
				game.put(ComparedGameCursor.COLUMN_SELF_PROGRESS, 0);
				game.put(ComparedGameCursor.COLUMN_SELF_PLAYED, false);
				
				game.put(ComparedGameCursor.COLUMN_SELF_BRONZE, 0);
				game.put(ComparedGameCursor.COLUMN_SELF_SILVER, 0);
				game.put(ComparedGameCursor.COLUMN_SELF_GOLD, 0);
				game.put(ComparedGameCursor.COLUMN_SELF_PLATINUM, 0);
				
				games.add(game);
				gameMap.put(uid, game);
			}
			else
			{
				game = gameMap.get(uid);
			}
			
			progress = bronze = silver = gold = platinum = 0;
			if ((m = PATTERN_GAME_PROGRESS.matcher(group)).find())
				progress = Integer.parseInt(m.group(1));
			
			if ((m = PATTERN_GAME_TROPHIES.matcher(group)).find())
			{
				bronze = Integer.parseInt(m.group(1));
				
				if (m.find())
				{
					silver = Integer.parseInt(m.group(1));
					
					if (m.find())
					{
						gold = Integer.parseInt(m.group(1));
						
						if (m.find())
							platinum = Integer.parseInt(m.group(1));
					}
				}
			}
			
			game.put(ComparedGameCursor.COLUMN_OPP_PLAYED, true);
			game.put(ComparedGameCursor.COLUMN_OPP_PROGRESS, progress);
			
			game.put(ComparedGameCursor.COLUMN_OPP_BRONZE, bronze);
			game.put(ComparedGameCursor.COLUMN_OPP_SILVER, silver);
			game.put(ComparedGameCursor.COLUMN_OPP_GOLD, gold);
			game.put(ComparedGameCursor.COLUMN_OPP_PLATINUM, platinum);
		}
		
		if (App.LOGV)
			started = displayTimeTaken("parseCompareGames/Opp. page processing", started);
		
		ComparedGameInfo cgi = new ComparedGameInfo(mContext.getContentResolver());
		
		cgi.myAvatarIconUrl = account.getIconUrl();
		cgi.yourAvatarIconUrl = avatarUrl;
		
		for (Map<Integer, Object> g: games)
		{
			cgi.cursor.addItem((String)g.get(ComparedGameCursor.COLUMN_UID), 
					(String)g.get(ComparedGameCursor.COLUMN_TITLE), 
					(String)g.get(ComparedGameCursor.COLUMN_ICON_URL),
					(Boolean)g.get(ComparedGameCursor.COLUMN_SELF_PLAYED), 
					(Integer)g.get(ComparedGameCursor.COLUMN_SELF_PLATINUM), 
					(Integer)g.get(ComparedGameCursor.COLUMN_SELF_GOLD), 
					(Integer)g.get(ComparedGameCursor.COLUMN_SELF_SILVER), 
					(Integer)g.get(ComparedGameCursor.COLUMN_SELF_BRONZE), 
					(Integer)g.get(ComparedGameCursor.COLUMN_SELF_PROGRESS),
					(Boolean)g.get(ComparedGameCursor.COLUMN_OPP_PLAYED), 
					(Integer)g.get(ComparedGameCursor.COLUMN_OPP_PLATINUM), 
					(Integer)g.get(ComparedGameCursor.COLUMN_OPP_GOLD), 
					(Integer)g.get(ComparedGameCursor.COLUMN_OPP_SILVER), 
					(Integer)g.get(ComparedGameCursor.COLUMN_OPP_BRONZE), 
					(Integer)g.get(ComparedGameCursor.COLUMN_OPP_PROGRESS));
		}
		
		return cgi;
	}
	
	protected GameCatalogItemDetails parseGameCatalogItemDetails(GameCatalogItem item)
	        throws ParserException, IOException
	{
		if (item == null)
			return null;
		
		GameCatalogItemDetails details = GameCatalogItemDetails.fromItem(item);
		
	    long started = System.currentTimeMillis();
	    
		String detailPage = getResponse(item.DetailUrl);
		
		if (App.LOGV)
			started = displayTimeTaken("parseGameCatalogItemDetails/data fetch", started);
		
		Matcher m = PATTERN_GAME_CATALOG_DETAILS.matcher(detailPage);
		while (m.find())
		{
			String key = m.group(1);
			String value = m.group(2);
			
			if (key.equals("gameTitle"))
				details.Description = value;
			else if (key.equals("gameLongDescription"))
				details.Description = value;
		}
		
		m = PATTERN_GAME_CATALOG_GAME_XML.matcher(detailPage);
		if (m.find())
		{
			// PS3 screenshots (Flash-based)
			
			String xmlFile = m.group(1);
			String xmlContents = null;
			
			try { xmlContents = getResponse(xmlFile); }
			catch(Exception e) { }
			
			if (xmlContents != null)
			{
				ArrayList<HashMap<String, String>> pairList = parsePairsInSimpleXml(xmlContents, "element");
				
				ArrayList<String> thumbs = new ArrayList<String>();
				ArrayList<String> screens = new ArrayList<String>();
				
				for (HashMap<String, String> pairs : pairList)
				{
					if (pairs.containsKey("thumb") && pairs.containsKey("large"))
					{
						thumbs.add(SCREENSHOT_BASE_URL + pairs.get("thumb"));
						screens.add(SCREENSHOT_BASE_URL + pairs.get("large"));
					}
				}
				
				details.ScreenshotsThumb = new String[thumbs.size()];
				thumbs.toArray(details.ScreenshotsThumb);
				
				details.ScreenshotsLarge = new String[screens.size()];
				screens.toArray(details.ScreenshotsLarge);
			}
		}
		else
		{
			// Try Vita screenshots
			
			ArrayList<String> thumbs = new ArrayList<String>();
			m = PATTERN_GAME_CATALOG_PSV_THUMBS.matcher(detailPage);
			
			while (m.find())
				thumbs.add(m.group(1));
			
			ArrayList<String> screens = new ArrayList<String>();
			m = PATTERN_GAME_CATALOG_PSV_LARGE.matcher(detailPage);
			
			while (m.find())
				screens.add(m.group(1));
			
			if (thumbs.size() > 0 && screens.size() == thumbs.size())
			{
				details.ScreenshotsThumb = new String[thumbs.size()];
				thumbs.toArray(details.ScreenshotsThumb);
				
				details.ScreenshotsLarge = new String[screens.size()];
				screens.toArray(details.ScreenshotsLarge);
			}
		}
		
		if (App.LOGV)
			displayTimeTaken("parseGameCatalogItemDetails/parsing", started);
		
		return details;
	}
	
	@Override
	protected GameCatalogList parseGameCatalog(int console, int page,
			int releaseStatus, int sortOrder) throws ParserException,
			IOException
	{
		String beginsWith = null;
		String[] rating = null;
		String[] genre = null;
		String publisher = null;
		String sortOrderSpec = null;
		String consoleSpec = null;
		
		if (console == PSN.CATALOG_CONSOLE_PSVITA)
			consoleSpec = "psvita";
		else if (console == PSN.CATALOG_CONSOLE_PSP)
			consoleSpec = "psp3000,pspgo";
		else if (console == PSN.CATALOG_CONSOLE_PS2)
			consoleSpec = "ps2";
		else 
			consoleSpec = "ps3";
		
		if (sortOrder == PSN.CATALOG_SORT_BY_ALPHA)
			sortOrderSpec = "a-z";
		else
			sortOrderSpec = "rDatenf";
		
		int pageSize = 12;
		
		List<NameValuePair> inputs = new ArrayList<NameValuePair>(3);
		
		addValue(inputs, "MaxReleaseDateValue", "2"); // ?
		addValue(inputs, "MinReleaseDateValue", "6"); // ?
		addValue(inputs, "beginsWith", (beginsWith == null) ? "Any" : beginsWith);
		addValue(inputs, "console", consoleSpec);
		addValue(inputs, "esrb", (rating == null) ? "All" : joinString(rating, ","));
		addValue(inputs, "genre", (genre == null) ? "All" : joinString(genre, ","));
		addValue(inputs, "lastAjaxCallTimeStamp", System.currentTimeMillis() - 36000000);
		addValue(inputs, "publisher", (publisher == null) ? "Any" : publisher);
		addValue(inputs, "recordsOnPage", pageSize + "");
		addValue(inputs, "sortOrder", sortOrderSpec);
		addValue(inputs, "throughAjax", "true");
		addValue(inputs, "viewType", "gridView");
		addValue(inputs, "page", page + "");
		
	    long started = System.currentTimeMillis();
	    
		String catalogPage = getResponse(URL_GAME_CATALOG, inputs, true);
		catalogPage = htmlDecode(catalogPage);
		
		int spacePos;
		int records = 0;
		
		if ((spacePos = catalogPage.indexOf(" ")) > 0)
		{
			try
			{
				records = Integer.parseInt(catalogPage.substring(0, spacePos));
			}
			catch(NumberFormatException ex) { }
		}
		
		if (App.LOGV)
			started = displayTimeTaken("parseGameCatalog/data fetch", started);
		
		Matcher m;
		Matcher itemMatcher = PATTERN_GAME_CATALOG_ITEMS.matcher(catalogPage);
		
		GameCatalogList catalog = new GameCatalogList();
		
		catalog.PageNumber = page;
		catalog.PageSize = pageSize;
		
		//SimpleDateFormat myParser = new SimpleDateFormat("MM.yyyy");
		//Pattern myDetector = Pattern.compile("^\\d{2}\\.\\d{4}$");
		boolean noMatches = false;
		
		while (itemMatcher.find())
		{
			String content = itemMatcher.group(1);
			
			if (!(m = PATTERN_GAME_CATALOG_ESSENTIALS.matcher(content)).find())
			{
				noMatches = true;
				if (App.LOGV)
					App.logv("PATTERN_GAME_CATALOG_ESSENTIALS matched nothing");
				
				continue;
			}
			
			GameCatalogItem item = new GameCatalogItem();
			
			item.BoxartUrl = m.group(1);
			item.DetailUrl = m.group(2);
			item.Title = htmlDecode(m.group(3));
			item.Overview = htmlDecode(m.group(4));
			
			m = PATTERN_GAME_CATALOG_STATS.matcher(content);
			for (int row = 0; m.find(); row++)
			{
				if (row == 0)
				{
					/* AK: Gone, daddy gone...
					
					item.ReleaseDate = htmlDecode(m.group(2));
					item.ReleaseDateTicks = 0;
					
					String parseDate = null;
					if (myDetector.matcher(item.ReleaseDate).find())
						parseDate = item.ReleaseDate;
					
					if (parseDate != null)
					{
						try
						{
							item.ReleaseDateTicks = myParser.parse(parseDate).getTime();
						}
						catch(Exception e)
						{
						}
					}
					*/
				}
				else if (row == 1) // Platform
				{
				}
				else if (row == 2) // Genre
				{
					item.Genre = htmlDecode(m.group(2));
				}
				else if (row == 3) // Players
				{
				}
				else if (row == 4) // Online players
				{
				}
				else if (row == 5) // Publisher
				{
				}
			}
			
			catalog.Items.add(item);
		}
		
		if (noMatches && catalog.Items.size() < 1)
			catalog.MorePages = false;
		else
			catalog.MorePages = (catalog.PageNumber * catalog.PageSize) < records;
		
		if (App.LOGV)
			App.logv("pN: " + catalog.PageNumber + 
					" ;pS: " + catalog.PageSize + 
					" ;records: " + records + 
					" ;more? " + catalog.MorePages);
		
		if (App.LOGV)
			displayTimeTaken("parseGameCatalog/parsing", started);
		
		return catalog;
	}
	
	protected ComparedTrophyInfo parseCompareTrophies(PsnAccount account,
			String friendId, String gameId)
			throws ParserException, IOException
	{
		String url = String.format(URL_COMPARE_TROPHIES, 
				URLEncoder.encode(gameId, "UTF-8"), 
				URLEncoder.encode(account.getScreenName(), "UTF-8"),
				URLEncoder.encode(friendId, "UTF-8"));
		
		HttpUriRequest request = new HttpGet(url);
		request.addHeader("Referer", String.format(URL_COMPARE_TROPHIES_REFERER, 
				URLEncoder.encode(friendId, "UTF-8")));
		
		// Fetch the "main" page
		
		String page = getResponse(request);
		
		ComparedTrophyInfo cti = new ComparedTrophyInfo(mContext.getContentResolver());
		
		Map<Integer, Object> trophy;
		ArrayList<Map<Integer, Object>> trophies = new ArrayList<Map<Integer,Object>>();
		Map<String, Map<Integer, Object>> trophyMap = new HashMap<String, Map<Integer,Object>>();
		Matcher m;
		
		if (!(m = PATTERN_COMPARE_TROPHIES_TITLE_ID.matcher(page)).find())
		{
			if (App.LOGV)
				App.logv("No title ID for " + gameId);
			
			return cti;
		}
		
		String nullUnlockString = PSN.getShortTrophyUnlockString(mContext, 0);
		
		String titleId = m.group(1);
		
	    long started = System.currentTimeMillis();
	    
		Matcher matcher = PATTERN_COMPARE_TROPHIES.matcher(page);
		for (; matcher.find(); )
		{
			String trophyId = matcher.group(1);
			String group = matcher.group(3);
			
			trophy = new HashMap<Integer, Object>();
			
			if ((m = PATTERN_COMPARE_TROPHIES_TITLE.matcher(group)).find())
			{
				trophy.put(ComparedTrophyCursor.COLUMN_TITLE, htmlDecode(m.group(1).trim()));
				
				if ((m = PATTERN_TROPHY_DESCRIPTION.matcher(group)).find())
					trophy.put(ComparedTrophyCursor.COLUMN_DESCRIPTION, htmlDecode(m.group(1).trim()));
				else
					trophy.put(ComparedTrophyCursor.COLUMN_DESCRIPTION, null);
			}
			else
			{
				trophy.put(ComparedTrophyCursor.COLUMN_TITLE, 
						mContext.getString(R.string.secret_trophy));
				trophy.put(ComparedTrophyCursor.COLUMN_DESCRIPTION, 
						mContext.getString(R.string.this_is_a_secret_trophy));
			}
			
			if ((m = PATTERN_TROPHY_ICON.matcher(group)).find())
				trophy.put(ComparedTrophyCursor.COLUMN_ICON_URL,
						getAvatarImage(m.group(1)));
			else
				trophy.put(ComparedTrophyCursor.COLUMN_ICON_URL, null);
			
			if ((m = PATTERN_TROPHY_TYPE.matcher(group)).find())
			{
				String trophyType = m.group(1).toUpperCase();
				if (trophyType.equals("BRONZE"))
					trophy.put(ComparedTrophyCursor.COLUMN_TYPE, PSN.TROPHY_BRONZE);
				else if (trophyType.equals("SILVER"))
					trophy.put(ComparedTrophyCursor.COLUMN_TYPE, PSN.TROPHY_SILVER);
				else if (trophyType.equals("GOLD"))
					trophy.put(ComparedTrophyCursor.COLUMN_TYPE, PSN.TROPHY_GOLD);
				else if (trophyType.equals("PLATINUM"))
					trophy.put(ComparedTrophyCursor.COLUMN_TYPE, PSN.TROPHY_PLATINUM);
				else if (trophyType.equals("HIDDEN"))
					trophy.put(ComparedTrophyCursor.COLUMN_TYPE, PSN.TROPHY_SECRET);
			}
			
			trophy.put(ComparedTrophyCursor.COLUMN_IS_LOCKED, true);
			trophy.put(ComparedTrophyCursor.COLUMN_SELF_EARNED, nullUnlockString);
			trophy.put(ComparedTrophyCursor.COLUMN_OPP_EARNED, nullUnlockString);
			
			trophyMap.put(trophyId, trophy);
			trophies.add(trophy);
		}
		
		if (App.LOGV)
			started = displayTimeTaken("parseCompareTrophies/processing", started);
		
		JSONArray array;
		JSONObject json;
		
		// Run the compare requests
		url = String.format(URL_COMPARE_TROPHIES_DETAIL,
				URLEncoder.encode(titleId, "UTF-8"),
				URLEncoder.encode(account.getScreenName(), "UTF-8"));
		
		page = getResponse(url, true);
		json = getJSONObject(page);
		
		if ((array = json.optJSONArray("trophyList")) != null)
		{
			int n = array.length();
			for (int i = 0; i < n; i++)
			{
				if ((json = array.optJSONObject(i)) != null)
				{
					String trophyId = json.optString("id");
					if (trophyId != null && trophyMap.containsKey(trophyId))
					{
						long earned;
						String dateTime = json.optString("stampDate") + " "
								+ json.optString("stampTime");
						
						try
						{
							earned = COMPARE_TROPHY_DATE_FORMAT.parse(dateTime).getTime();
						}
						catch (ParseException e)
						{
							if (App.LOGV)
								e.printStackTrace();
							
							continue;
						}
						
						trophyMap.get(trophyId).put(ComparedTrophyCursor.COLUMN_IS_LOCKED, false);
						trophyMap.get(trophyId).put(ComparedTrophyCursor.COLUMN_SELF_EARNED, 
								PSN.getShortTrophyUnlockString(mContext, earned));
					}
				}
			}
		}
		
		url = String.format(URL_COMPARE_TROPHIES_DETAIL,
				URLEncoder.encode(titleId, "UTF-8"),
				URLEncoder.encode(friendId, "UTF-8"));
		
		page = getResponse(url, true);
		json = getJSONObject(page);
		
		if ((array = json.optJSONArray("trophyList")) != null)
		{
			int n = array.length();
			for (int i = 0; i < n; i++)
			{
				if ((json = array.optJSONObject(i)) != null)
				{
					String trophyId = json.optString("id");
					if (trophyId != null && trophyMap.containsKey(trophyId))
					{
						long earned;
						String dateTime = json.optString("stampDate") + " "
								+ json.optString("stampTime");
						
						try
						{
							earned = COMPARE_TROPHY_DATE_FORMAT.parse(dateTime).getTime();
						}
						catch (ParseException e)
						{
							if (App.LOGV)
								e.printStackTrace();
							
							continue;
						}
						
						trophyMap.get(trophyId).put(ComparedTrophyCursor.COLUMN_OPP_EARNED, 
								PSN.getShortTrophyUnlockString(mContext, earned));
					}
				}
			}
		}
		
		for (Map<Integer, Object> t: trophies)
		{
			cti.cursor.addItem((String)t.get(ComparedTrophyCursor.COLUMN_TITLE), 
					(String)t.get(ComparedTrophyCursor.COLUMN_DESCRIPTION), 
					(String)t.get(ComparedTrophyCursor.COLUMN_ICON_URL), 
					(Integer)t.get(ComparedTrophyCursor.COLUMN_TYPE), 
					(Integer)t.get(ComparedTrophyCursor.COLUMN_TYPE) == PSN.TROPHY_SECRET, 
					(Boolean)t.get(ComparedTrophyCursor.COLUMN_IS_LOCKED), 
					(String)t.get(ComparedTrophyCursor.COLUMN_SELF_EARNED), 
					(String)t.get(ComparedTrophyCursor.COLUMN_OPP_EARNED));
		}
		
		return cti;
	}
	
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
	                if (App.LOGV) 
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
		            if (App.LOGV) 
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
		            if (App.LOGV) 
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
		            if (App.LOGV) 
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
	                if (App.LOGV) 
	                	App.logv("Re-authenticating");
	        		
	                deleteSession(account);
	        		continue;
	        	}
	        	
	        	throw e;
	        }
			break;
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
		            if (App.LOGV) 
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
		            if (App.LOGV) 
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
