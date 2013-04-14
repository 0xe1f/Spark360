/*
 * XboxLiveFriendListItem.java 
 * Copyright (C) 2010-2013 Akop Karapetyan
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

public class XboxLiveFriendListItem
		extends RelativeLayout
{
	public interface OnStarClickListener
	{
		void starClicked(long id, boolean isSet);
	}
	
	public int mStatusCode;
	public String mGamertag;
	public long mFriendId;
	public boolean mIsFavorite;
	public Object mBinding;
	
	private boolean mDownEvent;
	private boolean mCachedViewPositions;
	private int mStarLeft;
	public OnStarClickListener mClickListener = null;
	
	private final static float STAR_PAD = 10.0F;
	
	public XboxLiveFriendListItem(Context context)
	{
		super(context);
	}
	
	public XboxLiveFriendListItem(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}
	
	public XboxLiveFriendListItem(Context context, AttributeSet attrs, int defStyle)
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
		View star = findViewById(R.id.friend_favorite);
		
		if (star != null)
		{
			int touchX = (int) event.getX();
			
			if (!mCachedViewPositions)
			{
				float paddingScale = getContext().getResources()
						.getDisplayMetrics().density;
				int starPadding = (int) ((STAR_PAD * paddingScale) + 0.5);
				mStarLeft = star.getLeft() - starPadding;
				mCachedViewPositions = true;
			}
			
			switch (event.getAction())
			{
			case MotionEvent.ACTION_DOWN:
				mDownEvent = true;
				if (touchX > mStarLeft)
					handled = true;
				break;
			
			case MotionEvent.ACTION_CANCEL:
				mDownEvent = false;
				break;
			
			case MotionEvent.ACTION_UP:
				if (mDownEvent)
				{
					if (touchX > mStarLeft && mClickListener != null)
					{
						mClickListener.starClicked(mFriendId, mIsFavorite);
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