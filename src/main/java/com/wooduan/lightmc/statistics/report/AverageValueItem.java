package com.wooduan.lightmc.statistics.report;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class AverageValueItem extends StatisticHisotry<AverageValueItem.History> implements StatisticItem {
	
	static class History {
		public History(int totalCount, long totalValue)
		{
			this.totalCount = totalCount;
			this.totalValue = totalValue;
		}
		int totalCount;
		long totalValue;
	}
	
	
	AtomicInteger totalCount = new AtomicInteger();
	AtomicLong totalValue = new AtomicLong();
	
	
	public void record(long value)
	{
		totalValue.addAndGet(value);
		totalCount.incrementAndGet();
	}
	
	public int getTotalCount() {
		return totalCount.get();
	}
	
	public long getTotalValue() {
		return totalValue.get();
	}
	
	
	public long getAverageValue()
	{
		int count = totalCount.get();
		if (count == 0)
		{
			return 0;
		}
		else {
			return totalValue.get() / count;
		}
	}
	
	////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////
	
	
	////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////
	public int getHistoryTotalCount(int historyCount)
	{
		int count = 0;
		
		for(History history: historyList)
		{
			if (historyCount-- <= 0)
				break;
			count += history.totalCount;
		}
		return count;
	}
	
	public long getHistoryTotalValue(int historyCount)
	{
		int value = 0;
		for(History history: historyList)
		{
			if (historyCount-- <= 0)
				break;
			value += history.totalValue;
		}
		return value;
	}
	
	public long getHistoryAverageValue(int historyCount)
	{
		int value = 0;
		int count = 0;
		for(History history: historyList)
		{
			if (historyCount-- <= 0)
				break;
			value += history.totalValue;
			count += history.totalCount;
		}
		if (count > 0)
		{
			return value / count;
		}
		else {
			return 0;
		}
	}
	
	
	
////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////
	public void saveAndReset(String name, Map<String, Object> output) {
		
		long v = totalValue.get();
		int c = this.totalCount.get();
		long avg = (c == 0) ? 0 : (int)(v/(c));
		
		addHistory(new History(c, v));

		output.put(name + "_count", c);
		output.put(name + "_avgValue", avg);

		
		totalValue.set(0);
		this.totalCount.set(0);
		
	}
	
}