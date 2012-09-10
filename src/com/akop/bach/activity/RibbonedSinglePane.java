/*
 * RibbonedSinglePaneActivity.java 
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

package com.akop.bach.activity;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.akop.bach.App;
import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.CachePolicy;
import com.akop.bach.ImageCache.OnImageReadyListener;
import com.akop.bach.BasicAccount;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.fragment.ErrorDialogFragment;
import com.akop.bach.parser.Parser;

public abstract class RibbonedSinglePane extends FragmentActivity
{
	protected BasicAccount mAccount;
	protected MyHandler mHandler = new MyHandler();
	protected static CachePolicy sCp = new CachePolicy(CachePolicy.SECONDS_IN_HOUR * 4);
	
	protected class MyHandler extends Handler
	{
		public void updateAvatar(final Bitmap bmp)
		{
			this.post(new Runnable()
			{
				@Override
				public void run()
				{
					ImageView iv = (ImageView)findViewById(R.id.title_icon);
					if (iv != null)
						iv.setImageBitmap(bmp);
				}
			});
		}
	};
	
	private OnImageReadyListener mRibbonImageListener = new OnImageReadyListener()
	{
		@Override
		public void onImageReady(long id, Object param, Bitmap bmp)
		{
			mHandler.updateAvatar(bmp);
		}
	};
	
	private TaskListener mListener = new TaskListener()
	{
		public void onControllerBusy()
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					toggleProgressBar(true);
				}
			});
		}
		
		public void onAllTasksCompleted()
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					toggleProgressBar(false);
				}
			});
		}
		
		public void onAnyTaskFailed(int notified, Exception e) 
		{
			String message = Parser.getErrorMessage(RibbonedSinglePane.this, e);
			
			ErrorDialogFragment frag = ErrorDialogFragment.newInstance(message, e);
			frag.show(getSupportFragmentManager(), "errorDialog");
		}
	};
	
	@TargetApi(11)
    class ActionBarHelper
	{
		public void init()
		{
			getActionBar().setCustomView(getActionBarLayout());
		}
		
		public void setSubtitle(String subtitle)
		{
			getActionBar().setSubtitle(subtitle);
		}
	}
	
	protected abstract String getSubtitle();
	
	protected abstract Fragment createFragment();
	
	protected abstract int getLayout();
	
	protected abstract int getActionBarLayout();
	
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(getLayout());
		
		if (mAccount == null)
			mAccount = (BasicAccount)getIntent().getSerializableExtra("account");
		
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
        
		FragmentManager fm = getSupportFragmentManager();
		Fragment titleFrag;
		
		FragmentTransaction ft = fm.beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        
		if ((titleFrag = fm.findFragmentByTag("details")) == null)
		{
			titleFrag = createFragment();
			ft.replace(R.id.fragment_titles, titleFrag, "details");
		}
		
		ft.commit();
	}
	
	protected void updateRibbon()
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
						mAccount.open(RibbonedSinglePane.this);
				}
			});
		}
		
    	Bitmap bmp = null;
        String iconUrl = mAccount.getIconUrl();
        
        if (iconUrl != null)
        {
        	ImageCache ic = ImageCache.getInstance();
        	if ((bmp = ic.getCachedBitmap(iconUrl)) != null)
        		mHandler.updateAvatar(bmp);
        	
        	if (ic.isExpired(iconUrl, sCp))
                ic.requestImage(iconUrl, mRibbonImageListener, 0, null, sCp);
        }
        
        String title = mAccount.getScreenName();
        String subtitle = getSubtitle();
        
        TextView tv = (TextView)findViewById(R.id.title_gamertag);
        tv.setText(title);
        setTitle(title);
        
        tv = (TextView)findViewById(R.id.ribbon_line_1);
        tv.setText(subtitle);
        
        if (android.os.Build.VERSION.SDK_INT >= 11)
        {
        	new ActionBarHelper().setSubtitle(subtitle);
        }
	}
	
	@Override
	protected void onPause()
	{
	    super.onPause();
	    
	    ImageCache.getInstance().removeListener(mRibbonImageListener);
	    TaskController.getInstance().removeListener(mListener);
	}
	
	@Override
	protected void onResume()
	{
	    super.onResume();
	    
	    toggleProgressBar(false);
	    
	    ImageCache.getInstance().addListener(mRibbonImageListener);
	    TaskController.getInstance().addListener(mListener);
	    
	    updateRibbon();
	}
	
	protected void toggleProgressBar(boolean show)
	{
		ProgressBar bar = (ProgressBar)findViewById(R.id.ribbon_progress_bar);
		if (bar != null)
			bar.setVisibility(show ? View.VISIBLE : View.GONE);
		
		setProgressBarIndeterminateVisibility(show);
	}
}
