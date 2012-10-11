package com.akop.bach.configurations;

public class DevConfig extends AppConfig
{
	@Override
	public boolean logToConsole() 
	{
		return true;
	}
	
	@Override
	public boolean logHttp() 
	{
		return false;
	}
	
	@Override
	public boolean enableErrorReporting() 
	{
		return false;
	}
}
