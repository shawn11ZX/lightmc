package com.wooduan.lightmc.statistics.report;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.SingleThreadEventExecutor;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.wooduan.lightmc.statistics.report.StatisticItem;
import com.wooduan.lightmc.statistics.report.StatisticManager;

public class NettyEventExecutorGroupMonitor implements StatisticItem {
	
	public static void monitorApcServer(com.wooduan.lightmc.ApcServer server)
	{
		StatisticManager.INSTANCE.putItem("boss_" + server.getName(), new NettyEventExecutorGroupMonitor(server.getBossGroup()));
		StatisticManager.INSTANCE.putItem("work_" + server.getName(), new NettyEventExecutorGroupMonitor(server.getWorkerGroup()));
		StatisticManager.INSTANCE.putItem("apc_" + server.getName(), new NettyEventExecutorGroupMonitor(server.getApcExecutorGroup()));
	}
	
	
	public static void unmonitorApcServer(com.wooduan.lightmc.ApcServer server)
	{
		StatisticManager.INSTANCE.removeItem("boss_" + server.getName());
		StatisticManager.INSTANCE.removeItem("work_" + server.getName());
		StatisticManager.INSTANCE.removeItem("apc_" + server.getName());
	}
	
	public static void monitorApcClientFactory(com.wooduan.lightmc.ApcClientFactory client)
	{
		StatisticManager.INSTANCE.putItem("work_" + client.getName(), new NettyEventExecutorGroupMonitor(client.getIoExecutorGroup()));
		StatisticManager.INSTANCE.putItem("apc_" + client.getName(), new NettyEventExecutorGroupMonitor(client.getApcExecutorGroup()));
	}
	
	
	public static void unmonitorApcClientFactory(com.wooduan.lightmc.ApcClientFactory client)
	{
		StatisticManager.INSTANCE.removeItem("work_" + client.getName());
		StatisticManager.INSTANCE.removeItem("apc_" + client.getName());
	}
	

	
	public NettyEventExecutorGroupMonitor(EventExecutorGroup group)
	{
		this.group = group;
	}

	@Override
	public void saveAndReset(String name, Map<String, Object> output) {
		Set<EventExecutor> executors = group.children();
		int tasks = 0;
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
					tasks += f.get();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				} catch (ExecutionException e1) {
					e1.printStackTrace();
				}
				
			}
			
		}
		
		output.put("netty_" + name, tasks);
	}
	
	EventExecutorGroup group;
}
