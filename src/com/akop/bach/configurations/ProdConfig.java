package com.akop.bach.configurations;

public class ProdConfig extends AppConfig
{
	@Override
	public boolean logToConsole() 
	{
		return false;
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
