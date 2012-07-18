/*
 * Parser.java
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.os.Environment;

import com.akop.bach.Account;
import com.akop.bach.App;
import com.akop.bach.R;
import com.akop.bach.util.SerializableCookie;

public abstract class Parser
{
	private static final int TIMEOUT_MS = 30 * 1000;
	
	protected Context mContext;
	protected DefaultHttpClient mHttpClient;
	
	protected HttpResponse mLastResponse;
	protected String mLastUrl;
	
	protected Parser(Context context)
	{
		mContext = context;
		mHttpClient = createHttpClient(context);
		mLastResponse = null;
		
		HttpParams params = mHttpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(params, TIMEOUT_MS);
		HttpConnectionParams.setSoTimeout(params, TIMEOUT_MS);
		
		if (App.LOGV)
			App.logv("Creating parser");
	}
	
	public static String getErrorMessage(Context context, Exception e)
	{
		if (e instanceof SocketTimeoutException)
			return context.getString(R.string.error_timed_out);
		else if (e instanceof UnknownHostException)
			return context.getString(R.string.error_dns_error);
		else if (e instanceof IOException)
			return context.getString(R.string.error_network_error);
		else if (e instanceof AuthenticationException)
		{
			if (e.getMessage() == null)
				return context.getString(R.string.error_invalid_credentials);
			else
				return e.getMessage();
		}
		else if (e instanceof ParserException)
			return e.getMessage();
		
		// All other cases failed
		if (e.getMessage() != null)
		{
			return context.getString(R.string.error_unexpected_f, 
					e.getMessage());
		}
		else
		{
			return context.getString(R.string.error_unexpected_f, 
					e.getClass().getName());
		}
	}
	
	protected DefaultHttpClient createHttpClient(Context context)
	{
		return new DefaultHttpClient();
	}
	
	protected String resolveImageUrl(String pageUrl, String partialImageUrl)
	{
		try
		{
			if (partialImageUrl.startsWith("/"))
			{
				// Find the start of the hostname
				int hostStart = pageUrl.indexOf("://");
				if (hostStart >= 0 && hostStart + 3 < pageUrl.length())
				{
					hostStart += 3;
					
					// Find the end of the hostname
					int hostEnd = pageUrl.indexOf("/", hostStart);
					if (hostEnd < 0)
						hostEnd = pageUrl.length();
					
					// Concatenate protocol, host, partial image URL
					return pageUrl.substring(0, hostEnd) + partialImageUrl;
				}
			}
			
			return partialImageUrl;
		}
		catch(Exception e)
		{
			return partialImageUrl;
		}
	}
	
	protected void writeToFile(String filename, String text)
	{
		java.io.OutputStreamWriter osw = null;
		
		try
		{
			java.io.FileOutputStream fOut = mContext.openFileOutput(filename, 0666);
			osw = new java.io.OutputStreamWriter(fOut);
			osw.write(text);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (osw != null)
			{
				try
				{
					osw.flush();
					osw.close();
				}
				catch (IOException e)
				{
				}
			}
		}
	}
	
	protected void writeToSd(String filename, String text)
	{
		java.io.OutputStreamWriter osw = null;
		File root = Environment.getExternalStorageDirectory();
		
		try
		{
			java.io.FileOutputStream fOut = new FileOutputStream(root + "/" + filename);
			osw = new java.io.OutputStreamWriter(fOut);
			osw.write(text);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (osw != null)
			{
				try
				{
					osw.flush();
					osw.close();
				}
				catch (IOException e)
				{
				}
			}
		}
	}
	
	protected String readFromFile(String filename)
	{
		String text = "";
		java.io.BufferedReader buf = null;
		
		try
		{
			java.io.FileInputStream fis = mContext.openFileInput(filename);
			buf = new java.io.BufferedReader(new java.io.InputStreamReader(fis));
			String line;
			while ((line = buf.readLine()) != null)
				text += line + "\r\n";
			
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (buf != null)
					buf.close();
			}
			catch (IOException e)
			{
			}
		}
		
		return text;
	}
	
	@Override
	protected void finalize() throws Throwable
	{
		super.finalize();
		
		dispose();
	}
	
	public void dispose()
	{
		mHttpClient.getConnectionManager().shutdown();
		
		if (App.LOGV)
			App.logv("Disposing parser");
	}
	
	public static String joinString(List<Object> list, String delim)
	{
		Object[] array = new String[list.size()];
		list.toArray(array);
		
		return joinString(array, delim);
	}
	
	public static String joinString(long[] array, String delim)
	{
		Long[] objArray = new Long[array.length];
		for (int i = 0; i < array.length; i++)
			objArray[i] = array[i];
		
		return joinString(objArray, delim);
	}
	
	public static String joinString(Object[] array, String delim)
	{
		StringBuilder sb = new StringBuilder();
		for (Object obj: array)
		{
			if (obj != null)
			{
				String str = obj.toString();
				if (str.length() > 0)
				{
					sb.append(str);
					sb.append(delim);
				}
			}
		}
		
		if (sb.length() > 0)
			return sb.substring(0, sb.length() - delim.length());
		
		return "";
	}
	
	protected static void setValue(List<NameValuePair> inputs, 
			String name, Object value)
	{
		for (int i = inputs.size() - 1; i >= 0; i--)
		{
			if (inputs.get(i).getName().equals(name))
			{
				inputs.set(i, new BasicNameValuePair(name, value.toString()));
				return;
			}
		}
		
		inputs.add(new BasicNameValuePair(name, value.toString()));
	}
	
	protected static void addValue(List<NameValuePair> inputs, 
			String name, Object value)
	{
		inputs.add(new BasicNameValuePair(name, value.toString()));
	}
	
	protected static boolean hasName(List<NameValuePair> inputs, String name)
	{
		for (int i = inputs.size() - 1; i >= 0; i--)
			if (inputs.get(i).getName().equals(name))
				return true;
		return false;
	}
	
	protected void initRequest(HttpUriRequest request)
	{
	}
	
	protected void submitRequest(HttpUriRequest request)
		throws IOException
	{
		request.addHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
		request.addHeader("Accept", "text/javascript, text/html, application/xml, text/xml, */*");
		
		if (App.LOGV)
			App.logv("Parser: Fetching %s", request.getURI());
		
	    long started = System.currentTimeMillis();
	    
	    initRequest(request);
	    
	    try
	    {
			synchronized (mHttpClient)
			{
				mLastResponse = mHttpClient.execute(request);
				
				HttpEntity entity = mLastResponse.getEntity();
				if (entity != null)
					entity.consumeContent();
			}
	    }
	    finally
	    {
	    	if (App.LOGV)
	    		displayTimeTaken("Parser: Fetch took", started);
	    }
	}

	protected void submitRequest(String url)
		throws IOException
	{
		submitRequest(new HttpGet(url));
	}
	
	protected void submitRequest(String url, 
			List<NameValuePair> inputs) throws IOException
	{
		HttpPost httpPost = new HttpPost(url);
		HttpEntity entity = new UrlEncodedFormEntity(inputs, HTTP.UTF_8);
		httpPost.setEntity(entity);
		
		submitRequest(httpPost);
	}
	
	protected String getResponse(HttpUriRequest request)
		throws IOException, ParserException
	{
		if (!request.containsHeader("Accept"))
			request.addHeader("Accept", "text/javascript, text/html, application/xml, text/xml, */*");
		if (!request.containsHeader("Accept-Charset"))
			request.addHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
		
		if (App.LOGV)
			App.logv("Parser: Fetching %s", request.getURI());
		
	    long started = System.currentTimeMillis();
	    
	    initRequest(request);
	    
	    try
	    {
			synchronized (mHttpClient)
			{
				HttpContext context = new BasicHttpContext();
				
				try
				{
					mLastResponse = mHttpClient.execute(request, context);
				}
				catch(SocketTimeoutException e)
				{
					throw new ParserException(mContext, R.string.error_timed_out);
				}
				
				try
				{
					if (mLastResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
					{
				        HttpUriRequest currentReq = (HttpUriRequest)context.getAttribute( 
				                ExecutionContext.HTTP_REQUEST);
				        HttpHost currentHost = (HttpHost)  context.getAttribute( 
				                ExecutionContext.HTTP_TARGET_HOST);
				        
				        this.mLastUrl = currentHost.toURI() + currentReq.getURI();
					}
				}
				catch(Exception e)
				{
					if (App.LOGV)
					{
						App.logv("Unable to get last URL - see stack:");
						e.printStackTrace();
					}
					
					this.mLastUrl = null;
				}
				
				HttpEntity entity = mLastResponse.getEntity();
				
				if (entity == null)
					return null;
				
				InputStream stream = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(stream), 10000);
				StringBuilder builder = new StringBuilder(10000);
				
				try
				{
					int read;
					char[] buffer = new char[1000];
					
					while ((read = reader.read(buffer)) >= 0)
						builder.append(buffer, 0, read);
				}
				catch(OutOfMemoryError e)
				{
					return null;
				}
				
				stream.close();
				entity.consumeContent();
				
				try
				{
					return preparseResponse(builder.toString());
				}
				catch(OutOfMemoryError e)
				{
					if (App.LOGV)
						e.printStackTrace();
					
					return null;
				}
			}
	    }
	    finally
	    {
	    	if (App.LOGV)
	    		displayTimeTaken("Parser: Fetch took", started);
	    }
	}
	
	protected String preparseResponse(String response) 
		throws IOException
	{
		return response;
	}
	
	protected String getResponse(String url) 
		throws IOException, ParserException
	{
		return getResponse(url, false);
	}
	
	protected String getResponse(String url, boolean useXhr) 
		throws IOException, ParserException
	{
		HttpGet get = new HttpGet(url);
		if (useXhr)
			get.addHeader("X-Requested-With", "XMLHttpRequest");
		
		return getResponse(get);
	}
	
	protected String getResponse(String url, List<NameValuePair> inputs, boolean useXhr) 
		throws IOException, ParserException
	{
		HttpPost httpPost = new HttpPost(url);
		HttpEntity entity = new UrlEncodedFormEntity(inputs, HTTP.UTF_8);
		httpPost.setEntity(entity);
		
		if (useXhr)
			httpPost.addHeader("X-Requested-With", "XMLHttpRequest");
		
		return getResponse(httpPost);
	}
	
	protected String getResponse(String url, List<NameValuePair> inputs) 
		throws IOException, ParserException
	{
		return getResponse(url, inputs, false);
	}
	
	protected static long displayTimeTaken(String description, long started)
	{
		long now = System.currentTimeMillis();
		
		if (App.LOGV)
			App.logv("%s: %.02f s", description, (now - started) / 1000.0);
		
		return now;
	}
	
	protected static String htmlDecode(String s)
	{
		return android.text.Html.fromHtml(s).toString().trim();
	}
	
	protected static String htmlDecodeWithCrLf(String s)
	{
		if (s == null)
			return null;
		
		String replaced = s.replace("\n", "<br/>");
		return android.text.Html.fromHtml(replaced).toString().trim();
	}
	
	protected boolean deleteSession(Account account)
	{
		if (account == null)
			return false;
		
		try
		{
			return mContext.deleteFile(getSessionFile(account));
		}
		catch(Exception ex)
		{
			if (App.LOGV)
				ex.printStackTrace();
			
			return false;
		}
	}
	
	protected boolean saveSession(Account account)
	{
		ObjectOutputStream objStream = null;
		final List<Cookie> cookies = mHttpClient.getCookieStore().getCookies();
		
		// Create list of serializable cookies
		final List<Cookie> serializableCookies = 
			new ArrayList<Cookie>(cookies.size());
		for (Cookie cookie: cookies)
			serializableCookies.add(new SerializableCookie(cookie));
		
		// Write them to file
		// Don't care about errors - authentication can always be re-done
		try
		{
			objStream = new ObjectOutputStream(mContext.openFileOutput(
					getSessionFile(account), 0));
			objStream.writeObject(serializableCookies);
		}
		catch (FileNotFoundException e)
		{
			return false;
		}
		catch (IOException e)
		{
			return false;
		}
		finally
		{
			if (objStream != null)
			{
				try
				{
					objStream.close();
				}
				catch (IOException e)
				{
					// Don't care
				}
			}
		}
		
		return true;
	}
	
	protected String getSessionFile(Account account)
	{
		return account.getUuid() + ".session";
	}
	
	@SuppressWarnings("unchecked")
	protected boolean loadSession(Account account)
	{
		ObjectInputStream objStream = null;
		final List<Cookie> serializableCookies;
		
		CookieStore store = mHttpClient.getCookieStore();
		store.clear();
		
		if (account == null)
			return false;
		
		String filename = getSessionFile(account);
		if (filename == null)
			return false;
		
		File f = new File(mContext.getFilesDir(), filename);
		boolean fileExists = f.exists();
		
		if (!fileExists)
			return false;
		
		// Don't care about errors - authentication can always be re-done
		
		try
		{
			objStream = new ObjectInputStream(mContext
					.openFileInput(filename));
			serializableCookies = (ArrayList<Cookie>)objStream.readObject();
		}
		catch (StreamCorruptedException e)
		{
			return false;
		}
		catch (IOException e)
		{
			return false;
		}
		catch (ClassNotFoundException e)
		{
			return false;
		}
		catch (OutOfMemoryError e)
		{
			return false;
		}
		finally
		{
			if (objStream != null)
			{
				try
				{
					objStream.close();
				}
				catch (IOException e)
				{
					// Don't care
				}
			}
		}
		
		try
		{
			for (Cookie cookie : serializableCookies)
				store.addCookie(cookie);
		}
		catch(Exception ex)
		{
			deleteSession(account);
			return false;
		}
		
		return true;
	}
	
	protected void addCookie(String name, String value)
	{
		Cookie cookie = new BasicClientCookie(name, value);
		mHttpClient.getCookieStore().addCookie(cookie);
	}
	
	protected abstract boolean onAuthenticate(Account account)
			throws IOException, ParserException, AuthenticationException;
	
	public abstract void deleteAccount(Account account)
			throws AuthenticationException, IOException, ParserException;
	
	public abstract ContentValues validateAccount(Account account)
		throws AuthenticationException, IOException, ParserException;
	
	public final boolean authenticate(Account account, boolean useStoredSession) 
		throws IOException, ParserException, AuthenticationException
	{
        long started = System.currentTimeMillis();
        
        // Clear cookie store
        mHttpClient.getCookieStore().clear();
        
        if (App.LOGV) 
            App.logv("Authenticating...");
        
        boolean sessionLoaded;
        
        try
        {
        	sessionLoaded = loadSession(account);
        }
        catch(Exception ex)
        {
        	sessionLoaded = false;
        	deleteSession(account);
        	
        	if (App.LOGV)
        		ex.printStackTrace();
        }
        
		// Attempt to load session data from file
		if (useStoredSession && sessionLoaded)
		{
            if (App.LOGV) 
                App.logv("Authenticated with stored session '"
                		+ getSessionFile(account) + "'");
		}
		else
		{
	        if (App.LOGV) 
	            App.logv("Logging in...");
			
	        if (!account.isValid())
	        	throw new AuthenticationException(mContext, 
	        			R.string.error_account_corrupted);
	        
	        // Perform the actual authentication
	        if (!onAuthenticate(account))
	        {
	        	if (App.LOGV)
	        		App.logv("onAuthenticate failed!");
	        	
	        	return false;
	        }
			
			if (useStoredSession) 
				saveSession(account);
		}
		
		if (App.LOGV)
			displayTimeTaken("Authentication completed", started);
		
		return true;
	}
	
	protected JSONObject getJSONObject(String response) throws ParserException
	{
		return getJSONObject(response, true);
	}
	
	protected JSONObject getJSONObject(String response, 
			boolean findObject) throws ParserException
	{
		String object;
		
		if (findObject)
		{
			int startPos, endPos;
			if ((startPos = response.indexOf("{")) < 0)
			{
				throw new ParserException(mContext, 
						R.string.error_json_parser_error);
			}
			
			if ((endPos = response.lastIndexOf("}")) < 0)
			{
				throw new ParserException(mContext, 
						R.string.error_json_parser_error);
			}
			
			object = response.substring(startPos, endPos + 1);
		}
		else
		{
			object = response;
		}
		
		try
		{
			return new JSONObject(object);
		}
		catch(Exception e)
		{
			throw new ParserException(mContext, R.string.error_json_parser_error);
		}
	}
	
	protected String stripHTML(String text)
	{
		return Pattern.compile("<[^>]*>").matcher(text).replaceAll("");
	}
	
	protected static String sha1(String message)
	{
	    MessageDigest md;
	    
		try
		{
			md = MessageDigest.getInstance("SHA-1");
		}
		catch (NoSuchAlgorithmException e)
		{
			if (App.LOGV)
				e.printStackTrace();
			
			return null;
		}
		
	    try
		{
			md.update(message.getBytes("UTF-8"), 
					0, message.length());
		}
		catch (UnsupportedEncodingException e)
		{
			if (App.LOGV)
				e.printStackTrace();
			
			return null;
		}
	    
		return String.format("%x", new BigInteger(md.digest()));
	}
}
