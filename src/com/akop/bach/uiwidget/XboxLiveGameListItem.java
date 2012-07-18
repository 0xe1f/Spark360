/*
 * XboxLiveGameListItem.java 
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

package com.akop.bach.uiwidget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.akop.bach.R;

public class XboxLiveGameListItem
		extends RelativeLayout
{
	public interface OnBeaconClickListener
	{
		void beaconClicked(long id, boolean isSet);
	}
	
	public long mItemId;
	public boolean mBeaconSet;
	public OnBeaconClickListener mClickListener = null;
	
	private boolean mDownEvent;
	private boolean mCachedViewPositions;
	private int mIconLeft;
	
	private final static float ICON_PAD = 10.0F;
	
	public XboxLiveGameListItem(Context context)
	{
		super(context);
	}
	
	public XboxLiveGameListItem(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}
	
	public XboxLiveGameListItem(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}
	
	public void bindViewInit()
	{
		mCachedViewPositions = false;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		boolean handled = false;
		View star = findViewById(R.id.game_beacon);
		
		if (star != null)
		{
			int touchX = (int)event.getX();
			
			if (!mCachedViewPositions)
			{
				float paddingScale = getContext().getResources().getDisplayMetrics().density;
				int iconPadding = (int) ((ICON_PAD * paddingScale) + 0.5);
				mIconLeft = star.getLeft() - iconPadding;
				mCachedViewPositions = true;
			}
			
			switch (event.getAction())
			{
			case MotionEvent.ACTION_DOWN:
				mDownEvent = true;
				if (touchX > mIconLeft)
					handled = true;
				break;
			
			case MotionEvent.ACTION_CANCEL:
				mDownEvent = false;
				break;
			
			case MotionEvent.ACTION_UP:
				if (mDownEvent)
				{
					if (touchX > mIconLeft && mClickListener != null)
					{
						mClickListener.beaconClicked(mItemId, mBeaconSet);
						handled = true;
					}
				}
				break;
			}
		}
		
		if (handled)
			postInvalidate();
		else
			handled = super.onTouchEvent(event);
		
		return handled;
	}
}