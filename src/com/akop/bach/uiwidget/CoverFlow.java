/*
 * CoverFlow.java
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
import android.graphics.Camera;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Transformation;
import android.widget.Gallery;

public class CoverFlow extends Gallery
{
	private Camera mCamera = new Camera();
	private int mMaxRotationAngle = 60;
	private int mMaxZoom = -120;
	private int mCoverflowCenter;
	
	public CoverFlow(Context context)
	{
		super(context);
		
		this.setMaxZoom(-120);
		this.setStaticTransformationsEnabled(true);
	}
	
	public CoverFlow(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.setStaticTransformationsEnabled(true);
	}
	
	public CoverFlow(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		this.setStaticTransformationsEnabled(true);
	}
	
	public int getMaxRotationAngle()
	{
		return mMaxRotationAngle;
	}
	
	public void setMaxRotationAngle(int maxRotationAngle)
	{
		mMaxRotationAngle = maxRotationAngle;
	}
	
	public int getMaxZoom()
	{
		return mMaxZoom;
	}
	
	public void setMaxZoom(int maxZoom)
	{
		this.mMaxZoom = maxZoom;
	}
	
	private int getCenterOfCoverflow()
	{
		return (getWidth() - getPaddingLeft() - getPaddingRight()) / 2
		        + getPaddingLeft();
	}
	
	private static int getCenterOfView(View view)
	{
		return view.getLeft() + view.getWidth() / 2;
	}
	
	protected boolean getChildStaticTransformation(View child, Transformation t)
	{
		final int childCenter = getCenterOfView(child);
		final int childWidth = child.getWidth();
		int rotationAngle = 0;
		
		t.clear();
		t.setTransformationType(Transformation.TYPE_MATRIX);
		
		if (childCenter == mCoverflowCenter)
		{
			transformImageBitmap(child, t, 0);
		}
		else
		{
			rotationAngle = (int) (((float)(mCoverflowCenter - childCenter) 
					/ childWidth) * mMaxRotationAngle);
			
			if (Math.abs(rotationAngle) > mMaxRotationAngle)
			{
				rotationAngle = (rotationAngle < 0) ? -mMaxRotationAngle
				        : mMaxRotationAngle;
			}
			
			transformImageBitmap(child, t, rotationAngle);
		}
		
		return true;
	}
	
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		mCoverflowCenter = getCenterOfCoverflow();
		super.onSizeChanged(w, h, oldw, oldh);
	}
	
	private void transformImageBitmap(View child, Transformation t,
	        int rotationAngle)
	{
		mCamera.save();
		
		final Matrix imageMatrix = t.getMatrix();
		final int imageHeight = child.getLayoutParams().height;
		final int imageWidth = child.getLayoutParams().width;
		final int rotation = Math.abs(rotationAngle);
		
		mCamera.translate(0.0f, 0.0f, 140.0f);
		
		if (rotation < mMaxRotationAngle)
		{
			float zoomAmount = (float) (mMaxZoom + (rotation * 1.5));
			mCamera.translate(0.0f, 0.0f, zoomAmount);
		}
		
		//mCamera.rotateY(rotationAngle);
		mCamera.getMatrix(imageMatrix);
		
		imageMatrix.preTranslate(-(imageWidth / 2), -(imageHeight / 2));
		imageMatrix.postTranslate((imageWidth / 2), (imageHeight / 2));
		
		mCamera.restore();
	}
}
