package com.wooduan.lightmc.sample;

public class Config {
	public static String ip = "127.0.0.1";
	public static String getLoginIp()
	{
		return ip;
	}
	
	public static void setIp(String ip) {
		Config.ip = ip;
	}
	
	public static int getLoginPort()
	{
		return 2233;
	}
}
