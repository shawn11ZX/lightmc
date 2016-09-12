package com.wooduan.lightmc.statistics.report;

import java.util.concurrent.ConcurrentHashMap;


public class EventExecutorGroupItems {
	
	static private ConcurrentHashMap<String, EventExecutorGroupItem> records = new ConcurrentHashMap<String, EventExecutorGroupItem>();
	
	private static void putItem(String name, EventExecutorGroupItem item)
	{
		StatisticManager.INSTANCE.putItem(name, item);
		records.put(name, item);
	}
	
	private static void removeItem(String name)
	{
		StatisticManager.INSTANCE.removeItem(name);
		records.remove(name);
	}
	public static void monitorApcServer(com.wooduan.lightmc.ApcServer server)
	{
		putItem("boss_" + server.getName(), server.getListenEventExecutorGroupItem());
		putItem("work_" + server.getName(), server.getIoEventExecutorGroupItem());
		putItem("apc_" + server.getName(), server.getApcEventExecutorGroupItem());
	}
	
	
	public static void unmonitorApcServer(com.wooduan.lightmc.ApcServer server)
	{
		removeItem("boss_" + server.getName());
		removeItem("work_" + server.getName());
		removeItem("apc_" + server.getName());
	}
	
	public static void monitorApcClientFactory(com.wooduan.lightmc.ApcClientFactory client)
	{
		putItem("work_" + client.getName(), client.getIoEventExecutorGroupItem());
		putItem("apc_" + client.getName(), client.getApcEventExecutorGroupItem());
	}
	
	
	public static void unmonitorApcClientFactory(com.wooduan.lightmc.ApcClientFactory client)
	{
		removeItem("work_" + client.getName());
		removeItem("apc_" + client.getName());
	}
	

	public static ConcurrentHashMap<String, EventExecutorGroupItem> getAllItems() {
		return records;
	}
}
