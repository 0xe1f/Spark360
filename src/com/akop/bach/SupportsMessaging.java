/*
 * SupportsMessaging.java
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

package com.akop.bach;

import java.io.IOException;

import android.content.Context;

import com.akop.bach.parser.AuthenticationException;
import com.akop.bach.parser.ParserException;

public interface SupportsMessaging extends Account
{
	void updateMessages(Context context)
			throws AuthenticationException, IOException, ParserException;

	void updateMessage(Context context, Object messageId)
			throws AuthenticationException, IOException, ParserException;
	
	void sendMessage(Context context, String[] recipients, String body)
			throws AuthenticationException, IOException, ParserException;

	void deleteMessage(Context context, Object messageId)
			throws AuthenticationException, IOException, ParserException;
	
	public long getLastMessageUpdate();
	public long getMessageRefreshInterval();
	
	void actionComposeMessage(Context context, String to);
	void actionOpenMessage(Context context, long messageUid);
}
