/*
 * GenericSecret.java 
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

public class GenericSecret implements Secret
{
	@Override
	public String getCryptKey() 
	{
		return "TheOwlsAreNotWhatTheySeem";
	}
	
	@Override
	public String getPassword() 
	{
		return "WithoutChemicalsHePoints";
	}
	
	@Override
	public String getCipherAlgo() 
	{
		return "AES/CBC/PKCS5Padding";
	}
	
	@Override
	public String getKeygenAlgo() 
	{
		return "PBEWITHSHAAND256BITAES-CBC-BC";
	}
	
	@Override
	public String getSecretKeyAlgo() 
	{
		return "AES";
	}
	
	@Override
	public byte[] getSalt() 
	{
		return new byte[] 
		{
			(byte)0xb1,(byte)0x46,(byte)0xa1,(byte)0x0d,(byte)0x0b,
			(byte)0x37,(byte)0x89,(byte)0xb8,(byte)0x20,(byte)0x94,
			(byte)0xed,(byte)0xab,(byte)0xd1,(byte)0x00,(byte)0x8b,
			(byte)0x73,(byte)0x0d,(byte)0x47,(byte)0x0a,(byte)0xd6,
		};
	}
	
	@Override
	public byte[] getIv() 
	{
		return new byte[]
		{
	    	(byte)0x8e,(byte)0x35,(byte)0x54,(byte)0xae,
	    	(byte)0x2b,(byte)0x5a,(byte)0xa3,(byte)0xf8,
	    	(byte)0xf3,(byte)0x71,(byte)0xef,(byte)0x09,
	    	(byte)0x98,(byte)0xf4,(byte)0xb1,(byte)0x11,
		};
	}
}
