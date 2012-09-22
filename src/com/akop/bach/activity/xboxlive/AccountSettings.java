/*
 * AccountSettings.java 
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

package com.akop.bach.activity.xboxlive;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.akop.bach.Preferences;
import com.akop.bach.R;
import com.akop.bach.XboxLive.NotifyStates;
import com.akop.bach.XboxLiveAccount;
import com.akop.bach.service.NotificationService;

public class AccountSettings extends PreferenceActivity
{
	private XboxLiveAccount mAccount;
	
	private ListPreference mCheckFreqPref;
	private ListPreference mFriendNotifyPref;
	private ListPreference mBeaconNotifyPref;
	private ListPreference mCoverflowPref;
	private RingtonePreference mRingtonePref;
	private CheckBoxPreference mMessageNotifyPref;
	private CheckBoxPreference mVibratePref;
	private CheckBoxPreference mShowAppsPref;
	
	private int mUpdateFrequency;
	private int mFriendNotifications;
	private int mBeaconNotifications;
	private boolean mMessageNotifications;
	private boolean mVibrate;
	private boolean mShowApps;
	private int mCoverflowMode;
	
	private static final String PREFERENCE_RINGTONE = "account_ringtone";
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		mAccount = null;
		if (getIntent().hasExtra("account"))
		{
			mAccount = (XboxLiveAccount)getIntent().getParcelableExtra("account");
		}
		else if (getIntent().getData() != null)
		{
			String uriPart = getIntent().getData().getLastPathSegment();
			if (uriPart != null)
			{
				long accountId = Long.valueOf(uriPart);
				mAccount = (XboxLiveAccount)Preferences.get(this).getAccount(accountId);
			}
		}
		else
		{
			finish();
			return; // Invalid call
		}
		
		mUpdateFrequency = mAccount.getSyncPeriod();
		mVibrate = mAccount.isVibrationEnabled();
		mFriendNotifications = mAccount.getFriendNotifications();
		mBeaconNotifications = mAccount.getBeaconNotifications();
		mMessageNotifications = mAccount.isMessageNotificationEnabled();
		mShowApps = mAccount.isShowingApps();
		mCoverflowMode = mAccount.getCoverflowMode();
		
		String ringtone = mAccount.getRingtone();
		
        addPreferencesFromResource(R.xml.xbl_account_settings);
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
		
		if ((mBeaconNotifyPref = (ListPreference)findPreference("account_beacon_notifications")) != null)
		{
			mBeaconNotifyPref.setValue(String.valueOf(mBeaconNotifications));
			mBeaconNotifyPref.setSummary(mBeaconNotifyPref.getEntry());
			mBeaconNotifyPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			{
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue)
				{
	                final String summary = newValue.toString();
	                int index = mBeaconNotifyPref.findIndexOfValue(summary);
	                
	                mBeaconNotifyPref.setSummary(mBeaconNotifyPref.getEntries()[index]);
	                mBeaconNotifyPref.setValue((String)newValue);
	                mBeaconNotifications = Integer.valueOf((String)newValue);
	                
	                return false;
				}
			});
		}
		
		if ((mCoverflowPref = (ListPreference)findPreference("account_coverflow_mode")) != null)
		{
			mCoverflowPref.setValue(String.valueOf(mCoverflowMode));
			mCoverflowPref.setSummary(mCoverflowPref.getEntry());
			mCoverflowPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			{
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue)
				{
	                final String summary = newValue.toString();
	                int index = mCoverflowPref.findIndexOfValue(summary);
	                
	                mCoverflowPref.setSummary(mCoverflowPref.getEntries()[index]);
	                mCoverflowPref.setValue((String)newValue);
	                mCoverflowMode = Integer.valueOf((String)newValue);
	                
	                return false;
				}
			});
		}
		
		if ((mMessageNotifyPref = (CheckBoxPreference)findPreference("account_message_notifications")) != null)
		{
			mMessageNotifyPref.setChecked(mMessageNotifications);
			mMessageNotifyPref.setOnPreferenceClickListener(new OnPreferenceClickListener()
			{
				@Override
				public boolean onPreferenceClick(Preference preference)
				{
					mMessageNotifications = mMessageNotifyPref.isChecked();
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
		
		if ((mShowAppsPref = (CheckBoxPreference)findPreference("account_show_apps")) != null)
		{
			mShowAppsPref.setChecked(mShowApps);
			mShowAppsPref.setOnPreferenceClickListener(new OnPreferenceClickListener()
			{
				@Override
				public boolean onPreferenceClick(Preference preference)
				{
					mShowApps = mShowAppsPref.isChecked();
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
		mBeaconNotifyPref.setEnabled(mUpdateFrequency > 0);
		mRingtonePref.setEnabled(mUpdateFrequency > 0);
		mMessageNotifyPref.setEnabled(mUpdateFrequency > 0);
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
        
		if (updateFreqChanged || mFriendNotifications != mAccount.getFriendNotifications())
			NotifyStates.setFriendsLastNotified(this, mAccount, null);
		
		if (updateFreqChanged || mMessageNotifications != mAccount.isMessageNotificationEnabled())
			NotifyStates.setMessagesLastNotified(this, mAccount, null);
		
		mAccount.setFriendNotifications(mFriendNotifications);
		mAccount.setBeaconNotifications(mBeaconNotifications);
        mAccount.setMessageNotifications(mMessageNotifications);
		
		mAccount.setSyncPeriod(mUpdateFrequency);
		mAccount.setVibration(mVibrate);
        mAccount.setRingtone(ringtone);
        mAccount.setCoverflowMode(mCoverflowMode);
        mAccount.setShowApps(mShowApps);
        
		mAccount.save(Preferences.get(this));
		
		if (updateFreqChanged)
		{
			NotificationService.actionReschedule(this);
		}
		else
		{
			if (App.LOGV)
				App.logv("Update frequency did not change; not rescheduling");
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK)
			saveSettings();
		
		return super.onKeyDown(keyCode, event);
	}
}
