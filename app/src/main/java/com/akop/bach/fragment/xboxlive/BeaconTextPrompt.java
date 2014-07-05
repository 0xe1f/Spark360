/*
 * BeaconTextPrompt.java 
 * Copyright (C) 2010-2014 Akop Karapetyan
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

package com.akop.bach.fragment.xboxlive;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.akop.bach.R;

public class BeaconTextPrompt extends DialogFragment
{
	public interface OnOkListener
	{
		void beaconTextEntered(String message);
	}
	
	private OnOkListener mOkListener = null;
	
	public static BeaconTextPrompt newInstance()
	{
		BeaconTextPrompt f = new BeaconTextPrompt();

		Bundle args = new Bundle();
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
		LayoutInflater inflater = getActivity().getLayoutInflater();
		final View view = inflater.inflate(R.layout.xbl_beacon_text_prompt, null);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
				.setTitle(R.string.optional_message)
		        .setPositiveButton(android.R.string.ok,
		        		new DialogInterface.OnClickListener()
		                {
			                public void onClick(DialogInterface dialog,
			                        int whichButton)
			                {
			                	EditText textBox = (EditText)view.findViewById(R.id.beacon_text);
			                	
			                	if (mOkListener != null)
			                		mOkListener.beaconTextEntered(textBox.getText().toString());
			                	
			                	dismiss();
			                }
		                })
		        .setNegativeButton(android.R.string.cancel,
		                new DialogInterface.OnClickListener()
		                {
			                public void onClick(DialogInterface dialog,
			                        int whichButton)
			                {
			                	dismiss();
			                }
		                })
		       .setView(view);
		
		AlertDialog dialog = builder.create();
		return dialog;
	}
}
