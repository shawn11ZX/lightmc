package com.wooduan.lightmc.statistics.report;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ValueItem extends StatisticHisotry<ValueItem.History> implements StatisticItem {
	
	static class History {
		public History(int minValue, int maxValue)
		{
			this.minValue = minValue;
			this.maxValue = maxValue;
		}
		int minValue;
		int maxValue;
	}
	
	volatile AtomicInteger value = new AtomicInteger();
	volatile int minValue;
	volatile int maxValue;
	
	public void set(int v)
	{
		value.set(v);;
		
		if (v > maxValue)
		{
			maxValue =v;
		}
		if (v < minValue)
		{
			minValue =v;
		}
	}
	public int increase() {
		int v = value.incrementAndGet();
		if (v > maxValue)
		{
			maxValue =v;
		}
		
		return v;
	}
	
	public int descrease() {
		int v = value.decrementAndGet();
		if (v < minValue)
		{
			minValue =v;
		}
		
		return v;
	}
	
	public int getValue() {
		return value.get();
	}
	public void save(String name, Map<String, Object> output) {
		int c = value.get();
		output.put(name, c);
		output.put(name + "Max", maxValue);
		output.put(name + "Min", minValue);
		
		addHistory(new History(minValue, maxValue));
	}
	
	public void reset()
	{
		value.set(0);
	}

	public void resetMinMax()
	{
		minValue = 0;
		maxValue = 0;
	}
	
	public int getMaxValue() {
		return maxValue;
	}
	
	public int getMinValue() {
		return minValue;
	}
	
	public int getHistoryMaxValue(int historyCount) {
		int max = 0;
		for(History history: historyList)
		{
			if (historyCount-- <= 0)
				break;
			
			if (max < history.maxValue)
				max  = history.maxValue;
		}
		return max;
	}
	
	public int getHistoryMinValue(int historyCount) {
		int min = 0;
		for(History history: historyList)
		{
			if (historyCount-- <= 0)
				break;
			if (min > history.minValue)
				min  = history.minValue;
		}
		return min;
	}
	
}