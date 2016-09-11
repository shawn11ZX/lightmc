package com.wooduan.lightmc.statistics.report;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ValueItem implements StatisticItem {
	volatile AtomicInteger value = new AtomicInteger();
	volatile int lastValue = 0;
	volatile boolean isRecorded;
	public void set(int v)
	{
		value.set(v);;
		isRecorded = true;
	}
	public int increase() {
		int v = value.incrementAndGet();
		isRecorded = true;
		return v;
	}
	
	public int descrease() {
		int v = value.decrementAndGet();
		isRecorded = true;
		return v;
	}
	
	public int get() {
		return value.get();
	}
	public void save(String name, Map<String, Object> output) {
		if (isRecorded) {
			int c = value.get();
			output.put(name, c);
			lastValue = c;
		}
	}
	public int lastValue() {
		return lastValue;
	}
}