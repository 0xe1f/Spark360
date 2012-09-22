/*
 * EditProfileFragment.java 
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

package com.akop.bach.fragment.xboxlive;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.akop.bach.App;
import com.akop.bach.R;
import com.akop.bach.XboxLive.Profiles;
import com.akop.bach.XboxLiveAccount;

public class EditProfileFragment extends DialogFragment
{
	public static interface ProfileUpdater
	{
		void updateProfile(XboxLiveAccount account, 
				String motto, String name, String location, String bio);
	}
	
	private ProfileUpdater mUpdater = null;
	private XboxLiveAccount mAccount = null;
	
	private EditText mMotto;
	private EditText mName;
	private EditText mLocation;
	private EditText mBio;
	
	public static EditProfileFragment newInstance(XboxLiveAccount account)
	{
		EditProfileFragment f = new EditProfileFragment();
		
		Bundle args = new Bundle();
		
		if (account != null)
			args.putParcelable("account", account);
		
		f.setArguments(args);
		
		return f;
	}
	
	private void synchronizeLocal()
	{
		String motto = "";
		String name = "";
		String location = "";
		String bio = "";
		
		if (mAccount != null)
		{
			Cursor cursor = getActivity().getContentResolver().query(Profiles.CONTENT_URI,
					new String[] 
					{ 
						Profiles.MOTTO,
						Profiles.NAME,
						Profiles.LOCATION,
						Profiles.BIO,
					},
					Profiles.ACCOUNT_ID + "=" + mAccount.getId(), null,
					null);
			
			if (cursor != null)
			{
				try
				{
					if (cursor.moveToFirst())
					{
						motto = cursor.getString(0);
						name = cursor.getString(1);
						location = cursor.getString(2);
						bio = cursor.getString(3);
					}
				}
				catch(Exception ex)
				{
					// Do nothing
					if (App.LOGV)
						ex.printStackTrace();
				}
				finally
				{
					cursor.close();
				}
			}
		}
		
		mMotto.setText(motto);
		mName.setText(name);
		mLocation.setText(location);
		mBio.setText(bio);
	}
	
	@Override
	public void onCreate(Bundle state)
	{
		super.onCreate(state);
		
	    Bundle args = getArguments();
		mAccount = (XboxLiveAccount)args.getParcelable("account");
		
		setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Dialog);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		LayoutInflater li = getActivity().getLayoutInflater();
		View layout = li.inflate(R.layout.xbl_fragment_edit_profile, null);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
				.setView(layout)
				.setTitle(R.string.edit_profile)
				.setPositiveButton(R.string.save, 
						new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int whichButton)
					{
						if (mUpdater != null)
						{
							mUpdater.updateProfile(mAccount, 
									mMotto.getText().toString(), 
									mName.getText().toString(), 
									mLocation.getText().toString(), 
									mBio.getText().toString());
						}
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
		        });
		
		mMotto = (EditText)layout.findViewById(R.id.profile_motto);
		mName = (EditText)layout.findViewById(R.id.profile_name);
		mLocation = (EditText)layout.findViewById(R.id.profile_location);
		mBio = (EditText)layout.findViewById(R.id.profile_bio);
		
		synchronizeLocal();
		
		return builder.create();
	}
	
	public void setProfileUpdater(ProfileUpdater updater)
	{
		mUpdater = updater;
	}
}
