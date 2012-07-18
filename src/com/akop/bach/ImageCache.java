/*
 * ImageCache.java
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

package com.akop.bach;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Environment;
import android.os.Process;
import android.view.View;

public class ImageCache implements Runnable
{
	public static interface OnImageReadyListener
	{
		public class ViewBitmapContainer
		{
			public View Control;
			public Bitmap BitmapToSet;
		}
		
		void onImageReady(long id, Object param, Bitmap bmp);
	}
	
	private static final int TIMEOUT_MS = 15 * 1000;
	
    private MediaScannerConnection mMsc;
    
	class Api8Helper
	{
		public File getExternalCacheDir()
		{
			return mContext.getExternalCacheDir();
		}
		
		public File getExternalPictureDir()
		{
			return mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		}
	}
	
	public static class CachePolicy
	{
		public boolean bypassCache;
		public int resizeWidth;
		public int resizeHeight;
		public long maxAgeSeconds;
		
		public static final long SECONDS_IN_MINUTE = 60;
		public static final long SECONDS_IN_HOUR = 60 * 60;
		public static final long SECONDS_IN_DAY = 60 * 60 * 24;
		
		public CachePolicy()
		{
			bypassCache = false;
			resizeWidth = 0;
			resizeHeight = 0;
			maxAgeSeconds = 0;
		}
		
		public CachePolicy(long maxAgeSeconds)
		{
			this.maxAgeSeconds = maxAgeSeconds;
		}
		
		public CachePolicy(boolean bypassCache)
		{
			this.bypassCache = bypassCache;
		}
		
		public CachePolicy(int resizeWidth, int resizeHeight)
		{
			this.resizeWidth = resizeWidth;
			this.resizeHeight = resizeHeight;
		}
		
		public boolean expires()
		{
			return this.maxAgeSeconds > 0;
		}
		
		public boolean resize()
		{
			return this.resizeWidth > 0 || this.resizeHeight > 0;
		}
		
		public boolean expired(long currentTimeMillis, long lastModifiedMillis)
		{
			if (!expires())
				return false;
			
			long ageSeconds = (currentTimeMillis - lastModifiedMillis) / 1000;
			
			if (App.LOGV)
			{
				if (ageSeconds > this.maxAgeSeconds)
				{
					App.logv("Cached file has expired (age is %1$d seconds; max age %2$d)",
							ageSeconds, this.maxAgeSeconds);
				}
				else
				{
					App.logv("Cached file is still valid (age is %1$d seconds; max age %2$d)",
							ageSeconds, this.maxAgeSeconds);
				}
			}
			
			return (ageSeconds > this.maxAgeSeconds);
		}
	}
	
	private class Task
	{
		public OnImageReadyListener listener;
		public String imageUrl;
		public Object param;
		public long id;
		public boolean alwaysRun;
		public CachePolicy cachePol;
		
		public Task(String imageUrl, long id, Object param,
				OnImageReadyListener listener, boolean alwaysRun, CachePolicy cachePol)
		{
			this.listener = listener;
			this.imageUrl = imageUrl;
			this.param = param;
			this.id = id;
			this.alwaysRun = alwaysRun;
			this.cachePol = cachePol;
		}
	}
	
	private Thread mThread;
	private Context mContext;
	private boolean mBusy;
	private Task mCurrentTask;
    private HashSet<OnImageReadyListener> mListeners;
    private BlockingQueue<Task> mTasks;
    private static ImageCache inst = null;
    private File mCacheDir;
	private File mSdImageDir;
	private File mSdCacheDir;
    
	private ImageCache(Application application)
	{
		mContext = application.getApplicationContext();
		mTasks = new LinkedBlockingQueue<Task>();
		mListeners = new HashSet<OnImageReadyListener>();
		mCurrentTask = null;
		mCacheDir = application.getCacheDir();
		mSdCacheDir = null;
		mSdImageDir = null;
		
		try
		{
	        if (android.os.Build.VERSION.SDK_INT >= 8)
	        {
	        	Api8Helper helper = new Api8Helper();
	        	
	        	mSdCacheDir = helper.getExternalCacheDir();
	        	mSdImageDir = helper.getExternalPictureDir();
	        }
	        else
	        {
				mSdCacheDir = new File(Environment.getExternalStorageDirectory(),
						"/Android/data/" + mContext.getPackageName() + "/cache");
				
				mSdImageDir = new File(Environment.getExternalStorageDirectory(),
						"/Android/data/" + mContext.getPackageName() + "/files/Pictures");
	        }
		}
		catch(Exception e)
		{
			if (App.LOGV)
				e.printStackTrace();
		}
		
		mThread = new Thread(this);
		mThread.start();
	}
	
	public synchronized static ImageCache createInstance(Application application)
	{
		if (inst == null)
			inst = new ImageCache(application);
		
		return inst;
	}
	
	public synchronized static ImageCache get()
	{
		return inst;
	}

	public boolean isBusy()
	{
		return mBusy;
	}

	public void run()
	{
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		
		while (true)
		{
			try
			{
				mCurrentTask = mTasks.take();
				if (mCurrentTask.alwaysRun 
						|| mListeners.contains(mCurrentTask.listener))
				{
					mBusy = true;
					
					// Load bitmap
					Bitmap bmp = loadBitmapSynchronous(mCurrentTask.imageUrl,
							mCurrentTask.cachePol);
					
					if (mCurrentTask.alwaysRun
							|| mListeners.contains(mCurrentTask.listener))
						mCurrentTask.listener.onImageReady(mCurrentTask.id,
								mCurrentTask.param, bmp);
					
					mCurrentTask = null;
				}
			}
			catch (Exception e)
			{
				if (App.LOGV)
				{
					App.logv("Error running task", e);
					e.printStackTrace();
				}
			}
			
			mBusy = false;
		}
	}
	
	public void addListener(OnImageReadyListener listener)
	{
		mListeners.add(listener);
	}
	
	public void removeListener(OnImageReadyListener listener)
	{
		mListeners.remove(listener);
	}
	
	private File getCacheFile(String imageUrl, CachePolicy cp)
	{
		if (imageUrl == null)
			return null;
		
		int protocolPos = imageUrl.indexOf("://");
		String filename = (protocolPos > -1) 
			? imageUrl.substring(protocolPos + 3) : imageUrl;
		
		if (cp != null && cp.resize())
			filename += cp.resizeWidth + "x" + cp.resizeHeight;
		
		return new File(getCacheDirectory(), filename.replaceAll("\\W", "_"));
	}
	
	public boolean isUsingExternalCache()
	{
		return (mSdCacheDir != null && Environment.MEDIA_MOUNTED
				.equals(Environment.getExternalStorageState()));
	}
	
	public File getCacheDirectory()
	{
		File cacheDir = mCacheDir;
		
		if (mSdCacheDir != null
				&& Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
		{
			if (mSdCacheDir.exists() || mSdCacheDir.mkdirs())
				cacheDir = mSdCacheDir;
		}
		
		return cacheDir;
	}
	
	public long getCacheSize()
	{
		long size = 0;
		
		try
		{
			File[] files = getCacheDirectory().listFiles();
			for (File file : files)
				size += file.length();
		}
		catch(Exception e)
		{
			if (App.LOGV)
				e.printStackTrace();
		}
		
		return size;
	}
	
	public void clearCache()
	{
		try
		{
			File cacheDir = getCacheDirectory();
			if (cacheDir.delete())
				cacheDir.mkdirs();
		}
		catch(Exception e)
		{
			if (App.LOGV)
				e.printStackTrace();
		}
	}
	
	private File getImageFile(String imageUrl)
	{
		if (imageUrl == null || mSdImageDir == null)
			return null;
		
		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
			return null;
		
		if (!mSdImageDir.exists() && !mSdImageDir.mkdirs())
			return null;
		
		return new File(mSdImageDir, Uri.parse(imageUrl).getLastPathSegment());
	}
	
	private Bitmap loadBitmapSynchronous(String imageUrl, CachePolicy cachePol)
	{
		return getBitmap(imageUrl, cachePol);
	}
	
	public Bitmap getBitmap(String imageUrl, 
			boolean bypassCache)
	{
		return getBitmap(imageUrl, bypassCache, -1, -1);
	}
	
	public Bitmap getBitmap(String imageUrl, CachePolicy cachePol)
	{
		if (imageUrl == null || imageUrl.length() < 1)
			return null;
		
		File file = getCacheFile(imageUrl, cachePol);
		
		// See if it's in the local cache 
		// (but only if not being forced to refresh)
		if (!cachePol.bypassCache && file.canRead())
		{
			if (App.LOGV)
				App.logv("Cache hit: " + file.getName());
			
			try
			{
				if (!cachePol.expired(System.currentTimeMillis(), file.lastModified()))
					return BitmapFactory.decodeFile(file.getAbsolutePath());
			}
			catch(OutOfMemoryError e)
			{
				return null;
			}
		}
		
		// Fetch the image
		byte[] blob;
		int length;
		
		try
		{
			HttpClient client = new DefaultHttpClient();
			
			HttpParams params = client.getParams();
			params.setParameter("http.useragent", 
					"Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0;)");
			
			HttpConnectionParams.setConnectionTimeout(params, TIMEOUT_MS);
			HttpConnectionParams.setSoTimeout(params, TIMEOUT_MS);
			
			HttpResponse resp = client.execute(new HttpGet(imageUrl));
			HttpEntity entity = resp.getEntity();
			
			if (entity == null)
				return null;
			
			InputStream stream = entity.getContent();
			if (stream == null)
				return null;
			
			try
			{
				if ((length = (int)entity.getContentLength()) <= 0)
				{
					// Length is negative, perhaps content length is not set
					ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
					
					int chunkSize = 5000;
					byte[] chunk = new byte[chunkSize];
					
					for (int r = 0; r >= 0; r = stream.read(chunk, 0, chunkSize))
						byteStream.write(chunk, 0, r);
					
					blob = byteStream.toByteArray();
				}
				else
				{
					// We know the length
					blob = new byte[length];
					
					// Read the stream until nothing more to read
					for (int r = 0; r < length; r += stream.read(blob, r, length - r));
				}
			}
			finally
			{
				stream.close();
				entity.consumeContent();
			}
		}
		catch(IOException e)
		{
			if (App.LOGV)
				e.printStackTrace();
			
			return null;
		}
		
		// if (file.canWrite())
		{
			FileOutputStream fos = null;
			
			try
			{
				fos = new FileOutputStream(file);
				
				if (cachePol.resizeWidth > 0 || cachePol.resizeHeight > 0)
				{
					Bitmap bmp = BitmapFactory.decodeByteArray(blob, 0, blob.length);
					float aspectRatio = (float)bmp.getWidth() / (float)bmp.getHeight();
					
					int newWidth = (cachePol.resizeWidth <= 0)
							? (int)((float)cachePol.resizeHeight * aspectRatio) : cachePol.resizeWidth;
					int newHeight = (cachePol.resizeHeight <= 0)  
							? (int)((float)cachePol.resizeWidth / aspectRatio) : cachePol.resizeHeight;
					
					Bitmap resized = Bitmap.createScaledBitmap(bmp, newWidth, newHeight, true);
					resized.compress(Bitmap.CompressFormat.PNG, 100, fos);
					
					return resized;
				}
				else
				{
					fos.write(blob);
				}
				
				if (App.LOGV)
					App.logv("Wrote to cache: " + file.getName());
			}
			catch (IOException e)
			{
				if (App.LOGV)
					e.printStackTrace();
			}
			finally
			{
				if (fos != null)
				{
					try
					{
						fos.close();
					}
					catch (IOException e)
					{
					}
				}
			}
		}
		
		Bitmap bmp = null;
		
		try
		{
			bmp = BitmapFactory.decodeByteArray(blob, 0, blob.length);
		}
		catch(Exception e)
		{
			if (App.LOGV)
				e.printStackTrace();
		}
		
		return bmp;
	}
	
	public Bitmap getBitmap(String imageUrl, 
			boolean bypassCache, int resizeH, int resizeV)
	{
		File file = getCacheFile(imageUrl, null);
		
		// See if it's in the local cache 
		// (but only if not being forced to refresh)
		if (!bypassCache && file.canRead())
		{
			if (App.LOGV)
				App.logv("Cache hit: " + file.getAbsolutePath());
			
			file.lastModified();
			
			try
			{
				return BitmapFactory.decodeFile(file.getAbsolutePath());
			}
			catch(OutOfMemoryError e)
			{
				return null;
			}
		}
		
		// Fetch the image
		byte[] blob;
		int length;
		
		try
		{
			HttpClient client = new DefaultHttpClient();
			HttpResponse resp = client.execute(new HttpGet(imageUrl));
			HttpEntity entity = resp.getEntity();
			
			if (entity == null)
				return null;
			
			InputStream stream = entity.getContent();
			if (stream == null)
				return null;
			
			try
			{
				if ((length = (int)entity.getContentLength()) <= 0)
					return null;
				
				blob = new byte[length];
				
				// Read the stream until nothing more to read
				for (int r = 0; r < length; r += stream.read(blob, r, length - r));
			}
			finally
			{
				stream.close();
				entity.consumeContent();
			}
		}
		catch(IOException e)
		{
			if (App.LOGV)
				e.printStackTrace();
			
			return null;
		}
		
		// if (file.canWrite())
		{
			FileOutputStream fos = null;
			
			try
			{
				fos = new FileOutputStream(file);
				
				if (resizeH > -1 || resizeV > -1)
				{
					Bitmap bmp = BitmapFactory.decodeByteArray(blob, 0, length);
					float aspectRatio = (float)bmp.getWidth() / (float)bmp.getHeight();
					
					int newWidth = (resizeH < 0)
							? (int)((float)resizeV * aspectRatio) : resizeH;
					int newHeight = (resizeV < 0)  
							? (int)((float)resizeH / aspectRatio) : resizeV;
					
					Bitmap resized = Bitmap.createScaledBitmap(bmp, newWidth, newHeight, true);
					resized.compress(Bitmap.CompressFormat.PNG, 100, fos);
					
					return resized;
				}
				else
				{
					fos.write(blob);
				}
				
				if (App.LOGV)
					App.logv("Wrote to cache: " + file.getAbsolutePath());
			}
			catch (IOException e)
			{
				if (App.LOGV)
					e.printStackTrace();
			}
			finally
			{
				if (fos != null)
				{
					try
					{
						fos.close();
					}
					catch (IOException e)
					{
					}
				}
			}
		}
		
		Bitmap bmp = null;
		
		try
		{
			bmp = BitmapFactory.decodeByteArray(blob, 0, length);
		}
		catch(Exception e)
		{
			if (App.LOGV)
				e.printStackTrace();
		}
		
		return bmp;
	}
	
	public File downloadImage(String imageUrl)
	{
		if (imageUrl == null || imageUrl.length() < 1)
			return null;
		
		File file = getImageFile(imageUrl);
		if (file == null)
			return null;
		
		// Fetch the image
		byte[] blob;
		int length;
		
		try
		{
			HttpClient client = new DefaultHttpClient();
			HttpResponse resp = client.execute(new HttpGet(imageUrl));
			HttpEntity entity = resp.getEntity();
			
			if (entity == null)
				return null;
			
			InputStream stream = entity.getContent();
			if (stream == null)
				return null;
			
			try
			{
				if ((length = (int)entity.getContentLength()) <= 0)
					return null;
				
				blob = new byte[length];
				
				// Read the stream until nothing more to read
				for (int r = 0; r < length; r += stream.read(blob, r, length - r));
			}
			finally
			{
				stream.close();
				entity.consumeContent();
			}
		}
		catch(IOException e)
		{
			if (App.LOGV)
				e.printStackTrace();
			
			return null;
		}
		
		// if (file.canWrite())
		{
			FileOutputStream fos = null;
			
			try
			{
				fos = new FileOutputStream(file);
				fos.write(blob);
				
				if (App.LOGV)
					App.logv("Wrote to cache: " + file.getName());
			}
			catch (IOException e)
			{
				if (App.LOGV)
					e.printStackTrace();
			}
			finally
			{
				if (fos != null)
				{
					try
					{
						fos.close();
					}
					catch (IOException e)
					{
					}
				}
			}
		}
		
		final String filename = file + "";
	    MediaScannerConnectionClient mscc =
	        new MediaScannerConnectionClient() 
	    {
			public void onMediaScannerConnected()
			{
				mMsc.scanFile(filename, null);
			}
			
			public void onScanCompleted(String path, Uri uri)
			{
				if (path.equals(filename))
					mMsc.disconnect();
			}
	    };
	    
		try
		{
		    mMsc = new MediaScannerConnection(mContext, mscc);
		    mMsc.connect();
		}
		catch(Exception ex)
		{
			if (App.LOGV)
				ex.printStackTrace();
		}
	    
	    return file;
	}
	
	public void clearCachedBitmap(String imageUrl)
	{
		clearCachedBitmap(imageUrl, null);
	}
	
	public void clearCachedBitmap(String imageUrl, CachePolicy cachePol)
	{
		File file = getCacheFile(imageUrl, cachePol);
		
		if (file.canWrite())
		{
			if (App.LOGV)
				App.logv("Purging %s from cache", imageUrl);
			
			file.delete();
		}
	}
	
	public Bitmap getCachedOrRequest(String imageUrl, 
			OnImageReadyListener listener, int id, Object param, CachePolicy cp)
	{
		Bitmap bmp = getCachedBitmap(imageUrl, null);
		if (bmp != null)
			return bmp;
		
		requestImage(imageUrl, listener, 0, param, cp);
		return null;
	}
	
	public Bitmap getCachedBitmap(String imageUrl)
	{
		return getCachedBitmap(imageUrl, null);
	}
	
	public boolean isExpired(String imageUrl, CachePolicy cachePol)
	{
		File file = getCacheFile(imageUrl, cachePol);
		
		// Return 'true' if we don't have a cached copy
		if (!file.canRead())
			return true;
		
		return cachePol.expired(System.currentTimeMillis(), file.lastModified());
	}
	
	public Bitmap getCachedBitmap(String imageUrl, CachePolicy cachePol)
	{
		if (imageUrl == null || imageUrl.length() < 1)
			return null;
		
		// Returns the cached image, or NULL if the image is not yet cached
		File file = getCacheFile(imageUrl, cachePol);
		
		// Return NULL if we don't have a cached copy
		if (!file.canRead())
			return null;
		
		if (cachePol != null && cachePol.expired(System.currentTimeMillis(), file.lastModified()))
			return null;
		
		try
		{
			return BitmapFactory.decodeFile(file.getAbsolutePath());
		}
		catch(OutOfMemoryError e)
		{
			return null;
		}
		catch(Exception e)
		{
			if (App.LOGV)
			{
				App.logv("error decoding %s", file.getAbsolutePath());
				e.printStackTrace();
			}
			
			return null;
		}
	}
	
	public boolean isCached(String imageUrl, CachePolicy cachePol)
	{
		if (imageUrl == null || imageUrl.length() < 1)
			return false;
		
		File file = getCacheFile(imageUrl, cachePol);
		
		// Return NULL if we don't have a cached copy
		if (!file.canRead())
			return false;
		
		return file.exists();
	}
	
	public boolean requestImage(String imageUrl,
			OnImageReadyListener listener, long id, Object param, boolean bypassCache)
	{
		return requestImage(imageUrl, listener, id, param, false, 
				new CachePolicy(bypassCache));
	}
	
	public boolean requestImage(String imageUrl,
			OnImageReadyListener listener, long id, Object param)
	{
		return requestImage(imageUrl, listener, id, param, false, new CachePolicy());
	}
	
	public boolean requestImage(String imageUrl,
			OnImageReadyListener listener, long id, Object param,
			CachePolicy cachePol)
	{
		return requestImage(imageUrl, listener, id, param, false, 
				cachePol);
	}
	
	public synchronized boolean requestImage(String imageUrl,
			OnImageReadyListener listener, long id, Object param,
			boolean alwaysRun, CachePolicy cachePol)
	{
		if (imageUrl == null || imageUrl.length() < 1)
			return false;
		
		// Check for duplicate tasks
		if (mCurrentTask != null && mCurrentTask.imageUrl.equals(imageUrl)
				&& mCurrentTask.listener.equals(listener))
		{
			if (App.LOGV)
				App.logv("Image '" + imageUrl + "' already in queue");
			
			return false;
		}
		
		for (Task command : mTasks)
		{
			if (command.imageUrl.equals(imageUrl)
					&& command.listener.equals(listener))
			{
				if (App.LOGV)
					App.logv("Image '" + imageUrl + "' already in queue");
				
				return false;
			}
		}
		
		if (App.LOGV)
			App.logv("Image requested: " + imageUrl);
		
		// Add task
		try
		{
			mTasks.put(new Task(imageUrl, id, param, listener, 
					alwaysRun, cachePol));
		}
		catch (InterruptedException ie)
		{
			throw new Error(ie);
		}
		
		return true;
	}
}
