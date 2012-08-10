/*
 * GameOverview.java 
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

package com.akop.bach.activity.xboxlive;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;

import com.akop.bach.IAccount;
import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.CachePolicy;
import com.akop.bach.ImageCache.OnImageReadyListener;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.CustomTask;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.XboxLive.GameOverviewInfo;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.parser.AuthenticationException;
import com.akop.bach.parser.ParserException;
import com.akop.bach.parser.XboxLiveParser;

public class GameOverview
		extends RibbonedActivity
		implements OnImageReadyListener
{
	protected class ServerStatusHandler extends Handler
	{
		private class ImagePair
		{
			public ImagePair(ImageView imageView, Bitmap bitmap)
			{
				this.imageView = imageView;
				this.bitmap = bitmap;
			}
			
			public ImageView imageView;
			public Bitmap bitmap;
		}
		
		private static final int MSG_REFRESH = 1000;
		private static final int MSG_SET_BMP = 1001;
		private static final int MSG_REFRESH_SCREENSHOTS = 1002;
		
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case MSG_SET_BMP:
				if (!isFinishing())
				{
					ImagePair pair = (ImagePair)msg.obj;
					pair.imageView.setImageBitmap(pair.bitmap);
				}
				break;
			case MSG_REFRESH:
				if (!isFinishing())
				{
					if (msg.obj != null)
						mData = (GameOverviewInfo)msg.obj;
					
					update();
				}
				break;
			case MSG_REFRESH_SCREENSHOTS:
				if (!isFinishing() && mScreenshotAdapter != null)
					mScreenshotAdapter.notifyDataSetChanged();
				break;
			}
		}
		
		public void setImageView(ImageView iv, Bitmap bmp)
		{
			sendMessage(Message.obtain(this, MSG_SET_BMP,
					new ImagePair(iv, bmp)));
		}
		
		public void updateStatus(Object obj)
		{
			sendMessage(Message.obtain(this, MSG_REFRESH, obj));
		}
		
		public void refresh()
		{
			sendMessage(Message.obtain(this, MSG_REFRESH, null));
		}
		
		public void refreshScreenshots()
		{
			sendMessage(Message.obtain(this, MSG_REFRESH_SCREENSHOTS, null));
		}
	}
	
	private Map<String, SoftReference<Bitmap>> mIconCache;
	private ServerStatusHandler mLocalHandler = new ServerStatusHandler();
    private CachePolicy mScreenshotPolicy;
    
	private TaskListener mListener = new TaskListener()
	{
		@Override
		public void onTaskSucceeded(IAccount account, Object requestParam,
				Object result)
		{
			mLocalHandler.updateStatus(result);
		}
		
		public void onTaskFailed(IAccount account, Exception e)
		{
			mHandler.showError(e);
		}
		
		@Override
		public void onAllTasksCompleted()
		{
			mHandler.showThrobber(false);
		}
		
		@Override
		public void onTaskStarted()
		{
			mHandler.showThrobber(true);
		}
	};
	
	private ProgressDialog mProgDlg;
	private GameOverviewInfo mData;
	private String mGameUrl;
	private ImageAdapter mScreenshotAdapter;
	
	private int mScaledScreenshotWidth;
	private int mScaledScreenshotHeight;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.xbl_game_overview);
		
		float scale = getResources().getDisplayMetrics().density;
		mScaledScreenshotWidth = (int)(scale * 178f + 0.5f);
		mScaledScreenshotHeight = (int)(scale * 100f + 0.5f);
		
		mProgDlg = null;
		mScreenshotPolicy = new CachePolicy(mScaledScreenshotWidth,
				mScaledScreenshotHeight);
		
		mIconCache = new HashMap<String, SoftReference<Bitmap>>();
		mGameUrl = getIntent().getStringExtra("url");
		mData = null;
		
		if (savedInstanceState != null)
		{
			if (savedInstanceState.containsKey("data"))
				mData = (GameOverviewInfo)savedInstanceState
						.getSerializable("data");
		}
		
		if (mData != null)
			update();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		if (mData != null)
			outState.putSerializable("data", mData);
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		
		TaskController.getInstance().removeListener(mListener);
		ImageCache.getInstance().removeListener(this);
		ImageCache.getInstance().removeListener(mGalleryListener);
		
		if (mProgDlg != null)
		{
			try
			{
				mProgDlg.dismiss();
			}
			catch(Exception ex)
			{
			}
		}
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		
		TaskController.getInstance().addListener(mListener);
		ImageCache.getInstance().addListener(this);
		ImageCache.getInstance().addListener(mGalleryListener);
		
        mHandler.showThrobber(TaskController.getInstance().isBusy());
		
		if (mData == null)
			loadGameOverview(mGameUrl);
	}
	
	public static void actionShow(Context context, XboxLiveAccount account, String gameUrl)
	{
		Intent intent = new Intent(context, GameOverview.class);
		intent.putExtra("account", account);
		intent.putExtra("url", gameUrl);
		context.startActivity(intent);
	}
	
	private void update()
	{
		if (mData == null)
			return;
		
		updateRibbon();
		
		TextView tv;
		ImageView iv;
		
		tv = (TextView)findViewById(R.id.game_title);
		tv.setText(mData.Title);
		tv = (TextView)findViewById(R.id.game_description);
		tv.setText(mData.Description);
		tv = (TextView)findViewById(R.id.game_esrb_rating);
		tv.setText(mData.EsrbRatingDescription);
		
		ImageCache ic = ImageCache.getInstance();
		Bitmap bmp;
		
		if (mData.EsrbRatingIconUrl != null)
		{
			iv = (ImageView)findViewById(R.id.game_esrb_icon);
			if ((bmp = ic.getCachedBitmap(mData.EsrbRatingIconUrl)) != null)
				iv.setImageBitmap(bmp);
			else
				ic.requestImage(mData.EsrbRatingIconUrl, this, 0, iv, sCp);
		}
		
		if (mData.BannerUrl != null)
		{
			iv = (ImageView)findViewById(R.id.game_banner);
			if ((bmp = ic.getCachedBitmap(mData.BannerUrl)) != null)
				iv.setImageBitmap(bmp);
			else
				ic.requestImage(mData.BannerUrl, this, 0, iv, sCp);
		}
		
	    Gallery g = (Gallery)findViewById(R.id.game_image_gallery);
		g.setOnItemClickListener(new OnItemClickListener() 
		{
			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id)
			{
				if (mData != null && position < mData.Screenshots.size())
				{
					final String url = mData.Screenshots.get(position);
					if (url != null)
					{
						mProgDlg = new ProgressDialog(GameOverview.this);
						
						mProgDlg.setTitle(R.string.downloading);
						mProgDlg.setMessage(getString(R.string.please_wait));
						mProgDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
						mProgDlg.setCancelable(false);
						
						Thread downloadThread = new Thread(new Runnable()
						{
							@Override
							public void run()
							{
								File file;
								
								try
								{
									file = ImageCache.getInstance().downloadImage(url);
								}
								catch(Exception ex)
								{
									mLocalHandler.post(new Runnable() 
									{
										@Override
										public void run()
										{
											mProgDlg.hide();
										}
									});
									
									mHandler.showError(ex);
									return;
								}
								
								if (file != null)
								{
									Intent intent = new Intent(Intent.ACTION_VIEW);
									intent.setDataAndType(Uri.fromFile(file), "image/*");
									startActivity(intent);
									
									if (mProgDlg.isShowing())
									{
										mLocalHandler.post(new Runnable() 
										{
											@Override
											public void run()
											{
												mProgDlg.hide();
											}
										});
									}
								}
							}
						});
						
						downloadThread.start();
						
						mProgDlg.show();
					}
				}
			}
		});
	    
	    mScreenshotAdapter = new ImageAdapter(this);
	    g.setAdapter(mScreenshotAdapter);
	}
	
	private void loadGameOverview(final String url)
	{
		TaskController.getInstance().runCustomTask(null, new CustomTask<GameOverviewInfo>()
				{
					@Override
					public void runTask() throws AuthenticationException,
							IOException, ParserException
					{
						XboxLiveParser p = new XboxLiveParser(GameOverview.this);
						
						try
						{
							setResult(p.fetchGameOverview(mAccount, url));
						}
						finally
						{
							p.dispose();
						}
					}
				}, mListener);
	}
	
	@Override
	protected void updateRibbon()
	{
		if (mAccount != null)
		{
			updateRibbon(mAccount.getGamertag(), mAccount.getIconUrl(),
					(mData != null) ? mData.Title : "");
		}
	}
	
	@Override
	public void onImageReady(long id, Object param, Bitmap bmp)
	{
		mLocalHandler.setImageView((ImageView)param, bmp);
	}
	
	OnImageReadyListener mGalleryListener = new OnImageReadyListener() 
	{
		@Override
		public void onImageReady(long id, Object param, Bitmap bmp)
		{
			mIconCache.put((String)param, new SoftReference<Bitmap>(bmp));
			mLocalHandler.refreshScreenshots();
		}
	};
	
	public class ImageAdapter
			extends BaseAdapter
	{
		int mGalleryItemBackground;
		private Context mContext;
        
		public ImageAdapter(Context c)
		{
			mContext = c;
			
			TypedArray a = obtainStyledAttributes(R.styleable.ScreenshotGallery);
			mGalleryItemBackground = a.getResourceId(
					R.styleable.ScreenshotGallery_android_galleryItemBackground, 0);
			a.recycle();
		}
		
		public int getCount()
		{
			if (mData == null)
				return 0;
			
			return mData.Screenshots.size();
		}
		
		public Object getItem(int position)
		{
			return position;
		}
		
		public long getItemId(int position)
		{
			return position;
		}
		
		public View getView(int position, View convertView, ViewGroup parent)
		{
	        ImageView i = new ImageView(mContext);
	        
	        String url = mData.Screenshots.get(position);
        	Bitmap bmp = null;
        	
	        if (url != null)
	        {
	        	ImageCache ic = ImageCache.getInstance();
	        	SoftReference<Bitmap> cachedBmp = mIconCache.get(url);
	        	
	        	if (cachedBmp == null || (bmp = cachedBmp.get()) == null)
	        	{
	        		if ((bmp = ic.getCachedBitmap(url)) != null)
	        		{
	        			mIconCache.put(url, new SoftReference<Bitmap>(bmp));
	        		}
	        		else
	        		{
						ic.requestImage(url, mGalleryListener, 0, url,
								mScreenshotPolicy);
	        		}
	        	}
	        }
	        
	        i.setImageBitmap(bmp);
	        i.setLayoutParams(new Gallery.LayoutParams(mScaledScreenshotWidth,
					mScaledScreenshotHeight));
	        i.setScaleType(ImageView.ScaleType.FIT_XY);
	        i.setBackgroundResource(mGalleryItemBackground);
	        
	        return i;
		}
	}
}
