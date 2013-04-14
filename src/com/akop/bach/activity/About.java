/*
 * About.java 
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

package com.akop.bach.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.TextView;

import com.akop.bach.R;

public class About extends Activity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		
		TextView appVersion = (TextView)findViewById(R.id.app_version);
		TextView appWebsite = (TextView)findViewById(R.id.app_website);
		appVersion.setText(getString(R.string.app_version_format,
				getVersionName()));
		
		SpannableString s = new SpannableString(getText(R.string.app_website));
		Linkify.addLinks(s, Linkify.WEB_URLS);
		appWebsite.setText(s);
		appWebsite.setMovementMethod(LinkMovementMethod.getInstance());
	}
	
	public static void actionShowAbout(Context context)
	{
		Intent intent = new Intent(context, About.class);
		context.startActivity(intent);
	}
	
	public String getVersionName()
	{
		try
		{
			ComponentName comp = new ComponentName(this, About.class);
			PackageInfo pinfo = this.getPackageManager().getPackageInfo(
					comp.getPackageName(), 0);
			return pinfo.versionName;
		}
		catch (android.content.pm.PackageManager.NameNotFoundException e)
		{
			return null;
		}
	}
}
