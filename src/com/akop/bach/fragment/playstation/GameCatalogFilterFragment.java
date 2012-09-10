/*
 * GameCatalogFilterFragment.java 
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

package com.akop.bach.fragment.playstation;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.RadioButton;
import android.widget.Spinner;

import com.akop.bach.PSN;
import com.akop.bach.Preferences;
import com.akop.bach.PsnAccount;
import com.akop.bach.R;

public class GameCatalogFilterFragment extends DialogFragment
{
	private int mReleaseStatus;
	private int mSortOrder;
	private int mConsole;
	private PsnAccount mAccount;
	
	public interface OnOkListener
	{
		void okClicked(int console, int releaseStatusFilter, int sortFilter);
	}
	
	private OnOkListener mOkListener = null;
	
	public static GameCatalogFilterFragment newInstance(PsnAccount account)
	{
		GameCatalogFilterFragment f = new GameCatalogFilterFragment();
		
		Bundle args = new Bundle();
		args.putParcelable("account", account);
		args.putInt("sortOrder", account.getCatalogSortOrder());
		args.putInt("releaseStatus", account.getCatalogReleaseStatus());
		args.putInt("console", account.getCatalogConsole());
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
		
		mSortOrder = getArguments().getInt("sortOrder");
		mReleaseStatus = getArguments().getInt("releaseStatus");
		mConsole = getArguments().getInt("console");
		mAccount = (PsnAccount)getArguments().getSerializable("account");
		
		if (savedInstanceState != null)
		{
			mSortOrder = savedInstanceState.getInt("sortOrder", mSortOrder);
			mReleaseStatus = savedInstanceState.getInt("releaseStatus", mReleaseStatus);
			mConsole = savedInstanceState.getInt("console", mConsole);
		}
		
		setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Dialog);
	}
	
	@Override
	public void onSaveInstanceState(Bundle arg0)
	{
	    super.onSaveInstanceState(arg0);
	    
		arg0.putInt("sortOrder", mSortOrder);
		arg0.putInt("releaseStatus", mReleaseStatus);
		arg0.putInt("console", mConsole);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		LayoutInflater li = getActivity().getLayoutInflater();
		View layout = li.inflate(R.layout.psn_fragment_catalog_filter, null);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
				.setView(layout)
				.setTitle(R.string.catalog_filter_u)
				.setPositiveButton(android.R.string.yes, 
						new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int whichButton)
					{
						mAccount.setCatalogConsole(mConsole);
						mAccount.setCatalogReleaseStatus(mReleaseStatus);
						mAccount.setCatalogSortOrder(mSortOrder);
						mAccount.save(Preferences.get(getActivity()));
						
		            	if (mOkListener != null)
			                mOkListener.okClicked(mConsole,
			                        mReleaseStatus, mSortOrder);
		            	
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
		
		RadioButton btn;
		
		btn = (RadioButton)layout.findViewById(R.id.filter_order_alpha);
		btn.setChecked(mSortOrder == PSN.CATALOG_SORT_BY_ALPHA);
		btn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mSortOrder = PSN.CATALOG_SORT_BY_ALPHA;
			}
		});
		
		btn = (RadioButton)layout.findViewById(R.id.filter_order_reldate);
		btn.setChecked(mSortOrder == PSN.CATALOG_SORT_BY_RELEASE);
		btn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mSortOrder = PSN.CATALOG_SORT_BY_RELEASE;
			}
		});
		
		btn = (RadioButton)layout.findViewById(R.id.filter_rs_coming_soon);
		btn.setChecked(mReleaseStatus == PSN.CATALOG_RELEASE_COMING_SOON);
		btn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mReleaseStatus = PSN.CATALOG_RELEASE_COMING_SOON;
			}
		});
		
		btn = (RadioButton)layout.findViewById(R.id.filter_rs_out_now);
		btn.setChecked(mReleaseStatus == PSN.CATALOG_RELEASE_OUT_NOW);
		btn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mReleaseStatus = PSN.CATALOG_RELEASE_OUT_NOW;
			}
		});
		
		Spinner spin = (Spinner)layout.findViewById(R.id.filter_console);
		spin.setSelection(mConsole);
		spin.setOnItemSelectedListener(new OnItemSelectedListener()
		{
			@Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3)
            {
				mConsole = arg2;
            }
			
			@Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
		});
		
		View view = layout.findViewById(R.id.filter_rs);
		view.setVisibility(mAccount.supportsFilteringByReleaseDate()
				? View.VISIBLE : View.GONE);
		
		return builder.create();
	}
}