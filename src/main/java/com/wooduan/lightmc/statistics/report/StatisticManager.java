/*
 * (C) 2012-2013 Wooduan Group.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 * Authors:
 * duty <huahuai009@163.com>
 */
package com.wooduan.lightmc.statistics.report;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Game内部运行时统计信息

 */
public enum StatisticManager {
	INSTANCE;
	
	private static final Logger Logger = LoggerFactory.getLogger(StatisticManager.class);
	
	private ConcurrentHashMap<String, StatisticItem> records = new ConcurrentHashMap<String, StatisticItem>();
	volatile private Map<String, Object> lastMap = new Hashtable<String, Object>();
	
	public <T extends StatisticItem> T createItem(String name, Class<T> c) {
		return getItem(name, c);
	}
	public <T extends StatisticItem> T getItem(String name, Class<T> c) {
		T item;
		try {
			item = (T) records.get(name);
			if (item == null) {
				item = c.newInstance();
				records.put(name, item);
			}
			return item;
		} catch (Exception e) {
			Logger.info("error while creating item {}", name, e);
		}
		return null;
	}
	
	
	public <T extends StatisticItem> T putItem(String name, T item) {
		records.put(name, item);
		return item;
	}
	
	public void removeItem(String name)
	{
		records.remove(name);
	}
	
	public Map<String, Object> reset()
	{
		Map<String, Object> m = new Hashtable<String, Object>();
		
		
		for (Map.Entry<String, StatisticItem> entry: records.entrySet()) {
			try {
				entry.getValue().saveAndReset(entry.getKey(), m);
			} catch (Exception e) {
				Logger.error("error while saveAndReset", e);
			}
		}
		lastMap = m;
		return m;
	}
	public Map<String, Object> lastMap()
	{
		return lastMap;
	}
	public Map<String, Object> reportAndReset()
	{
		Map<String, Object> m = reset();
		
		StringBuffer sb = new StringBuffer();
		
		
		for (Map.Entry<String, Object> entry: m.entrySet()) {
			sb.append(entry.getKey());
			sb.append(":");
			sb.append(entry.getValue());
			sb.append(", ");
		}
		Logger.info("{}", sb);
		
		ReportClient.INSTANCE.report(m);
		return m;
	}
}
