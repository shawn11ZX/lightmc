package com.wooduan.lightmc.statistics.report;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.SingleThreadEventExecutor;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wooduan.lightmc.netty.TimedEventExecutor;

public class EventExecutorGroupItem implements StatisticItem {
	
	
	
	private static final Logger logger = LoggerFactory.getLogger(EventExecutorGroupItem.class);
	
	final private CumulativeValueItem execTimeItem = new CumulativeValueItem();
	final private CumulativeValueItem takeTimeItem = new CumulativeValueItem();
	final private CumulativeValueItem taskCount = new CumulativeValueItem();
	
	final private EventExecutorGroup group;
	public EventExecutorGroupItem(EventExecutorGroup group)
	{
		this.group = group;
	}

	@Override
	public void saveAndReset(String name, Map<String, Object> output) {
		Set<EventExecutor> executors = group.children();
		int tasks = 0;
		long takeTime = 0;
		long execTime = 0;
		for (EventExecutor executor: executors)
		{
			if (executor instanceof SingleThreadEventExecutor) {
				final SingleThreadEventExecutor e = (SingleThreadEventExecutor) executor;
				Future<Integer> f = e.submit(new Callable<Integer>() {

					@Override
					public Integer call() throws Exception {
						return e.pendingTasks();
					}
				});
				
				try {
					if (executor instanceof TimedEventExecutor) {
						TimedEventExecutor te = (TimedEventExecutor) executor;
						takeTime += te.getTotalTakeTime();
						execTime += te.getTotalExecTime();
						te.resetTime();
					}
					
					
					tasks += f.get(5, TimeUnit.SECONDS);
				} catch (Exception ex) {
					logger.error("exception", ex);
				} 
				
			}
			
		}
		
		taskCount.set(tasks);
		execTimeItem.set((int)(execTime/1000000L));
		takeTimeItem.set((int)(takeTime/1000000L));
		
		taskCount.saveAndReset("netty_" + name, output);
		execTimeItem.saveAndReset("netty_" + name + "_execMs", output);
		takeTimeItem.saveAndReset("netty_" + name + "_takeMs", output);
		
	}
	
	public CumulativeValueItem getExecTimeItem() {
		return execTimeItem;
	}
	public CumulativeValueItem getTakeTimeItem() {
		return takeTimeItem;
	}
	
	public CumulativeValueItem getTaskCountItem() {
		return taskCount;
	}

}
