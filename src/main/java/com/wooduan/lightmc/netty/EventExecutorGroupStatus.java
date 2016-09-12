package com.wooduan.lightmc.netty;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.SingleThreadEventExecutor;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventExecutorGroupStatus {
	private static final Logger logger = LoggerFactory.getLogger(EventExecutorGroupStatus.class);
	
	volatile long execMs;
	volatile long takeMs;
	volatile int taskCount;
	
	public EventExecutorGroupStatus(EventExecutorGroup group) {
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
		
		taskCount = tasks;
		execMs = execTime/1000000;
		takeMs = takeTime/1000000;
	}
	
	public long getExecMs() {
		return execMs;
	}
	
	public long getTakeMs() {
		return takeMs;
	}
	
	public int getTaskConut() {
		return taskCount;
	}
}
