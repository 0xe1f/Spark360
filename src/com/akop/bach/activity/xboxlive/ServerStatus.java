/*
 * ServerStatus.java 
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.akop.bach.Account;
import com.akop.bach.App;
import com.akop.bach.R;
import com.akop.bach.TaskController;
import com.akop.bach.TaskController.CustomTask;
import com.akop.bach.TaskController.TaskListener;
import com.akop.bach.XboxLive;
import com.akop.bach.XboxLive.LiveStatusInfo;
import com.akop.bach.XboxLive.LiveStatusInfo.Category;
import com.akop.bach.parser.XboxLiveParser;

public class ServerStatus
		extends Activity
{
	protected class ServerStatusHandler extends Handler
	{
		private static final int MSG_REFRESH = 1000;
		private static final int MSG_SHOW_THROBBER = 1001;
		
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case MSG_REFRESH:
				if (!isFinishing())
				{
					mStatus = (LiveStatusInfo)msg.obj; 
					update();
				}
				break;
			case MSG_SHOW_THROBBER:
				setProgressBarIndeterminateVisibility(msg.arg1 != 0);
				findViewById(R.id.progress).setVisibility(msg.arg1 != 0 
						? View.VISIBLE : View.INVISIBLE);
				break;
			}
		}
		
		public void updateStatus(Object obj)
		{
			sendMessage(Message.obtain(this, MSG_REFRESH, obj));
		}
		
		public void showProgressBar(boolean show)
		{
			sendMessage(Message.obtain(this, MSG_SHOW_THROBBER, show ? 1 : 0, 0));
		}
	}
	
	private ServerStatusHandler mHandler = new ServerStatusHandler();
	
	private TaskListener mListener = new TaskListener()
	{
		@Override
		public void onTaskSucceeded(Account account, Object requestParam,
				Object result)
		{
			mHandler.updateStatus(result);
		}
		
		@Override
		public void onAllTasksCompleted()
		{
			mHandler.showProgressBar(false);
		}
		
		@Override
		public void onTaskStarted()
		{
			mHandler.showProgressBar(true);
		}
	};
	
	private LiveStatusInfo mStatus;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.xbl_server_status);
		
		mStatus = null;
		if (savedInstanceState != null)
		{
			if (savedInstanceState.containsKey("status"))
				mStatus = (LiveStatusInfo)savedInstanceState
						.getParcelable("status");
		}
		
		if (mStatus != null)
			update();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		if (mStatus != null)
			outState.putParcelable("status", mStatus);
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		
		TaskController.getInstance().removeListener(mListener);
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		
		TaskController.getInstance().addListener(mListener);
		mHandler.showProgressBar(TaskController.getInstance().isBusy());
		
		if (mStatus == null)
			requestServerStatus();
	}
	
	public static void actionShow(Context context)
	{
		Intent intent = new Intent(context, ServerStatus.class);
		context.startActivity(intent);
	}
	
	private void update()
	{
		if (mStatus == null)
			return;
		
		LinearLayout parent = (LinearLayout)findViewById(R.id.status_group);
		
		for (Category cat : mStatus.categories)
		{
			View view = getLayoutInflater().inflate(
					R.layout.xbl_server_status_item, parent, false);
			
			ImageView icon = (ImageView)view.findViewById(R.id.icon);
			icon.setImageResource((cat.status == XboxLive.LIVE_STATUS_OK)
					? R.drawable.xbox_good : R.drawable.xbox_bad);
			
			TextView tv = (TextView)view.findViewById(R.id.title);
			tv.setText(cat.name);
			
			tv = (TextView)view.findViewById(R.id.statusText);
			tv.setText(cat.statusText);
			
			parent.addView(view);
		}
	}
	
	private void requestServerStatus()
	{
		TaskController.getInstance().runCustomTask(null, new CustomTask<LiveStatusInfo>()
				{
					@Override
					public void runTask()
					{
						XboxLiveParser p = new XboxLiveParser(ServerStatus.this);

						try
						{
							setResult(p.fetchServerStatus());
						}
						catch (Exception e)
						{
							if (App.LOGV)
								e.printStackTrace();
						}
						finally
						{
							p.dispose();
						}
					}
				}, mListener);
	}
}
