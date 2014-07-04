/*
 * SearchMessageView.java 
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

package com.akop.bach.activity.xboxlive;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.akop.bach.R;
import com.akop.bach.SupportsMessaging;
import com.akop.bach.fragment.xboxlive.SentMessageViewFragment;

public class SentMessageView extends XboxLiveSinglePane
{
	public static void actionShow(Context context, 
			SupportsMessaging account, long messageId)
	{
		Intent intent = new Intent(context, SentMessageView.class);
		intent.putExtra("account", account);
		intent.putExtra("messageId", messageId);
		
		context.startActivity(intent);
	}
	
	@Override
    protected String getSubtitle()
    {
		return getString(R.string.sent_messages);
    }
	
	@Override
	protected Fragment createFragment() 
	{
		return SentMessageViewFragment.newInstance(getAccount(), 
				getIntent().getLongExtra("messageId", -1));
	}
}
