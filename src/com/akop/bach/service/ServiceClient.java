/*
 * ServiceClient.java 
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

package com.akop.bach.service;

import java.io.IOException;

import android.app.NotificationManager;
import android.content.Context;

import com.akop.bach.BasicAccount;
import com.akop.bach.App;
import com.akop.bach.parser.AuthenticationException;
import com.akop.bach.parser.ParserException;
import com.akop.bach.service.NotificationService.AccountSchedule;

public abstract class ServiceClient
{
	protected static final int DEFAULT_LIGHTS_ON_MS = 3000;
	protected static final int DEFAULT_LIGHTS_OFF_MS = 10000;
	protected static final int DEFAULT_LIGHTS_COLOR = 0xffffffff;
	
	public abstract Object setupParameters(BasicAccount account);
	
	protected abstract void synchronize(BasicAccount account)
			throws IOException, ParserException, AuthenticationException;
	
	protected abstract void notify(BasicAccount account, AccountSchedule schedule);
	
	protected Context getContext()
	{
		return App.getInstance();
	}
	
	protected NotificationManager getNotificationManager() 
	{
		return (NotificationManager)App.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	public void update(BasicAccount account, AccountSchedule schedule)
	{
		try
		{
			synchronize(account);
		}
		catch(Exception e)
		{
			if (App.getConfig().logToConsole())
				e.printStackTrace();
		}
		
		try
		{
			notify(account, schedule);
		}
		catch(Exception e)
		{
			if (App.getConfig().logToConsole())
				e.printStackTrace();
		}
	}
}