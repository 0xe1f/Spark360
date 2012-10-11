/*
 * RibbonedMultipaneActivity.java 
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

import com.akop.bach.BasicAccount;
import com.akop.bach.App;
import com.akop.bach.ImageCache;
import com.akop.bach.ImageCache.CachePolicy;
import com.akop.bach.ImageCache.OnImageReadyListener;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.fragment.ErrorDialogFragment;
import com.akop.bach.parser.Parser;

public abstract class RibbonedMultiPane extends FragmentActivity
{
	protected BasicAccount mAccount;
	protected Fragment mTitleFragment;
	protected Fragment mDetailFragment;
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
			String message = Parser.getErrorMessage(RibbonedMultiPane.this, e);
			
			ErrorDialogFragment frag = ErrorDialogFragment.newInstance(message, e);
			frag.show(getSupportFragmentManager(), "errorDialog");
		}
	};
	
	protected abstract String getSubtitle();
	
	protected abstract int getLayout();
	
	protected abstract int getActionBarLayout();
	
	/* TODO
	{
		return R.layout.psn_multipane;
		return R.layout.xbl_multipane;
	}
	*/
	
	protected String getBachTitle()
	{
		return (mAccount != null) ? mAccount.getScreenName() : null;
	}
	
	protected boolean allowNullAccounts()
	{
		return false;
	}
	
	@TargetApi(11)
    class ActionBarHelper
	{
		public void init()
		{
			getActionBar().setCustomView(getActionBarLayout());
			/* TODO
			getActionBar().setCustomView(R.layout.psn_actionbar_custom);
			getActionBar().setCustomView(R.layout.xbl_actionbar_custom);
			*/
		}
		
		public void setSubtitle(String subtitle)
		{
			getActionBar().setSubtitle(subtitle);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(getLayout());
		
		if ((mAccount = (BasicAccount)getIntent().getParcelableExtra("account")) == null
				&& !allowNullAccounts())
		{
			if (App.getConfig().logToConsole())
				App.logv("Account is null");
			
        	finish();
        	return;
		}
		
        if (android.os.Build.VERSION.SDK_INT >= 11)
        {
        	new ActionBarHelper().init();
        }
        
        // this part was under each activity
        
        if (!initializeParameters())
        {
        	finish();
        	return;
        }
        
		FragmentManager fm = getSupportFragmentManager();
		Fragment titleFrag;
		
		FragmentTransaction ft = fm.beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        
		if ((titleFrag = fm.findFragmentByTag("title")) == null)
		{
			titleFrag = instantiateTitleFragment();
	        ft.replace(R.id.fragment_titles, titleFrag, "title");
		}
		
		if (isDualPane())
		{
			if ((mDetailFragment = fm.findFragmentByTag("details")) == null)
			{
				if ((mDetailFragment = instantiateDetailFragment()) != null)
					ft.replace(R.id.fragment_details, mDetailFragment, "details");
			}
			else if (mDetailFragment.isDetached())
			{
				ft.attach(mDetailFragment);
			}
		}
		else
		{
			Fragment detailFragment;
			if ((detailFragment = fm.findFragmentByTag("details")) != null)
			{
				ft.detach(detailFragment);
			}
		}
		
		ft.commit();
	}
	
	protected boolean initializeParameters()
	{
		return true;
	}
	
	protected abstract Fragment instantiateTitleFragment();
	
	protected abstract Fragment instantiateDetailFragment();
	
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
						mAccount.open(RibbonedMultiPane.this);
				}
			});
		}
		
    	Bitmap bmp = null;
        String iconUrl = (mAccount != null) ? mAccount.getIconUrl() : null;
        
        if (iconUrl != null)
        {
        	ImageCache ic = ImageCache.getInstance();
        	if ((bmp = ic.getCachedBitmap(iconUrl)) != null)
        		mHandler.updateAvatar(bmp);
        	
        	if (ic.isExpired(iconUrl, sCp))
                ic.requestImage(iconUrl, mRibbonImageListener, 0, null, sCp);
        }
        else
        {
			ImageView iv = (ImageView)findViewById(R.id.title_icon);
			if (iv != null)
				iv.setImageResource(R.drawable.icon);
        }
        
        String title = getBachTitle();
        String subtitle = getSubtitle();
        
        TextView tv;
        
        if ((tv = (TextView)findViewById(R.id.title_gamertag)) != null)
        	tv.setText(title);
        
        if ((tv = (TextView)findViewById(R.id.ribbon_line_1)) != null)
        	tv.setText(subtitle);
        
        setTitle(title);
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
	
	protected View getDetailPane()
	{
		return findViewById(R.id.fragment_details);
	}
	
	protected boolean isDualPane()
	{
		return (findViewById(R.id.fragment_details) != null);
	}
	
	protected void toggleProgressBar(boolean show)
	{
		ProgressBar bar = (ProgressBar)findViewById(R.id.ribbon_progress_bar);
		if (bar != null)
			bar.setVisibility(show ? View.VISIBLE : View.GONE);
		
		setProgressBarIndeterminateVisibility(show);
	}
}
