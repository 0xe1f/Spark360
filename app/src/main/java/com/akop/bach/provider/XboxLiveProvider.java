/*
 * XboxLiveProvider.java 
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

package com.akop.bach.provider;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.akop.bach.App;
import com.akop.bach.R;
import com.akop.bach.XboxLive;
import com.akop.bach.XboxLive.Achievements;
import com.akop.bach.XboxLive.Beacons;
import com.akop.bach.XboxLive.Events;
import com.akop.bach.XboxLive.Friends;
import com.akop.bach.XboxLive.Games;
import com.akop.bach.XboxLive.Messages;
import com.akop.bach.XboxLive.NotifyStates;
import com.akop.bach.XboxLive.Profiles;
import com.akop.bach.XboxLive.SentMessages;

public class XboxLiveProvider extends ContentProvider
{
	public static final String AUTHORITY = "com.akop.bach.xboxliveprovider";
	
	private static final int PROFILES = 1;
	private static final int PROFILE_ID = 2;
	private static final int GAMES = 3;
	private static final int GAME_ID = 4;
	private static final int ACHIEVEMENTS = 5;
	private static final int ACHIEVEMENT_ID = 6;
	private static final int FRIENDS = 7;
	private static final int FRIEND_ID = 8;
	private static final int MESSAGES = 9;
	private static final int MESSAGE_ID = 10;
	private static final int EVENTS = 11;
	private static final int EVENT_ID = 12;
	private static final int SEARCH_FRIEND_SUGGEST = 13;
	private static final int BEACONS = 14;
	private static final int BEACON_ID = 15;
	private static final int SENT_MESSAGES = 16;
	private static final int SENT_MESSAGE_ID = 17;
	private static final int NOTIFY_STATES = 18;
	private static final int NOTIFY_STATE_ID = 19;
	
	private static final UriMatcher sUriMatcher;
	
	private static final String PROFILES_TABLE_NAME = "profiles";
	private static final String FRIENDS_TABLE_NAME = "friends";
	private static final String GAMES_TABLE_NAME = "games";
	private static final String ACHIEVEMENTS_TABLE_NAME = "achievements";
	private static final String MESSAGES_TABLE_NAME = "messages";
	private static final String EVENTS_TABLE_NAME = "events";
	private static final String BEACONS_TABLE_NAME = "beacons";
	private static final String SENT_MESSAGES_TABLE_NAME = "sent_messages";
	private static final String NOTIFY_STATES_TABLE_NAME = "notify_states";
	
	private static final String DATABASE_NAME = "xboxlive.db";
	
	private static final int DATABASE_VERSION = 27;
	
    private DbHelper mDbHelper;
    
	static
	{
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		
		sUriMatcher.addURI(AUTHORITY, "profiles", PROFILES);
		sUriMatcher.addURI(AUTHORITY, "profiles/#", PROFILE_ID);
		sUriMatcher.addURI(AUTHORITY, "friends", FRIENDS);
		sUriMatcher.addURI(AUTHORITY, "friends/#", FRIEND_ID);
		sUriMatcher.addURI(AUTHORITY, "games", GAMES);
		sUriMatcher.addURI(AUTHORITY, "games/#", GAME_ID);
		sUriMatcher.addURI(AUTHORITY, "achievements", ACHIEVEMENTS);
		sUriMatcher.addURI(AUTHORITY, "achievements/#", ACHIEVEMENT_ID);
		sUriMatcher.addURI(AUTHORITY, "messages", MESSAGES);
		sUriMatcher.addURI(AUTHORITY, "messages/#", MESSAGE_ID);
		sUriMatcher.addURI(AUTHORITY, "events", EVENTS);
		sUriMatcher.addURI(AUTHORITY, "events/#", EVENT_ID);
		sUriMatcher.addURI(AUTHORITY, "beacons", BEACONS);
		sUriMatcher.addURI(AUTHORITY, "beacons/#", BEACON_ID);
		sUriMatcher.addURI(AUTHORITY, "sent_messages", SENT_MESSAGES);
		sUriMatcher.addURI(AUTHORITY, "sent_messages/#", SENT_MESSAGE_ID);
		sUriMatcher.addURI(AUTHORITY, "notify_states", NOTIFY_STATES);
		sUriMatcher.addURI(AUTHORITY, "notify_states/#", NOTIFY_STATE_ID);
		
		sUriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_FRIEND_SUGGEST);
		sUriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_FRIEND_SUGGEST);
	}
	
	private class DbHelper extends SQLiteOpenHelper
	{
		public DbHelper(Context context)
		{
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE " + PROFILES_TABLE_NAME + " ("
					+ Profiles._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ Profiles.UUID + " TEXT UNIQUE NOT NULL, "
					+ Profiles.ACCOUNT_ID + " INTEGER UNIQUE NOT NULL, "
					+ Profiles.ICON_URL + " TEXT NOT NULL, "
					+ Profiles.GAMERTAG + " TEXT NOT NULL, "
					+ Profiles.GAMERSCORE + " INTEGER NOT NULL, "
					+ Profiles.IS_GOLD + " INTEGER NOT NULL, "
					+ Profiles.TIER + " TEXT, "
					+ Profiles.UNREAD_MESSAGES + " INTEGER NOT NULL, "
					+ Profiles.UNREAD_NOTIFICATIONS + " INTEGER NOT NULL, "

                    // These are deprecated as of 07.03.2014
                    + Profiles.POINTS_BALANCE + " INTEGER NOT NULL, "
					+ Profiles.MOTTO + " TEXT, "
					+ Profiles.REP + " INTEGER NOT NULL, "
					+ Profiles.ZONE + " TEXT, "
					+ Profiles.NAME + " TEXT NOT NULL DEFAULT '', "
					+ Profiles.LOCATION + " TEXT NOT NULL DEFAULT '', "
					+ Profiles.BIO + " TEXT NOT NULL DEFAULT '');");
			db.execSQL("CREATE TABLE " + FRIENDS_TABLE_NAME + " ("
					+ Friends._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ Friends.ACCOUNT_ID + " INTEGER NOT NULL, "
					+ Friends.GAMERTAG + " TEXT NOT NULL, "
					+ Friends.ICON_URL + " TEXT NOT NULL, "
					+ Friends.IS_FAVORITE + " INTEGER NOT NULL DEFAULT 0, "
					+ Friends.GAMERSCORE + " INTEGER NOT NULL, "
					+ Friends.STATUS_CODE + " INTEGER NOT NULL, "
					+ Friends.STATUS + " TEXT, "
					+ Friends.MOTTO + " TEXT, "
					+ Friends.CURRENT_ACTIVITY + " TEXT, "
					+ Friends.NAME + " TEXT, "
					+ Friends.LOCATION + " TEXT, "
					+ Friends.BIO + " TEXT, "
					+ Friends.REP + " INTEGER NOT NULL, "
					+ Friends.DELETE_MARKER + " INTEGER NOT NULL, "
					+ Friends.LAST_UPDATED + " INTEGER NOT NULL, "
					+ Friends.TITLE_ID + " INTEGER NOT NULL DEFAULT 0, "
					+ Friends.TITLE_NAME + " TEXT, "
					+ Friends.TITLE_URL + " TEXT, "
					+ "UNIQUE (" + Friends.ACCOUNT_ID + "," + Friends.GAMERTAG + "));");
			db.execSQL("CREATE TABLE " + GAMES_TABLE_NAME + " (" 
					+ Games._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ Games.ACCOUNT_ID + " INTEGER NOT NULL, "
					+ Games.TITLE + " TEXT NOT NULL, "
					+ Games.BOXART_URL + " TEXT, "
					+ Games.UID + " TEXT NOT NULL, "
					+ Games.LAST_PLAYED + " INTEGER NOT NULL,"
					+ Games.POINTS_ACQUIRED + " INTEGER NOT NULL, "
					+ Games.POINTS_TOTAL + " INTEGER NOT NULL, "
					+ Games.ACHIEVEMENTS_UNLOCKED + " INTEGER NOT NULL, "
					+ Games.ACHIEVEMENTS_TOTAL + " INTEGER NOT NULL, "
					+ Games.ACHIEVEMENTS_STATUS + " INTEGER NOT NULL, "
					+ Games.BEACON_SET + " INTEGER NOT NULL DEFAULT 0, "
					+ Games.BEACON_TEXT + " TEXT, "
					+ Games.LAST_UPDATED + " INTEGER NOT NULL, "
					+ Games.GAME_URL + " TEXT, "
					+ Games.INDEX + " INTEGER NOT NULL, "
					+ "UNIQUE (" + Games.ACCOUNT_ID + "," + Games.UID + "));");
			db.execSQL("CREATE TABLE " + ACHIEVEMENTS_TABLE_NAME + " ("
					+ Achievements._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ Achievements.GAME_ID + " INTEGER NOT NULL, "
					+ Achievements.TITLE + " TEXT NOT NULL, "
					+ Achievements.DESCRIPTION + " TEXT NOT NULL, "
					+ Achievements.ICON_URL + " TEXT NOT NULL, "
					+ Achievements.POINTS + " INTEGER NOT NULL, "
					+ Achievements.ACQUIRED + " INTEGER, "
					+ Achievements.LOCKED + " INTEGER NOT NULL, "
					+ Achievements.INDEX + " INTEGER NOT NULL);");
			db.execSQL("CREATE TABLE " + MESSAGES_TABLE_NAME + " ("
					+ Messages._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ Messages.ACCOUNT_ID + " INTEGER NOT NULL, "
					+ Messages.UID + " INTEGER NOT NULL, "
					+ Messages.SENDER + " TEXT, "
					+ Messages.GAMERPIC + " TEXT, "
					+ Messages.TYPE + " INTEGER NOT NULL, "
					+ Messages.BODY + " TEXT NOT NULL, "
					+ Messages.SENT + " INTEGER NOT NULL, "
					+ Messages.IS_DIRTY + " INTEGER NOT NULL, "
					+ Messages.IS_READ + " INTEGER NOT NULL, "
					+ Messages.DELETE_MARKER + " INTEGER NOT NULL, "
					+ "UNIQUE (" + Messages.ACCOUNT_ID + "," + Messages.UID + "));");
			db.execSQL("CREATE TABLE " + EVENTS_TABLE_NAME + " ("
					+ Events._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ Events.ACCOUNT_ID + " INTEGER NOT NULL, "
					+ Events.TITLE + " TEXT, "
					+ Events.DESCRIPTION + " TEXT, "
					+ Events.DATE + " INTEGER NOT NULL, "
					+ Events.FLAGS + " INTEGER NOT NULL DEFAULT 0);");
			db.execSQL("CREATE TABLE " + BEACONS_TABLE_NAME + " ("
					+ Beacons._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ Beacons.ACCOUNT_ID + " INTEGER NOT NULL, "
					+ Beacons.FRIEND_ID + " INTEGER NOT NULL, "
					+ Beacons.TITLE_ID + " TEXT NOT NULL, "
					+ Beacons.TITLE_NAME + " TEXT, "
					+ Beacons.TITLE_BOXART + " TEXT, "
					+ Beacons.TEXT + " TEXT);");
			db.execSQL("CREATE TABLE " + SENT_MESSAGES_TABLE_NAME + " ("
					+ SentMessages._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ SentMessages.ACCOUNT_ID + " INTEGER NOT NULL, "
					+ SentMessages.RECIPIENTS + " TEXT, "
					+ SentMessages.TYPE + " INTEGER NOT NULL, "
					+ SentMessages.PREVIEW + " TEXT NOT NULL, "
					+ SentMessages.BODY + " TEXT NOT NULL, "
					+ SentMessages.SENT + " INTEGER NOT NULL);");
			db.execSQL("CREATE TABLE " + NOTIFY_STATES_TABLE_NAME + " ("
					+ NotifyStates._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ NotifyStates.ACCOUNT_ID + " INTEGER NOT NULL, "
					+ NotifyStates.LAST_UPDATED + " INTEGER NOT NULL DEFAULT 0, "
					+ NotifyStates.TYPE + " INTEGER NOT NULL, "
					+ NotifyStates.DATA + " TEXT NOT NULL DEFAULT '');");
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			if (App.getConfig().logToConsole())
				App.logv("XboxLiveProvider: Upgrading database from version "
						+ oldVersion + " to " + newVersion);
			
			boolean upgraded = false;
			if (oldVersion < 13)
			{
				if (App.getConfig().logToConsole())
					App.logv("XboxLiveProvider: upgrading for version 13");
				
				db.execSQL("ALTER TABLE " + PROFILES_TABLE_NAME + " ADD COLUMN "
						+ Profiles.ZONE + " TEXT");
				db.execSQL("ALTER TABLE " + GAMES_TABLE_NAME + " ADD COLUMN "
						+ Games.INDEX + " INTEGER NOT NULL DEFAULT 0");
				
				upgraded = true;
			}
			
			if (oldVersion < 15)
			{
				if (App.getConfig().logToConsole())
					App.logv("XboxLiveProvider: upgrading for version 15");
				
				db.execSQL("ALTER TABLE " + FRIENDS_TABLE_NAME + " ADD COLUMN "
						+ Friends.IS_FAVORITE + " INTEGER NOT NULL DEFAULT 0");
				
				upgraded = true;
			}
			
			if (oldVersion < 16)
			{
				if (App.getConfig().logToConsole())
					App.logv("XboxLiveProvider: upgrading for version 16");
				
				db.execSQL("ALTER TABLE " + PROFILES_TABLE_NAME + " ADD COLUMN "
						+ Profiles.TIER + " TEXT");
				
				upgraded = true;
			}
			
			if (oldVersion < 17)
			{
				if (App.getConfig().logToConsole())
					App.logv("XboxLiveProvider: upgrading for version 17");
				
				db.execSQL("CREATE TABLE " + EVENTS_TABLE_NAME + " ("
						+ Events._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
						+ Events.ACCOUNT_ID + " INTEGER NOT NULL, "
						+ Events.TITLE + " TEXT, "
						+ Events.DESCRIPTION + " TEXT, "
						+ Events.DATE + " INTEGER NOT NULL, "
						+ Events.FLAGS + " INTEGER NOT NULL DEFAULT 0);");
				
				upgraded = true;
			}
			
			if (oldVersion < 18)
			{
				if (App.getConfig().logToConsole())
					App.logv("XboxLiveProvider: upgrading for version 18");
				
				db.execSQL("DELETE FROM " + GAMES_TABLE_NAME + ";");
				db.execSQL("DELETE FROM " + ACHIEVEMENTS_TABLE_NAME + ";");
				
				upgraded = true;
			}
			
			if (oldVersion < 19)
			{
				if (App.getConfig().logToConsole())
					App.logv("XboxLiveProvider: upgrading for version 19");
				
				db.execSQL("ALTER TABLE " + GAMES_TABLE_NAME + " ADD COLUMN "
						+ Games.BEACON_SET + " INTEGER NOT NULL DEFAULT 0");
				db.execSQL("ALTER TABLE " + GAMES_TABLE_NAME + " ADD COLUMN "
						+ Games.BEACON_TEXT + " TEXT");
				
				upgraded = true;
			}
			
			if (oldVersion < 20)
			{
				if (App.getConfig().logToConsole())
					App.logv("XboxLiveProvider: upgrading for version 20");
				
				db.execSQL("CREATE TABLE " + BEACONS_TABLE_NAME + " ("
						+ Beacons._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
						+ Beacons.ACCOUNT_ID + " INTEGER NOT NULL, "
						+ Beacons.FRIEND_ID + " INTEGER NOT NULL, "
						+ Beacons.TITLE_ID + " TEXT NOT NULL, "
						+ Beacons.TITLE_NAME + " TEXT, "
						+ Beacons.TITLE_BOXART + " TEXT, "
						+ Beacons.TEXT + " TEXT);");
				
				upgraded = true;
			}
			
			if (oldVersion < 21)
			{
				if (App.getConfig().logToConsole())
					App.logv("XboxLiveProvider: upgrading for version 20");
				
				db.execSQL("ALTER TABLE " + FRIENDS_TABLE_NAME + " ADD COLUMN "
						+ Friends.TITLE_ID + " INTEGER NOT NULL DEFAULT 0");
				db.execSQL("ALTER TABLE " + FRIENDS_TABLE_NAME + " ADD COLUMN "
						+ Friends.TITLE_NAME + " TEXT");
				db.execSQL("ALTER TABLE " + FRIENDS_TABLE_NAME + " ADD COLUMN "
						+ Friends.TITLE_URL + " TEXT");
				
				upgraded = true;
			}
			
			if (oldVersion < 22)
			{
				if (App.getConfig().logToConsole())
					App.logv("XboxLiveProvider: upgrading for version 21");
				
				db.execSQL("UPDATE " + GAMES_TABLE_NAME + " SET " 
						+ Games.ACHIEVEMENTS_STATUS + "= 1");
				
				upgraded = true;
			}
			
			if (oldVersion < 23)
			{
				if (App.getConfig().logToConsole())
					App.logv("XboxLiveProvider: upgrading for version 22");
				
				db.execSQL("ALTER TABLE " + MESSAGES_TABLE_NAME + " ADD COLUMN "
						+ Messages.GAMERPIC + " TEXT");
				
				upgraded = true;
			}
			
			if (oldVersion < 24)
			{
				if (App.getConfig().logToConsole())
					App.logv("XboxLiveProvider: upgrading for version 23");
				
				db.execSQL("CREATE TABLE " + SENT_MESSAGES_TABLE_NAME + " ("
						+ SentMessages._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
						+ SentMessages.ACCOUNT_ID + " INTEGER NOT NULL, "
						+ SentMessages.RECIPIENTS + " TEXT, "
						+ SentMessages.TYPE + " INTEGER NOT NULL, "
						+ SentMessages.PREVIEW + " TEXT NOT NULL, "
						+ SentMessages.BODY + " TEXT NOT NULL, "
						+ SentMessages.SENT + " INTEGER NOT NULL);");
				
				upgraded = true;
			}
			
			if (oldVersion < 25)
			{
				if (App.getConfig().logToConsole())
					App.logv("XboxLiveProvider: upgrading for version 24");
				
				db.execSQL("ALTER TABLE " + PROFILES_TABLE_NAME + " ADD COLUMN "
						+ Profiles.NAME + " TEXT NOT NULL DEFAULT ''");
				db.execSQL("ALTER TABLE " + PROFILES_TABLE_NAME + " ADD COLUMN "
						+ Profiles.LOCATION + " TEXT NOT NULL DEFAULT ''");
				db.execSQL("ALTER TABLE " + PROFILES_TABLE_NAME + " ADD COLUMN "
						+ Profiles.BIO + " TEXT NOT NULL DEFAULT ''");
				
				upgraded = true;
			}
			
			if (oldVersion < 26)
			{
				if (App.getConfig().logToConsole())
					App.logv("XboxLiveProvider: upgrading for version 25");
				
				db.execSQL("CREATE TABLE " + NOTIFY_STATES_TABLE_NAME + " ("
						+ NotifyStates._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
						+ NotifyStates.ACCOUNT_ID + " INTEGER NOT NULL, "
						+ NotifyStates.TYPE + " INTEGER NOT NULL, "
						+ NotifyStates.DATA + " TEXT NOT NULL DEFAULT '');");
				
				upgraded = true;
			}
			
			if (oldVersion < 27)
			{
				if (App.getConfig().logToConsole())
					App.logv("XboxLiveProvider: upgrading for version 26");
				
				db.execSQL("ALTER TABLE " + NOTIFY_STATES_TABLE_NAME + " ADD COLUMN "
						+ NotifyStates.LAST_UPDATED + " INTEGER NOT NULL DEFAULT 0");
				
				upgraded = true;
			}
			
			if (!upgraded)
			{
				if (App.getConfig().logToConsole())
					App.logv("XboxLiveProvider: Recreating structure");
				
				db.execSQL("DROP TABLE IF EXISTS " + ACHIEVEMENTS_TABLE_NAME);
				db.execSQL("DROP TABLE IF EXISTS " + GAMES_TABLE_NAME);
				db.execSQL("DROP TABLE IF EXISTS " + FRIENDS_TABLE_NAME);
				db.execSQL("DROP TABLE IF EXISTS " + MESSAGES_TABLE_NAME);
				db.execSQL("DROP TABLE IF EXISTS " + PROFILES_TABLE_NAME);
				db.execSQL("DROP TABLE IF EXISTS " + EVENTS_TABLE_NAME);
				db.execSQL("DROP TABLE IF EXISTS " + BEACONS_TABLE_NAME);
				db.execSQL("DROP TABLE IF EXISTS " + SENT_MESSAGES_TABLE_NAME);
				db.execSQL("DROP TABLE IF EXISTS " + NOTIFY_STATES_TABLE_NAME);
				
				onCreate(db);
			}
		}
	}
    
	@Override
	public String getType(Uri uri)
	{
		switch (sUriMatcher.match(uri))
		{
		case PROFILES: return Profiles.CONTENT_TYPE;
		case PROFILE_ID: return Profiles.CONTENT_ITEM_TYPE;
		case FRIENDS: return Friends.CONTENT_TYPE;
		case FRIEND_ID: return Friends.CONTENT_ITEM_TYPE;
		case GAMES: return Games.CONTENT_TYPE;
		case GAME_ID: return Games.CONTENT_ITEM_TYPE;
		case ACHIEVEMENTS: return Achievements.CONTENT_TYPE;
		case ACHIEVEMENT_ID: return Achievements.CONTENT_ITEM_TYPE;
		case MESSAGES: return Messages.CONTENT_TYPE;
		case MESSAGE_ID: return Messages.CONTENT_ITEM_TYPE;
		case EVENTS: return Events.CONTENT_TYPE;
		case EVENT_ID: return Events.CONTENT_ITEM_TYPE;
		case BEACONS: return Beacons.CONTENT_TYPE;
		case BEACON_ID: return Beacons.CONTENT_ITEM_TYPE;
		case SENT_MESSAGES: return SentMessages.CONTENT_TYPE;
		case SENT_MESSAGE_ID: return SentMessages.CONTENT_ITEM_TYPE;
		case NOTIFY_STATES: return NotifyStates.CONTENT_TYPE;
		case NOTIFY_STATE_ID: return NotifyStates.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues)
	{
		ContentValues values;
		SQLiteDatabase db;
		long id;
		Uri newUri = null;
		
		switch(sUriMatcher.match(uri))
		{
		case PROFILES:
			if (initialValues == null)
				throw new IllegalArgumentException("Missing profile information");
			
			values = new ContentValues(initialValues);
			
			if (!values.containsKey(Profiles.ACCOUNT_ID))
				throw new SQLException("Account ID not specified");
			if (!values.containsKey(Profiles.UUID))
				throw new SQLException("UUID not specified");
			if (!values.containsKey(Profiles.GAMERTAG))
				throw new SQLException("Gamertag not specified");
			if (!values.containsKey(Profiles.IS_GOLD))
				throw new SQLException("Gamertag not specified");
			if (!values.containsKey(Profiles.REP))
				values.put(Profiles.REP, 0);
			
			db = mDbHelper.getWritableDatabase();
			if ((id = db.insert(PROFILES_TABLE_NAME, Profiles.ACCOUNT_ID, values)) > 0)
			{
				newUri = ContentUris.withAppendedId(Profiles.CONTENT_URI, id);
				getContext().getContentResolver().notifyChange(newUri, null);
			}
			
			break;
			
		case FRIENDS:
			if (initialValues == null)
				throw new IllegalArgumentException("Missing friend information");
			
			values = new ContentValues(initialValues);

			if (!values.containsKey(Friends.GAMERTAG))
				throw new SQLException("Gamertag not specified");
			if (!values.containsKey(Friends.ACCOUNT_ID))
				throw new SQLException("Profile ID not specified");
			if (!values.containsKey(Friends.GAMERSCORE))
				throw new SQLException("Gamer points not specified");
			if (!values.containsKey(Friends.STATUS_CODE))
				throw new SQLException("Status code not specified");
			if (!values.containsKey(Friends.REP))
				values.put(Friends.REP, 0);
			if (!values.containsKey(Friends.LAST_UPDATED))
				values.put(Friends.LAST_UPDATED, 0);
			if (!values.containsKey(Friends.DELETE_MARKER))
				values.put(Friends.DELETE_MARKER, 0);
			
			db = mDbHelper.getWritableDatabase();
			if ((id = db.insert(FRIENDS_TABLE_NAME, Friends.GAMERTAG, values)) > 0)
			{
				newUri = ContentUris.withAppendedId(Friends.CONTENT_URI, id);
				//NOTE: Suppressing notifyChange here - the parser will call it
				//      at the end of a bulkInsert
				//getContext().getContentResolver().notifyChange(newUri, null);
			}
			
			break;

		case GAMES:
			if (initialValues == null)
				throw new IllegalArgumentException("Missing game information");
			
			values = new ContentValues(initialValues);
			
			if (!values.containsKey(Games.ACCOUNT_ID))
				throw new SQLException("Profile ID not specified");
			if (!values.containsKey(Games.TITLE))
				throw new SQLException("Game title not specified");
			if (!values.containsKey(Games.UID))
				throw new SQLException("UID not specified");
			if (!values.containsKey(Games.LAST_PLAYED))
				throw new SQLException("Last played date not specified");
			if (!values.containsKey(Games.LAST_UPDATED))
				throw new SQLException("Last update date not specified");
			
			if (!values.containsKey(Games.ACHIEVEMENTS_STATUS))
				values.put(Games.ACHIEVEMENTS_STATUS, 1);
			if (!values.containsKey(Games.ACHIEVEMENTS_UNLOCKED))
				values.put(Games.ACHIEVEMENTS_UNLOCKED, 0);
			if (!values.containsKey(Games.ACHIEVEMENTS_TOTAL))
				values.put(Games.ACHIEVEMENTS_TOTAL, 0);
			if (!values.containsKey(Games.POINTS_ACQUIRED))
				values.put(Games.POINTS_ACQUIRED, 0);
			if (!values.containsKey(Games.POINTS_TOTAL))
				values.put(Games.POINTS_TOTAL, 0);
			
			db = mDbHelper.getWritableDatabase();
			if ((id = db.insert(GAMES_TABLE_NAME, Games.TITLE, values)) > 0)
			{
				newUri = ContentUris.withAppendedId(Games.CONTENT_URI, id);
				//NOTE: Suppressing notifyChange here - the parser will call it
				//      at the end of a bulkInsert
				//getContext().getContentResolver().notifyChange(newUri, null);
			}
			
			break;
			
		case ACHIEVEMENTS:
			if (initialValues == null)
				throw new IllegalArgumentException("Missing achievement information");
			
			values = new ContentValues(initialValues);
			
			if (!values.containsKey(Achievements.GAME_ID))
				throw new SQLException("Game ID not specified");
			if (!values.containsKey(Achievements.TITLE))
				throw new SQLException("Achievement title not specified");
			if (!values.containsKey(Achievements.DESCRIPTION))
				throw new SQLException("Achievement description not specified");
			if (!values.containsKey(Achievements.POINTS))
				throw new SQLException("Gamer points not specified");
			if (!values.containsKey(Achievements.LOCKED))
				throw new SQLException("Achievement lock status not specified");
			if (!values.containsKey(Achievements.INDEX))
				values.put(Achievements.INDEX, 0);
			
			if (!values.containsKey(Achievements.ACQUIRED))
				values.put(Achievements.ACQUIRED, 0);
			
			db = mDbHelper.getWritableDatabase();
			if ((id = db.insert(ACHIEVEMENTS_TABLE_NAME, Achievements.TITLE, values)) > 0)
			{
				newUri = ContentUris.withAppendedId(Achievements.CONTENT_URI, id);
				//NOTE: Suppressing notifyChange here - the parser will call it
				//      at the end of a bulkInsert
				//getContext().getContentResolver().notifyChange(newUri, null);
			}

			break;
			
		case MESSAGES:
			if (initialValues == null)
				throw new IllegalArgumentException("Missing message information");
			
			values = new ContentValues(initialValues);
			
			if (!values.containsKey(Messages.ACCOUNT_ID))
				throw new SQLException("Profile ID not specified");
			if (!values.containsKey(Messages.UID))
				throw new SQLException("UID not specified");
			if (!values.containsKey(Messages.SENT))
				throw new SQLException("Received date not specified");
			if (!values.containsKey(Messages.SENDER))
				throw new SQLException("Sender not specified");
			
			if (!values.containsKey(Messages.IS_DIRTY))
				values.put(Messages.IS_DIRTY, 1);
			if (!values.containsKey(Messages.IS_READ))
				values.put(Messages.IS_READ, 0);
			if (!values.containsKey(Messages.TYPE))
				values.put(Messages.TYPE, 0);
			
			db = mDbHelper.getWritableDatabase();
			if ((id = db.insert(MESSAGES_TABLE_NAME, Messages.UID, values)) > 0)
			{
				newUri = ContentUris.withAppendedId(Messages.CONTENT_URI, id);
				//NOTE: Suppressing notifyChange here - the parser will call it
				//      at the end of a bulkInsert
				//getContext().getContentResolver().notifyChange(newUri, null);
			}

			break;
		
		case EVENTS:
			if (initialValues == null)
				throw new IllegalArgumentException("Missing event information");
			
			values = new ContentValues(initialValues);
			
			if (!values.containsKey(Events.ACCOUNT_ID))
				throw new SQLException("Profile ID not specified");
			if (!values.containsKey(Events.TITLE))
				throw new SQLException("Title not specified");
			if (!values.containsKey(Events.DATE))
				throw new SQLException("Date not specified");
			
			db = mDbHelper.getWritableDatabase();
			if ((id = db.insert(EVENTS_TABLE_NAME, Events.DESCRIPTION, values)) > 0)
			{
				newUri = ContentUris.withAppendedId(Events.CONTENT_URI, id);
				//NOTE: Suppressing notifyChange here - the parser will call it
				//      at the end of a bulkInsert
				//getContext().getContentResolver().notifyChange(newUri, null);
			}

			break;
		
		case BEACONS:
			if (initialValues == null)
				throw new IllegalArgumentException("Missing beacon information");
			
			values = new ContentValues(initialValues);
			
			if (!values.containsKey(Beacons.ACCOUNT_ID))
				throw new SQLException("Profile ID not specified");
			if (!values.containsKey(Beacons.FRIEND_ID))
				throw new SQLException("Friend ID specified");
			if (!values.containsKey(Beacons.TITLE_ID))
				throw new SQLException("Title ID specified");
			
			db = mDbHelper.getWritableDatabase();
			if ((id = db.insert(BEACONS_TABLE_NAME, null, values)) > 0)
			{
				newUri = ContentUris.withAppendedId(Beacons.CONTENT_URI, id);
				//NOTE: Suppressing notifyChange here - the parser will call it
				//      at the end of a bulkInsert
				//getContext().getContentResolver().notifyChange(newUri, null);
			}
			
			break;
		
		case SENT_MESSAGES:
			
			if (initialValues == null)
				throw new IllegalArgumentException("Missing message information");
			
			values = new ContentValues(initialValues);
			
			if (!values.containsKey(SentMessages.ACCOUNT_ID))
				throw new SQLException("Profile ID not specified");
			if (!values.containsKey(SentMessages.SENT))
				throw new SQLException("Sent date not specified");
			if (!values.containsKey(SentMessages.RECIPIENTS))
				throw new SQLException("Recipients not specified");
			
			if (!values.containsKey(SentMessages.TYPE))
				values.put(SentMessages.TYPE, XboxLive.MESSAGE_TEXT);
			
			db = mDbHelper.getWritableDatabase();
			if ((id = db.insert(SENT_MESSAGES_TABLE_NAME, SentMessages.BODY, values)) > 0)
			{
				newUri = ContentUris.withAppendedId(SentMessages.CONTENT_URI, id);
				getContext().getContentResolver().notifyChange(newUri, null);
			}

			break;
		
		case NOTIFY_STATES:
			
			if (initialValues == null)
				throw new IllegalArgumentException("Missing notify state information");
			
			values = new ContentValues(initialValues);
			
			if (!values.containsKey(NotifyStates.ACCOUNT_ID))
				throw new SQLException("Profile ID not specified");
			if (!values.containsKey(NotifyStates.TYPE))
				throw new SQLException("Type not specified");
			if (!values.containsKey(NotifyStates.LAST_UPDATED))
				values.put(NotifyStates.LAST_UPDATED, System.currentTimeMillis());
			
			db = mDbHelper.getWritableDatabase();
			
			if ((id = db.insert(NOTIFY_STATES_TABLE_NAME, null, values)) > 0)
			{
				newUri = ContentUris.withAppendedId(NotifyStates.CONTENT_URI, id);
				getContext().getContentResolver().notifyChange(newUri, null);
			}

			break;
		
		default:
			throw new IllegalArgumentException("Unrecognized URI" + uri);
		}
		
		if (newUri != null)
			return newUri;

		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int type = sUriMatcher.match(uri);
        int count;
        String id;
        boolean notify = false;
        
		switch (type)
		{
		case PROFILES:
			count = db.delete(PROFILES_TABLE_NAME, selection, selectionArgs);
			break;
		case PROFILE_ID:
			id = uri.getPathSegments().get(1);
			count = db.delete(PROFILES_TABLE_NAME,
					Profiles._ID + "=" + id
							+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
									+ ')' : ""), selectionArgs);
			break;
		case FRIENDS:
			count = db.delete(FRIENDS_TABLE_NAME, selection, selectionArgs);
			break;
		case FRIEND_ID:
			id = uri.getPathSegments().get(1);
			count = db.delete(FRIENDS_TABLE_NAME,
					Friends._ID
							+ "="
							+ id
							+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
									+ ')' : ""), selectionArgs);
			break;
		case GAMES:
			count = db.delete(GAMES_TABLE_NAME, selection, selectionArgs);
			break;
		case GAME_ID:
			id = uri.getPathSegments().get(1);
			count = db.delete(GAMES_TABLE_NAME,
					Games._ID
							+ "="
							+ id
							+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
									+ ')' : ""), selectionArgs);
			break;
		case ACHIEVEMENTS:
			count = db.delete(ACHIEVEMENTS_TABLE_NAME, selection, selectionArgs);
			break;
		case ACHIEVEMENT_ID:
			id = uri.getPathSegments().get(1);
			count = db.delete(ACHIEVEMENTS_TABLE_NAME,
					Achievements._ID
							+ "="
							+ id
							+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
									+ ')' : ""), selectionArgs);
			break;
		case MESSAGES:
			count = db.delete(MESSAGES_TABLE_NAME, selection, selectionArgs);
			break;
		case MESSAGE_ID:
			id = uri.getPathSegments().get(1);
			count = db.delete(MESSAGES_TABLE_NAME,
					Messages._ID
							+ "="
							+ id
							+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
									+ ')' : ""), selectionArgs);
			break;
		case EVENTS:
			count = db.delete(EVENTS_TABLE_NAME, selection, selectionArgs);
			break;
		case EVENT_ID:
			id = uri.getPathSegments().get(1);
			count = db.delete(EVENTS_TABLE_NAME,
					Events._ID
							+ "="
							+ id
							+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
									+ ')' : ""), selectionArgs);
			break;
		case BEACONS:
			count = db.delete(BEACONS_TABLE_NAME, selection, selectionArgs);
			break;
		case BEACON_ID:
			id = uri.getPathSegments().get(1);
			count = db.delete(BEACONS_TABLE_NAME,
					Beacons._ID
							+ "="
							+ id
							+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
									+ ')' : ""), selectionArgs);
			break;
		case SENT_MESSAGES:
			count = db.delete(SENT_MESSAGES_TABLE_NAME, selection, selectionArgs);
			notify = true;
			break;
		case SENT_MESSAGE_ID:
			id = uri.getPathSegments().get(1);
			count = db.delete(SENT_MESSAGES_TABLE_NAME,
					SentMessages._ID
							+ "="
							+ id
							+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
									+ ')' : ""), selectionArgs);
			notify = true;
			break;
		case NOTIFY_STATES:
			count = db.delete(NOTIFY_STATES_TABLE_NAME, selection, selectionArgs);
			notify = true;
			break;
		case NOTIFY_STATE_ID:
			id = uri.getPathSegments().get(1);
			count = db.delete(NOTIFY_STATES_TABLE_NAME,
					NotifyStates._ID + "=" + id + 
					(!TextUtils.isEmpty(selection) 
							? " AND (" + selection + ')' : ""), selectionArgs);
			
			notify = true;
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		
		if (notify)
			getContext().getContentResolver().notifyChange(uri, null);
		
		return count;
	}

	@Override
	public boolean onCreate()
	{
		mDbHelper = new DbHelper(getContext());
		return true;
	}
	
	private Cursor getFriendSuggestions(String query)
	{
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(FRIENDS_TABLE_NAME);
		
		SQLiteDatabase db = mDbHelper.getReadableDatabase();
		return qb.query(db, 
				new String[] {
					Friends._ID,
					Friends.GAMERTAG + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1,
					"'" + getContext().getString(R.string.xbox_live_friend)
							+ "' AS " + SearchManager.SUGGEST_COLUMN_TEXT_2,
					Friends._ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
				},
				Friends.GAMERTAG + " LIKE '%'||?||'%'", new String[] { query }, 
				null, null,
				Friends.GAMERTAG + " COLLATE NOCASE ASC");
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder)
	{
		Cursor c;
		int match = sUriMatcher.match(uri); 
		String orderBy = sortOrder;
		
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		switch (match)
		{
        case SEARCH_FRIEND_SUGGEST:
			if (selectionArgs == null)
				throw new IllegalArgumentException(
						"selectionArgs must be provided for the Uri: " + uri);
			
			return getFriendSuggestions(selectionArgs[0]);
		case GAMES:
			qb.setTables(GAMES_TABLE_NAME);
			if (TextUtils.isEmpty(sortOrder))
				orderBy = Games.DEFAULT_SORT_ORDER;
			break;
		case GAME_ID:
			qb.setTables(GAMES_TABLE_NAME);
			qb.appendWhere(Games._ID + "=" + uri.getPathSegments().get(1));
			break;
		case PROFILES:
			qb.setTables(PROFILES_TABLE_NAME);
			if (TextUtils.isEmpty(sortOrder))
				orderBy = Profiles.DEFAULT_SORT_ORDER;
			break;
		case PROFILE_ID:
			qb.setTables(PROFILES_TABLE_NAME);
			qb.appendWhere(Profiles._ID + "=" + uri.getPathSegments().get(1));
			break;
		case FRIENDS:
			qb.setTables(FRIENDS_TABLE_NAME);
			if (TextUtils.isEmpty(sortOrder))
				orderBy = Friends.DEFAULT_SORT_ORDER;
			break;
		case FRIEND_ID:
			qb.setTables(FRIENDS_TABLE_NAME);
			qb.appendWhere(Friends._ID + "=" + uri.getPathSegments().get(1));
			break;
		case ACHIEVEMENTS:
			qb.setTables(ACHIEVEMENTS_TABLE_NAME);
			if (TextUtils.isEmpty(sortOrder))
				orderBy = Achievements.DEFAULT_SORT_ORDER;
			break;
		case ACHIEVEMENT_ID:
			qb.setTables(ACHIEVEMENTS_TABLE_NAME);
			qb.appendWhere(Achievements._ID + "=" + uri.getPathSegments().get(1));
			break;
		case MESSAGES:
			qb.setTables(MESSAGES_TABLE_NAME);
			if (TextUtils.isEmpty(sortOrder))
				orderBy = Messages.DEFAULT_SORT_ORDER;
			break;
		case MESSAGE_ID:
			qb.setTables(MESSAGES_TABLE_NAME);
			qb.appendWhere(Messages._ID + "=" + uri.getPathSegments().get(1));
			break;
		case EVENTS:
			qb.setTables(EVENTS_TABLE_NAME);
			if (TextUtils.isEmpty(sortOrder))
				orderBy = Events.DEFAULT_SORT_ORDER;
			break;
		case EVENT_ID:
			qb.setTables(EVENTS_TABLE_NAME);
			qb.appendWhere(Events._ID + "=" + uri.getPathSegments().get(1));
			break;
		case BEACONS:
			qb.setTables(BEACONS_TABLE_NAME);
			if (TextUtils.isEmpty(sortOrder))
				orderBy = Beacons.DEFAULT_SORT_ORDER;
			break;
		case BEACON_ID:
			qb.setTables(BEACONS_TABLE_NAME);
			qb.appendWhere(Beacons._ID + "=" + uri.getPathSegments().get(1));
			break;
		case SENT_MESSAGES:
			qb.setTables(SENT_MESSAGES_TABLE_NAME);
			if (TextUtils.isEmpty(sortOrder))
				orderBy = SentMessages.DEFAULT_SORT_ORDER;
			break;
		case SENT_MESSAGE_ID:
			qb.setTables(SENT_MESSAGES_TABLE_NAME);
			qb.appendWhere(SentMessages._ID + "=" + uri.getPathSegments().get(1));
			break;
		case NOTIFY_STATES:
			qb.setTables(NOTIFY_STATES_TABLE_NAME);
			if (TextUtils.isEmpty(sortOrder))
				orderBy = NotifyStates.DEFAULT_SORT_ORDER;
			break;
		case NOTIFY_STATE_ID:
			qb.setTables(NOTIFY_STATES_TABLE_NAME);
			qb.appendWhere(NotifyStates._ID + "=" + uri.getPathSegments().get(1));
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// Get the database and run the query
		SQLiteDatabase db = mDbHelper.getReadableDatabase();
		c = qb.query(db, projection, selection, selectionArgs, 
				null, null, 
				orderBy);
		
		// Tell the cursor which URI to watch, so it knows when its source data
		// changes
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs)
	{
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		int type = sUriMatcher.match(uri);
		int count;
		String id;
		boolean notify = false;
		
		switch (type)
		{
		case PROFILES:
			count = db.update(PROFILES_TABLE_NAME, values, selection, selectionArgs);
			break;
		case PROFILE_ID:
			id = uri.getPathSegments().get(1);
			count = db.update(PROFILES_TABLE_NAME, values,
					Profiles._ID
							+ "="
							+ id
							+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
									+ ')' : ""), selectionArgs);
			break;
		case FRIENDS:
			count = db.update(FRIENDS_TABLE_NAME, values, selection, selectionArgs);
			break;
		case FRIEND_ID:
			id = uri.getPathSegments().get(1);
			count = db.update(FRIENDS_TABLE_NAME, values,
					Friends._ID
							+ "="
							+ id
							+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
									+ ')' : ""), selectionArgs);
			break;
		case GAMES:
			count = db.update(GAMES_TABLE_NAME, values, selection, selectionArgs);
			break;
		case GAME_ID:
			id = uri.getPathSegments().get(1);
			count = db.update(GAMES_TABLE_NAME, values,
					Games._ID
							+ "="
							+ id
							+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
									+ ')' : ""), selectionArgs);
			break;
		case ACHIEVEMENTS:
			count = db.update(ACHIEVEMENTS_TABLE_NAME, values, selection, selectionArgs);
			break;
		case ACHIEVEMENT_ID:
			id = uri.getPathSegments().get(1);
			count = db.update(ACHIEVEMENTS_TABLE_NAME, values,
					Achievements._ID
							+ "="
							+ id
							+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
									+ ')' : ""), selectionArgs);
			break;
		case MESSAGES:
			count = db.update(MESSAGES_TABLE_NAME, values, selection, selectionArgs);
			break;
		case MESSAGE_ID:
			id = uri.getPathSegments().get(1);
			count = db.update(MESSAGES_TABLE_NAME, values,
					Messages._ID
							+ "="
							+ id
							+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
									+ ')' : ""), selectionArgs);
			break;
		case EVENTS:
			count = db.update(EVENTS_TABLE_NAME, values, selection, selectionArgs);
			break;
		case EVENT_ID:
			id = uri.getPathSegments().get(1);
			count = db.update(EVENTS_TABLE_NAME, values, Events._ID
					+ "="
					+ id
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
							+ ')' : ""), selectionArgs);
			break;
		case BEACONS:
			count = db.update(BEACONS_TABLE_NAME, values, selection, selectionArgs);
			break;
		case BEACON_ID:
			id = uri.getPathSegments().get(1);
			count = db.update(BEACONS_TABLE_NAME, values, Beacons._ID
					+ "="
					+ id
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
							+ ')' : ""), selectionArgs);
			break;
		case SENT_MESSAGES:
			count = db.update(SENT_MESSAGES_TABLE_NAME, values, selection, selectionArgs);
			notify = true;
			break;
		case SENT_MESSAGE_ID:
			id = uri.getPathSegments().get(1);
			notify = true;
			count = db.update(SENT_MESSAGES_TABLE_NAME, values,
					SentMessages._ID
							+ "="
							+ id
							+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
									+ ')' : ""), selectionArgs);
			break;
		case NOTIFY_STATES:
			count = db.update(NOTIFY_STATES_TABLE_NAME, values, selection, selectionArgs);
			notify = true;
			break;
		case NOTIFY_STATE_ID:
			id = uri.getPathSegments().get(1);
			notify = true;
			count = db.update(NOTIFY_STATES_TABLE_NAME, values,
					NotifyStates._ID + "=" + id + (!TextUtils.isEmpty(selection) 
							? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		
		if (notify)
			getContext().getContentResolver().notifyChange(uri, null);
		
		return count;
	}

}
