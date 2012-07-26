/*
 * LiveParser.java 
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecFactory;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SetCookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicExpiresHandler;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;

import android.content.Context;

import com.akop.bach.Account;
import com.akop.bach.App;
import com.akop.bach.R;
import com.akop.bach.XboxLiveAccount;

public abstract class LiveParser extends Parser
{
	private static final String URL_LOGIN = 
		"https://login.live.com/login.srf?wa=wsignin1.0&wreply=%1$s";
	private static final String URL_LOGIN_MSN =
		"https://msnia.login.live.com/ppsecure/post.srf?wa=wsignin1.0&wreply=%1$s";
	
	private static final Pattern PATTERN_LIVE_AUTH_URL = Pattern
			.compile(",urlPost:'([^']*)'");
	
	private static final Pattern PATTERN_ACTION_URL = Pattern.compile(
			"action=\"(https?://[^\"]+)\"", Pattern.CASE_INSENSITIVE);
	private static final Pattern PATTERN_INPUT_ATTR_LIST = Pattern.compile(
			"<input((\\s+\\w+=\"[^\"]*\")+)[^>]*>", Pattern.CASE_INSENSITIVE);
	private static final Pattern PATTERN_GET_ATTRS = Pattern
			.compile("(\\w+)=\"([^\"]*)\"");
	
	private static final Pattern PATTERN_AUTHENTICATED = Pattern
			.compile(" onload=\"javascript:DoSubmit\\(\\);\"");
	
	private static class LenientCookieSpec extends BrowserCompatSpec
	{
		private static final DateFormat LIVE_COOKIE_DATE_FORMAT = 
				new SimpleDateFormat("E, dd-MMM-yyyy HH:mm:ss zzz", Locale.US);
		
		public LenientCookieSpec()
		{
			super();
			
			registerAttribHandler(ClientCookie.EXPIRES_ATTR,
			        new BasicExpiresHandler(DATE_PATTERNS)
			        {
				        @Override
				        public void parse(SetCookie cookie, String value)
				                throws MalformedCookieException
				        {
				        	// THEIRS: Wed, 30-Dec-2037 16:00:00 GMT
				        	// PROPER: Tue, 15 Jan 2013 21:47:38 GMT
				        	
				        	Date expires = null;
				        	
				    		String language = Locale.getDefault().getLanguage();
				    		String country = Locale.getDefault().getCountry();
				    		
				    		if ("en".equals(language) && "US".equals(country))
				    		{
				    			// US only
				    			
					        	if (value != null && value.contains("-"))
						        {
									try
									{
										expires = LIVE_COOKIE_DATE_FORMAT.parse(value);
									}
									catch (Exception e)
									{
										if (App.LOGV)
										{
											App.logv("Got an error during expiration cookie parse:");
											e.printStackTrace();
										}
									}
						        }
				    		}
				    		
				    		if (expires != null)
				    		{
				    			if (App.LOGV)
				    				App.logv("Setting expiration date explicitly");
				    			
				    			cookie.setExpiryDate(expires);
				    		}
				    		else
				    		{
				    			if (App.LOGV)
				    				App.logv("Using default expiration date handler");
				    			
				    			super.parse(cookie, value);
				    		}
				        }
			        });
		}
	}
	
	protected LiveParser(Context context)
	{
		super(context);
		
		HttpParams params = mHttpClient.getParams();
		//params.setParameter("http.useragent", USER_AGENT);
		params.setParameter("http.protocol.max-redirects", 0);
	}
	
	@Override
	protected DefaultHttpClient createHttpClient(Context context)
	{
		DefaultHttpClient client = new DefaultHttpClient();
		
		client.getCookieSpecs().register("lenient", new CookieSpecFactory()
		{
			public CookieSpec newInstance(HttpParams params)
			{
				return new LenientCookieSpec();
			}
		});
		
		HttpClientParams.setCookiePolicy(client.getParams(), "lenient");
		
		return client;
	}
	
	private String getLoginUrl(XboxLiveAccount account)
	{
		if (account.isMsnAccount())
			return String.format(URL_LOGIN_MSN, getReplyToPage());
		else
			return String.format(URL_LOGIN, getReplyToPage());
	}
	
	private String getPostUrl(XboxLiveAccount account, String originalUrl)
	{
		if (originalUrl == null)
			return null;
		
		if (account.isMsnAccount())
			return originalUrl.replace("://login.live.com/", 
					"://msnia.login.live.com/");
		
		return originalUrl;
	}
	
	private static String getActionUrl(String response)
	{
		Matcher matcher = PATTERN_ACTION_URL.matcher(response);
		
		while (matcher.find())
			return matcher.group(1);
		
		return null;
	}
	
	protected abstract String getReplyToPage();
	
	protected static void getInputs(String response, 
			List<NameValuePair> inputs,
			Pattern pattern)
	{
		Matcher allAttrs = PATTERN_INPUT_ATTR_LIST.matcher(response);
		inputs.clear();
		boolean add;
		
		while (allAttrs.find())
		{
			String name = null, value = null;
			Matcher attrs = PATTERN_GET_ATTRS.matcher(allAttrs.group(1));
			add = true;

			while (attrs.find())
			{
				/*
				 * We actually need this; at least for message composition 
				if (attrs.group(1).equalsIgnoreCase("type") 
						&& attrs.group(2).equalsIgnoreCase("submit"))
				{
					add = false;
					break;
				}
				*/
				
				if (attrs.group(1).equalsIgnoreCase("name"))
					name = attrs.group(2);
				else if (attrs.group(1).equalsIgnoreCase("value"))
					value = attrs.group(2);
			}
			
			if (!add || name == null || value == null)
				continue;
			
			// If a pattern is supplied, make sure NAME matches pattern
			if (pattern != null)
			{
				if (!pattern.matcher(name).find())
					continue;
			}
			
			for (NameValuePair p : inputs)
			{
				if (p.getName().equals(name))
				{
					add = false;
					break;
				}
			}
			
			if (!add)
				continue;
			
			inputs.add(new BasicNameValuePair(name, value));
		}
	}
	
	private boolean newAuthenticate(XboxLiveAccount xblAccount)
			throws IOException, ParserException, AuthenticationException
	{
		List<NameValuePair> inputs = new ArrayList<NameValuePair>(10);
		
		// 1. Initial login page fetch
		String url = getLoginUrl(xblAccount);
		String page = getResponse(url);
		
		// 2. Post to initial login page
		Matcher m;
		if (!(m = PATTERN_LIVE_AUTH_URL.matcher(page)).find())
		{
			if (App.LOGV)
				App.logv("Authentication error in stage 1");
			
			return false;
		}
		
		url = getPostUrl(xblAccount, m.group(1));
		getInputs(page, inputs, null);
		
		setValue(inputs, "login", xblAccount.getEmailAddress());
		setValue(inputs, "passwd", xblAccount.getPassword());
		setValue(inputs, "KMSI", 1);
		setValue(inputs, "LoginOptions", 1);
		
		page = getResponse(url, inputs);
		
		// 3. Just post whatever was returned again
		url = getActionUrl(page);
		getInputs(page, inputs, null);
		
		// Did authentication succeed?
		if (!hasName(inputs, "ANON") && !PATTERN_AUTHENTICATED.matcher(page).find())
		{
			if (App.LOGV)
				App.logv("Authentication error in stage 2");
			
			return false;
		}
		
		try
		{
			// Final post
			submitRequest(url, inputs);
		}
		catch(ClientProtocolException e)
		{
			// Because redirection is disabled, this call will throw an 
			// exception when XBL redirects. We just ignore it
		}
		
		return true;
	}
	
	@Override
	protected boolean onAuthenticate(Account account) 
			throws IOException, ParserException, AuthenticationException
	{
		if (!newAuthenticate((XboxLiveAccount)account))
			throw new AuthenticationException(mContext.getString(R.string.credential_error_locked));
		
		return true;
	}
	
	@Override
	protected String getResponse(HttpUriRequest request)
		throws IOException, ParserException
	{
		int retries = -1;
		boolean retry;
		String response = null;
		
		do
		{
			retry = false;
			retries++;
			
			try
			{
				response = super.getResponse(request);
			}
			catch(HttpHostConnectException e)
			{
				// Sleep for a second, then try again
				
            	if (retries > 1)
            	{
            		if (App.LOGV)
            			App.logv("Too many HttpHostConnectException's; bailing");
            		
            		throw e;
            	}
            	
        		if (App.LOGV)
        			App.logv("Got HttpHostConnectException; retrying (%1$d)... ", retries);
        		
				try
                {
                    Thread.sleep(1500);
                }
                catch (InterruptedException e1)
                {
                	if (App.LOGV)
                		e1.printStackTrace();
                }
				
				retry = true;
			}
		} while(retry);
		
		return response;
	}
}
