/*
 * RibbonedActivity.java 
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
import android.app.Activity;
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
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.akop.bach.App;
import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.CachePolicy;
import com.akop.bach.ImageCache.OnImageReadyListener;
import com.akop.bach.Preferences;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.parser.Parser;

public abstract class RibbonedActivity
		extends Activity
{
	private static final int DIALOG_ERROR = 1000;
	protected static CachePolicy sCp = 
		new CachePolicy(CachePolicy.SECONDS_IN_HOUR * 4);
	
	protected XboxLiveAccount mAccount;
	protected TextView mNoRecords;
	protected XboxLiveHandler mHandler = new XboxLiveHandler();
	private AlertDialog mAlert;
	
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
		private static final int MSG_SHOW_THROBBER = 1003;
		private static final int MSG_REFRESH = 1004;
		private static final int MSG_SHOW_TOAST = 1005;
		
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
			case MSG_SHOW_THROBBER:
				toggleProgressBar(msg.arg1 != 0);
				break;
			case MSG_SHOW_TOAST:
		        Toast.makeText(getApplicationContext(),
		        		(String)msg.obj, Toast.LENGTH_LONG).show();
				break;
			case MSG_REFRESH:
				RibbonedActivity.this.onRefresh();
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
		
		public void showError(Exception ex)
		{
			Message m = Message.obtain(this, MSG_ERROR, 
					Parser.getErrorMessage(RibbonedActivity.this, ex));
			sendMessage(m);
			
			if (App.LOGV)
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
		
		public void showThrobber(boolean on)
		{
			Message msg = Message.obtain(this, MSG_SHOW_THROBBER, on ? 1 : 0, 0); 
			sendMessage(msg);
		}
		
		public void showToast(String message)
		{
			Message msg = Message.obtain(this, MSG_SHOW_TOAST, 0, 0);
			msg.obj = message;
			sendMessage(msg);
		}
		
		public void showToast(int resId)
		{
			Message msg = Message.obtain(this, MSG_SHOW_TOAST, 0, 0);
			msg.obj = getString(resId);
			sendMessage(msg);
		}
		
		public void refresh()
		{
			Message msg = Message.obtain(this, MSG_REFRESH); 
			sendMessage(msg);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		initializeWindowFeatures();
		
		toggleProgressBar(false);
		
		if (mAccount == null)
			mAccount = (XboxLiveAccount)getIntent().getParcelableExtra("account");
		
        if (mAccount == null)
        {
			if (App.LOGV)
				App.logv("Account is null");
			
        	finish();
        	return;
        }
        
        if (android.os.Build.VERSION.SDK_INT >= 11)
        {
        	new ActionBarHelper().init();
        }
	}
	
	protected void setRibbonTitles(String title, String subtitle)
	{
        TextView tv;
        
        if ((tv = (TextView)findViewById(R.id.title_gamertag)) != null)
        	tv.setText(title);
        
        setTitle(title);
        
        if ((tv = (TextView)findViewById(R.id.ribbon_line_1)) != null)
        	tv.setText(subtitle);
        
        if (android.os.Build.VERSION.SDK_INT >= 11)
        {
        	new ActionBarHelper().setSubtitle(subtitle);
        }
	}
	
	protected abstract void updateRibbon();
	
	protected void toggleProgressBar(boolean show)
	{
		ProgressBar bar = (ProgressBar)findViewById(R.id.ribbon_progress_bar);
		if (bar != null)
			bar.setVisibility(show ? View.VISIBLE : View.GONE);
		
		setProgressBarIndeterminateVisibility(show);
	}
	
	protected void updateRibbon(String title, String iconUrl, 
			String subtitle)
	{
		View view = findViewById(R.id.title_icon);
		if (view instanceof ImageButton)
		{
			ImageButton button = (ImageButton)view;
			button.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (mAccount != null)
						mAccount.open(RibbonedActivity.this);
				}
			});
		}
		
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
	
	protected void initializeWindowFeatures()
	{
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	}
	
	protected void onErrorDialogOk()
	{
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
					public void onClick(DialogInterface dialog, int which) 
					{
						onErrorDialogOk();
					}
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
        
		mHandler.showThrobber(TaskController.getInstance().isBusy());
        mAccount.refresh(Preferences.get(this));
        ImageCache.getInstance().addListener(mRibbonImageListener);
        
        updateRibbon();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case android.R.id.home:
			if (mAccount != null)
				mAccount.open(this);
			
			return true;
		}

		return false;
	}
	
	protected void onRefresh()
	{
	}
}
