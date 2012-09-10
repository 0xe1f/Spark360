/*
 * GenericFragment.java 
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

package com.akop.bach.fragment;

import java.lang.ref.SoftReference;
import java.util.HashMap;

import android.annotation.TargetApi;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.akop.bach.App;
import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.CachePolicy;
import com.akop.bach.ImageCache.OnImageReadyListener;
import com.akop.bach.R;
import com.akop.bach.SerializableMatrixCursor;

public abstract class GenericFragment extends Fragment implements
        OnImageReadyListener
{
	protected static GenericHandler mHandler = new GenericHandler();
	protected static class GenericHandler extends Handler
	{
		public void showToast(final String message)
		{
			this.post(new Runnable()
			{
				@Override
				public void run()
				{
					Toast.makeText(App.getInstance(), message, 
							Toast.LENGTH_LONG).show();
				}
			});
		}
	}
	
	@TargetApi(11)
    protected class HoneyCombHelper
	{
		public HoneyCombHelper()
		{
		}
		
		public void invalidateMenu()
		{
			getActivity().invalidateOptionsMenu();
		}
	}
	
	protected static CachePolicy sCp = 
		new CachePolicy(CachePolicy.SECONDS_IN_HOUR * 4);
	
	protected static class IconCursor extends SerializableMatrixCursor
	{
        private static final long serialVersionUID = 7926862283398567298L;
        
		public IconCursor()
        {
			super(new String[] { "_ID", "Icon" });
        }
	}
	
	protected static class IconCursor2 extends SerializableMatrixCursor
	{
        private static final long serialVersionUID = 6067436551004489292L;
        
		public IconCursor2()
        {
			super(new String[] { "_ID", "Icon1", "Icon2" });
        }
	}
	
	protected CachePolicy getCachePolicy()
	{
		return new CachePolicy();
	}
	
	private class IconTask extends AsyncTask<Void, Void, Void>
	{
		@Override
        protected Void doInBackground(Void... params)
        {
			int[] cursorIconColumns = getIconColumns();
			Cursor cursor = getIconCursor();
			
			if (cursor == null)
				return null;
			
			CachePolicy cp = getCachePolicy();
			ImageCache ic = ImageCache.getInstance();
			
			try
			{
				int n = -1;
				if (cursor.moveToFirst())
				{
					if (cursorIconColumns == null)
					{
						n = cursor.getColumnCount();
						cursorIconColumns = new int[n];
						
						for (int i = 0; i < n; i++)
							cursorIconColumns[i] = i;
					}
					else
					{
						n = cursorIconColumns.length;
					}
					
					do
					{
						if (isCancelled())
							break;
						
						long id = cursor.getLong(cursorIconColumns[0]);
						for (int i = 1; i < n; i++)
						{
							String iconUrl = (String)cursor.getString(cursorIconColumns[i]);
							SoftReference<Bitmap> cachedIcon = mIconCache.get(iconUrl);
							
							// Is it in the in-memory cache?
							if (cachedIcon == null || cachedIcon.get() == null)
							{
								Bitmap bmp;
								
								// TODO: LAME
								if ("http://tiles.xbox.com/consoleAssets/FFED0000/en-US/smallboxart.jpg".equalsIgnoreCase(iconUrl))
								{
									// Xbox.com boxart
									
									bmp = BitmapFactory.decodeResource(getResources(), 
											R.drawable.xbox_xboxdotcom);
									GenericFragment.this.onImageReady(id, iconUrl, bmp);
									
									continue;
								}
								else
								{
									bmp = ic.getCachedBitmap(iconUrl);
									
									// It's not in the in-memory cache; is it
									// in the disk cache?
									if (bmp == null)
									{
										ic.requestImage(iconUrl, GenericFragment.this, 
												id, iconUrl, false, cp);
									}
								}
							}
						}
					} while (cursor.moveToNext());
				}
			}
			catch (Exception e)
			{
				if (App.LOGV)
					e.printStackTrace();
			}
			finally
			{
				if (closeIconCursor())
					cursor.close();
			}
			
	        return null;
        }
	};
	
	private IconTask mIconTask;
	protected HashMap<String, SoftReference<Bitmap>> mIconCache;
	protected boolean mDualPane;
	
	public GenericFragment()
	{
	    mIconCache = new HashMap<String, SoftReference<Bitmap>>();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
	    super.onCreate(savedInstanceState);
	    
	    mIconCache.clear();
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
	    super.onActivityCreated(savedInstanceState);
	    
		mDualPane = (getActivity().findViewById(R.id.fragment_details) != null);
	}
	
	@Override
	public void onPause()
	{
	    super.onPause();
	    
		ImageCache.getInstance().removeListener(this);
		if (mIconTask != null)
			mIconTask.cancel(false);
	}
	
	@Override
	public void onResume()
	{
	    super.onResume();
	    
		ImageCache.getInstance().addListener(this);
		
		syncIcons();
	}
	
	@Override
	public void onImageReady(long id, Object param, Bitmap bmp)
	{
		String iconUrl = (String)param;
		mIconCache.put(iconUrl, new SoftReference<Bitmap>(bmp));
	}
	
	protected void syncIcons()
	{
		if (mIconTask != null)
			mIconTask.cancel(false);
		
		mIconCache.clear();
		
		mIconTask = new IconTask();
		mIconTask.execute();
	}
	
	protected long getFirstTitleId(Cursor cursor)
	{
		if (cursor == null)
			return -1;
		
		try
		{
			if (cursor.moveToNext())
				return cursor.getLong(0);
		}
		catch (Exception e)
		{
			if (App.LOGV)
				e.printStackTrace();
		}
		finally
		{
			cursor.close();
		}
		
		return -1;
	}
	
	protected Cursor getIconCursor()
	{
		return null;
	}
	
	protected int[] getIconColumns()
	{
		return null;
	}
	
	protected boolean closeIconCursor()
	{
		return true;
	}
}
