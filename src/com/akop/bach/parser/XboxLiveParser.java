/*
 * XboxLiveParser.java 
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

package com.akop.bach.parser;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.akop.bach.App;
import com.akop.bach.BasicAccount;
import com.akop.bach.Preferences;
import com.akop.bach.R;
import com.akop.bach.XboxLive;
import com.akop.bach.XboxLive.Achievements;
import com.akop.bach.XboxLive.BeaconInfo;
import com.akop.bach.XboxLive.Beacons;
import com.akop.bach.XboxLive.ComparedAchievementInfo;
import com.akop.bach.XboxLive.ComparedGameInfo;
import com.akop.bach.XboxLive.Friends;
import com.akop.bach.XboxLive.FriendsOfFriend;
import com.akop.bach.XboxLive.GameOverviewInfo;
import com.akop.bach.XboxLive.GamerProfileInfo;
import com.akop.bach.XboxLive.Games;
import com.akop.bach.XboxLive.LiveStatusInfo;
import com.akop.bach.XboxLive.Messages;
import com.akop.bach.XboxLive.NotifyStates;
import com.akop.bach.XboxLive.Profiles;
import com.akop.bach.XboxLive.RecentPlayers;
import com.akop.bach.XboxLive.SentMessages;
import com.akop.bach.XboxLiveAccount;

public class XboxLiveParser extends LiveParser
{
	private interface Parseable
	{
		void doParse() throws AuthenticationException, IOException, ParserException;
	}
	
	private interface ParseableWithResult
	{
		Object doParse() throws AuthenticationException, IOException, ParserException;
	}
	
	private static final String URL_SECRET_ACHIEVE_TILE = 
		"http://live.xbox.com/Content/Images/HiddenAchievement.png";
	private static final String URL_JSON_PROFILE = 
		"https://live.xbox.com/Handlers/ShellData.ashx?culture=%1$s" + 
		"&XBXMChg=%2$d&XBXNChg=%2$d&XBXSPChg=%2$d&XBXChg=%2$d" + 
		"&leetcallback=jsonp1287728723001";
	private static final String URL_JSON_READ_MESSAGE = 
		"https://live.xbox.com/%1$s/Messages/Message";
	private static final String URL_JSON_DELETE_MESSAGE = 
		"https://live.xbox.com/%1$s/Messages/Delete";
	private static final String URL_JSON_BLOCK_MESSAGE = 
		"https://live.xbox.com/%1$s/Messages/Block";
	private static final String URL_JSON_SEND_MESSAGE = 
		"https://live.xbox.com/%1$s/Messages/SendMessage";
	private static final String URL_JSON_FRIEND_REQUEST = 
		"https://live.xbox.com/%1$s/Friends/%2$s";
	private static final String URL_JSON_RECENT_LIST = 
		"https://live.xbox.com/%1$s/Friends/Recent";
	private static final String URL_JSON_FOF_LIST = 
		"https://live.xbox.com/%1$s/Friends/List";
	private static final String URL_JSON_MESSAGE_LIST = 
		"https://live.xbox.com/%1$s/Messages/GetMessages";
	private static final String URL_JSON_FRIEND_LIST = 
		"https://live.xbox.com/%1$s/Friends/List";
	private static final String URL_JSON_GAME_LIST = 
		"https://live.xbox.com/%1$s/Activity/Summary";
	private static final String URL_JSON_COMPARE_GAMES =
		"https://live.xbox.com/%1$s/Activity/Summary?CompareTo=%2$s";
	
	private static final String URL_JSON_BEACONS =
		"https://live.xbox.com/%1$s/Beacons/JumpInList";
	private static final String URL_JSON_BEACON_SET =
		"https://live.xbox.com/%1$s/Beacons/Set";
	private static final String URL_JSON_BEACON_CLEAR =
		"https://live.xbox.com/%1$s/Beacons/Clear";
	
	private static final String URL_VTOKEN_MESSAGES = 
		"https://live.xbox.com/%1$s/Messages?xr=socialtwistnav";
	private static final String URL_VTOKEN_ACTIVITY = 
		"https://live.xbox.com/%1$s/Activity?xr=socialtwistnav";
	private static final String URL_VTOKEN_FRIENDS = 
		"https://live.xbox.com/%1$s/Friends?xr=socialtwistnav";
	private static final String URL_VTOKEN_COMPARE_GAMES = 
		"https://live.xbox.com/%1$s/Activity?compareTo=%2$s";
	private static final String URL_VTOKEN_FRIEND_REQUEST = 
		"https://live.xbox.com/%1$s/Profile?gamertag=%2$s";
	
	private static final String URL_JSON_PROFILE_REFERER = 
		"https://live.xbox.com/en-US/MyXbox";
	
	private static final String URL_MY_PROFILE = 
		"https://live.xbox.com/%1$s/Profile";
	private static final String URL_STATUS = 
		"https://support.xbox.com/%1$s/xbox-live-status";
	private static final String URL_ACHIEVEMENTS = 
		"https://live.xbox.com/%1$s/Activity/Details?titleId=%2$s";
	private static final String URL_FRIEND_PROFILE = 
		"https://live.xbox.com/%1$s/Profile?gamertag=%2$s";
	private static final String URL_COMPARE_ACHIEVEMENTS = 
		"https://live.xbox.com/%1$s/Activity/Details?compareTo=%3$s&titleId=%2$s";
	private static final String URL_GAMERCARD = 
		"http://gamercard.xbox.com/%1$s/%2$s.card";
	private static final String URL_GAMERPIC = 
		"http://avatar.xboxlive.com/avatar/%s/avatarpic-l.png";
	private static final String URL_AVATAR_BODY = 
		"http://avatar.xboxlive.com/avatar/%s/avatar-body.png";
	private static final String URL_EDIT_PROFILE =
		"https://live.xbox.com/%1$s/MyXbox/GamerProfile";
	
	private static final String FRIEND_MANAGER_ADD = "Add";
	private static final String FRIEND_MANAGER_REMOVE = "Remove";
	private static final String FRIEND_MANAGER_ACCEPT = "Accept";
	private static final String FRIEND_MANAGER_REJECT = "Decline";
	private static final String FRIEND_MANAGER_CANCEL = "Cancel";
	
	private static final Pattern PATTERN_GAMERPIC_CLASSIC = Pattern
	        .compile("/(1)(\\d+)$");
	private static final Pattern PATTERN_GAMERPIC_AVATAR = Pattern.compile(
	        "/avatarpic-(s)(.png)$", Pattern.CASE_INSENSITIVE);
	
	private static final Pattern PATTERN_COMPARE_ACH_JSON = Pattern.compile(
			"broker\\.publish\\(routes\\.activity\\.details\\.load\\, (.*)\\);\\s*\\}\\);");
	
	private static final Pattern PATTERN_ACH_JSON = Pattern.compile(
			"broker\\.publish\\(routes\\.activity\\.details\\.load\\, (.*)\\);\\s*\\}\\);");
	
	private static final Pattern PATTERN_LOADBAL_ICON = Pattern
			.compile("^http://([0-9\\.]+/)");
	
	private static final Pattern PATTERN_GAMERCARD_REP = Pattern.compile(
			"class=\"Star ([^\"]*)\"");
	
	private static final Pattern PATTERN_SUMMARY_NAME = Pattern.compile(
			"<div class=\"name\" title=\"[^\"]*\">.*?<div class=\"value\">([^<]*)</div>",
				Pattern.DOTALL);
	private static final Pattern PATTERN_SUMMARY_LOCATION = Pattern.compile(
			"<div class=\"location\">.*?<div class=\"value\">([^<]*)</div>", 
				Pattern.DOTALL);
	private static final Pattern PATTERN_SUMMARY_BIO = Pattern.compile(
			"<div class=\"bio\">.*?<div class=\"value\" title=\"[^\"]*\">([^<]*)</div>",
				Pattern.DOTALL);
	private static final Pattern PATTERN_SUMMARY_POINTS = Pattern.compile(
			"<div class=\"gamerscore\">(\\d+)</div>");
	private static final Pattern PATTERN_SUMMARY_GAMERPIC = Pattern.compile(
			"<img class=\"gamerpic\" src=\"([^\"]+)\"");
	private static final Pattern PATTERN_SUMMARY_MOTTO = Pattern.compile(
			"<div class=\"motto\">([^<]*)<");
	private static final Pattern PATTERN_SUMMARY_ACTIVITY = Pattern.compile(
			"<div class=\"presence\">([^>]*)</div>");
	private static final Pattern PATTERN_SUMMARY_IS_FRIEND = Pattern.compile(
			"<a class=\"removeFriend button\"");
	
	private static final Pattern PATTERN_SUMMARY_REP = Pattern.compile(
			"<div class=\"reputation\">(.*?)<div class=\"clearfix\"", 
			Pattern.DOTALL);
	private static final Pattern PATTERN_SUMMARY_GAMERTAG = Pattern.compile(
			"<article class=\"profile you\" data-gamertag=\"([^\"]*)\">");
	
	private static final Pattern PATTERN_GAME_OVERVIEW_TITLE = 
		Pattern.compile("<h1>([^<]*)</h1>");
	private static final Pattern PATTERN_GAME_OVERVIEW_DESCRIPTION = 
		Pattern.compile("<div class=\"Text\">\\s*<p\\s*[^>]*>([^<]+)</p>\\s*</div>");
	private static final Pattern PATTERN_GAME_OVERVIEW_MANUAL = 
		Pattern.compile("<a class=\"Manual\" href=\"([^\"]+)\"");
	private static final Pattern PATTERN_GAME_OVERVIEW_ESRB = 
		Pattern.compile("<img alt=\"([^\"]*)\" class=\"ratingLogo\" src=\"([^\"]*)\"");
	private static final Pattern PATTERN_GAME_OVERVIEW_IMAGE = 
		Pattern.compile("<div id=\"image\\d+\" class=\"TabPage\">\\s*<img (?:width=\"[^\"]*\" )?src=\"([^\"]*)\"");
	private static final Pattern PATTERN_GAME_OVERVIEW_BANNER = 
		Pattern.compile("<img src=\"([^\"]*)\" alt=\"[^\"]*\" class=\"Banner\" />");
	
	private static final Pattern PATTERN_STATUS_LINE = 
		Pattern.compile("<div class=\"Status..\">\\s*(.*?\\s*</div>)\\s*</div>",
				Pattern.DOTALL);
	
	private static final Pattern PATTERN_STATUS_NAME = 
		Pattern.compile("<strong>([^<]*)</strong>");
	private static final Pattern PATTERN_STATUS_IS_OK = 
		Pattern.compile("class=\"StatusOKText\"");
	private static final Pattern PATTERN_STATUS_DESCRIPTION =
		Pattern.compile("<div class=\"StatusKOText\">(.*)?</div>", 
				Pattern.DOTALL);
	
	private static final Pattern PATTERN_GAME_OVERVIEW_REDIRECTING_URL = 
		Pattern.compile("/Title/\\d+$");
	
	private static final int COLUMN_GAME_ID = 0;
	private static final int COLUMN_GAME_LAST_PLAYED_DATE = 1;
	private static final int COLUMN_GAME_UID = 2;
	private static final int COLUMN_GAME_ACHIEVEMENTS_ACQUIRED = 3;
	private static final int COLUMN_GAME_ACHIEVEMENTS_TOTAL = 4;
	
	private static final String[] GAMES_PROJECTION = new String[] { 
		Games._ID,
		Games.LAST_PLAYED,
		Games.UID,
		Games.ACHIEVEMENTS_UNLOCKED,
		Games.ACHIEVEMENTS_TOTAL
	};
	
	private static final String[] STAR_CLASSES = {
		"empty",
		"quarter",
		"half",
		"threequarter",
		"full",
	};
	
	private static final int COLUMN_FRIEND_ID = 0;
	
	public static final int MAX_BEACONS = 3;
	
	private static final String BOXART_TEMPLATE =
		"http://tiles.xbox.com/consoleAssets/%1$X/%2$s/%3$s";
	
	private static final String[] FRIENDS_PROJECTION = new String[] {
		Friends._ID
	};
	
	private String mLocale;
	
	public XboxLiveParser(Context context)
	{
		super(context);
		
		String language = Locale.getDefault().getLanguage();
		String country = Locale.getDefault().getCountry();
		
		mLocale = context.getString(R.string.xbox_live_locale);
		
		if (App.getConfig().logToConsole())
			App.logv("XboxLiveParser: using " + mLocale + " locale (default: " +
					language + "-" + country + ")");
	}
	
	@Override
	protected void initRequest(HttpUriRequest request)
	{
		super.initRequest(request);
		
		// Set the timezone cookie
		TimeZone tz = TimeZone.getDefault();
		int utcOffsetMinutes = tz.getOffset(System.currentTimeMillis())/(1000*60);
		
		BasicClientCookie cookie = new BasicClientCookie("UtcOffsetMinutes",
				String.valueOf(utcOffsetMinutes));
		cookie.setPath("/");
		cookie.setDomain(".xbox.com");
		
		mHttpClient.getCookieStore().addCookie(cookie);
	}
	
	@Override
	protected String getReplyToPage()
	{
		return "https://live.xbox.com/xweb/live/passport/setCookies.ashx";
	}
	
	private boolean getXboxJsonStatus(String url, List<NameValuePair> inputs)
	        throws IOException, ParserException
	{
		String page = getResponse(url, inputs, true);
		JSONObject json = getJSONObject(page, false);
		
		if (!json.optBoolean("Success"))
			return false;
		
		return true;
	}
	
	private JSONObject getXboxJsonObject(String page) throws ParserException
	{
		JSONObject json = getJSONObject(page, false);
		
		if (!json.optBoolean("Success"))
			return null;
		
		return json.optJSONObject("Data");
	}
	
	private JSONArray getXboxJsonArray(String url, List<NameValuePair> inputs)
	        throws ParserException, IOException
	{
		String page = getResponse(url, inputs, true);
		JSONObject json = getJSONObject(page, false);
		
		if (!json.optBoolean("Success"))
			return null;
		
		return json.optJSONArray("Data");
	}
	
	public static String getLargeGamerpic(String iconUrl)
	{
		Matcher m;
		
		// Non-avatar (classic) gamerpic
		if ((m = PATTERN_GAMERPIC_CLASSIC.matcher(iconUrl)).find())
			return String.format("%s2%s", 
					iconUrl.substring(0, m.start(1)), m.group(2));
		// Avatar (NXE) gamerpic
		else if ((m = PATTERN_GAMERPIC_AVATAR.matcher(iconUrl)).find())
			return String.format("%sl%s", 
					iconUrl.substring(0, m.start(1)), m.group(2));
		
		if (App.getConfig().logToConsole())
			App.logv("%s has an unrecognized format; returning original",
					iconUrl);
		
		return iconUrl;
	}
	
	public static String getStandardIcon(String loadBalIcon)
	{
		if (loadBalIcon == null)
			return null;
		
		Matcher m;
		if (!(m = PATTERN_LOADBAL_ICON.matcher(loadBalIcon)).find())
			return loadBalIcon;
		
		String replacement = loadBalIcon.substring(0, m.start(1))
				+ loadBalIcon.substring(m.end(1));
		
		return replacement;
	}
	
	public static String getAvatarUrl(String gamertag)
	{
		if (gamertag == null)
			return null;
		
		try
		{
			return String.format(URL_AVATAR_BODY, 
					URLEncoder.encode(gamertag, "UTF-8")).replace("+", "%20");
		}
		catch (UnsupportedEncodingException e)
		{
			return null;
		}
	}
	
	public static String getGamerpicUrl(String gamertag)
	{
		if (gamertag == null)
			return null;
		
		try
		{
			return String.format(URL_GAMERPIC, 
					URLEncoder.encode(gamertag, "UTF-8")).replace("+", "%20");
		}
		catch (UnsupportedEncodingException e)
		{
			return null;
		}
	}
	
	public String getGamercardUrl(String gamertag)
	{
		if (gamertag == null)
			return null;
		
		try
		{
			return String.format(URL_GAMERCARD,
			        mLocale,
			        URLEncoder.encode(gamertag, "UTF-8")).replace("+", "%20");
		}
		catch (UnsupportedEncodingException e)
		{
			return null;
		}
	}
	
	private int getStarRating(String html)
	{
		final List<String> starClasses = Arrays.asList(STAR_CLASSES);
		int rating = 0;
		
		Matcher m = PATTERN_GAMERCARD_REP.matcher(html);
		while (m.find())
		{
			String starClass = m.group(1).trim().toLowerCase();
			int starValue = Math.max(starClasses.indexOf(starClass), 0);
			
			rating += starValue;
		}
		
		return rating;
	}
	
	private long parseTicks(String str)
	{
		if (str == null)
			return 0;
		
		int startPos, endPos;
		if ((startPos = str.indexOf("(")) < 0)
			return 0;
		
		if ((endPos = str.lastIndexOf(")")) < 0)
			return 0;
		
		long ticks;
		
		try
		{
			ticks = Long.parseLong(str.substring(startPos + 1, endPos));
		}
		catch (Exception e)
		{
			ticks = 0;
		}
		
		return ticks;
	}
	
	private String getVTokenFromContents(String page) throws IOException,
	        ParserException
	{
		List<NameValuePair> inputs = new ArrayList<NameValuePair>(10);
		getInputs(page, inputs, null);
		
		for (NameValuePair pair : inputs)
			if (pair.getName().equals("__RequestVerificationToken"))
				return pair.getValue();
		
		if (App.getConfig().logToConsole())
			App.logv("Token parsing failed");
		
		throw new TokenException(mContext);
	}
	
	private String getVToken(String url) throws IOException, ParserException
	{
		boolean retried = false;
		String token = null;
		
		do
		{
			String page;
			
			try
			{
				page = getResponse(url);
			}
			catch (ParserException e)
			{
				if (App.getConfig().logToConsole())
				{
					App.logv("Error fetching token from URL " + url);
					e.printStackTrace();
				}
				
				throw new TokenException(mContext);
			}
			
			try
			{
				token = getVTokenFromContents(page);
			}
			catch(TokenException ex)
			{
				if (retried)
					throw ex;
				
				if (App.getConfig().logToConsole())
					App.logv("Token parsing initially failed; retrying");
				
				retried = true;
				continue;
			}
		} while(false);
		
		return token;
	}
	
	private String getBoxArt(String titleId, boolean largeBoxart)
	{
		if (titleId == null)
			return null;
		
		long titleAsNumber;
		
		try
		{
			titleAsNumber = Long.parseLong(titleId);
		}
		catch(Exception e)
		{
			if (App.getConfig().logToConsole())
				App.logv("getBoxArt: " + titleId + " cannot be parsed as integer");
			
			return null;
		}
		
		return getBoxArt(titleAsNumber, largeBoxart);
	}
	
	private String getBoxArt(long titleId, boolean largeBoxart)
	{
		if (titleId < 1)
			return null;
		
		String jpgFile = (largeBoxart) ? "largeboxart.jpg" : "smallboxart.jpg";
		
		return String.format(BOXART_TEMPLATE, 
				titleId, mLocale, jpgFile);
	}
	
	private ContentValues[] parseGetBeacons(String gamertag, String token)
	        throws IOException, ParserException
	{
		long started = System.currentTimeMillis();
		
		ContentResolver cr = mContext.getContentResolver();
		String url = String.format(URL_JSON_BEACONS, 
				mLocale);
		
		List<NameValuePair> inputs = new ArrayList<NameValuePair>(3);
		addValue(inputs, "__RequestVerificationToken", token);
		
		if (gamertag != null)
			addValue(inputs, "gamertag", gamertag);
		
		JSONArray activities = getXboxJsonArray(url, inputs);
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Beacon page fetch", started);
		
		List<ContentValues> cvList = new ArrayList<ContentValues>();
		if (activities != null)
		{
			for (int i = 0, n = activities.length(); i < n; i++)
			{
				JSONObject activity = activities.optJSONObject(i);
				String titleId;
				JSONObject beacon;
				
				if (activity == null || 
						(titleId = activity.optString("titleId")) == null ||
						(beacon = activity.optJSONObject("beacon")) == null)
				{
					continue;
				}
				
				ContentValues cv = new ContentValues();
				cv.put(Beacons.TITLE_ID, titleId);
				cv.put(Beacons.TITLE_NAME, activity.optString("titleName"));
				cv.put(Beacons.TITLE_BOXART, getBoxArt(titleId, false));
				cv.put(Beacons.TEXT, beacon.isNull("text") 
						? null : beacon.optString("text"));
				
				cvList.add(cv);
			}
			
			cr.notifyChange(Games.CONTENT_URI, null);
		}
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Beacon data collection", started);
		
		ContentValues[] cvArray = new ContentValues[cvList.size()];
		cvList.toArray(cvArray);
		
		return cvArray;
	}
	
	private void parseSendMessage(XboxLiveAccount account, 
			String[] recipients,
			String body) throws IOException, ParserException
	{
		if (!account.isGold())
			throw new ParserException(mContext, R.string.account_cant_send);
		
		long started = System.currentTimeMillis();
		
		String token = getVToken(String.format(URL_VTOKEN_MESSAGES, 
				mLocale));
		
		String url = String.format(URL_JSON_SEND_MESSAGE, 
				mLocale);
		
		List<NameValuePair> inputs = new ArrayList<NameValuePair>(100);
		
		// Add recipients
		for (String recipient : recipients)
			addValue(inputs, "recipients", recipient);
		
		// Add message
		addValue(inputs, "message", body);
		
		// Req. ver. token
		addValue(inputs, "__RequestVerificationToken", token);
		
		String page = getResponse(url, inputs, true);
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Send message page fetch", started);
		
		JSONObject json = getJSONObject(page, false);
		
		try
		{
			if (!json.getBoolean("Success"))
			{
				if (App.getConfig().logToConsole())
					App.logv("XboxLiveParser/parseSendMessage: Parser error: " + page);
				
				throw new ParserException(json.optString("Status", 
						mContext.getString(R.string.message_could_not_be_sent)));
			}
			
			String preview = "";
			if (body != null)
			{
				preview = body.replaceAll("\\W", " ");
				if (preview.length() > 20)
					preview = preview.substring(0, 19);
			}
			
			ContentValues cv = new ContentValues(10);
			
			cv.put(SentMessages.ACCOUNT_ID, account.getId());
			cv.put(SentMessages.RECIPIENTS, joinString(recipients, ","));
			cv.put(SentMessages.SENT, System.currentTimeMillis());
			cv.put(SentMessages.PREVIEW, preview);
			cv.put(SentMessages.BODY, body);
			
			try
			{
				ContentResolver cr = mContext.getContentResolver();
				cr.insert(SentMessages.CONTENT_URI, cv);
			}
			catch(Exception e)
			{
				if (App.getConfig().logToConsole())
					e.printStackTrace();
			}
		}
		catch (JSONException e)
		{
			if (App.getConfig().logToConsole())
			{
				App.logv("XboxLiveParser/parseSendMessage: JSON error: " + page);
				e.printStackTrace();
			}
		}
		
		if (App.getConfig().logToConsole())
			displayTimeTaken("Message send processing", started);
	}
	
	private void parseFriendSummary(XboxLiveAccount account, String gamertag)
		throws ParserException, IOException
	{
		long started = System.currentTimeMillis();
		String url = String.format(URL_FRIEND_PROFILE, 
				mLocale, 
				URLEncoder.encode(gamertag, "UTF-8"));
		
		String page = getResponse(url);
		Matcher m;
		
		int gamerscore = 0;
		if ((m = PATTERN_SUMMARY_POINTS.matcher(page)).find())
			gamerscore = Integer.parseInt(m.group(1));
		
		String activity = null;
		if ((m = PATTERN_SUMMARY_ACTIVITY.matcher(page)).find())
			activity = htmlDecode(m.group(1)).trim();
		
		String bio = null;
		if ((m = PATTERN_SUMMARY_BIO.matcher(page)).find())
			bio = htmlDecodeWithCrLf(m.group(1));
		
		String name = null;
		if ((m = PATTERN_SUMMARY_NAME.matcher(page)).find())
			name = htmlDecode(m.group(1));
		
		String location = null;
		if ((m = PATTERN_SUMMARY_LOCATION.matcher(page)).find())
			location = htmlDecode(m.group(1));
		
		String motto = "";
		if ((m = PATTERN_SUMMARY_MOTTO.matcher(page)).find())
			motto = htmlDecode(m.group(1));
		
		String iconUrl = "";
		if ((m = PATTERN_SUMMARY_GAMERPIC.matcher(page)).find())
			iconUrl = getLargeGamerpic(m.group(1));
		else
			iconUrl = getGamerpicUrl(gamertag);
		
		int rep = 0;
		if ((m = PATTERN_SUMMARY_REP.matcher(page)).find())
			rep = getStarRating(m.group(1));
		
		ContentValues cv = new ContentValues(15);
		
		cv.put(Friends.CURRENT_ACTIVITY, activity);
		cv.put(Friends.GAMERSCORE, gamerscore);
		cv.put(Friends.BIO, bio);
		cv.put(Friends.NAME, name);
		cv.put(Friends.LOCATION, location);
		cv.put(Friends.MOTTO, motto);
		cv.put(Friends.REP, rep);
		cv.put(Friends.LAST_UPDATED, started);
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Summary processing", started);
		
		ContentResolver cr = mContext.getContentResolver();
		long accountId = account.getId();
		long friendId = -1;
		
		Cursor c = cr.query(Friends.CONTENT_URI, 
				new String[] { Friends._ID, Friends.ICON_URL },
				Friends.ACCOUNT_ID + "=" + accountId + " AND " +
				Friends.GAMERTAG + "=?", new String[] { gamertag }, 
				null);
		
		if (c != null)
		{
			Uri uri = null;
			
			try
			{
				if (c.moveToFirst())
				{
					// NOTE: Not writing icon for existing friends (not as accurate
					//       as what we can get from Friends list)
					// cv.put(Friends.ICON_URL, iconUrl);
					
					friendId = c.getLong(0);
					uri = ContentUris.withAppendedId(Friends.CONTENT_URI, friendId);
					
					cr.update(uri, cv, null, null);
				}
				else
				{
					cv.put(Friends.ACCOUNT_ID, account.getId());
					cv.put(Friends.GAMERTAG, gamertag);
			    	cv.put(Friends.ICON_URL, iconUrl);
					
					uri = cr.insert(Friends.CONTENT_URI, cv);
					friendId = Long.parseLong(uri.getLastPathSegment());
				}
			}
			finally
			{
				c.close();
			}
			
			if (uri != null)
				cr.notifyChange(uri, null);
			
			if (App.getConfig().logToConsole())
				started = displayTimeTaken("Summary update", started);
			
			cr.delete(Beacons.CONTENT_URI, 
					Beacons.ACCOUNT_ID + "=" + accountId + " AND " +
					Beacons.FRIEND_ID + "=" + friendId, null);
			
			try
			{
				String token = getVTokenFromContents(page);
				ContentValues[] cvList = parseGetBeacons(gamertag, token);
				
				for (ContentValues beaconCv: cvList)
				{
					beaconCv.put(Beacons.ACCOUNT_ID, accountId);
					beaconCv.put(Beacons.FRIEND_ID, friendId);
				}
				
				if (cvList.length > 0)
					cr.bulkInsert(Beacons.CONTENT_URI, cvList);
			}
			catch(Exception e)
			{
				// Ignore beacon errors
				if (App.getConfig().logToConsole())
					e.printStackTrace();
			}
			
			cr.notifyChange(Beacons.CONTENT_URI, null);
			
			if (App.getConfig().logToConsole())
				displayTimeTaken("Beacon update", started);
		}
	}
	
	private GamerProfileInfo parseGamerProfile(XboxLiveAccount account,
			String gamertag) throws ParserException, IOException
	{
	    long started = System.currentTimeMillis();
	    String url = String.format(URL_FRIEND_PROFILE, 
	    		mLocale, 
	    		URLEncoder.encode(gamertag, "UTF-8"));
	    
		String profilePage = getResponse(url);
		String cardPage;
		
		GamerProfileInfo info = new GamerProfileInfo();
		info.AccountId = account.getId();
		info.Gamertag = gamertag;
		
		Matcher m;
		if (!(m = PATTERN_SUMMARY_GAMERTAG.matcher(profilePage)).find())
			throw new ParserException(mContext,
					R.string.xbox_live_profile_not_found_f, gamertag);
	    
	    info.Gamertag = htmlDecode(m.group(1)).trim();
	    info.IsFriend = PATTERN_SUMMARY_IS_FRIEND.matcher(profilePage).find();
	    
	    if ((m = PATTERN_SUMMARY_POINTS.matcher(profilePage)).find())
	    	info.Gamerscore = Integer.parseInt(m.group(1));
	    if ((m = PATTERN_SUMMARY_ACTIVITY.matcher(profilePage)).find())
	    	info.CurrentActivity = htmlDecode(m.group(1)).trim();
	    if ((m = PATTERN_SUMMARY_BIO.matcher(profilePage)).find())
	    	info.Bio = htmlDecodeWithCrLf(m.group(1));
	    if ((m = PATTERN_SUMMARY_NAME.matcher(profilePage)).find())
	    	info.Name = htmlDecode(m.group(1));
	    if ((m = PATTERN_SUMMARY_LOCATION.matcher(profilePage)).find())
	    	info.Location = htmlDecode(m.group(1));
	    if ((m = PATTERN_SUMMARY_MOTTO.matcher(profilePage)).find())
	    	info.Motto = htmlDecode(m.group(1));
	    
		if ((m = PATTERN_SUMMARY_GAMERPIC.matcher(profilePage)).find())
			info.IconUrl = getLargeGamerpic(m.group(1));
		else
			info.IconUrl = getGamerpicUrl(gamertag);
		
		// Fetch rep
		url = getGamercardUrl(gamertag);
		
		try
		{
			cardPage = getResponse(url);
			info.Rep = getStarRating(cardPage);
		}
		catch(Exception e)
		{
			// Ignore errors - not vital
			if (App.getConfig().logToConsole())
				e.printStackTrace();
		}
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Summary fetch", started);
		
		try
		{
			String token = getVTokenFromContents(profilePage);
			
			ContentValues[] cvList = parseGetBeacons(gamertag, token);
			BeaconInfo[] beacons = new BeaconInfo[cvList.length];
			
			int i = 0;
			for (ContentValues cv: cvList)
				beacons[i++] = new BeaconInfo(cv);
			
			info.Beacons = beacons;
		}
		catch(Exception e)
		{
			// Ignore beacon errors
			if (App.getConfig().logToConsole())
				e.printStackTrace();
		}
		
		if (App.getConfig().logToConsole())
			displayTimeTaken("Beacon fetch", started);
		
		return info;
	}
	
	private ContentValues parseSummaryData(XboxLiveAccount account)
			throws ParserException, IOException
	{
		long started = System.currentTimeMillis();
		String url = String.format(URL_MY_PROFILE, 
				mLocale, System.currentTimeMillis());
		String page = getResponse(url);
		
		Matcher m;
		String bio = null;
		if ((m = PATTERN_SUMMARY_BIO.matcher(page)).find())
			bio = htmlDecodeWithCrLf(m.group(1));
		
		String name = null;
		if ((m = PATTERN_SUMMARY_NAME.matcher(page)).find())
			name = htmlDecode(m.group(1));
		
		String location = null;
		if ((m = PATTERN_SUMMARY_LOCATION.matcher(page)).find())
			location = htmlDecode(m.group(1));
		
		String motto = "";
		if ((m = PATTERN_SUMMARY_MOTTO.matcher(page)).find())
			motto = htmlDecode(m.group(1));
		
		int rep = 0;
		if ((m = PATTERN_SUMMARY_REP.matcher(page)).find())
			rep = getStarRating(m.group(1));
		
		url = String.format(URL_JSON_PROFILE, 
				mLocale, System.currentTimeMillis());
		
		HttpUriRequest request = new HttpGet(url);
		request.addHeader("Referer", URL_JSON_PROFILE_REFERER);
		request.addHeader("X-Requested-With", "XMLHttpRequest");
		
		page = getResponse(request, null);
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Profile page fetch", started);
		
		ContentValues cv = new ContentValues(15);
		
		String gamertag;
		JSONObject json = getJSONObject(page);
		
		try
		{
			gamertag = json.getString("gamertag");
		}
		catch(JSONException e)
		{
			throw new ParserException(mContext, 
					R.string.error_json_parser_error);
		}
		
		cv.put(Profiles.GAMERTAG, gamertag);
		cv.put(Profiles.ICON_URL, json.optString("gamerpic"));
		cv.put(Profiles.POINTS_BALANCE, json.optInt("pointsbalancetext"));
		cv.put(Profiles.IS_GOLD, json.optInt("tier") >= 6);
		cv.put(Profiles.TIER, json.optString("tiertext"));
		cv.put(Profiles.GAMERSCORE, json.optInt("gamerscore"));
		cv.put(Profiles.UNREAD_MESSAGES, json.optInt("messages"));
		cv.put(Profiles.UNREAD_NOTIFICATIONS, json.optInt("notifications"));
		cv.put(Profiles.NAME, name);
		cv.put(Profiles.LOCATION, location);
		cv.put(Profiles.MOTTO, motto);
		cv.put(Profiles.BIO, bio);
		cv.put(Profiles.REP, rep);
		
		return cv;
	}
	
	private void parseAccountSummary(XboxLiveAccount account) 
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
		account.setGamertag(cv.getAsString(Profiles.GAMERTAG));
		account.setIconUrl(cv.getAsString(Profiles.ICON_URL));
		account.setGoldStatus(cv.getAsBoolean(Profiles.IS_GOLD));
		account.setLastSummaryUpdate(System.currentTimeMillis());
		account.save(Preferences.get(mContext));
	}
	
	private void parseToggleBeacon(XboxLiveAccount account, long gameId,
			boolean setBeacon, String beaconText) throws ParserException, IOException
	{
		long started = System.currentTimeMillis();
		
		// Refuse setting more than MAX beacons
		if (setBeacon && Games.getSetBeaconCount(mContext, account) >= MAX_BEACONS)
			throw new ParserException(mContext, R.string.too_many_beacons_f, MAX_BEACONS);
		
		String titleId = Games.getUid(mContext, gameId);
		if (titleId == null)
			throw new ParserException(mContext, R.string.game_not_found_in_collection);
		
		String token = getVToken(String.format(URL_VTOKEN_ACTIVITY, 
				mLocale));
		String locale = mLocale;
		String url;
		
		if (setBeacon)
			url = String.format(URL_JSON_BEACON_SET, locale); 
		else
			url = String.format(URL_JSON_BEACON_CLEAR, locale);
		
		List<NameValuePair> inputs = new ArrayList<NameValuePair>(3);
		addValue(inputs, "__RequestVerificationToken", token);
		addValue(inputs, "titleId", titleId);
		
		if (setBeacon)
			addValue(inputs, "text", beaconText);
		
		try
		{
			if (!getXboxJsonStatus(url, inputs))
				throw new ParserException(mContext, R.string.could_not_modify_beacon);
		}
		catch(ParserException ex)
		{
			throw ex;
		}
		catch(Exception ex)
		{
			if (App.getConfig().logToConsole())
				ex.printStackTrace();
			
			throw new ParserException(mContext, R.string.could_not_modify_beacon);
		}
		
		parseRefreshBeaconData(account, token);
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Beacon toggle", started);
	}
	
	private void parseRefreshBeaconData(XboxLiveAccount account, String token) throws ParserException,
	        IOException
	{
		long started = System.currentTimeMillis();
		
		ContentResolver cr = mContext.getContentResolver();
		String url = String.format(URL_JSON_BEACONS, 
				mLocale);
		
		List<NameValuePair> inputs = new ArrayList<NameValuePair>(3);
		addValue(inputs, "__RequestVerificationToken", token);
		
		JSONArray activities = getXboxJsonArray(url, inputs);
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Beacon page fetch", started);
		
		if (activities != null)
		{
			// Unset all current beacons
			
			ContentValues cv = new ContentValues();
			cv.put(Games.BEACON_SET, 0);
			cv.put(Games.BEACON_TEXT, (String)null);
			
			cr.update(Games.CONTENT_URI, cv, 
					Games.ACCOUNT_ID + "=" + account.getId(), null);
			
			for (int i = 0, n = activities.length(); i < n; i++)
			{
				JSONObject activity = activities.optJSONObject(i);
				String titleId;
				JSONObject beacon;
				
				if (activity == null || 
						(titleId = activity.optString("titleId")) == null ||
						(beacon = activity.optJSONObject("beacon")) == null)
				{
					continue;
				}
				
				Long gameId = Games.getId(mContext, account, titleId);
				if (gameId == null)
					continue;
				
				cv = new ContentValues();
				cv.put(Games.BEACON_SET, 1);
				cv.put(Games.BEACON_TEXT, beacon.optString("text"));
				
				cr.update(Games.CONTENT_URI, cv, Games._ID + "=" + gameId, null);
			}
			
			cr.notifyChange(Games.CONTENT_URI, null);
		}
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Beacon updates", started);
	}
	
	private void parseGames(XboxLiveAccount account) throws ParserException, IOException
	{
		long started = System.currentTimeMillis();
		
		String token = getVToken(String.format(URL_VTOKEN_ACTIVITY, mLocale));
		String url = String.format(URL_JSON_BEACONS, mLocale);
		
		List<NameValuePair> inputs = new ArrayList<NameValuePair>(3);
		addValue(inputs, "__RequestVerificationToken", token);
		
		String page;
		JSONArray activities = null;
		
		try
		{
			activities = getXboxJsonArray(url, inputs);
		}
		catch(Exception ex)
		{
			if (App.getConfig().logToConsole())
				ex.printStackTrace();
			
			// Ignore the error - beacon errors can be ignored
		}
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Beacon page fetch", started);
		
		HashMap<String, JSONObject> beaconMap = new HashMap<String, JSONObject>();
		if (activities != null)
		{
			for (int i = 0, n = activities.length(); i < n; i++)
			{
				JSONObject activity = activities.optJSONObject(i);
				if (activity != null)
				{
					String titleId = activity.optString("titleId");
					if (titleId != null)
						beaconMap.put(titleId, activity.optJSONObject("beacon"));
				}
			}
		}
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Beacon page mapping", started);
		
		url = String.format(URL_JSON_GAME_LIST, mLocale);
		
		page = getResponse(url, inputs, true);
		JSONObject data = getXboxJsonObject(page);
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Game page fetch", started);
		
		if (data == null)
			throw new ParserException(mContext, R.string.error_games_retrieval);
		
		long accountId = account.getId();
		List<String> zeroGames = new ArrayList<String>(50);
		String[] queryParams = new String[1];
		int rowNo = 0;
		boolean changed = false;
		Cursor c;
		ContentValues cv;
		long updated = System.currentTimeMillis();
		List<ContentValues> newCvs = new ArrayList<ContentValues>(100);
		ContentResolver cr = mContext.getContentResolver();
		
		JSONArray games = data.optJSONArray("Games");
		for (int i = 0, n = games.length(); i < n; i++)
		{
			JSONObject game = games.optJSONObject(i);
			String uid;
			
			if (game == null || (uid = game.optString("Id")) == null)
				continue;
			
			JSONObject progRoot = game.optJSONObject("Progress");
			if (progRoot == null)
				continue;
			
			JSONObject progress = progRoot.optJSONObject(account.getGamertag());
			if (progress == null)
				continue;
			
			JSONObject beacon = null;
			if (beaconMap.containsKey(uid))
				beacon = beaconMap.get(uid);
			
			rowNo++;
			
			long lastPlayedTicks = parseTicks(progress.optString("LastPlayed"));
			int achUnlocked = progress.optInt("Achievements", 0);
			int achTotal = game.optInt("PossibleAchievements", 0);
			int gpAcquired = progress.optInt("Score", 0);
			int gpTotal = game.optInt("PossibleScore", 0);
			
			if (achTotal < 1)
				zeroGames.add(uid);
			
			// Check to see if we already have a record of this game
			queryParams[0] = uid;
			c = cr.query(Games.CONTENT_URI, GAMES_PROJECTION, Games.ACCOUNT_ID
					+ "=" + accountId + " AND " + Games.UID + "=?",
					queryParams, null);
			
			try
			{
		        if (c == null || !c.moveToFirst()) // New game
		        {
					String gameUrl = game.optString("Url");
					String title = game.optString("Name");
					String boxartUrl = game.optString("BoxArt");
					
					cv = new ContentValues(15);
	                cv.put(Games.ACCOUNT_ID, accountId);
	                cv.put(Games.TITLE, title);
	                cv.put(Games.UID, uid);
	                cv.put(Games.BOXART_URL, getStandardIcon(boxartUrl));
	                cv.put(Games.LAST_PLAYED, lastPlayedTicks);
	    			cv.put(Games.LAST_UPDATED, updated);
			        cv.put(Games.ACHIEVEMENTS_UNLOCKED, achUnlocked);
			        cv.put(Games.ACHIEVEMENTS_TOTAL, achTotal);
			        cv.put(Games.POINTS_ACQUIRED, gpAcquired);
			        cv.put(Games.POINTS_TOTAL, gpTotal);
			        cv.put(Games.GAME_URL, gameUrl);
			        cv.put(Games.INDEX, rowNo);
			        
			        if (beacon != null)
			        {
			        	cv.put(Games.BEACON_SET, 1);
			        	cv.put(Games.BEACON_TEXT, beacon.optString("text"));
			        }
			        
			        // Games with no achievements do not need achievement refresh
					cv.put(Games.ACHIEVEMENTS_STATUS, achTotal > 0 ? 1 : 0);
					
	                newCvs.add(cv);
		        }
		        else // Existing game
		        {
		        	long gameId = c.getLong(COLUMN_GAME_ID);
		        	long lastPlayedTicksRec = c.getLong(COLUMN_GAME_LAST_PLAYED_DATE);
		        	int achUnlockedRec = c.getInt(COLUMN_GAME_ACHIEVEMENTS_ACQUIRED);
		        	int achTotalRec = c.getInt(COLUMN_GAME_ACHIEVEMENTS_TOTAL);
		        	
		        	cv = new ContentValues(15);
		        	
					boolean refreshAchievements = (achUnlockedRec != achUnlocked 
							|| achTotalRec != achTotal);
	        		
		        	if (refreshAchievements)
		        	{
		        		cv.put(Games.ACHIEVEMENTS_UNLOCKED, achUnlocked);
				        cv.put(Games.ACHIEVEMENTS_TOTAL, achTotal);
				        cv.put(Games.POINTS_ACQUIRED, gpAcquired);
				        cv.put(Games.POINTS_TOTAL, gpTotal);
						cv.put(Games.ACHIEVEMENTS_STATUS, 1);
		        	}
		        	
			        if (beacon != null)
			        {
			        	cv.put(Games.BEACON_SET, 1);
			        	cv.put(Games.BEACON_TEXT, beacon.optString("text"));
			        }
			        else
			        {
			        	cv.put(Games.BEACON_SET, 0);
			        	cv.put(Games.BEACON_TEXT, (String)null);
			        }
			        
					if (lastPlayedTicks != lastPlayedTicksRec)
						cv.put(Games.LAST_PLAYED, lastPlayedTicks);
		        	
			        cv.put(Games.INDEX, rowNo);
					cv.put(Games.LAST_UPDATED, updated);
					cr.update(Games.CONTENT_URI, cv, Games._ID + "="
							+ gameId, null);
			        
	        		changed = true;
		        }
			}
			finally
			{
	        	if (c != null)
	        		c.close();
			}
		}
		
		// Remove games that are no longer present
		c = cr.query(Games.CONTENT_URI, GAMES_PROJECTION, Games.ACCOUNT_ID
				+ "=" + accountId + " AND " + Games.ACHIEVEMENTS_UNLOCKED + "=0",
				null, null);
		
		if (c != null)
		{
			while (c.moveToNext())
			{
				if (!zeroGames.contains(c.getString(COLUMN_GAME_UID)))
				{
					// Game is no longer in list of played games; remove it
					cr.delete(ContentUris.withAppendedId(Games.CONTENT_URI, 
							c.getLong(COLUMN_GAME_ID)), null, null);
					changed = true;
				}
			}
			
			c.close();
		}
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Game page processing", started);
		
		if (newCvs.size() > 0)
		{
			changed = true;
			
			ContentValues[] cvs = new ContentValues[newCvs.size()];
			newCvs.toArray(cvs);
			
			cr.bulkInsert(Games.CONTENT_URI, cvs);
			
			if (App.getConfig().logToConsole())
				displayTimeTaken("Game page insertion", started);
		}
		
		account.refresh(Preferences.get(mContext));
		account.setLastGameUpdate(System.currentTimeMillis());
		account.save(Preferences.get(mContext));
		
		if (changed)
			cr.notifyChange(Games.CONTENT_URI, null);
	}
	
	private void parseAchievements(XboxLiveAccount account, long gameId)
			throws ParserException, IOException
	{
		// Find game record in local DB
		ContentResolver cr = mContext.getContentResolver(); 
		String gameUid = Games.getUid(mContext, gameId);
		
		long updated = System.currentTimeMillis();
		long started = System.currentTimeMillis();
		
		String pageUrl = String.format(URL_ACHIEVEMENTS,
				mLocale, gameUid);
		
		String page = getResponse(pageUrl);
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Achievement page fetch", started);
		
        Matcher m;
        if (!(m = PATTERN_ACH_JSON.matcher(page)).find())
        	throw new ParserException(mContext, R.string.error_achieves_retrieval);
        
    	JSONObject data = getJSONObject(m.group(1), false);
		JSONArray players = data.optJSONArray("Players");
		
		if (players.length() < 1)
			throw new ParserException(mContext, R.string.error_achieves_retrieval);
		
		JSONObject player = players.optJSONObject(0);
		String gamertag = player.optString("Gamertag");
		
		List<ContentValues> cvList = new ArrayList<ContentValues>(100);
		
		JSONArray achieves = data.optJSONArray("Achievements");
		for (int i = 0, n = achieves.length(); i < n; i++)
		{
			JSONObject achieve = achieves.optJSONObject(i);
			if (achieve == null || achieve.optString("Id") == null)
				continue;
			
			JSONObject progRoot = achieve.optJSONObject("EarnDates");
			if (progRoot == null)
				continue;
			
			JSONObject prog = progRoot.optJSONObject(gamertag);
			
			String title;
			String description;
			String tileUrl;
			
			if (achieve.optBoolean("IsHidden"))
			{
				title = mContext.getString(R.string.secret_achieve_title);
				description = mContext.getString(R.string.secret_achieve_desc);
				tileUrl = URL_SECRET_ACHIEVE_TILE;
			}
			else
			{
				title = achieve.optString("Name");
				description = achieve.optString("Description");
				tileUrl = achieve.optString("TileUrl");
			}
			
			ContentValues cv = new ContentValues(10);
			
			// TODO cv.put(Achievements.UID, achieve.optString("Id"));
			cv.put(Achievements.GAME_ID, gameId);
			cv.put(Achievements.TITLE, title);
			cv.put(Achievements.DESCRIPTION, description);
			cv.put(Achievements.ICON_URL, tileUrl);
    		cv.put(Achievements.POINTS, achieve.optInt("Score", 0));
    		
			if (prog != null)
			{
				// Unlocked
				long earnedOn = 0;
				if (!prog.optBoolean("IsOffline"))
					earnedOn = parseTicks(prog.optString("EarnedOn"));
				
	    		cv.put(Achievements.ACQUIRED, earnedOn);
	            cv.put(Achievements.LOCKED, 0);
			}
			else
			{
				// Locked
	    		cv.put(Achievements.ACQUIRED, 0);
	            cv.put(Achievements.LOCKED, 1);
			}
			
			cvList.add(cv);
		}
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("New achievement parsing", started);
		
		ContentValues[] cva = new ContentValues[cvList.size()];
		cvList.toArray(cva);
		
		cr.delete(Achievements.CONTENT_URI, Achievements.GAME_ID + "=" + gameId, null);
		
		// Bulk-insert new achievements
		cr.bulkInsert(Achievements.CONTENT_URI, cva);
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("New achievement processing", started);
		
		// Update game stats
		JSONObject game = data.optJSONObject("Game");
		if (game != null)
		{
			ContentValues cv = new ContentValues(10);
			
			cv.put(Games.LAST_UPDATED, updated);
			cv.put(Games.ACHIEVEMENTS_STATUS, 0);
			cv.put(Games.ACHIEVEMENTS_TOTAL,
					game.optInt("PossibleAchievements", 0));
			cv.put(Games.POINTS_TOTAL,
					game.optInt("PossibleScore", 0));
			
			JSONObject progRoot = game.optJSONObject("Progress");
			if (progRoot != null)
			{
				JSONObject progress = game.optJSONObject(gamertag);
				if (progress != null)
				{
					cv.put(Games.ACHIEVEMENTS_UNLOCKED, 
							progress.optInt("Achievements", 0));
					cv.put(Games.POINTS_ACQUIRED, 
							progress.optInt("Score", 0));
					cv.put(Games.LAST_PLAYED, 
							parseTicks(progress.optString("LastPlayed")));
				}
			}
			
			// Write changes
	        cr.update(Games.CONTENT_URI, cv, Games._ID + "=" + gameId, null);
		}
		
		cr.notifyChange(Achievements.CONTENT_URI, null);
		
		if (App.getConfig().logToConsole())
			displayTimeTaken("Updating Game", started);
	}
	
	private void parseFriendSection(long accountId, JSONArray friends, long updated, 
			List<ContentValues> newCvs, int statusHack)
	{
		ContentResolver cr = mContext.getContentResolver();
		String[] queryParams = new String[1];
		
		for (int i = 0, n = friends.length(); i < n; i++)
		{
			JSONObject friend = friends.optJSONObject(i);
			if (friend == null)
				continue;
			
			String gamertag = friend.optString("GamerTag");
			String gamerpic = friend.optString("LargeGamerTileUrl");
			String activity = friend.optString("Presence");
			String titleName = null;
			
			int gamerscore = friend.optInt("GamerScore", 0);
			long titleId = 0;
			
			JSONObject titleInfo = friend.optJSONObject("TitleInfo");
			if (titleInfo != null)
			{
				titleId = titleInfo.optLong("Id", 0);
				titleName = null;
				
				if (!titleInfo.isNull("Name"))
					titleName = titleInfo.optString("Name");
			}
			
			int statusCode = XboxLive.STATUS_OTHER;
			if (statusHack < 0) // TODO: HACK!
			{
				if (friend.optBoolean("IsOnline"))
					statusCode = XboxLive.STATUS_ONLINE;
				else
					statusCode = XboxLive.STATUS_OFFLINE;
			}
			else
			{
				statusCode = statusHack;
			}
			
			String statusDescription = Friends.getStatusDescription(mContext,
					statusCode);
			
    		ContentValues cv = new ContentValues(15);
    		
			cv.put(Friends.DELETE_MARKER, updated);
			cv.put(Friends.GAMERSCORE, gamerscore);
			cv.put(Friends.CURRENT_ACTIVITY, activity);
			cv.put(Friends.ICON_URL, gamerpic);
			cv.put(Friends.STATUS_CODE, statusCode);
			cv.put(Friends.STATUS, statusDescription);
			cv.put(Friends.TITLE_ID, titleId);
			cv.put(Friends.TITLE_NAME, titleName);
			cv.put(Friends.TITLE_URL, getBoxArt(titleId, false));
			
			// check to see if friend is available locally
			queryParams[0] = gamertag;
			Cursor c = cr.query(Friends.CONTENT_URI, 
					FRIENDS_PROJECTION,
					Friends.ACCOUNT_ID + "=" + accountId + " AND " 
						+ Friends.GAMERTAG + "=?", queryParams, 
					null);
			
			try
			{
				if (c != null && c.moveToFirst())
				{
		    		// Friend in the system; update record
					long friendId = c.getLong(COLUMN_FRIEND_ID);
					
		            cr.update(Friends.CONTENT_URI, cv, 
		            		Friends._ID + "=" + friendId, null);
				}
				else
				{
		        	// New friend
					cv.put(Friends.GAMERTAG, gamertag);
					cv.put(Friends.ACCOUNT_ID, accountId);
					cv.put(Friends.IS_FAVORITE, 0);
					
					newCvs.add(cv);
				}
			}
			finally
			{
				if (c != null)
					c.close();
			}
		}
	}
	
	private void parseFriends(XboxLiveAccount account) throws IOException,
			ParserException
	{
		long started = System.currentTimeMillis();
		long updated = System.currentTimeMillis();
		long accountId = account.getId();
		
		synchronized (XboxLiveParser.class)
		{
			String token = getVToken(String.format(URL_VTOKEN_FRIENDS, 
					mLocale));
			
			String url = String.format(URL_JSON_FRIEND_LIST, 
					mLocale);
			
			List<NameValuePair> inputs = new ArrayList<NameValuePair>(3);
			addValue(inputs, "__RequestVerificationToken", token);
			
			String page = getResponse(url, inputs, true);
			JSONObject data = getXboxJsonObject(page);
			
			if (data == null)
				throw new ParserException(mContext, R.string.error_friend_retrieval);
			
			ContentResolver cr = mContext.getContentResolver();
			List<ContentValues> newCvs = new ArrayList<ContentValues>(100);
			
			parseFriendSection(accountId, 
					data.optJSONArray("Friends"), updated, newCvs, -1);
			parseFriendSection(accountId, 
					data.optJSONArray("Incoming"), updated, newCvs, XboxLive.STATUS_INVITE_RCVD);
			parseFriendSection(accountId, 
					data.optJSONArray("Outgoing"), updated, newCvs, XboxLive.STATUS_INVITE_SENT);
			
			// Remove friends that are missing from list
			cr.delete(Friends.CONTENT_URI, Friends.DELETE_MARKER + "!=" + updated
					+ " AND " + Friends.ACCOUNT_ID + "=" + accountId, null);
			
			if (newCvs.size() > 0)
			{
				ContentValues[] cvs = new ContentValues[newCvs.size()];
				newCvs.toArray(cvs);
				
				cr.bulkInsert(Friends.CONTENT_URI, cvs);
				
				if (App.getConfig().logToConsole())
					displayTimeTaken("Friend page insertion", started);
			}
			
			account.refresh(Preferences.get(mContext));
			account.setLastFriendUpdate(System.currentTimeMillis());
			account.save(Preferences.get(mContext));
			
			// A friend list is very likely to change at every sync, so
			// we just do it no matter what
			cr.notifyChange(Friends.CONTENT_URI, null);
		}
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Friend page processing", started);
	}
	
	private void parseUpdateProfile(XboxLiveAccount account, String motto,
			String name, String location, String bio) throws ParserException, IOException
	{
		long started = System.currentTimeMillis();
		
		String url = String.format(URL_EDIT_PROFILE, mLocale);
		String page = getResponse(url);
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Profile load", started);
		
		List<NameValuePair> inputs = new ArrayList<NameValuePair>(10);
		getInputs(page, inputs, null);
		
		setValue(inputs, "Motto", motto);
		setValue(inputs, "RealName", name);
		setValue(inputs, "Location", location);
		setValue(inputs, "Bio", bio);
		
		try
		{
			submitRequest(url, inputs);
			
			// This shouldn't happen - user was not redirected
			throw new ParserException(mContext, R.string.error_updating_profile);
		}
		catch(ClientProtocolException e)
		{
			// This is normal; user is redirected to his overview page
		}
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Profile update", started);
		
		// Update local information
		
		ContentResolver cr = mContext.getContentResolver();
		ContentValues cv = new ContentValues(10);
		
		cv.put(Profiles.MOTTO, motto);
		cv.put(Profiles.NAME, name);
		cv.put(Profiles.LOCATION, location);
		cv.put(Profiles.BIO, bio);
		
		Uri uri = ContentUris.withAppendedId(Profiles.CONTENT_URI, 
				account.getId());
		
		cr.update(uri, cv, null, null);
		
		account.setLastSummaryUpdate(System.currentTimeMillis());
		account.save(Preferences.get(mContext));
		
		cr.notifyChange(uri, null);
	}
	
	private FriendsOfFriend parseFriendsOfFriend(XboxLiveAccount account,
			String friendGamertag) throws IOException, ParserException
	{
		long started = System.currentTimeMillis();
		FriendsOfFriend list = new FriendsOfFriend(mContext.getContentResolver());
		
		String token = getVToken(String.format(URL_VTOKEN_FRIENDS, 
				mLocale));
		
		String url = String.format(URL_JSON_FOF_LIST, 
				mLocale);
		
		List<NameValuePair> inputs = new ArrayList<NameValuePair>(3);
		
		// Req. ver. token
		addValue(inputs, "__RequestVerificationToken", token);
		addValue(inputs, "gamertag", friendGamertag);
		
		String page = getResponse(url, inputs, true);
		JSONObject json = getXboxJsonObject(page);
		if (json == null)
			return list;
		
		JSONArray friends = json.optJSONArray("Friends");
		if (friends == null)
			return list;
		
		ArrayList<Gamer> shared = new ArrayList<Gamer>();
		ArrayList<Gamer> notYet = new ArrayList<Gamer>();
		
		for (int i = 0, n = friends.length(); i < n; i++)
		{
			String gamertag;
			JSONObject friend = friends.optJSONObject(i);
			
			if (friend == null || (gamertag = friend.optString("GamerTag")) == null)
				continue;
			
			long lastSeen = parseTicks(friend.optString("LastSeen"));
			
			Gamer gamer = new Gamer();
			
			gamer.Gamertag = gamertag;
			gamer.Gamerscore = friend.optInt("GamerScore", 0);
			gamer.IconUrl = friend.optString("LargeGamerTileUrl");
			
			JSONObject titleInfo = friend.optJSONObject("TitleInfo");
			if (titleInfo != null)
			{
				gamer.TitleIconUrl = getBoxArt(titleInfo.optInt("Id", 0), false);
				
				if (titleInfo.optInt("Id") == 0)
				{
					gamer.CurrentActivity = mContext.getString(R.string.online_status_unavailable);
				}
				else
				{
					gamer.CurrentActivity = mContext.getString(R.string.last_seen_playing_f, 
							DateFormat.getDateInstance().format(lastSeen),
							titleInfo.optString("Name"));
				}
			}
			
			gamer.IsFriend = Friends.isFriend(mContext, account, gamertag);
			
			if (gamer.IsFriend)
				shared.add(gamer);
			else
				notYet.add(gamer);
		}
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Friends of friend page loading", started);
		
		Gamer.Comparator cmp = new Gamer.Comparator();
		
		Collections.sort(shared, cmp);
		Collections.sort(notYet, cmp);
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Friends of friend page sorting", started);
		
		long id = 0;
		
		for (Gamer gamer : shared)
		{
			list.SharedFriends.addItem(id++, gamer.Gamertag, 
					gamer.CurrentActivity, gamer.IconUrl, gamer.TitleIconUrl, 
					gamer.TitleId, gamer.Gamerscore, gamer.IsFriend);
		}
		
		for (Gamer gamer : notYet)
		{
			list.NotYetFriends.addItem(id++, gamer.Gamertag, 
					gamer.CurrentActivity, gamer.IconUrl, gamer.TitleIconUrl, 
					gamer.TitleId, gamer.Gamerscore, gamer.IsFriend);
		}
		
		return list;
	}
	
	private RecentPlayers parseRecentPlayers(XboxLiveAccount account)
			throws IOException, ParserException
	{
		long started = System.currentTimeMillis();
		RecentPlayers list = new RecentPlayers(mContext.getContentResolver());
		
		String token = getVToken(String.format(URL_VTOKEN_FRIENDS, 
				mLocale));
		
		String url = String.format(URL_JSON_RECENT_LIST, 
				mLocale);
		
		List<NameValuePair> inputs = new ArrayList<NameValuePair>(3);
		
		// Req. ver. token
		addValue(inputs, "__RequestVerificationToken", token);
		
		JSONArray players = getXboxJsonArray(url, inputs);
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Player list page fetch", started);
		
		if (players == null)
			throw new ParserException(mContext, R.string.error_player_retrieval);
		
		ArrayList<Gamer> notYet = new ArrayList<Gamer>();
		
		for (int i = 0, n = players.length(); i < n; i++)
		{
			String gamertag;
			JSONObject player = players.optJSONObject(i);
			
			if (player == null || (gamertag = player.optString("GamerTag")) == null)
				continue;
			
			long lastSeen = parseTicks(player.optString("LastSeen"));
			
			Gamer gamer = new Gamer();
			
			gamer.Gamertag = gamertag;
			gamer.Gamerscore = player.optInt("GamerScore", 0);
			gamer.IconUrl = player.optString("LargeGamerTileUrl");
			
			JSONObject titleInfo = player.optJSONObject("TitleInfo");
			if (titleInfo != null)
			{
				gamer.TitleIconUrl = getBoxArt(titleInfo.optInt("Id", 0), false);
				
				if (titleInfo.optInt("Id") == 0)
				{
					gamer.CurrentActivity = mContext.getString(R.string.online_status_unavailable);
				}
				else
				{
					gamer.CurrentActivity = mContext.getString(R.string.last_seen_playing_f, 
							DateFormat.getDateInstance().format(lastSeen),
							titleInfo.optString("Name"));
				}
			}
			
			notYet.add(gamer);
		}
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Recent players page loading", started);
		
		Gamer.Comparator cmp = new Gamer.Comparator();
		
		Collections.sort(notYet, cmp);
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Recent players page sorting", started);
		
		for (Gamer gamer : notYet)
		{
			list.Players.addItem(gamer.Gamertag, gamer.CurrentActivity, 
					gamer.IconUrl, gamer.TitleIconUrl, gamer.TitleId, 
					gamer.Gamerscore, gamer.IsFriend);
		}
		
		return list;
	}
	
	private void parseMessages(XboxLiveAccount account) 
		throws IOException, ParserException 
	{
		long started = System.currentTimeMillis();
		
		String token = getVToken(String.format(URL_VTOKEN_MESSAGES, 
				mLocale));
		
		String url = String.format(URL_JSON_MESSAGE_LIST, 
				mLocale);
		
		List<NameValuePair> inputs = new ArrayList<NameValuePair>(3);
		addValue(inputs, "__RequestVerificationToken", token);
		
		String page = getResponse(url, inputs, true);
		JSONObject data = getXboxJsonObject(page);
		
		if (data == null)
			throw new ParserException(mContext, R.string.error_message_retrieval);
		
		JSONArray messages = data.optJSONArray("Messages");
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Message page fetch", started);
		
		long updated = started;
		boolean changed = false;
		String uid;
		ContentResolver cr = mContext.getContentResolver();
		Cursor c;
		ContentValues cv;
		List<ContentValues> newCvs = new ArrayList<ContentValues>();
		final String[] columns = new String[] { Messages._ID, Messages.IS_READ };
		
		for (int i = 0, n = messages.length(); i < n; i++)
		{
			JSONObject message = messages.optJSONObject(i);
			
			if (message == null || (uid = message.optString("Id")) == null)
				continue;
			
			int isRead = message.optBoolean("HasBeenRead") ? 1 : 0;
			
			c = cr.query(Messages.CONTENT_URI, columns,
					Messages.ACCOUNT_ID + "=" + account.getId() + " AND "
					+ Messages.UID + "=" + uid, null, null);
			
			try
			{
				if (c != null && c.moveToFirst())
				{
					String gamerpic = message.optString("GamerPic");
					
					// Message already in system
					cv = new ContentValues(5);
					cv.put(Messages.IS_READ, isRead);
					cv.put(Messages.DELETE_MARKER, updated);
					cv.put(Messages.GAMERPIC, gamerpic);
					
					changed = true;
					cr.update(Messages.CONTENT_URI, cv, 
							Messages._ID + "=" + c.getLong(0), null);
				}
				else
				{
					long sent = parseTicks(message.optString("SentTime"));
					
					String body = message.optString("Excerpt", "");
					String sender = message.optString("From", "");
					String gamerpic = message.optString("GamerPic");
					
					int type = XboxLive.MESSAGE_TEXT;
					if (message.optBoolean("HasImage"))
						type = XboxLive.MESSAGE_OTHER;
					if (message.optBoolean("HasVoice"))
						type = XboxLive.MESSAGE_VOICE;
					
					// New message
					cv = new ContentValues(10);
					cv.put(Messages.ACCOUNT_ID, account.getId());
					cv.put(Messages.SENDER, sender);
					cv.put(Messages.GAMERPIC, gamerpic);
					cv.put(Messages.UID, uid);
					cv.put(Messages.IS_READ, isRead);
					cv.put(Messages.IS_DIRTY, 1);
					cv.put(Messages.TYPE, type);
					cv.put(Messages.SENT, sent);
					cv.put(Messages.DELETE_MARKER, updated);
					cv.put(Messages.BODY, htmlDecode(body));
					
					newCvs.add(cv);
				}
			}
			finally
			{
				if (c != null)
					c.close();
			}
		}
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Message list processing", started);
		
		if (newCvs.size() > 0)
		{
			changed = true;
			
			ContentValues[] cvs = new ContentValues[newCvs.size()];
			newCvs.toArray(cvs);
			
			cr.bulkInsert(Messages.CONTENT_URI, cvs);
			
			if (App.getConfig().logToConsole())
				displayTimeTaken("Message list insertion", started);
		}
		
		int deleted = cr.delete(Messages.CONTENT_URI, Messages.DELETE_MARKER
				+ "!=" + updated + " AND " + Messages.ACCOUNT_ID + "="
				+ account.getId(), null);
		
		account.refresh(Preferences.get(mContext));
		account.setLastMessageUpdate(System.currentTimeMillis());
		account.save(Preferences.get(mContext));
		
		if (changed || deleted > 0)
			cr.notifyChange(Messages.CONTENT_URI, null);
	}
	
	private void parseDeleteMessage(XboxLiveAccount account, long messageUid)
			throws IOException, ParserException
	{
		String token = getVToken(String.format(URL_VTOKEN_MESSAGES, 
				mLocale));
		
		String url = String.format(URL_JSON_DELETE_MESSAGE, 
				mLocale);
		
		List<NameValuePair> inputs = new ArrayList<NameValuePair>(3);
		
		// Message ID
		addValue(inputs, "msgID", messageUid);
		
		// Req. ver. token
		addValue(inputs, "__RequestVerificationToken", token);
		
		String page = getResponse(url, inputs, true);
		
		long started = System.currentTimeMillis();
		
		JSONObject json = getJSONObject(page, false);
		
		try
		{
			if (!json.getBoolean("Success"))
			{
				if (App.getConfig().logToConsole())
					App.logv("XboxLiveParser/parseDeleteMessage: Parser error: " + page);
				
				throw new ParserException(json.optString("Status", 
						mContext.getString(R.string.message_could_not_be_deleted)));
			}
		}
		catch (JSONException e)
		{
			if (App.getConfig().logToConsole())
			{
				App.logv("XboxLiveParser/parseDeleteMessage: JSON error: " + page);
				e.printStackTrace();
			}
		}
		
		ContentResolver cr = mContext.getContentResolver();
		int rows = cr.delete(Messages.CONTENT_URI, 
				Messages.UID + "=" + messageUid + " AND " + Messages.ACCOUNT_ID + "=" + account.getId(), 
				null);
		
		if (rows > 0)
			cr.notifyChange(Messages.CONTENT_URI, null);
		
		if (App.getConfig().logToConsole())
			displayTimeTaken("Message deletion processing", started);
	}
	
	private void parseBlockMessage(XboxLiveAccount account, long messageUid)
			throws IOException, ParserException
	{
		String token = getVToken(String.format(URL_VTOKEN_MESSAGES, 
				mLocale));
		
		String url = String.format(URL_JSON_BLOCK_MESSAGE, 
				mLocale);
		
		List<NameValuePair> inputs = new ArrayList<NameValuePair>(3);
		
		// Message ID
		addValue(inputs, "msgID", messageUid);
		
		// Req. ver. token
		addValue(inputs, "__RequestVerificationToken", getVToken(token));
		
		String page = getResponse(url, inputs, true);
		
		long started = System.currentTimeMillis();
		
		JSONObject json = getJSONObject(page, false);
		
		try
		{
			if (!json.getBoolean("Success"))
			{
				if (App.getConfig().logToConsole())
					App.logv("XboxLiveParser/parseBlockMessage: Parser error: " + page);
				
				throw new ParserException(json.optString("Status", 
						mContext.getString(R.string.sender_not_blocked)));
			}
		}
		catch (JSONException e)
		{
			if (App.getConfig().logToConsole())
			{
				App.logv("XboxLiveParser/parseDeleteMessage: JSON error: " + page);
				e.printStackTrace();
			}
		}
		
		ContentResolver cr = mContext.getContentResolver();
		int rows = cr.delete(Messages.CONTENT_URI, 
				Messages.UID + "=" + messageUid + " AND " + Messages.ACCOUNT_ID + "=" + account.getId(), 
				null);
		
		if (rows > 0)
			cr.notifyChange(Messages.CONTENT_URI, null);
		
		if (App.getConfig().logToConsole())
			displayTimeTaken("Message deletion processing", started);
	}
	
	private void parseViewMessage(XboxLiveAccount account, long messageUid) 
		throws IOException, ParserException 
	{
		long started = System.currentTimeMillis();
		
		String token = getVToken(String.format(URL_VTOKEN_MESSAGES, 
				mLocale));
		
		String url = String.format(URL_JSON_READ_MESSAGE, 
				mLocale);
		
		List<NameValuePair> inputs = new ArrayList<NameValuePair>(3);
		
		addValue(inputs, "msgID", messageUid);
		addValue(inputs, "__RequestVerificationToken", token);
		
		String page = getResponse(url, inputs, true);
		
		if (App.getConfig().logToConsole())
			displayTimeTaken("Message page fetch", started);	
		
		JSONObject json = getXboxJsonObject(page);
		String message;
		
		if ((json == null) || (message = json.optString("Text")) == null)
			throw new ParserException(mContext, R.string.error_json_parser_error);
		
		ContentResolver cr = mContext.getContentResolver();
		ContentValues cv = new ContentValues();
		
		if (!json.isNull("Text"))
			cv.put(Messages.BODY, htmlDecode(message));
		
		cv.put(Messages.IS_DIRTY, 0);
		cv.put(Messages.IS_READ, 1);
		
		int rows = cr.update(Messages.CONTENT_URI, cv, 
				Messages.UID + "=" + messageUid + " AND " + 
				Messages.ACCOUNT_ID + "=" + account.getId(), 
				null);
		
		if (rows < 1)
			throw new ParserException(mContext, R.string.message_not_found);
		
		cr.notifyChange(Messages.CONTENT_URI, null);
		
		if (App.getConfig().logToConsole())
			displayTimeTaken("Message processing", started);
	}
	
	private ComparedAchievementInfo parseCompareAchievements(XboxLiveAccount account,
			String gamertag, String gameUid) throws IOException,
			ParserException
	{
		long started = System.currentTimeMillis();
		
		String pageUrl = String.format(URL_COMPARE_ACHIEVEMENTS, 
				mLocale, 
				gameUid, 
				URLEncoder.encode(gamertag, "UTF-8"));
		String page = getResponse(pageUrl);
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Achievement compare page fetch", started);
		
        Matcher m;
        if (!(m = PATTERN_COMPARE_ACH_JSON.matcher(page)).find())
        	throw new ParserException(mContext, R.string.error_achieves_retrieval);
        
    	JSONObject data = getJSONObject(m.group(1), false);
		JSONArray players = data.optJSONArray("Players");
		
		if (players.length() < 2)
			throw new ParserException(mContext, R.string.error_achieves_retrieval);
		
		JSONObject you = players.optJSONObject(0);
		JSONObject me = players.optJSONObject(1);
		
		String yourGamertag = you.optString("Gamertag");
		String myGamertag = me.optString("Gamertag");
		
    	ComparedAchievementInfo comparedAchieves = new ComparedAchievementInfo(mContext.getContentResolver());
    	
    	comparedAchieves.yourAvatarIconUrl = you.optString("Gamerpic");
    	comparedAchieves.myAvatarIconUrl = me.optString("Gamerpic");
		
		JSONArray achieves = data.optJSONArray("Achievements");
		for (int i = 0, n = achieves.length(); i < n; i++)
		{
			JSONObject achieve = achieves.optJSONObject(i);
			
			if (achieve == null || (achieve.optString("Id")) == null)
				continue;
			
			JSONObject progRoot = achieve.optJSONObject("EarnDates");
			if (progRoot == null)
				continue;
			
			JSONObject myProg = progRoot.optJSONObject(myGamertag);
			JSONObject yourProg = progRoot.optJSONObject(yourGamertag);
			
			//HashMap<String, Object> achieveMap = new HashMap<String, Object>();
			
			int score = achieve.optInt("Score");
			String uid = achieve.optString("Id");
			String title;
			String description;
			String iconUrl;
			
			if (achieve.optBoolean("IsHidden"))
			{
				title = mContext.getString(R.string.secret_achieve_title);
				description = mContext.getString(R.string.secret_achieve_desc);
				iconUrl = URL_SECRET_ACHIEVE_TILE;
			}
			else
			{
				title = achieve.optString("Name");
				description = achieve.optString("Description");
				iconUrl = achieve.optString("TileUrl");
			}
			
			int scoreEarned;
			long myAcquired = 0;
			int myIsLocked = 1;
			
			// My section
			
			scoreEarned = 0;
			
			if (myProg != null)
			{
				myIsLocked = 0;
				scoreEarned = score;
				if (!myProg.optBoolean("IsOffline"))
					myAcquired = parseTicks(myProg.optString("EarnedOn"));
			}
			
			comparedAchieves.myGamerscore += scoreEarned;
			
			// Your section
			
			scoreEarned = 0;
			long yourAcquired = 0;
			int yourIsLocked = 1;
			
			if (yourProg != null)
			{
				yourIsLocked = 0;
				scoreEarned = score;
				if (!yourProg.optBoolean("IsOffline"))
					yourAcquired = parseTicks(yourProg.optString("EarnedOn"));
			}
			
			comparedAchieves.yourGamerscore += scoreEarned;
			comparedAchieves.cursor.addItem(uid, title, description, score, 
					myAcquired, myIsLocked, yourAcquired, yourIsLocked, 
					iconUrl);
		}
		
		return comparedAchieves;
	}
	
	private ComparedGameInfo parseCompareGames(XboxLiveAccount account, String gamertag)
			throws IOException, ParserException
	{
		long started = System.currentTimeMillis();
		
		String token = getVToken(String.format(URL_VTOKEN_COMPARE_GAMES, 
				mLocale, URLEncoder.encode(gamertag,
				"UTF-8")));
		
		String url = String.format(URL_JSON_COMPARE_GAMES, 
				mLocale, URLEncoder.encode(gamertag,
				"UTF-8"));
		
		List<NameValuePair> inputs = new ArrayList<NameValuePair>(3);
		addValue(inputs, "__RequestVerificationToken", token);
		
		String page = getResponse(url, inputs, true);
		JSONObject data = getXboxJsonObject(page);
		
		if (data == null)
			throw new ParserException(mContext, R.string.error_games_retrieval);
		
		if (App.getConfig().logToConsole())
			started = displayTimeTaken("Game compare page fetch", started);
		
		ComparedGameInfo comparedGames = new ComparedGameInfo(mContext.getContentResolver());
		JSONArray players = data.optJSONArray("Players");
		
		if (players.length() < 2)
			throw new ParserException(mContext, R.string.error_games_retrieval);
		
		JSONObject you = players.optJSONObject(0);
		JSONObject me = players.optJSONObject(1);
		
		String yourGamertag = you.optString("Gamertag");
		String myGamertag = me.optString("Gamertag");
		
		comparedGames.yourAvatarIconUrl = you.optString("Gamerpic");
		comparedGames.myAvatarIconUrl = me.optString("Gamerpic");
		comparedGames.yourGamerscore = you.optInt("Gamerscore");
		comparedGames.myGamerscore = me.optInt("Gamerscore");
		
		JSONArray games = data.optJSONArray("Games");
		for (int i = 0, n = games.length(); i < n; i++)
		{
			String uid;
			JSONObject game = games.optJSONObject(i);
			
			if (game == null || (uid = game.optString("Id")) == null)
				continue;
			
			JSONObject progRoot = game.optJSONObject("Progress");
			if (progRoot == null)
				continue;
			
			JSONObject myProg = progRoot.optJSONObject(myGamertag);
			JSONObject yourProg = progRoot.optJSONObject(yourGamertag);
			
			if (myProg == null || yourProg == null)
				continue;
			
			int totalAch = game.optInt("PossibleAchievements", 0);
			if (account.isShowingApps() || totalAch > 0)
			{
				int totalScore = game.optInt("PossibleScore", 0);
				
				int myAch = myProg.optInt("Achievements", 0);
				int myScore = myProg.optInt("Score", 0);
				
				int yourAch = yourProg.optInt("Achievements", 0);
				int yourScore = yourProg.optInt("Score", 0);
				
				comparedGames.cursor.addItem(game.optString("Name"), 
						uid, 
						myAch, yourAch, totalAch,
						myScore, yourScore, totalScore,
						game.optString("BoxArt"), 
						game.optString("Url"));
			}
		}
		
		return comparedGames;
	}
	
	private void parseFriendRequest(XboxLiveAccount account, String requestId,
	        String gamertag) throws IOException, ParserException
	{
		long started = System.currentTimeMillis();
		
		String token = getVToken(String.format(URL_VTOKEN_FRIEND_REQUEST, 
				mLocale, URLEncoder.encode(gamertag,
				"UTF-8")));
		
		List<NameValuePair> inputs = new ArrayList<NameValuePair>(3);
		addValue(inputs, "gamertag", gamertag);
		addValue(inputs, "__RequestVerificationToken", token);
		
		String url = String.format(URL_JSON_FRIEND_REQUEST, 
				mLocale, requestId);
		
		String page = getResponse(url, inputs, true);
		
		if (App.getConfig().logToConsole())
			displayTimeTaken("Friend manager page fetch", started);	
		
		JSONObject json = getJSONObject(page, false);
		
		try
		{
			if (!json.getBoolean("Success"))
			{
				if (App.getConfig().logToConsole())
					App.logv("XboxLiveParser/parseSendMessage: Parser error: " + page);
				
				throw new ParserException(mContext.getString(R.string.request_unsuccessful));
			}
		}
		catch (JSONException e)
		{
			if (App.getConfig().logToConsole())
				e.printStackTrace();
			
			throw new ParserException(mContext, R.string.error_json_parser_error);
		}
    	
		// Request friend list update
		
		parseFriends(account);
		saveSession(account);
	}
	
	private GameOverviewInfo parseGameOverview(String url) throws IOException,
			ParserException
	{
		String loadUrl = url;
		
		if (PATTERN_GAME_OVERVIEW_REDIRECTING_URL.matcher(url).find())
		{
			// Redirecting URL; figure out where it's redirecting
			
			HttpParams p = mHttpClient.getParams();
			
			try
			{
				p.setParameter("http.protocol.max-redirects", 1);
				
				HttpGet httpget = new HttpGet(url);
				HttpContext context = new BasicHttpContext();
				HttpResponse response = mHttpClient.execute(httpget, context);
				
				HttpEntity entity = response.getEntity();
				if (entity != null)
				    entity.consumeContent();
				
				HttpUriRequest request = (HttpUriRequest)context.getAttribute(
				        ExecutionContext.HTTP_REQUEST);
				
				try
				{
					loadUrl = new URI(url).resolve(request.getURI()).toString();
				}
				catch (URISyntaxException e)
				{
					if (App.getConfig().logToConsole())
						e.printStackTrace();
				}
				
				if (App.getConfig().logToConsole())
					App.logv("Redirection URL determined to be " + loadUrl);
			}
			finally
			{
				p.setParameter("http.protocol.max-redirects", 0);
			}
		}
		
		String page = getResponse(loadUrl.concat("?NoSplash=1"));
		
		GameOverviewInfo overview = new GameOverviewInfo();
		
		Matcher m;
		if (!(m = PATTERN_GAME_OVERVIEW_TITLE.matcher(page)).find())
			throw new ParserException(mContext, R.string.error_no_details_available);
		
		overview.Title = htmlDecode(m.group(1));
		if ((m = PATTERN_GAME_OVERVIEW_DESCRIPTION.matcher(page)).find())
			overview.Description = htmlDecode(m.group(1));
		
		if ((m = PATTERN_GAME_OVERVIEW_MANUAL.matcher(page)).find())
			overview.ManualUrl = m.group(1);
		
		if ((m = PATTERN_GAME_OVERVIEW_ESRB.matcher(page)).find())
		{
			overview.EsrbRatingDescription = htmlDecode(m.group(1));
			overview.EsrbRatingIconUrl = m.group(2);
		}
		
		if ((m = PATTERN_GAME_OVERVIEW_BANNER.matcher(page)).find())
			overview.BannerUrl = m.group(1);
		
		m = PATTERN_GAME_OVERVIEW_IMAGE.matcher(page);
		while (m.find())
			overview.Screenshots.add(m.group(1));
		
		return overview;
	}
	
	public LiveStatusInfo fetchServerStatus() throws IOException, ParserException
	{
		String url = String.format(URL_STATUS, mLocale);
		String page = getResponse(url);
		
		LiveStatusInfo info = new LiveStatusInfo();
		
		Matcher m;
		Matcher lines = PATTERN_STATUS_LINE.matcher(page);
		
		while (lines.find())
		{
			String line = lines.group(1);
			
			if (!(m = PATTERN_STATUS_NAME.matcher(line)).find())
				continue;
			
			String name = htmlDecode(m.group(1));
			boolean isOk = PATTERN_STATUS_IS_OK.matcher(line).find();
			int status = isOk ? XboxLive.LIVE_STATUS_OK	: XboxLive.LIVE_STATUS_ERROR;
			
			String statusText;
			
			if (isOk)
			{
				statusText = mContext.getString(R.string.up_and_running);
			}
			else
			{
				if ((m = PATTERN_STATUS_DESCRIPTION.matcher(line)).find())
					statusText = htmlDecode(m.group(1));
				else
					statusText = mContext.getString(R.string.unknown_problem);
			}
			
			info.addCategory(name, status, statusText);
		}
		
		return info;
	}
	
	private Object fetchParseable(XboxLiveAccount account, ParseableWithResult parseable)
			throws AuthenticationException, IOException, ParserException
	{
		boolean reauthenticated = false;
		Object result = null;
		
		do
		{
			if (!authenticate(account, true))
				throw new AuthenticationException(mContext.getString(R.string.error_invalid_credentials_f, 
								account.getEmailAddress()));
			
	        try
	        {
	        	result = parseable.doParse();
				saveSession(account);
				
				break;
	        }
	        catch(ParserException e)
	        {
	        	if (App.getConfig().logToConsole())
	        	{
	        		App.logv("Unexpected exception");
	        		e.printStackTrace();
	        	}
	        	
	        	if (reauthenticated)
	        		throw e;
	        }
	        catch(ClientProtocolException e)
	        {
	        	if (App.getConfig().logToConsole())
	        	{
	        		App.logv("Unexpected exception");
	        		e.printStackTrace();
	        	}
	        	
	        	if (reauthenticated)
	        		throw e;
	        }
	        catch(IOException e)
	        {
	        	if (App.getConfig().logToConsole())
	        	{
	        		App.logv("Unexpected exception");
	        		e.printStackTrace();
	        	}
	        	
	        	if (reauthenticated)
	        		throw e;
	        }
	        
            if (App.getConfig().logToConsole()) 
            	App.logv("Re-authenticating");
    		
            reauthenticated = true;
            deleteSession(account);
		}
		while(true);
		
		return result;
	}
	
	private void fetchParseable(XboxLiveAccount account, Parseable parseable)
			throws AuthenticationException, IOException, ParserException
	{
		boolean reauthenticated = false;
		
		do
		{
			if (!authenticate(account, true))
				throw new AuthenticationException(mContext.getString(R.string.error_invalid_credentials_f, 
								account.getEmailAddress()));
			
	        try
	        {
	        	parseable.doParse();
				saveSession(account);
				
				break;
	        }
	        catch(ParserException e)
	        {
	        	if (App.getConfig().logToConsole())
	        	{
	        		App.logv("Unexpected exception");
	        		e.printStackTrace();
	        	}
	        	
	        	if (reauthenticated)
	        		throw e;
	        }
	        catch(ClientProtocolException e)
	        {
	        	if (App.getConfig().logToConsole())
	        	{
	        		App.logv("Unexpected exception");
	        		e.printStackTrace();
	        	}
	        	
	        	if (reauthenticated)
	        		throw e;
	        }
	        catch(IOException e)
	        {
	        	if (App.getConfig().logToConsole())
	        	{
	        		App.logv("Unexpected exception");
	        		e.printStackTrace();
	        	}
	        	
	        	if (reauthenticated)
	        		throw e;
	        }
	        
            if (App.getConfig().logToConsole()) 
            	App.logv("Re-authenticating");
    		
            reauthenticated = true;
            deleteSession(account);
		}
		while(true);
	}
	
	public void fetchSummary(final XboxLiveAccount account) 
		throws AuthenticationException, IOException, ParserException
	{
		fetchParseable(account, new Parseable() 
		{
			@Override
			public void doParse() throws AuthenticationException, IOException,
					ParserException 
			{
	        	parseAccountSummary(account);
			}
		});
	}
	
	public void fetchFriendSummary(final XboxLiveAccount account, final String gamertag) 
		throws AuthenticationException, IOException, ParserException
	{
		fetchParseable(account, new Parseable() 
		{
			@Override
			public void doParse() throws AuthenticationException, IOException,
					ParserException 
			{
				parseFriendSummary(account, gamertag);
			}
		});
	}
	
	public void fetchGames(final XboxLiveAccount account) 
		throws AuthenticationException, IOException, ParserException
	{
		fetchParseable(account, new Parseable() 
		{
			@Override
			public void doParse() throws AuthenticationException, IOException,
					ParserException 
			{
	        	parseGames(account);
			}
		});
	}
	
	public void fetchAchievements(final XboxLiveAccount account, final long gameId) 
		throws AuthenticationException, IOException, ParserException
	{
		fetchParseable(account, new Parseable() 
		{
			@Override
			public void doParse() throws AuthenticationException, IOException,
					ParserException 
			{
	        	parseAchievements(account, gameId);
			}
		});
	}
	
	public void fetchFriends(final XboxLiveAccount account) 
		throws AuthenticationException, IOException, ParserException
	{
		fetchParseable(account, new Parseable() 
		{
			@Override
			public void doParse() throws AuthenticationException, IOException,
					ParserException 
			{
	        	parseFriends(account);
			}
		});
	}
	
	public void fetchMessages(final XboxLiveAccount account) 
		throws AuthenticationException, IOException, ParserException
	{
		fetchParseable(account, new Parseable() 
		{
			@Override
			public void doParse() throws AuthenticationException, IOException,
					ParserException 
			{
	        	parseMessages(account);
			}
		});
	}
	
	public void fetchMessage(final XboxLiveAccount account, final long messageId) 
		throws AuthenticationException, IOException, ParserException
	{
		fetchParseable(account, new Parseable() 
		{
			@Override
			public void doParse() throws AuthenticationException, IOException,
					ParserException 
			{
	        	parseViewMessage(account, messageId);
			}
		});
	}
	
	public void fetchDeleteMessage(final XboxLiveAccount account, final long messageUid) 
		throws AuthenticationException, IOException, ParserException
	{
		fetchParseable(account, new Parseable() 
		{
			@Override
			public void doParse() throws AuthenticationException, IOException,
					ParserException 
			{
	        	parseDeleteMessage(account, messageUid);
			}
		});
	}

	public void fetchBlockMessage(final XboxLiveAccount account, final long messageUid) 
		throws AuthenticationException, IOException, ParserException
	{
		fetchParseable(account, new Parseable() 
		{
			@Override
			public void doParse() throws AuthenticationException, IOException,
					ParserException 
			{
	        	parseBlockMessage(account, messageUid);
			}
		});
	}
	
	public void fetchSendMessage(final XboxLiveAccount account,
			final String[] recipients,
			final String body) throws AuthenticationException, IOException, ParserException
	{
		fetchParseable(account, new Parseable() 
		{
			@Override
			public void doParse() throws AuthenticationException, IOException,
					ParserException 
			{
	        	parseSendMessage(account, recipients, body);
			}
		});
	}
	
	public void updateProfile(final XboxLiveAccount account, final String motto,
			final String name, final String location, final String bio) throws ParserException,
			IOException, AuthenticationException
	{
		fetchParseable(account, new Parseable() 
		{
			@Override
			public void doParse() throws AuthenticationException, IOException,
					ParserException 
			{
	        	parseUpdateProfile(account, motto, name, location, bio);
			}
		});
	}

	public void setBeacon(final XboxLiveAccount account, final long gameId, final String message)
		throws AuthenticationException, IOException, ParserException
	{
		fetchParseable(account, new Parseable() 
		{
			@Override
			public void doParse() throws AuthenticationException, IOException,
					ParserException 
			{
		    	parseToggleBeacon(account, gameId, true, message);
			}
		});
	}
	
	public void removeBeacon(final XboxLiveAccount account, final long gameId)
		throws AuthenticationException, IOException, ParserException
	{
		fetchParseable(account, new Parseable() 
		{
			@Override
			public void doParse() throws AuthenticationException, IOException,
					ParserException 
			{
		    	parseToggleBeacon(account, gameId, false, null);
			}
		});
	}
	
	private void fetchFriendRequest(final XboxLiveAccount account, final String requestId, final String gamertag) 
		throws IOException, ParserException, AuthenticationException
	{
		fetchParseable(account, new Parseable() 
		{
			@Override
			public void doParse() throws AuthenticationException, IOException,
					ParserException 
			{
		    	parseFriendRequest(account, requestId, gamertag);
			}
		});
	}
	
	public void addFriend(XboxLiveAccount account, String gamertag)
		throws AuthenticationException, IOException, ParserException
	{
		fetchFriendRequest(account, FRIEND_MANAGER_ADD, gamertag);
	}
	
	public void removeFriend(XboxLiveAccount account, String gamertag)
			throws AuthenticationException, IOException, ParserException
	{
		fetchFriendRequest(account, FRIEND_MANAGER_REMOVE, gamertag);
	}
	
	/*
	public void blockFriend(XboxLiveAccount account, String gamertag)
		throws AuthenticationException, IOException, ParserException
	{
		callFriendManager(account, FRIEND_MANAGER_BLOCK, gamertag);
	}
	*/
	
	public void cancelFriendRequest(XboxLiveAccount account, String gamertag)
		throws AuthenticationException, IOException, ParserException
	{
		fetchFriendRequest(account, FRIEND_MANAGER_CANCEL, gamertag);
	}
	
	public void acceptFriendRequest(XboxLiveAccount account, String gamertag)
		throws AuthenticationException, IOException, ParserException
	{
		fetchFriendRequest(account, FRIEND_MANAGER_ACCEPT, gamertag);
	}
	
	public void rejectFriendRequest(XboxLiveAccount account, String gamertag)
		throws AuthenticationException, IOException, ParserException
	{
		fetchFriendRequest(account, FRIEND_MANAGER_REJECT, gamertag);
	}
	
	public ComparedGameInfo fetchCompareGames(final XboxLiveAccount account, final String gamertag)
			throws AuthenticationException, IOException, ParserException
	{
		return (ComparedGameInfo)fetchParseable(account, new ParseableWithResult() 
		{
			@Override
			public Object doParse() throws AuthenticationException, IOException,
					ParserException 
			{
		    	return parseCompareGames(account, gamertag);
			}
		});
	}
	
	public GamerProfileInfo fetchGamerProfile(final XboxLiveAccount account, final String gamertag)
			throws AuthenticationException, IOException, ParserException
	{
		return (GamerProfileInfo)fetchParseable(account, new ParseableWithResult() 
		{
			@Override
			public Object doParse() throws AuthenticationException, IOException,
					ParserException 
			{
		    	return parseGamerProfile(account, gamertag);
			}
		});
	}
	
	public FriendsOfFriend fetchFriendsOfFriend(final XboxLiveAccount account,
			final String gamertag) throws AuthenticationException, IOException,
			ParserException
	{
		return (FriendsOfFriend)fetchParseable(account, new ParseableWithResult() 
		{
			@Override
			public Object doParse() throws AuthenticationException, IOException,
					ParserException 
			{
		    	return parseFriendsOfFriend(account, gamertag);
			}
		});
	}
	
	public RecentPlayers fetchRecentPlayers(final XboxLiveAccount account)
			throws AuthenticationException, IOException, ParserException
	{
		return (RecentPlayers)fetchParseable(account, new ParseableWithResult() 
		{
			@Override
			public Object doParse() throws AuthenticationException, IOException,
					ParserException 
			{
		    	return parseRecentPlayers(account);
			}
		});
	}
	
	public ComparedAchievementInfo fetchCompareAchievements(final XboxLiveAccount account,
			final String gamertag, final String gameUid) throws AuthenticationException,
			IOException, ParserException
	{
		return (ComparedAchievementInfo)fetchParseable(account, new ParseableWithResult() 
		{
			@Override
			public Object doParse() throws AuthenticationException, IOException,
					ParserException 
			{
		    	return parseCompareAchievements(account, gamertag, gameUid);
			}
		});
	}
	
	public GameOverviewInfo fetchGameOverview(final XboxLiveAccount account, final String url)
			throws AuthenticationException, IOException, ParserException
	{
		return (GameOverviewInfo)fetchParseable(account, new ParseableWithResult() 
		{
			@Override
			public Object doParse() throws AuthenticationException, IOException,
					ParserException 
			{
		    	return parseGameOverview(url);
			}
		});
	}
	
	@Override
	public ContentValues validateAccount(BasicAccount account)
			throws AuthenticationException, IOException, ParserException
	{
		if (!authenticate(account, true))
			throw new AuthenticationException(mContext
					.getString(R.string.error_invalid_credentials_f, 
							account.getLogonId()));
		
		ContentValues cv = parseSummaryData((XboxLiveAccount)account);
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
        Cursor c = cr.query(Games.CONTENT_URI, 
        		new String[] { Games._ID }, 
        		Games.ACCOUNT_ID + "=" + accountId, null, 
        		null);
        
        if (c != null)
        {
            StringBuffer buffer = new StringBuffer();
            
	        try
	        {
	        	while (c.moveToNext())
	        	{
	        		if (buffer.length() > 0)
	        			buffer.append(",");
	        		buffer.append(c.getLong(0));
	        	}
	        	
		        if (buffer.length() > 0)
		        {
			    	// Clear achievements
			    	cr.delete(Achievements.CONTENT_URI, 
			    			Achievements.GAME_ID + " IN (" + buffer.toString() + ")", null);
		        }
	        }
	        catch(Exception e)
	        {
	        	// Do nothing
	        }
	        finally
	        {
        		c.close();
	        }
		}
        
        try
        {
	    	// Clear games
	    	cr.delete(Games.CONTENT_URI, Games.ACCOUNT_ID + "=" + accountId, null);
        }
        catch(Exception e)
        {
        	// Do nothing
        }
        
        try
        {
            // Delete friends
            cr.delete(Friends.CONTENT_URI, Friends.ACCOUNT_ID + "=" + accountId, null);
        }
        catch(Exception e)
        {
        	// Do nothing
        }
        
        try
        {
            // Delete messages
            cr.delete(Messages.CONTENT_URI, Messages.ACCOUNT_ID + "=" + accountId, null);
        }
        catch(Exception e)
        {
        	// Do nothing
        }
        
        try
        {
            // Delete sent messages
            cr.delete(SentMessages.CONTENT_URI, SentMessages.ACCOUNT_ID + "=" + accountId, null);
        }
        catch(Exception e)
        {
        	// Do nothing
        }
        
        try
        {
            // Delete beacons
            cr.delete(Beacons.CONTENT_URI, Beacons.ACCOUNT_ID + "=" + accountId, null);
        }
        catch(Exception e)
        {
        	// Do nothing
        }
        
        try
        {
            // Delete profiles
            cr.delete(Profiles.CONTENT_URI, Profiles.ACCOUNT_ID + "=" + accountId, null);
        }
        catch(Exception e)
        {
        	// Do nothing
        }
        
        try
        {
            // Delete notify states
            cr.delete(NotifyStates.CONTENT_URI, Profiles.ACCOUNT_ID + "=" + accountId, null);
        }
        catch(Exception e)
        {
        	// Do nothing
        }
        
        try
        {
            // Delete authenticated session
            deleteSession(account);
        }
        catch(Exception e)
        {
        	// Do nothing
        }
        
        // Send notifications
        cr.notifyChange(Achievements.CONTENT_URI, null);
        cr.notifyChange(Games.CONTENT_URI, null);
        cr.notifyChange(Friends.CONTENT_URI, null);
        cr.notifyChange(Messages.CONTENT_URI, null);
        cr.notifyChange(Profiles.CONTENT_URI, null);
        cr.notifyChange(Beacons.CONTENT_URI, null);
        cr.notifyChange(SentMessages.CONTENT_URI, null);
        cr.notifyChange(NotifyStates.CONTENT_URI, null);
        
	}
	
	public void createAccount(BasicAccount account, ContentValues cv)
	{
		XboxLiveAccount xblAccount = (XboxLiveAccount)account;
		
		// Save changes to preferences
		xblAccount.setGamertag(cv.getAsString(Profiles.GAMERTAG));
		xblAccount.setLastSummaryUpdate(System.currentTimeMillis());
		xblAccount.setIconUrl(cv.getAsString(Profiles.ICON_URL));
		xblAccount.setGoldStatus(cv.getAsBoolean(Profiles.IS_GOLD));
		
		account.save(Preferences.get(mContext));
		
		// Add profile to database
		ContentResolver cr = mContext.getContentResolver();
		cr.insert(Profiles.CONTENT_URI, cv);
		cr.notifyChange(Profiles.CONTENT_URI, null);
	}
	
	private static final class Gamer implements Serializable
	{
		private static final long serialVersionUID = -7487842118929407021L;

		public String Gamertag;
		public String CurrentActivity;
		public String IconUrl;
		public String TitleIconUrl;
		public int Gamerscore;
		public String TitleId;
		public boolean IsFriend;
		
		public Gamer()
		{
			this.IsFriend = false;
			this.Gamertag = null;
			this.IconUrl = null;
			this.TitleIconUrl = null;
			this.CurrentActivity = null;
			this.Gamerscore = 0;
			this.TitleId = null;
		}
		
		public static class Comparator implements java.util.Comparator<Gamer>
		{
			@Override
			public int compare(Gamer object1, Gamer object2)
			{
				return object1.Gamertag.compareToIgnoreCase(object2.Gamertag);
			}
		}
	}
}
