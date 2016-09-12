package com.wooduan.lightmc.statistics.report;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class QpsItem implements StatisticItem {
	AtomicInteger countTotal = new AtomicInteger();
	volatile long lastResetTs;
	public void kick()
	{
		countTotal.incrementAndGet();
	}
	
	public void saveAndReset(String name, Map<String, Object> output) {
		int count = this.countTotal.get();
		if (lastResetTs != 0) {
			
			long currentTs = System.currentTimeMillis();
			long duration = currentTs - lastResetTs;
			if (duration > 1000) {
				output.put(name, count * 1.0 / duration);	
			}
		}
		lastResetTs = System.currentTimeMillis();
		this.countTotal.addAndGet(count * -1);
	}

}