package com.wooduan.lightmc.statistics.report;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class AverageValueItem implements StatisticItem {
	AtomicInteger countTotal = new AtomicInteger();
	AtomicLong totalValue = new AtomicLong();
	volatile long lastResetTs;
	
	volatile double lastQps;
	volatile int lastCount;
	volatile int lastAvgValue;
	volatile long lastTotalValue;
	public void record(long timeMicro)
	{
		totalValue.addAndGet(timeMicro);
		countTotal.incrementAndGet();
	}
	public double getLastQps()
	{
		return lastQps;
	}
	
	public int getLastCount()
	{
		return lastCount;
	}
	
	public int getLastAvgValue()
	{
		return lastAvgValue;
	}
	
	public long getLastTotalValue()
	{
		return lastTotalValue;
	}
	public void saveAndReset(String name, Map<String, Object> output) {
		long timeMicro = totalValue.get();
		int count = this.countTotal.get();
		
		if (count == 0)
			count = 1;
		if (lastResetTs != 0) {
			
			long currentTs = System.currentTimeMillis();
			long duration = currentTs - lastResetTs;
			lastQps = (count * 1.0 / duration);
			lastCount = count;
			lastAvgValue = (int)(timeMicro/(count));
			lastTotalValue = timeMicro;
			if (duration > 1000 && count > 0) {
				output.put(name + "_qps", lastQps);
				output.put(name + "_count", lastCount);
				output.put(name + "_avgValue", lastAvgValue);
				output.put(name + "_avgMs", lastAvgValue/1000);
			}
		}

		
		lastResetTs = System.currentTimeMillis();
		totalValue.set(0);
		this.countTotal.set(0);
	}
}