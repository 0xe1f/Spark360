/*
 * ErrorDialogFragment.java 
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

package com.akop.bach.fragment;

import org.acra.ErrorReporter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.akop.bach.App;
import com.akop.bach.R;

public class ErrorDialogFragment extends DialogFragment
{
	public interface OnOkListener
	{
		void okClicked(int code, long id, String param);
	}
	
	private OnOkListener mOkListener = null;
	
	public static ErrorDialogFragment newInstance(String errorMessage, Exception error)
	{
		ErrorDialogFragment f = new ErrorDialogFragment();
		
		Bundle args = new Bundle();
		args.putSerializable("exception", error);
		args.putString("message", errorMessage);
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
				.setTitle(R.string.error)
				.setMessage(args.getString("message"));
		
		builder.setNegativeButton(R.string.close, 
				new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog,
                    int whichButton)
            {
            	dismiss();
            }
        });
		
		if (App.getConfig().enableErrorReporting())
		{
			builder.setPositiveButton(R.string.send_report, 
					new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int whichButton)
				{
					Exception ex = (Exception)args.getSerializable("exception");
					ErrorReporter.getInstance().handleException(ex);
					
	            	dismiss();
				}
			});
		}
		
		AlertDialog dialog = builder.create();
		return dialog;
	}
}