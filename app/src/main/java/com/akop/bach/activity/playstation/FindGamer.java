/*
 * FindGamer.java 
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

package com.akop.bach.activity.playstation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.akop.bach.R;

public class FindGamer extends Activity implements OnClickListener, TextWatcher
{
	private EditText mTextbox;
	private Button mOk;
	private Button mCancel;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.psn_find_gamer);
		
		mTextbox = (EditText)findViewById(R.id.add_friend_gamertag);
		mOk = (Button)findViewById(R.id.add_friend_ok);
		mCancel = (Button)findViewById(R.id.add_friend_cancel);
		
		mOk.setOnClickListener(this);
		mCancel.setOnClickListener(this);
		mTextbox.addTextChangedListener(this);
	}
	
	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.add_friend_ok:
			Intent intent = new Intent();
	    	intent.putExtra("onlineId", mTextbox.getText().toString());
	    	setResult(RESULT_OK, intent);
			finish();
			break;
		case R.id.add_friend_cancel:
			setResult(RESULT_CANCELED);
			finish();
			break;
		}
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		
		validate();
	}
	
	private void validate()
	{
		boolean valid = mTextbox.getText().toString().trim().length() > 0;
		mOk.setEnabled(valid);
	}
	
	@Override
	public void afterTextChanged(Editable s)
	{
		validate();
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after)
	{
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
	}
	
	public static void actionShow(Activity context, Fragment frag, int requestCode)
	{
    	Intent intent = new Intent(context, FindGamer.class);
    	frag.startActivityForResult(intent, requestCode);
	}
}
