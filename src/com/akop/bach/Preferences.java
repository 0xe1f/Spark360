/*
 * Preferences.java
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

package com.akop.bach;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings.Secure;

import com.akop.bach.util.Base64;
import com.akop.bach.util.Base64DecoderException;

public class Preferences
{
	public static class WidgetInfo
	{
		public int widgetId;
		public BasicAccount account;
		public ComponentName componentName;
	}
	
	private static Preferences sPrefs;
	private SharedPreferences mSharedPrefs;
	
	private Secret mSecret = null;
	
	private Cipher mV3Encryptor = null;
	private Cipher mV3Decryptor = null;
	private static final String VERSION2_CRYPT_KEY = "Caffeine|Spark";
	
    private static final String UTF8 = "UTF-8";
    
	private Preferences(Context context)
	{
		mSharedPrefs = context.getSharedPreferences("Spark", 
				Context.MODE_PRIVATE);
		
		try
		{
			initCryptors(context);
		}
		catch (GeneralSecurityException e)
		{
			if (App.getConfig().logToConsole())
				e.printStackTrace();
		}
	}
	
	private Secret getSecret()
	{
		if (mSecret == null)
		{
			try
			{
				Class<?> privateSecret = Class.forName("com.akop.bach.PrivateSecret");
				mSecret = (Secret)privateSecret.newInstance();
			}
			catch(Exception ex)
			{
				if (App.getConfig().logToConsole())
					App.logv("Could not instantiate PrivateSecret");
			}
			
			if (mSecret == null)
				mSecret = new GenericSecret();
		}
		
		return mSecret;
	}
	
	private void initCryptors(Context context)
			throws GeneralSecurityException
	{
		Secret secret = getSecret();
		
		String androidId = Secure.getString(context.getContentResolver(),
				Secure.ANDROID_ID);
		
		if (androidId == null)
			androidId = VERSION2_CRYPT_KEY;
		
        SecretKeyFactory v3factory = SecretKeyFactory.getInstance(secret.getKeygenAlgo());
        String v3key = String.format("%s,%s", androidId, secret.getCryptKey());
        KeySpec v3KeySpec = new PBEKeySpec(v3key.toCharArray(), secret.getSalt(), 1024, 256);
        SecretKey v3Tmp = v3factory.generateSecret(v3KeySpec);
        SecretKey v3Secret = new SecretKeySpec(v3Tmp.getEncoded(), secret.getSecretKeyAlgo());
        
        mV3Encryptor = Cipher.getInstance(secret.getCipherAlgo());
        mV3Encryptor.init(Cipher.ENCRYPT_MODE, v3Secret, new IvParameterSpec(secret.getIv()));
        
        mV3Decryptor = Cipher.getInstance(secret.getCipherAlgo());
        mV3Decryptor.init(Cipher.DECRYPT_MODE, v3Secret, new IvParameterSpec(secret.getIv()));
	}
	
	public SharedPreferences getSharedPreferences()
	{
		return mSharedPrefs;
	}
	
	public static synchronized Preferences get(Context context)
	{
		if (sPrefs == null)
			sPrefs = new Preferences(context);
		
		return sPrefs;
	}
	
	public BasicAccount getAccount(String uuid)
	{
		String uuids = getSharedPreferences().getString("accountUuids", null);
		if (uuids == null || uuids.length() < 1)
			return null;
		
		if (Arrays.asList(uuids.split(",")).contains(uuid))
			return BasicAccount.create(this, uuid);
		
		return null;
	}
	
	public BasicAccount getAccount(long id)
	{
		String uuids = getSharedPreferences().getString("accountUuids", null);
		if (uuids == null || uuids.length() < 1)
			return null;
		
		String[] uuidList = uuids.split(",");
		for (int i = 0, length = uuidList.length; i < length; i++)
			if (BasicAccount.isMatch(this, uuidList[i], id))
				return BasicAccount.create(this, uuidList[i]);
		
		return null;
	}
	
	public BasicAccount[] getAccounts()
	{
		String uuids = getSharedPreferences().getString("accountUuids", null);
		if (uuids == null || uuids.length() < 1)
			return new BasicAccount[] {};
		
		String[] uuidList = uuids.split(",");
		BasicAccount[] accounts = new BasicAccount[uuidList.length];
		for (int i = 0, length = uuidList.length; i < length; i++)
			accounts[i] = BasicAccount.create(this, uuidList[i]);
		
		return accounts;
	}
	
	public boolean anyAccounts()
	{
		String uuids = getSharedPreferences().getString("accountUuids", null);
		return (uuids == null || uuids.length() < 1);
	}
	
	public BasicAccount getDefaultAccount()
	{
		BasicAccount[] accounts = getAccounts();
		if (accounts.length > 0)
			return accounts[0];
		return null;
	}

	public void clear()
	{
		getSharedPreferences().edit().clear().commit();
	}
	
	public void dump()
	{
		if (App.getConfig().logToConsole())
			for (String key : getSharedPreferences().getAll().keySet())
				App.logv(key + " = " + getSharedPreferences().getAll().get(key));
	}
	
	public boolean isNewAccount(BasicAccount account)
	{
        return !getSharedPreferences().getString("accountUuids", "").contains(account.getUuid());
	}
	
	public synchronized long reserveAccountId()
	{
        // Get a unique serial number for the account
        long counter = getSharedPreferences().getLong("accountCounter", 1);
        
        // Increment number
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putLong("accountCounter", counter + 1);
        editor.commit();
        
        return counter;
	}
	
	public void addAccount(BasicAccount account)
	{
        String accountUuids = getSharedPreferences().getString("accountUuids", "");
        accountUuids += (accountUuids.length() != 0 ? "," : "") + account.getUuid();
        
        // Update preferences with added account
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString("accountUuids", accountUuids);
        editor.commit();
	}
	
	public WidgetInfo getWidget(int widgetId)
	{
		WidgetInfo info = null;
        SharedPreferences shp = getSharedPreferences();
        
        if (shp.contains("widgetAccountUuid." + widgetId))
        {
        	String uuid = shp.getString("widgetAccountUuid." + widgetId, null);
    		String flatCn = shp.getString("widgetAccountCn." + widgetId, null);
    		
        	if (uuid != null && flatCn != null)
        	{
	    		BasicAccount account = getAccount(uuid);
	    		if (account != null)
	    		{
	    			info = new WidgetInfo();
	    			
	    			info.account = account;
	    			info.widgetId = widgetId;
	    			info.componentName = ComponentName.unflattenFromString(flatCn);
	    		}
        	}
        }
		
		return info;
	}
	
	public int[] getAllWidgetIds(Context context, BasicAccount account)
	{
		Set<String> prefSet = Preferences.get(context).getSharedPreferences().getAll().keySet();
		List<Integer> widgetIds = new ArrayList<Integer>(); 
		
		for (String key : prefSet)
		{
			String uuidPrefix = "widgetAccountUuid.";
			if (key.startsWith(uuidPrefix))
			{
				String value = getSharedPreferences().getString(key, null);
				if (value != null && value.equals(account.getUuid()))
					widgetIds.add(Integer.parseInt(key.substring(uuidPrefix.length())));
			}
		}
		
		int n = widgetIds.size();
		int[] wiArray = new int[n];
		
		for (int i = 0; i < n; i++)
			wiArray[i] = widgetIds.get(i);
        
		return wiArray;
	}
	
	public void addWidget(WidgetInfo info)
	{
        SharedPreferences.Editor editor = getSharedPreferences().edit();
    	editor.putString("widgetAccountUuid." + info.widgetId, info.account.getUuid());
    	editor.putString("widgetAccountCn." + info.widgetId, info.componentName.flattenToString());
        editor.commit();
	}
	
	public void deleteWidget(int widgetId)
	{
        SharedPreferences.Editor editor = getSharedPreferences().edit();
    	editor.remove("widgetAccountUuid." + widgetId);
    	editor.remove("widgetAccountCn." + widgetId);
        editor.commit();
	}
	
	private String encrypt(String plaintext, Cipher cipher)
			throws GeneralSecurityException, UnsupportedEncodingException
	{
        return Base64.encode(cipher.doFinal(plaintext.getBytes(UTF8)));
	}
	
	private String decrypt(String ciphertext, Cipher cipher)
			throws GeneralSecurityException, UnsupportedEncodingException,
			Base64DecoderException
	{
        return new String(cipher.doFinal(Base64.decode(ciphertext)), UTF8);
	}
	
	public boolean needsEncryptionRefresh(String key)
	{
		String ciphertext = getSharedPreferences().getString(key, null);
		if (ciphertext == null)
			return false;
		
		return !ciphertext.startsWith("#3");
	}
	
	public String getEncrypted(String key) throws EncryptionException
	{
		String ciphertext = getSharedPreferences().getString(key, null);
		if (ciphertext == null)
			return null;
		
		// Determine version
		if (ciphertext.startsWith("#3") && ciphertext.length() > 2)
		{
			// Versioned, v3
			
			try
			{
				return decrypt(ciphertext.substring(2), mV3Decryptor);
			}
			catch (UnsupportedEncodingException e)
			{
				throw new EncryptionException(e);
			}
			catch (GeneralSecurityException e)
			{
				throw new EncryptionException(e);
			}
			catch (Base64DecoderException e)
			{
				throw new EncryptionException(e);
			}
		}
		
		return null;
	}
	
	public void putEncrypted(SharedPreferences.Editor editor, String key,
			String plaintext) throws EncryptionException
	{
		String value = null;
		if (plaintext != null)
		{
			String ciphertext = null;
			
			try
			{
				ciphertext = encrypt(plaintext, mV3Encryptor);
			}
			catch (UnsupportedEncodingException e)
			{
				throw new EncryptionException(e);
			}
			catch (GeneralSecurityException e)
			{
				throw new EncryptionException(e);
			}
			
			if (ciphertext != null)
				value = "#3" + ciphertext;
		}
		
		editor.putString(key, value);
	}
	
	public boolean getBoolean(String key, boolean defValue)
	{
		return getSharedPreferences().getBoolean(key, defValue);
	}
	
	public String getString(String key, String defValue)
	{
		return getSharedPreferences().getString(key, defValue);
	}
	
	public int getInt(String key, int defValue)
	{
		return getSharedPreferences().getInt(key, defValue);
	}
	
	public long getLong(String key, long defValue)
	{
		return getSharedPreferences().getLong(key, defValue);
	}
	
	public void set(String key, Object value)
	{
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        
        if (value instanceof String)
        	editor.putString(key, (String)value);
        else if (value instanceof Integer)
        	editor.putInt(key, (Integer)value);
        
        editor.commit();
	}
	
	public synchronized void deleteAccount(Context context, BasicAccount account)
	{
		// Delete account
		account.delete(context);
		
		// Remove from list of accounts
		String[] uuids = getSharedPreferences().getString("accountUuids", "").split(",");
		if (uuids.length < 1)
			return;
		
		StringBuffer sb = new StringBuffer();
		for (String uuid : uuids)
		{
			if (!uuid.equals(account.getUuid()))
			{
				if (sb.length() > 0)
					sb.append(",");
				sb.append(uuid);
			}
		}
		
		// Save changes
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString("accountUuids", sb.toString());
        editor.commit();
	}
}
