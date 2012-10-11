/*
 * RibbonedScrollingActivity.java 
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

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.akop.bach.App;
import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.CachePolicy;
import com.akop.bach.ImageCache.OnImageReadyListener;
import com.akop.bach.Preferences;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.activity.ScrollingActivity;
import com.akop.bach.parser.Parser;

public abstract class RibbonedScrollingActivity
		extends ScrollingActivity
{
	private static final int DIALOG_ERROR = 1000;
	protected static CachePolicy sCp = 
		new CachePolicy(CachePolicy.SECONDS_IN_HOUR * 4);
	
	protected XboxLiveAccount mAccount;
	protected TextView mNoRecords;
	protected XboxLiveHandler mHandler = new XboxLiveHandler();
	private AlertDialog mAlert;
	
	private OnImageReadyListener mRibbonImageListener = new OnImageReadyListener()
	{
		@Override
		public void onImageReady(long id, Object param, Bitmap bmp)
		{
			mHandler.updateRibbonAvatar(bmp);
		}
	};
	
	protected class XboxLiveHandler extends Handler
	{
		private static final int MSG_ERROR = 1000;
		private static final int MSG_LOADING = 1001;
		private static final int MSG_UPDATE_AVATAR = 1002;
		
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case MSG_ERROR:
				if (!isFinishing())
				{
					showDialog(DIALOG_ERROR);
					
					if (mAlert != null)
						mAlert.setMessage((String)msg.obj);
					
					if (mNoRecords != null)
						mNoRecords.setText(R.string.error_unexpected);
				}
				break;
			case MSG_LOADING:
				if (msg.obj != null && mNoRecords != null)
					mNoRecords.setText((String)msg.obj);
				break;
			case MSG_UPDATE_AVATAR:
				{
					ImageView iv = (ImageView)findViewById(R.id.title_icon);
					if (iv != null)
						iv.setImageBitmap((Bitmap)msg.obj);
				}
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
		
		public void showError(Exception ex)
		{
			Message m = Message.obtain(this, MSG_ERROR, 
					Parser.getErrorMessage(RibbonedScrollingActivity.this, ex));
			sendMessage(m);
			
			if (App.getConfig().logToConsole())
				ex.printStackTrace();
		}
		
		public void setLoadText(String text)
		{
			sendMessage(Message.obtain(this, MSG_LOADING, text));
		}
		
		public void updateRibbonAvatar(Bitmap bmp)
		{
			Message msg = Message.obtain(this, MSG_UPDATE_AVATAR, bmp); 
			sendMessage(msg);
		}
	}
	
	@TargetApi(11)
	class ActionBarHelper
	{
		public void init()
		{
			getActionBar().setCustomView(R.layout.xbl_actionbar_custom);
		}
		
		public void setSubtitle(String subtitle)
		{
			getActionBar().setSubtitle(subtitle);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
        if ((mAccount = (XboxLiveAccount)getIntent().getParcelableExtra("account")) == null)
        {
        	finish();
        	return;
        }
        
		super.onCreate(savedInstanceState);
		
        if (android.os.Build.VERSION.SDK_INT >= 11)
        {
        	new ActionBarHelper().init();
        }
	}
	
	@Override
	protected void onDataUpdate()
	{
		super.onDataUpdate();
		updateRibbon();
	}
	
	protected void updateRibbon()
	{
		ImageButton button = (ImageButton)findViewById(R.id.title_icon);
		button.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (mAccount != null)
					mAccount.open(RibbonedScrollingActivity.this);
			}
		});
	}
	
	protected void setRibbonTitles(String title, String subtitle)
	{
        TextView tv;
        
        tv = (TextView)findViewById(R.id.title_gamertag);
        tv.setText(title);
        setTitle(title);
        
        tv = (TextView)findViewById(R.id.ribbon_line_1);
        tv.setText(subtitle);
        
        if (android.os.Build.VERSION.SDK_INT >= 11)
        {
        	new ActionBarHelper().setSubtitle(subtitle);
        }
	}
	
	protected void updateRibbon(String title, String iconUrl, 
			String subtitle)
	{
        if (iconUrl != null)
        {
        	ImageCache ic = ImageCache.getInstance();
        	Bitmap bmp = null;
        	
        	if ((bmp = ic.getCachedBitmap(iconUrl)) != null)
        		mHandler.updateRibbonAvatar(bmp);
        	
        	if (ic.isExpired(iconUrl, sCp))
                ic.requestImage(iconUrl, mRibbonImageListener, 0, null, sCp);
        }
        
        setRibbonTitles(title, subtitle);
	}
	
	@Override
	protected void initializeWindowFeatures()
	{
		super.initializeWindowFeatures();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case android.R.id.home:
			if (mAccount != null)
				mAccount.open(RibbonedScrollingActivity.this);
			
			return true;
		}

		return false;
	}
	
	@Override
	protected Dialog onCreateDialog(int id)
	{
		switch (id)
		{
		case DIALOG_ERROR:
			Builder builder = new Builder(this);
			builder.setTitle(R.string.error)
				.setNeutralButton(android.R.string.ok, new OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which) {}
				})
				.setMessage(R.string.error_unexpected);
			
			return mAlert = builder.create();
		}
		
		return super.onCreateDialog(id);
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		
        ImageCache.getInstance().removeListener(mRibbonImageListener);
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
        
        ImageCache.getInstance().addListener(mRibbonImageListener);
		mUpdater.showThrobber(TaskController.getInstance().isBusy());
        mAccount.refresh(Preferences.get(this));
        
        updateRibbon();
	}
	
	@Override
	protected void toggleProgressBar(boolean show)
	{
		ProgressBar bar = (ProgressBar)findViewById(R.id.ribbon_progress_bar);
		if (bar != null)
			bar.setVisibility(show ? View.VISIBLE : View.GONE);
		
		setProgressBarIndeterminateVisibility(show);
	}
}
