package com.wooduan.lightmc.statistics.report;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class StatisticHisotry<T> {
	
	public static int MAX_HISTORY_COUNT = 60;
	
	Deque<T> historyList = new ConcurrentLinkedDeque<T>();
	AtomicInteger historyListSize = new AtomicInteger();
	
	public void addHistory(T history)
	{
		historyListSize.incrementAndGet();
		historyList.addFirst(history);
		if (historyListSize.get() > MAX_HISTORY_COUNT)
		{
			historyListSize.decrementAndGet();
			historyList.removeLast();
		}
	}
}