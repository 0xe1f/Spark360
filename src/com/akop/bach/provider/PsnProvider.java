/*
 * PsnProvider.java 
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
import com.akop.bach.PSN.Friends;
import com.akop.bach.PSN.Games;
import com.akop.bach.PSN.Profiles;
import com.akop.bach.PSN.Trophies;
import com.akop.bach.R;

public class PsnProvider extends ContentProvider
{
	public static final String AUTHORITY = "com.akop.bach.psnprovider";
	
	private static final int PROFILES = 1;
	private static final int PROFILE_ID = 2;
	private static final int GAMES = 3;
	private static final int GAME_ID = 4;
	private static final int TROPHIES = 5;
	private static final int TROPHY_ID = 6;
	private static final int FRIENDS = 7;
	private static final int FRIEND_ID = 8;
	private static final int SEARCH_FRIEND_SUGGEST = 13;
	
	private static final UriMatcher sUriMatcher;
	
	private static final String PROFILES_TABLE_NAME = "profiles";
	private static final String GAMES_TABLE_NAME = "games";
	private static final String TROPHIES_TABLE_NAME = "trophies";
	private static final String FRIENDS_TABLE_NAME = "friends";
	
	private static final String DATABASE_NAME = "psn.db";
	
	private static final int DATABASE_VERSION = 16;
	
    private DbHelper mDbHelper;
    
	static
	{
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		
		sUriMatcher.addURI(AUTHORITY, "profiles", PROFILES);
		sUriMatcher.addURI(AUTHORITY, "profiles/#", PROFILE_ID);
		sUriMatcher.addURI(AUTHORITY, "games", GAMES);
		sUriMatcher.addURI(AUTHORITY, "games/#", GAME_ID);
		sUriMatcher.addURI(AUTHORITY, "trophies", TROPHIES);
		sUriMatcher.addURI(AUTHORITY, "trophies/#", TROPHY_ID);
		sUriMatcher.addURI(AUTHORITY, "friends", FRIENDS);
		sUriMatcher.addURI(AUTHORITY, "friends/#", FRIEND_ID);
		
		sUriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY,
				SEARCH_FRIEND_SUGGEST);
		sUriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY
				+ "/*", SEARCH_FRIEND_SUGGEST);
	}
	
	private static class DbHelper extends SQLiteOpenHelper
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
					+ Profiles.ONLINE_ID + " TEXT NOT NULL, "
					+ Profiles.ICON_URL + " TEXT, "
					+ Profiles.LEVEL + " INTEGER NOT NULL, "
					+ Profiles.MEMBER_TYPE + " INTEGER NOT NULL DEFAULT 0, "
					+ Profiles.TROPHIES_PLATINUM + " INTEGER NOT NULL, "
					+ Profiles.TROPHIES_GOLD + " INTEGER NOT NULL, "
					+ Profiles.TROPHIES_SILVER + " INTEGER NOT NULL, "
					+ Profiles.TROPHIES_BRONZE + " INTEGER NOT NULL, "
					+ Profiles.PROGRESS + " INTEGER NOT NULL);");
			
			db.execSQL("CREATE TABLE " + GAMES_TABLE_NAME + " ("
					+ Games._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ Games.UID + " TEXT NOT NULL, "
					+ Games.TITLE + " TEXT NOT NULL, "
					+ Games.ACCOUNT_ID + " INTEGER NOT NULL, "
					+ Games.PROGRESS + " INTEGER NOT NULL, "
					+ Games.UNLOCKED_PLATINUM + " INTEGER NOT NULL, "
					+ Games.UNLOCKED_GOLD + " INTEGER NOT NULL, "
					+ Games.UNLOCKED_SILVER + " INTEGER NOT NULL, "
					+ Games.UNLOCKED_BRONZE + " INTEGER NOT NULL, "
					+ Games.TROPHIES_DIRTY + " INTEGER NOT NULL, "
					+ Games.SORT_ORDER + " INTEGER NOT NULL, "
					+ Games.ICON_URL + " TEXT NOT NULL, "
					+ Games.LAST_UPDATED + " INTEGER NOT NULL,"
					+ "UNIQUE (" + Games.ACCOUNT_ID + "," + Games.UID + "));");
			
			db.execSQL("CREATE TABLE " + TROPHIES_TABLE_NAME + " ("
					+ Trophies._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ Trophies.GAME_ID + " INTEGER NOT NULL, "
					+ Trophies.TITLE + " TEXT NOT NULL, "
					+ Trophies.DESCRIPTION + " TEXT, "
					+ Trophies.ICON_URL + " TEXT, "
					+ Trophies.EARNED + " INTEGER NOT NULL, "
					+ Trophies.EARNED_TEXT + " TEXT, "
					+ Trophies.TYPE + " INTEGER NOT NULL, "
					+ Trophies.IS_SECRET + " INTEGER NOT NULL, "
					+ Trophies.SORT_ORDER + " INTEGER NOT NULL);");
			
			db.execSQL("CREATE TABLE " + FRIENDS_TABLE_NAME + " ("
					+ Friends._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ Friends.ACCOUNT_ID + " INTEGER NOT NULL, "
					+ Friends.ONLINE_ID + " TEXT NOT NULL, "
					+ Friends.LEVEL + " INTEGER NOT NULL, "
					+ Friends.PROGRESS + " INTEGER NOT NULL, "
					+ Friends.ONLINE_STATUS + " INTEGER NOT NULL, "
					+ Friends.PLAYING + " TEXT, "
					+ Friends.ICON_URL + " TEXT, "
					+ Friends.IS_FAVORITE + " INTEGER NOT NULL DEFAULT 0, "
					+ Friends.MEMBER_TYPE + " INTEGER NOT NULL DEFAULT 0, "
					+ Friends.COMMENT + " TEXT, "
					+ Friends.LAST_UPDATED + " INTEGER NOT NULL DEFAULT 0, "
					+ Friends.DELETE_MARKER + " INTEGER NOT NULL DEFAULT 0, "
					+ Friends.TROPHIES_BRONZE + " INTEGER NOT NULL, "
					+ Friends.TROPHIES_SILVER + " INTEGER NOT NULL, "
					+ Friends.TROPHIES_GOLD + " INTEGER NOT NULL, "
					+ Friends.TROPHIES_PLATINUM + " INTEGER NOT NULL);");
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			if (App.LOGV)
				App.logv("Upgrading database from version %d to %d", 
						oldVersion, newVersion);
			
			boolean upgraded = false;
			if (oldVersion < 13)
			{
				upgraded = true;
				if (App.LOGV)
					App.logv("PsnProvider: upgrading to version 13");
				
				db.execSQL("ALTER TABLE " + TROPHIES_TABLE_NAME + " ADD COLUMN "
						+ Trophies.EARNED_TEXT + " TEXT");
				db.execSQL("ALTER TABLE " + FRIENDS_TABLE_NAME + " ADD COLUMN "
						+ Friends.LAST_UPDATED + " INTEGER NOT NULL DEFAULT 0");
				db.execSQL("ALTER TABLE " + FRIENDS_TABLE_NAME + " ADD COLUMN "
						+ Friends.DELETE_MARKER + " INTEGER NOT NULL DEFAULT 0");
			}
			
			if (oldVersion < 14)
			{
				upgraded = true;
				if (App.LOGV)
					App.logv("PsnProvider: upgrading to version 14");
				
				db.execSQL("ALTER TABLE " + FRIENDS_TABLE_NAME + " ADD COLUMN "
						+ Friends.IS_FAVORITE + " INTEGER NOT NULL DEFAULT 0");
			}
			
			if (oldVersion < 15)
			{
				upgraded = true;
				if (App.LOGV)
					App.logv("PsnProvider: upgrading to version 15");
				
				db.execSQL("ALTER TABLE " + PROFILES_TABLE_NAME + " ADD COLUMN "
						+ Profiles.MEMBER_TYPE + " INTEGER NOT NULL DEFAULT 0");
				db.execSQL("ALTER TABLE " + FRIENDS_TABLE_NAME + " ADD COLUMN "
						+ Friends.MEMBER_TYPE + " INTEGER NOT NULL DEFAULT 0");
				db.execSQL("ALTER TABLE " + FRIENDS_TABLE_NAME + " ADD COLUMN "
						+ Friends.COMMENT + " TEXT");
				db.execSQL("DELETE FROM " + GAMES_TABLE_NAME);
				db.execSQL("DELETE FROM " + TROPHIES_TABLE_NAME);
			}
			
			if (oldVersion < 16)
			{
				upgraded = true;
				if (App.LOGV)
					App.logv("PsnProvider: upgrading to version 16");
				
				db.execSQL("DELETE FROM " + GAMES_TABLE_NAME);
				db.execSQL("DELETE FROM " + TROPHIES_TABLE_NAME);
			}
			
			if (!upgraded)
			{
				if (App.LOGV)
					App.logv("PsnProvider: Recreating structure");
				
				db.execSQL("DROP TABLE IF EXISTS " + PROFILES_TABLE_NAME);
				db.execSQL("DROP TABLE IF EXISTS " + GAMES_TABLE_NAME);
				db.execSQL("DROP TABLE IF EXISTS " + TROPHIES_TABLE_NAME);
				db.execSQL("DROP TABLE IF EXISTS " + FRIENDS_TABLE_NAME);
				
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
		case GAMES: return Games.CONTENT_TYPE;
		case GAME_ID: return Games.CONTENT_ITEM_TYPE;
		case TROPHIES: return Trophies.CONTENT_TYPE;
		case TROPHY_ID: return Trophies.CONTENT_ITEM_TYPE;
		case FRIENDS: return Friends.CONTENT_TYPE;
		case FRIEND_ID: return Friends.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
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
					Friends.ONLINE_ID + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1,
					"'" + getContext().getString(R.string.psn_friend)
							+ "' AS " + SearchManager.SUGGEST_COLUMN_TEXT_2,
					Friends._ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
				},
				Friends.ONLINE_ID + " LIKE '%'||?||'%'", new String[] { query }, 
				null, null,
				Friends.ONLINE_ID + " COLLATE NOCASE ASC");
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
		case PROFILES:
			qb.setTables(PROFILES_TABLE_NAME);
			if (TextUtils.isEmpty(sortOrder))
				orderBy = Profiles.DEFAULT_SORT_ORDER;
			break;
		case PROFILE_ID:
			qb.setTables(PROFILES_TABLE_NAME);
			qb.appendWhere(Profiles._ID + "=" + uri.getPathSegments().get(1));
			break;
		case GAMES:
			qb.setTables(GAMES_TABLE_NAME);
			if (TextUtils.isEmpty(sortOrder))
				orderBy = Games.DEFAULT_SORT_ORDER;
			break;
		case GAME_ID:
			qb.setTables(GAMES_TABLE_NAME);
			qb.appendWhere(Games._ID + "=" + uri.getPathSegments().get(1));
			break;
		case TROPHIES:
			qb.setTables(TROPHIES_TABLE_NAME);
			if (TextUtils.isEmpty(sortOrder))
				orderBy = Trophies.DEFAULT_SORT_ORDER;
			break;
		case TROPHY_ID:
			qb.setTables(TROPHIES_TABLE_NAME);
			qb.appendWhere(Trophies._ID + "=" + uri.getPathSegments().get(1));
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
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int type = sUriMatcher.match(uri);
        int count;
        String id;
        
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
		case GAMES:
			count = db.delete(GAMES_TABLE_NAME, selection, selectionArgs);
			break;
		case GAME_ID:
			id = uri.getPathSegments().get(1);
			count = db.delete(GAMES_TABLE_NAME,
					Games._ID + "=" + id
							+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
									+ ')' : ""), selectionArgs);
			break;
		case TROPHIES:
			count = db.delete(TROPHIES_TABLE_NAME, selection, selectionArgs);
			break;
		case TROPHY_ID:
			id = uri.getPathSegments().get(1);
			count = db.delete(TROPHIES_TABLE_NAME,
					Trophies._ID + "=" + id
							+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
									+ ')' : ""), selectionArgs);
			break;
		case FRIENDS:
			count = db.delete(FRIENDS_TABLE_NAME, selection, selectionArgs);
			break;
		case FRIEND_ID:
			id = uri.getPathSegments().get(1);
			count = db.delete(FRIENDS_TABLE_NAME,
					Friends._ID + "=" + id
							+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
									+ ')' : ""), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		
		// NOTE: suppressing update notification
		//getContext().getContentResolver().notifyChange(uri, null);
		
		return count;
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
			if (!values.containsKey(Profiles.ONLINE_ID))
				throw new SQLException("Online ID not specified");
			if (!values.containsKey(Profiles.LEVEL))
				values.put(Profiles.LEVEL, 0);
			if (!values.containsKey(Profiles.PROGRESS))
				values.put(Profiles.PROGRESS, 0);
			
			if (!values.containsKey(Profiles.TROPHIES_PLATINUM))
				values.put(Profiles.TROPHIES_PLATINUM, 0);
			if (!values.containsKey(Profiles.TROPHIES_GOLD))
				values.put(Profiles.TROPHIES_GOLD, 0);
			if (!values.containsKey(Profiles.TROPHIES_SILVER))
				values.put(Profiles.TROPHIES_SILVER, 0);
			if (!values.containsKey(Profiles.TROPHIES_BRONZE))
				values.put(Profiles.TROPHIES_BRONZE, 0);
			
			db = mDbHelper.getWritableDatabase();
			if ((id = db.insert(PROFILES_TABLE_NAME, Profiles.ACCOUNT_ID, values)) > 0)
			{
				newUri = ContentUris.withAppendedId(Profiles.CONTENT_URI, id);
				getContext().getContentResolver().notifyChange(newUri, null);
			}
			
			break;
			
		case GAMES:
			if (initialValues == null)
				throw new IllegalArgumentException("Missing game information");
			
			values = new ContentValues(initialValues);
			
			db = mDbHelper.getWritableDatabase();
			if ((id = db.insert(GAMES_TABLE_NAME, Games.ACCOUNT_ID, values)) > 0)
			{
				newUri = ContentUris.withAppendedId(Games.CONTENT_URI, id);
				//getContext().getContentResolver().notifyChange(newUri, null);
			}
			
			break;
			
		case TROPHIES:
			if (initialValues == null)
				throw new IllegalArgumentException("Missing trophy information");
			
			values = new ContentValues(initialValues);
			
			if (!values.containsKey(Trophies.EARNED))
				values.put(Trophies.EARNED, 0);
			if (!values.containsKey(Trophies.IS_SECRET))
				values.put(Trophies.IS_SECRET, 0);
			if (!values.containsKey(Trophies.TYPE))
				values.put(Trophies.TYPE, 0);
			
			db = mDbHelper.getWritableDatabase();
			if ((id = db.insert(TROPHIES_TABLE_NAME, Trophies.GAME_ID, values)) > 0)
			{
				newUri = ContentUris.withAppendedId(Trophies.CONTENT_URI, id);
				//getContext().getContentResolver().notifyChange(newUri, null);
			}
			
			break;
			
		case FRIENDS:
			if (initialValues == null)
				throw new IllegalArgumentException("Missing friend information");
			
			values = new ContentValues(initialValues);
			
			db = mDbHelper.getWritableDatabase();
			if ((id = db.insert(FRIENDS_TABLE_NAME, Friends.ACCOUNT_ID, values)) > 0)
			{
				newUri = ContentUris.withAppendedId(Friends.CONTENT_URI, id);
				//getContext().getContentResolver().notifyChange(newUri, null);
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
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs)
	{
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		int type = sUriMatcher.match(uri);
		int count;
		String id;
		
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
		case GAMES:
			count = db.update(GAMES_TABLE_NAME, values, selection, selectionArgs);
			break;
		case GAME_ID:
			id = uri.getPathSegments().get(1);
			count = db.update(GAMES_TABLE_NAME, values,
					Games._ID + "=" + id
							+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
									+ ')' : ""), selectionArgs);
			break;
		case TROPHIES:
			count = db.update(TROPHIES_TABLE_NAME, values, selection, selectionArgs);
			break;
		case TROPHY_ID:
			id = uri.getPathSegments().get(1);
			count = db.update(TROPHIES_TABLE_NAME, values,
					Trophies._ID + "=" + id
							+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
									+ ')' : ""), selectionArgs);
			break;
		case FRIENDS:
			count = db.update(FRIENDS_TABLE_NAME, values, selection, selectionArgs);
			break;
		case FRIEND_ID:
			id = uri.getPathSegments().get(1);
			count = db.update(FRIENDS_TABLE_NAME, values,
					Friends._ID + "=" + id
							+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
									+ ')' : ""), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		
		// NOTE: suppressing update notification
		//getContext().getContentResolver().notifyChange(uri, null);
		
		return count;
	}

}
