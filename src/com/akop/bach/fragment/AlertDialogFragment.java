/*
 * AlertDialogFragment.java
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class AlertDialogFragment extends DialogFragment
{
	public static final int TYPE_OK = 0;
	public static final int TYPE_YES_NO = 1;
	
	public interface OnOkListener
	{
		void okClicked(int code, long id, String param);
	}
	
	private OnOkListener mOkListener = null;
	
	public static AlertDialogFragment newInstance(int code, String title, String message, long id)
	{
		return newInstance(TYPE_YES_NO, code, title, message, id, null);
	}
	
	public static AlertDialogFragment newInstance(int code, String title, String message, String param)
	{
		return newInstance(TYPE_YES_NO, code, title, message, 0, param);
	}
	
	public static AlertDialogFragment newInstance(int type, int code, String title, String message, long id, String param)
	{
		AlertDialogFragment f = new AlertDialogFragment();
		
		// Supply num input as an argument.
		Bundle args = new Bundle();
		args.putInt("code", code);
		args.putInt("type", type);
		args.putString("title", title);
		args.putString("message", message);
		args.putLong("id", id);
		args.putString("param", param);
		f.setArguments(args);
		
		return f;
	}
	
	public OnOkListener getOnOkListener()
	{
		return mOkListener;
	}
	
	public void setOnOkListener(OnOkListener listener)
	{
		mOkListener = listener;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Dialog);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		final Bundle args = getArguments();
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
				.setTitle(args.getString("title"))
				.setMessage(args.getString("message"));
		
		if (args.getInt("type") == TYPE_OK)
		{
			builder
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int whichButton)
				{
	            	dismiss();
				}
			});
		}
		else if (args.getInt("type") == TYPE_YES_NO)
		{
			builder
			.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int whichButton)
				{
	            	if (mOkListener != null)
	            	{
	            		mOkListener.okClicked(args.getInt("code"), 
	            				args.getLong("id"), args.getString("param"));
	            	}
	            	
	            	dismiss();
				}
			})
			.setNegativeButton(android.R.string.no,
	                new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog,
                        int whichButton)
                {
                	dismiss();
                }
            });
		}
		
		AlertDialog dialog = builder.create();
		return dialog;
	}
}