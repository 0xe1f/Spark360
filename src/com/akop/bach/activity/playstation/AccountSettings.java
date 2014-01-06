/*
 * AccountSettings.java 
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.RingtonePreference;
import android.view.KeyEvent;

import com.akop.bach.BasicAccount;
import com.akop.bach.App;
import com.akop.bach.PSN.Games;
import com.akop.bach.PSN.Trophies;
import com.akop.bach.Preferences;
import com.akop.bach.PsnAccount;
import com.akop.bach.R;
import com.akop.bach.service.NotificationService;

public class AccountSettings
		extends PreferenceActivity
{
	private PsnAccount mAccount;
	
	private ListPreference mCheckFreqPref;
	private ListPreference mFriendNotifyPref;
	private ListPreference mPsnServerPref;
	private ListPreference mLocalePref;
	
	private RingtonePreference mRingtonePref;
	private CheckBoxPreference mVibratePref;
	
	private int mUpdateFrequency;
	private int mFriendNotifications;
	private String mPsnServer;
	private String mLocale;
	private boolean mVibrate;
	
	private static final String PREFERENCE_RINGTONE = "account_ringtone";
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		mAccount = null;
		if (getIntent().hasExtra("account"))
		{
			mAccount = (PsnAccount)getIntent().getParcelableExtra("account");
		}
		else if (getIntent().getData() != null)
		{
			String uriPart = getIntent().getData().getLastPathSegment();
			if (uriPart != null)
			{
				long accountId = Long.valueOf(uriPart);
				mAccount = (PsnAccount)Preferences.get(this).getAccount(accountId);
			}
		}
		else
		{
			finish();
			return; // Invalid call
		}
		
		mPsnServer = mAccount.getPsnServer();
		mLocale = mAccount.getLocale();
		mUpdateFrequency = mAccount.getSyncPeriod();
		mVibrate = mAccount.isVibrationEnabled();
		mFriendNotifications = mAccount.getFriendNotifications();
		
		String ringtone = mAccount.getRingtone();
		
        addPreferencesFromResource(R.xml.psn_account_settings);
		setTitle(getString(R.string.account_settings_f, mAccount.getDescription()));
		
		findPreference("login").setOnPreferenceClickListener(
				new Preference.OnPreferenceClickListener()
				{
					public boolean onPreferenceClick(Preference preference)
					{
						onLoginSettings();
						return true;
					}
				});
		
		if ((mCheckFreqPref = (ListPreference)findPreference("account_check_frequency")) != null)
		{
			mCheckFreqPref.setValue(String.valueOf(mUpdateFrequency));
			mCheckFreqPref.setSummary(mCheckFreqPref.getEntry());
			mCheckFreqPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			{
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue)
				{
	                final String summary = newValue.toString();
	                int index = mCheckFreqPref.findIndexOfValue(summary);
	                mCheckFreqPref.setSummary(mCheckFreqPref.getEntries()[index]);
	                mCheckFreqPref.setValue((String)newValue);
	                mUpdateFrequency = Integer.valueOf((String)newValue);
	                
	                toggleNotificationSection();
	        		
	                return false;
				}
			});
		}
		
		if ((mPsnServerPref = (ListPreference)findPreference("account_psn_server")) != null)
		{
			mPsnServerPref.setValue(mPsnServer);
			mPsnServerPref.setSummary(mPsnServerPref.getEntry());
			mPsnServerPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			{
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue)
				{
	                final String summary = newValue.toString();
	                int index = mPsnServerPref.findIndexOfValue(summary);
	                mPsnServerPref.setSummary(mPsnServerPref.getEntries()[index]);
	                mPsnServerPref.setValue((String)newValue);
	                mPsnServer = (String)newValue;
	                
	                return false;
				}
			});
		}
		
		if ((mLocalePref = (ListPreference)findPreference("account_locale")) != null)
		{
			mLocalePref.setValue(mLocale);
			mLocalePref.setSummary(mLocalePref.getEntry());
			mLocalePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			{
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue)
				{
	                final String summary = newValue.toString();
	                int index = mLocalePref.findIndexOfValue(summary);
	                mLocalePref.setSummary(mLocalePref.getEntries()[index]);
	                mLocalePref.setValue((String)newValue);
	                mLocale = (String)newValue;
	                
	                return false;
				}
			});
		}
		
		if ((mFriendNotifyPref = (ListPreference)findPreference("account_friend_notifications")) != null)
		{
			mFriendNotifyPref.setValue(String.valueOf(mFriendNotifications));
			mFriendNotifyPref.setSummary(mFriendNotifyPref.getEntry());
			mFriendNotifyPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			{
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue)
				{
	                final String summary = newValue.toString();
	                int index = mFriendNotifyPref.findIndexOfValue(summary);
	                mFriendNotifyPref.setSummary(mFriendNotifyPref.getEntries()[index]);
	                mFriendNotifyPref.setValue((String)newValue);
	                mFriendNotifications = Integer.valueOf((String)newValue);
	                return false;
				}
			});
		}
		
		if ((mVibratePref = (CheckBoxPreference)findPreference("account_vibrate")) != null)
		{
			mVibratePref.setChecked(mVibrate);
			mVibratePref.setOnPreferenceClickListener(new OnPreferenceClickListener()
			{
				@Override
				public boolean onPreferenceClick(Preference preference)
				{
					mVibrate = mVibratePref.isChecked();
					return false;
				}
			});
		}
		
		mRingtonePref = (RingtonePreference)findPreference("account_ringtone");
		
        SharedPreferences prefs = mRingtonePref.getPreferenceManager().getSharedPreferences();
        prefs.edit().putString(PREFERENCE_RINGTONE, ringtone).commit();
        
        toggleNotificationSection();
	}
	
	private void toggleNotificationSection()
	{
		mVibratePref.setEnabled(mUpdateFrequency > 0);
		mFriendNotifyPref.setEnabled(mUpdateFrequency > 0);
		mRingtonePref.setEnabled(mUpdateFrequency > 0);
	}
	
	public static void actionEditSettings(Context context, BasicAccount account)
	{
		Intent intent = new Intent(context, AccountSettings.class);
		intent.putExtra("account", account);
		context.startActivity(intent);		
	}
	
	private void onLoginSettings()
	{
		mAccount.editLogin(this);
	}
	
	private void saveSettings()
	{
		Preferences prefs = Preferences.get(this);
		mAccount.refresh(prefs);
		
        SharedPreferences sharedPrefs = mRingtonePref.getPreferenceManager().getSharedPreferences();
        String ringtone = sharedPrefs.getString(PREFERENCE_RINGTONE, null);
		
		boolean updateFreqChanged = (mUpdateFrequency != mAccount.getSyncPeriod());
        
		if (!mAccount.getPsnServer().equals(mPsnServer))
		{
			// Purge the tables
			
			ContentResolver cr = getContentResolver();
	        StringBuffer buffer = new StringBuffer();
	        
	        Cursor c = cr.query(Games.CONTENT_URI, 
	        		new String[] { Games._ID }, 
	        		Games.ACCOUNT_ID + "=" + mAccount.getId(), 
	        		null, null);
	        
	        if (c != null)
	        {
		        try
		        {
		        	while (c.moveToNext())
		        	{
		        		if (buffer.length() > 0)
		        			buffer.append(",");
		        		buffer.append(c.getLong(0));
		        	}
		        }
		        finally
		        {
	        		c.close();
		        }
	        }
	        
	        try
	        {
				cr.delete(Trophies.CONTENT_URI, Trophies.GAME_ID + " IN ("
						+ buffer.toString() + ")", null);
	        }
	        catch(Exception e)
	        {
	        	// Suppress errors
	        }
	        
	        try
	        {
				cr.delete(Games.CONTENT_URI, Games.ACCOUNT_ID + "="
						+ mAccount.getId(), null);
	        }
	        catch(Exception e)
	        {
	        	// Suppress errors
	        }
	        
			// and set games to refresh
			
			mAccount.setLastGameUpdate(0);
			mAccount.setPsnServer(mPsnServer);
		}
		
		mAccount.setLocale(mLocale);
		mAccount.setFriendNotifications(mFriendNotifications);
		mAccount.setSyncPeriod(mUpdateFrequency);
		mAccount.setVibration(mVibrate);
        mAccount.setRingtone(ringtone);
        
		mAccount.save(Preferences.get(this));
		
		if (updateFreqChanged)
		{
			NotificationService.actionReschedule(this);
		}
		else
		{
			if (App.getConfig().logToConsole())
				App.logv("Update frequency did not change; not rescheduling");
		}
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		mAccount.refresh(Preferences.get(this));
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK)
			saveSettings();
		return super.onKeyDown(keyCode, event);
	}
}
